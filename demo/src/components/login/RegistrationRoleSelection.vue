<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen"
  >
    <div class="relative flex min-h-screen w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full grow flex-col">
        <header
          class="flex items-center justify-between border-b border-primary/10 px-6 py-4 lg:px-40 bg-white dark:bg-background-dark"
        >
          <div class="flex items-center gap-3 text-primary">
            <div class="size-8 flex items-center justify-center bg-primary rounded-lg text-white">
              <span class="material-symbols-outlined text-xl">school</span>
            </div>
            <h2 class="text-slate-900 dark:text-slate-100 text-xl font-bold leading-tight tracking-tight">EduPlatform</h2>
          </div>
          <div class="flex items-center gap-4">
            <span class="text-sm text-slate-500 hidden sm:block">Already have an account?</span>
            <button
              type="button"
              @click="goToLogin"
              class="flex min-w-[84px] cursor-pointer items-center justify-center rounded-lg h-10 px-5 border-2 border-primary text-primary text-sm font-bold hover:bg-primary/5 transition-colors"
            >
              Sign In
            </button>
          </div>
        </header>

        <main class="flex-1 relative flex items-center justify-center p-6 lg:p-12 overflow-hidden">
          <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
          <div class="pointer-events-none absolute -bottom-24 -right-12 size-72 rounded-full bg-primary/15 blur-3xl animate-float-delay"></div>
          <div
            class="w-full max-w-[560px] bg-white dark:bg-slate-900/50 rounded-xl shadow-xl border border-primary/5 p-8 lg:p-12 animate-fade-up"
          >
            <div class="text-center mb-10">
              <h1 class="text-slate-900 dark:text-slate-100 text-4xl font-black leading-tight tracking-tight mb-2">
                Tạo Tài Khoản
              </h1>
              <p class="text-slate-500 dark:text-slate-400 text-lg">---------------------------</p>
            </div>

            <form class="space-y-8 animate-fade-up-delay" @submit.prevent="onSubmit">
              <div v-if="errorMessage" class="rounded-xl border border-rose-200 bg-rose-50 text-rose-700 text-sm font-semibold px-4 py-3">
                {{ errorMessage }}
              </div>
              <div class="space-y-5">
                <div class="flex flex-col gap-1.5">
                  <label class="text-slate-700 dark:text-slate-300 text-sm font-semibold">Username</label>
                  <div class="relative">
                    <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <span class="material-symbols-outlined text-slate-400 text-xl">person</span>
                    </div>
                    <input
                      v-model="username"
                      class="block w-full pl-11 pr-4 py-3.5 bg-background-light dark:bg-slate-800 border-none rounded-lg focus:ring-2 focus:ring-primary text-slate-900 dark:text-slate-100 placeholder:text-slate-400"
                      placeholder="janedoe"
                      type="text"
                    />
                  </div>
                </div>
                <div class="flex flex-col gap-1.5">
                  <label class="text-slate-700 dark:text-slate-300 text-sm font-semibold">Email Address</label>
                  <div class="relative">
                    <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <span class="material-symbols-outlined text-slate-400 text-xl">mail</span>
                    </div>
                    <input
                      v-model="email"
                      class="block w-full pl-11 pr-4 py-3.5 bg-background-light dark:bg-slate-800 border-none rounded-lg focus:ring-2 focus:ring-primary text-slate-900 dark:text-slate-100 placeholder:text-slate-400"
                      placeholder="jane@university.edu"
                      type="email"
                    />
                  </div>
                </div>
                <div class="flex flex-col gap-1.5">
                  <label class="text-slate-700 dark:text-slate-300 text-sm font-semibold">Password</label>
                  <div class="relative">
                    <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                      <span class="material-symbols-outlined text-slate-400 text-xl">lock</span>
                    </div>
                    <input
                      v-model="password"
                      class="block w-full pl-11 pr-4 py-3.5 bg-background-light dark:bg-slate-800 border-none rounded-lg focus:ring-2 focus:ring-primary text-slate-900 dark:text-slate-100 placeholder:text-slate-400"
                      placeholder="••••••••"
                      type="password"
                    />
                  </div>
                  <p class="text-xs text-slate-500 mt-1">Must be at least 8 characters long.</p>
                </div>
              </div>


              <button
                :disabled="isSubmitting"
                class="w-full flex items-center justify-center bg-primary hover:bg-primary/90 hover:-translate-y-0.5 active:translate-y-0 text-white font-bold py-4 px-6 rounded-lg transition-all duration-200 shadow-lg shadow-primary/20 disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:translate-y-0"
                type="submit"
              >
                {{ isSubmitting ? 'Creating Account...' : 'Create Account' }}
                <span class="material-symbols-outlined ml-2 text-xl">arrow_forward</span>
              </button>
              <p class="text-center text-xs text-slate-500 leading-relaxed">
                By clicking "Create Account", you agree to our
                <a class="text-primary hover:underline font-semibold" href="#">Terms of Service</a> and
                <a class="text-primary hover:underline font-semibold" href="#">Privacy Policy</a>.
              </p>
            </form>
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
import { register } from '../../services/authService'
import { useToast } from '../../composables/useToast'

const router = useRouter()

const isDark = ref(false)
const username = ref('')
const email = ref('')
const password = ref('')
const isSubmitting = ref(false)
const errorMessage = ref('')

const toast = useToast()

const goToLogin = () => {
  router.push('/login')
}

const onSubmit = async () => {
  if (!username.value || !email.value || !password.value) {
    toast.error('Please fill in all required fields.')
    return
  }

  isSubmitting.value = true
  errorMessage.value = ''

  try {
    const data = await register({
      username: username.value.trim(),
      email: email.value.trim(),
      password: password.value
    })

    if (data?.verificationPending) {
      errorMessage.value = ''
      const token = data?.verificationUrl ? data.verificationUrl.replace(/^.*token=/, '') : ''
      router.push({
        path: '/verify-email-pending',
        query: {
          email: email.value.trim(),
          emailSent: data?.emailSent ? 'true' : 'false',
          token: token,
          verificationUrl: data?.verificationUrl || ''
        }
      })
    } else if (data?.token) {
      router.push('/select-role')
    } else {
      router.push('/login')
    }
  } catch (error) {
    if (error?.status === 409) {
      errorMessage.value = error?.payload?.message || 'Tài khoản hoặc email đã tồn tại.'
      toast.error(errorMessage.value)
    } else {
      toast.error('Registration failed. Please try again.')
    }
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

.animate-fade-up-delay {
  animation: fadeUp 0.65s ease-out;
}

.animate-float-slow {
  animation: floatSlow 7s ease-in-out infinite;
}

.animate-float-delay {
  animation: floatDelay 8s ease-in-out infinite;
}
</style>