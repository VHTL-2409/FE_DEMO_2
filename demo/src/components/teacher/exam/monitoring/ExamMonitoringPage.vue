<template>
  <main class="mp-page">
    <section
      v-if="loading"
      class="mp-state"
    >
      <div class="mp-spinner" />
      <p>Đang tải phòng thi...</p>
    </section>

    <section
      v-else
      class="mp-shell"
    >
      <header class="mp-header">
        <RouterLink
          to="/teacher/live-monitoring"
          class="mp-back"
          aria-label="Quay lại danh sách kỳ thi"
        >
          <LucideIcon
            name="arrow-left"
            :size="18"
          />
        </RouterLink>

        <div class="mp-heading">
          <span>Phòng giám sát</span>
          <h1>{{ examTitle }}</h1>
          <p v-if="examCode">
            {{ examCode }}
          </p>
          <p v-else class="mp-subtle">
            Đồng bộ lần cuối {{ lastUpdatedLabel }}
          </p>
        </div>

        <div class="mp-header-actions">
          <span :class="['mp-conn', isConnected ? 'on' : 'off']">
            <i />{{ isConnected ? 'Realtime' : 'Tự cập nhật' }}
          </span>
          <button
            class="mp-icon"
            type="button"
            :disabled="isRefreshing"
            aria-label="Làm mới dữ liệu"
            @click="refresh"
          >
            <LucideIcon
              name="refresh-cw"
              :size="17"
              :class="{ 'mp-spin': isRefreshing }"
            />
          </button>
        </div>
      </header>

      <section
        class="mp-toolbar"
        aria-label="Bộ lọc thí sinh"
      >
        <label class="mp-search">
          <LucideIcon
            name="search"
            :size="16"
          />
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Tìm thí sinh"
          >
        </label>

        <div class="mp-toolbar__meta">
          <div
            class="mp-filters"
            role="tablist"
            aria-label="Lọc trạng thái"
          >
            <button
              v-for="tab in filterTabs"
              :key="tab.value"
              type="button"
              role="tab"
              :aria-selected="activeFilter === tab.value"
              :class="{ active: activeFilter === tab.value }"
              @click="activeFilter = tab.value"
            >
              {{ tab.label }} <span>{{ tab.count }}</span>
            </button>
          </div>
          <div class="mp-inline-stats">
            <span>{{ enrichedCards.length }} thí sinh</span>
            <span>{{ openFlagCount }} flag mở</span>
            <span>Cập nhật {{ lastUpdatedLabel }}</span>
          </div>
          <label class="mp-sort">
            <span>Sắp xếp</span>
            <select v-model="sortBy">
              <option value="risk">Risk cao nhất</option>
              <option value="violations">Nhiều vi phạm</option>
              <option value="status">Trạng thái</option>
              <option value="name">Tên</option>
            </select>
          </label>
        </div>
      </section>

      <div class="mp-layout">
        <section
          class="mp-roster"
          aria-label="Danh sách thí sinh"
        >
          <div v-if="visibleCards.length" class="mp-roster__head">
            <span>Thí sinh</span>
            <span>Risk</span>
          </div>
          <div
            v-if="visibleCards.length === 0"
            class="mp-empty"
          >
            <LucideIcon
              name="users"
              :size="32"
            />
            <p>Không có thí sinh phù hợp</p>
          </div>

          <button
            v-for="card in visibleCards"
            :key="getCardId(card)"
            type="button"
            class="mp-student"
            :class="{ selected: selectedId === String(getCardId(card)) }"
            @click="selectCard(card)"
          >
            <span
              class="mp-student__avatar"
              :style="{ background: card._avatarBg, color: card._avatarColor }"
            >
              {{ getInitials(card) }}
            </span>

            <span class="mp-student__body">
              <strong>{{ card.student || 'Chưa có tên' }}</strong>
              <small>{{ card.email || card._statusLabel }}</small>
              <small class="mp-student__meta">
                {{ card._statusLabel }} · {{ card.violationCount || 0 }} tín hiệu
                <template v-if="card.activeFlagStatus"> · Flag {{ card.activeFlagStatus }}</template>
              </small>
            </span>

            <div class="mp-risk-box">
              <span
                class="mp-risk"
                :style="{ color: card._riskColor, background: card._riskBg }"
              >
                {{ Math.round(card.riskScore || 0) }}
              </span>
              <small>{{ card._riskBandLabel }}</small>
            </div>
          </button>
        </section>

        <aside
          class="mp-panel"
          aria-label="Thông tin xử lý nhanh"
        >
          <div
            v-if="!selectedCard"
            class="mp-empty mp-empty--panel"
          >
            <LucideIcon
              name="mouse-pointer-2"
              :size="30"
            />
            <p>Chọn một thí sinh để xử lý.</p>
          </div>

          <template v-else>
            <div class="mp-panel__head">
              <div>
                <h2>{{ selectedCard.student || 'Chưa có tên' }}</h2>
                <p>{{ selectedCard.email || 'Chưa có email' }}</p>
              </div>
              <button
                class="mp-icon"
                type="button"
                title="Mở chi tiết"
                aria-label="Mở chi tiết thí sinh"
                @click="openDetailPage"
              >
                <LucideIcon
                  name="external-link"
                  :size="16"
                />
              </button>
            </div>

            <div class="mp-summary">
              <strong :style="{ color: selectedCard._riskColor }">{{ Math.round(selectedCard.riskScore || 0) }}</strong>
              <span>{{ selectedCard._riskBandLabel }} · {{ selectedCard._statusLabel }}</span>
              <small>{{ selectedCard.answeredCount || 0 }}/{{ selectedCard.totalQuestions || 0 }} câu đã trả lời</small>
            </div>

            <dl class="mp-facts">
              <div>
                <dt>Vi phạm</dt>
                <dd>{{ selectedCard.violationCount || 0 }}</dd>
              </div>
              <div>
                <dt>Flag</dt>
                <dd>{{ selectedCard.activeFlagStatus || 'Không' }}</dd>
              </div>
              <div>
                <dt>Kết nối</dt>
                <dd>{{ selectedCard._statusLabel }}</dd>
              </div>
            </dl>

            <div
              v-if="selectedCard.reasons?.length"
              class="mp-reasons"
            >
              <span
                v-for="reason in selectedCard.reasons.slice(0, 3)"
                :key="reason"
              >
                {{ reason }}
              </span>
            </div>

            <div v-if="selectedCard.activeFlagStatus || selectedCard.latestFlagTitle" class="mp-flag">
              <strong>{{ selectedCard.activeFlagStatus || 'FLAG' }}</strong>
              <span>{{ selectedCard.latestFlagTitle || 'Đang chờ review' }}</span>
            </div>

            <div v-if="selectedEvidence.length" class="mp-evidence">
              <p>Chứng cứ gần nhất</p>
              <ul>
                <li
                  v-for="item in selectedEvidence.slice(0, 4)"
                  :key="item"
                >
                  {{ item }}
                </li>
              </ul>
            </div>

            <div v-if="selectedFlagOpen" class="mp-flag-review">
              <textarea
                v-model="reviewNote"
                class="mp-textarea"
                rows="2"
                placeholder="Ghi chú review nhanh..."
              />
              <div class="mp-flag-review__actions">
                <button
                  class="success"
                  type="button"
                  :disabled="actionLoading === 'flag-CONFIRMED'"
                  @click="reviewSelectedFlag('CONFIRMED')"
                >
                  Xác nhận flag
                </button>
                <button
                  class="mp-ghost"
                  type="button"
                  :disabled="actionLoading === 'flag-DISMISSED'"
                  @click="reviewSelectedFlag('DISMISSED')"
                >
                  Bỏ qua flag
                </button>
              </div>
            </div>

            <div class="mp-actions">
              <button
                class="warn"
                type="button"
                @click="openWarnDialog(selectedCard)"
              >
                <LucideIcon
                  name="send"
                  :size="15"
                />
                Gửi cảnh báo
              </button>
              <button
                v-if="selectedCard._isPaused"
                class="success"
                type="button"
                @click="resumeStudent(selectedCard)"
              >
                <LucideIcon
                  name="play"
                  :size="15"
                />
                Cho tiếp tục
              </button>
              <button
                v-else
                class="pause"
                type="button"
                :disabled="selectedCard._isTerminal"
                @click="pauseStudent(selectedCard)"
              >
                <LucideIcon
                  name="pause"
                  :size="15"
                />
                Tạm dừng
              </button>
              <button
                class="danger"
                type="button"
                :disabled="selectedCard._isTerminal"
                @click="openStopDialog(selectedCard)"
              >
                <LucideIcon
                  name="stop-circle"
                  :size="15"
                />
                Buộc nộp bài
              </button>
            </div>

            <div v-if="recentAlertFeed.length" class="mp-alerts">
              <div class="mp-alerts__head">
                <h3>Alert mới</h3>
                <span>{{ recentAlertFeed.length }}</span>
              </div>
              <div class="mp-alert-list">
                <article
                  v-for="alert in recentAlertFeed"
                  :key="alert.id"
                  class="mp-alert-item"
                >
                  <strong>{{ alert.signalType || alert.type || 'ALERT' }}</strong>
                  <p>{{ alert.message || 'Rủi ro vừa thay đổi' }}</p>
                </article>
              </div>
            </div>
          </template>
        </aside>
      </div>
    </section>

    <Dialog
      v-model:visible="showWarnDialog"
      header="Gửi cảnh báo"
      :modal="true"
      :closable="true"
      :style="{ width: '420px' }"
    >
      <textarea
        v-model="warningMessage"
        class="mp-textarea"
        rows="3"
        placeholder="Nội dung cảnh báo"
      />
      <template #footer>
        <button
          class="mp-dialog-btn"
          @click="showWarnDialog = false"
        >
          Hủy
        </button>
        <button
          class="mp-dialog-btn primary"
          :disabled="actionLoading === 'warning'"
          @click="confirmSendWarning"
        >
          Gửi cảnh báo
        </button>
      </template>
    </Dialog>

    <Dialog
      v-model:visible="showStopDialog"
      header="Xác nhận buộc nộp bài"
      :modal="true"
      :closable="true"
      :style="{ width: '420px' }"
    >
      <textarea
        v-model="stopReason"
        class="mp-textarea"
        rows="2"
        placeholder="Lý do"
      />
      <template #footer>
        <button
          class="mp-dialog-btn"
          @click="showStopDialog = false"
        >
          Hủy
        </button>
        <button
          class="mp-dialog-btn danger"
          :disabled="actionLoading === 'stop'"
          @click="confirmStop"
        >
          Xác nhận
        </button>
      </template>
    </Dialog>
  </main>
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
import { fetchExamSummary, fetchExamAttemptsFilter, fetchProctorSessionAlerts, reviewProctorFlag, sendTeacherWarning, pauseAttempt, resumeAttempt, invalidateAttempt } from '../../../../services/examMonitoringService'

