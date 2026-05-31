<template>
  <div class="ec-section">
    <div class="ec-section__header">
      <div class="ec-section__icon">
        <LucideIcon name="shield" />
      </div>
      <div>
        <h3 class="ec-section__title">Cài đặt giám sát</h3>
      </div>
    </div>

    <div class="ec-section__body">

      <!-- Master toggle -->
      <div class="ec-proctor-master">
        <div class="ec-proctor-master__body">
          <div class="ec-proctor-master__icon" :class="localEnabled && 'ec-proctor-master__icon--on'">
            <LucideIcon name="verified_user" />
          </div>
          <div>
            <p class="ec-proctor-master__title">Chế độ giám sát</p>
            <p class="ec-proctor-master__desc">
              {{ localEnabled ? 'Bật giám sát để phát hiện hành vi bất thường' : 'Tắt giám sát nếu không cần chống gian lận' }}
            </p>
          </div>
        </div>
        <button
          type="button"
          class="ec-toggle"
          :class="localEnabled && 'ec-toggle--on'"
          @click="localEnabled = !localEnabled"
        >
          <span class="ec-toggle__knob" />
        </button>
      </div>

      <!-- Proctoring rules -->
      <div v-if="localEnabled" class="ec-proctor-rules">
        <div class="ec-proctor-rules__header">
          <h4 class="ec-proctor-rules__title">Quy tắc giám sát</h4>
          <button type="button" class="ec-proctor-rules__enable-all" @click="enableAll">
            Bật tất cả
          </button>
        </div>

        <div class="ec-proctor-rules__grid">
          <div
            v-for="rule in rules"
            :key="rule.key"
            class="ec-rule"
            :class="{ 'ec-rule--on': localRules[rule.key] }"
          >
            <div class="ec-rule__body">
              <span class="ec-rule__icon" :class="`ec-rule__icon--${rule.color}`">
                <LucideIcon :name="rule.icon" />
              </span>
              <div>
                <p class="ec-rule__title">{{ rule.title }}</p>
                <p class="ec-rule__desc">{{ rule.desc }}</p>
              </div>
            </div>
            <button
              type="button"
              class="ec-toggle ec-toggle--sm"
              :class="localRules[rule.key] && 'ec-toggle--on'"
              @click="toggleRule(rule.key)"
            >
              <span class="ec-toggle__knob" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive } from 'vue'

const props = defineProps({
  enabled: { type: Boolean, default: true },
  monitorTabSwitch: { type: Boolean, default: true },
  monitorBlur: { type: Boolean, default: true },
  monitorExitFullscreen: { type: Boolean, default: true },
  monitorCopyPaste: { type: Boolean, default: true },
  monitorIdleTime: { type: Boolean, default: true },
  monitorDevtools: { type: Boolean, default: true },
  monitorDuplicateIp: { type: Boolean, default: true },
  monitorFastSubmit: { type: Boolean, default: true },
  monitorRightClick: { type: Boolean, default: true },
  monitorPrintScreen: { type: Boolean, default: true },
  monitorRapidQuestionSwitch: { type: Boolean, default: true },
  monitorMultiMonitor: { type: Boolean, default: true },
  requireCameraMic: { type: Boolean, default: true },
  enableAiProctoring: { type: Boolean, default: true }
})

const emit = defineEmits([
  'update:enabled',
  'update:monitorTabSwitch', 'update:monitorBlur', 'update:monitorExitFullscreen',
  'update:monitorCopyPaste', 'update:monitorIdleTime', 'update:monitorDevtools',
  'update:monitorDuplicateIp', 'update:monitorFastSubmit',
  'update:monitorRightClick', 'update:monitorPrintScreen',
  'update:monitorRapidQuestionSwitch', 'update:monitorMultiMonitor',
  'update:requireCameraMic', 'update:enableAiProctoring'
])

const localEnabled = computed({
  get: () => props.enabled,
  set: (v) => emit('update:enabled', v)
})

const localRules = reactive({
  monitorTabSwitch: props.monitorTabSwitch,
  monitorBlur: props.monitorBlur,
  monitorExitFullscreen: props.monitorExitFullscreen,
  monitorCopyPaste: props.monitorCopyPaste,
  monitorIdleTime: props.monitorIdleTime,
  monitorDevtools: props.monitorDevtools,
  monitorDuplicateIp: props.monitorDuplicateIp,
  monitorFastSubmit: props.monitorFastSubmit,
  monitorRightClick: props.monitorRightClick,
  monitorPrintScreen: props.monitorPrintScreen,
  monitorRapidQuestionSwitch: props.monitorRapidQuestionSwitch,
  monitorMultiMonitor: props.monitorMultiMonitor,
  requireCameraMic: props.requireCameraMic,
  enableAiProctoring: props.enableAiProctoring
})

