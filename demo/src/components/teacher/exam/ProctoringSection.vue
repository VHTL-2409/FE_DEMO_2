<template>
  <div class="ec-section">
    <div class="ec-section__header">
      <div class="ec-section__icon">
        <LucideIcon name="shield" />
      </div>
      <div>
        <h3 class="ec-section__title">Cài đặt giám sát</h3>
        <p class="ec-section__desc">Chọn các lớp bảo vệ cần áp dụng cho đề thi này.</p>
      </div>
    </div>

    <div class="ec-section__body">
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
          :aria-pressed="localEnabled"
          :aria-label="localEnabled ? 'Tắt chế độ giám sát' : 'Bật chế độ giám sát'"
          @click="localEnabled = !localEnabled"
        >
          <span class="ec-toggle__label">{{ localEnabled ? 'Bật' : 'Tắt' }}</span>
          <span class="ec-toggle__knob">
            <LucideIcon v-if="localEnabled" name="check" size="12" />
          </span>
        </button>
      </div>

      <div v-if="localEnabled" class="ec-proctor-rules">
        <div class="ec-group-head">
          <div>
            <h4 class="ec-group-head__title">Quy tắc giám sát</h4>
            <p class="ec-group-head__desc">Các tín hiệu được ghi nhận trong quá trình thí sinh làm bài.</p>
          </div>
          <button type="button" class="ec-button ec-button--secondary" @click="enableAll">
            <LucideIcon name="check-check" size="15" />
            <span>Bật toàn bộ</span>
          </button>
        </div>

        <div class="ec-proctor-rules__grid">
          <div
            v-for="rule in rules"
            :key="rule.id"
            class="ec-rule"
            :class="{ 'ec-rule--on': isRuleOn(rule), 'ec-rule--disabled': isRuleDisabled(rule) }"
            role="button"
            :tabindex="isRuleDisabled(rule) ? -1 : 0"
            :aria-disabled="isRuleDisabled(rule)"
            :aria-pressed="isRuleOn(rule)"
            @click="toggleRule(rule)"
            @keydown.enter.prevent="toggleRule(rule)"
            @keydown.space.prevent="toggleRule(rule)"
          >
            <div class="ec-rule__body">
              <span class="ec-rule__icon" :class="`ec-rule__icon--${rule.color}`">
                <LucideIcon :name="rule.icon" />
              </span>
              <div class="ec-rule__content">
                <p class="ec-rule__title">{{ rule.title }}</p>
                <p class="ec-rule__desc">{{ rule.desc }}</p>
              </div>
            </div>
            <div class="ec-rule__state">
              <span class="ec-rule__state-label">{{ isRuleOn(rule) ? 'Đang bật' : 'Đang tắt' }}</span>
              <button
                type="button"
                class="ec-mini-switch"
                :class="isRuleOn(rule) && 'ec-mini-switch--on'"
                :disabled="isRuleDisabled(rule)"
                :aria-pressed="isRuleOn(rule)"
                :aria-label="`${isRuleOn(rule) ? 'Tắt' : 'Bật'} ${rule.title}`"
                @click.stop="toggleRule(rule)"
              >
                <span class="ec-mini-switch__knob">
                  <LucideIcon v-if="isRuleOn(rule)" name="check" size="10" stroke-width="3" />
                </span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="ec-entry-gate">
        <div class="ec-group-head">
          <div>
            <h4 class="ec-group-head__title">Cổng vào thi</h4>
            <p class="ec-group-head__desc">Điều kiện thí sinh cần hoàn tất trước khi bắt đầu lượt thi.</p>
          </div>
        </div>

        <div class="ec-entry-gate__grid">
          <label class="ec-entry-field ec-entry-field--wide">
            <span class="ec-entry-field__label">Quy chế hiển thị cho thí sinh</span>
            <textarea
              class="ec-entry-field__control ec-entry-field__control--textarea"
              :value="rulesText"
              rows="5"
              placeholder="Để trống để dùng quy chế mặc định của hệ thống"
              @input="emit('update:rulesText', $event.target.value)"
            />
          </label>

          <label class="ec-entry-field">
            <span class="ec-entry-field__label">Phiên bản quy chế</span>
            <input
              class="ec-entry-field__control"
              :value="rulesVersion"
              maxlength="64"
              placeholder="default-v1"
              @input="emit('update:rulesVersion', $event.target.value)"
            />
          </label>

          <label class="ec-entry-field">
            <span class="ec-entry-field__label">Chính sách danh tính cần duyệt</span>
            <select
              class="ec-entry-field__control"
              :value="identityReviewPolicy"
              :disabled="!requireIdentityVerification"
              @change="emit('update:identityReviewPolicy', $event.target.value)"
            >
              <option value="ALLOW_WITH_WARNING">Cho vào thi, cảnh báo giám thị</option>
              <option value="STRICT_VERIFIED_ONLY">Chỉ cho vào khi đã xác minh</option>
            </select>
          </label>

          <label class="ec-entry-field">
            <span class="ec-entry-field__label">Chu kỳ tái xác minh (giây)</span>
            <input
              class="ec-entry-field__control"
              type="number"
              min="30"
              max="600"
              step="30"
              :value="identityCheckIntervalSeconds"
              :disabled="!inExamIdentityCheckEnabled"
              @input="emit('update:identityCheckIntervalSeconds', Number($event.target.value) || 60)"
            />
          </label>
        </div>

        <div class="ec-entry-toggles">
          <div
            v-for="item in entryGateToggles"
            :key="item.key"
            class="ec-rule"
            :class="{ 'ec-rule--on': item.value, 'ec-rule--disabled': item.disabled }"
            role="button"
            :tabindex="item.disabled ? -1 : 0"
            :aria-disabled="item.disabled"
            :aria-pressed="item.value"
            @click="!item.disabled && item.toggle()"
            @keydown.enter.prevent="!item.disabled && item.toggle()"
            @keydown.space.prevent="!item.disabled && item.toggle()"
          >
            <div class="ec-rule__body">
              <span class="ec-rule__icon" :class="`ec-rule__icon--${item.color}`">
                <LucideIcon :name="item.icon" />
              </span>
              <div class="ec-rule__content">
                <p class="ec-rule__title">{{ item.title }}</p>
                <p class="ec-rule__desc">{{ item.desc }}</p>
              </div>
            </div>
            <div class="ec-rule__state">
              <span class="ec-rule__state-label">{{ item.value ? 'Đang bật' : 'Đang tắt' }}</span>
              <button
                type="button"
                class="ec-mini-switch"
                :class="item.value && 'ec-mini-switch--on'"
                :disabled="item.disabled"
                :aria-pressed="item.value"
                :aria-label="`${item.value ? 'Tắt' : 'Bật'} ${item.title}`"
                @click.stop="item.toggle"
              >
                <span class="ec-mini-switch__knob">
                  <LucideIcon v-if="item.value" name="check" size="10" stroke-width="3" />
                </span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { computed, reactive, watch } from 'vue'

