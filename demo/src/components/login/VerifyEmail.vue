<template>
  <div
    class="flex min-h-0 flex-1 flex-col items-center justify-center overflow-y-auto bg-gradient-to-br from-slate-50 via-white to-primary-50/40 p-4 portal-scrollbar dark:from-slate-950 dark:via-background-dark dark:to-primary-900/20"
  >
    <div
      class="w-full max-w-md rounded-2xl border border-slate-200/90 bg-white/98 p-8 shadow-soft dark:border-slate-700 dark:bg-slate-900/98"
    >
      <div v-if="!done" class="text-center">
        <div class="size-14 bg-primary/10 rounded-xl flex items-center justify-center mx-auto mb-4">
          <LucideIcon name="hourglass_empty" size="30" />
        </div>
        <h1 class="text-2xl font-bold text-slate-900 dark:text-white">Đang xác minh...</h1>
      </div>

      <div v-else-if="success" class="text-center">
        <div class="size-14 bg-primary/10 rounded-xl flex items-center justify-center mx-auto mb-4">
          <LucideIcon name="check_circle" size="30" />
        </div>
        <h1 class="text-2xl font-bold text-slate-900 dark:text-white">Xác minh thành công</h1>
        <RouterLink v-slot="{ navigate }" to="/login" custom>
          <BaseButton class="mt-6 w-full" size="lg" @click="navigate">Đăng nhập</BaseButton>
        </RouterLink>
      </div>

      <div v-else class="text-center">
        <div class="size-14 bg-rose-100 dark:bg-rose-900/30 rounded-xl flex items-center justify-center mx-auto mb-4">
          <LucideIcon name="error" size="30" />
        </div>
        <h1 class="text-2xl font-bold text-slate-900 dark:text-white">Xác minh thất bại</h1>
        <p class="text-slate-500 dark:text-slate-400 mt-1 text-sm">{{ errorMessage }}</p>
        <RouterLink v-slot="{ navigate }" to="/login" custom>
          <BaseButton class="mt-6 w-full" size="lg" @click="navigate">Quay lại đăng nhập</BaseButton>
        </RouterLink>
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
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { verifyEmail } from '../../services/authService'
import BaseButton from '../../shared/BaseButton.vue'

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
