<template>
  <div class="aem">
    <div class="aem__inner">

      <!-- Header -->
      <header class="aem__header">
        <div class="aem__header-left">
          <nav class="aem__breadcrumb" aria-label="Breadcrumb">
            <RouterLink to="/admin/dashboard" class="aem__breadcrumb-link">Tổng quan</RouterLink>
            <span class="aem__breadcrumb-sep">/</span>
            <span class="aem__breadcrumb-current">Đề thi</span>
          </nav>
          <h1 class="aem__title">Quản lý đề thi</h1>
          <p class="aem__subtitle">
            Xem toàn bộ đề trong hệ thống, trạng thái hoạt động, khung thời gian và thống kê câu hỏi / lượt thi.
          </p>
        </div>
        <div class="aem__header-right">
          <button
            type="button"
            class="aem__btn aem__btn--secondary"
            :disabled="loading"
            @click="load"
          >
            <LucideIcon name="refresh" :class="{ 'aem__spin': loading }" />
            <span>Làm mới</span>
          </button>
        </div>
      </header>

      <!-- Error -->
      <div v-if="errorMsg" class="aem__error">
        <LucideIcon name="alert_circle" />
        <span>{{ errorMsg }}</span>
        <button type="button" class="aem__error-close" @click="errorMsg = ''">
          <LucideIcon name="x" />
        </button>
      </div>

      <!-- Stats strip -->
      <div class="aem__stats-strip">
        <div class="aem__stat-chip">
          <LucideIcon name="quiz" />
          <strong>{{ totalElements.toLocaleString('vi-VN') }}</strong> đề thi
        </div>
        <div class="aem__stat-chip aem__stat-chip--success">
          <LucideIcon name="check_circle" />
          <strong>{{ activeCount }}</strong> đang bật
        </div>
        <div class="aem__stat-chip">
          <LucideIcon name="help_circle" />
          <strong>{{ totalQuestions.toLocaleString('vi-VN') }}</strong> câu hỏi
        </div>
        <div class="aem__stat-chip">
          <LucideIcon name="how_to_reg" />
          <strong>{{ totalAttempts.toLocaleString('vi-VN') }}</strong> lượt thi
        </div>
      </div>

      <!-- Table panel -->
      <div class="aem__table-panel">
        <!-- Toolbar -->
        <div class="aem__toolbar">
          <p class="aem__toolbar-meta">
            <strong>{{ totalElements.toLocaleString('vi-VN') }}</strong> đề thi
          </p>
          <div class="aem__toolbar-actions">
            <span class="aem__toolbar-hint">
              <LucideIcon name="info" />
              Click vào dòng để xem chi tiết
            </span>
          </div>
        </div>

        <!-- Table -->
        <div class="aem__table-wrap">
          <table class="aem__table">
            <thead>
              <tr>
                <th class="aem__th--id">ID</th>
                <th>Tiêu đề</th>
                <th class="aem__th--center">Mã</th>
                <th class="aem__th--center">Phút</th>
                <th class="aem__th--center">Trạng thái</th>
                <th>Thời gian</th>
                <th class="aem__th--center">Người tạo</th>
                <th class="aem__th--center">CH / Lượt</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="loading && !rows.length">
                <tr>
                  <td :colspan="8" class="aem__td--center">
                    <div class="aem__loading-cell">
                      <div v-for="i in 5" :key="i" class="aem__skeleton-row">
                        <div class="aem__skeleton aem__skeleton--sm" />
                        <div class="aem__skeleton aem__skeleton--lg" />
                        <div class="aem__skeleton aem__skeleton--md" />
                      </div>
                    </div>
                  </td>
                </tr>
              </template>
              <template v-else-if="!rows.length">
                <tr>
                  <td :colspan="8" class="aem__td--center">
                    <div class="aem__empty">
                      <LucideIcon name="quiz_outlined" />
                      <p>Chưa có đề thi nào.</p>
                    </div>
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr
                  v-for="row in rows"
                  :key="row.id"
                  class="aem__row"
                >
                  <td class="aem__td aem__td--id">{{ row.id }}</td>
                  <td class="aem__td">
                    <p class="aem__exam-title">{{ row.title }}</p>
                    <p v-if="row.description" class="aem__exam-desc">{{ row.description }}</p>
                  </td>
                  <td class="aem__td aem__td--center">
                    <span class="aem__code-badge">{{ row.code || '—' }}</span>
                  </td>
                  <td class="aem__td aem__td--center">
                    <span class="aem__duration">{{ row.durationMinutes ?? '—' }}</span>
                  </td>
                  <td class="aem__td aem__td--center">
                    <button
                      type="button"
                      class="aem__toggle-btn"
                      :class="row.isActive ? 'aem__toggle-btn--on' : 'aem__toggle-btn--off'"
                      :disabled="togglingId === row.id"
                      @click="toggleActive(row)"
                    >
                      <span class="aem__toggle-dot" />
                      {{ row.isActive ? 'Bật' : 'Tắt' }}
                    </button>
                  </td>
                  <td class="aem__td">
                    <div class="aem__time-range">
                      <span class="aem__time-start">{{ formatDt(row.startTime) }}</span>
                      <LucideIcon name="arrow_right_alt" class="aem__time-arrow" />
                      <span class="aem__time-end">{{ formatDt(row.endTime) }}</span>
                    </div>
                  </td>
                  <td class="aem__td aem__td--center">
                    <span class="aem__creator">{{ row.createdByUsername || '—' }}</span>
                  </td>
                  <td class="aem__td aem__td--center">
                    <div class="aem__counts">
                      <span class="aem__count-chip aem__count-chip--questions">{{ row.questionCount ?? 0 }}</span>
                      <span class="aem__count-sep">/</span>
                      <span class="aem__count-chip aem__count-chip--attempts">{{ row.attemptCount ?? 0 }}</span>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="aem__pagination">
          <p class="aem__pagination-info">
            Trang <strong>{{ page + 1 }}</strong> / {{ totalPages }}
          </p>
          <div class="aem__pagination-btns">
            <button
              type="button"
              class="aem__btn aem__btn--secondary aem__btn--sm"
              :disabled="page <= 0 || loading"
              @click="goPage(page - 1)"
            >
              <LucideIcon name="chevron_left" />
              Trước
            </button>
            <button
              type="button"
              class="aem__btn aem__btn--secondary aem__btn--sm"
              :disabled="page >= totalPages - 1 || loading"
              @click="goPage(page + 1)"
            >
              Sau
              <LucideIcon name="chevron_right" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchAdminExams, patchAdminExamActive } from '../../services/adminService'
