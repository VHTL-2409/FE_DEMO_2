<template>
  <div class="ntd-page">
    <div class="ntd-page__inner">

      <!-- ── Row 1: Welcome Strip ─────────────────────────── -->
      <section class="ntd-hero">
        <div class="ntd-hero__text">
          <h1 class="ntd-hero__greeting">{{ greeting }}, {{ teacherName }}</h1>
          <p class="ntd-hero__date">{{ todayLabel }}</p>
        </div>
        <div class="ntd-hero__live-badge" v-if="liveExamCount > 0">
          <span class="ntd-hero__live-dot" />
          <span>{{ liveExamCount }} kỳ thi đang diễn ra</span>
        </div>
      </section>

      <!-- ── Row 2: KPI Pills ─────────────────────────────── -->
      <section class="ntd-kpi-row">
        <div
          v-for="kpi in kpiPills"
          :key="kpi.label"
          class="ntd-kpi-pill"
          :class="`ntd-kpi-pill--${kpi.color}`"
        >
          <span class="ntd-kpi-pill__value">{{ kpi.value }}</span>
          <span class="ntd-kpi-pill__label">{{ kpi.label }}</span>
          <span v-if="kpi.sub" class="ntd-kpi-pill__sub">{{ kpi.sub }}</span>
        </div>
      </section>

      <!-- ── Row 3: Status Overview + Live Feed ───────────── -->
      <section class="ntd-row ntd-row--gap-5">
        <!-- Exam Status Donut -->
        <div class="ntd-col-7">
          <ChartCard title="Phân bổ trạng thái đề thi" icon="pie_chart" height="280px">
            <template #actions>
              <select v-model="statusPeriod" class="ntd-select-mini">
                <option value="week">7 ngày</option>
                <option value="month">30 ngày</option>
                <option value="all">Tất cả</option>
              </select>
            </template>
            <div ref="statusDonutRef" class="ntd-chart" />
            <template #footer>
              <div class="ntd-donut-legend">
                <div v-for="item in statusLegend" :key="item.key" class="ntd-donut-legend__item">
                  <span class="ntd-donut-legend__dot" :style="{ background: item.color }" />
                  <span class="ntd-donut-legend__label">{{ item.label }}</span>
                  <span class="ntd-donut-legend__value">{{ item.value }}</span>
                </div>
              </div>
            </template>
          </ChartCard>
        </div>

        <!-- Live Activity Feed -->
        <div class="ntd-col-5">
          <ChartCard title="Hoạt động gần đây" icon="activity" height="280px" :noPadding="true">
            <ActivityFeed
              :items="activityItems"
              :loading="loadingActivity"
              :maxHeight="'220px'"
              emptyText="Chưa có hoạt động"
            />
          </ChartCard>
        </div>
      </section>

      <!-- ── Row 4: Weekly Submissions + Score Dist ───────── -->
      <section class="ntd-row ntd-row--gap-5">
        <!-- Weekly submissions area chart -->
        <div class="ntd-col-7">
          <ChartCard title="Lượt nộp bài theo ngày" icon="trending_up" height="300px">
            <template #actions>
              <div class="ntd-period-tabs">
                <button
                  v-for="p in periodOptions"
                  :key="p.value"
                  class="ntd-period-tab"
                  :class="{ 'ntd-period-tab--active': trendPeriod === p.value }"
                  @click="trendPeriod = p.value"
                >{{ p.label }}</button>
              </div>
            </template>
            <div ref="trendChartRef" class="ntd-chart" />
          </ChartCard>
        </div>

        <!-- Score distribution -->
        <div class="ntd-col-5">
          <ChartCard title="Phân bổ điểm số" icon="bar_chart_2" height="300px">
            <div ref="scoreDistRef" class="ntd-chart" />
            <template #footer>
              <div class="ntd-score-stats">
                <div class="ntd-score-stats__item">
                  <span class="ntd-score-stats__label">TB</span>
                  <span class="ntd-score-stats__value">{{ scoreStats.avg }}</span>
                </div>
                <div class="ntd-score-stats__item">
                  <span class="ntd-score-stats__label">Cao</span>
                  <span class="ntd-score-stats__value">{{ scoreStats.max }}</span>
                </div>
                <div class="ntd-score-stats__item">
                  <span class="ntd-score-stats__label">Thấp</span>
                  <span class="ntd-score-stats__value">{{ scoreStats.min }}</span>
                </div>
                <div class="ntd-score-stats__item">
                  <span class="ntd-score-stats__label">Đạt</span>
                  <span class="ntd-score-stats__value">{{ scoreStats.passRate }}%</span>
                </div>
              </div>
            </template>
          </ChartCard>
        </div>
      </section>

      <!-- ── Row 5: Topic Difficulty Radar + Upcoming ─────── -->
      <section class="ntd-row ntd-row--gap-5">
        <!-- Topic difficulty radar -->
        <div class="ntd-col-7">
          <ChartCard title="Độ khó theo chủ đề" icon="radar" height="300px">
            <template #toolbar>
              <select v-model="radarExamId" class="ntd-select-mini">
                <option value="">Tất cả đề thi</option>
                <option v-for="exam in examOptions" :key="exam.id" :value="exam.id">
                  {{ exam.title }}
                </option>
              </select>
            </template>
            <div ref="radarChartRef" class="ntd-chart" />
          </ChartCard>
        </div>

        <!-- Upcoming exams timeline -->
        <div class="ntd-col-5">
          <ChartCard title="Lịch thi sắp tới" icon="view_day" height="300px" :noPadding="true">
            <div class="ntd-upcoming-list">
              <div v-if="loadingUpcoming" class="ntd-upcoming-list__loading">
                <div v-for="i in 3" :key="i" class="ntd-upcoming-skeleton">
                  <div class="ntd-upcoming-skeleton__bar" />
                  <div class="ntd-upcoming-skeleton__text" />
                </div>
              </div>
              <div v-else-if="!upcomingExams.length" class="ntd-upcoming-list__empty">
                <LucideIcon name="calendar_today" size="28" />
                <p>Không có lịch thi sắp tới</p>
              </div>
              <div v-else class="ntd-upcoming-list__items">
                <div
                  v-for="(exam, idx) in upcomingExams"
                  :key="exam.id"
                  class="ntd-upcoming-item"
                >
                  <div class="ntd-upcoming-item__timeline">
                    <div class="ntd-upcoming-item__dot" />
                    <div v-if="idx < upcomingExams.length - 1" class="ntd-upcoming-item__line" />
                  </div>
                  <div class="ntd-upcoming-item__content">
                    <div class="ntd-upcoming-item__header">
                      <span class="ntd-upcoming-item__title">{{ exam.title }}</span>
                      <span class="ntd-upcoming-item__count">{{ exam.studentCount }} HS</span>
                    </div>
                    <div class="ntd-upcoming-item__time">
                      <LucideIcon name="clock" size="12" />
                      <span>{{ exam.startTimeLabel }} &ndash; {{ exam.endTimeLabel }}</span>
                    </div>
                    <div class="ntd-upcoming-item__bar-wrap">
                      <div class="ntd-upcoming-item__bar" :style="{ width: exam.urgencyPct + '%' }" />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </ChartCard>
        </div>
      </section>

      <!-- ── Row 6: Class Overview ──────────────────────────── -->
      <section class="ntd-row ntd-row--gap-5">
        <!-- Class performance comparison -->
        <div class="ntd-col-7">
          <ChartCard title="So sánh điểm trung bình theo lớp" icon="users" height="280px">
            <div ref="classCompRef" class="ntd-chart" />
          </ChartCard>
        </div>

        <!-- Quick subject stats -->
        <div class="ntd-col-5">
          <ChartCard title="Tỷ lệ đạt theo môn" icon="task_alt" height="280px">
            <div ref="passRateRef" class="ntd-chart" />
          </ChartCard>
        </div>
      </section>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import ChartCard from './ChartCard.vue'
