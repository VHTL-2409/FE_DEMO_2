import { ref } from 'vue'

/**
 * Debounce lưu nháp server + localStorage đồng bộ ngay.
 * @param {object} options
 * @param {() => object} options.getAnswers
 * @param {() => number|null} options.getAttemptId
 * @param {() => Promise<void>} options.saveToServer
 * @param {number} [options.debounceMs]
 */
export function useAutoSaveDraft({ getAnswers, getAttemptId, saveToServer, debounceMs = 2000 }) {
  const saveStatus = ref('idle')
  const lastSavedAt = ref(null)
  const hasPendingChanges = ref(false)
  let debounceTimer = null

  const storageKey = () => {
    const id = getAttemptId()
    return id ? `exam_draft_${id}` : null
  }

  const persistLocal = () => {
    const key = storageKey()
    if (!key) return
    try {
      const answers = getAnswers()
      localStorage.setItem(
        key,
        JSON.stringify({
          answers,
          savedAt: Date.now()
        })
      )
    } catch {
      /* quota / private mode */
    }
  }

  const flushServer = async () => {
    if (!getAttemptId()) return
    saveStatus.value = 'saving'
    try {
      await saveToServer()
      lastSavedAt.value = new Date().toISOString()
      hasPendingChanges.value = false
      saveStatus.value = 'saved'
      window.setTimeout(() => {
        if (saveStatus.value === 'saved') {
          saveStatus.value = 'idle'
        }
      }, 4000)
    } catch {
      saveStatus.value = 'error'
    }
  }

  const schedule = () => {
    persistLocal()
    hasPendingChanges.value = true
    if (debounceTimer) {
      window.clearTimeout(debounceTimer)
    }
    debounceTimer = window.setTimeout(() => {
      debounceTimer = null
      void flushServer()
    }, debounceMs)
  }

  const forceSave = async () => {
    if (debounceTimer) {
      window.clearTimeout(debounceTimer)
      debounceTimer = null
    }
    persistLocal()
    hasPendingChanges.value = true
    await flushServer()
  }

  /**
   * Merge local backup lên object đáp án từ server.
   * Ưu tiên dữ liệu server; local chỉ lấp vào các câu server chưa có.
   */
  const mergeLocalIntoAnswers = (serverMap) => {
    const key = storageKey()
    if (!key) return serverMap
    try {
      const raw = localStorage.getItem(key)
      if (!raw) return serverMap
      const parsed = JSON.parse(raw)
      if (!parsed?.answers || typeof parsed.answers !== 'object') return serverMap
      return { ...parsed.answers, ...serverMap }
    } catch {
      return serverMap
    }
  }

  return {
    saveStatus,
    lastSavedAt,
    hasPendingChanges,
    schedule,
    forceSave,
    mergeLocalIntoAnswers
  }
}
