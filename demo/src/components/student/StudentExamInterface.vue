<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="portal-viewport flex h-full min-h-0 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100"
    @contextmenu="handleRightClick"
  >
    <div class="shrink-0">
    <StudentTopHeader :show-profile="false" :show-sign-out="false" :show-notifications="false" :show-menu="false">
      <template #rightActions>
        <div class="hidden xl:flex items-center gap-3 mr-2 text-xs font-medium">
          <span :class="cameraReady ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-500'" class="material-symbols-outlined text-sm leading-none">videocam</span>
          <span :class="cameraReady ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-500'">{{ cameraReady ? 'Camera bật' : 'Camera tắt' }}</span>
          <span :class="micReady ? 'bg-emerald-400' : 'bg-rose-500'" class="w-1 h-1 rounded-full"></span>
          <span :class="micReady ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-500'" class="material-symbols-outlined text-sm leading-none">mic</span>
          <span :class="micReady ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-500'">{{ micReady ? 'Mic bật' : 'Mic tắt' }}</span>
          <span class="w-1 h-1 rounded-full bg-slate-300 dark:bg-slate-700"></span>
          <span class="material-symbols-outlined text-sm leading-none text-slate-500">wifi</span>
          <span class="text-slate-500">Đã kết nối</span>
        </div>
        <div
          v-if="initialRemainingForProgress > 0"
          class="hidden sm:flex flex-col gap-1 min-w-[120px] max-w-[180px]"
          role="group"
          aria-label="Thời gian còn lại"
        >
          <progress
            class="exam-timer-progress w-full h-2 rounded-full overflow-hidden accent-[color:var(--color-primary)]"
            :class="timerBarAccentClass"
            :value="remainingSeconds"
            :max="initialRemainingForProgress || 1"
            :aria-valuenow="remainingSeconds"
            aria-valuemin="0"
            :aria-valuemax="initialRemainingForProgress || 1"
            :aria-label="timerAriaLabel"
          />
        </div>
        <div class="staff-surface flex items-center gap-3 rounded-xl px-4 py-2.5">
          <div class="flex flex-col items-center leading-none">
            <span class="text-sm font-bold tabular-nums">{{ timerHours }}</span>
            <span class="text-[9px] uppercase tracking-wider opacity-60">Giờ</span>
          </div>
          <span class="text-base font-light opacity-30">:</span>
          <div class="flex flex-col items-center leading-none">
            <span class="text-sm font-bold tabular-nums">{{ timerMinutes }}</span>
            <span class="text-[9px] uppercase tracking-wider opacity-60">Phút</span>
          </div>
          <span class="text-base font-light opacity-30">:</span>
          <div class="flex flex-col items-center leading-none">
            <span class="text-sm font-bold tabular-nums">{{ timerSeconds }}</span>
            <span class="text-[9px] uppercase tracking-wider opacity-60">Giây</span>
          </div>
        </div>
        <div
          v-if="saveStatusLabel"
          class="hidden md:flex items-center gap-1.5 text-[10px] font-semibold text-slate-600 dark:text-slate-300 max-w-[180px]"
          role="status"
          aria-live="polite"
        >
          <span v-if="saveStatus === 'saving'" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
          <span v-else-if="saveStatus === 'saved'" class="material-symbols-outlined text-sm text-emerald-600">check_circle</span>
          <span v-else-if="hasPendingChanges" class="material-symbols-outlined text-sm text-amber-600">schedule</span>
          <span v-else-if="saveStatus === 'error'" class="material-symbols-outlined text-sm text-amber-600">cloud_off</span>
          <span class="truncate">{{ saveStatusLabel }}</span>
        </div>
        <BaseButton
          type="button"
          size="sm"
          variant="ghost"
          class="hidden md:inline-flex text-xs"
          :disabled="isSuspended || saveStatus === 'saving'"
          @click="manualSaveDraft"
        >
          Lưu ngay
        </BaseButton>
        <BaseButton
          type="button"
          size="sm"
          class="text-xs"
          :disabled="isSuspended"
          @click="openSubmitModal"
        >
          Nộp bài
        </BaseButton>
      </template>
    </StudentTopHeader>
    </div>

    <main class="relative flex w-full min-h-0 flex-1 flex-col gap-3 overflow-hidden p-3 sm:p-4 md:gap-4 lg:flex-row lg:gap-4">
      <div
        v-if="showFullscreenPrompt && !isPracticeExam && !isSuspended"
        class="absolute inset-x-4 top-0 z-30 rounded-2xl border border-amber-200 bg-amber-50/95 px-4 py-3 shadow-lg backdrop-blur dark:border-amber-800 dark:bg-amber-900/70 sm:inset-x-6"
      >
        <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p class="text-sm font-bold text-amber-800 dark:text-amber-200">Yêu cầu toàn màn hình đang tắt</p>
            <p class="text-xs text-amber-700 dark:text-amber-300">Bấm vào để tiếp tục làm bài trong chế độ toàn màn hình.</p>
          </div>
          <BaseButton type="button" size="sm" @click="requestExamFullscreen">Vào toàn màn hình</BaseButton>
        </div>
      </div>
      <div v-if="isSuspended" class="absolute inset-0 z-20 bg-black/60 backdrop-blur-[1px] flex items-center justify-center px-4">
        <div class="max-w-lg w-full rounded-2xl border border-rose-300 bg-white p-6 text-center shadow-2xl">
          <h2 class="text-2xl font-bold text-rose-700 mb-2">{{ attemptStatus === 'PAUSED' ? 'Phiên thi đang tạm dừng' : 'Bài thi đã bị đình chỉ' }}</h2>
          <p class="text-sm text-slate-600">{{ suspensionMessage || 'Giám thị đã hủy hiệu lực bài thi của bạn.' }}</p>
        </div>
      </div>

      <!-- Modal cảnh báo từ giám thị -->
      <div v-if="showTeacherWarningModal" class="fixed inset-0 z-[70] flex items-center justify-center p-4 bg-amber-900/40 backdrop-blur-sm" @click.self="showTeacherWarningModal = false">
        <div class="max-w-lg w-full rounded-2xl border-2 border-amber-400 bg-white dark:bg-slate-900 p-6 shadow-2xl animate-fade-up ring-4 ring-amber-500/30">
          <div class="flex items-center gap-4 mb-4">
            <div class="size-14 rounded-2xl bg-amber-100 dark:bg-amber-900/40 flex items-center justify-center shrink-0">
              <span class="material-symbols-outlined text-amber-600 dark:text-amber-400 text-3xl">warning</span>
            </div>
            <div>
              <h2 class="text-xl font-bold text-amber-800 dark:text-amber-200">Cảnh báo từ giám thị</h2>
              <p class="text-sm text-slate-600 dark:text-slate-400 mt-1">Vui lòng chú ý và tuân thủ quy định phòng thi.</p>
            </div>
          </div>
          <div class="rounded-xl bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 p-4 mb-6">
            <p class="text-slate-800 dark:text-slate-200 font-medium">{{ teacherWarningMessage }}</p>
          </div>
          <button type="button" class="w-full py-3 rounded-xl bg-amber-500 hover:bg-amber-600 text-white font-bold flex items-center justify-center gap-2" @click="showTeacherWarningModal = false">
            <span class="material-symbols-outlined">check_circle</span>
            Tôi đã hiểu
          </button>
        </div>
      </div>
      <div class="relative flex min-h-0 min-w-0 flex-1 flex-col gap-3">

        <div
          v-if="!examSurfaceReady"
          class="staff-surface-strong flex min-h-[min(50dvh,20rem)] flex-1 flex-col items-center justify-center overflow-hidden rounded-[1.25rem] px-4 py-10 dark:bg-slate-900"
        >
          <span class="material-symbols-outlined mb-3 animate-spin text-4xl text-primary">progress_activity</span>
          <p class="text-sm font-semibold text-slate-700 dark:text-slate-200">Đang tải đề thi…</p>
          <p class="mt-1 max-w-xs text-center text-xs text-slate-500 dark:text-slate-400">Vui lòng đợi trong giây lát.</p>
        </div>

        <div
          v-else-if="examSurfaceReady && examLoadFailed"
          class="staff-surface-strong flex min-h-[min(40dvh,16rem)] flex-1 flex-col items-center justify-center overflow-hidden rounded-[1.25rem] px-4 py-8 text-center dark:bg-slate-900"
        >
          <span class="material-symbols-outlined mb-2 text-4xl text-rose-300 dark:text-rose-700">error</span>
          <p class="text-sm font-bold text-slate-800 dark:text-slate-100">Không tải được đề thi</p>
          <p class="mt-1 max-w-sm text-xs text-slate-500 dark:text-slate-400">Vui lòng làm mới trang hoặc quay lại sau.</p>
        </div>

        <div
          v-else-if="examSurfaceReady && !questions.length"
          class="staff-surface-strong flex min-h-[min(45dvh,18rem)] flex-1 flex-col items-center justify-center overflow-hidden rounded-[1.25rem] px-4 py-8 text-center dark:bg-slate-900"
        >
          <span class="material-symbols-outlined mb-2 text-4xl text-slate-300 dark:text-slate-600">quiz</span>
          <p class="text-sm font-bold text-slate-800 dark:text-slate-100">Không có câu hỏi</p>
          <p class="mt-1 max-w-sm text-xs text-slate-500 dark:text-slate-400">Đề thi chưa có nội dung. Hãy thoát và liên hệ giáo viên.</p>
        </div>

        <div
          v-else-if="currentQuestion"
          class="staff-surface-strong flex min-h-0 flex-1 flex-col overflow-hidden rounded-[1.25rem] dark:bg-slate-900"
        >
          <div class="portal-scrollbar flex min-h-0 flex-1 flex-col overflow-y-auto p-4 sm:p-6 md:p-7">
            <div class="mb-6 flex justify-between items-center shrink-0">
              <span class="inline-flex items-center gap-2 rounded-xl bg-primary/10 px-4 py-2 text-sm font-bold text-primary">
                <span class="material-symbols-outlined text-base">quiz</span>
                Câu {{ currentIndex + 1 }} / {{ questions.length }}
              </span>
              <button
                type="button"
                class="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-600 transition hover:border-amber-300 hover:text-amber-600 dark:border-slate-700 dark:text-slate-300 dark:hover:border-amber-600 dark:hover:text-amber-300"
                :disabled="isSuspended"
                @click="toggleMarkCurrentQuestion"
              >
                <span class="material-symbols-outlined text-base">{{ markedQuestions[String(currentQuestion.id)] ? 'bookmark_added' : 'bookmark_add' }}</span>
                {{ markedQuestions[String(currentQuestion.id)] ? 'Bỏ đánh dấu' : 'Đánh dấu xem lại' }}
              </button>
            </div>
            <div class="prose dark:prose-invert mb-6 max-w-none">
              <h2 class="text-xl font-semibold leading-relaxed text-slate-900 dark:text-slate-100 sm:text-2xl">
                {{ currentQuestion.content }}
              </h2>
            </div>

            <QuestionRenderer :question="currentQuestion" v-model="currentQuestionAnswer" :disabled="isSuspended" />
          </div>

          <div class="shrink-0 border-t border-slate-200 bg-slate-50/80 px-4 py-3 dark:border-slate-800 dark:bg-slate-900/50 sm:px-6 sm:py-4">
            <div class="flex flex-col-reverse items-stretch justify-between gap-3 sm:flex-row sm:items-center">
              <button @click="goPrevious" :disabled="currentIndex === 0" class="flex items-center justify-center gap-2 rounded-xl border-2 border-slate-200 px-5 py-3 font-bold text-slate-700 transition-all duration-200 hover:border-slate-300 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent dark:border-slate-700 dark:text-slate-300 dark:hover:border-slate-600 dark:hover:bg-slate-800" type="button">
                <span class="material-symbols-outlined text-xl">arrow_back</span>
                Câu trước
              </button>
              <div class="flex flex-col gap-2 sm:flex-row">
                <button
                  type="button"
                  class="flex items-center justify-center gap-2 rounded-xl border border-slate-200 px-4 py-3 text-sm font-semibold text-slate-600 transition hover:border-amber-300 hover:text-amber-600 dark:border-slate-700 dark:text-slate-300 dark:hover:border-amber-600 dark:hover:text-amber-300"
                  :disabled="isSuspended"
                  @click="toggleMarkCurrentQuestion"
                >
                  <span class="material-symbols-outlined text-lg">bookmark</span>
                  {{ markedQuestions[String(currentQuestion.id)] ? 'Đã đánh dấu' : 'Đánh dấu' }}
                </button>
                <button @click="goNext" :disabled="currentIndex >= questions.length - 1" class="flex flex-1 items-center justify-center gap-2 rounded-xl bg-primary px-6 py-3 font-bold text-white shadow-lg shadow-primary/25 transition-all duration-200 hover:bg-primary/90 hover:shadow-primary/30 disabled:cursor-not-allowed disabled:opacity-40 disabled:shadow-none sm:flex-initial" type="button">
                Câu tiếp theo
                <span class="material-symbols-outlined text-xl">arrow_forward</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <aside
        class="relative flex w-full shrink-0 flex-col gap-3 lg:w-[min(100%,20rem)] lg:max-h-full lg:min-h-0"
        :class="{ 'opacity-90': !examSurfaceReady }"
      >
        <div v-if="shouldCheckDevices && mediaStreamRef" class="staff-surface overflow-hidden rounded-[1.25rem] dark:bg-slate-900">
          <div class="px-3 py-2 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between gap-2">
            <span class="text-xs font-bold text-slate-700 dark:text-slate-300">Camera của bạn</span>
            <div class="flex items-center gap-1">
              <button
                :disabled="isSuspended"
                :title="cameraReady ? 'Tắt camera' : 'Bật camera'"
                :class="cameraReady ? 'bg-primary/15 text-primary hover:bg-primary/25' : 'bg-slate-200 dark:bg-slate-700 text-slate-500 hover:bg-slate-300 dark:hover:bg-slate-600'"
                class="p-2 rounded-lg transition-all disabled:opacity-50"
                type="button"
                @click="toggleCamera"
              >
                <span class="material-symbols-outlined text-lg">{{ cameraReady ? 'videocam' : 'videocam_off' }}</span>
              </button>
              <button
                :disabled="isSuspended"
                :title="micReady ? 'Tắt micro' : 'Bật micro'"
                :class="micReady ? 'bg-primary/15 text-primary hover:bg-primary/25' : 'bg-slate-200 dark:bg-slate-700 text-slate-500 hover:bg-slate-300 dark:hover:bg-slate-600'"
                class="p-2 rounded-lg transition-all disabled:opacity-50"
                type="button"
                @click="toggleMic"
              >
                <span class="material-symbols-outlined text-lg">{{ micReady ? 'mic' : 'mic_off' }}</span>
              </button>
            </div>
          </div>
          <div class="relative aspect-video bg-slate-900">
            <video ref="cameraPreviewRef" autoplay playsinline muted class="w-full h-full object-cover" :class="{ 'opacity-0': !cameraReady }" />
            <div v-if="!cameraReady" class="absolute inset-0 flex items-center justify-center bg-slate-800/90">
              <span class="material-symbols-outlined text-4xl text-slate-500">videocam_off</span>
            </div>
          </div>
        </div>
        <div v-if="examSurfaceReady" class="staff-surface rounded-[1.25rem] p-4 dark:bg-slate-900 sm:p-5">
          <div class="flex justify-between items-center mb-3">
            <h3 class="font-bold text-sm text-slate-800 dark:text-slate-200 flex items-center gap-2">
              <span class="material-symbols-outlined text-primary text-lg">trending_up</span>
              Tiến độ làm bài
            </h3>
            <span class="text-lg font-bold text-primary tabular-nums">{{ progressPercent }}%</span>
          </div>
          <div class="w-full bg-slate-100 dark:bg-slate-800 h-2.5 rounded-full overflow-hidden">
            <div class="bg-primary h-full rounded-full transition-all duration-500" :style="{ width: `${progressPercent}%` }"></div>
          </div>
          <div class="mt-3 flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
            <span class="flex items-center gap-1.5"><span class="w-2 h-2 rounded-full bg-primary"></span> Đã làm: {{ answeredCount }}</span>
            <span class="flex items-center gap-1.5"><span class="w-2 h-2 rounded-full bg-slate-300 dark:bg-slate-600"></span> Chưa làm: {{ unansweredCount }}</span>
          </div>
          <div class="mt-2 flex flex-wrap gap-2 text-[11px] text-slate-500 dark:text-slate-400">
            <span class="inline-flex items-center gap-1.5 rounded-full bg-amber-100 px-2.5 py-1 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300">Đánh dấu: {{ markedCount }}</span>
            <span class="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-2.5 py-1 text-slate-600 dark:bg-slate-800 dark:text-slate-300">Bỏ qua: {{ skippedCount }}</span>
          </div>
        </div>

        <div
          v-if="examSurfaceReady"
          class="portal-scrollbar flex min-h-0 max-h-[min(52dvh,28rem)] flex-1 overflow-y-auto rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900 sm:p-5 lg:max-h-none"
        >
          <div class="flex w-full min-w-0 flex-col">
          <div class="flex items-center justify-between mb-3">
            <h3 class="font-bold text-sm text-slate-800 dark:text-slate-200 flex items-center gap-2">
              <span class="material-symbols-outlined text-primary text-lg">list</span>
              Danh sách câu hỏi
            </h3>
            <span class="text-xs text-slate-500 font-medium">{{ questions.length }} câu</span>
          </div>
          <div class="grid grid-cols-5 gap-2 pb-2 overscroll-contain">
            <button
              v-for="(question, idx) in questions"
              :key="question.id"
              :class="questionButtonClass(idx)"
              class="aspect-square flex items-center justify-center rounded-xl text-xs font-bold transition-all duration-200 hover:scale-105"
              type="button"
              @click="selectQuestion(idx)"
            >
              {{ idx + 1 }}
            </button>
          </div>
          <div class="mt-3 grid grid-cols-2 gap-2 text-[11px] text-slate-500 dark:text-slate-400">
            <span class="inline-flex items-center gap-1.5"><span class="h-2 w-2 rounded-full bg-primary"></span> Đã làm</span>
            <span class="inline-flex items-center gap-1.5"><span class="h-2 w-2 rounded-full bg-amber-400"></span> Đánh dấu</span>
            <span class="inline-flex items-center gap-1.5"><span class="h-2 w-2 rounded-full bg-slate-300 dark:bg-slate-600"></span> Bỏ qua</span>
            <span class="inline-flex items-center gap-1.5"><span class="h-2 w-2 rounded-full bg-transparent border border-slate-400"></span> Chưa mở</span>
          </div>
          </div>
        </div>
      </aside>
    </main>

    <footer class="shrink-0 border-t border-slate-200/80 bg-slate-50/60 px-3 py-1.5 text-center text-[11px] text-slate-500 dark:border-slate-800 dark:bg-slate-900/30 dark:text-slate-400 sm:px-4">
      Hỗ trợ:
      <span class="cursor-pointer font-semibold text-primary hover:underline">support@edu-portal.com</span>
    </footer>

    <BaseModal v-model="showSubmitModal" title="Xác nhận nộp bài" :persistent="true">
      <p class="text-sm text-slate-600 dark:text-slate-400 mb-4">
        Đã trả lời <strong>{{ answeredCount }}</strong> / {{ questions.length }} câu.
        <span v-if="unansweredCount > 0" class="text-[color:var(--color-warning)] font-semibold">
          Còn {{ unansweredCount }} câu chưa trả lời.
        </span>
      </p>
      <div class="mb-4 grid grid-cols-2 gap-3 text-xs">
        <div class="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 dark:border-slate-700 dark:bg-slate-800/50">
          <p class="text-slate-500 dark:text-slate-400">Đánh dấu xem lại</p>
          <p class="mt-1 text-lg font-bold text-amber-600 dark:text-amber-300">{{ markedCount }}</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 dark:border-slate-700 dark:bg-slate-800/50">
          <p class="text-slate-500 dark:text-slate-400">Đã mở nhưng bỏ qua</p>
          <p class="mt-1 text-lg font-bold text-slate-700 dark:text-slate-100">{{ skippedCount }}</p>
        </div>
      </div>
      <p class="text-sm text-slate-500 dark:text-slate-400 mb-4">Bạn sẽ không thể thay đổi đáp án sau khi nộp.</p>
      <div v-if="unansweredCount > 0" class="rounded-xl bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700 p-4">
        <p class="text-sm font-semibold text-amber-800 dark:text-amber-200 flex items-center gap-2">
          <span class="material-symbols-outlined text-lg" aria-hidden="true">warning</span>
          Bạn còn {{ unansweredCount }} câu chưa làm và {{ notVisitedCount }} câu chưa mở.
        </p>
      </div>
      <template #footer>
        <div class="flex flex-col-reverse sm:flex-row gap-2 sm:justify-end w-full">
          <BaseButton variant="ghost" type="button" @click="closeSubmitModal">Tiếp tục làm bài</BaseButton>
          <BaseButton id="submit-exam-confirm" :loading="isSubmitting" type="button" @click="submitExamAction">
            Nộp bài
          </BaseButton>
        </div>
      </template>
    </BaseModal>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { getAttemptDetail, getDraftAnswers, saveDraftAnswers, submitAttempt } from '../../services/attemptService'
