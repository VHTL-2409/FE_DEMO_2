<template>
  <template v-if="compact">
    <header
      class="app-shell-topbar app-shell-topbar--compact student-exam-topbar sticky top-0 z-50 w-full border-b border-[#dbc2b0]/25 bg-[#faf9f5]/85 px-3 backdrop-blur-xl dark:border-slate-700/50 dark:bg-slate-950/85 sm:px-5 lg:px-6"
    >
      <div
        class="flex w-full flex-nowrap items-center justify-between gap-2 py-2 sm:gap-3 md:min-h-[3.5rem] md:py-2"
      >
        <div class="app-shell-brand min-w-0 max-w-[min(100%,13rem)] shrink-0 sm:max-w-[min(100%,19rem)]">
          <div class="app-shell-brand-mark size-10 shrink-0">
            <span class="material-symbols-outlined text-lg" style="font-variation-settings: 'FILL' 1">school</span>
          </div>
          <div class="min-w-0">
            <p class="truncate text-base font-black tracking-tight text-slate-900 dark:text-white">EduExam Academy</p>
            <p
              v-if="compactExamKicker"
              class="truncate text-[10px] font-semibold uppercase tracking-[0.16em] text-[#887364] dark:text-slate-500"
            >
              {{ compactExamKicker }}
            </p>
            <p
              v-if="compactExamTitle"
              class="stitch-font-headline truncate text-sm font-bold leading-tight text-primary dark:text-amber-300 md:text-base"
            >
              {{ compactExamTitle }}
            </p>
            <p
              v-else
              class="truncate text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-500 dark:text-slate-400"
            >
              Đang làm bài
            </p>
          </div>
        </div>
        <div
          v-if="$slots.compactCenter"
          class="order-last hidden min-w-0 flex-[1_1_100%] justify-center px-1 md:order-none md:flex md:max-w-[min(100%,28rem)] lg:max-w-none lg:flex-[1_1_auto] lg:justify-center"
        >
          <slot name="compactCenter" />
        </div>
        <div
          class="student-exam-topbar-actions flex min-w-0 flex-1 flex-nowrap items-center justify-end gap-1.5 overflow-x-auto overscroll-x-contain sm:gap-2 md:gap-2.5 [scrollbar-width:thin]"
        >
          <slot name="rightActions" />
        </div>
      </div>
      <div class="pointer-events-none h-px w-full bg-gradient-to-r from-transparent via-[#efeeea] to-transparent dark:via-slate-700/60" aria-hidden="true" />
    </header>
  </template>

  <template v-else>
    <header
      v-if="!compact"
      class="student-stitch-portal-topbar fixed left-0 right-0 top-0 z-[45] hidden h-12 items-center border-b border-[#dbc2b0]/35 bg-[#faf9f5]/82 backdrop-blur-xl dark:border-slate-700/50 dark:bg-slate-950/82 md:left-[var(--demo2-sidebar-offset,18rem)] md:flex md:justify-start md:px-6"
      role="banner"
    >
      <div class="flex w-full min-w-0 items-center justify-start gap-3 text-left">
        <div class="min-w-0 text-left">
          <p class="truncate text-[10px] font-bold uppercase tracking-[0.18em] text-[#887364] dark:text-slate-500">EduExam Academy</p>
          <p class="truncate text-sm font-bold leading-tight text-slate-900 dark:text-slate-100">Khu vực học sinh</p>
        </div>
      </div>
      <div
        class="pointer-events-none absolute bottom-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-[#efeeea] to-transparent dark:via-slate-700/60"
        aria-hidden="true"
      />
    </header>

    <button
      type="button"
      class="fixed left-3 top-3 z-[60] inline-flex rounded-xl border border-slate-200/80 bg-white/95 p-2.5 text-slate-700 shadow-sm backdrop-blur-sm md:hidden dark:border-slate-700 dark:bg-slate-900/95"
      aria-label="Mở menu điều hướng"
      @click="mobileNavOpen = true"
    >
      <span class="material-symbols-outlined">menu</span>
    </button>

    <div
      v-if="mobileNavOpen"
      class="fixed inset-0 z-[54] bg-slate-950/40 backdrop-blur-sm md:hidden"
      aria-hidden="true"
      @click="mobileNavOpen = false"
    />

    <aside
      class="app-shell-sidebar bottom-0 top-0 z-[56] w-72 rounded-r-[2rem] py-6 transition-all duration-200 ease-out md:translate-x-0"
      :class="[
        mobileNavOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0',
        isRailMd ? 'md:w-[4.5rem] md:px-2' : 'px-4 md:w-72'
      ]"
    >
      <div
        class="flex gap-2 px-2"
        :class="isRailMd ? 'md:flex-col md:items-center md:gap-3' : 'items-center justify-between'"
      >
        <div class="app-shell-brand min-w-0 flex-1" :class="isRailMd ? 'md:justify-center' : ''">
          <div class="app-shell-brand-mark size-12 shrink-0">
            <span class="material-symbols-outlined text-[26px]" style="font-variation-settings: 'FILL' 1">school</span>
          </div>
          <div class="min-w-0" :class="{ 'md:hidden': isRailMd }">
            <p class="truncate text-lg font-black leading-tight tracking-tight text-[var(--role-primary)]">EduExam Academy</p>
            <p class="text-[10px] font-semibold uppercase tracking-[0.18em] text-slate-500">Digital Archivist Edition</p>
          </div>
        </div>
        <div
          class="flex shrink-0"
          :class="isRailMd ? 'flex-col items-center gap-2' : 'flex-row items-center gap-1.5'"
        >
          <template v-if="!isRailMd">
            <button
              type="button"
              class="inline-flex rounded-xl border border-slate-200/80 bg-white/80 p-2 text-slate-700 shadow-sm transition hover:bg-white dark:border-slate-600 dark:bg-slate-800/80 dark:text-slate-200 dark:hover:bg-slate-800"
              title="Quay lại"
              aria-label="Quay lại"
              @click="goBack"
            >
              <span class="material-symbols-outlined text-[22px]">arrow_back</span>
            </button>
            <button
              type="button"
              class="inline-flex rounded-xl border border-slate-200/80 bg-white/80 p-2 text-slate-700 shadow-sm transition hover:bg-white dark:border-slate-600 dark:bg-slate-800/80 dark:text-slate-200 dark:hover:bg-slate-800"
              title="Thu gọn / đầy đủ menu"
              aria-label="Chuyển giữa đầy đủ tên và chỉ biểu tượng"
              @click="toggleNavDensity"
            >
              <span class="material-symbols-outlined text-[22px]">menu</span>
            </button>
          </template>
          <template v-else>
            <button
              type="button"
              class="inline-flex rounded-xl border border-slate-200/80 bg-white/80 p-2 text-slate-700 shadow-sm transition hover:bg-white dark:border-slate-600 dark:bg-slate-800/80 dark:text-slate-200 dark:hover:bg-slate-800"
              title="Thu gọn / đầy đủ menu"
              aria-label="Chuyển giữa đầy đủ tên và chỉ biểu tượng"
              @click="toggleNavDensity"
            >
              <span class="material-symbols-outlined text-[22px]">menu</span>
            </button>
            <button
              type="button"
              class="inline-flex rounded-xl border border-slate-200/80 bg-white/80 p-2 text-slate-700 shadow-sm transition hover:bg-white dark:border-slate-600 dark:bg-slate-800/80 dark:text-slate-200 dark:hover:bg-slate-800"
              title="Quay lại"
              aria-label="Quay lại"
              @click="goBack"
            >
              <span class="material-symbols-outlined text-[22px]">arrow_back</span>
            </button>
          </template>
        </div>
      </div>

      <nav class="mt-5 flex flex-1 flex-col gap-2">
        <RouterLink
          v-for="item in primaryNav"
          :key="item.section"
          :to="item.to"
          :class="navRowClass(item.section)"
          @click="mobileNavOpen = false"
        >
          <span class="material-symbols-outlined shrink-0 text-[20px]">{{ item.icon }}</span>
          <span v-if="labelFor(item)" class="text-sm font-semibold tracking-tight">{{ labelFor(item) }}</span>
        </RouterLink>
      </nav>

      <div class="mt-auto space-y-2 border-t border-white/50 pt-5 dark:border-slate-700/60">
        <div class="flex flex-wrap items-center gap-1.5">
          <div v-if="showNotifications" class="relative">
            <button
              type="button"
              class="relative inline-flex rounded-xl p-2.5 text-slate-600 transition-colors hover:bg-white/80 hover:text-[var(--role-primary)] dark:hover:bg-slate-800"
              aria-label="Thông báo"
              @click="showNotificationPanel = !showNotificationPanel"
            >
              <span class="material-symbols-outlined text-xl">notifications</span>
              <span
                v-if="hasUnread"
                class="absolute right-1 top-1 size-2 rounded-full border-2 border-white bg-rose-500 dark:border-slate-900"
              />
            </button>
            <div
              v-if="showNotificationPanel"
              class="portal-panel absolute bottom-full left-0 z-50 mb-2 max-h-96 w-[min(20rem,calc(100vw-0.75rem))] overflow-y-auto rounded-[1.25rem] shadow-xl"
            >
              <div class="flex items-center justify-between border-b border-slate-200/70 px-4 py-3 dark:border-slate-700/70">
                <span class="font-bold text-slate-900 dark:text-white">Thông báo</span>
                <button
                  v-if="hasUnread"
                  type="button"
                  class="text-xs font-bold text-[var(--role-primary)] hover:underline"
                  @click="markAllAsRead"
                >
                  Đánh dấu đã đọc
                </button>
              </div>
              <div class="max-h-64 overflow-y-auto">
                <div v-if="!notifications.length" class="p-6 text-center text-sm text-slate-500">Chưa có thông báo.</div>
                <div
                  v-for="n in notifications"
                  :key="n.id"
                  :class="n.read ? 'bg-transparent' : 'bg-primary/5'"
                  class="cursor-pointer border-b border-slate-100 p-4 transition-colors hover:bg-slate-50 dark:border-slate-800 dark:hover:bg-slate-800/50"
                  @click="markAsRead(n.id)"
                >
                  <p class="text-sm font-semibold text-slate-900 dark:text-white">{{ n.title }}</p>
                  <p class="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{{ n.message }}</p>
                </div>
              </div>
            </div>
          </div>
          <RouterLink
            v-if="showHelpLink"
            to="/help-center"
            class="inline-flex rounded-xl p-2.5 text-slate-600 transition-colors hover:bg-white/80 hover:text-[var(--role-primary)] dark:hover:bg-slate-800"
            title="Trợ giúp"
            @click="mobileNavOpen = false"
          >
            <span class="material-symbols-outlined text-xl">help_outline</span>
          </RouterLink>
        </div>
        <div v-if="showNotificationPanel && showNotifications" class="fixed inset-0 z-40 md:hidden" @click="showNotificationPanel = false" />

        <RouterLink to="/student/profile" :class="navRowClass('profile')" @click="mobileNavOpen = false">
          <span class="material-symbols-outlined text-[20px]">account_circle</span>
          <span v-if="labelFor({ label: 'Hồ sơ', short: 'HS' })" class="text-sm font-semibold tracking-tight">{{
            labelFor({ label: 'Hồ sơ', short: 'HS' })
          }}</span>
        </RouterLink>

        <RouterLink
          to="/student/exam-join"
          class="app-shell-cta inline-flex w-full items-center justify-center gap-2 px-4 py-3 text-sm font-bold"
          @click="mobileNavOpen = false"
        >
          <span class="material-symbols-outlined text-[20px]">rocket_launch</span>
          <span v-show="mode !== 'icons'">Vào thi ngay</span>
        </RouterLink>

        <button
          v-if="showSignOut"
          type="button"
          class="w-full rounded-xl border border-slate-200/80 bg-white/60 px-3 py-2.5 text-xs font-semibold text-slate-700 transition hover:bg-white dark:border-slate-700 dark:bg-slate-800/60 dark:text-slate-200"
          @click="goToLogin"
        >
          Đăng xuất
        </button>
      </div>
    </aside>
  </template>
