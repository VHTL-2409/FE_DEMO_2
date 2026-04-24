<template>
  <div class="pm-root">

    <!-- ── Top Header ─────────────────────────────────────────── -->
    <div class="pm-topbar">
      <div class="pm-topbar__left">
        <button class="pm-topbar__back" @click="handleBack">
          <LucideIcon name="arrow-left" :size="16" />
          <span>Danh sách</span>
        </button>
        <div class="pm-topbar__divider" />
        <div class="pm-topbar__meta">
          <div class="pm-live-badge">
            <span class="pm-live-badge__dot" />
            LIVE
          </div>
          <div>
            <h1 class="pm-topbar__title">{{ selectedExamTitle }}</h1>
            <span v-if="selectedExamMeta" class="pm-topbar__subtitle">{{ selectedExamMeta }}</span>
          </div>
        </div>
      </div>

      <div class="pm-topbar__right">
        <!-- Online count -->
        <div class="pm-chip pm-chip--success">
          <LucideIcon name="wifi" :size="13" />
          <span>{{ onlineCount }} online</span>
        </div>
        <!-- Alerts chip -->
        <div class="pm-chip pm-chip--warn" v-if="alertCount > 0">
          <LucideIcon name="alert-triangle" :size="13" />
          <span>{{ alertCount }} cảnh báo</span>
        </div>
        <!-- Total students -->
        <div class="pm-chip pm-chip--neutral">
          <LucideIcon name="users" :size="13" />
          <span>{{ store.cards.length }} thí sinh</span>
        </div>
        <!-- Timer -->
        <div v-if="timeRemaining" class="pm-timer">
          <LucideIcon name="clock" :size="14" />
          <span>{{ timeRemaining }}</span>
        </div>
        <!-- Alerts toggle -->
        <button
          class="pm-icon-btn"
          :class="{ 'pm-icon-btn--active': !allPanelsCollapsed }"
          @click="allPanelsCollapsed = !allPanelsCollapsed"
          title="Bật/tắt cảnh báo"
        >
          <LucideIcon name="bell" :size="16" />
          <span v-if="liveAlerts.length" class="pm-icon-btn__badge">{{ liveAlerts.length }}</span>
        </button>
        <!-- Refresh -->
        <button
          class="pm-icon-btn pm-icon-btn--primary"
          @click="handleRefresh"
          :disabled="isSyncing"
          title="Làm mới"
        >
          <LucideIcon name="refresh-cw" :size="16" :class="{ 'pm-spin': isSyncing }" />
        </button>
      </div>
    </div>

    <!-- ── Stats Bar ───────────────────────────────────────────── -->
    <div class="pm-stats-bar">
      <div class="pm-stat-item">
        <span class="pm-stat-item__value">{{ store.cards.length }}</span>
        <span class="pm-stat-item__label">Tổng</span>
      </div>
      <div class="pm-stat-item pm-stat-item--success">
        <span class="pm-stat-item__value">{{ onlineCount }}</span>
        <span class="pm-stat-item__label">Đang thi</span>
      </div>
      <div class="pm-stat-item pm-stat-item--warn">
        <span class="pm-stat-item__value">{{ alertCount }}</span>
        <span class="pm-stat-item__label">Nghi ngờ</span>
      </div>
      <div class="pm-stat-item pm-stat-item--danger">
        <span class="pm-stat-item__value">{{ submittedCount }}</span>
        <span class="pm-stat-item__label">Đã nộp</span>
      </div>
    </div>

    <!-- ── Main Layout ──────────────────────────────────────── -->
    <div class="pm-layout">
      <div class="pm-main">

        <!-- Filter + Search row -->
        <div class="pm-filter-row">
          <div class="pm-filter-tabs">
            <button
              v-for="tab in filterTabs"
              :key="tab.value"
              class="pm-filter-tab"
              :class="{ 'pm-filter-tab--active': activeTab === tab.value }"
              @click="activeTab = tab.value"
            >
              <span>{{ tab.label }}</span>
              <span v-if="tab.count > 0" class="pm-filter-tab__count">{{ tab.count }}</span>
            </button>
          </div>
          <div class="pm-search">
            <LucideIcon name="search" :size="14" class="pm-search__icon" />
            <input
              v-model="searchQuery"
              class="pm-search__input"
              placeholder="Tìm học sinh..."
            />
            <button v-if="searchQuery" class="pm-search__clear" @click="searchQuery = ''">
              <LucideIcon name="x" :size="12" />
            </button>
          </div>
        </div>

        <!-- Batch actions -->
        <Transition name="pm-slide">
          <div v-if="selectedIds.length > 0" class="pm-batch">
            <span class="pm-batch__label">
              <LucideIcon name="check-square" :size="14" />
              {{ selectedIds.length }} đã chọn
            </span>
            <div class="pm-batch__actions">
              <button class="pm-batch-btn pm-batch-btn--warn" @click="handleBatchWarn">
                <LucideIcon name="alert-triangle" :size="13" />
                Cảnh báo
              </button>
              <button class="pm-batch-btn pm-batch-btn--warn" @click="handleBatchPause">
                <LucideIcon name="pause" :size="13" />
                Tạm dừng
              </button>
              <button class="pm-batch-btn pm-batch-btn--danger" @click="handleBatchInvalidate">
                <LucideIcon name="x-circle" :size="13" />
                Đình chỉ
              </button>
              <button class="pm-batch-btn pm-batch-btn--ghost" @click="clearSelection">
                <LucideIcon name="x" :size="13" />
                Bỏ chọn
              </button>
            </div>
          </div>
        </Transition>

        <!-- View toggle -->
        <div class="pm-view-toggle">
          <button
            class="pm-view-btn"
            :class="{ 'pm-view-btn--active': viewMode === 'grid' }"
            @click="viewMode = 'grid'"
          >
            <LucideIcon name="layout-grid" :size="15" />
          </button>
          <button
            class="pm-view-btn"
            :class="{ 'pm-view-btn--active': viewMode === 'list' }"
            @click="viewMode = 'list'"
          >
            <LucideIcon name="list" :size="15" />
          </button>
        </div>

        <!-- Grid view -->
        <div v-if="viewMode === 'grid'" class="pm-grid">
          <div v-if="filteredStudents.length === 0" class="pm-empty">
            <LucideIcon name="users" :size="40" class="pm-empty__icon" />
            <p>Không có thí sinh nào</p>
          </div>
          <StudentCard
            v-for="student in filteredStudents"
            :key="student.id || student.attemptId"
            :student="student"
            :selected="selectedIds.includes(student.id || student.attemptId)"
            @toggle-select="toggleSelect(student.id || student.attemptId)"
            @view-detail="openStudentDetail(student)"
            @warn="openWarningModal(student)"
            @pause="openPauseModal(student)"
          />
        </div>

        <!-- List view -->
        <div v-else class="pm-list">
          <div v-if="filteredStudents.length === 0" class="pm-empty">
            <LucideIcon name="users" :size="40" class="pm-empty__icon" />
            <p>Không có thí sinh nào</p>
          </div>
          <StudentRow
            v-for="student in filteredStudents"
            :key="student.id || student.attemptId"
            :student="student"
            :selected="selectedIds.includes(student.id || student.attemptId)"
            @toggle-select="toggleSelect(student.id || student.attemptId)"
            @view-detail="openStudentDetail(student)"
            @warn="openWarningModal(student)"
            @pause="openPauseModal(student)"
          />
        </div>

        <!-- Connection status -->
        <div class="pm-conn-bar">
          <span class="pm-conn-bar__status" :class="isSocketConnected ? 'pm-conn-bar__status--live' : 'pm-conn-bar__status--poll'">
            <span class="pm-conn-bar__dot" />
            <LucideIcon :name="isSocketConnected ? 'zap' : 'refresh-cw'" :size="12" />
            {{ isSocketConnected ? 'Kết nối thời gian thực' : `Cập nhật định kỳ · lần cuối ${lastUpdatedLabel}` }}
          </span>
          <button v-if="wsError" class="pm-conn-bar__retry" @click="retryConnection">
            <LucideIcon name="refresh-cw" :size="12" />
            Thử lại
          </button>
        </div>
      </div>

      <!-- Sidebar -->
      <Transition name="pm-sidebar">
        <div v-if="!allPanelsCollapsed" class="pm-sidebar">
          <div class="pm-sidebar__header">
            <div class="pm-sidebar__title">
              <LucideIcon name="bell-ring" :size="16" />
              <span>Cảnh báo &amp; Sự kiện</span>
            </div>
            <div class="pm-sidebar__header-actions">
              <button v-if="liveAlerts.length" class="pm-sidebar__mark-all" @click="handleMarkAllRead">
                Đánh dấu đã đọc
              </button>
              <button class="pm-sidebar__clear" @click="handleClearEvents">
                <LucideIcon name="trash-2" :size="13" />
              </button>
            </div>
          </div>

          <div class="pm-sidebar__tabs">
            <button
              class="pm-sidebar__tab"
              :class="{ 'pm-sidebar__tab--active': sidebarTab === 'alerts' }"
              @click="sidebarTab = 'alerts'"
            >
              Cảnh báo
              <span v-if="liveAlerts.length" class="pm-sidebar__tab-count">{{ liveAlerts.length }}</span>
            </button>
            <button
              class="pm-sidebar__tab"
              :class="{ 'pm-sidebar__tab--active': sidebarTab === 'events' }"
              @click="sidebarTab = 'events'"
            >
              Sự kiện
              <span v-if="recentEvents.length" class="pm-sidebar__tab-count">{{ recentEvents.length }}</span>
            </button>
          </div>

          <!-- Alerts list -->
          <div v-if="sidebarTab === 'alerts'" class="pm-sidebar__content">
            <div v-if="liveAlerts.length === 0" class="pm-sidebar__empty">
              <LucideIcon name="check-circle" :size="28" />
              <p>Không có cảnh báo</p>
            </div>
            <TransitionGroup v-else name="pm-alert" tag="div" class="pm-alert-list">
              <div
                v-for="alert in liveAlerts"
                :key="alert.id"
                class="pm-alert-item"
                :class="[
                  alert.severity === 'HIGH' ? 'pm-alert-item--danger' : '',
                  alert.severity === 'MEDIUM' ? 'pm-alert-item--warn' : '',
                  alert.read ? 'pm-alert-item--read' : ''
                ]"
                @click="handleAlertClick(alert)"
              >
                <div class="pm-alert-item__icon-wrap">
                  <LucideIcon name="alert-triangle" :size="14" />
                </div>
                <div class="pm-alert-item__body">
                  <p class="pm-alert-item__name">{{ alert.studentName }}</p>
                  <p class="pm-alert-item__msg">{{ alert.message }}</p>
                  <span class="pm-alert-item__score">{{ alert.riskScore }}đ</span>
                </div>
                <div class="pm-alert-item__meta">
                  <span class="pm-alert-item__time">{{ relativeTime(alert.timestamp) }}</span>
                  <button class="pm-alert-item__dismiss" @click.stop="handleDismissAlert(alert.id)">
                    <LucideIcon name="x" :size="11" />
                  </button>
                </div>
                <span v-if="!alert.read" class="pm-alert-item__unread" />
              </div>
            </TransitionGroup>
          </div>

          <!-- Events list -->
          <div v-else class="pm-sidebar__content">
            <div v-if="recentEvents.length === 0" class="pm-sidebar__empty">
              <LucideIcon name="activity" :size="28" />
              <p>Không có sự kiện</p>
            </div>
            <TransitionGroup v-else name="pm-alert" tag="div" class="pm-alert-list">
              <div
                v-for="event in recentEvents"
                :key="event.id"
                class="pm-event-item"
                @click="handleAlertClick(event)"
              >
                <div class="pm-event-item__type" :class="`pm-event-item__type--${eventTypeColor(event.type)}`">
                  <LucideIcon :name="eventTypeIcon(event.type)" :size="12" />
                </div>
                <div class="pm-event-item__body">
                  <p v-if="event.studentName" class="pm-event-item__name">{{ event.studentName }}</p>
                  <p class="pm-event-item__msg">{{ event.message }}</p>
                </div>
                <span class="pm-event-item__time">{{ relativeTime(event.timestamp) }}</span>
              </div>
            </TransitionGroup>
          </div>
        </div>
      </Transition>
    </div>

    <!-- Action Confirm Modal -->
    <ConfirmDialog
      v-model="showActionModal"
      :variant="actionModalConfig.variant"
      :title="actionModalConfig.title"
      :message="actionModalConfig.message"
      :confirm-label="actionModalConfig.actionLabel"
      :icon="actionModalConfig.icon"
      :show-reason="true"
      :reason-label="'Lý do'"
      :reason-placeholder="actionModalConfig.placeholder"
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
import { listExamAttempts } from '../../services/attemptService'
import { invalidateAttempt, pauseAttempt, sendTeacherWarning } from '../../services/monitoringService'
import { useToast } from '../../composables/useToast'
import { useRealtimeChannel } from '../../composables/useRealtimeChannel'
import { writeMonitoringSessionQuery } from '../../services/monitoringContextStorage'
import { useProctorDashboardStore } from '../../stores/proctorDashboardStore'
import StudentCard from './StudentCard.vue'
import StudentRow from './StudentRow.vue'
import ConfirmDialog from '../ui/ConfirmDialog.vue'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const store = useProctorDashboardStore()
const { cards: attempts, connectionMode, lastUpdatedAt } = storeToRefs(store)

