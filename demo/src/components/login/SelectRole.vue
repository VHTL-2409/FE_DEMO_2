<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 portal-viewport">
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
        <header class="flex items-center justify-between border-b border-primary/10 px-6 py-4 lg:px-40 bg-white dark:bg-background-dark">
          <div class="flex items-center gap-3 text-primary">
            <div class="size-8 flex items-center justify-center bg-primary rounded-lg text-white">
              <LucideIcon name="school" />
            </div>
            <h2 class="text-slate-900 dark:text-slate-100 text-xl font-bold leading-tight tracking-tight">EduPlatform</h2>
          </div>
        </header>

        <main class="teacher-page-shell relative flex flex-1 items-center justify-center overflow-hidden p-6 lg:p-12">
          <div class="staff-surface-strong w-full max-w-[640px] animate-fade-up rounded-[1.75rem] p-8 lg:p-12">
            <div class="text-center mb-8">
              <h1 class="text-slate-900 dark:text-slate-100 text-3xl font-black leading-tight tracking-tight mb-2">Chọn vai trò</h1>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
              <button
                type="button"
                :disabled="isSubmitting"
                class="group rounded-2xl border border-slate-200 p-6 text-left transition-all hover:border-primary/40 hover:shadow-card-hover portal-focus disabled:opacity-60 dark:border-slate-700"
                @click="selectRole('STUDENT')"
              >
                <div class="size-12 rounded-xl bg-primary/10 text-primary flex items-center justify-center mb-4">
                  <LucideIcon name="school" size="24" />
                </div>
                <h3 class="text-lg font-bold mb-2">Học sinh</h3>
              </button>

              <button
                type="button"
                :disabled="isSubmitting"
                class="group rounded-2xl border border-slate-200 p-6 text-left transition-all hover:border-primary/40 hover:shadow-card-hover portal-focus disabled:opacity-60 dark:border-slate-700"
                @click="selectRole('TEACHER')"
              >
                <div class="size-12 rounded-xl bg-primary/10 text-primary flex items-center justify-center mb-4">
                  <LucideIcon name="co_present" size="24" />
                </div>
                <h3 class="text-lg font-bold mb-2">Giáo viên</h3>
              </button>
            </div>

            <div v-if="errorMessage" class="mt-6 rounded-xl border border-rose-200 bg-rose-50 text-rose-700 text-sm font-semibold px-4 py-3">
              {{ errorMessage }}
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
import { assignRole, redirectToSiteByDatabaseRole, updateSharedProfile } from '../../services/authService'
import { removeAccents } from '../../utils/textUtils'
import { useToast } from '../../composables/useToast'

const router = useRouter()
const toast = useToast()

const isDark = ref(false)
const isSubmitting = ref(false)
const errorMessage = ref('')

const selectRole = async (role) => {
  if (isSubmitting.value) return
  errorMessage.value = ''
  isSubmitting.value = true

  try {
    await assignRole(role)

    // Nếu là OAuth user mới (Google) — auto-fill profile với email + name
    const isOAuthPending = sessionStorage.getItem('oauth_pending') === 'true'
    if (isOAuthPending) {
      const googleEmail = sessionStorage.getItem('oauth_pending_email') || ''
      const googleName = sessionStorage.getItem('oauth_pending_name') || ''
      sessionStorage.removeItem('oauth_pending')
      sessionStorage.removeItem('oauth_pending_email')
      sessionStorage.removeItem('oauth_pending_name')

      try {
        await updateSharedProfile({
          email: googleEmail || null,
          fullName: googleName || null,
          displayName: removeAccents(googleName) || null
        })
        toast.success('Đã điền thông tin từ tài khoản Google.')
      } catch (profileErr) {
        // Không block flow — profile có thể để trống, user tự sửa sau
        console.warn('[SelectRole] Không thể cập nhật profile tự động:', profileErr)
      }
    }

    await redirectToSiteByDatabaseRole(router)
  } catch (error) {
    errorMessage.value = 'Không thể cập nhật role. Vui lòng thử lại.'
    toast.error(errorMessage.value)
  } finally {
    isSubmitting.value = false
  }
}
</script>
