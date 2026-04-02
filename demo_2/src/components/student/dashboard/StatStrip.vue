<template>
  <div class="stat-strip">
    <div class="flex gap-3 overflow-x-auto pb-2" style="scrollbar-width: none;">
      <div
        v-for="(stat, i) in stats"
        :key="i"
        class="stat-chip ln-card ln-glass--hover flex-shrink-0 p-4 text-center min-w-[140px] ln-reveal"
        :style="{ animationDelay: `${i * 60}ms` }"
      >
        <div class="w-10 h-10 rounded-xl flex items-center justify-center mx-auto mb-2"
          :style="`background: ${stat.bg}; box-shadow: ${stat.glow};`">
          <span class="material-symbols-outlined text-lg" :style="`color: ${stat.color};`">{{ stat.icon }}</span>
        </div>
        <p class="text-xl font-black ln-heading-display mb-0.5" :style="`color: ${stat.color}; font-family: var(--ln-font-mono);`">
          {{ stat.value }}
        </p>
        <p class="text-xs font-semibold" style="color: var(--ln-text-muted);">{{ stat.label }}</p>
        <p v-if="stat.sub" class="text-[10px] mt-0.5" style="color: var(--ln-text-faint);">{{ stat.sub }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  stats: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.stat-strip::-webkit-scrollbar { display: none; }
</style>
