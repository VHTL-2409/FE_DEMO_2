<template>
  <div class="esf">
    <!-- Filter tabs -->
    <div class="esf__tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        class="esf__tab"
        :class="{ 'esf__tab--active': activeTab === tab.key }"
        @click="$emit('tab-change', tab.key)"
      >
        {{ tab.label }}
        <span v-if="tabCounts[tab.key] > 0" class="esf__tab-count">{{ tabCounts[tab.key] }}</span>
      </button>
    </div>

    <!-- Search -->
    <div class="esf__search-wrap">
      <LucideIcon name="search" />
      <input
        v-model="localSearch"
        type="text"
        class="esf__search"
        :placeholder="placeholder"
        @input="$emit('search', localSearch)"
      />
      <button
        v-if="localSearch"
        type="button"
        class="esf__search-clear"
        @click="localSearch = ''; $emit('search', '')"
      >
        <LucideIcon name="close" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  activeTab: { type: String, default: 'all' },
  tabCounts: { type: Object, default: () => ({}) },
  searchValue: { type: String, default: '' },
  placeholder: { type: String, default: 'Tìm kiếm kỳ thi...' }
})

defineEmits(['tab-change', 'search'])

const localSearch = ref(props.searchValue)
watch(() => props.searchValue, (v) => { localSearch.value = v })

const tabs = [
  { key: 'all', label: 'Tất cả' },
  { key: 'today', label: 'Hôm nay' },
  { key: 'upcoming', label: 'Sắp tới' },
  { key: 'completed', label: 'Đã xong' }
]
</script>


<style scoped>
.esf {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

/* Tabs */
.esf__tabs {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  background: var(--ds-gray-100);
  padding: 0.25rem;
  border-radius: var(--ds-radius-xl);
}

.dark .esf__tabs { background: var(--ds-gray-800); }

.esf__tab {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-lg);
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.12s ease;
  font-family: inherit;
  white-space: nowrap;
}

.esf__tab:hover {
  color: var(--ds-text);
  background: var(--ds-surface);
}

.dark .esf__tab:hover {
  color: #f1f5f9;
  background: var(--ds-gray-700);
}

.esf__tab--active {
  background: var(--ds-surface);
  color: var(--ds-primary);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.dark .esf__tab--active {
  background: var(--ds-gray-700);
  color: var(--ds-primary);
}

.esf__tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 0.25rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 800;
  background: var(--ds-gray-200);
  color: var(--ds-text-muted);
}

.dark .esf__tab-count { background: var(--ds-gray-600); color: var(--ds-gray-300); }

.esf__tab--active .esf__tab-count {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

/* Search */
.esf__search-wrap {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1;
  max-width: 300px;
  min-width: 200px;
}

.esf__search-icon {
  position: absolute;
  left: 0.875rem;
  font-size: 1.125rem;
  color: var(--ds-text-muted);
  pointer-events: none;
}

.esf__search {
  width: 100%;
  padding: 0.625rem 2.5rem 0.625rem 2.75rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  background: var(--ds-surface);
  color: var(--ds-text);
  font-size: 0.875rem;
  font-family: inherit;
  outline: none;
  transition: border-color 0.12s, box-shadow 0.12s;
}

.dark .esf__search {
  border-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
  color: #f1f5f9;
}

.esf__search::placeholder { color: var(--ds-text-muted); }

.esf__search:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.esf__search-clear {
  position: absolute;
  right: 0.5rem;
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  border: none;
  background: var(--ds-gray-200);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.12s ease;
}

.dark .esf__search-clear { background: var(--ds-gray-700); }
.esf__search-clear:hover { background: var(--ds-gray-300); color: var(--ds-text); }
.dark .esf__search-clear:hover { background: var(--ds-gray-600); }

/* Responsive */
@media (max-width: 640px) {
  .esf { flex-direction: column; align-items: stretch; }
  .esf__tabs { overflow-x: auto; }
  .esf__search-wrap { max-width: 100%; }
}
</style>