const props = defineProps({
  enabled: { type: Boolean, default: true },
  monitorTabSwitch: { type: Boolean, default: true },
  monitorBlur: { type: Boolean, default: true },
  monitorExitFullscreen: { type: Boolean, default: true },
  monitorCopyPaste: { type: Boolean, default: false },
  monitorIdleTime: { type: Boolean, default: false },
  monitorDevtools: { type: Boolean, default: false },
  monitorDuplicateIp: { type: Boolean, default: true },
  monitorFastSubmit: { type: Boolean, default: false },
  monitorRightClick: { type: Boolean, default: true },
  monitorPrintScreen: { type: Boolean, default: true },
 monitorRapidQuestionSwitch: { type: Boolean, default: false },
 monitorMultiMonitor: { type: Boolean, default: true },
 requireCameraMic: { type: Boolean, default: true },
 monitorFullscreenEvasion: { type: Boolean, default: true },
 monitorNetworkInstability: { type: Boolean, default: false },
 monitorSessionRecovery: { type: Boolean, default: false },
 monitorQuestionTimingAnomaly: { type: Boolean, default: false },
 monitorAnswerChangeBurst: { type: Boolean, default: false },
 monitorClipboardBurst: { type: Boolean, default: false },
 monitorAnswerSimilarity: { type: Boolean, default: false },
 monitorIpFingerprintGraph: { type: Boolean, default: false },
 enableAiProctoring: { type: Boolean, default: false },
 aiFaceDetection: { type: Boolean, default: false },
 aiEyeTracking: { type: Boolean, default: false },
 rulesText: { type: String, default: '' },
 rulesVersion: { type: String, default: 'default-v1' },
 requireRulesAgreement: { type: Boolean, default: true },
 requireIdentityVerification: { type: Boolean, default: true },
 identityReviewPolicy: { type: String, default: 'ALLOW_WITH_WARNING' },
 inExamIdentityCheckEnabled: { type: Boolean, default: true },
 identityCheckIntervalSeconds: { type: Number, default: 60 }
})

