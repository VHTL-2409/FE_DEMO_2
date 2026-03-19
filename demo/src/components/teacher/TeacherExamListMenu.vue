<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen"
  >
    <div class="layout-container flex h-full grow flex-col">
      <TeacherTopHeader active-section="exam-list" />

      <main class="teacher-page-shell max-w-[1200px]">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/10 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-20 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

        <section class="animate-fade-up">
          <h1 class="text-slate-900 dark:text-slate-100 text-3xl font-black leading-tight tracking-tight mb-2">Danh sách đề thi</h1>
          <p class="text-slate-500 dark:text-slate-400 mb-6">Quản lý toàn bộ đề thi đã tạo, xem nhanh báo cáo và xóa đề thi.</p>

          <div class="grid grid-cols-1 lg:grid-cols-[1.4fr_0.8fr_0.7fr_auto] gap-3 mb-6">
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 text-lg">search</span>
              <input
                v-model.trim="searchQuery"
                class="w-full pl-11 pr-4 py-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-sm font-medium text-slate-700 dark:text-slate-200 placeholder:text-slate-400 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                type="text"
                placeholder="Tìm theo tên đề thi"
              />
            </div>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 text-lg">event</span>
              <input
                v-model="filterDate"
                class="w-full pl-11 pr-4 py-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-sm font-medium text-slate-700 dark:text-slate-200 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                type="date"
              />
            </div>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 text-lg">filter_alt</span>
              <select
                v-model="statusFilter"
                class="w-full pl-11 pr-4 py-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-sm font-medium text-slate-700 dark:text-slate-200 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all appearance-none"
              >
                <option value="all">Tất cả trạng thái</option>
                <option value="draft">Bản nháp</option>
                <option value="upcoming">Chưa bắt đầu</option>
                <option value="started">Đã bắt đầu</option>
                <option value="ended">Đã kết thúc</option>
              </select>
            </div>
            <button
              class="px-5 py-3 rounded-xl border border-slate-200 dark:border-slate-800 text-sm font-semibold text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-all"
              type="button"
              @click="resetFilters"
            >
              Xóa lọc
            </button>
          </div>

          <div class="bg-white dark:bg-slate-900 rounded-2xl shadow-sm border border-slate-200 dark:border-slate-800 overflow-hidden">
            <div v-if="isLoading" class="p-8 text-sm text-slate-500 dark:text-slate-400">Đang tải danh sách đề thi...</div>
            <div v-else-if="filteredExams.length === 0" class="p-8 text-sm text-slate-500 dark:text-slate-400">Không có đề thi phù hợp với bộ lọc hiện tại.</div>
            <table v-else class="w-full text-left border-collapse">
              <thead>
                <tr class="bg-slate-50 dark:bg-slate-800/50 border-b border-slate-200 dark:border-slate-800">
                  <th class="px-6 py-3 text-xs font-bold uppercase tracking-wide text-slate-500 dark:text-slate-400">Tiêu đề đề thi</th>
                  <th class="px-6 py-3 text-xs font-bold uppercase tracking-wide text-slate-500 dark:text-slate-400">Số câu hỏi</th>
                  <th class="px-6 py-3 text-xs font-bold uppercase tracking-wide text-slate-500 dark:text-slate-400">Cập nhật lần cuối</th>
                  <th class="px-6 py-3 text-xs font-bold uppercase tracking-wide text-slate-500 dark:text-slate-400">Trạng thái</th>
                  <th class="px-6 py-3 text-xs font-bold uppercase tracking-wide text-slate-500 dark:text-slate-400">Thao tác</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                <tr v-for="exam in paginatedExams" :key="exam.id" class="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition-colors">
                  <td class="px-6 py-4">
                    <div class="flex flex-col">
                      <span class="text-sm font-semibold text-slate-900 dark:text-slate-100">{{ exam.title }}</span>
                      <span class="text-xs text-slate-500 dark:text-slate-400">ID: {{ exam.id }}</span>
                    </div>
                  </td>
                  <td class="px-6 py-4 text-sm text-slate-600 dark:text-slate-300">{{ exam.questions }}</td>
                  <td class="px-6 py-4 text-sm text-slate-600 dark:text-slate-300">{{ exam.modified }}</td>
                  <td class="px-6 py-4">
                    <span :class="exam.statusClass" class="px-2.5 py-1 text-[11px] font-bold rounded-full">{{ exam.status }}</span>
                  </td>
                  <td class="px-6 py-4">
                    <div class="flex flex-wrap gap-2">
                      <button v-if="canEditExam(exam)" @click="goToEdit(exam)" class="px-3 py-1.5 text-xs font-semibold text-emerald-600 bg-emerald-50 hover:bg-emerald-100 dark:bg-emerald-900/20 dark:text-emerald-400 rounded-lg transition-colors" type="button">Chỉnh sửa</button>
                      <button v-if="canCreateNewSession(exam)" @click="goToNewSession(exam)" class="px-3 py-1.5 text-xs font-semibold text-indigo-600 bg-indigo-50 hover:bg-indigo-100 dark:bg-indigo-900/20 dark:text-indigo-400 rounded-lg transition-colors" type="button">Tạo đợt thi mới</button>
                      <button @click="goToReview(exam)" class="px-3 py-1.5 text-xs font-semibold text-blue-600 bg-blue-50 hover:bg-blue-100 dark:bg-blue-900/20 dark:text-blue-400 rounded-lg transition-colors" type="button">Xem</button>
                      <button @click="openDeleteModal(exam)" class="px-3 py-1.5 text-xs font-semibold text-rose-600 bg-rose-50 hover:bg-rose-100 dark:bg-rose-900/20 dark:text-rose-400 rounded-lg transition-colors" type="button">Xóa</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="filteredExams.length > 0" class="p-4 bg-slate-50 dark:bg-slate-800/30 border-t border-slate-200 dark:border-slate-800 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
              <p class="text-xs font-medium text-slate-500">Hiển thị {{ filteredExams.length }} đề thi</p>
              <div class="flex items-center gap-2">
                <button
                  type="button"
                  @click="goToPrevPage"
                  :disabled="currentPage <= 1"
                  class="px-3 py-1 text-xs font-semibold rounded bg-white dark:bg-slate-700 border border-slate-200 dark:border-slate-600 hover:bg-slate-50 dark:hover:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Trước
                </button>
                <span class="text-xs text-slate-600 dark:text-slate-300">Trang {{ currentPage }} / {{ totalPages }}</span>
                <button
                  type="button"
                  @click="goToNextPage"
                  :disabled="currentPage >= totalPages"
                  class="px-3 py-1 text-xs font-semibold rounded bg-white dark:bg-slate-700 border border-slate-200 dark:border-slate-600 hover:bg-slate-50 dark:hover:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Tiếp
                </button>
              </div>
            </div>
          </div>
        </section>
      </main>

      <div
        v-if="showDeleteModal && examPendingDelete"
        class="modal-overlay z-[60]"
        role="dialog"
        aria-modal="true"
        aria-labelledby="delete-exam-title"
        @click.self="closeDeleteModal"
      >
        <div class="modal-content w-full max-w-md">
          <div class="modal-header">
            <div class="flex items-center gap-3">
              <div class="size-10 rounded-xl bg-rose-100 dark:bg-rose-500/20 flex items-center justify-center">
                <span class="material-symbols-outlined text-rose-600 dark:text-rose-400 text-xl">delete_forever</span>
              </div>
              <div>
                <h3 id="delete-exam-title" class="text-lg font-bold text-slate-900 dark:text-slate-100">Xác nhận xóa đề thi</h3>
                <p class="text-sm text-slate-500 dark:text-slate-400 mt-0.5">Hành động này không thể hoàn tác</p>
              </div>
            </div>
            <button type="button" class="modal-close-btn" aria-label="Đóng" @click="closeDeleteModal">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
          <div class="modal-body">
            <p class="text-sm text-slate-600 dark:text-slate-300 mb-4">Bạn có chắc chắn muốn xóa đề thi này không?</p>
            <div class="rounded-xl border border-rose-200 bg-rose-50 dark:bg-rose-900/20 dark:border-rose-800 p-4 text-sm space-y-2">
              <p><span class="font-semibold text-slate-700 dark:text-slate-300">Tiêu đề:</span> <span class="text-slate-900 dark:text-slate-100">{{ examPendingDelete.title }}</span></p>
              <p><span class="font-semibold text-slate-700 dark:text-slate-300">Số câu:</span> <span class="text-slate-900 dark:text-slate-100">{{ examPendingDelete.questions }}</span></p>
              <p><span class="font-semibold text-slate-700 dark:text-slate-300">Trạng thái:</span> <span class="text-slate-900 dark:text-slate-100">{{ examPendingDelete.status }}</span></p>
                  <p><span class="font-semibold text-slate-700 dark:text-slate-300">Cập nhật:</span> <span class="text-slate-900 dark:text-slate-100">{{ examPendingDelete.modified }}</span></p>
            </div>
          </div>
          <div class="modal-footer">
            <button
              class="px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-600 text-slate-700 dark:text-slate-300 font-semibold hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors disabled:opacity-50"
              type="button"
              :disabled="isDeleting"
              @click="closeDeleteModal"
            >
              Hủy
            </button>
            <button
              class="px-4 py-2.5 rounded-xl bg-rose-600 text-white font-semibold hover:bg-rose-700 transition-colors disabled:opacity-60 flex items-center gap-2"
              type="button"
              :disabled="isDeleting"
              @click="confirmDeleteExam"
            >
              <span class="material-symbols-outlined text-lg" v-if="isDeleting">hourglass_empty</span>
              {{ isDeleting ? 'Đang xóa...' : 'Xác nhận xóa' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ApiError } from '../../services/apiClient'
import { deleteExam, listExams } from '../../services/examService'
import { useRouter } from 'vue-router'
import { useToast } from '../../composables/useToast'
import TeacherTopHeader from './TeacherTopHeader.vue'

const router = useRouter()
const isDark = ref(false)
const exams = ref([])
const isLoading = ref(false)
const showDeleteModal = ref(false)
const examPendingDelete = ref(null)
const isDeleting = ref(false)
const searchQuery = ref('')
const filterDate = ref('')
const statusFilter = ref('all')
const toast = useToast()

const getExamSortTime = (exam) => {
  const candidates = [exam.startTime, exam.endTime, exam.createdAt, exam.updatedAt]
  for (const value of candidates) {
    if (!value) continue
    const time = new Date(value).getTime()
    if (!Number.isNaN(time)) return time
  }
  return 0
}

const getExamStatusMeta = (exam) => {
  if (!exam.isActive) {
    return {
      key: 'draft',
      label: 'Bản nháp',
      className: 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300'
    }
  }

  const nowMs = Date.now()
  const startMs = new Date(exam.startTime || '').getTime()
  const endMs = new Date(exam.endTime || '').getTime()

  if (!Number.isNaN(startMs) && nowMs < startMs) {
    return {
      key: 'upcoming',
      label: 'Chưa bắt đầu',
      className: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400'
    }
  }

  if (!Number.isNaN(startMs) && !Number.isNaN(endMs) && nowMs >= startMs && nowMs <= endMs) {
    return {
      key: 'started',
      label: 'Đã bắt đầu',
      className: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400'
    }
  }

  return {
    key: 'ended',
    label: 'Đã kết thúc',
    className: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400'
  }
}

const formatDateOnly = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

const formattedExams = computed(() => exams.value
  .slice()
  .sort((a, b) => getExamSortTime(b) - getExamSortTime(a))
  .map((exam) => {
    const statusMeta = getExamStatusMeta(exam)
    const dateSource = exam.endTime || exam.startTime
    return {
      id: exam.id,
      title: exam.title,
      questions: String(exam.questionCount || 0),
      modified: dateSource ? new Date(dateSource).toLocaleString() : '-',
      modifiedDate: formatDateOnly(dateSource),
      status: statusMeta.label,
      statusKey: statusMeta.key,
      statusClass: statusMeta.className
    }
  }))

const filteredExams = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return formattedExams.value.filter((exam) => {
    if (query && !exam.title.toLowerCase().includes(query)) {
      return false
    }
    if (statusFilter.value !== 'all' && exam.statusKey !== statusFilter.value) {
      return false
    }
    if (filterDate.value && exam.modifiedDate !== filterDate.value) {
      return false
    }
    return true
  })
})

