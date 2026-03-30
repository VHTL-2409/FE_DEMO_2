/**
 * Remove bad LucideIcon imports and fix duplicates
 */
import { readFileSync, writeFileSync, readdirSync, statSync } from 'fs'
import { join, relative, extname } from 'path'
import { fileURLToPath } from 'url'
import { dirname } from 'path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const srcDir = join(__dirname, 'src')

const fixedFiles = []
const errors = []

function processVueFile(filePath) {
  let content = readFileSync(filePath, 'utf-8')
  const original = content

  // Remove bad imports: import LucideIcon from 'LucideIcon' (empty path)
  content = content.replace(
    /import LucideIcon from 'LucideIcon'\n?/g,
    ''
  )

  // Remove duplicate LucideIcon imports (keep only first)
  const importMatches = [...content.matchAll(/import LucideIcon from '[^']*'/g)]
  if (importMatches.length > 1) {
    // Keep first occurrence, remove rest
    const first = importMatches[0]
    const rest = content.slice(first.index + first[0].length)
    const restClean = rest.replace(/import LucideIcon from '[^']*'\n?/g, '')
    content = content.slice(0, first.index + first[0].length) + restClean
  }

  if (content !== original) {
    try {
      writeFileSync(filePath, content, 'utf-8')
      fixedFiles.push(relative(__dirname, filePath))
    } catch (e) {
      errors.push(`${filePath}: ${e.message}`)
    }
  }
}

function walkDir(dir) {
  const entries = readdirSync(dir)
  for (const entry of entries) {
    const full = join(dir, entry)
    const stat = statSync(full)
    if (stat.isDirectory()) {
      walkDir(full)
    } else if (extname(entry) === '.vue') {
      try {
        processVueFile(full)
      } catch (e) {
        errors.push(`${full}: ${e.message}`)
      }
    }
  }
}

walkDir(srcDir)

console.log(`\nFixed: ${fixedFiles.length} files`)
console.log(`Errors: ${errors.length} files`)
if (errors.length > 0) {
  errors.forEach(e => console.log(`  Error: ${e}`))
}
