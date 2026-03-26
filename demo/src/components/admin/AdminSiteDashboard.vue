<template>
  <div class="staff-page-wrap portal-scrollbar min-h-0 flex-1 gap-6 overflow-y-auto pb-2">
    <PageHeader
      eyebrow="Admin overview"
      title="Tổng quan hệ thống"
      subtitle="Theo dõi người dùng, đề thi, câu hỏi và lượt làm bài — một màn duy nhất, dữ liệu làm mới theo nhu cầu."
    >
      <template #actions>
        <button
          type="button"
          class="staff-action-btn staff-action-btn-neutral"
          :disabled="loading"
          @click="loadStats"
        >
          <span class="material-symbols-outlined text-lg" :class="{ 'animate-spin': loading }">refresh</span>
          Làm mới
        </button>
      </template>
    </PageHeader>

    <section class="staff-metric-grid">
      <article
        v-for="card in kpiCards"
        :key="card.label"
        class="staff-metric-card portal-card-lift rounded-3xl border border-slate-200/80 p-5 transition-shadow dark:border-slate-700/80"
      >
        <div class="mb-3 flex items-start justify-between gap-2">
          <span class="staff-metric-label">{{ card.label }}</span>
          <span class="material-symbols-outlined rounded-2xl bg-indigo-500/15 p-2 text-indigo-400">{{ card.icon }}</span>
        </div>
        <p class="staff-metric-value font-mono tabular-nums">{{ formatNum(card.value) }}</p>
      </article>
    </section>

    <section class="grid grid-cols-1 gap-4 xl:grid-cols-[0.9fr_1.1fr]">
      <div class="staff-surface rounded-[1.75rem] p-5 md:p-6">
        <div class="mb-4 flex items-center justify-between gap-3">
          <div>
            <p class="staff-kicker mb-2">Quick Access</p>
            <h3 class="text-[1.2rem] font-black tracking-[-0.03em] text-slate-900 dark:text-white">Đi nhanh tới từng nhóm quản trị</h3>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-3 sm:grid-cols-2">
          <RouterLink
            v-for="q in quickLinks"
            :key="q.to"
            :to="q.to"
            class="staff-surface-muted portal-card-lift group flex flex-col rounded-2xl p-4 text-left transition-colors hover:border-indigo-500/25"
          >
            <span class="material-symbols-outlined text-2xl text-indigo-400 transition group-hover:text-indigo-300">{{ q.icon }}</span>
            <p class="mt-3 text-sm font-semibold text-slate-900 dark:text-white">{{ q.label }}</p>
            <p v-if="q.hint" class="staff-muted-copy mt-1 line-clamp-2 text-xs">{{ q.hint }}</p>
          </RouterLink>
        </div>
      </div>

      <div class="staff-surface rounded-[1.75rem] p-5 md:p-6">
        <p class="staff-kicker mb-2">Attempts 7d</p>
        <h3 class="text-[1.2rem] font-black tracking-[-0.03em] text-slate-900 dark:text-white">Lượt thi trong 7 ngày gần đây</h3>
        <div ref="lineChartRef" class="mt-4 h-[250px] w-full" />
      </div>
    </section>

    <section class="staff-surface rounded-[1.75rem] p-5 md:p-6">
      <p class="staff-kicker mb-2">Role Mix</p>
      <div class="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,0.7fr)_minmax(0,1.3fr)] lg:items-center">
        <div>
          <h3 class="text-[1.2rem] font-black tracking-[-0.03em] text-slate-900 dark:text-white">Phân bổ vai trò người dùng</h3>
          <p class="staff-muted-copy mt-2">Theo dõi nhanh tỷ lệ học sinh, giáo viên và admin để nắm bức tranh vận hành toàn hệ thống.</p>
          <p v-if="errorMsg" class="mt-4 text-sm font-medium text-rose-500">{{ errorMsg }}</p>
        </div>
        <div ref="pieChartRef" class="h-[250px] w-full" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { fetchAdminDashboardStats } from '../../services/adminService'
import PageHeader from '../shared/PageHeader.vue'

