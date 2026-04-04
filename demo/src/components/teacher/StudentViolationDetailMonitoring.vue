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

      <!-- Loading state -->
      <div v-if="loading" class="flex items-center justify-center py-20">
        <div class="h-8 w-8 animate-spin rounded-full border-4 border-[var(--ds-primary)] border-t-transparent"></div>
        <span class="ml-3" style="color: var(--ds-text-muted)">Đang tải dữ liệu...</span>
      </div>

      <!-- Error state -->
      <div v-else-if="error" class="rounded-lg border border-[var(--ds-danger)] bg-[var(--ds-danger-soft)] p-4 text-center">
        <LucideIcon name="alert_circle" size="24" class="mx-auto mb-2" style="color: var(--ds-danger)" />
        <p style="color: var(--ds-danger)">{{ error }}</p>
        <button class="mt-3 rounded-lg px-4 py-2 text-sm font-bold transition-all hover:-translate-y-0.5" style="background-color: var(--ds-primary); color: white" @click="loadData">
          Thử lại
        </button>
      </div>

      <template v-else>
        <!-- Page Header -->
        <div class="mb-6 ds-animate-fade-up" style="animation-delay: 0.05s">
          <PageHeader
            eyebrow="Theo dõi vi phạm"
            title="Chi tiết hành vi học sinh"
            :subtitle="'Phân tích vi phạm của ' + (studentInfo.name || '—')"
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
                  :style="{ backgroundColor: riskColor }"
                  style="color: white"
                >
                  {{ studentInitials }}
                  <span
                    class="absolute -bottom-0.5 -right-0.5 flex size-4 items-center justify-center rounded-full"
                    :style="{ backgroundColor: riskColor, color: 'white' }"
                  >
                    <LucideIcon name="warning" size="12" />
                  </span>
                </div>
                <div>
                  <h2 class="text-xl font-bold" style="color: var(--ds-text)">{{ studentInfo.name || '—' }}</h2>
                  <p class="text-sm" style="color: var(--ds-text-muted)">Mã học sinh: {{ studentInfo.studentId || '—' }}</p>
                  <p class="text-xs" style="color: var(--ds-text-secondary)">{{ studentInfo.email || '—' }}</p>
                </div>
              </div>
              <StatusChip :status="riskLevelBadge" :label="riskLevelLabel" />
            </div>
          </DsCard>
        </div>

        <!-- Risk Score Indicator -->
        <div class="mb-6 ds-animate-fade-up" style="animation-delay: 0.15s">
          <DsCard padding="lg" variant="alert">
            <div class="flex items-center gap-4">
              <div
                class="flex size-16 items-center justify-center rounded-full"
                :style="{ backgroundColor: riskBgColor }"
              >
                <span class="text-3xl font-extrabold" :style="{ color: riskColor }">
                  {{ riskData.score ?? 0 }}
                </span>
              </div>
              <div class="flex-1">
                <p class="text-sm font-semibold" style="color: var(--ds-text)">Điểm rủi ro tổng thể</p>
                <p class="text-xs" style="color: var(--ds-text-muted)">
                  {{ riskDescription }}
                </p>
                <div class="mt-2 h-2 w-full overflow-hidden rounded-full" style="background-color: var(--ds-gray-100)">
                  <div
                    class="h-full rounded-full transition-all duration-500"
                    :style="{
                      width: (riskData.score ?? 0) + '%',
                      backgroundColor: riskColor
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
            <div v-if="violationEvents.length === 0" class="py-10 text-center" style="color: var(--ds-text-muted)">
              <LucideIcon name="check_circle" size="32" class="mx-auto mb-2" style="color: var(--ds-success)" />
              <p>Không có vi phạm nào được ghi nhận</p>
            </div>
            <DataTable
              v-else
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
                  <span class="text-sm font-medium" style="color: var(--ds-text)">{{ getViolationLabel(value) }}</span>
                </div>
              </template>
              <template #cell-severity="{ value }">
                <StatusChip :status="getSeverityStatus(value)" :label="getSeverityLabel(value)" />
              </template>
              <template #cell-description="{ value }">
                <p class="text-sm" style="color: var(--ds-text-secondary)">{{ value }}</p>
              </template>
            </DataTable>
          </DsCard>
        </div>

        <!-- Risk Breakdown -->
        <div v-if="riskData.breakdown && Object.keys(riskData.breakdown).length > 0" class="mb-6 ds-animate-fade-up" style="animation-delay: 0.28s">
          <DsCard padding="lg">
            <template #header>
              <div class="flex items-center gap-2">
                <LucideIcon name="analytics" />
                <h3 class="text-lg font-bold" style="color: var(--ds-text)">Phân tích điểm rủi ro</h3>
              </div>
            </template>
            <div class="space-y-3">
              <div
                v-for="(score, signalType) in riskData.breakdown"
                :key="signalType"
                class="flex items-center justify-between rounded-lg border p-3"
                :style="{ borderColor: 'var(--ds-border)' }"
              >
                <div class="flex items-center gap-2">
                  <LucideIcon
                    :name="getViolationIcon(signalType)"
                    size="18"
                    :style="{ color: signalScoreColor(score) }"
                  />
                  <span class="text-sm font-medium" style="color: var(--ds-text)">{{ getViolationLabel(signalType) }}</span>
                </div>
                <div class="flex items-center gap-2">
                  <div class="h-2 w-24 overflow-hidden rounded-full" style="background-color: var(--ds-gray-100)">
                    <div
                      class="h-full rounded-full"
                      :style="{ width: Math.min(score, 100) + '%', backgroundColor: signalScoreColor(score) }"
                    />
                  </div>
                  <span class="text-sm font-bold" :style="{ color: signalScoreColor(score) }">{{ score }}</span>
                </div>
              </div>
            </div>
          </DsCard>
        </div>

        <!-- Suspicious Patterns (from signals) -->
        <div v-if="suspiciousPatterns.length > 0" class="mb-6 ds-animate-fade-up" style="animation-delay: 0.3s">
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
                <LucideIcon
                  :name="pattern.icon"
                  size="20"
                  class="shrink-0 mt-0.5"
                  :style="{ color: pattern.level === 'high' ? 'var(--ds-danger)' : pattern.level === 'medium' ? 'var(--ds-warning)' : 'var(--ds-info)' }"
                />
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
            :disabled="actionLoading === 'warning'"
            @click="issueWarning"
          >
            <LucideIcon name="warning" size="18" />
            <span>{{ actionLoading === 'warning' ? 'Đang gửi...' : 'Gửi cảnh báo' }}</span>
          </button>
          <button
            type="button"
            class="inline-flex items-center justify-center gap-2 rounded-lg px-5 py-2.5 text-sm font-bold transition-all hover:-translate-y-0.5"
            style="background-color: var(--ds-surface); color: var(--ds-danger); border: 1px solid var(--ds-danger); box-shadow: var(--ds-shadow-sm)"
            :disabled="actionLoading === 'invalidate'"
            @click="suspendExam"
          >
            <LucideIcon name="block" size="18" />
            <span>{{ actionLoading === 'invalidate' ? 'Đang xử lý...' : 'Tạm dừng thi' }}</span>
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

        <!-- Warning Dialog -->
        <div v-if="showWarningDialog" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div class="w-full max-w-md rounded-xl p-6" style="background-color: var(--ds-surface)">
            <h3 class="mb-4 text-lg font-bold" style="color: var(--ds-text)">Gửi cảnh báo đến học sinh</h3>
            <textarea
              v-model="warningMessage"
              class="w-full rounded-lg border p-3 text-sm focus:outline-none focus:ring-2"
              style="border-color: var(--ds-border); background-color: var(--ds-bg); color: var(--ds-text)"
              rows="3"
              placeholder="Nhập nội dung cảnh báo (để trống = cảnh báo mặc định)"
            />
            <div class="mt-4 flex justify-end gap-3">
              <button
                class="rounded-lg px-4 py-2 text-sm font-bold"
                style="background-color: var(--ds-bg); color: var(--ds-text); border: 1px solid var(--ds-border)"
                @click="showWarningDialog = false"
              >
                Hủy
              </button>
              <button
                class="rounded-lg px-4 py-2 text-sm font-bold"
                style="background-color: var(--ds-warning); color: white"
                @click="confirmSendWarning"
              >
                Gửi
              </button>
            </div>
          </div>
        </div>

        <!-- Invalidate Dialog -->
        <div v-if="showInvalidateDialog" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div class="w-full max-w-md rounded-xl p-6" style="background-color: var(--ds-surface)">
            <h3 class="mb-2 text-lg font-bold" style="color: var(--ds-danger)">Xác nhận dừng thi</h3>
            <p class="mb-4 text-sm" style="color: var(--ds-text-muted)">
              Hành động này sẽ dừng bài thi của học sinh. Học sinh sẽ không thể tiếp tục làm bài.
            </p>
            <textarea
              v-model="invalidateReason"
              class="w-full rounded-lg border p-3 text-sm focus:outline-none focus:ring-2"
              style="border-color: var(--ds-border); background-color: var(--ds-bg); color: var(--ds-text)"
              rows="2"
              placeholder="Lý do dừng thi (để trống = lý do mặc định)"
            />
            <div class="mt-4 flex justify-end gap-3">
              <button
                class="rounded-lg px-4 py-2 text-sm font-bold"
                style="background-color: var(--ds-bg); color: var(--ds-text); border: 1px solid var(--ds-border)"
                @click="showInvalidateDialog = false"
              >
                Hủy
              </button>
              <button
                class="rounded-lg px-4 py-2 text-sm font-bold"
                style="background-color: var(--ds-danger); color: white"
                @click="confirmInvalidate"
              >
                Dừng thi
              </button>
            </div>
          </div>
        </div>

      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import { useToast } from '../../composables/useToast'
