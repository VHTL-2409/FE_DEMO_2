<template>
  <div class="neural-hero cosmos-reveal">
    <!-- Greeting -->
    <div class="flex flex-col lg:flex-row lg:items-end justify-between gap-4 mb-6">
      <div>
        <p class="text-sm font-semibold mb-1" style="color: var(--cosmos-text-muted);">
          {{ greeting }}
        </p>
        <h1 class="text-3xl lg:text-4xl font-black cosmos-heading-display" style="color: var(--cosmos-text);">
          {{ teacherName }}
        </h1>
        <p class="text-sm mt-1" style="color: var(--cosmos-text-secondary);">
          Hôm nay là {{ today }}
        </p>
      </div>
      <!-- Live status badge -->
      <div class="flex items-center gap-3">
        <div v-if="liveExamCount > 0" class="flex items-center gap-2 px-3 py-1.5 rounded-full cosmos-glass--teal" style="border: 1px solid var(--cosmos-teal-border);">
          <span class="w-2 h-2 rounded-full cosmos-pulse" style="background: var(--cosmos-teal);" />
          <span class="text-xs font-bold" style="color: var(--cosmos-teal);">{{ liveExamCount }} ky thi dang dien ra</span>
        </div>
        <div v-if="activeStudentCount > 0" class="flex items-center gap-2 px-3 py-1.5 rounded-full" style="background: var(--cosmos-rose-soft); border: 1px solid var(--cosmos-rose-border);">
          <span class="w-2 h-2 rounded-full cosmos-pulse" style="background: var(--cosmos-rose);" />
          <span class="text-xs font-bold" style="color: var(--cosmos-rose);">{{ activeStudentCount }} hoc sinh online</span>
        </div>
        <div v-if="alertCount > 0" class="flex items-center gap-2 px-3 py-1.5 rounded-full" style="background: var(--cosmos-danger-soft); border: 1px solid var(--cosmos-danger-border);">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--cosmos-danger)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          <span class="text-xs font-bold" style="color: var(--cosmos-danger);">{{ alertCount }} canh bao</span>
        </div>
      </div>
    </div>

    <!-- Quick stats strip -->
    <div class="grid grid-cols-3 lg:grid-cols-5 gap-3">
      <button
        v-for="stat in quickStats"
        :key="stat.label"
        class="neural-hero__stat cosmos-card p-3 text-left cosmos-lift"
        @click="stat.action"
      >
        <div class="flex items-center gap-2 mb-1">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" :stroke="stat.color" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">{{ stat.iconPath }}</svg>
          <span class="text-xs font-semibold" style="color: var(--cosmos-text-muted);">{{ stat.label }}</span>
        </div>
        <p class="text-xl font-black cosmos-heading-display" :style="`color: ${stat.color}; font-family: var(--cosmos-font-mono);`">{{ stat.value }}</p>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  teacherName: { type: String, default: 'Giao vien' },
  liveExamCount: { type: Number, default: 0 },
  activeStudentCount: { type: Number, default: 0 },
  alertCount: { type: Number, default: 0 },
  totalExamCount: { type: Number, default: 0 },
  upcomingCount: { type: Number, default: 0 },
  endedCount: { type: Number, default: 0 }
})

const emit = defineEmits(['go-monitoring', 'create-exam'])

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return 'Chao buoi sang'
  if (hour < 18) return 'Chao buoi chieu'
  return 'Chao buoi toi'
})

const today = computed(() => {
  return new Date().toLocaleDateString('vi-VN', { weekday: 'long', day: 'numeric', month: 'long' })
})

const quickStats = computed(() => [
  {
    iconPath: '<circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/>',
    label: 'Tong ky thi',
    value: totalExams > 0 ? totalExams : '—',
    color: 'var(--cosmos-teal)',
    action: () => emit('go-monitoring')
  },
  {
    iconPath: '<circle cx="12" cy="12" r="10"/><polygon points="10 8 16 12 10 16 10 8"/>',
    label: 'Dang dien ra',
    value: liveExamCount > 0 ? liveExamCount : '—',
    color: 'var(--cosmos-success)',
    action: () => emit('go-monitoring')
  },
  {
    iconPath: '<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>',
    label: 'Sap dien ra',
    value: upcomingCount > 0 ? upcomingCount : '—',
    color: 'var(--cosmos-rose)',
    action: () => emit('go-monitoring')
  },
  {
    iconPath: '<circle cx="12" cy="12" r="10"/><polyline points="9 11 12 14 15 11"/>',
    label: 'Da ket thuc',
    value: endedCount > 0 ? endedCount : '—',
    color: 'var(--cosmos-text-muted)',
    action: () => {}
  },
  {
    iconPath: '<line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/>',
    label: 'Hoc sinh',
    value: activeStudentCount > 0 ? activeStudentCount : '—',
    color: 'var(--cosmos-amber)',
    action: () => emit('go-monitoring')
  }
])

const totalExams = computed(() => props.totalExamCount || 0)
const upcomingCount = computed(() => props.upcomingCount || 0)
const endedCount = computed(() => props.endedCount || 0)
</script>

<style scoped>
.neural-hero__stat {
  cursor: pointer;
  border: 1px solid var(--cosmos-border);
}
.neural-hero__stat:hover {
  border-color: var(--cosmos-teal-border);
}
</style>
