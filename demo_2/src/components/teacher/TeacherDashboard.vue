<template>
  <div class="sm-app td-page">
    <!-- Page Header -->
    <header class="td-header">
      <div class="td-header__left">
        <h1 class="sm-heading-display">{{ greeting }}, {{ teacherName }}</h1>
        <p class="sm-body-small">{{ currentDate }}</p>
      </div>
      <div class="td-header__right">
        <div class="td-live-indicator" v-if="statusCounts.started > 0">
          <span class="td-live-dot"></span>
          <span>{{ statusCounts.started }} kỳ thi đang diễn ra</span>
        </div>
        <button class="sm-btn sm-btn--primary" @click="goToCreate">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          Tạo kỳ thi mới
        </button>
      </div>
    </header>

    <!-- Stats Row -->
    <section class="td-stats-row sm-stagger">
      <div class="td-stat-card">
        <div class="td-stat-card__icon td-stat-card__icon--default">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
          </svg>
        </div>
        <div class="td-stat-card__content">
          <span class="td-stat-card__value">{{ rawExams.length }}</span>
          <span class="td-stat-card__label">Tổng kỳ thi</span>
        </div>
      </div>

      <div class="td-stat-card" v-if="statusCounts.started > 0">
        <div class="td-stat-card__icon td-stat-card__icon--success">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <polygon points="10 8 16 12 10 16 10 8"></polygon>
          </svg>
        </div>
        <div class="td-stat-card__content">
          <span class="td-stat-card__value">{{ statusCounts.started }}</span>
          <span class="td-stat-card__label">Đang diễn ra</span>
        </div>
      </div>

      <div class="td-stat-card">
        <div class="td-stat-card__icon td-stat-card__icon--muted">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <polyline points="12 6 12 12 16 14"></polyline>
          </svg>
        </div>
        <div class="td-stat-card__content">
          <span class="td-stat-card__value">{{ statusCounts.upcoming }}</span>
          <span class="td-stat-card__label">Sắp diễn ra</span>
        </div>
      </div>

      <div class="td-stat-card">
        <div class="td-stat-card__icon td-stat-card__icon--muted">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
            <polyline points="22 4 12 14.01 9 11.01"></polyline>
          </svg>
        </div>
        <div class="td-stat-card__content">
          <span class="td-stat-card__value">{{ statusCounts.ended }}</span>
          <span class="td-stat-card__label">Đã kết thúc</span>
        </div>
      </div>
    </section>

    <!-- Live Exam Banner -->
    <section class="td-live-banner" v-if="liveExam" @click="goToMonitoring">
      <div class="td-live-banner__content">
        <div class="td-live-banner__header">
          <span class="td-live-badge">
            <span class="td-live-badge__dot"></span>
            LIVE
          </span>
          <span class="td-live-banner__title">{{ liveExam.title }}</span>
        </div>
        <div class="td-live-banner__meta">
          <span>{{ liveExam.studentCount }} thí sinh đang thi</span>
          <span class="td-live-banner__sep">·</span>
          <span>{{ liveExam.answeredCount }} đã trả lời</span>
          <span class="td-live-banner__sep" v-if="liveExam.alertCount > 0">·</span>
          <span class="td-live-banner__alert" v-if="liveExam.alertCount > 0">
            {{ liveExam.alertCount }} cảnh báo
          </span>
        </div>
      </div>
      <div class="td-live-banner__action">
        <span>Theo dõi ngay</span>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="5" y1="12" x2="19" y2="12"></line>
          <polyline points="12 5 19 12 12 19"></polyline>
        </svg>
      </div>
    </section>

    <!-- Main Content Grid -->
    <div class="td-main-grid">
      <!-- Recent Exams -->
      <section class="td-section">
        <div class="td-section__header">
          <h2 class="sm-heading-3">Kỳ thi gần đây</h2>
          <RouterLink to="/teacher/exams/list" class="sm-btn sm-btn--ghost sm-btn--sm">
            Xem tất cả
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </RouterLink>
        </div>
        
        <div class="td-exam-list sm-stagger">
          <div 
            v-for="exam in examTableData" 
            :key="exam.id" 
            class="td-exam-card"
            :class="{ 'td-exam-card--clickable': exam.statusKey === 'ended' }"
            @click="exam.statusKey === 'ended' ? openExamResult(exam) : null"
          >
            <div class="td-exam-card__main">
              <div class="td-exam-card__header">
                <span class="sm-badge" :class="getBadgeClass(exam.statusKey)">
                  {{ exam.status }}
                </span>
                <span class="td-exam-card__date">{{ exam.date }}</span>
              </div>
              <h3 class="td-exam-card__title">{{ exam.title }}</h3>
              <p class="td-exam-card__subtitle">{{ exam.subtitle }}</p>
            </div>
            <div class="td-exam-card__footer">
              <div class="td-exam-card__meta">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"></circle>
                  <polyline points="12 6 12 12 16 14"></polyline>
                </svg>
                <span>{{ exam.participants }}</span>
              </div>
              <div class="td-exam-card__actions">
                <button 
                  v-if="exam.statusKey === 'started'" 
                  class="sm-btn sm-btn--ghost sm-btn--sm"
                  @click.stop="goToMonitoring"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"></circle>
                    <polygon points="10 8 16 12 10 16 10 8"></polygon>
                  </svg>
                  Giám sát
                </button>
                <button 
                  v-if="exam.statusKey === 'ended'" 
                  class="sm-btn sm-btn--ghost sm-btn--sm"
                  @click.stop="openExamResult(exam)"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                    <circle cx="12" cy="12" r="3"></circle>
                  </svg>
                  Xem kết quả
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Quick Actions Sidebar -->
      <aside class="td-sidebar">
        <div class="td-sidebar__section">
          <h3 class="sm-label td-sidebar__title">Thao tác nhanh</h3>
          <div class="td-quick-actions">
            <button class="td-quick-action" @click="handleQuickAction('create')">
              <div class="td-quick-action__icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
              </div>
              <span>Tạo kỳ thi</span>
            </button>
            <button class="td-quick-action" @click="handleQuickAction('list')">
              <div class="td-quick-action__icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="8" y1="6" x2="21" y2="6"></line>
                  <line x1="8" y1="12" x2="21" y2="12"></line>
                  <line x1="8" y1="18" x2="21" y2="18"></line>
                  <line x1="3" y1="6" x2="3.01" y2="6"></line>
                  <line x1="3" y1="12" x2="3.01" y2="12"></line>
                  <line x1="3" y1="18" x2="3.01" y2="18"></line>
                </svg>
              </div>
              <span>Danh sách</span>
            </button>
            <button class="td-quick-action" @click="handleQuickAction('monitoring')" v-if="statusCounts.started > 0">
              <div class="td-quick-action__icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"></circle>
                  <polygon points="10 8 16 12 10 16 10 8"></polygon>
                </svg>
              </div>
              <span>Giám sát</span>
            </button>
            <button class="td-quick-action" @click="handleQuickAction('analytics')">
              <div class="td-quick-action__icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="18" y1="20" x2="18" y2="10"></line>
                  <line x1="12" y1="20" x2="12" y2="4"></line>
                  <line x1="6" y1="20" x2="6" y2="14"></line>
                </svg>
              </div>
              <span>Phân tích</span>
            </button>
            <button class="td-quick-action" @click="handleQuickAction('profile')">
              <div class="td-quick-action__icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
              </div>
              <span>Hồ sơ</span>
            </button>
          </div>
        </div>

        <div class="td-sidebar__section" v-if="upcomingExams.length > 0">
          <h3 class="sm-label td-sidebar__title">Sắp diễn ra</h3>
          <div class="td-upcoming-list">
            <div v-for="exam in upcomingExams" :key="exam.id" class="td-upcoming-item">
              <div class="td-upcoming-item__info">
                <span class="td-upcoming-item__title">{{ exam.title }}</span>
                <span class="td-upcoming-item__time">{{ formatDateTime(exam.startTime) }}</span>
              </div>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ApiError } from '../../services/apiClient'