// ── View mode ──────────────────────────────────────────────────────────
const viewMode = ref('grid')
const activeTab = ref('all')
const sidebarTab = ref('alerts')
const allPanelsCollapsed = ref(false)
const searchQuery = ref('')

// ── Route / Exam ────────────────────────────────────────────────────────
const examId = computed(() => {
  const raw = route.query.examId
  return raw ? Number.parseInt(String(raw), 10) : null
})
const selectedExamTitle = computed(() => route.query.title || 'Giám sát trực tiếp')
const selectedExamMeta = computed(() => route.query.meta || '')

// ── State ───────────────────────────────────────────────────────────────
const isSyncing = ref(false)
const actionLoading = ref(false)
const selectedIds = ref([])
let refreshTimer = null
let reconnectTimer = null

// ── Realtime ────────────────────────────────────────────────────────────
const rt = useRealtimeChannel()
const isSocketConnected = rt.isConnected
const wsError = rt.lastError
const retryConnection = rt.retryConnection

const liveAlerts = ref([])
const recentEvents = ref([])
let alertIdCounter = 0

// ── Modal ───────────────────────────────────────────────────────────────
const showActionModal = ref(false)
const actionTarget = ref(null)
const actionModalConfig = ref({})
const ACTION_CONFIGS = {
  warn: {
    variant: 'warning', title: 'Gửi cảnh báo',
    message: 'Bạn có chắc muốn gửi cảnh báo tới học sinh này?',
    actionLabel: 'Gửi cảnh báo', icon: 'send',
    placeholder: 'Để trống sẽ dùng mặc định: Bạn đang bị cảnh báo bởi giám thị. Vui lòng tuân thủ quy định phòng thi.'
  },
  pause: {
    variant: 'warning', title: 'Tạm dừng bài thi',
    message: 'Bài thi sẽ bị tạm dừng. Học sinh không thể tiếp tục cho đến khi được khôi phục.',
    actionLabel: 'Tạm dừng', icon: 'pause',
    placeholder: 'Nhập lý do tạm dừng...'
  },
  invalidate: {
    variant: 'danger', title: 'Đình chỉ bài thi',
    message: 'Bài thi sẽ bị đình chỉ vĩnh viễn. Hành động này không thể hoàn tác.',
    actionLabel: 'Xác nhận đình chỉ', icon: 'x-circle',
    placeholder: 'Để trống sẽ dùng mặc định: Bài thi đã bị đình chỉ bởi giám thị.'
  }
}
let currentActionType = ''

