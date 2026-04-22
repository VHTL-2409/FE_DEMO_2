<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto max-w-[1280px] px-4 pb-10 pt-4 sm:px-6 lg:px-8">
      <div class="mb-6 ds-animate-fade-up">
        <div class="flex items-center gap-2 text-sm mb-2" style="color: var(--ds-text-muted)">
          <RouterLink class="hover:text-[var(--ds-primary)] transition-colors flex items-center gap-1" to="/teacher/exams/list">
            <LucideIcon name="assignment" size="16" /> Đề thi
          </RouterLink>
          <LucideIcon name="chevron_right" size="12" />
          <span class="font-medium" style="color: var(--ds-text)">Tổng quan gian lận</span>
        </div>
        <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div>
            <h1 class="text-3xl font-extrabold tracking-tight" style="color: var(--ds-text)">{{ selectedExamTitle }}</h1>
            <p class="mt-1" style="color: var(--ds-text-secondary)">Xem xét và xử lý các vi phạm liêm chính học thuật bị gắn cờ trong tất cả phiên đang hoạt động.</p>
          </div>
          <div class="flex gap-2">
            <button
              :disabled="isLoadingSimilarity"
              class="inline-flex items-center justify-center gap-2 px-4 py-2.5 font-bold rounded-lg hover:-translate-y-0.5 transition-all duration-200 shadow-sm disabled:opacity-70"
              style="background-color: var(--ds-accent); color: white;"
              type="button"
              @click="loadAnswerSimilarity"
            >
              <LucideIcon name="compare_arrows" />
              <span>{{ isLoadingSimilarity ? 'Đang phân tích...' : 'Phân tích tương đồng đáp án' }}</span>
            </button>
            <button
              type="button"
              @click="downloadReport"
              class="inline-flex items-center justify-center gap-2 px-4 py-2.5 font-bold rounded-lg hover:-translate-y-0.5 transition-all duration-200 shadow-sm"
              style="background-color: var(--ds-primary); color: white;"
            >
              <LucideIcon name="download" />
              <span>Tải báo cáo</span>
            </button>
          </div>
        </div>
      </div>

      <div class="mb-6 ds-animate-fade-up-delay">
        <div class="inline-flex rounded-xl p-1 border" style="border-color: var(--ds-border); background-color: var(--ds-surface)">
          <RouterLink :to="summaryTabLink" class="px-4 py-2 rounded-lg text-sm font-bold transition-colors" style="color: var(--ds-text-secondary)">
            Tổng quan điểm &amp; báo cáo
          </RouterLink>
          <RouterLink :to="incidentTabLink" class="px-4 py-2 rounded-lg text-sm font-bold" style="background-color: var(--ds-primary); color: white">
            Tổng quan hành vi gian lận
          </RouterLink>
        </div>
      </div>

      <div class="relative grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6 ds-animate-fade-up-delay">
        <DsStatCard
          v-for="card in summaryCards"
          :key="card.title"
          :label="card.title"
          :value="card.value"
          :sub="card.sub || card.trend"
          :icon="card.icon"
        />
      </div>

      <div v-if="similarityPairs.length > 0" class="mb-6 p-4 ds-animate-fade-up-delay" style="background-color: var(--ds-accent-bg); border: 1px solid var(--ds-accent-border); border-radius: var(--ds-radius-xl); box-shadow: var(--ds-shadow-sm)">
        <h3 class="font-bold mb-3 flex items-center gap-2" style="color: var(--ds-accent)">
          <LucideIcon name="compare_arrows" />
          Cặp thí sinh có đáp án tương đồng cao (nghi ngờ gian lận)
        </h3>
        <div class="space-y-2">
          <div
            v-for="(pair, idx) in similarityPairs"
            :key="idx"
            class="flex items-center justify-between gap-4 py-2 px-3 rounded-lg"
            style="background-color: var(--ds-surface); border: 1px solid var(--ds-accent-border)"
          >
            <span class="font-semibold" style="color: var(--ds-text)">{{ pair.student1 }}</span>
            <span class="font-bold" style="color: var(--ds-accent)">{{ (pair.similarity * 100).toFixed(1) }}%</span>
            <span class="font-semibold" style="color: var(--ds-text)">{{ pair.student2 }}</span>
            <span class="text-xs" style="color: var(--ds-text-muted)">{{ pair.sameAnswers }}/{{ pair.commonQuestions }} câu trùng</span>
          </div>
        </div>
      </div>

      <div class="mb-6 p-4 ds-animate-fade-up-delay" style="background-color: var(--ds-surface); border-radius: var(--ds-radius-xl); border: 1px solid var(--ds-border)">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div class="flex flex-col gap-1.5">
            <label class="text-xs font-bold uppercase tracking-wider" style="color: var(--ds-text-muted)">Khoảng thời gian</label>
            <select
              v-model="periodFilter"
              class="w-full rounded-lg text-sm"
              style="background-color: var(--ds-gray-50); border-color: var(--ds-border); color: var(--ds-text); outline-color: var(--ds-primary)"
            >
              <option value="30d">30 ngày gần nhất</option>
              <option value="7d">7 ngày gần nhất</option>
              <option value="24h">24 giờ gần nhất</option>
              <option value="all">Tất cả thời gian</option>
            </select>
          </div>
          <div class="flex flex-col gap-1.5">
            <label class="text-xs font-bold uppercase tracking-wider" style="color: var(--ds-text-muted)">Loại vi phạm</label>
            <select
              v-model="violationFilter"
              class="w-full rounded-lg text-sm"
              style="background-color: var(--ds-gray-50); border-color: var(--ds-border); color: var(--ds-text); outline-color: var(--ds-primary)"
            >
              <option value="all">Tất cả vi phạm</option>
              <option value="suspicious">Hành vi đáng ngờ</option>
              <option value="high">Mức độ cao</option>
              <option value="risk">Vượt ngưỡng rủi ro</option>
            </select>
          </div>
          <div class="flex flex-col gap-1.5 lg:col-span-2">
            <label class="text-xs font-bold uppercase tracking-wider" style="color: var(--ds-text-muted)">Tìm kiếm</label>
            <input
              v-model.trim="searchQuery"
              class="w-full rounded-lg text-sm"
              style="background-color: var(--ds-gray-50); border-color: var(--ds-border); color: var(--ds-text); outline-color: var(--ds-primary)"
              placeholder="Tìm tên hoặc mã học sinh..."
            />
          </div>
        </div>
      </div>

      <div v-if="loadError" class="mb-6 ds-animate-fade-up-delay">
        <EmptyState
          icon="warning"
          title="Không tải được dữ liệu sự cố"
          :description="loadError"
          action-label="Mở lại tổng quan điểm"
          fill
          @action="goToSummaryReview"
        />
      </div>

      <div v-else-if="!isLoading && filteredIncidents.length === 0" class="mb-6 ds-animate-fade-up-delay">
        <EmptyState
          icon="fact_check"
          title="Chưa có sự cố cần xử lý"
          action-label="Mở tổng quan điểm"
          fill
          @action="goToSummaryReview"
        />
      </div>

      <div v-else class="overflow-hidden ds-animate-fade-up-delay" style="background-color: var(--ds-surface); border-radius: var(--ds-radius-xl); border: 1px solid var(--ds-border); box-shadow: var(--ds-shadow-sm)">
        <DataTable
          :columns="incidentColumns"
          :data="filteredIncidents"
          :row-key="'attemptId'"
          :loading="isLoading"
          :empty-text="'Không tìm thấy sự cố đáng ngờ nào cho đề thi này.'"
          :loading-text="'Đang tải sự cố...'"
        >
          <template #cell-student="{ value }">
            <span class="text-sm font-bold" style="color: var(--ds-text)">{{ value }}</span>
          </template>
          <template #cell-studentId="{ value }">
            <span class="text-xs" style="color: var(--ds-text-muted)">{{ value }}</span>
          </template>
          <template #cell-datetime="{ row }">
            <div class="flex flex-col">
              <span class="text-sm" style="color: var(--ds-text)">{{ row.date }}</span>
              <span class="text-xs" style="color: var(--ds-text-muted)">{{ row.time }}</span>
            </div>
          </template>
          <template #cell-violation="{ row }">
            <span :class="row.violationClass" class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold border">
              {{ row.violation }}
            </span>
          </template>
          <template #cell-action="{ value }">
            <span class="text-sm italic" style="color: var(--ds-text-muted)">{{ value }}</span>
          </template>
          <template #row-actions="{ row }">
            <button class="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm font-bold transition-all" style="color: var(--ds-primary); background-color: var(--ds-primary-soft)" type="button" @click="openIncidentReport(row)">
              <span>Xem xét</span>
              <LucideIcon name="open_in_new" size="18" />
            </button>
          </template>
        </DataTable>
      </div>
    </div>

    <Modal v-model="showIncidentModal" :title="selectedIncident ? `Báo cáo sự cố: ${selectedIncident.student}` : ''" size="xl">
      <template #header>
        <div v-if="selectedIncident" class="flex items-center gap-3">
          <div class="size-10 rounded-xl flex items-center justify-center" style="background-color: var(--ds-primary-soft)">
            <LucideIcon name="report" />
          </div>
          <div>
            <h2 class="text-xl font-bold leading-tight" style="color: var(--ds-text)">{{ selectedIncident.student }}</h2>
            <p class="text-sm" style="color: var(--ds-text-muted)">Mã: {{ selectedIncident.studentId }}</p>
          </div>
        </div>
      </template>

      <div v-if="selectedIncident" class="space-y-6">
        <div class="flex gap-2">
          <button type="button" @click="activeReportTab = 'result'" class="px-4 py-2 rounded-lg text-sm font-bold transition-colors" :style="activeReportTab === 'result' ? { backgroundColor: 'var(--ds-primary)', color: 'white' } : { backgroundColor: 'var(--ds-gray-100)', color: 'var(--ds-text-secondary)' }">
            Kết quả
          </button>
          <button type="button" @click="activeReportTab = 'warnings'" class="px-4 py-2 rounded-lg text-sm font-bold transition-colors" :style="activeReportTab === 'warnings' ? { backgroundColor: 'var(--ds-primary)', color: 'white' } : { backgroundColor: 'var(--ds-gray-100)', color: 'var(--ds-text-secondary)' }">
            Số lượt cảnh báo
          </button>
        </div>

        <section v-if="activeReportTab === 'result'" class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div class="col-span-1 md:col-span-2 grid grid-cols-2 gap-y-4 gap-x-8 p-6" style="background-color: var(--ds-surface); border-radius: var(--ds-radius-xl); border: 1px solid var(--ds-border)">
            <div class="space-y-1">
              <p class="text-xs font-semibold uppercase tracking-wider" style="color: var(--ds-text-muted)">Tên học sinh</p>
              <p class="font-medium" style="color: var(--ds-text)">{{ selectedIncident.student }}</p>
            </div>
            <div class="space-y-1">
              <p class="text-xs font-semibold uppercase tracking-wider" style="color: var(--ds-text-muted)">Mã học sinh</p>
              <p class="font-medium" style="color: var(--ds-text)">{{ selectedIncident.studentId }}</p>
            </div>
            <div class="space-y-1 border-t pt-3 mt-1" style="border-color: var(--ds-gray-100)">
              <p class="text-xs font-semibold uppercase tracking-wider" style="color: var(--ds-text-muted)">Tiêu đề đề thi</p>
              <p class="font-medium text-sm" style="color: var(--ds-text)">{{ selectedExamTitle }}</p>
            </div>
            <div class="space-y-1 border-t pt-3 mt-1" style="border-color: var(--ds-gray-100)">
              <p class="text-xs font-semibold uppercase tracking-wider" style="color: var(--ds-text-muted)">Ngày</p>
              <p class="font-medium" style="color: var(--ds-text)">{{ selectedIncident.date }}</p>
            </div>
            <div class="space-y-1 border-t pt-3 mt-1" style="border-color: var(--ds-gray-100)">
              <p class="text-xs font-semibold uppercase tracking-wider" style="color: var(--ds-text-muted)">Tên giám thị</p>
              <p class="font-medium" style="color: var(--ds-text)">Nguyễn Văn Minh</p>
            </div>
            <div class="space-y-1 border-t pt-3 mt-1" style="border-color: var(--ds-gray-100)">
              <p class="text-xs font-semibold uppercase tracking-wider" style="color: var(--ds-text-muted)">Kết quả</p>
              <span :class="selectedIncident.resultClass" class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold">
                {{ selectedIncident.result }}
              </span>
            </div>
          </div>
          <div class="flex flex-col gap-4">
            <div class="flex-1 flex flex-col justify-center items-center gap-1 p-4 rounded-xl" style="border: 1px solid var(--ds-danger-border); background-color: var(--ds-danger-bg)">
              <p class="text-xs font-bold uppercase" style="color: var(--ds-danger)">Mức độ nghiêm trọng</p>
              <p class="text-2xl font-black" style="color: var(--ds-danger)">{{ selectedIncident.severity }}</p>
            </div>
            <div class="flex-1 flex flex-col justify-center items-center gap-1 p-4 rounded-xl text-center" style="border: 1px solid var(--ds-primary-border); background-color: var(--ds-primary-soft)">
              <p class="text-xs font-bold uppercase" style="color: var(--ds-primary)">Loại vi phạm</p>
              <p class="text-sm font-bold leading-tight" style="color: var(--ds-text)">{{ selectedIncident.violation }}</p>
            </div>
          </div>
        </section>

        <section v-else class="space-y-4">
          <div class="p-6" style="background-color: var(--ds-surface); border-radius: var(--ds-radius-xl); border: 1px solid var(--ds-border)">
            <h3 class="font-bold flex items-center gap-2 mb-4" style="color: var(--ds-text)">
              <LucideIcon name="timeline" />
              Thời điểm vi phạm
            </h3>
            <div class="relative space-y-6">
              <div class="absolute left-[11px] top-2 bottom-2 w-0.5" style="background-color: var(--ds-gray-200)"></div>
              <div v-for="event in selectedIncident.warningTimeline" :key="event.time + event.label" class="relative flex gap-4">
                <div class="z-10 w-6 h-6 rounded-full flex items-center justify-center ring-4" :class="event.dotClass" style="ringColor: var(--ds-surface)">
                  <LucideIcon :name="event.icon" size="14" />
                </div>
                <div>
                  <p class="text-xs font-bold uppercase tracking-wide" :class="event.timeClass">{{ event.time }}</p>
                  <p class="text-sm font-semibold" style="color: var(--ds-text)">{{ event.label }}</p>
                  <p v-if="event.sub" class="text-xs" style="color: var(--ds-text-muted)">{{ event.sub }}</p>
                </div>
              </div>
            </div>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div class="p-5 rounded-xl text-center" style="background-color: var(--ds-surface); border: 1px solid var(--ds-border)">
              <p class="text-xs font-bold uppercase tracking-wider" style="color: var(--ds-text-muted)">Tổng cảnh báo</p>
              <p class="text-3xl font-black mt-1" style="color: var(--ds-danger)">{{ selectedIncident.warningCount }}</p>
            </div>
            <div class="p-5 rounded-xl text-center" style="background-color: var(--ds-surface); border: 1px solid var(--ds-border)">
              <p class="text-xs font-bold uppercase tracking-wider" style="color: var(--ds-text-muted)">Cảnh báo đầu tiên</p>
              <p class="text-lg font-bold mt-1" style="color: var(--ds-text)">{{ selectedIncident.warningTimeline[1]?.time }}</p>
            </div>
            <div class="p-5 rounded-xl text-center" style="background-color: var(--ds-surface); border: 1px solid var(--ds-border)">
              <p class="text-xs font-bold uppercase tracking-wider" style="color: var(--ds-text-muted)">Cảnh báo gần nhất</p>
              <p class="text-lg font-bold mt-1" style="color: var(--ds-text)">{{ selectedIncident.time }}</p>
            </div>
          </div>
        </section>
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-3">
          <button
            type="button"
            @click="printIncidentReport"
            class="flex items-center gap-2 px-4 py-2 text-sm font-bold border rounded-lg transition-colors"
            style="border-color: var(--ds-border); color: var(--ds-text-secondary); background-color: var(--ds-surface)"
          >
            <LucideIcon name="print" size="18" />
            In báo cáo
          </button>
          <button
            type="button"
            @click="downloadIncidentPdf"
            class="flex items-center gap-2 px-5 py-2 text-sm font-bold rounded-lg shadow-sm transition-shadow"
            style="background-color: var(--ds-primary); color: white"
          >
            <LucideIcon name="download" size="18" />
            Tải PDF
          </button>
        </div>
      </template>
    </Modal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ApiError } from '../../services/apiClient'