import { listExams } from '../../services/examService'
import { listExamAttempts } from '../../services/attemptService'
import { useAuthStore } from '../../stores/authStore'
import { useToast } from '../../composables/useToast'
import { useScrollToTop } from '../../composables/useScrollToTop'

useScrollToTop()

const router = useRouter()
const toast = useToast()
const authStore = useAuthStore()

const rawExams = ref([])
const liveExamCounts = ref({})

// Exam helpers
const getExamStatusMeta = (exam) => {
  if (!exam.isActive) return { key: 'draft', label: 'Bản nháp' }
  const nowMs = Date.now()
  const startMs = new Date(exam.startTime || '').getTime()
  const endMs = new Date(exam.endTime || '').getTime()
  if (!Number.isNaN(startMs) && nowMs < startMs) return { key: 'upcoming', label: 'Sắp diễn ra' }
  if (!Number.isNaN(startMs) && !Number.isNaN(endMs) && nowMs >= startMs && nowMs <= endMs) return { key: 'started', label: 'Đang diễn ra' }
  return { key: 'ended', label: 'Đã kết thúc' }
}

const getSortTime = (exam) => {
  for (const value of [exam.endTime, exam.startTime, exam.updatedAt, exam.createdAt]) {
    const t = new Date(value || '').getTime()
    if (!Number.isNaN(t)) return t
  }
  return 0
}

