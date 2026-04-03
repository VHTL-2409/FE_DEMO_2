<template>
  <div class="sm-app el-page">
    <!-- Page Header -->
    <header class="el-header">
      <div class="el-header__left">
        <h1 class="sm-heading-2">Kỳ thi của tôi</h1>
        <p class="sm-body-small">{{ filteredExams.length }} kỳ thi</p>
      </div>
      <div class="el-header__right">
        <button class="sm-btn sm-btn--primary" @click="goToCreate">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          Tạo kỳ thi mới
        </button>
      </div>
    </header>

    <!-- Filters -->
    <div class="el-filters">
      <div class="sm-tabs">
        <button 
          class="sm-tab" 
          :class="{ 'sm-tab--active': activeFilter === 'all' }"
          @click="activeFilter = 'all'"
        >
          Tất cả
        </button>
        <button 
          class="sm-tab" 
          :class="{ 'sm-tab--active': activeFilter === 'started' }"
          @click="activeFilter = 'started'"
        >
          Đang diễn ra
        </button>
        <button 
          class="sm-tab" 
          :class="{ 'sm-tab--active': activeFilter === 'upcoming' }"
          @click="activeFilter = 'upcoming'"
        >
          Sắp diễn ra
        </button>
        <button 
          class="sm-tab" 
          :class="{ 'sm-tab--active': activeFilter === 'ended' }"
          @click="activeFilter = 'ended'"
        >
          Đã kết thúc
        </button>
      </div>

      <div class="el-filters__right">
        <div class="sm-search">
          <svg class="sm-search__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input 
            type="text"
            v-model="searchQuery"
            class="sm-input"
            placeholder="Tìm kiếm..."
          />
        </div>

        <div class="el-view-toggle">
          <button 
            class="el-view-btn" 
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
            class="el-view-btn" 
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

    <!-- Content -->
    <div class="el-content">
      <!-- Grid View -->
      <div v-if="viewMode === 'grid'" class="el-grid sm-stagger">
        <div 
          v-for="exam in filteredExams" 
          :key="exam.id"
          class="el-card"
          :class="{ 'el-card--live': exam.statusKey === 'started' }"
          @click="openDetailModal(exam)"
          style="cursor: pointer;"
        >
          <div class="el-card__header">
            <span class="sm-badge" :class="getBadgeClass(exam.statusKey)">
              <span v-if="exam.statusKey === 'started'" class="el-live-dot"></span>
              {{ exam.status }}
            </span>
            <div class="el-card__actions">
              <button class="sm-btn sm-btn--ghost sm-btn--icon" @click.stop="openMenu(exam, $event)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="1"></circle>
                  <circle cx="12" cy="5" r="1"></circle>
                  <circle cx="12" cy="19" r="1"></circle>
                </svg>
              </button>
            </div>
          </div>

          <h3 class="el-card__title">{{ exam.title }}</h3>
          <p class="el-card__desc">{{ exam.subtitle }}</p>

          <div class="el-card__meta">
            <span>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                <line x1="16" y1="2" x2="16" y2="6"></line>
                <line x1="8" y1="2" x2="8" y2="6"></line>
                <line x1="3" y1="10" x2="21" y2="10"></line>
              </svg>
              {{ exam.date }}
            </span>
            <span>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <polyline points="12 6 12 12 16 14"></polyline>
              </svg>
              {{ exam.participants }}
            </span>
          </div>

          <div class="el-card__footer">
            <button 
              v-if="exam.statusKey === 'started'" 
              class="sm-btn sm-btn--primary sm-btn--sm"
              @click="goToMonitoring(exam)"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <polygon points="10 8 16 12 10 16 10 8"></polygon>
              </svg>
              Giám sát
            </button>
            <button 
              v-if="exam.statusKey === 'ended'" 
              class="sm-btn sm-btn--secondary sm-btn--sm"
              @click="goToResult(exam)"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="20" x2="18" y2="10"></line>
                <line x1="12" y1="20" x2="12" y2="4"></line>
                <line x1="6" y1="20" x2="6" y2="14"></line>
              </svg>
              Kết quả
            </button>
            <button 
              class="sm-btn sm-btn--ghost sm-btn--sm"
              @click="goToDetail(exam)"
            >
              Chi tiết
            </button>
          </div>
        </div>
      </div>

      <!-- Table View -->
      <div v-else class="el-table-container">
        <table class="sm-table">
          <thead>
            <tr>
              <th>Kỳ thi</th>
              <th>Trạng thái</th>
              <th>Ngày thi</th>
              <th>Số câu</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="exam in filteredExams" :key="exam.id">
              <td>
                <div class="el-table-exam">
                  <span class="el-table-title">{{ exam.title }}</span>
                  <span class="el-table-desc">{{ exam.subtitle }}</span>
                </div>
              </td>
              <td>
                <span class="sm-badge" :class="getBadgeClass(exam.statusKey)">
                  {{ exam.status }}
                </span>
              </td>
              <td>{{ exam.date }}</td>
              <td>{{ exam.participants }}</td>
              <td>
                <div class="el-table-actions">
                  <button 
                    v-if="exam.statusKey === 'started'" 
                    class="sm-btn sm-btn--primary sm-btn--sm"
                    @click="goToMonitoring(exam)"
                  >
                    Giám sát
                  </button>
                  <button 
                    v-if="exam.statusKey === 'ended'" 
                    class="sm-btn sm-btn--secondary sm-btn--sm"
                    @click="goToResult(exam)"
                  >
                    Kết quả
                  </button>
                  <button class="sm-btn sm-btn--ghost sm-btn--sm" @click="goToDetail(exam)">
                    Chi tiết
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Empty State -->
      <EmptyState
        v-if="filteredExams.length === 0"
        variant="no-data"
        title="Không có kỳ thi nào"
        description="Tạo kỳ thi đầu tiên của bạn để bắt đầu."
        action-label="Tạo kỳ thi mới"
        action-icon="plus"
        @action="goToCreate"
      />
    </div>
  </div>

  <!-- Exam Detail Modal -->
  <ExamDetailModal
    v-model="showDetailModal"
    :exam="selectedExam"
    @edit="handleEditExam"
    @duplicate="handleDuplicateExam"
    @view-analytics="handleViewAnalytics"
  />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listExams } from '../../../../services/examService'
