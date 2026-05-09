<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto max-w-7xl px-4 pb-10 pt-4 sm:px-6 lg:px-8">

      <!-- Breadcrumb -->
      <div class="mb-4 ds-animate-fade-up">
        <div class="flex items-center gap-2 text-sm" style="color: var(--ds-text-muted)">
          <RouterLink
            class="flex items-center gap-1 transition-colors hover:text-[var(--ds-primary)]"
            style="color: var(--ds-text-muted)"
            to="/teacher/exams/list"
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
          :subtitle="pageSubtitle"
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

      <div v-if="loadError" class="mb-6">
        <EmptyState
          icon="warning"
          title="Không tải được báo cáo học sinh"
          :description="loadError"
          action-label="Quay lại tổng quan kết quả"
          fill
          @action="goBack"
        />
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

      <!-- Report Tabs -->
      <nav
        class="mb-6 flex flex-wrap gap-2 ds-animate-fade-up"
        style="animation-delay: 0.12s"
        role="tablist"
        aria-label="Nội dung báo cáo học sinh"
      >
        <button
          v-for="tab in reportTabs"
          :key="tab.id"
          type="button"
          role="tab"
          class="inline-flex items-center gap-2 rounded-lg border px-4 py-2.5 text-sm font-bold transition-all hover:-translate-y-0.5"
          :aria-selected="activeReportTab === tab.id"
          :style="getReportTabStyle(tab.id)"
          @click="activeReportTab = tab.id"
        >
          <LucideIcon :name="tab.icon" size="16" />
          <span>{{ tab.label }}</span>
          <span
            v-if="tab.count != null"
            class="rounded-full px-2 py-0.5 text-[11px] font-extrabold tabular-nums"
            :style="getReportTabCountStyle(tab.id)"
          >
            {{ tab.count }}
          </span>
        </button>
      </nav>

      <!-- Score Tab -->
      <section v-if="activeReportTab === 'score'" class="ds-animate-fade-up" style="animation-delay: 0.15s">
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
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
            :icon="'verified_user'"
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
      </section>

      <!-- Answers Tab -->
      <section v-if="activeReportTab === 'answers'" class="space-y-6 ds-animate-fade-up" style="animation-delay: 0.15s">
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

        <DsCard padding="lg">
          <template #header>
            <div class="flex items-center gap-2">
              <LucideIcon name="pie_chart" />
              <h3 class="text-lg font-bold" style="color: var(--ds-text)">Phân bố câu trả lời</h3>
            </div>
          </template>
          <div class="grid grid-cols-1 gap-4 text-center sm:grid-cols-3">
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
      </section>

      <!-- Fraud Behavior Tab -->
      <section v-if="activeReportTab === 'behavior'" class="space-y-6 ds-animate-fade-up" style="animation-delay: 0.15s">
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <DsStatCard
            label="Risk score"
            :value="behaviorSummary.riskScore"
            sub-value="/ 100"
            icon="shield-alert"
            :badge="behaviorSummary.riskLevel"
            :badge-variant="behaviorSummary.badgeVariant"
          />
          <DsStatCard
            label="Số vi phạm"
            :value="behaviorSummary.violationCount"
            icon="warning"
            :sub="`${behaviorEvents.length} sự kiện timeline`"
            :badge-variant="behaviorSummary.badgeVariant"
          />
          <DsStatCard
            label="Cảnh báo"
            :value="behaviorSummary.warningCount"
            icon="notifications"
            :sub="behaviorSummary.reviewRequired ? 'Cần giám thị rà soát' : 'Không yêu cầu rà soát'"
          />
          <DsStatCard
            label="Đánh giá"
            :value="behaviorSummary.suspiciousLabel"
            icon="verified_user"
            :badge="behaviorSummary.recommendedActionLabel"
            :badge-variant="behaviorSummary.badgeVariant"
          />
        </div>

        <DsCard v-if="riskReasons.length || evidenceSummary.length || behaviorLoadError" padding="lg">
          <div v-if="behaviorLoadError" class="mb-4 flex items-start gap-3 rounded-lg border p-4" style="border-color: rgba(245, 158, 11, 0.25); background-color: rgba(245, 158, 11, 0.08)">
            <LucideIcon name="warning" size="18" style="color: var(--ds-warning)" />
            <div>
              <p class="text-sm font-bold" style="color: var(--ds-text)">Không tải đủ timeline giám sát</p>
              <p class="mt-1 text-sm" style="color: var(--ds-text-muted)">{{ behaviorLoadError }}</p>
            </div>
          </div>

          <div class="grid gap-5 lg:grid-cols-2">
            <div v-if="riskReasons.length">
              <h3 class="mb-3 text-base font-bold" style="color: var(--ds-text)">Lý do bị đánh dấu</h3>
              <div class="flex flex-wrap gap-2">
                <span
                  v-for="reason in riskReasons"
                  :key="reason"
                  class="rounded-full px-3 py-1 text-xs font-bold"
                  style="background-color: var(--ds-danger-bg); color: var(--ds-danger)"
                >
                  {{ reason }}
                </span>
              </div>
            </div>

            <div v-if="evidenceSummary.length">
              <h3 class="mb-3 text-base font-bold" style="color: var(--ds-text)">Bằng chứng chính</h3>
              <ul class="space-y-2">
                <li
                  v-for="evidence in evidenceSummary"
                  :key="evidence"
                  class="flex gap-2 text-sm"
                  style="color: var(--ds-text-muted)"
                >
                  <span>{{ evidence }}</span>
                </li>
              </ul>
            </div>
          </div>
        </DsCard>

        <DsCard v-if="behaviorPatterns.length" padding="lg">
          <template #header>
            <h3 class="text-lg font-bold" style="color: var(--ds-text)">Mẫu hành vi đáng ngờ</h3>
          </template>
          <div class="grid gap-3 md:grid-cols-2">
            <article
              v-for="pattern in behaviorPatterns"
              :key="pattern.id"
              class="rounded-lg border p-4"
              :style="{ borderColor: pattern.borderColor, backgroundColor: pattern.bgColor }"
            >
              <div class="flex items-start justify-between gap-3">
                <h4 class="font-bold" style="color: var(--ds-text)">{{ pattern.title }}</h4>
                <span class="rounded-full px-2 py-1 text-[10px] font-extrabold uppercase tracking-wider" :style="{ backgroundColor: pattern.color, color: 'white' }">
                  {{ pattern.level }}
                </span>
              </div>
              <p class="mt-2 text-sm" style="color: var(--ds-text-muted)">{{ pattern.description }}</p>
            </article>
          </div>
        </DsCard>

        <DsCard padding="lg">
          <template #header>
            <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <h3 class="text-lg font-bold" style="color: var(--ds-text)">Dòng thời gian hành vi</h3>
              <select
                v-model="behaviorFilter"
                class="rounded-lg border px-3 py-2 text-sm font-semibold outline-none"
                style="background-color: var(--ds-surface); color: var(--ds-text); border-color: var(--ds-border)"
                aria-label="Lọc loại hành vi"
              >
                <option value="">Tất cả hành vi</option>
                <option
                  v-for="option in behaviorFilterOptions"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }} ({{ option.count }})
                </option>
              </select>
            </div>
          </template>

          <div v-if="filteredBehaviorEvents.length === 0" class="py-8">
            <EmptyState
              icon="verified_user"
              title="Chưa ghi nhận hành vi gian lận"
              description="Timeline giám sát chưa có tín hiệu vi phạm hoặc cảnh báo cho lượt làm này."
              fill
            />
          </div>

          <div v-else class="space-y-3">
            <article
              v-for="event in filteredBehaviorEvents"
              :key="event.key"
              class="rounded-lg border p-4"
              style="border-color: var(--ds-border); background-color: var(--ds-surface)"
            >
              <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <h4 class="font-bold" style="color: var(--ds-text)">{{ event.label }}</h4>
                  <p class="mt-0.5 text-xs font-semibold" style="color: var(--ds-text-muted)">
                    {{ event.timeLabel }}
                    <span v-if="event.categoryLabel"> · {{ event.categoryLabel }}</span>
                  </p>
                </div>
                <span
                  v-if="event.severityLabel"
                  class="w-fit rounded-full px-2.5 py-1 text-[11px] font-extrabold uppercase tracking-wider"
                  :style="{ backgroundColor: event.severityBg, color: event.severityColor }"
                >
                  {{ event.severityLabel }}
                </span>
              </div>

              <p v-if="event.details" class="mt-3 text-sm" style="color: var(--ds-text-muted)">
                {{ event.details }}
              </p>

              <div class="mt-3 flex flex-wrap gap-2 text-xs font-semibold">
                <span
                  v-if="event.riskImpact != null"
                  class="rounded-full px-2 py-1"
                  style="background-color: var(--ds-danger-bg); color: var(--ds-danger)"
                >
                  {{ formatRiskImpact(event.riskImpact) }}
                </span>
              </div>
            </article>
          </div>
        </DsCard>
      </section>

    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import { getAttemptDetail, getAttemptReport, listExamAttempts } from '../../services/attemptService'
