<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto max-w-7xl px-4 pb-10 pt-4 sm:px-6 lg:px-8">

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
            :to="{ path: '/teacher/exams/review/summary', query: { examId, title: examTitle } }"
          >
            Tổng quan điểm
          </RouterLink>
          <LucideIcon name="chevron_right" size="12" />
          <span class="font-medium" style="color: var(--ds-text)">Chi tiết học sinh</span>
        </div>
      </div>

      <!-- Page Header -->
      <div class="mb-6 ds-animate-fade-up" style="animation-delay: 0.05s">
        <PageHeader
          :eyebrow="'Báo cáo chi tiết'"
          :title="examTitle"
          :subtitle="'Xem chi tiết kết quả làm bài của học sinh'"
        >
          <template #actions>
            <button
              type="button"
              class="inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-bold transition-all hover:-translate-y-0.5"
              style="background-color: var(--ds-surface); color: var(--ds-text); border: 1px solid var(--ds-border); box-shadow: var(--ds-shadow-sm)"
              @click="goBack"
            >
              <LucideIcon name="arrow_back" size="18" />
              <span>Quay lại</span>
            </button>
          </template>
        </PageHeader>
      </div>

      <!-- Student Info Header -->
      <div class="mb-6 ds-animate-fade-up" style="animation-delay: 0.1s">
        <DsCard padding="lg">
          <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div class="flex items-center gap-4">
              <div
                class="flex size-14 items-center justify-center rounded-full text-xl font-bold"
                style="background-color: var(--ds-primary); color: white"
              >
                {{ studentInitials }}
              </div>
              <div>
                <h2 class="text-xl font-bold" style="color: var(--ds-text)">{{ studentInfo.name }}</h2>
                <p class="text-sm" style="color: var(--ds-text-muted)">Mã học sinh: {{ studentInfo.studentId }}</p>
                <p class="text-xs" style="color: var(--ds-text-secondary)">{{ studentInfo.email }}</p>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <StatusChip :status="studentInfo.statusKey" :label="studentInfo.status" />
            </div>
          </div>
        </DsCard>
      </div>

      <!-- Score Summary Cards -->
      <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4 ds-animate-fade-up" style="animation-delay: 0.15s">
        <DsStatCard
          :label="'Điểm tổng'"
          :value="scoreSummary.totalScore"
          :sub-value="'/ 10'"
          :icon="'grade'"
          :badge="'Điểm'"
          :badge-variant="scoreSummary.badgeVariant"
        />
        <DsStatCard
          :label="'Độ chính xác'"
          :value="scoreSummary.accuracy"
          :sub-value="'%'"
          :icon="'verified'"
          :sub="scoreSummary.accuracyNote"
        />
        <DsStatCard
          :label="'Thời gian làm bài'"
          :value="scoreSummary.timeSpent"
          :icon="'timer'"
          :sub="'Tối đa: ' + scoreSummary.maxTime"
        />
        <DsStatCard
          :label="'Xếp hạng'"
          :value="scoreSummary.rank"
          :sub-value="'trên ' + scoreSummary.totalStudents"
          :icon="'leaderboard'"
          :sub="'Lớp ' + scoreSummary.className"
        />
      </div>

      <!-- Questions Review Table -->
      <div class="mb-6 ds-animate-fade-up" style="animation-delay: 0.2s">
        <DsCard padding="none">
          <template #header>
            <div class="flex items-center justify-between px-5 pt-5">
              <div class="flex items-center gap-2">
                <LucideIcon name="quiz" />
                <h3 class="text-lg font-bold" style="color: var(--ds-text)">Chi tiết câu hỏi</h3>
              </div>
              <div class="flex items-center gap-4 text-sm">
                <span class="flex items-center gap-1.5">
                  <span class="size-2 rounded-full" style="background-color: var(--ds-success)"></span>
                  <span style="color: var(--ds-text-muted)">Đúng</span>
                </span>
                <span class="flex items-center gap-1.5">
                  <span class="size-2 rounded-full" style="background-color: var(--ds-danger)"></span>
                  <span style="color: var(--ds-text-muted)">Sai</span>
                </span>
                <span class="flex items-center gap-1.5">
                  <span class="size-2 rounded-full" style="background-color: var(--ds-warning)"></span>
                  <span style="color: var(--ds-text-muted)">Bỏ qua</span>
                </span>
              </div>
            </div>
          </template>
          <DataTable
            :columns="questionColumns"
            :data="questionsData"
            :row-key="'id'"
          >
            <template #cell-status="{ row }">
              <span
                class="inline-flex items-center justify-center size-7 rounded-full text-xs font-bold"
                :style="{
                  backgroundColor: getStatusBg(row.status),
                  color: getStatusColor(row.status)
                }"
              >
                <LucideIcon :name="getStatusIcon(row.status)" size="14" />
              </span>
            </template>
            <template #cell-selectedAnswer="{ row }">
              <span v-if="row.selectedAnswer" class="text-sm" style="color: var(--ds-text)">
                {{ row.selectedAnswer }}
              </span>
              <span v-else class="text-sm italic" style="color: var(--ds-text-muted)">Không chọn</span>
            </template>
            <template #cell-correctAnswer="{ row }">
              <span class="text-sm font-semibold" style="color: var(--ds-success)">
                {{ row.correctAnswer }}
              </span>
            </template>
          </DataTable>
        </DsCard>
      </div>

      <!-- Answer Distribution Chart (optional) -->
      <div class="ds-animate-fade-up" style="animation-delay: 0.25s">
        <DsCard padding="lg">
          <template #header>
            <div class="flex items-center gap-2">
              <LucideIcon name="pie_chart" />
              <h3 class="text-lg font-bold" style="color: var(--ds-text)">Phân bố câu trả lời</h3>
            </div>
          </template>
          <div class="grid grid-cols-3 gap-4 text-center">
            <div
              class="rounded-lg p-4"
              style="background-color: rgba(34, 197, 94, 0.06); border: 1px solid rgba(22, 163, 74, 0.15)"
            >
              <p class="text-2xl font-bold" style="color: #16a34a">{{ answerStats.correct }}</p>
              <p class="text-sm font-medium" style="color: #16a34a">Câu đúng</p>
            </div>
            <div
              class="rounded-lg p-4"
              style="background-color: rgba(220, 38, 38, 0.06); border: 1px solid rgba(220, 38, 38, 0.15)"
            >
              <p class="text-2xl font-bold" style="color: #dc2626">{{ answerStats.wrong }}</p>
              <p class="text-sm font-medium" style="color: #dc2626">Câu sai</p>
            </div>
            <div
              class="rounded-lg p-4"
              style="background-color: rgba(245, 158, 11, 0.06); border: 1px solid rgba(245, 158, 11, 0.15)"
            >
              <p class="text-2xl font-bold" style="color: #d97706">{{ answerStats.skipped }}</p>
              <p class="text-sm font-medium" style="color: #d97706">Bỏ qua</p>
            </div>
          </div>
        </DsCard>
      </div>

    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import PageHeader from '../ui/PageHeader.vue'
