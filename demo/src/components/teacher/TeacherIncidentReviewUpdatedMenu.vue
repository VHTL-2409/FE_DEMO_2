<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen"
  >
    <div class="layout-container flex h-full grow flex-col">
      <TeacherTopHeader active-section="monitoring" />

      <main class="teacher-page-shell max-w-[1280px]">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

        <div class="relative mb-6 animate-fade-up">
          <div class="flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400 mb-2">
            <RouterLink class="hover:text-primary transition-colors flex items-center gap-1" to="/teacher/exams"><span class="material-symbols-outlined text-base">assignment</span> Đề thi</RouterLink>
            <span class="material-symbols-outlined text-xs">chevron_right</span>
            <span class="text-slate-900 dark:text-slate-100 font-medium">Tổng quan gian lận</span>
          </div>
          <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
            <div>
              <h1 class="text-3xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight">{{ selectedExamTitle }}</h1>
              <p class="text-slate-600 dark:text-slate-400 mt-1">Xem xét và xử lý các vi phạm liêm chính học thuật bị gắn cờ trong tất cả phiên đang hoạt động.</p>
            </div>
            <div class="flex gap-2">
              <button
                :disabled="isLoadingSimilarity"
                class="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-amber-600 text-white font-bold rounded-lg hover:bg-amber-500 hover:-translate-y-0.5 transition-all duration-200 shadow-sm disabled:opacity-70"
                type="button"
                @click="loadAnswerSimilarity"
              >
                <span class="material-symbols-outlined text-xl">compare_arrows</span>
                <span>{{ isLoadingSimilarity ? 'Đang phân tích...' : 'Phân tích tương đồng đáp án' }}</span>
              </button>
              <button
                type="button"
                @click="downloadReport"
                class="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-primary text-white font-bold rounded-lg hover:bg-primary/90 hover:-translate-y-0.5 transition-all duration-200 shadow-sm"
              >
                <span class="material-symbols-outlined text-xl">download</span>
                <span>Tải báo cáo</span>
              </button>
            </div>
          </div>
        </div>

        <div class="mb-6 animate-fade-up-delay">
          <div class="inline-flex rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-1 shadow-sm">
            <RouterLink :to="summaryTabLink" class="px-4 py-2 rounded-lg text-sm font-bold text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors">
              Tổng quan điểm &amp; báo cáo
            </RouterLink>
            <RouterLink :to="incidentTabLink" class="px-4 py-2 rounded-lg text-sm font-bold bg-primary text-white">
              Tổng quan hành vi gian lận
            </RouterLink>
          </div>
        </div>

        <div class="relative grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8 animate-fade-up-delay">
          <div class="bg-white dark:bg-slate-900 p-5 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200" v-for="card in summaryCards" :key="card.title">
            <div class="flex items-center justify-between mb-2">
              <p class="text-slate-500 dark:text-slate-400 text-sm font-medium">{{ card.title }}</p>
              <span class="material-symbols-outlined text-primary">{{ card.icon }}</span>
            </div>
            <div v-if="card.sub" class="flex flex-col">
              <h3 class="text-2xl font-bold text-slate-900 dark:text-slate-100 truncate">{{ card.value }}</h3>
              <p class="text-slate-400 text-xs mt-1">{{ card.sub }}</p>
            </div>
            <div v-else class="flex items-baseline gap-2">
              <h3 class="text-2xl font-bold text-slate-900 dark:text-slate-100">{{ card.value }}</h3>
              <span :class="card.trendClass" class="text-sm font-bold flex items-center">{{ card.trend }}</span>
            </div>
          </div>
        </div>

        <div v-if="similarityPairs.length > 0" class="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 p-4 rounded-xl shadow-sm mb-6">
          <h3 class="font-bold text-amber-800 dark:text-amber-200 mb-3 flex items-center gap-2">
            <span class="material-symbols-outlined">compare_arrows</span>
            Cặp thí sinh có đáp án tương đồng cao (nghi ngờ gian lận)
          </h3>
          <div class="space-y-2">
            <div
              v-for="(pair, idx) in similarityPairs"
              :key="idx"
              class="flex items-center justify-between gap-4 py-2 px-3 rounded-lg bg-white dark:bg-slate-900 border border-amber-200 dark:border-amber-800"
            >
              <span class="font-semibold text-slate-900 dark:text-slate-100">{{ pair.student1 }}</span>
              <span class="text-amber-600 dark:text-amber-400 font-bold">{{ (pair.similarity * 100).toFixed(1) }}%</span>
              <span class="font-semibold text-slate-900 dark:text-slate-100">{{ pair.student2 }}</span>
              <span class="text-xs text-slate-500">{{ pair.sameAnswers }}/{{ pair.commonQuestions }} câu trùng</span>
            </div>
          </div>
        </div>

        <div class="bg-white dark:bg-slate-900 p-4 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm mb-6">
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div class="flex flex-col gap-1.5">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Khoảng thời gian</label>
              <select class="form-select w-full bg-slate-50 dark:bg-slate-800 border-slate-200 dark:border-slate-700 rounded-lg text-sm focus:ring-primary focus:border-primary">
                <option>30 ngày gần nhất</option>
                <option>7 ngày gần nhất</option>
                <option>24 giờ gần nhất</option>
              </select>
            </div>
            <div class="flex flex-col gap-1.5">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Loại vi phạm</label>
              <select class="form-select w-full bg-slate-50 dark:bg-slate-800 border-slate-200 dark:border-slate-700 rounded-lg text-sm focus:ring-primary focus:border-primary">
                <option>Tất cả vi phạm</option>
                <option>Chuyển tab</option>
                <option>Phát hiện AI</option>
                <option>Thiết bị ngoài</option>
              </select>
            </div>
            <div class="flex flex-col gap-1.5 lg:col-span-2">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Tìm kiếm</label>
              <input class="form-input w-full bg-slate-50 dark:bg-slate-800 border-slate-200 dark:border-slate-700 rounded-lg text-sm focus:ring-primary focus:border-primary" placeholder="Tìm tên hoặc mã sinh viên..." />
            </div>
          </div>
        </div>

        <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
          <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="bg-slate-50 dark:bg-slate-800/50 border-b border-slate-200 dark:border-slate-800">
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Sinh viên</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Mã</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Thời điểm vi phạm</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Loại vi phạm</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Xử lý</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider text-right">Chi tiết</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                <tr v-if="isLoading">
                  <td colspan="6" class="px-6 py-10 text-center text-sm text-slate-500">Đang tải sự cố...</td>
                </tr>
                <tr v-else-if="loadError">
                  <td colspan="6" class="px-6 py-10 text-center text-sm text-rose-600">{{ loadError }}</td>
                </tr>
                <tr v-else-if="!incidents.length">
                  <td colspan="6" class="px-6 py-10 text-center text-sm text-slate-500">Không tìm thấy sự cố đáng ngờ nào cho đề thi này.</td>
                </tr>
                <template v-else>
                  <tr v-for="incident in incidents" :key="incident.attemptId" class="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition-colors">
                    <td class="px-6 py-4 text-sm font-bold text-slate-900 dark:text-slate-100">{{ incident.student }}</td>
                    <td class="px-6 py-4 text-xs text-slate-500">{{ incident.studentId }}</td>
                    <td class="px-6 py-4">
                      <div class="flex flex-col">
                        <span class="text-sm text-slate-900 dark:text-slate-100">{{ incident.date }}</span>
                        <span class="text-xs text-slate-500">{{ incident.time }}</span>
                      </div>
                    </td>
                    <td class="px-6 py-4">
                      <span :class="incident.violationClass" class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold border">
                        {{ incident.violation }}
                      </span>
                    </td>
                    <td class="px-6 py-4 text-sm text-slate-600 dark:text-slate-400 italic">{{ incident.action }}</td>
                    <td class="px-6 py-4 text-right">
                      <button class="text-primary hover:bg-primary/10 px-3 py-1.5 rounded-lg text-sm font-bold transition-all inline-flex items-center gap-1 bg-primary/5" type="button" @click="openIncidentReport(incident)">
                        <span>Xem xét</span>
                        <span class="material-symbols-outlined text-lg">open_in_new</span>
                      </button>
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>

    <div v-if="showIncidentModal && selectedIncident" class="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="incident-modal-title" @click.self="closeIncidentReport">
      <div class="modal-content w-full max-w-4xl flex flex-col">
        <header class="modal-header shrink-0">
          <div class="flex items-center gap-3">
            <div class="size-10 rounded-xl bg-indigo-100 dark:bg-indigo-500/20 flex items-center justify-center">
              <span class="material-symbols-outlined text-indigo-600 dark:text-indigo-400 text-xl">report</span>
            </div>
            <h2 id="incident-modal-title" class="text-xl font-bold text-slate-900 dark:text-slate-100 leading-tight">Báo cáo sự cố: {{ selectedIncident.student }}</h2>
          </div>
          <button type="button" class="modal-close-btn" aria-label="Đóng" @click="closeIncidentReport">
            <span class="material-symbols-outlined">close</span>
          </button>
        </header>

        <div class="px-6 pt-4 pb-4 border-b border-slate-200 dark:border-slate-700 bg-slate-50/50 dark:bg-slate-800/30">
          <div class="flex gap-2">
            <button type="button" @click="activeReportTab = 'result'" :class="activeReportTab === 'result' ? 'bg-primary text-white' : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300'" class="px-4 py-2 rounded-lg text-sm font-bold transition-colors">
              Kết quả
            </button>
            <button type="button" @click="activeReportTab = 'warnings'" :class="activeReportTab === 'warnings' ? 'bg-primary text-white' : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300'" class="px-4 py-2 rounded-lg text-sm font-bold transition-colors">
              Số lượt cảnh báo
            </button>
          </div>
        </div>

        <div class="modal-body space-y-8 bg-slate-50/30 dark:bg-slate-900/30">
          <section v-if="activeReportTab === 'result'" class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div class="col-span-1 md:col-span-2 grid grid-cols-2 gap-y-4 gap-x-8 bg-white dark:bg-slate-800 p-6 rounded-xl border border-slate-200 dark:border-slate-700">
              <div class="space-y-1">
                <p class="text-slate-500 text-xs font-semibold uppercase tracking-wider">Tên sinh viên</p>
                <p class="text-slate-900 dark:text-slate-100 font-medium">{{ selectedIncident.student }}</p>
              </div>
              <div class="space-y-1">
                <p class="text-slate-500 text-xs font-semibold uppercase tracking-wider">Mã sinh viên</p>
                <p class="text-slate-900 dark:text-slate-100 font-medium">{{ selectedIncident.studentId }}</p>
              </div>
              <div class="space-y-1 border-t border-slate-100 dark:border-slate-700 pt-3 mt-1">
                <p class="text-slate-500 text-xs font-semibold uppercase tracking-wider">Tiêu đề đề thi</p>
                <p class="text-slate-900 dark:text-slate-100 font-medium text-sm">{{ selectedExamTitle }}</p>
              </div>
              <div class="space-y-1 border-t border-slate-100 dark:border-slate-700 pt-3 mt-1">
                <p class="text-slate-500 text-xs font-semibold uppercase tracking-wider">Ngày</p>
                <p class="text-slate-900 dark:text-slate-100 font-medium">{{ selectedIncident.date }}</p>
              </div>
              <div class="space-y-1 border-t border-slate-100 dark:border-slate-700 pt-3 mt-1">
                <p class="text-slate-500 text-xs font-semibold uppercase tracking-wider">Tên giám thị</p>
                <p class="text-slate-900 dark:text-slate-100 font-medium">Sarah Smith</p>
              </div>
              <div class="space-y-1 border-t border-slate-100 dark:border-slate-700 pt-3 mt-1">
                <p class="text-slate-500 text-xs font-semibold uppercase tracking-wider">Kết quả</p>
                <span :class="selectedIncident.resultClass" class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold">
                  {{ selectedIncident.result }}
                </span>
              </div>
            </div>
            <div class="flex flex-col gap-4">
              <div class="flex-1 flex flex-col justify-center items-center gap-1 rounded-xl p-4 border border-red-200 dark:border-red-900/50 bg-red-50 dark:bg-red-950/20">
                <p class="text-red-800 dark:text-red-400 text-xs font-bold uppercase">Mức độ nghiêm trọng</p>
                <p class="text-red-600 dark:text-red-300 tracking-tight text-2xl font-black">{{ selectedIncident.severity }}</p>
              </div>
              <div class="flex-1 flex flex-col justify-center items-center gap-1 rounded-xl p-4 border border-primary/20 bg-primary/5">
                <p class="text-primary text-xs font-bold uppercase">Loại vi phạm</p>
                <p class="text-slate-900 dark:text-white text-center text-sm font-bold leading-tight">{{ selectedIncident.violation }}</p>
              </div>
            </div>
          </section>

          <section v-else class="space-y-4">
            <div class="bg-white dark:bg-slate-800 p-6 rounded-xl border border-slate-200 dark:border-slate-700">
              <h3 class="text-slate-900 dark:text-white font-bold flex items-center gap-2 mb-4">
                <span class="material-symbols-outlined text-primary">timeline</span>
                Thời điểm vi phạm
              </h3>
              <div class="relative space-y-6">
                <div class="absolute left-[11px] top-2 bottom-2 w-0.5 bg-slate-200 dark:bg-slate-700"></div>
                <div v-for="event in selectedIncident.warningTimeline" :key="event.time + event.label" class="relative flex gap-4">
                  <div :class="event.dotClass" class="z-10 w-6 h-6 rounded-full flex items-center justify-center ring-4 ring-white dark:ring-slate-800">
                    <span class="material-symbols-outlined text-[14px] text-white">{{ event.icon }}</span>
                  </div>
                  <div>
                    <p :class="event.timeClass" class="text-xs font-bold uppercase tracking-wide">{{ event.time }}</p>
                    <p class="text-sm font-semibold text-slate-900 dark:text-slate-100">{{ event.label }}</p>
                    <p v-if="event.sub" class="text-xs text-slate-500">{{ event.sub }}</p>
                  </div>
                </div>
              </div>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div class="bg-white dark:bg-slate-800 p-5 rounded-xl border border-slate-200 dark:border-slate-700 text-center">
                <p class="text-slate-500 text-xs font-bold uppercase tracking-wider">Tổng cảnh báo</p>
                <p class="text-3xl font-black text-red-600 mt-1">{{ selectedIncident.warningCount }}</p>
              </div>
              <div class="bg-white dark:bg-slate-800 p-5 rounded-xl border border-slate-200 dark:border-slate-700 text-center">
                <p class="text-slate-500 text-xs font-bold uppercase tracking-wider">Cảnh báo đầu tiên</p>
                <p class="text-lg font-bold mt-1">{{ selectedIncident.warningTimeline[1].time }}</p>
              </div>
              <div class="bg-white dark:bg-slate-800 p-5 rounded-xl border border-slate-200 dark:border-slate-700 text-center">
                <p class="text-slate-500 text-xs font-bold uppercase tracking-wider">Cảnh báo gần nhất</p>
                <p class="text-lg font-bold mt-1">{{ selectedIncident.time }}</p>
              </div>
            </div>
          </section>
        </div>

        <footer class="flex items-center justify-end gap-3 border-t border-slate-200 dark:border-slate-700 px-8 py-5 shrink-0 bg-slate-50 dark:bg-slate-900/80">
          <button
            type="button"
            @click="printIncidentReport"
            class="flex items-center gap-2 px-4 py-2 text-sm font-bold text-slate-700 dark:text-slate-300 border border-slate-300 dark:border-slate-600 rounded-lg hover:bg-white dark:hover:bg-slate-800 transition-colors"
          >
            <span class="material-symbols-outlined text-lg">print</span>
            In báo cáo
          </button>
          <button
            type="button"
            @click="downloadIncidentPdf"
            class="flex items-center gap-2 px-5 py-2 text-sm font-bold text-white bg-primary rounded-lg hover:bg-primary/90 transition-shadow shadow-sm"
          >
            <span class="material-symbols-outlined text-lg">download</span>
            Tải PDF
          </button>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ApiError } from '../../services/apiClient'