const RISK_BAND_THRESHOLDS = { CRITICAL: 81, HIGH_RISK: 61, SUSPICIOUS: 31 }
const RISK_BAND_LABELS = { CRITICAL: 'Nguy cơ cao', HIGH_RISK: 'Rủi ro cao', SUSPICIOUS: 'Đáng ngờ', CLEAN: 'Bình thường' }
const RISK_COLORS = {
  CRITICAL: 'var(--mon-risk-critical)',
  HIGH_RISK: 'var(--mon-risk-high)',
  SUSPICIOUS: 'var(--mon-risk-moderate)',
  CLEAN: 'var(--mon-risk-clean)'
}
const RISK_BG_SOFT = {
  CRITICAL: 'rgba(220, 38, 38, 0.08)',
  HIGH_RISK: 'rgba(234, 88, 12, 0.08)',
  SUSPICIOUS: 'rgba(217, 119, 6, 0.08)',
  CLEAN: 'rgba(22, 163, 74, 0.08)'
}
const STATUS_META = {
  ONLINE: { label: 'Đang thi', color: 'var(--mon-primary)', bg: 'var(--mon-primary-soft)' },
  SUBMITTED: { label: 'Đã nộp', color: 'var(--mon-success)', bg: 'var(--mon-success-soft)' },
  PAUSED: { label: 'Tạm dừng', color: 'var(--mon-warning)', bg: 'var(--mon-warning-soft)' },
  STOPPED: { label: 'Đã dừng', color: 'var(--mon-danger)', bg: 'var(--mon-danger-soft)' },
  OFFLINE: { label: 'Offline', color: 'var(--mon-text-muted)', bg: 'var(--mon-surface-2)' }
}

