<template>
  <div class="ec-page ds-animate-fade-up">
    <!-- Body: top header strip + form columns -->
    <div class="ec-body">

      <!-- Page header: breadcrumb + save status + action buttons -->
      <div class="ec-page-header tui-panel--anim">
        <div class="ec-page-header__breadcrumb">
          <RouterLink class="ec-page-header__bc-link" to="/teacher/exams/list">
            <LucideIcon name="assignment" />
            Đề thi
          </RouterLink>
          <LucideIcon name="chevron_right" />
          <span class="ec-page-header__bc-current">Tạo đề thi</span>
        </div>
        <div class="ec-page-header__actions">
          <div class="ec-page-header__save-status" :class="saveStatusClass">
            <LucideIcon :name="saveStatusIcon" />
            <span>{{ saveStatusLabel }}</span>
          </div>
          <button type="button" class="ec-btn ec-btn--ghost tui-btn tui-btn--ghost" @click="handleSaveDraft">
            <LucideIcon name="save" />
            <span>Lưu nháp</span>
          </button>
          <button type="button" class="ec-btn ec-btn--primary tui-btn tui-btn--primary" @click="handlePublish" :disabled="!canPublish">
            <LucideIcon name="rocket_launch" />
            <span>Xuất bản</span>
          </button>
        </div>
      </div>

      <div class="ec-body__inner">

        <!-- Left column: Form sections -->
        <div class="ec-form-col">
          <!-- Scrollable content wrapper -->
          <div class="ec-form-scroll">

          <!-- Progress steps -->
          <div class="ec-steps tui-panel tui-panel--anim" style="animation-delay: 0.05s">
            <button
              v-for="(step, i) in steps"
              :key="step.id"
              type="button"
              :class="['ec-steps__item', activeStep === step.id && 'ec-steps__item--active', isStepComplete(step.id) && 'ec-steps__item--done', activeStep === step.id ? 'ec-steps__item--active' : '']"
              @click="activeStep = step.id"
            >
              <span class="ec-steps__num">
                <LucideIcon name="check" v-if="isStepComplete(step.id)" />
                <span v-else>{{ i + 1 }}</span>
              </span>
              <span class="ec-steps__label">{{ step.label }}</span>
            </button>
          </div>

          <!-- Section: Thông tin đề thi -->
          <ExamInfoSection
            v-show="activeStep === 'info'"
            v-model:title="form.title"
            v-model:subject="form.subject"
            v-model:description="form.description"
            :exam-type="form.examType"
            v-model:class-id="form.classId"
            :available-classes="availableClasses"
            :is-loading-classes="isLoadingClasses"
          />

          <!-- Section: Cài đặt (gộp Cấu hình + Lịch thi) -->
          <div v-show="activeStep === 'settings'" class="ec-merged-section">
            <ExamConfigSection
              v-model:duration="form.durationMinutes"
              v-model:showAnswers="form.showAnswersAfterEnd"
              v-model:allowReview="form.allowReview"
              v-model:maxAttempts="form.maxAttempts"
            />
            <ScheduleSection
              v-model:startTime="form.startTime"
              v-model:endTime="form.endTime"
              v-model:timezone="form.timezone"
              v-model:durationMinutes="form.durationMinutes"
            />
          </div>

          <!-- Bước riêng: chỉ nhập file / đọc câu hỏi -->
          <QuestionImportStep
            v-show="activeStep === 'import'"
            v-model:questions="form.questions"
            v-model:shuffleQuestions="form.shuffleQuestions"
            v-model:shuffleAnswers="form.shuffleAnswers"
          />

          <!-- Bước riêng: chỉnh sửa danh sách câu hỏi -->
          <QuestionReviewStep
            v-show="activeStep === 'edit'"
            v-model:questions="form.questions"
            @back-to-import="goToImportStep"
          />

          <!-- Section: Giám sát -->
          <ProctoringSection
            v-show="activeStep === 'proctor'"
            v-model:enabled="form.proctoringEnabled"
            v-model:monitorTabSwitch="form.monitorTabSwitch"
            v-model:monitorBlur="form.monitorBlur"
            v-model:monitorExitFullscreen="form.monitorExitFullscreen"
            v-model:monitorCopyPaste="form.monitorCopyPaste"
            v-model:monitorIdleTime="form.monitorIdleTime"
            v-model:monitorDevtools="form.monitorDevtools"
            v-model:monitorDuplicateIp="form.monitorDuplicateIp"
            v-model:monitorFastSubmit="form.monitorFastSubmit"
            v-model:monitorRightClick="form.monitorRightClick"
            v-model:monitorPrintScreen="form.monitorPrintScreen"
            v-model:monitorRapidQuestionSwitch="form.monitorRapidQuestionSwitch"
            v-model:monitorMultiMonitor="form.monitorMultiMonitor"
            v-model:requireCameraMic="form.requireCameraMic"
          />

          </div><!-- end .ec-form-scroll -->

          <!-- Navigation between steps — always visible at bottom -->
          <div class="ec-form-nav">
            <button
              v-if="!isFirstStep"
              type="button"
              class="ec-btn ec-btn--outline ec-btn--sm"
              @click="prevStep"
            >
              <LucideIcon name="arrow_back" />
              Quay lại
            </button>
            <div v-else />

            <button
              v-if="!isLastStep"
              type="button"
              class="ec-btn ec-btn--primary ec-btn--sm"
              @click="nextStep"
            >
              Tiếp theo
              <LucideIcon name="arrow_forward" />
            </button>
            <button
              v-else
              type="button"
              class="ec-btn ec-btn--success ec-btn--sm"
              :disabled="!canPublish"
              @click="handlePublish"
            >
              <LucideIcon name="rocket_launch" />
              Xuất bản đề thi
            </button>
          </div>

        </div>

        <!-- Right column: Preview + checklist + tips -->
        <div class="ec-preview-col">
          <ExamPreviewPanel
            :form="form"
            :active-step="activeStep"
            :save-status="saveState"
            @navigate-to="jumpToStep"
            @preview-exam="showPreviewModal = true"
          />
        </div>
      </div>
    </div>

    <!-- Preview modal -->
    <Teleport to="body">
      <Transition name="ec-modal">
        <div v-if="showPreviewModal" class="ec-modal-overlay" @click.self="showPreviewModal = false">
          <div class="ec-modal">
            <div class="ec-modal__header">
              <h2 class="ec-modal__title">Xem trước đề thi</h2>
              <button type="button" class="ec-modal__close" @click="showPreviewModal = false">
                <LucideIcon name="close" />
              </button>
            </div>
            <div class="ec-modal__body">
              <div class="ec-modal__preview-content">
                <h3 class="ec-preview-exam-title">{{ form.title || 'Tiêu đề đề thi' }}</h3>
                <p class="ec-preview-exam-meta">{{ form.examType === 'private' ? className : form.subject }}</p>
                <div class="ec-preview-exam-info">
                  <span class="ec-preview-exam-info-item">
                    <LucideIcon name="timer" />
                    {{ form.durationMinutes }} phút
                  </span>
                  <span class="ec-preview-exam-info-item">
                    <LucideIcon name="quiz" />
                    {{ form.questions.length }} câu
                  </span>
                  <span v-if="form.proctoringEnabled" class="ec-preview-exam-info-item ec-preview-exam-info-item--alert">
                    <LucideIcon name="shield" />
                    Có giám sát
                  </span>
                </div>
                <p class="ec-preview-exam-desc">{{ form.description || 'Không có mô tả' }}</p>
              </div>
            </div>
            <div class="ec-modal__footer">
              <button type="button" class="ec-btn ec-btn--outline" @click="showPreviewModal = false">
                Đóng
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Draft saved toast (inline feedback) -->
    <Transition name="ec-toast">
      <div v-if="showSavedToast" class="ec-toast">
        <LucideIcon name="check_circle" />
        Đã lưu nháp
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, reactive, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ApiError } from '../../../services/apiClient'
import { createExam, updateExam } from '../../../services/examService'
import { persistExamQuestionsFromForm } from '../../../services/questionService'
import { listClasses } from '../../../services/classService'
import { useToast } from '../../../composables/useToast'

