/**
 * Optimized Image Component with lazy loading and WebP support
 * Features:
 * - Lazy loading using Intersection Observer
 * - WebP format detection and fallback
 * - Placeholder/blur-up effect
 * - Error handling with fallback
 */
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'

export function useLazyImage(options = {}) {
  const {
    src,
    fallbackSrc = '',
    rootMargin = '200px',    // Load images 200px before they enter viewport
    threshold = 0.01,
  } = options

  const imageRef = ref(null)
  const isLoaded = ref(false)
  const hasError = ref(false)
  const currentSrc = ref('')
  const isInView = ref(false)

  let observer = null

  // Check WebP support
  const supportsWebP = ref(false)

  const checkWebP = () => {
    const canvas = document.createElement('canvas')
    if (canvas.getContext && canvas.getContext('2d')) {
      canvas.width = 1
      canvas.height = 1
      const dataUrl = canvas.toDataURL('image/webp')
      supportsWebP.value = dataUrl.indexOf('data:image/webp') === 0
    }
    return supportsWebP.value
  }

  // Get optimized image URL
  const getOptimizedSrc = (url) => {
    if (!url) return ''

    // If already a full URL or data URL, return as-is
    if (url.startsWith('data:') || url.startsWith('http')) {
      return url
    }

    // If using Unsplash or similar services that support quality params
    if (url.includes('unsplash.com')) {
      const separator = url.includes('?') ? '&' : '?'
      return `${url}${separator}q=80&w=800&fm=webp`
    }

    // For local images, add loading optimization hints
    return url
  }

  // Setup Intersection Observer
  const setupObserver = () => {
    if (typeof window === 'undefined' || !window.IntersectionObserver) {
      // Fallback: load immediately if IntersectionObserver not supported
      isInView.value = true
      return
    }

    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            isInView.value = true
            // Once in view, stop observing
            if (observer) {
              observer.disconnect()
              observer = null
            }
          }
        })
      },
      {
        rootMargin,
        threshold,
      }
    )

    if (imageRef.value) {
      observer.observe(imageRef.value)
    }
  }

  // Load image
  const loadImage = () => {
    if (!src.value) return

    const img = new Image()

    img.onload = () => {
      currentSrc.value = src.value
      isLoaded.value = true
      hasError.value = false
    }

    img.onerror = () => {
      hasError.value = true
      isLoaded.value = false
      if (fallbackSrc) {
        currentSrc.value = fallbackSrc
      }
    }

    img.src = getOptimizedSrc(src.value)
  }

  // Watch for src changes
  watch(() => src.value, () => {
    isLoaded.value = false
    hasError.value = false
    if (isInView.value) {
      loadImage()
    }
  })

  // Watch for viewport visibility
  watch(isInView, (inView) => {
    if (inView && !isLoaded.value && src.value) {
      loadImage()
    }
  })

  onMounted(() => {
    checkWebP()
    setupObserver()
  })

  onUnmounted(() => {
    if (observer) {
      observer.disconnect()
      observer = null
    }
  })

  return {
    imageRef,
    isLoaded,
    hasError,
    currentSrc,
    isInView,
    supportsWebP,
  }
}

// Vue component for lazy images
export const LazyImage = {
  name: 'LazyImage',
  props: {
    src: { type: String, required: true },
    alt: { type: String, default: '' },
    fallback: { type: String, default: '' },
    width: { type: [Number, String], default: null },
    height: { type: [Number, String], default: null },
    objectFit: { type: String, default: 'cover' },
    placeholderClass: { type: String, default: '' },
  },
  setup(props, { emit }) {
    const containerRef = ref(null)
    const imageLoaded = ref(false)
    const imageError = ref(false)
    const shouldLoad = ref(false)

    let observer = null

    const loadImage = () => {
      shouldLoad.value = true
    }

    const onImageLoad = () => {
      imageLoaded.value = true
      emit('load')
    }

    const onImageError = () => {
      imageError.value = true
      emit('error')
    }

    onMounted(() => {
      if (typeof window !== 'undefined' && 'IntersectionObserver' in window) {
        observer = new IntersectionObserver(
          (entries) => {
            if (entries[0].isIntersecting) {
              loadImage()
              observer?.disconnect()
            }
          },
          { rootMargin: '200px', threshold: 0.01 }
        )
        observer.observe(containerRef.value)
      } else {
        // Fallback for browsers without IntersectionObserver
        loadImage()
      }
    })

    onUnmounted(() => {
      observer?.disconnect()
    })

    const displaySrc = computed(() => {
      if (imageError.value && props.fallback) {
        return props.fallback
      }
      return props.src
    })

    return {
      containerRef,
      imageLoaded,
      imageError,
      shouldLoad,
      displaySrc,
      onImageLoad,
      onImageError,
    }
  },
  template: `
    <div
      ref="containerRef"
      class="lazy-image"
      :class="{ 'lazy-image--loaded': imageLoaded, 'lazy-image--error': imageError }"
      :style="{
        width: width ? (typeof width === 'number' ? width + 'px' : width) : '100%',
        height: height ? (typeof height === 'number' ? height + 'px' : height) : 'auto'
      }"
    >
      <!-- Skeleton/Placeholder -->
      <div v-if="!imageLoaded && !imageError" class="lazy-image__skeleton">
        <slot name="placeholder">
          <div class="lazy-image__skeleton-inner"></div>
        </slot>
      </div>

      <!-- Actual Image -->
      <img
        v-if="shouldLoad"
        :src="displaySrc"
        :alt="alt"
        class="lazy-image__img"
        :class="{ 'lazy-image__img--visible': imageLoaded }"
        :style="{ objectFit }"
        @load="onImageLoad"
        @error="onImageError"
      />

      <!-- Error State -->
      <div v-if="imageError" class="lazy-image__error">
        <slot name="error">
          <div class="lazy-image__error-icon">?</div>
        </slot>
      </div>
    </div>
  `,
}

export default LazyImage
