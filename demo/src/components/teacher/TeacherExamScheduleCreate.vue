<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto max-w-4xl px-4 pb-10 pt-4 sm:px-6 lg:px-8">
      <div class="mb-6 ds-animate-fade-up">
        <h1 class="text-3xl font-bold tracking-tight" style="color: var(--ds-text)">{{ isEditMode ? 'Chỉnh sửa đề thi' : 'Thiết lập &amp; tạo đề thi' }}</h1>
        <p class="mt-1" style="color: var(--ds-text-muted)">{{ selectedExamTitle }}</p>
      </div>

      <div class="mb-6 ds-animate-fade-up">
        <div class="flex flex-wrap items-center gap-3 text-xs font-semibold uppercase tracking-wider" style="color: var(--ds-text-muted)">
          <template v-for="(step, index) in steps" :key="step">
            <div class="flex items-center gap-3">
              <span
                class="size-7 rounded-full flex items-center justify-center text-[11px] font-bold"
                :style="index + 1 <= currentStep
                  ? { backgroundColor: 'var(--ds-primary)', color: 'white' }
                  : { backgroundColor: 'var(--ds-gray-200)', color: 'var(--ds-text-muted)' }"
              >
                {{ index + 1 }}
              </span>
              <span :style="index + 1 === currentStep ? { color: 'var(--ds-text)' } : {}">{{ step }}</span>
            </div>
            <span v-if="index < steps.length - 1" class="h-px w-6" style="background-color: var(--ds-gray-200)"></span>
          </template>
        </div>
      </div>

      <section class="p-8 ds-animate-fade-up-delay" style="background-color: var(--ds-surface); border-radius: var(--ds-radius-xl); border: 1px solid var(--ds-border); box-shadow: var(--ds-shadow-sm)">
        <FormSection title="Cấu hình thời gian" description="Thiết lập thời gian làm bài và lịch thi">
          <template #header>
            <LucideIcon name="settings" />
          </template>

          <div class="space-y-4">
            <div class="p-4 rounded-xl border" style="background-color: var(--ds-gray-50); border-color: var(--ds-border)">
              <div class="flex items-center gap-2 text-sm font-semibold mb-3" style="color: var(--ds-text-secondary)">
                <LucideIcon name="timer" size="16" />
                Thời lượng làm bài (phút)
              </div>
              <div class="flex flex-wrap items-center gap-3">
                <input
                  v-model.number="timeLimit"
                  type="number"
                  min="5"
                  max="480"
                  class="w-24 px-4 py-2.5 rounded-lg border text-center font-bold"
                  style="border-color: var(--ds-border); background-color: var(--ds-surface); color: var(--ds-text); outline-color: var(--ds-primary)"
                  @blur="clampTimeLimit"
                />
                <span style="color: var(--ds-text-muted)">phút</span>
                <div class="flex flex-wrap gap-2">
                  <button
                    v-for="opt in durationPresets"
                    :key="opt"
                    type="button"
                    class="px-3 py-1.5 text-xs font-semibold rounded border"
                    :style="timeLimit === opt
                      ? { backgroundColor: 'var(--ds-primary)', color: 'white', borderColor: 'var(--ds-primary)' }
                      : { backgroundColor: 'var(--ds-surface)', borderColor: 'var(--ds-border)', color: 'var(--ds-text)' }"
                    @click="timeLimit = opt"
                  >
                    {{ opt }}p
                  </button>
                </div>
              </div>
            </div>

            <div class="p-4 rounded-xl border" style="background-color: var(--ds-gray-50); border-color: var(--ds-border)">
              <div class="flex items-center gap-2 text-sm font-semibold" style="color: var(--ds-text-secondary)">
                <LucideIcon name="schedule" size="16" />
                Bắt đầu
              </div>
              <div class="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-3">
                <input
                  v-model="startDate"
                  :min="minDateStr"
                  class="w-full px-4 py-3 rounded-lg border outline-none transition-all"
                  style="border-color: var(--ds-border); background-color: rgba(255,255,255,0.9); color: var(--ds-text); outline-color: var(--ds-primary)"
                  type="date"
                  @input="closePicker"
                  @change="closePicker"
                />
                <input
                  v-model="startClock"
                  class="w-full px-4 py-3 rounded-lg border outline-none transition-all"
                  style="border-color: var(--ds-border); background-color: rgba(255,255,255,0.9); color: var(--ds-text); outline-color: var(--ds-primary)"
                  type="time"
                  step="300"
                  @input="closePicker"
                  @change="closePicker"
                />
              </div>
              <div class="mt-3 flex flex-wrap gap-2">
                <button class="px-3 py-1.5 text-xs font-semibold rounded border" style="background-color: var(--ds-surface); border-color: var(--ds-border); color: var(--ds-text)" type="button" @click="setStartNow">Bây giờ</button>
                <button class="px-3 py-1.5 text-xs font-semibold rounded border" style="background-color: var(--ds-surface); border-color: var(--ds-border); color: var(--ds-text)" type="button" @click="setStartIn15Minutes">+15 phút</button>
                <button class="px-3 py-1.5 text-xs font-semibold rounded border" style="background-color: var(--ds-surface); border-color: var(--ds-border); color: var(--ds-text)" type="button" @click="setStartIn30Minutes">+30 phút</button>
                <button class="px-3 py-1.5 text-xs font-semibold rounded border" style="background-color: var(--ds-surface); border-color: var(--ds-border); color: var(--ds-text)" type="button" @click="setStartIn1Hour">+1 giờ</button>
              </div>
            </div>

            <div class="p-4 rounded-xl border" style="background-color: var(--ds-gray-50); border-color: var(--ds-border)">
              <div class="flex items-center gap-2 text-sm font-semibold" style="color: var(--ds-text-secondary)">
                <LucideIcon name="event_available" size="16" />
                Kết thúc
              </div>
              <div class="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-3">
                <input
                  v-model="endDate"
                  :min="startDate"
                  class="w-full px-4 py-3 rounded-lg border outline-none transition-all"
                  style="border-color: var(--ds-border); background-color: rgba(255,255,255,0.9); color: var(--ds-text); outline-color: var(--ds-primary)"
                  type="date"
                  @input="closePicker"
                  @change="closePicker"
                />
                <input
                  v-model="endClock"
                  class="w-full px-4 py-3 rounded-lg border outline-none transition-all"
                  style="border-color: var(--ds-border); background-color: rgba(255,255,255,0.9); color: var(--ds-text); outline-color: var(--ds-primary)"
                  type="time"
                  step="300"
                  @input="closePicker"
                  @change="closePicker"
                />
              </div>
              <p class="mt-2 text-xs" style="color: var(--ds-text-muted)">Các nút bên dưới tính từ thời gian bắt đầu đã chọn.</p>
              <div class="mt-3 flex flex-wrap gap-2">
                <button class="px-3 py-1.5 text-xs font-semibold rounded border" style="background-color: var(--ds-primary-soft); color: var(--ds-primary); border-color: var(--ds-primary-border)" type="button" @click="setEndByDuration">
                  Bắt đầu + {{ timeLimit }} phút
                </button>
                <button class="px-3 py-1.5 text-xs font-semibold rounded border" style="background-color: var(--ds-surface); border-color: var(--ds-border); color: var(--ds-text)" type="button" @click="setEndAfterMinutes(30)">+30 phút</button>
                <button class="px-3 py-1.5 text-xs font-semibold rounded border" style="background-color: var(--ds-surface); border-color: var(--ds-border); color: var(--ds-text)" type="button" @click="setEndAfterMinutes(60)">+1 giờ</button>
                <button class="px-3 py-1.5 text-xs font-semibold rounded border" style="background-color: var(--ds-surface); border-color: var(--ds-border); color: var(--ds-text)" type="button" @click="setEndAfterMinutes(90)">+1h30</button>
                <button class="px-3 py-1.5 text-xs font-semibold rounded border" style="background-color: var(--ds-surface); border-color: var(--ds-border); color: var(--ds-text)" type="button" @click="setEndAfterMinutes(120)">+2 giờ</button>
              </div>
            </div>
          </div>

          <div class="mt-6 px-4 py-3 text-sm rounded-lg border" style="background-color: var(--ds-gray-50); border-color: var(--ds-border); color: var(--ds-text-secondary)">
            <span class="font-semibold" style="color: var(--ds-text)">Xem trước:</span>
            <span v-if="previewLabel"> {{ previewLabel }}</span>
            <span v-else> Hãy chọn thời gian bắt đầu và kết thúc.</span>
          </div>

          <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4 pt-8">
            <button class="px-8 py-3 rounded-lg border font-semibold transition-all" style="border-color: var(--ds-border); color: var(--ds-text); background-color: var(--ds-surface)" type="button" @click="goBack">
              Quay lại
            </button>
            <div class="flex flex-col sm:flex-row gap-3">
              <button class="px-6 py-3 rounded-lg border font-semibold transition-all" style="border-color: var(--ds-border); color: var(--ds-text-secondary); background-color: var(--ds-surface)" type="button" @click="setEndByDuration">
                Tự tính kết thúc
              </button>
              <button :disabled="isSubmitting" class="px-10 py-3 rounded-lg font-bold shadow-lg transition-all flex items-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed" type="button" style="background-color: var(--ds-primary); color: white; --tw-shadow-color: var(--ds-primary)" @click="handleCreateAssignment">
                {{ isSubmitting ? (isEditMode ? 'Đang lưu...' : 'Đang xuất bản...') : (isEditMode ? 'Lưu thay đổi' : 'Xuất bản đề thi') }}
                <LucideIcon :name="isEditMode ? 'save' : 'rocket_launch'" size="18" />
              </button>
            </div>
          </div>
        </FormSection>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { createExamAssignment, listExamAssignments, updateExamAssignment } from '../../services/assignmentService'