import ExamInfoSection from './ExamInfoSection.vue'
import ExamConfigSection from './ExamConfigSection.vue'
import QuestionImportStep from './QuestionImportStep.vue'
import QuestionReviewStep from './QuestionReviewStep.vue'
import ScheduleSection from './ScheduleSection.vue'
import ProctoringSection from './ProctoringSection.vue'
import ExamPreviewPanel from './ExamPreviewPanel.vue'

const router = useRouter()
const route = useRoute()
const toast = useToast()

// ─── Steps definition ────────────────────────────────────────
const steps = [
  { id: 'info', label: 'Thông tin' },
  { id: 'settings', label: 'Cài đặt' },
  { id: 'import', label: 'Nhập file' },
  { id: 'edit', label: 'Chỉnh sửa' },
  { id: 'proctor', label: 'Giám sát' }
]

// ─── Form state ───────────────────────────────────────────────
const form = reactive({
  // Info
  title: '',
  subject: '',
  description: '',
  examType: 'free',
  classId: '',
  // Config
  durationMinutes: 60,
  showAnswersAfterEnd: false,
  allowReview: true,
  maxAttempts: 1,
  // Questions
  questions: [],
  shuffleQuestions: false,
  shuffleAnswers: false,
  // Schedule
  startTime: '',
  endTime: '',
  // Proctoring
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
  requireCameraMic: true
})

