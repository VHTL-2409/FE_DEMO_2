<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 transition-colors duration-200 min-h-screen">
    <TeacherTopHeader active-section="exam" />

    <main class="relative max-w-4xl mx-auto px-4 py-10 overflow-hidden">
      <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
      <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

      <div class="relative mb-6 animate-fade-up">
        <h2 class="text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight">Tạo đề thi mới (Nhập tệp)</h2>
        <p class="mt-2 text-slate-600 dark:text-slate-400 text-lg">Bước 1: Nhập tiêu đề và tải tệp. Bước 2: Cấu hình lịch thi.</p>
      </div>

      <div class="mb-10 animate-fade-up">
        <div class="flex flex-wrap items-center gap-3 text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
          <template v-for="(step, index) in steps" :key="step">
            <div class="flex items-center gap-3">
              <span
                class="size-7 rounded-full flex items-center justify-center text-[11px] font-bold"
                :class="index + 1 <= stepIndex ? 'bg-primary text-white' : 'bg-slate-200 text-slate-500 dark:bg-slate-800 dark:text-slate-400'"
              >
                {{ index + 1 }}
              </span>
              <span :class="index + 1 === stepIndex ? 'text-slate-900 dark:text-white' : ''">{{ step }}</span>
            </div>
            <span v-if="index < steps.length - 1" class="h-px w-6 bg-slate-200 dark:bg-slate-700"></span>
          </template>
        </div>
      </div>

      <div class="relative space-y-8 animate-fade-up-delay">
        <section v-if="stepIndex === 2" class="bg-white dark:bg-slate-900 p-8 rounded-2xl border border-slate-200/80 dark:border-slate-700/80 shadow-soft">
          <div class="flex items-center gap-2 mb-6">
            <span class="material-symbols-outlined text-primary">info</span>
            <h3 class="text-lg font-bold">Thông tin chung</h3>
          </div>
          <div class="space-y-2">
            <label class="text-sm font-semibold text-slate-700 dark:text-slate-300">Tiêu đề đề thi</label>
            <input v-model="examTitle" class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all" placeholder="VD: Kiểm tra giữa kỳ Giải tích nâng cao Q3" type="text" />
          </div>
        </section>

        <section v-if="stepIndex === 2" class="bg-white dark:bg-slate-900 p-8 rounded-2xl border border-slate-200/80 dark:border-slate-700/80 shadow-soft">
          <div class="flex items-center justify-between mb-6">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">upload_file</span>
              <h3 class="text-lg font-bold">Tải nội dung đề thi</h3>
            </div>
          </div>

          <label class="relative group border-2 border-dashed border-slate-300 dark:border-slate-600 rounded-2xl p-12 flex flex-col items-center justify-center transition-all duration-200 hover:border-primary/50 hover:bg-primary/5 cursor-pointer">
            <input class="absolute inset-0 w-full h-full opacity-0 cursor-pointer" type="file" accept=".csv,.xlsx,.pdf,.docx" @change="onFileChange" />
            <div class="bg-primary/10 p-4 rounded-full mb-4 group-hover:scale-110 transition-transform">
              <span class="material-symbols-outlined text-primary text-4xl">cloud_upload</span>
            </div>
            <h4 class="text-lg font-semibold mb-1">Nhấp để tải lên hoặc kéo thả</h4>
            <p class="text-slate-500 dark:text-slate-400 text-sm">{{ FILE_FORMAT_DESC }}</p>
            <div class="mt-2 flex flex-wrap gap-3">
              <a :href="getTemplateDownloadUrl('csv')" download class="text-primary hover:underline text-sm font-semibold flex items-center gap-1">
                <span class="material-symbols-outlined text-lg">download</span>
                Mẫu CSV
              </a>
              <a :href="getTemplateDownloadUrl('xlsx')" download class="text-primary hover:underline text-sm font-semibold flex items-center gap-1">
                <span class="material-symbols-outlined text-lg">download</span>
                Mẫu Excel
              </a>
            </div>
            <p v-if="fileName" class="text-primary text-sm font-semibold mt-3">{{ fileName }}</p>
            <p v-if="selectedFile" class="text-xs text-slate-500 mt-1">Dung lượng: {{ fileSizeLabel }}</p>
          </label>
        </section>

        <section v-if="stepIndex === 3" class="bg-white dark:bg-slate-900 p-8 rounded-2xl border border-slate-200/80 dark:border-slate-700/80 shadow-soft">
          <div class="flex items-center gap-2 mb-6">
            <span class="material-symbols-outlined text-primary">shield</span>
            <h3 class="text-lg font-bold">Cấu hình giám sát</h3>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 dark:border-slate-800 px-4 py-3">
              <div>
                <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Theo dõi chuyển tab</p>
                <p class="text-xs text-slate-500">Phát hiện đổi tab hoặc ẩn cửa sổ</p>
              </div>
              <input v-model="monitorTabSwitch" type="checkbox" class="h-5 w-5 accent-primary" />
            </label>

            <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 dark:border-slate-800 px-4 py-3">
              <div>
                <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Theo dõi blur</p>
                <p class="text-xs text-slate-500">Ghi nhận khi mất focus</p>
              </div>
              <input v-model="monitorBlur" type="checkbox" class="h-5 w-5 accent-primary" />
            </label>

            <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 dark:border-slate-800 px-4 py-3">
              <div>
                <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Thoát fullscreen</p>
                <p class="text-xs text-slate-500">Cảnh báo khi rời chế độ toàn màn hình</p>
              </div>
              <input v-model="monitorExitFullscreen" type="checkbox" class="h-5 w-5 accent-primary" />
            </label>

            <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 dark:border-slate-800 px-4 py-3">
              <div>
                <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Copy/Paste</p>
                <p class="text-xs text-slate-500">Ghi nhận thao tác sao chép/dán</p>
              </div>
              <input v-model="monitorCopyPaste" type="checkbox" class="h-5 w-5 accent-primary" />
            </label>

            <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 dark:border-slate-800 px-4 py-3">
              <div>
                <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Nhàn rỗi</p>
                <p class="text-xs text-slate-500">Cảnh báo khi không thao tác</p>
              </div>
              <input v-model="monitorIdleTime" type="checkbox" class="h-5 w-5 accent-primary" />
            </label>

            <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 dark:border-slate-800 px-4 py-3">
              <div>
                <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">DevTools</p>
                <p class="text-xs text-slate-500">Phát hiện mở DevTools</p>
              </div>
              <input v-model="monitorDevtools" type="checkbox" class="h-5 w-5 accent-primary" />
            </label>

            <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 dark:border-slate-800 px-4 py-3">
              <div>
                <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Duplicate IP</p>
                <p class="text-xs text-slate-500">Phát hiện IP trùng</p>
              </div>
              <input v-model="monitorDuplicateIp" type="checkbox" class="h-5 w-5 accent-primary" />
            </label>

            <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 dark:border-slate-800 px-4 py-3">
              <div>
                <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Nộp nhanh</p>
                <p class="text-xs text-slate-500">Cảnh báo nộp bài quá nhanh</p>
              </div>
              <input v-model="monitorFastSubmit" type="checkbox" class="h-5 w-5 accent-primary" />
            </label>

            <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 dark:border-slate-800 px-4 py-3">
              <div>
                <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Chuột phải</p>
                <p class="text-xs text-slate-500">Chặn menu chuột phải (copy)</p>
              </div>
              <input v-model="monitorRightClick" type="checkbox" class="h-5 w-5 accent-primary" />
            </label>

            <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 dark:border-slate-800 px-4 py-3">
              <div>
                <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Print Screen</p>
                <p class="text-xs text-slate-500">Phát hiện chụp màn hình</p>
              </div>
              <input v-model="monitorPrintScreen" type="checkbox" class="h-5 w-5 accent-primary" />
            </label>

            <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 dark:border-slate-800 px-4 py-3">
              <div>
                <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Đổi câu nhanh</p>
                <p class="text-xs text-slate-500">Cảnh báo đổi câu liên tục</p>
              </div>
              <input v-model="monitorRapidQuestionSwitch" type="checkbox" class="h-5 w-5 accent-primary" />
            </label>

            <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 dark:border-slate-800 px-4 py-3">
              <div>
                <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Đa màn hình</p>
                <p class="text-xs text-slate-500">Phát hiện nhiều màn hình</p>
              </div>
              <input v-model="monitorMultiMonitor" type="checkbox" class="h-5 w-5 accent-primary" />
            </label>

            <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 dark:border-slate-800 px-4 py-3">
              <div>
                <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Yêu cầu camera/mic</p>
                <p class="text-xs text-slate-500">Bắt buộc bật camera và micro</p>
              </div>
              <input v-model="requireCameraMic" type="checkbox" class="h-5 w-5 accent-primary" />
            </label>
          </div>
        </section>

        <div class="flex items-center justify-between gap-4 pt-6">
          <div class="text-xs text-slate-500 dark:text-slate-400">{{ FILE_FORMAT_DESC }}</div>
          <div class="flex items-center gap-4">
            <button class="px-8 py-3 rounded-lg border border-slate-200 dark:border-slate-800 font-semibold hover:bg-slate-100 dark:hover:bg-slate-800 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200" type="button" @click="goBack">Hủy bản nháp</button>
            <button v-if="stepIndex === 2" :disabled="isSubmitting" class="px-10 py-3 rounded-lg bg-primary text-white font-bold shadow-lg shadow-primary/30 hover:bg-primary/90 hover:-translate-y-0.5 transition-all duration-200 flex items-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed disabled:hover:translate-y-0" type="button" @click="goNext">
              {{ isSubmitting ? 'Đang xử lý...' : 'Tiếp theo' }}
              <span class="material-symbols-outlined text-lg">arrow_forward</span>
            </button>
            <button v-else :disabled="isSubmitting" class="px-10 py-3 rounded-lg bg-primary text-white font-bold shadow-lg shadow-primary/30 hover:bg-primary/90 hover:-translate-y-0.5 transition-all duration-200 flex items-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed disabled:hover:translate-y-0" type="button" @click="confirmMonitoring">
              {{ isSubmitting ? 'Đang lưu...' : 'Tiếp tục lập lịch' }}
              <span class="material-symbols-outlined text-lg">arrow_forward</span>
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { createExam, updateExam } from '../../services/examService'
import { FILE_FORMAT_DESC, getTemplateDownloadUrl, importQuestionsFromFile } from '../../services/questionService'
import { useToast } from '../../composables/useToast'
import { useRouter } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const router = useRouter()
const isDark = ref(false)
const steps = ['Chọn cách tạo', 'Nhập đề', 'Giám sát', 'Lập lịch', 'Hoàn tất']
const stepIndex = ref(2)
const examTitle = ref('')
const fileName = ref('')
const selectedFile = ref(null)
const isSubmitting = ref(false)
const createdExamId = ref(null)
const createdExamTitle = ref('')
const monitorTabSwitch = ref(true)
const monitorBlur = ref(true)
const monitorExitFullscreen = ref(true)
const monitorCopyPaste = ref(true)
const monitorIdleTime = ref(true)
const monitorDevtools = ref(true)
const monitorDuplicateIp = ref(true)
const monitorFastSubmit = ref(true)
const monitorRightClick = ref(true)
const monitorPrintScreen = ref(true)
const monitorRapidQuestionSwitch = ref(true)
const monitorMultiMonitor = ref(true)
const requireCameraMic = ref(true)