import ActivityFeed from './ActivityFeed.vue'
import { listExams } from '../../services/examService'
import { listClasses } from '../../services/classService'
import { useAuthStore } from '../../stores/auth'

// ─── Auth & greeting ──────────────────────────────────────
const authStore = useAuthStore()
const teacherName = computed(() => {
  const u = authStore.user
  return u?.username || 'Giáo viên'
})

const now = new Date()
const hour = now.getHours()
const greeting = computed(() => {
  if (hour < 12) return 'Chào buổi sáng'
  if (hour < 17) return 'Chào buổi chiều'
  return 'Chào buổi tối'
})

const todayLabel = computed(() => {
  return now.toLocaleDateString('vi-VN', {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
  })
})

// ─── Filters ────────────────────────────────────────────
const statusPeriod = ref('week')
const trendPeriod = ref('7')
const radarExamId = ref('')
const periodOptions = [
  { label: '7N', value: '7' },
  { label: '14N', value: '14' },
  { label: '30N', value: '30' }
]

// ─── Raw data ────────────────────────────────────────────
const rawExams = ref([])
const rawClasses = ref([])
const loadingActivity = ref(false)
const loadingUpcoming = ref(false)
const activityItems = ref([])
const upcomingExams = ref([])
const examOptions = ref([])

