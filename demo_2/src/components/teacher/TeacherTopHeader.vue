<template>
  <button
    type="button"
    class="fixed left-3 top-14 z-[60] inline-flex rounded-xl border border-slate-200/80 bg-white/95 p-2.5 text-slate-700 shadow-sm backdrop-blur-sm md:hidden dark:border-slate-700 dark:bg-slate-900/95"
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
    class="app-shell-sidebar bottom-0 top-0 z-[56] w-64 rounded-none py-4 transition-all duration-200 ease-out md:translate-x-0"
    :class="[
      mobileNavOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0',
      isRailMd ? 'md:w-[4.5rem] md:px-2' : 'px-3 md:w-64'
    ]"
  >
    <div class="flex flex-col gap-4 px-1 pt-1">
      <div class="app-shell-brand min-w-0" :class="isRailMd ? 'md:justify-center' : ''">
        <div class="app-shell-brand-mark size-10 shrink-0">
          <span class="material-symbols-outlined text-[22px]" style="font-variation-settings: 'FILL' 1">school</span>
        </div>
        <div class="min-w-0" :class="{ 'md:hidden': isRailMd }">
          <p class="truncate font-serif text-lg font-bold tracking-tight text-amber-900 dark:text-amber-100">EduExam</p>
          <p class="text-[11px] font-medium uppercase tracking-wider text-slate-500">Khu vực giảng viên</p>
        </div>
      </div>
      <div
        class="mt-1 flex shrink-0 justify-center gap-2 border-t border-[color:rgba(219,194,176,0.35)] pt-3 md:justify-start dark:border-slate-600/50"
        :class="isRailMd ? 'md:flex-col md:items-center md:border-t-0 md:pt-0' : ''"
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
            title="Chỉ hiển thị biểu tượng"
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
            title="Hiển thị đầy đủ tên mục"
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

    <nav class="mt-4 flex flex-1 flex-col gap-1.5">
      <RouterLink
        v-for="item in primaryNav"
        :key="item.section"
        :to="item.to"
        :class="navRowClass(item.section)"
        @click="mobileNavOpen = false"
      >
        <span class="material-symbols-outlined shrink-0 text-[20px]">{{ item.icon }}</span>
        <span
          v-if="labelFor(item)"
          class="leading-snug text-current"
          :class="
            mode === 'full'
              ? 'text-[11px] font-semibold uppercase tracking-wider'
              : 'text-xs font-semibold tracking-tight'
          "
        >{{ labelFor(item) }}</span>
      </RouterLink>
    </nav>

    <div class="mt-auto space-y-2 border-t border-white/50 pt-4 dark:border-slate-700/60">
      <div class="flex flex-wrap gap-1.5">
        <div class="relative">
          <button
            type="button"
            class="relative inline-flex rounded-xl p-2.5 text-slate-600 transition-colors hover:bg-white/80 hover:text-[var(--role-primary)] dark:hover:bg-slate-800"
            aria-label="Thông báo"
            @click="showNotifications = !showNotifications"
          >
            <span class="material-symbols-outlined text-xl">notifications</span>
            <span
              v-if="hasUnread"
              class="absolute right-1 top-1 size-2 rounded-full border-2 border-white bg-rose-500 dark:border-slate-900"
            />
          </button>
          <div
            v-if="showNotifications"
            class="portal-panel absolute bottom-full left-0 z-50 mb-2 max-h-96 w-80 overflow-y-auto rounded-[1.25rem] shadow-xl"
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
                :class="n.read ? 'bg-transparent' : 'bg-[var(--role-primary-soft)]'"
                class="cursor-pointer border-b border-slate-100/80 p-4 transition-colors hover:bg-slate-50 dark:border-slate-800 dark:hover:bg-slate-800/60"
                @click="markAsRead(n.id)"
              >
                <p class="text-sm font-semibold text-slate-900 dark:text-white">{{ n.title }}</p>
                <p class="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{{ n.message }}</p>
              </div>
            </div>
          </div>
        </div>
        <RouterLink
          to="/help-center?from=teacher"
          class="inline-flex rounded-xl p-2.5 text-slate-600 transition-colors hover:bg-white/80 hover:text-[var(--role-primary)] dark:hover:bg-slate-800"
          title="Trợ giúp"
          @click="mobileNavOpen = false"
        >
          <span class="material-symbols-outlined text-xl">help_outline</span>
        </RouterLink>
      </div>
      <div v-if="showNotifications" class="fixed inset-0 z-40 md:hidden" @click="showNotifications = false" />

      <RouterLink to="/teacher/profile" :class="navRowClass('profile')" @click="mobileNavOpen = false">
        <span class="material-symbols-outlined text-[20px]">account_circle</span>
        <span
          v-if="labelFor({ label: 'Hồ sơ', short: 'HS' })"
          class="leading-snug text-current"
          :class="
            mode === 'full'
              ? 'text-[11px] font-semibold uppercase tracking-wider'
              : 'text-xs font-semibold tracking-tight'
          "
        >{{ labelFor({ label: 'Hồ sơ', short: 'HS' }) }}</span>
      </RouterLink>

      <button
        type="button"
        class="w-full rounded-xl border border-slate-200/80 bg-white/60 px-3 py-2.5 text-xs font-semibold text-slate-700 transition hover:bg-white dark:border-slate-700 dark:bg-slate-800/60 dark:text-slate-200"
        @click="goToLogin"
      >
        Đăng xuất
      </button>
    </div>
  </aside>
</template>

<script setup>
import { computed, inject, ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { clearAuthSession } from '../../services/authService'
import { useNotifications } from '../../composables/useNotifications'
import { useNavLabelMode } from '../../composables/useNavLabelMode'

const { notifications, hasUnread, markAsRead, markAllAsRead } = useNotifications()
const { mode, setMode, labelFor } = useNavLabelMode()

const isRailMd = computed(() => mode.value === 'icons')

const toggleNavDensity = () => {
  if (mode.value === 'icons') {
    setMode('full')
  } else {
    setMode('icons')
  }
}
const showNotifications = ref(false)
const mobileNavOpen = inject('portalMobileNavOpen', ref(false))

const props = defineProps({
  activeSection: {
    type: String,
    default: 'dashboard'
  }
})

const router = useRouter()

const primaryNav = [
  { section: 'dashboard', to: '/teacher/dashboard', icon: 'dashboard', label: 'Trang chủ', short: 'TC' },
  { section: 'exam', to: '/teacher/exams', icon: 'quiz', label: 'Ngân hàng đề', short: 'Đề' },
  { section: 'exam-list', to: '/teacher/exams/list', icon: 'list_alt', label: 'Danh sách đề', short: 'DS' },
  { section: 'monitoring', to: '/teacher/live-monitoring', icon: 'live_tv', label: 'Giám sát', short: 'GS' },
  { section: 'review', to: '/teacher/exams/review/summary', icon: 'assessment', label: 'Báo cáo', short: 'BC' }
]

const sideLinkClass = (section) => {
  const active = props.activeSection === section
  const base = 'app-shell-nav-link active:scale-[0.99]'
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
  router.push('/teacher/dashboard')
}

</script>
