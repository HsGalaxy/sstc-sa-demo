/**
 * Captures the running dashboard with headless Chrome through the DevTools protocol.
 *
 * The dashboard keeps its session in localStorage, so the script first opens the app origin,
 * seeds the token, then navigates to the requested routes and screenshots each one after the
 * page has finished rendering.
 *
 * Usage:
 *   node scripts/capture-screenshots.mjs --base http://localhost:8080 --out docs/screenshots
 */
import { spawn } from 'node:child_process'
import { mkdirSync, writeFileSync, existsSync, rmSync } from 'node:fs'
import { join, resolve } from 'node:path'
import { randomUUID, createHash } from 'node:crypto'
import { tmpdir } from 'node:os'

// ---------------------------------------------------------------- tiny WebSocket client
class Ws {
  constructor(url) {
    this.url = url
    this.buffer = Buffer.alloc(0)
    this.handlers = new Map()
    this.nextId = 1
    this.fragments = []
    this.fragmentOpcode = 0
  }

  async connect() {
    const parsed = new URL(this.url)
    const key = Buffer.from(randomUUID().replace(/-/g, '')).toString('base64')
    const { connect } = await import('node:net')
    const socket = await new Promise((resolvePromise, reject) => {
      const s = connect(Number(parsed.port), parsed.hostname, () => resolvePromise(s))
      s.on('error', reject)
    })
    this.socket = socket

    // The upgrade response must be consumed before any frame parsing starts, otherwise the
    // HTTP header block would be interpreted as WebSocket frame data.
    await new Promise((resolvePromise, reject) => {
      let handshake = Buffer.alloc(0)
      const onHandshake = (chunk) => {
        handshake = Buffer.concat([handshake, chunk])
        const end = handshake.indexOf('\r\n\r\n')
        if (end < 0) return
        const head = handshake.subarray(0, end).toString('latin1')
        if (!/101/.test(head)) {
          reject(new Error(`websocket upgrade refused: ${head.split('\r\n')[0]}`))
          return
        }
        socket.off('data', onHandshake)
        const rest = handshake.subarray(end + 4)
        socket.on('data', (data) => this.onData(data))
        if (rest.length) this.onData(rest)
        resolvePromise()
      }
      socket.on('data', onHandshake)
      socket.on('error', reject)
      const request =
        `GET ${parsed.pathname}${parsed.search} HTTP/1.1\r\n` +
        `Host: ${parsed.host}\r\n` +
        'Upgrade: websocket\r\nConnection: Upgrade\r\n' +
        `Sec-WebSocket-Key: ${key}\r\nSec-WebSocket-Version: 13\r\n\r\n`
      socket.write(request)
    })
    return this
  }

  onData(chunk) {
    this.buffer = Buffer.concat([this.buffer, chunk])
    while (this.buffer.length >= 2) {
      const first = this.buffer[0]
      const second = this.buffer[1]
      const opcode = first & 0x0f
      let length = second & 0x7f
      let offset = 2
      if (length === 126) {
        if (this.buffer.length < 4) return
        length = this.buffer.readUInt16BE(2)
        offset = 4
      } else if (length === 127) {
        if (this.buffer.length < 10) return
        length = Number(this.buffer.readBigUInt64BE(2))
        offset = 10
      }
      if (this.buffer.length < offset + length) return
      const payload = this.buffer.subarray(offset, offset + length)
      this.buffer = this.buffer.subarray(offset + length)
      if (opcode === 0x9) {
        this.sendFrame(0xa, payload)
        continue
      }
      if (opcode === 0x0) {
        this.fragments.push(payload)
        if (first & 0x80) {
          this.dispatch(Buffer.concat(this.fragments).toString('utf8'))
          this.fragments = []
        }
        continue
      }
      if (!(first & 0x80)) {
        this.fragments = [payload]
        this.fragmentOpcode = opcode
        continue
      }
      this.dispatch(payload.toString('utf8'))
    }
  }

  dispatch(text) {
    let message
    try {
      message = JSON.parse(text)
    } catch {
      return
    }
    if (message.id && this.handlers.has(message.id)) {
      const { resolvePromise, reject } = this.handlers.get(message.id)
      this.handlers.delete(message.id)
      if (message.error) reject(new Error(JSON.stringify(message.error)))
      else resolvePromise(message.result)
    }
  }

  sendFrame(opcode, payload) {
    const mask = Buffer.from([1, 2, 3, 4])
    const length = payload.length
    let header
    if (length < 126) {
      header = Buffer.from([0x80 | opcode, 0x80 | length])
    } else if (length < 65536) {
      header = Buffer.alloc(4)
      header[0] = 0x80 | opcode
      header[1] = 0x80 | 126
      header.writeUInt16BE(length, 2)
    } else {
      header = Buffer.alloc(10)
      header[0] = 0x80 | opcode
      header[1] = 0x80 | 127
      header.writeBigUInt64BE(BigInt(length), 2)
    }
    const masked = Buffer.alloc(length)
    for (let i = 0; i < length; i++) masked[i] = payload[i] ^ mask[i % 4]
    this.socket.write(Buffer.concat([header, mask, masked]))
  }

