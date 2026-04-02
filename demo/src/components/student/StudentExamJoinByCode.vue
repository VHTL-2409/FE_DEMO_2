<template>
  <div class="bg-[var(--ds-bg)] min-h-full relative overflow-hidden">
    <!-- Decorative background gradient -->
    <div class="absolute inset-0 pointer-events-none overflow-hidden">
      <div class="absolute -top-40 -right-40 w-80 h-80 bg-gradient-to-br from-primary/5 to-transparent rounded-full blur-3xl"></div>
      <div class="absolute top-1/2 -left-20 w-60 h-60 bg-gradient-to-tr from-amber-500/5 to-transparent rounded-full blur-3xl"></div>
      <div class="absolute bottom-0 right-1/4 w-40 h-40 bg-gradient-to-t from-primary/5 to-transparent rounded-full blur-2xl"></div>
    </div>

    <div class="mx-auto px-4 pb-10 pt-4 sm:px-6 lg:px-8 relative">

      <!-- Page Header -->
      <div class="mb-6 ds-animate-fade-up max-w-6xl">
        <PageHeader
          eyebrow="Sinh viên"
          title="Vào thi"
          subtitle="Chọn đề thi từ lớp học hoặc nhập mã đề để tham gia thi."
          size="default"
        />
      </div>

      <!-- Two-card layout -->
      <div class="max-w-6xl grid grid-cols-1 lg:grid-cols-2 gap-4 ds-animate-fade-up" style="animation-delay: 0.05s">

        <!-- Card 1: Thi theo mã đề -->
        <div class="ejc__card rounded-[var(--ds-radius-2xl)] border border-[var(--ds-border)] bg-gradient-to-br from-[var(--ds-surface)] to-[var(--ds-primary-soft)]/20 p-6 shadow-[var(--ds-shadow-md)] relative overflow-hidden">
          <div class="absolute top-0 right-0 w-32 h-32 bg-gradient-to-bl from-primary/10 to-transparent rounded-bl-full"></div>
          <div class="mb-5 flex items-center gap-3 relative">
            <div class="flex size-12 items-center justify-center rounded-[var(--ds-radius-xl)] bg-gradient-to-br from-primary to-indigo-600 text-white shadow-lg shadow-primary/30">
              <LucideIcon name="login" size="24" />
            </div>
            <div>
              <h2 class="text-lg font-bold text-[var(--ds-text)]">Thi qua mã đề</h2>
              <p class="mt-0.5 text-xs text-[var(--ds-text-muted)]">Nhập mã hoặc tiêu đề để vào phòng chờ</p>
            </div>
          </div>

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

          <p class="mt-2 text-xs text-[var(--ds-text-muted)] flex items-center gap-1.5">
            <LucideIcon name="info" size="14" />
            Mã đề thi thường là chuỗi ký tự do giảng viên cung cấp.
          </p>

          <div v-if="errorMsg" class="mt-4 flex items-center gap-2 rounded-[var(--ds-radius-xl)] border border-[rgba(220,38,38,0.2)] bg-[var(--ds-danger-bg)] px-4 py-3 animate-fade-up">
            <LucideIcon name="error" size="18" />
            <p class="text-sm font-semibold text-[var(--ds-danger)]">{{ errorMsg }}</p>
          </div>

          <div v-if="isJoining" class="mt-4 flex items-center justify-center gap-3 py-3">
            <div class="w-5 h-5 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></div>
            <span class="text-sm text-[var(--ds-text-muted)]">Đang tìm kiếm...</span>
          </div>

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

        <!-- Card 2: Đề thi từ lớp học -->
        <div class="ejc__card rounded-[var(--ds-radius-2xl)] border border-[var(--ds-border)] bg-[var(--ds-surface)] p-6 shadow-[var(--ds-shadow-md)] overflow-hidden">
          <div class="mb-4 flex items-center gap-3">
            <div class="flex size-12 items-center justify-center rounded-[var(--ds-radius-xl)] bg-gradient-to-br from-amber-500 to-orange-500 text-white shadow-lg shadow-amber-500/25">
              <LucideIcon name="school" size="24" />
            </div>
            <div>
              <h2 class="text-lg font-bold text-[var(--ds-text)]">Đề thi từ lớp học</h2>
              <p class="mt-0.5 text-xs text-[var(--ds-text-muted)]">Chọn đề thi được giao từ lớp học của bạn</p>
            </div>
          </div>

          <!-- Loading -->
          <div v-if="isLoadingClasses" class="flex items-center justify-center gap-3 py-8">
            <div class="w-5 h-5 border-2 border-amber-500/30 border-t-amber-500 rounded-full animate-spin"></div>
            <span class="text-sm text-[var(--ds-text-muted)]">Đang tải đề thi...</span>
          </div>

          <!-- No classes -->
          <div v-else-if="classExams.length === 0" class="flex flex-col items-center justify-center py-8 text-center">
            <div class="size-14 rounded-full bg-[var(--ds-gray-100)] flex items-center justify-center mb-3">
              <LucideIcon name="school" size="28" class="text-[var(--ds-text-muted)]" />
            </div>
            <h3 class="text-sm font-bold text-[var(--ds-text)] mb-1">Chưa tham gia lớp học nào</h3>
            <p class="text-xs text-[var(--ds-text-muted)] max-w-xs">Bạn chưa tham gia lớp học nào. Tham gia lớp qua mã lớp để xem các đề thi được giao.</p>
          </div>

          <!-- Class exam list -->
          <div v-else class="ejc__class-list">
            <div v-for="cls in classExams" :key="cls.id" class="ejc__class-group">
              <div class="ejc__class-header">
                <LucideIcon name="folder" size="14" />
                <span>{{ cls.name }}</span>
                <span v-if="cls.subject" class="ejc__class-subject">{{ cls.subject }}</span>
              </div>
              <div v-if="cls.exams && cls.exams.length > 0" class="ejc__exam-items">
                <button
                  v-for="exam in cls.exams"
                  :key="exam.id"
                  type="button"
                  class="ejc__exam-item"
                  @click="enterClassExam(exam)"
                >
                  <div class="ejc__exam-icon">
                    <LucideIcon name="quiz" size="16" />
                  </div>
                  <div class="ejc__exam-info">
                    <span class="ejc__exam-title">{{ exam.title || exam.name || 'Bài thi' }}</span>
                    <span class="ejc__exam-meta">
                      <LucideIcon name="timer" size="11" />
                      {{ exam.durationMinutes || exam.duration || 60 }} phút
                      <span v-if="exam.questionCount" class="ml-2">
                        <LucideIcon name="help" size="11" />
                        {{ exam.questionCount }} câu
                      </span>
                    </span>
                  </div>
                  <div class="ejc__exam-arrow">
                    <LucideIcon name="chevron_right" size="18" />
                  </div>
                </button>
              </div>
              <div v-else class="ejc__no-exams">
                <LucideIcon name="inbox" size="14" />
                Chưa có đề thi nào
              </div>
            </div>
          </div>
        </div>

      </div>

      <!-- Info cards -->
      <div class="mt-4 max-w-6xl grid grid-cols-1 gap-4 sm:grid-cols-2 ds-animate-fade-up" style="animation-delay: 0.1s">
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
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { joinExamByCode } from '../../services/examService'
import { getMyClasses, getClassDetail } from '../../services/classService'
import { useToast } from '../../composables/useToast'
import PageHeader from '../ui/PageHeader.vue'

