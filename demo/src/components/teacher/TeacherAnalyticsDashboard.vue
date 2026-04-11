<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto max-w-7xl px-4 pb-10 pt-4 sm:px-6 lg:px-8">

      <!-- 1. Hero with greeting + date + key metrics -->
      <div class="mb-6 ds-animate-fade-up">
        <PageHeader
          :title="`Xin chào, ${teacherName}`"
          :subtitle="currentDate"
        >
          <template #actions>
            <button
              type="button"
              class="inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-bold transition-all hover:-translate-y-0.5"
              style="background-color: var(--ds-primary); color: white; box-shadow: var(--ds-shadow-sm)"
              @click="refreshData"
            >
              <LucideIcon name="refresh" size="18" />
              <span>Làm mới</span>
            </button>
          </template>
        </PageHeader>
      </div>

      <!-- 2. KPI Grid -->
      <div class="mb-6 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 ds-animate-fade-up" style="animation-delay: 0.05s">
        <DsStatCard
          v-for="kpi in kpiStats"
          :key="kpi.label"
          :label="kpi.label"
          :value="kpi.value"
          :sub-value="kpi.subValue"
          :sub="kpi.sub"
          :icon="kpi.icon"
          :badge="kpi.badge"
          :badge-variant="kpi.badgeVariant"
          :trend="kpi.trend"
        />
      </div>

      <!-- 3. Exam Performance Chart -->
      <div class="mb-6 ds-animate-fade-up" style="animation-delay: 0.1s">
        <DsCard padding="lg">
          <template #header>
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <LucideIcon name="bar_chart" />
                <h3 class="text-lg font-bold" style="color: var(--ds-text)">Xu hướng điểm thi</h3>
              </div>
              <div class="flex items-center gap-2">
                <select
                  v-model="selectedPeriod"
                  class="rounded-lg border px-3 py-1.5 text-sm"
                  style="border-color: var(--ds-border); background-color: var(--ds-surface); color: var(--ds-text)"
                >
                  <option value="7">7 ngày qua</option>
                  <option value="30">30 ngày qua</option>
                  <option value="90">90 ngày qua</option>
                </select>
              </div>
            </div>
          </template>
          <div ref="performanceChartRef" class="h-80 w-full"></div>
        </DsCard>
      </div>

      <!-- 4. Recent Activity Table -->
      <div class="mb-6 ds-animate-fade-up" style="animation-delay: 0.15s">
        <DsCard padding="none">
          <template #header>
            <div class="flex items-center justify-between px-5 pt-5">
              <div class="flex items-center gap-2">
                <LucideIcon name="history" />
                <h3 class="text-lg font-bold" style="color: var(--ds-text)">Hoạt động gần đây</h3>
              </div>
              <RouterLink
                to="/teacher/exams/list"
                class="text-sm font-semibold transition-colors hover:underline"
                style="color: var(--ds-primary)"
              >
                Xem tất cả
              </RouterLink>
            </div>
          </template>
          <DataTable
            :columns="activityColumns"
            :data="recentActivity"
            :row-key="'id'"
            :loading="isLoading"
            :loading-text="'Đang tải hoạt động...'"
            :empty-text="'Chưa có hoạt động nào'"
          >
            <template #cell-exam="{ row }">
              <div class="flex items-center gap-3">
                <div
                  class="flex size-9 items-center justify-center rounded-lg"
                  :style="{ backgroundColor: getExamTypeColor(row.type) + '20', color: getExamTypeColor(row.type) }"
                >
                  <LucideIcon :name="getExamTypeIcon(row.type)" size="18" />
                </div>
                <div>
                  <p class="text-sm font-semibold" style="color: var(--ds-text)">{{ row.examTitle }}</p>
                  <p class="text-xs" style="color: var(--ds-text-muted)">{{ row.studentName }}</p>
                </div>
              </div>
            </template>
            <template #cell-score="{ value }">
              <span class="text-sm font-bold" :style="{ color: getScoreColor(value) }">{{ value }}</span>
            </template>
            <template #cell-status="{ row }">
              <StatusChip :status="row.statusKey" :label="row.status" />
            </template>
          </DataTable>
        </DsCard>
      </div>

      <!-- 5. Quick Actions + Upcoming Exams -->
      <div class="grid grid-cols-1 gap-6 lg:grid-cols-[1fr_1fr] ds-animate-fade-up" style="animation-delay: 0.2s">
        <!-- Quick Actions -->
        <DsCard padding="lg">
          <template #header>
            <div class="flex items-center gap-2">
              <LucideIcon name="bolt" />
              <h3 class="text-lg font-bold" style="color: var(--ds-text)">Thao tác nhanh</h3>
            </div>
          </template>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <ActionCard
              v-for="action in quickActions"
              :key="action.title"
              :icon="action.icon"
              :title="action.title"
              :description="action.description"
              :to="action.to"
            />
          </div>
        </DsCard>

        <!-- Upcoming Exams -->
        <DsCard padding="lg">
          <template #header>
            <div class="flex items-center gap-2">
              <LucideIcon name="schedule" />
              <h3 class="text-lg font-bold" style="color: var(--ds-text)">Kỳ thi sắp tới</h3>
            </div>
          </template>
          <div v-if="upcomingExams.length === 0" class="flex flex-col items-center justify-center py-8 text-center">
            <LucideIcon name="event_available" size="36" />
            <p class="mt-2 text-sm" style="color: var(--ds-text-muted)">Không có kỳ thi nào sắp tới</p>
          </div>
          <div v-else class="space-y-3">
            <div
              v-for="exam in upcomingExams"
              :key="exam.id"
              class="flex items-center justify-between rounded-lg border p-3 transition-colors hover:border-[var(--ds-primary-border)]"
              style="border-color: var(--ds-border); background-color: var(--ds-gray-50)"
            >
              <div class="flex items-center gap-3">
                <div
                  class="flex size-10 items-center justify-center rounded-lg"
                  style="background-color: var(--ds-info-bg); color: var(--ds-info)"
                >
                  <LucideIcon name="assignment" size="18" />
                </div>
                <div>
                  <p class="text-sm font-semibold" style="color: var(--ds-text)">{{ exam.title }}</p>
                  <p class="text-xs" style="color: var(--ds-text-muted)">{{ exam.date }}</p>
                </div>
              </div>
              <div class="text-right">
                <p class="text-xs font-medium" style="color: var(--ds-text-muted)">{{ exam.students }} học sinh</p>
                <p class="text-xs" style="color: var(--ds-info)">{{ exam.timeUntil }}</p>
              </div>
            </div>
          </div>
        </DsCard>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError } from '../../services/apiClient'
