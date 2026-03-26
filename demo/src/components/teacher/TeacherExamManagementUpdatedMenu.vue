<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="flex h-full min-h-0 flex-1 flex-col overflow-hidden bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100"
  >
    <div class="layout-container flex min-h-0 flex-1 grow flex-col overflow-hidden">
      <TeacherTopHeader class="shrink-0" active-section="exam" />

      <main class="teacher-page-shell portal-scrollbar max-w-[1200px] min-h-0 flex-1 overflow-y-auto overflow-x-hidden">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/10 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-20 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>
        <div class="flex flex-col gap-2 mb-6 animate-fade-up">
          <h1 class="text-slate-900 dark:text-slate-100 text-4xl font-black leading-tight tracking-tight">Tạo và quản lý đề thi</h1>
          <p class="text-slate-500 dark:text-slate-400 text-lg">Quản lý các bài đánh giá hiện có hoặc tạo mới bằng công cụ nhập liệu tối ưu.</p>
        </div>

        <div class="mb-10 animate-fade-up">
          <div class="flex flex-wrap items-center gap-3 text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
            <template v-for="(step, index) in steps" :key="step">
              <div class="flex items-center gap-3">
                <span
                  class="size-7 rounded-full flex items-center justify-center text-[11px] font-bold"
                  :class="index + 1 <= currentStep ? 'bg-primary text-white' : 'bg-slate-200 text-slate-500 dark:bg-slate-800 dark:text-slate-400'"
                >
                  {{ index + 1 }}
                </span>
                <span :class="index + 1 === currentStep ? 'text-slate-900 dark:text-white' : ''">{{ step }}</span>
              </div>
              <span v-if="index < steps.length - 1" class="h-px w-6 bg-slate-200 dark:bg-slate-700"></span>
            </template>
          </div>
        </div>

        <section class="mb-12 animate-fade-up-delay">
          <h2 class="text-slate-900 dark:text-slate-100 text-xl font-bold mb-6 flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">add_circle</span>
            Tạo đề thi mới
          </h2>
          <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div class="lg:col-span-2 group bg-white dark:bg-slate-900 rounded-2xl shadow-soft border border-slate-200/80 dark:border-slate-700/80 overflow-hidden hover:-translate-y-0.5 hover:shadow-card-hover transition-all duration-200">
              <div class="flex flex-col md:flex-row h-full">
                <div class="md:w-1/3 bg-primary/5 dark:bg-primary/20 flex items-center justify-center p-8 border-r border-slate-100 dark:border-slate-800">
                  <div class="text-center">
                    <span class="material-symbols-outlined text-6xl text-primary mb-2">upload_file</span>
                    <p class="text-xs font-bold uppercase tracking-wider text-primary">Nhập hàng loạt</p>
                  </div>
                </div>
                <div class="p-8 flex flex-col justify-between flex-1">
                  <div>
                    <h3 class="text-slate-900 dark:text-slate-100 text-xl font-bold mb-2">Nhập dữ liệu đề thi</h3>
                    <p class="text-slate-500 dark:text-slate-400 mb-6">
                      Nhanh chóng điền đề thi với hàng trăm câu hỏi theo mẫu chuẩn. Hỗ trợ tệp <b>CSV</b> và <b>XLSX</b>.
                    </p>
                    <div class="flex flex-wrap gap-4 items-center">
                      <button @click="goToCreateExam" class="bg-primary text-white hover:bg-primary/90 px-6 py-2.5 rounded-lg font-semibold flex items-center gap-2 transition-colors" type="button">
                        <span class="material-symbols-outlined">publish</span>
                        Tải tệp lên
                      </button>
                      <a :href="getTemplateDownloadUrl('xlsx')" download class="bg-primary/10 text-primary hover:bg-primary/20 dark:bg-primary/20 dark:text-primary/100 px-6 py-2.5 rounded-lg font-semibold flex items-center gap-2 transition-colors">
                        <span class="material-symbols-outlined">download</span>
                        Tải mẫu Excel
                      </a>
                    </div>
                  </div>
                  <div class="mt-6 flex items-center gap-2 text-xs text-slate-400">
                    <span class="material-symbols-outlined text-sm">info</span>
                    <span>Kích thước tệp tối đa: 10MB. Cột: content, optionA, optionB, optionC, optionD, correctAnswer (A/B/C/D), scoreWeight.</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="bg-white dark:bg-slate-900 rounded-2xl shadow-soft border border-slate-200/80 dark:border-slate-700/80 p-8 flex flex-col items-center justify-center text-center hover:-translate-y-0.5 hover:shadow-card-hover transition-all duration-200 group">
              <div class="size-16 bg-slate-100 dark:bg-slate-800 rounded-full flex items-center justify-center mb-4 group-hover:bg-primary group-hover:text-white transition-colors">
                <span class="material-symbols-outlined text-3xl">edit_note</span>
              </div>
              <h3 class="text-slate-900 dark:text-slate-100 text-lg font-bold mb-2">Nhập thủ công</h3>
              <p class="text-slate-500 dark:text-slate-400 text-sm mb-6">Tạo từng câu hỏi bằng trình soạn thảo văn bản và tải phương tiện lên.</p>
              <button @click="goToManual" class="w-full py-2.5 border-2 border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 rounded-lg font-semibold hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors" type="button">
                Bắt đầu soạn thảo
              </button>
            </div>
          </div>
        </section>

        <section class="animate-fade-up-delay">
          <div class="bg-white dark:bg-slate-900 rounded-xl shadow-sm border border-slate-200 dark:border-slate-800 p-8 flex flex-col md:flex-row items-center justify-between gap-5">
            <div>
              <h2 class="text-slate-900 dark:text-slate-100 text-xl font-bold mb-1">Danh sách đề thi đã tách riêng</h2>
              <p class="text-slate-500 dark:text-slate-400 text-sm">Chuyển sang tab "Danh sách đề thi" trên thanh menu để xem, xóa và quản lý toàn bộ đề thi.</p>
            </div>
            <button @click="goToExamList" class="bg-primary text-white hover:bg-primary/90 px-6 py-2.5 rounded-lg font-semibold flex items-center gap-2 transition-colors" type="button">
              <span class="material-symbols-outlined">list_alt</span>
              Mở danh sách đề thi
            </button>
          </div>
        </section>
      </main>

      <footer class="mt-auto shrink-0 border-t border-slate-200 py-6 px-6 text-center dark:border-slate-800 sm:px-10">
        <p class="text-slate-500 text-sm">© 2023 Exam Manager Pro. Đã đăng ký bản quyền.</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getTemplateDownloadUrl } from '../../services/questionService'
import TeacherTopHeader from './TeacherTopHeader.vue'

const router = useRouter()
const isDark = ref(false)
const steps = ['Chọn cách tạo', 'Nhập đề', 'Lập lịch', 'Hoàn tất']
const currentStep = 1

const goToCreateExam = () => {
  router.push('/teacher/exams/create')
}

const goToManual = () => {
  router.push('/teacher/exams/manual')
}

const goToExamList = () => {
  router.push('/teacher/exams/list')
}
</script>

