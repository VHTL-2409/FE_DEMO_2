<template>
  <div class="login-page">
    <!-- Animated background -->
    <div class="login-bg" aria-hidden="true">
      <div class="login-bg__orb login-bg__orb--1" />
      <div class="login-bg__orb login-bg__orb--2" />
      <div class="login-bg__orb login-bg__orb--3" />
      <div class="login-bg__grid" />
    </div>

    <div class="login-container">
      <!-- Left brand panel -->
      <div class="login-brand">
        <div class="login-brand__inner">
          <AppLogo size="lg" variant="brand" animate />

          <div class="login-brand__hero">
            <div class="login-brand__badge">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
              </svg>
              Nền tảng thi trực tuyến hàng đầu
            </div>
            <h1 class="login-brand__headline">
              Học mọi lúc,<br />
              <span class="login-brand__headline-accent">thi mọi nơi.</span>
            </h1>
            <p class="login-brand__description">
              EduExam kết nối giáo viên và học sinh trong một hệ thống thi thông minh, an toàn và dễ sử dụng.
            </p>
          </div>

          <div class="login-brand__features">
            <div class="login-brand__feature">
              <span class="login-brand__feature-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/>
                </svg>
              </span>
              <span>Tạo đề thi nhanh chóng</span>
            </div>
            <div class="login-brand__feature">
              <span class="login-brand__feature-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/>
                </svg>
              </span>
              <span>Giám sát theo thời gian thực</span>
            </div>
            <div class="login-brand__feature">
              <span class="login-brand__feature-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
                </svg>
              </span>
              <span>Phân tích kết quả chi tiết</span>
            </div>
          </div>

          <div class="login-brand__stats">
            <div class="login-brand__stat">
              <strong>10K+</strong>
              <span>Học sinh</span>
            </div>
            <div class="login-brand__stat-divider" />
            <div class="login-brand__stat">
              <strong>500+</strong>
              <span>Giáo viên</span>
            </div>
            <div class="login-brand__stat-divider" />
            <div class="login-brand__stat">
              <strong>50K+</strong>
              <span>Đề thi</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right form panel -->
      <div class="login-form-panel">
        <div class="login-form-wrap">
          <!-- Header -->
          <div class="login-form-header">
            <AppLogo size="sm" show-text animate class="login-form-logo" />
            <h2 class="login-form-title">Chào mừng trở lại</h2>
            <p class="login-form-subtitle">Đăng nhập để truy cập hệ thống của bạn</p>
          </div>

          <!-- Email not verified alert -->
          <div v-if="emailNotVerified" class="login-alert login-alert--warning">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
            <p>Email chưa được xác minh. Kiểm tra hộp thư hoặc gửi lại.</p>
            <div class="login-alert__resend">
              <input v-model="resendEmail" type="email" placeholder="Email đăng ký" class="login-input login-input--sm" />
              <button @click="onResendVerification" :disabled="isResending" class="login-btn login-btn--sm login-btn--ghost-warning">
                {{ isResending ? 'Đang gửi...' : 'Gửi lại' }}
              </button>
            </div>
            <p v-if="resendMessage" class="login-alert__resend-msg" :class="resendSuccess ? 'login-alert__resend-msg--ok' : 'login-alert__resend-msg--fail'">
              {{ resendMessage }}
            </p>
          </div>

          <!-- Error alert -->
          <div v-if="globalError" class="login-alert login-alert--error">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
            </svg>
            <span>{{ globalError }}</span>
            <button type="button" class="login-alert__close" @click="globalError = ''" aria-label="Đóng">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>

          <!-- Form -->
          <form class="login-form" @submit.prevent="onSubmit" novalidate>
            <div class="login-field" :class="{ 'login-field--error': fieldErrors.username }">
              <label class="login-label" for="login-username">Username</label>
              <div class="login-input-wrap">
                <span class="login-input-icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/>
                  </svg>
                </span>
                <input
                  id="login-username"
                  v-model="username"
                  type="text"
                  autocomplete="username"
                  placeholder="Nhập tên đăng nhập"
                  class="login-input login-input--has-icon"
                  :disabled="isSubmitting"
                  @input="fieldErrors.username = ''"
                />
              </div>
              <p v-if="fieldErrors.username" class="login-field-error">{{ fieldErrors.username }}</p>
            </div>

            <div class="login-field" :class="{ 'login-field--error': fieldErrors.password }">
              <div class="login-label-row">
                <label class="login-label" for="login-password">Password</label>
                <RouterLink to="/forgot-password" class="login-forgot">Quên mật khẩu?</RouterLink>
              </div>
              <div class="login-input-wrap">
                <span class="login-input-icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/>
                  </svg>
                </span>
                <input
                  id="login-password"
                  v-model="password"
                  :type="showPassword ? 'text' : 'password'"
                  autocomplete="current-password"
                  placeholder="Nhập mật khẩu"
                  class="login-input login-input--has-icon login-input--pass"
                  :disabled="isSubmitting"
                  @input="fieldErrors.password = ''"
                />
                <button type="button" class="login-pass-toggle" @click="showPassword = !showPassword" :aria-label="showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'">
                  <svg v-if="!showPassword" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                  </svg>
                  <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/>
                  </svg>
                </button>
              </div>
              <p v-if="fieldErrors.password" class="login-field-error">{{ fieldErrors.password }}</p>
            </div>

            <div class="login-options">
              <label class="login-remember">
                <input v-model="remember" type="checkbox" class="login-checkbox" />
                <span>Ghi nhớ đăng nhập</span>
              </label>
            </div>

            <button
              type="submit"
              class="login-submit"
              :disabled="isSubmitting"
            >
              <span v-if="isSubmitting" class="login-submit__spinner" aria-hidden="true" />
              <span>{{ isSubmitting ? 'Đang đăng nhập...' : 'Đăng nhập' }}</span>
              <svg v-if="!isSubmitting" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
                <path d="M5 12h14M12 5l7 7-7 7"/>
              </svg>
            </button>
          </form>

          <!-- Divider -->
          <div class="login-divider">
            <span>Chưa có tài khoản?</span>
          </div>

          <!-- Register CTA -->
          <RouterLink to="/register" class="login-register-cta">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M16 21v-2a4 4 0 00-4-4H6a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><line x1="19" y1="8" x2="19" y2="14"/><line x1="22" y1="11" x2="16" y2="11"/>
            </svg>
            Tạo tài khoản mới
          </RouterLink>

          <!-- Help -->
          <RouterLink to="/help-center" class="login-help">
            Cần hỗ trợ?
          </RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { login, resendVerification, redirectToSiteByDatabaseRole } from '../../services/authService'
