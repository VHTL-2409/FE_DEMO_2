<template>
  <div class="gf">
    <!-- Header -->
    <div class="gf__header">
      <div class="gf__header-left">
        <LucideIcon name="shield_check" class="gf__header-icon" />
        <div>
          <h3 class="gf__title">Phân Tích Gian Lận &amp; Chấm Điểm</h3>
          <p class="gf__subtitle">Phân tích nâng cao: đạo văn, thời gian, thống kê, sinh trắc, IP</p>
        </div>
      </div>
      <div class="gf__header-right">
        <select v-model="selectedExamId" class="gf__select" @change="loadExam">
          <option value="">Chọn bài thi</option>
          <option v-for="exam in exams" :key="exam.id" :value="exam.id">
            {{ exam.title }}
          </option>
        </select>
        <button type="button" class="gf__btn gf__btn--primary" @click="runFullAnalysis" :disabled="!selectedExamId || loading">
          <LucideIcon name="play" />
          Phân Tích Toàn Diện
        </button>
      </div>
    </div>

    <!-- Loading state -->
    <div v-if="loading" class="gf__loading">
      <div class="gf__spinner" />
      <p>{{ loadingMessage }}</p>
    </div>

    <!-- Results -->
    <div v-else-if="hasResults" class="gf__results">

      <!-- Summary Cards -->
      <div class="gf__summary-cards">
        <div class="gf__card gf__card--plagiarism" @click="activeTab = 'plagiarism'">
          <LucideIcon name="file_text" />
          <div class="gf__card-content">
            <span class="gf__card-number">{{ plagiarismResults.length }}</span>
            <span class="gf__card-label">Cặp nghi vấn đạo văn</span>
          </div>
          <div class="gf__card-badge gf__card-badge--warning" v-if="plagiarismResults.length > 0">
            {{ plagiarismResults.length }}
          </div>
        </div>

        <div class="gf__card gf__card--timing" @click="activeTab = 'timing'">
          <LucideIcon name="timer" />
          <div class="gf__card-content">
            <span class="gf__card-number">{{ timingResults.length }}</span>
            <span class="gf__card-label">Bất thường thời gian</span>
          </div>
        </div>

        <div class="gf__card gf__card--statistical" @click="activeTab = 'statistical'">
          <LucideIcon name="bar_chart_2" />
          <div class="gf__card-content">
            <span class="gf__card-number">{{ statisticalResults.length }}</span>
            <span class="gf__card-label">Bất thường thống kê</span>
          </div>
        </div>

        <div class="gf__card gf__card--biometrics" @click="activeTab = 'biometrics'">
          <LucideIcon name="fingerprint" />
          <div class="gf__card-content">
            <span class="gf__card-number">{{ biometricsResults.length }}</span>
            <span class="gf__card-label">Bất thường sinh trắc</span>
          </div>
        </div>

        <div class="gf__card gf__card--ip" @click="activeTab = 'ip'">
          <LucideIcon name="globe" />
          <div class="gf__card-content">
            <span class="gf__card-number">{{ ipResults.length }}</span>
            <span class="gf__card-label">Bất thường IP</span>
          </div>
        </div>

        <div class="gf__card gf__card--grading">
          <LucideIcon name="award" />
          <div class="gf__card-content">
            <span class="gf__card-number">{{ gradingResult ? gradingResult.finalScore.toFixed(1) : '—' }}</span>
            <span class="gf__card-label">Điểm IRT</span>
          </div>
        </div>
      </div>

      <!-- Tab Navigation -->
      <div class="gf__tabs">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          type="button"
          class="gf__tab"
          :class="{ 'gf__tab--active': activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          <LucideIcon :name="tab.icon" />
          {{ tab.label }}
          <span class="gf__tab-count" v-if="tab.count > 0">{{ tab.count }}</span>
        </button>
      </div>

      <!-- Tab Content: Plagiarism -->
      <div v-if="activeTab === 'plagiarism'" class="gf__tab-content">
        <div v-if="plagiarismResults.length === 0" class="gf__empty">
          <LucideIcon name="check_circle" />
          <p>Không phát hiện đạo văn</p>
        </div>
        <div v-else class="gf__plagiarism-list">
          <div v-for="report in plagiarismResults" :key="report.id" class="gf__plagiarism-item">
            <div class="gf__plagiarism-header">
              <div class="gf__plagiarism-pair">
                <span class="gf__student-tag">{{ report.student1Name }}</span>
                <LucideIcon name="arrow_right" />
                <span class="gf__student-tag">{{ report.student2Name }}</span>
              </div>
              <span
                class="gf__severity-badge"
                :class="{
                  'gf__severity-badge--critical': report.verdict === 'NGHIEM_TRONG',
                  'gf__severity-badge--high': report.verdict === 'CAO',
                  'gf__severity-badge--medium': report.verdict === 'TRUNG_BINH'
                }"
              >
                {{ report.verdict }}
              </span>
            </div>
            <div class="gf__plagiarism-body">
              <div class="gf__plagiarism-metric">
                <span class="gf__metric-label">Độ tương đồng</span>
                <div class="gf__progress-bar">
                  <div
                    class="gf__progress-fill"
                    :class="{
                      'gf__progress-fill--critical': report.similarityScore > 0.95,
                      'gf__progress-fill--high': report.similarityScore > 0.85,
                      'gf__progress-fill--warning': report.similarityScore > 0.75
                    }"
                    :style="{ width: (report.similarityScore * 100) + '%' }"
                  />
                </div>
                <span class="gf__metric-value">{{ (report.similarityScore * 100).toFixed(1) }}%</span>
              </div>
              <div class="gf__plagiarism-meta">
                <span>{{ report.commonQuestions }} câu hỏi chung</span>
                <span v-if="report.timeCorrelation">Có tương quan thời gian</span>
              </div>
              <p class="gf__plagiarism-recommendation">{{ report.recommendation }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab Content: Timing -->
      <div v-if="activeTab === 'timing'" class="gf__tab-content">
        <div v-if="timingResults.length === 0" class="gf__empty">
          <LucideIcon name="check_circle" />
          <p>Không phát hiện bất thường thời gian</p>
        </div>
        <div v-else class="gf__timing-list">
          <div v-for="result in timingResults" :key="result.signalType + result.timestamp" class="gf__timing-item">
            <div class="gf__timing-icon" :class="`gf__timing-icon--${result.severity.toLowerCase()}`">
              <LucideIcon name="alert_triangle" />
            </div>
            <div class="gf__timing-content">
              <div class="gf__timing-header">
                <span class="gf__timing-type">{{ result.signalType }}</span>
                <span class="gf__timing-time">{{ formatTime(result.timestampMs) }}</span>
              </div>
              <p class="gf__timing-message">{{ result.evidence.message }}</p>
              <div class="gf__timing-details">
                <span v-for="(v, k) in result.evidence" :key="k" class="gf__timing-detail">
                  {{ k }}: {{ v }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab Content: Statistical -->
      <div v-if="activeTab === 'statistical'" class="gf__tab-content">
        <div v-if="statisticalResults.length === 0" class="gf__empty">
          <LucideIcon name="check_circle" />
          <p>Không phát hiện bất thường thống kê</p>
        </div>
        <div v-else class="gf__statistical-list">
          <div v-for="result in statisticalResults" :key="result.signalType" class="gf__statistical-item">
            <div class="gf__statistical-header">
              <span class="gf__statistical-type">{{ result.signalType }}</span>
              <span
                class="gf__severity-badge"
                :class="`gf__severity-badge--${result.severity.toLowerCase()}`"
              >
                {{ result.severity }}
              </span>
            </div>
            <div class="gf__statistical-body">
              <div v-for="(v, k) in result.evidence" :key="k" class="gf__stat-item">
                <span class="gf__stat-label">{{ k }}</span>
                <span class="gf__stat-value">{{ typeof v === 'number' ? v.toFixed(2) : v }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab Content: Biometrics -->
      <div v-if="activeTab === 'biometrics'" class="gf__tab-content">
        <div v-if="!biometricsResults" class="gf__empty">
          <LucideIcon name="check_circle" />
          <p>Không có dữ liệu sinh trắc</p>
        </div>
        <div v-else class="gf__biometrics-grid">
          <!-- Typing Profile -->
          <div class="gf__bio-card">
            <h4 class="gf__bio-title">
              <LucideIcon name="keyboard" /> Keystroke Dynamics
            </h4>
            <div class="gf__bio-metrics">
              <div class="gf__bio-metric">
                <span class="gf__bio-label">Tốc độ gõ</span>
                <span class="gf__bio-value">{{ biometricsResults.typingProfile?.avgSpeedCpm?.toFixed(0) || 0 }} cpm</span>
              </div>
              <div class="gf__bio-metric">
                <span class="gf__bio-label">Dwell time TB</span>
                <span class="gf__bio-value">{{ biometricsResults.typingProfile?.avgDwellTime?.toFixed(0) || 0 }} ms</span>
              </div>
              <div class="gf__bio-metric">
                <span class="gf__bio-label">Flight time TB</span>
                <span class="gf__bio-value">{{ biometricsResults.typingProfile?.avgFlightTimeMs?.toFixed(0) || 0 }} ms</span>
              </div>
              <div class="gf__bio-metric">
                <span class="gf__bio-label">Độ nhất quán</span>
                <span
                  class="gf__bio-value"
                  :class="{
                    'gf__bio-value--danger': biometricsResults.typingProfile?.consistencyScore < 0.5,
                    'gf__bio-value--warning': biometricsResults.typingProfile?.consistencyScore < 0.7
                  }"
                >
                  {{ ((biometricsResults.typingProfile?.consistencyScore || 0) * 100).toFixed(0) }}%
                </span>
              </div>
            </div>
          </div>
          <!-- Mouse Profile -->
          <div class="gf__bio-card">
            <h4 class="gf__bio-title">
              <LucideIcon name="mouse" /> Mouse Patterns
            </h4>
            <div class="gf__bio-metrics">
              <div class="gf__bio-metric">
                <span class="gf__bio-label">Tốc độ TB</span>
                <span class="gf__bio-value">{{ biometricsResults.mouseProfile?.avgSpeedPps?.toFixed(0) || 0 }} pps</span>
              </div>
              <div class="gf__bio-metric">
                <span class="gf__bio-label">Số lần di chuyển</span>
                <span class="gf__bio-value">{{ biometricsResults.mouseProfile?.totalMovements || 0 }}</span>
              </div>
            </div>
          </div>
          <!-- Anomalies -->
          <div class="gf__bio-card gf__bio-card--anomalies">
            <h4 class="gf__bio-title">
              <LucideIcon name="alert_triangle" /> Anomalies
            </h4>
            <div class="gf__bio-anomalies">
              <div v-for="anomaly in biometricsResults.anomalies" :key="anomaly" class="gf__bio-anomaly">
                {{ anomaly }}
              </div>
              <div v-if="!biometricsResults.anomalies?.length" class="gf__bio-no-anomaly">
                Không phát hiện bất thường
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab Content: IP Reputation -->
      <div v-if="activeTab === 'ip'" class="gf__tab-content">
        <div v-if="ipResults.length === 0" class="gf__empty">
          <LucideIcon name="check_circle" />
          <p>Không phát hiện bất thường IP</p>
        </div>
        <div v-else class="gf__ip-list">
          <div v-for="result in ipResults" :key="result.ipAddress" class="gf__ip-item">
            <div class="gf__ip-header">
              <span class="gf__ip-address">{{ result.ipAddress }}</span>
              <span v-if="result.isVpn" class="gf__ip-badge gf__ip-badge--vpn">VPN</span>
              <span v-if="result.isProxy" class="gf__ip-badge gf__ip-badge--proxy">Proxy</span>
              <span v-if="result.isTor" class="gf__ip-badge gf__ip-badge--tor">Tor</span>
            </div>
            <div class="gf__ip-body">
              <span v-if="result.hostname">Hostname: {{ result.hostname }}</span>
              <span v-if="result.geoLocation">{{ result.geoLocation.city }}, {{ result.geoLocation.country }}</span>
              <span v-if="result.subnetCount">Subnet: {{ result.subnetCount }} người</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab Content: Grading -->
      <div v-if="activeTab === 'grading'" class="gf__tab-content">
        <div v-if="!gradingResult" class="gf__empty">
          <LucideIcon name="info" />
          <p>Chưa có kết quả chấm điểm</p>
        </div>
        <div v-else class="gf__grading-grid">
          <!-- Score Summary -->
          <div class="gf__grade-card gf__grade-card--main">
            <div class="gf__grade-circle" :class="gradeCircleClass">
              <span class="gf__grade-score">{{ gradingResult.finalScore.toFixed(1) }}</span>
              <span class="gf__grade-max">/ {{ gradingResult.maxScore }}</span>
            </div>
            <div class="gf__grade-breakdown">
              <div class="gf__grade-item">
                <span>Điểm thuần</span>
                <span>{{ gradingResult.rawScore.toFixed(1) }}</span>
              </div>
              <div class="gf__grade-item" v-if="gradingResult.irtResult">
                <span>Điểm IRT</span>
                <span>{{ gradingResult.irtResult.irtScore.toFixed(1) }}</span>
              </div>
              <div class="gf__grade-item" v-if="gradingResult.peerResult">
                <span>Percentile</span>
                <span>{{ gradingResult.peerResult.percentile.toFixed(0) }}%</span>
              </div>
              <div class="gf__grade-item" v-if="gradingResult.peerResult">
                <span>Xếp hạng</span>
                <span>{{ gradingResult.peerResult.rank }} / {{ gradingResult.peerResult.totalPeers }}</span>
              </div>
            </div>
          </div>

          <!-- Question Analysis -->
          <div class="gf__grade-card">
            <h4 class="gf__grade-card-title">Phân Tích Câu Hỏi</h4>
            <div class="gf__question-list">
              <div
                v-for="qa in gradingResult.questionAnalyses"
                :key="qa.questionId"
                class="gf__question-item"
              >
                <span class="gf__question-difficulty" :class="`gf__question-difficulty--${qa.difficulty > 0.7 ? 'easy' : qa.difficulty > 0.3 ? 'medium' : 'hard'}`">
                  {{ (qa.difficulty * 100).toFixed(0) }}%
                </span>
                <span class="gf__question-content">{{ qa.content?.substring(0, 60) }}...</span>
                <span class="gf__question-quality" :class="`gf__question-quality--${qa.quality.toLowerCase()}`">
                  {{ qa.quality }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty state -->
    <div v-else class="gf__empty-state">
      <LucideIcon name="search" />
      <h3>Chọn bài thi để bắt đầu phân tích</h3>
      <p>Hệ thống sẽ phân tích đạo văn, thời gian, thống kê, sinh trắc, và IP reputation</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { listExams } from '../../../services/examService'
import {
  runPlagiarismAnalysis,
  runExamTimingAnalysis,
  runStatisticalAnalysis,
  runExamBiometricsAnalysis,
  runIpReputationAnalysis,
} from '../../../services/fraudAnalysisService'

const props = defineProps({
  initialExamId: {
    type: [Number, String],
    default: null
  }
})

const selectedExamId = ref(props.initialExamId ? String(props.initialExamId) : '')
const activeTab = ref('plagiarism')
const loading = ref(false)
const loadingMessage = ref('')
const exams = ref([])

const plagiarismResults = ref([])
const timingResults = ref([])
const statisticalResults = ref([])
const biometricsResults = ref(null)
const ipResults = ref([])
const gradingResult = ref(null)

const hasResults = computed(() =>
  plagiarismResults.value.length > 0 ||
  timingResults.value.length > 0 ||
  statisticalResults.value.length > 0 ||
  biometricsResults.value ||
  ipResults.value.length > 0 ||
  gradingResult.value
)

const tabs = computed(() => [
  { id: 'plagiarism', label: 'Đạo Văn', icon: 'file_text', count: plagiarismResults.value.length },
  { id: 'timing', label: 'Thời Gian', icon: 'timer', count: timingResults.value.length },
  { id: 'statistical', label: 'Thống Kê', icon: 'bar_chart_2', count: statisticalResults.value.length },
  { id: 'biometrics', label: 'Sinh Trắc', icon: 'fingerprint', count: biometricsResults.value?.anomalies?.length || 0 },
  { id: 'ip', label: 'IP', icon: 'globe', count: ipResults.value.length },
  { id: 'grading', label: 'Chấm Điểm', icon: 'award', count: gradingResult.value ? 1 : 0 }
])

const gradeCircleClass = computed(() => {
  const score = gradingResult.value?.finalScore || 0
  const max = gradingResult.value?.maxScore || 100
  const pct = score / max
  if (pct >= 0.8) return 'gf__grade-circle--green'
  if (pct >= 0.6) return 'gf__grade-circle--yellow'
  return 'gf__grade-circle--red'
})

onMounted(async () => {
  exams.value = await listExams()
})

async function runFullAnalysis() {
  if (!selectedExamId.value) return

  loading.value = true
  plagiarismResults.value = []
  timingResults.value = []
  statisticalResults.value = []
  biometricsResults.value = null
  ipResults.value = []

  try {
    loadingMessage.value = 'Đang phân tích đạo văn...'
    plagiarismResults.value = await runPlagiarismAnalysis(selectedExamId.value)

    loadingMessage.value = 'Đang phân tích thời gian...'
    timingResults.value = await runExamTimingAnalysis(selectedExamId.value)

    loadingMessage.value = 'Đang phân tích thống kê...'
    statisticalResults.value = await runStatisticalAnalysis(selectedExamId.value)

    loadingMessage.value = 'Đang phân tích sinh trắc...'
    biometricsResults.value = await runExamBiometricsAnalysis(selectedExamId.value)

    loadingMessage.value = 'Đang phân tích IP...'
    ipResults.value = await runIpReputationAnalysis(selectedExamId.value)

    loadingMessage.value = 'Hoàn thành!'
  } catch (error) {
    console.error('Analysis failed:', error)
  } finally {
    loading.value = false
    loadingMessage.value = ''
  }
}

function formatTime(timestampMs) {
  if (!timestampMs) return ''
  return new Date(timestampMs).toLocaleString('vi-VN')
}
</script>

<style scoped>
.gf {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 1.5rem;
  height: 100%;
  overflow-y: auto;
}

.gf__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
}

.gf__header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.gf__header-icon {
  width: 32px;
  height: 32px;
  color: var(--color-primary, #3b82f6);
}

.gf__title {
  font-size: 1.25rem;
  font-weight: 700;
  margin: 0;
}

.gf__subtitle {
  font-size: 0.875rem;
  color: var(--color-text-muted, #6b7280);
  margin: 0;
}

.gf__header-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.gf__select {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 0.5rem;
  font-size: 0.875rem;
  background: var(--color-surface, #fff);
}

.gf__btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: opacity 0.2s;
}

.gf__btn--primary {
  background: var(--color-primary, #3b82f6);
  color: #fff;
}

.gf__btn--primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Summary Cards */
.gf__summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 0.75rem;
}

.gf__card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  border-radius: 0.75rem;
  background: var(--color-surface, #fff);
  border: 1px solid var(--color-border, #e5e7eb);
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}

.gf__card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  transform: translateY(-1px);
}

.gf__card-content {
  display: flex;
  flex-direction: column;
}

.gf__card-number {
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1;
}

.gf__card-label {
  font-size: 0.75rem;
  color: var(--color-text-muted, #6b7280);
  margin-top: 0.25rem;
}

.gf__card-badge {
  margin-left: auto;
  padding: 0.125rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.gf__card-badge--warning {
  background: #fef3c7;
  color: #92400e;
}

/* Tabs */
.gf__tabs {
  display: flex;
  gap: 0.5rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  padding-bottom: 0.5rem;
}

.gf__tab {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border: none;
  background: none;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  color: var(--color-text-muted, #6b7280);
  transition: background 0.2s, color 0.2s;
}

.gf__tab:hover {
  background: var(--color-bg-subtle, #f3f4f6);
}

.gf__tab--active {
  background: var(--color-primary, #3b82f6);
  color: #fff;
}

.gf__tab-count {
  padding: 0 0.375rem;
  background: rgba(255,255,255,0.2);
  border-radius: 9999px;
  font-size: 0.75rem;
}

/* Tab Content */
.gf__tab-content {
  padding: 1rem 0;
}

/* Plagiarism */
.gf__plagiarism-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.gf__plagiarism-item {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 0.75rem;
  overflow: hidden;
}

.gf__plagiarism-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  background: var(--color-bg-subtle, #f9fafb);
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}

.gf__plagiarism-pair {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.gf__student-tag {
  padding: 0.25rem 0.75rem;
  background: var(--color-primary, #3b82f6);
  color: #fff;
  border-radius: 9999px;
  font-size: 0.875rem;
  font-weight: 500;
}

.gf__severity-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.gf__severity-badge--critical { background: #fee2e2; color: #991b1b; }
.gf__severity-badge--high { background: #fed7aa; color: #9a3412; }
.gf__severity-badge--medium { background: #fef3c7; color: #92400e; }

.gf__plagiarism-body {
  padding: 1rem;
}

.gf__plagiarism-metric {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.gf__progress-bar {
  flex: 1;
  height: 8px;
  background: #e5e7eb;
  border-radius: 9999px;
  overflow: hidden;
}

.gf__progress-fill {
  height: 100%;
  border-radius: 9999px;
  background: #3b82f6;
  transition: width 0.3s;
}

.gf__progress-fill--critical { background: #dc2626; }
.gf__progress-fill--high { background: #ea580c; }
.gf__progress-fill--warning { background: #d97706; }

/* Timing */
.gf__timing-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.gf__timing-item {
  display: flex;
  gap: 0.75rem;
  padding: 0.75rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 0.5rem;
}

.gf__timing-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.gf__timing-icon--high, .gf__timing-icon--critical { background: #fee2e2; color: #dc2626; }
.gf__timing-icon--medium { background: #fef3c7; color: #d97706; }
.gf__timing-icon--low { background: #f3f4f6; color: #6b7280; }

.gf__timing-content { flex: 1; }
.gf__timing-header { display: flex; justify-content: space-between; margin-bottom: 0.25rem; }
.gf__timing-type { font-weight: 600; font-size: 0.875rem; }
.gf__timing-time { font-size: 0.75rem; color: var(--color-text-muted, #6b7280); }
.gf__timing-message { font-size: 0.875rem; margin: 0.25rem 0; }
.gf__timing-details { display: flex; flex-wrap: wrap; gap: 0.5rem; margin-top: 0.5rem; }
.gf__timing-detail {
  padding: 0.125rem 0.5rem;
  background: var(--color-bg-subtle, #f3f4f6);
  border-radius: 0.25rem;
  font-size: 0.75rem;
  font-family: monospace;
}

/* Grading */
.gf__grading-grid {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 1.5rem;
}

.gf__grade-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  font-weight: 700;
}

.gf__grade-circle--green { background: #dcfce7; color: #166534; }
.gf__grade-circle--yellow { background: #fef3c7; color: #92400e; }
.gf__grade-circle--red { background: #fee2e2; color: #991b1b; }

.gf__grade-max { font-size: 0.875rem; font-weight: 400; }

.gf__grade-breakdown { display: flex; flex-direction: column; gap: 0.5rem; margin-top: 1rem; }
.gf__grade-item { display: flex; justify-content: space-between; font-size: 0.875rem; }

/* Empty states */
.gf__empty, .gf__empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 3rem;
  color: var(--color-text-muted, #6b7280);
  text-align: center;
}

.gf__empty svg, .gf__empty-state svg { width: 48px; height: 48px; opacity: 0.5; }

/* Loading */
.gf__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  padding: 4rem;
}

.gf__spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-border, #e5e7eb);
  border-top-color: var(--color-primary, #3b82f6);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }
</style>
