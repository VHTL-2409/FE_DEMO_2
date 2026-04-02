<template>
  <div class="edp">

    <!-- Loading state -->
    <div v-if="isLoading" class="edp__loading">
      <div class="edp__loading-inner">
        <div class="edp__skeleton edp__skeleton--title" />
        <div class="edp__skeleton edp__skeleton--meta" />
        <div class="edp__skeleton edp__skeleton--card" />
        <div class="edp__skeleton edp__skeleton--card" />
      </div>
    </div>

    <!-- Main layout -->
    <template v-else>

      <!-- Header -->
      <ExamDetailHeader
        :exam="exam"
        :save-state="saveState"
        :save-loading="saveLoading"
        @save="handleSave"
        @publish="handlePublish"
        @unpublish="handleUnpublish"
        @archive="openArchiveConfirm"
        @unarchive="handleUnarchive"
        @duplicate="openDuplicateModal"
        @delete="openDeleteConfirm"
        @preview="showPreviewModal = true"
      />

      <!-- Body -->
      <div class="edp__body">
        <div class="edp__body-inner">

          <!-- Left: tab navigation -->
          <nav class="edp__tabs">
            <button
              v-for="tab in tabs"
              :key="tab.id"
              type="button"
              class="edp__tab"
              :class="{ 'edp__tab--active': activeTab === tab.id }"
              @click="activeTab = tab.id"
            >
              <LucideIcon :name="tab.icon" />
              <span class="edp__tab-label">{{ tab.label }}</span>
              <span v-if="tab.badge" class="edp__tab-badge" :class="`edp__tab-badge--${tab.badgeVariant || 'default'}`">
                {{ tab.badge }}
              </span>
            </button>
          </nav>

          <!-- Main content area -->
          <main class="edp__main">

            <!-- Tab: Tong quan -->
            <div v-show="activeTab === 'overview'" class="edp__tab-content">
              <ExamMetaForm
                v-model:title="form.title"
                v-model:subject="form.subject"
                v-model:className="form.className"
                v-model:description="form.description"
                v-model:duration="form.durationMinutes"
                v-model:shuffleQuestions="form.shuffleQuestions"
                v-model:shuffleAnswers="form.shuffleAnswers"
                v-model:maxAttempts="form.maxAttempts"
              />
            </div>

            <!-- Tab: Cau hoi -->
            <div v-show="activeTab === 'questions'" class="edp__tab-content">
              <QuestionEditor
                v-model:questions="form.questions"
                :exam-title="form.title"
              />
            </div>

            <!-- Tab: Lich thi -->
            <div v-show="activeTab === 'schedule'" class="edp__tab-content">
              <ScheduleEditor
                v-model:startTime="form.startTime"
                v-model:endTime="form.endTime"
                v-model:durationMinutes="form.durationMinutes"
              />
            </div>

            <!-- Tab: Giám sát -->
            <div v-show="activeTab === 'proctoring'" class="edp__tab-content">
              <ProctoringEditor
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
            </div>

            <!-- Tab: Ket qua -->
            <div v-show="activeTab === 'results'" class="edp__tab-content">
              <div v-if="!exam?.participantCount || exam.participantCount === 0" class="edp__empty-state">
                <LucideIcon name="quiz" />
                <p>Chưa có học sinh tham gia thi</p>
                <p class="edp__empty-hint">Kết quả sẽ hiển thị sau khi học sinh hoàn thành bài thi</p>
              </div>
              <div v-else class="edp__results-placeholder">
                <div class="edp__results-header">
                  <LucideIcon name="assessment" />
                  <h3>Kết quả thi</h3>
                </div>
                <div class="edp__results-stats">
                  <div class="edp__result-stat">
                    <LucideIcon name="group" />
                    <div>
                      <p class="edp__result-val">{{ exam?.participantCount || 0 }}</p>
                      <p class="edp__result-label">Học sinh tham gia</p>
                    </div>
                  </div>
                  <div class="edp__result-stat">
                    <LucideIcon name="check_circle" />
                    <div>
                      <p class="edp__result-val">{{ exam?.questionCount || 0 }}</p>
                      <p class="edp__result-label">Câu hỏi</p>
                    </div>
                  </div>
                </div>
                <button type="button" class="edp__results-btn" @click="goToResults">
                  <LucideIcon name="open_in_new" />
                  Xem chi tiết kết quả
                </button>
              </div>
            </div>
          </main>

          <!-- Right: summary panel -->
          <aside class="edp__summary-col">
            <div class="edp__summary-sticky">
              <ExamSummaryPanel :exam="computedExam" @tab-click="navigateToTab" />

              <!-- Sticky action bar -->
              <div class="edp__sticky-bar">
                <button
                  type="button"
                  class="edp__action-btn edp__action-btn--outline"
                  :disabled="saveLoading"
                  @click="handleSave"
                >
                  <LucideIcon name="save" />
                  Lưu thay đổi
                </button>
                <button
                  v-if="!exam?.isActive"
                  type="button"
                  class="edp__action-btn edp__action-btn--publish"
                  :disabled="!canPublish"
                  @click="handlePublish"
                >
                  <LucideIcon name="rocket_launch" />
                  Xuất bản
                </button>
              </div>
            </div>
          </aside>
        </div>
      </div>

      <!-- Delete confirm -->
      <ConfirmDialog
        v-model="showDeleteConfirm"
        title="Xóa đề thi"
        :message="`Bạn có chắc muốn xóa vĩnh viễn đề thi &quot;${exam?.title}&quot;? Tất cả câu hỏi, lượt thi và dữ liệu liên quan sẽ bị mất.`"
        confirm-label="Xóa đề thi"
        cancel-label="Hủy bỏ"
        variant="danger"
        :loading="actionLoading"
        @confirm="confirmDelete"
      />

      <!-- Archive confirm -->
      <ConfirmDialog
        v-model="showArchiveConfirm"
        title="Lưu trữ đề thi"
        :message="exam?.isActive
          ? `Đề thi &quot;${exam?.title}&quot; đang hoạt động. Bạn có chắc muốn lưu trữ? Học sinh sẽ không thể truy cập sau khi lưu trữ.`
          : `Bạn có chắc muốn lưu trữ đề thi &quot;${exam?.title}&quot;?`"
        confirm-label="Lưu trữ"
        cancel-label="Hủy bỏ"
        variant="warning"
        :loading="actionLoading"
        @confirm="confirmArchive"
      />

      <!-- Duplicate confirm modal -->
      <Teleport to="body">
        <Transition name="edp-modal">
          <div v-if="duplicateModal" class="edp-modal-overlay" @click.self="duplicateModal = false">
            <div class="edp-modal">
              <div class="edp-modal__header">
                <div class="edp-modal__icon edp-modal__icon--success">
                  <LucideIcon name="content_copy" />
                </div>
                <div>
                  <h2 class="edp-modal__title">Nhân bản đề thi</h2>
                  <p class="edp-modal__subtitle">Tạo bản sao để sử dụng lại</p>
                </div>
                <button type="button" class="edp-modal__close" @click="duplicateModal = false">
                  <LucideIcon name="close" />
                </button>
              </div>
              <div class="edp-modal__body">
                Tạo bản sao của "<strong>{{ exam?.title }}</strong>"? Bản sao sẽ ở trạng thái nháp và có thể chỉnh sửa được.
              </div>
              <div class="edp-modal__footer">
                <button type="button" class="edp__modal-btn edp__modal-btn--ghost" @click="duplicateModal = false">
                  Hủy bỏ
                </button>
                <button type="button" class="edp__modal-btn edp__modal-btn--primary" :disabled="actionLoading" @click="confirmDuplicate">
                  <LucideIcon name="progress_activity" v-if="actionLoading"  ed-spin/>
                  {{ actionLoading ? 'Đang tạo...' : 'Nhân bản đề thi' }}
                </button>
              </div>
            </div>
          </div>
        </Transition>
      </Teleport>

      <!-- Preview modal -->
      <Teleport to="body">
        <Transition name="edp-modal">
          <div v-if="showPreviewModal" class="edp-modal-overlay" @click.self="showPreviewModal = false">
            <div class="edp-modal edp-modal--lg">
              <div class="edp-modal__header">
                <div class="edp-modal__icon">
                  <LucideIcon name="preview" />
                </div>
                <div>
                  <h2 class="edp-modal__title">Xem trước đề thi</h2>
                  <p class="edp-modal__subtitle">{{ exam?.code }} · {{ exam?.className }}</p>
                </div>
                <button type="button" class="edp-modal__close" @click="showPreviewModal = false">
                  <LucideIcon name="close" />
                </button>
              </div>
              <div class="edp-modal__body edp-modal__body--preview">
                <div class="edp-preview">
                  <div class="edp-preview__title">{{ exam?.title || '—' }}</div>
                  <div class="edp-preview__meta">
                    <span>{{ exam?.subject || '—' }}</span>
                    <span>·</span>
                    <span>{{ exam?.className || '—' }}</span>
                  </div>
                  <div v-if="exam?.description" class="edp-preview__desc">{{ exam.description }}</div>
                  <div class="edp-preview__info">
                    <span class="edp-preview__info-item">
                      <LucideIcon name="timer" />
                      {{ exam?.durationMinutes }} phút
                    </span>
                    <span class="edp-preview__info-item">
                      <LucideIcon name="quiz" />
                      {{ form.questions.length }} câu
                    </span>
                    <span v-if="exam?.isActive" class="edp-preview__info-item edp-preview__info-item--live">
                      <LucideIcon name="play_circle" />
                      Đang hoạt động
                    </span>
                    <span v-else class="edp-preview__info-item edp-preview__info-item--draft">
                      <LucideIcon name="edit_note" />
                      Nháp
                    </span>
                  </div>
                  <div v-if="exam?.startTime" class="edp-preview__schedule">
                    <LucideIcon name="schedule" />
                    {{ formatDateTime(exam.startTime) }}
                    <span v-if="exam?.endTime"> → {{ formatDateTime(exam.endTime) }}</span>
                  </div>
                </div>
              </div>
              <div class="edp-modal__footer">
                <button type="button" class="edp__modal-btn edp__modal-btn--outline" @click="showPreviewModal = false">
                  Đóng
                </button>
              </div>
            </div>
          </div>
        </Transition>
      </Teleport>

      <!-- Unsaved changes guard -->
      <Teleport to="body">
        <Transition name="edp-modal">
          <div v-if="showUnsavedModal" class="edp-modal-overlay" @click.self="showUnsavedModal = false">
            <div class="edp-modal">
              <div class="edp-modal__header">
                <div class="edp-modal__icon edp-modal__icon--warning">
                  <LucideIcon name="warning" />
                </div>
                <div>
                  <h2 class="edp-modal__title">Thay đổi chưa lưu</h2>
                  <p class="edp-modal__subtitle">Bạn có thay đổi chưa được lưu</p>
                </div>
                <button type="button" class="edp-modal__close" @click="showUnsavedModal = false">
                  <LucideIcon name="close" />
                </button>
              </div>
              <div class="edp-modal__body">
                Bạn có thay đổi chưa được lưu. Bạn có muốn lưu trước khi rời đi không?
              </div>
              <div class="edp-modal__footer">
                <button type="button" class="edp__modal-btn edp__modal-btn--ghost" @click="discardAndLeave">
                  Bỏ qua thay đổi
                </button>
                <button type="button" class="edp__modal-btn edp__modal-btn--outline" @click="showUnsavedModal = false">
                  Ở lại
                </button>
                <button type="button" class="edp__modal-btn edp__modal-btn--primary" @click="saveAndLeave">
                  Lưu và rời đi
                </button>
              </div>
            </div>
          </div>
        </Transition>
      </Teleport>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { ApiError } from '../../../../services/apiClient'
