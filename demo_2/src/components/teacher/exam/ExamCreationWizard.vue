/**
 * ExamCreationWizard.vue
 * Step-by-step wizard for creating exams
 * Features: Info -> Questions -> Config -> Schedule -> Review
 */
<template>
  <div class="ecw">
    <!-- Wizard Progress -->
    <div class="ecw__progress">
      <div
        v-for="(step, index) in steps"
        :key="step.id"
        class="ecw__step"
        :class="{
          'ecw__step--active': currentStepIndex === index,
          'ecw__step--completed': currentStepIndex > index,
          'ecw__step--clickable': currentStepIndex > index
        }"
        @click="goToStep(index)"
      >
        <div class="ecw__step-indicator">
          <span v-if="currentStepIndex > index" class="material-symbols-outlined">check</span>
          <span v-else>{{ index + 1 }}</span>
        </div>
        <div class="ecw__step-info">
          <span class="ecw__step-label">{{ step.label }}</span>
          <span class="ecw__step-desc">{{ step.description }}</span>
        </div>
        <div v-if="index < steps.length - 1" class="ecw__step-connector" />
      </div>
    </div>

    <!-- Wizard Content -->
    <div class="ecw__content">
      <transition name="slide-fade" mode="out-in">
        <!-- Step 1: Exam Info -->
        <div v-if="currentStep === 'info'" key="info" class="ecw__panel">
          <div class="ecw__panel-header">
            <h2 class="ecw__panel-title">Thông tin kỳ thi</h2>
            <p class="ecw__panel-desc">Nhập thông tin cơ bản về kỳ thi của bạn</p>
          </div>
          <div class="ecw__panel-body">
            <div class="ecw__form">
              <div class="ecw__form-group">
                <label class="ecw__label">Tiêu đề kỳ thi <span class="ecw__required">*</span></label>
                <input
                  v-model="form.title"
                  type="text"
                  class="ecw__input"
                  placeholder="VD: Giữa kỳ Tâm lý học đại cương"
                />
              </div>
              <div class="ecw__form-group">
                <label class="ecw__label">Mô tả</label>
                <textarea
                  v-model="form.description"
                  class="ecw__textarea"
                  rows="3"
                  placeholder="Mô tả ngắn về nội dung kỳ thi..."
                />
              </div>
              <div class="ecw__form-row">
                <div class="ecw__form-group">
                  <label class="ecw__label">Môn học</label>
                  <input
                    v-model="form.subject"
                    type="text"
                    class="ecw__input"
                    placeholder="VD: Tâm lý học"
                  />
                </div>
                <div class="ecw__form-group">
                  <label class="ecw__label">Lớp</label>
                  <select v-model="form.classId" class="ecw__select">
                    <option value="">Chọn lớp...</option>
                    <option v-for="cls in classes" :key="cls.id" :value="cls.id">
                      {{ cls.name }}
                    </option>
                  </select>
                </div>
              </div>
              <div class="ecw__form-row">
                <div class="ecw__form-group">
                  <label class="ecw__label">Số câu hỏi <span class="ecw__required">*</span></label>
                  <input
                    v-model.number="form.questionCount"
                    type="number"
                    min="1"
                    max="200"
                    class="ecw__input"
                  />
                </div>
                <div class="ecw__form-group">
                  <label class="ecw__label">Thời gian (phút) <span class="ecw__required">*</span></label>
                  <input
                    v-model.number="form.durationMinutes"
                    type="number"
                    min="5"
                    max="300"
                    class="ecw__input"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 2: Questions -->
        <div v-else-if="currentStep === 'questions'" key="questions" class="ecw__panel">
          <div class="ecw__panel-header">
            <h2 class="ecw__panel-title">Câu hỏi</h2>
            <p class="ecw__panel-desc">Tạo và quản lý câu hỏi cho kỳ thi</p>
          </div>
          <div class="ecw__panel-body">
            <div class="ecw__question-toolbar">
              <button type="button" class="ecw__toolbar-btn" @click="addQuestion">
                <LucideIcon name="add" />
                Thêm câu hỏi
              </button>
              <button type="button" class="ecw__toolbar-btn ecw__toolbar-btn--ai" @click="generateWithAI">
                <LucideIcon name="auto_awesome" />
                Tạo với AI
              </button>
            </div>
            <div class="ecw__questions-list">
              <QuestionBuilder
                v-for="(question, index) in form.questions"
                :key="question.id"
                :question="question"
                :index="index"
                @update="updateQuestion(index, $event)"
                @delete="deleteQuestion(index)"
                @move-up="moveQuestion(index, -1)"
                @move-down="moveQuestion(index, 1)"
              />
              <div v-if="!form.questions.length" class="ecw__empty-questions">
                <LucideIcon name="quiz" />
                <p>Chưa có câu hỏi nào</p>
                <span>Bấm "Thêm câu hỏi" hoặc "Tạo với AI" để bắt đầu</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 3: Config -->
        <div v-else-if="currentStep === 'config'" key="config" class="ecw__panel">
          <div class="ecw__panel-header">
            <h2 class="ecw__panel-title">Cấu hình kỳ thi</h2>
            <p class="ecw__panel-desc">Thiết lập các quy định và giám sát</p>
          </div>
          <div class="ecw__panel-body">
            <div class="ecw__config-section">
              <h3 class="ecw__config-title">Quy định thi</h3>
              <div class="ecw__toggle-group">
                <div class="ecw__toggle-item">
                  <div class="ecw__toggle-info">
                    <span class="ecw__toggle-label">Cho phép xem lại đáp án</span>
                    <span class="ecw__toggle-desc">Học sinh có thể xem đáp án sau khi nộp bài</span>
                  </div>
                  <label class="ecw__toggle">
                    <input v-model="form.config.showAnswers" type="checkbox" />
                    <span class="ecw__toggle-slider" />
                  </label>
                </div>
                <div class="ecw__toggle-item">
                  <div class="ecw__toggle-info">
                    <span class="ecw__toggle-label">Cho phép tạm dừng</span>
                    <span class="ecw__toggle-desc">Học sinh có thể tạm dừng và tiếp tục làm bài</span>
                  </div>
                  <label class="ecw__toggle">
                    <input v-model="form.config.allowPause" type="checkbox" />
                    <span class="ecw__toggle-slider" />
                  </label>
                </div>
                <div class="ecw__toggle-item">
                  <div class="ecw__toggle-info">
                    <span class="ecw__toggle-label">Hiển thị điểm ngay</span>
                    <span class="ecw__toggle-desc">Điểm được hiển thị ngay sau khi nộp bài</span>
                  </div>
                  <label class="ecw__toggle">
                    <input v-model="form.config.showScoreImmediately" type="checkbox" />
                    <span class="ecw__toggle-slider" />
                  </label>
                </div>
              </div>
            </div>

            <div class="ecw__config-section">
              <h3 class="ecw__config-title">Giám sát</h3>
              <div class="ecw__toggle-group">
                <div class="ecw__toggle-item">
                  <div class="ecw__toggle-info">
                    <span class="ecw__toggle-label">Yêu cầu camera & mic</span>
                    <span class="ecw__toggle-desc">Bật camera và micro trong suốt phiên thi</span>
                  </div>
                  <label class="ecw__toggle">
                    <input v-model="form.config.requireCameraMic" type="checkbox" />
                    <span class="ecw__toggle-slider" />
                  </label>
                </div>
                <div class="ecw__toggle-item">
                  <div class="ecw__toggle-info">
                    <span class="ecw__toggle-label">Giám sát chuyển tab</span>
                    <span class="ecw__toggle-desc">Ghi nhận khi học sinh chuyển sang tab khác</span>
                  </div>
                  <label class="ecw__toggle">
                    <input v-model="form.config.monitorTabSwitch" type="checkbox" />
                    <span class="ecw__toggle-slider" />
                  </label>
                </div>
                <div class="ecw__toggle-item">
                  <div class="ecw__toggle-info">
                    <span class="ecw__toggle-label">Chặn copy/paste</span>
                    <span class="ecw__toggle-desc">Không cho phép sao chép nội dung</span>
                  </div>
                  <label class="ecw__toggle">
                    <input v-model="form.config.monitorCopyPaste" type="checkbox" />
                    <span class="ecw__toggle-slider" />
                  </label>
                </div>
                <div class="ecw__toggle-item">
                  <div class="ecw__toggle-info">
                    <span class="ecw__toggle-label">Giám sát DevTools</span>
                    <span class="ecw__toggle-desc">Phát hiện mở công cụ phát triển trình duyệt</span>
                  </div>
                  <label class="ecw__toggle">
                    <input v-model="form.config.monitorDevtools" type="checkbox" />
                    <span class="ecw__toggle-slider" />
                  </label>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 4: Schedule -->
        <div v-else-if="currentStep === 'schedule'" key="schedule" class="ecw__panel">
          <div class="ecw__panel-header">
            <h2 class="ecw__panel-title">Lịch thi</h2>
            <p class="ecw__panel-desc">Thiết lập thời gian và ca thi</p>
          </div>
          <div class="ecw__panel-body">
            <div class="ecw__form">
              <div class="ecw__form-row">
                <div class="ecw__form-group">
                  <label class="ecw__label">Ngày bắt đầu</label>
                  <input v-model="form.schedule.startTime" type="datetime-local" class="ecw__input" />
                </div>
                <div class="ecw__form-group">
                  <label class="ecw__label">Ngày kết thúc</label>
                  <input v-model="form.schedule.endTime" type="datetime-local" class="ecw__input" />
                </div>
              </div>
              <div class="ecw__form-group">
                <label class="ecw__label">Mật khẩu dự phòng (tùy chọn)</label>
                <input
                  v-model="form.schedule.password"
                  type="text"
                  class="ecw__input"
                  placeholder="Để trống nếu không cần mật khẩu"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Step 5: Review -->
        <div v-else-if="currentStep === 'review'" key="review" class="ecw__panel">
          <div class="ecw__panel-header">
            <h2 class="ecw__panel-title">Xem lại và tạo</h2>
            <p class="ecw__panel-desc">Kiểm tra thông tin trước khi tạo kỳ thi</p>
          </div>
          <div class="ecw__panel-body">
            <div class="ecw__review-grid">
              <div class="ecw__review-card">
                <div class="ecw__review-card-header">
                  <LucideIcon name="info" />
                  <span>Thông tin</span>
                </div>
                <div class="ecw__review-card-body">
                  <div class="ecw__review-item">
                    <span class="ecw__review-label">Tiêu đề</span>
                    <span class="ecw__review-value">{{ form.title || '—' }}</span>
                  </div>
                  <div class="ecw__review-item">
                    <span class="ecw__review-label">Môn học</span>
                    <span class="ecw__review-value">{{ form.subject || '—' }}</span>
                  </div>
                  <div class="ecw__review-item">
                    <span class="ecw__review-label">Số câu hỏi</span>
                    <span class="ecw__review-value">{{ form.questions.length }}</span>
                  </div>
                  <div class="ecw__review-item">
                    <span class="ecw__review-label">Thời gian</span>
                    <span class="ecw__review-value">{{ form.durationMinutes }} phút</span>
                  </div>
                </div>
              </div>

              <div class="ecw__review-card">
                <div class="ecw__review-card-header">
                  <LucideIcon name="quiz" />
                  <span>Câu hỏi</span>
                </div>
                <div class="ecw__review-card-body">
                  <div v-for="(q, i) in form.questions.slice(0, 5)" :key="q.id" class="ecw__review-question">
                    <span class="ecw__review-question-num">Câu {{ i + 1 }}</span>
                    <span class="ecw__review-question-text">{{ q.content || 'Chưa có nội dung' }}</span>
                  </div>
                  <div v-if="form.questions.length > 5" class="ecw__review-more">
                    +{{ form.questions.length - 5 }} câu hỏi khác
                  </div>
                </div>
              </div>

              <div class="ecw__review-card">
                <div class="ecw__review-card-header">
                  <LucideIcon name="schedule" />
                  <span>Lịch</span>
                </div>
                <div class="ecw__review-card-body">
                  <div class="ecw__review-item">
                    <span class="ecw__review-label">Bắt đầu</span>
                    <span class="ecw__review-value">{{ formatDateTime(form.schedule.startTime) }}</span>
                  </div>
                  <div class="ecw__review-item">
                    <span class="ecw__review-label">Kết thúc</span>
                    <span class="ecw__review-value">{{ formatDateTime(form.schedule.endTime) }}</span>
                  </div>
                </div>
              </div>

              <div class="ecw__review-card">
                <div class="ecw__review-card-header">
                  <LucideIcon name="security" />
                  <span>Giám sát</span>
                </div>
                <div class="ecw__review-card-body">
                  <div class="ecw__review-badges">
                    <span v-if="form.config.requireCameraMic" class="ecw__review-badge ecw__review-badge--active">
                      <LucideIcon name="videocam" /> Camera
                    </span>
                    <span v-if="form.config.monitorTabSwitch" class="ecw__review-badge ecw__review-badge--active">
                      <LucideIcon name="tab_unselected" /> Tab
                    </span>
                    <span v-if="form.config.monitorDevtools" class="ecw__review-badge ecw__review-badge--active">
                      <LucideIcon name="code" /> DevTools
                    </span>
                    <span v-if="!form.config.requireCameraMic && !form.config.monitorTabSwitch && !form.config.monitorDevtools" class="ecw__review-badge ecw__review-badge--inactive">
                      Không có giám sát
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- Wizard Footer -->
    <div class="ecw__footer">
      <button
        v-if="currentStepIndex > 0"
        type="button"
        class="ecw__btn ecw__btn--ghost"
        @click="previousStep"
      >
        <LucideIcon name="arrow_back" />
        Quay lại
      </button>
      <div class="ecw__footer-spacer" />
      <button
        v-if="currentStepIndex < steps.length - 1"
        type="button"
        class="ecw__btn ecw__btn--primary"
        @click="nextStep"
      >
        Tiếp tục
        <LucideIcon name="arrow_forward" />
      </button>
      <button
        v-else
        type="button"
        class="ecw__btn ecw__btn--success"
        :disabled="isCreating"
        @click="createExam"
      >
        <span v-if="isCreating" class="ecw__btn-spinner material-symbols-outlined animate-spin">progress_activity</span>
        <template v-else>
          <LucideIcon name="check" />
          Tạo kỳ thi
        </template>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '../../composables/useToast'
