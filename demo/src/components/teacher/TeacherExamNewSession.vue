<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <TeacherTopHeader active-section="exam-list" />

    <main class="relative max-w-4xl mx-auto px-4 py-10 overflow-hidden">
      <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
      <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

      <div class="relative mb-6 animate-fade-up">
        <h1 class="text-3xl font-bold tracking-tight">Tạo đợt thi mới</h1>
        <p class="text-slate-500 mt-1">{{ examTitle }}</p>
        <p class="text-sm text-slate-500 dark:text-slate-400 mt-2">Mở lại đề thi với thời gian thi mới. Mã đề thi giữ nguyên.</p>
      </div>

      <section class="relative bg-white dark:bg-slate-900 p-8 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm animate-fade-up-delay">
        <div class="flex items-center gap-2 mb-6">
          <span class="material-symbols-outlined text-primary">schedule</span>
          <h3 class="text-lg font-bold">Thời gian đợt thi mới</h3>
        </div>

        <div class="space-y-4">
          <div class="rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50/80 dark:bg-slate-800/70 p-4">
            <div class="flex items-center gap-2 text-sm font-semibold text-slate-700 dark:text-slate-200 mb-3">
              <span class="material-symbols-outlined text-base">timer</span>
              Thời lượng làm bài (phút)
            </div>
            <div class="flex flex-wrap items-center gap-3">
              <input
                v-model.number="timeLimit"
                type="number"
                min="5"
                max="480"
                class="w-24 px-4 py-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none text-center font-bold"
                @blur="clampTimeLimit"
              />
              <span class="text-slate-600 dark:text-slate-400">phút</span>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="opt in durationPresets"
                  :key="opt"
                  type="button"
                  :class="timeLimit === opt ? 'bg-primary text-white border-primary' : 'bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-700'"
                  class="px-3 py-1.5 text-xs font-semibold rounded border hover:bg-primary/10 dark:hover:bg-primary/10"
                  @click="timeLimit = opt"
                >
                  {{ opt }}p
                </button>
              </div>
            </div>
          </div>

          <div class="rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50/70 dark:bg-slate-800/60 p-4">
            <div class="flex items-center gap-2 text-sm font-semibold text-slate-700 dark:text-slate-200">
              <span class="material-symbols-outlined text-base">schedule</span>
              Bắt đầu
            </div>
            <div class="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-3">
              <input
                v-model="startDate"
                :min="minDateStr"
                class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-white/90 dark:bg-slate-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                type="date"
              />
              <input
                v-model="startClock"
                class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-white/90 dark:bg-slate-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                type="time"
                step="300"
              />
            </div>
            <div class="mt-3 flex flex-wrap gap-2">
              <button class="px-3 py-1.5 text-xs font-semibold rounded bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-700" type="button" @click="setStartNow">Bây giờ</button>
              <button class="px-3 py-1.5 text-xs font-semibold rounded bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-700" type="button" @click="setStartIn15Minutes">+15 phút</button>
              <button class="px-3 py-1.5 text-xs font-semibold rounded bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-700" type="button" @click="setStartIn30Minutes">+30 phút</button>
              <button class="px-3 py-1.5 text-xs font-semibold rounded bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-700" type="button" @click="setStartIn1Hour">+1 giờ</button>
            </div>
          </div>

          <div class="rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50/70 dark:bg-slate-800/60 p-4">
            <div class="flex items-center gap-2 text-sm font-semibold text-slate-700 dark:text-slate-200">
              <span class="material-symbols-outlined text-base">event_available</span>
              Kết thúc
            </div>
            <div class="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-3">
              <input
                v-model="endDate"
                :min="startDate"
                class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-white/90 dark:bg-slate-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                type="date"
              />
              <input
                v-model="endClock"
                class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-white/90 dark:bg-slate-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                type="time"
                step="300"
              />
            </div>
            <div class="mt-3 flex flex-wrap gap-2">
              <button class="px-3 py-1.5 text-xs font-semibold rounded bg-primary/10 text-primary border border-primary/30 hover:bg-primary/20" type="button" @click="setEndByDuration">
                Bắt đầu + {{ timeLimit }} phút
              </button>
              <button class="px-3 py-1.5 text-xs font-semibold rounded bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-700" type="button" @click="setEndAfterMinutes(30)">+30 phút</button>
              <button class="px-3 py-1.5 text-xs font-semibold rounded bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-700" type="button" @click="setEndAfterMinutes(60)">+1 giờ</button>
              <button class="px-3 py-1.5 text-xs font-semibold rounded bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-700" type="button" @click="setEndAfterMinutes(90)">+1h30</button>
            </div>
          </div>
        </div>

        <div class="mt-6 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800/70 px-4 py-3 text-sm text-slate-600 dark:text-slate-300">
          <span class="font-semibold text-slate-700 dark:text-slate-200">Xem trước:</span>
          <span v-if="previewLabel"> {{ previewLabel }}</span>
          <span v-else> Hãy chọn thời gian bắt đầu và kết thúc.</span>
        </div>

        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4 pt-8">
          <button class="px-8 py-3 rounded-lg border border-slate-200 dark:border-slate-800 font-semibold hover:bg-slate-100 dark:hover:bg-slate-800 transition-all" type="button" @click="goBack">
            Quay lại
          </button>
          <div class="flex gap-3">
            <button class="px-6 py-3 rounded-lg border border-slate-200 dark:border-slate-700 font-semibold text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-all" type="button" @click="setEndByDuration">
              Tự tính kết thúc
            </button>
            <button :disabled="isSubmitting" class="px-10 py-3 rounded-lg bg-primary text-white font-bold shadow-lg shadow-primary/30 hover:bg-primary/90 transition-all flex items-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed" type="button" @click="handleCreateSession">
              {{ isSubmitting ? 'Đang tạo...' : 'Tạo đợt thi mới' }}
              <span class="material-symbols-outlined text-lg">add_circle</span>
            </button>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { createNewSession, getExamDetail } from '../../services/examService'
