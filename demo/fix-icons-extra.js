/**
 * Fix remaining Material Symbols icons with extra classes
 */
import { readFileSync, writeFileSync, readdirSync, statSync } from 'fs'
import { join, relative, extname } from 'path'
import { fileURLToPath } from 'url'
import { dirname } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const srcDir = join(__dirname, 'src')

const FILES_TO_FIX = {
  // ProfessionalInfoForm.vue
  'components\\teacher\\profile\\ProfessionalInfoForm.vue': [
    { pattern: '<span class="prf__field-icon material-symbols-outlined">psychology</span>', replacement: '<LucideIcon name="brain" class="prf__field-icon" />' },
    { pattern: '<span class="prf__field-icon material-symbols-outlined">groups</span>', replacement: '<LucideIcon name="users" class="prf__field-icon" />' },
    { pattern: '<span class="prf__field-icon material-symbols-outlined">domain</span>', replacement: '<LucideIcon name="building" class="prf__field-icon" />' },
    { pattern: '<span class="prf__field-icon material-symbols-outlined">tag</span>', replacement: '<LucideIcon name="tag" class="prf__field-icon" />' },
    { pattern: '<span class="prf__field-icon material-symbols-outlined">calendar_month</span>', replacement: '<LucideIcon name="calendar" class="prf__field-icon" />' },
    { pattern: '<span class="prf__field-icon material-symbols-outlined">notes</span>', replacement: '<LucideIcon name="file-text" class="prf__field-icon" />' },
    { pattern: '<span v-if="isSaving" class="material-symbols-outlined prf__spinner">progress_activity</span>', replacement: '<LucideIcon v-if="isSaving" name="loader-2" class="prf__spinner animate-spin" />' },
  ],
  // PersonalInfoForm.vue
  'components\\teacher\\profile\\PersonalInfoForm.vue': [
    { pattern: '<span class="pif__field-icon material-symbols-outlined">badge</span>', replacement: '<LucideIcon name="badge" class="pif__field-icon" />' },
    { pattern: '<span class="pif__field-icon material-symbols-outlined">label</span>', replacement: '<LucideIcon name="tag" class="pif__field-icon" />' },
    { pattern: '<span class="pif__field-icon material-symbols-outlined">person</span>', replacement: '<LucideIcon name="user" class="pif__field-icon" />' },
    { pattern: '<span class="pif__field-icon material-symbols-outlined">cake</span>', replacement: '<LucideIcon name="cake" class="pif__field-icon" />' },
    { pattern: '<span class="pif__field-icon material-symbols-outlined">mail</span>', replacement: '<LucideIcon name="mail" class="pif__field-icon" />' },
    { pattern: '<span class="pif__field-icon material-symbols-outlined">phone</span>', replacement: '<LucideIcon name="phone" class="pif__field-icon" />' },
    { pattern: '<span v-if="isSaving" class="material-symbols-outlined pif__spinner">progress_activity</span>', replacement: '<LucideIcon v-if="isSaving" name="loader-2" class="pif__spinner animate-spin" />' },
  ],
  // SecuritySettings.vue
  'components\\teacher\\profile\\SecuritySettings.vue': [
    { pattern: '<span v-if="isSavingPassword" class="material-symbols-outlined ss__spinner">progress_activity</span>', replacement: '<LucideIcon v-if="isSavingPassword" name="loader-2" class="ss__spinner animate-spin" />' },
  ],
}

const fixedFiles = []
const errors = []

for (const [file, fixes] of Object.entries(FILES_TO_FIX)) {
  const filePath = join(srcDir, file)
  try {
    let content = readFileSync(filePath, 'utf-8')
    const original = content
    for (const fix of fixes) {
      if (content.includes(fix.pattern)) {
        content = content.split(fix.pattern).join(fix.replacement)
      }
    }
    if (content !== original) {
      writeFileSync(filePath, content, 'utf-8')
      fixedFiles.push(file)
    }
  } catch (e) {
    errors.push(`${file}: ${e.message}`)
  }
}

console.log(`\nFixed: ${fixedFiles.length} files`)
console.log(`Errors: ${errors.length} files`)
if (fixedFiles.length > 0) {
  fixedFiles.forEach(f => console.log(`  ${f}`))
}
if (errors.length > 0) {
  errors.forEach(e => console.log(`  Error: ${e}`))
}
