<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-lg font-bold ln-heading-display" style="color: var(--ln-text);">Ky thi sap toi</h3>
      <button class="ln-btn ln-btn--ghost ln-btn--sm" @click="$emit('see-all')">
        Tat ca
        <span class="material-symbols-outlined text-base">arrow_forward</span>
      </button>
    </div>

    <div v-if="upcomingExams.length === 0" class="ln-card p-6 text-center" style="border: 1px solid var(--ln-border);">
      <div class="ln-empty__icon mx-auto mb-3" style="width: 3rem; height: 3rem;">
        <span class="material-symbols-outlined text-xl" style="color: var(--ln-text-muted);">event_available</span>
      </div>
      <p class="font-bold mb-1" style="color: var(--ln-text);">Khong co ky thi nao sap toi</p>
      <p class="text-sm" style="color: var(--ln-text-muted);">Hen gap lai ban than!</p>
    </div>

    <!-- Timeline -->
    <div v-else class="relative">
      <!-- Vertical line -->
      <div class="absolute left-4 top-0 bottom-0 w-px" style="background: linear-gradient(180deg, var(--ln-cyan), var(--ln-violet), transparent);" />

      <div class="space-y-4">
        <div
          v-for="(exam, i) in displayedExams"
          :key="exam.id || i"
          class="timeline-item relative pl-10 ln-reveal"
          :style="{ animationDelay: `${i * 80}ms` }"
        >
          <!-- Timeline dot -->
          <div class="absolute left-2 top-3 w-4 h-4 rounded-full border-2 z-10"
            :style="`background: ${exam.active ? 'var(--ln-cyan)' : 'var(--ln-bg-3)'}; border-color: ${exam.active ? 'var(--ln-cyan)' : 'var(--ln-border)'}; box-shadow: ${exam.active ? 'var(--ln-shadow-cyan)' : 'none'};`"
          >
            <div v-if="exam.active" class="absolute inset-0 rounded-full ln-pulse" style="background: var(--ln-cyan); opacity: 0.3;" />
          </div>

          <!-- Card -->
          <div
            class="ln-card p-4 ln-glass--hover cursor-pointer"
            :class="exam.active ? 'ln-card--cyan' : ''"
            :style="exam.active ? 'border-color: var(--ln-cyan-border);' : 'border: 1px solid var(--ln-border);'"
            @click="$emit('exam-click', exam)"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="flex-1 min-w-0">
                <p class="font-bold truncate" style="color: var(--ln-text);">{{ exam.title || 'Ky thi' }}</p>
                <div class="flex items-center gap-2 mt-1">
                  <span class="material-symbols-outlined text-xs" style="color: var(--ln-text-muted);">schedule</span>
                  <span class="text-xs" style="color: var(--ln-text-secondary);">{{ exam.date }}</span>
                </div>
                <div class="flex items-center gap-2 mt-0.5">
                  <span class="material-symbols-outlined text-xs" style="color: var(--ln-text-muted);">quiz</span>
                  <span class="text-xs" style="color: var(--ln-text-secondary);">{{ exam.questions }} cau hoi</span>
                </div>
              </div>
              <div class="flex-shrink-0 text-right">
                <span v-if="exam.active" class="ln-badge ln-badge--cyan">
                  <span class="ln-badge__dot" />
                  Sap bat dau
                </span>
                <span v-else class="ln-badge ln-badge--neutral">
                  {{ exam.daysLeft }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  upcomingExams: { type: Array, default: () => [] },
  displayLimit: { type: Number, default: 3 }
})

defineEmits(['exam-click', 'see-all'])

const displayedExams = computed(() =>
  props.upcomingExams.slice(0, props.displayLimit)
)
</script>

<style scoped>
.timeline-item::before {
  content: '';
}
</style>