const router = useRouter()
const route = useRoute()
const toast = useToast()
const store = useProctorDashboardStore()
const { cards, liveAlerts, lastUpdatedAt } = storeToRefs(store)
const { isConnected, connect, disconnect } = useExamMonitoring()

const examId = computed(() => route.params.examId)
const examTitle = ref('Đang tải...')
const examCode = ref('')
const loading = ref(true)
const isRefreshing = ref(false)
const activeFilter = ref('all')
const sortBy = ref('risk')
const selectedId = ref(null)
const searchQuery = ref('')
const showWarnDialog = ref(false)
const showStopDialog = ref(false)
const warningMessage = ref('')
const stopReason = ref('')
const reviewNote = ref('')
const actionLoading = ref('')
const dialogStudentName = ref('')
const dialogAttemptId = ref(null)
let refreshTimer = null
let searchTimer = null

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

  const riskColor = RISK_COLORS[riskBand]
  return {
    ...card,
    _riskBand: riskBand,
    _riskBandLabel: RISK_BAND_LABELS[riskBand],
    _riskColor: riskColor,
    _riskBg: RISK_BG_SOFT[riskBand],
    _statusToken: statusToken,
    _statusLabel: STATUS_META[statusToken].label,
    _statusColor: STATUS_META[statusToken].color,
    _statusBg: STATUS_META[statusToken].bg,
    _avatarBg: RISK_BG_SOFT[riskBand],
    _avatarColor: riskColor,
    _isPaused: statusToken === 'PAUSED',
    _isTerminal: statusToken === 'SUBMITTED' || statusToken === 'STOPPED'
  }
}))