import { getAttemptReport, listExamAttempts } from '../../services/attemptService'
import { getAnswerSimilarity } from '../../services/examService'
import { exportToCsv } from '../../utils/reportExport'
import { RouterLink, useRoute } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const route = useRoute()
const isDark = ref(false)
const showIncidentModal = ref(false)
const activeReportTab = ref('result')
const selectedIncident = ref(null)
const isLoading = ref(false)
const loadError = ref('')
const attempts = ref([])
const reportByAttempt = ref({})
const similarityPairs = ref([])
const isLoadingSimilarity = ref(false)

const examId = computed(() => Number.parseInt(String(route.query.examId || ''), 10) || null)
const selectedExamTitle = computed(() => route.query.title || 'Đề thi đã chọn')
const summaryTabLink = computed(() => ({ path: '/teacher/exams/review/summary', query: route.query }))
const incidentTabLink = computed(() => ({ path: '/teacher/exams/review/incidents', query: route.query }))

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

const buildIncident = (attempt) => {
  const report = reportByAttempt.value[attempt.id]
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
    student: attempt.student || 'Sinh viên không rõ',
    studentId: `AT-${attempt.id}`,
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
      { time: formatTime(attempt.startedAt), label: 'Bắt đầu thi', sub: '', icon: 'play_arrow', dotClass: 'bg-primary', timeClass: 'text-slate-400' },
      { time: firstWarningTime, label: 'Kích hoạt cờ rủi ro', sub: `Điểm rủi ro: ${riskScore}`, icon: 'warning', dotClass: 'bg-amber-500', timeClass: 'text-amber-500' },
      { time: latestWarningTime, label: 'Đã nộp bài', sub: report?.status || attempt.status, icon: 'fact_check', dotClass: 'bg-red-500', timeClass: 'text-red-500' }
    ]
  }
}

