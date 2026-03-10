<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark text-slate-900 dark:text-slate-100 min-h-screen flex flex-col font-display">
    <StudentTopHeader :show-profile="false" :show-sign-out="false" :show-notifications="false">
      <template #rightActions>
        <div class="hidden xl:flex items-center gap-3 mr-2 text-xs font-medium text-emerald-600 dark:text-emerald-400">
          <span class="material-symbols-outlined text-sm leading-none">videocam</span>
          <span>Camera On</span>
          <span class="w-1 h-1 rounded-full bg-slate-300 dark:bg-slate-700"></span>
          <span class="material-symbols-outlined text-sm leading-none">wifi</span>
          <span>Connected</span>
        </div>
        <div class="flex items-center gap-3 bg-slate-100 dark:bg-slate-800 px-4 py-2 rounded-xl border border-slate-200 dark:border-slate-700">
          <div class="flex flex-col items-center leading-none">
            <span class="text-sm font-bold tabular-nums">01</span>
            <span class="text-[9px] uppercase tracking-wider opacity-60">Hrs</span>
          </div>
          <span class="text-base font-light opacity-30">:</span>
          <div class="flex flex-col items-center leading-none">
            <span class="text-sm font-bold tabular-nums">29</span>
            <span class="text-[9px] uppercase tracking-wider opacity-60">Min</span>
          </div>
          <span class="text-base font-light opacity-30">:</span>
          <div class="flex flex-col items-center leading-none">
            <span class="text-sm font-bold tabular-nums">45</span>
            <span class="text-[9px] uppercase tracking-wider opacity-60">Sec</span>
          </div>
        </div>
        <button @click="openSubmitModal" class="bg-primary hover:bg-primary/90 text-white px-4 py-2 rounded-xl font-bold text-xs transition-colors shadow-lg shadow-primary/20" type="button">
          Submit Exam
        </button>
      </template>
    </StudentTopHeader>

    <main class="relative flex-1 flex max-w-[1440px] mx-auto w-full p-6 gap-6 overflow-hidden">
      <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
      <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

      <div class="relative flex-1 flex flex-col gap-6">
        <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-8 shadow-sm flex-1 animate-fade-up">
          <div class="flex justify-between items-center mb-8">
            <span class="bg-primary/10 text-primary px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider">Question 12 of 30</span>
            <button class="flex items-center gap-2 text-slate-500 hover:text-primary transition-colors" type="button">
              <span class="material-symbols-outlined text-xl">flag</span>
              <span class="text-sm font-medium">Flag for Review</span>
            </button>
          </div>
          <div class="prose dark:prose-invert max-w-none">
            <h2 class="text-2xl font-semibold leading-relaxed mb-8">
              According to the dual-process theory, which of the following best describes the difference between System 1 and System 2 thinking?
            </h2>
          </div>

          <div class="space-y-4">
            <label class="group relative flex items-center p-5 rounded-xl border-2 border-slate-100 dark:border-slate-800 hover:border-primary/50 cursor-pointer transition-all bg-slate-50/50 dark:bg-slate-800/30">
              <input class="w-5 h-5 text-primary focus:ring-primary border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900" name="exam-option" type="radio" />
              <span class="ml-4 text-lg font-medium">System 1 is fast and intuitive; System 2 is slow and analytical.</span>
            </label>
            <label class="group relative flex items-center p-5 rounded-xl border-2 border-primary bg-primary/5 dark:bg-primary/10 cursor-pointer transition-all">
              <input checked class="w-5 h-5 text-primary focus:ring-primary border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900" name="exam-option" type="radio" />
              <span class="ml-4 text-lg font-medium text-slate-900 dark:text-slate-100">System 1 is responsible for creative tasks; System 2 handles mathematical calculations.</span>
              <span class="ml-auto material-symbols-outlined text-primary">check_circle</span>
            </label>
            <label class="group relative flex items-center p-5 rounded-xl border-2 border-slate-100 dark:border-slate-800 hover:border-primary/50 cursor-pointer transition-all bg-slate-50/50 dark:bg-slate-800/30">
              <input class="w-5 h-5 text-primary focus:ring-primary border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900" name="exam-option" type="radio" />
              <span class="ml-4 text-lg font-medium">System 1 handles long-term memory; System 2 handles working memory.</span>
            </label>
            <label class="group relative flex items-center p-5 rounded-xl border-2 border-slate-100 dark:border-slate-800 hover:border-primary/50 cursor-pointer transition-all bg-slate-50/50 dark:bg-slate-800/30">
              <input class="w-5 h-5 text-primary focus:ring-primary border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900" name="exam-option" type="radio" />
              <span class="ml-4 text-lg font-medium">Both systems operate simultaneously with equal metabolic cost.</span>
            </label>
          </div>
        </div>

        <div class="flex items-center justify-between py-2 animate-fade-up-delay">
          <button class="flex items-center gap-2 px-6 py-3 rounded-xl border border-slate-200 dark:border-slate-800 font-bold hover:bg-slate-100 dark:hover:bg-slate-800 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200" type="button">
            <span class="material-symbols-outlined">arrow_back</span>
            Previous Question
          </button>
          <div class="flex gap-4">
            <button class="flex items-center gap-2 px-6 py-3 rounded-xl bg-slate-900 dark:bg-slate-100 text-white dark:text-slate-900 font-bold hover:opacity-90 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200" type="button">
              Next Question
              <span class="material-symbols-outlined">arrow_forward</span>
            </button>
          </div>
        </div>
      </div>

      <aside class="relative w-80 flex flex-col gap-6 animate-fade-up-delay">
        <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-sm">
          <div class="flex justify-between items-end mb-3">
            <h3 class="font-bold text-sm">Exam Progress</h3>
            <span class="text-sm font-bold text-primary">40%</span>
          </div>
          <div class="w-full bg-slate-100 dark:bg-slate-800 h-2 rounded-full overflow-hidden">
            <div class="bg-primary h-full w-[40%] rounded-full"></div>
          </div>
          <div class="mt-4 grid grid-cols-2 gap-3">
            <div class="flex items-center gap-2 text-xs font-medium text-slate-500">
              <div class="w-2 h-2 rounded-full bg-primary"></div>
              Answered (12)
            </div>
            <div class="flex items-center gap-2 text-xs font-medium text-slate-500">
              <div class="w-2 h-2 rounded-full bg-slate-200 dark:bg-slate-700"></div>
              Unanswered (18)
            </div>
            <div class="flex items-center gap-2 text-xs font-medium text-slate-500">
              <div class="w-2 h-2 rounded-full bg-amber-400"></div>
              Flagged (3)
            </div>
          </div>
        </div>

        <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-sm flex-1 overflow-y-auto">
          <div class="flex items-center justify-between mb-4">
            <h3 class="font-bold text-sm">Question Navigator</h3>
            <span class="text-xs text-slate-500 font-medium">30 Total</span>
          </div>
          <div class="grid grid-cols-5 gap-2">
            <button v-for="item in questions" :key="item.number" :class="item.className" class="aspect-square flex items-center justify-center rounded-lg text-xs font-bold relative" type="button">
              {{ item.number }}
              <span v-if="item.flagged" class="absolute top-0 right-0 w-1.5 h-1.5 bg-white rounded-full translate-x-1/3 -translate-y-1/3"></span>
            </button>
          </div>
        </div>

        <div class="bg-primary/5 dark:bg-primary/10 border border-primary/20 rounded-2xl p-4">
          <div class="flex items-start gap-3">
            <span class="material-symbols-outlined text-primary mt-0.5">verified_user</span>
            <div>
              <p class="text-xs font-bold text-slate-900 dark:text-slate-100">Exam Integrity</p>
              <p class="text-[11px] text-slate-500 leading-relaxed mt-1">
                Window focus is being monitored. Your session is protected by proctoring software.
              </p>
            </div>
          </div>
        </div>
      </aside>
    </main>

    <footer class="p-4 border-t border-slate-200 dark:border-slate-800 text-center">
      <p class="text-xs text-slate-400 font-medium">
        Need help? Contact exam support at <span class="text-primary cursor-pointer hover:underline">support@edu-portal.com</span>
      </p>
    </footer>

    <div v-if="showSubmitModal" class="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 px-4">
      <div class="w-full max-w-md rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 shadow-xl p-6">
        <div class="flex items-start gap-3 mb-4">
          <span class="material-symbols-outlined text-primary text-3xl">help</span>
          <div>
            <h3 class="text-lg font-bold">Xác nhận nộp bài?</h3>
            <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">Bạn sẽ không thể thay đổi đáp án sau khi nộp.</p>
          </div>
        </div>

        <div v-if="unansweredCount > 0" class="mb-5 rounded-xl bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700 p-3">
          <p class="text-sm font-semibold text-amber-700 dark:text-amber-300">
            Bạn còn {{ unansweredCount }} câu chưa làm.
          </p>
        </div>

        <div class="flex justify-end gap-3">
          <button @click="closeSubmitModal" class="px-4 py-2 rounded-lg border border-slate-200 dark:border-slate-700 font-semibold hover:bg-slate-50 dark:hover:bg-slate-800" type="button">
            Quay lại làm bài
          </button>
          <button @click="submitExam" class="px-4 py-2 rounded-lg bg-primary text-white font-semibold hover:bg-primary/90" type="button">
            Xác nhận nộp
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import StudentTopHeader from './StudentTopHeader.vue'

