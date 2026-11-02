/**
 * One-off refactoring helper: Spring Data JPA only discovers repository interfaces that are
 * top level, so the nested interfaces of the *Repositories holder classes are promoted to
 * top-level interfaces in the same package and every reference is rewritten.
 *
 * Usage: node scripts/promote-repos.mjs
 */
import { readFileSync, writeFileSync, rmSync } from 'node:fs'
import { join, dirname } from 'node:path'

const root = process.argv[2] ?? process.cwd()

const targets = [
  { file: 'backend/src/main/java/com/sstc/platform/identity/repo/IdentityRepositories.java', pkg: 'com.sstc.platform.identity.repo', out: 'IdentityRepos.java' },
  { file: 'backend/src/main/java/com/sstc/platform/dashboard/repo/DashboardRepositories.java', pkg: 'com.sstc.platform.dashboard.repo', out: 'DashboardRepos.java' },
  { file: 'backend/src/main/java/com/sstc/platform/collaboration/repo/CollaborationRepositories.java', pkg: 'com.sstc.platform.collaboration.repo', out: 'CollaborationRepos.java' },
  { file: 'backend/src/main/java/com/sstc/platform/toolbox/repo/ToolboxRepositories.java', pkg: 'com.sstc.platform.toolbox.repo', out: 'ToolboxRepos.java' }
]

const renameMap = new Map()

for (const target of targets) {
  const path = join(root, target.file)
  const source = readFileSync(path, 'utf8')
  const lines = source.split(/\r?\n/)

  const imports = []
  let bodyStart = -1
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    if (line.startsWith('import ')) imports.push(line)
    if (line.includes('public final class')) {
      bodyStart = i + 1
      break
    }
  }
  if (bodyStart < 0) throw new Error(`no holder class in ${target.file}`)

  const body = lines
    .slice(bodyStart, lines.length - 1)
    .join('\n')
    .replace(/^  public interface /gm, 'public interface ')

  const innerNames = [...body.matchAll(/public interface (\w+)/g)].map((match) => match[1])
  let renamed = body
  for (const name of innerNames) {
    renamed = renamed.split(`interface ${name} `).join(`interface ${name}Repo `)
    renameMap.set(`${holder}.${name}`, `${name}Repo`)
  }
  // the holder class body ends with the closing brace of the class, drop it
  const trimmedBody = renamed.replace(/\}\s*$/, '').trim()

  const kept = imports.filter(
    (line) => !line.includes('JpaRepository') && !line.includes('Pageable') && !line.includes('Query') && !line.includes('Param')
  )
  const jpaImports = imports.filter((line) => line.includes('JpaRepository'))
  const extraImports = imports.filter(
    (line) => line.includes('Pageable') || line.includes('Query') || line.includes('Param')
  )

  const output = [
    `package ${target.pkg};`,
    '',
    ...kept,
    ...jpaImports,
    ...extraImports,
    '',
    `/**`,
    ` * Persistence accessors of the sub-system. Each interface is a top level Spring Data`,
    ` * repository so the container can proxy it (they were nested inside a holder class in the`,
    ` * first draft of the project).`,
    ` */`,
    trimmedBody,
    ''
  ].join('\n')

  writeFileSync(join(dirname(path), target.out), output, 'utf8')
  rmSync(path)
  console.log(`promoted ${target.file} -> ${target.out}`)
}

// rewrite every reference of the old nested names
const rewrites = [...renameMap.entries()].sort((a, b) => b[0].length - a[0].length)
const { readdirSync, statSync } = await import('node:fs')

function walk(dir) {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry)
    if (statSync(full).isDirectory()) {
      walk(full)
    } else if (full.endsWith('.java')) {
      let text = readFileSync(full, 'utf8')
      const original = text
      for (const [from, to] of rewrites) {
        text = text.split(from).join(to)
      }
      text = text.replace(/^import com\.sstc\.platform\.(\w+)\.repo\.\w+Repositories;\n/gm, (match, module) => {
        const out = targets.find((target) => target.pkg.includes(`.${module}.`))
        return out ? `import ${out.pkg}.${out.out.replace('.java', '')};\n` : match
      })
      // remove now unused holder imports
      text = text.replace(/^import com\.sstc\.platform\.(\w+)\.repo\.\w+Repositories;\n/gm, '')
      if (text !== original) {
        writeFileSync(full, text, 'utf8')
        console.log(`rewritten ${full}`)
      }
    }
  }
}

walk(join(root, 'backend/src/main/java'))
