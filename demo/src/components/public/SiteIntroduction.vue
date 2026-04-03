<template>
  <div class="edu-container">

    <!-- Navigation -->
    <nav class="edu-nav" :class="{ 'edu-nav--scrolled': isScrolled }">
      <div class="edu-nav__inner">
        <a href="/" class="edu-nav__logo">
          <AppLogo size="sm" variant="default" tag="span" />
        </a>
        <div class="edu-nav__links">
          <a href="#features">Tính năng</a>
          <a href="#workflow">Quy trình</a>
          <a href="#roles">Vai trò</a>
          <RouterLink to="/help-center">Trợ giúp</RouterLink>
        </div>
        <div class="edu-nav__actions">
          <RouterLink to="/login" class="edu-nav__login">Đăng nhập</RouterLink>
          <RouterLink :to="primaryCta.to" class="edu-nav__cta">{{ primaryCta.label }}</RouterLink>
        </div>
      </div>
    </nav>

    <!-- Marquee Banner -->
    <div class="marquee" aria-hidden="true">
      <div class="marquee__track">
        <span v-for="(item, i) in marqueeItems" :key="`a-${i}`" class="marquee__item">{{ item }}</span>
        <span v-for="(item, i) in marqueeItems" :key="`b-${i}`" class="marquee__item" aria-hidden="true">{{ item }}</span>
      </div>
    </div>

    <!-- Hero Section -->
    <section class="hero">
      <div class="hero__content">
        <div class="hero__eyebrow edu-animate-fade-up">Hệ thống thi trực tuyến</div>
        <h1 class="hero__brand edu-animate-fade-up-d2">
          <span class="brand-main">Edu</span><span class="brand-accent">Exam</span>
        </h1>
        <p class="hero__tagline edu-animate-fade-up-d4">
          Thi thông minh.<br>Giám sát chính xác.<br>Kết quả minh bạch.
        </p>
        <p class="hero__desc edu-animate-fade-up-d5">
          Nền tảng thi trực tuyến hàng đầu Việt Nam. Kết hợp sự đơn giản của giấy với sức mạnh của phần mềm hiện đại — cho giáo viên, học sinh và quản trị viên.
        </p>
        <div class="hero__actions edu-animate-fade-up-d6">
          <RouterLink :to="primaryCta.to" class="edu-btn edu-btn--primary">
            {{ primaryCta.label }}
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
          </RouterLink>
          <RouterLink to="/help-center" class="edu-btn edu-btn--ghost">
            Tìm hiểu thêm
          </RouterLink>
        </div>
      </div>

      <div class="hero__visual edu-animate-fade-left">
        <div class="preview-card">
          <div class="preview-card__header">
            <div class="preview-dots">
              <span /><span /><span />
            </div>
            <span class="preview-badge">
              <span class="badge-dot"></span>
              LIVE
            </span>
          </div>
          <div class="preview-card__body">
            <div class="preview-exam">
              <span class="preview-exam__name">Toán rời rạc</span>
              <span class="preview-exam__meta">42 học sinh &bull; 45 phút</span>
            </div>
            <div class="preview-rows">
              <div class="preview-row">
                <div class="preview-row__avatar" style="background: linear-gradient(135deg, #4f46e5, #6366f1)">NA</div>
                <div class="preview-row__info">
                  <span class="preview-row__name">Nguyễn An</span>
                  <div class="preview-row__bar"><div class="preview-row__fill" style="width: 68%"></div></div>
                </div>
                <span class="preview-row__pct">68%</span>
              </div>
              <div class="preview-row">
                <div class="preview-row__avatar" style="background: linear-gradient(135deg, #f59e0b, #d97706)">TV</div>
                <div class="preview-row__info">
                  <span class="preview-row__name">Trần Văn</span>
                  <div class="preview-row__bar"><div class="preview-row__fill preview-row__fill--warn" style="width: 45%"></div></div>
                </div>
                <span class="preview-row__pct">45%</span>
              </div>
              <div class="preview-row">
                <div class="preview-row__avatar" style="background: linear-gradient(135deg, #10b981, #059669)">LGH</div>
                <div class="preview-row__info">
                  <span class="preview-row__name">Lê Gia Huy</span>
                  <div class="preview-row__bar"><div class="preview-row__fill" style="width: 92%"></div></div>
                </div>
                <span class="preview-row__pct">92%</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Floating badge -->
        <div class="float-badge edu-animate-fade-up-d7">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
          <span>Độ chính xác AI: 98%</span>
        </div>
      </div>
    </section>

    <!-- Divider -->
    <div class="section-divider" />

    <!-- Stats Section -->
    <section class="stats">
      <div class="stats__container">
        <div class="stat edu-animate-fade-up-d1" v-for="(s, i) in stats" :key="i">
          <span class="stat__num">{{ displayStats[i] }}{{ s.suffix }}</span>
          <span class="stat__label">{{ s.label }}</span>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="features" id="features">
      <div class="features__container">
        <div class="section-header edu-animate-fade-up">
          <span class="section-eyebrow">Tính năng</span>
          <h2 class="section-title">Giải pháp toàn diện<br>cho giáo dục</h2>
          <p class="section-desc">
            EduExam cung cấp đầy đủ công cụ để quản lý, tổ chức và giám sát kỳ thi trực tuyến một cách hiệu quả.
          </p>
        </div>

        <div class="features__grid">
          <div class="feat edu-animate-fade-up-d1" v-for="(f, i) in features" :key="i">
            <div class="feat__icon" v-html="f.icon" />
            <h3 class="feat__title">{{ f.title }}</h3>
            <p class="feat__desc">{{ f.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Workflow Section -->
    <section class="workflow" id="workflow">
      <div class="workflow__container">
        <div class="section-header edu-animate-fade-up">
          <span class="section-eyebrow">Quy trình</span>
          <h2 class="section-title">4 bước đơn giản<br>để bắt đầu</h2>
        </div>

        <div class="workflow__timeline">
          <div class="workflow__step edu-animate-fade-up" v-for="(s, i) in steps" :key="i" :style="`animation-delay: ${0.1 + i * 0.12}s`">
            <div class="workflow__step-num">{{ String(i + 1).padStart(2, '0') }}</div>
            <div class="workflow__step-icon" v-html="s.icon" />
            <h4 class="workflow__step-title">{{ s.title }}</h4>
            <p class="workflow__step-desc">{{ s.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Roles Section -->
    <section class="roles" id="roles">
      <div class="roles__container">
        <div class="section-header edu-animate-fade-up">
          <span class="section-eyebrow">Vai trò</span>
          <h2 class="section-title">Phù hợp với<br>mọi đối tượng</h2>
          <p class="section-desc">
            EduExam được thiết kế để phục vụ giáo viên, học sinh và quản trị viên với những tính năng riêng biệt.
          </p>
        </div>

        <div class="roles__grid">
          <div class="role-card edu-animate-fade-up" v-for="(r, i) in roles" :key="i" :style="`animation-delay: ${0.1 + i * 0.12}s`">
            <div class="role-card__icon" v-html="r.icon" />
            <h3 class="role-card__title">{{ r.title }}</h3>
            <p class="role-card__desc">{{ r.desc }}</p>
            <ul class="role-card__list">
              <li v-for="(item, j) in r.features" :key="j">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
                {{ item }}
              </li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="cta">
      <div class="cta__inner edu-animate-fade-up">
        <h2 class="cta__title">Sẵn sàng bắt đầu?</h2>
        <p class="cta__desc">
          Tham gia cùng hàng nghìn giáo viên và học sinh đang sử dụng EduExam mỗi ngày.
        </p>
        <div class="cta__actions">
          <RouterLink :to="primaryCta.to" class="edu-btn edu-btn--primary edu-btn--lg">
            {{ primaryCta.label }}
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
          </RouterLink>
          <RouterLink to="/help-center" class="edu-btn edu-btn--outline edu-btn--lg">
            Liên hệ hỗ trợ
          </RouterLink>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="footer">
      <div class="footer__inner">
        <div class="footer__brand">
          <a href="/" class="footer__logo">
            <AppLogo size="sm" variant="default" tag="span" />
            <span>EduExam</span>
          </a>
          <p class="footer__desc">
            Nền tảng thi trực tuyến hàng đầu Việt Nam, giúp quá trình giảng dạy và học tập trở nên dễ dàng hơn.
          </p>
        </div>
        <div class="footer__links">
          <div class="footer__col">
            <h4>Sản phẩm</h4>
            <a href="#features">Tính năng</a>
            <a href="#">Bảng giá</a>
            <a href="#">API</a>
          </div>
          <div class="footer__col">
            <h4>Hỗ trợ</h4>
            <a href="#">Trung tâm trợ giúp</a>
            <a href="#">Tài liệu</a>
            <a href="#">Liên hệ</a>
          </div>
          <div class="footer__col">
            <h4>Công ty</h4>
            <a href="#">Giới thiệu</a>
            <a href="#">Blog</a>
            <a href="#">Tuyển dụng</a>
          </div>
        </div>
      </div>
      <div class="footer__bottom">
        <p>&copy; 2024 EduExam. Mọi quyền được bảo lưu.</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '../../stores/authStore'
import AppLogo from '../common/AppLogo.vue'

const authStore = useAuthStore()
authStore.syncFromStorage()

const isScrolled = ref(false)

const primaryCta = {
  get to() {
    return authStore.isAuthenticated ? authStore.dashboardPath || '/select-role' : '/register'
  },
  get label() {
    return authStore.isAuthenticated ? 'Vào bảng điều khiển' : 'Bắt đầu miễn phí'
  }
}

const marqueeItems = [
  'THI TRẮC NGHIỆM THÔNG MINH',
  'GIÁM SÁT REALTIME',
  'PHÂN TÍCH KẾT QUẢ CHI TIẾT',
  'HỖ TRỢ AI TẠO ĐỀ',
  'BẢO MẬT CAO CẤP',
  'ĐA NỀN TẢNG',
]

const stats = [
  { target: 10000, suffix: '+', label: 'Học sinh', float: false },
  { target: 500, suffix: '+', label: 'Câu hỏi', float: false },
  { target: 98, suffix: '%', label: 'Độ chính xác', float: false },
  { target: 4.9, suffix: '', label: 'Đánh giá', float: true },
]

const displayStats = reactive(['0', '0', '0', '0.0'])

const animateCounter = (index, target, isFloat) => {
  const duration = 1800
  const startTime = performance.now()
  const update = (currentTime) => {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    const easeOut = 1 - Math.pow(1 - progress, 3)
    const current = target * easeOut
    if (isFloat) {
      displayStats[index] = current.toFixed(1)
    } else if (target >= 1000) {
      displayStats[index] = Math.floor(current).toLocaleString()
    } else {
      displayStats[index] = Math.floor(current).toString()
    }
    if (progress < 1) requestAnimationFrame(update)
  }
  requestAnimationFrame(update)
}

const features = [
  {
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>`,
    title: 'Tạo đề thông minh',
    desc: 'Sử dụng AI để tạo đề thi nhanh chóng với nhiều loại câu hỏi và độ khó khác nhau.',
  },
  {
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>`,
    title: 'Giám sát realtime',
    desc: 'Theo dõi học sinh trong thời gian thực, phát hiện hành vi bất thường và cảnh báo kịp thời.',
  },
  {
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>`,
    title: 'Phân tích chi tiết',
    desc: 'Báo cáo thống kê điểm thi, phân tích kết quả theo từng câu hỏi và từng học sinh.',
  },
  {
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>`,
    title: 'Bảo mật cao cấp',
    desc: 'Mã hóa dữ liệu, xác thực hai lớp và các biện pháp bảo mật tiên tiến nhất.',
  },
  {
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg>`,
    title: 'Quản lý người dùng',
    desc: 'Phân quyền rõ ràng cho giáo viên, học sinh và quản trị viên.',
  },
  {
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>`,
    title: 'Đa nền tảng',
    desc: 'Truy cập từ mọi thiết bị: máy tính, tablet, điện thoại với giao diện tương thích.',
  },
]

const steps = [
  {
    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="8.5" cy="7" r="4"/><line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/></svg>`,
    title: 'Đăng ký tài khoản',
    desc: 'Tạo tài khoản nhanh chóng với email hoặc mạng xã hội',
  },
  {
    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>`,
    title: 'Tạo đề thi',
    desc: 'Sử dụng AI hoặc tạo đề thủ công với nhiều tùy chọn',
  },
  {
    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polygon points="5 3 19 12 5 21 5 3"/></svg>`,
    title: 'Tổ chức thi',
    desc: 'Phát đề, giám sát và xử lý sự cố trong thời gian thực',
  },
  {
    icon: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>`,
    title: 'Xem kết quả',
    desc: 'Phân tích chi tiết, xuất báo cáo và đánh giá hiệu quả',
  },
]

const roles = [
  {
    icon: `<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg>`,
    title: 'Giáo viên',
    desc: 'Tạo đề thi, giám sát phòng thi, quản lý kết quả và xuất báo cáo chi tiết.',
    features: ['Tạo đề với AI', 'Giám sát realtime', 'Báo cáo chi tiết'],
  },
  {
    icon: `<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>`,
    title: 'Học sinh',
    desc: 'Tham gia thi trực tuyến, xem điểm số và theo dõi tiến độ học tập.',
    features: ['Thi mọi lúc mọi nơi', 'Xem kết quả ngay', 'Lịch sử thi chi tiết'],
  },
  {
    icon: `<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06a1.65 1.65 0 00.33-1.82 1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06a1.65 1.65 0 001.82.33H9a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/></svg>`,
    title: 'Quản trị viên',
    desc: 'Quản lý người dùng, cấu hình hệ thống và theo dõi hoạt động toàn diện.',
    features: ['Quản lý người dùng', 'Cấu hình hệ thống', 'Thống kê tổng quan'],
  },
]

let countersStarted = false

const startCountersIfVisible = () => {
  if (countersStarted) return
  const statsSection = document.querySelector('.stats__container')
  if (!statsSection) return
  const rect = statsSection.getBoundingClientRect()
  if (rect.top < window.innerHeight) {
    countersStarted = true
    stats.forEach((s, i) => {
      setTimeout(() => animateCounter(i, s.target, s.float), 300)
    })
  }
}

const handleScroll = () => {
  isScrolled.value = window.scrollY > 20
  startCountersIfVisible()
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
  startCountersIfVisible()
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
/* =============================================
   DESIGN TOKENS — Ghia Editorial Style
   ============================================= */
:global(:root) {
  --edu-bg:           #FAF7F2;
  --edu-bg-white:     #FFFFFF;
  --edu-text:         #1A1A1A;
  --edu-text-sec:     #6B6560;
  --edu-primary:      #4f46e5;
  --edu-primary-h:    #4338ca;
  --edu-accent:       #8B6914;
  --edu-border:       rgba(0,0,0,0.07);
  --edu-border-hi:    rgba(0,0,0,0.13);
  --edu-shadow:       0 2px 8px rgba(0,0,0,0.05);
  --edu-shadow-md:    0 4px 24px rgba(0,0,0,0.09);
  --edu-radius:       12px;
  --edu-radius-lg:    16px;
  --edu-fs-serif:    'Cormorant Garamond', Georgia, serif;
  --edu-fs-body:      'Be Vietnam Pro', -apple-system, sans-serif;
}

/* =============================================
   BASE
   ============================================= */
.edu-container {
  min-height: 100vh;
  background: var(--edu-bg);
  color: var(--edu-text);
  font-family: var(--edu-fs-body);
  position: relative;
  overflow-x: hidden;
}

/* =============================================
   NAVIGATION
   ============================================= */
.edu-nav {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 100;
  padding: 1.5rem 3rem;
  transition: background 0.35s ease, box-shadow 0.35s ease, padding 0.35s ease;
}

.edu-nav--scrolled {
  background: rgba(250, 247, 242, 0.97);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 0 1px 0 var(--edu-border);
  padding: 1rem 3rem;
}

.edu-nav__inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.edu-nav__logo {
  display: flex;
  align-items: center;
  text-decoration: none;
  opacity: 0.88;
  transition: opacity 0.2s ease;
}
.edu-nav__logo:hover { opacity: 1; }

.edu-nav__links {
  display: flex;
  gap: 2.5rem;
}

.edu-nav__links a {
  color: var(--edu-text);
  text-decoration: none;
  font-size: 0.875rem;
  font-weight: 500;
  letter-spacing: 0.01em;
  transition: color 0.2s ease;
}
.edu-nav__links a:hover { color: var(--edu-primary); }

.edu-nav__actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.edu-nav__login {
  color: var(--edu-text);
  text-decoration: none;
  font-size: 0.875rem;
  font-weight: 500;
  padding: 0.4rem 0.875rem;
  border-radius: 6px;
  transition: all 0.2s ease;
}
.edu-nav__login:hover {
  color: var(--edu-primary);
  background: rgba(79,70,229,0.06);
}

.edu-nav__cta {
  background: var(--edu-text);
  color: var(--edu-bg);
  text-decoration: none;
  font-size: 0.825rem;
  font-weight: 600;
  padding: 0.5rem 1.25rem;
  border-radius: 6px;
  letter-spacing: 0.02em;
  transition: background 0.2s ease, transform 0.2s ease;
}
.edu-nav__cta:hover {
  background: var(--edu-primary);
  transform: translateY(-1px);
}

/* =============================================
   MARQUEE — Ghia Editorial Strip
   ============================================= */
.marquee {
  position: absolute;
  top: 76px;
  left: 0; right: 0;
  overflow: hidden;
  padding: 0.6rem 0;
  background: var(--edu-bg-white);
  border-top: 1px solid var(--edu-border);
  border-bottom: 1px solid var(--edu-border);
  z-index: 10;
}

.marquee__track {
  display: flex;
  width: max-content;
  animation: marqueeScroll 38s linear infinite;
}

.marquee__track:hover {
  animation-play-state: paused;
}

.marquee__item {
  font-family: var(--edu-fs-body);
  font-size: 0.68rem;
  font-weight: 600;
  color: var(--edu-primary);
  letter-spacing: 0.22em;
  text-transform: uppercase;
  padding: 0 2.5rem;
  white-space: nowrap;
  position: relative;
}

.marquee__item::after {
  content: '·';
  position: absolute;
  right: -0.3rem;
  opacity: 0.35;
  font-size: 1rem;
}

@keyframes marqueeScroll {
  0%   { transform: translateX(0); }
  100% { transform: translateX(-50%); }
}

/* =============================================
   HERO — Editorial Ghia Style
   ============================================= */
.hero {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1fr;
  align-items: center;
  padding: 10rem 3rem 5rem;
  max-width: 1200px;
  margin: 0 auto;
  gap: 5rem;
}

.hero__content {
  padding-right: 1rem;
}

.hero__eyebrow {
  display: inline-block;
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--edu-primary);
  margin-bottom: 1.25rem;
  opacity: 0.8;
}

.hero__brand {
  font-family: var(--edu-fs-serif);
  font-size: clamp(4rem, 7vw, 6.5rem);
  font-weight: 600;
  line-height: 0.95;
  letter-spacing: -0.02em;
  margin: 0 0 1.75rem;
  color: var(--edu-text);
}

.brand-main {
  display: block;
}

.brand-accent {
  display: block;
  color: var(--edu-primary);
  font-style: italic;
}

.hero__tagline {
  font-family: var(--edu-fs-serif);
  font-size: 1.5rem;
  font-weight: 400;
  font-style: italic;
  color: var(--edu-text-sec);
  line-height: 1.5;
  margin: 0 0 2rem;
}

.hero__desc {
  font-size: 0.975rem;
  color: var(--edu-text-sec);
  line-height: 1.9;
  margin-bottom: 2.75rem;
  max-width: 420px;
}

.hero__actions {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

/* =============================================
   HERO VISUAL
   ============================================= */
.hero__visual {
  position: relative;
}

.preview-card {
  background: var(--edu-bg-white);
  border: 1px solid var(--edu-border);
  border-radius: var(--edu-radius-lg);
  box-shadow: var(--edu-shadow-md);
  overflow: hidden;
  transition: box-shadow 0.35s ease, transform 0.35s ease;
}

.preview-card:hover {
  box-shadow: 0 10px 40px rgba(0,0,0,0.12);
  transform: translateY(-4px);
}

.preview-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.875rem 1.25rem;
  border-bottom: 1px solid var(--edu-border);
  background: var(--edu-bg);
}

.preview-dots {
  display: flex;
  gap: 0.45rem;
}

.preview-dots span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--edu-border-hi);
}
.preview-dots span:first-child { background: #ef4444; }
.preview-dots span:nth-child(2) { background: #f59e0b; }
.preview-dots span:nth-child(3) { background: #22c55e; }

.preview-badge {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.62rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #ef4444;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ef4444;
  animation: badgePulse 2s ease-in-out infinite;
}

@keyframes badgePulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%       { opacity: 0.5; transform: scale(0.85); }
}

.preview-card__body {
  padding: 1.25rem;
}

.preview-exam {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  margin-bottom: 1.25rem;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid var(--edu-border);
}

.preview-exam__name {
  font-weight: 600;
  font-size: 1rem;
  color: var(--edu-text);
}

.preview-exam__meta {
  font-size: 0.78rem;
  color: var(--edu-text-sec);
}

.preview-rows {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.preview-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.4rem;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s ease;
}
.preview-row:hover { background: rgba(79,70,229,0.04); }

.preview-row__avatar {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.68rem;
  font-weight: 700;
  color: white;
  flex-shrink: 0;
}

.preview-row__info {
  flex: 1;
  min-width: 0;
}

.preview-row__name {
  display: block;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--edu-text);
  margin-bottom: 0.25rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.preview-row__bar {
  height: 4px;
  background: var(--edu-border);
  border-radius: 2px;
  overflow: hidden;
}

.preview-row__fill {
  height: 100%;
  background: linear-gradient(90deg, var(--edu-primary), #6366f1);
  border-radius: 2px;
  animation: barGrow 1.4s cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: 0.6s;
}

@keyframes barGrow {
  from { width: 0 !important; }
}

.preview-row__fill--warn {
  background: linear-gradient(90deg, #f59e0b, #d97706);
}

.preview-row__pct {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--edu-text);
  min-width: 30px;
  text-align: right;
}

/* Floating badge */
.float-badge {
  position: absolute;
  bottom: -1.5rem;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: var(--edu-bg-white);
  border: 1px solid var(--edu-border);
  border-radius: 100px;
  padding: 0.5rem 1rem;
  box-shadow: var(--edu-shadow);
  white-space: nowrap;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--edu-text);
  animation: floatBadgeIn 0.6s cubic-bezier(0.16, 1, 0.3, 1) 1s both;
}

@keyframes floatBadgeIn {
  from { opacity: 0; transform: translateX(-50%) translateY(8px); }
  to   { opacity: 1; transform: translateX(-50%) translateY(0); }
}

.float-badge svg {
  color: #22c55e;
  flex-shrink: 0;
}

/* =============================================
   BUTTONS — Minimal Editorial
   ============================================= */
.edu-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.8rem 1.625rem;
  font-family: var(--edu-fs-body);
  font-size: 0.875rem;
  font-weight: 600;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  letter-spacing: 0.01em;
}

.edu-btn--primary {
  background: var(--edu-text);
  color: var(--edu-bg);
  box-shadow: 0 2px 12px rgba(26,26,26,0.15);
}
.edu-btn--primary:hover {
  background: var(--edu-primary);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(79,70,229,0.3);
}

.edu-btn--outline {
  background: transparent;
  border: 1.5px solid var(--edu-border-hi);
  color: var(--edu-text);
}
.edu-btn--outline:hover {
  border-color: var(--edu-primary);
  color: var(--edu-primary);
  background: rgba(79,70,229,0.04);
  transform: translateY(-2px);
}

.edu-btn--ghost {
  background: transparent;
  color: var(--edu-text-sec);
  padding-left: 0.25rem;
  border: none;
}
.edu-btn--ghost:hover { color: var(--edu-primary); }

.edu-btn--lg {
  padding: 0.9rem 2.125rem;
  font-size: 0.925rem;
}

/* =============================================
   DIVIDER — Editorial thin line
   ============================================= */
.section-divider {
  max-width: 1200px;
  margin: 0 auto;
  height: 1px;
  background: var(--edu-border);
}

/* =============================================
   STATS — Clean grid
   ============================================= */
.stats {
  padding: 5rem 3rem;
}

.stats__container {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border: 1px solid var(--edu-border);
  border-radius: var(--edu-radius);
  overflow: hidden;
}

.stat {
  background: var(--edu-bg-white);
  text-align: center;
  padding: 2.75rem 1.5rem;
  border-right: 1px solid var(--edu-border);
  transition: background 0.2s ease;
}
.stat:last-child { border-right: none; }
.stat:hover { background: rgba(79,70,229,0.025); }

.stat__num {
  display: block;
  font-family: var(--edu-fs-serif);
  font-size: 3.25rem;
  font-weight: 600;
  color: var(--edu-primary);
  line-height: 1;
  margin-bottom: 0.5rem;
  letter-spacing: -0.02em;
}

.stat__label {
  display: block;
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--edu-text-sec);
  letter-spacing: 0.02em;
}

/* =============================================
   SECTION HEADER — Editorial
   ============================================= */
.section-header {
  text-align: center;
  margin-bottom: 3.5rem;
}

.section-eyebrow {
  display: inline-block;
  font-size: 0.68rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.22em;
  color: var(--edu-primary);
  margin-bottom: 1rem;
  opacity: 0.85;
}

.section-title {
  font-family: var(--edu-fs-serif);
  font-size: clamp(2.25rem, 4vw, 3rem);
  font-weight: 600;
  color: var(--edu-text);
  line-height: 1.15;
  letter-spacing: -0.01em;
  margin-bottom: 1rem;
}

.section-desc {
  font-size: 0.975rem;
  color: var(--edu-text-sec);
  max-width: 480px;
  margin: 0 auto;
  line-height: 1.85;
}

/* =============================================
   FEATURES — Editorial grid
   ============================================= */
.features {
  padding: 6rem 3rem;
  background: var(--edu-bg-white);
}

.features__container {
  max-width: 1200px;
  margin: 0 auto;
}

.features__grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  border: 1px solid var(--edu-border);
  border-radius: var(--edu-radius);
  overflow: hidden;
}

.feat {
  padding: 2.25rem;
  border-right: 1px solid var(--edu-border);
  border-bottom: 1px solid var(--edu-border);
  transition: background 0.25s ease, box-shadow 0.25s ease;
}
.feat:nth-child(3n) { border-right: none; }
.feat:nth-last-child(-n+3) { border-bottom: none; }
.feat:hover {
  background: rgba(79,70,229,0.025);
  box-shadow: inset 0 0 0 1.5px var(--edu-primary);
  z-index: 1;
  position: relative;
}

.feat__icon {
  width: 46px;
  height: 46px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(79,70,229,0.07);
  border-radius: 10px;
  color: var(--edu-primary);
  margin-bottom: 1.25rem;
  transition: all 0.25s ease;
  flex-shrink: 0;
}
.feat:hover .feat__icon {
  background: var(--edu-primary);
  color: white;
  transform: scale(1.08);
}

.feat__title {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--edu-text);
  margin-bottom: 0.5rem;
  transition: color 0.2s ease;
}
.feat:hover .feat__title { color: var(--edu-primary); }

