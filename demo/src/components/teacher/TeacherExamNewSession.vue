<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto max-w-4xl px-4 pb-10 pt-4 sm:px-6 lg:px-8">

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
          <span class="font-medium" style="color: var(--ds-text)">Tạo đợt thi mới</span>
        </div>
      </div>

      <!-- Page Header -->
      <div class="mb-6 ds-animate-fade-up" style="animation-delay: 0.05s">
        <PageHeader
          eyebrow="Tạo đợt thi"
          title="Thiết lập phiên thi mới"
          :subtitle="'Cấu hình lịch thi và giám sát cho: ' + examTitle"
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
              <LucideIcon name="assignment" size="24" />
            </div>
            <div>
              <h3 class="text-lg font-bold" style="color: var(--ds-text)">{{ examTitle }}</h3>
              <p class="text-sm" style="color: var(--ds-text-muted)">Mã đề: {{ examId || 'EX-001' }}</p>
            </div>
          </div>
        </DsCard>
      </div>

      <!-- Form Sections -->
      <div class="space-y-6">

        <!-- Schedule Section -->
        <div class="ds-animate-fade-up" style="animation-delay: 0.15s">
          <DsCard padding="lg">
            <FormSection
              title="Lịch thi"
              description="Thiết lập thời gian bắt đầu và kết thúc cho đợt thi này"
            >
              <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label class="mb-1.5 block text-sm font-semibold" style="color: var(--ds-text)">
                    Thời gian bắt đầu <span style="color: var(--ds-danger)">*</span>
                  </label>
                  <input
                    v-model="formData.startTime"
                    type="datetime-local"
                    class="ds-input w-full rounded-lg border px-4 py-2.5 text-sm"
                    style="border-color: var(--ds-border); background-color: var(--ds-surface); color: var(--ds-text)"
                  />
                </div>
                <div>
                  <label class="mb-1.5 block text-sm font-semibold" style="color: var(--ds-text)">
                    Thời gian kết thúc <span style="color: var(--ds-danger)">*</span>
                  </label>
                  <input
                    v-model="formData.endTime"
                    type="datetime-local"
                    class="ds-input w-full rounded-lg border px-4 py-2.5 text-sm"
                    style="border-color: var(--ds-border); background-color: var(--ds-surface); color: var(--ds-text)"
                  />
                </div>
              </div>
              <div class="mt-4">
                <label class="mb-1.5 block text-sm font-semibold" style="color: var(--ds-text)">
                  Thời gian làm bài (phút) <span style="color: var(--ds-danger)">*</span>
                </label>
                <input
                  v-model.number="formData.durationMinutes"
                  type="number"
                  min="1"
                  max="300"
                  class="ds-input w-full rounded-lg border px-4 py-2.5 text-sm sm:w-48"
                  style="border-color: var(--ds-border); background-color: var(--ds-surface); color: var(--ds-text)"
                />
                <p class="mt-1 text-xs" style="color: var(--ds-text-muted)">
                  Thời gian làm bài tối đa cho mỗi sinh viên
                </p>
              </div>
            </FormSection>
          </DsCard>
        </div>

        <!-- Monitor Settings Section -->
        <div class="ds-animate-fade-up" style="animation-delay: 0.2s">
          <DsCard padding="lg">
            <FormSection
              title="Cài đặt giám sát"
              description="Cấu hình các tính năng chống gian lận cho đợt thi"
            >
              <div class="space-y-4">
                <div class="flex items-center justify-between rounded-lg border p-4" style="border-color: var(--ds-border)">
                  <div class="flex items-center gap-3">
                    <LucideIcon name="videocam" />
                    <div>
                      <p class="text-sm font-semibold" style="color: var(--ds-text)">Giám sát camera</p>
                      <p class="text-xs" style="color: var(--ds-text-muted)">Yêu cầu sinh viên bật camera trong quá trình thi</p>
                    </div>
                  </div>
                  <button
                    type="button"
                    class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors"
                    :style="{ backgroundColor: formData.cameraEnabled ? 'var(--ds-primary)' : 'var(--ds-gray-200)' }"
                    @click="formData.cameraEnabled = !formData.cameraEnabled"
                  >
                    <span
                      class="inline-block size-4 rounded-full bg-white shadow transition-transform"
                      :class="formData.cameraEnabled ? 'translate-x-6' : 'translate-x-1'"
                    />
                  </button>
                </div>

                <div class="flex items-center justify-between rounded-lg border p-4" style="border-color: var(--ds-border)">
                  <div class="flex items-center gap-3">
                    <LucideIcon name="mic" />
                    <div>
                      <p class="text-sm font-semibold" style="color: var(--ds-text)">Giám sát microphone</p>
                      <p class="text-xs" style="color: var(--ds-text-muted)">Yêu cầu sinh viên bật microphone trong quá trình thi</p>
                    </div>
                  </div>
                  <button
                    type="button"
                    class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors"
                    :style="{ backgroundColor: formData.micEnabled ? 'var(--ds-primary)' : 'var(--ds-gray-200)' }"
                    @click="formData.micEnabled = !formData.micEnabled"
                  >
                    <span
                      class="inline-block size-4 rounded-full bg-white shadow transition-transform"
                      :class="formData.micEnabled ? 'translate-x-6' : 'translate-x-1'"
                    />
                  </button>
                </div>

                <div class="flex items-center justify-between rounded-lg border p-4" style="border-color: var(--ds-border)">
                  <div class="flex items-center gap-3">
                    <LucideIcon name="tab" />
                    <div>
                      <p class="text-sm font-semibold" style="color: var(--ds-text)">Phát hiện chuyển tab</p>
                      <p class="text-xs" style="color: var(--ds-text-muted)">Cảnh báo khi sinh viên chuyển sang tab khác</p>
                    </div>
                  </div>
                  <button
                    type="button"
                    class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors"
                    :style="{ backgroundColor: formData.tabSwitchDetection ? 'var(--ds-primary)' : 'var(--ds-gray-200)' }"
                    @click="formData.tabSwitchDetection = !formData.tabSwitchDetection"
                  >
                    <span
                      class="inline-block size-4 rounded-full bg-white shadow transition-transform"
                      :class="formData.tabSwitchDetection ? 'translate-x-6' : 'translate-x-1'"
                    />
                  </button>
                </div>

                <div class="flex items-center justify-between rounded-lg border p-4" style="border-color: var(--ds-border)">
                  <div class="flex items-center gap-3">
                    <LucideIcon name="visibility_off" />
                    <div>
                      <p class="text-sm font-semibold" style="color: var(--ds-text)">Chống copy câu hỏi</p>
                      <p class="text-xs" style="color: var(--ds-text-muted)">Ngăn sinh viên copy nội dung câu hỏi</p>
                    </div>
                  </div>
                  <button
                    type="button"
                    class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors"
                    :style="{ backgroundColor: formData.preventCopy ? 'var(--ds-primary)' : 'var(--ds-gray-200)' }"
                    @click="formData.preventCopy = !formData.preventCopy"
                  >
                    <span
                      class="inline-block size-4 rounded-full bg-white shadow transition-transform"
                      :class="formData.preventCopy ? 'translate-x-6' : 'translate-x-1'"
                    />
                  </button>
                </div>
              </div>
            </FormSection>
          </DsCard>
        </div>

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
          style="background-color: var(--ds-surface); color: var(--ds-text-secondary); border: 1px solid var(--ds-border); box-shadow: var(--ds-shadow-sm)"
          @click="saveDraft"
        >
          <LucideIcon name="save" size="18" />
          <span>Lưu nháp</span>
        </button>
        <button
          type="button"
          class="inline-flex items-center justify-center gap-2 rounded-lg px-5 py-2.5 text-sm font-bold transition-all hover:-translate-y-0.5"
          style="background-color: var(--ds-primary); color: white; box-shadow: var(--ds-shadow-sm)"
          @click="publish"
        >
          <LucideIcon name="publish" size="18" />
          <span>Xuất bản ngay</span>
        </button>
      </ActionBar>

    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import PageHeader from '../ui/PageHeader.vue'