const onlineCount = computed(() => enrichedCards.value.filter(c => c._statusToken === 'ONLINE').length)
const attentionCount = computed(() => enrichedCards.value.filter(c => c._riskBand !== 'CLEAN' || c.reviewRequired).length)
const openFlagCount = computed(() => enrichedCards.value.filter(c => String(c.activeFlagStatus || '').toUpperCase() === 'OPEN').length)
const selectedCard = computed(() => enrichedCards.value.find(c => String(getCardId(c)) === selectedId.value) || null)
const filterTabs = computed(() => [
  { label: 'Tất cả', value: 'all', count: enrichedCards.value.length },
  { label: 'Cần chú ý', value: 'attention', count: attentionCount.value },
  { label: 'Đang thi', value: 'online', count: onlineCount.value }
])
const visibleCards = computed(() => {
  let list = enrichedCards.value
  if (activeFilter.value === 'attention') list = list.filter(c => c._riskBand !== 'CLEAN' || c.reviewRequired)
  if (activeFilter.value === 'online') list = list.filter(c => c._statusToken === 'ONLINE')
  const q = searchQuery.value.trim().toLowerCase()
  if (q) list = list.filter(c => `${c.student || ''} ${c.email || ''}`.toLowerCase().includes(q))
  return [...list].sort((a, b) => {
    if (sortBy.value === 'name') {
      return String(a.student || '').localeCompare(String(b.student || ''))
    }
    if (sortBy.value === 'status') {
      return String(a._statusToken || '').localeCompare(String(b._statusToken || ''))
    }
    if (sortBy.value === 'violations') {
      return Number(b.violationCount || 0) - Number(a.violationCount || 0)
    }
    return Number(b.riskScore || 0) - Number(a.riskScore || 0)
  })
})

const recentAlertFeed = computed(() => liveAlerts.value
  .filter(alert => String(alert.examId || examId.value) === String(examId.value))
  .slice(0, 6))

const selectedEvidence = computed(() => selectedCard.value?.evidenceSummary || [])
const selectedFlagOpen = computed(() => String(selectedCard.value?.activeFlagStatus || '').toUpperCase() === 'OPEN')
const lastUpdatedLabel = computed(() => {
  if (!lastUpdatedAt.value) return 'Chưa đồng bộ'
  return new Date(lastUpdatedAt.value).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
})

