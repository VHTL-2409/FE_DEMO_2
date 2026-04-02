<template>
  <div class="sm-app lm-page">
    <!-- Header -->
    <header class="lm-header">
      <div class="lm-header__left">
        <button class="sm-btn sm-btn--ghost" @click="handleBack">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6"></polyline>
          </svg>
          Quay lại
        </button>
        <div class="lm-header__info">
          <h1 class="sm-heading-3">{{ selectedExamTitle }}</h1>
          <span class="sm-body-small">{{ selectedExamMeta }}</span>
        </div>
      </div>
      
      <div class="lm-header__stats">
        <div class="lm-stat">
          <span class="lm-stat__value">{{ attempts.length }}</span>
          <span class="lm-stat__label">Thí sinh</span>
        </div>
        <div class="lm-stat lm-stat--success">
          <span class="lm-stat__value">{{ onlineCount }}</span>
          <span class="lm-stat__label">Online</span>
        </div>
        <div class="lm-stat lm-stat--warning" v-if="alertCount > 0">
          <span class="lm-stat__value">{{ alertCount }}</span>
          <span class="lm-stat__label">Cảnh báo</span>
        </div>
        <div class="lm-stat">
          <span class="lm-stat__value">{{ offlineCount }}</span>
          <span class="lm-stat__label">Offline</span>
        </div>
      </div>

      <div class="lm-header__actions">
        <button class="sm-btn sm-btn--ghost sm-btn--icon" @click="handleRefresh">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="23 4 23 10 17 10"></polyline>
            <polyline points="1 20 1 14 7 14"></polyline>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
          </svg>
        </button>
        <div class="lm-connection-status" :class="{ 'lm-connection-status--live': isSocketConnected }">
          <span class="lm-connection-dot"></span>
          <span>{{ isSocketConnected ? 'Real-time' : 'Polling' }}</span>
        </div>
      </div>
    </header>

    <!-- Filters -->
    <div class="lm-filters">
      <div class="sm-tabs">
        <button 
          class="sm-tab" 
          :class="{ 'sm-tab--active': activeTab === 'all' }"
          @click="activeTab = 'all'"
        >
          Tất cả <span class="lm-tab-count">{{ attempts.length }}</span>
        </button>
        <button 
          class="sm-tab" 
          :class="{ 'sm-tab--active': activeTab === 'online' }"
          @click="activeTab = 'online'"
        >
          Ổn định <span class="lm-tab-count lm-tab-count--success">{{ cleanCount }}</span>
        </button>
        <button 
          class="sm-tab" 
          :class="{ 'sm-tab--active': activeTab === 'warning' }"
          @click="activeTab = 'warning'"
        >
          Cần chú ý <span class="lm-tab-count lm-tab-count--warning">{{ warningCount }}</span>
        </button>
        <button 
          class="sm-tab" 
          :class="{ 'sm-tab--active': activeTab === 'critical' }"
          @click="activeTab = 'critical'"
        >
          Nghiêm trọng <span class="lm-tab-count lm-tab-count--error">{{ criticalCount }}</span>
        </button>
      </div>

      <div class="lm-filters__right">
        <div class="sm-search">
          <svg class="sm-search__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input 
            type="text"
            v-model="searchQuery"
            class="sm-input"
            placeholder="Tìm học sinh..."
          />
        </div>

        <div class="lm-view-toggle">
          <button 
            class="lm-view-btn" 
            :class="{ active: viewMode === 'grid' }"
            @click="viewMode = 'grid'"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="7" height="7"></rect>
              <rect x="14" y="3" width="7" height="7"></rect>
              <rect x="14" y="14" width="7" height="7"></rect>
              <rect x="3" y="14" width="7" height="7"></rect>
            </svg>
          </button>
          <button 
            class="lm-view-btn" 
            :class="{ active: viewMode === 'table' }"
            @click="viewMode = 'table'"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="8" y1="6" x2="21" y2="6"></line>
              <line x1="8" y1="12" x2="21" y2="12"></line>
              <line x1="8" y1="18" x2="21" y2="18"></line>
              <line x1="3" y1="6" x2="3.01" y2="6"></line>
              <line x1="3" y1="12" x2="3.01" y2="12"></line>
              <line x1="3" y1="18" x2="3.01" y2="18"></line>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Quick Actions -->
    <div class="lm-quick-actions" v-if="attempts.length > 0">
      <button 
        class="sm-btn sm-btn--secondary sm-btn--sm" 
        :disabled="highRiskCount === 0"
        @click="handleBatchWarn"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
          <line x1="12" y1="9" x2="12" y2="13"></line>
          <line x1="12" y1="17" x2="12.01" y2="17"></line>
        </svg>
        Cảnh báo ({{ highRiskCount }})
      </button>
      <button 
        class="sm-btn sm-btn--secondary sm-btn--sm" 
        :disabled="onlineCount === 0"
        @click="handlePauseAll"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="10" y1="15" x2="10" y2="9"></line>
          <line x1="14" y1="15" x2="14" y2="9"></line>
        </svg>
        Tạm dừng ({{ onlineCount }})
      </button>
      <button 
        class="sm-btn sm-btn--secondary sm-btn--sm" 
        @click="handleExportReport"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
          <polyline points="7 10 12 15 17 10"></polyline>
          <line x1="12" y1="15" x2="12" y2="3"></line>
        </svg>
        Xuất báo cáo
      </button>
    </div>

    <!-- Content -->
    <div class="lm-content">
      <!-- Grid View -->
      <div v-if="viewMode === 'grid'" class="lm-grid sm-stagger">
        <div 
          v-for="student in filteredStudents" 
          :key="student.id"
          class="lm-card"
          @click="openStudentDetail(student)"
        >
          <div class="lm-card__header">
            <div class="lm-avatar">
              {{ getInitials(student.name) }}
            </div>
            <div class="lm-card__info">
              <span class="lm-card__name">{{ student.name }}</span>
              <span class="lm-card__email">{{ student.email }}</span>
            </div>
            <span class="sm-badge" :class="getBadgeClass(student.status)">
              {{ getStatusLabel(student.status) }}
            </span>
          </div>

          <div class="lm-card__stats">
            <div class="lm-card__stat">
              <span class="lm-card__stat-value">{{ student.answeredCount || 0 }}</span>
              <span class="lm-card__stat-label">Đã trả lời</span>
            </div>
            <div class="lm-card__stat">
              <span class="lm-card__stat-value">{{ student.timeSpent || '00:00' }}</span>
              <span class="lm-card__stat-label">Thời gian</span>
            </div>
          </div>

          <div class="lm-card__risk">
            <div class="lm-risk-bar">
              <div 
                class="lm-risk-bar__fill" 
                :class="getRiskClass(student.riskScore)"
                :style="{ width: (student.riskScore || 0) + '%' }"
              />
            </div>
            <div class="lm-risk-info">
              <span>Risk: {{ student.riskScore || 0 }}%</span>
              <span :class="getRiskClass(student.riskScore)">{{ getRiskLabel(student.riskScore) }}</span>
            </div>
          </div>

          <div class="lm-card__devices">
            <span v-if="student.hasCamera" class="lm-device lm-device--active">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                <circle cx="12" cy="13" r="4"></circle>
              </svg>
            </span>
            <span v-if="student.hasMic" class="lm-device lm-device--active">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path>
              </svg>
            </span>
            <span v-if="student.tabSwitches > 0" class="lm-device lm-device--warning">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
                <line x1="8" y1="21" x2="16" y2="21"></line>
                <line x1="12" y1="17" x2="12" y2="21"></line>
              </svg>
              {{ student.tabSwitches }}
            </span>
          </div>

          <div class="lm-card__actions">
            <button class="sm-btn sm-btn--ghost sm-btn--sm" @click.stop="openWarningModal(student)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
              </svg>
            </button>
            <button class="sm-btn sm-btn--ghost sm-btn--sm" @click.stop="openPauseModal(student)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="10" y1="15" x2="10" y2="9"></line>
                <line x1="14" y1="15" x2="14" y2="9"></line>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- Table View -->
      <div v-else class="lm-table-container">
        <table class="sm-table">
          <thead>
            <tr>
              <th>Học sinh</th>
              <th>Thiết bị</th>
              <th>Tiến độ</th>
              <th>Điểm</th>
              <th>Vi phạm</th>
              <th>Risk</th>
              <th>Trạng thái</th>
              <th>Hành động</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="student in filteredStudents" :key="student.id">
              <td>
                <div class="lm-table-student">
                  <div class="lm-avatar lm-avatar--sm">{{ getInitials(student.name) }}</div>
                  <div>
                    <span class="lm-table-name">{{ student.name }}</span>
                    <span class="lm-table-email">{{ student.email }}</span>
                  </div>
                </div>
              </td>
              <td>
                <div class="lm-table-devices">
                  <span v-if="student.hasCamera" class="lm-device lm-device--sm lm-device--active">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/></svg>
                  </span>
                  <span v-if="student.hasMic" class="lm-device lm-device--sm lm-device--active">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/></svg>
                  </span>
                </div>
              </td>
              <td>{{ student.answeredCount || 0 }} / {{ student.questionCount || 0 }}</td>
              <td>{{ student.score || '—' }}</td>
              <td>
                <span v-if="student.tabSwitches > 0" class="sm-badge sm-badge--warning">
                  {{ student.tabSwitches }} switch
                </span>
                <span v-else class="sm-text-tertiary">—</span>
              </td>
              <td>
                <div class="lm-table-risk">
                  <div class="sm-progress" style="width: 60px;">
                    <div class="sm-progress__bar" :class="getRiskClass(student.riskScore)" :style="{ width: (student.riskScore || 0) + '%', background: getRiskColor(student.riskScore) }"></div>
                  </div>
                  <span>{{ student.riskScore || 0 }}%</span>
                </div>
              </td>
              <td>
                <span class="sm-badge" :class="getBadgeClass(student.status)">
                  {{ getStatusLabel(student.status) }}
                </span>
              </td>
              <td>
                <div class="lm-table-actions">
                  <button class="sm-btn sm-btn--ghost sm-btn--icon" @click="openWarningModal(student)">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/></svg>
                  </button>
                  <button class="sm-btn sm-btn--ghost sm-btn--icon" @click="openPauseModal(student)">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="10" y1="15" x2="10" y2="9"/><line x1="14" y1="15" x2="14" y2="9"/></svg>
                  </button>
                  <button class="sm-btn sm-btn--ghost sm-btn--icon" @click="openStudentDetail(student)">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Empty State -->
      <div v-if="filteredStudents.length === 0" class="sm-empty">
        <svg class="sm-empty__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"></circle>
          <path d="M8 15h8M9 9h.01M15 9h.01"></path>
        </svg>
        <h3 class="sm-empty__title">Không có thí sinh nào</h3>
        <p class="sm-empty__description">Danh sách thí sinh đang trống hoặc không có kết quả tìm kiếm.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const attempts = ref([])
