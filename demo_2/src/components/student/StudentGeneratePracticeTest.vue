<template>
  <div :class="isDark ? 'dark' : 'light'" class="portal-viewport flex h-full min-h-0 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100">
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
        <StudentTopHeader active-section="practice" class="shrink-0" />

        <main class="teacher-page-shell student-stitch-main relative flex min-h-0 w-full max-w-screen-2xl flex-1 flex-col overflow-hidden">
          <div
            class="student-stitch-paper-bg portal-scrollbar flex min-h-0 flex-1 flex-col overflow-y-auto overscroll-contain px-3 pb-4 pt-2 sm:px-6 sm:pb-6 lg:px-10"
          >
            <nav class="mb-1 flex items-center gap-2 text-xs font-medium text-slate-500">
              <RouterLink to="/student/dashboard" class="transition hover:text-primary">Trang chủ</RouterLink>
              <span class="material-symbols-outlined text-[14px] text-slate-400">chevron_right</span>
              <span class="font-semibold text-primary">Luyện tập</span>
            </nav>

            <div class="mb-3 flex flex-wrap items-end justify-between gap-2">
              <h1 class="stitch-font-headline text-xl font-bold tracking-tight text-amber-900 dark:text-amber-100 md:text-2xl">
                Tạo bài luyện tập
              </h1>
              <span
                v-if="prefillSourceLabel"
                class="portal-chip inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-[11px] font-semibold text-primary"
              >
                <span class="material-symbols-outlined text-sm">track_changes</span>
                {{ prefillSourceLabel }}
              </span>
            </div>

            <div class="mb-3 grid grid-cols-1 gap-2 sm:grid-cols-3">
              <button
                v-for="preset in practicePresets"
                :key="preset.mode"
                type="button"
                class="rounded-xl border px-3 py-2.5 text-left text-sm font-bold transition-all"
                :class="
                  activePracticePlan.mode === preset.mode
                    ? 'border-primary bg-primary/10 text-slate-900 dark:text-slate-100'
                    : 'border-slate-200/80 bg-white/80 hover:border-primary/40 dark:border-slate-700 dark:bg-slate-800/50'
                "
                @click="applyPracticePreset(preset)"
              >
                {{ preset.title }}
              </button>
            </div>

            <div class="mb-3 flex flex-wrap gap-2 text-[11px] font-semibold">
              <span class="portal-chip rounded-full px-2.5 py-1 text-slate-700 dark:text-slate-200">{{ questionCountToImport }} câu</span>
              <span class="portal-chip rounded-full px-2.5 py-1 text-slate-700 dark:text-slate-200">{{ durationMinutes }} phút</span>
              <span class="portal-chip rounded-full px-2.5 py-1 text-slate-600 dark:text-slate-300">{{ activePracticePlan.focus }}</span>
            </div>

            <div class="relative flex min-h-0 flex-col gap-2 sm:gap-3">
            <div class="flex min-h-0 w-full max-w-screen-2xl flex-1 flex-col gap-2 overflow-x-hidden sm:gap-3">
              <div class="grid min-h-0 flex-1 grid-cols-1 gap-2 sm:gap-3 lg:grid-cols-2 lg:items-stretch lg:gap-4 lg:min-h-0">
                <section
                  class="portal-panel flex min-h-[10rem] flex-col overflow-hidden rounded-[1.5rem] border border-[#dbc2b0]/25 p-3 sm:p-4 lg:min-h-[min(28dvh,11rem)] lg:self-stretch"
                >
                  <div class="mb-2 flex shrink-0 items-center gap-2 border-b border-[#dbc2b0]/20 pb-2 dark:border-slate-600/50">
                    <span class="material-symbols-outlined shrink-0 text-base text-primary sm:text-lg">upload_file</span>
                    <h3 class="text-sm font-bold leading-snug sm:text-base">Tải tệp câu hỏi</h3>
                  </div>

                  <div class="flex min-h-0 flex-1 flex-col">
                    <label class="group relative flex min-h-0 w-full flex-1 cursor-pointer flex-col items-center justify-center overflow-hidden rounded-xl border-2 border-dashed border-slate-300 p-2 transition-all hover:border-primary/50 hover:bg-primary/5 sm:p-3">
                      <input class="absolute inset-0 h-full w-full cursor-pointer opacity-0" type="file" accept=".csv,.xlsx,.pdf,.docx" @change="onFileChange" />
                      <div class="mb-1.5 rounded-full bg-primary/10 p-2 transition-transform group-hover:scale-105 sm:p-2.5">
                        <span class="material-symbols-outlined text-2xl text-primary sm:text-3xl">cloud_upload</span>
                      </div>
                      <h4 class="mb-0.5 text-center text-xs font-semibold sm:text-sm">Chọn hoặc kéo thả tệp</h4>
                      <p class="px-1 text-center text-[10px] text-slate-500 sm:text-[11px]">CSV, XLSX, PDF, Word · tối đa 10MB</p>
                      <div class="mt-1.5 flex flex-wrap justify-center gap-2 text-[10px] sm:mt-2 sm:text-[11px]">
                        <a :href="getTemplateDownloadUrl('csv')" download class="flex items-center gap-0.5 font-semibold text-primary hover:underline">
                          <span class="material-symbols-outlined text-sm">download</span>
                          CSV
                        </a>
                        <a :href="getTemplateDownloadUrl('xlsx')" download class="flex items-center gap-0.5 font-semibold text-primary hover:underline">
                          <span class="material-symbols-outlined text-sm">download</span>
                          Excel
                        </a>
                      </div>
                      <p v-if="fileName" class="mt-2 max-w-full break-all px-1 text-center text-xs font-semibold leading-5 text-primary">{{ fileName }}</p>
                      <p v-if="selectedFile" class="text-[11px] text-slate-500">Dung lượng: {{ fileSizeLabel }}</p>
                    </label>
                  </div>
                </section>

                <section
                  class="portal-panel flex min-h-[10rem] flex-col overflow-hidden rounded-[1.5rem] border border-[#dbc2b0]/25 p-3 sm:p-4 lg:min-h-[min(28dvh,11rem)] lg:self-stretch"
                >
                  <div class="mb-2 flex shrink-0 items-center gap-2 border-b border-[#dbc2b0]/20 pb-2 dark:border-slate-600/50">
                    <span class="material-symbols-outlined shrink-0 text-base text-primary sm:text-lg">analytics</span>
                    <h3 class="text-sm font-bold leading-snug sm:text-base">Số câu trong tệp</h3>
                  </div>

                  <div class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-slate-100 bg-slate-50/90">
                    <div class="min-h-0 flex-1 overflow-y-auto overscroll-contain p-2.5 portal-scrollbar sm:p-3">
                      <div
                        v-if="!selectedFile"
                        class="flex h-full min-h-[5.5rem] flex-col items-center justify-center gap-1.5 px-2 text-center text-slate-400"
                      >
                        <span class="material-symbols-outlined text-2xl opacity-50 sm:text-3xl">summarize</span>
                        <p class="text-[11px] font-medium leading-snug text-slate-500 sm:text-xs">Chưa có tệp</p>
                        <p class="max-w-[14rem] text-[10px] leading-snug text-slate-400 sm:text-[11px]">Tải tệp bên trái để xem số câu hợp lệ.</p>
                      </div>

                      <div v-else-if="previewLoading" class="flex h-full min-h-[6rem] flex-col items-center justify-center gap-2">
                        <span class="material-symbols-outlined animate-pulse text-2xl text-primary sm:text-3xl">hourglass_top</span>
                        <p class="text-xs text-slate-600">Đang đọc tệp…</p>
                      </div>

                      <div v-else-if="previewError" class="flex min-h-[6rem] items-center justify-center px-1">
                        <p class="text-center text-xs text-red-600">{{ previewError }}</p>
                      </div>

                      <div v-else-if="totalParsed !== null && totalParsed > 0">
                        <div class="flex flex-col items-center text-center">
                          <p class="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Hợp lệ</p>
                          <p class="mt-0.5 text-2xl font-black tabular-nums leading-none tracking-tight text-primary sm:text-3xl">{{ totalParsed }}</p>
                        </div>
                        <div class="mt-2 border-t border-slate-200 pt-2">
                          <label class="block text-center text-[11px] font-semibold text-slate-700 sm:text-xs">Số câu đưa vào bài (1–{{ totalParsed }})</label>
                          <input
                            v-model.number="questionCountToImport"
                            type="number"
                            min="1"
                            :max="totalParsed"
                            class="mx-auto mt-1.5 block w-full max-w-[11rem] rounded-lg border border-slate-200 bg-white px-2.5 py-2 text-center text-sm font-bold tabular-nums outline-none transition-all focus:border-transparent focus:ring-2 focus:ring-primary sm:max-w-[12rem] sm:py-2.5 sm:text-base"
                          />
                        </div>
                      </div>

                      <div v-else-if="totalParsed === 0" class="flex min-h-[6rem] items-center justify-center">
                        <p class="text-center text-xs text-amber-700">Không có câu hợp lệ.</p>
                      </div>
                    </div>
                  </div>
                </section>
              </div>

              <section class="portal-panel shrink-0 rounded-[1.5rem] border border-[#dbc2b0]/25 p-3 sm:p-4">
                <div class="mb-2 flex items-center gap-2 border-b border-[#dbc2b0]/20 pb-2 dark:border-slate-600/50">
                  <span class="material-symbols-outlined shrink-0 text-base text-primary sm:text-lg">schedule</span>
                  <h3 class="text-sm font-bold leading-snug sm:text-base">Thời lượng làm bài</h3>
                </div>
                <div class="flex flex-wrap items-center gap-x-2 gap-y-2">
                  <button
                    v-for="opt in durationOptions"
                    :key="opt.value"
                    type="button"
                    :class="durationMinutes === opt.value
                      ? 'border-primary bg-primary text-white shadow-md'
                      : 'border-slate-200 bg-white hover:border-primary/50 hover:bg-primary/5'"
                    class="min-w-[4.75rem] rounded-xl border px-3 py-2 text-sm font-semibold transition-all sm:min-w-[5rem] sm:px-4 sm:py-2.5"
                    @click="durationMinutes = opt.value"
                  >
                    {{ opt.label }}
                  </button>
                  <span class="hidden h-5 w-px shrink-0 bg-slate-200 sm:block" aria-hidden="true"></span>
                  <span class="text-xs font-medium text-slate-500 shrink-0">Khác:</span>
                  <input
                    v-model.number="durationMinutes"
                    type="number"
                    min="5"
                    max="240"
                    class="w-20 shrink-0 rounded-lg border border-slate-200 bg-white px-2 py-2 text-center text-sm font-semibold tabular-nums outline-none focus:border-transparent focus:ring-2 focus:ring-primary sm:w-24 sm:px-3"
                  />
                  <span class="text-sm text-slate-500 shrink-0">phút</span>
                </div>
              </section>
            </div>

            <div
              class="flex w-full max-w-screen-2xl shrink-0 flex-col gap-2 border-t border-[#dbc2b0]/30 pt-3 sm:flex-row sm:items-center sm:justify-end sm:gap-3 sm:pt-3.5 dark:border-slate-600/50"
            >
              <div class="flex w-full flex-col-reverse gap-2 sm:w-auto sm:flex-row sm:justify-end sm:gap-3">
                <button
                  class="inline-flex min-h-[44px] w-full items-center justify-center rounded-xl border border-slate-200 bg-white px-5 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50 sm:w-auto sm:min-w-[9rem]"
                  type="button"
                  @click="goBack"
                >
                  Quay lại
                </button>
                <button
                  :disabled="isCreating"
                  class="silk-press-gradient inline-flex min-h-[44px] w-full items-center justify-center gap-2 rounded-xl px-6 text-sm font-bold text-white shadow-lg shadow-primary/20 transition-all hover:opacity-95 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto sm:min-w-[9rem]"
                  type="button"
                  @click="startPractice"
                >
                  <template v-if="!isCreating">
                    Tạo &amp; bắt đầu
                    <span class="material-symbols-outlined text-[20px]">arrow_forward</span>
                  </template>
                  <template v-else>Đang xử lý…</template>
                </button>
              </div>
            </div>
          </div>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { startAttempt } from '../../services/attemptService'
