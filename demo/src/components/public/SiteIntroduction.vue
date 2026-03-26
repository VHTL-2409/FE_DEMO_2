<template>
  <div class="portal-viewport landing-page font-display text-slate-900">
    <div class="flex min-h-0 flex-1 flex-col overflow-x-hidden">
      <header class="border-b border-slate-200/80 bg-white px-4 py-3 sm:px-8">
        <div class="mx-auto flex max-w-7xl items-center justify-between gap-4">
          <RouterLink to="/" class="flex min-w-0 items-center gap-3 text-primary transition-opacity hover:opacity-90">
            <div class="flex size-11 items-center justify-center rounded-2xl bg-[linear-gradient(135deg,#6366f1_0%,#4f46e5_100%)] text-sm font-black text-white shadow-[0_14px_28px_-18px_rgba(79,70,229,0.45)]">
              E
            </div>
            <div class="min-w-0">
              <div class="truncate text-[1.08rem] font-black leading-tight tracking-[-0.02em] sm:text-[1.12rem]">EduExam Platform</div>
              <div class="hidden text-[0.7rem] font-semibold uppercase tracking-[0.18em] text-slate-500 md:block">
                Thi trực tuyến, giám sát tập trung, vận hành rõ ràng
              </div>
            </div>
          </RouterLink>

          <nav class="hidden items-center gap-6 text-[0.92rem] font-semibold text-slate-600 lg:flex">
            <a v-for="link in navLinks" :key="link.href" :href="link.href" class="whitespace-nowrap transition-colors hover:text-primary">
              {{ link.label }}
            </a>
            <RouterLink class="transition-colors hover:text-primary" to="/help-center">Trợ giúp</RouterLink>
          </nav>

          <div class="flex items-center gap-2 sm:gap-3">
            <RouterLink to="/login" class="landing-btn landing-btn-secondary hidden sm:inline-flex">Đăng nhập</RouterLink>
            <RouterLink :to="primaryCta.to" class="landing-btn landing-btn-primary">{{ primaryCta.label }}</RouterLink>
          </div>
        </div>
      </header>

      <main class="portal-main-scroll portal-scrollbar min-h-0 flex-1">
        <section id="overview" class="px-4 pb-12 pt-10 sm:px-8 lg:px-12 lg:pb-16 lg:pt-14">
          <div class="mx-auto grid max-w-7xl gap-10 lg:grid-cols-[minmax(0,0.88fr)_minmax(32rem,1.12fr)] lg:items-center">
            <div>
              <div class="flex flex-wrap gap-2">
                <span v-for="badge in heroBadges" :key="badge" class="landing-chip">{{ badge }}</span>
              </div>

              <h1 class="mt-6 max-w-4xl text-[clamp(2.45rem,4.8vw,4.85rem)] font-black leading-[0.95] tracking-[-0.05em] text-slate-950">
                Thi trực tuyến
                <span class="landing-accent-text">gọn, rõ</span>
                và dễ kiểm soát.
              </h1>

              <p class="mt-5 max-w-xl text-[1rem] leading-7 text-slate-600">
                Một nơi để tạo đề, giám sát kỳ thi, import câu hỏi và xem kết quả mà không phải chuyển qua nhiều màn hình rời rạc.
              </p>

              <div class="mt-8 flex flex-col gap-3 sm:flex-row">
                <RouterLink :to="primaryCta.to" class="landing-btn landing-btn-primary">
                  {{ primaryCta.label }}
                </RouterLink>
                <RouterLink to="/help-center" class="landing-btn landing-btn-secondary">
                  Xem hướng dẫn
                </RouterLink>
              </div>

              <div class="mt-8 grid gap-3 sm:grid-cols-3">
                <div v-for="stat in heroStats" :key="stat.label" class="landing-surface-card rounded-3xl px-5 py-4">
                  <div class="text-[1.68rem] font-black tracking-[-0.04em] text-slate-950">{{ stat.value }}</div>
                  <div class="mt-1 text-[0.9rem] font-semibold text-slate-700">{{ stat.label }}</div>
                </div>
              </div>
            </div>

            <div class="landing-frame rounded-[2rem] p-5 md:p-6">
              <div class="flex items-center justify-between border-b border-slate-200/80 pb-4">
                <div>
                  <div class="text-[0.72rem] font-bold uppercase tracking-[0.18em] text-primary">Product Snapshot</div>
                  <div class="mt-1 text-[1.42rem] font-black tracking-[-0.03em] text-slate-950">Một dashboard cho toàn bộ kỳ thi</div>
                </div>
                <div class="inline-flex items-center gap-2 rounded-full bg-emerald-50 px-3 py-1.5 text-[0.72rem] font-bold uppercase tracking-[0.14em] text-emerald-700">
                  <span class="size-2 rounded-full bg-emerald-500"></span>
                  Online
                </div>
              </div>

              <div class="mt-5 grid gap-4 xl:grid-cols-[1.04fr_0.96fr]">
                <section class="landing-exam-panel rounded-[1.75rem] p-5 text-white">
                  <div class="flex items-center justify-between gap-3">
                    <div>
                      <div class="text-[0.7rem] font-bold uppercase tracking-[0.18em] text-white/65">Student View</div>
                      <div class="mt-1 text-[1.08rem] font-black tracking-[-0.03em]">Màn hình làm bài tập trung</div>
                    </div>
                    <div class="rounded-full bg-white/12 px-3 py-1 text-[0.72rem] font-bold uppercase tracking-[0.14em] text-white/80">
                      Auto-save
                    </div>
                  </div>

                  <div class="mt-5 rounded-[1.35rem] border border-white/12 bg-white/10 p-4">
                    <div class="flex items-center justify-between text-[0.8rem] text-white/70">
                      <span>Toán rời rạc</span>
                      <span>34:18</span>
                    </div>
                    <div class="mt-4 grid grid-cols-5 gap-2">
                      <div
                        v-for="item in questionSlots"
                        :key="item.label"
                        class="flex h-10 items-center justify-center rounded-xl border text-[0.76rem] font-bold"
                        :class="item.tone"
                      >
                        {{ item.label }}
                      </div>
                    </div>
                    <div class="mt-5 rounded-[1.2rem] bg-slate-950/28 p-4">
                      <div class="text-[0.72rem] font-bold uppercase tracking-[0.18em] text-white/55">Câu hỏi 18 / 40</div>
                      <div class="mt-2 text-[0.96rem] font-bold leading-6 text-white">Nội dung câu hỏi hiển thị rõ ràng, không gây nhiễu.</div>
                      <div class="mt-4 space-y-2">
                        <div v-for="choice in answerChoices" :key="choice.label" class="flex items-center justify-between rounded-2xl border px-3 py-3 text-[0.84rem]" :class="choice.tone">
                          <span>{{ choice.label }}</span>
                          <span class="text-[0.72rem] font-bold uppercase tracking-[0.12em]">{{ choice.meta }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </section>

                <div class="space-y-4">
                  <section class="landing-surface-card rounded-[1.65rem] p-5">
                    <div class="flex items-center justify-between gap-3">
                      <div>
                        <div class="text-[0.7rem] font-bold uppercase tracking-[0.18em] text-primary">Proctor Grid</div>
                        <div class="mt-1 text-[1.04rem] font-black tracking-[-0.03em] text-slate-950">Risk realtime, đọc nhanh</div>
                      </div>
                      <span class="rounded-full bg-slate-100 px-3 py-1 text-[0.72rem] font-bold uppercase tracking-[0.14em] text-slate-600">
                        {{ liveAlerts.length }} cảnh báo
                      </span>
                    </div>

                    <div class="mt-4 space-y-3">
                      <div v-for="alert in liveAlerts" :key="alert.name" class="rounded-2xl border border-slate-200/80 bg-white px-4 py-3">
                        <div class="flex items-center justify-between gap-3">
                          <div>
                            <div class="text-[0.94rem] font-bold text-slate-950">{{ alert.name }}</div>
                            <div class="mt-0.5 text-[0.8rem] text-slate-500">{{ alert.event }}</div>
                          </div>
                          <span class="rounded-full px-2.5 py-1 text-[0.72rem] font-bold uppercase tracking-[0.12em]" :class="alert.tone">
                            {{ alert.score }}
                          </span>
                        </div>
                        <div class="mt-3 h-2 overflow-hidden rounded-full bg-slate-100">
                          <div class="h-full rounded-full bg-[linear-gradient(90deg,#818cf8_0%,#4f46e5_100%)]" :style="{ width: alert.progress }"></div>
                        </div>
                      </div>
                    </div>
                  </section>

                  <section class="landing-import-panel rounded-[1.65rem] p-5">
                    <div class="text-[0.7rem] font-bold uppercase tracking-[0.18em] text-primary">Import Review</div>
                    <div class="mt-1 text-[1.04rem] font-black tracking-[-0.03em] text-slate-950">Kiểm duyệt trước khi công bố</div>
                    <div class="mt-4 space-y-3">
                      <div v-for="step in importPreview" :key="step.title" class="flex items-start gap-3 rounded-2xl bg-white px-4 py-3">
                        <div class="mt-0.5 flex size-8 items-center justify-center rounded-full bg-primary text-[0.82rem] font-black text-white">{{ step.id }}</div>
                        <div>
                          <div class="text-[0.9rem] font-bold text-slate-950">{{ step.title }}</div>
                          <div class="mt-1 text-[0.82rem] leading-5 text-slate-600">{{ step.description }}</div>
                        </div>
                      </div>
                    </div>
                  </section>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section id="experience" class="px-4 py-10 sm:px-8 lg:px-12 lg:py-14">
          <div class="mx-auto max-w-7xl">
            <div class="max-w-3xl">
              <div class="text-[0.78rem] font-bold uppercase tracking-[0.18em] text-primary">3 góc nhìn chính</div>
              <h2 class="mt-3 text-[clamp(1.9rem,3.2vw,2.85rem)] font-black leading-[1.04] tracking-[-0.04em] text-slate-950">
                Ít mô tả hơn, nhìn vào là hiểu luồng sản phẩm.
              </h2>
            </div>

            <div class="mt-8 grid gap-4 xl:grid-cols-3">
              <article v-for="card in visualCards" :key="card.title" class="landing-surface-card rounded-[1.75rem] p-5">
                <div class="flex items-center justify-between">
                  <div class="text-[1.02rem] font-black tracking-[-0.03em] text-slate-950">{{ card.title }}</div>
                  <div class="text-[0.72rem] font-bold uppercase tracking-[0.14em]" :class="card.tagTone">{{ card.tag }}</div>
                </div>
                <p class="mt-2 text-[0.86rem] leading-6 text-slate-600">{{ card.copy }}</p>
                <div class="mt-4 rounded-[1.4rem] border border-slate-200 bg-slate-50 px-4 py-4">
                  <div class="grid gap-2" :class="card.layout">
                    <div v-for="block in card.blocks" :key="block.label" class="rounded-2xl border px-3 py-3 text-[0.8rem]" :class="block.tone">
                      <div class="font-bold">{{ block.label }}</div>
                      <div class="mt-1 text-slate-500">{{ block.meta }}</div>
                    </div>
                  </div>
                </div>
              </article>
            </div>
          </div>
        </section>

        <section class="px-4 py-12 sm:px-8 lg:px-12 lg:py-16">
          <div class="landing-cta-panel mx-auto max-w-7xl rounded-[2rem] px-6 py-8 text-white sm:px-10 sm:py-10">
            <div class="grid gap-6 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-center">
              <div>
                <div class="text-[0.78rem] font-bold uppercase tracking-[0.18em] text-primary-200">Sẵn sàng bắt đầu?</div>
                <h2 class="mt-3 text-[clamp(1.95rem,3vw,3rem)] font-black leading-[1.04] tracking-[-0.04em]">
                  Dùng EduExam để vận hành kỳ thi online gọn hơn, rõ hơn và nhẹ đầu hơn.
                </h2>
                <p class="mt-4 max-w-2xl text-[0.95rem] leading-7 text-slate-300">
                  Từ tạo đề, import file, làm bài đến giám sát và hậu kiểm, mọi bước đều nằm trong một luồng đồng nhất.
                </p>
              </div>

              <div class="flex flex-col gap-3 sm:flex-row lg:flex-col">
                <RouterLink :to="primaryCta.to" class="landing-btn landing-btn-primary">
                  {{ primaryCta.label }}
                </RouterLink>
                <RouterLink to="/help-center" class="landing-btn landing-btn-secondary">
                  Mở trung tâm trợ giúp
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
  { value: '500+', label: 'Phiên thi đồng thời' },
  { value: '3', label: 'Luồng chính' },
  { value: '1', label: 'Dashboard tập trung' }
]