const alerts = ref([])
const searchQuery = ref('')
const activeTab = ref('all')
const viewMode = ref('grid')
const isSocketConnected = ref(true)

const selectedExamTitle = ref('Kỳ thi Giữa Kỳ')
const selectedExamMeta = ref('Lớp 12A1 - 2024')

const onlineCount = computed(() => attempts.value.filter(a => a.status === 'ONLINE').length)
const offlineCount = computed(() => attempts.value.filter(a => a.status === 'OFFLINE').length)
const alertCount = computed(() => alerts.value.length)
const criticalCount = computed(() => attempts.value.filter(a => (a.riskScore || 0) > 80).length)
const warningCount = computed(() => attempts.value.filter(a => (a.riskScore || 0) > 30 && (a.riskScore || 0) <= 80).length)
const cleanCount = computed(() => attempts.value.filter(a => (a.riskScore || 0) <= 30).length)
const highRiskCount = computed(() => criticalCount.value + warningCount.value)

const filteredStudents = computed(() => {
  let filtered = attempts.value
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    filtered = filtered.filter(s => 
      s.name?.toLowerCase().includes(q) || 
      s.email?.toLowerCase().includes(q)
    )
  }
  if (activeTab.value === 'online') {
    filtered = filtered.filter(a => (a.riskScore || 0) <= 30)
  } else if (activeTab.value === 'warning') {
    filtered = filtered.filter(a => (a.riskScore || 0) > 30 && (a.riskScore || 0) <= 80)
  } else if (activeTab.value === 'critical') {
    filtered = filtered.filter(a => (a.riskScore || 0) > 80)
  }
  return filtered
})

