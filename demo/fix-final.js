/**
 * Final manual fixes for remaining Material Symbols icons
 */
import { readFileSync, writeFileSync, readdirSync, statSync } from 'fs'
import { join, relative, extname } from 'path'
import { fileURLToPath } from 'url'
import { dirname } from 'path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const srcDir = join(__dirname, 'src')

// File -> list of {from, to} replacements
const REPLACEMENTS = [
  // ExamStatusTimeline.vue
  ['components/teacher/exam/detail/ExamStatusTimeline.vue',
    'name="check" v-if="milestone.done" material-symbols-outlined est__dot-icon',
    'name="check" v-if="milestone.done" class="est__dot-icon"'
  ],
  ['components/teacher/exam/detail/ExamStatusTimeline.vue',
    'name="radio_button_checked" v-else-if="milestone.current" material-symbols-outlined est__dot-icon',
    'name="circle-dot" v-else-if="milestone.current" class="est__dot-icon"'
  ],
  ['components/teacher/exam/detail/ExamStatusTimeline.vue',
    '<span v-else class="material-symbols-outlined est__dot-icon">{{ milestone.icon }}</span>',
    '<LucideIcon v-else :name="milestone.icon" class="est__dot-icon" />'
  ],

  // Sidebar.vue
  ['components/layout/Sidebar.vue',
    '<span class="ds-sidebar-nav-icon material-symbols-outlined shrink-0"> {{ item.icon }}</span>',
    '<LucideIcon :name="item.icon" class="ds-sidebar-nav-icon shrink-0" />'
  ],
  ['components/layout/Sidebar.vue',
    'name="chevron_left" material-symbols-outlined shrink-0 transition-transform',
    'name="chevron_left" class="shrink-0 transition-transform"'
  ],
  ['components/layout/Sidebar.vue',
    'class="material-symbols-outlined ds-sidebar-overlay-icon"',
    'class="ds-sidebar-overlay-icon"'
  ],

  // StudentViolationDetailMonitoring.vue
  ['components/teacher/StudentViolationDetailMonitoring.vue',
    '<span class="material-symbols-outlined text-lg" :style="{ color: getViolationColor(value) }">',
    '<LucideIcon :name="icon" size="20" :style="{ color: getViolationColor(value) }" />'
  ],
  ['components/teacher/StudentViolationDetailMonitoring.vue',
    '<span class="material-symbols-outlined text-lg shrink-0 mt-0.5" :style="{ color: pattern.level',
    '<LucideIcon :name="pattern.icon" size="20" class="shrink-0 mt-0.5" :style="{ color: pattern.level'
  ],

  // LoginRoleSelection.vue
  ['components/login/LoginRoleSelection.vue',
    'class="material-symbols-outlined text-3xl font-normal',
    'class="text-3xl font-normal"'
  ],

  // ToastHost.vue
  ['components/common/ToastHost.vue',
    'class="material-symbols-outlined"',
    'class=""'
  ],
]

const fixed = []
const errors = []

// Group by file
const byFile = {}
for (let i = 0; i < REPLACEMENTS.length; i += 3) {
  const file = REPLACEMENTS[i]
  const from = REPLACEMENTS[i + 1]
  const to = REPLACEMENTS[i + 2]
  if (!byFile[file]) byFile[file] = []
  byFile[file].push({ from, to })
}

for (const [file, fixes] of Object.entries(byFile)) {
  const filePath = join(srcDir, file)
  try {
    let content = readFileSync(filePath, 'utf-8')
    const orig = content
    for (const fix of fixes) {
      if (content.includes(fix.from)) {
        content = content.split(fix.from).join(fix.to)
      }
    }
    if (content !== orig) {
      writeFileSync(filePath, content, 'utf-8')
      fixed.push(file)
    }
  } catch (e) {
    errors.push(`${file}: ${e.message}`)
  }
}

console.log(`Fixed: ${fixed.length} files`)
console.log(`Errors: ${errors.length} files`)
if (errors.length) console.log(errors)
