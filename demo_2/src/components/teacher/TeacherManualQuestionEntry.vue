<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto max-w-5xl px-4 pb-10 pt-4 sm:px-6 lg:px-8">

      <!-- Breadcrumb -->
      <div class="mb-4 ds-animate-fade-up">
        <div class="flex items-center gap-2 text-sm" style="color: var(--ds-text-muted)">
          <RouterLink
            class="flex items-center gap-1 transition-colors hover:text-[var(--ds-primary)]"
            style="color: var(--ds-text-muted)"
            to="/teacher/exams"
          >
            <LucideIcon name="assignment" size="16" />
            Đề thi
          </RouterLink>
          <LucideIcon name="chevron_right" size="12" />
          <RouterLink
            class="flex items-center gap-1 transition-colors hover:text-[var(--ds-primary)]"
            style="color: var(--ds-text-muted)"
            :to="{ path: '/teacher/exams/create', query: { examId } }"
          >
            Tạo đề thi
          </RouterLink>
          <LucideIcon name="chevron_right" size="12" />
          <span class="font-medium" style="color: var(--ds-text)">Nhập câu hỏi</span>
        </div>
      </div>

      <!-- Page Header -->
      <div class="mb-6 ds-animate-fade-up" style="animation-delay: 0.05s">
        <PageHeader
          eyebrow="Nhập câu hỏi"
          title="Thêm câu hỏi thủ công"
          :subtitle="'Nhập nội dung câu hỏi cho đề: ' + examTitle"
        />
      </div>

      <!-- Exam Info Card -->
      <div class="mb-6 ds-animate-fade-up" style="animation-delay: 0.1s">
        <DsCard padding="md" variant="accent" accent-color="primary">
          <div class="flex items-center gap-4">
            <div
              class="flex size-12 items-center justify-center rounded-lg"
              style="background-color: var(--ds-primary-soft); color: var(--ds-primary)"
            >
              <LucideIcon name="quiz" size="24" />
            </div>
            <div class="flex-1">
              <h3 class="text-lg font-bold" style="color: var(--ds-text)">{{ examTitle }}</h3>
              <p class="text-sm" style="color: var(--ds-text-muted)">
                {{ questions.length }} câu hỏi đã thêm
              </p>
            </div>
            <StatusChip status="info" :label="'Bản nháp'" />
          </div>
        </DsCard>
      </div>

      <!-- Question Form -->
      <div class="mb-6 ds-animate-fade-up" style="animation-delay: 0.15s">
        <DsCard padding="lg">
          <FormSection
            title="Nội dung câu hỏi"
            description="Nhập thông tin câu hỏi và đáp án"
          >
            <!-- Question Content -->
            <div>
              <label class="mb-1.5 block text-sm font-semibold" style="color: var(--ds-text)">
                Nội dung câu hỏi <span style="color: var(--ds-danger)">*</span>
              </label>
              <textarea
                v-model="newQuestion.content"
                rows="3"
                class="ds-input w-full rounded-lg border px-4 py-2.5 text-sm"
                style="border-color: var(--ds-border); background-color: var(--ds-surface); color: var(--ds-text); resize: vertical"
                placeholder="Nhập nội dung câu hỏi..."
              />
            </div>

            <!-- Options A, B, C, D -->
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label class="mb-1.5 flex items-center gap-2 text-sm font-semibold" style="color: var(--ds-text)">
                  <span
                    class="flex size-6 items-center justify-center rounded text-xs font-bold"
                    style="background-color: var(--ds-primary); color: white"
                  >A</span>
                  Đáp án A <span style="color: var(--ds-danger)">*</span>
                </label>
                <input
                  v-model="newQuestion.optionA"
                  type="text"
                  class="ds-input w-full rounded-lg border px-4 py-2.5 text-sm"
                  style="border-color: var(--ds-border); background-color: var(--ds-surface); color: var(--ds-text)"
                  placeholder="Nhập đáp án A..."
                />
              </div>
              <div>
                <label class="mb-1.5 flex items-center gap-2 text-sm font-semibold" style="color: var(--ds-text)">
                  <span
                    class="flex size-6 items-center justify-center rounded text-xs font-bold"
                    style="background-color: var(--ds-primary); color: white"
                  >B</span>
                  Đáp án B <span style="color: var(--ds-danger)">*</span>
                </label>
                <input
                  v-model="newQuestion.optionB"
                  type="text"
                  class="ds-input w-full rounded-lg border px-4 py-2.5 text-sm"
                  style="border-color: var(--ds-border); background-color: var(--ds-surface); color: var(--ds-text)"
                  placeholder="Nhập đáp án B..."
                />
              </div>
              <div>
                <label class="mb-1.5 flex items-center gap-2 text-sm font-semibold" style="color: var(--ds-text)">
                  <span
                    class="flex size-6 items-center justify-center rounded text-xs font-bold"
                    style="background-color: var(--ds-primary); color: white"
                  >C</span>
                  Đáp án C <span style="color: var(--ds-danger)">*</span>
                </label>
                <input
                  v-model="newQuestion.optionC"
                  type="text"
                  class="ds-input w-full rounded-lg border px-4 py-2.5 text-sm"
                  style="border-color: var(--ds-border); background-color: var(--ds-surface); color: var(--ds-text)"
                  placeholder="Nhập đáp án C..."
                />
              </div>
              <div>
                <label class="mb-1.5 flex items-center gap-2 text-sm font-semibold" style="color: var(--ds-text)">
                  <span
                    class="flex size-6 items-center justify-center rounded text-xs font-bold"
                    style="background-color: var(--ds-primary); color: white"
                  >D</span>
                  Đáp án D <span style="color: var(--ds-danger)">*</span>
                </label>
                <input
                  v-model="newQuestion.optionD"
                  type="text"
                  class="ds-input w-full rounded-lg border px-4 py-2.5 text-sm"
                  style="border-color: var(--ds-border); background-color: var(--ds-surface); color: var(--ds-text)"
                  placeholder="Nhập đáp án D..."
                />
              </div>
            </div>

            <!-- Correct Answer and Score -->
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label class="mb-1.5 block text-sm font-semibold" style="color: var(--ds-text)">
                  Đáp án đúng <span style="color: var(--ds-danger)">*</span>
                </label>
                <div class="flex gap-2">
                  <button
                    v-for="option in ['A', 'B', 'C', 'D']"
                    :key="option"
                    type="button"
                    class="flex size-10 items-center justify-center rounded-lg font-bold transition-all"
                    :style="newQuestion.correctAnswer === option
                      ? { backgroundColor: 'var(--ds-success)', color: 'white' }
                      : { backgroundColor: 'var(--ds-gray-100)', color: 'var(--ds-text-muted)' }"
                    @click="newQuestion.correctAnswer = option"
                  >
                    {{ option }}
                  </button>
                </div>
              </div>
              <div>
                <label class="mb-1.5 block text-sm font-semibold" style="color: var(--ds-text)">
                  Điểm số <span style="color: var(--ds-danger)">*</span>
                </label>
                <input
                  v-model.number="newQuestion.score"
                  type="number"
                  min="0.5"
                  max="10"
                  step="0.5"
                  class="ds-input w-full rounded-lg border px-4 py-2.5 text-sm sm:w-32"
                  style="border-color: var(--ds-border); background-color: var(--ds-surface); color: var(--ds-text)"
                />
              </div>
            </div>

            <!-- Add Question Button -->
            <div class="flex justify-end">
              <button
                type="button"
                class="inline-flex items-center justify-center gap-2 rounded-lg px-5 py-2.5 text-sm font-bold transition-all hover:-translate-y-0.5"
                :class="isFormValid ? '' : 'opacity-50 cursor-not-allowed'"
                :disabled="!isFormValid"
                style="background-color: var(--ds-primary); color: white; box-shadow: var(--ds-shadow-sm)"
                @click="addQuestion"
              >
                <LucideIcon name="add" size="18" />
                <span>Thêm câu hỏi</span>
              </button>
            </div>
          </FormSection>
        </DsCard>
      </div>

      <!-- Questions List -->
      <div class="mb-6 ds-animate-fade-up" style="animation-delay: 0.2s">
        <DsCard padding="none">
          <template #header>
            <div class="flex items-center justify-between px-5 pt-5">
              <div class="flex items-center gap-2">
                <LucideIcon name="list" />
                <h3 class="text-lg font-bold" style="color: var(--ds-text)">Danh sách câu hỏi</h3>
                <span
                  class="ml-2 rounded-full px-2 py-0.5 text-xs font-bold"
                  style="background-color: var(--ds-primary-soft); color: var(--ds-primary)"
                >
                  {{ questions.length }}
                </span>
              </div>
            </div>
          </template>

          <DataTable
            v-if="questions.length > 0"
            :columns="questionColumns"
            :data="questions"
            :row-key="'id'"
          >
            <template #cell-number="{ row }">
              <span
                class="flex size-8 items-center justify-center rounded-lg font-bold"
                style="background-color: var(--ds-primary-soft); color: var(--ds-primary)"
              >
                {{ row.number }}
              </span>
            </template>
            <template #cell-content="{ value }">
              <p class="max-w-md truncate text-sm" style="color: var(--ds-text)" :title="value">
                {{ value }}
              </p>
            </template>
            <template #cell-correctAnswer="{ value }">
              <span
                class="inline-flex items-center justify-center rounded-full px-2 py-0.5 text-xs font-bold"
                style="background-color: var(--ds-success-bg); color: var(--ds-success)"
              >
                {{ value }}
              </span>
            </template>
            <template #cell-score="{ value }">
              <span class="text-sm font-semibold" style="color: var(--ds-text)">{{ value }} điểm</span>
            </template>
            <template #row-actions="{ row, index }">
              <div class="flex items-center justify-end gap-2">
                <button
                  type="button"
                  class="rounded-lg p-1.5 transition-colors hover:bg-[var(--ds-gray-100)]"
                  style="color: var(--ds-primary)"
                  title="Chỉnh sửa"
                  @click="editQuestion(row, index)"
                >
                  <LucideIcon name="edit" size="18" />
                </button>
                <button
                  type="button"
                  class="rounded-lg p-1.5 transition-colors hover:bg-[var(--ds-danger-soft)]"
                  style="color: var(--ds-danger)"
                  title="Xóa"
                  @click="deleteQuestion(index)"
                >
                  <LucideIcon name="delete" size="18" />
                </button>
              </div>
            </template>
          </DataTable>

          <!-- Empty State -->
          <div v-else class="flex flex-col items-center justify-center py-12 text-center">
            <LucideIcon name="quiz" />
            <p class="mt-3 text-sm font-medium" style="color: var(--ds-text-muted)">Chưa có câu hỏi nào</p>
            <p class="mt-1 text-xs" style="color: var(--ds-text-muted)">Điền form trên để thêm câu hỏi mới</p>
          </div>
        </DsCard>
      </div>

      <!-- Action Bar -->
      <ActionBar class="mt-6">
        <button
          type="button"
          class="inline-flex items-center justify-center gap-2 rounded-lg px-5 py-2.5 text-sm font-bold transition-all hover:-translate-y-0.5"
          style="background-color: var(--ds-surface); color: var(--ds-text); border: 1px solid var(--ds-border); box-shadow: var(--ds-shadow-sm)"
          @click="cancel"
        >
          <LucideIcon name="close" size="18" />
          <span>Hủy bỏ</span>
        </button>
        <button
          type="button"
          class="inline-flex items-center justify-center gap-2 rounded-lg px-5 py-2.5 text-sm font-bold transition-all hover:-translate-y-0.5"
          :class="questions.length === 0 ? 'opacity-50 cursor-not-allowed' : ''"
          :disabled="questions.length === 0"
          style="background-color: var(--ds-surface); color: var(--ds-text-secondary); border: 1px solid var(--ds-border); box-shadow: var(--ds-shadow-sm)"
          @click="saveAll"
        >
          <LucideIcon name="save" size="18" />
          <span>Lưu tất cả ({{ questions.length }})</span>
        </button>
        <button
          type="button"
          class="inline-flex items-center justify-center gap-2 rounded-lg px-5 py-2.5 text-sm font-bold transition-all hover:-translate-y-0.5"
          :class="questions.length === 0 ? 'opacity-50 cursor-not-allowed' : ''"
          :disabled="questions.length === 0"
          style="background-color: var(--ds-primary); color: white; box-shadow: var(--ds-shadow-sm)"
          @click="saveAndContinue"
        >
          <LucideIcon name="check" size="18" />
          <span>Lưu và tiếp tục</span>
        </button>
      </ActionBar>

    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import PageHeader from '../ui/PageHeader.vue'