.feat__desc {
  font-size: 0.85rem;
  color: var(--edu-text-sec);
  line-height: 1.75;
}

/* =============================================
   WORKFLOW — Timeline editorial
   ============================================= */
.workflow {
  padding: 6rem 3rem;
  background: var(--edu-bg);
}

.workflow__container {
  max-width: 1200px;
  margin: 0 auto;
}

.workflow__timeline {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  position: relative;
}

.workflow__timeline::before {
  content: '';
  position: absolute;
  top: 32px;
  left: calc(12.5% + 18px);
  right: calc(12.5% + 18px);
  height: 1px;
  background: var(--edu-border-hi);
}

.workflow__step {
  text-align: center;
  position: relative;
  padding: 0 1rem;
}

.workflow__step-num {
  width: 48px;
  height: 48px;
  margin: 0 auto 1.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--edu-bg-white);
  border: 1.5px solid var(--edu-border-hi);
  border-radius: 50%;
  font-family: var(--edu-fs-serif);
  font-weight: 600;
  font-size: 0.875rem;
  color: var(--edu-primary);
  position: relative;
  z-index: 1;
  transition: all 0.3s ease;
}
.workflow__step:hover .workflow__step-num {
  background: var(--edu-primary);
  border-color: var(--edu-primary);
  color: white;
  box-shadow: 0 0 0 5px rgba(79,70,229,0.1);
  transform: scale(1.1);
}