import PageHeader from '../ui/PageHeader.vue'
import DsCard from '../ui/DsCard.vue'
import DsStatCard from '../ui/DsStatCard.vue'
import DataTable from '../ui/DataTable.vue'
import StatusChip from '../ui/StatusChip.vue'
import {
  fetchAttemptRisk,
  listMonitoringTimeline,
  listMonitoringAudit,
  sendTeacherWarning,
  invalidateAttempt
} from '../../services/monitoringService'

const router = useRouter()
const route = useRoute()
const toast = useToast()

const attemptId = computed(() => route.query.attemptId || '')
const examId = computed(() => route.query.examId || '')
const studentId = computed(() => route.query.studentId || '')

const loading = ref(false)
const error = ref('')
const actionLoading = ref('')
const showWarningDialog = ref(false)
const showInvalidateDialog = ref(false)
const warningMessage = ref('')
const invalidateReason = ref('')

const riskData = ref({})
const timeline = ref([])
const auditLog = ref([])

const violationColumns = [
  { key: 'at', label: 'Thời gian', width: '160px' },
  { key: 'eventType', label: 'Loại vi phạm' },
  { key: 'details', label: 'Mô tả' },
  { key: 'severity', label: 'Mức độ', width: '100px' }
]

const studentInfo = computed(() => {
  const student = riskData.value.student
  if (!student) return { name: '', studentId: '', email: '' }
  return {
    name: student.name || student.fullName || student.username || '—',
    studentId: student.studentCode || student.studentId || student.id || '—',
    email: student.email || '—'
  }
})

