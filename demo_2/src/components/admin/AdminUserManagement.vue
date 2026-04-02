<template>
  <div class="aum">
    <div class="aum__inner">

      <!-- Header -->
      <header class="aum__header">
        <div class="aum__header-left">
          <nav class="aum__breadcrumb" aria-label="Breadcrumb">
            <RouterLink to="/admin/dashboard" class="aum__breadcrumb-link">Tổng quan</RouterLink>
            <span class="aum__breadcrumb-sep">/</span>
            <span class="aum__breadcrumb-current">{{ sectionEyebrow }}</span>
          </nav>
          <h1 class="aum__title">{{ sectionTitle }}</h1>
          <p class="aum__subtitle">{{ sectionSubtitle }}</p>
        </div>
        <div class="aum__header-actions">
          <button
            type="button"
            class="aum__btn aum__btn--secondary"
            :disabled="loading"
            @click="load"
          >
            <LucideIcon name="refresh" :class="{ 'aum__spin': loading }" />
            <span>Làm mới</span>
          </button>
        </div>
      </header>

      <!-- Search bar -->
      <div v-if="showSearch" class="aum__search-bar">
        <div class="aum__search-input-wrap">
          <LucideIcon name="search" class="aum__search-icon" />
          <input
            v-model="searchInput"
            type="search"
            autocomplete="off"
            placeholder="Tìm theo username, email, họ tên..."
            class="aum__search-input"
            @input="scheduleSearchFromInput"
            @keydown.enter.prevent="applySearch"
          />
          <button v-if="searchInput" type="button" class="aum__search-clear" @click="clearSearch">
            <LucideIcon name="x" />
          </button>
        </div>
        <button
          type="button"
          class="aum__btn aum__btn--primary"
          @click="applySearch"
        >
          <LucideIcon name="search" />
          <span>Tìm kiếm</span>
        </button>
      </div>

      <!-- Error -->
      <div v-if="errorMsg" class="aum__error">
        <LucideIcon name="alert_circle" />
        <span>{{ errorMsg }}</span>
        <button type="button" class="aum__error-close" @click="errorMsg = ''">
          <LucideIcon name="x" />
        </button>
      </div>

      <!-- Table panel -->
      <div class="aum__table-panel">
        <!-- Toolbar -->
        <div class="aum__toolbar">
          <p class="aum__toolbar-meta">
            <strong>{{ totalElements.toLocaleString('vi-VN') }}</strong>
            {{ sectionEyebrow.toLowerCase() }}
            <span v-if="appliedQuery" class="aum__toolbar-filter"> · Lọc: "{{ appliedQuery }}"</span>
          </p>
        </div>

        <!-- Table -->
        <div class="aum__table-wrap">
          <table class="aum__table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Username</th>
                <th>Email</th>
                <th>Tên</th>
                <th>Đã xác minh</th>
                <th>Trạng thái</th>
                <th v-if="showActions">Hành động</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="loading && !rows.length">
                <tr>
                  <td :colspan="colspanRows" class="aum__td--center">
                    <div class="aum__loading-row">
                      <div class="aum__skeleton aum__skeleton--sm" />
                      <div class="aum__skeleton aum__skeleton--md" />
                      <div class="aum__skeleton aum__skeleton--lg" />
                    </div>
                  </td>
                </tr>
              </template>
              <template v-else-if="!rows.length">
                <tr>
                  <td :colspan="colspanRows" class="aum__td--center aum__empty-cell">
                    <div class="aum__empty">
                      <LucideIcon name="group_off" />
                      <p>Chưa có tài khoản nào.</p>
                    </div>
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="row in rows" :key="row.userId" class="aum__row">
                  <td class="aum__td aum__td--mono aum__td--id">{{ row.userId }}</td>
                  <td class="aum__td aum__td--mono aum__td--primary">{{ row.username || '—' }}</td>
                  <td class="aum__td">{{ row.email || '—' }}</td>
                  <td class="aum__td">{{ displayName(row) }}</td>
                  <td class="aum__td">
                    <span class="aum__badge" :class="row.emailVerified ? 'aum__badge--success' : 'aum__badge--muted'">
                      <LucideIcon :name="row.emailVerified ? 'check_circle' : 'radio_button_unchecked'" />
                      {{ row.emailVerified ? 'Đã xác minh' : 'Chưa' }}
                    </span>
                  </td>
                  <td class="aum__td">
                    <span
                      class="aum__status-dot"
                      :class="row.disabled || row.isLocked ? 'aum__status-dot--danger' : 'aum__status-dot--success'"
                    >
                      <span class="aum__status-pulse" />
                      {{ row.disabled || row.isLocked ? 'Đã khóa' : 'Hoạt động' }}
                    </span>
                  </td>
                  <td v-if="showActions" class="aum__td aum__td--actions">
                    <div class="aum__actions">
                      <button
                        type="button"
                        class="aum__action-btn aum__action-btn--view"
                        title="Xem chi tiết"
                        @click="openDetail(row)"
                      >
                        <LucideIcon name="visibility" />
                      </button>
                      <button
                        v-if="variant !== 'admins'"
                        type="button"
                        class="aum__action-btn aum__action-btn--danger"
                        title="Xóa tài khoản"
                        @click="confirmDelete(row)"
                      >
                        <LucideIcon name="delete" />
                      </button>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="aum__pagination">
          <p class="aum__pagination-info">
            Trang <strong>{{ page + 1 }}</strong> / {{ totalPages }}
          </p>
          <div class="aum__pagination-btns">
            <button
              type="button"
              class="aum__btn aum__btn--secondary aum__btn--sm"
              :disabled="page <= 0 || loading"
              @click="goPage(page - 1)"
            >
              <LucideIcon name="chevron_left" />
              Trước
            </button>
            <button
              type="button"
              class="aum__btn aum__btn--secondary aum__btn--sm"
              :disabled="page >= totalPages - 1 || loading"
              @click="goPage(page + 1)"
            >
              Sau
              <LucideIcon name="chevron_right" />
            </button>
          </div>
        </div>
      </div>

      <!-- Detail Modal -->
      <Teleport to="body">
        <Transition name="aum-modal">
          <div v-if="detailOpen" class="aum__modal-overlay" @click.self="closeDetail">
            <div class="aum__modal" role="dialog" aria-modal="true">
              <!-- Modal header -->
              <div class="aum__modal-header">
                <div class="aum__modal-header-left">
                  <div class="aum__modal-icon">
                    <LucideIcon name="account_circle" />
                  </div>
                  <div>
                    <h3 class="aum__modal-title">Chi tiết tài khoản</h3>
                    <p class="aum__modal-subtitle">{{ sectionEyebrow }}</p>
                  </div>
                </div>
                <button
                  type="button"
                  class="aum__modal-close"
                  @click="closeDetail"
                >
                  <LucideIcon name="close" />
                </button>
              </div>

              <!-- Modal body -->
              <div class="aum__modal-body">
                <div v-if="detailLoading" class="aum__modal-loading">
                  <div v-for="i in 6" :key="i" class="aum__skeleton-row">
                    <div class="aum__skeleton aum__skeleton--label" />
                    <div class="aum__skeleton aum__skeleton--value" />
                  </div>
                </div>
                <div v-else-if="detail" class="aum__detail-list">
                  <div class="aum__detail-avatar" v-if="detail.avatarUrl">
                    <img :src="detail.avatarUrl" alt="" class="aum__detail-avatar-img" />
                  </div>

                  <div class="aum__detail-row">
                    <span class="aum__detail-label">ID</span>
                    <span class="aum__detail-value aum__detail-value--mono">{{ detail.userId }}</span>
                  </div>
                  <div class="aum__detail-row">
                    <span class="aum__detail-label">Username</span>
                    <span class="aum__detail-value aum__detail-value--mono aum__detail-value--primary">{{ detail.username }}</span>
                  </div>
                  <div class="aum__detail-row">
                    <span class="aum__detail-label">Email</span>
                    <span class="aum__detail-value">{{ detail.email }}</span>
                  </div>
                  <div class="aum__detail-row">
                    <span class="aum__detail-label">Xác minh email</span>
                    <span class="aum__detail-value">
                      <span class="aum__badge" :class="detail.emailVerified ? 'aum__badge--success' : 'aum__badge--muted'">
                        <LucideIcon :name="detail.emailVerified ? 'check_circle' : 'radio_button_unchecked'" />
                        {{ detail.emailVerified ? 'Đã xác minh' : 'Chưa' }}
                      </span>
                    </span>
                  </div>
                  <div class="aum__detail-row">
                    <span class="aum__detail-label">Họ tên</span>
                    <span class="aum__detail-value">{{ detail.fullName || '—' }}</span>
                  </div>
                  <div class="aum__detail-row">
                    <span class="aum__detail-label">Tên hiển thị</span>
                    <span class="aum__detail-value">{{ detail.displayName || '—' }}</span>
                  </div>
                  <div class="aum__detail-row">
                    <span class="aum__detail-label">Điện thoại</span>
                    <span class="aum__detail-value">{{ detail.phone || '—' }}</span>
                  </div>
                  <div class="aum__detail-row">
                    <span class="aum__detail-label">Ngày sinh</span>
                    <span class="aum__detail-value">{{ formatDob(detail.dateOfBirth) }}</span>
                  </div>
                  <div class="aum__detail-row">
                    <span class="aum__detail-label">Trạng thái</span>
                    <span class="aum__detail-value">
                      <span class="aum__status-dot" :class="detail.disabled || detail.isLocked ? 'aum__status-dot--danger' : 'aum__status-dot--success'">
                        <span class="aum__status-pulse" />
                        {{ detail.disabled || detail.isLocked ? 'Đã khóa' : 'Hoạt động' }}
                      </span>
                    </span>
                  </div>
                </div>

                <!-- Modal footer -->
                <div v-if="!detailLoading && detail" class="aum__modal-footer">
                  <button
                    v-if="variant !== 'admins'"
                    type="button"
                    class="aum__btn aum__btn--danger"
                    @click="confirmDelete(detail); closeDetail()"
                  >
                    <LucideIcon name="delete" />
                    Xóa tài khoản
                  </button>
                  <button
                    type="button"
                    class="aum__btn aum__btn--secondary"
                    @click="closeDetail"
                  >
                    Đóng
                  </button>
                </div>
              </div>
            </div>
          </div>
        </Transition>
      </Teleport>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { RouterLink } from 'vue-router'
