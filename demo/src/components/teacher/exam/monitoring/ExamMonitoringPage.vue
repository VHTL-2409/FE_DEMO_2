<template>
  <div class="mon-root">

    <!-- Loading -->
    <div v-if="loading" class="mon-loading">
      <div class="mon-loading__spinner" />
      <span>Đang tải dữ liệu giám sát...</span>
    </div>

    <template v-else>

      <!-- ── Header ─────────────────────────────────────────────── -->
      <header class="mon-header">
        <div class="mon-header__left">
          <RouterLink to="/teacher/live-monitoring" class="portal-icon-btn">
            <LucideIcon name="arrow-left" :size="16" />
          </RouterLink>
          <div>
            <h1 class="mon-header__title">{{ examTitle }}</h1>
            <span v-if="examCode" class="mon-header__code">{{ examCode }}</span>
          </div>
        </div>

        <div class="mon-header__center">
          <!-- LIVE badge -->
          <div class="mon-live-badge">
            <span class="mon-live-badge__dot" />
            LIVE
          </div>

          <!-- Stats chips -->
          <div class="mon-stat-chips">
            <div class="mon-stat-chip mon-stat-chip--online">
              <LucideIcon name="wifi" :size="12" />
              <span>{{ onlineCount }}</span>
              <span class="mon-stat-chip__label">Online</span>
            </div>
            <div class="mon-stat-chip mon-stat-chip--critical">
              <LucideIcon name="shield-alert" :size="12" />
              <span>{{ criticalCount }}</span>
              <span class="mon-stat-chip__label">Critical</span>
            </div>
            <div class="mon-stat-chip mon-stat-chip--warn">
              <LucideIcon name="alert-triangle" :size="12" />
              <span>{{ warnCount }}</span>
              <span class="mon-stat-chip__label">Cảnh báo</span>
            </div>
            <div class="mon-stat-chip mon-stat-chip--total">
              <LucideIcon name="users" :size="12" />
              <span>{{ cards.length }}</span>
              <span class="mon-stat-chip__label">Tổng</span>
            </div>
          </div>
        </div>

        <div class="mon-header__right">
          <!-- WS status -->
          <div class="mon-ws-badge" :class="isConnected ? 'mon-ws-badge--on' : 'mon-ws-badge--off'">
            <span class="mon-ws-badge__dot" />
            {{ isConnected ? 'RT' : '...' }}
          </div>

          <!-- Search -->
          <div class="mon-search">
            <LucideIcon name="search" :size="14" style="color: var(--mon-text-muted); flex-shrink: 0;" />
            <input
              v-model="searchQuery"
              type="text"
              class="mon-search__input"
              placeholder="Tìm học sinh..."
              @input="handleSearchInput"
            />
          </div>

          <!-- Sort -->
          <select v-model="sortBy" class="mon-sort-select">
            <option value="risk">Độ rủi ro</option>
            <option value="name">Tên</option>
            <option value="violations">Vi phạm</option>
            <option value="progress">Tiến độ</option>
          </select>

          <!-- Refresh -->
          <button class="portal-icon-btn" :class="{ 'mon-icon-btn--spin': isRefreshing }" @click="refresh" title="Làm mới">
            <LucideIcon name="refresh-cw" :size="15" />
          </button>
        </div>
      </header>

      <!-- ── Filter Bar ─────────────────────────────────────── -->
      <div class="mon-filter-bar">
        <button
          v-for="tab in filterTabs"
          :key="tab.value"
          class="mon-filter-btn"
          :class="[`mon-filter-btn--${tab.value}`, { 'mon-filter-btn--active': activeFilter === tab.value }]"
          @click="activeFilter = tab.value"
        >
          {{ tab.label }}
          <span class="mon-filter-btn__count">{{ tab.count }}</span>
        </button>
      </div>

      <!-- ── Main Body ─────────────────────────────────────── -->
      <div class="mon-body mon-body--with-sidebar">

        <!-- Student Grid -->
        <section class="mon-grid">
          <div v-if="visibleCards.length === 0" class="mon-empty">
            <LucideIcon name="users" :size="36" />
            <p>Không có học sinh nào phù hợp</p>
          </div>
          <div v-else class="mon-grid__inner">
            <div
              v-for="card in visibleCards"
              :key="card.id"
              class="mon-card"
              :class="[
                `mon-card--${card._riskBandClass}`,
                { 'mon-card--selected': selectedId === String(card.id) }
              ]"
              @click="selectCard(card)"
            >
              <!-- Risk accent bar -->
              <div class="mon-card__accent" />

              <!-- Card head -->
              <div class="mon-card__head">
                <div class="mon-card__avatar-wrap">
                  <div class="mon-card__avatar" :style="{ background: card._avatarBg }">
                    <span :style="{ color: card._avatarColor }">{{ getInitials(card) }}</span>
                  </div>
                  <div class="mon-card__status-dot" :class="`mon-card__status-dot--${card._statusToken?.toLowerCase()}`" />
                </div>
                <div class="mon-card__info">
                  <span class="mon-card__name">{{ card.student || '—' }}</span>
                  <span class="mon-card__email">{{ card.email || '' }}</span>
                </div>
                <div class="mon-card__risk" :style="{ color: card._riskColor, background: card._riskBg }">
                  {{ Math.round(card.riskScore || 0) }}
                </div>
              </div>

              <!-- Progress -->
              <div class="mon-card__progress">
                <div class="mon-card__progress-meta">
                  <span>{{ card.answeredCount || 0 }}/{{ card.totalQuestions || 0 }} câu</span>
                  <span>{{ card._progressPct }}%</span>
                </div>
                <div class="mon-card__progress-bar">
                  <div class="mon-card__progress-fill"
                    :style="{ width: card._progressPct + '%', background: card._riskColor }" />
                </div>
              </div>

              <!-- Card foot -->
              <div class="mon-card__foot">
                <span class="mon-card__status-label" :style="{ color: card._statusColor }">
                  {{ card._statusLabel }}
                </span>
                <span v-if="card.violationCount > 0" class="mon-card__violation">
                  <LucideIcon name="alert-circle" :size="10" />
                  {{ card.violationCount }}
                </span>
                <span v-if="card.reviewRequired" class="mon-card__review-badge">Review</span>
              </div>
            </div>
          </div>
        </section>

        <!-- ── Detail Panel ─────────────────────────────────── -->
        <aside class="mon-detail" :class="{ 'mon-detail--visible': !!selectedCard }">
          <div v-if="!selectedCard" class="mon-detail__empty">
            <LucideIcon name="mouse-pointer-click" :size="28" />
            <p>Chọn học sinh để xem chi tiết</p>
          </div>

          <template v-else>
            <!-- Student info -->
            <div class="mon-detail__student">
              <div class="mon-detail__avatar-wrap">
                <div class="mon-detail__avatar" :style="{ background: selectedCard._avatarBg }">
                  <span :style="{ color: selectedCard._avatarColor }">{{ getInitials(selectedCard) }}</span>
                  <div class="mon-detail__avatar-ring" :style="{ borderColor: selectedCard._statusColor }" />
                </div>
              </div>
              <div class="mon-detail__student-info">
                <div class="mon-detail__name">{{ selectedCard.student || '—' }}</div>
                <div class="mon-detail__badges">
                  <span class="mon-badge" :style="{ color: selectedCard._riskColor, background: selectedCard._riskBg }">
                    <LucideIcon name="shield-alert" :size="10" />
                    {{ selectedCard._riskBandLabel }}
                  </span>
                  <span class="mon-badge mon-badge--status" :style="{ color: selectedCard._statusColor }">
                    {{ selectedCard._statusLabel }}
                  </span>
                </div>
              </div>
              <button class="portal-icon-btn" @click="openDetailPage" title="Mở chi tiết đầy đủ">
                <LucideIcon name="external-link" :size="14" />
              </button>
            </div>

            <!-- Risk gauge -->
            <div class="mon-detail__gauge">
              <svg class="mon-gauge__svg" viewBox="0 0 120 120">
                <circle cx="60" cy="60" r="50" class="mon-gauge__track" />
                <circle cx="60" cy="60" r="50" class="mon-gauge__fill"
                  :stroke="selectedCard._riskColor"
                  :stroke-dasharray="gaugeArc(selectedCard)" />
              </svg>
              <div class="mon-gauge__center">
                <span class="mon-gauge__score" :style="{ color: selectedCard._riskColor }">
                  {{ Math.round(selectedCard.riskScore || 0) }}
                </span>
                <span class="mon-gauge__label">điểm</span>
              </div>
            </div>

            <!-- Meta info -->
            <div class="mon-meta-list">
              <div class="mon-meta-row">
                <LucideIcon name="list-checks" :size="13" />
                <span class="mon-meta-row__label">Tiến độ</span>
                <span class="mon-meta-row__val">
                  {{ selectedCard.answeredCount || 0 }}/{{ selectedCard.totalQuestions || 0 }} câu
                </span>
              </div>
              <div v-if="selectedCard.startedAt" class="mon-meta-row">
                <LucideIcon name="clock" :size="13" />
                <span class="mon-meta-row__label">Đã thi</span>
                <span class="mon-meta-row__val">{{ formatSessionDuration(selectedCard.startedAt) }}</span>
              </div>
              <div class="mon-meta-row">
                <LucideIcon name="monitor" :size="13" />
                <span class="mon-meta-row__label">Thiết bị</span>
                <div class="mon-meta-row__chips">
                  <span class="mon-chip" :class="{ 'mon-chip--on': selectedCard.cameraOn }">
                    <LucideIcon name="camera" :size="10" /> Cam
                  </span>
                  <span class="mon-chip" :class="{ 'mon-chip--on': selectedCard.micOn }">
                    <LucideIcon name="mic" :size="10" /> Mic
                  </span>
                </div>
              </div>
              <div v-if="selectedCard.violationCount > 0" class="mon-meta-row">
                <LucideIcon name="alert-triangle" :size="13" />
                <span class="mon-meta-row__label">Vi phạm</span>
                <span class="mon-meta-row__val mon-meta-row__val--danger">
                  {{ selectedCard.violationCount }}
                </span>
              </div>
            </div>

            <!-- Reasons -->
            <div v-if="selectedCard.reasons?.length" class="mon-detail__reasons">
              <div class="mon-section-label">Lý do rủi ro</div>
              <div class="mon-reason-list">
                <span v-for="reason in selectedCard.reasons.slice(0, 4)" :key="reason" class="mon-reason-tag">
                  {{ reason }}
                </span>
              </div>
            </div>

            <!-- Actions -->
            <div class="mon-detail__actions">
              <button class="mon-action-btn mon-action-btn--warn" @click="openWarnDialog(selectedCard)">
                <LucideIcon name="alert-triangle" :size="13" />
                Gửi cảnh báo
              </button>
              <button
                v-if="selectedCard._isPaused"
                class="mon-action-btn mon-action-btn--success"
                @click="resumeStudent(selectedCard)"
              >
                <LucideIcon name="play" :size="13" />
                Tiếp tục thi
              </button>
              <button
                v-else
                class="mon-action-btn mon-action-btn--pause"
                :disabled="selectedCard._isTerminal"
                @click="pauseStudent(selectedCard)"
              >
                <LucideIcon name="pause" :size="13" />
                Tạm dừng
              </button>
              <button
                class="mon-action-btn mon-action-btn--danger"
                :disabled="selectedCard._isTerminal"
                @click="openStopDialog(selectedCard)"
              >
                <LucideIcon name="x-circle" :size="13" />
                Buộc nộp bài
              </button>
            </div>
          </template>
        </aside>
      </div>

      <!-- ── Alert Feed ─────────────────────────────────────── -->
      <div class="mon-alerts" :class="{ 'mon-alerts--collapsed': alertsCollapsed }">
        <button class="mon-alerts__toggle" @click="alertsCollapsed = !alertsCollapsed">
          <LucideIcon name="bell" :size="14" />
          <span>Luồng cảnh báo</span>
          <span v-if="!alertsCollapsed && liveAlerts.length" class="mon-alerts__badge">
            {{ liveAlerts.length }}
          </span>
          <LucideIcon :name="alertsCollapsed ? 'chevron-up' : 'chevron-down'" :size="14" />
        </button>
        <TransitionGroup v-if="!alertsCollapsed" name="alert-in" tag="div" class="mon-alerts__feed">
          <div v-if="liveAlerts.length === 0" key="empty" class="mon-alerts__empty">
            Chưa có cảnh báo mới
          </div>
          <div
            v-for="alert in liveAlerts"
            :key="alert.id"
            class="mon-alert-item"
            :class="`mon-alert-item--${alert.severityClass || 'warn'}`"
          >
            <LucideIcon name="alert-circle" :size="11" />
            <span class="mon-alert-item__student">{{ alert.studentName || '—' }}</span>
            <span class="mon-alert-item__msg">{{ alert.message || alert.type }}</span>
            <span class="mon-alert-item__time">{{ formatTime(alert.timestamp) }}</span>
          </div>
        </TransitionGroup>
      </div>

    </template>

    <!-- ── Dialogs (PrimeVue) ────────────────────────────────── -->
    <Dialog
      v-model:visible="showWarnDialog"
      header="Gửi cảnh báo"
      :modal="true"
      :closable="true"
      :style="{ width: '420px' }"
      :pt="{
        root: { class: 'portal-surface-card' },
        header: { style: { background: 'var(--mon-surface)', border: 'none', padding: '1rem 1.25rem' } },
        content: { style: { padding: '1rem 1.25rem' } },
        footer: { style: { padding: '0.75rem 1.25rem', border: 'none' } }
      }"
    >
      <p style="font-size: 0.875rem; color: var(--mon-text-secondary); margin: 0 0 1rem;">
        Gửi cảnh báo tới <strong style="color: var(--mon-text);">{{ dialogStudentName }}</strong>
      </p>
      <textarea
        v-model="warningMessage"
        class="mon-dialog__textarea"
        rows="3"
        placeholder="Nội dung cảnh báo (để trống = mặc định)"
        style="width: 100%; padding: 0.625rem 0.875rem; border: 1.5px solid var(--mon-border); border-radius: var(--mon-radius-sm); background: var(--mon-surface-2); color: var(--mon-text); font-size: 0.85rem; resize: vertical; outline: none; font-family: var(--mon-font); transition: border-color 0.15s ease;"
      />
      <template #footer>
        <button class="mon-dialog__cancel" @click="showWarnDialog = false">Hủy</button>
        <button
          class="mon-dialog__confirm mon-dialog__confirm--warn"
          :disabled="actionLoading === 'warning'"
          @click="confirmSendWarning"
        >
          {{ actionLoading === 'warning' ? 'Đang gửi...' : 'Gửi cảnh báo' }}
        </button>
      </template>
    </Dialog>

    <Dialog
      v-model:visible="showStopDialog"
      header="Xác nhận dừng thi"
      :modal="true"
      :closable="true"
      :style="{ width: '420px' }"
      :pt="{
        root: { class: 'portal-surface-card' },
        header: { style: { background: 'var(--mon-surface)', border: 'none', padding: '1rem 1.25rem', color: 'var(--mon-danger)' } },
        content: { style: { padding: '1rem 1.25rem' } },
        footer: { style: { padding: '0.75rem 1.25rem', border: 'none' } }
      }"
    >
      <p style="font-size: 0.875rem; color: var(--mon-text-secondary); margin: 0 0 1rem;">
        Hành động này sẽ <strong style="color: var(--mon-danger);">buộc nộp bài</strong> của
        <strong style="color: var(--mon-text);">{{ dialogStudentName }}</strong>. Không thể hoàn tác.
      </p>
      <textarea
        v-model="stopReason"
        class="mon-dialog__textarea"
        rows="2"
        placeholder="Lý do dừng thi (để trống = mặc định)"
        style="width: 100%; padding: 0.625rem 0.875rem; border: 1.5px solid var(--mon-border); border-radius: var(--mon-radius-sm); background: var(--mon-surface-2); color: var(--mon-text); font-size: 0.85rem; resize: vertical; outline: none; font-family: var(--mon-font); transition: border-color 0.15s ease;"
      />
      <template #footer>
        <button class="mon-dialog__cancel" @click="showStopDialog = false">Hủy</button>
        <button
          class="mon-dialog__confirm mon-dialog__confirm--danger"
          :disabled="actionLoading === 'stop'"
          @click="confirmStop"
        >
          {{ actionLoading === 'stop' ? 'Đang xử lý...' : 'Xác nhận dừng thi' }}
        </button>
      </template>
    </Dialog>

  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import Dialog from 'primevue/dialog'
