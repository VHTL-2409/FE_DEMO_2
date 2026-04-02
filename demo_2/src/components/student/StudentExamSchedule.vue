<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto max-w-6xl px-4 pb-10 pt-4 sm:px-6 lg:px-8">

      <!-- Header -->
      <div class="mb-4">
        <ExamScheduleHeader
          :total-count="filteredExams.length"
          :live-count="statusCounts.live"
          :upcoming-count="statusCounts.upcoming + statusCounts.opening"
          :completed-count="statusCounts.completed"
        />
      </div>

      <!-- View Toggle -->
      <div class="mb-4 flex items-center justify-between">
        <ExamScheduleFilters
          :active-tab="activeTab"
          :tab-counts="tabCounts"
          :search-value="searchQuery"
          placeholder="Tìm kiếm theo tên kỳ thi..."
          @tab-change="onTabChange"
          @search="onSearch"
        />
        <div class="flex items-center gap-2">
          <button
            type="button"
            class="view-toggle-btn"
            :class="{ 'view-toggle-btn--active': viewMode === 'list' }"
            @click="viewMode = 'list'"
            title="Xem danh sách"
          >
            <LucideIcon name="list" />
          </button>
          <button
            type="button"
            class="view-toggle-btn"
            :class="{ 'view-toggle-btn--active': viewMode === 'calendar' }"
            @click="viewMode = 'calendar'"
            title="Xem lịch"
          >
            <LucideIcon name="calendar_month" />
          </button>
        </div>
      </div>

      <!-- Calendar View -->
      <div v-if="viewMode === 'calendar'" class="mb-6">
        <div class="grid gap-6 lg:grid-cols-3">
          <div class="lg:col-span-2">
            <StudentExamCalendar
              :exams="filteredExams"
              @day-click="onCalendarDayClick"
            />
          </div>
          <div>
            <BaseCard padding="lg" class="h-full">
              <h3 class="text-base font-bold mb-4 flex items-center gap-2" style="font-family: 'Playfair Display', serif; color: var(--ds-text)">
                <LucideIcon name="event" />
                Kỳ thi ngày {{ selectedDayLabel }}
              </h3>
              <div v-if="!selectedDayExams.length" class="text-center py-8">
                <LucideIcon name="event_available" class="text-4xl mb-2" style="color: var(--ds-text-muted)" />
                <p class="text-sm" style="color: var(--ds-text-muted)">Không có kỳ thi nào trong ngày này</p>
              </div>
              <div v-else class="space-y-3">
                <div
                  v-for="exam in selectedDayExams"
                  :key="exam.id || exam.examId"
                  class="exam-day-item"
                  @click="onExamClick(exam)"
                >
                  <div class="exam-day-item__icon" :class="getExamStatusClass(exam)">
                    <LucideIcon :name="getExamStatusIcon(exam)" />
                  </div>
                  <div class="exam-day-item__info">
                    <p class="exam-day-item__title">{{ exam.title || exam.examTitle }}</p>
                    <p class="exam-day-item__time">
                      {{ formatTime(exam.startTime || exam.startDate) }}
                    </p>
                  </div>
                  <div class="exam-day-item__status">
                    <span class="status-badge" :class="getExamStatusClass(exam)">
                      {{ getExamStatusLabel(exam) }}
                    </span>
                  </div>
                </div>
              </div>
            </BaseCard>
          </div>
        </div>
      </div>

      <!-- List View -->
      <div v-else>
        <UpcomingExamList
          :exams="filteredExams"
          :loading="isLoading"
          :section-label="sectionLabel"
          :empty-type="emptyType"
          :has-more="false"
          :remaining-count="0"
          :show-cta="true"
          @exam-click="onExamClick"
          @exam-enter="onExamEnter"
          @exam-details="onExamDetails"
          @cta-click="goToExamJoin"
        />
      </div>

    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listMyAttempts } from '../../services/attemptService'
import { useToast } from '../../composables/useToast'
import LucideIcon from '../common/LucideIcon.vue'

// Schedule components
import ExamScheduleHeader from './schedule/ExamScheduleHeader.vue'
import ExamScheduleFilters from './schedule/ExamScheduleFilters.vue'
import UpcomingExamList from './schedule/UpcomingExamList.vue'
import StudentExamCalendar from './schedule/StudentExamCalendar.vue'
import BaseCard from '../shared/BaseCard.vue'

const router = useRouter()
const toast = useToast()

// Filter state
const activeTab = ref('all')
const searchQuery = ref('')
const viewMode = ref('list') // 'list' | 'calendar'
const selectedDate = ref(null)

// Data
const allExams = ref([])
const isLoading = ref(false)

