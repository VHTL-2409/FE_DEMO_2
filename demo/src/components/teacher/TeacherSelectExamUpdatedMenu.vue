<template>
  <div class="ems-root">

    <!-- ── Page Header ──────────────────────────────────────────── -->
    <header class="ems-header">
      <div class="ems-header__left">
        <button class="ems-back-btn" @click="$router.push('/teacher/dashboard')" aria-label="Quay lại dashboard">
          <LucideIcon name="arrow-left" :size="18" />
        </button>
        <div class="ems-header__titles">
          <h1 class="ems-header__title">Command Center</h1>
          <p class="ems-header__sub">Giám sát kỳ thi trực tiếp</p>
        </div>
      </div>
      <div class="ems-header__right">
        <div class="ems-live-pill">
          <span class="ems-live-pill__dot" />
          <span>{{ liveExams.length }} đang diễn ra</span>
        </div>
        <button class="ems-refresh-btn" @click="loadExams" :class="{ 'ems-refresh-btn--spinning': isLoading }">
          <LucideIcon name="refresh-cw" :size="15" />
          <span>Làm mới</span>
        </button>
      </div>
    </header>

    <!-- ── Stats Overview ───────────────────────────────────────── -->
    <div class="ems-overview">
      <!-- Live exam mini-cards -->
      <div class="ems-overview__main">
        <div class="ems-overview__label">
          <LucideIcon name="radio" :size="14" />
          Kỳ thi đang diễn ra
        </div>
        <div class="ems-live-grid">
          <div
            v-for="exam in liveExams.slice(0, 3)"
            :key="exam.id"
            class="ems-live-card ems-live-card--live"
            @click="goToMonitoring(exam)"
          >
            <div class="ems-live-card__header">
              <span class="ems-live-card__dot" />
              <span class="ems-live-card__status">LIVE</span>
              <span class="ems-live-card__time">
                <LucideIcon name="clock" :size="11" />
                {{ formatRemaining(exam.remainingSeconds) }}
              </span>
            </div>
            <h3 class="ems-live-card__title">{{ exam.title }}</h3>
            <div class="ems-live-card__meta">
              <span class="ems-live-card__meta-item">
                <LucideIcon name="users" :size="12" />
                {{ exam.currentSessionParticipants || 0 }}/{{ exam.participantCount || 0 }}
              </span>
              <span class="ems-live-card__meta-item">
                <LucideIcon name="alert-triangle" :size="12" />
                {{ getExamAlertCount(exam.id) }}
              </span>
            </div>
            <div class="ems-live-card__progress">
              <div
                class="ems-live-card__progress-fill"
                :style="{ width: getProgressWidth(exam) + '%' }"
              />
            </div>
          </div>

          <!-- Quick-add card if no live exams -->
          <div v-if="liveExams.length === 0" class="ems-live-card ems-live-card--empty">
            <LucideIcon name="monitoring" :size="32" class="ems-live-card__empty-icon" />
            <p>Không có kỳ thi nào đang diễn ra</p>
            <button class="ems-live-card__empty-btn" @click="$router.push('/teacher/exams/list')">
              <LucideIcon name="plus" :size="14" />
              Tạo kỳ thi mới
            </button>
          </div>
        </div>
      </div>

      <!-- Side stat cards -->
      <div class="ems-overview__stats">
        <div class="ems-stat-card ems-stat-card--live">
          <div class="ems-stat-card__icon">
            <LucideIcon name="radio" :size="20" />
          </div>
          <div class="ems-stat-card__body">
            <span class="ems-stat-card__value">{{ liveExams.length }}</span>
            <span class="ems-stat-card__label">Đang diễn ra</span>
          </div>
          <div class="ems-stat-card__glow" />
        </div>
        <div class="ems-stat-card ems-stat-card--upcoming">
          <div class="ems-stat-card__icon">
            <LucideIcon name="clock" :size="20" />
          </div>
          <div class="ems-stat-card__body">
            <span class="ems-stat-card__value">{{ upcomingExams.length }}</span>
            <span class="ems-stat-card__label">Sắp diễn ra</span>
          </div>
          <div class="ems-stat-card__glow" />
        </div>
        <div class="ems-stat-card ems-stat-card--ended">
          <div class="ems-stat-card__icon">
            <LucideIcon name="check-circle" :size="20" />
          </div>
          <div class="ems-stat-card__body">
            <span class="ems-stat-card__value">{{ endedExams.length }}</span>
            <span class="ems-stat-card__label">Đã kết thúc</span>
          </div>
          <div class="ems-stat-card__glow" />
        </div>
      </div>
    </div>

    <!-- ── Main Content ─────────────────────────────────────────── -->
    <div class="ems-content">

      <!-- Loading -->
      <div v-if="isLoading && !allExams.length" class="ems-loading">
        <div class="ems-loading__grid">
          <div v-for="i in 6" :key="i" class="ems-exam-skeleton" />
        </div>
      </div>

      <!-- Error -->
      <div v-else-if="loadError && !allExams.length" class="ems-error-state">
        <div class="ems-error-state__card">
          <LucideIcon name="alert-circle" :size="36" class="ems-error-state__icon" />
          <h3 class="ems-error-state__title">Không thể tải dữ liệu</h3>
          <p class="ems-error-state__msg">{{ loadError }}</p>
          <button class="ems-error-state__retry" @click="loadExams">
            <LucideIcon name="refresh-cw" :size="14" />
            Thử lại
          </button>
        </div>
      </div>

      <template v-else>

        <!-- ── Section: Đang diễn ra ── -->
        <section v-if="liveExams.length" class="ems-section">
          <div class="ems-section__head">
            <div class="ems-section__badge ems-section__badge--live">
              <span class="ems-section__live-dot" />
              LIVE
            </div>
            <h2 class="ems-section__title">Kỳ thi đang diễn ra</h2>
            <span class="ems-section__count">{{ liveExams.length }} kỳ thi</span>
          </div>

          <div class="ems-exam-grid">
            <div
              v-for="exam in liveExams"
              :key="exam.id"
              class="ems-exam-card"
              :class="{ 'ems-exam-card--hovered': hoveredExam === exam.id }"
              @mouseenter="hoveredExam = exam.id"
              @mouseleave="hoveredExam = null"
            >
              <div class="ems-exam-card__accent ems-exam-card__accent--live" />

              <div class="ems-exam-card__top">
                <div class="ems-exam-card__badges">
                  <span class="ems-badge ems-badge--live">
                    <span class="ems-badge__dot" />
                    Đang thi
                  </span>
                  <span v-if="getExamAlertCount(exam.id) > 0" class="ems-badge ems-badge--warn">
                    <LucideIcon name="alert-triangle" :size="10" />
                    {{ getExamAlertCount(exam.id) }} cảnh báo
                  </span>
                </div>
                <span class="ems-exam-card__code">{{ exam.code || '—' }}</span>
              </div>

              <h3 class="ems-exam-card__title">{{ exam.title }}</h3>
              <p class="ems-exam-card__class">
                <LucideIcon name="book-open" :size="12" />
                {{ exam.className || 'Trực tuyến' }}
              </p>

              <div class="ems-exam-card__stats">
                <div class="ems-exam-card__stat">
                  <LucideIcon name="users" :size="13" />
                  <span class="ems-exam-card__stat-value">{{ exam.currentSessionParticipants || 0 }}</span>
                  <span class="ems-exam-card__stat-sep">/</span>
                  <span class="ems-exam-card__stat-total">{{ exam.participantCount || 0 }}</span>
                  <span class="ems-exam-card__stat-label">đang thi</span>
                </div>
                <div class="ems-exam-card__stat ems-exam-card__stat--time">
                  <LucideIcon name="clock" :size="13" />
                  <span class="ems-exam-card__stat-value ems-exam-card__stat-value--live">
                    {{ formatRemaining(exam.remainingSeconds) }}
                  </span>
                  <span class="ems-exam-card__stat-label">còn lại</span>
                </div>
              </div>

              <div class="ems-exam-card__progress">
                <div class="ems-exam-card__progress-track">
                  <div
                    class="ems-exam-card__progress-fill ems-exam-card__progress-fill--live"
                    :style="{ width: getProgressWidth(exam) + '%' }"
                  />
                </div>
                <span class="ems-exam-card__progress-label">{{ getProgressWidth(exam) }}% tiến độ</span>
              </div>

              <div class="ems-exam-card__actions">
                <button class="ems-btn ems-btn--primary" @click="goToMonitoring(exam)">
                  <LucideIcon name="monitoring" :size="14" />
                  Giám sát
                </button>
                <button class="ems-btn ems-btn--ghost" @click="goToIncidents(exam)">
                  <LucideIcon name="file-text" :size="14" />
                  Báo cáo
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- ── Section: Sắp diễn ra ── -->
        <section v-if="upcomingExams.length" class="ems-section">
          <div class="ems-section__head">
            <div class="ems-section__badge ems-section__badge--upcoming">
              <LucideIcon name="clock" :size="12" />
              SẮP
            </div>
            <h2 class="ems-section__title">Kỳ thi sắp bắt đầu</h2>
            <span class="ems-section__count">{{ upcomingExams.length }} kỳ thi</span>
          </div>

          <div class="ems-exam-table">
            <div class="ems-exam-table__head">
              <div class="ems-exam-table__th">Kỳ thi</div>
              <div class="ems-exam-table__th">Lớp</div>
              <div class="ems-exam-table__th">Giờ bắt đầu</div>
              <div class="ems-exam-table__th">Thí sinh</div>
              <div class="ems-exam-table__th">Trạng thái</div>
              <div class="ems-exam-table__th ems-exam-table__th--action" />
            </div>
            <div
              v-for="exam in upcomingExams"
              :key="exam.id"
              class="ems-exam-table__row"
            >
              <div class="ems-exam-table__td">
                <div class="ems-exam-name-cell">
                  <span class="ems-exam-name-cell__title">{{ exam.title }}</span>
                  <span class="ems-exam-name-cell__code">{{ exam.code || '—' }}</span>
                </div>
              </div>
              <div class="ems-exam-table__td">
                <span class="ems-text-muted">{{ exam.className || 'Trực tuyến' }}</span>
              </div>
              <div class="ems-exam-table__td">
                <div class="ems-time-cell">
                  <LucideIcon name="calendar" :size="12" />
                  <span class="ems-time-cell__date">{{ formatDate(exam.startTime) }}</span>
                  <span class="ems-time-cell__time">{{ formatTime(exam.startTime) }}</span>
                </div>
              </div>
              <div class="ems-exam-table__td">
                <span class="ems-text-muted">{{ exam.participantCount || 0 }} thí sinh</span>
              </div>
              <div class="ems-exam-table__td">
                <span class="ems-badge ems-badge--upcoming">
                  <LucideIcon name="clock" :size="10" />
                  Chờ bắt đầu
                </span>
              </div>
              <div class="ems-exam-table__td ems-exam-table__td--action">
                <button class="ems-btn ems-btn--ghost-sm" @click="goToMonitoring(exam)">
                  <LucideIcon name="eye" :size="13" />
                  Xem trước
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- ── Section: Chưa mở lịch ── -->
        <section v-if="noSessionExams.length" class="ems-section">
          <div class="ems-section__head">
            <div class="ems-section__badge ems-section__badge--muted">
              <LucideIcon name="calendar-off" :size="12" />
              CHƯA CÓ LỊCH
            </div>
            <h2 class="ems-section__title">Kỳ thi chưa mở lịch</h2>
            <span class="ems-section__count">{{ noSessionExams.length }} kỳ thi</span>
          </div>

          <div class="ems-exam-table">
            <div class="ems-exam-table__head">
              <div class="ems-exam-table__th">Kỳ thi</div>
              <div class="ems-exam-table__th">Lớp</div>
              <div class="ems-exam-table__th">Câu hỏi</div>
              <div class="ems-exam-table__th">Trạng thái</div>
              <div class="ems-exam-table__th ems-exam-table__th--action" />
            </div>
            <div
              v-for="exam in noSessionExams"
              :key="exam.id"
              class="ems-exam-table__row ems-exam-table__row--muted"
            >
              <div class="ems-exam-table__td">
                <div class="ems-exam-name-cell">
                  <span class="ems-exam-name-cell__title">{{ exam.title }}</span>
                  <span class="ems-exam-name-cell__code">{{ exam.code || '—' }}</span>
                </div>
              </div>
              <div class="ems-exam-table__td">
                <span class="ems-text-muted">{{ exam.className || 'Trực tuyến' }}</span>
              </div>
              <div class="ems-exam-table__td">
                <span class="ems-text-muted">{{ exam.questionCount || 0 }} câu</span>
              </div>
              <div class="ems-exam-table__td">
                <span class="ems-badge ems-badge--muted">
                  <LucideIcon name="calendar-plus" :size="10" />
                  Chưa mở lịch
                </span>
              </div>
              <div class="ems-exam-table__td ems-exam-table__td--action">
                <button class="ems-btn ems-btn--ghost-sm" @click="goToSchedule(exam)">
                  <LucideIcon name="calendar-plus" :size="13" />
                  Mở lịch thi
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- ── Section: Đã kết thúc ── -->
        <section v-if="endedExams.length" class="ems-section">
          <div class="ems-section__head">
            <div class="ems-section__badge ems-section__badge--ended">
              <LucideIcon name="check-circle" :size="12" />
              KẾT THÚC
            </div>
            <h2 class="ems-section__title">Kỳ thi đã kết thúc</h2>
            <span class="ems-section__count">{{ endedExams.length }} kỳ thi</span>
          </div>

          <div class="ems-exam-table">
            <div class="ems-exam-table__head">
              <div class="ems-exam-table__th">Kỳ thi</div>
              <div class="ems-exam-table__th">Lớp</div>
              <div class="ems-exam-table__th">Kết thúc lúc</div>
              <div class="ems-exam-table__th">Thí sinh</div>
              <div class="ems-exam-table__th">Trạng thái</div>
              <div class="ems-exam-table__th ems-exam-table__th--action" />
            </div>
            <div
              v-for="exam in endedExams"
              :key="exam.id"
              class="ems-exam-table__row"
            >
              <div class="ems-exam-table__td">
                <div class="ems-exam-name-cell">
                  <span class="ems-exam-name-cell__title">{{ exam.title }}</span>
                  <span class="ems-exam-name-cell__code">{{ exam.code || '—' }}</span>
                </div>
              </div>
              <div class="ems-exam-table__td">
                <span class="ems-text-muted">{{ exam.className || 'Trực tuyến' }}</span>
              </div>
              <div class="ems-exam-table__td">
                <div class="ems-time-cell">
                  <LucideIcon name="calendar-check" :size="12" />
                  <span class="ems-time-cell__date">{{ formatDate(exam.endTime) }}</span>
                  <span class="ems-time-cell__time">{{ formatTime(exam.endTime) }}</span>
                </div>
              </div>
              <div class="ems-exam-table__td">
                <span class="ems-text-muted">{{ exam.participantCount || 0 }} thí sinh</span>
              </div>
              <div class="ems-exam-table__td">
                <span class="ems-badge ems-badge--ended">
                  <LucideIcon name="check-circle" :size="10" />
                  Đã kết thúc
                </span>
              </div>
              <div class="ems-exam-table__td ems-exam-table__td--action">
                <button class="ems-btn ems-btn--ghost-sm" @click="goToIncidents(exam)">
                  <LucideIcon name="assessment" :size="13" />
                  Xem báo cáo
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- ── Empty State ── -->
        <div v-if="!allExams.length" class="ems-empty">
          <div class="ems-empty__illustration">
            <svg width="120" height="120" viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="60" cy="60" r="56" fill="var(--ds-primary-soft)" />
              <rect x="30" y="40" width="60" height="45" rx="6" fill="var(--ds-primary)" opacity="0.15" />
              <rect x="35" y="45" width="50" height="35" rx="4" fill="var(--ds-primary)" opacity="0.3" />
              <circle cx="60" cy="62" r="12" fill="var(--ds-primary)" opacity="0.6" />
              <circle cx="60" cy="62" r="6" fill="var(--ds-primary)" />
              <rect x="50" y="25" width="20" height="20" rx="4" fill="var(--ds-primary)" opacity="0.4" />
            </svg>
          </div>
          <h3 class="ems-empty__title">Chưa có kỳ thi nào</h3>
          <p class="ems-empty__sub">Tạo kỳ thi mới hoặc mở lịch thi để bắt đầu giám sát.</p>
          <button class="ems-btn ems-btn--primary" @click="$router.push('/teacher/exams/list')">
            <LucideIcon name="plus" :size="15" />
            Tạo đề thi
          </button>
        </div>

      </template>
    </div>
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
const hoveredExam = ref(null)

