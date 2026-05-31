<template>
  <div class="td-page">
    <div class="td-page__inner mx-auto max-w-7xl px-4 pb-10 pt-6 sm:px-6 lg:px-8">
      <div class="td-shell mb-5">
        <div class="td-shell__breadcrumb">
          <RouterLink class="td-shell__bc-link" to="/teacher/dashboard">
            <LucideIcon name="home" />
            <span>Trang chủ</span>
          </RouterLink>
          <LucideIcon class="td-shell__bc-sep" name="chevron_right" />
          <span class="td-shell__bc-current">{{ breadcrumbCurrent }}</span>
        </div>

        <nav class="td-tabs td-tabs--in-shell" role="tablist" aria-label="Khu vực bảng điều khiển">
          <button
            v-for="t in tabDefs"
            :key="t.id"
            type="button"
            role="tab"
            class="td-tabs__btn"
            :class="{ 'td-tabs__btn--active': activeTab === t.id }"
            :id="`teacher-dashboard-tab-${t.id}`"
            :aria-selected="activeTab === t.id"
            :aria-controls="`teacher-dashboard-panel-${t.id}`"
            :tabindex="activeTab === t.id ? 0 : -1"
            @click="goTab(t.id)"
          >
            <LucideIcon :name="t.icon" class="td-tabs__icon" />
            {{ t.label }}
          </button>
        </nav>

        <div v-if="activeTab === 'overview'" class="td-shell__band">
          <DashboardHero
            embedded
            :teacher-name="teacherName"
            :live-exam-count="statusCounts.started"
            :active-student-count="activeStudentCount"
            :alert-count="alertCount"
          />
        </div>

        <div v-else class="td-shell__band td-shell__band--analytics">
          <div class="td-shell__analytics-head">
            <div class="td-shell__analytics-icon">
              <LucideIcon name="bar_chart" />
            </div>
            <div>
              <h2 class="td-shell__analytics-title">Phân tích & hiệu suất</h2>
              <p class="td-shell__analytics-sub">Xu hướng làm bài và hiệu quả đề thi</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab: overview -->
      <div
        v-show="activeTab === 'overview'"
        id="teacher-dashboard-panel-overview"
        class="td-tab-panel"
        role="tabpanel"
        aria-labelledby="teacher-dashboard-tab-overview"
      >

        <div class="mb-6">
          <DashboardKpiGrid
            :stats="kpiStats"
            :alert-count="alertCount"
          />
        </div>

        <div v-if="liveExam" class="mb-6">
          <MonitoringCard
            :live-exam="liveExam"
            @go-monitoring="goToMonitoring"
            @go-monitoring-exam="goToLiveMonitoringSession"
          />
        </div>

        <div v-if="!rawExams.length" class="mb-6">
          <EmptyState
            icon="assignment"
            title="Chưa có đề thi nào"
            :action-label="dashCopy.emptyCta"
            fill
            @action="goToCreate"
          />
        </div>

        <div v-if="rawExams.length" class="mb-6">
          <div class="td-panel td-panel--2">
            <SchedulePanel :schedule-series="scheduleSeries" />
          </div>
        </div>
      </div>

      <!-- Tab: Phân tích (nhúng cùng trang) -->
      <div
        v-show="activeTab === 'analytics'"
        id="teacher-dashboard-panel-analytics"
        class="td-tab-panel td-tab-panel--embed"
        role="tabpanel"
        aria-labelledby="teacher-dashboard-tab-analytics"
      >
        <TeacherAnalyticsDashboard />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch, defineAsyncComponent } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ApiError } from '../../services/apiClient'
import { listExams } from '../../services/examService'
import { useToast } from '../../composables/useToast'

import DashboardHero from './dashboard/DashboardHero.vue'
import DashboardKpiGrid from './dashboard/DashboardKpiGrid.vue'
import MonitoringCard from './dashboard/MonitoringCard.vue'
import SchedulePanel from './dashboard/SchedulePanel.vue'
import EmptyState from '../ui/EmptyState.vue'

const TeacherAnalyticsDashboard = defineAsyncComponent(() => import('./TeacherAnalyticsDashboard.vue'))

const route = useRoute()
const router = useRouter()
const toast = useToast()

