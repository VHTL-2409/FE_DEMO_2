<template>
  <div class="est">
    <div
      v-for="(milestone, idx) in milestones"
      :key="milestone.id"
      class="est__item"
      :class="{
        'est__item--done': milestone.done,
        'est__item--current': milestone.current,
        'est__item--future': !milestone.done && !milestone.current,
        'est__item--clickable': milestone.action && !milestone.done
      }"
      @click="milestone.action && !milestone.done && milestone.action()"
    >
      <!-- Connector line -->
      <div v-if="idx > 0" class="est__connector" :class="{ 'est__connector--done': milestone.done }" />

      <!-- Dot -->
      <div class="est__dot">
        <LucideIcon name="check" v-if="milestone.done" class="est__dot-icon" />
        <LucideIcon name="circle-dot" v-else-if="milestone.current" class="est__dot-icon" />
        <LucideIcon v-else :name="milestone.icon" class="est__dot-icon" />
      </div>

      <!-- Content -->
      <div class="est__content">
        <div class="est__label">{{ milestone.label }}</div>
        <div v-if="milestone.time" class="est__time">{{ milestone.time }}</div>
        <div v-else-if="milestone.hint" class="est__hint">{{ milestone.hint }}</div>
      </div>

      <!-- Action chip -->
      <div v-if="milestone.action && !milestone.done && !milestone.current" class="est__action">
        {{ milestone.actionLabel }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  exam: { type: Object, default: null }
})

const emit = defineEmits(['tab-click'])

const fmt = (d) => {
  if (!d) return null
  try {
    return new Date(d).toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    })
  } catch { return null }
}

const now = () => new Date()

const isPast = (d) => d && now() > new Date(d)
const isFuture = (d) => d && now() < new Date(d)
const isBetween = (start, end) => {
  if (!start || !end) return false
  const n = now()
  return n >= new Date(start) && n <= new Date(end)
}

const milestones = computed(() => {
  if (!props.exam) return []

  const e = props.exam
  const statuses = []

  // Draft / Created
  const createdAt = e.createdAt || e.updatedAt
  const isArchived = e.isArchived
  const isActive = e.isActive

  const draftDone = true // always done once exam exists
  const draftCurrent = !isActive && !isArchived

  // Published
  const pubDone = isActive || isArchived
  const pubCurrent = isActive && !isPast(e.endTime) && !isBetween(e.startTime, e.endTime) && isPast(e.startTime)
  // If exam is live or ended, publish milestone is done
  const publishDone = pubDone || isPast(e.endTime) || (isActive && isBetween(e.startTime, e.endTime))

  // Started
  const startDone = isPast(e.endTime) || isBetween(e.startTime, e.endTime)
  const startCurrent = isActive && isFuture(e.startTime) && !startDone
  const startFuture = !isActive || isFuture(e.startTime)

  // Ended
  const endDone = isPast(e.endTime)
  const endCurrent = isActive && !isPast(e.endTime) && !isFuture(e.startTime)
  const endFuture = !endDone && !endCurrent

  // Archived
  const archDone = isArchived

  // Simple state machine based on exam flags
  let state = 'draft'
  if (isArchived) state = 'archived'
  else if (isActive && isPast(e.endTime)) state = 'ended'
  else if (isActive && isBetween(e.startTime, e.endTime)) state = 'live'
  else if (isActive && isPast(e.startTime) && !isPast(e.endTime)) state = 'upcoming'
  else if (isActive) state = 'published'
  else state = 'draft'

  return [
    {
      id: 'draft',
      label: 'Nháp',
      icon: 'edit_note',
      done: true,
      current: state === 'draft',
      time: fmt(createdAt)
    },
    {
      id: 'published',
      label: 'Xuất bản',
      icon: 'rocket_launch',
      done: ['published', 'live', 'upcoming', 'ended', 'archived'].includes(state),
      current: state === 'published',
      hint: state === 'draft' ? 'Chưa xuất bản' : null
    },
    {
      id: 'started',
      label: e.startTime ? `Mở lúc ${fmt(e.startTime)}` : 'Chưa đặt giờ',
      icon: 'play_circle',
      done: ['live', 'upcoming', 'ended', 'archived'].includes(state) && !!e.startTime,
      current: state === 'upcoming',
      hint: !e.startTime ? 'Mở 24/7' : null
    },
    {
      id: 'ended',
      label: 'Kết thúc',
      icon: 'stop_circle',
      done: ['ended', 'archived'].includes(state),
      current: state === 'live' || state === 'upcoming',
      hint: state === 'live' ? 'Đang diễn ra' : state === 'upcoming' ? 'Sắp diễn ra' : null
    },
    {
      id: 'archived',
      label: 'Lưu trữ',
      icon: 'archive',
      done: isArchived,
      current: false,
      hint: !isArchived ? 'Không bắt buộc' : null
    }
  ]
})
</script>


<style scoped>
.est {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 0.25rem 0;
}

.est__item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  position: relative;
  padding: 0.375rem 0;
  min-height: 2.5rem;
}

.est__item--clickable {
  cursor: pointer;
}

.est__item--clickable:hover .est__dot {
  transform: scale(1.15);
  box-shadow: 0 0 0 3px var(--ds-primary-soft);
}

/* Connector */
.est__connector {
  position: absolute;
  left: calc(12px - 1px);
  top: -0.25rem;
  bottom: 50%;
  width: 2px;
  background: var(--ds-border);
  border-radius: 1px;
}

.est__connector--done {
  background: var(--ds-primary);
  opacity: 0.4;
}

/* Dot */
.est__dot {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
  background: var(--ds-gray-100);
  border: 2px solid var(--ds-border);
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.dark .est__dot {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
}

.est__item--done .est__dot {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: white;
}

.est__item--current .est__dot {
  background: var(--ds-surface);
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-soft);
}

.dark .est__item--current .est__dot {
  background: var(--ds-gray-800);
}

.est__item--future .est__dot {
  background: var(--ds-gray-100);
  border-color: var(--ds-border);
  color: var(--ds-text-muted);
}

.dark .est__item--future .est__dot {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: var(--ds-gray-500);
}

.est__dot-icon {
  font-size: 0.75rem;
}

/* Content */
.est__content {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  flex: 1;
  min-width: 0;
  padding-top: 0.125rem;
}

.est__label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
  line-height: 1.3;
}

.est__item--done .est__label { color: var(--ds-primary); }
.est__item--current .est__label { color: var(--ds-text); font-weight: 800; }
.dark .est__item--current .est__label { color: #f1f5f9; }

.est__time {
  font-size: 0.65rem;
  color: var(--ds-text-muted);
  font-weight: 500;
  line-height: 1.2;
}

.est__hint {
  font-size: 0.65rem;
  color: var(--ds-text-muted);
  font-style: italic;
}

/* Action chip */
.est__action {
  font-size: 0.6rem;
  font-weight: 700;
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  padding: 0.125rem 0.5rem;
  border-radius: var(--ds-radius-full);
  white-space: nowrap;
  flex-shrink: 0;
  align-self: center;
}
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}