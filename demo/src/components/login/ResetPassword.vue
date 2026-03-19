<template>
  <div class="min-h-screen flex items-center justify-center bg-slate-50 dark:bg-slate-900 p-4">
    <div class="w-full max-w-md bg-white dark:bg-slate-800 rounded-2xl shadow-xl border border-slate-200 dark:border-slate-700 p-8">
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

        <div v-else>
          <div>
            <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Mật khẩu mới</label>
            <input
              v-model="newPassword"
              type="password"
              required
              minlength="6"
              placeholder="Ít nhất 6 ký tự"
              class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Xác nhận mật khẩu</label>
            <input
              v-model="confirmPassword"
              type="password"
              required
              minlength="6"
              placeholder="Nhập lại mật khẩu"
              class="w-full px-4 py-3 rounded-lg border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
            />
            <p v-if="confirmPassword && newPassword !== confirmPassword" class="text-rose-600 text-xs mt-1">Mật khẩu không khớp</p>
          </div>

          <button
            type="submit"
            :disabled="isSubmitting || newPassword !== confirmPassword || newPassword.length < 6"
            class="w-full py-3 bg-primary text-white rounded-lg font-bold hover:bg-primary/90 disabled:opacity-60 disabled:cursor-not-allowed transition-colors mt-4"
          >
            {{ isSubmitting ? 'Đang xử lý...' : 'Đặt lại mật khẩu' }}
          </button>
        </div>
      </form>

      <RouterLink to="/login" class="mt-6 block text-center text-sm text-primary hover:underline">
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