const rawExams = ref([])

const dashCopy = {
  tabOverview: 'T\u1ed5ng quan',
  tabAnalytics: 'Ph\u00e2n t\u00edch',
  emptyCta: 'M\u1edf workspace t\u1ea1o \u0111\u1ec1',
  statusUpcoming: 'Ch\u01b0a b\u1eaft \u0111\u1ea7u',
  statusStarted: '\u0110ang di\u1ec5n ra'
}

const tabDefs = [
  { id: 'overview', label: dashCopy.tabOverview, icon: 'dashboard' },
  { id: 'analytics', label: dashCopy.tabAnalytics, icon: 'bar_chart' }
]

const activeTab = computed(() => (route.query.tab === 'analytics' ? 'analytics' : 'overview'))

const breadcrumbCurrent = computed(() =>
  activeTab.value === 'analytics' ? 'Phân tích' : 'Bảng điều khiển'
)

const goTab = (tabId) => {
  const q = { ...route.query }
  if (tabId === 'overview') {
    delete q.tab
  } else if (tabId === 'analytics') {
    q.tab = 'analytics'
  }
  router.replace({ path: '/teacher/dashboard', query: q })
}

/** Back-compat: ?tab=results -> /teacher/exams/review/summary */
watch(
  () => [route.path, route.query.tab, route.query.examId, route.query.title],
  () => {
    if (route.path !== '/teacher/dashboard') return
    if (route.query.tab !== 'results') return
    const examId = route.query.examId
    const title = route.query.title
    if (examId != null && String(examId) !== '') {
      router.replace({
        path: '/teacher/exams/review/summary',
        query: { examId: String(examId), title: title || '' }
      })
    } else {
      router.replace({ path: '/teacher/exams/list' })
    }
  },
  { immediate: true }
)

const getExamStatusMeta = (exam) => {
  if (!exam.isActive) {
    return { key: 'draft', label: 'Bản nháp' }
  }
  const nowMs = Date.now()
  const startMs = new Date(exam.startTime || '').getTime()
  const endMs = new Date(exam.endTime || '').getTime()
  if (!Number.isNaN(startMs) && nowMs < startMs) {
    return { key: 'upcoming', label: dashCopy.statusUpcoming }
  }
  if (!Number.isNaN(startMs) && !Number.isNaN(endMs) && nowMs >= startMs && nowMs <= endMs) {
    return { key: 'started', label: dashCopy.statusStarted }
  }
  return { key: 'ended', label: 'Đã kết thúc' }
}

const getSortTime = (exam) => {
  for (const value of [exam.endTime, exam.startTime, exam.updatedAt, exam.createdAt]) {
    const t = new Date(value || '').getTime()
    if (!Number.isNaN(t)) return t
  }
  return 0
}

const exams = computed(() =>
  rawExams.value.slice().sort((a, b) => getSortTime(b) - getSortTime(a))
)

const statusCounts = computed(() =>
  exams.value.reduce((acc, exam) => {
    const { key } = getExamStatusMeta(exam)
    acc[key] = (acc[key] || 0) + 1
    return acc
  }, { draft: 0, upcoming: 0, started: 0, ended: 0 })
)

const activeStudentCount = computed(() => statusCounts.value.started * 3)
const alertCount = ref(0)

const liveExam = computed(() => {
  const started = exams.value.find(e => getExamStatusMeta(e).key === 'started')
  if (!started) return null
  return {
    ...started,
    isLive: true,
    studentCount: started.participantCount || 0,
    answeredCount: 0,
    questionCount: started.questionCount || 0,
    alertCount: 0
  }
})

const kpiStats = computed(() => ({
  total: rawExams.value.length,
  active: statusCounts.value.started,
  ended: statusCounts.value.ended,
  draft: statusCounts.value.draft,
  upcoming: statusCounts.value.upcoming,
  activeTrend: null
}))

