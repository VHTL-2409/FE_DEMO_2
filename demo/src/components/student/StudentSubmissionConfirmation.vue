<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <div class="relative flex h-auto min-h-screen w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full grow flex-col">
        <StudentTopHeader />

        <main class="flex-1 flex flex-col items-center justify-center px-4 py-12 lg:px-40">
          <div class="max-w-[640px] w-full flex flex-col items-center">
            <div class="mb-8 relative">
              <div class="absolute inset-0 bg-primary/20 blur-3xl rounded-full scale-150"></div>
              <div class="relative h-24 w-24 bg-primary text-white rounded-full flex items-center justify-center shadow-lg shadow-primary/30">
                <span class="material-symbols-outlined text-6xl">check_circle</span>
              </div>
            </div>

            <h1 class="text-3xl lg:text-4xl font-bold text-center mb-3">Exam Submitted Successfully</h1>
            <p class="text-slate-600 dark:text-slate-400 text-lg text-center mb-10">Your responses have been recorded and sent to your instructor for evaluation.</p>

            <div class="w-full bg-white dark:bg-slate-800/50 rounded-xl border border-primary/10 p-6 mb-8 shadow-sm">
              <h3 class="text-sm font-bold uppercase tracking-wider text-primary mb-6">Submission Summary</h3>
              <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div class="flex items-start gap-4">
                  <div class="bg-primary/10 p-2 rounded-lg text-primary">
                    <span class="material-symbols-outlined">calendar_today</span>
                  </div>
                  <div>
                    <p class="text-xs text-slate-500 dark:text-slate-400">Date &amp; Time</p>
                    <p class="font-semibold">October 24, 2023 at 2:45 PM</p>
                  </div>
                </div>
                <div class="flex items-start gap-4">
                  <div class="bg-primary/10 p-2 rounded-lg text-primary">
                    <span class="material-symbols-outlined">timer</span>
                  </div>
                  <div>
                    <p class="text-xs text-slate-500 dark:text-slate-400">Time Taken</p>
                    <p class="font-semibold">01:14:22</p>
                  </div>
                </div>
                <div class="flex items-start gap-4">
                  <div class="bg-primary/10 p-2 rounded-lg text-primary">
                    <span class="material-symbols-outlined">inventory_2</span>
                  </div>
                  <div>
                    <p class="text-xs text-slate-500 dark:text-slate-400">Questions Answered</p>
                    <p class="font-semibold">48 / 50</p>
                  </div>
                </div>
                <div class="flex items-start gap-4">
                  <div class="bg-primary/10 p-2 rounded-lg text-primary">
                    <span class="material-symbols-outlined">verified_user</span>
                  </div>
                  <div>
                    <p class="text-xs text-slate-500 dark:text-slate-400">Submission ID</p>
                    <p class="font-semibold">#EX-99283-TR</p>
                  </div>
                </div>
              </div>
            </div>

            <div class="flex flex-col sm:flex-row gap-4 w-full justify-center mt-4">
              <button @click="goToDashboard" class="flex items-center justify-center gap-2 bg-primary text-white px-8 py-3.5 rounded-xl font-bold shadow-lg shadow-primary/20 hover:brightness-110 active:scale-95 transition-all" type="button">
                <span class="material-symbols-outlined">dashboard</span>
                Return to Dashboard
              </button>
              <button class="flex items-center justify-center gap-2 bg-primary/10 text-primary border border-primary/20 px-8 py-3.5 rounded-xl font-bold hover:bg-primary/20 transition-all" type="button">
                <span class="material-symbols-outlined">download</span>
                View Submission Receipt
              </button>
            </div>

            <p class="mt-12 text-slate-400 text-sm flex items-center gap-2 italic">
              <span class="material-symbols-outlined text-sm">info</span>
              A copy of this receipt has been sent to your registered email address.
            </p>
          </div>
        </main>

        <div class="fixed bottom-0 left-0 w-64 h-64 bg-primary/5 rounded-full -translate-x-1/2 translate-y-1/2 blur-3xl pointer-events-none"></div>
        <div class="fixed top-20 right-0 w-96 h-96 bg-primary/5 rounded-full translate-x-1/2 -translate-y-1/2 blur-3xl pointer-events-none"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import StudentTopHeader from './StudentTopHeader.vue'

const router = useRouter()
const isDark = ref(false)

const goToDashboard = () => {
  router.push('/student/dashboard')
}
</script>

<style scoped>
.font-display {
  font-family: 'Public Sans', sans-serif;
}
</style>
