<template>
  <div class="gf">
    <!-- Header -->
    <div class="gf__header">
      <div class="gf__header-left">
        <LucideIcon name="shield_check" class="gf__header-icon" />
        <div>
          <h3 class="gf__title">Phan Tich Gian Lan &amp; Cham Diem</h3>
          <p class="gf__subtitle">Phan tich nang cao: dao van, thoi gian, thong ke, sinh trac, IP</p>
        </div>
      </div>
      <div class="gf__header-right">
        <select v-model="selectedExamId" class="gf__select" @change="loadExam">
          <option value="">Chon bai thi</option>
          <option v-for="exam in exams" :key="exam.id" :value="exam.id">
            {{ exam.title }}
          </option>
        </select>
        <button type="button" class="gf__btn gf__btn--primary" @click="runFullAnalysis" :disabled="!selectedExamId || loading">
          <LucideIcon name="play" />
          Phan Tich Toan Dien
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
            <span class="gf__card-number">{{ plagiarismReports.length }}</span>
            <span class="gf__card-label">Cap nghi van dao van</span>
          </div>
          <div class="gf__card-badge gf__card-badge--warning" v-if="plagiarismReports.length > 0">
            {{ plagiarismReports.length }}
          </div>
        </div>

        <div class="gf__card gf__card--timing" @click="activeTab = 'timing'">
          <LucideIcon name="timer" />
          <div class="gf__card-content">
            <span class="gf__card-number">{{ timingResults.length }}</span>
            <span class="gf__card-label">Bat thuong thoi gian</span>
          </div>
        </div>

        <div class="gf__card gf__card--statistical" @click="activeTab = 'statistical'">
          <LucideIcon name="bar_chart_2" />
          <div class="gf__card-content">
            <span class="gf__card-number">{{ statisticalResults.length }}</span>
            <span class="gf__card-label">Bat thuong thong ke</span>
          </div>
        </div>

        <div class="gf__card gf__card--biometrics" @click="activeTab = 'biometrics'">
          <LucideIcon name="fingerprint" />
          <div class="gf__card-content">
            <span class="gf__card-number">{{ biometricsAnomalies.length }}</span>
            <span class="gf__card-label">Bat thuong sinh trac</span>
          </div>
        </div>

        <div class="gf__card gf__card--ip" @click="activeTab = 'ip'">
          <LucideIcon name="globe" />
          <div class="gf__card-content">
            <span class="gf__card-number">{{ ipResults.length }}</span>
            <span class="gf__card-label">Bat thuong IP</span>
          </div>
        </div>

        <div class="gf__card gf__card--grading" @click="activeTab = 'grading'">
          <LucideIcon name="award" />
          <div class="gf__card-content">
            <span class="gf__card-number">{{ gradingResult ? gradingResult.finalScore.toFixed(1) : '---' }}</span>
            <span class="gf__card-label">Diem IRT</span>
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
        <div v-if="plagiarismReports.length === 0" class="gf__empty">
          <LucideIcon name="check_circle" />
          <p>Khong phat hien dao van</p>
        </div>
        <div v-else class="gf__plagiarism-list">
          <div v-for="report in plagiarismReports" :key="report.id" class="gf__plagiarism-item">
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
                <span class="gf__metric-label">Do tuong dong</span>
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
                <span>{{ report.commonQuestions }} cau hoi chung</span>
                <span v-if="report.timeCorrelation">Co tuong quan thoi gian</span>
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
          <p>Khong phat hien bat thuong thoi gian</p>
        </div>
        <div v-else class="gf__timing-list">
          <div v-for="(result, idx) in timingResults" :key="result.signalType + '-' + idx" class="gf__timing-item">
            <div class="gf__timing-icon" :class="`gf__timing-icon--${(result.severity || 'low').toLowerCase()}`">
              <LucideIcon name="alert_triangle" />
            </div>
            <div class="gf__timing-content">
              <div class="gf__timing-header">
                <span class="gf__timing-type">{{ result.signalType }}</span>
                <span class="gf__timing-time">{{ formatTime(result.timestampMs) }}</span>
              </div>
              <p class="gf__timing-message">{{ result.evidence?.message || result.signalType }}</p>
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
          <p>Khong phat hien bat thuong thong ke</p>
        </div>
        <div v-else class="gf__statistical-list">
          <div v-for="(result, idx) in statisticalResults" :key="result.signalType + '-' + idx" class="gf__statistical-item">
            <div class="gf__statistical-header">
              <span class="gf__statistical-type">{{ result.signalType }}</span>
              <span
                class="gf__severity-badge"
                :class="`gf__severity-badge--${(result.severity || 'medium').toLowerCase()}`"
              >
                {{ result.severity }}
              </span>
            </div>
            <div class="gf__statistical-body">
              <div v-for="(v, k) in result.evidence" :key="k" class="gf__stat-item">
                <span class="gf__stat-label">{{ k }}</span>
                <span class="gf__stat-value">{{ typeof v === 'number' ? (Number.isInteger(v) ? v : v.toFixed(2)) : v }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab Content: Biometrics -->
      <div v-if="activeTab === 'biometrics'" class="gf__tab-content">
        <div v-if="!biometricsData" class="gf__empty">
          <LucideIcon name="check_circle" />
          <p>Khong co du lieu sinh trac</p>
        </div>
        <div v-else class="gf__biometrics-grid">
          <!-- Typing Profile -->
          <div class="gf__bio-card">
            <h4 class="gf__bio-title">
              <LucideIcon name="keyboard" /> Keystroke Dynamics
            </h4>
            <div class="gf__bio-metrics">
              <div class="gf__bio-metric">
                <span class="gf__bio-label">Toc do go</span>
                <span class="gf__bio-value">{{ biometricsData.typingProfile?.avgSpeedCpm?.toFixed(0) || 0 }} cpm</span>
              </div>
              <div class="gf__bio-metric">
                <span class="gf__bio-label">Dwell time TB</span>
                <span class="gf__bio-value">{{ biometricsData.typingProfile?.avgDwellTime?.toFixed(0) || 0 }} ms</span>
              </div>
              <div class="gf__bio-metric">
                <span class="gf__bio-label">Flight time TB</span>
                <span class="gf__bio-value">{{ biometricsData.typingProfile?.avgFlightTimeMs?.toFixed(0) || 0 }} ms</span>
              </div>
              <div class="gf__bio-metric">
                <span class="gf__bio-label">Do nhat quan</span>
                <span
                  class="gf__bio-value"
                  :class="{
                    'gf__bio-value--danger': biometricsData.typingProfile?.consistencyScore < 0.5,
                    'gf__bio-value--warning': biometricsData.typingProfile?.consistencyScore < 0.7
                  }"
                >
                  {{ (((biometricsData.typingProfile?.consistencyScore || 0) * 100).toFixed(0) }}%
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
                <span class="gf__bio-label">Toc do TB</span>
                <span class="gf__bio-value">{{ biometricsData.mouseProfile?.avgSpeedPps?.toFixed(0) || 0 }} pps</span>
              </div>
              <div class="gf__bio-metric">
                <span class="gf__bio-label">So lan di chuyen</span>
                <span class="gf__bio-value">{{ biometricsData.mouseProfile?.totalMovements || 0 }}</span>
              </div>
            </div>
          </div>
          <!-- Anomalies -->
          <div class="gf__bio-card gf__bio-card--anomalies">
            <h4 class="gf__bio-title">
              <LucideIcon name="alert_triangle" /> Anomalies
            </h4>
            <div class="gf__bio-anomalies">
              <div v-for="anomaly in biometricsAnomalies" :key="anomaly" class="gf__bio-anomaly">
                {{ anomaly }}
              </div>
              <div v-if="!biometricsAnomalies.length" class="gf__bio-no-anomaly">
                Khong phat hien bat thuong
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab Content: IP Reputation -->
      <div v-if="activeTab === 'ip'" class="gf__tab-content">
        <div v-if="ipResults.length === 0" class="gf__empty">
          <LucideIcon name="check_circle" />
          <p>Khong phat hien bat thuong IP</p>
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
              <span v-if="result.subnetCount">Subnet: {{ result.subnetCount }} nguoi</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab Content: Grading -->
      <div v-if="activeTab === 'grading'" class="gf__tab-content">
        <div v-if="!gradingResult" class="gf__empty">
          <LucideIcon name="info" />
          <p>Chua co ket qua cham diem</p>
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
                <span>Diem thuan</span>
                <span>{{ gradingResult.rawScore.toFixed(1) }}</span>
              </div>
              <div class="gf__grade-item" v-if="gradingResult.irtResult">
                <span>Diem IRT</span>
                <span>{{ gradingResult.irtResult.irtScore.toFixed(1) }}</span>
              </div>
              <div class="gf__grade-item" v-if="gradingResult.peerResult">
                <span>Percentile</span>
                <span>{{ gradingResult.peerResult.percentile.toFixed(0) }}%</span>
              </div>
              <div class="gf__grade-item" v-if="gradingResult.peerResult">
                <span>Xep hang</span>
                <span>{{ gradingResult.peerResult.rank }} / {{ gradingResult.peerResult.totalPeers }}</span>
              </div>
            </div>
          </div>

          <!-- Question Analysis -->
          <div class="gf__grade-card" v-if="gradingResult.questionAnalyses?.length">
            <h4 class="gf__grade-card-title">Phan Tich Cau Hoi</h4>
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
                <span class="gf__question-quality" :class="`gf__question-quality--${(qa.quality || 'medium').toLowerCase()}`">
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
      <h3>Chon bai thi de bat dau phan tich</h3>
      <p>He thong se phan tich dao van, thoi gian, thong ke, sinh trac, va IP reputation</p>
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
  runGradingByExam,
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

const plagiarismReports = ref([])
const timingResults = ref([])
const statisticalResults = ref([])
const biometricsData = ref(null)
const biometricsAnomalies = computed(() => biometricsData.value?.anomalies || [])
const ipResults = ref([])
const gradingResult = ref(null)

const hasResults = computed(() =>
  plagiarismReports.value.length > 0 ||
  timingResults.value.length > 0 ||
  statisticalResults.value.length > 0 ||
  biometricsData.value !== null ||
  ipResults.value.length > 0 ||
  gradingResult.value !== null
)

const tabs = computed(() => [
  { id: 'plagiarism', label: 'Dao Van', icon: 'file_text', count: plagiarismReports.value.length },
  { id: 'timing', label: 'Thoi Gian', icon: 'timer', count: timingResults.value.length },
  { id: 'statistical', label: 'Thong Ke', icon: 'bar_chart_2', count: statisticalResults.value.length },
  { id: 'biometrics', label: 'Sinh Trac', icon: 'fingerprint', count: biometricsAnomalies.value.length },
  { id: 'ip', label: 'IP', icon: 'globe', count: ipResults.value.length },
  { id: 'grading', label: 'Cham Diem', icon: 'award', count: gradingResult.value ? 1 : 0 }
])

const gradeCircleClass = computed(() => {
  const score = gradingResult.value?.finalScore || 0
  const max = gradingResult.value?.maxScore || 100
  const pct = max > 0 ? score / max : 0
  if (pct >= 0.8) return 'gf__grade-circle--green'
  if (pct >= 0.6) return 'gf__grade-circle--yellow'
  return 'gf__grade-circle--red'
})

onMounted(async () => {
  exams.value = await listExams()
})

async function loadExam() {
  if (!selectedExamId.value) return
  plagiarismReports.value = []
  timingResults.value = []
  statisticalResults.value = []
  biometricsData.value = null
  ipResults.value = []
  gradingResult.value = null
}

async function runFullAnalysis() {
  if (!selectedExamId.value) return

  loading.value = true
  plagiarismReports.value = []
  timingResults.value = []
  statisticalResults.value = []
  biometricsData.value = null
  ipResults.value = []
  gradingResult.value = null

  try {
    loadingMessage.value = 'Dang phan tich dao van...'
    const plagiarismResp = await runPlagiarismAnalysis(selectedExamId.value)
    plagiarismReports.value = plagiarismResp?.plagiarismReports || []

    loadingMessage.value = 'Dang phan tich thoi gian...'
    const timingResp = await runExamTimingAnalysis(selectedExamId.value)
    timingResults.value = timingResp?.timingResults || []

    loadingMessage.value = 'Dang phan tich thong ke...'
    const statResp = await runStatisticalAnalysis(selectedExamId.value)
    statisticalResults.value = statResp?.statisticalResults || []

    loadingMessage.value = 'Dang phan tich sinh trac...'
    const bioResp = await runExamBiometricsAnalysis(selectedExamId.value)
    biometricsData.value = bioResp || null

    loadingMessage.value = 'Dang phan tich IP...'
    const ipResp = await runIpReputationAnalysis(selectedExamId.value)
    ipResults.value = ipResp?.ipResults || []

    loadingMessage.value = 'Dang tai ket qua cham diem...'
    const gradeResp = await runGradingByExam(selectedExamId.value)
    gradingResult.value = gradeResp || null

    loadingMessage.value = 'Hoan thanh!'
  } catch (error) {
    console.error('Analysis failed:', error)
    loadingMessage.value = 'Loi phan tich: ' + (error?.message || 'Unknown error')
  } finally {
    loading.value = false
  }
}

function formatTime(timestampMs) {
  if (!timestampMs) return ''
  const d = new Date(timestampMs)
  return d.toLocaleString('vi-VN')
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

.gf__plagiarism-meta {
  display: flex;
  gap: 1rem;
  font-size: 0.875rem;
  color: var(--color-text-muted, #6b7280);
  margin-bottom: 0.5rem;
}

.gf__plagiarism-recommendation {
  font-size: 0.875rem;
  color: var(--color-text, #374151);
  margin: 0;
}

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

/* Statistical */
.gf__statistical-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.gf__statistical-item {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 0.5rem;
  overflow: hidden;
}

.gf__statistical-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  background: var(--color-bg-subtle, #f9fafb);
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}

.gf__statistical-type { font-weight: 600; font-size: 0.875rem; }

.gf__statistical-body {
  padding: 0.75rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.gf__stat-item {
  display: flex;
  justify-content: space-between;
  font-size: 0.875rem;
}

.gf__stat-label { color: var(--color-text-muted, #6b7280); }
.gf__stat-value { font-weight: 500; }

/* Biometrics */
.gf__biometrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1rem;
}

.gf__bio-card {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 0.75rem;
  padding: 1rem;
  background: var(--color-surface, #fff);
}

.gf__bio-title {
  font-size: 0.875rem;
  font-weight: 600;
  margin: 0 0 0.75rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.gf__bio-metrics {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.gf__bio-metric {
  display: flex;
  justify-content: space-between;
  font-size: 0.875rem;
}

.gf__bio-label { color: var(--color-text-muted, #6b7280); }
.gf__bio-value { font-weight: 500; }
.gf__bio-value--danger { color: #dc2626; }
.gf__bio-value--warning { color: #d97706; }

.gf__bio-anomalies {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.gf__bio-anomaly {
  padding: 0.375rem 0.75rem;
  background: #fef3c7;
  color: #92400e;
  border-radius: 0.25rem;
  font-size: 0.875rem;
  font-weight: 500;
}

.gf__bio-no-anomaly {
  color: var(--color-text-muted, #6b7280);
  font-size: 0.875rem;
  text-align: center;
  padding: 1rem;
}

/* IP */
.gf__ip-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.gf__ip-item {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 0.5rem;
  padding: 0.75rem 1rem;
}

.gf__ip-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.gf__ip-address {
  font-family: monospace;
  font-size: 0.875rem;
  font-weight: 600;
}

.gf__ip-badge {
  padding: 0.125rem 0.5rem;
  border-radius: 0.25rem;
  font-size: 0.75rem;
  font-weight: 600;
}

.gf__ip-badge--vpn { background: #fee2e2; color: #991b1b; }
.gf__ip-badge--proxy { background: #fef3c7; color: #92400e; }
.gf__ip-badge--tor { background: #ede9fe; color: #5b21b6; }

.gf__ip-body {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-size: 0.875rem;
  color: var(--color-text-muted, #6b7280);
}

/* Grading */
.gf__grading-grid {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 1.5rem;
}

.gf__grade-card {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 0.75rem;
  padding: 1.5rem;
}

.gf__grade-card--main {}

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

.gf__grade-breakdown {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 1rem;
}

.gf__grade-item {
  display: flex;
  justify-content: space-between;
  font-size: 0.875rem;
}

.gf__grade-breakdown .gf__grade-item span:first-child { color: var(--color-text-muted, #6b7280); }

.gf__grade-card-title {
  font-size: 1rem;
  font-weight: 600;
  margin: 0 0 1rem 0;
}

.gf__question-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 400px;
  overflow-y: auto;
}

.gf__question-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem;
  border-radius: 0.375rem;
  background: var(--color-bg-subtle, #f9fafb);
}

.gf__question-difficulty {
  padding: 0.125rem 0.5rem;
  border-radius: 0.25rem;
  font-size: 0.75rem;
  font-weight: 600;
  min-width: 40px;
  text-align: center;
}

.gf__question-difficulty--easy { background: #dcfce7; color: #166534; }
.gf__question-difficulty--medium { background: #fef3c7; color: #92400e; }
.gf__question-difficulty--hard { background: #fee2e2; color: #991b1b; }

.gf__question-content {
  flex: 1;
  font-size: 0.875rem;
  color: var(--color-text, #374151);
}

.gf__question-quality {
  padding: 0.125rem 0.5rem;
  border-radius: 0.25rem;
  font-size: 0.75rem;
  font-weight: 600;
}

.gf__question-quality--easy { background: #dcfce7; color: #166534; }
.gf__question-quality--medium { background: #fef3c7; color: #92400e; }
.gf__question-quality--hard { background: #fee2e2; color: #991b1b; }

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
