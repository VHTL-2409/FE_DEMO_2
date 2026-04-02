<template>
  <div>
    <h3 class="text-lg font-bold ln-heading-display mb-4" style="color: var(--ln-text);">Phan tich nhanh</h3>

    <!-- Stats -->
    <div class="grid grid-cols-2 gap-3 mb-5">
      <div class="ln-card p-3 text-center" style="border: 1px solid var(--ln-border);">
        <p class="text-2xl font-black ln-heading-display" style="color: var(--ln-cyan); font-family: var(--ln-font-mono);">
          {{ totalExams }}
        </p>
        <p class="text-[10px] font-semibold uppercase tracking-wide" style="color: var(--ln-text-muted);">Tong ky thi</p>
      </div>
      <div class="ln-card p-3 text-center" style="border: 1px solid var(--ln-border);">
        <p class="text-2xl font-black ln-heading-display" style="color: var(--ln-success); font-family: var(--ln-font-mono);">
          {{ activeExams }}
        </p>
        <p class="text-[10px] font-semibold uppercase tracking-wide" style="color: var(--ln-text-muted);">Dang chay</p>
      </div>
    </div>

    <!-- Mini bar chart -->
    <div class="mb-5">
      <p class="text-xs font-semibold mb-3" style="color: var(--ln-text-muted);">Ky thi theo ngay (7 ngay gan nhat)</p>
      <div class="flex items-end gap-1.5 h-16">
        <div
          v-for="(val, i) in barData"
          :key="i"
          class="flex-1 rounded-t-sm transition-all duration-300"
          :style="`height: ${Math.max(val / maxVal * 100, 4)}%; background: linear-gradient(180deg, var(--ln-cyan) 0%, var(--ln-violet) 100%); opacity: ${val > 0 ? 1 : 0.3};`"
          :title="`${val} ky thi`"
        />
      </div>
      <div class="flex justify-between mt-1">
        <span v-for="(label, i) in barLabels" :key="i" class="text-[10px] font-mono" style="color: var(--ln-text-muted);">{{ label }}</span>
      </div>
    </div>

    <!-- Insight items -->
    <div class="space-y-2">
      <div
        v-for="insight in insights"
        :key="insight.label"
        class="flex items-center gap-3 p-3 rounded-xl ln-card"
        style="border: 1px solid var(--ln-border);"
      >
        <div
          class="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0"
          :style="`background: ${insight.bg};`"
        >
          <span class="material-symbols-outlined text-base" :style="`color: ${insight.color};`">{{ insight.icon }}</span>
        </div>
        <div class="flex-1 min-w-0">
          <p class="text-xs font-semibold truncate" style="color: var(--ln-text);">{{ insight.label }}</p>
          <p class="text-[10px] truncate" style="color: var(--ln-text-muted);">{{ insight.sub }}</p>
        </div>
        <span class="ln-mono text-sm font-bold flex-shrink-0" :style="`color: ${insight.color};`">{{ insight.value }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  exams: { type: Array, default: () => [] },
  stats: {
    type: Object,
    default: () => ({
      total: 0,
      ended: 0,
      upcoming: 0,
      draft: 0
    })
  },
  barData: { type: Array, default: () => [] },
  barLabels: { type: Array, default: () => [] },
  insights: { type: Array, default: () => [] }
})

const totalExams = computed(() => props.exams.length || 0)
const activeExams = computed(() => props.stats?.active || 0)

const upcomingCount = computed(() => {
  return props.exams.filter(e => e.statusKey === 'upcoming').length
})

const endedCount = computed(() => {
  return props.exams.filter(e => e.statusKey === 'ended').length
})

const completionRate = computed(() => {
  const total = props.stats?.total || 0
  if (!total) return 0
  return Math.round((props.stats.ended / total) * 100)
})

const barData = computed(() => {
  if (props.barData && props.barData.length > 0) {
    return props.barData
  }
  return [0, 0, 0, 0, 0, 0, 0]
})

const maxVal = computed(() => Math.max(...barData.value, 1))

const barLabels = computed(() => {
  if (props.barLabels && props.barLabels.length > 0) {
    return props.barLabels
  }
  const days = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN']
  return days
})

const insights = computed(() => {
  if (props.insights && props.insights.length > 0) {
    return props.insights
  }
  return [
    { icon: 'trending_up', label: 'Ty le hoan thanh', sub: 'Tat ca ky thi', value: completionRate.value > 0 ? `${completionRate.value}%` : '—', color: 'var(--ln-success)', bg: 'var(--ln-success-soft)' },
    { icon: 'schedule', label: 'Ky thi sap toi', sub: `${upcomingCount.value} ky thi`, value: String(upcomingCount.value), color: 'var(--ln-cyan)', bg: 'var(--ln-cyan-soft)' },
    { icon: 'groups', label: 'Ky thi da ket thuc', sub: `${endedCount.value} ky thi`, value: String(endedCount.value), color: 'var(--ln-violet)', bg: 'var(--ln-violet-soft)' }
  ]
})
</script>