import { listExams } from '../../services/examService'
import { useToast } from '../../composables/useToast'
import PageHeader from '../ui/PageHeader.vue'
import DsStatCard from '../ui/DsStatCard.vue'
import DsCard from '../ui/DsCard.vue'
import DataTable from '../ui/DataTable.vue'
import StatusChip from '../ui/StatusChip.vue'
import ActionCard from '../ui/ActionCard.vue'

const toast = useToast()
const teacherName = ref('Giáo viên')
const isLoading = ref(false)
const selectedPeriod = ref('7')
const rawExams = ref([])

const currentDate = computed(() => {
  return new Date().toLocaleDateString('vi-VN', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
})

const getExamStatusMeta = (exam) => {
  if (!exam?.isActive) return { key: 'draft', label: 'Bản nháp' }
  const now = Date.now()
  const start = new Date(exam.startTime || '').getTime()
  const end = new Date(exam.endTime || '').getTime()
  if (!Number.isNaN(start) && now < start) return { key: 'upcoming', label: 'Sắp diễn ra' }
  if (!Number.isNaN(start) && !Number.isNaN(end) && now >= start && now <= end) {
    return { key: 'live', label: 'Đang diễn ra' }
  }
  return { key: 'ended', label: 'Đã kết thúc' }
}

const statusCounts = computed(() =>
  rawExams.value.reduce((acc, exam) => {
    const { key } = getExamStatusMeta(exam)
    acc[key] = (acc[key] || 0) + 1
    return acc
  }, { draft: 0, upcoming: 0, live: 0, ended: 0 })
)

const kpiStats = computed(() => [
  {
    label: 'Tổng số đề thi',
    value: String(rawExams.value.length),
    subValue: 'đề',
    sub: `${statusCounts.value.draft} bản nháp`,
    icon: 'assignment',
    badge: 'Tổng quan',
    badgeVariant: 'info'
  },
  {
    label: 'Đang diễn ra',
    value: String(statusCounts.value.live),
    subValue: 'đề',
    sub: 'Có thể mở giám sát ngay',
    icon: 'live_tv',
    badge: statusCounts.value.live > 0 ? 'Cần theo dõi' : 'Ổn định',
    badgeVariant: statusCounts.value.live > 0 ? 'warning' : 'success'
  },
  {
    label: 'Sắp diễn ra',
    value: String(statusCounts.value.upcoming),
    subValue: 'đề',
    sub: 'Cần kiểm tra lịch thi',
    icon: 'schedule'
  },
  {
    label: 'Đã kết thúc',
    value: String(statusCounts.value.ended),
    subValue: 'đề',
    sub: 'Sẵn sàng xem báo cáo',
    icon: 'check_circle'
  }
])

const quickActions = [
  {
    icon: 'add_circle',
    title: 'Tạo đề thi mới',
    description: 'Bắt đầu tạo một kỳ thi mới',
    to: '/teacher/exams/create'
  },
  {
    icon: 'monitor_heart',
    title: 'Giám sát thi',
    description: 'Theo dõi các kỳ thi đang diễn ra',
    to: '/teacher/live-monitoring'
  },
  {
    icon: 'list_alt',
    title: 'Danh sách đề thi',
    description: 'Chọn đề để xem phân tích kết quả',
    to: '/teacher/exams/list'
  },
  {
    icon: 'edit_square',
    title: 'Soạn đề mới',
    description: 'Quay lại workspace tạo và nhập đề',
    to: '/teacher/exams/build'
  }
]

const formatDateTime = (value) => {
  const date = new Date(value || '')
  return Number.isNaN(date.getTime())
    ? 'Chưa có lịch'
    : date.toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
}

const formatTimeUntil = (value) => {
  const start = new Date(value || '').getTime()
  if (Number.isNaN(start)) return 'Chưa xác định'
  const diff = start - Date.now()
  if (diff <= 0) return 'Đang diễn ra hoặc đã bắt đầu'
  const days = Math.floor(diff / (24 * 60 * 60 * 1000))
  if (days > 0) return `Còn ${days} ngày`
  const hours = Math.max(1, Math.floor(diff / (60 * 60 * 1000)))
  return `Còn ${hours} giờ`
}

const upcomingExams = computed(() => rawExams.value
  .filter((exam) => getExamStatusMeta(exam).key === 'upcoming')
  .sort((a, b) => new Date(a.startTime || '').getTime() - new Date(b.startTime || '').getTime())
  .slice(0, 4)
  .map((exam) => ({
    id: exam.id,
    title: exam.title || 'Đề thi',
    date: formatDateTime(exam.startTime),
    students: exam.participantCount || 0,
    timeUntil: formatTimeUntil(exam.startTime)
  })))

const activityColumns = [
  { key: 'exam', label: 'Kỳ thi' },
  { key: 'score', label: 'Số câu', align: 'center' },
  { key: 'time', label: 'Bắt đầu', align: 'center' },
  { key: 'status', label: 'Trạng thái', align: 'center' }
]

const recentActivity = computed(() => rawExams.value
  .slice()
  .sort((a, b) => new Date(b.updatedAt || b.startTime || b.createdAt || 0).getTime() - new Date(a.updatedAt || a.startTime || a.createdAt || 0).getTime())
  .slice(0, 6)
  .map((exam) => {
    const status = getExamStatusMeta(exam)
    return {
      id: exam.id,
      type: 'exam',
      examTitle: exam.title || 'Đề thi',
      studentName: exam.className || exam.subject || 'Đề thi tự do',
      score: String(exam.questionCount || 0),
      time: formatDateTime(exam.startTime),
      status: status.label,
      statusKey: status.key === 'live' ? 'warning' : (status.key === 'ended' ? 'active' : 'info')
    }
  }))

// Methods
const getExamTypeColor = (type) => {
  const colors = {
    exam: 'var(--ds-primary)',
    violation: 'var(--ds-danger)',
    quiz: 'var(--ds-success)'
  }
  return colors[type] || 'var(--ds-gray-500)'
}

const getExamTypeIcon = (type) => {
  const icons = {
    exam: 'assignment',
    violation: 'warning',
    quiz: 'quiz'
  }
  return icons[type] || 'assignment'
}

const getScoreColor = (score) => {
  const num = parseFloat(score)
  if (num >= 40) return 'var(--ds-success)'
  if (num >= 20) return 'var(--ds-warning)'
  return 'var(--ds-text)'
}

const refreshData = async () => {
  isLoading.value = true
  try {
    rawExams.value = await listExams()
  } catch (error) {
    toast.error(error instanceof ApiError ? error.message : 'Không thể tải dữ liệu phân tích.')
  } finally {
    isLoading.value = false
  }
}

// Chart setup
const performanceChartRef = ref(null)
let performanceChart = null
let performanceResizeObserver = null
let echartsLibPromise = null

const getChartPixelRatio = () => {
  if (typeof window === 'undefined') return 1
  return Math.min(Math.max(window.devicePixelRatio || 1, 1), 2)
}

const getEcharts = async () => {
  if (!echartsLibPromise) {
    echartsLibPromise = import('../../utils/echartsCore.js').then((mod) => mod.default)
  }
  return echartsLibPromise
}

const chartData = computed(() => {
  const days = Number.parseInt(selectedPeriod.value, 10) || 7
  const bucketCount = days <= 7 ? 7 : 6
  const bucketSpan = Math.ceil(days / bucketCount)
  const end = new Date()
  end.setHours(23, 59, 59, 999)
  const buckets = Array.from({ length: bucketCount }, (_, index) => {
    const bucketEnd = new Date(end)
    bucketEnd.setDate(end.getDate() - ((bucketCount - 1 - index) * bucketSpan))
    const bucketStart = new Date(bucketEnd)
    bucketStart.setDate(bucketEnd.getDate() - bucketSpan + 1)
    bucketStart.setHours(0, 0, 0, 0)
    return {
      start: bucketStart,
      end: bucketEnd,
      label: bucketSpan === 1
        ? bucketStart.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
        : `${bucketStart.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })}-${bucketEnd.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })}`,
      started: 0,
      ended: 0
    }
  })

  for (const exam of rawExams.value) {
    const startMs = new Date(exam.startTime || exam.createdAt || '').getTime()
    const endMs = new Date(exam.endTime || '').getTime()
    for (const bucket of buckets) {
      if (!Number.isNaN(startMs) && startMs >= bucket.start.getTime() && startMs <= bucket.end.getTime()) {
        bucket.started += 1
      }
      if (!Number.isNaN(endMs) && endMs >= bucket.start.getTime() && endMs <= bucket.end.getTime()) {
        bucket.ended += 1
      }
    }
  }

  return {
    labels: buckets.map((bucket) => bucket.label),
    started: buckets.map((bucket) => bucket.started),
    ended: buckets.map((bucket) => bucket.ended)
  }
})

const initPerformanceChart = async () => {
  if (!performanceChartRef.value) return
  const echarts = await getEcharts()

  if (!performanceChart) {
    performanceChart = echarts.init(performanceChartRef.value, null, {
      renderer: 'canvas',
      devicePixelRatio: getChartPixelRatio()
    })
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: 'var(--ds-border)',
      borderWidth: 1,
      borderRadius: 12,
      padding: [12, 16],
      textStyle: { 
        color: 'var(--ds-text)',
        fontWeight: 600,
        fontSize: 13
      },
      extraCssText: 'box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);'
    },
    legend: {
      data: ['Đề bắt đầu', 'Đề kết thúc'],
      bottom: 0,
      textStyle: { 
        color: 'var(--ds-text-muted)',
        fontWeight: 600
      },
      itemWidth: 20,
      itemHeight: 10
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: chartData.value.labels,
      axisLine: { lineStyle: { color: 'var(--ds-border)', width: 2 } },
      axisLabel: { 
        color: 'var(--ds-text-muted)',
        fontWeight: 600
      },
      axisTick: { show: false }
    },
    yAxis: [
      {
        type: 'value',
        name: 'Số đề',
        min: 0,
        axisLine: { lineStyle: { color: 'var(--ds-border)' } },
        axisLabel: { color: 'var(--ds-text-muted)', fontWeight: 600 },
        splitLine: { lineStyle: { color: 'var(--ds-border)', type: 'dashed', opacity: 0.5 } }
      },
      {
        type: 'value',
        name: 'Số đề',
        min: 0,
        axisLine: { lineStyle: { color: 'var(--ds-border)' } },
        axisLabel: { color: 'var(--ds-text-muted)', fontWeight: 600 },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: 'Đề bắt đầu',
        type: 'bar',
        barWidth: '35%',
        animationDuration: 1500,
        animationEasing: 'elasticOut',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#6366f1' },
            { offset: 1, color: '#a5b4fc' }
          ]),
          borderRadius: [6, 6, 0, 0],
          shadowColor: 'rgba(99, 102, 241, 0.3)',
          shadowBlur: 10
        },
        emphasis: {
          itemStyle: {
            shadowColor: 'rgba(99, 102, 241, 0.5)',
            shadowBlur: 15
          }
        },
        data: chartData.value.started
      },
      {
        name: 'Đề kết thúc',
        type: 'line',
        yAxisIndex: 1,
        smooth: 0.4,
        symbol: 'circle',
        symbolSize: 8,
        animationDuration: 2000,
        animationEasing: 'elasticOut',
        lineStyle: { 
          color: '#10b981', 
          width: 3,
          shadowColor: 'rgba(16, 185, 129, 0.3)',
          shadowBlur: 8
        },
        itemStyle: { 
          color: '#10b981',
          borderWidth: 2,
          borderColor: '#fff'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(16, 185, 129, 0.3)' },
            { offset: 1, color: 'rgba(16, 185, 129, 0.02)' }
          ])
        },
        emphasis: {
          scale: true,
          itemStyle: {
            shadowColor: 'rgba(16, 185, 129, 0.5)',
            shadowBlur: 10
          }
        },
        data: chartData.value.ended
      }
    ]
  }

  performanceChart.setOption(option, true)
  performanceChart.resize()
}

onMounted(async () => {
  await refreshData()
  nextTick(() => {
    void initPerformanceChart()

    if (typeof ResizeObserver !== 'undefined' && performanceChartRef.value) {
      performanceResizeObserver = new ResizeObserver(() => {
        performanceChart?.resize()
      })
      performanceResizeObserver.observe(performanceChartRef.value)
    }
  })

  window.addEventListener('resize', handlePerformanceResize, { passive: true })
})

watch([selectedPeriod, rawExams], () => {
  void initPerformanceChart()
})

const handlePerformanceResize = () => {
  performanceChart?.resize()
}

onUnmounted(() => {
  window.removeEventListener('resize', handlePerformanceResize)
  performanceResizeObserver?.disconnect()
  performanceResizeObserver = null
  performanceChart?.dispose()
  performanceChart = null
})
</script>


<style scoped>
.ds-animate-fade-up {
  animation: fadeUp 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes fadeUp {
  from { opacity: 0; }
  to   { opacity: 1; }
}

@media (prefers-reduced-motion: reduce) {
  .ds-animate-fade-up { animation: none; }
}
</style>
