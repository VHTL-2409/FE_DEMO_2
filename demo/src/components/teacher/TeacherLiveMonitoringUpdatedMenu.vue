<template>
  <div class="pm-root">
    <!-- ── Top Command Bar ──────────────────────────────────────── -->
    <header class="pm-topbar">
      <div class="pm-topbar__left">
        <button
          class="pm-back-btn"
          title="Quay lại danh sách kỳ thi"
          @click="handleBack"
        >
          <LucideIcon
            name="arrow-left"
            :size="16"
          />
          <span>Quay lại</span>
        </button>
        <div class="pm-topbar__divider" />
        <div class="pm-exam-info">
          <div class="pm-exam-badge">
            <span class="pm-exam-badge__dot" />
            LIVE
          </div>
          <div class="pm-exam-meta">
            <h1 class="pm-exam-meta__title">
              {{ selectedExamTitle }}
            </h1>
            <span class="pm-exam-meta__sub">{{ selectedExamMeta }}</span>
          </div>
        </div>
      </div>

      <div class="pm-topbar__center">
        <!-- Connection status -->
        <div
          class="pm-conn-status"
          :class="isSocketConnected ? 'pm-conn-status--live' : 'pm-conn-status--poll'"
        >
          <span class="pm-conn-status__dot" />
          <LucideIcon
            :name="isSocketConnected ? 'zap' : 'refresh-cw'"
            :size="12"
          />
          <span>{{ isSocketConnected ? 'Real-time' : 'Polling' }}</span>
        </div>

        <!-- Quick stats pills -->
        <div class="pm-stat-pill pm-stat-pill--online">
          <LucideIcon
            name="wifi"
            :size="13"
          />
          <span class="pm-stat-pill__value">{{ onlineCount }}</span>
          <span class="pm-stat-pill__label">Online</span>
        </div>
        <div
          v-if="alertCount > 0"
          class="pm-stat-pill pm-stat-pill--warn"
        >
          <LucideIcon
            name="alert-triangle"
            :size="13"
          />
          <span class="pm-stat-pill__value">{{ alertCount }}</span>
          <span class="pm-stat-pill__label">Nghi ngờ</span>
        </div>
        <div class="pm-stat-pill pm-stat-pill--submit">
          <LucideIcon
            name="check-circle"
            :size="13"
          />
          <span class="pm-stat-pill__value">{{ submittedCount }}</span>
          <span class="pm-stat-pill__label">Đã nộp</span>
        </div>
        <div class="pm-stat-pill pm-stat-pill--neutral">
          <LucideIcon
            name="users"
            :size="13"
          />
          <span class="pm-stat-pill__value">{{ store.cards.length }}</span>
          <span class="pm-stat-pill__label">Tổng</span>
        </div>

        <!-- Timer -->
        <div
          v-if="timeRemaining"
          class="pm-timer"
        >
          <LucideIcon
            name="clock"
            :size="14"
          />
          <span>{{ timeRemaining }}</span>
        </div>
      </div>

      <div class="pm-topbar__right">
        <!-- Alerts toggle -->
        <button
          class="pm-icon-btn"
          :class="{ 'pm-icon-btn--active': !allPanelsCollapsed }"
          title="Bật/tắt cảnh báo"
          @click="allPanelsCollapsed = !allPanelsCollapsed"
        >
          <LucideIcon
            name="bell"
            :size="16"
          />
          <span
            v-if="liveAlerts.length"
            class="pm-icon-btn__badge"
          >{{ liveAlerts.length }}</span>
        </button>
        <!-- Refresh -->
        <button
          class="pm-icon-btn pm-icon-btn--primary"
          :disabled="isSyncing"
          title="Làm mới"
          @click="handleRefresh"
        >
          <LucideIcon
            name="refresh-cw"
            :size="16"
            :class="{ 'pm-spin': isSyncing }"
          />
        </button>
      </div>
    </header>

    <!-- ── Main Layout ──────────────────────────────────────────── -->
    <div class="pm-layout">
      <div class="pm-main">
        <!-- ── Filter Bar ── -->
        <div class="pm-filter-bar">
          <div class="pm-filter-tabs">
            <button
              v-for="tab in filterTabs"
              :key="tab.value"
              class="pm-filter-tab"
              :class="{ 'pm-filter-tab--active': activeTab === tab.value }"
              @click="activeTab = tab.value"
            >
              <span>{{ tab.label }}</span>
              <span
                v-if="tab.count > 0"
                class="pm-filter-tab__count"
              >{{ tab.count }}</span>
            </button>
          </div>

          <div class="pm-filter-actions">
            <div class="pm-search-box">
              <LucideIcon
                name="search"
                :size="14"
                class="pm-search-box__icon"
              />
              <input
                v-model="searchQuery"
                class="pm-search-box__input"
                placeholder="Tìm học sinh..."
              >
              <button
                v-if="searchQuery"
                class="pm-search-box__clear"
                @click="searchQuery = ''"
              >
                <LucideIcon
                  name="x"
                  :size="12"
                />
              </button>
            </div>

            <div class="pm-view-toggle">
              <button
                class="pm-view-btn"
                :class="{ 'pm-view-btn--active': viewMode === 'grid' }"
                title="Lưới"
                @click="viewMode = 'grid'"
              >
                <LucideIcon
                  name="layout-grid"
                  :size="15"
                />
              </button>
              <button
                class="pm-view-btn"
                :class="{ 'pm-view-btn--active': viewMode === 'list' }"
                title="Danh sách"
                @click="viewMode = 'list'"
              >
                <LucideIcon
                  name="list"
                  :size="15"
                />
              </button>
            </div>
          </div>
        </div>

        <!-- ── Batch Actions ── -->
        <Transition name="pm-slide">
          <div
            v-if="selectedIds.length > 0"
            class="pm-batch-bar"
          >
            <div class="pm-batch-bar__info">
              <LucideIcon
                name="check-square"
                :size="15"
              />
              <span>{{ selectedIds.length }} học sinh đã chọn</span>
            </div>
            <div class="pm-batch-bar__actions">
              <button
                class="pm-batch-btn pm-batch-btn--warn"
                @click="handleBatchWarn"
              >
                <LucideIcon
                  name="alert-triangle"
                  :size="13"
                />
                Cảnh báo
              </button>
              <button
                class="pm-batch-btn pm-batch-btn--pause"
                @click="handleBatchPause"
              >
                <LucideIcon
                  name="pause"
                  :size="13"
                />
                Tạm dừng
              </button>
              <button
                v-if="hasPausedSelection"
                class="pm-batch-btn pm-batch-btn--resume"
                @click="handleBatchResume"
              >
                <LucideIcon
                  name="play"
                  :size="13"
                />
                Cho phép tiếp tục
              </button>
              <button
                v-if="pausedCountInExam > 0"
                class="pm-batch-btn pm-batch-btn--restore"
                @click="handleRestoreAllPaused"
              >
                <LucideIcon
                  name="rotate-ccw"
                  :size="13"
                />
                Khôi phục hàng loạt ({{ pausedCountInExam }})
              </button>
              <button
                class="pm-batch-btn pm-batch-btn--danger"
                @click="handleBatchInvalidate"
              >
                <LucideIcon
                  name="x-circle"
                  :size="13"
                />
                Đình chỉ
              </button>
              <div class="pm-batch-bar__sep" />
              <button
                class="pm-batch-btn pm-batch-btn--ghost"
                @click="clearSelection"
              >
                <LucideIcon
                  name="x"
                  :size="13"
                />
                Bỏ chọn
              </button>
            </div>
          </div>
        </Transition>

        <!-- ── Risk Summary Strip ── -->
        <div class="pm-risk-strip">
          <div class="pm-risk-strip__item pm-risk-strip__item--clean">
            <span class="pm-risk-strip__dot" />
            <span class="pm-risk-strip__label">Bình thường</span>
            <span class="pm-risk-strip__count">{{ getCountByBand('clean') }}</span>
          </div>
          <div class="pm-risk-strip__item pm-risk-strip__item--suspicious">
            <span class="pm-risk-strip__dot" />
            <span class="pm-risk-strip__label">Nghi ngờ</span>
            <span class="pm-risk-strip__count">{{ getCountByBand('suspicious') }}</span>
          </div>
          <div class="pm-risk-strip__item pm-risk-strip__item--high">
            <span class="pm-risk-strip__dot" />
            <span class="pm-risk-strip__label">Rủi ro cao</span>
            <span class="pm-risk-strip__count">{{ getCountByBand('high') }}</span>
          </div>
          <div class="pm-risk-strip__item pm-risk-strip__item--critical">
            <span class="pm-risk-strip__dot" />
            <span class="pm-risk-strip__label">Nguy hiểm</span>
            <span class="pm-risk-strip__count">{{ getCountByBand('critical') }}</span>
          </div>
          <div class="pm-risk-strip__legend">
            <span class="pm-risk-strip__legend-tick" />
            <span>Cập nhật: {{ lastUpdatedLabel }}</span>
          </div>
        </div>

        <!-- ── Student Grid ── -->
        <div
          v-if="viewMode === 'grid'"
          class="pm-grid"
        >
          <div
            v-if="filteredStudents.length === 0"
            class="pm-empty"
          >
            <LucideIcon
              name="users"
              :size="44"
              class="pm-empty__icon"
            />
            <p class="pm-empty__title">
              Không có thí sinh nào
            </p>
            <p class="pm-empty__sub">
              Thử thay đổi bộ lọc hoặc tìm kiếm
            </p>
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
            @resume="openResumeModal(student)"
          />
        </div>

        <!-- ── Student List ── -->
        <div
          v-else
          class="pm-list-view"
        >
          <div
            v-if="filteredStudents.length === 0"
            class="pm-empty"
          >
            <LucideIcon
              name="users"
              :size="44"
              class="pm-empty__icon"
            />
            <p class="pm-empty__title">
              Không có thí sinh nào
            </p>
            <p class="pm-empty__sub">
              Thử thay đổi bộ lọc hoặc tìm kiếm
            </p>
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
            @resume="openResumeModal(student)"
          />
        </div>

        <!-- ── Connection Bar ── -->
        <div class="pm-conn-bar">
          <div class="pm-conn-bar__left">
            <span
              class="pm-conn-indicator"
              :class="isSocketConnected ? 'pm-conn-indicator--live' : 'pm-conn-indicator--poll'"
            >
              <span class="pm-conn-indicator__dot" />
              <LucideIcon
                :name="isSocketConnected ? 'zap' : 'refresh-cw'"
                :size="11"
              />
            </span>
            <span class="pm-conn-bar__text">
              {{ isSocketConnected ? 'Đang kết nối thời gian thực' : `Cập nhật định kỳ · lần cuối ${lastUpdatedLabel}` }}
            </span>
          </div>
          <div class="pm-conn-bar__right">
            <span class="pm-conn-bar__version">v2.0</span>
            <button
              v-if="wsError"
              class="pm-conn-bar__retry"
              @click="retryConnection"
            >
              <LucideIcon
                name="refresh-cw"
                :size="11"
              />
              Kết nối lại
            </button>
          </div>
        </div>
      </div>

      <!-- ── Sidebar ── -->
      <Transition name="pm-sidebar">
        <aside
          v-if="!allPanelsCollapsed"
          class="pm-sidebar"
        >
          <div class="pm-sidebar__header">
            <div class="pm-sidebar__title">
              <LucideIcon
                name="bell-ring"
                :size="16"
              />
              <span>Cảnh báo &amp; Sự kiện</span>
            </div>
            <div class="pm-sidebar__actions">
              <button
                v-if="liveAlerts.length"
                class="pm-sidebar__mark-all"
                @click="handleMarkAllRead"
              >
                Đánh dấu đã đọc
              </button>
              <button
                class="pm-sidebar__clear-btn"
                title="Xóa tất cả"
                @click="handleClearEvents"
              >
                <LucideIcon
                  name="trash-2"
                  :size="13"
                />
              </button>
            </div>
          </div>

          <div class="pm-sidebar__tabs">
            <button
              class="pm-sidebar__tab"
              :class="{ 'pm-sidebar__tab--active': sidebarTab === 'alerts' }"
              @click="sidebarTab = 'alerts'"
            >
              <LucideIcon
                name="alert-triangle"
                :size="13"
              />
              Cảnh báo
              <span
                v-if="liveAlerts.length"
                class="pm-sidebar__tab-count pm-sidebar__tab-count--danger"
              >{{ liveAlerts.length }}</span>
            </button>
            <button
              class="pm-sidebar__tab"
              :class="{ 'pm-sidebar__tab--active': sidebarTab === 'events' }"
              @click="sidebarTab = 'events'"
            >
              <LucideIcon
                name="activity"
                :size="13"
              />
              Sự kiện
              <span
                v-if="recentEvents.length"
                class="pm-sidebar__tab-count"
              >{{ recentEvents.length }}</span>
            </button>
          </div>

          <!-- Alerts content -->
          <div
            v-if="sidebarTab === 'alerts'"
            class="pm-sidebar__content"
          >
            <div
              v-if="liveAlerts.length === 0"
              class="pm-sidebar__empty"
            >
              <LucideIcon
                name="check-circle"
                :size="28"
                class="pm-sidebar__empty-icon"
              />
              <p>Tất cả an toàn</p>
              <span>Không có cảnh báo nào</span>
            </div>
            <TransitionGroup
              v-else
              name="pm-alert"
              tag="div"
              class="pm-alert-list"
            >
              <div
                v-for="alert in liveAlerts"
                :key="alert.id"
                class="pm-alert-item"
                :class="[
                  alert.severity === 'HIGH' || alert.severity === 'CRITICAL' ? 'pm-alert-item--danger' : '',
                  alert.severity === 'MEDIUM' ? 'pm-alert-item--warn' : '',
                  alert.read ? 'pm-alert-item--read' : ''
                ]"
                @click="handleAlertClick(alert)"
              >
                <div class="pm-alert-item__icon-wrap">
                  <LucideIcon
                    name="alert-triangle"
                    :size="14"
                  />
                </div>
                <div class="pm-alert-item__body">
                  <div class="pm-alert-item__header">
                    <span class="pm-alert-item__name">{{ alert.studentName }}</span>
                    <span
                      class="pm-alert-item__score"
                      :class="alert.severity === 'HIGH' || alert.severity === 'CRITICAL' ? 'pm-alert-item__score--danger' : 'pm-alert-item__score--warn'"
                    >
                      {{ alert.riskScore }}đ
                    </span>
                  </div>
                  <p class="pm-alert-item__msg">
                    {{ alert.message }}
                  </p>
                  <span class="pm-alert-item__time">{{ relativeTime(alert.timestamp) }}</span>
                </div>
                <button
                  class="pm-alert-item__dismiss"
                  title="Bỏ qua"
                  @click.stop="handleDismissAlert(alert.id)"
                >
                  <LucideIcon
                    name="x"
                    :size="11"
                  />
                </button>
                <span
                  v-if="!alert.read"
                  class="pm-alert-item__unread"
                />
              </div>
            </TransitionGroup>
          </div>

          <!-- Events content -->
          <div
            v-else
            class="pm-sidebar__content"
          >
            <div
              v-if="recentEvents.length === 0"
              class="pm-sidebar__empty"
            >
              <LucideIcon
                name="activity"
                :size="28"
                class="pm-sidebar__empty-icon"
              />
              <p>Chưa có sự kiện</p>
              <span>Đang chờ sự kiện từ thí sinh...</span>
            </div>
            <TransitionGroup
              v-else
              name="pm-alert"
              tag="div"
              class="pm-alert-list"
            >
              <div
                v-for="event in recentEvents"
                :key="event.id"
                class="pm-event-item"
                @click="handleAlertClick(event)"
              >
                <div
                  class="pm-event-item__icon"
                  :class="`pm-event-item__icon--${eventTypeColor(event.type)}`"
                >
                  <LucideIcon
                    :name="eventTypeIcon(event.type)"
                    :size="12"
                  />
                </div>
                <div class="pm-event-item__body">
                  <p
                    v-if="event.studentName"
                    class="pm-event-item__name"
                  >
                    {{ event.studentName }}
                  </p>
                  <p class="pm-event-item__msg">
                    {{ event.message }}
                  </p>
                </div>
                <span class="pm-event-item__time">{{ relativeTime(event.timestamp) }}</span>
              </div>
            </TransitionGroup>
          </div>
        </aside>
      </Transition>
    </div>

    <!-- ── Action Confirm Modal ── -->
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
import { invalidateAttempt, pauseAttempt, resumeAttempt, sendTeacherWarning, batchWarn, batchPause, batchResume, batchInvalidate } from '../../services/monitoringService'
import { isAttemptPaused } from '../../utils/proctorStatusMeta'
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
const { cards: attempts, lastUpdatedAt } = storeToRefs(store)

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
  },
  resume: {
    variant: 'success', title: 'Cho phép tiếp tục thi',
    message: 'Học sinh sẽ được khôi phục bài thi và có thể tiếp tục làm bài.',
    actionLabel: 'Cho phép tiếp tục', icon: 'play-circle',
    placeholder: 'Để trống sẽ dùng mặc định: Giám thị đã hoàn tất kiểm tra. Bạn có thể tiếp tục làm bài.'
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

function getCountByBand(band) {
  if (band === 'clean') return attempts.value.filter(a => Number(a.riskScore || 0) < 31).length
  if (band === 'suspicious') return attempts.value.filter(a => Number(a.riskScore || 0) >= 31 && Number(a.riskScore || 0) < 61).length
  if (band === 'high') return attempts.value.filter(a => Number(a.riskScore || 0) >= 61 && Number(a.riskScore || 0) < 81).length
  if (band === 'critical') return attempts.value.filter(a => Number(a.riskScore || 0) >= 81).length
  return 0
}

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
const MAX_ALERTS = 50
const MAX_EVENTS = 30

function buildAlert(alert, severity, riskLevel, message) {
  return {
    id: ++alertIdCounter,
    attemptId: alert.attemptId,
    studentName: alert.studentName || '—',
    riskScore: alert.riskScore ?? 0,
    riskLevel: riskLevel || alert.riskLevel || 'UNKNOWN',
    severity,
    message: message || alert.message || alert.type || 'Thông báo',
    timestamp: alert.timestamp || new Date().toISOString(),
    read: false
  }
}

const ALERT_DEDUPE_WINDOW_MS = 10000
function pushAlert(alert) {
  const now = Date.now()
  const existingIndex = liveAlerts.value.findIndex(a =>
    a.attemptId === alert.attemptId
    && a.severity === alert.severity
    && (now - new Date(a.timestamp).getTime()) < ALERT_DEDUPE_WINDOW_MS
  )
  if (existingIndex >= 0) {
    const updated = [...liveAlerts.value]
    updated[existingIndex] = { ...updated[existingIndex], riskScore: alert.riskScore, message: alert.message, timestamp: alert.timestamp, read: false }
    liveAlerts.value = updated
    return
  }
  liveAlerts.value = [alert, ...liveAlerts.value].slice(0, MAX_ALERTS)
}

function patchAttemptLocal(attemptId, patch) {
  if (!attemptId) return
  const target = attempts.value.find(a => (a.id || a.attemptId) === attemptId)
  if (!target) return
  store.upsertCard({ ...target, ...patch, attemptId: target.attemptId || target.id })
}

function handleTeacherAlert(alert) {
  if (!alert || !alert.attemptId) return
  const type = String(alert.type || '').toUpperCase()
  const studentName = alert.studentName || ''

  switch (type) {
    case 'RISK_UPDATED': {
      const severity = alert.riskScore >= 80 ? 'HIGH' : alert.riskScore >= 30 ? 'MEDIUM' : 'LOW'
      pushAlert(buildAlert(alert, severity, alert.riskLevel, `${alert.recommendedAction || 'Rủi ro'}: ${alert.riskScore ?? 0}%`))
      addEvent({ type: 'ALERT', message: [alert.recommendedAction || `Rủi ro thay đổi → ${alert.riskScore ?? 0}%`, ...(alert.reasons || []).slice(0, 1)].filter(Boolean).join(' • '), studentName })
      patchAttemptLocal(alert.attemptId, { riskScore: alert.riskScore, reviewRequired: alert.reviewRequired, reasons: alert.reasons })
      break
    }
    case 'SUSPICIOUS':
      pushAlert(buildAlert(alert, 'MEDIUM', 'SUSPICIOUS', alert.message || 'Phát hiện hành vi đáng ngờ'))
      addEvent({ type: 'ALERT', message: alert.message || 'Hành vi đáng ngờ', studentName })
      break
    case 'WARNING_SENT':
      addEvent({ type: 'WARN', message: `GV gửi cảnh báo: ${alert.message || ''}`, studentName })
      break
    case 'ATTEMPT_STOPPED':
      addEvent({ type: 'INVALIDATE', message: alert.message || 'Bài thi bị đình chỉ', studentName })
      patchAttemptLocal(alert.attemptId, { status: 'STOPPED' })
      break
    case 'ATTEMPT_PAUSED':
      addEvent({ type: 'PAUSE', message: alert.message || 'Bài thi bị tạm dừng', studentName })
      pushAlert(buildAlert(alert, 'MEDIUM', alert.riskLevel || 'PAUSED', alert.message || 'Bài thi bị tạm dừng — chờ giám thị kiểm tra'))
      patchAttemptLocal(alert.attemptId, { status: 'PAUSED' })
      break
    case 'ATTEMPT_RESUMED':
      addEvent({ type: 'RESUME', message: alert.message || 'Bài thi được khôi phục', studentName })
      patchAttemptLocal(alert.attemptId, { status: 'IN_PROGRESS' })
      break
    default:
      pushAlert(buildAlert(alert, 'LOW', 'UNKNOWN'))
      addEvent({ type: 'ALERT', message: alert.message || alert.type, studentName })
  }
}

const EVENT_DEDUPE_WINDOW_MS = 5000
function addEvent(event) {
  const now = Date.now()
  const message = event.message || ''
  const studentName = event.studentName || ''
  const dup = recentEvents.value.find(e =>
    e.type === event.type
    && e.studentName === studentName
    && (e.message || '') === message
    && (now - new Date(e.timestamp).getTime()) < EVENT_DEDUPE_WINDOW_MS
  )
  if (dup) return
  const evt = {
    ...event,
    message,
    studentName,
    id: `${now}-${Math.random().toString(36).slice(2, 8)}`,
    timestamp: new Date().toISOString()
  }
  recentEvents.value = [evt, ...recentEvents.value].slice(0, MAX_EVENTS)
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
  store.setConnectionMode('realtime')
  await rt.connect({
    topics: [{ destination: `/topic/exams/${examId.value}/alerts`, handler: (body) => { handleTeacherAlert(body); void loadAttempts(true) } }],
    reconnectDelay: 3000,
    onConnect: () => { store.setConnectionMode('realtime') },
    onDisconnect: () => { if (examId.value) scheduleReconnect() },
    onError: () => { toast.error('Mất kết nối thời gian thực. Đã chuyển sang cập nhật định kỳ.') }
  })
}
const disconnectRealtime = () => { if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null } rt.disconnect() }
const scheduleReconnect = () => { if (reconnectTimer) return; reconnectTimer = setTimeout(() => { reconnectTimer = null; if (examId.value) void connectRealtime() }, 5000) }

