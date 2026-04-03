<template>
  <div class="edu-container" ref="container">
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

    <!-- Hero Section -->
    <section class="hero">
      <div class="hero__bg">
        <div class="hero__gradient"></div>
        <div class="hero__shapes">
          <div class="hero__shape hero__shape--1"></div>
          <div class="hero__shape hero__shape--2"></div>
          <div class="hero__shape hero__shape--3"></div>
        </div>
      </div>
      
      <div class="hero__content">
        <div class="hero__badge" data-animate="fade-up">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
          </svg>
          Nền tảng thi trực tuyến hàng đầu
        </div>
        
        <h1 class="hero__title" data-animate="fade-up" data-delay="100">
          <span class="hero__title-line">EduExam</span>
          <span class="hero__title-line hero__title-line--sub">Thi trực tuyến thông minh</span>
        </h1>
        
        <p class="hero__description" data-animate="fade-up" data-delay="200">
          Không gian làm việc kết hợp sự đơn giản của giấy với sức mạnh của phần mềm hiện đại. 
          Tạo đề, giám sát và xem kết quả thi trong một nền tảng liền mạch.
        </p>
        
        <div class="hero__actions" data-animate="fade-up" data-delay="300">
          <RouterLink :to="primaryCta.to" class="edu-btn edu-btn--primary edu-btn--lg">
            {{ primaryCta.label }}
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
          </RouterLink>
          <RouterLink to="/help-center" class="edu-btn edu-btn--outline edu-btn--lg">
            Tìm hiểu thêm
          </RouterLink>
        </div>
      </div>

      <!-- Dashboard Preview -->
      <div class="hero__preview" data-animate="slide-left" data-delay="400">
        <div class="preview-card">
          <div class="preview-card__header">
            <div class="preview-card__dots">
              <span></span><span></span><span></span>
            </div>
            <span class="preview-card__title">Dashboard Giám sát</span>
            <span class="preview-card__status">
              <span class="preview-card__status-dot"></span>
              Trực tuyến
            </span>
          </div>
          <div class="preview-card__body">
            <div class="preview-header">
              <div class="preview-info">
                <h4>Toán rời rạc - Đề thi giữa kỳ</h4>
                <p>42 học sinh đang thi • 45 phút còn lại</p>
              </div>
              <div class="preview-timer">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <path d="M12 6v6l4 2"/>
                </svg>
                <span>34:18</span>
              </div>
            </div>
            
            <div class="preview-alerts">
              <div class="preview-alert preview-alert--warning">
                <div class="preview-alert__icon">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
                    <line x1="12" y1="9" x2="12" y2="13"/>
                    <line x1="12" y1="17" x2="12.01" y2="17"/>
                  </svg>
                </div>
                <div class="preview-alert__content">
                  <span class="preview-alert__title">Tab switch</span>
                  <span class="preview-alert__desc">Nguyễn An vừa chuyển tab</span>
                </div>
                <span class="preview-alert__time">2 phút trước</span>
              </div>
            </div>
            
            <div class="preview-students">
              <div class="preview-student">
                <div class="preview-student__avatar" style="background: linear-gradient(135deg, #4f46e5, #6366f1)">NA</div>
                <div class="preview-student__info">
                  <span class="preview-student__name">Nguyễn An</span>
                  <span class="preview-student__status preview-student__status--good">
                    <svg width="10" height="10" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="12" r="10"/></svg>
                    Ổn định
                  </span>
                </div>
                <div class="preview-student__progress">
                  <span>68%</span>
                  <div class="preview-progress-bar">
                    <div class="preview-progress-fill" style="width: 68%"></div>
                  </div>
                </div>
              </div>
              
              <div class="preview-student">
                <div class="preview-student__avatar" style="background: linear-gradient(135deg, #f59e0b, #d97706)">TV</div>
                <div class="preview-student__info">
                  <span class="preview-student__name">Trần Vy</span>
                  <span class="preview-student__status preview-student__status--warning">
                    <svg width="10" height="10" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="12" r="10"/></svg>
                    Tab switch
                  </span>
                </div>
                <div class="preview-student__progress">
                  <span>45%</span>
                  <div class="preview-progress-bar">
                    <div class="preview-progress-fill preview-progress-fill--warning" style="width: 45%"></div>
                  </div>
                </div>
              </div>
              
              <div class="preview-student">
                <div class="preview-student__avatar" style="background: linear-gradient(135deg, #10b981, #059669)">LGH</div>
                <div class="preview-student__info">
                  <span class="preview-student__name">Lê Gia Huy</span>
                  <span class="preview-student__status preview-student__status--good">
                    <svg width="10" height="10" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="12" r="10"/></svg>
                    Ổn định
                  </span>
                </div>
                <div class="preview-student__progress">
                  <span>92%</span>
                  <div class="preview-progress-bar">
                    <div class="preview-progress-fill" style="width: 92%"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Stats Section -->
    <section class="stats" data-animate="fade-up">
      <div class="stats__grid">
        <div class="stat" data-animate="fade-up" data-delay="0">
          <span class="stat__value">500+</span>
          <span class="stat__label">Phiên thi đồng thời</span>
        </div>
        <div class="stat" data-animate="fade-up" data-delay="100">
          <span class="stat__value">10K+</span>
          <span class="stat__label">Học sinh sử dụng</span>
        </div>
        <div class="stat" data-animate="fade-up" data-delay="200">
          <span class="stat__value">99.9%</span>
          <span class="stat__label">Uptime hệ thống</span>
        </div>
        <div class="stat" data-animate="fade-up" data-delay="300">
          <span class="stat__value">3</span>
          <span class="stat__label">Luồng người dùng</span>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section id="features" class="features">
      <div class="features__header" data-animate="fade-up">
        <span class="features__label">Tính năng</span>
        <h2 class="features__title">Thiết kế cho sự xuất sắc</h2>
        <p class="features__subtitle">Mọi chi tiết đều được chăm chút để mang lại trải nghiệm tốt nhất cho giáo viên và học sinh</p>
      </div>

      <div class="features__grid">
        <div class="feature" data-animate="fade-up" data-delay="0">
          <div class="feature__icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M12 20h9"/>
              <path d="M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/>
            </svg>
          </div>
          <div class="feature__content">
            <h3 class="feature__title">Tạo đề thi linh hoạt</h3>
            <p class="feature__desc">Tạo đề thi thủ công hoặc nhập từ file CSV, XLSX với bản xem trước trực quan trước khi xác nhận.</p>
          </div>
          <div class="feature__tags">
            <span class="feature__tag">Nhiều loại câu hỏi</span>
            <span class="feature__tag">Import file</span>
            <span class="feature__tag">Preview trực quan</span>
          </div>
        </div>

        <div class="feature" data-animate="fade-up" data-delay="100">
          <div class="feature__icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 6v6l4 2"/>
            </svg>
          </div>
          <div class="feature__content">
            <h3 class="feature__title">Giám sát thời gian thực</h3>
            <p class="feature__desc">Theo dõi hành vi học sinh trong suốt phiên thi với hệ thống cảnh báo thông minh và điểm rủi ro trực quan.</p>
          </div>
          <div class="feature__tags">
            <span class="feature__tag">Risk score</span>
            <span class="feature__tag">Cảnh báo realtime</span>
          </div>
        </div>

        <div class="feature" data-animate="fade-up" data-delay="200">
          <div class="feature__icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
          </div>
          <div class="feature__content">
            <h3 class="feature__title">Chống gian lận đa lớp</h3>
            <p class="feature__desc">Phát hiện tab switching, chuyển cửa sổ và các hành vi bất thường khác một cách tự động.</p>
          </div>
          <div class="feature__tags">
            <span class="feature__tag">Tab detection</span>
            <span class="feature__tag">Face detection</span>
          </div>
        </div>

        <div class="feature" data-animate="fade-up" data-delay="300">
          <div class="feature__icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/>
              <line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
          </div>
          <div class="feature__content">
            <h3 class="feature__title">Auto-save thông minh</h3>
            <p class="feature__desc">Lưu bài thi định kỳ, đảm bảo không mất dữ liệu ngay cả khi gặp sự cố kỹ thuật.</p>
          </div>
          <div class="feature__tags">
            <span class="feature__tag">Định kỳ</span>
            <span class="feature__tag">Manual save</span>
          </div>
        </div>

        <div class="feature" data-animate="fade-up" data-delay="0">
          <div class="feature__icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <path d="M3 9h18"/>
              <path d="M9 21V9"/>
            </svg>
          </div>
          <div class="feature__content">
            <h3 class="feature__title">Giao diện tập trung</h3>
            <p class="feature__desc">Màn hình làm bài thiết kế tối giản, loại bỏ mọi yếu tố gây phân tâm, giúp học sinh tập trung hoàn toàn.</p>
          </div>
          <div class="feature__tags">
            <span class="feature__tag">Fullscreen</span>
            <span class="feature__tag">Minimal UI</span>
          </div>
        </div>

        <div class="feature" data-animate="fade-up" data-delay="100">
          <div class="feature__icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
            </svg>
          </div>
          <div class="feature__content">
            <h3 class="feature__title">AI hỗ trợ chấm điểm</h3>
            <p class="feature__desc">Sử dụng AI để hỗ trợ chấm điểm tự động các câu hỏi trắc nghiệm và tự luận nhanh chóng.</p>
          </div>
          <div class="feature__tags">
            <span class="feature__tag">Auto grading</span>
            <span class="feature__tag">AI assist</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Role Section -->
    <section id="roles" class="roles">
      <div class="roles__header" data-animate="fade-up">
        <span class="roles__label">Vai trò</span>
        <h2 class="roles__title">Phù hợp với mọi đối tượng</h2>
      </div>

      <div class="roles__grid">
        <div class="role-card" data-animate="slide-up" data-delay="0">
          <div class="role-card__icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
              <path d="M23 21v-2a4 4 0 00-3-3.87"/>
              <path d="M16 3.13a4 4 0 010 7.75"/>
            </svg>
          </div>
          <h3 class="role-card__title">Học sinh</h3>
          <p class="role-card__desc">Môi trường thi tập trung với giao diện thân thiện, timer đếm ngược và tính năng đánh dấu câu hỏi.</p>
          <ul class="role-card__features">
            <li>Làm bài thi online</li>
            <li>Xem lịch thi</li>
            <li>Xem kết quả chi tiết</li>
            <li>Thi thử với AI</li>
          </ul>
          <RouterLink to="/register" class="role-card__link">
            Đăng ký làm học sinh
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
          </RouterLink>
        </div>

        <div class="role-card role-card--primary" data-animate="slide-up" data-delay="100">
          <div class="role-card__icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M22 10v6M2 10l10-5 10 5-10 5z"/>
              <path d="M6 12v5c3 3 9 3 12 0v-5"/>
            </svg>
          </div>
          <h3 class="role-card__title">Giáo viên</h3>
          <p class="role-card__desc">Công cụ quản lý kỳ thi toàn diện từ tạo đề, giám sát đến xem kết quả và báo cáo chi tiết.</p>
          <ul class="role-card__features">
            <li>Tạo và quản lý đề thi</li>
            <li>Giám sát realtime</li>
            <li>Xem báo cáo chi tiết</li>
            <li>Import đề từ file</li>
          </ul>
          <RouterLink to="/register" class="role-card__link">
            Đăng ký làm giáo viên
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
          </RouterLink>
        </div>

        <div class="role-card" data-animate="slide-up" data-delay="200">
          <div class="role-card__icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M12 15a3 3 0 100-6 3 3 0 000 6z"/>
              <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06a1.65 1.65 0 00.33-1.82 1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06a1.65 1.65 0 001.82.33H9a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/>
            </svg>
          </div>
          <h3 class="role-card__title">Quản trị viên</h3>
          <p class="role-card__desc">Bảng điều khiển quản lý người dùng, kỳ thi và theo dõi hoạt động của toàn hệ thống.</p>
          <ul class="role-card__features">
            <li>Quản lý người dùng</li>
            <li>Quản lý kỳ thi</li>
            <li>Thống kê hệ thống</li>
            <li>Phân quyền</li>
          </ul>
          <RouterLink to="/register" class="role-card__link">
            Đăng ký quản trị
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
          </RouterLink>
        </div>
      </div>
    </section>

    <!-- How It Works -->
    <section id="workflow" class="workflow">
      <div class="workflow__header" data-animate="fade-up">
        <span class="workflow__label">Quy trình</span>
        <h2 class="workflow__title">Từ tạo đề đến chấm điểm</h2>
      </div>

      <div class="workflow__timeline">
        <div class="workflow__line"></div>
        
        <div class="workflow__step" data-animate="fade-up" data-delay="0">
          <div class="workflow__number">01</div>
          <div class="workflow__card">
            <div class="workflow__card-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M12 20h9"/>
                <path d="M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/>
              </svg>
            </div>
            <h3>Tạo kỳ thi</h3>
            <p>Giáo viên tạo đề thi với nhiều loại câu hỏi hoặc import từ file có sẵn</p>
          </div>
        </div>

        <div class="workflow__step" data-animate="fade-up" data-delay="150">
          <div class="workflow__number">02</div>
          <div class="workflow__card">
            <div class="workflow__card-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
                <circle cx="8.5" cy="7" r="4"/>
                <polyline points="17 11 19 13 23 9"/>
              </svg>
            </div>
            <h3>Học sinh làm bài</h3>
            <p>Giao diện thi tập trung với timer, đánh dấu câu và lưu tự động</p>
          </div>
        </div>

        <div class="workflow__step" data-animate="fade-up" data-delay="300">
          <div class="workflow__number">03</div>
          <div class="workflow__card">
            <div class="workflow__card-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
            </div>
            <h3>Giám sát realtime</h3>
            <p>Theo dõi toàn bộ phiên thi và phát hiện gian lận tự động</p>
          </div>
        </div>

        <div class="workflow__step" data-animate="fade-up" data-delay="450">
          <div class="workflow__number">04</div>
          <div class="workflow__card">
            <div class="workflow__card-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M22 11.08V12a10 10 0 11-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
            </div>
            <h3>Xem kết quả</h3>
            <p>Báo cáo chi tiết điểm số, thời gian và các sự cố trong phiên thi</p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="cta" data-animate="fade-up">
      <div class="cta__bg">
        <div class="cta__shape cta__shape--1"></div>
        <div class="cta__shape cta__shape--2"></div>
      </div>
      <div class="cta__content">
        <h2 class="cta__title">Sẵn sàng bắt đầu?</h2>
        <p class="cta__desc">
          Tham gia cùng hàng trăm giáo viên và học sinh đang sử dụng EduExam mỗi ngày
        </p>
        <div class="cta__actions">
          <RouterLink :to="primaryCta.to" class="edu-btn edu-btn--light edu-btn--lg">
            {{ primaryCta.label }}
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
          </RouterLink>
          <RouterLink to="/help-center" class="edu-btn edu-btn--outline-light edu-btn--lg">
            Tìm hiểu thêm
          </RouterLink>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="footer">
      <div class="footer__inner">
        <div class="footer__brand">
          <a href="/" class="footer__logo">
            <AppLogo size="md" variant="default" tag="span" />
          </a>
          <p class="footer__tagline">Nền tảng thi trực tuyến hàng đầu Việt Nam</p>
          <div class="footer__social">
            <a href="#" class="footer__social-link">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M24 4.557c-.883.392-1.832.656-2.828.775 1.017-.609 1.798-1.574 2.165-2.724-.951.564-2.005.974-3.127 1.195-.897-.957-2.178-1.555-3.594-1.555-3.179 0-5.515 2.966-4.797 6.045-4.091-.205-7.719-2.165-10.148-5.144-1.29 2.213-.669 5.108 1.523 6.574-.806-.026-1.566-.247-2.229-.616-.054 2.281 1.581 4.415 3.949 4.89-.693.188-1.452.232-2.224.084.626 1.956 2.444 3.379 4.6 3.419-2.07 1.623-4.678 2.348-7.29 2.04 2.179 1.397 4.768 2.212 7.548 2.212 9.142 0 14.307-7.721 13.995-14.646.962-.695 1.797-1.562 2.457-2.549z"/></svg>
            </a>
            <a href="#" class="footer__social-link">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/></svg>
            </a>
          </div>
        </div>
        
        <div class="footer__links">
          <div class="footer__link-group">
            <h4>Sản phẩm</h4>
            <a href="#features">Tính năng</a>
            <a href="#workflow">Quy trình</a>
            <a href="#roles">Vai trò</a>
            <RouterLink to="/help-center">Trợ giúp</RouterLink>
          </div>
          <div class="footer__link-group">
            <h4>Tài khoản</h4>
            <RouterLink to="/login">Đăng nhập</RouterLink>
            <RouterLink to="/register">Đăng ký</RouterLink>
          </div>
        </div>
      </div>
      <div class="footer__bottom">
        <p>© 2026 EduExam. Nền tảng thi trực tuyến hàng đầu Việt Nam.</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useAuthStore } from '../../stores/authStore'
