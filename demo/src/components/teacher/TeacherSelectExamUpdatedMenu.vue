<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto max-w-7xl px-4 pb-10 pt-4 sm:px-6 lg:px-8">

      <div class="mb-5 ds-animate-fade-up">
        <div class="mb-2 flex items-center gap-2 text-sm text-[var(--ds-text-muted)]">
          <RouterLink to="/teacher/dashboard" class="flex items-center gap-1 hover:text-[var(--ds-primary)] transition-colors">
            <LucideIcon name="home" size="16" />
            Trang chủ
          </RouterLink>
          <LucideIcon name="chevron_right" size="12" />
          <RouterLink to="/teacher/live-monitoring" class="flex items-center gap-1 hover:text-[var(--ds-primary)] transition-colors">
            <LucideIcon name="live_tv" size="16" />
            Giám sát
          </RouterLink>
          <LucideIcon name="chevron_right" size="12" />
          <span class="text-[var(--ds-text)] font-medium">Chọn đề thi</span>
        </div>
        <PageHeader
          title="Giám sát trực tiếp"
          subtitle="Chọn một đề thi đang trong thời gian thi để giám sát."
          size="default"
        />
      </div>

      <div class="mb-5 ds-animate-fade-up" style="animation-delay: 0.05s">
        <div class="tsem__search-wrap">
          <LucideIcon name="search" class="tsem__search-icon" />
          <input
            v-model="search"
            type="text"
            placeholder="Tìm đề thi theo tiêu đề hoặc mã..."
            class="ds-input tsem__search-input w-full text-sm"
          />
        </div>
      </div>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3 xl:gap-5 ds-animate-fade-up" style="animation-delay: 0.08s">
        <div v-if="isLoading" class="col-span-full flex flex-col items-center justify-center gap-3 rounded-[var(--ds-radius-xl)] border border-[var(--ds-border)] bg-[var(--ds-surface)] py-16 text-center">
          <LucideIcon name="progress_activity" size="24" />
          <p class="text-sm text-[var(--ds-text-muted)]">Đang tải danh sách đề thi…</p>
        </div>

        <div v-else-if="!filteredExams.length" class="col-span-full">
          <EmptyState
            icon="monitoring"
            title="Không có đề thi đang diễn ra"
            description="Hiện không có kỳ thi nào trong khung giờ. Thử tìm kiếm khác hoặc quay lại sau."
            action-label="Mở ngân hàng đề"
            fill
            dense
            @action="goToExamBank"
          />
        </div>

        <div
          v-for="exam in filteredExams"
          :key="exam.id"
          class="bg-[var(--ds-surface)] rounded-[var(--ds-radius-xl)] border border-[var(--ds-border)] border-l-4 border-l-[var(--ds-accent)] shadow-[var(--ds-shadow-sm)] overflow-hidden flex flex-col transition-all duration-200 hover:-translate-y-0.5 hover:shadow-[var(--ds-shadow-lg)] cursor-pointer"
          @click="goToMonitoring(exam)"
        >
          <div class="p-5 flex-1">
            <div class="mb-3 flex items-start justify-between gap-2">
              <StatusChip :status="exam.statusChip" size="sm" />
              <span class="text-[11px] font-mono text-[var(--ds-text-muted)]">ID: {{ exam.id }}</span>
            </div>
            <h3 class="text-lg font-bold text-[var(--ds-text)] mb-2 leading-snug">{{ exam.title }}</h3>
            <div class="flex items-center gap-2 text-sm text-[var(--ds-text-muted)]">
              <LucideIcon name="meeting_room" size="16" />
              {{ exam.location }}
            </div>

            <div class="mt-4 grid grid-cols-2 gap-3 border-t border-[var(--ds-border)] pt-4">
              <div>
                <p class="text-[10px] font-bold uppercase tracking-wider text-[var(--ds-text-muted)]">{{ exam.leftLabel }}</p>
                <div class="mt-1 flex items-center gap-1.5">
                  <LucideIcon name="group" size="16" />
                  <span class="text-lg font-bold text-[var(--ds-text)]">{{ exam.students }}</span>
                </div>
              </div>
              <div>
                <p class="text-[10px] font-bold uppercase tracking-wider text-[var(--ds-text-muted)]">{{ exam.timeLabel }}</p>
                <div class="mt-1 flex items-center gap-1.5">
                  <LucideIcon name="schedule" size="16" />
                  <span class="text-lg font-bold text-[var(--ds-text)]">{{ exam.timeLeft }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="border-t border-[var(--ds-border)] px-5 py-3">
            <button
              type="button"
              class="inline-flex w-full items-center justify-center gap-2 rounded-[var(--ds-radius-lg)] bg-[var(--ds-primary)] py-2 text-sm font-bold text-white transition-all hover:bg-[var(--ds-primary-hover)]"
            >
              <LucideIcon name="monitoring" size="18" />
              Giám sát
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listExams } from '../../services/examService'
import { useToast } from '../../composables/useToast'
import PageHeader from '../ui/PageHeader.vue'
import StatusChip from '../ui/StatusChip.vue'
import EmptyState from '../ui/EmptyState.vue'

const router = useRouter()
const toast = useToast()
const isLoading = ref(false)
const exams = ref([])
const search = ref('')

const filteredExams = computed(() => {
  const q = search.value.trim().toLowerCase()
  return exams.value
    .filter(exam => exam.isActive && exam.startTime && exam.endTime)
    .filter(exam => {
      const now = Date.now()
      const start = new Date(exam.startTime).getTime()
      const end = new Date(exam.endTime).getTime()
      return now >= start && now <= end
    })
    .filter(exam => !q || exam.title?.toLowerCase().includes(q) || exam.code?.toLowerCase().includes(q))
    .map(exam => {
      const now = Date.now()
      const start = new Date(exam.startTime).getTime()
      const end = new Date(exam.endTime).getTime()
      const elapsed = Math.floor((now - start) / 60000)
      const total = Math.floor((end - start) / 60000)
      return {
        id: exam.id,
        title: exam.title || '—',
        location: 'Trực tuyến',
        code: exam.code || '',
        students: exam.participantCount || 0,
        timeLeft: `${Math.max(0, total - elapsed)} phút`,
        leftLabel: 'Đang có mặt',
        timeLabel: 'Còn lại',
        statusChip: 'live'
      }
    })
})

const goToMonitoring = (exam) => {
  router.push({
    path: '/teacher/live-monitoring/session',
    query: { examId: exam.id, title: exam.title, code: exam.code }
  })
}

const goToExamBank = () => router.push('/teacher/exams/list')

const loadExams = async () => {
  isLoading.value = true
  try {
    exams.value = await listExams()
  } catch (error) {
    toast.error('Không thể tải danh sách đề thi.')
  } finally {
    isLoading.value = false
  }
}

onMounted(loadExams)
</script>

<style scoped>
.ds-input {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  color: var(--ds-text);
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.ds-input::placeholder { color: var(--ds-text-muted); }
.ds-input:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.tsem__search-wrap {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 0.75rem;
  padding: 0 0.9375rem;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  background: var(--ds-surface);
  box-shadow: var(--ds-shadow-sm);
  transition: border-color 0.15s, box-shadow 0.15s, background-color 0.15s;
}

.tsem__search-wrap:focus-within {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.tsem__search-icon {
  color: var(--ds-text-muted);
  pointer-events: none;
}

.tsem__search-input {
  min-width: 0;
  padding: 0.75rem 0;
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.tsem__search-input:focus {
  box-shadow: none;
}
</style>
