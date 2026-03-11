<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="bg-background-light dark:bg-background-dark text-slate-900 dark:text-slate-100 font-display min-h-screen"
  >
    <div class="min-h-screen flex-col">
      <header class="sticky top-0 z-50 w-full bg-white dark:bg-background-dark border-b border-slate-200 dark:border-slate-800">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div class="flex justify-between items-center h-16">
            <div class="flex items-center gap-8">
              <div class="flex flex-col">
                <span class="text-primary dark:text-slate-100 text-lg font-bold leading-none">Bảng điều khiển giáo viên</span>
                <span class="text-slate-500 dark:text-slate-400 text-[10px] font-bold uppercase tracking-widest">
                  Hệ thống thi trực tuyến
                </span>
              </div>
              <nav class="hidden md:flex items-center gap-1">
                <RouterLink class="px-4 py-2 rounded-lg text-sm text-primary border border-primary/10" to="/teacher/dashboard">
                  Trang chủ
                </RouterLink>
                <RouterLink class="px-4 py-2 rounded-lg text-sm text-primary border border-primary/10" to="/teacher/exams">
                  Tạo đề thi
                </RouterLink>
                <RouterLink
                  class="px-4 py-2 rounded-lg text-sm text-primary border border-primary/10" to="/teacher/live-monitoring"
                >
                  Giám sát trực tiếp
                </RouterLink>
              </nav>
            </div>
            <div class="flex items-center gap-4">
              <button class="p-2 text-slate-400 hover:text-primary transition-colors" type="button">
                <span class="material-symbols-outlined">notifications</span>
              </button>
              <button
                type="button"
                @click="goToLogin"
                class="text-xs font-semibold px-3 py-1.5 rounded bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700"
              >
                Đăng xuất
              </button>
              <div class="flex items-center gap-3 pl-4 border-l border-slate-200 dark:border-slate-800">
                <div class="text-right hidden sm:block">
                  <p class="text-sm font-bold">GS. John Doe</p>
                  <p class="text-[10px] text-slate-500 font-medium uppercase">Khoa học máy tính</p>
                </div>
                <div class="h-9 w-9 rounded-full bg-primary text-white flex items-center justify-center font-bold text-sm">JD</div>
              </div>
            </div>
          </div>
        </div>
      </header>

      <main class="flex-1 relative p-6 md:p-10">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-20 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>
        <div class="max-w-6xl mx-auto relative">
          <header class="mb-8 animate-fade-up">
            <h2 class="text-3xl font-black tracking-tight text-slate-900 dark:text-slate-100">Chào mừng quay lại, giảng viên</h2>
            <p class="text-slate-500 dark:text-slate-400 mt-1">
              Tổng quan về hiệu suất kỳ thi và các phiên đang hoạt động.
            </p>
          </header>

          <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10 animate-fade-up-delay">
            <div class="bg-white dark:bg-background-dark border border-slate-200 dark:border-slate-800 p-6 rounded-xl shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
              <div class="flex justify-between items-start mb-4">
                <span class="material-symbols-outlined p-2 bg-primary/5 text-primary rounded-lg">inventory_2</span>
                <span class="text-emerald-600 text-xs font-bold px-2 py-1 bg-emerald-50 dark:bg-emerald-900/20 rounded-full">+2%</span>
              </div>
              <p class="text-slate-500 dark:text-slate-400 text-sm font-medium">Tổng số đề thi</p>
              <p class="text-3xl font-bold mt-1">42</p>
            </div>
            <div class="bg-white dark:bg-background-dark border border-slate-200 dark:border-slate-800 p-6 rounded-xl shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
              <div class="flex justify-between items-start mb-4">
                <span class="material-symbols-outlined p-2 bg-amber-500/5 text-amber-500 rounded-lg">timer</span>
                <span class="text-slate-500 text-xs font-bold px-2 py-1 bg-slate-50 dark:bg-slate-800 rounded-full">Tĩnh</span>
              </div>
              <p class="text-slate-500 dark:text-slate-400 text-sm font-medium">Đề thi đang hoạt động</p>
              <p class="text-3xl font-bold mt-1">5</p>
            </div>
            <div class="bg-white dark:bg-background-dark border border-slate-200 dark:border-slate-800 p-6 rounded-xl shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
              <div class="flex justify-between items-start mb-4">
                <span class="material-symbols-outlined p-2 bg-primary/5 text-primary rounded-lg">group</span>
                <span class="text-emerald-600 text-xs font-bold px-2 py-1 bg-emerald-50 dark:bg-emerald-900/20 rounded-full">+12%</span>
              </div>
              <p class="text-slate-500 dark:text-slate-400 text-sm font-medium">Sinh viên đã tham gia</p>
              <p class="text-3xl font-bold mt-1">1,280</p>
            </div>
          </div>

          <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-10">
            <div class="lg:col-span-2 bg-white dark:bg-background-dark border border-slate-200 dark:border-slate-800 p-6 rounded-xl shadow-sm">
              <div class="flex justify-between items-center mb-6">
                <div>
                  <h3 class="font-bold text-lg">Xu hướng tham gia của sinh viên</h3>
                  <p class="text-slate-500 text-sm">Hoạt động 7 ngày gần nhất</p>
                </div>
                <div class="text-right">
                  <p class="text-2xl font-black text-primary">85%</p>
                  <p class="text-xs text-emerald-600 font-bold">Điểm trung bình</p>
                </div>
              </div>
              <div class="grid grid-cols-7 gap-4 items-end h-48 px-2">
                <div v-for="day in weekBars" :key="day.label" class="flex flex-col items-center gap-2 h-full justify-end">
                  <div class="bg-primary/20 hover:bg-primary transition-colors rounded-t-sm w-full" :style="{ height: day.height }"></div>
                  <span class="text-[10px] font-bold text-slate-400 uppercase">{{ day.label }}</span>
                </div>
              </div>
            </div>

            <div class="bg-white dark:bg-background-dark border border-slate-200 dark:border-slate-800 p-6 rounded-xl shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
              <h3 class="font-bold text-lg mb-4">Thao tác nhanh</h3>
              <div class="flex flex-col gap-3">
                <button
                  v-for="action in quickActions"
                  :key="action.title"
                  class="flex items-center gap-3 w-full p-3 rounded-lg border border-slate-100 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800 text-left transition-all"
                  type="button"
                >
                  <span class="material-symbols-outlined text-primary">{{ action.icon }}</span>
                  <div>
                    <p class="text-sm font-semibold">{{ action.title }}</p>
                    <p class="text-xs text-slate-500">{{ action.subtitle }}</p>
                  </div>
                </button>
              </div>
            </div>
          </div>

          <div class="bg-white dark:bg-background-dark border border-slate-200 dark:border-slate-800 rounded-xl shadow-sm overflow-hidden">
            <div class="p-6 border-b border-slate-200 dark:border-slate-800 flex justify-between items-center">
              <h3 class="font-bold text-lg">Đề thi gần đây &amp; đã kết thúc</h3>
              <div class="flex gap-2">
                <button class="text-xs font-semibold px-3 py-1.5 rounded bg-slate-100 dark:bg-slate-800" type="button">Tất cả</button>
                <button class="text-xs font-semibold px-3 py-1.5 rounded text-slate-500 hover:bg-slate-50" type="button">Đang hoạt động</button>
                <button class="text-xs font-semibold px-3 py-1.5 rounded text-slate-500 hover:bg-slate-50" type="button">Đã kết thúc</button>
              </div>
            </div>
            <div class="overflow-x-auto">
              <table class="w-full text-left">
                <thead class="bg-slate-50 dark:bg-slate-900/50 text-slate-500 dark:text-slate-400 text-xs uppercase tracking-wider">
                  <tr>
                    <th class="px-6 py-4 font-bold">Tiêu đề đề thi</th>
                    <th class="px-6 py-4 font-bold">Ngày</th>
                    <th class="px-6 py-4 font-bold">Trạng thái</th>
                    <th class="px-6 py-4 font-bold">Số người tham gia</th>
                    <th class="px-6 py-4 font-bold text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-200 dark:divide-slate-800">
                  <tr v-for="exam in exams" :key="exam.title" class="hover:bg-slate-50 dark:hover:bg-slate-900/40 transition-colors">
                    <td class="px-6 py-4">
                      <p class="text-sm font-semibold">{{ exam.title }}</p>
                      <p class="text-xs text-slate-500">{{ exam.subtitle }}</p>
                    </td>
                    <td class="px-6 py-4 text-sm">{{ exam.date }}</td>
                    <td class="px-6 py-4">
                      <span :class="exam.statusClass" class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium">
                        {{ exam.status }}
                      </span>
                    </td>
                    <td class="px-6 py-4 text-sm">{{ exam.participants }}</td>
                    <td class="px-6 py-4 text-right">
                      <button
                        :disabled="exam.disabled"
                        :class="exam.disabled ? 'text-slate-400 cursor-not-allowed' : 'text-primary hover:text-primary/80'"
                        class="font-semibold text-sm inline-flex items-center gap-1"
                        type="button"
                        @click="openExamResult(exam)"
                      >
                        <span class="material-symbols-outlined text-[18px]">{{ exam.disabled ? 'lock' : 'visibility' }}</span>
                        {{ exam.disabled ? 'Đang xử lý' : 'Xem kết quả' }}
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="p-4 bg-slate-50 dark:bg-slate-900/20 border-t border-slate-200 dark:border-slate-800 flex justify-center">
              <button class="text-sm font-semibold text-primary hover:underline" type="button">Xem tất cả hồ sơ kỳ thi</button>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const isDark = ref(false)

