<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <div class="layout-container flex h-full grow flex-col">
      <TeacherTopHeader active-section="monitoring" />

      <main class="relative flex-1 p-6 lg:p-10 max-w-7xl mx-auto w-full overflow-hidden">
        <div class="pointer-events-none absolute -top-16 -left-20 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-16 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

        <div class="mb-8 relative animate-fade-up">
          <h1 class="text-3xl font-black text-slate-900 dark:text-white tracking-tight mb-2">Chọn đề thi đang hoạt động</h1>
          <p class="text-slate-500 dark:text-slate-400">Theo dõi và quản lý các bài thi đang diễn ra theo thời gian thực.</p>
        </div>

        <div class="relative flex flex-col md:flex-row gap-4 mb-8 animate-fade-up-delay">
          <div class="relative flex-1">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-primary/50">search</span>
            <input v-model="search" class="w-full pl-10 pr-4 py-3 rounded-xl border-none bg-white dark:bg-slate-800 shadow-sm focus:ring-2 focus:ring-primary text-slate-900 dark:text-white" placeholder="Tìm đề thi theo tiêu đề hoặc mã..." type="text" />
          </div>
          <div class="flex gap-2">
            <button class="flex items-center gap-2 px-4 py-3 rounded-xl bg-white dark:bg-slate-800 shadow-sm border-none text-slate-700 dark:text-slate-300 hover:bg-primary/5 transition-colors" type="button">
              <span class="material-symbols-outlined text-sm">filter_alt</span>
              <span class="font-medium">Khoa</span>
              <span class="material-symbols-outlined text-sm">expand_more</span>
            </button>
            <button class="flex items-center gap-2 px-4 py-3 rounded-xl bg-white dark:bg-slate-800 shadow-sm border-none text-slate-700 dark:text-slate-300 hover:bg-primary/5 transition-colors" type="button">
              <span class="material-symbols-outlined text-sm">subject</span>
              <span class="font-medium">Tâm lý học</span>
              <span class="material-symbols-outlined text-sm">close</span>
            </button>
          </div>
        </div>

        <div class="relative grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6 animate-fade-up-delay">
          <div v-for="exam in exams" :key="exam.id" :class="exam.cardBorder" class="bg-white dark:bg-slate-800 rounded-xl overflow-hidden shadow-sm border border-primary/5 flex flex-col group hover:-translate-y-0.5 hover:shadow-lg transition-all duration-200 border-l-4">
            <div class="p-6 flex-1">
              <div class="flex justify-between items-start mb-4">
                <span :class="exam.statusClass" class="px-2 py-1 rounded text-xs font-bold uppercase tracking-wider">{{ exam.status }}</span>
                <span class="text-slate-400 text-xs font-mono">ID: {{ exam.id }}</span>
              </div>
              <h3 class="text-xl font-bold text-slate-900 dark:text-white mb-2 leading-snug">{{ exam.title }}</h3>
              <div class="flex items-center gap-2 text-slate-500 dark:text-slate-400 text-sm mb-4">
                <span class="material-symbols-outlined text-base">meeting_room</span>
                <span>{{ exam.location }}</span>
              </div>
              <div class="grid grid-cols-2 gap-4 py-4 border-t border-slate-100 dark:border-slate-700 mt-4">
                <div>
                  <p class="text-xs text-slate-400 uppercase font-bold tracking-tighter">{{ exam.leftLabel }}</p>
                  <div class="flex items-center gap-1 mt-1">
                    <span class="material-symbols-outlined text-primary text-base">group</span>
                    <span class="text-lg font-bold text-slate-900 dark:text-white">{{ exam.students }}</span>
                  </div>
                </div>
                <div>
                  <p class="text-xs text-slate-400 uppercase font-bold tracking-tighter">{{ exam.timeLabel }}</p>
                  <div class="flex items-center gap-1 mt-1">
                    <span :class="exam.timeIconClass" class="material-symbols-outlined text-base">{{ exam.timeIcon }}</span>
                    <span class="text-lg font-bold text-slate-900 dark:text-white">{{ exam.timeValue }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div class="p-4 bg-slate-50 dark:bg-slate-800/50 border-t border-slate-100 dark:border-slate-700">
              <button :class="exam.actionClass" class="w-full font-bold py-3 px-4 rounded-lg flex items-center justify-center gap-2 transition-all" type="button" @click="openLiveSession(exam)">
                <span class="material-symbols-outlined">{{ exam.actionIcon }}</span>
                {{ exam.actionLabel }}
              </button>
            </div>
          </div>

          <div class="border-2 border-dashed border-primary/10 rounded-xl flex flex-col items-center justify-center p-8 text-center bg-transparent min-h-[300px]">
            <div class="size-16 rounded-full bg-primary/5 flex items-center justify-center mb-4">
              <span class="material-symbols-outlined text-primary/40 text-4xl">add</span>
            </div>
            <h4 class="text-slate-500 dark:text-slate-400 font-bold">Lên lịch đề thi mới</h4>
            <p class="text-slate-400 text-sm mt-1">Tạo hoặc khởi chạy bài đánh giá mới</p>
            <button class="mt-6 px-4 py-2 text-primary font-bold hover:bg-primary/5 rounded-lg border border-primary/20 transition-all" type="button">
              Xem ngân hàng đề
            </button>
          </div>
        </div>

        <div class="relative mt-12 flex flex-col md:flex-row justify-between items-center gap-4 bg-white dark:bg-slate-800 p-4 rounded-xl shadow-sm animate-fade-up-delay">
          <div class="flex items-center gap-6">
            <div class="flex items-center gap-2">
              <span class="size-3 rounded-full bg-green-500"></span>
              <span class="text-sm font-medium">2 đề thi đang hoạt động</span>
            </div>
            <div class="flex items-center gap-2">
              <span class="size-3 rounded-full bg-blue-500"></span>
              <span class="text-sm font-medium">1 đề thi đã lên lịch</span>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <button class="p-2 rounded hover:bg-primary/5 text-slate-400" type="button"><span class="material-symbols-outlined">chevron_left</span></button>
            <span class="text-sm font-bold px-4">Trang 1 / 1</span>
            <button class="p-2 rounded hover:bg-primary/5 text-slate-400" type="button"><span class="material-symbols-outlined">chevron_right</span></button>
          </div>
        </div>
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
const search = ref('')

const openLiveSession = (exam) => {
  router.push({
    path: '/teacher/live-monitoring/session',
    query: {
      title: exam.title,
      meta: exam.sessionMeta
    }
  })
}

const exams = [
  {
    id: 'PSY-2024-08',
    title: 'Thi cuối kỳ Tâm lý học nâng cao',
    location: 'Phòng 402 • Hội trường chính',
    sessionMeta: 'Phòng 402 • Bắt đầu: 09:00 • Thời lượng 120 phút',
    status: 'Đang diễn ra',
    statusClass: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
    students: '42 / 45',
    leftLabel: 'Sinh viên',
    timeLabel: 'Thời gian còn lại',
    timeIcon: 'timer',
    timeIconClass: 'text-orange-500',
    timeValue: '01:14:22',
    actionLabel: 'Theo dõi trực tiếp',
    actionIcon: 'visibility',
    actionClass: 'bg-primary hover:bg-primary/90 text-white',
    cardBorder: 'border-l-green-500'
  },
  {
    id: 'CS-101-MID',
    title: 'Thi giữa kỳ Nhập môn Khoa học máy tính',
    location: 'Phòng thi trực tuyến B',
    sessionMeta: 'Phòng trực tuyến B • Bắt đầu: 09:30 • Thời lượng 90 phút',
    status: 'Đang diễn ra',
    statusClass: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
    students: '128 / 130',
    leftLabel: 'Sinh viên',
    timeLabel: 'Thời gian còn lại',
    timeIcon: 'timer',
    timeIconClass: 'text-orange-500',
    timeValue: '00:42:05',
    actionLabel: 'Theo dõi trực tiếp',
    actionIcon: 'visibility',
    actionClass: 'bg-primary hover:bg-primary/90 text-white',
    cardBorder: 'border-l-green-500'
  },
  {
    id: 'MATH-450',
    title: 'Giải tích III: Đánh giá nhiều biến',
    location: 'Phòng 101 • Khu Khoa học',
    sessionMeta: 'Phòng 101 • Bắt đầu sau: 00:08:45 • Thời lượng 100 phút',
    status: 'Sắp bắt đầu',
    statusClass: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
    students: '12 / 28',
    leftLabel: 'Đã vào',
    timeLabel: 'Bắt đầu sau',
    timeIcon: 'schedule',
    timeIconClass: 'text-blue-500',
    timeValue: '00:08:45',
    actionLabel: 'Mở phòng điều khiển',
    actionIcon: 'play_circle',
    actionClass: 'bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-300 cursor-not-allowed',
    cardBorder: 'border-l-blue-500'
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
