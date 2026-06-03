<template>
  <section class="fraud-panel">
    <header class="fraud-panel__header">
      <div class="fraud-panel__title-wrap">
        <LucideIcon
          name="shield-alert"
          class="fraud-panel__title-icon"
        />
        <div>
          <h2 class="fraud-panel__title">
            Phân tích gian lận và chấm điểm
          </h2>
          <p class="fraud-panel__subtitle">
            Tổng hợp đáp án, thời gian, IP, hành vi phiên thi, thống kê điểm và cảnh báo cần rà soát.
          </p>
        </div>
      </div>
      <div class="fraud-panel__actions">
        <select
          v-model="selectedExamId"
          class="fraud-panel__select"
          @change="resetResults"
        >
          <option value="">
            Chọn bài thi
          </option>
          <option
            v-for="exam in exams"
            :key="exam.id"
            :value="exam.id"
          >
            {{ exam.title }}
          </option>
        </select>
        <button
          type="button"
          class="fraud-panel__button fraud-panel__button--ghost"
          :disabled="!selectedExamId || loading"
          @click="loadWarnings"
        >
          <LucideIcon name="refresh-cw" />
          Tải cảnh báo
        </button>
        <button
          type="button"
          class="fraud-panel__button fraud-panel__button--primary"
          :disabled="!selectedExamId || loading"
          @click="runFullAnalysis"
        >
          <LucideIcon name="play" />
          Phân tích
        </button>
      </div>
    </header>

    <div
      v-if="loading"
      class="fraud-panel__loading"
    >
      <div class="fraud-panel__spinner" />
      <span>{{ loadingMessage }}</span>
    </div>

    <div
      v-else-if="!selectedExamId"
      class="fraud-panel__empty"
    >
      <LucideIcon name="search" />
      <h3>Chọn bài thi để bắt đầu phân tích</h3>
      <p>Dữ liệu phân tích sẽ được tổng hợp từ bài làm đã nộp, tín hiệu giám sát và cảnh báo đã ghi nhận.</p>
    </div>

    <template v-else>
      <div class="fraud-panel__summary">
        <button
          type="button"
          class="fraud-panel__metric"
          @click="activeTab = 'warnings'"
        >
          <span class="fraud-panel__metric-label">Cảnh báo cần duyệt</span>
          <strong>{{ pendingWarningCount }}</strong>
          <small>{{ warningSummary.totalWarnings || 0 }} cảnh báo tổng</small>
        </button>
        <button
          type="button"
          class="fraud-panel__metric"
          @click="activeTab = 'overview'"
        >
          <span class="fraud-panel__metric-label">Thí sinh bị gắn cờ</span>
          <strong>{{ comprehensiveResult?.flaggedAttempts || flaggedAttemptItems.length }}</strong>
          <small>{{ comprehensiveResult?.totalAttempts || 0 }} bài đã xét</small>
        </button>
        <button
          type="button"
          class="fraud-panel__metric"
          @click="activeTab = 'overview'"
        >
          <span class="fraud-panel__metric-label">Rủi ro cao nhất</span>
          <strong>{{ highestRiskScore }}</strong>
          <small>{{ highestRiskLevel }}</small>
        </button>
        <button
          type="button"
          class="fraud-panel__metric"
          @click="activeTab = 'plagiarism'"
        >
          <span class="fraud-panel__metric-label">Cặp đáp án đáng ngờ</span>
          <strong>{{ plagiarismReports.length }}</strong>
          <small>{{ timingResults.length + behaviorAnomalies.length }} tín hiệu hành vi</small>
        </button>
      </div>

      <nav
        class="fraud-panel__tabs"
        aria-label="Nhóm phân tích gian lận"
      >
        <button
          v-for="tab in tabs"
          :key="tab.id"
          type="button"
          class="fraud-panel__tab"
          :class="{ 'fraud-panel__tab--active': activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          <LucideIcon :name="tab.icon" />
          <span>{{ tab.label }}</span>
          <em v-if="tab.count">{{ tab.count }}</em>
        </button>
      </nav>

      <div
        v-if="errorMessage"
        class="fraud-panel__notice fraud-panel__notice--error"
      >
        <LucideIcon name="alert-triangle" />
        <span>{{ errorMessage }}</span>
      </div>

      <section
        v-if="activeTab === 'overview'"
        class="fraud-panel__section"
      >
        <div class="fraud-panel__grid fraud-panel__grid--two">
          <article class="fraud-panel__card">
            <h3>Mẫu gian lận nổi bật</h3>
            <div
              v-if="suspiciousPatterns.length"
              class="fraud-panel__list"
            >
              <div
                v-for="pattern in suspiciousPatterns"
                :key="pattern.id"
                class="fraud-panel__row"
              >
                <div>
                  <strong>{{ pattern.title || pattern.patternType }}</strong>
                  <p>{{ pattern.studentUsername }} · {{ pattern.description }}</p>
                </div>
                <span :class="severityClass(pattern.severity)">{{ labelSeverity(pattern.severity) }}</span>
              </div>
            </div>
            <p
              v-else
              class="fraud-panel__muted"
            >
              Chưa phát hiện mẫu gian lận nổi bật.
            </p>
          </article>

          <article class="fraud-panel__card">
            <h3>Thí sinh cần rà soát</h3>
            <div
              v-if="flaggedAttemptItems.length"
              class="fraud-panel__list"
            >
              <div
                v-for="item in flaggedAttemptItems"
                :key="item.attemptId"
                class="fraud-panel__row"
              >
                <div>
                  <strong>{{ item.studentUsername }}</strong>
                  <p>{{ formatIndicators(item.fraudIndicators) }}</p>
                </div>
                <span :class="riskClass(item.riskScore)">{{ item.riskScore }}</span>
              </div>
            </div>
            <p
              v-else
              class="fraud-panel__muted"
            >
              Không có thí sinh bị gắn cờ trong kết quả hiện tại.
            </p>
          </article>
        </div>
      </section>

      <section
        v-if="activeTab === 'warnings'"
        class="fraud-panel__section"
      >
        <div class="fraud-panel__toolbar">
          <select
            v-model="warningFilters.reviewStatus"
            class="fraud-panel__select fraud-panel__select--compact"
          >
            <option value="">
              Tất cả trạng thái
            </option>
            <option value="NEEDS_REVIEW">
              Cần duyệt
            </option>
            <option value="CONFIRMED">
              Đã xác nhận
            </option>
            <option value="FALSE_POSITIVE">
              Báo sai
            </option>
            <option value="RESOLVED">
              Đã xử lý
            </option>
            <option value="DISMISSED">
              Đã bỏ qua
            </option>
          </select>
          <select
            v-model="warningFilters.severity"
            class="fraud-panel__select fraud-panel__select--compact"
          >
            <option value="">
              Tất cả mức độ
            </option>
            <option value="CRITICAL">
              Nghiêm trọng
            </option>
            <option value="HIGH">
              Cao
            </option>
            <option value="MEDIUM">
              Trung bình
            </option>
            <option value="LOW">
              Thấp
            </option>
          </select>
          <select
            v-model="warningFilters.category"
            class="fraud-panel__select fraud-panel__select--compact"
          >
            <option value="">
              Tất cả nhóm
            </option>
            <option
              v-for="category in warningCategories"
              :key="category"
              :value="category"
            >
              {{ labelCategory(category) }}
            </option>
          </select>
          <button
            type="button"
            class="fraud-panel__button fraud-panel__button--ghost"
            :disabled="loading"
            @click="generateWarnings"
          >
            <LucideIcon name="sparkles" />
            Tạo từ tín hiệu
          </button>
        </div>

        <div
          v-if="filteredWarnings.length"
          class="fraud-panel__warning-list"
        >
          <article
            v-for="warning in filteredWarnings"
            :key="warning.id"
            class="fraud-panel__warning"
          >
            <div class="fraud-panel__warning-main">
              <div class="fraud-panel__warning-head">
                <strong>{{ warning.message || warning.type }}</strong>
                <span :class="severityClass(warning.severity)">{{ labelSeverity(warning.severity) }}</span>
                <span class="fraud-panel__chip">{{ labelReviewStatus(warning.reviewStatus) }}</span>
              </div>
              <p>{{ warning.studentName || warning.studentUsername || 'Nhóm thí sinh' }} · {{ labelCategory(warning.category) }} · {{ formatDate(warning.createdAt) }}</p>
              <div class="fraud-panel__evidence">
                {{ summarizeEvidence(warning.evidence) }}
              </div>
            </div>
            <div class="fraud-panel__review">
              <button
                type="button"
                @click="reviewWarningItem(warning, 'CONFIRMED')"
              >
                Xác nhận
              </button>
              <button
                type="button"
                @click="reviewWarningItem(warning, 'FALSE_POSITIVE')"
              >
                Báo sai
              </button>
              <button
                type="button"
                @click="reviewWarningItem(warning, 'RESOLVED')"
              >
                Đã xử lý
              </button>
            </div>
          </article>
        </div>
        <p
          v-else
          class="fraud-panel__muted fraud-panel__muted--center"
        >
          Không có cảnh báo phù hợp bộ lọc.
        </p>
      </section>

      <section
        v-if="activeTab === 'statistics'"
        class="fraud-panel__section"
      >
        <div class="fraud-panel__grid fraud-panel__grid--stats">
          <div class="fraud-panel__stat">
            <span>Trung bình</span><strong>{{ scoreStats.mean ?? 0 }}</strong>
          </div>
          <div class="fraud-panel__stat">
            <span>Độ lệch chuẩn</span><strong>{{ scoreStats.stdDev ?? 0 }}</strong>
          </div>
          <div class="fraud-panel__stat">
            <span>Thấp nhất</span><strong>{{ scoreStats.min ?? 0 }}</strong>
          </div>
          <div class="fraud-panel__stat">
            <span>Cao nhất</span><strong>{{ scoreStats.max ?? 0 }}</strong>
          </div>
        </div>
        <div
          v-if="statisticalResults.length"
          class="fraud-panel__list fraud-panel__list--spaced"
        >
          <div
            v-for="(item, idx) in statisticalResults"
            :key="`${item.signalType}-${idx}`"
            class="fraud-panel__row"
          >
            <div>
              <strong>{{ labelSignal(item.signalType) }}</strong>
              <p>{{ summarizeEvidence(item.evidence) }}</p>
            </div>
            <span :class="severityClass(item.severity)">{{ labelSeverity(item.severity) }}</span>
          </div>
        </div>
        <p
          v-else
          class="fraud-panel__muted fraud-panel__muted--center"
        >
          Không có bất thường thống kê.
        </p>
      </section>

      <section
        v-if="activeTab === 'behavior'"
        class="fraud-panel__section"
      >
        <div
          v-if="behaviorAnomalies.length"
          class="fraud-panel__list fraud-panel__list--spaced"
        >
          <div
            v-for="(item, idx) in behaviorAnomalies"
            :key="idx"
            class="fraud-panel__row"
          >
            <strong>{{ item }}</strong>
          </div>
        </div>
        <p
          v-else
          class="fraud-panel__muted fraud-panel__muted--center"
        >
          Không có bất thường hành vi.
        </p>
      </section>

      <section
        v-if="activeTab === 'plagiarism'"
        class="fraud-panel__section"
      >
        <div
          v-if="plagiarismReports.length"
          class="fraud-panel__list fraud-panel__list--spaced"
        >
          <div
            v-for="report in plagiarismReports"
            :key="report.id"
            class="fraud-panel__row"
          >
            <div>
              <strong>{{ report.student1Name }} ↔ {{ report.student2Name }}</strong>
              <p>{{ report.sameAnswers }}/{{ report.commonQuestions }} câu giống nhau · {{ report.recommendation }}</p>
            </div>
            <span :class="similarityClass(report.similarityScore)">{{ formatPercent(report.similarityScore) }}</span>
          </div>
        </div>
        <p
          v-else
          class="fraud-panel__muted fraud-panel__muted--center"
        >
          Không phát hiện cặp đáp án trùng bất thường.
        </p>
      </section>

      <section
        v-if="activeTab === 'timing'"
        class="fraud-panel__section"
      >
        <div
          v-if="timingResults.length"
          class="fraud-panel__list fraud-panel__list--spaced"
        >
          <div
            v-for="(item, idx) in timingResults"
            :key="`${item.signalType}-${idx}`"
            class="fraud-panel__row"
          >
            <div>
              <strong>{{ labelSignal(item.signalType) }}</strong>
              <p>{{ summarizeEvidence(item.evidence) }}</p>
            </div>
            <span :class="severityClass(item.severity)">{{ labelSeverity(item.severity) }}</span>
          </div>
        </div>
        <p
          v-else
          class="fraud-panel__muted fraud-panel__muted--center"
        >
          Không phát hiện bất thường thời gian.
        </p>
      </section>

      <section
        v-if="activeTab === 'ip'"
        class="fraud-panel__section"
      >
        <div
          v-if="ipResults.length"
          class="fraud-panel__list fraud-panel__list--spaced"
        >
          <div
            v-for="item in ipResults"
            :key="item.ipAddress"
            class="fraud-panel__row"
          >
            <div>
              <strong>{{ item.ipAddress }}</strong>
              <p>{{ item.studentCount || 0 }} thí sinh · {{ formatList(item.studentUsernames) }}</p>
            </div>
            <span :class="severityClass(item.riskLevel)">{{ labelSeverity(item.riskLevel) }}</span>
          </div>
        </div>
        <p
          v-else
          class="fraud-panel__muted fraud-panel__muted--center"
        >
          Không phát hiện bất thường IP.
        </p>
      </section>

      <section
        v-if="activeTab === 'ml-risk'"
        class="fraud-panel__section"
      >
        <div
          v-if="mlRiskResults.length"
          class="fraud-panel__list fraud-panel__list--spaced"
        >
          <div
            v-for="item in sortedMlRiskResults"
            :key="item.attemptId"
            class="fraud-panel__row"
          >
            <div>
              <strong>{{ item.studentUsername || `Attempt #${item.attemptId}` }}</strong>
              <p>
                Rule: {{ item.ruleBasedScore ?? 0 }} · {{ mlScoreLabel(item) }} · Combined: {{ item.combinedScore ?? 0 }}
              </p>
              <p>{{ mlSourceLabel(item) }}</p>
              <p>{{ summarizeMlFeatures(item.topFeatures) }}</p>
            </div>
            <span :class="severityClass(item.combinedLevel || item.mlRiskLevel || item.ruleBasedLevel)">
              {{ labelSeverity(item.combinedLevel || item.mlRiskLevel || item.ruleBasedLevel) }}
            </span>
          </div>
        </div>
        <p
          v-else
          class="fraud-panel__muted fraud-panel__muted--center"
        >
          Chưa có kết quả ML risk cho bài thi này.
        </p>
      </section>

      <section
        v-if="activeTab === 'grading'"
        class="fraud-panel__section"
      >
        <article
          v-if="gradingResult"
          class="fraud-panel__card fraud-panel__card--grading"
        >
          <div class="fraud-panel__grade">
            <strong>{{ formatScore(gradingResult.finalScore) }}</strong>
            <span>/ {{ gradingResult.maxScore || 100 }}</span>
          </div>
          <div class="fraud-panel__grade-details">
            <p>Điểm thô: <strong>{{ formatScore(gradingResult.rawScore) }}</strong></p>
            <p>Độ tin cậy IRT: <strong>{{ gradingResult.irtResult?.reliability ?? '-' }}</strong></p>
            <p>Số bài so sánh: <strong>{{ gradingResult.peerResult?.totalPeers ?? 0 }}</strong></p>
          </div>
        </article>
        <p
          v-else
          class="fraud-panel__muted fraud-panel__muted--center"
        >
          Chưa có kết quả chấm điểm.
        </p>
      </section>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { listExams } from '../../../services/examService'