import LucideIcon from '../../../common/LucideIcon.vue'
import { useToast } from '../../../../composables/useToast'
import { useProctorDashboardStore } from '../../../../stores/proctorDashboardStore'
import { useExamMonitoring } from '../../../../composables/useExamMonitoring'
import {
  fetchExamSummary,
  fetchExamAttempts,
  sendTeacherWarning,
  pauseAttempt,
  resumeAttempt,
  invalidateAttempt
} from '../../../../services/examMonitoringService'

const RISK_BAND_THRESHOLDS = { CRITICAL: 81, HIGH_RISK: 61, SUSPICIOUS: 31 }
const RISK_BAND_LABELS = { CRITICAL: 'Nguy cơ cao', HIGH_RISK: 'Rủi ro cao', SUSPICIOUS: 'Đáng ngờ', CLEAN: 'Bình thường' }
const RISK_COLORS = { CRITICAL: '#dc2626', HIGH_RISK: '#f59e0b', SUSPICIOUS: '#eab308', CLEAN: '#16a34a' }
const STATUS_META = {
  ONLINE:    { label: 'Đang thi',   color: '#4f46e5', bg: 'rgba(79,70,229,0.08)' },
  SUBMITTED: { label: 'Đã nộp',     color: '#16a34a', bg: 'rgba(22,163,74,0.08)' },
  PAUSED:    { label: 'Tạm dừng',  color: '#f59e0b', bg: 'rgba(245,158,11,0.08)' },
  STOPPED:   { label: 'Đã dừng',    color: '#dc2626', bg: 'rgba(220,38,38,0.08)' },
  OFFLINE:   { label: 'Offline',     color: '#94a3b8', bg: 'rgba(148,163,184,0.08)' }
}

