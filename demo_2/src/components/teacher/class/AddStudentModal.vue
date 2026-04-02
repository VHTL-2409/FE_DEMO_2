<template>
  <Transition name="asm-modal">
    <div v-if="modelValue" class="asm-overlay" @click.self="close">
      <div class="asm-modal asm-modal--lg">
        <div class="asm-modal__header">
          <div class="asm-modal__icon">
            <LucideIcon name="user-plus" />
          </div>
          <div>
            <h3 class="asm-modal__title">Thêm học sinh vào lớp</h3>
            <p class="asm-modal__subtitle">{{ className }}</p>
          </div>
          <button type="button" class="asm-modal__close" @click="close">
            <LucideIcon name="x" />
          </button>
        </div>

        <div class="asm-modal__search">
          <LucideIcon name="search" class="asm-search-icon" />
          <input
            v-model="searchQuery"
            type="text"
            class="asm-search-input"
            placeholder="Tìm kiếm học sinh..."
            @input="debounceSearch"
          />
        </div>

        <div class="asm-modal__body">
          <div v-if="isLoading" class="asm-loading">
            <div class="asm-spinner"></div>
            <p>Đang tải...</p>
          </div>
          <div v-else-if="availableStudents.length === 0" class="asm-empty">
            <LucideIcon name="users" />
            <p>{{ searchQuery ? 'Không tìm thấy học sinh' : 'Tất cả học sinh đã trong lớp' }}</p>
          </div>
          <div v-else class="asm-list">
            <label
              v-for="student in availableStudents"
              :key="student.studentId"
              class="asm-item"
              :class="{ 'asm-item--selected': isSelected(student.studentId) }"
            >
              <input
                type="checkbox"
                :checked="isSelected(student.studentId)"
                class="asm-checkbox"
                @change="toggleStudent(student)"
              />
              <div class="asm-item__avatar">
                {{ getInitials(student.studentUsername) }}
              </div>
              <div class="asm-item__info">
                <span class="asm-item__name">{{ student.studentUsername }}</span>
                <span class="asm-item__email">{{ student.studentEmail }}</span>
              </div>
              <LucideIcon v-if="isSelected(student.studentId)" name="check-circle" class="asm-item__check" />
            </label>
          </div>
        </div>

        <div class="asm-modal__footer">
          <div class="asm-selected-info">
            <LucideIcon name="check-square" />
            <span>{{ selectedStudents.length }} học sinh được chọn</span>
          </div>
          <div class="asm-actions">
            <button type="button" class="asm-btn asm-btn--outline" @click="close">Hủy</button>
            <button
              type="button"
              class="asm-btn asm-btn--primary"
              :disabled="loading || selectedStudents.length === 0"
              @click="handleAdd"
            >
              <span v-if="loading" class="asm-spinner"></span>
              <template v-else>
                <LucideIcon name="plus" />
                Thêm {{ selectedStudents.length > 0 ? `${selectedStudents.length} học sinh` : '' }}
              </template>
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ApiError } from '../../../services/apiClient'
import { getAvailableStudents, addStudentsToClass } from '../../../services/classService'
import { useToast } from '../../../composables/useToast'
import LucideIcon from '../../common/LucideIcon.vue'

const props = defineProps({
  modelValue: Boolean,
  classId: [Number, String],
  className: String
})

const emit = defineEmits(['update:modelValue', 'added'])

const toast = useToast()

const availableStudents = ref([])
const selectedStudents = ref([])
const isLoading = ref(false)
const loading = ref(false)
const searchQuery = ref('')
let searchTimeout = null

watch(() => props.modelValue, async (val) => {
  if (val) {
    selectedStudents.value = []
    searchQuery.value = ''
    await loadAvailableStudents()
  }
})

const loadAvailableStudents = async () => {
  isLoading.value = true
  try {
    const data = await getAvailableStudents(props.classId)
    availableStudents.value = data || []
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể tải danh sách học sinh.')
  } finally {
    isLoading.value = false
  }
}

const debounceSearch = () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    loadAvailableStudents()
  }, 300)
}

const getInitials = (name) => {
  if (!name) return '?'
  return name.slice(0, 2).toUpperCase()
}

const isSelected = (studentId) => {
  return selectedStudents.value.some(s => s.studentId === studentId)
}

const toggleStudent = (student) => {
  const idx = selectedStudents.value.findIndex(s => s.studentId === student.studentId)
  if (idx >= 0) {
    selectedStudents.value.splice(idx, 1)
  } else {
    selectedStudents.value.push(student)
  }
}

const close = () => {
  emit('update:modelValue', false)
}

