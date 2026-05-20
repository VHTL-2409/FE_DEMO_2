<template>
  <main class="emp-root">
    <!-- Loading -->
    <section
      v-if="loading"
      class="emp-loading"
    >
      <div class="emp-spinner" />
      <p>Đang tải phòng thi…</p>
    </section>

    <!-- Main content -->
    <template v-else>
      <!-- ── Header ─────────────────────────────────────────── -->
      <header class="emp-header">
        <RouterLink
          to="/teacher/live-monitoring"
          class="emp-back"
          aria-label="Quay lại danh sách kỳ thi"
        >
          <LucideIcon name="arrow-left" :size="18" />
        </RouterLink>

        <div class="emp-header__main">
          <div class="emp-header__meta">
            <span class="emp-label">Phòng giám sát</span>
            <span
              v-if="examStatus"
              class="emp-status-badge"
              :class="`emp-status-badge--${examStatus.toLowerCase()}`"
            >
              <i class="emp-status-dot" />
              {{ examStatus === 'LIVE' ? 'LIVE' : examStatus === 'ENDED' ? 'Đã kết thúc' : examStatus }}
            </span>
          </div>
          <h1 class="emp-title">{{ examTitle }}</h1>
          <p v-if="examCode" class="emp-subtitle">{{ examCode }}</p>
        </div>

        <div class="emp-header__actions">
          <span
            class="emp-conn"
            :class="isConnected ? 'emp-conn--on' : 'emp-conn--off'"
            :title="isConnected ? 'Kết nối realtime' : 'Đang dùng polling'"
          >
            <i class="emp-conn-dot" />
            {{ isConnected ? 'Realtime' : 'Polling' }}
          </span>
          <button
            class="emp-icon-btn"
            type="button"
            :disabled="isRefreshing"
            aria-label="Làm mới dữ liệu"
            title="Làm mới"
            @click="refresh"
          >
            <LucideIcon
              name="refresh-cw"
              :size="16"
              :class="{ 'emp-spin': isRefreshing }"
            />
          </button>
        </div>
      </header>

      <!-- ── Toolbar ─────────────────────────────────────────── -->
      <div class="emp-toolbar">
        <div class="emp-search" role="search">
          <LucideIcon name="search" :size="16" />
          <input
            v-model="searchQuery"
            type="text"
            name="studentSearch"
            autocomplete="off"
            spellcheck="false"
            placeholder="Tìm thí sinh…"
            aria-label="Tìm thí sinh"
          >
          <button
            v-if="searchQuery"
            class="emp-search-clear"
            type="button"
            aria-label="Xóa tìm kiếm"
            @click="searchQuery = ''"
          >
            <LucideIcon name="x" :size="14" />
          </button>
        </div>

        <div class="emp-toolbar__right">
          <div class="emp-filters" role="tablist" aria-label="Lọc theo mức rủi ro">
            <button
              v-for="tab in riskFilterTabs"
              :key="tab.value"
              type="button"
              role="tab"
              :aria-selected="activeRiskFilter === tab.value"
              :class="['emp-filter-btn', `emp-filter-btn--${tab.value.toLowerCase()}`, { active: activeRiskFilter === tab.value }]"
              @click="activeRiskFilter = tab.value"
            >
              {{ tab.label }}
              <span class="emp-filter-count">{{ tab.count }}</span>
            </button>
          </div>

          <div class="emp-filters" role="tablist" aria-label="Lọc theo trạng thái">
            <button
              v-for="tab in statusFilterTabs"
              :key="tab.value"
              type="button"
              role="tab"
              :aria-selected="activeStatusFilter === tab.value"
              :class="['emp-filter-btn', { active: activeStatusFilter === tab.value }]"
              @click="activeStatusFilter = tab.value"
            >
              {{ tab.label }}
              <span class="emp-filter-count">{{ tab.count }}</span>
            </button>
          </div>

          <label class="emp-sort">
            <span>Sắp xếp</span>
            <select v-model="sortBy" name="monitoringSort" aria-label="Sắp xếp">
              <option value="riskScore">Risk cao nhất</option>
              <option value="latestSignalAt">Tín hiệu mới nhất</option>
              <option value="name">Tên (A-Z)</option>
            </select>
          </label>
        </div>
      </div>

      <!-- ── Body: Table + Panel ─────────────────────────────── -->
      <div class="emp-metrics" aria-label="Tổng quan giám sát">
        <article
          v-for="metric in monitoringMetrics"
          :key="metric.label"
          class="emp-metric"
          :class="`emp-metric--${metric.tone}`"
        >
          <div class="emp-metric__icon">
            <LucideIcon :name="metric.icon" :size="16" />
          </div>
          <div class="emp-metric__body">
            <span class="emp-metric__label">{{ metric.label }}</span>
            <strong class="emp-metric__value">{{ metric.value }}</strong>
          </div>
          <span class="emp-metric__note">{{ metric.note }}</span>
        </article>
      </div>

      <div class="emp-body">
        <!-- Left: Student table -->
        <section class="emp-table-wrap" aria-label="Danh sách thí sinh">
          <!-- Table header -->
          <div class="emp-table-head">
            <span class="emp-th">Học sinh</span>
            <span class="emp-th emp-th--center">Trạng thái</span>
            <span class="emp-th emp-th--center">Rủi ro</span>
            <span class="emp-th">Flag</span>
            <span class="emp-th">Hành vi mới</span>
          </div>

          <!-- Empty state -->
          <div v-if="visibleCards.length === 0" class="emp-empty">
            <LucideIcon name="users" :size="32" />
            <p>Không có thí sinh phù hợp</p>
          </div>

          <!-- Rows -->
          <div
            v-for="card in visibleCards"
            :key="getCardId(card)"
            class="emp-row"
            :class="{
              'emp-row--selected': selectedId === String(getCardId(card)),
              [`emp-row--${card._riskBand?.toLowerCase()}`]: true
            }"
            role="button"
            tabindex="0"
            :aria-label="`Chọn ${card.student || 'học sinh'} để xem chi tiết`"
            @click="selectCard(card)"
            @keydown.enter="selectCard(card)"
            @keydown.space.prevent="selectCard(card)"
          >
            <!-- Student -->
            <div class="emp-cell emp-cell--student">
              <span
                class="emp-avatar"
                :style="{ background: card._riskBg, color: card._riskColor }"
              >
                {{ getInitials(card) }}
              </span>
              <div class="emp-student-info">
                <span class="emp-student-name">{{ card.student || 'Chưa có tên' }}</span>
                <span class="emp-student-email">{{ card.email || card.studentCode || '' }}</span>
              </div>
            </div>

            <!-- Status -->
            <div class="emp-cell emp-cell--center">
              <span
                class="emp-status-chip"
                :class="`emp-status-chip--${card._statusToken?.toLowerCase()}`"
              >
                {{ card._statusLabel }}
              </span>
            </div>

            <!-- Risk -->
            <div class="emp-cell emp-cell--center emp-cell--risk">
              <span
                class="emp-risk-score"
                :style="{ color: card._riskColor }"
              >
                {{ Math.round(card.riskScore || 0) }}
              </span>
              <span
                class="emp-level-badge"
                :class="`emp-level-badge--${card._riskBand?.toLowerCase()}`"
              >
                {{ card._riskBandLabel }}
              </span>
            </div>

            <!-- Active flag -->
            <div class="emp-cell">
              <span
                v-if="card.activeFlagStatus && card.activeFlagStatus !== 'DISMISSED'"
                class="emp-flag-chip"
                :class="`emp-flag-chip--${card.activeFlagStatus?.toLowerCase()}`"
              >
                <LucideIcon name="flag" :size="12" />
                {{ getFlagStatusLabel(card.activeFlagStatus) }}
              </span>
              <span v-else class="emp-flag-chip emp-flag-chip--none">—</span>
            </div>

            <!-- Latest behavior -->
            <div class="emp-cell emp-cell--signal">
              <span class="emp-signal-type">{{ getSignalLabel(card.latestSignalType) }}</span>
              <span class="emp-signal-time">{{ formatTimeAgo(card.latestSignalAt || card.lastSignalAt) }}</span>
            </div>
          </div>
        </section>

        <!-- Right: Quick detail panel -->
        <aside class="emp-panel" aria-label="Thông tin chi tiết nhanh">
          <div v-if="!selectedCard" class="emp-panel-empty">
            <LucideIcon name="mouse-pointer-click" :size="28" />
            <p>Chọn thí sinh để xem nhanh.</p>
          </div>

          <template v-else>
            <!-- Student header -->
            <div class="emp-panel-head">
              <div class="emp-panel-student">
                <span
                  class="emp-avatar emp-avatar--lg"
                  :style="{ background: selectedCard._riskBg, color: selectedCard._riskColor }"
                >
                  {{ getInitials(selectedCard) }}
                </span>
                <div>
                  <h2 class="emp-panel-name">{{ selectedCard.student || 'Chưa có tên' }}</h2>
                  <p class="emp-panel-email">{{ selectedCard.email || selectedCard.studentCode || 'Chưa có thông tin' }}</p>
                </div>
              </div>
              <button
                type="button"
                class="emp-icon-btn emp-icon-btn--sm"
                title="Mở chi tiết"
                aria-label="Mở chi tiết thí sinh"
                @click="goToDetail(selectedCard)"
              >
                <LucideIcon name="external-link" :size="15" />
              </button>
            </div>

            <div class="emp-panel-summary">
              <span class="emp-panel-summary__chip emp-panel-summary__chip--status">
                {{ selectedCard._statusLabel }}
              </span>
              <span class="emp-panel-summary__chip emp-panel-summary__chip--risk" :style="{ color: selectedCard._riskColor }">
                {{ Math.round(selectedCard.riskScore || 0) }} · {{ selectedCard._riskBandLabel }}
              </span>
              <span
                v-if="selectedCard.activeFlagStatus && selectedCard.activeFlagStatus !== 'DISMISSED'"
                class="emp-panel-summary__chip emp-panel-summary__chip--flag"
              >
                {{ getFlagStatusLabel(selectedCard.activeFlagStatus) }}
              </span>
              <span class="emp-panel-summary__chip emp-panel-summary__chip--signal">
                {{ getSignalLabel(selectedCard.latestSignalType) }} · {{ formatTimeAgo(selectedCard.latestSignalAt || selectedCard.lastSignalAt) }}
              </span>
            </div>

            <!-- Actions -->
            <div class="emp-panel-actions">
              <button
                type="button"
                class="emp-btn emp-btn--warn"
                :disabled="actionLoading === 'warning'"
                @click="openWarnDialog(selectedCard)"
              >
                <LucideIcon name="send" :size="15" />
                Gửi cảnh báo
              </button>

              <button
                v-if="selectedCard._isPaused"
                type="button"
                class="emp-btn emp-btn--success"
                :disabled="actionLoading === 'resume' || !backendCapabilities.resume"
                :title="!backendCapabilities.resume ? 'Chưa hỗ trợ backend' : ''"
                @click="resumeStudent(selectedCard)"
              >
                <LucideIcon name="play" :size="15" />
                Cho tiếp tục
              </button>
              <button
                v-else
                type="button"
                class="emp-btn emp-btn--pause"
                :disabled="selectedCard._isTerminal || actionLoading === 'pause' || !backendCapabilities.pause"
                :title="!backendCapabilities.pause ? 'Chưa hỗ trợ backend' : ''"
                @click="pauseStudent(selectedCard)"
              >
                <LucideIcon name="pause" :size="15" />
                Tạm dừng
              </button>

              <button
                type="button"
                class="emp-btn emp-btn--danger"
                :disabled="selectedCard._isTerminal || actionLoading === 'invalidate' || !backendCapabilities.invalidate"
                :title="!backendCapabilities.invalidate ? 'Chưa hỗ trợ backend' : ''"
                @click="forceSubmitStudent(selectedCard)"
              >
                <LucideIcon name="stop-circle" :size="15" />
                Buộc nộp bài
              </button>
            </div>
          </template>
        </aside>
      </div>
    </template>

    <!-- ── Warning Dialog ─────────────────────────────────────── -->
    <Dialog
      v-model:visible="showWarnDialog"
      header="Gửi cảnh báo"
      :modal="true"
      :closable="true"
      :style="{ width: '420px' }"
    >
      <p class="emp-dialog-info">
        Gửi cảnh báo tới <strong>{{ dialogStudentName }}</strong>
      </p>
      <textarea
        v-model="warningMessage"
        class="emp-textarea"
        rows="3"
        placeholder="Nội dung cảnh báo (không bắt buộc)"
        aria-label="Nội dung cảnh báo"
      />
      <template #footer>
        <button
          type="button"
          class="emp-dialog-btn"
          @click="showWarnDialog = false"
        >
          Hủy
        </button>
        <button
          type="button"
          class="emp-dialog-btn emp-dialog-btn--warn"
          :disabled="actionLoading === 'warning'"
          @click="confirmSendWarning"
        >
          {{ actionLoading === 'warning' ? 'Đang gửi…' : 'Gửi cảnh báo' }}
        </button>
      </template>
    </Dialog>
  </main>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, onActivated } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import LucideIcon from '../../../common/LucideIcon.vue'