import DsCard from '../ui/DsCard.vue'
import FormSection from '../ui/FormSection.vue'
import DataTable from '../ui/DataTable.vue'
import StatusChip from '../ui/StatusChip.vue'
import ActionBar from '../ui/ActionBar.vue'
import { createQuestion } from '../../services/questionService'
import { useToast } from '../../composables/useToast'

const router = useRouter()
const route = useRoute()
const toast = useToast()

const examId = route.query.examId || ''
const examTitle = route.query.title || 'Đề thi chưa có tiêu đề'

// New question form
const newQuestion = ref({
  content: '',
  optionA: '',
  optionB: '',
  optionC: '',
  optionD: '',
  correctAnswer: '',
  score: 1
})

// Questions list
const questions = ref([])

const isFormValid = computed(() => {
  return (
    newQuestion.value.content.trim() &&
    newQuestion.value.optionA.trim() &&
    newQuestion.value.optionB.trim() &&
    newQuestion.value.optionC.trim() &&
    newQuestion.value.optionD.trim() &&
    newQuestion.value.correctAnswer &&
    newQuestion.value.score > 0
  )
})

const questionColumns = [
  { key: 'number', label: '#', width: '60px', align: 'center' },
  { key: 'content', label: 'Nội dung câu hỏi' },
  { key: 'correctAnswer', label: 'Đáp án đúng', width: '120px', align: 'center' },
  { key: 'score', label: 'Điểm', width: '80px', align: 'center' },
  { key: '_actions', label: 'Thao tác', width: '100px', align: 'right' }
]

