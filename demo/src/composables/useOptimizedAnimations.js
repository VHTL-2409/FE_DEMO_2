/**
 * Optimized Card Animation Composable
 * Uses Intersection Observer for viewport-based animation triggering
 * GPU-accelerated with minimal paint/reflow
 */
import { ref, onMounted, onUnmounted, computed } from 'vue'

export function useCardAnimation(options = {}) {
  const {
    threshold = 0.1,
    rootMargin = '0px 0px -30px 0px',
    animationDuration = 450,
    staggerDelay = 100,
    animationType = 'fadeUp' // 'fadeUp', 'fadeLeft', 'scale'
  } = options

  const cardRefs = ref([])
  const isVisible = ref(false)
  const containerRef = ref(null)
  let observer = null

  // Pre-compute animation class
  const getAnimationClass = computed(() => {
    switch (animationType) {
      case 'fadeLeft':
        return 'card-animate-left'
      case 'scale':
        return 'card-animate-scale'
      default:
        return 'card-animate-fade-up'
    }
  })

  // Calculate stagger delay for each card
  const getStaggerDelay = (index) => {
    return Math.min(index * staggerDelay, 600) // Cap at 600ms total
  }

  const setupObserver = () => {
    if (typeof window === 'undefined' || !containerRef.value) return

    observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0]
        if (entry.isIntersecting) {
          isVisible.value = true
          // Disconnect after first trigger
          if (observer) {
            observer.disconnect()
            observer = null
          }
        }
      },
      { threshold, rootMargin }
    )

    observer.observe(containerRef.value)
  }

  const setCardRef = (el, index) => {
    if (el) {
      cardRefs.value[index] = el
    }
  }

  onMounted(() => {
    setupObserver()
  })

  onUnmounted(() => {
    if (observer) {
      observer.disconnect()
      observer = null
    }
  })

  return {
    containerRef,
    cardRefs,
    isVisible,
    getAnimationClass,
    getStaggerDelay,
    setCardRef,
    animationDuration
  }
}

/**
 * Use Debounced Scroll for scroll-linked animations
 * Batched updates for better performance
 */
export function useDebouncedScroll(callback, delay = 16) {
  let timeout = null
  let lastCall = 0

  const handleScroll = () => {
    const now = Date.now()

    // Throttle to ~60fps
    if (now - lastCall < delay) {
      return
    }

    lastCall = now

    if (timeout) {
      clearTimeout(timeout)
    }

    timeout = setTimeout(() => {
      callback()
    }, delay)
  }

  const cleanup = () => {
    if (timeout) {
      clearTimeout(timeout)
      timeout = null
    }
  }

  return {
    handleScroll,
    cleanup
  }
}

/**
 * Memoize expensive calculations
 */
export function useMemo(fn, deps = []) {
  const cache = ref(null)
  const lastDeps = ref(null)

  const compute = () => {
    const depsChanged = !lastDeps.value ||
      deps.some((dep, i) => dep !== lastDeps.value[i])

    if (depsChanged) {
      cache.value = fn()
      lastDeps.value = deps
    }

    return cache.value
  }

  return compute
}