import { getExamDetail, updateExam } from '../../services/examService'
import { useToast } from '../../composables/useToast'
import { useRoute, useRouter } from 'vue-router'
import FormSection from '../ui/FormSection.vue'

const router = useRouter()
const route = useRoute()
const steps = ['Chọn cách tạo', 'Nhập đề', 'Lập lịch', 'Hoàn tất']
const currentStep = 3
const timeLimit = ref(Math.max(5, Math.min(480, Number.parseInt(String(route.query.durationMinutes || ''), 10) || 60)))
const isSubmitting = ref(false)

const durationPresets = [15, 30, 45, 60, 90, 120, 180]

const toast = useToast()

const minDateStr = computed(() => formatDatePart(new Date()))

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
const isEditMode = computed(() => route.query.mode === 'edit')
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

const clampTimeLimit = () => {
  const val = Number(timeLimit.value)
  if (Number.isNaN(val) || val < 5) timeLimit.value = 5
  else if (val > 480) timeLimit.value = 480
}

const setEndAfterMinutes = (minutes) => {
  const start = toDate(startAt.value)
  if (!start) return
  const date = new Date(start.getTime() + minutes * 60000)
  endDate.value = formatDatePart(date)
  endClock.value = formatTimePart(date)
}

const goBack = () => {
  if (isEditMode.value) {
    router.push('/teacher/exams/list')
    return
  }
  const source = route.query.source
  if (source === 'manual') {
    router.push('/teacher/exams/manual')
    return
  }
  router.push('/teacher/exams/create')
}

