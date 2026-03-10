<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 transition-colors duration-200 min-h-screen">
    <TeacherTopHeader active-section="exam" />

    <main class="relative max-w-4xl mx-auto px-4 py-10 overflow-hidden">
      <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
      <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

      <div class="relative mb-10 animate-fade-up">
        <h2 class="text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight">Create New Exam (Import)</h2>
        <p class="mt-2 text-slate-600 dark:text-slate-400 text-lg">Step 1: Add title and import file. Step 2: Configure schedule.</p>
      </div>

      <div class="relative space-y-8 animate-fade-up-delay">
        <section class="bg-white dark:bg-slate-900 p-8 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
          <div class="flex items-center gap-2 mb-6">
            <span class="material-symbols-outlined text-primary">info</span>
            <h3 class="text-lg font-bold">General Information</h3>
          </div>
          <div class="space-y-2">
            <label class="text-sm font-semibold text-slate-700 dark:text-slate-300">Exam Title</label>
            <input v-model="examTitle" class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all" placeholder="e.g., Q3 Advanced Calculus Midterm" type="text" />
          </div>
        </section>

        <section class="bg-white dark:bg-slate-900 p-8 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
          <div class="flex items-center justify-between mb-6">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">upload_file</span>
              <h3 class="text-lg font-bold">Upload Exam Content</h3>
            </div>
          </div>

          <label class="relative group border-2 border-dashed border-slate-300 dark:border-slate-700 rounded-xl p-12 flex flex-col items-center justify-center transition-all hover:border-primary hover:bg-primary/5 cursor-pointer">
            <input class="absolute inset-0 w-full h-full opacity-0 cursor-pointer" type="file" accept=".csv, .xlsx" @change="onFileChange" />
            <div class="bg-primary/10 p-4 rounded-full mb-4 group-hover:scale-110 transition-transform">
              <span class="material-symbols-outlined text-primary text-4xl">cloud_upload</span>
            </div>
            <h4 class="text-lg font-semibold mb-1">Click to upload or drag and drop</h4>
            <p class="text-slate-500 dark:text-slate-400 text-sm">Support for CSV and XLSX formats (Max 10MB)</p>
            <p v-if="fileName" class="text-primary text-sm font-semibold mt-3">{{ fileName }}</p>
          </label>
        </section>

        <div class="flex items-center justify-end gap-4 pt-6">
          <button class="px-8 py-3 rounded-lg border border-slate-200 dark:border-slate-800 font-semibold hover:bg-slate-100 dark:hover:bg-slate-800 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200" type="button" @click="goBack">Discard Draft</button>
          <button class="px-10 py-3 rounded-lg bg-primary text-white font-bold shadow-lg shadow-primary/30 hover:bg-primary/90 hover:-translate-y-0.5 transition-all duration-200 flex items-center gap-2" type="button" @click="goNext">Next<span class="material-symbols-outlined text-lg">arrow_forward</span></button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const router = useRouter()
const isDark = ref(false)
const examTitle = ref('')
const fileName = ref('')

const onFileChange = (event) => {
  fileName.value = event.target.files?.[0]?.name || ''
}

const goBack = () => {
  router.push('/teacher/exams')
}

const goNext = () => {
  router.push({ path: '/teacher/exams/schedule', query: { title: examTitle.value || 'Imported Exam', source: 'import' } })
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