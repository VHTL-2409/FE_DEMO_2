<template>
  <div class="bg-[var(--ds-bg)] min-h-full relative overflow-hidden">
    <!-- Decorative background gradient -->
    <div class="absolute inset-0 pointer-events-none overflow-hidden">
      <div class="absolute -top-40 -right-40 w-80 h-80 bg-gradient-to-br from-primary/5 to-transparent rounded-full blur-3xl"></div>
      <div class="absolute top-1/2 -left-20 w-60 h-60 bg-gradient-to-tr from-amber-500/5 to-transparent rounded-full blur-3xl"></div>
      <div class="absolute bottom-0 right-1/4 w-40 h-40 bg-gradient-to-t from-primary/5 to-transparent rounded-full blur-2xl"></div>
    </div>
    
    <div class="mx-auto max-w-2xl px-4 pb-10 pt-4 sm:px-6 lg:px-8 relative">

      <!-- Page Header -->
      <div class="mb-6 ds-animate-fade-up">
        <PageHeader
          eyebrow="Sinh viên"
          title="Thi qua mã"
          subtitle="Nhập mã đề hoặc tiêu đề để vào phòng chờ trước khi bắt đầu làm bài."
          size="default"
        />
      </div>

      <!-- Join card -->
      <div class="ds-animate-fade-up relative" style="animation-delay: 0.05s">
        <div class="rounded-[var(--ds-radius-2xl)] border border-[var(--ds-border)] bg-gradient-to-br from-[var(--ds-surface)] to-[var(--ds-primary-soft)]/20 p-6 shadow-[var(--ds-shadow-md)] relative overflow-hidden">
          <!-- Decorative corner -->
          <div class="absolute top-0 right-0 w-32 h-32 bg-gradient-to-bl from-primary/10 to-transparent rounded-bl-full"></div>
          
          <div class="mb-5 flex items-center gap-3 relative">
            <div class="flex size-12 items-center justify-center rounded-[var(--ds-radius-xl)] bg-gradient-to-br from-primary to-indigo-600 text-white shadow-lg shadow-primary/30">
              <LucideIcon name="login" size="24" />
            </div>
            <div>
              <h2 class="text-lg font-bold text-[var(--ds-text)]">Vào phòng thi</h2>
              <p class="mt-0.5 text-xs text-[var(--ds-text-muted)]">Tìm kiếm bài thi bằng mã hoặc tiêu đề</p>
            </div>
          </div>

          <!-- Input -->
          <div class="relative mb-4">
            <div class="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--ds-text-muted)] transition-colors duration-200" :class="{ 'text-primary': isInputFocused }">
              <LucideIcon name="search" size="20" />
            </div>
            <input
              v-model.trim="examCode"
              type="text"
              autocomplete="off"
              placeholder="Nhập mã đề hoặc tiêu đề bài thi..."
              class="ds-input w-full pl-12 pr-4 py-4 rounded-[var(--ds-radius-xl)] text-sm transition-all duration-200"
              :class="{ 'ring-2 ring-primary/20': isInputFocused }"
              @focus="isInputFocused = true"
              @blur="isInputFocused = false"
              @keyup.enter="goToWaitingRoom"
            />
          </div>

          <!-- Hint -->
          <p class="mt-2 text-xs text-[var(--ds-text-muted)] flex items-center gap-1.5">
            <LucideIcon name="info" size="14" />
            Mã đề thi thường là chuỗi ký tự do giảng viên cung cấp.
          </p>

          <!-- Error -->
          <div v-if="errorMsg" class="mt-4 flex items-center gap-2 rounded-[var(--ds-radius-xl)] border border-[rgba(220,38,38,0.2)] bg-[var(--ds-danger-bg)] px-4 py-3 animate-fade-up">
            <LucideIcon name="error" size="18" />
            <p class="text-sm font-semibold text-[var(--ds-danger)]">{{ errorMsg }}</p>
          </div>

          <!-- Loading state -->
          <div v-if="isJoining" class="mt-4 flex items-center justify-center gap-3 py-3">
            <div class="w-5 h-5 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></div>
            <span class="text-sm text-[var(--ds-text-muted)]">Đang tìm kiếm...</span>
          </div>

          <!-- CTA -->
          <button
            v-if="!isJoining"
            type="button"
            class="mt-4 w-full rounded-[var(--ds-radius-xl)] bg-gradient-to-r from-primary to-indigo-600 px-4 py-4 text-sm font-bold text-white shadow-lg shadow-primary/25 transition-all duration-300 hover:shadow-xl hover:shadow-primary/30 hover:-translate-y-0.5 disabled:opacity-60 disabled:cursor-not-allowed disabled:transform-none disabled:hover:translate-y-0"
            :disabled="!examCode.trim()"
            @click="goToWaitingRoom"
          >
            <span class="flex items-center justify-center gap-2">
              <LucideIcon name="arrow_forward" size="18" />
              Vào phòng thi
            </span>
          </button>
        </div>
      </div>

      <!-- Info cards -->
      <div class="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 ds-animate-fade-up" style="animation-delay: 0.1s">
        <div class="group flex items-start gap-4 rounded-[var(--ds-radius-xl)] border border-[var(--ds-border)] bg-[var(--ds-surface)] p-5 shadow-sm transition-all duration-300 hover:shadow-lg hover:shadow-primary/5 hover:-translate-y-1 hover:border-primary/20">
          <div class="flex size-11 shrink-0 items-center justify-center rounded-[var(--ds-radius-lg)] bg-amber-100 text-amber-600 transition-transform group-hover:scale-110 dark:bg-amber-900/30 dark:text-amber-400">
            <LucideIcon name="quiz" size="20" />
          </div>
          <div>
            <p class="text-sm font-bold text-[var(--ds-text)] group-hover:text-primary transition-colors">Đủ thời gian</p>
            <p class="mt-1 text-xs text-[var(--ds-text-muted)]">Mỗi kỳ thi có giới hạn thời gian riêng.</p>
          </div>
        </div>
        <div class="group flex items-start gap-4 rounded-[var(--ds-radius-xl)] border border-[var(--ds-border)] bg-[var(--ds-surface)] p-5 shadow-sm transition-all duration-300 hover:shadow-lg hover:shadow-primary/5 hover:-translate-y-1 hover:border-primary/20">
          <div class="flex size-11 shrink-0 items-center justify-center rounded-[var(--ds-radius-lg)] bg-emerald-100 text-emerald-600 transition-transform group-hover:scale-110 dark:bg-emerald-900/30 dark:text-emerald-400">
            <LucideIcon name="lock" size="20" />
          </div>
          <div>
            <p class="text-sm font-bold text-[var(--ds-text)] group-hover:text-primary transition-colors">An toàn</p>
            <p class="mt-1 text-xs text-[var(--ds-text-muted)]">Câu trả lời được lưu tự động khi làm bài.</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { joinExamByCode } from '../../services/examService'