const router = useRouter()
const route = useRoute()
const toast = useToast()
const store = useProctorDashboardStore()
const { cards, liveAlerts } = storeToRefs(store)
const { isConnected, connect, disconnect, subscribeToAttempt } = useExamMonitoring()

const examId = computed(() => route.params.examId)
const examTitle = ref('Đang tải...')
const examCode = ref('')
const loading = ref(true)
const isRefreshing = ref(false)
const sortBy = ref('risk')
const activeFilter = ref('all')
const selectedId = ref(null)
const alertsCollapsed = ref(false)
const searchQuery = ref('')

const showWarnDialog = ref(false)
const showStopDialog = ref(false)
const warningMessage = ref('')
const stopReason = ref('')
const actionLoading = ref('')
const dialogStudentName = ref('')
const dialogAttemptId = ref(null)

// Live clock tick — 1s interval for session duration without triggering full re-render
const nowTick = ref(Date.now())
let nowTimer = null
let refreshTimer = null

// ── Enrich cards ──────────────────────────────────────────────────────────────────

const enrichedCards = computed(() =>
  cards.value.map(card => {
    const score = Math.round(card.riskScore || 0)
    let riskBand = 'CLEAN'
    if (score >= RISK_BAND_THRESHOLDS.CRITICAL) riskBand = 'CRITICAL'
    else if (score >= RISK_BAND_THRESHOLDS.HIGH_RISK) riskBand = 'HIGH_RISK'
    else if (score >= RISK_BAND_THRESHOLDS.SUSPICIOUS) riskBand = 'SUSPICIOUS'

    const statusStr = String(card.status || '').toUpperCase()
    let statusToken = 'OFFLINE'
    if (/SUBMITTED/.test(statusStr)) statusToken = 'SUBMITTED'
    else if (/STOPPED/.test(statusStr)) statusToken = 'STOPPED'
    else if (/PAUSED/.test(statusStr)) statusToken = 'PAUSED'
    else if (/ACTIVE|IN_PROGRESS/.test(statusStr)) statusToken = 'ONLINE'

    const statusMeta = STATUS_META[statusToken] || STATUS_META.OFFLINE
    const riskColor = RISK_COLORS[riskBand] || RISK_COLORS.CLEAN
    const riskBg = riskColor === RISK_COLORS.CRITICAL ? 'rgba(220,38,38,0.1)' :
      riskColor === RISK_COLORS.HIGH_RISK ? 'rgba(245,158,11,0.1)' :
      riskColor === RISK_COLORS.SUSPICIOUS ? 'rgba(234,179,8,0.1)' :
      'rgba(22,163,74,0.1)'

    const total = card.totalQuestions || 0
    const answered = card.answeredCount || 0

    return {
      ...card,
      _riskBand,
      _riskBandClass: riskBand.toLowerCase(),
      _riskBandLabel: RISK_BAND_LABELS[riskBand] || riskBand,
      _riskColor: riskColor,
      _riskBg: riskBg,
      _statusToken: statusToken,
      _statusLabel: statusMeta.label,
      _statusColor: statusMeta.color,
      _statusBg: statusMeta.bg,
      _avatarBg: riskBg,
      _avatarColor: riskColor,
      _isPaused: statusToken === 'PAUSED',
      _isTerminal: statusToken === 'SUBMITTED' || statusToken === 'STOPPED',
      _progressPct: total > 0 ? Math.round((answered / total) * 100) : 0
    }
  })
)

