<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <div class="relative flex h-auto min-h-screen w-full flex-col overflow-x-hidden">
      <TeacherTopHeader active-section="monitoring" />

      <main class="relative flex-1 max-w-[1440px] mx-auto w-full p-4 md:p-8 overflow-hidden">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-20 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

        <div class="relative flex flex-col md:flex-row md:items-end justify-between mb-8 gap-4 animate-fade-up">
          <div>
            <nav class="flex text-sm text-slate-500 dark:text-slate-400 mb-2 gap-2 items-center">
              <span class="material-symbols-outlined text-sm">home</span>
              <span>/</span>
              <span>Monitoring</span>
              <span>/</span>
              <span class="text-primary font-medium">{{ selectedExamTitle }}</span>
            </nav>
            <h1 class="text-3xl font-bold text-slate-900 dark:text-white flex items-center gap-3">
              Live Session Monitor
              <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400">
                <span class="w-2 h-2 rounded-full bg-green-500 mr-1.5 animate-pulse"></span>
                Active
              </span>
            </h1>
            <p class="text-slate-500 dark:text-slate-400 mt-1">{{ selectedExamMeta }}</p>
          </div>
          <div class="flex gap-3">
            <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-3 flex flex-col items-center min-w-[100px] hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
              <span class="text-2xl font-bold text-slate-900 dark:text-white">42</span>
              <span class="text-[10px] uppercase font-bold text-slate-500 tracking-wider">Present</span>
            </div>
            <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-3 flex flex-col items-center min-w-[100px] hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
              <span class="text-2xl font-bold text-red-600">03</span>
              <span class="text-[10px] uppercase font-bold text-slate-500 tracking-wider">Flagged</span>
            </div>
            <div class="bg-primary text-white rounded-xl p-3 flex flex-col items-center min-w-[100px]">
              <span class="text-2xl font-bold">1h 14m</span>
              <span class="text-[10px] uppercase font-bold text-white/70 tracking-wider">Remaining</span>
            </div>
          </div>
        </div>

        <section class="relative mb-10 animate-fade-up-delay">
          <h3 class="text-red-600 dark:text-red-400 text-sm font-bold uppercase tracking-widest mb-4 flex items-center gap-2">
            <span class="material-symbols-outlined text-lg">warning</span>
            Immediate Attention Required
          </h3>
          <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
            <div v-for="alert in alerts" :key="alert.student" :class="alert.cardClass" class="rounded-xl p-5 shadow-lg relative overflow-hidden group hover:-translate-y-0.5 hover:shadow-xl transition-all duration-200">
              <div v-if="alert.isCritical" class="absolute top-0 right-0 p-2">
                <span class="flex h-3 w-3 rounded-full bg-red-500 animate-ping"></span>
              </div>
              <div class="flex gap-4 items-start">
                <div :class="alert.avatarClass" class="h-16 w-16 rounded-lg overflow-hidden shrink-0 shadow-sm relative border-2">
                  <img class="w-full h-full object-cover" :src="alert.image" :alt="alert.student" />
                  <div :class="alert.feedClass" class="absolute bottom-0 inset-x-0 text-white text-[8px] text-center font-bold py-0.5 uppercase">Live Feed</div>
                </div>
                <div class="flex-1">
                  <h4 class="font-bold text-slate-900 dark:text-white text-lg leading-none">{{ alert.student }}</h4>
                  <p :class="alert.titleClass" class="text-sm font-semibold mt-1">{{ alert.title }}</p>
                  <p class="text-xs text-slate-500 dark:text-slate-400 mt-1">{{ alert.meta }}</p>
                </div>
              </div>
              <div class="mt-4 flex gap-2">
                <button :class="alert.primaryActionClass" class="flex-1 py-2 text-white rounded-lg text-sm font-bold transition-all" type="button" @click="openStudentDetail(alert)">{{ alert.primaryAction }}</button>
                <button class="px-3 py-2 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-200 rounded-lg text-sm hover:bg-slate-50 transition-all flex items-center justify-center" type="button" @click="openStudentDetail(alert)">
                  <span class="material-symbols-outlined text-lg">videocam</span>
                </button>
              </div>
            </div>
          </div>
        </section>

        <section class="relative bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 overflow-hidden animate-fade-up-delay">
          <div class="px-6 py-4 border-b border-slate-200 dark:border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <h3 class="text-lg font-bold text-slate-900 dark:text-white">Active Student Sessions</h3>
            <div class="flex items-center gap-2">
              <button class="flex items-center gap-2 px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-800 text-xs font-semibold text-slate-600 dark:text-slate-300 bg-slate-50 dark:bg-slate-800/50" type="button">
                <span class="material-symbols-outlined text-sm">filter_list</span> Filter: Status
              </button>
              <button class="flex items-center gap-2 px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-800 text-xs font-semibold text-slate-600 dark:text-slate-300 bg-slate-50 dark:bg-slate-800/50" type="button">
                <span class="material-symbols-outlined text-sm">sort</span> Sort: Progress
              </button>
            </div>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full border-collapse">
              <thead>
                <tr class="bg-slate-50 dark:bg-slate-800/50 text-left border-b border-slate-200 dark:border-slate-800">
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Student</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Progress</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Activity Status</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Camera/Audio</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider text-right">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                <tr v-for="student in students" :key="student.id" :class="student.rowClass" class="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition-colors">
                  <td class="px-6 py-4">
                    <div class="flex items-center gap-3">
                      <div class="size-10 rounded-full bg-slate-200 overflow-hidden shrink-0">
                        <img class="w-full h-full object-cover" :src="student.image" :alt="student.name" />
                      </div>
                      <div>
                        <p class="font-bold text-slate-900 dark:text-white">{{ student.name }}</p>
                        <p class="text-xs text-slate-500">ID: {{ student.id }}</p>
                      </div>
                    </div>
                  </td>
                  <td class="px-6 py-4">
                    <div class="w-full max-w-[140px]">
                      <div :class="student.progressTextClass" class="flex justify-between text-[10px] font-bold mb-1">
                        <span>{{ student.progress }}%</span>
                        <span>{{ student.questions }}</span>
                      </div>
                      <div class="h-1.5 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                        <div :class="student.progressBarClass" class="h-full rounded-full" :style="{ width: `${student.progress}%` }"></div>
                      </div>
                    </div>
                  </td>
                  <td class="px-6 py-4">
                    <span :class="student.statusClass" class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium border">{{ student.status }}</span>
                  </td>
                  <td class="px-6 py-4">
                    <div class="flex gap-2">
                      <span :class="student.cameraClass" class="material-symbols-outlined text-lg">{{ student.cameraIcon }}</span>
                      <span :class="student.micClass" class="material-symbols-outlined text-lg">{{ student.micIcon }}</span>
                    </div>
                  </td>
                  <td class="px-6 py-4 text-right">
                    <button class="p-2 text-slate-400 hover:text-primary transition-colors" type="button">
                      <span class="material-symbols-outlined">more_vert</span>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="px-6 py-4 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between">
            <p class="text-sm text-slate-500 dark:text-slate-400">Showing 4 of 42 active students</p>
            <div class="flex gap-2">
              <button class="px-4 py-2 text-sm font-semibold text-slate-600 dark:text-slate-300 border border-slate-200 dark:border-slate-800 rounded-lg disabled:opacity-50" disabled type="button">Previous</button>
              <button class="px-4 py-2 text-sm font-semibold text-white bg-primary rounded-lg" type="button">Next</button>
            </div>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const route = useRoute()
