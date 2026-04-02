<template>
  <div class="rdp">
    <!-- Loading skeleton -->
    <div v-if="loading" class="rdp__skeleton">
      <div class="rdp__skel-row">
        <div v-for="i in 3" :key="i" class="rdp__skel-card">
          <div class="rdp__skel" style="height:14px;width:80px" />
          <div class="rdp__skel" style="height:32px;width:64px" />
        </div>
      </div>
      <div class="rdp__skel" style="height:200px" />
    </div>

    <div v-else>
      <!-- Score + meta cards -->
      <div class="rdp__top-cards">
        <div class="rdp__score-card">
          <p class="rdp__score-label">Tổng điểm</p>
          <div class="rdp__score-ring" :class="scoreRingClass">
            <span class="rdp__score-val">{{ scoreDisplay }}<span class="rdp__score-max">/10</span></span>
          </div>
          <div class="rdp__progress-bar">
            <div class="rdp__progress-fill" :class="progressBarClass" :style="{ width: `${scorePercent}%` }" />
          </div>
        </div>

        <div class="rdp__meta-card">
          <div class="rdp__meta-icon">
            <LucideIcon name="timer" />
          </div>
          <div>
            <p class="rdp__meta-val">{{ timeTaken }}</p>
            <p class="rdp__meta-lbl">Thời gian làm bài</p>
          </div>
        </div>

        <div class="rdp__meta-card">
          <div class="rdp__meta-icon rdp__meta-icon--rank">
            <LucideIcon name="leaderboard" />
          </div>
          <div>
            <p class="rdp__meta-val">{{ rankDisplay }}</p>
            <p class="rdp__meta-lbl">Thứ hạng</p>
          </div>
        </div>
      </div>

      <!-- Answer distribution -->
      <div class="rdp__distribution">
        <div class="rdp__section-header">
          <LucideIcon name="analytics" />
          <h3>Phân bổ câu trả lời</h3>
        </div>
        <div class="rdp__dist-bars">
          <div class="rdp__dist-item">
            <div class="rdp__dist-label">
              <LucideIcon name="check_circle" />
              <span>Đúng</span>
            </div>
            <div class="rdp__dist-track">
              <div class="rdp__dist-fill rdp__dist-fill--correct" :style="{ width: `${correctRatio}%` }" />
            </div>
            <span class="rdp__dist-count">{{ correctCount }} câu</span>
          </div>
          <div class="rdp__dist-item">
            <div class="rdp__dist-label">
              <LucideIcon name="cancel" />
              <span>Sai</span>
            </div>
            <div class="rdp__dist-track">
              <div class="rdp__dist-fill rdp__dist-fill--incorrect" :style="{ width: `${incorrectRatio}%` }" />
            </div>
            <span class="rdp__dist-count">{{ incorrectCount }} câu</span>
          </div>
          <div class="rdp__dist-item">
            <div class="rdp__dist-label">
              <LucideIcon name="skip_next" />
              <span>Bỏ qua</span>
            </div>
            <div class="rdp__dist-track">
              <div class="rdp__dist-fill rdp__dist-fill--skipped" :style="{ width: `${skippedRatio}%` }" />
            </div>
            <span class="rdp__dist-count">{{ skippedCount }} câu</span>
          </div>
        </div>
      </div>

      <!-- Question review -->
      <div v-if="reviewAnswers.length" class="rdp__review">
        <div class="rdp__section-header">
          <LucideIcon name="quiz" />
          <h3>Xem lại câu hỏi</h3>
          <div class="rdp__review-badges">
            <span class="rdp__badge rdp__badge--correct">
              <LucideIcon name="check_circle" /> Đúng
            </span>
            <span class="rdp__badge rdp__badge--incorrect">
              <LucideIcon name="cancel" /> Sai
            </span>
          </div>
        </div>

        <div class="rdp__questions">
          <div
            v-for="item in reviewAnswers"
            :key="item.questionId"
            class="rdp__question-card"
            :class="item.correct ? 'rdp__question-card--correct' : 'rdp__question-card--incorrect'"
          >
            <div class="rdp__question-header">
              <span class="rdp__question-index">Câu {{ item.index }}</span>
              <span class="rdp__question-status" :class="item.correct ? 'rdp__question-status--correct' : 'rdp__question-status--incorrect'">
                <LucideIcon :name="item.correct ? 'check_circle' : 'cancel'" />
                {{ item.correct ? 'Đúng' : 'Sai' }}
              </span>
            </div>

            <p class="rdp__question-text">{{ item.question }}</p>

            <div v-if="item.options.length" class="rdp__options">
              <div
                v-for="option in item.options"
                :key="`${item.questionId}-${option.id}`"
                class="rdp__option"
                :class="optionClass(item, option)"
              >
                <span class="rdp__option-letter">{{ option.id }}</span>
                <span class="rdp__option-text">{{ option.text }}</span>
                <LucideIcon name="check_circle" v-if="isCorrectOption(item, option)" class="rdp__option-icon rdp__option-icon--ok" />
                <LucideIcon name="cancel" v-else-if="isSelectedOption(item, option) && !isCorrectOption(item, option)" class="rdp__option-icon rdp__option-icon--wrong" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- No questions -->
      <div v-else class="rdp__no-review">
        <LucideIcon name="quiz" />
        <p>Chưa có chi tiết câu hỏi để xem lại.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  loading: { type: Boolean, default: false },
  score: { type: [Number, String], default: 0 },
  timeTaken: { type: String, default: '-' },
  rankOrder: { type: String, default: '' },
  correctCount: { type: Number, default: 0 },
  incorrectCount: { type: Number, default: 0 },
  skippedCount: { type: Number, default: 0 },
  totalQuestions: { type: Number, default: 0 },
  reviewAnswers: { type: Array, default: () => [] }
})

