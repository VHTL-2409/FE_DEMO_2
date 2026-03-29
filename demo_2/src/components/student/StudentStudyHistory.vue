<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="portal-viewport flex h-full min-h-0 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100"
  >
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
        <StudentTopHeader active-section="history" class="shrink-0" />

        <main
          class="teacher-page-shell student-stitch-main relative flex min-h-0 w-full max-w-screen-2xl flex-1 flex-col overflow-hidden"
        >
          <div
            class="student-stitch-paper-bg portal-scrollbar flex min-h-0 flex-1 flex-col overflow-y-auto overscroll-contain px-3 pb-8 pt-3 sm:px-5 lg:px-8"
          >
            <nav class="mb-2 flex items-center gap-2 text-xs font-medium text-slate-500">
              <RouterLink to="/student/dashboard" class="transition hover:text-primary">Trang chủ</RouterLink>
              <span class="material-symbols-outlined text-[14px] text-slate-400">chevron_right</span>
              <span class="font-semibold text-primary">Lịch sử &amp; kết quả</span>
            </nav>

            <!-- stitch_new/study_history — Academic History -->
            <header class="mb-4 w-full">
              <h1
                class="stitch-font-headline text-2xl font-bold leading-tight tracking-tight text-[#1b1c1a] dark:text-amber-100 sm:text-3xl md:text-4xl"
              >
                Lịch sử học tập
              </h1>
            </header>

            <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div class="flex flex-wrap items-center gap-2">
                <button
                  type="button"
                  class="silk-press-gradient inline-flex items-center gap-1.5 rounded-xl px-4 py-2 text-xs font-bold text-white shadow-md shadow-primary/20 sm:text-sm"
                  @click="goToRecommendedPractice"
                >
                  <span class="material-symbols-outlined text-lg">play_circle</span>
                  Luyện nhanh
                </button>
                <RouterLink
                  to="/student/generate-practice-test"
                  class="inline-flex items-center gap-1 rounded-xl border border-[#dbc2b0]/50 px-4 py-2 text-xs font-bold text-[#191c1e] transition hover:bg-[#f4f4f0] dark:border-slate-600 dark:text-slate-100 dark:hover:bg-slate-800 sm:text-sm"
                >
                  <span class="material-symbols-outlined text-base">upload_file</span>
                  Tạo luyện tập
                </RouterLink>
              </div>
            </div>

            <!-- Tabs + search — stitch_new/study_history -->
            <section class="mb-4 mt-2 w-full">
              <div
                class="flex flex-col gap-3 border-b border-[#dbc2b0]/30 pb-3 dark:border-slate-700 md:flex-row md:items-center md:justify-between"
              >
                <div
                  class="student-stitch-segment w-full min-w-0 sm:w-auto sm:min-w-[16rem]"
                  role="tablist"
                  aria-label="Lọc theo loại bài"
                >
                  <button
                    type="button"
                    role="tab"
                    :aria-selected="activeTab === 'exam'"
                    class="student-stitch-segment__btn"
                    :class="{ 'student-stitch-segment__btn--active': activeTab === 'exam' }"
                    @click="setTab('exam')"
                  >
                    Bài thi
                  </button>
                  <button
                    type="button"
                    role="tab"
                    :aria-selected="activeTab === 'practice'"
                    class="student-stitch-segment__btn"
                    :class="{ 'student-stitch-segment__btn--active': activeTab === 'practice' }"
                    @click="setTab('practice')"
                  >
                    Luyện tập
                  </button>
                </div>
                <div class="group relative w-full md:w-80 md:max-w-sm">
                  <span
                    class="material-symbols-outlined pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 transition-colors group-focus-within:text-primary"
                  >
                    search
                  </span>
                  <input
                    v-model="searchQuery"
                    type="search"
                    autocomplete="off"
                    placeholder="Tìm theo tên bài…"
                    class="w-full rounded-t-lg border-0 border-b-2 border-transparent bg-[#f4f4f0] py-3 pl-12 pr-4 text-sm transition-all focus:border-primary focus:outline-none focus:ring-0 dark:bg-slate-800/80 dark:text-slate-100"
                  />
                </div>
              </div>
            </section>

            <!-- Danh sách dạng thẻ — dễ quét, không cần cuộn ngang -->
            <div
              class="mb-8 flex min-h-0 w-full flex-1 flex-col overflow-hidden rounded-2xl border border-[#dbc2b0]/25 bg-[#f3f1ec]/40 shadow-sm dark:border-slate-700 dark:bg-slate-900/35"
            >
              <div class="portal-scrollbar min-h-0 flex-1 overflow-y-auto p-2 sm:p-3">
                <div v-if="isLoading" class="space-y-2" aria-busy="true">
                  <div v-for="n in 3" :key="n" class="rounded-xl border border-[#dbc2b0]/20 bg-white p-4 dark:border-slate-700 dark:bg-slate-900/50">
                    <SkeletonLoader variant="table-row" />
                  </div>
                </div>
                <div
                  v-else-if="!pagedRows.length"
                  class="flex flex-col items-center justify-center rounded-xl border border-dashed border-[#dbc2b0]/40 bg-white/60 px-6 py-14 text-center dark:border-slate-600 dark:bg-slate-900/40"
                >
                  <span class="material-symbols-outlined mb-3 text-4xl text-slate-300 dark:text-slate-600">inventory_2</span>
                  <p class="max-w-sm text-sm leading-relaxed text-slate-600 dark:text-slate-400">{{ emptyMessage }}</p>
                  <button
                    type="button"
                    class="mt-5 rounded-full bg-primary px-5 py-2 text-sm font-bold text-white shadow-md transition hover:opacity-95 dark:bg-amber-800"
                    @click="goDashboard"
                  >
                    Về Dashboard
                  </button>
                </div>
                <ul v-else role="list" class="space-y-2">
                  <li
                    v-for="session in pagedRows"
                    :key="session.attemptId"
                    class="group cursor-pointer rounded-xl border border-[#e4ddd4]/90 bg-white p-4 shadow-sm transition hover:border-primary/30 hover:shadow-md dark:border-slate-700/90 dark:bg-slate-900/65 dark:hover:border-amber-700/50 sm:p-4"
                    @click="goToExamResult(session)"
                  >
                    <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
                      <div class="min-w-0 flex-1">
                        <div class="flex items-start justify-between gap-2 sm:justify-start sm:gap-3">
                          <h3
                            class="stitch-font-headline text-[0.95rem] font-semibold leading-snug text-[#1b1c1a] group-hover:text-primary dark:text-slate-100 sm:text-base"
                          >
                            {{ session.subject }}
                          </h3>
                          <span
                            class="material-symbols-outlined shrink-0 text-xl text-slate-300 transition group-hover:translate-x-0.5 group-hover:text-primary/70 dark:text-slate-600"
                            aria-hidden="true"
                          >
                            chevron_right
                          </span>
                        </div>
                        <div
                          class="mt-2.5 flex flex-wrap items-center gap-x-3 gap-y-1.5 text-[0.8125rem] leading-relaxed text-slate-500 dark:text-slate-400"
                        >
                          <span class="inline-flex items-center gap-1 rounded-md bg-[#f4f4f0] px-2 py-0.5 font-medium text-slate-700 dark:bg-slate-800 dark:text-slate-300">
                            {{ session.kindLabel }}
                          </span>
                          <span class="text-slate-300 dark:text-slate-600" aria-hidden="true">·</span>
                          <span class="tabular-nums">{{ session.durationLabel }}</span>
                          <span class="text-slate-300 dark:text-slate-600" aria-hidden="true">·</span>
                          <span class="tabular-nums">{{ session.date }}</span>
                        </div>
                      </div>
                      <div
                        class="flex shrink-0 flex-row items-center justify-between gap-3 border-t border-[#eee9e2] pt-3 sm:min-w-[9.5rem] sm:flex-col sm:items-end sm:justify-center sm:gap-2 sm:border-t-0 sm:pt-0 sm:pl-3"
                      >
                        <div class="text-left sm:text-right">
                          <template v-if="session.scoreDisplay !== '—'">
                            <span class="stitch-font-headline text-2xl font-bold tabular-nums text-primary dark:text-amber-200">
                              {{ session.scoreDisplay }}
                            </span>
                            <span class="ml-0.5 text-xs font-medium text-slate-400">/10</span>
                          </template>
                          <span v-else class="text-lg font-medium text-slate-400">—</span>
                        </div>
                        <span
                          class="inline-flex max-w-[9rem] items-center justify-center text-center text-[11px] font-semibold leading-snug sm:max-w-none"
                          :class="[
                            session.statusBadgeClass,
                            'rounded-full px-3 py-1.5 shadow-sm'
                          ]"
                        >
                          {{ session.statusLabel }}
                        </span>
                      </div>
                    </div>
                  </li>
                </ul>
              </div>

              <div
                class="flex shrink-0 flex-wrap items-center justify-between gap-3 border-t border-[#dbc2b0]/25 bg-white/90 px-3 py-3 sm:px-4 dark:border-slate-700 dark:bg-slate-900/80"
              >
                <span class="text-xs font-medium text-slate-500 dark:text-slate-400">
                  {{ filteredRows.length }} kết quả
                  <span v-if="filteredRows.length" class="text-slate-400"> · Trang {{ currentPage }}/{{ totalPages }}</span>
                </span>
                <div class="flex items-center gap-1">
                  <button
                    type="button"
                    class="inline-flex size-9 items-center justify-center rounded-full border border-slate-200 text-slate-600 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-35 dark:border-slate-600 dark:hover:bg-slate-800"
                    :disabled="currentPage <= 1"
                    aria-label="Trang trước"
                    @click="goToPrevPage"
                  >
                    <span class="material-symbols-outlined text-xl">chevron_left</span>
                  </button>
                  <button
                    type="button"
                    class="inline-flex size-9 items-center justify-center rounded-full border border-slate-200 text-slate-600 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-35 dark:border-slate-600 dark:hover:bg-slate-800"
                    :disabled="currentPage >= totalPages"
                    aria-label="Trang sau"
                    @click="goToNextPage"
                  >
                    <span class="material-symbols-outlined text-xl">chevron_right</span>
                  </button>
                </div>
              </div>
            </div>

            <!-- stitch_new/study_history — Summary Statistics -->
            <section class="mt-2 flex w-full flex-col gap-4 sm:gap-6 lg:flex-row lg:items-stretch">
              <div
                class="flex flex-1 items-center gap-6 rounded-xl border border-[#dbc2b0]/10 bg-[#e9e8e4] p-6 dark:border-slate-600/50 dark:bg-slate-800/60"
              >
                <div
                  class="flex size-16 shrink-0 items-center justify-center rounded-full bg-primary text-white shadow-sm"
                >
                  <span class="material-symbols-outlined text-3xl">analytics</span>
                </div>
                <div>
                  <p class="stitch-font-headline text-3xl font-bold tabular-nums text-primary md:text-4xl">
                    {{ statsSummary.avgDisplay }}
                  </p>
                  <p class="mt-1 text-[11px] font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
                    Điểm TB (thang 10)
                  </p>
                </div>
              </div>
              <div
                class="flex flex-1 items-center gap-6 rounded-xl border border-[#dbc2b0]/10 bg-[#efeeea] p-6 dark:border-slate-600/50 dark:bg-slate-800/40"
              >
                <div
                  class="flex size-16 shrink-0 items-center justify-center rounded-full bg-[#e3e2df] text-amber-900 dark:bg-slate-700 dark:text-amber-200"
                >
                  <span class="material-symbols-outlined text-3xl">assignment_turned_in</span>
                </div>
                <div>
                  <p class="stitch-font-headline text-3xl font-bold tabular-nums text-amber-900 dark:text-amber-100 md:text-4xl">
                    {{ statsSummary.total }}
                  </p>
                  <p class="mt-1 text-[11px] font-semibold uppercase tracking-[0.2em] text-slate-500 dark:text-slate-400">
                    Tổng lượt đã ghi nhận
                  </p>
                </div>
              </div>
              <div
                class="relative flex flex-1 items-center justify-between gap-4 overflow-hidden rounded-xl bg-gradient-to-br from-[#6e3900] to-[#3d2200] p-6 text-white"
              >
                <div class="relative z-10 min-w-0">
                  <p class="stitch-font-headline text-lg font-bold md:text-xl">Báo cáo nhanh</p>
                  <p class="mt-1 text-[10px] font-semibold uppercase tracking-widest text-white/75">Xuất CSV từ bảng hiện tại</p>
                </div>
                <button
                  type="button"
                  class="relative z-10 flex size-12 shrink-0 items-center justify-center rounded-lg bg-white/20 backdrop-blur-md transition hover:bg-white/30"
                  :disabled="!filteredRows.length"
                  aria-label="Tải CSV"
                  @click="exportHistoryCsv"
                >
                  <span class="material-symbols-outlined">download</span>
                </button>
                <span
                  class="pointer-events-none absolute -bottom-2 -right-2 rotate-12 text-[120px] opacity-10"
                  aria-hidden="true"
                >
                  <span class="material-symbols-outlined">menu_book</span>
                </span>
              </div>
            </section>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { listMyAttempts } from '../../services/attemptService'
