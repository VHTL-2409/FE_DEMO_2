<template>
  <main class="tm-page">
    <header class="tm-header">
      <button
        class="tm-back"
        type="button"
        aria-label="Quay lại dashboard"
        @click="$router.push('/teacher/dashboard')"
      >
        <LucideIcon
          name="arrow-left"
          :size="18"
        />
      </button>

      <div class="tm-title">
        <span>Giám sát trực tiếp</span>
        <h1>Kỳ thi đang diễn ra</h1>
        <p>Chỉ hiển thị phòng thi đang cần theo dõi. Các đề chưa mở lịch hoặc đã kết thúc không xuất hiện ở màn này.</p>
        <div class="tm-inline-stats">
          <span>{{ liveExams.length }} kỳ thi live</span>
          <span>{{ totalActiveStudents }} thí sinh đang thi</span>
        </div>
      </div>

      <button
        class="tm-refresh"
        type="button"
        :disabled="isLoading"
        @click="loadExams"
      >
        <LucideIcon
          name="refresh-cw"
          :size="16"
          :class="{ 'tm-spin': isLoading }"
        />
        Làm mới
      </button>
    </header>

    <section
      v-if="isLoading && !allExams.length"
      class="tm-list"
    >
      <div
        v-for="i in 3"
        :key="i"
        class="tm-skeleton"
      />
    </section>

    <section
      v-else-if="loadError && !allExams.length"
      class="tm-state"
    >
      <LucideIcon
        name="alert-circle"
        :size="34"
      />
      <h2>Không thể tải phòng thi</h2>
      <p>{{ loadError }}</p>
      <button
        class="tm-primary"
        type="button"
        @click="loadExams"
      >
        Thử lại
      </button>
    </section>

    <section
      v-else-if="!liveExams.length"
      class="tm-state"
    >
      <LucideIcon
        name="radio"
        :size="34"
      />
      <h2>Không có kỳ thi đang diễn ra</h2>
      <p>Khi có phòng thi live, kỳ thi sẽ xuất hiện tại đây để vào giám sát ngay.</p>
    </section>

    <section
      v-else
      class="tm-list"
      aria-label="Kỳ thi đang diễn ra"
    >
      <label class="tm-search">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Tìm theo tên kỳ thi hoặc mã"
        >
      </label>

      <div
        v-if="!filteredLiveExams.length"
        class="tm-state"
      >
        <h2>Không có kỳ thi khớp bộ lọc</h2>
        <p>Thử đổi từ khóa tìm kiếm hoặc chọn phòng thi khác.</p>
      </div>

      <article
        v-for="exam in filteredLiveExams"
        :key="exam.id"
        class="tm-row"
      >
        <div class="tm-row__main">
          <span class="tm-status"><i />Đang thi</span>
          <h2>{{ exam.title }}</h2>
          <p>{{ exam.className || 'Trực tuyến' }} · {{ exam.code || 'Chưa có mã' }}</p>
        </div>

        <div class="tm-row__meta">
          <span>{{ exam.currentSessionParticipants || 0 }}/{{ exam.participantCount || 0 }} thí sinh</span>
          <span>{{ formatRemaining(exam.remainingSeconds) }} còn lại</span>
        </div>

        <div class="tm-row__actions">
          <button
            class="tm-primary"
            type="button"
            @click="goToMonitoring(exam)"
          >
            Vào giám sát
          </button>
          <button
            class="tm-secondary"
            type="button"
            @click="goToIncidents(exam)"
          >
            Báo cáo
          </button>
        </div>
      </article>

      <div v-if="upcomingExams.length" class="tm-section">
        <h2>Kỳ thi sắp bắt đầu</h2>
        <article
          v-for="exam in upcomingExams.slice(0, 5)"
          :key="`upcoming-${exam.id}`"
          class="tm-row tm-row--muted"
        >
          <div class="tm-row__main">
            <span class="tm-status tm-status--muted"><i />Sắp diễn ra</span>
            <h2>{{ exam.title }}</h2>
            <p>{{ exam.className || 'Trực tuyến' }} · {{ exam.code || 'Chưa có mã' }}</p>
          </div>

          <div class="tm-row__meta">
            <span>{{ formatSchedule(exam.startTime) }}</span>
            <span>{{ exam.participantCount || 0 }} thí sinh</span>
          </div>

          <div class="tm-row__actions">
            <button
              class="tm-secondary"
              type="button"
              @click="goToMonitoring(exam)"
            >
              Mở phòng
            </button>
          </div>
        </article>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listMonitoringExams } from '../../services/examService'