// ── Stats ───────────────────────────────────────────────────────────────
const onlineCount = computed(() =>
  attempts.value.filter(a => {
    const s = String(a.status || '').toUpperCase()
    return s === 'ACTIVE' || s === 'IN_PROGRESS'
  }).length
)
const alertCount = computed(() =>
  attempts.value.filter(a => Boolean(a.reviewRequired) || Number(a.riskScore || 0) > 30).length
)
const submittedCount = computed(() =>
  attempts.value.filter(a => /SUBMITTED/i.test(a.status || '')).length
)
const lastUpdatedLabel = computed(() => {
  if (!lastUpdatedAt.value) return 'chưa có dữ liệu'
  return new Date(lastUpdatedAt.value).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
})
const timeRemaining = ref('')

// ── Filter tabs ─────────────────────────────────────────────────────────
const filterTabs = computed(() => [
  { label: 'Tất cả', value: 'all', count: attempts.value.length },
  { label: 'Nghi ngờ', value: 'suspicious', count: attempts.value.filter(a => Number(a.riskScore || 0) > 30).length },
  { label: 'Đang thi', value: 'active', count: attempts.value.filter(a => /^(ACTIVE|IN_PROGRESS)$/i.test(a.status || '')).length },
  { label: 'Đã nộp', value: 'submitted', count: attempts.value.filter(a => /SUBMITTED/i.test(a.status || '')).length },
  { label: 'Bị dừng', value: 'stopped', count: attempts.value.filter(a => /STOPPED|PAUSED/i.test(a.status || '')).length },
])

// ── Filtered students ───────────────────────────────────────────────────
const filteredStudents = computed(() => {
  let list = attempts.value
  if (activeTab.value === 'suspicious') list = list.filter(s => Number(s.riskScore || 0) > 30)
  else if (activeTab.value === 'active') list = list.filter(s => /^(ACTIVE|IN_PROGRESS)$/i.test(s.status || ''))
  else if (activeTab.value === 'submitted') list = list.filter(s => /SUBMITTED/i.test(s.status || ''))
  else if (activeTab.value === 'stopped') list = list.filter(s => /STOPPED|PAUSED/i.test(s.status || ''))
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    list = list.filter(s =>
      (s.student || s.name || s.userName || '').toLowerCase().includes(q)
    )
  }
  return list
})