import { createExam } from '../../services/examService'
import LucideIcon from '../common/LucideIcon.vue'
import QuestionBuilder from './QuestionBuilder.vue'

const router = useRouter()
const toast = useToast()

const steps = [
  { id: 'info', label: 'Thông tin', description: 'Cơ bản' },
  { id: 'questions', label: 'Câu hỏi', description: 'Nội dung' },
  { id: 'config', label: 'Cấu hình', description: 'Quy định' },
  { id: 'schedule', label: 'Lịch thi', description: 'Thời gian' },
  { id: 'review', label: 'Xem lại', description: 'Hoàn tất' }
]

const currentStepIndex = ref(0)
const currentStep = computed(() => steps[currentStepIndex.value].id)
const isCreating = ref(false)

const classes = ref([])

const form = ref({
  title: '',
  description: '',
  subject: '',
  classId: '',
  questionCount: 20,
  durationMinutes: 60,
  questions: [],
  config: {
    showAnswers: true,
    allowPause: false,
    showScoreImmediately: true,
    requireCameraMic: true,
    monitorTabSwitch: true,
    monitorCopyPaste: true,
    monitorDevtools: true
  },
  schedule: {
    startTime: '',
    endTime: '',
    password: ''
  }
})

const goToStep = (index) => {
  if (index < currentStepIndex.value) {
    currentStepIndex.value = index
  }
}

