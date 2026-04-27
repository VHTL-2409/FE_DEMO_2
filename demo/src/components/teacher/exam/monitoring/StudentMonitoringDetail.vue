<template>
  <main class="smd-root">
    <!-- Loading -->
    <section
      v-if="loading"
      class="smd-loading"
    >
      <div class="smd-spinner" />
      <p>Đang tải hồ sơ thí sinh...</p>
    </section>

    <!-- Error -->
    <section
      v-else-if="error"
      class="smd-error"
    >
      <LucideIcon name="alert-circle" :size="34" />
      <h2>Không thể tải dữ liệu</h2>
      <p>{{ error }}</p>
      <button
        type="button"
        class="smd-btn smd-btn--primary"
        @click="loadData"
      >
        Thử lại
      </button>
    </section>

    <!-- Content -->
    <template v-else>
      <!-- ── Header ───────────────────────────────────────────── -->
      <header class="smd-header">
        <RouterLink
          :to="backLink"
          class="smd-back"
          aria-label="Quay lại phòng giám sát"
        >
          <LucideIcon name="arrow-left" :size="18" />
        </RouterLink>

        <div class="smd-header__main">
          <div class="smd-header__meta">
            <span class="smd-label">Hồ sơ thí sinh</span>
            <span
              class="smd-status-badge"
              :class="`smd-status-badge--${statusToken?.toLowerCase()}`"
            >
              {{ statusLabel }}
            </span>
          </div>
          <h1 class="smd-title">{{ studentName }}</h1>
          <p class="smd-subtitle">
            {{ [
              riskData.student?.studentCode || attemptData.studentCode,
              riskData.student?.email || attemptData.email || attemptData.studentEmail
            ].filter(Boolean).join(' · ') || 'Chưa có thông tin liên hệ' }}
          </p>
        </div>

        <div class="smd-header__risk">
          <span
            class="smd-risk-big"
            :style="{ color: riskColor }"
          >
            {{ riskScore }}
          </span>
          <span class="smd-risk-level">
            {{ riskBandLabel }}
          </span>
        </div>

        <div class="smd-header__actions">
          <span
            class="smd-conn"
            :class="isConnected ? 'smd-conn--on' : 'smd-conn--off'"
            :title="isConnected ? 'Kết nối realtime' : 'Đang dùng polling'"
          >
            <i class="smd-conn-dot" />
            {{ isConnected ? 'Realtime' : 'Polling' }}
          </span>
          <button
            type="button"
            class="smd-icon-btn"
            :disabled="isLoadingData"
            aria-label="Làm mới dữ liệu"
            title="Làm mới"
            @click="loadData"
          >
            <LucideIcon
              name="refresh-cw"
              :size="16"
              :class="{ 'smd-spin': isLoadingData }"
            />
          </button>
        </div>
      </header>

      <!-- ── Quick stats strip ─────────────────────────────── -->
      <div class="smd-strip">
        <div class="smd-strip-item">
          <span class="smd-strip-label">Thời gian</span>
          <span class="smd-strip-val">{{ sessionTime }}</span>
        </div>
        <div class="smd-strip-item">
          <span class="smd-strip-label">Tiến độ</span>
          <span class="smd-strip-val">
            {{ attemptData.answeredCount || 0 }}/{{ attemptData.totalQuestions || 0 }} câu
          </span>
        </div>
        <div class="smd-strip-item">
          <span class="smd-strip-label">Camera</span>
          <span
            class="smd-strip-val"
            :class="attemptData.cameraOn ? 'smd-strip-val--ok' : 'smd-strip-val--warn'"
          >
            {{ attemptData.cameraOn ? 'Bật' : 'Tắt' }}
          </span>
        </div>
        <div class="smd-strip-item">
          <span class="smd-strip-label">Micro</span>
          <span
            class="smd-strip-val"
            :class="attemptData.micOn ? 'smd-strip-val--ok' : 'smd-strip-val--warn'"
          >
            {{ attemptData.micOn ? 'Bật' : 'Tắt' }}
          </span>
        </div>
        <div v-if="riskData.activeFlagId" class="smd-strip-item">
          <span class="smd-strip-label">Flag</span>
          <span class="smd-strip-val smd-strip-val--warn">
            {{ activeFlagStatusLabel }}
          </span>
        </div>
        <div class="smd-strip-item">
          <span class="smd-strip-label">Cập nhật</span>
          <span class="smd-strip-val">{{ lastUpdatedLabel }}</span>
        </div>
      </div>

      <!-- ── Action buttons ───────────────────────────────── -->
      <div class="smd-actions">
        <button
          type="button"
          class="smd-btn smd-btn--warn"
          :disabled="actionLoading === 'warning' || !backendCapabilities.warn"
          :title="!backendCapabilities.warn ? 'Chưa hỗ trợ backend' : ''"
          @click="showWarnDialog = true"
        >
          <LucideIcon name="send" :size="15" />
          Gửi cảnh báo
        </button>

        <button
          v-if="isStudentPaused"
          type="button"
          class="smd-btn smd-btn--success"
          :disabled="actionLoading === 'resume' || !backendCapabilities.resume"
          :title="!backendCapabilities.resume ? 'Chưa hỗ trợ backend' : ''"
          @click="showResumeDialog = true"
        >
          <LucideIcon name="play" :size="15" />
          Cho phép tiếp tục
        </button>
        <button
          v-else
          type="button"
          class="smd-btn smd-btn--pause"
          :disabled="isStudentTerminal || actionLoading === 'pause' || !backendCapabilities.pause"
          :title="!backendCapabilities.pause ? 'Chưa hỗ trợ backend' : ''"
          @click="showPauseDialog = true"
        >
          <LucideIcon name="pause" :size="15" />
          Tạm dừng bài thi
        </button>

        <button
          type="button"
          class="smd-btn smd-btn--danger"
          :disabled="isStudentTerminal || actionLoading === 'invalidate' || !backendCapabilities.invalidate"
          :title="!backendCapabilities.invalidate ? 'Chưa hỗ trợ backend' : ''"
          @click="showStopDialog = true"
        >
          <LucideIcon name="stop-circle" :size="15" />
          Buộc nộp bài
        </button>
      </div>

      <!-- ── Main content: 2 columns ────────────────────────── -->
      <div class="smd-body">
        <!-- Left column -->
        <div class="smd-main">
          <!-- Risk summary -->
          <section class="smd-card">
            <div class="smd-card__head">
              <h2 class="smd-card__title">Đánh giá rủi ro</h2>
              <span
                class="smd-level-badge"
                :class="`smd-level-badge--${riskBand?.toLowerCase()?.replace('_', '-')}`"
              >
                {{ riskBandLabel }}
              </span>
            </div>

            <div class="smd-risk-summary">
              <div class="smd-risk-big-wrap">
                <span
                  class="smd-risk-big"
                  :style="{ color: riskColor }"
                >
                  {{ riskScore }}
                </span>
                <span class="smd-risk-max">/100</span>
              </div>

              <div class="smd-breakdown">
                <div class="smd-breakdown-row">
                  <span class="smd-breakdown-label">Screen leave</span>
                  <span class="smd-breakdown-val">{{ getBreakdownCount('SCREEN_LEAVE') }}</span>
                </div>
                <div class="smd-breakdown-row">
                  <span class="smd-breakdown-label">Clipboard</span>
                  <span class="smd-breakdown-val">{{ getBreakdownCount('CLIPBOARD') }}</span>
                </div>
                <div class="smd-breakdown-row">
                  <span class="smd-breakdown-label">Kỹ thuật</span>
                  <span class="smd-breakdown-val">{{ getBreakdownCount('TECHNICAL') }}</span>
                </div>
                <div class="smd-breakdown-row">
                  <span class="smd-breakdown-label">Định danh</span>
                  <span class="smd-breakdown-val">{{ getBreakdownCount('IDENTITY') }}</span>
                </div>
              </div>

              <div v-if="riskData.evidenceSummary?.length" class="smd-reasons">
                <span
                  v-for="(reason, i) in riskData.evidenceSummary.slice(0, 3)"
                  :key="i"
                  class="smd-reason-tag"
                >
                  {{ reason }}
                </span>
              </div>
            </div>
          </section>

          <!-- Latest evidence -->
          <section v-if="latestSignals.length" class="smd-card">
            <div class="smd-card__head">
              <h2 class="smd-card__title">Tín hiệu mới nhất</h2>
            </div>

            <div class="smd-evidence-list">
              <article
                v-for="signal in latestSignals"
                :key="`${signal.signalType}-${signal.createdAt}`"
                class="smd-evidence-item"
              >
                <div class="smd-evidence-item__head">
                  <span class="smd-evidence-item__type">{{ getSignalLabel(signal.signalType) }}</span>
                  <span class="smd-evidence-item__time">{{ formatTime(signal.occurredAt || signal.createdAt) }}</span>
                </div>
                <div v-if="signal.metadata && Object.keys(signal.metadata).length" class="smd-evidence-item__meta-keyvals">
                  <span
                    v-for="(val, key) in signal.metadata"
                    :key="key"
                    class="smd-evidence-kv"
                  >
                    <strong>{{ formatMetaKey(key) }}:</strong> {{ val }}
                  </span>
                </div>
              </article>
            </div>
          </section>

          <!-- Timeline -->
          <section class="smd-card">
            <div class="smd-card__head">
              <h2 class="smd-card__title">Dòng thời gian</h2>
              <div class="smd-card__head-right">
                <select
                  v-model="timelineFilter"
                  class="smd-select"
                  aria-label="Lọc dòng thời gian"
                >
                  <option value="">Tất cả</option>
                  <option value="SCREEN_LEAVE">Screen leave</option>
                  <option value="CLIPBOARD">Clipboard</option>
                  <option value="TECHNICAL">Kỹ thuật</option>
                  <option value="IDENTITY">Định danh</option>
                  <option value="WARNING">Cảnh báo</option>
                  <option value="NOTE">Ghi chú</option>
                </select>
              </div>
            </div>

            <div v-if="timeline.length === 0" class="smd-empty">
              <LucideIcon name="clock" :size="24" />
              <p>Chưa có sự kiện nào</p>
            </div>

            <div v-else class="smd-timeline">
              <article
                v-for="event in paginatedTimeline"
                :key="event._key"
                class="smd-event"
              >
                <span
                  class="smd-event__dot"
                  :style="{ background: getEventColor(event.eventType) }"
                />
                <div class="smd-event__body">
                  <span class="smd-event__type">{{ getEventLabel(event.eventType) }}</span>
                  <p v-if="event.displayMessage || event.details" class="smd-event__msg">
                    {{ event.displayMessage || event.details }}
                  </p>
                  <span v-if="event.riskImpact" class="smd-event__impact">
                    +{{ event.riskImpact }} risk
                  </span>
                </div>
                <time class="smd-event__time">{{ formatTime(event.at || event.timestamp || event.occurredAt) }}</time>
              </article>
            </div>

            <div v-if="timelinePageCount > 1" class="smd-pagination">
              <button
                type="button"
                class="smd-btn smd-btn--ghost"
                :disabled="timelinePage === 1"
                @click="timelinePage--"
              >
                <LucideIcon name="chevron-left" :size="14" />
                Trước
              </button>
              <span class="smd-pagination__info">{{ timelinePage }}/{{ timelinePageCount }}</span>
              <button
                type="button"
                class="smd-btn smd-btn--ghost"
                :disabled="timelinePage === timelinePageCount"
                @click="timelinePage++"
              >
                Sau
                <LucideIcon name="chevron-right" :size="14" />
              </button>
            </div>
          </section>
        </div>

        <!-- Right column: Review & Notes -->
        <aside class="smd-sidebar">
          <!-- Review flag -->
          <section class="smd-card">
            <div class="smd-card__head">
              <h2 class="smd-card__title">Review flag</h2>
              <span
                v-if="riskData.activeFlagId"
                class="smd-flag-badge"
                :class="`smd-flag-badge--${activeFlagStatusLabel?.toLowerCase()?.replace(/\s+/g, '-')}`"
              >
                {{ activeFlagStatusLabel }}
              </span>
            </div>

            <div v-if="riskData.activeFlagId" class="smd-flag-review">
              <p v-if="riskData.recommendedAction" class="smd-flag-review__action">
                {{ riskData.recommendedAction }}
              </p>
              <textarea
                v-model="flagReviewNote"
                class="smd-textarea"
                rows="3"
                placeholder="Ghi chú review..."
                aria-label="Ghi chú review"
              />
              <div class="smd-flag-review__btns">
                <button
                  type="button"
                  class="smd-btn smd-btn--success"
                  :disabled="flagActionLoading === 'CONFIRMED' || !backendCapabilities.reviewFlag"
                  :title="!backendCapabilities.reviewFlag ? 'Chưa hỗ trợ backend' : ''"
                  @click="reviewFlag('CONFIRMED')"
                >
                  <LucideIcon name="check" :size="15" />
                  Xác nhận
                </button>
                <button
                  type="button"
                  class="smd-btn smd-btn--ghost"
                  :disabled="flagActionLoading === 'DISMISSED' || !backendCapabilities.reviewFlag"
                  :title="!backendCapabilities.reviewFlag ? 'Chưa hỗ trợ backend' : ''"
                  @click="reviewFlag('DISMISSED')"
                >
                  Bỏ qua
                </button>
              </div>
            </div>
            <div v-else class="smd-empty smd-empty--sm">
              <LucideIcon name="flag-off" :size="20" />
              <p>Chưa có flag mở</p>
            </div>
          </section>

          <!-- Notes -->
          <section class="smd-card">
            <div class="smd-card__head">
              <h2 class="smd-card__title">Ghi chú</h2>
              <span class="smd-count">{{ notes.length }}</span>
            </div>

            <textarea
              v-model="newNote"
              class="smd-textarea"
              rows="2"
              placeholder="Nhập ghi chú..."
              aria-label="Nhập ghi chú"
            />
            <div class="smd-notes-actions">
              <button
                type="button"
                class="smd-btn smd-btn--primary"
                :disabled="!newNote.trim() || noteLoading"
                @click="addNote"
              >
                <LucideIcon name="plus" :size="15" />
                Thêm ghi chú
              </button>
              <button
                type="button"
                class="smd-btn smd-btn--ghost"
                :disabled="isLoadingData"
                title="Sao chép bằng chứng"
                @click="copyEvidence"
              >
                <LucideIcon name="copy" :size="15" />
                Sao chép
              </button>
              <button
                type="button"
                class="smd-btn smd-btn--ghost"
                :disabled="isLoadingData"
                title="Xuất file bằng chứng"
                @click="exportEvidence"
              >
                <LucideIcon name="download" :size="15" />
                Xuất file
              </button>
            </div>

            <div v-if="notes.length === 0" class="smd-empty smd-empty--sm">
              <p>Chưa có ghi chú</p>
            </div>
            <div v-else class="smd-notes">
              <article
                v-for="note in notes"
                :key="note._key"
                class="smd-note-item"
              >
                <p class="smd-note-item__body">{{ note.details || note.note || note.content }}</p>
                <time class="smd-note-item__time">{{ formatTime(note.at || note.timestamp) }}</time>
              </article>
            </div>
          </section>
        </aside>
      </div>
    </template>

    <!-- ── Dialogs ─────────────────────────────────────────────── -->
    <!-- Warning -->
    <Dialog
      v-model:visible="showWarnDialog"
      header="Gửi cảnh báo"
      :modal="true"
      :style="{ width: '420px' }"
    >
      <textarea
        v-model="warningMessage"
        class="smd-textarea"
        rows="3"
        placeholder="Nội dung cảnh báo (không bắt buộc)"
        aria-label="Nội dung cảnh báo"
      />
      <template #footer>
        <button type="button" class="smd-btn smd-btn--ghost" @click="showWarnDialog = false">
          Hủy
        </button>
        <button
          type="button"
          class="smd-btn smd-btn--warn"
          :disabled="actionLoading === 'warning'"
          @click="confirmSendWarning"
        >
          {{ actionLoading === 'warning' ? 'Đang gửi...' : 'Gửi cảnh báo' }}
        </button>
      </template>
    </Dialog>

    <!-- Pause -->
    <Dialog
      v-model:visible="showPauseDialog"
      header="Tạm dừng bài thi"
      :modal="true"
      :style="{ width: '420px' }"
    >
      <textarea
        v-model="pauseReason"
        class="smd-textarea"
        rows="2"
        placeholder="Lý do tạm dừng (không bắt buộc)"
        aria-label="Lý do"
      />
      <template #footer>
        <button type="button" class="smd-btn smd-btn--ghost" @click="showPauseDialog = false">
          Hủy
        </button>
        <button
          type="button"
          class="smd-btn smd-btn--pause"
          :disabled="actionLoading === 'pause'"
          @click="confirmPause"
        >
          {{ actionLoading === 'pause' ? 'Đang xử lý...' : 'Xác nhận' }}
        </button>
      </template>
    </Dialog>

    <!-- Resume -->
    <Dialog
      v-model:visible="showResumeDialog"
      header="Cho phép tiếp tục"
      :modal="true"
      :style="{ width: '420px' }"
    >
      <textarea
        v-model="resumeMessage"
        class="smd-textarea"
        rows="2"
        placeholder="Lời nhắn tới thí sinh (không bắt buộc)"
        aria-label="Lời nhắn"
      />
      <template #footer>
        <button type="button" class="smd-btn smd-btn--ghost" @click="showResumeDialog = false">
          Hủy
        </button>
        <button
          type="button"
          class="smd-btn smd-btn--success"
          :disabled="actionLoading === 'resume'"
          @click="confirmResume"
        >
          {{ actionLoading === 'resume' ? 'Đang xử lý...' : 'Cho tiếp tục' }}
        </button>
      </template>
    </Dialog>

    <!-- Stop -->
    <Dialog
      v-model:visible="showStopDialog"
      header="Xác nhận buộc nộp bài"
      :modal="true"
      :style="{ width: '420px' }"
    >
      <textarea
        v-model="stopReason"
        class="smd-textarea"
        rows="2"
        placeholder="Lý do buộc nộp bài"
        aria-label="Lý do"
      />
      <template #footer>
        <button type="button" class="smd-btn smd-btn--ghost" @click="showStopDialog = false">
          Hủy
        </button>
        <button
          type="button"
          class="smd-btn smd-btn--danger"
          :disabled="actionLoading === 'invalidate'"
          @click="confirmStop"
        >
          {{ actionLoading === 'invalidate' ? 'Đang xử lý...' : 'Xác nhận nộp bài' }}
        </button>
      </template>
    </Dialog>
  </main>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, onActivated } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import LucideIcon from '../../../common/LucideIcon.vue'
