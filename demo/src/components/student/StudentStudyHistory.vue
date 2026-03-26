<template>
  <div :class="isDark ? 'dark' : 'light'" class="portal-viewport flex h-full min-h-0 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100">
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
      <StudentTopHeader class="shrink-0" />

      <main class="teacher-page-shell relative flex min-h-0 w-full flex-1 flex-col overflow-hidden">
        <div class="staff-page-wrap relative min-h-0 flex-1 flex-col gap-4">
          <PageHeader
            class="shrink-0 animate-fade-up"
            eyebrow="Student"
            title="Lịch sử / Kết quả"
            subtitle="Xem lại các phiên thi và luyện tập đã hoàn thành."
          />

          <div class="flex shrink-0 gap-4 border-b border-slate-200 pb-px animate-fade-up-delay dark:border-slate-800">
            <button
              type="button"
              @click="setTab('exam')"
              :class="activeTab === 'exam'
                ? 'border-b-2 border-primary pb-3 px-2 text-sm font-semibold text-primary'
                : 'pb-3 px-2 text-sm font-medium text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 transition-colors'"
            >
              Bài thi
            </button>
            <button
              type="button"
              @click="setTab('practice')"
              :class="activeTab === 'practice'
                ? 'border-b-2 border-primary pb-3 px-2 text-sm font-semibold text-primary'
                : 'pb-3 px-2 text-sm font-medium text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 transition-colors'"
            >
              Luyện tập
            </button>
          </div>


          <div class="staff-table-panel flex min-h-0 flex-1 flex-col overflow-hidden rounded-[1.5rem] animate-fade-up-delay">
            <div class="portal-scrollbar min-h-0 flex-1 overflow-auto">
              <table class="staff-table min-w-[720px] text-left">
                <thead>
                  <tr>
                    <th>Bài</th>
                    <th>Ngày làm</th>
                    <th>Điểm</th>
                    <th class="text-right">Thời gian làm bài</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="isLoading">
                    <td colspan="4" class="py-6 space-y-3" aria-busy="true">
                      <SkeletonLoader variant="table-row" />
                      <SkeletonLoader variant="table-row" />
                      <SkeletonLoader variant="table-row" />
                    </td>
                  </tr>
                  <tr v-else-if="!sessions.length">
                    <td colspan="4" class="px-2 py-4">
                      <EmptyState
                        icon="history"
                        :title="emptyMessage"
                        description="Tham gia bài thi hoặc luyện tập để xem tại đây."
                        action-label="Về Dashboard"
                        dense
                        fill
                        @action="goDashboard"
                      />
                    </td>
                  </tr>
                  <tr v-for="session in sessions" :key="session.attemptId" class="cursor-pointer transition-colors hover:bg-slate-50/80 dark:hover:bg-slate-800/40" @click="goToExamResult(session)">
                    <td class="py-5">
                      <div class="flex items-center gap-3">
                        <div :class="session.iconClass" class="h-10 w-10 rounded-lg flex items-center justify-center">
                          <span class="material-symbols-outlined">{{ session.icon }}</span>
                        </div>
                        <span class="font-semibold text-slate-900 dark:text-slate-100">{{ session.subject }}</span>
                      </div>
                    </td>
                    <td class="px-6 py-5 text-slate-600 dark:text-slate-400">{{ session.date }}</td>
                    <td class="px-6 py-5">
                      <div class="inline-flex items-center px-3 py-1 rounded-full bg-primary/10 text-primary font-bold text-sm">
                        {{ session.score }} / 10
                      </div>
                    </td>
                    <td class="py-5 text-right text-slate-600 dark:text-slate-400 font-medium">{{ session.timeTaken }}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="flex shrink-0 flex-wrap items-center justify-between gap-2 border-t border-slate-200/90 bg-slate-50/90 px-4 py-3 dark:border-slate-800 dark:bg-slate-900/40">
              <span class="text-sm text-slate-500 dark:text-slate-400">Hiển thị {{ allSessions.length }} phiên</span>
              <div class="flex items-center gap-2">
                <button
                  type="button"
                  @click="goToPrevPage"
                  :disabled="currentPage <= 1"
                  class="p-2 rounded-lg border border-slate-200 dark:border-slate-700 hover:bg-white dark:hover:bg-slate-800 text-slate-600 dark:text-slate-300 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <span class="material-symbols-outlined">chevron_left</span>
                </button>
                <span class="text-sm font-medium text-slate-600 dark:text-slate-300">Trang {{ currentPage }} / {{ totalPages }}</span>
                <button
                  type="button"
                  @click="goToNextPage"
                  :disabled="currentPage >= totalPages"
                  class="p-2 rounded-lg border border-slate-200 dark:border-slate-700 hover:bg-white dark:hover:bg-slate-800 text-slate-600 dark:text-slate-300 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <span class="material-symbols-outlined">chevron_right</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listMyAttempts } from '../../services/attemptService'