function mergeAlertsIntoAttempts(attempts = [], alerts = []) {
  const alertsByAttempt = new Map((alerts || []).map(alert => [String(alert.attemptId), alert]))
  return (attempts || []).map((attempt) => {
    const alert = alertsByAttempt.get(String(getCardId(attempt)))
    if (!alert) return attempt
    return {
      ...attempt,
      riskScore: alert.riskScore ?? attempt.riskScore,
      riskLevel: alert.riskLevel ?? attempt.riskLevel,
      reviewRequired: alert.reviewRequired ?? attempt.reviewRequired,
      recommendedAction: alert.recommendedAction ?? attempt.recommendedAction,
      reasons: alert.reasons?.length ? alert.reasons : attempt.reasons,
      evidenceSummary: alert.evidenceSummary?.length ? alert.evidenceSummary : attempt.evidenceSummary,
      activeFlagId: alert.activeFlagId ?? attempt.activeFlagId,
      activeFlagStatus: alert.activeFlagStatus ?? attempt.activeFlagStatus,
      latestFlagTitle: alert.latestFlagTitle ?? attempt.latestFlagTitle
    }
  })
}

function getCardId(card) {
  return card?.id ?? card?.attemptId
}

function getInitials(card) {
  const name = card.student || card.name || '?'
  return (name.trim().split(/\s+/).at(-1)?.[0] || '?').toUpperCase()
}

function selectCard(card) {
  selectedId.value = String(getCardId(card))
}

function openDetailPage() {
  if (selectedCard.value) router.push(`/teacher/exams/${examId.value}/monitoring/student/${getCardId(selectedCard.value)}`)
}

async function loadData() {
  if (!examId.value) return
  loading.value = true
  try {
    const filters = {
      status: activeFilter.value === 'online' ? 'IN_PROGRESS' : '',
      riskBand: activeFilter.value === 'attention' ? 'SUSPICIOUS' : '',
      search: searchQuery.value.trim()
    }
    const [summary, attempts, alerts] = await Promise.all([
      fetchExamSummary(examId.value),
      fetchExamAttemptsFilter(examId.value, filters),
      fetchProctorSessionAlerts(examId.value).catch(() => [])
    ])
    examTitle.value = summary?.title || summary?.name || 'Kỳ thi'
    examCode.value = summary?.code || ''
    const mergedAttempts = mergeAlertsIntoAttempts(attempts || [], alerts || [])
    store.setCards(mergedAttempts)
    if (!selectedId.value && mergedAttempts?.length) selectedId.value = String(getCardId(mergedAttempts[0]))
  } catch (err) {
    toast.error(err?.message || 'Không thể tải dữ liệu giám sát.')
  } finally {
    loading.value = false
  }
}

async function refresh() {
  isRefreshing.value = true
  try { await loadData() } finally { isRefreshing.value = false }
}

function openWarnDialog(card) {
  dialogStudentName.value = card.student || 'Chưa có tên'
  dialogAttemptId.value = getCardId(card)
  warningMessage.value = ''
  showWarnDialog.value = true
}

