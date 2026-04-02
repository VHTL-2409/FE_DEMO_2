<template>
  <div class="lm-wrapper">
    <!-- Control Center Layout -->
    <ControlCenterLayout>
      <!-- Header slot: MonitoringHeader -->
      <template #header>
        <MonitoringHeader
          :exam-title="selectedExamTitle"
          :class-info="selectedExamMeta"
          :rooms-count="examCount"
          :online-count="onlineCount"
          :alert-count="alertCount"
          :issue-count="offlineCount"
          :total-count="attempts.length"
          :is-connected="isSocketConnected"
          :connection-mode="connectionMode"
          :loading="isSyncing"
          :all-panels-collapsed="allPanelsCollapsed"
          @back="handleBack"
          @connection-mode="toggleConnectionMode"
          @refresh="handleRefresh"
          @toggle-alerts="alertPanelOpen = !alertPanelOpen"
          @toggle-all-panels="handleToggleAllPanels"
        />
      </template>

      <!-- Filters slot -->
      <template #filters>
        <MonitoringFilters
          v-model:activeTab="activeTab"
          v-model:search-query="searchQuery"
          v-model:risk-band="riskBand"
          v-model:status="statusFilter"
          v-model:time-range="timeRange"
          v-model:view-mode="viewMode"
          :stats="filterStats"
        />
      </template>

      <!-- Main content slot -->
      <template #content>
        <!-- GRID VIEW: Room cards -->
        <div v-if="viewMode === 'grid'" class="lm__grid-view">
          <div v-if="roomsLoading" class="lm__grid-loading">
            <div v-for="i in 3" :key="i" class="lm__grid-skeleton">
              <div class="lm__skeleton lm__skeleton--stripe" />
              <div class="lm__skeleton lm__skeleton--content" />
              <div class="lm__skeleton lm__skeleton--stats" />
              <div class="lm__skeleton lm__skeleton--actions" />
            </div>
          </div>

          <div v-else-if="filteredRooms.length === 0" class="lm__empty-grid">
            <LucideIcon name="search_off" />
            <p>Không tìm thấy phòng thi nào</p>
            <button type="button" @click="resetFilters">Xóa bộ lọc</button>
          </div>

          <div v-else class="lm__rooms-grid">
            <RoomStatusCard
              v-for="room in filteredRooms"
              :key="room.id"
              :exam="room"
              @view-room="openRoomView(room)"
              @manage="openRoomView(room)"
              @pause="handlePauseRoom(room)"
            />
          </div>
        </div>

        <!-- TABLE VIEW: Student table -->
        <div v-else class="lm__table-view">
          <StudentMonitorTable
            :students="tableStudents"
            :loading="isSyncing && !attempts.length"
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

      <!-- Sidebar slot: AlertPanel + LiveEventFeed -->
      <template #sidebar>
        <div v-if="!allPanelsCollapsed">
          <AlertPanel
            v-model="alertPanelOpen"
            :alerts="alerts"
            :loading="isSyncing"
            :total-count="alerts.length"
            @alert-click="handleAlertClick"
            @dismiss="handleDismissAlert"
            @mark-all-read="handleMarkAllRead"
          />

          <LiveEventFeed
            :events="recentEvents"
            :loading="isSyncing"
            :connected="isSocketConnected"
            @clear="handleClearEvents"
          />
        </div>
      </template>

      <!-- Footer slot: batch info + connection status -->
      <template #footer>
        <div class="lm__footer">
          <div class="lm__footer-left">
            <span class="lm__footer-stat">
              <LucideIcon name="group" />
              <strong>{{ attempts.length }}</strong> thí sinh
            </span>
            <span class="lm__footer-stat lm__footer-stat--online">
              <LucideIcon name="radio_button_checked" />
              <strong>{{ onlineCount }}</strong> Trực tuyến
            </span>
            <span v-if="alertCount > 0" class="lm__footer-stat lm__footer-stat--alert">
              <LucideIcon name="warning" />
              <strong>{{ alertCount }}</strong> cảnh báo
            </span>
          </div>

          <div class="lm__footer-right">
            <span class="lm__footer-conn" :class="{ 'lm__footer-conn--live': isSocketConnected }">
              <LucideIcon :name="isSocketConnected ? 'wifi' : 'sync'" />
              {{ isSocketConnected ? 'Đã kết nối thời gian thực' : `Cập nhật định kỳ (cập nhật ${lastUpdatedLabel})` }}
            </span>
            <button type="button" class="lm__footer-refresh" @click="handleRefresh">
              <LucideIcon name="refresh" />
            </button>
          </div>
        </div>
      </template>
    </ControlCenterLayout>

    <!-- Warning Modal -->
    <Modal v-model="showWarningModal" title="" size="md">
      <template #header>
        <div class="lm__modal-header">
          <div class="lm__modal-icon lm__modal-icon--warning">
            <LucideIcon name="warning" />
          </div>
          <div>
            <h3 class="lm__modal-title">Gửi cảnh báo</h3>
            <p class="lm__modal-subtitle">Sinh viên: {{ warningTarget?.name || '—' }}</p>
          </div>
        </div>
      </template>

      <div class="lm__modal-body">
        <label class="lm__modal-label">Nội dung cảnh báo (tùy chọn)</label>
        <textarea
          v-model="warningMessage"
          rows="3"
          class="lm__modal-textarea"
          placeholder="Để trống sẽ dùng mặc định: Bạn đang bị cảnh báo bởi giám thị. Vui lòng tuân thủ quy định phòng thi."
        />
      </div>

      <template #footer>
        <div class="lm__modal-footer">
          <button type="button" class="lm__modal-btn lm__modal-btn--ghost" @click="closeWarningModal">Hủy</button>
          <button type="button" class="lm__modal-btn lm__modal-btn--warning" :disabled="actionLoading" @click="confirmSendWarning">
            <LucideIcon name="send" />
            Gửi cảnh báo
          </button>
        </div>
      </template>
    </Modal>

    <!-- Pause Modal -->
    <Modal v-model="showPauseModal" title="" size="md">
      <template #header>
        <div class="lm__modal-header">
          <div class="lm__modal-icon lm__modal-icon--warning">
            <LucideIcon name="pause_circle" />
          </div>
          <div>
            <h3 class="lm__modal-title">Tạm dừng bài thi</h3>
            <p class="lm__modal-subtitle">Sinh viên: {{ pauseTarget?.name || '—' }}</p>
          </div>
        </div>
      </template>

      <div class="lm__modal-body">
        <label class="lm__modal-label">Lý do tạm dừng (tùy chọn)</label>
        <textarea
          v-model="pauseReason"
          rows="3"
          class="lm__modal-textarea"
          placeholder="Nhập lý do tạm dừng..."
        />
      </div>

      <template #footer>
        <div class="lm__modal-footer">
          <button type="button" class="lm__modal-btn lm__modal-btn--ghost" @click="closePauseModal">Hủy</button>
          <button type="button" class="lm__modal-btn lm__modal-btn--warning" :disabled="actionLoading" @click="confirmPause">
            <LucideIcon name="pause" />
            Tạm dừng
          </button>
        </div>
      </template>
    </Modal>

    <!-- Invalidate Modal -->
    <Modal v-model="showInvalidateModal" title="" size="md">
      <template #header>
        <div class="lm__modal-header">
          <div class="lm__modal-icon lm__modal-icon--danger">
            <LucideIcon name="block" />
          </div>
          <div>
            <h3 class="lm__modal-title">Đình chỉ bài thi</h3>
            <p class="lm__modal-subtitle">Sinh viên: {{ invalidateTarget?.name || '—' }}</p>
          </div>
        </div>
      </template>

      <div class="lm__modal-body">
        <label class="lm__modal-label">Lý do đình chỉ (tùy chọn)</label>
        <textarea
          v-model="invalidateReason"
          rows="3"
          class="lm__modal-textarea"
          placeholder="Để trống sẽ dùng mặc định: Bài thi đã bị đình chỉ bởi giám thị."
        />
      </div>

      <template #footer>
        <div class="lm__modal-footer">
          <button type="button" class="lm__modal-btn lm__modal-btn--ghost" @click="closeInvalidateModal">Hủy</button>
          <button type="button" class="lm__modal-btn lm__modal-btn--danger" :disabled="actionLoading" @click="confirmInvalidate">
            <LucideIcon name="block" />
            Xác nhận đình chỉ
          </button>
        </div>
      </template>
    </Modal>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '../../services/apiClient'
