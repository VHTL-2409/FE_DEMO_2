<template>
  <div class="epp">
    <!-- ── Top navigation bar ── -->
    <div class="epp__nav">
      <button type="button" class="epp__back-btn" @click="$router.back()">
        <LucideIcon name="arrow_back" size="16" />
        Quay lại
      </button>
      <div class="epp__nav-title">
        <LucideIcon name="preview" size="16" />
        Xem trước đề thi
      </div>
    </div>

    <!-- ── Page header: exam metadata ── -->
    <div class="epp__page-header">
      <div class="epp__meta-grid">
        <div class="epp__meta-item">
          <span class="epp__meta-label">File gốc</span>
          <span class="epp__meta-value">{{ previewData?.originalFileName || sessionId }}</span>
        </div>
        <div class="epp__meta-item" v-if="meta?.title">
          <span class="epp__meta-label">Tiêu đề</span>
          <span class="epp__meta-value">{{ meta.title }}</span>
        </div>
        <div class="epp__meta-item" v-if="meta?.subject">
          <span class="epp__meta-label">Môn học</span>
          <span class="epp__meta-value">{{ meta.subject }}</span>
        </div>
        <div class="epp__meta-item" v-if="meta?.grade">
          <span class="epp__meta-label">Khối lớp</span>
          <span class="epp__meta-value">{{ meta.grade }}</span>
        </div>
        <div class="epp__meta-item" v-if="meta?.duration">
          <span class="epp__meta-label">Thời gian</span>
          <span class="epp__meta-value">{{ meta.duration }}</span>
        </div>
        <div class="epp__meta-item" v-if="report?.parseTimeMs != null">
          <span class="epp__meta-label">Parse time</span>
          <span class="epp__meta-value">{{ report.parseTimeMs }}ms</span>
        </div>
      </div>

      <!-- Chips row -->
      <div class="epp__chips">
        <span v-if="report?.selectedTemplate" class="epp__chip epp__chip--template">
          <LucideIcon name="fingerprint" size="12" />
          {{ templateLabel(report.selectedTemplate) }}
        </span>
        <span v-if="report?.questionCount != null" class="epp__chip epp__chip--count">
          <LucideIcon name="quiz" size="12" />
          {{ report.questionCount }} câu
        </span>
        <span v-if="report?.answerCount != null" class="epp__chip epp__chip--answer">
          <LucideIcon name="check" size="12" />
          {{ report.answerCount }} đáp án
        </span>
        <span v-if="report?.confidence != null" class="epp__chip" :class="confChipClass(report.confidence)">
          <LucideIcon :name="confIcon(report.confidence)" size="12" />
          {{ (report.confidence * 100).toFixed(0) }}%
        </span>
        <span v-if="report?.invalidQuestions?.length" class="epp__chip epp__chip--warn">
          <LucideIcon name="error" size="12" />
          {{ report.invalidQuestions.length }} câu lỗi
        </span>
        <span v-if="report?.warnings?.length" class="epp__chip epp__chip--warn">
          <LucideIcon name="warning" size="12" />
          {{ report.warnings.length }} cảnh báo
        </span>
      </div>
    </div>

    <!-- ── Tab navigation ── -->
    <div class="epp__tabs">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        type="button"
        class="epp__tab"
        :class="{ 'epp__tab--active': activeTab === tab.id }"
        @click="activeTab = tab.id"
      >
        <LucideIcon :name="tab.icon" size="14" />
        {{ tab.label }}
        <span v-if="tab.badge" class="epp__tab-badge">{{ tab.badge }}</span>
      </button>
    </div>

    <!-- ── Tab content ── -->
    <div class="epp__tab-content">

      <!-- Tab: Questions -->
      <div v-if="activeTab === 'questions'" class="epp__tab-pane">
        <div v-if="loading" class="epp__loading">
          <LucideIcon name="sync" size="28" class="epp__spin" />
          <span>Đang tải dữ liệu...</span>
        </div>
        <div v-else-if="!questions.length" class="epp__empty">
          <LucideIcon name="quiz" size="40" />
          <p>Chưa có câu hỏi nào</p>
        </div>
        <div v-else>
          <!-- Filter bar -->
          <div class="epp__filter-bar">
            <div class="epp__filter-group">
              <button
                v-for="f in questionFilters"
                :key="f.id"
                type="button"
                class="epp__filter-btn"
                :class="{ 'epp__filter-btn--active': activeQuestionFilter === f.id }"
                @click="activeQuestionFilter = f.id"
              >
                {{ f.label }}
                <span class="epp__filter-count">{{ f.count }}</span>
              </button>
            </div>
            <div class="epp__filter-group">
              <button
                type="button"
                class="epp__filter-btn"
                :class="{ 'epp__filter-btn--active': viewMode === 'grid' }"
                @click="viewMode = 'grid'"
              >
                <LucideIcon name="grid_view" size="14" />
              </button>
              <button
                type="button"
                class="epp__filter-btn"
                :class="{ 'epp__filter-btn--active': viewMode === 'list' }"
                @click="viewMode = 'list'"
              >
                <LucideIcon name="view_list" size="14" />
              </button>
            </div>
          </div>

          <!-- Question grid/list -->
          <div v-if="viewMode === 'grid'" class="epp__qgrid">
            <div
              v-for="q in filteredQuestions"
              :key="q.index"
              class="epp__qcard"
              :class="{
                'epp__qcard--low-conf': (q.parseConfidence || 0) < 0.7,
                'epp__qcard--has-issues': q.issues?.length,
                'epp__qcard--image': q.renderMode === 'IMAGE'
              }"
              @click="selectedQuestion = q"
            >
              <div class="epp__qcard-header">
                <span class="epp__qcard-num">{{ q.index }}</span>
                <span class="epp__qcard-type">{{ typeLabel(q.type) }}</span>
                <span v-if="q.renderMode === 'IMAGE'" class="epp__qcard-render-badge">
                  <LucideIcon name="image" size="10" />
                </span>
                <span v-if="q.issues?.length" class="epp__qcard-issue-badge">
                  <LucideIcon name="warning" size="10" />
                </span>
              </div>
              <p class="epp__qcard-stem">{{ truncate(q.text ?? q.content, 200) }}</p>
              <div class="epp__qcard-footer">
                <span class="epp__qcard-conf" :class="confClass(q.parseConfidence)">
                  {{ (q.parseConfidence || 0).toFixed(2) }}
                </span>
                <span v-if="q.correctAnswer" class="epp__qcard-answer">{{ q.correctAnswer }}</span>
                <span v-if="q.options?.length" class="epp__qcard-optcount">{{ q.options.length }} opts</span>
              </div>
            </div>
          </div>

          <div v-else class="epp__qlist">
            <div
              v-for="q in filteredQuestions"
              :key="q.index"
              class="epp__qrow"
              :class="{
                'epp__qrow--low-conf': (q.parseConfidence || 0) < 0.7,
                'epp__qrow--has-issues': q.issues?.length,
              }"
              @click="selectedQuestion = q"
            >
              <span class="epp__qrow-num">{{ q.index }}</span>
              <span class="epp__qrow-stem">{{ truncate(q.text ?? q.content, 80) }}</span>
              <span class="epp__qrow-type">{{ typeLabel(q.type) }}</span>
              <span v-if="q.renderMode === 'IMAGE'" class="epp__qrow-badge"><LucideIcon name="image" size="11" /></span>
              <span v-if="q.issues?.length" class="epp__qrow-badge epp__qrow-badge--warn">
                <LucideIcon name="warning" size="11" />
              </span>
              <span class="epp__qrow-answer" v-if="q.correctAnswer">{{ q.correctAnswer }}</span>
              <span class="epp__qrow-conf" :class="confClass(q.parseConfidence)">
                {{ (q.parseConfidence || 0).toFixed(2) }}
              </span>
            </div>
          </div>

          <p class="epp__filter-result">
            Hiển thị {{ filteredQuestions.length }} / {{ questions.length }} câu
          </p>
        </div>
      </div>

      <!-- Tab: Report -->
      <div v-if="activeTab === 'report'" class="epp__tab-pane">
        <ParseReportPanel
          v-if="report"
          :report="report"
          @goto-question="gotoQuestion"
        />
        <div v-else class="epp__empty">
          <LucideIcon name="summarize" size="40" />
          <p>Chưa có báo cáo</p>
        </div>
      </div>

    </div>

    <!-- ── Question detail modal ── -->
    <div v-if="selectedQuestion" class="epp__modal-overlay" @click.self="selectedQuestion = null">
      <div class="epp__modal">
        <div class="epp__modal-header">
          <span class="epp__modal-title">Câu {{ selectedQuestion.index }}</span>
          <button type="button" class="epp__modal-close" @click="selectedQuestion = null">
            <LucideIcon name="close" size="18" />
          </button>
        </div>
        <div class="epp__modal-body">
          <QuestionRenderer
            v-if="selectedQuestion"
            :question="selectedQuestion"
            :default-mode="selectedQuestion.renderMode === 'IMAGE' ? 'image' : 'text'"
            :session-id="sessionId"
          />
        </div>
      </div>
    </div>

    <!-- ── Bottom action bar ── -->
    <div class="epp__bottom-bar">
      <div class="epp__bottom-stats">
        <span>{{ questions.length }} câu</span>
        <span>{{ report?.answerCount || 0 }} đáp án</span>
        <span v-if="report?.invalidQuestions?.length" class="epp__warn-text">
          {{ report.invalidQuestions.length }} câu lỗi
        </span>
      </div>
      <div class="epp__bottom-actions">
        <button
          type="button"
          class="epp__btn epp__btn--outline"
          :disabled="loading"
          @click="refresh"
        >
          <LucideIcon name="refresh" size="14" />
          Làm mới
        </button>
        <button
          type="button"
          class="epp__btn epp__btn--primary"
          :disabled="loading || !questions.length"
          @click="$emit('confirm')"
        >
          <LucideIcon name="check" size="14" />
          Xác nhận import
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getExamImportPreview } from '@/services/importService.js'
import ParseReportPanel from './ParseReportPanel.vue'
import QuestionRenderer from './QuestionRenderer.vue'
import LucideIcon from '@/components/common/LucideIcon.vue'