import { updateDeviceStatus } from '../../services/monitoringService'
import { listExamQuestions, parseQuestionJson, parseQuestionOptions } from '../../services/questionService'
import { useToast } from '../../composables/useToast'
import { useAutoSaveDraft } from '../../composables/useAutoSaveDraft'
import { useProctoringSession } from '../../composables/useProctoringSession'
import { useRealtimeChannel } from '../../composables/useRealtimeChannel'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { useExamSessionStore } from '../../stores/examSessionStore'
import StudentTopHeader from './StudentTopHeader.vue'
import BaseModal from '../shared/BaseModal.vue'
import BaseButton from '../shared/BaseButton.vue'
import QuestionRenderer from './questions/QuestionRenderer.vue'

const route = useRoute()
const router = useRouter()
const isDark = ref(false)

const examTitle = computed(() => route.query.exam || 'Giữa kỳ Tâm lý học nâng cao')
const examId = computed(() => Number.parseInt(String(route.query.examId || ''), 10) || null)
const attemptId = computed(() => Number.parseInt(String(route.query.attemptId || ''), 10) || null)
const isPracticeExam = computed(() => String(route.query.isPractice || '') === 'true')
const examConfig = ref({
  monitorTabSwitch: true,
  monitorBlur: true,
  monitorExitFullscreen: true,
  monitorCopyPaste: true,
  monitorIdleTime: true,
  monitorDevtools: true,
  monitorDuplicateIp: true,
  monitorFastSubmit: true,
  monitorRightClick: true,
  monitorPrintScreen: true,
  monitorRapidQuestionSwitch: true,
  monitorMultiMonitor: true,
  requireCameraMic: true
})
const showSubmitModal = ref(false)
const isSubmitting = ref(false)
const questions = ref([])
/** Đặt true sau khi gán questions — tránh vùng trống khi đang tải */
const examSurfaceReady = ref(false)
const examLoadFailed = ref(false)
const answers = ref({})
const markedQuestions = ref({})
const visitedQuestions = ref({})
const currentIndex = ref(0)
const remainingSeconds = ref(Number.parseInt(String(route.query.remainingSeconds || ''), 10) || 0)
const initialRemainingForProgress = ref(0)

