<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto w-full px-3 pb-10 pt-4 sm:px-5 lg:px-8 xl:px-10" style="max-width: 1640px">

      <!-- Header -->
      <div class="mb-6 ds-animate-fade-up">
        <h1 class="m-0 text-xl font-extrabold leading-tight" style="color: var(--ds-text)">Lịch &amp; kết quả</h1>
        <p class="mt-1 mb-0 text-sm font-medium" style="color: var(--ds-text-muted)">
          <template v-if="activeTab === 'upcoming'">Kỳ thi từ lớp · {{ upcomingExamCount }} kỳ</template>
          <template v-else>{{ totalCount }} bài đã làm · {{ totalPractice }} luyện tập · {{ totalExam }} bài thi</template>
        </p>
      </div>

      <!-- Tab list -->
      <div class="mb-6 ds-animate-fade-up ssh-tabs-wrap" style="animation-delay: 0.04s">
        <div class="ssh-tabs" role="tablist">
          <button
            role="tab"
            type="button"
            class="ssh-tab"
            :class="{ 'ssh-tab--active': activeTab === 'upcoming' }"
            :aria-selected="activeTab === 'upcoming'"
            @click="setTab('upcoming')"
          >
            <LucideIcon name="calendar_month" :size="16" />
            Sắp tới
            <span class="ssh-tab__count">{{ upcomingExamCount }}</span>
          </button>
          <button
            role="tab"
            type="button"
            class="ssh-tab"
            :class="{ 'ssh-tab--active': activeTab === 'exam' }"
            :aria-selected="activeTab === 'exam'"
            @click="setTab('exam')"
          >
            <LucideIcon name="file_text" :size="16" />
            Bài thi đã làm
            <span class="ssh-tab__count">{{ tabCounts.exam }}</span>
          </button>
          <button
            role="tab"
            type="button"
            class="ssh-tab"
            :class="{ 'ssh-tab--active': activeTab === 'practice' }"
            :aria-selected="activeTab === 'practice'"
            @click="setTab('practice')"
          >
            <LucideIcon name="pencil" :size="16" />
            Luyện tập
            <span class="ssh-tab__count">{{ tabCounts.practice }}</span>
          </button>
        </div>
      </div>

      <!-- Embedded schedule -->
      <div v-show="activeTab === 'upcoming'" class="mb-6 ds-animate-fade-up ssh-embed">
        <StudentExamSchedule embedded />
      </div>

      <template v-if="activeTab !== 'upcoming'">
        <!-- Summary + Trend row -->
        <div class="mb-6 grid grid-cols-1 gap-5 xl:grid-cols-2 xl:items-start min-w-0 isolate ds-animate-fade-up" style="animation-delay: 0.06s">
          <ResultSummaryCard :attempts="filteredAttempts" />
          <ScoreTrendCard :attempts="filteredAttempts" />
        </div>

        <!-- History section header -->
        <div class="mb-4 flex items-center justify-between ds-animate-fade-up" style="animation-delay: 0.1s">
          <h2 class="m-0 text-base font-extrabold" style="color: var(--ds-text)">Danh sách bài đã làm</h2>
          <span class="text-xs font-semibold" style="color: var(--ds-text-muted)">
            Hiển thị {{ sessions.length }} / {{ totalCount }} · Trang {{ currentPage }} / {{ totalPages }}
          </span>
        </div>

        <!-- History list -->
        <div class="ds-animate-fade-up" style="animation-delay: 0.13s">
          <ResultHistoryList
            :sessions="sessions"
            :loading="isLoading"
            :page-loading="isPageLoading"
            :current-page="currentPage"
            :total-pages="totalPages"
            :total-count="totalCount"
            :selected-id="null"
            @session-click="openSession"
            @go-dashboard="goDashboard"
            @prev-page="goToPrevPage"
            @next-page="goToNextPage"
          />
        </div>
      </template>

    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listMyAttempts } from '../../services/attemptService'
import { getMyClasses, getStudentClassExams } from '../../services/classService'
import { useToast } from '../../composables/useToast'
import { buildResultQuery } from '../../services/studentExamContextStorage'
import { formatScoreTen } from '../../utils/attemptResult'
import LucideIcon from '../common/LucideIcon.vue'
import ResultSummaryCard from './results/ResultSummaryCard.vue'
import ResultHistoryList from './results/ResultHistoryList.vue'
import ScoreTrendCard from './results/ScoreTrendCard.vue'
import StudentExamSchedule from './StudentExamSchedule.vue'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const VALID_TABS = new Set(['upcoming', 'exam', 'practice'])
const upcomingExamCount = ref(0)

// ── Tab state ────────────────────────────────────────────────────────────────────
const activeTab = ref('exam')

const readTabFromRoute = () => {
  const t = String(route.query.tab || '')
  if (VALID_TABS.has(t)) activeTab.value = t
  else activeTab.value = 'exam'
}

const setTab = (t) => {
  if (!VALID_TABS.has(t)) return
  activeTab.value = t
  const q = { ...route.query }
  if (t === 'exam') delete q.tab
  else q.tab = t
  router.replace({ path: route.path, query: q })
}