import { useToast } from '../../composables/useToast'
import PageHeader from '../ui/PageHeader.vue'

const router = useRouter()
const toast = useToast()
const examCode = ref('')
const isJoining = ref(false)
const errorMsg = ref('')
const isInputFocused = ref(false)

const goToWaitingRoom = async () => {
  const query = examCode.value.trim()
  if (!query) return

  errorMsg.value = ''
  isJoining.value = true

  try {
    const matchedExam = await joinExamByCode(query)
    if (!matchedExam) {
      errorMsg.value = 'Không tìm thấy bài thi phù hợp. Vui lòng kiểm tra lại mã.'
      return
    }

    router.push({
      path: '/student/exam-waiting-room',
      query: {
        examId: matchedExam.id,
        examCode: matchedExam.code || '',
        exam: matchedExam.title || 'Bài thi',
        duration: matchedExam.durationMinutes || 60,
        questions: matchedExam.questionCount || 0,
        startAt: matchedExam.startTime || '',
        endAt: matchedExam.endTime || '',
        requireCameraMic: matchedExam.requireCameraMic === false ? 'false' : 'true'
      }
    })
  } catch (error) {
    errorMsg.value = 'Không thể tìm bài thi lúc này. Vui lòng thử lại.'
  } finally {
    isJoining.value = false
  }
}
</script>

<style scoped>
.ds-input {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  color: var(--ds-text);
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.ds-input::placeholder {
  color: var(--ds-text-muted);
}
.ds-input:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
.animate-fade-up { animation: fadeUp 0.4s ease-out; }
</style>