import DsCard from '../ui/DsCard.vue'
import FormSection from '../ui/FormSection.vue'
import ActionBar from '../ui/ActionBar.vue'

const router = useRouter()
const route = useRoute()

const examId = route.query.examId || ''
const examTitle = route.query.title || 'Đề thi chưa có tiêu đề'

// Form state
const formData = ref({
  startTime: '',
  endTime: '',
  durationMinutes: 30,
  cameraEnabled: true,
  micEnabled: false,
  tabSwitchDetection: true,
  preventCopy: true
})

// Set default times
const now = new Date()
const defaultStart = new Date(now.getTime() + 24 * 60 * 60 * 1000) // Tomorrow
const defaultEnd = new Date(defaultStart.getTime() + 2 * 60 * 60 * 1000) // +2 hours

formData.value.startTime = formatDateTimeLocal(defaultStart)
formData.value.endTime = formatDateTimeLocal(defaultEnd)

function formatDateTimeLocal(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}`
}

// Actions
const cancel = () => {
  router.back()
}

const saveDraft = () => {
  console.log('Save draft:', formData.value)
  router.push({
    path: '/teacher/exams',
    query: { draft: 'true' }
  })
}

const publish = () => {
  console.log('Publish exam session:', formData.value)
  router.push({
    path: '/teacher/exams/created-success',
    query: {
      examId: examId || 'EX-001',
      code: generateExamCode(),
      title: examTitle,
      durationMinutes: formData.value.durationMinutes,
      startAt: formData.value.startTime,
      endAt: formData.value.endTime
    }
  })
}

function generateExamCode() {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
  let code = ''
  for (let i = 0; i < 8; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return code
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