import { useToast } from '../../../composables/useToast'
import {
  fetchFraudWarningsByExam,
  generateFraudWarningsForExam,
  reviewFraudWarning,
  runComprehensiveExamAnalysis,
  runExamBehaviorAnalysis,
  runExamStatisticalAnalysis,
  runGradingByExam,
  runIpReputationAnalysis,
  runPlagiarismAnalysis,
  runExamTimingAnalysis,
  runMlRiskByExam
} from '../../../services/fraudAnalysisService'

const props = defineProps({
  initialExamId: {
    type: [Number, String],
    default: null
  }
})

const toast = useToast()
const selectedExamId = ref(props.initialExamId ? String(props.initialExamId) : '')
const activeTab = ref('overview')
const loading = ref(false)
const loadingMessage = ref('')
const errorMessage = ref('')
const exams = ref([])

const warningSummary = ref({})
const plagiarismResult = ref(null)
const timingResult = ref(null)
const ipResult = ref(null)
const gradingResult = ref(null)
const comprehensiveResult = ref(null)
const statisticalResult = ref(null)
const behaviorResult = ref(null)
const mlRiskResults = ref([])

const warningFilters = reactive({
  reviewStatus: '',
  severity: '',
  category: ''
})

onMounted(async () => {
  exams.value = await listExams()
  if (selectedExamId.value) {
    await runFullAnalysis()
  }
})

