<template>
  <div class="reg-page">
    <!-- Animated background -->
    <div class="reg-bg" aria-hidden="true">
      <div class="reg-bg__orb reg-bg__orb--1" />
      <div class="reg-bg__orb reg-bg__orb--2" />
      <div class="reg-bg__orb reg-bg__orb--3" />
      <div class="reg-bg__grid" />
    </div>

    <div class="reg-container">
      <!-- Left brand panel -->
      <div class="reg-brand">
        <div class="reg-brand__inner">
          <AppLogo size="lg" variant="brand" animate />

          <div class="reg-brand__hero">
            <div class="reg-brand__badge">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
              </svg>
              Tham gia cùng hàng nghìn người dùng
            </div>
            <h1 class="reg-brand__headline">
              Bắt đầu hành trình<br />
              <span class="reg-brand__headline-accent">học tập ngay hôm nay.</span>
            </h1>
            <p class="reg-brand__description">
              Đăng ký tài khoản EduExam miễn phí để tiếp cận hệ thống thi trực tuyến chất lượng cao với hàng nghìn đề thi phong phú.
            </p>
          </div>

          <div class="reg-brand__benefits">
            <div class="reg-brand__benefit">
              <span class="reg-brand__benefit-icon">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
              </span>
              <span>Tài khoản miễn phí, không giới hạn thời gian</span>
            </div>
            <div class="reg-brand__benefit">
              <span class="reg-brand__benefit-icon">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
              </span>
              <span>Tham gia thi thử không giới hạn</span>
            </div>
            <div class="reg-brand__benefit">
              <span class="reg-brand__benefit-icon">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
              </span>
              <span>Xem kết quả và phân tích chi tiết</span>
            </div>
            <div class="reg-brand__benefit">
              <span class="reg-brand__benefit-icon">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
              </span>
              <span>Hỗ trợ 24/7 từ đội ngũ EduExam</span>
            </div>
          </div>

          <div class="reg-brand__testimonial">
            <div class="reg-brand__testimonial-card">
              <p class="reg-brand__testimonial-text">
                "EduExam giúp tôi tiết kiệm hàng giờ mỗi tuần trong việc tạo và chấm thi. Giao diện rất trực quan!"
              </p>
              <div class="reg-brand__testimonial-author">
                <div class="reg-brand__testimonial-avatar">GV</div>
                <div>
                  <p class="reg-brand__testimonial-name">Giáo viên trường THPT Chu Văn An</p>
                  <p class="reg-brand__testimonial-role">Giáo viên</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Right form panel -->
      <div class="reg-form-panel">
        <div class="reg-form-wrap">
          <!-- Header -->
          <div class="reg-form-header">
            <AppLogo size="sm" show-text animate class="reg-form-logo" />
            <h2 class="reg-form-title">Tạo tài khoản mới</h2>
            <p class="reg-form-subtitle">Nhanh chóng, miễn phí và dễ dàng</p>
          </div>

          <!-- Server error -->
          <div v-if="serverError" class="reg-alert reg-alert--error">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            <span>{{ serverError }}</span>
            <button type="button" class="reg-alert__close" @click="serverError = ''" aria-label="Đóng">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>

          <!-- Form -->
          <form class="reg-form" @submit.prevent="onSubmit" novalidate>
            <div class="reg-field" :class="{ 'reg-field--error': errors.username }">
              <label class="reg-label" for="reg-username">Username</label>
              <div class="reg-input-wrap">
                <span class="reg-input-icon">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/>
                  </svg>
                </span>
                <input
                  id="reg-username"
                  v-model="username"
                  type="text"
                  autocomplete="username"
                  placeholder=" VD: nguyenvana"
                  class="reg-input reg-input--has-icon"
                  :disabled="isSubmitting"
                  @input="errors.username = ''"
                />
              </div>
              <p v-if="errors.username" class="reg-field-error">{{ errors.username }}</p>
            </div>

            <div class="reg-field" :class="{ 'reg-field--error': errors.email }">
              <label class="reg-label" for="reg-email">Email</label>
              <div class="reg-input-wrap">
                <span class="reg-input-icon">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/>
                  </svg>
                </span>
                <input
                  id="reg-email"
                  v-model="email"
                  type="email"
                  autocomplete="email"
                  placeholder=" VD: nguyen.van.a@email.com"
                  class="reg-input reg-input--has-icon"
                  :disabled="isSubmitting"
                  @input="errors.email = ''"
                />
              </div>
              <p v-if="errors.email" class="reg-field-error">{{ errors.email }}</p>
            </div>

            <div class="reg-field" :class="{ 'reg-field--error': errors.password }">
              <label class="reg-label" for="reg-password">
                Mật khẩu
                <span class="reg-label-hint">(Tối thiểu 8 ký tự)</span>
              </label>
              <div class="reg-input-wrap">
                <span class="reg-input-icon">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/>
                  </svg>
                </span>
                <input
                  id="reg-password"
                  v-model="password"
                  :type="showPassword ? 'text' : 'password'"
                  autocomplete="new-password"
                  placeholder="Nhập mật khẩu"
                  class="reg-input reg-input--has-icon reg-input--pass"
                  :disabled="isSubmitting"
                  @input="errors.password = ''"
                />
                <button type="button" class="reg-pass-toggle" @click="showPassword = !showPassword" :aria-label="showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'">
                  <svg v-if="!showPassword" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
                  </svg>
                  <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/>
                  </svg>
                </button>
              </div>
              <!-- Password strength -->
              <div v-if="password" class="reg-strength">
                <div class="reg-strength__bars">
                  <div
                    v-for="i in 4"
                    :key="i"
                    class="reg-strength__bar"
                    :class="{ 'reg-strength__bar--filled': passwordStrength >= i }"
                  />
                </div>
                <span class="reg-strength__label">{{ passwordStrengthLabel }}</span>
              </div>
              <p v-if="errors.password" class="reg-field-error">{{ errors.password }}</p>
            </div>

            <!-- Terms -->
            <label class="reg-terms">
              <input v-model="agreedToTerms" type="checkbox" class="reg-checkbox" :disabled="isSubmitting" />
              <span>
                Tôi đồng ý với
                <a href="#" class="reg-terms-link">Điều khoản dịch vụ</a>
                và
                <a href="#" class="reg-terms-link">Chính sách bảo mật</a>
              </span>
            </label>
            <p v-if="errors.terms" class="reg-field-error">{{ errors.terms }}</p>

            <button
              type="submit"
              class="reg-submit"
              :disabled="isSubmitting"
            >
              <span v-if="isSubmitting" class="reg-submit__spinner" aria-hidden="true" />
              <span>{{ isSubmitting ? 'Đang tạo tài khoản...' : 'Tạo tài khoản' }}</span>
              <svg v-if="!isSubmitting" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
                <path d="M5 12h14M12 5l7 7-7 7"/>
              </svg>
            </button>
          </form>

          <!-- Divider -->
          <div class="reg-divider">
            <span>Đã có tài khoản?</span>
          </div>

          <!-- Login CTA -->
          <button @click="goToLogin" class="reg-login-cta" :disabled="isSubmitting">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M15 3h4a2 2 0 012 2v14a2 2 0 01-2 2h-4"/><polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/>
            </svg>
            Đăng nhập ngay
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../../services/authService'
import { useToast } from '../../composables/useToast'
import AppLogo from '../common/AppLogo.vue'

