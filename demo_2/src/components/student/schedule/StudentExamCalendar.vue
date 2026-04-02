/**
 * StudentExamCalendar.vue
 * Calendar view component for student exam schedule
 * Supports month view with exam indicators
 */
<template>
  <div class="exam-calendar">
    <!-- Calendar Header -->
    <div class="exam-calendar__header">
      <button type="button" class="exam-calendar__nav-btn" @click="previousMonth">
        <span class="material-symbols-outlined">chevron_left</span>
      </button>
      <h3 class="exam-calendar__month-title">{{ monthYearLabel }}</h3>
      <button type="button" class="exam-calendar__nav-btn" @click="nextMonth">
        <span class="material-symbols-outlined">chevron_right</span>
      </button>
      <button type="button" class="exam-calendar__today-btn" @click="goToToday">
        Hôm nay
      </button>
    </div>

    <!-- Weekday Headers -->
    <div class="exam-calendar__weekdays">
      <div v-for="day in weekdays" :key="day" class="exam-calendar__weekday">
        {{ day }}
      </div>
    </div>

    <!-- Calendar Grid -->
    <div class="exam-calendar__grid">
      <div
        v-for="(day, index) in calendarDays"
        :key="index"
        class="exam-calendar__day"
        :class="{
          'exam-calendar__day--other-month': !day.isCurrentMonth,
          'exam-calendar__day--today': day.isToday,
          'exam-calendar__day--has-exam': day.exams.length > 0,
          'exam-calendar__day--selected': isSelected(day)
        }"
        @click="selectDay(day)"
      >
        <span class="exam-calendar__day-number">{{ day.dayNumber }}</span>
        
        <!-- Exam indicators -->
        <div v-if="day.exams.length > 0" class="exam-calendar__exams">
          <div
            v-for="(exam, examIndex) in day.exams.slice(0, 3)"
            :key="exam.id || examIndex"
            class="exam-calendar__exam-dot"
            :class="getExamDotClass(exam)"
            :title="exam.title || exam.examTitle"
          >
            <span class="material-symbols-outlined exam-calendar__exam-icon">
              {{ getExamIcon(exam) }}
            </span>
          </div>
          <div v-if="day.exams.length > 3" class="exam-calendar__exam-more">
            +{{ day.exams.length - 3 }}
          </div>
        </div>
      </div>
    </div>

    <!-- Legend -->
    <div class="exam-calendar__legend">
      <div class="exam-calendar__legend-item">
        <span class="exam-calendar__legend-dot exam-calendar__legend-dot--live"></span>
        <span>Đang thi</span>
      </div>
      <div class="exam-calendar__legend-item">
        <span class="exam-calendar__legend-dot exam-calendar__legend-dot--upcoming"></span>
        <span>Sắp thi</span>
      </div>
      <div class="exam-calendar__legend-item">
        <span class="exam-calendar__legend-dot exam-calendar__legend-dot--completed"></span>
        <span>Đã thi</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  exams: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['day-click', 'exam-click'])

const currentDate = ref(new Date())
const selectedDate = ref(null)

const weekdays = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']

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

const monthYearLabel = computed(() => {
  return currentDate.value.toLocaleDateString('vi-VN', {
    month: 'long',
    year: 'numeric'
  })
})