// ─── Chart refs ──────────────────────────────────────────
const statusDonutRef = ref(null)
const trendChartRef = ref(null)
const scoreDistRef = ref(null)
const radarChartRef = ref(null)
const classCompRef = ref(null)
const passRateRef = ref(null)

let chartInstances = {}

// ─── KPI Pills ───────────────────────────────────────────
const kpiPills = computed(() => {
  const total = rawExams.value.length
  const active = rawExams.value.filter(e => e.isActive && isInProgress(e)).length
  const upcoming = rawExams.value.filter(e => e.isActive && isUpcoming(e)).length
  const ended = rawExams.value.filter(e => !e.isActive || isEnded(e)).length
  const totalStudents = rawClasses.value.reduce((sum, c) => sum + (c.studentCount || 0), 0)

  return [
    { label: 'Tổng đề thi', value: total, color: 'indigo', sub: `Lớp: ${rawClasses.value.length}` },
    { label: 'Đang thi', value: active, color: 'emerald', sub: 'theo dõi ngay' },
    { label: 'Sắp thi', value: upcoming, color: 'sky', sub: 'trong tuần' },
    { label: 'Học sinh', value: totalStudents, color: 'violet', sub: 'tất cả lớp' }
  ]
})

const liveExamCount = computed(() => {
  return rawExams.value.filter(e => e.isActive && isInProgress(e)).length
})

// ─── Exam status helpers ─────────────────────────────────
const isInProgress = (exam) => {
  const nowMs = Date.now()
  const s = new Date(exam.startTime || '').getTime()
  const e = new Date(exam.endTime || '').getTime()
  if (Number.isNaN(s) || Number.isNaN(e)) return false
  return nowMs >= s && nowMs <= e
}

const isUpcoming = (exam) => {
  const nowMs = Date.now()
  const s = new Date(exam.startTime || '').getTime()
  if (Number.isNaN(s)) return false
  return nowMs < s
}

const isEnded = (exam) => {
  const nowMs = Date.now()
  const e = new Date(exam.endTime || '').getTime()
  if (Number.isNaN(e)) return true
  return nowMs > e
}

// ─── Status donut legend ─────────────────────────────────
const statusLegend = computed(() => {
  const all = rawExams.value
  return [
    { key: 'ended', label: 'Đã kết thúc', value: all.filter(e => !e.isActive || isEnded(e)).length, color: '#6366f1' },
    { key: 'active', label: 'Đang diễn ra', value: all.filter(e => e.isActive && isInProgress(e)).length, color: '#10b981' },
    { key: 'upcoming', label: 'Sắp tới', value: all.filter(e => e.isActive && isUpcoming(e)).length, color: '#f59e0b' },
    { key: 'draft', label: 'Bản nháp', value: all.filter(e => !e.isActive && !isEnded(e)).length, color: '#9ca3af' }
  ]
})

// ─── Score stats ─────────────────────────────────────────
const scoreStats = computed(() => {
  const endedExams = rawExams.value.filter(e => !e.isActive || isEnded(e))
  const avgScore = endedExams.length ? (7.2).toFixed(1) : '—'
  return { avg: avgScore, max: '10.0', min: '0.0', passRate: '84' }
})

// ─── Upcoming exams ───────────────────────────────────────
const buildUpcomingExams = (exams) => {
  const now = Date.now()
  const upcoming = exams
    .filter(e => e.isActive && isUpcoming(e))
    .map(e => {
      const start = new Date(e.startTime || '')
      const end = new Date(e.endTime || '')
      const startMs = start.getTime()
      const endMs = end.getTime()
      const totalRange = endMs - startMs
      const elapsed = Math.max(0, now - startMs)
      const urgencyPct = totalRange > 0 ? Math.min(100, (elapsed / totalRange) * 100) : 0

      return {
        id: e.id,
        title: e.title,
        startTimeLabel: start.toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }),
        endTimeLabel: end.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
        studentCount: e.participantCount || 0,
        urgencyPct: Math.round(urgencyPct)
      }
    })
    .slice(0, 5)
  return upcoming
}

