<template>
  <div
    class="flex min-h-0 flex-1 flex-col items-center justify-center overflow-y-auto bg-gradient-to-br from-slate-50 via-white to-primary-50/40 p-4 portal-scrollbar dark:from-slate-950 dark:via-background-dark dark:to-primary-900/20"
  >
    <div
      class="w-full max-w-md rounded-2xl border border-slate-200/90 bg-white/95 p-8 shadow-soft backdrop-blur-sm dark:border-slate-700 dark:bg-slate-900/95"
    >
      <div class="text-center mb-8">
        <div class="size-14 bg-primary/10 rounded-xl flex items-center justify-center mx-auto mb-4">
          <span class="material-symbols-outlined text-primary text-3xl">key</span>
        </div>
        <h1 class="text-2xl font-bold text-slate-900 dark:text-white">Đặt lại mật khẩu</h1>
        <p class="text-slate-500 dark:text-slate-400 mt-1 text-sm">Nhập mật khẩu mới (ít nhất 6 ký tự)</p>
      </div>

      <form @submit.prevent="onSubmit" class="space-y-4">
        <div v-if="!tokenFromQuery" class="p-3 rounded-lg bg-amber-50 dark:bg-amber-900/20 text-amber-800 dark:text-amber-200 text-sm">
          Thiếu token. Vui lòng mở link từ email hoặc trang quên mật khẩu.
        </div>

        <div v-else class="space-y-4">
          <BaseField label="Mật khẩu mới" v-slot="{ inputId, hintId, errorId }">
            <BaseInput
              :id="inputId"
              v-model="newPassword"
              type="password"
              required
              minlength="6"
              autocomplete="new-password"
              placeholder="Ít nhất 6 ký tự"
              :hint-id="hintId"
              :error-id="errorId"
            />
          </BaseField>

          <BaseField
            label="Xác nhận mật khẩu"
            :error="confirmPassword && newPassword !== confirmPassword ? 'Mật khẩu không khớp' : ''"
            v-slot="{ inputId, hintId, errorId }"
          >
            <BaseInput
              :id="inputId"
              v-model="confirmPassword"
              type="password"
              required
              minlength="6"
              autocomplete="new-password"
              placeholder="Nhập lại mật khẩu"
              :invalid="!!(confirmPassword && newPassword !== confirmPassword)"
              :hint-id="hintId"
              :error-id="errorId"
            />
          </BaseField>

          <BaseButton
            type="submit"
            class="mt-2 w-full"
            size="lg"
            :loading="isSubmitting"
            :disabled="newPassword !== confirmPassword || newPassword.length < 6"
          >
            {{ isSubmitting ? 'Đang xử lý...' : 'Đặt lại mật khẩu' }}
          </BaseButton>
        </div>
      </form>

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
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { resetPassword } from '../../services/authService'
import { useToast } from '../../composables/useToast'
import BaseButton from '../shared/BaseButton.vue'
import BaseField from '../shared/BaseField.vue'
import BaseInput from '../shared/BaseInput.vue'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const newPassword = ref('')
const confirmPassword = ref('')
const isSubmitting = ref(false)

const tokenFromQuery = computed(() => route.query.token || '')

const onSubmit = async () => {
  if (!tokenFromQuery.value || newPassword.value !== confirmPassword.value || newPassword.value.length < 6) return
  isSubmitting.value = true
  try {
    await resetPassword({ token: tokenFromQuery.value, newPassword: newPassword.value })
    toast.success('Đặt lại mật khẩu thành công. Vui lòng đăng nhập.')
    router.push('/login')
  } catch (error) {
    toast.error(error?.message || 'Không thể đặt lại mật khẩu.')
  } finally {
    isSubmitting.value = false
  }
}

onMounted(() => {
  if (tokenFromQuery.value) {
    document.title = 'Đặt lại mật khẩu - ExamPortal'
  }
})
</script>