const questionSlots = [
  { label: '01', tone: 'border-white/16 bg-white/8 text-white/65' },
  { label: '02', tone: 'border-white/16 bg-white/8 text-white/65' },
  { label: '03', tone: 'border-emerald-300/20 bg-emerald-400/14 text-emerald-100' },
  { label: '04', tone: 'border-amber-300/20 bg-amber-400/14 text-amber-100' },
  { label: '05', tone: 'border-primary-200/20 bg-primary-300/18 text-white' }
]

const answerChoices = [
  { label: 'A. O(log n)', meta: 'bỏ qua', tone: 'border-white/10 bg-white/6 text-white/72' },
  { label: 'B. O(n log n)', meta: 'đã chọn', tone: 'border-primary-200/20 bg-primary-300/24 text-white' },
  { label: 'C. O(n²)', meta: 'cần xem', tone: 'border-white/10 bg-white/6 text-white/72' }
]

const liveAlerts = [
  {
    name: 'Nguyễn Minh An',
    event: 'Risk 32 • tab switch • tiến độ 24/30',
    score: '32',
    progress: '38%',
    tone: 'bg-amber-50 text-amber-700'
  },
  {
    name: 'Trần Thảo Vy',
    event: 'Risk 12 • ổn định • tiến độ 30/30',
    score: '12',
    progress: '20%',
    tone: 'bg-emerald-50 text-emerald-700'
  },
  {
    name: 'Lê Gia Huy',
    event: 'Risk 68 • cần theo dõi • tiến độ 15/30',
    score: '68',
    progress: '76%',
    tone: 'bg-rose-50 text-rose-700'
  }
]