const toast = useToast()

const fileSizeLabel = computed(() => {
  if (!selectedFile.value) return ''
  const sizeInMb = selectedFile.value.size / (1024 * 1024)
  return `${sizeInMb.toFixed(2)} MB`
})

const onFileChange = (event) => {
  const file = event.target.files?.[0] || null
  selectedFile.value = file
  fileName.value = file?.name || ''

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
  }
}

const goBack = () => {
  router.push('/teacher/exams')
}

const goNext = async () => {
  if (!examTitle.value.trim()) {
    toast.error('Vui lòng nhập tiêu đề đề thi.')
    return
  }

  if (!selectedFile.value) {
    toast.error('Vui lòng chọn tệp (CSV, XLSX, PDF hoặc Word) trước khi tiếp tục.')
    return
  }

  isSubmitting.value = true
  try {
    const createdExam = await createExam({
      title: examTitle.value.trim(),
      description: '',
      durationMinutes: 60,
      isActive: false,
      monitorTabSwitch: monitorTabSwitch.value,
      monitorBlur: monitorBlur.value,
      monitorExitFullscreen: monitorExitFullscreen.value,
      monitorCopyPaste: monitorCopyPaste.value,
      monitorIdleTime: monitorIdleTime.value,
      monitorDevtools: monitorDevtools.value,
      monitorDuplicateIp: monitorDuplicateIp.value,
      monitorFastSubmit: monitorFastSubmit.value,
      monitorRightClick: monitorRightClick.value,
      monitorPrintScreen: monitorPrintScreen.value,
      monitorRapidQuestionSwitch: monitorRapidQuestionSwitch.value,
      monitorMultiMonitor: monitorMultiMonitor.value,
      requireCameraMic: requireCameraMic.value
    })

    await importQuestionsFromFile(createdExam.id, selectedFile.value)

    createdExamId.value = createdExam.id
    createdExamTitle.value = createdExam.title || examTitle.value.trim()
    stepIndex.value = 3
  } catch (error) {
    toast.error('Không thể tạo đề thi từ tệp. Vui lòng thử lại.')
  } finally {
    isSubmitting.value = false
  }
}