import { useToast } from '../../../../composables/useToast'
import { useProctorDashboardStore } from '../../../../stores/proctorDashboardStore'
import { useExamMonitoring } from '../../../../composables/useExamMonitoring'
import {
  fetchExamSummary,
  fetchExamAttemptsFilter,
  fetchProctorSessionAlerts,
  sendTeacherWarning,
  pauseAttempt,
  resumeAttempt,
  invalidateAttempt
} from '../../../../services/examMonitoringService'
import { normalizeSignalType } from '../../../../utils/proctorSignalTypes'

// ── Constants ───────────────────────────────────────────────────────────
const RISK_BAND_THRESHOLDS = { CRITICAL: 81, HIGH_RISK: 61, SUSPICIOUS: 31 }
const RISK_BAND_LABELS = {
  CRITICAL: 'Nguy cơ cao',
  HIGH_RISK: 'Rủi ro cao',
  SUSPICIOUS: 'Đáng ngờ',
  CLEAN: 'Bình thường'
}
const RISK_COLORS = {
  CRITICAL: 'var(--ds-risk-critical)',
  HIGH_RISK: 'var(--ds-risk-high)',
  SUSPICIOUS: 'var(--ds-risk-moderate)',
  CLEAN: 'var(--ds-risk-clean)'
}
const RISK_BG_SOFT = {
  CRITICAL: 'rgba(220, 38, 38, 0.08)',
  HIGH_RISK: 'rgba(234, 88, 12, 0.08)',
  SUSPICIOUS: 'rgba(217, 119, 6, 0.08)',
  CLEAN: 'rgba(22, 163, 74, 0.08)'
}
const STATUS_META = {
  ONLINE: { label: 'Đang thi', token: 'ONLINE' },
  SUBMITTED: { label: 'Đã nộp', token: 'SUBMITTED' },
  PAUSED: { label: 'Tạm dừng', token: 'PAUSED' },
  STOPPED: { label: 'Đã dừng', token: 'STOPPED' },
  OFFLINE: { label: 'Offline', token: 'OFFLINE' }
}
const SIGNAL_LABELS = {
  NO_CAMERA: 'Camera tắt',
  NO_MIC: 'Micro tắt',
  FACE_NOT_DETECTED: 'Không có khuôn mặt',
  MULTIPLE_FACES: 'Nhiều khuôn mặt',
  FACE_SPOOFING_SUSPECTED: 'Nghi vấn giả mạo',
  FACE_OBSTRUCTED_MASK: 'Khuôn mặt bị che',
  EYES_OBSTRUCTED: 'Mắt bị che',
  PARTIAL_FACE_VISIBLE: 'Mặt không đầy đủ',
  FACE_TOO_FAR: 'Mặt quá xa',
  FACE_TOO_CLOSE: 'Mặt quá gần',
  FACE_TURNED_AWAY: 'Quay mặt đi',
  FACE_NOT_CENTERED: 'Mặt lệch tâm',
  EYES_NOT_DETECTED: 'Không phát hiện mắt',
  EYE_BLINK_ANOMALY: 'Nháy mắt bất thường',
  EYES_CLOSED_PROLONGED: 'Nhắm mắt lâu',
  GAZE_OFF_SCREEN: 'Nhìn lệch màn hình',
  RAPID_EYE_MOVEMENT: 'Chuyển động mắt nhanh',
  VERY_LOW_LIGHTING: 'Ánh sáng rất yếu',
  LOW_LIGHTING: 'Ánh sáng yếu',
  OVEREXPOSED_FRAME: 'Ảnh quá sáng',
  VERY_BLURRY_FRAME: 'Ảnh rất mờ',
  BLURRY_FRAME: 'Ảnh mờ',
  AI_SPEAKING_DETECTED: 'Tiếng ồn',
  PRINTED_PHOTO: 'Ảnh in',
  SCREEN_REPLAY: 'Phát lại màn hình',
  DEEPFAKE: 'Nghi vấn deepfake',
  FLAT_IMAGE: 'Ảnh phẳng',
  SCREEN_DISPLAY: 'Ảnh từ màn hình',
  TAB_SWITCH: 'Chuyển tab',
  WINDOW_BLUR: 'Mất tiêu điểm',
  LONG_SCREEN_LEAVE: 'Rời màn hình lâu',
  EXIT_FULLSCREEN: 'Thoát toàn màn hình',
  COPY_PASTE: 'Sao chép / dán',
  DEVTOOLS_OPEN: 'Mở DevTools',
  RIGHT_CLICK: 'Chuột phải',
  PRINT_SCREEN: 'Chụp màn hình',
  MULTI_MONITOR: 'Nhiều màn hình',
  IP_CHANGED: 'IP thay đổi',
  DUPLICATE_IP: 'IP trùng lặp',
  IP_FINGERPRINT_GRAPH: 'Trùng IP / thiết bị',
  DEVICE_FINGERPRINT_CHANGED: 'Thay đổi thiết bị',
  MULTIPLE_DEVICE_SESSION: 'Nhiều thiết bị',
  WARNING_SENT: 'Cảnh báo',
  ATTEMPT_PAUSED: 'Tạm dừng',
  ATTEMPT_RESUMED: 'Tiếp tục'
}

