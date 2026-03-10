<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen"
  >
    <div class="layout-container flex h-full grow flex-col">
      <TeacherTopHeader active-section="exam" />

      <main class="relative max-w-[1200px] mx-auto w-full px-6 py-10 overflow-hidden">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/10 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-20 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>
        <div class="flex flex-col gap-2 mb-10 animate-fade-up">
          <h1 class="text-slate-900 dark:text-slate-100 text-4xl font-black leading-tight tracking-tight">Exam Creation &amp; Management</h1>
          <p class="text-slate-500 dark:text-slate-400 text-lg">Organize your existing assessments or build new ones with our streamlined import tools.</p>
        </div>

        <section class="mb-12 animate-fade-up-delay">
          <h2 class="text-slate-900 dark:text-slate-100 text-xl font-bold mb-6 flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">add_circle</span>
            Create New Exam
          </h2>
          <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div class="lg:col-span-2 group bg-white dark:bg-slate-900 rounded-xl shadow-sm border border-slate-200 dark:border-slate-800 overflow-hidden hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
              <div class="flex flex-col md:flex-row h-full">
                <div class="md:w-1/3 bg-primary/5 dark:bg-primary/20 flex items-center justify-center p-8 border-r border-slate-100 dark:border-slate-800">
                  <div class="text-center">
                    <span class="material-symbols-outlined text-6xl text-primary mb-2">upload_file</span>
                    <p class="text-xs font-bold uppercase tracking-wider text-primary">Bulk Import</p>
                  </div>
                </div>
                <div class="p-8 flex flex-col justify-between flex-1">
                  <div>
                    <h3 class="text-slate-900 dark:text-slate-100 text-xl font-bold mb-2">Import Exam Data</h3>
                    <p class="text-slate-500 dark:text-slate-400 mb-6">
                      Quickly populate your exam with hundreds of questions using our standard format. Supports <b>CSV</b> and <b>XLSX</b> files.
                    </p>
                    <div class="flex flex-wrap gap-4 items-center">
                      <button @click="goToCreateExam" class="bg-primary text-white hover:bg-primary/90 px-6 py-2.5 rounded-lg font-semibold flex items-center gap-2 transition-colors" type="button">
                        <span class="material-symbols-outlined">publish</span>
                        Upload File
                      </button>
                      <button class="bg-primary/10 text-primary hover:bg-primary/20 dark:bg-primary/20 dark:text-primary/100 px-6 py-2.5 rounded-lg font-semibold flex items-center gap-2 transition-colors" type="button">
                        <span class="material-symbols-outlined">download</span>
                        Download Template
                      </button>
                    </div>
                  </div>
                  <div class="mt-6 flex items-center gap-2 text-xs text-slate-400">
                    <span class="material-symbols-outlined text-sm">info</span>
                    <span>Maximum file size: 25MB. Ensure headers match the template.</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="bg-white dark:bg-slate-900 rounded-xl shadow-sm border border-slate-200 dark:border-slate-800 p-8 flex flex-col items-center justify-center text-center hover:-translate-y-0.5 hover:shadow-md transition-all duration-200 group">
              <div class="size-16 bg-slate-100 dark:bg-slate-800 rounded-full flex items-center justify-center mb-4 group-hover:bg-primary group-hover:text-white transition-colors">
                <span class="material-symbols-outlined text-3xl">edit_note</span>
              </div>
              <h3 class="text-slate-900 dark:text-slate-100 text-lg font-bold mb-2">Manual Entry</h3>
              <p class="text-slate-500 dark:text-slate-400 text-sm mb-6">Create questions one by one using our rich-text editor and media uploader.</p>
              <button @click="goToManual" class="w-full py-2.5 border-2 border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 rounded-lg font-semibold hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors" type="button">
                Start Editor
              </button>
            </div>
          </div>
        </section>

        <section class="animate-fade-up-delay">
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-slate-900 dark:text-slate-100 text-xl font-bold flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">list_alt</span>
              Recent Exams
            </h2>
            <div class="flex gap-2">
              <button class="p-2 text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg" type="button">
                <span class="material-symbols-outlined">filter_list</span>
              </button>
              <button class="p-2 text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg" type="button">
                <span class="material-symbols-outlined">grid_view</span>
              </button>
            </div>
          </div>

          <div class="bg-white dark:bg-slate-900 rounded-xl shadow-sm border border-slate-200 dark:border-slate-800 overflow-hidden">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="bg-slate-50 dark:bg-slate-800/50 border-b border-slate-200 dark:border-slate-800">
                  <th class="px-6 py-4 text-sm font-semibold text-slate-600 dark:text-slate-400">Exam Title</th>
                  <th class="px-6 py-4 text-sm font-semibold text-slate-600 dark:text-slate-400">Subject</th>
                  <th class="px-6 py-4 text-sm font-semibold text-slate-600 dark:text-slate-400">Questions</th>
                  <th class="px-6 py-4 text-sm font-semibold text-slate-600 dark:text-slate-400">Last Modified</th>
                  <th class="px-6 py-4 text-sm font-semibold text-slate-600 dark:text-slate-400">Status</th>
                  <th class="px-6 py-4 text-sm font-semibold text-slate-600 dark:text-slate-400">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                <tr v-for="exam in exams" :key="exam.title" class="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition-colors">
                  <td class="px-6 py-4 font-medium text-slate-900 dark:text-slate-100">{{ exam.title }}</td>
                  <td class="px-6 py-4 text-slate-500 dark:text-slate-400">{{ exam.subject }}</td>
                  <td class="px-6 py-4 text-slate-500 dark:text-slate-400">{{ exam.questions }}</td>
                  <td class="px-6 py-4 text-slate-500 dark:text-slate-400">{{ exam.modified }}</td>
                  <td class="px-6 py-4">
                    <span :class="exam.statusClass" class="px-2 py-1 text-xs font-bold rounded">{{ exam.status }}</span>
                  </td>
                  <td class="px-6 py-4">
                    <div class="flex gap-2">
                      <button @click="goToReview(exam.title)" class="px-3 py-1.5 text-xs font-semibold text-blue-600 bg-blue-50 hover:bg-blue-100 dark:bg-blue-900/20 dark:text-blue-400 rounded transition-colors" type="button">Xem</button>
                      <button class="px-3 py-1.5 text-xs font-semibold text-red-600 bg-red-50 hover:bg-red-100 dark:bg-red-900/20 dark:text-red-400 rounded transition-colors" type="button">Xóa</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
            <div class="p-4 bg-slate-50 dark:bg-slate-800/30 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between">
              <p class="text-xs text-slate-500">Showing 3 of 12 exams</p>
              <div class="flex gap-2">
                <button class="px-3 py-1 text-xs font-semibold rounded bg-white dark:bg-slate-700 border border-slate-200 dark:border-slate-600 disabled:opacity-50" type="button">Previous</button>
                <button class="px-3 py-1 text-xs font-semibold rounded bg-white dark:bg-slate-700 border border-slate-200 dark:border-slate-600" type="button">Next</button>
              </div>
            </div>
          </div>
        </section>
      </main>

      <footer class="mt-auto border-t border-slate-200 dark:border-slate-800 py-8 px-10 text-center">
        <p class="text-slate-500 text-sm">© 2023 Exam Manager Pro. All rights reserved.</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const router = useRouter()
const isDark = ref(false)

const goToCreateExam = () => {
  router.push('/teacher/exams/create')
}

const goToManual = () => {
  router.push('/teacher/exams/manual')
}

const goToReview = (examTitle) => {
  router.push({
    path: '/teacher/exams/review',
    query: { title: examTitle }
  })
}

const exams = [
  {
    title: 'Calculus II Midterm', subject: 'Mathematics', questions: '45', modified: 'Oct 12, 2023',
    status: 'Published', statusClass: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
  },
  {
    title: 'Linear Algebra Final', subject: 'Mathematics', questions: '30', modified: 'Oct 10, 2023',
    status: 'Draft', statusClass: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400'
  },
  {
    title: 'Discrete Structures Quiz 4', subject: 'Computer Science', questions: '15', modified: 'Oct 08, 2023',
    status: 'Archived', statusClass: 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-400'
  }
]
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