  send(method, params = {}, sessionId) {
    const id = this.nextId++
    const payload = JSON.stringify({ id, method, params, sessionId })
    return new Promise((resolvePromise, reject) => {
      this.handlers.set(id, { resolvePromise, reject })
      this.sendFrame(0x1, Buffer.from(payload, 'utf8'))
      setTimeout(() => {
        if (this.handlers.has(id)) {
          this.handlers.delete(id)
          reject(new Error(`timeout calling ${method}`))
        }
      }, 30000)
    })
  }

  close() {
    try {
      this.socket.destroy()
    } catch {
      /* ignore */
    }
  }
}

const sleep = (ms) => new Promise((resolvePromise) => setTimeout(resolvePromise, ms))

function argOf(name, fallback) {
  const index = process.argv.indexOf(`--${name}`)
  return index >= 0 && process.argv[index + 1] ? process.argv[index + 1] : fallback
}

const base = argOf('base', 'http://localhost:8080')
const outDir = resolve(argOf('out', 'docs/screenshots'))
const width = Number(argOf('width', '1560'))
const height = Number(argOf('height', '1100'))
const routes = argOf(
  'routes',
  '/,/architecture,/tasks,/files,/okr,/operations,/collaboration,/meetings,/inbox,' +
    '/toolbox/documents,/toolbox/presentations,/toolbox/repos,/toolbox/environments,/toolbox/files,' +
    '/identity/users,/identity/policies,/identity/audit'
).split(',')

const chromeCandidates = [
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
  'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe'
]
const chrome = chromeCandidates.find((candidate) => existsSync(candidate))
if (!chrome) {
  console.error('no Chrome or Edge binary found')
  process.exit(1)
}

mkdirSync(outDir, { recursive: true })
const profile = join(tmpdir(), `stc-cdp-${randomUUID().replace(/-/g, '')}`)
mkdirSync(profile, { recursive: true })
const port = 9333

const child = spawn(
  chrome,
  [
    '--headless=new',
    '--disable-gpu',
    '--hide-scrollbars',
    '--no-first-run',
    '--no-default-browser-check',
    `--remote-debugging-port=${port}`,
    `--user-data-dir=${profile}`,
    `--window-size=${width},${height}`,
    'about:blank'
  ],
  { stdio: 'ignore' }
)

async function waitForEndpoint() {
  for (let attempt = 0; attempt < 60; attempt++) {
    try {
      const response = await fetch(`http://127.0.0.1:${port}/json/version`)
      if (response.ok) return response.json()
    } catch {
      /* not ready yet */
    }
    await sleep(400)
  }
  throw new Error('Chrome DevTools endpoint did not start')
}

const version = await waitForEndpoint()
const ws = await new Ws(version.webSocketDebuggerUrl).connect()

// ---------------------------------------------------------------- sign in for a token
const loginResponse = await fetch(`${base}/api/auth/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'lead', password: 'Lead@123' })
})
if (!loginResponse.ok) throw new Error(`login failed: ${loginResponse.status}`)
const session = await loginResponse.json()

const { targetId } = await ws.send('Target.createTarget', { url: 'about:blank' })
const { sessionId } = await ws.send('Target.attachToTarget', { targetId, flatten: true })
const call = (method, params) => ws.send(method, params, sessionId)

await call('Page.enable')
await call('Runtime.enable')
await call('Emulation.setDeviceMetricsOverride', {
  width,
  height,
  deviceScaleFactor: 1,
  mobile: false
})

await call('Page.navigate', { url: `${base}/login` })
await sleep(2500)

// seed the session in localStorage of the application origin
await call('Runtime.evaluate', {
  expression: `localStorage.setItem('stc.token', ${JSON.stringify(session.token)});
    localStorage.setItem('stc.user', ${JSON.stringify(JSON.stringify(session.user))});
    'seeded'`
})

const captured = []
const hashes = new Map()

for (const route of routes) {
  const name = route === '/' ? 'overview' : route.replace(/^\//, '').replace(/\//g, '-')
  await call('Page.navigate', { url: `${base}${route}` })
  await sleep(3200)
  const { data } = await call('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true })
  const buffer = Buffer.from(data, 'base64')
  const hash = createHash('sha256').update(buffer).digest('hex')
  const file = join(outDir, `${name}.png`)
  writeFileSync(file, buffer)
  const duplicate = hashes.has(hash)
  hashes.set(hash, name)
  captured.push({ route, file, bytes: buffer.length, duplicateOf: duplicate ?? null })
  console.log(
    `${duplicate ? 'DUPLICATE' : 'captured '} ${route.padEnd(30)} ${buffer.length} bytes` +
      (duplicate ? ` (same as ${duplicate})` : '')
  )
}

ws.close()
child.kill()
await sleep(600)
try {
  rmSync(profile, { recursive: true, force: true })
} catch {
  /* the profile directory is temporary */
}

const unique = new Set(captured.map((item) => item.bytes)).size
console.log(`\n${captured.length} screenshots, ${unique} distinct byte sizes, written to ${outDir}`)