.workflow__step-icon {
  width: 52px;
  height: 52px;
  margin: 0 auto 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--edu-bg-white);
  border: 1px solid var(--edu-border);
  border-radius: 14px;
  color: var(--edu-primary);
  transition: all 0.3s ease;
}
.workflow__step:hover .workflow__step-icon {
  transform: translateY(-5px);
  box-shadow: var(--edu-shadow-md);
  border-color: var(--edu-primary);
}

.workflow__step-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--edu-text);
  margin-bottom: 0.375rem;
  transition: color 0.2s ease;
}
.workflow__step:hover .workflow__step-title { color: var(--edu-primary); }

.workflow__step-desc {
  font-size: 0.825rem;
  color: var(--edu-text-sec);
  line-height: 1.6;
  max-width: 180px;
  margin: 0 auto;
}

/* =============================================
   ROLES — Editorial cards
   ============================================= */
.roles {
  padding: 6rem 3rem;
  background: var(--edu-bg-white);
}

.roles__container {
  max-width: 1200px;
  margin: 0 auto;
}

.roles__grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
}

.role-card {
  background: var(--edu-bg);
  border: 1px solid var(--edu-border);
  border-radius: var(--edu-radius-lg);
  padding: 2.75rem 2rem;
  text-align: center;
  transition: all 0.35s ease;
}