const quickLinks = [
  { to: '/admin/students', label: 'Học sinh', hint: 'Tìm, xem chi tiết, xóa tài khoản', icon: 'school' },
  { to: '/admin/teachers', label: 'Giáo viên', hint: 'Danh sách & thao tác quản trị', icon: 'co_present' },
  { to: '/admin/admins', label: 'Admin', hint: 'Tài khoản quản trị nội bộ', icon: 'admin_panel_settings' },
  { to: '/admin/exams', label: 'Đề thi', hint: 'Bật/tắt đề, thời gian & thống kê', icon: 'quiz' }
]
const loading = ref(true)
const errorMsg = ref('')
const stats = ref(null)

const lineChartRef = ref(null)
const pieChartRef = ref(null)
let lineChart = null
let pieChart = null
let echartsLib = null

const kpiCards = computed(() => {
  const s = stats.value
  if (!s) {
    return [
      { label: 'Người dùng', value: 0, icon: 'group' },
      { label: 'Đề thi', value: 0, icon: 'quiz' },
      { label: 'Câu hỏi', value: 0, icon: 'help' },
      { label: 'Lượt thi', value: 0, icon: 'assignment_turned_in' }
    ]
  }
  return [
    { label: 'Người dùng', value: s.totalUsers, icon: 'group' },
    { label: 'Đề thi', value: s.totalExams, icon: 'quiz' },
    { label: 'Câu hỏi', value: s.totalQuestions, icon: 'help' },
    { label: 'Lượt thi', value: s.totalAttempts, icon: 'assignment_turned_in' }
  ]
})

const formatNum = (n) => (n == null ? '—' : new Intl.NumberFormat('vi-VN').format(n))

const disposeCharts = () => {
  lineChart?.dispose()
  pieChart?.dispose()
  lineChart = null
  pieChart = null
}

const buildLineOption = (echarts, series) => ({
  backgroundColor: 'transparent',
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(15, 23, 42, 0.92)',
    borderColor: 'rgba(99, 102, 241, 0.45)',
    textStyle: { color: '#e2e8f0' }
  },
  grid: { left: '3%', right: '4%', bottom: '3%', top: '12%', containLabel: true },
  xAxis: {
    type: 'category',
    data: series.map((d) => d.date),
    axisLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.25)' } },
    axisLabel: { color: '#94a3b8', fontSize: 11 }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.12)' } },
    axisLabel: { color: '#94a3b8', fontSize: 11 }
  },
  series: [
    {
      name: 'Attempts',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { width: 3, color: '#2dd4bf' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(45, 212, 191, 0.35)' },
          { offset: 1, color: 'rgba(45, 212, 191, 0)' }
        ])
      },
      data: series.map((d) => d.count)
    }
  ]
})

const buildPieOption = (s) => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'item', backgroundColor: 'rgba(15, 23, 42, 0.92)', borderColor: 'rgba(99, 102, 241, 0.45)' },
  legend: { bottom: 0, textStyle: { color: '#94a3b8' } },
  series: [
    {
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '46%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 8, borderColor: '#0f172a', borderWidth: 2 },
      label: { color: '#cbd5e1' },
      data: [
        { value: s.totalTeachers, name: 'Giáo viên', itemStyle: { color: '#818cf8' } },
        { value: s.totalStudents, name: 'Học sinh', itemStyle: { color: '#38bdf8' } },
        { value: s.totalAdmins, name: 'Admin', itemStyle: { color: '#fbbf24' } }
      ]
    }
  ]
})

const renderCharts = async () => {
  await nextTick()
  if (!stats.value) return
  if (!echartsLib) {
    echartsLib = await import('echarts')
  }
  const echarts = echartsLib.default || echartsLib

  const series = stats.value.attemptsLast7Days || []
  if (lineChartRef.value) {
    if (!lineChart) lineChart = echarts.init(lineChartRef.value)
    lineChart.setOption(buildLineOption(echarts, series))
  }
  if (pieChartRef.value) {
    if (!pieChart) pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption(buildPieOption(stats.value))
  }
}

const onResize = () => {
  lineChart?.resize()
  pieChart?.resize()
}

const loadStats = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    stats.value = await fetchAdminDashboardStats()
    await renderCharts()
  } catch (e) {
    errorMsg.value = e?.payload?.message || e?.message || 'Không tải được dữ liệu.'
    stats.value = null
    disposeCharts()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadStats()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  disposeCharts()
})
</script>