import AppLogo from '../common/AppLogo.vue'

const authStore = useAuthStore()
authStore.syncFromStorage()

const container = ref(null)
const isScrolled = ref(false)

const primaryCta = computed(() => 
  authStore.isAuthenticated 
    ? { to: authStore.dashboardPath || '/select-role', label: 'Vào bảng điều khiển' } 
    : { to: '/register', label: 'Bắt đầu miễn phí' }
)

// Scroll handler
const handleScroll = () => {
  isScrolled.value = window.scrollY > 20
}

// Intersection Observer for animations
const setupAnimations = () => {
  const observerOptions = {
    root: null,
    rootMargin: '0px',
    threshold: 0.1
  }

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const delay = entry.target.dataset.delay || 0
        setTimeout(() => {
          entry.target.classList.add('is-visible')
        }, delay)
        observer.unobserve(entry.target)
      }
    })
  }, observerOptions)

  // Observe all elements with data-animate attribute
  document.querySelectorAll('[data-animate]').forEach(el => {
    observer.observe(el)
  })
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  setupAnimations()
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style>
/* ============================================
   EDUEXAM LANDING PAGE
   Modern, Animated, Portal-Consistent Design
   ============================================ */

@import url('https://fonts.googleapis.com/css2?family=Montserrat:wght@400;500;600;700;800&family=Be+Vietnam+Pro:wght@300;400;500;600;700&display=swap');