import {
  fetchAdminStudents,
  fetchAdminTeachers,
  fetchAdminAdmins,
  fetchAdminStudentDetail,
  fetchAdminTeacherDetail,
  deleteAdminStudent,
  deleteAdminTeacher
} from '../../services/adminService'
import { useToast } from '../../composables/useToast'
import LucideIcon from '../common/LucideIcon.vue'

const toast = useToast()

const SEARCH_DEBOUNCE_MS = 380
let searchDebounceTimer = null

const props = defineProps({
  variant: {
    type: String,
    required: true,
    validator: (v) => v === 'students' || v === 'teachers' || v === 'admins'
  }
})

const sectionEyebrow = computed(() => {
  if (props.variant === 'students') return 'Học sinh'
  if (props.variant === 'teachers') return 'Giáo viên'
  return 'Quản trị viên'
})

const sectionTitle = computed(() => {
  if (props.variant === 'students') return 'Quản lý học sinh'
  if (props.variant === 'teachers') return 'Quản lý giáo viên'
  return 'Quản lý tài khoản admin'
})

const sectionSubtitle = computed(() => {
  if (props.variant === 'students') return 'Danh sách phân trang, tìm theo từ khóa, xem chi tiết và xóa khi cần.'
  if (props.variant === 'teachers') return 'Theo dõi giáo viên đã đăng ký — tìm kiếm, chi tiết hồ sơ, gỡ tài khoản.'
  return 'Chỉ xem danh sách quản trị viên; không có thao tác xóa từ màn này.'
})