import { useToast } from '../../../../composables/useToast'
import { useExamMonitoring } from '../../../../composables/useExamMonitoring'
import {
  fetchAttemptDetail,
  fetchProctorRisk,
  fetchProctorTimeline,
  reviewProctorFlag,
  sendTeacherWarning,
  pauseAttempt,
  resumeAttempt,
  invalidateAttempt,
  addMonitoringNote
} from '../../../../services/examMonitoringService'

// ── Constants ─────────────────────────────────────────────────────
const RISK_BAND_THRESHOLDS = { CRITICAL: 81, HIGH_RISK: 61, SUSPICIOUS: 31 }
const RISK_LEVEL_LABELS = {
  CRITICAL: 'Nguy cơ cao',
  HIGH_RISK: 'Rủi ro cao',
  SUSPICIOUS: 'Đáng ngờ',
  CLEAN: 'Bình thường'
}
const SIGNAL_LABELS = {
  TAB_SWITCH: 'Chuyển tab',
  WINDOW_BLUR: 'Mất tiêu điểm',
  SCREEN_LEAVE: 'Rời màn hình',
  FULLSCREEN_VIOLATION: 'Thoát toàn màn hình',
  CLIPBOARD_ABUSE: 'Clipboard bất thường',
  COPY_ATTEMPT: 'Sao chép',
  PASTE_ATTEMPT: 'Dán nội dung',
  DEVTOOLS_OPEN: 'Mở DevTools',
  IP_ANOMALY: 'IP bất thường',
  WARNING_SENT: 'Cảnh báo',
  ATTEMPT_PAUSED: 'Tạm dừng',
  ATTEMPT_RESUMED: 'Tiếp tục',
  ATTEMPT_STOPPED: 'Đình chỉ',
  RISK_UPDATED: 'Cập nhật rủi ro',
  NOTE: 'Ghi chú'
}
const EVENT_COLORS = {
  TAB_SWITCH: 'var(--ds-warning)',
  WINDOW_BLUR: 'var(--ds-warning)',
  SCREEN_LEAVE: 'var(--ds-warning)',
  FULLSCREEN_VIOLATION: 'var(--ds-warning)',
  CLIPBOARD_ABUSE: 'var(--ds-danger)',
  COPY_ATTEMPT: 'var(--ds-danger)',
  PASTE_ATTEMPT: 'var(--ds-danger)',
  DEVTOOLS_OPEN: 'var(--ds-danger)',
  IP_ANOMALY: 'var(--ds-danger)',
  WARNING_SENT: 'var(--ds-primary)',
  ATTEMPT_PAUSED: 'var(--ds-warning)',
  ATTEMPT_RESUMED: 'var(--ds-success)',
  ATTEMPT_STOPPED: 'var(--ds-danger)',
  RISK_UPDATED: 'var(--ds-text-muted)',
  NOTE: 'var(--ds-text-muted)'
}

