<template>
  <div class="oauth-callback">
    <div v-if="status === 'loading'" class="oauth-callback__card">
      <div class="oauth-callback__spinner" />
      <p class="oauth-callback__title">Đang xử lý đăng nhập Google...</p>
      <p class="oauth-callback__sub">Vui lòng chờ trong giây lát</p>
    </div>

    <div v-else-if="status === 'success'" class="oauth-callback__card oauth-callback__card--success">
      <div class="oauth-callback__icon oauth-callback__icon--success">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="20 6 9 17 4 12"/>
        </svg>
      </div>
      <p class="oauth-callback__title">Đăng nhập thành công!</p>
      <p class="oauth-callback__sub">Đang chuyển đến trang chủ...</p>
    </div>

    <div v-else class="oauth-callback__card oauth-callback__card--error">
      <div class="oauth-callback__icon oauth-callback__icon--error">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
        </svg>
      </div>
      <p class="oauth-callback__title">Đăng nhập Google thất bại</p>
      <p class="oauth-callback__sub">{{ errorMessage }}</p>
      <RouterLink to="/login" class="oauth-callback__btn">Quay lại trang đăng nhập</RouterLink>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/authStore'
import { storeAuthSession } from '../../services/authService'
import { useToast } from '../../composables/useToast'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()

const status = ref('loading')
const errorMessage = ref('')

onMounted(async () => {
  const params = new URLSearchParams(window.location.search)
  const error = params.get('error')
  const token = params.get('token')
  const refreshToken = params.get('refreshToken')
  const roles = params.get('roles')
  const username = params.get('username')
  const name = params.get('name')

  // Xử lý lỗi từ Google
  if (error) {
    if (error === 'google_denied') {
      errorMessage.value = 'Bạn đã hủy đăng nhập Google.'
    } else if (error === 'google_not_configured') {
      errorMessage.value = 'Đăng nhập Google chưa được cấu hình. Vui lòng liên hệ quản trị viên.'
    } else if (error === 'google_oauth_failed') {
      errorMessage.value = 'Không thể xác thực với Google. Vui lòng thử lại.'
    } else {
      errorMessage.value = 'Đã xảy ra lỗi không xác định. Vui lòng thử lại.'
    }
    toast.error(errorMessage.value)
    status.value = 'error'
    return
  }

  // Xử lý thành công
  // Chỉ bắt buộc access token; refresh có thể thiếu nếu URL bị cắt (giới hạn độ dài query)
  if (token) {
    try {
      const roleList = roles ? roles.split(',').map((r) => r.trim()).filter(Boolean) : []
      const resolvedUsername = (username && username.trim()) || 'User'
      storeAuthSession({
        token,
        ...(refreshToken ? { refreshToken } : {}),
        username: resolvedUsername,
        roles: roleList
      })
      authStore.setSessionFromPayload({
        token,
        username: resolvedUsername,
        roles: roleList
      })

      // OAuth user mới (chưa có role) → lưu Google data sang select-role
      if (!roleList.length) {
        // Lưu tạm để select-role điền vào profile
        sessionStorage.setItem('oauth_pending_email', (username && username.trim()) || '')
        sessionStorage.setItem('oauth_pending_name', (name && name.trim()) || '')
        sessionStorage.setItem('oauth_pending', 'true')
        toast.success(`Chào mừng ${resolvedUsername === 'User' ? 'bạn' : resolvedUsername}! Vui lòng chọn vai trò.`)
        status.value = 'success'
        setTimeout(() => { router.push('/select-role') }, 1000)
        return
      }

      toast.success(`Chào mừng ${resolvedUsername === 'User' ? 'bạn' : resolvedUsername}! Đăng nhập thành công.`)
      status.value = 'success'
      // Chuyển đến redirect page để prefetch dashboard data
      const isTeacher = roleList.some((r) => r === 'TEACHER' || r === 'ROLE_TEACHER')
      setTimeout(() => {
        router.push(`/redirect?role=${isTeacher ? 'teacher' : 'student'}`)
      }, 1000)
    } catch (err) {
      errorMessage.value = 'Không thể lưu phiên đăng nhập. Vui lòng thử lại.'
      toast.error(errorMessage.value)
      status.value = 'error'
    }
  } else {
    errorMessage.value = 'Không nhận được thông tin từ Google. Vui lòng thử lại.'
    toast.error(errorMessage.value)
    status.value = 'error'
  }
})
</script>

<style scoped>
.oauth-callback {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f8fafc 0%, #eef2ff 100%);
  padding: 1rem;
}

.oauth-callback__card {
  background: white;
  border-radius: 20px;
  padding: 2.5rem;
  text-align: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  max-width: 400px;
  width: 100%;
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.oauth-callback__card--success {
  border: 2px solid #dcfce7;
}

.oauth-callback__card--error {
  border: 2px solid #fee2e2;
}

.oauth-callback__spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #e2e8f0;
  border-top-color: #4f46e5;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 1.5rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.oauth-callback__icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.5rem;
}

.oauth-callback__icon--success {
  background: #dcfce7;
  color: #16a34a;
}

.oauth-callback__icon--error {
  background: #fee2e2;
  color: #dc2626;
}

.oauth-callback__title {
  font-size: 1.25rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 0.5rem;
}

.oauth-callback__sub {
  font-size: 0.9rem;
  color: #64748b;
  margin: 0 0 1.5rem;
}

.oauth-callback__btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  background: #4f46e5;
  color: white;
  border-radius: 10px;
  font-size: 0.875rem;
  font-weight: 700;
  text-decoration: none;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.oauth-callback__btn:hover {
  background: #4338ca;
  transform: translateY(-1px);
}
</style>
@media (prefers-reduced-motion: reduce) {
  * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
  }
}