const toast = useToast()
const attemptStatus = ref('IN_PROGRESS')
const teacherWarningMessage = ref('')
const showTeacherWarningModal = ref(false)
const isSuspended = ref(false)
const suspensionMessage = ref('')
const lastViolationAtByType = ref({})
const pendingViolationByType = ref({})
const questionSwitchTimestamps = ref([])
const cameraReady = ref(false)
const micReady = ref(false)
const deviceError = ref('')
const isCheckingDevices = ref(false)
const mediaStreamRef = ref(null)
const cameraPreviewRef = ref(null)
let deviceStatusInterval = null
let timerId = null
let blurGraceTimer = null
let attemptStatusTimer = null
let idleTimer = null
let devtoolsCheckTimer = null
let blockBackHandler = null

const VIOLATION_COOLDOWN_MS = 7000
const LONG_VIOLATION_COOLDOWN_MS = 60000
const BLUR_GRACE_MS = 1200
const IDLE_THRESHOLD_MS = 3 * 60 * 1000
const DEVTOOLS_GAP_PX = 160
const RAPID_SWITCH_WINDOW_MS = 10000
const RAPID_SWITCH_THRESHOLD = 6

const examSessionStore = useExamSessionStore()
const realtimeChannel = useRealtimeChannel()
const showFullscreenPrompt = ref(false)
const isFullscreenActive = ref(false)

