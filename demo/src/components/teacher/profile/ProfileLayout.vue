<template>
  <div class="pl">
    <!-- Profile Header (sticky top) -->
    <div class="pl__header">
      <slot name="header" />
    </div>

    <!-- Navigation tabs -->
    <div class="pl__nav">
      <div class="pl__nav-inner">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          type="button"
          class="pl__nav-tab"
          :class="{ 'pl__nav-tab--active': activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          <LucideIcon :name="tab.icon" />
          <span class="pl__nav-label">{{ tab.label }}</span>
        </button>
      </div>
    </div>

    <!-- Tab content -->
    <div class="pl__content">
      <div v-show="activeTab === 'personal'" class="pl__tab-panel">
        <slot name="tab-personal" />
      </div>
      <div v-show="activeTab === 'professional'" class="pl__tab-panel">
        <slot name="tab-professional" />
      </div>
      <div v-show="activeTab === 'security'" class="pl__tab-panel">
        <slot name="tab-security" />
      </div>
      <div v-show="activeTab === 'notifications'" class="pl__tab-panel">
        <slot name="tab-notifications" />
      </div>
      <div v-show="activeTab === 'preferences'" class="pl__tab-panel">
        <slot name="tab-preferences" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  defaultTab: { type: String, default: 'personal' }
})

const activeTab = ref(props.defaultTab)

const tabs = [
  { id: 'personal', label: 'Cá nhân', icon: 'person' },
  { id: 'professional', label: 'Chuyên môn', icon: 'school' },
  { id: 'security', label: 'Bảo mật', icon: 'lock' },
  { id: 'notifications', label: 'Thông báo', icon: 'notifications' },
  { id: 'preferences', label: 'Tùy chọn', icon: 'settings' }
]

defineExpose({ activeTab })
</script>


<style scoped>
.pl {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  max-width: 900px;
  margin: 0 auto;
  padding: 1.25rem;
}

/* Nav tabs */
.pl__nav {
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--ds-bg);
  padding: 0.5rem 0;
  margin: -0.5rem 0;
}

.dark .pl__nav {
  background: var(--ds-bg);
}

.pl__nav-inner {
  display: flex;
  gap: 0.25rem;
  overflow-x: auto;
  scrollbar-width: none;
  border-bottom: 2px solid var(--ds-border);
  padding-bottom: 0;
}

.dark .pl__nav-inner {
  border-bottom-color: var(--ds-border-strong);
}

.pl__nav-inner::-webkit-scrollbar {
  display: none;
}

.pl__nav-tab {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  border-radius: var(--ds-radius-lg) var(--ds-radius-lg) 0 0;
  border: none;
  background: transparent;
  color: var(--ds-text-muted);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  white-space: nowrap;
  position: relative;
  font-family: inherit;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
}

.pl__nav-tab:hover {
  color: var(--ds-text);
  background: var(--ds-gray-50);
}

.dark .pl__nav-tab:hover {
  background: var(--ds-gray-800);
  color: #f1f5f9;
}

.pl__nav-tab--active {
  color: var(--ds-primary);
  background: transparent;
  border-bottom: 2px solid var(--ds-primary);
}

.pl__nav-tab--active:hover {
  background: var(--ds-primary-soft);
}

.dark .pl__nav-tab--active:hover {
  background: rgba(79, 70, 229, 0.1);
}

.pl__nav-icon {
  font-size: 1.125rem;
}

.pl__nav-label {
  font-size: 0.875rem;
}

/* Tab panels */
.pl__tab-panel {
  animation: tabFadeIn 0.15s ease;
}

@keyframes tabFadeIn {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Responsive */
@media (max-width: 640px) {
  .pl {
    padding: 1rem;
  }

  .pl__nav-tab {
    padding: 0.5rem 0.75rem;
  }

  .pl__nav-icon {
    font-size: 1rem;
  }

  .pl__nav-label {
    font-size: 0.8rem;
  }
}
</style>
