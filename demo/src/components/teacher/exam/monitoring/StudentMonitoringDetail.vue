<template>
  <main class="sd-page">
    <section
      v-if="loading"
      class="sd-state"
    >
      <div class="sd-spinner" />
      <p>Đang tải hồ sơ thí sinh...</p>
    </section>

    <section
      v-else-if="error"
      class="sd-state sd-state--error"
    >
      <LucideIcon
        name="alert-circle"
        :size="34"
      />
      <h2>Không thể tải dữ liệu</h2>
      <p>{{ error }}</p>
      <button
        class="sd-btn"
        type="button"
        @click="loadData"
      >
        Thử lại
      </button>
    </section>

    <template v-else>
      <header class="sd-header">
        <RouterLink
          :to="backLink"
          class="sd-back"
          aria-label="Quay lại phòng giám sát"
        >
          <LucideIcon
            name="arrow-left"
            :size="18"
          />
        </RouterLink>

        <div
          class="sd-avatar"
          :style="{ background: riskBg, color: riskColor }"
        >
          {{ studentInitials }}
        </div>

        <div class="sd-title">
          <span>Hồ sơ thí sinh</span>
          <h1>{{ studentName }}</h1>
          <p>{{ [studentCode, studentEmail].filter(Boolean).join(' · ') || 'Chưa có thông tin liên hệ' }}</p>
        </div>

        <div class="sd-status">
          <strong :style="{ color: riskColor }">{{ riskScore }}</strong>
          <span>{{ riskBandLabel }} · {{ statusLabel }}</span>
        </div>

        <button
          class="sd-btn"
          type="button"
          @click="loadData"
        >
          <LucideIcon
            name="refresh-cw"
            :size="15"
          />
          Làm mới
        </button>
      </header>

      <section
        class="sd-strip"
        aria-label="Thông tin nhanh"
      >
        <span>Thời gian: <strong>{{ sessionTime }}</strong></span>
        <span>Tiến độ: <strong>{{ attemptData.answeredCount || 0 }}/{{ attemptData.totalQuestions || 0 }} câu</strong></span>
        <span>Camera: <strong>{{ attemptData.cameraOn ? 'Bật' : 'Tắt' }}</strong></span>
        <span>Micro: <strong>{{ attemptData.micOn ? 'Bật' : 'Tắt' }}</strong></span>
        <span v-if="riskData.activeFlagId">Flag: <strong>{{ activeFlagStatusLabel }}</strong></span>
      </section>

      <section class="sd-actions">
        <button
          class="sd-btn sd-btn--warn"
          type="button"
          @click="showWarnDialog = true"
        >
          <LucideIcon
            name="send"
            :size="15"
          />
          Gửi cảnh báo
        </button>
        <button
          v-if="isStudentPaused"
          class="sd-btn sd-btn--success"
          type="button"
          :disabled="actionLoading === 'resume'"
          @click="showResumeDialog = true"
        >
          <LucideIcon
            name="play"
            :size="15"
          />
          Cho phép tiếp tục
        </button>
        <button
          v-else
          class="sd-btn sd-btn--primary"
          type="button"
          :disabled="isStudentTerminal || actionLoading === 'pause'"
          @click="showPauseDialog = true"
        >
          <LucideIcon
            name="pause"
            :size="15"
          />
          Tạm dừng bài thi
        </button>
        <button
          class="sd-btn sd-btn--danger"
          type="button"
          :disabled="isStudentTerminal || actionLoading === 'invalidate'"
          @click="showStopDialog = true"
        >
          <LucideIcon
            name="stop-circle"
            :size="15"
          />
          Buộc nộp bài
        </button>
      </section>

      <section class="sd-content">
        <article class="sd-panel">
          <div class="sd-panel__head">
            <h2>Phát hiện gian lận</h2>
            <select
              v-model="timelineFilter"
              class="sd-select"
              aria-label="Lọc dòng thời gian"
            >
              <option value="">
                Tất cả
              </option>
              <option value="TAB_SWITCH">
                Chuyển tab
              </option>
              <option value="WINDOW_BLUR">
                Mất tiêu điểm
              </option>
              <option value="FULLSCREEN_VIOLATION">
                Thoát toàn màn hình
              </option>
              <option value="CLIPBOARD_ABUSE">
                Clipboard
              </option>
              <option value="IP_ANOMALY">
                IP / thiết bị
              </option>
              <option value="WARNING_SENT">
                Cảnh báo
              </option>
              <option value="NOTE">
                Ghi chú
              </option>
            </select>
          </div>

          <div v-if="riskData.componentScores || riskData.explanations?.length" class="sd-risk-card">
            <div v-if="riskData.componentScores" class="sd-risk-list">
              <div><span>Màn hình</span><strong>{{ riskData.componentScores.screenLeaveScore || 0 }}</strong></div>
              <div><span>Clipboard</span><strong>{{ riskData.componentScores.clipboardScore || 0 }}</strong></div>
              <div><span>Kỹ thuật</span><strong>{{ riskData.componentScores.technicalScore || 0 }}</strong></div>
              <div><span>Định danh</span><strong>{{ riskData.componentScores.identityScore || 0 }}</strong></div>
              <div><span>Khuyến nghị</span><strong>{{ shortRecommendedAction }}</strong></div>
            </div>
            <div v-if="riskData.explanations?.length" class="sd-reasons">
              <article v-for="item in riskData.explanations.slice(0, 4)" :key="`${item.category}-${item.scoreImpact}`" class="sd-reason-item">
                <strong>{{ item.label || item.category }}</strong>
                <p>{{ item.message }}</p>
              </article>
            </div>
          </div>

          <div v-if="evidenceItems.length" class="sd-evidence">
            <div class="sd-subhead">
              <h3>Chứng cứ nổi bật</h3>
            </div>
            <ul>
              <li
                v-for="item in evidenceItems"
                :key="item"
              >
                {{ item }}
              </li>
            </ul>
          </div>

          <div
            v-if="paginatedTimeline.length === 0"
            class="sd-empty"
          >
            Chưa có phát hiện nào
          </div>
          <div
            v-else
            class="sd-timeline"
          >
            <article
              v-for="event in paginatedTimeline"
              :key="event.key || event.id || event.at"
              class="sd-event"
            >
              <span
                class="sd-event__dot"
                :style="{ background: getVColor(event.eventType) }"
              />
              <div>
                <strong>{{ getVLabel(event.eventType) }}</strong>
                <p v-if="event.details">
                  {{ event.details }}
                </p>
              </div>
              <time>{{ formatTime(event.at || event.timestamp) }}</time>
            </article>
          </div>
          <div
            v-if="timelinePageCount > 1"
            class="sd-pagination"
          >
            <button
              class="sd-btn"
              type="button"
              :disabled="timelinePage === 1"
              @click="timelinePage--"
            >
              <LucideIcon
                name="chevron-left"
                :size="14"
              />
              Trước
            </button>
            <span>{{ timelinePage }}/{{ timelinePageCount }}</span>
            <button
              class="sd-btn"
              type="button"
              :disabled="timelinePage === timelinePageCount"
              @click="timelinePage++"
            >
              Sau
              <LucideIcon
                name="chevron-right"
                :size="14"
              />
            </button>
          </div>
        </article>

        <aside class="sd-panel">
          <div class="sd-panel__head">
            <h2>Review flag</h2>
            <span class="sd-count">{{ activeFlagStatusLabel }}</span>
          </div>
          <div v-if="riskData.activeFlagId" class="sd-flag-box">
            <p class="sd-dialog-text">{{ riskData.recommendedAction || 'Theo dõi và review thủ công' }}</p>
            <textarea
              v-model="flagReviewNote"
              class="sd-note"
              rows="3"
              placeholder="Ghi chú review..."
            />
            <div class="sd-flag-actions">
              <button
                class="sd-btn sd-btn--success"
                type="button"
                :disabled="flagActionLoading === 'CONFIRMED'"
                @click="reviewFlag('CONFIRMED')"
              >
                Xác nhận
              </button>
              <button
                class="sd-btn"
                type="button"
                :disabled="flagActionLoading === 'DISMISSED'"
                @click="reviewFlag('DISMISSED')"
              >
                Bỏ qua
              </button>
            </div>
          </div>
          <div v-else class="sd-empty">
            Chưa có flag mở
          </div>

          <div v-if="latestSignals.length" class="sd-side-section">
            <div class="sd-subhead">
              <h3>Tín hiệu mới nhất</h3>
            </div>
            <div class="sd-signal-list">
              <article
                v-for="signal in latestSignals"
                :key="`${signal.signalType}-${signal.createdAt}`"
                class="sd-signal-item"
              >
                <strong>{{ getVLabel(signal.signalType) }}</strong>
                <p>{{ signal.severity }} · {{ Math.round((signal.confidence || 0) * 100) }}%</p>
              </article>
            </div>
          </div>

          <div class="sd-panel__head sd-panel__head--sub">
            <h2>Ghi chú</h2>
            <span class="sd-count">{{ notes.length }}</span>
          </div>
          <textarea
            v-model="newNote"
            class="sd-note"
            rows="3"
            placeholder="Nhập ghi chú..."
          />
          <button
            class="sd-btn sd-btn--primary sd-note-submit"
            type="button"
            :disabled="!newNote.trim() || noteLoading"
            @click="addNote"
          >
            Thêm ghi chú
          </button>

          <div
            v-if="notes.length === 0"
            class="sd-empty"
          >
            Chưa có ghi chú
          </div>
          <div
            v-else
            class="sd-notes"
          >
            <article
              v-for="note in notes"
              :key="note.key || note.id || note.at"
              class="sd-note-item"
            >
              <p>{{ note.details || note.note || note.content }}</p>
              <time>{{ formatTime(note.at || note.timestamp) }}</time>
            </article>
          </div>
        </aside>
      </section>
    </template>

    <Dialog
      v-model:visible="showWarnDialog"
      header="Gửi cảnh báo"
      :modal="true"
      :closable="true"
      :style="{ width: '420px' }"
    >
      <textarea
        v-model="warningMessage"
        class="sd-note"
        rows="3"
        placeholder="Nội dung cảnh báo"
      />
      <template #footer>
        <button
          class="sd-btn"
          @click="showWarnDialog = false"
        >
          Hủy
        </button>
        <button
          class="sd-btn sd-btn--primary"
          :disabled="actionLoading === 'warning'"
          @click="confirmSendWarning"
        >
          Gửi
        </button>
      </template>
    </Dialog>

    <Dialog
      v-model:visible="showPauseDialog"
      header="Tạm dừng bài thi"
      :modal="true"
      :closable="true"
      :style="{ width: '420px' }"
    >
      <textarea
        v-model="pauseReason"
        class="sd-note"
        rows="2"
        placeholder="Lý do"
      />
      <template #footer>
        <button
          class="sd-btn"
          @click="showPauseDialog = false"
        >
          Hủy
        </button>
        <button
          class="sd-btn sd-btn--primary"
          :disabled="actionLoading === 'pause'"
          @click="confirmPause"
        >
          Xác nhận
        </button>
      </template>
    </Dialog>

    <Dialog
      v-model:visible="showResumeDialog"
      header="Cho phép tiếp tục"
      :modal="true"
      :closable="true"
      :style="{ width: '420px' }"
    >
      <textarea
        v-model="resumeMessage"
        class="sd-note"
        rows="2"
        placeholder="Lời nhắn"
      />
      <template #footer>
        <button
          class="sd-btn"
          @click="showResumeDialog = false"
        >
          Hủy
        </button>
        <button
          class="sd-btn sd-btn--success"
          :disabled="actionLoading === 'resume'"
          @click="confirmResumeAction"
        >
          Cho tiếp tục
        </button>
      </template>
    </Dialog>

    <Dialog
      v-model:visible="showStopDialog"
      header="Buộc nộp bài"
      :modal="true"
      :closable="true"
      :style="{ width: '420px' }"
    >
      <textarea
        v-model="stopReason"
        class="sd-note"
        rows="2"
        placeholder="Lý do"
      />
      <template #footer>
        <button
          class="sd-btn"
          @click="showStopDialog = false"
        >
          Hủy
        </button>
        <button
          class="sd-btn sd-btn--danger"
          :disabled="actionLoading === 'invalidate'"
          @click="confirmStop"
        >
          Xác nhận
        </button>
      </template>
    </Dialog>
  </main>
