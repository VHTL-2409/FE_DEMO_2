<template>
  <div
    class="flex min-h-0 flex-1 flex-col items-center justify-center overflow-y-auto bg-gradient-to-br from-slate-50 via-white to-primary-50/40 p-4 portal-scrollbar dark:from-slate-950 dark:via-background-dark dark:to-primary-900/20"
  >
    <div
      class="w-full max-w-md rounded-2xl border border-slate-200/90 bg-white/98 p-8 shadow-soft dark:border-slate-700 dark:bg-slate-900/98"
    >
      <div class="text-center mb-8">
        <div class="size-14 bg-primary/10 rounded-xl flex items-center justify-center mx-auto mb-4">
          <LucideIcon name="lock_reset" size="30" />
        </div>
        <h1 class="text-2xl font-bold text-slate-900 dark:text-white">Quên mật khẩu</h1>
        <p class="text-slate-500 dark:text-slate-400 mt-1 text-sm">Nhập email đăng ký để nhận link đặt lại mật khẩu qua Gmail</p>
      </div>

      <form v-if="!emailSent" class="space-y-4" @submit.prevent="onSubmit">
        <BaseField label="Email" v-slot="{ inputId, hintId, errorId }">
          <BaseInput
            :id="inputId"
            v-model="email"
            type="email"
            required
            autocomplete="email"
            placeholder="email-cua-ban@gmail.com"
            :hint-id="hintId"
            :error-id="errorId"
          />
        </BaseField>

        <BaseButton type="submit" class="w-full" size="lg" :loading="isSubmitting">
          {{ isSubmitting ? 'Đang gửi...' : 'Gửi link đặt lại mật khẩu' }}
        </BaseButton>
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
            <LucideIcon name="mark_email_read" />
            Đã gửi email. Vui lòng kiểm tra hộp thư (kể cả thư mục spam).
          </p>
        </div>
        <BaseButton variant="secondary" type="button" class="w-full" @click="onSendAnother">
          Gửi lại email khác
        </BaseButton>
      </div>

      <RouterLink
        to="/login"
        class="mt-6 block text-center text-sm font-semibold text-primary hover:underline portal-focus rounded-lg"
      >
        ← Quay lại đăng nhập
      </RouterLink>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { forgotPassword } from '../../services/authService'
import { useToast } from '../../composables/useToast'
import BaseButton from '../shared/BaseButton.vue'
import BaseField from '../shared/BaseField.vue'
import BaseInput from '../shared/BaseInput.vue'

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

const onSendAnother = () => {
  emailSent.value = false
  resetUrl.value = ''
}

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