import { listExamAttempts } from '../../services/attemptService'
import { getAnswerSimilarity } from '../../services/examService'
import { exportToCsv } from '../../utils/reportExport'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import DsStatCard from '../ui/DsStatCard.vue'
import DataTable from '../ui/DataTable.vue'
import Modal from '../ui/Modal.vue'
import EmptyState from '../ui/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const showIncidentModal = ref(false)
const activeReportTab = ref('result')
const selectedIncident = ref(null)
const isLoading = ref(false)
const loadError = ref('')
const attempts = ref([])
const similarityPairs = ref([])
const isLoadingSimilarity = ref(false)
const periodFilter = ref('30d')
const violationFilter = ref('all')
const searchQuery = ref('')

const examId = computed(() => Number.parseInt(String(route.query.examId || ''), 10) || null)
const selectedExamTitle = computed(() => route.query.title || 'Đề thi đã chọn')
const summaryTabLink = computed(() => ({
  path: '/teacher/exams/review/summary',
  query: { ...route.query }
}))
const incidentTabLink = computed(() => ({ path: '/teacher/exams/review/incidents', query: route.query }))
const goToSummaryReview = () => {
  router.push(summaryTabLink.value)
}

const incidentColumns = computed(() => [
  { key: 'student', label: 'Học sinh' },
  { key: 'studentId', label: 'Mã' },
  { key: 'datetime', label: 'Thời điểm vi phạm' },
  { key: 'violation', label: 'Loại vi phạm' },
  { key: 'action', label: 'Xử lý' },
  { key: '_actions', label: 'Chi tiết', align: 'right' }
])