import { useToast } from '../../composables/useToast'
import StudentTopHeader from './StudentTopHeader.vue'
import PageHeader from '../shared/PageHeader.vue'
import SkeletonLoader from '../shared/SkeletonLoader.vue'
import EmptyState from '../shared/EmptyState.vue'

const router = useRouter()
const route = useRoute()
const isDark = ref(false)
const attempts = ref([])
const isLoading = ref(false)
const toast = useToast()
const activeTab = computed(() => (route.query.tab === 'practice' ? 'practice' : 'exam'))

const detectAttemptType = (attempt) => (attempt.isPractice ? 'practice' : 'exam')

const PAGE_SIZE = 8
const currentPage = ref(1)

const allSessions = computed(() => attempts.value
  .filter((attempt) => detectAttemptType(attempt) === activeTab.value)
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
    const isPractice = detectAttemptType(attempt) === 'practice'

    return {
      subject,
      date: submittedAt ? submittedAt.toLocaleDateString() : '-',
      score: (Number(attempt.score || 0) / 10).toFixed(1),
      timeTaken: durationMinutes ? `${durationMinutes}m` : '-',
      icon: isPractice ? 'model_training' : 'menu_book',
      iconClass: isPractice
        ? 'bg-violet-50 dark:bg-violet-900/20 text-violet-600 dark:text-violet-400'
        : 'bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400',
      attemptId: attempt.id,
      examId: attempt.examId,
      examTitle: attempt.examTitle || subject,
      attemptedAt: submittedAt ? submittedAt.toLocaleString() : '-'
    }
  }))

const sessions = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return allSessions.value.slice(start, start + PAGE_SIZE)
})

const totalPages = computed(() => Math.max(1, Math.ceil(allSessions.value.length / PAGE_SIZE)))

const goToPrevPage = () => {
  if (currentPage.value > 1) currentPage.value--
}

const goToNextPage = () => {
  if (currentPage.value < totalPages.value) currentPage.value++
}

const emptyMessage = computed(() => {
  if (activeTab.value === 'practice') {
    return 'Chưa có lượt luyện tập nào.'
  }
  return 'Chưa có bài thi nào.'
})

watch(activeTab, () => {
  currentPage.value = 1
})

const setTab = (tab) => {
  router.replace({
    path: '/student/study-history',
    query: { tab }
  })
}

const goDashboard = () => {
  router.push('/student/dashboard')
}

const goToExamResult = (session) => {
  router.push({
    path: '/student/exam-result',
    query: {
      attemptId: session.attemptId,
      examTitle: session.examTitle
    }
  })
}

const loadAttempts = async () => {
  isLoading.value = true
  try {
    // Backend trả về tất cả khi không truyền type; lọc theo tab trên client
    attempts.value = await listMyAttempts()
  } catch (error) {
    attempts.value = []
    toast.error('Không thể tải lịch sử tự học.')
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadAttempts()
})
</script>
