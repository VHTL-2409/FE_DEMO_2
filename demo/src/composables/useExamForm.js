/**
 * useExamForm — shared form state, validation, and dirty tracking
 * for the teacher exam detail/edit page.
 *
 * Usage:
 *   const { form, errors, isDirty, isValid, validate, reset, markSaved } = useExamForm()
 */

import { reactive, computed, watch } from 'vue'

// ─── Validation helpers ───────────────────────────────────────────────────────

export function validateOverview(form) {
  const errors = {}
  if (!form.title || form.title.trim().length < 3) {
    errors.title = 'Tiêu đề phải có ít nhất 3 ký tự'
  }
  if (form.title && form.title.trim().length > 200) {
    errors.title = 'Tiêu đề không được quá 200 ký tự'
  }
  if (!form.durationMinutes || form.durationMinutes < 5) {
    errors.durationMinutes = 'Thời lượng tối thiểu là 5 phút'
  }
  if (form.maxAttempts < 1) {
    errors.maxAttempts = 'Số lần thi tối thiểu là 1'
  }
  return errors
}

export function validateSchedule(form) {
  const errors = {}
  if (!form.startTime && !form.endTime) {
    // 24/7 mode — OK
    return errors
  }
  if (form.startTime && form.endTime) {
    const start = new Date(form.startTime)
    const end = new Date(form.endTime)
    if (end <= start) {
      errors.endTime = 'Thời gian kết thúc phải sau thời gian bắt đầu'
    }
  } else if (form.startTime && !form.endTime) {
    errors.endTime = 'Vui lòng đặt thời gian kết thúc'
  } else if (!form.startTime && form.endTime) {
    errors.startTime = 'Vui lòng đặt thời gian bắt đầu'
  }
  return errors
}

export function validateQuestions(questions) {
  const errors = {}
  if (!questions || questions.length === 0) {
    errors.questions = 'Cần ít nhất 1 câu hỏi để xuất bản'
  }
  return errors
}

export function validateAll(form, questions) {
  return {
    ...validateOverview(form),
    ...validateSchedule(form),
    ...validateQuestions(questions)
  }
}

// ─── Composable factory ───────────────────────────────────────────────────────