const handleAdd = async () => {
  if (selectedStudents.value.length === 0) return
  loading.value = true
  try {
    const studentIds = selectedStudents.value.map(s => s.studentId)
    await addStudentsToClass(props.classId, studentIds)
    toast.success(`Đã thêm ${selectedStudents.value.length} học sinh vào lớp.`)
    emit('added')
    close()
  } catch (err) {
    toast.error(err instanceof ApiError ? err.message : 'Không thể thêm học sinh.')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.asm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1.5rem;
}

.asm-modal {
  background: white;
  border-radius: var(--ds-radius-2xl);
  width: 100%;
  max-width: 480px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 24px 64px rgba(0,0,0,0.2);
  overflow: hidden;
}

.dark .asm-modal { background: var(--ds-gray-800); }
.asm-modal--lg { max-width: 560px; }

.asm-modal__header {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
  padding: 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  flex-shrink: 0;
}

.dark .asm-modal__header { border-bottom-color: var(--ds-border-strong); }

.asm-modal__icon {
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

.asm-modal__title {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
}

.dark .asm-modal__title { color: var(--ds-text); }

.asm-modal__subtitle {
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.asm-modal__close {
  margin-left: auto;
  width: 32px;
  height: 32px;
  border: none;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  border-radius: var(--ds-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.12s ease;
  flex-shrink: 0;
}

.dark .asm-modal__close { background: var(--ds-gray-700); }
.asm-modal__close:hover { background: var(--ds-gray-200); color: var(--ds-text); }
.dark .asm-modal__close:hover { background: var(--ds-gray-600); }

.asm-modal__search {
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  flex-shrink: 0;
}

.dark .asm-modal__search { border-bottom-color: var(--ds-border-strong); }

.asm-search-icon {
  position: absolute;
  left: 2.5rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--ds-text-muted);
  width: 18px;
  height: 18px;
}

.asm-search-input {
  width: 100%;
  padding: 0.875rem 1.25rem 0.875rem 3rem;
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-3xl);
  font-size: 0.9375rem;
  background: var(--ds-surface);
  color: var(--ds-text);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  line-height: 1.5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.dark .asm-search-input {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: var(--ds-text);
}

.asm-search-input:focus {
  outline: none;
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 4px var(--ds-primary-ring), 0 8px 24px rgba(79, 70, 229, 0.15);
  transform: translateY(-1px);
}

.dark .asm-search-input:focus {
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.3), 0 8px 24px rgba(0, 0, 0, 0.3);
}

.asm-modal__body {
  flex: 1;
  overflow-y: auto;
  padding: 1rem 1.5rem;
  min-height: 200px;
}

.asm-loading,
.asm-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 2rem;
  color: var(--ds-text-muted);
}

.asm-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--ds-border);
  border-top-color: var(--ds-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.asm-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.asm-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  cursor: pointer;
  transition: all 0.12s ease;
  background: var(--ds-surface);
}

.dark .asm-item {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.asm-item:hover {
  border-color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.dark .asm-item:hover { background: rgba(79, 70, 229, 0.1); }

.asm-item--selected {
  border-color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.dark .asm-item--selected { background: rgba(79, 70, 229, 0.1); }

.asm-checkbox {
  width: 20px;
  height: 20px;
  accent-color: var(--ds-primary);
  cursor: pointer;
  flex-shrink: 0;
  border-radius: 6px;
}

.asm-item__avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 700;
  flex-shrink: 0;
}

.dark .asm-item__avatar { background: rgba(79, 70, 229, 0.2); color: var(--ds-primary); }

.asm-item__info {
  flex: 1;
  min-width: 0;
}

.asm-item__name {
  display: block;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dark .asm-item__name { color: var(--ds-text); }

.asm-item__email {
  display: block;
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.asm-item__check {
  color: var(--ds-primary);
  flex-shrink: 0;
}

.asm-modal__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.25rem 1.5rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
  flex-shrink: 0;
}

.dark .asm-modal__footer {
  border-top-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.asm-selected-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
}

.dark .asm-selected-info { color: var(--ds-text-muted); }

.asm-actions {
  display: flex;
  gap: 0.75rem;
}

.asm-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.125rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1.5px solid transparent;
  white-space: nowrap;
}

.asm-btn--primary {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.asm-btn--primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.35);
}

.asm-btn--outline {
  background: var(--ds-surface);
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .asm-btn--outline {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: var(--ds-text-muted);
}

.asm-btn--outline:hover { border-color: var(--ds-primary); color: var(--ds-primary); }
.asm-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }

/* Transition */
.asm-modal-enter-active,
.asm-modal-leave-active {
  transition: all 0.2s ease;
}

.asm-modal-enter-from,
.asm-modal-leave-to {
  opacity: 0;
}

.asm-modal-enter-from .asm-modal,
.asm-modal-leave-to .asm-modal {
  transform: scale(0.95) translateX(10px);
}
</style>
