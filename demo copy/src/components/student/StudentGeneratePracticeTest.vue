<template>
  <div class="portal-viewport flex h-full min-h-0 flex-col bg-background-light font-display text-slate-900">
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
        <main class="teacher-page-shell relative flex min-h-0 w-full flex-1 flex-col overflow-hidden px-3 py-2 sm:px-4 sm:py-2.5">
          <div class="staff-page-wrap min-h-0 flex-1 overflow-x-hidden">
            <PageHeader
              class="shrink-0 animate-fade-up [&_.teacher-page-title]:text-[clamp(1.05rem,2.2vw,1.5rem)]"
              title="Tạo bài luyện tập"
            />

            <div class="relative flex min-h-0 flex-1 flex-col gap-2 animate-fade-up-delay sm:gap-3">
              <div class="mx-auto flex min-h-0 w-full max-w-6xl flex-1 flex-col gap-2 overflow-x-hidden sm:gap-3">
              <div class="grid min-h-0 flex-1 grid-cols-1 gap-2 sm:gap-3 lg:grid-cols-2 lg:items-stretch lg:gap-4 lg:min-h-0">
                <section class="staff-surface relative flex min-h-[12rem] flex-col overflow-hidden rounded-2xl p-3 sm:p-4 lg:min-h-[min(32dvh,13rem)] lg:self-stretch">
                  <div class="mb-2 flex shrink-0 items-center gap-2 border-b border-slate-100 pb-2">
                    <LucideIcon name="upload_file" size="16" />
                    <h3 class="text-sm font-bold leading-snug sm:text-base">Tải tệp câu hỏi</h3>
                  </div>

                  <div class="flex min-h-0 flex-1 flex-col">
                    <label class="group relative flex min-h-0 w-full flex-1 cursor-pointer flex-col items-center justify-center overflow-hidden rounded-xl border-2 border-dashed border-slate-300 p-2 transition-all hover:border-primary/50 hover:bg-primary/5 sm:p-3">
                      <input class="absolute inset-0 h-full w-full cursor-pointer opacity-0" type="file" accept=".csv,.xlsx,.pdf,.docx,.json,.md,.markdown" @change="onFileChange" />
                      <div class="mb-1.5 rounded-full bg-gradient-to-br from-primary/20 to-indigo-500/20 p-2 transition-all group-hover:scale-110 group-hover:shadow-lg sm:p-2.5">
                        <LucideIcon name="cloud_upload" size="24" class="text-primary" />
                      </div>
                      <h4 class="mb-0.5 text-center text-xs font-semibold sm:text-sm">Chọn hoặc kéo thả tệp</h4>
                      <div class="mt-1.5 flex flex-wrap justify-center gap-2 text-[10px] sm:mt-2 sm:text-[11px]">
                        <a :href="getTemplateDownloadUrl('csv')" download class="flex items-center gap-0.5 font-semibold text-primary hover:underline">
                          <LucideIcon name="download" size="14" />
                          CSV
                        </a>
                        <a :href="getTemplateDownloadUrl('xlsx')" download class="flex items-center gap-0.5 font-semibold text-primary hover:underline">
                          <LucideIcon name="download" size="14" />
                          Excel
                        </a>
                      </div>
                      <Transition name="fade">
                        <div v-if="fileName" class="mt-2 max-w-full truncate px-1 text-center text-xs font-semibold text-primary">
                          {{ fileName }}
                        </div>
                      </Transition>
                      <p v-if="selectedFile" class="text-[11px] text-slate-500">Dung lượng: {{ fileSizeLabel }}</p>
                    </label>
                  </div>
                </section>

                <section class="staff-surface flex min-h-[12rem] flex-col overflow-hidden rounded-2xl p-3 sm:p-4 lg:min-h-[min(32dvh,13rem)] lg:self-stretch">
                  <div class="mb-2 flex shrink-0 items-center gap-2 border-b border-slate-100 pb-2">
                    <LucideIcon name="analytics" size="16" />
                    <h3 class="text-sm font-bold leading-snug sm:text-base">Số câu trong tệp</h3>
                  </div>

                  <div class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-slate-100 bg-slate-50/90">
                    <div class="min-h-0 flex-1 overflow-y-auto overscroll-contain p-2.5 portal-scrollbar sm:p-3">
                      <div
                        v-if="!selectedFile"
                        class="flex h-full min-h-[5.5rem] flex-col items-center justify-center gap-1.5 px-2 text-center text-slate-400"
                      >
                        <LucideIcon name="summarize" size="24" />
                        <p class="text-[11px] font-medium leading-snug text-slate-500 sm:text-xs">Chưa có tệp</p>
                      </div>

                      <div v-else-if="previewLoading" class="flex h-full min-h-[6rem] flex-col items-center justify-center gap-2">
                        <LucideIcon name="hourglass_top" size="24" />
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

              <section class="staff-surface shrink-0 rounded-2xl p-3 sm:p-4">
                <div class="mb-2 flex items-center gap-2 border-b border-slate-100 pb-2">
                  <LucideIcon name="schedule" size="16" />
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

            <div class="mx-auto flex w-full max-w-6xl shrink-0 flex-col gap-2 border-t border-slate-200/80 pt-3 sm:flex-row sm:items-center sm:justify-end sm:gap-3 sm:pt-3.5">
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
                  class="inline-flex min-h-[44px] w-full items-center justify-center gap-2 rounded-xl bg-primary px-6 text-sm font-bold text-white shadow-md shadow-primary/25 transition-all hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto sm:min-w-[9rem]"
                  type="button"
                  @click="startPractice"
                >
                  <template v-if="!isCreating">
                    Tạo &amp; bắt đầu
                    <LucideIcon name="arrow_forward" />
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
import { useRouter } from 'vue-router'
import { useToast } from '../../composables/useToast'
import { buildAttemptQuery } from '../../services/studentExamContextStorage'
import PageHeader from '../ui/PageHeader.vue'