const scorePercent = computed(() => {
  const s = Number(props.score)
  return s > 0 ? Math.round(s) : 0
})

const scoreDisplay = computed(() => (scorePercent.value / 10).toFixed(1))

const rankDisplay = computed(() => props.rankOrder ? `#${props.rankOrder}` : '-')

const scoreRingClass = computed(() => {
  if (scorePercent.value >= 80) return 'rdp__score-ring--high'
  if (scorePercent.value >= 50) return 'rdp__score-ring--mid'
  return 'rdp__score-ring--low'
})

const progressBarClass = computed(() => {
  if (scorePercent.value >= 80) return 'rdp__progress-fill--high'
  if (scorePercent.value >= 50) return 'rdp__progress-fill--mid'
  return 'rdp__progress-fill--low'
})

const total = computed(() => Math.max(props.totalQuestions || 1, 1))

const correctRatio = computed(() => Math.round((props.correctCount / total.value) * 100))
const incorrectRatio = computed(() => Math.round((props.incorrectCount / total.value) * 100))
const skippedRatio = computed(() => Math.round((props.skippedCount / total.value) * 100))

const normalizeOptionId = (value) => {
  if (!value) return ''
  const str = String(value).trim().toUpperCase()
  return ['A', 'B', 'C', 'D'].includes(str) ? str : str.charAt(0)
}

const parseOptions = (options) => {
  if (!options) return []
  if (Array.isArray(options)) {
    return options.map((opt) => {
      if (typeof opt === 'string') {
        const match = opt.match(/^([A-D])\.\s*(.*)/)
        return match ? { id: match[1].trim(), text: match[2].trim() } : { id: '?', text: opt.trim() }
      }
      return { 
        id: normalizeOptionId(opt.id || opt.key || opt.letter || opt.label), 
        text: String(opt.text || opt.content || opt.value || '').replace(/^[A-D]\.\s*/, '').trim() 
      }
    })
  }
  if (typeof options === 'string') {
    const trimmed = options.trim()
    // Handle JSON array format: [{"id":"A","text":"..."},...]
    if (trimmed.startsWith('[')) {
      try {
        const parsed = JSON.parse(trimmed)
        const arr = Array.isArray(parsed) ? parsed : (parsed.options || parsed.choices || [])
        if (Array.isArray(arr) && arr.length > 0) {
          return arr.map((opt) => {
            if (typeof opt === 'string') {
              const match = opt.match(/^([A-D])\.\s*(.*)/)
              return match ? { id: match[1].trim(), text: match[2].trim() } : { id: '?', text: opt.trim() }
            }
            return { 
              id: normalizeOptionId(opt.id || opt.key || opt.letter || opt.label), 
              text: String(opt.text || opt.content || opt.value || opt.option || '').replace(/^[A-D]\.\s*/, '').trim() 
            }
          })
        }
      } catch {
        // fall through to line-by-line
      }
    }
    // Handle JSON object format
    if (trimmed.startsWith('{')) {
      try {
        const parsed = JSON.parse(trimmed)
        const arr = parsed.options || parsed.choices || parsed.answers || parsed.items || []
        if (Array.isArray(arr) && arr.length > 0) {
          return arr.map((opt) => {
            if (typeof opt === 'string') {
              const match = opt.match(/^([A-D])\.\s*(.*)/)
              return match ? { id: match[1].trim(), text: match[2].trim() } : { id: '?', text: opt.trim() }
            }
            return { 
              id: normalizeOptionId(opt.id || opt.key || opt.letter || opt.label), 
              text: String(opt.text || opt.content || opt.value || opt.option || '').replace(/^[A-D]\.\s*/, '').trim() 
            }
          })
        }
      } catch {
        // fall through to line-by-line
      }
    }
    // Line-by-line format
    return trimmed.split(/\n/).filter(Boolean).map((line) => {
      const match = line.trim().match(/^([A-D])\.\s*(.*)/)
      return match ? { id: match[1].trim(), text: match[2].trim() } : { id: '?', text: line.trim() }
    })
  }
  return []
}

