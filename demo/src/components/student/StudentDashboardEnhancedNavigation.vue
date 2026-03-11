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
              Đăng xuất
            </button>
            <div class="flex items-center gap-3 pl-2 border-l border-primary/10">
              <div class="hidden md:block text-right">
                <p class="text-sm font-bold">Sinh viên</p>
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
            <h1 class="text-3xl md:text-4xl font-black tracking-tight text-primary dark:text-slate-100">Chào mừng bạn quay lại</h1>
            <p class="text-slate-600 dark:text-slate-400 text-lg">Bảng điều khiển cho kỳ thi và tự học của bạn.</p>
          </section>

          <div class="relative grid grid-cols-1 lg:grid-cols-3 gap-8 animate-fade-up-delay">
            <div class="lg:col-span-2 space-y-8">
              <div class="bg-white dark:bg-slate-900 p-6 rounded-xl border border-primary/10 shadow-sm">
                <div class="flex items-center gap-3 mb-6">
                  <span class="material-symbols-outlined text-primary text-3xl">login</span>
                  <h2 class="text-xl font-bold">Vào phòng thi</h2>
                </div>
                <div class="flex flex-col md:flex-row items-end gap-4">
                  <div class="flex flex-col flex-1 w-full">
                    <label class="text-sm font-semibold mb-2 text-slate-700 dark:text-slate-300">Mã / Code / Tiêu đề bài thi</label>
                    <input
                      v-model="examCode"
                      class="w-full rounded-lg border border-primary/20 bg-background-light dark:bg-background-dark px-4 py-3 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                      placeholder="Ví dụ: 12 hoặc Toán cao cấp"
                      type="text"
                    />
                  </div>
                  <button :disabled="isJoining" @click="goToWaitingRoom" class="w-full md:w-auto px-8 py-3 bg-primary text-white font-bold rounded-lg hover:bg-primary/90 hover:-translate-y-0.5 transition-all duration-200 disabled:opacity-70 disabled:cursor-not-allowed disabled:hover:translate-y-0" type="button">
                    {{ isJoining ? 'Đang vào...' : 'Vào phòng' }}
                  </button>
                </div>
                <p class="mt-3 text-xs text-slate-500">Vui lòng đảm bảo kết nối internet ổn định trước khi vào phòng thi.</p>
                <p v-if="joinError" class="mt-2 text-xs text-rose-600">{{ joinError }}</p>
              </div>

              <div class="bg-white dark:bg-slate-900 p-6 rounded-xl border border-primary/10 shadow-sm">
                <div class="flex items-center justify-between mb-6">
                  <div class="flex items-center gap-3">
                    <span class="material-symbols-outlined text-primary text-3xl">model_training</span>
                    <h2 class="text-xl font-bold">Tự học &amp; Luyện tập</h2>
                  </div>
                  <button class="flex items-center gap-2 text-sm font-bold text-primary hover:underline" type="button">
                    <span class="material-symbols-outlined text-sm">download</span>
                    Mẫu
                  </button>
                </div>
                <label
                  for="file-upload"
                  class="border-2 border-dashed border-primary/20 rounded-xl p-8 flex flex-col items-center justify-center bg-primary/5 hover:bg-primary/10 transition-colors cursor-pointer group"
                >
                  <span class="material-symbols-outlined text-4xl text-primary/60 group-hover:text-primary transition-colors mb-4">upload_file</span>
                  <p class="font-bold text-slate-800 dark:text-slate-200">Tải lên CSV hoặc XLSX để tạo bài luyện tập</p>
                  <p class="text-sm text-slate-500 mt-1">Kích thước tệp tối đa: 5MB</p>
                  <input id="file-upload" class="hidden" type="file" @change="goToGeneratePractice" />
                </label>
                <div class="mt-6 grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div class="p-4 bg-background-light dark:bg-background-dark rounded-lg flex items-center gap-3 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                    <span class="material-symbols-outlined text-primary">auto_awesome</span>
                    <div class="text-sm">
                      <p class="font-bold">Tạo bằng AI</p>
                      <p class="text-slate-500">Câu hỏi được tạo từ ghi chú của bạn</p>
                    </div>
                  </div>
                  <div class="p-4 bg-background-light dark:bg-background-dark rounded-lg flex items-center gap-3 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
                    <span class="material-symbols-outlined text-primary">timer</span>
                    <div class="text-sm">
                      <p class="font-bold">Phiên có giới hạn thời gian</p>
                      <p class="text-slate-500">Mô phỏng điều kiện thi thực tế</p>
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
                    <h2 class="text-xl font-bold">Lịch sử bài thi</h2>
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
                    Xem toàn bộ kết quả
                  </button>
                </div>
              </div>
            </div>
          </div>
        </main>

        <footer class="bg-white dark:bg-background-dark border-t border-primary/10 py-6 px-10 text-center text-slate-500 text-sm">
          <p>© 2024 Hệ thống thi trực tuyến ExamPortal. Bảo lưu mọi quyền.</p>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ApiError } from '../../services/apiClient'