const warnings = computed(() => warningSummary.value?.warnings || [])
const plagiarismReports = computed(() => plagiarismResult.value?.plagiarismReports || comprehensiveResult.value?.plagiarism?.plagiarismReports || [])
const timingResults = computed(() => timingResult.value?.timingResults || comprehensiveResult.value?.timing?.timingResults || [])
const ipResults = computed(() => ipResult.value?.ipResults || comprehensiveResult.value?.ipReputation?.ipResults || [])
const scoreStats = computed(() => statisticalResult.value?.scoreStats || comprehensiveResult.value?.statistical?.scoreStats || {})
const statisticalResults = computed(() => statisticalResult.value?.statisticalResults || comprehensiveResult.value?.statistical?.statisticalResults || [])
const behaviorAnomalies = computed(() => behaviorResult.value?.anomalies || comprehensiveResult.value?.behavior?.anomalies || [])
const suspiciousPatterns = computed(() => comprehensiveResult.value?.suspiciousPatterns || [])
const flaggedAttemptItems = computed(() => comprehensiveResult.value?.flaggedAttemptItems || [])
const sortedMlRiskResults = computed(() => [...mlRiskResults.value].sort((a, b) => Number(b.combinedScore || b.ruleBasedScore || 0) - Number(a.combinedScore || a.ruleBasedScore || 0)))
const pendingWarningCount = computed(() => warnings.value.filter(item => item.reviewStatus === 'NEEDS_REVIEW').length)
const warningCategories = computed(() => [...new Set(warnings.value.map(item => item.category).filter(Boolean))])