const parseAnswerValue = (value) => {
  if (!value) return null
  if (typeof value === 'object' && !Array.isArray(value)) return value
  if (typeof value === 'object' && Array.isArray(value)) return value
  try { return JSON.parse(String(value)) } catch { return null }
}

const normalizeAnswerIds = (value) => {
  if (!value) return []
  // Already an array
  if (Array.isArray(value)) return value.map(normalizeOptionId)
  // Try to parse as JSON
  if (typeof value === 'object') {
    if (Array.isArray(value)) return value.map(normalizeOptionId)
    // Single string value in object
    return [normalizeOptionId(value)]
  }
  // String - try JSON parse first
  try { 
    const parsed = JSON.parse(String(value))
    if (Array.isArray(parsed)) return parsed.map(normalizeOptionId)
    if (typeof parsed === 'string') return [normalizeOptionId(parsed)]
  } catch { /* continue */ }
  // Plain string
  return [normalizeOptionId(value)]
}

const isSelectedOption = (item, option) => {
  const selected = normalizeAnswerIds(item.selectedAnswer || item.selectedIds || item.selectedIds_ || item.selected || [])
  const optId = String(option.id || '').toUpperCase().trim()
  const optText = String(option.text || '').trim()
  return selected.some(id => {
    const normalizedId = String(id || '').toUpperCase().trim()
    return normalizedId === optId || normalizedId === optText
  })
}

const isCorrectOption = (item, option) => {
  const correct = normalizeAnswerIds(item.correctAnswer || item.correctIds || item.correctIds_ || item.correct || [])
  const optId = String(option.id || '').toUpperCase().trim()
  const optText = String(option.text || '').trim()
  return correct.some(id => {
    const normalizedId = String(id || '').toUpperCase().trim()
    return normalizedId === optId || normalizedId === optText
  })
}

const optionClass = (item, option) => {
  if (isCorrectOption(item, option)) return 'rdp__option--correct'
  if (isSelectedOption(item, option)) return 'rdp__option--wrong'
  return 'rdp__option--neutral'
}
</script>


<style scoped>
.rdp { display: flex; flex-direction: column; gap: 1rem; }

/* Skeleton */
.rdp__skeleton { display: flex; flex-direction: column; gap: 1rem; }
.rdp__skel-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.75rem; }
.rdp__skel-card { padding: 1.25rem; border-radius: var(--ds-radius-2xl); background: var(--ds-surface); border: 1.5px solid var(--ds-border); display: flex; flex-direction: column; gap: 0.5rem; align-items: center; }
.rdp__skel { background: linear-gradient(90deg, var(--ds-gray-100) 25%, var(--ds-gray-200) 50%, var(--ds-gray-100) 75%); background-size: 200% 100%; animation: rdpShimmer 1.5s infinite; border-radius: var(--ds-radius-md); }
.dark .rdp__skel { background: linear-gradient(90deg, var(--ds-gray-800) 25%, var(--ds-gray-700) 50%, var(--ds-gray-800) 75%); background-size: 200% 100%; }
@keyframes rdpShimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }

/* Top cards */
.rdp__top-cards { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 0.75rem; }

/* Score card */
.rdp__score-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.625rem;
  padding: 1.25rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  text-align: center;
}

.dark .rdp__score-card { border-color: var(--ds-border-strong); }

.rdp__score-label {
  font-size: 0.7rem;
  font-weight: 800;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin: 0;
}

.rdp__score-ring {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 4px solid;
  position: relative;
  transition: all 0.3s ease;
}

.rdp__score-ring--high {
  border-color: var(--ds-success);
  background: rgba(16, 185, 129, 0.06);
}

.rdp__score-ring--mid {
  border-color: var(--ds-warning);
  background: rgba(234, 179, 8, 0.06);
}

.rdp__score-ring--low {
  border-color: var(--ds-danger);
  background: rgba(220, 38, 38, 0.06);
}

.rdp__score-ring:hover {
  transform: scale(1.03);
}

.rdp__score-val {
  font-family: var(--ds-font-display);
  font-size: 1.5rem;
  font-weight: 900;
  color: var(--ds-text);
  line-height: 1;
}

.dark .rdp__score-val { color: #f1f5f9; }

.rdp__score-max { font-size: 0.875rem; font-weight: 600; opacity: 0.6; }

.rdp__progress-bar { width: 100%; height: 6px; border-radius: var(--ds-radius-full); background: var(--ds-gray-100); overflow: hidden; }
.dark .rdp__progress-bar { background: var(--ds-gray-800); }

.rdp__progress-fill { 
  height: 100%; 
  border-radius: var(--ds-radius-full); 
  transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.rdp__progress-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.4), transparent);
  animation: progressShine 2s ease-in-out infinite;
}

@keyframes progressShine {
  0% { left: -100%; }
  50%, 100% { left: 100%; }
}