const filteredAttempts = computed(() =>
  activeTab.value === 'practice'
    ? allAttempts.value.filter(a => a.isPractice)
    : allAttempts.value.filter(a => !a.isPractice)
)

const tabCounts = computed(() => ({ exam: totalExam.value, practice: totalPractice.value }))

watch(activeTab, () => { currentPage.value = 1 })

watch(() => route.query.tab, () => { readTabFromRoute() })

const loadUpcomingExamCount = async () => {
  try {
    const classes = await getMyClasses()
    const examGroups = await Promise.all(
      (classes || []).map(async (cls) => {
        try {
          const exams = await getStudentClassExams(cls.id)
          return exams || []
        } catch {
          return []
        }
      })
    )
    const now = Date.now()
    upcomingExamCount.value = examGroups
      .flat()
      .filter((exam) => {
        const endMs = new Date(exam.endTime || '').getTime()
        return Number.isNaN(endMs) || endMs >= now
      }).length
  } catch {
    upcomingExamCount.value = 0
  }
}

// ── Session list ────────────────────────────────────────────────────────────────
const attempts = ref([])
const isLoading = ref(false)
const isPageLoading = ref(false)
const PAGE_SIZE = 10
const currentPage = ref(1)

const allAttempts = computed(() =>
  [...attempts.value].sort((a, b) => {
    const at = new Date(a.submittedAt || a.startedAt || 0).getTime()
    const bt = new Date(b.submittedAt || b.startedAt || 0).getTime()
    return (Number.isNaN(bt) ? 0 : bt) - (Number.isNaN(at) ? 0 : at)
  })
)

const totalCount = computed(() => filteredAttempts.value.length)
const totalPractice = computed(() => allAttempts.value.filter(a => a.isPractice).length)
const totalExam = computed(() => allAttempts.value.filter(a => !a.isPractice).length)

const sessions = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return filteredAttempts.value.slice(start, start + PAGE_SIZE).map(a => {
    const startedAt = a.startedAt ? new Date(a.startedAt) : null
    const submittedAt = a.submittedAt ? new Date(a.submittedAt) : null
    const durationMinutes = startedAt && submittedAt
      ? Math.max(1, Math.round((submittedAt.getTime() - startedAt.getTime()) / 60000))
      : 0
    return {
      subject: a.examTitle || 'Bài thi',
      date: submittedAt
        ? submittedAt.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
        : '-',
      score: formatScoreTen(a.score || 0),
      timeTaken: durationMinutes ? `${durationMinutes}m` : '-',
      isPractice: !!a.isPractice,
      attemptId: a.id,
      examId: a.examId,
      examTitle: a.examTitle || 'Bài thi',
      attemptedAt: submittedAt ? submittedAt.toLocaleString('vi-VN') : '-',
      isNew: Date.now() - (submittedAt?.getTime() || 0) < 7 * 24 * 60 * 60 * 1000,
    }
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(totalCount.value / PAGE_SIZE)))

const goToPrevPage = () => {
  if (currentPage.value > 1) {
    isPageLoading.value = true
    currentPage.value--
    setTimeout(() => { isPageLoading.value = false }, 300)
  }
}

const goToNextPage = () => {
  if (currentPage.value < totalPages.value) {
    isPageLoading.value = true
    currentPage.value++
    setTimeout(() => { isPageLoading.value = false }, 300)
  }
}

const goDashboard = () => router.push('/student/dashboard')

// ── Navigate to result detail ────────────────────────────────────────────────────
const openSession = (session) => {
  router.push({
    path: '/student/exam-result',
    query: buildResultQuery({
      attemptId: session.attemptId,
      examTitle: session.examTitle
    })
  })
}

const loadAttempts = async () => {
  isLoading.value = true
  try { attempts.value = await listMyAttempts() }
  catch { attempts.value = []; toast.error('Không thể tải lịch sử học tập.') }
  finally { isLoading.value = false }
}

onMounted(() => {
  readTabFromRoute()
  loadAttempts()
  loadUpcomingExamCount()
})
</script>

<style scoped>
.ssh-tabs-wrap {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.ssh-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  background: var(--ds-surface-raised, var(--ds-surface));
  border: 1px solid var(--ds-border);
  border-radius: 12px;
  padding: 4px;
  width: fit-content;
  max-width: 100%;
}

.ssh-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--ds-text-muted);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, box-shadow 0.15s;
  white-space: nowrap;
}

.ssh-tab:hover {
  background: var(--ds-bg);
  color: var(--ds-text);
}

.ssh-tab--active {
  background: var(--ds-bg);
  color: var(--ds-primary);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
}

.ssh-tab__count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: var(--ds-border);
  color: var(--ds-text-muted);
  font-size: 0.75rem;
  font-weight: 700;
  line-height: 1;
}

.ssh-tab--active .ssh-tab__count {
  background: var(--ds-primary-bg, var(--ds-info-bg));
  color: var(--ds-primary, var(--ds-info));
}

@media (prefers-reduced-motion: reduce) {
  .ssh-tab {
    transition: none;
  }
}
</style>