const padDateKey = (d) => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const scheduleSeries = computed(() => {
  const days = []
  const now = new Date()
  for (let i = 6; i >= 0; i--) {
    const d = new Date(now)
    d.setHours(0, 0, 0, 0)
    d.setDate(d.getDate() - i)
    days.push(d)
  }
  const keys = days.map(padDateKey)
  const startCount = new Map(keys.map(k => [k, 0]))
  const endCount = new Map(keys.map(k => [k, 0]))
  for (const exam of rawExams.value) {
    const sk = padDateKey(new Date(exam.startTime || ''))
    const ek = padDateKey(new Date(exam.endTime || ''))
    if (startCount.has(sk)) startCount.set(sk, startCount.get(sk) + 1)
    if (endCount.has(ek)) endCount.set(ek, endCount.get(ek) + 1)
  }
  return {
    labels: days.map(d => d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })),
    starts: keys.map(k => startCount.get(k)),
    ends: keys.map(k => endCount.get(k))
  }
})

const teacherName = ref('Giáo viên')

const goToMonitoring = () => router.push('/teacher/live-monitoring')
const goToLiveMonitoringSession = (exam) => {
  if (!exam?.id) return
  router.push(`/teacher/exams/${exam.id}/monitoring`)
}
const goToCreate = () => router.push('/teacher/exams/create')

const loadExams = async () => {
  try {
    const cached = sessionStorage.getItem('prefetch_teacher_data')
    if (cached) {
      rawExams.value = JSON.parse(cached)
      sessionStorage.removeItem('prefetch_teacher_data')
    } else {
      rawExams.value = await listExams()
    }
  } catch (error) {
    toast.error(error instanceof ApiError ? error.message : 'Không thể tải dữ liệu dashboard.')
  }
}

onMounted(async () => {
  await loadExams()
})
</script>

<style scoped>
.td-page {
  min-height: 100vh;
}

.td-shell {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.25rem 1.5rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
}

.dark .td-shell {
  background: var(--ds-gray-900);
}

.td-shell__breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
}

.td-shell__bc-link {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: var(--ds-text-muted);
  text-decoration: none;
  font-weight: 600;
  transition: color 0.15s ease;
}

.td-shell__bc-link:hover {
  color: var(--ds-primary);
}

.td-shell__bc-sep {
  color: var(--ds-text-muted);
  width: 14px;
  height: 14px;
}

.td-shell__bc-current {
  font-weight: 600;
  color: var(--ds-text);
}

.td-shell__band {
  padding-top: 0.25rem;
  margin-top: 0.25rem;
  border-top: 1px solid var(--ds-border);
}

.td-shell__band--analytics {
  padding-top: 1rem;
  margin-top: 0;
}

.td-shell__analytics-head {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
}

.td-shell__analytics-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.75rem;
  height: 2.75rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  flex-shrink: 0;
}

.td-shell__analytics-title {
  margin: 0;
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--ds-text);
  letter-spacing: -0.02em;
}

.td-shell__analytics-sub {
  margin: 0.25rem 0 0;
  font-size: 0.8125rem;
  color: var(--ds-text-secondary);
  line-height: 1.45;
}

.td-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 0;
  padding: 0.25rem;
  border-radius: var(--ds-radius-xl);
  background: color-mix(in srgb, var(--ds-gray-50) 80%, transparent);
  border: 1px solid var(--ds-border);
}

.dark .td-tabs {
  background: color-mix(in srgb, var(--ds-gray-800) 65%, transparent);
}

.td-tabs--in-shell {
  margin-bottom: 0;
}

.td-tabs__btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1rem;
  border-radius: var(--ds-radius-lg);
  border: none;
  background: transparent;
  color: var(--ds-text-muted);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease, box-shadow 0.15s ease;
}

.td-tabs__btn:hover {
  color: var(--ds-text);
  background: var(--ds-primary-soft);
}

.td-tabs__btn--active {
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  box-shadow: inset 0 0 0 1px var(--ds-primary-border);
}

.td-tabs__icon {
  flex-shrink: 0;
}

.td-tab-panel--embed {
  margin-top: 0.25rem;
}

.td-panel {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 1.375rem;
  animation: slideInPanel 0.45s cubic-bezier(0.34, 1.2, 0.64, 1) both;
}

@keyframes slideInPanel {
  from { opacity: 0; }
  to   { opacity: 1; }
}

.td-panel--1 { animation-delay: 0s; }
.td-panel--2 { animation-delay: 0.06s; }

@media (prefers-reduced-motion: reduce) {
  .td-panel { animation: none; }
}
</style>
