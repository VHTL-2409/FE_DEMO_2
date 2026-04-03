<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto max-w-7xl px-4 pb-10 pt-4 sm:px-6 lg:px-8">

      <!-- 1. Hero with greeting + date + key metrics -->
      <div class="mb-6 ds-animate-fade-up">
        <PageHeader
          eyebrow="Bảng điều khiển"
          title="Xin chào, {{ teacherName }}"
          subtitle="{{ currentDate }}"
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
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { RouterLink } from 'vue-router'
import * as echarts from 'echarts'
import PageHeader from '../ui/PageHeader.vue'
import DsStatCard from '../ui/DsStatCard.vue'
import DsCard from '../ui/DsCard.vue'
import DataTable from '../ui/DataTable.vue'
import StatusChip from '../ui/StatusChip.vue'
import ActionCard from '../ui/ActionCard.vue'

// Mock data
const teacherName = ref('Nguyễn Văn A')
const isLoading = ref(false)
const selectedPeriod = ref('7')

const currentDate = computed(() => {
  return new Date().toLocaleDateString('vi-VN', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
})

const kpiStats = computed(() => [
  {
    label: 'Tổng số đề thi',
    value: '24',
    subValue: 'đề',
    sub: '8 đang hoạt động',
    icon: 'assignment',
    badge: 'Đang hoạt động',
    badgeVariant: 'success',
    trend: 12
  },
  {
    label: 'Học sinh đang thi',
    value: '156',
    subValue: 'em',
    sub: 'Đang theo dõi',
    icon: 'school',
    badge: 'Đang thi',
    badgeVariant: 'warning',
    trend: 5
  },
  {
    label: 'Điểm trung bình',
    value: '7.2',
    subValue: '/10',
    sub: 'Cải thiện 8%',
    icon: 'trending_up',
    trend: 8
  },
  {
    label: 'Tỷ lệ hoàn thành',
    value: '94',
    subValue: '%',
    sub: 'Tháng này',
    icon: 'check_circle',
    trend: 3
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
    icon: 'analytics',
    title: 'Xem báo cáo',
    description: 'Phân tích kết quả học tập',
    to: '/teacher/exams'
  },
  {
    icon: 'quiz',
    title: 'Tạo câu hỏi',
    description: 'Thêm câu hỏi mới vào ngân hàng',
    to: '/teacher/questions'
  }
]

const upcomingExams = computed(() => [
  {
    id: 1,
    title: 'Giữa kỳ Toán cao cấp',
    date: '01/04/2026 09:00',
    students: 45,
    timeUntil: 'Còn 3 ngày'
  },
  {
    id: 2,
    title: 'Cuối kỳ Vật lý đại cương',
    date: '05/04/2026 14:00',
    students: 38,
    timeUntil: 'Còn 7 ngày'
  },
  {
    id: 3,
    title: 'Kiểm tra Tiếng Anh B1',
    date: '10/04/2026 08:00',
    students: 52,
    timeUntil: 'Còn 12 ngày'
  }
])

const activityColumns = [
  { key: 'exam', label: 'Kỳ thi' },
  { key: 'score', label: 'Điểm', align: 'center' },
  { key: 'time', label: 'Thời gian', align: 'center' },
  { key: 'status', label: 'Trạng thái', align: 'center' }
]

const recentActivity = computed(() => [
  {
    id: 1,
    type: 'exam',
    examTitle: 'Giữa kỳ Toán cao cấp',
    studentName: 'Trần Minh Đức',
    score: '8.5/10',
    time: '15 phút',
    status: 'Hoàn thành',
    statusKey: 'active'
  },
  {
    id: 2,
    type: 'exam',
    examTitle: 'Giữa kỳ Toán cao cấp',
    studentName: 'Lê Thị Hương',
    score: '6.0/10',
    time: '28 phút',
    status: 'Hoàn thành',
    statusKey: 'active'
  },
  {
    id: 3,
    type: 'violation',
    examTitle: 'Cuối kỳ Vật lý',
    studentName: 'Phạm Văn Bình',
    score: '-',
    time: '12 phút',
    status: 'Cảnh báo',
    statusKey: 'warning'
  },
  {
    id: 4,
    type: 'exam',
    examTitle: 'Kiểm tra Tiếng Anh',
    studentName: 'Nguyễn Thị Lan',
    score: '9.0/10',
    time: '22 phút',
    status: 'Hoàn thành',
    statusKey: 'active'
  },
  {
    id: 5,
    type: 'exam',
    examTitle: 'Giữa kỳ Toán cao cấp',
    studentName: 'Hoàng Văn An',
    score: '7.5/10',
    time: '18 phút',
    status: 'Hoàn thành',
    statusKey: 'active'
  }
])

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
  if (num >= 8) return 'var(--ds-success)'
  if (num >= 5) return 'var(--ds-warning)'
  return 'var(--ds-danger)'
}

const refreshData = () => {
  isLoading.value = true
  setTimeout(() => {
    isLoading.value = false
  }, 1000)
}

// Chart setup
const performanceChartRef = ref(null)
let performanceChart = null

const initPerformanceChart = () => {
  if (!performanceChartRef.value) return

  performanceChart = echarts.init(performanceChartRef.value)

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
      data: ['Điểm trung bình', 'Số lượng nộp bài'],
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
      data: ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'],
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
        name: 'Điểm',
        min: 0,
        max: 10,
        axisLine: { lineStyle: { color: 'var(--ds-border)' } },
        axisLabel: { color: 'var(--ds-text-muted)', fontWeight: 600 },
        splitLine: { lineStyle: { color: 'var(--ds-border)', type: 'dashed', opacity: 0.5 } }
      },
      {
        type: 'value',
        name: 'Số bài',
        min: 0,
        max: 50,
        axisLine: { lineStyle: { color: 'var(--ds-border)' } },
        axisLabel: { color: 'var(--ds-text-muted)', fontWeight: 600 },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: 'Điểm trung bình',
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
        data: [7.2, 7.5, 6.8, 7.9, 8.1, 7.4, 7.6]
      },
      {
        name: 'Số lượng nộp bài',
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
        data: [32, 28, 45, 38, 42, 35, 40]
      }
    ]
  }

  performanceChart.setOption(option)
}

onMounted(() => {
  nextTick(() => {
    initPerformanceChart()
  })

  window.addEventListener('resize', () => {
    performanceChart?.resize()
  })
})

watch(selectedPeriod, () => {
  initPerformanceChart()
})
</script>


<style scoped>
.ds-animate-fade-up {
  animation: fadeUp 0.5s ease-out;
}

@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