// Fake alert counts (trong thực tế sẽ lấy từ store/API)
function getExamAlertCount(examId) {
  return Math.floor(Math.random() * 5)
}

function getProgressWidth(exam) {
  if (!exam.remainingSeconds || !exam.totalDurationSeconds) return 0
  return Math.round(((exam.totalDurationSeconds - exam.remainingSeconds) / exam.totalDurationSeconds) * 100)
}

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
  padding-bottom: 3rem;
}

/* ── Header ─────────────────────────────────────────────────────── */
.ems-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 2rem;
  background: var(--ds-surface);
  border-bottom: 1px solid var(--ds-border);
  gap: 1rem;
  position: sticky;
  top: 0;
  z-index: 50;
  backdrop-filter: blur(8px);
}
.ems-header__left {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.ems-back-btn {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-md);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s;
  flex-shrink: 0;
}
.ems-back-btn:hover {
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
.ems-header__right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.ems-live-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.875rem;
  border-radius: 9999px;
  background: rgba(22, 163, 74, 0.1);
  border: 1px solid rgba(22, 163, 74, 0.2);
  color: var(--ds-success);
  font-size: 0.8rem;
  font-weight: 700;
}
.ems-live-pill__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--ds-success);
  animation: ems-pulse 1.5s ease-in-out infinite;
}

