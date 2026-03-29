<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="flex h-full min-h-0 flex-1 flex-col overflow-hidden bg-background-light font-display text-slate-900 transition-colors duration-200 dark:bg-background-dark dark:text-slate-100"
  >
    <TeacherTopHeader class="shrink-0" active-section="exam" />

    <main class="teacher-stitch-main relative flex min-h-0 flex-1 flex-col overflow-hidden">
      <div
        class="portal-scrollbar flex min-h-0 w-full max-w-screen-2xl flex-1 flex-col overflow-y-auto overflow-x-hidden px-4 pb-6 pt-4 sm:px-8 sm:pt-5 lg:px-10"
      >
        <div class="relative mb-5 animate-fade-up">
          <nav class="mb-2 flex flex-wrap items-center gap-2 text-xs font-medium text-slate-500">
            <RouterLink to="/teacher/dashboard" class="transition hover:text-[var(--role-primary)]">Trang chủ</RouterLink>
            <span class="material-symbols-outlined text-[14px] text-slate-400">chevron_right</span>
            <RouterLink to="/teacher/exams" class="transition hover:text-[var(--role-primary)]">Ngân hàng đề</RouterLink>
            <span class="material-symbols-outlined text-[14px] text-slate-400">chevron_right</span>
            <span class="font-semibold text-[var(--role-primary)]">Smart Import Studio</span>
          </nav>
          <p class="portal-kicker">Import workspace</p>
          <h2 class="teacher-page-title text-3xl sm:text-4xl md:text-[2.5rem]">
            Tạo đề thi mới từ tệp
          </h2>
        </div>

        <div class="mb-6 animate-fade-up">
          <div class="flex flex-wrap items-center gap-3 text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
            <template v-for="(step, index) in steps" :key="step">
              <div class="flex items-center gap-3">
                <span
                  class="flex size-7 items-center justify-center rounded-full text-[11px] font-bold"
                  :class="index + 1 <= stepIndex ? 'bg-primary text-white' : 'bg-slate-200 text-slate-500 dark:bg-slate-800 dark:text-slate-400'"
                >
                  {{ index + 1 }}
                </span>
                <span :class="index + 1 === stepIndex ? 'text-slate-900 dark:text-white' : ''">{{ step }}</span>
              </div>
              <span v-if="index < steps.length - 1" class="h-px w-6 bg-slate-200 dark:bg-slate-700"></span>
            </template>
          </div>
        </div>

        <div class="relative space-y-5 animate-fade-up-delay pb-4">
          <section
            v-if="stepIndex === 2"
            class="portal-panel rounded-[1.6rem] p-5 sm:p-6"
          >
            <div class="mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">auto_awesome</span>
              <h3 class="text-lg font-bold">Thông tin & chiến lược import</h3>
            </div>
            <div class="grid gap-4 lg:grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)]">
              <div class="space-y-2">
                <label class="text-sm font-semibold text-slate-700 dark:text-slate-300">Tiêu đề đề thi</label>
                <input
                  v-model="examTitle"
                  class="teacher-stitch-field w-full rounded-lg px-4 py-2.5 outline-none transition-all focus:ring-2 focus:ring-primary/25 focus:border-primary/50"
                  placeholder="VD: Kiểm tra giữa kỳ Giải tích nâng cao Q3"
                  type="text"
                />
              </div>

              <div class="grid gap-3 sm:grid-cols-2">
                <div
                  v-for="metric in importMetrics"
                  :key="metric.label"
                  class="portal-panel-soft rounded-2xl px-4 py-3"
                >
                  <div class="mb-2 flex items-center justify-between gap-2">
                    <span class="text-xs font-bold uppercase tracking-wider text-slate-500">{{ metric.label }}</span>
                    <span class="material-symbols-outlined text-base text-primary">{{ metric.icon }}</span>
                  </div>
                  <p class="text-lg font-black tracking-tight text-slate-900 dark:text-white">{{ metric.value }}</p>
                  <p v-if="metric.help" class="mt-1 text-[11px] text-slate-500 dark:text-slate-400">{{ metric.help }}</p>
                </div>
              </div>
            </div>
          </section>

          <section
            v-if="stepIndex === 2"
            class="portal-panel rounded-[1.6rem] p-5 sm:p-6"
          >
            <div class="mb-3 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div class="flex items-center gap-2">
                <span class="material-symbols-outlined text-primary">upload_file</span>
                <div>
                  <h3 class="text-lg font-bold">Tải nội dung đề thi</h3>
                  <p class="text-sm text-slate-500 dark:text-slate-400">Hỗ trợ parse có cấu trúc cho `CSV/XLSX` và review AI/OCR cho `PDF/Word`.</p>
                </div>
              </div>
              <div class="flex flex-wrap gap-2 text-xs font-semibold">
                <span class="portal-chip rounded-full px-3 py-1.5 text-primary">Preview trước khi tạo đề</span>
                <span class="portal-chip rounded-full px-3 py-1.5 text-slate-600 dark:text-slate-300">{{ selectedFile ? fileTypeLabel : 'Chưa chọn tệp' }}</span>
              </div>
            </div>

            <label class="group relative flex cursor-pointer flex-col items-center justify-center rounded-2xl border-2 border-dashed border-slate-300 p-6 transition-all duration-200 hover:border-primary/50 hover:bg-primary/5 dark:border-slate-600 sm:p-8 md:p-10">
              <input class="absolute inset-0 h-full w-full cursor-pointer opacity-0" type="file" accept=".csv,.xlsx,.pdf,.docx" @change="onFileChange" />
              <div class="mb-3 rounded-full bg-primary/10 p-3 transition-transform group-hover:scale-110 sm:p-4">
                <span class="material-symbols-outlined text-3xl text-primary sm:text-4xl">cloud_upload</span>
              </div>
              <h4 class="mb-1 text-base font-semibold sm:text-lg">Nhấp để tải lên hoặc kéo thả</h4>
              <p class="text-center text-sm text-slate-500 dark:text-slate-400">{{ FILE_FORMAT_DESC }}</p>
              <div class="mt-2 flex flex-wrap gap-3">
                <a :href="getTemplateDownloadUrl('csv')" download class="flex items-center gap-1 text-sm font-semibold text-primary hover:underline">
                  <span class="material-symbols-outlined text-lg">download</span>
                  Mẫu CSV
                </a>
                <a :href="getTemplateDownloadUrl('xlsx')" download class="flex items-center gap-1 text-sm font-semibold text-primary hover:underline">
                  <span class="material-symbols-outlined text-lg">download</span>
                  Mẫu Excel
                </a>
              </div>
              <p v-if="fileName" class="mt-3 text-sm font-semibold text-primary">{{ fileName }}</p>
              <p v-if="selectedFile" class="mt-1 text-xs text-slate-500">Dung lượng: {{ fileSizeLabel }}</p>
            </label>

            <div class="mt-4 flex flex-wrap gap-3">
              <button
                type="button"
                class="app-shell-cta inline-flex min-h-[42px] items-center justify-center gap-2 px-4 py-2.5 text-sm font-bold disabled:cursor-not-allowed disabled:opacity-60"
                :disabled="!selectedFile || isAnalyzingPreview || isSubmitting"
                @click="analyzeImportPreview"
              >
                <span class="material-symbols-outlined text-lg">{{ hasPreview ? 'refresh' : 'psychology' }}</span>
                {{ isAnalyzingPreview ? 'Đang phân tích...' : hasPreview ? 'Phân tích lại' : 'Phân tích & xem trước' }}
              </button>
              <button
                v-if="hasPreview"
                type="button"
                class="portal-panel-soft inline-flex min-h-[42px] items-center justify-center gap-2 px-4 py-2.5 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"
                :disabled="isSavingReview || isSubmitting"
                @click="savePreviewReview()"
              >
                <span class="material-symbols-outlined text-lg">save</span>
                {{ isSavingReview ? 'Đang lưu...' : 'Lưu chỉnh sửa review' }}
              </button>
              <button
                v-if="jobId"
                type="button"
                class="inline-flex min-h-[42px] items-center justify-center gap-2 rounded-xl border border-rose-200 px-4 py-2.5 text-sm font-semibold text-rose-600 transition-colors hover:bg-rose-50 dark:border-rose-900/40 dark:text-rose-300 dark:hover:bg-rose-900/20"
                :disabled="isAnalyzingPreview || isSavingReview || isSubmitting"
                @click="discardPreview"
              >
                <span class="material-symbols-outlined text-lg">close</span>
                Đặt lại preview
              </button>
            </div>
          </section>

          <section
            v-if="stepIndex === 2"
            class="portal-panel rounded-[1.6rem] p-5 sm:p-6"
          >
            <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <div class="flex items-center gap-2">
                  <span class="material-symbols-outlined text-primary">preview</span>
                  <h3 class="text-lg font-bold">Preview câu hỏi trước khi import</h3>
                </div>
                <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">{{ previewGuidance }}</p>
              </div>
              <div v-if="jobId" class="inline-flex items-center gap-2 rounded-full bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                <span class="material-symbols-outlined text-base">sync</span>
                Job #{{ jobId }}
              </div>
            </div>

            <div v-if="isAnalyzingPreview" class="rounded-2xl border border-dashed border-primary/30 bg-primary/5 px-4 py-10 text-center">
              <span class="material-symbols-outlined mb-3 inline-flex rounded-full bg-white p-3 text-3xl text-primary shadow-sm">progress_activity</span>
              <p class="text-base font-bold text-slate-900 dark:text-white">Đang phân tích tệp</p>
              <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Hệ thống đang parse nội dung, chuẩn hóa câu hỏi và gom các mục cần rà soát.</p>
              <div class="mx-auto mt-4 h-2 w-full max-w-xl overflow-hidden rounded-full bg-slate-200 dark:bg-slate-800">
                <div class="h-full rounded-full bg-primary transition-all" :style="{ width: `${Math.max(importProgress, 12)}%` }"></div>
              </div>
            </div>

            <div v-else-if="previewErrorMessage" class="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-6 text-sm text-rose-700 dark:border-rose-900/40 dark:bg-rose-900/10 dark:text-rose-300">
              <div class="flex items-start gap-3">
                <span class="material-symbols-outlined mt-0.5 text-lg">error</span>
                <div>
                  <p class="font-semibold">Không thể tạo preview cho tệp này.</p>
                  <p class="mt-1">{{ previewErrorMessage }}</p>
                </div>
              </div>
            </div>

            <div v-else-if="hasPreview" class="grid gap-5 xl:grid-cols-[minmax(0,1fr)_20rem]">
              <ImportPreviewWorkbench
                :summary="importSummary"
                :questions="reviewedQuestions"
                :disabled="isSavingReview || isSubmitting"
                @update-question="handlePreviewQuestionUpdate"
              />
              <div class="space-y-5">
                <ImportIssuePanel :issues="importIssues" @resolve="handleResolveIssue" />
                <div class="rounded-[1.5rem] border border-slate-200 bg-slate-50/90 p-4 dark:border-slate-800 dark:bg-slate-800/50">
                  <p class="text-sm font-bold text-slate-900 dark:text-white">Gợi ý tự động</p>
                  <ul class="mt-3 space-y-2 text-sm text-slate-600 dark:text-slate-300">
                    <li class="flex gap-2">
                      <span class="material-symbols-outlined text-base text-primary">auto_awesome</span>
                      {{ importSuggestionPrimary }}
                    </li>
                    <li class="flex gap-2">
                      <span class="material-symbols-outlined text-base text-primary">flag</span>
                      {{ importSuggestionSecondary }}
                    </li>
                    <li class="flex gap-2">
                      <span class="material-symbols-outlined text-base text-primary">rule</span>
                      {{ importSuggestionTertiary }}
                    </li>
                  </ul>
                </div>
              </div>
            </div>

            <div v-else class="rounded-2xl border border-dashed border-slate-200 bg-slate-50/90 px-4 py-10 text-center dark:border-slate-700 dark:bg-slate-800/50">
              <span class="material-symbols-outlined mb-3 inline-flex rounded-full bg-white p-3 text-3xl text-primary shadow-sm dark:bg-slate-900">auto_stories</span>
              <p class="text-base font-bold text-slate-900 dark:text-white">Chưa có preview nào</p>
              <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Tải tệp lên và chạy phân tích để mở workspace chỉnh sửa câu hỏi trước khi tạo đề.</p>
            </div>
          </section>

          <section
            v-if="stepIndex === 3"
            class="stitch-ambient-shadow rounded-2xl border border-slate-200/80 bg-white p-5 shadow-soft dark:border-slate-700/80 dark:bg-slate-900 sm:p-6"
          >
            <div class="mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">shield</span>
              <h3 class="text-lg font-bold">Cấu hình giám sát</h3>
            </div>

            <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
              <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-4 py-3 dark:border-slate-800">
                <div>
                  <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Theo dõi chuyển tab</p>
                  <p class="text-xs text-slate-500">Phát hiện đổi tab hoặc ẩn cửa sổ</p>
                </div>
                <input v-model="monitorTabSwitch" type="checkbox" class="h-5 w-5 accent-primary" />
              </label>

              <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-4 py-3 dark:border-slate-800">
                <div>
                  <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Theo dõi blur</p>
                  <p class="text-xs text-slate-500">Ghi nhận khi mất focus</p>
                </div>
                <input v-model="monitorBlur" type="checkbox" class="h-5 w-5 accent-primary" />
              </label>

              <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-4 py-3 dark:border-slate-800">
                <div>
                  <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Thoát fullscreen</p>
                  <p class="text-xs text-slate-500">Cảnh báo khi rời chế độ toàn màn hình</p>
                </div>
                <input v-model="monitorExitFullscreen" type="checkbox" class="h-5 w-5 accent-primary" />
              </label>

              <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-4 py-3 dark:border-slate-800">
                <div>
                  <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Copy/Paste</p>
                  <p class="text-xs text-slate-500">Ghi nhận thao tác sao chép/dán</p>
                </div>
                <input v-model="monitorCopyPaste" type="checkbox" class="h-5 w-5 accent-primary" />
              </label>

              <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-4 py-3 dark:border-slate-800">
                <div>
                  <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Nhàn rỗi</p>
                  <p class="text-xs text-slate-500">Cảnh báo khi không thao tác</p>
                </div>
                <input v-model="monitorIdleTime" type="checkbox" class="h-5 w-5 accent-primary" />
              </label>

              <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-4 py-3 dark:border-slate-800">
                <div>
                  <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">DevTools</p>
                  <p class="text-xs text-slate-500">Phát hiện mở DevTools</p>
                </div>
                <input v-model="monitorDevtools" type="checkbox" class="h-5 w-5 accent-primary" />
              </label>

              <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-4 py-3 dark:border-slate-800">
                <div>
                  <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Duplicate IP</p>
                  <p class="text-xs text-slate-500">Phát hiện IP trùng</p>
                </div>
                <input v-model="monitorDuplicateIp" type="checkbox" class="h-5 w-5 accent-primary" />
              </label>

              <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-4 py-3 dark:border-slate-800">
                <div>
                  <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Nộp nhanh</p>
                  <p class="text-xs text-slate-500">Cảnh báo nộp bài quá nhanh</p>
                </div>
                <input v-model="monitorFastSubmit" type="checkbox" class="h-5 w-5 accent-primary" />
              </label>

              <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-4 py-3 dark:border-slate-800">
                <div>
                  <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Chuột phải</p>
                  <p class="text-xs text-slate-500">Chặn menu chuột phải (copy)</p>
                </div>
                <input v-model="monitorRightClick" type="checkbox" class="h-5 w-5 accent-primary" />
              </label>

              <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-4 py-3 dark:border-slate-800">
                <div>
                  <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Print Screen</p>
                  <p class="text-xs text-slate-500">Phát hiện chụp màn hình</p>
                </div>
                <input v-model="monitorPrintScreen" type="checkbox" class="h-5 w-5 accent-primary" />
              </label>

              <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-4 py-3 dark:border-slate-800">
                <div>
                  <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Đổi câu nhanh</p>
                  <p class="text-xs text-slate-500">Cảnh báo đổi câu liên tục</p>
                </div>
                <input v-model="monitorRapidQuestionSwitch" type="checkbox" class="h-5 w-5 accent-primary" />
              </label>

              <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-4 py-3 dark:border-slate-800">
                <div>
                  <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Đa màn hình</p>
                  <p class="text-xs text-slate-500">Phát hiện nhiều màn hình</p>
                </div>
                <input v-model="monitorMultiMonitor" type="checkbox" class="h-5 w-5 accent-primary" />
              </label>

              <label class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 px-4 py-3 dark:border-slate-800">
                <div>
                  <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Yêu cầu camera/mic</p>
                  <p class="text-xs text-slate-500">Bắt buộc bật camera và micro</p>
                </div>
                <input v-model="requireCameraMic" type="checkbox" class="h-5 w-5 accent-primary" />
              </label>
            </div>
          </section>
        </div>
      </div>

      <footer
        class="shrink-0 border-t border-slate-200/90 bg-white/95 px-4 py-3 shadow-[0_-4px_24px_-12px_rgba(15,23,42,0.12)] dark:border-slate-700/90 dark:bg-slate-900/95 sm:px-5"
      >
        <div class="flex w-full max-w-screen-2xl flex-col gap-3 sm:flex-row sm:items-end sm:justify-between sm:gap-4">
          <p class="max-w-full text-[11px] leading-relaxed text-slate-500 dark:text-slate-400 sm:max-w-[min(100%,34rem)]">
            {{ footerGuidance }}
          </p>
          <div class="flex w-full flex-col gap-2 sm:w-auto sm:flex-row sm:items-center sm:justify-end sm:gap-3">
            <button
              class="min-h-[44px] w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-800 transition-colors hover:bg-slate-100 dark:border-slate-600 dark:text-slate-100 dark:hover:bg-slate-800 sm:w-auto sm:min-w-[9rem]"
              type="button"
              @click="goBack"
            >
              Hủy bản nháp
            </button>
            <button
              v-if="stepIndex === 2 && hasPreview"
              :disabled="isSavingReview || isAnalyzingPreview || isSubmitting"
              class="inline-flex min-h-[44px] w-full items-center justify-center gap-2 rounded-xl border border-slate-200 px-5 py-2.5 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800 sm:w-auto sm:min-w-[11rem]"
              type="button"
              @click="savePreviewReview()"
            >
              <span class="material-symbols-outlined text-lg">save</span>
              {{ isSavingReview ? 'Đang lưu...' : 'Lưu review' }}
            </button>
            <button
              v-if="stepIndex === 2"
              :disabled="isSubmitting || isAnalyzingPreview || (hasPreview && isSavingReview)"
              class="inline-flex min-h-[44px] w-full items-center justify-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-white shadow-md shadow-primary/25 transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto sm:min-w-[11rem]"
              type="button"
              @click="goNext"
            >
              {{ stepTwoPrimaryLabel }}
              <span class="material-symbols-outlined text-lg">arrow_forward</span>
            </button>
            <button
              v-else
              :disabled="isSubmitting"
              class="inline-flex min-h-[44px] w-full items-center justify-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-white shadow-md shadow-primary/25 transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto sm:min-w-[11rem]"
              type="button"
              @click="confirmMonitoring"
            >
              {{ isSubmitting ? 'Đang lưu...' : 'Tiếp tục lập lịch' }}
              <span class="material-symbols-outlined text-lg">arrow_forward</span>
            </button>
          </div>
        </div>
      </footer>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { createExam, updateExam } from '../../services/examService'