import { fetchAttemptRisk, listMonitoringTimeline } from '../../services/examMonitoringService'
import { ApiError } from '../../services/apiClient'
import {
  formatAttemptAnswer,
  formatScoreTen,
  getAttemptStatusMeta,
  parseResultOptions,
  scorePercentValue
} from '../../utils/attemptResult'
import { normalizeSignalType } from '../../utils/proctorSignalTypes'
import PageHeader from '../ui/PageHeader.vue'
import DsCard from '../ui/DsCard.vue'
import EmptyState from '../ui/EmptyState.vue'
import DsStatCard from '../ui/DsStatCard.vue'
import DataTable from '../ui/DataTable.vue'
import StatusChip from '../ui/StatusChip.vue'

const router = useRouter()
const route = useRoute()

const examId = computed(() => Number.parseInt(String(route.query.examId || ''), 10) || null)
const attemptId = computed(() => Number.parseInt(String(route.query.attemptId || ''), 10) || null)
const examTitle = computed(() => route.query.title || 'Đề thi')
const attemptDetail = ref(null)
const attemptReport = ref(null)
const riskSnapshot = ref(null)
const monitoringTimeline = ref([])
const examAttempts = ref([])
const isLoading = ref(false)
const loadError = ref('')
const behaviorLoadError = ref('')
const activeReportTab = ref('score')
const behaviorFilter = ref('')
let latestRequestId = 0

const RISK_LEVEL_LABELS = {
  CRITICAL: 'Nguy cơ rất cao',
  HIGH_RISK: 'Rủi ro cao',
  SUSPICIOUS: 'Đáng ngờ',
  CLEAN: 'Bình thường'
}

const RISK_LEVEL_BADGES = {
  CRITICAL: 'danger',
  HIGH_RISK: 'warning',
  SUSPICIOUS: 'warning',
  CLEAN: 'success'
}

const RISK_ACTION_LABELS = {
  FLAG_AND_REVIEW: 'Gắn cờ và rà soát',
  FLAG_FOR_REVIEW: 'Cần rà soát',
  REVIEW_ATTEMPT: 'Rà soát lượt làm',
  CONTINUE_MONITORING: 'Tiếp tục giám sát',
  REVIEW: 'Cần rà soát',
  WARN: 'Gửi cảnh báo',
  PAUSE: 'Tạm dừng',
  INVALIDATE: 'Buộc nộp',
  MONITOR: 'Tiếp tục giám sát',
  NONE: 'Theo dõi'
}