import { useToast } from '../../composables/useToast'
import LucideIcon from '../common/LucideIcon.vue'

const toast = useToast()
const loading = ref(true)
const errorMsg = ref('')
const rows = ref([])
const page = ref(0)
const size = ref(20)
const totalElements = ref(0)
const totalPages = ref(0)
const togglingId = ref(null)

const activeCount = computed(() => rows.value.filter(r => r.isActive).length)
const totalQuestions = computed(() => rows.value.reduce((s, r) => s + (r.questionCount || 0), 0))
const totalAttempts = computed(() => rows.value.reduce((s, r) => s + (r.attemptCount || 0), 0))

const formatDt = (iso) => {
  if (iso == null || iso === '') return '—'
  try {
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return String(iso)
    return d.toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' })
  } catch {
    return '—'
  }
}

const load = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await fetchAdminExams({ page: page.value, size: size.value })
    rows.value = data.content || []
    totalElements.value = data.totalElements ?? 0
    totalPages.value = data.totalPages ?? 0
    page.value = data.page ?? page.value
    size.value = data.size ?? size.value
  } catch (e) {
    errorMsg.value = e?.payload?.message || e?.message || 'Không tải được danh sách đề thi.'
    rows.value = []
  } finally {
    loading.value = false
  }
}

const goPage = (p) => {
  page.value = Math.max(0, p)
  void load()
}

