<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="portal-viewport flex h-full min-h-0 flex-col"
    style="background: var(--glass-bg)"
  >
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
        <StudentTopHeader active-section="dashboard" class="shrink-0" />

        <main
          class="student-page-shell relative flex min-h-0 w-full max-w-screen-2xl flex-1 flex-col overflow-hidden"
        >
          <div
            class="portal-scrollbar flex min-h-0 flex-1 flex-col overflow-y-auto overscroll-contain"
            style="background: var(--glass-bg)"
          >
            <div class="w-full px-3 pb-12 pt-5 sm:px-5 lg:px-8">

              <!-- Page Header -->
              <header class="glass-card mb-6 p-5 gs-reveal">
                <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <p class="text-xs font-bold uppercase tracking-widest mb-1" style="color: var(--glass-text-muted)">Bảng điều khiển</p>
                    <h1 class="text-2xl lg:text-3xl font-black leading-tight" style="font-family: 'Playfair Display', serif; color: var(--glass-text)">
                      Xin chào, <span style="color: var(--glass-amber)">{{ displayName }}</span>
                    </h1>
                    <p class="mt-1 text-sm leading-relaxed" style="color: var(--glass-text-secondary)">
                      Theo dõi điểm số và hoạt động — dữ liệu từ các lượt thi / luyện tập đã ghi nhận.
                    </p>
                  </div>
                  <!-- Avatar chip -->
                  <div class="flex items-center gap-3">
                    <div class="hidden sm:flex flex-col items-end">
                      <p class="text-sm font-bold" style="color: var(--glass-text)">{{ displayName }}</p>
                      <p class="text-xs" style="color: var(--glass-text-muted)">Học sinh</p>
                    </div>
                    <div class="w-10 h-10 rounded-full overflow-hidden border-2 shrink-0" style="border-color: var(--glass-amber)">
                      <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=student" alt="Avatar" class="w-full h-full object-cover" />
                    </div>
                  </div>
                </div>
              </header>

              <!-- Quick Join Bar -->
              <section class="glass-card mb-8 p-4 gs-reveal">
                <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
                  <label class="sr-only" for="student-dash-quick-code">Mã bài thi</label>
                  <div class="relative flex-1">
                    <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-lg" style="color: var(--glass-text-muted)">qr_code_scanner</span>
                    <input
                      id="student-dash-quick-code"
                      v-model="dashboardQuickCode"
                      type="text"
                      autocomplete="off"
                      maxlength="120"
                      class="gs-input min-h-[48px] w-full pl-10"
                      placeholder="Nhập mã bài thi để vào thi ngay…"
                      @keyup.enter="quickJoinExam"
                    />
                  </div>
                  <button
                    type="button"
                    class="gs-btn-primary min-h-[48px] px-6 inline-flex items-center justify-center gap-2 text-sm font-bold rounded-xl"
                    :disabled="isQuickJoining"
                    @click="quickJoinExam"
                  >
                    <span class="material-symbols-outlined text-lg" style="font-variation-settings:'FILL'1">login</span>
                    {{ isQuickJoining ? 'Đang…' : 'Vào phòng thi' }}
                  </button>
                </div>
                <div class="flex flex-wrap gap-2 mt-3">
                  <RouterLink
                    to="/student/exam-join"
                    class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold border transition-all"
                    style="background: var(--glass-surface); border-color: var(--glass-border); color: var(--glass-text-secondary)"
                  >
                    <span class="material-symbols-outlined text-sm">login</span>
                    Trang nhập mã
                  </RouterLink>
                  <RouterLink
                    to="/student/generate-practice-test"
                    class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold border transition-all"
                    style="background: var(--glass-violet-soft); border-color: var(--glass-violet-border); color: var(--glass-violet)"
                  >
                    <span class="material-symbols-outlined text-sm">auto_awesome</span>
                    Tạo luyện tập
                  </RouterLink>
                </div>
              </section>

              <!-- KPI Cards -->
              <section class="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
                <div
                  v-for="(k, i) in summaryKpis"
                  :key="k.label"
                  :style="{ animationDelay: `${i * 80}ms` }"
                  class="glass-card p-5 gs-reveal gs-spring group"
                >
                  <div class="w-11 h-11 rounded-xl flex items-center justify-center mb-3 transition-transform group-hover:scale-110" :style="`background: ${k.bg}`">
                    <span class="material-symbols-outlined text-xl" :style="`color: ${k.color}`">{{ k.icon }}</span>
                  </div>
                  <p class="text-2xl lg:text-3xl font-black mb-1" :style="`color: var(--glass-text); font-family: 'Space Grotesk', monospace`">{{ k.value }}</p>
                  <p class="text-xs font-semibold leading-tight" style="color: var(--glass-text-muted)">{{ k.label }}</p>
                </div>
              </section>

              <!-- Charts Row -->
              <section class="mb-8 grid grid-cols-1 gap-6 xl:grid-cols-2">
                <!-- Trend Chart -->
                <div class="glass-card p-5 gs-reveal">
                  <div class="flex items-center justify-between gap-2 mb-4">
                    <div>
                      <h2 class="text-sm font-bold" style="color: var(--glass-text)">Điểm các lượt gần đây</h2>
                      <p class="text-xs" style="color: var(--glass-text-muted)">Thang 10 điểm</p>
                    </div>
                    <div class="w-9 h-9 rounded-lg flex items-center justify-center" style="background: var(--glass-amber-soft)">
                      <span class="material-symbols-outlined text-lg" style="color: var(--glass-amber)">show_chart</span>
                    </div>
                  </div>
                  <div
                    ref="chartTrendRef"
                    class="student-dash-chart h-[260px] w-full max-w-full shrink-0 overflow-hidden"
                    role="img"
                    aria-label="Biểu đồ điểm theo lượt"
                  />
                  <p v-if="!trendHasData" class="mt-4 text-center text-xs" style="color: var(--glass-text-muted)">
                    Chưa có đủ lượt có điểm để vẽ biểu đồ.
                  </p>
                </div>

                <!-- Donut Chart -->
                <div class="glass-card p-5 gs-reveal">
                  <div class="flex items-center justify-between gap-2 mb-4">
                    <div>
                      <h2 class="text-sm font-bold" style="color: var(--glass-text)">Bài thi & Luyện tập</h2>
                      <p class="text-xs" style="color: var(--glass-text-muted)">Phân bổ theo lượt đã ghi nhận</p>
                    </div>
                    <div class="w-9 h-9 rounded-lg flex items-center justify-center" style="background: var(--glass-violet-soft)">
                      <span class="material-symbols-outlined text-lg" style="color: var(--glass-violet)">donut_large</span>
                    </div>
                  </div>
                  <div
                    ref="chartDonutRef"
                    class="student-dash-chart h-[260px] w-full max-w-full shrink-0 overflow-hidden"
                    role="img"
                    aria-label="Biểu đồ phân bổ bài thi và luyện tập"
                  />
                </div>
              </section>

              <!-- Monthly Activity Chart -->
              <section class="mb-8">
                <div class="glass-card p-5 gs-reveal">
                  <div class="flex items-center justify-between gap-2 mb-4">
                    <div>
                      <h2 class="text-sm font-bold" style="color: var(--glass-text)">Hoạt động 6 tháng gần nhất</h2>
                      <p class="text-xs" style="color: var(--glass-text-muted)">Số lượt đã nộp theo tháng</p>
                    </div>
                    <div class="w-9 h-9 rounded-lg flex items-center justify-center" style="background: var(--glass-success-soft)">
                      <span class="material-symbols-outlined text-lg" style="color: var(--glass-success)">bar_chart</span>
                    </div>
                  </div>
                  <div
                    ref="chartBarRef"
                    class="student-dash-chart h-[240px] w-full max-w-full shrink-0 overflow-hidden sm:h-[260px]"
                    role="img"
                    aria-label="Biểu đồ cột hoạt động theo tháng"
                  />
                </div>
              </section>

              <!-- Recent Activity -->
              <section>
                <div class="flex flex-wrap items-center justify-between gap-3 mb-4">
                  <h2 class="text-lg font-black" style="font-family: 'Playfair Display', serif; color: var(--glass-text)">
                    Hoạt động gần đây
                  </h2>
                  <button
                    type="button"
                    class="inline-flex items-center gap-1 text-sm font-semibold transition-colors"
                    style="color: var(--glass-amber)"
                    @click="goToStudyHistory"
                  >
                    Xem toàn bộ lịch sử
                    <span class="material-symbols-outlined text-base">chevron_right</span>
                  </button>
                </div>

                <!-- Loading state -->
                <div v-if="isLoadingAttempts" class="space-y-2" aria-busy="true">
                  <SkeletonLoader variant="table-row" />
                  <SkeletonLoader variant="table-row" />
                </div>

                <!-- Empty state -->
                <div
                  v-else-if="!recentRows.length"
                  class="glass-card text-center py-12 px-6"
                >
                  <div class="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4" style="background: var(--glass-bg-mid)">
                    <span class="material-symbols-outlined text-3xl" style="color: var(--glass-text-muted)">history_edu</span>
                  </div>
                  <p class="text-sm font-semibold mb-4" style="color: var(--glass-text-secondary)">Chưa có lượt thi hoặc luyện tập nào.</p>
                  <RouterLink
                    to="/student/exam-join"
                    class="gs-btn-primary inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-bold"
                  >
                    <span class="material-symbols-outlined text-base" style="font-variation-settings:'FILL'1">login</span>
                    Tham gia bài thi
                  </RouterLink>
                </div>

                <!-- History list -->
                <div v-else class="space-y-3">
                  <div
                    v-for="(item, i) in recentRows"
                    :key="item.attemptId"
                    :style="{ animationDelay: `${i * 60}ms` }"
                    class="glass-card p-4 gs-reveal gs-spring group cursor-pointer"
                    @click="goToExamResult(item)"
                  >
                    <div class="flex items-start justify-between gap-4">
                      <div class="flex items-start gap-3 flex-1 min-w-0">
                        <div class="w-10 h-10 rounded-xl flex items-center justify-center shrink-0 mt-0.5"
                          :style="item.isPractice ? 'background: var(--glass-violet-soft)' : 'background: var(--glass-amber-soft)'">
                          <span class="material-symbols-outlined text-lg"
                            :style="item.isPractice ? 'color: var(--glass-violet)' : 'color: var(--glass-amber)'">
                            {{ item.isPractice ? 'auto_awesome' : 'assignment' }}
                          </span>
                        </div>
                        <div class="min-w-0 flex-1">
                          <h3 class="text-sm font-bold leading-snug truncate" style="color: var(--glass-text)">
                            {{ item.title }}
                          </h3>
                          <div class="flex flex-wrap items-center gap-2 mt-1.5">
                            <span class="inline-flex items-center gap-1 text-[11px] font-semibold px-2 py-0.5 rounded-full"
                              :style="item.isPractice
                                ? 'background: var(--glass-violet-soft); color: var(--glass-violet)'
                                : 'background: var(--glass-amber-soft); color: var(--glass-amber)'">
                              {{ item.kind }}
                            </span>
                            <span class="text-xs" style="color: var(--glass-text-muted)">{{ item.time }}</span>
                            <span class="text-xs" style="color: var(--glass-text-muted)">·</span>
                            <span class="text-xs" style="color: var(--glass-text-muted)">{{ item.date }}</span>
                          </div>
                        </div>
                      </div>
                      <div class="flex items-center gap-1 shrink-0">
                        <span class="text-2xl font-black" :style="`font-family: 'Space Grotesk', monospace; color: ${item.scoreColor}`">
                          {{ item.score }}
                        </span>
                        <span class="text-xs font-medium" style="color: var(--glass-text-muted)">/10</span>
                        <span class="material-symbols-outlined text-lg transition-transform group-hover:translate-x-1" style="color: var(--glass-text-muted)">chevron_right</span>
                      </div>
                    </div>
                  </div>
                </div>
              </section>

            </div>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { listMyAttempts } from '../../services/attemptService'
