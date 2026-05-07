<template>
  <div class="wr-page">
    <div class="wr-container">
      <!-- Header -->
      <div class="wr-header">
        <div class="wr-header__left">
          <button class="wr-btn wr-btn--ghost" @click="goBack">
            <LucideIcon name="arrow_back" />
            Quay lại
          </button>
          <div class="wr-header__info">
            <h1 class="wr-header__title">{{ examTitle }}</h1>
            <div class="wr-header__meta">
              <span v-if="examType === 'free'" class="wr-badge wr-badge--info">
                <LucideIcon name="vpn_key" size="14" />
                {{ examCode }}
              </span>
              <span v-else class="wr-badge wr-badge--primary">
                <LucideIcon name="school" size="14" />
                Bài thi riêng tư
              </span>
              <span class="wr-badge wr-badge--secondary">
                <LucideIcon name="timer" size="14" />
                {{ durationMinutes }} phút
              </span>
            </div>
          </div>
        </div>
        <div class="wr-header__stats">
          <div class="wr-stat">
            <span class="wr-stat__value">{{ waitingStudents.length }}</span>
            <span class="wr-stat__label">Đang chờ</span>
          </div>
          <div class="wr-stat wr-stat--success">
            <span class="wr-stat__value">{{ inProgressCount }}</span>
            <span class="wr-stat__label">Đang thi</span>
          </div>
          <div class="wr-stat wr-stat--warning">
            <span class="wr-stat__value">{{ submittedCount }}</span>
            <span class="wr-stat__label">Đã nộp</span>
          </div>
        </div>
        <div class="wr-header__actions">
          <div class="wr-sync-status" :class="{ 'wr-sync-status--syncing': isPolling }">
            <span class="wr-sync-dot" :class="{ 'wr-sync-dot--live': isConnected }"></span>
            {{ isConnected ? 'Kết nối thời gian thực' : 'Đã cập nhật: ' + lastSyncLabel }}
          </div>
        </div>
      </div>

      <!-- Main content -->
      <div class="wr-content">
        <!-- Left panel: Student list -->
        <div class="wr-panel wr-panel--main">
          <div class="wr-panel__header">
            <h2 class="wr-panel__title">
              <LucideIcon name="group" />
              Danh sách học sinh
              <span class="wr-panel__count">{{ waitingStudents.length }}</span>
            </h2>
            <button class="wr-btn wr-btn--ghost wr-btn--sm" @click="refreshNow" :disabled="isPolling">
              <LucideIcon name="refresh" :class="{ 'wr-spin': isPolling }" />
              Làm mới
            </button>
          </div>

          <div v-if="isLoading && waitingStudents.length === 0" class="wr-empty">
            <LucideIcon name="hourglass_empty" size="48" />
            <p>Đang tải danh sách...</p>
          </div>

          <div v-else-if="waitingStudents.length === 0" class="wr-empty">
            <LucideIcon name="group" size="48" />
            <p>Chưa có học sinh nào tham gia</p>
            <p class="wr-empty__sub">Danh sách sẽ cập nhật khi học sinh tham gia phòng chờ</p>
          </div>

          <div v-else class="wr-student-list">
            <div
              v-for="student in waitingStudents"
              :key="student.attemptId"
              class="wr-student-card"
              :class="getStudentCardClass(student)"
            >
              <div class="wr-student-card__avatar">
                {{ getInitials(student.studentName) }}
              </div>
              <div class="wr-student-card__info">
                <p class="wr-student-card__name">{{ student.studentName }}</p>
                <p class="wr-student-card__email">{{ student.studentEmail }}</p>
                <p v-if="student.joinedAt" class="wr-student-card__time">
                  <LucideIcon name="schedule" size="12" />
                  Tham gia: {{ formatTime(student.joinedAt) }}
                </p>
              </div>
              <div class="wr-student-card__status">
                <span class="wr-status-badge" :class="getStatusBadgeClass(student.status)">
                  {{ student.status }}
                </span>
                <span v-if="student.riskScore > 0" class="wr-risk-indicator" :class="getRiskClass(student.riskScore)">
                  <LucideIcon name="warning" size="14" />
                  {{ student.riskScore }}%
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Right panel: Actions -->
        <div class="wr-panel wr-panel--sidebar">
          <div class="wr-action-card wr-action-card--primary" @click="goToMonitoring">
            <div class="wr-action-card__icon">
              <LucideIcon name="monitor_heart" size="28" />
            </div>
            <div class="wr-action-card__content">
              <h3>Giám sát ngay</h3>
              <p>Theo dõi tiến trình thi của học sinh</p>
            </div>
            <LucideIcon name="chevron_right" size="20" class="wr-action-card__arrow" />
          </div>

          <div class="wr-action-card" @click="goToExamList">
            <div class="wr-action-card__icon">
              <LucideIcon name="assignment" size="24" />
            </div>
            <div class="wr-action-card__content">
              <h3>Danh sách đề thi</h3>
              <p>Quản lý các đề thi của bạn</p>
            </div>
            <LucideIcon name="chevron_right" size="20" class="wr-action-card__arrow" />
          </div>

          <div class="wr-action-card" @click="copyExamCode">
            <div class="wr-action-card__icon">
              <LucideIcon name="content_copy" size="24" />
            </div>
            <div class="wr-action-card__content">
              <h3>{{ examType === 'free' ? 'Sao chép mã đề thi' : 'Thông tin đề thi' }}</h3>
              <p v-if="examType === 'free'">{{ examCode }}</p>
              <p v-else>Chia sẻ với học sinh trong lớp</p>
            </div>
            <LucideIcon name="chevron_right" size="20" class="wr-action-card__arrow" />
          </div>

          <!-- Exam Schedule -->
          <div class="wr-schedule-card">
            <h4 class="wr-schedule-card__title">
              <LucideIcon name="schedule" size="16" />
              Lịch thi
            </h4>
            <div class="wr-schedule-card__times">
              <div class="wr-schedule-card__time">
                <span class="wr-schedule-card__label">Bắt đầu</span>
                <span class="wr-schedule-card__value">{{ formatScheduleTime(startAt) }}</span>
              </div>
              <div class="wr-schedule-card__time">
                <span class="wr-schedule-card__label">Kết thúc</span>
                <span class="wr-schedule-card__value">{{ formatScheduleTime(endAt) }}</span>
              </div>
            </div>
          </div>

          <!-- Countdown -->
          <div v-if="!isExamEnded && startAt" class="wr-countdown-card">
            <h4 class="wr-countdown-card__title">Thời gian đến khi bắt đầu</h4>
            <div class="wr-countdown-card__value">{{ countdownLabel }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Flash events notification overlay -->
    <div class="wr-flash-container">
      <TransitionGroup name="wr-flash">
        <div
          v-for="event in flashEvents"
          :key="event.id"
          class="wr-flash-item"
        >
          <LucideIcon name="zap" size="14" />
          {{ event.message }}
        </div>
      </TransitionGroup>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getExamDetail, getWaitingStudents } from '../../services/examService'