// ── Filtered + sorted cards ─────────────────────────────────────────────────────

const visibleCards = computed(() => {
  let result = enrichedCards.value

  // Filter by band
  if (activeFilter.value !== 'all') {
    result = result.filter(c => c._riskBand === activeFilter.value)
  }

  // Filter by search
  const q = searchQuery.value.trim().toLowerCase()
  if (q) {
    result = result.filter(c =>
      (c.student || '').toLowerCase().includes(q) ||
      (c.email || '').toLowerCase().includes(q)
    )
  }

  // Sort
  return [...result].sort((a, b) => {
    if (sortBy.value === 'risk') return (b.riskScore || 0) - (a.riskScore || 0)
    if (sortBy.value === 'name') return (a.student || '').localeCompare(b.student || '')
    if (sortBy.value === 'violations') return (b.violationCount || 0) - (a.violationCount || 0)
    if (sortBy.value === 'progress') return b._progressPct - a._progressPct
    return 0
  })
})

// ── Stats (single-pass count aggregation) ────────────────────────────────────────

const filterTabs = computed(() => {
  const counts = { all: enrichedCards.value.length, CRITICAL: 0, HIGH_RISK: 0, SUSPICIOUS: 0, CLEAN: 0 }
  for (const c of enrichedCards.value) {
    const band = c._riskBand
    if (band in counts) counts[band]++
  }
  return [
    { value: 'all', label: 'Tất cả', count: counts.all },
    { value: 'CRITICAL', label: 'Critical', count: counts.CRITICAL },
    { value: 'HIGH_RISK', label: 'Cao', count: counts.HIGH_RISK },
    { value: 'SUSPICIOUS', label: 'Ngờ', count: counts.SUSPICIOUS },
    { value: 'CLEAN', label: 'Sạch', count: counts.CLEAN }
  ]
})

