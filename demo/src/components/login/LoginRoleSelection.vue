<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="portal-viewport bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100"
  >
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden bg-slate-950/[0.015]">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
        <header
          class="sticky top-0 z-50 flex items-center justify-between whitespace-nowrap border-b border-white/60 bg-white/72 px-6 py-3 backdrop-blur-xl md:px-10"
        >
          <div class="flex items-center gap-4 text-primary dark:text-slate-100">
            <div class="size-8 flex items-center justify-center bg-primary text-white rounded-lg">
              <span class="material-symbols-outlined">menu_book</span>
            </div>
            <h2
              class="text-lg font-extrabold leading-tight tracking-[-0.02em] text-primary dark:text-slate-100"
            >
              EduExam System
            </h2>
          </div>
          <RouterLink to="/help-center" custom v-slot="{ navigate }">
            <BaseButton size="sm" class="min-w-[7rem]" @click="navigate">Help Center</BaseButton>
          </RouterLink>
        </header>

        <main
          class="relative flex flex-1 items-center justify-center overflow-hidden bg-gradient-to-br from-slate-50 via-white to-primary-50/60 p-4 dark:from-slate-950 dark:via-background-dark dark:to-primary-900/20 md:p-8 lg:p-12"
        >
          <div
            class="portal-glass-panel grid w-full max-w-[1040px] grid-cols-1 overflow-hidden rounded-[2rem] animate-fade-up lg:grid-cols-2"
          >
            <div class="flex flex-col justify-center p-8 md:p-12">
              <div class="mb-8">
                <h1
                  class="mb-2 text-center text-[2.4rem] font-black leading-[1.02] tracking-[-0.035em] text-slate-900 dark:text-slate-100 sm:text-[2.7rem]"
                >
                  Đăng nhập
                </h1>
                <p class="mx-auto mt-3 max-w-md text-center text-[0.96rem] leading-7 text-slate-500 dark:text-slate-400">
                  Truy cập hệ thống để tạo đề thi, quản lý kỳ thi hoặc tham gia làm bài trong một trải nghiệm nhất quán và an toàn.
                </p>
              </div>


              <div v-if="emailNotVerified" class="mb-4 p-4 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800">
                <p class="text-amber-800 dark:text-amber-200 text-sm font-medium mb-2">Email chưa được xác minh. Vui lòng kiểm tra hộp thư hoặc gửi lại link xác minh.</p>
                <div class="flex gap-2">
                  <BaseInput
                    v-model="resendEmail"
                    type="email"
                    placeholder="Email đăng ký"
                    input-class="flex-1 min-w-0 border-amber-200 dark:border-amber-700 py-2 text-sm"
                  />
                  <button
                    type="button"
                    class="portal-focus inline-flex shrink-0 items-center justify-center gap-2 rounded-lg bg-amber-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-amber-700 disabled:cursor-not-allowed disabled:opacity-60"
                    :disabled="isResending"
                    @click="onResendVerification"
                  >
                    <span
                      v-if="isResending"
                      class="material-symbols-outlined animate-spin text-lg"
                      aria-hidden="true"
                      >progress_activity</span
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
                <BaseField label="Username">
                  <template #default="{ inputId, hintId, errorId }">
                    <div class="relative">
                      <span
                        class="material-symbols-outlined pointer-events-none absolute left-3 top-1/2 z-[1] -translate-y-1/2 text-slate-400"
                        aria-hidden="true"
                        >person</span
                      >
                      <BaseInput
                        :id="inputId"
                        v-model="username"
                        autocomplete="username"
                        placeholder="Enter your username"
                        input-class="pl-10"
                        :hint-id="hintId"
                        :error-id="errorId"
                      />
                    </div>
                  </template>
                </BaseField>

                <BaseField label="Password">
                  <template #labelTrailing>
                    <RouterLink
                      to="/forgot-password"
                      class="rounded text-xs font-bold text-primary hover:underline portal-focus"
                    >
                      Quên mật khẩu?
                    </RouterLink>
                  </template>
                  <template #default="{ inputId, hintId, errorId }">
                    <div class="relative flex items-center">
                      <span
                        class="material-symbols-outlined pointer-events-none absolute left-3 z-[1] text-slate-400"
                        aria-hidden="true"
                        >lock</span
                      >
                      <BaseInput
                        :id="inputId"
                        v-model="password"
                        :type="showPassword ? 'text' : 'password'"
                        autocomplete="current-password"
                        placeholder="Enter your password"
                        input-class="pl-10 pr-12"
                        :hint-id="hintId"
                        :error-id="errorId"
                      />
                      <button
                        type="button"
                        class="absolute right-2 inline-flex size-9 items-center justify-center rounded-lg text-slate-400 transition-colors hover:bg-slate-100 hover:text-primary dark:hover:bg-slate-700"
                        :aria-pressed="showPassword"
                        :aria-label="showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
                        @click="showPassword = !showPassword"
                      >
                        <span class="material-symbols-outlined text-xl" aria-hidden="true">
                          {{ showPassword ? 'visibility_off' : 'visibility' }}
                        </span>
                      </button>
                    </div>
                  </template>
                </BaseField>

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


                <BaseButton type="submit" size="lg" class="w-full text-[0.98rem]" :loading="isSubmitting">
                  <span>{{ isSubmitting ? 'Signing In...' : 'Sign In' }}</span>
                  <span class="material-symbols-outlined text-lg" aria-hidden="true">login</span>
                </BaseButton>
              </form>

              <div class="mt-8 text-center animate-fade-in">
                <p class="text-[0.94rem] text-slate-500 dark:text-slate-400">
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

            <div class="portal-mesh-surface relative hidden flex-col justify-center overflow-hidden p-12 lg:flex">
              <div class="pointer-events-none absolute inset-0 grid grid-cols-4 gap-8 p-12 opacity-[0.12]">
                <span class="material-symbols-outlined text-8xl text-white">calculate</span>
                <span class="material-symbols-outlined text-8xl text-white">science</span>
                <span class="material-symbols-outlined text-8xl text-white">history_edu</span>
                <span class="material-symbols-outlined text-8xl text-white">language</span>
                <span class="material-symbols-outlined text-8xl text-white">functions</span>
                <span class="material-symbols-outlined text-8xl text-white">biotech</span>
                <span class="material-symbols-outlined text-8xl text-white">terminal</span>
                <span class="material-symbols-outlined text-8xl text-white">psychology</span>
              </div>
              <div class="relative z-10 max-w-sm text-center text-white">
                <div
                  class="mx-auto mb-8 flex size-20 items-center justify-center rounded-[1.4rem] border border-white/25 bg-white/16 shadow-[0_24px_54px_-26px_rgba(15,23,42,0.55)] backdrop-blur-sm"
                >
                  <span class="material-symbols-outlined text-4xl text-white">school</span>
                </div>
                <h2 class="text-3xl font-bold mb-4">EduExam</h2>
                <p class="text-white/80 leading-relaxed">
                  Hệ thống thi trực tuyến
                </p>
                <div class="mt-12 grid grid-cols-2 gap-4">
                  <div class="rounded-2xl border border-white/16 bg-white/10 p-4 backdrop-blur-sm">
                    <p class="text-2xl font-bold">CSV</p>
                    <p class="text-xs text-white/60 uppercase tracking-widest">Import đề thi</p>
                  </div>
                  <div class="rounded-2xl border border-white/16 bg-white/10 p-4 backdrop-blur-sm">
                    <p class="text-2xl font-bold">Live</p>
                    <p class="text-xs text-white/60 uppercase tracking-widest">Giám sát</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login, resendVerification, redirectToSiteByDatabaseRole } from '../../services/authService'
import { useToast } from '../../composables/useToast'
import BaseButton from '../shared/BaseButton.vue'
import BaseField from '../shared/BaseField.vue'
import BaseInput from '../shared/BaseInput.vue'

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

    await redirectToSiteByDatabaseRole(router, authData)
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
