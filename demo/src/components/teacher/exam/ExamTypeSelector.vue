<template>
  <div class="ets">
    <div class="ets__workspace-link">
      <RouterLink to="/teacher/exams/list" class="ets__back-link">
        <LucideIcon name="arrow_back" />
        <span>Quay lại danh sách đề</span>
      </RouterLink>
      <div class="ets__workspace-badge">
        <LucideIcon name="edit_square" />
        <span>Workspace tạo đề</span>
      </div>
    </div>

    <!-- Hero -->
    <div class="ets__hero">
      <div class="ets__hero-badge">
        <LucideIcon name="add_circle" />
        <span>Tạo đề thi mới</span>
      </div>
      <h1 class="ets__hero-title">Chọn hình thức bài thi</h1>
      <p class="ets__hero-sub">
        Bắt đầu từ một workspace thống nhất: chọn loại bài thi trước, sau đó tiếp tục sang các bước nhập câu hỏi, cài đặt lịch và xuất bản.
      </p>
    </div>

    <!-- Choice cards -->
    <div class="ets__cards">
      <!-- Free exam -->
      <button
        type="button"
        class="ets__card"
        :class="{ 'ets__card--selected': selected === 'free' }"
        :aria-pressed="selected === 'free'"
        @click="selected = 'free'"
      >
        <div class="ets__card-glow ets__card-glow--blue" />
        <div class="ets__card-header">
          <div class="ets__card-icon ets__card-icon--blue">
            <LucideIcon name="public" />
          </div>
          <div v-if="selected === 'free'" class="ets__card-check ets__card-check--blue">
            <LucideIcon name="check" />
          </div>
        </div>
        <h2 class="ets__card-title">Bài thi tự do</h2>
        <p class="ets__card-desc">
          Thí sinh tham gia bằng mã đề thi. Phù hợp cho thi online tự do,
          ôn tập, kiểm tra nhanh — không giới hạn lớp học.
        </p>
        <ul class="ets__card-features">
          <li>
            <LucideIcon name="vpn_key" />
            <span>Mã đề thi tự động, chia sẻ dễ dàng</span>
          </li>
          <li>
            <LucideIcon name="group" />
            <span>Không giới hạn số thí sinh</span>
          </li>
          <li>
            <LucideIcon name="lock_open" />
            <span>Không cần gán lớp học</span>
          </li>
        </ul>
        <div class="ets__card-tag ets__card-tag--blue">
          <LucideIcon name="bolt" />
          Nhanh — thiết lập trong 2 phút
        </div>
      </button>

      <!-- Private exam -->
      <button
        type="button"
        class="ets__card"
        :class="{ 'ets__card--selected': selected === 'private' }"
        :aria-pressed="selected === 'private'"
        @click="selected = 'private'"
      >
        <div class="ets__card-glow ets__card-glow--purple" />
        <div class="ets__card-header">
          <div class="ets__card-icon ets__card-icon--purple">
            <LucideIcon name="school" />
          </div>
          <div v-if="selected === 'private'" class="ets__card-check ets__card-check--purple">
            <LucideIcon name="check" />
          </div>
        </div>
        <h2 class="ets__card-title">Bài thi riêng tư</h2>
        <p class="ets__card-desc">
          Chỉ học sinh trong lớp bạn phụ trách mới thấy và tham gia thi.
          Phù hợp cho kiểm tra lớp học, thi giữa kỳ, cuối kỳ.
        </p>
        <ul class="ets__card-features">
          <li>
            <LucideIcon name="class" />
            <span>Giới hạn truy cập theo lớp học</span>
          </li>
          <li>
            <LucideIcon name="shield" />
            <span>An toàn — chỉ học sinh được phép mới thi được</span>
          </li>
          <li>
            <LucideIcon name="manage_accounts" />
            <span>Theo dõi kết quả theo từng lớp</span>
          </li>
        </ul>
        <div class="ets__card-tag ets__card-tag--purple">
          <LucideIcon name="school" />
          Dành cho lớp bạn đảm nhiệm
        </div>
      </button>
    </div>

    <!-- Next button -->
    <div class="ets__footer">
      <button
        type="button"
        class="ets__next-btn"
        :class="selected === 'free' ? 'ets__next-btn--blue' : 'ets__next-btn--purple'"
        :disabled="!selected"
        @click="proceed"
      >
        <span>Tiếp tục tạo đề thi</span>
        <LucideIcon name="arrow_forward" />
      </button>
      <p class="ets__footer-hint">
        Bạn có thể thay đổi hình thức bất cứ lúc nào trước khi xuất bản
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

