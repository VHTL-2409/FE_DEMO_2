<template>
  <main class="ai-dashboard">
    <header class="ai-dashboard__header">
      <div class="ai-dashboard__header-left">
        <button
          type="button"
          class="ai-dashboard__back-btn"
          title="Quay lại"
          @click="$router.back()"
        >
          <LucideIcon name="arrow-left" :size="18" />
        </button>
        <div>
          <h1 class="ai-dashboard__title">Giám sát camera AI</h1>
          <p class="ai-dashboard__subtitle">
            {{ examTitle }} · <span :class="connectionClass">{{ connectionLabel }}</span>
          </p>
        </div>
      </div>

      <div class="ai-dashboard__header-right">
        <div class="ai-dashboard__stats-summary">
          <div class="ai-dashboard__stat-item">
            <span class="ai-dashboard__stat-value">{{ totalStudents }}</span>
            <span class="ai-dashboard__stat-label">Tổng</span>
          </div>
          <div class="ai-dashboard__stat-item ai-dashboard__stat-item--ok">
            <span class="ai-dashboard__stat-value">{{ okCount }}</span>
            <span class="ai-dashboard__stat-label">OK</span>
          </div>
          <div class="ai-dashboard__stat-item ai-dashboard__stat-item--warn">
            <span class="ai-dashboard__stat-value">{{ warningCount }}</span>
            <span class="ai-dashboard__stat-label">Cảnh báo</span>
          </div>
          <div class="ai-dashboard__stat-item ai-dashboard__stat-item--danger">
            <span class="ai-dashboard__stat-value">{{ criticalCount }}</span>
            <span class="ai-dashboard__stat-label">Nguy hiểm</span>
          </div>
        </div>

        <button
          type="button"
          class="ai-dashboard__refresh-btn"
          :disabled="loading"
          @click="refreshData"
        >
          <LucideIcon name="refresh-cw" :size="16" :class="{ 'ai-dashboard__spin': loading }" />
          {{ loading ? 'Đang tải...' : 'Làm mới' }}
        </button>
      </div>
    </header>

    <div class="ai-dashboard__filters">
      <select v-model="statusFilter" class="ai-dashboard__select" aria-label="Lọc trạng thái">
        <option value="">Tất cả trạng thái</option>
        <option value="OK">OK</option>
        <option value="WARNING">Cảnh báo</option>
        <option value="CRITICAL">Nguy hiểm</option>
        <option value="NO_CAMERA">Không có camera</option>
      </select>

      <select v-model="signalFilter" class="ai-dashboard__select" aria-label="Lọc tín hiệu">
        <option value="">Tất cả tín hiệu</option>
        <option
          v-for="signal in signalOptions"
          :key="signal"
          :value="signal"
        >
          {{ formatSignalType(signal) }}
        </option>
      </select>

      <select v-model="sortBy" class="ai-dashboard__select" aria-label="Sắp xếp">
        <option value="risk">Theo rủi ro</option>
        <option value="recent">Mới nhất</option>
        <option value="name">Theo tên</option>
      </select>

      <div class="ai-dashboard__search">
        <LucideIcon name="search" :size="16" class="ai-dashboard__search-icon" />
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Tìm thí sinh..."
          class="ai-dashboard__search-input"
        />
      </div>

      <button
        type="button"
        class="ai-dashboard__clear-btn"
        :disabled="!hasActiveFilters"
        @click="clearFilters"
      >
        Xóa lọc
      </button>
    </div>

    <div class="ai-dashboard__content" :class="{ 'ai-dashboard__content--with-alerts': visibleAlerts.length }">
      <section class="ai-dashboard__main" aria-label="Danh sách camera">
        <div v-if="loading && !cameraStatuses.length" class="ai-dashboard__loading">
          <div class="ai-dashboard__spinner" />
          <p>Đang tải dữ liệu camera...</p>
        </div>

        <div v-else-if="!filteredCameraStatuses.length" class="ai-dashboard__empty">
          <LucideIcon name="camera-off" :size="48" />
          <p>Không có dữ liệu camera phù hợp</p>
        </div>

        <div v-else class="ai-dashboard__grid">
          <CameraStatusCard
            v-for="camera in filteredCameraStatuses"
            :key="camera.attemptId"
            :camera="camera"
            @click="openStudentDetail(camera)"
          />
        </div>
      </section>

      <aside v-if="visibleAlerts.length" class="ai-dashboard__alerts">
        <div class="ai-dashboard__alerts-header">
          <h3>
            <LucideIcon name="alert-triangle" :size="16" />
            Cảnh báo gần đây
          </h3>
          <span class="ai-dashboard__alerts-count">{{ visibleAlerts.length }}</span>
        </div>

        <div class="ai-dashboard__alerts-list">
          <article
            v-for="alert in visibleAlerts"
            :key="alert.id"
            class="ai-dashboard__alert-item"
            :class="`ai-dashboard__alert-item--${String(alert.severity || '').toLowerCase()}`"
          >
            <div class="ai-dashboard__alert-info">
              <span class="ai-dashboard__alert-student">{{ alert.studentName || 'Chưa rõ thí sinh' }}</span>
              <span class="ai-dashboard__alert-signal">{{ formatSignalType(alert.signalType) }}</span>
              <span v-if="alert.riskImpact != null" class="ai-dashboard__alert-risk">
                +{{ alert.riskImpact }} rủi ro
              </span>
            </div>
            <span class="ai-dashboard__alert-time">{{ formatTime(alert.createdAt) }}</span>
            <div class="ai-dashboard__alert-actions">
              <button
                type="button"
                class="ai-dashboard__alert-action"
                title="Mở chi tiết thí sinh"
                @click.stop="openStudentDetailByAlert(alert)"
              >
                <LucideIcon name="eye" :size="14" />
              </button>
              <button
                type="button"
                class="ai-dashboard__alert-action"
                title="Bỏ qua cảnh báo"
                @click.stop="dismissAlertHandler(alert.id)"
              >
                <LucideIcon name="x" :size="14" />
              </button>
            </div>
          </article>
        </div>
      </aside>
    </div>
  </main>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LucideIcon from '../../common/LucideIcon.vue'