// ── Snapshot helper ────────────────────────────────────────────────────
const buildAttemptSnapshot = (items = []) =>
  items.map(a => [a.id, a.status, a.riskScore, a.reviewRequired, Array.isArray(a.reasons) ? a.reasons.join('|') : ''].join('::')).join('||')

// ── Alert handlers ──────────────────────────────────────────────────────
function handleTeacherAlert(alert) {
  if (!alert || !alert.attemptId) return
  switch (alert.type) {
    case 'RISK_UPDATED':
      liveAlerts.value = [{ id: ++alertIdCounter, attemptId: alert.attemptId, studentName: alert.studentName || '—', riskScore: alert.riskScore ?? 0, riskLevel: alert.riskLevel || 'UNKNOWN', severity: alert.riskScore >= 80 ? 'HIGH' : alert.riskScore >= 30 ? 'MEDIUM' : 'LOW', message: alert.message || `${alert.recommendedAction || 'Rủi ro'}: ${alert.riskScore ?? 0}%`, timestamp: new Date().toISOString(), read: false }, ...liveAlerts.value].slice(0, 50)
      addEvent({ type: 'ALERT', message: [alert.recommendedAction || `Rủi ro thay đổi → ${alert.riskScore ?? 0}%`, ...(alert.reasons || []).slice(0, 1)].filter(Boolean).join(' • '), studentName: alert.studentName || '' })
      break
    case 'SUSPICIOUS':
      liveAlerts.value = [{ id: ++alertIdCounter, attemptId: alert.attemptId, studentName: alert.studentName || '—', riskScore: alert.riskScore ?? 0, riskLevel: 'SUSPICIOUS', severity: 'MEDIUM', message: alert.message || 'Phát hiện hành vi đáng ngờ', timestamp: new Date().toISOString(), read: false }, ...liveAlerts.value].slice(0, 50)
      addEvent({ type: 'ALERT', message: alert.message || 'Hành vi đáng ngờ', studentName: alert.studentName || '' })
      break
    case 'WARNING_SENT': addEvent({ type: 'WARN', message: `GV gửi cảnh báo: ${alert.message || ''}`, studentName: alert.studentName || '' }); break
    case 'ATTEMPT_STOPPED': addEvent({ type: 'INVALIDATE', message: alert.message || 'Bài thi bị đình chỉ', studentName: alert.studentName || '' }); break
    case 'ATTEMPT_PAUSED': addEvent({ type: 'PAUSE', message: alert.message || 'Bài thi bị tạm dừng', studentName: alert.studentName || '' }); break
    case 'ATTEMPT_RESUMED': addEvent({ type: 'RESUME', message: alert.message || 'Bài thi được khôi phục', studentName: alert.studentName || '' }); break
    default:
      liveAlerts.value = [{ id: ++alertIdCounter, attemptId: alert.attemptId, studentName: alert.studentName || '—', riskScore: alert.riskScore ?? 0, riskLevel: 'UNKNOWN', severity: 'LOW', message: alert.message || alert.type || 'Thông báo', timestamp: new Date().toISOString(), read: false }, ...liveAlerts.value].slice(0, 50)
      addEvent({ type: 'ALERT', message: alert.message || alert.type, studentName: alert.studentName || '' })
  }
}

function addEvent(event) {
  const now = Date.now()
  const evt = { ...event, id: now + Math.random(), timestamp: new Date().toISOString() }
  recentEvents.value = [evt, ...recentEvents.value].slice(0, 30)
}

// ── Load ────────────────────────────────────────────────────────────────
const loadAttempts = async (silent = false) => {
  if (!examId.value) return
  if (!silent) isSyncing.value = true
  try {
    const fetched = await listExamAttempts(examId.value)
    const newStr = buildAttemptSnapshot(fetched)
    const oldStr = buildAttemptSnapshot(attempts.value)
    if (newStr !== oldStr) {
      store.setCards(fetched.map(a => ({ ...a, lastSignalAt: a.evidenceSummary?.length ? new Date().toISOString() : (a.startedAt || a.submittedAt || null) })))
    }
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể tải dữ liệu giám sát.')
  } finally {
    if (!silent) isSyncing.value = false
  }
}

// ── Realtime ────────────────────────────────────────────────────────────
const connectRealtime = async () => {
  if (!examId.value) return
  disconnectRealtime()
  await rt.connect({
    topics: [{ destination: `/topic/exams/${examId.value}/alerts`, handler: (body) => { handleTeacherAlert(body); void loadAttempts(true) } }],
    reconnectDelay: 3000,
    onConnect: () => { isSocketConnected.value = true; store.setConnectionMode('realtime') },
    onDisconnect: () => { isSocketConnected.value = false; if (connectionMode.value === 'realtime') scheduleReconnect() },
    onError: () => { isSocketConnected.value = false; toast.error('Mất kết nối thời gian thực. Đã chuyển sang cập nhật định kỳ.') }
  })
}
const disconnectRealtime = () => { if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null } rt.disconnect() }
const scheduleReconnect = () => { if (reconnectTimer) return; reconnectTimer = setTimeout(() => { reconnectTimer = null; if (connectionMode.value === 'realtime' && examId.value) void connectRealtime() }, 5000) }

// ── Events & alerts ─────────────────────────────────────────────────────
const handleAlertClick = (alert) => {
  const student = attempts.value.find(s => s.id === alert.attemptId)
  if (student) openStudentDetail(student)
}
const handleDismissAlert = (id) => { liveAlerts.value = liveAlerts.value.filter(a => a.id !== id) }
const handleMarkAllRead = () => { liveAlerts.value = liveAlerts.value.map(a => ({ ...a, read: true })) }
const handleClearEvents = () => { recentEvents.value = [] }