import {
  getExamDetail,
  updateExam,
  publishExam,
  unpublishExam,
  archiveExam,
  unarchiveExam,
  duplicateExam,
  deleteExam
} from '../../../../services/examService'
import { listExamQuestions } from '../../../../services/questionService'
import { useToast } from '../../../../composables/useToast'
import ExamDetailHeader from './ExamDetailHeader.vue'
import ExamSummaryPanel from './ExamSummaryPanel.vue'
import ExamMetaForm from './ExamMetaForm.vue'
import QuestionEditor from './QuestionEditor.vue'
import ScheduleEditor from './ScheduleEditor.vue'
import ProctoringEditor from './ProctoringEditor.vue'
import ConfirmDialog from '../../../ui/ConfirmDialog.vue'

const router = useRouter()
const toast = useToast()

const tabs = [
  { id: 'overview', label: 'Tổng quan', icon: 'info' },
  { id: 'questions', label: 'Câu hỏi', icon: 'quiz' },
  { id: 'schedule', label: 'Lịch thi', icon: 'schedule' },
  { id: 'proctoring', label: 'Giám sát', icon: 'shield' },
  { id: 'results', label: 'Kết quả', icon: 'assessment' }
]
const activeTab = ref('overview')

const exam = ref(null)
const isLoading = ref(false)
const saveState = ref('idle')
const saveLoading = ref(false)
const actionLoading = ref(false)
const duplicateModal = ref(false)
const showPreviewModal = ref(false)
const showUnsavedModal = ref(false)
const showDeleteConfirm = ref(false)
const showArchiveConfirm = ref(false)
let pendingNavigation = null
let hasUnsavedChanges = false

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