import CameraStatusCard from './CameraStatusCard.vue'
import { useAiCameraDashboard } from '../../../composables/useAiCameraDashboard'
import { useToast } from '../../../composables/useToast'
import { normalizeSignalType, uniqueSignalTypes } from '../../../utils/proctorSignalTypes'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const examId = computed(() => route.params.examId)
const examTitle = computed(() => route.query.title || 'Bài thi')

const {
  loading,
  cameraStatuses,
  recentAlerts,
  isConnected,
  connectionMode,
  connectionLabel,
  totalStudents,
  okCount,
  warningCount,
  criticalCount,
  refreshData,
  dismissAlert
} = useAiCameraDashboard(examId)

const statusFilter = ref('')
const signalFilter = ref('')
const sortBy = ref('risk')
const searchQuery = ref('')

const connectionClass = computed(() => {
  if (isConnected.value || connectionMode.value === 'websocket') return 'ai-dashboard__connection--live'
  if (connectionMode.value === 'polling') return 'ai-dashboard__connection--polling'
  return 'ai-dashboard__connection--lost'
})

const signalOptions = computed(() => {
  const signals = cameraStatuses.value.flatMap(camera => camera.activeSignals || [])
  const alertSignals = recentAlerts.value.map(alert => alert.signalType)
  return uniqueSignalTypes([...signals, ...alertSignals]).sort()
})

const hasActiveFilters = computed(() =>
  Boolean(statusFilter.value || signalFilter.value || searchQuery.value || sortBy.value !== 'risk')
)

