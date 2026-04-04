<template>
  <Transition name="cdd-drawer">
    <div v-if="modelValue" class="cdd-wrapper">
      <div class="cdd-overlay" @click.self="close">
        <div class="cdd-drawer">
          <div class="cdd-drawer__header">
            <div class="cdd-drawer__title-area">
              <div class="cdd-drawer__icon">
                <LucideIcon name="book-open" />
              </div>
              <div>
                <h2 class="cdd-drawer__title">{{ classItem?.name }}</h2>
                <p class="cdd-drawer__subtitle">{{ classItem?.subject || 'Chưa có môn học' }}</p>
              </div>
            </div>
            <button type="button" class="cdd-drawer__close" @click="close">
              <LucideIcon name="x" />
            </button>
          </div>

          <div class="cdd-drawer__tabs">
            <button
              type="button"
              class="cdd-tab"
              :class="{ 'cdd-tab--active': activeTab === 'info' }"
              @click="activeTab = 'info'"
            >
              <LucideIcon name="info" />
              Thông tin
            </button>
            <button
              type="button"
              class="cdd-tab"
              :class="{ 'cdd-tab--active': activeTab === 'students' }"
              @click="activeTab = 'students'"
            >
              <LucideIcon name="users" />
              Học sinh
              <span v-if="students.length > 0" class="cdd-tab__badge">{{ students.length }}</span>
            </button>
          </div>

          <!-- Info Tab -->
          <div v-if="activeTab === 'info'" class="cdd-drawer__content">
            <div class="cdd-info-grid">
              <div class="cdd-info-item">
                <span class="cdd-info-label">Môn học</span>
                <span class="cdd-info-value">{{ classItem?.subject || '—' }}</span>
              </div>
              <div class="cdd-info-item">
                <span class="cdd-info-label">Số học sinh</span>
                <span class="cdd-info-value">{{ students.length }}</span>
              </div>
              <div class="cdd-info-item cdd-info-item--full">
                <span class="cdd-info-label">Mô tả</span>
                <span class="cdd-info-value">{{ classItem?.description || 'Chưa có mô tả' }}</span>
              </div>
              <div class="cdd-info-item cdd-info-item--full cdd-info-item--code">
                <span class="cdd-info-label">Mã lớp</span>
                <div class="cdd-class-code-wrapper">
                  <span class="cdd-class-code">{{ classItem?.classCode || '—' }}</span>
                  <button type="button" class="cdd-copy-btn" @click="copyClassCode" title="Sao chép mã">
                    <LucideIcon :name="copied ? 'check' : 'copy'" />
                  </button>
                </div>
              </div>
              <div class="cdd-info-item">
                <span class="cdd-info-label">Ngày tạo</span>
                <span class="cdd-info-value">{{ formatDate(classItem?.createdAt) }}</span>
              </div>
              <div class="cdd-info-item">
                <span class="cdd-info-label">Cập nhật lần cuối</span>
                <span class="cdd-info-value">{{ formatDate(classItem?.updatedAt) }}</span>
              </div>
            </div>
          </div>

          <!-- Students Tab -->
          <div v-if="activeTab === 'students'" class="cdd-drawer__content">
            <div class="cdd-students-header">
              <h3 class="cdd-students-title">Danh sách học sinh</h3>
              <button type="button" class="cdd-btn cdd-btn--primary" @click="showBulkAddModal = true">
                <LucideIcon name="upload" />
                Import học sinh
              </button>
            </div>

            <div v-if="isLoadingStudents" class="cdd-loading">
              <div class="cdd-spinner"></div>
              <p>Đang tải danh sách học sinh...</p>
            </div>

            <div v-else-if="students.length === 0" class="cdd-empty">
              <LucideIcon name="users" />
              <p>Chưa có học sinh trong lớp</p>
              <button type="button" class="cdd-btn cdd-btn--outline" @click="showBulkAddModal = true">
                <LucideIcon name="upload" />
                Import học sinh
              </button>
            </div>

            <div v-else class="cdd-students-list">
              <div
                v-for="student in students"
                :key="student.id"
                class="cdd-student-item"
              >
                <div class="cdd-student__avatar">
                  {{ getInitials(student.studentUsername) }}
                </div>
                <div class="cdd-student__info">
                  <span class="cdd-student__name">{{ student.studentUsername }}</span>
                  <span class="cdd-student__email">{{ student.studentEmail }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Footer -->
          <div class="cdd-drawer__footer">
            <button type="button" class="cdd-btn cdd-btn--outline" @click="close">Đóng</button>
          </div>
        </div>
      </div>

      <!-- Bulk Add Students Modal -->
      <BulkAddStudentsModal
        v-model="showBulkAddModal"
        :class-id="classItem?.id"
        :class-name="classItem?.name"
        @added="loadStudents"
      />
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ApiError } from '../../../services/apiClient'
import { getClassStudents } from '../../../services/classService'
import { useToast } from '../../../composables/useToast'
import LucideIcon from '../../common/LucideIcon.vue'
import BulkAddStudentsModal from './BulkAddStudentsModal.vue'

