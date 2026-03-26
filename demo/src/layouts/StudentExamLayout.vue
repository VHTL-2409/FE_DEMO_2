<template>
  <div class="route-layout route-layout-exam min-h-full">
    <slot />
  </div>
</template>

<style scoped>
.route-layout-exam {
  min-height: 100%;
  isolation: isolate;
}
</style>