import { useToast } from '../../composables/useToast'
import { useRealtimeChannel } from '../../composables/useRealtimeChannel'
import { parseBackendDate } from '../../utils/dateUtils.js'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const { isConnected, lastError, connect, disconnect } = useRealtimeChannel()

// State
const examTitle = ref('')
const examCode = ref('')
const examType = ref('free')
const durationMinutes = ref(60)
const startAt = ref(null)
const endAt = ref(null)
const waitingStudents = ref([])
const isLoading = ref(true)
const isPolling = ref(false)
const lastSyncedAt = ref(null)
const nowMs = ref(Date.now())
let pollTimerId = null
let countdownTimerId = null

// Computed
const inProgressCount = computed(() =>
  waitingStudents.value.filter(s => s.status === 'Đang thi').length
)

const submittedCount = computed(() =>
  waitingStudents.value.filter(s => s.status === 'Đã nộp').length
)

const lastSyncLabel = computed(() => {
  if (!lastSyncedAt.value) return 'chưa có dữ liệu'
  return new Date(lastSyncedAt.value).toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
})

const startAtDate = computed(() => parseBackendDate(startAt.value))
const endAtDate = computed(() => parseBackendDate(endAt.value))

const isExamEnded = computed(() => {
  const end = endAtDate.value
  return end ? nowMs.value > end.getTime() : false
})