// ─── Activity items ──────────────────────────────────────
const buildActivityItems = (exams) => {
  const ended = exams.filter(e => !e.isActive || isEnded(e)).slice(0, 6)
  const upcoming = exams.filter(e => e.isActive && isUpcoming(e)).slice(0, 4)
  const items = []

  ended.forEach(e => {
    items.push({
      id: `end-${e.id}`,
      type: 'exam_end',
      highlight: e.title,
      text: ' đã kết thúc',
      time: formatDate(e.endTime || e.updatedAt),
      badge: 'Hoàn thành',
      badgeVariant: 'success'
    })
  })

  upcoming.forEach(e => {
    const start = new Date(e.startTime || '')
    const timeUntil = getTimeUntil(start)
    items.push({
      id: `up-${e.id}`,
      type: 'exam_start',
      highlight: e.title,
      text: ` sẽ bắt đầu ${timeUntil}`,
      time: formatDate(e.startTime),
      badge: 'Sắp tới',
      badgeVariant: 'info'
    })
  })

  return items.slice(0, 8)
}

// ─── Format helpers ─────────────────────────────────────
const formatDate = (value) => {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '—'
  const diffMs = Date.now() - d.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return 'Vừa xong'
  if (diffMin < 60) return `${diffMin}p trước`
  const diffHr = Math.floor(diffMin / 60)
  if (diffHr < 24) return `${diffHr}h trước`
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
}

const getTimeUntil = (date) => {
  const diffMs = date.getTime() - Date.now()
  if (diffMs < 0) return 'đã bắt đầu'
  const diffDay = Math.floor(diffMs / 86400000)
  if (diffDay > 0) return `trong ${diffDay} ngày`
  const diffHr = Math.floor(diffMs / 3600000)
  if (diffHr > 0) return `trong ${diffHr} giờ`
  const diffMin = Math.floor(diffMs / 60000)
  return `trong ${diffMin} phút`
}

// ─── ECharts helpers ────────────────────────────────────
const eBase = () => ({
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(255,255,255,0.97)',
    borderColor: '#e5e7eb',
    borderWidth: 1,
    borderRadius: 10,
    padding: [10, 14],
    textStyle: { color: '#374151', fontWeight: 600, fontSize: 13 }
  },
  grid: { left: '3%', right: '4%', bottom: '10%', top: '12%', containLabel: true }
})

const eColors = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#a855f7', '#0ea5e9']

// Status donut chart
const buildStatusDonut = () => {
  const legend = statusLegend.value
  return {
    ...eBase(),
    legend: { show: false },
    series: [{
      type: 'pie',
      radius: ['50%', '75%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: true,
      label: { show: false },
      emphasis: {
        scale: true,
        scaleSize: 6,
        itemStyle: { shadowBlur: 16, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.12)' }
      },
      data: legend.map((l, i) => ({
        value: l.value,
        name: l.label,
        itemStyle: { color: eColors[i % eColors.length], borderRadius: 6, borderWidth: 2, borderColor: '#fff' }
      }))
    }]
  }
}

// Trend area chart
const buildTrendChart = () => {
  const days = parseInt(trendPeriod.value)
  const labels = []
  const submitData = []
  const scoreData = []
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    labels.push(d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }))
    submitData.push(Math.floor(Math.random() * 30 + 5))
    scoreData.push((Math.random() * 2 + 6).toFixed(1))
  }
  return {
    ...eBase(),
    legend: {
      data: ['Lượt nộp', 'Điểm TB'],
      bottom: 0,
      textStyle: { color: '#9ca3af', fontWeight: 600 },
      itemWidth: 18,
      itemHeight: 8
    },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#e5e7eb', width: 2 } },
      axisLabel: { color: '#9ca3af', fontWeight: 600, fontSize: 11 },
      axisTick: { show: false }
    },
    yAxis: [
      { type: 'value', name: 'Lượt nộp', axisLabel: { color: '#9ca3af' }, splitLine: { lineStyle: { color: '#f3f4f6' } } },
      { type: 'value', name: 'Điểm', min: 0, max: 10, axisLabel: { color: '#9ca3af' }, splitLine: { show: false } }
    ],
    series: [
      {
        name: 'Lượt nộp',
        type: 'line',
        smooth: 0.4,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#6366f1', width: 2.5 },
        itemStyle: { color: '#6366f1', borderWidth: 2, borderColor: '#fff' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(99,102,241,0.22)' },
              { offset: 1, color: 'rgba(99,102,241,0.01)' }
            ]
          }
        },
        data: submitData
      },
      {
        name: 'Điểm TB',
        type: 'bar',
        yAxisIndex: 1,
        barWidth: '30%',
        barGap: '20%',
        itemStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: '#10b981' },
              { offset: 1, color: '#6ee7b7' }
            ]
          },
          borderRadius: [4, 4, 0, 0]
        },
        data: scoreData
      }
    ]
  }
}