import { createPracticeFromFile } from '../../services/examService'
import { getTemplateDownloadUrl, previewImportFile } from '../../services/questionService'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useToast } from '../../composables/useToast'
import StudentTopHeader from './StudentTopHeader.vue'

const isDark = ref(false)

const practicePresets = [
  {
    mode: 'warmup',
    title: 'Khởi động',
    short: 'Ít câu, thời lượng ngắn để bắt nhịp.',
    description: 'Phù hợp khi bạn muốn vào guồng nhanh hoặc vừa quay lại sau một quãng nghỉ.',
    questionCount: 12,
    durationMinutes: 20,
    focus: 'Làm quen nhịp làm bài'
  },
  {
    mode: 'stabilize',
    title: 'Củng cố',
    short: 'Cân bằng giữa độ chính xác và tốc độ.',
    description: 'Một lượt luyện vừa sức để khóa lại kiến thức và ổn định phong độ trước khi tăng tải.',
    questionCount: 18,
    durationMinutes: 30,
    focus: 'Ổn định độ chính xác'
  },
  {
    mode: 'stretch',
    title: 'Bứt tốc',
    short: 'Tăng số câu để nâng độ bền làm bài.',
    description: 'Dành cho giai đoạn cần thử sức ở nhịp cao hơn mà vẫn giữ được độ chính xác.',
    questionCount: 24,
    durationMinutes: 35,
    focus: 'Mở rộng số câu và độ bền'
  }
]