.role-card:hover {
  border-color: var(--edu-border-hi);
  box-shadow: var(--edu-shadow-md);
  transform: translateY(-5px);
}

.role-card__icon {
  width: 68px;
  height: 68px;
  margin: 0 auto 1.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 20px;
  transition: all 0.35s ease;
}

.role-card:nth-child(1) .role-card__icon {
  background: rgba(79,70,229,0.08);
  color: var(--edu-primary);
}
.role-card:nth-child(1):hover .role-card__icon {
  background: var(--edu-primary);
  color: white;
  transform: scale(1.05);
}

.role-card:nth-child(2) .role-card__icon {
  background: rgba(16,185,129,0.08);
  color: #10b981;
}
.role-card:nth-child(2):hover .role-card__icon {
  background: linear-gradient(135deg, #10b981, #059669);
  color: white;
}

.role-card:nth-child(3) .role-card__icon {
  background: rgba(245,158,11,0.08);
  color: #f59e0b;
}
.role-card:nth-child(3):hover .role-card__icon {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: white;
}

.role-card__title {
  font-family: var(--edu-fs-serif);
  font-size: 1.35rem;
  font-weight: 600;
  color: var(--edu-text);
  margin-bottom: 0.625rem;
}

.role-card__desc {
  font-size: 0.875rem;
  color: var(--edu-text-sec);
  line-height: 1.7;
  margin-bottom: 1.5rem;
}

.role-card__list {
  list-style: none;
  padding: 0;
  margin: 0;
  text-align: left;
}

.role-card__list li {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: var(--edu-text-sec);
  padding: 0.3rem 0;
}

.role-card__list li svg {
  width: 13px;
  height: 13px;
  color: #22c55e;
  flex-shrink: 0;
}

/* =============================================
   CTA — Editorial full-width
   ============================================= */
.cta {
  padding: 8rem 3rem;
  text-align: center;
  background: var(--edu-bg);
  border-top: 1px solid var(--edu-border);
}

.cta__inner {
  max-width: 620px;
  margin: 0 auto;
}

.cta__title {
  font-family: var(--edu-fs-serif);
  font-size: clamp(2.5rem, 4vw, 3.5rem);
  font-weight: 600;
  color: var(--edu-text);
  letter-spacing: -0.01em;
  margin-bottom: 1rem;
  line-height: 1.1;
}

.cta__desc {
  font-size: 1rem;
  color: var(--edu-text-sec);
  margin-bottom: 2.75rem;
  line-height: 1.8;
}

.cta__actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
}