const BEHAVIOR_LABELS = {
  TAB_SWITCH: 'Chuyển tab',
  WINDOW_BLUR: 'Mất tiêu điểm',
  SCREEN_LEAVE: 'Rời màn hình',
  EXIT_FULLSCREEN: 'Thoát toàn màn hình',
  LONG_SCREEN_LEAVE: 'Rời màn hình lâu',
  COPY_PASTE: 'Sao chép / dán',
  DEVTOOLS_OPEN: 'Mở DevTools',
  RIGHT_CLICK: 'Click chuột phải',
  PRINT_SCREEN: 'Chụp màn hình',
  RAPID_QUESTION_SWITCH: 'Chuyển câu nhanh',
  MULTI_MONITOR: 'Nhiều màn hình',
  DUPLICATE_IP: 'IP trùng lặp',
  IP_CHANGED: 'IP thay đổi',
  DEVICE_FINGERPRINT_CHANGED: 'Đổi thiết bị',
  HEARTBEAT_STALE: 'Mất kết nối',
  NO_CAMERA: 'Camera tắt',
  FACE_NOT_DETECTED: 'Không phát hiện khuôn mặt',
  MULTIPLE_FACES: 'Nhiều khuôn mặt',
  FACE_SPOOFING_SUSPECTED: 'Nghi vấn giả mạo',
  EYES_OBSTRUCTED: 'Mắt bị che',
  FACE_OBSTRUCTED_MASK: 'Khuôn mặt bị che',
  PARTIAL_FACE_VISIBLE: 'Khuôn mặt không đầy đủ',
  FACE_TOO_FAR: 'Khuôn mặt quá xa',
  FACE_TOO_CLOSE: 'Khuôn mặt quá gần',
  FACE_TURNED_AWAY: 'Quay mặt đi',
  FACE_NOT_CENTERED: 'Khuôn mặt lệch tâm',
  EYES_NOT_DETECTED: 'Không phát hiện mắt',
  VERY_LOW_LIGHTING: 'Ánh sáng rất yếu',
  LOW_LIGHTING: 'Ánh sáng yếu',
  OVEREXPOSED_FRAME: 'Ảnh quá sáng',
  VERY_BLURRY_FRAME: 'Ảnh rất mờ',
  BLURRY_FRAME: 'Ảnh mờ',
  EYE_BLINK_ANOMALY: 'Nháy mắt bất thường',
  EYES_CLOSED_PROLONGED: 'Mắt nhắm lâu',
  GAZE_OFF_SCREEN: 'Nhìn lệch màn hình',
  RAPID_EYE_MOVEMENT: 'Mắt di chuyển nhanh',
  AI_SPEAKING_DETECTED: 'Tiếng ồn',
  NO_MIC: 'Micro tắt',
  WARNING_SENT: 'Cảnh báo đã gửi',
  FRAUD_WARNING: 'Cảnh báo giám sát',
  TEACHER_WARNING: 'Cảnh báo của giám thị',
  TEACHER_PAUSE: 'Tạm dừng bài thi',
  TEACHER_RESUME: 'Cho tiếp tục',
  TEACHER_INVALIDATE: 'Buộc nộp bài',
  NOTE: 'Ghi chú'
}

const BEHAVIOR_ICONS = {
  TAB_SWITCH: 'timeline',
  WINDOW_BLUR: 'visibility_off',
  SCREEN_LEAVE: 'monitor',
  EXIT_FULLSCREEN: 'fullscreen_exit',
  LONG_SCREEN_LEAVE: 'timer_off',
  COPY_PASTE: 'content_copy',
  DEVTOOLS_OPEN: 'terminal',
  RIGHT_CLICK: 'mouse_pointer_2',
  PRINT_SCREEN: 'screenshot',
  RAPID_QUESTION_SWITCH: 'shuffle',
  MULTI_MONITOR: 'desktop_windows',
  DUPLICATE_IP: 'public',
  IP_CHANGED: 'public',
  DEVICE_FINGERPRINT_CHANGED: 'fingerprint',
  HEARTBEAT_STALE: 'wifi_off',
  NO_CAMERA: 'videocam_off',
  FACE_NOT_DETECTED: 'user-x',
  MULTIPLE_FACES: 'users',
  FACE_SPOOFING_SUSPECTED: 'shield-alert',
  EYES_OBSTRUCTED: 'eye-off',
  FACE_OBSTRUCTED_MASK: 'shield-alert',
  PARTIAL_FACE_VISIBLE: 'monitor',
  FACE_TOO_FAR: 'search',
  FACE_TOO_CLOSE: 'search',
  FACE_TURNED_AWAY: 'rotate-ccw',
  FACE_NOT_CENTERED: 'monitor',
  EYES_NOT_DETECTED: 'eye-off',
  VERY_LOW_LIGHTING: 'warning',
  LOW_LIGHTING: 'warning',
  OVEREXPOSED_FRAME: 'warning',
  VERY_BLURRY_FRAME: 'image_off',
  BLURRY_FRAME: 'image_off',
  EYE_BLINK_ANOMALY: 'eye',
  EYES_CLOSED_PROLONGED: 'eye-off',
  GAZE_OFF_SCREEN: 'eye-off',
  RAPID_EYE_MOVEMENT: 'activity',
  AI_SPEAKING_DETECTED: 'mic',
  NO_MIC: 'mic_off',
  WARNING_SENT: 'warning',
  FRAUD_WARNING: 'shield-alert',
  TEACHER_WARNING: 'warning',
  TEACHER_PAUSE: 'pause_circle',
  TEACHER_RESUME: 'play_circle',
  TEACHER_INVALIDATE: 'stop_circle',
  NOTE: 'sticky_note'
}

