<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="flex h-full min-h-0 flex-1 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100"
  >
    <div class="layout-container flex min-h-0 flex-1 grow flex-col">
      <TeacherTopHeader active-section="review" />

      <main class="teacher-stitch-main teacher-page-shell portal-scrollbar relative mx-auto w-full max-w-none min-h-0 flex-1 overflow-x-hidden overflow-y-auto px-3 py-6 sm:px-4 lg:px-5">

        <div class="relative mb-6 w-full max-w-screen-2xl animate-fade-up">
          <p class="portal-kicker mb-2">
            <RouterLink to="/teacher/dashboard" class="text-slate-500 transition hover:text-[var(--role-primary)] dark:text-slate-400">Trang chủ</RouterLink>
            <span class="mx-1.5 text-slate-300 dark:text-slate-600">/</span>
            <RouterLink class="text-slate-500 transition hover:text-[var(--role-primary)] dark:text-slate-400" to="/teacher/exams">Đề thi</RouterLink>
            <span class="mx-1.5 text-slate-300 dark:text-slate-600">/</span>
            <span class="font-semibold text-[var(--role-primary)]">Tổng quan gian lận</span>
          </p>
          <div class="flex flex-col justify-between gap-4 md:flex-row md:items-end">
            <div>
              <h1 class="stitch-font-headline text-4xl font-bold tracking-tight text-amber-900 dark:text-amber-100 md:text-5xl">
                {{ selectedExamTitle }}
              </h1>
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

        <div class="mb-6 flex w-full animate-fade-up-delay justify-start">
          <div class="teacher-stitch-segment inline-flex rounded-xl p-1 shadow-sm">
            <RouterLink :to="summaryTabLink" class="px-4 py-2 rounded-lg text-sm font-bold text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors">
              Tổng quan điểm &amp; báo cáo
            </RouterLink>
            <RouterLink :to="incidentTabLink" class="px-4 py-2 rounded-lg text-sm font-bold bg-primary text-white">
              Tổng quan hành vi gian lận
            </RouterLink>
          </div>
        </div>

        <div class="relative mb-8 grid grid-cols-1 gap-4 animate-fade-up-delay sm:grid-cols-2 lg:grid-cols-4">
          <div class="stitch-stat-bento transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md" v-for="card in summaryCards" :key="card.title">
            <div class="flex items-center justify-between mb-2">
              <p class="text-slate-500 dark:text-slate-400 text-sm font-medium">{{ card.title }}</p>
              <span class="material-symbols-outlined text-primary">{{ card.icon }}</span>
            </div>
            <div v-if="card.sub" class="flex flex-col">
              <h3 class="text-2xl font-bold leading-tight text-slate-900 dark:text-slate-100 break-words">{{ card.value }}</h3>
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
              <select
                v-model="filterTimeRange"
                class="teacher-stitch-field teacher-stitch-select w-full rounded-lg px-3 py-2.5 text-sm text-slate-800 dark:text-slate-100"
              >
                <option value="30d">30 ngày gần nhất</option>
                <option value="7d">7 ngày gần nhất</option>
                <option value="24h">24 giờ gần nhất</option>
              </select>
            </div>
            <div class="flex flex-col gap-1.5">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Loại vi phạm</label>
              <select
                v-model="filterViolationType"
                class="teacher-stitch-field teacher-stitch-select w-full rounded-lg px-3 py-2.5 text-sm text-slate-800 dark:text-slate-100"
              >
                <option value="all">Tất cả vi phạm</option>
                <option value="tab">Chuyển tab</option>
                <option value="ai">Phát hiện AI</option>
                <option value="device">Thiết bị ngoài</option>
              </select>
            </div>
            <div class="flex flex-col gap-1.5 lg:col-span-2">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Tìm kiếm</label>
              <input
                v-model.trim="incidentSearch"
                class="form-input w-full bg-slate-50 dark:bg-slate-800 border-slate-200 dark:border-slate-700 rounded-lg text-sm focus:ring-primary focus:border-primary"
                placeholder="Tìm tên hoặc mã sinh viên..."
                type="search"
                autocomplete="off"
              />
            </div>
          </div>
        </div>

        <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
          <div class="teacher-stitch-table-scroll teacher-stitch-table-scroll--slate-head">
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
                <tr v-else-if="!filteredIncidents.length">
                  <td colspan="6" class="px-6 py-10 text-center text-sm text-slate-500">Không có sự cố nào khớp bộ lọc hoặc đề thi này chưa có dữ liệu.</td>
                </tr>
                <template v-else>
                  <tr v-for="incident in paginatedIncidents" :key="incident.attemptId" class="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition-colors">
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
          <div
            v-if="filteredIncidents.length > INC_PAGE_SIZE"
            class="flex flex-col gap-2 border-t border-slate-200 bg-slate-50/80 px-4 py-3 dark:border-slate-800 dark:bg-slate-800/30 sm:flex-row sm:items-center sm:justify-between"
          >
            <p class="text-xs text-slate-500">
              {{ filteredIncidents.length }} sự cố · trang {{ pageIncidents }} / {{ totalPagesIncidents }}
            </p>
            <div class="flex items-center gap-2">
              <button
                type="button"
                class="rounded-lg border border-slate-200 bg-white px-3 py-1 text-xs font-semibold disabled:opacity-40 dark:border-slate-600 dark:bg-slate-800"
                :disabled="pageIncidents <= 1"
                @click="pageIncidents--"
              >
                Trước
              </button>
              <button
                type="button"
                class="rounded-lg border border-slate-200 bg-white px-3 py-1 text-xs font-semibold disabled:opacity-40 dark:border-slate-600 dark:bg-slate-800"
                :disabled="pageIncidents >= totalPagesIncidents"
                @click="pageIncidents++"
              >
                Sau
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>

    <div v-if="showIncidentModal && selectedIncident" class="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="incident-modal-title" @click.self="closeIncidentReport">
      <div class="modal-content w-full max-w-4xl flex flex-col">
        <header class="modal-header shrink-0">
          <div class="flex items-center gap-3">
            <div class="size-10 rounded-xl bg-primary/10 dark:bg-primary/20 flex items-center justify-center">
              <span class="material-symbols-outlined text-primary dark:text-amber-300 text-xl">report</span>
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
                <p class="text-slate-900 dark:text-slate-100 font-medium">{{ proctorDisplayName }}</p>
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
                <p class="text-lg font-bold mt-1">{{ selectedIncident.warningTimeline[1]?.time ?? '—' }}</p>
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
import { computed, onMounted, ref, watch } from 'vue'
import { ApiError, unwrapApiData } from '../../services/apiClient'
import { displayNameFromMe, fetchMyProfile } from '../../services/authService'
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
const filterTimeRange = ref('30d')
const filterViolationType = ref('all')
const incidentSearch = ref('')
const meProfile = ref(null)