const emit = defineEmits([
  'update:enabled',
  'update:monitorTabSwitch', 'update:monitorBlur', 'update:monitorExitFullscreen',
  'update:monitorCopyPaste', 'update:monitorIdleTime', 'update:monitorDevtools',
  'update:monitorDuplicateIp', 'update:monitorFastSubmit',
 'update:monitorRightClick', 'update:monitorPrintScreen',
 'update:monitorRapidQuestionSwitch', 'update:monitorMultiMonitor',
 'update:requireCameraMic',
 'update:monitorFullscreenEvasion',
 'update:monitorNetworkInstability', 'update:monitorSessionRecovery',
 'update:monitorQuestionTimingAnomaly', 'update:monitorAnswerChangeBurst',
 'update:monitorClipboardBurst', 'update:monitorAnswerSimilarity',
 'update:monitorIpFingerprintGraph', 'update:enableAiProctoring',
 'update:aiFaceDetection', 'update:aiEyeTracking',
 'update:rulesText', 'update:rulesVersion',
 'update:requireRulesAgreement', 'update:requireIdentityVerification',
 'update:identityReviewPolicy', 'update:inExamIdentityCheckEnabled',
 'update:identityCheckIntervalSeconds'
])

const localEnabled = computed({
  get: () => props.enabled,
  set: (v) => emit('update:enabled', v)
})

const localRules = reactive({
  monitorTabSwitch: props.monitorTabSwitch,
  monitorBlur: props.monitorBlur,
  monitorExitFullscreen: props.monitorExitFullscreen,
  monitorCopyPaste: false,
  monitorIdleTime: props.monitorIdleTime,
  monitorDevtools: false,
  monitorDuplicateIp: props.monitorDuplicateIp,
  monitorFastSubmit: false,
  monitorRightClick: props.monitorRightClick,
  monitorPrintScreen: props.monitorPrintScreen,
 monitorRapidQuestionSwitch: props.monitorRapidQuestionSwitch,
 monitorMultiMonitor: props.monitorMultiMonitor,
 requireCameraMic: props.requireCameraMic,
 monitorFullscreenEvasion: props.monitorFullscreenEvasion,
 monitorNetworkInstability: props.monitorNetworkInstability,
 monitorSessionRecovery: props.monitorSessionRecovery,
 monitorQuestionTimingAnomaly: props.monitorQuestionTimingAnomaly,
 monitorAnswerChangeBurst: props.monitorAnswerChangeBurst,
 monitorClipboardBurst: false,
 monitorAnswerSimilarity: props.monitorAnswerSimilarity,
 monitorIpFingerprintGraph: props.monitorIpFingerprintGraph,
 aiFaceDetection: props.requireCameraMic !== false && (props.aiFaceDetection || props.enableAiProctoring),
 aiEyeTracking: props.requireCameraMic !== false && (props.aiEyeTracking || props.enableAiProctoring)
})

const ruleKeys = [
  'monitorTabSwitch',
  'monitorBlur',
  'monitorExitFullscreen',
  'monitorIdleTime',
  'monitorDuplicateIp',
  'monitorRightClick',
  'monitorPrintScreen',
  'monitorRapidQuestionSwitch',
  'monitorMultiMonitor',
  'requireCameraMic',
  'monitorFullscreenEvasion',
  'monitorNetworkInstability',
  'monitorSessionRecovery',
  'monitorQuestionTimingAnomaly',
  'monitorAnswerChangeBurst',
  'monitorAnswerSimilarity',
  'monitorIpFingerprintGraph',
  'aiFaceDetection',
  'aiEyeTracking'
]