const importPreview = [
  {
    id: '01',
    title: 'Tải file đề',
    description: 'Nhận file và phân loại định dạng.'
  },
  {
    id: '02',
    title: 'Preview',
    description: 'Xem trước câu hỏi và issue.'
  },
  {
    id: '03',
    title: 'Confirm',
    description: 'Chỉ lưu sau khi đã duyệt.'
  }
]

const visualCards = [
  {
    title: 'Màn hình làm bài',
    tag: 'Student',
    tagTone: 'text-primary',
    copy: 'Ít nhiễu, dễ nhìn, rõ trạng thái từng câu.',
    layout: 'grid-cols-2',
    blocks: [
      { label: 'Sidebar', meta: 'đã làm / đánh dấu', tone: 'border-slate-200 bg-white text-slate-900' },
      { label: 'Timer', meta: 'màu theo ngưỡng', tone: 'border-slate-200 bg-white text-slate-900' },
      { label: 'Auto-save', meta: 'định kỳ + thủ công', tone: 'border-emerald-200 bg-emerald-50 text-slate-900' },
      { label: 'Submit', meta: 'summary trước khi nộp', tone: 'border-slate-200 bg-white text-slate-900' }
    ]
  },
  {
    title: 'Giám sát realtime',
    tag: 'Proctor',
    tagTone: 'text-amber-600',
    copy: 'Điểm rủi ro và cảnh báo được gom về một nơi.',
    layout: 'grid-cols-1',
    blocks: [
      { label: 'Risk score', meta: 'xanh / vàng / đỏ', tone: 'border-slate-200 bg-white text-slate-900' },
      { label: 'Timeline', meta: 'các mốc cần chú ý', tone: 'border-amber-200 bg-amber-50 text-slate-900' },
      { label: 'Bulk action', meta: 'cảnh báo / review', tone: 'border-slate-200 bg-white text-slate-900' }
    ]
  },
  {
    title: 'Import đề thi',
    tag: 'Import',
    tagTone: 'text-emerald-600',
    copy: 'Preview trước, lưu sau, đỡ nhập tay và đỡ sai hơn.',
    layout: 'grid-cols-1',
    blocks: [
      { label: 'Upload', meta: 'CSV / XLSX / PDF / DOCX', tone: 'border-slate-200 bg-white text-slate-900' },
      { label: 'Review', meta: 'issue + confidence', tone: 'border-rose-200 bg-rose-50 text-slate-900' },
      { label: 'Confirm', meta: 'publish sau khi duyệt', tone: 'border-emerald-200 bg-emerald-50 text-slate-900' }
    ]
  }
]
</script>