// ── Route & composables ────────────────────────────────────────────
const route = useRoute()
const toast = useToast()
const { subscribeToAttempt, unsubscribeFromAttempt, isConnected } = useExamMonitoring()

const attemptId = computed(() => route.params.attemptId)
const examId = computed(() => route.params.examId)
const backLink = computed(() => `/teacher/exams/${examId.value}/monitoring`)

// ── State ─────────────────────────────────────────────────────────
const loading = ref(true)
const isLoadingData = ref(false)
const error = ref('')
const attemptData = ref({})
const riskData = ref({})
const timeline = ref([])
const notesTimeline = ref([])
const timelineFilter = ref('')
const timelinePage = ref(1)
const timelinePageSize = 15

// Backend capability detection — true = assume supported, will be patched on loadData if error
const backendCapabilities = ref({
  warn: true,
  pause: true,
  resume: true,
  invalidate: true,
  reviewFlag: true,
  addNote: true
})

// Dialogs
const showWarnDialog = ref(false)
const showPauseDialog = ref(false)
const showResumeDialog = ref(false)
const showStopDialog = ref(false)
const warningMessage = ref('')
const pauseReason = ref('')
const resumeMessage = ref('')
const stopReason = ref('')

// Notes
const newNote = ref('')
const noteLoading = ref(false)
const flagReviewNote = ref('')
const flagActionLoading = ref('')