const normalizeQuestionType = (question) => String(question?.type || 'SINGLE_CHOICE').toUpperCase()

const hasAnswerValue = (value) => {
  if (Array.isArray(value)) return value.length > 0
  if (value && typeof value === 'object') return Object.keys(value).length > 0
  return Boolean(value)
}

const serializeAnswerValue = (question, value) => {
  const type = normalizeQuestionType(question)
  if (value == null) return ''
  if (type === 'MULTIPLE_CHOICE' || type === 'ORDERING') {
    return JSON.stringify(Array.isArray(value) ? value : [])
  }
  if (type === 'MATCHING') {
    return JSON.stringify(value && typeof value === 'object' ? value : {})
  }
  return String(value)
}

const deserializeAnswerValue = (question, value) => {
  const type = normalizeQuestionType(question)
  if (value == null) {
    return type === 'MULTIPLE_CHOICE' || type === 'ORDERING' ? [] : (type === 'MATCHING' ? {} : '')
  }
  if (type === 'MULTIPLE_CHOICE' || type === 'ORDERING') {
    return Array.isArray(value) ? value : parseQuestionJson(value, [])
  }
  if (type === 'MATCHING') {
    return (value && typeof value === 'object' && !Array.isArray(value)) ? value : parseQuestionJson(value, {})
  }
  return String(value)
}

const currentQuestion = computed(() => questions.value[currentIndex.value] || null)
const devicesReady = computed(() => cameraReady.value && micReady.value)
const shouldCheckDevices = computed(() => !isPracticeExam.value && examConfig.value.requireCameraMic !== false)
const answeredCount = computed(() => Object.values(answers.value).filter((value) => {
  return hasAnswerValue(value)
}).length)
const unansweredCount = computed(() => Math.max(questions.value.length - answeredCount.value, 0))
const markedCount = computed(() => Object.values(markedQuestions.value).filter(Boolean).length)
const skippedCount = computed(() => questions.value.filter((question) => {
  const key = String(question.id)
  return Boolean(visitedQuestions.value[key]) && !hasAnswerValue(answers.value[key]) && !Boolean(markedQuestions.value[key])
}).length)
const notVisitedCount = computed(() => questions.value.filter((question) => !visitedQuestions.value[String(question.id)]).length)
const progressPercent = computed(() => {
  if (!questions.value.length) return 0
  return Math.round((answeredCount.value / questions.value.length) * 100)
})
const timerHours = computed(() => String(Math.floor(remainingSeconds.value / 3600)).padStart(2, '0'))
const timerMinutes = computed(() => String(Math.floor((remainingSeconds.value % 3600) / 60)).padStart(2, '0'))
const timerSeconds = computed(() => String(remainingSeconds.value % 60).padStart(2, '0'))

const prefersReducedMotion = () =>
  typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

const timerBarAccentClass = computed(() => {
  const max = Math.max(initialRemainingForProgress.value, 1)
  const ratio = remainingSeconds.value / max
  if (ratio < 0.2) {
    return prefersReducedMotion() ? 'exam-timer--danger' : 'exam-timer--danger exam-timer--pulse'
  }
  if (ratio <= 0.5) return 'exam-timer--warning'
  return ''
})

const timerAriaLabel = computed(() => {
  const m = Math.floor(remainingSeconds.value / 60)
  const sec = remainingSeconds.value % 60
  return `Thời gian còn lại: ${m} phút ${sec} giây`
})

const currentQuestionAnswer = computed({
  get: () => {
    const question = currentQuestion.value
    if (!question) return ''
    const currentValue = answers.value[question.id]
    const type = normalizeQuestionType(question)
    if (type === 'MULTIPLE_CHOICE' || type === 'ORDERING') {
      return Array.isArray(currentValue) ? currentValue : []
    }
    if (type === 'MATCHING') {
      return currentValue && typeof currentValue === 'object' && !Array.isArray(currentValue) ? currentValue : {}
    }
    return currentValue ?? ''
  },
  set: (value) => {
    const question = currentQuestion.value
    if (!question) return
    onSelectAnswer(question.id, value)
  }
})


const clearBlurGraceTimer = () => {
  if (blurGraceTimer) {
    window.clearTimeout(blurGraceTimer)
    blurGraceTimer = null
  }
}

const syncDeviceStatusToBackend = async () => {
  if (!attemptId.value || isPracticeExam.value || isSuspended.value) return
  try {
    await updateDeviceStatus(attemptId.value, cameraReady.value, micReady.value)
  } catch {
    // ignore
  }
}

const toggleCamera = () => {
  const stream = mediaStreamRef.value
  const videoTrack = stream?.getVideoTracks()[0]
  if (!videoTrack) return
  videoTrack.enabled = !videoTrack.enabled
  cameraReady.value = videoTrack.enabled
  void syncDeviceStatusToBackend()
}

const toggleMic = () => {
  const stream = mediaStreamRef.value
  const audioTrack = stream?.getAudioTracks()[0]
  if (!audioTrack) return
  audioTrack.enabled = !audioTrack.enabled
  micReady.value = audioTrack.enabled
  void syncDeviceStatusToBackend()
}

const checkDevices = async () => {
  if (!shouldCheckDevices.value) {
    cameraReady.value = true
    micReady.value = true
    deviceError.value = ''
    return
  }
  if (!navigator?.mediaDevices?.getUserMedia) {
    cameraReady.value = false
    micReady.value = false
    deviceError.value = 'Trình duyệt không hỗ trợ kiểm tra camera/micro.'
    return
  }

  isCheckingDevices.value = true
  deviceError.value = ''
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
    const videoTrack = stream.getVideoTracks()[0]
    const audioTrack = stream.getAudioTracks()[0]
    cameraReady.value = Boolean(videoTrack?.enabled)
    micReady.value = Boolean(audioTrack?.enabled)
    mediaStreamRef.value = stream

    const onTrackEnded = () => {
      cameraReady.value = Boolean(stream.getVideoTracks().some((t) => t.enabled && t.readyState === 'live'))
      micReady.value = Boolean(stream.getAudioTracks().some((t) => t.enabled && t.readyState === 'live'))
      syncDeviceStatusToBackend()
    }
    videoTrack?.addEventListener('ended', onTrackEnded)
    audioTrack?.addEventListener('ended', onTrackEnded)

    await syncDeviceStatusToBackend()
    deviceStatusInterval = setInterval(syncDeviceStatusToBackend, 15000)
  } catch (error) {
    cameraReady.value = false
    micReady.value = false
    deviceError.value = error?.name === 'NotAllowedError'
      ? 'Bạn cần cấp quyền camera và micro để vào phòng thi.'
      : 'Không thể truy cập camera/micro. Vui lòng kiểm tra thiết bị.'
  } finally {
    isCheckingDevices.value = false
  }
}