// ─── Status helpers ─────────────────────────────────────────────────
const getExamStatus = (exam) => {
  const nowMs = Date.now()
  const startMs = new Date(exam.startTime || exam.startDate || '').getTime()
  const endMs = new Date(exam.endTime || exam.endDate || '').getTime()

  if (Number.isNaN(startMs)) return 'upcoming'
  if (Number.isNaN(endMs)) {
    if (nowMs >= startMs - 10 * 60 * 1000 && nowMs < startMs) return 'opening'
    if (nowMs >= startMs && nowMs < startMs + 60 * 60 * 1000) return 'live'
    if (nowMs < startMs) return 'upcoming'
    return 'completed'
  }

  if (nowMs > endMs) return 'completed'
  if (nowMs >= startMs - 10 * 60 * 1000 && nowMs < startMs) return 'opening'
  if (nowMs >= startMs && nowMs <= endMs) return 'live'
  return 'upcoming'
}

// ─── Calendar helpers ───────────────────────────────────────────────
const selectedDayLabel = computed(() => {
  if (!selectedDate.value) return 'hôm nay'
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const selected = new Date(selectedDate.value)
  selected.setHours(0, 0, 0, 0)
  
  if (selected.getTime() === today.getTime()) return 'hôm nay'
  
  return selected.toLocaleDateString('vi-VN', {
    day: 'numeric',
    month: 'long'
  })
})

const selectedDayExams = computed(() => {
  if (!selectedDate.value) return []
  const targetDate = new Date(selectedDate.value)
  targetDate.setHours(0, 0, 0, 0)
  
  return filteredExams.value.filter(exam => {
    const examDate = new Date(exam.startTime || exam.startDate || '')
    if (Number.isNaN(examDate.getTime())) return false
    examDate.setHours(0, 0, 0, 0)
    return examDate.getTime() === targetDate.getTime()
  })
})

const onCalendarDayClick = (day) => {
  selectedDate.value = day.date
}

const getExamStatusClass = (exam) => {
  const status = getExamStatus(exam)
  const statusMap = {
    live: 'exam-day-item__icon--live',
    opening: 'exam-day-item__icon--upcoming',
    upcoming: 'exam-day-item__icon--upcoming',
    completed: 'exam-day-item__icon--completed'
  }
  return statusMap[status] || ''
}

const getExamStatusIcon = (exam) => {
  const status = getExamStatus(exam)
  const iconMap = {
    live: 'play_circle',
    opening: 'schedule',
    upcoming: 'quiz',
    completed: 'check_circle'
  }
  return iconMap[status] || 'quiz'
}

const getExamStatusLabel = (exam) => {
  const status = getExamStatus(exam)
  const labelMap = {
    live: 'Đang thi',
    opening: 'Sắp bắt đầu',
    upcoming: 'Sắp tới',
    completed: 'Đã xong'
  }
  return labelMap[status] || ''
}

const formatTime = (value) => {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

// ─── Tab counts ─────────────────────────────────────────────────────
const tabCounts = computed(() => ({
  all: allExams.value.length,
  today: allExams.value.filter(e => {
    const d = new Date(e.startTime || e.startDate || '')
    if (Number.isNaN(d.getTime())) return false
    const today = new Date()
    return d.toDateString() === today.toDateString()
  }).length,
  upcoming: allExams.value.filter(e => {
    const status = getExamStatus(e)
    return status === 'upcoming' || status === 'opening'
  }).length,
  completed: allExams.value.filter(e => getExamStatus(e) === 'completed').length
}))

const statusCounts = computed(() => ({
  live: allExams.value.filter(e => getExamStatus(e) === 'live').length,
  upcoming: allExams.value.filter(e => getExamStatus(e) === 'upcoming').length,
  opening: allExams.value.filter(e => getExamStatus(e) === 'opening').length,
  completed: allExams.value.filter(e => getExamStatus(e) === 'completed').length,
  missed: 0
}))

// ─── Filtered exams ────────────────────────────────────────────────
const filteredExams = computed(() => {
  let list = [...allExams.value]

  // Tab filter
  if (activeTab.value === 'today') {
    const today = new Date()
    list = list.filter(e => {
      const d = new Date(e.startTime || e.startDate || '')
      return !Number.isNaN(d.getTime()) && d.toDateString() === today.toDateString()
    })
  } else if (activeTab.value === 'upcoming') {
    list = list.filter(e => {
      const status = getExamStatus(e)
      return status === 'upcoming' || status === 'opening'
    })
  } else if (activeTab.value === 'completed') {
    list = list.filter(e => getExamStatus(e) === 'completed')
  }

  // Search filter
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase().trim()
    list = list.filter(e => {
      const title = (e.title || e.examTitle || '').toLowerCase()
      const subject = (e.subject || '').toLowerCase()
      return title.includes(q) || subject.includes(q)
    })
  }

  // Sort: live first, then opening, upcoming, completed
  const order = { live: 0, opening: 1, upcoming: 2, missed: 3, completed: 4 }
  list.sort((a, b) => {
    const sa = order[getExamStatus(a)] ?? 99
    const sb = order[getExamStatus(b)] ?? 99
    if (sa !== sb) return sa - sb
    const ta = new Date(a.startTime || a.startDate || 0).getTime()
    const tb = new Date(b.startTime || b.startDate || 0).getTime()
    return ta - tb
  })

  return list
})

