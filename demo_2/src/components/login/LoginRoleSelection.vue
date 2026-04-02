<template>
  <div class="sm-app sl-page">
    <div class="sl-container">
      <!-- Login Card -->
      <div class="sl-card">
        <!-- Logo & Header -->
        <div class="sl-header">
          <div class="sl-logo">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="48" height="48" rx="12" fill="var(--sm-accent-muted)"/>
              <path d="M24 10L14 16V32L24 38L34 32V16L24 10Z" stroke="var(--sm-accent-primary)" stroke-width="2" stroke-linejoin="round" fill="none"/>
              <circle cx="24" cy="24" r="4" fill="var(--sm-accent-primary)"/>
            </svg>
          </div>
          <h1 class="sl-title">EduExam</h1>
          <p class="sl-subtitle">Đăng nhập để tiếp tục</p>
        </div>

        <!-- Email Not Verified Alert -->
        <div v-if="emailNotVerified" class="sl-alert sl-alert--warning">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
            <polyline points="22,6 12,13 2,6"/>
          </svg>
          <div class="sl-alert__content">
            <p class="sl-alert__title">Email chưa xác minh</p>
            <p class="sl-alert__desc">Kiểm tra hộp thư hoặc nhập email bên dưới để gửi lại.</p>
            <div class="sl-alert__form">
              <input
                v-model="resendEmail"
                type="email"
                placeholder="Email đăng ký"
                class="sm-input"
              />
              <button
                type="button"
                class="sm-btn sm-btn--secondary sm-btn--sm"
                :disabled="isResending"
                @click="onResendVerification"
              >
                {{ isResending ? 'Đang gửi...' : 'Gửi lại' }}
              </button>
            </div>
            <p v-if="resendMessage" class="sl-alert__message" :class="resendSuccess ? 'sl-alert__message--success' : 'sl-alert__message--error'">
              {{ resendMessage }}
            </p>
          </div>
        </div>

        <!-- Demo Quick Fill -->
        <div class="sl-demo">
          <span class="sl-demo__label">Demo nhanh</span>
          <div class="sl-demo__chips">
            <button type="button" class="sl-demo-chip" @click="fillDemo('teacher')">
              <span class="sl-demo-chip__badge">GV</span>
              <span>Giáo viên</span>
            </button>
            <button type="button" class="sl-demo-chip" @click="fillDemo('student')">
              <span class="sl-demo-chip__badge">HS</span>
              <span>Học sinh</span>
            </button>
            <button type="button" class="sl-demo-chip" @click="fillDemo('admin')">
              <span class="sl-demo-chip__badge sl-demo-chip__badge--warning">AD</span>
              <span>Admin</span>
            </button>
          </div>
        </div>

        <!-- Form -->
        <form @submit.prevent="handleLogin" class="sl-form">
          <div class="sm-field" :class="{ 'sm-field--error': errors.username }">
            <label class="sm-field__label">Tên đăng nhập</label>
            <input
              id="username"
              v-model="form.username"
              class="sm-input"
              type="text"
              placeholder="Nhập tên đăng nhập"
              autocomplete="username"
            />
            <span v-if="errors.username" class="sm-field__error">{{ errors.username }}</span>
          </div>

          <div class="sm-field" :class="{ 'sm-field--error': errors.password }">
            <label class="sm-field__label">Mật khẩu</label>
            <div class="sl-password-wrapper">
              <input
                id="password"
                v-model="form.password"
                class="sm-input"
                :type="showPassword ? 'text' : 'password'"
                placeholder="Nhập mật khẩu"
                autocomplete="current-password"
              />
              <button
                type="button"
                class="sl-password-toggle"
                @click="showPassword = !showPassword"
              >
                <svg v-if="showPassword" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                  <line x1="1" y1="1" x2="23" y2="23"/>
                </svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
              </button>
            </div>
            <span v-if="errors.password" class="sm-field__error">{{ errors.password }}</span>
          </div>

          <div class="sl-form__options">
            <label class="sl-checkbox">
              <input v-model="form.remember" type="checkbox" />
              <span>Ghi nhớ đăng nhập</span>
            </label>
            <RouterLink to="/forgot-password" class="sl-link">
              Quên mật khẩu?
            </RouterLink>
          </div>

          <button
            type="submit"
            class="sm-btn sm-btn--primary sl-submit"
            :disabled="isSubmitting"
          >
            <span v-if="isSubmitting" class="sl-spinner"></span>
            <template v-else>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/>
                <polyline points="10 17 15 12 10 7"/>
                <line x1="15" y1="12" x2="3" y2="12"/>
              </svg>
              Đăng nhập
            </template>
          </button>
        </form>

        <!-- Success State -->
        <div v-if="isSuccess" class="sl-success">
          <div class="sl-success__icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
          </div>
          <p class="sl-success__text">Đăng nhập thành công!</p>
        </div>

        <!-- Footer -->
        <div class="sl-footer">
          <p>
            Chưa có tài khoản?
            <RouterLink to="/register" class="sl-link">Đăng ký ngay</RouterLink>
          </p>
        </div>

        <p class="sl-hint">
          Môi trường demo — không dùng mật khẩu mặc định trên hệ thống thật.
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { login, resendVerification, redirectToSiteByDatabaseRole } from '../../services/authService'
import { useToast } from '../../composables/useToast'