import { useToast } from '../../composables/useToast'
import AppLogo from '../common/AppLogo.vue'

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
const globalError = ref('')

const fieldErrors = reactive({
  username: '',
  password: ''
})

const toast = useToast()

const onSubmit = async () => {
  globalError.value = ''
  fieldErrors.username = ''
  fieldErrors.password = ''

  if (!username.value.trim()) {
    fieldErrors.username = 'Vui lòng nhập tên đăng nhập.'
    return
  }
  if (!password.value) {
    fieldErrors.password = 'Vui lòng nhập mật khẩu.'
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
      resendEmail.value = username.value.includes('@') ? username.value : ''
      toast.error('Email chưa được xác minh.')
    } else {
      globalError.value = error?.payload?.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.'
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
    resendMessage.value = 'Không thể gửi email. Vui lòng thử lại.'
  } finally {
    isResending.value = false
  }
}
</script>

<style scoped>
/* ===== Page layout ===== */
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: stretch;
  background: #f8fafc;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.login-bg__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(79, 70, 229, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(79, 70, 229, 0.04) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: radial-gradient(ellipse 80% 80% at 50% 50%, black 0%, transparent 100%);
}

.login-bg__orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  animation: orb-float 12s ease-in-out infinite alternate;
}

.login-bg__orb--1 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.18) 0%, transparent 70%);
  top: -200px;
  left: -100px;
  animation-delay: 0s;
}

.login-bg__orb--2 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.15) 0%, transparent 70%);
  bottom: -150px;
  right: -100px;
  animation-delay: -4s;
}