<style scoped>
.landing-page {
  background: linear-gradient(180deg, #f7f8fc 0%, #ffffff 34%, #f8fafc 100%);
}

.landing-chip {
  display: inline-flex;
  align-items: center;
  border-radius: 9999px;
  border: 1px solid rgba(99, 102, 241, 0.16);
  background: #ffffff;
  padding: 0.48rem 0.88rem;
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #4f46e5;
  box-shadow: 0 10px 24px -22px rgba(79, 70, 229, 0.18);
}

.landing-accent-text {
  background: linear-gradient(135deg, #7c3aed 0%, #4f46e5 58%, #6366f1 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.landing-frame,
.landing-surface-card {
  border: 1px solid rgba(226, 232, 240, 0.92);
  background: #ffffff;
  box-shadow: 0 24px 48px -42px rgba(15, 23, 42, 0.14);
}

.landing-exam-panel {
  background: linear-gradient(150deg, #2f2a70 0%, #4338ca 60%, #4f46e5 100%);
}

.landing-import-panel {
  border: 1px solid rgba(196, 181, 253, 0.34);
  background: linear-gradient(145deg, #eef2ff 0%, #ffffff 100%);
  box-shadow: 0 20px 40px -36px rgba(79, 70, 229, 0.18);
}

.landing-btn {
  display: inline-flex;
  min-height: 3rem;
  align-items: center;
  justify-content: center;
  border-radius: 1rem;
  padding: 0.8rem 1.25rem;
  font-size: 0.95rem;
  font-weight: 700;
  text-decoration: none;
  transition: transform 160ms ease, box-shadow 180ms ease, border-color 180ms ease, background-color 180ms ease;
}

.landing-btn:hover {
  transform: translateY(-1px);
}

.landing-btn-primary {
  border: 1px solid rgba(79, 70, 229, 0.18);
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: white;
  box-shadow: 0 18px 34px -24px rgba(79, 70, 229, 0.42);
}

.landing-btn-secondary {
  border: 1px solid rgba(226, 232, 240, 0.92);
  background: #ffffff;
  color: #0f172a;
}

.landing-cta-panel {
  background:
    radial-gradient(circle at bottom left, rgba(59, 130, 246, 0.16), transparent 22%),
    linear-gradient(145deg, #111827 0%, #1e1b4b 46%, #312e81 100%);
  box-shadow: 0 34px 72px -58px rgba(15, 23, 42, 0.46);
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
    background: linear-gradient(180deg, #f7f8fc 0%, #ffffff 30%, #f8fafc 100%);
  }
}
</style>