import { useToast } from '../../../../composables/useToast'
import { useScrollToTop } from '../../../../composables/useScrollToTop'
import EmptyState from '../../../common/EmptyState.vue'
import ExamDetailModal from './ExamDetailModal.vue'

useScrollToTop()

const router = useRouter()
const toast = useToast()

const exams = ref([])
const searchQuery = ref('')
const activeFilter = ref('all')
const viewMode = ref('grid')
const isLoading = ref(true)
const showDetailModal = ref(false)
const selectedExam = ref(null)

// Exam helpers
const getExamStatusMeta = (exam) => {
  if (!exam.isActive) return { key: 'draft', label: 'Bản nháp' }
  const nowMs = Date.now()
  const startMs = new Date(exam.startTime || '').getTime()
  const endMs = new Date(exam.endTime || '').getTime()
  if (!Number.isNaN(startMs) && nowMs < startMs) return { key: 'upcoming', label: 'Sắp diễn ra' }
  if (!Number.isNaN(startMs) && !Number.isNaN(endMs) && nowMs >= startMs && nowMs <= endMs) return { key: 'started', label: 'Đang diễn ra' }
  return { key: 'ended', label: 'Đã kết thúc' }
}

const getSortTime = (exam) => {
  for (const value of [exam.endTime, exam.startTime, exam.updatedAt, exam.createdAt]) {
    const t = new Date(value || '').getTime()
    if (!Number.isNaN(t)) return t
  }
  return 0
}

const formatDateTime = (value) => {
  const d = new Date(value || '')
  return Number.isNaN(d.getTime()) ? '—' : d.toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const examTableData = computed(() =>
  exams.value.slice().sort((a, b) => getSortTime(b) - getSortTime(a)).map(exam => {
    const { key, label } = getExamStatusMeta(exam)
    return {
      id: exam.id,
      title: exam.title,
      subtitle: exam.description || '—',
      date: formatDateTime(exam.endTime || exam.startTime),
      status: label,
      statusKey: key,
      participants: `${exam.participantCount ?? 0} HS`
    }
  })
)

const filteredExams = computed(() => {
  let filtered = examTableData.value
  
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    filtered = filtered.filter(e => 
      e.title?.toLowerCase().includes(q) || 
      e.subtitle?.toLowerCase().includes(q)
    )
  }
  
  if (activeFilter.value !== 'all') {
    filtered = filtered.filter(e => e.statusKey === activeFilter.value)
  }
  
  return filtered
})

const getBadgeClass = (statusKey) => {
  const classes = {
    draft: '',
    upcoming: 'sm-badge--info',
    started: 'sm-badge--success',
    ended: ''
  }
  return classes[statusKey] || ''
}

const goToCreate = () => router.push('/teacher/exams/create')
const goToDetail = (exam) => router.push(`/teacher/exams/${exam.id}`)
const goToMonitoring = (exam) => router.push({ path: '/teacher/live-monitoring/session', query: { examId: exam.id, title: exam.title } })
const goToResult = (exam) => router.push({ path: '/teacher/exams/review/summary', query: { examId: exam.id, title: exam.title } })

