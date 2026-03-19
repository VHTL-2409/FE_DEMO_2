<template>
  <div class="min-h-screen flex items-center justify-center bg-slate-50 dark:bg-slate-900 p-4">
    <div class="w-full max-w-md bg-white dark:bg-slate-800 rounded-2xl shadow-xl border border-slate-200 dark:border-slate-700 p-8">
      <div class="text-center mb-8">
        <div class="size-14 bg-primary/10 rounded-xl flex items-center justify-center mx-auto mb-4">
          <span class="material-symbols-outlined text-primary text-3xl">lock_reset</span>
        </div>
        <h1 class="text-2xl font-bold text-slate-900 dark:text-white">Quên mật khẩu</h1>
        <p class="text-slate-500 dark:text-slate-400 mt-1 text-sm">Nhập email đăng ký để nhận link đặt lại mật khẩu qua Gmail</p>
      </div>

      <form v-if="!emailSent" @submit.prevent="onSubmit" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Email</label>
          <input
            v-model="email"
            type="email"
            required
            placeholder="your-email@gmail.com"
            class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
          />
        </div>

        <button
          type="submit"
          :disabled="isSubmitting"
          class="w-full py-3 bg-primary text-white rounded-lg font-bold hover:bg-primary/90 disabled:opacity-60 disabled:cursor-not-allowed transition-colors flex items-center justify-center gap-2"
        >
          <span v-if="isSubmitting" class="inline-block size-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
          {{ isSubmitting ? 'Đang gửi...' : 'Gửi link đặt lại mật khẩu' }}
        </button>
      </form>

      <div v-else class="space-y-4">
        <div v-if="resetUrl" class="p-4 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800">
          <p class="text-sm font-medium text-amber-800 dark:text-amber-200 mb-2">Chế độ demo (chưa cấu hình Gmail):</p>
          <p class="text-slate-600 dark:text-slate-400 text-sm mb-2">Sao chép link dưới đây để đặt lại mật khẩu:</p>
          <a :href="fullResetUrl" target="_blank" class="text-primary text-sm font-semibold break-all hover:underline block">
            {{ fullResetUrl }}
          </a>
        </div>
        <div v-else class="p-4 rounded-lg bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800">
          <p class="text-green-800 dark:text-green-200 text-sm font-medium flex items-center gap-2">
            <span class="material-symbols-outlined text-xl">mark_email_read</span>
            Đã gửi email. Vui lòng kiểm tra hộp thư (kể cả thư mục spam).
          </p>
        </div>
        <button
          type="button"
          @click="emailSent = false; resetUrl = ''"
          class="w-full py-3 border border-slate-300 dark:border-slate-600 text-slate-700 dark:text-slate-300 rounded-lg font-medium hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
        >
          Gửi lại email khác
        </button>
      </div>

      <RouterLink to="/login" class="mt-6 block text-center text-sm text-primary hover:underline">
        ← Quay lại đăng nhập
      </RouterLink>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { forgotPassword } from '../../services/authService'
import { useToast } from '../../composables/useToast'

const router = useRouter()
const toast = useToast()
const email = ref('')
const isSubmitting = ref(false)
const emailSent = ref(false)
const resetUrl = ref('')
const fullResetUrl = computed(() => {
  if (!resetUrl.value) return ''
  const path = resetUrl.value.startsWith('/') ? resetUrl.value : '/' + resetUrl.value
  return typeof window !== 'undefined' ? window.location.origin + path : path
})

const onSubmit = async () => {
  isSubmitting.value = true
  resetUrl.value = ''
  emailSent.value = false
  try {
    const data = await forgotPassword({ email: email.value })
    emailSent.value = true
    if (data?.resetUrl) {
      resetUrl.value = data.resetUrl
      toast.info('Chế độ demo: Sử dụng link bên dưới.')
    } else {
      toast.success('Đã gửi email. Vui lòng kiểm tra hộp thư.')
    }
  } catch (error) {
    toast.error(error?.message || 'Không thể gửi yêu cầu.')
  } finally {
    isSubmitting.value = false
  }
}
</script>