import { displayNameFromMe, fetchMyProfile } from '../../services/authService'
import { joinExamByCode } from '../../services/examService'
import { useToast } from '../../composables/useToast'
import { scoreOnTenDisplay } from '../../utils/scoreDisplay'
import StudentTopHeader from './StudentTopHeader.vue'
import SkeletonLoader from '../shared/SkeletonLoader.vue'

const router = useRouter()
const isDark = ref(false)
const attempts = ref([])
const isLoadingAttempts = ref(false)
const toast = useToast()
const profile = ref(null)
const dashboardQuickCode = ref('')
const isQuickJoining = ref(false)

const chartTrendRef = ref(null)
const chartDonutRef = ref(null)
const chartBarRef = ref(null)
let chartTrend = null
let chartDonut = null
let chartBar = null

const displayName = computed(() => displayNameFromMe(profile.value) || 'Học sinh')

const AMBER = '#d97706'
const AMBER_LIGHT = '#f59e0b'
const VIOLET = '#7c3aed'
const MUTED = '#94a3b8'

function parseScoreNum(attempt) {
  const s = attempt?.score
  if (s === null || s === undefined) return null
  const n = Number(s)
  return Number.isFinite(n) ? n : null
}

function scoreColor(score) {
  if (score === null || score === undefined) return 'var(--glass-text-muted)'
  const n = Number(score)
  if (n >= 8) return 'var(--glass-success)'
  if (n >= 5) return 'var(--glass-amber)'
  return 'var(--glass-danger)'
}

