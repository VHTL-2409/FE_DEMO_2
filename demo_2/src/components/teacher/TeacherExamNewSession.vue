<template>
  <div class="sm-app ts-page">
    <!-- Page Header -->
    <header class="ts-header">
      <div class="ts-header__left">
        <RouterLink to="/teacher/exams" class="ts-back">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6"></polyline>
          </svg>
          <span>Quay lại</span>
        </RouterLink>
        <div class="ts-header__info">
          <h1 class="sm-heading-2">Thiết lập phiên thi</h1>
          <p class="sm-body-small">{{ examTitle }}</p>
        </div>
      </div>
    </header>

    <!-- Main Form -->
    <div class="ts-form-container">
      <!-- Exam Info Card -->
      <div class="ts-exam-card">
        <div class="ts-exam-card__icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
          </svg>
        </div>
        <div class="ts-exam-card__content">
          <h3 class="ts-exam-card__title">{{ examTitle }}</h3>
          <span class="sm-badge">Bản nháp</span>
        </div>
      </div>

      <!-- Schedule Section -->
      <section class="ts-section">
        <div class="ts-section__header">
          <h2 class="sm-heading-4">Lịch thi</h2>
          <p class="sm-body-small">Thiết lập thời gian bắt đầu và kết thúc cho phiên thi này</p>
        </div>

        <div class="ts-grid ts-grid--2">
          <div class="sm-field">
            <label class="sm-field__label">Thời gian bắt đầu <span class="ts-required">*</span></label>
            <input
              v-model="formData.startTime"
              type="datetime-local"
              class="sm-input"
            />
          </div>

          <div class="sm-field">
            <label class="sm-field__label">Thời gian kết thúc <span class="ts-required">*</span></label>
            <input
              v-model="formData.endTime"
              type="datetime-local"
              class="sm-input"
            />
          </div>
        </div>

        <div class="sm-field" style="max-width: 200px;">
          <label class="sm-field__label">Thời gian làm bài (phút) <span class="ts-required">*</span></label>
          <input
            v-model.number="formData.durationMinutes"
            type="number"
            min="1"
            max="300"
            class="sm-input"
          />
          <span class="sm-field__hint">Thời gian làm bài tối đa cho mỗi thí sinh</span>
        </div>
      </section>

      <!-- Proctoring Section -->
      <section class="ts-section">
        <div class="ts-section__header">
          <h2 class="sm-heading-4">Cài đặt giám sát</h2>
          <p class="sm-body-small">Cấu hình các tính năng chống gian lận cho phiên thi</p>
        </div>

        <div class="ts-toggle-list">
          <label class="ts-toggle-item">
            <div class="ts-toggle-item__content">
              <div class="ts-toggle-item__header">
                <div class="ts-toggle-item__icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                    <circle cx="12" cy="13" r="4"></circle>
                  </svg>
                </div>
                <span class="ts-toggle-item__title">Giám sát camera</span>
              </div>
              <span class="ts-toggle-item__desc">Yêu cầu thí sinh bật camera trong quá trình thi</span>
            </div>
            <input type="checkbox" v-model="formData.cameraEnabled" class="ts-toggle-input" />
            <span class="ts-toggle-switch" :class="{ 'ts-toggle-switch--active': formData.cameraEnabled }"></span>
          </label>

          <label class="ts-toggle-item">
            <div class="ts-toggle-item__content">
              <div class="ts-toggle-item__header">
                <div class="ts-toggle-item__icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path>
                    <path d="M19 10v2a7 7 0 0 1-14 0v-2"></path>
                    <line x1="12" y1="19" x2="12" y2="23"></line>
                    <line x1="8" y1="23" x2="16" y2="23"></line>
                  </svg>
                </div>
                <span class="ts-toggle-item__title">Giám sát microphone</span>
              </div>
              <span class="ts-toggle-item__desc">Yêu cầu thí sinh bật microphone trong quá trình thi</span>
            </div>
            <input type="checkbox" v-model="formData.micEnabled" class="ts-toggle-input" />
            <span class="ts-toggle-switch" :class="{ 'ts-toggle-switch--active': formData.micEnabled }"></span>
          </label>

          <label class="ts-toggle-item">
            <div class="ts-toggle-item__content">
              <div class="ts-toggle-item__header">
                <div class="ts-toggle-item__icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
                    <line x1="8" y1="21" x2="16" y2="21"></line>
                    <line x1="12" y1="17" x2="12" y2="21"></line>
                  </svg>
                </div>
                <span class="ts-toggle-item__title">Phát hiện chuyển tab</span>
              </div>
              <span class="ts-toggle-item__desc">Cảnh báo khi thí sinh chuyển sang tab khác</span>
            </div>
            <input type="checkbox" v-model="formData.tabSwitchDetection" class="ts-toggle-input" />
            <span class="ts-toggle-switch" :class="{ 'ts-toggle-switch--active': formData.tabSwitchDetection }"></span>
          </label>

          <label class="ts-toggle-item">
            <div class="ts-toggle-item__content">
              <div class="ts-toggle-item__header">
                <div class="ts-toggle-item__icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                    <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                  </svg>
                </div>
                <span class="ts-toggle-item__title">Chống copy câu hỏi</span>
              </div>
              <span class="ts-toggle-item__desc">Ngăn thí sinh copy nội dung câu hỏi</span>
            </div>
            <input type="checkbox" v-model="formData.preventCopy" class="ts-toggle-input" />
            <span class="ts-toggle-switch" :class="{ 'ts-toggle-switch--active': formData.preventCopy }"></span>
          </label>
        </div>
      </section>
    </div>

    <!-- Action Bar -->
    <div class="ts-actions">
      <button class="sm-btn sm-btn--secondary" @click="cancel">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
        Hủy bỏ
      </button>
      <button class="sm-btn sm-btn--ghost" @click="saveDraft">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
          <polyline points="17 21 17 13 7 13 7 21"></polyline>
          <polyline points="7 3 7 8 15 8"></polyline>
        </svg>
        Lưu nháp
      </button>
      <button class="sm-btn sm-btn--primary" @click="publish">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 2L11 13"></path>
          <path d="M22 2L15 22 11 13 2 9l20-7z"></path>
        </svg>
        Xuất bản ngay
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import { publishExam, createNewSession } from '../../services/examService'
import { useToast } from '../../composables/useToast'

