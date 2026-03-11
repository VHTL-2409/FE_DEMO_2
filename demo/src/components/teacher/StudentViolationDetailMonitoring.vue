<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark text-slate-900 dark:text-slate-100 font-display min-h-screen">
    <div class="relative flex h-auto min-h-screen w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full grow flex-col">
        <TeacherTopHeader active-section="monitoring" />

        <main class="relative flex flex-col flex-1 px-4 lg:px-20 py-8 max-w-[1400px] mx-auto w-full overflow-hidden">
          <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
          <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

          <div class="relative flex flex-wrap items-center gap-2 mb-6 animate-fade-up">
            <span class="text-slate-500 text-sm">Đề thi</span>
            <span class="material-symbols-outlined text-sm text-slate-400">chevron_right</span>
            <span class="text-slate-500 text-sm">{{ examName }}</span>
            <span class="material-symbols-outlined text-sm text-slate-400">chevron_right</span>
            <span class="text-slate-900 dark:text-slate-100 text-sm font-bold">Giám sát: {{ studentName }}</span>
          </div>

          <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 mb-8 flex flex-col md:flex-row justify-between items-start md:items-center gap-6 shadow-sm animate-fade-up-delay hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
            <div class="flex gap-5 items-center">
              <div class="relative">
                <div class="bg-center bg-no-repeat aspect-square bg-cover rounded-full h-20 w-20 border-2 border-red-500" :style="`background-image: url('${studentAvatar}')`"></div>
                <div class="absolute bottom-0 right-0 bg-red-600 h-6 w-6 rounded-full border-2 border-white dark:border-slate-900 flex items-center justify-center">
                  <span class="material-symbols-outlined text-[14px] text-white font-bold">warning</span>
                </div>
              </div>
              <div>
                <div class="flex items-center gap-3 mb-1">
                  <h1 class="text-slate-900 dark:text-slate-100 text-2xl font-bold">{{ studentName }}</h1>
                  <span class="px-3 py-1 bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400 text-xs font-bold uppercase rounded-full tracking-wider animate-pulse border border-red-200 dark:border-red-800">
                    Hoạt động đáng ngờ
                  </span>
                </div>
                <p class="text-slate-500 dark:text-slate-400 text-sm flex items-center gap-4">
                  <span>ID: <span class="font-medium text-slate-700 dark:text-slate-300">{{ studentId }}</span></span>
                  <span class="h-4 w-px bg-slate-300 dark:bg-slate-700"></span>
                  <span>Đề thi: <span class="font-medium text-slate-700 dark:text-slate-300">{{ examName }}</span></span>
                </p>
              </div>
            </div>
            <div class="flex gap-3 w-full md:w-auto">
              <button class="flex-1 md:flex-none flex items-center justify-center gap-2 px-6 py-2.5 rounded-lg border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 font-bold text-sm hover:bg-slate-50 dark:hover:bg-slate-800 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200" type="button">
                <span class="material-symbols-outlined text-lg">history</span> Xem nhật ký
              </button>
              <button class="flex-1 md:flex-none flex items-center justify-center gap-2 px-6 py-2.5 rounded-lg bg-primary text-white font-bold text-sm hover:opacity-90 transition-all" type="button">
                <span class="material-symbols-outlined text-lg">chat</span> Trò chuyện trực tiếp
              </button>
            </div>
          </div>

          <div class="relative grid grid-cols-1 lg:grid-cols-3 gap-8 animate-fade-up-delay">
            <div class="lg:col-span-2 flex flex-col gap-6">
              <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden shadow-sm">
                <div class="p-4 border-b border-slate-200 dark:border-slate-800 flex justify-between items-center bg-slate-50 dark:bg-slate-800/50">
                  <div class="flex items-center gap-2">
                    <span class="flex h-2 w-2 rounded-full bg-red-500"></span>
                    <h3 class="font-bold text-slate-900 dark:text-slate-100">Luồng trực tiếp - Màn hình hiện tại</h3>
                  </div>
                  <div class="flex gap-2">
                    <span class="text-xs bg-slate-200 dark:bg-slate-700 px-2 py-1 rounded text-slate-600 dark:text-slate-400 font-medium uppercase">1080p</span>
                    <span class="text-xs bg-slate-200 dark:bg-slate-700 px-2 py-1 rounded text-slate-600 dark:text-slate-400 font-medium uppercase">Mã hóa</span>
                  </div>
                </div>
                <div class="relative aspect-video bg-slate-900 flex items-center justify-center group">
                  <div class="absolute inset-0 bg-cover bg-center opacity-80" style="background-image: url('https://lh3.googleusercontent.com/aida-public/AB6AXuCvhXV2sdkMp4W2ffIoqzyfpojUOUjoYWCgLENEw6jGPAEyakBzOLVV326qs53ZLP9cecR4r1jR90UfB64FpbFkIXb-iLSxtTbHjmV8fJbeX7qntdiGj4QGEKm4VbEwPs9EaZYKeUzJdJ4KTAJR25evdSsQCkDXXqJYAJhWrFonOM7_GSC6sG7m9Yp-AYxNkZ8HB5j_jJ7pOEIcyZC2HDBaSL2-_wxckc48ydhrSO1P037tcfqD_4uG8yGDSAoznGeLWY70D4viaw');"></div>
                  <div class="absolute inset-0 flex flex-col items-center justify-center bg-black/40">
                    <div class="bg-red-600 text-white px-4 py-2 rounded-lg font-bold flex items-center gap-2 shadow-xl mb-4">
                      <span class="material-symbols-outlined">visibility</span> Phát hiện cửa sổ ngoài
                    </div>
                  </div>
                </div>
              </div>

              <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div class="bg-white dark:bg-slate-900 p-5 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
                  <p class="text-slate-500 dark:text-slate-400 text-xs font-bold uppercase tracking-wider mb-2">Tiến độ làm bài</p>
                  <div class="flex items-end justify-between">
                    <p class="text-2xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">12 <span class="text-slate-400 font-normal">/ 40</span></p>
                    <span class="text-emerald-600 bg-emerald-50 dark:bg-emerald-900/20 text-xs font-bold px-2 py-1 rounded">Hoàn thành 30%</span>
                  </div>
                  <div class="w-full bg-slate-100 dark:bg-slate-800 h-1.5 rounded-full mt-4">
                    <div class="bg-primary h-1.5 rounded-full" style="width: 30%"></div>
                  </div>
                </div>
                <div class="bg-white dark:bg-slate-900 p-5 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
                  <p class="text-slate-500 dark:text-slate-400 text-xs font-bold uppercase tracking-wider mb-2">Sự kiện bị gắn cờ</p>
                  <div class="flex items-end justify-between">
                    <p class="text-2xl font-bold text-red-600 tracking-tight">6 <span class="text-slate-400 font-normal text-lg">Tổng</span></p>
                    <span class="text-red-600 bg-red-50 dark:bg-red-900/20 text-xs font-bold px-2 py-1 rounded">+200% xu hướng</span>
                  </div>
                </div>
                <div class="bg-white dark:bg-slate-900 p-5 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
                  <p class="text-slate-500 dark:text-slate-400 text-xs font-bold uppercase tracking-wider mb-2">Thời gian còn lại</p>
                  <div class="flex items-end justify-between">
                    <p class="text-2xl font-bold text-slate-900 dark:text-slate-100 tracking-tight">42:15</p>
                    <span class="text-slate-500 bg-slate-100 dark:bg-slate-800 text-xs font-bold px-2 py-1 rounded">01:30:00 Tổng</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="flex flex-col gap-6">
              <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden shadow-sm flex-1">
                <div class="p-4 border-b border-slate-200 dark:border-slate-800 flex justify-between items-center">
                  <h3 class="font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                    <span class="material-symbols-outlined text-red-500">assignment_late</span>
                    Nhật ký vi phạm
                  </h3>
                  <button class="text-xs text-primary font-bold hover:underline" type="button">Xóa tất cả</button>
                </div>
                <div class="divide-y divide-slate-100 dark:divide-slate-800">
                  <div class="p-4 bg-red-50 dark:bg-red-900/10 flex gap-4">
                    <div class="shrink-0 text-red-600"><span class="material-symbols-outlined">tab_unselected</span></div>
                    <div>
                      <div class="flex justify-between items-start mb-1">
                        <p class="text-sm font-bold text-slate-900 dark:text-slate-100">Phát hiện chuyển tab</p>
                        <span class="text-[10px] text-slate-500 font-medium">10:42 AM</span>
                      </div>
                      <p class="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">Sinh viên chuyển tab sang tài nguyên không được phép. <span class="font-bold text-red-600">(Xảy ra 4 lần)</span></p>
                    </div>
                  </div>
                  <div class="p-4 flex gap-4">
                    <div class="shrink-0 text-amber-500"><span class="material-symbols-outlined">content_paste</span></div>
                    <div>
                      <div class="flex justify-between items-start mb-1">
                        <p class="text-sm font-bold text-slate-900 dark:text-slate-100">Sử dụng clipboard</p>
                        <span class="text-[10px] text-slate-500 font-medium">10:38 AM</span>
                      </div>
                      <p class="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">Sinh viên đã cố dán nội dung vào ô trả lời ngắn từ nguồn bên ngoài.</p>
                    </div>
                  </div>
                </div>
              </div>

              <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-6 shadow-sm">
                <h3 class="font-bold text-slate-900 dark:text-slate-100 mb-6 flex items-center gap-2">
                  <span class="material-symbols-outlined text-primary">settings_applications</span>
                  Điều khiển giám thị
                </h3>
                <div class="flex flex-col gap-3">
                  <button class="w-full flex items-center justify-center gap-3 py-3 px-4 bg-amber-500 hover:bg-amber-600 text-white font-bold rounded-lg transition-colors" type="button">
                    <span class="material-symbols-outlined">warning</span> Gửi cảnh báo
                  </button>
                  <button class="w-full flex items-center justify-center gap-3 py-3 px-4 bg-red-600 hover:bg-red-700 text-white font-bold rounded-lg transition-colors" type="button">
                    <span class="material-symbols-outlined">block</span> Hủy hiệu lực bài thi
                  </button>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const route = useRoute()
const isDark = ref(false)

const studentName = computed(() => route.query.student || 'Marcus Wright')
const studentId = computed(() => route.query.studentId || 'STU-88293')
const examName = computed(() => route.query.exam || 'Sinh học nâng cao 101 - Giữa kỳ')
const studentAvatar = computed(
  () =>
    route.query.avatar ||
    'https://lh3.googleusercontent.com/aida-public/AB6AXuCS2RhYd0VS4fVVYoA2GgJuZzt6TTNNleFcdImbGSB3IZsBGlQY3W3_8DnpvWiAQ08yeiCuKP9ul1X8fZKIsAa2kb6Fz6jRM8rrBUK30-_8x487epcjiAGOetJYM1jwVrdUReYJacR8oMZG3jkyeYdbaMIJksyOphO4c7DzJeO8jaczGPgMZ5nrqWAw003cc3HOtSP0jxqftO4wuJCLWj7meS0yE7HT-vnt3XHDdKYANhCOuN2bCqpr_GQhbNfL4asVsRK5JTBzwQ'
)
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
