<template>
  <div class="font-display text-slate-900 bg-white min-h-screen">
    <header class="border-b border-slate-200 bg-white px-4 py-3 sm:px-8">
      <div class="mx-auto flex max-w-7xl items-center justify-between">
        <RouterLink to="/" class="flex items-center gap-3">
          <div class="size-11 flex items-center justify-center rounded-xl bg-indigo-600 text-white font-black text-lg">E</div>
          <div>
            <div class="text-lg font-bold">EduExam Platform</div>
            <div class="hidden text-xs text-slate-500 sm:block">Thi trực tuyến, giám sát tập trung</div>
          </div>
        </RouterLink>

        <nav class="hidden items-center gap-6 text-sm font-medium text-slate-600 lg:flex">
          <a href="#overview" class="hover:text-indigo-600">Tổng quan</a>
          <a href="#experience" class="hover:text-indigo-600">Trải nghiệm</a>
          <RouterLink to="/help-center" class="hover:text-indigo-600">Trợ giúp</RouterLink>
        </nav>

        <div class="flex items-center gap-3">
          <RouterLink to="/login" class="hidden px-4 py-2 text-sm font-semibold border border-slate-300 rounded-lg sm:block hover:bg-slate-50">Đăng nhập</RouterLink>
          <RouterLink :to="primaryCta.to" class="px-4 py-2 text-sm font-semibold text-white bg-indigo-600 rounded-lg hover:bg-indigo-700">{{ primaryCta.label }}</RouterLink>
        </div>
      </div>
    </header>

    <main class="min-h-[calc(100vh-64px)]">
      <!-- Hero -->
      <section id="overview" class="px-4 py-12 sm:px-8 lg:px-12 lg:py-16">
        <div class="mx-auto max-w-7xl grid lg:grid-cols-2 gap-10 items-center">
          <div>
            <div class="flex flex-wrap gap-2 mb-6">
              <span v-for="badge in heroBadges" :key="badge" class="px-3 py-1 text-xs font-bold text-indigo-600 bg-indigo-50 rounded-full border border-indigo-100">{{ badge }}</span>
            </div>

            <h1 class="text-4xl sm:text-5xl font-black text-slate-900 leading-tight">
              Thi trực tuyến <span class="text-transparent bg-clip-text bg-gradient-to-r from-purple-600 to-indigo-600">gọn, rõ</span> và dễ kiểm soát.
            </h1>

            <p class="mt-4 text-slate-600 leading-relaxed">Một nơi để tạo đề, giám sát kỳ thi, import câu hỏi và xem kết quả mà không phải chuyển qua nhiều màn hình rời rạc.</p>

            <div class="mt-6 flex flex-wrap gap-3">
              <RouterLink :to="primaryCta.to" class="px-6 py-3 text-base font-semibold text-white bg-indigo-600 rounded-xl hover:bg-indigo-700">{{ primaryCta.label }}</RouterLink>
              <RouterLink to="/help-center" class="px-6 py-3 text-base font-semibold border border-slate-300 rounded-xl hover:bg-slate-50">Xem hướng dẫn</RouterLink>
            </div>

            <div class="mt-8 grid grid-cols-3 gap-4">
              <div v-for="stat in heroStats" :key="stat.label" class="p-4 bg-slate-50 rounded-xl border border-slate-200">
                <div class="text-2xl font-black text-slate-900">{{ stat.value }}</div>
                <div class="text-sm text-slate-600">{{ stat.label }}</div>
              </div>
            </div>
          </div>

          <!-- Snapshot -->
          <div class="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm">
            <div class="flex items-center justify-between pb-4 border-b border-slate-200">
              <div>
                <div class="text-xs font-bold text-indigo-600 uppercase tracking-wider">Product Snapshot</div>
                <div class="text-xl font-bold text-slate-900">Một dashboard cho toàn bộ kỳ thi</div>
              </div>
              <span class="px-2 py-1 text-xs font-bold text-emerald-700 bg-emerald-50 rounded-full">Online</span>
            </div>

            <div class="mt-4 grid gap-4">
              <!-- Student Panel -->
              <div class="p-4 text-white bg-gradient-to-br from-indigo-900 to-purple-800 rounded-xl">
                <div class="flex justify-between items-start mb-4">
                  <div>
                    <div class="text-xs text-white/60 uppercase tracking-wider">Giao diện Học sinh</div>
                    <div class="font-bold">Màn hình làm bài tập trung</div>
                  </div>
                  <span class="px-2 py-1 text-xs bg-white/10 rounded-full">Auto-save</span>
                </div>
                <div class="p-3 bg-white/10 rounded-lg">
                  <div class="flex justify-between text-sm text-white/70 mb-3">
                    <span>Toán rời rạc</span>
                    <span>34:18</span>
                  </div>
                  <div class="grid grid-cols-5 gap-2 mb-3">
                    <div v-for="q in [1,2,3,4,5]" :key="q" class="h-8 flex items-center justify-center rounded-lg border text-xs font-bold" :class="q === 3 ? 'bg-emerald-400/20 border-emerald-300/40 text-emerald-200' : 'bg-white/10 border-white/20 text-white/60'">{{ String(q).padStart(2, '0') }}</div>
                  </div>
                  <div class="p-3 bg-black/20 rounded-lg">
                    <div class="text-xs text-white/50 uppercase tracking-wider mb-1">Câu hỏi 18 / 40</div>
                    <div class="text-sm font-bold mb-3">Nội dung câu hỏi hiển thị rõ ràng.</div>
                    <div class="space-y-2">
                      <div class="flex justify-between items-center p-2 bg-white/10 rounded-lg text-xs"><span>A. O(log n)</span><span class="text-white/60">bỏ qua</span></div>
                      <div class="flex justify-between items-center p-2 bg-indigo-400/30 border border-indigo-300/40 rounded-lg text-xs"><span>B. O(n log n)</span><span class="text-white/80">đã chọn</span></div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Monitoring -->
              <div class="p-4 bg-slate-50 rounded-xl border border-slate-200">
                <div class="flex justify-between items-start mb-3">
                  <div>
                    <div class="text-xs font-bold text-indigo-600 uppercase tracking-wider">Lưới Giám sát</div>
                    <div class="font-bold text-slate-900">Risk realtime, đọc nhanh</div>
                  </div>
                  <span class="px-2 py-1 text-xs font-bold bg-slate-100 rounded-full">3 cảnh báo</span>
                </div>
                <div class="space-y-2">
                  <div v-for="alert in liveAlerts" :key="alert.name" class="p-3 bg-white rounded-lg border border-slate-200">
                    <div class="flex justify-between items-center">
                      <div>
                        <div class="font-bold text-slate-900 text-sm">{{ alert.name }}</div>
                        <div class="text-xs text-slate-500">{{ alert.event }}</div>
                      </div>
                      <span class="px-2 py-1 text-xs font-bold rounded-full" :class="alert.tone">{{ alert.score }}</span>
                    </div>
                    <div class="mt-2 h-1.5 bg-slate-100 rounded-full overflow-hidden">
                      <div class="h-full bg-indigo-500 rounded-full" :style="{ width: alert.progress }"></div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Import -->
              <div class="p-4 bg-gradient-to-br from-indigo-50 to-white rounded-xl border border-indigo-100">
                <div class="text-xs font-bold text-indigo-600 uppercase tracking-wider mb-1">Import Review</div>
                <div class="font-bold text-slate-900 mb-3">Kiểm duyệt trước khi công bố</div>
                <div class="space-y-2">
                  <div v-for="step in importSteps" :key="step.id" class="flex items-center gap-3 p-2 bg-white rounded-lg">
                    <span class="size-7 flex items-center justify-center bg-indigo-600 text-white text-xs font-bold rounded-full">{{ step.id }}</span>
                    <div>
                      <div class="font-bold text-sm text-slate-900">{{ step.title }}</div>
                      <div class="text-xs text-slate-500">{{ step.desc }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Features -->
      <section id="experience" class="px-4 py-12 sm:px-8 lg:px-12 bg-slate-50">
        <div class="mx-auto max-w-7xl">
          <div class="max-w-2xl mb-8">
            <div class="text-xs font-bold text-indigo-600 uppercase tracking-wider">3 góc nhìn chính</div>
            <h2 class="text-2xl sm:text-3xl font-black text-slate-900 mt-2">Ít mô tả hơn, nhìn vào là hiểu luồng sản phẩm.</h2>
          </div>

          <div class="grid md:grid-cols-3 gap-4">
            <div v-for="card in featureCards" :key="card.title" class="p-5 bg-white rounded-xl border border-slate-200">
              <div class="flex justify-between items-center mb-2">
                <div class="font-bold text-slate-900">{{ card.title }}</div>
                <span class="text-xs font-bold" :class="card.tagColor">{{ card.tag }}</span>
              </div>
              <p class="text-sm text-slate-600 mb-4">{{ card.copy }}</p>
              <div class="space-y-2">
                <div v-for="block in card.blocks" :key="block.label" class="p-2 rounded-lg border text-sm" :class="block.tone">
                  <span class="font-bold">{{ block.label }}</span>
                  <span class="text-slate-500 text-xs ml-2">{{ block.meta }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- CTA -->
      <section class="px-4 py-12 sm:px-8 lg:px-12">
        <div class="mx-auto max-w-5xl p-8 sm:p-12 bg-gradient-to-br from-slate-900 to-indigo-900 rounded-2xl text-white">
          <div class="grid md:grid-cols-2 gap-8 items-center">
            <div>
              <div class="text-xs font-bold text-indigo-300 uppercase tracking-wider">Sẵn sàng bắt đầu?</div>
              <h2 class="text-2xl sm:text-3xl font-black mt-2">Dùng EduExam để vận hành kỳ thi online gọn hơn, rõ hơn và nhẹ đầu hơn.</h2>
              <p class="mt-3 text-slate-300">Từ tạo đề, import file, làm bài đến giám sát và hậu kiểm, mọi bước đều nằm trong một luồng đồng nhất.</p>
            </div>
            <div class="flex flex-col gap-3">
              <RouterLink :to="primaryCta.to" class="px-6 py-3 text-base font-semibold text-center text-indigo-900 bg-white rounded-xl hover:bg-slate-100">{{ primaryCta.label }}</RouterLink>
              <RouterLink to="/help-center" class="px-6 py-3 text-base font-semibold text-center border border-white/30 rounded-xl hover:bg-white/10">Mở trung tâm trợ giúp</RouterLink>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '../../stores/authStore'

const authStore = useAuthStore()
authStore.syncFromStorage()

const primaryCta = computed(() => authStore.isAuthenticated ? { to: authStore.dashboardPath || '/select-role', label: 'Vào bảng điều khiển' } : { to: '/register', label: 'Bắt đầu ngay' })

const heroBadges = ['Nền tảng thi trực tuyến', 'Giám sát thời gian thực', 'Sẵn sàng Import']
const heroStats = [
  { value: '500+', label: 'Phiên thi đồng thời' },
  { value: '3', label: 'Luồng chính' },
  { value: '1', label: 'Dashboard tập trung' }
]

const liveAlerts = [
  { name: 'Nguyễn Minh An', event: 'Risk 32 • tab switch', score: '32', progress: '38%', tone: 'bg-amber-100 text-amber-700' },
  { name: 'Trần Thảo Vy', event: 'Risk 12 • ổn định', score: '12', progress: '20%', tone: 'bg-emerald-100 text-emerald-700' },
  { name: 'Lê Gia Huy', event: 'Risk 68 • cần theo dõi', score: '68', progress: '76%', tone: 'bg-rose-100 text-rose-700' }
]

const importSteps = [
  { id: '1', title: 'Tải file đề', desc: 'Nhận file và phân loại định dạng.' },
  { id: '2', title: 'Xem trước', desc: 'Xem trước câu hỏi và issue.' },
  { id: '3', title: 'Xác nhận', desc: 'Chỉ lưu sau khi đã duyệt.' }
]

const featureCards = [
  {
    title: 'Màn hình làm bài',
    tag: 'Học sinh',
    tagColor: 'text-indigo-600',
    copy: 'Ít nhiễu, dễ nhìn, rõ trạng thái từng câu.',
    blocks: [
      { label: 'Sidebar', meta: 'đã làm / đánh dấu', tone: 'bg-white border-slate-200' },
      { label: 'Timer', meta: 'màu theo ngưỡng', tone: 'bg-white border-slate-200' },
      { label: 'Auto-save', meta: 'định kỳ + thủ công', tone: 'bg-emerald-50 border-emerald-200' },
      { label: 'Nộp bài', meta: 'summary trước khi nộp', tone: 'bg-white border-slate-200' }
    ]
  },
  {
    title: 'Giám sát realtime',
    tag: 'Giám thị',
    tagColor: 'text-amber-600',
    copy: 'Điểm rủi ro và cảnh báo được gom về một nơi.',
    blocks: [
      { label: 'Risk score', meta: 'xanh / vàng / đỏ', tone: 'bg-white border-slate-200' },
      { label: 'Timeline', meta: 'các mốc cần chú ý', tone: 'bg-amber-50 border-amber-200' },
      { label: 'Bulk action', meta: 'cảnh báo / review', tone: 'bg-white border-slate-200' }
    ]
  },
  {
    title: 'Import đề thi',
    tag: 'Import',
    tagColor: 'text-emerald-600',
    copy: 'Preview trước, lưu sau, đỡ nhập tay và đỡ sai hơn.',
    blocks: [
      { label: 'Upload', meta: 'CSV / XLSX / PDF', tone: 'bg-white border-slate-200' },
      { label: 'Review', meta: 'issue + confidence', tone: 'bg-rose-50 border-rose-200' },
      { label: 'Xác nhận', meta: 'publish sau khi duyệt', tone: 'bg-emerald-50 border-emerald-200' }
    ]
  }
]
</script>