const studentInitials = computed(() => {
  const name = studentInfo.value.name
  if (!name || name === '—') return '?'
  const parts = name.split(' ')
  return parts[parts.length - 1].charAt(0).toUpperCase() + parts[0].charAt(0).toUpperCase()
})

const riskLevelBadge = computed(() => {
  const level = riskData.value.level
  return level === 'HIGH_RISK' || level === 'CRITICAL' ? 'error'
    : level === 'SUSPICIOUS' ? 'warning'
    : 'success'
})

const riskLevelLabel = computed(() => {
  const level = riskData.value.level
  return level === 'CRITICAL' ? 'Nguy cơ cao'
    : level === 'HIGH_RISK' ? 'Rủi ro cao'
    : level === 'SUSPICIOUS' ? 'Đáng ngờ'
    : 'Bình thường'
})

const riskDescription = computed(() => {
  const score = riskData.value.score ?? 0
  return score >= 80 ? 'Nguy cơ cao — hành vi gian lận rõ ràng'
    : score >= 60 ? 'Rủi ro cao — cần giám sát kỹ lưỡng'
    : score >= 30 ? 'Đáng ngờ — có một số hành vi bất thường'
    : 'Bình thường — không có dấu hiệu bất thường'
})

const riskColor = computed(() => {
  const score = riskData.value.score ?? 0
  return score >= 60 ? 'var(--ds-danger)'
    : score >= 30 ? 'var(--ds-warning)'
    : 'var(--ds-success)'
})