import DsCard from '../ui/DsCard.vue'
import DsStatCard from '../ui/DsStatCard.vue'
import DataTable from '../ui/DataTable.vue'
import StatusChip from '../ui/StatusChip.vue'

const router = useRouter()
const route = useRoute()

const examId = computed(() => route.query.examId || '')
const examTitle = computed(() => route.query.title || 'Đề thi')

// Mock student data
const studentInfo = computed(() => ({
  name: 'Trần Minh Đức',
  studentId: 'SV2024001',
  email: 'tranminhduc@student.edu.vn',
  status: 'Hoàn thành',
  statusKey: 'active'
}))

const studentInitials = computed(() => {
  const parts = studentInfo.value.name.split(' ')
  return parts[parts.length - 1].charAt(0) + parts[0].charAt(0)
})

const scoreSummary = computed(() => {
  const totalScore = 8.5
  const badgeVariant = totalScore >= 8 ? 'success' : totalScore >= 5 ? 'warning' : 'danger'
  return {
    totalScore: totalScore.toFixed(1),
    badgeVariant,
    accuracy: '85',
    accuracyNote: '17/20 câu',
    timeSpent: '15 phút',
    maxTime: '30 phút',
    rank: '3',
    totalStudents: '45',
    className: '22IT1'
  }
})

