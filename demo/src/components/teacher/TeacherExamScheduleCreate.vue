<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <TeacherTopHeader active-section="exam" />

    <main class="relative max-w-4xl mx-auto px-4 py-10 overflow-hidden">
      <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
      <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

      <div class="relative mb-8 animate-fade-up">
        <h1 class="text-3xl font-bold tracking-tight">Thiết lập &amp; tạo đề thi</h1>
        <p class="text-slate-500 mt-1">{{ selectedExamTitle }}</p>
      </div>

      <section class="relative bg-white dark:bg-slate-900 p-8 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm animate-fade-up-delay hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
        <div class="flex items-center gap-2 mb-6">
          <span class="material-symbols-outlined text-primary">settings</span>
          <h3 class="text-lg font-bold">Cấu hình thời gian</h3>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div class="space-y-4">
            <div class="flex items-center justify-between">
              <label class="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-2">
                <span class="material-symbols-outlined text-sm">timer</span>
                Thời lượng (phút)
              </label>
              <span class="text-primary font-bold">{{ timeLimit }} phút</span>
            </div>
            <div class="px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-sm text-slate-600 dark:text-slate-300">
              Thời lượng lấy theo đề thi đã chọn.
            </div>
          </div>
          <div class="space-y-3">
            <label class="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-2">
              <span class="material-symbols-outlined text-sm">calendar_today</span>
              Ngày &amp; giờ bắt đầu
            </label>
            <div class="grid grid-cols-2 gap-3">
              <input v-model="startDate" class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all" type="date" />
              <input v-model="startClock" class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all" type="time" step="60" />
            </div>
            <div class="flex flex-wrap gap-2">
              <button class="px-3 py-1.5 text-xs font-semibold rounded bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700" type="button" @click="setStartNow">Bây giờ</button>
              <button class="px-3 py-1.5 text-xs font-semibold rounded bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700" type="button" @click="setStartIn15Minutes">+15 phút</button>
            </div>
          </div>
          <div class="space-y-3">
            <label class="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-2">
              <span class="material-symbols-outlined text-sm">event_busy</span>
              Ngày &amp; giờ kết thúc
            </label>
            <div class="grid grid-cols-2 gap-3">
              <input v-model="endDate" class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all" type="date" />
              <input v-model="endClock" class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all" type="time" step="60" />
            </div>
            <div class="flex flex-wrap gap-2">
              <button class="px-3 py-1.5 text-xs font-semibold rounded bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700" type="button" @click="setEndByDuration">Kết thúc = Bắt đầu + Thời lượng</button>
              <button class="px-3 py-1.5 text-xs font-semibold rounded bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700" type="button" @click="setEndAfter30Minutes">+30 phút</button>
            </div>
          </div>
        </div>

        <div class="flex items-center justify-end gap-4 pt-8">
          <button class="px-8 py-3 rounded-lg border border-slate-200 dark:border-slate-800 font-semibold hover:bg-slate-100 dark:hover:bg-slate-800 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200" type="button" @click="goBack">
            Quay lại
          </button>
          <button :disabled="isSubmitting" class="px-10 py-3 rounded-lg bg-primary text-white font-bold shadow-lg shadow-primary/30 hover:bg-primary/90 hover:-translate-y-0.5 transition-all duration-200 flex items-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed disabled:hover:translate-y-0" type="button" @click="handleCreateAssignment">
            {{ isSubmitting ? 'Đang xuất bản...' : 'Tạo đề thi' }}
            <span class="material-symbols-outlined text-lg">rocket_launch</span>
          </button>
        </div>
        <p v-if="errorMessage" class="text-sm text-rose-600 mt-3">{{ errorMessage }}</p>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ApiError } from '../../services/apiClient'
import { createExamAssignment } from '../../services/assignmentService'
import { updateExam } from '../../services/examService'
import { useRoute, useRouter } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const router = useRouter()
const route = useRoute()
const isDark = ref(false)
const timeLimit = ref(Number.parseInt(String(route.query.durationMinutes || ''), 10) || 60)
const isSubmitting = ref(false)
const errorMessage = ref('')

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
const defaultEnd = new Date(now.getTime() + 60 * 60000)

const startDate = ref(formatDatePart(now))
const startClock = ref(formatTimePart(now))
const endDate = ref(formatDatePart(defaultEnd))
const endClock = ref(formatTimePart(defaultEnd))

const examId = computed(() => Number.parseInt(String(route.query.examId || ''), 10) || null)
const selectedExamTitle = computed(() => route.query.title || 'Đề thi mới')

const buildLocalDateTime = (datePart, timePart) => {
  if (!datePart || !timePart) return ''
  return `${datePart}T${timePart}:00`
}

const startAt = computed(() => buildLocalDateTime(startDate.value, startClock.value))
const endAt = computed(() => buildLocalDateTime(endDate.value, endClock.value))

const toDate = (value) => {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
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
  const source = route.query.source
  if (source === 'manual') {
    router.push('/teacher/exams/manual')
    return
  }
  router.push('/teacher/exams/create')
}

const handleCreateAssignment = async () => {
  errorMessage.value = ''

  if (!examId.value) {
    errorMessage.value = 'Thiếu mã đề thi. Vui lòng tạo lại đề thi.'
    return
  }

  if (!startAt.value || !endAt.value) {
    errorMessage.value = 'Vui lòng chọn cả ngày/giờ bắt đầu và kết thúc.'
    return
  }

  const start = toDate(startAt.value)
  const end = toDate(endAt.value)
  if (!start || !end || end <= start) {
    errorMessage.value = 'Thời gian kết thúc phải sau thời gian bắt đầu.'
    return
  }

  isSubmitting.value = true
  try {
    const updatedExam = await updateExam(examId.value, {
      title: selectedExamTitle.value,
      description: '',
      durationMinutes: timeLimit.value,
      startTime: startAt.value,
      endTime: endAt.value,
      isActive: true
    })

    await createExamAssignment(examId.value, {
      title: selectedExamTitle.value,
      openAt: startAt.value,
      closeAt: endAt.value,
      maxAttempts: 1,
      allowReviewAfterSubmit: true,
      isPublished: true
    })

    router.push({
      path: '/teacher/exams/created-success',
      query: {
        examId: examId.value,
        code: updatedExam?.code || '',
        title: selectedExamTitle.value,
        durationMinutes: timeLimit.value,
        startAt: startAt.value,
        endAt: endAt.value
      }
    })
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : 'Không thể xuất bản đề thi. Vui lòng thử lại.'
  } finally {
    isSubmitting.value = false
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