const formatDate = (value) => {
  if (!value) return '-'
  const date = new Date(String(value))
  return Number.isNaN(date.getTime()) ? '-' : date.toLocaleDateString()
}

const formatTime = (value) => {
  if (!value) return '-'
  const date = new Date(String(value))
  return Number.isNaN(date.getTime()) ? '-' : date.toLocaleTimeString()
}

const withinPeriod = (value) => {
  if (periodFilter.value === 'all') return true
  const date = new Date(String(value))
  if (Number.isNaN(date.getTime())) return false
  const now = Date.now()
  const diffMs = now - date.getTime()
  const periodMap = {
    '24h': 24 * 60 * 60 * 1000,
    '7d': 7 * 24 * 60 * 60 * 1000,
    '30d': 30 * 24 * 60 * 60 * 1000
  }
  return diffMs <= (periodMap[periodFilter.value] ?? periodMap['30d'])
}

const buildIncident = (attempt) => {
  const riskScore = Number(attempt.riskScore || 0)
  const suspicious = Boolean(attempt.suspicious)
  const warningCount = Math.max(riskScore, suspicious ? 1 : 0)
  const severity = riskScore >= 70 ? 'CAO' : riskScore >= 40 ? 'TRUNG BÌNH' : 'THẤP'
  const hasHighRisk = severity === 'CAO'
  const violation = suspicious ? 'Hành vi đáng ngờ' : 'Vượt ngưỡng rủi ro'
  const submittedAt = attempt.submittedAt || attempt.startedAt
  const firstWarningTime = formatTime(attempt.startedAt)
  const latestWarningTime = formatTime(submittedAt)

  return {
    attemptId: attempt.id,
    student: attempt.student || 'Học sinh không rõ',
    studentId: attempt.studentCode || `AT-${attempt.id}`,
    submittedAt,
    date: formatDate(submittedAt),
    time: latestWarningTime,
    violation,
    violationClass: hasHighRisk
      ? 'bg-rose-100 text-rose-800 dark:bg-rose-900/40 dark:text-rose-400 border-rose-200 dark:border-rose-800'
      : 'bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-400 border-amber-200 dark:border-amber-800',
    action: hasHighRisk ? 'Đang điều tra' : 'Đã cảnh báo',
    result: hasHighRisk ? 'Đang xem xét' : 'Đã gắn cờ',
    resultClass: hasHighRisk
      ? 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400'
      : 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400',
    severity,
    warningCount,
    firstWarningTime,
    warningTimeline: [
      { time: formatTime(attempt.startedAt), label: 'Bắt đầu thi', sub: '', icon: 'play_arrow', dotClass: 'bg-[var(--ds-primary)]', timeClass: 'text-[var(--ds-text-muted)]' },
      { time: firstWarningTime, label: 'Kích hoạt cờ rủi ro', sub: `Điểm rủi ro: ${riskScore}`, icon: 'warning', dotClass: 'bg-[var(--ds-accent)]', timeClass: 'text-[var(--ds-accent)]' },
      { time: latestWarningTime, label: 'Đã nộp bài', sub: attempt.status, icon: 'fact_check', dotClass: 'bg-[var(--ds-danger)]', timeClass: 'text-[var(--ds-danger)]' }
    ]
  }
}

