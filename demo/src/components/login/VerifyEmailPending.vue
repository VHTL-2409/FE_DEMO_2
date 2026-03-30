<template>
  <div
    class="flex min-h-0 flex-1 flex-col items-center justify-center overflow-y-auto bg-gradient-to-br from-slate-50 via-white to-primary-50/40 p-4 portal-scrollbar dark:from-slate-950 dark:via-background-dark dark:to-primary-900/20"
  >
    <div
      class="w-full max-w-md rounded-2xl border border-slate-200/90 bg-white/95 p-8 shadow-soft backdrop-blur-sm dark:border-slate-700 dark:bg-slate-900/95"
    >
      <div class="text-center">
        <div class="size-14 bg-amber-100 dark:bg-amber-900/30 rounded-xl flex items-center justify-center mx-auto mb-4">
          <LucideIcon name="mail" size="30" />
        </div>
        <h1 class="text-2xl font-bold text-slate-900 dark:text-white">Xác minh email để hoàn tất đăng ký</h1>
        <p class="text-slate-500 dark:text-slate-400 mt-2 text-sm">
          Tài khoản đã được tạo nhưng <strong>chưa hoàn tất đăng ký</strong>. Bạn cần xác minh email trước khi đăng nhập.
        </p>

        <div v-if="email" class="mt-4 p-3 rounded-lg bg-slate-100 dark:bg-slate-700/50 text-sm">
          <span class="text-slate-600 dark:text-slate-300">Email đăng ký: </span>
          <span class="font-medium text-slate-900 dark:text-white">{{ email }}</span>
        </div>

        <div v-if="emailSent" class="mt-4 p-3 rounded-lg bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800">
          <p class="text-green-800 dark:text-green-200 text-sm">
            Email xác minh đã được gửi tới hộp thư của bạn. Vui lòng kiểm tra (kể cả thư mục spam) và nhấp vào link trong email.
          </p>
        </div>

        <div v-else class="mt-4 p-3 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800">
          <p class="text-amber-800 dark:text-amber-200 text-sm mb-2">
            Không thể gửi email. Bạn có thể dùng link sau để xác minh ngay:
          </p>
          <RouterLink
            v-if="verificationToken"
            v-slot="{ navigate }"
            :to="{ path: '/verify-email', query: { token: verificationToken } }"
            custom
          >
            <button
              type="button"
              class="portal-focus inline-flex items-center gap-2 rounded-lg bg-amber-600 px-4 py-2 text-sm font-bold text-white transition-colors hover:bg-amber-700"
              @click="navigate"
            >
              Xác minh ngay
              <LucideIcon name="arrow_forward" size="18" />
            </button>
          </RouterLink>
        </div>

        <div class="mt-6 space-y-3">
          <BaseButton
            variant="secondary"
            type="button"
            class="w-full border-2 border-primary text-primary hover:bg-primary/5"
            :loading="isResending"
            @click="onResend"
          >
            <LucideIcon name="refresh" v-if="!isResending"  text-lg aria-hidden="true"/>
            {{ isResending ? 'Đang gửi...' : 'Gửi lại email xác minh' }}
          </BaseButton>
          <p v-if="resendMessage" class="text-sm" :class="resendSuccess ? 'text-green-600 dark:text-green-400' : 'text-rose-600 dark:text-rose-400'">
            {{ resendMessage }}
          </p>
          <p v-if="resendSuccess && resendVerificationUrl" class="text-sm text-amber-700 dark:text-amber-300">
            Chế độ demo: <RouterLink :to="{ path: '/verify-email', query: { token: resendToken } }" class="font-bold underline">Nhấp vào đây để xác minh</RouterLink>
          </p>

          <RouterLink v-slot="{ navigate }" to="/login" custom>
            <BaseButton class="w-full" size="lg" @click="navigate">Đăng nhập</BaseButton>
          </RouterLink>
        </div>

        <RouterLink
          to="/login"
          class="mt-4 block text-center text-sm font-semibold text-primary hover:underline portal-focus rounded-lg"
        >
          ← Quay lại đăng nhập
        </RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { resendVerification } from '../../services/authService'
import BaseButton from '../shared/BaseButton.vue'

const route = useRoute()
const email = ref(route.query.email || '')
const emailSent = ref(route.query.emailSent === 'true')
const verificationToken = ref(route.query.token || (route.query.verificationUrl ? String(route.query.verificationUrl).replace(/^.*token=/, '') : ''))

const isResending = ref(false)
const resendMessage = ref('')
const resendSuccess = ref(false)
const resendVerificationUrl = ref('')
const resendToken = ref('')

const onResend = async () => {
  if (!email.value) {
    resendMessage.value = 'Vui lòng nhập email đăng ký.'
    return
  }
  isResending.value = true
  resendMessage.value = ''
  resendSuccess.value = false
  resendVerificationUrl.value = ''
  try {
    const data = await resendVerification({ email: email.value.trim() })
    if (!data) {
      resendMessage.value = 'Không tìm thấy tài khoản hoặc email đã được xác minh.'
      return
    }
    resendSuccess.value = true
    if (data.emailSent) {
      resendMessage.value = 'Email xác minh đã được gửi lại. Vui lòng kiểm tra hộp thư.'
    } else if (data.verificationUrl) {
      resendVerificationUrl.value = data.verificationUrl
      resendToken.value = data.verificationUrl.replace(/^.*token=/, '')
      resendMessage.value = 'Không gửi được email. Dùng link bên dưới để xác minh.'
    } else {
      resendMessage.value = 'Đã xử lý. Vui lòng thử lại.'
    }
  } catch (e) {
    resendSuccess.value = false
    resendMessage.value = e?.payload?.message || 'Gửi lại thất bại. Vui lòng thử lại.'
  } finally {
    isResending.value = false
  }
}

onMounted(() => {
  if (route.query.verificationUrl && !verificationToken.value) {
    verificationToken.value = String(route.query.verificationUrl).replace(/^.*token=/, '')
  }
})
</script>