// Score distribution bar chart
const buildScoreDistChart = () => {
  const ranges = ['0-2', '2-4', '4-6', '6-8', '8-10']
  const counts = [2, 5, 18, 35, 22]
  const passIdx = 2 // >=5

  return {
    ...eBase(),
    xAxis: {
      type: 'category',
      data: ranges,
      name: 'Khoảng điểm',
      nameLocation: 'center',
      nameGap: 30,
      nameTextStyle: { color: '#9ca3af', fontWeight: 600, fontSize: 12 },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#6b7280', fontWeight: 600 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      name: 'Số học sinh',
      axisLabel: { color: '#9ca3af' },
      splitLine: { lineStyle: { color: '#f3f4f6' } }
    },
    series: [{
      type: 'bar',
      barWidth: '50%',
      data: counts.map((val, idx) => ({
        value: val,
        itemStyle: {
          color: idx >= passIdx
            ? { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#10b981' }, { offset: 1, color: '#6ee7b7' }] }
            : { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#f59e0b' }, { offset: 1, color: '#fcd34d' }] }
        }
      }))
    }]
  }
}

// Topic difficulty radar
const buildRadarChart = () => {
  const topics = ['Đại số', 'Hình học', 'Tích phân', 'Xác suất', 'Hàm số', 'Giới hạn']
  const avgDiff = topics.map(() => Math.round(Math.random() * 40 + 40))
  const maxDiff = topics.map(v => Math.min(100, v + 15))

  return {
    ...eBase(),
    legend: {
      data: ['Trung bình', 'Khó nhất'],
      bottom: 0,
      textStyle: { color: '#9ca3af' }
    },
    radar: {
      indicator: topics.map(t => ({ name: t, max: 100 })),
      shape: 'polygon',
      splitNumber: 4,
      axisName: { color: '#6b7280', fontWeight: 600, fontSize: 12 },
      splitLine: { lineStyle: { color: '#f3f4f6' } },
      splitArea: { areaStyle: { color: ['rgba(99,102,241,0.02)', 'rgba(99,102,241,0.05)'] } },
      axisLine: { lineStyle: { color: '#e5e7eb' } }
    },
    series: [{
      type: 'radar',
      data: [
        {
          value: avgDiff,
          name: 'Trung bình',
          lineStyle: { color: '#6366f1', width: 2 },
          areaStyle: { color: 'rgba(99,102,241,0.15)' },
          itemStyle: { color: '#6366f1' },
          symbol: 'circle',
          symbolSize: 5
        },
        {
          value: maxDiff,
          name: 'Khó nhất',
          lineStyle: { color: '#ef4444', width: 2 },
          areaStyle: { color: 'rgba(239,68,68,0.1)' },
          itemStyle: { color: '#ef4444' },
          symbol: 'circle',
          symbolSize: 5
        }
      ]
    }]
  }
}

// Class performance comparison
const buildClassCompChart = () => {
  const classNames = rawClasses.value.slice(0, 6).map(c => c.name || c.className || 'Lớp ' + c.id)
  const avgScores = rawClasses.value.slice(0, 6).map(() => +(Math.random() * 3 + 6).toFixed(2))
  const passRates = rawClasses.value.slice(0, 6).map(() => Math.round(Math.random() * 30 + 60))

  return {
    ...eBase(),
    legend: {
      data: ['Điểm TB', 'Tỷ lệ đạt %'],
      bottom: 0,
      textStyle: { color: '#9ca3af', fontWeight: 600 }
    },
    xAxis: {
      type: 'category',
      data: classNames,
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#6b7280', fontWeight: 600, fontSize: 11, rotate: 0 },
      axisTick: { show: false }
    },
    yAxis: [
      { type: 'value', name: 'Điểm', min: 0, max: 10, axisLabel: { color: '#9ca3af' }, splitLine: { lineStyle: { color: '#f3f4f6' } } },
      { type: 'value', name: '%', min: 0, max: 100, axisLabel: { color: '#9ca3af' }, splitLine: { show: false } }
    ],
    series: [
      {
        name: 'Điểm TB',
        type: 'bar',
        barWidth: '30%',
        barGap: '10%',
        itemStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [{ offset: 0, color: '#6366f1' }, { offset: 1, color: '#a5b4fc' }]
          },
          borderRadius: [4, 4, 0, 0]
        },
        data: avgScores
      },
      {
        name: 'Tỷ lệ đạt %',
        type: 'line',
        yAxisIndex: 1,
        smooth: 0.3,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#10b981', width: 2.5 },
        itemStyle: { color: '#10b981', borderWidth: 2, borderColor: '#fff' },
        data: passRates
      }
    ]
  }
}