// Actions
const actionLoading = ref('')

// ── Computed ───────────────────────────────────────────────────────
const studentName = computed(() => {
  // Try student snapshot fields first (from risk API), then attempt fields
  const s = riskData.value.student?.name
    || riskData.value.student?.username
    || attemptData.value.student
    || attemptData.value.studentName
    || riskData.value.student?.email
    || attemptData.value.email
    || attemptData.value.studentCode
    || attemptData.value.username
  return s || 'Chưa có tên'
})
const riskScore = computed(() =>
  Math.round(riskData.value.score ?? attemptData.value.riskScore ?? 0)
)
const riskBand = computed(() => {
  // Use level from API first, fallback to computing from score
  const level = riskData.value.level
  if (level) return level.toUpperCase()
  const score = riskScore.value
  if (score >= RISK_BAND_THRESHOLDS.CRITICAL) return 'CRITICAL'
  if (score >= RISK_BAND_THRESHOLDS.HIGH_RISK) return 'HIGH_RISK'
  if (score >= RISK_BAND_THRESHOLDS.SUSPICIOUS) return 'SUSPICIOUS'
  return 'CLEAN'
})
const riskBandLabel = computed(() => RISK_LEVEL_LABELS[riskBand.value] || riskBand.value)
const riskColor = computed(() => ({
  CRITICAL: 'var(--ds-risk-critical)',
  HIGH_RISK: 'var(--ds-risk-high)',
  SUSPICIOUS: 'var(--ds-risk-moderate)',
  CLEAN: 'var(--ds-risk-clean)'
}[riskBand.value] || 'var(--ds-text-muted)'))

const statusToken = computed(() => {
  // Try attempt status first, then risk status
  const raw = String(attemptData.value.status || riskData.value.status || '').toUpperCase()
  if (/SUBMITTED|COMPLETED/.test(raw)) return 'SUBMITTED'
  if (/PAUSED/.test(raw)) return 'PAUSED'
  if (/STOPPED|OFFLINE/.test(raw)) return 'STOPPED'
  if (/ACTIVE|IN_PROGRESS/.test(raw)) return 'ONLINE'
  return 'OFFLINE'
})
const statusLabel = computed(() => ({
  ONLINE: 'Đang thi',
  SUBMITTED: 'Đã nộp',
  PAUSED: 'Tạm dừng',
  STOPPED: 'Đã dừng',
  OFFLINE: 'Offline'
}[statusToken.value]))

const isStudentPaused = computed(() => statusToken.value === 'PAUSED')
const isStudentTerminal = computed(() =>
  statusToken.value === 'SUBMITTED' || statusToken.value === 'STOPPED'
)

const sessionTime = computed(() => {
  // Try attemptSnapshot first, then attemptData
  const ts = riskData.value.attempt?.startedAt || attemptData.value.startedAt
  if (!ts) return 'Chưa bắt đầu'
  const diff = Math.floor((Date.now() - new Date(ts).getTime()) / 60000)
  if (Number.isNaN(diff) || diff < 0) return 'Chưa rõ'
  if (diff < 1) return '<1 phút'
  if (diff < 60) return `${diff} phút`
  return `${Math.floor(diff / 60)}h ${diff % 60}p`
})

const lastUpdatedLabel = computed(() => {
  const ts = riskData.value.updatedAt || attemptData.value.lastUpdatedAt || attemptData.value.updatedAt
  if (!ts) return '—'
  try {
    return new Date(ts).toLocaleTimeString('vi-VN', {
      hour: '2-digit', minute: '2-digit', second: '2-digit'
    })
  } catch {
    return '—'
  }
})

const latestSignals = computed(() => riskData.value.latestSignals || [])

// Breakdown from API returns Map<String, Integer> - access by signal type key
const getBreakdownCount = (key) => {
  const breakdown = riskData.value.breakdown
  if (!breakdown) return 0
  // Keys might be uppercase or comeCase
  return breakdown[key] ?? breakdown[key.toUpperCase()] ?? breakdown[key.toLowerCase()] ?? 0
}

const activeFlagStatusLabel = computed(() => {
  const status = String(riskData.value.status || '').toUpperCase()
  if (!status || status === 'CLEAN') return 'Chưa có'
  if (status === 'OPEN' || status === 'ACTIVE') return 'Đang mở'
  if (status === 'CONFIRMED') return 'Đã xác nhận'
  if (status === 'DISMISSED') return 'Đã bỏ qua'
  return status
})