import { listExamAttempts, getAttemptDetail } from '../../services/attemptService'
import { invalidateAttempt, sendTeacherWarning } from '../../services/monitoringService'
import { useToast } from '../../composables/useToast'
import { writeMonitoringSessionQuery } from '../../services/monitoringContextStorage'

// Components
import ControlCenterLayout from './monitoring/ControlCenterLayout.vue'
import MonitoringHeader from './monitoring/MonitoringHeader.vue'
import MonitoringFilters from './monitoring/MonitoringFilters.vue'
import RoomStatusCard from './monitoring/RoomStatusCard.vue'
import StudentMonitorTable from './monitoring/StudentMonitorTable.vue'
import AlertPanel from './monitoring/AlertPanel.vue'
import LiveEventFeed from './monitoring/LiveEventFeed.vue'
import Modal from '../ui/Modal.vue'

const route = useRoute()
const router = useRouter()
const toast = useToast()

// ── Route / Exam data ───────────────────────────────────────────────────────────
const examId = computed(() => {
  const raw = route.query.examId
  return raw ? Number.parseInt(String(raw), 10) : null
})

const selectedExamTitle = computed(() => route.query.title || 'Giám sát trực tiếp')
const selectedExamCode = computed(() => route.query.code || '')
const selectedExamMeta = computed(() => route.query.meta || '')

