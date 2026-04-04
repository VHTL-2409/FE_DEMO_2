<template>
  <div class="elr" :class="compact ? 'elr--compact' : ''">
    <button
      v-if="showMenu"
      ref="triggerRef"
      type="button"
      class="elr__trigger"
      @click="toggleMenu"
      @blur="onBlur"
    >
      <LucideIcon name="more_vert" />
    </button>

    <Teleport to="body">
      <div
        v-if="menuOpen && showMenu"
        ref="menuRef"
        class="elr__menu"
        :style="menuStyle"
      >
        <!-- View -->
        <button type="button" class="elr__item" @mousedown.prevent="emit('view', exam)">
          <LucideIcon name="visibility" />
          Xem chi tiết
        </button>

        <!-- Edit -->
        <button
          v-if="canEdit"
          type="button"
          class="elr__item"
          @mousedown.prevent="emit('edit', exam)"
        >
          <LucideIcon name="edit" />
          Chỉnh sửa
        </button>

        <!-- Duplicate -->
        <button type="button" class="elr__item" @mousedown.prevent="emit('duplicate', exam)">
          <LucideIcon name="content_copy" />
          Nhân bản
        </button>

        <!-- Schedule -->
        <button
          v-if="statusKey === 'ended'"
          type="button"
          class="elr__item"
          @mousedown.prevent="emit('schedule', exam)"
        >
          <LucideIcon name="event" />
          Tạo đợt mới
        </button>

        <!-- Divider -->
        <div v-if="canPublish || canUnpublish || canArchive || canUnarchive" class="elr__divider" />

        <!-- Publish -->
        <button
          v-if="canPublish"
          type="button"
          class="elr__item elr__item--success"
          @mousedown.prevent="emit('publish', exam)"
        >
          <LucideIcon name="publish" />
          Xuất bản
        </button>

        <!-- Unpublish -->
        <button
          v-if="canUnpublish"
          type="button"
          class="elr__item elr__item--warning"
          @mousedown.prevent="emit('unpublish', exam)"
        >
          <LucideIcon name="unpublished" />
          Hủy xuất bản
        </button>

        <!-- Archive -->
        <button
          v-if="canArchive"
          type="button"
          class="elr__item"
          @mousedown.prevent="emit('archive', exam)"
        >
          <LucideIcon name="archive" />
          Lưu trữ
        </button>

        <!-- Unarchive -->
        <button
          v-if="canUnarchive"
          type="button"
          class="elr__item"
          @mousedown.prevent="emit('unarchive', exam)"
        >
          <LucideIcon name="unarchive" />
          Bỏ lưu trữ
        </button>

        <!-- Divider -->
        <div class="elr__divider" />

        <!-- Delete -->
        <button
          type="button"
          class="elr__item elr__item--danger"
          @mousedown.prevent="emit('delete', exam)"
        >
          <LucideIcon name="delete" />
          Xóa đề thi
        </button>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'

const props = defineProps({
  exam: { type: Object, required: true },
  compact: { type: Boolean, default: false }
})

const emit = defineEmits([
  'view', 'edit', 'duplicate', 'schedule',
  'publish', 'unpublish', 'archive', 'unarchive', 'delete'
])

const triggerRef = ref(null)
const menuRef = ref(null)
const menuOpen = ref(false)
const menuStyle = ref({})

const statusKey = computed(() => {
  const e = props.exam
  if (e.isArchived) return 'archived'
  if (!e.isActive) return 'draft'
  const now = new Date()
  const start = e.startTime ? new Date(e.startTime) : null
  const end = e.endTime ? new Date(e.endTime) : null
  if (start && now < start) return 'upcoming'
  if ((!end || now <= end) && (!start || now >= start)) return 'live'
  return 'ended'
})