const incidents = computed(() => attempts.value
  .filter((attempt) => Number(attempt.riskScore || 0) > 0 || Boolean(attempt.suspicious))
  .map(buildIncident))

const incidentSummary = computed(() => {
  const counts = new Map()
  let high = 0
  let resolved = 0

  for (const incident of filteredIncidents.value) {
    if (incident.severity === 'CAO') high += 1
    if (incident.result !== 'Đang xem xét') resolved += 1
    counts.set(incident.violation, (counts.get(incident.violation) || 0) + 1)
  }

  const commonViolation = filteredIncidents.value.length
    ? [...counts.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] || '-'
    : '-'

  return {
    total: filteredIncidents.value.length,
    high,
    resolved,
    commonViolation
  }
})

const filteredIncidents = computed(() => {
  let list = incidents.value

  if (periodFilter.value !== 'all') {
    list = list.filter((incident) => withinPeriod(incident.submittedAt))
  }

  if (violationFilter.value === 'suspicious') {
    list = list.filter((incident) => incident.violation === 'Hành vi đáng ngờ')
  } else if (violationFilter.value === 'high') {
    list = list.filter((incident) => incident.severity === 'CAO')
  } else if (violationFilter.value === 'risk') {
    list = list.filter((incident) => incident.violation === 'Vượt ngưỡng rủi ro')
  }

  if (searchQuery.value) {
    const keyword = searchQuery.value.toLowerCase()
    list = list.filter((incident) =>
      String(incident.student || '').toLowerCase().includes(keyword) ||
      String(incident.studentId || '').toLowerCase().includes(keyword)
    )
  }

  return list
})

