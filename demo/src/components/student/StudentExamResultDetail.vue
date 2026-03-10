<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 antialiased">
    <div class="relative flex h-auto min-h-screen w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full grow flex-col">
        <StudentTopHeader />

        <main class="flex flex-1 justify-center py-8">
          <div class="layout-content-container flex flex-col max-w-[1000px] flex-1 px-4 md:px-10">
            <div class="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-8">
              <div class="flex flex-col gap-2">
                <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400 text-xs font-bold w-fit">
                  <span class="size-2 rounded-full bg-green-500"></span>
                  EXAM COMPLETED
                </div>
                <h1 class="text-slate-900 dark:text-slate-100 text-3xl md:text-4xl font-black leading-tight tracking-tight">{{ examTitle }}</h1>
                <p class="text-slate-500 dark:text-slate-400 text-base font-normal">{{ attemptedAt }}</p>
              </div>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-10">
              <div class="flex flex-col gap-2 rounded-xl p-6 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm">
                <p class="text-slate-500 dark:text-slate-400 text-sm font-medium uppercase tracking-wider">Overall Score</p>
                <div class="flex items-baseline gap-2">
                  <p class="text-slate-900 dark:text-slate-100 text-3xl font-bold">{{ scorePercent }}/100</p>
                  <span class="text-green-600 dark:text-green-400 text-sm font-bold">({{ scorePercent }}%)</span>
                </div>
                <div class="w-full bg-slate-100 dark:bg-slate-800 h-1.5 rounded-full mt-2 overflow-hidden">
                  <div class="bg-primary h-full rounded-full" :style="{ width: `${scorePercent}%` }"></div>
                </div>
              </div>

              <div class="flex flex-col gap-2 rounded-xl p-6 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm">
                <p class="text-slate-500 dark:text-slate-400 text-sm font-medium uppercase tracking-wider">Time Taken</p>
                <p class="text-slate-900 dark:text-slate-100 text-3xl font-bold">{{ timeTaken }}</p>
                <p class="text-slate-500 dark:text-slate-400 text-sm">Limit: {{ timeLimit }}</p>
              </div>

              <div class="flex flex-col gap-2 rounded-xl p-6 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm">
                <p class="text-slate-500 dark:text-slate-400 text-sm font-medium uppercase tracking-wider">Accuracy</p>
                <p class="text-slate-900 dark:text-slate-100 text-3xl font-bold">{{ accuracy }}%</p>
                <p class="text-green-600 dark:text-green-400 text-sm font-bold">+5% vs Avg</p>
              </div>

              <div class="flex flex-col gap-2 rounded-xl p-6 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm">
                <p class="text-slate-500 dark:text-slate-400 text-sm font-medium uppercase tracking-wider">Percentile</p>
                <p class="text-slate-900 dark:text-slate-100 text-3xl font-bold">{{ percentile }}</p>
                <p class="text-slate-500 dark:text-slate-400 text-sm">Top 10 Students</p>
              </div>
            </div>

            <div class="mb-10">
              <h2 class="text-slate-900 dark:text-slate-100 text-xl font-bold mb-6">Performance Breakdown</h2>
              <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <div class="lg:col-span-2 bg-white dark:bg-slate-900 p-8 rounded-xl border border-slate-200 dark:border-slate-800">
                  <p class="text-slate-900 dark:text-slate-100 text-base font-semibold mb-6">Response Distribution</p>
                  <div class="grid grid-cols-3 gap-8 items-end h-48 px-4">
                    <div class="flex flex-col items-center gap-3 h-full justify-end">
                      <div class="bg-green-500/20 border-t-4 border-green-500 w-full rounded-t-lg" style="height: 85%;"></div>
                      <p class="text-slate-600 dark:text-slate-400 text-xs font-bold uppercase">Correct</p>
                      <span class="text-green-600 font-bold">34</span>
                    </div>
                    <div class="flex flex-col items-center gap-3 h-full justify-end">
                      <div class="bg-red-500/20 border-t-4 border-red-500 w-full rounded-t-lg" style="height: 10%;"></div>
                      <p class="text-slate-600 dark:text-slate-400 text-xs font-bold uppercase">Incorrect</p>
                      <span class="text-red-600 font-bold">4</span>
                    </div>
                    <div class="flex flex-col items-center gap-3 h-full justify-end">
                      <div class="bg-slate-300 dark:bg-slate-700 border-t-4 border-slate-400 w-full rounded-t-lg" style="height: 5%;"></div>
                      <p class="text-slate-600 dark:text-slate-400 text-xs font-bold uppercase">Skipped</p>
                      <span class="text-slate-500 font-bold">2</span>
                    </div>
                  </div>
                </div>

                <div class="bg-white dark:bg-slate-900 p-8 rounded-xl border border-slate-200 dark:border-slate-800 flex flex-col justify-center items-center">
                  <div class="relative size-32 mb-4 flex items-center justify-center">
                    <svg class="size-full -rotate-90" viewBox="0 0 36 36">
                      <path class="stroke-slate-100 dark:stroke-slate-800 fill-none stroke-[3]" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"></path>
                      <path class="stroke-primary fill-none stroke-[3]" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" :stroke-dasharray="`${scorePercent}, 100`"></path>
                    </svg>
                    <div class="absolute inset-0 flex flex-col items-center justify-center">
                      <span class="text-2xl font-black text-slate-900 dark:text-slate-100">{{ scorePercent }}%</span>
                    </div>
                  </div>
                  <h3 class="text-slate-900 dark:text-slate-100 font-bold text-center">Mastery Level</h3>
                  <p class="text-slate-500 dark:text-slate-400 text-sm text-center">Calculus &amp; Algebra</p>
                </div>
              </div>
            </div>

            <div class="mb-10">
              <div class="flex items-center justify-between mb-6">
                <h2 class="text-slate-900 dark:text-slate-100 text-xl font-bold">Question Review</h2>
                <div class="flex gap-2">
                  <span class="flex items-center gap-1 text-xs font-semibold px-2 py-1 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400 rounded">
                    <span class="material-symbols-outlined text-sm">check_circle</span> Correct
                  </span>
                  <span class="flex items-center gap-1 text-xs font-semibold px-2 py-1 bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400 rounded">
                    <span class="material-symbols-outlined text-sm">cancel</span> Incorrect
                  </span>
                </div>
              </div>

              <div class="flex flex-col gap-4">
                <div class="bg-white dark:bg-slate-900 rounded-xl border-l-4 border-green-500 shadow-sm border-y border-r border-slate-200 dark:border-slate-800 overflow-hidden">
                  <div class="p-6">
                    <div class="flex justify-between items-start mb-4">
                      <span class="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-widest">Question 1 — Multiple Choice</span>
                      <span class="text-green-600 dark:text-green-400 font-bold text-sm">+2.5 Points</span>
                    </div>
                    <p class="text-slate-900 dark:text-slate-100 text-lg font-medium mb-6">What is the derivative of f(x) = 3x² + 2x + 1 with respect to x?</p>

                    <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                      <div class="flex items-center justify-between p-4 rounded-lg bg-green-50 dark:bg-green-900/10 border border-green-200 dark:border-green-800/50">
                        <div class="flex items-center gap-3">
                          <span class="w-6 h-6 flex items-center justify-center rounded-full bg-green-500 text-white text-xs font-bold">A</span>
                          <span class="text-slate-700 dark:text-slate-300">6x + 2</span>
                        </div>
                        <span class="material-symbols-outlined text-green-600">check_circle</span>
                      </div>

                      <div class="flex items-center justify-between p-4 rounded-lg bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800">
                        <div class="flex items-center gap-3">
                          <span class="w-6 h-6 flex items-center justify-center rounded-full bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-400 text-xs font-bold">B</span>
                          <span class="text-slate-500">3x + 2</span>
                        </div>
                      </div>

                      <div class="flex items-center justify-between p-4 rounded-lg bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800">
                        <div class="flex items-center gap-3">
                          <span class="w-6 h-6 flex items-center justify-center rounded-full bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-400 text-xs font-bold">C</span>
                          <span class="text-slate-500">6x</span>
                        </div>
                      </div>

                      <div class="flex items-center justify-between p-4 rounded-lg bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800">
                        <div class="flex items-center gap-3">
                          <span class="w-6 h-6 flex items-center justify-center rounded-full bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-400 text-xs font-bold">D</span>
                          <span class="text-slate-500">x² + x</span>
                        </div>
                      </div>
                    </div>

                    <div class="mt-6 pt-6 border-t border-slate-100 dark:border-slate-800">
                      <p class="text-sm text-slate-500 dark:text-slate-400 leading-relaxed italic">
                        <strong>Explanation:</strong> The power rule for derivatives states that d/dx[x^n] = nx^(n-1). Applying this: d/dx[3x²] = 6x, d/dx[2x] = 2, and d/dx[1] = 0. Total = 6x + 2.
                      </p>
                    </div>
                  </div>
                </div>

                <div class="bg-white dark:bg-slate-900 rounded-xl border-l-4 border-red-500 shadow-sm border-y border-r border-slate-200 dark:border-slate-800 overflow-hidden">
                  <div class="p-6">
                    <div class="flex justify-between items-start mb-4">
                      <span class="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-widest">Question 2 — True/False</span>
                      <span class="text-red-600 dark:text-red-400 font-bold text-sm">0/2.5 Points</span>
                    </div>
                    <p class="text-slate-900 dark:text-slate-100 text-lg font-medium mb-6">The integral of sin(x) is cos(x).</p>

                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div class="flex items-center justify-between p-4 rounded-lg bg-red-50 dark:bg-red-900/10 border border-red-200 dark:border-red-800/50 relative">
                        <div class="flex items-center gap-3">
                          <span class="w-6 h-6 flex items-center justify-center rounded-full bg-red-500 text-white text-xs font-bold">A</span>
                          <span class="text-slate-700 dark:text-slate-300 font-semibold">True</span>
                        </div>
                        <div class="flex items-center gap-1.5">
                          <span class="text-[10px] font-bold text-red-600 dark:text-red-400 uppercase">Your Answer</span>
                          <span class="material-symbols-outlined text-red-600">cancel</span>
                        </div>
                      </div>

                      <div class="flex items-center justify-between p-4 rounded-lg bg-green-50 dark:bg-green-900/10 border border-green-200 dark:border-green-800/50">
                        <div class="flex items-center gap-3">
                          <span class="w-6 h-6 flex items-center justify-center rounded-full bg-green-500 text-white text-xs font-bold">B</span>
                          <span class="text-slate-700 dark:text-slate-300 font-semibold">False</span>
                        </div>
                        <div class="flex items-center gap-1.5">
                          <span class="text-[10px] font-bold text-green-600 dark:text-green-400 uppercase">Correct Answer</span>
                          <span class="material-symbols-outlined text-green-600">check_circle</span>
                        </div>
                      </div>
                    </div>

                    <div class="mt-6 pt-6 border-t border-slate-100 dark:border-slate-800">
                      <p class="text-sm text-slate-500 dark:text-slate-400 leading-relaxed italic">
                        <strong>Explanation:</strong> The integral of sin(x) is -cos(x) + C. The derivative of sin(x) is cos(x), which is a common point of confusion.
                      </p>
                    </div>
                  </div>
                </div>

                <div class="flex items-center justify-center gap-4 py-8">
                  <button class="size-10 rounded-lg border border-slate-200 dark:border-slate-800 flex items-center justify-center text-slate-400 cursor-not-allowed" disabled type="button">
                    <span class="material-symbols-outlined">chevron_left</span>
                  </button>
                  <span class="text-slate-600 dark:text-slate-400 font-medium">Page 1 of 5</span>
                  <button class="size-10 rounded-lg border border-slate-200 dark:border-slate-800 flex items-center justify-center text-primary hover:bg-primary/5" type="button">
                    <span class="material-symbols-outlined">chevron_right</span>
                  </button>
                </div>
              </div>
            </div>

            <div class="bg-primary/5 rounded-xl p-8 flex flex-col items-center justify-center gap-4 border border-primary/10 mb-8">
              <p class="text-slate-600 dark:text-slate-400 text-center font-medium">Need to discuss these results with your instructor?</p>
              <button class="bg-primary text-white px-8 py-3 rounded-lg font-bold hover:bg-opacity-90 transition-all flex items-center gap-2" type="button">
                <span class="material-symbols-outlined text-xl">mail</span>
                Contact Instructor
              </button>
            </div>
          </div>
        </main>

        <footer class="border-t border-slate-200 dark:border-slate-800 py-10 px-10 text-center">
          <p class="text-slate-400 dark:text-slate-600 text-sm">© 2023 Student Learning Portal. All rights reserved.</p>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import StudentTopHeader from './StudentTopHeader.vue'

const route = useRoute()
const isDark = ref(false)

const parseNumber = (value, fallback) => {
  const parsed = Number.parseInt(String(value ?? ''), 10)
  return Number.isNaN(parsed) ? fallback : parsed
}

const examTitle = computed(() => route.query.exam || 'Advanced Mathematics: Final Exam')
const attemptedAt = computed(() => route.query.attempted || 'Attempted on October 24, 2023 • 10:00 AM - 11:30 AM')
const scorePercent = computed(() => parseNumber(route.query.score, 85))
const timeTaken = computed(() => route.query.time || '45:12')
const timeLimit = computed(() => route.query.limit || '60:00')
const accuracy = computed(() => parseNumber(route.query.accuracy, 92))
const percentile = computed(() => route.query.percentile || '94th')
</script>

<style scoped>
.font-display {
  font-family: 'Inter', sans-serif;
}
</style>
