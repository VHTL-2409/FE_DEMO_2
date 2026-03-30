<template>
  <div class="staff-page-wrap min-h-0 flex-1 gap-6 pb-2">
    <PageHeader :eyebrow="sectionEyebrow" :title="sectionTitle" :subtitle="sectionSubtitle" />

    <div
      v-if="showSearch"
      class="staff-surface rounded-[1.5rem] p-4 sm:p-5 flex shrink-0 flex-col gap-3 sm:flex-row sm:items-center"
    >
      <div class="relative flex-1 max-w-xl">
        <LucideIcon name="search" />
        <input
          v-model="searchInput"
          type="search"
          placeholder="Tìm theo username, email, họ tên, hiển thị, SĐT…"
          class="staff-search-input w-full rounded-xl pl-10 pr-4 py-2.5 text-sm placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-primary/30"
          @keydown.enter.prevent="applySearch"
        />
      </div>
      <div class="flex gap-2">
        <button
          type="button"
          class="staff-action-btn staff-action-btn-primary"
          @click="applySearch"
        >
          Tìm
        </button>
        <button
          v-if="appliedQuery"
          type="button"
          class="staff-action-btn staff-action-btn-neutral"
          @click="clearSearch"
        >
          Xóa bộ lọc
        </button>
      </div>
    </div>

    <div class="staff-table-panel rounded-[1.75rem]">
      <div class="staff-toolbar">
        <p class="staff-toolbar-meta">
          <span class="font-semibold tabular-nums text-slate-900 dark:text-slate-100">{{ totalElements }}</span>
          tài khoản
          <span v-if="appliedQuery" class="text-slate-500"> · lọc: "{{ appliedQuery }}"</span>
        </p>
        <button
          type="button"
          class="staff-action-btn staff-action-btn-neutral self-start disabled:opacity-50 sm:self-auto"
          :disabled="loading"
          @click="load"
        >
          <LucideIcon name="refresh" size="18" />
          Làm mới
        </button>
      </div>

      <div class="min-h-0 flex-1 overflow-auto portal-scrollbar">
        <table class="staff-table min-w-[820px]">
          <thead>
            <tr>
              <th>#</th>
              <th>Username</th>
              <th>Email</th>
              <th>Xác minh</th>
              <th>Họ tên / hiển thị</th>
              <th>Điện thoại</th>
              <th v-if="showActions" class="text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <template v-if="loading && !rows.length">
              <tr class="border-b border-white/[0.04]">
                <td :colspan="colspanRows" class="px-6 py-12 text-center text-slate-500">Đang tải…</td>
              </tr>
            </template>
            <template v-else-if="!rows.length">
              <tr class="border-b border-white/[0.04]">
                <td :colspan="colspanRows" class="px-6 py-12 text-center text-slate-500">Chưa có dữ liệu.</td>
              </tr>
            </template>
            <template v-else>
              <tr
                v-for="(row, idx) in rows"
                :key="row.userId"
                class="border-b border-white/[0.04] hover:bg-white/[0.02] transition-colors"
              >
                <td class="text-slate-500 tabular-nums">{{ page * size + idx + 1 }}</td>
                <td class="font-mono text-primary">{{ row.username }}</td>
                <td class="text-slate-700 dark:text-slate-300">{{ row.email }}</td>
                <td>
                  <span
                    class="staff-status-chip"
                    :class="row.emailVerified ? 'bg-emerald-500/15 text-emerald-300' : 'bg-amber-500/15 text-amber-200'"
                  >
                    {{ row.emailVerified ? 'Đã xác minh' : 'Chưa' }}
                  </span>
                </td>
                <td class="text-slate-700 dark:text-slate-300">
                  {{ displayName(row) }}
                </td>
                <td class="text-slate-500 dark:text-slate-400">{{ row.phone || '—' }}</td>
                <td v-if="showActions" class="text-right whitespace-nowrap">
                  <button
                    type="button"
                    class="staff-action-btn staff-action-btn-primary mr-1"
                    @click="openDetail(row)"
                  >
                    <LucideIcon name="visibility" size="16" />
                    Chi tiết
                  </button>
                  <button
                    type="button"
                    class="staff-action-btn staff-action-btn-neutral text-rose-600 dark:text-rose-400"
                    @click="confirmDelete(row)"
                  >
                    <LucideIcon name="delete" size="16" />
                    Xóa
                  </button>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <div
        v-if="totalPages > 1"
        class="staff-toolbar"
      >
        <p class="staff-toolbar-meta">
          Trang <span class="font-semibold text-slate-900 dark:text-slate-100">{{ page + 1 }}</span> / {{ totalPages }}
        </p>
        <div class="flex items-center gap-2">
          <button
            type="button"
            :disabled="page <= 0 || loading"
            class="staff-action-btn staff-action-btn-neutral disabled:opacity-40"
            @click="goPage(page - 1)"
          >
            Trước
          </button>
          <button
            type="button"
            :disabled="page >= totalPages - 1 || loading"
            class="staff-action-btn staff-action-btn-neutral disabled:opacity-40"
            @click="goPage(page + 1)"
          >
            Sau
          </button>
        </div>
      </div>
    </div>

    <p v-if="errorMsg" class="shrink-0 text-sm font-medium text-rose-400">{{ errorMsg }}</p>

    <!-- Chi tiết -->
    <Teleport to="body">
      <div
        v-if="detailOpen"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm"
        @click.self="closeDetail"
      >
        <div
          class="admin-detail-dialog portal-scrollbar max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl"
          role="dialog"
          aria-modal="true"
        >
          <div class="admin-detail-dialog__header flex items-center justify-between px-5 py-4">
            <h3 class="text-lg font-bold text-slate-50">Chi tiết tài khoản</h3>
            <button
              type="button"
              class="rounded-lg p-1.5 text-slate-400 transition hover:bg-white/10 hover:text-slate-200"
              @click="closeDetail"
            >
              <LucideIcon name="close" />
            </button>
          </div>
          <div v-if="detailLoading" class="px-5 py-12 text-center text-slate-400">Đang tải…</div>
          <div v-else-if="detail" class="space-y-4 px-5 py-5 text-sm">
            <div v-if="detail.avatarUrl" class="flex justify-center">
              <img :src="detail.avatarUrl" alt="" class="size-20 rounded-2xl border border-slate-600/50 object-cover" />
            </div>
            <dl class="grid grid-cols-1 gap-3">
              <div class="flex justify-between gap-4 border-b border-slate-700/50 pb-2">
                <dt>ID</dt>
                <dd class="font-mono text-slate-100">{{ detail.userId }}</dd>
              </div>
              <div class="flex justify-between gap-4 border-b border-slate-700/50 pb-2">
                <dt>Username</dt>
                <dd class="font-mono text-indigo-300">{{ detail.username }}</dd>
              </div>
              <div class="flex justify-between gap-4 border-b border-slate-700/50 pb-2">
                <dt>Email</dt>
                <dd class="break-all text-slate-100">{{ detail.email }}</dd>
              </div>
              <div class="flex justify-between gap-4 border-b border-slate-700/50 pb-2">
                <dt>Xác minh email</dt>
                <dd class="text-slate-100">{{ detail.emailVerified ? 'Đã xác minh' : 'Chưa' }}</dd>
              </div>
              <div class="flex justify-between gap-4 border-b border-slate-700/50 pb-2">
                <dt>Họ tên</dt>
                <dd class="text-slate-100">{{ detail.fullName || '—' }}</dd>
              </div>
              <div class="flex justify-between gap-4 border-b border-slate-700/50 pb-2">
                <dt>Tên hiển thị</dt>
                <dd class="text-slate-100">{{ detail.displayName || '—' }}</dd>
              </div>
              <div class="flex justify-between gap-4 border-b border-slate-700/50 pb-2">
                <dt>Điện thoại</dt>
                <dd class="text-slate-100">{{ detail.phone || '—' }}</dd>
              </div>
              <div class="flex justify-between gap-4">
                <dt>Ngày sinh</dt>
                <dd class="text-slate-100">{{ formatDob(detail.dateOfBirth) }}</dd>
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
import PageHeader from '../shared/PageHeader.vue'

const toast = useToast()

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
  if (props.variant === 'students') {
    return 'Danh sách phân trang, tìm theo từ khóa, xem chi tiết và xóa khi cần.'
  }
  if (props.variant === 'teachers') {
    return 'Theo dõi giáo viên đã đăng ký — tìm kiếm, chi tiết hồ sơ, gỡ tài khoản.'
  }
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

const closeDetail = () => {
  detailOpen.value = false
  detail.value = null
}

const confirmDelete = (row) => {
  const label = props.variant === 'students' ? 'học sinh' : 'giáo viên'
  const msg = `Xóa ${label} "${row.username}"? Thao tác không hoàn tác.`
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
    toast.success('Đã xóa tài khoản.')
    if (detail.value?.userId === userId) closeDetail()
    await load()
  } catch (e) {
    toast.error(e?.payload?.message || e?.message || 'Không xóa được.')
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
