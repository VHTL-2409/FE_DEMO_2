<template>
  <div class="staff-page-wrap min-h-0 flex-1 gap-6 pb-2">
    <PageHeader
      eyebrow="Exam governance"
      title="Quản lý đề thi"
      subtitle="Xem toàn bộ đề trong hệ thống, trạng thái hoạt động, khung thời gian và thống kê câu hỏi / lượt thi."
    />

    <div class="staff-table-panel rounded-[1.75rem]">
      <div class="staff-toolbar">
        <p class="staff-toolbar-meta">
          <span class="font-semibold tabular-nums text-slate-900 dark:text-slate-100">{{ totalElements }}</span>
          đề thi
        </p>
        <button
          type="button"
          class="staff-action-btn staff-action-btn-neutral self-start disabled:opacity-50 sm:self-auto"
          :disabled="loading"
          @click="load"
        >
          <span class="material-symbols-outlined text-lg" :class="{ 'animate-spin': loading }">refresh</span>
          Làm mới
        </button>
      </div>

      <div class="min-h-0 flex-1 overflow-auto portal-scrollbar">
        <table class="staff-table min-w-[960px]">
          <thead>
            <tr>
              <th>ID</th>
              <th>Tiêu đề</th>
              <th>Mã</th>
              <th>Phút</th>
              <th>Hoạt động</th>
              <th>Thời gian</th>
              <th>GMT</th>
              <th>Người tạo</th>
              <th>CH / Lượt</th>
            </tr>
          </thead>
          <tbody>
            <template v-if="loading && !rows.length">
              <tr>
                <td colspan="9" class="px-6 py-12 text-center text-slate-500">Đang tải…</td>
              </tr>
            </template>
            <template v-else-if="!rows.length">
              <tr>
                <td colspan="9" class="px-6 py-12 text-center text-slate-500">Chưa có đề thi.</td>
              </tr>
            </template>
            <template v-else>
              <tr
                v-for="row in rows"
                :key="row.id"
              >
              <td class="text-slate-500 tabular-nums">{{ row.id }}</td>
              <td class="max-w-[220px] truncate text-slate-900 dark:text-slate-100 font-medium" :title="row.title">
                {{ row.title }}
              </td>
              <td class="font-mono text-primary text-xs">{{ row.code || '—' }}</td>
              <td class="tabular-nums text-slate-700 dark:text-slate-300">{{ row.durationMinutes ?? '—' }}</td>
              <td>
                <button
                  type="button"
                  :disabled="togglingId === row.id"
                  class="staff-action-btn disabled:opacity-50"
                  :class="
                    row.isActive
                      ? 'border border-emerald-300 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 dark:border-emerald-800 dark:bg-emerald-900/20 dark:text-emerald-400'
                      : 'border border-slate-200 bg-slate-100 text-slate-500 hover:bg-slate-200 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300'
                  "
                  @click="toggleActive(row)"
                >
                  <span
                    class="size-1.5 rounded-full"
                    :class="row.isActive ? 'bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.6)]' : 'bg-slate-500'"
                  />
                  {{ row.isActive ? 'Bật' : 'Tắt' }}
                </button>
              </td>
              <td class="text-slate-500 text-xs leading-snug max-w-[200px]">
                <div>{{ formatDt(row.startTime) }}</div>
                <div class="text-slate-600">→</div>
                <div>{{ formatDt(row.endTime) }}</div>
              </td>
              <td class="text-slate-500 text-xs">{{ row.timezone || '—' }}</td>
              <td class="font-mono text-xs text-slate-500">{{ row.createdByUsername }}</td>
              <td class="tabular-nums text-slate-700 dark:text-slate-300 text-xs">
                {{ row.questionCount }} / {{ row.attemptCount }}
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
          Trang <span class="text-slate-900 dark:text-slate-100 font-semibold">{{ page + 1 }}</span> / {{ totalPages }}
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchAdminExams, patchAdminExamActive } from '../../services/adminService'
import PageHeader from '../shared/PageHeader.vue'
import { useToast } from '../../composables/useToast'

const toast = useToast()
const loading = ref(true)
const errorMsg = ref('')
const rows = ref([])
const page = ref(0)
const size = ref(20)
const totalElements = ref(0)
const totalPages = ref(0)
const togglingId = ref(null)

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
