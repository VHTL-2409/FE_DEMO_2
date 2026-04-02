<template>
  <div>
    <h3 class="text-lg font-bold ln-heading-display mb-4" style="color: var(--ln-text);">Thao tac nhanh</h3>
    <div class="grid grid-cols-2 gap-3">
      <button
        v-for="action in actions"
        :key="action.id"
        class="action-grid__item ln-card p-4 text-left ln-glass--hover ln-spring"
        @click="$emit('action', action.id)"
      >
        <div
          class="w-10 h-10 rounded-xl flex items-center justify-center mb-3"
          :style="`background: ${action.bg}; box-shadow: ${action.glow};`"
        >
          <span class="material-symbols-outlined text-xl" :style="`color: ${action.color};`">{{ action.icon }}</span>
        </div>
        <p class="text-sm font-bold" style="color: var(--ln-text);">{{ action.label }}</p>
        <p class="text-xs mt-0.5" style="color: var(--ln-text-muted);">{{ action.sub }}</p>
      </button>
    </div>
  </div>
</template>

<script setup>
defineEmits(['action'])

const actions = [
  { id: 'create', icon: 'add_circle', label: 'Tao de thi', sub: 'Nhanh chong', bg: 'var(--ln-cyan-soft)', color: 'var(--ln-cyan)', glow: 'var(--ln-shadow-cyan)' },
  { id: 'list', icon: 'list_alt', label: 'Danh sach de', sub: 'Quan ly', bg: 'var(--ln-violet-soft)', color: 'var(--ln-violet)', glow: 'var(--ln-shadow-violet)' },
  { id: 'monitoring', icon: 'visibility', label: 'Giam sat', sub: 'Realtime', bg: 'var(--ln-success-soft)', color: 'var(--ln-success)', glow: 'none' },
  { id: 'analytics', icon: 'bar_chart', label: 'Bao cao', sub: 'Phan tich', bg: 'var(--ln-warning-soft)', color: 'var(--ln-warning)', glow: 'none' },
  { id: 'schedule', icon: 'event', label: 'Lich thi', sub: 'Sap xep', bg: 'var(--ln-info-soft)', color: 'var(--ln-info)', glow: 'none' },
  { id: 'profile', icon: 'account_circle', label: 'Ho so', sub: 'Ca nhan', bg: 'var(--ln-surface-muted)', color: 'var(--ln-text-muted)', glow: 'none' }
]
</script>

<style scoped>
.action-grid__item {
  cursor: pointer;
  border: 1px solid var(--ln-border);
}
.action-grid__item:hover {
  border-color: var(--ln-border-cyan);
}
</style>