const countdownLabel = computed(() => {
  if (!startAtDate.value) return '--:--:--'
  const diffMs = startAtDate.value.getTime() - nowMs.value
  if (diffMs <= 0) return '00:00:00'
  const totalSeconds = Math.floor(diffMs / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

// ── Realtime: xử lý thông báo từ WebSocket ──────────────────────────────────
function handleWaitingRoomEvent(event) {
  if (!event) return

  switch (event.type) {
    case 'STUDENT_JOINED':
      upsertStudent({
        attemptId: event.attemptId,
        studentName: event.studentName || event.student?.name || '—',
        studentEmail: event.studentEmail || event.student?.email || '—',
        status: 'Đang chờ',
        riskScore: event.riskScore ?? 0,
        joinedAt: new Date().toISOString()
      })
      addFlashEvent(`${event.studentName || 'Một học sinh'} đã tham gia`)
      break

    case 'STUDENT_STARTED':
    case 'EXAM_STARTED':
    case 'ATTEMPT_STARTED':
    case 'ATTEMPT_JOINED': {
      const displayName = event.studentName || event.student?.name || event.student || 'Học sinh'
      upsertStudent({
        attemptId: event.attemptId,
        studentName: displayName,
        studentEmail: event.studentEmail || event.email || event.student?.email || '—',
        status: 'Đang thi',
        riskScore: event.riskScore ?? 0,
        joinedAt: event.startedAt || event.issuedAt || new Date().toISOString()
      })
      updateStudentStatus(event.attemptId, 'Đang thi')
      const isAttemptPresenceEvent = event.type === 'ATTEMPT_STARTED' || event.type === 'ATTEMPT_JOINED'
      addFlashEvent(`${displayName} ${isAttemptPresenceEvent ? 'đã vào phòng thi' : 'đã bắt đầu làm bài'}`)
      break
    }

    case 'ATTEMPT_SUBMITTED':
    case 'SUBMITTED':
    case 'AUTO_SUBMITTED':
    case 'STUDENT_SUBMITTED':
    case 'EXAM_SUBMITTED':
      {
        const nextStudent = {
          attemptId: event.attemptId,
          studentName: event.studentName || event.student || 'Học sinh',
          studentEmail: event.studentEmail || event.email || event.student?.email || '—',
          status: 'Đã nộp',
          joinedAt: event.startedAt || event.issuedAt || new Date().toISOString(),
          submittedAt: event.submittedAt || event.issuedAt || new Date().toISOString()
        }
        if (event.riskScore != null) nextStudent.riskScore = event.riskScore
        upsertStudent(nextStudent)
      }
      updateStudentStatus(event.attemptId, 'Đã nộp')
      addFlashEvent(`${event.studentName || 'Học sinh'} đã nộp bài`)
      break

    case 'ATTEMPT_PAUSED':
    case 'STUDENT_PAUSED':
      updateStudentStatus(event.attemptId, 'Tạm dừng')
      addFlashEvent(`${event.studentName || 'Học sinh'} bị tạm dừng`)
      break

    case 'ATTEMPT_RESUMED':
      updateStudentStatus(event.attemptId, 'Đang thi')
      addFlashEvent(`${event.studentName || 'Học sinh'} được tiếp tục`)
      break

    case 'RISK_UPDATED':
      updateStudentRisk(event.attemptId, event.riskScore ?? 0)
      if ((event.riskScore ?? 0) > 50) {
        addFlashEvent(`${event.studentName || 'Học sinh'} có hành vi đáng ngờ (${event.riskScore}%)`)
      }
      break

    case 'ATTEMPT_STOPPED':
    case 'STUDENT_STOPPED':
      updateStudentStatus(event.attemptId, 'Đã dừng')
      addFlashEvent(`${event.studentName || 'Học sinh'} bị đình chỉ`)
      break

    default:
      break
  }
}

function upsertStudent(student) {
  const existing = waitingStudents.value.findIndex(s => s.attemptId === student.attemptId)
  if (existing >= 0) {
    waitingStudents.value[existing] = { ...waitingStudents.value[existing], ...student }
  } else {
    waitingStudents.value = [...waitingStudents.value, student]
  }
}

function updateStudentStatus(attemptId, status) {
  const idx = waitingStudents.value.findIndex(s => s.attemptId === attemptId)
  if (idx >= 0) {
    waitingStudents.value[idx] = { ...waitingStudents.value[idx], status }
  }
}

function updateStudentRisk(attemptId, riskScore) {
  const idx = waitingStudents.value.findIndex(s => s.attemptId === attemptId)
  if (idx >= 0) {
    waitingStudents.value[idx] = { ...waitingStudents.value[idx], riskScore }
  }
}

// Flash events (toast-like inline notification)
const flashEvents = ref([])
let flashId = 0
function addFlashEvent(message) {
  const id = ++flashId
  flashEvents.value = [{ id, message }, ...flashEvents.value]
  setTimeout(() => {
    flashEvents.value = flashEvents.value.filter(e => e.id !== id)
  }, 4000)
}

// ── WebSocket setup ──────────────────────────────────────────────────────────
let reconnectTimer = null

async function connectRealtime() {
  const examId = getExamId()
  if (!examId) return

  await connect({
    topics: [
      {
        destination: `/topic/exams/${examId}/alerts`,
        handler: (body) => {
          handleWaitingRoomEvent(body)
        }
      }
    ],
    reconnectDelay: 5000,
    onConnect: () => {
      lastSyncedAt.value = Date.now()
    },
    onDisconnect: () => {
      scheduleReconnect()
    },
    onError: () => {
      scheduleReconnect()
    }
  })
}

function scheduleReconnect() {
  if (reconnectTimer) return
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    const examId = getExamId()
    if (examId) connectRealtime()
  }, 5000)
}

// ── Methods ──────────────────────────────────────────────────────────────────
const getExamId = () => route.query.examId || null

const fetchExamInfo = async () => {
  const examId = getExamId()
  if (!examId) return

  try {
    const exam = await getExamDetail(examId)
    if (exam) {
      examTitle.value = exam.title || 'Đề thi'
      examCode.value = exam.code || ''
      durationMinutes.value = exam.durationMinutes || 60
      startAt.value = exam.startTime || null
      endAt.value = exam.endTime || null
    }
  } catch (err) {
    console.warn('Could not fetch exam info:', err)
  }
}

const fetchWaitingStudents = async () => {
  const examId = getExamId()
  if (!examId) return

  isPolling.value = true
  try {
    const students = await getWaitingStudents(examId)
    waitingStudents.value = students || []
    lastSyncedAt.value = Date.now()
  } catch (err) {
    console.warn('Could not fetch waiting students:', err)
  } finally {
    isPolling.value = false
  }
}

const refreshNow = async () => {
  await fetchWaitingStudents()
}

const startPolling = () => {
  fetchWaitingStudents()
  // Poll only when WebSocket is disconnected
  pollTimerId = window.setInterval(() => {
    if (!isConnected.value) {
      fetchWaitingStudents()
    }
  }, 5000)
}

const getInitials = (name) => {
  if (!name) return '?'
  const parts = name.trim().split(' ')
  if (parts.length >= 2) {
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
  }
  return name.substring(0, 2).toUpperCase()
}

const getStudentCardClass = (student) => {
  if (student.status === 'Đang thi') return 'wr-student-card--active'
  if (student.status === 'Đã nộp') return 'wr-student-card--submitted'
  if (student.riskScore > 50) return 'wr-student-card--warning'
  return ''
}

const getStatusBadgeClass = (status) => {
  if (status === 'Đang thi') return 'wr-status-badge--active'
  if (status === 'Đã nộp') return 'wr-status-badge--submitted'
  if (status === 'Tạm dừng') return 'wr-status-badge--paused'
  return 'wr-status-badge--waiting'
}

const getRiskClass = (score) => {
  if (score > 80) return 'wr-risk-indicator--high'
  if (score > 50) return 'wr-risk-indicator--medium'
  return 'wr-risk-indicator--low'
}

const formatTime = (isoString) => {
  const date = parseBackendDate(isoString)
  if (!date) return isoString || ''
  return date.toLocaleTimeString('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatScheduleTime = (isoString) => {
  const date = parseBackendDate(isoString)
  if (!date) return '-'
  return date.toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

  const goToMonitoring = () => {
  const examId = getExamId()
  if (examId) {
    router.push(`/teacher/exams/${examId}/monitoring`)
  } else {
    router.push('/teacher/live-monitoring')
  }
}

const goToExamList = () => {
  router.push('/teacher/exams/list')
}

const goBack = () => {
  router.back()
}

const copyExamCode = () => {
  if (examCode.value) {
    navigator.clipboard.writeText(examCode.value)
    toast.success('Đã sao chép mã đề thi!')
  } else {
    toast.info('Không có mã để sao chép')
  }
}

// ── Lifecycle ──────────────────────────────────────────────────────────────────
onMounted(async () => {
  if (route.query.title) examTitle.value = route.query.title
  if (route.query.examCode) examCode.value = route.query.examCode
  if (route.query.examType) examType.value = route.query.examType
  if (route.query.durationMinutes) durationMinutes.value = route.query.durationMinutes
  if (route.query.startAt) startAt.value = route.query.startAt
  if (route.query.endAt) endAt.value = route.query.endAt

  await fetchExamInfo()
  isLoading.value = false
  startPolling()
  await connectRealtime()

  countdownTimerId = window.setInterval(() => {
    nowMs.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (pollTimerId) {
    window.clearInterval(pollTimerId)
  }
  if (countdownTimerId) {
    window.clearInterval(countdownTimerId)
  }
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
  }
  disconnect()
})
</script>

<style scoped>
/* Page Layout */
.wr-page {
  min-height: 100vh;
  background: var(--ds-bg);
  padding: 1.5rem;
}

.wr-container {
  max-width: 1400px;
  margin: 0 auto;
}

@media (min-width: 1400px) {
  .wr-container { max-width: 1500px; }
}

@media (min-width: 1600px) {
  .wr-container { max-width: 1600px; }
}

/* Header */
.wr-header {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  padding: 1.25rem 1.5rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  margin-bottom: 1.5rem;
}

.wr-header__left {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex: 1;
}

.wr-header__info {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.wr-header__title {
  font-family: var(--ds-font-display);
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.wr-header__meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.wr-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.75rem;
  font-weight: 600;
}

.wr-badge--info {
  background: var(--ds-info-soft);
  color: var(--ds-info);
}

.wr-badge--primary {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.wr-badge--secondary {
  background: var(--ds-gray-100);
  color: var(--ds-text-secondary);
}

.wr-header__stats {
  display: flex;
  gap: 1.5rem;
}

.wr-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
}

.wr-stat__value {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--ds-text);
  line-height: 1;
}

.wr-stat--success .wr-stat__value {
  color: var(--ds-success);
}

.wr-stat--warning .wr-stat__value {
  color: var(--ds-warning);
}

.wr-stat__label {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.wr-header__actions {
  display: flex;
  align-items: center;
}

.wr-sync-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  background: var(--ds-gray-100);
  border-radius: var(--ds-radius-full);
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}

.wr-sync-status--syncing {
  background: var(--ds-info-soft);
  color: var(--ds-info);
}

.wr-sync-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.wr-sync-dot--live {
  background: var(--ds-success);
  animation: pulse-dot 2s infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.wr-sync-status--syncing .wr-sync-dot {
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* Content Grid */
.wr-content {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 1.5rem;
}

@media (max-width: 1024px) {
  .wr-content {
    grid-template-columns: 1fr;
  }
}

/* Panels */
.wr-panel {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.wr-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
}

.wr-panel__title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.wr-panel__count {
  padding: 0.125rem 0.5rem;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border-radius: var(--ds-radius-full);
  font-size: 0.75rem;
}

/* Student List */
.wr-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  color: var(--ds-text-muted);
  text-align: center;
  gap: 0.5rem;
}

.wr-empty__sub {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
}

.wr-student-list {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  max-height: 600px;
  overflow-y: auto;
}

.wr-student-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  transition: border-color 0.15s ease, background-color 0.15s ease;
}

.wr-student-card:hover {
  border-color: var(--ds-primary-border);
}

.wr-student-card--active {
  border-color: var(--ds-success-border);
  background: var(--ds-success-soft);
}

.wr-student-card--submitted {
  border-color: var(--ds-info-border);
  background: var(--ds-info-soft);
}

.wr-student-card--warning {
  border-color: var(--ds-warning-border);
}

.wr-student-card__avatar {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  font-weight: 700;
  flex-shrink: 0;
}

.wr-student-card--active .wr-student-card__avatar {
  background: var(--ds-success);
  color: white;
}

.wr-student-card__info {
  flex: 1;
  min-width: 0;
}

.wr-student-card__name {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.wr-student-card__email {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.125rem 0 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.wr-student-card__time {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.wr-student-card__status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.375rem;
}

.wr-status-badge {
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
  font-weight: 700;
}

.wr-status-badge--waiting {
  background: var(--ds-gray-200);
  color: var(--ds-text-secondary);
}

.wr-status-badge--active {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.wr-status-badge--submitted {
  background: var(--ds-info-soft);
  color: var(--ds-info);
}

.wr-status-badge--paused {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.wr-risk-indicator {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.7rem;
  font-weight: 600;
}

.wr-risk-indicator--high {
  color: var(--ds-danger);
}

.wr-risk-indicator--medium {
  color: var(--ds-warning);
}

.wr-risk-indicator--low {
  color: var(--ds-text-muted);
}

/* Action Cards */
.wr-panel--sidebar {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.wr-action-card {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  cursor: pointer;
  transition: border-color 0.15s ease, background-color 0.15s ease;
}

.wr-action-card:hover {
  border-color: var(--ds-primary-border);
  background: var(--ds-primary-soft);
}

.wr-action-card--primary {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  border-color: transparent;
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.wr-action-card--primary:hover {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  border-color: transparent;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.35);
}

.wr-action-card__icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-lg);
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.wr-action-card:not(.wr-action-card--primary) .wr-action-card__icon {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.wr-action-card__content {
  flex: 1;
}

.wr-action-card__content h3 {
  font-size: 0.875rem;
  font-weight: 700;
  margin: 0 0 0.125rem;
}

.wr-action-card__content p {
  font-size: 0.75rem;
  margin: 0;
  opacity: 0.8;
}

.wr-action-card:not(.wr-action-card--primary) .wr-action-card__content p {
  color: var(--ds-text-muted);
}

.wr-action-card__arrow {
  opacity: 0.6;
}

/* Schedule Card */
.wr-schedule-card {
  padding: 1rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
}

.wr-schedule-card__title {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
  margin: 0 0 0.75rem;
}

.wr-schedule-card__times {
  display: flex;
  gap: 1rem;
}

.wr-schedule-card__time {
  flex: 1;
}

.wr-schedule-card__label {
  display: block;
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin-bottom: 0.25rem;
}

.wr-schedule-card__value {
  display: block;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text);
}

/* Countdown Card */
.wr-countdown-card {
  padding: 1rem;
  background: var(--ds-warning-soft);
  border: 1px dashed var(--ds-warning-border);
  border-radius: var(--ds-radius-xl);
  text-align: center;
}

.wr-countdown-card__title {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-warning);
  margin: 0 0 0.5rem;
}

.wr-countdown-card__value {
  font-family: 'Space Grotesk', monospace;
  font-size: 1.75rem;
  font-weight: 900;
  color: var(--ds-warning);
}

/* Buttons */
.wr-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease;
  border: 1px solid var(--ds-border);
  background: transparent;
  color: var(--ds-text-secondary);
}

.wr-btn--ghost:hover {
  background: var(--ds-gray-100);
  color: var(--ds-text);
}

.wr-btn--sm {
  padding: 0.375rem 0.75rem;
  font-size: 0.8rem;
}

.wr-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.wr-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg) translateZ(0); }
}

/* Flash events overlay */
.wr-flash-container {
  position: fixed;
  top: 1.5rem;
  right: 1.5rem;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  pointer-events: none;
}

.wr-flash-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  background: var(--ds-primary);
  color: white;
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 600;
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.35);
  max-width: 320px;
}

.wr-flash-enter-active,
.wr-flash-leave-active {
  transition: color 0.3s ease, background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease, transform 0.3s ease;
}

.wr-flash-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.wr-flash-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

/* Responsive */
@media (max-width: 768px) {
  .wr-page {
    padding: 1rem;
  }

  .wr-header {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }

  .wr-header__left {
    flex-direction: column;
    align-items: flex-start;
  }

  .wr-header__stats {
    justify-content: space-around;
  }
}
/* ── prefers-reduced-motion ── */
@media (prefers-reduced-motion: reduce) {
  .wr-flash-enter-active,
  .wr-flash-leave-active {
    transition: none;
  }

  .wr-student-card:hover,
  .wr-action-card:hover,
  .wr-btn:hover {
    transform: none;
    transition: none;
  }
}
</style>
