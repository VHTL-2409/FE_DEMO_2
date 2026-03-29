import { ref } from 'vue'

const STORAGE_KEY = 'demo2-nav-label-mode'
const VALID = ['icons', 'short', 'full']

/** Singleton — học sinh / giáo viên dùng chung một chế độ */
const mode = ref('full')

if (typeof localStorage !== 'undefined') {
  const s = localStorage.getItem(STORAGE_KEY)
  if (VALID.includes(s)) mode.value = s
}

/**
 * Chế độ hiển thị nhãn menu sidebar: icons | short | full (persist localStorage)
 */
export function useNavLabelMode() {
  const setMode = (m) => {
    if (!VALID.includes(m)) return
    mode.value = m
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(STORAGE_KEY, m)
    }
  }

  const labelFor = (item) => {
    if (!item) return ''
    if (mode.value === 'icons') return ''
    if (mode.value === 'short') return item.short ?? item.label
    return item.label
  }

  return { mode, setMode, labelFor, VALID }
}