.edu-container {
  --edu-primary: #4f46e5;
  --edu-primary-hover: #4338ca;
  --edu-primary-mid: #6366f1;
  --edu-primary-soft: rgba(79, 70, 229, 0.08);
  --edu-primary-ring: rgba(79, 70, 229, 0.14);
  --edu-bg-light: #f8fafc;
  --edu-bg-dark: #0f172a;
  --edu-text: #1e293b;
  --edu-text-secondary: #64748b;
  --edu-border: #e2e8f0;
  
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, sans-serif;
  background: #ffffff;
  color: var(--edu-text);
  line-height: 1.6;
  -webkit-font-smoothing: antialiased;
  overflow-x: hidden;
}

/* Animation Classes */
[data-animate] {
  opacity: 0;
  transition: opacity 0.6s ease, transform 0.6s ease;
}

[data-animate="fade-up"] {
  transform: translateY(30px);
}

[data-animate="fade-down"] {
  transform: translateY(-30px);
}

[data-animate="slide-left"] {
  transform: translateX(50px);
}

[data-animate="slide-right"] {
  transform: translateX(-50px);
}

[data-animate="scale"] {
  transform: scale(0.9);
}

[data-animate].is-visible {
  opacity: 1;
  transform: translate(0);
}

/* Navigation */
.edu-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  padding: 1rem 4rem;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  transition: all 0.3s ease;
}