const handleCreateAssignment = async () => {
  const duration = Math.max(5, Math.min(480, Number(timeLimit.value) || 60))
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
      durationMinutes: duration,
      startTime: startAt.value,
      endTime: endAt.value,
      isActive: true,
      monitorTabSwitch: route.query.monitorTabSwitch === 'true',
      monitorBlur: route.query.monitorBlur === 'true',
      monitorExitFullscreen: route.query.monitorExitFullscreen === 'true',
      monitorCopyPaste: route.query.monitorCopyPaste === 'true',
      monitorIdleTime: route.query.monitorIdleTime === 'true',
      monitorDevtools: route.query.monitorDevtools === 'true',
      monitorDuplicateIp: route.query.monitorDuplicateIp === 'true',
      monitorFastSubmit: route.query.monitorFastSubmit === 'true',
      monitorRightClick: route.query.monitorRightClick !== 'false',
      monitorPrintScreen: route.query.monitorPrintScreen !== 'false',
      monitorRapidQuestionSwitch: route.query.monitorRapidQuestionSwitch !== 'false',
      monitorMultiMonitor: route.query.monitorMultiMonitor !== 'false',
      requireCameraMic: route.query.requireCameraMic === 'true'
    })

    const assignPayload = {
      title: selectedExamTitle.value,
      openAt: startAt.value,
      closeAt: endAt.value,
      maxAttempts: 1,
      allowReviewAfterSubmit: true,
      isPublished: true
    }
    if (isEditMode.value) {
      const assignments = await listExamAssignments(examId.value)
      if (assignments?.length > 0) {
        await updateExamAssignment(examId.value, assignments[0].id, assignPayload)
      } else {
        await createExamAssignment(examId.value, assignPayload)
      }
    } else {
      await createExamAssignment(examId.value, assignPayload)
    }

    toast.success(isEditMode.value ? 'Cập nhật đợt thi thành công!' : 'Xuất bản đợt thi thành công!')
    await new Promise(resolve => setTimeout(resolve, 300))
    router.push({
      path: '/teacher/exams/created-success',
      query: {
        examId: examId.value,
        code: updatedExam?.code || '',
        title: selectedExamTitle.value,
        durationMinutes: duration,
        startAt: startAt.value,
        endAt: endAt.value
      }
    })
  } catch (error) {
    toast.error(isEditMode.value ? 'Không thể lưu thay đổi. Vui lòng thử lại.' : 'Không thể xuất bản đề thi. Vui lòng thử lại.')
  } finally {
    isSubmitting.value = false
  }
}

const loadExamForEdit = async () => {
  if (!examId.value || !isEditMode.value) return
  try {
    const exam = await getExamDetail(examId.value)
    if (!exam) return
    if (exam.durationMinutes) {
      timeLimit.value = Math.max(5, Math.min(480, exam.durationMinutes))
    }
    if (exam.startTime) {
      const start = new Date(exam.startTime)
      if (!Number.isNaN(start.getTime())) {
        startDate.value = formatDatePart(start)
        startClock.value = formatTimePart(start)
      }
    }
    if (exam.endTime) {
      const end = new Date(exam.endTime)
      if (!Number.isNaN(end.getTime())) {
        endDate.value = formatDatePart(end)
        endClock.value = formatTimePart(end)
      }
    }
  } catch {
    // ignore
  }
}

onMounted(loadExamForEdit)
</script>

<style scoped>
.ds-animate-fade-up {
  animation: fadeUp 0.5s ease-out;
}

.ds-animate-fade-up-delay {
  animation: fadeUp 0.65s ease-out;
}

@keyframes fadeUp {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
