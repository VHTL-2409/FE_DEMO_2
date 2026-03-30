/**
 * Script: Replace Material Symbols icons with LucideIcon component
 * Run: node replace-icons.js
 */
import { readFileSync, writeFileSync, readdirSync, statSync } from 'fs'
import { join, relative, extname } from 'path'
import { fileURLToPath } from 'url'
import { dirname } from 'path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const srcDir = join(__dirname, 'src')

const SIZE_MAP = {
  'text-xs': 12,
  'text-sm': 14,
  'text-base': 16,
  'text-lg': 18,
  'text-xl': 20,
  'text-2xl': 24,
  'text-3xl': 30,
  'text-4xl': 36,
}

const skippedFiles = []
const updatedFiles = []
const errors = []

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

  const hasIcons = content.includes('material-symbols-outlined')
  const isVueFile = filePath.endsWith('.vue')
  const hasImport = content.includes("import LucideIcon")
  const importPath = getImportPath(filePath)

  // Add LucideIcon import for .vue files that have icons but no import
  if (isVueFile && hasIcons && !hasImport) {
    if (content.includes('<script setup>')) {
      content = content.replace(
        /(<script setup>[\s\S]*?<\/script>)/,
        (match) => match + '\nimport LucideIcon from \'' + importPath + '\''
      )
    } else if (content.includes('<script>')) {
      content = content.replace(
        /(<script>[\s\S]*?<\/script>)/,
        (match) => match + '\nimport LucideIcon from \'' + importPath + '\''
      )
    } else {
      content = content.replace(
        /(<template>)/,
        '<script setup>\nimport LucideIcon from \'' + importPath + '\'\n</script>\n\n$1'
      )
    }
  }

  // Remove Material Symbols CDN link from index.html
  content = content.replace(
    /<link[^>]*Material\+Symbols[^>]*>/g,
    ''
  )

  // Replace Material Symbols class definitions in CSS
  // We keep the class for backward compatibility, just add font-display
  content = content.replace(
    /(\.material-symbols-outlined \{[^}]*font-variation-settings:[^}]*\})/,
    "$1\n  font-display: swap;"
  )

  // Replace <span class="material-symbols-outlined">icon</span>
  // with <LucideIcon name="icon" />
  // Handle various patterns:
  // 1. <span class="material-symbols-outlined">icon</span>
  content = content.replace(
    /<span class="material-symbols-outlined">([a-z0-9_]+)<\/span>/gi,
    (match, icon) => `<LucideIcon name="${icon}" />`
  )

  // 2. <span class="material-symbols-outlined text-xl">icon</span>
  content = content.replace(
    /<span class="material-symbols-outlined([^>]*)>([a-z0-9_]+)<\/span>/gi,
    (match, classes, icon) => {
      // Extract size class
      let size = 20 // default
      for (const [cls, px] of Object.entries(SIZE_MAP)) {
        if (classes.includes(cls)) {
          size = px
          break
        }
      }
      // Extract arbitrary Tailwind value like text-[0.875rem] or text-[14px]
      const arbMatch = classes.match(/text-\[([\d.]+)(rem|em|px)?\]/)
      if (arbMatch) {
        const val = parseFloat(arbMatch[1])
        const unit = arbMatch[2] || 'px'
        size = unit === 'rem' ? Math.round(val * 16) : val
      }
      // Extract inline style font-size
      const styleMatch = classes.match(/style\s*=\s*["']([^"']*)["']/)
      if (styleMatch) {
        const fontMatch = styleMatch[1].match(/font-size:\s*([\d.]+)(px|rem)?/)
        if (fontMatch) {
          const val = parseFloat(fontMatch[1])
          const unit = fontMatch[2] || 'px'
          size = unit === 'rem' ? Math.round(val * 16) : val
        }
      }
      return `<LucideIcon name="${icon}"${size !== 20 ? ` size="${size}"` : ''} />`
    }
  )

  // 3. Handle Vue dynamic bindings: <span class="material-symbols-outlined">{{ icon }}</span>
  content = content.replace(
    /<span class="material-symbols-outlined([^>]*)>\{\{\s*([^{}]+)\s*\}\}<\/span>/gi,
    (match, classes, expr) => {
      // Strip 'this.' from expression like 'this.icon' -> 'icon'
      const cleanExpr = expr.replace(/^this\./, '').trim()
      let size = 20
      for (const [cls, px] of Object.entries(SIZE_MAP)) {
        if (classes.includes(cls)) {
          size = px
          break
        }
      }
      const arbMatch = classes.match(/text-\[([\d.]+)(rem|em|px)?\]/)
      if (arbMatch) {
        const val = parseFloat(arbMatch[1])
        const unit = arbMatch[2] || 'px'
        size = unit === 'rem' ? Math.round(val * 16) : val
      }
      const styleMatch = classes.match(/style\s*=\s*["']([^"']*)["']/)
      if (styleMatch) {
        const fontMatch = styleMatch[1].match(/font-size:\s*([\d.]+)(px|rem)?/)
        if (fontMatch) {
          const val = parseFloat(fontMatch[1])
          const unit = fontMatch[2] || 'px'
          size = unit === 'rem' ? Math.round(val * 16) : val
        }
      }
      const sizeAttr = size !== 20 ? ` size="${size}"` : ''
      // If expression is complex (contains ternary, etc), wrap in computed or use :name
      if (expr.includes('?')) {
        return `<LucideIcon :name="${cleanExpr}"${sizeAttr} />`
      }
      return `<LucideIcon :name="${cleanExpr}"${sizeAttr} />`
    }
  )

  // 4. Also handle standalone {{ icon }} in classes of spans
  content = content.replace(
    /<span([^>]*)class="([^"]*material-symbols-outlined[^"]*)"([^>]*)><\/span>/gi,
    (match, before, allClasses, after) => {
      // If there's no content, remove the span entirely or add a placeholder
      return match // Keep empty spans for now, they'll render nothing
    }
  )

  // Also handle SVG-based icons (for cases where Material Symbols has SVG variants)
  // Just in case, but mostly we handle <span> patterns above

  if (content !== original) {
    try {
      writeFileSync(filePath, content, 'utf-8')
      updatedFiles.push(relative(__dirname, filePath))
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
    } else if (extname(entry) === '.vue' || extname(entry) === '.html') {
      try {
        processVueFile(full)
      } catch (e) {
        errors.push(`${full}: ${e.message}`)
      }
    }
  }
}

walkDir(srcDir)

console.log(`\nUpdated: ${updatedFiles.length} files`)
console.log(`Errors: ${errors.length} files`)
if (updatedFiles.length > 0) {
  console.log('\nUpdated files:')
  updatedFiles.forEach(f => console.log(`  ${f}`))
}
if (errors.length > 0) {
  console.log('\nErrors:')
  errors.forEach(e => console.log(`  ${e}`))
}
