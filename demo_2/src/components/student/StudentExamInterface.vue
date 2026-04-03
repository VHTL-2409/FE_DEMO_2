<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="portal-viewport flex h-full min-h-0 flex-col font-display"
    style="background: var(--glass-bg)"
    @contextmenu="handleRightClick"
  >
    <div class="shrink-0">
    <StudentTopHeader
      :show-profile="false"
      :show-sign-out="false"
      :show-notifications="false"
      :show-menu="false"
      :compact-exam-kicker="isPracticeExam ? 'Luyện tập' : 'Bài thi'"
      :compact-exam-title="examTitle"
    >
      <template #rightActions>
        <div
          v-if="shouldCheckDevices"
          class="hidden shrink-0 items-center gap-1 rounded-lg border px-1.5 py-0.5 md:flex"
          style="border-color: var(--glass-danger-border); background: var(--glass-danger-soft)"
        >
          <span class="stitch-proctoring-pulse relative inline-flex text-[#ba1a1a] dark:text-rose-400">
            <span class="material-symbols-outlined text-sm" style="font-variation-settings: 'FILL' 1">videocam</span>
          </span>
          <span class="text-[8px] font-bold uppercase tracking-tight text-[#ba1a1a] dark:text-rose-300">Giám sát</span>
        </div>
        <div class="hidden shrink-0 items-center gap-1.5 whitespace-nowrap text-[10px] font-medium lg:flex xl:gap-2 xl:text-xs">
          <span :style="cameraReady ? 'color: var(--glass-success)' : 'color: var(--glass-danger)'" class="material-symbols-outlined text-sm leading-none">videocam</span>
          <span :style="cameraReady ? 'color: var(--glass-success)' : 'color: var(--glass-danger)'">{{ cameraReady ? 'Camera bật' : 'Camera tắt' }}</span>
          <span class="h-1 w-1 shrink-0 rounded-full" :style="micReady ? 'background: var(--glass-success)' : 'background: var(--glass-danger)'" />
          <span :style="micReady ? 'color: var(--glass-success)' : 'color: var(--glass-danger)'" class="material-symbols-outlined text-sm leading-none">mic</span>
          <span :style="micReady ? 'color: var(--glass-success)' : 'color: var(--glass-danger)'">{{ micReady ? 'Mic bật' : 'Mic tắt' }}</span>
          <span class="h-1 w-1 shrink-0 rounded-full" style="background: var(--glass-border)" />
          <span class="material-symbols-outlined text-sm leading-none" style="color: var(--glass-text-muted)">wifi</span>
          <span style="color: var(--glass-text-muted)">Đã kết nối</span>
        </div>
        <div
          v-if="initialRemainingForProgress > 0"
          class="flex w-[5.5rem] shrink-0 flex-col justify-center gap-0.5 md:hidden"
          role="group"
          aria-label="Thời gian còn lại"
        >
          <progress
            class="exam-timer-progress h-1.5 w-full rounded-full overflow-hidden accent-[color:var(--color-primary)]"
            :class="timerBarAccentClass"
            :value="remainingSeconds"
            :max="initialRemainingForProgress || 1"
            :aria-valuenow="remainingSeconds"
            aria-valuemin="0"
            :aria-valuemax="initialRemainingForProgress || 1"
            :aria-label="timerAriaLabel"
          />
        </div>
        <div
          v-if="!examSurfaceReady || !questions.length"
          class="portal-panel-soft hidden shrink-0 items-center gap-2 rounded-full px-3 py-1.5 md:flex"
        >
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
          v-if="!examSurfaceReady || !questions.length"
          class="portal-panel-soft flex shrink-0 items-center gap-1.5 rounded-full px-2.5 py-1.5 md:hidden"
        >
          <div class="flex flex-col items-center leading-none">
            <span class="text-xs font-bold tabular-nums">{{ timerHours }}</span>
            <span class="text-[8px] uppercase tracking-wider opacity-60">H</span>
          </div>
          <span class="text-sm font-light opacity-30">:</span>
          <div class="flex flex-col items-center leading-none">
            <span class="text-xs font-bold tabular-nums">{{ timerMinutes }}</span>
            <span class="text-[8px] uppercase tracking-wider opacity-60">M</span>
          </div>
          <span class="text-sm font-light opacity-30">:</span>
          <div class="flex flex-col items-center leading-none">
            <span class="text-xs font-bold tabular-nums">{{ timerSeconds }}</span>
            <span class="text-[8px] uppercase tracking-wider opacity-60">S</span>
          </div>
        </div>
        <div
          v-if="examSurfaceReady && questions.length"
          class="student-exam-focus-center glass gs-card--glass inline-flex max-w-[min(100vw-5rem,48rem)] shrink-0 flex-row flex-nowrap items-center gap-2 rounded-xl border border-[#dbc2b0]/30 px-2 py-1 dark:border-slate-600/50 sm:gap-2.5 sm:px-2.5 sm:py-1 md:gap-3 md:px-3"
        >
          <!-- Circular Timer -->
          <div class="exam-timer relative inline-flex items-center justify-center">
            <svg class="exam-timer__ring" width="48" height="48" viewBox="0 0 48 48">
              <circle cx="24" cy="24" r="20" fill="none" stroke="var(--glass-border)" stroke-width="3" />
              <circle
                cx="24" cy="24" r="20"
                fill="none"
                stroke="var(--glass-amber)"
                stroke-width="3"
                stroke-linecap="round"
                :stroke-dasharray="examCircumference"
                :stroke-dashoffset="examProgressOffset"
                transform="rotate(-90 24 24)"
                class="transition-all duration-1000"
              />
            </svg>
            <div class="absolute inset-0 flex items-center justify-center">
              <span class="font-display text-xs font-bold tabular-nums text-[--glass-text]">{{ timerMinutes }}:{{ timerSeconds }}</span>
            </div>
          </div>
          <div
            v-if="initialRemainingForProgress > 0"
            class="flex w-[4.25rem] shrink-0 flex-col justify-center gap-0.5 sm:w-[5.5rem]"
            role="group"
            aria-label="Thời gian còn lại"
          >
            <progress
              class="exam-timer-progress h-1 w-full rounded-full overflow-hidden accent-[color:var(--glass-amber)]"
              :class="timerBarAccentClass"
              :value="remainingSeconds"
              :max="initialRemainingForProgress || 1"
              :aria-valuenow="remainingSeconds"
              aria-valuemin="0"
              :aria-valuemax="initialRemainingForProgress || 1"
              :aria-label="timerAriaLabel"
            />
          </div>
          <div class="flex min-w-0 shrink-0 items-center gap-1.5 sm:gap-2">
            <div class="h-1 w-14 rounded-full sm:w-20" style="background: var(--glass-border)">
              <div
                class="h-full rounded-full transition-all duration-300"
                style="background: linear-gradient(90deg, var(--glass-amber), var(--glass-amber-hover))"
                :style="{ width: `${questionNavigatePercent}%` }"
              />
            </div>
            <span class="shrink-0 whitespace-nowrap text-[9px] font-bold uppercase tracking-wide sm:text-[10px]" style="color: var(--glass-text-muted)">
              Câu {{ currentIndex + 1 }}/{{ questions.length }}
            </span>
          </div>
        </div>
        <div
          v-if="saveStatusLabel"
          class="hidden min-w-0 max-w-[min(11rem,28vw)] shrink-0 items-center gap-1.5 text-[10px] font-semibold text-slate-600 dark:text-slate-300 md:flex"
          role="status"
          aria-live="polite"
        >
          <span v-if="saveStatus === 'saving'" class="material-symbols-outlined shrink-0 text-sm animate-spin">progress_activity</span>
          <span v-else-if="saveStatus === 'saved'" class="material-symbols-outlined shrink-0 text-sm text-emerald-600">check_circle</span>
          <span v-else-if="hasPendingChanges" class="material-symbols-outlined shrink-0 text-sm text-amber-600">schedule</span>
          <span v-else-if="saveStatus === 'error'" class="material-symbols-outlined shrink-0 text-sm text-amber-600">cloud_off</span>
          <span class="min-w-0 truncate leading-4">{{ saveStatusLabel }}</span>
        </div>
        <BaseButton
          type="button"
          size="sm"
          variant="ghost"
          class="hidden shrink-0 md:inline-flex text-xs [min-height:calc(2.4rem*1.05)] [padding:calc(0.65rem*1.05)_calc(1rem*1.05)] [font-size:calc(0.86rem*1.05)]"
          :disabled="isSuspended || saveStatus === 'saving'"
          @click="manualSaveDraft"
        >
          Lưu ngay
        </BaseButton>
        <BaseButton
          type="button"
          size="sm"
          class="shrink-0 text-xs [min-height:calc(2.4rem*1.05)] [padding:calc(0.65rem*1.05)_calc(1rem*1.05)] [font-size:calc(0.86rem*1.05)]"
          :disabled="isSuspended"
          @click="openSubmitModal"
        >
          Nộp bài
        </BaseButton>
      </template>
    </StudentTopHeader>
    </div>

    <main class="relative flex w-full min-h-0 flex-1 flex-col gap-3 overflow-x-hidden p-3 sm:p-4 md:gap-4 lg:flex-row lg:gap-4">
      <div
        v-if="showFullscreenPrompt && !isPracticeExam && !isSuspended"
          class="absolute inset-x-4 top-0 z-30 rounded-2xl border px-4 py-3 shadow-lg backdrop-blur sm:inset-x-6"
          style="border-color: var(--glass-amber-border); background: var(--glass-amber-soft)"
      >
        <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p class="text-sm font-bold" style="color: var(--glass-amber)">Yêu cầu toàn màn hình đang tắt</p>
            <p class="text-xs" style="color: var(--glass-amber)">Bấm vào để tiếp tục làm bài trong chế độ toàn màn hình.</p>
          </div>
          <BaseButton type="button" size="sm" @click="requestExamFullscreen">Vào toàn màn hình</BaseButton>
        </div>
      </div>
      <div v-if="isSuspended" class="absolute inset-0 z-20 flex items-center justify-center px-4" style="background: rgba(0,0,0,0.6); backdrop-filter: blur(8px)">
        <div class="glass-card max-w-lg w-full rounded-2xl p-6 text-center shadow-2xl">
          <h2 class="text-xl font-bold mb-2" style="color: var(--glass-danger)">{{ attemptStatus === 'PAUSED' ? 'Phiên thi đang tạm dừng' : 'Bài thi đã bị đình chỉ' }}</h2>
          <p class="text-sm" style="color: var(--glass-text-secondary)">{{ suspensionMessage || 'Giám thị đã hủy hiệu lực bài thi của bạn.' }}</p>
        </div>
      </div>

      <!-- Modal cảnh báo từ giám thị -->
      <div v-if="showTeacherWarningModal" class="fixed inset-0 z-[70] flex items-center justify-center p-4 backdrop-blur-sm" style="background: rgba(180,120,0,0.2)" @click.self="showTeacherWarningModal = false">
        <div class="glass-card max-w-lg w-full rounded-2xl border p-6 shadow-2xl gs-scale" style="border-color: var(--glass-amber-border)">
          <div class="flex items-center gap-4 mb-4">
            <div class="w-14 h-14 rounded-2xl flex items-center justify-center shrink-0" style="background: var(--glass-amber-soft)">
              <span class="material-symbols-outlined text-3xl" style="color: var(--glass-amber)">warning</span>
            </div>
            <div>
              <h2 class="text-xl font-bold" style="color: var(--glass-amber)">Cảnh báo từ giám thị</h2>
              <p class="text-sm mt-1" style="color: var(--glass-text-secondary)">Vui lòng chú ý và tuân thủ quy định phòng thi.</p>
            </div>
          </div>
          <div class="rounded-xl p-4 mb-6" style="background: var(--glass-amber-soft); border: 1px solid var(--glass-amber-border)">
            <p class="font-medium" style="color: var(--glass-text)">{{ teacherWarningMessage }}</p>
          </div>
          <button type="button" class="w-full py-3 rounded-xl flex items-center justify-center gap-2 font-bold transition-all gs-spring"
            style="background: linear-gradient(135deg, var(--glass-amber) 0%, var(--glass-amber-hover) 100%); color: white"
            @click="showTeacherWarningModal = false">
            <span class="material-symbols-outlined">check_circle</span>
            Tôi đã hiểu
          </button>
        </div>
      </div>
      <div class="relative flex min-h-0 min-w-0 flex-1 flex-col gap-3">

        <div
          v-if="!examSurfaceReady"
          class="glass-card flex min-h-[min(50dvh,20rem)] flex-1 flex-col items-center justify-center overflow-hidden rounded-2xl px-4 py-10"
        >
          <span class="material-symbols-outlined mb-3 animate-spin text-4xl gs-pulse" style="color: var(--glass-amber)">progress_activity</span>
          <p class="text-sm font-semibold" style="color: var(--glass-text)">Đang tải đề thi…</p>
          <p class="mt-1 max-w-xs text-center text-xs" style="color: var(--glass-text-muted)">Vui lòng đợi trong giây lát.</p>
        </div>

        <div
          v-else-if="examSurfaceReady && examLoadFailed"
          class="glass-card flex min-h-[min(40dvh,16rem)] flex-1 flex-col items-center justify-center overflow-hidden rounded-2xl px-4 py-8 text-center"
        >
          <span class="material-symbols-outlined mb-2 text-4xl" style="color: var(--glass-danger)">error</span>
          <p class="text-sm font-bold" style="color: var(--glass-text)">Không tải được đề thi</p>
          <p class="mt-1 max-w-sm text-xs" style="color: var(--glass-text-muted)">Vui lòng làm mới trang hoặc quay lại sau.</p>
        </div>

        <div
          v-else-if="examSurfaceReady && !questions.length"
          class="glass-card flex min-h-[min(45dvh,18rem)] flex-1 flex-col items-center justify-center overflow-hidden rounded-2xl px-4 py-8 text-center"
        >
          <span class="material-symbols-outlined mb-2 text-4xl" style="color: var(--glass-text-muted)">quiz</span>
          <p class="text-sm font-bold" style="color: var(--glass-text)">Không có câu hỏi</p>
          <p class="mt-1 max-w-sm text-xs" style="color: var(--glass-text-muted)">Đề thi chưa có nội dung. Hãy thoát và liên hệ giáo viên.</p>
        </div>

        <div
          v-else-if="currentQuestion"
          class="glass-card gs-scale flex min-h-0 flex-1 flex-col overflow-hidden rounded-2xl border"
          style="border-color: var(--glass-border)"
        >
          <div
            class="portal-scrollbar flex min-h-0 flex-1 flex-col overflow-y-auto lg:flex-row lg:gap-0"
          >
            <div
              class="student-exam-focus-stem min-h-0 flex-1 p-5 sm:p-6 md:p-7 lg:max-w-[58%] lg:border-r"
              style="background: var(--glass-bg-warm); border-color: var(--glass-border)"
            >
              <div class="mb-6 flex shrink-0 items-center justify-between gap-2">
                <span class="inline-flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-bold" style="background: var(--glass-amber-soft); color: var(--glass-amber)">
                  <span class="material-symbols-outlined text-base">quiz</span>
                  Câu {{ currentIndex + 1 }} / {{ questions.length }}
                </span>
                <button
                  type="button"
                  class="inline-flex items-center gap-2 px-4 py-2 text-xs font-semibold rounded-xl border transition-all gs-spring"
                  style="background: var(--glass-surface); border-color: var(--glass-border); color: var(--glass-text-secondary)"
                  :disabled="isSuspended"
                  @click="toggleMarkCurrentQuestion"
                >
                  <span class="material-symbols-outlined text-base">{{
                    markedQuestions[String(currentQuestion.id)] ? 'bookmark_added' : 'bookmark_add'
                  }}</span>
                  {{ markedQuestions[String(currentQuestion.id)] ? 'Bỏ đánh dấu' : 'Đánh dấu xem lại' }}
                </button>
              </div>
              <div class="prose dark:prose-invert max-w-none">
                <h2 class="text-xl font-semibold leading-relaxed sm:text-2xl" style="font-family: 'DM Sans', serif; color: var(--glass-text)">
                  {{ currentQuestion.content }}
                </h2>
              </div>
            </div>
            <div
              class="student-exam-focus-answer min-h-0 flex-1 p-5 sm:p-6 md:p-7"
              style="background: var(--glass-bg); color: var(--glass-text)"
            >
              <p class="text-xs font-bold uppercase tracking-wider mb-4" style="color: var(--glass-text-muted)">Phần trả lời</p>
              <QuestionRenderer :question="currentQuestion" v-model="currentQuestionAnswer" :disabled="isSuspended" />
            </div>
          </div>

          <div class="shrink-0 border-t px-4 py-3 sm:px-6 sm:py-4" style="background: var(--glass-surface); border-color: var(--glass-border)">
            <div class="flex flex-col-reverse items-stretch justify-between gap-3 sm:flex-row sm:items-center">
              <button @click="goPrevious" :disabled="currentIndex === 0" class="flex items-center justify-center gap-2 px-5 py-3 font-bold rounded-xl border transition-all gs-spring disabled:cursor-not-allowed disabled:opacity-40"
                style="background: var(--glass-surface); border-color: var(--glass-border); color: var(--glass-text-secondary)" type="button">
                <span class="material-symbols-outlined text-xl">arrow_back</span>
                Câu trước
              </button>
              <div class="flex flex-col gap-2 sm:flex-row">
                <button
                  type="button"
                  class="flex items-center justify-center gap-2 px-4 py-3 text-sm font-semibold rounded-xl border transition-all gs-spring"
                  style="background: var(--glass-surface); border-color: var(--glass-border); color: var(--glass-text-secondary)"
                  :disabled="isSuspended"
                  @click="toggleMarkCurrentQuestion"
                >
                  <span class="material-symbols-outlined text-lg">bookmark</span>
                  {{ markedQuestions[String(currentQuestion.id)] ? 'Đã đánh dấu' : 'Đánh dấu' }}
                </button>
                <button @click="goNext" :disabled="currentIndex >= questions.length - 1"
                  class="flex items-center justify-center gap-2 px-6 py-3 font-bold rounded-xl transition-all gs-spring disabled:cursor-not-allowed disabled:opacity-40 sm:flex-initial"
                  style="background: linear-gradient(135deg, var(--glass-amber) 0%, var(--glass-amber-hover) 100%); color: white; border: none" type="button">
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
        <div v-if="shouldCheckDevices && mediaStreamRef" class="glass-card overflow-hidden rounded-2xl">
          <div class="px-3 py-2 border-b flex items-center justify-between gap-2" style="border-color: var(--glass-border)">
            <span class="text-xs font-bold" style="color: var(--glass-text)">Camera của bạn</span>
            <div class="flex items-center gap-1">
              <button
                :disabled="isSuspended"
                :title="cameraReady ? 'Tắt camera' : 'Bật camera'"
                :style="cameraReady ? 'background: var(--glass-success-soft); color: var(--glass-success)' : 'background: var(--glass-danger-soft); color: var(--glass-danger)'"
                class="p-2 rounded-lg transition-all disabled:opacity-50"
                type="button"
                @click="toggleCamera"
              >
                <span class="material-symbols-outlined text-lg">{{ cameraReady ? 'videocam' : 'videocam_off' }}</span>
              </button>
              <button
                :disabled="isSuspended"
                :title="micReady ? 'Tắt micro' : 'Bật micro'"
                :style="micReady ? 'background: var(--glass-success-soft); color: var(--glass-success)' : 'background: var(--glass-danger-soft); color: var(--glass-danger)'"
                class="p-2 rounded-lg transition-all disabled:opacity-50"
                type="button"
                @click="toggleMic"
              >
                <span class="material-symbols-outlined text-lg">{{ micReady ? 'mic' : 'mic_off' }}</span>
              </button>
            </div>
          </div>
          <div class="relative aspect-video" style="background: var(--glass-bg-mid)">
            <video ref="cameraPreviewRef" autoplay playsinline muted class="w-full h-full object-cover" :class="{ 'opacity-0': !cameraReady }" />
            <div v-if="!cameraReady" class="absolute inset-0 flex items-center justify-center" style="background: rgba(0,0,0,0.7)">
              <span class="material-symbols-outlined text-4xl" style="color: var(--glass-text-muted)">videocam_off</span>
            </div>
          </div>
        </div>
        <div v-if="examSurfaceReady" class="glass gs-card rounded-[1.25rem] p-4 dark:bg-slate-900 sm:p-5">
          <div class="flex justify-between items-center mb-3">
            <h3 class="gs-heading-serif font-bold text-sm text-[--glass-text] flex items-center gap-2">
              <span class="material-symbols-outlined text-[--glass-amber] text-lg">trending_up</span>
              Tiến độ làm bài
            </h3>
            <span class="gs-stat__value text-lg tabular-nums">{{ progressPercent }}%</span>
          </div>
          <div class="w-full h-2.5 rounded-full overflow-hidden" style="background: var(--glass-bg-mid)">
            <div class="h-full rounded-full transition-all duration-500" style="background: linear-gradient(90deg, var(--glass-amber), var(--glass-amber-hover))" :style="{ width: `${progressPercent}%` }"></div>
          </div>
          <div class="mt-3 flex items-center justify-between text-xs text-[--glass-text-secondary]">
            <span class="flex items-center gap-1.5"><span class="w-2 h-2 rounded-full bg-[--glass-amber]"></span> Đã làm: {{ answeredCount }}</span>
            <span class="flex items-center gap-1.5"><span class="w-2 h-2 rounded-full bg-slate-300 dark:bg-slate-600"></span> Chưa làm: {{ unansweredCount }}</span>
          </div>
          <div class="mt-2 flex flex-wrap gap-2 text-[11px] text-[--glass-text-muted]">
            <span class="gs-badge gs-badge--amber">Đánh dấu: {{ markedCount }}</span>
            <span class="gs-badge gs-badge--neutral">Bỏ qua: {{ skippedCount }}</span>
          </div>
        </div>

        <div
          v-if="examSurfaceReady"
          class="glass gs-card portal-scrollbar flex min-h-0 max-h-[min(52dvh,28rem)] flex-1 overflow-y-auto rounded-2xl lg:max-h-none"
        >
          <div class="flex w-full min-w-0 flex-col p-4 sm:p-5">
          <div class="flex items-center justify-between mb-3">
            <h3 class="gs-heading-serif font-bold text-sm text-[--glass-text] flex items-center gap-2">
              <span class="material-symbols-outlined text-[--glass-amber] text-lg">list</span>
              Danh sách câu hỏi
            </h3>
            <span class="text-xs text-[--glass-text-muted] font-medium">{{ questions.length }} câu</span>
          </div>
          <div class="grid grid-cols-5 gap-2 pb-2 overscroll-contain">
            <button
              v-for="(question, idx) in questions"
              :key="question.id"
              :style="questionButtonStyle(idx)"
              class="gs-spring aspect-square flex items-center justify-center rounded-xl text-xs font-bold transition-all duration-200 hover:scale-105"
              type="button"
              @click="selectQuestion(idx)"
            >
              {{ idx + 1 }}
            </button>
          </div>
          <div class="mt-3 grid grid-cols-2 gap-2 text-[11px]" style="color: var(--glass-text-muted)">
            <span class="inline-flex items-center gap-1.5"><span class="h-2 w-2 rounded-full" style="background: var(--glass-amber)"></span> Đã làm</span>
            <span class="inline-flex items-center gap-1.5"><span class="h-2 w-2 rounded-full" style="background: var(--glass-amber-hover)"></span> Đánh dấu</span>
            <span class="inline-flex items-center gap-1.5"><span class="h-2 w-2 rounded-full" style="background: var(--glass-border)"></span> Bỏ qua</span>
            <span class="inline-flex items-center gap-1.5"><span class="h-2 w-2 rounded-full border" style="border-color: var(--glass-text-muted)"></span> Chưa mở</span>
          </div>
          </div>
        </div>
      </aside>
    </main>

    <footer class="shrink-0 border-t px-3 py-2 text-center text-[11px]" style="background: var(--glass-surface); border-color: var(--glass-border); color: var(--glass-text-muted)">
      Hỗ trợ:
      <span class="cursor-pointer font-semibold" style="color: var(--glass-amber)">support@edu-portal.com</span>
    </footer>

    <BaseModal v-model="showSubmitModal" title="Xác nhận nộp bài" :persistent="true">
      <div class="space-y-4">
        <p class="text-sm text-[--glass-text-secondary]">
          Đã trả lời <strong class="text-[--glass-text]">{{ answeredCount }}</strong> / {{ questions.length }} câu.
          <span v-if="unansweredCount > 0" class="text-[--glass-warning] font-semibold">
            Còn {{ unansweredCount }} câu chưa trả lời.
          </span>
        </p>
        <div class="grid grid-cols-2 gap-3 text-xs">
          <div class="gs-card p-4">
            <p class="text-[--glass-text-muted]">Đánh dấu xem lại</p>
            <p class="gs-stat__value mt-1 text-lg text-[--glass-amber]">{{ markedCount }}</p>
          </div>
          <div class="gs-card p-4">
            <p class="text-[--glass-text-muted]">Đã mở nhưng bỏ qua</p>
            <p class="gs-stat__value mt-1 text-lg text-[--glass-text]">{{ skippedCount }}</p>
          </div>
        </div>
        <p class="text-sm text-[--glass-text-muted]">Bạn sẽ không thể thay đổi đáp án sau khi nộp.</p>
        <div v-if="unansweredCount > 0" class="glass-soft rounded-xl p-4">
          <p class="text-sm font-semibold text-[--glass-warning] flex items-center gap-2">
            <span class="material-symbols-outlined text-lg" aria-hidden="true">warning</span>
            Bạn còn {{ unansweredCount }} câu chưa làm và {{ notVisitedCount }} câu chưa mở.
          </p>
        </div>
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
/** Tiến độ vị trí câu hiện tại (header Stitch — thanh "Câu x / n") */
const questionNavigatePercent = computed(() => {
  if (!questions.value.length) return 0
  return Math.round(((currentIndex.value + 1) / questions.value.length) * 100)
})

