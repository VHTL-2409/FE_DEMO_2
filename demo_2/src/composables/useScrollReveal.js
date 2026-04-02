/**
 * useScrollReveal — IntersectionObserver-based scroll animations
 * Adds class 'is-visible' when elements enter the viewport.
 * Supports both CSS class toggling and callback mode.
 */
import { onMounted, onUnmounted, ref } from 'vue'

export function useScrollReveal(options = {}) {
  let observer = null
  const { threshold = 0.1, rootMargin = '0px 0px -40px 0px' } = options

  const init = (selector = '.gs-scroll-reveal') => {
    if (typeof window === 'undefined') return

    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-visible')
            observer.unobserve(entry.target)
          }
        })
      },
      { threshold, rootMargin }
    )

    const elements = typeof document !== 'undefined'
      ? document.querySelectorAll(selector)
      : []
    elements.forEach((el) => observer.observe(el))
  }

  /** Observe a specific element manually */
  const observe = (el) => {
    if (observer && el) observer.observe(el)
  }

  /** Refresh observers (call after DOM changes) */
  const refresh = (selector = '.gs-scroll-reveal') => {
    if (observer) observer.disconnect()
    init(selector)
  }

  onUnmounted(() => {
    if (observer) {
      observer.disconnect()
      observer = null
    }
  })

  return { init, observe, refresh }
}

/**
 * useCountUp — animates a number from 0 to target
 */
export function useCountUp(target, duration = 2000, options = {}) {
  const { start = 0, decimals = 0, easing = true } = options
  const current = ref(start)
  let animationFrame = null

  const easeOutQuart = (t) => 1 - Math.pow(1 - t, 4)

  const start = () => {
    const startTime = performance.now()
    const animate = (now) => {
      const elapsed = now - startTime
      const progress = easing
        ? easeOutQuart(Math.min(elapsed / duration, 1))
        : Math.min(elapsed / duration, 1)
      current.value = Number((start + (target - start) * progress).toFixed(decimals))
      if (progress < 1) {
        animationFrame = requestAnimationFrame(animate)
      }
    }
    animationFrame = requestAnimationFrame(animate)
  }

  const stop = () => {
    if (animationFrame) {
      cancelAnimationFrame(animationFrame)
      animationFrame = null
    }
  }

  return { current, start, stop }
}
