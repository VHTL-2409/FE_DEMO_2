<template>
  <div class="sd-root">

    <!-- Loading -->
    <div v-if="loading" class="sd-loading">
      <div class="sd-loading__spinner" />
      <span class="sd-loading__text">Đang tải dữ liệu...</span>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="sd-error">
      <div class="sd-error__card">
        <LucideIcon name="alert-circle" :size="28" class="sd-error__icon" />
        <p class="sd-error__msg">{{ error }}</p>
        <button class="sd-btn sd-btn--secondary" @click="loadData">Thử lại</button>
      </div>
    </div>

    <template v-else>
      <!-- Top bar -->
      <div class="sd-topbar">
        <div class="sd-topbar__left">
          <button class="sd-topbar__back" @click="goBack">
            <LucideIcon name="arrow-left" :size="16" />
            <span>Quay lại</span>
          </button>
          <div class="sd-topbar__sep" />
          <nav class="sd-topbar__crumbs">
            <RouterLink to="/teacher/live-monitoring" class="sd-topbar__crumb">Giám sát thi</RouterLink>
            <LucideIcon name="chevron-right" :size="12" class="sd-topbar__crumb-sep" />
            <RouterLink :to="{ path: '/teacher/exams/review/incidents', query: { examId } }" class="sd-topbar__crumb">Hành vi</RouterLink>
            <LucideIcon name="chevron-right" :size="12" class="sd-topbar__crumb-sep" />
            <span class="sd-topbar__crumb sd-topbar__crumb--active">{{ studentName }}</span>
          </nav>
        </div>
        <div v-if="attemptId" class="sd-topbar__id">
          <LucideIcon name="hash" :size="12" />
          <span>{{ attemptId }}</span>
        </div>
      </div>

      <!-- Hero -->
      <div class="sd-hero" :class="`sd-hero--${riskBand}`">
        <div class="sd-hero__left">
          <div class="sd-hero__avatar-wrap">
            <div class="sd-hero__avatar" :style="{ background: avatarBg }">
              <span class="sd-hero__initials" :style="{ color: riskColor }">{{ studentInitials }}</span>
              <div class="sd-hero__status-ring" :style="{ borderColor: attemptStatusColor }" />
            </div>
          </div>
          <div class="sd-hero__info">
            <div class="sd-hero__name-row">
              <h1 class="sd-hero__name">{{ studentName }}</h1>
              <span class="sd-hero__status-badge" :style="{ color: attemptStatusColor, background: attemptStatusBg }">
                <LucideIcon :name="attemptStatusIcon" :size="11" />
                {{ attemptStatusLabel }}
              </span>
            </div>
            <p class="sd-hero__meta">
              <span v-if="studentCode">{{ studentCode }}</span>
              <span v-if="studentCode && studentEmail" class="sd-hero__meta-sep">·</span>
              <span v-if="studentEmail">{{ studentEmail }}</span>
            </p>
            <div class="sd-hero__badges">
              <span class="sd-badge" :style="{ background: riskBg, color: riskColor }">
                <LucideIcon name="shield-alert" :size="11" />
                {{ riskLevelLabel }}
              </span>
              <span v-if="riskData.reviewRequired" class="sd-badge sd-badge--warn">
                <LucideIcon name="eye" :size="11" />
                Cần review
              </span>
              <span class="sd-badge sd-badge--neutral">
                <LucideIcon name="clock" :size="11" />
                {{ sessionTime }}
              </span>
            </div>
          </div>
        </div>

        <div class="sd-hero__right">
          <!-- Circular risk gauge -->
          <div class="sd-gauge">
            <svg class="sd-gauge__svg" viewBox="0 0 120 120">
              <circle class="sd-gauge__track" cx="60" cy="60" r="52" />
              <circle
                class="sd-gauge__fill"
                cx="60" cy="60" r="52"
                :stroke="riskColor"
                :stroke-dasharray="gaugeArc"
              />
            </svg>
            <div class="sd-gauge__center">
              <span class="sd-gauge__score" :style="{ color: riskColor }">{{ riskScore }}</span>
              <span class="sd-gauge__label">điểm rủi ro</span>
            </div>
          </div>
          <div class="sd-hero__gauge-desc">
            <p class="sd-gauge-desc__text">{{ riskDescription }}</p>
            <div class="sd-gauge-desc__track">
              <div class="sd-gauge-desc__fill" :style="{ width: riskScore + '%', background: riskColor }" />
            </div>
          </div>
        </div>
      </div>

      <!-- Stats row -->
      <div class="sd-stats">
        <div class="sd-stat">
          <div class="sd-stat__icon-wrap" style="color: #94a3b8">
            <LucideIcon name="users" :size="16" />
          </div>
          <div class="sd-stat__body">
            <span class="sd-stat__value">{{ store.cards.length }}</span>
            <span class="sd-stat__label">Tổng thí sinh</span>
          </div>
        </div>
        <div class="sd-stat">
          <div class="sd-stat__icon-wrap" style="color: #4ade80">
            <LucideIcon name="play-circle" :size="16" />
          </div>
          <div class="sd-stat__body">
            <span class="sd-stat__value" style="color: #4ade80">{{ onlineCount }}</span>
            <span class="sd-stat__label">Đang thi</span>
          </div>
        </div>
        <div class="sd-stat">
          <div class="sd-stat__icon-wrap" style="color: #fbbf24">
            <LucideIcon name="alert-triangle" :size="16" />
          </div>
          <div class="sd-stat__body">
            <span class="sd-stat__value" style="color: #fbbf24">{{ alertCount }}</span>
            <span class="sd-stat__label">Nghi ngờ</span>
          </div>
        </div>
        <div class="sd-stat">
          <div class="sd-stat__icon-wrap" style="color: #a5b4fc">
            <LucideIcon name="check-circle" :size="16" />
          </div>
          <div class="sd-stat__body">
            <span class="sd-stat__value" style="color: #a5b4fc">{{ submittedCount }}</span>
            <span class="sd-stat__label">Đã nộp</span>
          </div>
        </div>
        <div class="sd-stat">
          <div class="sd-stat__icon-wrap" :style="{ color: attemptStatusColor }">
            <LucideIcon :name="attemptStatusIcon" :size="16" />
          </div>
          <div class="sd-stat__body">
            <span class="sd-stat__value" :style="{ color: attemptStatusColor }">{{ maxSeverity }}</span>
            <span class="sd-stat__label">{{ attemptStatusLabel }}</span>
          </div>
        </div>
      </div>

      <div class="sd-grid">
        <!-- Left column -->
        <div class="sd-grid__left">

          <!-- Recommendation -->
          <div v-if="riskData.reviewRequired || riskData.recommendedAction || (riskData.reasons && riskData.reasons.length)" class="sd-card">
            <div class="sd-card__header">
              <LucideIcon name="lightbulb" :size="16" />
              <span class="sd-card__title">Đề xuất xử lý</span>
            </div>
            <div class="sd-card__body">
              <div class="sd-rec__action">
                <span class="sd-rec__badge" :style="{ background: riskBg, color: riskColor }">{{ recommendedActionLabel }}</span>
              </div>
              <div v-if="riskData.reasons && riskData.reasons.length" class="sd-rec__reasons">
                <p class="sd-rec__label">Lý do chính</p>
                <div class="sd-rec__chips">
                  <span
                    v-for="reason in riskData.reasons"
                    :key="reason"
                    class="sd-chip"
                    :style="{ background: 'rgba(251,191,36,0.1)', color: '#fbbf24' }"
                  >{{ reason }}</span>
                </div>
              </div>
              <div v-if="riskData.evidenceSummary && riskData.evidenceSummary.length" class="sd-rec__evidence">
                <p class="sd-rec__label">Bằng chứng tóm tắt</p>
                <ul class="sd-rec__list">
                  <li v-for="(item, i) in riskData.evidenceSummary" :key="i" class="sd-rec__item">
                    <LucideIcon name="circle-dot" :size="10" class="sd-rec__item-dot" />
                    {{ item }}
                  </li>
                </ul>
              </div>
            </div>
          </div>

          <!-- Timeline -->
          <div class="sd-card">
            <div class="sd-card__header">
              <LucideIcon name="activity" :size="16" />
              <span class="sd-card__title">Dòng thời gian vi phạm</span>
              <span class="sd-card__badge">{{ timelineEvents.length }}</span>
            </div>
            <div class="sd-card__body sd-card__body--flush">
              <div v-if="timelineEvents.length === 0" class="sd-empty">
                <LucideIcon name="check-circle" :size="32" class="sd-empty__icon" />
                <p>Không có vi phạm nào được ghi nhận</p>
              </div>
              <div v-else class="sd-timeline">
                <div v-for="event in timelineEvents" :key="event.key" class="sd-tl-item">
                  <div class="sd-tl-item__dot" :style="{ background: getVColor(event.eventType) }">
                    <LucideIcon :name="getVIcon(event.eventType)" :size="10" style="color: white" />
                  </div>
                  <div class="sd-tl-item__content">
                    <div class="sd-tl-item__header">
                      <span class="sd-tl-item__type" :style="{ color: getVColor(event.eventType) }">{{ getVLabel(event.eventType) }}</span>
                      <span class="sd-tl-item__time">{{ formatTime(event.at) }}</span>
                    </div>
                    <p v-if="event.details" class="sd-tl-item__details">{{ event.details }}</p>
                    <span class="sd-tl-item__severity" :class="`sd-tl-item__severity--${getSeverityStatus(event.severity)}`">
                      {{ getSeverityLabel(event.severity) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

        </div>

        <!-- Right column -->
        <div class="sd-grid__right">

          <!-- Risk breakdown -->
          <div v-if="hasBreakdown" class="sd-card">
            <div class="sd-card__header">
              <LucideIcon name="bar-chart-2" :size="16" />
              <span class="sd-card__title">Phân tích điểm rủi ro</span>
            </div>
            <div class="sd-card__body">
              <div v-for="(score, key) in riskData.breakdown" :key="key" class="sd-breakdown">
                <span class="sd-breakdown__label">{{ getVLabel(key) }}</span>
                <div class="sd-breakdown__bar">
                  <div class="sd-breakdown__fill" :style="{ width: Math.min(score, 100) + '%', background: sColor(score) }" />
                </div>
                <span class="sd-breakdown__score" :style="{ color: sColor(score) }">{{ score }}</span>
              </div>
            </div>
          </div>

          <!-- Suspicious patterns -->
          <div v-if="suspiciousPatterns.length > 0" class="sd-card">
            <div class="sd-card__header">
              <LucideIcon name="brain" :size="16" />
              <span class="sd-card__title">Mẫu hành vi đáng ngờ</span>
              <span class="sd-card__badge sd-card__badge--danger">{{ suspiciousPatterns.length }}</span>
            </div>
            <div class="sd-card__body">
              <div
                v-for="p in suspiciousPatterns"
                :key="p.id"
                class="sd-pattern"
                :style="{ background: pBg(p.level), borderColor: pBorder(p.level) }"
              >
                <div class="sd-pattern__head">
                  <LucideIcon name="alert-triangle" :size="13" :style="{ color: pColor(p.level) }" />
                  <span class="sd-pattern__title">{{ p.title }}</span>
                  <span class="sd-pattern__level" :style="{ background: pColor(p.level) }">{{ p.level }}</span>
                </div>
                <p class="sd-pattern__desc">{{ p.description }}</p>
              </div>
            </div>
          </div>

          <!-- Latest signals -->
          <div v-if="latestSignals.length > 0" class="sd-card">
            <div class="sd-card__header">
              <LucideIcon name="zap" :size="16" />
              <span class="sd-card__title">Tín hiệu gần đây</span>
            </div>
            <div class="sd-card__body">
              <div v-for="sig in latestSignals" :key="sig.signalType + sig.createdAt" class="sd-signal">
                <div class="sd-signal__icon" :style="{ background: sigColor(sig.severity) }">
                  <LucideIcon name="alert-circle" :size="12" :style="{ color: sigColor(sig.severity) }" />
                </div>
                <div class="sd-signal__body">
                  <p class="sd-signal__type">{{ getVLabel(sig.signalType) }}</p>
                  <p class="sd-signal__evidence">{{ sig.evidence || '—' }}</p>
                </div>
                <div class="sd-signal__meta">
                  <span class="sd-signal__conf">{{ Math.round((sig.confidence || 0) * 100) }}%</span>
                  <span class="sd-signal__sev" :class="`sd-signal__sev--${sig.severity?.toLowerCase()}`">{{ sig.severity }}</span>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>

      <!-- Actions -->
      <div class="sd-actions">
        <button class="sd-action sd-action--warn" :disabled="actionLoading === 'warning'" @click="issueWarning">
          <LucideIcon name="alert-triangle" :size="16" />
          <span>{{ actionLoading === 'warning' ? 'Đang gửi...' : 'Gửi cảnh báo' }}</span>
        </button>
        <button class="sd-action sd-action--danger" :disabled="actionLoading === 'invalidate'" @click="suspendExam">
          <LucideIcon name="x-circle" :size="16" />
          <span>{{ actionLoading === 'invalidate' ? 'Đang xử lý...' : 'Tạm dừng thi' }}</span>
        </button>
        <button class="sd-action sd-action--outline" @click="viewFullReport">
          <LucideIcon name="file-text" :size="16" />
          <span>Báo cáo đầy đủ</span>
        </button>
      </div>
    </template>

    <!-- Warning dialog -->
    <Teleport to="body">
      <div v-if="showWarningDialog" class="sd-overlay" @click.self="showWarningDialog = false">
        <div class="sd-dialog">
          <div class="sd-dialog__head">
            <LucideIcon name="alert-triangle" :size="20" />
            <h3 class="sd-dialog__title">Gửi cảnh báo</h3>
          </div>
          <div class="sd-dialog__body">
            <p class="sd-dialog__desc">Gửi cảnh báo tới <strong>{{ studentName }}</strong>.</p>
            <textarea v-model="warningMessage" class="sd-dialog__textarea" rows="3" placeholder="Nhập nội dung cảnh báo (để trống = cảnh báo mặc định)" />
          </div>
          <div class="sd-dialog__foot">
            <button class="sd-btn sd-btn--secondary" @click="showWarningDialog = false">Hủy</button>
            <button class="sd-btn sd-btn--warn" @click="confirmSendWarning">Gửi cảnh báo</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Invalidate dialog -->
    <Teleport to="body">
      <div v-if="showInvalidateDialog" class="sd-overlay" @click.self="showInvalidateDialog = false">
        <div class="sd-dialog">
          <div class="sd-dialog__head sd-dialog__head--danger">
            <LucideIcon name="shield-alert" :size="20" />
            <h3 class="sd-dialog__title">Xác nhận dừng thi</h3>
          </div>
          <div class="sd-dialog__body">
            <p class="sd-dialog__desc sd-dialog__desc--danger">Hành động này sẽ <strong>dừng bài thi</strong>. Không thể hoàn tác.</p>
            <textarea v-model="invalidateReason" class="sd-dialog__textarea" rows="2" placeholder="Lý do dừng thi (để trống = lý do mặc định)" />
          </div>
          <div class="sd-dialog__foot">
            <button class="sd-btn sd-btn--secondary" @click="showInvalidateDialog = false">Hủy</button>
            <button class="sd-btn sd-btn--danger" @click="confirmInvalidate">Xác nhận dừng thi</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useToast } from '../../composables/useToast'
import { useProctorDashboardStore } from '../../stores/proctorDashboardStore'
import {
  fetchAttemptRisk,
  listMonitoringTimeline,
  listMonitoringAudit,
  sendTeacherWarning,
  invalidateAttempt
} from '../../services/monitoringService'
import LucideIcon from '../common/LucideIcon.vue'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const store = useProctorDashboardStore()
const { cards: attempts } = storeToRefs(store)

const attemptId = computed(() => route.query.attemptId || '')
const examId = computed(() => route.query.examId || '')

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

// ── Student info ──────────────────────────────────────────────────────────────
const studentName = computed(() => {
  const s = riskData.value.student
  if (!s) return '—'
  return s.name || s.username || '—'
})
const studentCode = computed(() => {
  const s = riskData.value.student
  if (!s) return '—'
  return s.studentCode || String(s.id || '—')
})
const studentEmail = computed(() => {
  const s = riskData.value.student
  return s?.email || ''
})
const studentInitials = computed(() => {
  const name = studentName.value
  if (!name || name === '—') return '?'
  const parts = name.trim().split(' ')
  const last = parts[parts.length - 1]
  const first = parts[0]
  return (last.charAt(0) + (parts.length > 1 ? first.charAt(0) : '')).toUpperCase()
})

// ── Attempt ───────────────────────────────────────────────────────────────────
const attemptStatusLabel = computed(() => {
  const s = String(riskData.value.status || riskData.value.attempt?.status || '').toUpperCase()
  if (/SUBMITTED/.test(s)) return 'Đã nộp'
  if (/STOPPED/.test(s)) return 'Đã dừng'
  if (/PAUSED/.test(s)) return 'Tạm dừng'
  if (/ACTIVE|IN_PROGRESS/.test(s)) return 'Đang thi'
  return s || '—'
})
const attemptStatusColor = computed(() => {
  const s = String(riskData.value.status || riskData.value.attempt?.status || '').toUpperCase()
  if (/SUBMITTED/.test(s)) return '#4ade80'
  if (/STOPPED/.test(s)) return '#f87171'
  if (/PAUSED/.test(s)) return '#fbbf24'
  if (/ACTIVE|IN_PROGRESS/.test(s)) return '#a5b4fc'
  return '#94a3b8'
})
const attemptStatusBg = computed(() => {
  const s = String(riskData.value.status || riskData.value.attempt?.status || '').toUpperCase()
  if (/SUBMITTED/.test(s)) return 'rgba(74,222,128,0.12)'
  if (/STOPPED/.test(s)) return 'rgba(248,113,113,0.12)'
  if (/PAUSED/.test(s)) return 'rgba(251,191,36,0.12)'
  if (/ACTIVE|IN_PROGRESS/.test(s)) return 'rgba(165,180,252,0.12)'
  return 'rgba(148,163,184,0.08)'
})
const attemptStatusIcon = computed(() => {
  const s = String(riskData.value.status || riskData.value.attempt?.status || '').toUpperCase()
  if (/SUBMITTED/.test(s)) return 'check-circle'
  if (/STOPPED/.test(s)) return 'x-circle'
  if (/PAUSED/.test(s)) return 'pause-circle'
  if (/ACTIVE|IN_PROGRESS/.test(s)) return 'play-circle'
  return 'help-circle'
})
const startTime = computed(() => {
  const ts = riskData.value.attempt?.startedAt
  if (!ts) return '—'
  return new Date(ts).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
})
const sessionTime = computed(() => {
  const ts = riskData.value.attempt?.startedAt
  if (!ts) return '—'
  const diff = Math.floor((Date.now() - new Date(ts).getTime()) / 60000)
  if (diff < 1) return '<1 phút'
  if (diff < 60) return `${diff} phút`
  return `${Math.floor(diff / 60)}h ${diff % 60}p`
})

// ── Stats for session ─────────────────────────────────────────────────────
const onlineCount = computed(() => attempts.value.filter(a => /^(ACTIVE|IN_PROGRESS)$/i.test(a.status || '')).length)
const alertCount = computed(() => attempts.value.filter(a => Number(a.riskScore || 0) > 30).length)
const submittedCount = computed(() => attempts.value.filter(a => /SUBMITTED/i.test(a.status || '')).length)

// ── Risk ──────────────────────────────────────────────────────────────────────
const riskBand = computed(() => {
  if (riskScore.value >= 60) return 'danger'
  if (riskScore.value >= 30) return 'warn'
  return 'clean'
})
const riskScore = computed(() => Number(riskData.value.score ?? 0))
const riskLevelLabel = computed(() => {
  const map = { CRITICAL: 'Nguy cơ cao', HIGH_RISK: 'Rủi ro cao', SUSPICIOUS: 'Đáng ngờ', CLEAN: 'Bình thường' }
  return map[riskData.value.level] || riskData.value.level || '—'
})
const riskDescription = computed(() => {
  const s = riskScore.value
  return s >= 80 ? 'Hành vi gian lận rõ ràng — cần xử lý ngay'
    : s >= 60 ? 'Rủi ro cao — cần giám sát kỹ lưỡng'
    : s >= 30 ? 'Đáng ngờ — có hành vi bất thường'
    : 'Không có dấu hiệu bất thường'
})
const riskColor = computed(() =>
  riskScore.value >= 60 ? '#f87171'
    : riskScore.value >= 30 ? '#fbbf24'
    : '#4ade80'
)
const riskBg = computed(() =>
  riskScore.value >= 60 ? 'rgba(248,113,113,0.15)'
    : riskScore.value >= 30 ? 'rgba(251,191,36,0.15)'
    : 'rgba(74,222,128,0.15)'
)
const avatarBg = computed(() => riskBg.value)
const recommendedActionLabel = computed(() => {
  const map = {
    PAUSE_AND_REVIEW: 'Tạm dừng và kiểm tra ngay',
    WARN_AND_ESCALATE: 'Cảnh báo và tăng cường giám sát',
    REVIEW_ATTEMPT: 'Mở hồ sơ để review thủ công',
    CONTINUE_MONITORING: 'Tiếp tục giám sát'
  }
  return map[riskData.value.recommendedAction] || 'Tiếp tục giám sát'
})
const hasBreakdown = computed(() =>
  riskData.value.breakdown && Object.keys(riskData.value.breakdown).length > 0
)

// ── Gauge SVG ────────────────────────────────────────────────────────────────
const gaugeArc = computed(() => {
  const circ = 2 * Math.PI * 52
  return `${(riskScore.value / 100) * circ} ${circ}`
})

// ── Timeline events ───────────────────────────────────────────────────────────
const timelineEvents = computed(() => {
  const evts = []
  const seen = new Set()
  for (const item of timeline.value) {
    const key = `${item.eventType}-${item.at}`
    if (seen.has(key)) continue
    seen.add(key)
    evts.push({
      key,
      at: item.at,
      eventType: item.eventType || item.signalType || '',
      details: item.details || item.evidence || '',
      severity: item.severity || mapSeverity(item.eventType, item.confidence)
    })
  }
  return evts
})

const violationCount = computed(() => timelineEvents.value.length)

const tabSwitchCount = computed(() =>
  timelineEvents.value.filter(e => /TAB_SWITCH/i.test(e.eventType || '')).length
)

const maxSeverity = computed(() => {
  const counts = { HIGH: 0, MEDIUM: 0, LOW: 0 }
  timelineEvents.value.forEach(e => { if (e.severity) counts[e.severity] = (counts[e.severity] || 0) + 1 })
  return counts.HIGH > 0 ? 'Nghiêm trọng' : counts.MEDIUM > 0 ? 'Trung bình' : counts.LOW > 0 ? 'Thấp' : '—'
})

// ── Latest signals ───────────────────────────────────────────────────────────
const latestSignals = computed(() => riskData.value.latestSignals || [])

// ── Suspicious patterns ────────────────────────────────────────────────────────
const suspiciousPatterns = computed(() => {
  const patterns = []
  const tab = tabSwitchCount.value
  if (tab > 3) patterns.push({ id: 'tab', title: 'Chuyển tab nhiều lần', description: `${tab} lần chuyển tab, vượt ngưỡng bình thường (3 lần)`, level: tab > 5 ? 'high' : 'medium' })
  const copy = timelineEvents.value.filter(e => /COPY/i.test(e.eventType || '')).length
  if (copy > 0) patterns.push({ id: 'copy', title: 'Cố gắng copy nội dung', description: `${copy} lần sao chép dữ liệu từ đề thi`, level: 'medium' })
  const fs = timelineEvents.value.filter(e => /EXIT_FULLSCREEN/i.test(e.eventType || '')).length
  if (fs > 0) patterns.push({ id: 'fs', title: 'Thoát chế độ toàn màn hình', description: `${fs} lần thoát toàn màn hình`, level: fs > 2 ? 'high' : 'medium' })
  const dev = timelineEvents.value.filter(e => /DEVTOOLS/i.test(e.eventType || '')).length
  if (dev > 0) patterns.push({ id: 'dev', title: 'Mở công cụ phát triển', description: `${dev} lần mở DevTools`, level: 'high' })
  const dup = timelineEvents.value.filter(e => /DUPLICATE_IP/i.test(e.eventType || '')).length
  if (dup > 0) patterns.push({ id: 'dup', title: 'Phát hiện IP trùng lặp', description: `${dup} thiết bị khác cùng IP đang thi`, level: 'high' })
  const sync = timelineEvents.value.filter(e => /SYNC/i.test(e.eventType || '')).length
  if (sync > 0) patterns.push({ id: 'sync', title: 'Hành vi đồng bộ', description: `${sync} tín hiệu nhiều thí sinh thao tác đồng thời`, level: 'high' })
  return patterns
})

function pColor(l) { return l === 'high' ? '#f87171' : l === 'medium' ? '#fbbf24' : '#38bdf8' }
function pBg(l) { return l === 'high' ? 'rgba(248,113,113,0.08)' : l === 'medium' ? 'rgba(251,191,36,0.08)' : 'rgba(56,189,248,0.08)' }
function pBorder(l) { return l === 'high' ? 'rgba(248,113,113,0.2)' : l === 'medium' ? 'rgba(251,191,36,0.2)' : 'rgba(56,189,248,0.2)' }
function sColor(score) { return score >= 60 ? '#f87171' : score >= 30 ? '#fbbf24' : '#38bdf8' }
function sigColor(sev) {
  return sev === 'HIGH' ? 'rgba(248,113,113,0.15)' : sev === 'MEDIUM' ? 'rgba(251,191,36,0.15)' : 'rgba(56,189,248,0.15)'
}

// ── Helpers ──────────────────────────────────────────────────────────────────
function mapSeverity(type, confidence) {
  const map = {
    TAB_SWITCH: 'LOW', WINDOW_BLUR: 'LOW', IDLE_TIME: 'LOW', RIGHT_CLICK: 'LOW',
    EXIT_FULLSCREEN: 'MEDIUM', COPY_PASTE: 'MEDIUM', RAPID_QUESTION_SWITCH: 'MEDIUM',
    DUPLICATE_IP: 'MEDIUM', HEARTBEAT_STALE: 'MEDIUM',
    DEVTOOLS_OPEN: 'HIGH', PRINT_SCREEN: 'HIGH', MULTI_MONITOR: 'HIGH',
    DEVICE_FINGERPRINT_CHANGED: 'HIGH', SYNC_BEHAVIOR: 'HIGH',
    IP_FINGERPRINT_GRAPH: 'HIGH', ANSWER_SIMILARITY: 'HIGH',
    AI_MULTIPLE_FACES: 'HIGH', AI_PHONE_DETECTED: 'HIGH'
  }
  return map[type] || (confidence >= 0.8 ? 'HIGH' : confidence >= 0.5 ? 'MEDIUM' : 'LOW')
}

function getVColor(type) {
  const map = {
    TAB_SWITCH: '#fbbf24', COPY_PASTE: '#f87171',
    DEVTOOLS_OPEN: '#f87171', EXIT_FULLSCREEN: '#fbbf24',
    MULTI_MONITOR: '#f87171', DUPLICATE_IP: '#f87171',
    PRINT_SCREEN: '#f87171', WINDOW_BLUR: '#fbbf24',
    IDLE_TIME: '#38bdf8', RIGHT_CLICK: '#38bdf8',
    HEARTBEAT_STALE: '#fbbf24', RAPID_QUESTION_SWITCH: '#fbbf24',
    DEVICE_FINGERPRINT_CHANGED: '#f87171', SYNC_BEHAVIOR: '#f87171',
    IP_FINGERPRINT_GRAPH: '#f87171', ANSWER_SIMILARITY: '#f87171',
    AI_MULTIPLE_FACES: '#f87171', AI_PHONE_DETECTED: '#f87171',
    AI_LOOKING_AWAY: '#fbbf24'
  }
  return map[type] || '#94a3b8'
}

function getVIcon(type) {
  const map = {
    TAB_SWITCH: 'layers', WINDOW_BLUR: 'layers', IDLE_TIME: 'clock',
    RIGHT_CLICK: 'mouse-pointer-2', EXIT_FULLSCREEN: 'minimize', COPY_PASTE: 'copy',
    DEVTOOLS_OPEN: 'code', PRINT_SCREEN: 'code', MULTI_MONITOR: 'monitor',
    DUPLICATE_IP: 'globe', RAPID_QUESTION_SWITCH: 'monitor', HEARTBEAT_STALE: 'wifi-off',
    DEVICE_FINGERPRINT_CHANGED: 'code', SYNC_BEHAVIOR: 'monitor',
    IP_FINGERPRINT_GRAPH: 'globe', ANSWER_SIMILARITY: 'copy',
    AI_MULTIPLE_FACES: 'monitor', AI_PHONE_DETECTED: 'monitor',
    AI_LOOKING_AWAY: 'wifi-off'
  }
  return map[type] || 'alert-circle'
}

function getVLabel(type) {
  const map = {
    TAB_SWITCH: 'Chuyển tab', WINDOW_BLUR: 'Mất tiêu điểm cửa sổ',
    IDLE_TIME: 'Không hoạt động', RIGHT_CLICK: 'Click chuột phải',
    EXIT_FULLSCREEN: 'Thoát toàn màn hình', COPY_PASTE: 'Sao chép nội dung',
    DEVTOOLS_OPEN: 'Mở DevTools', PRINT_SCREEN: 'Chụp màn hình',
    MULTI_MONITOR: 'Nhiều màn hình', DUPLICATE_IP: 'IP trùng lặp',
    RAPID_QUESTION_SWITCH: 'Chuyển câu nhanh', HEARTBEAT_STALE: 'Mất kết nối',
    DEVICE_FINGERPRINT_CHANGED: 'Thay đổi thiết bị',
    SYNC_BEHAVIOR: 'Hành vi đồng bộ', IP_FINGERPRINT_GRAPH: 'Liên kết IP/fingerprint',
    ANSWER_SIMILARITY: 'Tương đồng đáp án', AI_MULTIPLE_FACES: 'Nhiều khuôn mặt',
    AI_PHONE_DETECTED: 'Phát hiện điện thoại', AI_LOOKING_AWAY: 'Nhìn lệch hướng'
  }
  return map[type] || type || '—'
}

function getSeverityLabel(s) {
  const map = { HIGH: 'Nghiêm trọng', MEDIUM: 'Trung bình', LOW: 'Thấp', CRITICAL: 'Nghiêm trọng' }
  return map[s] || s || '—'
}
function getSeverityStatus(s) {
  return s === 'HIGH' || s === 'CRITICAL' ? 'high'
    : s === 'MEDIUM' ? 'medium'
    : 'low'
}

function formatTime(ts) {
  if (!ts) return '—'
  return new Date(ts).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

// ── Actions ───────────────────────────────────────────────────────────────────
async function loadData() {
  if (!attemptId.value) { error.value = 'Không có thông tin attempt'; return }
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

function goBack() { router.back() }
function issueWarning() { showWarningDialog.value = true }
function suspendExam() { showInvalidateDialog.value = true }

async function confirmSendWarning() {
  actionLoading.value = 'warning'
  showWarningDialog.value = false
  try {
    await sendTeacherWarning(attemptId.value, warningMessage.value)
    toast.success('Đã gửi cảnh báo tới học sinh.')
    warningMessage.value = ''
  } catch { toast.error('Gửi cảnh báo thất bại.') }
  finally { actionLoading.value = '' }
}

async function confirmInvalidate() {
  actionLoading.value = 'invalidate'
  showInvalidateDialog.value = false
  try {
    await invalidateAttempt(attemptId.value, invalidateReason.value)
    invalidateReason.value = ''
    await loadData()
    toast.success('Đã dừng bài thi.')
  } catch { toast.error('Dừng thi thất bại.') }
  finally { actionLoading.value = '' }
}

function viewFullReport() {
  router.push({ path: '/teacher/exams/review/incidents', query: { examId: examId.value } })
}

onMounted(loadData)
</script>

<style scoped>
/* ── Root ─────────────────────────────────────────────────────────────── */
.sd-root { min-height: 100vh; background: #0f172a; scroll-behavior: smooth; }

/* ── Loading ──────────────────────────────────────────────────────────── */
.sd-loading { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 0.875rem; height: 60vh; }
.sd-loading__spinner { width: 32px; height: 32px; border: 3px solid rgba(255,255,255,0.08); border-top-color: #6366f1; border-radius: 50%; animation: sd-spin 0.7s linear infinite; }
.sd-loading__text { color: rgba(255,255,255,0.4); font-size: 0.875rem; }
@keyframes sd-spin { to { transform: rotate(360deg); } }

/* ── Error ──────────────────────────────────────────────────────────── */
.sd-error { display: flex; align-items: center; justify-content: center; height: 60vh; }
.sd-error__card { display: flex; flex-direction: column; align-items: center; gap: 0.75rem; padding: 2.5rem; border: 1px solid rgba(248,113,113,0.3); border-radius: var(--ds-radius-xl); background: rgba(248,113,113,0.06); max-width: 360px; text-align: center; }
.sd-error__icon { color: #f87171; }
.sd-error__msg { color: #f87171; font-size: 0.875rem; font-weight: 600; }

/* ── Top bar ────────────────────────────────────────────────────────── */
.sd-topbar { display: flex; align-items: center; justify-content: space-between; padding: 0.625rem 1.5rem; border-bottom: 1px solid rgba(255,255,255,0.07); background: rgba(30,41,59,0.9); backdrop-filter: blur(12px); position: sticky; top: 0; z-index: 50; gap: 1rem; }
.sd-topbar__left { display: flex; align-items: center; gap: 0.75rem; min-width: 0; }
.sd-topbar__back { display: flex; align-items: center; gap: 0.375rem; padding: 0.3rem 0.75rem; border-radius: var(--ds-radius); border: 1px solid rgba(255,255,255,0.1); background: rgba(255,255,255,0.05); color: rgba(255,255,255,0.7); font-size: 0.775rem; font-weight: 600; cursor: pointer; transition: all 0.15s; white-space: nowrap; flex-shrink: 0; }
.sd-topbar__back:hover { background: rgba(255,255,255,0.1); color: white; }
.sd-topbar__sep { width: 1px; height: 20px; background: rgba(255,255,255,0.1); flex-shrink: 0; }
.sd-topbar__crumbs { display: flex; align-items: center; gap: 0.375rem; font-size: 0.775rem; min-width: 0; overflow: hidden; }
.sd-topbar__crumb { color: rgba(255,255,255,0.4); text-decoration: none; white-space: nowrap; transition: color 0.15s; }
.sd-topbar__crumb:hover { color: #a5b4fc; }
.sd-topbar__crumb-sep { color: rgba(255,255,255,0.2); flex-shrink: 0; }
.sd-topbar__crumb--active { color: rgba(255,255,255,0.9); font-weight: 600; overflow: hidden; text-overflow: ellipsis; }
.sd-topbar__id { display: flex; align-items: center; gap: 0.3rem; font-size: 0.7rem; font-weight: 700; padding: 0.2rem 0.625rem; border-radius: 9999px; background: rgba(165,180,252,0.12); color: #a5b4fc; white-space: nowrap; flex-shrink: 0; }

/* ── Hero ──────────────────────────────────────────────────────────── */
.sd-hero { display: flex; align-items: center; justify-content: space-between; gap: 1.5rem; padding: 1.25rem 1.5rem; background: rgba(30,41,59,0.7); backdrop-filter: blur(12px); border-radius: var(--ds-radius-xl); flex-wrap: wrap; margin: 1.25rem 1.5rem 0; border: 1px solid rgba(255,255,255,0.07); }
.sd-hero--danger { border-top: 3px solid rgba(248,113,113,0.6); }
.sd-hero--warn { border-top: 3px solid rgba(251,191,36,0.6); }
.sd-hero--clean { border-top: 3px solid rgba(74,222,128,0.4); }

.sd-hero__left { display: flex; align-items: center; gap: 1rem; min-width: 0; flex: 1; }
.sd-hero__avatar-wrap { flex-shrink: 0; }
.sd-hero__avatar { position: relative; width: 52px; height: 52px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.sd-hero__initials { font-size: 1.125rem; font-weight: 900; }
.sd-hero__status-ring { position: absolute; inset: -2px; border-radius: 50%; border: 2px solid; }
.sd-hero__info { min-width: 0; }
.sd-hero__name-row { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.25rem; }
.sd-hero__name { font-size: 1rem; font-weight: 900; color: white; margin: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sd-hero__status-badge { display: inline-flex; align-items: center; gap: 0.25rem; padding: 0.15rem 0.5rem; border-radius: 9999px; font-size: 0.65rem; font-weight: 700; white-space: nowrap; }
.sd-hero__meta { font-size: 0.75rem; color: rgba(255,255,255,0.4); margin: 0 0 0.5rem; display: flex; align-items: center; gap: 0.375rem; flex-wrap: wrap; }
.sd-hero__meta-sep { opacity: 0.4; }
.sd-hero__badges { display: flex; gap: 0.375rem; flex-wrap: wrap; }
.sd-hero__right { display: flex; align-items: center; gap: 1rem; flex-shrink: 0; }

/* Gauge */
.sd-gauge { position: relative; width: 96px; height: 96px; flex-shrink: 0; }
.sd-gauge__svg { transform: rotate(-90deg); }
.sd-gauge__track { fill: none; stroke: rgba(255,255,255,0.08); stroke-width: 8; }
.sd-gauge__fill { fill: none; stroke-width: 8; stroke-linecap: round; transition: stroke-dasharray 0.6s ease; will-change: stroke-dasharray; }
.sd-gauge__center { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; }
.sd-gauge__score { font-size: 1.5rem; font-weight: 900; line-height: 1; }
.sd-gauge__label { font-size: 0.55rem; color: rgba(255,255,255,0.4); font-weight: 600; margin-top: 1px; }
.sd-hero__gauge-desc { min-width: 180px; }
.sd-gauge-desc__text { font-size: 0.8rem; color: rgba(255,255,255,0.5); margin: 0 0 0.5rem; }
.sd-gauge-desc__track { height: 5px; border-radius: 9999px; background: rgba(255,255,255,0.08); overflow: hidden; }
.sd-gauge-desc__fill { height: 100%; border-radius: 9999px; transition: width 0.6s ease; will-change: width; }

/* ── Stats ──────────────────────────────────────────────────────── */
.sd-stats { display: grid; grid-template-columns: repeat(5, 1fr); gap: 0.75rem; margin: 1.25rem 1.5rem 0; }
.sd-stat { display: flex; align-items: center; gap: 0.75rem; padding: 0.875rem 1rem; background: rgba(30,41,59,0.6); backdrop-filter: blur(8px); border: 1px solid rgba(255,255,255,0.07); border-radius: var(--ds-radius-lg); transition: border-color 0.15s; }
.sd-stat:hover { border-color: rgba(99,102,241,0.3); }
.sd-stat__icon-wrap { width: 34px; height: 34px; border-radius: var(--ds-radius); display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.06); flex-shrink: 0; }
.sd-stat__body { display: flex; flex-direction: column; min-width: 0; }
.sd-stat__value { font-size: 1.25rem; font-weight: 900; color: white; font-variant-numeric: tabular-nums; line-height: 1.2; }
.sd-stat__label { font-size: 0.65rem; color: rgba(255,255,255,0.4); margin-top: 2px; font-weight: 600; }

/* ── Grid ──────────────────────────────────────────────────────── */
.sd-grid { display: grid; grid-template-columns: 1fr 340px; gap: 1.25rem; margin: 1.25rem 1.5rem 0; align-items: start; }
@media (max-width: 1024px) { .sd-grid { grid-template-columns: 1fr; } .sd-stats { grid-template-columns: repeat(3, 1fr); } .sd-hero { flex-direction: column; align-items: flex-start; } }

/* ── Cards ─────────────────────────────────────────────────────── */
.sd-card { background: rgba(30,41,59,0.6); backdrop-filter: blur(8px); border: 1px solid rgba(255,255,255,0.07); border-radius: var(--ds-radius-xl); overflow: hidden; }
.sd-card__header { display: flex; align-items: center; gap: 0.5rem; padding: 0.875rem 1.25rem; border-bottom: 1px solid rgba(255,255,255,0.07); color: rgba(255,255,255,0.4); }
.sd-card__title { font-size: 0.8rem; font-weight: 800; color: white; flex: 1; }
.sd-card__badge { font-size: 0.68rem; font-weight: 800; padding: 0.15rem 0.5rem; border-radius: 9999px; background: rgba(165,180,252,0.15); color: #a5b4fc; }
.sd-card__badge--danger { background: rgba(248,113,113,0.15); color: #f87171; }
.sd-card__body { padding: 1.25rem; }
.sd-card__body--flush { padding: 0; }

/* ── Recommendation ──────────────────────────────────────────────── */
.sd-rec__action { margin-bottom: 0.875rem; }
.sd-rec__badge { display: inline-flex; padding: 0.3rem 0.75rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 700; }
.sd-rec__label { font-size: 0.68rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; color: rgba(255,255,255,0.35); margin: 0 0 0.5rem; }
.sd-rec__chips { display: flex; gap: 0.375rem; flex-wrap: wrap; margin-bottom: 0.875rem; }
.sd-chip { display: inline-flex; padding: 0.25rem 0.625rem; border-radius: 9999px; font-size: 0.7rem; font-weight: 600; }
.sd-rec__list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.375rem; }
.sd-rec__item { display: flex; align-items: flex-start; gap: 0.5rem; font-size: 0.8rem; color: rgba(255,255,255,0.6); }
.sd-rec__item-dot { color: rgba(255,255,255,0.3); margin-top: 4px; flex-shrink: 0; }

/* ── Timeline ──────────────────────────────────────────────────────── */
.sd-timeline { display: flex; flex-direction: column; }
.sd-tl-item { display: flex; align-items: flex-start; gap: 0.75rem; padding: 0.875rem 1.25rem; border-bottom: 1px solid rgba(255,255,255,0.04); transition: background 0.15s; }
.sd-tl-item:last-child { border-bottom: none; }
.sd-tl-item:hover { background: rgba(255,255,255,0.02); }
.sd-tl-item__dot { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.sd-tl-item__content { flex: 1; min-width: 0; }
.sd-tl-item__header { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; margin-bottom: 0.25rem; }
.sd-tl-item__type { font-size: 0.8rem; font-weight: 700; }
.sd-tl-item__time { font-size: 0.72rem; color: rgba(255,255,255,0.3); font-variant-numeric: tabular-nums; white-space: nowrap; flex-shrink: 0; }
.sd-tl-item__details { font-size: 0.75rem; color: rgba(255,255,255,0.4); margin: 0 0 0.3rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sd-tl-item__severity { display: inline-flex; padding: 0.15rem 0.5rem; border-radius: 9999px; font-size: 0.65rem; font-weight: 700; }
.sd-tl-item__severity--high { background: rgba(248,113,113,0.15); color: #f87171; }
.sd-tl-item__severity--medium { background: rgba(251,191,36,0.15); color: #fbbf24; }
.sd-tl-item__severity--low { background: rgba(56,189,248,0.15); color: #38bdf8; }

/* ── Breakdown ──────────────────────────────────────────────────── */
.sd-breakdown { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.75rem; }
.sd-breakdown:last-child { margin-bottom: 0; }
.sd-breakdown__label { font-size: 0.78rem; color: rgba(255,255,255,0.7); min-width: 130px; max-width: 130px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sd-breakdown__bar { flex: 1; height: 6px; border-radius: 9999px; background: rgba(255,255,255,0.08); overflow: hidden; }
.sd-breakdown__fill { height: 100%; border-radius: 9999px; transition: width 0.5s ease; will-change: width; }
.sd-breakdown__score { font-size: 0.78rem; font-weight: 800; min-width: 24px; text-align: right; font-variant-numeric: tabular-nums; }

/* ── Pattern ────────────────────────────────────────────────────── */
.sd-pattern { padding: 0.75rem 1rem; border: 1px solid; border-radius: var(--ds-radius-lg); margin-bottom: 0.625rem; }
.sd-pattern:last-child { margin-bottom: 0; }
.sd-pattern__head { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.25rem; }
.sd-pattern__title { font-size: 0.8rem; font-weight: 700; color: white; flex: 1; }
.sd-pattern__level { font-size: 0.6rem; font-weight: 800; text-transform: uppercase; letter-spacing: 0.06em; padding: 0.15rem 0.4rem; border-radius: 9999px; color: white; }
.sd-pattern__desc { font-size: 0.73rem; color: rgba(255,255,255,0.5); padding-left: 1.5rem; margin: 0; }

/* ── Signal ─────────────────────────────────────────────────────── */
.sd-signal { display: flex; align-items: flex-start; gap: 0.625rem; margin-bottom: 0.75rem; padding-bottom: 0.75rem; border-bottom: 1px solid rgba(255,255,255,0.05); }
.sd-signal:last-child { margin-bottom: 0; padding-bottom: 0; border-bottom: none; }
.sd-signal__icon { width: 28px; height: 28px; border-radius: var(--ds-radius); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.sd-signal__body { flex: 1; min-width: 0; }
.sd-signal__type { font-size: 0.775rem; font-weight: 700; color: white; margin: 0 0 0.15rem; }
.sd-signal__evidence { font-size: 0.72rem; color: rgba(255,255,255,0.4); margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sd-signal__meta { display: flex; flex-direction: column; align-items: flex-end; gap: 0.25rem; flex-shrink: 0; }
.sd-signal__conf { font-size: 0.78rem; font-weight: 800; color: white; font-variant-numeric: tabular-nums; }
.sd-signal__sev { font-size: 0.6rem; font-weight: 800; padding: 0.1rem 0.4rem; border-radius: 9999px; }
.sd-signal__sev--high { background: rgba(248,113,113,0.15); color: #f87171; }
.sd-signal__sev--medium { background: rgba(251,191,36,0.15); color: #fbbf24; }
.sd-signal__sev--low { background: rgba(56,189,248,0.15); color: #38bdf8; }

/* ── Empty ─────────────────────────────────────────────────────── */
.sd-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 0.5rem; padding: 3rem; color: rgba(255,255,255,0.3); font-size: 0.85rem; }
.sd-empty__icon { color: #4ade80; }

/* ── Actions ────────────────────────────────────────────────────── */
.sd-actions { display: flex; align-items: center; justify-content: flex-end; gap: 0.75rem; flex-wrap: wrap; margin: 1.5rem 1.5rem 2rem; }
.sd-action { display: inline-flex; align-items: center; gap: 0.5rem; padding: 0.625rem 1.25rem; border-radius: var(--ds-radius); font-size: 0.825rem; font-weight: 700; cursor: pointer; border: 1px solid transparent; transition: all 0.15s; }
.sd-action:disabled { opacity: 0.5; cursor: not-allowed; }
.sd-action--warn { background: rgba(251,191,36,0.12); color: #fbbf24; border-color: rgba(251,191,36,0.25); }
.sd-action--warn:hover:not(:disabled) { background: rgba(251,191,36,0.2); }
.sd-action--danger { background: rgba(248,113,113,0.12); color: #f87171; border-color: rgba(248,113,113,0.25); }
.sd-action--danger:hover:not(:disabled) { background: rgba(248,113,113,0.2); }
.sd-action--outline { background: rgba(255,255,255,0.05); color: rgba(255,255,255,0.7); border-color: rgba(255,255,255,0.1); }
.sd-action--outline:hover:not(:disabled) { background: rgba(255,255,255,0.08); color: white; border-color: rgba(255,255,255,0.2); }

/* ── Dialog ─────────────────────────────────────────────────────── */
.sd-overlay { position: fixed; inset: 0; z-index: 200; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; backdrop-filter: blur(4px); }
.sd-dialog { width: 100%; max-width: 440px; margin: 1rem; background: #1e293b; border: 1px solid rgba(255,255,255,0.1); border-radius: var(--ds-radius-xl); box-shadow: 0 24px 56px rgba(0,0,0,0.5); overflow: hidden; }
.sd-dialog__head { display: flex; align-items: center; gap: 0.625rem; padding: 1.25rem 1.5rem 1rem; border-bottom: 1px solid rgba(255,255,255,0.07); color: #fbbf24; }
.sd-dialog__head--danger { color: #f87171; }
.sd-dialog__title { font-size: 1rem; font-weight: 800; color: white; margin: 0; }
.sd-dialog__body { padding: 1.25rem 1.5rem; }
.sd-dialog__desc { font-size: 0.85rem; color: rgba(255,255,255,0.6); margin: 0 0 0.875rem; }
.sd-dialog__desc--danger { color: #f87171; }
.sd-dialog__textarea { width: 100%; padding: 0.625rem 0.875rem; border: 1px solid rgba(255,255,255,0.1); border-radius: var(--ds-radius); background: rgba(255,255,255,0.04); color: white; font-size: 0.825rem; resize: vertical; outline: none; transition: border-color 0.15s; font-family: inherit; }
.sd-dialog__textarea:focus { border-color: rgba(99,102,241,0.5); }
.sd-dialog__foot { display: flex; align-items: center; justify-content: flex-end; gap: 0.625rem; padding: 1rem 1.5rem; border-top: 1px solid rgba(255,255,255,0.07); }

/* ── Buttons ────────────────────────────────────────────────────── */
.sd-btn { display: inline-flex; align-items: center; justify-content: center; gap: 0.375rem; padding: 0.5rem 1rem; border-radius: var(--ds-radius); font-size: 0.825rem; font-weight: 700; cursor: pointer; border: 1px solid transparent; transition: all 0.15s; }
.sd-btn--secondary { background: rgba(255,255,255,0.05); color: rgba(255,255,255,0.7); border-color: rgba(255,255,255,0.1); }
.sd-btn--secondary:hover { background: rgba(255,255,255,0.08); color: white; }
.sd-btn--warn { background: #d97706; color: white; }
.sd-btn--warn:hover { filter: brightness(1.08); }
.sd-btn--danger { background: #dc2626; color: white; }
.sd-btn--danger:hover { filter: brightness(1.08); }

/* ── Badges ─────────────────────────────────────────────────────── */
.sd-badge { display: inline-flex; align-items: center; gap: 0.3rem; padding: 0.2rem 0.6rem; border-radius: 9999px; border: 1px solid transparent; font-size: 0.7rem; font-weight: 700; }
.sd-badge--warn { background: rgba(251,191,36,0.12); color: #fbbf24; border-color: rgba(251,191,36,0.2); }
.sd-badge--neutral { background: rgba(255,255,255,0.05); color: rgba(255,255,255,0.4); border-color: rgba(255,255,255,0.1); }
</style>