const openMenu = (exam, event) => {
  event.stopPropagation()
  toast.info(`Menu cho: ${exam.title}`)
}

const openDetailModal = (exam) => {
  selectedExam.value = exam
  showDetailModal.value = true
}

const handleEditExam = (exam) => {
  showDetailModal.value = false
  router.push(`/teacher/exams/${exam.id}`)
}

const handleDuplicateExam = (exam) => {
  showDetailModal.value = false
  toast.success(`Đã nhân bản đề thi: ${exam.title}`)
  loadExams()
}

const handleViewAnalytics = (exam) => {
  showDetailModal.value = false
  router.push({ path: '/teacher/exams/review/summary', query: { examId: exam.id, title: exam.title } })
}

const loadExams = async () => {
  try {
    isLoading.value = true
    exams.value = await listExams()
  } catch (error) {
    toast.error('Không thể tải danh sách kỳ thi.')
  } finally {
    isLoading.value = false
  }
}

onMounted(async () => {
  await loadExams()
})
</script>

<style scoped>
/* Page */
.el-page {
  padding: var(--sm-space-6);
}

/* Header */
.el-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--sm-space-4);
  margin-bottom: var(--sm-space-6);
}

.el-header__left {
  display: flex;
  flex-direction: column;
  gap: var(--sm-space-1);
}

/* Filters */
.el-filters {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sm-space-4);
  margin-bottom: var(--sm-space-6);
  flex-wrap: wrap;
}

.el-filters__right {
  display: flex;
  align-items: center;
  gap: var(--sm-space-3);
}

.el-view-toggle {
  display: flex;
  gap: var(--sm-space-1);
  padding: var(--sm-space-1);
  background: var(--sm-bg-tertiary);
  border-radius: var(--sm-radius-md);
}

.el-view-btn {
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

.el-view-btn:hover {
  color: var(--sm-text-primary);
}

.el-view-btn.active {
  background: var(--sm-bg-secondary);
  color: var(--sm-text-primary);
  box-shadow: var(--sm-shadow-sm);
}

/* Grid */
.el-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--sm-space-4);
}

.el-card {
  background: var(--sm-bg-secondary);
  border: 1px solid var(--sm-border-default);
  border-radius: var(--sm-radius-lg);
  padding: var(--sm-space-5);
  display: flex;
  flex-direction: column;
  transition: all var(--sm-duration-normal) var(--sm-ease-out);
}

.el-card:hover {
  border-color: var(--sm-border-strong);
  box-shadow: var(--sm-shadow-sm);
}

.el-card--live {
  border-color: var(--sm-success-border);
}

.el-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--sm-space-3);
}

.el-card__actions {
  opacity: 0;
  transition: opacity var(--sm-duration-fast);
}

.el-card:hover .el-card__actions {
  opacity: 1;
}

.el-live-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  background: var(--sm-success-text);
  border-radius: 50%;
  margin-right: var(--sm-space-1);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.el-card__title {
  font-size: var(--sm-text-base);
  font-weight: 600;
  color: var(--sm-text-primary);
  margin-bottom: var(--sm-space-2);
  line-height: 1.4;
}

.el-card__desc {
  font-size: var(--sm-text-sm);
  color: var(--sm-text-secondary);
  margin-bottom: var(--sm-space-4);
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.el-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sm-space-4);
  margin-bottom: var(--sm-space-4);
  padding-top: var(--sm-space-3);
  border-top: 1px solid var(--sm-border-subtle);
}

.el-card__meta span {
  display: flex;
  align-items: center;
  gap: var(--sm-space-2);
  font-size: var(--sm-text-xs);
  color: var(--sm-text-tertiary);
}

.el-card__footer {
  display: flex;
  gap: var(--sm-space-2);
}

/* Table */
.el-table-container {
  background: var(--sm-bg-secondary);
  border: 1px solid var(--sm-border-default);
  border-radius: var(--sm-radius-lg);
  overflow: hidden;
}

.el-table-exam {
  display: flex;
  flex-direction: column;
  gap: var(--sm-space-1);
}

.el-table-title {
  font-weight: 500;
  color: var(--sm-text-primary);
}

.el-table-desc {
  font-size: var(--sm-text-xs);
  color: var(--sm-text-tertiary);
}

.el-table-actions {
  display: flex;
  gap: var(--sm-space-2);
}

/* Responsive */
@media (max-width: 768px) {
  .el-page {
    padding: var(--sm-space-4);
  }

  .el-header {
    flex-direction: column;
  }

  .el-filters {
    flex-direction: column;
    align-items: stretch;
  }

  .el-filters__right {
    justify-content: space-between;
  }

  .el-grid {
    grid-template-columns: 1fr;
  }
}
</style>