import {
  FILE_FORMAT_DESC,
  cancelImportJob,
  confirmImportJob,
  getImportJobPreview,
  getTemplateDownloadUrl,
  reviewImportJob,
  uploadImportJob,
  waitForImportJob
} from '../../services/questionService'
import { useToast } from '../../composables/useToast'
import { RouterLink, useRouter } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'
import ImportPreviewWorkbench from '../import/ImportPreviewWorkbench.vue'
import ImportIssuePanel from '../import/ImportIssuePanel.vue'
import { useImportJobStore } from '../../stores/importJobStore'

const router = useRouter()
const importJobStore = useImportJobStore()
const { jobId, progress: importProgress, reviewedQuestions, summary: importSummary, issues: importIssues, unresolvedIssues, resolvedIssueIds } = storeToRefs(importJobStore)

const isDark = ref(false)
const steps = ['Chọn cách tạo', 'Nhập đề', 'Giám sát', 'Lập lịch', 'Hoàn tất']
const stepIndex = ref(2)
const examTitle = ref('')
const fileName = ref('')
const selectedFile = ref(null)
const isSubmitting = ref(false)
const isAnalyzingPreview = ref(false)
const isSavingReview = ref(false)
const createdExamId = ref(null)
const createdExamTitle = ref('')
const previewErrorMessage = ref('')
const monitorTabSwitch = ref(true)
const monitorBlur = ref(true)
const monitorExitFullscreen = ref(true)
const monitorCopyPaste = ref(true)
const monitorIdleTime = ref(true)
const monitorDevtools = ref(true)
const monitorDuplicateIp = ref(true)
const monitorFastSubmit = ref(true)
const monitorRightClick = ref(true)
const monitorPrintScreen = ref(true)
const monitorRapidQuestionSwitch = ref(true)
const monitorMultiMonitor = ref(true)
const requireCameraMic = ref(true)