const calendarDays = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  
  const firstDayOfMonth = new Date(year, month, 1)
  const lastDayOfMonth = new Date(year, month + 1, 0)
  const firstDayWeekday = firstDayOfMonth.getDay()
  
  const days = []
  
  // Previous month days
  const prevMonth = new Date(year, month, 0)
  for (let i = firstDayWeekday - 1; i >= 0; i--) {
    const dayNumber = prevMonth.getDate() - i
    days.push({
      date: new Date(year, month - 1, dayNumber),
      dayNumber,
      isCurrentMonth: false,
      isToday: false,
      exams: []
    })
  }
  
  // Current month days
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  
  for (let day = 1; day <= lastDayOfMonth.getDate(); day++) {
    const date = new Date(year, month, day)
    date.setHours(0, 0, 0, 0)
    
    const dayExams = props.exams.filter(exam => {
      const examDate = new Date(exam.startTime || exam.startDate || '')
      if (Number.isNaN(examDate.getTime())) return false
      examDate.setHours(0, 0, 0, 0)
      return examDate.getTime() === date.getTime()
    })
    
    days.push({
      date,
      dayNumber: day,
      isCurrentMonth: true,
      isToday: date.getTime() === today.getTime(),
      exams: dayExams
    })
  }
  
  // Next month days
  const remainingDays = 42 - days.length
  for (let day = 1; day <= remainingDays; day++) {
    days.push({
      date: new Date(year, month + 1, day),
      dayNumber: day,
      isCurrentMonth: false,
      isToday: false,
      exams: []
    })
  }
  
  return days
})

const previousMonth = () => {
  const newDate = new Date(currentDate.value)
  newDate.setMonth(newDate.getMonth() - 1)
  currentDate.value = newDate
}

const nextMonth = () => {
  const newDate = new Date(currentDate.value)
  newDate.setMonth(newDate.getMonth() + 1)
  currentDate.value = newDate
}

const goToToday = () => {
  currentDate.value = new Date()
  selectedDate.value = new Date()
}

const selectDay = (day) => {
  if (!day.isCurrentMonth) return
  selectedDate.value = day.date
  emit('day-click', day)
}

const isSelected = (day) => {
  if (!selectedDate.value || !day.isCurrentMonth) return false
  const selected = new Date(selectedDate.value)
  selected.setHours(0, 0, 0, 0)
  const current = new Date(day.date)
  current.setHours(0, 0, 0, 0)
  return selected.getTime() === current.getTime()
}

const getExamDotClass = (exam) => {
  const status = getExamStatus(exam)
  return {
    'exam-calendar__exam-dot--live': status === 'live',
    'exam-calendar__exam-dot--upcoming': status === 'upcoming' || status === 'opening',
    'exam-calendar__exam-dot--completed': status === 'completed'
  }
}

const getExamIcon = (exam) => {
  const status = getExamStatus(exam)
  if (status === 'live') return 'play_circle'
  if (status === 'opening') return 'schedule'
  if (status === 'completed') return 'check_circle'
  return 'quiz'
}
</script>

