<template>
  <div class="sdl">
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
</script>

<style scoped>
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(14px); }
  to   { opacity: 1; transform: translateY(0); }
}

.sdl {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 1.5rem;
  max-width: 1200px;
  margin: 0 auto;
}

/* Entrance animations */
.sdl__hero {
  animation: fadeInUp 0.5s cubic-bezier(0.34, 1.2, 0.64, 1) 0.05s both;
}

.sdl__kpis {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
  animation: fadeInUp 0.5s cubic-bezier(0.34, 1.2, 0.64, 1) 0.15s both;
}

.sdl__main {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 1.5rem;
  align-items: start;
  animation: fadeInUp 0.5s cubic-bezier(0.34, 1.2, 0.64, 1) 0.25s both;
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
    animation: none;
  }
}

/* Responsive - large screens */
@media (min-width: 1400px) {
  .sdl { max-width: 1400px; }
  .sdl__main { grid-template-columns: 1fr 360px; }
}

@media (min-width: 1600px) {
  .sdl { max-width: 1600px; }
  .sdl__main { grid-template-columns: 1fr 400px; }
}

@media (min-width: 1920px) {
  .sdl { max-width: 1800px; }
  .sdl__main { grid-template-columns: 1fr 440px; }
}

/* Responsive - medium screens */
@media (max-width: 1200px) {
  .sdl__main {
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