const summaryKpis = computed(() => {
  const list = attempts.value || []
  const n = list.length
  const scored = list.map(parseScoreNum).filter((x) => x !== null)
  const avg = scored.length > 0 ? (scored.reduce((a, b) => a + b, 0) / scored.length).toFixed(1) : '—'
  const examN = list.filter((a) => !a.isPractice).length
  const prN = list.filter((a) => a.isPractice).length
  return [
    { label: 'Tổng lượt đã ghi nhận', value: n ? String(n) : '0', icon: 'history', bg: 'var(--glass-amber-soft)', color: 'var(--glass-amber)' },
    { label: 'Điểm trung bình', value: avg, icon: 'trending_up', bg: 'var(--glass-success-soft)', color: 'var(--glass-success)' },
    { label: 'Bài thi chính thức', value: String(examN), icon: 'assignment', bg: 'var(--glass-violet-soft)', color: 'var(--glass-violet)' },
    { label: 'Luyện tập', value: String(prN), icon: 'auto_awesome', bg: 'var(--glass-rose-soft)', color: 'var(--glass-rose)' }
  ]
})

const trendHasData = computed(() => {
  const pts = buildTrendPoints()
  return pts.labels.length > 0 && pts.values.some((v) => v !== null)
})

function buildTrendPoints() {
  const rows = (attempts.value || [])
    .filter((a) => a.submittedAt || a.startedAt)
    .slice()
    .sort((a, b) => {
      const t1 = new Date(a.submittedAt || a.startedAt).getTime()
      const t2 = new Date(b.submittedAt || b.startedAt).getTime()
      return t1 - t2
    })
    .slice(-12)

  const labels = []
  const values = []
  for (const a of rows) {
    const d = new Date(a.submittedAt || a.startedAt)
    labels.push(`${d.getDate().toString().padStart(2, '0')}/${(d.getMonth() + 1).toString().padStart(2, '0')}`)
    const n = parseScoreNum(a)
    values.push(n !== null ? Number(n.toFixed(2)) : null)
  }
  return { labels, values }
}