const durationOptions = [
  { value: 15, label: '15 phút' },
  { value: 30, label: '30 phút' },
  { value: 45, label: '45 phút' },
  { value: 60, label: '60 phút' },
  { value: 90, label: '90 phút' },
  { value: 120, label: '2 giờ' }
]

const clampNumber = (value, min, max, fallback) => {
  const parsed = Number.parseInt(String(value ?? ''), 10)
  if (Number.isNaN(parsed)) return fallback
  return Math.min(max, Math.max(min, parsed))
}

const getPresetByMode = (mode) => practicePresets.find((preset) => preset.mode === mode) || practicePresets[1]

const router = useRouter()
const route = useRoute()
const initialPreset = getPresetByMode(String(route.query.mode || '').trim())
const selectedFile = ref(null)
const fileName = ref('')
const selectedPlanMode = ref(initialPreset.mode)
const customFocus = ref(String(route.query.focus || initialPreset.focus))
const durationMinutes = ref(clampNumber(route.query.durationMinutes, 5, 240, initialPreset.durationMinutes))
const isCreating = ref(false)
const toast = useToast()
const previewLoading = ref(false)
const totalParsed = ref(null)
const previewError = ref('')
const questionCountToImport = ref(clampNumber(route.query.questionCount, 1, 50, initialPreset.questionCount))