const router = useRouter()
const toast = useToast()

const form = reactive({
  username: '',
  password: '',
  remember: false
})

const errors = reactive({
  username: '',
  password: ''
})

const showPassword = ref(false)
const isSubmitting = ref(false)
const isSuccess = ref(false)
const emailNotVerified = ref(false)
const resendEmail = ref('')
const isResending = ref(false)
const resendMessage = ref('')
const resendSuccess = ref(false)

const fillDemo = (role) => {
  const creds = {
    teacher: { username: 'nguyenvana', password: 'password123' },
    student: { username: 'student01', password: 'password123' },
    admin: { username: 'admin01', password: 'admin123' }
  }
  const c = creds[role] || creds.teacher
  form.username = c.username
  form.password = c.password
  errors.username = ''
  errors.password = ''
  toast.info(`Đã điền demo: ${role}`)
}

const handleLogin = async () => {
  errors.username = ''
  errors.password = ''
  
  if (!form.username.trim()) {
    errors.username = 'Vui lòng nhập tên đăng nhập'
    return
  }

  if (!form.password) {
    errors.password = 'Vui lòng nhập mật khẩu'
    return
  }

  isSubmitting.value = true
  emailNotVerified.value = false
  resendMessage.value = ''

  try {
    const authData = await login({
      username: form.username.trim(),
      password: form.password
    })

    isSuccess.value = true

    setTimeout(async () => {
      await redirectToSiteByDatabaseRole(router, authData)
    }, 800)
  } catch (error) {
    if (error?.status === 403 && String(error?.payload?.message || '').includes('EMAIL_NOT_VERIFIED')) {
      emailNotVerified.value = true
      resendEmail.value = ''
      toast.error('Email chưa được xác minh. Kiểm tra hộp thư hoặc gửi lại liên kết.')
    } else {
      toast.error('Đăng nhập không thành công. Kiểm tra tài khoản và thử lại.')
    }
  } finally {
    if (!isSuccess.value) {
      isSubmitting.value = false
    }
  }
}

const onResendVerification = async () => {
  if (!resendEmail.value?.trim()) {
    toast.error('Vui lòng nhập email đăng ký.')
    return
  }
  isResending.value = true
  resendMessage.value = ''
  
  try {
    const data = await resendVerification({ email: resendEmail.value.trim() })
    resendSuccess.value = true
    resendMessage.value = data?.verificationUrl 
      ? 'Đã gửi email xác minh. Kiểm tra hộp thư.'
      : 'Đã gửi email xác minh. Vui lòng kiểm tra hộp thư.'
  } catch {
    resendSuccess.value = false
    resendMessage.value = 'Không gửi được. Email có thể đã xác minh hoặc không tồn tại.'
  } finally {
    isResending.value = false
  }
}
</script>

<style scoped>
.sl-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--sm-bg-primary) 0%, var(--sm-bg-tertiary) 100%);
  padding: var(--sm-space-6);
}

.sl-container {
  width: 100%;
  max-width: 420px;
}

.sl-card {
  background: var(--sm-bg-secondary);
  border: 1px solid var(--sm-border-default);
  border-radius: var(--sm-radius-xl);
  padding: var(--sm-space-8);
  box-shadow: var(--sm-shadow-lg);
}

/* Header */
.sl-header {
  text-align: center;
  margin-bottom: var(--sm-space-8);
}

.sl-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--sm-space-4);
}

.sl-title {
  font-family: var(--sm-font-display);
  font-size: var(--sm-text-2xl);
  font-weight: 400;
  color: var(--sm-text-primary);
  margin-bottom: var(--sm-space-2);
  letter-spacing: -0.02em;
}

.sl-subtitle {
  font-size: var(--sm-text-sm);
  color: var(--sm-text-secondary);
}

/* Alert */
.sl-alert {
  display: flex;
  gap: var(--sm-space-3);
  padding: var(--sm-space-4);
  border-radius: var(--sm-radius-md);
  margin-bottom: var(--sm-space-6);
}

.sl-alert--warning {
  background: var(--sm-warning-bg);
  border: 1px solid var(--sm-warning-border);
}

.sl-alert svg {
  color: var(--sm-warning-text);
  flex-shrink: 0;
  margin-top: 2px;
}

.sl-alert__content {
  flex: 1;
}