// ── Events & alerts ─────────────────────────────────────────────────────
const handleAlertClick = (alert) => {
  const student = attempts.value.find(s => s.id === alert.attemptId)
  if (student) openStudentDetail(student)
}
const handleDismissAlert = (id) => { liveAlerts.value = liveAlerts.value.filter(a => a.id !== id) }
const handleMarkAllRead = () => { liveAlerts.value = liveAlerts.value.map(a => ({ ...a, read: true })) }
const handleClearEvents = () => { recentEvents.value = [] }

// ── Actions ─────────────────────────────────────────────────────────────
const openActionModal = (student, type) => {
  actionTarget.value = student
  currentActionType = type
  actionModalConfig.value = ACTION_CONFIGS[type]
  showActionModal.value = true
}
const openWarningModal = (student) => openActionModal(student, 'warn')
const openPauseModal = (student) => openActionModal(student, 'pause')
const openResumeModal = (student) => openActionModal(student, 'resume')
const openStudentDetail = (student) => {
  router.push(`/teacher/exams/${examId.value}/monitoring/student/${student.id || student.attemptId}`)
}
const ACTION_HANDLERS = {
  warn: { call: sendTeacherWarning, success: 'Đã gửi cảnh báo tới', error: 'Không gửi được cảnh báo.' },
  pause: { call: pauseAttempt, success: 'Đã tạm dừng bài thi của', error: 'Không thể tạm dừng bài thi.' },
  resume: { call: resumeAttempt, success: 'Đã cho phép tiếp tục bài thi của', error: 'Không thể khôi phục bài thi.' },
  invalidate: { call: invalidateAttempt, success: 'Đã đình chỉ bài thi của', error: 'Không thể đình chỉ bài thi.' }
}