// ── Router & stores ──────────────────────────────────────────────────
const router = useRouter()
const route = useRoute()
const toast = useToast()
const store = useProctorDashboardStore()
const { isConnected, connect, disconnect } = useExamMonitoring()

const examId = computed(() => route.params.examId)
const examTitle = ref('Đang tải…')
const examCode = ref('')
const examStatus = ref('')
const loading = ref(true)
const isRefreshing = ref(false)
const backendCapabilities = ref({
  pause: true,
  resume: true,
  warn: true,
  invalidate: true
})

// Filters
const activeRiskFilter = ref('ALL')
const activeStatusFilter = ref('ALL')
const sortBy = ref('riskScore')
const searchQuery = ref('')
const selectedId = ref(null)

// Dialog state
const showWarnDialog = ref(false)
const dialogStudentName = ref('')
const dialogAttemptId = ref(null)
const warningMessage = ref('')
const actionLoading = ref('')

let refreshTimer = null

// ── Computed ─────────────────────────────────────────────────────────
const enrichedCards = computed(() => cards.value.map(card => {
  const score = Math.round(card.riskScore || 0)
  let riskBand = 'CLEAN'
  if (score >= RISK_BAND_THRESHOLDS.CRITICAL) riskBand = 'CRITICAL'
  else if (score >= RISK_BAND_THRESHOLDS.HIGH_RISK) riskBand = 'HIGH_RISK'
  else if (score >= RISK_BAND_THRESHOLDS.SUSPICIOUS) riskBand = 'SUSPICIOUS'

  const statusStr = String(card.status || '').toUpperCase()
  let statusToken = 'OFFLINE'
  if (/SUBMITTED|COMPLETED/.test(statusStr)) statusToken = 'SUBMITTED'
  else if (/STOPPED/.test(statusStr)) statusToken = 'STOPPED'
  else if (/PAUSED/.test(statusStr)) statusToken = 'PAUSED'
  else if (/ACTIVE|IN_PROGRESS/.test(statusStr)) statusToken = 'ONLINE'

  const identityStatus = card.identityStatus || card.identityVerified ? 'VERIFIED' : 'UNVERIFIED'

  return {
    ...card,
    _riskBand: riskBand,
    _riskBandLabel: RISK_BAND_LABELS[riskBand],
    _riskColor: RISK_COLORS[riskBand],
    _riskBg: RISK_BG_SOFT[riskBand],
    _statusToken: statusToken,
    _statusLabel: STATUS_META[statusToken]?.label || statusToken,
    _identityStatus: identityStatus,
    _isPaused: statusToken === 'PAUSED',
    _isTerminal: statusToken === 'SUBMITTED' || statusToken === 'STOPPED'
  }
}))