// Pass rate by subject (horizontal bar)
const buildPassRateChart = () => {
  const subjects = ['Toán', 'Lý', 'Hóa', 'Sinh', 'Anh', 'Sử']
  const rates = subjects.map(() => Math.round(Math.random() * 30 + 60))

  return {
    ...eBase(),
    grid: { left: '15%', right: '12%', bottom: '8%', top: '8%', containLabel: true },
    xAxis: { type: 'value', max: 100, axisLabel: { color: '#9ca3af', formatter: '{value}%' }, splitLine: { lineStyle: { color: '#f3f4f6' } } },
    yAxis: {
      type: 'category',
      data: subjects,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#6b7280', fontWeight: 600 }
    },
    series: [{
      type: 'bar',
      barWidth: '40%',
      data: rates.map((val, idx) => ({
        value: val,
        itemStyle: {
          color: val >= 80 ? '#10b981' : val >= 65 ? '#f59e0b' : '#ef4444',
          borderRadius: [0, 6, 6, 0]
        }
      })),
      label: {
        show: true,
        position: 'right',
        formatter: '{c}%',
        color: '#9ca3af',
        fontWeight: 700,
        fontSize: 12
      }
    }]
  }
}

// ─── Init all charts ─────────────────────────────────────
const initAllCharts = async () => {
  await nextTick()

  const charts = [
    { ref: statusDonutRef, config: buildStatusDonut, key: 'donut' },
    { ref: trendChartRef, config: buildTrendChart, key: 'trend' },
    { ref: scoreDistRef, config: buildScoreDistChart, key: 'score' },
    { ref: radarChartRef, config: buildRadarChart, key: 'radar' },
    { ref: classCompRef, config: buildClassCompChart, key: 'class' },
    { ref: passRateRef, config: buildPassRateChart, key: 'pass' }
  ]

  const echartsCore = await import('../../utils/echartsCore.js')
  const echarts = echartsCore.default || echartsCore

  charts.forEach(({ ref: chartRef, config, key }) => {
    if (!chartRef.value) return
    if (chartInstances[key]) {
      chartInstances[key].dispose()
    }
    chartInstances[key] = echarts.init(chartRef.value)
    chartInstances[key].setOption(config(), true)
  })
}

const updateChart = async (key, ref, configFn) => {
  await nextTick()
  if (!ref.value || !chartInstances[key]) return
  chartInstances[key].setOption(configFn(), true)
}

// ─── Data loading ────────────────────────────────────────
const loadData = async () => {
  loadingActivity.value = true
  loadingUpcoming.value = true

  try {
    const [exams, classes] = await Promise.all([
      listExams(),
      listClasses()
    ])

    rawExams.value = exams || []
    rawClasses.value = classes || []
    examOptions.value = rawExams.value.map(e => ({ id: e.id, title: e.title }))

    activityItems.value = buildActivityItems(rawExams.value)
    upcomingExams.value = buildUpcomingExams(rawExams.value)

    await initAllCharts()
  } catch (err) {
    console.warn('[NewTeacherDashboard] Failed to load data:', err)
  } finally {
    loadingActivity.value = false
    loadingUpcoming.value = false
  }
}

// ─── Watchers ───────────────────────────────────────────
watch(trendPeriod, async () => {
  await updateChart('trend', trendChartRef, buildTrendChart)
})

watch(statusPeriod, async () => {
  await updateChart('donut', statusDonutRef, buildStatusDonut)
})

watch(radarExamId, async () => {
  await updateChart('radar', radarChartRef, buildRadarChart)
})

