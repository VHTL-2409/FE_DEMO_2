<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto max-w-5xl px-4 pb-10 pt-4 sm:px-6 lg:px-8">

      <!-- Breadcrumb -->
      <div class="mb-4 ds-animate-fade-up">
        <div class="flex items-center gap-2 text-sm" style="color: var(--ds-text-muted)">
          <RouterLink
            class="flex items-center gap-1 transition-colors hover:text-[var(--ds-primary)]"
            style="color: var(--ds-text-muted)"
            to="/teacher/live-monitoring"
          >
            <LucideIcon name="monitor_heart" size="16" />
            Giám sát thi
          </RouterLink>
          <LucideIcon name="chevron_right" size="12" />
          <RouterLink
            class="flex items-center gap-1 transition-colors hover:text-[var(--ds-primary)]"
            style="color: var(--ds-text-muted)"
            :to="{ path: '/teacher/exams/review/incidents', query: { examId } }"
          >
            Tổng quan hành vi
          </RouterLink>
          <LucideIcon name="chevron_right" size="12" />
          <span class="font-medium" style="color: var(--ds-text)">Chi tiết vi phạm</span>
        </div>
      </div>

      <!-- Page Header -->
      <div class="mb-6 ds-animate-fade-up" style="animation-delay: 0.05s">
        <PageHeader
          eyebrow="Theo dõi vi phạm"
          title="Chi tiết hành vi học sinh"
          :subtitle="'Phân tích vi phạm của ' + studentInfo.name"
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
                class="relative flex size-14 items-center justify-center rounded-full text-xl font-bold"
                style="background-color: var(--ds-danger); color: white"
              >
                {{ studentInitials }}
                <span
                  class="absolute -bottom-0.5 -right-0.5 flex size-4 items-center justify-center rounded-full"
                  :style="{
                    backgroundColor: getRiskColor(violationData.overallRiskScore),
                    color: 'white'
                  }"
                >
                  <LucideIcon name="warning" size="12" />
                </span>
              </div>
              <div>
                <h2 class="text-xl font-bold" style="color: var(--ds-text)">{{ studentInfo.name }}</h2>
                <p class="text-sm" style="color: var(--ds-text-muted)">Mã học sinh: {{ studentInfo.studentId }}</p>
                <p class="text-xs" style="color: var(--ds-text-secondary)">{{ studentInfo.email }}</p>
              </div>
            </div>
            <StatusChip :status="violationData.riskLevel" :label="violationData.riskLabel" />
          </div>
        </DsCard>
      </div>

      <!-- Risk Score Indicator -->
      <div class="mb-6 ds-animate-fade-up" style="animation-delay: 0.15s">
        <DsCard padding="lg" variant="alert">
          <div class="flex items-center gap-4">
            <div class="flex size-16 items-center justify-center rounded-full" :style="{ backgroundColor: getRiskBgColor(violationData.overallRiskScore) }">
              <span class="text-3xl font-extrabold" :style="{ color: getRiskColor(violationData.overallRiskScore) }">
                {{ violationData.overallRiskScore }}
              </span>
            </div>
            <div class="flex-1">
              <p class="text-sm font-semibold" style="color: var(--ds-text)">Điểm rủi ro tổng thể</p>
              <p class="text-xs" style="color: var(--ds-text-muted)">
                {{ violationData.riskDescription }}
              </p>
              <div class="mt-2 h-2 w-full overflow-hidden rounded-full" style="background-color: var(--ds-gray-100)">
                <div
                  class="h-full rounded-full transition-all duration-500"
                  :style="{
                    width: violationData.overallRiskScore + '%',
                    backgroundColor: getRiskColor(violationData.overallRiskScore)
                  }"
                />
              </div>
            </div>
          </div>
        </DsCard>
      </div>

      <!-- Session Summary -->
      <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4 ds-animate-fade-up" style="animation-delay: 0.2s">
        <DsStatCard
          :label="'Thời gian tham gia'"
          :value="sessionSummary.timeJoined"
          :icon="'schedule'"
          :sub="'Bắt đầu lúc ' + sessionSummary.startTime"
        />
        <DsStatCard
          :label="'Số lần chuyển tab'"
          :value="sessionSummary.tabSwitches"
          :icon="'tab'"
          :badge="'Cảnh báo'"
          :badge-variant="sessionSummary.tabSwitches > 3 ? 'warning' : 'info'"
        />
        <DsStatCard
          :label="'Số vi phạm'"
          :value="sessionSummary.totalViolations"
          :icon="'flag'"
          :sub="'Mức nghiêm trọng: ' + sessionSummary.severity"
        />
        <DsStatCard
          :label="'Trạng thái'"
          :value="sessionSummary.status"
          :icon="'info'"
          :badge="sessionSummary.statusBadge"
          :badge-variant="sessionSummary.statusVariant"
        />
      </div>

      <!-- Violation Events Timeline -->
      <div class="mb-6 ds-animate-fade-up" style="animation-delay: 0.25s">
        <DsCard padding="none">
          <template #header>
            <div class="flex items-center justify-between px-5 pt-5">
              <div class="flex items-center gap-2">
                <LucideIcon name="timeline" />
                <h3 class="text-lg font-bold" style="color: var(--ds-text)">Dòng thời gian vi phạm</h3>
              </div>
              <span
                class="rounded-full px-3 py-1 text-xs font-bold"
                style="background-color: var(--ds-danger-bg); color: var(--ds-danger)"
              >
                {{ violationEvents.length }} sự kiện
              </span>
            </div>
          </template>
          <DataTable
            :columns="violationColumns"
            :data="violationEvents"
            :row-key="'id'"
          >
            <template #cell-timestamp="{ value }">
              <span class="text-sm" style="color: var(--ds-text)">
                {{ formatTimestamp(value) }}
              </span>
            </template>
            <template #cell-type="{ value }">
              <div class="flex items-center gap-2">
              <LucideIcon :name="getViolationIcon(value)" size="20" :style="{ color: getViolationColor(value) }" />
                <span class="text-sm font-medium" style="color: var(--ds-text)">{{ value }}</span>
              </div>
            </template>
            <template #cell-severity="{ value }">
              <StatusChip :status="value" :label="getSeverityLabel(value)" />
            </template>
            <template #cell-description="{ value }">
              <p class="text-sm" style="color: var(--ds-text-secondary)">{{ value }}</p>
            </template>
          </DataTable>
        </DsCard>
      </div>

      <!-- Suspicious Patterns -->
      <div class="mb-6 ds-animate-fade-up" style="animation-delay: 0.3s">
        <DsCard padding="lg">
          <template #header>
            <div class="flex items-center gap-2">
              <LucideIcon name="psychology" />
              <h3 class="text-lg font-bold" style="color: var(--ds-text)">Mẫu hành vi đáng ngờ</h3>
            </div>
          </template>
          <div class="space-y-3">
            <div
              v-for="pattern in suspiciousPatterns"
              :key="pattern.id"
              class="flex items-start gap-3 rounded-lg border p-3"
              :style="{
                borderColor: pattern.level === 'high' ? 'var(--ds-danger)' : pattern.level === 'medium' ? 'var(--ds-warning)' : 'var(--ds-info)',
                backgroundColor: pattern.level === 'high' ? 'var(--ds-danger-soft)' : pattern.level === 'medium' ? 'var(--ds-warning-soft)' : 'var(--ds-info-soft)'
              }"
            >
              <LucideIcon :name="pattern.icon" size="20" class="shrink-0 mt-0.5" :style="{ color: pattern.level === 'high' ? 'var(--ds-danger)' : pattern.level === 'medium' ? 'var(--ds-warning)' : 'var(--ds-info)' }" />
              <div class="flex-1">
                <p class="text-sm font-semibold" style="color: var(--ds-text)">{{ pattern.title }}</p>
                <p class="mt-0.5 text-xs" style="color: var(--ds-text-secondary)">{{ pattern.description }}</p>
              </div>
              <span
                class="rounded px-2 py-0.5 text-[10px] font-bold uppercase"
                :style="{
                  backgroundColor: pattern.level === 'high' ? 'var(--ds-danger)' : pattern.level === 'medium' ? 'var(--ds-warning)' : 'var(--ds-info)',
                  color: 'white'
                }"
              >
                {{ pattern.level }}
              </span>
            </div>
          </div>
        </DsCard>
      </div>

      <!-- Action Buttons -->
      <div class="flex flex-wrap justify-end gap-3 ds-animate-fade-up" style="animation-delay: 0.35s">
        <button
          type="button"
          class="inline-flex items-center justify-center gap-2 rounded-lg px-5 py-2.5 text-sm font-bold transition-all hover:-translate-y-0.5"
          style="background-color: var(--ds-surface); color: var(--ds-warning); border: 1px solid var(--ds-warning); box-shadow: var(--ds-shadow-sm)"
          @click="issueWarning"
        >
          <LucideIcon name="warning" size="18" />
          <span>Gửi cảnh báo</span>
        </button>
        <button
          type="button"
          class="inline-flex items-center justify-center gap-2 rounded-lg px-5 py-2.5 text-sm font-bold transition-all hover:-translate-y-0.5"
          style="background-color: var(--ds-surface); color: var(--ds-danger); border: 1px solid var(--ds-danger); box-shadow: var(--ds-shadow-sm)"
          @click="suspendExam"
        >
          <LucideIcon name="block" size="18" />
          <span>Tạm dừng thi</span>
        </button>
        <button
          type="button"
          class="inline-flex items-center justify-center gap-2 rounded-lg px-5 py-2.5 text-sm font-bold transition-all hover:-translate-y-0.5"
          style="background-color: var(--ds-primary); color: white; box-shadow: var(--ds-shadow-sm)"
          @click="viewFullReport"
        >
          <LucideIcon name="description" size="18" />
          <span>Xem báo cáo đầy đủ</span>
        </button>
      </div>

    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import PageHeader from '../ui/PageHeader.vue'
