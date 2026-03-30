import { readFileSync, writeFileSync, readdirSync } from 'fs'
import { join, relative, extname } from 'path'
import { fileURLToPath } from 'url'
import { dirname } from 'path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const srcDir = join(__dirname, 'src')

function processFile(filePath) {
  let content = readFileSync(filePath, 'utf-8')
  const original = content

  // Remove material-symbols-outlined from LucideIcon in templates (stray attribute, not a real class needed)
  // Handles: <LucideIcon name="foo" material-symbols-outlined class="bar" />
  // and:     <LucideIcon name="foo" material-symbols-outlined />
  content = content.replace(
    /<LucideIcon(\s[^>]*?)material-symbols-outlined([^>]*?)\s*\/?>/g,
    (match, before, after) => {
      // Remove material-symbols-outlined from the class attribute if present
      const cleanedAfter = after.replace(/\s*material-symbols-outlined/, '')
      return `<LucideIcon${before}${cleanedAfter}/>`
    }
  )

  // Remove material-symbols-outlined as a standalone class attr value
  content = content.replace(/\s+material-symbols-outlined(?=\s)/g, '')

  // Remove CSS rules that reference .material-symbols-outlined
  // Match selector lines containing only material-symbols-outlined
  // e.g. "  .material-symbols-outlined { font-size: 20px; }"
  content = content.replace(
    /[ \t]+(?:\/\*[^*]*\*+(?:[^\/*][^*]*\*+)*\/[ \t]*)?\.material-symbols-outlined[^{]*\{[^}]*\}/g,
    ''
  )
  // Also handle multiline: first line is selector, rest is block
  content = content.replace(
    /[ \t]+\.material-symbols-outlined[ \t]*\n[ \t]*\{[^\}]*\}/g,
    ''
  )

  if (content !== original) {
    writeFileSync(filePath, content, 'utf-8')
    return true
  }
  return false
}

function walkDir(dir) {
  const results = []
  const entries = readdirSync(dir, { withFileTypes: true })
  for (const entry of entries) {
    const full = join(dir, entry.name)
    if (entry.isDirectory()) {
      results.push(...walkDir(full))
    } else if (entry.isFile() && extname(entry.name) === '.vue') {
      results.push(full)
    }
  }
  return results
}

const files = walkDir(srcDir)
const fixed = []
const errors = []

for (const file of files) {
  try {
    if (processFile(file)) {
      fixed.push(relative(srcDir, file))
    }
  } catch (e) {
    errors.push(`${relative(srcDir, file)}: ${e.message}`)
  }
}

console.log(`Fixed: ${fixed.length} files`)
if (fixed.length) fixed.forEach(f => console.log(' ', f))
console.log(`Errors: ${errors.length}`)
if (errors.length) errors.forEach(e => console.log(' ', e))