const cards = computed(() => store.cards)

const onlineCount = computed(() => enrichedCards.value.filter(c => c._statusToken === 'ONLINE').length)
const openFlagCount = computed(() =>
  enrichedCards.value.filter(c =>
    c.activeFlagStatus && !['DISMISSED', 'CONFIRMED'].includes(String(c.activeFlagStatus).toUpperCase())
  ).length
)

const highRiskCount = computed(() =>
  enrichedCards.value.filter(c => ['HIGH_RISK', 'CRITICAL'].includes(c._riskBand)).length
)

const selectedCard = computed(() =>
  enrichedCards.value.find(c => String(getCardId(c)) === selectedId.value) || null
)

const visibleCards = computed(() => {
  let list = enrichedCards.value

  // Search
  const q = searchQuery.value.trim().toLowerCase()
  if (q) {
    list = list.filter(c =>
      `${c.student || ''} ${c.email || ''} ${c.studentCode || ''}`.toLowerCase().includes(q)
    )
  }

  // Risk filter
  if (activeRiskFilter.value !== 'ALL') {
    list = list.filter(c => c._riskBand === activeRiskFilter.value)
  }

  // Status filter
  if (activeStatusFilter.value !== 'ALL') {
    list = list.filter(c => c._statusToken === activeStatusFilter.value)
  }

  // Sort
  return [...list].sort((a, b) => {
    if (sortBy.value === 'riskScore') {
      return Number(b.riskScore || 0) - Number(a.riskScore || 0)
    }
    if (sortBy.value === 'latestSignalAt') {
      const aTime = new Date(a.latestSignalAt || a.lastSignalAt || 0).getTime()
      const bTime = new Date(b.latestSignalAt || b.lastSignalAt || 0).getTime()
      return bTime - aTime
    }
    if (sortBy.value === 'name') {
      return String(a.student || '').localeCompare(String(b.student || ''))
    }
    return 0
  })
})

const riskFilterTabs = computed(() => [
  { label: 'Tất cả', value: 'ALL', count: enrichedCards.value.length },
  { label: 'Bình thường', value: 'CLEAN', count: enrichedCards.value.filter(c => c._riskBand === 'CLEAN').length },
  { label: 'Đáng ngờ', value: 'SUSPICIOUS', count: enrichedCards.value.filter(c => c._riskBand === 'SUSPICIOUS').length },
  { label: 'Cao', value: 'HIGH_RISK', count: enrichedCards.value.filter(c => c._riskBand === 'HIGH_RISK').length },
  { label: 'Nghiêm trọng', value: 'CRITICAL', count: enrichedCards.value.filter(c => c._riskBand === 'CRITICAL').length }
])

const statusFilterTabs = computed(() => [
  { label: 'Tất cả', value: 'ALL', count: enrichedCards.value.length },
  { label: 'Đang thi', value: 'ONLINE', count: enrichedCards.value.filter(c => c._statusToken === 'ONLINE').length },
  { label: 'Tạm dừng', value: 'PAUSED', count: enrichedCards.value.filter(c => c._statusToken === 'PAUSED').length },
  { label: 'Đã nộp', value: 'SUBMITTED', count: enrichedCards.value.filter(c => c._statusToken === 'SUBMITTED').length }
])

// ── Helpers ──────────────────────────────────────────────────────────
const monitoringMetrics = computed(() => [
  {
    label: 'Tổng thí sinh',
    value: enrichedCards.value.length,
    note: `${visibleCards.value.length} đang hiển thị`,
    icon: 'users',
    tone: 'neutral'
  },
  {
    label: 'Đang thi',
    value: onlineCount.value,
    note: 'Theo dõi realtime',
    icon: 'user-check',
    tone: 'success'
  },
  {
    label: 'Rủi ro cao',
    value: highRiskCount.value,
    note: 'Cần ưu tiên',
    icon: 'shield-alert',
    tone: 'danger'
  },
  {
    label: 'Flag mở',
    value: openFlagCount.value,
    note: 'Chưa xử lý',
    icon: 'flag',
    tone: 'warning'
  }
])

function getCardId(card) {
  return card?.id ?? card?.attemptId
}

function getInitials(card) {
  const name = card.student || card.name || '?'
  const parts = name.trim().split(/\s+/)
  return (parts.at(-1)?.[0] || '?').toUpperCase()
}

function getSignalLabel(type) {
  const signalType = normalizeSignalType(type)
  if (!signalType) return '—'
  return SIGNAL_LABELS[signalType] || signalType.replace(/_/g, ' ')
}

function getFlagStatusLabel(status) {
  const normalized = String(status || '').toUpperCase()
  if (normalized === 'OPEN') return 'Đang mở'
  if (normalized === 'CONFIRMED') return 'Đã xác nhận'
  if (normalized === 'DISMISSED') return 'Đã bỏ qua'
  return status || '—'
}