const handleActionConfirm = async (reason) => {
  if (!actionTarget.value) return
  const handler = ACTION_HANDLERS[currentActionType]
  if (!handler) return
  actionLoading.value = true
  try {
    const id = actionTarget.value.id
    const studentName = actionTarget.value.student
    await handler.call(id, reason)
    toast.success(`${handler.success} ${studentName}.`)
    showActionModal.value = false
    actionTarget.value = null
    await loadAttempts()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : handler.error)
  } finally {
    actionLoading.value = false
  }
}
const handleBack = () => router.push('/teacher/live-monitoring')
const handleRefresh = () => { void loadAttempts() }
const clearSelection = () => { selectedIds.value = [] }
const toggleSelect = (id) => { selectedIds.value = selectedIds.value.includes(id) ? selectedIds.value.filter(i => i !== id) : [...selectedIds.value, id] }

// ── Batch ─────────────────────────────────────────────────────────────
const runBatchAction = async ({ ids, call, reason, successMsg, errorMsg, refresh = false }) => {
  if (!ids.length) return
  actionLoading.value = true
  try {
    const result = await call(ids, reason)
    if (result?.succeeded > 0) {
      toast.success(`${successMsg} ${result.succeeded}/${result.total} học sinh.${result.failed > 0 ? ` (${result.failed} thất bại)` : ''}`)
    } else {
      toast.error(errorMsg)
    }
    if (refresh) await loadAttempts()
    selectedIds.value = []
  } catch {
    toast.error(errorMsg)
  } finally {
    actionLoading.value = false
  }
}

