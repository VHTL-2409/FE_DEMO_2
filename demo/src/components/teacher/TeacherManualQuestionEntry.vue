<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <div class="layout-container flex h-full grow flex-col">
      <TeacherTopHeader active-section="exam" />

      <main class="relative flex-1 max-w-7xl mx-auto w-full p-4 md:p-8 overflow-hidden">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

        <div class="relative mb-6 animate-fade-up">
          <h1 class="text-3xl font-bold tracking-tight">Manual Question Entry</h1>
          <p class="text-slate-500 mt-1">Configure individual questions for your exam.</p>
        </div>

        <section class="bg-white dark:bg-slate-900 rounded-xl border border-primary/10 shadow-sm overflow-hidden animate-fade-up-delay hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
          <div class="p-6 border-b border-primary/10">
            <h2 class="text-lg font-bold">Question Editor</h2>
          </div>
          <div class="p-6 space-y-6">
            <div class="space-y-2">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Exam Title</label>
              <input v-model="examTitle" class="w-full bg-slate-50 dark:bg-slate-800 border-primary/10 rounded-lg text-sm focus:ring-primary focus:border-primary" placeholder="e.g. Mid-term Physics" type="text" />
            </div>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div class="space-y-2">
                <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Question Type</label>
                <select class="w-full bg-slate-50 dark:bg-slate-800 border-primary/10 rounded-lg text-sm focus:ring-primary focus:border-primary">
                  <option>Multiple Choice</option>
                  <option>True/False</option>
                  <option>Short Answer</option>
                </select>
              </div>
              <div class="space-y-2">
                <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Points / Marks</label>
                <input class="w-full bg-slate-50 dark:bg-slate-800 border-primary/10 rounded-lg text-sm focus:ring-primary focus:border-primary" type="number" value="1.0" />
              </div>
              <div class="space-y-2">
                <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Difficulty</label>
                <div class="flex gap-2">
                  <button class="flex-1 py-2 text-xs font-semibold rounded-lg border border-green-200 bg-green-50 text-green-700" type="button">Easy</button>
                  <button class="flex-1 py-2 text-xs font-semibold rounded-lg border border-primary/10 bg-slate-50 text-slate-600" type="button">Medium</button>
                  <button class="flex-1 py-2 text-xs font-semibold rounded-lg border border-primary/10 bg-slate-50 text-slate-600" type="button">Hard</button>
                </div>
              </div>
            </div>

            <div class="space-y-2">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Question Stem</label>
              <textarea class="w-full p-4 text-sm bg-white dark:bg-slate-900 border border-primary/10 rounded-lg resize-none" placeholder="Enter your question here..." rows="4"></textarea>
            </div>
          </div>
          <div class="px-6 py-4 bg-slate-50 dark:bg-slate-800/50 border-t border-primary/10 flex justify-end gap-3">
            <button class="px-4 py-2 rounded-lg text-primary font-bold text-sm hover:bg-primary/5 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200" type="button">Save Question</button>
            <button class="px-6 py-2 rounded-lg bg-primary text-white font-bold text-sm shadow-md shadow-primary/20 hover:bg-primary/90 hover:-translate-y-0.5 transition-all duration-200" type="button" @click="goNext">
              Next
            </button>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const router = useRouter()
const isDark = ref(false)
const examTitle = ref('')

const goNext = () => {
  router.push({ path: '/teacher/exams/schedule', query: { title: examTitle.value || 'Manual Exam', source: 'manual' } })
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