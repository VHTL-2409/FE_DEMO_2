<template>
  <div class="admin-shell min-h-screen text-slate-100 font-sans selection:bg-teal-500/30">
    <div class="fixed inset-0 pointer-events-none overflow-hidden">
      <div class="absolute inset-0 bg-[#070b12]" />
      <div
        class="absolute inset-0 opacity-[0.35]"
        style="
          background-image:
            linear-gradient(rgba(45, 212, 191, 0.06) 1px, transparent 1px),
            linear-gradient(90deg, rgba(45, 212, 191, 0.06) 1px, transparent 1px);
          background-size: 48px 48px;
        "
      />
      <div class="absolute -top-32 -right-32 w-[520px] h-[520px] rounded-full bg-teal-500/10 blur-[100px]" />
      <div class="absolute bottom-0 left-1/4 w-[420px] h-[420px] rounded-full bg-cyan-600/10 blur-[90px]" />
    </div>

    <div class="relative z-10 max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-10 py-8 pb-16">
      <header class="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6 mb-10">
        <div>
          <p class="text-teal-400/90 text-xs font-semibold tracking-[0.25em] uppercase mb-2">Control plane</p>
          <h1 class="text-2xl sm:text-3xl font-black tracking-tight bg-gradient-to-r from-white via-slate-100 to-slate-400 bg-clip-text text-transparent">
            EduExam Admin
          </h1>
        </div>
        <div class="flex flex-col sm:flex-row sm:items-center gap-3 sm:gap-2">
          <nav class="flex flex-wrap items-center gap-1 sm:gap-2">
            <RouterLink
              v-for="link in navLinks"
              :key="link.to"
              :to="link.to"
              class="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl text-sm font-semibold border border-transparent text-slate-400 hover:text-slate-200 hover:bg-white/5 transition-colors"
              active-class="!border-teal-500/35 !bg-teal-500/10 !text-teal-200"
            >
              <span class="material-symbols-outlined text-[18px]">{{ link.icon }}</span>
              {{ link.label }}
            </RouterLink>
          </nav>
          <button
            type="button"
            @click="logout"
            class="inline-flex items-center justify-center gap-2 px-4 py-2 rounded-xl bg-teal-500/15 border border-teal-500/30 text-teal-300 hover:bg-teal-500/25 text-sm font-semibold sm:ml-2"
          >
            <span class="material-symbols-outlined text-lg">logout</span>
            Đăng xuất
          </button>
        </div>
      </header>

      <RouterView />
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
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

onMounted(() => {
  document.documentElement.classList.add('dark')
})

onUnmounted(() => {
  document.documentElement.classList.remove('dark')
})
</script>

<style scoped>
.admin-shell {
  font-family: 'Inter', system-ui, sans-serif;
}
</style>