export function useExamForm(options = {}) {
  const {
    onDirtyChange = null,
    autoSaveInterval = 0 // milliseconds; 0 = disabled
  } = options

  // ── Form state
  const form = reactive({
    title: '',
    subject: '',
    className: '',
    description: '',
    durationMinutes: 60,
    shuffleQuestions: false,
    shuffleAnswers: false,
    maxAttempts: 1,
    startTime: '',
    endTime: '',
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    proctoringEnabled: true,
    monitorTabSwitch: true,
    monitorBlur: true,
    monitorExitFullscreen: true,
    monitorCopyPaste: true,
    monitorIdleTime: true,
    monitorDevtools: true,
    monitorDuplicateIp: true,
    monitorFastSubmit: true,
    monitorRightClick: true,
    monitorPrintScreen: true,
    monitorRapidQuestionSwitch: true,
    monitorMultiMonitor: true,
    requireCameraMic: true,
    questions: []
  })

  // ── Errors
  const errors = reactive({})

  // ── Dirty tracking
  let _isDirty = false
  const isDirty = computed(() => _isDirty)

  const markDirty = () => {
    _isDirty = true
    if (onDirtyChange) onDirtyChange(true)
  }

  const markSaved = () => {
    _isDirty = false
    if (onDirtyChange) onDirtyChange(false)
  }

  // Watch all form fields for changes
  watch(
    form,
    () => { markDirty() },
    { deep: true }
  )

  watch(
    () => form.questions,
    () => { markDirty() },
    { deep: true }
  )

  // ── Validation
  const isValid = computed(() => {
    const allErrors = validateAll(form, form.questions)
    return Object.keys(allErrors).length === 0
  })

  const hasErrors = computed(() => Object.keys(errors).length > 0)

  const validate = (tab = 'all') => {
    // Clear previous errors
    Object.keys(errors).forEach(k => delete errors[k])

    let tabErrors = {}
    if (tab === 'all' || tab === 'overview') {
      Object.assign(tabErrors, validateOverview(form))
    }
    if (tab === 'all' || tab === 'schedule') {
      Object.assign(tabErrors, validateSchedule(form))
    }
    if (tab === 'all' || tab === 'questions') {
      Object.assign(tabErrors, validateQuestions(form.questions))
    }

    Object.assign(errors, tabErrors)
    return Object.keys(errors).length === 0
  }

  const validateOverviewTab = () => validate('overview')
  const validateScheduleTab = () => validate('schedule')
  const validateQuestionsTab = () => validate('questions')

  // ── Reset
  const reset = (data = {}) => {
    Object.assign(form, {
      title: data.title || '',
      subject: data.subject || '',
      className: data.className || '',
      description: data.description || '',
      durationMinutes: data.durationMinutes || 60,
      shuffleQuestions: data.shuffleQuestions || false,
      shuffleAnswers: data.shuffleAnswers || false,
      maxAttempts: data.maxAttempts || 1,
      startTime: data.startTime || '',
      endTime: data.endTime || '',
      timezone: data.timezone || Intl.DateTimeFormat().resolvedOptions().timeZone,
      proctoringEnabled: data.monitorTabSwitch || data.monitorBlur || data.monitorDevtools,
      monitorTabSwitch: data.monitorTabSwitch !== false,
      monitorBlur: data.monitorBlur !== false,
      monitorExitFullscreen: data.monitorExitFullscreen !== false,
      monitorCopyPaste: data.monitorCopyPaste !== false,
      monitorIdleTime: data.monitorIdleTime !== false,
      monitorDevtools: data.monitorDevtools !== false,
      monitorDuplicateIp: data.monitorDuplicateIp !== false,
      monitorFastSubmit: data.monitorFastSubmit !== false,
      monitorRightClick: data.monitorRightClick !== false,
      monitorPrintScreen: data.monitorPrintScreen !== false,
      monitorRapidQuestionSwitch: data.monitorRapidQuestionSwitch !== false,
      monitorMultiMonitor: data.monitorMultiMonitor !== false,
      requireCameraMic: data.requireCameraMic !== false,
      questions: data.questions || []
    })
    // Clear errors
    Object.keys(errors).forEach(k => delete errors[k])
    markSaved()
  }

  // ── Auto-save (optional)
  let _autoSaveTimer = null

  const startAutoSave = async (saveFn, interval = autoSaveInterval) => {
    if (!interval || interval <= 0) return
    _autoSaveTimer = setInterval(async () => {
      if (_isDirty) {
        try {
          await saveFn()
        } catch (err) {
          console.warn('[useExamForm] auto-save failed:', err)
        }
      }
    }, interval)
  }

  const stopAutoSave = () => {
    if (_autoSaveTimer) {
      clearInterval(_autoSaveTimer)
      _autoSaveTimer = null
    }
  }

  // ── Checklist helpers
  const checklistItems = computed(() => {
    const items = []
    const hasTitle = !!form.title && form.title.trim().length >= 3
    const hasQuestions = form.questions.length > 0
    const hasSchedule = !!form.startTime && !!form.endTime
    const hasDuration = !!form.durationMinutes && form.durationMinutes >= 5
    const isPublished = form._isActive

    items.push({ key: 'title', label: 'Có tiêu đề', done: hasTitle })
    items.push({ key: 'questions', label: 'Có câu hỏi', done: hasQuestions })
    items.push({ key: 'schedule', label: 'Có lịch thi', done: hasSchedule })
    items.push({ key: 'duration', label: 'Có thời lượng', done: hasDuration })
    items.push({ key: 'published', label: 'Đã xuất bản', done: isPublished })

    return items
  })

  const checklistProgress = computed(() => {
    if (!checklistItems.value.length) return 0
    return Math.round(
      (checklistItems.value.filter(i => i.done).length / checklistItems.value.length) * 100
    )
  })

  const warnings = computed(() => {
    const w = []
    if (!form.title || form.title.trim().length < 3) {
      w.push({ key: 'title', message: 'Tiêu đề quá ngắn', severity: 'warning' })
    }
    if (form.questions.length === 0) {
      w.push({ key: 'questions', message: 'Chưa có câu hỏi nào', severity: 'danger' })
    }
    if (!form.startTime || !form.endTime) {
      w.push({ key: 'schedule', message: 'Chưa thiết lập lịch thi', severity: 'warning' })
    }
    if (!form.durationMinutes || form.durationMinutes < 5) {
      w.push({ key: 'duration', message: 'Chưa thiết lập thời lượng', severity: 'danger' })
    }
    return w
  })

  return {
    form,
    errors,
    isDirty,
    isValid,
    hasErrors,
    checklistItems,
    checklistProgress,
    warnings,
    validate,
    validateOverviewTab,
    validateScheduleTab,
    validateQuestionsTab,
    reset,
    markDirty,
    markSaved,
    startAutoSave,
    stopAutoSave
  }
}
