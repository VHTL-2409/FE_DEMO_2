<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="portal-viewport flex h-full min-h-0 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100"
  >
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
        <StudentTopHeader active-section="examJoin" class="shrink-0" />

        <main
          class="teacher-page-shell student-stitch-main relative flex min-h-0 w-full max-w-screen-2xl flex-1 flex-col overflow-hidden"
        >
          <div
            class="portal-scrollbar student-stitch-paper-bg relative flex min-h-0 flex-1 flex-col overflow-y-auto px-4 pb-10 pt-4 sm:px-8 lg:px-10"
          >
            <nav class="mb-3 flex items-center gap-2 text-xs font-medium text-slate-500">
              <RouterLink to="/student/dashboard" class="transition hover:text-primary">Trang chủ</RouterLink>
              <span class="material-symbols-outlined text-[14px] text-slate-400">chevron_right</span>
              <span class="font-semibold text-primary">Thi qua mã</span>
            </nav>
            <div class="stitch-topbar-gradient-line mb-4 max-w-[1000px] rounded-t-sm" aria-hidden="true" />

            <div
              class="mx-auto flex w-full max-w-[1000px] flex-col overflow-hidden rounded-xl border border-[#dbc2b0]/20 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-900/40 md:min-h-[min(560px,85vh)] md:flex-row"
            >
              <!-- stitch_new/join_exam_room — cột trái brand -->
              <div
                class="student-stitch-academic-gradient relative hidden min-h-[200px] flex-col justify-between p-8 text-white md:flex md:w-[42%] md:max-w-none md:p-10"
              >
                <div class="relative z-10">
                  <div
                    class="mb-6 flex size-14 items-center justify-center rounded-2xl bg-white/15 ring-1 ring-white/25"
                  >
                    <span class="material-symbols-outlined text-3xl text-white" style="font-variation-settings: 'FILL' 1">school</span>
                  </div>
                  <h2 class="stitch-font-headline text-2xl font-bold leading-tight md:text-3xl">Phòng thi trực tuyến</h2>
                  <p class="mt-2 text-xs font-semibold uppercase tracking-[0.2em] text-white/80">Academic Amber</p>
                </div>
                <p class="relative z-10 text-sm leading-relaxed text-white/75">
                  Nhập mã phòng do giáo viên cung cấp để vào phòng chờ trước khi làm bài.
                </p>
                <div
                  class="pointer-events-none absolute inset-0 opacity-[0.12]"
                  aria-hidden="true"
                  style="
                    background-image: radial-gradient(circle at 20% 80%, #fff 0%, transparent 45%),
                      radial-gradient(circle at 80% 20%, #fff 0%, transparent 40%);
                  "
                />
              </div>

              <!-- Form — stitch_new/join_exam_room -->
              <div class="flex flex-1 flex-col justify-center p-6 sm:p-8 md:w-[58%] md:py-10 lg:px-12">
                <header class="mb-8 text-center md:text-left">
                  <div
                    class="mb-3 inline-flex rounded-full bg-[#e9e8e4] px-3 py-1 dark:bg-slate-800"
                  >
                    <span class="text-[10px] font-bold uppercase tracking-[0.2em] text-primary">Phòng thi an toàn</span>
                  </div>
                  <h1 class="stitch-font-headline text-3xl font-bold tracking-tight text-amber-900 dark:text-amber-100 md:text-4xl">
                    Vào phòng
                  </h1>
                  <p class="mt-2 text-sm text-[var(--stitch-on-surface-variant)] dark:text-slate-400">
                    Nhập mã hoặc tiêu đề bài thi để tìm phòng chờ.
                  </p>
                </header>

                <div class="space-y-6">
                  <div>
                    <label
                      class="mb-2 block px-1 text-xs font-bold uppercase tracking-widest text-[var(--stitch-on-surface-variant)]"
                      for="exam-code-input"
                    >
                      Mã / tiêu đề bài thi
                    </label>
                    <input
                      id="exam-code-input"
                      v-model="examCode"
                      type="text"
                      autocomplete="off"
                      maxlength="120"
                      placeholder="Ví dụ: ABCD1234 hoặc tên bài…"
                      class="w-full rounded-t-lg border-0 border-b-2 border-[#dbc2b0]/50 bg-[#e9e8e4] px-4 py-3.5 font-serif text-base text-[#191c1e] placeholder:italic placeholder:text-slate-500 focus:border-primary focus:outline-none focus:ring-0 dark:bg-slate-800/90 dark:text-slate-100"
                      @keyup.enter="goToWaitingRoom"
                    />
                  </div>

                  <button
                    type="button"
                    class="silk-press-gradient flex w-full items-center justify-center gap-2 rounded-xl py-4 text-sm font-bold uppercase tracking-[0.12em] text-white shadow-lg shadow-primary/20 transition hover:opacity-95 disabled:cursor-not-allowed disabled:opacity-60"
                    :disabled="isJoining"
                    @click="goToWaitingRoom"
                  >
                    <span>{{ isJoining ? 'Đang tìm…' : 'Vào phòng' }}</span>
                    <span class="material-symbols-outlined text-lg">login</span>
                  </button>
                </div>

                <footer class="mt-10 border-t border-[#e9e8e4] pt-6 dark:border-slate-700">
                  <div class="flex gap-3 text-slate-500 dark:text-slate-400">
                    <span class="material-symbols-outlined shrink-0 text-amber-800 dark:text-amber-400">verified_user</span>
                    <div>
                      <p class="text-xs font-semibold uppercase tracking-wider text-slate-700 dark:text-slate-300">
                        Xác minh học sinh
                      </p>
                      <p class="mt-1 text-[11px] leading-relaxed">
                        Phiên làm bài có thể được giám sát theo cấu hình đề. Tuân thủ nội quy phòng thi.
                      </p>
                    </div>
                  </div>
                </footer>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { joinExamByCode } from '../../services/examService'
import { useToast } from '../../composables/useToast'
import StudentTopHeader from './StudentTopHeader.vue'

const router = useRouter()
const route = useRoute()
const isDark = ref(false)
const examCode = ref('')

const applyPrefillFromRoute = () => {
  const q = route.query.prefill ?? route.query.q ?? route.query.code
  if (typeof q === 'string' && q.trim()) {
    examCode.value = q.trim()
  }
}

onMounted(applyPrefillFromRoute)
watch(() => route.query, applyPrefillFromRoute, { deep: true })
const isJoining = ref(false)
const toast = useToast()

const resolveExamByInput = async () => {
  const query = examCode.value.trim()
  if (!query) {
    return null
  }

  return joinExamByCode(query)
}

const goToWaitingRoom = async () => {
  isJoining.value = true

  try {
    const matchedExam = await resolveExamByInput()
    if (!matchedExam) {
      toast.error('Vui lòng nhập mã hoặc tiêu đề để tìm bài thi.')
      return
    }

    router.push({
      path: '/student/exam-waiting-room',
      query: {
        examId: matchedExam.id,
        examCode: matchedExam.code || '',
        exam: matchedExam.title || 'Bài thi',
        duration: matchedExam.durationMinutes || 60,
        questions: matchedExam.questionCount || 0,
        startAt: matchedExam.startTime || '',
        endAt: matchedExam.endTime || '',
        requireCameraMic: matchedExam.requireCameraMic === false ? 'false' : 'true'
      }
    })
  } catch (error) {
    toast.error('Không thể vào bài thi lúc này.')
  } finally {
    isJoining.value = false
  }
}
</script>