function openStopDialog(card) {
  dialogStudentName.value = card.student || 'Chưa có tên'
  dialogAttemptId.value = getCardId(card)
  stopReason.value = ''
  showStopDialog.value = true
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

async function reviewSelectedFlag(status) {
  if (!selectedCard.value?.activeFlagId) return
  actionLoading.value = `flag-${status}`
  try {
    await reviewProctorFlag(selectedCard.value.activeFlagId, {
      status,
      teacherNote: reviewNote.value
    })
    toast.success(status === 'CONFIRMED' ? 'Đã xác nhận flag.' : 'Đã bỏ qua flag.')
    reviewNote.value = ''
    await refresh()
  } catch (err) {
    toast.error(err?.message || 'Cập nhật flag thất bại.')
  } finally {
    actionLoading.value = ''
  }
}

function pauseStudent(card) {
  dialogAttemptId.value = getCardId(card)
  void runAction('pause', pauseAttempt, 'Đã tạm dừng bài thi.', 'Tạm dừng thất bại.')
}

function resumeStudent(card) {
  dialogAttemptId.value = getCardId(card)
  void runAction('resume', resumeAttempt, 'Đã cho phép tiếp tục.', 'Khôi phục thất bại.')
}

async function confirmSendWarning() {
  showWarnDialog.value = false
  await runAction('warning', id => sendTeacherWarning(id, warningMessage.value), 'Đã gửi cảnh báo.', 'Gửi cảnh báo thất bại.')
}

async function confirmStop() {
  showStopDialog.value = false
  await runAction('stop', id => invalidateAttempt(id, stopReason.value), 'Đã buộc nộp bài.', 'Buộc nộp bài thất bại.')
}

watch(examId, id => {
  if (id) {
    selectedId.value = null
    store.reset()
    void loadData()
    connect(id)
  }
}, { immediate: true })

watch([activeFilter, searchQuery], () => {
  if (!examId.value) return
  if (searchTimer) window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    selectedId.value = null
    void loadData()
  }, 250)
})

onMounted(() => {
  refreshTimer = window.setInterval(() => {
    if (!document.hidden) void refresh()
  }, 30000)
})

onUnmounted(() => {
  if (refreshTimer) window.clearInterval(refreshTimer)
  if (searchTimer) window.clearTimeout(searchTimer)
  disconnect()
})
</script>

<style scoped>
.mp-page {
  min-height: 100vh;
  background: var(--ds-bg);
  color: var(--ds-text);
}

.mp-shell {
  padding: var(--ds-space-5);
}

.mp-header,
.mp-toolbar,
.mp-student,
.mp-panel,
.mp-empty {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  box-shadow: var(--ds-shadow-sm);
}

.mp-header {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: var(--ds-space-4);
  align-items: center;
  padding: var(--ds-space-4);
}

.mp-back,
.mp-icon {
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
}

.mp-back:hover,
.mp-icon:hover:not(:disabled) {
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.mp-icon:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.mp-heading {
  min-width: 0;
}

.mp-heading span {
  color: var(--ds-primary);
  font-size: var(--ds-text-xs);
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.mp-heading h1 {
  margin: var(--ds-space-1) 0 0;
  overflow: hidden;
  font-size: var(--ds-text-2xl);
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mp-heading p,
.mp-empty p,
.mp-panel p,
.mp-summary small,
.mp-student small {
  color: var(--ds-text-secondary);
}

.mp-subtle {
  opacity: 0.8;
}

.mp-heading p,
.mp-panel p {
  margin: var(--ds-space-1) 0 0;
  font-size: var(--ds-text-sm);
}

.mp-header-actions {
  display: flex;
  align-items: center;
  gap: var(--ds-space-2);
}

.mp-conn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 32px;
  padding: 0 var(--ds-space-3);
  border-radius: var(--ds-radius-full);
  font-size: var(--ds-text-xs);
  font-weight: 800;
}

.mp-conn i {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: currentColor;
}

.mp-conn.on {
  color: var(--ds-text);
  background: var(--ds-surface-muted);
}

.mp-conn.off {
  color: var(--ds-text-muted);
  background: var(--ds-surface-muted);
}

.mp-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ds-space-3);
  margin: var(--ds-space-4) 0;
  padding: var(--ds-space-3);
}

.mp-toolbar__meta {
  display: flex;
  align-items: center;
  gap: var(--ds-space-3);
  flex-wrap: wrap;
}

.mp-search {
  display: flex;
  align-items: center;
  gap: var(--ds-space-2);
  width: min(320px, 100%);
  min-height: 40px;
  padding: 0 var(--ds-space-3);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  color: var(--ds-text-secondary);
  background: var(--ds-surface-muted);
}

.mp-search input {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--ds-text);
  font: inherit;
}

.mp-filters {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ds-space-2);
}

.mp-filters button {
  min-height: 34px;
  padding: 0 var(--ds-space-3);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-full);
  color: var(--ds-text-secondary);
  background: var(--ds-surface);
  font: inherit;
  font-size: var(--ds-text-sm);
  font-weight: 700;
  cursor: pointer;
}