</template>

<script setup>
import { computed, inject, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { clearAuthSession } from '../../services/authService'
import { useNotifications } from '../../composables/useNotifications'
import { useNavLabelMode } from '../../composables/useNavLabelMode'

const { notifications, hasUnread, markAsRead, markAllAsRead } = useNotifications()
const { mode, setMode, labelFor } = useNavLabelMode()

/** Chỉ icon + sidebar hẹp (md+); mobile luôn drawer đủ rộng */
const isRailMd = computed(() => mode.value === 'icons')

const toggleNavDensity = () => {
  if (mode.value === 'icons') {
    setMode('full')
  } else {
    setMode('icons')
  }
}
const showNotificationPanel = ref(false)
const mobileNavOpen = inject('portalMobileNavOpen', ref(false))

const props = defineProps({
  activeSection: {
    type: String,
    default: 'dashboard'
  },
  showSignOut: {
    type: Boolean,
    default: true
  },
  showProfile: {
    type: Boolean,
    default: true
  },
  showNotifications: {
    type: Boolean,
    default: true
  },
  showMenu: {
    type: Boolean,
    default: true
  },
  showHelpLink: {
    type: Boolean,
    default: true
  },
  /** Chế độ compact (làm bài): nhãn phụ, ví dụ "Bài thi" / "Luyện tập" */
  compactExamKicker: {
    type: String,
    default: ''
  },
  /** Chế độ compact: tiêu đề đề (truncate) */
  compactExamTitle: {
    type: String,
    default: ''
  }
})

const compact = computed(() => !props.showMenu)

const router = useRouter()

const primaryNav = [
  { section: 'dashboard', to: '/student/dashboard', icon: 'dashboard', label: 'Bảng điều khiển', short: 'Bảng' },
  { section: 'examJoin', to: '/student/exam-join', icon: 'login', label: 'Thi qua mã', short: 'Mã' },
  { section: 'practice', to: '/student/generate-practice-test', icon: 'model_training', label: 'Luyện tập', short: 'LT' },
  { section: 'history', to: '/student/study-history', icon: 'history_edu', label: 'Lịch sử & kết quả', short: 'LS' }
]

const sideLinkClass = (section) => {
  const active = props.activeSection === section
  const base = 'app-shell-nav-link font-[family-name:Manrope] active:scale-[0.99]'
  if (active) {
    return `${base} is-active`
  }
  return base
}

const navRowClass = (section) => {
  const base = sideLinkClass(section)
  const iconOnly = mode.value === 'icons'
  return [base, iconOnly ? '!justify-center gap-0 px-2' : ''].filter(Boolean).join(' ')
}

const goToLogin = () => {
  clearAuthSession()
  router.push('/login')
}

const goBack = () => {
  if (typeof window !== 'undefined' && window.history.length > 1) {
    router.back()
    return
  }
  router.push('/student/dashboard')
}

</script>