import { listMyAttempts } from '../../services/attemptService'
import { listExams } from '../../services/examService'
import { useRouter } from 'vue-router'

const router = useRouter()
const isDark = ref(false)
const examCode = ref('')
const isJoining = ref(false)
const joinError = ref('')
const attempts = ref([])

const historyItems = computed(() => attempts.value
  .slice()
  .sort((a, b) => {
    const aTime = new Date(a.submittedAt || a.startedAt || 0).getTime()
    const bTime = new Date(b.submittedAt || b.startedAt || 0).getTime()
    return (Number.isNaN(bTime) ? 0 : bTime) - (Number.isNaN(aTime) ? 0 : aTime)
  })
  .map((attempt) => ({
    title: attempt.examTitle || `Bài thi #${attempt.examId}`,
    score: `${Math.round(Number(attempt.score || 0))}%`,
    date: attempt.submittedAt ? new Date(attempt.submittedAt).toLocaleDateString() : '-',
    grade: attempt.status || 'SUBMITTED',
    attemptId: attempt.id,
    examId: attempt.examId,
    accuracy: Math.round(Number(attempt.score || 0)),
    time: attempt.startedAt && attempt.submittedAt
      ? `${Math.max(1, Math.round((new Date(attempt.submittedAt).getTime() - new Date(attempt.startedAt).getTime()) / 60000))}m`
      : '-'
  })))

const resolveExamByInput = async () => {
  const query = examCode.value.trim().toLowerCase()
  const exams = await listExams()

  if (!query) {
    return exams.find((exam) => exam.isActive) || exams[0] || null
  }

  const byId = exams.find((exam) => String(exam.id) === query)
  if (byId) return byId

  const byCode = exams.find((exam) => String(exam.code || '').toLowerCase() === query)
  if (byCode) return byCode

  return exams.find((exam) => String(exam.title || '').toLowerCase().includes(query)) || null
}

const goToWaitingRoom = async () => {
  isJoining.value = true
  joinError.value = ''
  try {
    const matchedExam = await resolveExamByInput()
    if (!matchedExam) {
      joinError.value = 'Không tìm thấy bài thi. Vui lòng kiểm tra mã/tiêu đề và thử lại.'
      return
    }

    if (!matchedExam.isActive) {
      joinError.value = 'Bài thi này chưa được mở.'
      return
    }

    router.push({
      path: '/student/exam-waiting-room',
      query: {
        examId: matchedExam.id,
        examCode: matchedExam.code || '',
        exam: matchedExam.title || `Bài thi #${matchedExam.id}`,
        duration: matchedExam.durationMinutes || 60,
        questions: matchedExam.questionCount || 0,
        startAt: matchedExam.startTime || '',
        endAt: matchedExam.endTime || ''
      }
    })
  } catch (error) {
    joinError.value = error instanceof ApiError ? error.message : 'Không thể vào bài thi lúc này.'
  } finally {
    isJoining.value = false
  }
}

const goToGeneratePractice = (event) => {
  const fileName = event.target.files?.[0]?.name || 'Ghi_chu_Vat_ly.pdf'
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
      attemptId: item.attemptId,
      examId: item.examId,
      score: Number.parseInt(item.score, 10),
      attempted: `Đã làm lúc ${item.date}`,
      time: item.time,
      accuracy: item.accuracy
    }
  })
}

const goToLogin = () => {
  router.push('/login')
}

onMounted(async () => {
  try {
    attempts.value = await listMyAttempts()
  } catch {
    attempts.value = []
  }
})
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