const computedExam = computed(() => {
  if (!exam.value) return null
  return {
    ...exam.value,
    questionCount: form.questions.length || exam.value.questionCount || 0
  }
})

const canPublish = computed(() =>
  form.title.trim().length >= 3 &&
  form.questions.length > 0
)

watch(form, () => {
  hasUnsavedChanges = true
}, { deep: true })

const loadExam = async () => {
  const id = routeExamId.value
  if (!id) {
    toast.error('Không tìm thấy đề thi.')
    router.push('/teacher/exams/list')
    return
  }

  isLoading.value = true
  try {
    const data = await getExamDetail(id)
    exam.value = data
    populateForm(data)

    try {
      const qs = await listExamQuestions(id)
      form.questions = (qs || []).map(q => ({
        id: q.id,
        content: q.content,
        optionA: q.optionA || '',
        optionB: q.optionB || '',
        optionC: q.optionC || '',
        optionD: q.optionD || '',
        correctAnswer: q.correctAnswer || q.correctOption || '',
        score: Number(q.scoreWeight || q.score || 1),
        scoreWeight: q.scoreWeight,
        type: q.type,
        options: q.options
      }))
    } catch { /* questions optional */ }
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể tải đề thi.')
    router.push('/teacher/exams/list')
  } finally {
    isLoading.value = false
  }
}