const previousStep = () => {
  if (currentStepIndex.value > 0) {
    currentStepIndex.value--
  }
}

const nextStep = () => {
  if (currentStepIndex.value < steps.length - 1) {
    currentStepIndex.value++
  }
}

const addQuestion = () => {
  form.value.questions.push({
    id: Date.now(),
    content: '',
    type: 'SINGLE_CHOICE',
    options: [
      { id: 'A', content: '' },
      { id: 'B', content: '' },
      { id: 'C', content: '' },
      { id: 'D', content: '' }
    ],
    correctAnswer: '',
    difficulty: 'MEDIUM',
    points: 1
  })
}

const updateQuestion = (index, question) => {
  form.value.questions[index] = question
}

const deleteQuestion = (index) => {
  form.value.questions.splice(index, 1)
}

const moveQuestion = (index, direction) => {
  const newIndex = index + direction
  if (newIndex < 0 || newIndex >= form.value.questions.length) return
  const temp = form.value.questions[index]
  form.value.questions[index] = form.value.questions[newIndex]
  form.value.questions[newIndex] = temp
}

const generateWithAI = () => {
  toast.info('Tính năng AI đang được phát triển')
}

const formatDateTime = (value) => {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleString('vi-VN')
}

const handleCreateExam = async () => {
  isCreating.value = true
  try {
    const exam = await createExam({
      title: form.value.title,
      description: form.value.description,
      subject: form.value.subject,
      classId: form.value.classId,
      questionCount: form.value.questionCount,
      durationMinutes: form.value.durationMinutes,
      questions: form.value.questions,
      config: form.value.config,
      schedule: form.value.schedule
    })
    toast.success('Đã tạo kỳ thi thành công!')
    router.push({
      path: '/teacher/exams/success',
      query: { examId: exam.id, title: exam.title }
    })
  } catch (error) {
    toast.error('Không thể tạo kỳ thi. Vui lòng thử lại.')
  } finally {
    isCreating.value = false
  }
}
</script>

