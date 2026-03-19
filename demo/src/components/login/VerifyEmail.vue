<template>
  <div class="min-h-screen flex items-center justify-center bg-slate-50 dark:bg-slate-900 p-4">
    <div class="w-full max-w-md bg-white dark:bg-slate-800 rounded-2xl shadow-xl border border-slate-200 dark:border-slate-700 p-8">
      <div v-if="!done" class="text-center">
        <div class="size-14 bg-primary/10 rounded-xl flex items-center justify-center mx-auto mb-4">
          <span class="material-symbols-outlined text-primary text-3xl">hourglass_empty</span>
        </div>
        <h1 class="text-2xl font-bold text-slate-900 dark:text-white">Đang xác minh...</h1>
        <p class="text-slate-500 dark:text-slate-400 mt-1 text-sm">Vui lòng đợi trong giây lát</p>
      </div>

      <div v-else-if="success" class="text-center">
        <div class="size-14 bg-primary/10 rounded-xl flex items-center justify-center mx-auto mb-4">
          <span class="material-symbols-outlined text-primary text-3xl">check_circle</span>
        </div>
        <h1 class="text-2xl font-bold text-slate-900 dark:text-white">Xác minh thành công</h1>
        <p class="text-slate-500 dark:text-slate-400 mt-1 text-sm">Email của bạn đã được xác minh. Bạn có thể đăng nhập ngay.</p>
        <RouterLink
          to="/login"
          class="mt-6 inline-flex items-center justify-center w-full py-3 bg-primary text-white rounded-lg font-bold hover:bg-primary/90 transition-colors"
        >
          Đăng nhập
        </RouterLink>
      </div>

      <div v-else class="text-center">
        <div class="size-14 bg-rose-100 dark:bg-rose-900/30 rounded-xl flex items-center justify-center mx-auto mb-4">
          <span class="material-symbols-outlined text-rose-600 text-3xl">error</span>
        </div>
        <h1 class="text-2xl font-bold text-slate-900 dark:text-white">Xác minh thất bại</h1>
        <p class="text-slate-500 dark:text-slate-400 mt-1 text-sm">{{ errorMessage }}</p>
        <RouterLink
          to="/login"
          class="mt-6 inline-flex items-center justify-center w-full py-3 bg-primary text-white rounded-lg font-bold hover:bg-primary/90 transition-colors"
        >
          Quay lại đăng nhập
        </RouterLink>
      </div>

      <RouterLink to="/login" class="mt-6 block text-center text-sm text-primary hover:underline">
        ← Quay lại đăng nhập
      </RouterLink>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { verifyEmail } from '../../services/authService'

const route = useRoute()
const done = ref(false)
const success = ref(false)
const errorMessage = ref('')

onMounted(async () => {
  const token = route.query.token
  if (!token) {
    done.value = true
    success.value = false
    errorMessage.value = 'Thiếu token. Vui lòng mở link từ email.'
    return
  }

  try {
    await verifyEmail(token)
    done.value = true
    success.value = true
  } catch (error) {
    done.value = true
    success.value = false
    errorMessage.value = error?.payload?.message || error?.message || 'Token không hợp lệ hoặc đã hết hạn.'
  }
})
</script>