const filteredCameraStatuses = computed(() => {
  let result = [...cameraStatuses.value]

  if (statusFilter.value) {
    if (statusFilter.value === 'NO_CAMERA') {
      result = result.filter(camera => !camera.cameraActive)
    } else {
      result = result.filter(camera => camera.status === statusFilter.value)
    }
  }

  if (signalFilter.value) {
    result = result.filter(camera =>
      uniqueSignalTypes(camera.activeSignals || []).includes(signalFilter.value)
    )
  }

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(camera =>
      camera.studentName?.toLowerCase().includes(query) ||
      camera.studentCode?.toLowerCase().includes(query)
    )
  }

  switch (sortBy.value) {
    case 'risk':
      result.sort((a, b) => (b.riskScore || 0) - (a.riskScore || 0))
      break
    case 'name':
      result.sort((a, b) => (a.studentName || '').localeCompare(b.studentName || '', 'vi'))
      break
    case 'recent':
      result.sort((a, b) => new Date(b.lastFrameAt || b.lastUpdate || 0) - new Date(a.lastFrameAt || a.lastUpdate || 0))
      break
  }

  return result
})

const visibleAlerts = computed(() => {
  if (!signalFilter.value) return recentAlerts.value
  return recentAlerts.value.filter(alert => normalizeSignalType(alert.signalType) === signalFilter.value)
})

function clearFilters() {
  statusFilter.value = ''
  signalFilter.value = ''
  sortBy.value = 'risk'
  searchQuery.value = ''
}

async function dismissAlertHandler(alertId) {
  const success = await dismissAlert(alertId)
  if (success) {
    toast.success('Đã bỏ qua cảnh báo')
  } else {
    toast.error('Không thể bỏ qua cảnh báo')
  }
}

function openStudentDetail(camera) {
  if (!camera?.attemptId) return
  router.push(`/teacher/exams/${examId.value}/monitoring/student/${camera.attemptId}`)
}

function openStudentDetailByAlert(alert) {
  openStudentDetail(alert)
}

function formatSignalType(signal) {
  const signalType = normalizeSignalType(signal)
  const labelMap = {
    NO_CAMERA: 'Camera tắt',
    NO_MIC: 'Micro tắt',
    FACE_NOT_DETECTED: 'Không thấy mặt',
    MULTIPLE_FACES: 'Nhiều mặt',
    IDENTITY_FACE_MISMATCH: 'Mặt không khớp',
    FACE_SPOOFING_SUSPECTED: 'Nghi giả mạo',
    FACE_OBSTRUCTED_MASK: 'Che mặt',
    EYES_OBSTRUCTED: 'Mắt bị che',
    EYES_NOT_DETECTED: 'Không thấy mắt',
    EYES_CLOSED_PROLONGED: 'Nhắm mắt lâu',
    GAZE_OFF_SCREEN: 'Nhìn lệch',
    RAPID_EYE_MOVEMENT: 'Mắt đảo nhanh',
    AI_SPEAKING_DETECTED: 'Có tiếng nói',
    VERY_LOW_LIGHTING: 'Rất tối',
    LOW_LIGHTING: 'Thiếu sáng',
    OVEREXPOSED_FRAME: 'Cháy sáng',
    VERY_BLURRY_FRAME: 'Rất mờ',
    BLURRY_FRAME: 'Mờ',
    FACE_TURNED_AWAY: 'Quay mặt đi',
    PARTIAL_FACE_VISIBLE: 'Mặt không rõ',
    FACE_TOO_FAR: 'Quá xa',
    FACE_TOO_CLOSE: 'Quá gần',
    FACE_NOT_CENTERED: 'Lệch tâm',
    SCREEN_REPLAY: 'Phát lại màn hình',
    SCREEN_DISPLAY: 'Màn hình khác',
    LOW_LIVENESS: 'Nghi không phải người thật',
    PRINTED_PHOTO: 'Ảnh in',
    FLAT_IMAGE: 'Ảnh phẳng',
    DEEPFAKE: 'Deepfake'
  }
  return labelMap[signalType] || String(signalType || '').replace(/_/g, ' ')
}

function formatTime(timestamp) {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}
</script>

<style scoped>
.ai-dashboard {
  min-height: 100vh;
  padding: var(--ds-space-5);
  background: var(--ds-bg);
  font-family: var(--ds-font);
}