const router = useRouter()
const selected = ref('')

const proceed = () => {
  if (!selected.value) return
  router.push({
    path: '/teacher/exams/build',
    query: { type: selected.value }
  })
}
</script>

<style scoped>
.ets {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--ds-bg);
  padding: 2rem 1.5rem 2rem;
}

.ets__workspace-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.ets__back-link {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--ds-text-secondary);
  text-decoration: none;
  font-weight: 700;
}

.ets__back-link:hover {
  color: var(--ds-primary);
}

.ets__workspace-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--ds-radius-full);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border: 1px solid var(--ds-primary-border);
  font-size: 0.8rem;
  font-weight: 800;
}

/* Hero */
.ets__hero {
  text-align: center;
  margin-bottom: 2rem;
  animation: fadeUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.ets__hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 1rem;
  border-radius: var(--ds-radius-full);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  font-size: 0.75rem;
  font-weight: 700;
  margin-bottom: 1rem;
  border: 1px solid var(--ds-primary-border);
}

.ets__hero-title {
  font-family: var(--ds-font-display);
  font-size: clamp(1.5rem, 3vw, 2.25rem);
  font-weight: 900;
  color: var(--ds-text);
  margin: 0 0 0.75rem;
  letter-spacing: -0.025em;
  line-height: 1.15;
}

.dark .ets__hero-title { color: var(--ds-text); }

.ets__hero-sub {
  font-size: 1rem;
  color: var(--ds-text-secondary);
  margin: 0;
  line-height: 1.7;
  max-width: 720px;
  margin-inline: auto;
}

.dark .ets__hero-sub { color: var(--ds-text-muted); }

/* Cards */
.ets__cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
  flex: 1;
  animation: fadeUp 0.45s cubic-bezier(0.16, 1, 0.3, 1) 0.05s both;
}

@media (max-width: 640px) {
  .ets__cards { grid-template-columns: 1fr; }
}

.ets__card {
  position: relative;
  padding: 1.75rem;
  background: var(--ds-surface);
  border: 2px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  cursor: pointer;
  transition: color 0.25s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1), transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
  text-align: left;
}

.ets__card:hover {
  border-color: rgba(99, 102, 241, 0.35);
  box-shadow: 0 12px 36px rgba(79, 70, 229, 0.12);
  transform: translateY(-2px);
}

.ets__card:focus-visible {
  outline: 3px solid var(--ds-primary-ring);
  outline-offset: 2px;
}

.ets__card--selected {
  border-color: var(--ds-primary);
  box-shadow: 0 12px 40px rgba(79, 70, 229, 0.18);
  transform: translateY(-3px);
}

.ets__card--selected .ets__card-glow--blue { opacity: 1; }
.ets__card--selected .ets__card-glow--purple { opacity: 1; }

