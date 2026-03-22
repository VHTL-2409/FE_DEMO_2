<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen"
  >
    <div class="relative flex min-h-screen w-full flex-col overflow-x-hidden">
      <header
        class="sticky top-0 z-50 flex items-center justify-between gap-4 border-b border-primary/10 bg-white/85 dark:bg-background-dark/90 backdrop-blur-md px-4 sm:px-8 py-3"
      >
        <RouterLink to="/login" class="flex items-center gap-3 text-primary dark:text-slate-100 hover:opacity-90 transition-opacity">
          <div class="size-9 flex items-center justify-center bg-primary text-white rounded-lg shadow-lg shadow-primary/20">
            <span class="material-symbols-outlined text-xl">menu_book</span>
          </div>
          <div>
            <span class="text-base font-bold leading-tight">EduExam</span>
            <span class="hidden sm:block text-[10px] text-slate-500 dark:text-slate-400 font-medium uppercase tracking-wider">Help Center</span>
          </div>
        </RouterLink>
        <div class="flex items-center gap-2 sm:gap-3">
          <button
            type="button"
            class="p-2 rounded-lg text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800"
            :title="isDark ? 'Sáng' : 'Tối'"
            @click="isDark = !isDark"
          >
            <span class="material-symbols-outlined text-xl">{{ isDark ? 'light_mode' : 'dark_mode' }}</span>
          </button>
          <RouterLink
            to="/login"
            class="hidden sm:inline-flex items-center px-4 py-2 rounded-lg text-sm font-semibold text-slate-700 dark:text-slate-200 border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800/80 transition-colors"
          >
            Đăng nhập
          </RouterLink>
          <RouterLink
            to="/register"
            class="inline-flex items-center px-4 py-2 rounded-lg text-sm font-bold bg-primary text-white hover:bg-primary/90 transition-colors shadow-md shadow-primary/20"
          >
            Đăng ký
          </RouterLink>
        </div>
      </header>

      <main class="flex-1">
        <section class="relative px-4 sm:px-8 py-12 sm:py-16 lg:py-20 overflow-hidden">
          <div class="pointer-events-none absolute -top-24 -right-24 size-96 rounded-full bg-primary/10 blur-3xl" />
          <div class="pointer-events-none absolute bottom-0 left-1/4 size-80 rounded-full bg-indigo-500/5 blur-3xl" />
          <div class="relative max-w-3xl mx-auto text-center">
            <p class="text-primary font-semibold text-sm uppercase tracking-[0.2em] mb-3">Trợ giúp & hướng dẫn</p>
            <h1 class="text-3xl sm:text-4xl lg:text-5xl font-black tracking-tight text-slate-900 dark:text-white mb-4">
              Help Center
            </h1>
            <p class="text-slate-600 dark:text-slate-400 text-lg leading-relaxed">
              Tìm câu trả lời nhanh về đăng ký, làm bài thi, luyện tập và tài khoản giáo viên. Nếu chưa đủ, hãy liên hệ bộ phận hỗ trợ.
            </p>
          </div>
        </section>

        <section class="max-w-4xl mx-auto px-4 sm:px-8 pb-8">
          <div class="grid sm:grid-cols-3 gap-4 mb-12">
            <RouterLink
              to="/login"
              class="group rounded-2xl border border-slate-200/80 dark:border-slate-700/80 bg-white dark:bg-slate-900/50 p-5 text-left hover:border-primary/40 hover:shadow-lg hover:shadow-primary/5 transition-all duration-200"
            >
              <span class="material-symbols-outlined text-primary text-2xl mb-2">login</span>
              <h3 class="font-bold text-slate-900 dark:text-white mb-1">Đăng nhập</h3>
              <p class="text-sm text-slate-500 dark:text-slate-400">Quên mật khẩu, xác minh email</p>
            </RouterLink>
            <RouterLink
              to="/register"
              class="group rounded-2xl border border-slate-200/80 dark:border-slate-700/80 bg-white dark:bg-slate-900/50 p-5 text-left hover:border-primary/40 hover:shadow-lg hover:shadow-primary/5 transition-all duration-200"
            >
              <span class="material-symbols-outlined text-primary text-2xl mb-2">person_add</span>
              <h3 class="font-bold text-slate-900 dark:text-white mb-1">Tạo tài khoản</h3>
              <p class="text-sm text-slate-500 dark:text-slate-400">Đăng ký và chọn vai trò</p>
            </RouterLink>
            <a
              href="mailto:support@examportal.edu"
              class="group rounded-2xl border border-slate-200/80 dark:border-slate-700/80 bg-white dark:bg-slate-900/50 p-5 text-left hover:border-primary/40 hover:shadow-lg hover:shadow-primary/5 transition-all duration-200"
            >
              <span class="material-symbols-outlined text-primary text-2xl mb-2">mail</span>
              <h3 class="font-bold text-slate-900 dark:text-white mb-1">Liên hệ</h3>
              <p class="text-sm text-slate-500 dark:text-slate-400">support@examportal.edu</p>
            </a>
          </div>

          <h2 class="text-xl font-bold text-slate-900 dark:text-white mb-4 flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">quiz</span>
            Câu hỏi thường gặp
          </h2>
          <div class="space-y-3 mb-16">
            <details
              v-for="(item, idx) in faqs"
              :key="idx"
              class="group rounded-xl border border-slate-200/80 dark:border-slate-700/80 bg-white dark:bg-slate-900/40 open:shadow-md open:border-primary/20 transition-shadow"
            >
              <summary
                class="cursor-pointer list-none flex items-center justify-between gap-3 px-5 py-4 font-semibold text-slate-900 dark:text-white marker:content-none [&::-webkit-details-marker]:hidden"
              >
                <span>{{ item.q }}</span>
                <span class="material-symbols-outlined text-slate-400 group-open:rotate-180 transition-transform shrink-0">expand_more</span>
              </summary>
              <div class="px-5 pb-4 text-slate-600 dark:text-slate-400 text-sm leading-relaxed border-t border-slate-100 dark:border-slate-800/80 pt-3">
                {{ item.a }}
              </div>
            </details>
          </div>

          <div class="grid md:grid-cols-2 gap-6 mb-16">
            <div class="rounded-2xl border border-slate-200/80 dark:border-slate-700/80 bg-gradient-to-br from-primary/[0.06] to-transparent dark:from-primary/10 p-6 sm:p-8">
              <h3 class="font-bold text-lg text-slate-900 dark:text-white mb-3 flex items-center gap-2">
                <span class="material-symbols-outlined text-primary">school</span>
                Dành cho học sinh
              </h3>
              <ul class="space-y-2 text-sm text-slate-600 dark:text-slate-400">
                <li class="flex gap-2"><span class="text-primary font-bold">•</span> Đăng nhập → <strong class="text-slate-800 dark:text-slate-200">Thi qua mã</strong> để nhập mã đề hoặc tìm theo tiêu đề.</li>
                <li class="flex gap-2"><span class="text-primary font-bold">•</span> Vào phòng chờ đúng giờ; khi được phép, màn hình làm bài sẽ mở.</li>
                <li class="flex gap-2"><span class="text-primary font-bold">•</span> <strong class="text-slate-800 dark:text-slate-200">Luyện tập</strong> tạo đề nhanh để ôn tập, không ảnh hưởng điểm chính thức.</li>
                <li class="flex gap-2"><span class="text-primary font-bold">•</span> Xem <strong class="text-slate-800 dark:text-slate-200">Lịch sử / Kết quả</strong> sau khi nộp bài.</li>
              </ul>
            </div>
            <div class="rounded-2xl border border-slate-200/80 dark:border-slate-700/80 bg-gradient-to-br from-emerald-500/[0.06] to-transparent dark:from-emerald-500/10 p-6 sm:p-8">
              <h3 class="font-bold text-lg text-slate-900 dark:text-white mb-3 flex items-center gap-2">
                <span class="material-symbols-outlined text-emerald-600 dark:text-emerald-400">co_present</span>
                Dành cho giáo viên
              </h3>
              <ul class="space-y-2 text-sm text-slate-600 dark:text-slate-400">
                <li class="flex gap-2"><span class="text-emerald-600 font-bold">•</span> <strong class="text-slate-800 dark:text-slate-200">Tạo đề thi</strong>: nhập thông tin, thời gian, mã đề và câu hỏi (hoặc import).</li>
                <li class="flex gap-2"><span class="text-emerald-600 font-bold">•</span> <strong class="text-slate-800 dark:text-slate-200">Giám sát</strong> theo dõi phiên thi, cảnh báo vi phạm.</li>
                <li class="flex gap-2"><span class="text-emerald-600 font-bold">•</span> Sau kỳ thi: xem báo cáo, tổng kết theo lớp/đề.</li>
              </ul>
            </div>
          </div>

          <div class="rounded-2xl border border-dashed border-slate-300 dark:border-slate-600 bg-slate-50/80 dark:bg-slate-900/30 px-6 py-8 text-center">
            <p class="text-slate-600 dark:text-slate-400 text-sm mb-4">
              Không tìm thấy nội dung bạn cần? Gửi email mô tả vấn đề (kèm tài khoản nếu liên quan) — chúng tôi phản hồi trong giờ hành chính.
            </p>
            <a
              href="mailto:support@examportal.edu?subject=EduExam%20-%20Hỗ%20trợ"
              class="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-primary text-white font-bold text-sm hover:bg-primary/90 transition-colors shadow-lg shadow-primary/25"
            >
              <span class="material-symbols-outlined text-lg">outgoing_mail</span>
              Gửi yêu cầu hỗ trợ
            </a>
          </div>
        </section>
      </main>

      <footer class="border-t border-slate-200/80 dark:border-slate-800 py-8 px-4 text-center text-slate-500 dark:text-slate-500 text-xs sm:text-sm">
        <RouterLink to="/login" class="text-primary font-semibold hover:underline">Đăng nhập</RouterLink>
        <span class="mx-2">·</span>
        <RouterLink to="/register" class="text-primary font-semibold hover:underline">Đăng ký</RouterLink>
        <span class="mx-2">·</span>
        <RouterLink to="/forgot-password" class="hover:underline">Quên mật khẩu</RouterLink>
        <p class="mt-4">© {{ year }} EduExam Online Examination System</p>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const isDark = ref(false)