<style scoped>
.ecw {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  background: var(--ds-surface);
  border-radius: var(--ds-radius-2xl);
  border: 1.5px solid var(--ds-border);
  overflow: hidden;
}

.dark .ecw {
  border-color: var(--ds-border-strong);
}

/* Progress */
.ecw__progress {
  display: flex;
  align-items: flex-start;
  padding: 1.5rem;
  background: var(--ds-gray-50);
  border-bottom: 1px solid var(--ds-border);
  overflow-x: auto;
}

.dark .ecw__progress {
  background: var(--ds-gray-800);
  border-bottom-color: var(--ds-border-strong);
}

.ecw__step {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
  min-width: 120px;
  position: relative;
}

.ecw__step--clickable {
  cursor: pointer;
}

.ecw__step-indicator {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  font-weight: 700;
  background: var(--ds-border);
  color: var(--ds-text-muted);
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.ecw__step--active .ecw__step-indicator {
  background: var(--ds-primary);
  color: white;
}

.ecw__step--completed .ecw__step-indicator {
  background: var(--ds-success);
  color: white;
}

.ecw__step-info {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.ecw__step-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .ecw__step-label {
  color: var(--ds-text);
}

.ecw__step-desc {
  font-size: 0.65rem;
  color: var(--ds-text-muted);
}

.ecw__step-connector {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: calc(100% - 140px);
  height: 2px;
  background: var(--ds-border);
}

.ecw__step--completed .ecw__step-connector {
  background: var(--ds-success);
}

/* Content */
.ecw__content {
  flex: 1;
  padding: 1.5rem;
  overflow-y: auto;
}

.ecw__panel-header {
  margin-bottom: 1.5rem;
}

.ecw__panel-title {
  font-family: var(--ds-font-display);
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0 0 0.25rem;
}

.dark .ecw__panel-title {
  color: var(--ds-text);
}

.ecw__panel-desc {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  margin: 0;
}

/* Form */
.ecw__form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.ecw__form-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

@media (max-width: 640px) {
  .ecw__form-row {
    grid-template-columns: 1fr;
  }
}

.ecw__form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.ecw__label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
}

.dark .ecw__label {
  color: var(--ds-text-muted);
}

.ecw__required {
  color: var(--ds-danger);
}

.ecw__input,
.ecw__textarea,
.ecw__select {
  padding: 0.75rem 1rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  color: var(--ds-text);
  transition: all 0.15s ease;
}

.dark .ecw__input,
.dark .ecw__textarea,
.dark .ecw__select {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text);
}