// Watch proctoringEnabled to reset related settings when disabled
watch(() => form.proctoringEnabled, (enabled) => {
  if (!enabled) {
    form.requireCameraMic = false
    form.monitorTabSwitch = false
    form.monitorBlur = false
    form.monitorExitFullscreen = false
    form.monitorCopyPaste = false
    form.monitorIdleTime = false
    form.monitorDevtools = false
    form.monitorDuplicateIp = false
    form.monitorFastSubmit = false
    form.monitorRightClick = false
    form.monitorPrintScreen = false
    form.monitorRapidQuestionSwitch = false
    form.monitorMultiMonitor = false
  }
})

// Available classes loaded from API
const availableClasses = ref([])
const isLoadingClasses = ref(false)

const loadTeacherClasses = async () => {
  isLoadingClasses.value = true
  try {
    const classes = await listClasses()
    availableClasses.value = classes.map(cls => ({
      id: cls.id,
      name: cls.name || cls.className || `Lớp ${cls.id}`
    }))
  } catch (error) {
    console.warn('Failed to load classes:', error)
    availableClasses.value = []
  } finally {
    isLoadingClasses.value = false
  }
}

// ─── Init from query ─────────────────────────────────────────
onMounted(async () => {
  const type = route.query.type
  if (type === 'free' || type === 'private') {
    form.examType = type
  }
  // Load teacher's classes for private exam selection
  await loadTeacherClasses()
})

// ─── UI state ─────────────────────────────────────────────────
const activeStep = ref('info')
const saveState = ref('idle') // idle | saving | saved | error
const showPreviewModal = ref(false)
const showSavedToast = ref(false)
const createdExamId = ref(null)
const isSubmitting = ref(false)

const isFirstStep = computed(() => activeStep.value === steps[0].id)
const isLastStep = computed(() => activeStep.value === steps[steps.length - 1].id)

const completedSteps = ref(new Set())

const className = computed(() => {
  const cls = availableClasses.value.find(c => c.id == form.classId)
  return cls ? cls.name : ''
})

const isStepComplete = (stepId) => completedSteps.value.has(stepId)

const prevStep = () => {
  const idx = steps.findIndex(s => s.id === activeStep.value)
  if (idx > 0) activeStep.value = steps[idx - 1].id
}

const nextStep = () => {
  const idx = steps.findIndex(s => s.id === activeStep.value)
  if (idx < steps.length - 1) {
    completedSteps.value.add(steps[idx].id)
    activeStep.value = steps[idx + 1].id
  }
}

const jumpToStep = (stepId) => {
  activeStep.value = stepId
}