const summaryCards = computed(() => {
  const { total, high, resolved, commonViolation } = incidentSummary.value
  const incidentRate = attempts.value.length ? (total / attempts.value.length) * 100 : 0

  return [
    {
      title: 'Tổng sự cố',
      icon: 'warning',
      value: String(total),
      trend: `${high} mức độ cao`
    },
    {
      title: 'Vi phạm phổ biến',
      icon: 'report',
      value: commonViolation,
      sub: total ? 'Từ các lượt làm hiện tại' : 'Chưa có dữ liệu'
    },
    {
      title: 'Tỷ lệ sự cố',
      icon: 'analytics',
      value: attempts.value.length ? `${incidentRate.toFixed(1)}%` : '-',
      trend: `${total}/${attempts.value.length || 0} lượt làm`
    },
    {
      title: 'Sự cố đã xử lý',
      icon: 'check_circle',
      value: String(resolved),
      trend: `trên ${total} trường hợp`
    }
  ]
})

const downloadReport = () => {
  const columns = [
    { key: 'student', label: 'Học sinh' },
    { key: 'studentId', label: 'Mã học sinh' },
    { key: 'date', label: 'Ngày' },
    { key: 'time', label: 'Thời gian' },
    { key: 'violation', label: 'Vi phạm' },
    { key: 'severity', label: 'Mức độ' },
    { key: 'warningCount', label: 'Số cảnh báo' }
  ]
  const safeTitle = (selectedExamTitle.value || 'bao-cao-gian-lan').replace(/[^a-zA-Z0-9\u00C0-\u024F]/g, '-')
  exportToCsv(filteredIncidents.value, columns, `bao-cao-gian-lan-${safeTitle}.csv`)
}