/* =============================================
   FOOTER — Editorial minimal
   ============================================= */
.footer {
  padding: 4rem 3rem 2rem;
  background: var(--edu-bg-white);
  border-top: 1px solid var(--edu-border);
}

.footer__inner {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.5fr repeat(3, 1fr);
  gap: 3rem;
  margin-bottom: 3rem;
}

.footer__logo {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  text-decoration: none;
  color: var(--edu-text);
  font-family: var(--edu-fs-serif);
  font-weight: 600;
  font-size: 1.1rem;
  margin-bottom: 1rem;
}

.footer__desc {
  font-size: 0.875rem;
  color: var(--edu-text-sec);
  line-height: 1.8;
  max-width: 260px;
}

.footer__col h4 {
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--edu-text);
  margin-bottom: 1.125rem;
}

.footer__col a {
  display: block;
  color: var(--edu-text-sec);
  text-decoration: none;
  font-size: 0.85rem;
  padding: 0.25rem 0;
  transition: color 0.2s ease, transform 0.2s ease;
}
.footer__col a:hover {
  color: var(--edu-primary);
  transform: translateX(3px);
}

.footer__bottom {
  max-width: 1200px;
  margin: 0 auto;
  padding-top: 1.75rem;
  border-top: 1px solid var(--edu-border);
  text-align: center;
  font-size: 0.78rem;
  color: var(--edu-text-sec);
}