.mp-filters button.active,
.mp-filters button:hover {
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
}

.mp-filters span {
  margin-left: 4px;
  color: var(--ds-text-muted);
}

.mp-sort {
  display: flex;
  align-items: center;
  gap: var(--ds-space-2);
  color: var(--ds-text-secondary);
  font-size: var(--ds-text-sm);
}

.mp-sort select {
  min-height: 36px;
  padding: 0 var(--ds-space-3);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  color: var(--ds-text);
  background: var(--ds-surface);
  font: inherit;
}

.mp-inline-stats {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ds-space-2);
}

.mp-inline-stats span {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 var(--ds-space-3);
  border: 1px solid var(--ds-border);
  border-radius: 999px;
  color: var(--ds-text-secondary);
  background: var(--ds-surface-muted);
  font-size: var(--ds-text-xs);
  font-weight: 700;
}

.mp-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: var(--ds-space-4);
  align-items: start;
}

.mp-roster {
  display: grid;
  gap: var(--ds-space-2);
}

.mp-roster__head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--ds-space-3);
  padding: 0 var(--ds-space-3);
  color: var(--ds-text-muted);
  font-size: var(--ds-text-xs);
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.mp-student {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: var(--ds-space-3);
  align-items: center;
  width: 100%;
  padding: var(--ds-space-3);
  text-align: left;
  cursor: pointer;
}

.mp-student:hover,
.mp-student.selected {
  border-color: var(--ds-border-strong, var(--ds-border));
  background: var(--ds-surface-muted);
}

.mp-student__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: var(--ds-radius-md);
  font-weight: 900;
}

.mp-student__body {
  min-width: 0;
}

.mp-student strong,
.mp-student small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mp-student__meta {
  margin-top: 2px;
  font-size: 11px;
}

.mp-student strong {
  color: var(--ds-text);
}

.mp-risk-box {
  display: grid;
  justify-items: end;
  gap: 4px;
}

.mp-risk {
  min-width: 40px;
  padding: 5px 8px;
  border-radius: var(--ds-radius-md);
  text-align: center;
  font-weight: 900;
}

.mp-risk-box small {
  color: var(--ds-text-muted);
  font-size: 11px;
}

.mp-panel {
  position: sticky;
  top: var(--ds-space-4);
  padding: var(--ds-space-4);
}

.mp-panel__head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 40px;
  gap: var(--ds-space-3);
  align-items: start;
}

.mp-panel h2 {
  margin: 0;
  overflow: hidden;
  font-size: var(--ds-text-xl);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mp-summary {
  display: grid;
  gap: var(--ds-space-1);
  margin: var(--ds-space-4) 0;
  padding: var(--ds-space-4);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  background: var(--ds-surface-muted);
}

.mp-summary strong {
  font-size: 44px;
  line-height: 1;
}

.mp-summary span {
  font-weight: 800;
}

.mp-facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--ds-space-3);
  margin-bottom: var(--ds-space-4);
}

.mp-facts div {
  min-width: 0;
}

.mp-facts dt {
  margin: 0 0 4px;
  color: var(--ds-text-secondary);
  font-size: var(--ds-text-xs);
  text-transform: uppercase;
}

.mp-facts dd {
  margin: 0;
  color: var(--ds-text);
  font-size: var(--ds-text-base);
  font-weight: 800;
}

.mp-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ds-space-2);
  margin-bottom: var(--ds-space-4);
}

.mp-reasons span {
  padding: 6px 8px;
  border-radius: var(--ds-radius-full);
  color: var(--ds-text-secondary);
  background: var(--ds-surface-muted);
  font-size: var(--ds-text-xs);
  font-weight: 800;
}

.mp-flag {
  display: grid;
  gap: 4px;
  margin-bottom: var(--ds-space-4);
  padding: var(--ds-space-3);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  background: var(--ds-surface-muted);
}

.mp-flag strong {
  color: var(--ds-text-secondary);
  font-size: var(--ds-text-xs);
  letter-spacing: 0.08em;
}

.mp-flag span {
  color: var(--ds-text-secondary);
  font-size: var(--ds-text-sm);
}

