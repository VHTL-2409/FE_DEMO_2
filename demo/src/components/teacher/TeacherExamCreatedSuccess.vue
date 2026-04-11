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
          Học sinh có thể bắt đầu làm bài khi đến thời gian thi.
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
              <p class="text-sm" style="color: var(--ds-text-muted)">Lưu lại mã để chia sẻ với học sinh</p>
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
              <p class="mt-1 text-xs" style="color: var(--ds-text-muted)">Tối đa cho mỗi học sinh</p>
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
      <div class="mt-8 ds-animate-fade-up" style="animation-delay: 0.15s">
        <!-- Giám sát - quan trọng nhất -->
        <RouterLink
          :to="`/teacher/live-monitoring/session?examId=${examId}`"
          class="flex items-center gap-4 p-4 mb-4 rounded-xl transition-all hover:scale-[1.01] cursor-pointer"
          style="background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%); color: white; box-shadow: 0 4px 16px rgba(79, 70, 229, 0.3); text-decoration: none;"
        >
          <div class="flex items-center justify-center size-12 rounded-lg" style="background: rgba(255,255,255,0.2)">
            <LucideIcon name="monitor_heart" size="24" />
          </div>
          <div class="flex-1">
            <p class="font-bold text-lg">Giám sát ngay</p>
            <p class="text-sm opacity-90">Theo dõi phòng chờ và tiến trình thi của học sinh</p>
          </div>
          <LucideIcon name="chevron_right" size="20" class="opacity-75" />
        </RouterLink>

        <!-- Grid cho các action khác -->
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <RouterLink
            to="/teacher"
            class="flex items-center gap-3 p-3 rounded-xl border border-[var(--ds-border)] bg-[var(--ds-surface)] transition-all hover:-translate-y-0.5 hover:border-[var(--ds-primary-border)] hover:bg-[var(--ds-primary-soft)] hover:shadow-md text-decoration-none"
          >
            <span class="flex size-9 items-center justify-center rounded-lg bg-[var(--ds-primary-soft)] text-[var(--ds-primary)]">
              <LucideIcon name="dashboard" />
            </span>
            <div>
              <p class="text-sm font-semibold text-[var(--ds-text)]">Dashboard</p>
              <p class="text-xs text-[var(--ds-text-muted)]">Tổng quan</p>
            </div>
          </RouterLink>

          <RouterLink
            to="/teacher/exams"
            class="flex items-center gap-3 p-3 rounded-xl border border-[var(--ds-border)] bg-[var(--ds-surface)] transition-all hover:-translate-y-0.5 hover:border-[var(--ds-primary-border)] hover:bg-[var(--ds-primary-soft)] hover:shadow-md text-decoration-none"
          >
            <span class="flex size-9 items-center justify-center rounded-lg bg-[var(--ds-primary-soft)] text-[var(--ds-primary)]">
              <LucideIcon name="assignment" />
            </span>
            <div>
              <p class="text-sm font-semibold text-[var(--ds-text)]">Danh sách</p>
              <p class="text-xs text-[var(--ds-text-muted)]">Quản lý đề thi</p>
            </div>
          </RouterLink>

          <RouterLink
            to="/teacher/exams/create"
            class="flex items-center gap-3 p-3 rounded-xl border border-[var(--ds-border)] bg-[var(--ds-surface)] transition-all hover:-translate-y-0.5 hover:border-[var(--ds-primary-border)] hover:bg-[var(--ds-primary-soft)] hover:shadow-md text-decoration-none"
          >
            <span class="flex size-9 items-center justify-center rounded-lg bg-[var(--ds-primary-soft)] text-[var(--ds-primary)]">
              <LucideIcon name="add_circle" />
            </span>
            <div>
              <p class="text-sm font-semibold text-[var(--ds-text)]">Tạo mới</p>
              <p class="text-xs text-[var(--ds-text-muted)]">Đề thi khác</p>
            </div>
          </RouterLink>

          <button
            type="button"
            class="flex items-center gap-3 p-3 rounded-xl border border-[var(--ds-border)] bg-[var(--ds-surface)] transition-all hover:-translate-y-0.5 hover:border-[var(--ds-info)] hover:bg-[var(--ds-info-bg)] hover:shadow-md text-left"
            @click="copyCode"
          >
            <span class="flex size-9 items-center justify-center rounded-lg" style="background-color: var(--ds-info-bg); color: var(--ds-info);">
              <LucideIcon name="content_copy" />
            </span>
            <div>
              <p class="text-sm font-semibold text-[var(--ds-text)]">Sao chép mã</p>
              <p class="text-xs text-[var(--ds-text-muted)]">Chia sẻ đề thi</p>
            </div>
          </button>
        </div>
      </div>

      <!-- Share Info — chỉ với bài thi tự do -->
      <div v-if="examType === 'free'" class="mt-8 ds-animate-fade-up" style="animation-delay: 0.2s">
        <DsCard padding="md" variant="accent" accent-color="info">
          <div class="flex items-start gap-3">
            <LucideIcon name="tips_and_updates" />
            <div>
              <p class="text-sm font-semibold" style="color: var(--ds-text)">Mẹo chia sẻ đợt thi</p>
              <p class="mt-1 text-xs" style="color: var(--ds-text-secondary)">
                Chia sẻ mã đề thi <span class="font-bold" style="color: #0284c7">{{ examCode }}</span> với học sinh qua email, Zalo, hoặc phương tiện khác.
                Học sinh sẽ nhập mã này để tham gia thi.
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
import { computed, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import DsCard from '../ui/DsCard.vue'
import { useToast } from '../../composables/useToast'

const route = useRoute()
const toast = useToast()

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
  if (examCode.value) {
    navigator.clipboard.writeText(examCode.value)
    toast.success('Đã sao chép mã đề thi!')
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
  }
  to {
    opacity: 1;
  }
}
</style>
