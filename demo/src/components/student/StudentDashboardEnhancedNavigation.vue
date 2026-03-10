<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100"
  >
    <div class="relative flex h-auto min-h-screen w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full grow flex-col">
        <header
          class="flex items-center justify-between whitespace-nowrap border-b border-solid border-primary/10 bg-white dark:bg-background-dark px-6 md:px-20 py-4"
        >
          <div class="flex items-center gap-3 text-primary dark:text-slate-100">
            <div class="size-8 bg-primary rounded-lg flex items-center justify-center text-white">
              <span class="material-symbols-outlined">menu_book</span>
            </div>
            <h2 class="text-xl font-bold leading-tight tracking-tight">ExamPortal</h2>
          </div>
          <div class="flex flex-1 justify-end gap-4 items-center">
            <button class="flex items-center justify-center rounded-full size-10 bg-primary/10 text-primary hover:bg-primary/20 hover:-translate-y-0.5 transition-all duration-200" type="button">
              <span class="material-symbols-outlined">notifications</span>
            </button>
            <button
              type="button"
              @click="goToLogin"
              class="text-xs font-semibold px-3 py-1.5 rounded bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700"
            >
              Sign Out
            </button>
            <div class="flex items-center gap-3 pl-2 border-l border-primary/10">
              <div class="hidden md:block text-right">
                <p class="text-sm font-bold">Alex Johnson</p>
                <p class="text-xs text-slate-500">ID: 4421-STU</p>
              </div>
              <div
                class="bg-center bg-no-repeat aspect-square bg-cover rounded-full size-10 border-2 border-primary/20"
                style='background-image: url("https://lh3.googleusercontent.com/aida-public/AB6AXuB1e4bA4nxwNhHI-Fn4dFl5ffPsV2Qcq-aTU38W1KpDGwJVtSs8Uu50HUjL-6AQ1rsj8FzgZ85caSzJLBV2kIzkBDQx4LboDGfHJTBM4ekHERyEJBMrHARjYfGyK-OOed1VR2AVLI8Set2ttmV6DKD-1ADupmLpYFhEoCRyviMIao-qfOPN6LDLGiDaSyvu15GGz3wp3epYInY9djSMLy1DHqOjBrCWEn_nXjmRki5_ystPT2x5YTemNdGgEHmK39v616MkRN2Pcg")'
              ></div>
            </div>
          </div>
        </header>

        <main class="relative flex-1 max-w-6xl mx-auto w-full p-6 md:p-10 space-y-10 overflow-hidden">
          <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
          <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

          <section class="relative flex flex-col gap-2 animate-fade-up">
            <h1 class="text-3xl md:text-4xl font-black tracking-tight text-primary dark:text-slate-100">Welcome back, Alex</h1>
            <p class="text-slate-600 dark:text-slate-400 text-lg">Your dashboard for exams and self-paced learning.</p>
          </section>

          <div class="relative grid grid-cols-1 lg:grid-cols-3 gap-8 animate-fade-up-delay">
            <div class="lg:col-span-2 space-y-8">
              <div class="bg-white dark:bg-slate-900 p-6 rounded-xl border border-primary/10 shadow-sm">
                <div class="flex items-center gap-3 mb-6">
                  <span class="material-symbols-outlined text-primary text-3xl">login</span>
                  <h2 class="text-xl font-bold">Join Examination</h2>
                </div>
                <div class="flex flex-col md:flex-row items-end gap-4">
                  <div class="flex flex-col flex-1 w-full">
                    <label class="text-sm font-semibold mb-2 text-slate-700 dark:text-slate-300">Exam or Teacher Code</label>
                    <input
                      v-model="examCode"
                      class="w-full rounded-lg border border-primary/20 bg-background-light dark:bg-background-dark px-4 py-3 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                      placeholder="e.g. EXAM-2024-MATH"
                      type="text"
                    />
                  </div>
                  <button @click="goToWaitingRoom" class="w-full md:w-auto px-8 py-3 bg-primary text-white font-bold rounded-lg hover:bg-primary/90 hover:-translate-y-0.5 transition-all duration-200" type="button">
                    Enter Room
                  </button>
                </div>
                <p class="mt-3 text-xs text-slate-500">Please ensure you have a stable internet connection before joining.</p>
              </div>

              <div class="bg-white dark:bg-slate-900 p-6 rounded-xl border border-primary/10 shadow-sm">
                <div class="flex items-center justify-between mb-6">
                  <div class="flex items-center gap-3">
                    <span class="material-symbols-outlined text-primary text-3xl">model_training</span>
                    <h2 class="text-xl font-bold">Self-Study &amp; Practice</h2>
                  </div>
                  <button class="flex items-center gap-2 text-sm font-bold text-primary hover:underline" type="button">
                    <span class="material-symbols-outlined text-sm">download</span>
                    Template
                  </button>
                </div>
                <label
                  for="file-upload"
                  class="border-2 border-dashed border-primary/20 rounded-xl p-8 flex flex-col items-center justify-center bg-primary/5 hover:bg-primary/10 transition-colors cursor-pointer group"
                >
                  <span class="material-symbols-outlined text-4xl text-primary/60 group-hover:text-primary transition-colors mb-4">upload_file</span>
                  <p class="font-bold text-slate-800 dark:text-slate-200">Upload CSV or XLSX to generate test</p>
                  <p class="text-sm text-slate-500 mt-1">Maximum file size: 5MB</p>
                  <input id="file-upload" class="hidden" type="file" @change="goToGeneratePractice" />
                </label>
                <div class="mt-6 grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div class="p-4 bg-background-light dark:bg-background-dark rounded-lg flex items-center gap-3 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                    <span class="material-symbols-outlined text-primary">auto_awesome</span>
                    <div class="text-sm">
                      <p class="font-bold">AI Generation</p>
                      <p class="text-slate-500">Questions created from your notes</p>
                    </div>
                  </div>
                  <div class="p-4 bg-background-light dark:bg-background-dark rounded-lg flex items-center gap-3 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                    <span class="material-symbols-outlined text-primary">timer</span>
                    <div class="text-sm">
                      <p class="font-bold">Timed Sessions</p>
                      <p class="text-slate-500">Simulate real exam conditions</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="lg:col-span-1">
              <div class="bg-white dark:bg-slate-900 rounded-xl border border-primary/10 shadow-sm flex flex-col h-full">
                <div class="p-6 border-b border-primary/10">
                  <div class="flex items-center gap-3">
                    <span class="material-symbols-outlined text-primary text-3xl">history</span>
                    <h2 class="text-xl font-bold">Exam History</h2>
                  </div>
                </div>
                <div class="p-0 overflow-y-auto max-h-[600px]">
                  <div
                    v-for="item in historyItems"
                    :key="item.title"
                    @click="goToExamResult(item)"
                    class="p-4 border-b border-primary/5 hover:bg-background-light dark:hover:bg-background-dark transition-colors cursor-pointer"
                  >
                    <div class="flex justify-between items-start mb-1">
                      <p class="font-bold text-slate-800 dark:text-slate-200">{{ item.title }}</p>
                      <span class="text-primary font-bold">{{ item.score }}</span>
                    </div>
                    <div class="flex justify-between text-xs text-slate-500">
                      <span>{{ item.date }}</span>
                      <span>{{ item.grade }}</span>
                    </div>
                  </div>
                </div>
                <div class="p-4 mt-auto">
                  <button @click="goToStudyHistory" class="w-full py-3 text-sm font-bold text-white bg-primary rounded-lg hover:bg-primary/90 hover:-translate-y-0.5 transition-all duration-200 shadow-md shadow-primary/20" type="button">
                    View Full Transcript
                  </button>
                </div>
              </div>
            </div>
          </div>
        </main>

        <footer class="bg-white dark:bg-background-dark border-t border-primary/10 py-6 px-10 text-center text-slate-500 text-sm">
          <p>© 2024 ExamPortal Online Examination System. All rights reserved.</p>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const isDark = ref(false)
