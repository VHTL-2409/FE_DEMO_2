<template>
  <div class="lm-wrapper">
    <!-- Control Center Layout -->
    <ControlCenterLayout>
      <!-- Header slot: MonitoringHeader -->
      <template #header>
        <MonitoringHeader
          :exam-title="selectedExamTitle"
          :class-info="selectedExamMeta"
          :online-count="onlineCount"
          :alert-count="alertCount"
          :issue-count="offlineCount"
          :total-count="store.cards.length"
          :is-connected="isSocketConnected"
          :connection-mode="connectionMode"
          :loading="isSyncing"
          :all-panels-collapsed="allPanelsCollapsed"
          @back="handleBack"
          @connection-mode="toggleConnectionMode"
          @refresh="handleRefresh"
          @toggle-alerts="allPanelsCollapsed = !allPanelsCollapsed"
          @toggle-all-panels="handleToggleAllPanels"
        />
      </template>

      <!-- Filters slot -->
      <template #filters>
        <MonitoringFilters />
      </template>

      <!-- Main content slot -->
      <template #content>
        <div class="lm__table-view">
          <StudentMonitorTable
            :students="store.visibleCards"
            v-model:sort-by="tableSortBy"
            :loading="isSyncing && !store.cards.length"
            :exam-id="examId ? String(examId) : ''"
            @warn="openWarningModal"
            @pause="openPauseModal"
            @view-detail="openStudentDetail"
            @batch-warn="handleBatchWarn"
            @batch-pause="handleBatchPause"
            @batch-invalidate="handleBatchInvalidate"
          />
        </div>
      </template>

      <!-- Sidebar slot: ActivityFeed -->
      <template #sidebar>
        <div v-if="!allPanelsCollapsed">
          <ActivityFeed
            :alerts="liveAlerts"
            :events="recentEvents"
            :loading="isSyncing"
            :total-alert-count="liveAlerts.length"
            :max-events="30"
            @alert-click="handleAlertClick"
            @dismiss-alert="handleDismissAlert"
            @mark-all-read="handleMarkAllRead"
            @clear-events="handleClearEvents"
          />
        </div>
      </template>

      <!-- Footer slot: connection status only -->
      <template #footer>
        <div class="lm__footer">
          <div class="lm__footer-right">
            <span class="lm__footer-conn" :class="{ 'lm__footer-conn--live': isSocketConnected }">
              <LucideIcon :name="isSocketConnected ? 'wifi' : 'sync'" />
              {{ isSocketConnected ? 'Đã kết nối thời gian thực' : `Cập nhật định kỳ (cập nhật ${lastUpdatedLabel})` }}
            </span>
            <span v-if="wsError" class="lm__footer-ws-error" @click="retryConnection">
              <LucideIcon name="alert_circle" size="14" />
              Lỗi kết nối — {{ wsError }}
              <button class="lm__retry-btn" @click.stop="retryConnection">Thử lại</button>
            </span>
          </div>
        </div>
      </template>
    </ControlCenterLayout>

    <!-- Action Confirm Modal (reused for warn/pause/invalidate) -->
    <ActionConfirmModal
      v-model="showActionModal"
      :variant="actionModalConfig.variant"
      :title="actionModalConfig.title"
      :action-label="actionModalConfig.actionLabel"
      :icon="actionModalConfig.icon"
      :placeholder="actionModalConfig.placeholder"
      :student-name="actionTarget?.userName || actionTarget?.name || '—'"
      :loading="actionLoading"
      @confirm="handleActionConfirm"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { ApiError } from '../../services/apiClient'
import { listExamAttempts, getAttemptDetail } from '../../services/attemptService'
import { invalidateAttempt, sendTeacherWarning } from '../../services/monitoringService'
import { useToast } from '../../composables/useToast'
import { useRealtimeChannel } from '../../composables/useRealtimeChannel'
import { writeMonitoringSessionQuery } from '../../services/monitoringContextStorage'
import { useProctorDashboardStore } from '../../stores/proctorDashboardStore'

