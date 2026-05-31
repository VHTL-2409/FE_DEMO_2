<template>
  <div class="tui-page-wrap">

    <!-- Page Header -->
    <header class="tui-header">
      <div class="tui-header-left">
        <div class="tui-header-icon">
          <LucideIcon name="clipboard_list" :size="22" />
        </div>
        <div>
          <p class="tui-header-eyebrow">Quản lý đề thi</p>
          <h1 class="tui-header-title">Danh sách đề thi</h1>
          <p class="tui-header-desc">Theo dõi trạng thái, thời gian và thống kê đề thi trong hệ thống.</p>
        </div>
      </div>
      <div class="tui-header-right">
        <button type="button" class="tui-btn tui-btn--secondary" :disabled="loading" @click="load">
          <LucideIcon name="refresh" :size="14" :class="loading ? 'tui-spin' : ''" />
          Làm mới
        </button>
      </div>
    </header>

    <!-- Error Alert -->
    <div v-if="errorMsg" class="tui-alert tui-alert--danger" style="margin-top: 1rem">
      <LucideIcon name="alert_circle" :size="16" />
      <span>{{ errorMsg }}</span>
      <button type="button" @click="errorMsg = ''"><LucideIcon name="x" :size="14" /></button>
    </div>

    <!-- KPI Strip -->
    <div class="tui-stat-grid" style="margin: 1rem 1.5rem 0">
      <div class="tui-kpi-card tui-kpi-card--primary" style="animation-delay: 0s">
        <div class="tui-kpi-icon-wrap">
          <LucideIcon name="clipboard_list" :size="18" />
        </div>
        <div class="tui-kpi-body">
          <p class="tui-kpi-value">{{ formatNum(totalElements) }}</p>
          <p class="tui-kpi-label">Tổng đề thi</p>
        </div>
      </div>
      <div class="tui-kpi-card tui-kpi-card--success" style="animation-delay: 0.06s">
        <div class="tui-kpi-icon-wrap">
          <LucideIcon name="check_circle" :size="18" />
        </div>
        <div class="tui-kpi-body">
          <p class="tui-kpi-value">{{ formatNum(activeCount) }}</p>
          <p class="tui-kpi-label">Đang hoạt động</p>
        </div>
      </div>
      <div class="tui-kpi-card tui-kpi-card--warning" style="animation-delay: 0.12s">
        <div class="tui-kpi-icon-wrap">
          <LucideIcon name="file_text" :size="18" />
        </div>
        <div class="tui-kpi-body">
          <p class="tui-kpi-value">{{ formatNum(totalQuestions) }}</p>
          <p class="tui-kpi-label">Tổng câu hỏi</p>
        </div>
      </div>
      <div class="tui-kpi-card tui-kpi-card--info" style="animation-delay: 0.18s">
        <div class="tui-kpi-icon-wrap">
          <LucideIcon name="users" :size="18" />
        </div>
        <div class="tui-kpi-body">
          <p class="tui-kpi-value">{{ formatNum(totalAttempts) }}</p>
          <p class="tui-kpi-label">Tổng lượt thi</p>
        </div>
      </div>
    </div>

    <!-- Tab Filter -->
    <div class="tui-tabs" style="margin-top: 1rem">
      <button
        v-for="tab in statusTabs"
        :key="tab.key"
        type="button"
        class="tui-tab"
        :class="{ 'tui-tab--active': activeFilter === tab.key }"
        @click="activeFilter = tab.key"
      >
        {{ tab.label }}
        <span v-if="tab.count != null" style="margin-left: 0.3rem; font-size: 0.65rem; opacity: 0.75">({{ tab.count }})</span>
      </button>
    </div>

    <!-- Table Panel -->
    <div class="tui-panel tui-panel--anim" style="margin: 1rem 1.5rem 1.5rem; flex: 1; display: flex; flex-direction: column; min-height: 0; overflow: hidden">

      <!-- Table -->
      <div class="tui-table-wrap" style="flex: 1; overflow-y: auto">
        <table class="tui-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Tiêu đề đề thi</th>
              <th>Mã đề</th>
              <th>Phút</th>
              <th>Trạng thái</th>
              <th>Thời gian bắt đầu</th>
              <th>Người tạo</th>
              <th>Câu hỏi / Lượt thi</th>
            </tr>
          </thead>
          <tbody>
            <template v-if="loading && !rows.length">
              <tr v-for="n in 7" :key="n">
                <td colspan="8" style="padding: 0.5rem 1.25rem">
                  <div class="tui-skeleton" style="height: 44px; border-radius: 10px" />
                </td>
              </tr>
            </template>
            <template v-else-if="!rows.length">
              <tr>
                <td colspan="8" style="border-bottom: none">
                  <div class="tui-empty">
                    <div class="tui-empty-icon"><LucideIcon name="clipboard_list" :size="22" /></div>
                    <p class="tui-empty-title">Chưa có đề thi</p>
                    <p class="tui-empty-desc">Hệ thống chưa có đề thi nào.</p>
                  </div>
                </td>
              </tr>
            </template>
            <template v-else>
              <tr
                v-for="(row, idx) in filteredRows"
                :key="row.id"
                class="tui-table-row--anim"
                :style="{ animationDelay: `${idx * 0.04}s` }"
              >
                <td style="font-family: monospace; color: var(--ds-text-secondary); font-size: 0.72rem; white-space: nowrap">{{ row.id }}</td>
                <td>
                  <span style="font-weight: 700; font-size: 0.8rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px; display: block" :title="row.title">{{ row.title }}</span>
                </td>
                <td>
                  <span style="font-family: 'JetBrains Mono', monospace; font-size: 0.72rem; color: var(--ds-primary); font-weight: 700">{{ row.code || '—' }}</span>
                </td>
                <td style="color: var(--ds-text-secondary); font-size: 0.78rem">{{ row.durationMinutes ?? '—' }}</td>
                <td>
                  <button
                    type="button"
                    :disabled="togglingId === row.id"
                    class="tui-toggle"
                    :class="row.isActive ? 'tui-toggle--on' : 'tui-toggle--off'"
                    @click="toggleActive(row)"
                  >
                    <span class="tui-dot" :class="row.isActive ? 'tui-dot--up' : 'tui-dot--muted'" />
                    {{ row.isActive ? 'Đang bật' : 'Đang tắt' }}
                  </button>
                </td>
                <td style="font-size: 0.72rem; color: var(--ds-text-secondary); white-space: nowrap">
                  {{ formatDt(row.startTime) }}
                </td>
                <td>
                  <span style="font-family: monospace; font-size: 0.72rem; color: var(--ds-text-secondary)">{{ row.createdByUsername || '—' }}</span>
                </td>
                <td style="white-space: nowrap">
                  <span style="font-weight: 800; color: var(--ds-primary); font-size: 0.8rem">{{ row.questionCount }}</span>
                  <span style="color: var(--ds-text-secondary); font-size: 0.75rem"> / </span>
                  <span style="font-weight: 700; color: var(--ds-text); font-size: 0.8rem">{{ row.attemptCount }}</span>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="tui-pagination">
        <p class="tui-pagination-info">
          Trang <strong>{{ page + 1 }}</strong> / {{ totalPages }}
          &nbsp;&middot;&nbsp;
          {{ formatNum(totalElements) }} đề thi
        </p>
        <div class="tui-pagination-controls">
          <button type="button" class="tui-btn tui-btn--secondary tui-btn--sm" :disabled="page <= 0 || loading" @click="goPage(page - 1)">
            <LucideIcon name="chevron_left" :size="14" />
            Trước
          </button>
          <button type="button" class="tui-btn tui-btn--secondary tui-btn--sm" :disabled="page >= totalPages - 1 || loading" @click="goPage(page + 1)">
            Sau
            <LucideIcon name="chevron_right" :size="14" />
          </button>
        </div>
      </div>
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
const activeFilter = ref('all')

const activeCount = computed(() => rows.value.filter(r => r.isActive).length)
const totalQuestions = computed(() => rows.value.reduce((s, r) => s + (r.questionCount || 0), 0))
const totalAttempts = computed(() => rows.value.reduce((s, r) => s + (r.attemptCount || 0), 0))

const statusTabs = computed(() => [
  { key: 'all',      label: 'Tất cả',      count: totalElements.value || null },
  { key: 'active',   label: 'Đang bật',   count: activeCount.value },
  { key: 'inactive', label: 'Đang tắt',   count: rows.value.length - activeCount.value }
])

const filteredRows = computed(() => {
  if (activeFilter.value === 'active') return rows.value.filter(r => r.isActive)
  if (activeFilter.value === 'inactive') return rows.value.filter(r => !r.isActive)
  return rows.value
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
    errorMsg.value = e?.payload?.message || e?.message || 'Không tải được danh sách đề thi.'
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
    toast.success(next ? 'Đã bật đề thi.' : 'Đã tắt đề thi.')
  } catch (e) {
    toast.error(e?.payload?.message || e?.message || 'Không cập nhật được.')
  } finally {
    togglingId.value = null
  }
}

onMounted(load)
</script>
