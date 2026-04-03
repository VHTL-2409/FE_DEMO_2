<template>
  <div class="scl">
    <!-- Page Header -->
    <div class="scl__header">
      <div class="scl__header-content">
        <div class="scl__header-icon">
          <LucideIcon name="groups" />
        </div>
        <div class="scl__header-text">
          <h1 class="scl__title">Lớp học của tôi</h1>
          <p class="scl__subtitle">Xem và quản lý các lớp học bạn đã tham gia</p>
        </div>
      </div>
      <button type="button" class="scl__btn scl__btn--primary" @click="showJoinModal = true">
        <LucideIcon name="add" />
        Tham gia lớp
      </button>
    </div>

    <!-- Stats Cards -->
    <div class="scl__stats">
      <div class="scl__stat-card scl__stat-card--primary">
        <div class="scl__stat-icon">
          <LucideIcon name="groups" />
        </div>
        <div class="scl__stat-content">
          <p class="scl__stat-val">{{ classes.length }}</p>
          <p class="scl__stat-label">Lớp học</p>
        </div>
      </div>
      <div class="scl__stat-card scl__stat-card--success">
        <div class="scl__stat-icon">
          <LucideIcon name="user" />
        </div>
        <div class="scl__stat-content">
          <p class="scl__stat-val">{{ totalTeachers }}</p>
          <p class="scl__stat-label">Giáo viên</p>
        </div>
      </div>
    </div>

    <!-- Search & Filter -->
    <div class="scl__toolbar">
      <div class="scl__search">
        <LucideIcon name="search" class="scl__search-icon" />
        <input
          v-model="searchQuery"
          type="text"
          class="scl__search-input"
          placeholder="Tìm kiếm theo tên lớp, môn học..."
          @input="currentPage = 1"
        />
        <button v-if="searchQuery" type="button" class="scl__search-clear" @click="searchQuery = ''">
          <LucideIcon name="x" />
        </button>
      </div>
      <span class="scl__result-count">{{ filteredClasses.length }} lớp</span>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="scl__loading">
      <div class="scl__spinner"></div>
      <p>Đang tải danh sách lớp học...</p>
    </div>

    <!-- Empty State -->
    <div v-else-if="!isLoading && filteredClasses.length === 0" class="scl__empty">
      <div class="scl__empty-icon">
        <LucideIcon name="school" />
      </div>
      <h3>{{ searchQuery ? 'Không tìm thấy lớp học' : 'Chưa tham gia lớp học nào' }}</h3>
      <p>{{ searchQuery ? 'Thử thay đổi từ khóa tìm kiếm' : 'Tham gia lớp học bằng mã lớp được chia sẻ từ giáo viên' }}</p>
      <button v-if="!searchQuery" type="button" class="scl__btn scl__btn--primary" @click="showJoinModal = true">
        <LucideIcon name="add" />
        Tham gia lớp học
      </button>
    </div>

    <!-- Classes Grid -->
    <div v-else class="scl__grid">
      <TransitionGroup name="card">
        <div
          v-for="cls in paginatedClasses"
          :key="cls.id"
          class="scl__card"
          @click="openClassDetail(cls)"
        >
          <!-- Card Header -->
          <div class="scl__card-header">
            <div class="scl__card-icon">
              <LucideIcon name="groups" />
            </div>
            <div class="scl__card-badge" v-if="cls.subject">
              {{ cls.subject }}
            </div>
          </div>

          <!-- Card Body -->
          <div class="scl__card-body">
            <h3 class="scl__card-title">{{ cls.className }}</h3>
            <p v-if="cls.description" class="scl__card-desc">{{ cls.description }}</p>
            <div class="scl__card-teacher" v-if="cls.teacherName">
              <LucideIcon name="person" />
              <span>{{ cls.teacherName }}</span>
            </div>
          </div>

          <!-- Card Footer -->
          <div class="scl__card-footer">
            <div class="scl__card-meta">
              <div class="scl__card-stat">
                <LucideIcon name="users" />
                <span>{{ cls.studentCount || 0 }} học sinh</span>
              </div>
            </div>
            <button type="button" class="scl__card-action-btn" @click.stop="openClassDetail(cls)">
              <span>Xem chi tiết</span>
              <LucideIcon name="chevron-right" />
            </button>
          </div>
        </div>
      </TransitionGroup>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="scl__pagination">
      <button
        type="button"
        class="scl__page-btn"
        :disabled="currentPage === 1"
        @click="currentPage--"
      >
        <LucideIcon name="chevron_left" />
      </button>
      <div class="scl__page-numbers">
        <button
          v-for="page in visiblePages"
          :key="page"
          type="button"
          class="scl__page-num"
          :class="{ 'scl__page-num--active': page === currentPage }"
          @click="currentPage = page"
        >
          {{ page }}
        </button>
      </div>
      <button
        type="button"
        class="scl__page-btn"
        :disabled="currentPage === totalPages"
        @click="currentPage++"
      >
        <LucideIcon name="chevron_right" />
      </button>
    </div>

    <!-- Join Class Modal -->
    <StudentJoinClassModal
      v-model="showJoinModal"
      @joined="handleJoined"
    />

    <!-- Class Detail Modal -->
    <Transition name="modal">
      <div v-if="showDetailModal" class="scl__modal-overlay" @click.self="showDetailModal = false">
        <div class="scl__modal">
          <div class="scl__modal-header">
            <div class="scl__modal-icon">
              <LucideIcon name="groups" />
            </div>
            <div class="scl__modal-title-wrap">
              <h2 class="scl__modal-title">{{ selectedClass?.className }}</h2>
              <p class="scl__modal-subtitle" v-if="selectedClass?.teacherName">
                Giáo viên: {{ selectedClass.teacherName }}
              </p>
            </div>
            <button type="button" class="scl__modal-close" @click="showDetailModal = false">
              <LucideIcon name="x" />
            </button>
          </div>

          <div class="scl__modal-body">
            <p v-if="selectedClass?.description" class="scl__modal-desc">
              {{ selectedClass.description }}
            </p>

            <!-- Class Info -->
            <div class="scl__modal-info">
              <div class="scl__modal-info-item" v-if="selectedClass?.subject">
                <LucideIcon name="book" />
                <span>{{ selectedClass.subject }}</span>
              </div>
              <div class="scl__modal-info-item">
                <LucideIcon name="users" />
                <span>{{ selectedClass?.studentCount || 0 }} học sinh</span>
              </div>
            </div>

            <!-- Class Code -->
            <div class="scl__modal-code">
              <p class="scl__modal-code-label">Mã lớp</p>
              <div class="scl__modal-code-box">
                <span class="scl__modal-code-value">{{ selectedClass?.classCode || selectedClass?.code }}</span>
                <button type="button" class="scl__modal-copy-btn" @click="copyClassCode" title="Sao chép mã">
                  <LucideIcon :name="copied ? 'check' : 'copy'" />
                </button>
              </div>
            </div>
          </div>

          <div class="scl__modal-footer">
            <button type="button" class="scl__btn scl__btn--outline" @click="showDetailModal = false">
              Đóng
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMyClasses } from '../../../services/classService'
import { ApiError } from '../../../services/apiClient'
import { useToast } from '../../../composables/useToast'
import LucideIcon from '../../../common/LucideIcon.vue'
import StudentJoinClassModal from './StudentJoinClassModal.vue'