.edu-nav--scrolled {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  padding: 0.75rem 4rem;
}

.edu-nav__inner {
  max-width: 100%;
  padding: 0 2rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.edu-nav__logo {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  text-decoration: none;
}

.edu-nav__logo-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, var(--edu-primary), var(--edu-primary-mid));
  color: white;
  font-family: 'Montserrat', sans-serif;
  font-size: 1.25rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.3);
}

.edu-nav__logo-text {
  font-family: 'Montserrat', sans-serif;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--edu-text);
  letter-spacing: -0.02em;
}

.edu-nav__links {
  display: flex;
  align-items: center;
  gap: 2rem;
}

.edu-nav__links a {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--edu-text-secondary);
  text-decoration: none;
  transition: color 0.2s;
  position: relative;
}

.edu-nav__links a:not(.router-link-active):hover {
  color: var(--edu-primary);
}

.edu-nav__links a::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 0;
  width: 0;
  height: 2px;
  background: var(--edu-primary);
  transition: width 0.3s ease;
}

.edu-nav__links a:hover::after {
  width: 100%;
}

.edu-nav__actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.edu-nav__login {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--edu-text-secondary);
  text-decoration: none;
  padding: 0.5rem 1rem;
  transition: color 0.2s;
}

.edu-nav__login:hover {
  color: var(--edu-primary);
}

.edu-nav__cta {
  font-size: 0.875rem;
  font-weight: 600;
  color: white;
  background: var(--edu-primary);
  padding: 0.625rem 1.25rem;
  border-radius: 8px;
  text-decoration: none;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
}