const populateForm = (data) => {
  form.title = data.title || ''
  form.subject = data.subject || ''
  form.className = data.className || ''
  form.description = data.description || ''
  form.durationMinutes = data.durationMinutes || 60
  form.shuffleQuestions = data.shuffleQuestions || false
  form.shuffleAnswers = data.shuffleAnswers || false
  form.maxAttempts = data.maxAttempts || 1
  form.startTime = data.startTime || ''
  form.endTime = data.endTime || ''
  form.proctoringEnabled = data.monitorTabSwitch || data.monitorBlur || data.monitorDevtools
  form.monitorTabSwitch = data.monitorTabSwitch !== false
  form.monitorBlur = data.monitorBlur !== false
  form.monitorExitFullscreen = data.monitorExitFullscreen !== false
  form.monitorCopyPaste = data.monitorCopyPaste !== false
  form.monitorIdleTime = data.monitorIdleTime !== false
  form.monitorDevtools = data.monitorDevtools !== false
  form.monitorDuplicateIp = data.monitorDuplicateIp !== false
  form.monitorFastSubmit = data.monitorFastSubmit !== false
  form.monitorRightClick = data.monitorRightClick !== false
  form.monitorPrintScreen = data.monitorPrintScreen !== false
  form.monitorRapidQuestionSwitch = data.monitorRapidQuestionSwitch !== false
  form.monitorMultiMonitor = data.monitorMultiMonitor !== false
  form.requireCameraMic = data.requireCameraMic !== false
  hasUnsavedChanges = false
}

const buildPayload = () => ({
  title: form.title.trim(),
  description: form.description.trim(),
  className: form.className.trim() || null,
  durationMinutes: Number(form.durationMinutes) || 60,
  startTime: form.startTime || null,
  endTime: form.endTime || null,
  isActive: exam.value?.isActive || false,
  shuffleQuestions: form.shuffleQuestions,
  shuffleAnswers: form.shuffleAnswers,
  maxAttempts: Number(form.maxAttempts) || 1,
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
  requireCameraMic: form.requireCameraMic
})

