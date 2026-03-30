<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto max-w-4xl px-4 pb-10 pt-4 sm:px-6 lg:px-8">
      <div class="flex min-h-0 flex-1 flex-col gap-5">
        <div class="ds-animate-fade-up">
          <StudentResultsHeader
            :active-tab="activeTab"
            :tab-counts="tabCounts"
            :summary-stats="headerSummaryStats"
            @tab-change="setTab"
          />
        </div>
        
        <!-- Stats cards -->
        <div class="grid gap-4 md:grid-cols-3 ds-animate-fade-up" style="animation-delay: 0.05s">
          <div class="rounded-2xl border border-[var(--ds-border)] bg-gradient-to-br from-[var(--ds-surface)] to-[var(--ds-primary-soft)] p-5 shadow-sm hover:shadow-md transition-shadow">
            <div class="flex items-center gap-3">
              <div class="p-2.5 rounded-xl bg-[var(--ds-primary-soft)] text-[var(--ds-primary)]">
                <LucideIcon name="assignment" size="20" />
              </div>
              <div>
                <p class="text-xs text-[var(--ds-text-muted)] font-medium">Tổng bài thi</p>
                <p class="text-xl font-bold text-[var(--ds-text)]">{{ headerSummaryStats.totalCount }}</p>
              </div>
            </div>
          </div>
          <div class="rounded-2xl border border-[var(--ds-border)] bg-gradient-to-br from-[var(--ds-surface)] to-emerald-50 dark:to-emerald-900/10 p-5 shadow-sm hover:shadow-md transition-shadow">
            <div class="flex items-center gap-3">
              <div class="p-2.5 rounded-xl bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400">
                <LucideIcon name="trending_up" size="20" />
              </div>
              <div>
                <p class="text-xs text-[var(--ds-text-muted)] font-medium">Điểm trung bình</p>
                <p class="text-xl font-bold text-[var(--ds-text)]">{{ headerSummaryStats.avgScore }}</p>
              </div>
            </div>
          </div>
          <div class="rounded-2xl border border-[var(--ds-border)] bg-gradient-to-br from-[var(--ds-surface)] to-amber-50 dark:to-amber-900/10 p-5 shadow-sm hover:shadow-md transition-shadow">
            <div class="flex items-center gap-3">
              <div class="p-2.5 rounded-xl bg-amber-100 dark:bg-amber-900/30 text-amber-600 dark:text-amber-400">
                <LucideIcon name="star" size="20" />
              </div>
              <div>
                <p class="text-xs text-[var(--ds-text-muted)] font-medium">Điểm mới nhất</p>
                <p class="text-xl font-bold text-[var(--ds-text)]">{{ headerSummaryStats.latestScore }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Results + Trend -->
        <div class="grid gap-4 md:grid-cols-2 ds-animate-fade-up" style="animation-delay: 0.08s">
          <ResultSummaryCard :attempts="tabAttempts" />
          <ScoreTrendCard :attempts="tabAttempts" />
        </div>
        
        <!-- History list -->
        <div class="ds-animate-fade-up" style="animation-delay: 0.12s">
          <ResultHistoryList
            :sessions="mappedSessions"
            :loading="isLoading"
            :empty-type="activeTab"
            :current-page="currentPage"
            :total-pages="totalPages"
            :total-count="allSessions.length"
            @session-click="goToExamResult"
            @go-dashboard="goDashboard"
            @prev-page="goToPrevPage"
            @next-page="goToNextPage"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listMyAttempts } from '../../services/attemptService'
import { useToast } from '../../composables/useToast'
import StudentResultsHeader from './results/StudentResultsHeader.vue'
import ResultSummaryCard from './results/ResultSummaryCard.vue'
import ResultHistoryList from './results/ResultHistoryList.vue'
import ScoreTrendCard from './results/ScoreTrendCard.vue'

const router = useRouter()
const route = useRoute()
const attempts = ref([])
const isLoading = ref(false)
const toast = useToast()
const activeTab = computed(() => route.query.tab === 'practice' ? 'practice' : 'exam')
const PAGE_SIZE = 8
const currentPage = ref(1)

