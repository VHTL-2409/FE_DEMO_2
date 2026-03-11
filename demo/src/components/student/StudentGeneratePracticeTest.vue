<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <div class="relative flex h-auto min-h-screen w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full grow flex-col">
        <StudentTopHeader />

        <main class="relative flex flex-1 justify-center py-8 px-4 md:px-0 overflow-hidden">
          <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
          <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

          <div class="layout-content-container relative flex flex-col max-w-[800px] flex-1 gap-6">
            <div class="flex items-center animate-fade-up">
              <button @click="goBack" class="flex items-center gap-2 text-primary font-medium hover:underline text-sm" type="button">
                <span class="material-symbols-outlined text-sm">arrow_back</span>
                Quay lại trang chủ
              </button>
            </div>

            <div class="flex flex-col gap-2 animate-fade-up">
              <h1 class="text-slate-900 dark:text-slate-100 text-3xl font-bold leading-tight">Cấu hình bài luyện tập</h1>
              <p class="text-slate-500 dark:text-slate-400 text-base font-normal">Tệp của bạn đã được xử lý. Hãy tùy chỉnh phiên luyện tập theo mục tiêu học tập của bạn.</p>
            </div>

            <div class="p-1 rounded-xl bg-gradient-to-r from-primary/20 to-primary/5 animate-fade-up-delay">
              <div class="flex flex-col md:flex-row items-center justify-between gap-6 rounded-lg bg-white dark:bg-slate-800 p-6 shadow-sm border border-primary/5 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                <div class="flex items-center gap-5 flex-1">
                  <div class="size-16 flex items-center justify-center rounded-xl bg-primary/10 text-primary shrink-0">
                    <span class="material-symbols-outlined text-3xl">description</span>
                  </div>
                  <div class="flex flex-col gap-1">
                    <p class="text-slate-400 dark:text-slate-500 text-xs font-semibold uppercase tracking-wider">Nội dung đã tải lên</p>
                    <p class="text-slate-900 dark:text-slate-100 text-xl font-bold leading-tight">{{ fileName }}</p>
                    <div class="flex items-center gap-2 text-slate-500 dark:text-slate-400 text-sm">
                      <span class="material-symbols-outlined text-sm">auto_awesome</span>
                      <span>Đã phát hiện 15 trang • Xác định 4 chương chính</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="flex flex-col gap-8 bg-white dark:bg-slate-800 p-8 rounded-xl shadow-sm border border-primary/5 animate-fade-up-delay">
              <h3 class="text-slate-900 dark:text-slate-100 text-xl font-bold leading-tight border-b border-primary/10 pb-4">Thiết lập bài kiểm tra</h3>

              <div class="space-y-2">
                <label class="text-slate-900 dark:text-slate-100 text-base font-semibold">Số lượng câu hỏi</label>
                <input
                  v-model.number="questionCount"
                  type="number"
                  min="5"
                  max="50"
                  class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                />
                <p class="text-xs text-slate-500 dark:text-slate-400">Chọn từ 5 đến 50 câu hỏi.</p>
              </div>

              <div class="space-y-4">
                <div class="flex items-center justify-between">
                  <label class="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-2">
                    <span class="material-symbols-outlined text-sm">timer</span>
                    Giới hạn thời gian (phút)
                  </label>
                  <span class="text-primary font-bold">{{ timeLimit }} min</span>
                </div>
                <input v-model="timeLimit" class="w-full h-2 bg-slate-200 dark:bg-slate-700 rounded-lg appearance-none cursor-pointer accent-primary" type="range" min="5" max="240" step="5" />
              </div>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div class="space-y-2">
                  <label class="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-2">
                    <span class="material-symbols-outlined text-sm">calendar_today</span>
                    Ngày &amp; giờ bắt đầu
                  </label>
                  <input v-model="startAt" class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all" type="datetime-local" />
                </div>
                <div class="space-y-2">
                  <label class="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-2">
                    <span class="material-symbols-outlined text-sm">event_busy</span>
                    Ngày &amp; giờ kết thúc
                  </label>
                  <input v-model="endAt" class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all" type="datetime-local" />
                </div>
              </div>
            </div>

            <div class="flex flex-col gap-4 mt-4 animate-fade-up-delay">
              <button @click="startPractice" class="flex w-full items-center justify-center gap-3 rounded-xl h-14 bg-primary text-white text-lg font-bold shadow-lg shadow-primary/25 hover:bg-primary/90 transition-all" type="button">
                <span class="material-symbols-outlined">rocket_launch</span>
                Tạo &amp; bắt đầu luyện tập
              </button>
              <p class="text-center text-slate-400 text-xs">
                Khi bắt đầu, AI sẽ tạo các câu hỏi riêng dựa trên nội dung tài liệu của bạn.
              </p>
            </div>
          </div>
        </main>

        <footer class="py-10 text-center text-slate-400 text-sm">
          <p>© 2024 StudyPrep AI. Bảo lưu mọi quyền.</p>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import StudentTopHeader from './StudentTopHeader.vue'

const router = useRouter()
const route = useRoute()
const isDark = ref(false)
const questionCount = ref(20)
const timeLimit = ref(30)
const startAt = ref('')
const endAt = ref('')

const fileName = computed(() => route.query.file || 'Ghi_chu_Vat_ly.pdf')

const goBack = () => {
  router.push('/student/dashboard')
}

const startPractice = () => {
  router.push({
    path: '/student/exam-interface',
    query: { exam: `Bài luyện tập - ${fileName.value}` }
  })
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