import { useToast } from '../../composables/useToast'

const router = useRouter()
const toast = useToast()
const isLoading = ref(false)
const loadError = ref('')
const allExams = ref([])
const searchQuery = ref('')

const liveExams = computed(() => allExams.value.filter(e => e.monitoringStatus === 'LIVE'))
const upcomingExams = computed(() => allExams.value.filter(e => e.monitoringStatus === 'UPCOMING'))
const filteredLiveExams = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  if (!query) return liveExams.value
  return liveExams.value.filter(exam => `${exam.title || ''} ${exam.code || ''}`.toLowerCase().includes(query))
})
const totalActiveStudents = computed(() => liveExams.value.reduce((sum, exam) => sum + Number(exam.currentSessionParticipants || 0), 0))

function formatRemaining(seconds) {
  if (!seconds && seconds !== 0) return 'Chưa rõ'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  return h > 0 ? `${h}h ${m}p` : `${m} phút`
}

function formatSchedule(value) {
  if (!value) return 'Chưa rõ lịch'
  return new Date(value).toLocaleString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit'
  })
}

function goToMonitoring(exam) {
  router.push(`/teacher/exams/${exam.id}/monitoring`)
}

function goToIncidents(exam) {
  router.push({ path: '/teacher/exams/review/incidents', query: { examId: String(exam.id), title: exam.title || '' } })
}

async function loadExams() {
  isLoading.value = true
  loadError.value = ''
  try {
    allExams.value = await listMonitoringExams()
  } catch (err) {
    loadError.value = err?.message || 'Không thể tải danh sách kỳ thi.'
    toast.error(loadError.value)
  } finally {
    isLoading.value = false
  }
}

onMounted(loadExams)
</script>

<style scoped>
.tm-page {
  min-height: 100vh;
  padding: var(--ds-space-5);
  background: var(--ds-bg);
  color: var(--ds-text);
}

.tm-header,
.tm-row,
.tm-state {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  box-shadow: var(--ds-shadow-sm);
}

.tm-header {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: var(--ds-space-4);
  align-items: center;
  padding: var(--ds-space-5);
}

.tm-back,
.tm-refresh,
.tm-primary,
.tm-secondary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--ds-space-2);
  min-height: 40px;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  font: inherit;
  font-weight: 700;
  cursor: pointer;
  transition: all var(--ds-duration-base) var(--ds-easing);
}

.tm-back {
  width: 40px;
  color: var(--ds-text-secondary);
  background: var(--ds-surface-muted);
}

.tm-refresh,
.tm-secondary {
  padding: 0 var(--ds-space-4);
  color: var(--ds-text);
  background: var(--ds-surface);
}

.tm-primary {
  padding: 0 var(--ds-space-4);
  color: var(--ds-text);
  background: var(--ds-surface-muted);
  border-color: var(--ds-border);
}

.tm-back:hover,
.tm-refresh:hover:not(:disabled),
.tm-secondary:hover {
  color: var(--ds-text);
  border-color: var(--ds-border);
  background: var(--ds-surface-muted);
}

.tm-primary:hover {
  background: var(--ds-surface);
  border-color: var(--ds-border);
}

.tm-refresh:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.tm-title {
  min-width: 0;
}