const isRecent = (ts) => {
  if (!ts) return false
  return Date.now() - new Date(ts).getTime() < 7 * 24 * 60 * 60 * 1000
}

const detectAttemptType = (attempt) => attempt.practiceExamId ? 'practice' : 'exam'

const tabCounts = computed(() => ({
  exam: attempts.value.filter(a => detectAttemptType(a) === 'exam').length,
  practice: attempts.value.filter(a => detectAttemptType(a) === 'practice').length
}))

const headerSummaryStats = computed(() => {
  const scores = tabAttempts.value.map(a => Number(a.score || 0)).filter(s => s > 0)
  const avg = scores.length ? (scores.reduce((a, b) => a + b, 0) / scores.length / 10).toFixed(1) : '-'
  const sorted = tabAttempts.value.slice().sort((a, b) => {
    const at = new Date(a.submittedAt || a.startedAt || 0).getTime()
    const bt = new Date(b.submittedAt || b.startedAt || 0).getTime()
    return bt - at
  })
  return {
    totalCount: tabAttempts.value.length,
    avgScore: avg,
    latestScore: sorted.length ? (Number(sorted[0].score || 0) / 10).toFixed(1) : '-'
  }
})

const tabAttempts = computed(() =>
  attempts.value.filter(a => detectAttemptType(a) === activeTab.value)
)

const allSessions = computed(() =>
  tabAttempts.value
    .slice()
    .sort((a, b) => {
      const aTime = new Date(a.submittedAt || a.startedAt || 0).getTime()
      const bTime = new Date(b.submittedAt || b.startedAt || 0).getTime()
      return (Number.isNaN(bTime) ? 0 : bTime) - (Number.isNaN(aTime) ? 0 : aTime)
    })
    .map((attempt) => {
      const startedAt = attempt.startedAt ? new Date(attempt.startedAt) : null
      const submittedAt = attempt.submittedAt ? new Date(attempt.submittedAt) : null
      const durationMinutes = startedAt && submittedAt
        ? Math.max(1, Math.round((submittedAt.getTime() - startedAt.getTime()) / 60000))
        : 0
      const subject = attempt.examTitle || 'Bài thi'
      return {
        subject,
        date: submittedAt ? submittedAt.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '-',
        score: (Number(attempt.score || 0) / 10).toFixed(1),
        timeTaken: durationMinutes ? `${durationMinutes}m` : '-',
        isPractice: !!attempt.practiceExamId,
        attemptId: attempt.id,
        examId: attempt.examId,
        examTitle: attempt.examTitle || subject,
        attemptedAt: submittedAt ? submittedAt.toLocaleString('vi-VN') : '-',
        isNew: isRecent(attempt.submittedAt || attempt.startedAt)
      }
    })
)

const sessions = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return allSessions.value.slice(start, start + PAGE_SIZE)
})

const totalPages = computed(() => Math.max(1, Math.ceil(allSessions.value.length / PAGE_SIZE)))
const mappedSessions = computed(() => sessions.value)

const goToPrevPage = () => { if (currentPage.value > 1) currentPage.value-- }
const goToNextPage = () => { if (currentPage.value < totalPages.value) currentPage.value++ }
const setTab = (tab) => { currentPage.value = 1; router.replace({ path: '/student/study-history', query: { tab } }) }
const goDashboard = () => router.push('/student/dashboard')
const goToExamResult = (session) => {
  router.push({ path: '/student/exam-result', query: { attemptId: session.attemptId, examTitle: session.examTitle } })
}
const loadAttempts = async () => {
  isLoading.value = true
  try { attempts.value = await listMyAttempts() }
  catch { attempts.value = []; toast.error('Không thể tải lịch sử học tập.') }
  finally { isLoading.value = false }
}
watch(activeTab, () => { currentPage.value = 1 })
onMounted(loadAttempts)
</script>