const router = useRouter()

const username = ref('')
const email = ref('')
const password = ref('')
const agreedToTerms = ref(false)
const showPassword = ref(false)
const isSubmitting = ref(false)
const serverError = ref('')

const errors = reactive({
  username: '',
  email: '',
  password: '',
  terms: ''
})

const toast = useToast()

// Password strength: 0-4
const passwordStrength = computed(() => {
  const p = password.value
  if (!p) return 0
  let score = 0
  if (p.length >= 8) score++
  if (p.length >= 12) score++
  if (/[A-Z]/.test(p) && /[a-z]/.test(p)) score++
  if (/[0-9]/.test(p) || /[^A-Za-z0-9]/.test(p)) score++
  return Math.min(score, 4)
})

const passwordStrengthLabel = computed(() => {
  const labels = ['Rất yếu', 'Yếu', 'Trung bình', 'Mạnh']
  return labels[Math.max(0, passwordStrength.value - 1)] || ''
})

const validate = () => {
  let valid = true
  errors.username = ''
  errors.email = ''
  errors.password = ''
  errors.terms = ''

  const uname = username.value.trim()
  if (!uname) {
    errors.username = 'Vui lòng nhập username.'
    valid = false
  } else if (uname.length < 3) {
    errors.username = 'Username phải có ít nhất 3 ký tự.'
    valid = false
  } else if (!/^[a-zA-Z0-9_.-]+$/.test(uname)) {
    errors.username = 'Username chỉ chứa chữ, số, dấu gạch dưới, chấm và gạch ngang.'
    valid = false
  }

  const em = email.value.trim()
  if (!em) {
    errors.email = 'Vui lòng nhập email.'
    valid = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(em)) {
    errors.email = 'Email không hợp lệ.'
    valid = false
  }

  if (!password.value) {
    errors.password = 'Vui lòng nhập mật khẩu.'
    valid = false
  } else if (password.value.length < 8) {
    errors.password = 'Mật khẩu phải có ít nhất 8 ký tự.'
    valid = false
  }

  if (!agreedToTerms.value) {
    errors.terms = 'Bạn cần đồng ý với điều khoản để tiếp tục.'
    valid = false
  }

  return valid
}

