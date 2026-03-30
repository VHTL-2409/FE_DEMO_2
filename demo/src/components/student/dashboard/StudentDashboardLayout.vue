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
  gap: 1.25rem;
  padding: 1.25rem;
  max-width: 1100px;
  margin: 0 auto;
}

/* KPI strip */
.sdl__kpis {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.875rem;
}

/* Main layout */
.sdl__main {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 1.25rem;
  align-items: start;
}

.sdl__main-col {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.sdl__sidebar {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* Responsive */
@media (max-width: 1024px) {
  .sdl__main {
    grid-template-columns: 1fr;
  }

  .sdl__sidebar {
    order: -1;
  }
}

@media (max-width: 768px) {
  .sdl {
    padding: 1rem;
    gap: 1rem;
  }

  .sdl__kpis {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .sdl__kpis {
    grid-template-columns: 1fr;
  }
}
</style>