// Circular timer computed properties
const examCircumference = 2 * Math.PI * 20 // r=20 for header timer
const examProgressOffset = computed(() => {
  if (!initialRemainingForProgress.value) return 0
  const ratio = remainingSeconds.value / initialRemainingForProgress.value
  return examCircumference * (1 - ratio)
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

const questionButtonStyle = (index) => {
  const question = questions.value[index]
  if (!question) return { background: 'var(--glass-bg-mid)', color: 'var(--glass-text-muted)', borderColor: 'var(--glass-border)' }
  const key = String(question.id)
  const answered = hasAnswerValue(answers.value[key])
  const marked = Boolean(markedQuestions.value[key])
  const visited = Boolean(visitedQuestions.value[key])

  if (index === currentIndex.value) {
    return { background: 'var(--glass-amber-soft)', color: 'var(--glass-amber)', borderColor: 'var(--glass-amber)' }
  }
  if (marked && answered) {
    return { background: 'var(--glass-amber)', color: 'white', borderColor: 'var(--glass-amber)' }
  }
  if (marked) {
    return { background: 'var(--glass-amber-soft)', color: 'var(--glass-amber)', borderColor: 'var(--glass-amber-border)' }
  }
  if (answered) {
    return { background: 'var(--glass-amber)', color: 'white', borderColor: 'var(--glass-amber)' }
  }
  if (visited) {
    return { background: 'var(--glass-bg-mid)', color: 'var(--glass-text-secondary)', borderColor: 'var(--glass-border)' }
  }
  return { background: 'var(--glass-bg-mid)', color: 'var(--glass-text-muted)', borderColor: 'var(--glass-border)' }
}

const questionButtonClass = (index) => {
  if (index === currentIndex.value) {
    return 'border-2 text-sm font-bold bg-[var(--glass-amber-soft)] text-[var(--glass-amber)] border-[var(--glass-amber)]'
  }
  const question = questions.value[index]
  if (!question) return 'border-2 text-sm font-bold bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400 border-2 border-transparent hover:border-slate-300 dark:hover:border-slate-600'
  const key = String(question.id)
  const answered = hasAnswerValue(answers.value[key])
  const marked = Boolean(markedQuestions.value[key])
  if (marked && answered) return 'border-2 text-sm font-bold bg-[var(--glass-amber)] text-white border-[var(--glass-amber)]'
  if (marked) return 'border-2 text-sm font-bold bg-[var(--glass-amber-soft)] text-[var(--glass-amber)] border-[var(--glass-amber-border)]'
  if (answered) return 'border-2 text-sm font-bold bg-[var(--glass-amber)] text-white border-[var(--glass-amber)]'
  return 'border-2 text-sm font-bold bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400 border-2 border-transparent hover:border-slate-300 dark:hover:border-slate-600'
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

    let userMessage = 'Không thể tải nội dung bài thi.'
    if (error?.message?.includes('403') || error?.status === 403) {
      userMessage = 'Bạn không có quyền làm bài thi này.'
    } else if (error?.message?.includes('404') || error?.status === 404) {
      userMessage = 'Bài thi không tồn tại hoặc đã bị xóa.'
    } else if (error?.message?.includes('401') || error?.status === 401) {
      userMessage = 'Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.'
    }
    toast.error(userMessage)
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

/* Circular Timer Styles */
.exam-timer__ring {
  filter: drop-shadow(0 2px 8px var(--glass-amber-glow));
}
.exam-timer .transition-all {
  transition: stroke-dashoffset 1s linear, stroke 0.3s;
}

.exam-timer-progress {
  transition: opacity var(--duration-normal, 250ms) var(--easing-default, ease);
}

.exam-timer--warning {
  accent-color: var(--glass-warning);
}

.exam-timer--danger {
  accent-color: var(--glass-danger);
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
