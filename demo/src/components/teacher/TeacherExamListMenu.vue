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
          <p class="text-slate-500 dark:text-slate-400 mb-8">Quản lý toàn bộ đề thi đã tạo, xem nhanh báo cáo và xóa đề thi.</p>

          <p v-if="errorMessage" class="mb-4 text-sm text-rose-600">{{ errorMessage }}</p>
          <p v-if="successMessage" class="mb-4 text-sm text-emerald-700 dark:text-emerald-400">{{ successMessage }}</p>

          <div class="bg-white dark:bg-slate-900 rounded-xl shadow-sm border border-slate-200 dark:border-slate-800 overflow-hidden">
            <div v-if="isLoading" class="p-8 text-sm text-slate-500 dark:text-slate-400">Đang tải danh sách đề thi...</div>
            <div v-else-if="formattedExams.length === 0" class="p-8 text-sm text-slate-500 dark:text-slate-400">Chưa có đề thi nào. Hãy tạo đề thi đầu tiên.</div>
            <table v-else class="w-full text-left border-collapse">
              <thead>
                <tr class="bg-slate-50 dark:bg-slate-800/50 border-b border-slate-200 dark:border-slate-800">
                  <th class="px-6 py-4 text-sm font-semibold text-slate-600 dark:text-slate-400">Tiêu đề đề thi</th>
                  <th class="px-6 py-4 text-sm font-semibold text-slate-600 dark:text-slate-400">Môn học</th>
                  <th class="px-6 py-4 text-sm font-semibold text-slate-600 dark:text-slate-400">Số câu hỏi</th>
                  <th class="px-6 py-4 text-sm font-semibold text-slate-600 dark:text-slate-400">Cập nhật lần cuối</th>
                  <th class="px-6 py-4 text-sm font-semibold text-slate-600 dark:text-slate-400">Trạng thái</th>
                  <th class="px-6 py-4 text-sm font-semibold text-slate-600 dark:text-slate-400">Thao tác</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                <tr v-for="exam in formattedExams" :key="exam.id" class="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition-colors">
                  <td class="px-6 py-4 font-medium text-slate-900 dark:text-slate-100">{{ exam.title }}</td>
                  <td class="px-6 py-4 text-slate-500 dark:text-slate-400">{{ exam.subject }}</td>
                  <td class="px-6 py-4 text-slate-500 dark:text-slate-400">{{ exam.questions }}</td>
                  <td class="px-6 py-4 text-slate-500 dark:text-slate-400">{{ exam.modified }}</td>
                  <td class="px-6 py-4">
                    <span :class="exam.statusClass" class="px-2 py-1 text-xs font-bold rounded">{{ exam.status }}</span>
                  </td>
                  <td class="px-6 py-4">
                    <div class="flex gap-2">
                      <button @click="goToReview(exam)" class="px-3 py-1.5 text-xs font-semibold text-blue-600 bg-blue-50 hover:bg-blue-100 dark:bg-blue-900/20 dark:text-blue-400 rounded transition-colors" type="button">Xem</button>
                      <button @click="openDeleteModal(exam)" class="px-3 py-1.5 text-xs font-semibold text-rose-600 bg-rose-50 hover:bg-rose-100 dark:bg-rose-900/20 dark:text-rose-400 rounded transition-colors" type="button">Xóa</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="formattedExams.length > 0" class="p-4 bg-slate-50 dark:bg-slate-800/30 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between">
              <p class="text-xs text-slate-500">Hiển thị {{ formattedExams.length }} đề thi</p>
              <div class="flex gap-2">
                <button class="px-3 py-1 text-xs font-semibold rounded bg-white dark:bg-slate-700 border border-slate-200 dark:border-slate-600 disabled:opacity-50" disabled type="button">Trước</button>
                <button class="px-3 py-1 text-xs font-semibold rounded bg-white dark:bg-slate-700 border border-slate-200 dark:border-slate-600 disabled:opacity-50" disabled type="button">Tiếp</button>
              </div>
            </div>
          </div>
        </section>
      </main>

      <div
        v-if="showDeleteModal && examPendingDelete"
        class="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 px-4"
      >
        <div class="w-full max-w-md rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xl p-6">
          <h3 class="text-lg font-bold text-slate-900 dark:text-slate-100 mb-2">Xác nhận xóa đề thi</h3>
          <p class="text-sm text-slate-600 dark:text-slate-400 mb-4">Bạn có chắc chắn muốn xóa đề thi này không? Hành động này không thể hoàn tác.</p>

          <div class="rounded-lg border border-rose-200 bg-rose-50 dark:bg-rose-900/20 dark:border-rose-800 p-3 text-sm mb-5">
            <p><span class="font-semibold">Tiêu đề:</span> {{ examPendingDelete.title }}</p>
            <p><span class="font-semibold">Môn học:</span> {{ examPendingDelete.subject }}</p>
            <p><span class="font-semibold">Số câu:</span> {{ examPendingDelete.questions }}</p>
            <p><span class="font-semibold">Trạng thái:</span> {{ examPendingDelete.status }}</p>
            <p><span class="font-semibold">Cập nhật:</span> {{ examPendingDelete.modified }}</p>
            <p><span class="font-semibold">ID:</span> {{ examPendingDelete.id }}</p>
          </div>

          <div class="flex justify-end gap-3">
            <button
              class="px-4 py-2 rounded-lg border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800"
              type="button"
              :disabled="isDeleting"
              @click="closeDeleteModal"
            >
              Hủy
            </button>
            <button
              class="px-4 py-2 rounded-lg bg-rose-600 text-white font-semibold hover:bg-rose-700 disabled:opacity-60"
              type="button"
              :disabled="isDeleting"
              @click="confirmDeleteExam"
            >
              {{ isDeleting ? 'Đang xóa...' : 'Xác nhận xóa' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ApiError } from '../../services/apiClient'
import { deleteExam, listExams } from '../../services/examService'
import { useRouter } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const router = useRouter()
const isDark = ref(false)
const exams = ref([])
const isLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const showDeleteModal = ref(false)
const examPendingDelete = ref(null)
const isDeleting = ref(false)

const getExamSortTime = (exam) => {
  const candidates = [exam.startTime, exam.endTime, exam.createdAt, exam.updatedAt]
  for (const value of candidates) {
    if (!value) continue
    const time = new Date(value).getTime()
    if (!Number.isNaN(time)) return time
  }
  return 0
}

const formattedExams = computed(() => exams.value
  .slice()
  .sort((a, b) => getExamSortTime(b) - getExamSortTime(a))
  .map((exam) => ({
    id: exam.id,
    title: exam.title,
    subject: exam.description || '-',
    questions: String(exam.questionCount || 0),
    modified: exam.endTime ? new Date(exam.endTime).toLocaleString() : '-',
    status: exam.isActive ? 'Đã xuất bản' : 'Bản nháp',
    statusClass: exam.isActive
      ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
      : 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400'
  })))

const goToReview = (exam) => {
  router.push({
    path: '/teacher/exams/review/summary',
    query: { title: exam.title, examId: exam.id }
  })
}

const openDeleteModal = (exam) => {
  successMessage.value = ''
  examPendingDelete.value = exam
  showDeleteModal.value = true
}

const closeDeleteModal = () => {
  if (isDeleting.value) return
  showDeleteModal.value = false
  examPendingDelete.value = null
}

const confirmDeleteExam = async () => {
  if (!examPendingDelete.value) return

  const deletingExamTitle = examPendingDelete.value.title
  errorMessage.value = ''
  successMessage.value = ''
  isDeleting.value = true
  try {
    await deleteExam(examPendingDelete.value.id)
    exams.value = exams.value.filter((item) => item.id !== examPendingDelete.value.id)
    showDeleteModal.value = false
    examPendingDelete.value = null
    successMessage.value = `Đã xóa đề thi "${deletingExamTitle}" thành công.`
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : 'Không thể xóa đề thi.'
  } finally {
    isDeleting.value = false
  }
}

const loadExams = async () => {
  isLoading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    exams.value = await listExams()
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : 'Không thể tải danh sách đề thi.'
  } finally {
    isLoading.value = false
  }
}

onMounted(loadExams)
</script>
