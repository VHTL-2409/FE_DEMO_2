<template>
  <div class="lp" ref="containerRef">
    <!-- Particle Canvas Background -->
    <canvas ref="particleCanvas" class="lp__particles" aria-hidden="true"></canvas>

    <!-- Navigation -->
    <nav class="lp__nav" :class="{ 'lp__nav--scrolled': isScrolled }">
      <div class="lp__nav-inner">
        <a href="/" class="lp__nav-logo">
          <AppLogo size="sm" variant="default" tag="span" :show-text="false" />
          <span class="lp__nav-brand">EduExam</span>
        </a>
        <div class="lp__nav-links">
          <a href="#features" class="lp__nav-link">Tính năng</a>
          <a href="#workflow" class="lp__nav-link">Quy trình</a>
          <a href="#testimonials" class="lp__nav-link">Đánh giá</a>
          <a href="#faq" class="lp__nav-link">FAQ</a>
          <RouterLink to="/help-center" class="lp__nav-link">Trợ giúp</RouterLink>
        </div>
        <div class="lp__nav-actions">
          <RouterLink to="/login" class="lp__nav-login">Đăng nhập</RouterLink>
          <RouterLink :to="primaryCta.to" class="lp__nav-cta">
            {{ primaryCta.label }}
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
          </RouterLink>
        </div>
        <!-- Mobile menu toggle -->
        <button class="lp__nav-mobile-toggle" @click="mobileMenuOpen = !mobileMenuOpen" aria-label="Menu">
          <span></span><span></span><span></span>
        </button>
      </div>
      <!-- Mobile Menu -->
      <div class="lp__mobile-menu" :class="{ 'lp__mobile-menu--open': mobileMenuOpen }">
        <a href="#features" class="lp__mobile-link" @click="mobileMenuOpen = false">Tính năng</a>
        <a href="#workflow" class="lp__mobile-link" @click="mobileMenuOpen = false">Quy trình</a>
        <a href="#testimonials" class="lp__mobile-link" @click="mobileMenuOpen = false">Đánh giá</a>
        <a href="#faq" class="lp__mobile-link" @click="mobileMenuOpen = false">FAQ</a>
        <RouterLink to="/login" class="lp__mobile-link">Đăng nhập</RouterLink>
        <RouterLink :to="primaryCta.to" class="lp__mobile-cta">{{ primaryCta.label }}</RouterLink>
      </div>
    </nav>

    <!-- ===================== HERO SECTION ===================== -->
    <section class="lp__hero">
      <!-- Animated gradient orbs -->
      <div class="lp__hero-orbs" aria-hidden="true">
        <div class="lp__orb lp__orb--1"></div>
        <div class="lp__orb lp__orb--2"></div>
        <div class="lp__orb lp__orb--3"></div>
      </div>

      <!-- Grid pattern overlay -->
      <div class="lp__hero-grid" aria-hidden="true"></div>

      <div class="lp__hero-content">
        <!-- Badge -->
        <div class="lp__hero-badge animate-on-scroll" data-animate="badge">
          <span class="lp__hero-badge-dot"></span>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
          Nền tảng thi trực tuyến hàng đầu Việt Nam
        </div>

        <!-- Main Title with Typing Effect -->
        <h1 class="lp__hero-title animate-on-scroll" data-animate="title">
          <span class="lp__hero-title-main">
            <span class="lp__hero-title-brand">EduExam</span>
            <span class="lp__hero-title-sub">Thi trực tuyến thông minh</span>
          </span>
        </h1>

        <!-- Description with reveal animation -->
        <p class="lp__hero-desc animate-on-scroll" data-animate="desc">
          Không gian làm việc kết hợp sự đơn giản của giấy với sức mạnh của phần mềm hiện đại.
          Tạo đề, giám sát và xem kết quả thi trong một nền tảng liền mạch.
        </p>

        <!-- CTA Buttons -->
        <div class="lp__hero-cta animate-on-scroll" data-animate="cta">
          <RouterLink :to="primaryCta.to" class="lp__btn lp__btn--primary lp__btn--lg lp__btn--magnetic">
            <span>{{ primaryCta.label }}</span>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
            <span class="lp__btn-ripple"></span>
          </RouterLink>
          <RouterLink to="/help-center" class="lp__btn lp__btn--outline lp__btn--lg">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3M12 17h.01"/></svg>
            Tìm hiểu thêm
          </RouterLink>
        </div>

        <!-- Trust badges -->
        <div class="lp__hero-trust animate-on-scroll" data-animate="trust">
          <span class="lp__hero-trust-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            Miễn phí dùng thử
          </span>
          <span class="lp__hero-trust-sep"></span>
          <span class="lp__hero-trust-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            Bảo mật cao cấp
          </span>
          <span class="lp__hero-trust-sep"></span>
          <span class="lp__hero-trust-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
            Hỗ trợ 24/7
          </span>
        </div>
      </div>

      <!-- Hero Dashboard Preview -->
      <div class="lp__hero-preview animate-on-scroll" data-animate="preview">
        <div class="lp__preview-card lp__preview-card--float">
          <div class="lp__preview-header">
            <div class="lp__preview-dots">
              <span></span><span></span><span></span>
            </div>
            <span class="lp__preview-title">Dashboard Giám sát</span>
            <span class="lp__preview-status">
              <span class="lp__preview-status-dot"></span>
              Trực tuyến
            </span>
          </div>
          <div class="lp__preview-body">
            <div class="lp__preview-exam-info">
              <div>
                <h4>Toán rời rạc - Đề thi giữa kỳ</h4>
                <p>42 học sinh đang thi</p>
              </div>
              <div class="lp__preview-timer">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
                <span>34:18</span>
              </div>
            </div>
            <!-- Live activity feed -->
            <div class="lp__preview-activity">
              <div class="lp__preview-activity-label">
                <span class="lp__pulse"></span>
                Hoạt động realtime
              </div>
              <div class="lp__preview-event lp__preview-event--warning">
                <div class="lp__preview-event-icon">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
                </div>
                <span>Nguyễn An - Tab switch</span>
                <span class="lp__preview-event-time">2 phút</span>
              </div>
            </div>
            <!-- Student progress bars -->
            <div class="lp__preview-students">
              <div v-for="s in previewStudents" :key="s.name" class="lp__preview-student">
                <div class="lp__preview-avatar" :style="{ background: s.gradient }">{{ s.initials }}</div>
                <div class="lp__preview-student-info">
                  <span class="lp__preview-student-name">{{ s.name }}</span>
                  <span class="lp__preview-student-status" :class="s.statusClass">{{ s.status }}</span>
                </div>
                <div class="lp__preview-progress-wrap">
                  <span>{{ s.progress }}%</span>
                  <div class="lp__preview-progress-bar">
                    <div class="lp__preview-progress-fill" :class="s.barClass" :style="{ width: s.progress + '%' }"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Floating notification cards -->
        <div class="lp__float-card lp__float-card--1">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
          <div>
            <span class="lp__float-card-title">+128</span>
            <span class="lp__float-card-desc">Bài thi hôm nay</span>
          </div>
        </div>
        <div class="lp__float-card lp__float-card--2">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/></svg>
          <div>
            <span class="lp__float-card-title">+24</span>
            <span class="lp__float-card-desc">Học sinh online</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ===================== MARQUEE TRUST STRIP ===================== -->
    <section class="lp__marquee-section">
      <div class="lp__marquee">
        <div class="lp__marquee-content">
          <span v-for="i in 4" :key="i">
            <span class="lp__marquee-item">Thi trực tuyến</span>
            <span class="lp__marquee-dot"></span>
            <span class="lp__marquee-item">Chống gian lận</span>
            <span class="lp__marquee-dot"></span>
            <span class="lp__marquee-item">AI chấm điểm</span>
            <span class="lp__marquee-dot"></span>
            <span class="lp__marquee-item">Giám sát realtime</span>
            <span class="lp__marquee-dot"></span>
            <span class="lp__marquee-item">Auto-save</span>
            <span class="lp__marquee-dot"></span>
            <span class="lp__marquee-item">Import CSV/XLSX</span>
            <span class="lp__marquee-dot"></span>
          </span>
        </div>
      </div>
    </section>

    <!-- ===================== STATS SECTION ===================== -->
    <section class="lp__stats">
      <div class="lp__stats-bg" aria-hidden="true"></div>
      <div class="lp__stats-inner">
        <div
          v-for="(stat, i) in stats"
          :key="stat.label"
          class="lp__stat animate-on-scroll"
          :data-animate="'stat-' + i"
        >
          <div class="lp__stat-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" v-html="stat.icon"></svg>
          </div>
          <span class="lp__stat-value">
            <span class="lp__stat-number" :data-target="stat.value">{{ animatedStats[i] || 0 }}</span>{{ stat.suffix }}
          </span>
          <span class="lp__stat-label">{{ stat.label }}</span>
        </div>
      </div>
    </section>

    <!-- ===================== FEATURES SECTION ===================== -->
    <section id="features" class="lp__features">
      <div class="lp__section-header animate-on-scroll" data-animate="section-header">
        <span class="lp__section-eyebrow">Tính năng</span>
        <h2 class="lp__section-title">Thiết kế cho sự xuất sắc</h2>
        <p class="lp__section-subtitle">Mọi chi tiết đều được chăm chút để mang lại trải nghiệm tốt nhất cho giáo viên và học sinh</p>
      </div>

      <div class="lp__features-grid">
        <div
          v-for="(feature, i) in features"
          :key="feature.title"
          class="lp__feature-card lp__feature-card--glass animate-on-scroll"
          :data-animate="'feature-' + i"
          @mousemove="handleCardTilt($event, i)"
          @mouseleave="resetCardTilt(i)"
        >
          <div class="lp__feature-card-glow" aria-hidden="true"></div>
          <div class="lp__feature-card-inner">
            <div class="lp__feature-icon-wrap" :style="{ background: feature.iconBg }">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" v-html="feature.icon"></svg>
            </div>
            <div class="lp__feature-content">
              <h3 class="lp__feature-title">{{ feature.title }}</h3>
              <p class="lp__feature-desc">{{ feature.desc }}</p>
            </div>
            <div class="lp__feature-tags">
              <span v-for="tag in feature.tags" :key="tag" class="lp__feature-tag">{{ tag }}</span>
            </div>
          </div>
          <div class="lp__feature-card-border" aria-hidden="true"></div>
        </div>
      </div>
    </section>

    <!-- ===================== WORKFLOW SECTION ===================== -->
    <section id="workflow" class="lp__workflow">
      <div class="lp__workflow-bg" aria-hidden="true">
        <div class="lp__workflow-orb lp__workflow-orb--1"></div>
        <div class="lp__workflow-orb lp__workflow-orb--2"></div>
      </div>
      <div class="lp__section-header animate-on-scroll" data-animate="section-header">
        <span class="lp__section-eyebrow">Quy trình</span>
        <h2 class="lp__section-title">Từ tạo đề đến chấm điểm</h2>
        <p class="lp__section-subtitle">4 bước đơn giản để vận hành kỳ thi hoàn chỉnh</p>
      </div>

      <div class="lp__workflow-timeline">
        <!-- Animated connector line -->
        <div class="lp__workflow-connector" aria-hidden="true">
          <div class="lp__workflow-connector-fill" ref="timelineFill"></div>
        </div>

        <div
          v-for="(step, i) in workflowSteps"
          :key="step.title"
          class="lp__workflow-step animate-on-scroll"
          :data-animate="'step-' + i"
        >
          <div class="lp__workflow-step-number">
            <span>{{ String(i + 1).padStart(2, '0') }}</span>
          </div>
          <div class="lp__workflow-step-card">
            <div class="lp__workflow-step-icon" :style="{ background: step.iconBg }">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" v-html="step.icon"></svg>
            </div>
            <h3>{{ step.title }}</h3>
            <p>{{ step.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ===================== TESTIMONIALS SECTION ===================== -->
    <section id="testimonials" class="lp__testimonials">
      <div class="lp__section-header animate-on-scroll" data-animate="section-header">
        <span class="lp__section-eyebrow">Đánh giá</span>
        <h2 class="lp__section-title">Giáo viên nói gì về EduExam</h2>
      </div>

      <div class="lp__testimonials-carousel">
        <div class="lp__testimonials-track" ref="testimonialTrack">
          <div
            v-for="(testimonial, i) in testimonials"
            :key="i"
            class="lp__testimonial-card lp__testimonial-card--glass"
            :class="{ 'lp__testimonial-card--active': activeTestimonial === i }"
          >
            <div class="lp__testimonial-quote">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="currentColor"><path d="M11.192 15.757c0-.88-.23-1.618-.69-2.217-.326-.412-.768-.683-1.327-.812-.55-.128-1.07-.137-1.54-.028-.16-.95.1-1.956.76-3.022.66-1.065 1.515-1.867 2.558-2.403L9.373 5c-.8.396-1.56.898-2.26 1.505-.71.607-1.34 1.305-1.9 2.094s-.98 1.68-1.25 2.69-.346 2.04-.217 3.1c.168 1.4.62 2.52 1.356 3.35.735.84 1.652 1.26 2.748 1.26.965 0 1.766-.29 2.4-.878.628-.576.94-1.365.94-2.368l.002.003zm9.124 0c0-.88-.23-1.618-.69-2.217-.326-.42-.768-.69-1.327-.817-.55-.124-1.07-.13-1.54-.022-.16-.94.09-1.95.75-3.02.66-1.06 1.514-1.86 2.557-2.4L18.49 5c-.8.396-1.555.898-2.26 1.505-.708.607-1.34 1.305-1.894 2.094-.556.79-.97 1.68-1.24 2.69-.273 1-.345 2.04-.217 3.1.168 1.4.62 2.52 1.356 3.35.735.84 1.652 1.26 2.748 1.26.965 0 1.766-.29 2.4-.878.628-.576.94-1.365.94-2.368l-.007.006z"/></svg>
            </div>
            <p class="lp__testimonial-text">{{ testimonial.text }}</p>
            <div class="lp__testimonial-author">
              <div class="lp__testimonial-avatar" :style="{ background: testimonial.gradient }">
                {{ testimonial.name.charAt(0) }}
              </div>
              <div>
                <span class="lp__testimonial-name">{{ testimonial.name }}</span>
                <span class="lp__testimonial-role">{{ testimonial.role }}</span>
              </div>
            </div>
            <div class="lp__testimonial-stars">
              <svg v-for="s in 5" :key="s" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
            </div>
          </div>
        </div>

        <!-- Carousel controls -->
        <div class="lp__testimonials-controls">
          <button class="lp__testimonials-btn" @click="prevTestimonial" aria-label="Previous">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
          </button>
          <div class="lp__testimonials-dots">
            <button
              v-for="(_, i) in testimonials"
              :key="i"
              class="lp__testimonials-dot"
              :class="{ 'lp__testimonials-dot--active': activeTestimonial === i }"
              @click="activeTestimonial = i"
              :aria-label="'Testimonial ' + (i + 1)"
            ></button>
          </div>
          <button class="lp__testimonials-btn" @click="nextTestimonial" aria-label="Next">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
          </button>
        </div>
      </div>
    </section>

    <!-- ===================== FAQ SECTION ===================== -->
    <section id="faq" class="lp__faq">
      <div class="lp__section-header animate-on-scroll" data-animate="section-header">
        <span class="lp__section-eyebrow">Câu hỏi thường gặp</span>
        <h2 class="lp__section-title">Bạn có câu hỏi?</h2>
        <p class="lp__section-subtitle">Giải đáp những thắc mắc phổ biến về EduExam</p>
      </div>

      <div class="lp__faq-list">
        <div
          v-for="(item, i) in faqItems"
          :key="i"
          class="lp__faq-item animate-on-scroll"
          :data-animate="'faq-' + i"
        >
          <button
            class="lp__faq-question"
            :class="{ 'lp__faq-question--open': openFaq === i }"
            @click="toggleFaq(i)"
            :aria-expanded="openFaq === i"
          >
            <span>{{ item.question }}</span>
            <svg class="lp__faq-chevron" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 9l6 6 6-6"/></svg>
          </button>
          <div class="lp__faq-answer" :class="{ 'lp__faq-answer--open': openFaq === i }">
            <p>{{ item.answer }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ===================== CTA SECTION ===================== -->
    <section class="lp__cta">
      <div class="lp__cta-bg" aria-hidden="true">
        <div class="lp__cta-orb lp__cta-orb--1"></div>
        <div class="lp__cta-orb lp__cta-orb--2"></div>
        <div class="lp__cta-orb lp__cta-orb--3"></div>
      </div>
      <div class="lp__cta-content animate-on-scroll" data-animate="cta-section">
        <div class="lp__cta-badge">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
          Bắt đầu ngay hôm nay
        </div>
        <h2 class="lp__cta-title">Sẵn sàng bắt đầu?</h2>
        <p class="lp__cta-desc">
          Tham gia cùng hàng trăm giáo viên và học sinh đang sử dụng EduExam mỗi ngày.
          Đăng ký miễn phí và trải nghiệm ngay hôm nay.
        </p>
        <div class="lp__cta-actions">
          <RouterLink :to="primaryCta.to" class="lp__btn lp__btn--light lp__btn--xl lp__btn--magnetic">
            <span>{{ primaryCta.label }}</span>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
            <span class="lp__btn-ripple"></span>
          </RouterLink>
          <RouterLink to="/help-center" class="lp__btn lp__btn--outline-light lp__btn--lg">
            Liên hệ hỗ trợ
          </RouterLink>
        </div>
        <div class="lp__cta-trust">
          <span>Không cần thẻ tín dụng</span>
          <span class="lp__cta-trust-sep"></span>
          <span>Thiết lập trong 2 phút</span>
          <span class="lp__cta-trust-sep"></span>
          <span>Hủy bất kỳ lúc nào</span>
        </div>
      </div>
    </section>

    <!-- ===================== FOOTER ===================== -->
    <footer class="lp__footer">
      <div class="lp__footer-main">
        <div class="lp__footer-brand">
          <a href="/" class="lp__footer-logo">
            <AppLogo size="md" variant="default" tag="span" :show-text="false" />
            <span>EduExam</span>
          </a>
          <p class="lp__footer-tagline">
            Nền tảng thi trực tuyến hàng đầu Việt Nam. Giúp giáo viên và học sinh kết nối, tạo đề thi và giám sát kỳ thi một cách dễ dàng.
          </p>
          <div class="lp__footer-social">
            <a href="#" class="lp__footer-social-link" aria-label="Facebook">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/></svg>
            </a>
            <a href="#" class="lp__footer-social-link" aria-label="Twitter/X">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/></svg>
            </a>
            <a href="#" class="lp__footer-social-link" aria-label="YouTube">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M23.498 6.186a3.016 3.016 0 00-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 00.502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 002.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 002.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z"/></svg>
            </a>
          </div>
        </div>

        <div class="lp__footer-links">
          <div class="lp__footer-col">
            <h4>Sản phẩm</h4>
            <a href="#features">Tính năng</a>
            <a href="#workflow">Quy trình</a>
            <a href="#testimonials">Đánh giá</a>
            <a href="#faq">FAQ</a>
          </div>
          <div class="lp__footer-col">
            <h4>Tài khoản</h4>
            <RouterLink to="/login">Đăng nhập</RouterLink>
            <RouterLink to="/register">Đăng ký</RouterLink>
            <RouterLink to="/help-center">Trung tâm trợ giúp</RouterLink>
          </div>
          <div class="lp__footer-col">
            <h4>Pháp lý</h4>
            <a href="#">Điều khoản sử dụng</a>
            <a href="#">Chính sách bảo mật</a>
            <a href="#">Chính sách cookie</a>
          </div>
        </div>
      </div>

      <div class="lp__footer-bottom">
        <p>© 2026 EduExam. Nền tảng thi trực tuyến hàng đầu Việt Nam.</p>
        <div class="lp__footer-bottom-links">
          <a href="#">Sơ đồ trang</a>
          <a href="#">Liên hệ</a>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useAuthStore } from '../../stores/authStore'
import AppLogo from '../common/AppLogo.vue'

const authStore = useAuthStore()
authStore.syncFromStorage()

const containerRef = ref(null)
const particleCanvas = ref(null)
const timelineFill = ref(null)
const testimonialTrack = ref(null)
const isScrolled = ref(false)
const mobileMenuOpen = ref(false)
const openFaq = ref(null)
const activeTestimonial = ref(0)
const animatedStats = reactive([0, 0, 0, 0])
const cardTilts = reactive({})
const counterAnimated = reactive({})
let testimonialInterval = null
let particleAnimationId = null

const primaryCta = computed(() =>
  authStore.isAuthenticated
    ? { to: authStore.dashboardPath || '/select-role', label: 'Bắt Đầu Miên Phí' }
    : { to: '/register', label: 'Bắt đầu miễn phí' }
)

const previewStudents = [
  { name: 'Nguyễn An', initials: 'NA', gradient: 'linear-gradient(135deg, #4f46e5, #6366f1)', status: 'Ổn định', statusClass: 'lp__preview-student-status--good', progress: 68, barClass: '' },
  { name: 'Trần Vy', initials: 'TV', gradient: 'linear-gradient(135deg, #f59e0b, #d97706)', status: 'Tab switch', statusClass: 'lp__preview-student-status--warning', progress: 45, barClass: 'lp__preview-progress-fill--warn' },
  { name: 'Lê Gia Huy', initials: 'LGH', gradient: 'linear-gradient(135deg, #10b981, #059669)', status: 'Ổn định', statusClass: 'lp__preview-student-status--good', progress: 92, barClass: '' },
]

const stats = [
  { value: 500, suffix: '+', label: 'Phiên thi đồng thời', icon: '<path d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>' },
  { value: 10, suffix: 'K+', label: 'Học sinh sử dụng', icon: '<path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/>' },
  { value: 99.9, suffix: '%', label: 'Uptime hệ thống', icon: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>' },
  { value: 3, suffix: '', label: 'Luồng người dùng', icon: '<rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/>' },
]

const features = [
  {
    title: 'Tạo đề thi linh hoạt',
    desc: 'Tạo đề thi thủ công hoặc nhập từ file CSV, XLSX với bản xem trước trực quan trước khi xác nhận.',
    tags: ['Nhiều loại câu hỏi', 'Import file', 'Preview trực quan'],
    icon: '<path d="M12 20h9M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/>',
    iconBg: 'linear-gradient(135deg, rgba(79,70,229,0.15), rgba(99,102,241,0.1))'
  },
  {
    title: 'Giám sát thời gian thực',
    desc: 'Theo dõi hành vi học sinh trong suốt phiên thi với hệ thống cảnh báo thông minh và điểm rủi ro trực quan.',
    tags: ['Risk score', 'Cảnh báo realtime'],
    icon: '<circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>',
    iconBg: 'linear-gradient(135deg, rgba(14,165,233,0.15), rgba(59,130,246,0.1))'
  },
  {
    title: 'Chống gian lận đa lớp',
    desc: 'Phát hiện tab switching, chuyển cửa sổ và các hành vi bất thường khác một cách tự động.',
    tags: ['Tab detection', 'Face detection'],
    icon: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>',
    iconBg: 'linear-gradient(135deg, rgba(22,163,74,0.15), rgba(34,197,94,0.1))'
  },
  {
    title: 'Auto-save thông minh',
    desc: 'Lưu bài thi định kỳ, đảm bảo không mất dữ liệu ngay cả khi gặp sự cố kỹ thuật.',
    tags: ['Định kỳ', 'Manual save'],
    icon: '<path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>',
    iconBg: 'linear-gradient(135deg, rgba(234,179,8,0.15), rgba(245,158,11,0.1))'
  },
  {
    title: 'Giao diện tập trung',
    desc: 'Màn hình làm bài thiết kế tối giản, loại bỏ mọi yếu tố gây phân tâm, giúp học sinh tập trung hoàn toàn.',
    tags: ['Fullscreen', 'Minimal UI'],
    icon: '<rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/>',
    iconBg: 'linear-gradient(135deg, rgba(168,85,247,0.15), rgba(139,92,246,0.1))'
  },
  {
    title: 'AI hỗ trợ chấm điểm',
    desc: 'Sử dụng AI để hỗ trợ chấm điểm tự động các câu hỏi trắc nghiệm và tự luận nhanh chóng.',
    tags: ['Auto grading', 'AI assist'],
    icon: '<path d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>',
    iconBg: 'linear-gradient(135deg, rgba(236,72,153,0.15), rgba(244,114,182,0.1))'
  },
]

const workflowSteps = [
  {
    title: 'Tạo kỳ thi',
    desc: 'Giáo viên tạo đề thi với nhiều loại câu hỏi hoặc import từ file có sẵn',
    icon: '<path d="M12 20h9M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/>',
    iconBg: 'linear-gradient(135deg, #4f46e5, #6366f1)'
  },
  {
    title: 'Học sinh làm bài',
    desc: 'Giao diện thi tập trung với timer, đánh dấu câu và lưu tự động',
    icon: '<path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="8.5" cy="7" r="4"/><polyline points="17 11 19 13 23 9"/>',
    iconBg: 'linear-gradient(135deg, #14b8a6, #2dd4bf)'
  },
  {
    title: 'Giám sát realtime',
    desc: 'Theo dõi toàn bộ phiên thi và phát hiện gian lận tự động',
    icon: '<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>',
    iconBg: 'linear-gradient(135deg, #f59e0b, #fbbf24)'
  },
  {
    title: 'Xem kết quả',
    desc: 'Báo cáo chi tiết điểm số, thời gian và các sự cố trong phiên thi',
    icon: '<path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>',
    iconBg: 'linear-gradient(135deg, #10b981, #34d399)'
  },
]

const testimonials = [
  {
    text: 'EduExam đã thay đổi hoàn toàn cách tôi tổ chức kỳ thi. Trước đây mất hàng giờ để chấm bài, bây giờ chỉ cần vài phút với hệ thống chấm tự động. Giao diện rất trực quan và dễ sử dụng.',
    name: 'Nguyễn Thị Lan',
    role: 'Giáo viên Toán, THPT Chu Văn An',
    gradient: 'linear-gradient(135deg, #4f46e5, #6366f1)'
  },
  {
    text: 'Tính năng giám sát realtime giúp tôi yên tâm hơn rất nhiều. Hệ thống phát hiện gian lận rất chính xác và thông báo kịp thời. Đây là công cụ không thể thiếu trong công tác giảng dạy của tôi.',
    name: 'Trần Đình Minh',
    role: 'Trưởng bộ môn Tin học, THPT Nguyễn Huệ',
    gradient: 'linear-gradient(135deg, #14b8a6, #2dd4bf)'
  },
  {
    text: 'Con tôi rất thích giao diện làm bài thi. Timer trực quan, có thể đánh dấu câu hỏi và không lo mất bài. Điểm số được công bố ngay sau khi nộp bài, rất minh bạch.',
    name: 'Phạm Thị Hương',
    role: 'Phụ huynh học sinh lớp 12',
    gradient: 'linear-gradient(135deg, #f59e0b, #fbbf24)'
  },
]

const faqItems = [
  {
    question: 'EduExam có miễn phí không?',
    answer: 'EduExam cung cấp gói miễn phí với đầy đủ tính năng cơ bản cho tối đa 50 học sinh. Các gói trả phí với nhiều tính năng nâng cao hơn bắt đầu từ 199.000đ/tháng.'
  },
  {
    question: 'Làm sao để bắt đầu tạo kỳ thi?',
    answer: 'Sau khi đăng ký và đăng nhập, bạn vào mục "Tạo đề thi" từ dashboard. Có thể tạo đề thủ công hoặc import từ file CSV/XLSX. Hệ thống hỗ trợ nhiều loại câu hỏi: trắc nghiệm, tự luận, đúng/sai.'
  },
  {
    question: 'Hệ thống chống gian lận hoạt động như thế nào?',
    answer: 'EduExam sử dụng nhiều lớp bảo mật: theo dõi tab switching, phát hiện chuyển cửa sổ, đo thời gian làm bài bất thường. Giáo viên có thể xem báo cáo chi tiết về các hành vi đáng nghi ngờ và xem lại lịch sử hoạt động của từng học sinh.'
  },
  {
    question: 'Dữ liệu bài thi có bị mất không?',
    answer: 'Không. Hệ thống auto-save định kỳ mỗi 30 giây, đồng thời học sinh có thể lưu bài thủ công bất kỳ lúc nào. Ngay cả khi mất kết nối internet, bài thi vẫn được bảo toàn và đồng bộ khi có mạng trở lại.'
  },
  {
    question: 'Có thể sử dụng EduExam trên điện thoại không?',
    answer: 'EduExam được tối ưu hóa cho cả máy tính và thiết bị di động. Giao diện làm bài thi được thiết kế responsive, tuy nhiên để có trải nghiệm tốt nhất, chúng tôi khuyến khích sử dụng trên máy tính với màn hình lớn hơn 10 inch.'
  },
]

// Scroll handler — throttled để tránh lag
let scrollRafId = null
const handleScroll = () => {
  if (scrollRafId) return
  scrollRafId = requestAnimationFrame(() => {
    isScrolled.value = window.scrollY > 20
    scrollRafId = null
  })
}

// Intersection Observer for animations
const setupScrollAnimations = () => {
  if (typeof IntersectionObserver === 'undefined') return

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const el = entry.target
        const animType = el.dataset.animate

        el.classList.add('is-visible')

        if (animType?.startsWith('stat-')) {
          const idx = parseInt(animType.replace('stat-', ''))
          animateCounter(idx)
        }

        observer.unobserve(el)
      }
    })
  }, {
    root: null,
    rootMargin: '0px 0px -60px 0px',
    threshold: 0.08
  })

  document.querySelectorAll('.animate-on-scroll').forEach(el => {
    observer.observe(el)
  })
}

