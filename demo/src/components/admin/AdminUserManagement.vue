<template>
  <div class="tui-page-wrap">

    <!-- Page Header -->
    <header class="tui-header">
      <div class="tui-header-left">
        <div class="tui-header-icon">
          <LucideIcon :name="headerIcon" :size="22" />
        </div>
        <div>
          <p class="tui-header-eyebrow">{{ sectionEyebrow }}</p>
          <h1 class="tui-header-title">{{ sectionTitle }}</h1>
          <p class="tui-header-desc">{{ sectionSubtitle }}</p>
        </div>
      </div>
      <div class="tui-header-right">
        <button type="button" class="tui-btn tui-btn--secondary" :disabled="loading" @click="load">
          <LucideIcon name="refresh" :size="14" :class="loading ? 'tui-spin' : ''" />
          Làm mới
        </button>
        <!-- Nút tạo admin — chỉ hiện ở tab Quản trị -->
        <button
          v-if="variant === 'admins'"
          type="button"
          class="tui-btn tui-btn--primary"
          @click="openCreateAdmin"
        >
          <LucideIcon name="plus" :size="14" />
          Tạo quản trị
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
      <div class="tui-kpi-card" :class="`tui-kpi-card--primary`" style="animation-delay: 0s">
        <div class="tui-kpi-icon-wrap">
          <LucideIcon :name="headerIcon" :size="18" />
        </div>
        <div class="tui-kpi-body">
          <p class="tui-kpi-value">{{ formatNum(totalElements) }}</p>
          <p class="tui-kpi-label">Tổng tài khoản</p>
        </div>
      </div>
      <div class="tui-kpi-card tui-kpi-card--success" style="animation-delay: 0.06s">
        <div class="tui-kpi-icon-wrap">
          <LucideIcon name="check_circle" :size="18" />
        </div>
        <div class="tui-kpi-body">
          <p class="tui-kpi-value">{{ formatNum(verifiedCount) }}</p>
          <p class="tui-kpi-label">Đã xác minh</p>
        </div>
      </div>
      <div class="tui-kpi-card tui-kpi-card--warning" style="animation-delay: 0.12s">
        <div class="tui-kpi-icon-wrap">
          <LucideIcon name="pending" :size="18" />
        </div>
        <div class="tui-kpi-body">
          <p class="tui-kpi-value">{{ formatNum(unverifiedCount) }}</p>
          <p class="tui-kpi-label">Chưa xác minh</p>
        </div>
      </div>
      <div class="tui-kpi-card tui-kpi-card--info" style="animation-delay: 0.18s">
        <div class="tui-kpi-icon-wrap">
          <LucideIcon name="file_text" :size="18" />
        </div>
        <div class="tui-kpi-body">
          <p class="tui-kpi-value">{{ totalPages }}</p>
          <p class="tui-kpi-label">Trang</p>
        </div>
      </div>
    </div>

    <!-- Search Bar (students / teachers only) -->
    <div v-if="showSearch" class="tui-search-bar">
      <div class="tui-search-wrap">
        <LucideIcon name="search" :size="14" style="color: var(--ds-text-secondary); flex-shrink: 0" />
        <input
          v-model="searchInput"
          type="search"
          placeholder="Tìm theo username, email, họ tên, SDT..."
          class="tui-search-input"
          @keydown.enter.prevent="applySearch"
        />
      </div>
      <button type="button" class="tui-btn tui-btn--primary tui-btn--sm" @click="applySearch">
        <LucideIcon name="search" :size="13" />
        Tìm kiếm
      </button>
      <button
        v-if="appliedQuery"
        type="button"
        class="tui-btn tui-btn--secondary tui-btn--sm"
        @click="clearSearch"
      >
        <LucideIcon name="x" :size="13" />
        Xóa lọc
      </button>
    </div>

    <!-- Table Panel -->
    <div class="tui-panel tui-panel--anim" style="margin: 1rem 1.5rem 1.5rem; flex: 1; display: flex; flex-direction: column; min-height: 0; overflow: hidden">

      <!-- Table -->
      <div class="tui-table-wrap" style="flex: 1; overflow-y: auto">
        <table class="tui-table">
          <thead>
            <tr>
              <th style="width: 40px">#</th>
              <th>Username</th>
              <th>Email</th>
              <th>Xác minh</th>
              <th>Họ tên / Hiển thị</th>
              <th>Điện thoại</th>
              <th v-if="variant === 'admins'">Vai trò</th>
              <th v-if="showActions" style="text-align: right">Thao tác</th>
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
                <td :colspan="showActions ? 7 : 6" style="border-bottom: none">
                  <div class="tui-empty">
                    <div class="tui-empty-icon"><LucideIcon name="inbox" :size="22" /></div>
                    <p class="tui-empty-title">Không tìm thấy tài khoản</p>
                    <p class="tui-empty-desc">{{ appliedQuery ? 'Thử từ khóa khác hoặc xóa bộ lọc.' : 'Danh sách trống.' }}</p>
                  </div>
                </td>
              </tr>
            </template>
            <template v-else>
              <tr
                v-for="(row, idx) in rows"
                :key="row.userId"
                class="tui-table-row--anim"
                :style="{ animationDelay: `${idx * 0.04}s` }"
              >
                <td style="color: var(--ds-text-secondary); font-size: 0.72rem">{{ page * size + idx + 1 }}</td>
                <td>
                  <span style="font-family: 'JetBrains Mono', monospace; font-size: 0.75rem; color: var(--ds-primary); font-weight: 700">{{ row.username }}</span>
                </td>
                <td style="font-size: 0.78rem">{{ row.email }}</td>
                <td>
                  <span class="tui-status" :class="row.emailVerified ? 'tui-status--up' : 'tui-status--warning'">
                    <span class="tui-dot" :class="row.emailVerified ? 'tui-dot--up' : 'tui-dot--warning'" />
                    {{ row.emailVerified ? 'Đã xác minh' : 'Chưa' }}
                  </span>
                </td>
                <td style="font-size: 0.78rem">{{ displayName(row) }}</td>
                <td style="font-size: 0.78rem; color: var(--ds-text-secondary)">{{ row.phone || '—' }}</td>
                <!-- Cột vai trò (admin) -->
                <td v-if="variant === 'admins'">
                  <div style="display: flex; flex-wrap: wrap; gap: 0.25rem">
                    <span
                      v-for="role in (row.roles || [])"
                      :key="role"
                      class="tui-role-chip"
                      :class="`tui-role-chip--${roleClass(role)}`"
                    >
                      <LucideIcon :name="roleIcon(role)" :size="11" />
                      {{ roleLabel(role) }}
                    </span>
                    <span v-if="!row.roles?.length" style="font-size: 0.72rem; color: var(--ds-text-secondary)">—</span>
                  </div>
                </td>
                <td v-if="showActions" style="text-align: right; white-space: nowrap">
                  <button type="button" class="tui-btn tui-btn--secondary tui-btn--sm" style="margin-right: 0.375rem" @click="openDetail(row)">
                    <LucideIcon name="visibility" :size="13" />
                    Chi tiết
                  </button>
                  <button type="button" class="tui-btn tui-btn--danger tui-btn--sm" @click="confirmDelete(row)">
                    <LucideIcon name="delete" :size="13" />
                    Xóa
                  </button>
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
          {{ formatNum(totalElements) }} tài khoản
          <span v-if="appliedQuery" style="color: var(--ds-primary)"> &middot; lọc: "{{ appliedQuery }}"</span>
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

    <!-- Detail Modal -->
    <Teleport to="body">
      <div v-if="detailOpen" class="tui-modal-overlay" @click.self="closeDetail">
        <div class="tui-modal" role="dialog" aria-modal="true">
          <div class="tui-modal-header">
            <h3 class="tui-modal-title">Chi tiết tài khoản</h3>
            <button type="button" class="tui-modal-close" @click="closeDetail">
              <LucideIcon name="x" :size="16" />
            </button>
          </div>
          <div v-if="detailLoading" class="tui-modal-body" style="display: flex; justify-content: center; padding: 3rem">
            <LucideIcon name="loader_2" :size="28" class="tui-spin" style="color: var(--ds-text-secondary)" />
          </div>
          <div v-else-if="detail" class="tui-modal-body">
            <div v-if="detail.avatarUrl" style="display: flex; justify-content: center; margin-bottom: 1.25rem">
              <img
                :src="detail.avatarUrl"
                alt=""
                style="width: 72px; height: 72px; border-radius: 50%; border: 2px solid var(--ds-gray-200); object-fit: cover"
              />
            </div>
            <dl style="display: flex; flex-direction: column; gap: 0">
              <div v-for="field in detailFields" :key="field.key" class="tui-detail-row">
                <dt>{{ field.label }}</dt>
                <dd>{{ field.value }}</dd>
              </div>
            </dl>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Create Admin Modal -->
    <Teleport to="body">
      <div v-if="createOpen" class="tui-modal-overlay" @click.self="closeCreateAdmin">
        <div class="tui-modal tui-modal--lg" role="dialog" aria-modal="true">
          <div class="tui-modal-header">
            <h3 class="tui-modal-title">Tạo tài khoản quản trị</h3>
            <button type="button" class="tui-modal-close" @click="closeCreateAdmin">
              <LucideIcon name="x" :size="16" />
            </button>
          </div>
          <div class="tui-modal-body">
            <p style="font-size: 0.8rem; color: var(--ds-text-secondary); margin-bottom: 1.25rem">
              Tạo tài khoản quản trị với quyền hạn chế. Người dùng sẽ nhận email kích hoạt.
            </p>
            <div class="tui-form-grid">
              <div class="tui-form-field">
                <label class="tui-form-label">Username <span class="tui-required">*</span></label>
                <input v-model="createForm.username" type="text" class="tui-form-input" placeholder="VD: admin_phuhung" />
              </div>
              <div class="tui-form-field">
                <label class="tui-form-label">Email <span class="tui-required">*</span></label>
                <input v-model="createForm.email" type="email" class="tui-form-input" placeholder="admin@eduexam.vn" />
              </div>
              <div class="tui-form-field">
                <label class="tui-form-label">Họ tên</label>
                <input v-model="createForm.fullName" type="text" class="tui-form-input" placeholder="Nguyễn Văn A" />
              </div>
              <div class="tui-form-field">
                <label class="tui-form-label">Số điện thoại</label>
                <input v-model="createForm.phone" type="tel" class="tui-form-input" placeholder="0912 345 678" />
              </div>
            </div>

            <!-- Phân quyền -->
            <div style="margin-top: 1.5rem">
              <label class="tui-form-label">Phân quyền <span class="tui-required">*</span></label>
              <div class="tui-role-grid">
                <label
                  v-for="perm in permissionOptions"
                  :key="perm.value"
                  class="tui-perm-card"
                  :class="{ 'tui-perm-card--active': createForm.permissions.includes(perm.value) }"
                >
                  <input
                    type="checkbox"
                    :value="perm.value"
                    v-model="createForm.permissions"
                    style="display: none"
                  />
                  <LucideIcon :name="perm.icon" :size="18" />
                  <div>
                    <p class="tui-perm-title">{{ perm.label }}</p>
                    <p class="tui-perm-desc">{{ perm.desc }}</p>
                  </div>
                </label>
              </div>
            </div>
          </div>
          <div class="tui-modal-footer">
            <button type="button" class="tui-btn tui-btn--secondary" @click="closeCreateAdmin">
              Hủy
            </button>
            <button type="button" class="tui-btn tui-btn--primary" :disabled="createLoading" @click="submitCreateAdmin">
              <LucideIcon v-if="createLoading" name="loader_2" :size="14" class="tui-spin" />
              <LucideIcon v-else name="plus" :size="14" />
              {{ createLoading ? 'Đang tạo...' : 'Tạo tài khoản' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, reactive } from 'vue'
import {
  fetchAdminStudents,
  fetchAdminTeachers,
  fetchAdminAdmins,
  fetchAdminStudentDetail,
  fetchAdminTeacherDetail,
  deleteAdminStudent,
  deleteAdminTeacher,
  createAdminUser,
  deleteAdminAdmin
} from '../../services/adminService'
import { useToast } from '../../composables/useToast'
import LucideIcon from '../common/LucideIcon.vue'

const toast = useToast()

const props = defineProps({
  variant: {
    type: String,
    required: true,
    validator: (v) => v === 'students' || v === 'teachers' || v === 'admins'
  }
})

// ─── Header metadata ──────────────────────────────────────────────
const headerIcon = computed(() => {
  if (props.variant === 'students') return 'school'
  if (props.variant === 'teachers') return 'co_present'
  return 'shield_check'
})
const sectionEyebrow = computed(() => {
  if (props.variant === 'students') return 'Quản lý học sinh'
  if (props.variant === 'teachers') return 'Quản lý giáo viên'
  return 'Quản lý quản trị'
})
const sectionTitle = computed(() => {
  if (props.variant === 'students') return 'Danh sách học sinh'
  if (props.variant === 'teachers') return 'Danh sách giáo viên'
  return 'Danh sách quản trị viên'
})
const sectionSubtitle = computed(() => {
  if (props.variant === 'students') return 'Theo dõi, tìm kiếm và quản lý tài khoản học sinh.'
  if (props.variant === 'teachers') return 'Theo dõi, tìm kiếm và quản lý tài khoản giáo viên.'
  return 'Quản lý tài khoản quản trị viên và phân quyền.'
})

const showSearch = computed(() => props.variant === 'students' || props.variant === 'teachers')
const showActions = computed(() => props.variant === 'students' || props.variant === 'teachers')

// ─── Data ──────────────────────────────────────────────────────────
const loading = ref(true)
const errorMsg = ref('')
const rows = ref([])
const page = ref(0)
const size = ref(20)
const totalElements = ref(0)
const totalPages = ref(0)
const searchInput = ref('')
const appliedQuery = ref('')
const verifiedCount = computed(() => rows.value.filter(r => r.emailVerified).length)
const unverifiedCount = computed(() => rows.value.filter(r => !r.emailVerified).length)

// ─── Detail modal ─────────────────────────────────────────────────
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const detailFields = computed(() => {
  if (!detail.value) return []
  return [
    { key: 'id',         label: 'ID',           value: detail.value.userId },
    { key: 'username',   label: 'Username',     value: detail.value.username },
    { key: 'email',      label: 'Email',         value: detail.value.email },
    { key: 'verified',   label: 'Xác minh',     value: detail.value.emailVerified ? 'Đã xác minh' : 'Chưa xác minh' },
    { key: 'fullName',   label: 'Họ tên',       value: detail.value.fullName || '—' },
    { key: 'displayName',label: 'Tên hiển thị', value: detail.value.displayName || '—' },
    { key: 'phone',      label: 'Điện thoại',   value: detail.value.phone || '—' },
    { key: 'dob',        label: 'Ngày sinh',    value: detail.value.dateOfBirth || '—' }
  ]
})

// ─── Create admin modal ────────────────────────────────────────────
const createOpen = ref(false)
const createLoading = ref(false)
const createForm = reactive({
  username: '',
  email: '',
  fullName: '',
  phone: '',
  permissions: []
})

const permissionOptions = [
  { value: 'ROLE_ADMIN_DASHBOARD',  label: 'Xem dashboard',     desc: 'Truy cập trang tổng quan',     icon: 'layout_dashboard' },
  { value: 'ROLE_ADMIN_USERS',      label: 'Quản lý người dùng', desc: 'Xem, xóa tài khoản',          icon: 'users' },
  { value: 'ROLE_ADMIN_STUDENTS',    label: 'Quản lý học sinh',   desc: 'Quản lý tài khoản học sinh',   icon: 'school' },
  { value: 'ROLE_ADMIN_TEACHERS',    label: 'Quản lý giáo viên',  desc: 'Quản lý tài khoản giáo viên',  icon: 'presentation' },
  { value: 'ROLE_ADMIN_EXAMS',      label: 'Quản lý đề thi',      desc: 'Bật/tắt, quản lý đề thi',      icon: 'clipboard_list' },
  { value: 'ROLE_ADMIN_SETTINGS',   label: 'Cài đặt hệ thống',   desc: 'Thay đổi cấu hình toàn cục',   icon: 'settings' }
]

// ─── Role helpers ──────────────────────────────────────────────────
const roleIcon = (role) => {
  if (role === 'ROLE_ADMIN')       return 'shield_check'
  if (role === 'ROLE_TEACHER')      return 'presentation'
  if (role === 'ROLE_STUDENT')     return 'school'
  if (role === 'ROLE_ADMIN_DASHBOARD') return 'layout_dashboard'
  if (role === 'ROLE_ADMIN_USERS')  return 'users'
  if (role === 'ROLE_ADMIN_STUDENTS') return 'school'
  if (role === 'ROLE_ADMIN_TEACHERS') return 'presentation'
  if (role === 'ROLE_ADMIN_EXAMS')  return 'clipboard_list'
  if (role === 'ROLE_ADMIN_SETTINGS') return 'settings'
  return 'badge'
}

const roleLabel = (role) => {
  if (role === 'ROLE_ADMIN')         return 'Admin'
  if (role === 'ROLE_TEACHER')       return 'Giáo viên'
  if (role === 'ROLE_STUDENT')       return 'Học sinh'
  if (role === 'ROLE_ADMIN_DASHBOARD') return 'Dashboard'
  if (role === 'ROLE_ADMIN_USERS')   return 'Người dùng'
  if (role === 'ROLE_ADMIN_STUDENTS') return 'Học sinh'
  if (role === 'ROLE_ADMIN_TEACHERS') return 'Giáo viên'
  if (role === 'ROLE_ADMIN_EXAMS')   return 'Đề thi'
  if (role === 'ROLE_ADMIN_SETTINGS') return 'Cài đặt'
  return role.replace('ROLE_', '').replace('ADMIN_', '').replace('_', ' ')
}

const roleClass = (role) => {
  if (role === 'ROLE_ADMIN') return 'primary'
  if (role === 'ROLE_TEACHER') return 'warning'
  if (role === 'ROLE_STUDENT') return 'success'
  if (role.startsWith('ROLE_ADMIN_')) return 'info'
  return 'muted'
}

// ─── Display helpers ───────────────────────────────────────────────
const formatNum = (n) => (n == null ? '—' : new Intl.NumberFormat('vi-VN').format(n))

const displayName = (row) => {
  const full = row.fullName?.trim()
  const disp = row.displayName?.trim()
  if (full && disp) return `${full} (${disp})`
  return full || disp || '—'
}

// ─── API calls ────────────────────────────────────────────────────
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
    if (showSearch.value && appliedQuery.value) params.q = appliedQuery.value
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
  appliedQuery.value = searchInput.value.trim()
  page.value = 0
  load()
}