// ── State ───────────────────────────────────────────────────────────────────────
const attempts = ref([])
const detailsByAttemptId = ref({})
const isSyncing = ref(false)
const lastUpdatedAt = ref(null)
const actionLoading = ref(false)
let refreshTimer = null
let stompClient = null
let lastRealtimeRefreshAt = 0

const isSocketConnected = ref(false)
const connectionMode = ref('realtime')

// View state
const viewMode = ref('table')  // 'grid' | 'table'
const activeTab = ref('all')
const searchQuery = ref('')
const riskBand = ref('ALL')
const statusFilter = ref('ALL')
const timeRange = ref('all')
const alertPanelOpen = ref(true)
const allPanelsCollapsed = ref(false)

// Modals
const showWarningModal = ref(false)
const showPauseModal = ref(false)
const showInvalidateModal = ref(false)
const warningTarget = ref(null)
const pauseTarget = ref(null)
const invalidateTarget = ref(null)
const warningMessage = ref('')
const pauseReason = ref('')
const invalidateReason = ref('')

// ── Alerts & Events ───────────────────────────────────────────────────────────────
const alerts = ref([])
const recentEvents = ref([])

// ── Computed: rooms (grid view) ─────────────────────────────────────────────────
const rooms = computed(() => {
  if (!examId.value) return []
  const dur = Number(route.query.durationMinutes || 60)
  return [{
    id: examId.value,
    title: selectedExamTitle.value,
    subject: '',
    className: selectedExamMeta.value,
    code: selectedExamCode.value,
    startTime: route.query.startTime || null,
    durationMinutes: dur,
    studentCount: attempts.value.length,
    participantCount: attempts.value.length,
    answeredCount: onlineCount.value,
    alertCount: alertCount.value,
    isActive: true,
    isLive: true,
    status: 'live',
    riskScore: overallRiskScore.value
  }]
})

