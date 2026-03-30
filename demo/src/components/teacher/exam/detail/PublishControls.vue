<template>
  <div class="pc">

    <!-- Archive dropdown -->
    <div class="pc__dropdown" v-if="showMore">
      <Teleport to="body">
        <div class="pc__overlay" @click="showMore = false" />
        <div class="pc__menu" :style="dropdownStyle">
          <button type="button" class="pc__menu-item" @click="handleDuplicate">
            <LucideIcon name="content_copy" />
            Nhân bản đề thi
          </button>
          <div v-if="exam?.isArchived" class="pc__menu-item pc__menu-item--restore" @click="handleUnarchive">
            <LucideIcon name="unarchive" />
            Khôi phục đề thi
          </div>
          <div v-else class="pc__menu-item" @click="handleArchive">
            <LucideIcon name="archive" />
            Lưu trữ
          </div>
          <div class="pc__menu-divider" />
          <div class="pc__menu-item pc__menu-item--danger" @click="handleDelete">
            <LucideIcon name="delete" />
            Xóa đề thi
          </div>
        </div>
      </Teleport>
    </div>

    <!-- More actions button -->
    <button type="button" class="pc__more-btn" @click="showMore = !showMore">
      <LucideIcon name="more_horiz" />
    </button>

    <!-- Preview exam code -->
    <div v-if="exam?.code" class="pc__code-chip" @click="copyCode" :title="copied ? 'Đã copy!' : 'Copy mã thi'">
      <LucideIcon name="tag" />
      {{ copied ? 'Đã copy!' : exam.code }}
    </div>

    <!-- Main actions -->
    <div class="pc__main-actions">
      <!-- Unpublish (published) -->
      <button
        v-if="exam?.isActive && !exam?.isArchived"
        type="button"
        class="pc__btn pc__btn--unpublish"
        :disabled="loading"
        @click="handleUnpublish"
      >
        <LucideIcon name="do_not_disturb_on" />
        Hủy xuất bản
      </button>

      <!-- Publish (draft) -->
      <button
        v-else-if="!exam?.isActive && !exam?.isArchived"
        type="button"
        class="pc__btn pc__btn--publish"
        :disabled="loading"
        @click="handlePublish"
      >
        <LucideIcon name="rocket_launch" />
        {{ loading ? 'Đang xuất bản...' : 'Xuất bản' }}
      </button>

      <!-- Archived state -->
      <div v-if="exam?.isArchived" class="pc__archived-label">
        <LucideIcon name="archive" />
        Đã lưu trữ
      </div>
    </div>

    <!-- Unpublish confirm dialog -->
    <ConfirmDialog
      v-model="showUnpublishConfirm"
      title="Hủy xuất bản đề thi"
      :message="`Bạn có chắc muốn hủy xuất bản đề thi &quot;${exam?.title}&quot;? Học sinh sẽ không thể truy cập bài thi.`"
      confirm-label="Hủy xuất bản"
      cancel-label="Giữ nguyên"
      variant="warning"
      :loading="loading"
      @confirm="confirmUnpublish"
    />

    <!-- Archive confirm dialog -->
    <ConfirmDialog
      v-model="showArchiveConfirm"
      title="Lưu trữ đề thi"
      :message="exam?.isActive
        ? `Đề thi &quot;${exam?.title}&quot; đang hoạt động. Bạn có chắc muốn lưu trữ?`
        : `Bạn có chắc muốn lưu trữ đề thi &quot;${exam?.title}&quot;?`"
      confirm-label="Lưu trữ"
      cancel-label="Hủy bỏ"
      variant="warning"
      :loading="loading"
      @confirm="confirmArchive"
    />

    <!-- Delete confirm dialog -->
    <ConfirmDialog
      v-model="showDeleteConfirm"
      title="Xóa đề thi"
      :message="`Bạn có chắc muốn xóa vĩnh viễn đề thi &quot;${exam?.title}&quot;? Hành động này không thể hoàn tác.`"
      confirm-label="Xóa đề thi"
      cancel-label="Hủy bỏ"
      variant="danger"
      :loading="loading"
      @confirm="confirmDelete"
    />

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import ConfirmDialog from '../../../ui/ConfirmDialog.vue'