<style scoped>
.exam-calendar {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.dark .exam-calendar {
  border-color: var(--ds-border-strong);
}

/* Header */
.exam-calendar__header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .exam-calendar__header {
  border-bottom-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.exam-calendar__nav-btn {
  width: 2rem;
  height: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--ds-radius-lg);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  cursor: pointer;
  transition: all 0.15s ease;
}

.dark .exam-calendar__nav-btn {
  border-color: var(--ds-border-strong);
  background: var(--ds-gray-700);
  color: var(--ds-text);
}

.exam-calendar__nav-btn:hover {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border-color: var(--ds-primary-border);
}

.exam-calendar__month-title {
  flex: 1;
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 800;
  color: var(--ds-text);
  text-align: center;
  margin: 0;
}

.dark .exam-calendar__month-title {
  color: var(--ds-text);
}

.exam-calendar__today-btn {
  padding: 0.375rem 0.875rem;
  border-radius: var(--ds-radius-lg);
  border: 1px solid var(--ds-primary-border);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.exam-calendar__today-btn:hover {
  background: var(--ds-primary);
  color: white;
}

/* Weekdays */
.exam-calendar__weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: var(--ds-gray-50);
  border-bottom: 1px solid var(--ds-border);
}

.dark .exam-calendar__weekdays {
  background: var(--ds-gray-800);
  border-bottom-color: var(--ds-border-strong);
}

.exam-calendar__weekday {
  padding: 0.625rem;
  text-align: center;
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* Grid */
.exam-calendar__grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  padding: 0.5rem;
  gap: 0.25rem;
}

.exam-calendar__day {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding: 0.375rem;
  border-radius: var(--ds-radius-lg);
  cursor: pointer;
  transition: all 0.15s ease;
  min-height: 3.5rem;
}

.exam-calendar__day:hover:not(.exam-calendar__day--other-month) {
  background: var(--ds-gray-100);
}

.dark .exam-calendar__day:hover:not(.exam-calendar__day--other-month) {
  background: var(--ds-gray-700);
}

.exam-calendar__day--other-month {
  cursor: default;
  opacity: 0.4;
}

.exam-calendar__day--today .exam-calendar__day-number {
  background: var(--ds-primary);
  color: white;
  border-radius: var(--ds-radius-full);
  width: 1.75rem;
  height: 1.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.exam-calendar__day--selected {
  background: var(--ds-primary-soft);
}

.exam-calendar__day--selected .exam-calendar__day-number {
  color: var(--ds-primary);
  font-weight: 800;
}

.exam-calendar__day--has-exam {
  background: var(--ds-gray-50);
}

.dark .exam-calendar__day--has-exam {
  background: var(--ds-gray-800);
}

.dark .exam-calendar__day--has-exam.exam-calendar__day--today {
  background: rgba(79, 70, 229, 0.15);
}

.exam-calendar__day-number {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text);
  margin-bottom: 0.25rem;
}

.dark .exam-calendar__day-number {
  color: var(--ds-text);
}

/* Exam dots */
.exam-calendar__exams {
  display: flex;
  flex-wrap: wrap;
  gap: 0.125rem;
  justify-content: center;
  margin-top: auto;
}

.exam-calendar__exam-dot {
  width: 1.25rem;
  height: 1.25rem;
  border-radius: var(--ds-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.15s ease;
}

.exam-calendar__exam-dot:hover {
  transform: scale(1.15);
}

.exam-calendar__exam-dot--live {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.exam-calendar__exam-dot--upcoming {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.exam-calendar__exam-dot--completed {
  background: var(--ds-gray-200);
  color: var(--ds-gray-500);
}

.dark .exam-calendar__exam-dot--completed {
  background: var(--ds-gray-700);
  color: var(--ds-gray-400);
}

.exam-calendar__exam-icon {
  font-size: 0.75rem !important;
}

.exam-calendar__exam-more {
  font-size: 0.6rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  padding: 0.125rem 0.25rem;
  background: var(--ds-gray-100);
  border-radius: var(--ds-radius-xs);
}

.dark .exam-calendar__exam-more {
  background: var(--ds-gray-700);
  color: var(--ds-text-muted);
}

/* Legend */
.exam-calendar__legend {
  display: flex;
  justify-content: center;
  gap: 1.25rem;
  padding: 0.875rem 1rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .exam-calendar__legend {
  border-top-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.exam-calendar__legend-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}

.exam-calendar__legend-dot {
  width: 0.625rem;
  height: 0.625rem;
  border-radius: var(--ds-radius-xs);
}

.exam-calendar__legend-dot--live {
  background: var(--ds-success);
}

.exam-calendar__legend-dot--upcoming {
  background: var(--ds-primary);
}

.exam-calendar__legend-dot--completed {
  background: var(--ds-gray-400);
}

/* Responsive */
@media (max-width: 640px) {
  .exam-calendar__day {
    min-height: 2.5rem;
    padding: 0.25rem;
  }
  
  .exam-calendar__day-number {
    font-size: 0.7rem;
  }
  
  .exam-calendar__exam-dot {
    width: 1rem;
    height: 1rem;
  }
  
  .exam-calendar__exam-icon {
    font-size: 0.625rem !important;
  }
  
  .exam-calendar__legend {
    gap: 0.75rem;
    padding: 0.75rem 0.5rem;
  }
}
</style>
