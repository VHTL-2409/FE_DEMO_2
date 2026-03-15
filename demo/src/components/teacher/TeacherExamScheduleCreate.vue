<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <TeacherTopHeader active-section="exam" />

    <main class="relative max-w-4xl mx-auto px-4 py-10 overflow-hidden">
      <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
      <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

      <div class="relative mb-6 animate-fade-up">
        <h1 class="text-3xl font-bold tracking-tight">Thiết lập &amp; tạo đề thi</h1>
        <p class="text-slate-500 mt-1">{{ selectedExamTitle }}</p>
      </div>

      <div class="mb-8 animate-fade-up">
        <div class="flex flex-wrap items-center gap-3 text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
          <template v-for="(step, index) in steps" :key="step">
            <div class="flex items-center gap-3">
              <span
                class="size-7 rounded-full flex items-center justify-center text-[11px] font-bold"
                :class="index + 1 <= currentStep ? 'bg-primary text-white' : 'bg-slate-200 text-slate-500 dark:bg-slate-800 dark:text-slate-400'"
              >
                {{ index + 1 }}
              </span>
              <span :class="index + 1 === currentStep ? 'text-slate-900 dark:text-white' : ''">{{ step }}</span>
            </div>
            <span v-if="index < steps.length - 1" class="h-px w-6 bg-slate-200 dark:bg-slate-700"></span>
          </template>
        </div>
      </div>

      <section class="relative bg-white dark:bg-slate-900 p-8 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm animate-fade-up-delay hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
        <div class="flex items-center gap-2 mb-6">
          <span class="material-symbols-outlined text-primary">settings</span>
          <h3 class="text-lg font-bold">Cấu hình thời gian</h3>
        </div>

        <div class="space-y-4">
          <div class="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50/80 dark:bg-slate-800/70 px-4 py-3">
            <div class="flex items-center gap-2 text-sm font-semibold text-slate-700 dark:text-slate-200">
              <span class="material-symbols-outlined text-base">timer</span>
              Thời lượng (phút)
            </div>
            <span class="text-primary font-bold text-sm">{{ timeLimit }} phút</span>
          </div>

          <div class="rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50/70 dark:bg-slate-800/60 p-4">
            <div class="flex items-center gap-2 text-sm font-semibold text-slate-700 dark:text-slate-200">
              <span class="material-symbols-outlined text-base">schedule</span>
              Bắt đầu
            </div>
            <div class="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-3">
              <input v-model="startDate" class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-white/90 dark:bg-slate-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all" type="date" @input="closePicker" @change="closePicker" />
              <input v-model="startClock" class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-white/90 dark:bg-slate-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all" type="time" step="60" @input="closePicker" @change="closePicker" />
            </div>
            <div class="mt-3 flex flex-wrap gap-2">
              <button class="px-3 py-1.5 text-xs font-semibold rounded bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-700" type="button" @click="setStartNow">Bây giờ</button>
              <button class="px-3 py-1.5 text-xs font-semibold rounded bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-700" type="button" @click="setStartIn15Minutes">+15 phút</button>
              <button class="px-3 py-1.5 text-xs font-semibold rounded bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-700" type="button" @click="setStartIn30Minutes">+30 phút</button>
            </div>
          </div>

          <div class="rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50/70 dark:bg-slate-800/60 p-4">
            <div class="flex items-center gap-2 text-sm font-semibold text-slate-700 dark:text-slate-200">
              <span class="material-symbols-outlined text-base">event_available</span>
              Kết thúc
            </div>
            <div class="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-3">
              <input v-model="endDate" class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-white/90 dark:bg-slate-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all" type="date" @input="closePicker" @change="closePicker" />
              <input v-model="endClock" class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-white/90 dark:bg-slate-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all" type="time" step="60" @input="closePicker" @change="closePicker" />
            </div>
            <div class="mt-3 flex flex-wrap gap-2">
              <button class="px-3 py-1.5 text-xs font-semibold rounded bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-700" type="button" @click="setEndByDuration">Kết thúc = Bắt đầu + Thời lượng</button>
              <button class="px-3 py-1.5 text-xs font-semibold rounded bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-700" type="button" @click="setEndAfter30Minutes">+30 phút</button>
              <button class="px-3 py-1.5 text-xs font-semibold rounded bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-700" type="button" @click="setEndAfter60Minutes">+60 phút</button>
            </div>
          </div>
        </div>

        <div class="mt-6 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800/70 px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
          <span class="font-semibold text-slate-700 dark:text-slate-200">Xem trước:</span>
          <span v-if="previewLabel"> {{ previewLabel }}</span>
          <span v-else> Hãy chọn thời gian bắt đầu và kết thúc.</span>
        </div>

        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4 pt-8">
          <button class="px-8 py-3 rounded-lg border border-slate-200 dark:border-slate-800 font-semibold hover:bg-slate-100 dark:hover:bg-slate-800 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200" type="button" @click="goBack">
            Quay lại
          </button>
          <div class="flex flex-col sm:flex-row gap-3">
            <button class="px-6 py-3 rounded-lg border border-slate-200 dark:border-slate-700 font-semibold text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-all" type="button" @click="setEndByDuration">
              Tự tính kết thúc
            </button>
            <button :disabled="isSubmitting" class="px-10 py-3 rounded-lg bg-primary text-white font-bold shadow-lg shadow-primary/30 hover:bg-primary/90 hover:-translate-y-0.5 transition-all duration-200 flex items-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed disabled:hover:translate-y-0" type="button" @click="handleCreateAssignment">
              {{ isSubmitting ? 'Đang xuất bản...' : 'Xuất bản đề thi' }}
              <span class="material-symbols-outlined text-lg">rocket_launch</span>
            </button>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { createExamAssignment } from '../../services/assignmentService'
import { updateExam } from '../../services/examService'
import { useToast } from '../../composables/useToast'
import { useRoute, useRouter } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const router = useRouter()
const route = useRoute()
const isDark = ref(false)
const steps = ['Chọn cách tạo', 'Nhập đề', 'Lập lịch', 'Hoàn tất']
const currentStep = 3
const timeLimit = ref(Number.parseInt(String(route.query.durationMinutes || ''), 10) || 60)
const isSubmitting = ref(false)

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
const previewLabel = computed(() => {
  const start = toDate(startAt.value)
  const end = toDate(endAt.value)
  if (!start || !end) return ''
  if (end <= start) return 'Thời gian kết thúc đang sớm hơn thời gian bắt đầu.'
  return `${start.toLocaleString()} → ${end.toLocaleString()}`
})

const toDate = (value) => {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

const closePicker = (event) => {
  const target = event?.target
  if (!target) return
  window.setTimeout(() => target.blur(), 0)
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

const setStartIn30Minutes = () => {
  const date = new Date(Date.now() + 30 * 60000)
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

const setEndAfter60Minutes = () => {
  const date = new Date(Date.now() + 60 * 60000)
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
  if (!examId.value) {
    toast.error('Thiếu mã đề thi. Vui lòng tạo lại đề thi.')
    return
  }

  if (!startAt.value || !endAt.value) {
    toast.error('Vui lòng chọn cả ngày/giờ bắt đầu và kết thúc.')
    return
  }

  const start = toDate(startAt.value)
  const end = toDate(endAt.value)
  if (!start || !end || end <= start) {
    toast.error('Thời gian kết thúc phải sau thời gian bắt đầu.')
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
    toast.error('Không thể xuất bản đề thi. Vui lòng thử lại.')
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