const props = defineProps({
  exam: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['publish', 'unpublish', 'archive', 'unarchive', 'duplicate', 'delete'])

const showMore = ref(false)
const copied = ref(false)
const showUnpublishConfirm = ref(false)
const showArchiveConfirm = ref(false)
const showDeleteConfirm = ref(false)
const dropdownStyle = ref({})

const copyCode = async () => {
  if (!props.exam?.code) return
  try {
    await navigator.clipboard.writeText(props.exam.code)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch { /* fallback */ }
}

const updateDropdownPosition = () => {
  // Simple positioning — menu appears above the button
  const btn = document.querySelector('.pc__more-btn')
  if (btn) {
    const rect = btn.getBoundingClientRect()
    dropdownStyle.value = {
      position: 'fixed',
      top: `${rect.bottom + 8}px`,
      right: `${window.innerWidth - rect.right}px`,
      zIndex: 2000
    }
  }
}

const handlePublish = () => emit('publish')
const handleUnpublish = () => { showUnpublishConfirm.value = true }
const handleArchive = () => { showMore.value = false; showArchiveConfirm.value = true }
const handleUnarchive = () => { showMore.value = false; emit('unarchive') }
const handleDuplicate = () => { showMore.value = false; emit('duplicate') }
const handleDelete = () => { showMore.value = false; showDeleteConfirm.value = true }

const confirmUnpublish = () => {
  showUnpublishConfirm.value = false
  emit('unpublish')
}

const confirmArchive = () => {
  showArchiveConfirm.value = false
  emit('archive')
}

const confirmDelete = () => {
  showDeleteConfirm.value = false
  emit('delete')
}

onMounted(() => {
  document.addEventListener('click', updateDropdownPosition)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', updateDropdownPosition)
})
</script>


<style scoped>
.pc {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  position: relative;
}

/* Code chip */
.pc__code-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--ds-radius-full);
  background: var(--ds-primary-soft);
  border: 1px solid var(--ds-primary-border);
  color: var(--ds-primary);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  user-select: none;
}

.pc__code-chip:hover {
  background: var(--ds-primary);
  color: white;
}


/* Main actions */
.pc__main-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

/* Buttons */
.pc__btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid transparent;
  white-space: nowrap;
}


.pc__btn--publish {
  background: linear-gradient(135deg, var(--ds-success) 0%, #059669 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(22, 163, 74, 0.25);
}

.pc__btn--publish:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(22, 163, 74, 0.35);
}

.pc__btn--unpublish {
  background: rgba(234, 179, 8, 0.1);
  border-color: rgba(234, 179, 8, 0.3);
  color: #d97706;
}

.pc__btn--unpublish:hover:not(:disabled) {
  background: rgba(234, 179, 8, 0.2);
  border-color: #d97706;
}

.pc__btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }

/* Archived label */
.pc__archived-label {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-xl);
  background: rgba(100, 116, 139, 0.1);
  border: 1px solid rgba(100, 116, 139, 0.2);
  color: var(--ds-gray-500);
  font-size: 0.8rem;
  font-weight: 700;
}


/* More button */
.pc__more-btn {
  width: 2rem;
  height: 2rem;
  border-radius: var(--ds-radius-md);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.12s ease;
}

.dark .pc__more-btn { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.pc__more-btn:hover {
  background: var(--ds-gray-100);
  color: var(--ds-text);
}

.dark .pc__more-btn:hover { background: var(--ds-gray-700); }


/* Dropdown */
.pc__overlay {
  position: fixed;
  inset: 0;
  z-index: 1999;
}

.pc__menu {
  background: white;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  min-width: 200px;
  padding: 0.375rem;
  z-index: 2000;
}

.dark .pc__menu { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.pc__menu-item {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.625rem 0.875rem;
  border-radius: var(--ds-radius-lg);
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
  cursor: pointer;
  transition: all 0.12s ease;
  border: none;
  background: transparent;
  text-align: left;
  width: 100%;
  font-family: inherit;
}

.dark .pc__menu-item { color: #94a3b8; }

.pc__menu-item:hover {
  background: var(--ds-gray-50);
  color: var(--ds-text);
}

.dark .pc__menu-item:hover { background: var(--ds-gray-700); }

.pc__menu-item:hover
.dark .pc__menu-item:hover

.pc__menu-item--danger {
  color: var(--ds-danger);
}

.pc__menu-item--danger:hover {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}


.pc__menu-item--restore {
  color: var(--ds-primary);
}

.pc__menu-item--restore:hover {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}


.pc__menu-divider {
  height: 1px;
  background: var(--ds-border);
  margin: 0.375rem 0;
}

.dark .pc__menu-divider { background: var(--ds-border-strong); }
</style>