.edu-nav__cta:hover {
  background: var(--edu-primary-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

/* Buttons */
.edu-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-family: 'Be Vietnam Pro', sans-serif;
  font-size: 0.9375rem;
  font-weight: 600;
  padding: 0.75rem 1.5rem;
  border-radius: 10px;
  text-decoration: none;
  transition: all 0.3s ease;
  cursor: pointer;
  border: none;
}

.edu-btn--primary {
  background: var(--edu-primary);
  color: white;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.25);
}

.edu-btn--primary:hover {
  background: var(--edu-primary-hover);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.35);
}

.edu-btn--outline {
  background: transparent;
  color: var(--edu-text);
  border: 1.5px solid var(--edu-border);
}

.edu-btn--outline:hover {
  border-color: var(--edu-primary);
  color: var(--edu-primary);
  background: var(--edu-primary-soft);
}

.edu-btn--lg {
  padding: 0.875rem 2rem;
  font-size: 1rem;
}

.edu-btn--light {
  background: white;
  color: var(--edu-primary);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.1);
}

.edu-btn--light:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.edu-btn--outline-light {
  background: transparent;
  color: white;
  border: 1.5px solid rgba(255, 255, 255, 0.3);
}

.edu-btn--outline-light:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.5);
}

/* Hero Section */
.hero {
  position: relative;
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6rem;
  align-items: center;
  padding: 8rem 6rem 4rem;
  max-width: 100%;
  overflow: hidden;
}

.hero__bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.hero__gradient {
  position: absolute;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.03) 0%, rgba(99, 102, 241, 0.05) 50%, rgba(139, 92, 246, 0.03) 100%);
}

.hero__shapes {
  position: absolute;
  inset: 0;
}

.hero__shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  animation: float 20s ease-in-out infinite;
}

.hero__shape--1 {
  width: 700px;
  height: 700px;
  background: rgba(79, 70, 229, 0.08);
  top: -200px;
  right: -150px;
  animation-delay: 0s;
}

.hero__shape--2 {
  width: 500px;
  height: 500px;
  background: rgba(139, 92, 246, 0.06);
  bottom: -150px;
  left: -150px;
  animation-delay: -5s;
}

.hero__shape--3 {
  width: 400px;
  height: 400px;
  background: rgba(99, 102, 241, 0.05);
  top: 40%;
  left: 60%;
  animation-delay: -10s;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  25% { transform: translate(20px, -20px) rotate(5deg); }
  50% { transform: translate(-10px, 20px) rotate(-5deg); }
  75% { transform: translate(-20px, -10px) rotate(3deg); }
}

.hero__content {
  position: relative;
  z-index: 1;
  max-width: 600px;
}

.hero__badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--edu-primary);
  background: var(--edu-primary-soft);
  padding: 0.5rem 1rem;
  border-radius: 50px;
  margin-bottom: 1.5rem;
}

.hero__title {
  margin-bottom: 1.5rem;
}

.hero__title-line {
  display: block;
  font-family: 'Montserrat', sans-serif;
  font-size: 4.5rem;
  font-weight: 800;
  color: var(--edu-text);
  letter-spacing: -0.03em;
  line-height: 1.05;
}

.hero__title-line--sub {
  font-size: 2rem;
  font-weight: 500;
  color: var(--edu-text-secondary);
  letter-spacing: -0.01em;
  margin-top: 0.75rem;
}

.hero__description {
  font-size: 1.125rem;
  color: var(--edu-text-secondary);
  line-height: 1.8;
  margin-bottom: 2rem;
  max-width: 520px;
}

.hero__actions {
  display: flex;
  gap: 1rem;
}

/* Preview Card */
.hero__preview {
  position: relative;
  z-index: 1;
  max-width: 550px;
}

.preview-card {
  background: white;
  border-radius: 16px;
  border: 1px solid var(--edu-border);
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.15);
  transition: transform 0.5s ease, box-shadow 0.5s ease;
}

.preview-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 35px 60px -15px rgba(79, 70, 229, 0.2);
}

.preview-card__header {
  background: linear-gradient(135deg, #1e293b, #334155);
  padding: 1rem 1.25rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.preview-card__dots {
  display: flex;
  gap: 0.5rem;
}

.preview-card__dots span {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
}

.preview-card__dots span:first-child { background: #ef4444; }
.preview-card__dots span:nth-child(2) { background: #f59e0b; }
.preview-card__dots span:nth-child(3) { background: #22c55e; }

.preview-card__title {
  flex: 1;
  font-size: 0.8125rem;
  font-weight: 500;
  color: white;
  text-align: center;
}

.preview-card__status {
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

.preview-card__status-dot {
  width: 6px;
  height: 6px;
  background: #4ade80;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.preview-card__body {
  padding: 1.25rem;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.25rem;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid var(--edu-border);
}

.preview-info h4 {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--edu-text);
  margin-bottom: 0.25rem;
}

.preview-info p {
  font-size: 0.75rem;
  color: var(--edu-text-secondary);
}

.preview-timer {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-family: 'JetBrains Mono', monospace;
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--edu-primary);
  background: var(--edu-primary-soft);
  padding: 0.5rem 0.75rem;
  border-radius: 8px;
}

.preview-alerts {
  margin-bottom: 1.25rem;
}

.preview-alert {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  border-radius: 10px;
  transition: transform 0.2s;
}

.preview-alert:hover {
  transform: translateX(4px);
}

.preview-alert--warning {
  background: rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.2);
}

.preview-alert__icon {
  width: 32px;
  height: 32px;
  background: rgba(245, 158, 11, 0.15);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #f59e0b;
}

.preview-alert__content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.preview-alert__title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--edu-text);
}

.preview-alert__desc {
  font-size: 0.6875rem;
  color: var(--edu-text-secondary);
}

.preview-alert__time {
  font-size: 0.6875rem;
  color: var(--edu-text-secondary);
}

.preview-students {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.preview-student {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  background: var(--edu-bg-light);
  border-radius: 10px;
  transition: all 0.2s;
}

.preview-student:hover {
  background: var(--edu-primary-soft);
}

.preview-student__avatar {
  width: 36px;
  height: 36px;
  color: white;
  font-size: 0.75rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
}

.preview-student__info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.preview-student__name {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--edu-text);
}

.preview-student__status {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.6875rem;
  font-weight: 500;
}

.preview-student__status--good {
  color: #16a34a;
}

.preview-student__status--warning {
  color: #d97706;
}

.preview-student__status--danger {
  color: #dc2626;
}

.preview-student__progress {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
}

.preview-student__progress span {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--edu-text);
}