.login-bg__orb--3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(79, 70, 229, 0.1) 0%, transparent 70%);
  top: 50%;
  left: 40%;
  transform: translate(-50%, -50%);
  animation-delay: -8s;
}

@keyframes orb-float {
  from { transform: translateY(0) scale(1); }
  to   { transform: translateY(-30px) scale(1.05); }
}

.login-container {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  min-height: 100vh;
}

/* ===== Left brand panel ===== */
.login-brand {
  flex: 0 0 45%;
  background: linear-gradient(145deg, #3730a3 0%, #4338ca 38%, #4f46e5 68%, #6366f1 100%);
  display: none;
  position: relative;
  overflow: hidden;
}

@media (min-width: 1024px) {
  .login-brand { display: flex; }
}

.login-brand::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 20%, rgba(255, 255, 255, 0.08) 0%, transparent 18%),
    radial-gradient(circle at 80% 0%, rgba(255, 255, 255, 0.06) 0%, transparent 22%),
    radial-gradient(circle at 100% 100%, rgba(129, 140, 248, 0.2) 0%, transparent 24%);
  pointer-events: none;
}

.login-brand__inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  padding: 48px 52px;
  height: 100%;
  animation: brand-enter 0.8s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes brand-enter {
  from { opacity: 0; transform: translateX(-20px); }
  to   { opacity: 1; transform: translateX(0); }
}

.login-brand__hero {
  margin-top: auto;
  margin-bottom: 40px;
  animation: brand-enter 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.15s both;
}

.login-brand__badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.8rem;
  font-weight: 500;
  margin-bottom: 24px;
  backdrop-filter: blur(8px);
}

.login-brand__headline {
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, sans-serif;
  font-size: 2.6rem;
  font-weight: 800;
  color: white;
  line-height: 1.15;
  letter-spacing: -0.03em;
  margin: 0 0 16px 0;
}

.login-brand__headline-accent {
  background: linear-gradient(135deg, #a5b4fc, #f472b6);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.login-brand__description {
  font-size: 1rem;
  color: rgba(255, 255, 255, 0.75);
  line-height: 1.65;
  margin: 0 0 32px 0;
  max-width: 380px;
}

.login-brand__features {
  display: flex;
  flex-direction: column;
  gap: 12px;
  animation: brand-enter 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.25s both;
}

.login-brand__feature {
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 0.9rem;
}

.login-brand__feature-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: rgba(255, 255, 255, 0.9);
}

.login-brand__stats {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-top: auto;
  padding-top: 40px;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
  animation: brand-enter 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.35s both;
}

.login-brand__stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.login-brand__stat strong {
  font-size: 1.4rem;
  font-weight: 800;
  color: white;
  font-family: 'Be Vietnam Pro', sans-serif;
}

.login-brand__stat span {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 500;
}

.login-brand__stat-divider {
  width: 1px;
  height: 36px;
  background: rgba(255, 255, 255, 0.15);
}

/* ===== Right form panel ===== */
.login-form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  overflow-y: auto;
}

