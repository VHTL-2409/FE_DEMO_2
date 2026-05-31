<template>
  <div class="ser">
    <main class="ser__main">
      <section class="ser__header">
        <button type="button" class="ser__back" @click="goBack">
          <LucideIcon name="arrow_back" />
          Quay lại phòng chờ
        </button>
        <div class="ser__title-row">
          <div>
            <p class="ser__eyebrow">Cam kết trước khi vào thi</p>
            <h1 class="ser__title">{{ examTitle }}</h1>
          </div>
          <span class="ser__code">{{ examCode }}</span>
        </div>
      </section>

      <section class="ser__grid">
        <article class="ser__panel">
          <h2 class="ser__panel-title">
            <LucideIcon name="info" />
            Thông tin kỳ thi
          </h2>
          <dl class="ser__facts">
            <div><dt>Thời lượng</dt><dd>{{ durationLabel }}</dd></div>
            <div><dt>Số câu</dt><dd>{{ questionLabel }}</dd></div>
            <div><dt>Bắt đầu</dt><dd>{{ timeLabel(startAt) }}</dd></div>
            <div><dt>Kết thúc</dt><dd>{{ timeLabel(endAt) }}</dd></div>
            <div v-if="className"><dt>Lớp</dt><dd>{{ className }}</dd></div>
          </dl>
        </article>

        <article class="ser__panel">
          <h2 class="ser__panel-title">
            <LucideIcon name="shield_check" />
            Trạng thái vào thi
          </h2>
          <div class="ser__status-list">
            <span class="ser__status" :class="entryStatus?.rulesAccepted ? 'ser__status--ok' : 'ser__status--warn'">
              <LucideIcon :name="entryStatus?.rulesAccepted ? 'check_circle' : 'contract'" />
              Quy chế {{ entryStatus?.rulesAccepted ? 'đã cam kết' : 'chưa cam kết' }}
            </span>
            <span class="ser__status" :class="identityStatusClass">
              <LucideIcon :name="identityStatusIcon" />
              Danh tính {{ identityStatusText }}
            </span>
            <span class="ser__status" :class="entryStatus?.cameraReady ? 'ser__status--ok' : 'ser__status--warn'">
              <LucideIcon name="videocam" />
              Camera {{ entryStatus?.cameraReady ? 'sẵn sàng' : 'cần kiểm tra' }}
            </span>
          </div>
          <p v-if="needsReview" class="ser__note ser__note--warn">
            Danh tính cần giám thị kiểm tra lại, bạn vẫn được vào thi và phiên thi sẽ được theo dõi.
          </p>
          <div v-if="blockedReasons.length" class="ser__blocked">
            <p>Điều kiện còn thiếu:</p>
            <ul>
              <li v-for="reason in blockedReasons" :key="reason">{{ reasonLabel(reason) }}</li>
            </ul>
          </div>
        </article>
      </section>

      <section class="ser__panel ser__rules">
        <h2 class="ser__panel-title">
          <LucideIcon name="gavel" />
          Quy chế bắt buộc
        </h2>
        <ol class="ser__rule-list">
          <li v-for="(line, index) in ruleLines" :key="`${index}-${line}`">{{ line }}</li>
        </ol>
      </section>

      <section class="ser__grid">
        <article class="ser__panel">
          <h2 class="ser__panel-title">
            <LucideIcon name="monitor" />
            Giám sát trong kỳ thi
          </h2>
          <div class="ser__chips">
            <span v-for="item in proctoringItems" :key="item.key" class="ser__chip" :class="item.enabled ? 'ser__chip--on' : 'ser__chip--off'">
              <LucideIcon :name="item.icon" />
              {{ item.label }}
            </span>
          </div>
        </article>

        <article class="ser__panel">
          <h2 class="ser__panel-title">
            <LucideIcon name="scan-face" />
            Xác minh danh tính
          </h2>
          <p class="ser__copy">
            Hệ thống đã ghi nhận kết quả xác minh ban đầu. Khi kỳ thi bật camera/AI, hệ thống sẽ kiểm tra nhẹ định kỳ trong lúc thi để hỗ trợ giám thị phát hiện bất thường.
          </p>
          <p class="ser__copy ser__copy--muted">
            Chu kỳ kiểm tra: {{ entryStatus?.identityCheckIntervalSeconds || 60 }} giây.
          </p>
        </article>
      </section>
    </main>

    <footer class="ser__cta">
      <label class="ser__agree">
        <input v-model="agreed" type="checkbox" />
        <span>Tôi đã đọc, hiểu và cam kết tuân thủ toàn bộ quy chế kỳ thi.</span>
      </label>
      <button type="button" class="ser__start" :disabled="!canSubmitAgreement" @click="confirmAndStart">
        <LucideIcon :name="isStarting ? 'progress_activity' : 'rocket_launch'" />
        {{ isStarting ? 'Đang vào bài thi...' : 'Bắt đầu thi' }}
      </button>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { agreeAttemptRules, getAttemptEntryStatus, startAttempt } from '../../services/attemptService'