const activePracticePlan = computed(() => {
  const preset = getPresetByMode(selectedPlanMode.value)
  return {
    ...preset,
    focus: customFocus.value || preset.focus,
    questionCount: questionCountToImport.value,
    durationMinutes: durationMinutes.value
  }
})

const prefillSourceLabel = computed(() => {
  if (route.query.source === 'study-history') return 'Đề xuất từ lịch sử học'
  if (route.query.source === 'exam-result') return 'Đề xuất từ kết quả vừa xem'
  return route.query.source ? 'Đề xuất được nạp sẵn' : ''
})

const applyPracticePreset = (preset) => {
  selectedPlanMode.value = preset.mode
  durationMinutes.value = preset.durationMinutes
  questionCountToImport.value = preset.questionCount
  if (!route.query.focus) {
    customFocus.value = preset.focus
  }
}

const fileSizeLabel = computed(() => {
  if (!selectedFile.value) return ''
  const sizeInMb = selectedFile.value.size / (1024 * 1024)
  return `${sizeInMb.toFixed(2)} MB`
})

const onFileChange = async (e) => {
  const file = e?.target?.files?.[0] || null
  selectedFile.value = file
  fileName.value = file?.name || ''
  totalParsed.value = null
  previewError.value = ''
  questionCountToImport.value = activePracticePlan.value.questionCount

  if (!file) return

  const allowed = ['text/csv', 'application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', '']
  const ext = (file.name || '').toLowerCase().slice(-5)
  const validType = allowed.includes(file.type) || ext.endsWith('.csv') || ext.endsWith('.xlsx') || ext.endsWith('.pdf') || ext.endsWith('.docx')
  if (!validType) {
    toast.error('Định dạng tệp không hợp lệ. Vui lòng chọn CSV, XLSX, PDF hoặc Word.')
    selectedFile.value = null
    fileName.value = ''
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    toast.error('Dung lượng tệp vượt quá 10MB.')
    selectedFile.value = null
    fileName.value = ''
    return
  }

  previewLoading.value = true
  try {
    const r = await previewImportFile(file)
    const n = Number(r?.totalQuestions ?? 0)
    totalParsed.value = n
    if (n <= 0) {
      previewError.value = 'Không tìm thấy câu hỏi hợp lệ trong tệp.'
      toast.error(previewError.value)
    } else {
      questionCountToImport.value = Math.min(
        n <= 50 ? n : 50,
        Math.max(1, Number(questionCountToImport.value) || activePracticePlan.value.questionCount)
      )
    }
  } catch (err) {
    previewError.value = err?.message || 'Không đọc được tệp.'
    toast.error(previewError.value)
    selectedFile.value = null
    fileName.value = ''
  } finally {
    previewLoading.value = false
  }
}

