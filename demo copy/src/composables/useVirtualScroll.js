/**
 * Virtual Scroll Composable
 * Efficiently renders only visible items for large lists
 * Reduces DOM nodes and improves scrolling performance
 */
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'

export function useVirtualScroll(options = {}) {
  const {
    itemHeight = 72,          // Fixed height of each item
    bufferSize = 5,           // Number of items to render outside viewport
    containerRef = ref(null),
  } = options

  const scrollTop = ref(0)
  const containerHeight = ref(0)

  // Items prop - can be reactive
  const items = ref([])

  // Computed values
  const totalHeight = computed(() => items.value.length * itemHeight)

  const visibleCount = computed(() =>
    Math.ceil(containerHeight.value / itemHeight) + bufferSize * 2
  )

  const startIndex = computed(() =>
    Math.max(0, Math.floor(scrollTop.value / itemHeight) - bufferSize)
  )

  const endIndex = computed(() =>
    Math.min(items.value.length, startIndex.value + visibleCount.value)
  )

  const visibleItems = computed(() => {
    const start = startIndex.value
    const end = endIndex.value
    return items.value.slice(start, end).map((item, index) => ({
      ...item,
      _index: start + index,
    }))
  })

  const offsetY = computed(() => startIndex.value * itemHeight)

  // Handle scroll
  let rafId = null
  const handleScroll = (e) => {
    if (rafId) return

    rafId = requestAnimationFrame(() => {
      scrollTop.value = e.target.scrollTop
      rafId = null
    })
  }

  // Resize observer
  let resizeObserver = null

  const setupResizeObserver = () => {
    if (typeof ResizeObserver === 'undefined') return

    resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        containerHeight.value = entry.contentRect.height
      }
    })

    if (containerRef.value) {
      resizeObserver.observe(containerRef.value)
    }
  }

  // Update items
  const setItems = (newItems) => {
    items.value = newItems
  }

  // Scroll to index
  const scrollToIndex = (index, behavior = 'smooth') => {
    if (!containerRef.value) return

    const targetScroll = index * itemHeight
    containerRef.value.scrollTo({
      top: targetScroll,
      behavior,
    })
  }

  // Scroll to item
  const scrollToItem = (predicate, behavior = 'smooth') => {
    const index = items.value.findIndex(predicate)
    if (index !== -1) {
      scrollToIndex(index, behavior)
    }
  }

  onMounted(() => {
    nextTick(() => {
      if (containerRef.value) {
        containerHeight.value = containerRef.value.clientHeight
        setupResizeObserver()
      }
    })
  })

  onUnmounted(() => {
    if (rafId) {
      cancelAnimationFrame(rafId)
    }
    if (resizeObserver) {
      resizeObserver.disconnect()
    }
  })

  return {
    items,
    setItems,
    totalHeight,
    visibleItems,
    offsetY,
    startIndex,
    endIndex,
    handleScroll,
    scrollToIndex,
    scrollToItem,
    containerRef,
  }
}

/**
 * Intersection Observer based infinite scroll
 * Loads more items when reaching the bottom
 */
export function useInfiniteScroll(callback, options = {}) {
  const {
    threshold = 100,          // Load more when within this many pixels of bottom
    containerRef = ref(null),
  } = options

  const isLoading = ref(false)
  const hasMore = ref(true)

  const loadMore = async () => {
    if (isLoading.value || !hasMore.value) return

    isLoading.value = true
    try {
      const result = await callback()
      if (result === false) {
        hasMore.value = false
      }
    } finally {
      isLoading.value = false
    }
  }

  let observer = null

  const setupObserver = () => {
    if (typeof IntersectionObserver === 'undefined') return

    observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0]
        if (entry.isIntersecting && hasMore.value && !isLoading.value) {
          loadMore()
        }
      },
      {
        root: containerRef.value,
        rootMargin: `0px 0px ${threshold}px 0px`,
        threshold: 0,
      }
    )

    // Observe a sentinel element at the bottom
    const sentinel = document.createElement('div')
    sentinel.setAttribute('data-scroll-sentinel', '')
    sentinel.style.height = '1px'
    containerRef.value?.appendChild(sentinel)
    observer.observe(sentinel)
  }

  onMounted(() => {
    if (containerRef.value) {
      setupObserver()
    }
  })

  onUnmounted(() => {
    if (observer) {
      observer.disconnect()
    }
    // Clean up sentinel
    const sentinel = document.querySelector('[data-scroll-sentinel]')
    sentinel?.remove()
  })

  return {
    isLoading,
    hasMore,
    loadMore,
  }
}