const onlineCount = computed(() => enrichedCards.value.filter(c => c._statusToken === 'ONLINE').length)
const criticalCount = computed(() => enrichedCards.value.filter(c => c._riskBand === 'CRITICAL').length)
const warnCount = computed(() =>
  enrichedCards.value.filter(c => c._riskBand === 'HIGH_RISK' || c._riskBand === 'SUSPICIOUS').length
)
const selectedCard = computed(() =>
  enrichedCards.value.find(c => String(c.id) === selectedId.value) || null
)

// ── Helpers ─────────────────────────────────────────────────────────────────────

function getInitials(card) {
  const name = card.student || '?'
  const parts = name.trim().split(/\s+/)
  return (parts[parts.length - 1].charAt(0) + (parts.length > 1 ? parts[0].charAt(0) : '')).toUpperCase()
}

function gaugeArc(card) {
  const circ = 2 * Math.PI * 50
  const score = Math.max(0, Math.min(100, Math.round(card.riskScore || 0)))
  return `${(score / 100) * circ} ${circ}`
}

function formatTime(ts) {
  if (!ts) return '—'
  return new Date(ts).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

function formatSessionDuration(startedAt) {
  if (!startedAt) return '—'
  const diff = Math.floor((nowTick.value - new Date(startedAt).getTime()) / 60000)
  if (diff < 1) return '<1 phút'
  if (diff < 60) return `${diff} phút`
  return `${Math.floor(diff / 60)}h ${diff % 60}p`
}

// Debounce search to avoid filtering on every keystroke
let searchTimer = null
function handleSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { /* reactive, no-op needed — v-model handles it */ }, 300)
}

