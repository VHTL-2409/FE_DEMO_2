/**
 * useScrollToTop — scroll the page to top on mount
 * Used to fix the issue where pages load in the middle of the viewport
 */
import { onMounted } from 'vue'

export function useScrollToTop(instant = true) {
  onMounted(() => {
    if (instant) {
      window.scrollTo({ top: 0, behavior: 'instant' })
    } else {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  })
}