const riskBgColor = computed(() => {
  const score = riskData.value.score ?? 0
  return score >= 60 ? 'var(--ds-danger-soft)'
    : score >= 30 ? 'var(--ds-warning-soft)'
    : 'var(--ds-success-soft)'
})

const sessionSummary = computed(() => {
  const attempt = riskData.value.attempt
  const startedAt = attempt?.startedAt
  let timeJoined = '—'
  let startTime = '—'
  if (startedAt) {
    startTime = new Date(startedAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
    const diff = Math.floor((Date.now() - new Date(startedAt).getTime()) / 60000)
    if (diff < 60) timeJoined = diff + ' phút'
    else timeJoined = Math.floor(diff / 60) + 'h ' + (diff % 60) + 'p'
  }
  const status = attempt?.status || 'UNKNOWN'
  const statusMap = {
    IN_PROGRESS: { label: 'Đang thi', badge: 'Đang thi', variant: 'warning' },
    PAUSED: { label: 'Tạm dừng', badge: 'Tạm dừng', variant: 'warning' },
    SUBMITTED: { label: 'Đã nộp', badge: 'Đã nộp', variant: 'success' },
    AUTO_SUBMITTED: { label: 'Tự động nộp', badge: 'Tự động nộp', variant: 'info' },
    STOPPED: { label: 'Đã dừng', badge: 'Đã dừng', variant: 'error' }
  }
  const s = statusMap[status] || { label: status, badge: status, variant: 'info' }
  const tabCount = violationEvents.value.filter(e =>
    e.eventType === 'TAB_SWITCH' || e.eventType === 'CHUYỂN TAB'
  ).length
  const severityCounts = { HIGH: 0, MEDIUM: 0, LOW: 0 }
  violationEvents.value.forEach(e => {
    if (e.severity) severityCounts[e.severity] = (severityCounts[e.severity] || 0) + 1
  })
  const maxSeverity = severityCounts.HIGH > 0 ? 'Nghiêm trọng'
    : severityCounts.MEDIUM > 0 ? 'Trung bình'
    : severityCounts.LOW > 0 ? 'Thấp'
    : '—'
  return {
    timeJoined,
    startTime,
    tabSwitches: tabCount,
    totalViolations: violationEvents.value.length,
    severity: maxSeverity,
    status: s.label,
    statusBadge: s.badge,
    statusVariant: s.variant
  }
})

const violationEvents = computed(() => {
  const events = []
  const eventMap = new Map()
  timeline.value.forEach(item => {
    if (item.id) return
    const key = `${item.eventType}-${item.at}`
    if (!eventMap.has(key)) {
      eventMap.set(key, true)
      events.push({
        id: `${item.eventType}-${item.at}`,
        at: item.at,
        eventType: item.eventType,
        details: item.details || item.eventType,
        severity: item.severity || mapSeverity(item.eventType, item.confidence)
      })
    }
  })
  return events
})

const suspiciousPatterns = computed(() => {
  const patterns = []
  const tabCount = violationEvents.value.filter(e =>
    e.eventType === 'TAB_SWITCH' || e.eventType === 'CHUYỂN TAB'
  ).length
  if (tabCount > 3) {
    patterns.push({
      id: 'tab-switch',
      icon: 'tab',
      title: 'Chuyển tab nhiều lần',
      description: `Đã chuyển tab ${tabCount} lần, vượt ngưỡng bình thường (3 lần)`,
      level: tabCount > 5 ? 'high' : 'medium'
    })
  }
  const copyCount = violationEvents.value.filter(e =>
    e.eventType === 'COPY_PASTE' || e.eventType === 'COPY'
  ).length
  if (copyCount > 0) {
    patterns.push({
      id: 'copy-paste',
      icon: 'content_copy',
      title: 'Cố gắng copy nội dung',
      description: `Phát hiện ${copyCount} lần sao chép dữ liệu từ đề thi`,
      level: 'medium'
    })
  }
  const fsCount = violationEvents.value.filter(e =>
    e.eventType === 'EXIT_FULLSCREEN' || e.eventType === 'THOÁT TOÀN MÀN HÌNH'
  ).length
  if (fsCount > 0) {
    patterns.push({
      id: 'exit-fullscreen',
      icon: 'fullscreen_exit',
      title: 'Thoát khỏi chế độ toàn màn hình',
      description: `Đã thoát toàn màn hình ${fsCount} lần`,
      level: fsCount > 2 ? 'high' : 'medium'
    })
  }
  const devtoolsCount = violationEvents.value.filter(e =>
    e.eventType === 'DEVTOOLS_OPEN'
  ).length
  if (devtoolsCount > 0) {
    patterns.push({
      id: 'devtools',
      icon: 'code',
      title: 'Mở công cụ phát triển',
      description: `Phát hiện mở DevTools ${devtoolsCount} lần`,
      level: 'high'
    })
  }
  const duplicateIpCount = violationEvents.value.filter(e =>
    e.eventType === 'DUPLICATE_IP'
  ).length
  if (duplicateIpCount > 0) {
    patterns.push({
      id: 'duplicate-ip',
      icon: 'language',
      title: 'Phát hiện IP trùng lặp',
      description: `Có ${duplicateIpCount} thiết bị khác cùng IP đang thi`,
      level: 'high'
    })
  }
  return patterns
})

function mapSeverity(eventType, confidence) {
  const typeMap = {
    TAB_SWITCH: 'LOW',
    WINDOW_BLUR: 'LOW',
    IDLE_TIME: 'LOW',
    RIGHT_CLICK: 'LOW',
    EXIT_FULLSCREEN: 'MEDIUM',
    COPY_PASTE: 'MEDIUM',
    RAPID_QUESTION_SWITCH: 'MEDIUM',
    DUPLICATE_IP: 'MEDIUM',
    HEARTBEAT_STALE: 'MEDIUM',
    DEVTOOLS_OPEN: 'HIGH',
    PRINT_SCREEN: 'HIGH',
    MULTI_MONITOR: 'HIGH',
    DEVICE_FINGERPRINT_CHANGED: 'HIGH',
    TIME_ANOMALY: 'MEDIUM'
  }
  return typeMap[eventType] || (confidence >= 0.8 ? 'HIGH' : confidence >= 0.5 ? 'MEDIUM' : 'LOW')
}

async function loadData() {
  if (!attemptId.value) {
    error.value = 'Không có thông tin attempt'
    toast.error(error.value)
    return
  }
  loading.value = true
  error.value = ''
  try {
    const [risk, tl, audit] = await Promise.all([
      fetchAttemptRisk(attemptId.value),
      listMonitoringTimeline(attemptId.value),
      listMonitoringAudit(attemptId.value)
    ])
    riskData.value = risk || {}
    timeline.value = tl || []
    auditLog.value = audit || []
  } catch (err) {
    error.value = err.message || 'Không thể tải dữ liệu giám sát'
    toast.error(error.value)
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.back()
}

async function issueWarning() {
  showWarningDialog.value = true
}

async function confirmSendWarning() {
  actionLoading.value = 'warning'
  showWarningDialog.value = false
  try {
    await sendTeacherWarning(attemptId.value, warningMessage.value)
    toast.success('Đã gửi cảnh báo tới học sinh.')
    warningMessage.value = ''
  } catch (err) {
    toast.error('Gửi cảnh báo thất bại. Vui lòng thử lại.')
  } finally {
    actionLoading.value = ''
  }
}

async function suspendExam() {
  showInvalidateDialog.value = true
}

async function confirmInvalidate() {
  actionLoading.value = 'invalidate'
  showInvalidateDialog.value = false
  try {
    await invalidateAttempt(attemptId.value, invalidateReason.value)
    invalidateReason.value = ''
    await loadData()
  } catch (err) {
    toast.error('Dừng thi thất bại. Vui lòng thử lại.')
  } finally {
    actionLoading.value = ''
  }
}

function viewFullReport() {
  router.push({
    path: '/teacher/exams/review/incidents',
    query: { examId: examId.value }
  })
}

const signalScoreColor = (score) => {
  if (score >= 60) return 'var(--ds-danger)'
  if (score >= 30) return 'var(--ds-warning)'
  return 'var(--ds-success)'
}

const getRiskColor = (score) => {
  if (score >= 60) return 'var(--ds-danger)'
  if (score >= 30) return 'var(--ds-warning)'
  return 'var(--ds-success)'
}

const getRiskBgColor = (score) => {
  if (score >= 60) return 'var(--ds-danger-soft)'
  if (score >= 30) return 'var(--ds-warning-soft)'
  return 'var(--ds-success-soft)'
}

const getViolationColor = (type) => {
  const colors = {
    TAB_SWITCH: 'var(--ds-warning)',
    WINDOW_BLUR: 'var(--ds-warning)',
    IDLE_TIME: 'var(--ds-info)',
    RIGHT_CLICK: 'var(--ds-info)',
    EXIT_FULLSCREEN: 'var(--ds-warning)',
    COPY_PASTE: 'var(--ds-danger)',
    DEVTOOLS_OPEN: 'var(--ds-danger)',
    PRINT_SCREEN: 'var(--ds-danger)',
    MULTI_MONITOR: 'var(--ds-danger)',
    DUPLICATE_IP: 'var(--ds-danger)',
    RAPID_QUESTION_SWITCH: 'var(--ds-warning)',
    HEARTBEAT_STALE: 'var(--ds-warning)',
    DEVICE_FINGERPRINT_CHANGED: 'var(--ds-danger)',
    TIME_ANOMALY: 'var(--ds-warning)'
  }
  return colors[type] || 'var(--ds-text-muted)'
}

const getViolationIcon = (type) => {
  const icons = {
    TAB_SWITCH: 'tab',
    WINDOW_BLUR: 'tab',
    IDLE_TIME: 'schedule',
    RIGHT_CLICK: 'mouse_right_button',
    EXIT_FULLSCREEN: 'fullscreen_exit',
    COPY_PASTE: 'content_copy',
    DEVTOOLS_OPEN: 'code',
    PRINT_SCREEN: 'screenshot',
    MULTI_MONITOR: 'desktop_windows',
    DUPLICATE_IP: 'language',
    RAPID_QUESTION_SWITCH: 'flip_to_front',
    HEARTBEAT_STALE: 'favorite',
    DEVICE_FINGERPRINT_CHANGED: 'fingerprint',
    TIME_ANOMALY: 'schedule',
    FRAUD_SIGNAL: 'psychology'
  }
  return icons[type] || 'warning'
}

const getViolationLabel = (type) => {
  const labels = {
    TAB_SWITCH: 'Chuyển tab',
    WINDOW_BLUR: 'Mất tiêu điểm cửa sổ',
    IDLE_TIME: 'Không hoạt động',
    RIGHT_CLICK: 'Click chuột phải',
    EXIT_FULLSCREEN: 'Thoát toàn màn hình',
    COPY_PASTE: 'Sao chép nội dung',
    DEVTOOLS_OPEN: 'Mở DevTools',
    PRINT_SCREEN: 'Chụp màn hình',
    MULTI_MONITOR: 'Nhiều màn hình',
    DUPLICATE_IP: 'IP trùng lặp',
    RAPID_QUESTION_SWITCH: 'Chuyển câu nhanh',
    HEARTBEAT_STALE: 'Mất kết nối',
    DEVICE_FINGERPRINT_CHANGED: 'Thay đổi thiết bị',
    TIME_ANOMALY: 'Bất thường thời gian',
    RISK_SCORE: 'Cập nhật rủi ro',
    WARNING: 'Cảnh báo GV',
    FRAUD_SIGNAL: 'Tín hiệu gian lận',
    MONITORING_EVENT: 'Sự kiện giám sát',
    EXAM_EVENT: 'Sự kiện thi'
  }
  return labels[type] || type || '—'
}

const getSeverityLabel = (status) => {
  const labels = {
    HIGH: 'Nghiêm trọng',
    MEDIUM: 'Trung bình',
    LOW: 'Thấp',
    CRITICAL: 'Nghiêm trọng',
    WARNING: 'Cảnh báo',
    ERROR: 'Nghiêm trọng',
    INFO: 'Thông tin'
  }
  return labels[status] || status || '—'
}

const getSeverityStatus = (status) => {
  return status === 'HIGH' || status === 'CRITICAL' ? 'error'
    : status === 'MEDIUM' || status === 'WARNING' ? 'warning'
    : status === 'LOW' ? 'info'
    : 'info'
}

const formatTimestamp = (timestamp) => {
  if (!timestamp) return '—'
  const date = new Date(timestamp)
  return date.toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(loadData)
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