const printIncidentReport = () => {
  window.print()
}

const downloadIncidentPdf = () => {
  window.print()
}

const openIncidentReport = (incident) => {
  selectedIncident.value = incident
  activeReportTab.value = 'result'
  showIncidentModal.value = true
}

const closeIncidentReport = () => {
  showIncidentModal.value = false
}

const loadIncidentData = async () => {
  if (!examId.value) {
    loadError.value = 'Thiếu mã đề thi. Vui lòng mở báo cáo này từ trang Quản lý đề thi.'
    return
  }

  isLoading.value = true
  loadError.value = ''
  try {
    const attemptsPayload = await listExamAttempts(examId.value)
    attempts.value = attemptsPayload
  } catch (error) {
    loadError.value = error instanceof ApiError ? error.message : 'Không thể tải dữ liệu sự cố.'
  } finally {
    isLoading.value = false
  }
}

const loadAnswerSimilarity = async () => {
  if (!examId.value) return
  isLoadingSimilarity.value = true
  similarityPairs.value = []
  try {
    similarityPairs.value = await getAnswerSimilarity(examId.value)
  } catch {
    similarityPairs.value = []
  } finally {
    isLoadingSimilarity.value = false
  }
}

onMounted(loadIncidentData)
watch(() => route.query.examId, loadIncidentData)
</script>

<style scoped>
.ds-animate-fade-up {
  animation: fadeUp 0.5s ease-out;
}

.ds-animate-fade-up-delay {
  animation: fadeUp 0.65s ease-out;
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