function buildMonthlyBuckets() {
  const now = new Date()
  const months = []
  for (let i = 5; i >= 0; i -= 1) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    months.push({
      key: `${d.getFullYear()}-${d.getMonth()}`,
      label: `T${d.getMonth() + 1}`
    })
  }
  const counts = Object.fromEntries(months.map((m) => [m.key, 0]))
  for (const a of attempts.value || []) {
    const t = a.submittedAt || a.startedAt
    if (!t) continue
    const d = new Date(t)
    const key = `${d.getFullYear()}-${d.getMonth()}`
    if (key in counts) counts[key] += 1
  }
  return { labels: months.map((m) => m.label), values: months.map((m) => counts[m.key] || 0) }
}

function getChartTextColor() {
  if (typeof document === 'undefined') return '#334155'
  return document.documentElement.classList.contains('dark') ? '#e2e8f0' : '#475569'
}

function applyCharts() {
  const textColor = getChartTextColor()
  const { labels, values } = buildTrendPoints()
  const examCount = (attempts.value || []).filter((a) => !a.isPractice).length
  const practiceCount = (attempts.value || []).filter((a) => a.isPractice).length
  const month = buildMonthlyBuckets()

  if (chartTrendRef.value) {
    if (!chartTrend) chartTrend = echarts.init(chartTrendRef.value, undefined, { renderer: 'canvas' })
    chartTrend.setOption(
      {
        color: [AMBER],
        grid: { left: 48, right: 16, top: 20, bottom: 36 },
        tooltip: { trigger: 'axis', backgroundColor: 'rgba(255,255,255,0.95)', borderColor: '#e2e8f0', textStyle: { color: '#334155' } },
        xAxis: {
          type: 'category',
          data: labels,
          axisLabel: { color: MUTED, fontSize: 10 },
          axisLine: { lineStyle: { color: '#e2e8f0' } }
        },
        yAxis: {
          type: 'value',
          min: 0,
          max: 10,
          splitNumber: 5,
          axisLabel: { color: MUTED, fontSize: 10 },
          splitLine: { lineStyle: { color: 'rgba(148,163,184,0.25)' } }
        },
        series: [
          {
            name: 'Điểm',
            type: 'line',
            smooth: true,
            showSymbol: true,
            connectNulls: false,
            data: values,
            lineStyle: { width: 3 },
            areaStyle: { color: 'rgba(217, 119, 6, 0.12)' }
          }
        ]
      },
      true
    )
  }

  if (chartDonutRef.value) {
    if (!chartDonut) chartDonut = echarts.init(chartDonutRef.value, undefined, { renderer: 'canvas' })
    const empty = examCount === 0 && practiceCount === 0
    chartDonut.setOption(
      {
        color: [AMBER, VIOLET],
        tooltip: { trigger: 'item', backgroundColor: 'rgba(255,255,255,0.95)', borderColor: '#e2e8f0', textStyle: { color: '#334155' } },
        legend: {
          bottom: 0,
          textStyle: { color: textColor, fontSize: 11 }
        },
        series: [
          {
            type: 'pie',
            radius: ['42%', '68%'],
            center: ['50%', '46%'],
            avoidLabelOverlap: true,
            itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
            label: { color: textColor, fontSize: 11 },
            data: empty
              ? [{ value: 1, name: 'Chưa có dữ liệu', itemStyle: { color: '#e2e8f0' } }]
              : [
                  { value: examCount, name: 'Bài thi' },
                  { value: practiceCount, name: 'Luyện tập' }
                ]
          }
        ]
      },
      true
    )
  }

  if (chartBarRef.value) {
    if (!chartBar) chartBar = echarts.init(chartBarRef.value, undefined, { renderer: 'canvas' })
    chartBar.setOption(
      {
        color: [AMBER],
        grid: { left: 40, right: 16, top: 20, bottom: 32 },
        tooltip: { trigger: 'axis', backgroundColor: 'rgba(255,255,255,0.95)', borderColor: '#e2e8f0', textStyle: { color: '#334155' } },
        xAxis: {
          type: 'category',
          data: month.labels,
          axisLabel: { color: MUTED, fontSize: 10 }
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          axisLabel: { color: MUTED, fontSize: 10 },
          splitLine: { lineStyle: { color: 'rgba(148,163,184,0.2)' } }
        },
        series: [
          {
            name: 'Lượt',
            type: 'bar',
            barWidth: '48%',
            data: month.values,
            itemStyle: { borderRadius: [6, 6, 0, 0] }
          }
        ]
      },
      true
    )
  }
}