const stopMediaStream = () => {
  if (deviceStatusInterval) {
    clearInterval(deviceStatusInterval)
    deviceStatusInterval = null
  }
  const stream = mediaStreamRef.value
  if (stream) {
    stream.getTracks().forEach((t) => t.stop())
    mediaStreamRef.value = null
  }
  if (cameraPreviewRef.value) {
    cameraPreviewRef.value.srcObject = null
  }
}

watch(mediaStreamRef, (stream) => {
  if (!stream) return
  nextTick(() => {
    if (cameraPreviewRef.value) {
      cameraPreviewRef.value.srcObject = stream
    }
  })
})

const syncRiskState = (payload) => {
  if (!payload || typeof payload !== 'object') return
  const nextRiskScore = typeof payload.riskScore === 'number' ? payload.riskScore : payload.score
  const nextRiskLevel = payload.riskLevel || payload.level
  if (typeof nextRiskScore === 'number') {
    examSessionStore.setRiskState({
      riskScore: nextRiskScore,
      riskLevel: nextRiskLevel || examSessionStore.riskState.riskLevel,
      status: payload.status || attemptStatus.value
    })
  }
}

const handleProctorActions = (actions = [], payload = {}) => {
  const normalized = actions.map((item) => String(item || '').toUpperCase())
  if (normalized.includes('PAUSE_ATTEMPT') || normalized.includes('ATTEMPT_PAUSED')) {
    applyAttemptStatus(payload.status || 'PAUSED', payload.message || 'Phiên thi đang được giám thị kiểm tra.')
  }
  if (normalized.includes('SHOW_WARNING') || normalized.includes('WARNING_SENT')) {
    teacherWarningMessage.value = payload.message || 'Hệ thống phát hiện rủi ro cao. Vui lòng tiếp tục làm bài đúng quy định.'
    showTeacherWarningModal.value = true
    toast.warning(teacherWarningMessage.value)
  }
}

const buildDeviceFingerprintSeed = () => JSON.stringify({
  screen: `${window.screen?.width || 0}x${window.screen?.height || 0}`,
  timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || '',
  language: navigator.language || '',
  platform: navigator.platform || '',
  userAgent: navigator.userAgent || ''
})

const getHeartbeatPayload = () => ({
  fullscreen: Boolean(document.fullscreenElement),
  visibility: document.visibilityState || 'visible',
  cameraOn: cameraReady.value,
  micOn: micReady.value,
  screenMetrics: {
    screenWidth: window.screen?.width || null,
    screenHeight: window.screen?.height || null,
    availWidth: window.screen?.availWidth || null,
    availHeight: window.screen?.availHeight || null,
    viewportWidth: window.innerWidth || null,
    viewportHeight: window.innerHeight || null
  }
})

const {
  queueEvent,
  flush: flushQueuedViolations,
  syncHeartbeat,
  startHeartbeat,
  stopHeartbeat
} = useProctoringSession({
  getAttemptId: () => attemptId.value,
  getDeviceFingerprint: buildDeviceFingerprintSeed,
  getHeartbeatPayload,
  onRiskUpdate: syncRiskState,
  onActionRequired: handleProctorActions,
  batchWindowMs: 1200,
  heartbeatIntervalMs: 15000
})

const reportViolation = async (eventType, details, cooldownMs = VIOLATION_COOLDOWN_MS) => {
  if (isPracticeExam.value || !attemptId.value || isSuspended.value) return
  if (!examConfig.value || examConfig.value.monitorFastSubmit === false && eventType === 'FAST_SUBMIT') return
  if (examConfig.value.monitorTabSwitch === false && eventType === 'TAB_SWITCH') return
  if (examConfig.value.monitorBlur === false && eventType === 'BLUR') return
  if (examConfig.value.monitorExitFullscreen === false && eventType === 'EXIT_FULLSCREEN') return
  if (examConfig.value.monitorCopyPaste === false && eventType === 'COPY_PASTE') return
  if (examConfig.value.monitorIdleTime === false && eventType === 'IDLE_TIME') return
  if (examConfig.value.monitorDevtools === false && eventType === 'DEVTOOLS_OPEN') return
  if (examConfig.value.monitorRightClick === false && eventType === 'RIGHT_CLICK') return
  if (examConfig.value.monitorPrintScreen === false && eventType === 'PRINT_SCREEN') return
  if (examConfig.value.monitorRapidQuestionSwitch === false && eventType === 'RAPID_QUESTION_SWITCH') return
  if (examConfig.value.monitorMultiMonitor === false && eventType === 'MULTI_MONITOR') return

  const now = Date.now()
  const lastAt = lastViolationAtByType.value[eventType] || 0
  if (now - lastAt < cooldownMs) return

  lastViolationAtByType.value = {
    ...lastViolationAtByType.value,
    [eventType]: now
  }

  try {
    queueEvent(eventType, details, { questionIndex: currentIndex.value + 1 })
  } catch {
    // ignore monitoring send failures and keep exam flow stable
  }
}

const applyAttemptStatus = (status, message = '') => {
  const normalized = String(status || '').toUpperCase() || 'IN_PROGRESS'
  attemptStatus.value = normalized
  examSessionStore.setRiskState({ status: normalized })

  if (normalized === 'STOPPED' || normalized === 'PAUSED') {
    isSuspended.value = true
    if (message) {
      suspensionMessage.value = message
    } else if (normalized === 'PAUSED') {
      suspensionMessage.value = 'Phiên thi đang được tạm dừng để giám thị kiểm tra.'
    }
    showSubmitModal.value = false
    stopMediaStream()
    return
  }

  isSuspended.value = false
}

const enforceDeviceAccess = async () => {
  if (isPracticeExam.value) return
  if (mediaStreamRef.value) {
    const videoTrack = mediaStreamRef.value.getVideoTracks()[0]
    const audioTrack = mediaStreamRef.value.getAudioTracks()[0]
    const videoEnded = !videoTrack || videoTrack.readyState === 'ended'
    const audioEnded = !audioTrack || audioTrack.readyState === 'ended'
    if (videoEnded || audioEnded) {
      isSuspended.value = true
      suspensionMessage.value = 'Camera hoặc micro đã bị thu hồi. Vui lòng tải lại trang và cấp quyền lại.'
      return
    }
    cameraReady.value = Boolean(videoTrack?.enabled)
    micReady.value = Boolean(audioTrack?.enabled)
    await syncDeviceStatusToBackend()
    return
  }
  await checkDevices()
  if (!devicesReady.value) {
    isSuspended.value = true
    suspensionMessage.value = deviceError.value || 'Bạn cần cấp quyền camera và micro để tiếp tục làm bài.'
  }
}

const requestExamFullscreen = async () => {
  if (isPracticeExam.value) return
  if (!document.documentElement?.requestFullscreen) return
  try {
    await document.documentElement.requestFullscreen()
    isFullscreenActive.value = true
    showFullscreenPrompt.value = false
    examSessionStore.setFullscreenState({ required: true, active: true })
  } catch {
    showFullscreenPrompt.value = true
  }
}

const syncAttemptStatus = async () => {
  if (!attemptId.value) return
  try {
    const detail = await getAttemptDetail(attemptId.value)
    applyAttemptStatus(detail?.status || 'IN_PROGRESS')
    if (typeof detail?.riskScore === 'number') {
      examSessionStore.setRiskState({
        riskScore: detail.riskScore,
        riskLevel: detail.riskLevel || examSessionStore.riskState.riskLevel,
        status: detail.status || attemptStatus.value
      })
    }
    // Đồng bộ thời gian còn lại từ server (tránh lệch do client)
    if (typeof detail?.remainingSeconds === 'number' && detail.remainingSeconds >= 0) {
      const diff = Math.abs(remainingSeconds.value - detail.remainingSeconds)
      if (diff > 5) remainingSeconds.value = Math.max(0, detail.remainingSeconds)
    }
  } catch {
    // ignore status sync errors
  }
}