.ecw__input:focus,
.ecw__textarea:focus,
.ecw__select:focus {
  outline: none;
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.ecw__textarea {
  resize: vertical;
  min-height: 80px;
}

/* Question Toolbar */
.ecw__question-toolbar {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.ecw__toolbar-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.dark .ecw__toolbar-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-muted);
}

.ecw__toolbar-btn:hover {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.ecw__toolbar-btn--ai {
  background: linear-gradient(135deg, var(--ds-primary-soft) 0%, rgba(139, 92, 246, 0.1) 100%);
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
}

.ecw__toolbar-btn--ai:hover {
  background: var(--ds-primary);
  color: white;
}

.ecw__questions-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.ecw__empty-questions {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 3rem 1rem;
  text-align: center;
  background: var(--ds-gray-50);
  border: 2px dashed var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  color: var(--ds-text-muted);
}

.dark .ecw__empty-questions {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-muted);
}

.ecw__empty-questions p {
  font-size: 0.9rem;
  font-weight: 700;
  margin: 0;
}

.ecw__empty-questions span {
  font-size: 0.8rem;
}

/* Config */
.ecw__config-section {
  margin-bottom: 2rem;
}

.ecw__config-title {
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0 0 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--ds-border);
}

.dark .ecw__config-title {
  color: var(--ds-text);
  border-bottom-color: var(--ds-border-strong);
}

.ecw__toggle-group {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.ecw__toggle-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.875rem 1rem;
  background: var(--ds-gray-50);
  border-radius: var(--ds-radius-xl);
  border: 1px solid var(--ds-border);
}

