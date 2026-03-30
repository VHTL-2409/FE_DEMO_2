<template>
  <div class="ns">
    <!-- Card header -->
    <div class="ns__header">
      <div class="ns__header-icon">
        <LucideIcon name="notifications" />
      </div>
      <div>
        <h3 class="ns__title">Thông báo</h3>
        <p class="ns__subtitle">Quản lý cách bạn nhận thông báo</p>
      </div>
    </div>

    <!-- Content -->
    <div class="ns__content">
      <div
        v-for="(group, groupKey) in notificationGroups"
        :key="groupKey"
        class="ns__group"
      >
        <div class="ns__group-header">
          <LucideIcon :name="group.icon" />
          <h4 class="ns__group-title">{{ group.label }}</h4>
        </div>

        <div class="ns__toggles">
          <div
            v-for="item in group.items"
            :key="item.key"
            class="ns__toggle-row"
          >
            <div class="ns__toggle-info">
              <span class="ns__toggle-label">{{ item.label }}</span>
              <span class="ns__toggle-desc">{{ item.description }}</span>
            </div>
            <button
              type="button"
              class="ns__toggle-btn"
              :class="{ 'ns__toggle-btn--on': isOn(item.key) }"
              role="switch"
              :aria-checked="isOn(item.key)"
              @click="toggleNotification(item.key)"
            >
              <span class="ns__toggle-thumb" />
            </button>
          </div>
        </div>
      </div>

      <!-- Notification summary -->
      <div class="ns__summary">
        <div class="ns__summary-icon">
          <LucideIcon name="notifications_active" />
        </div>
        <div class="ns__summary-info">
          <span class="ns__summary-label">{{ activeCount }} / {{ totalCount }} thông báo đang bật</span>
          <div class="ns__summary-bar">
            <div class="ns__summary-fill" :style="{ width: summaryPercent + '%' }" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'

const emit = defineEmits(['update'])

const notifications = reactive({
  email_exam_results: true,
  email_exam_reminder: true,
  email_new_student: false,
  email_security_alert: true,
  push_exam_alerts: true,
  push_violation: true,
  push_student_join: false,
  push_submission: true,
  inapp_exam_alerts: true,
  inapp_violations: true,
  inapp_messages: true,
  inapp_system: true
})

const notificationGroups = {
  email: {
    icon: 'mail',
    label: 'Email',
    items: [
      { key: 'email_exam_results', label: 'Kết quả thi', description: 'Thông báo khi có kết quả thi mới' },
      { key: 'email_exam_reminder', label: 'Nhắc nhở lịch thi', description: 'Nhắc trước khi kỳ thi bắt đầu' },
      { key: 'email_new_student', label: 'Học sinh mới', description: 'Khi có học sinh đăng ký lớp' },
      { key: 'email_security_alert', label: 'Cảnh báo bảo mật', description: 'Đăng nhập bất thường, thay đổi mật khẩu' }
    ]
  },
  push: {
    icon: 'notifications_active',
    label: 'Đẩy (Push)',
    items: [
      { key: 'push_exam_alerts', label: 'Cảnh báo kỳ thi', description: 'Vi phạm, sự cố trong phòng thi' },
      { key: 'push_violation', label: 'Vi phạm của học sinh', description: 'Cảnh báo khi phát hiện vi phạm' },
      { key: 'push_student_join', label: 'Học sinh vào phòng', description: 'Khi học sinh bắt đầu làm bài' },
      { key: 'push_submission', label: 'Nộp bài', description: 'Khi học sinh nộp bài thi' }
    ]
  },
  inapp: {
    icon: 'apps',
    label: 'Trong ứng dụng',
    items: [
      { key: 'inapp_exam_alerts', label: 'Cảnh báo kỳ thi', description: 'Thông báo real-time trong dashboard' },
      { key: 'inapp_violations', label: 'Vi phạm thi', description: 'Badge/alert khi có vi phạm mới' },
      { key: 'inapp_messages', label: 'Tin nhắn', description: 'Nhắn tin giữa giáo viên và học sinh' },
      { key: 'inapp_system', label: 'Thông báo hệ thống', description: 'Cập nhật tính năng, bảo trì' }
    ]
  }
}

const totalCount = computed(() => Object.keys(notifications).length)
const activeCount = computed(() => Object.values(notifications).filter(Boolean).length)
const summaryPercent = computed(() => Math.round((activeCount.value / totalCount.value) * 100))

const isOn = (key) => !!notifications[key]

const toggleNotification = (key) => {
  notifications[key] = !notifications[key]
  emit('update', { key, value: notifications[key] })
}

defineExpose({ notifications })
</script>


<style scoped>
.ns {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.dark .ns {
  border-color: var(--ds-border-strong);
}

/* Header */
.ns__header {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1.125rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .ns__header {
  border-bottom-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.ns__header-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  background: rgba(14, 165, 233, 0.08);
  color: var(--ds-info);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.ns__title {
  font-family: var(--ds-font-display);
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .ns__title {
  color: #f1f5f9;
}

.ns__subtitle {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
  font-weight: 500;
}

/* Content */
.ns__content {
  display: flex;
  flex-direction: column;
}

/* Group */
.ns__group {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
}

.dark .ns__group {
  border-bottom-color: var(--ds-border-strong);
}

.ns__group:last-of-type {
  border-bottom: none;
}

.ns__group-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.875rem;
}


.ns__group-title {
  font-size: 0.8rem;
  font-weight: 800;
  color: var(--ds-text-muted);
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.dark .ns__group-title {
  color: #94a3b8;
}

/* Toggle rows */
.ns__toggles {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.ns__toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.625rem 0.75rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-gray-50);
  transition: background 0.1s ease;
}

.dark .ns__toggle-row {
  background: var(--ds-gray-800);
}

.ns__toggle-row:hover {
  background: var(--ds-gray-100);
}

.dark .ns__toggle-row:hover {
  background: var(--ds-gray-700);
}

.ns__toggle-info {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  min-width: 0;
}

.ns__toggle-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ds-text);
}

.dark .ns__toggle-label {
  color: #f1f5f9;
}

.ns__toggle-desc {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}

/* Toggle switch */
.ns__toggle-btn {
  width: 44px;
  height: 24px;
  border-radius: 12px;
  background: var(--ds-gray-300);
  border: none;
  cursor: pointer;
  position: relative;
  transition: background 0.2s ease;
  flex-shrink: 0;
}

.dark .ns__toggle-btn {
  background: var(--ds-gray-600);
}

.ns__toggle-btn--on {
  background: var(--ds-primary);
}

.dark .ns__toggle-btn--on {
  background: var(--ds-primary);
}

.ns__toggle-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s ease;
}

.ns__toggle-btn--on .ns__toggle-thumb {
  transform: translateX(20px);
}

/* Summary */
.ns__summary {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 0.875rem 1.25rem;
  background: var(--ds-gray-50);
  border-top: 1px solid var(--ds-border);
}

.dark .ns__summary {
  background: var(--ds-gray-800);
  border-top-color: var(--ds-border-strong);
}

.ns__summary-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.ns__summary-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.ns__summary-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .ns__summary-label {
  color: #f1f5f9;
}

.ns__summary-bar {
  height: 6px;
  background: var(--ds-gray-200);
  border-radius: 3px;
  overflow: hidden;
}

.dark .ns__summary-bar {
  background: var(--ds-gray-600);
}

.ns__summary-fill {
  height: 100%;
  background: var(--ds-primary);
  border-radius: 3px;
  transition: width 0.3s ease;
}
</style>