const formatDateTime = (value) => {
  const d = new Date(value || '')
  return Number.isNaN(d.getTime()) ? '—' : d.toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const exams = computed(() => rawExams.value.slice().sort((a, b) => getSortTime(b) - getSortTime(a)))

const statusCounts = computed(() =>
  exams.value.reduce((acc, exam) => {
    const { key } = getExamStatusMeta(exam)
    acc[key] = (acc[key] || 0) + 1
    return acc
  }, { draft: 0, upcoming: 0, started: 0, ended: 0 })
)

const upcomingExams = computed(() =>
  exams.value.filter(e => getExamStatusMeta(e).key === 'upcoming').slice(0, 3)
)

const liveExam = computed(() => {
  const started = exams.value.find(e => getExamStatusMeta(e).key === 'started')
  if (!started) return null
  const counts = liveExamCounts.value[started.id] || {}
  return {
    ...started,
    isLive: true,
    studentCount: counts.studentCount ?? 0,
    answeredCount: counts.answeredCount ?? 0,
    questionCount: started.questionCount || 0,
    alertCount: counts.alertCount ?? 0
  }
})

const examTableData = computed(() =>
  exams.value.slice(0, 6).map(exam => {
    const { key, label } = getExamStatusMeta(exam)
    return {
      id: exam.id,
      title: exam.title,
      subtitle: exam.description || '—',
      date: formatDateTime(exam.endTime || exam.startTime),
      status: label,
      statusKey: key,
      participants: `${exam.questionCount ?? 0} câu`,
      disabled: key !== 'ended',
      resultPath: '/teacher/exams/review/summary'
    }
  })
)

const teacherName = computed(() => {
  const u = authStore.user
  if (u?.username) return u.username
  return 'Giáo viên'
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return 'Chào buổi sáng'
  if (hour < 18) return 'Chào buổi chiều'
  return 'Chào buổi tối'
})

const currentDate = computed(() => {
  return new Date().toLocaleDateString('vi-VN', { 
    weekday: 'long', 
    day: 'numeric', 
    month: 'long', 
    year: 'numeric' 
  })
})

const goToMonitoring = () => router.push('/teacher/live-monitoring')
const goToList = () => router.push('/teacher/exams/list')
const goToCreate = () => router.push('/teacher/exams/create')
const goToProfile = () => router.push('/teacher/profile')

const openExamResult = (exam) => {
  if (exam.disabled) return
  router.push({ path: exam.resultPath, query: { title: exam.title, examId: exam.id } })
}

const handleQuickAction = (actionId) => {
  switch (actionId) {
    case 'create': goToCreate(); break
    case 'list': goToList(); break
    case 'monitoring': goToMonitoring(); break
    case 'analytics': router.push('/teacher/exams/review/summary'); break
    case 'profile': goToProfile(); break
  }
}

const loadExams = async () => {
  try {
    const exams = await listExams()
    rawExams.value = exams

    const startedExams = exams.filter(e => {
      const { key } = getExamStatusMeta(e)
      return key === 'started'
    })
    const counts = {}
    await Promise.allSettled(
      startedExams.map(async (exam) => {
        try {
          const attempts = await listExamAttempts(exam.id)
          counts[exam.id] = {
            studentCount: attempts.length,
            answeredCount: attempts.filter(a => {
              const s = String(a.status || '').toUpperCase()
              return s === 'ACTIVE' || s === 'IN_PROGRESS'
            }).length,
            alertCount: attempts.filter(a => Number(a.riskScore || 0) > 30).length
          }
        } catch {
          counts[exam.id] = { studentCount: 0, answeredCount: 0, alertCount: 0 }
        }
      })
    )
    liveExamCounts.value = counts
  } catch (error) {
    toast.error(error instanceof ApiError ? error.message : 'Không thể tải dữ liệu dashboard.')
  }
}

const getBadgeClass = (statusKey) => {
  const classes = {
    draft: '',
    upcoming: 'sm-badge--info',
    started: 'sm-badge--success',
    ended: ''
  }
  return classes[statusKey] || ''
}

onMounted(async () => {
  await loadExams()
})
</script>

<style scoped>
/* Page */
.td-page {
  padding: var(--sm-space-8);
  max-width: 1400px;
  margin: 0 auto;
}

/* Header */
.td-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--sm-space-6);
  margin-bottom: var(--sm-space-8);
}

