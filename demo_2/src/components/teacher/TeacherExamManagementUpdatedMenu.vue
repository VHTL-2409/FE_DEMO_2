<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="flex h-full min-h-0 flex-1 flex-col overflow-hidden bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100"
  >
    <div class="layout-container flex min-h-0 flex-1 grow flex-col overflow-hidden">
      <TeacherTopHeader class="shrink-0" active-section="exam" />

      <main class="teacher-stitch-main teacher-page-shell portal-scrollbar relative mx-auto w-full max-w-none min-h-0 flex-1 overflow-y-auto overflow-x-hidden px-3 sm:px-4 lg:px-5">
        <div
          class="exam-hub-hero relative mb-8 animate-fade-up overflow-hidden rounded-2xl border border-[#dbc2b0]/35 bg-gradient-to-br from-[#faf9f5] via-[#f8f4ee] to-[#f0e6dc] p-6 shadow-sm transition-[box-shadow,border-color] duration-300 hover:border-[var(--role-primary)]/30 hover:shadow-md dark:border-slate-700/60 dark:from-slate-900/90 dark:via-slate-900/85 dark:to-slate-950/90 dark:hover:border-amber-700/40 md:p-8"
        >
          <img
            src="/images/hero-academic-amber.svg"
            alt=""
            class="pointer-events-none absolute -right-6 -top-8 h-44 w-36 select-none object-cover opacity-[0.22] sm:h-52 sm:w-44 md:-right-4 md:top-0 md:h-56 md:w-48 md:opacity-[0.28] lg:h-64 lg:w-56"
            width="224"
            height="256"
            loading="lazy"
            decoding="async"
          />
          <div class="relative z-[1] max-w-3xl">
            <nav class="mb-2 flex flex-wrap items-center gap-2 text-xs font-medium text-slate-500 dark:text-slate-400">
              <RouterLink
                to="/teacher/dashboard"
                class="inline-flex items-center gap-1 rounded-lg px-1.5 py-0.5 transition-colors hover:bg-white/70 hover:text-[var(--role-primary)] dark:hover:bg-slate-800/80"
              >
                <span class="material-symbols-outlined text-[16px] text-slate-400">home</span>
                Trang chủ
              </RouterLink>
              <span class="material-symbols-outlined text-[14px] text-slate-400">chevron_right</span>
              <span class="inline-flex items-center gap-1 font-semibold text-[var(--role-primary)]">
                <span class="material-symbols-outlined text-[16px]">inventory_2</span>
                Ngân hàng đề thi
              </span>
            </nav>
            <div class="flex flex-wrap items-end gap-3">
              <h1 class="stitch-font-headline text-3xl font-bold leading-tight tracking-tight text-amber-900 dark:text-amber-100 md:text-4xl lg:text-5xl">
                Trung tâm tạo đề thi
              </h1>
              <span
                class="material-symbols-outlined hidden text-[min(4rem,12vw)] leading-none text-[#8d4b00]/15 dark:text-amber-200/10 sm:inline"
                aria-hidden="true"
              >
                workspace_premium
              </span>
            </div>
            <p class="mt-3 flex flex-wrap items-center gap-2 text-sm text-slate-600 dark:text-slate-400">
              <span class="material-symbols-outlined text-primary text-xl">auto_stories</span>
              <span>Chọn cách soạn đề — thủ công, import thông minh hoặc tải tệp có sẵn.</span>
            </p>
          </div>
        </div>

        <div class="mb-5 animate-fade-up">
          <div class="flex flex-wrap items-center gap-x-2 gap-y-1 text-[10px] font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
            <template v-for="(step, index) in steps" :key="step">
              <div class="flex items-center gap-1.5">
                <span
                  class="size-6 shrink-0 rounded-full flex items-center justify-center text-[10px] font-bold"
                  :class="index + 1 <= currentStep ? 'bg-primary text-white' : 'bg-slate-200 text-slate-500 dark:bg-slate-800 dark:text-slate-400'"
                >
                  {{ index + 1 }}
                </span>
                <span :class="index + 1 === currentStep ? 'text-slate-900 dark:text-white' : ''">{{ step }}</span>
              </div>
              <span v-if="index < steps.length - 1" class="h-px w-3 bg-slate-200 dark:bg-slate-700"></span>
            </template>
          </div>
        </div>

        <section class="mb-10 animate-fade-up-delay">
          <div class="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-4">
            <!-- Soạn thủ công → /teacher/exams/manual -->
            <button
              type="button"
              class="stitch-bento-card stitch-creation-tile group relative flex cursor-pointer flex-col rounded-xl border border-transparent p-6 text-left transition-all duration-300 hover:border-amber-200/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--role-primary)]/45 focus-visible:ring-offset-2 focus-visible:ring-offset-[#faf9f5] dark:focus-visible:ring-amber-400/50 dark:focus-visible:ring-offset-slate-950 md:p-8 lg:translate-y-0"
              @click="goToManual"
            >
              <span
                class="pointer-events-none absolute right-3 top-3 text-4xl text-primary/0 transition-all duration-300 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 group-hover:text-primary/10 dark:group-hover:text-amber-300/15"
                aria-hidden="true"
              >
                <span class="material-symbols-outlined">draw</span>
              </span>
              <div class="mb-6 flex items-start justify-between">
                <div
                  class="flex size-14 shrink-0 items-center justify-center rounded-full bg-amber-50 text-primary shadow-sm transition-all duration-300 group-hover:scale-110 group-hover:shadow-md dark:bg-amber-950/40"
                >
                  <span class="material-symbols-outlined text-3xl">edit_note</span>
                </div>
                <span class="material-symbols-outlined text-amber-200 transition-colors group-hover:text-primary dark:text-amber-700/80">arrow_outward</span>
              </div>
              <div class="mt-auto">
                <h3 class="stitch-font-headline mb-3 text-2xl font-bold text-amber-900 dark:text-amber-100">Soạn thủ công</h3>
                <p class="mb-4 text-xs leading-snug text-[var(--role-on-surface-variant)] dark:text-slate-400 md:text-sm">
                  Soạn từng câu, công thức &amp; hình minh họa.
                </p>
                <div class="h-1 w-0 bg-primary transition-all duration-500 group-hover:w-full" />
              </div>
            </button>

            <!-- Smart Import → luồng tạo đề (import/preview) -->
            <button
              type="button"
              class="stitch-bento-card stitch-creation-tile stitch-smart-import-tile group relative flex cursor-pointer flex-col rounded-xl border border-transparent p-6 text-left transition-all duration-300 hover:border-amber-200/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--role-primary)]/45 focus-visible:ring-offset-2 focus-visible:ring-offset-[#faf9f5] dark:focus-visible:ring-amber-400/50 dark:focus-visible:ring-offset-slate-950 md:p-8 lg:translate-y-6"
              @click="goToCreateExam"
            >
              <div class="mb-6 flex items-start justify-between">
                <div
                  class="smart-import-logo flex size-14 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-[#b15f00] to-[#6e3900] text-white shadow-md ring-1 ring-black/5 transition-all duration-300 ease-out group-hover:scale-110 group-hover:from-[#964B00] group-hover:to-[#964B00] group-hover:shadow-lg group-hover:ring-[#964B00]/30 dark:from-amber-700 dark:to-amber-950 dark:group-hover:from-[#964B00] dark:group-hover:to-[#964B00]"
                  aria-hidden="true"
                >
                  <span
                    class="material-symbols-outlined text-[1.65rem] text-white transition-transform duration-300 group-hover:scale-105"
                    style="font-variation-settings: 'FILL' 1, 'wght' 500, 'GRAD' 0, 'opsz' 24"
                  >
                    auto_awesome
                  </span>
                </div>
                <span
                  class="rounded bg-amber-100 px-2 py-1 text-[10px] font-bold uppercase tracking-widest text-[#3d2817] transition-colors group-hover:bg-[#fef3c7] group-hover:text-[#2d1a10] dark:bg-amber-900/50 dark:text-amber-200 dark:group-hover:bg-amber-800/70"
                  >Beta</span>
              </div>
              <div class="mt-auto">
                <h3 class="stitch-font-headline mb-3 text-2xl font-bold text-amber-900 dark:text-amber-100">Smart Import</h3>
                <p class="mb-4 text-xs leading-snug text-[var(--role-on-surface-variant)] dark:text-slate-400 md:text-sm">
                  Preview, sửa parse, xuất bản (CSV/XLSX/PDF/Word).
                </p>
                <div
                  class="h-1 w-0 rounded-full bg-[#2d1a10] transition-all duration-500 group-hover:w-full"
                />
              </div>
            </button>

            <!-- Import tệp — cùng luồng tạo đề + tải mẫu -->
            <button
              type="button"
              class="stitch-bento-card stitch-creation-tile group relative flex cursor-pointer flex-col rounded-xl border border-transparent p-6 text-left transition-all duration-300 hover:border-amber-200/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--role-primary)]/45 focus-visible:ring-offset-2 focus-visible:ring-offset-[#faf9f5] dark:focus-visible:ring-amber-400/50 dark:focus-visible:ring-offset-slate-950 md:p-8 lg:translate-y-0"
              @click="goToCreateExam"
            >
              <span
                class="pointer-events-none absolute bottom-16 right-4 text-5xl text-primary/0 transition-all duration-300 group-hover:text-primary/[0.07] dark:group-hover:text-amber-200/10"
                aria-hidden="true"
              >
                <span class="material-symbols-outlined">description</span>
              </span>
              <div class="mb-6 flex items-start justify-between">
                <div
                  class="flex size-14 shrink-0 items-center justify-center rounded-full bg-amber-50 text-primary shadow-sm transition-all duration-300 group-hover:scale-110 group-hover:shadow-md dark:bg-amber-950/40"
                >
                  <span class="material-symbols-outlined text-3xl">upload_file</span>
                </div>
                <span class="material-symbols-outlined text-amber-200 transition-colors group-hover:text-primary dark:text-amber-700/80">arrow_outward</span>
              </div>
              <div class="mt-auto">
                <h3 class="stitch-font-headline mb-3 text-2xl font-bold text-amber-900 dark:text-amber-100">Import tệp</h3>
                <p class="mb-4 text-xs leading-snug text-[var(--role-on-surface-variant)] dark:text-slate-400 md:text-sm">
                  Word/PDF/Excel → đề (≤10MB; nên dùng mẫu).
                </p>
                <div class="flex flex-wrap items-center gap-2">
                  <a
                    :href="getTemplateDownloadUrl('xlsx')"
                    download
                    class="inline-flex items-center gap-1.5 rounded-full border border-primary/25 bg-primary/5 px-3 py-1.5 text-xs font-bold text-primary transition-all hover:border-primary/50 hover:bg-primary/12 hover:shadow-sm dark:border-amber-500/30 dark:bg-amber-950/40 dark:hover:bg-amber-900/50"
                    @click.stop
                  >
                    <span class="material-symbols-outlined text-[18px]">download</span>
                    Tải mẫu Excel
                  </a>
                </div>
                <div class="mt-4 h-1 w-0 bg-primary transition-all duration-500 group-hover:w-full" />
              </div>
            </button>

            <!-- Danh sách đề đã lưu -->
            <button
              type="button"
              class="stitch-bento-card stitch-creation-tile group relative flex cursor-pointer flex-col rounded-xl border border-transparent p-6 text-left transition-all duration-300 hover:border-amber-200/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--role-primary)]/45 focus-visible:ring-offset-2 focus-visible:ring-offset-[#faf9f5] dark:focus-visible:ring-amber-400/50 dark:focus-visible:ring-offset-slate-950 md:p-8 lg:translate-y-6"
              @click="goToExamList"
            >
              <span
                class="pointer-events-none absolute right-2 bottom-24 text-4xl text-emerald-600/0 transition-all duration-300 group-hover:text-emerald-600/10 dark:group-hover:text-emerald-400/15"
                aria-hidden="true"
              >
                <span class="material-symbols-outlined">menu_book</span>
              </span>
              <div class="mb-6 flex items-start justify-between">
                <div
                  class="flex size-14 shrink-0 items-center justify-center rounded-full bg-amber-50 text-primary shadow-sm transition-all duration-300 group-hover:scale-110 group-hover:shadow-md dark:bg-amber-950/40"
                >
                  <span class="material-symbols-outlined text-3xl">local_library</span>
                </div>
                <span class="material-symbols-outlined text-amber-200 transition-colors group-hover:text-primary dark:text-amber-700/80">arrow_outward</span>
              </div>
              <div class="mt-auto">
                <h3 class="stitch-font-headline mb-3 text-2xl font-bold text-amber-900 dark:text-amber-100">Danh sách đề</h3>
                <p class="mb-4 text-xs leading-snug text-[var(--role-on-surface-variant)] dark:text-slate-400 md:text-sm">
                  Đề đã lưu: lọc, lịch, giám sát.
                </p>
                <div class="h-1 w-0 bg-primary transition-all duration-500 group-hover:w-full" />
              </div>
            </button>
          </div>
        </section>
      </main>

    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { getTemplateDownloadUrl } from '../../services/questionService'
import TeacherTopHeader from './TeacherTopHeader.vue'

const router = useRouter()
const isDark = ref(false)
const steps = ['Tạo', 'Nội dung', 'Lịch', 'Xong']
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