const router = useRouter()
const route = useRoute()
const toast = useToast()

const examId = route.query.examId || ''
const examTitle = route.query.title || 'Đề thi chưa có tiêu đề'

const formData = ref({
  startTime: '',
  endTime: '',
  durationMinutes: 30,
  cameraEnabled: true,
  micEnabled: false,
  tabSwitchDetection: true,
  preventCopy: true
})

const now = new Date()
const defaultStart = new Date(now.getTime() + 24 * 60 * 60 * 1000)
const defaultEnd = new Date(defaultStart.getTime() + 2 * 60 * 60 * 1000)

formData.value.startTime = formatDateTimeLocal(defaultStart)
formData.value.endTime = formatDateTimeLocal(defaultEnd)

function formatDateTimeLocal(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}`
}

const cancel = () => {
  router.back()
}

const saveDraft = async () => {
  try {
    await createNewSession(examId, {
      startTime: formData.value.startTime,
      endTime: formData.value.endTime,
      durationMinutes: formData.value.durationMinutes
    })
    toast.info('Đã lưu nháp phiên thi.')
    router.push({ path: '/teacher/exams', query: { draft: 'true' } })
  } catch (err) {
    toast.error('Lưu nháp thất bại.')
  }
}

const publish = async () => {
  try {
    const session = await createNewSession(examId, {
      startTime: formData.value.startTime,
      endTime: formData.value.endTime,
      durationMinutes: formData.value.durationMinutes
    })
    await publishExam(examId)
    toast.success('Xuất bản phiên thi thành công!')
    router.push({
      path: '/teacher/exams/created-success',
      query: {
        examId,
        code: session?.code || '',
        title: examTitle,
        durationMinutes: formData.value.durationMinutes,
        startAt: formData.value.startTime,
        endAt: formData.value.endTime,
        questionCount: route.query.questionCount || '0'
      }
    })
  } catch (err) {
    toast.error('Xuất bản thất bại: ' + (err.message || 'Lỗi không xác định'))
  }
}
</script>

<style scoped>
.ts-page {
  padding: var(--sm-space-8);
  max-width: 800px;
  margin: 0 auto;
}

/* Header */
.ts-header {
  margin-bottom: var(--sm-space-8);
}

.ts-header__left {
  display: flex;
  flex-direction: column;
  gap: var(--sm-space-4);
}

.ts-back {
  display: inline-flex;
  align-items: center;
  gap: var(--sm-space-2);
  font-size: var(--sm-text-sm);
  font-weight: 500;
  color: var(--sm-text-secondary);
  text-decoration: none;
  transition: color var(--sm-duration-fast) var(--sm-ease-out);
}

.ts-back:hover {
  color: var(--sm-text-primary);
}

.ts-header__info {
  display: flex;
  flex-direction: column;
  gap: var(--sm-space-1);
}

/* Form Container */
.ts-form-container {
  display: flex;
  flex-direction: column;
  gap: var(--sm-space-6);
}

/* Exam Card */
.ts-exam-card {
  display: flex;
  align-items: center;
  gap: var(--sm-space-4);
  padding: var(--sm-space-5);
  background: var(--sm-bg-secondary);
  border: 1px solid var(--sm-border-default);
  border-radius: var(--sm-radius-lg);
}

.ts-exam-card__icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--sm-accent-muted);
  border-radius: var(--sm-radius-md);
  color: var(--sm-accent-primary);
  flex-shrink: 0;
}

.ts-exam-card__content {
  display: flex;
  flex-direction: column;
  gap: var(--sm-space-2);
}

.ts-exam-card__title {
  font-size: var(--sm-text-lg);
  font-weight: 600;
  color: var(--sm-text-primary);
  margin: 0;
}

/* Sections */
.ts-section {
  background: var(--sm-bg-secondary);
  border: 1px solid var(--sm-border-default);
  border-radius: var(--sm-radius-lg);
  padding: var(--sm-space-6);
}

.ts-section__header {
  margin-bottom: var(--sm-space-6);
}

.ts-section__header h2 {
  margin-bottom: var(--sm-space-1);
}

/* Grid */
.ts-grid {
  display: grid;
  gap: var(--sm-space-4);
}

.ts-grid--2 {
  grid-template-columns: repeat(2, 1fr);
}

@media (max-width: 640px) {
  .ts-grid--2 {
    grid-template-columns: 1fr;
  }
}

/* Required */
.ts-required {
  color: var(--sm-error-text);
}

/* Toggle List */
.ts-toggle-list {
  display: flex;
  flex-direction: column;
  gap: var(--sm-space-3);
}

.ts-toggle-item {
  display: flex;
  align-items: center;
  gap: var(--sm-space-4);
  padding: var(--sm-space-4);
  background: var(--sm-bg-tertiary);
  border-radius: var(--sm-radius-md);
  cursor: pointer;
  transition: background var(--sm-duration-fast) var(--sm-ease-out);
}

.ts-toggle-item:hover {
  background: var(--sm-border-subtle);
}

.ts-toggle-item__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--sm-space-1);
}

.ts-toggle-item__header {
  display: flex;
  align-items: center;
  gap: var(--sm-space-3);
}

.ts-toggle-item__icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--sm-bg-secondary);
  border-radius: var(--sm-radius-sm);
  color: var(--sm-text-secondary);
}

.ts-toggle-item__title {
  font-size: var(--sm-text-sm);
  font-weight: 500;
  color: var(--sm-text-primary);
}

.ts-toggle-item__desc {
  font-size: var(--sm-text-xs);
  color: var(--sm-text-tertiary);
  margin-left: 44px;
}

.ts-toggle-input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.ts-toggle-switch {
  position: relative;
  width: 44px;
  height: 24px;
  background: var(--sm-border-default);
  border-radius: 12px;
  flex-shrink: 0;
  transition: background var(--sm-duration-normal) var(--sm-ease-out);
}

.ts-toggle-switch::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 50%;
  box-shadow: var(--sm-shadow-sm);
  transition: transform var(--sm-duration-normal) var(--sm-ease-out);
}

.ts-toggle-switch--active {
  background: var(--sm-accent-primary);
}

.ts-toggle-switch--active::after {
  transform: translateX(20px);
}

/* Actions */
.ts-actions {
  position: sticky;
  bottom: 0;
  display: flex;
  justify-content: flex-end;
  gap: var(--sm-space-3);
  padding: var(--sm-space-4) var(--sm-space-6);
  background: var(--sm-bg-secondary);
  border-top: 1px solid var(--sm-border-default);
  margin: var(--sm-space-8) calc(-1 * var(--sm-space-8)) calc(-1 * var(--sm-space-8));
  border-radius: 0 0 var(--sm-radius-lg) var(--sm-radius-lg);
}

@media (max-width: 640px) {
  .ts-page {
    padding: var(--sm-space-4);
  }
  
  .ts-actions {
    margin: var(--sm-space-6) calc(-1 * var(--sm-space-4)) calc(-1 * var(--sm-space-4));
    flex-wrap: wrap;
  }
  
  .ts-actions .sm-btn {
    flex: 1;
  }
}
</style>