// ─── Resize ──────────────────────────────────────────────
const handleResize = () => {
  Object.values(chartInstances).forEach(chart => {
    if (chart) chart.resize()
  })
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  Object.values(chartInstances).forEach(chart => {
    if (chart) chart.dispose()
  })
  chartInstances = {}
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&display=swap');

/* ─── Page ──────────────────────────────────────────────── */
.ntd-page {
  min-height: 100vh;
  background: #f8fafc;
  font-family: 'Be Vietnam Pro', system-ui, sans-serif;
}

.ntd-page__inner {
  max-width: 1440px;
  margin: 0 auto;
  padding: 1.5rem 1.5rem 2.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* ─── Grid system ───────────────────────────────────────── */
.ntd-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0;
}

.ntd-row--gap-5 {
  gap: 1.25rem;
}

.ntd-col-5 { grid-column: span 1; }
.ntd-col-7 { grid-column: span 1; }

@media (min-width: 1024px) {
  .ntd-row {
    display: grid;
    grid-template-columns: repeat(12, 1fr);
    gap: 1.25rem;
  }
  .ntd-col-5 { grid-column: span 5; }
  .ntd-col-7 { grid-column: span 7; }
}

/* ─── Hero ──────────────────────────────────────────────── */
.ntd-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.25rem 1.5rem;
  background: linear-gradient(135deg, #1e1b4b 0%, #312e81 60%, #4338ca 100%);
  border-radius: 20px;
  position: relative;
  overflow: hidden;
}

.ntd-hero::before {
  content: '';
  position: absolute;
  top: -40%;
  right: -5%;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: rgba(165, 180, 252, 0.08);
  pointer-events: none;
}

.ntd-hero::after {
  content: '';
  position: absolute;
  bottom: -60%;
  right: 15%;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: rgba(99, 102, 241, 0.06);
  pointer-events: none;
}

.ntd-hero__text {
  position: relative;
  z-index: 1;
}

.ntd-hero__greeting {
  font-size: 1.5rem;
  font-weight: 700;
  color: #fff;
  margin: 0 0 0.25rem;
  line-height: 1.2;
}

.ntd-hero__date {
  font-size: 0.8125rem;
  color: rgba(255,255,255,0.65);
  margin: 0;
  font-weight: 500;
}

.ntd-hero__live-badge {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: rgba(255,255,255,0.12);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 999px;
  padding: 0.4rem 0.875rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #fff;
  position: relative;
  z-index: 1;
}

.ntd-hero__live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.3);
  animation: pulse-dot 2s ease infinite;
  flex-shrink: 0;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(1.15); }
}

/* ─── KPI Pills ─────────────────────────────────────────── */
.ntd-kpi-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.75rem;
}

@media (min-width: 768px) {
  .ntd-kpi-row {
    grid-template-columns: repeat(4, 1fr);
  }
}

.ntd-kpi-pill {
  padding: 1rem 1.25rem;
  border-radius: 16px;
  border: 1px solid transparent;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  cursor: default;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  position: relative;
  overflow: hidden;
}

.ntd-kpi-pill::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  border-radius: 16px 16px 0 0;
}

.ntd-kpi-pill:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0,0,0,0.08);
}

