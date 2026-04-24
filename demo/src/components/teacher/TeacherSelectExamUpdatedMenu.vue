<template>
  <div class="ems-root">
    <!-- Page Header -->
    <div class="ems-header">
      <div class="ems-header__left">
        <button class="ems-back" @click="$router.push('/teacher/dashboard')">
          <LucideIcon name="arrow-left" size="18" />
        </button>
        <div class="ems-header__titles">
          <h1 class="ems-header__title">Giám sát kỳ thi</h1>
          <p class="ems-header__sub">{{ liveExams.length }} kỳ thi đang diễn ra</p>
        </div>
      </div>
      <div class="ems-header__right">
        <button class="ems-refresh-btn" @click="loadExams" :class="{ 'ems-refresh-btn--spinning': isLoading }">
          <LucideIcon name="refresh-cw" size="16" />
          Làm mới
        </button>
      </div>
    </div>

    <!-- Stats Bar -->
    <div class="ems-stats">
      <div class="ems-stat ems-stat--live">
        <div class="ems-stat__icon">
          <LucideIcon name="radio" size="18" />
        </div>
        <div class="ems-stat__body">
          <span class="ems-stat__value">{{ liveExams.length }}</span>
          <span class="ems-stat__label">Đang diễn ra</span>
        </div>
      </div>
      <div class="ems-stat ems-stat--upcoming">
        <div class="ems-stat__icon">
          <LucideIcon name="clock" size="18" />
        </div>
        <div class="ems-stat__body">
          <span class="ems-stat__value">{{ upcomingExams.length }}</span>
          <span class="ems-stat__label">Sắp diễn ra</span>
        </div>
      </div>
      <div class="ems-stat ems-stat--ended">
        <div class="ems-stat__icon">
          <LucideIcon name="check-circle" size="18" />
        </div>
        <div class="ems-stat__body">
          <span class="ems-stat__value">{{ endedExams.length }}</span>
          <span class="ems-stat__label">Đã kết thúc</span>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="isLoading" class="ems-loading">
      <div class="ems-spinner" />
      <p>Đang tải danh sách kỳ thi...</p>
    </div>

    <!-- Error -->
    <div v-else-if="loadError" class="ems-error">
      <LucideIcon name="alert-circle" size="40" class="ems-error__icon" />
      <p class="ems-error__msg">{{ loadError }}</p>
      <button class="ems-error__retry" @click="loadExams">Thử lại</button>
    </div>

    <template v-else>
      <!-- SECTION: Đang diễn ra -->
      <div v-if="liveExams.length" class="ems-section">
        <div class="ems-section__header">
          <div class="ems-section__badge ems-section__badge--live">
            <span class="ems-section__live-dot" />
            ĐANG DIỄN RA
          </div>
          <h2 class="ems-section__title">Kỳ thi đang diễn ra</h2>
        </div>

        <div class="ems-table">
          <div class="ems-table__head">
            <div class="ems-table__th ems-table__th--name">Kỳ thi</div>
            <div class="ems-table__th ems-table__th--class">Lớp</div>
            <div class="ems-table__th ems-table__th--time">Thời gian còn lại</div>
            <div class="ems-table__th ems-table__th--participants">Thí sinh</div>
            <div class="ems-table__th ems-table__th--status">Trạng thái</div>
            <div class="ems-table__th ems-table__th--action"></div>
          </div>
          <div
            v-for="exam in liveExams"
            :key="exam.id"
            class="ems-table__row ems-table__row--live"
          >
            <div class="ems-table__td ems-table__td--name">
              <div class="ems-exam-name">
                <span class="ems-exam-name__title">{{ exam.title }}</span>
                <span v-if="exam.code" class="ems-exam-name__code">{{ exam.code }}</span>
              </div>
            </div>
            <div class="ems-table__td ems-table__td--class">
              <span class="ems-class-name">{{ exam.className || 'Trực tuyến' }}</span>
            </div>
            <div class="ems-table__td ems-table__td--time">
              <div class="ems-time-left">
                <LucideIcon name="timer" size="14" />
                <span class="ems-time-left__value">{{ formatRemaining(exam.remainingSeconds) }}</span>
              </div>
            </div>
            <div class="ems-table__td ems-table__td--participants">
              <div class="ems-participants">
                <div class="ems-participants__active">
                  <span class="ems-participants__num">{{ exam.currentSessionParticipants || 0 }}</span>
                  <span class="ems-participants__label">đang thi</span>
                </div>
                <span class="ems-participants__sep">/</span>
                <span class="ems-participants__total">{{ exam.participantCount || 0 }}</span>
              </div>
            </div>
            <div class="ems-table__td ems-table__td--status">
              <span class="ems-status-chip ems-status-chip--live">
                <span class="ems-status-chip__dot" />
                Đang thi
              </span>
            </div>
            <div class="ems-table__td ems-table__td--action">
              <button class="ems-action-btn ems-action-btn--primary" @click="goToMonitoring(exam)">
                <LucideIcon name="monitoring" size="15" />
                Giám sát
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- SECTION: Sắp diễn ra -->
      <div v-if="upcomingExams.length" class="ems-section">
        <div class="ems-section__header">
          <div class="ems-section__badge ems-section__badge--upcoming">
            <LucideIcon name="clock" size="12" />
            SẮP DIỄN RA
          </div>
          <h2 class="ems-section__title">Kỳ thi sắp bắt đầu</h2>
        </div>

        <div class="ems-table">
          <div class="ems-table__head">
            <div class="ems-table__th ems-table__th--name">Kỳ thi</div>
            <div class="ems-table__th ems-table__th--class">Lớp</div>
            <div class="ems-table__th ems-table__th--time">Giờ bắt đầu</div>
            <div class="ems-table__th ems-table__th--participants">Thí sinh</div>
            <div class="ems-table__th ems-table__th--status">Trạng thái</div>
            <div class="ems-table__th ems-table__th--action"></div>
          </div>
          <div
            v-for="exam in upcomingExams"
            :key="exam.id"
            class="ems-table__row"
          >
            <div class="ems-table__td ems-table__td--name">
              <div class="ems-exam-name">
                <span class="ems-exam-name__title">{{ exam.title }}</span>
                <span v-if="exam.code" class="ems-exam-name__code">{{ exam.code }}</span>
              </div>
            </div>
            <div class="ems-table__td ems-table__td--class">
              <span class="ems-class-name">{{ exam.className || 'Trực tuyến' }}</span>
            </div>
            <div class="ems-table__td ems-table__td--time">
              <div class="ems-time-left">
                <LucideIcon name="calendar" size="14" />
                <span class="ems-time-left__value">{{ formatTime(exam.startTime) }}</span>
                <span class="ems-time-left__date">{{ formatDate(exam.startTime) }}</span>
              </div>
            </div>
            <div class="ems-table__td ems-table__td--participants">
              <span class="ems-participants__total">{{ exam.participantCount || 0 }} thí sinh</span>
            </div>
            <div class="ems-table__td ems-table__td--status">
              <span class="ems-status-chip ems-status-chip--upcoming">
                <LucideIcon name="clock" size="10" />
                Chờ bắt đầu
              </span>
            </div>
            <div class="ems-table__td ems-table__td--action">
              <button class="ems-action-btn ems-action-btn--ghost" @click="goToMonitoring(exam)">
                <LucideIcon name="eye" size="15" />
                Xem trước
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- SECTION: Chưa có lịch -->
      <div v-if="noSessionExams.length" class="ems-section">
        <div class="ems-section__header">
          <div class="ems-section__badge ems-section__badge--muted">
            <LucideIcon name="calendar-off" size="12" />
            CHƯA CÓ LỊCH THI
          </div>
          <h2 class="ems-section__title">Kỳ thi chưa mở lịch</h2>
        </div>

        <div class="ems-table">
          <div class="ems-table__head">
            <div class="ems-table__th ems-table__th--name">Kỳ thi</div>
            <div class="ems-table__th ems-table__th--class">Lớp</div>
            <div class="ems-table__th ems-table__th--participants">Câu hỏi</div>
            <div class="ems-table__th ems-table__th--status">Trạng thái</div>
            <div class="ems-table__th ems-table__th--action"></div>
          </div>
          <div
            v-for="exam in noSessionExams"
            :key="exam.id"
            class="ems-table__row ems-table__row--muted"
          >
            <div class="ems-table__td ems-table__td--name">
              <div class="ems-exam-name">
                <span class="ems-exam-name__title">{{ exam.title }}</span>
                <span v-if="exam.code" class="ems-exam-name__code">{{ exam.code }}</span>
              </div>
            </div>
            <div class="ems-table__td ems-table__td--class">
              <span class="ems-class-name">{{ exam.className || 'Trực tuyến' }}</span>
            </div>
            <div class="ems-table__td ems-table__td--participants">
              <span class="ems-participants__total">{{ exam.questionCount || 0 }} câu</span>
            </div>
            <div class="ems-table__td ems-table__td--status">
              <span class="ems-status-chip ems-status-chip--muted">
                <LucideIcon name="calendar-plus" size="10" />
                Chưa mở lịch
              </span>
            </div>
            <div class="ems-table__td ems-table__td--action">
              <button class="ems-action-btn ems-action-btn--outline" @click="goToSchedule(exam)">
                <LucideIcon name="calendar-plus" size="15" />
                Mở lịch thi
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- SECTION: Đã kết thúc -->
      <div v-if="endedExams.length" class="ems-section">
        <div class="ems-section__header">
          <div class="ems-section__badge ems-section__badge--ended">
            <LucideIcon name="check-circle" size="12" />
            ĐÃ KẾT THÚC
          </div>
          <h2 class="ems-section__title">Kỳ thi đã kết thúc</h2>
        </div>

        <div class="ems-table">
          <div class="ems-table__head">
            <div class="ems-table__th ems-table__th--name">Kỳ thi</div>
            <div class="ems-table__th ems-table__th--class">Lớp</div>
            <div class="ems-table__th ems-table__th--time">Kết thúc lúc</div>
            <div class="ems-table__th ems-table__th--participants">Thí sinh</div>
            <div class="ems-table__th ems-table__th--status">Trạng thái</div>
            <div class="ems-table__th ems-table__th--action"></div>
          </div>
          <div
            v-for="exam in endedExams"
            :key="exam.id"
            class="ems-table__row ems-table__row--ended"
          >
            <div class="ems-table__td ems-table__td--name">
              <div class="ems-exam-name">
                <span class="ems-exam-name__title">{{ exam.title }}</span>
                <span v-if="exam.code" class="ems-exam-name__code">{{ exam.code }}</span>
              </div>
            </div>
            <div class="ems-table__td ems-table__td--class">
              <span class="ems-class-name">{{ exam.className || 'Trực tuyến' }}</span>
            </div>
            <div class="ems-table__td ems-table__td--time">
              <span class="ems-time-left__date">{{ formatDate(exam.endTime) }} {{ formatTime(exam.endTime) }}</span>
            </div>
            <div class="ems-table__td ems-table__td--participants">
              <span class="ems-participants__total">{{ exam.participantCount || 0 }} thí sinh</span>
            </div>
            <div class="ems-table__td ems-table__td--status">
              <span class="ems-status-chip ems-status-chip--ended">
                <LucideIcon name="check-circle" size="10" />
                Đã kết thúc
              </span>
            </div>
            <div class="ems-table__td ems-table__td--action">
              <button class="ems-action-btn ems-action-btn--ghost" @click="goToIncidents(exam)">
                <LucideIcon name="assessment" size="15" />
                Xem báo cáo
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="!allExams.length && !isLoading" class="ems-empty">
        <LucideIcon name="monitoring" size="56" class="ems-empty__icon" />
        <h3 class="ems-empty__title">Chưa có kỳ thi nào</h3>
        <p class="ems-empty__sub">Tạo kỳ thi mới hoặc mở lịch thi để bắt đầu giám sát.</p>
        <button class="ems-empty__cta" @click="$router.push('/teacher/exams/list')">
          <LucideIcon name="add" size="16" />
          Tạo đề thi
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listMonitoringExams } from '../../services/examService'
import { useToast } from '../../composables/useToast'