const durationOptions = [
  { value: 15, label: '15 phút' },
  { value: 30, label: '30 phút' },
  { value: 45, label: '45 phút' },
  { value: 60, label: '60 phút' },
  { value: 90, label: '90 phút' },
  { value: 120, label: '2 giờ' }
]

const router = useRouter()
const selectedFile = ref(null)
const fileName = ref('')
const durationMinutes = ref(30)
const isCreating = ref(false)
const toast = useToast()
const previewLoading = ref(false)
const totalParsed = ref(null)
const previewError = ref('')
const questionCountToImport = ref(1)

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
  questionCountToImport.value = 1

  if (!file) return

  const allowed = ['text/csv', 'application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'application/msword', '']
  const ext = (file.name || '').toLowerCase().replace(/^.*\./, '.')
  const validExts = ['.csv', '.xlsx', '.pdf', '.docx', '.xls', '.doc']
  const validType = allowed.includes(file.type) || validExts.includes(ext)
  if (!validType) {
    toast.error('Dinh dang tep khong hop le. Vui long chon CSV, XLSX, PDF hoac Word.')
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
    const n = Number(r?.totalQuestions ?? r?.count ?? r?.questions?.length ?? r?.questionCount ?? 0)
    totalParsed.value = n
    if (n <= 0) {
      previewError.value = 'Không tìm thấy câu hỏi hợp lệ trong tệp.'
    } else {
      questionCountToImport.value = n <= 50 ? n : 50
    }
  } catch (err) {
    previewError.value = 'Lỗi đọc tệp. Vui lòng kiểm tra định dạng và thử lại.'
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
      query: buildAttemptQuery({
        examTitle: practiceExam.title || 'Bài luyện tập',
        examId: practiceExam.id,
        attemptId: attempt.attemptId,
        deadlineAt: attempt.deadlineAt || '',
        remainingSeconds: attempt.remainingSeconds || 0,
        startedAt: attempt.startedAt || '',
        isPractice: true
      })
    })
  } catch (error) {
    toast.error(error?.message || 'Không thể tạo bài luyện tập lúc này.')
  } finally {
    isCreating.value = false
  }
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