watch(
 () => [...ruleKeys.map((key) => props[key]), props.enableAiProctoring],
 () => {
  ruleKeys.forEach((key) => {
   localRules[key] = key === 'aiFaceDetection' || key === 'aiEyeTracking'
    ? props.requireCameraMic !== false && (props[key] || props.enableAiProctoring)
    : props[key]
  })
 },
 { immediate: true }
)

const rules = [
  {
    id: 'screenLeave',
    keys: ['monitorTabSwitch', 'monitorBlur', 'monitorExitFullscreen', 'monitorFullscreenEvasion'],
    icon: 'tab',
    color: 'primary',
    title: 'Rời màn hình thi',
    desc: 'Chuyển tab, mất focus hoặc thoát toàn màn hình'
  },
  {
    id: 'screenCapture',
    keys: ['monitorRightClick', 'monitorPrintScreen'],
    icon: 'screenshot',
    color: 'muted',
    title: 'Hạn chế thao tác',
    desc: 'Ghi nhận chuột phải và phím chụp màn hình'
  },
  {
    id: 'identity',
    keys: ['monitorDuplicateIp', 'monitorMultiMonitor'],
    icon: 'desktop_windows',
    color: 'warning',
    title: 'Thiết bị và IP',
    desc: 'Theo dõi IP trùng lặp và dấu hiệu nhiều màn hình'
  },
  {
    id: 'cameraMic',
    keys: ['requireCameraMic'],
    icon: 'videocam',
    color: 'primary',
    title: 'Yêu cầu camera/mic',
    desc: 'Kiểm tra quyền camera và microphone trước khi vào thi'
  },
  {
    id: 'aiFaceDetection',
    keys: ['aiFaceDetection'],
    icon: 'scan-face',
    color: 'info',
    title: 'AI nhận diện khuôn mặt',
    desc: 'Phát hiện không có mặt, nhiều khuôn mặt hoặc khuôn mặt bất thường',
    requiresCameraMic: true
  },
  {
    id: 'aiEyeTracking',
    keys: ['aiEyeTracking'],
    icon: 'visibility',
    color: 'info',
    title: 'AI theo dõi mắt',
    desc: 'Phân tích hướng nhìn và dấu hiệu nhìn ra ngoài màn hình',
    requiresCameraMic: true
  }
]

const emitAiAggregate = () => {
  emit('update:enableAiProctoring', localRules.aiFaceDetection || localRules.aiEyeTracking)
}

const isRuleOn = (rule) => rule.keys.some((key) => localRules[key])
const isRuleDisabled = (rule) => rule.requiresCameraMic && !localRules.requireCameraMic

const disableAiCameraRules = () => {
  localRules.aiFaceDetection = false
  localRules.aiEyeTracking = false
  emit('update:aiFaceDetection', false)
  emit('update:aiEyeTracking', false)
  emit('update:enableAiProctoring', false)
}

const emitRuleKeys = (keys, value) => {
  for (const key of keys) {
    localRules[key] = value
    emit(`update:${key}`, value)
  }
  if (keys.includes('requireCameraMic') && value === false) {
    disableAiCameraRules()
    return
  }
  if (keys.includes('aiFaceDetection') || keys.includes('aiEyeTracking')) emitAiAggregate()
}

const toggleRule = (rule) => {
  if (isRuleDisabled(rule)) return
  emitRuleKeys(rule.keys, !isRuleOn(rule))
}

const enableAll = () => {
  for (const rule of rules) {
    emitRuleKeys(rule.keys, true)
  }
  emit('update:enableAiProctoring', true)
}

const entryGateToggles = computed(() => [
  {
    key: 'rulesAgreement',
    icon: 'contract',
    color: 'primary',
    title: 'Bắt buộc cam kết quy chế',
    desc: 'Thí sinh phải đọc và xác nhận trước khi bắt đầu lượt thi',
    value: props.requireRulesAgreement,
    disabled: false,
    toggle: () => emit('update:requireRulesAgreement', !props.requireRulesAgreement)
  },
  {
    key: 'identityVerification',
    icon: 'verified_user',
    color: 'warning',
    title: 'Xác minh danh tính trước thi',
    desc: 'Chặn vào thi nếu chưa có kết quả danh tính hợp lệ',
    value: props.requireIdentityVerification,
    disabled: !localEnabled.value || !localRules.requireCameraMic,
    toggle: () => emit('update:requireIdentityVerification', !props.requireIdentityVerification)
  },
  {
    key: 'identityRecheck',
    icon: 'scan-face',
    color: 'warning',
    title: 'Tái xác minh trong khi thi',
    desc: 'Gửi mẫu camera định kỳ để giám thị theo dõi danh tính',
    value: props.inExamIdentityCheckEnabled,
    disabled: !localEnabled.value || !localRules.requireCameraMic || !props.requireIdentityVerification,
    toggle: () => emit('update:inExamIdentityCheckEnabled', !props.inExamIdentityCheckEnabled)
  }
])
</script>