.td-header__left {
  flex: 1;
}

.td-header__left .sm-heading-display {
  margin-bottom: var(--sm-space-1);
}

.td-header__right {
  display: flex;
  align-items: center;
  gap: var(--sm-space-4);
}

.td-live-indicator {
  display: flex;
  align-items: center;
  gap: var(--sm-space-2);
  padding: var(--sm-space-2) var(--sm-space-3);
  font-size: var(--sm-text-sm);
  color: var(--sm-success-text);
  background: var(--sm-success-bg);
  border-radius: var(--sm-radius-full);
}

.td-live-dot {
  width: 8px;
  height: 8px;
  background: currentColor;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* Stats Row */
.td-stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--sm-space-4);
  margin-bottom: var(--sm-space-8);
}

.td-stat-card {
  display: flex;
  align-items: center;
  gap: var(--sm-space-4);
  padding: var(--sm-space-5);
  background: var(--sm-bg-secondary);
  border: 1px solid var(--sm-border-default);
  border-radius: var(--sm-radius-lg);
}

.td-stat-card__icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--sm-radius-md);
  flex-shrink: 0;
}

.td-stat-card__icon--default {
  background: var(--sm-bg-tertiary);
  color: var(--sm-text-secondary);
}

.td-stat-card__icon--success {
  background: var(--sm-success-bg);
  color: var(--sm-success-text);
}

.td-stat-card__icon--muted {
  background: var(--sm-bg-tertiary);
  color: var(--sm-text-tertiary);
}

.td-stat-card__content {
  display: flex;
  flex-direction: column;
  gap: var(--sm-space-1);
}

.td-stat-card__value {
  font-size: var(--sm-text-2xl);
  font-weight: 700;
  color: var(--sm-text-primary);
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.td-stat-card__label {
  font-size: var(--sm-text-sm);
  color: var(--sm-text-secondary);
}

/* Live Banner */
.td-live-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sm-space-4);
  padding: var(--sm-space-4) var(--sm-space-5);
  background: var(--sm-bg-secondary);
  border: 1px solid var(--sm-success-border);
  border-radius: var(--sm-radius-lg);
  margin-bottom: var(--sm-space-8);
  cursor: pointer;
  transition: all var(--sm-duration-normal) var(--sm-ease-out);
}

.td-live-banner:hover {
  box-shadow: var(--sm-shadow-md);
}

.td-live-banner__header {
  display: flex;
  align-items: center;
  gap: var(--sm-space-3);
  margin-bottom: var(--sm-space-2);
}

.td-live-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--sm-space-1);
  padding: var(--sm-space-1) var(--sm-space-2);
  font-size: var(--sm-text-xs);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--sm-success-text);
  background: var(--sm-success-bg);
  border-radius: var(--sm-radius-sm);
}

.td-live-badge__dot {
  width: 6px;
  height: 6px;
  background: var(--sm-success-text);
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}

.td-live-banner__title {
  font-size: var(--sm-text-lg);
  font-weight: 600;
  color: var(--sm-text-primary);
}

.td-live-banner__meta {
  display: flex;
  align-items: center;
  gap: var(--sm-space-2);
  font-size: var(--sm-text-sm);
  color: var(--sm-text-secondary);
}

.td-live-banner__sep {
  color: var(--sm-text-tertiary);
}

.td-live-banner__alert {
  color: var(--sm-warning-text);
  font-weight: 500;
}

.td-live-banner__action {
  display: flex;
  align-items: center;
  gap: var(--sm-space-2);
  font-size: var(--sm-text-sm);
  font-weight: 500;
  color: var(--sm-accent-primary);
}