.ems-refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-md);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.825rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.ems-refresh-btn:hover {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}
.ems-refresh-btn--spinning svg {
  animation: ems-spin 0.8s linear infinite;
}
@keyframes ems-spin { to { transform: rotate(360deg); } }
@keyframes ems-pulse { 0%,100%{opacity:1;transform:scale(1)} 50%{opacity:0.4;transform:scale(0.75)} }

/* ── Overview ──────────────────────────────────────────────────── */
.ems-overview {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 1.5rem;
  padding: 1.5rem 2rem 0;
}

.ems-overview__label {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.7rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--ds-text-muted);
  margin-bottom: 0.875rem;
}

.ems-live-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.875rem;
}

.ems-live-card {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  padding: 1rem;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  overflow: hidden;
}
.ems-live-card:hover {
  border-color: var(--ds-primary-border);
  box-shadow: var(--ds-shadow-md);
  transform: translateY(-2px);
}
.ems-live-card--live {
  border-top: 2px solid var(--ds-success);
}
.ems-live-card--empty {
  border: 2px dashed var(--ds-border);
  background: var(--ds-surface-muted);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  min-height: 120px;
  color: var(--ds-text-muted);
  font-size: 0.8rem;
  text-align: center;
  cursor: default;
}
.ems-live-card__empty-icon { opacity: 0.3; }
.ems-live-card__empty-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-md);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border: 1px solid var(--ds-primary-border);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
}
.ems-live-card__empty-btn:hover { background: var(--ds-primary); color: #fff; }

.ems-live-card__header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.625rem;
}
.ems-live-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--ds-success);
  animation: ems-pulse 1.5s ease-in-out infinite;
}
.ems-live-card__status {
  font-size: 0.6rem;
  font-weight: 900;
  letter-spacing: 0.1em;
  color: var(--ds-success);
  background: rgba(22,163,74,0.1);
  padding: 0.1rem 0.4rem;
  border-radius: 9999px;
  border: 1px solid rgba(22,163,74,0.2);
}
.ems-live-card__time {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  margin-left: auto;
  font-variant-numeric: tabular-nums;
}
.ems-live-card__title {
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0 0 0.5rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ems-live-card__meta {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}
.ems-live-card__meta-item {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.72rem;
  color: var(--ds-text-muted);
  font-weight: 600;
}
.ems-live-card__progress {
  position: relative;
}
.ems-live-card__progress-track {
  height: 4px;
  border-radius: 9999px;
  background: var(--ds-gray-100);
  overflow: hidden;
}
.ems-live-card__progress-fill {
  height: 100%;
  border-radius: 9999px;
  background: linear-gradient(90deg, var(--ds-success), rgba(22,163,74,0.6));
  transition: width 0.6s ease;
}

.ems-overview__stats {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

.ems-stat-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.125rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  position: relative;
  overflow: hidden;
  box-shadow: var(--ds-shadow-xs);
  transition: all 0.15s;
}
.ems-stat-card:hover {
  box-shadow: var(--ds-shadow-sm);
  transform: translateY(-1px);
}
.ems-stat-card__icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.ems-stat-card--live .ems-stat-card__icon { background: rgba(22,163,74,0.12); color: var(--ds-success); }
.ems-stat-card--upcoming .ems-stat-card__icon { background: var(--ds-primary-soft); color: var(--ds-primary); }
.ems-stat-card--ended .ems-stat-card__icon { background: var(--ds-surface-muted); color: var(--ds-text-muted); }
.ems-stat-card__body {
  display: flex;
  flex-direction: column;
}
.ems-stat-card__value {
  font-size: 1.75rem;
  font-weight: 900;
  color: var(--ds-text);
  line-height: 1;
  font-variant-numeric: tabular-nums;
}
.ems-stat-card__label {
  font-size: 0.72rem;
  color: var(--ds-text-muted);
  margin-top: 0.25rem;
  font-weight: 600;
}
.ems-stat-card__glow {
  position: absolute;
  right: -20px;
  top: 50%;
  transform: translateY(-50%);
  width: 80px;
  height: 80px;
  border-radius: 50%;
  opacity: 0.06;
  pointer-events: none;
}
.ems-stat-card--live .ems-stat-card__glow { background: var(--ds-success); }
.ems-stat-card--upcoming .ems-stat-card__glow { background: var(--ds-primary); }
.ems-stat-card--ended .ems-stat-card__glow { background: var(--ds-text-muted); }

/* ── Content ───────────────────────────────────────────────────── */
.ems-content {
  padding: 1.5rem 2rem 0;
  max-width: 1400px;
}

/* ── Section ──────────────────────────────────────────────────── */
.ems-section {
  margin-bottom: 2.5rem;
}
.ems-section__head {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}
.ems-section__badge {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.2rem 0.625rem;
  border-radius: 9999px;
  font-size: 0.62rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  border: 1px solid;
}
.ems-section__badge--live {
  background: rgba(22,163,74,0.1);
  color: var(--ds-success);
  border-color: rgba(22,163,74,0.25);
}
.ems-section__badge--upcoming {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border-color: var(--ds-primary-border);
}
.ems-section__badge--muted {
  background: var(--ds-surface-muted);
  color: var(--ds-text-muted);
  border-color: var(--ds-border);
}
.ems-section__badge--ended {
  background: var(--ds-surface-muted);
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
.ems-section__title {
  font-size: 0.925rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}
.ems-section__count {
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  background: var(--ds-surface-muted);
  padding: 0.2rem 0.5rem;
  border-radius: 9999px;
  border: 1px solid var(--ds-border);
}

/* ── Exam Grid (Live) ───────────────────────────────────────────── */
.ems-exam-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}

.ems-exam-card {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  padding: 1.25rem;
  position: relative;
  overflow: hidden;
  box-shadow: var(--ds-shadow-xs);
  transition: all 0.2s;
}
.ems-exam-card:hover {
  border-color: var(--ds-primary-border);
  box-shadow: var(--ds-shadow-md);
  transform: translateY(-2px);
}
.ems-exam-card__accent {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  border-radius: var(--ds-radius-xl) var(--ds-radius-xl) 0 0;
}
.ems-exam-card__accent--live { background: linear-gradient(90deg, var(--ds-success), rgba(22,163,74,0.4)); }

.ems-exam-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}
.ems-exam-card__badges {
  display: flex;
  gap: 0.375rem;
  flex-wrap: wrap;
}
.ems-exam-card__code {
  font-size: 0.68rem;
  font-family: monospace;
  color: var(--ds-text-muted);
  font-weight: 600;
  background: var(--ds-surface-muted);
  padding: 0.1rem 0.4rem;
  border-radius: var(--ds-radius-sm);
  border: 1px solid var(--ds-border);
}
.ems-exam-card__title {
  font-size: 1rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0 0 0.375rem;
  line-height: 1.3;
}
.ems-exam-card__class {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0 0 1rem;
  font-weight: 600;
}
.ems-exam-card__stats {
  display: flex;
  gap: 1.25rem;
  margin-bottom: 0.875rem;
}
.ems-exam-card__stat {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.78rem;
  color: var(--ds-text-muted);
}
.ems-exam-card__stat-value {
  font-weight: 800;
  color: var(--ds-text);
}
.ems-exam-card__stat-value--live { color: var(--ds-success); }
.ems-exam-card__stat-sep { color: var(--ds-border); }
.ems-exam-card__stat-total { color: var(--ds-text-muted); }
.ems-exam-card__stat-label { font-size: 0.7rem; font-weight: 600; }
.ems-exam-card__progress {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  margin-bottom: 1rem;
}
.ems-exam-card__progress-track {
  flex: 1;
  height: 5px;
  border-radius: 9999px;
  background: var(--ds-gray-100);
  overflow: hidden;
}
.ems-exam-card__progress-fill {
  height: 100%;
  border-radius: 9999px;
  transition: width 0.6s ease;
}
.ems-exam-card__progress-fill--live { background: linear-gradient(90deg, var(--ds-success), rgba(22,163,74,0.6)); }
.ems-exam-card__progress-label {
  font-size: 0.68rem;
  font-weight: 700;
  color: var(--ds-success);
  white-space: nowrap;
}
.ems-exam-card__actions {
  display: flex;
  gap: 0.5rem;
}