.ets__card-glow {
  position: absolute;
  top: -60px;
  right: -60px;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.ets__card-glow--blue {
  background: radial-gradient(circle, rgba(2, 132, 199, 0.18) 0%, transparent 70%);
}

.ets__card-glow--purple {
  background: radial-gradient(circle, rgba(79, 70, 229, 0.18) 0%, transparent 70%);
}

.dark .ets__card {
  background: rgba(30, 41, 59, 0.9);
  border-color: var(--ds-border-strong);
}

.dark .ets__card:hover { border-color: rgba(99, 102, 241, 0.5); }
.dark .ets__card--selected { border-color: var(--ds-primary); }

.ets__card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.ets__card-icon {
  width: 52px;
  height: 52px;
  border-radius: var(--ds-radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.ets__card-icon--blue {
  background: var(--ds-info-bg);
  color: #0284c7;
}

.ets__card-icon--purple {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}

.dark .ets__card-icon--purple { background: rgba(99, 102, 241, 0.25); }

.ets__card--selected .ets__card-icon--blue {
  box-shadow: 0 6px 16px rgba(2, 132, 199, 0.3);
  transform: scale(1.05);
}

.ets__card--selected .ets__card-icon--purple {
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.3);
  transform: scale(1.05);
}

.ets__card-check {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: checkPop 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.ets__card-check--blue { background: #0284c7; color: white; }
.ets__card-check--purple { background: var(--ds-primary); color: white; }

@keyframes checkPop {
  from { transform: scale(0); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.ets__card-title {
  font-family: var(--ds-font-display);
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
  letter-spacing: -0.015em;
}

.dark .ets__card-title { color: var(--ds-text); }

.ets__card-desc {
  font-size: 0.85rem;
  color: var(--ds-text-secondary);
  margin: 0;
  line-height: 1.6;
}

.dark .ets__card-desc { color: var(--ds-text-muted); }

.ets__card-features {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex: 1;
}

.ets__card-features li {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.82rem;
  color: var(--ds-text-secondary);
}

.ets__card-features li :deep(svg),
.ets__card-features li svg {
  color: var(--ds-success);
  flex-shrink: 0;
  font-size: 1rem;
}

.ets__card-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.4rem 0.875rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.72rem;
  font-weight: 700;
  align-self: flex-start;
}

.ets__card-tag--blue {
  background: var(--ds-info-bg);
  color: #0284c7;
  border: 1px solid rgba(2, 132, 199, 0.2);
}

.ets__card-tag--purple {
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  border: 1px solid var(--ds-primary-border);
}

.dark .ets__card-tag--purple {
  background: rgba(99, 102, 241, 0.2);
  color: #a5b4fc;
}

/* Footer */
.ets__footer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--ds-border);
  animation: fadeUp 0.45s cubic-bezier(0.16, 1, 0.3, 1) 0.1s both;
}

.dark .ets__footer { border-top-color: var(--ds-border-strong); }

.ets__next-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.875rem 2.5rem;
  border-radius: var(--ds-radius-2xl);
  font-size: 1rem;
  font-weight: 800;
  cursor: pointer;
  border: none;
  color: white;
  transition: color 0.22s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.22s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.22s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.22s cubic-bezier(0.4, 0, 0.2, 1), transform 0.22s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  letter-spacing: -0.01em;
}

.ets__next-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}

.ets__next-btn--blue {
  background: linear-gradient(135deg, #0284c7 0%, #0ea5e9 100%);
}

.ets__next-btn--blue:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(2, 132, 199, 0.35);
}

.ets__next-btn--purple {
  background: linear-gradient(135deg, var(--ds-primary) 0%, #6366f1 100%);
}

.ets__next-btn--purple:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(79, 70, 229, 0.35);
}

.ets__footer-hint {
  font-size: 0.75rem;
  color: var(--ds-text-muted);
  margin: 0;
  text-align: center;
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (prefers-reduced-motion: reduce) {
  .ets__card,
  .ets__next-btn {
    transition: border-color 0.15s linear, box-shadow 0.15s linear;
  }
  .ets__card:hover,
  .ets__card--selected,
  .ets__next-btn:hover { transform: none; }
  .ets__card-check { animation: none; }
  .ets__hero,
  .ets__cards,
  .ets__footer { animation: none; opacity: 1; }
}
</style>
