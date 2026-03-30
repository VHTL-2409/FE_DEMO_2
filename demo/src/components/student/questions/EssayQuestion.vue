<template>
  <div class="space-y-4">
    <textarea
      :value="normalizedValue"
      :disabled="disabled"
      rows="8"
      class="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-base text-slate-900 shadow-sm outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10 disabled:cursor-not-allowed disabled:bg-slate-100 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100 dark:disabled:bg-slate-800"
      :placeholder="question?.placeholder || question?.metadata?.placeholder || 'Nhập câu trả lời của bạn...'"
      @input="$emit('update:modelValue', $event.target.value)"
    />

    <!-- AI Feedback Panel -->
    <div v-if="showAIFeedback && aiFeedback" class="rounded-xl border border-primary/20 bg-gradient-to-br from-primary/5 to-violet-500/5 p-4 space-y-3">
      <div class="flex items-center gap-2 pb-2 border-b border-primary/10">
        <div class="size-6 rounded-md bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center">
          <svg class="size-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
        </div>
        <h4 class="text-sm font-bold text-primary">Phản hồi từ AI</h4>
        <span
          class="ml-auto text-[10px] font-bold px-2 py-0.5 rounded-full"
          :class="{
            'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400': aiFeedback.totalScore >= 8,
            'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400': aiFeedback.totalScore >= 5 && aiFeedback.totalScore < 8,
            'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400': aiFeedback.totalScore < 5
          }"
        >
          {{ aiFeedback.totalScore }}/{{ aiFeedback.maxScore }} - {{ aiFeedback.grade }}
        </span>
      </div>

      <!-- Score Bar -->
      <div class="space-y-1.5">
        <div class="flex justify-between text-xs">
          <span class="text-slate-600 dark:text-slate-400">Điểm tổng</span>
          <span class="font-bold text-primary">{{ aiFeedback.totalScore }}/{{ aiFeedback.maxScore }}</span>
        </div>
        <div class="h-2 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
          <div
            class="h-full rounded-full transition-all duration-500"
            :class="{
              'bg-green-500': aiFeedback.totalScore >= 8,
              'bg-amber-500': aiFeedback.totalScore >= 5 && aiFeedback.totalScore < 8,
              'bg-red-500': aiFeedback.totalScore < 5
            }"
            :style="{ width: `${(aiFeedback.totalScore / aiFeedback.maxScore) * 100}%` }"
          ></div>
        </div>
      </div>

      <!-- Criteria Scores -->
      <div v-if="aiFeedback.criteriaScores && aiFeedback.criteriaScores.length > 0" class="space-y-2">
        <p class="text-xs font-semibold text-slate-500 uppercase tracking-wider">Điểm theo tiêu chí</p>
        <div class="grid grid-cols-2 gap-2">
          <div
            v-for="criteria in aiFeedback.criteriaScores"
            :key="criteria.criterion"
            class="bg-white dark:bg-slate-800/50 rounded-lg p-2.5"
          >
            <div class="flex justify-between items-center mb-1">
              <span class="text-xs font-medium text-slate-600 dark:text-slate-400">{{ criteria.criterion }}</span>
              <span class="text-xs font-bold text-primary">{{ criteria.score }}/{{ criteria.maxScore }}</span>
            </div>
            <p class="text-[11px] text-slate-500 dark:text-slate-500 line-clamp-2">{{ criteria.feedback }}</p>
          </div>
        </div>
      </div>

      <!-- Overall Feedback -->
      <div v-if="aiFeedback.overallFeedback" class="space-y-1.5">
        <p class="text-xs font-semibold text-slate-500 uppercase tracking-wider">Nhận xét chung</p>
        <p class="text-sm text-slate-700 dark:text-slate-300">{{ aiFeedback.overallFeedback }}</p>
      </div>

      <!-- Improvements -->
      <div v-if="aiFeedback.improvements && aiFeedback.improvements.length > 0" class="space-y-1.5">
        <p class="text-xs font-semibold text-slate-500 uppercase tracking-wider">Đề xuất cải thiện</p>
        <ul class="space-y-1">
          <li v-for="(improvement, idx) in aiFeedback.improvements" :key="idx" class="flex items-start gap-2 text-sm text-slate-600 dark:text-slate-400">
            <svg class="size-4 text-violet-500 mt-0.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
            </svg>
            {{ improvement }}
          </li>
        </ul>
      </div>

      <!-- Loading State -->
      <div v-if="isEvaluating" class="flex items-center gap-2 text-sm text-slate-500 py-2">
        <svg class="size-4 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        <span>AI đang đánh giá bài viết...</span>
      </div>

      <!-- Evaluate Button -->
      <button
        v-if="!isEvaluating && !aiFeedback"
        @click="$emit('request-ai-evaluation')"
        class="w-full py-2 rounded-lg border border-dashed border-violet-300 dark:border-violet-700 text-violet-600 dark:text-violet-400 text-sm font-medium hover:bg-violet-50 dark:hover:bg-violet-900/20 transition-colors flex items-center justify-center gap-2"
      >
        <svg class="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
        </svg>
        Yêu cầu AI đánh giá
      </button>
    </div>

    <!-- Evaluate Button when feedback is hidden -->
    <button
      v-if="!showAIFeedback && !aiFeedback && !isEvaluating && !disabled"
      @click="$emit('request-ai-evaluation')"
      class="py-2 rounded-lg border border-dashed border-violet-300 dark:border-violet-700 text-violet-600 dark:text-violet-400 text-sm font-medium hover:bg-violet-50 dark:hover:bg-violet-900/20 transition-colors flex items-center justify-center gap-2"
    >
      <svg class="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
      </svg>
      Yêu cầu AI đánh giá
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  question: {
    type: Object,
    default: () => ({})
  },
  modelValue: {
    type: String,
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  },
  aiFeedback: {
    type: Object,
    default: null
  },
  isEvaluating: {
    type: Boolean,
    default: false
  },
  showAIFeedback: {
    type: Boolean,
    default: false
  }
})

defineEmits(['update:modelValue', 'request-ai-evaluation'])

const normalizedValue = computed(() => String(props.modelValue || ''))
</script>