const router = useRouter()
const toast = useToast()
const isLoading = ref(false)
const loadError = ref('')
const allExams = ref([])

const liveExams = computed(() => allExams.value.filter(e => e.monitoringStatus === 'LIVE'))
const upcomingExams = computed(() => allExams.value.filter(e => e.monitoringStatus === 'UPCOMING'))
const noSessionExams = computed(() => allExams.value.filter(e => e.monitoringStatus === 'NO_SESSION'))
const endedExams = computed(() => allExams.value.filter(e => e.monitoringStatus === 'ENDED'))

function formatRemaining(seconds) {
  if (!seconds && seconds !== 0) return '—'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  return `${m}:${String(s).padStart(2, '0')}`
}

function formatTime(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
}

function goToMonitoring(exam) {
  router.push({
    path: '/teacher/live-monitoring/session',
    query: {
      examId: String(exam.id),
      title: exam.title || '',
      code: exam.code || ''
    }
  })
}

function goToSchedule(exam) {
  router.push({ path: '/teacher/exams/schedule', query: { examId: String(exam.id) } })
}

function goToIncidents(exam) {
  router.push({
    path: '/teacher/exams/review/incidents',
    query: { examId: String(exam.id), title: exam.title || '' }
  })
}

async function loadExams() {
  isLoading.value = true
  loadError.value = ''
  try {
    allExams.value = await listMonitoringExams()
  } catch (err) {
    loadError.value = err.message || 'Không thể tải danh sách kỳ thi.'
    toast.error(loadError.value)
  } finally {
    isLoading.value = false
  }
}