const BEHAVIOR_EVENT_TYPES = new Set([
  'TAB_SWITCH',
  'WINDOW_BLUR',
  'SCREEN_LEAVE',
  'EXIT_FULLSCREEN',
  'LONG_SCREEN_LEAVE',
  'COPY_PASTE',
  'DEVTOOLS_OPEN',
  'RIGHT_CLICK',
  'PRINT_SCREEN',
  'RAPID_QUESTION_SWITCH',
  'MULTI_MONITOR',
  'DUPLICATE_IP',
  'IP_CHANGED',
  'DEVICE_FINGERPRINT_CHANGED',
  'HEARTBEAT_STALE',
  'NO_CAMERA',
  'FACE_NOT_DETECTED',
  'MULTIPLE_FACES',
  'FACE_SPOOFING_SUSPECTED',
  'EYES_OBSTRUCTED',
  'FACE_OBSTRUCTED_MASK',
  'PARTIAL_FACE_VISIBLE',
  'FACE_TOO_FAR',
  'FACE_TOO_CLOSE',
  'FACE_TURNED_AWAY',
  'FACE_NOT_CENTERED',
  'EYES_NOT_DETECTED',
  'VERY_LOW_LIGHTING',
  'LOW_LIGHTING',
  'OVEREXPOSED_FRAME',
  'VERY_BLURRY_FRAME',
  'BLURRY_FRAME',
  'EYE_BLINK_ANOMALY',
  'EYES_CLOSED_PROLONGED',
  'GAZE_OFF_SCREEN',
  'RAPID_EYE_MOVEMENT',
  'AI_SPEAKING_DETECTED',
  'NO_MIC',
  'WARNING_SENT',
  'FRAUD_WARNING',
  'TEACHER_WARNING',
  'TEACHER_PAUSE',
  'TEACHER_RESUME',
  'TEACHER_INVALIDATE',
  'NOTE'
])

const PATTERN_RULES = [
  {
    id: 'tab-switch',
    key: 'TAB_SWITCH',
    threshold: 3,
    icon: 'timeline',
    title: 'Chuyển tab nhiều lần',
    description: (count) => `${count} lần chuyển tab`,
    level: (count) => (count >= 6 ? 'HIGH' : 'MEDIUM')
  },
  {
    id: 'copy-paste',
    key: 'COPY_PASTE',
    threshold: 1,
    icon: 'content_copy',
    title: 'Có dấu hiệu sao chép',
    description: (count) => `${count} lần sao chép hoặc dán`,
    level: () => 'MEDIUM'
  },
  {
    id: 'devtools',
    key: 'DEVTOOLS_OPEN',
    threshold: 1,
    icon: 'terminal',
    title: 'Mở công cụ phát triển',
    description: (count) => `${count} lần mở DevTools`,
    level: () => 'HIGH'
  },
  {
    id: 'fullscreen',
    key: 'EXIT_FULLSCREEN',
    threshold: 1,
    icon: 'fullscreen_exit',
    title: 'Thoát toàn màn hình',
    description: (count) => `${count} lần thoát fullscreen`,
    level: () => 'MEDIUM'
  },
  {
    id: 'identity',
    key: 'DUPLICATE_IP',
    threshold: 1,
    icon: 'public',
    title: 'Rủi ro định danh / IP',
    description: (count) => `${count} tín hiệu liên quan IP hoặc thiết bị`,
    level: () => 'HIGH'
  }
]

const selectedAttempt = computed(() =>
  examAttempts.value.find((attempt) => Number(attempt.id) === attemptId.value) || null
)

const studentInfo = computed(() => {
  const statusMeta = getAttemptStatusMeta(attemptDetail.value?.status || selectedAttempt.value?.status)
  return {
    name:
      selectedAttempt.value?.student ||
      attemptDetail.value?.student ||
      String(route.query.student || 'Học sinh không rõ'),
    studentId:
      selectedAttempt.value?.studentCode ||
      String(route.query.studentCode || `AT-${attemptId.value || 'N/A'}`),
    email:
      selectedAttempt.value?.email ||
      String(route.query.email || 'Chưa có email'),
    status: statusMeta.label,
    statusKey: statusMeta.key
  }
})

const studentInitials = computed(() => {
  const parts = String(studentInfo.value.name || '').trim().split(/\s+/).filter(Boolean)
  if (!parts.length) return 'HS'
  const first = parts[0]?.charAt(0) || 'H'
  const last = parts[parts.length - 1]?.charAt(0) || 'S'
  return `${last}${first}`.toUpperCase()
})