const examCode = ref('')

const historyItems = [
  { title: 'Advanced Mathematics', score: '92%', date: 'Oct 12, 2023', grade: 'Grade: A' },
  { title: 'Physics II: Optics', score: '85%', date: 'Oct 05, 2023', grade: 'Grade: B+' },
  { title: 'World History 101', score: '78%', date: 'Sep 28, 2023', grade: 'Grade: B' },
  { title: 'Intro to Sociology', score: '88%', date: 'Sep 15, 2023', grade: 'Grade: A-' },
  { title: 'Organic Chemistry', score: '64%', date: 'Sep 02, 2023', grade: 'Grade: C+' }
]

const goToWaitingRoom = () => {
  router.push({
    path: '/student/exam-waiting-room',
    query: { exam: examCode.value || 'Advanced Psychology Midterm' }
  })
}

const goToGeneratePractice = (event) => {
  const fileName = event.target.files?.[0]?.name || 'Physics_Notes.pdf'
  router.push({
    path: '/student/generate-practice-test',
    query: { file: fileName }
  })
}

const goToStudyHistory = () => {
  router.push('/student/study-history')
}

const goToExamResult = (item) => {
  router.push({
    path: '/student/exam-result',
    query: {
      exam: item.title,
      score: Number.parseInt(item.score, 10),
      attempted: `Attempted on ${item.date}`
    }
  })
}

const goToLogin = () => {
  router.push('/login')
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