const filteredRooms = computed(() => {
  let list = rooms.value
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    list = list.filter(r =>
      (r.title || '').toLowerCase().includes(q) ||
      (r.className || '').toLowerCase().includes(q)
    )
  }
  if (activeTab.value === 'stable') {
    list = list.filter(r => (r.riskScore || 0) < 31)
  } else if (activeTab.value === 'attention') {
    list = list.filter(r => (r.riskScore || 0) >= 31 && (r.riskScore || 0) < 61)
  } else if (activeTab.value === 'critical') {
    list = list.filter(r => (r.riskScore || 0) >= 61)
  }
  return list
})

const examCount = computed(() => rooms.value.length)

// ── Computed: students (table view) ────────────────────────────────────────────
const tableStudents = computed(() => attempts.value.map(attempt => {
  const riskScore = Number(attempt.riskScore || 0)
  const suspicious = riskScore > 0 || Boolean(attempt.suspicious)
  const detail = detailsByAttemptId.value[attempt.id]
  const answered = Number(detail?.answeredCount || 0)
  const total = Number(detail?.totalQuestions || 0)
  const progress = total > 0 ? Math.round((answered / total) * 100) : 0

  return {
    id: attempt.id,
    attemptId: attempt.id,
    userName: attempt.student || 'Sinh viên không rõ',
    fullName: attempt.student || '',
    email: attempt.email || '',
    studentCode: attempt.studentCode || '',
    score: attempt.score,
    cameraOn: Boolean(attempt.cameraOn),
    micOn: Boolean(attempt.micOn),
    screenShare: Boolean(attempt.screenShare),
    tabCount: attempt.tabCount || 1,
    violationCount: attempt.violationCount || 0,
    riskScore,
    status: attemptStatus(attempt),
    isOnline: attempt.status !== 'STOPPED' && attempt.status !== 'SUBMITTED',
    startedAt: attempt.startedAt || attempt.startTime,
    answeredCount: answered,
    totalQuestions: total,
    timeSpent: detail?.timeSpent,
    elapsedTime: detail?.elapsedTime,
    browser: attempt.browser,
    ipAddress: attempt.ipAddress || attempt.ip,
    location: attempt.location,
    city: attempt.city
  }
}))

const attemptStatus = (attempt) => {
  const s = String(attempt.status || '').toUpperCase()
  if (s === 'STOPPED' || s === 'PAUSED') return 'PAUSED'
  if (s === 'SUBMITTED' || s === 'COMPLETED') return 'SUBMITTED'
  if (attempt.riskScore > 80) return 'CRITICAL'
  if (s === 'ACTIVE' || s === 'IN_PROGRESS') return 'ONLINE'
  return 'OFFLINE'
}

// ── Computed: stats ──────────────────────────────────────────────────────────────
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

const examDurationLabel = computed(() => {
  const mins = Number(route.query.durationMinutes || 0)
  if (mins > 0) return `${mins} phút`
  return '—'
})

const overallRiskScore = computed(() => {
  if (!attempts.value.length) return 0
  const avg = attempts.value.reduce((sum, a) => sum + Number(a.riskScore || 0), 0)
  return Math.round(avg / attempts.value.length)
})

const filterStats = computed(() => ({
  total: rooms.value.length,
  stable: rooms.value.filter(r => (r.riskScore || 0) < 31).length,
  attention: rooms.value.filter(r => (r.riskScore || 0) >= 31 && (r.riskScore || 0) < 61).length,
  critical: rooms.value.filter(r => (r.riskScore || 0) >= 61).length
}))

const roomsLoading = computed(() => isSyncing.value && attempts.value.length === 0)

