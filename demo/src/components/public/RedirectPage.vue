<template>
  <div class="rdl">
    <!-- Animated background -->
    <div class="rdl__bg" aria-hidden="true">
      <div class="rdl__orb rdl__orb--1"></div>
      <div class="rdl__orb rdl__orb--2"></div>
      <div class="rdl__orb rdl__orb--3"></div>
    </div>

    <!-- Card -->
    <div class="rdl__card">
      <!-- Logo -->
      <div class="rdl__logo">
        <AppLogo size="lg" variant="brand" tag="div" />
      </div>

      <!-- Role badge -->
      <div class="rdl__role-badge" :class="`rdl__role-badge--${role}`">
        <svg v-if="role === 'teacher'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/>
        </svg>
        <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/>
        </svg>
        {{ role === 'teacher' ? 'Giáo viên' : 'Học sinh' }}
      </div>

      <!-- Spinner -->
      <div class="rdl__spinner" aria-label="Đang tải">
        <div class="rdl__spinner-ring"></div>
        <div class="rdl__spinner-ring rdl__spinner-ring--2"></div>
        <div class="rdl__spinner-ring rdl__spinner-ring--3"></div>
        <div class="rdl__spinner-core">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
          </svg>
        </div>
      </div>

      <!-- Status title -->
      <h1 class="rdl__title">
        <span v-if="currentStep < 2">Đang chuẩn bị workspace...</span>
        <span v-else>Workspace đã sẵn sàng!</span>
      </h1>

      <!-- Steps -->
      <div class="rdl__steps">
        <div
          v-for="(step, i) in steps"
          :key="step.id"
          class="rdl__step"
          :class="{
            'rdl__step--done': i < currentStep,
            'rdl__step--active': i === currentStep,
          }"
        >
          <div class="rdl__step-icon">
            <svg v-if="i < currentStep" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            <span v-else-if="i === currentStep" class="rdl__step-dot"></span>
          </div>
          <span class="rdl__step-label">{{ step.label }}</span>
        </div>
      </div>

      <!-- Progress bar -->
      <div class="rdl__progress">
        <div class="rdl__progress-fill" :style="{ width: progressWidth }"></div>
      </div>

      <!-- Error state -->
      <div v-if="hasError" class="rdl__error">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        <span>{{ errorMessage }}</span>
      </div>

      <!-- Fallback button -->
      <button v-if="hasError" class="rdl__fallback-btn" @click="goToDashboard">
        Vào Dashboard ngay
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <path d="M5 12h14M12 5l7 7-7 7"/>
        </svg>
      </button>
    </div>

    <!-- Footer -->
    <p class="rdl__footer">EduExam &mdash; Nền tảng thi trực tuyến hàng đầu Việt Nam</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLogo from '../common/AppLogo.vue'
import { listMyAttempts } from '../../services/attemptService'
import { listExams } from '../../services/examService'

const route = useRoute()
const router = useRouter()

const role = computed(() => route.query.role === 'teacher' ? 'teacher' : 'student')
const dashboardPath = computed(() => role.value === 'teacher' ? '/teacher/dashboard' : '/student/dashboard')

const steps = [
  { id: 'auth',   label: 'Xác thực người dùng' },
  { id: 'fetch',  label: 'Tải dữ liệu dashboard' },
  { id: 'ready',  label: 'Sẵn sàng vào workspace' },
]

const currentStep = ref(0)
const hasError = ref(false)
const errorMessage = ref('')
let timeoutId = null

const progressWidth = computed(() => {
  if (hasError.value) return '100%'
  return `${Math.min(((currentStep.value + 1) / steps.length) * 100, 100)}%`
})

const advance = (stepIndex) => {
  currentStep.value = stepIndex
}

const goToDashboard = () => {
  router.replace(dashboardPath.value)
}

const run = async () => {
  // Safety timeout: 15 seconds
  timeoutId = setTimeout(() => {
    if (!hasError.value) {
      hasError.value = true
      errorMessage.value = 'Quá thời gian chờ. Vui lòng thử lại.'
    }
  }, 15000)

  try {
    // Step 1: Auth (instant - token already exists from login)
    advance(1)

    // Small visual pause so user can see step 1 complete
    await new Promise(r => setTimeout(r, 300))

    // Step 2: Fetch dashboard data
    advance(2)

    let data
    if (role.value === 'student') {
      data = await listMyAttempts()
      sessionStorage.setItem('prefetch_student_data', JSON.stringify(data))
    } else {
      data = await listExams()
      sessionStorage.setItem('prefetch_teacher_data', JSON.stringify(data))
    }

    // Step 3: Ready
    advance(3)

    // Brief pause to show completion
    await new Promise(r => setTimeout(r, 400))

    clearTimeout(timeoutId)
    router.replace(dashboardPath.value)

  } catch (err) {
    clearTimeout(timeoutId)
    hasError.value = true
    errorMessage.value = err?.message || err?.payload?.message || 'Không thể tải dữ liệu. Vui lòng thử lại.'
  }
}

onMounted(() => {
  run()
})

onUnmounted(() => {
  if (timeoutId) clearTimeout(timeoutId)
})
</script>

<style scoped>
.rdl {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #3730a3 0%, #4338ca 38%, #4f46e5 68%, #6366f1 100%);
  position: relative;
  overflow: hidden;
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, sans-serif;
}

/* Animated background orbs */
.rdl__bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.rdl__orb {
  position: absolute;
  border-radius: 50%;
  opacity: 0.22;
  animation: orb-drift 20s ease-in-out infinite;
}