const confirmMonitoring = async () => {
  if (!createdExamId.value) {
    toast.error('Không tìm thấy đề thi vừa tạo.')
    return
  }

  isSubmitting.value = true
  try {
    await updateExam(createdExamId.value, {
      title: createdExamTitle.value || examTitle.value.trim(),
      description: '',
      durationMinutes: 60,
      isActive: false,
      monitorTabSwitch: monitorTabSwitch.value,
      monitorBlur: monitorBlur.value,
      monitorExitFullscreen: monitorExitFullscreen.value,
      monitorCopyPaste: monitorCopyPaste.value,
      monitorIdleTime: monitorIdleTime.value,
      monitorDevtools: monitorDevtools.value,
      monitorDuplicateIp: monitorDuplicateIp.value,
      monitorFastSubmit: monitorFastSubmit.value,
      monitorRightClick: monitorRightClick.value,
      monitorPrintScreen: monitorPrintScreen.value,
      monitorRapidQuestionSwitch: monitorRapidQuestionSwitch.value,
      monitorMultiMonitor: monitorMultiMonitor.value,
      requireCameraMic: requireCameraMic.value
    })

    router.push({
      path: '/teacher/exams/schedule',
      query: {
        examId: createdExamId.value,
        title: createdExamTitle.value || examTitle.value.trim(),
        source: 'import',
        monitorTabSwitch: String(monitorTabSwitch.value),
        monitorBlur: String(monitorBlur.value),
        monitorExitFullscreen: String(monitorExitFullscreen.value),
        monitorCopyPaste: String(monitorCopyPaste.value),
        monitorIdleTime: String(monitorIdleTime.value),
        monitorDevtools: String(monitorDevtools.value),
        monitorDuplicateIp: String(monitorDuplicateIp.value),
        monitorFastSubmit: String(monitorFastSubmit.value),
        monitorRightClick: String(monitorRightClick.value),
        monitorPrintScreen: String(monitorPrintScreen.value),
        monitorRapidQuestionSwitch: String(monitorRapidQuestionSwitch.value),
        monitorMultiMonitor: String(monitorMultiMonitor.value),
        requireCameraMic: String(requireCameraMic.value)
      }
    })
  } catch (error) {
    toast.error('Không thể lưu cấu hình giám sát. Vui lòng thử lại.')
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