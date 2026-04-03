<template>
  <div class="db-page-wrap">

    <!-- Page Header -->
    <header class="db-header">
      <div class="db-header-left">
        <div class="db-header-icon">
          <LucideIcon name="clipboard_list" :size="24" />
        </div>
        <div>
          <p class="db-eyebrow">Exam governance</p>
          <h1 class="db-title">Quan ly de thi</h1>
          <p class="db-subtitle">Xem toan bo de trong he thong, trang thai hoat dong, thoi gian va thong ke cau hoi / luot thi.</p>
        </div>
      </div>
    </header>

    <!-- Stats Strip -->
    <div class="db-stats-strip">
      <div class="db-stats-strip__inner">
        <div
          v-for="(stat, i) in quickStats"
          :key="stat.label"
          class="db-stats-strip__item"
          :style="{ animationDelay: `${0.05 + i * 0.08}s` }"
        >
          <LucideIcon :name="stat.icon" :size="16" class="db-stats-strip__icon" :class="`db-stats-strip__icon--${stat.color}`" />
          <div>
            <p class="db-stats-strip__val">{{ formatNum(stat.value) }}</p>
            <p class="db-stats-strip__label">{{ stat.label }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Table Panel -->
    <div class="db-table-panel">
      <!-- Toolbar -->
      <div class="db-toolbar">
        <p class="db-toolbar-meta">
          <span style="font-weight: 700; color: var(--db-text)">{{ formatNum(totalElements) }}</span> de thi
        </p>
        <div class="db-toolbar-right">
          <button
            type="button"
            class="db-btn db-btn--secondary db-btn--sm"
            :disabled="loading"
            @click="load"
          >
            <LucideIcon name="refresh" :size="14" />
            Lam moi
          </button>
        </div>
      </div>

      <!-- Table -->
      <div class="db-table-wrap">
        <table class="db-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Tieu de</th>
              <th>Ma</th>
              <th>Phut</th>
              <th>Hoat dong</th>
              <th>Thoi gian</th>
              <th>GMT</th>
              <th>CH / Luot</th>
            </tr>
          </thead>
          <tbody>
            <template v-if="loading && !rows.length">
              <tr>
                <td colspan="8" class="db-empty-cell">
                  <div style="display: flex; justify-content: center; padding: 3rem">
                    <LucideIcon name="loader_2" :size="28" class="db-spin" style="color: var(--db-text-muted)" />
                  </div>
                </td>
              </tr>
            </template>
            <template v-else-if="!rows.length">
              <tr>
                <td colspan="8" class="db-empty-cell">
                  <div class="db-empty">
                    <div class="db-empty-icon">
                      <LucideIcon name="clipboard_list" :size="24" />
                    </div>
                    <p class="db-empty-title">Chua co de thi</p>
                    <p class="db-empty-desc">He thong chua co de thi nao.</p>
                  </div>
                </td>
              </tr>
            </template>
            <template v-else>
              <tr
                v-for="(row, idx) in rows"
                :key="row.id"
                class="db-table-row"
                :style="{ animationDelay: `${idx * 0.03}s` }"
              >
                <td style="color: var(--db-text-muted); font-size: 0.8rem">{{ row.id }}</td>
                <td class="truncate" style="max-width: 200px; font-weight: 600; color: var(--db-text)" :title="row.title">
                  {{ row.title }}
                </td>
                <td class="text-mono" style="color: var(--db-primary); font-size: 0.75rem">{{ row.code || '—' }}</td>
                <td style="color: var(--db-text-muted)">{{ row.durationMinutes ?? '—' }}</td>
                <td>
                  <button
                    type="button"
                    :disabled="togglingId === row.id"
                    class="db-exam-toggle"
                    :class="row.isActive ? 'db-exam-toggle--on' : 'db-exam-toggle--off'"
                    @click="toggleActive(row)"
                  >
                    <span
                      class="db-exam-toggle__dot"
                      :class="row.isActive ? 'db-exam-toggle__dot--on' : 'db-exam-toggle__dot--off'"
                    />
                    {{ row.isActive ? 'Bat' : 'Tat' }}
                  </button>
                </td>
                <td style="color: var(--db-text-muted); font-size: 0.75rem; line-height: 1.6; white-space: nowrap">
                  {{ formatDt(row.startTime) }}
                </td>
                <td class="text-mono" style="color: var(--db-text-muted); font-size: 0.75rem">{{ row.createdByUsername || '—' }}</td>
                <td style="color: var(--db-text-muted); font-size: 0.8rem">
                  <span style="font-weight: 700; color: var(--db-primary)">{{ row.questionCount }}</span>
                  <span style="color: var(--db-text-muted)"> / </span>
                  <span style="font-weight: 700; color: var(--db-text)">{{ row.attemptCount }}</span>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="db-pagination">
        <p class="db-pagination-info">
          Trang <span style="font-weight: 700; color: var(--db-text)">{{ page + 1 }}</span> / {{ totalPages }}
        </p>
        <div class="db-pagination-controls">
          <button
            type="button"
            :disabled="page <= 0 || loading"
            class="db-btn db-btn--secondary db-btn--sm"
            @click="goPage(page - 1)"
          >
            Truoc
          </button>
          <button
            type="button"
            :disabled="page >= totalPages - 1 || loading"
            class="db-btn db-btn--secondary db-btn--sm"
            @click="goPage(page + 1)"
          >
            Sau
          </button>
        </div>
      </div>
    </div>

    <!-- Error -->
    <div v-if="errorMsg" class="db-alert db-alert--danger">
      <LucideIcon name="alert_circle" :size="18" />
      <span>{{ errorMsg }}</span>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
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

const quickStats = computed(() => {
  const total = totalElements.value
  const active = rows.value.filter(r => r.isActive).length
  const inactive = total - active
  const totalAttempts = rows.value.reduce((sum, r) => sum + (r.attemptCount || 0), 0)
  const totalQuestions = rows.value.reduce((sum, r) => sum + (r.questionCount || 0), 0)
  return [
    { label: 'Tong de', value: total, icon: 'clipboard_list', color: 'primary' },
    { label: 'Dang bat', value: active, icon: 'check_circle', color: 'success' },
    { label: 'Dang tat', value: inactive, icon: 'x_circle', color: 'muted' },
    { label: 'Tong luot thi', value: totalAttempts, icon: 'users', color: 'warning' }
  ]
})

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

const formatNum = (n) => (n == null ? '—' : new Intl.NumberFormat('vi-VN').format(n))

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
    errorMsg.value = e?.payload?.message || e?.message || 'Khong tai duoc danh sach de thi.'
    rows.value = []
  } finally {
    loading.value = false
  }
}