import { useToast } from '../../composables/useToast'
import { scoreOnTenDisplay } from '../../utils/scoreDisplay'
import { exportToCsv } from '../../utils/reportExport'
import StudentTopHeader from './StudentTopHeader.vue'
import SkeletonLoader from '../shared/SkeletonLoader.vue'

const router = useRouter()
const route = useRoute()
const isDark = ref(false)
const attempts = ref([])
const isLoading = ref(false)
const toast = useToast()
const searchQuery = ref('')
const activeTab = computed(() => (route.query.tab === 'practice' ? 'practice' : 'exam'))

const PAGE_SIZE = 5
const currentPage = ref(1)

const allSessions = computed(() =>
  attempts.value
    .filter((a) => a.submittedAt || a.startedAt)
    .slice()
    .sort((a, b) => {
      const aTime = new Date(a.submittedAt || a.startedAt || 0).getTime()
      const bTime = new Date(b.submittedAt || b.startedAt || 0).getTime()
      return (Number.isNaN(bTime) ? 0 : bTime) - (Number.isNaN(aTime) ? 0 : aTime)
    })
    .map((attempt) => {
      const startedAt = attempt.startedAt ? new Date(attempt.startedAt) : null
      const submittedAt = attempt.submittedAt ? new Date(attempt.submittedAt) : null
      const durationMinutes =
        startedAt && submittedAt
          ? Math.max(1, Math.round((submittedAt.getTime() - startedAt.getTime()) / 60000))
          : 0
      const subject = attempt.examTitle || 'Bài thi'
      const isPractice = Boolean(attempt.isPractice)
      const kind = isPractice ? 'Luyện tập' : 'Bài thi'
      const timePart = durationMinutes ? `${durationMinutes} phút` : '—'
      const scoreDisplay = scoreOnTenDisplay(attempt.score)
      const pending = scoreDisplay === '—'
      const n = parseFloat(scoreDisplay, 10)
      const statusLabel = pending ? 'Chờ chấm' : n >= 9.5 ? 'Xuất sắc' : 'Hoàn thành'
      const statusBadgeClass = pending
        ? 'bg-[#cee5ff]/55 text-[#004a75] dark:bg-sky-950/60 dark:text-sky-200'
        : n >= 9.5
          ? 'bg-[#ffc329]/25 text-[#5c4300] dark:bg-amber-900/45 dark:text-amber-100'
          : 'bg-[#ffc329]/20 text-[#6f5100] dark:bg-amber-950/40 dark:text-amber-200'
      return {
        subject,
        kindLabel: kind,
        durationLabel: timePart,
        metaLine: `${kind} · ${timePart}`,
        date: submittedAt ? submittedAt.toLocaleDateString('vi-VN') : '—',
        scoreDisplay,
        timeTaken: durationMinutes ? `${durationMinutes} phút` : '—',
        statusLabel,
        statusBadgeClass,
        icon: isPractice ? 'model_training' : 'menu_book',
        iconClass: isPractice
          ? 'bg-amber-100 text-amber-900 dark:bg-amber-950/50 dark:text-amber-200'
          : 'bg-[#efeeea] text-primary dark:bg-slate-800 dark:text-amber-300',
        attemptId: attempt.id,
        examId: attempt.examId,
        examTitle: attempt.examTitle || subject
      }
    })
)

