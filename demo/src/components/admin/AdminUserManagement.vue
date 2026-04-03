<template>
  <div class="db-page-wrap">

    <!-- Page Header -->
    <header class="db-header">
      <div class="db-header-left">
        <div class="db-header-icon">
          <LucideIcon name="users" :size="24" />
        </div>
        <div>
          <p class="db-eyebrow">{{ sectionEyebrow }}</p>
          <h1 class="db-title">{{ sectionTitle }}</h1>
          <p class="db-subtitle">{{ sectionSubtitle }}</p>
        </div>
      </div>
    </header>

    <!-- Search Bar -->
    <div v-if="showSearch" class="db-search-bar-card">
      <div class="db-search-wrap" style="flex: 1">
        <LucideIcon name="search" :size="16" class="db-search-icon" />
        <input
          v-model="searchInput"
          type="search"
          placeholder="Tim theo username, email, ho ten, hien thi, SDT..."
          class="db-search-input"
          @keydown.enter.prevent="applySearch"
        />
      </div>
      <div style="display: flex; gap: 0.5rem">
        <button
          type="button"
          class="db-btn db-btn--primary db-btn--sm"
          @click="applySearch"
        >
          Tim
        </button>
        <button
          v-if="appliedQuery"
          type="button"
          class="db-btn db-btn--secondary db-btn--sm"
          @click="clearSearch"
        >
          Xoa loc
        </button>
      </div>
    </div>

    <!-- Table Panel -->
    <div class="db-table-panel">
      <!-- Toolbar -->
      <div class="db-toolbar">
        <p class="db-toolbar-meta">
          <span style="font-weight: 700; color: var(--db-text)">{{ formatNum(totalElements) }}</span> tai khoan
          <span v-if="appliedQuery" style="color: var(--db-text-muted)"> · loc: "{{ appliedQuery }}"</span>
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
              <th>#</th>
              <th>Username</th>
              <th>Email</th>
              <th>Xac minh</th>
              <th>Ho ten / hien thi</th>
              <th>Dien thoai</th>
              <th v-if="showActions" class="text-right">Thao tac</th>
            </tr>
          </thead>
          <tbody>
            <template v-if="loading && !rows.length">
              <tr>
                <td :colspan="colspanRows" class="db-empty-cell">
                  <div style="display: flex; flex-direction: column; align-items: center; gap: 0.5rem; padding: 3rem">
                    <LucideIcon name="loader_2" :size="24" class="db-spin" style="color: var(--db-text-muted)" />
                    <span style="color: var(--db-text-muted); font-size: 0.875rem">Dang tai...</span>
                  </div>
                </td>
              </tr>
            </template>
            <template v-else-if="!rows.length">
              <tr>
                <td :colspan="colspanRows" class="db-empty-cell">
                  <div class="db-empty">
                    <div class="db-empty-icon">
                      <LucideIcon name="inbox" :size="24" />
                    </div>
                    <p class="db-empty-title">Chua co du lieu</p>
                    <p class="db-empty-desc">Khong tim thay tai khoan nao.</p>
                  </div>
                </td>
              </tr>
            </template>
            <template v-else>
              <tr
                v-for="(row, idx) in rows"
                :key="row.userId"
                class="db-table-row"
                :style="{ animationDelay: `${idx * 0.03}s` }"
              >
                <td style="color: var(--db-text-muted)">{{ page * size + idx + 1 }}</td>
                <td class="text-mono" style="color: var(--db-primary); font-size: 0.8rem">{{ row.username }}</td>
                <td style="color: var(--db-text)">{{ row.email }}</td>
                <td>
                  <span
                    class="db-chip"
                    :class="row.emailVerified ? 'db-chip--success' : 'db-chip--warning'"
                  >
                    <span
                      class="db-dot"
                      :class="row.emailVerified ? 'db-dot--success' : 'db-dot--warning'"
                      style="width: 6px; height: 6px"
                    />
                    {{ row.emailVerified ? 'Da xac minh' : 'Chua' }}
                  </span>
                </td>
                <td style="color: var(--db-text)">{{ displayName(row) }}</td>
                <td style="color: var(--db-text-muted)">{{ row.phone || '—' }}</td>
                <td v-if="showActions" class="text-right" style="white-space: nowrap">
                  <button
                    type="button"
                    class="db-btn db-btn--primary db-btn--sm"
                    style="margin-right: 0.375rem"
                    @click="openDetail(row)"
                  >
                    <LucideIcon name="visibility" :size="14" />
                    Chi tiet
                  </button>
                  <button
                    type="button"
                    class="db-btn db-btn--danger db-btn--sm"
                    @click="confirmDelete(row)"
                  >
                    <LucideIcon name="delete" :size="14" />
                    Xoa
                  </button>
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

    <!-- Detail Modal -->
    <Teleport to="body">
      <div
        v-if="detailOpen"
        class="db-modal-overlay"
        @click.self="closeDetail"
      >
        <div class="db-modal" role="dialog" aria-modal="true">
          <div class="db-modal-header">
            <h3 class="db-modal-title">Chi tiet tai khoan</h3>
            <button
              type="button"
              class="db-modal-close"
              @click="closeDetail"
            >
              <LucideIcon name="x" :size="18" />
            </button>
          </div>
          <div v-if="detailLoading" class="db-modal-body" style="display: flex; justify-content: center; padding: 3rem">
            <LucideIcon name="loader_2" :size="28" class="db-spin" style="color: var(--db-text-muted)" />
          </div>
          <div v-else-if="detail" class="db-modal-body">
            <div v-if="detail.avatarUrl" style="display: flex; justify-content: center; margin-bottom: 1.5rem">
              <img
                :src="detail.avatarUrl"
                alt=""
                style="width: 80px; height: 80px; border-radius: var(--db-radius-xl); border: 2px solid var(--db-border-strong); object-fit: cover"
              />
            </div>
            <dl style="display: flex; flex-direction: column; gap: 0.75rem">
              <div v-for="field in detailFields" :key="field.key" class="db-detail-row">
                <dt>{{ field.label }}</dt>
                <dd :class="field.mono ? 'text-mono' : ''" :style="field.style">{{ field.value }}</dd>
              </div>
            </dl>
          </div>
        </div>
      </div>
    </Teleport>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
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

