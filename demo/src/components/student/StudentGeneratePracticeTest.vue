<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen transition-colors duration-200">
    <div class="layout-container flex h-full grow flex-col">
      <StudentTopHeader />

      <main class="relative max-w-4xl mx-auto px-4 py-10 overflow-hidden w-full">
        <div class="relative mb-10 animate-fade-up">
          <h2 class="text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight">Tạo bài luyện tập mới</h2>
          <p class="mt-2 text-slate-600 dark:text-slate-400">Tải tệp CSV/XLSX chứa câu hỏi, chọn thời lượng và bắt đầu luyện tập.</p>
        </div>

        <div class="relative space-y-6 animate-fade-up-delay">
          <section class="bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200/80 dark:border-slate-700/80 shadow-soft">
            <h3 class="text-lg font-bold mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">upload_file</span>
              Tải tệp câu hỏi
            </h3>
            <label class="block border-2 border-dashed border-slate-300 dark:border-slate-600 rounded-2xl p-8 flex flex-col items-center justify-center transition-all duration-200 hover:border-primary/50 hover:bg-primary/5 cursor-pointer">
              <input class="hidden" type="file" accept=".csv,.xlsx,.pdf,.docx" @change="onFileChange" />
              <span class="material-symbols-outlined text-primary text-4xl mb-3">cloud_upload</span>
              <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Nhấp để chọn tệp CSV, XLSX, PDF hoặc Word</p>
              <p class="text-xs text-slate-500 dark:text-slate-400 mt-1">{{ FILE_FORMAT_DESC }}</p>
              <div class="mt-2 flex flex-wrap gap-3">
                <a :href="getTemplateDownloadUrl('csv')" download class="text-primary hover:underline text-sm font-semibold flex items-center gap-1">Mẫu CSV</a>
                <a :href="getTemplateDownloadUrl('xlsx')" download class="text-primary hover:underline text-sm font-semibold flex items-center gap-1">Mẫu Excel</a>
              </div>
              <p v-if="selectedFile" class="text-primary font-semibold mt-3">{{ selectedFile.name }}</p>
            </label>
          </section>

          <section class="bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200/80 dark:border-slate-700/80 shadow-soft">
            <h3 class="text-lg font-bold mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">schedule</span>
              Thời lượng làm bài
            </h3>
            <div>
                <p class="text-xs text-slate-500 dark:text-slate-400 mb-3">Chọn thời gian tối đa để hoàn thành bài. Hết giờ sẽ tự động nộp bài.</p>
                <div class="flex flex-wrap gap-2">
                  <button
                    v-for="opt in durationOptions"
                    :key="opt.value"
                    type="button"
                    :class="durationMinutes === opt.value
                      ? 'bg-primary text-white border-primary shadow-md'
                      : 'bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-600 hover:border-primary/50 hover:bg-primary/5 dark:hover:bg-primary/10'"
                    class="px-4 py-2.5 rounded-xl border font-semibold text-sm transition-all"
                    @click="durationMinutes = opt.value"
                  >
                    {{ opt.label }}
                  </button>
                </div>
              <div class="mt-3 flex items-center gap-2">
                <span class="text-sm text-slate-500 dark:text-slate-400">Hoặc nhập:</span>
                <input
                  v-model.number="durationMinutes"
                  type="number"
                  min="5"
                  max="240"
                  class="w-24 px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none text-center"
                />
                <span class="text-sm text-slate-500 dark:text-slate-400">phút</span>
              </div>
            </div>
          </section>

          <div class="flex items-center justify-end gap-4 pt-2">
            <button
              class="px-8 py-3 rounded-lg border border-slate-200 dark:border-slate-800 font-semibold hover:bg-slate-100 dark:hover:bg-slate-800 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200"
              type="button"
              @click="goBack"
            >
              Quay lại
            </button>
            <button
              :disabled="isCreating"
              class="px-10 py-3 rounded-lg bg-primary text-white font-bold shadow-lg shadow-primary/30 hover:bg-primary/90 hover:-translate-y-0.5 transition-all duration-200 flex items-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed disabled:hover:translate-y-0"
              type="button"
              @click="startPractice"
            >
              {{ isCreating ? 'Đang tạo...' : 'Tạo & bắt đầu' }}
              <span class="material-symbols-outlined text-lg">arrow_forward</span>
            </button>
          </div>
        </div>
      </main>

    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { startAttempt } from '../../services/attemptService'
import { createPracticeFromFile } from '../../services/examService'
import { FILE_FORMAT_DESC, getTemplateDownloadUrl } from '../../services/questionService'
import { useRouter } from 'vue-router'
import { useToast } from '../../composables/useToast'
import StudentTopHeader from './StudentTopHeader.vue'

const durationOptions = [
  { value: 15, label: '15 phút' },
  { value: 30, label: '30 phút' },
  { value: 45, label: '45 phút' },
  { value: 60, label: '60 phút' },
  { value: 90, label: '90 phút' },
  { value: 120, label: '2 giờ' }
]

const router = useRouter()
const isDark = ref(false)
const selectedFile = ref(null)
const durationMinutes = ref(30)
const isCreating = ref(false)
const toast = useToast()

const onFileChange = (e) => {
  const file = e?.target?.files?.[0] || null
  selectedFile.value = file

  if (!file) return

  const allowed = ['text/csv', 'application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', '']
  const ext = (file.name || '').toLowerCase().slice(-5)
  const validType = allowed.includes(file.type) || ext.endsWith('.csv') || ext.endsWith('.xlsx') || ext.endsWith('.pdf') || ext.endsWith('.docx')
  if (!validType) {
    toast.error('Định dạng tệp không hợp lệ. Vui lòng chọn CSV, XLSX, PDF hoặc Word.')
    selectedFile.value = null
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    toast.error('Dung lượng tệp vượt quá 10MB.')
    selectedFile.value = null
  }
}

const goBack = () => router.push('/student/dashboard')

const startPractice = async () => {
  if (!selectedFile.value) {
    toast.error('Vui lòng chọn tệp (CSV, XLSX, PDF hoặc Word) chứa câu hỏi.')
    return
  }

  const duration = Math.max(5, Math.min(240, Number(durationMinutes.value) || 30))
  if (duration < 5 || duration > 240) {
    toast.error('Thời lượng phải từ 5 đến 240 phút.')
    return
  }

  isCreating.value = true
  try {
    const practiceExam = await createPracticeFromFile(selectedFile.value, duration)

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

<style scoped>
.animate-fade-up { animation: fadeUp 0.5s ease-out; }
.animate-fade-up-delay { animation: fadeUp 0.65s ease-out; }
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
