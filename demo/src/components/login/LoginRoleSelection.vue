<template>
  <div class="portal-viewport bg-background-light font-display text-slate-900">
    <header class="flex items-center justify-between border-b border-slate-200 bg-white px-6 py-3 md:px-10">
      <div class="flex items-center gap-4">
        <div class="size-8 flex items-center justify-center bg-indigo-600 text-white rounded-lg">
          <LucideIcon name="menu_book" size="18" />
        </div>
        <h2 class="text-lg font-bold text-slate-900">EduExam System</h2>
      </div>
      <RouterLink to="/help-center" custom v-slot="{ navigate }">
        <button @click="navigate" class="px-4 py-2 text-sm font-semibold text-indigo-600 border border-indigo-600 rounded-lg hover:bg-indigo-50">Trung tâm trợ giúp</button>
      </RouterLink>
    </header>

    <main class="flex items-center justify-center min-h-[calc(100vh-64px)] p-4 md:p-8 bg-slate-50">
      <div class="w-full max-w-[900px] bg-white rounded-2xl shadow-lg overflow-hidden grid grid-cols-1 lg:grid-cols-2">
        <!-- Form -->
        <div class="p-8 md:p-12">
          <div class="mb-8 text-center">
            <h1 class="text-3xl font-bold text-slate-900">Đăng nhập</h1>
            <p class="mt-2 text-slate-500">Truy cập hệ thống để tạo đề thi, quản lý kỳ thi hoặc tham gia làm bài.</p>
          </div>

          <div v-if="emailNotVerified" class="mb-4 p-4 rounded-lg bg-amber-50 border border-amber-200">
            <p class="text-amber-800 text-sm font-medium mb-2">Email chưa được xác minh.</p>
            <div class="flex gap-2">
              <input v-model="resendEmail" type="email" placeholder="Email đăng ký" class="flex-1 px-3 py-2 text-sm border border-amber-200 rounded-lg" />
              <button @click="onResendVerification" :disabled="isResending" class="px-4 py-2 text-sm font-semibold text-white bg-amber-600 rounded-lg hover:bg-amber-700 disabled:opacity-50">
                {{ isResending ? 'Đang gửi...' : 'Gửi lại' }}
              </button>
            </div>
            <p v-if="resendMessage" class="mt-2 text-sm" :class="resendSuccess ? 'text-green-600' : 'text-rose-600'">{{ resendMessage }}</p>
          </div>

          <form @submit.prevent="onSubmit" class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-slate-700 mb-1">Username</label>
              <div class="relative">
                <LucideIcon name="user" class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size="18" />
                <input v-model="username" type="text" autocomplete="username" placeholder="Nhập tên đăng nhập" class="w-full pl-10 pr-4 py-3 border border-slate-300 rounded-lg focus:outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20" />
              </div>
            </div>

            <div>
              <div class="flex justify-between items-center mb-1">
                <label class="text-sm font-medium text-slate-700">Password</label>
                <RouterLink to="/forgot-password" class="text-xs text-indigo-600 hover:underline">Quên mật khẩu?</RouterLink>
              </div>
              <div class="relative">
                <LucideIcon name="lock" class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size="18" />
                <input v-model="password" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" placeholder="Nhập mật khẩu" class="w-full pl-10 pr-12 py-3 border border-slate-300 rounded-lg focus:outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20" />
                <button type="button" @click="showPassword = !showPassword" class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600">
                  <LucideIcon :name="showPassword ? 'eye_off' : 'eye'" size="18" />
                </button>
              </div>
            </div>

            <div class="flex items-center gap-2">
              <input id="remember" v-model="remember" type="checkbox" class="w-4 h-4 rounded border-slate-300 text-indigo-600" />
              <label for="remember" class="text-sm text-slate-600">Ghi nhớ đăng nhập</label>
            </div>

            <button type="submit" :disabled="isSubmitting" class="w-full py-3 text-base font-semibold text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2">
              <LucideIcon v-if="isSubmitting" name="loader-2" size="18" class="animate-spin" />
              <span>{{ isSubmitting ? 'Đang đăng nhập...' : 'Đăng nhập' }}</span>
            </button>
          </form>

          <p class="mt-6 text-center text-slate-500">
            Chưa có tài khoản?
            <button @click="router.push('/register')" class="text-indigo-600 font-semibold hover:underline ml-1">Tạo tài khoản mới</button>
          </p>
        </div>

        <!-- Right Panel -->
        <div class="hidden lg:flex flex-col justify-center items-center p-12 bg-gradient-to-br from-indigo-900 to-purple-800 text-white">
          <div class="size-20 flex items-center justify-center rounded-2xl border border-white/20 bg-white/10 mb-6">
            <LucideIcon name="school" size="36" />
          </div>
          <h2 class="text-3xl font-bold mb-2">EduExam</h2>
          <p class="text-white/80 mb-8">Hệ thống thi trực tuyến</p>
          <div class="grid grid-cols-2 gap-4 w-full max-w-[280px]">
            <div class="p-4 rounded-xl border border-white/20 bg-white/10 text-center">
              <p class="text-2xl font-bold">CSV</p>
              <p class="text-xs text-white/60 uppercase">Import đề thi</p>
            </div>
            <div class="p-4 rounded-xl border border-white/20 bg-white/10 text-center">
              <p class="text-2xl font-bold">Live</p>
              <p class="text-xs text-white/60 uppercase">Giám sát</p>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login, resendVerification, redirectToSiteByDatabaseRole } from '../../services/authService'
import { useToast } from '../../composables/useToast'
import LucideIcon from '../common/LucideIcon.vue'

const router = useRouter()
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

const toast = useToast()

const onSubmit = async () => {
  if (!username.value || !password.value) {
    toast.error('Vui lòng nhập tên đăng nhập và mật khẩu.')
    return
  }
  isSubmitting.value = true
  emailNotVerified.value = false
  try {
    const authData = await login({ username: username.value.trim(), password: password.value })
    await redirectToSiteByDatabaseRole(router, authData)
  } catch (error) {
    if (error?.status === 403 && String(error?.payload?.message || '').includes('EMAIL_NOT_VERIFIED')) {
      emailNotVerified.value = true
      toast.error('Email chưa được xác minh.')
    } else {
      toast.error('Đăng nhập thất bại.')
    }
  } finally {
    isSubmitting.value = false
  }
}

const onResendVerification = async () => {
  if (!resendEmail.value?.trim()) return
  isResending.value = true
  try {
    await resendVerification({ email: resendEmail.value.trim() })
    resendSuccess.value = true
    resendMessage.value = 'Đã gửi lại email xác minh.'
  } catch {
    resendSuccess.value = false
    resendMessage.value = 'Không thể gửi email.'
  } finally {
    isResending.value = false
  }
}
</script>