const props = defineProps({
  sessionId: { type: [Number, String], required: true },
  examId: { type: Number, default: null }
})

const emit = defineEmits(['confirm', 'question-click'])

const loading = ref(true)
const previewData = ref(null)
const selectedQuestion = ref(null)
const activeTab = ref('questions')
const viewMode = ref('grid')
const activeQuestionFilter = ref('all')

const meta = computed(() => previewData.value?.meta)
const report = computed(() => previewData.value?.report)
const questions = computed(() => previewData.value?.questions || [])
const sessionId = computed(() => props.sessionId)

const tabs = computed(() => [
  {
    id: 'questions',
    icon: 'quiz',
    label: 'Câu hỏi',
    badge: questions.value.length || null
  },
  {
    id: 'report',
    icon: 'summarize',
    label: 'Báo cáo',
    badge: (report.value?.warnings?.length || 0) + (report.value?.invalidQuestions?.length || 0) || null
  }
])

const questionFilters = computed(() => [
  { id: 'all', label: 'Tất cả', count: questions.value.length },
  {
    id: 'low',
    label: 'Confidence thấp',
    count: questions.value.filter(q => (q.parseConfidence || 0) < 0.7).length
  },
  {
    id: 'image',
    label: 'Ảnh',
    count: questions.value.filter(q => q.renderMode === 'IMAGE').length
  },
  {
    id: 'issues',
    label: 'Có lỗi',
    count: questions.value.filter(q => q.issues?.length).length
  }
])