const showSearch = computed(() => props.variant === 'students' || props.variant === 'teachers')
const showActions = computed(() => props.variant === 'students' || props.variant === 'teachers')
const colspanRows = computed(() => (showActions.value ? 7 : 6))

const loading = ref(true)
const errorMsg = ref('')
const rows = ref([])
const page = ref(0)
const size = ref(20)
const totalElements = ref(0)
const totalPages = ref(0)

const searchInput = ref('')
const appliedQuery = ref('')

const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const displayName = (row) => {
  const full = row.fullName?.trim()
  const disp = row.displayName?.trim()
  if (full && disp) return `${full} (${disp})`
  return full || disp || '—'
}

const formatDob = (d) => {
  if (d == null || d === '') return '—'
  return String(d)
}

const load = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    const fetcher =
      props.variant === 'students'
        ? fetchAdminStudents
        : props.variant === 'teachers'
          ? fetchAdminTeachers
          : fetchAdminAdmins
    const params = { page: page.value, size: size.value }
    if (showSearch.value && appliedQuery.value) {
      params.q = appliedQuery.value
    }
    const data = await fetcher(params)
    rows.value = data.content || []
    totalElements.value = data.totalElements ?? 0
    totalPages.value = data.totalPages ?? 0
    page.value = data.page ?? page.value
    size.value = data.size ?? size.value
  } catch (e) {
    errorMsg.value = e?.payload?.message || e?.message || 'Không tải được danh sách.'
    rows.value = []
  } finally {
    loading.value = false
  }
}