// ─── Section label & empty type ────────────────────────────────────
const sectionLabel = computed(() => {
  const labels = {
    all: 'Tất cả kỳ thi',
    today: 'Kỳ thi hôm nay',
    upcoming: 'Kỳ thi sắp tới',
    completed: 'Kỳ thi đã hoàn thành'
  }
  return labels[activeTab.value] || 'Kết quả lọc'
})

const emptyType = computed(() => {
  if (searchQuery.value.trim()) return 'no-results'
  if (activeTab.value === 'completed') return 'all-done'
  return 'no-exams'
})

// ─── Actions ────────────────────────────────────────────────────────
const onTabChange = (tab) => { activeTab.value = tab }
const onSearch = (q) => { searchQuery.value = q }

const onExamClick = (exam) => { onExamDetails(exam) }

const onExamEnter = (exam) => {
  router.push({
    path: '/student/exam-waiting-room',
    query: {
      examId: exam.id,
      exam: exam.title || exam.examTitle || 'Kỳ thi',
      duration: exam.duration || exam.durationMinutes || 60,
      questions: exam.questionCount || 0,
      startAt: exam.startTime || exam.startDate || '',
      endAt: exam.endTime || exam.endDate || '',
      requireCameraMic: exam.requireCameraMic === false ? 'false' : 'true'
    }
  })
}

const onExamDetails = (exam) => {
  const status = getExamStatus(exam)
  if (status === 'completed') {
    router.push({
      path: '/student/exam-result',
      query: {
        attemptId: exam.attemptId || exam.id,
        examTitle: exam.title || exam.examTitle
      }
    })
  } else if (status === 'live') {
    onExamEnter(exam)
  } else {
    router.push({
      path: '/student/exam-waiting-room',
      query: {
        examId: exam.id,
        exam: exam.title || exam.examTitle || 'Kỳ thi',
        duration: exam.duration || exam.durationMinutes || 60,
        questions: exam.questionCount || 0,
        startAt: exam.startTime || exam.startDate || '',
        endAt: exam.endTime || exam.endDate || ''
      }
    })
  }
}

const goToExamJoin = () => router.push('/student/exam-join')

// ─── Data loading ───────────────────────────────────────────────────
const loadData = async () => {
  isLoading.value = true
  try {
    const attempts = await listMyAttempts()
    // Build exam list from attempts
    allExams.value = attempts.map(a => ({
      id: a.examId,
      examId: a.examId,
      attemptId: a.id,
      title: a.examTitle || 'Kỳ thi',
      examTitle: a.examTitle || 'Kỳ thi',
      startTime: a.startTime,
      endTime: a.endTime,
      startDate: a.startedAt,
      endDate: a.submittedAt,
      duration: a.durationMinutes,
      questionCount: a.questionCount,
      status: a.status,
      score: a.score,
      subject: a.subject,
      className: a.className,
      room: a.room
    }))
  } catch (error) {
    allExams.value = []
    toast.error('Không thể tải danh sách kỳ thi.')
  } finally {
    isLoading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
/* View Toggle Buttons */
.view-toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: var(--ds-radius-lg);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-muted);
  cursor: pointer;
  transition: all 0.15s ease;
}

.dark .view-toggle-btn {
  border-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
  color: var(--ds-text-muted);
}

.view-toggle-btn:hover {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border-color: var(--ds-primary-border);
}

.view-toggle-btn--active {
  background: var(--ds-primary);
  color: white;
  border-color: var(--ds-primary);
}

.view-toggle-btn--active:hover {
  background: var(--ds-primary-hover);
  color: white;
}

/* Exam Day Item */
.exam-day-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-gray-50);
  border: 1.5px solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dark .exam-day-item {
  background: var(--ds-gray-800);
}

.exam-day-item:hover {
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
  transform: translateX(4px);
}

.exam-day-item__icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.exam-day-item__icon--live {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.exam-day-item__icon--upcoming {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.exam-day-item__icon--completed {
  background: var(--ds-gray-200);
  color: var(--ds-gray-500);
}

.dark .exam-day-item__icon--completed {
  background: var(--ds-gray-700);
  color: var(--ds-gray-400);
}

.exam-day-item__info {
  flex: 1;
  min-width: 0;
}

.exam-day-item__title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dark .exam-day-item__title {
  color: var(--ds-text);
}

.exam-day-item__time {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.125rem 0 0;
  font-weight: 500;
}

.exam-day-item__status {
  flex-shrink: 0;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
  font-weight: 700;
  white-space: nowrap;
}

.status-badge.exam-day-item__icon--live {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.status-badge.exam-day-item__icon--upcoming {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.status-badge.exam-day-item__icon--completed {
  background: var(--ds-gray-200);
  color: var(--ds-gray-500);
}

.dark .status-badge.exam-day-item__icon--completed {
  background: var(--ds-gray-700);
  color: var(--ds-gray-400);
}

/* Responsive */
@media (max-width: 768px) {
  .view-toggle-btn {
    width: 2.25rem;
    height: 2.25rem;
  }
}
</style>