// ── Actions ─────────────────────────────────────────────────────────────
const openWarningModal = (student) => { actionTarget.value = student; currentActionType = 'warn'; actionModalConfig.value = ACTION_CONFIGS.warn; showActionModal.value = true }
const openPauseModal = (student) => { actionTarget.value = student; currentActionType = 'pause'; actionModalConfig.value = ACTION_CONFIGS.pause; showActionModal.value = true }
const openStudentDetail = (student) => {
  router.push({
    path: '/teacher/live-monitoring/student-detail',
    query: {
      attemptId: String(student.id),
      examId: examId.value ? String(examId.value) : '',
      student: student.student || student.email || '—',
      studentId: String(student.id)
    }
  })
}
const handleActionConfirm = async (reason) => {
  if (!actionTarget.value) return
  actionLoading.value = true
  try {
    const id = actionTarget.value.id
    const studentName = actionTarget.value.student
    if (currentActionType === 'warn') { await sendTeacherWarning(id, reason); toast.success(`Đã gửi cảnh báo tới ${studentName}.`); addEvent({ type: 'WARN', studentName }) }
    else if (currentActionType === 'pause') { await pauseAttempt(id, reason); toast.success(`Đã tạm dừng bài thi của ${studentName}.`); addEvent({ type: 'PAUSE', studentName }) }
    else if (currentActionType === 'invalidate') { await invalidateAttempt(id, reason); toast.success(`Đã đình chỉ bài thi của ${studentName}.`); addEvent({ type: 'INVALIDATE', studentName }) }
    showActionModal.value = false; actionTarget.value = null; await loadAttempts()
  } catch (err) {
    const msg = currentActionType === 'warn' ? 'Không gửi được cảnh báo.' : currentActionType === 'pause' ? 'Không thể tạm dừng bài thi.' : 'Không thể đình chỉ bài thi.'
    toast.error(err instanceof ApiError ? err.message : msg)
  } finally { actionLoading.value = false }
}
const handleBack = () => router.push('/teacher/live-monitoring')
const handleRefresh = () => { void loadAttempts() }
const handleToggleAllPanels = () => { allPanelsCollapsed.value = !allPanelsCollapsed.value }
const clearSelection = () => { selectedIds.value = [] }
const toggleSelect = (id) => { selectedIds.value = selectedIds.value.includes(id) ? selectedIds.value.filter(i => i !== id) : [...selectedIds.value, id] }

// ── Batch ─────────────────────────────────────────────────────────────
const handleBatchWarn = async () => { if (selectedIds.value.length === 0) return; actionLoading.value = true; try { await Promise.allSettled(selectedIds.value.map(id => sendTeacherWarning(id, ''))); toast.success(`Đã gửi cảnh báo tới ${selectedIds.value.length} học sinh.`); addEvent({ type: 'WARN', message: `Cảnh báo hàng loạt ${selectedIds.value.length} HS`, studentName: '' }); selectedIds.value = [] } catch { toast.error('Không gửi được cảnh báo hàng loạt.') } finally { actionLoading.value = false } }
const handleBatchPause = async () => { if (selectedIds.value.length === 0) return; actionLoading.value = true; try { await Promise.allSettled(selectedIds.value.map(id => pauseAttempt(id, 'Tạm dừng hàng loạt'))); toast.success(`Đã tạm dừng ${selectedIds.value.length} bài thi.`); addEvent({ type: 'PAUSE', message: `Tạm dừng hàng loạt ${selectedIds.value.length} HS`, studentName: '' }); await loadAttempts(); selectedIds.value = [] } catch { toast.error('Không thể tạm dừng hàng loạt.') } finally { actionLoading.value = false } }
const handleBatchInvalidate = async () => { if (selectedIds.value.length === 0) return; actionLoading.value = true; try { await Promise.allSettled(selectedIds.value.map(id => invalidateAttempt(id, 'Đình chỉ hàng loạt'))); toast.success(`Đã đình chỉ ${selectedIds.value.length} bài thi.`); addEvent({ type: 'INVALIDATE', message: `Đình chỉ hàng loạt ${selectedIds.value.length} HS`, studentName: '' }); await loadAttempts(); selectedIds.value = [] } catch { toast.error('Không thể đình chỉ hàng loạt.') } finally { actionLoading.value = false } }

// ── Helpers ────────────────────────────────────────────────────────────
const relativeTime = (ts) => {
  if (!ts) return ''
  const diff = Math.floor((Date.now() - new Date(ts).getTime()) / 1000)
  if (diff < 60) return 'vừa xong'
  if (diff < 3600) return `${Math.floor(diff / 60)}p`
  if (diff < 86400) return `${Math.floor(diff / 3600)}g`
  return `${Math.floor(diff / 86400)}ng`
}
const eventTypeIcon = (type) => ({ ALERT: 'alert-triangle', WARN: 'alert-circle', PAUSE: 'pause', RESUME: 'play', INVALIDATE: 'x-circle', REFRESH: 'refresh-cw' }[type] || 'activity')
const eventTypeColor = (type) => ({ ALERT: 'warn', WARN: 'warn', PAUSE: 'info', RESUME: 'success', INVALIDATE: 'danger', REFRESH: 'neutral' }[type] || 'neutral')

// ── Watchers ────────────────────────────────────────────────────────────
watch(() => route.path === '/teacher/live-monitoring/session' ? { ...route.query } : null, (q) => { if (q?.examId) writeMonitoringSessionQuery(q) }, { deep: true, immediate: true })
watch(examId, (newId, oldId) => { if (!newId || newId === oldId) return; void loadAttempts(); disconnectRealtime(); if (connectionMode.value === 'realtime') void connectRealtime() })

// ── Lifecycle ───────────────────────────────────────────────────────────
onMounted(async () => { await loadAttempts(); if (connectionMode.value === 'realtime') await connectRealtime(); refreshTimer = window.setInterval(() => { if (!document.hidden && !isSocketConnected.value) void loadAttempts(true) }, 5000) })
onUnmounted(() => { if (refreshTimer) window.clearInterval(refreshTimer); disconnectRealtime() })
</script>