const rules = [
  { key: 'monitorTabSwitch', icon: 'tab', color: 'primary', title: 'Theo dõi chuyển tab', desc: 'Phát hiện đổi tab hoặc ẩn cửa sổ trình duyệt' },
  { key: 'monitorBlur', icon: 'visibility_off', color: 'info', title: 'Theo dõi blur', desc: 'Ghi nhận khi cửa sổ mất focus' },
  { key: 'monitorExitFullscreen', icon: 'fullscreen_exit', color: 'warning', title: 'Thoát toàn màn hình', desc: 'Cảnh báo khi rời chế độ toàn màn hình' },
  { key: 'monitorCopyPaste', icon: 'content_copy', color: 'muted', title: 'Copy/Paste', desc: 'Ghi nhận thao tác sao chép và dán nội dung' },
  { key: 'monitorIdleTime', icon: 'timer_off', color: 'warning', title: 'Nhàn rỗi', desc: 'Cảnh báo khi học sinh không thao tác trong thời gian dài' },
  { key: 'monitorDevtools', icon: 'code', color: 'danger', title: 'DevTools', desc: 'Phát hiện mở công cụ phát triển trình duyệt' },
  { key: 'monitorDuplicateIp', icon: 'router', color: 'danger', title: "IP trùng lặp", desc: 'Phát hiện nhiều tài khoản từ cùng một địa chỉ IP' },
  { key: 'monitorFastSubmit', icon: 'bolt', color: 'warning', title: 'Nộp nhanh', desc: 'Cảnh báo nộp bài quá nhanh bất thường' },
  { key: 'monitorRightClick', icon: 'mouse', color: 'muted', title: 'Chuột phải', desc: 'Chặn menu chuột phải để tránh sao chép' },
  { key: 'monitorPrintScreen', icon: 'screenshot', color: 'muted', title: 'Chụp màn hình', desc: 'Phát hiện hành vi chụp màn hình' },
  { key: 'monitorRapidQuestionSwitch', icon: 'skip_next', color: 'info', title: 'Đổi câu nhanh', desc: 'Cảnh báo đổi câu hỏi liên tục với tốc độ bất thường' },
  { key: 'monitorMultiMonitor', icon: 'desktop_windows', color: 'danger', title: 'Đa màn hình', desc: 'Phát hiện nhiều màn hình hoặc chia đôi màn hình' },
  { key: 'requireCameraMic', icon: 'videocam', color: 'primary', title: 'Yêu cầu camera/mic', desc: 'Bắt buộc bật camera và microphone trong suốt kỳ thi' },
  { key: 'enableAiProctoring', icon: 'scan-face', color: 'danger', title: 'AI camera analysis', desc: 'Phân tích camera để ghi tín hiệu và tính điểm review' }
]

const toggleRule = (key) => {
  localRules[key] = !localRules[key]
  emit(`update:${key}`, localRules[key])
}

const enableAll = () => {
  for (const rule of rules) {
    localRules[rule.key] = true
    emit(`update:${rule.key}`, true)
  }
}
</script>


<style scoped>
.ec-section {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.ec-section__header {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  background: linear-gradient(135deg, var(--ds-primary-soft) 0%, transparent 100%);
}

.ec-section__icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}


.ec-section__title {
  font-family: var(--ds-font-display);
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .ec-section__title { color: #f1f5f9; }

.ec-section__desc {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.ec-section__body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* Master toggle */
.ec-proctor-master {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.25rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
}

.dark .ec-proctor-master {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ec-proctor-master__body {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  flex: 1;
}

.ec-proctor-master__icon {
  width: 48px;
  height: 48px;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-gray-200);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.ec-proctor-master__icon--on {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}


.ec-proctor-master__title {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .ec-proctor-master__title { color: #f1f5f9; }

.ec-proctor-master__desc {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  line-height: 1.4;
}

/* Toggle */
.ec-toggle {
  position: relative;
  width: 52px;
  height: 28px;
  border-radius: 14px;
  border: none;
  background: var(--ds-gray-300);
  cursor: pointer;
  transition: background 0.2s ease;
  flex-shrink: 0;
}

.dark .ec-toggle { background: var(--ds-gray-600); }

.ec-toggle--on { background: var(--ds-primary); }

.ec-toggle--sm {
  width: 44px;
  height: 24px;
  border-radius: 12px;
}

.ec-toggle__knob {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 1px 4px rgba(0,0,0,0.2);
  transition: transform 0.2s ease;
}

.ec-toggle--sm .ec-toggle__knob {
  width: 18px;
  height: 18px;
}

.ec-toggle--on .ec-toggle__knob {
  transform: translateX(24px);
}

.ec-toggle--sm.ec-toggle--on .ec-toggle__knob {
  transform: translateX(20px);
}

/* Rules */
.ec-proctor-rules__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.ec-proctor-rules__title {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin: 0;
}

.ec-proctor-rules__enable-all {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-primary);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: var(--ds-radius-md);
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.ec-proctor-rules__enable-all:hover {
  background: var(--ds-primary-soft);
}

.ec-proctor-rules__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 0.5rem;
}

/* Rule card */
.ec-rule {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.dark .ec-rule {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ec-rule--on {
  background: var(--ds-primary-soft);
  border-color: var(--ds-primary-border);
}

.ec-rule__body {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  flex: 1;
  min-width: 0;
}

.ec-rule__icon {
  width: 32px;
  height: 32px;
  border-radius: var(--ds-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.ec-rule__icon--primary { background: var(--ds-primary-soft); color: var(--ds-primary); }
.ec-rule__icon--success { background: var(--ds-success-soft); color: var(--ds-success); }
.ec-rule__icon--info { background: var(--ds-info-soft); color: var(--ds-info); }
.ec-rule__icon--warning { background: var(--ds-warning-soft); color: var(--ds-warning); }
.ec-rule__icon--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }
.ec-rule__icon--muted { background: var(--ds-gray-200); color: var(--ds-text-muted); }
.dark .ec-rule__icon--muted { background: var(--ds-gray-700); color: #94a3b8; }

.ec-rule__title {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .ec-rule__title { color: #f1f5f9; }

.ec-rule__desc {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  line-height: 1.4;
}
</style>