const applySearch = () => {
  clearTimeout(searchDebounceTimer)
  searchDebounceTimer = null
  appliedQuery.value = searchInput.value.trim()
  page.value = 0
  void load()
}

const scheduleSearchFromInput = () => {
  if (!showSearch.value) return
  clearTimeout(searchDebounceTimer)
  searchDebounceTimer = setTimeout(() => {
    searchDebounceTimer = null
    const q = searchInput.value.trim()
    if (q === appliedQuery.value) return
    appliedQuery.value = q
    page.value = 0
    void load()
  }, SEARCH_DEBOUNCE_MS)
}

const clearSearch = () => {
  clearTimeout(searchDebounceTimer)
  searchDebounceTimer = null
  searchInput.value = ''
  appliedQuery.value = ''
  page.value = 0
  void load()
}

const goPage = (p) => {
  page.value = Math.max(0, p)
  void load()
}

const openDetail = async (row) => {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  try {
    if (props.variant === 'students') {
      detail.value = await fetchAdminStudentDetail(row.userId)
    } else {
      detail.value = await fetchAdminTeacherDetail(row.userId)
    }
  } catch (e) {
    toast.error(e?.payload?.message || e?.message || 'Không tải được chi tiết.')
    detailOpen.value = false
  } finally {
    detailLoading.value = false
  }
}

const closeDetail = () => {
  detailOpen.value = false
  detail.value = null
}

const confirmDelete = (row) => {
  const label = props.variant === 'students' ? 'học sinh' : 'giáo viên'
  const msg = `Xóa ${label} "${row.username}"? Thao tác không hoàn tác.`
  if (!window.confirm(msg)) return
  void doDelete(row.userId)
}

const doDelete = async (userId) => {
  try {
    if (props.variant === 'students') {
      await deleteAdminStudent(userId)
    } else {
      await deleteAdminTeacher(userId)
    }
    toast.success('Đã xóa tài khoản.')
    if (detail.value?.userId === userId) closeDetail()
    await load()
  } catch (e) {
    toast.error(e?.payload?.message || e?.message || 'Không xóa được.')
  }
}

watch(() => props.variant, () => {
  clearSearch()
  void load()
}, { immediate: false })

