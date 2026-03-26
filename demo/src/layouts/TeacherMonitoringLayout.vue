<template>
  <div class="route-layout route-layout-monitoring min-h-full">
    <slot />
  </div>
</template>

<style scoped>
.route-layout-monitoring {
  min-height: 100%;
  isolation: isolate;
}
</style>