function resizeCharts() {
  chartTrend?.resize()
  chartDonut?.resize()
  chartBar?.resize()
}

function disposeCharts() {
  chartTrend?.dispose()
  chartDonut?.dispose()
  chartBar?.dispose()
  chartTrend = null
  chartDonut = null
  chartBar = null
}

watch(
  attempts,
  () => {
    nextTick(() => applyCharts())
  },
  { deep: true }
)

const recentRows = computed(() =>
  attempts.value
    .filter((a) => a.submittedAt || a.startedAt)
    .slice()
    .sort((a, b) => {
      const aTime = new Date(a.submittedAt || a.startedAt || 0).getTime()
      const bTime = new Date(b.submittedAt || b.startedAt || 0).getTime()
      return (Number.isNaN(bTime) ? 0 : bTime) - (Number.isNaN(aTime) ? 0 : aTime)
    })
    .slice(0, 5)
    .map((attempt) => {
      const scoreVal = scoreOnTenDisplay(attempt.score)
      return {
        title: attempt.examTitle || 'Bài thi',
        kind: attempt.isPractice ? 'Luyện tập' : 'Bài thi',
        isPractice: !!attempt.isPractice,
        score: scoreVal,
        scoreColor: scoreColor(parseScoreNum(attempt)),
        date: attempt.submittedAt ? new Date(attempt.submittedAt).toLocaleDateString('vi-VN') : '—',
        attemptId: attempt.id,
        examId: attempt.examId,
        time:
          attempt.startedAt && attempt.submittedAt
            ? `${Math.max(1, Math.round((new Date(attempt.submittedAt).getTime() - new Date(attempt.startedAt).getTime()) / 60000))} phút`
            : '—'
      }
    })
)