// Components
import ControlCenterLayout from './monitoring/ControlCenterLayout.vue'
import MonitoringHeader from './monitoring/MonitoringHeader.vue'
import MonitoringFilters from './monitoring/MonitoringFilters.vue'
import StudentMonitorTable from './monitoring/StudentMonitorTable.vue'
import ActivityFeed from './monitoring/ActivityFeed.vue'
import ActionConfirmModal from './monitoring/ActionConfirmModal.vue'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const store = useProctorDashboardStore()

// Store refs
const { cards: attempts, detailsByAttemptId, connectionMode, lastUpdatedAt, filters } = storeToRefs(store)

// Local table sort preference (not in store)
const tableSortBy = ref('risk')

// ── Route / Exam data ───────────────────────────────────────────────────────────
const examId = computed(() => {
  const raw = route.query.examId
  return raw ? Number.parseInt(String(raw), 10) : null
})

const selectedExamTitle = computed(() => route.query.title || 'Giám sát trực tiếp')
const selectedExamMeta = computed(() => route.query.meta || '')

// ── State ──────────────────────────────────────────────────────────────────────
const isSyncing = ref(false)
const actionLoading = ref(false)
const allPanelsCollapsed = ref(false)
let refreshTimer = null
let reconnectTimer = null

// ── Realtime channel ───────────────────────────────────────────────────────────
const { isConnected: isSocketConnected, lastError: wsError, connect, disconnect } = useRealtimeChannel()

// ── Alerts & Events ────────────────────────────────────────────────────────────
const liveAlerts = ref([])
const recentEvents = ref([])
let alertIdCounter = 0

// ── Action modal ────────────────────────────────────────────────────────────────
const showActionModal = ref(false)
const actionTarget = ref(null)
const actionModalConfig = ref({
  variant: 'warning',
  title: '',
  actionLabel: '',
  icon: 'warning',
  placeholder: ''
})

const ACTION_CONFIGS = {
  warn: {
    variant: 'warning',
    title: 'Gửi cảnh báo',
    actionLabel: 'Gửi cảnh báo',
    icon: 'send',
    placeholder: 'Để trống sẽ dùng mặc định: Bạn đang bị cảnh báo bởi giám thị. Vui lòng tuân thủ quy định phòng thi.'
  },
  pause: {
    variant: 'warning',
    title: 'Tạm dừng bài thi',
    actionLabel: 'Tạm dừng',
    icon: 'pause',
    placeholder: 'Nhập lý do tạm dừng...'
  },
  invalidate: {
    variant: 'danger',
    title: 'Đình chỉ bài thi',
    actionLabel: 'Xác nhận đình chỉ',
    icon: 'block',
    placeholder: 'Để trống sẽ dùng mặc định: Bài thi đã bị đình chỉ bởi giám thị.'
  }
}

let currentActionType = ''

// ── Computed: stats ────────────────────────────────────────────────────────────
const onlineCount = computed(() =>
  attempts.value.filter(a => {
    const s = String(a.status || '').toUpperCase()
    return s === 'ACTIVE' || s === 'IN_PROGRESS'
  }).length
)

const alertCount = computed(() =>
  attempts.value.filter(a => Number(a.riskScore || 0) > 30).length
)

const offlineCount = computed(() =>
  Math.max(0, attempts.value.length - onlineCount.value)
)

