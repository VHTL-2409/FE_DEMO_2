<template>
  <div class="aap" :class="{ 'aap--active': anomalies.length > 0 }">
    <!-- Header -->
    <div class="aap__header">
      <div class="aap__header-left">
        <div class="aap__ai-icon" :class="anomalies.length > 0 ? 'aap__ai-icon--active' : 'aap__ai-icon--idle'">
          <LucideIcon :name="anomalies.length > 0 ? 'psychology' : 'auto_awesome'" />
        </div>
        <div>
          <h3 class="aap__title">Phát hiện bất thường</h3>
          <span class="aap__subtitle">{{ anomalies.length > 0 ? 'AI đang giám sát' : 'Đang chờ dữ liệu' }}</span>
        </div>
      </div>

      <div class="aap__header-right">
        <!-- AI confidence indicator -->
        <div class="aap__ai-badge" :class="confidenceClass">
          <LucideIcon name="auto_awesome" />
          <span>{{ aiConfidence }}%</span>
        </div>

        <!-- Toggle -->
        <button
          type="button"
          class="aap__toggle-btn"
          :class="{ 'aap__toggle-btn--on': isEnabled }"
          :title="isEnabled ? 'Tắt AI giám sát' : 'Bật AI giám sát'"
          @click="isEnabled = !isEnabled"
        >
          <span class="aap__toggle-thumb" />
        </button>
      </div>
    </div>

    <!-- Stats strip -->
    <div class="aap__stats">
      <div class="aap__stat" :class="{ 'aap__stat--danger': anomalyStats.critical > 0 }">
        <LucideIcon name="gpp_bad" />
        <span class="aap__stat-val">{{ anomalyStats.critical }}</span>
        <span class="aap__stat-label">Nghiêm trọng</span>
      </div>
      <div class="aap__stat" :class="{ 'aap__stat--warning': anomalyStats.suspicious > 0 }">
        <LucideIcon name="report" />
        <span class="aap__stat-val">{{ anomalyStats.suspicious }}</span>
        <span class="aap__stat-label">Đáng nghi</span>
      </div>
      <div class="aap__stat" :class="{ 'aap__stat--info': anomalyStats.patterns > 0 }">
        <LucideIcon name="bubble_chart" />
        <span class="aap__stat-val">{{ anomalyStats.patterns }}</span>
        <span class="aap__stat-label">Pattern</span>
      </div>
    </div>

    <!-- AI scanning animation -->
    <div v-if="isScanning && isEnabled" class="aap__scan-bar">
      <div class="aap__scan-track">
        <div class="aap__scan-fill" />
      </div>
      <span class="aap__scan-label">
        <LucideIcon name="radar" />
        Đang phân tích...
      </span>
    </div>

    <!-- Loading skeleton -->
    <div v-if="loading && isEnabled" class="aap__loading">
      <div v-for="i in 3" :key="i" class="aap__skeleton-card">
        <div class="aap__skeleton ap__skeleton--icon" />
        <div class="aap__skeleton-body">
          <div class="aap__skeleton aap__skeleton--line" />
          <div class="aap__skeleton aap__skeleton--line aap__skeleton--short" />
        </div>
      </div>
    </div>

    <!-- Empty state -->
    <div v-else-if="anomalies.length === 0 && isEnabled && !loading" class="aap__empty">
      <div class="aap__empty-icon">
        <LucideIcon name="verified_user" />
      </div>
      <p class="aap__empty-title">Không phát hiện bất thường</p>
      <p class="aap__empty-desc">Tất cả thí sinh đang hoạt động bình thường theo đánh giá của AI.</p>
    </div>

    <!-- Disabled state -->
    <div v-else-if="!isEnabled" class="aap__disabled">
      <div class="aap__disabled-icon">
        <LucideIcon name="auto_awesome" />
      </div>
      <p class="aap__disabled-title">AI giám sát đang tắt</p>
      <p class="aap__disabled-desc">Bật để AI phân tích hành vi thí sinh theo thời gian thực.</p>
      <button type="button" class="aap__enable-btn" @click="isEnabled = true">
        <LucideIcon name="power" />
        Bật AI giám sát
      </button>
    </div>

    <!-- Anomaly list -->
    <div v-else-if="anomalies.length > 0" class="aap__list">
      <TransitionGroup name="aap-anomaly" tag="div" class="aap__anomaly-items">
        <div
          v-for="(anomaly, idx) in displayedAnomalies"
          :key="anomaly.id || idx"
          class="aap__anomaly-item"
          :class="[`aap__anomaly-item--${anomalySeverity(anomaly)}`, { 'aap__anomaly-item--new': isNewAnomaly(anomaly) }]"
        >
          <!-- Severity icon -->
          <div class="aap__anomaly-icon" :class="`aap__anomaly-icon--${anomalySeverity(anomaly)}`">
            <LucideIcon :name="anomalyIcon(anomaly)" />
          </div>

          <!-- Content -->
          <div class="aap__anomaly-content">
            <div class="aap__anomaly-title-row">
              <p class="aap__anomaly-title">{{ anomalyTitle(anomaly) }}</p>
              <span class="aap__anomaly-badge" :class="`aap__anomaly-badge--${anomalySeverity(anomaly)}`">
                {{ anomalyConfidence(anomaly) }}%
              </span>
            </div>
            <p class="aap__anomaly-student">
              {{ anomaly.studentName || anomaly.studentEmail || '—' }}
              <span v-if="anomaly.examTitle"> · {{ anomaly.examTitle }}</span>
            </p>

            <!-- AI explanation -->
            <p v-if="anomaly.aiExplanation" class="aap__ai-explanation">
              <LucideIcon name="auto_awesome" />
              {{ anomaly.aiExplanation }}
            </p>

            <!-- Evidence chips -->
            <div v-if="anomaly.evidence && anomaly.evidence.length > 0" class="aap__evidence">
              <span
                v-for="ev in anomaly.evidence.slice(0, 3)"
                :key="ev.key || ev"
                class="aap__evidence-chip"
                :class="`aap__evidence-chip--${ev.severity || 'info'}`"
              >
                {{ ev.label || ev.key || ev }}
              </span>
              <span v-if="anomaly.evidence.length > 3" class="aap__evidence-chip aap__evidence-chip--more">
                +{{ anomaly.evidence.length - 3 }}
              </span>
            </div>
          </div>

          <!-- Time + actions -->
          <div class="aap__anomaly-meta">
            <span class="aap__anomaly-time">{{ relativeTime(anomaly.detectedAt || anomaly.timestamp) }}</span>
            <div class="aap__anomaly-actions">
              <button
                type="button"
                class="aap__action-btn aap__action-btn--warn"
                title="Cảnh báo"
                @click.stop="$emit('warn', anomaly)"
              >
                <LucideIcon name="warning" />
              </button>
              <button
                type="button"
                class="aap__action-btn aap__action-btn--investigate"
                title="Điều tra"
                @click.stop="$emit('investigate', anomaly)"
              >
                <LucideIcon name="search" />
              </button>
            </div>
          </div>

          <!-- New indicator -->
          <span v-if="isNewAnomaly(anomaly)" class="aap__new-dot" />
        </div>
      </TransitionGroup>

      <!-- Show more -->
      <div v-if="anomalies.length > displayLimit" class="aap__show-more">
        <button type="button" class="aap__show-more-btn" @click="displayLimit += 5">
          Hiện thêm {{ Math.min(5, anomalies.length - displayLimit) }} bất thường
          <LucideIcon name="expand_more" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import LucideIcon from '../../common/LucideIcon.vue'

