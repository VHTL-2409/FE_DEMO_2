<template>
  <div class="portal-viewport landing-page font-display text-slate-900">
    <div class="flex min-h-0 flex-1 flex-col overflow-x-hidden">
      <header
        class="landing-nav-amber sticky top-0 z-50 border-b border-amber-200/25 bg-[#faf9f5]/85 px-4 py-3 backdrop-blur-xl sm:px-8 dark:border-amber-900/30 dark:bg-slate-950/85"
      >
        <div class="mx-auto flex max-w-7xl items-center justify-between gap-4">
          <RouterLink to="/" class="flex min-w-0 items-center gap-3 text-amber-900 transition-opacity hover:opacity-90 dark:text-amber-100">
            <div
              class="flex size-11 items-center justify-center rounded-[1.1rem] bg-[linear-gradient(135deg,#8d4b00_0%,#b15f00_100%)] text-sm font-black text-white shadow-[0_18px_30px_-18px_rgba(141,75,0,0.4)]"
            >
              E
            </div>
            <div class="min-w-0">
              <div class="truncate text-[1.08rem] font-black leading-tight tracking-[-0.02em] sm:text-[1.12rem]">EduExam Platform</div>
              <div class="hidden text-[0.7rem] font-semibold uppercase tracking-[0.18em] text-slate-500 md:block">
                Thi trực tuyến, giám sát tập trung, vận hành rõ ràng
              </div>
            </div>
          </RouterLink>

          <nav
            class="hidden items-center gap-1 rounded-full border border-amber-200/40 bg-white/80 px-1.5 py-1 text-[0.88rem] font-semibold text-slate-600 shadow-sm dark:border-slate-600 dark:bg-slate-800/80 dark:text-slate-300 md:flex"
          >
            <a
              v-for="link in navLinks"
              :key="link.href"
              :href="link.href"
              class="rounded-full px-3 py-1.5 whitespace-nowrap transition-colors hover:bg-amber-50 hover:text-amber-900 dark:hover:bg-slate-700 dark:hover:text-amber-100"
            >
              {{ link.label }}
            </a>
            <RouterLink
              class="rounded-full px-3 py-1.5 transition-colors hover:bg-amber-50 hover:text-amber-900 dark:hover:bg-slate-700 dark:hover:text-amber-100"
              to="/help-center"
            >
              Trợ giúp
            </RouterLink>
          </nav>

          <div class="flex items-center gap-2 sm:gap-3">
            <RouterLink to="/login" class="landing-btn landing-btn-secondary landing-btn-header-secondary">
              Đăng nhập
            </RouterLink>
            <RouterLink :to="primaryCta.to" class="landing-btn landing-btn-primary">{{ primaryCta.label }}</RouterLink>
          </div>
        </div>
      </header>

      <main class="portal-main-scroll portal-scrollbar min-h-0 flex-1">
        <section id="overview" class="px-4 pb-12 pt-10 sm:px-8 lg:px-12 lg:pb-16 lg:pt-14">
          <div class="mx-auto grid max-w-7xl gap-10 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)] lg:items-center lg:gap-12">
            <div>
              <div class="flex flex-wrap gap-2">
                <span v-for="badge in heroBadges" :key="badge" class="landing-chip">{{ badge }}</span>
              </div>

              <h1 class="landing-headline-serif mt-6 max-w-4xl text-[#1b1c1a] dark:text-slate-100">
                Thi trực tuyến
                <span class="landing-accent-text">gọn, rõ</span>
                và dễ kiểm soát.
              </h1>

              <p class="portal-subtitle mt-5 max-w-xl text-[#554336] dark:text-slate-300">
                Một nơi để tạo đề, giám sát kỳ thi, import câu hỏi và xem kết quả mà không phải chuyển qua nhiều màn hình rời rạc.
              </p>

              <div class="mt-8 flex flex-col gap-3 sm:flex-row sm:items-center">
                <RouterLink to="/help-center" class="landing-btn landing-btn-secondary max-w-fit">
                  Xem hướng dẫn
                </RouterLink>
                <p v-if="!authStore.isAuthenticated" class="text-sm text-slate-600">
                  <span class="text-slate-500">Chưa có tài khoản?</span>
                  <RouterLink to="/register" class="ml-1.5 font-bold text-amber-800 underline-offset-4 hover:underline dark:text-amber-300">
                    Đăng ký
                  </RouterLink>
                  <span class="mx-1.5 text-slate-300">·</span>
                  <RouterLink to="/login" class="font-semibold text-slate-700 underline-offset-4 hover:underline">
                    Đăng nhập
                  </RouterLink>
                </p>
                <RouterLink
                  v-else
                  :to="primaryCta.to"
                  class="text-sm font-semibold text-amber-800 underline-offset-4 hover:underline dark:text-amber-300"
                >
                  → {{ primaryCta.label }}
                </RouterLink>
              </div>

              <div class="mt-8 grid gap-3 sm:grid-cols-3">
                <div v-for="stat in heroStats" :key="stat.label" class="landing-stat-card landing-surface-card rounded-2xl px-4 py-3.5">
                  <div class="text-[0.72rem] font-bold uppercase tracking-[0.12em] text-[#8d4b00] dark:text-amber-300/90">
                    {{ stat.value }}
                  </div>
                  <div class="mt-1 text-[0.9rem] font-semibold leading-snug text-[#554336] dark:text-slate-300">
                    {{ stat.label }}
                  </div>
                </div>
              </div>
            </div>

            <div class="relative mx-auto w-full max-w-lg lg:max-w-none">
              <div
                class="aspect-[4/5] overflow-hidden rounded-[2rem] shadow-2xl shadow-amber-900/10 transition-transform duration-700 ease-out hover:rotate-0 lg:rotate-2"
              >
                <img
                  src="/images/hero-academic-amber.svg"
                  width="800"
                  height="1000"
                  class="h-full w-full object-cover"
                  alt="Minh họa không gian học thuật tông nâu cam"
                  loading="eager"
                  decoding="async"
                />
              </div>
              <div
                class="absolute -bottom-6 left-0 max-w-xs rounded-xl border border-[#dbc2b0]/30 bg-white/95 p-6 shadow-lg backdrop-blur-sm dark:border-slate-600 dark:bg-slate-900/95 sm:-left-6"
              >
                <span class="material-symbols-outlined mb-3 text-4xl text-[#8d4b00]" style="font-variation-settings: 'FILL' 1">verified</span>
                <h4 class="font-semibold text-lg text-[#1b1c1a] dark:text-slate-100">Chất lượng thẩm định</h4>
                <p class="mt-2 text-sm leading-relaxed text-[#554336] dark:text-slate-400">
                  Luồng thi và lưu trữ dữ liệu rõ ràng cho đơn vị đào tạo.
                </p>
              </div>
            </div>
          </div>

          <div class="mx-auto mt-12 max-w-7xl md:mt-14">
            <div class="mb-4 text-center md:text-left">
              <div class="portal-kicker">Tính năng</div>
              <h2 class="mt-2 text-xl font-black tracking-[-0.03em] text-slate-950 sm:text-2xl">
                Một luồng cho giảng viên, học sinh và quản trị
              </h2>
            </div>
            <div class="grid gap-4 sm:grid-cols-3">
              <article
                v-for="feat in featureHighlights"
                :key="feat.title"
                class="landing-surface-card flex flex-col rounded-2xl p-5 text-left"
              >
                <span
                  class="material-symbols-outlined mb-3 text-3xl text-[#8d4b00] dark:text-amber-300"
                  aria-hidden="true"
                  >{{ feat.icon }}</span
                >
                <h3 class="text-base font-black text-slate-950 dark:text-slate-100">{{ feat.title }}</h3>
                <p class="mt-2 text-sm leading-relaxed text-slate-600 dark:text-slate-400">
                  {{ feat.text }}
                </p>
              </article>
            </div>
          </div>
        </section>

        <section id="experience" class="px-4 py-10 sm:px-8 lg:px-12 lg:py-14">
          <div class="mx-auto max-w-7xl">
            <div class="max-w-3xl">
              <div class="portal-kicker">3 góc nhìn chính</div>
              <h2 class="mt-3 text-[clamp(1.9rem,3.2vw,2.85rem)] font-black leading-[1.04] tracking-[-0.04em] text-slate-950">
                Ít mô tả hơn, nhìn vào là hiểu luồng sản phẩm.
              </h2>
            </div>

            <div class="mt-8 grid gap-4 xl:grid-cols-3">
              <article v-for="card in visualCards" :key="card.title" class="landing-surface-card rounded-2xl p-5">
                <div class="flex items-center justify-between gap-2">
                  <div class="text-[1.02rem] font-black tracking-[-0.03em] text-slate-950">{{ card.title }}</div>
                  <div class="shrink-0 text-[0.72rem] font-bold uppercase tracking-[0.14em]" :class="card.tagTone">
                    {{ card.tag }}
                  </div>
                </div>
                <p class="mt-2 text-[0.86rem] leading-6 text-slate-600 dark:text-slate-400">{{ card.copy }}</p>
                <ul class="mt-4 space-y-2 border-t border-slate-200/80 pt-4 dark:border-slate-600/80">
                  <li
                    v-for="(line, idx) in card.bullets"
                    :key="idx"
                    class="flex gap-2 text-[0.84rem] leading-relaxed text-slate-600 dark:text-slate-400"
                  >
                    <span class="mt-1.5 size-1.5 shrink-0 rounded-full bg-[#b15f00]/70 dark:bg-amber-400/80" aria-hidden="true" />
                    <span>{{ line }}</span>
                  </li>
                </ul>
              </article>
            </div>
          </div>
        </section>

        <section class="px-4 py-12 sm:px-8 lg:px-12 lg:py-16">
          <div class="landing-cta-panel relative mx-auto max-w-7xl overflow-hidden rounded-[2rem] px-6 py-8 text-white sm:px-10 sm:py-10">
            <div class="landing-cta-panel__bg pointer-events-none absolute inset-0 z-0" aria-hidden="true" />
            <div class="relative z-10 grid gap-6 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-center">
              <div>
                <div class="text-[0.78rem] font-bold uppercase tracking-[0.18em] text-amber-100/95">Sẵn sàng bắt đầu?</div>
                <h2 class="mt-3 text-[clamp(1.95rem,3vw,3rem)] font-black leading-[1.04] tracking-[-0.04em]">
                  Dùng EduExam để vận hành kỳ thi online gọn hơn, rõ hơn và nhẹ đầu hơn.
                </h2>
                <p class="mt-4 max-w-2xl text-[0.95rem] leading-7 text-slate-300">
                  Từ tạo đề, import file, làm bài đến giám sát và hậu kiểm, mọi bước đều nằm trong một luồng đồng nhất.
                </p>
              </div>

              <div class="flex flex-col gap-3 sm:flex-row lg:flex-col lg:items-end">
                <RouterLink to="/help-center" class="landing-btn landing-btn-secondary landing-btn-on-dark max-w-full sm:max-w-none">
                  Mở trung tâm trợ giúp
                </RouterLink>
                <RouterLink
                  v-if="!authStore.isAuthenticated"
                  to="/login"
                  class="text-center text-sm font-semibold text-white/90 underline-offset-4 hover:text-white hover:underline"
                >
                  Đã có tài khoản? Đăng nhập
                </RouterLink>
                <RouterLink
                  v-else
                  :to="primaryCta.to"
                  class="landing-btn landing-btn-primary landing-btn-on-dark max-w-full sm:max-w-none"
                >
                  {{ primaryCta.label }}
                </RouterLink>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '../../stores/authStore'