const proctorDisplayName = computed(() => displayNameFromMe(meProfile.value) || '—')

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
  const submittedAtMs = submittedAt ? new Date(String(submittedAt)).getTime() : 0
  const firstWarningTime = formatTime(attempt.startedAt)
  const latestWarningTime = formatTime(submittedAt)

  return {
    attemptId: attempt.id,
    student: attempt.student || 'Sinh viên không rõ',
    studentId: `AT-${attempt.id}`,
    submittedAtMs,
    violationKind: suspicious ? 'suspicious' : 'risk',
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

const MS_24H = 24 * 60 * 60 * 1000
const MS_7D = 7 * MS_24H
const MS_30D = 30 * MS_24H

const filteredIncidents = computed(() => {
  const now = Date.now()
  const windowMs =
    filterTimeRange.value === '24h' ? MS_24H : filterTimeRange.value === '7d' ? MS_7D : MS_30D
  const q = incidentSearch.value.trim().toLowerCase()

  return incidents.value.filter((inc) => {
    if (inc.submittedAtMs > 0 && now - inc.submittedAtMs > windowMs) return false

    const v = filterViolationType.value
    // BE chỉ có cờ suspicious + riskScore; map UI: tab→đáng ngờ, AI→mức CAO, thiết bị→vượt ngưỡng nhưng chưa CAO
    if (v === 'tab' && inc.violationKind !== 'suspicious') return false
    if (v === 'ai' && inc.severity !== 'CAO') return false
    if (v === 'device' && (inc.violationKind !== 'risk' || inc.severity === 'CAO')) return false

    if (q) {
      const name = String(inc.student || '').toLowerCase()
      const id = String(inc.studentId || '').toLowerCase()
      if (!name.includes(q) && !id.includes(q)) return false
    }
    return true
  })
})

const INC_PAGE_SIZE = 10
const pageIncidents = ref(1)

const paginatedIncidents = computed(() => {
  const list = filteredIncidents.value
  const start = (pageIncidents.value - 1) * INC_PAGE_SIZE
  return list.slice(start, start + INC_PAGE_SIZE)
})

const totalPagesIncidents = computed(() => Math.max(1, Math.ceil(filteredIncidents.value.length / INC_PAGE_SIZE)))

watch([attempts, filterTimeRange, filterViolationType, incidentSearch], () => {
  pageIncidents.value = 1
})

const summaryCards = computed(() => {
  const total = filteredIncidents.value.length
  const high = filteredIncidents.value.filter((incident) => incident.severity === 'CAO').length
  const resolved = filteredIncidents.value.filter((incident) => incident.result !== 'Đang xem xét').length
  const commonViolation = filteredIncidents.value.length ? filteredIncidents.value[0].violation : '-'
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
  exportToCsv(filteredIncidents.value, columns, `bao-cao-gian-lan-${safeTitle}.csv`)
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

onMounted(async () => {
  try {
    const raw = await fetchMyProfile()
    meProfile.value = unwrapApiData(raw)
  } catch {
    meProfile.value = null
  }
  await loadIncidentData()
})
</script>

