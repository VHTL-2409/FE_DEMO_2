import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const useImportJobStore = defineStore('importJob', () => {
  const jobId = ref(null)
  const status = ref('IDLE')
  const progress = ref(0)
  const summary = ref(null)
  const questions = ref([])
  const editedQuestions = ref({})
  const issues = ref([])

  const reviewedQuestions = computed(() =>
    questions.value.map((question, index) => ({
      ...question,
      ...(editedQuestions.value[index] || {})
    }))
  )

  const hydratePreview = ({ jobId: nextJobId, status: nextStatus, progress: nextProgress, summary: nextSummary, questions: nextQuestions, issues: nextIssues }) => {
    jobId.value = nextJobId || null
    status.value = nextStatus || 'DONE'
    progress.value = typeof nextProgress === 'number' ? nextProgress : 100
    summary.value = nextSummary || null
    questions.value = Array.isArray(nextQuestions) ? nextQuestions : []
    issues.value = Array.isArray(nextIssues) ? nextIssues : []
  }

  const patchQuestion = (index, patch) => {
    editedQuestions.value = {
      ...editedQuestions.value,
      [index]: {
        ...(editedQuestions.value[index] || {}),
        ...(patch || {})
      }
    }
  }

  const markIssueResolved = (issueId, resolved = true) => {
    issues.value = issues.value.map((issue) => (
      issue.id === issueId
        ? { ...issue, resolved }
        : issue
    ))
  }

  const reset = () => {
    jobId.value = null
    status.value = 'IDLE'
    progress.value = 0
    summary.value = null
    questions.value = []
    editedQuestions.value = {}
    issues.value = []
  }

  return {
    jobId,
    status,
    progress,
    summary,
    questions,
    editedQuestions,
    issues,
    reviewedQuestions,
    hydratePreview,
    patchQuestion,
    markIssueResolved,
    reset
  }
})