import { useToast } from '../../composables/useToast'
import { useRoute, useRouter } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const router = useRouter()
const route = useRoute()
const isDark = ref(false)
const timeLimit = ref(60)
const isSubmitting = ref(false)
const durationPresets = [15, 30, 45, 60, 90, 120, 180]
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
const examTitle = computed(() => route.query.title || 'Đề thi')

const minDateStr = computed(() => formatDatePart(new Date()))

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

const previewLabel = computed(() => {
  const start = toDate(startAt.value)
  const end = toDate(endAt.value)
  if (!start || !end) return ''
  if (end <= start) return 'Thời gian kết thúc đang sớm hơn thời gian bắt đầu.'
  return `${start.toLocaleString()} → ${end.toLocaleString()}`
})

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

const setStartIn1Hour = () => {
  const date = new Date(Date.now() + 60 * 60000)
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

const setEndAfterMinutes = (minutes) => {
  const start = toDate(startAt.value)
  if (!start) return
  const date = new Date(start.getTime() + minutes * 60000)
  endDate.value = formatDatePart(date)
  endClock.value = formatTimePart(date)
}

const clampTimeLimit = () => {
  const val = Number(timeLimit.value)
  if (Number.isNaN(val) || val < 5) timeLimit.value = 5
  else if (val > 480) timeLimit.value = 480
}

const goBack = () => {
  router.push('/teacher/exams/list')
}

const handleCreateSession = async () => {
  const duration = Math.max(5, Math.min(480, Number(timeLimit.value) || 60))
  if (!examId.value) {
    toast.error('Thiếu mã đề thi.')
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
    const exam = await createNewSession(examId.value, {
      startTime: startAt.value,
      endTime: endAt.value,
      durationMinutes: duration
    })

    router.push({
      path: '/teacher/exams/created-success',
      query: {
        examId: exam.id,
        code: exam.code || '',
        title: exam.title || examTitle.value,
        durationMinutes: duration,
        startAt: startAt.value,
        endAt: endAt.value
      }
    })
  } catch (error) {
    toast.error(error?.payload?.message || 'Không thể tạo đợt thi mới. Vui lòng thử lại.')
  } finally {
    isSubmitting.value = false
  }
}

onMounted(async () => {
  if (examId.value) {
    try {
      const exam = await getExamDetail(examId.value)
      if (exam?.durationMinutes) {
        timeLimit.value = Math.max(5, Math.min(480, exam.durationMinutes))
      }
    } catch {
      // ignore
    }
  }
})
</script>