const connectProctorRealtime = async () => {
  if (isPracticeExam.value || !attemptId.value) return
  await realtimeChannel.connect({
    reconnectDelay: 5000,
    topics: [
      {
        destination: `/topic/attempts/${attemptId.value}/proctor-actions`,
        handler: (payload) => {
          const type = String(payload?.type || '').toUpperCase()
          if (type === 'TEACHER_WARNING') {
            teacherWarningMessage.value = payload.message || 'Bạn đang bị cảnh báo bởi giám thị. Vui lòng tuân thủ quy định phòng thi.'
            showTeacherWarningModal.value = true
            toast.warning(teacherWarningMessage.value)
          }
          if (type === 'ATTEMPT_STOPPED' || type === 'ATTEMPT_PAUSED') {
            applyAttemptStatus(payload.status || (type === 'ATTEMPT_PAUSED' ? 'PAUSED' : 'STOPPED'),
              payload.message || 'Bài thi đã bị đình chỉ.')
          }
          if (type === 'RISK_UPDATE') {
            syncRiskState({
              riskScore: payload.riskScore,
              riskLevel: payload.riskLevel,
              status: payload.status
            })
            handleProctorActions([payload.actionTaken], payload)
          }
        }
      }
    ]
  })
}

const setupBlockBackButton = () => {
  history.pushState({ examInProgress: true }, '', window.location.href)
  blockBackHandler = () => {
    history.pushState({ examInProgress: true }, '', window.location.href)
    toast.warning('Không thể quay lại khi đang làm bài thi. Vui lòng nộp bài để thoát.')
  }
  window.addEventListener('popstate', blockBackHandler)
}

const teardownBlockBackButton = () => {
  if (blockBackHandler) {
    window.removeEventListener('popstate', blockBackHandler)
    blockBackHandler = null
  }
}

const handleVisibilityChange = () => {
  if (document.hidden) {
    pendingViolationByType.value = {
      ...pendingViolationByType.value,
      TAB_SWITCH: true
    }
    void reportViolation('TAB_SWITCH', 'TAB_SWITCH')
  } else if (pendingViolationByType.value.TAB_SWITCH) {
    pendingViolationByType.value = {
      ...pendingViolationByType.value,
      TAB_SWITCH: false
    }
  }
}

const handleWindowBlur = () => {
  clearBlurGraceTimer()
  blurGraceTimer = window.setTimeout(() => {
    pendingViolationByType.value = {
      ...pendingViolationByType.value,
      BLUR: true
    }
    void reportViolation('BLUR', 'Window lost focus during exam attempt')
  }, BLUR_GRACE_MS)
}

const handleWindowFocus = () => {
  clearBlurGraceTimer()
  if (pendingViolationByType.value.BLUR) {
    pendingViolationByType.value = {
      ...pendingViolationByType.value,
      BLUR: false
    }
  }
}

const handleFullscreenChange = () => {
  const inFullscreen = Boolean(document.fullscreenElement)
  isFullscreenActive.value = inFullscreen
  examSessionStore.setFullscreenState({ required: true, active: inFullscreen })
  if (inFullscreen) {
    pendingViolationByType.value = {
      ...pendingViolationByType.value,
      EXIT_FULLSCREEN: false
    }
    showFullscreenPrompt.value = false
    return
  }

  pendingViolationByType.value = {
    ...pendingViolationByType.value,
    EXIT_FULLSCREEN: true
  }
  showFullscreenPrompt.value = !isPracticeExam.value
  void reportViolation('EXIT_FULLSCREEN', 'Exited fullscreen during exam attempt')
}

const resetIdleTimer = () => {
  if (idleTimer) window.clearTimeout(idleTimer)
  idleTimer = window.setTimeout(() => {
    void reportViolation('IDLE_TIME', `Idle for ${Math.round(IDLE_THRESHOLD_MS / 60000)} minutes`, LONG_VIOLATION_COOLDOWN_MS)
  }, IDLE_THRESHOLD_MS)
}

const handleCopyPaste = (event) => {
  const target = event?.target
  if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA')) return
  const clipboardText = event?.clipboardData?.getData?.('text') || ''
  const summary = clipboardText.length > 50
    ? `${event.type} ${clipboardText.length} ký tự`
    : `Detected ${event.type} during exam`
  void reportViolation('COPY_PASTE', summary, LONG_VIOLATION_COOLDOWN_MS)
}

const detectDevToolsOpen = () => {
  if (document.hidden) return false
  const widthGap = Math.abs(window.outerWidth - window.innerWidth)
  const heightGap = Math.abs(window.outerHeight - window.innerHeight)
  return widthGap > DEVTOOLS_GAP_PX || heightGap > DEVTOOLS_GAP_PX
}

const scheduleDevToolsCheck = () => {
  if (devtoolsCheckTimer) window.clearInterval(devtoolsCheckTimer)
  devtoolsCheckTimer = window.setInterval(() => {
    if (detectDevToolsOpen()) {
      void reportViolation('DEVTOOLS_OPEN', 'DevTools detected during exam', LONG_VIOLATION_COOLDOWN_MS)
    }
  }, 5000)
}

const checkRapidQuestionSwitch = () => {
  const now = Date.now()
  questionSwitchTimestamps.value = [...questionSwitchTimestamps.value, now].filter(
    (t) => now - t <= RAPID_SWITCH_WINDOW_MS
  )
  if (questionSwitchTimestamps.value.length >= RAPID_SWITCH_THRESHOLD) {
    void reportViolation(
      'RAPID_QUESTION_SWITCH',
      `${questionSwitchTimestamps.value.length} lần đổi câu trong ${RAPID_SWITCH_WINDOW_MS / 1000}s`,
      VIOLATION_COOLDOWN_MS
    )
    questionSwitchTimestamps.value = []
  }
}

const selectQuestion = (index) => {
  if (isSuspended.value) return
  if (index !== currentIndex.value && examConfig.value.monitorRapidQuestionSwitch !== false) {
    checkRapidQuestionSwitch()
  }
  currentIndex.value = index
  const question = questions.value[index]
  if (question?.id != null) {
    visitedQuestions.value = {
      ...visitedQuestions.value,
      [String(question.id)]: true
    }
    examSessionStore.setCurrentQuestion(question.id)
  }
}

const goPrevious = () => {
  if (isSuspended.value) return
  if (currentIndex.value > 0) {
    if (examConfig.value.monitorRapidQuestionSwitch !== false) checkRapidQuestionSwitch()
    currentIndex.value -= 1
    const question = questions.value[currentIndex.value]
    if (question?.id != null) {
      visitedQuestions.value = {
        ...visitedQuestions.value,
        [String(question.id)]: true
      }
      examSessionStore.setCurrentQuestion(question.id)
    }
  }
}

const goNext = () => {
  if (isSuspended.value) return
  if (currentIndex.value < questions.value.length - 1) {
    if (examConfig.value.monitorRapidQuestionSwitch !== false) checkRapidQuestionSwitch()
    currentIndex.value += 1
    const question = questions.value[currentIndex.value]
    if (question?.id != null) {
      visitedQuestions.value = {
        ...visitedQuestions.value,
        [String(question.id)]: true
      }
      examSessionStore.setCurrentQuestion(question.id)
    }
  }
}

const handleRightClick = (e) => {
  if (examConfig.value.monitorRightClick !== false) {
    e.preventDefault()
    e.stopPropagation()
    void reportViolation('RIGHT_CLICK', 'Chuột phải bị chặn trong phòng thi', VIOLATION_COOLDOWN_MS)
  }
}

const handlePrintScreen = (e) => {
  if (examConfig.value.monitorPrintScreen !== false && (e.key === 'PrintScreen' || e.keyCode === 44)) {
    e.preventDefault()
    void reportViolation('PRINT_SCREEN', 'Phát hiện phím Print Screen', LONG_VIOLATION_COOLDOWN_MS)
  }
}