const router = useRouter()
const toast = useToast()
const examCode = ref('')
const isJoining = ref(false)
const errorMsg = ref('')
const isInputFocused = ref(false)

const classExams = ref([])
const isLoadingClasses = ref(false)

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

const enterClassExam = (exam) => {
  router.push({
    path: '/student/exam-waiting-room',
    query: {
      examId: exam.id,
      examCode: exam.code || '',
      exam: exam.title || exam.name || 'Bài thi',
      duration: exam.durationMinutes || exam.duration || 60,
      questions: exam.questionCount || 0,
      startAt: exam.startTime || '',
      endAt: exam.endTime || '',
      requireCameraMic: exam.requireCameraMic === false ? 'false' : 'true'
    }
  })
}

const loadClassExams = async () => {
  isLoadingClasses.value = true
  try {
    const classes = await getMyClasses()
    if (!classes || classes.length === 0) {
      classExams.value = []
      return
    }
    const results = []
    for (const cls of classes) {
      try {
        const detail = await getClassDetail(cls.id)
        results.push({ ...cls, exams: detail?.exams || [] })
      } catch {
        results.push({ ...cls, exams: [] })
      }
    }
    classExams.value = results
  } catch {
    classExams.value = []
  } finally {
    isLoadingClasses.value = false
  }
}

onMounted(() => { loadClassExams() })
</script>

<style scoped>
.ds-input {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  color: var(--ds-text);
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.ds-input::placeholder { color: var(--ds-text-muted); }
.ds-input:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

/* Class exam list */
.ejc__class-list { display: flex; flex-direction: column; gap: 0.75rem; max-height: 340px; overflow-y: auto; }
.ejc__class-group { display: flex; flex-direction: column; gap: 0.375rem; }

.ejc__class-header {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.7rem;
  font-weight: 800;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  padding: 0 0.25rem;
}

.ejc__class-subject {
  font-weight: 600;
  color: var(--ds-text-muted);
  font-size: 0.65rem;
  background: var(--ds-gray-100);
  padding: 0.1rem 0.4rem;
  border-radius: var(--ds-radius-full);
}

.ejc__exam-items { display: flex; flex-direction: column; gap: 0.375rem; }

.ejc__exam-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 0.875rem;
  border-radius: var(--ds-radius-lg);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: left;
  width: 100%;
  font-family: inherit;
}

.ejc__exam-item:hover {
  border-color: var(--ds-primary-border);
  background: var(--ds-primary-soft);
  transform: translateX(2px);
}

.ejc__exam-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--ds-radius-md);
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ejc__exam-info { flex: 1; min-width: 0; }

.ejc__exam-title {
  display: block;
  font-size: 0.825rem;
  font-weight: 700;
  color: var(--ds-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ejc__exam-meta {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.68rem;
  color: var(--ds-text-muted);
  margin-top: 0.125rem;
}

.ejc__exam-arrow { color: var(--ds-text-muted); flex-shrink: 0; }
.ejc__exam-item:hover .ejc__exam-arrow { color: var(--ds-primary); }

.ejc__no-exams {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.72rem;
  color: var(--ds-text-muted);
  padding: 0.375rem 0.5rem;
  font-style: italic;
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
.animate-fade-up { animation: fadeUp 0.4s ease-out; }
</style>