const goToImportStep = () => {
  activeStep.value = 'import'
  const el = document.querySelector('.ec-form-scroll')
  if (el) {
    el.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

// ─── Save status ───────────────────────────────────────────────
const saveStatusLabel = computed(() => {
  if (saveState.value === 'saving') return 'Đang lưu...'
  if (saveState.value === 'saved') return 'Đã lưu'
  if (saveState.value === 'error') return 'Lỗi lưu'
  return 'Chưa lưu'
})

const saveStatusIcon = computed(() => {
  if (saveState.value === 'saving') return 'progress_activity'
  if (saveState.value === 'saved') return 'check_circle'
  if (saveState.value === 'error') return 'error'
  return 'radio_button_unchecked'
})

const saveStatusClass = computed(() => ({
  'ec-page-header__save-status--saving': saveState.value === 'saving',
  'ec-page-header__save-status--saved': saveState.value === 'saved',
  'ec-page-header__save-status--error': saveState.value === 'error'
}))

// ─── Validation ───────────────────────────────────────────────
const isInfoValid = computed(() => {
  if (form.title.trim().length < 3) return false
  if (form.examType === 'free' && form.subject.trim().length === 0) return false
  if (form.examType === 'private' && !form.classId) return false
  return true
})

const isQuestionsValid = computed(() => form.questions.length > 0)

const isScheduleValid = computed(() => {
  // Flexible/practice mode: no schedule required
  if (!form.startTime && !form.endTime) return true
  // One-sided: flexible (no start+end) = valid, practice (no start) = valid
  if (!form.startTime || !form.endTime) return true
  // Both filled: must be end > start
  const start = new Date(form.startTime)
  const end = new Date(form.endTime)
  return !isNaN(start) && !isNaN(end) && end > start
})

const isConfigValid = computed(() => {
  if (!form.durationMinutes || form.durationMinutes < 5) return false
  return isScheduleValid.value
})

const canPublish = computed(() =>
  isInfoValid.value &&
  isConfigValid.value &&
  isQuestionsValid.value &&
  !isSubmitting.value
)

// ─── Handlers ─────────────────────────────────────────────────
const handleSaveDraft = async () => {
  if (!form.title.trim()) {
    toast.error('Vui lòng nhập tiêu đề đề thi trước khi lưu.')
    activeStep.value = 'info'
    return
  }

  saveState.value = 'saving'
  try {
    const payload = buildExamPayload()
    payload.isActive = false

    if (createdExamId.value) {
      await updateExam(createdExamId.value, payload)
    } else {
      const created = await createExam(payload)
      createdExamId.value = created.id
    }

    saveState.value = 'saved'
    showSavedToast.value = true
    setTimeout(() => { showSavedToast.value = false }, 3000)
  } catch (error) {
    saveState.value = 'error'
    toast.error(error instanceof ApiError ? error.message : 'Không thể lưu nháp.')
  } finally {
    setTimeout(() => { if (saveState.value === 'saved') saveState.value = 'idle' }, 5000)
  }
}

const handlePublish = async () => {
  if (!canPublish.value) {
    if (!isInfoValid.value) {
      toast.error('Vui lòng điền đầy đủ thông tin đề thi.')
      activeStep.value = 'info'
      return
    }
    if (!isConfigValid.value) {
      toast.error('Vui lòng thiết lập thời lượng thi.')
      activeStep.value = 'settings'
      return
    }
    if (!isQuestionsValid.value) {
      toast.error('Vui lòng thêm ít nhất 1 câu hỏi.')
      activeStep.value = 'edit'
      return
    }
    return
  }

  isSubmitting.value = true
  saveState.value = 'saving'

  try {
    const payload = buildExamPayload()
    payload.isActive = true

    let examId = createdExamId.value
    let createdExam = null

    if (!examId) {
      createdExam = await createExam(payload)
      examId = createdExam?.id ?? createdExam?.examId ?? createdExam?.data?.id ?? createdExam?.data?.examId
      if (!examId) {
        throw new Error('Không nhận được ID đề thi từ server')
      }
      createdExamId.value = examId
    } else {
      await updateExam(examId, payload)
    }

    // Đồng bộ câu hỏi từ form lên DB (trước đây chỉ gọi import file nhưng importedFile không bao giờ được gán)
    if (form.questions.length > 0) {
      await persistExamQuestionsFromForm(examId, form.questions)
    }

    // Navigate to waiting room
    toast.success('Xuất bản đề thi thành công!')
    
    // Small delay to ensure toast is shown
    await new Promise(resolve => setTimeout(resolve, 300))
    
    const finalCode = form.examType === 'free'
      ? (createdExam?.code || generateExamCode())
      : null
      
    const query = {
      examId: String(examId),
      title: form.title,
      examType: form.examType,
      durationMinutes: String(form.durationMinutes),
      startAt: form.startTime || '',
      endAt: form.endTime || '',
      questionCount: String(form.questions.length)
    }
    
    if (finalCode) {
      query.examCode = finalCode
    }
    
    try {
      await router.replace({
        path: '/teacher/exams/waiting-room',
        query
      })
    } catch (navErr) {
      toast.error('Không thể chuyển trang. Vui lòng vào thủ công: /teacher/exams/waiting-room?examId=' + examId)
    }
  } catch (error) {
    saveState.value = 'error'
    toast.error(error instanceof ApiError ? error.message : 'Không thể xuất bản đề thi.')
  } finally {
    isSubmitting.value = false
  }
}

function buildExamPayload() {
  const className = form.examType === 'private'
    ? (availableClasses.value.find(c => c.id == form.classId)?.name || null)
    : null
  return {
    title: form.title.trim(),
    description: form.description.trim(),
    className,
    durationMinutes: Number(form.durationMinutes) || 60,
    startTime: form.startTime || null,
    endTime: form.endTime || null,
    isActive: true,
    monitorTabSwitch: form.monitorTabSwitch,
    monitorBlur: form.monitorBlur,
    monitorExitFullscreen: form.monitorExitFullscreen,
    monitorCopyPaste: form.monitorCopyPaste,
    monitorIdleTime: form.monitorIdleTime,
    monitorDevtools: form.monitorDevtools,
    monitorDuplicateIp: form.monitorDuplicateIp,
    monitorFastSubmit: form.monitorFastSubmit,
    monitorRightClick: form.monitorRightClick,
    monitorPrintScreen: form.monitorPrintScreen,
    monitorRapidQuestionSwitch: form.monitorRapidQuestionSwitch,
    monitorMultiMonitor: form.monitorMultiMonitor,
    requireCameraMic: form.proctoringEnabled ? form.requireCameraMic : false,
    shuffleQuestions: form.shuffleQuestions,
    shuffleAnswers: form.shuffleAnswers
  }
}

function generateExamCode() {
  return Math.random().toString(36).substring(2, 8).toUpperCase()
}

// Auto-mark step complete when data filled
watch(isInfoValid, (v) => { if (v) completedSteps.value.add('info') })
watch(isConfigValid, (v) => { if (v) completedSteps.value.add('settings') })
watch(isQuestionsValid, (v) => {
  if (v) {
    completedSteps.value.add('import')
    completedSteps.value.add('edit')
  }
})
</script>

<style scoped>
/* ===== Page Layout ===== */
.ec-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--ds-bg);
}