.dark .ecw__toggle-item {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ecw__toggle-info {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.ecw__toggle-label {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .ecw__toggle-label {
  color: var(--ds-text);
}

.ecw__toggle-desc {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
}

.ecw__toggle {
  position: relative;
  width: 3rem;
  height: 1.5rem;
  flex-shrink: 0;
}

.ecw__toggle input {
  opacity: 0;
  width: 0;
  height: 0;
}

.ecw__toggle-slider {
  position: absolute;
  cursor: pointer;
  inset: 0;
  background: var(--ds-border);
  border-radius: var(--ds-radius-full);
  transition: all 0.2s ease;
}

.ecw__toggle-slider::before {
  content: '';
  position: absolute;
  height: 1.125rem;
  width: 1.125rem;
  left: 0.175rem;
  bottom: 0.175rem;
  background: white;
  border-radius: 50%;
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}

.ecw__toggle input:checked + .ecw__toggle-slider {
  background: var(--ds-primary);
}

.ecw__toggle input:checked + .ecw__toggle-slider::before {
  transform: translateX(1.5rem);
}

/* Review */
.ecw__review-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

@media (max-width: 768px) {
  .ecw__review-grid {
    grid-template-columns: 1fr;
  }
}

.ecw__review-card {
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  overflow: hidden;
}

.dark .ecw__review-card {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ecw__review-card-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: var(--ds-gray-100);
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
  border-bottom: 1px solid var(--ds-border);
}

.dark .ecw__review-card-header {
  background: var(--ds-gray-700);
  border-bottom-color: var(--ds-border-strong);
  color: var(--ds-text);
}

.ecw__review-card-body {
  padding: 1rem;
}

.ecw__review-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0;
  border-bottom: 1px dashed var(--ds-border);
}

.ecw__review-item:last-child {
  border-bottom: none;
}

.ecw__review-label {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
}

.ecw__review-value {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text);
}

.dark .ecw__review-value {
  color: var(--ds-text);
}

.ecw__review-question {
  display: flex;
  gap: 0.5rem;
  padding: 0.5rem 0;
  border-bottom: 1px dashed var(--ds-border);
}

.ecw__review-question:last-child {
  border-bottom: none;
}

.ecw__review-question-num {
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--ds-primary);
  flex-shrink: 0;
}

.ecw__review-question-text {
  font-size: 0.8rem;
  color: var(--ds-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dark .ecw__review-question-text {
  color: var(--ds-text);
}

.ecw__review-more {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  text-align: center;
  padding: 0.5rem;
}

.ecw__review-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.ecw__review-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
  font-weight: 600;
  background: var(--ds-gray-200);
  color: var(--ds-text-muted);
}

.dark .ecw__review-badge {
  background: var(--ds-gray-700);
  color: var(--ds-text-muted);
}

.ecw__review-badge--active {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.ecw__review-badge--inactive {
  background: var(--ds-gray-200);
  color: var(--ds-text-muted);
}

.dark .ecw__review-badge--inactive {
  background: var(--ds-gray-700);
  color: var(--ds-text-muted);
}

/* Footer */
.ecw__footer {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  background: var(--ds-gray-50);
  border-top: 1px solid var(--ds-border);
}

.dark .ecw__footer {
  background: var(--ds-gray-800);
  border-top-color: var(--ds-border-strong);
}

.ecw__footer-spacer {
  flex: 1;
}

.ecw__btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1.5px solid transparent;
}

.ecw__btn--ghost {
  background: transparent;
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .ecw__btn--ghost {
  border-color: var(--ds-border-strong);
  color: var(--ds-text-muted);
}

.ecw__btn--ghost:hover {
  background: var(--ds-gray-100);
}

.dark .ecw__btn--ghost:hover {
  background: var(--ds-gray-700);
}

.ecw__btn--primary {
  background: var(--ds-primary);
  color: white;
}

.ecw__btn--primary:hover {
  background: var(--ds-primary-hover);
  transform: translateY(-1px);
}

.ecw__btn--success {
  background: var(--ds-success);
  color: white;
}

.ecw__btn--success:hover:not(:disabled) {
  background: #15803d;
  transform: translateY(-1px);
}

.ecw__btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
}

.ecw__btn-spinner {
  font-size: 1.25rem;
}

/* Transitions */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.25s ease;
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
</style>