.ai-dashboard__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--ds-space-4);
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  margin-bottom: var(--ds-space-4);
  gap: var(--ds-space-4);
  flex-wrap: wrap;
}

.ai-dashboard__header-left,
.ai-dashboard__header-right,
.ai-dashboard__stats-summary,
.ai-dashboard__alerts-header h3,
.ai-dashboard__refresh-btn,
.ai-dashboard__alert-actions,
.ai-dashboard__alert-action,
.ai-dashboard__back-btn {
  display: flex;
  align-items: center;
}

.ai-dashboard__header-left {
  gap: var(--ds-space-3);
}

.ai-dashboard__header-right {
  gap: var(--ds-space-4);
}

.ai-dashboard__back-btn,
.ai-dashboard__alert-action {
  justify-content: center;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  background: var(--ds-surface-muted);
  color: var(--ds-text-secondary);
  cursor: pointer;
  transition: all var(--ds-duration-base);
}

.ai-dashboard__back-btn {
  width: 40px;
  height: 40px;
}

.ai-dashboard__back-btn:hover,
.ai-dashboard__alert-action:hover {
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
}

.ai-dashboard__title {
  margin: 0;
  font-size: var(--ds-text-xl);
  font-weight: 800;
  color: var(--ds-text);
}

.ai-dashboard__subtitle {
  margin: 2px 0 0;
  font-size: var(--ds-text-sm);
  color: var(--ds-text-secondary);
}

.ai-dashboard__connection--live {
  color: var(--ds-success);
  font-weight: 700;
}

.ai-dashboard__connection--polling {
  color: var(--ds-warning);
  font-weight: 700;
}

.ai-dashboard__connection--lost {
  color: var(--ds-danger);
  font-weight: 700;
}

.ai-dashboard__stats-summary {
  gap: var(--ds-space-3);
}

.ai-dashboard__stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--ds-space-2) var(--ds-space-3);
  background: var(--ds-gray-50);
  border-radius: var(--ds-radius-md);
  min-width: 60px;
}

.ai-dashboard__stat-item--ok {
  background: var(--ds-success-soft);
}

.ai-dashboard__stat-item--warn {
  background: var(--ds-warning-soft);
}

.ai-dashboard__stat-item--danger {
  background: var(--ds-danger-soft);
}

.ai-dashboard__stat-value {
  font-size: var(--ds-text-xl);
  font-weight: 800;
  color: var(--ds-text);
}

.ai-dashboard__stat-label {
  font-size: var(--ds-text-xs);
  color: var(--ds-text-secondary);
  font-weight: 600;
}

.ai-dashboard__refresh-btn,
.ai-dashboard__clear-btn,
.ai-dashboard__select,
.ai-dashboard__search-input {
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  background: var(--ds-surface);
  color: var(--ds-text);
  font-size: var(--ds-text-sm);
}

.ai-dashboard__refresh-btn,
.ai-dashboard__clear-btn {
  gap: var(--ds-space-2);
  padding: var(--ds-space-2) var(--ds-space-4);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--ds-duration-base);
}

.ai-dashboard__refresh-btn:hover:not(:disabled),
.ai-dashboard__clear-btn:hover:not(:disabled) {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
}

.ai-dashboard__refresh-btn:disabled,
.ai-dashboard__clear-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ai-dashboard__spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.ai-dashboard__filters {
  display: flex;
  gap: var(--ds-space-3);
  margin-bottom: var(--ds-space-4);
  flex-wrap: wrap;
}

.ai-dashboard__select {
  padding: var(--ds-space-2) var(--ds-space-4);
  font-weight: 600;
  cursor: pointer;
  min-width: 160px;
}

.ai-dashboard__search {
  flex: 1;
  min-width: 220px;
  max-width: 360px;
  position: relative;
}

.ai-dashboard__search-icon {
  position: absolute;
  left: var(--ds-space-3);
  top: 50%;
  transform: translateY(-50%);
  color: var(--ds-text-muted);
}