/* ===== Page Header — breadcrumb strip inside content, not a separate bar ===== */
.ec-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.5rem;
  background: var(--ds-surface);
  border-bottom: 1px solid var(--ds-border);
  flex-shrink: 0;
}

.ec-page-header__breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.8rem;
  color: var(--ds-text-muted);
}

.ec-page-header__bc-link {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: var(--ds-text-muted);
  text-decoration: none;
  transition: color 0.15s ease;
}

.ec-page-header__bc-link:hover {
  color: var(--ds-primary);
}

.ec-page-header__bc-current {
  font-weight: 600;
  color: var(--ds-text);
}

.ec-page-header__actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.ec-page-header__save-status {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.75rem;
  font-weight: 600;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  border: 1px solid var(--ds-border);
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.ec-page-header__save-status--saving {
  background: var(--ds-info-soft);
  color: var(--ds-info);
  border-color: rgba(2, 132, 199, 0.2);
}

.ec-page-header__save-status--saving .ec-page-header__save-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.ec-page-header__save-status--saved {
  background: var(--ds-success-soft);
  color: var(--ds-success);
  border-color: rgba(22, 163, 74, 0.2);
}

.ec-page-header__save-status--error {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
  border-color: rgba(220, 38, 38, 0.2);
}

.ec-page-header__save-icon {
  font-size: 1rem;
}

/* ===== Button styles ===== */
.ec-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5625rem 1.125rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  border: 1px solid transparent;
  white-space: nowrap;
}

.ec-btn--sm {
  padding: 0.5rem 1rem;
  font-size: 0.8rem;
}

.ec-btn--ghost {
  background: transparent;
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.ec-btn--ghost:hover {
  background: var(--ds-gray-50);
  color: var(--ds-text);
}

.dark .ec-btn--ghost:hover {
  background: var(--ds-gray-700);
}

.ec-btn--outline {
  background: transparent;
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.ec-btn--outline:hover {
  background: var(--ds-gray-50);
  border-color: var(--ds-gray-300);
  color: var(--ds-text);
}

.dark .ec-btn--outline:hover {
  background: var(--ds-gray-700);
}

.ec-btn--primary {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.ec-btn--primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.35);
}

.ec-btn--primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.ec-btn--success {
  background: linear-gradient(135deg, var(--ds-success) 0%, #059669 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(22, 163, 74, 0.25);
}

.ec-btn--success:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(22, 163, 74, 0.35);
}

.ec-btn--success:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ===== Body Layout ===== */
.ec-body {
  flex: 1;
}

.ec-body__inner {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 1.5rem;
  max-width: 1440px;
  margin: 0 auto;
  padding: 1.5rem 2rem;
  align-items: start;
  height: 100%;
}

@media (max-width: 1024px) {
  .ec-body__inner {
    grid-template-columns: 1fr;
  }

  .ec-preview-col {
    order: -1;
  }
}

/* ===== Steps ===== */
.ec-steps {
  display: flex;
  gap: 0.25rem;
  margin-bottom: 1.5rem;
  padding: 0.5rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  overflow-x: auto;
}

.ec-steps__item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-lg);
  border: none;
  background: transparent;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.ec-steps__item:hover {
  background: var(--ds-gray-50);
  color: var(--ds-text);
}

