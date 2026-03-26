<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <div class="layout-container flex h-full grow flex-col">
      <TeacherTopHeader active-section="monitoring" />

      <main class="teacher-page-shell relative mx-auto max-w-7xl overflow-x-hidden">
        <div class="relative mb-6 animate-fade-up">
          <h1 class="mb-1.5 text-2xl font-black tracking-tight text-slate-900 dark:text-white sm:text-3xl">Chọn đề thi đang diễn ra</h1>
          <p class="text-sm text-slate-500 dark:text-slate-400 sm:text-base">Chỉ hiển thị các đề thi đang trong thời gian thi để giám sát theo thời gian thực.</p>
        </div>

        <div class="relative mb-6 flex animate-fade-up-delay flex-col gap-3 md:flex-row md:gap-4">
          <div class="relative flex-1">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-primary/50">search</span>
            <input v-model="search" class="w-full pl-10 pr-4 py-3 rounded-xl border-none bg-white dark:bg-slate-800 shadow-sm focus:ring-2 focus:ring-primary text-slate-900 dark:text-white" placeholder="Tìm đề thi theo tiêu đề hoặc mã..." type="text" />
          </div>
        </div>


        <div class="relative grid grid-cols-1 gap-4 animate-fade-up-delay md:grid-cols-2 xl:grid-cols-3 xl:gap-5">
          <div v-if="isLoading" class="col-span-full rounded-xl border border-slate-200 bg-white p-6 text-center text-sm text-slate-500 dark:border-slate-800 dark:bg-slate-800">
            <span class="material-symbols-outlined mb-2 inline-block animate-spin text-xl text-primary">progress_activity</span>
            <p>Đang tải danh sách đề thi…</p>
          </div>

          <div v-else-if="!filteredExams.length" class="col-span-full">
            <EmptyState
              class="rounded-2xl border border-dashed border-slate-200 bg-slate-50/80 dark:border-slate-700 dark:bg-slate-900/40"
              icon="monitoring"
              title="Không có đề thi đang diễn ra"
              description="Hiện không có kỳ thi nào trong khung giờ. Thử tìm kiếm khác hoặc quay lại sau; bạn cũng có thể tạo đề mới từ ngân hàng đề."
              action-label="Mở ngân hàng đề"
              dense
              fill
              @action="goToExamBank"
            />
          </div>

          <div v-for="exam in filteredExams" :key="exam.id" :class="exam.cardBorder" class="bg-white dark:bg-slate-800 rounded-xl overflow-hidden shadow-sm border border-primary/5 flex flex-col group hover:-translate-y-0.5 hover:shadow-lg transition-all duration-200 border-l-4">
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
            <button
              type="button"
              @click="goToExamBank"
              class="mt-6 px-4 py-2 text-primary font-bold hover:bg-primary/5 rounded-lg border border-primary/20 transition-all"
            >
              Xem ngân hàng đề
            </button>
          </div>
        </div>

        <div class="relative mt-12 flex justify-center items-center gap-2 bg-white dark:bg-slate-800 p-4 rounded-xl shadow-sm animate-fade-up-delay">
          <span class="size-3 rounded-full bg-green-500"></span>
          <span class="text-sm font-medium">{{ monitoringCards.length }} đề thi đang diễn ra</span>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ApiError } from '../../services/apiClient'
import { listExams } from '../../services/examService'
import { useRouter } from 'vue-router'
import { useToast } from '../../composables/useToast'
import TeacherTopHeader from './TeacherTopHeader.vue'
import EmptyState from '../shared/EmptyState.vue'

const router = useRouter()
const isDark = ref(false)
const search = ref('')
const exams = ref([])
const isLoading = ref(false)
const toast = useToast()

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString()
}

const isExamOngoing = (exam, nowMs) => {
  const startMs = new Date(exam.startTime || '').getTime()
  const endMs = new Date(exam.endTime || '').getTime()
  if (Number.isNaN(startMs) || Number.isNaN(endMs)) return false
  return Boolean(exam.isActive) && startMs <= nowMs && nowMs <= endMs
}

const monitoringCards = computed(() => {
  const nowMs = Date.now()
  return exams.value
    .filter((exam) => isExamOngoing(exam, nowMs))
    .map((exam) => ({
      id: exam.id,
      examId: exam.id,
      examCode: exam.code || '',
      title: exam.title,
      durationMinutes: exam.durationMinutes,
      location: exam.description || 'Đề thi trực tuyến',
      sessionMeta: `Bắt đầu: ${formatDateTime(exam.startTime)} • Kết thúc: ${formatDateTime(exam.endTime)}`,
      status: 'Đang hoạt động',
      statusClass: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
      students: `${exam.questionCount || 0}`,
      leftLabel: 'Số câu hỏi',
      timeLabel: 'Kết thúc',
      timeIcon: 'timer',
      timeIconClass: 'text-orange-500',
      timeValue: formatDateTime(exam.endTime),
      actionLabel: 'Theo dõi trực tiếp',
      actionIcon: 'visibility',
      actionClass: 'bg-primary hover:bg-primary/90 text-white',
      cardBorder: 'border-l-green-500'
    }))
})

const filteredExams = computed(() => {
  const keyword = search.value.trim().toLowerCase()
  if (!keyword) return monitoringCards.value
  return monitoringCards.value.filter((exam) =>
    exam.title.toLowerCase().includes(keyword) || String(exam.examCode || '').toLowerCase().includes(keyword)
  )
})

const goToExamBank = () => {
  router.push('/teacher/exams')
}

const openLiveSession = (exam) => {
  router.push({
    path: '/teacher/live-monitoring/session',
    query: {
      examId: exam.examId,
      title: exam.title,
      code: exam.examCode || '',
      meta: exam.sessionMeta,
      durationMinutes: exam.durationMinutes || ''
    }
  })
}

const loadExams = async () => {
  isLoading.value = true
  try {
    exams.value = await listExams()
  } catch (error) {
    toast.error(error instanceof ApiError ? error.message : 'Không thể tải danh sách đề thi.')
  } finally {
    isLoading.value = false
  }
}

onMounted(loadExams)
</script>

