<template>
  <router-view v-slot="{ Component, route }">
    <template v-if="usePortalTransition(route)">
      <transition name="portal-page" mode="out-in">
        <div :key="route.path" class="portal-route-root">
          <component :is="Component" />
        </div>
      </transition>
    </template>
    <component v-else :is="Component" :key="route.path" />
  </router-view>
  <ToastHost />
</template>

<script setup>
import ToastHost from './components/common/ToastHost.vue'

/** Chỉ áp dụng hiệu ứng chuyển trang cho khu vực học sinh / giáo viên */
const usePortalTransition = (r) => {
  const p = r?.path || ''
  return p.startsWith('/student/') || p.startsWith('/teacher/')
}
</script>