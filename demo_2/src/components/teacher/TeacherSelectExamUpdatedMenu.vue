<template>
  <div :class="isDark ? 'dark' : 'light'" class="flex h-full min-h-0 flex-1 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100">
    <div class="layout-container flex min-h-0 flex-1 grow flex-col">
      <TeacherTopHeader active-section="monitoring" />

      <main class="teacher-stitch-main teacher-page-shell portal-scrollbar relative min-h-0 w-full max-w-none flex-1 overflow-x-hidden overflow-y-auto px-3 py-4 sm:px-4 lg:px-5">
        <div class="monitoring-pick-shell w-full max-w-screen-2xl space-y-4 sm:space-y-5">
        <header class="relative flex w-full flex-wrap items-end justify-between gap-3 animate-fade-up">
          <div class="min-w-0">
            <p class="portal-kicker mb-1">
              <RouterLink to="/teacher/dashboard" class="text-slate-500 transition hover:text-[var(--role-primary)] dark:text-slate-400">Trang chủ</RouterLink>
              <span class="mx-1.5 text-slate-300 dark:text-slate-600">/</span>
              <span class="font-semibold text-[var(--role-primary)]">Giám sát</span>
            </p>
            <h1 class="stitch-font-headline text-2xl font-bold tracking-tight text-amber-900 dark:text-amber-100 md:text-3xl lg:text-4xl">
              Đề đang diễn ra
            </h1>
          </div>
          <button
            type="button"
            class="shrink-0 rounded-xl border border-primary/25 bg-white/80 px-4 py-2 text-sm font-bold text-primary shadow-sm transition hover:bg-primary/10 dark:bg-slate-900/60"
            @click="goToExamBank"
          >
            Ngân hàng đề
          </button>
        </header>

        <div class="relative animate-fade-up-delay">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-primary/50">search</span>
          <input
            v-model="search"
            class="stitch-registry-search w-full rounded-xl py-2.5 pl-10 pr-4 text-sm text-slate-900 outline-none transition-all dark:text-white"
            placeholder="Tìm tiêu đề hoặc mã…"
            type="text"
          />
        </div>

        <div class="stitch-ambient-shadow overflow-hidden rounded-xl border border-[color:rgba(219,194,176,0.12)] bg-white shadow-sm dark:border-slate-700 dark:bg-slate-900">
          <div v-if="isLoading" class="p-8 text-center text-sm text-slate-500 dark:text-slate-400">
            <span class="material-symbols-outlined mb-2 inline-block animate-spin text-xl text-primary">progress_activity</span>
            <p>Đang tải…</p>
          </div>
          <div v-else-if="!filteredExams.length" class="p-8">
            <EmptyState
              class="rounded-xl border border-dashed border-slate-200 bg-slate-50/80 dark:border-slate-700 dark:bg-slate-900/40"
              icon="monitoring"
              title="Không có đề trong giờ thi"
              description=""
              action-label="Ngân hàng đề"
              dense
              fill
              @action="goToExamBank"
            />
          </div>
          <div v-else class="teacher-stitch-table-scroll">
            <table class="w-full border-collapse text-left text-sm">
              <thead>
                <tr class="teacher-stitch-table-head border-b border-[color:rgba(219,194,176,0.15)] dark:border-slate-800">
                  <th class="px-4 py-3 text-[10px] font-bold uppercase tracking-widest text-slate-400">TT</th>
                  <th class="px-4 py-3 text-[10px] font-bold uppercase tracking-widest text-slate-400">ID</th>
                  <th class="px-4 py-3 text-[10px] font-bold uppercase tracking-widest text-slate-400">Đề thi</th>
                  <th class="hidden px-4 py-3 text-[10px] font-bold uppercase tracking-widest text-slate-400 sm:table-cell">Câu</th>
                  <th class="hidden px-4 py-3 text-[10px] font-bold uppercase tracking-widest text-slate-400 md:table-cell">Kết thúc</th>
                  <th class="px-4 py-3 text-right text-[10px] font-bold uppercase tracking-widest text-slate-400">Thao tác</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-[color:rgba(219,194,176,0.12)] dark:divide-slate-800">
                <tr
                  v-for="exam in filteredExams"
                  :key="exam.id"
                  :class="exam.cardBorder"
                  class="border-l-4 border-l-transparent transition-colors hover:bg-amber-50/40 dark:hover:bg-slate-800/30"
                >
                  <td class="px-4 py-3">
                    <span :class="exam.statusClass" class="inline-block rounded px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide">{{ exam.status }}</span>
                  </td>
                  <td class="px-4 py-3 font-mono text-xs text-slate-500">{{ exam.id }}</td>
                  <td class="max-w-[min(100%,20rem)] px-4 py-3">
                    <span class="font-semibold leading-snug text-slate-900 dark:text-white">{{ exam.title }}</span>
                  </td>
                  <td class="hidden px-4 py-3 tabular-nums text-slate-600 dark:text-slate-300 sm:table-cell">{{ exam.students }}</td>
                  <td class="hidden whitespace-nowrap px-4 py-3 text-slate-600 dark:text-slate-300 md:table-cell">{{ exam.timeValue }}</td>
                  <td class="px-4 py-3 text-right">
                    <button
                      :class="exam.actionClass"
                      class="inline-flex items-center gap-1 rounded-lg px-3 py-2 text-xs font-bold transition-all"
                      type="button"
                      @click="openLiveSession(exam)"
                    >
                      <span class="material-symbols-outlined text-base">{{ exam.actionIcon }}</span>
                      {{ exam.actionLabel }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <p v-if="monitoringCards.length" class="text-left text-xs font-medium text-slate-500 dark:text-slate-400">
          {{ monitoringCards.length }} đề đang mở
        </p>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ApiError } from '../../services/apiClient'