const props = defineProps({
  anomalies: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  isScanning: { type: Boolean, default: false },
  aiConfidence: { type: Number, default: 92 }
})

const emit = defineEmits(['warn', 'investigate', 'toggle'])

const isEnabled = ref(true)
const displayLimit = ref(5)

// Track new anomalies for flash animation
const newAnomalyIds = ref(new Set())
const prevAnomalyCount = ref(0)

watch(() => props.anomalies.length, (newLen) => {
  if (newLen > prevAnomalyCount.value && props.anomalies.length > 0) {
    const newest = props.anomalies[0]
    if (newest?.id) {
      newAnomalyIds.value.add(newest.id)
      setTimeout(() => {
        newAnomalyIds.value.delete(newest.id)
      }, 4000)
    }
  }
  prevAnomalyCount.value = newLen
})

watch(isEnabled, (val) => emit('toggle', val))

const displayedAnomalies = computed(() => props.anomalies.slice(0, displayLimit.value))

const isNewAnomaly = (anomaly) => newAnomalyIds.value.has(anomaly.id)

const anomalyStats = computed(() => {
  const list = props.anomalies
  return {
    critical: list.filter(a => a.severity === 'CRITICAL' || a.riskLevel === 'critical' || a.riskScore >= 80).length,
    suspicious: list.filter(a => a.severity === 'SUSPICIOUS' || a.riskLevel === 'suspicious' || (a.riskScore >= 40 && a.riskScore < 80)).length,
    patterns: list.filter(a => a.type === 'PATTERN' || a.type === 'GROUP_BEHAVIOR' || a.isPattern).length
  }
})