const toast = useToast()

// State
const classes = ref([])
const isLoading = ref(false)
const searchQuery = ref('')
const currentPage = ref(1)
const PAGE_SIZE = 9

const showJoinModal = ref(false)
const showDetailModal = ref(false)
const selectedClass = ref(null)
const copied = ref(false)

// Computed
const totalTeachers = computed(() => {
  const teachers = new Set(classes.value.map(c => c.teacherName).filter(Boolean))
  return teachers.size
})

const filteredClasses = computed(() => {
  if (!searchQuery.value.trim()) return classes.value
  const q = searchQuery.value.toLowerCase().trim()
  return classes.value.filter(c =>
    (c.className || '').toLowerCase().includes(q) ||
    (c.subject || '').toLowerCase().includes(q) ||
    (c.description || '').toLowerCase().includes(q) ||
    (c.teacherName || '').toLowerCase().includes(q)
  )
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredClasses.value.length / PAGE_SIZE)))

const paginatedClasses = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return filteredClasses.value.slice(start, start + PAGE_SIZE)
})

const visiblePages = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = currentPage.value
  let start = Math.max(1, current - 2)
  let end = Math.min(total, current + 2)
  
  if (end - start < 4) {
    if (start === 1) {
      end = Math.min(total, start + 4)
    } else {
      start = Math.max(1, end - 4)
    }
  }
  
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

// API
const loadClasses = async () => {
  isLoading.value = true
  try {
    const data = await getMyClasses()
    classes.value = data || []
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể tải danh sách lớp học.')
  } finally {
    isLoading.value = false
  }
}

// Handlers
const openClassDetail = (cls) => {
  selectedClass.value = cls
  showDetailModal.value = true
  copied.value = false
}

const copyClassCode = async () => {
  const code = selectedClass.value?.classCode || selectedClass.value?.code
  if (!code) return
  try {
    await navigator.clipboard.writeText(code)
    copied.value = true
    toast.success('Đã sao chép mã lớp!')
    setTimeout(() => { copied.value = false }, 2000)
  } catch {
    toast.error('Không thể sao chép mã lớp.')
  }
}