const router = useRouter()
const isDark = ref(false)

const openStudentDetail = (alert) => {
  router.push({
    path: '/teacher/live-monitoring/student-detail',
    query: {
      student: alert.student,
      studentId: alert.studentId,
      exam: selectedExamTitle.value,
      avatar: alert.image
    }
  })
}

const selectedExamTitle = computed(() => route.query.title || 'Final Exam: Advanced Psychology')
const selectedExamMeta = computed(() => route.query.meta || 'Room 302 • Started: 09:00 AM • 120m duration')

const alerts = [
  {
    student: 'Robert Fox',
    title: 'Multiple tab switches detected',
    studentId: 'STU-88293',
    meta: 'Occurred 2m ago • Flag 4/3',
    image: 'https://lh3.googleusercontent.com/aida-public/AB6AXuAIJ4SxrDKzIOAxYgwMxfCl9VECA94Lz5URlQAHT8wlNhUW4MNw9D1HLca97xsa6unSv5ggAypK-LlUtn8ncy9v-bzAFa3_8M1DavSb0bTFXN0n5cGflRifrcKtMGsdiwoybTbp9NCUsP7loGo43vS15PYo7KCItvkic1tbE7vNg_JatXHLLpScEs05NVeQLNXTuq294VTZO_Ynq_xP5jG7khjmSKYRitHROXp4_84cfjODRxtCxXX59LP1gTMP-0pkrK6iKTNdhA',
    cardClass: 'bg-red-50 dark:bg-red-900/10 border-2 border-red-500',
    avatarClass: 'border-red-200',
    feedClass: 'bg-red-600',
    titleClass: 'text-red-600',
    primaryAction: 'Intervene Now',
    primaryActionClass: 'bg-red-600 hover:bg-red-700',
    isCritical: true
  },
  {
    student: 'Bessie Cooper',
    title: 'Secondary device detected',
    studentId: 'STU-88294',
    meta: 'AI Detection • High Confidence',
    image: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCyI0vEAOjG_D5V9g5E1wLhdDkGHMJ8GVF_GV-9onQ_6jhLivWqXRJZy6Jt9qOJncK-v08UIaC-mP6wFLfjMNK-2hPQeFZxy-gmCW42dG2K7mGLADSKyIdvZVQ8YXiXbpeiRwl4Wg0hETNenLVJcxayfOxqMAWZJ5wlHSrUbzvDIlIel_ItlkwbmpUhZD9UMoE5fwDuk7x6iySOdkzXIlhDZXR5NfKpKGqmgVtgd6aqimE7ShOyJ-S6xbljlGFcogapiGpgspSEjg',
    cardClass: 'bg-red-50 dark:bg-red-900/10 border-2 border-red-500',
    avatarClass: 'border-red-200',
    feedClass: 'bg-red-600',
    titleClass: 'text-red-600',
    primaryAction: 'Intervene Now',
    primaryActionClass: 'bg-red-600 hover:bg-red-700',
    isCritical: false
  },
  {
    student: 'Cameron Williamson',
    title: 'Face out of frame',
    studentId: 'STU-88295',
    meta: 'Duration: 15 seconds',
    image: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDtMjF5mj_ClCW9EmyXg00BZV0rxWtVROfIwWA_31knvMxtMb_hH2lQ8TLf9rJyzD4FmPtqyem5oFpeaPmHw5ouM3R99aY5Pu3hq2Xko-tuuAxzgS9nj6NlEFpSPEzM3Kg58Sj4L4SbKTAa4jOkdtdibCyWYQOyMJkbY2pAFrELirAHczDVLdy4a68rsypmWOwgfKjViF6Y2Xvq66P-1m1IQn2VQyLsbEMvXwOqPkHB0XwKLBX9BQ-obz5Afm55qqj4EJoGuAyINA',
    cardClass: 'bg-amber-50 dark:bg-amber-900/10 border-2 border-amber-500',
    avatarClass: 'border-amber-200',
    feedClass: 'bg-amber-600',
    titleClass: 'text-amber-600',
    primaryAction: 'Issue Warning',
    primaryActionClass: 'bg-amber-600 hover:bg-amber-700',
    isCritical: false
  }
]