const lastUpdatedLabel = computed(() => {
  if (!lastUpdatedAt.value) return 'chưa có dữ liệu'
  return new Date(lastUpdatedAt.value).toLocaleTimeString('vi-VN', {
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
})

// ── Alert handlers from WebSocket message ───────────────────────────────────────
function handleTeacherAlert(alert) {
  if (!alert || !alert.attemptId) return

  const now = Date.now()

  switch (alert.type) {
    case 'RISK_UPDATED':
      addAlert({
        id: ++alertIdCounter,
        attemptId: alert.attemptId,
        studentName: alert.studentName || alert.student?.name || '—',
        riskScore: alert.riskScore ?? 0,
        riskLevel: alert.riskLevel || 'UNKNOWN',
        severity: alert.riskScore >= 80 ? 'HIGH' : alert.riskScore >= 30 ? 'MEDIUM' : 'LOW',
        message: alert.message || `Rủi ro: ${alert.riskScore ?? 0}%`,
        action: alert.actionTaken || null,
        timestamp: new Date().toISOString(),
        read: false
      })
      addEvent({ type: 'ALERT', message: alert.message || `Rủi ro thay đổi → ${alert.riskScore ?? 0}%`, studentName: alert.studentName || '' })
      break

    case 'SUSPICIOUS':
      addAlert({
        id: ++alertIdCounter,
        attemptId: alert.attemptId,
        studentName: alert.studentName || '—',
        riskScore: alert.riskScore ?? 0,
        riskLevel: alert.riskLevel || 'SUSPICIOUS',
        severity: 'MEDIUM',
        message: alert.message || 'Phát hiện hành vi đáng ngờ',
        timestamp: new Date().toISOString(),
        read: false
      })
      addEvent({ type: 'ALERT', message: alert.message || 'Hành vi đáng ngờ', studentName: alert.studentName || '' })
      break

    case 'WARNING_SENT':
      addEvent({ type: 'WARN', message: `GV gửi cảnh báo: ${alert.message || ''}`, studentName: alert.studentName || '' })
      break

    case 'ATTEMPT_STOPPED':
      addEvent({ type: 'INVALIDATE', message: alert.message || 'Bài thi bị đình chỉ', studentName: alert.studentName || '' })
      break

    case 'ATTEMPT_PAUSED':
      addEvent({ type: 'PAUSE', message: alert.message || 'Bài thi bị tạm dừng', studentName: alert.studentName || '' })
      break

    case 'ATTEMPT_RESUMED':
      addEvent({ type: 'RESUME', message: alert.message || 'Bài thi được khôi phục', studentName: alert.studentName || '' })
      break

    default:
      addAlert({
        id: ++alertIdCounter,
        attemptId: alert.attemptId,
        studentName: alert.studentName || '—',
        riskScore: alert.riskScore ?? 0,
        riskLevel: alert.riskLevel || 'UNKNOWN',
        severity: 'LOW',
        message: alert.message || alert.type || 'Thông báo',
        timestamp: new Date().toISOString(),
        read: false
      })
      addEvent({ type: 'ALERT', message: alert.message || alert.type, studentName: alert.studentName || '' })
  }
}

function addAlert(alert) {
  const maxAlerts = 50
  liveAlerts.value = [alert, ...liveAlerts.value].slice(0, maxAlerts)
}

// ── Load data ──────────────────────────────────────────────────────────────────
const loadAttempts = async (silent = false) => {
  if (!examId.value) return
  if (!silent) isSyncing.value = true
  try {
    const fetchedAttempts = await listExamAttempts(examId.value)
    const detailPairs = await Promise.all(
      fetchedAttempts.map(async (attempt) => {
        try {
          const detail = await getAttemptDetail(attempt.id)
          return [attempt.id, detail]
        } catch {
          return [attempt.id, null]
        }
      })
    )

    const newDataStr = JSON.stringify(fetchedAttempts.map(a => ({ id: a.id, status: a.status, riskScore: a.riskScore })))
    const oldDataStr = JSON.stringify(attempts.value.map(a => ({ id: a.id, status: a.status, riskScore: a.riskScore })))

    if (newDataStr !== oldDataStr) {
      store.setCards(fetchedAttempts)
      for (const [id, detail] of detailPairs) {
        store.setDetail(id, detail)
      }

      if (!silent) {
        addEvent({ type: 'REFRESH', message: 'Dữ liệu được cập nhật', studentName: '' })
      }
    }
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể tải dữ liệu giám sát.')
  } finally {
    if (!silent) isSyncing.value = false
  }
}

watch(
  () => (route.path === '/teacher/live-monitoring/session' ? { ...route.query } : null),
  (q) => {
    if (q?.examId) writeMonitoringSessionQuery(q)
  },
  { deep: true, immediate: true }
)

// Đổi kỳ thi: tải lại + reconnect WebSocket theo examId mới
watch(examId, (newId, oldId) => {
  if (!newId || newId === oldId) return
  void loadAttempts()
  disconnectRealtime()
  if (connectionMode.value === 'realtime') {
    void connectRealtime()
  }
})

// ── Realtime ───────────────────────────────────────────────────────────────────
const connectRealtime = async () => {
  if (!examId.value) return
  disconnectRealtime()

  await connect({
    topics: [
      {
        destination: `/topic/exams/${examId.value}/alerts`,
        handler: (body) => {
          handleTeacherAlert(body)
          void loadAttempts(true)
        }
      }
    ],
    reconnectDelay: 3000,
    onConnect: () => {
      isSocketConnected.value = true
      wsError.value = ''
      store.setConnectionMode('realtime')
    },
    onDisconnect: (event) => {
      isSocketConnected.value = false
      if (connectionMode.value === 'realtime') {
        scheduleReconnect()
      }
    },
    onError: (frame) => {
      isSocketConnected.value = false
      wsError.value = 'Kết nối bị lỗi'
      toast.error('Mất kết nối thời gian thực. Đã chuyển sang chế độ cập nhật định kỳ.')
    }
  })
}

const disconnectRealtime = () => {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  disconnect()
}

const scheduleReconnect = () => {
  if (reconnectTimer) return
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    if (connectionMode.value === 'realtime' && examId.value) {
      void connectRealtime()
    }
  }, 5000)
}

