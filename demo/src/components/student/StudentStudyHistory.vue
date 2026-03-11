<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 antialiased min-h-screen">
    <div class="relative flex h-auto min-h-screen w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full grow flex-col">
        <StudentTopHeader />

        <main class="relative flex-1 px-6 md:px-20 lg:px-40 py-8 overflow-hidden">
          <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
          <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

          <div class="relative max-w-5xl mx-auto space-y-8">
            <div class="flex flex-col md:flex-row md:items-end justify-between gap-6 animate-fade-up">
              <div class="space-y-1">
                <h1 class="text-3xl font-extrabold tracking-tight text-slate-900 dark:text-slate-100">Kết quả tự học</h1>
                <p class="text-slate-500 dark:text-slate-400">Theo dõi và xem lại hiệu suất các phiên học trước đây của bạn.</p>
              </div>
            </div>

            <div class="flex gap-4 border-b border-slate-200 dark:border-slate-800 pb-px animate-fade-up-delay">
              <button class="border-b-2 border-primary pb-3 px-2 text-sm font-semibold text-primary" type="button">Tất cả phiên</button>
              <button class="pb-3 px-2 text-sm font-medium text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 transition-colors" type="button">Toán học</button>
              <button class="pb-3 px-2 text-sm font-medium text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 transition-colors" type="button">Khoa học</button>
              <button class="pb-3 px-2 text-sm font-medium text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 transition-colors" type="button">Ngôn ngữ</button>
            </div>

            <div class="bg-white dark:bg-slate-900/50 rounded-xl border border-slate-200 dark:border-slate-800 overflow-hidden shadow-sm animate-fade-up-delay">
              <div class="overflow-x-auto">
                <table class="w-full text-left border-collapse">
                  <thead>
                    <tr class="bg-slate-50 dark:bg-slate-800/50 border-b border-slate-200 dark:border-slate-800">
                      <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">Môn học</th>
                      <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">Ngày thi</th>
                      <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">Điểm</th>
                      <th class="px-6 py-4 text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 text-right">Thời gian làm bài</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-slate-200 dark:divide-slate-800">
                    <tr v-for="session in sessions" :key="session.subject + session.date" @click="goToExamResult(session)" class="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors group cursor-pointer hover:-translate-y-0.5">
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
                          {{ session.score }} / 100
                        </div>
                      </td>
                      <td class="px-6 py-5 text-right text-slate-600 dark:text-slate-400 font-medium">{{ session.timeTaken }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div class="px-6 py-4 bg-slate-50 dark:bg-slate-800/50 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between">
                <span class="text-sm text-slate-500 dark:text-slate-400">Hiển thị 1 đến 5 trong 24 phiên</span>
                <div class="flex items-center gap-2">
                  <button class="p-2 rounded-lg border border-slate-200 dark:border-slate-700 hover:bg-white dark:hover:bg-slate-800 text-slate-400 disabled:opacity-50" disabled type="button">
                    <span class="material-symbols-outlined">chevron_left</span>
                  </button>
                  <button class="h-8 w-8 rounded-lg bg-primary text-white text-sm font-bold" type="button">1</button>
                  <button class="h-8 w-8 rounded-lg text-slate-600 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-800 text-sm font-medium" type="button">2</button>
                  <button class="h-8 w-8 rounded-lg text-slate-600 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-800 text-sm font-medium" type="button">3</button>
                  <button class="p-2 rounded-lg border border-slate-200 dark:border-slate-700 hover:bg-white dark:hover:bg-slate-800 text-slate-400" type="button">
                    <span class="material-symbols-outlined">chevron_right</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </main>

        <footer class="mt-auto px-6 md:px-20 lg:px-40 py-8 border-t border-slate-200 dark:border-slate-800 text-center">
          <p class="text-slate-500 dark:text-slate-400 text-sm">© 2023 StudyPortal. Mọi hồ sơ học tập đều riêng tư và được bảo mật.</p>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { listMyAttempts } from '../../services/attemptService'
import { useRouter } from 'vue-router'
import StudentTopHeader from './StudentTopHeader.vue'

const router = useRouter()
const isDark = ref(false)
const attempts = ref([])

const sessions = computed(() => attempts.value
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

    return {
      subject: attempt.examTitle || `Bài thi #${attempt.examId}`,
      date: submittedAt ? submittedAt.toLocaleDateString() : '-',
      score: Math.round(Number(attempt.score || 0)),
      timeTaken: durationMinutes ? `${durationMinutes}m` : '-',
      icon: 'menu_book',
      iconClass: 'bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400',
      attemptId: attempt.id,
      examId: attempt.examId,
      attemptedAt: submittedAt ? submittedAt.toLocaleString() : '-'
    }
  }))

const goToExamResult = (session) => {
  router.push({
    path: '/student/exam-result',
    query: {
      exam: session.subject,
      attemptId: session.attemptId,
      examId: session.examId,
      score: session.score,
      attempted: `Đã làm lúc ${session.attemptedAt}`,
      time: session.timeTaken,
      accuracy: session.score
    }
  })
}

onMounted(async () => {
  try {
    attempts.value = await listMyAttempts()
  } catch {
    attempts.value = []
  }
})
</script>

<style scoped>
.font-display {
  font-family: 'Inter', sans-serif;
}

@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes floatSlow {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(0, -14px, 0);
  }
}

@keyframes floatDelay {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(0, 12px, 0);
  }
}

.animate-fade-up {
  animation: fadeUp 0.5s ease-out;
}

.animate-fade-up-delay {
  animation: fadeUp 0.65s ease-out;
}

.animate-float-slow {
  animation: floatSlow 7s ease-in-out infinite;
}

.animate-float-delay {
  animation: floatDelay 8s ease-in-out infinite;
}
</style>