const checkMultiMonitor = () => {
  if (examConfig.value.monitorMultiMonitor === false) return
  const screenW = window.screen?.width || 0
  const screenH = window.screen?.height || 0
  const availW = window.screen?.availWidth || 0
  const availH = window.screen?.availHeight || 0
  if (screenW > 0 && availW > 0 && (screenW - availW > 100 || screenH - availH > 100)) {
    void reportViolation('MULTI_MONITOR', 'Có thể sử dụng nhiều màn hình', LONG_VIOLATION_COOLDOWN_MS)
  }
}

const persistDraftToServer = async () => {
  if (!attemptId.value || isSuspended.value) return
  const payload = Object.entries(answers.value)
    .filter(([, selectedAnswer]) => hasAnswerValue(selectedAnswer))
    .map(([questionId, selectedAnswer]) => {
      const question = questions.value.find((item) => Number(item.id) === Number(questionId))
      return {
        questionId: Number(questionId),
        selectedAnswer: serializeAnswerValue(question, selectedAnswer)
      }
    })
  if (!payload.length) return
  await saveDraftAnswers(attemptId.value, payload)
}

const { saveStatus, lastSavedAt, hasPendingChanges, schedule, forceSave, mergeLocalIntoAnswers } = useAutoSaveDraft({
  getAnswers: () => answers.value,
  getAttemptId: () => attemptId.value,
  saveToServer: persistDraftToServer,
  debounceMs: 30000
})

const saveStatusLabel = computed(() => {
  if (hasPendingChanges.value && saveStatus.value === 'idle') {
    return 'Có thay đổi chưa đồng bộ'
  }
  switch (saveStatus.value) {
    case 'saving':
      return 'Đang lưu...'
    case 'saved':
      return lastSavedAt.value
        ? `Đã lưu lúc ${new Date(lastSavedAt.value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}`
        : 'Đã lưu'
    case 'error':
      return 'Lỗi lưu — bài vẫn an toàn'
    default:
      return lastSavedAt.value
        ? `Đã lưu lúc ${new Date(lastSavedAt.value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}`
        : ''
  }
})

const onSelectAnswer = (questionId, selectedAnswer) => {
  if (isSuspended.value) return

  answers.value = {
    ...answers.value,
    [questionId]: selectedAnswer
  }
  visitedQuestions.value = {
    ...visitedQuestions.value,
    [String(questionId)]: true
  }
  examSessionStore.setAnswer(questionId, selectedAnswer)

  schedule()
}

const toggleMarkCurrentQuestion = () => {
  const question = currentQuestion.value
  if (!question) return
  const key = String(question.id)
  markedQuestions.value = {
    ...markedQuestions.value,
    [key]: !markedQuestions.value[key]
  }
  examSessionStore.toggleMarked(question.id)
}

const manualSaveDraft = async () => {
  try {
    await forceSave()
    toast.success('Đã lưu bài làm thủ công.')
  } catch {
    toast.error('Không thể lưu bài lúc này.')
  }
}

const buildSubmitPayload = () => Object.entries(answers.value)
  .filter(([, selectedAnswer]) => hasAnswerValue(selectedAnswer))
  .map(([questionId, selectedAnswer]) => {
    const question = questions.value.find((item) => Number(item.id) === Number(questionId))
    return {
      questionId: Number(questionId),
      selectedAnswer: serializeAnswerValue(question, selectedAnswer)
    }
  })

const openSubmitModal = () => {
  if (isSuspended.value) return
  showSubmitModal.value = true
}

const closeSubmitModal = () => {
  showSubmitModal.value = false
}

const autoSubmitOnTimeUp = async () => {
  if (!attemptId.value || isSuspended.value || isSubmitting.value) return
  isSubmitting.value = true
  try {
    await forceSave()
  } catch {
    /* local backup đã có */
  }
  toast.info('Hết giờ làm bài. Đang tự động nộp bài...')
  try {
    const result = await submitAttempt(attemptId.value, buildSubmitPayload())
    showSubmitModal.value = false
    router.push({
      path: '/student/submission-confirmation',
      query: {
        exam: examTitle.value,
        attemptId: attemptId.value,
        score: Math.round(Number(result?.score || 0)),
        submittedAt: result?.submittedAt || ''
      }
    })
  } catch (error) {
    toast.error('Không thể tự động nộp bài. Vui lòng thử nộp thủ công.')
  } finally {
    isSubmitting.value = false
  }
}

const submitExamAction = async () => {
  if (!attemptId.value || isSuspended.value) return

  isSubmitting.value = true
  try {
    await forceSave()
  } catch {
    /* vẫn nộp với đáp án local */
  }
  try {
    const result = await submitAttempt(attemptId.value, buildSubmitPayload())
    showSubmitModal.value = false
    router.push({
      path: '/student/submission-confirmation',
      query: {
        exam: examTitle.value,
        attemptId: attemptId.value,
        score: Math.round(Number(result?.score || 0)),
        submittedAt: result?.submittedAt || ''
      }
    })
  } catch (error) {
    toast.error('Không thể nộp bài lúc này.')
  } finally {
    isSubmitting.value = false
  }
}

const questionButtonClass = (index) => {
  const question = questions.value[index]
  if (!question) return 'bg-slate-100 dark:bg-slate-800 text-slate-400'
  const key = String(question.id)
  const answered = hasAnswerValue(answers.value[key])
  const marked = Boolean(markedQuestions.value[key])
  const visited = Boolean(visitedQuestions.value[key])

  if (index === currentIndex.value) {
    return 'border-2 border-primary bg-primary/10 text-primary ring-2 ring-primary/20'
  }

  if (marked && answered) {
    return 'bg-amber-500 text-white border-2 border-amber-500'
  }

  if (marked) {
    return 'bg-amber-100 text-amber-700 border-2 border-amber-300 dark:bg-amber-900/30 dark:text-amber-300 dark:border-amber-700'
  }

  if (answered) {
    return 'bg-primary text-white border-2 border-primary'
  }

  if (visited) {
    return 'bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-200 border-2 border-slate-300 dark:border-slate-600'
  }

  return 'bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400 border-2 border-transparent hover:border-slate-300 dark:hover:border-slate-600'
}

const handleBeforeUnload = (e) => {
  if (isSubmitting.value) return
  if (!attemptId.value) return
  if (Object.keys(answers.value).length === 0) return
  e.preventDefault()
  e.returnValue = ''
}

const handleExamKeydown = (e) => {
  if (isSuspended.value) return
  if (e.key === 'F12' || (e.ctrlKey && ['c', 'v', 'u'].includes(String(e.key || '').toLowerCase()))) {
    e.preventDefault()
    void reportViolation('DEVTOOLS_OPEN', `Blocked shortcut: ${e.key}`, LONG_VIOLATION_COOLDOWN_MS)
    return
  }
  if (!e.ctrlKey) return
  if (e.key === 'ArrowLeft') {
    e.preventDefault()
    goPrevious()
  } else if (e.key === 'ArrowRight') {
    e.preventDefault()
    goNext()
  }
}

onBeforeRouteLeave(() => {
  if (isSubmitting.value) return true
  if (isPracticeExam.value) return true
  return window.confirm('Bạn có chắc muốn rời khỏi bài thi? Đáp án đã lưu cục bộ trên thiết bị.')
})