</template>

<script setup>
import { ref, computed, onUnmounted, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import Dialog from 'primevue/dialog'
import LucideIcon from '../../../common/LucideIcon.vue'
import { useToast } from '../../../../composables/useToast'
import { useExamMonitoring } from '../../../../composables/useExamMonitoring'
import { fetchAttemptDetail, fetchProctorRisk, fetchProctorTimeline, reviewProctorFlag, sendTeacherWarning, pauseAttempt, resumeAttempt, invalidateAttempt, addMonitoringNote } from '../../../../services/examMonitoringService'

const RISK_BAND_THRESHOLDS = { CRITICAL: 81, HIGH_RISK: 61, SUSPICIOUS: 31 }
const RISK_LEVEL_LABELS = { CRITICAL: 'Nguy cơ cao', HIGH_RISK: 'Rủi ro cao', SUSPICIOUS: 'Đáng ngờ', CLEAN: 'Bình thường' }
const V_LABELS = {
  TAB_SWITCH: 'Chuyển tab',
  WINDOW_BLUR: 'Mất tiêu điểm',
  SCREEN_LEAVE: 'Rời màn hình thi',
  IDLE_TIME: 'Không hoạt động',
  RIGHT_CLICK: 'Click chuột phải',
  EXIT_FULLSCREEN: 'Thoát toàn màn hình',
  FULLSCREEN_VIOLATION: 'Thoát hoặc né toàn màn hình',
  CLIPBOARD_ABUSE: 'Clipboard bất thường',
  COPY_ATTEMPT: 'Sao chép nội dung',
  PASTE_ATTEMPT: 'Dán nội dung',
  LONG_PASTE: 'Dán đoạn dài',
  DEVTOOLS_OPEN: 'Mở DevTools',
  PRINT_SCREEN: 'Chụp màn hình',
  MULTI_MONITOR: 'Nhiều màn hình',
  DUPLICATE_IP: 'IP trùng lặp',
  IP_ANOMALY: 'IP hoặc thiết bị đáng ngờ',
  DEVICE_FINGERPRINT_CHANGED: 'Thiết bị thay đổi',
  RISK_SCORE: 'Cập nhật điểm rủi ro',
  ATTEMPT_PAUSED: 'Tạm dừng bài thi',
  ATTEMPT_RESUMED: 'Khôi phục bài thi',
  ATTEMPT_STOPPED: 'Đình chỉ bài thi',
  WARNING_SENT: 'Cảnh báo đã gửi',
  NOTE: 'Ghi chú'
}
const V_COLORS = {
  TAB_SWITCH: 'var(--ds-warning)',
  WINDOW_BLUR: 'var(--ds-warning)',
  SCREEN_LEAVE: 'var(--ds-warning)',
  EXIT_FULLSCREEN: 'var(--ds-warning)',
  FULLSCREEN_VIOLATION: 'var(--ds-warning)',
  CLIPBOARD_ABUSE: 'var(--ds-danger)',
  COPY_ATTEMPT: 'var(--ds-danger)',
  PASTE_ATTEMPT: 'var(--ds-danger)',
  LONG_PASTE: 'var(--ds-danger)',
  DEVTOOLS_OPEN: 'var(--ds-danger)',
  DUPLICATE_IP: 'var(--ds-danger)',
  IP_ANOMALY: 'var(--ds-danger)',
  DEVICE_FINGERPRINT_CHANGED: 'var(--ds-danger)',
  RISK_SCORE: 'var(--ds-text-muted)',
  ATTEMPT_PAUSED: 'var(--ds-warning)',
  ATTEMPT_RESUMED: 'var(--ds-success)',
  ATTEMPT_STOPPED: 'var(--ds-danger)',
  WARNING_SENT: 'var(--ds-primary)',
  NOTE: 'var(--ds-text-muted)'
}

const route = useRoute()
const toast = useToast()
const { subscribeToAttempt, unsubscribeFromAttempt } = useExamMonitoring()
const attemptId = computed(() => route.params.attemptId)
const examId = computed(() => route.params.examId)
const backLink = computed(() => `/teacher/exams/${examId.value}/monitoring`)
const loading = ref(true)
const error = ref('')
const attemptData = ref({})
const riskData = ref({})
const timeline = ref([])
const notesTimeline = ref([])
const timelineFilter = ref('')
const timelinePage = ref(1)
const timelinePageSize = 10
const timelineTotalPages = ref(1)
const timelineTotalElements = ref(0)
const newNote = ref('')
const noteLoading = ref(false)
const flagReviewNote = ref('')
const flagActionLoading = ref('')
const showWarnDialog = ref(false)
const showStopDialog = ref(false)
const showPauseDialog = ref(false)
const showResumeDialog = ref(false)
const warningMessage = ref('')
const stopReason = ref('')
const pauseReason = ref('')
const resumeMessage = ref('')
const actionLoading = ref('')

const studentName = computed(() => attemptData.value.student || attemptData.value.studentName || 'Chưa có tên')
const studentCode = computed(() => attemptData.value.studentCode || '')
const studentEmail = computed(() => attemptData.value.email || attemptData.value.studentEmail || '')
const studentInitials = computed(() => (studentName.value.trim().split(/\s+/).at(-1)?.[0] || '?').toUpperCase())
const riskScore = computed(() => Math.round(riskData.value.riskScore ?? riskData.value.score ?? 0))
const riskBand = computed(() => riskScore.value >= RISK_BAND_THRESHOLDS.CRITICAL ? 'CRITICAL' : riskScore.value >= RISK_BAND_THRESHOLDS.HIGH_RISK ? 'HIGH_RISK' : riskScore.value >= RISK_BAND_THRESHOLDS.SUSPICIOUS ? 'SUSPICIOUS' : 'CLEAN')
const riskBandLabel = computed(() => RISK_LEVEL_LABELS[riskBand.value] || riskBand.value)
const riskColor = computed(() => ({ CRITICAL: 'var(--ds-danger)', HIGH_RISK: 'var(--ds-warning)', SUSPICIOUS: 'var(--ds-accent)', CLEAN: 'var(--ds-text-muted)' }[riskBand.value] || 'var(--ds-text-muted)'))
const riskBg = computed(() => ({ CRITICAL: 'rgba(220,38,38,0.08)', HIGH_RISK: 'rgba(217,119,6,0.08)', SUSPICIOUS: 'rgba(245,158,11,0.08)', CLEAN: 'rgba(0,0,0,0.04)' }[riskBand.value] || 'rgba(0,0,0,0.04)'))
const statusToken = computed(() => {
  const raw = String(attemptData.value.status || '').toUpperCase()
  if (/SUBMITTED|COMPLETED/.test(raw)) return 'SUBMITTED'
  if (/PAUSED/.test(raw)) return 'PAUSED'
  if (/STOPPED|OFFLINE/.test(raw)) return 'STOPPED'
  if (/ACTIVE|IN_PROGRESS/.test(raw)) return 'ONLINE'
  return 'OFFLINE'
})
const statusLabel = computed(() => ({ ONLINE: 'Đang thi', SUBMITTED: 'Đã nộp', PAUSED: 'Tạm dừng', STOPPED: 'Dừng', OFFLINE: 'Offline' }[statusToken.value]))
const isStudentPaused = computed(() => statusToken.value === 'PAUSED')
const isStudentTerminal = computed(() => statusToken.value === 'SUBMITTED' || statusToken.value === 'STOPPED')
const sessionTime = computed(() => {
  const ts = attemptData.value.startedAt
  if (!ts) return 'Chưa bắt đầu'
  const diff = Math.floor((Date.now() - new Date(ts).getTime()) / 60000)
  if (Number.isNaN(diff) || diff < 0) return 'Chưa rõ'
  if (diff < 1) return '<1 phút'
  if (diff < 60) return `${diff} phút`
  return `${Math.floor(diff / 60)}h ${diff % 60}p`
})
const sortedTimeline = computed(() => [...timeline.value].sort((a, b) => new Date(b.at || b.timestamp || 0).getTime() - new Date(a.at || a.timestamp || 0).getTime()))
const sortedNotes = computed(() => [...notesTimeline.value].sort((a, b) => new Date(b.at || b.timestamp || 0).getTime() - new Date(a.at || a.timestamp || 0).getTime()))
const timelinePageCount = computed(() => timelineTotalPages.value)
const paginatedTimeline = computed(() => sortedTimeline.value)
const notes = computed(() => sortedNotes.value.filter(e => e.eventType === 'NOTE'))
const evidenceItems = computed(() => riskData.value.evidenceSummary || [])
const latestSignals = computed(() => riskData.value.latestSignals || [])
const shortRecommendedAction = computed(() => {
  const action = String(riskData.value.recommendedAction || '').toUpperCase()
  if (action === 'PAUSE_AND_REVIEW') return 'Pause + review'
  if (action === 'WARN_AND_ESCALATE') return 'Warn + escalate'
  if (action === 'REVIEW_ATTEMPT') return 'Review'
  if (action === 'CONTINUE_MONITORING') return 'Theo dõi'
  return action || 'Theo dõi'
})
const activeFlagStatusLabel = computed(() => {
  const status = String(riskData.value.activeFlagStatus || '').toUpperCase()
  if (!status) return 'Chưa có'
  if (status === 'OPEN') return 'Đang mở'
  if (status === 'CONFIRMED') return 'Đã xác nhận'
  if (status === 'DISMISSED') return 'Đã bỏ qua'
  return status
})

function getVLabel(type) { return V_LABELS[type] || type || 'Sự kiện' }
function getVColor(type) { return V_COLORS[type] || 'var(--ds-text-muted)' }
function formatTime(ts) { return ts ? new Date(ts).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : 'Chưa rõ' }
function normalizeRealtimeTime(payload) { return payload?.issuedAt || payload?.updatedAt || payload?.createdAt || new Date().toISOString() }
function matchesTimelineFilter(eventType) { return !timelineFilter.value || String(eventType || '').toUpperCase() === String(timelineFilter.value || '').toUpperCase() }
function prependTimelineItem(item) {
  if (timelinePage.value !== 1 || !matchesTimelineFilter(item?.eventType)) return false
  const dedupeKey = `${item.type}|${item.eventType}|${item.at}|${item.details || ''}`
  const exists = timeline.value.some(existing => `${existing.type}|${existing.eventType}|${existing.at}|${existing.details || ''}` === dedupeKey)
  if (exists) return false
  const next = [item, ...timeline.value.filter(existing => `${existing.type}|${existing.eventType}|${existing.at}|${existing.details || ''}` !== dedupeKey)]
  timeline.value = next.slice(0, timelinePageSize)
  return true
}

function isCurrentAttemptPayload(payload) {
  if (!payload) return false
  const payloadAttemptId = payload.attemptId ?? payload.id ?? payload.attempt?.id
  if (payloadAttemptId == null) return false
  return String(payloadAttemptId) === String(attemptId.value)
}

function handleRealtimeUpdate(payload) {
  if (!isCurrentAttemptPayload(payload)) return
  const type = String(payload?.type || '').toUpperCase()
  const issuedAt = normalizeRealtimeTime(payload)
  if (payload?.riskScore != null) riskData.value = { ...riskData.value, riskScore: payload.riskScore }
  if (payload?.riskLevel) riskData.value = { ...riskData.value, level: payload.riskLevel, riskLevel: payload.riskLevel }
  if (payload?.reviewRequired != null) riskData.value = { ...riskData.value, reviewRequired: payload.reviewRequired }
  if (payload?.reasons) riskData.value = { ...riskData.value, reasons: payload.reasons }
  if (payload?.recommendedAction) riskData.value = { ...riskData.value, recommendedAction: payload.recommendedAction }
  if (payload?.evidenceSummary) riskData.value = { ...riskData.value, evidenceSummary: payload.evidenceSummary }
  if (payload?.activeFlagId != null || payload?.activeFlagStatus) {
    riskData.value = {
      ...riskData.value,
      activeFlagId: payload.activeFlagId ?? riskData.value.activeFlagId,
      activeFlagStatus: payload.activeFlagStatus ?? riskData.value.activeFlagStatus
    }
  }

  if (type === 'ATTEMPT_PAUSED') attemptData.value = { ...attemptData.value, status: 'PAUSED' }
  else if (type === 'ATTEMPT_RESUMED') attemptData.value = { ...attemptData.value, status: 'IN_PROGRESS' }
  else if (type === 'ATTEMPT_STOPPED') attemptData.value = { ...attemptData.value, status: 'STOPPED' }

  if (type === 'FRAUD_SIGNAL_RECORDED') {
    const added = prependTimelineItem({
      type: 'FRAUD_SIGNAL',
      at: issuedAt,
      eventType: payload.signalType || 'UNKNOWN_SIGNAL',
      severity: payload.severity,
      confidence: payload.confidence,
      evidence: payload.evidence,
      details: payload.signalType || 'UNKNOWN_SIGNAL'
    })
    if (added) {
      timelineTotalElements.value += 1
      timelineTotalPages.value = Math.max(1, Math.ceil(timelineTotalElements.value / timelinePageSize))
    }
    return
  }

  if (type === 'RISK_UPDATED') {
    const added = prependTimelineItem({
      type: 'MONITORING_EVENT',
      at: issuedAt,
      eventType: 'RISK_SCORE',
      riskScore: payload.riskScore,
      riskLevel: payload.riskLevel,
      suspicious: payload.reviewRequired,
      breakdown: payload.breakdown,
      details: payload.actionTaken || 'UPDATED'
    })
    if (added) {
      timelineTotalElements.value += 1
      timelineTotalPages.value = Math.max(1, Math.ceil(timelineTotalElements.value / timelinePageSize))
    }
    return
  }

  if (type === 'WARNING_SENT' || type === 'ATTEMPT_PAUSED' || type === 'ATTEMPT_RESUMED' || type === 'ATTEMPT_STOPPED') {
    const added = prependTimelineItem({
      type: 'MONITORING_EVENT',
      at: issuedAt,
      eventType: type,
      riskScore: payload.riskScore,
      riskLevel: payload.riskLevel,
      details: payload.message || type
    })
    if (added) {
      timelineTotalElements.value += 1
      timelineTotalPages.value = Math.max(1, Math.ceil(timelineTotalElements.value / timelinePageSize))
    }
  }
}

async function loadTimeline() {
  try {
    const page = await fetchProctorTimeline(attemptId.value, {
      page: timelinePage.value,
      size: timelinePageSize,
      eventType: timelineFilter.value
    })
    timeline.value = Array.isArray(page) ? page : (page.items || [])
    timelineTotalPages.value = Math.max(1, page.totalPages || 1)
    timelineTotalElements.value = page.totalElements || timeline.value.length
  } catch {
    timeline.value = []
    timelineTotalPages.value = 1
    timelineTotalElements.value = 0
  }
}

async function loadNotes() {
  try {
    const page = await fetchProctorTimeline(attemptId.value, {
      page: 1,
      size: 50,
      eventType: 'NOTE'
    })
    notesTimeline.value = Array.isArray(page) ? page : (page.items || [])
  } catch {
    notesTimeline.value = []
  }
}

async function loadData() {
  if (!attemptId.value) return
  loading.value = true
  error.value = ''
  try {
    const [attempt, risk] = await Promise.allSettled([fetchAttemptDetail(attemptId.value), fetchProctorRisk(attemptId.value)])
    if (attempt.status === 'fulfilled' && attempt.value) attemptData.value = attempt.value
    else {
      error.value = 'Không tìm thấy thông tin bài thi.'
      return
    }
    if (risk.status === 'fulfilled' && risk.value) riskData.value = risk.value
    flagReviewNote.value = ''
    await Promise.all([loadTimeline(), loadNotes()])
  } catch (err) {
    error.value = err?.message || 'Tải dữ liệu thất bại.'
  } finally {
    loading.value = false
  }
}

async function addNote() {
  if (!newNote.value.trim() || noteLoading.value) return
  noteLoading.value = true
  try {
    await addMonitoringNote(attemptId.value, newNote.value)
    newNote.value = ''
    toast.success('Đã thêm ghi chú.')
    await loadNotes()
  } catch (err) {
    toast.error(err?.message || 'Thêm ghi chú thất bại.')
  } finally {
    noteLoading.value = false
  }
}

async function reviewFlag(status) {
  if (!riskData.value.activeFlagId || flagActionLoading.value) return
  flagActionLoading.value = status
  try {
    const reviewed = await reviewProctorFlag(riskData.value.activeFlagId, {
      status,
      teacherNote: flagReviewNote.value
    })
    riskData.value = {
      ...riskData.value,
      activeFlagStatus: reviewed?.status || status
    }
    toast.success(status === 'CONFIRMED' ? 'Đã xác nhận flag.' : 'Đã bỏ qua flag.')
    await loadData()
  } catch (err) {
    toast.error(err?.message || 'Cập nhật flag thất bại.')
  } finally {
    flagActionLoading.value = ''
  }
}

async function runAction(key, call, successMsg, errorMsg) {
  actionLoading.value = key
  try {
    await call(attemptId.value)
    toast.success(successMsg)
    await loadData()
  } catch (err) {
    toast.error(err?.message || errorMsg)
  } finally {
    actionLoading.value = ''
  }
}

async function confirmSendWarning() {
  showWarnDialog.value = false
  await runAction('warning', id => sendTeacherWarning(id, warningMessage.value), 'Đã gửi cảnh báo.', 'Gửi cảnh báo thất bại.')
}

async function confirmPause() {
  showPauseDialog.value = false
  await runAction('pause', id => pauseAttempt(id, pauseReason.value), 'Đã tạm dừng bài thi.', 'Tạm dừng thất bại.')
}

async function confirmResumeAction() {
  showResumeDialog.value = false
  await runAction('resume', id => resumeAttempt(id, resumeMessage.value), 'Đã cho phép tiếp tục.', 'Khôi phục thất bại.')
}

async function confirmStop() {
  showStopDialog.value = false
  await runAction('invalidate', id => invalidateAttempt(id, stopReason.value), 'Đã buộc nộp bài.', 'Buộc nộp bài thất bại.')
}

watch(attemptId, (id, prev) => {
  if (prev && prev !== id) unsubscribeFromAttempt(prev)
  if (id) {
    void loadData()
    subscribeToAttempt(id, handleRealtimeUpdate)
  }
}, { immediate: true })

watch(timelineFilter, () => {
  timelinePage.value = 1
  void loadTimeline()
})

watch(timelinePage, () => {
  void loadTimeline()
})

watch(timelinePageCount, count => {
  if (timelinePage.value > count) {
    timelinePage.value = count
  }
})

onUnmounted(() => {
  if (attemptId.value) unsubscribeFromAttempt(attemptId.value)
})
</script>

<style scoped>
.sd-page {
  min-height: 100vh;
  padding: var(--ds-space-5);
  background: var(--ds-bg);
  color: var(--ds-text);
}

.sd-header,
.sd-strip,
.sd-actions,
.sd-panel,
.sd-state,
.sd-empty {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  box-shadow: var(--ds-shadow-sm);
}

.sd-header {
  display: grid;
  grid-template-columns: 40px 48px minmax(0, 1fr) auto auto;
  gap: var(--ds-space-3);
  align-items: center;
  padding: var(--ds-space-4);
}

.sd-back,
.sd-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.sd-back {
  width: 40px;
  color: var(--ds-text-secondary);
  background: var(--ds-surface-muted);
}

.sd-back:hover {
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.sd-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: var(--ds-radius-md);
  font-weight: 900;
}

.sd-title {
  min-width: 0;
}

.sd-title span {
  color: var(--ds-primary);
  font-size: var(--ds-text-xs);
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.sd-title h1 {
  margin: var(--ds-space-1) 0 0;
  overflow: hidden;
  font-size: var(--ds-text-2xl);
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sd-title p,
.sd-strip,
.sd-event p,
.sd-note-item p,
.sd-dialog-text,
.sd-state p {
  color: var(--ds-text-secondary);
}

.sd-title p {
  margin: var(--ds-space-1) 0 0;
  overflow: hidden;
  font-size: var(--ds-text-sm);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sd-status {
  display: grid;
  gap: 2px;
  text-align: right;
}

.sd-status strong {
  font-size: var(--ds-text-3xl);
  line-height: 1;
}

.sd-status span {
  color: var(--ds-text-secondary);
  font-size: var(--ds-text-sm);
  font-weight: 800;
}

.sd-strip {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ds-space-3);
  margin: var(--ds-space-4) 0;
  padding: var(--ds-space-3) var(--ds-space-4);
  font-size: var(--ds-text-sm);
}

.sd-strip strong {
  color: var(--ds-text);
}

.sd-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ds-space-2);
  margin-bottom: var(--ds-space-4);
  padding: var(--ds-space-3);
}

.sd-btn {
  padding: 0 var(--ds-space-4);
  color: var(--ds-text);
  background: var(--ds-surface);
}

.sd-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.sd-btn--primary {
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary);
}