const filteredQuestions = computed(() => {
  const q = questions.value
  switch (activeQuestionFilter.value) {
    case 'low': return q.filter(q => (q.parseConfidence || 0) < 0.7)
    case 'image': return q.filter(q => q.renderMode === 'IMAGE')
    case 'issues': return q.filter(q => q.issues?.length)
    default: return q
  }
})

const load = async () => {
  loading.value = true
  try {
    const res = await getExamImportPreview(Number(props.sessionId))
    previewData.value = res.data?.data || res.data || {}
  } catch (e) {
    console.error('[ExamPreviewPage] Failed to load preview:', e)
  } finally {
    loading.value = false
  }
}

const refresh = () => load()

const gotoQuestion = (qIndex) => {
  activeTab.value = 'questions'
  const q = questions.value.find(q => q.index === qIndex)
  if (q) selectedQuestion.value = q
}

const templateLabel = (t) => {
  const m = {
    template_01_math_broken: 'Toán vỡ',
    template_02_clean_mcq: 'Tiếng Anh',
    template_03_math_answer_grid: 'Toán đáp án lưới'
  }
  return m[t] || t || 'Tự động'
}

const typeLabel = (type) => {
  const m = {
    SINGLE_CHOICE: 'Trắc nghiệm',
    MULTIPLE_CHOICE: 'Nhiều đáp án',
    ESSAY: 'Tự luận',
    FILL_IN_BLANK: 'Điền trống',
    CODE: 'Code'
  }
  return m[type] || type || '—'
}

const truncate = (text, len) => {
  if (!text) return ''
  return text.length > len ? text.slice(0, len) + '…' : text
}

const confClass = (conf) => {
  if (conf == null) return 'epp__conf--unknown'
  if (conf >= 0.8) return 'epp__conf--high'
  if (conf >= 0.5) return 'epp__conf--mid'
  return 'epp__conf--low'
}