const scoreSummary = computed(() => {
  const scoreRaw = scorePercentValue(attemptReport.value?.score ?? attemptDetail.value?.score ?? 0)
  const totalScore = Number(formatScoreTen(scoreRaw))
  const submittedAttempts = examAttempts.value
    .filter((attempt) => ['SUBMITTED', 'AUTO_SUBMITTED'].includes(String(attempt.status || '').toUpperCase()))
    .sort((a, b) => Number(b.score || 0) - Number(a.score || 0))
  const rankIndex = submittedAttempts.findIndex((attempt) => Number(attempt.id) === attemptId.value)
  const timeSpentMinutes = (() => {
    const startedAt = attemptDetail.value?.startedAt
    const submittedAt = attemptDetail.value?.submittedAt
    if (!startedAt || !submittedAt) return '-'
    const elapsedMs = new Date(submittedAt).getTime() - new Date(startedAt).getTime()
    if (!Number.isFinite(elapsedMs) || elapsedMs <= 0) return '-'
    return `${Math.max(1, Math.round(elapsedMs / 60000))} phút`
  })()
  const maxTimeMinutes = (() => {
    const startedAt = attemptDetail.value?.startedAt
    const deadlineAt = attemptDetail.value?.deadlineAt
    if (!startedAt || !deadlineAt) return '-'
    const elapsedMs = new Date(deadlineAt).getTime() - new Date(startedAt).getTime()
    if (!Number.isFinite(elapsedMs) || elapsedMs <= 0) return '-'
    return `${Math.max(1, Math.round(elapsedMs / 60000))} phút`
  })()
  const totalStudents = submittedAttempts.length || examAttempts.value.length || 0
  const correctCount = Number(attemptReport.value?.correctCount || 0)
  const totalQuestions = Number(attemptDetail.value?.totalQuestions || attemptReport.value?.answers?.length || 0)
  const accuracy = totalQuestions > 0 ? Math.round((correctCount / totalQuestions) * 100) : 0
  const badgeVariant = totalScore >= 8 ? 'success' : totalScore >= 5 ? 'warning' : 'danger'
  return {
    totalScore: totalScore.toFixed(1),
    badgeVariant,
    accuracy: String(accuracy),
    accuracyNote: `${correctCount}/${totalQuestions} câu`,
    timeSpent: timeSpentMinutes,
    maxTime: maxTimeMinutes,
    rank: rankIndex >= 0 ? String(rankIndex + 1) : '-',
    totalStudents: String(totalStudents || '-'),
    className: selectedAttempt.value?.className || examTitle.value
  }
})

const answerStats = computed(() => {
  const totalQuestions = Number(attemptDetail.value?.totalQuestions || attemptReport.value?.answers?.length || 0)
  const answeredCount = Number(attemptReport.value?.answeredCount || 0)
  const correct = Number(attemptReport.value?.correctCount || 0)
  return {
    correct,
    wrong: Math.max(answeredCount - correct, 0),
    skipped: Math.max(totalQuestions - answeredCount, 0)
  }
})

const questionColumns = [
  { key: 'number', label: '#', width: '60px', align: 'center' },
  { key: 'content', label: 'Câu hỏi' },
  { key: 'selectedAnswer', label: 'Câu trả lời của bạn' },
  { key: 'correctAnswer', label: 'Đáp án đúng' },
  { key: 'status', label: 'Kết quả', width: '80px', align: 'center' }
]

const questionsData = computed(() => (attemptReport.value?.answers || []).map((answer, index) => {
  const questionContent = typeof answer.question === 'string'
    ? answer.question
    : (answer.question?.content || answer.question?.text || 'Câu hỏi không có nội dung')
  const questionOptions = answer.options || answer.questionOptions || answer.question?.options || []
  const normalizedOptions = parseResultOptions(questionOptions)
  const hasSelected = String(answer.selectedAnswer || '').trim() !== ''
  const status = !hasSelected
    ? 'skipped'
    : (answer.correct === true ? 'correct' : 'wrong')
  return {
    id: answer.questionId || index,
    number: index + 1,
    content: questionContent,
    selectedAnswer: hasSelected ? formatAttemptAnswer(answer.selectedAnswer, normalizedOptions) : '',
    correctAnswer: formatAttemptAnswer(answer.correctAnswer, normalizedOptions),
    status
  }
}))

const pageSubtitle = computed(() => {
  if (attemptDetail.value?.submittedAt) {
    return `Nộp bài lúc ${new Date(attemptDetail.value.submittedAt).toLocaleString('vi-VN')}`
  }
  return 'Xem chi tiết kết quả làm bài của học sinh'
})

const behaviorTimelineSource = computed(() => Array.isArray(monitoringTimeline.value) ? monitoringTimeline.value : [])

function toNumberOrNull(value) {
  if (value == null || value === '') return null
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

function uniqueStrings(values = []) {
  const seen = new Set()
  const out = []
  for (const value of values) {
    const text = String(value || '').trim()
    if (!text || seen.has(text)) continue
    seen.add(text)
    out.push(text)
  }
  return out
}

function toStringList(value) {
  if (value == null) return []
  return Array.isArray(value) ? value : [value]
}

function parseTimelineText(value) {
  if (value == null) return ''
  if (typeof value === 'string') {
    const text = value.trim()
    if (!text) return ''
    if ((text.startsWith('{') || text.startsWith('[')) && text.length > 1) {
      try {
        const parsed = JSON.parse(text)
        return parseTimelineText(parsed)
      } catch {
        return text
      }
    }
    return text
  }
  if (Array.isArray(value)) {
    return value.map(parseTimelineText).filter(Boolean).join(' · ')
  }
  if (typeof value === 'object') {
    return parseTimelineText(
      value.details || value.message || value.evidence || value.displayMessage || value.note || value.reason || value.text || ''
    )
  }
  return String(value).trim()
}

function parseTimelineTime(value) {
  if (!value) return '—'
  const timestamp = new Date(value).getTime()
  if (!Number.isFinite(timestamp)) return '—'
  try {
    return new Date(timestamp).toLocaleString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      day: '2-digit',
      month: '2-digit'
    })
  } catch {
    return '—'
  }
}

function normalizeTimelineFingerprintText(value) {
  return String(value || '').trim().replace(/\s+/g, ' ')
}

function getTimelineTimestamp(value) {
  if (!value) return 0
  const timestamp = new Date(value).getTime()
  return Number.isFinite(timestamp) ? timestamp : 0
}

function getTimelineDetailsKey(event = {}) {
  const details = normalizeTimelineFingerprintText(event.details || event.displayMessage || event.message || '')
  const label = normalizeTimelineFingerprintText(BEHAVIOR_LABELS[event.eventType] || event.eventType)
  if (!details || details === label || details.toUpperCase() === event.eventType) return ''
  return details
}