const highestRiskScore = computed(() => {
  const candidates = [
    ...flaggedAttemptItems.value.map(item => Number(item.riskScore || 0)),
    ...mlRiskResults.value.map(item => Number(item.combinedScore || item.ruleBasedScore || 0))
  ]
  return candidates.length ? Math.max(...candidates) : 0
})

const highestRiskLevel = computed(() => {
  const item = [...flaggedAttemptItems.value]
    .sort((a, b) => Number(b.riskScore || 0) - Number(a.riskScore || 0))[0]
  return item?.riskLevel || (highestRiskScore.value >= 61 ? 'HIGH_RISK' : highestRiskScore.value >= 40 ? 'SUSPICIOUS' : 'CLEAN')
})

const filteredWarnings = computed(() => warnings.value.filter(item => {
  if (warningFilters.reviewStatus && item.reviewStatus !== warningFilters.reviewStatus) return false
  if (warningFilters.severity && item.severity !== warningFilters.severity) return false
  if (warningFilters.category && item.category !== warningFilters.category) return false
  return true
}))

const tabs = computed(() => [
  { id: 'overview', label: 'Tổng quan', icon: 'layout-dashboard', count: suspiciousPatterns.value.length },
  { id: 'warnings', label: 'Cảnh báo', icon: 'alert-triangle', count: pendingWarningCount.value },
  { id: 'statistics', label: 'Thống kê', icon: 'bar-chart-2', count: statisticalResults.value.length },
  { id: 'behavior', label: 'Hành vi', icon: 'activity', count: behaviorAnomalies.value.length },
  { id: 'plagiarism', label: 'Đáp án', icon: 'file_text', count: plagiarismReports.value.length },
  { id: 'timing', label: 'Thời gian', icon: 'timer', count: timingResults.value.length },
  { id: 'ip', label: 'IP', icon: 'globe', count: ipResults.value.length },
  { id: 'ml-risk', label: 'ML risk', icon: 'psychology', count: mlRiskResults.value.length },
  { id: 'grading', label: 'Chấm điểm', icon: 'award', count: gradingResult.value ? 1 : 0 }
])