// ── Selection ───────────────────────────────────────────────────────────────────

function selectCard(card) {
  const id = String(card.id)
  if (selectedId.value === id) {
    selectedId.value = null
  } else {
    selectedId.value = id
    subscribeToAttempt(id)
  }
}

function openDetailPage() {
  if (!selectedCard.value) return
  router.push(`/teacher/exams/${examId.value}/monitoring/student/${selectedCard.value.id}`)
}

// ── Data loading ────────────────────────────────────────────────────────────────

async function loadData() {
  if (!examId.value) return
  try {
    const [summary, attempts] = await Promise.allSettled([
      fetchExamSummary(examId.value),
      fetchExamAttempts(examId.value)
    ])
    if (summary.status === 'fulfilled' && summary.value) {
      examTitle.value = summary.value.title || 'Kỳ thi'
      examCode.value = summary.value.code || ''
    }
    if (attempts.status === 'fulfilled' && Array.isArray(attempts.value)) {
      store.setCards(attempts.value)
    }
  } catch { /* error shown via template */ }
  finally {
    loading.value = false
  }
}

async function refresh() {
  if (isRefreshing.value) return
  isRefreshing.value = true
  try { await loadData() } finally { isRefreshing.value = false }
}

function startAutoRefresh() {
  stopAutoRefresh()
  refreshTimer = setInterval(() => {
    if (!document.hidden && !isRefreshing.value) void loadData()
  }, 30000)
}

