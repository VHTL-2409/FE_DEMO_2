<template>
  <div class="space-y-4">
    <!-- Tab chon nguon -->
    <div class="flex rounded-xl border border-slate-200 bg-slate-50 p-1 dark:border-slate-700 dark:bg-slate-800/50">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        class="flex-1 rounded-lg px-3 py-2 text-sm font-semibold transition-all duration-200"
        :class="activeTab === tab.value
          ? 'bg-white text-primary shadow-sm dark:bg-slate-900 dark:text-primary'
          : 'text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200'"
        type="button"
        @click="activeTab = tab.value"
      >
        <LucideIcon :name="tab.icon" size="18" />
        {{ tab.label }}
      </button>
    </div>

    <!-- Tab File: Upload nhu cu -->
    <div v-if="activeTab === 'file'">
      <slot name="file-tab">
        <!-- Noi dung file tab duoc truyen vao tu component cha -->
      </slot>
    </div>

    <!-- Tab Azota -->
    <div v-if="activeTab === 'azota'" class="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-soft dark:border-slate-700/80 dark:bg-slate-900 sm:p-6">
      <AzotaQuestionBankList
        @questions-fetched="onAzotaQuestionsFetched"
        @loading-change="loading => emit('loading-change', loading)"
      />
    </div>

    <!-- Preview azota -->
    <div v-if="azotaQuestions.length > 0" class="mt-4">
      <div class="mb-3 flex items-center justify-between">
        <div>
          <h4 class="text-base font-bold text-slate-900 dark:text-white">Preview cau hoi tu Azota</h4>
          <p class="text-xs text-slate-500 dark:text-slate-400">{{ azotaQuestions.length }} cau hoi tu Azota</p>
        </div>
        <button
          type="button"
          class="text-xs font-semibold text-slate-500 hover:text-red-500 transition-colors"
          @click="azotaQuestions = []"
        >
          Xoa het
        </button>
      </div>
      <ImportPreviewWorkbench
        :summary="{ totalDetected: azotaQuestions.length, needsReview: 0, fileType: 'azota', parseMethod: 'azota-api' }"
        :questions="azotaQuestions"
        :disabled="previewDisabled"
        @update-question="onUpdateQuestion"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import AzotaQuestionBankList from './AzotaQuestionBankList.vue'
import ImportPreviewWorkbench from './ImportPreviewWorkbench.vue'

const props = defineProps({
  previewDisabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['questions-fetched', 'loading-change'])

const tabs = [
  { value: 'file', label: 'Tu tap tin', icon: 'upload_file' },
  { value: 'azota', label: 'Tu Azota.vn', icon: 'school' }
]
const activeTab = ref('file')

const azotaQuestions = ref([])

const onAzotaQuestionsFetched = (questions) => {
  azotaQuestions.value = questions.map((q, i) => ({
    index: i + 1,
    content: q.content || '',
    type: q.type || 'SINGLE_CHOICE',
    options: (q.options || []).map(o => ({
      id: o.id || o.key || String.fromCharCode(65 + (o.index || 0)),
      text: o.text || o.value || ''
    })),
    correctAnswer: q.correctAnswer || 'A',
    scoreWeight: q.scoreWeight || 1.0,
    difficulty: q.difficulty || 'MEDIUM',
    metadata: q.metadata || null,
    attachments: null,
    parseConfidence: 1.0
  }))
  emit('questions-fetched', azotaQuestions.value)
}

const onUpdateQuestion = ({ questionIndex, patch }) => {
  const idx = azotaQuestions.value.findIndex(q => q.index === questionIndex)
  if (idx !== -1) {
    azotaQuestions.value[idx] = { ...azotaQuestions.value[idx], ...patch }
  }
}
</script>