const goToLogin = () => {
  router.push('/login')
}

const onSubmit = async () => {
  if (!validate()) return

  isSubmitting.value = true
  serverError.value = ''

  try {
    const data = await register({
      username: username.value.trim(),
      email: email.value.trim(),
      password: password.value
    })

    if (data?.verificationPending) {
      toast.info('Vui lòng xác minh email để đăng nhập.')
      const token = data?.verificationUrl
        ? data.verificationUrl.replace(/^.*token=/, '')
        : ''
      router.push({
        path: '/verify-email-pending',
        query: {
          email: email.value.trim(),
          emailSent: data?.emailSent ? 'true' : 'false',
          token,
          verificationUrl: data?.verificationUrl || ''
        }
      })
    } else if (data?.token) {
      toast.success('Tạo tài khoản thành công! Vui lòng chọn vai trò.')
      router.push('/select-role')
    } else {
      router.push('/login')
    }
  } catch (error) {
    if (error?.status === 409) {
      serverError.value = error?.payload?.message || 'Tài khoản hoặc email đã tồn tại.'
    } else {
      serverError.value = 'Đăng ký thất bại. Vui lòng thử lại.'
    }
    toast.error(serverError.value)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
/* ===== Page layout ===== */
.reg-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: stretch;
  background: #f8fafc;
  overflow: hidden;
}

.reg-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.reg-bg__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(79, 70, 229, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(79, 70, 229, 0.04) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: radial-gradient(ellipse 80% 80% at 50% 50%, black 0%, transparent 100%);
}

.reg-bg__orb {
  position: absolute;
  border-radius: 50%;
  opacity: 0.32;
  animation: orb-float 12s ease-in-out infinite alternate;
}

.reg-bg__orb--1 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.18) 0%, transparent 70%);
  top: -200px;
  right: -100px;
  animation-delay: 0s;
}

.reg-bg__orb--2 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.15) 0%, transparent 70%);
  bottom: -150px;
  left: -100px;
  animation-delay: -4s;
}

.reg-bg__orb--3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(79, 70, 229, 0.1) 0%, transparent 70%);
  top: 40%;
  right: 30%;
  animation-delay: -8s;
}

@keyframes orb-float {
  from { transform: translateY(0) scale(1); }
  to   { transform: translateY(-30px) scale(1.05); }
}

.reg-container {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  min-height: 100vh;
}

