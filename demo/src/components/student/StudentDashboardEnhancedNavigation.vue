<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="portal-viewport flex h-full min-h-0 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100"
  >
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
      <StudentTopHeader class="shrink-0" />

      <main class="teacher-page-shell flex min-h-0 w-full flex-1 flex-col overflow-hidden">
        <div class="portal-scrollbar flex min-h-0 flex-1 flex-col overflow-y-auto overscroll-contain">
        <PageHeader
          class="animate-fade-up text-primary dark:[&_.teacher-page-title]:text-slate-100"
          eyebrow="Student Workspace"
          title="Khu vực học sinh"
          subtitle="Đi nhanh tới kỳ thi, luyện tập và lịch sử học tập trong một màn hình gọn gàng hơn."
        />

        <!-- Cards grid with staggered animation -->
        <div class="relative grid grid-cols-1 lg:grid-cols-3 gap-4 lg:gap-6 animate-fade-up-delay lg:min-h-0 lg:items-stretch" style="animation-delay: 0.05s;">
          <div class="lg:col-span-2 space-y-4 lg:space-y-6 min-h-0">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 md:gap-5">
              <div class="staff-surface portal-card-lift rounded-[1.75rem] p-6 flex flex-col gap-4 relative overflow-hidden group transition-all duration-300 hover:shadow-xl hover:shadow-primary/10 hover:-translate-y-1">
                <div class="absolute inset-0 bg-gradient-to-br from-primary/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 rounded-[1.75rem]"></div>
                <div class="relative flex items-center gap-3">
                  <div class="p-2.5 rounded-xl bg-primary/10 text-primary">
                    <LucideIcon name="login" size="24" />
                  </div>
                  <h2 class="text-xl font-bold">Thi qua mã</h2>
                </div>
                <BaseButton
                  class="mt-auto w-full md:w-auto relative z-10 transition-all duration-200 hover:shadow-lg hover:shadow-primary/20 hover:-translate-y-0.5"
                  size="lg"
                  @click="goToExamJoin"
                >
                  Đi đến thi qua mã
                </BaseButton>
              </div>

              <div class="staff-surface portal-card-lift rounded-[1.75rem] p-6 flex flex-col gap-4 relative overflow-hidden group transition-all duration-300 hover:shadow-xl hover:shadow-primary/10 hover:-translate-y-1">
                <div class="absolute inset-0 bg-gradient-to-br from-amber-500/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 rounded-[1.75rem]"></div>
                <div class="relative flex items-center gap-3">
                  <div class="p-2.5 rounded-xl bg-amber-500/10 text-amber-600 dark:text-amber-400">
                    <LucideIcon name="model_training" size="24" />
                  </div>
                  <h2 class="text-xl font-bold">Tự luyện tập</h2>
                </div>
                <BaseButton class="mt-auto w-full relative z-10 transition-all duration-200 hover:shadow-lg hover:shadow-amber-500/20 hover:-translate-y-0.5 md:w-auto" size="lg" @click="goToPractice">
                  Đi đến luyện tập
                </BaseButton>
              </div>
            </div>

            <div class="staff-surface portal-card-lift rounded-[1.75rem] p-6 relative overflow-hidden">
              <div class="flex items-center justify-between flex-wrap gap-3 mb-5">
                <div class="flex items-center gap-3">
                  <div class="p-2 rounded-lg bg-primary/10">
                    <LucideIcon name="history" size="20" class="text-primary" />
                  </div>
                  <h2 class="text-xl font-bold">Lịch sử/Kết quả</h2>
                </div>
                <button
                  type="button"
                  @click="goToStudyHistory"
                  class="text-sm font-bold text-primary hover:underline transition-all duration-200 hover:scale-105"
                >
                  Xem tất cả
                </button>
              </div>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <button
                  type="button"
                  @click="goToStudyHistoryTab('exam')"
                  class="p-5 rounded-2xl border-2 border-primary/15 bg-gradient-to-br from-white to-primary/5 dark:from-slate-800 dark:to-primary/10 text-left hover:border-primary/40 hover:shadow-lg hover:shadow-primary/5 portal-card-lift transition-all duration-200 ease-out portal-focus group"
                >
                  <div class="flex items-center gap-3 mb-2">
                    <div class="p-2 rounded-lg bg-primary/10 group-hover:bg-primary/20 transition-colors">
                      <LucideIcon name="quiz" size="18" class="text-primary" />
                    </div>
                    <p class="text-base font-bold">Bài thi</p>
                  </div>
                  <p class="text-xs text-slate-500 dark:text-slate-400">Xem lại kết quả các bài thi đã làm</p>
                </button>

                <button
                  type="button"
                  @click="goToStudyHistoryTab('practice')"
                  class="p-5 rounded-2xl border-2 border-amber-500/15 bg-gradient-to-br from-white to-amber-500/5 dark:from-slate-800 dark:to-amber-500/10 text-left hover:border-amber-500/40 hover:shadow-lg hover:shadow-amber-500/5 portal-card-lift transition-all duration-200 ease-out portal-focus group"
                >
                  <div class="flex items-center gap-3 mb-2">
                    <div class="p-2 rounded-lg bg-amber-500/10 group-hover:bg-amber-500/20 transition-colors">
                      <LucideIcon name="school" size="18" class="text-amber-600 dark:text-amber-400" />
                    </div>
                    <p class="text-base font-bold">Luyện tập</p>
                  </div>
                  <p class="text-xs text-slate-500 dark:text-slate-400">Các bài luyện tập cá nhân</p>
                </button>
              </div>
            </div>
          </div>

          <div class="lg:col-span-1 flex min-h-0 flex-col lg:min-h-[min(36vh,320px)]">
            <div class="staff-surface portal-card-lift flex min-h-0 flex-1 flex-col overflow-hidden rounded-[1.75rem] relative">
              <div class="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-primary via-primary/50 to-transparent rounded-t-[1.75rem]"></div>
              <div class="shrink-0 p-6 border-b border-primary/10">
                <div class="flex items-center gap-3">
                  <div class="p-2 rounded-lg bg-primary/10">
                    <LucideIcon name="insights" size="22" class="text-primary" />
                  </div>
                  <h2 class="text-lg font-bold">Hoạt động gần đây</h2>
                </div>
              </div>

              <div class="min-h-0 flex-1 p-0">
                <template v-if="isLoadingAttempts">
                  <div class="p-4 space-y-4" aria-busy="true" aria-label="Đang tải lịch sử">
                    <SkeletonLoader variant="table-row" />
                    <SkeletonLoader variant="table-row" />
                    <SkeletonLoader variant="table-row" />
                    <SkeletonLoader variant="table-row" />
                  </div>
                </template>
                <EmptyState
                  v-else-if="!historyItems.length"
                  icon="edit_note"
                  title="Chưa có hoạt động gần đây"
                  description="Chưa có lượt thi hoặc luyện tập."
                  action-label="Tìm bài thi"
                  class="py-6"
                  @action="goToExamJoin"
                />
                <template v-else>
                  <TransitionGroup 
                    name="list" 
                    tag="div"
                    class="divide-y divide-primary/5"
                  >
                    <div
                      v-for="item in historyItems"
                      :key="item.attemptId"
                      @click="goToExamResult(item)"
                      class="p-4 hover:bg-gradient-to-r hover:from-primary/5 hover:to-transparent dark:hover:from-primary/10 transition-all duration-200 cursor-pointer group"
                    >
                      <div class="flex justify-between items-start mb-1.5">
                        <p class="font-semibold text-slate-800 dark:text-slate-200 group-hover:text-primary transition-colors">{{ item.title }}</p>
                        <span class="px-2.5 py-0.5 rounded-full text-xs font-bold" :class="Number(item.score) >= 5 ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300' : 'bg-rose-100 text-rose-700 dark:bg-rose-900/30 dark:text-rose-300'">
                          {{ item.score }} / 10
                        </span>
                      </div>
                      <div class="flex justify-between text-xs text-slate-500">
                        <span>{{ item.date }}</span>
                        <span class="px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-800 font-medium">{{ item.grade }}</span>
                      </div>
                    </div>
                  </TransitionGroup>
                </template>
              </div>
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
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listMyAttempts } from '../../services/attemptService'
import { useToast } from '../../composables/useToast'
import StudentTopHeader from './StudentTopHeader.vue'
import BaseButton from '../shared/BaseButton.vue'
import EmptyState from '../shared/EmptyState.vue'
import PageHeader from '../ui/PageHeader.vue'
import SkeletonLoader from '../shared/SkeletonLoader.vue'