const PAGE_SIZE = 10
const currentPage = ref(1)

const paginatedExams = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return filteredExams.value.slice(start, start + PAGE_SIZE)
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredExams.value.length / PAGE_SIZE)))

const goToPrevPage = () => {
  if (currentPage.value > 1) currentPage.value--
}

const goToNextPage = () => {
  if (currentPage.value < totalPages.value) currentPage.value++
}

const canEditExam = (exam) => exam.statusKey === 'upcoming' || exam.statusKey === 'draft'

const canCreateNewSession = (exam) => exam.statusKey === 'ended'

const goToNewSession = (exam) => {
  router.push({
    path: '/teacher/exams/new-session',
    query: { examId: exam.id, title: exam.title }
  })
}

const goToEdit = (exam) => {
  router.push({
    path: '/teacher/exams/schedule',
    query: {
      examId: exam.id,
      title: exam.title,
      mode: 'edit'
    }
  })
}

const goToReview = (exam) => {
  router.push({
    path: '/teacher/exams/review/summary',
    query: { title: exam.title, examId: exam.id }
  })
}

const openDeleteModal = (exam) => {
  examPendingDelete.value = exam
  showDeleteModal.value = true
}

const closeDeleteModal = () => {
  if (isDeleting.value) return
  showDeleteModal.value = false
  examPendingDelete.value = null
}

const resetFilters = () => {
  searchQuery.value = ''
  filterDate.value = ''
  statusFilter.value = 'all'
  currentPage.value = 1
}

const confirmDeleteExam = async () => {
  if (!examPendingDelete.value) return

  const deletingExamTitle = examPendingDelete.value.title
  isDeleting.value = true
  try {
    await deleteExam(examPendingDelete.value.id)
    exams.value = exams.value.filter((item) => item.id !== examPendingDelete.value.id)
    showDeleteModal.value = false
    examPendingDelete.value = null
    toast.success(`Đã xóa đề thi "${deletingExamTitle}" thành công.`)
  } catch (error) {
    toast.error(error instanceof ApiError ? error.message : 'Không thể xóa đề thi.')
  } finally {
    isDeleting.value = false
  }
}

const loadExams = async () => {
  isLoading.value = true
  try {
    exams.value = await listExams()
  } catch (error) {
    toast.error(error instanceof ApiError ? error.message : 'Không thể tải danh sách đề thi.')
  } finally {
    isLoading.value = false
  }
}

watch([searchQuery, filterDate, statusFilter], () => {
  currentPage.value = 1
})

onMounted(loadExams)
</script>