/* ── Exam Table ────────────────────────────────────────────────── */
.ems-exam-table {
  border-radius: var(--ds-radius-xl);
  border: 1px solid var(--ds-border);
  overflow: hidden;
  background: var(--ds-surface);
  box-shadow: var(--ds-shadow-xs);
}
.ems-exam-table__head {
  display: grid;
  grid-template-columns: 2.5fr 1.2fr 1.5fr 1fr 1.2fr 120px;
  gap: 0;
  padding: 0 1.25rem;
  background: var(--ds-surface-muted);
  border-bottom: 1px solid var(--ds-border);
}
.ems-exam-table__th {
  padding: 0.75rem 0.5rem;
  font-size: 0.68rem;
  font-weight: 800;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.ems-exam-table__th--action { text-align: right; }
.ems-exam-table__row {
  display: grid;
  grid-template-columns: 2.5fr 1.2fr 1.5fr 1fr 1.2fr 120px;
  gap: 0;
  padding: 0 1.25rem;
  border-bottom: 1px solid var(--ds-border);
  transition: background 0.12s;
  align-items: center;
}
.ems-exam-table__row:last-child { border-bottom: none; }
.ems-exam-table__row:hover { background: var(--ds-surface-muted); }
.ems-exam-table__row--muted { opacity: 0.75; }
.ems-exam-table__td {
  padding: 0.875rem 0.5rem;
  font-size: 0.825rem;
}
.ems-exam-table__td--action { text-align: right; }

.ems-exam-name-cell {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}
.ems-exam-name-cell__title {
  font-weight: 700;
  color: var(--ds-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 300px;
}
.ems-exam-name-cell__code {
  font-size: 0.68rem;
  color: var(--ds-text-muted);
  font-family: monospace;
}

.ems-time-cell {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  color: var(--ds-text-muted);
}
.ems-time-cell__date {
  font-size: 0.78rem;
  color: var(--ds-text);
  font-weight: 600;
}
.ems-time-cell__time {
  font-size: 0.72rem;
  font-variant-numeric: tabular-nums;
}

.ems-text-muted {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  font-weight: 600;
}

/* ── Badges ────────────────────────────────────────────────────── */
.ems-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.2rem 0.6rem;
  border-radius: 9999px;
  font-size: 0.68rem;
  font-weight: 700;
  border: 1px solid;
  width: fit-content;
}
.ems-badge--live {
  background: rgba(22,163,74,0.1);
  color: var(--ds-success);
  border-color: rgba(22,163,74,0.2);
}
.ems-badge__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ds-success);
  animation: ems-pulse 1.5s ease-in-out infinite;
}
.ems-badge--warn {
  background: rgba(245,158,11,0.1);
  color: var(--ds-warning);
  border-color: rgba(245,158,11,0.2);
}
.ems-badge--upcoming {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border-color: var(--ds-primary-border);
}
.ems-badge--muted {
  background: var(--ds-surface-muted);
  color: var(--ds-text-muted);
  border-color: var(--ds-border);
}
.ems-badge--ended {
  background: var(--ds-surface-muted);
  color: var(--ds-text-muted);
  border-color: var(--ds-border);
}