const quickJoinExam = async () => {
  const q = dashboardQuickCode.value.trim()
  if (!q) {
    toast.error('Vui lòng nhập mã hoặc tiêu đề.')
    return
  }
  isQuickJoining.value = true
  try {
    const matchedExam = await joinExamByCode(q)
    if (!matchedExam) {
      toast.error('Không tìm thấy bài thi.')
      return
    }
    router.push({
      path: '/student/exam-waiting-room',
      query: {
        examId: matchedExam.id,
        examCode: matchedExam.code || '',
        exam: matchedExam.title || 'Bài thi',
        duration: matchedExam.durationMinutes || 60,
        questions: matchedExam.questionCount || 0,
        startAt: matchedExam.startTime || '',
        endAt: matchedExam.endTime || '',
        requireCameraMic: matchedExam.requireCameraMic === false ? 'false' : 'true'
      }
    })
  } catch {
    toast.error('Không thể vào bài thi lúc này.')
  } finally {
    isQuickJoining.value = false
  }
}

const goToStudyHistory = () => {
  router.push('/student/study-history')
}

const goToExamResult = (item) => {
  router.push({
    path: '/student/exam-result',
    query: {
      attemptId: item.attemptId,
      examTitle: item.title
    }
  })
}

onMounted(async () => {
  try {
    profile.value = await fetchMyProfile()
  } catch {
    profile.value = null
  }

  isLoadingAttempts.value = true
  try {
    attempts.value = await listMyAttempts()
  } catch {
    attempts.value = []
    toast.error('Không thể tải dữ liệu học tập.')
  } finally {
    isLoadingAttempts.value = false
  }
  await nextTick()
  applyCharts()

  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
})
</script>

<style scoped>
.portal-viewport {
  font-family: 'DM Sans', system-ui, sans-serif;
}

.gs-btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.5rem;
  border-radius: var(--radius-glass-pill);
  font-size: 0.875rem;
  font-weight: 700;
  text-decoration: none;
  background: linear-gradient(135deg, var(--glass-amber) 0%, var(--glass-amber-hover) 100%);
  color: white;
  box-shadow: var(--shadow-glass-md);
  transition: all var(--duration-base) var(--ease-spring);
  border: none;
  cursor: pointer;
}
.gs-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-glass-xl);
  color: white;
}
.gs-btn-primary:active {
  transform: scale(0.97);
}
.gs-btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

@media (prefers-reduced-motion: reduce) {
  .gs-reveal,
  .gs-spring {
    opacity: 1 !important;
    transform: none !important;
    animation: none !important;
  }
}
</style>