function resetResults() {
  errorMessage.value = ''
  warningSummary.value = {}
  plagiarismResult.value = null
  timingResult.value = null
  ipResult.value = null
  gradingResult.value = null
  comprehensiveResult.value = null
  statisticalResult.value = null
  behaviorResult.value = null
  mlRiskResults.value = []
}

async function runFullAnalysis() {
  if (!selectedExamId.value) return
  loading.value = true
  errorMessage.value = ''
  resetResults()
  try {
    loadingMessage.value = 'Đang phân tích tổng hợp...'
    const [
      comprehensive,
      plagiarism,
      timing,
      ip,
      statistical,
      behavior,
      grading,
      mlRisk
    ] = await Promise.allSettled([
      runComprehensiveExamAnalysis(selectedExamId.value),
      runPlagiarismAnalysis(selectedExamId.value),
      runExamTimingAnalysis(selectedExamId.value),
      runIpReputationAnalysis(selectedExamId.value),
      runExamStatisticalAnalysis(selectedExamId.value),
      runExamBehaviorAnalysis(selectedExamId.value),
      runGradingByExam(selectedExamId.value),
      runMlRiskByExam(selectedExamId.value)
    ])

    comprehensiveResult.value = unwrapSettled(comprehensive)
    plagiarismResult.value = unwrapSettled(plagiarism)
    timingResult.value = unwrapSettled(timing)
    ipResult.value = unwrapSettled(ip)
    statisticalResult.value = unwrapSettled(statistical)
    behaviorResult.value = unwrapSettled(behavior)
    gradingResult.value = unwrapSettled(grading)
    mlRiskResults.value = unwrapSettled(mlRisk) || []

    loadingMessage.value = 'Đang tải cảnh báo...'
    await loadWarnings()
    const failed = [comprehensive, plagiarism, timing, ip, statistical, behavior, grading, mlRisk].filter(r => r.status === 'rejected').length
    if (failed) {
      errorMessage.value = `${failed} nhóm phân tích chưa tải được. Các phần còn lại vẫn có thể rà soát.`
    }
  } catch (error) {
    errorMessage.value = error?.message || 'Không thể chạy phân tích gian lận.'
  } finally {
    loading.value = false
    loadingMessage.value = ''
  }
}

async function loadWarnings() {
  if (!selectedExamId.value) return
  const summary = await fetchFraudWarningsByExam(selectedExamId.value)
  warningSummary.value = summary || {}
}

async function generateWarnings() {
  if (!selectedExamId.value) return
  loading.value = true
  loadingMessage.value = 'Đang tạo cảnh báo từ tín hiệu giám sát...'
  try {
    const result = await generateFraudWarningsForExam(selectedExamId.value)
    await loadWarnings()
    toast.success(`Đã ghi nhận ${result?.recorded || 0} cảnh báo từ tín hiệu.`)
  } catch (error) {
    errorMessage.value = error?.message || 'Không thể tạo cảnh báo từ tín hiệu.'
  } finally {
    loading.value = false
    loadingMessage.value = ''
  }
}