const getInitials = (name) => {
  if (!name) return '?'
  const parts = name.split(' ')
  return parts.length >= 2 ? parts[0][0] + parts[parts.length - 1][0] : parts[0].substring(0, 2)
}

const getRiskClass = (score) => {
  if (!score || score <= 30) return 'lm-risk-bar__fill--low'
  if (score <= 80) return 'lm-risk-bar__fill--medium'
  return 'lm-risk-bar__fill--high'
}

const getRiskColor = (score) => {
  if (!score || score <= 30) return 'var(--sm-success-text)'
  if (score <= 80) return 'var(--sm-warning-text)'
  return 'var(--sm-error-text)'
}

const getRiskLabel = (score) => {
  if (!score || score <= 30) return 'Ổn định'
  if (score <= 80) return 'Cần chú ý'
  return 'Nghiêm trọng'
}

const getBadgeClass = (status) => {
  const map = {
    ONLINE: 'sm-badge--success',
    OFFLINE: '',
    PAUSED: 'sm-badge--warning',
    INVALIDATED: 'sm-badge--error'
  }
  return map[status] || ''
}

const getStatusLabel = (status) => {
  const map = {
    ONLINE: 'Online',
    OFFLINE: 'Offline',
    PAUSED: 'Tạm dừng',
    INVALIDATED: 'Bị hủy'
  }
  return map[status] || status
}