.sd-btn--warn {
  color: var(--ds-warning);
  background: var(--ds-warning-soft);
  border-color: var(--ds-warning);
}

.sd-btn--success {
  color: var(--ds-success);
  background: var(--ds-success-soft);
  border-color: var(--ds-success);
}

.sd-btn--danger {
  color: var(--ds-danger);
  background: var(--ds-danger-soft);
  border-color: var(--ds-danger);
}

.sd-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: var(--ds-space-4);
  align-items: start;
}

.sd-panel {
  padding: var(--ds-space-4);
}

.sd-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ds-space-3);
  margin-bottom: var(--ds-space-3);
}

.sd-panel__head h2 {
  margin: 0;
  font-size: var(--ds-text-lg);
}

.sd-count {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 var(--ds-space-3);
  border-radius: var(--ds-radius-full);
  color: var(--ds-text-secondary);
  background: var(--ds-surface-muted);
  font-size: var(--ds-text-xs);
  font-weight: 900;
}

.sd-select,
.sd-note {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  color: var(--ds-text);
  background: var(--ds-surface);
  font: inherit;
}

.sd-select {
  width: auto;
  min-height: 38px;
  padding: 0 var(--ds-space-3);
}

.sd-note {
  padding: var(--ds-space-3);
  resize: vertical;
  margin-bottom: var(--ds-space-3);
}