function stopAutoRefresh() {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
}

// ── Dialog openers ───────────────────────────────────────────────────────────────

function openWarnDialog(card) {
  dialogStudentName.value = card.student || '—'
  dialogAttemptId.value = card.id
  warningMessage.value = ''
  showWarnDialog.value = true
}

function openStopDialog(card) {
  dialogStudentName.value = card.student || '—'
  dialogAttemptId.value = card.id
  stopReason.value = ''
  showStopDialog.value = true
}

function pauseStudent(card) {
  dialogStudentName.value = card.student || '—'
  dialogAttemptId.value = card.id
  runAction({ key: 'pause', call: pauseAttempt, successMsg: 'Đã tạm dừng bài thi.', errorMsg: 'Tạm dừng thất bại.' })
}

function resumeStudent(card) {
  dialogStudentName.value = card.student || '—'
  dialogAttemptId.value = card.id
  runAction({ key: 'resume', call: resumeAttempt, successMsg: 'Đã cho phép tiếp tục.', errorMsg: 'Khôi phục thất bại.' })
}

// ── Action runner ────────────────────────────────────────────────────────────────

async function runAction({ key, call, successMsg, errorMsg }) {
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
  await runAction({ key: 'warning', call: (id) => sendTeacherWarning(id, warningMessage.value), successMsg: 'Đã gửi cảnh báo.', errorMsg: 'Gửi cảnh báo thất bại.' })
}

