/**
 * Intersection Observer Composable
 * Lazy animation triggers - only animate when element enters viewport
 * Optimized for performance with minimal re-renders
 */
import { ref, onMounted, onUnmounted, watch } from 'vue'

export function useIntersectionObserver(options = {}) {
  const isVisible = ref(false)
  const hasAnimated = ref(false)
  const elementRef = ref(null)
  const containerRef = elementRef // Alias for backwards compatibility
  let observer = null

  const defaultOptions = {
    threshold: 0.1,
    rootMargin: '0px 0px -50px 0px', // Trigger slightly before element enters
    once: true // Only animate once
  }

  const mergedOptions = { ...defaultOptions, ...options }

  const handleIntersection = (entries) => {
    const entry = entries[0]
    if (entry.isIntersecting) {
      isVisible.value = true

      // Mark as animated to prevent re-animation
      if (mergedOptions.once) {
        hasAnimated.value = true
        // Disconnect observer after first trigger
        if (observer) {
          observer.disconnect()
          observer = null
        }
      }
    } else if (!mergedOptions.once) {
      isVisible.value = false
    }
  }

  const setupObserver = () => {
    if (typeof window === 'undefined') return

    observer = new IntersectionObserver(handleIntersection, {
      threshold: mergedOptions.threshold,
      rootMargin: mergedOptions.rootMargin
    })

    if (elementRef.value) {
      observer.observe(elementRef.value)
    }
  }

  const cleanup = () => {
    if (observer) {
      observer.disconnect()
      observer = null
    }
  }

  // Watch for ref changes
  watch(elementRef, (newRef, oldRef) => {
    if (oldRef && observer) {
      observer.unobserve(oldRef)
    }
    if (newRef) {
      setupObserver()
    }
  })

  onMounted(() => {
    setupObserver()
  })

  onUnmounted(() => {
    cleanup()
  })

  return {
    elementRef,
    containerRef,
    isVisible,
    hasAnimated
  }
}

/**
 * Staggered animation helper
 * Returns delay based on index for cascading animations
 */
export function useStaggeredAnimation(itemCount = 10, baseDelay = 50, maxDelay = 500) {
  const getDelay = (index) => {
    const delay = baseDelay * index
    return Math.min(delay, maxDelay)
  }

  return { getDelay }
}
