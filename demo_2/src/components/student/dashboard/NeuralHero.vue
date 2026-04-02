<template>
  <div class="neural-hero-student ln-reveal">
    <!-- Greeting -->
    <div class="flex flex-col lg:flex-row lg:items-center justify-between gap-4 mb-6">
      <div class="flex items-center gap-4">
        <!-- Avatar -->
        <div class="relative">
          <div class="w-16 h-16 rounded-2xl flex items-center justify-center text-xl font-black"
            style="background: linear-gradient(135deg, var(--ln-cyan) 0%, var(--ln-violet) 100%); box-shadow: var(--ln-shadow-cyan); color: white; font-family: var(--ln-font-display);">
            {{ studentInitial }}
          </div>
          <span class="absolute -bottom-1 -right-1 w-4 h-4 rounded-full border-2 border-[var(--ln-bg-0)]" style="background: var(--ln-success);" />
        </div>
        <div>
          <p class="text-sm font-semibold mb-0.5" style="color: var(--ln-text-muted);">{{ greeting }}</p>
          <h1 class="text-2xl lg:text-3xl font-black ln-heading-display" style="color: var(--ln-text);">{{ studentName }}</h1>
          <p class="text-xs mt-0.5" style="color: var(--ln-text-secondary);">{{ today }}</p>
        </div>
      </div>
      <!-- Quick action buttons -->
      <div class="flex gap-2">
        <button class="ln-btn ln-btn--primary ln-btn--md ln-btn--shimmer" @click="$emit('action', 'join-exam')">
          <span class="material-symbols-outlined text-base" style="font-variation-settings:'FILL'1">login</span>
          Tham gia thi
        </button>
        <button class="ln-btn ln-btn--outline ln-btn--md" @click="$emit('action', 'practice')">
          <span class="material-symbols-outlined text-base">auto_awesome</span>
          Luyen tap
        </button>
      </div>
    </div>

    <!-- Quick stats -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-3">
      <button
        v-for="stat in quickStats"
        :key="stat.label"
        class="neural-hero-stat ln-card p-4 text-left ln-glass--hover"
        @click="$emit('action', stat.action)"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
            :style="`background: ${stat.bg}; box-shadow: ${stat.glow};`">
            <span class="material-symbols-outlined text-lg" :style="`color: ${stat.color};`">{{ stat.icon }}</span>
          </div>
          <div>
            <p class="text-xl font-black ln-heading-display" :style="`color: ${stat.color}; font-family: var(--ln-font-mono);`">{{ stat.value }}</p>
            <p class="text-xs font-semibold" style="color: var(--ln-text-muted);">{{ stat.label }}</p>
          </div>
        </div>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  studentName: { type: String, default: 'Hoc sinh' },
  examCount: { type: Number, default: 0 },
  pendingCount: { type: Number, default: 0 },
  newScoreCount: { type: Number, default: 0 },
  totalAttempts: { type: Number, default: 0 },
  avgScore: { type: String, default: '—' }
})

const emit = defineEmits(['action-click', 'action'])

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return 'Chao buoi sang'
  if (hour < 18) return 'Chao buoi chieu'
  return 'Chao buoi toi'
})

const today = computed(() => {
  return new Date().toLocaleDateString('vi-VN', { weekday: 'long', day: 'numeric', month: 'long' })
})

const studentInitial = computed(() => {
  return props.studentName?.slice(0, 1).toUpperCase() || 'H'
})

const quickStats = computed(() => [
  { icon: 'event', label: 'Ky thi sap toi', value: props.examCount || '—', color: 'var(--ln-cyan)', bg: 'var(--ln-cyan-soft)', glow: 'var(--ln-shadow-cyan)', action: 'schedule' },
  { icon: 'pending_actions', label: 'Chua hoan thanh', value: props.pendingCount || '—', color: 'var(--ln-warning)', bg: 'var(--ln-warning-soft)', glow: 'none', action: 'join-exam' },
  { icon: 'insights', label: 'Diem trung binh', value: props.avgScore || '—', color: 'var(--ln-violet)', bg: 'var(--ln-violet-soft)', glow: 'var(--ln-shadow-violet)', action: 'results' },
  { icon: 'trending_up', label: 'Ty le dat', value: props.newScoreCount > 0 ? `${props.newScoreCount} moi` : '—', color: 'var(--ln-success)', bg: 'var(--ln-success-soft)', glow: 'none', action: 'results' }
])
</script>

<style scoped>
.neural-hero-stat {
  cursor: pointer;
  border: 1px solid var(--ln-border);
}
.neural-hero-stat:hover {
  border-color: var(--ln-border-cyan);
}
</style>