.sd-note-submit {
  width: 100%;
  margin-bottom: var(--ds-space-3);
}

.sd-timeline,
.sd-notes {
  display: grid;
  gap: var(--ds-space-2);
}

.sd-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ds-space-3);
  margin-top: var(--ds-space-3);
}

.sd-pagination span {
  color: var(--ds-text-secondary);
  font-size: var(--ds-text-sm);
  font-weight: 800;
}

.sd-event,
.sd-note-item {
  padding: var(--ds-space-3);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  background: var(--ds-surface-muted);
}

.sd-event {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  gap: var(--ds-space-3);
  align-items: start;
}

.sd-event__dot {
  width: 10px;
  height: 10px;
  margin-top: 5px;
  border-radius: 999px;
}

.sd-event strong {
  font-size: var(--ds-text-sm);
}

.sd-event p,
.sd-note-item p {
  margin: var(--ds-space-1) 0 0;
  line-height: 1.45;
}

.sd-event time,
.sd-note-item time {
  color: var(--ds-text-muted);
  font-size: var(--ds-text-xs);
  font-weight: 800;
  white-space: nowrap;
}

.sd-empty {
  padding: var(--ds-space-5);
  color: var(--ds-text-secondary);
  text-align: center;
  border-style: dashed;
  box-shadow: none;
}