const incidents = computed(() => attempts.value
  .filter((attempt) => Number(attempt.riskScore || 0) > 0 || Boolean(attempt.suspicious))
  .map(buildIncident))

const summaryCards = computed(() => {
  const total = incidents.value.length
  const high = incidents.value.filter((incident) => incident.severity === 'HIGH').length
  const resolved = incidents.value.filter((incident) => incident.result !== 'Đang xem xét').length
  const commonViolation = incidents.value.length ? incidents.value[0].violation : '-'
  const incidentRate = attempts.value.length ? (total / attempts.value.length) * 100 : 0

  return [
    {
      title: 'Tổng sự cố',
      icon: 'warning',
      value: String(total),
      trend: `${high} mức độ cao`,
      trendClass: 'text-slate-500 dark:text-slate-400'
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
      trend: `${total}/${attempts.value.length || 0} lượt làm`,
      trendClass: 'text-slate-500 dark:text-slate-400'
    },
    {
      title: 'Sự cố đã xử lý',
      icon: 'check_circle',
      value: String(resolved),
      trend: `trên ${total} trường hợp`,
      trendClass: 'text-slate-400'
    }
  ]
})

const downloadReport = () => {
  const columns = [
    { key: 'student', label: 'Sinh viên' },
    { key: 'studentId', label: 'Mã sinh viên' },
    { key: 'date', label: 'Ngày' },
    { key: 'time', label: 'Thời gian' },
    { key: 'violation', label: 'Vi phạm' },
    { key: 'severity', label: 'Mức độ' },
    { key: 'warningCount', label: 'Số cảnh báo' }
  ]
  const safeTitle = (selectedExamTitle.value || 'bao-cao-gian-lan').replace(/[^a-zA-Z0-9\u00C0-\u024F]/g, '-')
  exportToCsv(incidents.value, columns, `bao-cao-gian-lan-${safeTitle}.csv`)
}

const printIncidentReport = () => {
  if (showIncidentModal.value && selectedIncident.value) {
    window.print()
  } else {
    window.print()
  }
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

    const reports = await Promise.all(attemptsPayload.map(async (attempt) => {
      try {
        const report = await getAttemptReport(attempt.id)
        return [attempt.id, report]
      } catch {
        return [attempt.id, null]
      }
    }))
    reportByAttempt.value = Object.fromEntries(reports)
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
</script>