onMounted(loadExams)
</script>

<style scoped>
/* ── Root ─────────────────────────────────────────────────────────── */
.ems-root {
  min-height: 100vh;
  background: var(--ds-bg);
  padding: 1.5rem 2rem 3rem;
  max-width: 1200px;
  margin: 0 auto;
}

/* ── Header ───────────────────────────────────────────────────────── */
.ems-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}
.ems-header__left {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.ems-back {
  width: 38px;
  height: 38px;
  border-radius: var(--ds-radius);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s;
}
.ems-back:hover {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}
.ems-header__titles {}
.ems-header__title {
  font-size: 1.375rem;
  font-weight: 900;
  color: var(--ds-text);
  margin: 0 0 0.125rem;
  letter-spacing: -0.02em;
}
.ems-header__sub {
  font-size: 0.825rem;
  color: var(--ds-text-muted);
  margin: 0;
}
.ems-header__right {}
.ems-refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text);
  font-size: 0.825rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.ems-refresh-btn:hover {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
}
.ems-refresh-btn--spinning svg {
  animation: ems-spin 0.8s linear infinite;
}
@keyframes ems-spin { to { transform: rotate(360deg); } }

/* ── Stats Bar ─────────────────────────────────────────────────────── */
.ems-stats {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.75rem;
}
.ems-stat {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1.25rem;
  border-radius: var(--ds-radius-lg);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  flex: 1;
}
.ems-stat__icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.ems-stat--live .ems-stat__icon { background: rgba(22,163,74,0.1); color: var(--ds-success); }
.ems-stat--upcoming .ems-stat__icon { background: rgba(99,102,241,0.1); color: var(--ds-primary); }
.ems-stat--ended .ems-stat__icon { background: var(--ds-bg); color: var(--ds-text-muted); }
.ems-stat__body {
  display: flex;
  flex-direction: column;
}
.ems-stat__value {
  font-size: 1.5rem;
  font-weight: 900;
  color: var(--ds-text);
  line-height: 1;
  font-variant-numeric: tabular-nums;
}
.ems-stat__label {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin-top: 0.25rem;
}

