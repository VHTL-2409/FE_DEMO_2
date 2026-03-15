<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen"
  >
    <div class="relative flex min-h-screen w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full grow flex-col">
        <header
          class="flex items-center justify-between whitespace-nowrap border-b border-solid border-primary/10 bg-white/80 dark:bg-background-dark/80 backdrop-blur-md px-6 md:px-10 py-3 sticky top-0 z-50"
        >
          <div class="flex items-center gap-4 text-primary dark:text-slate-100">
            <div class="size-8 flex items-center justify-center bg-primary text-white rounded-lg">
              <span class="material-symbols-outlined">menu_book</span>
            </div>
            <h2
              class="text-primary dark:text-slate-100 text-lg font-bold leading-tight tracking-[-0.015em]"
            >
              EduExam System
            </h2>
          </div>
          <button
            type="button"
            class="flex min-w-[84px] cursor-pointer items-center justify-center overflow-hidden rounded-lg h-10 px-4 bg-primary text-white text-sm font-bold leading-normal tracking-[0.015em] hover:bg-primary/90 transition-colors"
          >
            <span class="truncate">Help Center</span>
          </button>
        </header>

        <main
          class="flex-1 relative flex items-center justify-center p-4 md:p-8 lg:p-12 bg-gradient-to-br from-primary/5 via-background-light to-primary/10 dark:from-background-dark dark:via-background-dark dark:to-primary/20 overflow-hidden"
        >
          <div class="pointer-events-none absolute -top-20 -left-16 size-72 rounded-full bg-primary/20 blur-3xl animate-float-slow"></div>
          <div class="pointer-events-none absolute -bottom-24 -right-16 size-80 rounded-full bg-primary/15 blur-3xl animate-float-delay"></div>
          <div
            class="w-full max-w-[1000px] grid grid-cols-1 lg:grid-cols-2 bg-white dark:bg-slate-900/50 rounded-xl overflow-hidden shadow-2xl border border-primary/10 animate-fade-up"
          >
            <div class="p-8 md:p-12 flex flex-col justify-center">
              <div class="mb-8">
                <h1
                  class="text-slate-900 dark:text-slate-100 text-3xl font-black leading-tight tracking-tight mb-2"
                >
                  Login &amp; Role Selection
                </h1>
                <p class="text-slate-500 dark:text-slate-400 text-base font-normal">
                  Welcome back! Please enter your details.
                </p>
              </div>

              <div class="mb-6">
                <p class="text-slate-900 dark:text-slate-100 text-sm font-semibold mb-3">
                  Select your role
                </p>
                <div
                  class="flex h-12 w-full items-center justify-center rounded-lg bg-slate-100 dark:bg-slate-800 p-1"
                >
                  <label
                    class="flex cursor-pointer h-full grow items-center justify-center overflow-hidden rounded-lg px-2 has-[:checked]:bg-white dark:has-[:checked]:bg-slate-700 has-[:checked]:shadow-sm has-[:checked]:text-primary dark:has-[:checked]:text-white text-slate-500 text-sm font-medium transition-all"
                  >
                    <span class="truncate flex items-center gap-2">
                      <span class="material-symbols-outlined text-sm">school</span>
                      Student
                    </span>
                    <input v-model="role" class="hidden" name="role" type="radio" value="Student" />
                  </label>
                  <label
                    class="flex cursor-pointer h-full grow items-center justify-center overflow-hidden rounded-lg px-2 has-[:checked]:bg-white dark:has-[:checked]:bg-slate-700 has-[:checked]:shadow-sm has-[:checked]:text-primary dark:has-[:checked]:text-white text-slate-500 text-sm font-medium transition-all"
                  >
                    <span class="truncate flex items-center gap-2">
                      <span class="material-symbols-outlined text-sm">co_present</span>
                      Teacher
                    </span>
                    <input v-model="role" class="hidden" name="role" type="radio" value="Teacher" />
                  </label>
                </div>
              </div>

              <form class="space-y-5 animate-fade-up-delay" @submit.prevent="onSubmit">
                <div class="flex flex-col gap-2">
                  <label class="text-slate-700 dark:text-slate-300 text-sm font-medium">Username</label>
                  <div class="relative">
                    <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                      >person</span
                    >
                    <input
                      v-model="username"
                      class="w-full pl-10 pr-4 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all placeholder:text-slate-400"
                      placeholder="Enter your username"
                      type="text"
                    />
                  </div>
                </div>

                <div class="flex flex-col gap-2">
                  <div class="flex justify-between items-center">
                    <label class="text-slate-700 dark:text-slate-300 text-sm font-medium">Password</label>
                    <a class="text-primary text-xs font-bold hover:underline" href="#">Forgot Password?</a>
                  </div>
                  <div class="relative flex items-center">
                    <span class="material-symbols-outlined absolute left-3 text-slate-400">lock</span>
                    <input
                      v-model="password"
                      class="w-full pl-10 pr-12 py-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all placeholder:text-slate-400"
                      placeholder="Enter your password"
                      :type="showPassword ? 'text' : 'password'"
                    />
                    <button
                      type="button"
                      class="absolute right-3 text-slate-400 hover:text-primary transition-colors"
                      @click="showPassword = !showPassword"
                    >
                      <span class="material-symbols-outlined">
                        {{ showPassword ? 'visibility_off' : 'visibility' }}
                      </span>
                    </button>
                  </div>
                </div>

                <div class="flex items-center gap-2">
                  <input
                    id="remember"
                    v-model="remember"
                    class="rounded border-slate-300 text-primary focus:ring-primary h-4 w-4"
                    type="checkbox"
                  />
                  <label class="text-slate-600 dark:text-slate-400 text-sm" for="remember"
                    >Remember me for 30 days</label
                  >
                </div>


                <button
                  type="submit"
                  :disabled="isSubmitting"
                  class="w-full py-3 px-4 bg-primary text-white rounded-lg font-bold text-base hover:bg-primary/90 hover:-translate-y-0.5 active:translate-y-0 shadow-lg shadow-primary/20 transition-all duration-200 flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:translate-y-0"
                >
                  <span>{{ isSubmitting ? 'Signing In...' : 'Sign In' }}</span>
                  <span class="material-symbols-outlined text-sm">login</span>
                </button>
              </form>

              <div class="mt-8 text-center animate-fade-in">
                <p class="text-slate-500 dark:text-slate-400 text-sm">
                  Don't have an account?
                  <button
                    type="button"
                    class="text-primary font-bold hover:underline ml-1"
                    @click="goToRegister"
                  >
                    Create an Account
                  </button>
                </p>
              </div>
            </div>

            <div class="hidden lg:flex relative bg-primary flex-col justify-center items-center p-12 overflow-hidden">
              <div class="absolute inset-0 opacity-10 pointer-events-none grid grid-cols-4 gap-8 p-12">
                <span class="material-symbols-outlined text-8xl text-white">calculate</span>
                <span class="material-symbols-outlined text-8xl text-white">science</span>
                <span class="material-symbols-outlined text-8xl text-white">history_edu</span>
                <span class="material-symbols-outlined text-8xl text-white">language</span>
                <span class="material-symbols-outlined text-8xl text-white">functions</span>
                <span class="material-symbols-outlined text-8xl text-white">biotech</span>
                <span class="material-symbols-outlined text-8xl text-white">terminal</span>
                <span class="material-symbols-outlined text-8xl text-white">psychology</span>
              </div>
              <div class="relative z-10 text-center text-white max-w-sm">
                <div
                  class="size-20 bg-white/20 rounded-2xl flex items-center justify-center mx-auto mb-8 backdrop-blur-sm border border-white/30"
                >
                  <span class="material-symbols-outlined text-4xl text-white">auto_awesome</span>
                </div>
                <h2 class="text-3xl font-bold mb-4">Empowering the Future of Assessment</h2>
                <p class="text-white/80 leading-relaxed">
                  Secure, accessible, and intuitive online examination platform designed for global excellence in
                  education.
                </p>
                <div class="mt-12 grid grid-cols-2 gap-4">
                  <div class="bg-white/10 p-4 rounded-xl backdrop-blur-sm border border-white/10">
                    <p class="text-2xl font-bold">99.9%</p>
                    <p class="text-xs text-white/60 uppercase tracking-widest">Uptime</p>
                  </div>
                  <div class="bg-white/10 p-4 rounded-xl backdrop-blur-sm border border-white/10">
                    <p class="text-2xl font-bold">Secure</p>
                    <p class="text-xs text-white/60 uppercase tracking-widest">Proctoring</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </main>

        <footer class="p-6 text-center text-slate-500 dark:text-slate-400 text-xs">
          <div class="flex justify-center gap-6 mb-4">
            <a class="hover:text-primary transition-colors" href="#">Privacy Policy</a>
            <a class="hover:text-primary transition-colors" href="#">Terms of Service</a>
            <a class="hover:text-primary transition-colors" href="#">Support</a>
          </div>
          <p>© 2024 EduExam Online Examination System. All rights reserved.</p>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../../services/authService'
import { useToast } from '../../composables/useToast'

const router = useRouter()

const isDark = ref(false)
const role = ref('Student')
const username = ref('')
const password = ref('')
const remember = ref(false)
const showPassword = ref(false)
const isSubmitting = ref(false)

const toast = useToast()

const goToRegister = () => {
  router.push('/register')
}

const routeByRole = (selectedRole) => {
  const normalizedRole = String(selectedRole || '').toLowerCase()
  if (normalizedRole === 'teacher') {
    router.push('/teacher/dashboard')
    return
  }

  router.push('/student/dashboard')
}

const onSubmit = async () => {
  if (!username.value || !password.value) {
    toast.error('Please enter username and password.')
    return
  }

  isSubmitting.value = true

  try {
    await login({
      username: username.value.trim(),
      password: password.value
    })

    routeByRole(role.value)
  } catch (error) {
    toast.error('Login failed. Please try again.')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800;900&display=swap');
@import url('https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap');

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

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
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

.animate-fade-in {
  animation: fadeIn 0.8s ease-out;
}

.animate-float-slow {
  animation: floatSlow 7s ease-in-out infinite;
}

.animate-float-delay {
  animation: floatDelay 8s ease-in-out infinite;
}
</style>
