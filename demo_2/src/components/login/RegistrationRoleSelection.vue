<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 portal-viewport"
  >
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
        <header
          class="flex items-center justify-between border-b border-primary/10 px-6 py-4 lg:px-40 bg-white dark:bg-background-dark"
        >
          <div class="flex items-center gap-3 text-primary">
            <div class="size-8 flex items-center justify-center bg-primary rounded-lg text-white">
              <span class="material-symbols-outlined text-xl">school</span>
            </div>
            <h2 class="text-slate-900 dark:text-slate-100 text-xl font-bold leading-tight tracking-tight">EduExam Platform</h2>
          </div>
          <div class="flex items-center gap-4">
            <span class="text-sm text-slate-500 hidden sm:block">Đã có tài khoản?</span>
            <BaseButton variant="ghost" size="sm" class="min-w-[5.25rem] border-2 border-primary" @click="goToLogin">
              Đăng nhập
            </BaseButton>
          </div>
        </header>

        <main class="flex-1 relative flex items-center justify-center p-6 lg:p-12 overflow-hidden">
          <div
            class="w-full max-w-[560px] animate-fade-up rounded-xl border border-primary/10 bg-white/95 p-8 shadow-soft backdrop-blur-sm dark:border-primary/20 dark:bg-slate-900/80 lg:p-12"
          >
            <div class="text-center mb-10">
              <h1 class="text-slate-900 dark:text-slate-100 text-4xl font-black leading-tight tracking-tight mb-2">
                Tạo Tài Khoản
              </h1>
            </div>

            <form class="space-y-8 animate-fade-up-delay" @submit.prevent="onSubmit">
              <div v-if="errorMessage" class="rounded-xl border border-rose-200 bg-rose-50 text-rose-700 text-sm font-semibold px-4 py-3">
                {{ errorMessage }}
              </div>
              <div class="space-y-5">
                <BaseField label="Username" v-slot="{ inputId, hintId, errorId }">
                  <div class="relative">
                    <span
                      class="material-symbols-outlined pointer-events-none absolute left-3 top-1/2 z-[1] -translate-y-1/2 text-xl text-slate-400"
                      aria-hidden="true"
                      >person</span
                    >
                    <BaseInput
                      :id="inputId"
                      v-model="username"
                      autocomplete="username"
                      placeholder="janedoe"
                      input-class="border-0 bg-background-light py-3.5 pl-11 dark:bg-slate-800"
                      :hint-id="hintId"
                      :error-id="errorId"
                    />
                  </div>
                </BaseField>
                <BaseField label="Email" v-slot="{ inputId, hintId, errorId }">
                  <div class="relative">
                    <span
                      class="material-symbols-outlined pointer-events-none absolute left-3 top-1/2 z-[1] -translate-y-1/2 text-xl text-slate-400"
                      aria-hidden="true"
                      >mail</span
                    >
                    <BaseInput
                      :id="inputId"
                      v-model="email"
                      type="email"
                      autocomplete="email"
                      placeholder="jane@university.edu"
                      input-class="border-0 bg-background-light py-3.5 pl-11 dark:bg-slate-800"
                      :hint-id="hintId"
                      :error-id="errorId"
                    />
                  </div>
                </BaseField>
                <BaseField
                  label="Mật khẩu"
                  hint="Tối thiểu 8 ký tự."
                  v-slot="{ inputId, hintId, errorId }"
                >
                  <div class="relative">
                    <span
                      class="material-symbols-outlined pointer-events-none absolute left-3 top-1/2 z-[1] -translate-y-1/2 text-xl text-slate-400"
                      aria-hidden="true"
                      >lock</span
                    >
                    <BaseInput
                      :id="inputId"
                      v-model="password"
                      type="password"
                      autocomplete="new-password"
                      placeholder="••••••••"
                      input-class="border-0 bg-background-light py-3.5 pl-11 dark:bg-slate-800"
                      :hint-id="hintId"
                      :error-id="errorId"
                    />
                  </div>
                </BaseField>
              </div>

              <BaseButton type="submit" class="w-full" size="lg" :loading="isSubmitting">
                {{ isSubmitting ? 'Đang tạo tài khoản...' : 'Tạo tài khoản' }}
                <span class="material-symbols-outlined text-xl" aria-hidden="true">arrow_forward</span>
              </BaseButton>
              <p class="text-center text-xs text-slate-500 leading-relaxed">
                Bằng việc tạo tài khoản, bạn đồng ý với
                <a class="text-primary hover:underline font-semibold" href="#">Điều khoản dịch vụ</a> và
                <a class="text-primary hover:underline font-semibold" href="#">Chính sách bảo mật</a>.
              </p>
            </form>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../../services/authService'
import { useToast } from '../../composables/useToast'
import BaseButton from '../shared/BaseButton.vue'
import BaseField from '../shared/BaseField.vue'
import BaseInput from '../shared/BaseInput.vue'

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