/* Main Grid */
.td-main-grid {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: var(--sm-space-8);
}

@media (max-width: 1024px) {
  .td-main-grid {
    grid-template-columns: 1fr;
  }
}

/* Section */
.td-section {
  min-width: 0;
}

.td-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--sm-space-4);
}

/* Exam List */
.td-exam-list {
  display: flex;
  flex-direction: column;
  gap: var(--sm-space-3);
}

.td-exam-card {
  background: var(--sm-bg-secondary);
  border: 1px solid var(--sm-border-default);
  border-radius: var(--sm-radius-lg);
  padding: var(--sm-space-5);
  transition: all var(--sm-duration-normal) var(--sm-ease-out);
}

.td-exam-card--clickable {
  cursor: pointer;
}

.td-exam-card--clickable:hover {
  border-color: var(--sm-border-strong);
  box-shadow: var(--sm-shadow-sm);
}

.td-exam-card__header {
  display: flex;
  align-items: center;
  gap: var(--sm-space-3);
  margin-bottom: var(--sm-space-2);
}

.td-exam-card__date {
  font-size: var(--sm-text-xs);
  color: var(--sm-text-tertiary);
}

.td-exam-card__title {
  font-size: var(--sm-text-base);
  font-weight: 600;
  color: var(--sm-text-primary);
  margin-bottom: var(--sm-space-1);
}

.td-exam-card__subtitle {
  font-size: var(--sm-text-sm);
  color: var(--sm-text-secondary);
  margin-bottom: var(--sm-space-4);
}

.td-exam-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: var(--sm-space-3);
  border-top: 1px solid var(--sm-border-subtle);
}

.td-exam-card__meta {
  display: flex;
  align-items: center;
  gap: var(--sm-space-2);
  font-size: var(--sm-text-sm);
  color: var(--sm-text-tertiary);
}

.td-exam-card__actions {
  display: flex;
  gap: var(--sm-space-2);
}

/* Sidebar */
.td-sidebar {
  display: flex;
  flex-direction: column;
  gap: var(--sm-space-6);
}

.td-sidebar__section {
  background: var(--sm-bg-secondary);
  border: 1px solid var(--sm-border-default);
  border-radius: var(--sm-radius-lg);
  padding: var(--sm-space-5);
}

.td-sidebar__title {
  margin-bottom: var(--sm-space-4);
}

/* Quick Actions */
.td-quick-actions {
  display: flex;
  flex-direction: column;
  gap: var(--sm-space-2);
}

.td-quick-action {
  display: flex;
  align-items: center;
  gap: var(--sm-space-3);
  padding: var(--sm-space-3);
  background: transparent;
  border: none;
  border-radius: var(--sm-radius-md);
  cursor: pointer;
  transition: all var(--sm-duration-fast) var(--sm-ease-out);
  text-align: left;
  width: 100%;
}

.td-quick-action:hover {
  background: var(--sm-bg-tertiary);
}

.td-quick-action__icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--sm-bg-tertiary);
  border-radius: var(--sm-radius-md);
  color: var(--sm-text-secondary);
  flex-shrink: 0;
}

.td-quick-action:hover .td-quick-action__icon {
  background: var(--sm-accent-muted);
  color: var(--sm-accent-primary);
}

.td-quick-action span {
  font-size: var(--sm-text-sm);
  font-weight: 500;
  color: var(--sm-text-primary);
}

/* Upcoming List */
.td-upcoming-list {
  display: flex;
  flex-direction: column;
  gap: var(--sm-space-3);
}

.td-upcoming-item {
  padding-bottom: var(--sm-space-3);
  border-bottom: 1px solid var(--sm-border-subtle);
}

.td-upcoming-item:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.td-upcoming-item__title {
  display: block;
  font-size: var(--sm-text-sm);
  font-weight: 500;
  color: var(--sm-text-primary);
  margin-bottom: var(--sm-space-1);
}

.td-upcoming-item__time {
  font-size: var(--sm-text-xs);
  color: var(--sm-text-tertiary);
}

/* Responsive */
@media (max-width: 768px) {
  .td-page {
    padding: var(--sm-space-4);
  }

  .td-header {
    flex-direction: column;
    gap: var(--sm-space-4);
  }

  .td-header__right {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .td-live-banner {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