.sd-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--ds-space-3);
  min-height: calc(100vh - 40px);
  text-align: center;
}

.sd-state h2,
.sd-state p {
  margin: 0;
}

.sd-state--error {
  color: var(--ds-danger);
}

.sd-spinner {
  width: 34px;
  height: 34px;
  border: 3px solid var(--ds-gray-200);
  border-top-color: var(--ds-primary);
  border-radius: 50%;
  animation: sd-spin 0.8s linear infinite;
}

.sd-dialog-text {
  margin: 0 0 var(--ds-space-3);
}

.sd-panel__head--sub {
  margin-top: var(--ds-space-4);
}

.sd-risk-card,
.sd-flag-box {
  margin-bottom: var(--ds-space-3);
  padding: var(--ds-space-3);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  background: var(--ds-surface-muted);
}

.sd-risk-list {
  display: grid;
  gap: var(--ds-space-2);
  margin-bottom: var(--ds-space-3);
}

.sd-risk-list div,
.sd-reason-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ds-space-3);
  padding: var(--ds-space-2) 0;
  border-bottom: 1px solid var(--ds-border);
}

.sd-risk-list div:last-child {
  border-bottom: 0;
}

.sd-risk-list span,
.sd-reason-item p {
  color: var(--ds-text-secondary);
}