onMounted(() => {
  void load()
})

onUnmounted(() => {
  clearTimeout(searchDebounceTimer)
})
</script>

<style scoped>
/* ── Page ─────────────────────────────────────────────────────────────────── */
.aum {
  min-height: 100vh;
  background: var(--ds-bg);
}

.aum__inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 1.5rem 1.5rem 2.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* ── Header ────────────────────────────────────────────────────────────── */
.aum__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.aum__header-left { flex: 1; min-width: 0; }

/* Breadcrumb */
.aum__breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  margin-bottom: 0.375rem;
}

.aum__breadcrumb-link {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  text-decoration: none;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  transition: color 0.12s ease;
}

.aum__breadcrumb-link:hover { color: var(--ds-primary); }

.aum__breadcrumb-sep {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
}

.aum__breadcrumb-current {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--ds-text);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.dark .aum__breadcrumb-current { color: #94a3b8; }

.aum__title {
  font-family: var(--ds-font-display);
  font-size: 1.5rem;
  font-weight: 900;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .aum__title { color: #f1f5f9; }

.aum__subtitle {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  margin: 0.375rem 0 0;
  font-weight: 500;
  max-width: 600px;
  line-height: 1.5;
}

.aum__header-actions { display: flex; align-items: center; gap: 0.5rem; }

/* ── Buttons ──────────────────────────────────────────────────────────── */
.aum__btn {
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

.dark .aum__btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.aum__btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.dark .aum__btn:hover:not(:disabled) { box-shadow: 0 4px 12px rgba(0,0,0,0.2); }
.aum__btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }

.aum__btn--primary {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: white;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.25);
}

.aum__btn--primary:hover:not(:disabled) {
  background: var(--ds-primary-hover);
  color: white;
  border-color: var(--ds-primary-hover);
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.35);
}

.aum__btn--secondary:hover:not(:disabled) {
  border-color: var(--ds-primary);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.dark .aum__btn--secondary:hover:not(:disabled) { background: rgba(79, 70, 229, 0.1); }

.aum__btn--danger {
  background: var(--ds-danger);
  border-color: var(--ds-danger);
  color: white;
}

.aum__btn--danger:hover:not(:disabled) {
  background: #dc2626;
  border-color: #dc2626;
  box-shadow: 0 4px 16px rgba(220, 38, 38, 0.3);
}

.aum__btn--sm { padding: 0.5rem 0.875rem; font-size: 0.8rem; }
.aum__spin { animation: aumSpin 1s linear infinite; }
@keyframes aumSpin { to { transform: rotate(360deg); } }

/* ── Search bar ────────────────────────────────────────────────────────── */
.aum__search-bar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.aum__search-input-wrap {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  flex: 1;
  min-width: 240px;
  max-width: 480px;
  padding: 0.625rem 1rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  transition: all 0.15s ease;
}

.dark .aum__search-input-wrap {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.aum__search-input-wrap:focus-within {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 4px var(--ds-primary-ring);
}

.aum__search-icon { color: var(--ds-text-muted); flex-shrink: 0; }

.aum__search-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 0.875rem;
  color: var(--ds-text);
  min-width: 0;
}

.dark .aum__search-input { color: #f1f5f9; }
.aum__search-input::placeholder { color: var(--ds-text-muted); }

.aum__search-clear {
  width: 1.5rem;
  height: 1.5rem;
  border: none;
  border-radius: 50%;
  background: var(--ds-gray-100);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.12s ease;
}

.dark .aum__search-clear { background: var(--ds-gray-700); }
.aum__search-clear:hover { background: var(--ds-gray-200); color: var(--ds-text); }
.dark .aum__search-clear:hover { background: var(--ds-gray-600); }

/* ── Error ──────────────────────────────────────────────────────────── */
.aum__error {
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

.aum__error span { flex: 1; }
.aum__error-close {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--ds-danger);
  display: flex;
  align-items: center;
  padding: 0.25rem;
}

/* ── Table Panel ─────────────────────────────────────────────────────── */
.aum__table-panel {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.dark .aum__table-panel { border-color: var(--ds-border-strong); }

.aum__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
}

.dark .aum__toolbar { border-bottom-color: var(--ds-border-strong); }

.aum__toolbar-meta {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  margin: 0;
  font-weight: 500;
}

.aum__toolbar-meta strong { color: var(--ds-text); font-weight: 700; }
.dark .aum__toolbar-meta strong { color: #f1f5f9; }
.aum__toolbar-filter { color: var(--ds-primary); }

.aum__table-wrap { overflow-x: auto; }

.aum__table {
  width: 100%;
  border-collapse: collapse;
  min-width: 800px;
}

.aum__table th {
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

.dark .aum__table th {
  background: var(--ds-gray-800);
  border-bottom-color: var(--ds-border-strong);
  color: #94a3b8;
}

.aum__table td {
  padding: 0.875rem 1.25rem;
  font-size: 0.875rem;
  border-bottom: 1px solid var(--ds-border);
  vertical-align: middle;
  color: var(--ds-text);
}

.dark .aum__table td { border-bottom-color: var(--ds-border-strong); color: #f1f5f9; }

.aum__row:hover td { background: var(--ds-gray-50); }
.dark .aum__row:hover td { background: var(--ds-gray-800); }

.aum__td--mono { font-family: var(--ds-font-display); }
.aum__td--primary { color: var(--ds-primary); font-weight: 700; }
.aum__td--id { color: var(--ds-text-muted); font-size: 0.8rem; }
.aum__td--center { text-align: center; }

/* Empty cell */
.aum__empty-cell { padding: 3rem 1.25rem; }
.aum__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  color: var(--ds-text-muted);
}
.aum__empty .lucide { font-size: 2.5rem; opacity: 0.4; }
.aum__empty p { margin: 0; font-size: 0.875rem; font-weight: 600; }

/* Loading row */
.aum__loading-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1.5rem 0;
}

/* Skeleton */
.aum__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-100) 25%, var(--ds-gray-200) 50%, var(--ds-gray-100) 75%);
  background-size: 200% 100%;
  animation: aumShimmer 1.5s infinite;
  border-radius: var(--ds-radius-md);
}

.dark .aum__skeleton {
  background: linear-gradient(90deg, var(--ds-gray-800) 25%, var(--ds-gray-700) 50%, var(--ds-gray-800) 75%);
  background-size: 200% 100%;
}

@keyframes aumShimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.aum__skeleton--sm { width: 40px; height: 16px; }
.aum__skeleton--md { width: 120px; height: 16px; }
.aum__skeleton--lg { width: 200px; height: 16px; }
.aum__skeleton--label { width: 80px; height: 12px; }
.aum__skeleton--value { flex: 1; height: 12px; }

/* Badges */
.aum__badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.75rem;
  font-weight: 700;
  white-space: nowrap;
}