/* ── Loading ──────────────────────────────────────────────────────── */
.ems-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  padding: 4rem 2rem;
  color: var(--ds-text-muted);
}
.ems-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--ds-border);
  border-top-color: var(--ds-primary);
  border-radius: 50%;
  animation: ems-spin 0.7s linear infinite;
}
.ems-loading p { font-size: 0.875rem; }

/* ── Error ────────────────────────────────────────────────────────── */
.ems-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 3rem 2rem;
  text-align: center;
}
.ems-error__icon { color: var(--ds-danger); opacity: 0.7; }
.ems-error__msg { font-size: 0.875rem; color: var(--ds-danger); font-weight: 600; margin: 0; }
.ems-error__retry {
  padding: 0.5rem 1.25rem;
  border-radius: var(--ds-radius);
  border: 1px solid var(--ds-danger);
  background: transparent;
  color: var(--ds-danger);
  font-size: 0.825rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
}
.ems-error__retry:hover { background: rgba(220,38,38,0.06); }

/* ── Sections ─────────────────────────────────────────────────────── */
.ems-section {
  margin-bottom: 2.5rem;
}
.ems-section__header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.875rem;
}
.ems-section__badge {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  font-size: 0.65rem;
  font-weight: 900;
  letter-spacing: 0.05em;
  border: 1px solid;
}
.ems-section__badge--live {
  background: rgba(22,163,74,0.1);
  color: var(--ds-success);
  border-color: rgba(22,163,74,0.25);
}
.ems-section__badge--upcoming {
  background: rgba(99,102,241,0.1);
  color: var(--ds-primary);
  border-color: rgba(99,102,241,0.25);
}
.ems-section__badge--muted {
  background: var(--ds-bg);
  color: var(--ds-text-muted);
  border-color: var(--ds-border);
}
.ems-section__badge--ended {
  background: var(--ds-bg);
  color: var(--ds-text-muted);
  border-color: var(--ds-border);
}
.ems-section__live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ds-success);
  animation: ems-pulse 1.5s ease-in-out infinite;
}
@keyframes ems-pulse { 0%,100%{opacity:1;transform:scale(1)} 50%{opacity:0.5;transform:scale(0.8)} }
.ems-section__title {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

/* ── Table ────────────────────────────────────────────────────────── */
.ems-table {
  border-radius: var(--ds-radius-lg);
  border: 1px solid var(--ds-border);
  overflow: hidden;
  background: var(--ds-surface);
  box-shadow: var(--ds-shadow-sm);
}
.ems-table__head {
  display: grid;
  grid-template-columns: 2fr 1fr 1.25fr 1fr 1fr 140px;
  gap: 0;
  padding: 0 1rem;
  background: var(--ds-bg);
  border-bottom: 1px solid var(--ds-border);
}
.ems-table__th {
  padding: 0.625rem 0.75rem;
  font-size: 0.7rem;
  font-weight: 800;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.ems-table__th--action { text-align: right; }

.ems-table__row {
  display: grid;
  grid-template-columns: 2fr 1fr 1.25fr 1fr 1fr 140px;
  gap: 0;
  padding: 0 1rem;
  border-bottom: 1px solid var(--ds-border);
  transition: background 0.12s;
  align-items: center;
}
.ems-table__row:last-child { border-bottom: none; }
.ems-table__row:hover { background: var(--ds-bg); }
.ems-table__row--live {
  background: rgba(22,163,74,0.02);
}
.ems-table__row--live:hover { background: rgba(22,163,74,0.05); }
.ems-table__row--muted { opacity: 0.75; }
.ems-table__row--ended { opacity: 0.8; }

.ems-table__td {
  padding: 0.875rem 0.75rem;
  font-size: 0.825rem;
}
.ems-table__td--action { text-align: right; }

/* Exam name cell */
.ems-exam-name {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}
.ems-exam-name__title {
  font-weight: 700;
  color: var(--ds-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 280px;
}
.ems-exam-name__code {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  font-family: monospace;
}

/* Class cell */
.ems-class-name {
  color: var(--ds-text-muted);
  font-size: 0.8rem;
}

/* Time left cell */
.ems-time-left {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  color: var(--ds-text-muted);
}
.ems-time-left__value {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--ds-text);
  font-size: 0.85rem;
}
.ems-time-left__date {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
}

/* Participants cell */
.ems-participants {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.8rem;
}
.ems-participants__active {
  display: flex;
  align-items: baseline;
  gap: 0.25rem;
}
.ems-participants__num {
  font-weight: 800;
  color: var(--ds-success);
  font-size: 0.9rem;
}
.ems-participants__label {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
}
.ems-participants__sep {
  color: var(--ds-border);
  font-size: 0.8rem;
}
.ems-participants__total {
  color: var(--ds-text-muted);
}

/* Status chip */
.ems-status-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.25rem 0.6rem;
  border-radius: 9999px;
  font-size: 0.7rem;
  font-weight: 700;
  border: 1px solid;
  width: fit-content;
}
.ems-status-chip--live {
  background: rgba(22,163,74,0.08);
  color: var(--ds-success);
  border-color: rgba(22,163,74,0.2);
}
.ems-status-chip__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ds-success);
}
.ems-status-chip--upcoming {
  background: rgba(99,102,241,0.08);
  color: var(--ds-primary);
  border-color: rgba(99,102,241,0.2);
}
.ems-status-chip--muted {
  background: var(--ds-bg);
  color: var(--ds-text-muted);
  border-color: var(--ds-border);
}
.ems-status-chip--ended {
  background: var(--ds-bg);
  color: var(--ds-text-muted);
  border-color: var(--ds-border);
}

/* Action buttons */
.ems-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.45rem 0.875rem;
  border-radius: var(--ds-radius);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
  border: 1px solid;
}
.ems-action-btn--primary {
  background: var(--ds-primary);
  color: white;
  border-color: var(--ds-primary);
}
.ems-action-btn--primary:hover {
  filter: brightness(1.08);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99,102,241,0.3);
}
.ems-action-btn--ghost {
  background: transparent;
  color: var(--ds-text-muted);
  border-color: var(--ds-border);
}
.ems-action-btn--ghost:hover {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}
.ems-action-btn--outline {
  background: transparent;
  color: var(--ds-text);
  border-color: var(--ds-border);
}
.ems-action-btn--outline:hover {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

/* ── Empty ────────────────────────────────────────────────────────── */
.ems-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 5rem 2rem;
  text-align: center;
}
.ems-empty__icon { color: var(--ds-text-muted); opacity: 0.3; }
.ems-empty__title {
  font-size: 1.1rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}
.ems-empty__sub {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  margin: 0;
  max-width: 320px;
}
.ems-empty__cta {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
  padding: 0.625rem 1.25rem;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary);
  color: white;
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
}
.ems-empty__cta:hover { filter: brightness(1.08); }
</style>