const authStore = useAuthStore()
authStore.syncFromStorage()

const primaryCta = computed(() => {
  if (authStore.isAuthenticated) {
    return {
      to: authStore.dashboardPath || '/select-role',
      label: 'Vào bảng điều khiển'
    }
  }

  return {
    to: '/register',
    label: 'Bắt đầu ngay'
  }
})

const navLinks = [
  { label: 'Tổng quan', href: '#overview' },
  { label: 'Trải nghiệm', href: '#experience' }
]

const heroBadges = [
  'Online Exam Platform',
  'Realtime Monitoring',
  'Import Ready'
]

const heroStats = [
  { value: 'Một luồng', label: 'Từ tạo đề đến kết quả, không nhảy màn rời rạc' },
  { value: 'Theo dõi', label: 'Giám sát và cảnh báo gom về một dashboard' },
  { value: 'Kiểm duyệt', label: 'Import có preview trước khi công bố' }
]

const featureHighlights = [
  {
    icon: 'school',
    title: 'Giảng viên & giám thị',
    text: 'Tạo kỳ thi, lịch, theo dõi tiến độ và xử lý cảnh báo trong cùng một không gian làm việc.'
  },
  {
    icon: 'edit_note',
    title: 'Học sinh',
    text: 'Làm bài tập trung, đánh dấu câu, auto-save và xem lại trước khi nộp — ít nhiễu, dễ đọc.'
  },
  {
    icon: 'admin_panel_settings',
    title: 'Quản trị',
    text: 'Import câu hỏi có bước review, cấu hình tổng thể và hỗ trợ vận hành demo/đào tạo.'
  }
]