function makeBehaviorEventKey(event = {}) {
  const timestamp = getTimelineTimestamp(event.at || event.createdAt || event.timestamp || event.issuedAt || event.updatedAt)
  const timeBucket = event._bucketAt != null
    ? event._bucketAt
    : (timestamp > 0 ? Math.floor(timestamp / 1000) * 1000 : '')
  return [
    event.eventType || '',
    timeBucket,
    getTimelineDetailsKey(event)
  ].join('|')
}

function getSeverityRank(value) {
  const ranks = {
    CRITICAL: 5,
    HIGH: 4,
    MEDIUM: 3,
    LOW: 2,
    INFO: 1
  }
  return ranks[normalizeSeverity(value)] || 0
}

function getSeverityDisplayLabel(value) {
  const severity = normalizeSeverity(value)
  if (severity === 'CRITICAL') return 'Rất cao'
  if (severity === 'HIGH') return 'Cao'
  return ''
}

function normalizeTimelineSourceLabel(value) {
  const raw = String(value || '').trim()
  if (!raw) return ''
  const normalized = raw.toLowerCase().replace(/[\s_-]+/g, '_')
  if (normalized === 'exam_events') return ''
  return raw
}

function behaviorBucketSize(eventType = '') {
  return eventType === 'NOTE' ? 1_000 : 15_000
}

function preferBehaviorEvent(candidate = {}, existing = {}) {
  if (candidate.riskImpact != null && existing.riskImpact == null) return true
  if (candidate.confidence != null && existing.confidence == null) return true
  if (getSeverityRank(candidate.severity) > getSeverityRank(existing.severity)) return true
  if (candidate.source && !existing.source) return true
  return normalizeTimelineFingerprintText(candidate.details).length > normalizeTimelineFingerprintText(existing.details).length
}

function normalizeSeverity(value) {
  const raw = String(value || '').trim().toUpperCase()
  if (!raw) return 'MEDIUM'
  if (['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'].includes(raw)) return raw
  return raw
}

function severityTone(severity, eventType) {
  const normalized = normalizeSeverity(severity)
  if (normalized === 'CRITICAL' || normalized === 'HIGH') return 'danger'
  if (normalized === 'MEDIUM') return 'warning'
  if (normalized === 'LOW') return 'info'
  if (['COPY_PASTE', 'DEVTOOLS_OPEN', 'DUPLICATE_IP', 'IP_CHANGED', 'DEVICE_FINGERPRINT_CHANGED', 'NO_CAMERA', 'NO_MIC', 'MULTIPLE_FACES', 'FACE_SPOOFING_SUSPECTED', 'WARNING_SENT', 'FRAUD_WARNING', 'TEACHER_INVALIDATE'].includes(eventType)) return 'danger'
  if (['TAB_SWITCH', 'WINDOW_BLUR', 'SCREEN_LEAVE', 'EXIT_FULLSCREEN', 'LONG_SCREEN_LEAVE', 'RAPID_QUESTION_SWITCH', 'MULTI_MONITOR', 'AI_SPEAKING_DETECTED', 'FACE_TURNED_AWAY', 'GAZE_OFF_SCREEN', 'VERY_LOW_LIGHTING', 'LOW_LIGHTING', 'VERY_BLURRY_FRAME', 'BLURRY_FRAME', 'EYE_BLINK_ANOMALY', 'EYES_CLOSED_PROLONGED', 'EYES_OBSTRUCTED', 'FACE_OBSTRUCTED_MASK', 'PARTIAL_FACE_VISIBLE', 'FACE_TOO_FAR', 'FACE_TOO_CLOSE', 'TEACHER_WARNING', 'TEACHER_PAUSE'].includes(eventType)) return 'warning'
  if (['TEACHER_RESUME', 'NOTE'].includes(eventType)) return 'info'
  return 'info'
}

function toneColor(tone) {
  if (tone === 'danger') return 'var(--ds-danger)'
  if (tone === 'warning') return 'var(--ds-warning)'
  if (tone === 'success') return 'var(--ds-success)'
  return 'var(--ds-info)'
}

function toneSoftBg(tone) {
  if (tone === 'danger') return 'var(--ds-danger-bg)'
  if (tone === 'warning') return 'var(--ds-warning-bg)'
  if (tone === 'success') return 'var(--ds-success-bg)'
  return 'var(--ds-info-bg)'
}

function getBehaviorCategoryLabel(type) {
  const raw = normalizeSignalType(type) || String(type || '').trim().toUpperCase()
  const map = {
    TAB_SWITCH: 'Trình duyệt',
    WINDOW_BLUR: 'Trình duyệt',
    SCREEN_LEAVE: 'Trình duyệt',
    EXIT_FULLSCREEN: 'Trình duyệt',
    LONG_SCREEN_LEAVE: 'Trình duyệt',
    COPY_PASTE: 'Nội dung',
    DEVTOOLS_OPEN: 'Trình duyệt',
    RIGHT_CLICK: 'Trình duyệt',
    PRINT_SCREEN: 'Thiết bị',
    RAPID_QUESTION_SWITCH: 'Bài làm',
    MULTI_MONITOR: 'Thiết bị',
    DUPLICATE_IP: 'Mạng',
    IP_CHANGED: 'Mạng',
    DEVICE_FINGERPRINT_CHANGED: 'Thiết bị',
    HEARTBEAT_STALE: 'Kết nối',
    NO_CAMERA: 'Camera',
    FACE_NOT_DETECTED: 'Camera',
    MULTIPLE_FACES: 'Camera',
    FACE_SPOOFING_SUSPECTED: 'Camera',
    EYES_OBSTRUCTED: 'Camera',
    FACE_OBSTRUCTED_MASK: 'Camera',
    PARTIAL_FACE_VISIBLE: 'Camera',
    FACE_TOO_FAR: 'Camera',
    FACE_TOO_CLOSE: 'Camera',
    FACE_TURNED_AWAY: 'Camera',
    FACE_NOT_CENTERED: 'Camera',
    EYES_NOT_DETECTED: 'Camera',
    VERY_LOW_LIGHTING: 'Camera',
    LOW_LIGHTING: 'Camera',
    OVEREXPOSED_FRAME: 'Camera',
    VERY_BLURRY_FRAME: 'Camera',
    BLURRY_FRAME: 'Camera',
    EYE_BLINK_ANOMALY: 'Camera',
    EYES_CLOSED_PROLONGED: 'Camera',
    GAZE_OFF_SCREEN: 'Camera',
    RAPID_EYE_MOVEMENT: 'Camera',
    AI_SPEAKING_DETECTED: 'Mic',
    NO_MIC: 'Mic',
    WARNING_SENT: 'Giám thị',
    FRAUD_WARNING: 'Giám thị',
    TEACHER_WARNING: 'Giám thị',
    TEACHER_PAUSE: 'Giám thị',
    TEACHER_RESUME: 'Giám thị',
    TEACHER_INVALIDATE: 'Giám thị',
    NOTE: 'Ghi chú'
  }
  return map[raw] || 'Khác'
}

