<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto max-w-3xl px-4 pb-10 pt-4 sm:px-6 lg:px-8">

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
            to="/teacher/exams/create"
          >
            Tạo đề thi
          </RouterLink>
          <LucideIcon name="chevron_right" size="12" />
          <span class="font-medium" style="color: var(--ds-text)">Thành công</span>
        </div>
      </div>

      <!-- Success Content -->
      <div class="text-center ds-animate-fade-up" style="animation-delay: 0.05s">
        <!-- Success Icon -->
        <div class="mb-6 flex justify-center">
          <div
            class="relative flex size-24 items-center justify-center rounded-full"
            style="background-color: var(--ds-success-bg)"
          >
            <LucideIcon name="check_circle" />
            <div
              class="absolute -right-2 -top-2 flex size-8 items-center justify-center rounded-full"
              style="background-color: var(--ds-success); color: white"
            >
              <LucideIcon name="celebration" size="18" />
            </div>
          </div>
        </div>

        <!-- Congratulations Message -->
        <h1 class="text-3xl font-extrabold tracking-tight sm:text-4xl" style="color: var(--ds-text)">
          Chúc mừng bạn!
        </h1>
        <p class="mt-3 text-lg" style="color: var(--ds-text-secondary)">
          Đợt thi <span class="font-semibold" style="color: var(--ds-primary)">{{ examTitle }}</span> đã được tạo thành công.
        </p>
        <p class="mt-1 text-sm" style="color: var(--ds-text-muted)">
          Sinh viên có thể bắt đầu làm bài khi đến thời gian thi.
        </p>
      </div>

      <!-- Exam Details Card -->
      <div class="mt-8 ds-animate-fade-up" style="animation-delay: 0.1s">
        <DsCard padding="lg">
          <div class="flex items-center gap-3 mb-6">
            <div
              class="flex size-12 items-center justify-center rounded-lg"
              style="background-color: var(--ds-primary-soft); color: var(--ds-primary)"
            >
              <LucideIcon name="info" size="24" />
            </div>
            <div>
              <h2 class="text-lg font-bold" style="color: var(--ds-text)">Thông tin đợt thi</h2>
              <p class="text-sm" style="color: var(--ds-text-muted)">Lưu lại mã để chia sẻ với sinh viên</p>
            </div>
          </div>

          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <!-- Exam Code — chỉ hiện với bài thi tự do -->
            <div
              v-if="examType === 'free'"
              class="rounded-lg p-4 text-center"
              style="background-color: var(--ds-info-bg); border: 1px solid rgba(2, 132, 199, 0.25)"
            >
              <p class="text-xs font-semibold uppercase tracking-wider" style="color: #0284c7">Mã đề thi</p>
              <p class="mt-1 text-2xl font-extrabold tracking-wider" style="color: #0284c7; font-family: 'Courier New', monospace">{{ examCode }}</p>
              <button
                type="button"
                class="mt-2 text-xs font-semibold underline-offset-200 hover:underline"
                style="color: #0284c7"
                @click="copyCode"
              >
                <span class="flex items-center justify-center gap-1">
                  <LucideIcon name="content_copy" size="14" />
                  Sao chép mã
                </span>
              </button>
            </div>

            <!-- Exam type badge — hiện với bài thi riêng tư -->
            <div
              v-if="examType === 'private'"
              class="rounded-lg p-4 text-center"
              style="background-color: var(--ds-primary-soft); border: 1px solid var(--ds-primary-border)"
            >
              <p class="text-xs font-semibold uppercase tracking-wider" style="color: var(--ds-primary)">Hình thức</p>
              <p class="mt-1 text-xl font-extrabold" style="color: var(--ds-primary)">Bài thi riêng tư</p>
              <p class="mt-1 text-xs" style="color: var(--ds-text-muted)">Chỉ học sinh trong lớp được phép</p>
            </div>

            <!-- Duration -->
            <div
              class="rounded-lg p-4 text-center"
              style="background-color: var(--ds-gray-50); border: 1px solid var(--ds-border)"
            >
              <p class="text-xs font-semibold uppercase tracking-wider" style="color: var(--ds-text-muted)">Thời gian làm bài</p>
              <p class="mt-1 text-2xl font-extrabold" style="color: var(--ds-text)">{{ durationMinutes }} phút</p>
              <p class="mt-1 text-xs" style="color: var(--ds-text-muted)">Tối đa cho mỗi sinh viên</p>
            </div>

            <!-- Question Count -->
            <div
              class="rounded-lg p-4 text-center"
              style="background-color: var(--ds-gray-50); border: 1px solid var(--ds-border)"
            >
              <p class="text-xs font-semibold uppercase tracking-wider" style="color: var(--ds-text-muted)">Số câu hỏi</p>
              <p class="mt-1 text-2xl font-extrabold" style="color: var(--ds-text)">{{ questionCount }}</p>
              <p class="mt-1 text-xs" style="color: var(--ds-text-muted)">Câu hỏi trong đề thi</p>
            </div>

            <!-- Scheduled Time -->
            <div
              class="rounded-lg p-4 text-center"
              style="background-color: var(--ds-gray-50); border: 1px solid var(--ds-border)"
            >
              <p class="text-xs font-semibold uppercase tracking-wider" style="color: var(--ds-text-muted)">Lịch thi</p>
              <p class="mt-1 text-lg font-extrabold" style="color: var(--ds-text)">{{ formattedStartTime }}</p>
              <p class="mt-0.5 text-xs" style="color: var(--ds-text-muted)">đến {{ formattedEndTime }}</p>
            </div>
          </div>
        </DsCard>
      </div>

      <!-- Action Cards -->
      <div class="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-3 ds-animate-fade-up" style="animation-delay: 0.15s">
        <ActionCard
          icon="dashboard"
          title="Về Dashboard"
          description="Xem tổng quan các đợt thi"
          to="/teacher"
        />
        <ActionCard
          icon="list"
          title="Danh sách đề thi"
          description="Quản lý tất cả đề thi của bạn"
          to="/teacher/exams"
        />
        <ActionCard
          icon="add_circle"
          title="Tạo đề thi khác"
          description="Bắt đầu một đợt thi mới"
          to="/teacher/exams/create"
        />
      </div>

      <!-- Share Info — chỉ với bài thi tự do -->
      <div v-if="examType === 'free'" class="mt-8 ds-animate-fade-up" style="animation-delay: 0.2s">
        <DsCard padding="md" variant="accent" accent-color="info">
          <div class="flex items-start gap-3">
            <LucideIcon name="tips_and_updates" />
            <div>
              <p class="text-sm font-semibold" style="color: var(--ds-text)">Mẹo chia sẻ đợt thi</p>
              <p class="mt-1 text-xs" style="color: var(--ds-text-secondary)">
                Chia sẻ mã đề thi <span class="font-bold" style="color: #0284c7">{{ examCode }}</span> với sinh viên qua email, Zalo, hoặc phương tiện khác.
                Sinh viên sẽ nhập mã này để tham gia thi.
              </p>
            </div>
          </div>
        </DsCard>
      </div>

      <!-- Info — với bài thi riêng tư -->
      <div v-if="examType === 'private'" class="mt-8 ds-animate-fade-up" style="animation-delay: 0.2s">
        <DsCard padding="md" variant="accent" accent-color="primary">
          <div class="flex items-start gap-3">
            <LucideIcon name="school" />
            <div>
              <p class="text-sm font-semibold" style="color: var(--ds-text)">Đợt thi riêng tư</p>
              <p class="mt-1 text-xs" style="color: var(--ds-text-secondary)">
                Chỉ học sinh trong lớp được chỉ định mới có thể tham gia thi.
                Học sinh sẽ thấy đề thi này trong danh sách bài thi của lớp.
              </p>
            </div>
          </div>
        </DsCard>
      </div>

    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import DsCard from '../ui/DsCard.vue'
import ActionCard from '../ui/ActionCard.vue'

const route = useRoute()

const examId = computed(() => route.query.examId || 'EX-001')
const examCode = computed(() => route.query.examCode || '')
const examType = computed(() => route.query.examType || 'free')
const examTitle = computed(() => route.query.title || 'Đề thi mới')
const durationMinutes = computed(() => route.query.durationMinutes || '30')
const questionCount = computed(() => route.query.questionCount || '20')

const startAt = computed(() => {
  const date = route.query.startAt
  if (!date) return new Date()
  return new Date(date)
})

const endAt = computed(() => {
  const date = route.query.endAt
  if (!date) return new Date()
  return new Date(date)
})

const formattedStartTime = computed(() => {
  return startAt.value.toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
})

const formattedEndTime = computed(() => {
  return endAt.value.toLocaleString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit'
  })
})

const copyCode = () => {
  navigator.clipboard.writeText(examCode.value)
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
</style>