.rdl__orb--1 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(255,255,255,0.12) 0%, transparent 70%);
  top: -200px;
  left: -100px;
  animation-delay: 0s;
}

.rdl__orb--2 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(139,92,246,0.15) 0%, transparent 70%);
  bottom: -150px;
  right: -50px;
  animation-delay: -7s;
}

.rdl__orb--3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(255,255,255,0.08) 0%, transparent 70%);
  top: 40%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: -14s;
}

@keyframes orb-drift {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -20px) scale(1.05); }
  66% { transform: translate(-20px, 30px) scale(0.97); }
}

/* Card */
.rdl__card {
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 28px;
  padding: 3rem 3.5rem;
  width: 100%;
  max-width: 460px;
  text-align: center;
  box-shadow:
    0 25px 50px rgba(0, 0, 0, 0.25),
    0 0 0 1px rgba(255, 255, 255, 0.05);
  animation: card-enter 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes card-enter {
  from { opacity: 0; }
  to   { opacity: 1; }
}

/* Logo */
.rdl__logo {
  display: flex;
  justify-content: center;
  margin-bottom: 1.25rem;
}

/* Role badge */
.rdl__role-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  font-weight: 600;
  padding: 0.375rem 0.875rem;
  border-radius: 50px;
  margin-bottom: 2rem;
}

.rdl__role-badge--teacher {
  background: rgba(79, 70, 229, 0.3);
  border: 1px solid rgba(165, 180, 252, 0.4);
  color: #c7d2fe;
}

.rdl__role-badge--student {
  background: rgba(20, 184, 166, 0.25);
  border: 1px solid rgba(94, 234, 212, 0.4);
  color: #5eead4;
}

/* Spinner */
.rdl__spinner {
  position: relative;
  width: 90px;
  height: 90px;
  margin: 0 auto 2rem;
}

.rdl__spinner-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 3px solid rgba(255, 255, 255, 0.15);
  border-top-color: rgba(255, 255, 255, 0.9);
  animation: spin 1.4s linear infinite;
}

.rdl__spinner-ring--2 {
  inset: 12px;
  border-width: 2.5px;
  border-top-color: rgba(255, 255, 255, 0.7);
  animation-duration: 2.1s;
  animation-direction: reverse;
}

.rdl__spinner-ring--3 {
  inset: 26px;
  border-width: 2px;
  border-top-color: rgba(255, 255, 255, 0.5);
  animation-duration: 1.7s;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.rdl__spinner-core {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.95);
  animation: core-pulse 2s ease-in-out infinite;
}

@keyframes core-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(0.92); }
}

/* Title */
.rdl__title {
  font-family: 'Be Vietnam Pro', sans-serif;
  font-size: 1.375rem;
  font-weight: 700;
  color: white;
  letter-spacing: -0.01em;
  margin: 0 0 2rem;
}

/* Steps */
.rdl__steps {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.75rem;
  text-align: left;
}

.rdl__step {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  transition: color 0.3s ease, background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease, transform 0.3s ease;
}

.rdl__step-icon {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 1.5px solid rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: color 0.3s ease, background-color 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease, transform 0.3s ease;
  background: rgba(255, 255, 255, 0.05);
}

.rdl__step--done .rdl__step-icon {
  background: rgba(52, 211, 153, 0.9);
  border-color: #34d399;
  color: white;
  transform: scale(1.05);
}

.rdl__step--active .rdl__step-icon {
  border-color: rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.1);
}

.rdl__step-dot {
  width: 8px;
  height: 8px;
  background: white;
  border-radius: 50%;
  animation: dot-blink 1.2s ease-in-out infinite;
}

@keyframes dot-blink {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.7); }
}

.rdl__step-label {
  font-size: 0.9375rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.5);
  transition: color 0.3s ease;
}

.rdl__step--done .rdl__step-label {
  color: rgba(255, 255, 255, 0.75);
}

.rdl__step--active .rdl__step-label {
  color: white;
  font-weight: 600;
}

/* Progress bar */
.rdl__progress {
  height: 4px;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 1rem;
}

.rdl__progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #a5b4fc, #818cf8, #c084fc);
  border-radius: 2px;
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

/* Error state */
.rdl__error {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.75rem 1rem;
  background: rgba(239, 68, 68, 0.2);
  border: 1px solid rgba(239, 68, 68, 0.4);
  border-radius: 12px;
  color: #fca5a5;
  font-size: 0.875rem;
  margin-bottom: 1rem;
  animation: shake 0.4s ease;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-6px); }
  40% { transform: translateX(6px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(4px); }
}

.rdl__fallback-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem 1.75rem;
  background: white;
  color: #4f46e5;
  border: none;
  border-radius: 10px;
  font-size: 0.9375rem;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  width: 100%;
}

.rdl__fallback-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25);
}

/* Footer */
.rdl__footer {
  position: relative;
  z-index: 1;
  margin-top: 2rem;
  font-size: 0.8125rem;
  color: rgba(255, 255, 255, 0.4);
}

/* Responsive */
@media (max-width: 480px) {
  .rdl__card {
    padding: 2rem 1.5rem;
    margin: 1rem;
    border-radius: 20px;
  }

  .rdl__title {
    font-size: 1.125rem;
  }
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .rdl__orb,
  .rdl__spinner-ring,
  .rdl__spinner-core,
  .rdl__step-dot,
  .rdl__card {
    animation: none;
  }
}
</style>