const handleBatchWarn = () => runBatchAction({
  ids: selectedIds.value, call: (ids, reason) => batchWarn(ids, reason),
  successMsg: 'Đã gửi cảnh báo tới', errorMsg: 'Không gửi được cảnh báo hàng loạt.'
})
const handleBatchPause = () => runBatchAction({
  ids: selectedIds.value, call: (ids, reason) => batchPause(ids, reason),
  successMsg: 'Đã tạm dừng', errorMsg: 'Không thể tạm dừng hàng loạt.', refresh: true
})
const handleBatchResume = () => runBatchAction({
  ids: selectedIds.value, call: (ids, reason) => batchResume(ids, reason),
  successMsg: 'Đã khôi phục', errorMsg: 'Không thể khôi phục hàng loạt.', refresh: true
})
const handleBatchInvalidate = () => runBatchAction({
  ids: selectedIds.value, call: (ids, reason) => batchInvalidate(ids, reason),
  successMsg: 'Đã đình chỉ', errorMsg: 'Không thể đình chỉ hàng loạt.', refresh: true
})

const hasPausedSelection = computed(() =>
  selectedIds.value.some(id => {
    const s = attempts.value.find(a => (a.id || a.attemptId) === id)
    return s && isAttemptPaused(s.status)
  })
)