.dark .ec-steps__item:hover {
  background: var(--ds-gray-700);
}

.ec-steps__item--active {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.dark .ec-steps__item--active {
  background: rgba(79, 70, 229, 0.15);
}

.ec-steps__item--done {
  color: var(--ds-success);
}

.ec-steps__num {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.7rem;
  font-weight: 700;
  background: var(--ds-gray-200);
  color: var(--ds-text-muted);
  flex-shrink: 0;
}

.ec-steps__item--active .ec-steps__num {
  background: var(--ds-primary);
  color: white;
}

.ec-steps__item--done .ec-steps__num {
  background: var(--ds-success);
  color: white;
}


/* ===== Form Layout — flex column so nav can be sticky ===== */
.ec-form-col {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* Scrollable content area */
.ec-form-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 0.5rem;
  margin-right: -0.5rem;
}

/* Merged section wrapper for combined steps */
.ec-merged-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* ===== Form Navigation — sticky at bottom ===== */
.ec-form-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 1.5rem;
  padding: 1rem 0;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-bg);
  position: sticky;
  bottom: 0;
  z-index: 10;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .ec-form-nav {
    padding: 0.875rem 0;
    margin-top: 1rem;
  }
}

/* ===== Modal ===== */
.ec-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.ec-modal {
  background: var(--ds-surface);
  border-radius: var(--ds-radius-2xl);
  box-shadow: var(--ds-shadow-xl);
  width: 100%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
}

.ec-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
}

.ec-modal__title {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .ec-modal__title {
  color: var(--ds-text);
}

.ec-modal__close {
  width: 2rem;
  height: 2rem;
  border-radius: var(--ds-radius-lg);
  border: none;
  background: transparent;
  color: var(--ds-text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.ec-modal__close:hover {
  background: var(--ds-gray-100);
  color: var(--ds-text);
}

.dark .ec-modal__close:hover {
  background: var(--ds-gray-700);
}

.ec-modal__body {
  padding: 1.5rem;
}

.ec-modal__footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--ds-border);
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.ec-preview-exam-title {
  font-family: var(--ds-font-display);
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0 0 0.5rem;
}

.dark .ec-preview-exam-title {
  color: var(--ds-text);
}

.ec-preview-exam-meta {
  font-size: 0.875rem;
  color: var(--ds-text-secondary);
  margin: 0 0 1rem;
}

.ec-preview-exam-info {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}

.ec-preview-exam-info-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  background: var(--ds-gray-50);
  border-radius: var(--ds-radius-full);
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
}


.ec-preview-exam-info-item--alert {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.ec-preview-exam-desc {
  font-size: 0.875rem;
  color: var(--ds-text-secondary);
  line-height: 1.6;
  margin: 0;
}

/* ===== Toast ===== */
.ec-toast {
  position: fixed;
  bottom: 1.5rem;
  right: 1.5rem;
  z-index: 200;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  background: var(--ds-success);
  color: white;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  box-shadow: 0 8px 24px rgba(22, 163, 74, 0.35);
}


/* Transitions */
.ec-modal-enter-active,
.ec-modal-leave-active {
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.ec-modal-enter-from,
.ec-modal-leave-to {
  opacity: 0;
}

.ec-modal-enter-from .ec-modal,
.ec-modal-leave-to .ec-modal {
  transform: scale(0.95) translateY(10px);
}

.ec-toast-enter-active,
.ec-toast-leave-active {
  transition: color 0.25s ease, background-color 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease, transform 0.25s ease;
}

.ec-toast-enter-from,
.ec-toast-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.95);
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .ec-page-header {
    flex-wrap: wrap;
    gap: 0.75rem;
    padding: 0.875rem 1rem;
  }

  .ec-page-header__save-status {
    display: none;
  }

  .ec-steps__label {
    display: none;
  }

  .ec-steps__item {
    padding: 0.5rem;
  }

  .ec-body__inner {
    padding: 1rem;
  }
}
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}