const year = computed(() => new Date().getFullYear())

const faqs = [
  {
    q: 'Làm sao để tham gia bài thi?',
    a: 'Đăng nhập bằng tài khoản học sinh → mục «Thi qua mã» → nhập mã đề hoặc tìm theo tiêu đề. Làm theo hướng dẫn vào phòng chờ; khi giờ thi đến, hệ thống cho phép vào làm bài.'
  },
  {
    q: 'Quên mật khẩu hoặc email chưa xác minh?',
    a: 'Tại màn đăng nhập, chọn «Quên mật khẩu» và làm theo email đặt lại mật khẩu. Nếu tài khoản mới chưa xác minh email, kiểm tra hộp thư (và thư mục spam) hoặc yêu cầu gửi lại link xác minh từ màn đăng nhập.'
  },
  {
    q: 'Giáo viên tạo đề và giám sát như thế nào?',
    a: 'Đăng nhập vai trò giáo viên → «Tạo đề thi» để cấu hình thời gian, độ dài và câu hỏi. Dùng «Giám sát» để theo dõi phiên thi trực tiếp và xử lý cảnh báo theo quy định trường.'
  },
  {
    q: 'Dữ liệu và quyền riêng tư?',
    a: 'Thông tin tài khoản và bài làm được dùng để vận hành kỳ thi và báo cáo trong hệ thống. Không chia sẻ mật khẩu; đăng xuất khi dùng máy chung.'
  }
]
</script>
