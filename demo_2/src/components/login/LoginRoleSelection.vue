<template>
  <div class="login-auth font-display text-slate-900 antialiased">
    <div class="login-auth__bg" aria-hidden="true" />

    <header class="login-auth__header">
      <RouterLink to="/" class="login-auth__brand group">
        <span class="login-auth__logo" aria-hidden="true">
          <span class="material-symbols-outlined text-[22px] text-white">school</span>
        </span>
        <span class="min-w-0">
          <span class="block truncate text-[0.95rem] font-extrabold tracking-tight text-[#1b1c1a] group-hover:text-[#6e3900]">
            EduExam
          </span>
          <span class="hidden text-[11px] font-medium text-slate-500 sm:block">Nền tảng thi trực tuyến</span>
        </span>
      </RouterLink>
      <RouterLink to="/help-center" class="login-auth__help"> Trợ giúp </RouterLink>
    </header>

    <main class="login-auth__main">
      <div class="login-auth__card">
        <div class="login-auth__accent" aria-hidden="true" />

        <div class="login-auth__body">
          <div class="login-auth__intro">
            <h1 class="login-auth__title">Đăng nhập</h1>
            <p class="login-auth__lead">
              Nhập tài khoản được cấp để vào khu giảng viên, học sinh hoặc quản trị.
            </p>
          </div>

          <div v-if="emailNotVerified" class="login-auth__alert">
            <p class="login-auth__alert-title">
              <span class="material-symbols-outlined text-[20px] text-amber-700" aria-hidden="true">mark_email_unread</span>
              Email chưa xác minh
            </p>
            <p class="login-auth__alert-text">
              Kiểm tra hộp thư hoặc nhập email bên dưới để gửi lại liên kết.
            </p>
            <div class="login-auth__alert-row">
              <BaseInput
                v-model="resendEmail"
                type="email"
                placeholder="Email đăng ký"
                input-class="login-auth__alert-input min-w-0 flex-1 py-2.5 text-sm"
              />
              <button
                type="button"
                class="login-auth__alert-btn"
                :disabled="isResending"
                @click="onResendVerification"
              >
                <span v-if="isResending" class="material-symbols-outlined animate-spin text-lg" aria-hidden="true">
                  progress_activity
                </span>
                {{ isResending ? 'Đang gửi…' : 'Gửi lại' }}
              </button>
            </div>
            <p
              v-if="resendMessage"
              class="login-auth__alert-msg"
              :class="resendSuccess ? 'login-auth__alert-msg--ok' : 'login-auth__alert-msg--err'"
            >
              <template v-if="resendSuccess && resendVerificationUrl">
                Demo:
                <RouterLink
                  :to="{ path: '/verify-email', query: { token: resendVerificationToken } }"
                  class="font-bold underline decoration-2 underline-offset-2 hover:no-underline"
                >
                  Mở liên kết xác minh
                </RouterLink>
              </template>
              <template v-else>{{ resendMessage }}</template>
            </p>
          </div>

          <form class="login-auth__form" @submit.prevent="onSubmit">
            <BaseField label="Tên đăng nhập">
              <template #default="{ inputId, hintId, errorId }">
                <div class="login-auth__field">
                  <span class="login-auth__icon material-symbols-outlined" aria-hidden="true">person</span>
                  <BaseInput
                    :id="inputId"
                    v-model="username"
                    autocomplete="username"
                    placeholder="Nhập tên đăng nhập"
                    input-class="login-auth__input login-auth__input--pad-left"
                    :hint-id="hintId"
                    :error-id="errorId"
                  />
                </div>
              </template>
            </BaseField>

            <BaseField label="Mật khẩu">
              <template #labelTrailing>
                <RouterLink to="/forgot-password" class="login-auth__link-muted"> Quên mật khẩu? </RouterLink>
              </template>
              <template #default="{ inputId, hintId, errorId }">
                <div class="login-auth__field">
                  <span class="login-auth__icon material-symbols-outlined" aria-hidden="true">lock</span>
                  <BaseInput
                    :id="inputId"
                    v-model="password"
                    :type="showPassword ? 'text' : 'password'"
                    autocomplete="current-password"
                    placeholder="Nhập mật khẩu"
                    input-class="login-auth__input login-auth__input--pad-both"
                    :hint-id="hintId"
                    :error-id="errorId"
                  />
                  <button
                    type="button"
                    class="login-auth__toggle"
                    :aria-pressed="showPassword"
                    :aria-label="showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
                    @click="showPassword = !showPassword"
                  >
                    <span class="material-symbols-outlined text-[22px]" aria-hidden="true">
                      {{ showPassword ? 'visibility_off' : 'visibility' }}
                    </span>
                  </button>
                </div>
              </template>
            </BaseField>

            <label class="login-auth__remember">
              <input id="remember" v-model="remember" type="checkbox" class="login-auth__checkbox" />
              <span>Ghi nhớ đăng nhập</span>
            </label>

            <BaseButton type="submit" size="lg" class="login-auth__submit w-full" :loading="isSubmitting">
              <span>{{ isSubmitting ? 'Đang đăng nhập…' : 'Đăng nhập' }}</span>
              <span class="material-symbols-outlined text-[22px]" aria-hidden="true">login</span>
            </BaseButton>
          </form>

          <p class="login-auth__footer">
            Chưa có tài khoản?
            <RouterLink to="/register" class="login-auth__link">Đăng ký</RouterLink>
          </p>
        </div>
      </div>

      <p class="login-auth__hint">
        Môi trường demo — không dùng mật khẩu mặc định trên hệ thống thật.
      </p>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { login, resendVerification, redirectToSiteByDatabaseRole } from '../../services/authService'