/* =============================================
   RESPONSIVE
   ============================================= */
@media (max-width: 1024px) {
  .hero {
    grid-template-columns: 1fr;
    gap: 3rem;
    padding-top: 11rem;
    text-align: center;
  }

  .hero__desc { max-width: 100%; }
  .hero__actions { justify-content: center; }

  .hero__visual { max-width: 440px; margin: 0 auto; }

  .features__grid { grid-template-columns: repeat(2, 1fr); }
  .feat:nth-child(3n) { border-right: 1px solid var(--edu-border); }
  .feat:nth-child(2n) { border-right: none; }
  .feat:nth-last-child(-n+3) { border-bottom: 1px solid var(--edu-border); }
  .feat:nth-last-child(-n+2) { border-bottom: none; }

  .roles__grid { grid-template-columns: repeat(2, 1fr); }
  .workflow__timeline { grid-template-columns: repeat(2, 1fr); gap: 2.5rem; }
  .workflow__timeline::before { display: none; }

  .stats__container { grid-template-columns: repeat(2, 1fr); }
  .stat:nth-child(2) { border-right: none; }
  .stat:nth-child(1),
  .stat:nth-child(2) { border-bottom: 1px solid var(--edu-border); }

  .footer__inner { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .edu-nav { padding: 1.25rem 1.5rem; }
  .edu-nav--scrolled { padding: 0.875rem 1.5rem; }
  .edu-nav__links { display: none; }

  .hero { padding: 10rem 1.5rem 4rem; }

  .features,
  .workflow,
  .roles { padding: 4rem 1.5rem; }

  .cta { padding: 5rem 1.5rem; }
  .stats { padding: 3.5rem 1.5rem; }

  .roles__grid { grid-template-columns: 1fr; }

  .footer { padding: 3rem 1.5rem 1.5rem; }
  .footer__inner { grid-template-columns: 1fr; gap: 2rem; }
}

@media (max-width: 480px) {
  .edu-btn--lg { padding: 0.8rem 1.5rem; font-size: 0.875rem; }

  .features__grid { grid-template-columns: 1fr; }
  .feat { border-right: none !important; }
  .feat:last-child { border-bottom: none !important; }

  .workflow__timeline { grid-template-columns: repeat(2, 1fr); }
  .stats__container { grid-template-columns: repeat(2, 1fr); }

  .cta__actions { flex-direction: column; align-items: center; }
}
</style>