.sl-alert__title {
  font-size: var(--sm-text-sm);
  font-weight: 600;
  color: var(--sm-warning-text);
  margin-bottom: var(--sm-space-1);
}

.sl-alert__desc {
  font-size: var(--sm-text-xs);
  color: var(--sm-text-secondary);
  margin-bottom: var(--sm-space-3);
}

.sl-alert__form {
  display: flex;
  gap: var(--sm-space-2);
}

.sl-alert__form .sm-input {
  flex: 1;
}

.sl-alert__message {
  font-size: var(--sm-text-xs);
  margin-top: var(--sm-space-2);
}

.sl-alert__message--success {
  color: var(--sm-success-text);
}

.sl-alert__message--error {
  color: var(--sm-error-text);
}

/* Demo */
.sl-demo {
  padding: var(--sm-space-4);
  background: var(--sm-bg-tertiary);
  border-radius: var(--sm-radius-md);
  margin-bottom: var(--sm-space-6);
}

.sl-demo__label {
  display: block;
  font-size: var(--sm-text-xs);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--sm-text-tertiary);
  margin-bottom: var(--sm-space-3);
}

.sl-demo__chips {
  display: flex;
  gap: var(--sm-space-2);
  flex-wrap: wrap;
}

.sl-demo-chip {
  display: inline-flex;
  align-items: center;
  gap: var(--sm-space-2);
  padding: var(--sm-space-2) var(--sm-space-3);
  background: var(--sm-bg-secondary);
  border: 1px solid var(--sm-border-default);
  border-radius: var(--sm-radius-full);
  font-size: var(--sm-text-sm);
  font-weight: 500;
  color: var(--sm-text-primary);
  cursor: pointer;
  transition: all var(--sm-duration-fast) var(--sm-ease-out);
}

.sl-demo-chip:hover {
  border-color: var(--sm-accent-primary);
  color: var(--sm-accent-primary);
}

.sl-demo-chip__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: var(--sm-radius-sm);
  background: var(--sm-accent-muted);
  color: var(--sm-accent-primary);
  font-size: var(--sm-text-xs);
  font-weight: 700;
}

.sl-demo-chip__badge--warning {
  background: var(--sm-warning-bg);
  color: var(--sm-warning-text);
}

/* Form */
.sl-form {
  display: flex;
  flex-direction: column;
  gap: var(--sm-space-5);
}

.sl-password-wrapper {
  position: relative;
}

.sl-password-wrapper .sm-input {
  padding-right: 44px;
}

.sl-password-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  padding: 4px;
  color: var(--sm-text-tertiary);
  cursor: pointer;
  transition: color var(--sm-duration-fast);
}

.sl-password-toggle:hover {
  color: var(--sm-text-primary);
}

.sl-form__options {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sl-checkbox {
  display: flex;
  align-items: center;
  gap: var(--sm-space-2);
  font-size: var(--sm-text-sm);
  color: var(--sm-text-secondary);
  cursor: pointer;
}

.sl-checkbox input {
  width: 16px;
  height: 16px;
  accent-color: var(--sm-accent-primary);
}

.sl-link {
  font-size: var(--sm-text-sm);
  font-weight: 500;
  color: var(--sm-accent-primary);
  text-decoration: none;
  transition: color var(--sm-duration-fast);
}

.sl-link:hover {
  color: var(--sm-accent-hover);
  text-decoration: underline;
}

.sl-submit {
  width: 100%;
  height: 44px;
  font-size: var(--sm-text-base);
}

.sl-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: sl-spin 0.6s linear infinite;
}

@keyframes sl-spin {
  to { transform: rotate(360deg); }
}

/* Success */
.sl-success {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--sm-bg-secondary);
  border-radius: var(--sm-radius-xl);
  animation: sl-fade-in 0.3s var(--sm-ease-out);
}

@keyframes sl-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

.sl-success__icon {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--sm-success-bg);
  color: var(--sm-success-text);
  border-radius: 50%;
  margin-bottom: var(--sm-space-4);
}

.sl-success__text {
  font-size: var(--sm-text-lg);
  font-weight: 600;
  color: var(--sm-success-text);
}

/* Footer */
.sl-footer {
  margin-top: var(--sm-space-6);
  padding-top: var(--sm-space-6);
  border-top: 1px solid var(--sm-border-subtle);
  text-align: center;
}

.sl-footer p {
  font-size: var(--sm-text-sm);
  color: var(--sm-text-secondary);
}

.sl-hint {
  margin-top: var(--sm-space-4);
  text-align: center;
  font-size: var(--sm-text-xs);
  color: var(--sm-text-tertiary);
}

/* Responsive */
@media (max-width: 480px) {
  .sl-card {
    padding: var(--sm-space-6);
  }

  .sl-demo__chips {
    flex-direction: column;
  }

  .sl-demo-chip {
    width: 100%;
    justify-content: center;
  }
}
</style>
