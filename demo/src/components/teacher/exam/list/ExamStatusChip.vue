<template>
  <span class="el-chip" :class="`el-chip--${chipVariant}`">
    <span v-if="showDot" class="el-chip__dot" />
    {{ label }}
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: {
    type: String,
    default: 'draft'
  },
  isArchived: {
    type: Boolean,
    default: false
  }
})

const chipVariant = computed(() => {
  if (props.isArchived) return 'archived'
  const map = {
    draft: 'draft',
    upcoming: 'upcoming',
    live: 'live',
    started: 'live',
    ended: 'ended',
    unknown: 'unknown'
  }
  return map[props.status] || 'unknown'
})

const showDot = computed(() => ['live', 'started'].includes(props.status))

const label = computed(() => {
  if (props.isArchived) return 'Lưu trữ'
  const map = {
    draft: 'Nháp',
    upcoming: 'Sắp diễn ra',
    live: 'Đang diễn ra',
    started: 'Đang diễn ra',
    ended: 'Đã kết thúc',
    unknown: 'Không rõ'
  }
  return map[props.status] || 'Không rõ'
})
</script>

<style scoped>
.el-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
  font-weight: 700;
  white-space: nowrap;
  letter-spacing: 0.02em;
}

.el-chip__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.el-chip--draft {
  background: var(--ds-gray-100);
  color: var(--ds-gray-600);
}
.el-chip--draft .el-chip__dot { background: var(--ds-gray-400); }

.el-chip--upcoming {
  background: rgba(2, 132, 199, 0.1);
  color: #0284c7;
  border: 1px solid rgba(2, 132, 199, 0.2);
}
.el-chip--upcoming .el-chip__dot { background: #0284c7; }

.el-chip--live {
  background: rgba(245, 158, 11, 0.1);
  color: #d97706;
  border: 1px solid rgba(245, 158, 11, 0.25);
}
.el-chip--live .el-chip__dot {
  background: #d97706;
  animation: pulse-dot 1.5s ease-in-out infinite;
}

.el-chip--ended {
  background: rgba(79, 70, 229, 0.08);
  color: #4f46e5;
  border: 1px solid rgba(79, 70, 229, 0.15);
}
.el-chip--ended .el-chip__dot { background: #4f46e5; }

.el-chip--archived {
  background: var(--ds-gray-100);
  color: var(--ds-gray-500);
  border: 1px solid var(--ds-border);
}
.el-chip--archived .el-chip__dot { background: var(--ds-gray-400); }

.el-chip--unknown {
  background: var(--ds-gray-100);
  color: var(--ds-gray-500);
}
.el-chip--unknown .el-chip__dot { background: var(--ds-gray-400); }

@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(0.85); }
}
</style>