const canEdit = computed(() => ['draft', 'upcoming'].includes(statusKey.value))
const canPublish = computed(() => !props.exam.isActive && !props.exam.isArchived)
const canUnpublish = computed(() => props.exam.isActive && !props.exam.isArchived)
const canArchive = computed(() => !props.exam.isArchived)
const canUnarchive = computed(() => !!props.exam.isArchived)
const showMenu = computed(() => true)

const onBlur = () => {
  setTimeout(() => { menuOpen.value = false }, 150)
}

const updateMenuPosition = () => {
  if (!triggerRef.value) return
  const rect = triggerRef.value.getBoundingClientRect()
  menuStyle.value = {
    position: 'fixed',
    top: `${rect.bottom + 6}px`,
    right: `${window.innerWidth - rect.right}px`,
    left: 'auto',
    zIndex: 9999
  }
}

const toggleMenu = () => {
  if (menuOpen.value) {
    menuOpen.value = false
    return
  }
  updateMenuPosition()
  menuOpen.value = true
  nextTick(() => updateMenuPosition())
}

const onScrollOrResize = () => {
  if (menuOpen.value) updateMenuPosition()
}

watch(menuOpen, (open) => {
  if (open) {
    nextTick(() => updateMenuPosition())
  }
})

onMounted(() => {
  document.addEventListener('click', handleOutsideClick)
  window.addEventListener('scroll', onScrollOrResize, true)
  window.addEventListener('resize', onScrollOrResize)
})

onUnmounted(() => {
  document.removeEventListener('click', handleOutsideClick)
  window.removeEventListener('scroll', onScrollOrResize, true)
  window.removeEventListener('resize', onScrollOrResize)
})

const handleOutsideClick = (e) => {
  if (menuRef.value && !menuRef.value.contains(e.target) &&
      triggerRef.value && !triggerRef.value.contains(e.target)) {
    menuOpen.value = false
  }
}

const open = () => {
  updateMenuPosition()
  menuOpen.value = true
  nextTick(() => updateMenuPosition())
}

const close = () => { menuOpen.value = false }

defineExpose({ open, close })
</script>


<style scoped>
.elr {
  position: relative;
  display: flex;
  align-items: center;
}

.elr__trigger {
  width: 32px;
  height: 32px;
  border-radius: var(--ds-radius-md);
  border: none;
  background: transparent;
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.12s ease, background-color 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease, transform 0.12s ease;
}

.elr__trigger:hover {
  background: var(--ds-gray-100);
  color: var(--ds-text);
}

.dark .elr__trigger:hover {
  background: var(--ds-gray-700);
}

.elr--compact .elr__trigger {
  width: 28px;
  height: 28px;
}

.elr__menu {
  background: white;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  box-shadow: 0 8px 30px rgba(0,0,0,0.12);
  min-width: 180px;
  padding: 0.375rem;
  overflow: hidden;
}

.dark .elr__menu {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.elr__item {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  width: 100%;
  padding: 0.5625rem 0.75rem;
  border: none;
  border-radius: var(--ds-radius-lg);
  background: transparent;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text);
  cursor: pointer;
  text-align: left;
  transition: color 0.1s ease, background-color 0.1s ease, border-color 0.1s ease, box-shadow 0.1s ease, transform 0.1s ease;
  white-space: nowrap;
}

.dark .elr__item {
  color: #f1f5f9;
}

.elr__item:hover {
  background: var(--ds-gray-100);
}

.dark .elr__item:hover {
  background: var(--ds-gray-700);
}


.elr__item--success {
  color: var(--ds-success);
}

.elr__item--success:hover {
  background: var(--ds-success-soft);
}

.elr__item--warning {
  color: var(--ds-warning);
}

.elr__item--warning:hover {
  background: rgba(234, 179, 8, 0.1);
}

.elr__item--danger {
  color: var(--ds-danger);
}

.elr__item--danger:hover {
  background: var(--ds-danger-soft);
}

.elr__divider {
  height: 1px;
  background: var(--ds-border);
  margin: 0.375rem 0;
}

.dark .elr__divider {
  background: var(--ds-border-strong);
}
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}