const filteredRows = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return allSessions.value
  return allSessions.value.filter((s) => s.subject.toLowerCase().includes(q))
})

const statsSummary = computed(() => {
  const rows = filteredRows.value
  const nums = rows.map((r) => parseFloat(r.scoreDisplay, 10)).filter((n) => !Number.isNaN(n))
  const avgDisplay = nums.length
    ? (nums.reduce((a, b) => a + b, 0) / nums.length).toFixed(2)
    : '—'
  return { avgDisplay, total: rows.length }
})

const exportHistoryCsv = () => {
  const tab = activeTab.value === 'practice' ? 'luyen-tap' : 'bai-thi'
  exportToCsv(
    filteredRows.value,
    [
      { key: 'subject', label: 'Tên bài' },
      { key: 'kindLabel', label: 'Loại' },
      { key: 'durationLabel', label: 'Thời gian làm' },
      { key: 'date', label: 'Ngày nộp' },
      { key: 'scoreDisplay', label: 'Điểm (/10)' },
      { key: 'statusLabel', label: 'Trạng thái' }
    ],
    `lich-su-hoc-tap-${tab}.csv`
  )
}

const totalPages = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / PAGE_SIZE)))

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return filteredRows.value.slice(start, start + PAGE_SIZE)
})

const goToPrevPage = () => {
  if (currentPage.value > 1) currentPage.value--
}

