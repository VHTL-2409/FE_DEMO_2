<template>
  <div>
    <header class="mb-8">
      <p class="text-teal-400/90 text-xs font-semibold tracking-[0.25em] uppercase mb-2">{{ sectionEyebrow }}</p>
      <h2 class="text-2xl sm:text-3xl font-black tracking-tight bg-gradient-to-r from-white via-slate-100 to-slate-400 bg-clip-text text-transparent">
        {{ sectionTitle }}
      </h2>
      <p class="mt-2 text-slate-500 text-sm max-w-xl">
        Danh sách tài khoản theo vai trò — email đăng nhập và thông tin hồ sơ (nếu có).
      </p>
    </header>

    <div
      v-if="showSearch"
      class="mb-4 flex flex-col sm:flex-row gap-3 sm:items-center"
    >
      <div class="relative flex-1 max-w-xl">
        <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 text-xl pointer-events-none">search</span>
        <input
          v-model="searchInput"
          type="search"
          placeholder="Tìm theo username, email, họ tên, hiển thị, SĐT…"
          class="w-full pl-10 pr-4 py-2.5 rounded-xl border border-white/10 bg-white/[0.04] text-slate-100 text-sm placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-teal-500/40"
          @keydown.enter.prevent="applySearch"
        />
      </div>
      <div class="flex gap-2">
        <button
          type="button"
          class="px-4 py-2.5 rounded-xl bg-teal-500/20 border border-teal-500/35 text-teal-200 text-sm font-semibold hover:bg-teal-500/30"
          @click="applySearch"
        >
          Tìm
        </button>
        <button
          v-if="appliedQuery"
          type="button"
          class="px-4 py-2.5 rounded-xl border border-white/10 text-slate-400 text-sm hover:bg-white/5"
          @click="clearSearch"
        >
          Xóa bộ lọc
        </button>
      </div>
    </div>

    <div class="rounded-2xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-sm overflow-hidden">
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 px-4 sm:px-6 py-4 border-b border-white/[0.06]">
        <p class="text-sm text-slate-400">
          <span class="text-slate-200 font-semibold tabular-nums">{{ totalElements }}</span>
          tài khoản
          <span v-if="appliedQuery" class="text-slate-500"> · lọc: "{{ appliedQuery }}"</span>
        </p>
        <button
          type="button"
          @click="load"
          :disabled="loading"
          class="inline-flex items-center gap-2 px-3 py-2 rounded-xl border border-white/10 bg-white/5 hover:bg-white/10 text-sm font-semibold disabled:opacity-50 self-start sm:self-auto"
        >
          <span class="material-symbols-outlined text-lg" :class="{ 'animate-spin': loading }">refresh</span>
          Làm mới
        </button>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm min-w-[820px]">
          <thead>
            <tr class="border-b border-white/[0.06] text-slate-500 text-xs uppercase tracking-wide">
              <th class="px-4 sm:px-6 py-3 font-semibold">#</th>
              <th class="px-4 sm:px-6 py-3 font-semibold">Username</th>
              <th class="px-4 sm:px-6 py-3 font-semibold">Email</th>
              <th class="px-4 sm:px-6 py-3 font-semibold">Xác minh</th>
              <th class="px-4 sm:px-6 py-3 font-semibold">Họ tên / hiển thị</th>
              <th class="px-4 sm:px-6 py-3 font-semibold">Điện thoại</th>
              <th v-if="showActions" class="px-4 sm:px-6 py-3 font-semibold text-right">Thao tác</th>
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
                <td class="px-4 sm:px-6 py-3 text-slate-500 tabular-nums">{{ page * size + idx + 1 }}</td>
                <td class="px-4 sm:px-6 py-3 font-mono text-teal-200/90">{{ row.username }}</td>
                <td class="px-4 sm:px-6 py-3 text-slate-300">{{ row.email }}</td>
                <td class="px-4 sm:px-6 py-3">
                  <span
                    class="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium"
                    :class="row.emailVerified ? 'bg-emerald-500/15 text-emerald-300' : 'bg-amber-500/15 text-amber-200'"
                  >
                    {{ row.emailVerified ? 'Đã xác minh' : 'Chưa' }}
                  </span>
                </td>
                <td class="px-4 sm:px-6 py-3 text-slate-300">
                  {{ displayName(row) }}
                </td>
                <td class="px-4 sm:px-6 py-3 text-slate-400">{{ row.phone || '—' }}</td>
                <td v-if="showActions" class="px-4 sm:px-6 py-3 text-right whitespace-nowrap">
                  <button
                    type="button"
                    class="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold text-teal-300 hover:bg-white/10 mr-1"
                    @click="openDetail(row)"
                  >
                    <span class="material-symbols-outlined text-[16px]">visibility</span>
                    Chi tiết
                  </button>
                  <button
                    type="button"
                    class="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold text-rose-300/90 hover:bg-rose-500/10"
                    @click="confirmDelete(row)"
                  >
                    <span class="material-symbols-outlined text-[16px]">delete</span>
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
        class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 px-4 sm:px-6 py-4 border-t border-white/[0.06]"
      >
        <p class="text-xs text-slate-500">
          Trang <span class="text-slate-300 font-semibold">{{ page + 1 }}</span> / {{ totalPages }}
        </p>
        <div class="flex items-center gap-2">
          <button
            type="button"
            :disabled="page <= 0 || loading"
            class="px-3 py-1.5 rounded-lg border border-white/10 text-sm disabled:opacity-40 hover:bg-white/5"
            @click="goPage(page - 1)"
          >
            Trước
          </button>
          <button
            type="button"
            :disabled="page >= totalPages - 1 || loading"
            class="px-3 py-1.5 rounded-lg border border-white/10 text-sm disabled:opacity-40 hover:bg-white/5"
            @click="goPage(page + 1)"
          >
            Sau
          </button>
        </div>
      </div>
    </div>

    <p v-if="errorMsg" class="mt-4 text-rose-400 text-sm font-medium">{{ errorMsg }}</p>

    <!-- Chi tiết -->
    <Teleport to="body">
      <div
        v-if="detailOpen"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm"
        @click.self="closeDetail"
      >
        <div
          class="w-full max-w-lg rounded-2xl border border-white/10 bg-[#0c1220] shadow-2xl max-h-[90vh] overflow-y-auto"
          role="dialog"
          aria-modal="true"
        >
          <div class="flex items-center justify-between px-5 py-4 border-b border-white/[0.06]">
            <h3 class="text-lg font-bold text-white">Chi tiết tài khoản</h3>
            <button type="button" class="p-1 rounded-lg text-slate-400 hover:bg-white/10" @click="closeDetail">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div v-if="detailLoading" class="px-5 py-12 text-center text-slate-500">Đang tải…</div>
          <div v-else-if="detail" class="px-5 py-5 space-y-4 text-sm">
            <div v-if="detail.avatarUrl" class="flex justify-center">
              <img :src="detail.avatarUrl" alt="" class="size-20 rounded-2xl object-cover border border-white/10" />
            </div>
            <dl class="grid grid-cols-1 gap-3">
              <div class="flex justify-between gap-4 border-b border-white/[0.05] pb-2">
                <dt class="text-slate-500">ID</dt>
                <dd class="text-slate-200 font-mono">{{ detail.userId }}</dd>
              </div>
              <div class="flex justify-between gap-4 border-b border-white/[0.05] pb-2">
                <dt class="text-slate-500">Username</dt>
                <dd class="text-teal-200 font-mono">{{ detail.username }}</dd>
              </div>
              <div class="flex justify-between gap-4 border-b border-white/[0.05] pb-2">
                <dt class="text-slate-500">Email</dt>
                <dd class="text-slate-200 break-all">{{ detail.email }}</dd>
              </div>
              <div class="flex justify-between gap-4 border-b border-white/[0.05] pb-2">
                <dt class="text-slate-500">Xác minh email</dt>
                <dd class="text-slate-200">{{ detail.emailVerified ? 'Đã xác minh' : 'Chưa' }}</dd>
              </div>
              <div class="flex justify-between gap-4 border-b border-white/[0.05] pb-2">
                <dt class="text-slate-500">Họ tên</dt>
                <dd class="text-slate-200">{{ detail.fullName || '—' }}</dd>
              </div>
              <div class="flex justify-between gap-4 border-b border-white/[0.05] pb-2">
                <dt class="text-slate-500">Tên hiển thị</dt>
                <dd class="text-slate-200">{{ detail.displayName || '—' }}</dd>
              </div>
              <div class="flex justify-between gap-4 border-b border-white/[0.05] pb-2">
                <dt class="text-slate-500">Điện thoại</dt>
                <dd class="text-slate-200">{{ detail.phone || '—' }}</dd>
              </div>
              <div class="flex justify-between gap-4">
                <dt class="text-slate-500">Ngày sinh</dt>
                <dd class="text-slate-200">{{ formatDob(detail.dateOfBirth) }}</dd>
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

const toast = useToast()

const props = defineProps({
  variant: {
    type: String,
    required: true,
    validator: (v) => v === 'students' || v === 'teachers' || v === 'admins'
  }
})

const sectionEyebrow = computed(() => {
  if (props.variant === 'students') return 'Students'
  if (props.variant === 'teachers') return 'Teachers'
  return 'Administrators'
})

const sectionTitle = computed(() => {
  if (props.variant === 'students') return 'Quản lý học sinh'
  if (props.variant === 'teachers') return 'Quản lý giáo viên'
  return 'Quản lý tài khoản admin'
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
