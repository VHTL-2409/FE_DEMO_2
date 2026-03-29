<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="portal-viewport flex h-full min-h-0 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100"
  >
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
        <StudentTopHeader active-section="dashboard" class="shrink-0" />

        <main
          class="teacher-page-shell student-stitch-main student-dashboard-shell relative flex min-h-0 w-full max-w-screen-2xl flex-1 flex-col overflow-hidden"
        >
          <div
            class="student-stitch-paper-bg portal-scrollbar flex min-h-0 flex-1 flex-col overflow-y-auto overscroll-contain dark:bg-background-dark"
          >
            <div class="w-full px-3 pb-12 pt-5 sm:px-5 lg:px-8">
              <!-- Tiêu đề — gọn, tách biệt phong cách GV -->
              <header class="mb-6 border-b border-[#dbc2b0]/25 pb-5 dark:border-slate-700">
                <p class="mb-1 text-[11px] font-semibold uppercase tracking-[0.12em] text-primary dark:text-amber-400/90">
                  Bảng điều khiển học sinh
                </p>
                <h1 class="stitch-font-headline text-2xl font-bold tracking-tight text-amber-950 dark:text-amber-100 md:text-3xl">
                  Xin chào, <span class="text-[var(--role-primary)]">{{ displayName }}</span>
                </h1>
                <p class="mt-2 max-w-2xl text-sm leading-relaxed text-slate-600 dark:text-slate-400">
                  Theo dõi điểm số và hoạt động — dữ liệu từ các lượt thi / luyện tập đã ghi nhận.
                </p>
              </header>

              <!-- Hành động nhanh: thanh ngang, không dùng hero 2 cột kiểu GV -->
              <section
                class="mb-8 flex flex-col gap-3 rounded-2xl border border-[#dbc2b0]/30 bg-white/90 p-4 shadow-sm dark:border-slate-600 dark:bg-slate-900/70 sm:flex-row sm:items-stretch sm:gap-4"
              >
                <div class="flex min-h-[3rem] min-w-0 flex-1 flex-col gap-2 sm:flex-row sm:items-center">
                  <label class="sr-only" for="student-dash-quick-code">Mã hoặc mã bài thi</label>
                  <input
                    id="student-dash-quick-code"
                    v-model="dashboardQuickCode"
                    type="text"
                    autocomplete="off"
                    maxlength="120"
                    class="min-h-[48px] w-full flex-1 rounded-xl border border-[#dbc2b0]/40 bg-[#faf9f5] px-4 text-sm text-[#191c1e] placeholder:text-slate-400 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
                    placeholder="Nhập mã bài thi…"
                    @keyup.enter="quickJoinExam"
                  />
                  <button
                    type="button"
                    class="silk-press-gradient inline-flex min-h-[48px] shrink-0 items-center justify-center gap-2 rounded-xl px-6 text-sm font-bold text-white shadow-md transition hover:opacity-95 disabled:opacity-50"
                    :disabled="isQuickJoining"
                    @click="quickJoinExam"
                  >
                    {{ isQuickJoining ? 'Đang…' : 'Vào phòng thi' }}
                  </button>
                </div>
                <div class="flex shrink-0 flex-col gap-2 sm:flex-row sm:items-center sm:border-l sm:border-[#dbc2b0]/30 sm:pl-4 dark:sm:border-slate-600">
                  <RouterLink
                    to="/student/exam-join"
                    class="inline-flex min-h-[44px] items-center justify-center rounded-xl border border-[#dbc2b0]/50 px-4 text-sm font-bold text-[#191c1e] transition hover:bg-[#f4f4f0] dark:border-slate-600 dark:text-slate-100 dark:hover:bg-slate-800"
                  >
                    Trang nhập mã
                  </RouterLink>
                  <RouterLink
                    to="/student/generate-practice-test"
                    class="inline-flex min-h-[44px] items-center justify-center rounded-xl bg-[#efeeea] px-4 text-sm font-bold text-[#191c1e] transition hover:bg-[#e5e2dc] dark:bg-slate-800 dark:text-amber-100 dark:hover:bg-slate-700"
                  >
                    Tạo luyện tập
                  </RouterLink>
                </div>
              </section>

              <!-- Chỉ số tóm tắt (dữ liệu thật) -->
              <section class="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
                <div
                  v-for="k in summaryKpis"
                  :key="k.label"
                  class="rounded-xl border border-[#dbc2b0]/20 bg-white/95 px-4 py-3 dark:border-slate-700 dark:bg-slate-900/60"
                >
                  <p class="stitch-font-headline text-2xl font-bold tabular-nums text-primary dark:text-amber-200">
                    {{ k.value }}
                  </p>
                  <p class="mt-0.5 text-[11px] font-medium leading-tight text-slate-500 dark:text-slate-400">
                    {{ k.label }}
                  </p>
                </div>
              </section>

              <!-- Biểu đồ -->
              <section class="mb-8 grid grid-cols-1 gap-6 xl:grid-cols-2">
                <div
                  class="flex min-h-0 flex-col rounded-2xl border border-[#dbc2b0]/25 bg-white p-4 shadow-sm dark:border-slate-700 dark:bg-slate-900/50 sm:p-5"
                >
                  <div class="mb-2 flex items-center justify-between gap-2">
                    <h2 class="text-sm font-bold text-slate-800 dark:text-slate-100">Điểm các lượt gần đây</h2>
                    <span class="text-[11px] text-slate-400">Thang 10</span>
                  </div>
                  <div
                    ref="chartTrendRef"
                    class="student-dash-chart h-[260px] w-full max-w-full shrink-0 overflow-hidden"
                    role="img"
                    aria-label="Biểu đồ điểm theo lượt"
                  />
                  <p v-if="!trendHasData" class="mt-2 text-center text-xs text-slate-500">
                    Chưa có đủ lượt có điểm để vẽ biểu đồ.
                  </p>
                </div>
                <div
                  class="flex min-h-0 flex-col rounded-2xl border border-[#dbc2b0]/25 bg-white p-4 shadow-sm dark:border-slate-700 dark:bg-slate-900/50 sm:p-5"
                >
                  <div class="mb-2">
                    <h2 class="text-sm font-bold text-slate-800 dark:text-slate-100">Bài thi và luyện tập</h2>
                    <p class="text-[11px] text-slate-500">Tỷ lệ theo số lượt đã ghi nhận</p>
                  </div>
                  <div
                    ref="chartDonutRef"
                    class="student-dash-chart h-[260px] w-full max-w-full shrink-0 overflow-hidden"
                    role="img"
                    aria-label="Biểu đồ phân bổ bài thi và luyện tập"
                  />
                </div>
              </section>

              <section class="mb-8">
                <div class="mb-3 flex min-h-0 flex-col rounded-2xl border border-[#dbc2b0]/25 bg-white p-4 shadow-sm dark:border-slate-700 dark:bg-slate-900/50 sm:p-5">
                  <h2 class="mb-1 text-sm font-bold text-slate-800 dark:text-slate-100">Hoạt động 6 tháng gần nhất</h2>
                  <p class="mb-3 text-[11px] text-slate-500">Số lượt đã nộp theo tháng</p>
                  <div
                    ref="chartBarRef"
                    class="student-dash-chart h-[240px] w-full max-w-full shrink-0 overflow-hidden sm:h-[260px]"
                    role="img"
                    aria-label="Biểu đồ cột hoạt động theo tháng"
                  />
                </div>
              </section>

              <!-- Lịch sử gần đây (dữ liệu thật) -->
              <section>
                <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
                  <h2 class="stitch-font-headline text-lg font-bold text-amber-950 dark:text-amber-100">
                    Hoạt động gần đây
                  </h2>
                  <button
                    type="button"
                    class="inline-flex items-center gap-1 text-sm font-bold text-primary hover:underline dark:text-amber-300"
                    @click="goToStudyHistory"
                  >
                    Xem toàn bộ lịch sử
                    <span class="material-symbols-outlined text-base">chevron_right</span>
                  </button>
                </div>

                <div v-if="isLoadingAttempts" class="space-y-2" aria-busy="true">
                  <SkeletonLoader variant="table-row" />
                  <SkeletonLoader variant="table-row" />
                </div>
                <div
                  v-else-if="!recentRows.length"
                  class="rounded-xl border border-dashed border-[#dbc2b0]/50 bg-[#faf9f5]/80 px-6 py-10 text-center dark:border-slate-600 dark:bg-slate-900/40"
                >
                  <p class="text-sm text-slate-600 dark:text-slate-400">Chưa có lượt thi hoặc luyện tập nào.</p>
                  <RouterLink
                    to="/student/exam-join"
                    class="mt-4 inline-block text-sm font-bold text-primary hover:underline dark:text-amber-300"
                  >
                    Tham gia bài thi
                  </RouterLink>
                </div>
                <div
                  v-else
                  class="overflow-hidden rounded-2xl border border-[#dbc2b0]/25 bg-[#f3f1ec]/40 p-2 shadow-sm dark:border-slate-700 dark:bg-slate-900/35 sm:p-3"
                >
                  <ul role="list" class="space-y-2">
                    <li
                      v-for="item in recentRows"
                      :key="item.attemptId"
                      class="group cursor-pointer rounded-xl border border-[#e4ddd4]/90 bg-white p-3.5 shadow-sm transition hover:border-primary/30 hover:shadow-md dark:border-slate-700/90 dark:bg-slate-900/65 sm:p-4"
                      @click="goToExamResult(item)"
                    >
                      <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
                        <div class="min-w-0 flex-1">
                          <div class="flex items-start justify-between gap-2">
                            <h3
                              class="stitch-font-headline line-clamp-2 text-[0.9375rem] font-semibold leading-snug text-[#191c1e] group-hover:text-primary dark:text-slate-100"
                            >
                              {{ item.title }}
                            </h3>
                            <span
                              class="material-symbols-outlined shrink-0 text-lg text-slate-300 transition group-hover:text-primary/70 dark:text-slate-600"
                              aria-hidden="true"
                            >
                              chevron_right
                            </span>
                          </div>
                          <div
                            class="mt-2 flex flex-wrap items-center gap-x-2 gap-y-1 text-[0.8125rem] leading-relaxed text-slate-500 dark:text-slate-400"
                          >
                            <span
                              class="inline-flex items-center rounded-md bg-[#f4f4f0] px-2 py-0.5 font-medium text-slate-700 dark:bg-slate-800 dark:text-slate-300"
                            >
                              {{ item.kind }}
                            </span>
                            <span class="text-slate-300 dark:text-slate-600" aria-hidden="true">·</span>
                            <span class="tabular-nums">{{ item.time }}</span>
                            <span class="text-slate-300 dark:text-slate-600" aria-hidden="true">·</span>
                            <span class="tabular-nums">{{ item.date }}</span>
                          </div>
                        </div>
                        <div
                          class="flex shrink-0 items-baseline gap-0.5 border-t border-[#eee9e2] pt-2.5 sm:border-t-0 sm:pt-0 sm:pl-2 dark:border-slate-700"
                        >
                          <span class="stitch-font-headline text-2xl font-bold tabular-nums text-primary dark:text-amber-200">
                            {{ item.score }}
                          </span>
                          <span class="text-xs font-medium text-slate-400">/10</span>
                        </div>
                      </div>
                    </li>
                  </ul>
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
import { createRafThrottle } from '../../utils/chartResizeThrottle'
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