import { updateDeviceStatus } from '../../services/monitoringService'
import { buildAttemptQuery } from '../../services/studentExamContextStorage'
import { useToast } from '../../composables/useToast'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const agreed = ref(false)
const isStarting = ref(false)
const entryStatus = ref(null)

const examId = computed(() => Number.parseInt(String(route.query.examId || ''), 10) || null)
const attemptId = computed(() => Number.parseInt(String(route.query.attemptId || ''), 10) || null)
const examTitle = computed(() => String(route.query.exam || 'Bài thi'))
const examCode = computed(() => String(route.query.examCode || '-'))
const durationLabel = computed(() => `${Number.parseInt(String(route.query.duration || '60'), 10) || 60} phút`)
const questionLabel = computed(() => `${Number.parseInt(String(route.query.questions || '0'), 10) || 0} câu`)
const startAt = computed(() => String(route.query.startAt || ''))
const endAt = computed(() => String(route.query.endAt || ''))
const className = computed(() => String(route.query.className || '').trim())

const identityStatus = computed(() => String(entryStatus.value?.identityStatus || route.query.verificationStatus || 'NOT_CHECKED').toUpperCase())
const needsReview = computed(() => identityStatus.value === 'NEEDS_REVIEW')
const blockedReasons = computed(() => Array.isArray(entryStatus.value?.blockedReasons) ? entryStatus.value.blockedReasons : [])
const canSubmitAgreement = computed(() => agreed.value && !isStarting.value)

const ruleLines = computed(() => {
  const text = entryStatus.value?.rulesText || ''
  return text
    .split(/\r?\n/)
    .map((line) => line.replace(/^\s*\d+[\).\s-]*/, '').trim())
    .filter(Boolean)
})

const proctoringItems = computed(() => [
  { key: 'camera', icon: 'videocam', label: 'Camera/micro', enabled: String(route.query.requireCameraMic || 'true') !== 'false' },
  { key: 'ai', icon: 'scan-face', label: 'AI camera', enabled: String(route.query.enableAiProctoring || 'false') === 'true' },
  { key: 'identity', icon: 'verified_user', label: 'Xác minh định kỳ', enabled: entryStatus.value?.inExamIdentityCheckEnabled !== false },
  { key: 'fullscreen', icon: 'fullscreen', label: 'Toàn màn hình', enabled: true }
])

const identityStatusText = computed(() => {
  if (identityStatus.value === 'VERIFIED') return 'đã xác minh'
  if (identityStatus.value === 'NEEDS_REVIEW') return 'cần giám thị duyệt'
  if (identityStatus.value === 'REJECTED') return 'bị từ chối'
  return 'chưa xác minh'
})

const identityStatusIcon = computed(() => {
  if (identityStatus.value === 'VERIFIED') return 'check_circle'
  if (identityStatus.value === 'NEEDS_REVIEW') return 'flag'
  if (identityStatus.value === 'REJECTED') return 'error'
  return 'shield'
})

const identityStatusClass = computed(() => {
  if (identityStatus.value === 'VERIFIED') return 'ser__status--ok'
  if (identityStatus.value === 'NEEDS_REVIEW') return 'ser__status--warn'
  return 'ser__status--danger'
})

const timeLabel = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' })
}

const reasonLabel = (reason) => {
  const map = {
    MISSING_RULES_AGREEMENT: 'Bạn chưa cam kết quy chế.',
    CAMERA_NOT_READY: 'Camera chưa được backend ghi nhận là sẵn sàng.',
    IDENTITY_NOT_VERIFIED: 'Danh tính chưa đạt điều kiện vào thi.'
  }
  return map[reason] || reason
}

const loadEntryStatus = async () => {
  if (!attemptId.value) return
  entryStatus.value = await getAttemptEntryStatus(attemptId.value)
  agreed.value = Boolean(entryStatus.value?.rulesAccepted)
}

const requestExamFullscreen = async () => {
  if (typeof document === 'undefined') return false
  if (document.fullscreenElement || document.webkitFullscreenElement) return true
  const target = document.documentElement
  const request = target?.requestFullscreen || target?.webkitRequestFullscreen
  if (!request) return false
  try {
    await request.call(target, { navigationUI: 'hide' })
  } catch {
    try {
      await request.call(target)
    } catch {
      return false
    }
  }
  return Boolean(document.fullscreenElement || document.webkitFullscreenElement)
}

const goBack = () => {
  router.push({ path: '/student/exam-waiting-room', query: route.query })
}