let questionIdCounter = 1

const addQuestion = () => {
  if (!isFormValid.value) return

  questions.value.push({
    id: questionIdCounter++,
    number: questions.value.length + 1,
    content: newQuestion.value.content,
    optionA: newQuestion.value.optionA,
    optionB: newQuestion.value.optionB,
    optionC: newQuestion.value.optionC,
    optionD: newQuestion.value.optionD,
    correctAnswer: newQuestion.value.correctAnswer,
    score: newQuestion.value.score
  })

  // Reset form
  newQuestion.value = {
    content: '',
    optionA: '',
    optionB: '',
    optionC: '',
    optionD: '',
    correctAnswer: '',
    score: 1
  }
}

const editQuestion = (question, index) => {
  newQuestion.value = {
    content: question.content,
    optionA: question.optionA,
    optionB: question.optionB,
    optionC: question.optionC,
    optionD: question.optionD,
    correctAnswer: question.correctAnswer,
    score: question.score
  }
  questions.value.splice(index, 1)
  // Renumber
  questions.value.forEach((q, i) => {
    q.number = i + 1
  })
}

const deleteQuestion = (index) => {
  questions.value.splice(index, 1)
  // Renumber
  questions.value.forEach((q, i) => {
    q.number = i + 1
  })
}

const cancel = () => {
  router.back()
}

