<template>
  <div class="landing-root cosmos-aurora-bg cosmos-hex-pattern">
    <!-- Starfield canvas -->
    <canvas ref="starfieldCanvas" class="starfield-canvas" aria-hidden="true" />

    <!-- Floating Aurora Orbs -->
    <div class="cosmos-orb cosmos-orb--1" aria-hidden="true" />
    <div class="cosmos-orb cosmos-orb--2" aria-hidden="true" />
    <div class="cosmos-orb cosmos-orb--3" aria-hidden="true" />
    <div class="cosmos-orb cosmos-orb--rose" aria-hidden="true" />

    <!-- Navigation -->
    <nav class="cosmos-nav--floating cosmos-reveal" style="z-index: 100;">
      <div class="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
        <RouterLink to="/" class="cosmos-nav__brand">
          <div class="brand-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="12" cy="12" r="10" stroke="url(#brandGrad)" stroke-width="2" fill="none"/>
              <circle cx="12" cy="12" r="4" fill="url(#brandGrad)"/>
              <circle cx="12" cy="12" r="7" stroke="url(#brandGrad)" stroke-width="0.5" fill="none" stroke-dasharray="2 2"/>
              <defs>
                <linearGradient id="brandGrad" x1="0" y1="0" x2="24" y2="24">
                  <stop offset="0%" stop-color="#06b6d4"/>
                  <stop offset="50%" stop-color="#d946ef"/>
                  <stop offset="100%" stop-color="#f59e0b"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <span class="brand-name">EduExam <span class="cosmos-text-prismatic">Cosmos</span></span>
        </RouterLink>
        <div class="flex gap-3 items-center">
          <RouterLink to="/login" class="cosmos-btn cosmos-btn--ghost cosmos-btn--sm">Đăng nhập</RouterLink>
          <RouterLink to="/register" class="cosmos-btn cosmos-btn--primary cosmos-btn--sm">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
            Bắt đầu
          </RouterLink>
        </div>
      </div>
    </nav>

    <!-- Hero Section -->
    <section class="hero-section relative overflow-hidden">
      <div class="max-w-6xl mx-auto px-6 pt-36 pb-20 flex flex-col lg:flex-row items-center gap-12 lg:gap-16">
        <!-- Left: Text -->
        <div class="flex-1 text-center lg:text-left">
          <div class="inline-flex items-center gap-2 px-4 py-2 rounded-full mb-8 cosmos-reveal cosmos-glass--teal" style="border: 1px solid var(--cosmos-teal-border);">
            <span class="w-2 h-2 rounded-full cosmos-pulse" style="background: var(--cosmos-teal);" />
            <span class="text-xs font-bold uppercase tracking-widest" style="color: var(--cosmos-teal);">
              Nền tảng thi thông minh 2026
            </span>
          </div>

          <h1 class="text-5xl lg:text-7xl font-black leading-[1.05] mb-6 cosmos-heading-display cosmos-reveal" style="animation-delay: 100ms;">
            Thi trắc nghiệm<br />
            <span class="cosmos-text-prismatic">vũ trụ số</span><br />
            thông minh hơn
          </h1>

          <p class="text-lg mb-10 max-w-lg mx-auto lg:mx-0 leading-relaxed cosmos-reveal" style="animation-delay: 200ms; color: var(--cosmos-text-secondary);">
            EduExam Cosmos mang đến trải nghiệm thi trắc nghiệm với AI, giám sát realtime và phân tích chấm đẹp. Từ tạo đề đến kết quả — một luồng liên tục.
          </p>

          <div class="flex flex-wrap gap-4 justify-center lg:justify-start cosmos-reveal" style="animation-delay: 300ms;">
            <RouterLink to="/login" class="cosmos-btn cosmos-btn--primary cosmos-btn--lg cosmos-btn--prismatic">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20M2 12h20"/></svg>
              Khám phá ngay
            </RouterLink>
            <RouterLink to="/register" class="cosmos-btn cosmos-btn--outline cosmos-btn--lg">
              Tìm hiểu thêm
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
            </RouterLink>
          </div>
        </div>

        <!-- Right: Prismatic Dashboard Preview -->
        <div class="flex-1 hidden lg:block relative w-full max-w-lg" style="height: 520px;">
          <!-- Floating prism decoration -->
          <div class="absolute inset-0 flex items-center justify-center">
            <div class="prism-decoration" aria-hidden="true">
              <svg width="200" height="200" viewBox="0 0 200 200" fill="none" xmlns="http://www.w3.org/2000/svg">
                <polygon points="100,10 190,160 10,160" stroke="url(#prismGrad)" stroke-width="1" fill="none" opacity="0.3"/>
                <polygon points="100,30 175,155 25,155" stroke="url(#prismGrad)" stroke-width="0.5" fill="none" opacity="0.2"/>
                <defs>
                  <linearGradient id="prismGrad" x1="0" y1="0" x2="200" y2="200">
                    <stop offset="0%" stop-color="#06b6d4"/>
                    <stop offset="50%" stop-color="#d946ef"/>
                    <stop offset="100%" stop-color="#f59e0b"/>
                  </linearGradient>
                </defs>
              </svg>
            </div>
          </div>

          <!-- Card 1: AI Create -->
          <div class="cosmos-glow-card absolute top-0 right-0 w-72 p-5 cosmos-scale" style="animation-delay: 0.3s">
            <div class="flex items-center gap-3 mb-4">
              <div class="w-12 h-12 rounded-xl flex items-center justify-center" style="background: var(--cosmos-teal-soft)">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--cosmos-teal)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>
              </div>
              <div>
                <p class="font-bold text-sm" style="color: var(--cosmos-text)">Tạo đề với AI</p>
                <p class="text-xs" style="color: var(--cosmos-text-muted)">Nhanh chóng</p>
              </div>
            </div>
            <div class="h-2 rounded-full overflow-hidden cosmos-progress mb-2">
              <div class="cosmos-progress__bar" style="width: 78%; animation: cosmosShimmer 2s infinite;" />
            </div>
            <p class="text-xs" style="color: var(--cosmos-text-muted);">Đang sinh câu hỏi...</p>
          </div>

          <!-- Card 2: Live Monitoring -->
          <div class="cosmos-glow-card absolute top-40 left-0 w-64 p-5 cosmos-scale" style="animation-delay: 0.5s">
            <div class="flex items-center gap-3 mb-3">
              <div class="w-12 h-12 rounded-xl flex items-center justify-center cosmos-ring is-live" style="background: var(--cosmos-rose-soft)">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--cosmos-rose)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
              </div>
              <div>
                <p class="font-bold text-sm" style="color: var(--cosmos-text)">Giám sát realtime</p>
                <p class="text-xs" style="color: var(--cosmos-text-muted)">24/7</p>
              </div>
            </div>
            <div class="flex items-center gap-3">
              <div class="flex -space-x-2">
                <div class="cosmos-avatar cosmos-avatar--sm" style="border-color: var(--cosmos-teal-border);">T</div>
                <div class="cosmos-avatar cosmos-avatar--sm" style="border-color: var(--cosmos-rose-border); background: var(--cosmos-rose-soft); color: var(--cosmos-rose);">H</div>
                <div class="cosmos-avatar cosmos-avatar--sm" style="border-color: var(--cosmos-success-border); background: var(--cosmos-success-soft); color: var(--cosmos-success);">M</div>
              </div>
              <span class="text-xs font-medium" style="color: var(--cosmos-success);">+28 thí sinh online</span>
            </div>
          </div>

          <!-- Card 3: Results -->
          <div class="cosmos-glow-card absolute bottom-8 right-4 w-72 p-5 cosmos-scale" style="animation-delay: 0.7s">
            <div class="flex items-center gap-3 mb-3">
              <div class="w-12 h-12 rounded-xl flex items-center justify-center" style="background: var(--cosmos-success-soft)">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--cosmos-success)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
              </div>
              <div>
                <p class="font-bold text-sm" style="color: var(--cosmos-text)">Phân tích kết quả</p>
                <p class="text-xs" style="color: var(--cosmos-text-muted)">Chi tiết</p>
              </div>
            </div>
            <div class="flex gap-3">
              <div class="flex-1 text-center py-2 rounded-lg" style="background: var(--cosmos-bg-3)">
                <p class="text-lg font-black" style="color: var(--cosmos-teal); font-family: var(--cosmos-font-mono);">94%</p>
                <p class="text-[10px] font-semibold uppercase tracking-wide" style="color: var(--cosmos-text-muted);">Đạt</p>
              </div>
              <div class="flex-1 text-center py-2 rounded-lg" style="background: var(--cosmos-bg-3)">
                <p class="text-lg font-black" style="color: var(--cosmos-rose); font-family: var(--cosmos-font-mono);">156</p>
                <p class="text-[10px] font-semibold uppercase tracking-wide" style="color: var(--cosmos-text-muted);">Thí sinh</p>
              </div>
              <div class="flex-1 text-center py-2 rounded-lg" style="background: var(--cosmos-bg-3)">
                <p class="text-lg font-black" style="color: var(--cosmos-success); font-family: var(--cosmos-font-mono);">8.4</p>
                <p class="text-[10px] font-semibold uppercase tracking-wide" style="color: var(--cosmos-text-muted);">Điểm TB</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Stats Counter Row -->
    <section class="py-16 px-6" style="background: rgba(6, 11, 24, 0.5);">
      <div class="max-w-5xl mx-auto grid grid-cols-2 md:grid-cols-4 gap-4">
        <div
          v-for="(stat, i) in stats"
          :key="i"
          class="cosmos-card p-6 text-center cosmos-reveal cosmos-lift"
          :style="{ animationDelay: `${i * 80}ms` }"
        >
          <div class="w-12 h-12 rounded-2xl flex items-center justify-center mx-auto mb-3" :style="`background: ${stat.bg};`">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" :stroke="stat.color" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">{{ stat.iconPath }}</svg>
          </div>
          <p class="text-3xl font-black mb-1" :style="`color: var(--cosmos-text); font-family: var(--cosmos-font-mono);`">{{ stat.value }}</p>
          <p class="text-sm font-semibold" style="color: var(--cosmos-text-muted);">{{ stat.label }}</p>
        </div>
      </div>
    </section>

    <!-- Features Section - Bento Grid -->
    <section class="py-24 px-6 cosmos-mesh">
      <div class="max-w-6xl mx-auto">
        <div class="text-center mb-14 cosmos-reveal">
          <div class="inline-flex items-center gap-2 px-4 py-2 rounded-full mb-5 cosmos-glass--rose" style="border: 1px solid var(--cosmos-rose-border);">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--cosmos-rose)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>
            <span class="text-xs font-bold uppercase tracking-widest" style="color: var(--cosmos-rose);">Tính năng nổi bật</span>
          </div>
          <h2 class="text-4xl lg:text-5xl font-black leading-tight mb-4 cosmos-heading-display">
            Mọi thứ bạn cần
          </h2>
          <p class="text-lg max-w-2xl mx-auto" style="color: var(--cosmos-text-secondary);">
            Từ tạo đề đến chấm điểm — một hệ thống hoàn chỉnh, không thiếu sót.
          </p>
        </div>

        <!-- Bento Grid -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-5">
          <!-- Large: AI Question Generation -->
          <div class="md:col-span-2 cosmos-glow-card cosmos-spring p-8 cosmos-reveal cosmos-tilt">
            <div class="flex items-start gap-4 mb-6">
              <div class="w-14 h-14 rounded-2xl flex items-center justify-center flex-shrink-0" style="background: linear-gradient(135deg, var(--cosmos-teal-soft), var(--cosmos-rose-soft));">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--cosmos-teal)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>
              </div>
              <div>
                <h3 class="text-xl font-bold mb-2 cosmos-heading-display" style="color: var(--cosmos-text);">Sinh câu hỏi bằng AI</h3>
                <p class="leading-relaxed" style="color: var(--cosmos-text-secondary);">Tạo ra câu hỏi tự động từ nội dung bài giảng. Hỗ trợ nhiều định dạng, tự luận, trắc nghiệm, và câu hỏi tình huống.</p>
              </div>
            </div>
            <div class="grid grid-cols-3 gap-3">
              <div class="cosmos-card p-3 text-center">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--cosmos-teal)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mb-1"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
                <p class="text-xs font-bold" style="color: var(--cosmos-text);">100+ câu hỏi</p>
              </div>
              <div class="cosmos-card p-3 text-center">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--cosmos-rose)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mb-1"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>
                <p class="text-xs font-bold" style="color: var(--cosmos-text);">AI thông minh</p>
              </div>
              <div class="cosmos-card p-3 text-center">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--cosmos-success)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mb-1"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
                <p class="text-xs font-bold" style="color: var(--cosmos-text);">Chỉ 30 giây</p>
              </div>
            </div>
          </div>

          <!-- Small: Realtime Monitoring -->
          <div class="cosmos-card cosmos-spring p-7 cosmos-reveal cosmos-tilt" style="animation-delay: 100ms;">
            <div class="w-14 h-14 rounded-2xl flex items-center justify-center mb-5 cosmos-ring is-live" style="background: var(--cosmos-rose-soft);">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--cosmos-rose)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
            </div>
            <h3 class="text-xl font-bold mb-3 cosmos-heading-display" style="color: var(--cosmos-text);">Giám sát realtime</h3>
            <p class="leading-relaxed mb-4" style="color: var(--cosmos-text-secondary);">Theo dõi tiến độ làm bài, phát hiện hành vi bất thường và cảnh báo kịp thời.</p>
            <span class="cosmos-badge cosmos-badge--success">
              <span class="cosmos-badge__dot" />
              Đang hoạt động
            </span>
          </div>

          <!-- Small: Analytics -->
          <div class="cosmos-card cosmos-spring p-7 cosmos-reveal cosmos-tilt" style="animation-delay: 150ms;">
            <div class="w-14 h-14 rounded-2xl flex items-center justify-center mb-5" style="background: var(--cosmos-success-soft);">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--cosmos-success)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
            </div>
            <h3 class="text-xl font-bold mb-3 cosmos-heading-display" style="color: var(--cosmos-text);">Thống kê chi tiết</h3>
            <p class="leading-relaxed mb-4" style="color: var(--cosmos-text-secondary);">Báo cáo kết quả thi, phân tích điểm số và xuất dữ liệu theo nhiều định dạng.</p>
            <div class="flex -space-x-1">
              <div class="w-6 h-6 rounded-full" style="background: var(--cosmos-teal);" />
              <div class="w-6 h-6 rounded-full" style="background: var(--cosmos-rose);" />
              <div class="w-6 h-6 rounded-full" style="background: var(--cosmos-success);" />
            </div>
          </div>

          <!-- Small: Security -->
          <div class="cosmos-card cosmos-spring p-7 cosmos-reveal cosmos-tilt" style="animation-delay: 200ms;">
            <div class="w-14 h-14 rounded-2xl flex items-center justify-center mb-5" style="background: var(--cosmos-warning-soft);">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--cosmos-warning)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            </div>
            <h3 class="text-xl font-bold mb-3 cosmos-heading-display" style="color: var(--cosmos-text);">Bảo mật cao cấp</h3>
            <p class="leading-relaxed mb-4" style="color: var(--cosmos-text-secondary);">Mã hóa dữ liệu, xác thực hai yếu tố và kiểm soát truy cập theo vai trò.</p>
            <div class="flex gap-2">
              <span class="cosmos-tag cosmos-tag--teal">AES-256</span>
              <span class="cosmos-tag cosmos-tag--rose">2FA</span>
            </div>
          </div>

          <!-- Small: Multi-platform -->
          <div class="cosmos-card cosmos-spring p-7 cosmos-reveal cosmos-tilt" style="animation-delay: 250ms;">
            <div class="w-14 h-14 rounded-2xl flex items-center justify-center mb-5" style="background: rgba(59, 130, 246, 0.1);">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>
            </div>
            <h3 class="text-xl font-bold mb-3 cosmos-heading-display" style="color: var(--cosmos-text);">Đa nền tảng</h3>
            <p class="leading-relaxed mb-4" style="color: var(--cosmos-text-secondary);">Truy cập từ mọi thiết bị: máy tính, máy tính bảng và điện thoại thông minh.</p>
            <div class="flex items-center gap-2 text-xs" style="color: var(--cosmos-text-muted);">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--cosmos-success)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
              Responsive 100%
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Role Cards Section -->
    <section class="py-24 px-6" style="background: rgba(6, 11, 24, 0.5);">
      <div class="max-w-6xl mx-auto">
        <div class="text-center mb-14 cosmos-reveal">
          <div class="inline-flex items-center gap-2 px-4 py-2 rounded-full mb-5 cosmos-glass--success" style="border: 1px solid var(--cosmos-success-border);">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--cosmos-success)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
            <span class="text-xs font-bold uppercase tracking-widest" style="color: var(--cosmos-success);">3 vai trò chính</span>
          </div>
          <h2 class="text-4xl lg:text-5xl font-black leading-tight mb-4 cosmos-heading-display">
            Phù hợp với mọi người
          </h2>
          <p class="text-lg max-w-2xl mx-auto" style="color: var(--cosmos-text-secondary);">
            Học sinh, giáo viên và quản trị — mỗi người có không gian riêng.
          </p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div
            v-for="(role, i) in roles"
            :key="i"
            class="cosmos-glow-card cosmos-lift p-8 text-center cosmos-reveal cosmos-tilt"
            :style="{ animationDelay: `${(i + 1) * 120}ms` }"
          >
            <div class="w-24 h-24 rounded-full flex items-center justify-center mx-auto mb-6 cosmos-spring transition-all"
              :style="`background: ${role.bg}; box-shadow: 0 0 40px ${role.color}33;`">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" :stroke="role.color" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" v-html="role.iconPath" />
            </div>
            <h3 class="text-2xl font-black mb-3 cosmos-heading-display" style="color: var(--cosmos-text);">{{ role.title }}</h3>
            <p class="mb-7 leading-relaxed" style="color: var(--cosmos-text-secondary);">{{ role.desc }}</p>
            <RouterLink
              :to="role.ctaLink"
              class="cosmos-btn cosmos-btn--primary cosmos-btn--md"
            >
              {{ role.ctaText }}
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
            </RouterLink>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="py-24 px-6 cosmos-mesh">
      <div class="max-w-4xl mx-auto">
        <div class="cosmos-glow-card relative overflow-hidden p-14 lg:p-20 text-center cosmos-reveal">
          <!-- Decorative orbs -->
          <div class="cosmos-orb cosmos-orb--1 opacity-20" style="top: -120px; left: -100px; width: 350px; height: 350px;" />
          <div class="cosmos-orb cosmos-orb--2 opacity-20" style="bottom: -100px; right: -80px; width: 300px; height: 300px;" />

          <div class="relative z-10">
            <div class="inline-flex items-center gap-2 px-4 py-2 rounded-full mb-6 cosmos-glass--teal" style="border: 1px solid var(--cosmos-teal-border);">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--cosmos-teal)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polygon points="4 14 10 14 10 20 4 20 4 14"/><line x1="1" y1="10" x2="7" y2="10"/></svg>
              <span class="text-xs font-bold uppercase tracking-widest" style="color: var(--cosmos-teal);">Sẵn sàng bắt đầu?</span>
            </div>

            <h2 class="text-4xl lg:text-5xl font-black leading-tight mb-6 cosmos-heading-display">
              Tham gia cùng <span class="cosmos-text-prismatic">hàng nghìn</span> người dùng
            </h2>

            <p class="text-lg mb-10 max-w-2xl mx-auto leading-relaxed" style="color: var(--cosmos-text-secondary);">
              Đăng ký ngay hôm nay và trải nghiệm nền tảng thi trực tuyến hiện đại nhất.
            </p>

            <RouterLink
              v-if="!authStore.isAuthenticated"
              to="/register"
              class="cosmos-btn cosmos-btn--primary cosmos-btn--lg cosmos-btn--prismatic"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/></svg>
              Đăng ký miễn phí
            </RouterLink>
            <RouterLink
              v-else
              :to="primaryCta.to"
              class="cosmos-btn cosmos-btn--primary cosmos-btn--lg"
            >
              {{ primaryCta.label }}
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
            </RouterLink>
          </div>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="py-8 px-6 border-t" style="background: rgba(3, 7, 18, 0.9); border-color: var(--cosmos-border);">
      <div class="max-w-6xl mx-auto flex flex-col md:flex-row items-center justify-between gap-4">
        <RouterLink to="/" class="flex items-center gap-2">
          <div class="brand-icon" style="width:1.75rem;height:1.75rem; border-radius: 6px; display: flex; align-items: center; justify-content: center;">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="10" stroke="url(#footerGrad)" stroke-width="2" fill="none"/>
              <circle cx="12" cy="12" r="4" fill="url(#footerGrad)"/>
              <defs>
                <linearGradient id="footerGrad" x1="0" y1="0" x2="24" y2="24">
                  <stop offset="0%" stop-color="#06b6d4"/>
                  <stop offset="100%" stop-color="#d946ef"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <span class="font-black text-base" style="font-family: var(--cosmos-font-display); color: var(--cosmos-teal);">EduExam Cosmos</span>
        </RouterLink>
        <p class="text-sm" style="color: var(--cosmos-text-muted);">
          2026 EduExam Cosmos Platform. Mọi quyền được bảo lưu.
        </p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '../../stores/authStore'