async function confirmStop() {
  showStopDialog.value = false
  await runAction({ key: 'stop', call: (id) => invalidateAttempt(id, stopReason.value), successMsg: 'Đã dừng bài thi.', errorMsg: 'Dừng thi thất bại.' })
}

// ── Lifecycle ───────────────────────────────────────────────────────────────────

watch(examId, (id) => {
  if (id) {
    selectedId.value = null
    loading.value = true
    store.reset()
    void loadData()
    connect(id)
  }
}, { immediate: true })

onMounted(() => {
  nowTimer = setInterval(() => { nowTick.value = Date.now() }, 1000)
  startAutoRefresh()
})

onUnmounted(() => {
  clearInterval(nowTimer)
  stopAutoRefresh()
  disconnect()
})
</script>

<style scoped>
@import '../../../../styles/monitoring.css';

/* ── Dialog textarea ──────────────────────────────────────────────── */
.mon-dialog__textarea {
  width: 100%;
  box-sizing: border-box;
}

/* ── Dialog buttons ───────────────────────────────────────────────── */
.mon-dialog__cancel {
  padding: 0.5rem 1rem;
  border-radius: var(--mon-radius-sm);
  border: 1.5px solid var(--mon-border);
  background: var(--mon-surface);
  color: var(--mon-text-secondary);
  font-size: 0.825rem;
  font-weight: 600;
  cursor: pointer;
  font-family: var(--mon-font);
  transition: all var(--mon-transition);
}
.mon-dialog__cancel:hover { border-color: var(--mon-text-secondary); color: var(--mon-text); }

.mon-dialog__confirm {
  padding: 0.5rem 1rem;
  border-radius: var(--mon-radius-sm);
  border: none;
  font-size: 0.825rem;
  font-weight: 700;
  cursor: pointer;
  font-family: var(--mon-font);
  transition: filter var(--mon-transition);
}
.mon-dialog__confirm:disabled { opacity: 0.5; cursor: not-allowed; }
.mon-dialog__confirm--warn { background: var(--mon-warning); color: #0f172a; }
.mon-dialog__confirm--warn:hover:not(:disabled) { filter: brightness(1.05); }
.mon-dialog__confirm--danger { background: var(--mon-danger); color: white; }
.mon-dialog__confirm--danger:hover:not(:disabled) { filter: brightness(1.05); }

/* ── Alert transitions ────────────────────────────────────────────── */
.alert-in-enter-active { transition: all 0.3s ease; }
.alert-in-leave-active { transition: all 0.2s ease; }
.alert-in-enter-from { opacity: 0; transform: translateY(-8px); }
.alert-in-leave-to { opacity: 0; transform: translateX(20px); }
.alert-in-move { transition: transform 0.3s ease; }

/* ── Header title ────────────────────────────────────────────────── */
.mon-header__title {
  font-size: 1rem;
  font-weight: 800;
  color: var(--mon-text);
  margin: 0;
  line-height: 1.2;
}
.mon-header__code {
  font-size: 0.7rem;
  color: var(--mon-text-muted);
  font-family: monospace;
}

/* ── Responsive ───────────────────────────────────────────────────── */
@media (max-width: 1024px) {
  .mon-body--with-sidebar { grid-template-columns: 1fr !important; }
  .mon-detail { position: static; }
  .mon-grid__inner { grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); }
}
@media (max-width: 640px) {
  .mon-header { flex-direction: column; align-items: flex-start; gap: 0.75rem; }
  .mon-header__center { flex-wrap: wrap; width: 100%; }
  .mon-filter-bar { padding: 0.5rem 1rem; }
  .mon-body { padding: 0.75rem; }
}
</style>