const visualCards = [
  {
    title: 'Màn hình làm bài',
    tag: 'Học sinh',
    tagTone: 'text-primary-600',
    copy: 'Tập trung vào đề, timer và trạng thái từng câu.',
    bullets: ['Sidebar tiến độ / đánh dấu', 'Timer và ngưỡng màu', 'Auto-save + xác nhận trước khi nộp']
  },
  {
    title: 'Giám sát realtime',
    tag: 'Giám thị',
    tagTone: 'text-amber-600',
    copy: 'Điểm rủi ro và sự kiện được gom để xử lý nhanh.',
    bullets: ['Risk score và timeline', 'Danh sách thí sinh theo phiên', 'Hành động hàng loạt khi cần']
  },
  {
    title: 'Import đề thi',
    tag: 'Quản trị',
    tagTone: 'text-emerald-600',
    copy: 'Không publish thẳng từ file — luôn có bước kiểm.',
    bullets: ['Upload nhiều định dạng phổ biến', 'Preview và danh sách issue', 'Chỉ lưu sau khi duyệt']
  }
]
</script>

<style scoped>
.landing-page {
  background:
    radial-gradient(circle at top right, rgba(141, 75, 0, 0.07), transparent 24%),
    radial-gradient(circle at left center, rgba(255, 183, 125, 0.12), transparent 20%),
    linear-gradient(180deg, #faf9f5 0%, #ffffff 38%, #f4f4f0 100%);
}

.landing-headline-serif {
  font-family: 'Noto Serif', Georgia, 'Times New Roman', serif;
  font-weight: 800;
  letter-spacing: -0.03em;
  line-height: 1.08;
}

.landing-chip {
  display: inline-flex;
  align-items: center;
  border-radius: 9999px;
  border: 1px solid rgba(219, 194, 176, 0.55);
  background: rgba(255, 255, 255, 0.88);
  padding: 0.52rem 0.92rem;
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #8d4b00;
  box-shadow: 0 16px 28px -24px rgba(141, 75, 0, 0.18);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

.landing-accent-text {
  background: linear-gradient(135deg, #b15f00 0%, #8d4b00 52%, #6e3900 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.landing-surface-card {
  border: 1px solid rgba(219, 194, 176, 0.45);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(250, 249, 245, 0.96) 100%);
  box-shadow: 0 28px 58px -42px rgba(27, 28, 26, 0.12), 0 18px 36px -30px rgba(141, 75, 0, 0.1);
}

.landing-stat-card {
  position: relative;
  overflow: hidden;
}

.landing-stat-card::after {
  content: '';
  position: absolute;
  inset: auto -1rem -1rem auto;
  width: 5rem;
  height: 5rem;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(177, 95, 0, 0.12), transparent 72%);
}

.landing-btn {
  display: inline-flex;
  min-height: 3rem;
  align-items: center;
  justify-content: center;
  border-radius: 1.05rem;
  padding: 0.82rem 1.28rem;
  font-size: 0.95rem;
  font-weight: 700;
  text-decoration: none;
  transition: transform 160ms ease, box-shadow 180ms ease, border-color 180ms ease, background-color 180ms ease;
}

.landing-btn:hover {
  transform: translateY(-1px);
}

.landing-btn-primary {
  border: 1px solid rgba(141, 75, 0, 0.22);
  background: linear-gradient(135deg, #b15f00 0%, #8d4b00 52%, #6e3900 100%);
  color: white;
  box-shadow: 0 20px 38px -24px rgba(141, 75, 0, 0.4);
}

.landing-btn-secondary {
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(255, 255, 255, 0.84);
  color: #0f172a;
}

/* Header: vừa mobile, tránh chồng hai nút full-width */
.landing-btn-header-secondary {
  min-height: 2.65rem;
  padding: 0.62rem 0.95rem;
  font-size: 0.88rem;
}

@media (min-width: 640px) {
  .landing-btn-header-secondary {
    min-height: 3rem;
    padding: 0.82rem 1.28rem;
    font-size: 0.95rem;
  }
}

/* Nút phụ trên nền tối (khối CTA cuối) — không lặp style nút sáng */
.landing-btn-on-dark.landing-btn-secondary {
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.92);
}

.landing-btn-on-dark.landing-btn-secondary:hover {
  background: rgba(255, 255, 255, 0.14);
}

.landing-btn-on-dark.landing-btn-primary {
  border: 1px solid rgba(255, 255, 255, 0.25);
  background: linear-gradient(135deg, #fff 0%, #faf9f5 100%);
  color: #6e3900;
  box-shadow: 0 18px 40px -28px rgba(0, 0, 0, 0.35);
}

.landing-cta-panel__bg {
  background-image: url('/images/cta-academic-amber.svg');
  background-size: cover;
  background-position: center;
  opacity: 0.35;
}

.landing-cta-panel__bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(141, 75, 0, 0.88) 0%, rgba(110, 57, 0, 0.92) 50%, rgba(74, 40, 0, 0.94) 100%);
}

.landing-cta-panel {
  box-shadow: 0 40px 84px -48px rgba(74, 40, 0, 0.45);
}

@media (prefers-reduced-motion: reduce) {
  .landing-btn {
    transition: none;
  }

  .landing-btn:hover {
    transform: none;
  }
}

@media (max-width: 1023px) {
  .landing-page {
    background:
      radial-gradient(circle at top right, rgba(141, 75, 0, 0.06), transparent 28%),
      linear-gradient(180deg, #faf9f5 0%, #ffffff 32%, #f4f4f0 100%);
  }
}
</style>