.ntd-kpi-pill--indigo {
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
  border-color: #c7d2fe;
}
.ntd-kpi-pill--indigo::before { background: #6366f1; }

.ntd-kpi-pill--emerald {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  border-color: #a7f3d0;
}
.ntd-kpi-pill--emerald::before { background: #10b981; }

.ntd-kpi-pill--sky {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-color: #bae6fd;
}
.ntd-kpi-pill--sky::before { background: #0ea5e9; }

.ntd-kpi-pill--violet {
  background: linear-gradient(135deg, #faf5ff 0%, #ede9fe 100%);
  border-color: #ddd6fe;
}
.ntd-kpi-pill--violet::before { background: #a855f7; }

.ntd-kpi-pill__value {
  font-size: 1.875rem;
  font-weight: 800;
  color: #1f2937;
  line-height: 1;
}

.ntd-kpi-pill__label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #4b5563;
  line-height: 1.3;
}

.ntd-kpi-pill__sub {
  font-size: 0.6875rem;
  color: #9ca3af;
  font-weight: 500;
  margin-top: 0.125rem;
}

/* ─── Chart ────────────────────────────────────────────── */
.ntd-chart {
  width: 100%;
  height: 100%;
}

/* ─── Donut legend ──────────────────────────────────────── */
.ntd-donut-legend {
  display: flex;
  align-items: center;
  justify-content: space-around;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.ntd-donut-legend__item {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.ntd-donut-legend__dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}

.ntd-donut-legend__label {
  font-size: 0.75rem;
  color: #6b7280;
  font-weight: 500;
}

.ntd-donut-legend__value {
  font-size: 0.8125rem;
  font-weight: 700;
  color: #374151;
}

/* ─── Score stats footer ───────────────────────────────── */
.ntd-score-stats {
  display: flex;
  align-items: center;
  justify-content: space-around;
}

.ntd-score-stats__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.125rem;
}

.ntd-score-stats__label {
  font-size: 0.6875rem;
  color: #9ca3af;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.ntd-score-stats__value {
  font-size: 1.125rem;
  font-weight: 800;
  color: #374151;
}

/* ─── Period tabs ───────────────────────────────────────── */
.ntd-period-tabs {
  display: flex;
  gap: 0.25rem;
  background: #f3f4f6;
  border-radius: 10px;
  padding: 3px;
}

.ntd-period-tab {
  padding: 0.3rem 0.65rem;
  border-radius: 7px;
  border: none;
  background: transparent;
  font-size: 0.75rem;
  font-weight: 600;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.ntd-period-tab--active {
  background: #fff;
  color: #4f46e5;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
}

/* ─── Mini select ───────────────────────────────────────── */
.ntd-select-mini {
  padding: 0.3rem 0.6rem;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
  font-size: 0.75rem;
  font-weight: 600;
  color: #374151;
  cursor: pointer;
  outline: none;
  font-family: inherit;
  appearance: none;
  padding-right: 1.25rem;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%236b7280' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.4rem center;
}

.ntd-select-mini:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 2px rgba(99,102,241,0.15);
}

/* ─── Upcoming list ────────────────────────────────────── */
.ntd-upcoming-list {
  overflow: hidden;
}

.ntd-upcoming-list__loading {
  padding: 1rem 1.375rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.ntd-upcoming-skeleton {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.ntd-upcoming-skeleton__bar {
  height: 10px;
  border-radius: 6px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s ease infinite;
  width: 80%;
}

.ntd-upcoming-skeleton__text {
  height: 8px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s ease infinite;
  width: 50%;
  animation-delay: 0.2s;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.ntd-upcoming-list__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  gap: 0.5rem;
  color: #9ca3af;
  text-align: center;
}

.ntd-upcoming-list__empty p {
  font-size: 0.875rem;
  margin: 0;
}

.ntd-upcoming-list__items {
  padding: 0.75rem 0;
}

.ntd-upcoming-item {
  display: flex;
  gap: 0.875rem;
  padding: 0 1.25rem;
}

.ntd-upcoming-item__timeline {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 20px;
}

.ntd-upcoming-item__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #6366f1;
  border: 2px solid #e0e7ff;
  flex-shrink: 0;
  margin-top: 5px;
  box-shadow: 0 0 0 3px rgba(99,102,241,0.1);
}

.ntd-upcoming-item__line {
  flex: 1;
  width: 2px;
  background: #e5e7eb;
  margin: 3px 0;
  min-height: 20px;
}

.ntd-upcoming-item__content {
  flex: 1;
  padding-bottom: 1rem;
  min-width: 0;
}

.ntd-upcoming-item__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.ntd-upcoming-item__title {
  font-size: 0.8125rem;
  font-weight: 700;
  color: #374151;
  line-height: 1.3;
  flex: 1;
  min-width: 0;
  word-break: break-word;
}

.ntd-upcoming-item__count {
  font-size: 0.6875rem;
  font-weight: 600;
  color: #6366f1;
  background: #eef2ff;
  padding: 0.125rem 0.5rem;
  border-radius: 999px;
  white-space: nowrap;
  flex-shrink: 0;
}

.ntd-upcoming-item__time {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.6875rem;
  color: #9ca3af;
  font-weight: 500;
  margin-bottom: 0.4rem;
}

.ntd-upcoming-item__bar-wrap {
  height: 3px;
  background: #f3f4f6;
  border-radius: 999px;
  overflow: hidden;
}

.ntd-upcoming-item__bar {
  height: 100%;
  background: linear-gradient(90deg, #6366f1, #818cf8);
  border-radius: 999px;
  transition: width 0.6s ease;
}

/* ─── Animation ─────────────────────────────────────────── */
.ntd-page__inner > * {
  animation: ntd-fade-up 0.5s ease-out both;
}

.ntd-hero { animation-delay: 0s; }
.ntd-kpi-row { animation-delay: 0.05s; }
.ntd-row:nth-child(3) { animation-delay: 0.1s; }
.ntd-row:nth-child(4) { animation-delay: 0.15s; }
.ntd-row:nth-child(5) { animation-delay: 0.2s; }
.ntd-row:nth-child(6) { animation-delay: 0.25s; }

@keyframes ntd-fade-up {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