const weekBars = [
  { label: 'T2', height: '60%' },
  { label: 'T3', height: '80%' },
  { label: 'T4', height: '90%' },
  { label: 'T5', height: '30%' },
  { label: 'T6', height: '40%' },
  { label: 'T7', height: '100%' },
  { label: 'CN', height: '95%' }
]

const quickActions = [
  { icon: 'add_circle', title: 'Tạo đề thi mới', subtitle: 'Lên lịch phiên thi mới' },
  { icon: 'analytics', title: 'Tạo báo cáo', subtitle: 'Tải dữ liệu hiệu suất' },
  { icon: 'mail', title: 'Liên hệ sinh viên', subtitle: 'Gửi thông báo hàng loạt' },
  { icon: 'archive', title: 'Lưu trữ đã hoàn tất', subtitle: 'Dọn dẹp bảng điều khiển' }
]

const exams = [
  {
    title: 'Nhập môn thuật toán',
    subtitle: 'Chương 4: Sắp xếp & Tìm kiếm',
    date: '24 Thg 10, 2023',
    status: 'Đã kết thúc',
    statusClass: 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-300',
    participants: '142 sinh viên',
    disabled: false,
    resultPath: '/teacher/exams/review/summary'
  },
  {
    title: 'Thi cuối kỳ Giải tích nâng cao',
    subtitle: 'Khoa Toán',
    date: '22 Thg 10, 2023',
    status: 'Đã kết thúc',
    statusClass: 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-300',
    participants: '85 sinh viên',
    disabled: false,
    resultPath: '/teacher/exams/review/summary'
  },
  {
    title: 'Bài kiểm tra Cấu trúc dữ liệu',
    subtitle: 'Đánh giá giữa kỳ',
    date: '20 Thg 10, 2023',
    status: 'Đang diễn ra',
    statusClass: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-400',
    participants: '210 sinh viên',
    disabled: true
  },
  {
    title: 'Phát triển Web 101',
    subtitle: 'Nền tảng HTML & CSS',
    date: '18 Thg 10, 2023',
    status: 'Đã kết thúc',
    statusClass: 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-300',
    participants: '312 sinh viên',
    disabled: false,
    resultPath: '/teacher/exams/review/summary'
  }
]

const goToLogin = () => {
  router.push('/login')
}

const openExamResult = (exam) => {
  if (exam.disabled || !exam.resultPath) {
    return
  }

  router.push({
    path: exam.resultPath,
    query: { title: exam.title }
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