const confidenceClass = computed(() => {
  if (props.aiConfidence >= 90) return 'aap__ai-badge--high'
  if (props.aiConfidence >= 70) return 'aap__ai-badge--medium'
  return 'aap__ai-badge--low'
})

// Anomaly detection helpers
const anomalySeverity = (anomaly) => {
  if (anomaly.severity === 'CRITICAL' || anomaly.riskLevel === 'critical' || anomaly.riskScore >= 80) return 'critical'
  if (anomaly.severity === 'HIGH_RISK' || anomaly.riskLevel === 'high' || (anomaly.riskScore >= 60 && anomaly.riskScore < 80)) return 'high'
  if (anomaly.severity === 'SUSPICIOUS' || anomaly.riskLevel === 'suspicious') return 'suspicious'
  return 'info'
}

const anomalyIcon = (anomaly) => {
  const type = anomaly.type || ''
  const subtype = anomaly.subType || ''
  if (type === 'SPEED_ABNORMAL' || subtype === 'FAST_COMPLETION') return 'bolt'
  if (type === 'COPY_PASTE' || subtype === 'COPY') return 'content_paste_off'
  if (type === 'TAB_SWITCH' || subtype === 'TAB') return 'tab'
  if (type === 'DEVTOOLS') return 'code'
  if (type === 'FACE_DETECTION_FAIL') return 'face'
  if (type === 'MULTI_SCREEN') return 'desktop_windows'
  if (type === 'PATTERN_MATCH') return 'bubble_chart'
  if (type === 'GROUP_BEHAVIOR' || subtype === 'COLLABORATION') return 'groups'
  if (type === 'IDLE_TIME') return 'timer_off'
  if (type === 'NETWORK_ANOMALY') return 'wifi_off'
  if (type === 'SCREENSHOT') return 'screenshot'
  if (type === 'FULLSCREEN_EXIT') return 'fullscreen_exit'
  if (anomaly.riskScore >= 80) return 'gpp_bad'
  if (anomaly.riskScore >= 60) return 'warning'
  return 'report'
}

const anomalyTitle = (anomaly) => {
  const type = anomaly.type || ''
  const subtype = anomaly.subType || ''
  if (type === 'SPEED_ABNORMAL' || subtype === 'FAST_COMPLETION') return 'Tốc độ bất thường'
  if (type === 'COPY_PASTE' || subtype === 'COPY') return 'Phát hiện copy/paste'
  if (type === 'TAB_SWITCH' || subtype === 'TAB') return 'Chuyển tab nhiều lần'
  if (type === 'DEVTOOLS') return 'Mở DevTools'
  if (type === 'FACE_DETECTION_FAIL') return 'Lỗi nhận diện khuôn mặt'
  if (type === 'MULTI_SCREEN') return 'Đa màn hình'
  if (type === 'PATTERN_MATCH') return 'Pattern đáng nghi'
  if (type === 'GROUP_BEHAVIOR' || subtype === 'COLLABORATION') return 'Hành vi nhóm'
  if (type === 'IDLE_TIME') return 'Không hoạt động'
  if (type === 'NETWORK_ANOMALY') return 'Bất thường mạng'
  if (type === 'SCREENSHOT') return 'Chụp màn hình'
  if (type === 'FULLSCREEN_EXIT') return 'Thoát toàn màn hình'
  if (type === 'CAMERA_OFF') return 'Camera tắt kéo dài'
  if (type === 'VOICE_DETECTED') return 'Phát hiện giọng nói'
  return anomaly.title || anomaly.description || 'Bất thường được phát hiện'
}