async function reviewWarningItem(warning, status) {
  if (!warning?.id) return
  try {
    await reviewFraudWarning(warning.id, status, '')
    await loadWarnings()
    toast.success('Đã cập nhật trạng thái cảnh báo.')
  } catch (error) {
    errorMessage.value = error?.message || 'Không thể cập nhật cảnh báo.'
  }
}

function unwrapSettled(result) {
  return result.status === 'fulfilled' ? result.value : null
}

function labelSeverity(value) {
  const map = { CRITICAL: 'Nghiêm trọng', HIGH: 'Cao', HIGH_RISK: 'Cao', MEDIUM: 'Trung bình', SUSPICIOUS: 'Đáng ngờ', LOW: 'Thấp', CLEAN: 'Sạch' }
  return map[String(value || '').toUpperCase()] || value || '-'
}

function labelCategory(value) {
  const map = {
    ANSWER_PATTERN: 'Mẫu đáp án',
    TIMING_PATTERN: 'Thời gian',
    SYNCHRONIZATION: 'Đồng bộ hành vi',
    SESSION_INTEGRITY: 'Phiên thi',
    IDENTITY_NETWORK: 'Danh tính/IP',
    CAMERA_PROCTORING: 'AI camera',
    POST_EXAM_STATISTICAL: 'Thống kê sau thi'
  }
  return map[value] || value || 'Khác'
}

function labelReviewStatus(value) {
  const map = {
    NEEDS_REVIEW: 'Cần duyệt',
    CONFIRMED: 'Đã xác nhận',
    FALSE_POSITIVE: 'Báo sai',
    RESOLVED: 'Đã xử lý',
    DISMISSED: 'Đã bỏ qua'
  }
  return map[value] || value || '-'
}

function labelSignal(value) {
  return String(value || '').replaceAll('_', ' ').toLowerCase().replace(/(^|\s)\S/g, s => s.toUpperCase())
}

function severityClass(value) {
  const normalized = String(value || '').toUpperCase()
  return {
    'fraud-panel__badge': true,
    'fraud-panel__badge--danger': ['CRITICAL', 'HIGH', 'HIGH_RISK'].includes(normalized),
    'fraud-panel__badge--warn': ['MEDIUM', 'SUSPICIOUS'].includes(normalized),
    'fraud-panel__badge--neutral': !['CRITICAL', 'HIGH', 'HIGH_RISK', 'MEDIUM', 'SUSPICIOUS'].includes(normalized)
  }
}

function riskClass(score) {
  const value = Number(score || 0)
  return severityClass(value >= 61 ? 'HIGH' : value >= 40 ? 'MEDIUM' : 'LOW')
}

function similarityClass(score) {
  const value = Number(score || 0)
  return severityClass(value >= 0.95 ? 'HIGH' : value >= 0.9 ? 'MEDIUM' : 'LOW')
}

function summarizeEvidence(evidence) {
  if (!evidence) return 'Không có bằng chứng chi tiết.'
  if (typeof evidence === 'string') return evidence
  const message = evidence.message || evidence.details
  if (message) return String(message)
  const entries = Object.entries(evidence).filter(([, value]) => value !== null && value !== undefined)
  return entries.slice(0, 4).map(([key, value]) => `${key}: ${formatEvidenceValue(value)}`).join(' · ') || 'Không có bằng chứng chi tiết.'
}

function summarizeMlFeatures(features) {
  if (!Array.isArray(features) || !features.length) return 'Chưa có feature nổi bật.'
  return features
    .slice(0, 3)
    .map(feature => `${labelSignal(feature.featureName)}: ${formatScore(feature.value)}`)
    .join(' · ')
}

function mlScoreLabel(item) {
  const source = String(item?.scoringSource || item?.mlAnalysis?.source || '').toUpperCase()
  if (source === 'RULE_BASED' || item?.modelType === 'RULE_BASED') {
    return 'ML: chưa bật'
  }
  if (source === 'STATISTICAL_FALLBACK') {
    return `Fallback: ${formatScore(item?.mlScore)}`
  }
  return `ML: ${formatScore(item?.mlScore)}`
}

function mlSourceLabel(item) {
  const source = String(item?.scoringSource || item?.mlAnalysis?.source || '').toUpperCase()
  const algorithm = item?.algorithm || item?.mlAnalysis?.algorithm || item?.modelVersion || ''
  const map = {
    RULE_BASED: 'Nguồn: rule-based, external ML chưa bật.',
    EXTERNAL_ML: `Nguồn: external ML${algorithm ? ` (${algorithm})` : ''}.`,
    STATISTICAL_FALLBACK: `Nguồn: fallback thống kê${algorithm ? ` (${algorithm})` : ''}.`,
    '': item?.modelType === 'RULE_BASED' ? 'Nguồn: rule-based, external ML chưa bật.' : 'Nguồn risk chưa xác định.'
  }
  return map[source] || `Nguồn: ${source.toLowerCase().replaceAll('_', ' ')}${algorithm ? ` (${algorithm})` : ''}.`
}