import DsCard from '../ui/DsCard.vue'
import DsStatCard from '../ui/DsStatCard.vue'
import DataTable from '../ui/DataTable.vue'
import StatusChip from '../ui/StatusChip.vue'

const router = useRouter()
const route = useRoute()

const examId = computed(() => route.query.examId || '')
const studentId = computed(() => route.query.studentId || '')

// Mock student data
const studentInfo = computed(() => ({
  name: 'Phạm Văn Bình',
  studentId: 'SV2024023',
  email: 'phamvanbinh@student.edu.vn'
}))

const studentInitials = computed(() => {
  const parts = studentInfo.value.name.split(' ')
  return parts[parts.length - 1].charAt(0) + parts[0].charAt(0)
})

const violationData = computed(() => ({
  overallRiskScore: 78,
  riskLevel: 'warning',
  riskLabel: 'Nguy cơ cao',
  riskDescription: 'Học sinh có nhiều hành vi bất thường trong quá trình làm bài thi. Cần được giám sát kỹ lưỡng.'
}))

const sessionSummary = computed(() => ({
  timeJoined: '15 phút',
  startTime: '14:30',
  tabSwitches: 7,
  totalViolations: 5,
  severity: 'Trung bình',
  status: 'Đang thi',
  statusBadge: 'Đang thi',
  statusVariant: 'warning'
}))

