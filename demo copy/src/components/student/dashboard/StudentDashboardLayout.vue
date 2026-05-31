<template>
  <!-- sdl--visible must live on ancestor: CSS uses .sdl--visible .sdl__hero (descendant), not same element -->
  <div ref="containerRef" class="sdl" :class="{ 'sdl--visible': isVisible }">
    <!-- Hero section -->
    <div class="sdl__hero">
      <slot name="hero" />
    </div>

    <!-- KPI strip -->
    <div v-if="$slots.kpis" class="sdl__kpis">
      <slot name="kpis" />
    </div>

    <!-- Main content: 2-column on large screens -->
    <div class="sdl__main">
      <!-- Left column (main content) -->
      <div class="sdl__main-col">
        <slot name="main" />
      </div>

      <!-- Right column (sidebar) -->
      <div v-if="$slots.sidebar" class="sdl__sidebar">
        <slot name="sidebar" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { useIntersectionObserver } from '../../../composables/useIntersectionObserver'

// Use intersection observer for optimized animation triggering
// Only animate when component enters viewport
const { containerRef, isVisible } = useIntersectionObserver({
  threshold: 0.05,
  rootMargin: '0px 0px -20px 0px',
  once: true
})
</script>

<style scoped>
/* Entrance animation without GPU text compositing */
@keyframes fadeInUp {
  from { opacity: 0; }
  to   { opacity: 1; }
}

.sdl {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 1.5rem;
  max-width: 1640px;
  width: 100%;
  margin: 0 auto;
  contain: layout style;
  /* Note: NOT using content-visibility: auto here — it breaks
     IntersectionObserver sizing, causing the dashboard to stay invisible */
}

/* Entrance animations - GPU accelerated, triggered by visibility class */
.sdl__hero {
  opacity: 0;
}

.sdl--visible .sdl__hero {
  animation: fadeInUp 0.45s cubic-bezier(0.16, 1, 0.3, 1) 0.05s both;
}

.sdl__kpis {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
  opacity: 0;
}

.sdl--visible .sdl__kpis {
  animation: fadeInUp 0.45s cubic-bezier(0.16, 1, 0.3, 1) 0.12s both;
}

.sdl__main {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
  align-items: start;
  opacity: 0;
}

.sdl__main:has(.sdl__sidebar) {
  grid-template-columns: 1fr 320px;
}

/* Main section enters sooner (0.22s) so cards are visible faster */
.sdl--visible .sdl__main {
  animation: fadeInUp 0.45s cubic-bezier(0.16, 1, 0.3, 1) 0.22s both;
}

.sdl__main-col {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.sdl__sidebar {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Dark mode */
.dark .sdl {
  background: transparent;
}

@media (prefers-reduced-motion: reduce) {
  .sdl__hero,
  .sdl__kpis,
  .sdl__main {
    animation: none !important;
    opacity: 1 !important;
    transform: none !important;
    will-change: auto;
  }
}

/* Responsive - large screens */
@media (min-width: 1400px) {
  .sdl { max-width: 1640px; }
  .sdl__main:has(.sdl__sidebar) { grid-template-columns: 1fr 360px; }
}

@media (min-width: 1600px) {
  .sdl { max-width: 1720px; }
  .sdl__main:has(.sdl__sidebar) { grid-template-columns: 1fr 400px; }
}

@media (min-width: 1920px) {
  .sdl { max-width: 1800px; }
  .sdl__main:has(.sdl__sidebar) { grid-template-columns: 1fr 440px; }
}

/* Responsive - medium screens */
@media (max-width: 1200px) {
  .sdl__main:has(.sdl__sidebar) {
    grid-template-columns: 1fr 280px;
  }
}

@media (max-width: 1024px) {
  .sdl {
    padding: 1.25rem;
    gap: 1.25rem;
  }

  .sdl__main {
    grid-template-columns: 1fr;
    gap: 1.25rem;
  }

  .sdl__sidebar {
    order: -1;
  }

  .sdl__kpis {
    grid-template-columns: repeat(2, 1fr);
    gap: 1rem;
  }
}

@media (max-width: 768px) {
  .sdl {
    padding: 1rem;
    gap: 1rem;
  }

  .sdl__kpis {
    grid-template-columns: repeat(2, 1fr);
    gap: 0.75rem;
  }

  .sdl__main-col {
    gap: 1rem;
  }

  .sdl__sidebar {
    gap: 1rem;
  }
}

@media (max-width: 480px) {
  .sdl {
    padding: 0.75rem;
  }

  .sdl__kpis {
    grid-template-columns: 1fr;
    gap: 0.625rem;
  }
}
</style>
