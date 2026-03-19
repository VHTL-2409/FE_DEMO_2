<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <div class="layout-container flex h-full grow flex-col">
      <StudentTopHeader />

      <main class="teacher-page-shell max-w-5xl">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

        <div class="relative space-y-8">
          <div class="flex flex-col md:flex-row md:items-end justify-between gap-6 animate-fade-up">
            <div class="space-y-1">
              <h1 class="text-3xl font-extrabold tracking-tight text-slate-900 dark:text-slate-100">Lịch sử/Kết quả</h1>
              <p class="text-slate-500 dark:text-slate-400">Theo dõi lại kết quả bài thi và các phiên luyện tập của bạn.</p>
            </div>
          </div>

          <div class="flex gap-4 border-b border-slate-200 dark:border-slate-800 pb-px animate-fade-up-delay">
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


          <div class="teacher-card overflow-hidden shadow-sm animate-fade-up-delay">
            <div class="overflow-x-auto">
              <table class="w-full text-left border-collapse">
                <thead>
                  <tr class="bg-slate-50 dark:bg-slate-800/50 border-b border-slate-200 dark:border-slate-800">
                    <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">Bài</th>
                    <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">Ngày làm</th>
                    <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">Điểm</th>
                    <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 text-right">Thời gian làm bài</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-200 dark:divide-slate-800">
                  <tr v-if="isLoading" class="text-sm text-slate-500">
                    <td colspan="4" class="px-6 py-8 text-center">Đang tải lịch sử...</td>
                  </tr>
                  <tr v-else-if="!sessions.length" class="text-sm text-slate-500">
                    <td colspan="4" class="px-6 py-8 text-center">{{ emptyMessage }}</td>
                  </tr>
                  <tr v-for="session in sessions" :key="session.attemptId" @click="goToExamResult(session)" class="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors group cursor-pointer hover:-translate-y-0.5">
                    <td class="px-6 py-5">
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
                    <td class="px-6 py-5 text-right text-slate-600 dark:text-slate-400 font-medium">{{ session.timeTaken }}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="px-6 py-4 bg-slate-50 dark:bg-slate-800/50 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between">
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

      <footer class="mt-auto px-6 md:px-20 lg:px-40 py-8 border-t border-slate-200 dark:border-slate-800 text-center">
        <p class="text-slate-500 dark:text-slate-400 text-sm">© 2026 Hệ thống thi trực tuyến ExamPortal. Bảo lưu mọi quyền.</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listMyAttempts } from '../../services/attemptService'
import { useToast } from '../../composables/useToast'
import StudentTopHeader from './StudentTopHeader.vue'

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

const goToExamResult = (session) => {
  router.push({
    path: '/student/exam-result',
    query: {
      attemptId: session.attemptId,
      examTitle: session.examTitle
    }
  })
}

onMounted(async () => {
  isLoading.value = true

  try {
    attempts.value = await listMyAttempts({ type: activeTab.value })
  } catch (error) {
    attempts.value = []
    toast.error('Không thể tải lịch sử tự học.')
  } finally {
    isLoading.value = false
  }
})
</script>