const pausedCountInExam = computed(() =>
  attempts.value.filter(a => isAttemptPaused(a.status)).length
)

const handleRestoreAllPaused = () => {
  const pausedIds = attempts.value
    .filter(a => isAttemptPaused(a.status))
    .map(a => a.id || a.attemptId)
  if (pausedIds.length === 0) return
  return runBatchAction({
    ids: pausedIds, call: batchResume, reason: 'Khôi phục hàng loạt',
    successMsg: 'Đã khôi phục', errorMsg: 'Không thể khôi phục hàng loạt.', refresh: true
  })
}

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
watch(() => (route.path === '/teacher/live-monitoring/session' || route.path.startsWith('/teacher/exams/')) ? { ...route.query } : null, (q) => { if (q?.examId) writeMonitoringSessionQuery(q) }, { deep: true, immediate: true })

const resetSessionState = () => {
  store.setCards([])
  store.clearSelection()
  liveAlerts.value = []
  recentEvents.value = []
  selectedIds.value = []
}

watch(examId, async (newId, oldId) => {
  if (!newId || newId === oldId) return
  resetSessionState()
  disconnectRealtime()
  await loadAttempts()
  await connectRealtime()
})

// ── Lifecycle ───────────────────────────────────────────────────────────
onMounted(async () => {
  if (store.selectedExamId && store.selectedExamId !== examId.value) {
    resetSessionState()
  }
  if (examId.value) {
    store.setSelectedExam(examId.value)
  }
  await loadAttempts()
  await connectRealtime()
  refreshTimer = window.setInterval(() => {
    if (document.hidden) return
    if (!isSocketConnected.value) void loadAttempts(true)
  }, 5000)
})
onUnmounted(() => { if (refreshTimer) window.clearInterval(refreshTimer); disconnectRealtime() })
</script>

<style scoped>
/* ── Root ──────────────────────────────────────────────────────────── */
.pm-root {
  min-height: 100vh;
  background: var(--ds-bg);
  color: var(--ds-text);
  display: flex;
  flex-direction: column;
  scroll-behavior: smooth;
}

/* ── Top Command Bar ─────────────────────────────────────────────── */
.pm-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.625rem 1.25rem;
  background: var(--ds-surface);
  border-bottom: 1px solid var(--ds-border);
  position: sticky;
  top: 0;
  z-index: 100;
  gap: 1rem;
  flex-wrap: wrap;
  box-shadow: var(--ds-shadow-xs);
}
.pm-topbar__left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 0;
  flex-shrink: 1;
}
.pm-topbar__center {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}
.pm-topbar__right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

.pm-back-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.4rem 0.875rem;
  border-radius: var(--ds-radius-md);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.775rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: all 0.15s;
}
.pm-back-btn:hover {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border-color: var(--ds-primary-border);
}
.pm-topbar__divider {
  width: 1px;
  height: 24px;
  background: var(--ds-border);
  flex-shrink: 0;
}

