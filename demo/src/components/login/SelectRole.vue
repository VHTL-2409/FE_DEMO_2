<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <div class="relative flex min-h-screen w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full grow flex-col">
        <header class="flex items-center justify-between border-b border-primary/10 px-6 py-4 lg:px-40 bg-white dark:bg-background-dark">
          <div class="flex items-center gap-3 text-primary">
            <div class="size-8 flex items-center justify-center bg-primary rounded-lg text-white">
              <span class="material-symbols-outlined text-xl">school</span>
            </div>
            <h2 class="text-slate-900 dark:text-slate-100 text-xl font-bold leading-tight tracking-tight">EduPlatform</h2>
          </div>
        </header>

        <main class="flex-1 relative flex items-center justify-center p-6 lg:p-12 overflow-hidden">
          <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
          <div class="pointer-events-none absolute -bottom-24 -right-12 size-72 rounded-full bg-primary/15 blur-3xl animate-float-delay"></div>
          <div class="w-full max-w-[640px] bg-white dark:bg-slate-900/50 rounded-xl shadow-xl border border-primary/5 p-8 lg:p-12 animate-fade-up">
            <div class="text-center mb-8">
              <h1 class="text-slate-900 dark:text-slate-100 text-3xl font-black leading-tight tracking-tight mb-2">Chọn vai trò</h1>
              <p class="text-slate-500 dark:text-slate-400 text-base">Mỗi tài khoản chỉ chọn 1 lần.</p>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
              <button
                type="button"
                :disabled="isSubmitting"
                @click="selectRole('STUDENT')"
                class="group rounded-2xl border border-slate-200 dark:border-slate-700 p-6 text-left hover:border-primary/40 hover:shadow-lg transition-all disabled:opacity-60"
              >
                <div class="size-12 rounded-xl bg-primary/10 text-primary flex items-center justify-center mb-4">
                  <span class="material-symbols-outlined text-2xl">school</span>
                </div>
                <h3 class="text-lg font-bold mb-2">Học sinh</h3>
                <p class="text-sm text-slate-500 dark:text-slate-400">Làm bài thi, luyện tập và xem kết quả.</p>
              </button>

              <button
                type="button"
                :disabled="isSubmitting"
                @click="selectRole('TEACHER')"
                class="group rounded-2xl border border-slate-200 dark:border-slate-700 p-6 text-left hover:border-primary/40 hover:shadow-lg transition-all disabled:opacity-60"
              >
                <div class="size-12 rounded-xl bg-primary/10 text-primary flex items-center justify-center mb-4">
                  <span class="material-symbols-outlined text-2xl">co_present</span>
                </div>
                <h3 class="text-lg font-bold mb-2">Giáo viên</h3>
                <p class="text-sm text-slate-500 dark:text-slate-400">Tạo đề thi, quản lý kỳ thi và giám sát.</p>
              </button>
            </div>

            <div v-if="errorMessage" class="mt-6 rounded-xl border border-rose-200 bg-rose-50 text-rose-700 text-sm font-semibold px-4 py-3">
              {{ errorMessage }}
            </div>

            <div class="mt-8 text-center text-xs text-slate-400">Nếu bạn đã chọn role, vui lòng liên hệ quản trị để thay đổi.</div>
          </div>
        </main>
        <footer class="py-8 px-6 text-center text-slate-400 text-sm">© 2024 EduPlatform Inc. All rights reserved.</footer>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { assignRole, redirectToSiteByDatabaseRole } from '../../services/authService'
import { useToast } from '../../composables/useToast'

const router = useRouter()
const toast = useToast()

const isDark = ref(false)
const isSubmitting = ref(false)
const errorMessage = ref('')

const selectRole = async (role) => {
  if (isSubmitting.value) return
  errorMessage.value = ''
  isSubmitting.value = true

  try {
    await assignRole(role)
    await redirectToSiteByDatabaseRole(router)
  } catch (error) {
    errorMessage.value = 'Không thể cập nhật role. Vui lòng thử lại.'
    toast.error(errorMessage.value)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.font-display {
  font-family: 'Inter', sans-serif;
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

.animate-float-slow {
  animation: floatSlow 7s ease-in-out infinite;
}

.animate-float-delay {
  animation: floatDelay 8s ease-in-out infinite;
}
</style>
