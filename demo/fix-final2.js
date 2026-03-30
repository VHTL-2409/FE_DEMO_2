/**
 * Final manual fixes for remaining Material Symbols icons - FIXED PATH HANDLING
 */
import { readFileSync, writeFileSync } from 'fs'
import { join } from 'path'

const srcDir = 'c:\\Users\\Administrator\\Desktop\\FE_DEMO\\demo\\src'

// File -> list of {from, to} replacements
const REPLACEMENTS = [
  // ExamStatusTimeline.vue
  [
    join(srcDir, 'components', 'teacher', 'exam', 'detail', 'ExamStatusTimeline.vue'),
    'name="check" v-if="milestone.done" material-symbols-outlined est__dot-icon',
    'name="check" v-if="milestone.done" class="est__dot-icon"'
  ],
  [
    join(srcDir, 'components', 'teacher', 'exam', 'detail', 'ExamStatusTimeline.vue'),
    'name="radio_button_checked" v-else-if="milestone.current" material-symbols-outlined est__dot-icon',
    'name="circle-dot" v-else-if="milestone.current" class="est__dot-icon"'
  ],
  [
    join(srcDir, 'components', 'teacher', 'exam', 'detail', 'ExamStatusTimeline.vue'),
    '<span v-else class="material-symbols-outlined est__dot-icon">{{ milestone.icon }}</span>',
    '<LucideIcon v-else :name="milestone.icon" class="est__dot-icon" />'
  ],

  // Sidebar.vue
  [
    join(srcDir, 'components', 'layout', 'Sidebar.vue'),
    '<span class="ds-sidebar-nav-icon material-symbols-outlined shrink-0"> {{ item.icon }}</span>',
    '<LucideIcon :name="item.icon" class="ds-sidebar-nav-icon shrink-0" />'
  ],
  [
    join(srcDir, 'components', 'layout', 'Sidebar.vue'),
    'name="chevron_left" material-symbols-outlined shrink-0 transition-transform',
    'name="chevron_left" class="shrink-0 transition-transform"'
  ],
  [
    join(srcDir, 'components', 'layout', 'Sidebar.vue'),
    'class="material-symbols-outlined ds-sidebar-overlay-icon"',
    'class="ds-sidebar-overlay-icon"'
  ],

  // StudentViolationDetailMonitoring.vue
  [
    join(srcDir, 'components', 'teacher', 'StudentViolationDetailMonitoring.vue'),
    '<span class="material-symbols-outlined text-lg" :style="{ color: getViolationColor(value) }">',
    '<LucideIcon :name="icon" size="20" :style="{ color: getViolationColor(value) }" />'
  ],
  [
    join(srcDir, 'components', 'teacher', 'StudentViolationDetailMonitoring.vue'),
    '<span class="material-symbols-outlined text-lg shrink-0 mt-0.5" :style="{ color: pattern.level',
    '<LucideIcon :name="pattern.icon" size="20" class="shrink-0 mt-0.5" :style="{ color: pattern.level'
  ],

  // LoginRoleSelection.vue
  [
    join(srcDir, 'components', 'login', 'LoginRoleSelection.vue'),
    'class="material-symbols-outlined text-3xl font-normal',
    'class="text-3xl font-normal"'
  ],

  // ToastHost.vue
  [
    join(srcDir, 'components', 'common', 'ToastHost.vue'),
    'class="material-symbols-outlined"',
    'class=""'
  ],
]

const fixed = []
const errors = []

for (let i = 0; i < REPLACEMENTS.length; i += 3) {
  const filePath = REPLACEMENTS[i]
  const from = REPLACEMENTS[i + 1]
  const to = REPLACEMENTS[i + 2]
  try {
    let content = readFileSync(filePath, 'utf-8')
    if (content.includes(from)) {
      content = content.split(from).join(to)
      writeFileSync(filePath, content, 'utf-8')
      fixed.push(filePath)
    }
  } catch (e) {
    errors.push(`${filePath}: ${e.message}`)
  }
}

console.log(`Fixed: ${fixed.length} files`)
console.log(`Errors: ${errors.length} files`)
if (errors.length) errors.forEach(e => console.log('  Error:', e))
if (fixed.length) fixed.forEach(f => console.log('  ', f))
