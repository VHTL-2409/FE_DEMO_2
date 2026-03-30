/**
 * Fix bad LucideIcon imports (empty 'LucideIcon' paths)
 */
import { readFileSync, writeFileSync, readdirSync, statSync } from 'fs'
import { join, relative, extname } from 'path'
import { fileURLToPath } from 'url'
import { dirname } from 'path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const srcDir = join(__dirname, 'src')

const fixedFiles = []

function getImportPath(filePath) {
  const srcComponents = filePath.indexOf('src/components')
  if (srcComponents === -1) return 'LucideIcon'
  const depth = filePath.slice(srcComponents + 'src/components'.length).split(/[/\\]/).filter(Boolean).length
  if (depth === 1) return './LucideIcon.vue'
  return '../'.repeat(depth - 1) + 'common/LucideIcon.vue'
}

function processVueFile(filePath) {
  let content = readFileSync(filePath, 'utf-8')
  const original = content
  const importPath = getImportPath(filePath)

  // Fix empty imports like: import LucideIcon from 'LucideIcon'
  content = content.replace(
    /import LucideIcon from 'LucideIcon'/g,
    `import LucideIcon from '${importPath}'`
  )

  // Fix duplicate imports - keep only the first one
  const matches = content.match(/import LucideIcon from/g)
  if (matches && matches.length > 1) {
    // Remove all but first occurrence of the import line
    let firstIndex = content.indexOf("import LucideIcon from '")
    const rest = content.slice(firstIndex + 27)
    const restClean = rest.replace(/import LucideIcon from '[^']*'\n?/g, '')
    content = content.slice(0, firstIndex + 27) + restClean
  }

  if (content !== original) {
    try {
      writeFileSync(filePath, content, 'utf-8')
      fixedFiles.push({ path: relative(__dirname, filePath), importPath })
    } catch (e) {
      console.error('Error:', filePath, e.message)
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
        console.error('Error:', full, e.message)
      }
    }
  }
}

walkDir(srcDir)

console.log(`\nFixed: ${fixedFiles.length} files`)
if (fixedFiles.length > 0) {
  fixedFiles.forEach(f => console.log(`  ${f.path} -> ${f.importPath}`))
}