.preview-progress-bar {
  width: 60px;
  height: 4px;
  background: var(--edu-border);
  border-radius: 2px;
  overflow: hidden;
}

.preview-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--edu-primary), var(--edu-primary-mid));
  border-radius: 2px;
  transition: width 1s ease;
}

.preview-progress-fill--warning {
  background: linear-gradient(90deg, #f59e0b, #d97706);
}

/* Stats Section */
.stats {
  padding: 5rem 6rem;
  background: linear-gradient(135deg, var(--edu-primary), var(--edu-primary-mid));
  position: relative;
  overflow: hidden;
}

.stats__grid {
  max-width: 1400px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4rem;
  position: relative;
}

.stat {
  text-align: center;
  padding: 2rem;
}

.stat__value {
  display: block;
  font-family: 'Montserrat', sans-serif;
  font-size: 3.5rem;
  font-weight: 800;
  color: white;
  letter-spacing: -0.02em;
  margin-bottom: 0.75rem;
}

.stat__label {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.75);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

/* Features Section */
.features {
  padding: 6rem 6rem;
  background: var(--edu-bg-light);
}

.features__header {
  text-align: center;
  max-width: 700px;
  margin: 0 auto 4rem;
}

.features__label {
  display: inline-block;
  font-size: 0.8125rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.15em;
  color: var(--edu-primary);
  margin-bottom: 0.75rem;
}

.features__title {
  font-family: 'Montserrat', sans-serif;
  font-size: 2.75rem;
  font-weight: 700;
  color: var(--edu-text);
  margin-bottom: 1rem;
  letter-spacing: -0.02em;
}

.features__subtitle {
  font-size: 1.125rem;
  color: var(--edu-text-secondary);
  line-height: 1.7;
}

.features__grid {
  max-width: 1400px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 2rem;
}

.feature {
  background: white;
  padding: 2rem;
  border-radius: 16px;
  border: 1px solid var(--edu-border);
  transition: all 0.4s ease;
  position: relative;
  overflow: hidden;
}

.feature::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--edu-primary), var(--edu-primary-mid));
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.4s ease;
}

.feature:hover {
  transform: translateY(-6px);
  box-shadow: 0 20px 40px rgba(79, 70, 229, 0.12);
  border-color: var(--edu-primary);
}

.feature:hover::before {
  transform: scaleX(1);
}

.feature__icon {
  width: 52px;
  height: 52px;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.1), rgba(99, 102, 241, 0.1));
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--edu-primary);
  margin-bottom: 1.25rem;
  transition: all 0.3s ease;
}

.feature:hover .feature__icon {
  background: linear-gradient(135deg, var(--edu-primary), var(--edu-primary-mid));
  color: white;
  transform: scale(1.1);
}

.feature__content {
  margin-bottom: 1rem;
}

.feature__title {
  font-family: 'Montserrat', sans-serif;
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--edu-text);
  margin-bottom: 0.5rem;
}

.feature__desc {
  font-size: 0.875rem;
  color: var(--edu-text-secondary);
  line-height: 1.6;
}

.feature__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.feature__tag {
  font-size: 0.6875rem;
  font-weight: 500;
  color: var(--edu-primary);
  background: var(--edu-primary-soft);
  padding: 0.25rem 0.625rem;
  border-radius: 6px;
}

/* Roles Section */
.roles {
  padding: 6rem 6rem;
  background: white;
}

.roles__header {
  text-align: center;
  max-width: 700px;
  margin: 0 auto 4rem;
}

.roles__label {
  display: inline-block;
  font-size: 0.8125rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.15em;
  color: var(--edu-primary);
  margin-bottom: 0.75rem;
}

.roles__title {
  font-family: 'Montserrat', sans-serif;
  font-size: 2.75rem;
  font-weight: 700;
  color: var(--edu-text);
  letter-spacing: -0.02em;
}

.roles__grid {
  max-width: 1400px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 2rem;
}

.role-card {
  background: white;
  padding: 2.5rem;
  border-radius: 20px;
  border: 1px solid var(--edu-border);
  transition: all 0.4s ease;
  position: relative;
}

.role-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.1);
}