.mp-evidence {
  margin-bottom: var(--ds-space-4);
  padding: var(--ds-space-3);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  background: var(--ds-surface-muted);
}

.mp-evidence p {
  margin: 0 0 var(--ds-space-2);
  color: var(--ds-text);
  font-weight: 800;
}

.mp-evidence ul {
  margin: 0;
  padding-left: 18px;
}

.mp-evidence li {
  color: var(--ds-text-secondary);
}

.mp-evidence li + li {
  margin-top: 6px;
}

.mp-flag-review {
  display: grid;
  gap: var(--ds-space-2);
  margin-bottom: var(--ds-space-4);
}

.mp-flag-review__actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--ds-space-2);
}

.mp-ghost {
  min-height: 40px;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  color: var(--ds-text-secondary);
  background: var(--ds-surface);
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.mp-actions {
  display: grid;
  gap: var(--ds-space-2);
}

.mp-actions button,
.mp-dialog-btn {
  min-height: 40px;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.mp-actions button:disabled,
.mp-dialog-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.mp-actions .warn {
  color: var(--ds-warning);
  background: var(--ds-warning-soft);
  border-color: var(--ds-warning);
}

.mp-actions .pause,
.mp-dialog-btn.primary {
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary);
}

.mp-actions .success {
  color: var(--ds-success);
  background: var(--ds-success-soft);
  border-color: var(--ds-success);
}

.mp-actions .danger,
.mp-dialog-btn.danger {
  color: var(--ds-danger);
  background: var(--ds-danger-soft);
  border-color: var(--ds-danger);
}

.mp-alerts {
  margin-top: var(--ds-space-4);
  padding-top: var(--ds-space-4);
  border-top: 1px solid var(--ds-border);
}

.mp-alerts__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ds-space-2);
  margin-bottom: var(--ds-space-2);
}

.mp-alerts__head h3 {
  margin: 0;
  font-size: var(--ds-text-base);
}

.mp-alerts__head span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  color: var(--ds-text-secondary);
  background: var(--ds-surface-muted);
  font-size: var(--ds-text-xs);
  font-weight: 800;
}

.mp-alert-list {
  display: grid;
  gap: var(--ds-space-2);
}

.mp-alert-item {
  padding: var(--ds-space-3);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  background: var(--ds-surface-muted);
}

.mp-alert-item strong {
  display: block;
  margin-bottom: 4px;
  font-size: var(--ds-text-sm);
}

.mp-alert-item p,
.mp-alerts__empty {
  margin: 0;
  color: var(--ds-text-secondary);
  font-size: var(--ds-text-sm);
}

.mp-empty,
.mp-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--ds-space-3);
  min-height: 220px;
  padding: var(--ds-space-6);
  text-align: center;
}

.mp-empty--panel {
  min-height: 280px;
  border-style: dashed;
  box-shadow: none;
}

.mp-state {
  min-height: 100vh;
}

.mp-spinner {
  width: 34px;
  height: 34px;
  border: 3px solid var(--ds-gray-200);
  border-top-color: var(--ds-primary);
  border-radius: 50%;
  animation: mp-spin 0.8s linear infinite;
}

.mp-dialog-text {
  margin: 0 0 var(--ds-space-3);
  color: var(--ds-text-secondary);
}

.mp-textarea {
  width: 100%;
  box-sizing: border-box;
  padding: var(--ds-space-3);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  color: var(--ds-text);
  resize: vertical;
}

.mp-spin {
  animation: mp-spin 0.8s linear infinite;
}

@keyframes mp-spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 980px) {
  .mp-layout {
    grid-template-columns: 1fr;
  }

  .mp-panel {
    position: static;
  }
}

@media (max-width: 700px) {
  .mp-shell {
    padding: var(--ds-space-3);
  }

  .mp-header,
  .mp-toolbar {
    align-items: stretch;
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .mp-toolbar__meta,
  .mp-header-actions,
  .mp-search {
    width: 100%;
  }

  .mp-toolbar__meta,
  .mp-facts,
  .mp-flag-review__actions {
    display: grid;
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .mp-spinner,
  .mp-spin {
    animation: none;
  }
}
</style>