// Counter animation — runs once
const animateCounter = (index) => {
  if (counterAnimated[index]) return
  counterAnimated[index] = true

  const target = stats[index].value
  const duration = 1800
  const startTime = performance.now()

  const tick = (now) => {
    const elapsed = now - startTime
    const t = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - t, 3)
    animatedStats[index] = parseFloat((target * eased).toFixed(target % 1 !== 0 ? 1 : 0))
    if (t < 1) requestAnimationFrame(tick)
    else animatedStats[index] = target
  }

  requestAnimationFrame(tick)
}

// FAQ toggle
const toggleFaq = (index) => {
  openFaq.value = openFaq.value === index ? null : index
}

// Testimonial carousel
const nextTestimonial = () => {
  activeTestimonial.value = (activeTestimonial.value + 1) % testimonials.length
}

const prevTestimonial = () => {
  activeTestimonial.value = (activeTestimonial.value - 1 + testimonials.length) % testimonials.length
}

// Card tilt effect — throttled with rAF
const handleCardTilt = (event, index) => {
  if (cardTilts[index]) return
  const card = event.currentTarget
  if (!card) return
  cardTilts[index] = requestAnimationFrame(() => {
    if (!card || !card.getBoundingClientRect) return
    const rect = card.getBoundingClientRect()
    const x = event.clientX - rect.left
    const y = event.clientY - rect.top
    const rx = ((y / rect.height) - 0.5) * -6
    const ry = ((x / rect.width) - 0.5) * 6

    card.style.transform = `perspective(1000px) rotateX(${rx}deg) rotateY(${ry}deg) scale3d(1.02, 1.02, 1.02)`
    cardTilts[index] = null
  })
}