.role-card--primary {
  background: linear-gradient(135deg, var(--edu-primary), var(--edu-primary-mid));
  border: none;
  color: white;
}

.role-card--primary .role-card__title,
.role-card--primary .role-card__desc,
.role-card--primary .role-card__features li {
  color: white;
}

.role-card--primary .role-card__desc,
.role-card--primary .role-card__features li {
  opacity: 0.85;
}

.role-card__icon {
  width: 64px;
  height: 64px;
  background: var(--edu-primary-soft);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--edu-primary);
  margin-bottom: 1.5rem;
  transition: transform 0.3s ease;
}

.role-card:hover .role-card__icon {
  transform: scale(1.1) rotate(-5deg);
}

.role-card--primary .role-card__icon {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.role-card__title {
  font-family: 'Montserrat', sans-serif;
  font-size: 1.375rem;
  font-weight: 700;
  color: var(--edu-text);
  margin-bottom: 0.75rem;
}

.role-card__desc {
  font-size: 0.9375rem;
  color: var(--edu-text-secondary);
  line-height: 1.6;
  margin-bottom: 1.5rem;
}

.role-card__features {
  list-style: none;
  padding: 0;
  margin: 0 0 1.5rem;
}

.role-card__features li {
  font-size: 0.875rem;
  color: var(--edu-text);
  padding: 0.5rem 0;
  padding-left: 1.5rem;
  position: relative;
}

.role-card__features li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 8px;
  height: 8px;
  background: var(--edu-primary);
  border-radius: 50%;
}

.role-card--primary .role-card__features li::before {
  background: white;
}

.role-card__link {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--edu-primary);
  text-decoration: none;
  transition: gap 0.2s;
}

.role-card__link:hover {
  gap: 0.75rem;
}

.role-card--primary .role-card__link {
  color: white;
}

/* Workflow Section */
.workflow {
  padding: 6rem 6rem;
  background: var(--edu-bg-light);
  position: relative;
  overflow: hidden;
}

.workflow__header {
  text-align: center;
  max-width: 700px;
  margin: 0 auto 4rem;
}

.workflow__label {
  display: inline-block;
  font-size: 0.8125rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.15em;
  color: var(--edu-primary);
  margin-bottom: 0.75rem;
}

.workflow__title {
  font-family: 'Montserrat', sans-serif;
  font-size: 2.75rem;
  font-weight: 700;
  color: var(--edu-text);
  letter-spacing: -0.02em;
}

.workflow__timeline {
  max-width: 1400px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 2rem;
  position: relative;
}

.workflow__line {
  position: absolute;
  top: 40px;
  left: 10%;
  right: 10%;
  height: 2px;
  background: linear-gradient(90deg, var(--edu-primary), var(--edu-primary-mid), var(--edu-primary));
  opacity: 0.2;
}

.workflow__step {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  position: relative;
}

.workflow__number {
  font-family: 'Montserrat', sans-serif;
  font-size: 3rem;
  font-weight: 800;
  color: var(--edu-primary);
  opacity: 0.1;
  margin-bottom: 1rem;
  position: relative;
  z-index: 1;
}

.workflow__card {
  background: white;
  padding: 1.5rem;
  border-radius: 16px;
  border: 1px solid var(--edu-border);
  transition: all 0.3s ease;
  width: 100%;
}

.workflow__card:hover {
  transform: translateY(-4px);
  box-shadow: 0 15px 35px rgba(79, 70, 229, 0.1);
  border-color: var(--edu-primary);
}

.workflow__card-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, var(--edu-primary), var(--edu-primary-mid));
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin: 0 auto 1rem;
  transition: transform 0.3s ease;
}

.workflow__card:hover .workflow__card-icon {
  transform: rotateY(180deg);
}

.workflow__card h3 {
  font-family: 'Montserrat', sans-serif;
  font-size: 1rem;
  font-weight: 600;
  color: var(--edu-text);
  margin-bottom: 0.5rem;
}

.workflow__card p {
  font-size: 0.8125rem;
  color: var(--edu-text-secondary);
  line-height: 1.6;
}

/* CTA Section */
.cta {
  padding: 8rem 6rem;
  background: linear-gradient(135deg, var(--edu-bg-dark), #1e293b);
  position: relative;
  overflow: hidden;
  text-align: center;
}

.cta__bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.cta__shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
}

.cta__shape--1 {
  width: 600px;
  height: 600px;
  background: rgba(79, 70, 229, 0.4);
  top: -150px;
  right: 15%;
}

.cta__shape--2 {
  width: 400px;
  height: 400px;
  background: rgba(139, 92, 246, 0.3);
  bottom: -150px;
  left: 15%;
}

.cta__content {
  max-width: 700px;
  margin: 0 auto;
  position: relative;
}

.cta__title {
  font-family: 'Montserrat', sans-serif;
  font-size: 3.5rem;
  font-weight: 700;
  color: white;
  margin-bottom: 1.25rem;
  letter-spacing: -0.02em;
}

.cta__desc {
  font-size: 1.25rem;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 2.5rem;
}

.cta__actions {
  display: flex;
  justify-content: center;
  gap: 1.25rem;
}

/* Footer */
.footer {
  background: white;
  border-top: 1px solid var(--edu-border);
}

.footer__inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 4rem 6rem 2rem;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6rem;
}

.footer__brand {
  max-width: 350px;
}

.footer__logo {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  text-decoration: none;
  margin-bottom: 1rem;
}

