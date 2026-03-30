const fs = require('fs')
const path = require('path')

function walk(dir, results = []) {
  const entries = fs.readdirSync(dir, { withFileTypes: true })
  for (const entry of entries) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      walk(full, results)
    } else if (entry.isFile() && entry.name.endsWith('.vue')) {
      results.push(full)
    }
  }
  return results
}

const files = walk('src')
let fixed = 0

for (const f of files) {
  const raw = fs.readFileSync(f, 'utf-8')
  const lines = raw.replace(/\r\n/g, '\n').replace(/\r/g, '\n').split('\n')
  let modified = false
  const out = []

  for (let i = 0; i < lines.length; i++) {
    const l = lines[i]
    const trimmed = l.trim()

    if (/^[.#][\w-:]+$/.test(trimmed) && !trimmed.includes('{') && !trimmed.includes('}')) {
      // Skip blank lines when looking back
      let prevNonEmptyIdx = i - 1
      while (prevNonEmptyIdx >= 0 && !lines[prevNonEmptyIdx].trim()) prevNonEmptyIdx--
      // Skip blank lines when looking forward
      let nextNonEmptyIdx = i + 1
      while (nextNonEmptyIdx < lines.length && !lines[nextNonEmptyIdx].trim()) nextNonEmptyIdx++
      const prevNonEmpty = prevNonEmptyIdx >= 0 ? lines[prevNonEmptyIdx].trim() : ''
      const nextNonEmpty = nextNonEmptyIdx < lines.length ? lines[nextNonEmptyIdx].trim() : ''

      // Orphan if: prev non-empty line ends with } (CSS block end) AND
      // next non-empty line is another selector or </style>
      const prevEndsWithCloseBrace = prevNonEmpty.endsWith('}')
      const nextIsSelector = nextNonEmpty.startsWith('.') || nextNonEmpty.startsWith('#') || nextNonEmpty.startsWith('/*') || nextNonEmpty.startsWith('@') || nextNonEmpty.startsWith('</')

      if (prevEndsWithCloseBrace && nextIsSelector) {
        modified = true
        continue
      }
    }

    out.push(l)
  }

  if (modified) {
    fs.writeFileSync(f, out.join('\n'), 'utf-8')
    fixed++
    console.log('Fixed:', f.replace(/\\/g, '/').replace('C:/Users/Administrator/Desktop/FE_DEMO/demo/', ''))
  }
}

console.log('Total files fixed:', fixed)