const authStore = useAuthStore()
authStore.syncFromStorage()

const starfieldCanvas = ref(null)

const primaryCta = computed(() => {
  if (authStore.isAuthenticated) {
    return { to: authStore.dashboardPath || '/select-role', label: 'Vào bảng điều khiển' }
  }
  return { to: '/register', label: 'Bắt đầu ngay' }
})

const stats = [
  {
    iconPath: '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
    value: '10,000+', label: 'Học sinh',
    bg: 'var(--cosmos-teal-soft)', color: 'var(--cosmos-teal)'
  },
  {
    iconPath: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/>',
    value: '5,000+', label: 'Kỳ thi',
    bg: 'var(--cosmos-rose-soft)', color: 'var(--cosmos-rose)'
  },
  {
    iconPath: '<path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>',
    value: '200+', label: 'Lớp học',
    bg: 'var(--cosmos-success-soft)', color: 'var(--cosmos-success)'
  },
  {
    iconPath: '<polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>',
    value: '98%', label: 'Hài lòng',
    bg: 'var(--cosmos-amber-soft)', color: 'var(--cosmos-amber)'
  }
]

const roles = [
  {
    iconPath: '<path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/>',
    title: 'Học sinh',
    desc: 'Làm bài thi tập trung với giao diện thiên nhiên, theo dõi tiến độ và xem kết quả ngay.',
    ctaLink: '/login',
    ctaText: 'Làm bài thi',
    bg: 'var(--cosmos-teal-soft)',
    color: 'var(--cosmos-teal)'
  },
  {
    iconPath: '<circle cx="12" cy="12" r="10"/><path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20M2 12h20"/>',
    title: 'Giáo viên',
    desc: 'Tạo và quản lý kỳ thi, theo dõi giám sát, xem báo cáo chi tiết về kết quả thi.',
    ctaLink: '/login',
    ctaText: 'Tạo kỳ thi',
    bg: 'var(--cosmos-rose-soft)',
    color: 'var(--cosmos-rose)'
  },
  {
    iconPath: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>',
    title: 'Quản trị',
    desc: 'Quản lý người dùng, cấu hình hệ thống, import câu hỏi và theo dõi hoạt động.',
    ctaLink: '/login',
    ctaText: 'Quản lý hệ thống',
    bg: 'var(--cosmos-success-soft)',
    color: 'var(--cosmos-success)'
  }
]

