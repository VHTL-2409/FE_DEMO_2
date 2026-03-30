<template>
  <div
    :class="[
      'submit-modal portal-panel fixed inset-0 z-50 flex items-center justify-center p-4',
      'bg-slate-950/40 backdrop-blur-sm'
    ]"
    role="dialog"
    aria-modal="true"
    aria-labelledby="submit-title"
    @click.self="$emit('cancel')"
    @keydown.esc="$emit('cancel')"
  >
    <div class="w-full max-w-md rounded-[var(--ds-radius-2xl)] border border-[var(--ds-border)] bg-[var(--ds-surface)] shadow-[var(--ds-shadow-xl)]">
      <!-- Header -->
      <div class="flex items-center justify-between border-b border-[var(--ds-border)] px-6 py-4">
        <div class="flex items-center gap-3">
          <div class="flex size-10 items-center justify-center rounded-[var(--ds-radius-lg)] bg-[var(--ds-primary-soft)]">
            <LucideIcon name="task_alt" />
          </div>
          <div>
            <h2 id="submit-title" class="text-lg font-bold text-[var(--ds-text)]">Xác nhận nộp bài</h2>
            <p class="text-xs text-[var(--ds-text-muted)]">Hành động không thể hoàn tác</p>
          </div>
        </div>
        <button
          type="button"
          class="flex size-8 items-center justify-center rounded-[var(--ds-radius-md)] text-[var(--ds-text-muted)] transition-colors hover:bg-[var(--ds-gray-100)] hover:text-[var(--ds-text)]"
          aria-label="Đóng"
          @click="$emit('cancel')"
        >
          <LucideIcon name="close" />
        </button>
      </div>

      <!-- Body -->
      <div class="px-6 py-5">
        <div class="mb-4 space-y-2.5 rounded-[var(--ds-radius-lg)] border border-[var(--ds-border)] bg-[var(--ds-gray-50)] p-4 text-sm">
          <div class="flex justify-between">
            <span class="text-[var(--ds-text-muted)]">Đã trả lời:</span>
            <span class="font-bold text-[var(--ds-primary)]">{{ answeredCount }} / {{ totalCount }} câu</span>
          </div>
          <div class="flex justify-between">
            <span class="text-[var(--ds-text-muted)]">Chưa trả lời:</span>
            <span class="font-bold" :class="unansweredCount > 0 ? 'text-[var(--ds-danger)]' : 'text-[var(--ds-text)]'">{{ unansweredCount }} câu</span>
          </div>
          <div class="flex justify-between">
            <span class="text-[var(--ds-text-muted)]">Thời gian còn lại:</span>
            <span class="font-bold tabular-nums" :class="timeRemaining < 300 ? 'text-[var(--ds-danger)]' : 'text-[var(--ds-text)]'">{{ timeRemainingLabel }}</span>
          </div>
        </div>

        <p v-if="unansweredCount > 0" class="flex items-start gap-2 rounded-[var(--ds-radius-lg)] border border-[rgba(220,38,38,0.15)] bg-[var(--ds-danger-bg)] p-3 text-sm">
          <LucideIcon name="warning" size="18" />
          <span class="text-[var(--ds-text)]">Bạn còn <strong>{{ unansweredCount }} câu chưa trả lời</strong>. Nộp bài sẽ không thể sửa lại.</span>
        </p>
        <p v-else class="flex items-start gap-2 rounded-[var(--ds-radius-lg)] border border-[rgba(22,163,74,0.15)] bg-[var(--ds-success-bg)] p-3 text-sm">
          <LucideIcon name="check_circle" size="18" />
          <span class="text-[var(--ds-text)]">Bạn đã trả lời tất cả câu hỏi. Sẵn sàng nộp bài!</span>
        </p>
      </div>

      <!-- Footer -->
      <div class="flex items-center justify-end gap-3 border-t border-[var(--ds-border)] px-6 py-4">
        <button
          type="button"
          class="rounded-[var(--ds-radius-lg)] border border-[var(--ds-border)] px-5 py-2.5 text-sm font-semibold text-[var(--ds-text-secondary)] transition-colors hover:bg-[var(--ds-gray-100)]"
          :disabled="loading"
          @click="$emit('cancel')"
        >
          Tiếp tục làm bài
        </button>
        <button
          type="button"
          class="inline-flex items-center gap-2 rounded-[var(--ds-radius-lg)] bg-[var(--ds-primary)] px-5 py-2.5 text-sm font-bold text-white transition-colors hover:bg-[var(--ds-primary-hover)] disabled:opacity-60"
          :disabled="loading"
          @click="$emit('confirm')"
        >
          <LucideIcon name="hourglass_empty" size="18" />
          {{ loading ? 'Đang nộp...' : 'Nộp bài ngay' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  answeredCount: { type: Number, default: 0 },
  totalCount: { type: Number, default: 0 },
  unansweredCount: { type: Number, default: 0 },
  timeRemaining: { type: Number, default: 0 },
  loading: { type: Boolean, default: false }
})

defineEmits(['confirm', 'cancel'])

const timeRemainingLabel = computed(() => {
  const m = Math.floor(props.timeRemaining / 60)
  const s = props.timeRemaining % 60
  return `${m}:${String(s).padStart(2, '0')}`
})
</script>