function formatTimeAgo(ts) {
  if (!ts) return '—'
  try {
    const diff = Date.now() - new Date(ts).getTime()
    if (diff < 60000) return 'Vừa xong'
    if (diff < 3600000) return `${Math.floor(diff / 60000)} phút trước`
    if (diff < 86400000) return `${Math.floor(diff / 3600000)} giờ trước`
    return new Date(ts).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
  } catch {
    return '—'
  }
}

function selectCard(card) {
  selectedId.value = String(getCardId(card))
}

function goToDetail(card) {
  if (card) {
    router.push(`/teacher/exams/${examId.value}/monitoring/student/${getCardId(card)}`)
  }
}

// ── Data loading ─────────────────────────────────────────────────────
async function loadData() {
  if (!examId.value) return
  loading.value = true
  try {
    const [summary, attempts, alerts] = await Promise.all([
      fetchExamSummary(examId.value).catch(() => ({})),
      fetchExamAttemptsFilter(examId.value, {}).catch(() => []),
      fetchProctorSessionAlerts(examId.value).catch(() => [])
    ])

    examTitle.value = summary?.title || summary?.name || 'Kỳ thi'
    examCode.value = summary?.code || ''
    examStatus.value = summary?.status || 'LIVE'

    // Merge alerts into attempts
    const alertsMap = new Map((alerts || []).map(a => [String(a.attemptId), a]))
    const merged = (attempts || []).map(attempt => {
      const alert = alertsMap.get(String(getCardId(attempt)))
      if (!alert) return attempt
      return {
        ...attempt,
        riskScore: alert.riskScore ?? attempt.riskScore,
        riskLevel: alert.riskLevel ?? attempt.riskLevel,
        reviewRequired: alert.reviewRequired ?? attempt.reviewRequired,
        reasons: alert.reasons?.length ? alert.reasons : attempt.reasons,
        evidenceSummary: alert.evidenceSummary?.length ? alert.evidenceSummary : attempt.evidenceSummary,
        activeFlagId: alert.activeFlagId ?? attempt.activeFlagId,
        activeFlagStatus: alert.activeFlagStatus ?? attempt.activeFlagStatus,
        latestFlagTitle: alert.latestFlagTitle ?? attempt.latestFlagTitle
      }
    })

    store.setCards(merged)

    // Auto-select first card if none selected
    if (!selectedId.value && merged.length) {
      selectedId.value = String(getCardId(merged[0]))
    }
  } catch (err) {
    toast.error(err?.message || 'Không thể tải dữ liệu giám sát.')
  } finally {
    loading.value = false
  }
}

async function refresh() {
  isRefreshing.value = true
  try {
    await loadData()
  } finally {
    isRefreshing.value = false
  }
}

// ── Actions ───────────────────────────────────────────────────────────
function openWarnDialog(card) {
  dialogStudentName.value = card.student || 'Chưa có tên'
  dialogAttemptId.value = getCardId(card)
  warningMessage.value = ''
  showWarnDialog.value = true
}

async function runAction(key, call, successMsg, errorMsg) {
  if (!dialogAttemptId.value) return
  actionLoading.value = key
  try {
    await call(dialogAttemptId.value)
    toast.success(successMsg)
    await refresh()
  } catch (err) {
    toast.error(err?.message || errorMsg)
  } finally {
    actionLoading.value = ''
  }
}

async function confirmSendWarning() {
  showWarnDialog.value = false
  await runAction(
    'warning',
    id => sendTeacherWarning(id, warningMessage.value),
    'Đã gửi cảnh báo.',
    'Gửi cảnh báo thất bại.'
  )
}

function pauseStudent(card) {
  if (!backendCapabilities.value.pause) return
  dialogAttemptId.value = getCardId(card)
  void runAction('pause', pauseAttempt, 'Đã tạm dừng bài thi.', 'Tạm dừng thất bại.')
}

function resumeStudent(card) {
  if (!backendCapabilities.value.resume) return
  dialogAttemptId.value = getCardId(card)
  void runAction('resume', resumeAttempt, 'Đã cho phép tiếp tục.', 'Khôi phục thất bại.')
}

function forceSubmitStudent(card) {
  if (!backendCapabilities.value.invalidate || card._isTerminal) return
  dialogAttemptId.value = getCardId(card)
  void runAction('invalidate', invalidateAttempt, 'Đã buộc nộp bài.', 'Buộc nộp bài thất bại.')
}

// ── Watchers ────────────────────────────────────────────────────────
watch(examId, id => {
  if (id) {
    selectedId.value = null
    store.reset()
    void loadData()
    connect(id)
  }
}, { immediate: true })

onMounted(() => {
  refreshTimer = window.setInterval(() => {
    if (!document.hidden && isConnected()) void refresh()
  }, 30000)
})

onUnmounted(() => {
  if (refreshTimer) window.clearInterval(refreshTimer)
  disconnect()
})

onActivated(() => {
  // Reload data when navigating back to this page
  void loadData()
})
</script>

<style scoped>
/* ── Reset & root ─────────────────────────────────────────────────── */
.emp-root {
  min-height: 100vh;
  background: var(--ds-bg);
  color: var(--ds-text);
  font-family: var(--ds-font);
  overflow-x: hidden;
}

/* ── Loading ─────────────────────────────────────────────────────── */
.emp-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--ds-space-3);
  min-height: 100vh;
  color: var(--ds-text-secondary);
}
.emp-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--ds-gray-200);
  border-top-color: var(--ds-primary);
  border-radius: 50%;
  animation: emp-spin 0.8s linear infinite;
}
@keyframes emp-spin { to { transform: rotate(360deg); } }

/* ── Header ─────────────────────────────────────────────────────── */
.emp-header {
  display: flex;
  align-items: center;
  gap: var(--ds-space-4);
  padding: var(--ds-space-4) var(--ds-space-5);
  background: var(--ds-surface);
  border-bottom: 1px solid var(--ds-border);
  flex-wrap: wrap;
}

.emp-back,
.emp-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  color: var(--ds-text-secondary);
  background: var(--ds-surface-muted);
  cursor: pointer;
  flex-shrink: 0;
  transition: color var(--ds-duration-base) var(--ds-easing),
    background-color var(--ds-duration-base) var(--ds-easing),
    border-color var(--ds-duration-base) var(--ds-easing),
    box-shadow var(--ds-duration-base) var(--ds-easing),
    opacity var(--ds-duration-base) var(--ds-easing);
  outline: 2px solid transparent;
  outline-offset: 2px;
}