.tm-title span {
  color: var(--ds-primary);
  font-size: var(--ds-text-xs);
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.tm-title h1 {
  margin: var(--ds-space-1) 0 0;
  font-size: var(--ds-text-2xl);
  line-height: 1.2;
}

.tm-title p,
.tm-state p,
.tm-row__main p,
.tm-row__meta {
  color: var(--ds-text-secondary);
}

.tm-title p {
  margin: var(--ds-space-2) 0 0;
  line-height: 1.5;
}

.tm-inline-stats {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ds-space-2);
  margin-top: var(--ds-space-3);
}

.tm-inline-stats span {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 var(--ds-space-3);
  border: 1px solid var(--ds-border);
  border-radius: 999px;
  color: var(--ds-text-secondary);
  background: var(--ds-surface-muted);
  font-size: var(--ds-text-xs);
  font-weight: 700;
}

.tm-list {
  display: grid;
  gap: var(--ds-space-3);
  margin-top: var(--ds-space-4);
}

.tm-search input {
  width: 100%;
  min-height: 42px;
  padding: 0 var(--ds-space-3);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  color: var(--ds-text);
  background: var(--ds-surface);
  font: inherit;
}

.tm-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: var(--ds-space-4);
  align-items: center;
  padding: var(--ds-space-4);
}

.tm-row__main {
  min-width: 0;
}

.tm-row__main h2 {
  margin: var(--ds-space-2) 0 var(--ds-space-1);
  overflow: hidden;
  font-size: var(--ds-text-lg);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tm-row__main p {
  margin: 0;
  overflow: hidden;
  font-size: var(--ds-text-sm);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tm-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 26px;
  padding: 0 var(--ds-space-3);
  border-radius: var(--ds-radius-full);
  color: var(--ds-text-secondary);
  background: var(--ds-surface-muted);
  font-size: var(--ds-text-xs);
  font-weight: 800;
}

.tm-status i {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: currentColor;
}

.tm-row__meta {
  display: grid;
  gap: var(--ds-space-1);
  font-size: var(--ds-text-sm);
  font-weight: 700;
  white-space: nowrap;
}

.tm-row__actions {
  display: flex;
  gap: var(--ds-space-2);
}

.tm-section {
  display: grid;
  gap: var(--ds-space-3);
  margin-top: var(--ds-space-2);
}

.tm-section h2 {
  margin: var(--ds-space-2) 0 0;
  color: var(--ds-text-secondary);
  font-size: var(--ds-text-sm);
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.tm-row--muted {
  opacity: 0.88;
}

.tm-status--muted {
  color: var(--ds-text-muted);
}

.tm-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--ds-space-3);
  min-height: 320px;
  margin-top: var(--ds-space-4);
  padding: var(--ds-space-6);
  text-align: center;
}

.tm-state h2,
.tm-state p {
  margin: 0;
}

.tm-skeleton {
  height: 108px;
  border-radius: var(--ds-radius-lg);
  background: linear-gradient(90deg, var(--ds-gray-100), var(--ds-gray-200), var(--ds-gray-100));
  background-size: 200% 100%;
  animation: tm-shimmer 1.1s ease-in-out infinite;
}

.tm-spin {
  animation: tm-spin 0.8s linear infinite;
}

@keyframes tm-spin {
  to { transform: rotate(360deg); }
}

@keyframes tm-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@media (max-width: 760px) {
  .tm-page {
    padding: var(--ds-space-3);
  }

  .tm-header,
  .tm-row {
    grid-template-columns: 1fr;
  }

  .tm-back {
    width: 40px;
  }

  .tm-refresh,
  .tm-row__actions,
  .tm-primary,
  .tm-secondary,
  .tm-search {
    width: 100%;
  }

  .tm-row__actions {
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  .tm-back,
  .tm-refresh,
  .tm-primary,
  .tm-secondary,
  .tm-skeleton,
  .tm-spin {
    animation: none;
    transition: none;
  }
}
</style>