.sd-risk-list strong {
  color: var(--ds-text);
  font-size: var(--ds-text-sm);
}

.sd-reasons {
  display: grid;
  gap: 0;
}

.sd-reason-item strong {
  flex: 0 0 180px;
  margin: 0;
}

.sd-reason-item p {
  margin: 0;
  line-height: 1.45;
  text-align: right;
}

.sd-flag-actions {
  display: flex;
  gap: var(--ds-space-2);
}

.sd-evidence {
  margin-bottom: var(--ds-space-3);
  padding: var(--ds-space-3) 0;
  border-top: 1px solid var(--ds-border);
  border-bottom: 1px solid var(--ds-border);
}

.sd-evidence ul {
  margin: 0;
  padding-left: 18px;
}

.sd-evidence li {
  color: var(--ds-text-secondary);
}

.sd-evidence li + li {
  margin-top: 6px;
}

.sd-subhead {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--ds-space-2);
}

.sd-subhead h3 {
  margin: 0;
  font-size: var(--ds-text-sm);
}

.sd-side-section {
  margin-top: var(--ds-space-4);
  padding-top: var(--ds-space-4);
  border-top: 1px solid var(--ds-border);
}

.sd-signal-list {
  display: grid;
  gap: var(--ds-space-2);
}

.sd-signal-item {
  padding: var(--ds-space-2) 0;
  border-bottom: 1px solid var(--ds-border);
}

.sd-signal-item:last-child {
  border-bottom: 0;
}

.sd-signal-item strong {
  display: block;
  margin-bottom: 4px;
  font-size: var(--ds-text-sm);
}

.sd-signal-item p {
  margin: 0;
  color: var(--ds-text-secondary);
  font-size: var(--ds-text-sm);
}

@keyframes sd-spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 900px) {
  .sd-content {
    grid-template-columns: 1fr;
  }

  .sd-header {
    grid-template-columns: 40px 48px minmax(0, 1fr) auto;
  }

  .sd-status {
    grid-column: 1 / -1;
    text-align: left;
  }
}

@media (max-width: 560px) {
  .sd-page {
    padding: var(--ds-space-3);
  }

  .sd-actions,
  .sd-btn,
  .sd-select {
    width: 100%;
  }

  .sd-event {
    grid-template-columns: 10px minmax(0, 1fr);
  }

  .sd-event time {
    grid-column: 2;
  }
}

@media (prefers-reduced-motion: reduce) {
  .sd-spinner {
    animation: none;
  }
}
</style>