const router = useRouter()
const isDark = ref(false)
const attempts = ref([])
const isLoadingAttempts = ref(false)
const toast = useToast()

const historyItems = computed(() => attempts.value
  .slice()
  .sort((a, b) => {
    const aTime = new Date(a.submittedAt || a.startedAt || 0).getTime()
    const bTime = new Date(b.submittedAt || b.startedAt || 0).getTime()
    return (Number.isNaN(bTime) ? 0 : bTime) - (Number.isNaN(aTime) ? 0 : aTime)
  })
  .slice(0, 8)
  .map((attempt) => ({
    title: attempt.examTitle || 'Bài thi',
    score: (Number(attempt.score || 0) / 10).toFixed(1),
    date: attempt.submittedAt ? new Date(attempt.submittedAt).toLocaleDateString() : '-',
    grade: attempt.status || 'SUBMITTED',
    attemptId: attempt.id,
    examId: attempt.examId,
    accuracy: (Number(attempt.score || 0) / 10).toFixed(1),
    time: attempt.startedAt && attempt.submittedAt
      ? `${Math.max(1, Math.round((new Date(attempt.submittedAt).getTime() - new Date(attempt.startedAt).getTime()) / 60000))}m`
      : '-'
  })))

const goToExamJoin = () => {
  router.push('/student/exam-join')
}

const goToPractice = () => {
  router.push('/student/generate-practice-test')
}

const goToStudyHistory = () => {
  router.push('/student/study-history')
}

const goToStudyHistoryTab = (tab) => {
  router.push({
    path: '/student/study-history',
    query: { tab }
  })
}

const goToExamResult = (item) => {
  router.push({
    path: '/student/exam-result',
    query: {
      attemptId: item.attemptId,
      examTitle: item.title
    }
  })
}

onMounted(async () => {
  isLoadingAttempts.value = true
  try {
    attempts.value = await listMyAttempts()
  } catch (error) {
    attempts.value = []
    toast.error('Không thể tải lịch sử bài thi.')
  } finally {
    isLoadingAttempts.value = false
  }
})
</script>

<style scoped>
/* List transition for history items */
.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

.list-move {
  transition: transform 0.3s ease;
}

/* Optimized animations with will-change */
.animate-fade-up,
.animate-fade-up-delay {
  will-change: transform, opacity;
  backface-visibility: hidden;
}
</style>
