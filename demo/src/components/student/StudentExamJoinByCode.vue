<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="portal-viewport flex h-full min-h-0 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100"
  >
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
        <StudentTopHeader class="shrink-0" />

        <main class="teacher-page-shell relative flex min-h-0 w-full flex-1 flex-col overflow-hidden">
          <div class="portal-scrollbar relative flex min-h-0 flex-1 flex-col overflow-y-auto">
            <PageHeader
              class="animate-fade-up"
              eyebrow="Student"
              title="Thi qua mã"
              subtitle="Nhập mã đề hoặc tiêu đề để vào phòng chờ trước khi bắt đầu làm bài."
            />

            <BaseCard class="animate-fade-up-delay" hoverable>
              <div class="mb-4 flex items-center gap-3">
                <span class="material-symbols-outlined text-3xl text-primary" aria-hidden="true">login</span>
                <h2 class="teacher-section-title text-slate-900 dark:text-slate-100">Vào phòng thi</h2>
              </div>

              <div class="flex flex-col items-end gap-4 md:flex-row">
                <BaseField
                  class="w-full flex-1"
                  label="Mã / Code / Tiêu đề bài thi"
                  for-id="exam-code-input"
                  v-slot="{ inputId, hintId, errorId }"
                >
                  <BaseInput
                    :id="inputId"
                    v-model="examCode"
                    autocomplete="off"
                    placeholder="Ví dụ: ABCD 1234"
                    input-class="border-primary/25 bg-background-light dark:bg-background-dark"
                    :hint-id="hintId"
                    :error-id="errorId"
                  />
                </BaseField>
                <BaseButton class="w-full shrink-0 md:w-auto" :loading="isJoining" @click="goToWaitingRoom">
                  Vào phòng
                </BaseButton>
              </div>
            </BaseCard>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { joinExamByCode } from '../../services/examService'
import { useToast } from '../../composables/useToast'
import StudentTopHeader from './StudentTopHeader.vue'
import BaseCard from '../shared/BaseCard.vue'
import BaseButton from '../shared/BaseButton.vue'
import BaseField from '../shared/BaseField.vue'
import BaseInput from '../shared/BaseInput.vue'
import PageHeader from '../shared/PageHeader.vue'

const router = useRouter()
const isDark = ref(false)
const examCode = ref('')
const isJoining = ref(false)
const toast = useToast()

const resolveExamByInput = async () => {
  const query = examCode.value.trim()
  if (!query) {
    return null
  }

  return joinExamByCode(query)
}

const goToWaitingRoom = async () => {
  isJoining.value = true

  try {
    const matchedExam = await resolveExamByInput()
    if (!matchedExam) {
      toast.error('Vui lòng nhập mã hoặc tiêu đề để tìm bài thi.')
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
    toast.error('Không thể vào bài thi lúc này.')
  } finally {
    isJoining.value = false
  }
}
</script>