const violationColumns = [
  { key: 'timestamp', label: 'Thời gian', width: '160px' },
  { key: 'type', label: 'Loại vi phạm' },
  { key: 'description', label: 'Mô tả' },
  { key: 'severity', label: 'Mức độ', width: '100px' }
]

const violationEvents = computed(() => [
  {
    id: 1,
    timestamp: '2026-03-29T14:32:00',
    type: 'Chuyển tab',
    description: 'Chuyển từ cửa sổ thi sang tab trình duyệt khác',
    severity: 'warning'
  },
  {
    id: 2,
    timestamp: '2026-03-29T14:35:00',
    type: 'Copy nội dung',
    description: 'Cố gắng sao chép nội dung câu hỏi',
    severity: 'warning'
  },
  {
    id: 3,
    timestamp: '2026-03-29T14:38:00',
    type: 'Chuyển tab',
    description: 'Chuyển sang ứng dụng khác',
    severity: 'warning'
  },
  {
    id: 4,
    timestamp: '2026-03-29T14:40:00',
    type: 'Phát hiện khuôn mặt',
    description: 'Không phát hiện khuôn mặt trong khung hình quá 30 giây',
    severity: 'error'
  },
  {
    id: 5,
    timestamp: '2026-03-29T14:45:00',
    type: 'Chuyển tab',
    description: 'Chuyển sang cửa sổ trình duyệt mới',
    severity: 'warning'
  }
])

const suspiciousPatterns = computed(() => [
  {
    id: 1,
    icon: 'tab',
    title: 'Chuyển tab nhiều lần',
    description: 'Đã chuyển tab 7 lần trong 15 phút, vượt ngưỡng bình thường',
    level: 'high'
  },
  {
    id: 2,
    icon: 'content_copy',
    title: 'Cố gắng copy nội dung',
    description: 'Phát hiện hành vi sao chép dữ liệu từ đề thi',
    level: 'medium'
  },
  {
    id: 3,
    icon: 'face',
    title: 'Mất khuôn mặt',
    description: 'Khuôn mặt không xuất hiện trong khung hình quá lâu',
    level: 'medium'
  }
])

const getRiskColor = (score) => {
  if (score >= 70) return 'var(--ds-danger)'
  if (score >= 40) return 'var(--ds-warning)'
  return 'var(--ds-success)'
}

const getRiskBgColor = (score) => {
  if (score >= 70) return 'var(--ds-danger-soft)'
  if (score >= 40) return 'var(--ds-warning-soft)'
  return 'var(--ds-success-soft)'
}

const getViolationColor = (type) => {
  const colors = {
    'Chuyển tab': 'var(--ds-warning)',
    'Copy nội dung': 'var(--ds-danger)',
    'Phát hiện khuôn mặt': 'var(--ds-danger)'
  }
  return colors[type] || 'var(--ds-text-muted)'
}

const getViolationIcon = (type) => {
  const icons = {
    'Chuyển tab': 'tab',
    'Copy nội dung': 'content_copy',
    'Phát hiện khuôn mặt': 'face'
  }
  return icons[type] || 'warning'
}

const getSeverityLabel = (status) => {
  const labels = {
    warning: 'Cảnh báo',
    error: 'Nghiêm trọng',
    info: 'Thông tin'
  }
  return labels[status] || status
}

const formatTimestamp = (timestamp) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const goBack = () => {
  router.back()
}

const issueWarning = () => {
  // Issue warning action
}

const suspendExam = () => {
  // Suspend exam action
}

const viewFullReport = () => {
  // View full report action
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
