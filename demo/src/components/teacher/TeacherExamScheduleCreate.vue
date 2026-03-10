<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <TeacherTopHeader active-section="exam" />

    <main class="max-w-4xl mx-auto px-4 py-10">
      <div class="mb-8">
        <h1 class="text-3xl font-bold tracking-tight">Exam Settings &amp; Create</h1>
        <p class="text-slate-500 mt-1">{{ selectedExamTitle }}</p>
      </div>

      <section class="bg-white dark:bg-slate-900 p-8 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
        <div class="flex items-center gap-2 mb-6">
          <span class="material-symbols-outlined text-primary">settings</span>
          <h3 class="text-lg font-bold">Time Configuration</h3>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div class="space-y-4">
            <div class="flex items-center justify-between">
              <label class="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-2">
                <span class="material-symbols-outlined text-sm">timer</span>
                Time Limit (Minutes)
              </label>
              <span class="text-primary font-bold">{{ timeLimit }} min</span>
            </div>
            <input v-model="timeLimit" class="w-full h-2 bg-slate-200 dark:bg-slate-700 rounded-lg appearance-none cursor-pointer accent-primary" type="range" min="5" max="240" step="5" />
          </div>
          <div class="space-y-2">
            <label class="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-2">
              <span class="material-symbols-outlined text-sm">calendar_today</span>
              Start Date &amp; Time
            </label>
            <input v-model="startAt" class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all" type="datetime-local" />
          </div>
          <div class="space-y-2">
            <label class="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-2">
              <span class="material-symbols-outlined text-sm">event_busy</span>
              End Date &amp; Time
            </label>
            <input v-model="endAt" class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all" type="datetime-local" />
          </div>
        </div>

        <div class="flex items-center justify-end gap-4 pt-8">
          <button class="px-8 py-3 rounded-lg border border-slate-200 dark:border-slate-800 font-semibold hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors" type="button" @click="goBack">
            Back
          </button>
          <button class="px-10 py-3 rounded-lg bg-primary text-white font-bold shadow-lg shadow-primary/30 hover:bg-primary/90 transition-all flex items-center gap-2" type="button">
            Create Exam
            <span class="material-symbols-outlined text-lg">rocket_launch</span>
          </button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const router = useRouter()
const route = useRoute()
const isDark = ref(false)
const timeLimit = ref(60)
const startAt = ref('')
const endAt = ref('')

const selectedExamTitle = computed(() => route.query.title || 'New Exam')

const goBack = () => {
  const source = route.query.source
  if (source === 'manual') {
    router.push('/teacher/exams/manual')
    return
  }
  router.push('/teacher/exams/create')
}
</script>

<style scoped>
.font-display {
  font-family: 'Inter', sans-serif;
}
</style>