/* ── Buttons ───────────────────────────────────────────────────── */
.ems-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-md);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
  border: 1px solid transparent;
  white-space: nowrap;
}
.ems-btn--primary {
  background: var(--ds-primary);
  color: white;
  border-color: var(--ds-primary);
  box-shadow: 0 2px 8px rgba(79,70,229,0.2);
}
.ems-btn--primary:hover {
  background: var(--ds-primary-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79,70,229,0.3);
}
.ems-btn--ghost {
  background: transparent;
  color: var(--ds-text-secondary);
  border-color: var(--ds-border);
}
.ems-btn--ghost:hover {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}
.ems-btn--ghost-sm {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem 0.65rem;
  border-radius: var(--ds-radius-md);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
  background: transparent;
  color: var(--ds-text-secondary);
  border: 1px solid var(--ds-border);
}
.ems-btn--ghost-sm:hover {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

/* ── Loading Skeleton ───────────────────────────────────────────── */
.ems-loading {}
.ems-loading__grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
}
.ems-exam-skeleton {
  height: 200px;
  border-radius: var(--ds-radius-xl);
  background: linear-gradient(90deg, var(--ds-surface-muted) 0%, var(--ds-border) 50%, var(--ds-surface-muted) 100%);
  background-size: 200% 100%;
  animation: ems-shimmer 1.2s ease-in-out infinite;
}
@keyframes ems-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ── Error ─────────────────────────────────────────────────────── */
.ems-error-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
}
.ems-error-state__card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 2.5rem;
  border: 1px solid rgba(220,38,38,0.15);
  border-radius: var(--ds-radius-xl);
  background: rgba(220,38,38,0.04);
  max-width: 360px;
  text-align: center;
}
.ems-error-state__icon { color: var(--ds-danger); opacity: 0.7; }
.ems-error-state__title { font-size: 1rem; font-weight: 800; color: var(--ds-danger); margin: 0; }
.ems-error-state__msg { font-size: 0.825rem; color: var(--ds-danger); margin: 0; }
.ems-error-state__retry {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1.25rem;
  border-radius: var(--ds-radius-md);
  border: 1px solid var(--ds-danger);
  background: transparent;
  color: var(--ds-danger);
  font-size: 0.825rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
  margin-top: 0.5rem;
}
.ems-error-state__retry:hover { background: rgba(220,38,38,0.06); }

