<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen"
  >
    <div class="layout-container flex h-full grow flex-col">
      <StudentTopHeader />

      <main class="teacher-page-shell max-w-6xl">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

        <section class="relative flex flex-col gap-2 animate-fade-up mb-8">
          <h1 class="text-3xl md:text-4xl font-black tracking-tight text-primary dark:text-slate-100">Dashboard sinh viên</h1>
          <p class="text-slate-600 dark:text-slate-400 text-lg">Chọn nhanh khu vực bạn cần: thi qua mã, luyện tập hoặc xem kết quả.</p>
        </section>

        <div class="relative grid grid-cols-1 lg:grid-cols-3 gap-8 animate-fade-up-delay">
          <div class="lg:col-span-2 space-y-8">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div class="teacher-card p-6 flex flex-col gap-4 rounded-2xl shadow-soft hover:shadow-card-hover transition-all duration-200">
                <div class="flex items-center gap-3">
                  <span class="material-symbols-outlined p-2.5 bg-primary/10 text-primary rounded-xl text-2xl">login</span>
                  <h2 class="text-xl font-bold">Thi qua mã</h2>
                </div>
                <p class="text-sm text-slate-500 dark:text-slate-400">Nhập mã/tiêu đề bài thi để vào phòng chờ và bắt đầu làm bài.</p>
                <button
                  type="button"
                  @click="goToExamJoin"
                  class="mt-auto w-full md:w-auto px-6 py-3 bg-primary text-white font-bold rounded-lg hover:bg-primary/90 hover:-translate-y-0.5 transition-all duration-200"
                >
                  Đi đến thi qua mã
                </button>
              </div>

              <div class="teacher-card p-6 flex flex-col gap-4 rounded-2xl shadow-soft hover:shadow-card-hover transition-all duration-200">
                <div class="flex items-center gap-3">
                  <span class="material-symbols-outlined p-2.5 bg-primary/10 text-primary rounded-xl text-2xl">model_training</span>
                  <h2 class="text-xl font-bold">Tự luyện tập</h2>
                </div>
                <p class="text-sm text-slate-500 dark:text-slate-400">Tạo đề luyện tập nhanh từ ngân hàng câu hỏi và làm bài ngay.</p>
                <button
                  type="button"
                  @click="goToPractice"
                  class="mt-auto w-full md:w-auto px-6 py-3 bg-primary text-white font-bold rounded-xl hover:bg-primary/90 hover:-translate-y-0.5 shadow-lg shadow-primary/20 transition-all duration-200"
                >
                  Đi đến luyện tập
                </button>
              </div>
            </div>

            <div class="teacher-card p-6 rounded-2xl shadow-soft">
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
                  class="p-4 rounded-xl border border-primary/15 bg-background-light dark:bg-background-dark text-left hover:border-primary/40 hover:-translate-y-0.5 transition-all"
                >
                  <p class="text-sm text-slate-500">Tab lịch sử</p>
                  <p class="text-base font-bold mt-1">Bài thi</p>
                </button>

                <button
                  type="button"
                  @click="goToStudyHistoryTab('practice')"
                  class="p-4 rounded-xl border border-primary/15 bg-background-light dark:bg-background-dark text-left hover:border-primary/40 hover:-translate-y-0.5 transition-all"
                >
                  <p class="text-sm text-slate-500">Tab lịch sử</p>
                  <p class="text-base font-bold mt-1">Luyện tập</p>
                </button>
              </div>
            </div>
          </div>

          <div class="lg:col-span-1">
            <div class="teacher-card flex flex-col h-full">
              <div class="p-6 border-b border-primary/10">
                <div class="flex items-center gap-3">
                  <span class="material-symbols-outlined text-primary text-3xl">insights</span>
                  <h2 class="text-xl font-bold">Hoạt động gần đây</h2>
                </div>
              </div>

              <div class="p-0 overflow-y-auto max-h-[520px]">
                <p v-if="isLoadingAttempts" class="p-4 text-sm text-slate-500">Đang tải lịch sử bài thi...</p>
                <p v-else-if="!historyItems.length" class="p-4 text-sm text-slate-500">Chưa có lượt làm bài nào.</p>

                <div
                  v-for="item in historyItems"
                  v-else
                  :key="item.attemptId"
                  @click="goToExamResult(item)"
                  class="p-4 border-b border-primary/5 hover:bg-background-light dark:hover:bg-background-dark transition-colors cursor-pointer"
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
              </div>
            </div>
          </div>
        </div>
      </main>

      <footer class="bg-white dark:bg-background-dark border-t border-primary/10 py-6 px-10 text-center text-slate-500 text-sm">
        <p>© 2026 Hệ thống thi trực tuyến ExamPortal. Bảo lưu mọi quyền.</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listMyAttempts } from '../../services/attemptService'
import { useToast } from '../../composables/useToast'
import StudentTopHeader from './StudentTopHeader.vue'

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