.rdp__progress-fill--high { 
  background: linear-gradient(90deg, #10b981, #34d399); 
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.4);
}
.rdp__progress-fill--mid { 
  background: linear-gradient(90deg, #f59e0b, #fbbf24); 
  box-shadow: 0 0 10px rgba(245, 158, 11, 0.4);
}
.rdp__progress-fill--low { 
  background: linear-gradient(90deg, #ef4444, #f87171); 
  box-shadow: 0 0 10px rgba(239, 68, 68, 0.4);
}

/* Meta card */
.rdp__meta-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1.25rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
}

.dark .rdp__meta-card { border-color: var(--ds-border-strong); }

.rdp__meta-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rdp__meta-icon--rank { background: rgba(234, 179, 8, 0.1); color: var(--ds-warning); }

.rdp__meta-val { font-size: 1.25rem; font-weight: 900; color: var(--ds-text); margin: 0; line-height: 1; }
.dark .rdp__meta-val { color: #f1f5f9; }

.rdp__meta-lbl { font-size: 0.6rem; font-weight: 700; color: var(--ds-text-muted); text-transform: uppercase; letter-spacing: 0.06em; margin-top: 0.25rem; }

/* Distribution */
.rdp__distribution {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 1.125rem 1.25rem;
}

.dark .rdp__distribution { border-color: var(--ds-border-strong); }

.rdp__section-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.875rem;
}

.rdp__section-header h3 { font-size: 0.9rem; font-weight: 800; color: var(--ds-text); margin: 0; }
.dark .rdp__section-header h3 { color: #f1f5f9; }

.rdp__dist-bars { display: flex; flex-direction: column; gap: 0.75rem; }

.rdp__dist-item { display: flex; align-items: center; gap: 0.75rem; }

.rdp__dist-label {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  min-width: 80px;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .rdp__dist-label { color: #f1f5f9; }


.rdp__dist-track { flex: 1; height: 10px; border-radius: var(--ds-radius-full); background: var(--ds-gray-100); overflow: hidden; }
.dark .rdp__dist-track { background: var(--ds-gray-800); }

.rdp__dist-fill { 
  height: 100%; 
  border-radius: var(--ds-radius-full); 
  transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.rdp__dist-fill--correct { 
  background: linear-gradient(90deg, #10b981, #34d399); 
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.3);
}
.rdp__dist-fill--incorrect { 
  background: linear-gradient(90deg, #ef4444, #f87171); 
  box-shadow: 0 0 8px rgba(239, 68, 68, 0.3);
}
.rdp__dist-fill--skipped { 
  background: linear-gradient(90deg, var(--ds-gray-300), #d1d5db); 
}
.dark .rdp__dist-fill--skipped { 
  background: linear-gradient(90deg, var(--ds-gray-600), #6b7280); 
}

.rdp__dist-count { min-width: 60px; font-size: 0.75rem; font-weight: 700; color: var(--ds-text-muted); text-align: right; }

/* Review */
.rdp__review {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  padding: 1.125rem 1.25rem;
}

.dark .rdp__review { border-color: var(--ds-border-strong); }

.rdp__review-badges { display: flex; gap: 0.5rem; margin-left: auto; }

.rdp__badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 700;
}


.rdp__badge--correct { background: var(--ds-success-soft); color: var(--ds-success); }
.rdp__badge--incorrect { background: var(--ds-danger-soft); color: var(--ds-danger); }

.rdp__questions { display: flex; flex-direction: column; gap: 0.75rem; }

.rdp__question-card {
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid;
  overflow: hidden;
}

.rdp__question-card--correct { border-color: rgba(16, 185, 129, 0.3); border-left-width: 4px; border-left-color: var(--ds-success); }
.rdp__question-card--incorrect { border-color: rgba(220, 38, 38, 0.3); border-left-width: 4px; border-left-color: var(--ds-danger); }

.rdp__question-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  background: var(--ds-gray-50);
  border-bottom: 1px solid var(--ds-border);
}

.dark .rdp__question-header { background: var(--ds-gray-800); border-bottom-color: var(--ds-border-strong); }

.rdp__question-index { font-size: 0.7rem; font-weight: 800; color: var(--ds-text-muted); text-transform: uppercase; letter-spacing: 0.08em; }

.rdp__question-status {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.7rem;
  font-weight: 800;
}


.rdp__question-status--correct { color: var(--ds-success); }
.rdp__question-status--incorrect { color: var(--ds-danger); }

.rdp__question-text {
  padding: 1rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.5;
}

.dark .rdp__question-text { color: #f1f5f9; }

.rdp__options { padding: 0 1rem 1rem; display: flex; flex-direction: column; gap: 0.5rem; }

.rdp__option {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.625rem 0.875rem;
  border-radius: var(--ds-radius-lg);
  border: 1.5px solid;
  transition: all 0.1s ease;
}

.rdp__option--correct { border-color: var(--ds-success); background: rgba(16, 185, 129, 0.06); }
.rdp__option--wrong { border-color: var(--ds-danger); background: rgba(220, 38, 38, 0.06); }
.rdp__option--neutral { border-color: var(--ds-border); background: var(--ds-surface); }
.dark .rdp__option--neutral { border-color: var(--ds-border-strong); background: var(--ds-gray-800); }

.rdp__option-letter {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1.5px solid;
  font-size: 0.7rem;
  font-weight: 800;
  flex-shrink: 0;
}

.rdp__option--correct .rdp__option-letter { border-color: var(--ds-success); color: var(--ds-success); }
.rdp__option--wrong .rdp__option-letter { border-color: var(--ds-danger); color: var(--ds-danger); }
.rdp__option--neutral .rdp__option-letter { border-color: var(--ds-border); color: var(--ds-text-muted); }
.dark .rdp__option--neutral .rdp__option-letter { border-color: var(--ds-border-strong); }

.rdp__option-text { flex: 1; font-size: 0.875rem; font-weight: 500; color: var(--ds-text); line-height: 1.4; }
.dark .rdp__option-text { color: #f1f5f9; }

.rdp__option-icon { font-size: 1.125rem; flex-shrink: 0; }
.rdp__option-icon--ok { color: var(--ds-success); }
.rdp__option-icon--wrong { color: var(--ds-danger); }

/* No review */
.rdp__no-review {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 2rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  text-align: center;
  color: var(--ds-text-muted);
}

.dark .rdp__no-review { border-color: var(--ds-border-strong); }

.rdp__no-review p { font-size: 0.875rem; margin: 0; font-weight: 500; }

/* Responsive */
@media (max-width: 640px) {
  .rdp__top-cards { grid-template-columns: 1fr; }
  .rdp__review-badges { display: none; }
}
</style>
