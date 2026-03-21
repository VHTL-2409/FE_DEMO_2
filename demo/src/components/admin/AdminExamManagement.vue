<template>
  <div>
    <header class="mb-8">
      <p class="text-teal-400/90 text-xs font-semibold tracking-[0.25em] uppercase mb-2">Exams</p>
      <h2 class="text-2xl sm:text-3xl font-black tracking-tight bg-gradient-to-r from-white via-slate-100 to-slate-400 bg-clip-text text-transparent">
        Quản lý đề thi
      </h2>
      <p class="mt-2 text-slate-500 text-sm max-w-xl">
        Toàn bộ đề trong hệ thống — bật/tắt hoạt động, xem người tạo và thống kê nhanh.
      </p>
    </header>

    <div class="rounded-2xl border border-white/[0.08] bg-white/[0.03] backdrop-blur-sm overflow-hidden">
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 px-4 sm:px-6 py-4 border-b border-white/[0.06]">
        <p class="text-sm text-slate-400">
          <span class="text-slate-200 font-semibold tabular-nums">{{ totalElements }}</span>
          đề thi
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
        <table class="w-full text-left text-sm min-w-[960px]">
          <thead>
            <tr class="border-b border-white/[0.06] text-slate-500 text-xs uppercase tracking-wide">
              <th class="px-3 sm:px-5 py-3 font-semibold">ID</th>
              <th class="px-3 sm:px-5 py-3 font-semibold">Tiêu đề</th>
              <th class="px-3 sm:px-5 py-3 font-semibold">Mã</th>
              <th class="px-3 sm:px-5 py-3 font-semibold">Phút</th>
              <th class="px-3 sm:px-5 py-3 font-semibold">Hoạt động</th>
              <th class="px-3 sm:px-5 py-3 font-semibold">Bắt đầu → Kết thúc</th>
              <th class="px-3 sm:px-5 py-3 font-semibold">GMT</th>
              <th class="px-3 sm:px-5 py-3 font-semibold">Người tạo</th>
              <th class="px-3 sm:px-5 py-3 font-semibold">CH / Lượt</th>
            </tr>
          </thead>
          <tbody>
            <template v-if="loading && !rows.length">
              <tr class="border-b border-white/[0.04]">
                <td colspan="9" class="px-6 py-12 text-center text-slate-500">Đang tải…</td>
              </tr>
            </template>
            <template v-else-if="!rows.length">
              <tr class="border-b border-white/[0.04]">
                <td colspan="9" class="px-6 py-12 text-center text-slate-500">Chưa có đề thi.</td>
              </tr>
            </template>
            <template v-else>
              <tr
                v-for="row in rows"
                :key="row.id"
                class="border-b border-white/[0.04] hover:bg-white/[0.02] transition-colors"
              >
              <td class="px-3 sm:px-5 py-3 text-slate-500 tabular-nums">{{ row.id }}</td>
              <td class="px-3 sm:px-5 py-3 text-slate-100 font-medium max-w-[220px] truncate" :title="row.title">
                {{ row.title }}
              </td>
              <td class="px-3 sm:px-5 py-3 font-mono text-teal-200/90 text-xs">{{ row.code || '—' }}</td>
              <td class="px-3 sm:px-5 py-3 tabular-nums text-slate-300">{{ row.durationMinutes ?? '—' }}</td>
              <td class="px-3 sm:px-5 py-3">
                <button
                  type="button"
                  :disabled="togglingId === row.id"
                  class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold border transition-colors disabled:opacity-50"
                  :class="
                    row.isActive
                      ? 'border-emerald-500/40 bg-emerald-500/10 text-emerald-300 hover:bg-emerald-500/20'
                      : 'border-slate-600/50 bg-slate-800/40 text-slate-400 hover:bg-slate-700/40'
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
              <td class="px-3 sm:px-5 py-3 text-slate-400 text-xs leading-snug max-w-[200px]">
                <div>{{ formatDt(row.startTime) }}</div>
                <div class="text-slate-600">→</div>
                <div>{{ formatDt(row.endTime) }}</div>
              </td>
              <td class="px-3 sm:px-5 py-3 text-slate-500 text-xs">{{ row.timezone || '—' }}</td>
              <td class="px-3 sm:px-5 py-3 font-mono text-xs text-slate-400">{{ row.createdByUsername }}</td>
              <td class="px-3 sm:px-5 py-3 tabular-nums text-slate-300 text-xs">
                {{ row.questionCount }} / {{ row.attemptCount }}
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { fetchAdminExams, patchAdminExamActive } from '../../services/adminService'
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