const lastUpdatedLabel = computed(() => {
  if (!lastUpdatedAt.value) return 'chưa có dữ liệu'
  return new Date(lastUpdatedAt.value).toLocaleTimeString('vi-VN', {
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
})

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
    
    // Check if data actually changed to avoid unnecessary updates
    const newDataStr = JSON.stringify(fetchedAttempts.map(a => ({ id: a.id, status: a.status, riskScore: a.riskScore })))
    const oldDataStr = JSON.stringify(attempts.value.map(a => ({ id: a.id, status: a.status, riskScore: a.riskScore })))
    
    if (newDataStr !== oldDataStr) {
      attempts.value = fetchedAttempts
      detailsByAttemptId.value = Object.fromEntries(detailPairs)
      lastUpdatedAt.value = Date.now()
      
      // Only add refresh event when data actually changed
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

// Đổi kỳ thi / vào lại từ tab khác: tải lại dữ liệu + kết nối WebSocket theo examId mới
watch(examId, (newId, oldId) => {
  if (!newId || newId === oldId) return
  void loadAttempts()
  disconnectRealtime()
  if (connectionMode.value === 'realtime') {
    void connectRealtime()
  }
})

// ── Realtime ───────────────────────────────────────────────────────────────────
const getAuthToken = async () => {
  if (typeof window === 'undefined') return ''
  const { getStoredToken } = await import('../../services/authService')
  return String(getStoredToken() || '')
}

const connectRealtime = async () => {
  if (!examId.value) return
  disconnectRealtime()

  const [{ Client }, { default: SockJS }] = await Promise.all([
    import('@stomp/stompjs'),
    import('sockjs-client')
  ])

  const { API_BASE_URL } = await import('../../services/apiClient')
  const wsUrl = `${API_BASE_URL.replace(/\/$/, '')}/ws`
  const token = await getAuthToken()

  stompClient = new Client({
    reconnectDelay: 1000,
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    webSocketFactory: () => new SockJS(wsUrl)
  })

  stompClient.onConnect = () => {
    isSocketConnected.value = true
    stompClient.subscribe(`/topic/exams/${examId.value}/alerts`, () => {
      const now = Date.now()
      if (now - lastRealtimeRefreshAt < 500) return // Debounce 500ms
      lastRealtimeRefreshAt = now
      void loadAttempts(true) // silent mode - don't show loading, don't add event
    })
  }

  stompClient.onStompError = () => {
    isSocketConnected.value = false
  }

  stompClient.onWebSocketClose = () => {
    isSocketConnected.value = false
  }

  stompClient.activate()
}

const disconnectRealtime = () => {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
  isSocketConnected.value = false
}

const toggleConnectionMode = (mode) => {
  connectionMode.value = mode
  if (mode === 'realtime') {
    void connectRealtime()
  } else {
    disconnectRealtime()
  }
}

// ── Events & Alerts ─────────────────────────────────────────────────────────────
let lastEventType = ''
let lastEventTime = 0

const addEvent = (event) => {
  const now = Date.now()
  // Throttle: don't add same event type within 2 seconds
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
  
  recentEvents.value = [evt, ...recentEvents.value].slice(0, 30) // Limit to 30 events
}

const handleAlertClick = (alert) => {
  const student = tableStudents.value.find(s => s.id === alert.attemptId)
  if (student) {
    openStudentDetail(student)
  }
}

const handleDismissAlert = (alertId) => {
  alerts.value = alerts.value.filter(a => a.id !== alertId)
}

const handleMarkAllRead = () => {
  alerts.value = alerts.value.map(a => ({ ...a, read: true }))
}

const handleClearEvents = () => {
  recentEvents.value = []
}

// ── Actions ────────────────────────────────────────────────────────────────────
const openWarningModal = (student) => {
  warningTarget.value = student
  warningMessage.value = ''
  showWarningModal.value = true
}

const closeWarningModal = () => {
  showWarningModal.value = false
  warningTarget.value = null
  warningMessage.value = ''
}

const confirmSendWarning = async () => {
  if (!warningTarget.value) return
  actionLoading.value = true
  try {
    await sendTeacherWarning(warningTarget.value.attemptId, warningMessage.value.trim())
    toast.success(`Đã gửi cảnh báo tới ${warningTarget.value.userName}.`)
    addEvent({ type: 'WARN', studentName: warningTarget.value.userName })
    closeWarningModal()
    await loadAttempts()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không gửi được cảnh báo.')
  } finally {
    actionLoading.value = false
  }
}

const openPauseModal = (student) => {
  pauseTarget.value = student
  pauseReason.value = ''
  showPauseModal.value = true
}

const closePauseModal = () => {
  showPauseModal.value = false
  pauseTarget.value = null
  pauseReason.value = ''
}

const confirmPause = async () => {
  if (!pauseTarget.value) return
  actionLoading.value = true
  try {
    await invalidateAttempt(pauseTarget.value.attemptId, pauseReason.value.trim())
    toast.success(`Đã tạm dừng bài thi của ${pauseTarget.value.userName}.`)
    addEvent({ type: 'PAUSE', studentName: pauseTarget.value.userName })
    closePauseModal()
    await loadAttempts()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể tạm dừng bài thi.')
  } finally {
    actionLoading.value = false
  }
}

const openInvalidateModal = (student) => {
  invalidateTarget.value = student
  invalidateReason.value = ''
  showInvalidateModal.value = true
}

const closeInvalidateModal = () => {
  showInvalidateModal.value = false
  invalidateTarget.value = null
  invalidateReason.value = ''
}

const confirmInvalidate = async () => {
  if (!invalidateTarget.value) return
  actionLoading.value = true
  try {
    await invalidateAttempt(invalidateTarget.value.attemptId, invalidateReason.value.trim())
    toast.success(`Đã đình chỉ bài thi của ${invalidateTarget.value.userName}.`)
    addEvent({ type: 'INvalidate', studentName: invalidateTarget.value.userName })
    closeInvalidateModal()
    await loadAttempts()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể đình chỉ bài thi.')
  } finally {
    actionLoading.value = false
  }
}

const openStudentDetail = (student) => {
  router.push({
    path: '/teacher/live-monitoring/student-detail',
    query: {
      attemptId: student.attemptId,
      examId: examId.value ? String(examId.value) : '',
      student: student.userName || student.fullName || student.email,
      studentId: student.id,
      exam: selectedExamTitle.value,
      avatar: student.image || ''
    }
  })
}

const openRoomView = (room) => {
  viewMode.value = 'table'
}

const handlePauseRoom = async (room) => {
  // Pause all active students in this room
  toast.info(`Tạm dừng tất cả trong phòng: ${room.title}`)
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
    clearSelection()
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
    clearSelection()
  }
}

const handleBatchInvalidate = async (ids) => {
  if (ids.length === 0) return
  actionLoading.value = true
  try {
    await Promise.allSettled(ids.map(id => invalidateAttempt(id, 'Đình chỉ hàng loạt')))
    toast.success(`Đã đình chỉ ${ids.length} bài thi.`)
    addEvent({ type: 'INvalidate', message: `Đình chỉ hàng loạt ${ids.length} HS`, studentName: '' })
    await loadAttempts()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể đình chỉ hàng loạt.')
  } finally {
    actionLoading.value = false
    clearSelection()
  }
}

const handleBack = () => {
  router.push('/teacher/live-monitoring')
}

const handleRefresh = () => {
  void loadAttempts()
}

const resetFilters = () => {
  activeTab.value = 'all'
  searchQuery.value = ''
  riskBand.value = 'ALL'
  statusFilter.value = 'ALL'
}

const handleToggleAllPanels = () => {
  allPanelsCollapsed.value = !allPanelsCollapsed.value
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
      void loadAttempts(true) // silent mode
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

/* Grid view */
.lm__grid-view {
  width: 100%;
}

.lm__rooms-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1rem;
  padding: 0.25rem 0;
}

.lm__empty-grid {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 4rem 2rem;
  text-align: center;
  background: var(--ds-surface);
  border: 1.5px dashed var(--ds-border);
  border-radius: var(--ds-radius-2xl);
}

.dark .lm__empty-grid {
  border-color: var(--ds-border-strong);
}


.lm__empty-grid p {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  margin: 0;
}

.dark .lm__empty-grid p { color: var(--ds-text-muted); }

.lm__empty-grid button {
  padding: 0.5rem 1.25rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-primary);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
}

.lm__empty-grid button:hover {
  background: var(--ds-primary);
  color: white;
}

/* Loading skeletons */
.lm__grid-loading {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1rem;
}

.lm__grid-skeleton {
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.dark .lm__grid-skeleton {
  border-color: var(--ds-border-strong);
}

.lm__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-100) 25%, var(--ds-gray-200) 50%, var(--ds-gray-100) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--ds-radius-lg);
}