function normalizeTimelineItem(item = {}) {
  const eventType = normalizeSignalType(item.eventType || item.type || item.warningType || '')
    || String(item.eventType || item.type || item.warningType || '').trim().toUpperCase()
  if (!eventType || !BEHAVIOR_EVENT_TYPES.has(eventType)) return null
  const at = item.at || item.createdAt || item.timestamp || item.issuedAt || item.updatedAt || null
  const details = parseTimelineText(item.details || item.evidence || item.message || item.displayMessage || item.note || '')
  const source = normalizeTimelineSourceLabel(item.source || item.type || '')
  const severity = normalizeSeverity(item.severity || item.riskLevel || '')
  const tone = severityTone(severity, eventType)
  const bucketSize = behaviorBucketSize(eventType)
  const bucketAt = bucketSize > 0
    ? Math.floor(new Date(at || 0).getTime() / bucketSize) * bucketSize
    : new Date(at || 0).getTime()
  return {
    ...item,
    key: item.key || `${eventType}|${at || ''}|${details || source || ''}`,
    eventType,
    label: BEHAVIOR_LABELS[eventType] || eventType,
    details,
    at,
    timeLabel: parseTimelineTime(at),
    categoryLabel: getBehaviorCategoryLabel(item.category || eventType),
    severity,
    severityLabel: getSeverityDisplayLabel(severity),
    severityColor: toneColor(tone),
    severityBg: toneSoftBg(tone),
    color: toneColor(tone),
    iconBg: toneSoftBg(tone),
    icon: BEHAVIOR_ICONS[eventType] || 'activity',
    riskImpact: toNumberOrNull(item.riskImpact),
    confidence: toNumberOrNull(item.confidence),
    source,
    _bucketAt: bucketAt
  }
}

const behaviorEvents = computed(() => {
  const unique = new Map()
  for (const rawItem of behaviorTimelineSource.value) {
    const item = normalizeTimelineItem(rawItem)
    if (!item) continue
    const key = makeBehaviorEventKey(item)
    const existing = unique.get(key)
    if (!existing || preferBehaviorEvent(item, existing)) {
      unique.set(key, { ...item, key })
    }
  }
  return [...unique.values()]
    .sort((a, b) => getTimelineTimestamp(b.at) - getTimelineTimestamp(a.at))
})

const behaviorFilterOptions = computed(() => {
  const counts = new Map()
  for (const event of behaviorEvents.value) {
    counts.set(event.eventType, (counts.get(event.eventType) || 0) + 1)
  }
  return [...counts.entries()]
    .map(([value, count]) => ({ value, count, label: BEHAVIOR_LABELS[value] || value }))
    .sort((a, b) => a.label.localeCompare(b.label, 'vi'))
})

const filteredBehaviorEvents = computed(() => {
  if (!behaviorFilter.value) return behaviorEvents.value
  return behaviorEvents.value.filter((event) => event.eventType === behaviorFilter.value)
})

const behaviorPatterns = computed(() => {
  const counts = new Map()
  for (const event of behaviorEvents.value) {
    counts.set(event.eventType, (counts.get(event.eventType) || 0) + 1)
  }
  return PATTERN_RULES
    .map((rule) => {
      const count = Number(counts.get(rule.key) || 0)
      if (count < rule.threshold) return null
      const level = rule.level(count)
      const tone = level === 'HIGH' ? 'danger' : 'warning'
      return {
        id: rule.id,
        title: rule.title,
        description: rule.description(count),
        level: level === 'HIGH' ? 'Cao' : 'Trung bình',
        icon: rule.icon,
        color: toneColor(tone),
        bgColor: toneSoftBg(tone),
        borderColor: toneColor(tone)
      }
    })
    .filter(Boolean)
})

const riskReference = computed(() => riskSnapshot.value || attemptDetail.value || attemptReport.value || {})

const riskScoreValue = computed(() =>
  Number(riskSnapshot.value?.score ?? attemptDetail.value?.riskScore ?? attemptReport.value?.riskScore ?? 0)
)

const riskLevelValue = computed(() => {
  const raw = String(riskSnapshot.value?.level ?? attemptDetail.value?.riskLevel ?? '').trim().toUpperCase()
  if (raw) return raw
  if (riskScoreValue.value >= 81) return 'CRITICAL'
  if (riskScoreValue.value >= 61) return 'HIGH_RISK'
  if (riskScoreValue.value >= 31) return 'SUSPICIOUS'
  return 'CLEAN'
})