const toast = useToast()

const hasPreview = computed(() => reviewedQuestions.value.length > 0)
const unresolvedCount = computed(() => unresolvedIssues.value.length)
const detectedCount = computed(() => Number(importSummary.value?.totalDetected || reviewedQuestions.value.length || 0))
const fileTypeLabel = computed(() => String(importSummary.value?.fileType || fileExtension.value || '').toUpperCase() || 'Unknown')
const parseMethodLabel = computed(() => importSummary.value?.parseMethod || 'Chưa phân tích')
const fileExtension = computed(() => {
  const raw = selectedFile.value?.name || ''
  const idx = raw.lastIndexOf('.')
  return idx >= 0 ? raw.slice(idx + 1).toLowerCase() : ''
})

const fileSizeLabel = computed(() => {
  if (!selectedFile.value) return ''
  const sizeInMb = selectedFile.value.size / (1024 * 1024)
  return `${sizeInMb.toFixed(2)} MB`
})

const importMetrics = computed(() => [
  {
    label: 'Độ phủ parse',
    value: detectedCount.value ? `${detectedCount.value} câu` : 'Chưa có',
    icon: 'format_list_numbered',
    help: hasPreview.value ? 'Số câu được chuẩn hóa vào preview.' : 'Sẽ xuất hiện sau khi chạy phân tích.'
  },
  {
    label: 'Cần rà soát',
    value: hasPreview.value ? `${unresolvedCount.value} mục` : 'Chưa có',
    icon: 'warning',
    help: hasPreview.value ? 'Warning/Error còn lại trong issue panel.' : 'Các lỗi parse sẽ xuất hiện tại đây.'
  },
  {
    label: 'Kiểu tệp',
    value: selectedFile.value ? fileTypeLabel.value : 'Chưa chọn',
    icon: 'description',
    help: selectedFile.value ? 'Định dạng nguồn được phát hiện cho luồng import.' : 'CSV/XLSX/PDF/Word'
  },
  {
    label: 'Phương thức parse',
    value: hasPreview.value ? parseMethodLabel.value : 'Chờ phân tích',
    icon: 'psychology',
    help: hasPreview.value ? 'Cho biết parser hoặc pipeline đang được dùng.' : 'Structured parse hoặc AI/OCR review.'
  }
])

