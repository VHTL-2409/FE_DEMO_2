<template>
  <div class="erc">
    <!-- Header -->
    <div class="erc__header">
      <div class="erc__header-icon">
        <LucideIcon name="menu_book" />
      </div>
      <div>
        <h2 class="erc__header-title">Hướng dẫn & Quy định</h2>
        <p class="erc__header-sub">Vui lòng đọc kỹ trước khi vào thi</p>
      </div>
    </div>

    <!-- Info rows -->
    <div class="erc__info-grid">
      <div class="erc__info-item">
        <div class="erc__info-icon erc__info-icon--time">
          <LucideIcon name="timer" />
        </div>
        <div>
          <span class="erc__info-val">{{ duration }} phút</span>
          <span class="erc__info-lbl">Thời gian làm bài</span>
        </div>
      </div>
      <div class="erc__info-item">
        <div class="erc__info-icon erc__info-icon--question">
          <LucideIcon name="help" />
        </div>
        <div>
          <span class="erc__info-val">{{ questionCount }} câu</span>
          <span class="erc__info-lbl">Tổng số câu hỏi</span>
        </div>
      </div>
      <div class="erc__info-item">
        <div class="erc__info-icon erc__info-icon--submit">
          <LucideIcon name="save" />
        </div>
        <div>
          <span class="erc__info-val">Auto</span>
          <span class="erc__info-lbl">Lưu & Nộp bài</span>
        </div>
      </div>
    </div>

    <!-- Rules -->
    <div class="erc__rules">
      <div class="erc__section-label">Quy định thi</div>
      <div class="erc__rules-list">
        <div v-for="rule in displayRules" :key="rule.id" class="erc__rule">
          <LucideIcon :name="rule.icon" />
          <div class="erc__rule-content">
            <span class="erc__rule-title">{{ rule.title }}</span>
            <span v-if="rule.desc" class="erc__rule-desc">{{ rule.desc }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Proctoring warning if needed -->
    <div v-if="requireCameraMic" class="erc__proctoring">
      <div class="erc__proctoring-header">
        <LucideIcon name="videocam" />
        <span>Giám sát khi thi</span>
      </div>
      <div class="erc__proctoring-rules">
        <p>- Camera sẽ được kích hoạt trong suốt buổi thi</p>
        <p>- Không được tắt camera hoặc chuyển tab</p>
        <p>- Hệ thống sẽ tự động phát hiện hành vi bất thường</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  duration: { type: [Number, String], default: 60 },
  questionCount: { type: [Number, String], default: 0 },
  requireCameraMic: { type: Boolean, default: false }
})

const defaultRules = [
  { id: 1, icon: 'schedule', title: 'Làm bài trong thời gian quy định', desc: 'Hệ thống tự động nộp bài khi hết giờ' },
  { id: 2, icon: 'save', title: 'Trả lời được lưu tự động', desc: 'Bạn có thể làm lỗi or làm lại câu trả lời' },
  { id: 3, icon: 'smartphone', title: 'Không được chuyển tab/ứng dụng', desc: 'Nếu chuyển tab, hệ thống sẽ ghi nhận hành vi bất thường' },
  { id: 4, icon: 'wifi', title: 'Duy trì kết nối mạng ổn định', desc: 'Mất mạng sẽ ảnh hưởng đến việc nộp bài' }
]

const displayRules = computed(() => {
  if (props.requireCameraMic) return defaultRules
  return defaultRules.filter(r => r.id !== 3)
})
</script>


<style scoped>
.erc {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.dark .erc { border-color: var(--ds-border-strong); }

/* Header */
.erc__header {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1.125rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .erc__header { background: var(--ds-gray-800); border-bottom-color: var(--ds-border-strong); }

.erc__header-icon {
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


.erc__header-title {
  font-family: var(--ds-font-display);
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .erc__header-title { color: #f1f5f9; }

.erc__header-sub {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
  font-weight: 500;
}

/* Info grid */
.erc__info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0;
  border-bottom: 1px solid var(--ds-border);
}

.dark .erc__info-grid { border-bottom-color: var(--ds-border-strong); }

.erc__info-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.375rem;
  padding: 1rem 0.75rem;
  border-right: 1px solid var(--ds-border);
}

.dark .erc__info-item { border-right-color: var(--ds-border-strong); }
.erc__info-item:last-child { border-right: none; }

.erc__info-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
}


.erc__info-icon--time { background: var(--ds-primary-soft); color: var(--ds-primary); }
.erc__info-icon--question { background: var(--ds-info-soft); color: var(--ds-info); }
.erc__info-icon--submit { background: var(--ds-success-soft); color: var(--ds-success); }

.erc__info-item > div {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.125rem;
  text-align: center;
}

.erc__info-val {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 900;
  color: var(--ds-text);
  line-height: 1;
}

.dark .erc__info-val { color: #f1f5f9; }

.erc__info-lbl {
  font-size: 0.6rem;
  font-weight: 700;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* Rules */
.erc__rules {
  padding: 1rem 1.25rem;
}

.erc__section-label {
  font-size: 0.65rem;
  font-weight: 800;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 0.75rem;
}

.erc__rules-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.erc__rule {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.5rem;
  border-radius: var(--ds-radius-lg);
  transition: all 0.2s ease;
}

.erc__rule:hover {
  background: var(--ds-gray-50);
  transform: translateX(4px);
}

.dark .erc__rule:hover {
  background: var(--ds-gray-800);
}

.erc__rule-icon {
  font-size: 1.125rem;
  color: var(--ds-primary);
  flex-shrink: 0;
  margin-top: 0.1rem;
  transition: transform 0.2s ease;
}

.erc__rule:hover .erc__rule-icon {
  transform: scale(1.1);
}

.erc__rule-content {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.erc__rule-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .erc__rule-title { color: #f1f5f9; }

.erc__rule-desc {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  font-weight: 500;
  line-height: 1.4;
}

/* Proctoring */
.erc__proctoring {
  margin: 0 1.25rem 1.25rem;
  padding: 1rem;
  border-radius: var(--ds-radius-xl);
  background: rgba(14, 165, 233, 0.06);
  border: 1px solid rgba(14, 165, 233, 0.2);
}

.erc__proctoring-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--ds-info);
  margin-bottom: 0.625rem;
}


.erc__proctoring-rules {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.erc__proctoring-rules p {
  font-size: 0.75rem;
  color: var(--ds-info);
  font-weight: 500;
  margin: 0;
  line-height: 1.5;
  opacity: 0.85;
}

/* Responsive */
@media (max-width: 480px) {
  .erc__info-grid { grid-template-columns: 1fr; }
  .erc__info-item { border-right: none; border-bottom: 1px solid var(--ds-border); }
  .erc__info-item:last-child { border-bottom: none; }
}
</style>
