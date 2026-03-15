<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen transition-colors duration-200">
    <div class="layout-container flex h-full grow flex-col">
      <StudentTopHeader />

      <main class="relative max-w-4xl mx-auto px-4 py-10 overflow-hidden w-full">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

        <div class="relative mb-10 animate-fade-up">
          <h2 class="text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight">Tạo bài luyện tập mới</h2>
          <p class="mt-2 text-slate-600 dark:text-slate-400 text-lg">Thiết kế tương tự luồng tạo đề bên giáo viên, tối ưu cho sinh viên tự luyện tập nhanh.</p>
        </div>

        <div class="relative space-y-8 animate-fade-up-delay">
          <section class="bg-white dark:bg-slate-900 p-8 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
            <div class="flex items-center gap-2 mb-6">
              <span class="material-symbols-outlined text-primary">info</span>
              <h3 class="text-lg font-bold">Thông tin chung</h3>
            </div>
            <div class="space-y-2">
              <input
                class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 outline-none"
                type="file"
                @change="handleFileChange"
              />
              <p v-if="fileName" class="text-xs text-slate-500 dark:text-slate-400">Đã chọn: {{ fileName }}</p>
            </div>
          </section>

          <section class="bg-white dark:bg-slate-900 p-8 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm">
            <div class="flex items-center justify-between mb-6">
              <div class="flex items-center gap-2">
                <span class="material-symbols-outlined text-primary">auto_awesome</span>
                <h3 class="text-lg font-bold">Thiết lập phiên luyện tập</h3>
              </div>
            </div>

            <div class="space-y-2">
              <input
                v-model.number="questionCount"
                type="number"
                min="5"
                max="50"
                class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
              />
            </div>
            <div class="grid grid-cols-1 gap-4 mt-4">
              <div class="rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50/70 dark:bg-slate-800/60 p-4">
                <div class="flex items-center gap-2 text-sm font-semibold text-slate-700 dark:text-slate-200">
                  <span class="material-symbols-outlined text-base">schedule</span>
                  Bắt đầu
                </div>
                <div class="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <input v-model="startDate" class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-white/90 dark:bg-slate-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all" type="date" @input="closePicker" @change="closePicker" />
                  <input v-model="startClock" class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-white/90 dark:bg-slate-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all" type="time" step="60" @input="closePicker" @change="closePicker" />
                </div>
              </div>

              <div class="rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50/70 dark:bg-slate-800/60 p-4">
                <div class="flex items-center gap-2 text-sm font-semibold text-slate-700 dark:text-slate-200">
                  <span class="material-symbols-outlined text-base">event_available</span>
                  Kết thúc
                </div>
               
                <div class="mt-3 flex flex-wrap gap-2">
                  <button class="px-3 py-1.5 text-xs font-semibold rounded bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-700" type="button" @click="setEndByDuration">Kết thúc = Bắt đầu + Thời lượng</button>
                  <button class="px-3 py-1.5 text-xs font-semibold rounded bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-700" type="button" @click="setEndAfter30Minutes">+30 phút</button>
                </div>
              </div>
            </div>
          </section>

          <div class="flex items-center justify-end gap-4 pt-2">
            <button
              class="px-8 py-3 rounded-lg border border-slate-200 dark:border-slate-800 font-semibold hover:bg-slate-100 dark:hover:bg-slate-800 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200"
              type="button"
              @click="goBack"
            >
              Quay lại
            </button>
            <button
              :disabled="isCreating"
              class="px-10 py-3 rounded-lg bg-primary text-white font-bold shadow-lg shadow-primary/30 hover:bg-primary/90 hover:-translate-y-0.5 transition-all duration-200 flex items-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed disabled:hover:translate-y-0"
              type="button"
              @click="startPractice"
            >
              {{ isCreating ? 'Đang tạo...' : 'Tạo & bắt đầu' }}
              <span class="material-symbols-outlined text-lg">arrow_forward</span>
            </button>
          </div>
        </div>
      </main>

      <footer class="py-8 text-center text-slate-500 text-sm border-t border-primary/10">
        <p>© 2026 Hệ thống thi trực tuyến ExamPortal. Bảo lưu mọi quyền.</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { startAttempt } from '../../services/attemptService'
import { createPracticeExam } from '../../services/examService'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from '../../composables/useToast'
import StudentTopHeader from './StudentTopHeader.vue'

const router = useRouter()
const route = useRoute()
const isDark = ref(false)
const questionCount = ref(20)
const timeLimit = ref(Number.parseInt(String(route.query.durationMinutes || ''), 10) || 60)
const isCreating = ref(false)
const fileName = ref('')
const toast = useToast()

const formatDatePart = (date) => {
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

const formatTimePart = (date) => {
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${hh}:${mm}`
}

const now = new Date()
const defaultEnd = new Date(now.getTime() + timeLimit.value * 60000)

const startDate = ref(formatDatePart(now))
const startClock = ref(formatTimePart(now))
const endDate = ref(formatDatePart(defaultEnd))
const endClock = ref(formatTimePart(defaultEnd))

const buildLocalDateTime = (datePart, timePart) => {
  if (!datePart || !timePart) return ''
  return `${datePart}T${timePart}:00`
}

const startAt = computed(() => buildLocalDateTime(startDate.value, startClock.value))
const endAtValue = computed(() => buildLocalDateTime(endDate.value, endClock.value))

const toDate = (value) => {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

const handleFileChange = (event) => {
  const file = event?.target?.files?.[0]
  fileName.value = file ? file.name : ''
}

const setStartNow = () => {
  const date = new Date()
  startDate.value = formatDatePart(date)
  startClock.value = formatTimePart(date)
}

const setStartIn15Minutes = () => {
  const date = new Date(Date.now() + 15 * 60000)
  startDate.value = formatDatePart(date)
  startClock.value = formatTimePart(date)
}

const setEndByDuration = () => {
  const start = toDate(startAt.value)
  if (!start) return
  const date = new Date(start.getTime() + Number(timeLimit.value || 60) * 60000)
  endDate.value = formatDatePart(date)
  endClock.value = formatTimePart(date)
}

const setEndAfter30Minutes = () => {
  const date = new Date(Date.now() + 30 * 60000)
  endDate.value = formatDatePart(date)
  endClock.value = formatTimePart(date)
}

const goBack = () => {
  router.push('/student/dashboard')
}

const closePicker = (event) => {
  const target = event?.target
  if (!target) return
  window.setTimeout(() => target.blur(), 0)
}

const startPractice = async () => {
  isCreating.value = true

  const start = toDate(startAt.value)
  const end = toDate(endAtValue.value)
  if (!start || !end || end <= start) {
    toast.error('Thời gian kết thúc phải sau thời gian bắt đầu.')
    isCreating.value = false
    return
  }

  try {
    const practiceExam = await createPracticeExam({
      questionCount: questionCount.value,
      durationMinutes: Number(timeLimit.value)
    })

    const attempt = await startAttempt(practiceExam.id)

    router.push({
      path: '/student/exam-interface',
      query: {
        exam: practiceExam.title || `Bài luyện tập - ${fileName.value}`,
        examId: practiceExam.id,
        attemptId: attempt.attemptId,
        deadlineAt: attempt.deadlineAt || '',
        remainingSeconds: attempt.remainingSeconds || 0,
        startedAt: attempt.startedAt || '',
        isPractice: 'true'
      }
    })
  } catch (error) {
    toast.error('Không thể tạo bài luyện tập lúc này.')
  } finally {
    isCreating.value = false
  }
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