.dark .lm__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-800) 25%, var(--ds-gray-700) 50%, var(--ds-gray-800) 75%);
  background-size: 200% 100%;
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.lm__skeleton--stripe { height: 4px; border-radius: var(--ds-radius-2xl) var(--ds-radius-2xl) 0 0; margin: -1rem -1rem 0; }
.lm__skeleton--content { height: 3rem; }
.lm__skeleton--stats { height: 2.5rem; }
.lm__skeleton--actions { height: 2rem; }

/* Footer */
.lm__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 1.5rem;
  flex-wrap: wrap;
}

.lm__footer-left {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.lm__footer-stat {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  font-weight: 600;
}

.lm__footer-stat strong {
  color: var(--ds-text);
  font-family: var(--ds-font-display);
}

.dark .lm__footer-stat strong { color: var(--ds-text); }


.lm__footer-stat--online strong { color: var(--ds-success); }
.lm__footer-stat--alert strong { color: var(--ds-warning); }

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

.lm__footer-refresh {
  width: 2rem;
  height: 2rem;
  border-radius: var(--ds-radius-md);
  border: 1px solid var(--ds-border);
  background: transparent;
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.dark .lm__footer-refresh {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.lm__footer-refresh:hover {
  background: var(--ds-gray-100);
  color: var(--ds-primary);
}

.dark .lm__footer-refresh:hover {
  background: var(--ds-gray-700);
}


.lm__spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* Modal */
.lm__modal-header {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
}

.lm__modal-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.lm__modal-icon--warning { background: var(--ds-warning-soft); color: var(--ds-warning); }
.lm__modal-icon--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }

.lm__modal-title {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .lm__modal-title { color: var(--ds-text); }

.lm__modal-subtitle {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.lm__modal-body {
  padding-top: 0.5rem;
}

.lm__modal-label {
  display: block;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
  margin-bottom: 0.5rem;
}

.dark .lm__modal-label { color: var(--ds-text-muted); }

.lm__modal-textarea {
  width: 100%;
  padding: 0.6875rem 1rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  color: var(--ds-text);
  outline: none;
  resize: vertical;
  transition: all 0.15s ease;
  font-family: inherit;
  line-height: 1.5;
  min-height: 80px;
}

.dark .lm__modal-textarea {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text);
}

.lm__modal-textarea::placeholder { color: var(--ds-text-muted); }
.dark .lm__modal-textarea::placeholder { color: var(--ds-gray-500); }

.lm__modal-textarea:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.lm__modal-footer {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}

.lm__modal-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid transparent;
  font-family: inherit;
}


.lm__modal-btn--ghost {
  background: transparent;
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .lm__modal-btn--ghost {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-muted);
}

.lm__modal-btn--ghost:hover {
  border-color: var(--ds-gray-300);
  color: var(--ds-text);
}

.dark .lm__modal-btn--ghost:hover {
  background: var(--ds-gray-700);
}

.lm__modal-btn--warning {
  background: var(--ds-warning);
  color: white;
  box-shadow: 0 4px 12px rgba(234, 179, 8, 0.25);
}

.lm__modal-btn--warning:hover:not(:disabled) {
  background: var(--ds-primary-hover);
  transform: translateY(-1px);
}

.lm__modal-btn--danger {
  background: var(--ds-danger);
  color: white;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.25);
}

.lm__modal-btn--danger:hover:not(:disabled) {
  background: #b91c1c;
  transform: translateY(-1px);
}

.lm__modal-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
}
</style>