const props = defineProps({
  variant: {
    type: String,
    required: true,
    validator: (v) => v === 'students' || v === 'teachers' || v === 'admins'
  }
})

const sectionEyebrow = computed(() => {
  if (props.variant === 'students') return 'Hoc sinh'
  if (props.variant === 'teachers') return 'Giao vien'
  return 'Quan tri vien'
})

const sectionTitle = computed(() => {
  if (props.variant === 'students') return 'Quan ly hoc sinh'
  if (props.variant === 'teachers') return 'Quan ly giao vien'
  return 'Quan ly tai khoan admin'
})

const sectionSubtitle = computed(() => {
  if (props.variant === 'students') {
    return 'Danh sach phan trang, tim theo tu khoa, xem chi tiet va xoa khi can.'
  }
  if (props.variant === 'teachers') {
    return 'Theo doi giao vien da dang ky — tim kiem, chi tiet ho so, go tai khoan.'
  }
  return 'Chi xem danh sach quan tri vien; khong co thao tac xoa tu man nay.'
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

const detailFields = computed(() => {
  if (!detail.value) return []
  return [
    { key: 'id', label: 'ID', value: detail.value.userId, mono: true },
    { key: 'username', label: 'Username', value: detail.value.username, mono: true, style: { color: 'var(--db-primary)' } },
    { key: 'email', label: 'Email', value: detail.value.email },
    { key: 'verified', label: 'Xac minh email', value: detail.value.emailVerified ? 'Da xac minh' : 'Chua' },
    { key: 'fullName', label: 'Ho ten', value: detail.value.fullName || '—' },
    { key: 'displayName', label: 'Ten hien thi', value: detail.value.displayName || '—' },
    { key: 'phone', label: 'Dien thoai', value: detail.value.phone || '—' },
    { key: 'dob', label: 'Ngay sinh', value: detail.value.dateOfBirth || '—' }
  ]
})

const formatNum = (n) => (n == null ? '—' : new Intl.NumberFormat('vi-VN').format(n))

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
    errorMsg.value = e?.payload?.message || e?.message || 'Khong tai duoc danh sach.'
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
    toast.error(e?.payload?.message || e?.message || 'Khong tai duoc chi tiet.')
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
  const label = props.variant === 'students' ? 'hoc sinh' : 'giao vien'
  const msg = `Xoa ${label} "${row.username}"? Thao tac khong hoan tac.`
  if (!window.confirm(msg)) return
  doDelete(row.userId)
}

const doDelete = async (userId) => {
  try {
    if (props.variant === 'students') {
      await deleteAdminStudent(userId)
    } else {
      await deleteAdminTeacher(userId)
    }
    toast.success('Da xoa tai khoan.')
    if (detail.value?.userId === userId) closeDetail()
    await load()
  } catch (e) {
    toast.error(e?.payload?.message || e?.message || 'Khong xoa duoc.')
  }
}

onMounted(load)

watch(
  () => props.variant,
  () => {
    page.value = 0
    searchInput.value = ''
    appliedQuery.value = ''
    closeDetail()
    load()
  }
)
</script>

<style scoped>
.db-search-bar-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  background: rgba(17, 17, 19, 0.85);
  border: 1px solid var(--db-border-strong);
  border-radius: var(--db-radius-xl);
  box-shadow: var(--db-shadow-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  flex-wrap: wrap;
  animation: fadeInUp 0.4s ease backwards;
  animation-delay: 0.1s;
}

.db-table-row {
  animation: fadeInUp 0.3s ease backwards;
}

.db-empty-cell {
  border-bottom: none !important;
  padding: 0 !important;
}

.db-detail-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--db-border);
}

.db-detail-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.db-detail-row dt {
  color: var(--db-text-muted);
  font-size: 0.8rem;
  font-weight: 600;
  flex-shrink: 0;
}

.db-detail-row dd {
  color: var(--db-text);
  font-size: 0.8rem;
  font-weight: 500;
  text-align: right;
  word-break: break-all;
}
</style>