const props = defineProps({
  modelValue: Boolean,
  classItem: Object
})

const emit = defineEmits(['update:modelValue', 'edit'])

const toast = useToast()

const activeTab = ref('info')
const students = ref([])
const isLoadingStudents = ref(false)
const copied = ref(false)
const showBulkAddModal = ref(false)

watch(() => props.modelValue, async (val) => {
  if (val) {
    activeTab.value = 'info'
    students.value = []
    await loadStudents()
  }
})

watch(activeTab, (tab) => {
  if (tab === 'students' && students.value.length === 0) {
    loadStudents()
  }
})

const loadStudents = async () => {
  if (!props.classItem?.id) return
  isLoadingStudents.value = true
  try {
    const data = await getClassStudents(props.classItem.id)
    students.value = data || []
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể tải danh sách học sinh.')
  } finally {
    isLoadingStudents.value = false
  }
}

const getInitials = (name) => {
  if (!name) return '?'
  return name.slice(0, 2).toUpperCase()
}

const formatDate = (date) => {
  if (!date) return '—'
  try {
    return new Date(date).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    })
  } catch {
    return '—'
  }
}

const copyClassCode = async () => {
  if (!props.classItem?.classCode) return
  try {
    await navigator.clipboard.writeText(props.classItem.classCode)
    copied.value = true
    toast.success('Đã sao chép mã lớp!')
    setTimeout(() => { copied.value = false }, 2000)
  } catch {
    toast.error('Không thể sao chép mã lớp.')
  }
}

const close = () => {
  emit('update:modelValue', false)
}
</script>

<style scoped>
.cdd-wrapper {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: stretch;
  justify-content: flex-end;
  z-index: 1000;
}

.cdd-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: stretch;
  justify-content: flex-end;
  z-index: 1000;
}

.cdd-drawer {
  background: white;
  width: 100%;
  max-width: 480px;
  display: flex;
  flex-direction: column;
  box-shadow: -8px 0 32px rgba(0,0,0,0.15);
  overflow: hidden;
}

.dark .cdd-drawer { background: var(--ds-gray-800); }

.cdd-drawer__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  flex-shrink: 0;
}

.dark .cdd-drawer__header { border-bottom-color: var(--ds-border-strong); }

.cdd-drawer__title-area {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
}

.cdd-drawer__icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cdd-drawer__title {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
}