/* ===== Left brand panel ===== */
.reg-brand {
  flex: 0 0 45%;
  background: linear-gradient(145deg, #3730a3 0%, #4338ca 38%, #4f46e5 68%, #6366f1 100%);
  display: none;
  position: relative;
  overflow: hidden;
}

@media (min-width: 1024px) {
  .reg-brand { display: flex; }
}

.reg-brand::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.08) 0%, transparent 18%),
    radial-gradient(circle at 20% 80%, rgba(255, 255, 255, 0.06) 0%, transparent 22%),
    radial-gradient(circle at 0% 100%, rgba(129, 140, 248, 0.2) 0%, transparent 24%);
  pointer-events: none;
}

.reg-brand__inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  padding: 48px 52px;
  height: 100%;
  overflow-y: auto;
  animation: brand-enter 0.8s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes brand-enter {
  from { opacity: 0; }
  to   { opacity: 1; }
}

.reg-brand__hero {
  margin-top: auto;
  margin-bottom: 32px;
  animation: brand-enter 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.15s both;
}

.reg-brand__badge {
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
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.reg-brand__headline {
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, sans-serif;
  font-size: 2.2rem;
  font-weight: 800;
  color: white;
  line-height: 1.2;
  letter-spacing: -0.03em;
  margin: 0 0 16px 0;
}

.reg-brand__headline-accent {
  background: linear-gradient(135deg, #a5b4fc, #fbbf24);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.reg-brand__description {
  font-size: 0.95rem;
  color: rgba(255, 255, 255, 0.75);
  line-height: 1.65;
  margin: 0;
  max-width: 380px;
}

.reg-brand__benefits {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 32px;
  animation: brand-enter 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.25s both;
}

.reg-brand__benefit {
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(255, 255, 255, 0.88);
  font-size: 0.875rem;
}

.reg-brand__benefit-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(16, 185, 129, 0.25);
  border: 1px solid rgba(16, 185, 129, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #34d399;
}

.reg-brand__testimonial {
  margin-top: auto;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  animation: brand-enter 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.35s both;
}

.reg-brand__testimonial-card {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 14px;
  padding: 20px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.reg-brand__testimonial-text {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.6;
  margin: 0 0 14px 0;
  font-style: italic;
}

.reg-brand__testimonial-author {
  display: flex;
  align-items: center;
  gap: 10px;
}

.reg-brand__testimonial-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 700;
  color: white;
  flex-shrink: 0;
}

.reg-brand__testimonial-name {
  font-size: 0.8rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.reg-brand__testimonial-role {
  font-size: 0.72rem;
  color: rgba(255, 255, 255, 0.55);
  margin: 0;
}

/* ===== Right form panel ===== */
.reg-form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  overflow-y: auto;
}

.reg-form-wrap {
  width: 100%;
  max-width: 440px;
  animation: form-enter 0.7s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes form-enter {
  from { opacity: 0; }
  to   { opacity: 1; }
}

.reg-form-header {
  text-align: center;
  margin-bottom: 32px;
}

.reg-form-logo {
  justify-content: center;
  margin-bottom: 20px;
}

.reg-form-title {
  font-size: 1.75rem;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.025em;
  margin: 0 0 6px 0;
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, sans-serif;
}

.reg-form-subtitle {
  font-size: 0.9rem;
  color: #64748b;
  margin: 0;
}

/* ===== Alerts ===== */
.reg-alert {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 10px;
  margin-bottom: 20px;
  font-size: 0.85rem;
  animation: alert-enter 0.3s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes alert-enter {
  from { opacity: 0; transform: scale(0.97); }
  to   { opacity: 1; transform: scale(1); }
}

.reg-alert--error {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
}

.reg-alert__close {
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

.reg-alert__close:hover { opacity: 1; }

/* ===== Form fields ===== */
.reg-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.reg-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.reg-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #374151;
  display: flex;
  align-items: center;
  gap: 6px;
}

.reg-label-hint {
  font-size: 0.75rem;
  font-weight: 400;
  color: #94a3b8;
}

.reg-input-wrap {
  position: relative;
}

.reg-input-icon {
  position: absolute;
  left: 13px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
  display: flex;
  align-items: center;
  pointer-events: none;
  transition: color 0.2s ease;
}

.reg-input {
  width: 100%;
  padding: 11px 13px;
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

.reg-input--has-icon {
  padding-left: 40px;
}

.reg-input--pass {
  padding-right: 42px;
}

.reg-input:focus {
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12);
}

.reg-input:focus + .reg-input-icon,
.reg-input-wrap:focus-within .reg-input-icon {
  color: #4f46e5;
}

.reg-input:disabled {
  background: #f8fafc;
  opacity: 0.6;
  cursor: not-allowed;
}

.reg-field--error .reg-input {
  border-color: #ef4444;
}

.reg-field--error .reg-input:focus {
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
}

.reg-field-error {
  font-size: 0.78rem;
  color: #ef4444;
  font-weight: 500;
  margin: 0;
}

.reg-pass-toggle {
  position: absolute;
  right: 11px;
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

.reg-pass-toggle:hover {
  color: #4f46e5;
  background: rgba(79, 70, 229, 0.06);
}

/* ===== Password strength ===== */
.reg-strength {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.reg-strength__bars {
  display: flex;
  gap: 4px;
  flex: 1;
}

.reg-strength__bar {
  height: 4px;
  flex: 1;
  border-radius: 4px;
  background: #e2e8f0;
  transition: background 0.3s ease;
}

.reg-strength__bar--filled {
  background: #4f46e5;
}

.reg-strength__bar--filled:nth-child(1) { background: #ef4444; }
.reg-strength__bar--filled:nth-child(2) { background: #f59e0b; }
.reg-strength__bar--filled:nth-child(3) { background: #10b981; }
.reg-strength__bar--filled:nth-child(4) { background: #4f46e5; }

.reg-strength__label {
  font-size: 0.72rem;
  font-weight: 500;
  color: #94a3b8;
  min-width: 64px;
  text-align: right;
}

/* ===== Terms ===== */
.reg-terms {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  cursor: pointer;
  font-size: 0.82rem;
  color: #64748b;
  line-height: 1.5;
  user-select: none;
}

.reg-checkbox {
  width: 16px;
  height: 16px;
  border-radius: 5px;
  border: 1.5px solid #d1d5db;
  cursor: pointer;
  accent-color: #4f46e5;
  margin-top: 2px;
  flex-shrink: 0;
}

.reg-terms-link {
  color: #4f46e5;
  text-decoration: none;
  font-weight: 500;
}

.reg-terms-link:hover { text-decoration: underline; }

/* ===== Submit ===== */
.reg-submit {
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

.reg-submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 22px rgba(79, 70, 229, 0.4);
}

.reg-submit:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.3);
}

.reg-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.reg-submit__spinner {
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
.reg-divider {
  text-align: center;
  margin: 20px 0 14px;
  position: relative;
}

.reg-divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: #e2e8f0;
}

.reg-divider span {
  position: relative;
  background: #f8fafc;
  padding: 0 12px;
  color: #94a3b8;
  font-size: 0.8rem;
}

/* ===== Login CTA ===== */
.reg-login-cta {
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
  cursor: pointer;
  transition: border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease, transform 0.15s ease;
}

.reg-login-cta:hover:not(:disabled) {
  border-color: #4f46e5;
  color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.08);
  transform: translateY(-1px);
}

.reg-login-cta:active {
  transform: translateY(0);
}

.reg-login-cta:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ===== Reduced motion ===== */
@media (prefers-reduced-motion: reduce) {
  .reg-bg__orb,
  .reg-brand__inner,
  .reg-brand__hero,
  .reg-brand__benefits,
  .reg-brand__testimonial,
  .reg-form-wrap,
  .reg-alert,
  .reg-submit__spinner {
    animation: none;
  }
  .reg-submit:hover:not(:disabled),
  .reg-login-cta:hover:not(:disabled) {
    transform: none;
  }
}
</style>