.pm-exam-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 0;
}
.pm-exam-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.2rem 0.625rem;
  border-radius: 9999px;
  background: rgba(22, 163, 74, 0.1);
  color: var(--ds-success);
  border: 1px solid rgba(22, 163, 74, 0.2);
  font-size: 0.62rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  flex-shrink: 0;
}
.pm-exam-badge__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ds-success);
  animation: pm-pulse 1.5s ease-in-out infinite;
}
@keyframes pm-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.75); }
}
.pm-exam-meta__title {
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 280px;
}
.pm-exam-meta__sub {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
}

/* Connection status */
.pm-conn-status {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem 0.7rem;
  border-radius: 9999px;
  font-size: 0.7rem;
  font-weight: 700;
  border: 1px solid;
}
.pm-conn-status--live {
  background: rgba(22,163,74,0.08);
  color: var(--ds-success);
  border-color: rgba(22,163,74,0.2);
}
.pm-conn-status--poll {
  background: var(--ds-surface-muted);
  color: var(--ds-text-muted);
  border-color: var(--ds-border);
}
.pm-conn-status__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}
.pm-conn-status--live .pm-conn-status__dot { animation: pm-pulse 1.5s ease-in-out infinite; }

/* Stat pills */
.pm-stat-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem 0.7rem;
  border-radius: 9999px;
  font-size: 0.72rem;
  font-weight: 700;
  border: 1px solid;
}
.pm-stat-pill__value {
  font-weight: 900;
  font-variant-numeric: tabular-nums;
}
.pm-stat-pill__label { font-weight: 600; opacity: 0.8; }
.pm-stat-pill--online {
  background: rgba(22,163,74,0.08);
  color: var(--ds-success);
  border-color: rgba(22,163,74,0.2);
}
.pm-stat-pill--warn {
  background: rgba(245,158,11,0.08);
  color: var(--ds-warning);
  border-color: rgba(245,158,11,0.2);
}
.pm-stat-pill--submit {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border-color: var(--ds-primary-border);
}
.pm-stat-pill--neutral {
  background: var(--ds-surface-muted);
  color: var(--ds-text-secondary);
  border-color: var(--ds-border);
}

/* Timer */
.pm-timer {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem 0.8rem;
  border-radius: var(--ds-radius-md);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  font-size: 0.78rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  border: 1px solid var(--ds-primary-border);
}

/* Icon buttons */
.pm-icon-btn {
  position: relative;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--ds-radius-md);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
}
.pm-icon-btn:hover {
  background: var(--ds-surface-muted);
  color: var(--ds-text);
  border-color: var(--ds-primary-border);
}
.pm-icon-btn--active {
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
}
.pm-icon-btn--primary {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: #fff;
}
.pm-icon-btn--primary:hover {
  background: var(--ds-primary-hover);
  color: #fff;
}
.pm-icon-btn__badge {
  position: absolute;
  top: -5px;
  right: -5px;
  min-width: 16px;
  height: 16px;
  border-radius: 9999px;
  background: var(--ds-danger);
  color: #fff;
  font-size: 0.6rem;
  font-weight: 900;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 3px;
  border: 1.5px solid var(--ds-surface);
}
.pm-spin { animation: pm-spin 0.8s linear infinite; }
@keyframes pm-spin { to { transform: rotate(360deg); } }

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
.pm-main::-webkit-scrollbar { width: 6px; }
.pm-main::-webkit-scrollbar-track { background: transparent; }
.pm-main::-webkit-scrollbar-thumb { background: var(--ds-gray-200); border-radius: 3px; }
.pm-main::-webkit-scrollbar-thumb:hover { background: var(--ds-gray-300); }