const toggleActive = async (row) => {
  if (togglingId.value != null) return
  const next = !row.isActive
  togglingId.value = row.id
  try {
    await patchAdminExamActive(row.id, next)
    row.isActive = next
    toast.success(next ? 'Đã bật đề thi.' : 'Đã tắt đề thi.')
  } catch (e) {
    toast.error(e?.payload?.message || e?.message || 'Không cập nhật được.')
  } finally {
    togglingId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
/* ── Page ───────────────────────────────────────────────────────────────── */
.aem { min-height: 100vh; background: var(--ds-bg); }

.aem__inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 1.5rem 1.5rem 2.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* ── Header ─────────────────────────────────────────────────────────────── */
.aem__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.aem__header-left { flex: 1; min-width: 0; }

.aem__breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  margin-bottom: 0.375rem;
}

.aem__breadcrumb-link {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  text-decoration: none;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  transition: color 0.12s ease;
}

.aem__breadcrumb-link:hover { color: var(--ds-primary); }

.aem__breadcrumb-sep {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
}

.aem__breadcrumb-current {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-text);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.dark .aem__breadcrumb-current { color: #94a3b8; }

.aem__title {
  font-family: var(--ds-font-display);
  font-size: 1.5rem;
  font-weight: 900;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .aem__title { color: #f1f5f9; }

.aem__subtitle {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  margin: 0.375rem 0 0;
  font-weight: 500;
  max-width: 600px;
  line-height: 1.5;
}

.aem__header-right { display: flex; align-items: center; gap: 0.5rem; }

/* ── Buttons ─────────────────────────────────────────────────────────────── */
.aem__btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.125rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
  white-space: nowrap;
}

.dark .aem__btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.aem__btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.dark .aem__btn:hover:not(:disabled) { box-shadow: 0 4px 12px rgba(0,0,0,0.2); }
.aem__btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }

.aem__btn--secondary:hover:not(:disabled) {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.dark .aem__btn--secondary:hover:not(:disabled) { background: rgba(79, 70, 229, 0.1); }

.aem__btn--sm { padding: 0.5rem 0.875rem; font-size: 0.8rem; }
.aem__spin { animation: aemSpin 1s linear infinite; }
@keyframes aemSpin { to { transform: rotate(360deg); } }

/* ── Error ─────────────────────────────────────────────────────────────── */
.aem__error {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: var(--ds-danger-soft);
  border: 1.5px solid rgba(220, 38, 38, 0.2);
  border-radius: var(--ds-radius-xl);
  color: var(--ds-danger);
  font-size: 0.875rem;
  font-weight: 600;
}

.aem__error span { flex: 1; }
.aem__error-close {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--ds-danger);
  display: flex;
  align-items: center;
  padding: 0.25rem;
}

/* ── Stats strip ─────────────────────────────────────────────────────────── */
.aem__stats-strip {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.aem__stat-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: var(--ds-radius-full);
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
}

.dark .aem__stat-chip {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.aem__stat-chip strong { color: var(--ds-text); font-weight: 800; font-family: var(--ds-font-display); }
.dark .aem__stat-chip strong { color: #f1f5f9; }

.aem__stat-chip--success { border-color: rgba(22, 163, 74, 0.3); background: var(--ds-success-soft); color: var(--ds-success); }
.aem__stat-chip--success strong { color: var(--ds-success); }

/* ── Table panel ─────────────────────────────────────────────────────────── */
.aem__table-panel {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.dark .aem__table-panel { border-color: var(--ds-border-strong); }

.aem__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
  flex-wrap: wrap;
  gap: 0.75rem;
}

.dark .aem__toolbar { border-bottom-color: var(--ds-border-strong); }

.aem__toolbar-meta {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  margin: 0;
  font-weight: 500;
}

.aem__toolbar-meta strong { color: var(--ds-text); font-weight: 700; }
.dark .aem__toolbar-meta strong { color: #f1f5f9; }

.aem__toolbar-actions { display: flex; align-items: center; gap: 0.5rem; }

.aem__toolbar-hint {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}

/* ── Table ─────────────────────────────────────────────────────────────── */
.aem__table-wrap { overflow-x: auto; }

.aem__table {
  width: 100%;
  border-collapse: collapse;
  min-width: 900px;
}

.aem__table th {
  padding: 0.75rem 1.25rem;
  text-align: left;
  font-size: 0.7rem;
  font-weight: 800;
  color: var(--ds-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
  white-space: nowrap;
}

.dark .aem__table th {
  background: var(--ds-gray-800);
  border-bottom-color: var(--ds-border-strong);
  color: #94a3b8;
}

.aem__th--id { width: 60px; }
.aem__th--center { text-align: center; }

.aem__table td {
  padding: 0.875rem 1.25rem;
  font-size: 0.875rem;
  border-bottom: 1px solid var(--ds-border);
  vertical-align: middle;
  color: var(--ds-text);
}

.dark .aem__table td { border-bottom-color: var(--ds-border-strong); color: #f1f5f9; }

.aem__td--id { color: var(--ds-text-muted); font-size: 0.8rem; font-family: var(--ds-font-display); }
.aem__td--center { text-align: center; }

.aem__row:hover td { background: var(--ds-gray-50); }
.dark .aem__row:hover td { background: var(--ds-gray-800); }

.aem__exam-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}

.dark .aem__exam-title { color: #f1f5f9; }

.aem__exam-desc {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}

.aem__code-badge {
  font-family: var(--ds-font-display);
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
  padding: 0.2rem 0.5rem;
  border-radius: var(--ds-radius-md);
}

.dark .aem__code-badge { background: rgba(79, 70, 229, 0.15); }

.aem__duration {
  font-family: var(--ds-font-display);
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--ds-text-muted);
}

/* Toggle button */
.aem__toggle-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.875rem;
  border-radius: var(--ds-radius-full);
  border: 1.5px solid;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
}

.aem__toggle-btn--on {
  background: var(--ds-success-soft);
  border-color: rgba(22, 163, 74, 0.3);
  color: var(--ds-success);
}

.aem__toggle-btn--on:hover:not(:disabled) {
  background: rgba(22, 163, 74, 0.2);
}

.aem__toggle-btn--off {
  background: var(--ds-gray-100);
  border-color: var(--ds-border);
  color: var(--ds-text-muted);
}

.dark .aem__toggle-btn--off { background: var(--ds-gray-800); border-color: var(--ds-border-strong); }

.aem__toggle-btn--off:hover:not(:disabled) {
  background: rgba(220, 38, 38, 0.08);
  border-color: rgba(220, 38, 38, 0.3);
  color: var(--ds-danger);
}

.aem__toggle-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.aem__toggle-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.aem__toggle-btn--on .aem__toggle-dot {
  background: var(--ds-success);
  box-shadow: 0 0 6px rgba(22, 163, 74, 0.5);
}

.aem__toggle-btn--off .aem__toggle-dot { background: var(--ds-gray-400); }

.aem__time-range {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8rem;
  min-width: 200px;
}

.aem__time-start { color: var(--ds-text-secondary); font-weight: 500; }
.aem__time-end { color: var(--ds-text-secondary); font-weight: 500; }
.aem__time-arrow { color: var(--ds-text-muted); font-size: 1rem; flex-shrink: 0; }

.aem__creator {
  font-family: var(--ds-font-display);
  font-size: 0.8rem;
  color: var(--ds-text-muted);
  font-weight: 600;
}

.aem__counts { display: flex; align-items: center; gap: 0.25rem; }

.aem__count-chip {
  font-family: var(--ds-font-display);
  font-size: 0.875rem;
  font-weight: 800;
}

.aem__count-chip--questions { color: var(--ds-primary); }
.aem__count-chip--attempts { color: var(--ds-success); }
.aem__count-sep { color: var(--ds-text-muted); font-size: 0.8rem; }

/* Empty / loading */
.aem__td--center { text-align: center; }

.aem__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 3rem 1rem;
  color: var(--ds-text-muted);
}

.aem__empty .lucide { font-size: 2.5rem; opacity: 0.3; }
.aem__empty p { margin: 0; font-size: 0.875rem; font-weight: 600; }

.aem__loading-cell {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.5rem 0;
}

.aem__skeleton-row { display: flex; align-items: center; gap: 1rem; justify-content: center; }

.aem__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-100) 25%, var(--ds-gray-200) 50%, var(--ds-gray-100) 75%);
  background-size: 200% 100%;
  animation: aemShimmer 1.5s infinite;
  border-radius: var(--ds-radius-md);
}

.dark .aem__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-800) 25%, var(--ds-gray-700) 50%, var(--ds-gray-800) 75%);
  background-size: 200% 100%;
}

@keyframes aemShimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.aem__skeleton--sm { width: 40px; height: 14px; }
.aem__skeleton--md { width: 100px; height: 14px; }
.aem__skeleton--lg { width: 200px; height: 14px; }

/* Pagination */
.aem__pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.875rem 1.25rem;
  border-top: 1px solid var(--ds-border);
  flex-wrap: wrap;
  gap: 0.75rem;
}

.dark .aem__pagination { border-top-color: var(--ds-border-strong); }

.aem__pagination-info {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  margin: 0;
  font-weight: 500;
}

.aem__pagination-info strong { color: var(--ds-text); font-weight: 700; }
.dark .aem__pagination-info strong { color: #f1f5f9; }

.aem__pagination-btns { display: flex; gap: 0.5rem; }
</style>