const previewGuidance = computed(() => {
  if (!selectedFile.value) return 'Chọn tệp để bắt đầu Smart Import Studio.'
  if (isAnalyzingPreview.value) return 'Đang dựng preview câu hỏi và issue list từ tệp đã tải lên.'
  if (previewErrorMessage.value) return 'Tệp vừa chọn chưa parse được; hãy điều chỉnh file và thử lại.'
  if (!hasPreview.value) return 'Chạy phân tích để mở workspace chỉnh sửa trước khi tạo đề.'
  if (unresolvedCount.value > 0) return `Còn ${unresolvedCount.value} mục chưa xử lý. Bạn vẫn có thể lưu review rồi tạo đề nháp khi đã chấp nhận các cảnh báo.`
  return 'Preview đã sẵn sàng. Bạn có thể lưu review hoặc tạo đề nháp ngay.'
})

const footerGuidance = computed(() => {
  if (stepIndex.value === 3) return 'Xác nhận các quy tắc giám sát rồi chuyển sang bước lên lịch cho đề vừa tạo.'
  if (!selectedFile.value) return FILE_FORMAT_DESC
  if (isAnalyzingPreview.value) return 'Hệ thống đang phân tích tệp và dựng preview câu hỏi.'
  if (hasPreview.value) return unresolvedCount.value > 0
    ? `Preview đã sẵn sàng nhưng còn ${unresolvedCount.value} mục cần rà soát trong issue panel.`
    : 'Preview đã sạch lỗi. Bạn có thể tạo đề nháp và chuyển sang bước cấu hình giám sát.'
  return 'Sau khi chọn tệp, hãy chạy phân tích để mở workspace review trước khi tạo đề.'
})