.dark .cdd-drawer__title { color: #f1f5f9; }

.cdd-drawer__subtitle {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.cdd-drawer__close {
  width: 36px;
  height: 36px;
  border: none;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  border-radius: var(--ds-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
  flex-shrink: 0;
}

.dark .cdd-drawer__close { background: var(--ds-gray-700); }
.cdd-drawer__close:hover { background: var(--ds-gray-200); color: var(--ds-text); }
.dark .cdd-drawer__close:hover { background: var(--ds-gray-600); }

.cdd-drawer__tabs {
  display: flex;
  gap: 0.25rem;
  padding: 0.75rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  flex-shrink: 0;
}

.dark .cdd-drawer__tabs { border-bottom-color: var(--ds-border-strong); }

.cdd-tab {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  border: none;
  background: transparent;
  color: var(--ds-text-muted);
  border-radius: var(--ds-radius-lg);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
}

.cdd-tab:hover { background: var(--ds-gray-100); color: var(--ds-text); }
.dark .cdd-tab:hover { background: var(--ds-gray-700); color: #f1f5f9; }

.cdd-tab--active {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.dark .cdd-tab--active { background: rgba(79, 70, 229, 0.15); color: var(--ds-primary); }

.cdd-tab__badge {
  background: var(--ds-primary);
  color: white;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 0.125rem 0.5rem;
  border-radius: 9999px;
  min-width: 20px;
  text-align: center;
}

.cdd-drawer__content {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
}

/* Info Tab */
.cdd-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.cdd-info-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 1.25rem;
  background: var(--ds-gray-50);
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  transition: color 0.25s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1), transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.dark .cdd-info-item { 
  background: var(--ds-gray-700); 
  border-color: var(--ds-border-strong);
}

.cdd-info-item:hover {
  border-color: var(--ds-primary-border);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.1);
}

.dark .cdd-info-item:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}
.cdd-info-item--full { grid-column: 1 / -1; }

.cdd-info-label {
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.cdd-info-value {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .cdd-info-value { color: #f1f5f9; }

.cdd-info-item--code {
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
}

.dark .cdd-info-item--code { background: rgba(79, 70, 229, 0.15); }

.cdd-class-code-wrapper {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.cdd-class-code {
  font-family: var(--ds-font-display);
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--ds-primary);
  letter-spacing: 0.1em;
}

.dark .cdd-class-code { color: var(--ds-primary); }

.cdd-copy-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: var(--ds-primary);
  color: white;
  border-radius: var(--ds-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.cdd-copy-btn:hover {
  background: var(--ds-primary-hover);
  transform: scale(1.05);
}

/* Students Tab */
.cdd-students-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.cdd-students-title {
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .cdd-students-title { color: #f1f5f9; }

.cdd-loading,
.cdd-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 2rem;
  color: var(--ds-text-muted);
  text-align: center;
}

.cdd-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--ds-border);
  border-top-color: var(--ds-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.cdd-spinner--sm {
  width: 16px;
  height: 16px;
  border-width: 2px;
}

@keyframes spin { to { transform: rotate(360deg); } }

.cdd-students-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.cdd-student-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
}

.dark .cdd-student-item { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }
.cdd-student-item:hover { border-color: var(--ds-primary); }

.cdd-student__avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 700;
  flex-shrink: 0;
}

.dark .cdd-student__avatar { background: rgba(79, 70, 229, 0.2); color: var(--ds-primary); }

.cdd-student__info {
  flex: 1;
  min-width: 0;
}

.cdd-student__name {
  display: block;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dark .cdd-student__name { color: #f1f5f9; }

.cdd-student__email {
  display: block;
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cdd-drawer__footer {
  padding: 1.25rem 1.5rem;
  border-top: 1px solid var(--ds-border);
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
}

.dark .cdd-drawer__footer { border-top-color: var(--ds-border-strong); }

/* Buttons */
.cdd-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.125rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  border: 1.5px solid transparent;
  white-space: nowrap;
}

.cdd-btn--primary {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.cdd-btn--primary:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(79, 70, 229, 0.35); }

.cdd-btn--outline {
  background: var(--ds-surface);
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .cdd-btn--outline { background: var(--ds-gray-700); border-color: var(--ds-border-strong); color: #94a3b8; }
.cdd-btn--outline:hover { border-color: var(--ds-primary); color: var(--ds-primary); }

.cdd-btn--danger {
  background: var(--ds-danger);
  color: white;
  border-color: var(--ds-danger);
}

.cdd-btn--danger:hover { background: #dc2626; }
.cdd-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }

.cdd-btn--secondary {
  background: var(--ds-surface);
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .cdd-btn--secondary { background: var(--ds-gray-700); border-color: var(--ds-border-strong); color: #94a3b8; }
.cdd-btn--secondary:hover { border-color: var(--ds-primary); color: var(--ds-primary); }

/* Transition */
.cdd-drawer-enter-active,
.cdd-drawer-leave-active {
  transition: color 0.3s ease, background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease, transform 0.3s ease;
}

.cdd-drawer-enter-from,
.cdd-drawer-leave-to {
  opacity: 0;
}

.cdd-drawer-enter-from .cdd-drawer,
.cdd-drawer-leave-to .cdd-drawer {
  transform: translateX(100%);
}
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}