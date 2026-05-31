<template>
  <div class="pe">
    <div class="pe__section">

      <div class="pe__section-header">
        <div class="pe__section-icon">
          <LucideIcon name="shield" />
        </div>
        <div>
          <h3 class="pe__section-title">Giám sát</h3>
          <p class="pe__section-desc">Thiết lập các quy tắc giám sát trong quá trình thi</p>
        </div>
      </div>

      <div class="pe__body">

        <!-- Summary -->
        <div class="pe__summary" :class="`pe__summary--${strictnessLevel}`">
          <div class="pe__summary-icon">
            <LucideIcon :name="summaryIcon" />
          </div>
          <div class="pe__summary-body">
            <p class="pe__summary-level">
              Mức độ nghiêm ngặt:
              <strong :class="`pe__summary-level--${strictnessLevel}`">{{ strictnessLabel }}</strong>
            </p>
            <p class="pe__summary-desc">{{ summaryDesc }}</p>
          </div>
          <div class="pe__summary-count">
            <span class="pe__summary-num">{{ activeCount }}</span>
            <span class="pe__summary-denom">/{{ totalRules }}</span>
          </div>
        </div>

        <!-- Presets -->
        <div class="pe__presets">
          <span class="pe__presets-label">Preset nhanh:</span>
          <button
            v-for="preset in presets"
            :key="preset.id"
            type="button"
            class="pe__preset-btn"
            :class="[`pe__preset-btn--${preset.color}`, { 'pe__preset-btn--active': currentPreset === preset.id }]"
            @click="applyPreset(preset.id)"
          >
            <LucideIcon :name="preset.icon" />
            {{ preset.label }}
          </button>
        </div>

        <!-- Master toggle -->
        <div class="pe__master-toggle">
          <div class="pe__master-left">
            <div class="pe__master-icon" :class="{ 'pe__master-icon--on': localEnabled }">
              <LucideIcon :name="localEnabled ? 'shield' : 'shield_off'" />
            </div>
            <div>
              <p class="pe__master-title">{{ localEnabled ? 'Đã bật giám sát' : 'Tắt giám sát' }}</p>
              <p class="pe__master-desc">Bật để theo dõi hành vi của học sinh</p>
            </div>
          </div>
          <button
            type="button"
            class="pe__toggle"
            :class="{ 'pe__toggle--on': localEnabled }"
            @click="localEnabled = !localEnabled"
          >
            <div class="pe__toggle-knob" />
          </button>
        </div>

        <!-- Rules grid -->
        <div class="pe__rules-section">
          <div class="pe__rules-header">
            <span class="pe__rules-title">Quy tắc giám sát</span>
            <button type="button" class="pe__rules-toggle-all" @click="toggleAll">
              {{ allOn ? 'Tắt tất cả' : 'Bật tất cả' }}
            </button>
          </div>

          <div class="pe__rules-grid">
            <div
              v-for="rule in rules"
              :key="rule.key"
              class="pe__rule-card"
              :class="{ 'pe__rule-card--active': localFlags[rule.key] }"
            >
              <div class="pe__rule-icon">
                <LucideIcon :name="rule.icon" />
              </div>
              <div class="pe__rule-body">
                <p class="pe__rule-name">{{ rule.label }}</p>
                <p class="pe__rule-desc">{{ rule.desc }}</p>
              </div>
              <button
                type="button"
                class="pe__rule-toggle"
                :class="{ 'pe__rule-toggle--on': localFlags[rule.key] }"
                @click="toggleRule(rule.key)"
              >
                <div class="pe__rule-knob" />
              </button>
            </div>
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
  enableAiProctoring: { type: Boolean, default: false }
})

const emit = defineEmits([
  'update:enabled',
  'update:monitorTabSwitch',
  'update:monitorBlur',
  'update:monitorExitFullscreen',
  'update:monitorCopyPaste',
  'update:monitorIdleTime',
  'update:monitorDevtools',
  'update:monitorDuplicateIp',
  'update:monitorFastSubmit',
  'update:monitorRightClick',
  'update:monitorPrintScreen',
  'update:monitorRapidQuestionSwitch',
  'update:monitorMultiMonitor',
  'update:requireCameraMic',
  'update:enableAiProctoring'
])