.aum__badge--success { background: var(--ds-success-soft); color: var(--ds-success); }
.aum__badge--muted { background: var(--ds-gray-100); color: var(--ds-text-muted); }
.dark .aum__badge--muted { background: var(--ds-gray-700); }

/* Status dot */
.aum__status-dot {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8rem;
  font-weight: 700;
}

.aum__status-dot--success { color: var(--ds-success); }
.aum__status-dot--danger { color: var(--ds-danger); }

.aum__status-pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.aum__status-dot--success .aum__status-pulse {
  background: var(--ds-success);
  box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.15);
}

.aum__status-dot--danger .aum__status-pulse {
  background: var(--ds-danger);
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.15);
}

/* Actions */
.aum__td--actions { text-align: right; }

.aum__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.375rem;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.aum__row:hover .aum__actions { opacity: 1; }

.aum__action-btn {
  width: 2rem;
  height: 2rem;
  border-radius: var(--ds-radius-md);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.12s ease;
}

.dark .aum__action-btn {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.aum__action-btn:hover { transform: translateY(-1px); }

.aum__action-btn--view:hover {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border-color: var(--ds-primary-border);
}

.dark .aum__action-btn--view:hover { background: rgba(79, 70, 229, 0.1); }

.aum__action-btn--danger:hover {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
  border-color: rgba(220, 38, 38, 0.3);
}

.dark .aum__action-btn--danger:hover { background: rgba(220, 38, 38, 0.1); }

/* Pagination */
.aum__pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.875rem 1.25rem;
  border-top: 1px solid var(--ds-border);
  flex-wrap: wrap;
  gap: 0.75rem;
}