.ai-dashboard__search-input {
  width: 100%;
  padding: var(--ds-space-2) var(--ds-space-3) var(--ds-space-2) calc(var(--ds-space-3) + 24px);
  outline: none;
  transition: border-color var(--ds-duration-base);
}

.ai-dashboard__search-input:focus {
  border-color: var(--ds-primary);
}

.ai-dashboard__content {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--ds-space-4);
  align-items: start;
}

.ai-dashboard__content--with-alerts {
  grid-template-columns: minmax(0, 1fr) 340px;
}

.ai-dashboard__main {
  min-width: 0;
}

.ai-dashboard__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--ds-space-4);
}

.ai-dashboard__loading,
.ai-dashboard__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--ds-space-3);
  padding: var(--ds-space-10);
  color: var(--ds-text-muted);
}

.ai-dashboard__spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--ds-gray-200);
  border-top-color: var(--ds-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.ai-dashboard__alerts {
  position: sticky;
  top: var(--ds-space-4);
  max-height: calc(100vh - 120px);
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  box-shadow: var(--ds-shadow-sm);
  overflow: hidden;
}

.ai-dashboard__alerts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--ds-space-3) var(--ds-space-4);
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.ai-dashboard__alerts-header h3 {
  gap: var(--ds-space-2);
  margin: 0;
  font-size: var(--ds-text-sm);
  font-weight: 700;
  color: var(--ds-text);
}

.ai-dashboard__alerts-count {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 6px;
  background: var(--ds-danger);
  color: white;
  border-radius: 9999px;
  font-size: var(--ds-text-xs);
  font-weight: 800;
}

.ai-dashboard__alerts-list {
  max-height: calc(100vh - 190px);
  overflow-y: auto;
  padding: var(--ds-space-2);
}

.ai-dashboard__alert-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: var(--ds-space-2);
  padding: var(--ds-space-2) var(--ds-space-3);
  border-radius: var(--ds-radius-md);
  margin-bottom: var(--ds-space-1);
  font-size: var(--ds-text-xs);
}

.ai-dashboard__alert-item--critical {
  background: var(--ds-danger-soft);
  border-left: 3px solid var(--ds-danger);
}

.ai-dashboard__alert-item--high {
  background: var(--ds-warning-soft);
  border-left: 3px solid var(--ds-warning);
}

.ai-dashboard__alert-item--medium {
  background: var(--ds-info-soft);
  border-left: 3px solid var(--ds-info);
}

.ai-dashboard__alert-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.ai-dashboard__alert-student,
.ai-dashboard__alert-signal {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ai-dashboard__alert-student {
  font-weight: 700;
  color: var(--ds-text);
}

.ai-dashboard__alert-signal {
  color: var(--ds-text-secondary);
}

.ai-dashboard__alert-risk {
  width: fit-content;
  padding: 1px 6px;
  border-radius: var(--ds-radius-full);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  font-weight: 800;
  line-height: 1.35;
  white-space: nowrap;
}

.ai-dashboard__alert-time {
  color: var(--ds-text-muted);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.ai-dashboard__alert-actions {
  gap: 4px;
}

.ai-dashboard__alert-action {
  width: 26px;
  height: 26px;
}

@media (max-width: 1024px) {
  .ai-dashboard__content,
  .ai-dashboard__content--with-alerts {
    grid-template-columns: 1fr;
  }

  .ai-dashboard__alerts {
    position: static;
    width: 100%;
  }

  .ai-dashboard__alerts-list {
    max-height: 360px;
  }
}

@media (max-width: 768px) {
  .ai-dashboard {
    padding: var(--ds-space-3);
  }

  .ai-dashboard__header,
  .ai-dashboard__header-right,
  .ai-dashboard__filters {
    flex-direction: column;
    align-items: stretch;
  }

  .ai-dashboard__stats-summary {
    justify-content: space-between;
  }

  .ai-dashboard__search {
    max-width: none;
  }
}
</style>