const localFlags = reactive({
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

const localEnabled = computed({
  get: () => props.enabled,
  set: (v) => emit('update:enabled', v)
})

const flagEmits = {
  monitorTabSwitch: (v) => emit('update:monitorTabSwitch', v),
  monitorBlur: (v) => emit('update:monitorBlur', v),
  monitorExitFullscreen: (v) => emit('update:monitorExitFullscreen', v),
  monitorCopyPaste: (v) => emit('update:monitorCopyPaste', v),
  monitorIdleTime: (v) => emit('update:monitorIdleTime', v),
  monitorDevtools: (v) => emit('update:monitorDevtools', v),
  monitorDuplicateIp: (v) => emit('update:monitorDuplicateIp', v),
  monitorFastSubmit: (v) => emit('update:monitorFastSubmit', v),
  monitorRightClick: (v) => emit('update:monitorRightClick', v),
  monitorPrintScreen: (v) => emit('update:monitorPrintScreen', v),
  monitorRapidQuestionSwitch: (v) => emit('update:monitorRapidQuestionSwitch', v),
  monitorMultiMonitor: (v) => emit('update:monitorMultiMonitor', v),
  requireCameraMic: (v) => emit('update:requireCameraMic', v),
  enableAiProctoring: (v) => emit('update:enableAiProctoring', v)
}

const rules = [
  { key: 'monitorTabSwitch', label: 'Theo dõi đổi tab', desc: 'Cảnh báo khi chuyển sang tab khác', icon: 'tab' },
  { key: 'monitorBlur', label: 'Phát hiện mất focus', desc: 'Cảnh báo khi rời khỏi cửa sổ thi', icon: 'visibility_off' },
  { key: 'monitorExitFullscreen', label: 'Thoát toàn màn hình', desc: 'Cảnh báo khi thoát chế độ toàn màn hình', icon: 'fullscreen_exit' },
  { key: 'monitorCopyPaste', label: 'Copy / Paste', desc: 'Chặn sao chép và dán nội dung', icon: 'content_paste' },
  { key: 'monitorIdleTime', label: 'Thời gian nhàn rỗi', desc: 'Cảnh báo khi không tương tác lâu', icon: 'timer_off' },
  { key: 'monitorDevtools', label: 'DevTools', desc: 'Phát hiện mở công cụ phát triển', icon: 'code' },
  { key: 'monitorDuplicateIp', label: "IP trùng lặp", desc: 'Phát hiện nhiều học sinh cùng IP', icon: 'router' },
  { key: 'monitorFastSubmit', label: 'Nộp nhanh', desc: 'Cảnh báo nộp bài quá nhanh', icon: 'bolt' },
  { key: 'monitorRightClick', label: 'Chuột phải', desc: 'Vô hiệu hóa menu chuột phải', icon: 'mouse' },
  { key: 'monitorPrintScreen', label: 'Chụp màn hình', desc: 'Chặn chụp màn hình', icon: 'screenshot' },
  { key: 'monitorRapidQuestionSwitch', label: 'Đổi câu nhanh', desc: 'Phát hiện chuyển câu quá nhanh', icon: 'skip_next' },
  { key: 'monitorMultiMonitor', label: 'Đa màn hình', desc: 'Cảnh báo kết nối màn hình phụ', icon: 'desktop_windows' },
  { key: 'requireCameraMic', label: 'Camera & Micro', desc: 'Yêu cầu bật camera và microphone', icon: 'videocam' },
  { key: 'enableAiProctoring', label: 'AI camera analysis', desc: 'Phân tích camera để ghi tín hiệu và tính điểm review', icon: 'scan-face' }
]

const totalRules = rules.length

const activeCount = computed(() => Object.values(localFlags).filter(Boolean).length)

const strictnessLevel = computed(() => {
  const ratio = activeCount.value / totalRules
  if (ratio >= 0.7) return 'high'
  if (ratio >= 0.4) return 'medium'
  return 'low'
})

const strictnessLabel = computed(() => {
  const map = { high: 'Nghiêm ngặt', medium: 'Trung bình', low: 'Nhẹ' }
  return map[strictnessLevel.value]
})

const summaryIcon = computed(() => {
  const map = { high: 'security', medium: 'verified_user', low: 'shield' }
  return map[strictnessLevel.value]
})

const summaryDesc = computed(() => {
  const map = {
    high: 'Nhiều quy tắc được kích hoạt. Học sinh sẽ bị giám sát chặt chẽ.',
    medium: 'Một số quy tắc được kích hoạt. Cân bằng giữa giám sát và trải nghiệm.',
    low: 'Ít quy tắc được kích hoạt. Giám sát nhẹ nhàng.'
  }
  return map[strictnessLevel.value]
})

const presets = [
  { id: 'light', label: 'Nhẹ', color: 'success', icon: 'shield', flags: { monitorTabSwitch: false, monitorBlur: false, monitorExitFullscreen: false, monitorCopyPaste: false, monitorIdleTime: false, monitorDevtools: false, monitorDuplicateIp: true, monitorFastSubmit: true, monitorRightClick: false, monitorPrintScreen: false, monitorRapidQuestionSwitch: false, monitorMultiMonitor: false, requireCameraMic: false, enableAiProctoring: false } },
  { id: 'medium', label: 'Trung bình', color: 'warning', icon: 'verified_user', flags: { monitorTabSwitch: true, monitorBlur: true, monitorExitFullscreen: true, monitorCopyPaste: false, monitorIdleTime: true, monitorDevtools: true, monitorDuplicateIp: true, monitorFastSubmit: true, monitorRightClick: false, monitorPrintScreen: false, monitorRapidQuestionSwitch: false, monitorMultiMonitor: false, requireCameraMic: false, enableAiProctoring: false } },
  { id: 'strict', label: 'Nghiêm ngặt', color: 'danger', icon: 'security', flags: { monitorTabSwitch: true, monitorBlur: true, monitorExitFullscreen: true, monitorCopyPaste: true, monitorIdleTime: true, monitorDevtools: true, monitorDuplicateIp: true, monitorFastSubmit: true, monitorRightClick: true, monitorPrintScreen: true, monitorRapidQuestionSwitch: true, monitorMultiMonitor: true, requireCameraMic: true, enableAiProctoring: true } }
]

const currentPreset = computed(() => {
  for (const p of presets) {
    const match = p.flags.monitorTabSwitch === localFlags.monitorTabSwitch
      && p.flags.monitorDevtools === localFlags.monitorDevtools
      && p.flags.requireCameraMic === localFlags.requireCameraMic
    if (match) return p.id
  }
  return null
})

const allOn = computed(() => Object.values(localFlags).every(Boolean))

const toggleRule = (key) => {
  localFlags[key] = !localFlags[key]
  flagEmits[key](localFlags[key])
}

const toggleAll = () => {
  const next = !allOn.value
  rules.forEach(r => {
    localFlags[r.key] = next
    flagEmits[r.key](next)
  })
}

const applyPreset = (presetId) => {
  const preset = presets.find(p => p.id === presetId)
  if (!preset) return
  Object.entries(preset.flags).forEach(([key, val]) => {
    localFlags[key] = val
    flagEmits[key](val)
  })
}
</script>


<style scoped>
.pe {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}
.dark .pe { border-color: var(--ds-border-strong); }

.pe__section-header {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  background: linear-gradient(135deg, var(--ds-primary-soft) 0%, transparent 100%);
}
.dark .pe__section-header { border-bottom-color: var(--ds-border-strong); }

.pe__section-icon {
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

.pe__section-title {
  font-family: var(--ds-font-display);
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}
.dark .pe__section-title { color: #f1f5f9; }

.pe__section-desc {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

.pe__body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* Summary */
.pe__summary {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.25rem;
  border-radius: var(--ds-radius-2xl);
  border: 1.5px solid var(--ds-border);
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}
.dark .pe__summary { border-color: var(--ds-border-strong); }
.pe__summary--high { background: var(--ds-danger-soft); border-color: rgba(220, 38, 38, 0.2); }
.pe__summary--medium { background: rgba(234, 179, 8, 0.08); border-color: rgba(234, 179, 8, 0.2); }
.pe__summary--low { background: var(--ds-success-soft); border-color: rgba(22, 163, 74, 0.2); }

.pe__summary-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.pe__summary--high .pe__summary-icon { background: var(--ds-danger); color: white; }
.pe__summary--medium .pe__summary-icon { background: #d97706; color: white; }
.pe__summary--low .pe__summary-icon { background: var(--ds-success); color: white; }

.pe__summary-body {
  flex: 1;
  min-width: 0;
}
.pe__summary-level {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0 0 0.25rem;
}
.dark .pe__summary-level { color: #f1f5f9; }
.pe__summary-level--high { color: var(--ds-danger); }
.pe__summary-level--medium { color: #d97706; }
.pe__summary-level--low { color: var(--ds-success); }

.pe__summary-desc {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0;
}

.pe__summary-count {
  text-align: center;
  flex-shrink: 0;
}
.pe__summary-num {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--ds-text);
  font-family: var(--ds-font-display);
  line-height: 1;
}
.dark .pe__summary-num { color: #f1f5f9; }
.pe__summary-denom {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
}

/* Presets */
.pe__presets {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.pe__presets-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
  margin-right: 0.25rem;
}
.dark .pe__presets-label { color: #94a3b8; }

.pe__preset-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}
.dark .pe__preset-btn { background: var(--ds-gray-800); border-color: var(--ds-border-strong); color: #94a3b8; }
.pe__preset-btn:hover { transform: translateY(-1px); }
.pe__preset-btn--success { border-color: rgba(22, 163, 74, 0.3); color: var(--ds-success); }
.pe__preset-btn--success:hover { background: var(--ds-success-soft); }
.pe__preset-btn--warning { border-color: rgba(234, 179, 8, 0.3); color: #d97706; }
.pe__preset-btn--warning:hover { background: rgba(234, 179, 8, 0.08); }
.pe__preset-btn--danger { border-color: rgba(220, 38, 38, 0.3); color: var(--ds-danger); }
.pe__preset-btn--danger:hover { background: var(--ds-danger-soft); }
.pe__preset-btn--success.pe__preset-btn--active { background: var(--ds-success); color: white; box-shadow: 0 4px 12px rgba(22, 163, 74, 0.25); }
.pe__preset-btn--warning.pe__preset-btn--active { background: #d97706; color: white; box-shadow: 0 4px 12px rgba(234, 179, 8, 0.25); }
.pe__preset-btn--danger.pe__preset-btn--active { background: var(--ds-danger); color: white; box-shadow: 0 4px 12px rgba(220, 38, 38, 0.25); }

/* Master toggle */
.pe__master-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.25rem;
  background: var(--ds-gray-50);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
}
.dark .pe__master-toggle { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.pe__master-left {
  display: flex;
  align-items: center;
  gap: 0.875rem;
}

.pe__master-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-gray-200);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}
.dark .pe__master-icon { background: var(--ds-gray-700); }
.pe__master-icon--on { background: var(--ds-primary-soft); color: var(--ds-primary); }

.pe__master-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}
.dark .pe__master-title { color: #f1f5f9; }
.pe__master-desc {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
}

/* Toggle switch */
.pe__toggle {
  width: 52px;
  height: 28px;
  border-radius: 14px;
  border: none;
  background: var(--ds-gray-300);
  cursor: pointer;
  position: relative;
  transition: background 0.2s ease;
  flex-shrink: 0;
  outline: none;
}
.dark .pe__toggle { background: var(--ds-gray-600); }
.pe__toggle--on { background: var(--ds-primary); }
.pe__toggle-knob {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.15);
  transition: transform 0.2s ease;
}
.pe__toggle--on .pe__toggle-knob { transform: translateX(24px); }

/* Rules grid */
.pe__rules-section {}

.pe__rules-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.875rem;
}
.pe__rules-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
}
.dark .pe__rules-title { color: #94a3b8; }

.pe__rules-toggle-all {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  border: none;
  border-radius: var(--ds-radius-full);
  padding: 0.25rem 0.75rem;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}
.pe__rules-toggle-all:hover { background: var(--ds-primary); color: white; }

.pe__rules-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.625rem;
}
@media (max-width: 600px) {
  .pe__rules-grid { grid-template-columns: 1fr; }
}

.pe__rule-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem;
  background: var(--ds-gray-50);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}
.dark .pe__rule-card { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }
.pe__rule-card--active {
  border-color: var(--ds-primary-border);
  background: var(--ds-primary-soft);
}
.dark .pe__rule-card--active { background: rgba(79, 70, 229, 0.1); }

.pe__rule-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--ds-radius-md);
  background: var(--ds-gray-200);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}
.dark .pe__rule-icon { background: var(--ds-gray-700); }
.pe__rule-card--active .pe__rule-icon { background: var(--ds-primary); color: white; }

.pe__rule-body {
  flex: 1;
  min-width: 0;
}
.pe__rule-name {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}
.dark .pe__rule-name { color: #f1f5f9; }
.pe__rule-desc {
  font-size: 0.65rem;
  color: var(--ds-text-muted);
  margin: 0.125rem 0 0;
}

.pe__rule-toggle {
  width: 40px;
  height: 22px;
  border-radius: 11px;
  border: none;
  background: var(--ds-gray-300);
  cursor: pointer;
  position: relative;
  transition: background 0.2s ease;
  flex-shrink: 0;
  outline: none;
}
.dark .pe__rule-toggle { background: var(--ds-gray-600); }
.pe__rule-toggle--on { background: var(--ds-primary); }
.pe__rule-knob {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
  transition: transform 0.2s ease;
}
.pe__rule-toggle--on .pe__rule-knob { transform: translateX(18px); }
</style>