const retryConnection = () => {
  wsError.value = ''
  void connectRealtime()
}

const toggleConnectionMode = (mode) => {
  store.setConnectionMode(mode)
  if (mode === 'realtime') {
    void connectRealtime()
  } else {
    disconnectRealtime()
  }
}

// ── Events & Alerts ────────────────────────────────────────────────────────────
let lastEventType = ''
let lastEventTime = 0

const addEvent = (event) => {
  const now = Date.now()
  if (event.type === lastEventType && now - lastEventTime < 2000) {
    return
  }
  const evt = {
    ...event,
    id: now + Math.random(),
    timestamp: new Date().toISOString()
  }
  lastEventType = event.type
  lastEventTime = now
  recentEvents.value = [evt, ...recentEvents.value].slice(0, 30)
}

const handleAlertClick = (alert) => {
  const student = store.visibleCards.find(s => s.id === alert.attemptId || s.attemptId === alert.attemptId)
  if (student) {
    openStudentDetail({
      ...student,
      attemptId: student.attemptId || student.id,
      userName: student.student || student.name || student.userName || '—'
    })
  }
}

const handleDismissAlert = (alertId) => {
  liveAlerts.value = liveAlerts.value.filter(a => a.id !== alertId)
}

const handleMarkAllRead = () => {
  liveAlerts.value = liveAlerts.value.map(a => ({ ...a, read: true }))
}

const handleClearEvents = () => {
  recentEvents.value = []
}

// ── Actions ───────────────────────────────────────────────────────────────────
const openWarningModal = (student) => {
  actionTarget.value = student
  currentActionType = 'warn'
  actionModalConfig.value = ACTION_CONFIGS.warn
  showActionModal.value = true
}

const openPauseModal = (student) => {
  actionTarget.value = student
  currentActionType = 'pause'
  actionModalConfig.value = ACTION_CONFIGS.pause
  showActionModal.value = true
}

const openInvalidateModal = (student) => {
  actionTarget.value = student
  currentActionType = 'invalidate'
  actionModalConfig.value = ACTION_CONFIGS.invalidate
  showActionModal.value = true
}

const handleActionConfirm = async (reason) => {
  if (!actionTarget.value) return
  actionLoading.value = true
  try {
    if (currentActionType === 'warn') {
      await sendTeacherWarning(actionTarget.value.attemptId, reason)
      toast.success(`Đã gửi cảnh báo tới ${actionTarget.value.userName}.`)
      addEvent({ type: 'WARN', studentName: actionTarget.value.userName })
    } else if (currentActionType === 'pause') {
      await invalidateAttempt(actionTarget.value.attemptId, reason)
      toast.success(`Đã tạm dừng bài thi của ${actionTarget.value.userName}.`)
      addEvent({ type: 'PAUSE', studentName: actionTarget.value.userName })
    } else if (currentActionType === 'invalidate') {
      await invalidateAttempt(actionTarget.value.attemptId, reason)
      toast.success(`Đã đình chỉ bài thi của ${actionTarget.value.userName}.`)
      addEvent({ type: 'INVALIDATE', studentName: actionTarget.value.userName })
    }
    showActionModal.value = false
    actionTarget.value = null
    await loadAttempts()
  } catch (err) {
    const msg = currentActionType === 'warn'
      ? 'Không gửi được cảnh báo.'
      : currentActionType === 'pause'
        ? 'Không thể tạm dừng bài thi.'
        : 'Không thể đình chỉ bài thi.'
    toast.error(err instanceof ApiError ? err.message : msg)
  } finally {
    actionLoading.value = false
  }
}