const goToNextPage = () => {
  if (currentPage.value < totalPages.value) currentPage.value++
}

const emptyMessage = computed(() => {
  if (searchQuery.value.trim() && filteredRows.value.length === 0) {
    return 'Không có kết quả khớp tìm kiếm.'
  }
  return activeTab.value === 'practice' ? 'Chưa có lượt luyện tập.' : 'Chưa có bài thi.'
})

const setTab = (tab) => {
  router.replace({ path: '/student/study-history', query: { tab } })
}

const goDashboard = () => {
  router.push('/student/dashboard')
}

const goToRecommendedPractice = () => {
  router.push({
    path: '/student/generate-practice-test',
    query: { source: 'study-history', mode: 'warmup', questionCount: '12', durationMinutes: '20' }
  })
}

const goToExamResult = (session) => {
  router.push({
    path: '/student/exam-result',
    query: { attemptId: session.attemptId, examTitle: session.examTitle }
  })
}

const loadAttempts = async () => {
  isLoading.value = true
  try {
    const type = activeTab.value === 'practice' ? 'practice' : 'exam'
    attempts.value = await listMyAttempts({ type })
  } catch {
    attempts.value = []
    toast.error('Không thể tải lịch sử.')
  } finally {
    isLoading.value = false
  }
}

watch(activeTab, () => {
  currentPage.value = 1
  searchQuery.value = ''
  loadAttempts()
})

watch(searchQuery, () => {
  currentPage.value = 1
})

watch(filteredRows, (rows) => {
  const maxPage = Math.max(1, Math.ceil(rows.length / PAGE_SIZE))
  if (currentPage.value > maxPage) currentPage.value = maxPage
})

onMounted(() => {
  loadAttempts()
})
</script>
