<template>
  <div v-if="liveExam" class="live-monitor-card ln-glow-card ln-card p-5 ln-spring">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-3">
        <div class="ln-ring is-live">
          <div class="w-10 h-10 rounded-xl flex items-center justify-center" style="background: var(--ln-success-soft);">
            <span class="material-symbols-outlined text-xl" style="color: var(--ln-success);">live_tv</span>
          </div>
        </div>
        <div>
          <p class="font-bold" style="color: var(--ln-text);">{{ liveExam.title || 'Ky thi realtime' }}</p>
          <p class="text-xs" style="color: var(--ln-text-muted);">Giam sat truc tiep</p>
        </div>
      </div>
      <span class="ln-badge ln-badge--success">
        <span class="ln-badge__dot" />
        LIVE
      </span>
    </div>

    <!-- Progress bar -->
    <div class="mb-4">
      <div class="flex justify-between mb-1.5">
        <span class="text-xs font-semibold" style="color: var(--ln-text-muted);">Tien do</span>
        <span class="text-xs font-bold ln-mono" style="color: var(--ln-cyan);">
          {{ answeredCount }}/{{ questionCount }} cau
        </span>
      </div>
      <div class="ln-progress" style="height: 6px;">
        <div
          class="ln-progress__bar"
          :style="`width: ${questionCount > 0 ? (answeredCount / questionCount * 100) : 0}%;`"
        />
      </div>
    </div>

    <!-- Mini stats row -->
    <div class="grid grid-cols-3 gap-3 mb-4">
      <div class="text-center p-2 rounded-lg" style="background: var(--ln-bg-3);">
        <p class="text-lg font-black ln-heading-display" style="color: var(--ln-cyan); font-family: var(--ln-font-mono);">{{ studentCount }}</p>
        <p class="text-[10px] font-semibold uppercase" style="color: var(--ln-text-muted);">Thi sinh</p>
      </div>
      <div class="text-center p-2 rounded-lg" style="background: var(--ln-bg-3);">
        <p class="text-lg font-black ln-heading-display" style="color: var(--ln-violet); font-family: var(--ln-font-mono);">{{ answeredCount }}</p>
        <p class="text-[10px] font-semibold uppercase" style="color: var(--ln-text-muted);">Da tra loi</p>
      </div>
      <div class="text-center p-2 rounded-lg" style="background: var(--ln-bg-3);">
        <p class="text-lg font-black ln-heading-display" style="color: var(--ln-warning); font-family: var(--ln-font-mono);">{{ alertCount }}</p>
        <p class="text-[10px] font-semibold uppercase" style="color: var(--ln-text-muted);">Canh bao</p>
      </div>
    </div>

    <!-- Student avatars -->
    <div class="flex items-center justify-between mb-4">
      <div class="flex -space-x-2">
        <div v-for="n in Math.min(studentCount, 5)" :key="n" class="ln-avatar ln-avatar--sm ln-ring" :style="`z-index: ${6-n}; border: 2px solid var(--ln-border);`">
          <span style="font-size: 0.5rem; color: var(--ln-cyan);">S{{ n }}</span>
        </div>
        <div v-if="studentCount > 5" class="ln-avatar ln-avatar--sm" style="background: var(--ln-surface-muted); border: 2px solid var(--ln-border); z-index: 1;">
          <span style="font-size: 0.5rem; color: var(--ln-text-muted);">+{{ studentCount - 5 }}</span>
        </div>
      </div>
      <button
        class="ln-btn ln-btn--primary ln-btn--sm ln-btn--shimmer"
        @click="$emit('go-monitoring')"
      >
        <span class="material-symbols-outlined text-base" style="font-variation-settings:'FILL'1">visibility</span>
        Giam sat ngay
      </button>
    </div>
  </div>

  <!-- Empty state -->
  <div v-else class="ln-card p-6 text-center">
    <div class="ln-empty__icon mx-auto mb-3">
      <span class="material-symbols-outlined text-3xl">live_tv</span>
    </div>
    <p class="font-bold mb-1" style="color: var(--ln-text);">Khong co ky thi nao dang dien ra</p>
    <p class="text-sm" style="color: var(--ln-text-muted);">Bat dau mot ky thi de bat dau giam sat</p>
  </div>
</template>

<script setup>
const props = defineProps({
  liveExam: { type: Object, default: null },
  studentCount: { type: Number, default: 0 },
  answeredCount: { type: Number, default: 0 },
  questionCount: { type: Number, default: 0 },
  alertCount: { type: Number, default: 0 }
})

defineEmits(['go-monitoring'])
</script>