/* ── Filter Bar ──────────────────────────────────────────────────── */
.pm-filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
}
.pm-filter-tabs {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  padding: 0.25rem;
  box-shadow: var(--ds-shadow-xs);
}
.pm-filter-tab {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.8rem;
  border-radius: var(--ds-radius-sm);
  border: none;
  background: transparent;
  color: var(--ds-text-secondary);
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.pm-filter-tab:hover { color: var(--ds-text); background: var(--ds-surface-muted); }
.pm-filter-tab--active { background: var(--ds-primary); color: #fff; }
.pm-filter-tab__count {
  min-width: 18px;
  height: 18px;
  border-radius: 9999px;
  background: var(--ds-surface-muted);
  color: var(--ds-text-secondary);
  font-size: 0.65rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
}
.pm-filter-tab--active .pm-filter-tab__count { background: rgba(255,255,255,0.25); color: #fff; }

.pm-filter-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

/* Search */
.pm-search-box {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.8rem;
  border-radius: var(--ds-radius-md);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  transition: all 0.15s;
}
.pm-search-box:focus-within { border-color: var(--ds-primary); box-shadow: var(--ds-shadow-focus); }
.pm-search-box__icon { color: var(--ds-text-muted); flex-shrink: 0; }
.pm-search-box__input {
  border: none;
  background: transparent;
  color: var(--ds-text);
  font-size: 0.82rem;
  outline: none;
  width: 180px;
}
.pm-search-box__input::placeholder { color: var(--ds-text-muted); }
.pm-search-box__clear {
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  padding: 0;
  transition: color 0.15s;
}
.pm-search-box__clear:hover { color: var(--ds-text); }

/* View toggle */
.pm-view-toggle {
  display: flex;
  gap: 0.2rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  padding: 0.2rem;
  box-shadow: var(--ds-shadow-xs);
}
.pm-view-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--ds-radius-sm);
  border: none;
  background: transparent;
  color: var(--ds-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
}
.pm-view-btn:hover { background: var(--ds-surface-muted); color: var(--ds-text); }
.pm-view-btn--active { background: var(--ds-primary); color: #fff; }

/* ── Batch Bar ──────────────────────────────────────────────────── */
.pm-batch-bar {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1rem;
  background: linear-gradient(135deg, rgba(79,70,229,0.08) 0%, rgba(79,70,229,0.04) 100%);
  border: 1px solid var(--ds-primary-border);
  border-radius: var(--ds-radius-lg);
  flex-wrap: wrap;
}
.pm-batch-bar__info {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--ds-primary);
  font-size: 0.82rem;
  font-weight: 700;
}
.pm-batch-bar__actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-left: auto;
}
.pm-batch-bar__sep {
  width: 1px;
  background: var(--ds-border);
  margin: 0 0.25rem;
}
.pm-batch-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.4rem 0.875rem;
  border-radius: var(--ds-radius-md);
  font-size: 0.76rem;
  font-weight: 700;
  cursor: pointer;
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  transition: all 0.15s;
}
.pm-batch-btn:hover { color: var(--ds-text); border-color: var(--ds-primary-border); background: var(--ds-surface-muted); }
.pm-batch-btn--warn { color: var(--ds-warning); border-color: var(--ds-warning-soft); background: var(--ds-warning-bg); }
.pm-batch-btn--warn:hover { background: var(--ds-warning); color: #fff; border-color: var(--ds-warning); }
.pm-batch-btn--pause { color: var(--ds-primary); border-color: var(--ds-primary-border); background: var(--ds-primary-soft); }
.pm-batch-btn--pause:hover { background: var(--ds-primary); color: #fff; border-color: var(--ds-primary); }
.pm-batch-btn--resume { color: var(--ds-success); border-color: var(--ds-success-soft); background: var(--ds-success-bg); }
.pm-batch-btn--resume:hover { background: var(--ds-success); color: #fff; border-color: var(--ds-success); }
.pm-batch-btn--danger { color: var(--ds-danger); border-color: var(--ds-danger-soft); background: var(--ds-danger-bg); }
.pm-batch-btn--danger:hover { background: var(--ds-danger); color: #fff; border-color: var(--ds-danger); }
.pm-batch-btn--ghost { background: transparent; color: var(--ds-text-secondary); border-color: var(--ds-border); }
.pm-batch-btn--ghost:hover { background: var(--ds-surface-muted); color: var(--ds-text); }
.pm-batch-btn--restore {
  background: transparent;
  color: var(--ds-text-secondary);
  border-color: var(--ds-border);
  font-weight: 600;
}
.pm-batch-btn--restore:hover { background: var(--ds-surface-muted); color: var(--ds-text); }

/* ── Risk Strip ─────────────────────────────────────────────────── */
.pm-risk-strip {
  display: flex;
  align-items: center;
  gap: 0;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  overflow: hidden;
  box-shadow: var(--ds-shadow-xs);
}
.pm-risk-strip__item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  padding: 0.75rem 1rem;
  border-right: 1px solid var(--ds-border);
  transition: background 0.15s;
}
.pm-risk-strip__item:last-of-type { border-right: none; }
.pm-risk-strip__item:hover { background: var(--ds-surface-muted); }
.pm-risk-strip__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.pm-risk-strip__item--clean .pm-risk-strip__dot { background: var(--ds-success); }
.pm-risk-strip__item--suspicious .pm-risk-strip__dot { background: var(--ds-warning); }
.pm-risk-strip__item--high .pm-risk-strip__dot { background: var(--mon-risk-high); }
.pm-risk-strip__item--critical .pm-risk-strip__dot { background: var(--ds-danger); animation: pm-pulse 1.5s ease-in-out infinite; }
.pm-risk-strip__label {
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
  flex: 1;
}
.pm-risk-strip__count {
  font-size: 1rem;
  font-weight: 900;
  color: var(--ds-text);
  font-variant-numeric: tabular-nums;
}
.pm-risk-strip__legend {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  white-space: nowrap;
  flex-shrink: 0;
}
.pm-risk-strip__legend-tick {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--ds-primary);
}

/* ── Grid ────────────────────────────────────────────────────────── */
.pm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 0.875rem;
  content-visibility: auto;
  contain-intrinsic-size: 0 160px;
}

/* ── List View ─────────────────────────────────────────────────── */
.pm-list-view {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

/* ── Empty ─────────────────────────────────────────────────────── */
.pm-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 4rem 2rem;
  color: var(--ds-text-muted);
  font-size: 0.875rem;
  text-align: center;
}
.pm-empty__icon { color: var(--ds-gray-300); margin-bottom: 0.5rem; }
.pm-empty__title { font-size: 1rem; font-weight: 700; color: var(--ds-text); margin: 0; }
.pm-empty__sub { font-size: 0.825rem; margin: 0; }

/* ── Connection Bar ─────────────────────────────────────────────── */
.pm-conn-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.6rem 0.875rem;
  border-radius: var(--ds-radius-md);
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  margin-top: auto;
  box-shadow: var(--ds-shadow-xs);
}
.pm-conn-bar__left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.pm-conn-bar__text {
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}
.pm-conn-bar__right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.pm-conn-bar__version {
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  background: var(--ds-surface-muted);
  padding: 0.1rem 0.4rem;
  border-radius: 9999px;
}
.pm-conn-indicator {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.7rem;
  font-weight: 700;
}
.pm-conn-indicator__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}
.pm-conn-indicator--live { color: var(--ds-success); }
.pm-conn-indicator--live .pm-conn-indicator__dot { animation: pm-pulse 2s ease-in-out infinite; }
.pm-conn-indicator--poll { color: var(--ds-text-muted); }
.pm-conn-bar__retry {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--ds-danger);
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: var(--ds-radius-sm);
  transition: background 0.15s;
}
.pm-conn-bar__retry:hover { background: var(--ds-danger-bg); }

/* ── Sidebar ───────────────────────────────────────────────────── */
.pm-sidebar {
  width: 340px;
  flex-shrink: 0;
  border-left: 1px solid var(--ds-border);
  background: var(--ds-surface);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.pm-sidebar__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.875rem 1rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-surface-muted);
  gap: 0.5rem;
}
.pm-sidebar__title {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.825rem;
  font-weight: 800;
  color: var(--ds-text);
}
.pm-sidebar__actions {
  display: flex;
  gap: 0.375rem;
  align-items: center;
}
.pm-sidebar__mark-all {
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--ds-primary);
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0.25rem 0.55rem;
  border-radius: var(--ds-radius-sm);
  transition: background 0.15s;
}
.pm-sidebar__mark-all:hover { background: var(--ds-primary-soft); }
.pm-sidebar__clear-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--ds-radius-sm);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
}
.pm-sidebar__clear-btn:hover {
  background: var(--ds-danger-bg);
  border-color: var(--ds-danger-soft);
  color: var(--ds-danger);
}