const goBack = () => router.push('/student/dashboard')

const startPractice = async () => {
  if (!selectedFile.value) {
    toast.error('Vui lòng chọn tệp (CSV, XLSX, PDF hoặc Word) chứa câu hỏi.')
    return
  }
  if (totalParsed.value == null || totalParsed.value <= 0) {
    toast.error('Vui lòng chọn tệp có ít nhất một câu hỏi hợp lệ, hoặc đợi hệ thống đếm xong.')
    return
  }

  const k = Math.max(1, Math.min(totalParsed.value, Math.floor(Number(questionCountToImport.value) || 0)))
  if (k < 1 || k > totalParsed.value) {
    toast.error(`Số câu phải từ 1 đến ${totalParsed.value}.`)
    return
  }

  const duration = Math.max(5, Math.min(240, Number(durationMinutes.value) || 30))
  if (duration < 5 || duration > 240) {
    toast.error('Thời lượng phải từ 5 đến 240 phút.')
    return
  }

  isCreating.value = true
  try {
    const practiceExam = await createPracticeFromFile(selectedFile.value, duration, k)

    const attempt = await startAttempt(practiceExam.id)

    router.push({
      path: '/student/exam-interface',
      query: {
        exam: practiceExam.title || 'Bài luyện tập',
        examId: practiceExam.id,
        attemptId: attempt.attemptId,
        deadlineAt: attempt.deadlineAt || '',
        remainingSeconds: attempt.remainingSeconds || 0,
        startedAt: attempt.startedAt || '',
        isPractice: 'true'
      }
    })
  } catch (error) {
    toast.error(error?.message || 'Không thể tạo bài luyện tập lúc này.')
  } finally {
    isCreating.value = false
  }
}
</script>