import { useToast } from '../../composables/useToast'
import BaseButton from '../shared/BaseButton.vue'
import BaseField from '../shared/BaseField.vue'
import BaseInput from '../shared/BaseInput.vue'

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
const resendVerificationUrl = ref('')
const resendVerificationToken = ref('')

const toast = useToast()

const onSubmit = async () => {
  if (!username.value || !password.value) {
    toast.error('Vui lòng nhập tên đăng nhập và mật khẩu.')
    return
  }

  isSubmitting.value = true
  emailNotVerified.value = false
  resendMessage.value = ''

  try {
    const authData = await login({
      username: username.value.trim(),
      password: password.value
    })

    await redirectToSiteByDatabaseRole(router, authData)
  } catch (error) {
    if (error?.status === 403 && String(error?.payload?.message || '').includes('EMAIL_NOT_VERIFIED')) {
      emailNotVerified.value = true
      resendEmail.value = ''
      toast.error('Email chưa được xác minh. Kiểm tra hộp thư hoặc gửi lại liên kết.')
    } else {
      toast.error('Đăng nhập không thành công. Kiểm tra tài khoản và thử lại.')
    }
  } finally {
    isSubmitting.value = false
  }
}

const onResendVerification = async () => {
  if (!resendEmail.value?.trim()) {
    toast.error('Vui lòng nhập email đăng ký.')
    return
  }
  isResending.value = true
  resendMessage.value = ''
  resendVerificationUrl.value = ''
  resendVerificationToken.value = ''
  try {
    const data = await resendVerification({ email: resendEmail.value.trim() })
    resendSuccess.value = true
    if (data?.verificationUrl) {
      resendVerificationUrl.value = data.verificationUrl
      resendVerificationToken.value = data.verificationUrl.replace(/^.*token=/, '')
      resendMessage.value = 'Demo: dùng liên kết bên dưới để xác minh.'
    } else {
      resendMessage.value = 'Đã gửi email xác minh. Vui lòng kiểm tra hộp thư.'
    }
  } catch {
    resendSuccess.value = false
    resendMessage.value = 'Không gửi được. Email có thể đã xác minh hoặc không tồn tại.'
  } finally {
    isResending.value = false
  }
}
</script>

<style scoped>
.login-auth {
  position: relative;
  isolation: isolate;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  overflow-x: clip;
  background: #faf9f5;
}