const stepTwoPrimaryLabel = computed(() => {
  if (isSubmitting.value) return 'Đang tạo đề...'
  if (isAnalyzingPreview.value) return 'Đang phân tích...'
  return hasPreview.value ? 'Tạo đề nháp' : 'Phân tích & xem trước'
})

const importSuggestionPrimary = computed(() => {
  if (!selectedFile.value) return 'CSV/XLSX phù hợp khi bạn muốn import nhanh và ít bước rà soát nhất.'
  if (fileExtension.value === 'pdf' || fileExtension.value === 'docx') {
    return 'Tệp nguồn dạng tài liệu nên ưu tiên rà lại nội dung câu và đáp án đúng trước khi publish.'
  }
  return 'Với tệp có cấu trúc sẵn, ưu tiên rà lại độ khó và trọng số để ngân hàng đề cân bằng hơn.'
})

const importSuggestionSecondary = computed(() => {
  if (!hasPreview.value) return 'Sau khi preview xong, issue panel sẽ gom các warning/error để xử lý tập trung.'
  if (unresolvedCount.value > 0) return `Hiện còn ${unresolvedCount.value} mục chưa xử lý; hãy dùng issue panel để đánh dấu từng mục đã rà soát.`
  return 'Issue panel đã sạch. Bộ câu hỏi này phù hợp để tạo đề nháp và chuyển sang bước giám sát/lịch thi.'
})

