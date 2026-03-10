<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <div class="relative flex h-auto min-h-screen w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full grow flex-col">
        <StudentTopHeader />

        <main class="relative flex-1 px-6 md:px-20 lg:px-32 py-8 overflow-hidden">
          <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
          <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

          <div class="relative max-w-6xl mx-auto space-y-8">
            <header class="flex flex-col md:flex-row md:items-end justify-between gap-4 animate-fade-up">
              <div>
                <h1 class="text-3xl font-black tracking-tight">Student Profile</h1>
                <p class="text-slate-500 dark:text-slate-400 mt-1">Manage your learning profile and track academic activity.</p>
              </div>
              <button @click="goToDashboard" class="px-5 py-2.5 rounded-xl border border-primary/20 bg-primary/10 text-primary font-bold hover:bg-primary/20 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200" type="button">
                Back to Dashboard
              </button>
            </header>

            <section class="grid grid-cols-1 xl:grid-cols-3 gap-6 animate-fade-up-delay">
              <div class="xl:col-span-1 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                <div class="flex flex-col items-center text-center">
                  <div class="size-24 rounded-full border-4 border-primary/20 bg-center bg-cover" style="background-image: url('https://lh3.googleusercontent.com/aida-public/AB6AXuB1e4bA4nxwNhHI-Fn4dFl5ffPsV2Qcq-aTU38W1KpDGwJVtSs8Uu50HUjL-6AQ1rsj8FzgZ85caSzJLBV2kIzkBDQx4LboDGfHJTBM4ekHERyEJBMrHARjYfGyK-OOed1VR2AVLI8Set2ttmV6DKD-1ADupmLpYFhEoCRyviMIao-qfOPN6LDLGiDaSyvu15GGz3wp3epYInY9djSMLy1DHqOjBrCWEn_nXjmRki5_ystPT2x5YTemNdGgEHmK39v616MkRN2Pcg')"></div>
                  <h2 class="mt-4 text-xl font-bold">Alex Johnson</h2>
                  <p class="text-sm text-slate-500 dark:text-slate-400">ID: 4421-STU</p>
                  <p class="text-sm text-slate-500 dark:text-slate-400">alex.johnson@student.edu</p>
                </div>
                <div class="mt-6 space-y-3 text-sm">
                  <div class="flex justify-between"><span class="text-slate-500">Program</span><span class="font-semibold">Psychology</span></div>
                  <div class="flex justify-between"><span class="text-slate-500">Semester</span><span class="font-semibold">Fall 2026</span></div>
                  <div class="flex justify-between"><span class="text-slate-500">Status</span><span class="font-semibold text-emerald-600 dark:text-emerald-400">Active</span></div>
                </div>
              </div>

              <div class="xl:col-span-2 space-y-6">
                <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-5 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                    <p class="text-xs uppercase tracking-wider text-slate-500">Exams Completed</p>
                    <p class="text-2xl font-bold mt-2">24</p>
                  </div>
                  <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-5 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                    <p class="text-xs uppercase tracking-wider text-slate-500">Average Score</p>
                    <p class="text-2xl font-bold mt-2">86%</p>
                  </div>
                  <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-5 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                    <p class="text-xs uppercase tracking-wider text-slate-500">Best Subject</p>
                    <p class="text-2xl font-bold mt-2">Cognitive</p>
                  </div>
                </div>

                <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
                  <div class="px-6 py-4 border-b border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/50">
                    <h3 class="font-bold">Recent Activity</h3>
                  </div>
                  <div class="divide-y divide-slate-200 dark:divide-slate-800">
                    <div v-for="item in recentActivities" :key="item.title" class="px-6 py-4 flex items-center justify-between hover:bg-primary/5 transition-colors">
                      <div>
                        <p class="font-semibold">{{ item.title }}</p>
                        <p class="text-sm text-slate-500 dark:text-slate-400">{{ item.meta }}</p>
                      </div>
                      <span class="text-sm font-bold text-primary">{{ item.score }}</span>
                    </div>
                  </div>
                </div>

                <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm">
                  <h3 class="font-bold mb-4">Account Preferences</h3>
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                    <div class="rounded-lg bg-slate-50 dark:bg-slate-800/50 p-4 border border-slate-200 dark:border-slate-700">
                      <p class="text-slate-500">Email Notifications</p>
                      <p class="font-semibold mt-1">Enabled</p>
                    </div>
                    <div class="rounded-lg bg-slate-50 dark:bg-slate-800/50 p-4 border border-slate-200 dark:border-slate-700">
                      <p class="text-slate-500">Theme Preference</p>
                      <p class="font-semibold mt-1">System Default</p>
                    </div>
                  </div>
                </div>
              </div>
            </section>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import StudentTopHeader from './StudentTopHeader.vue'

const isDark = ref(false)
const router = useRouter()

const recentActivities = [
  { title: 'Advanced Psychology Final', meta: 'Oct 24, 2026 • 45m', score: '85/100' },
  { title: 'Behavioral Science Quiz', meta: 'Oct 18, 2026 • 32m', score: '90/100' },
  { title: 'Research Methods Practice', meta: 'Oct 12, 2026 • 50m', score: '83/100' }
]

const goToDashboard = () => {
  router.push('/student/dashboard')
}
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