const PRIMARY = '#8d4b00'
const AMBER_LIGHT = '#f59e0b'
const MUTED = '#94a3b8'

function parseScoreNum(attempt) {
  const s = attempt?.score
  if (s === null || s === undefined) return null
  const n = Number(s)
  return Number.isFinite(n) ? n : null
}

const summaryKpis = computed(() => {
  const list = attempts.value || []
  const n = list.length
  const scored = list.map(parseScoreNum).filter((x) => x !== null)
  const avg =
    scored.length > 0 ? (scored.reduce((a, b) => a + b, 0) / scored.length).toFixed(1) : '—'
  const examN = list.filter((a) => !a.isPractice).length
  const prN = list.filter((a) => a.isPractice).length
  return [
    { label: 'Tổng lượt đã ghi nhận', value: n ? String(n) : '0' },
    { label: 'Điểm trung bình (có chấm)', value: avg },
    { label: 'Bài thi chính thức', value: String(examN) },
    { label: 'Luyện tập', value: String(prN) }
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
        color: [PRIMARY],
        grid: { left: 48, right: 16, top: 28, bottom: 40 },
        tooltip: { trigger: 'axis' },
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
            areaStyle: { color: 'rgba(141, 75, 0, 0.14)' }
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
        color: [PRIMARY, AMBER_LIGHT],
        tooltip: { trigger: 'item' },
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
        color: [PRIMARY],
        grid: { left: 40, right: 16, top: 20, bottom: 32 },
        tooltip: { trigger: 'axis' },
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
    .map((attempt) => ({
      title: attempt.examTitle || 'Bài thi',
      kind: attempt.isPractice ? 'Luyện tập' : 'Bài thi',
      score: scoreOnTenDisplay(attempt.score),
      date: attempt.submittedAt ? new Date(attempt.submittedAt).toLocaleDateString('vi-VN') : '—',
      attemptId: attempt.id,
      examId: attempt.examId,
      time:
        attempt.startedAt && attempt.submittedAt
          ? `${Math.max(1, Math.round((new Date(attempt.submittedAt).getTime() - new Date(attempt.startedAt).getTime()) / 60000))} phút`
          : '—'
    }))
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

e>