const importSuggestionTertiary = computed(() => {
  if (!hasPreview.value) return 'Khi cần import đề dài, nên dùng preview để sửa trực tiếp thay vì upload lại nhiều lần.'
  const lowConfidenceCount = reviewedQuestions.value.filter((question) => Number(question?.parseConfidence || 0) < 0.8).length
  if (lowConfidenceCount > 0) {
    return `${lowConfidenceCount} câu có confidence thấp; nên đọc nhanh nội dung và đáp án đúng trước khi xác nhận import.`
  }
  return 'Confidence parse đang ổn. Có thể lưu review để chốt phiên bản preview cuối cùng trước khi tạo đề.'
})

const validateFile = (file) => {
  if (!file) return false
  const allowed = [
    'text/csv',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/pdf',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    ''
  ]
  const ext = (file.name || '').toLowerCase()
  const validType = allowed.includes(file.type) || ext.endsWith('.csv') || ext.endsWith('.xlsx') || ext.endsWith('.pdf') || ext.endsWith('.docx')
  if (!validType) {
    toast.error('Định dạng tệp không hợp lệ. Vui lòng chọn CSV, XLSX, PDF hoặc Word.')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    toast.error('Dung lượng tệp vượt quá 10MB.')
    return false
  }
  return true
}

const resetPreviewState = async ({ cancelRemote = false } = {}) => {
  const previousJobId = jobId.value
  importJobStore.reset()
  previewErrorMessage.value = ''
  if (cancelRemote && previousJobId) {
    try {
      await cancelImportJob(previousJobId)
    } catch {
      // Ignore cleanup failures when the UI switches to a new file.
    }
  }
}