<style scoped>
.ec-section {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.ec-section__header {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1.15rem 1.35rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-surface);
}

.ec-section__icon {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: #eef2ff;
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: inset 0 0 0 1px #dbe4ff;
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
  line-height: 1.45;
}

.ec-section__body {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

/* Master toggle */
.ec-proctor-master {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem;
  background: #f8fafc;
  border: 1px solid var(--ds-border);
  border-radius: 16px;
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
  min-width: 0;
}

.ec-proctor-master__icon {
  width: 42px;
  height: 42px;
  border-radius: 13px;
  background: #e5e7eb;
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.ec-proctor-master__icon--on {
  background: #eef2ff;
  color: var(--ds-primary);
  box-shadow: inset 0 0 0 1px #dbe4ff;
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
  width: 76px;
  height: 32px;
  border-radius: 999px;
  border: 1px solid var(--ds-border);
  background: var(--ds-gray-300);
  color: var(--ds-text-muted);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  padding: 3px;
  flex-shrink: 0;
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.dark .ec-toggle { background: var(--ds-gray-600); }

.ec-toggle:hover:not(:disabled) {
  border-color: var(--ds-primary-border);
  box-shadow: 0 0 0 3px var(--ds-primary-soft);
}

.ec-toggle:focus-visible {
  outline: none;
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-soft);
}

.ec-toggle--on {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: white;
}

.ec-toggle__knob {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: white;
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 4px rgba(15,23,42,0.24);
  transition: transform 0.2s ease, color 0.2s ease;
}

.ec-toggle__label {
  flex: 1;
  padding-left: 30px;
  padding-right: 8px;
  font-size: 0.72rem;
  font-weight: 800;
  line-height: 1;
  text-align: center;
  user-select: none;
}

.ec-toggle--on .ec-toggle__label {
  padding-left: 8px;
  padding-right: 30px;
}

.ec-toggle--on .ec-toggle__knob {
  transform: translateX(44px);
}

.ec-toggle:disabled {
  cursor: not-allowed;
  opacity: 0.65;
  box-shadow: none;
}

.ec-group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.ec-group-head__title {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
  letter-spacing: 0.01em;
  margin: 0;
}

.ec-group-head__desc {
  color: var(--ds-text-muted);
  font-size: 0.72rem;
  line-height: 1.4;
  margin: 0.2rem 0 0;
}

.ec-button {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  min-height: 34px;
  white-space: nowrap;
  font-size: 0.74rem;
  font-weight: 700;
  border-radius: 10px;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease;
}

.ec-button--secondary {
  color: var(--ds-primary);
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  padding: 0.45rem 0.7rem;
}

.ec-button--secondary:hover {
  background: #eef2ff;
  border-color: #c7d2fe;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}

.ec-button:focus-visible {
  outline: none;
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-soft);
}

.ec-proctor-rules__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 0.75rem;
}

.ec-entry-gate {
  display: grid;
  gap: 0.9rem;
  padding-top: 0.1rem;
}

.ec-entry-gate__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.875rem;
}

.ec-entry-field {
  display: grid;
  gap: 0.45rem;
  min-width: 0;
}

.ec-entry-field--wide {
  grid-column: 1 / -1;
}

.ec-entry-field__label {
  color: var(--ds-text-secondary);
  font-size: 0.75rem;
  font-weight: 650;
}

.ec-entry-field__control {
  width: 100%;
  border: 1px solid var(--ds-border);
  border-radius: 12px;
  background: var(--ds-surface);
  color: var(--ds-text);
  padding: 0.72rem 0.8rem;
  font: inherit;
  font-size: 0.82rem;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, background-color 0.15s ease;
}

