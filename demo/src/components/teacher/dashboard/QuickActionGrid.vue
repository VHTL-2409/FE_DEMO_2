<template>
  <div class="td-qa">
    <div class="td-qa__header">
      <h3 class="td-qa__title">Thao tác nhanh</h3>
      <p class="td-qa__sub">Truy cập các chức năng phổ biến</p>
    </div>

    <div class="td-qa__grid">
      <button
        v-for="action in actions"
        :key="action.id"
        type="button"
        class="td-qa__item"
        @click="$emit('action', action.id)"
      >
        <div class="td-qa__item-icon" :class="`td-qa__item-icon--${action.color}`">
          <LucideIcon :name="action.icon" />
        </div>
        <div class="td-qa__item-body">
          <span class="td-qa__item-title">{{ action.title }}</span>
          <span class="td-qa__item-sub">{{ action.subtitle }}</span>
        </div>
        <LucideIcon name="chevron_right" td-qa__item-arrow />
      </button>
    </div>
  </div>
</template>

<script setup>
defineEmits(['action'])

const actions = [
  {
    id: 'create',
    icon: 'add_circle',
    title: 'Tạo đề thi mới',
    subtitle: 'Lên lịch & cấu hình proctor',
    color: 'primary'
  },
  {
    id: 'list',
    icon: 'list_alt',
    title: 'Danh sách đề thi',
    subtitle: 'Quản lý tất cả đề thi',
    color: 'info'
  },
  {
    id: 'monitoring',
    icon: 'live_tv',
    title: 'Giám sát trực tiếp',
    subtitle: 'Theo dõi kỳ thi đang thi',
    color: 'success'
  },
  {
    id: 'analytics',
    icon: 'bar_chart',
    title: 'Phân tích & Báo cáo',
    subtitle: 'Xem thống kê chi tiết',
    color: 'accent'
  },
  {
    id: 'profile',
    icon: 'manage_accounts',
    title: 'Hồ sơ cá nhân',
    subtitle: 'Cập nhật thông tin',
    color: 'muted'
  },
  {
    id: 'help',
    icon: 'help',
    title: 'Trung tâm trợ giúp',
    subtitle: 'Hướng dẫn sử dụng',
    color: 'muted'
  }
]
</script>


<style scoped>
.td-qa {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.td-qa__header {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.td-qa__title {
  font-family: var(--ds-font-display);
  font-size: 1rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .td-qa__title {
  color: #f1f5f9;
}

.td-qa__sub {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0;
}

.td-qa__grid {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.td-qa__item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 0.875rem;
  border-radius: var(--ds-radius-xl);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: left;
  width: 100%;
}

.td-qa__item:hover {
  border-color: var(--ds-primary-border);
  background: var(--ds-primary-soft);
  transform: translateX(2px);
}

.dark .td-qa__item:hover {
  background: rgba(79, 70, 229, 0.1);
}

.td-qa__item:active {
  transform: translateX(0);
}

.td-qa__item-icon {
  width: 38px;
  height: 38px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.15s ease;
}


.td-qa__item-icon--primary {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.td-qa__item:hover .td-qa__item-icon--primary {
  background: var(--ds-primary);
  color: white;
}

.td-qa__item-icon--info {
  background: var(--ds-info-soft);
  color: var(--ds-info);
}

.td-qa__item:hover .td-qa__item-icon--info {
  background: var(--ds-info);
  color: white;
}

.td-qa__item-icon--success {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.td-qa__item:hover .td-qa__item-icon--success {
  background: var(--ds-success);
  color: white;
}

.td-qa__item-icon--accent {
  background: var(--ds-accent-soft);
  color: var(--ds-accent);
}

.td-qa__item:hover .td-qa__item-icon--accent {
  background: var(--ds-accent);
  color: white;
}

.td-qa__item-icon--muted {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
}

.dark .td-qa__item-icon--muted {
  background: var(--ds-gray-700);
}

.td-qa__item:hover .td-qa__item-icon--muted {
  background: var(--ds-gray-300);
  color: var(--ds-text);
}

.dark .td-qa__item:hover .td-qa__item-icon--muted {
  background: var(--ds-gray-600);
  color: white;
}

.td-qa__item-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  min-width: 0;
}

.td-qa__item-title {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
  line-height: 1.2;
}

.dark .td-qa__item-title {
  color: #f1f5f9;
}

.td-qa__item:hover .td-qa__item-title {
  color: var(--ds-primary);
}

.dark .td-qa__item:hover .td-qa__item-title {
  color: #a5b4fc;
}

.td-qa__item-sub {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
}

.td-qa__item-arrow {
  font-size: 1.125rem;
  color: var(--ds-text-muted);
  flex-shrink: 0;
  transition: all 0.15s ease;
}

.td-qa__item:hover .td-qa__item-arrow {
  color: var(--ds-primary);
  transform: translateX(2px);
}
</style>