const onFileChange = async (event) => {
  const file = event.target.files?.[0] || null
  await resetPreviewState({ cancelRemote: true })
  selectedFile.value = file
  fileName.value = file?.name || ''
  if (!file) return
  if (!validateFile(file)) {
    selectedFile.value = null
    fileName.value = ''
  }
}

const analyzeImportPreview = async () => {
  if (!selectedFile.value) {
    toast.error('Vui lòng chọn tệp (CSV, XLSX, PDF hoặc Word) trước khi phân tích.')
    return false
  }
  if (!validateFile(selectedFile.value)) {
    selectedFile.value = null
    fileName.value = ''
    return false
  }

  await resetPreviewState()
  isAnalyzingPreview.value = true
  try {
    const upload = await uploadImportJob(selectedFile.value)
    importJobStore.startJob({ jobId: upload.jobId, status: upload.status, progress: 12 })
    const statusPayload = await waitForImportJob(upload.jobId, { intervalMs: 900, maxAttempts: 90 })
    if (statusPayload?.status === 'FAILED') {
      throw new Error(statusPayload.errorMessage || 'Không thể phân tích tệp import.')
    }
    if (statusPayload?.status === 'CANCELLED') {
      throw new Error('Import job đã bị hủy trước khi hoàn tất.')
    }
    const preview = await getImportJobPreview(upload.jobId)
    importJobStore.hydratePreview({
      jobId: preview.jobId,
      status: preview.status,
      progress: statusPayload?.progress ?? 100,
      summary: preview.parseSummary,
      questions: preview.questions,
      issues: preview.issues
    })
    toast.success(`Đã tạo preview cho ${preview.parseSummary?.totalDetected || preview.questions?.length || 0} câu hỏi.`)
    return true
  } catch (error) {
    previewErrorMessage.value = error?.message || 'Không thể phân tích tệp import.'
    toast.error(previewErrorMessage.value)
    return false
  } finally {
    isAnalyzingPreview.value = false
  }
}

const savePreviewReview = async ({ silent = false } = {}) => {
  if (!jobId.value || !hasPreview.value) return true
  isSavingReview.value = true
  try {
    const preview = await reviewImportJob(jobId.value, {
      questions: reviewedQuestions.value,
      resolvedIssueIds: resolvedIssueIds.value
    })
    importJobStore.hydratePreview({
      jobId: preview.jobId,
      status: preview.status,
      progress: 100,
      summary: preview.parseSummary,
      questions: preview.questions,
      issues: preview.issues
    })
    if (!silent) {
      toast.success('Đã lưu phiên bản review hiện tại.')
    }
    return true
  } catch (error) {
    if (!silent) {
      toast.error(error?.message || 'Không thể lưu chỉnh sửa preview.')
    }
    return false
  } finally {
    isSavingReview.value = false
  }
}

const discardPreview = async () => {
  await resetPreviewState({ cancelRemote: true })
  toast.info('Đã đặt lại preview hiện tại.')
}

const handlePreviewQuestionUpdate = ({ questionIndex, patch }) => {
  importJobStore.patchQuestion(questionIndex, patch)
}

const handleResolveIssue = (issueId) => {
  importJobStore.markIssueResolved(issueId, true)
}

const goBack = async () => {
  await resetPreviewState({ cancelRemote: true })
  router.push('/teacher/exams')
}