.emp-back:hover,
.emp-icon-btn:hover:not(:disabled) {
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
}

.emp-back:focus-visible,
.emp-icon-btn:focus-visible {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.18);
}

.emp-icon-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.emp-icon-btn--sm {
  width: 32px;
  height: 32px;
}

.emp-header__main {
  flex: 1;
  min-width: 0;
}

.emp-header__meta {
  display: flex;
  align-items: center;
  gap: var(--ds-space-2);
  margin-bottom: 2px;
}

.emp-label {
  font-size: var(--ds-text-xs);
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--ds-primary);
}

.emp-status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 8px;
  border-radius: 9999px;
  font-size: var(--ds-text-xs);
  font-weight: 700;
}

.emp-status-badge--live {
  background: var(--ds-success-soft);
  color: var(--ds-success);
  border: 1px solid rgba(22, 163, 74, 0.2);
}

.emp-status-badge--ended {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  border: 1px solid var(--ds-border);
}

.emp-status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.emp-status-badge--live .emp-status-dot {
  animation: emp-pulse 1.5s ease-in-out infinite;
}

@keyframes emp-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.emp-title {
  margin: 0;
  font-size: var(--ds-text-2xl);
  font-weight: 800;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.emp-subtitle {
  margin: 2px 0 0;
  font-size: var(--ds-text-sm);
  color: var(--ds-text-secondary);
}

.emp-header__actions {
  display: flex;
  align-items: center;
  gap: var(--ds-space-2);
}

.emp-conn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: 9999px;
  font-size: var(--ds-text-xs);
  font-weight: 700;
  border: 1px solid;
}

.emp-conn--on {
  color: var(--ds-success);
  border-color: rgba(22, 163, 74, 0.2);
  background: var(--ds-success-soft);
}

.emp-conn--off {
  color: var(--ds-text-muted);
  border-color: var(--ds-border);
  background: var(--ds-surface-muted);
}

.emp-conn-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.emp-conn--on .emp-conn-dot {
  animation: emp-pulse 2s ease-in-out infinite;
}

.emp-spin {
  animation: emp-spin 0.8s linear infinite;
}

/* ── Toolbar ─────────────────────────────────────────────────────── */
.emp-toolbar {
  display: flex;
  align-items: center;
  gap: var(--ds-space-3);
  padding: var(--ds-space-3) var(--ds-space-5);
  background: var(--ds-surface);
  border-bottom: 1px solid var(--ds-border);
  flex-wrap: wrap;
}

.emp-toolbar__right {
  display: flex;
  align-items: center;
  gap: var(--ds-space-3);
  flex-wrap: wrap;
  margin-left: auto;
}

.emp-search {
  display: flex;
  align-items: center;
  gap: var(--ds-space-2);
  width: 220px;
  min-height: 38px;
  padding: 0 var(--ds-space-3);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  transition: border-color var(--ds-duration-base), box-shadow var(--ds-duration-base), background-color var(--ds-duration-base);
}

.emp-search:focus-within {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12);
}

.emp-search input {
  flex: 1;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--ds-text);
  font-size: var(--ds-text-sm);
  font-family: inherit;
}

.emp-search input::placeholder {
  color: var(--ds-text-muted);
}

.emp-search-clear {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 50%;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  cursor: pointer;
  flex-shrink: 0;
}

.emp-search-clear:hover {
  background: var(--ds-gray-200);
  color: var(--ds-text);
}

.emp-filters {
  display: flex;
  gap: 4px;
}

.emp-filter-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 28px;
  padding: 4px 10px;
  border: 1.5px solid transparent;
  border-radius: var(--ds-radius-full);
  font-size: var(--ds-text-xs);
  font-weight: 700;
  color: var(--ds-text-secondary);
  background: transparent;
  cursor: pointer;
  transition: color var(--ds-duration-base), background-color var(--ds-duration-base), border-color var(--ds-duration-base), box-shadow var(--ds-duration-base);
  font-family: inherit;
  white-space: nowrap;
  outline: 2px solid transparent;
  outline-offset: 2px;
}

.emp-filter-btn:hover {
  background: var(--ds-surface-muted);
  color: var(--ds-text);
}

.emp-filter-btn.active {
  background: var(--ds-surface-muted);
  border-color: var(--ds-border);
  color: var(--ds-text);
}

.emp-filter-btn:focus-visible {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12);
}

.emp-filter-btn--critical.active {
  background: rgba(220, 38, 38, 0.08);
  border-color: rgba(220, 38, 38, 0.2);
  color: var(--ds-risk-critical);
}

.emp-filter-btn--high_risk.active {
  background: rgba(234, 88, 12, 0.08);
  border-color: rgba(234, 88, 12, 0.2);
  color: var(--ds-risk-high);
}

.emp-filter-btn--suspicious.active {
  background: rgba(217, 119, 6, 0.08);
  border-color: rgba(217, 119, 6, 0.2);
  color: var(--ds-risk-moderate);
}

.emp-filter-btn--clean.active {
  background: var(--ds-success-soft);
  border-color: rgba(22, 163, 74, 0.2);
  color: var(--ds-risk-clean);
}

.emp-filter-count {
  min-width: 1.3rem;
  text-align: center;
  font-size: 10px;
  padding: 0 4px;
  border-radius: 9999px;
  background: rgba(0, 0, 0, 0.06);
  color: inherit;
  opacity: 0.7;
}

.emp-sort {
  display: flex;
  align-items: center;
  gap: var(--ds-space-2);
  font-size: var(--ds-text-sm);
  color: var(--ds-text-secondary);
}

.emp-sort select {
  padding: 4px 28px 4px 10px;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  font-size: var(--ds-text-sm);
  font-weight: 600;
  color: var(--ds-text);
  background: var(--ds-surface);
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2364748b' d='M2 4l4 4 4-4'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  transition: border-color var(--ds-duration-base), box-shadow var(--ds-duration-base), background-color var(--ds-duration-base);
  outline: 2px solid transparent;
  outline-offset: 2px;
}

.emp-sort select:focus-visible {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12);
}

/* ── Body layout ─────────────────────────────────────────────────── */
.emp-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--ds-space-3);
  padding: var(--ds-space-3) var(--ds-space-5) 0;
}

.emp-metric {
  display: flex;
  align-items: center;
  gap: var(--ds-space-3);
  min-width: 0;
  padding: var(--ds-space-3);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  background: var(--ds-surface);
}