onMounted(async () => {
  try {
    if (!examId.value || !attemptId.value) {
      toast.error('Thiếu thông tin bài thi/lượt làm bài.')
      examLoadFailed.value = true
      examSurfaceReady.value = true
      return
    }

    const [{ getExamDetail }] = await Promise.all([
      import('../../services/examService')
    ])

    const [questionList, draftData, examDetail] = await Promise.all([
      listExamQuestions(examId.value),
      getDraftAnswers(attemptId.value),
      getExamDetail(examId.value)
    ])

    examConfig.value = {
      monitorTabSwitch: examDetail?.monitorTabSwitch !== false,
      monitorBlur: examDetail?.monitorBlur !== false,
      monitorExitFullscreen: examDetail?.monitorExitFullscreen !== false,
      monitorCopyPaste: examDetail?.monitorCopyPaste !== false,
      monitorIdleTime: examDetail?.monitorIdleTime !== false,
      monitorDevtools: examDetail?.monitorDevtools !== false,
      monitorDuplicateIp: examDetail?.monitorDuplicateIp !== false,
      monitorFastSubmit: examDetail?.monitorFastSubmit !== false,
      monitorRightClick: examDetail?.monitorRightClick !== false,
      monitorPrintScreen: examDetail?.monitorPrintScreen !== false,
      monitorRapidQuestionSwitch: examDetail?.monitorRapidQuestionSwitch !== false,
      monitorMultiMonitor: examDetail?.monitorMultiMonitor !== false,
      requireCameraMic: examDetail?.requireCameraMic !== false
    }

    questions.value = questionList.map((item) => ({
      id: item.id,
      content: item.content,
      type: item.type || 'SINGLE_CHOICE',
      options: parseQuestionOptions(item.options),
      metadata: parseQuestionJson(item.metadata, null),
      attachments: parseQuestionJson(item.attachments, [])
    }))
    examSurfaceReady.value = true

    const serverAnswers = (draftData?.answers || []).reduce((acc, answer) => {
      const question = questions.value.find((item) => Number(item.id) === Number(answer.questionId))
      acc[answer.questionId] = deserializeAnswerValue(question, answer.selectedAnswer)
      return acc
    }, {})
    answers.value = mergeLocalIntoAnswers(serverAnswers)
    const answeredQuestionKeys = Object.keys(answers.value).reduce((acc, key) => {
      acc[String(key)] = true
      return acc
    }, {})
    examSessionStore.hydrateSession({
      attempt: { id: attemptId.value, status: draftData?.status || 'IN_PROGRESS' },
      exam: { id: examId.value, title: examTitle.value },
      questions: questions.value,
      answers: answers.value
    })
    if (questions.value[0]?.id != null) {
      visitedQuestions.value = {
        [String(questions.value[0].id)]: true,
        ...answeredQuestionKeys
      }
      examSessionStore.setCurrentQuestion(questions.value[0].id)
    }

    applyAttemptStatus(draftData?.status || 'IN_PROGRESS')

    // Thời gian làm bài tính từ lúc sinh viên tham gia (startAttempt). Đồng bộ từ backend.
    const serverRemaining = draftData?.remainingSeconds
    const serverDeadline = draftData?.deadlineAt || route.query.deadlineAt
    if (typeof serverRemaining === 'number' && serverRemaining >= 0) {
      remainingSeconds.value = Math.max(0, serverRemaining)
    } else if (serverDeadline) {
      const deadlineMs = new Date(String(serverDeadline)).getTime()
      remainingSeconds.value = Math.max(0, Math.floor((deadlineMs - Date.now()) / 1000))
    }

    initialRemainingForProgress.value = Math.max(remainingSeconds.value, 1)

    if (remainingSeconds.value <= 0 && !isPracticeExam.value) {
      void autoSubmitOnTimeUp()
      return
    }

    timerId = window.setInterval(() => {
      if (remainingSeconds.value > 0) {
        remainingSeconds.value -= 1
        if (remainingSeconds.value === 0) {
          if (timerId) {
            clearInterval(timerId)
            timerId = null
          }
          void autoSubmitOnTimeUp()
        }
      }
    }, 1000)

    await enforceDeviceAccess()

    if (!devicesReady.value) {
      if (!shouldCheckDevices.value) {
        isSuspended.value = false
      } else {
        toast.error(suspensionMessage.value)
        return
      }
    }

    isSuspended.value = false
    isFullscreenActive.value = Boolean(document.fullscreenElement)
    if (!isPracticeExam.value && examConfig.value.monitorExitFullscreen !== false && !isFullscreenActive.value) {
      showFullscreenPrompt.value = true
      await requestExamFullscreen()
    }

    setupBlockBackButton()

    window.addEventListener('beforeunload', handleBeforeUnload)
    window.addEventListener('keydown', handleExamKeydown)

    if (!isPracticeExam.value) {
      await connectProctorRealtime()
      startHeartbeat()
      void syncHeartbeat()
      attemptStatusTimer = window.setInterval(() => {
        syncAttemptStatus()
        enforceDeviceAccess()
      }, 5000)

      if (examConfig.value.monitorIdleTime !== false) {
        resetIdleTimer()
        window.addEventListener('mousemove', resetIdleTimer)
        window.addEventListener('keydown', resetIdleTimer)
        window.addEventListener('scroll', resetIdleTimer)
      }

      if (examConfig.value.monitorDevtools !== false) {
        scheduleDevToolsCheck()
      }

      if (examConfig.value.monitorTabSwitch !== false) {
        document.addEventListener('visibilitychange', handleVisibilityChange)
      }
      if (examConfig.value.monitorBlur !== false) {
        window.addEventListener('blur', handleWindowBlur)
        window.addEventListener('focus', handleWindowFocus)
      }
      if (examConfig.value.monitorExitFullscreen !== false) {
        document.addEventListener('fullscreenchange', handleFullscreenChange)
      }
      if (examConfig.value.monitorCopyPaste !== false) {
        document.addEventListener('copy', handleCopyPaste)
        document.addEventListener('paste', handleCopyPaste)
      }
      if (examConfig.value.monitorPrintScreen !== false) {
        document.addEventListener('keydown', handlePrintScreen)
      }
      if (examConfig.value.monitorMultiMonitor !== false) {
        setTimeout(checkMultiMonitor, 3000)
      }
    }
  } catch (error) {
    examLoadFailed.value = true
    examSurfaceReady.value = true
    toast.error('Không thể tải nội dung bài thi.')
  }
})

onUnmounted(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  window.removeEventListener('keydown', handleExamKeydown)
  teardownBlockBackButton()
  stopMediaStream()
  stopHeartbeat()
  void flushQueuedViolations().catch(() => {})
  if (timerId) window.clearInterval(timerId)
  if (attemptStatusTimer) window.clearInterval(attemptStatusTimer)
  if (idleTimer) window.clearTimeout(idleTimer)
  if (devtoolsCheckTimer) window.clearInterval(devtoolsCheckTimer)
  clearBlurGraceTimer()
  realtimeChannel.disconnect()
  if (!isPracticeExam.value) {
    if (examConfig.value.monitorTabSwitch !== false) {
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
    if (examConfig.value.monitorBlur !== false) {
      window.removeEventListener('blur', handleWindowBlur)
      window.removeEventListener('focus', handleWindowFocus)
    }
    if (examConfig.value.monitorExitFullscreen !== false) {
      document.removeEventListener('fullscreenchange', handleFullscreenChange)
    }
    if (examConfig.value.monitorCopyPaste !== false) {
      document.removeEventListener('copy', handleCopyPaste)
      document.removeEventListener('paste', handleCopyPaste)
    }
    if (examConfig.value.monitorPrintScreen !== false) {
      document.removeEventListener('keydown', handlePrintScreen)
    }
    if (examConfig.value.monitorIdleTime !== false) {
      window.removeEventListener('mousemove', resetIdleTimer)
      window.removeEventListener('keydown', resetIdleTimer)
      window.removeEventListener('scroll', resetIdleTimer)
    }
  }
})
</script>

<style scoped>
.font-display {
  font-family: 'Public Sans', sans-serif;
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

.exam-timer-progress {
  transition: opacity var(--duration-normal, 250ms) var(--easing-default, ease);
}

.exam-timer--warning {
  accent-color: var(--color-warning);
}

.exam-timer--danger {
  accent-color: var(--color-danger);
}

@keyframes exam-timer-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.65;
  }
}

.exam-timer--pulse {
  animation: exam-timer-pulse 1s ease-in-out infinite;
}

@media (prefers-reduced-motion: reduce) {
  .exam-timer--pulse {
    animation: none;
  }
}
</style>
