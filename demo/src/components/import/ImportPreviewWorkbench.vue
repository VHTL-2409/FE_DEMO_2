<template>
  <section class="staff-surface-strong flex min-h-0 flex-1 flex-col overflow-x-hidden rounded-[1.5rem] p-4 dark:bg-slate-900">
    <div class="mb-4 flex flex-col gap-3 border-b border-slate-100 pb-4 dark:border-slate-800 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h3 class="text-base font-bold text-slate-900 dark:text-white">Preview câu hỏi đã parse</h3>
        <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">
          {{ summary?.totalDetected || questions.length }} câu phát hiện • {{ summary?.needsReview || 0 }} mục cần review
        </p>
      </div>
      <div class="flex flex-wrap gap-2 text-xs">
        <span class="rounded-full bg-slate-100 px-3 py-1.5 font-semibold text-slate-600 dark:bg-slate-800 dark:text-slate-300">Method: {{ summary?.parseMethod || '-' }}</span>
        <span class="rounded-full bg-slate-100 px-3 py-1.5 font-semibold text-slate-600 dark:bg-slate-800 dark:text-slate-300">Format: {{ summary?.fileType || '-' }}</span>
      </div>
    </div>

    <div v-if="!questions.length" class="rounded-xl border border-dashed border-slate-200 bg-slate-50 px-4 py-6 text-center text-sm text-slate-500 dark:border-slate-700 dark:bg-slate-800/30 dark:text-slate-300">
      Chưa có câu hỏi nào trong preview.
    </div>

    <div v-else class="flex min-h-0 flex-1 flex-col">
      <div class="portal-scrollbar min-h-0 flex-1 space-y-4 overflow-y-auto overscroll-contain">
      <article
        v-for="question in questions"
        :key="question.index"
        class="rounded-2xl border border-slate-200 bg-slate-50/80 p-4 dark:border-slate-800 dark:bg-slate-800/40"
      >
        <div class="mb-3 flex items-center justify-between gap-3">
          <span class="inline-flex items-center gap-2 rounded-full bg-primary/10 px-3 py-1.5 text-xs font-bold text-primary">Câu {{ question.index }}</span>
          <div class="flex items-center gap-2">
            <select
              :value="question.type || 'SINGLE_CHOICE'"
              :disabled="disabled"
              class="rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-xs font-semibold text-slate-700 outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200"
              @change="updateQuestion(question.index, { type: $event.target.value })"
            >
              <option value="SINGLE_CHOICE">Single</option>
              <option value="MULTIPLE_CHOICE">Multiple</option>
              <option value="FILL_IN_BLANK">Fill blank</option>
              <option value="ESSAY">Essay</option>
              <option value="CODE">Code</option>
            </select>
            <span class="text-xs font-semibold text-slate-500 dark:text-slate-400">Confidence {{ Number(question.parseConfidence || 0).toFixed(2) }}</span>
          </div>
        </div>

        <label class="block text-xs font-semibold text-slate-600 dark:text-slate-300">Nội dung</label>
        <textarea
          :value="question.content"
          :disabled="disabled"
          rows="3"
          class="mt-1 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
          @input="updateQuestion(question.index, { content: $event.target.value })"
        />

        <div v-if="usesOptions(question.type)" class="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">
          <label
            v-for="option in question.options || []"
            :key="`${question.index}-${option.id}`"
            class="block"
          >
            <span class="text-xs font-semibold text-slate-600 dark:text-slate-300">Đáp án {{ option.id }}</span>
            <input
              :value="option.text"
              :disabled="disabled"
              type="text"
              class="mt-1 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
              @input="updateOption(question.index, option.id, $event.target.value)"
            />
          </label>
        </div>

        <div class="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
          <label class="block">
            <span class="text-xs font-semibold text-slate-600 dark:text-slate-300">Đáp án đúng</span>
            <input
              :value="question.correctAnswer"
              :disabled="disabled"
              type="text"
              class="mt-1 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
              @input="updateQuestion(question.index, { correctAnswer: normalizeCorrectAnswerInput(question, $event.target.value) })"
            />
          </label>
          <label class="block">
            <span class="text-xs font-semibold text-slate-600 dark:text-slate-300">Điểm</span>
            <input
              :value="question.scoreWeight"
              :disabled="disabled"
              type="number"
              min="0.1"
              step="0.1"
              class="mt-1 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
              @input="updateQuestion(question.index, { scoreWeight: Number($event.target.value) || 1 })"
            />
          </label>
          <label class="block">
            <span class="text-xs font-semibold text-slate-600 dark:text-slate-300">Độ khó</span>
            <select
              :value="question.difficulty || 'MEDIUM'"
              :disabled="disabled"
              class="mt-1 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
              @change="updateQuestion(question.index, { difficulty: $event.target.value })"
            >
              <option value="EASY">EASY</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HARD">HARD</option>
            </select>
          </label>
        </div>
      </article>
      </div>
    </div>
  </section>
</template>

<script setup>
const props = defineProps({
  summary: {
    type: Object,
    default: () => ({})
  },
  questions: {
    type: Array,
    default: () => []
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update-question'])

const updateQuestion = (questionIndex, patch) => {
  emit('update-question', { questionIndex, patch })
}

const updateOption = (questionIndex, optionId, value) => {
  const question = props.questions.find((item) => item.index === questionIndex)
  if (!question) return
  const nextOptions = (question.options || []).map((option) => (
    option.id === optionId ? { ...option, text: value } : option
  ))
  emit('update-question', { questionIndex, patch: { options: nextOptions } })
}

const usesOptions = (type) => ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'MATCHING', 'ORDERING'].includes(String(type || 'SINGLE_CHOICE').toUpperCase())
const normalizeCorrectAnswerInput = (question, value) => usesOptions(question?.type) ? String(value || '').toUpperCase() : String(value || '')
</script>
