<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <div class="layout-container flex h-full grow flex-col">
      <TeacherTopHeader active-section="exam" />

      <main class="relative flex-1 max-w-7xl mx-auto w-full p-4 md:p-8 overflow-hidden">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

        <div class="relative mb-6 animate-fade-up">
          <h1 class="text-3xl font-bold tracking-tight">Nhập câu hỏi thủ công</h1>
          <p class="text-slate-500 mt-1">Cấu hình từng câu hỏi cho đề thi của bạn.</p>
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

        <section class="bg-white dark:bg-slate-900 rounded-xl border border-primary/10 shadow-sm overflow-hidden animate-fade-up-delay hover:-translate-y-0.5 hover:shadow-md transition-all duration-200">
          <div class="p-6 border-b border-primary/10">
            <h2 class="text-lg font-bold">Trình soạn câu hỏi</h2>
          </div>
          <div class="p-6 space-y-6">
            <div class="space-y-2">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Tiêu đề đề thi</label>
              <input v-model="examTitle" class="w-full bg-slate-50 dark:bg-slate-800 border-primary/10 rounded-lg text-sm focus:ring-primary focus:border-primary" placeholder="VD: Vật lý giữa kỳ" type="text" />
            </div>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div class="space-y-2">
                <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Loại câu hỏi</label>
                <select class="w-full bg-slate-50 dark:bg-slate-800 border-primary/10 rounded-lg text-sm focus:ring-primary focus:border-primary">
                  <option>Trắc nghiệm</option>
                </select>
              </div>
              <div class="space-y-2">
                <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Điểm</label>
                <input v-model.number="scoreWeight" class="w-full bg-slate-50 dark:bg-slate-800 border-primary/10 rounded-lg text-sm focus:ring-primary focus:border-primary" type="number" min="0.5" step="0.5" />
              </div>
              <div class="space-y-2">
                <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Tiến độ</label>
                <div class="h-full rounded-lg border border-primary/10 bg-slate-50 dark:bg-slate-800 p-4 text-sm">
                  <p class="font-semibold text-slate-700 dark:text-slate-200">Đã lưu: {{ savedQuestions }} câu</p>
                  <p class="text-xs text-slate-500 mt-1">Tạo xong có thể chuyển sang bước lập lịch.</p>
                </div>
              </div>
            </div>

            <div class="space-y-2">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Nội dung câu hỏi</label>
              <textarea v-model="questionContent" class="w-full p-4 text-sm bg-white dark:bg-slate-900 border border-primary/10 rounded-lg resize-none" placeholder="Nhập câu hỏi của bạn tại đây..." rows="4"></textarea>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div class="space-y-2" v-for="option in optionFields" :key="option.id">
                <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Lựa chọn {{ option.id }}</label>
                <input v-model="option.text" class="w-full bg-slate-50 dark:bg-slate-800 border-primary/10 rounded-lg text-sm focus:ring-primary focus:border-primary" type="text" :placeholder="`Lựa chọn ${option.id}`" />
              </div>
            </div>

            <div class="space-y-2">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Đáp án đúng</label>
              <select v-model="correctAnswer" class="w-full md:w-60 bg-slate-50 dark:bg-slate-800 border-primary/10 rounded-lg text-sm focus:ring-primary focus:border-primary">
                <option value="A">A</option>
                <option value="B">B</option>
                <option value="C">C</option>
                <option value="D">D</option>
              </select>
            </div>
          </div>
          <div class="px-6 py-4 bg-slate-50 dark:bg-slate-800/50 border-t border-primary/10 flex justify-end gap-3">
            <button :disabled="isSubmitting" class="px-4 py-2 rounded-lg text-primary font-bold text-sm hover:bg-primary/5 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200 disabled:opacity-70" type="button" @click="saveQuestion">{{ isSubmitting ? 'Đang lưu...' : 'Lưu câu hỏi' }}</button>
            <button class="px-6 py-2 rounded-lg bg-primary text-white font-bold text-sm shadow-md shadow-primary/20 hover:bg-primary/90 hover:-translate-y-0.5 transition-all duration-200" type="button" @click="goNext">
              Tiếp theo
            </button>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { createExam } from '../../services/examService'
import { createQuestion } from '../../services/questionService'
import { useToast } from '../../composables/useToast'
import { useRouter } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const router = useRouter()
const isDark = ref(false)
const steps = ['Chọn cách tạo', 'Nhập đề', 'Lập lịch', 'Hoàn tất']
const currentStep = 2
const examTitle = ref('')
const scoreWeight = ref(1)
const questionContent = ref('')
const correctAnswer = ref('A')
const optionFields = ref([
  { id: 'A', text: '' },
  { id: 'B', text: '' },
  { id: 'C', text: '' },
  { id: 'D', text: '' }
])
const isSubmitting = ref(false)
const savedQuestions = ref(0)

const toast = useToast()
let createdExamId = null

const saveQuestion = async () => {
  if (!examTitle.value.trim()) {
    toast.error('Vui lòng nhập tiêu đề đề thi.')
    return
  }

  if (!questionContent.value.trim()) {
    toast.error('Vui lòng nhập nội dung câu hỏi.')
    return
  }

  const hasEmptyOption = optionFields.value.some((option) => !option.text.trim())
  if (hasEmptyOption) {
    toast.error('Vui lòng điền đầy đủ các phương án trả lời (A, B, C, D).')
    return
  }

  isSubmitting.value = true
  try {
    if (!createdExamId) {
      const exam = await createExam({
        title: examTitle.value.trim(),
        description: '',
        durationMinutes: 60,
        isActive: false
      })
      createdExamId = exam.id
    }

    await createQuestion(createdExamId, {
      content: questionContent.value.trim(),
      scoreWeight: scoreWeight.value,
      options: optionFields.value.map((option) => ({ id: option.id, text: option.text.trim() })),
      correctAnswer: correctAnswer.value
    })

    toast.success('Lưu câu hỏi thành công.')
    savedQuestions.value += 1
    questionContent.value = ''
    optionFields.value = optionFields.value.map((option) => ({ ...option, text: '' }))
    correctAnswer.value = 'A'
  } catch (error) {
    toast.error('Không thể lưu câu hỏi. Vui lòng thử lại.')
  } finally {
    isSubmitting.value = false
  }
}

const goNext = async () => {
  if (!examTitle.value.trim()) {
    toast.error('Vui lòng nhập tiêu đề đề thi trước khi tiếp tục.')
    return
  }

  if (savedQuestions.value === 0 && !questionContent.value.trim()) {
    toast.error('Vui lòng lưu ít nhất 1 câu hỏi trước khi tiếp tục.')
    return
  }

  isSubmitting.value = true
  try {
    if (!createdExamId) {
      const exam = await createExam({
        title: examTitle.value.trim(),
        description: '',
        durationMinutes: 60,
        isActive: false
      })
      createdExamId = exam.id
    }

    router.push({
      path: '/teacher/exams/schedule',
      query: {
        examId: createdExamId,
        title: examTitle.value || 'Đề thi thủ công',
        source: 'manual'
      }
    })
  } catch (error) {
    toast.error('Không thể tạo đề thi. Vui lòng thử lại.')
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