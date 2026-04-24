<template>
  <div
    class="dashboard-monitoring relative overflow-hidden rounded-[var(--ds-radius-xl)] border transition-all duration-200"
    :class="hasLive ? 'border-[rgba(245,158,11,0.3)] bg-gradient-to-br from-[var(--ds-surface)] to-[var(--ds-accent-bg)]' : 'border-[var(--ds-border)] bg-[var(--ds-surface)]'"
  >
    <!-- Empty state -->
    <div v-if="!hasLive" class="flex flex-col items-center justify-center gap-3 px-6 py-10 text-center">
      <div class="flex size-14 items-center justify-center rounded-full bg-[var(--ds-gray-100)]">
        <LucideIcon name="monitoring" size="30" />
      </div>
      <div>
        <p class="text-sm font-semibold text-[var(--ds-text)]">Không có kỳ thi nào đang diễn ra</p>
        <p class="mt-1 text-xs text-[var(--ds-text-muted)]">Các kỳ thi sẽ xuất hiện tại đây khi bắt đầu.</p>
      </div>
    </div>

    <!-- Live exam info -->
    <div v-else class="p-5">
      <!-- Header -->
      <div class="mb-4 flex items-start justify-between gap-3">
        <div class="min-w-0 flex-1">
          <div class="mb-2 flex items-center gap-2">
            <!-- LIVE badge -->
            <span class="ds-live-badge inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-bold uppercase tracking-wider">
              <span class="ds-live-dot relative flex size-2">
                <span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-current opacity-75"></span>
                <span class="relative inline-flex size-2 rounded-full bg-current"></span>
              </span>
              Live
            </span>
            <span class="rounded-full bg-[var(--ds-gray-100)] px-2 py-0.5 text-[11px] font-semibold text-[var(--ds-text-secondary)]">
              {{ liveExam.sessionCount || 1 }} phiên
            </span>
          </div>
          <h3 class="text-base font-bold text-[var(--ds-text)] sm:text-lg truncate">{{ liveExam.title }}</h3>
          <p class="mt-0.5 text-xs text-[var(--ds-text-muted)]">{{ liveExam.classInfo || liveExam.description }}</p>
        </div>

        <!-- Time info -->
        <div v-if="liveExam.startedAt" class="shrink-0 text-right">
          <p class="text-xs font-semibold text-[var(--ds-text-muted)]">Bắt đầu</p>
          <p class="text-sm font-bold tabular-nums text-[var(--ds-text)]">{{ formatTime(liveExam.startedAt) }}</p>
          <p class="text-xs font-semibold text-[var(--ds-accent)]">{{ elapsedLabel }}</p>
        </div>
      </div>

      <!-- Stats row -->
      <div class="mb-4 grid grid-cols-3 gap-3">
        <div class="rounded-[var(--ds-radius-lg)] border border-[var(--ds-border)] bg-[var(--ds-bg)] px-3 py-2.5 text-center">
          <p class="text-xl font-extrabold tabular-nums text-[var(--ds-primary)]">{{ liveExam.studentCount || 0 }}</p>
          <p class="text-[10px] font-semibold uppercase tracking-wider text-[var(--ds-text-muted)]">Học sinh</p>
        </div>
        <div class="rounded-[var(--ds-radius-lg)] border border-[var(--ds-border)] bg-[var(--ds-bg)] px-3 py-2.5 text-center">
          <p class="text-xl font-extrabold tabular-nums text-[var(--ds-success)]">{{ liveExam.answeredCount || 0 }}</p>
          <p class="text-[10px] font-semibold uppercase tracking-wider text-[var(--ds-text-muted)]">Đã nộp</p>
        </div>
        <div class="rounded-[var(--ds-radius-lg)] border px-3 py-2.5 text-center" :class="liveExam.alertCount > 0 ? 'border-[rgba(220,38,38,0.25)] bg-[var(--ds-danger-bg)]' : 'border-[var(--ds-border)] bg-[var(--ds-bg)]'">
          <p class="text-xl font-extrabold tabular-nums" :class="liveExam.alertCount > 0 ? 'text-[var(--ds-danger)]' : 'text-[var(--ds-text)]'">{{ liveExam.alertCount || 0 }}</p>
          <p class="text-[10px] font-semibold uppercase tracking-wider text-[var(--ds-text-muted)]">Cảnh báo</p>
        </div>
      </div>

      <!-- Progress bar -->
      <div v-if="liveExam.durationMinutes" class="mb-4">
        <div class="mb-1.5 flex justify-between text-xs">
          <span class="font-semibold text-[var(--ds-text-muted)]">Tiến độ chung</span>
          <span class="font-bold text-[var(--ds-primary)]">{{ progressPercent }}%</span>
        </div>
        <div class="h-2 w-full overflow-hidden rounded-full bg-[var(--ds-gray-100)]">
          <div
            class="h-full rounded-full bg-gradient-to-r from-[var(--ds-primary)] to-[var(--ds-primary-hover)] transition-all duration-500"
            :style="{ width: `${progressPercent}%` }"
          />
        </div>
        <p class="mt-1 text-right text-[11px] text-[var(--ds-text-muted)]">
          Còn {{ remainingTimeLabel }} — {{ liveExam.durationMinutes }} phút tổng
        </p>
      </div>

      <!-- CTA -->
      <button
        type="button"
        class="ds-btn-monitor inline-flex w-full items-center justify-center gap-2 rounded-[var(--ds-radius-lg)] bg-[var(--ds-accent)] px-4 py-3 text-sm font-bold text-white shadow-sm transition-all hover:-translate-y-0.5 hover:shadow-md"
        @click="$emit('go-monitoring-exam', liveExam)"
      >
        <LucideIcon name="monitoring" size="18" />
        Giám sát ngay
        <span v-if="liveExam.alertCount > 0" class="ml-1 flex size-5 items-center justify-center rounded-full bg-white/20 text-[11px] font-bold">
          {{ liveExam.alertCount }}
        </span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  liveExam: {
    type: Object,
    default: null
  }
})

defineEmits(['go-monitoring', 'go-monitoring-exam'])

const hasLive = computed(() => props.liveExam && props.liveExam.isLive)

const formatTime = (iso) => {
  if (!iso) return '—'
  return new Date(iso).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

const elapsedLabel = computed(() => {
  if (!props.liveExam?.startedAt) return ''
  const elapsed = Math.floor((Date.now() - new Date(props.liveExam.startedAt).getTime()) / 60000)
  if (elapsed < 60) return `${elapsed} phút trước`
  return `${Math.floor(elapsed / 60)}h ${elapsed % 60}m trước`
})

const progressPercent = computed(() => {
  if (!props.liveExam?.startedAt || !props.liveExam?.durationMinutes) return 0
  const elapsed = (Date.now() - new Date(props.liveExam.startedAt).getTime()) / 60000
  return Math.min(100, Math.round((elapsed / props.liveExam.durationMinutes) * 100))
})

const remainingTimeLabel = computed(() => {
  if (!props.liveExam?.startedAt || !props.liveExam?.durationMinutes) return '—'
  const remaining = Math.max(0, props.liveExam.durationMinutes - (Date.now() - new Date(props.liveExam.startedAt).getTime()) / 60000)
  return `${Math.round(remaining)} phút`
})
</script>


<style scoped>
.ds-live-badge {
  background: var(--ds-accent-bg);
  color: var(--ds-accent);
  border: 1px solid rgba(245, 158, 11, 0.25);
}

.ds-btn-monitor {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}
.ds-btn-monitor:hover {
  background: linear-gradient(135deg, #d97706 0%, #b45309 100%);
}
</style>