const handleBack = () => {}
const handleRefresh = () => {}
const handleBatchWarn = () => {}
const handlePauseAll = () => {}
const handleExportReport = () => {}
const openStudentDetail = (student) => {}
const openWarningModal = (student) => {}
const openPauseModal = (student) => {}
</script>

<style scoped>
/* Page */
.lm-page {
  padding: var(--sm-space-6);
}

/* Header */
.lm-header {
  display: flex;
  align-items: center;
  gap: var(--sm-space-6);
  padding-bottom: var(--sm-space-6);
  border-bottom: 1px solid var(--sm-border-default);
  margin-bottom: var(--sm-space-6);
}

.lm-header__left {
  display: flex;
  align-items: center;
  gap: var(--sm-space-4);
}

.lm-header__info {
  display: flex;
  flex-direction: column;
  gap: var(--sm-space-1);
}

.lm-header__stats {
  display: flex;
  gap: var(--sm-space-6);
  margin-left: auto;
}

.lm-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--sm-space-1);
}

.lm-stat__value {
  font-size: var(--sm-text-xl);
  font-weight: 700;
  color: var(--sm-text-primary);
  line-height: 1;
}

.lm-stat--success .lm-stat__value {
  color: var(--sm-success-text);
}

.lm-stat--warning .lm-stat__value {
  color: var(--sm-warning-text);
}

.lm-stat__label {
  font-size: var(--sm-text-xs);
  color: var(--sm-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.lm-header__actions {
  display: flex;
  align-items: center;
  gap: var(--sm-space-3);
}

.lm-connection-status {
  display: flex;
  align-items: center;
  gap: var(--sm-space-2);
  padding: var(--sm-space-2) var(--sm-space-3);
  font-size: var(--sm-text-sm);
  color: var(--sm-text-tertiary);
  background: var(--sm-bg-tertiary);
  border-radius: var(--sm-radius-full);
}

.lm-connection-status--live {
  color: var(--sm-success-text);
  background: var(--sm-success-bg);
}

.lm-connection-dot {
  width: 8px;
  height: 8px;
  background: currentColor;
  border-radius: 50%;
}

.lm-connection-status--live .lm-connection-dot {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* Filters */
.lm-filters {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sm-space-4);
  margin-bottom: var(--sm-space-4);
}

.lm-filters__right {
  display: flex;
  align-items: center;
  gap: var(--sm-space-3);
}

.lm-tab-count {
  margin-left: var(--sm-space-2);
  padding: 2px 6px;
  font-size: var(--sm-text-xs);
  background: var(--sm-bg-tertiary);
  border-radius: var(--sm-radius-full);
}

.lm-tab-count--success {
  background: var(--sm-success-bg);
  color: var(--sm-success-text);
}

.lm-tab-count--warning {
  background: var(--sm-warning-bg);
  color: var(--sm-warning-text);
}

.lm-tab-count--error {
  background: var(--sm-error-bg);
  color: var(--sm-error-text);
}

.lm-view-toggle {
  display: flex;
  gap: var(--sm-space-1);
  padding: var(--sm-space-1);
  background: var(--sm-bg-tertiary);
  border-radius: var(--sm-radius-md);
}

.lm-view-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  border-radius: var(--sm-radius-sm);
  color: var(--sm-text-tertiary);
  cursor: pointer;
  transition: all var(--sm-duration-fast) var(--sm-ease-out);
}

.lm-view-btn:hover {
  color: var(--sm-text-primary);
}

.lm-view-btn.active {
  background: var(--sm-bg-secondary);
  color: var(--sm-text-primary);
  box-shadow: var(--sm-shadow-sm);
}

/* Quick Actions */
.lm-quick-actions {
  display: flex;
  gap: var(--sm-space-3);
  margin-bottom: var(--sm-space-6);
}

/* Grid */
.lm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--sm-space-4);
}

.lm-card {
  background: var(--sm-bg-secondary);
  border: 1px solid var(--sm-border-default);
  border-radius: var(--sm-radius-lg);
  padding: var(--sm-space-5);
  cursor: pointer;
  transition: all var(--sm-duration-normal) var(--sm-ease-out);
}

.lm-card:hover {
  border-color: var(--sm-border-strong);
  box-shadow: var(--sm-shadow-sm);
}

.lm-card__header {
  display: flex;
  align-items: center;
  gap: var(--sm-space-3);
  margin-bottom: var(--sm-space-4);
}

