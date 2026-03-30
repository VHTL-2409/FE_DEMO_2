<template>
  <div class="ss">
    <!-- Card header -->
    <div class="ss__header">
      <div class="ss__header-icon">
        <LucideIcon name="lock" />
      </div>
      <div>
        <h3 class="ss__title">Bảo mật</h3>
        <p class="ss__subtitle">Đổi mật khẩu và quản lý bảo mật tài khoản</p>
      </div>
    </div>

    <!-- Content -->
    <div class="ss__content">

      <!-- Change Password -->
      <div class="ss__section">
        <div class="ss__section-header">
          <div class="ss__section-icon">
            <LucideIcon name="key" />
          </div>
          <div>
            <h4 class="ss__section-title">Đổi mật khẩu</h4>
            <p class="ss__section-desc">Cập nhật mật khẩu để bảo vệ tài khoản</p>
          </div>
          <button
            type="button"
            class="ss__toggle-btn"
            :class="{ 'ss__toggle-btn--open': isChangingPassword }"
            @click="isChangingPassword = !isChangingPassword"
          >
            <LucideIcon :name="isChangingPassword ? 'expand_less' : 'expand_more'" />
            {{ isChangingPassword ? 'Đóng' : 'Thay đổi' }}
          </button>
        </div>

        <div v-if="isChangingPassword" class="ss__expand-body">
          <form class="ss__form" @submit.prevent="submitPasswordChange">
            <div class="ss__form-group">
              <label class="ss__label">
                Mật khẩu hiện tại
                <span class="ss__required">*</span>
              </label>
              <div class="ss__input-wrap">
                <input
                  v-model="passwordForm.currentPassword"
                  class="ss__input"
                  :class="{ 'ss__input--error': errors.currentPassword }"
                  :type="showCurrent ? 'text' : 'password'"
                  placeholder="Nhập mật khẩu hiện tại"
                  autocomplete="current-password"
                />
                <button
                  type="button"
                  class="ss__eye-toggle"
                  @click="showCurrent = !showCurrent"
                >
                  <LucideIcon :name="showCurrent ? 'visibility_off' : 'visibility'" />
                </button>
              </div>
              <span v-if="errors.currentPassword" class="ss__error-msg">{{ errors.currentPassword }}</span>
            </div>

            <div class="ss__form-group">
              <label class="ss__label">
                Mật khẩu mới
                <span class="ss__required">*</span>
              </label>
              <div class="ss__input-wrap">
                <input
                  v-model="passwordForm.newPassword"
                  class="ss__input"
                  :class="{ 'ss__input--error': errors.newPassword }"
                  :type="showNew ? 'text' : 'password'"
                  placeholder="Ít nhất 6 ký tự"
                  autocomplete="new-password"
                />
                <button
                  type="button"
                  class="ss__eye-toggle"
                  @click="showNew = !showNew"
                >
                  <LucideIcon :name="showNew ? 'visibility_off' : 'visibility'" />
                </button>
              </div>
              <!-- Password strength indicator -->
              <div v-if="passwordForm.newPassword" class="ss__strength">
                <div class="ss__strength-bars">
                  <div
                    v-for="i in 4"
                    :key="i"
                    class="ss__strength-bar"
                    :class="getStrengthClass(i)"
                  />
                </div>
                <span class="ss__strength-label" :class="getStrengthClass(0)">
                  {{ strengthLabel }}
                </span>
              </div>
              <span v-if="errors.newPassword" class="ss__error-msg">{{ errors.newPassword }}</span>
            </div>

            <div class="ss__form-group">
              <label class="ss__label">
                Xác nhận mật khẩu mới
                <span class="ss__required">*</span>
              </label>
              <div class="ss__input-wrap">
                <input
                  v-model="passwordForm.confirmPassword"
                  class="ss__input"
                  :class="{ 'ss__input--error': errors.confirmPassword }"
                  :type="showConfirm ? 'text' : 'password'"
                  placeholder="Nhập lại mật khẩu mới"
                  autocomplete="new-password"
                />
                <button
                  type="button"
                  class="ss__eye-toggle"
                  @click="showConfirm = !showConfirm"
                >
                  <LucideIcon :name="showConfirm ? 'visibility_off' : 'visibility'" />
                </button>
              </div>
              <!-- Match indicator -->
              <div v-if="passwordForm.confirmPassword" class="ss__match-indicator">
                <LucideIcon :name="passwordsMatch ? 'check_circle' : 'x'" :style="{ color: passwordsMatch ? 'var(--ds-success)' : 'var(--ds-danger)' }" />
                {{ passwordsMatch ? 'Mật khẩu khớp' : 'Mật khẩu không khớp' }}
              </div>
              <span v-if="errors.confirmPassword" class="ss__error-msg">{{ errors.confirmPassword }}</span>
            </div>

            <div class="ss__form-actions">
              <button
                type="button"
                class="ss__btn ss__btn--cancel"
                :disabled="isSavingPassword"
                @click="closePasswordForm"
              >
                Hủy bỏ
              </button>
              <button
                type="submit"
                class="ss__btn ss__btn--save"
                :disabled="isSavingPassword || !passwordsMatch"
              >
                <LucideIcon name="progress_activity" v-if="isSavingPassword"  ss__spinner/>
                {{ isSavingPassword ? 'Đang cập nhật...' : 'Cập nhật mật khẩu' }}
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Security tips -->
      <div class="ss__section">
        <div class="ss__section-header">
          <div class="ss__section-icon ss__section-icon--warning">
            <LucideIcon name="tips_and_updates" />
          </div>
          <div>
            <h4 class="ss__section-title">Mẹo bảo mật</h4>
            <p class="ss__section-desc">Bảo vệ tài khoản của bạn tốt hơn</p>
          </div>
        </div>
        <div class="ss__tips">
          <div v-for="tip in securityTips" :key="tip.title" class="ss__tip">
            <LucideIcon :name="tip.icon" />
            <div>
              <strong>{{ tip.title }}</strong>
              <p>{{ tip.desc }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Login sessions placeholder -->
      <div class="ss__section">
        <div class="ss__section-header">
          <div class="ss__section-icon">
            <LucideIcon name="devices" />
          </div>
          <div>
            <h4 class="ss__section-title">Phiên đăng nhập</h4>
            <p class="ss__section-desc">Các thiết bị đã đăng nhập gần đây</p>
          </div>
        </div>
        <div class="ss__sessions">
          <div class="ss__session-item">
            <div class="ss__session-icon">
              <LucideIcon name="computer" />
            </div>
            <div class="ss__session-info">
              <span class="ss__session-device">Máy tính hiện tại</span>
              <span class="ss__session-meta">Trình duyệt · Đang hoạt động</span>
            </div>
            <span class="ss__session-badge ss__session-badge--active">
              <LucideIcon name="check_circle" />
              Hiện tại
            </span>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'

const emit = defineEmits(['change-password'])

const isChangingPassword = ref(false)
const isSavingPassword = ref(false)
const showCurrent = ref(false)
const showNew = ref(false)
const showConfirm = ref(false)
const errors = ref({})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordsMatch = computed(() => {
  return (
    passwordForm.newPassword.length > 0 &&
    passwordForm.confirmPassword.length > 0 &&
    passwordForm.newPassword === passwordForm.confirmPassword
  )
})

const passwordStrength = computed(() => {
  const p = passwordForm.newPassword
  if (!p) return 0
  let score = 0
  if (p.length >= 6) score++
  if (p.length >= 10) score++
  if (/[A-Z]/.test(p) && /[a-z]/.test(p)) score++
  if (/[0-9]/.test(p)) score++
  if (/[^A-Za-z0-9]/.test(p)) score++
  return Math.min(4, score)
})

const strengthLabel = computed(() => {
  const s = passwordStrength.value
  if (s === 0) return ''
  if (s === 1) return 'Yếu'
  if (s === 2) return 'Trung bình'
  if (s === 3) return 'Mạnh'
  return 'Rất mạnh'
})

const getStrengthClass = (i) => {
  if (i === 0) {
    const s = passwordStrength.value
    if (s <= 1) return 'ss__strength-label--weak'
    if (s === 2) return 'ss__strength-label--medium'
    if (s === 3) return 'ss__strength-label--strong'
    return 'ss__strength-label--very-strong'
  }
  if (i <= passwordStrength.value) {
    const s = passwordStrength.value
    if (s <= 1) return 'ss__strength-bar--weak'
    if (s === 2) return 'ss__strength-bar--medium'
    if (s === 3) return 'ss__strength-bar--strong'
    return 'ss__strength-bar--very-strong'
  }
  return ''
}

const securityTips = [
  {
    icon: 'password',
    title: 'Sử dụng mật khẩu mạnh',
    desc: 'Kết hợp chữ hoa, chữ thường, số và ký tự đặc biệt. Tối thiểu 10 ký tự.'
  },
  {
    icon: 'update',
    title: 'Đổi mật khẩu định kỳ',
    desc: 'Nên thay đổi mật khẩu mỗi 3-6 tháng để tăng cường bảo mật.'
  },
  {
    icon: 'share',
    title: 'Không chia sẻ thông tin',
    desc: 'Tuyệt đối không chia sẻ mật khẩu với người khác, kể cả đồng nghiệp.'
  }
]

const validate = () => {
  errors.value = {}
  if (!passwordForm.currentPassword) {
    errors.value.currentPassword = 'Vui lòng nhập mật khẩu hiện tại.'
  }
  if (!passwordForm.newPassword) {
    errors.value.newPassword = 'Vui lòng nhập mật khẩu mới.'
  } else if (passwordForm.newPassword.length < 6) {
    errors.value.newPassword = 'Mật khẩu mới phải có ít nhất 6 ký tự.'
  }
  if (!passwordForm.confirmPassword) {
    errors.value.confirmPassword = 'Vui lòng xác nhận mật khẩu mới.'
  } else if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    errors.value.confirmPassword = 'Mật khẩu xác nhận không khớp.'
  }
  return Object.keys(errors.value).length === 0
}