.footer__logo-icon {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, var(--edu-primary), var(--edu-primary-mid));
  color: white;
  font-family: 'Montserrat', sans-serif;
  font-size: 1.25rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
}

.footer__logo-text {
  font-family: 'Montserrat', sans-serif;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--edu-text);
}

.footer__tagline {
  font-size: 1rem;
  color: var(--edu-text-secondary);
  margin-bottom: 1.5rem;
  line-height: 1.6;
}

.footer__social {
  display: flex;
  gap: 0.75rem;
}

.footer__social-link {
  width: 44px;
  height: 44px;
  background: var(--edu-bg-light);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--edu-text-secondary);
  transition: all 0.2s;
}

.footer__social-link:hover {
  background: var(--edu-primary);
  color: white;
  transform: translateY(-2px);
}

.footer__links {
  display: flex;
  gap: 5rem;
}

.footer__link-group h4 {
  font-family: 'Montserrat', sans-serif;
  font-size: 0.8125rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--edu-text);
  margin-bottom: 1.25rem;
}

.footer__link-group a {
  display: block;
  font-size: 0.9375rem;
  color: var(--edu-text-secondary);
  text-decoration: none;
  margin-bottom: 0.625rem;
  transition: color 0.2s;
}

.footer__link-group a:hover {
  color: var(--edu-primary);
}

.footer__bottom {
  max-width: 1400px;
  margin: 0 auto;
  padding: 1.5rem 6rem;
  border-top: 1px solid var(--edu-border);
}

.footer__bottom p {
  font-size: 0.875rem;
  color: var(--edu-text-secondary);
  text-align: center;
}

/* Responsive */
@media (max-width: 1400px) {
  .hero {
    padding: 7rem 4rem 4rem;
    gap: 4rem;
  }
  
  .hero__title-line {
    font-size: 3.5rem;
  }
  
  .hero__title-line--sub {
    font-size: 1.75rem;
  }
  
  .hero__preview {
    max-width: 480px;
  }
  
  .stats, .features, .roles, .workflow, .cta {
    padding-left: 4rem;
    padding-right: 4rem;
  }
  
  .stats__grid, .features__grid, .roles__grid, .workflow__timeline {
    max-width: 1200px;
  }
  
  .footer__inner {
    padding-left: 4rem;
    padding-right: 4rem;
    gap: 4rem;
  }
}

@media (max-width: 1024px) {
  .edu-nav {
    padding: 1rem 2rem;
  }
  
  .edu-nav--scrolled {
    padding: 0.75rem 2rem;
  }
  
  .edu-nav__links {
    display: none;
  }
  
  .hero {
    grid-template-columns: 1fr;
    text-align: center;
    min-height: auto;
    padding: 6rem 2rem 3rem;
    gap: 3rem;
  }
  
  .hero__title-line {
    font-size: 3rem;
  }
  
  .hero__title-line--sub {
    font-size: 1.5rem;
  }
  
  .hero__description {
    margin: 0 auto 2rem;
  }
  
  .hero__actions {
    justify-content: center;
  }
  
  .hero__preview {
    order: -1;
    max-width: 500px;
    margin: 0 auto;
  }
  
  .hero__shape--1 {
    width: 400px;
    height: 400px;
  }
  
  .hero__shape--2 {
    width: 300px;
    height: 300px;
  }
  
  .features__grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .roles__grid {
    grid-template-columns: 1fr;
    max-width: 450px;
  }
  
  .workflow__timeline {
    grid-template-columns: repeat(2, 1fr);
    gap: 2rem;
  }
  
  .workflow__line {
    display: none;
  }
  
  .stats {
    padding: 3rem 2rem;
  }
  
  .stats__grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 2rem;
  }
  
  .stat__value {
    font-size: 2.5rem;
  }
  
  .features, .roles, .workflow {
    padding: 4rem 2rem;
  }
  
  .cta {
    padding: 5rem 2rem;
  }
  
  .cta__title {
    font-size: 2.5rem;
  }
  
  .footer__inner {
    grid-template-columns: 1fr;
    gap: 2rem;
    padding: 3rem 2rem 2rem;
  }
  
  .footer__links {
    flex-direction: column;
    gap: 2rem;
  }
  
  .footer__bottom {
    padding: 1.5rem 2rem;
  }
}

@media (max-width: 768px) {
  .hero {
    padding: 5rem 1.5rem 3rem;
  }
  
  .hero__title-line {
    font-size: 2.25rem;
  }
  
  .hero__title-line--sub {
    font-size: 1.125rem;
  }
  
  .hero__description {
    font-size: 1rem;
  }
  
  .hero__actions {
    flex-direction: column;
    align-items: center;
  }
  
  .hero__btn--lg {
    width: 100%;
    max-width: 280px;
    justify-content: center;
  }
  
  .features__grid {
    grid-template-columns: 1fr;
  }
  
  .workflow__timeline {
    grid-template-columns: 1fr;
  }
  
  .stats__grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 1rem;
  }
  
  .stat__value {
    font-size: 2rem;
  }
  
  .stat__label {
    font-size: 0.75rem;
  }
  
  .features__title, .roles__title, .workflow__title {
    font-size: 2rem;
  }
  
  .cta__title {
    font-size: 2rem;
  }
  
  .cta__actions {
    flex-direction: column;
    align-items: center;
  }
  
  .cta__btn--lg {
    width: 100%;
    max-width: 280px;
    justify-content: center;
  }
}
</style>