const goPage = (p) => {
  page.value = Math.max(0, p)
  load()
}

const toggleActive = async (row) => {
  if (togglingId.value != null) return
  const next = !row.isActive
  togglingId.value = row.id
  try {
    await patchAdminExamActive(row.id, next)
    row.isActive = next
    toast.success(next ? 'Da bat de thi.' : 'Da tat de thi.')
  } catch (e) {
    toast.error(e?.payload?.message || e?.message || 'Khong cap nhat duoc.')
  } finally {
    togglingId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
/* Stats strip */
.db-stats-strip {
  animation: fadeInUp 0.4s ease backwards;
  animation-delay: 0.1s;
}

.db-stats-strip__inner {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.75rem;
}

@media (max-width: 640px) {
  .db-stats-strip__inner { grid-template-columns: repeat(2, 1fr); }
}

.db-stats-strip__item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  background: rgba(17, 17, 19, 0.85);
  border: 1px solid var(--db-border-strong);
  border-radius: var(--db-radius-lg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  transition: all var(--db-transition);
  animation: fadeInUp 0.4s ease backwards;
}

.db-stats-strip__item:hover {
  border-color: var(--db-border-accent);
  transform: translateY(-2px);
}

.db-stats-strip__icon {
  flex-shrink: 0;
}

.db-stats-strip__icon--primary { color: var(--db-primary); }
.db-stats-strip__icon--success { color: var(--db-success); }
.db-stats-strip__icon--warning { color: var(--db-warning); }
.db-stats-strip__icon--muted { color: var(--db-text-muted); }

.db-stats-strip__val {
  font-family: var(--db-font);
  font-size: 1.25rem;
  font-weight: 900;
  color: var(--db-text);
  margin: 0;
  line-height: 1;
  letter-spacing: -0.02em;
}

.db-stats-strip__label {
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--db-text-muted);
  margin: 0.2rem 0 0;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  white-space: nowrap;
}

/* Table rows */
.db-table-row {
  animation: fadeInUp 0.3s ease backwards;
}

.db-empty-cell {
  border-bottom: none !important;
  padding: 0 !important;
}

/* Exam toggle */
.db-exam-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.875rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all var(--db-transition);
  border: 1.5px solid transparent;
  font-family: var(--db-font);
}

.db-exam-toggle:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.db-exam-toggle--on {
  background: rgba(16, 185, 129, 0.12);
  border-color: rgba(16, 185, 129, 0.3);
  color: var(--db-success);
}

.db-exam-toggle--on:hover:not(:disabled) {
  background: rgba(16, 185, 129, 0.2);
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.25);
}

.db-exam-toggle--off {
  background: var(--db-surface-3);
  border-color: var(--db-border-strong);
  color: var(--db-text-muted);
}

.db-exam-toggle--off:hover:not(:disabled) {
  background: var(--db-surface-2);
  color: var(--db-text);
  transform: translateY(-2px) scale(1.05);
}

.db-exam-toggle__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.db-exam-toggle__dot--on {
  background: var(--db-success);
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.7);
  animation: pulseRingSuccess 2.5s infinite;
}

.db-exam-toggle__dot--off {
  background: var(--db-text-muted);
}

.db-exam-toggle__dot--on {
  background: var(--db-success);
  animation: pulseRingSuccess 2.5s infinite;
}
</style>