const handleJoined = async () => {
  showJoinModal.value = false
  await loadClasses()
}

onMounted(loadClasses)
</script>

<style scoped>
.scl {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  max-width: 1200px;
  margin: 0 auto;
  padding: 1.5rem;
}

/* Header */
.scl__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.scl__header-content {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.scl__header-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--ds-radius-2xl);
  background: linear-gradient(135deg, var(--ds-primary) 0%, #818cf8 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.25);
}

.scl__title {
  font-family: var(--ds-font-display);
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .scl__title { color: var(--ds-text); }

.scl__subtitle {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.dark .scl__subtitle { color: var(--ds-text-muted); }

/* Stats */
.scl__stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
  max-width: 400px;
}

@media (max-width: 640px) {
  .scl__stats { max-width: 100%; }
}

.scl__stat-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.25rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
}

.dark .scl__stat-card {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.scl__stat-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.scl__stat-card--primary .scl__stat-icon {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.scl__stat-card--success .scl__stat-icon {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.scl__stat-val {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1;
}

.dark .scl__stat-val { color: var(--ds-text); }

.scl__stat-label {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  font-weight: 600;
}

/* Toolbar */
.scl__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.scl__search {
  position: relative;
  flex: 1;
  max-width: 400px;
}

.scl__search-icon {
  position: absolute;
  left: 1rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--ds-text-muted);
  pointer-events: none;
}

.scl__search-input {
  width: 100%;
  padding: 0.875rem 1.25rem 0.875rem 3rem;
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  font-size: 0.9375rem;
  background: var(--ds-surface);
  color: var(--ds-text);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.dark .scl__search-input {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text);
}

.scl__search-input:focus {
  outline: none;
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 4px var(--ds-primary-ring);
}

.scl__search-clear {
  position: absolute;
  right: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 24px;
  border: none;
  background: var(--ds-gray-200);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--ds-text-muted);
  transition: all 0.15s ease;
}

.dark .scl__search-clear { background: var(--ds-gray-700); color: var(--ds-text-muted); }
.scl__search-clear:hover { background: var(--ds-gray-300); color: var(--ds-text); }

.scl__result-count {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  padding: 0.5rem 0.875rem;
  background: var(--ds-gray-100);
  border-radius: var(--ds-radius-lg);
}

.dark .scl__result-count { background: var(--ds-gray-700); color: var(--ds-text-muted); }

/* Loading */
.scl__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  gap: 1rem;
  color: var(--ds-text-muted);
}

.scl__spinner {
  width: 48px;
  height: 48px;
  border: 4px solid var(--ds-border);
  border-top-color: var(--ds-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* Empty */
.scl__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  gap: 1rem;
  text-align: center;
}

.scl__empty-icon {
  width: 80px;
  height: 80px;
  border-radius: var(--ds-radius-2xl);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.15);
}

.dark .scl__empty-icon {
  background: rgba(99, 102, 241, 0.2);
  color: var(--ds-primary);
}

.scl__empty h3 {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .scl__empty h3 { color: var(--ds-text); }

.scl__empty p {
  font-size: 0.9rem;
  color: var(--ds-text-muted);
  margin: 0;
  max-width: 320px;
}

/* Grid */
.scl__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.25rem;
}

/* Card Animation */
.card-enter-active,
.card-leave-active {
  transition: all 0.3s ease;
}

.card-enter-from,
.card-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

.card-move {
  transition: transform 0.3s ease;
}

.scl__card {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dark .scl__card {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.scl__card:hover {
  border-color: var(--ds-primary);
  box-shadow: 0 8px 32px rgba(79, 70, 229, 0.12);
  transform: translateY(-4px);
}

.scl__card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 1.25rem 1.25rem 0;
}

.scl__card-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--ds-radius-xl);
  background: linear-gradient(135deg, var(--ds-primary-soft) 0%, rgba(99, 102, 241, 0.15) 100%);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.dark .scl__card-icon {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.2) 0%, rgba(99, 102, 241, 0.1) 100%);
}

.scl__card-badge {
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-lg);
}

.dark .scl__card-badge {
  background: rgba(99, 102, 241, 0.15);
}