const confChipClass = (conf) => {
  if (conf == null) return 'epp__chip'
  if (conf >= 0.8) return 'epp__chip--success'
  if (conf >= 0.5) return 'epp__chip--warn'
  return 'epp__chip--danger'
}

const confIcon = (conf) => {
  if (conf == null) return 'help'
  return conf >= 0.8 ? 'check_circle' : conf >= 0.5 ? 'warning' : 'error'
}

onMounted(load)
</script>

<style scoped>
.epp {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: var(--ds-gray-50, #f8fafc);
}

/* ── Nav ── */
.epp__nav {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-surface);
  flex-shrink: 0;
}

.epp__back-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.35rem 0.75rem;
  border-radius: var(--ds-radius-lg);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-muted);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.12s;
}

.epp__back-btn:hover { border-color: var(--ds-primary); color: var(--ds-primary); }

.epp__nav-title {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
}

/* ── Page header ── */
.epp__page-header {
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-surface);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.epp__meta-grid {
  display: flex;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.epp__meta-item {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.epp__meta-label {
  font-size: 0.68rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--ds-text-muted);
}

.epp__meta-value {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--ds-text);
}

.epp__chips {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.epp__chip {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  font-size: 0.72rem;
  font-weight: 700;
  border: 1px solid var(--ds-border);
}

.epp__chip--template { background: var(--ds-primary-soft); color: var(--ds-primary); border-color: var(--ds-primary-border); }
.epp__chip--count { background: rgba(59, 130, 246, 0.1); color: #3b82f6; border-color: rgba(59, 130, 246, 0.2); }
.epp__chip--answer { background: var(--ds-success-soft); color: var(--ds-success); border-color: rgba(22, 163, 74, 0.2); }
.epp__chip--success { background: var(--ds-success-soft); color: var(--ds-success); border-color: rgba(22, 163, 74, 0.2); }
.epp__chip--warn { background: rgba(217, 119, 6, 0.1); color: #d97706; border-color: rgba(217, 119, 6, 0.2); }
.epp__chip--danger { background: rgba(220, 38, 38, 0.1); color: var(--ds-danger); border-color: rgba(220, 38, 38, 0.2); }

/* ── Tabs ── */
.epp__tabs {
  display: flex;
  gap: 0;
  padding: 0 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-surface);
  flex-shrink: 0;
}

.epp__tab {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.75rem 1rem;
  border: none;
  background: transparent;
  color: var(--ds-text-muted);
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: color 0.12s, border-color 0.12s;
}

.epp__tab:hover { color: var(--ds-text); }
.epp__tab--active { color: var(--ds-primary); border-bottom-color: var(--ds-primary); }

.epp__tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 0.3rem;
  border-radius: 9999px;
  background: var(--ds-gray-200);
  color: var(--ds-text-muted);
  font-size: 0.65rem;
  font-weight: 800;
}

.epp__tab--active .epp__tab-badge { background: var(--ds-primary-soft); color: var(--ds-primary); }

/* ── Tab content ── */
.epp__tab-content {
  flex: 1;
  overflow-y: auto;
  padding: 1rem 1.5rem;
}

.epp__tab-pane { display: flex; flex-direction: column; gap: 1rem; }

/* ── Loading / Empty ── */
.epp__loading, .epp__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 4rem 2rem;
  text-align: center;
  color: var(--ds-text-muted);
  font-size: 0.875rem;
}

.epp__spin {
  animation: epp-spin 1s linear infinite;
}

@keyframes epp-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ── Filter bar ── */
.epp__filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.epp__filter-group { display: flex; gap: 0.25rem; }

.epp__filter-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem 0.75rem;
  border-radius: var(--ds-radius-full);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-muted);
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.12s;
}

.epp__filter-btn:hover { border-color: var(--ds-primary); color: var(--ds-primary); }
.epp__filter-btn--active { border-color: var(--ds-primary); background: var(--ds-primary-soft); color: var(--ds-primary); }

.epp__filter-count {
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  min-width: 16px;
  text-align: center;
}

/* ── Question grid ── */
.epp__qgrid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 0.75rem;
}

.epp__qcard {
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  background: var(--ds-surface);
  padding: 0.875rem;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  transition: border-color 0.12s, box-shadow 0.12s;
}

.epp__qcard:hover { border-color: var(--ds-primary-border); box-shadow: 0 0 0 3px var(--ds-primary-ring); }