<style scoped>
/* ── Root ──────────────────────────────────────────────────────────── */
.pm-root {
  min-height: 100vh;
  background: var(--pm-bg, #0f172a);
  display: flex;
  flex-direction: column;
  scroll-behavior: smooth;
}

/* ── Top bar ─────────────────────────────────────────────────────── */
.pm-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.625rem 1.25rem;
  background: rgba(30, 41, 59, 0.9);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(255,255,255,0.07);
  position: sticky;
  top: 0;
  z-index: 100;
  gap: 1rem;
  flex-wrap: wrap;
}
.pm-topbar__left { display: flex; align-items: center; gap: 0.75rem; min-width: 0; }
.pm-topbar__back {
  display: flex; align-items: center; gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius);
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.05); color: rgba(255,255,255,0.7);
  font-size: 0.775rem; font-weight: 600;
  cursor: pointer; white-space: nowrap; flex-shrink: 0;
  transition: all 0.15s;
}
.pm-topbar__back:hover { background: rgba(255,255,255,0.1); color: white; border-color: rgba(255,255,255,0.2); }
.pm-topbar__divider { width: 1px; height: 24px; background: rgba(255,255,255,0.1); flex-shrink: 0; }
.pm-topbar__meta { display: flex; align-items: center; gap: 0.75rem; min-width: 0; }
.pm-live-badge {
  display: flex; align-items: center; gap: 0.375rem;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  background: rgba(220,38,38,0.2);
  color: #f87171;
  font-size: 0.65rem; font-weight: 900; letter-spacing: 0.08em;
  flex-shrink: 0;
}
.pm-live-badge__dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: #f87171;
  animation: pm-pulse 1.5s ease-in-out infinite;
  will-change: opacity, transform;
}
@keyframes pm-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.75); }
}
.pm-topbar__title {
  font-size: 0.9rem; font-weight: 800; color: white; margin: 0;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.pm-topbar__subtitle { font-size: 0.72rem; color: rgba(255,255,255,0.5); }
.pm-topbar__right { display: flex; align-items: center; gap: 0.5rem; flex-shrink: 0; }

/* Chips */
.pm-chip {
  display: inline-flex; align-items: center; gap: 0.3rem;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  font-size: 0.72rem; font-weight: 700;
  border: 1px solid transparent;
}
.pm-chip--success { background: rgba(22,163,74,0.15); color: #4ade80; border-color: rgba(22,163,74,0.2); }
.pm-chip--warn { background: rgba(217,119,6,0.15); color: #fbbf24; border-color: rgba(217,119,6,0.2); }
.pm-chip--neutral { background: rgba(255,255,255,0.05); color: rgba(255,255,255,0.5); border-color: rgba(255,255,255,0.1); }

/* Timer */
.pm-timer {
  display: flex; align-items: center; gap: 0.3rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius);
  background: rgba(99,102,241,0.15);
  color: #a5b4fc;
  font-size: 0.775rem; font-weight: 700;
  font-variant-numeric: tabular-nums;
}

/* Icon buttons */
.pm-icon-btn {
  position: relative;
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--ds-radius);
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.05);
  color: rgba(255,255,255,0.5);
  cursor: pointer; transition: all 0.15s;
}
.pm-icon-btn:hover { background: rgba(255,255,255,0.1); color: white; border-color: rgba(255,255,255,0.2); }
.pm-icon-btn--active { background: rgba(99,102,241,0.2); border-color: rgba(99,102,241,0.4); color: #a5b4fc; }
.pm-icon-btn--primary { background: rgba(99,102,241,0.3); border-color: rgba(99,102,241,0.4); color: #a5b4fc; }
.pm-icon-btn--primary:hover { background: rgba(99,102,241,0.4); }
.pm-icon-btn__badge {
  position: absolute; top: -5px; right: -5px;
  min-width: 16px; height: 16px; border-radius: 9999px;
  background: #dc2626; color: white;
  font-size: 0.6rem; font-weight: 900;
  display: flex; align-items: center; justify-content: center; padding: 0 3px;
}
.pm-spin { animation: pm-spin 0.8s linear infinite; }
@keyframes pm-spin { to { transform: rotate(360deg); } }

/* ── Stats Bar ──────────────────────────────────────────────────────── */
.pm-stats-bar {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 0.75rem 1.25rem;
  background: rgba(30, 41, 59, 0.7);
  border-bottom: 1px solid rgba(255,255,255,0.05);
}
.pm-stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 1.25rem;
  border-right: 1px solid rgba(255,255,255,0.07);
}
.pm-stat-item:first-child { padding-left: 0; }
.pm-stat-item:last-child { border-right: none; }
.pm-stat-item__value {
  font-size: 1.25rem; font-weight: 900; color: white;
  font-variant-numeric: tabular-nums; line-height: 1.1;
}
.pm-stat-item--success .pm-stat-item__value { color: #4ade80; }
.pm-stat-item--warn .pm-stat-item__value { color: #fbbf24; }
.pm-stat-item--danger .pm-stat-item__value { color: #f87171; }
.pm-stat-item__label {
  font-size: 0.65rem; font-weight: 600; color: rgba(255,255,255,0.4);
  text-transform: uppercase; letter-spacing: 0.06em; margin-top: 2px;
}

/* ── Layout ───────────────────────────────────────────────────────── */
.pm-layout {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* ── Main ─────────────────────────────────────────────────────────── */
.pm-main {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  padding: 1rem 1.25rem 2rem;
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}
.pm-main::-webkit-scrollbar { width: 5px; }
.pm-main::-webkit-scrollbar-track { background: transparent; }
.pm-main::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 3px; }
.pm-main::-webkit-scrollbar-thumb:hover { background: rgba(255,255,255,0.2); }

/* ── Filter row ──────────────────────────────────────────────────── */
.pm-filter-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}
.pm-filter-tabs {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: var(--ds-radius);
  padding: 0.25rem;
}
.pm-filter-tab {
  display: flex; align-items: center; gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: calc(var(--ds-radius) - 2px);
  border: none; background: transparent;
  color: rgba(255,255,255,0.5);
  font-size: 0.775rem; font-weight: 600;
  cursor: pointer; transition: all 0.15s; white-space: nowrap;
}
.pm-filter-tab:hover { color: white; background: rgba(255,255,255,0.06); }
.pm-filter-tab--active { background: rgba(99,102,241,0.3); color: #a5b4fc; }
.pm-filter-tab__count {
  min-width: 18px; height: 18px;
  border-radius: 9999px;
  background: rgba(255,255,255,0.1);
  color: rgba(255,255,255,0.6);
  font-size: 0.65rem; font-weight: 900;
  display: flex; align-items: center; justify-content: center; padding: 0 4px;
}
.pm-filter-tab--active .pm-filter-tab__count { background: rgba(165,180,252,0.2); color: #a5b4fc; }

/* Search */
.pm-search {
  display: flex; align-items: center; gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius);
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.05);
  margin-left: auto;
  transition: border-color 0.15s;
}
.pm-search:focus-within { border-color: rgba(99,102,241,0.5); }
.pm-search__icon { color: rgba(255,255,255,0.3); flex-shrink: 0; }
.pm-search__input {
  border: none; background: transparent;
  color: white; font-size: 0.8rem; outline: none; width: 160px;
}
.pm-search__input::placeholder { color: rgba(255,255,255,0.3); }
.pm-search__clear {
  background: transparent; border: none; cursor: pointer;
  color: rgba(255,255,255,0.3); display: flex; align-items: center;
  padding: 0; transition: color 0.15s;
}
.pm-search__clear:hover { color: rgba(255,255,255,0.7); }

/* Batch bar */
.pm-batch {
  display: flex; align-items: center; gap: 1rem;
  padding: 0.625rem 1rem;
  background: rgba(99,102,241,0.2);
  border: 1px solid rgba(99,102,241,0.3);
  border-radius: var(--ds-radius-lg);
  flex-wrap: wrap;
}
.pm-batch__label { display: flex; align-items: center; gap: 0.375rem; color: #a5b4fc; font-size: 0.8rem; font-weight: 700; }
.pm-batch__actions { display: flex; gap: 0.5rem; margin-left: auto; }
.pm-batch-btn {
  display: inline-flex; align-items: center; gap: 0.375rem;
  padding: 0.3rem 0.75rem;
  border-radius: var(--ds-radius);
  font-size: 0.75rem; font-weight: 700;
  cursor: pointer; border: 1px solid transparent;
  transition: all 0.15s;
}
.pm-batch-btn--warn { background: rgba(255,255,255,0.1); color: rgba(255,255,255,0.8); }
.pm-batch-btn--warn:hover { background: rgba(255,255,255,0.15); }
.pm-batch-btn--danger { background: rgba(220,38,38,0.2); color: #f87171; border-color: rgba(220,38,38,0.3); }
.pm-batch-btn--danger:hover { background: rgba(220,38,38,0.3); }
.pm-batch-btn--ghost { background: transparent; color: rgba(255,255,255,0.5); border-color: rgba(255,255,255,0.1); }
.pm-batch-btn--ghost:hover { background: rgba(255,255,255,0.05); color: rgba(255,255,255,0.8); }

/* View toggle */
.pm-view-toggle {
  display: flex; gap: 0.25rem;
  margin-left: auto;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: var(--ds-radius);
  padding: 0.125rem;
}
.pm-view-btn {
  width: 30px; height: 30px;
  display: flex; align-items: center; justify-content: center;
  border-radius: calc(var(--ds-radius) - 2px);
  border: none; background: transparent;
  color: rgba(255,255,255,0.4); cursor: pointer;
  transition: all 0.15s;
}
.pm-view-btn:hover { background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.8); }
.pm-view-btn--active { background: rgba(99,102,241,0.3); color: #a5b4fc; }

/* ── Grid ────────────────────────────────────────────────────────── */
.pm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 0.75rem;
  content-visibility: auto;
  contain-intrinsic-size: 0 160px;
}

/* ── List ────────────────────────────────────────────────────────── */
.pm-list { display: flex; flex-direction: column; gap: 0.5rem; }

/* ── Empty ──────────────────────────────────────────────────────── */
.pm-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 0.75rem; padding: 4rem 2rem;
  color: rgba(255,255,255,0.3); font-size: 0.875rem; text-align: center;
}
.pm-empty__icon { color: rgba(255,255,255,0.1); }

/* ── Connection bar ──────────────────────────────────────────────── */
.pm-conn-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0.5rem 0.75rem;
  border-radius: var(--ds-radius);
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  margin-top: auto;
}
.pm-conn-bar__status {
  display: flex; align-items: center; gap: 0.375rem;
  font-size: 0.72rem; font-weight: 600;
  color: rgba(255,255,255,0.4);
}
.pm-conn-bar__status--live { color: #4ade80; }
.pm-conn-bar__dot {
  width: 6px; height: 6px; border-radius: 50%; background: currentColor;
}
.pm-conn-bar__status--live .pm-conn-bar__dot { animation: pm-pulse 2s ease-in-out infinite; }
.pm-conn-bar__retry {
  display: flex; align-items: center; gap: 0.25rem;
  font-size: 0.7rem; font-weight: 700;
  color: #f87171; background: transparent; border: none; cursor: pointer;
}

/* ── Sidebar ──────────────────────────────────────────────────────── */
.pm-sidebar {
  width: 320px;
  flex-shrink: 0;
  border-left: 1px solid rgba(255,255,255,0.07);
  background: rgba(30, 41, 59, 0.5);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  backdrop-filter: blur(8px);
}
.pm-sidebar__header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0.875rem 1rem;
  border-bottom: 1px solid rgba(255,255,255,0.07);
  gap: 0.5rem;
}
.pm-sidebar__title {
  display: flex; align-items: center; gap: 0.5rem;
  font-size: 0.8rem; font-weight: 800; color: white;
}
.pm-sidebar__header-actions { display: flex; gap: 0.375rem; align-items: center; }
.pm-sidebar__mark-all {
  font-size: 0.7rem; font-weight: 700;
  color: #a5b4fc; background: transparent; border: none; cursor: pointer;
  padding: 0.2rem 0.5rem; border-radius: var(--ds-radius);
  transition: background 0.15s;
}
.pm-sidebar__mark-all:hover { background: rgba(165,180,252,0.1); }
.pm-sidebar__clear {
  width: 26px; height: 26px;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--ds-radius);
  border: 1px solid rgba(255,255,255,0.1);
  background: transparent; color: rgba(255,255,255,0.4);
  cursor: pointer; transition: all 0.15s;
}
.pm-sidebar__clear:hover { background: rgba(220,38,38,0.15); border-color: rgba(220,38,38,0.3); color: #f87171; }

/* Sidebar tabs */
.pm-sidebar__tabs { display: flex; border-bottom: 1px solid rgba(255,255,255,0.07); }
.pm-sidebar__tab {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 0.375rem;
  padding: 0.625rem;
  border: none; background: transparent;
  color: rgba(255,255,255,0.4);
  font-size: 0.775rem; font-weight: 600;
  cursor: pointer; transition: all 0.15s;
  border-bottom: 2px solid transparent;
}
.pm-sidebar__tab:hover { color: rgba(255,255,255,0.7); }
.pm-sidebar__tab--active { color: #a5b4fc; border-bottom-color: #6366f1; }
.pm-sidebar__tab-count {
  min-width: 18px; height: 18px; border-radius: 9999px;
  background: rgba(165,180,252,0.2); color: #a5b4fc;
  font-size: 0.6rem; font-weight: 900;
  display: flex; align-items: center; justify-content: center; padding: 0 4px;
}

/* Sidebar content */
.pm-sidebar__content {
  flex: 1; overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(255,255,255,0.1) transparent;
}
.pm-sidebar__content::-webkit-scrollbar { width: 3px; }
.pm-sidebar__content::-webkit-scrollbar-track { background: transparent; }
.pm-sidebar__content::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 2px; }
.pm-sidebar__content::-webkit-scrollbar-thumb:hover { background: rgba(255,255,255,0.2); }
.pm-sidebar__empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 0.5rem; padding: 3rem 1rem;
  color: rgba(255,255,255,0.3); font-size: 0.8rem; text-align: center;
}

/* Alert list */
.pm-alert-list { display: flex; flex-direction: column; }
.pm-alert-item {
  display: flex; align-items: flex-start; gap: 0.625rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid rgba(255,255,255,0.05);
  cursor: pointer; transition: background 0.12s; position: relative;
}
.pm-alert-item:hover { background: rgba(255,255,255,0.03); }
.pm-alert-item--read { opacity: 0.5; }
.pm-alert-item--danger { border-left: 2px solid rgba(220,38,38,0.7); }
.pm-alert-item--warn { border-left: 2px solid rgba(217,119,6,0.7); }
.pm-alert-item__icon-wrap {
  width: 28px; height: 28px; border-radius: var(--ds-radius);
  display: flex; align-items: center; justify-content: center;
  background: rgba(220,38,38,0.15); color: #f87171; flex-shrink: 0;
}
.pm-alert-item--warn .pm-alert-item__icon-wrap { background: rgba(217,119,6,0.15); color: #fbbf24; }
.pm-alert-item__body { flex: 1; min-width: 0; }
.pm-alert-item__name { font-size: 0.775rem; font-weight: 700; color: white; margin: 0 0 0.15rem; }
.pm-alert-item__msg { font-size: 0.72rem; color: rgba(255,255,255,0.4); margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pm-alert-item__score {
  display: inline-block; margin-top: 0.25rem;
  font-size: 0.68rem; font-weight: 800;
  padding: 0.1rem 0.4rem;
  border-radius: 9999px;
  background: rgba(220,38,38,0.15); color: #f87171;
}
.pm-alert-item--warn .pm-alert-item__score { background: rgba(217,119,6,0.15); color: #fbbf24; }
.pm-alert-item__meta { display: flex; flex-direction: column; align-items: flex-end; gap: 0.25rem; flex-shrink: 0; }
.pm-alert-item__time { font-size: 0.65rem; color: rgba(255,255,255,0.3); white-space: nowrap; }
.pm-alert-item__dismiss {
  width: 18px; height: 18px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%; border: none;
  background: transparent; color: rgba(255,255,255,0.3); cursor: pointer;
  transition: all 0.15s;
}
.pm-alert-item__dismiss:hover { background: rgba(255,255,255,0.08); color: rgba(255,255,255,0.7); }
.pm-alert-item__unread {
  position: absolute; top: 0.75rem; right: 0.5rem;
  width: 6px; height: 6px; border-radius: 50%;
  background: #6366f1;
}

/* Event items */
.pm-event-item {
  display: flex; align-items: flex-start; gap: 0.5rem;
  padding: 0.625rem 1rem;
  border-bottom: 1px solid rgba(255,255,255,0.05);
  cursor: pointer; transition: background 0.12s;
}
.pm-event-item:hover { background: rgba(255,255,255,0.03); }
.pm-event-item__type {
  width: 24px; height: 24px; border-radius: var(--ds-radius);
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.pm-event-item__type--warn { background: rgba(217,119,6,0.15); color: #fbbf24; }
.pm-event-item__type--danger { background: rgba(220,38,38,0.15); color: #f87171; }
.pm-event-item__type--success { background: rgba(22,163,74,0.15); color: #4ade80; }
.pm-event-item__type--info { background: rgba(2,132,199,0.15); color: #38bdf8; }
.pm-event-item__type--neutral { background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.4); }
.pm-event-item__body { flex: 1; min-width: 0; }
.pm-event-item__name { font-size: 0.75rem; font-weight: 700; color: white; margin: 0 0 0.1rem; }
.pm-event-item__msg { font-size: 0.72rem; color: rgba(255,255,255,0.4); margin: 0; }
.pm-event-item__time { font-size: 0.65rem; color: rgba(255,255,255,0.3); white-space: nowrap; flex-shrink: 0; margin-top: 0.2rem; }

/* Transitions */
.pm-slide-enter-active, .pm-slide-leave-active { transition: all 0.2s ease; }
.pm-slide-enter-from, .pm-slide-leave-to { opacity: 0; transform: translateY(-8px); }
.pm-sidebar-enter-active, .pm-sidebar-leave-active { transition: all 0.25s ease; }
.pm-sidebar-enter-from, .pm-sidebar-leave-to { opacity: 0; transform: translateX(20px); }
.pm-alert-enter-active { transition: all 0.3s ease; }
.pm-alert-enter-from { opacity: 0; transform: translateX(-12px); }
</style>