// Sorted & filtered timeline
const sortedTimeline = computed(() => {
  return [...timeline.value].sort((a, b) => {
    const aTime = new Date(a.at || a.timestamp || a.occurredAt || 0).getTime()
    const bTime = new Date(b.at || b.timestamp || b.occurredAt || 0).getTime()
    return bTime - aTime
  }).map((item, i) => ({ ...item, _key: item.id || `${i}-${item.at}` }))
})

const filteredTimeline = computed(() => {
  if (!timelineFilter.value) return sortedTimeline.value
  return sortedTimeline.value.filter(e => {
    const type = String(e.eventType || '').toUpperCase()
    return type === timelineFilter.value.toUpperCase()
  })
})

const timelinePageCount = computed(() =>
  Math.max(1, Math.ceil(filteredTimeline.value.length / timelinePageSize))
)

const paginatedTimeline = computed(() => {
  const start = (timelinePage.value - 1) * timelinePageSize
  return filteredTimeline.value.slice(start, start + timelinePageSize)
})

const notes = computed(() =>
  sortedTimeline.value
    .filter(e => String(e.eventType || '').toUpperCase() === 'NOTE')
    .map((item, i) => ({ ...item, _key: item.id || `note-${i}` }))
)

// ── Helpers ──────────────────────────────────────────────────────────
function getSignalLabel(type) {
  if (!type) return 'Tín hiệu'
  return SIGNAL_LABELS[type] || type.replace(/_/g, ' ')
}

function getEventLabel(type) {
  if (!type) return 'Sự kiện'
  return SIGNAL_LABELS[type] || type.replace(/_/g, ' ')
}

function getEventColor(type) {
  return EVENT_COLORS[type] || 'var(--ds-text-muted)'
}

function formatTime(ts) {
  if (!ts) return '—'
  try {
    return new Date(ts).toLocaleTimeString('vi-VN', {
      hour: '2-digit', minute: '2-digit', second: '2-digit'
    })
  } catch {
    return '—'
  }
}

function formatMetaKey(key) {
  if (!key) return ''
  return key.replace(/([A-Z])/g, ' $1').replace(/^./, s => s.toUpperCase())
}

function makeEventKey(event) {
  return `${event.eventType || event.type || ''}|${event.at || event.timestamp || ''}|${event.details || event.displayMessage || ''}`
}

function prependTimelineItem(item) {
  const key = makeEventKey(item)
  const exists = timeline.value.some(e => makeEventKey(e) === key)
  if (exists) return false
  timeline.value = [item, ...timeline.value]
  return true
}

// ── Realtime handler ───────────────────────────────────────────────
function handleRealtimeUpdate(payload) {
  if (!payload) return
  const payloadAttemptId = payload.attemptId ?? payload.id ?? payload.attempt?.id
  if (String(payloadAttemptId || '') !== String(attemptId.value)) return

  const type = String(payload.type || '').toUpperCase()

  // Patch risk
  if (payload.riskScore != null) {
    riskData.value = { ...riskData.value, riskScore: payload.riskScore }
  }
  if (payload.riskLevel) {
    riskData.value = { ...riskData.value, riskLevel: payload.riskLevel }
  }
  if (payload.reasons) {
    riskData.value = { ...riskData.value, reasons: payload.reasons }
  }
  if (payload.recommendedAction) {
    riskData.value = { ...riskData.value, recommendedAction: payload.recommendedAction }
  }
  if (payload.activeFlagId != null || payload.activeFlagStatus) {
    riskData.value = {
      ...riskData.value,
      activeFlagId: payload.activeFlag?.id ?? payload.activeFlagId ?? riskData.value.activeFlagId,
      activeFlagStatus: payload.activeFlag?.status ?? payload.activeFlagStatus ?? riskData.value.activeFlagStatus
    }
  }

  // Patch status
  if (type === 'ATTEMPT_PAUSED') {
    attemptData.value = { ...attemptData.value, status: 'PAUSED' }
  } else if (type === 'ATTEMPT_RESUMED') {
    attemptData.value = { ...attemptData.value, status: 'IN_PROGRESS' }
  } else if (type === 'ATTEMPT_STOPPED') {
    attemptData.value = { ...attemptData.value, status: 'STOPPED' }
  }

  // Handle fraud signal
  if (type === 'FRAUD_SIGNAL_RECORDED') {
    const signal = payload.latestSignal || {}
    prependTimelineItem({
      eventType: signal.signalType || 'UNKNOWN',
      at: signal.occurredAt || payload.issuedAt || new Date().toISOString(),
      details: signal.displayMessage || signal.signalType,
      displayMessage: signal.displayMessage,
      severity: signal.severity,
      confidence: signal.confidence,
      riskImpact: signal.riskImpact,
      category: signal.category
    })
  }

  // Handle risk update — only update data, don't add timeline item to avoid flooding
  if (type === 'RISK_UPDATED') {
    if (payload.riskScore != null) {
      riskData.value = { ...riskData.value, riskScore: payload.riskScore }
    }
    // Removed: prependTimelineItem caused timeline to flood with RISK_UPDATED events
    // Risk score is already displayed in header, no need to duplicate in timeline
  }

  // Handle system events
  if (['WARNING_SENT', 'ATTEMPT_PAUSED', 'ATTEMPT_RESUMED', 'ATTEMPT_STOPPED'].includes(type)) {
    prependTimelineItem({
      eventType: type,
      at: payload.issuedAt || new Date().toISOString(),
      details: payload.message || SIGNAL_LABELS[type]
    })
  }
}

// ── Data loading ────────────────────────────────────────────────────
async function loadTimeline() {
  try {
    const result = await fetchProctorTimeline(attemptId.value, {
      page: 1,
      size: 100,
      eventType: timelineFilter.value || undefined
    })
    const apiItems = Array.isArray(result) ? result : (result?.items || [])

    // Deduplicate: remove items already in timeline before adding from API
    const existingKeys = new Set(timeline.value.map(e => makeEventKey(e)))
    const newItems = apiItems.filter(item => !existingKeys.has(makeEventKey(item)))

    // Merge: keep realtime items + new API items sorted by time
    const merged = [...timeline.value, ...newItems]
    timeline.value = merged.sort((a, b) => {
      const aTime = new Date(a.at || a.timestamp || a.occurredAt || 0).getTime()
      const bTime = new Date(b.at || b.timestamp || b.occurredAt || 0).getTime()
      return bTime - aTime
    })
  } catch {
    // Keep existing timeline on error
  }
}

async function loadNotes() {
  try {
    const result = await fetchProctorTimeline(attemptId.value, {
      page: 1,
      size: 50,
      eventType: 'NOTE'
    })
    notesTimeline.value = Array.isArray(result) ? result : (result?.items || [])
  } catch {
    notesTimeline.value = []
  }
}