const goNext = async () => {
  if (!hasPreview.value) {
    await analyzeImportPreview()
    return
  }

  if (!examTitle.value.trim()) {
    toast.error('Vui lòng nhập tiêu đề đề thi trước khi tạo đề nháp.')
    return
  }

  isSubmitting.value = true
  try {
    const savedReview = await savePreviewReview({ silent: true })
    if (!savedReview) return

    const createdExam = await createExam({
      title: examTitle.value.trim(),
      description: '',
      durationMinutes: 60,
      isActive: false,
      monitorTabSwitch: monitorTabSwitch.value,
      monitorBlur: monitorBlur.value,
      monitorExitFullscreen: monitorExitFullscreen.value,
      monitorCopyPaste: monitorCopyPaste.value,
      monitorIdleTime: monitorIdleTime.value,
      monitorDevtools: monitorDevtools.value,
      monitorDuplicateIp: monitorDuplicateIp.value,
      monitorFastSubmit: monitorFastSubmit.value,
      monitorRightClick: monitorRightClick.value,
      monitorPrintScreen: monitorPrintScreen.value,
      monitorRapidQuestionSwitch: monitorRapidQuestionSwitch.value,
      monitorMultiMonitor: monitorMultiMonitor.value,
      requireCameraMic: requireCameraMic.value
    })

    const confirmResult = await confirmImportJob(jobId.value, { examId: createdExam.id })
    createdExamId.value = createdExam.id
    createdExamTitle.value = createdExam.title || examTitle.value.trim()
    stepIndex.value = 3
    toast.success(`Đã tạo đề nháp với ${confirmResult?.importedCount || reviewedQuestions.value.length} câu hỏi.`)
  } catch (error) {
    toast.error(error?.message || 'Không thể tạo đề thi từ preview hiện tại.')
  } finally {
    isSubmitting.value = false
  }
}

const confirmMonitoring = async () => {
  if (!createdExamId.value) {
    toast.error('Không tìm thấy đề thi vừa tạo.')
    return
  }

  isSubmitting.value = true
  try {
    await updateExam(createdExamId.value, {
      title: createdExamTitle.value || examTitle.value.trim(),
      description: '',
      durationMinutes: 60,
      isActive: false,
      monitorTabSwitch: monitorTabSwitch.value,
      monitorBlur: monitorBlur.value,
      monitorExitFullscreen: monitorExitFullscreen.value,
      monitorCopyPaste: monitorCopyPaste.value,
      monitorIdleTime: monitorIdleTime.value,
      monitorDevtools: monitorDevtools.value,
      monitorDuplicateIp: monitorDuplicateIp.value,
      monitorFastSubmit: monitorFastSubmit.value,
      monitorRightClick: monitorRightClick.value,
      monitorPrintScreen: monitorPrintScreen.value,
      monitorRapidQuestionSwitch: monitorRapidQuestionSwitch.value,
      monitorMultiMonitor: monitorMultiMonitor.value,
      requireCameraMic: requireCameraMic.value
    })

    router.push({
      path: '/teacher/exams/schedule',
      query: {
        examId: createdExamId.value,
        title: createdExamTitle.value || examTitle.value.trim(),
        source: 'import',
        monitorTabSwitch: String(monitorTabSwitch.value),
        monitorBlur: String(monitorBlur.value),
        monitorExitFullscreen: String(monitorExitFullscreen.value),
        monitorCopyPaste: String(monitorCopyPaste.value),
        monitorIdleTime: String(monitorIdleTime.value),
        monitorDevtools: String(monitorDevtools.value),
        monitorDuplicateIp: String(monitorDuplicateIp.value),
        monitorFastSubmit: String(monitorFastSubmit.value),
        monitorRightClick: String(monitorRightClick.value),
        monitorPrintScreen: String(monitorPrintScreen.value),
        monitorRapidQuestionSwitch: String(monitorRapidQuestionSwitch.value),
        monitorMultiMonitor: String(monitorMultiMonitor.value),
        requireCameraMic: String(requireCameraMic.value)
      }
    })
  } catch (error) {
    toast.error('Không thể lưu cấu hình giám sát. Vui lòng thử lại.')
  } finally {
    isSubmitting.value = false
  }
}

onMounted(() => {
  importJobStore.reset()
})

onUnmounted(() => {
  importJobStore.reset()
})
</script>

<style scoped>
.font-display {
  font-family: var(--font-sans, system-ui, sans-serif);
}

@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes floatSlow {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(0, -14px, 0);
  }
}

@keyframes floatDelay {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(0, 12px, 0);
  }
}

.animate-fade-up {
  animation: fadeUp 0.5s ease-out;
}

.animate-fade-up-delay {
  animation: fadeUp 0.65s ease-out;
}

.animate-float-slow {
  animation: floatSlow 7s ease-in-out infinite;
}

.animate-float-delay {
  animation: floatDelay 8s ease-in-out infinite;
}
</style>