/* ── Empty ─────────────────────────────────────────────────────── */
.ems-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.875rem;
  padding: 5rem 2rem;
  text-align: center;
}
.ems-empty__illustration { margin-bottom: 0.5rem; }
.ems-empty__title {
  font-size: 1.125rem;
  font-weight: 900;
  color: var(--ds-text);
  margin: 0;
}
.ems-empty__sub {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  margin: 0;
  max-width: 320px;
}

/* ── Responsive ─────────────────────────────────────────────────── */
@media (max-width: 1100px) {
  .ems-overview { grid-template-columns: 1fr; }
  .ems-overview__stats { flex-direction: row; }
  .ems-stat-card { flex: 1; }
}
@media (max-width: 768px) {
  .ems-root { padding: 0; }
  .ems-header { padding: 1rem 1.25rem; }
  .ems-header__title { font-size: 1.1rem; }
  .ems-overview { padding: 1rem 1.25rem 0; }
  .ems-overview__stats { flex-direction: column; }
  .ems-content { padding: 1rem 1.25rem 0; }
  .ems-live-grid { grid-template-columns: 1fr; }
  .ems-exam-grid { grid-template-columns: 1fr; }
  .ems-exam-table__head,
  .ems-exam-table__row {
    grid-template-columns: 2fr 1fr 1.5fr 120px;
  }
  .ems-exam-table__th:nth-child(3),
  .ems-exam-table__td:nth-child(3),
  .ems-exam-table__th:nth-child(5),
  .ems-exam-table__td:nth-child(5) {
    display: none;
  }
}
</style>