const openStudentDetail = (student) => {
  router.push({
    path: '/teacher/live-monitoring/student-detail',
    query: {
      attemptId: student.attemptId || student.id,
      examId: examId.value ? String(examId.value) : '',
      student: student.userName || student.fullName || student.email || '—',
      studentId: student.id || student.studentId || ''
    }
  })
}

const handleBack = () => {
  router.push('/teacher/live-monitoring')
}

const handleRefresh = () => {
  void loadAttempts()
}

const handleToggleAllPanels = () => {
  allPanelsCollapsed.value = !allPanelsCollapsed.value
}

const handleBatchWarn = async (ids) => {
  if (ids.length === 0) return
  actionLoading.value = true
  try {
    await Promise.allSettled(ids.map(id => sendTeacherWarning(id, '')))
    toast.success(`Đã gửi cảnh báo tới ${ids.length} học sinh.`)
    addEvent({ type: 'WARN', message: `Cảnh báo hàng loạt ${ids.length} HS`, studentName: '' })
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không gửi được cảnh báo hàng loạt.')
  } finally {
    actionLoading.value = false
  }
}

const handleBatchPause = async (ids) => {
  if (ids.length === 0) return
  actionLoading.value = true
  try {
    await Promise.allSettled(ids.map(id => invalidateAttempt(id, 'Tạm dừng hàng loạt')))
    toast.success(`Đã tạm dừng ${ids.length} bài thi.`)
    addEvent({ type: 'PAUSE', message: `Tạm dừng hàng loạt ${ids.length} HS`, studentName: '' })
    await loadAttempts()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể tạm dừng hàng loạt.')
  } finally {
    actionLoading.value = false
  }
}

const handleBatchInvalidate = async (ids) => {
  if (ids.length === 0) return
  actionLoading.value = true
  try {
    await Promise.allSettled(ids.map(id => invalidateAttempt(id, 'Đình chỉ hàng loạt')))
    toast.success(`Đã đình chỉ ${ids.length} bài thi.`)
    addEvent({ type: 'INVALIDATE', message: `Đình chỉ hàng loạt ${ids.length} HS`, studentName: '' })
    await loadAttempts()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể đình chỉ hàng loạt.')
  } finally {
    actionLoading.value = false
  }
}

// ── Lifecycle ──────────────────────────────────────────────────────────────────
onMounted(async () => {
  await loadAttempts()
  if (connectionMode.value === 'realtime') {
    await connectRealtime()
  }
  // Poll only when WebSocket is disconnected, with 5s interval
  refreshTimer = window.setInterval(() => {
    if (!isSocketConnected.value) {
      void loadAttempts(true)
    }
  }, 5000)
})

onUnmounted(() => {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
  }
  disconnectRealtime()
})
</script>

<style scoped>
.lm-wrapper {
  min-height: 100vh;
  background: var(--ds-bg);
}

/* Footer */
.lm__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 1rem;
  padding: 0.75rem 1.5rem;
  flex-wrap: wrap;
}

.lm__footer-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.lm__footer-conn {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}

.lm__footer-conn--live { color: var(--ds-success); }

.lm__footer-ws-error {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-danger);
  cursor: pointer;
}

.lm__retry-btn {
  margin-left: 0.25rem;
  padding: 0.125rem 0.5rem;
  border-radius: var(--ds-radius-full);
  border: 1px solid var(--ds-danger);
  background: transparent;
  color: var(--ds-danger);
  font-size: 0.7rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.lm__retry-btn:hover {
  background: var(--ds-danger);
  color: white;
}

.lm__spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}