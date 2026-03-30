/**
 * Fix ALL remaining Material Symbols spans across all Vue files
 * Finds: <span class="[...material-symbols-outlined...]">ICON</span> -> <LucideIcon name="ICON" />
 */
import { readFileSync, writeFileSync, readdirSync, statSync } from 'fs'
import { join, relative, extname } from 'path'
import { fileURLToPath } from 'url'
import { dirname } from 'path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const srcDir = join(__dirname, 'src')

const fixedFiles = []
const totalFixed = []
const errors = []

function processVueFile(filePath) {
  let content = readFileSync(filePath, 'utf-8')
  const original = content
  let count = 0

  // Pattern 1: <span class="...material-symbols-outlined...">icon</span>
  // Supports multiline
  content = content.replace(
    /<span([^>]*)class="([^"]*material-symbols-outlined[^"]*)"([^>]*)>([^<]+)<\/span>/g,
    (match, before, allClasses, after, icon) => {
      const cleanIcon = icon.trim()
      if (!cleanIcon || cleanIcon.startsWith('{{')) return match
      count++
      // Preserve parent classes by applying them to LucideIcon
      return `<LucideIcon name="${cleanIcon}"${before}${allClasses}${after} />`.replace(/\s*\/>/g, ' />')
    }
  )

  // Pattern 2: <span class="material-symbols-outlined">icon</span> (inline)
  content = content.replace(
    /<span class="material-symbols-outlined">([a-z0-9_]+)<\/span>/gi,
    (match, icon) => {
      count++
      return `<LucideIcon name="${icon}" />`
    }
  )

  // Pattern 3: <span class="material-symbols-outlined ...">icon</span> (with other classes)
  content = content.replace(
    /<span class="material-symbols-outlined([^"]*)">([a-z0-9_]+)<\/span>/gi,
    (match, extra, icon) => {
      count++
      return `<LucideIcon name="${icon}"${extra ? ' class="' + extra.trim() + '"' : ''} />`
    }
  )

  if (count > 0) {
    try {
      writeFileSync(filePath, content, 'utf-8')
      fixedFiles.push(relative(__dirname, filePath))
      totalFixed.push(count)
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

const total = totalFixed.reduce((a, b) => a + b, 0)
console.log(`\nFixed: ${fixedFiles.length} files (${total} replacements total)`)
console.log(`Errors: ${errors.length} files`)
if (fixedFiles.length > 0) {
  fixedFiles.slice(0, 30).forEach((f, i) => console.log(`  [${totalFixed[i]}] ${f}`))
  if (fixedFiles.length > 30) console.log(`  ... and ${fixedFiles.length - 30} more`)
}
if (errors.length > 0) {
  errors.forEach(e => console.log(`  Error: ${e}`))
}