const anomalyConfidence = (anomaly) => {
  if (anomaly.confidence !== undefined) return Math.round(anomaly.confidence * 100)
  if (anomaly.riskScore !== undefined) return Math.round(anomaly.riskScore)
  return anomalySeverity(anomaly) === 'critical' ? 88 : anomalySeverity(anomaly) === 'high' ? 75 : 65
}

const now = () => new Date()

const relativeTime = (ts) => {
  if (!ts) return ''
  const diff = now() - new Date(ts)
  const secs = Math.floor(diff / 1000)
  if (secs < 60) return 'Vừa xong'
  const mins = Math.floor(secs / 60)
  if (mins < 60) return `${mins}m trước`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}h trước`
  return `${Math.floor(hours / 24)}d trước`
}
</script>


<style scoped>
.aap {
  display: flex;
  flex-direction: column;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
  transition: border-color 0.3s ease;
}

.dark .aap { border-color: var(--ds-border-strong); }

.aap--active { border-color: rgba(139, 92, 246, 0.3); }

/* Header */
.aap__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.875rem 1rem;
  border-bottom: 1px solid var(--ds-border);
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.05) 0%, transparent 100%);
  flex-shrink: 0;
}

.dark .aap__header {
  border-bottom-color: var(--ds-border-strong);
}

.aap__header-left {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.aap__ai-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.aap__ai-icon--idle {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
}

.aap__ai-icon--active {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.12), rgba(99, 102, 241, 0.12));
  color: #8b5cf6;
  animation: aapPulse 2s ease-in-out infinite;
}

@keyframes aapPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(139, 92, 246, 0.3); }
  50% { box-shadow: 0 0 0 6px rgba(139, 92, 246, 0); }
}

.aap__title {
  font-family: var(--ds-font-display);
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .aap__title { color: #f1f5f9; }

.aap__subtitle {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}

.aap__header-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

/* AI confidence badge */
.aap__ai-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.2rem 0.5rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 800;
}

.aap__ai-badge--high { background: var(--ds-success-soft); color: var(--ds-success); }
.aap__ai-badge--medium { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.aap__ai-badge--low { background: var(--ds-danger-soft); color: var(--ds-danger); }

/* Toggle switch */
.aap__toggle-btn {
  width: 36px;
  height: 20px;
  border-radius: 10px;
  border: none;
  background: var(--ds-gray-200);
  cursor: pointer;
  position: relative;
  transition: background 0.2s ease;
  padding: 0;
}

.dark .aap__toggle-btn { background: var(--ds-gray-700); }

.aap__toggle-btn--on { background: #8b5cf6; }

.aap__toggle-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s ease;
}

.aap__toggle-btn--on .aap__toggle-thumb { transform: translateX(16px); }

/* Stats strip */
.aap__stats {
  display: flex;
  gap: 0.375rem;
  padding: 0.625rem 0.875rem;
  border-bottom: 1px solid var(--ds-border);
  flex-shrink: 0;
}

.dark .aap__stats { border-bottom-color: var(--ds-border-strong); }

.aap__stat {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.625rem;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-gray-50);
  font-size: 0.7rem;
  flex: 1;
  justify-content: center;
  color: var(--ds-text-muted);
  transition: all 0.15s ease;
}

.dark .aap__stat { background: var(--ds-gray-800); }

.aap__stat > .lucide { font-size: 0.875rem; flex-shrink: 0; }

.aap__stat--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }
.aap__stat--warning { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.aap__stat--info { background: var(--ds-primary-soft); color: var(--ds-primary); }

.aap__stat-val {
  font-weight: 900;
  font-family: var(--ds-font-display);
  font-size: 0.875rem;
  line-height: 1;
}

.aap__stat-label {
  font-weight: 600;
  font-size: 0.6rem;
  white-space: nowrap;
}

/* Scan bar */
.aap__scan-bar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  border-bottom: 1px solid var(--ds-border);
  flex-shrink: 0;
}

.dark .aap__scan-bar { border-bottom-color: var(--ds-border-strong); }

.aap__scan-track {
  flex: 1;
  height: 3px;
  background: var(--ds-gray-200);
  border-radius: 2px;
  overflow: hidden;
}

.dark .aap__scan-track { background: var(--ds-gray-700); }

.aap__scan-fill {
  height: 100%;
  width: 30%;
  background: linear-gradient(90deg, #8b5cf6, #6366f1, #8b5cf6);
  background-size: 200% 100%;
  border-radius: 2px;
  animation: aapScan 1.5s ease-in-out infinite;
}

@keyframes aapScan {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.aap__scan-label {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.65rem;
  font-weight: 700;
  color: #8b5cf6;
  white-space: nowrap;
  animation: aapBlink 1.5s ease-in-out infinite;
}

@keyframes aapBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* Loading */
.aap__loading {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.75rem;
}

.aap__skeleton-card {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.625rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-gray-50);
}

.dark .aap__skeleton-card { background: var(--ds-gray-800); }

.aap__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-100) 25%, var(--ds-gray-200) 50%, var(--ds-gray-100) 75%);
  background-size: 200% 100%;
  animation: aapShimmer 1.5s infinite;
  border-radius: var(--ds-radius-md);
}

.dark .aap__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-800) 25%, var(--ds-gray-700) 50%, var(--ds-gray-800) 75%);
  background-size: 200% 100%;
}

@keyframes aapShimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.ap__skeleton--icon { width: 32px; height: 32px; border-radius: var(--ds-radius-lg); flex-shrink: 0; }
.aap__skeleton--line { height: 0.75rem; margin-bottom: 0.25rem; }
.aap__skeleton--short { width: 60%; }
.aap__skeleton-body { flex: 1; min-width: 0; }

/* Empty state */
.aap__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 2rem 1rem;
  text-align: center;
}

.aap__empty-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--ds-success-soft);
  color: var(--ds-success);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
}

.aap__empty-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .aap__empty-title { color: #f1f5f9; }

.aap__empty-desc {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0;
  line-height: 1.5;
}

/* Disabled state */
.aap__disabled {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 2rem 1rem;
  text-align: center;
}

.aap__disabled-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
}

.dark .aap__disabled-icon { background: var(--ds-gray-700); }

.aap__disabled-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .aap__disabled-title { color: #f1f5f9; }

.aap__disabled-desc {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0;
  line-height: 1.5;
}

.aap__enable-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-full);
  background: #8b5cf6;
  color: white;
  font-size: 0.8rem;
  font-weight: 700;
  border: none;
  cursor: pointer;
  transition: all 0.15s ease;
  margin-top: 0.25rem;
}

.aap__enable-btn:hover {
  background: #7c3aed;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}

/* Anomaly list */
.aap__list { overflow-y: auto; max-height: 400px; }
.aap__anomaly-items { display: flex; flex-direction: column; }

/* Anomaly item */
.aap__anomaly-item {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.75rem 0.875rem;
  cursor: pointer;
  transition: background 0.1s ease;
  position: relative;
  border-left: 4px solid transparent;
  border-bottom: 1px solid var(--ds-border);
}

.dark .aap__anomaly-item { border-bottom-color: var(--ds-border-strong); }

.aap__anomaly-item:hover { background: var(--ds-gray-50); }
.dark .aap__anomaly-item:hover { background: var(--ds-gray-800); }

.aap__anomaly-item--new {
  animation: aapFlash 4s ease-out forwards;
}

@keyframes aapFlash {
  0% { background: rgba(139, 92, 246, 0.08); }
  15% { background: transparent; }
  30% { background: rgba(139, 92, 246, 0.05); }
  100% { background: transparent; }
}

/* Severity borders */
.aap__anomaly-item--critical { border-left-color: var(--ds-danger); }
.aap__anomaly-item--high { border-left-color: #d97706; }
.aap__anomaly-item--suspicious { border-left-color: #eab308; }
.aap__anomaly-item--info { border-left-color: var(--ds-info); }

/* Anomaly icon */
.aap__anomaly-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.aap__anomaly-icon--critical { background: var(--ds-danger-soft); color: var(--ds-danger); }
.aap__anomaly-icon--high { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.aap__anomaly-icon--suspicious { background: rgba(234, 179, 8, 0.08); color: #eab308; }
.aap__anomaly-icon--info { background: var(--ds-info-soft); color: var(--ds-info); }

/* Content */
.aap__anomaly-content { flex: 1; min-width: 0; }

.aap__anomaly-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.2rem;
}

.aap__anomaly-title {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.3;
}

.dark .aap__anomaly-title { color: #f1f5f9; }

.aap__anomaly-badge {
  font-size: 0.6rem;
  font-weight: 800;
  padding: 0.1rem 0.4rem;
  border-radius: var(--ds-radius-full);
  white-space: nowrap;
  flex-shrink: 0;
}

.aap__anomaly-badge--critical { background: var(--ds-danger-soft); color: var(--ds-danger); }
.aap__anomaly-badge--high { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.aap__anomaly-badge--suspicious { background: rgba(234, 179, 8, 0.07); color: #ca8a04; }
.aap__anomaly-badge--info { background: var(--ds-info-soft); color: var(--ds-info); }

.aap__anomaly-student {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.aap__ai-explanation {
  display: flex;
  align-items: flex-start;
  gap: 0.25rem;
  font-size: 0.65rem;
  color: #8b5cf6;
  margin: 0.375rem 0 0;
  font-style: italic;
  line-height: 1.4;
}

.aap__ai-explanation > .lucide { flex-shrink: 0; margin-top: 0.1rem; }

/* Evidence chips */
.aap__evidence {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
  margin-top: 0.375rem;
}

.aap__evidence-chip {
  font-size: 0.6rem;
  font-weight: 700;
  padding: 0.1rem 0.4rem;
  border-radius: var(--ds-radius-full);
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
}

.dark .aap__evidence-chip { background: var(--ds-gray-700); }

.aap__evidence-chip--danger { background: var(--ds-danger-soft); color: var(--ds-danger); }
.aap__evidence-chip--warning { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.aap__evidence-chip--info { background: var(--ds-info-soft); color: var(--ds-info); }
.aap__evidence-chip--more { background: var(--ds-gray-200); }
.dark .aap__evidence-chip--more { background: var(--ds-gray-600); }

/* Meta */
.aap__anomaly-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.375rem;
  flex-shrink: 0;
}

.aap__anomaly-time {
  font-size: 0.65rem;
  color: var(--ds-text-muted);
  font-weight: 600;
  white-space: nowrap;
}

.aap__anomaly-actions {
  display: flex;
  gap: 0.125rem;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.aap__anomaly-item:hover .aap__anomaly-actions { opacity: 1; }

.aap__action-btn {
  width: 1.625rem;
  height: 1.625rem;
  border: none;
  border-radius: var(--ds-radius-sm);
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.12s ease;
}

.dark .aap__action-btn { background: var(--ds-gray-700); }

.aap__action-btn--warn:hover { background: rgba(234, 179, 8, 0.1); color: #d97706; }
.dark .aap__action-btn--warn:hover { background: rgba(234, 179, 8, 0.1); }

.aap__action-btn--investigate:hover { background: var(--ds-primary-soft); color: var(--ds-primary); }
.dark .aap__action-btn--investigate:hover { background: rgba(79, 70, 229, 0.1); }

/* New dot */
.aap__new-dot {
  position: absolute;
  top: 0.75rem;
  right: 0.5rem;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #8b5cf6;
  box-shadow: 0 0 0 2px rgba(139, 92, 246, 0.2);
}

/* Show more */
.aap__show-more { padding: 0.625rem 0.875rem; border-top: 1px solid var(--ds-border); }
.dark .aap__show-more { border-top-color: var(--ds-border-strong); }

.aap__show-more-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  padding: 0.5rem;
  border: 1.5px dashed var(--ds-border);
  border-radius: var(--ds-radius-lg);
  background: transparent;
  color: var(--ds-text-muted);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
}

.dark .aap__show-more-btn { border-color: var(--ds-border-strong); }
.aap__show-more-btn:hover { border-color: #8b5cf6; color: #8b5cf6; background: rgba(139, 92, 246, 0.04); }
.dark .aap__show-more-btn:hover { background: rgba(139, 92, 246, 0.08); }

/* Transition */
.aap-anomaly-enter-active { transition: all 0.3s ease; }
.aap-anomaly-enter-from { opacity: 0; transform: translateX(-12px); background: rgba(139, 92, 246, 0.08); }
.aap-anomaly-leave-active { transition: all 0.2s ease; }
.aap-anomaly-leave-to { opacity: 0; transform: translateX(12px); }
</style>
