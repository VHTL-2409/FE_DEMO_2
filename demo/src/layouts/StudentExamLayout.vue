<template>
  <div class="route-layout route-layout-exam min-h-full">
    <router-view v-slot="{ Component }">
      <Transition name="page" mode="out-in">
        <component :is="Component" />
      </Transition>
    </router-view>
  </div>
</template>

<style scoped>
.route-layout-exam {
  min-height: 100%;
  isolation: isolate;
}
</style>