const confirmAndStart = async () => {
  if (!examId.value || !attemptId.value) {
    toast.error('Thiếu thông tin phiên thi.')
    return
  }
  isStarting.value = true
  try {
    await updateDeviceStatus(attemptId.value, true, true)
    const status = await agreeAttemptRules(attemptId.value)
    entryStatus.value = status
    if (status?.blockedReasons?.length) {
      toast.error('Chưa đủ điều kiện vào thi.')
      return
    }
    const fullscreenReady = await requestExamFullscreen()
    if (!fullscreenReady) {
      toast.warning('Trình duyệt chưa cho phép vào toàn màn hình. Hãy thử lại để bắt đầu bài thi.')
      return
    }
    const started = await startAttempt(examId.value)
    router.push({
      path: '/student/exam-interface',
      query: buildAttemptQuery({
        examTitle: examTitle.value,
        examId: examId.value,
        attemptId: started.attemptId,
        deadlineAt: started.deadlineAt,
        remainingSeconds: started.remainingSeconds,
        startedAt: started.startedAt,
        identityCheckId: started.identityCheckId || route.query.identityCheckId || '',
        verificationStatus: started.identityStatus || identityStatus.value
      })
    })
  } catch (error) {
    const data = error?.data || error?.response?.data
    const reasons = data?.data?.entryBlockedReasons || data?.data?.blockedReasons
    if (Array.isArray(reasons) && reasons.length) {
      toast.error(`Chưa đủ điều kiện: ${reasons.map(reasonLabel).join(' ')}`)
    } else {
      toast.error('Không thể bắt đầu bài thi lúc này.')
    }
    await loadEntryStatus()
  } finally {
    isStarting.value = false
  }
}

onMounted(loadEntryStatus)
</script>

<style scoped>
.ser {
  min-height: 100%;
  background: var(--ds-bg);
  color: var(--ds-text);
  padding-bottom: 96px;
}

.ser__main {
  width: min(1100px, calc(100% - 32px));
  margin: 0 auto;
  padding: 24px 0;
}

.ser__header,
.ser__panel {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: 8px;
  box-shadow: var(--ds-shadow-sm);
}

.ser__header {
  padding: 20px;
  margin-bottom: 16px;
}

.ser__back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  border-radius: 8px;
  padding: 8px 12px;
  font-weight: 700;
}

.ser__title-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
}

.ser__eyebrow {
  margin: 0 0 6px;
  color: var(--ds-primary);
  font-size: 0.78rem;
  font-weight: 800;
  text-transform: uppercase;
}

.ser__title {
  margin: 0;
  font-size: clamp(1.35rem, 3vw, 2rem);
  line-height: 1.2;
}

.ser__code {
  border: 1px solid var(--ds-border);
  border-radius: 8px;
  padding: 8px 10px;
  font-weight: 900;
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.ser__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.ser__panel {
  padding: 18px;
}

.ser__panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 14px;
  font-size: 1rem;
}

.ser__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 0;
}

.ser__facts dt {
  color: var(--ds-text-muted);
  font-size: 0.72rem;
  font-weight: 700;
}

.ser__facts dd {
  margin: 4px 0 0;
  font-weight: 800;
}

.ser__status-list,
.ser__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ser__status,
.ser__chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 0.78rem;
  font-weight: 800;
}

.ser__status--ok,
.ser__chip--on {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.ser__status--warn {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.ser__status--danger {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

.ser__chip--off {
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
}

.ser__note,
.ser__blocked {
  margin: 12px 0 0;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 0.82rem;
  font-weight: 650;
}

.ser__note--warn,
.ser__blocked {
  background: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.ser__blocked ul {
  margin: 6px 0 0;
  padding-left: 18px;
}

.ser__rules {
  margin-bottom: 16px;
}

.ser__rule-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding-left: 22px;
  line-height: 1.55;
  font-weight: 650;
}

.ser__copy {
  margin: 0;
  line-height: 1.6;
  font-weight: 650;
}

.ser__copy--muted {
  margin-top: 10px;
  color: var(--ds-text-muted);
}

.ser__cta {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 14px 20px;
  border-top: 1px solid var(--ds-border);
  background: color-mix(in srgb, var(--ds-bg) 96%, transparent);
  box-shadow: 0 -8px 28px rgba(15, 23, 42, 0.08);
}

.ser__agree {
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: 620px;
  font-weight: 750;
  line-height: 1.35;
}

.ser__agree input {
  width: 18px;
  height: 18px;
}

.ser__start {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 180px;
  border: 0;
  border-radius: 8px;
  padding: 12px 18px;
  background: var(--ds-primary);
  color: white;
  font-weight: 900;
}

.ser__start:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 760px) {
  .ser__grid,
  .ser__facts {
    grid-template-columns: 1fr;
  }

  .ser__title-row,
  .ser__cta {
    align-items: stretch;
    flex-direction: column;
  }

  .ser__start {
    width: 100%;
  }
}
</style>