/* Sidebar tabs */
.pm-sidebar__tabs {
  display: flex;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-surface);
}
.pm-sidebar__tab {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  padding: 0.7rem;
  border: none;
  background: transparent;
  color: var(--ds-text-secondary);
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  border-bottom: 2px solid transparent;
}
.pm-sidebar__tab:hover { color: var(--ds-text); background: var(--ds-surface-muted); }
.pm-sidebar__tab--active {
  color: var(--ds-primary);
  border-bottom-color: var(--ds-primary);
  background: var(--ds-primary-soft);
}
.pm-sidebar__tab-count {
  min-width: 18px;
  height: 18px;
  border-radius: 9999px;
  background: var(--ds-surface-muted);
  color: var(--ds-text-secondary);
  font-size: 0.62rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
}
.pm-sidebar__tab-count--danger {
  background: var(--ds-danger-bg);
  color: var(--ds-danger);
}
.pm-sidebar__tab--active .pm-sidebar__tab-count { background: var(--ds-primary); color: #fff; }

/* Sidebar content */
.pm-sidebar__content {
  flex: 1;
  overflow-y: auto;
}
.pm-sidebar__content::-webkit-scrollbar { width: 4px; }
.pm-sidebar__content::-webkit-scrollbar-track { background: transparent; }
.pm-sidebar__content::-webkit-scrollbar-thumb { background: var(--ds-gray-200); border-radius: 2px; }
.pm-sidebar__content::-webkit-scrollbar-thumb:hover { background: var(--ds-gray-300); }
.pm-sidebar__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  padding: 3rem 1rem;
  color: var(--ds-text-muted);
  font-size: 0.82rem;
  text-align: center;
}
.pm-sidebar__empty-icon { margin-bottom: 0.25rem; }
.pm-sidebar__empty p { margin: 0; font-weight: 700; font-size: 0.875rem; color: var(--ds-text); }
.pm-sidebar__empty span { font-size: 0.75rem; }

/* Alert list */
.pm-alert-list { display: flex; flex-direction: column; }
.pm-alert-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border-bottom: 1px solid var(--ds-border);
  cursor: pointer;
  transition: background 0.12s;
  position: relative;
  border-left: 3px solid transparent;
}
.pm-alert-item:hover { background: var(--ds-surface-muted); }
.pm-alert-item--read { opacity: 0.55; }
.pm-alert-item--danger { border-left-color: var(--ds-danger); }
.pm-alert-item--warn { border-left-color: var(--ds-warning); }
.pm-alert-item__icon-wrap {
  width: 30px;
  height: 30px;
  border-radius: var(--ds-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ds-danger-bg);
  color: var(--ds-danger);
  flex-shrink: 0;
}
.pm-alert-item--warn .pm-alert-item__icon-wrap { background: var(--ds-warning-bg); color: var(--ds-warning); }
.pm-alert-item__body { flex: 1; min-width: 0; }
.pm-alert-item__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}
.pm-alert-item__name { font-size: 0.8rem; font-weight: 700; color: var(--ds-text); margin: 0; }
.pm-alert-item__score {
  font-size: 0.68rem;
  font-weight: 800;
  padding: 0.15rem 0.5rem;
  border-radius: 9999px;
  flex-shrink: 0;
}
.pm-alert-item__score--danger { background: var(--ds-danger-bg); color: var(--ds-danger); }
.pm-alert-item__score--warn { background: var(--ds-warning-bg); color: var(--ds-warning); }
.pm-alert-item__msg {
  font-size: 0.74rem;
  color: var(--ds-text-secondary);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pm-alert-item__time {
  font-size: 0.66rem;
  color: var(--ds-text-muted);
  white-space: nowrap;
  margin-top: 0.3rem;
  display: block;
}
.pm-alert-item__dismiss {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--ds-text-muted);
  cursor: pointer;
  transition: all 0.15s;
  flex-shrink: 0;
}
.pm-alert-item__dismiss:hover { background: var(--ds-surface-muted); color: var(--ds-text); }
.pm-alert-item__unread {
  position: absolute;
  top: 0.875rem;
  right: 0.5rem;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ds-primary);
}

/* Event items */
.pm-event-item {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--ds-border);
  cursor: pointer;
  transition: background 0.12s;
}
.pm-event-item:hover { background: var(--ds-surface-muted); }
.pm-event-item__icon {
  width: 28px;
  height: 28px;
  border-radius: var(--ds-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.pm-event-item__icon--warn { background: var(--ds-warning-bg); color: var(--ds-warning); }
.pm-event-item__icon--danger { background: var(--ds-danger-bg); color: var(--ds-danger); }
.pm-event-item__icon--success { background: var(--ds-success-bg); color: var(--ds-success); }
.pm-event-item__icon--info { background: var(--ds-primary-soft); color: var(--ds-primary); }
.pm-event-item__icon--neutral { background: var(--ds-surface-muted); color: var(--ds-text-secondary); }
.pm-event-item__body { flex: 1; min-width: 0; }
.pm-event-item__name { font-size: 0.78rem; font-weight: 700; color: var(--ds-text); margin: 0 0 0.1rem; }
.pm-event-item__msg { font-size: 0.74rem; color: var(--ds-text-secondary); margin: 0; }
.pm-event-item__time {
  font-size: 0.66rem;
  color: var(--ds-text-muted);
  white-space: nowrap;
  flex-shrink: 0;
  margin-top: 0.2rem;
  font-variant-numeric: tabular-nums;
}

/* Transitions */
.pm-slide-enter-active, .pm-slide-leave-active { transition: all 0.2s ease; }
.pm-slide-enter-from, .pm-slide-leave-to { opacity: 0; transform: translateY(-8px); }
.pm-sidebar-enter-active, .pm-sidebar-leave-active { transition: all 0.25s ease; }
.pm-sidebar-enter-from, .pm-sidebar-leave-to { opacity: 0; transform: translateX(20px); }
.pm-alert-enter-active { transition: all 0.3s ease; }
.pm-alert-enter-from { opacity: 0; transform: translateX(-12px); }

/* ── Responsive ─────────────────────────────────────────────────── */
@media (max-width: 1024px) {
  .pm-sidebar { width: 280px; }
  .pm-topbar__center { gap: 0.375rem; }
}
@media (max-width: 768px) {
  .pm-layout { flex-direction: column; }
  .pm-sidebar { width: 100%; border-left: none; border-top: 1px solid var(--ds-border); }
  .pm-topbar { flex-wrap: wrap; }
  .pm-topbar__center { order: 3; width: 100%; justify-content: center; }
}
</style>