const clearSearch = () => {
  searchInput.value = ''
  appliedQuery.value = ''
  page.value = 0
  load()
}

const goPage = (p) => {
  page.value = Math.max(0, p)
  load()
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

const closeDetail = () => { detailOpen.value = false; detail.value = null }

const confirmDelete = (row) => {
  const label = props.variant === 'students' ? 'học sinh' : props.variant === 'teachers' ? 'giáo viên' : 'quản trị'
  const msg = `Xóa tài khoản ${label} "${row.username}"? Thao tác không hoàn tác.`
  if (!window.confirm(msg)) return
  doDelete(row.userId)
}

const doDelete = async (userId) => {
  try {
    if (props.variant === 'students') await deleteAdminStudent(userId)
    else if (props.variant === 'teachers') await deleteAdminTeacher(userId)
    else await deleteAdminAdmin(userId)
    toast.success('Đã xóa tài khoản.')
    if (detail.value?.userId === userId) closeDetail()
    await load()
  } catch (e) {
    toast.error(e?.payload?.message || e?.message || 'Không xóa được.')
  }
}

// ─── Create admin ─────────────────────────────────────────────────
const openCreateAdmin = () => {
  createForm.username = ''
  createForm.email = ''
  createForm.fullName = ''
  createForm.phone = ''
  createForm.permissions = []
  createOpen.value = true
}

const closeCreateAdmin = () => { createOpen.value = false }

const submitCreateAdmin = async () => {
  if (!createForm.username.trim()) { toast.error('Username là bắt buộc.'); return }
  if (!createForm.email.trim()) { toast.error('Email là bắt buộc.'); return }
  if (!createForm.permissions.length) { toast.error('Phải chọn ít nhất một quyền.'); return }
  createLoading.value = true
  try {
    await createAdminUser({
      username: createForm.username.trim(),
      email: createForm.email.trim(),
      fullName: createForm.fullName.trim() || undefined,
      phone: createForm.phone.trim() || undefined,
      roles: createForm.permissions
    })
    toast.success('Đã tạo tài khoản quản trị. Email kích hoạt đã được gửi.')
    closeCreateAdmin()
    await load()
  } catch (e) {
    toast.error(e?.payload?.message || e?.message || 'Không tạo được tài khoản.')
  } finally {
    createLoading.value = false
  }
}

onMounted(load)
watch(() => props.variant, () => {
  page.value = 0
  searchInput.value = ''
  appliedQuery.value = ''
  closeDetail()
  load()
})
</script>
