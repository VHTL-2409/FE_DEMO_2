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
            :key="rule.id"
            class="ec-rule"
            :class="{ 'ec-rule--on': isRuleOn(rule), 'ec-rule--disabled': isRuleDisabled(rule) }"
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
              :class="isRuleOn(rule) && 'ec-toggle--on'"
              :disabled="isRuleDisabled(rule)"
              @click="toggleRule(rule)"
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
 aiEyeTracking: { type: Boolean, default: false }
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
 'update:aiFaceDetection', 'update:aiEyeTracking'
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
    color: 'danger',
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
    color: 'danger',
    title: 'AI nhận diện khuôn mặt',
    desc: 'Phát hiện không có mặt, nhiều khuôn mặt hoặc khuôn mặt bất thường',
    requiresCameraMic: true
  },
  {
    id: 'aiEyeTracking',
    keys: ['aiEyeTracking'],
    icon: 'visibility',
    color: 'danger',
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

.ec-rule--disabled {
  opacity: 0.55;
}

.ec-rule--disabled .ec-rule__body,
.ec-rule--disabled .ec-toggle {
  cursor: not-allowed;
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
