<template>
  <div class="admin-shell staff-shell-surface dark flex h-dvh max-h-dvh min-h-0 w-full flex-1 flex-col overflow-hidden text-slate-100 selection:bg-indigo-500/20">
    <header class="staff-topbar sticky top-0 z-50 shrink-0">
      <div class="w-full px-4 sm:px-6 lg:px-10">
        <div class="flex min-h-[4.6rem] flex-col justify-center gap-3 py-3">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div class="flex min-w-0 items-center gap-3">
              <div class="staff-brand-mark flex size-11 items-center justify-center rounded-2xl text-sm font-black text-white">
                A
              </div>
              <div class="min-w-0">
                <p class="truncate text-[1.08rem] font-black tracking-[-0.03em] text-slate-900 dark:text-white">EduExam Admin</p>
                <p class="truncate text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500 dark:text-slate-400">
                  Control plane · users · exams · analytics
                </p>
              </div>
            </div>

            <div class="flex items-center gap-2 sm:gap-3">
              <div class="staff-user-pill hidden items-center gap-3 rounded-xl px-3 py-2 sm:flex">
                <div class="text-right">
                  <p class="text-sm font-bold text-slate-900 dark:text-white">Quản trị viên</p>
                  <p class="text-[11px] font-medium uppercase tracking-[0.14em] text-slate-500 dark:text-slate-400">
                    Internal staff
                  </p>
                </div>
                <div class="staff-brand-mark flex size-10 items-center justify-center rounded-2xl text-sm font-black text-white">A</div>
              </div>
              <button
                type="button"
                class="staff-action-btn staff-action-btn-neutral gap-2"
                @click="logout"
              >
                <span class="material-symbols-outlined text-lg">logout</span>
                <span class="hidden sm:inline">Đăng xuất</span>
              </button>
            </div>
          </div>

          <nav class="staff-nav-scroller -mx-1 flex items-center gap-2 overflow-x-auto px-1">
            <RouterLink
              v-for="link in navLinks"
              :key="link.to"
              :to="link.to"
              class="staff-nav-link rounded-xl px-3 py-2 text-sm font-semibold"
              active-class="is-active"
            >
              <span class="inline-flex items-center gap-2">
                <span class="material-symbols-outlined text-[18px]">{{ link.icon }}</span>
                {{ link.label }}
              </span>
            </RouterLink>
          </nav>
        </div>
      </div>
    </header>

    <div class="admin-route-host relative z-10 flex min-h-0 flex-1 flex-col overflow-x-hidden overflow-y-hidden px-4 py-5 sm:px-6 md:py-6 lg:px-10">
      <RouterView />
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { clearAuthSession } from '../../services/authService'

const router = useRouter()

const navLinks = [
  { to: '/admin/dashboard', label: 'Tổng quan', icon: 'dashboard' },
  { to: '/admin/students', label: 'Học sinh', icon: 'school' },
  { to: '/admin/teachers', label: 'Giáo viên', icon: 'co_present' },
  { to: '/admin/admins', label: 'Admin', icon: 'admin_panel_settings' },
  { to: '/admin/exams', label: 'Đề thi', icon: 'quiz' }
]

const logout = () => {
  clearAuthSession()
  router.push('/login')
}

</script>

<style scoped>
.admin-shell {
  font-family: var(--font-sans);
}
</style>