.lm-avatar {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--sm-accent-muted);
  color: var(--sm-accent-primary);
  font-size: var(--sm-text-sm);
  font-weight: 600;
  border-radius: var(--sm-radius-md);
  flex-shrink: 0;
}

.lm-avatar--sm {
  width: 32px;
  height: 32px;
  font-size: var(--sm-text-xs);
  border-radius: var(--sm-radius-sm);
}

.lm-card__info {
  flex: 1;
  min-width: 0;
}

.lm-card__name {
  display: block;
  font-size: var(--sm-text-sm);
  font-weight: 600;
  color: var(--sm-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lm-card__email {
  display: block;
  font-size: var(--sm-text-xs);
  color: var(--sm-text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
}

.lm-card__stats {
  display: flex;
  gap: var(--sm-space-4);
  margin-bottom: var(--sm-space-4);
}

.lm-card__stat {
  flex: 1;
  text-align: center;
}

.lm-card__stat-value {
  display: block;
  font-size: var(--sm-text-lg);
  font-weight: 700;
  color: var(--sm-text-primary);
  line-height: 1;
}

.lm-card__stat-label {
  font-size: var(--sm-text-xs);
  color: var(--sm-text-tertiary);
}

/* Risk Bar */
.lm-card__risk {
  margin-bottom: var(--sm-space-4);
}

.lm-risk-bar {
  width: 100%;
  height: 4px;
  background: var(--sm-bg-tertiary);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: var(--sm-space-2);
}

.lm-risk-bar__fill {
  height: 100%;
  border-radius: 2px;
  transition: width var(--sm-duration-slow) var(--sm-ease-out);
}

.lm-risk-bar__fill--low {
  background: var(--sm-success-text);
}

.lm-risk-bar__fill--medium {
  background: var(--sm-warning-text);
}

.lm-risk-bar__fill--high {
  background: var(--sm-error-text);
}

.lm-risk-info {
  display: flex;
  justify-content: space-between;
  font-size: var(--sm-text-xs);
  color: var(--sm-text-tertiary);
}

/* Devices */
.lm-card__devices {
  display: flex;
  gap: var(--sm-space-2);
  margin-bottom: var(--sm-space-4);
}

.lm-device {
  display: flex;
  align-items: center;
  gap: var(--sm-space-1);
  padding: var(--sm-space-1) var(--sm-space-2);
  font-size: var(--sm-text-xs);
  background: var(--sm-bg-tertiary);
  border-radius: var(--sm-radius-sm);
  color: var(--sm-text-tertiary);
}

.lm-device--active {
  background: var(--sm-success-bg);
  color: var(--sm-success-text);
}

.lm-device--warning {
  background: var(--sm-warning-bg);
  color: var(--sm-warning-text);
}

.lm-device--sm {
  padding: var(--sm-space-1);
}

/* Card Actions */
.lm-card__actions {
  display: flex;
  gap: var(--sm-space-2);
  padding-top: var(--sm-space-3);
  border-top: 1px solid var(--sm-border-subtle);
}

/* Table */
.lm-table-container {
  background: var(--sm-bg-secondary);
  border: 1px solid var(--sm-border-default);
  border-radius: var(--sm-radius-lg);
  overflow: hidden;
}

.lm-table-student {
  display: flex;
  align-items: center;
  gap: var(--sm-space-3);
}

.lm-table-name {
  display: block;
  font-weight: 500;
}

.lm-table-email {
  display: block;
  font-size: var(--sm-text-xs);
  color: var(--sm-text-tertiary);
}

.lm-table-devices {
  display: flex;
  gap: var(--sm-space-1);
}

.lm-table-risk {
  display: flex;
  align-items: center;
  gap: var(--sm-space-2);
  font-size: var(--sm-text-sm);
}

.lm-table-actions {
  display: flex;
  gap: var(--sm-space-1);
}

/* Responsive */
@media (max-width: 1024px) {
  .lm-header {
    flex-wrap: wrap;
  }
  
  .lm-header__stats {
    margin-left: 0;
    width: 100%;
    justify-content: flex-start;
    padding-top: var(--sm-space-4);
    border-top: 1px solid var(--sm-border-subtle);
    margin-top: var(--sm-space-4);
  }
}

@media (max-width: 768px) {
  .lm-page {
    padding: var(--sm-space-4);
  }
  
  .lm-filters {
    flex-direction: column;
    align-items: stretch;
  }
  
  .lm-filters__right {
    justify-content: space-between;
  }
  
  .lm-quick-actions {
    flex-wrap: wrap;
  }
}
</style>
