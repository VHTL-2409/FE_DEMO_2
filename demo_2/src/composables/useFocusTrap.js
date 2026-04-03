import { watch, onUnmounted, nextTick } from 'vue'

/**
 * @param {import('vue').Ref<HTMLElement | null>} containerRef
 * @param {import('vue').Ref<boolean>} isActive
 */
export function useFocusTrap(containerRef, isActive) {
  let previousActive = null
  let isTrapActive = false

  const getFocusable = (root) => {
    if (!root) return []
    const sel =
      'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
    return Array.from(root.querySelectorAll(sel)).filter((el) => {
      if (el.closest('[inert]')) return false
      const style = window.getComputedStyle(el)
      return style.visibility !== 'hidden' && style.display !== 'none'
    })
  }

  const handleKeydown = (e) => {
    if (e.key !== 'Tab' || !containerRef.value) return
    const nodes = getFocusable(containerRef.value)
    if (nodes.length === 0) return
    const first = nodes[0]
    const last = nodes[nodes.length - 1]
    if (e.shiftKey) {
      if (document.activeElement === first) {
        e.preventDefault()
        last.focus()
      }
    } else if (document.activeElement === last) {
      e.preventDefault()
      first.focus()
    }
  }

  const activate = () => {
    previousActive = document.activeElement
    document.addEventListener('keydown', handleKeydown)
    nextTick(() => {
      queueMicrotask(() => {
        const nodes = getFocusable(containerRef.value)
        if (nodes.length) {
          nodes[0].focus()
        }
      })
    })
    isTrapActive = true
  }

  const deactivate = () => {
    if (isTrapActive) {
      document.removeEventListener('keydown', handleKeydown)
    }
    isTrapActive = false
    if (previousActive && typeof previousActive.focus === 'function') {
      try {
        previousActive.focus()
      } catch {
        /* ignore */
      }
    }
    previousActive = null
  }

  watch(
    isActive,
    (v) => {
      if (v) activate()
      else deactivate()
    },
    { flush: 'post', immediate: true }
  )

  onUnmounted(() => {
    if (isTrapActive) {
      document.removeEventListener('keydown', handleKeydown)
    }
  })

  return { activate, deactivate }
}