async function loadData() {
  if (!attemptId.value) return
  isLoadingData.value = true
  error.value = ''
  // Reset timeline on fresh load to avoid duplication
  timeline.value = []
  try {
    const [attemptResult, riskResult] = await Promise.allSettled([
      fetchAttemptDetail(attemptId.value),
      fetchProctorRisk(attemptId.value)
    ])

    if (attemptResult.status === 'fulfilled' && attemptResult.value) {
      attemptData.value = attemptResult.value
    } else {
      error.value = 'Không tìm thấy thông tin bài thi.'
      return
    }

    if (riskResult.status === 'fulfilled' && riskResult.value) {
      riskData.value = riskResult.value
    }

    await Promise.all([loadTimeline(), loadNotes()])
  } catch (err) {
    error.value = err?.message || 'Tải dữ liệu thất bại.'
  } finally {
    isLoadingData.value = false
    loading.value = false
  }
}

// Detect unsupported capabilities from error responses
function detectCapabilityError(err, capability) {
  // Check various error response formats
  const status = err?.response?.status || err?.status || err?.data?.status
  if (status === 404 || status === 405 || status === 501) {
    backendCapabilities.value[capability] = false
    console.warn(`[StudentMonitoringDetail] Backend capability disabled: ${capability} (status: ${status})`)
  }
  // Log error for debugging
  console.error(`[StudentMonitoringDetail] Action error:`, err)
}

// ── Actions ─────────────────────────────────────────────────────────
async function addNote() {
  if (!newNote.value.trim() || noteLoading.value) return
  noteLoading.value = true
  try {
    await addMonitoringNote(attemptId.value, newNote.value)
    newNote.value = ''
    toast.success('Đã thêm ghi chú.')
    await loadNotes()
  } catch (err) {
    detectCapabilityError(err, 'addNote')
    toast.error(err?.message || 'Thêm ghi chú thất bại.')
  } finally {
    noteLoading.value = false
  }
}

function copyEvidence() {
  const evidence = {
    student: studentName.value,
    attemptId: attemptId.value,
    examId: examId.value,
    riskScore: riskScore.value,
    riskBand: riskBand.value,
    status: statusToken.value,
    latestSignals: latestSignals.value,
    timeline: sortedTimeline.value,
    notes: notes.value,
    flag: riskData.value.activeFlagId ? {
      id: riskData.value.activeFlagId,
      status: riskData.value.activeFlagStatus,
      recommendedAction: riskData.value.recommendedAction
    } : null,
    exportedAt: new Date().toISOString()
  }
  const text = JSON.stringify(evidence, null, 2)
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(text).then(() => {
      toast.success('Đã sao chép bằng chứng vào clipboard.')
    }).catch(() => {
      fallbackCopy(text)
    })
  } else {
    fallbackCopy(text)
  }
}

function fallbackCopy(text) {
  const ta = document.createElement('textarea')
  ta.value = text
  ta.style.position = 'fixed'
  ta.style.opacity = '0'
  document.body.appendChild(ta)
  ta.focus()
  ta.select()
  try {
    document.execCommand('copy')
    toast.success('Đã sao chép bằng chứng vào clipboard.')
  } catch {
    toast.error('Không thể sao chép. Hãy thử chọn text bên dưới.')
  }
  document.body.removeChild(ta)
}