const handleSave = async () => {
  if (!exam.value) return
  saveLoading.value = true
  saveState.value = 'saving'
  try {
    const payload = buildPayload()
    const updated = await updateExam(exam.value.id, payload)
    exam.value = updated
    hasUnsavedChanges = false
    saveState.value = 'saved'
    toast.success('Đã lưu thay đổi.')
    setTimeout(() => { if (saveState.value === 'saved') saveState.value = 'idle' }, 4000)
  } catch (err) {
    saveState.value = 'error'
    toast.error(err instanceof ApiError ? err.message : 'Không thể lưu đề thi.')
    setTimeout(() => { if (saveState.value === 'error') saveState.value = 'idle' }, 4000)
  } finally {
    saveLoading.value = false
  }
}

const handlePublish = async () => {
  if (!exam.value) return
  actionLoading.value = true
  try {
    const payload = buildPayload()
    await updateExam(exam.value.id, payload)
    const updated = await publishExam(exam.value.id)
    exam.value = updated
    hasUnsavedChanges = false
    toast.success('Đề thi đã được xuất bản.')
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể xuất bản.')
  } finally {
    actionLoading.value = false
  }
}

const handleUnpublish = async () => {
  if (!exam.value) return
  actionLoading.value = true
  try {
    const updated = await unpublishExam(exam.value.id)
    exam.value = updated
    toast.success('Đã hủy xuất bản đề thi.')
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể hủy xuất bản.')
  } finally {
    actionLoading.value = false
  }
}

const openArchiveConfirm = () => {
  showArchiveConfirm.value = true
}

const confirmArchive = async () => {
  if (!exam.value) return
  actionLoading.value = true
  showArchiveConfirm.value = false
  try {
    const updated = await archiveExam(exam.value.id)
    exam.value = updated
    toast.success('Đề thi đã được lưu trữ.')
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể lưu trữ.')
  } finally {
    actionLoading.value = false
  }
}

const handleUnarchive = async () => {
  if (!exam.value) return
  actionLoading.value = true
  try {
    const updated = await unarchiveExam(exam.value.id)
    exam.value = updated
    toast.success('Đã khôi phục đề thi.')
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể khôi phục.')
  } finally {
    actionLoading.value = false
  }
}

const openDuplicateModal = () => {
  duplicateModal.value = true
}

const confirmDuplicate = async () => {
  if (!exam.value) return
  actionLoading.value = true
  try {
    const created = await duplicateExam(exam.value.id)
    duplicateModal.value = false
    toast.success(`Đã tạo bản sao: "${created.title}"`)
    router.push({ path: `/teacher/exams/${created.id}` })
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể nhân bản.')
  } finally {
    actionLoading.value = false
  }
}

const openDeleteConfirm = () => {
  showDeleteConfirm.value = true
}

const confirmDelete = async () => {
  if (!exam.value) return
  actionLoading.value = true
  showDeleteConfirm.value = false
  try {
    await deleteExam(exam.value.id)
    toast.success('Đã xóa đề thi.')
    router.push('/teacher/exams/list')
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể xóa đề thi.')
  } finally {
    actionLoading.value = false
  }
}

const goToResults = () => {
  if (!exam.value) return
  router.push({ path: '/teacher/exams/review/student-report', query: { examId: exam.value.id } })
}

const navigateToTab = (tabId) => {
  activeTab.value = tabId
}

const formatDateTime = (d) => {
  if (!d) return '—'
  try {
    return new Date(d).toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    })
  } catch { return '—' }
}

const discardAndLeave = () => {
  showUnsavedModal.value = false
  hasUnsavedChanges = false
  if (pendingNavigation) pendingNavigation()
}

const saveAndLeave = async () => {
  showUnsavedModal.value = false
  try {
    await handleSave()
  } catch { /* already handled */ }
  if (pendingNavigation) pendingNavigation()
}

const routeExamId = computed(() => {
  const params = router.currentRoute.value.params
  return params.id || router.currentRoute.value.query.id || null
})

onBeforeRouteLeave(async (to, from) => {
  if (hasUnsavedChanges) {
    pendingNavigation = () => router.push(to.fullPath)
    showUnsavedModal.value = true
    return false
  }
})

onMounted(loadExam)
</script>

<style scoped>
.edp {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--ds-bg);
}

/* Loading */
.edp__loading {
  padding: 2rem;
  max-width: 1440px;
  margin: 0 auto;
  width: 100%;
}