// Starfield animation
onMounted(() => {
  const canvas = starfieldCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  let w, h, stars

  const resize = () => {
    w = canvas.width = window.innerWidth
    h = canvas.height = window.innerHeight
    stars = Array.from({ length: 200 }, () => ({
      x: Math.random() * w,
      y: Math.random() * h,
      r: Math.random() * 1.5 + 0.5,
      speed: Math.random() * 0.3 + 0.1,
      opacity: Math.random()
    }))
  }

  resize()
  window.addEventListener('resize', resize)

  const animate = () => {
    ctx.clearRect(0, 0, w, h)
    stars.forEach(star => {
      star.opacity += (Math.random() - 0.5) * 0.05
      star.opacity = Math.max(0.1, Math.min(1, star.opacity))
      ctx.beginPath()
      ctx.arc(star.x, star.y, star.r, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(255,255,255,${star.opacity * 0.6})`
      ctx.fill()
    })
    requestAnimationFrame(animate)
  }
  animate()

  // Scroll reveal
  if (typeof IntersectionObserver !== 'undefined') {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-visible')
          }
        })
      },
      { threshold: 0.1 }
    )
    document.querySelectorAll('.cosmos-scroll-reveal').forEach((el) => observer.observe(el))
  }
})
</script>

<style scoped>
.landing-root {
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
}

.starfield-canvas {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.hero-section {
  position: relative;
  z-index: 1;
}

.brand-icon {
  background: linear-gradient(135deg, var(--cosmos-teal) 0%, var(--cosmos-rose) 100%);
  box-shadow: var(--cosmos-shadow-teal);
}

.brand-name {
  font-family: var(--cosmos-font-display);
  font-weight: 800;
  font-size: 1.25rem;
  letter-spacing: -0.03em;
  color: var(--cosmos-text);
}

.prism-decoration {
  animation: prismRotate 20s linear infinite;
  opacity: 0.4;
}

@keyframes prismRotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (prefers-reduced-motion: reduce) {
  .cosmos-reveal,
  .cosmos-scale,
  .cosmos-spring,
  .cosmos-lift,
  .cosmos-orb,
  .cosmos-tilt,
  .prism-decoration {
    opacity: 1 !important;
    transform: none !important;
    animation: none !important;
  }

  .cosmos-nav-link::before {
    transform: none !important;
  }

  .cosmos-orb {
    display: none;
  }
}
</style>