import { listExams } from '../../services/examService'
import { RouterLink, useRouter } from 'vue-router'
import { useToast } from '../../composables/useToast'
import TeacherTopHeader from './TeacherTopHeader.vue'
import EmptyState from '../shared/EmptyState.vue'

const router = useRouter()
const isDark = ref(false)
const search = ref('')
const exams = ref([])
const isLoading = ref(false)
const toast = useToast()

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString()
}

const isExamOngoing = (exam, nowMs) => {
  const startMs = new Date(exam.startTime || '').getTime()
  const endMs = new Date(exam.endTime || '').getTime()
  if (Number.isNaN(startMs) || Number.isNaN(endMs)) return false
  return Boolean(exam.isActive) && startMs <= nowMs && nowMs <= endMs
}

const monitoringCards = computed(() => {
  const nowMs = Date.now()
  return exams.value
    .filter((exam) => isExamOngoing(exam, nowMs))
    .map((exam) => ({
      id: exam.id,
      examId: exam.id,
      examCode: exam.code || '',
      title: exam.title,
      durationMinutes: exam.durationMinutes,
      location: exam.description || 'Đề thi trực tuyến',
      sessionMeta: `Bắt đầu: ${formatDateTime(exam.startTime)} • Kết thúc: ${formatDateTime(exam.endTime)}`,
      status: 'Live',
      statusClass: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
      students: `${exam.questionCount || 0}`,
      leftLabel: 'Số câu hỏi',
      timeLabel: 'Kết thúc',
      timeIcon: 'timer',
      timeIconClass: 'text-orange-500',
      timeValue: formatDateTime(exam.endTime),
      actionLabel: 'Theo dõi',
      actionIcon: 'visibility',
      actionClass: 'bg-primary text-white hover:bg-primary/90',
      cardBorder: 'border-l-green-500'
    }))
})

const filteredExams = computed(() => {
  const keyword = search.value.trim().toLowerCase()
  if (!keyword) return monitoringCards.value
  return monitoringCards.value.filter(
    (exam) =>
      exam.title.toLowerCase().includes(keyword) || String(exam.examCode || '').toLowerCase().includes(keyword)
  )
})

const goToExamBank = () => {
  router.push('/teacher/exams')
}

const openLiveSession = (exam) => {
  router.push({
    path: '/teacher/live-monitoring/session',
    query: {
      examId: exam.examId,
      title: exam.title,
      code: exam.examCode || '',
      meta: exam.sessionMeta,
      durationMinutes: exam.durationMinutes || ''
    }
  })
}

const loadExams = async () => {
  isLoading.value = true
  try {
    exams.value = await listExams()
  } catch (error) {
    toast.error(error instanceof ApiError ? error.message : 'Không thể tải danh sách đề thi.')
  } finally {
    isLoading.value = false
  }
}

onMounted(loadExams)
</script>