.login-form-wrap {
  width: 100%;
  max-width: 440px;
  animation: form-enter 0.7s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes form-enter {
  from { opacity: 0; transform: translateY(16px); }
  to   { opacity: 1; transform: translateY(0); }
}

.login-form-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-form-logo {
  justify-content: center;
  margin-bottom: 20px;
}

.login-form-title {
  font-size: 1.75rem;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.025em;
  margin: 0 0 6px 0;
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, sans-serif;
}

.login-form-subtitle {
  font-size: 0.9rem;
  color: #64748b;
  margin: 0;
}

/* ===== Alerts ===== */
.login-alert {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 12px;
  margin-bottom: 20px;
  font-size: 0.85rem;
  animation: alert-enter 0.35s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes alert-enter {
  from { opacity: 0; transform: scale(0.97); }
  to   { opacity: 1; transform: scale(1); }
}

.login-alert--warning {
  background: #fffbeb;
  border: 1px solid #fde68a;
  color: #92400e;
}

.login-alert--error {
  flex-direction: row;
  align-items: center;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
}

.login-alert__close {
  margin-left: auto;
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px;
  opacity: 0.6;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.login-alert__close:hover { opacity: 1; }

.login-alert__resend {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.login-alert__resend-msg {
  font-size: 0.8rem;
  font-weight: 500;
  margin-top: 4px;
}

.login-alert__resend-msg--ok { color: #15803d; }
.login-alert__resend-msg--fail { color: #dc2626; }

/* ===== Form fields ===== */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.login-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.login-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.login-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #374151;
}

.login-forgot {
  font-size: 0.8rem;
  color: #4f46e5;
  text-decoration: none;
  font-weight: 500;
}

.login-forgot:hover { text-decoration: underline; }

.login-input-wrap {
  position: relative;
}

.login-input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
  display: flex;
  align-items: center;
  pointer-events: none;
  transition: color 0.2s ease;
}

.login-input {
  width: 100%;
  padding: 12px 14px;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  font-size: 0.9rem;
  font-family: inherit;
  color: #0f172a;
  background: white;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  box-sizing: border-box;
}

.login-input--has-icon {
  padding-left: 42px;
}

.login-input--pass {
  padding-right: 44px;
}

.login-input--sm {
  padding: 8px 12px;
  font-size: 0.85rem;
}

.login-input:focus {
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12);
}

.login-input:focus + .login-input-icon,
.login-input-wrap:focus-within .login-input-icon {
  color: #4f46e5;
}

.login-input:disabled {
  background: #f8fafc;
  opacity: 0.6;
  cursor: not-allowed;
}

.login-field--error .login-input {
  border-color: #ef4444;
}

.login-field--error .login-input:focus {
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.12);
}

.login-field-error {
  font-size: 0.78rem;
  color: #ef4444;
  font-weight: 500;
  margin: 0;
}

.login-pass-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: #9ca3af;
  display: flex;
  align-items: center;
  padding: 4px;
  border-radius: 6px;
  transition: color 0.2s ease, background 0.2s ease;
}

.login-pass-toggle:hover {
  color: #4f46e5;
  background: rgba(79, 70, 229, 0.06);
}

/* ===== Options & Submit ===== */
.login-options {
  display: flex;
  align-items: center;
}

.login-remember {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 0.85rem;
  color: #64748b;
  user-select: none;
}

.login-checkbox {
  width: 16px;
  height: 16px;
  border-radius: 5px;
  border: 1.5px solid #d1d5db;
  cursor: pointer;
  accent-color: #4f46e5;
}

.login-submit {
  width: 100%;
  padding: 13px 20px;
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 0.95rem;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: transform 0.15s ease, box-shadow 0.2s ease, opacity 0.2s ease;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.35);
  letter-spacing: 0.01em;
  margin-top: 4px;
}

.login-submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 22px rgba(79, 70, 229, 0.4);
}

.login-submit:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.3);
}

.login-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.login-submit__spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== Divider ===== */
.login-divider {
  text-align: center;
  margin: 24px 0 16px;
  position: relative;
}

.login-divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: #e2e8f0;
}

.login-divider span {
  position: relative;
  background: #f8fafc;
  padding: 0 12px;
  color: #94a3b8;
  font-size: 0.8rem;
}

/* ===== Register CTA ===== */
.login-register-cta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 12px 20px;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  color: #374151;
  font-size: 0.9rem;
  font-weight: 600;
  font-family: inherit;
  text-decoration: none;
  background: white;
  transition: border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.15s ease;
}

.login-register-cta:hover {
  border-color: #4f46e5;
  color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.08);
  transform: translateY(-1px);
}

.login-register-cta:active {
  transform: translateY(0);
}

/* ===== Help ===== */
.login-help {
  display: block;
  text-align: center;
  margin-top: 20px;
  font-size: 0.8rem;
  color: #94a3b8;
  text-decoration: none;
}

.login-help:hover { color: #4f46e5; text-decoration: underline; }

/* ===== Reduced motion ===== */
@media (prefers-reduced-motion: reduce) {
  .login-bg__orb,
  .login-brand__inner,
  .login-brand__hero,
  .login-brand__features,
  .login-brand__stats,
  .login-form-wrap,
  .login-alert,
  .login-submit__spinner {
    animation: none;
  }
  .login-submit:hover:not(:disabled),
  .login-register-cta:hover {
    transform: none;
  }
}
</style>
