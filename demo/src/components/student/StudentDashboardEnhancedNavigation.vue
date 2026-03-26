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

        <div class="relative grid grid-cols-1 lg:grid-cols-3 gap-4 lg:gap-6 animate-fade-up-delay lg:min-h-0 lg:items-stretch">
          <div class="lg:col-span-2 space-y-4 lg:space-y-6 min-h-0">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 md:gap-5">
              <div class="staff-surface portal-card-lift rounded-[1.75rem] p-6 flex flex-col gap-4">
                <div class="flex items-center gap-3">
                  <span class="material-symbols-outlined p-2.5 bg-primary/10 text-primary rounded-xl text-2xl">login</span>
                  <h2 class="text-xl font-bold">Thi qua mã</h2>
                </div>
                <BaseButton
                  class="mt-auto w-full md:w-auto"
                  size="lg"
                  @click="goToExamJoin"
                >
                  Đi đến thi qua mã
                </BaseButton>
              </div>

              <div class="staff-surface portal-card-lift rounded-[1.75rem] p-6 flex flex-col gap-4">
                <div class="flex items-center gap-3">
                  <span class="material-symbols-outlined p-2.5 bg-primary/10 text-primary rounded-xl text-2xl">model_training</span>
                  <h2 class="text-xl font-bold">Tự luyện tập</h2>
                </div>
                <BaseButton class="mt-auto w-full shadow-lg shadow-primary/20 md:w-auto" size="lg" @click="goToPractice">
                  Đi đến luyện tập
                </BaseButton>
              </div>
            </div>

            <div class="staff-surface portal-card-lift rounded-[1.75rem] p-6">
              <div class="flex items-center justify-between flex-wrap gap-3 mb-5">
                <div class="flex items-center gap-3">
                  <span class="material-symbols-outlined p-2.5 bg-primary/10 text-primary rounded-xl text-2xl">history</span>
                  <h2 class="text-xl font-bold">Lịch sử/Kết quả</h2>
                </div>
                <button
                  type="button"
                  @click="goToStudyHistory"
                  class="text-sm font-bold text-primary hover:underline"
                >
                  Xem tất cả
                </button>
              </div>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <button
                  type="button"
                  @click="goToStudyHistoryTab('exam')"
                  class="p-4 rounded-xl border border-primary/15 bg-background-light dark:bg-background-dark text-left hover:border-primary/40 portal-card-lift transition-[border-color,transform,box-shadow] duration-200 ease-out portal-focus"
                >
                  <p class="text-base font-bold">Bài thi</p>
                </button>

                <button
                  type="button"
                  @click="goToStudyHistoryTab('practice')"
                  class="p-4 rounded-xl border border-primary/15 bg-background-light dark:bg-background-dark text-left hover:border-primary/40 portal-card-lift transition-[border-color,transform,box-shadow] duration-200 ease-out portal-focus"
                >
                  <p class="text-base font-bold">Luyện tập</p>
                </button>
              </div>
            </div>
          </div>

          <div class="lg:col-span-1 flex min-h-0 flex-col lg:min-h-[min(36vh,320px)]">
            <div class="staff-surface portal-card-lift flex min-h-0 flex-1 flex-col overflow-hidden rounded-[1.75rem]">
              <div class="shrink-0 p-6 border-b border-primary/10">
                <div class="flex items-center gap-3">
                  <span class="material-symbols-outlined text-primary text-3xl">insights</span>
                  <h2 class="text-xl font-bold">Hoạt động gần đây</h2>
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
                  <div
                    v-for="item in historyItems"
                    :key="item.attemptId"
                    @click="goToExamResult(item)"
                    class="p-4 border-b border-primary/5 hover:bg-background-light dark:hover:bg-background-dark transition-colors duration-150 cursor-pointer"
                  >
                    <div class="flex justify-between items-start mb-1">
                      <p class="font-bold text-slate-800 dark:text-slate-200">{{ item.title }}</p>
                      <span class="text-primary font-bold">{{ item.score }} / 10</span>
                    </div>
                    <div class="flex justify-between text-xs text-slate-500">
                      <span>{{ item.date }}</span>
                      <span>{{ item.grade }}</span>
                    </div>
                  </div>
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
import PageHeader from '../shared/PageHeader.vue'
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