.epp__qcard--low-conf { border-left: 3px solid var(--ds-warning); }
.epp__qcard--has-issues { border-left: 3px solid var(--ds-danger); }
.epp__qcard--image { border-left: 3px solid #7c3aed; }

.epp__qcard-header {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.epp__qcard-num {
  font-size: 0.8rem;
  font-weight: 800;
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  padding: 0.1rem 0.4rem;
  border-radius: var(--ds-radius-md);
}

.epp__qcard-type {
  font-size: 0.62rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  flex: 1;
}

.epp__qcard-render-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.1rem 0.3rem;
  border-radius: 9999px;
  background: rgba(139, 92, 246, 0.12);
  color: #7c3aed;
  font-size: 0.6rem;
}

.epp__qcard-issue-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.1rem 0.3rem;
  border-radius: 9999px;
  background: rgba(220, 38, 38, 0.1);
  color: var(--ds-danger);
  font-size: 0.6rem;
}

.epp__qcard-stem {
  font-size: 0.8rem;
  line-height: 1.4;
  color: var(--ds-text);
  margin: 0;
  flex: 1;
}

.epp__qcard-footer {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.7rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.epp__qcard-answer {
  color: var(--ds-success);
  background: var(--ds-success-soft);
  padding: 0.1rem 0.4rem;
  border-radius: 9999px;
}

.epp__qcard-optcount { color: var(--ds-text-muted); }

/* ── Question list ── */
.epp__qlist { display: flex; flex-direction: column; gap: 0.25rem; }

.epp__qrow {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-lg);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  cursor: pointer;
  font-size: 0.8rem;
  transition: border-color 0.12s;
}

.epp__qrow:hover { border-color: var(--ds-primary-border); }
.epp__qrow--low-conf { border-left: 3px solid var(--ds-warning); }
.epp__qrow--has-issues { border-left: 3px solid var(--ds-danger); }

.epp__qrow-num { font-weight: 800; color: var(--ds-primary); min-width: 28px; }
.epp__qrow-stem { flex: 1; color: var(--ds-text); }
.epp__qrow-type { color: var(--ds-text-muted); min-width: 80px; font-size: 0.72rem; }
.epp__qrow-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(139, 92, 246, 0.1);
  color: #7c3aed;
}
.epp__qrow-badge--warn { background: rgba(220, 38, 38, 0.1); color: var(--ds-danger); }
.epp__qrow-answer { color: var(--ds-success); font-weight: 700; min-width: 20px; }

/* Confidence display */
.epp__qcard-conf, .epp__qrow-conf {
  font-weight: 800;
  font-size: 0.68rem;
  font-variant-numeric: tabular-nums;
}
.epp__conf--high { color: var(--ds-success); }
.epp__conf--mid { color: #d97706; }
.epp__conf--low { color: var(--ds-danger); }
.epp__conf--unknown { color: var(--ds-text-muted); }

/* Filter result */
.epp__filter-result {
  font-size: 0.72rem;
  color: var(--ds-text-muted);
  text-align: center;
  margin: 0;
}

/* ── Modal ── */
.epp__modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 2rem;
}

.epp__modal {
  width: 100%;
  max-width: 720px;
  max-height: 90vh;
  background: var(--ds-surface);
  border-radius: var(--ds-radius-2xl);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.25);
}

.epp__modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  flex-shrink: 0;
}

.epp__modal-title { font-size: 0.95rem; font-weight: 700; color: var(--ds-text); }

.epp__modal-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--ds-border);
  background: transparent;
  color: var(--ds-text-muted);
  cursor: pointer;
  transition: all 0.12s;
}

.epp__modal-close:hover { border-color: var(--ds-danger); color: var(--ds-danger); }

.epp__modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
}

/* ── Bottom bar ── */
.epp__bottom-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.875rem 1.5rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-surface);
  flex-shrink: 0;
}

.epp__bottom-stats {
  display: flex;
  gap: 1rem;
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  font-weight: 600;
}

.epp__warn-text { color: var(--ds-warning); }

.epp__bottom-actions { display: flex; gap: 0.5rem; }

/* Buttons */
.epp__btn {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.12s;
  border: 1.5px solid transparent;
}

.epp__btn:disabled { opacity: 0.5; cursor: not-allowed; }

.epp__btn--outline {
  border-color: var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-muted);
}

.epp__btn--outline:hover:not(:disabled) { border-color: var(--ds-primary); color: var(--ds-primary); }

.epp__btn--primary {
  background: var(--ds-primary);
  color: white;
  border-color: var(--ds-primary);
}

.epp__btn--primary:hover:not(:disabled) { background: var(--ds-primary-hover); }
</style>