.dark .aum__pagination { border-top-color: var(--ds-border-strong); }

.aum__pagination-info {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  margin: 0;
  font-weight: 500;
}

.aum__pagination-info strong { color: var(--ds-text); font-weight: 700; }
.dark .aum__pagination-info strong { color: #f1f5f9; }

.aum__pagination-btns { display: flex; gap: 0.5rem; }

/* ── Modal ──────────────────────────────────────────────────────────── */
.aum__modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(6px);
}

.aum__modal {
  width: 100%;
  max-width: 520px;
  max-height: 90vh;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.3);
}

.dark .aum__modal {
  border-color: var(--ds-border-strong);
  background: rgba(15, 23, 42, 0.98);
}

.aum__modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  flex-shrink: 0;
}

.dark .aum__modal-header { border-bottom-color: var(--ds-border-strong); }

.aum__modal-header-left {
  display: flex;
  align-items: center;
  gap: 0.875rem;
}

.aum__modal-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dark .aum__modal-icon { background: rgba(79, 70, 229, 0.15); }

.aum__modal-title {
  font-family: var(--ds-font-display);
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .aum__modal-title { color: #f1f5f9; }

.aum__modal-subtitle {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0.25rem 0 0;
  font-weight: 500;
}

.aum__modal-close {
  width: 2.25rem;
  height: 2.25rem;
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  background: transparent;
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.12s ease;
}

.dark .aum__modal-close {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.aum__modal-close:hover { background: var(--ds-gray-100); color: var(--ds-text); }
.dark .aum__modal-close:hover { background: var(--ds-gray-700); }

.aum__modal-body {
  overflow-y: auto;
  flex: 1;
  padding: 1.5rem;
}

.aum__modal-loading { display: flex; flex-direction: column; gap: 0.75rem; }

.aum__skeleton-row { display: flex; align-items: center; gap: 1rem; justify-content: space-between; }

/* Detail list */
.aum__detail-avatar {
  display: flex;
  justify-content: center;
  margin-bottom: 1.5rem;
}

.aum__detail-avatar-img {
  width: 80px;
  height: 80px;
  border-radius: var(--ds-radius-2xl);
  border: 2px solid var(--ds-border);
  object-fit: cover;
}

.aum__detail-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.aum__detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--ds-border);
}

.dark .aum__detail-row { border-bottom-color: var(--ds-border-strong); }

.aum__detail-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-muted);
  flex-shrink: 0;
}

.aum__detail-value {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ds-text);
  text-align: right;
  word-break: break-all;
}

.dark .aum__detail-value { color: #f1f5f9; }

.aum__detail-value--mono { font-family: var(--ds-font-display); }
.aum__detail-value--primary { color: var(--ds-primary); }

/* Modal footer */
.aum__modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.625rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--ds-border);
  flex-shrink: 0;
  margin-top: 0.5rem;
}

.dark .aum__modal-footer { border-top-color: var(--ds-border-strong); }

/* Modal transition */
.aum-modal-enter-active { transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1); }
.aum-modal-leave-active { transition: all 0.2s ease; }
.aum-modal-enter-from { opacity: 0; }
.aum-modal-enter-from .aum__modal { transform: scale(0.95) translateY(12px); }
.aum-modal-leave-to { opacity: 0; }
.aum-modal-leave-to .aum__modal { transform: scale(0.95); }
</style>
