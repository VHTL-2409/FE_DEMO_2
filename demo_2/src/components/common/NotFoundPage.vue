<template>
  <div class="not-found">
    <div class="not-found__inner">
      <div class="not-found__icon">
        <LucideIcon name="help" :size="64" />
      </div>
      <h1 class="not-found__code">404</h1>
      <h2 class="not-found__title">Trang không tồn tại</h2>
      <p class="not-found__desc">
        Trang bạn đang tìm kiếm không tồn tại hoặc đã bị di chuyển.
      </p>
      <div class="not-found__actions">
        <button type="button" class="not-found__btn not-found__btn--primary" @click="goHome">
          <LucideIcon name="home" :size="18" />
          Về trang chủ
        </button>
        <button type="button" class="not-found__btn not-found__btn--secondary" @click="goBack">
          <LucideIcon name="arrow_back" :size="18" />
          Quay lại
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import LucideIcon from '../common/LucideIcon.vue'

const router = useRouter()

const goHome = () => {
  const path = router.currentRoute.value.path
  if (path.startsWith('/admin')) {
    router.push('/admin/dashboard')
  } else if (path.startsWith('/teacher')) {
    router.push('/teacher/dashboard')
  } else if (path.startsWith('/student')) {
    router.push('/student/dashboard')
  } else {
    router.push('/')
  }
}

const goBack = () => {
  if (window.history.length > 2) {
    router.back()
  } else {
    goHome()
  }
}
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ds-bg, #f8fafc);
  padding: 2rem;
}

.not-found__inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 1rem;
  max-width: 420px;
  animation: fadeInUp 0.4s ease;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.not-found__icon {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: var(--ds-primary-soft, rgba(79, 70, 229, 0.1));
  color: var(--ds-primary, #4f46e5);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.5rem;
}

.not-found__code {
  font-family: var(--ds-font-display, system-ui);
  font-size: 5rem;
  font-weight: 900;
  line-height: 1;
  margin: 0;
  background: linear-gradient(135deg, var(--ds-primary, #4f46e5), #7c3aed);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.not-found__title {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--ds-text, #0f172a);
  margin: 0;
}

.not-found__desc {
  font-size: 0.9rem;
  color: var(--ds-text-muted, #64748b);
  margin: 0;
  line-height: 1.6;
}

.not-found__actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  justify-content: center;
  margin-top: 0.5rem;
}

.not-found__btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  border-radius: var(--ds-radius-lg, 0.5rem);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  border: none;
  transition: all 0.2s ease;
  font-family: inherit;
}

.not-found__btn--primary {
  background: var(--ds-primary, #4f46e5);
  color: white;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.3);
}

.not-found__btn--primary:hover {
  background: var(--ds-primary-hover, #4338ca);
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.35);
}

.not-found__btn--secondary {
  background: var(--ds-surface, white);
  color: var(--ds-text, #0f172a);
  border: 1.5px solid var(--ds-border, #e2e8f0);
}

.not-found__btn--secondary:hover {
  border-color: var(--ds-primary-border, rgba(79, 70, 229, 0.3));
  color: var(--ds-primary, #4f46e5);
  transform: translateY(-1px);
}

@media (prefers-reduced-motion: reduce) {
  .not-found__inner { animation: none; }
  .not-found__btn { transition: none; }
}
</style>