const students = [
  {
    name: 'Jerome Bell',
    id: '#45920',
    image: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBbu_v9mGtU7dAJuLh_cqxHsNWPFe9CW2ZCoGNHs_czGAfLkheNxFN_NijT54zea-4uc_GPTOfqtxXhT0iw8vW4AWFqinfW-RiBuCirB6aLZJXs-Fhv2_TxNZTVr5l3GK71DopGZnRXsUfA4ZKzKhfGm5ocZj5AU2M5hEYp7wAjzFv9LlfdEscMmjU4eLaT8IkVnFGV-K5F8-qbDLTLIx83l2s-Cg5nF6AF4dTiokaj9GqW-iBbt8gWcr_hzt16YizH5D4drenrJQ',
    progress: 75,
    questions: '45/60 Qns',
    status: 'Normal',
    statusClass: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400 border-green-200 dark:border-green-800',
    progressTextClass: '',
    progressBarClass: 'bg-primary',
    cameraIcon: 'videocam',
    micIcon: 'mic',
    cameraClass: 'text-green-500',
    micClass: 'text-green-500',
    rowClass: ''
  },
  {
    name: 'Arlene McCoy',
    id: '#45921',
    image: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDumshticGOziPlke8Nf8YPuFfp3KHAVPNgtyy4HFet1fjuR-zpM-suYsDQWPCrjl-YvZfJMh4IbEbqACGYz5sBZ9HJxjOZImL13JHfESnUQMudMt95juyQFBqEx7i7bc3x-gBYyubI53ElNcytUMw61X4stoGJOIipFqVC12UZUTSF8kntNfDuTJ7LL2f6VAmhliK-bwvGWEXBhrSazEhRy6or5d9HSgDRAEOVl9nt4Kub8FAPUJ7-jRsG5ms19fCAR6LyU71PuA',
    progress: 42,
    questions: '25/60 Qns',
    status: 'Reviewing',
    statusClass: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400 border-blue-200 dark:border-blue-800',
    progressTextClass: '',
    progressBarClass: 'bg-primary',
    cameraIcon: 'videocam',
    micIcon: 'mic_off',
    cameraClass: 'text-green-500',
    micClass: 'text-slate-300 dark:text-slate-600',
    rowClass: ''
  },
  {
    name: 'Devon Lane',
    id: '#45922',
    image: 'https://lh3.googleusercontent.com/aida-public/AB6AXuAuFFwh3iRxpN-9mYHlBN7AxkIVhtzlvg-zbI3AMhEviuAkYY2YNuwz-NUSCQiH6iFO1o_5X2tpGpGRlJ_DR5fLfA0vrObO3zC3SYxkdPsITPrWqTXvdKZg-1BOYLvjxS51kf2T5CUZ1qDMUrIht79v-kDiZK8aJLRbXnjlQR4rAdUPn3Tgj2QIAnZ0z-Ya2KPjm9NS1AQilHKtdd6cc3b6wHtBHtwktskWNw4fV-nGCFkLWyOTncE0xDDOb9xTHvtZINPLRIw6_Q',
    progress: 92,
    questions: '55/60 Qns',
    status: 'Idle (1m)',
    statusClass: 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-400 border-slate-200 dark:border-slate-700',
    progressTextClass: '',
    progressBarClass: 'bg-primary',
    cameraIcon: 'videocam',
    micIcon: 'mic',
    cameraClass: 'text-green-500',
    micClass: 'text-green-500',
    rowClass: ''
  },
  {
    name: 'Esther Howard',
    id: '#45923',
    image: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDJGLtVqDWiqr09yUYr_EKVfQ-r70N3UjRMOqeWxaAprZiU6nZJws9kNPNFFMbWG7ZDXumDSi6Hwc8iSgpka-HkHG6-1322BcZsj7rwrYoGIObyDUwZyOPmezxi1A3CpRdGtoJ0XgFtcTcOsQC6CRy6biwHpDD6ilY1MrxbvsGzNQJDoyihPA6hliXu7tW2LoEHLgyAKrOFRpsV1hASERYghA7RP_UJXATXVNYnPO9qX-jkyA0ZHMxQdOZJoD60dT9lhxX518nKBg',
    progress: 60,
    questions: '36/60 Qns',
    status: 'Suspicious Activity',
    statusClass: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400 border-red-200 dark:border-red-800',
    progressTextClass: 'text-red-600',
    progressBarClass: 'bg-red-500',
    cameraIcon: 'videocam_off',
    micIcon: 'mic',
    cameraClass: 'text-red-500',
    micClass: 'text-green-500',
    rowClass: 'bg-red-50/30 dark:bg-red-900/5'
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