const route = useRoute()
const router = useRouter()
const isDark = ref(false)

const examTitle = computed(() => route.query.exam || 'Advanced Psychology Midterm')
const showSubmitModal = ref(false)

const unansweredCount = computed(() =>
  questions.filter((item) => item.className.includes('bg-slate-100')).length
)

const openSubmitModal = () => {
  showSubmitModal.value = true
}

const closeSubmitModal = () => {
  showSubmitModal.value = false
}

const submitExam = () => {
  showSubmitModal.value = false
  router.push({
    path: '/student/submission-confirmation',
    query: { exam: examTitle.value }
  })
}

const questions = [
  { number: 1, className: 'bg-primary text-white', flagged: false },
  { number: 2, className: 'bg-primary text-white', flagged: false },
  { number: 3, className: 'bg-primary text-white', flagged: false },
  { number: 4, className: 'bg-primary text-white', flagged: false },
  { number: 5, className: 'bg-amber-400 text-white', flagged: true },
  { number: 6, className: 'bg-primary text-white', flagged: false },
  { number: 7, className: 'bg-primary text-white', flagged: false },
  { number: 8, className: 'bg-primary text-white', flagged: false },
  { number: 9, className: 'bg-primary text-white', flagged: false },
  { number: 10, className: 'bg-primary text-white', flagged: false },
  { number: 11, className: 'bg-primary text-white', flagged: false },
  { number: 12, className: 'border-2 border-primary text-primary shadow-inner', flagged: false },
  { number: 13, className: 'bg-slate-100 dark:bg-slate-800 text-slate-400', flagged: false },
  { number: 14, className: 'bg-slate-100 dark:bg-slate-800 text-slate-400', flagged: false },
  { number: 15, className: 'bg-amber-400 text-white', flagged: true },
  { number: 16, className: 'bg-slate-100 dark:bg-slate-800 text-slate-400', flagged: false },
  { number: 17, className: 'bg-slate-100 dark:bg-slate-800 text-slate-400', flagged: false },
  { number: 18, className: 'bg-slate-100 dark:bg-slate-800 text-slate-400', flagged: false },
  { number: 19, className: 'bg-slate-100 dark:bg-slate-800 text-slate-400', flagged: false },
  { number: 20, className: 'bg-slate-100 dark:bg-slate-800 text-slate-400', flagged: false },
  { number: 21, className: 'bg-slate-100 dark:bg-slate-800 text-slate-400', flagged: false },
  { number: 22, className: 'bg-slate-100 dark:bg-slate-800 text-slate-400', flagged: false },
  { number: 23, className: 'bg-slate-100 dark:bg-slate-800 text-slate-400', flagged: false },
  { number: 24, className: 'bg-slate-100 dark:bg-slate-800 text-slate-400', flagged: false },
  { number: 25, className: 'bg-amber-400 text-white', flagged: true },
  { number: 26, className: 'bg-slate-100 dark:bg-slate-800 text-slate-400', flagged: false },
  { number: 27, className: 'bg-slate-100 dark:bg-slate-800 text-slate-400', flagged: false },
  { number: 28, className: 'bg-slate-100 dark:bg-slate-800 text-slate-400', flagged: false },
  { number: 29, className: 'bg-slate-100 dark:bg-slate-800 text-slate-400', flagged: false },
  { number: 30, className: 'bg-slate-100 dark:bg-slate-800 text-slate-400', flagged: false }
]
</script>

<style scoped>
.font-display {
  font-family: 'Public Sans', sans-serif;
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
