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
          class="flex-1 relative flex items-center justify-center p-4 md:p-8 lg:p-12 bg-gradient-to-br from-slate-50 via-white to-primary-50/50 dark:from-slate-950 dark:via-background-dark dark:to-primary-900/20 overflow-hidden"
        >
          <div class="pointer-events-none absolute -top-20 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
          <div class="pointer-events-none absolute -bottom-24 -right-16 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>
          <div
            class="w-full max-w-[1000px] grid grid-cols-1 lg:grid-cols-2 bg-white dark:bg-slate-900/80 backdrop-blur-sm rounded-2xl overflow-hidden shadow-xl border border-slate-200/80 dark:border-slate-800/80 animate-fade-up"
          >
            <div class="p-8 md:p-12 flex flex-col justify-center">
              <div class="mb-8">
                <h1
                  class="text-slate-900 dark:text-slate-100 text-3xl font-black leading-tight tracking-tight mb-2 text-center"
                >
                  Đăng nhập
                </h1>
                <p class="text-slate-500 dark:text-slate-400 text-base font-normal text-center">
                  Đăng nhập để truy cập hệ thống thi trực tuyến
                </p>
              </div>


              <div v-if="emailNotVerified" class="mb-4 p-4 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800">
                <p class="text-amber-800 dark:text-amber-200 text-sm font-medium mb-2">Email chưa được xác minh. Vui lòng kiểm tra hộp thư hoặc gửi lại link xác minh.</p>
                <div class="flex gap-2">
                  <input
                    v-model="resendEmail"
                    type="email"
                    placeholder="Email đăng ký"
                    class="flex-1 px-3 py-2 rounded-lg border border-amber-200 dark:border-amber-700 bg-white dark:bg-slate-800 text-sm"
                  />
                  <button
                    type="button"
                    :disabled="isResending"
                    @click="onResendVerification"
                    class="px-4 py-2 bg-amber-600 text-white rounded-lg text-sm font-medium hover:bg-amber-700 disabled:opacity-60"
                  >
                    {{ isResending ? 'Đang gửi...' : 'Gửi lại' }}
                  </button>
                </div>
                <p v-if="resendMessage" class="mt-2 text-sm" :class="resendSuccess ? 'text-green-600 dark:text-green-400' : 'text-rose-600 dark:text-rose-400'">
                  <template v-if="resendSuccess && resendVerificationUrl">
                    Chế độ demo: <RouterLink :to="{ path: '/verify-email', query: { token: resendVerificationToken } }" class="font-bold underline hover:no-underline">Nhấp vào đây để xác minh</RouterLink>
                  </template>
                  <template v-else>{{ resendMessage }}</template>
                </p>
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
                    <RouterLink to="/forgot-password" class="text-primary text-xs font-bold hover:underline">Quên mật khẩu?</RouterLink>
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
                    >Remember me </label
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

            <div class="hidden lg:flex relative bg-gradient-to-br from-primary to-primary-800 flex-col justify-center items-center p-12 overflow-hidden">
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
                  class="size-20 bg-white/20 rounded-2xl flex items-center justify-center mx-auto mb-8 backdrop-blur-sm border border-white/30 shadow-xl"
                >
                  <span class="material-symbols-outlined text-4xl text-white">school</span>
                </div>
                <h2 class="text-3xl font-bold mb-4">EduExam</h2>
                <p class="text-white/80 leading-relaxed">
                  Hệ thống thi trực tuyến
                </p>
                <div class="mt-12 grid grid-cols-2 gap-4">
                  <div class="bg-white/10 p-4 rounded-xl backdrop-blur-sm border border-white/20">
                    <p class="text-2xl font-bold">CSV</p>
                    <p class="text-xs text-white/60 uppercase tracking-widest">Import đề thi</p>
                  </div>
                  <div class="bg-white/10 p-4 rounded-xl backdrop-blur-sm border border-white/20">
                    <p class="text-2xl font-bold">Live</p>
                    <p class="text-xs text-white/60 uppercase tracking-widest">Giám sát</p>
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
import { login, resendVerification } from '../../services/authService'
import { useToast } from '../../composables/useToast'

const router = useRouter()

const isDark = ref(false)
const username = ref('')
const password = ref('')
const remember = ref(false)
const showPassword = ref(false)
const isSubmitting = ref(false)
const emailNotVerified = ref(false)
const resendEmail = ref('')
const isResending = ref(false)
const resendMessage = ref('')
const resendSuccess = ref(false)
const resendVerificationUrl = ref('')
const resendVerificationToken = ref('')

const toast = useToast()

const goToRegister = () => {
  router.push('/register')
}

const routeByRole = (roles = []) => {
  const normalizedRoles = roles.map((role) => String(role || '').toUpperCase())
  if (normalizedRoles.some(role => role === 'ROLE_TEACHER' || role === 'TEACHER')) {
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
  emailNotVerified.value = false
  resendMessage.value = ''

  try {
    const authData = await login({
      username: username.value.trim(),
      password: password.value
    })

    if (!authData?.roles?.length) {
      router.push('/select-role')
      return
    }

    routeByRole(authData?.roles || [])
  } catch (error) {
    if (error?.status === 403 && String(error?.payload?.message || '').includes('EMAIL_NOT_VERIFIED')) {
      emailNotVerified.value = true
      resendEmail.value = ''
      toast.error('Email chưa được xác minh. Vui lòng kiểm tra hộp thư hoặc gửi lại link.')
    } else {
      toast.error('Login failed. Please try again.')
    }
  } finally {
    isSubmitting.value = false
  }
}

const onResendVerification = async () => {
  if (!resendEmail.value?.trim()) {
    toast.error('Vui lòng nhập email đăng ký.')
    return
  }
  isResending.value = true
  resendMessage.value = ''
  resendVerificationUrl.value = ''
  resendVerificationToken.value = ''
  try {
    const data = await resendVerification({ email: resendEmail.value.trim() })
    resendSuccess.value = true
    if (data?.verificationUrl) {
      resendVerificationUrl.value = data.verificationUrl
      resendVerificationToken.value = data.verificationUrl.replace(/^.*token=/, '')
      resendMessage.value = 'Chế độ demo: Nhấp vào link để xác minh.'
    } else {
      resendMessage.value = 'Đã gửi lại email xác minh. Vui lòng kiểm tra hộp thư.'
    }
  } catch (error) {
    resendSuccess.value = false
    resendMessage.value = 'Không thể gửi. Email có thể đã được xác minh hoặc không tồn tại.'
  } finally {
    isResending.value = false
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