.edp__loading-inner {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.edp__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-100) 25%, var(--ds-gray-200) 50%, var(--ds-gray-100) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--ds-radius-xl);
}

.dark .edp__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-800) 25%, var(--ds-gray-700) 50%, var(--ds-gray-800) 75%);
  background-size: 200% 100%;
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.edp__skeleton--title { height: 2rem; width: 60%; }
.edp__skeleton--meta { height: 1rem; width: 40%; }
.edp__skeleton--card { height: 8rem; width: 100%; }

/* Body */
.edp__body {
  flex: 1;
  padding: 1.5rem;
}

.edp__body-inner {
  display: grid;
  grid-template-columns: 180px 1fr 320px;
  gap: 1.25rem;
  max-width: 1440px;
  margin: 0 auto;
  align-items: start;
}

@media (max-width: 1100px) {
  .edp__body-inner {
    grid-template-columns: 1fr;
  }
  .edp__tabs {
    display: flex;
    overflow-x: auto;
    padding-bottom: 0.5rem;
  }
  .edp__summary-col {
    order: -1;
  }
}

/* Tabs */
.edp__tabs {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  position: sticky;
  top: 80px;
}

.edp__tab {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.625rem 0.875rem;
  border-radius: var(--ds-radius-xl);
  border: none;
  background: transparent;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: left;
  white-space: nowrap;
  position: relative;
}

.edp__tab:hover {
  background: var(--ds-gray-50);
  color: var(--ds-text);
}

.dark .edp__tab:hover {
  background: var(--ds-gray-800);
}

.edp__tab--active {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.dark .edp__tab--active {
  background: rgba(79, 70, 229, 0.15);
}

.edp__tab-icon {
  font-size: 1.125rem;
  flex-shrink: 0;
}

.edp__tab-badge {
  margin-left: auto;
  padding: 0.1rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 800;
  background: var(--ds-gray-200);
  color: var(--ds-text-muted);
}

.edp__tab-badge--success { background: var(--ds-success-soft); color: var(--ds-success); }
.edp__tab-badge--warning { background: var(--ds-warning-soft); color: var(--ds-warning); }
.edp__tab-badge--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }

/* Main content */
.edp__main {
  min-width: 0;
}

.edp__tab-content {
  animation: fadeIn 0.15s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Empty state */
.edp__empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 4rem 2rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  text-align: center;
  color: var(--ds-text-muted);
}

.dark .edp__empty-state { border-color: var(--ds-border-strong); }
.edp__empty-state p { margin: 0; font-weight: 600; }
.edp__empty-hint { font-size: 0.8rem; font-weight: 400 !important; color: var(--ds-text-muted); }

/* Results placeholder */
.edp__results-placeholder {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 2rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
}

.dark .edp__results-placeholder { border-color: var(--ds-border-strong); }

.edp__results-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}