const saveAll = async () => {
  if (questions.value.length === 0) return
  try {
    for (const q of questions.value) {
      await createQuestion(Number(examId), {
        content: q.content,
        type: 'SINGLE_CHOICE',
        scoreWeight: q.score,
        options: [
          { id: 'A', text: q.optionA },
          { id: 'B', text: q.optionB },
          { id: 'C', text: q.optionC },
          { id: 'D', text: q.optionD }
        ],
        correctAnswer: q.correctAnswer,
        difficulty: null
      })
    }
    toast.success(`Đã lưu ${questions.value.length} câu hỏi thành công!`)
    router.push({
      path: '/teacher/exams',
      query: { saved: 'true' }
    })
  } catch (err) {
    toast.error('Lưu câu hỏi thất bại: ' + (err.message || 'Lỗi không xác định'))
  }
}

const saveAndContinue = async () => {
  if (questions.value.length === 0) return
  try {
    for (const q of questions.value) {
      await createQuestion(Number(examId), {
        content: q.content,
        type: 'SINGLE_CHOICE',
        scoreWeight: q.score,
        options: [
          { id: 'A', text: q.optionA },
          { id: 'B', text: q.optionB },
          { id: 'C', text: q.optionC },
          { id: 'D', text: q.optionD }
        ],
        correctAnswer: q.correctAnswer,
        difficulty: null
      })
    }
    toast.success(`Đã lưu ${questions.value.length} câu hỏi! Đang chuyển đến tạo đợt thi...`)
    router.push({
      path: '/teacher/exams/new-session',
      query: {
        examId,
        title: examTitle,
        questionCount: questions.value.length
      }
    })
  } catch (err) {
    toast.error('Lưu câu hỏi thất bại: ' + (err.message || 'Lỗi không xác định'))
  }
}
</script>

<style scoped>
.ds-animate-fade-up {
  animation: fadeUp 0.5s ease-out;
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

.ds-input:focus {
  outline: none;
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-soft);
}
</style>
