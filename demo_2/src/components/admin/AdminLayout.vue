<template>
  <div
    class="dark admin-shell admin-portal-stitch font-display flex h-dvh max-h-dvh min-h-0 w-full flex-1 overflow-hidden text-slate-100 selection:bg-primary-500/25"
  >
    <button
      type="button"
      class="fixed left-3 top-3 z-[60] inline-flex rounded-xl border border-slate-600/80 bg-slate-800/95 p-2.5 text-slate-200 shadow-md backdrop-blur-sm transition hover:bg-slate-700 md:hidden"
      aria-label="Mở menu"
      @click="mobileNavOpen = !mobileNavOpen"
    >
      <span class="material-symbols-outlined">menu</span>
    </button>
    <div
      v-if="mobileNavOpen"
      class="fixed inset-0 z-40 bg-slate-950/40 backdrop-blur-sm md:hidden"
      aria-hidden="true"
      @click="mobileNavOpen = false"
    />

    <aside
      class="admin-sidebar-stitch flex w-[min(17rem,88vw)] shrink-0 flex-col border-r border-white/[0.06] bg-[#0a0a09] px-3 py-6 transition-transform duration-300 ease-out max-md:fixed max-md:inset-y-0 max-md:left-0 max-md:z-50 md:w-64 md:rounded-none"
      :class="mobileNavOpen ? 'max-md:translate-x-0' : 'max-md:-translate-x-full md:translate-x-0'"
    >
      <div class="mb-8 flex items-center gap-3 px-2">
        <div
          class="staff-brand-mark flex size-11 shrink-0 items-center justify-center rounded-[1rem] text-sm font-black text-white shadow-md"
        >
          A
        </div>
        <div class="min-w-0">
          <p class="truncate font-serif text-lg font-bold italic tracking-tight text-amber-100">EduExam Admin</p>
          <p class="truncate font-sans text-[10px] font-semibold uppercase tracking-[0.18em] text-slate-500">
            Quản trị hệ thống
          </p>
        </div>
      </div>

      <nav class="flex flex-1 flex-col gap-1.5">
        <RouterLink
          v-for="link in navLinks"
          :key="link.to"
          :to="link.to"
          class="admin-stitch-nav-link flex items-center gap-3 rounded-lg px-3 py-3 text-[11px] font-semibold uppercase tracking-widest"
          active-class="is-active"
          @click="mobileNavOpen = false"
        >
          <span class="material-symbols-outlined shrink-0 text-[20px]">{{ link.icon }}</span>
          <span class="leading-snug">{{ link.label }}</span>
        </RouterLink>
      </nav>

      <button
        type="button"
        class="mt-auto flex items-center justify-center gap-2 rounded-lg px-3 py-3 text-[11px] font-semibold uppercase tracking-widest text-red-400/90 transition hover:bg-red-500/10 hover:text-red-400"
        @click="logout"
      >
        <span class="material-symbols-outlined text-lg">logout</span>
        Đăng xuất
      </button>
    </aside>

    <div
      class="admin-route-host portal-page-scroll portal-scrollbar relative z-10 flex min-h-0 min-w-0 flex-1 flex-col overflow-auto bg-[#0f172a] px-4 py-6 pt-14 sm:px-6 md:py-8 md:pt-8 lg:px-10"
    >
      <RouterView v-slot="{ Component }">
        <transition name="admin-view" mode="out-in">
          <component :is="Component" :key="route.fullPath" />
        </transition>
      </RouterView>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter, RouterLink, RouterView } from 'vue-router'
import { clearAuthSession } from '../../services/authService'

const route = useRoute()
const router = useRouter()
const mobileNavOpen = ref(false)

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