function formatEvidenceValue(value) {
  if (Array.isArray(value)) return value.join(', ')
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function formatIndicators(values) {
  return Array.isArray(values) && values.length ? values.map(labelSignal).join(', ') : 'Có dấu hiệu cần rà soát'
}

function formatList(values) {
  return Array.isArray(values) && values.length ? values.join(', ') : 'Không có danh sách thí sinh'
}

function formatPercent(value) {
  return `${(Number(value || 0) * 100).toFixed(1)}%`
}

function formatScore(value) {
  return Number(value || 0).toFixed(1)
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('vi-VN')
}
</script>

<style scoped>
.fraud-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.25rem;
  color: var(--ds-text, #172033);
}

.fraud-panel__header,
.fraud-panel__actions,
.fraud-panel__title-wrap,
.fraud-panel__tabs,
.fraud-panel__toolbar,
.fraud-panel__warning-head,
.fraud-panel__review {
  display: flex;
  align-items: center;
}

.fraud-panel__header {
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.fraud-panel__title-wrap {
  gap: 0.75rem;
  min-width: 260px;
}

.fraud-panel__title-icon {
  width: 2rem;
  height: 2rem;
  color: var(--ds-primary, #2563eb);
  flex: 0 0 auto;
}

.fraud-panel__title {
  margin: 0;
  font-size: 1.125rem;
  line-height: 1.3;
}

.fraud-panel__subtitle,
.fraud-panel__muted,
.fraud-panel__row p,
.fraud-panel__warning p,
.fraud-panel__metric small {
  color: var(--ds-text-muted, #64748b);
}

.fraud-panel__subtitle {
  margin: 0.15rem 0 0;
  font-size: 0.875rem;
}

.fraud-panel__actions,
.fraud-panel__toolbar {
  gap: 0.625rem;
  flex-wrap: wrap;
}

.fraud-panel__select {
  min-height: 2.5rem;
  min-width: 210px;
  border: 1px solid var(--ds-border, #d9e2ef);
  border-radius: 0.5rem;
  background: var(--ds-surface, #fff);
  padding: 0 0.75rem;
  color: inherit;
}

.fraud-panel__select--compact {
  min-width: 150px;
}

.fraud-panel__button {
  min-height: 2.5rem;
  border: 1px solid transparent;
  border-radius: 0.5rem;
  padding: 0 0.875rem;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 700;
  cursor: pointer;
}

.fraud-panel__button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.fraud-panel__button svg,
.fraud-panel__tab svg {
  width: 1rem;
  height: 1rem;
}

.fraud-panel__button--primary {
  background: var(--ds-primary, #2563eb);
  color: #fff;
}

.fraud-panel__button--ghost {
  background: var(--ds-surface, #fff);
  border-color: var(--ds-border, #d9e2ef);
  color: var(--ds-text, #172033);
}

.fraud-panel__summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.75rem;
}

.fraud-panel__metric,
.fraud-panel__card,
.fraud-panel__warning,
.fraud-panel__stat {
  border: 1px solid var(--ds-border, #d9e2ef);
  border-radius: 0.5rem;
  background: var(--ds-surface, #fff);
}

.fraud-panel__metric {
  text-align: left;
  padding: 1rem;
  cursor: pointer;
}

.fraud-panel__metric strong {
  display: block;
  margin-top: 0.35rem;
  font-size: 1.65rem;
}

.fraud-panel__metric-label {
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--ds-text-muted, #64748b);
}

.fraud-panel__tabs {
  gap: 0.4rem;
  overflow-x: auto;
  padding-bottom: 0.25rem;
}

.fraud-panel__tab {
  min-height: 2.25rem;
  border: 1px solid var(--ds-border, #d9e2ef);
  border-radius: 0.5rem;
  background: var(--ds-surface, #fff);
  color: var(--ds-text-muted, #64748b);
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0 0.75rem;
  white-space: nowrap;
  cursor: pointer;
}

.fraud-panel__tab--active {
  border-color: var(--ds-primary, #2563eb);
  background: rgba(37, 99, 235, 0.08);
  color: var(--ds-primary, #2563eb);
}

.fraud-panel__tab em {
  min-width: 1.25rem;
  height: 1.25rem;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.12);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-style: normal;
  font-size: 0.75rem;
}

.fraud-panel__section {
  min-height: 220px;
}

.fraud-panel__grid {
  display: grid;
  gap: 0.875rem;
}

.fraud-panel__grid--two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.fraud-panel__grid--stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.fraud-panel__card,
.fraud-panel__stat {
  padding: 1rem;
}

.fraud-panel__card h3 {
  margin: 0 0 0.875rem;
  font-size: 1rem;
}

.fraud-panel__list {
  display: flex;
  flex-direction: column;
}

.fraud-panel__list--spaced {
  gap: 0.625rem;
}

.fraud-panel__row {
  display: flex;
  justify-content: space-between;
  gap: 0.875rem;
  padding: 0.75rem 0;
  border-top: 1px solid var(--ds-border-subtle, #edf2f7);
}

.fraud-panel__row:first-child {
  border-top: 0;
  padding-top: 0;
}

.fraud-panel__row p {
  margin: 0.25rem 0 0;
  font-size: 0.85rem;
}

.fraud-panel__badge,
.fraud-panel__chip {
  align-self: flex-start;
  border-radius: 999px;
  padding: 0.2rem 0.55rem;
  font-size: 0.75rem;
  font-weight: 800;
  white-space: nowrap;
}

.fraud-panel__badge--danger {
  background: rgba(220, 38, 38, 0.1);
  color: #b91c1c;
}

.fraud-panel__badge--warn {
  background: rgba(217, 119, 6, 0.12);
  color: #b45309;
}

.fraud-panel__badge--neutral,
.fraud-panel__chip {
  background: rgba(100, 116, 139, 0.12);
  color: #475569;
}

.fraud-panel__warning-list {
  display: grid;
  gap: 0.75rem;
  margin-top: 0.875rem;
}

.fraud-panel__warning {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 1rem;
  padding: 1rem;
}

.fraud-panel__warning-head {
  gap: 0.5rem;
  flex-wrap: wrap;
}

.fraud-panel__warning p {
  margin: 0.35rem 0;
  font-size: 0.85rem;
}

.fraud-panel__evidence {
  padding: 0.625rem;
  border-radius: 0.4rem;
  background: var(--ds-surface-muted, #f8fafc);
  font-size: 0.8rem;
  color: var(--ds-text-muted, #64748b);
}

.fraud-panel__review {
  gap: 0.45rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.fraud-panel__review button {
  min-height: 2rem;
  border: 1px solid var(--ds-border, #d9e2ef);
  border-radius: 0.4rem;
  background: #fff;
  padding: 0 0.55rem;
  font-weight: 700;
  cursor: pointer;
}

.fraud-panel__stat span {
  color: var(--ds-text-muted, #64748b);
  font-size: 0.8rem;
  font-weight: 700;
}

.fraud-panel__stat strong {
  display: block;
  margin-top: 0.25rem;
  font-size: 1.35rem;
}

.fraud-panel__card--grading {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

.fraud-panel__grade {
  width: 8rem;
  aspect-ratio: 1;
  border-radius: 50%;
  background: rgba(37, 99, 235, 0.1);
  color: var(--ds-primary, #2563eb);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.fraud-panel__grade strong {
  font-size: 2rem;
}

.fraud-panel__grade-details p {
  margin: 0.35rem 0;
}

.fraud-panel__loading,
.fraud-panel__empty,
.fraud-panel__muted--center,
.fraud-panel__notice {
  display: flex;
  align-items: center;
  justify-content: center;
}

.fraud-panel__loading,
.fraud-panel__empty {
  min-height: 260px;
  flex-direction: column;
  gap: 0.75rem;
  text-align: center;
}

.fraud-panel__empty svg {
  width: 2.5rem;
  height: 2.5rem;
  color: var(--ds-text-muted, #64748b);
}

.fraud-panel__empty h3,
.fraud-panel__empty p {
  margin: 0;
}

.fraud-panel__notice {
  justify-content: flex-start;
  gap: 0.5rem;
  border-radius: 0.5rem;
  padding: 0.75rem;
}

.fraud-panel__notice--error {
  background: rgba(220, 38, 38, 0.08);
  color: #b91c1c;
}

.fraud-panel__spinner {
  width: 2rem;
  height: 2rem;
  border: 3px solid var(--ds-border, #d9e2ef);
  border-top-color: var(--ds-primary, #2563eb);
  border-radius: 50%;
  animation: fraud-spin 0.8s linear infinite;
}

@keyframes fraud-spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 900px) {
  .fraud-panel__summary,
  .fraud-panel__grid--two,
  .fraud-panel__grid--stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .fraud-panel__warning {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .fraud-panel {
    padding: 1rem;
  }

  .fraud-panel__summary,
  .fraud-panel__grid--two,
  .fraud-panel__grid--stats {
    grid-template-columns: 1fr;
  }

  .fraud-panel__actions,
  .fraud-panel__select,
  .fraud-panel__button {
    width: 100%;
  }

  .fraud-panel__card--grading {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
