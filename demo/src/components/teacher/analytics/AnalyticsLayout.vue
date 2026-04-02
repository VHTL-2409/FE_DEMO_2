<template>
  <div class="al">
    <!-- Header -->
    <header class="al__header">
      <slot name="header" />
    </header>

    <!-- KPI strip -->
    <div class="al__kpi-strip">
      <slot name="kpis" />
    </div>

    <!-- Charts row -->
    <div class="al__charts-row">
      <div class="al__chart-main">
        <slot name="chart" />
      </div>
      <div class="al__chart-side">
        <slot name="insights" />
      </div>
    </div>

    <!-- Filters -->
    <div class="al__filters">
      <slot name="filters" />
    </div>

    <!-- Table -->
    <div class="al__table">
      <slot name="table" />
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.al {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 1.25rem;
  max-width: 1440px;
  margin: 0 auto;
}

@media (min-width: 1400px) {
  .al { max-width: 1600px; }
}

@media (min-width: 1600px) {
  .al { max-width: 1800px; }
}

/* KPI strip */
.al__kpi-strip {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 0.875rem;
}

@media (min-width: 1600px) {
  .al__kpi-strip { grid-template-columns: repeat(6, 1fr); }
}

/* Charts row: 2/3 + 1/3 */
.al__charts-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 1rem;
  align-items: start;
}

@media (min-width: 1600px) {
  .al__charts-row { gap: 1.25rem; }
}

/* Responsive */
@media (max-width: 1100px) {
  .al__kpi-strip {
    grid-template-columns: repeat(3, 1fr);
  }

  .al__charts-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .al {
    padding: 1rem;
    gap: 1rem;
  }

  .al__kpi-strip {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .al__kpi-strip {
    grid-template-columns: 1fr;
  }
}
</style>