const riskReasons = computed(() => uniqueStrings([
  ...toStringList(riskReference.value?.reasons),
  ...toStringList(attemptDetail.value?.reasons),
  ...toStringList(attemptReport.value?.reasons)
]))

const evidenceSummary = computed(() => uniqueStrings([
  ...toStringList(riskReference.value?.evidenceSummary),
  ...toStringList(attemptDetail.value?.evidenceSummary)
]))

const behaviorSummary = computed(() => {
  const action = String(riskSnapshot.value?.recommendedAction || attemptDetail.value?.recommendedAction || '').trim().toUpperCase()
  const reviewRequired = Boolean(riskReference.value?.reviewRequired || attemptDetail.value?.reviewRequired || behaviorEvents.value.length > 0)
  const suspicious = Boolean(riskReference.value?.suspicious || attemptDetail.value?.suspicious || riskScoreValue.value >= 31)
  const warningCount = Number(attemptDetail.value?.warningCount ?? 0)
  const violationCount = Number(attemptDetail.value?.violationCount ?? behaviorEvents.value.length ?? 0)
  const badgeVariant = RISK_LEVEL_BADGES[riskLevelValue.value] || (riskLevelValue.value === 'CLEAN' ? 'success' : 'warning')
  return {
    riskScore: Math.round(riskScoreValue.value),
    riskLevel: RISK_LEVEL_LABELS[riskLevelValue.value] || riskLevelValue.value || 'Bình thường',
    badgeVariant,
    violationCount,
    warningCount,
    reviewRequired,
    suspiciousLabel: suspicious ? 'Có dấu hiệu' : 'Bình thường',
    recommendedActionLabel: RISK_ACTION_LABELS[action] || (action || 'Tiếp tục giám sát'),
    eventCount: behaviorEvents.value.length
  }
})

const reportTabs = computed(() => [
  { id: 'score', label: 'Điểm', icon: 'grade' },
  { id: 'answers', label: 'Câu trả lời', icon: 'quiz', count: questionsData.value.length },
  { id: 'behavior', label: 'Hành vi gian lận', icon: 'shield-alert', count: behaviorSummary.value.eventCount }
])

function getReportTabStyle(tabId) {
  const active = activeReportTab.value === tabId
  return {
    backgroundColor: active ? 'var(--ds-primary)' : 'var(--ds-surface)',
    color: active ? '#fff' : 'var(--ds-text-muted)',
    borderColor: active ? 'var(--ds-primary)' : 'var(--ds-border)',
    boxShadow: active ? 'var(--ds-shadow-sm)' : 'none'
  }
}

function getReportTabCountStyle(tabId) {
  const active = activeReportTab.value === tabId
  return {
    backgroundColor: active ? 'rgba(255,255,255,0.18)' : 'var(--ds-gray-100)',
    color: active ? '#fff' : 'var(--ds-text-muted)'
  }
}

function formatRiskImpact(value) {
  const number = toNumberOrNull(value)
  if (number == null) return '—'
  return `${number > 0 ? '+' : ''}${number} điểm`
}

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
  router.push({
    path: '/teacher/exams/review/summary',
    query: {
      examId: examId.value,
      title: examTitle.value
    }
  })
}

const loadReport = async () => {
  const requestId = ++latestRequestId
  attemptDetail.value = null
  attemptReport.value = null
  riskSnapshot.value = null
  monitoringTimeline.value = []
  examAttempts.value = []
  behaviorLoadError.value = ''
  if (!attemptId.value) {
    loadError.value = 'Thiếu mã lượt làm. Vui lòng mở lại từ trang tổng quan kết quả.'
    return
  }

  isLoading.value = true
  loadError.value = ''
  try {
    const [detailPayload, reportPayload, attemptsPayload, riskPayload, timelinePayload] = await Promise.allSettled([
      getAttemptDetail(attemptId.value),
      getAttemptReport(attemptId.value),
      examId.value ? listExamAttempts(examId.value) : Promise.resolve([]),
      fetchAttemptRisk(attemptId.value),
      listMonitoringTimeline(attemptId.value)
    ])
    if (requestId !== latestRequestId) {
      return
    }
    if (detailPayload.status !== 'fulfilled') throw detailPayload.reason
    if (reportPayload.status !== 'fulfilled') throw reportPayload.reason
    attemptDetail.value = detailPayload.value
    attemptReport.value = reportPayload.value
    examAttempts.value = attemptsPayload.status === 'fulfilled' && Array.isArray(attemptsPayload.value)
      ? attemptsPayload.value
      : []
    if (riskPayload.status === 'fulfilled') {
      riskSnapshot.value = riskPayload.value || {}
    } else {
      behaviorLoadError.value = riskPayload.reason instanceof Error
        ? riskPayload.reason.message
        : 'Không tải được dữ liệu rủi ro.'
    }
    if (timelinePayload.status === 'fulfilled') {
      const timelineValue = timelinePayload.value
      monitoringTimeline.value = Array.isArray(timelineValue)
        ? timelineValue
        : Array.isArray(timelineValue?.items)
          ? timelineValue.items
          : []
    } else if (!behaviorLoadError.value) {
      behaviorLoadError.value = timelinePayload.reason instanceof Error
        ? timelinePayload.reason.message
        : 'Không tải được dòng thời gian vi phạm.'
    }
  } catch (error) {
    if (requestId !== latestRequestId) {
      return
    }
    loadError.value = error instanceof ApiError ? error.message : 'Không thể tải báo cáo học sinh.'
  } finally {
    if (requestId === latestRequestId) {
      isLoading.value = false
    }
  }
}

onMounted(loadReport)
watch(() => [route.query.examId, route.query.attemptId], loadReport)
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
