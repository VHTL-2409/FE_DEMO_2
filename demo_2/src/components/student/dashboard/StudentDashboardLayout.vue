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
.sdl {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 1.5rem;
  max-width: 1200px;
  margin: 0 auto;
}

/* KPI strip */
.sdl__kpis {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

/* Main layout */
.sdl__main {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 1.5rem;
  align-items: start;
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

/* Responsive */
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