function exportEvidence() {
  const evidence = {
    student: studentName.value,
    attemptId: attemptId.value,
    examId: examId.value,
    riskScore: riskScore.value,
    riskBand: riskBand.value,
    status: statusToken.value,
    latestSignals: latestSignals.value,
    timeline: sortedTimeline.value,
    notes: notes.value,
    flag: riskData.value.activeFlagId ? {
      id: riskData.value.activeFlagId,
      status: riskData.value.activeFlagStatus,
      recommendedAction: riskData.value.recommendedAction
    } : null,
    exportedAt: new Date().toISOString()
  }
  const blob = new Blob([JSON.stringify(evidence, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `evidence-${studentName.value}-${attemptId.value}.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  toast.success('Đã tải file bằng chứng.')
}

async function reviewFlag(status) {
  if (!riskData.value.activeFlagId || flagActionLoading.value) return
  flagActionLoading.value = status
  try {
    const reviewed = await reviewProctorFlag(riskData.value.activeFlagId, {
      status,
      teacherNote: flagReviewNote.value
    })
    // Clear flag from UI immediately on success
    riskData.value = {
      ...riskData.value,
      activeFlagId: null,
      activeFlagStatus: reviewed?.status || status
    }
    flagReviewNote.value = ''
    toast.success(status === 'CONFIRMED' ? 'Đã xác nhận flag.' : 'Đã bỏ qua flag.')
  } catch (err) {
    detectCapabilityError(err, 'reviewFlag')
    toast.error(err?.message || 'Cập nhật flag thất bại.')
  } finally {
    flagActionLoading.value = ''
  }
}

async function runAction(key, call, successMsg, errorMsg, capabilityKey) {
  actionLoading.value = key
  try {
    await call(attemptId.value)
    toast.success(successMsg)
    await loadData()
  } catch (err) {
    if (capabilityKey) detectCapabilityError(err, capabilityKey)
    toast.error(err?.message || errorMsg)
  } finally {
    actionLoading.value = ''
  }
}

async function confirmSendWarning() {
  showWarnDialog.value = false
  await runAction(
    'warning',
    () => sendTeacherWarning(attemptId.value, warningMessage.value),
    'Đã gửi cảnh báo.',
    'Gửi cảnh báo thất bại.',
    'warn'
  )
}

async function confirmPause() {
  showPauseDialog.value = false
  await runAction(
    'pause',
    () => pauseAttempt(attemptId.value, pauseReason.value),
    'Đã tạm dừng bài thi.',
    'Tạm dừng thất bại.',
    'pause'
  )
}

async function confirmResume() {
  showResumeDialog.value = false
  await runAction(
    'resume',
    () => resumeAttempt(attemptId.value, resumeMessage.value),
    'Đã cho phép tiếp tục.',
    'Khôi phục thất bại.',
    'resume'
  )
}

async function confirmStop() {
  showStopDialog.value = false
  await runAction(
    'invalidate',
    () => invalidateAttempt(attemptId.value, stopReason.value),
    'Đã buộc nộp bài.',
    'Buộc nộp bài thất bại.',
    'invalidate'
  )
}

// ── Watchers ────────────────────────────────────────────────────────
watch(attemptId, (id, prev) => {
  if (prev && prev !== id) unsubscribeFromAttempt(prev)
  if (id) {
    loading.value = true
    void loadData()
    subscribeToAttempt(id, handleRealtimeUpdate)
  }
}, { immediate: true })

// Timeline filter is applied locally — no extra API call needed
watch(timelineFilter, () => {
  timelinePage.value = 1
})

watch(timelinePageCount, count => {
  if (timelinePage.value > count) {
    timelinePage.value = count
  }
})

onUnmounted(() => {
  if (attemptId.value) unsubscribeFromAttempt(attemptId.value)
})

onActivated(() => {
  // Reload data when navigating back to this page
  loading.value = true
  void loadData()
})
</script>

<style scoped>
/* ── Root ─────────────────────────────────────────────────────── */
.smd-root {
  min-height: 100vh;
  padding: var(--ds-space-5);
  background: var(--ds-bg);
  color: var(--ds-text);
  font-family: var(--ds-font);
}

/* ── Loading & Error ─────────────────────────────────────────── */
.smd-loading,
.smd-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--ds-space-3);
  min-height: calc(100vh - 80px);
  color: var(--ds-text-secondary);
  text-align: center;
}

.smd-error {
  color: var(--ds-danger);
}

.smd-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--ds-gray-200);
  border-top-color: var(--ds-primary);
  border-radius: 50%;
  animation: smd-spin 0.8s linear infinite;
}

@keyframes smd-spin { to { transform: rotate(360deg); } }

/* ── Header ─────────────────────────────────────────────────── */
.smd-header {
  display: flex;
  align-items: center;
  gap: var(--ds-space-4);
  padding: var(--ds-space-4);
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  margin-bottom: var(--ds-space-4);
  flex-wrap: wrap;
}

.smd-back,
.smd-icon-btn {
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
  transition: all var(--ds-duration-base);
}

.smd-back:hover,
.smd-icon-btn:hover:not(:disabled) {
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
}

.smd-icon-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.smd-header__main {
  flex: 1;
  min-width: 0;
}

.smd-header__meta {
  display: flex;
  align-items: center;
  gap: var(--ds-space-2);
  margin-bottom: 2px;
}

.smd-label {
  font-size: var(--ds-text-xs);
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--ds-primary);
}

.smd-status-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 9999px;
  font-size: var(--ds-text-xs);
  font-weight: 700;
}

.smd-status-badge--online {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.smd-status-badge--submitted {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
}

.smd-status-badge--paused {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.smd-status-badge--stopped,
.smd-status-badge--offline {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.smd-title {
  margin: 0;
  font-size: var(--ds-text-2xl);
  font-weight: 800;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.smd-subtitle {
  margin: 2px 0 0;
  font-size: var(--ds-text-sm);
  color: var(--ds-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.smd-header__risk {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.smd-risk-big {
  font-size: var(--ds-text-4xl);
  font-weight: 900;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.smd-risk-level {
  font-size: var(--ds-text-xs);
  font-weight: 700;
  color: var(--ds-text-secondary);
}

.smd-header__actions {
  display: flex;
  align-items: center;
  gap: var(--ds-space-2);
}

.smd-conn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: 9999px;
  font-size: var(--ds-text-xs);
  font-weight: 700;
  border: 1px solid;
}

.smd-conn--on {
  color: var(--ds-success);
  border-color: rgba(22, 163, 74, 0.2);
  background: var(--ds-success-soft);
}

.smd-conn--off {
  color: var(--ds-text-muted);
  border-color: var(--ds-border);
  background: var(--ds-surface-muted);
}

.smd-conn-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.smd-conn--on .smd-conn-dot {
  animation: smd-pulse 2s ease-in-out infinite;
}

@keyframes smd-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.smd-spin {
  animation: smd-spin 0.8s linear infinite;
}

/* ── Strip ───────────────────────────────────────────────────── */
.smd-strip {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ds-space-3);
  padding: var(--ds-space-3) var(--ds-space-4);
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  margin-bottom: var(--ds-space-3);
  font-size: var(--ds-text-sm);
}

.smd-strip-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 80px;
}

.smd-strip-label {
  font-size: var(--ds-text-xs);
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.smd-strip-val {
  font-size: var(--ds-text-sm);
  font-weight: 700;
  color: var(--ds-text);
}

.smd-strip-val--ok { color: var(--ds-success); }
.smd-strip-val--warn { color: var(--ds-warning); }

/* ── Actions ─────────────────────────────────────────────────── */
.smd-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ds-space-2);
  margin-bottom: var(--ds-space-4);
}

/* ── Buttons ────────────────────────────────────────────────── */
.smd-btn {
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
  transition: all var(--ds-duration-base);
  font-family: inherit;
}

.smd-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.smd-btn--primary {
  background: var(--ds-primary);
  color: #fff;
  border-color: var(--ds-primary);
}

.smd-btn--primary:hover:not(:disabled) {
  background: var(--ds-primary-hover);
}

.smd-btn--warn {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
  border-color: var(--ds-warning);
}

.smd-btn--warn:hover:not(:disabled) {
  background: rgba(217, 119, 6, 0.12);
}

.smd-btn--pause {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border-color: var(--ds-primary);
}

.smd-btn--pause:hover:not(:disabled) {
  background: rgba(79, 70, 229, 0.1);
}

.smd-btn--success {
  background: var(--ds-success-soft);
  color: var(--ds-success);
  border-color: var(--ds-success);
}

.smd-btn--success:hover:not(:disabled) {
  background: rgba(22, 163, 74, 0.12);
}

.smd-btn--danger {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
  border-color: var(--ds-danger);
}

.smd-btn--danger:hover:not(:disabled) {
  background: rgba(220, 38, 38, 0.12);
}

.smd-btn--ghost {
  background: var(--ds-surface);
  color: var(--ds-text);
  border-color: var(--ds-border);
}

.smd-btn--ghost:hover:not(:disabled) {
  background: var(--ds-gray-50);
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
}

/* ── Body layout ─────────────────────────────────────────────── */
.smd-body {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: var(--ds-space-4);
  align-items: start;
}

.smd-main {
  display: flex;
  flex-direction: column;
  gap: var(--ds-space-4);
}

.smd-sidebar {
  display: flex;
  flex-direction: column;
  gap: var(--ds-space-4);
  position: sticky;
  top: var(--ds-space-4);
}

/* ── Cards ──────────────────────────────────────────────────── */
.smd-card {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  padding: var(--ds-space-4);
}

.smd-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ds-space-3);
  margin-bottom: var(--ds-space-3);
}

.smd-card__title {
  margin: 0;
  font-size: var(--ds-text-lg);
  font-weight: 800;
}

.smd-card__head-right {
  display: flex;
  align-items: center;
  gap: var(--ds-space-2);
}

/* ── Level badge ─────────────────────────────────────────────── */
.smd-level-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: var(--ds-radius-sm);
  font-size: var(--ds-text-xs);
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.smd-level-badge--critical {
  background: var(--ds-risk-critical-soft);
  color: var(--ds-risk-critical);
}

.smd-level-badge--high-risk {
  background: var(--ds-risk-high-soft);
  color: var(--ds-risk-high);
}

.smd-level-badge--suspicious {
  background: var(--ds-risk-moderate-soft);
  color: var(--ds-risk-moderate);
}

.smd-level-badge--clean {
  background: var(--ds-success-soft);
  color: var(--ds-risk-clean);
}

/* ── Risk summary ───────────────────────────────────────────── */
.smd-risk-summary {
  display: flex;
  flex-direction: column;
  gap: var(--ds-space-3);
}

.smd-risk-big-wrap {
  display: flex;
  align-items: baseline;
  gap: var(--ds-space-1);
}

.smd-risk-max {
  font-size: var(--ds-text-lg);
  color: var(--ds-text-muted);
  font-weight: 600;
}

.smd-breakdown {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--ds-space-2);
}

.smd-breakdown-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--ds-space-2) var(--ds-space-3);
  background: var(--ds-gray-50);
  border-radius: var(--ds-radius-sm);
}

.smd-breakdown-label {
  font-size: var(--ds-text-sm);
  font-weight: 600;
  color: var(--ds-text-secondary);
}

.smd-breakdown-val {
  font-size: var(--ds-text-sm);
  font-weight: 800;
  color: var(--ds-text);
  font-variant-numeric: tabular-nums;
}

.smd-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ds-space-2);
}

.smd-reason-tag {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 9999px;
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
  font-size: var(--ds-text-xs);
  font-weight: 700;
}

/* ── Evidence ───────────────────────────────────────────────── */
.smd-evidence-list {
  display: flex;
  flex-direction: column;
  gap: var(--ds-space-2);
}

.smd-evidence-item {
  padding: var(--ds-space-3);
  background: var(--ds-gray-50);
  border-radius: var(--ds-radius-md);
}

.smd-evidence-item__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--ds-space-1);
}

.smd-evidence-item__type {
  font-size: var(--ds-text-sm);
  font-weight: 700;
  color: var(--ds-text);
}

.smd-evidence-item__time {
  font-size: var(--ds-text-xs);
  color: var(--ds-text-muted);
  font-variant-numeric: tabular-nums;
}

.smd-evidence-item__meta-keyvals {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ds-space-2);
  margin-top: var(--ds-space-2);
}

.smd-evidence-kv {
  font-size: var(--ds-text-xs);
  color: var(--ds-text-secondary);
  background: var(--ds-gray-100);
  padding: 2px 6px;
  border-radius: var(--ds-radius-xs);
}

.smd-evidence-kv strong {
  font-weight: 700;
  color: var(--ds-text);
}

/* ── Timeline ───────────────────────────────────────────────── */
.smd-timeline {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.smd-event {
  display: grid;
  grid-template-columns: 12px 1fr auto;
  gap: var(--ds-space-3);
  align-items: start;
  padding: var(--ds-space-3) 0;
  border-bottom: 1px solid var(--ds-border);
}

.smd-event:last-child {
  border-bottom: none;
}

.smd-event__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 4px;
  flex-shrink: 0;
}

.smd-event__body {
  min-width: 0;
}

.smd-event__type {
  display: block;
  font-size: var(--ds-text-sm);
  font-weight: 700;
  color: var(--ds-text);
  margin-bottom: 2px;
}

.smd-event__msg {
  margin: 0;
  font-size: var(--ds-text-xs);
  color: var(--ds-text-secondary);
  line-height: 1.5;
}

.smd-event__impact {
  display: inline-block;
  margin-top: 4px;
  padding: 1px 6px;
  border-radius: 9999px;
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
  font-size: 10px;
  font-weight: 700;
}

.smd-event__time {
  font-size: var(--ds-text-xs);
  color: var(--ds-text-muted);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

/* ── Pagination ─────────────────────────────────────────────── */
.smd-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--ds-space-3);
  margin-top: var(--ds-space-4);
  padding-top: var(--ds-space-4);
  border-top: 1px solid var(--ds-border);
}

.smd-pagination__info {
  font-size: var(--ds-text-sm);
  font-weight: 700;
  color: var(--ds-text-secondary);
}

/* ── Select ─────────────────────────────────────────────────── */
.smd-select {
  padding: 6px 28px 6px 10px;
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
}

/* ── Empty ───────────────────────────────────────────────────── */
.smd-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--ds-space-2);
  padding: var(--ds-space-6);
  color: var(--ds-text-muted);
  text-align: center;
}

.smd-empty--sm {
  padding: var(--ds-space-4);
}

.smd-empty p {
  margin: 0;
  font-size: var(--ds-text-sm);
}

/* ── Flag review ───────────────────────────────────────────── */
.smd-flag-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 9999px;
  font-size: var(--ds-text-xs);
  font-weight: 700;
}

.smd-flag-badge--đang-mở,
.smd-flag-badge--open {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.smd-flag-badge--đã-xác-nhận,
.smd-flag-badge--confirmed {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.smd-flag-review {
  display: flex;
  flex-direction: column;
  gap: var(--ds-space-3);
}

.smd-flag-review__action {
  margin: 0;
  font-size: var(--ds-text-sm);
  color: var(--ds-text-secondary);
  line-height: 1.5;
}

.smd-flag-review__btns {
  display: flex;
  gap: var(--ds-space-2);
}

/* ── Textarea ──────────────────────────────────────────────── */
.smd-textarea {
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
  outline: none;
  transition: border-color var(--ds-duration-base);
}

.smd-textarea:focus {
  border-color: var(--ds-primary);
}

/* ── Count ──────────────────────────────────────────────────── */
.smd-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 6px;
  border-radius: 9999px;
  background: var(--ds-gray-100);
  color: var(--ds-text-secondary);
  font-size: var(--ds-text-xs);
  font-weight: 800;
}

/* ── Notes ─────────────────────────────────────────────────── */
.smd-notes-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ds-space-2);
  margin-bottom: var(--ds-space-3);
}

.smd-notes {
  margin-top: var(--ds-space-3);
  display: flex;
  flex-direction: column;
  gap: var(--ds-space-2);
}

.smd-note-item {
  padding: var(--ds-space-3);
  background: var(--ds-gray-50);
  border-radius: var(--ds-radius-md);
  border-left: 3px solid var(--ds-primary);
}

.smd-note-item__body {
  margin: 0 0 var(--ds-space-1);
  font-size: var(--ds-text-sm);
  color: var(--ds-text);
  line-height: 1.5;
}

.smd-note-item__time {
  font-size: var(--ds-text-xs);
  color: var(--ds-text-muted);
  font-variant-numeric: tabular-nums;
}

/* ── Responsive ─────────────────────────────────────────────── */
@media (max-width: 1024px) {
  .smd-body {
    grid-template-columns: 1fr;
  }

  .smd-sidebar {
    position: static;
  }
}

@media (max-width: 768px) {
  .smd-root {
    padding: var(--ds-space-3);
  }

  .smd-header {
    padding: var(--ds-space-3);
    gap: var(--ds-space-3);
  }

  .smd-header__risk {
    display: none;
  }

  .smd-strip {
    padding: var(--ds-space-3);
  }

  .smd-actions {
    flex-direction: column;
  }

  .smd-btn {
    width: 100%;
  }
}

/* ── Reduced motion ─────────────────────────────────────────── */
@media (prefers-reduced-motion: reduce) {
  .smd-spin,
  .smd-conn-dot {
    animation: none;
  }

  .smd-btn,
  .smd-back,
  .smd-icon-btn,
  .smd-textarea {
    transition: none;
  }
}
</style>
