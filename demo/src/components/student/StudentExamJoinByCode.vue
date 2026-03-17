<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen"
  >
    <div class="layout-container flex h-full grow flex-col">
      <StudentTopHeader />

      <main class="teacher-page-shell max-w-3xl">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

        <section class="relative flex flex-col gap-2 animate-fade-up mb-8">
          <h1 class="text-3xl md:text-4xl font-black tracking-tight text-primary dark:text-slate-100">Thi qua mã</h1>
          <p class="text-slate-600 dark:text-slate-400 text-lg">Nhập mã hoặc tiêu đề để vào đúng phòng thi của bạn.</p>
        </section>

        <div class="teacher-card p-6 animate-fade-up-delay">
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
                placeholder="Ví dụ: ABCD 1234"
                type="text"
              />
            </div>
            <button
              :disabled="isJoining"
              @click="goToWaitingRoom"
              class="w-full md:w-auto px-8 py-3 bg-primary text-white font-bold rounded-lg hover:bg-primary/90 hover:-translate-y-0.5 transition-all duration-200 disabled:opacity-70 disabled:cursor-not-allowed disabled:hover:translate-y-0"
              type="button"
            >
              {{ isJoining ? 'Đang vào...' : 'Vào phòng' }}
            </button>
          </div>

          <p class="mt-3 text-xs text-slate-500">Vui lòng đảm bảo kết nối internet ổn định trước khi vào phòng thi.</p>
        </div>
      </main>

      <footer class="bg-white dark:bg-background-dark border-t border-primary/10 py-6 px-10 text-center text-slate-500 text-sm">
        <p>© 2026 Hệ thống thi trực tuyến ExamPortal. Bảo lưu mọi quyền.</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { joinExamByCode } from '../../services/examService'
import { useToast } from '../../composables/useToast'
import StudentTopHeader from './StudentTopHeader.vue'

const router = useRouter()
const isDark = ref(false)
const examCode = ref('')
const isJoining = ref(false)
const toast = useToast()

const resolveExamByInput = async () => {
  const query = examCode.value.trim()
  if (!query) {
    return null
  }

  return joinExamByCode(query)
}

const goToWaitingRoom = async () => {
  isJoining.value = true

  try {
    const matchedExam = await resolveExamByInput()
    if (!matchedExam) {
      toast.error('Vui lòng nhập mã hoặc tiêu đề để tìm bài thi.')
      return
    }

    router.push({
      path: '/student/exam-waiting-room',
      query: {
        examId: matchedExam.id,
        examCode: matchedExam.code || '',
        exam: matchedExam.title || 'Bài thi',
        duration: matchedExam.durationMinutes || 60,
        questions: matchedExam.questionCount || 0,
        startAt: matchedExam.startTime || '',
        endAt: matchedExam.endTime || '',
        requireCameraMic: matchedExam.requireCameraMic === false ? 'false' : 'true'
      }
    })
  } catch (error) {
    toast.error('Không thể vào bài thi lúc này.')
  } finally {
    isJoining.value = false
  }
}
</script>