.ec-entry-field__control:focus {
  outline: none;
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-soft);
}

.ec-entry-field__control--textarea {
  resize: vertical;
  min-height: 112px;
  line-height: 1.45;
}

.ec-entry-field__control:disabled {
  background: #f8fafc;
  color: var(--ds-text-muted);
}

.ec-entry-toggles {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 0.75rem;
}

/* Rule card */
.ec-rule {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  min-height: 86px;
  padding: 0.95rem;
  background: #fbfdff;
  border: 1px solid var(--ds-border);
  border-radius: 14px;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.ec-rule:hover:not(.ec-rule--disabled) {
  background: var(--ds-surface);
  border-color: #c7d2fe;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.055);
}

.ec-rule:focus-visible {
  outline: none;
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-soft);
}

.dark .ec-rule {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ec-rule--on {
  background: linear-gradient(180deg, #ffffff 0%, #fafaff 100%);
  border-color: #c7d2fe;
  box-shadow: inset 3px 0 0 #a5b4fc, 0 1px 2px rgba(15, 23, 42, 0.035);
}

.dark .ec-rule--on {
  background: var(--ds-gray-800);
  border-color: var(--ds-primary-border);
}

.ec-rule--disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ec-rule--disabled .ec-rule__body,
.ec-rule--disabled .ec-mini-switch {
  cursor: not-allowed;
}

.ec-rule__body {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
  min-width: 0;
}

.ec-rule__content {
  min-width: 0;
}

.ec-rule__icon {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: inset 0 0 0 1px rgba(255,255,255,0.65);
}


.ec-rule__icon--primary { background: var(--ds-primary-soft); color: var(--ds-primary); }
.ec-rule__icon--success { background: var(--ds-success-soft); color: var(--ds-success); }
.ec-rule__icon--info { background: var(--ds-info-soft); color: var(--ds-info); }
.ec-rule__icon--warning { background: var(--ds-warning-soft); color: var(--ds-warning); }
.ec-rule__icon--danger { background: #fef2f2; color: var(--ds-danger); }
.ec-rule__icon--muted { background: var(--ds-gray-200); color: var(--ds-text-muted); }
.dark .ec-rule__icon--muted { background: var(--ds-gray-700); color: #94a3b8; }

.ec-rule__title {
  font-size: 0.86rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.25;
}

.dark .ec-rule__title { color: #f1f5f9; }

.ec-rule__desc {
  font-size: 0.73rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  line-height: 1.45;
}

.ec-rule__state {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  flex-shrink: 0;
}

.ec-rule__state-label {
  color: var(--ds-text-muted);
  font-size: 0.69rem;
  font-weight: 700;
  white-space: nowrap;
}

.ec-rule--on .ec-rule__state-label {
  color: var(--ds-primary);
}

.ec-mini-switch {
  position: relative;
  width: 40px;
  height: 22px;
  border-radius: 999px;
  border: 1px solid var(--ds-border);
  background: #e5e7eb;
  color: var(--ds-primary);
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  padding: 2px;
  flex-shrink: 0;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.ec-mini-switch:hover:not(:disabled) {
  border-color: #c7d2fe;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.10);
}

.ec-mini-switch:focus-visible {
  outline: none;
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-soft);
}

.ec-mini-switch--on {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: var(--ds-primary);
}

.ec-mini-switch--on:hover:not(:disabled) {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
}

.ec-mini-switch__knob {
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.22);
  transition: transform 0.18s ease;
}

.ec-mini-switch--on .ec-mini-switch__knob {
  transform: translateX(18px);
}

.ec-mini-switch:disabled {
  background: var(--ds-gray-100);
  border-color: var(--ds-border);
  cursor: not-allowed;
  box-shadow: none;
}

@media (max-width: 720px) {
  .ec-section__body {
    padding: 1rem;
  }

  .ec-proctor-master,
  .ec-rule {
    align-items: flex-start;
  }

  .ec-proctor-master {
    padding: 1rem;
  }

  .ec-proctor-rules__grid,
  .ec-entry-toggles {
    grid-template-columns: 1fr;
  }

  .ec-entry-gate__grid {
    grid-template-columns: 1fr;
  }

  .ec-rule__state-label {
    display: none;
  }
}
</style>