.edp__results-header h3 {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .edp__results-header h3 { color: #f1f5f9; }

.edp__results-stats {
  display: flex;
  gap: 1rem;
}

.edp__result-stat {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  flex: 1;
}

.dark .edp__result-stat { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }


.edp__result-val {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1;
}

.dark .edp__result-val { color: #f1f5f9; }

.edp__result-label {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.edp__results-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  align-self: flex-start;
}

.dark .edp__results-btn { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: #94a3b8; }
.edp__results-btn:hover { border-color: var(--ds-primary); color: var(--ds-primary); background: var(--ds-primary-soft); }
.dark .edp__results-btn:hover { background: rgba(79, 70, 229, 0.15); }

/* Summary col */
.edp__summary-col {}

.edp__summary-sticky {
  position: sticky;
  top: 80px;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/* Sticky action bar */
.edp__sticky-bar {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 1rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
}

.dark .edp__sticky-bar { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

/* Action buttons */
.edp__action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid transparent;
  width: 100%;
}


.edp__action-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }

.edp__action-btn--outline {
  background: transparent;
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .edp__action-btn--outline { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: #94a3b8; }

.edp__action-btn--outline:hover:not(:disabled) {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.dark .edp__action-btn--outline:hover:not(:disabled) { background: rgba(79, 70, 229, 0.15); }

.edp__action-btn--publish {
  background: linear-gradient(135deg, var(--ds-success) 0%, #059669 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(22, 163, 74, 0.25);
}

.edp__action-btn--publish:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(22, 163, 74, 0.35);
}

/* Modal */
.edp-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0,0,0,0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}

.edp-modal {
  background: white;
  border-radius: var(--ds-radius-2xl);
  width: 100%;
  max-width: 440px;
  box-shadow: 0 24px 64px rgba(0,0,0,0.2);
  overflow: hidden;
}

.dark .edp-modal { background: var(--ds-gray-800); }
.edp-modal--lg { max-width: 600px; }

.edp-modal__header {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
  padding: 1.5rem;
  border-bottom: 1px solid var(--ds-border);
}

.dark .edp-modal__header { border-bottom-color: var(--ds-border-strong); }

.edp-modal__icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.edp-modal__icon--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }
.edp-modal__icon--success { background: var(--ds-success-soft); color: var(--ds-success); }
.edp-modal__icon--warning { background: rgba(234, 179, 8, 0.1); color: #d97706; }

.edp-modal__title {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
}

.dark .edp-modal__title { color: #f1f5f9; }

.edp-modal__subtitle {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.edp-modal__close {
  margin-left: auto;
  width: 2rem;
  height: 2rem;
  border: none;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  border-radius: var(--ds-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.12s ease;
  flex-shrink: 0;
}

.dark .edp-modal__close { background: var(--ds-gray-700); }
.edp-modal__close:hover { background: var(--ds-gray-200); color: var(--ds-text); }
.dark .edp-modal__close:hover { background: var(--ds-gray-600); }

.edp-modal__body {
  padding: 1.5rem;
  font-size: 0.875rem;
  color: var(--ds-text-secondary);
  line-height: 1.6;
}

.dark .edp-modal__body { color: #94a3b8; }
.edp-modal__body strong { color: var(--ds-text); }
.dark .edp-modal__body strong { color: #f1f5f9; }

.edp-modal__body--preview { padding: 1rem 1.5rem; }

.edp-modal__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1.25rem 1.5rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .edp-modal__footer { border-top-color: var(--ds-border-strong); background: var(--ds-gray-800); }

/* Preview */
.edp-preview {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem;
  background: linear-gradient(135deg, var(--ds-primary-soft) 0%, var(--ds-info-soft) 100%);
  border: 1px solid var(--ds-primary-border);
  border-radius: var(--ds-radius-xl);
}

.edp-preview__title {
  font-family: var(--ds-font-display);
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--ds-text);
}

.dark .edp-preview__title { color: #f1f5f9; }

.edp-preview__meta {
  display: flex;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--ds-text-secondary);
}

.edp-preview__desc {
  font-size: 0.875rem;
  color: var(--ds-text-secondary);
  line-height: 1.5;
}

.edp-preview__info {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.edp-preview__info-item {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  background: rgba(255,255,255,0.6);
  border-radius: var(--ds-radius-full);
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
}

.dark .edp-preview__info-item { background: rgba(255,255,255,0.1); color: #f1f5f9; }


.edp-preview__info-item--live { background: rgba(22,163,74,0.1); color: var(--ds-success); }
.edp-preview__info-item--draft { background: rgba(100,116,139,0.1); color: var(--ds-gray-500); }

.edp-preview__schedule {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  color: var(--ds-text-secondary);
}


/* Modal buttons */
.edp__modal-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid transparent;
}


.edp__modal-btn--ghost {
  background: transparent;
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .edp__modal-btn--ghost { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: #94a3b8; }

.edp__modal-btn--ghost:hover {
  border-color: var(--ds-gray-300);
  color: var(--ds-text);
}

.dark .edp__modal-btn--ghost:hover { background: var(--ds-gray-700); }

.edp__modal-btn--outline {
  background: transparent;
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .edp__modal-btn--outline { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: #94a3b8; }

.edp__modal-btn--outline:hover:not(:disabled) {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.dark .edp__modal-btn--outline:hover:not(:disabled) { background: rgba(79, 70, 229, 0.15); }

.edp__modal-btn--primary {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.edp__modal-btn--primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.35);
}

.edp__modal-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }

/* Transitions */
.edp-modal-enter-active, .edp-modal-leave-active { transition: all 0.2s ease; }
.edp-modal-enter-from, .edp-modal-leave-to { opacity: 0; }
.edp-modal-enter-from .edp-modal, .edp-modal-leave-to .edp-modal { transform: scale(0.95); }

.ed-spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