.scl__card-body {
  flex: 1;
  padding: 0.75rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.scl__card-title {
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
}

.dark .scl__card-title { color: var(--ds-text); }

.scl__card-desc {
  font-size: 0.85rem;
  color: var(--ds-text-muted);
  margin: 0;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.scl__card-teacher {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
}

.dark .scl__card-teacher { color: var(--ds-text-muted); }

.scl__card-footer {
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
  margin-top: auto;
}

.dark .scl__card-footer {
  border-top-color: var(--ds-border-strong);
  background: var(--ds-gray-700);
}

.scl__card-meta {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.scl__card-stat {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}

.dark .scl__card-stat { color: var(--ds-text-muted); }

.scl__card-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 700;
  background: var(--ds-primary);
  color: white;
  border: none;
  cursor: pointer;
  transition: all 0.15s ease;
  width: 100%;
  justify-content: center;
}

.scl__card-action-btn:hover {
  background: var(--ds-primary-hover);
  transform: translateX(2px);
}

/* Pagination */
.scl__pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin-top: 1rem;
}

.scl__page-btn {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.dark .scl__page-btn { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: var(--ds-text-muted); }
.scl__page-btn:hover:not(:disabled) { border-color: var(--ds-primary); color: var(--ds-primary); background: var(--ds-primary-soft); }
.scl__page-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.scl__page-numbers {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.scl__page-num {
  min-width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  border: 1.5px solid transparent;
  background: transparent;
  color: var(--ds-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
  font-weight: 600;
  font-size: 0.875rem;
}

.scl__page-num:hover:not(.scl__page-num--active) {
  background: var(--ds-gray-100);
  color: var(--ds-text);
}

.dark .scl__page-num:hover:not(.scl__page-num--active) { background: var(--ds-gray-700); color: var(--ds-text); }

.scl__page-num--active {
  background: var(--ds-primary);
  color: white;
  border-color: var(--ds-primary);
}

/* Buttons */
.scl__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1.5px solid transparent;
  white-space: nowrap;
}

.scl__btn--primary {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white;
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.25);
}

.scl__btn--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.35);
}

.scl__btn--outline {
  background: var(--ds-surface);
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .scl__btn--outline {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-muted);
}

.scl__btn--outline:hover { border-color: var(--ds-primary); color: var(--ds-primary); }

/* Modal */
.scl__modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1.5rem;
}

.scl__modal {
  background: var(--ds-surface);
  border-radius: var(--ds-radius-2xl);
  width: 100%;
  max-width: 480px;
  box-shadow: 0 24px 64px rgba(0,0,0,0.25);
}

.dark .scl__modal { background: var(--ds-gray-800); }

.scl__modal-header {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1.5rem;
  border-bottom: 1px solid var(--ds-border);
}

.dark .scl__modal-header { border-bottom-color: var(--ds-border-strong); }

.scl__modal-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.scl__modal-title-wrap {
  flex: 1;
}

.scl__modal-title {
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .scl__modal-title { color: var(--ds-text); }

.scl__modal-subtitle {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.scl__modal-close {
  width: 36px;
  height: 36px;
  border: none;
  background: var(--ds-gray-100);
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--ds-text-muted);
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.dark .scl__modal-close { background: var(--ds-gray-700); color: var(--ds-text-muted); }
.scl__modal-close:hover { background: var(--ds-gray-200); color: var(--ds-text); }

.scl__modal-body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.scl__modal-desc {
  font-size: 0.9rem;
  color: var(--ds-text-secondary);
  margin: 0;
  line-height: 1.6;
}

.scl__modal-info {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.scl__modal-info-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
  background: var(--ds-gray-50);
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-lg);
}

.dark .scl__modal-info-item { background: var(--ds-gray-700); color: var(--ds-text-muted); }

.scl__modal-code {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.scl__modal-code-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin: 0;
}

.scl__modal-code-box {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  background: var(--ds-primary-soft);
  border: 2px dashed var(--ds-primary);
  border-radius: var(--ds-radius-xl);
}

.dark .scl__modal-code-box { background: rgba(79, 70, 229, 0.15); }

.scl__modal-code-value {
  font-family: var(--ds-font-display);
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--ds-primary);
  letter-spacing: 0.1em;
  flex: 1;
}

.scl__modal-copy-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: var(--ds-primary);
  color: white;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.scl__modal-copy-btn:hover {
  background: var(--ds-primary-hover);
  transform: scale(1.05);
}

.scl__modal-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--ds-border);
  display: flex;
  justify-content: flex-end;
}

.dark .scl__modal-footer { border-top-color: var(--ds-border-strong); }

/* Modal Transitions */
.modal-enter-active,
.modal-leave-active {
  transition: all 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .scl__modal,
.modal-leave-to .scl__modal {
  transform: scale(0.95);
}

/* Responsive */
@media (max-width: 768px) {
  .scl {
    padding: 1rem;
    gap: 1rem;
  }

  .scl__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .scl__btn {
    width: 100%;
  }

  .scl__grid {
    grid-template-columns: 1fr;
  }
}
</style>
