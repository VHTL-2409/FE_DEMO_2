<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <div class="relative flex h-auto min-h-screen w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full grow flex-col">
        <StudentTopHeader />

        <main class="relative flex flex-1 justify-center py-8 px-4 overflow-hidden">
          <div class="pointer-events-none absolute -top-20 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
          <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

          <div class="layout-content-container relative flex flex-col max-w-[960px] flex-1 gap-8">
            <div class="flex flex-col gap-3 animate-fade-up">
              <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/10 text-primary w-fit text-sm font-semibold">
                <span class="relative flex h-2 w-2">
                  <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75"></span>
                  <span class="relative inline-flex rounded-full h-2 w-2 bg-primary"></span>
                </span>
                Waiting Room Active
              </div>
              <h1 class="text-slate-900 dark:text-white text-4xl font-black leading-tight tracking-[-0.033em]">Waiting for Exam to Start</h1>
              <p class="text-slate-600 dark:text-slate-400 text-lg font-normal leading-normal">The proctor will start the session shortly. Please remain on this screen and ensure your hardware is ready.</p>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6 animate-fade-up-delay">
              <div class="bg-white dark:bg-primary/5 rounded-xl border border-primary/10 dark:border-primary/20 p-6 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                <h2 class="text-slate-900 dark:text-white text-xl font-bold leading-tight mb-4 flex items-center gap-2">
                  <span class="material-symbols-outlined text-primary">info</span>
                  Exam Details
                </h2>
                <div class="space-y-4">
                  <div class="flex justify-between border-b border-primary/5 pb-2">
                    <p class="text-slate-500 dark:text-slate-400 text-sm">Exam Title</p>
                    <p class="text-slate-900 dark:text-white text-sm font-semibold">{{ examTitle }}</p>
                  </div>
                  <div class="flex justify-between border-b border-primary/5 pb-2">
                    <p class="text-slate-500 dark:text-slate-400 text-sm">Duration</p>
                    <p class="text-slate-900 dark:text-white text-sm font-semibold">60 mins</p>
                  </div>
                  <div class="flex justify-between">
                    <p class="text-slate-500 dark:text-slate-400 text-sm">Total Questions</p>
                    <p class="text-slate-900 dark:text-white text-sm font-semibold">30 Questions</p>
                  </div>
                </div>
              </div>

              <div class="bg-white dark:bg-primary/5 rounded-xl border border-primary/10 dark:border-primary/20 p-6 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                <h2 class="text-slate-900 dark:text-white text-xl font-bold leading-tight mb-4 flex items-center gap-2">
                  <span class="material-symbols-outlined text-primary">check_circle</span>
                  System Check
                </h2>
                <div class="space-y-4">
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-3">
                      <span class="material-symbols-outlined text-green-600 dark:text-green-400">videocam</span>
                      <span class="text-sm font-medium">Camera</span>
                    </div>
                    <span class="text-xs font-bold text-green-600 dark:text-green-400 uppercase">Ready</span>
                  </div>
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-3">
                      <span class="material-symbols-outlined text-green-600 dark:text-green-400">mic</span>
                      <span class="text-sm font-medium">Microphone</span>
                    </div>
                    <span class="text-xs font-bold text-green-600 dark:text-green-400 uppercase">Ready</span>
                  </div>
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-3">
                      <span class="material-symbols-outlined text-green-600 dark:text-green-400">wifi</span>
                      <span class="text-sm font-medium">Internet Connection</span>
                    </div>
                    <span class="text-xs font-bold text-green-600 dark:text-green-400 uppercase">Stable</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="bg-white dark:bg-primary/5 rounded-xl border border-primary/10 dark:border-primary/20 p-6 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200 animate-fade-up-delay">
              <h2 class="text-slate-900 dark:text-white text-xl font-bold leading-tight mb-4 flex items-center gap-2">
                <span class="material-symbols-outlined text-primary">gavel</span>
                Exam Rules
              </h2>
              <ul class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <li class="flex items-start gap-3">
                  <span class="material-symbols-outlined text-primary text-lg">error</span>
                  <p class="text-sm text-slate-600 dark:text-slate-300">Switching tabs or windows will result in automatic disqualification.</p>
                </li>
                <li class="flex items-start gap-3">
                  <span class="material-symbols-outlined text-primary text-lg">error</span>
                  <p class="text-sm text-slate-600 dark:text-slate-300">Your camera and microphone must remain on for the entire duration.</p>
                </li>
                <li class="flex items-start gap-3">
                  <span class="material-symbols-outlined text-primary text-lg">error</span>
                  <p class="text-sm text-slate-600 dark:text-slate-300">No external materials, books, or notes are permitted on your desk.</p>
                </li>
                <li class="flex items-start gap-3">
                  <span class="material-symbols-outlined text-primary text-lg">error</span>
                  <p class="text-sm text-slate-600 dark:text-slate-300">Ensure you are in a quiet, well-lit environment alone.</p>
                </li>
              </ul>
            </div>

            <div class="flex flex-col items-center gap-6 py-8 border-t border-primary/10 dark:border-primary/20 animate-fade-up-delay">
              <div class="text-center">
                <p class="text-slate-500 dark:text-slate-400 text-sm mb-2 uppercase tracking-widest font-bold">Time until start (Est.)</p>
                <div class="text-5xl font-black text-primary flex gap-4">
                  <div class="flex flex-col items-center">
                    <span>04</span>
                    <span class="text-[10px] uppercase text-slate-400">min</span>
                  </div>
                  <span>:</span>
                  <div class="flex flex-col items-center">
                    <span>52</span>
                    <span class="text-[10px] uppercase text-slate-400">sec</span>
                  </div>
                </div>
              </div>
              <div class="w-full max-w-md space-y-4">
                <button @click="goToExamInterface" class="w-full py-4 px-6 rounded-xl bg-primary text-white font-bold text-lg flex items-center justify-center gap-2 hover:bg-primary/90 transition-colors" type="button">
                  <span class="material-symbols-outlined">play_arrow</span>
                  Start Exam
                </button>
                <p class="text-center text-slate-500 dark:text-slate-400 text-sm italic">
                  The proctor will start the exam shortly. The button will be enabled once the session begins.
                </p>
              </div>
            </div>
          </div>
        </main>

        <footer class="p-6 text-center text-slate-400 text-xs">
          © 2024 ExamPortal Security Suite. All rights reserved.
          <br>
          Need help?
          <button type="button" class="text-primary hover:underline">Contact Support</button>
        </footer>
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

const goToExamInterface = () => {
  router.push({
    path: '/student/exam-interface',
    query: { exam: examTitle.value }
  })
}
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