.login-auth__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background:
    radial-gradient(ellipse 120% 80% at 50% -20%, rgba(177, 95, 0, 0.09), transparent 50%),
    radial-gradient(circle at 100% 60%, rgba(255, 183, 125, 0.12), transparent 42%),
    radial-gradient(circle at 0% 80%, rgba(141, 75, 0, 0.05), transparent 45%),
    linear-gradient(180deg, #faf9f5 0%, #f4f4f0 100%);
}

.login-auth__header {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid rgba(219, 194, 176, 0.35);
  background: rgba(250, 249, 245, 0.82);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

@media (min-width: 640px) {
  .login-auth__header {
    padding: 0.85rem 1.5rem;
  }
}

.login-auth__brand {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 0.65rem;
  text-decoration: none;
  color: inherit;
}

.login-auth__logo {
  display: flex;
  width: 2.25rem;
  height: 2.25rem;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 0.85rem;
  background: linear-gradient(135deg, #8d4b00 0%, #b15f00 100%);
  box-shadow: 0 12px 28px -16px rgba(74, 40, 0, 0.45);
}

.login-auth__help {
  flex-shrink: 0;
  border-radius: 999px;
  border: 1px solid rgba(219, 194, 176, 0.55);
  background: rgba(255, 255, 255, 0.88);
  padding: 0.45rem 0.95rem;
  font-size: 0.8125rem;
  font-weight: 700;
  color: #554336;
  text-decoration: none;
  transition: background-color 0.15s ease, border-color 0.15s ease;
}

.login-auth__help:hover {
  background: #fff;
  border-color: rgba(141, 75, 0, 0.28);
}

.login-auth__main {
  position: relative;
  z-index: 1;
  flex: 1 1 auto;
  display: flex;
  min-height: 0;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 1.25rem 1rem 1.5rem;
  overflow-y: auto;
  overscroll-behavior: contain;
}

@media (min-width: 640px) {
  .login-auth__main {
    padding: 2rem 1.5rem;
  }
}

.login-auth__card {
  position: relative;
  width: 100%;
  max-width: 26rem;
  border-radius: 1.25rem;
  border: 1px solid rgba(219, 194, 176, 0.45);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(250, 249, 245, 0.96) 100%);
  box-shadow:
    0 32px 64px -40px rgba(74, 40, 0, 0.22),
    0 16px 32px -24px rgba(27, 28, 26, 0.08);
  overflow: hidden;
}

.login-auth__accent {
  height: 4px;
  width: 100%;
  background: linear-gradient(90deg, #6e3900 0%, #b15f00 40%, #d4a574 100%);
}

.login-auth__body {
  padding: 1.75rem 1.5rem 1.5rem;
}

@media (min-width: 640px) {
  .login-auth__body {
    padding: 2rem 2rem 1.75rem;
  }
}

.login-auth__intro {
  margin-bottom: 1.5rem;
}

.login-auth__title {
  margin: 0;
  font-family: 'Noto Serif', Georgia, 'Times New Roman', serif;
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: -0.03em;
  line-height: 1.2;
  color: #1b1c1a;
}

@media (min-width: 640px) {
  .login-auth__title {
    font-size: 1.75rem;
  }
}

.login-auth__lead {
  margin: 0.5rem 0 0;
  font-size: 0.9rem;
  line-height: 1.55;
  color: #64748b;
}

.login-auth__alert {
  margin-bottom: 1.25rem;
  border-radius: 0.85rem;
  border: 1px solid rgba(245, 158, 11, 0.35);
  background: linear-gradient(180deg, rgba(255, 251, 235, 0.98) 0%, rgba(255, 247, 237, 0.95) 100%);
  padding: 1rem;
  box-shadow: 0 16px 32px -24px rgba(245, 158, 11, 0.35);
}

.login-auth__alert-title {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin: 0 0 0.35rem;
  font-size: 0.9rem;
  font-weight: 800;
  color: #92400e;
}

.login-auth__alert-text {
  margin: 0 0 0.75rem;
  font-size: 0.8125rem;
  line-height: 1.45;
  color: #78716c;
}

.login-auth__alert-row {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

@media (min-width: 480px) {
  .login-auth__alert-row {
    flex-direction: row;
    align-items: stretch;
  }
}

.login-auth__alert-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  border-radius: 0.75rem;
  border: none;
  background: #b45309;
  padding: 0.65rem 1rem;
  font-size: 0.8125rem;
  font-weight: 800;
  color: #fff;
  cursor: pointer;
  transition: background-color 0.15s ease;
  white-space: nowrap;
}

.login-auth__alert-btn:hover:not(:disabled) {
  background: #92400e;
}

.login-auth__alert-btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.login-auth__alert-msg {
  margin: 0.65rem 0 0;
  font-size: 0.8125rem;
  line-height: 1.45;
}

.login-auth__alert-msg--ok {
  color: #059669;
}

.login-auth__alert-msg--err {
  color: #e11d48;
}

.login-auth__form {
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
}

.login-auth__field {
  position: relative;
  display: flex;
  align-items: center;
}

.login-auth__icon {
  position: absolute;
  left: 0.85rem;
  z-index: 2;
  pointer-events: none;
  font-size: 1.25rem;
  color: #94a3b8;
  top: 50%;
  transform: translateY(-50%);
}

.login-auth__toggle {
  position: absolute;
  right: 0.35rem;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  border: none;
  border-radius: 0.65rem;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.login-auth__toggle:hover {
  background: rgba(15, 23, 42, 0.06);
  color: #8d4b00;
}

.login-auth__link-muted {
  font-size: 0.75rem;
  font-weight: 800;
  color: #8d4b00;
  text-decoration: none;
}

.login-auth__link-muted:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}

.login-auth__remember {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: #64748b;
  cursor: pointer;
  user-select: none;
}

.login-auth__checkbox {
  width: 1rem;
  height: 1rem;
  border-radius: 0.25rem;
  border: 1px solid #cbd5e1;
  accent-color: #8d4b00;
  cursor: pointer;
}

.login-auth__footer {
  margin: 1.5rem 0 0;
  text-align: center;
  font-size: 0.875rem;
  color: #64748b;
}

.login-auth__link {
  margin-left: 0.25rem;
  font-weight: 800;
  color: #8d4b00;
  text-decoration: none;
}

.login-auth__link:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}

.login-auth__hint {
  margin: 1.25rem 0 0;
  max-width: 26rem;
  text-align: center;
  font-size: 0.75rem;
  line-height: 1.5;
  color: #94a3b8;
}

/* BaseInput: amber focus, bỏ viền indigo mặc định — khớp thương hiệu */
.login-auth :deep(.base-input) {
  border-radius: 0.85rem;
  border-color: rgba(148, 163, 184, 0.28);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(250, 249, 245, 0.96) 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 1px 2px rgba(15, 23, 42, 0.04);
}

.login-auth :deep(.base-input:hover) {
  border-color: rgba(141, 75, 0, 0.22);
}

.login-auth :deep(.base-input:focus) {
  border-color: rgba(141, 75, 0, 0.45);
  background: #fff;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 1),
    0 0 0 4px rgba(141, 75, 0, 0.14);
}

/* input-class gắn trên BaseInput — phải :deep() nếu không padding không áp dụng → placeholder chồng icon */
.login-auth :deep(.login-auth__input--pad-left) {
  padding-left: 2.75rem !important;
}

.login-auth :deep(.login-auth__input--pad-both) {
  padding-left: 2.75rem !important;
  padding-right: 3.1rem !important;
}

.login-auth :deep(.login-auth__alert-input) {
  border-radius: 0.75rem !important;
}

/* Nút submit: gradient amber khớp trang */
.login-auth :deep(.login-auth__submit.base-button--primary) {
  border: 1px solid rgba(141, 75, 0, 0.22);
  background: linear-gradient(135deg, #b15f00 0%, #8d4b00 52%, #6e3900 100%);
  color: #fff;
  box-shadow: 0 16px 28px -18px rgba(74, 40, 0, 0.45);
}

.login-auth :deep(.login-auth__submit.base-button--primary:hover:not(:disabled)) {
  background: linear-gradient(135deg, #c46a00 0%, #9d5200 52%, #7a4300 100%);
}

@media (prefers-reduced-motion: reduce) {
  .login-auth__help,
  .login-auth :deep(.base-button) {
    transition: none;
  }
}
</style>