.emp-metric__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-md);
  flex-shrink: 0;
  background: var(--ds-surface-muted);
  color: var(--ds-text-secondary);
}

.emp-metric__body {
  min-width: 0;
  flex: 1;
}

.emp-metric__label {
  display: block;
  font-size: var(--ds-text-xs);
  font-weight: 700;
  color: var(--ds-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.emp-metric__value {
  display: block;
  margin-top: 2px;
  font-size: var(--ds-text-xl);
  font-weight: 900;
  line-height: 1;
  color: var(--ds-text);
  font-variant-numeric: tabular-nums;
}

.emp-metric__note {
  align-self: flex-start;
  margin-left: auto;
  padding: 2px 6px;
  border-radius: 9999px;
  font-size: 10px;
  font-weight: 700;
  color: var(--ds-text-muted);
  background: var(--ds-gray-100);
  white-space: nowrap;
}

.emp-metric--success .emp-metric__icon {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.emp-metric--warning .emp-metric__icon {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.emp-metric--danger .emp-metric__icon {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.emp-metric--neutral .emp-metric__icon {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.emp-body {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: var(--ds-space-4);
  padding: var(--ds-space-4) var(--ds-space-5) var(--ds-space-5);
  min-height: calc(100vh - 130px);
  align-items: start;
}

/* ── Table ──────────────────────────────────────────────────────── */
.emp-table-wrap {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  overflow: hidden;
  box-shadow: var(--ds-shadow-sm);
}

.emp-table-head {
  display: grid;
  grid-template-columns: 2fr 100px 132px 100px 1fr;
  gap: 1px;
  padding: 0 var(--ds-space-3);
  background: var(--ds-gray-50);
  border-bottom: 1px solid var(--ds-border);
}

.dark .emp-table-head {
  background: var(--ds-gray-800);
}

.emp-th {
  padding: var(--ds-space-2) var(--ds-space-2);
  font-size: var(--ds-text-xs);
  font-weight: 800;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.emp-th--center {
  text-align: center;
}

.emp-row {
  display: grid;
  grid-template-columns: 2fr 100px 132px 100px 1fr;
  gap: 1px;
  padding: 0 var(--ds-space-3);
  border-bottom: 1px solid var(--ds-border);
  cursor: pointer;
  transition: background-color var(--ds-duration-fast), border-color var(--ds-duration-fast), box-shadow var(--ds-duration-fast);
  border-left: 3px solid transparent;
  align-items: center;
  outline: 2px solid transparent;
  outline-offset: -2px;
}

.emp-row:last-child {
  border-bottom: none;
}

.emp-row:hover {
  background: var(--ds-gray-50);
}

.emp-row:focus-visible {
  background: var(--ds-primary-soft);
  box-shadow: inset 0 0 0 2px rgba(79, 70, 229, 0.22);
}

.dark .emp-row:hover {
  background: var(--ds-gray-800);
}

.emp-row--selected {
  background: var(--ds-primary-soft);
}

.emp-row--critical { border-left-color: var(--ds-risk-critical); }
.emp-row--high_risk { border-left-color: var(--ds-risk-high); }
.emp-row--suspicious { border-left-color: var(--ds-risk-moderate); }
.emp-row--clean { border-left-color: transparent; }

.emp-cell {
  padding: var(--ds-space-2) var(--ds-space-2);
  min-width: 0;
}

.emp-cell--center {
  text-align: center;
}

.emp-cell--student {
  display: flex;
  align-items: center;
  gap: var(--ds-space-2);
}

.emp-cell--signal {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.emp-cell--risk {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

/* Avatar */
.emp-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--ds-text-xs);
  font-weight: 900;
  flex-shrink: 0;
}

.emp-avatar--lg {
  width: 40px;
  height: 40px;
  font-size: var(--ds-text-sm);
}

/* Student info */
.emp-student-info {
  min-width: 0;
}

.emp-student-name {
  display: block;
  font-size: var(--ds-text-sm);
  font-weight: 700;
  color: var(--ds-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.emp-student-email {
  display: block;
  font-size: var(--ds-text-xs);
  color: var(--ds-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Status chip */
.emp-status-chip {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 9999px;
  font-size: var(--ds-text-xs);
  font-weight: 700;
  white-space: nowrap;
}

.emp-status-chip--online {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.emp-status-chip--submitted {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
}

.emp-status-chip--paused {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.emp-status-chip--stopped {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.emp-status-chip--offline {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
}

/* Risk score */
.emp-risk-score {
  font-size: var(--ds-text-base);
  font-weight: 900;
  font-variant-numeric: tabular-nums;
}

/* Level badge */
.emp-level-badge {
  display: inline-block;
  padding: 2px 6px;
  border-radius: var(--ds-radius-sm);
  font-size: 10px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.emp-level-badge--lg {
  padding: 4px 8px;
  font-size: var(--ds-text-xs);
}

.emp-level-badge--critical {
  background: var(--ds-risk-critical-soft);
  color: var(--ds-risk-critical);
}

.emp-level-badge--high_risk {
  background: var(--ds-risk-high-soft);
  color: var(--ds-risk-high);
}

.emp-level-badge--suspicious {
  background: var(--ds-risk-moderate-soft);
  color: var(--ds-risk-moderate);
}

.emp-level-badge--clean {
  background: var(--ds-success-soft);
  color: var(--ds-risk-clean);
}

/* Flag chip */
.emp-flag-chip {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 6px;
  border-radius: 9999px;
  font-size: 10px;
  font-weight: 700;
}

.emp-flag-chip--open {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.emp-flag-chip--confirmed {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.emp-flag-chip--none {
  color: var(--ds-text-muted);
}

/* Signal */
.emp-signal-type {
  display: block;
  font-size: var(--ds-text-xs);
  font-weight: 700;
  color: var(--ds-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.emp-signal-time {
  display: block;
  font-size: 10px;
  color: var(--ds-text-muted);
  font-variant-numeric: tabular-nums;
}

/* Empty */
.emp-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--ds-space-3);
  padding: var(--ds-space-10);
  color: var(--ds-text-secondary);
  text-align: center;
}

/* ── Detail panel ────────────────────────────────────────────────── */
.emp-panel {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  padding: var(--ds-space-4);
  position: sticky;
  top: var(--ds-space-4);
  box-shadow: var(--ds-shadow-sm);
}

.emp-panel-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--ds-space-3);
  padding: var(--ds-space-10) var(--ds-space-4);
  color: var(--ds-text-muted);
  text-align: center;
}

.emp-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ds-space-3);
  margin-bottom: var(--ds-space-4);
  padding-bottom: var(--ds-space-4);
  border-bottom: 1px solid var(--ds-border);
}

.emp-panel-student {
  display: flex;
  align-items: center;
  gap: var(--ds-space-3);
  min-width: 0;
}

.emp-panel-name {
  margin: 0;
  font-size: var(--ds-text-lg);
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.emp-panel-email {
  margin: 2px 0 0;
  font-size: var(--ds-text-sm);
  color: var(--ds-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.emp-panel-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-bottom: var(--ds-space-4);
}

.emp-panel-summary__chip {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-height: 28px;
  padding: 3px 8px;
  border-radius: var(--ds-radius-full);
  font-size: var(--ds-text-xs);
  font-weight: 800;
  line-height: 1.2;
  background: var(--ds-gray-100);
  color: var(--ds-text-secondary);
  min-width: 0;
}

.emp-panel-summary__chip--status {
  background: var(--ds-gray-100);
}

.emp-panel-summary__chip--risk {
  background: var(--ds-surface-muted);
}

.emp-panel-summary__chip--flag {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.emp-panel-summary__chip--signal {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Panel actions */
.emp-panel-actions {
  display: flex;
  flex-direction: column;
  gap: var(--ds-space-2);
}

.emp-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--ds-space-2);
  min-height: 40px;
  padding: 0 var(--ds-space-4);
  border: 1.5px solid;
  border-radius: var(--ds-radius-md);
  font-size: var(--ds-text-sm);
  font-weight: 700;
  cursor: pointer;
  transition: color var(--ds-duration-base), background-color var(--ds-duration-base), border-color var(--ds-duration-base), box-shadow var(--ds-duration-base), opacity var(--ds-duration-base);
  font-family: inherit;
  outline: 2px solid transparent;
  outline-offset: 2px;
  white-space: nowrap;
}

.emp-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.emp-btn:focus-visible {
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.14);
}

.emp-btn--warn {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
  border-color: var(--ds-warning);
}

.emp-btn--warn:hover:not(:disabled) {
  background: rgba(217, 119, 6, 0.12);
}

.emp-btn--pause {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border-color: var(--ds-primary);
}

.emp-btn--pause:hover:not(:disabled) {
  background: rgba(79, 70, 229, 0.1);
}

.emp-btn--success {
  background: var(--ds-success-soft);
  color: var(--ds-success);
  border-color: var(--ds-success);
}

.emp-btn--success:hover:not(:disabled) {
  background: rgba(22, 163, 74, 0.12);
}

.emp-btn--detail {
  background: var(--ds-surface);
  color: var(--ds-text);
  border-color: var(--ds-border);
}

.emp-btn--detail:hover:not(:disabled) {
  background: var(--ds-gray-50);
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
}

.emp-btn--danger {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
  border-color: var(--ds-danger);
}

.emp-btn--danger:hover:not(:disabled) {
  background: rgba(220, 38, 38, 0.12);
}

/* Dialog */
.emp-dialog-info {
  margin: 0 0 var(--ds-space-3);
  font-size: var(--ds-text-sm);
  color: var(--ds-text-secondary);
}

.emp-dialog-info strong {
  color: var(--ds-text);
}

.emp-textarea {
  width: 100%;
  box-sizing: border-box;
  padding: var(--ds-space-3);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  font-size: var(--ds-text-sm);
  font-family: inherit;
  color: var(--ds-text);
  background: var(--ds-surface);
  resize: vertical;
  outline: 2px solid transparent;
  outline-offset: 2px;
  transition: border-color var(--ds-duration-base), box-shadow var(--ds-duration-base);
}

.emp-textarea:focus-visible {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12);
}

.emp-dialog-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--ds-space-2);
  min-height: 38px;
  padding: 0 var(--ds-space-4);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  font-size: var(--ds-text-sm);
  font-weight: 700;
  cursor: pointer;
  transition: color var(--ds-duration-base), background-color var(--ds-duration-base), border-color var(--ds-duration-base), box-shadow var(--ds-duration-base), opacity var(--ds-duration-base);
  font-family: inherit;
  background: var(--ds-surface);
  color: var(--ds-text);
  outline: 2px solid transparent;
  outline-offset: 2px;
}

.emp-dialog-btn:hover {
  background: var(--ds-gray-50);
}

.emp-dialog-btn:focus-visible {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12);
}

.emp-dialog-btn--warn {
  background: var(--ds-warning);
  color: #fff;
  border-color: var(--ds-warning);
}

.emp-dialog-btn--warn:hover:not(:disabled) {
  filter: brightness(1.05);
}

.emp-dialog-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ── Responsive ─────────────────────────────────────────────────── */
@media (max-width: 1200px) {
  .emp-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .emp-body {
    grid-template-columns: 1fr;
  }

  .emp-panel {
    position: static;
  }

  .emp-table-head,
  .emp-row {
    grid-template-columns: 2fr 90px 120px 90px 1fr;
  }
}

@media (max-width: 900px) {
  .emp-header {
    padding: var(--ds-space-3);
    gap: var(--ds-space-3);
  }

  .emp-toolbar {
    padding: var(--ds-space-3);
  }

  .emp-metrics {
    padding: var(--ds-space-3);
  }

  .emp-toolbar__right {
    margin-left: 0;
    width: 100%;
  }

  .emp-search {
    width: 100%;
  }

  .emp-body {
    padding: var(--ds-space-3);
  }
}

@media (max-width: 600px) {
  .emp-metrics {
    grid-template-columns: 1fr;
  }

  .emp-table-head {
    display: none;
  }

  .emp-row {
    display: flex;
    flex-direction: column;
    gap: var(--ds-space-2);
    padding: var(--ds-space-3);
    border-left-width: 3px;
    border-bottom: 1px solid var(--ds-border);
  }

  .emp-cell--center,
  .emp-cell--action {
    justify-content: flex-start;
    text-align: left;
  }

  .emp-cell--student {
    margin-bottom: var(--ds-space-1);
  }
}

/* ── Reduced motion ─────────────────────────────────────────────── */
@media (prefers-reduced-motion: reduce) {
  .emp-spin,
  .emp-conn-dot,
  .emp-status-dot {
    animation: none;
  }

  .emp-row,
  .emp-filter-btn,
  .emp-btn,
  .emp-back,
  .emp-icon-btn,
  .emp-dialog-btn,
  .emp-sort select,
  .emp-textarea,
  .emp-search,
  .emp-search input {
    transition: none;
  }
}
</style>
