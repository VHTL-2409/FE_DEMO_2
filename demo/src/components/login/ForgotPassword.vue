<template>
  <div class="auth-page">
    <BaseCard class="auth-card" padding="lg">
      <div class="auth-header">
        <div class="auth-icon">
          <LucideIcon name="lock_reset" size="28" />
        </div>
        <h1>Quên mật khẩu</h1>
        <p>Nhập email đã đăng ký để nhận liên kết đặt lại mật khẩu.</p>
      </div>

      <form v-if="!emailSent" class="auth-form" @submit.prevent="onSubmit">
        <BaseField label="Email" required hint="Dùng email đã liên kết với tài khoản EduExam.">
          <template #default="{ inputId, hintId }">
            <BaseInput
              :id="inputId"
              v-model="email"
              type="email"
              autocomplete="email"
              placeholder="email-cua-ban@gmail.com"
              :described-by="hintId"
              size="lg"
            />
          </template>
        </BaseField>

        <BaseButton type="submit" class="w-full" size="lg" icon="send" :loading="isSubmitting">
          {{ isSubmitting ? 'Đang gửi...' : 'Gửi liên kết đặt lại mật khẩu' }}
        </BaseButton>
      </form>

      <div v-else class="auth-form">
        <div v-if="resetUrl" class="auth-alert auth-alert--warning">
          <strong>Chế độ demo</strong>
          <p>Sao chép liên kết dưới đây để đặt lại mật khẩu:</p>
          <a :href="fullResetUrl" target="_blank" rel="noreferrer">
            {{ fullResetUrl }}
          </a>
        </div>
        <div v-else class="auth-alert auth-alert--success">
          <LucideIcon name="mark_email_read" />
          <p>Đã gửi email. Vui lòng kiểm tra hộp thư, bao gồm cả thư mục spam.</p>
        </div>
        <BaseButton variant="secondary" type="button" class="w-full" icon="refresh_cw" @click="onSendAnother">
          Gửi lại email khác
        </BaseButton>
      </div>

      <RouterLink to="/login" class="auth-back">
        <LucideIcon name="arrow_back" size="16" />
        Quay lại đăng nhập
      </RouterLink>
    </BaseCard>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { forgotPassword } from '../../services/authService'
import { useToast } from '../../composables/useToast'
import BaseButton from '../shared/BaseButton.vue'
import BaseCard from '../shared/BaseCard.vue'
import BaseField from '../shared/BaseField.vue'
import BaseInput from '../shared/BaseInput.vue'

const toast = useToast()
const email = ref('')
const isSubmitting = ref(false)
const emailSent = ref(false)
const resetUrl = ref('')
const fullResetUrl = computed(() => {
  if (!resetUrl.value) return ''
  const path = resetUrl.value.startsWith('/') ? resetUrl.value : '/' + resetUrl.value
  return typeof window !== 'undefined' ? window.location.origin + path : path
})

const onSendAnother = () => {
  emailSent.value = false
  resetUrl.value = ''
}

const onSubmit = async () => {
  isSubmitting.value = true
  resetUrl.value = ''
  emailSent.value = false
  try {
    const data = await forgotPassword({ email: email.value })
    emailSent.value = true
    if (data?.resetUrl) {
      resetUrl.value = data.resetUrl
      toast.info('Chế độ demo: sử dụng liên kết bên dưới.')
    } else {
      toast.success('Đã gửi email. Vui lòng kiểm tra hộp thư.')
    }
  } catch (error) {
    toast.error(error?.message || 'Không thể gửi yêu cầu.')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100%;
  display: grid;
  place-items: center;
  padding: 1rem;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.76), rgba(248, 250, 252, 0.92)),
    var(--ds-bg);
}

.auth-card {
  width: min(100%, 440px);
}

.auth-header {
  text-align: center;
  margin-bottom: 1.75rem;
}

.auth-icon {
  width: 3.5rem;
  height: 3.5rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.auth-header h1 {
  margin: 0;
  color: var(--ds-text);
  font-size: 1.5rem;
  font-weight: 800;
}

.auth-header p {
  margin: 0.5rem 0 0;
  color: var(--ds-text-secondary);
  font-size: 0.9rem;
  line-height: 1.55;
}

.auth-form {
  display: grid;
  gap: 1rem;
}

.auth-alert {
  display: grid;
  gap: 0.45rem;
  border: 1px solid;
  border-radius: var(--ds-radius-lg);
  padding: 1rem;
  font-size: 0.875rem;
  line-height: 1.55;
}

.auth-alert p {
  margin: 0;
}

.auth-alert a {
  color: var(--ds-primary);
  font-weight: 700;
  overflow-wrap: anywhere;
}

.auth-alert--warning {
  background: var(--ds-warning-bg);
  border-color: var(--ds-warning-soft);
  color: var(--ds-warning);
}

.auth-alert--success {
  grid-template-columns: auto 1fr;
  align-items: start;
  background: var(--ds-success-bg);
  border-color: var(--ds-success-soft);
  color: var(--ds-success);
}

.auth-back {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  width: 100%;
  margin-top: 1.25rem;
  color: var(--ds-primary);
  font-size: 0.875rem;
  font-weight: 700;
  text-decoration: none;
}

.auth-back:hover {
  text-decoration: underline;
}
</style>
