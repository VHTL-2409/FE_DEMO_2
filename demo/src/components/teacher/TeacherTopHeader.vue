<template>
  <header class="sticky top-0 z-50 w-full bg-white dark:bg-background-dark border-b border-slate-200 dark:border-slate-800">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex justify-between items-center h-16">
        <div class="flex items-center gap-8">
          <div class="flex flex-col">
            <span class="text-primary dark:text-slate-100 text-lg font-bold leading-none">Teacher Dashboard</span>
            <span class="text-slate-500 dark:text-slate-400 text-[10px] font-bold uppercase tracking-widest">
              Online Exam System
            </span>
          </div>
          <nav class="hidden md:flex items-center gap-1">
            <RouterLink :class="navClass('dashboard')" to="/teacher/dashboard">
              Dashboard
            </RouterLink>
            <RouterLink :class="navClass('exam')" to="/teacher/exams">
              Exam Creation
            </RouterLink>
            <RouterLink :class="navClass('monitoring')" to="/teacher/live-monitoring">
              Live Monitoring
            </RouterLink>
          </nav>
        </div>
        <div class="flex items-center gap-4">
          <button class="p-2 text-slate-400 hover:text-primary transition-colors" type="button">
            <span class="material-symbols-outlined">notifications</span>
          </button>
          <button
            type="button"
            @click="goToLogin"
            class="text-xs font-semibold px-3 py-1.5 rounded bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700"
          >
            Sign Out
          </button>
          <div class="flex items-center gap-3 pl-4 border-l border-slate-200 dark:border-slate-800">
            <div class="text-right hidden sm:block">
              <p class="text-sm font-bold">Prof. John Doe</p>
              <p class="text-[10px] text-slate-500 font-medium uppercase">Computer Science</p>
            </div>
            <div class="h-9 w-9 rounded-full bg-primary text-white flex items-center justify-center font-bold text-sm">JD</div>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useRouter, RouterLink } from 'vue-router'

const props = defineProps({
  activeSection: {
    type: String,
    default: 'dashboard'
  }
})

const router = useRouter()

const navClass = (section) => {
  if (props.activeSection === section) {
    return 'px-4 py-2 rounded-lg text-sm text-primary border border-primary/10'
  }

  return 'px-4 py-2 rounded-lg text-sm text-slate-500 dark:text-slate-400 hover:text-primary transition-colors'
}

const goToLogin = () => {
  router.push('/login')
}
</script>
