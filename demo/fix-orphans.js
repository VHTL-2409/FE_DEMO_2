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
  // Normalize CRLF -> LF
  const lines = raw.replace(/\r\n/g, '\n').replace(/\r/g, '\n').split('\n')
  let modified = false
  const out = []

  for (let i = 0; i < lines.length; i++) {
    const l = lines[i]
    const trimmed = l.trim()

    // Check for orphan selector: starts with . or #, no { or }, preceded by }
    if (/^[.#][\w-:]+$/.test(trimmed) && !trimmed.includes('{') && !trimmed.includes('}')) {
      const prevTrimmed = lines[i - 1] !== undefined ? lines[i - 1].trim() : ''
      const nextTrimmed = lines[i + 1] !== undefined ? lines[i + 1].trim() : ''

      if (
        prevTrimmed === '}' &&
        (
          nextTrimmed.startsWith('.') ||
          nextTrimmed.startsWith('#') ||
          nextTrimmed.startsWith('/*') ||
          nextTrimmed.startsWith('@') ||
          nextTrimmed.startsWith('</')
        )
      ) {
        modified = true
        continue // skip orphan line
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