const resetCardTilt = (index) => {
  if (cardTilts[index]) {
    cancelAnimationFrame(cardTilts[index])
    cardTilts[index] = null
  }
}

// Particle animation — optimized: lazy init, throttled resize, reduced count
const initParticles = () => {
  const canvas = particleCanvas.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  let particles = []
  let width = canvas.width = window.innerWidth
  let height = canvas.height = window.innerHeight
  let animId = null
  let resizeTimer = null

  class Particle {
    constructor() { this.reset(true) }

    reset(randomY = false) {
      this.x = Math.random() * width
      this.y = randomY ? Math.random() * height : -5
      this.size = Math.random() * 1.8 + 0.3
      this.speedX = (Math.random() - 0.5) * 0.25
      this.speedY = Math.random() * 0.3 + 0.05
      this.opacity = Math.random() * 0.4 + 0.05
    }

    update() {
      this.x += this.speedX
      this.y += this.speedY
      if (this.y > height + 5 || this.x < -5 || this.x > width + 5) this.reset()
    }

    draw() {
      ctx.beginPath()
      ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(99, 102, 241, ${this.opacity})`
      ctx.fill()
    }
  }

  // Reduced particle count for performance
  for (let i = 0; i < 45; i++) particles.push(new Particle())

  let lastTime = 0
  const targetFPS = 30
  const interval = 1000 / targetFPS

  const animate = (timestamp) => {
    if (timestamp - lastTime >= interval) {
      lastTime = timestamp
      ctx.clearRect(0, 0, width, height)

      // Draw particles
      for (let i = 0; i < particles.length; i++) {
        particles[i].update()
        particles[i].draw()
      }

      // Draw connections — only check nearby particles using spatial grid
      for (let i = 0; i < particles.length; i += 3) {
        const p1 = particles[i]
        for (let j = i + 1; j < Math.min(i + 8, particles.length); j++) {
          const p2 = particles[j]
          const dx = p1.x - p2.x
          const dy = p1.y - p2.y
          const distSq = dx * dx + dy * dy
          if (distSq < 14400) { // 120^2
            const dist = Math.sqrt(distSq)
            const alpha = (1 - dist / 120) * 0.08
            ctx.beginPath()
            ctx.moveTo(p1.x, p1.y)
            ctx.lineTo(p2.x, p2.y)
            ctx.strokeStyle = `rgba(99, 102, 241, ${alpha})`
            ctx.lineWidth = 0.4
            ctx.stroke()
          }
        }
      }
    }

    animId = requestAnimationFrame(animate)
  }

  particleAnimationId = animId

  const resize = () => {
    clearTimeout(resizeTimer)
    resizeTimer = setTimeout(() => {
      width = canvas.width = window.innerWidth
      height = canvas.height = window.innerHeight
    }, 200)
  }

  animate(0)
  window.addEventListener('resize', resize, { passive: true })
}

// Ripple effect for buttons
const initRipple = () => {
  document.querySelectorAll('.lp__btn--magnetic').forEach(btn => {
    btn.addEventListener('click', function(e) {
      const rect = this.getBoundingClientRect()
      const ripple = document.createElement('span')
      ripple.className = 'lp__btn-ripple'
      ripple.style.cssText = `
        position: absolute;
        border-radius: 50%;
        background: rgba(255,255,255,0.3);
        transform: scale(0);
        animation: ripple 0.6s ease-out;
        pointer-events: none;
        left: ${e.clientX - rect.left}px;
        top: ${e.clientY - rect.top}px;
        width: 100px;
        height: 100px;
      `
      this.appendChild(ripple)
      setTimeout(() => ripple.remove(), 600)
    })
  })
}

// Smooth scroll for anchor links
const initSmoothScroll = () => {
  document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function(e) {
      e.preventDefault()
      const target = document.querySelector(this.getAttribute('href'))
      if (target) {
        target.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
    })
  })
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  setupScrollAnimations()
  initParticles()
  initSmoothScroll()
  initRipple()

  // Auto-rotate testimonials
  testimonialInterval = setInterval(nextTestimonial, 5000)

  // Add ripple keyframes if not exists
  if (!document.getElementById('lp-ripple-style')) {
    const style = document.createElement('style')
    style.id = 'lp-ripple-style'
    style.textContent = `
      @keyframes ripple {
        to { transform: scale(4); opacity: 0; }
      }
    `
    document.head.appendChild(style)
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  if (particleAnimationId) cancelAnimationFrame(particleAnimationId)
  if (testimonialInterval) clearInterval(testimonialInterval)
})
</script>

<style scoped>
/* ============================================
   EDUEXAM LANDING PAGE v2
   Modern Animated Landing Page
   ============================================ */

/* Import fonts */
@import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@300;400;500;600;700;800&family=Manrope:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500;600&display=swap');

/* ============================================
   CSS VARIABLES
   ============================================ */
.lp {
  --lp-primary: var(--ds-primary, #4f46e5);
  --lp-primary-hover: var(--ds-primary-hover, #4338ca);
  --lp-primary-soft: rgba(79, 70, 229, 0.08);
  --lp-primary-ring: rgba(79, 70, 229, 0.14);
  --lp-bg: var(--ds-bg, #f8fafc);
  --lp-surface: var(--ds-surface, #ffffff);
  --lp-text: var(--ds-text, #0f172a);
  --lp-text-secondary: var(--ds-text-secondary, #64748b);
  --lp-border: var(--ds-border, #e2e8f0);
  --lp-success: var(--ds-success, #16a34a);
  --lp-warning: var(--ds-warning, #d97706);
  --lp-danger: var(--ds-danger, #dc2626);
  --lp-info: var(--ds-info, #0284c7);

  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, sans-serif;
  background: var(--lp-bg);
  color: var(--lp-text);
  line-height: 1.6;
  overflow-x: hidden;
  position: relative;
}

/* ============================================
   PARTICLE CANVAS
   ============================================ */
.lp__particles {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  contain: strict;
}

/* ============================================
   SCROLL ANIMATIONS
   ============================================ */
.animate-on-scroll {
  opacity: 0;
  transform: translateY(40px);
  transition: opacity 0.8s cubic-bezier(0.4, 0, 0.2, 1),
              transform 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.animate-on-scroll.is-visible {
  opacity: 1;
  transform: translateY(0);
}

[data-animate="badge"] { transition-delay: 0.1s; }
[data-animate="title"] { transition-delay: 0.2s; }
[data-animate="desc"] { transition-delay: 0.35s; }
[data-animate="cta"] { transition-delay: 0.5s; }
[data-animate="trust"] { transition-delay: 0.65s; }
[data-animate="preview"] { transition-delay: 0.4s; transform: translateX(60px); }
[data-animate="preview"].is-visible { transform: translateX(0); }
[data-animate="section-header"] { transform: translateY(30px); }

/* ============================================
   NAVIGATION
   ============================================ */
.lp__nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  padding: 1rem 2rem;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.lp__nav--scrolled {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(24px) saturate(200%);
  -webkit-backdrop-filter: blur(24px) saturate(200%);
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.08),
    0 1px 4px rgba(0, 0, 0, 0.04);
  padding: 0;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  border-radius: 0;
}

/* Navbar card wrapper — creates the floating card look */
.lp__nav--scrolled .lp__nav-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 2rem;
  background: white;
  border-radius: 0;
  box-shadow:
    0 1px 0 rgba(0, 0, 0, 0.04),
    0 8px 32px rgba(0, 0, 0, 0.06);
}

.lp__nav--scrolled .lp__nav-brand {
  color: var(--lp-text);
}

.lp__nav--scrolled .lp__nav-link {
  color: var(--lp-text-secondary);
}

.lp__nav--scrolled .lp__nav-link:hover {
  color: var(--lp-primary);
  background: var(--lp-primary-soft);
}

.lp__nav--scrolled .lp__nav-login {
  color: var(--lp-text-secondary);
}

.lp__nav--scrolled .lp__nav-login:hover {
  color: var(--lp-primary);
  background: var(--lp-primary-soft);
}

.lp__nav-inner {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 2rem;
}

.lp__nav-logo {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  text-decoration: none;
}

.lp__nav-brand {
  font-family: 'Manrope', sans-serif;
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--lp-text);
  letter-spacing: -0.02em;
}

.lp__nav-links {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  margin-left: auto;
}

.lp__nav-link {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--lp-text-secondary);
  text-decoration: none;
  padding: 0.5rem 0.875rem;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.lp__nav-link:hover {
  color: var(--lp-primary);
  background: var(--lp-primary-soft);
}

.lp__nav-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.lp__nav-login {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--lp-text-secondary);
  text-decoration: none;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.lp__nav-login:hover {
  color: var(--lp-primary);
  background: var(--lp-primary-soft);
}

.lp__nav-cta {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: white;
  background: var(--lp-primary);
  padding: 0.625rem 1.25rem;
  border-radius: 10px;
  text-decoration: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.3);
}

.lp__nav--scrolled .lp__nav-cta {
  background: var(--lp-primary);
  color: white;
}

.lp__nav--scrolled .lp__nav-cta:hover {
  background: var(--lp-primary-hover);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.4);
}

.lp__nav-mobile-toggle {
  display: none;
  flex-direction: column;
  gap: 5px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.5rem;
  margin-left: auto;
}

.lp__nav-mobile-toggle span {
  width: 24px;
  height: 2px;
  background: var(--lp-text);
  border-radius: 2px;
  transition: all 0.3s ease;
}

.lp__mobile-menu {
  display: none;
  flex-direction: column;
  gap: 0.5rem;
  padding: 1rem;
  background: var(--lp-surface);
  border-top: 1px solid var(--lp-border);
}

.lp__mobile-menu--open {
  display: flex;
}

.lp__mobile-link {
  font-size: 1rem;
  font-weight: 500;
  color: var(--lp-text);
  text-decoration: none;
  padding: 0.75rem 1rem;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.lp__mobile-link:hover {
  background: var(--lp-primary-soft);
  color: var(--lp-primary);
}

.lp__mobile-cta {
  display: block;
  text-align: center;
  font-size: 1rem;
  font-weight: 600;
  color: white;
  background: var(--lp-primary);
  padding: 0.875rem 1.5rem;
  border-radius: 10px;
  text-decoration: none;
  margin-top: 0.5rem;
}

/* ============================================
   BUTTONS
   ============================================ */
.lp__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.625rem;
  font-family: 'Be Vietnam Pro', sans-serif;
  font-weight: 600;
  padding: 0.75rem 1.5rem;
  border-radius: 12px;
  text-decoration: none;
  border: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.lp__btn--primary {
  background: var(--lp-primary);
  color: white;
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.3);
}

.lp__btn--primary:hover {
  background: var(--lp-primary-hover);
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.4);
}

.lp__btn--outline {
  background: transparent;
  color: var(--lp-text);
  border: 1.5px solid var(--lp-border);
}

.lp__btn--outline:hover {
  border-color: var(--lp-primary);
  color: var(--lp-primary);
  background: var(--lp-primary-soft);
}

.lp__btn--lg {
  padding: 0.875rem 2rem;
  font-size: 1rem;
}

.lp__btn--xl {
  padding: 1rem 2.5rem;
  font-size: 1.0625rem;
}

.lp__btn--light {
  background: white;
  color: var(--lp-primary);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.lp__btn--light:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
}

.lp__btn--outline-light {
  background: transparent;
  color: white;
  border: 1.5px solid rgba(255, 255, 255, 0.3);
}

.lp__btn--outline-light:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.5);
}

.lp__btn-ripple {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  transform: scale(0);
  animation: ripple 0.6s ease-out;
  pointer-events: none;
}

/* ============================================
   HERO SECTION
   ============================================ */
.lp__hero {
  position: relative;
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1fr;
  align-items: center;
  gap: 4rem;
  padding: 8rem 4rem 5rem;
  max-width: 1400px;
  margin: 0 auto;
  overflow: hidden;
  z-index: 1;
}

/* Animated orbs */
.lp__hero-orbs {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.lp__orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.5;
  animation: orbFloat 25s ease-in-out infinite;
  will-change: transform, opacity;
  transform: translateZ(0);
}

.lp__orb--1 {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(79, 70, 229, 0.15) 0%, transparent 70%);
  top: -200px;
  right: -100px;
  animation-delay: 0s;
}

.lp__orb--2 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.12) 0%, transparent 70%);
  bottom: -100px;
  left: -50px;
  animation-delay: -8s;
}

.lp__orb--3 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.1) 0%, transparent 70%);
  top: 40%;
  left: 40%;
  animation-delay: -16s;
}

@keyframes orbFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -30px) scale(1.05); }
  50% { transform: translate(-20px, 20px) scale(0.95); }
  75% { transform: translate(-30px, -10px) scale(1.02); }
}

/* Grid pattern */
.lp__hero-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(79, 70, 229, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(79, 70, 229, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(ellipse at center, black 30%, transparent 70%);
  pointer-events: none;
}

.lp__hero-content {
  position: relative;
  z-index: 2;
  max-width: 600px;
}

/* Badge */
.lp__hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--lp-primary);
  background: var(--lp-primary-soft);
  padding: 0.5rem 1rem;
  border-radius: 50px;
  margin-bottom: 1.5rem;
  border: 1px solid rgba(79, 70, 229, 0.1);
}

.lp__hero-badge-dot {
  width: 8px;
  height: 8px;
  background: var(--lp-primary);
  border-radius: 50%;
  animation: badgePulse 2s infinite;
}

@keyframes badgePulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.2); }
}

/* Title */
.lp__hero-title {
  margin-bottom: 1.5rem;
}

.lp__hero-title-main {
  display: flex;
  flex-direction: column;
}

.lp__hero-title-brand {
  display: block;
  font-family: 'Manrope', sans-serif;
  font-size: clamp(3rem, 6vw, 4.5rem);
  font-weight: 800;
  color: var(--lp-text);
  letter-spacing: -0.03em;
  line-height: 1;
  background: linear-gradient(135deg, var(--lp-text) 0%, var(--lp-primary) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.lp__hero-title-sub {
  display: block;
  font-size: clamp(1.5rem, 3vw, 2rem);
  font-weight: 500;
  color: var(--lp-text-secondary);
  letter-spacing: -0.01em;
  margin-top: 0.5rem;
}

/* Description */
.lp__hero-desc {
  font-size: 1.125rem;
  color: var(--lp-text-secondary);
  line-height: 1.8;
  margin-bottom: 2rem;
  max-width: 520px;
}

/* CTA */
.lp__hero-cta {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 2rem;
}

/* Trust badges */
.lp__hero-trust {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.lp__hero-trust-item {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--lp-text-secondary);
}

.lp__hero-trust-item svg {
  color: var(--lp-success);
}

.lp__hero-trust-sep {
  width: 4px;
  height: 4px;
  background: var(--lp-border);
  border-radius: 50%;
}

/* ============================================
   HERO PREVIEW
   ============================================ */
.lp__hero-preview {
  position: relative;
  z-index: 2;
}

.lp__preview-card {
  background: var(--lp-surface);
  border-radius: 20px;
  border: 1px solid var(--lp-border);
  overflow: hidden;
  box-shadow:
    0 25px 50px -12px rgba(0, 0, 0, 0.12),
    0 0 0 1px rgba(0, 0, 0, 0.04);
  transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.5s ease;
}

.lp__preview-card--float {
  animation: cardFloat 7s ease-in-out infinite;
  will-change: transform;
  transform: translateZ(0);
}

@keyframes cardFloat {
  0%, 100% { transform: translateY(0) rotate(0deg) translateZ(0); }
  50% { transform: translateY(-10px) rotate(0.3deg) translateZ(0); }
}

.lp__preview-card:hover {
  box-shadow:
    0 35px 60px -15px rgba(79, 70, 229, 0.2),
    0 0 0 1px rgba(79, 70, 229, 0.1);
}

.lp__preview-header {
  background: linear-gradient(135deg, #1e293b, #334155);
  padding: 1rem 1.25rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.lp__preview-dots {
  display: flex;
  gap: 0.5rem;
}

.lp__preview-dots span {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
}

.lp__preview-dots span:nth-child(1) { background: #ef4444; }
.lp__preview-dots span:nth-child(2) { background: #f59e0b; }
.lp__preview-dots span:nth-child(3) { background: #22c55e; }

.lp__preview-title {
  flex: 1;
  font-size: 0.8125rem;
  font-weight: 500;
  color: white;
  text-align: center;
}

.lp__preview-status {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.6875rem;
  font-weight: 600;
  color: #4ade80;
  background: rgba(74, 222, 128, 0.15);
  padding: 0.25rem 0.625rem;
  border-radius: 20px;
}

.lp__preview-status-dot {
  width: 6px;
  height: 6px;
  background: #4ade80;
  border-radius: 50%;
  animation: statusPulse 2s infinite;
}

@keyframes statusPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.lp__preview-body {
  padding: 1.25rem;
}

.lp__preview-exam-info {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--lp-border);
}

.lp__preview-exam-info h4 {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--lp-text);
  margin-bottom: 0.25rem;
}

.lp__preview-exam-info p {
  font-size: 0.75rem;
  color: var(--lp-text-secondary);
}

.lp__preview-timer {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-family: 'JetBrains Mono', monospace;
  font-size: 1rem;
  font-weight: 600;
  color: var(--lp-primary);
  background: var(--lp-primary-soft);
  padding: 0.5rem 0.75rem;
  border-radius: 8px;
}

/* Activity feed */
.lp__preview-activity {
  margin-bottom: 1rem;
}

.lp__preview-activity-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.6875rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--lp-text-secondary);
  margin-bottom: 0.5rem;
}

.lp__pulse {
  width: 8px;
  height: 8px;
  background: var(--lp-success);
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.3); opacity: 0.7; }
}

.lp__preview-event {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 0.75rem;
  border-radius: 10px;
  background: rgba(245, 158, 11, 0.06);
  border: 1px solid rgba(245, 158, 11, 0.15);
  animation: slideInEvent 0.5s ease;
}

@keyframes slideInEvent {
  from { opacity: 0; transform: translateX(-10px); }
  to { opacity: 1; transform: translateX(0); }
}

.lp__preview-event-icon {
  width: 28px;
  height: 28px;
  background: rgba(245, 158, 11, 0.15);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #f59e0b;
  flex-shrink: 0;
}

.lp__preview-event span:nth-child(2) {
  flex: 1;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--lp-text);
}

.lp__preview-event-time {
  font-size: 0.6875rem;
  color: var(--lp-text-secondary);
}

/* Students list */
.lp__preview-students {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.lp__preview-student {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 0.75rem;
  background: var(--lp-bg);
  border-radius: 10px;
  transition: all 0.2s ease;
}

.lp__preview-student:hover {
  background: var(--lp-primary-soft);
}

.lp__preview-avatar {
  width: 36px;
  height: 36px;
  color: white;
  font-size: 0.75rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  flex-shrink: 0;
}

.lp__preview-student-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.lp__preview-student-name {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--lp-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.lp__preview-student-status {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.6875rem;
  font-weight: 500;
}

.lp__preview-student-status--good { color: var(--lp-success); }
.lp__preview-student-status--warning { color: var(--lp-warning); }

.lp__preview-progress-wrap {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
}

.lp__preview-progress-wrap span {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--lp-text);
}

.lp__preview-progress-bar {
  width: 56px;
  height: 4px;
  background: var(--lp-border);
  border-radius: 2px;
  overflow: hidden;
}

.lp__preview-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--lp-primary), #6366f1);
  border-radius: 2px;
  transition: width 1.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.lp__preview-progress-fill--warn {
  background: linear-gradient(90deg, #f59e0b, #d97706);
}

/* Floating cards */
.lp__float-card {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1.125rem;
  background: var(--lp-surface);
  border-radius: 14px;
  border: 1px solid var(--lp-border);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);
  animation: floatCard 5s ease-in-out infinite;
}

.lp__float-card svg {
  color: var(--lp-success);
  flex-shrink: 0;
}

.lp__float-card-title {
  display: block;
  font-family: 'Manrope', sans-serif;
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--lp-text);
}

.lp__float-card-desc {
  display: block;
  font-size: 0.75rem;
  color: var(--lp-text-secondary);
}

.lp__float-card--1 {
  top: -20px;
  left: -40px;
  animation-delay: -1s;
}

.lp__float-card--2 {
  bottom: 40px;
  right: -30px;
  animation-delay: -3s;
}

@keyframes floatCard {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

/* ============================================
   MARQUEE SECTION
   ============================================ */
.lp__marquee-section {
  background: var(--lp-surface);
  border-top: 1px solid var(--lp-border);
  border-bottom: 1px solid var(--lp-border);
  padding: 1rem 0;
  overflow: hidden;
  position: relative;
  z-index: 1;
}

.lp__marquee {
  overflow: hidden;
}

.lp__marquee-content {
  display: flex;
  animation: marquee 30s linear infinite;
  width: max-content;
}

.lp__marquee-content span {
  display: flex;
  align-items: center;
}

.lp__marquee-item {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--lp-text);
  padding: 0 2rem;
  white-space: nowrap;
}

.lp__marquee-dot {
  width: 6px;
  height: 6px;
  background: var(--lp-primary);
  border-radius: 50%;
  flex-shrink: 0;
}

@keyframes marquee {
  from { transform: translateX(0); }
  to { transform: translateX(-50%); }
}

/* ============================================
   STATS SECTION
   ============================================ */
.lp__stats {
  position: relative;
  padding: 5rem 4rem;
  background: linear-gradient(135deg, var(--lp-primary) 0%, #6366f1 50%, #8b5cf6 100%);
  overflow: hidden;
  z-index: 1;
}

.lp__stats-bg {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 20% 50%, rgba(255, 255, 255, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 50%, rgba(255, 255, 255, 0.08) 0%, transparent 50%);
}

.lp__stats-inner {
  position: relative;
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 3rem;
}

.lp__stat {
  text-align: center;
  padding: 2rem 1rem;
  transition: transform 0.3s ease;
}

.lp__stat:hover {
  transform: translateY(-4px);
}

.lp__stat-icon {
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin: 0 auto 1rem;
  backdrop-filter: blur(8px);
}

.lp__stat-value {
  display: block;
  font-family: 'Manrope', sans-serif;
  font-size: 3rem;
  font-weight: 800;
  color: white;
  letter-spacing: -0.02em;
  line-height: 1;
  margin-bottom: 0.5rem;
}

.lp__stat-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.8);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

/* ============================================
   SECTION HEADER (shared)
   ============================================ */
.lp__section-header {
  text-align: center;
  max-width: 700px;
  margin: 0 auto 4rem;
}

.lp__section-eyebrow {
  display: inline-block;
  font-size: 0.8125rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.15em;
  color: var(--lp-primary);
  margin-bottom: 0.75rem;
}

.lp__section-title {
  font-family: 'Manrope', sans-serif;
  font-size: clamp(2rem, 4vw, 3rem);
  font-weight: 700;
  color: var(--lp-text);
  margin-bottom: 1rem;
  letter-spacing: -0.02em;
}

.lp__section-subtitle {
  font-size: 1.125rem;
  color: var(--lp-text-secondary);
  line-height: 1.7;
}

/* ============================================
   FEATURES SECTION
   ============================================ */
.lp__features {
  padding: 6rem 4rem;
  background: var(--lp-bg);
  position: relative;
  z-index: 1;
}

.lp__features-grid {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
}

.lp__feature-card {
  position: relative;
  border-radius: 20px;
  background: var(--lp-surface);
  border: 1px solid var(--lp-border);
  padding: 2rem;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: default;
  overflow: hidden;
}

.lp__feature-card-glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 50% 0%, var(--lp-primary-soft) 0%, transparent 60%);
  opacity: 0;
  transition: opacity 0.4s ease;
  pointer-events: none;
}

.lp__feature-card:hover .lp__feature-card-glow {
  opacity: 1;
}

.lp__feature-card:hover {
  transform: translateY(-8px);
  box-shadow:
    0 25px 50px -12px rgba(79, 70, 229, 0.15),
    0 0 0 1px rgba(79, 70, 229, 0.1);
  border-color: rgba(79, 70, 229, 0.2);
}

.lp__feature-card-border {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--lp-primary), #6366f1);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.4s ease;
}

.lp__feature-card:hover .lp__feature-card-border {
  transform: scaleX(1);
}

.lp__feature-card-inner {
  position: relative;
  z-index: 1;
}

.lp__feature-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.25rem;
  transition: all 0.3s ease;
}

.lp__feature-card:hover .lp__feature-icon-wrap {
  transform: scale(1.1);
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.25);
}

.lp__feature-title {
  font-family: 'Manrope', sans-serif;
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--lp-text);
  margin-bottom: 0.5rem;
}

.lp__feature-desc {
  font-size: 0.875rem;
  color: var(--lp-text-secondary);
  line-height: 1.6;
  margin-bottom: 1rem;
}

.lp__feature-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.lp__feature-tag {
  font-size: 0.6875rem;
  font-weight: 600;
  color: var(--lp-primary);
  background: var(--lp-primary-soft);
  padding: 0.25rem 0.625rem;
  border-radius: 6px;
}

/* ============================================
   WORKFLOW SECTION
   ============================================ */
.lp__workflow {
  padding: 6rem 4rem;
  background: var(--lp-surface);
  position: relative;
  overflow: hidden;
  z-index: 1;
}

.lp__workflow-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.lp__workflow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.5;
  will-change: transform;
  transform: translateZ(0);
}

.lp__workflow-orb--1 {
  width: 500px;
  height: 500px;
  background: rgba(79, 70, 229, 0.08);
  top: -200px;
  left: -100px;
}

.lp__workflow-orb--2 {
  width: 400px;
  height: 400px;
  background: rgba(139, 92, 246, 0.06);
  bottom: -150px;
  right: -100px;
}

.lp__workflow-timeline {
  position: relative;
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 2rem;
}

.lp__workflow-connector {
  position: absolute;
  top: 44px;
  left: 15%;
  right: 15%;
  height: 3px;
  background: linear-gradient(90deg, var(--lp-border), var(--lp-primary-soft), var(--lp-border));
  border-radius: 2px;
  overflow: hidden;
}

.lp__workflow-connector-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: 0;
  background: linear-gradient(90deg, var(--lp-primary), #6366f1);
  border-radius: 2px;
  transition: width 2s ease 0.5s;
}

.lp__workflow.is-visible .lp__workflow-connector-fill {
  width: 100%;
}

.lp__workflow-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  position: relative;
  transition-delay: calc(var(--i, 0) * 0.15s);
}

.lp__workflow-step:nth-child(2) { --i: 1; }
.lp__workflow-step:nth-child(3) { --i: 2; }
.lp__workflow-step:nth-child(4) { --i: 3; }

.lp__workflow-step-number {
  width: 80px;
  height: 80px;
  background: var(--lp-surface);
  border: 2px solid var(--lp-border);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.5rem;
  position: relative;
  z-index: 2;
  transition: all 0.4s ease;
}

.lp__workflow-step:hover .lp__workflow-step-number {
  border-color: var(--lp-primary);
  background: var(--lp-primary);
  transform: scale(1.1);
}

.lp__workflow-step:hover .lp__workflow-step-number span {
  color: white;
}

.lp__workflow-step-number span {
  font-family: 'Manrope', sans-serif;
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--lp-primary);
  transition: color 0.3s ease;
}

.lp__workflow-step-card {
  background: var(--lp-surface);
  border: 1px solid var(--lp-border);
  border-radius: 16px;
  padding: 1.5rem;
  width: 100%;
  transition: all 0.3s ease;
}

.lp__workflow-step-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 40px rgba(79, 70, 229, 0.1);
  border-color: rgba(79, 70, 229, 0.2);
}

.lp__workflow-step-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
  color: white;
  transition: transform 0.4s ease;
}

.lp__workflow-step-card:hover .lp__workflow-step-icon {
  transform: rotateY(180deg);
}

.lp__workflow-step-card h3 {
  font-family: 'Manrope', sans-serif;
  font-size: 1rem;
  font-weight: 700;
  color: var(--lp-text);
  margin-bottom: 0.5rem;
}

.lp__workflow-step-card p {
  font-size: 0.8125rem;
  color: var(--lp-text-secondary);
  line-height: 1.6;
}

/* ============================================
   TESTIMONIALS SECTION
   ============================================ */
.lp__testimonials {
  padding: 6rem 4rem;
  background: var(--lp-bg);
  position: relative;
  z-index: 1;
}

.lp__testimonials-carousel {
  max-width: 900px;
  margin: 0 auto;
  position: relative;
}

.lp__testimonials-track {
  position: relative;
}

.lp__testimonial-card {
  background: var(--lp-surface);
  border: 1px solid var(--lp-border);
  border-radius: 24px;
  padding: 2.5rem;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0;
  position: absolute;
  inset: 0;
  transform: translateX(40px);
  pointer-events: none;
}

.lp__testimonial-card--active {
  opacity: 1;
  position: relative;
  transform: translateX(0);
  pointer-events: auto;
}

.lp__testimonial-quote {
  color: var(--lp-primary);
  opacity: 0.3;
  margin-bottom: 1rem;
}

.lp__testimonial-text {
  font-size: 1.125rem;
  color: var(--lp-text);
  line-height: 1.8;
  margin-bottom: 1.5rem;
  font-style: italic;
}

.lp__testimonial-author {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
}

.lp__testimonial-avatar {
  width: 48px;
  height: 48px;
  color: white;
  font-size: 1.25rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  flex-shrink: 0;
}

.lp__testimonial-name {
  display: block;
  font-weight: 700;
  color: var(--lp-text);
  font-size: 1rem;
}

.lp__testimonial-role {
  display: block;
  font-size: 0.875rem;
  color: var(--lp-text-secondary);
}

.lp__testimonial-stars {
  display: flex;
  gap: 0.25rem;
  color: #fbbf24;
}

.lp__testimonials-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-top: 2rem;
}

.lp__testimonials-btn {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  border: 1.5px solid var(--lp-border);
  background: var(--lp-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--lp-text);
  cursor: pointer;
  transition: all 0.2s ease;
}

.lp__testimonials-btn:hover {
  border-color: var(--lp-primary);
  color: var(--lp-primary);
  background: var(--lp-primary-soft);
}

.lp__testimonials-dots {
  display: flex;
  gap: 0.5rem;
}

.lp__testimonials-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: none;
  background: var(--lp-border);
  cursor: pointer;
  transition: all 0.3s ease;
}

.lp__testimonials-dot--active {
  background: var(--lp-primary);
  width: 32px;
  border-radius: 5px;
}

/* ============================================
   FAQ SECTION
   ============================================ */
.lp__faq {
  padding: 6rem 4rem;
  background: var(--lp-surface);
  position: relative;
  z-index: 1;
}

.lp__faq-list {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.lp__faq-item {
  background: var(--lp-bg);
  border: 1px solid var(--lp-border);
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.lp__faq-item:hover {
  border-color: rgba(79, 70, 229, 0.2);
}

.lp__faq-question {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.25rem 1.5rem;
  background: none;
  border: none;
  font-family: 'Be Vietnam Pro', sans-serif;
  font-size: 1rem;
  font-weight: 600;
  color: var(--lp-text);
  text-align: left;
  cursor: pointer;
  transition: color 0.2s ease;
}

.lp__faq-question:hover {
  color: var(--lp-primary);
}

.lp__faq-chevron {
  flex-shrink: 0;
  color: var(--lp-text-secondary);
  transition: transform 0.3s ease, color 0.2s ease;
}

.lp__faq-question--open {
  color: var(--lp-primary);
}

.lp__faq-question--open .lp__faq-chevron {
  transform: rotate(180deg);
  color: var(--lp-primary);
}

.lp__faq-answer {
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.4s cubic-bezier(0.4, 0, 0.2, 1), padding 0.3s ease;
}

.lp__faq-answer--open {
  max-height: 300px;
  padding: 0 1.5rem 1.25rem;
}

.lp__faq-answer p {
  font-size: 0.9375rem;
  color: var(--lp-text-secondary);
  line-height: 1.7;
  margin: 0;
}

/* ============================================
   CTA SECTION
   ============================================ */
.lp__cta {
  position: relative;
  padding: 8rem 4rem;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  overflow: hidden;
  z-index: 1;
}

.lp__cta-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.lp__cta-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  will-change: transform;
  transform: translateZ(0);
}

.lp__cta-orb--1 {
  width: 600px;
  height: 600px;
  background: rgba(79, 70, 229, 0.3);
  top: -200px;
  right: -100px;
  animation: ctaOrbMove 20s ease-in-out infinite;
}

.lp__cta-orb--2 {
  width: 400px;
  height: 400px;
  background: rgba(139, 92, 246, 0.25);
  bottom: -150px;
  left: -50px;
  animation: ctaOrbMove 15s ease-in-out infinite reverse;
}

.lp__cta-orb--3 {
  width: 300px;
  height: 300px;
  background: rgba(59, 130, 246, 0.2);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: ctaOrbPulse 8s ease-in-out infinite;
}

@keyframes ctaOrbMove {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(40px, -40px); }
}

@keyframes ctaOrbPulse {
  0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 0.3; }
  50% { transform: translate(-50%, -50%) scale(1.2); opacity: 0.5; }
}

.lp__cta-content {
  position: relative;
  text-align: center;
  max-width: 700px;
  margin: 0 auto;
}

.lp__cta-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #a5b4fc;
  background: rgba(79, 70, 229, 0.2);
  padding: 0.5rem 1rem;
  border-radius: 50px;
  margin-bottom: 1.5rem;
  border: 1px solid rgba(79, 70, 229, 0.3);
}

.lp__cta-title {
  font-family: 'Manrope', sans-serif;
  font-size: clamp(2.5rem, 5vw, 3.5rem);
  font-weight: 800;
  color: white;
  margin-bottom: 1.25rem;
  letter-spacing: -0.02em;
}

.lp__cta-desc {
  font-size: 1.125rem;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.7;
  margin-bottom: 2.5rem;
}

.lp__cta-actions {
  display: flex;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 2rem;
}

.lp__cta-trust {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.lp__cta-trust span {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.5);
}

.lp__cta-trust-sep {
  width: 4px;
  height: 4px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 50%;
}

/* ============================================
   FOOTER
   ============================================ */
.lp__footer {
  background: var(--lp-surface);
  border-top: 1px solid var(--lp-border);
  position: relative;
  z-index: 1;
}

.lp__footer-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 4rem 4rem 3rem;
  display: grid;
  grid-template-columns: 1.5fr 2fr;
  gap: 6rem;
}

.lp__footer-logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-decoration: none;
  margin-bottom: 1rem;
}

.lp__footer-logo span {
  font-family: 'Manrope', sans-serif;
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--lp-text);
}

.lp__footer-tagline {
  font-size: 0.9375rem;
  color: var(--lp-text-secondary);
  line-height: 1.7;
  max-width: 320px;
  margin-bottom: 1.5rem;
}

.lp__footer-social {
  display: flex;
  gap: 0.75rem;
}

.lp__footer-social-link {
  width: 40px;
  height: 40px;
  background: var(--lp-bg);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--lp-text-secondary);
  transition: all 0.2s ease;
}

.lp__footer-social-link:hover {
  background: var(--lp-primary);
  color: white;
  transform: translateY(-2px);
}

.lp__footer-links {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 3rem;
}

.lp__footer-col h4 {
  font-family: 'Manrope', sans-serif;
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--lp-text);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 1.25rem;
}

.lp__footer-col a {
  display: block;
  font-size: 0.9375rem;
  color: var(--lp-text-secondary);
  text-decoration: none;
  margin-bottom: 0.625rem;
  transition: color 0.2s ease;
}

.lp__footer-col a:hover {
  color: var(--lp-primary);
}

.lp__footer-bottom {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1.5rem 4rem;
  border-top: 1px solid var(--lp-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
}

.lp__footer-bottom p {
  font-size: 0.875rem;
  color: var(--lp-text-secondary);
}

.lp__footer-bottom-links {
  display: flex;
  gap: 1.5rem;
}

.lp__footer-bottom-links a {
  font-size: 0.875rem;
  color: var(--lp-text-secondary);
  text-decoration: none;
  transition: color 0.2s ease;
}

.lp__footer-bottom-links a:hover {
  color: var(--lp-primary);
}

/* ============================================
   RESPONSIVE
   ============================================ */
@media (max-width: 1200px) {
  .lp__hero {
    padding: 7rem 3rem 4rem;
    gap: 3rem;
  }

  .lp__features-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .lp__stats-inner {
    grid-template-columns: repeat(2, 1fr);
    gap: 2rem;
  }

  .lp__workflow-timeline {
    grid-template-columns: repeat(2, 1fr);
    gap: 3rem;
  }

  .lp__workflow-connector {
    display: none;
  }

  .lp__footer-main {
    gap: 4rem;
    padding: 3rem 3rem 2rem;
  }
}

@media (max-width: 1024px) {
  .lp__hero {
    grid-template-columns: 1fr;
    text-align: center;
    min-height: auto;
    padding: 6rem 2rem 3rem;
  }

  .lp__hero-content {
    max-width: 600px;
    margin: 0 auto;
  }

  .lp__hero-desc {
    max-width: 100%;
  }

  .lp__hero-cta {
    justify-content: center;
  }

  .lp__hero-trust {
    justify-content: center;
  }

  .lp__hero-preview {
    order: -1;
    max-width: 500px;
    margin: 0 auto;
  }

  .lp__float-card--1 { left: 0; }
  .lp__float-card--2 { right: 0; }

  .lp__nav-links,
  .lp__nav-actions {
    display: none;
  }

  .lp__nav-mobile-toggle {
    display: flex;
  }

  .lp__features,
  .lp__workflow,
  .lp__testimonials,
  .lp__faq {
    padding: 4rem 2rem;
  }

  .lp__cta {
    padding: 5rem 2rem;
  }

  .lp__stats {
    padding: 4rem 2rem;
  }

  .lp__footer-main {
    grid-template-columns: 1fr;
    gap: 2rem;
    padding: 3rem 2rem 2rem;
  }

  .lp__footer-links {
    grid-template-columns: repeat(3, 1fr);
    gap: 2rem;
  }

  .lp__footer-bottom {
    padding: 1.5rem 2rem;
    flex-direction: column;
    text-align: center;
  }
}

@media (max-width: 768px) {
  .lp__hero {
    padding: 5rem 1.5rem 3rem;
  }

  .lp__hero-badge {
    font-size: 0.75rem;
  }

  .lp__hero-cta {
    flex-direction: column;
    align-items: center;
  }

  .lp__btn--lg,
  .lp__btn--xl {
    width: 100%;
    max-width: 300px;
  }

  .lp__hero-trust {
    flex-direction: column;
    gap: 0.75rem;
  }

  .lp__hero-trust-sep {
    display: none;
  }

  .lp__float-card {
    display: none;
  }

  .lp__features-grid {
    grid-template-columns: 1fr;
  }

  .lp__stats-inner {
    grid-template-columns: repeat(2, 1fr);
    gap: 1.5rem;
  }

  .lp__stat-value {
    font-size: 2.5rem;
  }

  .lp__workflow-timeline {
    grid-template-columns: 1fr;
    gap: 2rem;
  }

  .lp__footer-links {
    grid-template-columns: 1fr 1fr;
    gap: 2rem;
  }

  .lp__section-title {
    font-size: 2rem;
  }

  .lp__cta-title {
    font-size: 2.5rem;
  }

  .lp__cta-actions {
    flex-direction: column;
    align-items: center;
  }

  .lp__cta-trust {
    flex-direction: column;
    gap: 0.75rem;
  }

  .lp__cta-trust-sep {
    display: none;
  }
}

@media (max-width: 480px) {
  .lp__stats-inner {
    grid-template-columns: 1fr;
  }

  .lp__stat-value {
    font-size: 3rem;
  }

  .lp__footer-links {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }

  .lp__footer-main {
    padding: 2.5rem 1.5rem 2rem;
  }

  .lp__footer-bottom {
    padding: 1.5rem 1.5rem;
  }
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }

  .lp__orb,
  .lp__preview-card--float,
  .lp__float-card,
  .lp__marquee-content,
  .lp__cta-orb {
    animation: none !important;
  }
}
</style>

