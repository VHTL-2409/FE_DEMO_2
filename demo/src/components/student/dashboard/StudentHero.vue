<template>
  <div class="sh">
    <!-- Left: greeting + meta -->
    <div class="sh__left">
      <div class="sh__greeting-row">
        <div class="sh__wave-icon">
          <LucideIcon name="waving_hand" />
        </div>
        <div class="sh__greeting-text">
          <p class="sh__time-label">{{ timeOfDayLabel }}, {{ studentName }}</p>
          <p class="sh__date-label">{{ todayLabel }}</p>
        </div>
      </div>

      <p class="sh__headline">{{ headlineText }}</p>

      <!-- Quick todo hint -->
      <div v-if="nextAction" class="sh__next-action" @click="$emit('action-click', nextAction.action)">
        <LucideIcon :name="nextAction.icon" />
        <span>{{ nextAction.label }}</span>
        <LucideIcon name="chevron_right" sh__next-arrow />
      </div>
    </div>

    <!-- Right: stats summary -->
    <div class="sh__right">
      <div class="sh__stat-item" v-if="examCount > 0">
        <LucideIcon name="quiz" />
        <div>
          <span class="sh__stat-val">{{ examCount }}</span>
          <span class="sh__stat-lbl">Kỳ thi sắp tới</span>
        </div>
      </div>

      <div class="sh__stat-item" v-if="pendingCount > 0">
        <LucideIcon name="pending" />
        <div>
          <span class="sh__stat-val">{{ pendingCount }}</span>
          <span class="sh__stat-lbl">Chưa hoàn thành</span>
        </div>
      </div>

      <div class="sh__stat-item" v-if="newScoreCount > 0">
        <LucideIcon name="star" />
        <div>
          <span class="sh__stat-val">{{ newScoreCount }}</span>
          <span class="sh__stat-lbl">Điểm mới</span>
        </div>
      </div>

      <div class="sh__stat-item" v-if="totalAttempts > 0">
        <LucideIcon name="history" />
        <div>
          <span class="sh__stat-val">{{ totalAttempts }}</span>
          <span class="sh__stat-lbl">Bài thi đã làm</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  studentName: { type: String, default: 'Học sinh' },
  examCount: { type: Number, default: 0 },
  pendingCount: { type: Number, default: 0 },
  newScoreCount: { type: Number, default: 0 },
  totalAttempts: { type: Number, default: 0 },
  avgScore: { type: [Number, String], default: null }
})

defineEmits(['action-click'])

const timeOfDayLabel = computed(() => {
  const h = new Date().getHours()
  if (h < 12) return 'Buổi sáng'
  if (h < 18) return 'Buổi chiều'
  return 'Buổi tối'
})

const todayLabel = computed(() =>
  new Date().toLocaleDateString('vi-VN', { weekday: 'long', day: 'numeric', month: 'long' })
)

const headlineText = computed(() => {
  if (props.examCount > 0) {
    return `Bạn có ${props.examCount} kỳ thi sắp tới. Chuẩn bị thật kỹ nhé!`
  }
  if (props.pendingCount > 0) {
    return `Bạn có ${props.pendingCount} bài thi chưa hoàn thành.`
  }
  if (props.newScoreCount > 0) {
    return `Có ${props.newScoreCount} kết quả thi mới đang chờ bạn xem.`
  }
  return 'Không có kỳ thi nào sắp tới. Hãy luyện tập thêm!'
})

const nextAction = computed(() => {
  if (props.examCount > 0) {
    return { action: 'exam', label: 'Xem lịch thi', icon: 'calendar_month' }
  }
  if (props.pendingCount > 0) {
    return { action: 'pending', label: 'Hoàn thành bài thi', icon: 'quiz' }
  }
  if (props.newScoreCount > 0) {
    return { action: 'results', label: 'Xem kết quả mới', icon: 'grade' }
  }
  return { action: 'practice', label: 'Luyện tập ngay', icon: 'model_training' }
})
</script>


<style scoped>
.sh {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  padding: 1.5rem 1.75rem;
  background: linear-gradient(135deg, #ffffff 0%, #f0f4ff 60%, #ede9fe 100%);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  box-shadow: var(--ds-shadow-sm);
  position: relative;
  overflow: hidden;
}

.dark .sh {
  background: linear-gradient(135deg, #1e293b 0%, #1e1b4b 60%, #1e1b4b 100%);
  border-color: rgba(79, 70, 229, 0.15);
}

.sh::before {
  content: '';
  position: absolute;
  top: -30px;
  right: -30px;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(79, 70, 229, 0.06) 0%, transparent 70%);
  pointer-events: none;
}

/* Left */
.sh__left {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
  flex: 1;
  min-width: 0;
}

.sh__greeting-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.sh__wave-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-xl);
  background: rgba(79, 70, 229, 0.1);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  animation: wave 3s ease-in-out infinite;
}


@keyframes wave {
  0%, 100% { transform: rotate(-5deg); }
  50% { transform: rotate(5deg); }
}

.sh__greeting-text {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.sh__time-label {
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin: 0;
}

.sh__date-label {
  font-size: 0.75rem;
  color: var(--ds-text-secondary);
  font-weight: 500;
  margin: 0;
}

.sh__headline {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.4;
  max-width: 420px;
}

.dark .sh__headline {
  color: #f1f5f9;
}

/* Next action chip */
.sh__next-action {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-full);
  background: var(--ds-primary);
  color: white;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.25);
  width: fit-content;
  margin-top: 0.25rem;
}

.sh__next-action:hover {
  background: var(--ds-primary-hover, #4338ca);
  transform: translateX(2px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}


.sh__next-arrow {
  margin-left: 0.25rem;
  font-size: 1.125rem;
}

/* Right */
.sh__right {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex-shrink: 0;
  min-width: 160px;
}

.sh__stat-item {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-xl);
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid var(--ds-border);
  backdrop-filter: blur(4px);
  box-shadow: var(--ds-shadow-xs);
}

.dark .sh__stat-item {
  background: rgba(30, 41, 59, 0.8);
  border-color: var(--ds-border-strong);
}

.sh__stat-item > div {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.sh__stat-icon {
  font-size: 1.25rem;
  flex-shrink: 0;
}

.sh__stat-icon--exam { color: var(--ds-primary); }
.sh__stat-icon--pending { color: var(--ds-warning); }
.sh__stat-icon--score { color: var(--ds-success); }
.sh__stat-icon--history { color: var(--ds-info); }

.sh__stat-val {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--ds-text);
  line-height: 1.1;
}

.dark .sh__stat-val {
  color: #f1f5f9;
}

.sh__stat-lbl {
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  white-space: nowrap;
}

/* Responsive */
@media (max-width: 768px) {
  .sh {
    flex-direction: column;
    align-items: flex-start;
    padding: 1.25rem;
    gap: 1rem;
  }

  .sh__right {
    flex-direction: row;
    flex-wrap: wrap;
    min-width: unset;
    width: 100%;
  }

  .sh__stat-item {
    flex: 1;
    min-width: 120px;
  }
}

@media (max-width: 480px) {
  .sh__right {
    flex-direction: column;
  }
}
</style>