const answerStats = computed(() => ({
  correct: 17,
  wrong: 2,
  skipped: 1
}))

const questionColumns = [
  { key: 'number', label: '#', width: '60px', align: 'center' },
  { key: 'content', label: 'Câu hỏi' },
  { key: 'selectedAnswer', label: 'Câu trả lời của bạn' },
  { key: 'correctAnswer', label: 'Đáp án đúng' },
  { key: 'status', label: 'Kết quả', width: '80px', align: 'center' }
]

const questionsData = computed(() => [
  {
    id: 1,
    number: 1,
    content: 'Giải phương trình: x² - 5x + 6 = 0',
    selectedAnswer: 'A. x = 2 hoặc x = 3',
    correctAnswer: 'A. x = 2 hoặc x = 3',
    status: 'correct'
  },
  {
    id: 2,
    number: 2,
    content: 'Đạo hàm của f(x) = x³ + 2x là?',
    selectedAnswer: 'B. 3x² + 2',
    correctAnswer: 'B. 3x² + 2',
    status: 'correct'
  },
  {
    id: 3,
    number: 3,
    content: 'Tích phân ∫x dx = ?',
    selectedAnswer: 'D. x²/2 + C',
    correctAnswer: 'C. x²/2',
    status: 'wrong'
  },
  {
    id: 4,
    number: 4,
    content: 'Giới hạn lim(x→0) sin(x)/x = ?',
    selectedAnswer: 'A. 1',
    correctAnswer: 'A. 1',
    status: 'correct'
  },
  {
    id: 5,
    number: 5,
    content: 'Ma trận đơn vị cấp 3 có dạng?',
    selectedAnswer: 'B. Ma trận có đường chéo chính bằng 1',
    correctAnswer: 'B. Ma trận có đường chéo chính bằng 1',
    status: 'correct'
  },
  {
    id: 6,
    number: 6,
    content: 'Số phức liên hợp của z = 3 + 4i là?',
    selectedAnswer: 'Không chọn',
    correctAnswer: 'C. 3 - 4i',
    status: 'skipped'
  },
  {
    id: 7,
    number: 7,
    content: 'Diện tích hình tròn bán kính r là?',
    selectedAnswer: 'B. πr²',
    correctAnswer: 'B. πr²',
    status: 'correct'
  },
  {
    id: 8,
    number: 8,
    content: 'Nghiệm của phương trình 2ˣ = 8 là?',
    selectedAnswer: 'C. x = 3',
    correctAnswer: 'C. x = 3',
    status: 'correct'
  },
  {
    id: 9,
    number: 9,
    content: 'Vector (1, 2, 3) có độ dài là?',
    selectedAnswer: 'A. √14',
    correctAnswer: 'B. √12',
    status: 'wrong'
  },
  {
    id: 10,
    number: 10,
    content: 'Hàm số f(x) = eˣ có đạo hàm là?',
    selectedAnswer: 'C. eˣ',
    correctAnswer: 'C. eˣ',
    status: 'correct'
  }
])

const getStatusBg = (status) => {
  const bgs = {
    correct: 'var(--ds-success-bg)',
    wrong: 'var(--ds-danger-bg)',
    skipped: 'var(--ds-warning-bg)'
  }
  return bgs[status] || 'var(--ds-gray-100)'
}

const getStatusColor = (status) => {
  const colors = {
    correct: 'var(--ds-success)',
    wrong: 'var(--ds-danger)',
    skipped: 'var(--ds-warning)'
  }
  return colors[status] || 'var(--ds-text-muted)'
}

const getStatusIcon = (status) => {
  const icons = {
    correct: 'check',
    wrong: 'close',
    skipped: 'remove'
  }
  return icons[status] || 'help'
}

const goBack = () => {
  router.back()
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
