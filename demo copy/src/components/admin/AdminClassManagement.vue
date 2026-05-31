<template>
  <div class="tui-page-wrap">

    <header class="tui-header">
      <div class="tui-header-left">
        <div class="tui-header-icon">
          <LucideIcon name="grid_view" :size="22" />
        </div>
        <div>
          <p class="tui-header-eyebrow">Quản lý lớp</p>
          <h1 class="tui-header-title">Danh sách lớp toàn hệ thống</h1>
          <p class="tui-header-desc">Theo dõi lớp học, mã lớp, giáo viên phụ trách và sĩ số.</p>
        </div>
      </div>
      <div class="tui-header-right">
        <button type="button" class="tui-btn tui-btn--secondary" :disabled="loading" @click="load">
          <LucideIcon name="refresh" :size="14" :class="loading ? 'tui-spin' : ''" />
          Làm mới
        </button>
      </div>
    </header>

    <div v-if="errorMsg" class="tui-alert tui-alert--danger" style="margin-top: 1rem">
      <LucideIcon name="alert_circle" :size="16" />
      <span>{{ errorMsg }}</span>
      <button type="button" @click="errorMsg = ''"><LucideIcon name="x" :size="14" /></button>
    </div>

    <div class="tui-stat-grid" style="margin: 1rem 1.5rem 0">
      <div class="tui-kpi-card tui-kpi-card--primary" style="animation-delay: 0s">
        <div class="tui-kpi-icon-wrap">
          <LucideIcon name="grid_view" :size="18" />
        </div>
        <div class="tui-kpi-body">
          <p class="tui-kpi-value">{{ formatNum(totalElements) }}</p>
          <p class="tui-kpi-label">Tổng lớp</p>
        </div>
      </div>
      <div class="tui-kpi-card tui-kpi-card--success" style="animation-delay: 0.06s">
        <div class="tui-kpi-icon-wrap">
          <LucideIcon name="users" :size="18" />
        </div>
        <div class="tui-kpi-body">
          <p class="tui-kpi-value">{{ formatNum(totalStudentsOnPage) }}</p>
          <p class="tui-kpi-label">HS trên trang này</p>
        </div>
      </div>
    </div>

    <div class="tui-panel tui-panel--anim" style="margin: 1rem 1.5rem 1.5rem; flex: 1; display: flex; flex-direction: column; min-height: 0; overflow: hidden">
      <div class="tui-table-wrap" style="flex: 1; overflow-y: auto">
        <table class="tui-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Tên lớp</th>
              <th>Môn</th>
              <th>Mã lớp</th>
              <th>Giáo viên</th>
              <th>Sĩ số</th>
              <th>Cập nhật</th>
            </tr>
          </thead>
          <tbody>
            <template v-if="loading && !rows.length">
              <tr v-for="n in 6" :key="n">
                <td colspan="7" style="padding: 0.5rem 1.25rem">
                  <div class="tui-skeleton" style="height: 44px; border-radius: 10px" />
                </td>
              </tr>
            </template>
            <template v-else-if="!rows.length">
              <tr>
                <td colspan="7" style="border-bottom: none">
                  <div class="tui-empty">
                    <div class="tui-empty-icon"><LucideIcon name="grid_view" :size="22" /></div>
                    <p class="tui-empty-title">Chưa có lớp</p>
                    <p class="tui-empty-desc">Hệ thống chưa có lớp học nào.</p>
                  </div>
                </td>
              </tr>
            </template>
            <template v-else>
              <tr
                v-for="(row, idx) in rows"
                :key="row.id"
                class="tui-table-row--anim"
                :style="{ animationDelay: `${idx * 0.04}s` }"
              >
                <td style="font-family: monospace; color: var(--ds-text-secondary); font-size: 0.72rem; white-space: nowrap">{{ row.id }}</td>
                <td>
                  <span style="font-weight: 700; font-size: 0.8rem; display: block; max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap" :title="row.name">{{ row.name }}</span>
                </td>
                <td style="font-size: 0.78rem; color: var(--ds-text-secondary)">{{ row.subject || '—' }}</td>
                <td>
                  <span style="font-family: 'JetBrains Mono', monospace; font-size: 0.72rem; color: var(--ds-primary); font-weight: 700">{{ row.classCode || '—' }}</span>
                </td>
                <td style="font-family: monospace; font-size: 0.72rem; color: var(--ds-text-secondary)">{{ row.teacherName || '—' }}</td>
                <td style="font-weight: 800; color: var(--ds-primary); font-size: 0.8rem">{{ formatNum(row.studentCount) }}</td>
                <td style="font-size: 0.72rem; color: var(--ds-text-secondary); white-space: nowrap">{{ formatDt(row.updatedAt) }}</td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <div v-if="totalPages > 1" class="tui-pagination">
        <p class="tui-pagination-info">
          Trang <strong>{{ page + 1 }}</strong> / {{ totalPages }}
          &nbsp;&middot;&nbsp;
          {{ formatNum(totalElements) }} lớp
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
import { fetchAdminClasses } from '../../services/adminService'
import LucideIcon from '../common/LucideIcon.vue'

const loading = ref(true)
const errorMsg = ref('')
const rows = ref([])
const page = ref(0)
const size = ref(20)
const totalElements = ref(0)
const totalPages = ref(0)

const totalStudentsOnPage = computed(() =>
  rows.value.reduce((s, r) => s + (Number(r.studentCount) || 0), 0)
)

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
    const data = await fetchAdminClasses({ page: page.value, size: size.value })
    rows.value = data.content || []
    totalElements.value = data.totalElements ?? 0
    totalPages.value = data.totalPages ?? 0
    page.value = data.page ?? page.value
    size.value = data.size ?? size.value
  } catch (e) {
    errorMsg.value = e?.payload?.message || e?.message || 'Không tải được danh sách lớp.'
    rows.value = []
  } finally {
    loading.value = false
  }
}

const goPage = (p) => {
  page.value = Math.max(0, p)
  load()
}

onMounted(load)
</script>
