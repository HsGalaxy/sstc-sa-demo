/**
 * One-off refactoring helper: rewrite the old nested-repository references
 * (`XxxRepositories.Yyy`) to the promoted top level interfaces (`YyyRepo`) and drop the
 * now unused imports. Spring Data JPA only proxies top level interfaces, which is why the
 * nested form had to be promoted.
 *
 * Usage: node scripts/rewrite-repo-refs.mjs <project-root>
 */
import { readFileSync, writeFileSync, readdirSync, statSync } from 'node:fs'
import { join } from 'node:path'

const root = process.argv[2] ?? process.cwd()

const modules = ['identity', 'dashboard', 'collaboration', 'toolbox']

/** holder class name -> [inner interface name] */
const holders = {
  IdentityRepositories: [
    'Users',
    'Roles',
    'Permissions',
    'RolePermissions',
    'UserRoles',
    'Policies',
    'AccountEvents',
    'AccessAudits',
    'SessionTokens'
  ],
  DashboardRepositories: [
    'Tasks',
    'Cycles',
    'Okrs',
    'KeyResults',
    'FileRecords',
    'FileVersions',
    'OperationLogs',
    'Snapshots'
  ],
  CollaborationRepositories: [
    'Channels',
    'ChannelMembers',
    'Messages',
    'Mentions',
    'Notifications',
    'DigestItems',
    'Meetings',
    'Transcripts',
    'Summaries'
  ],
  ToolboxRepositories: [
    'GitRepos',
    'Commits',
    'Environments',
    'Documents',
    'DocEdits',
    'Presentations',
    'Slides',
    'Files',
    'ShareLinks'
  ]
}

const rewrites = []
for (const [holder, innerNames] of Object.entries(holders)) {
  for (const name of innerNames) {
    rewrites.push([`${holder}.${name}`, `${name}Repo`])
  }
}
rewrites.sort((a, b) => b[0].length - a[0].length)

function walk(dir) {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry)
    if (statSync(full).isDirectory()) {
      walk(full)
      continue
    }
    if (!full.endsWith('.java')) continue
    let text = readFileSync(full, 'utf8')
    const original = text
    for (const [from, to] of rewrites) {
      text = text.split(from).join(to)
    }
    // drop imports of the removed holder classes and add the module wildcard import
    const holderImports = [...text.matchAll(/^import com\.sstc\.platform\.(\w+)\.repo\.\w+Repositories;\n/gm)]
    if (holderImports.length > 0) {
      const moduleNames = new Set(holderImports.map((match) => match[1]))
      text = text.replace(/^import com\.sstc\.platform\.(\w+)\.repo\.\w+Repositories;\n/gm, '')
      for (const moduleName of moduleNames) {
        if (!modules.includes(moduleName)) continue
        const wildcard = `import com.sstc.platform.${moduleName}.repo.*;\n`
        if (!text.includes(wildcard)) {
          const firstImport = text.indexOf('import ')
          const insertAt = firstImport >= 0 ? text.lastIndexOf('\n', text.lastIndexOf('\n', firstImport) - 1) + 1 : 0
          text = `${text.slice(0, insertAt)}${wildcard}${text.slice(insertAt)}`
        }
      }
    }
    if (text !== original) {
      writeFileSync(full, text, 'utf8')
      console.log(`rewritten ${full}`)
    }
  }
}

walk(join(root, 'backend/src/main/java'))