const closePasswordForm = () => {
  isChangingPassword.value = false
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  errors.value = {}
}

const submitPasswordChange = async () => {
  if (!validate()) return
  isSavingPassword.value = true
  emit('change-password', {
    currentPassword: passwordForm.currentPassword,
    newPassword: passwordForm.newPassword
  })
  isSavingPassword.value = false
  closePasswordForm()
}

defineExpose({ isSavingPassword })
</script>


<style scoped>
.ss {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.dark .ss {
  border-color: var(--ds-border-strong);
}

/* Header */
.ss__header {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1.125rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .ss__header {
  border-bottom-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.ss__header-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.ss__title {
  font-family: var(--ds-font-display);
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .ss__title {
  color: #f1f5f9;
}

.ss__subtitle {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
  font-weight: 500;
}

/* Content */
.ss__content {
  display: flex;
  flex-direction: column;
}

/* Section */
.ss__section {
  border-bottom: 1px solid var(--ds-border);
  padding: 1.125rem 1.25rem;
}

.dark .ss__section {
  border-bottom-color: var(--ds-border-strong);
}

.ss__section:last-child {
  border-bottom: none;
}

.ss__section-header {
  display: flex;
  align-items: center;
  gap: 0.875rem;
}

.ss__section-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ss__section-icon--warning {
  background: rgba(234, 179, 8, 0.08);
  color: var(--ds-warning);
}


.ss__section-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0;
}

.dark .ss__section-title {
  color: #f1f5f9;
}

.ss__section-desc {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
  font-weight: 500;
}

.ss__toggle-btn {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.4rem 0.875rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.dark .ss__toggle-btn {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.ss__toggle-btn:hover {
  border-color: var(--ds-primary-border);
  color: var(--ds-primary);
  background: var(--ds-primary-soft);
}

.dark .ss__toggle-btn:hover {
  background: rgba(79, 70, 229, 0.1);
}

.ss__toggle-btn--open {
  border-color: var(--ds-danger-border, #fca5a5);
  color: var(--ds-danger);
  background: var(--ds-danger-soft);
}


/* Expand body */
.ss__expand-body {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--ds-border);
  animation: slideDown 0.2s ease;
}

.dark .ss__expand-body {
  border-top-color: var(--ds-border-strong);
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Form */
.ss__form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  max-width: 480px;
}

.ss__form-group {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.ss__label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text-secondary);
}

.ss__required {
  color: var(--ds-danger);
}

.ss__input-wrap {
  position: relative;
  display: flex;
}

.ss__input {
  flex: 1;
  padding: 0.625rem 2.5rem 0.625rem 0.875rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  color: var(--ds-text);
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  font-family: inherit;
  width: 100%;
  box-sizing: border-box;
}

.dark .ss__input {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: #f1f5f9;
}

.ss__input::placeholder {
  color: var(--ds-text-muted);
}

.ss__input:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.ss__input--error {
  border-color: var(--ds-danger);
}

.ss__eye-toggle {
  position: absolute;
  right: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--ds-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.25rem;
  transition: color 0.12s ease;
}

.ss__eye-toggle:hover {
  color: var(--ds-text);
}

.dark .ss__eye-toggle {
  color: #94a3b8;
}

.dark .ss__eye-toggle:hover {
  color: #f1f5f9;
}


/* Password strength */
.ss__strength {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 0.25rem;
}

.ss__strength-bars {
  display: flex;
  gap: 0.25rem;
  flex: 1;
}

.ss__strength-bar {
  flex: 1;
  height: 4px;
  border-radius: 2px;
  background: var(--ds-gray-200);
  transition: background 0.2s ease;
}

.dark .ss__strength-bar {
  background: var(--ds-gray-600);
}

.ss__strength-bar--weak { background: var(--ds-danger); }
.ss__strength-bar--medium { background: var(--ds-warning); }
.ss__strength-bar--strong { background: var(--ds-info); }
.ss__strength-bar--very-strong { background: var(--ds-success); }

.ss__strength-label {
  font-size: 0.7rem;
  font-weight: 700;
  min-width: 80px;
  text-align: right;
}

.ss__strength-label--weak { color: var(--ds-danger); }
.ss__strength-label--medium { color: var(--ds-warning); }
.ss__strength-label--strong { color: var(--ds-info); }
.ss__strength-label--very-strong { color: var(--ds-success); }

/* Match indicator */
.ss__match-indicator {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  font-weight: 600;
  margin-top: 0.25rem;
}

.ss__match-indicator {
  color: var(--ds-success);
}

.ss__error-msg {
  font-size: 0.7rem;
  color: var(--ds-danger);
  font-weight: 600;
}

/* Form actions */
.ss__form-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding-top: 0.5rem;
}

.ss__btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  border-radius: var(--ds-radius-xl);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1.5px solid;
  font-family: inherit;
}

.ss__btn--cancel {
  background: var(--ds-surface);
  border-color: var(--ds-border);
  color: var(--ds-text-secondary);
}

.dark .ss__btn--cancel {
  background: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
}

.ss__btn--cancel:hover:not(:disabled) {
  background: var(--ds-gray-100);
}

.dark .ss__btn--cancel:hover:not(:disabled) {
  background: var(--ds-gray-600);
}

.ss__btn--save {
  background: var(--ds-primary);
  border-color: var(--ds-primary);
  color: white;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
}

.ss__btn--save:hover:not(:disabled) {
  background: var(--ds-primary-hover, #4338ca);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.ss__btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.ss__spinner {
  font-size: 1rem;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Tips */
.ss__tips {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 1rem;
}

.ss__tip {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.75rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-gray-50);
}

.dark .ss__tip {
  background: var(--ds-gray-800);
}


.ss__tip strong {
  display: block;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ds-text);
  margin-bottom: 0.25rem;
}

.dark .ss__tip strong {
  color: #f1f5f9;
}

.ss__tip p {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0;
  font-weight: 500;
  line-height: 1.4;
}

/* Sessions */
.ss__sessions {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 1rem;
}

.ss__session-item {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 0.875rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-gray-50);
  border: 1px solid var(--ds-border);
}

.dark .ss__session-item {
  background: var(--ds-gray-800);
  border-color: var(--ds-border-strong);
}

.ss__session-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}


.ss__session-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.ss__session-device {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--ds-text);
}

.dark .ss__session-device {
  color: #f1f5f9;
}

.ss__session-meta {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}

.ss__session-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
  font-weight: 700;
  flex-shrink: 0;
}


.ss__session-badge--active {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}
</style>
