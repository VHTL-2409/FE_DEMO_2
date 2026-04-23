<template>
  <div class="portal-viewport bg-background-light font-display text-slate-900">
    <div class="relative flex h-full min-h-0 flex-1 w-full min-w-0 flex-col overflow-x-hidden">
      <header
        class="z-50 flex shrink-0 items-center justify-between gap-4 border-b border-primary/10 bg-white/95 px-4 py-3 sm:px-8"
      >
        <RouterLink to="/" class="flex items-center gap-3 text-primary transition-opacity hover:opacity-90">
          <div class="size-9 flex items-center justify-center bg-primary text-white rounded-lg shadow-lg shadow-primary/20">
            <LucideIcon name="menu_book" />
          </div>
          <div>
            <span class="text-base font-bold leading-tight">EduExam</span>
            <span class="hidden text-[10px] font-medium uppercase tracking-wider text-slate-500 sm:block">Trung tâm trợ giúp</span>
          </div>
        </RouterLink>
        <div class="flex items-center gap-2 sm:gap-3">
          <RouterLink v-slot="{ navigate }" to="/login" custom>
            <BaseButton variant="secondary" size="sm" class="hidden sm:inline-flex" @click="navigate">
              Đăng nhập
            </BaseButton>
          </RouterLink>
          <RouterLink v-slot="{ navigate }" to="/register" custom>
            <BaseButton size="sm" @click="navigate">Đăng ký</BaseButton>
          </RouterLink>
        </div>
      </header>

      <div class="flex shrink-0 flex-wrap gap-2 border-b border-slate-200/80 bg-white/70 px-4 py-2 sm:px-8">
        <button
          v-for="tab in helpTabs"
          :key="tab.id"
          type="button"
          class="portal-focus rounded-lg px-3 py-1.5 text-xs font-bold transition-colors sm:text-sm"
          :class="
            activeHelpTab === tab.id
              ? 'bg-primary text-white shadow-md shadow-primary/20'
              : 'text-slate-600 hover:bg-slate-100'
          "
          @click="activeHelpTab = tab.id"
        >
          {{ tab.label }}
        </button>
      </div>

      <main class="portal-main-scroll portal-scrollbar min-h-0 flex-1">
        <section v-show="activeHelpTab === 'overview'" class="portal-shell-bg relative overflow-hidden px-4 py-6 sm:px-8 sm:py-8">
          <div class="relative w-full text-center">
            <p class="mb-2 text-sm font-semibold uppercase tracking-[0.2em] text-primary">Trợ giúp & hướng dẫn</p>
            <h1 class="mb-3 text-2xl font-black tracking-tight text-slate-900 dark:text-white sm:text-3xl lg:text-4xl">
              Trung tâm trợ giúp
            </h1>
          </div>
        </section>

        <section class="w-full px-4 pb-6 sm:px-8 sm:pb-8">
          <div v-show="activeHelpTab === 'overview'" class="mb-8 grid gap-4 sm:grid-cols-3">
            <RouterLink
              to="/login"
              class="group rounded-2xl border border-slate-200/80 bg-white p-5 text-left shadow-soft transition-all duration-200 hover:border-primary/40 hover:shadow-card-hover portal-focus dark:border-slate-700/80 dark:bg-slate-900/50"
            >
              <LucideIcon name="login" size="24" />
              <h3 class="font-bold text-slate-900 dark:text-white mb-1">Đăng nhập</h3>
            </RouterLink>
            <RouterLink
              to="/register"
              class="group rounded-2xl border border-slate-200/80 bg-white p-5 text-left shadow-soft transition-all duration-200 hover:border-primary/40 hover:shadow-card-hover portal-focus dark:border-slate-700/80 dark:bg-slate-900/50"
            >
              <LucideIcon name="person_add" size="24" />
              <h3 class="font-bold text-slate-900 dark:text-white mb-1">Tạo tài khoản</h3>
            </RouterLink>
            <a
              href="mailto:support@examportal.edu"
              class="group rounded-2xl border border-slate-200/80 bg-white p-5 text-left shadow-soft transition-all duration-200 hover:border-primary/40 hover:shadow-card-hover portal-focus dark:border-slate-700/80 dark:bg-slate-900/50"
            >
              <LucideIcon name="mail" size="24" />
              <h3 class="font-bold text-slate-900 dark:text-white mb-1">Liên hệ</h3>
              <p class="text-sm text-slate-500 dark:text-slate-400">support@examportal.edu</p>
            </a>
          </div>

          <div v-show="activeHelpTab === 'faq'" class="mb-8">
            <h2 class="mb-4 flex items-center gap-2 text-xl font-bold text-slate-900 dark:text-white">
              <LucideIcon name="quiz" />
              Câu hỏi thường gặp
            </h2>
            <div class="space-y-3">
              <details
                v-for="(item, idx) in faqs"
                :key="idx"
                class="group rounded-xl border border-slate-200/80 bg-white transition-shadow open:border-primary/20 open:shadow-md dark:border-slate-700/80 dark:bg-slate-900/40"
              >
                <summary
                  class="flex cursor-pointer list-none items-center justify-between gap-3 px-5 py-4 font-semibold text-slate-900 marker:content-none dark:text-white [&::-webkit-details-marker]:hidden"
                >
                  <span>{{ item.q }}</span>
                  <LucideIcon name="expand_more" />
                </summary>
                <div class="border-t border-slate-100 px-5 pb-4 pt-3 text-sm leading-relaxed text-slate-600 dark:border-slate-800/80 dark:text-slate-400">
                  {{ item.a }}
                </div>
              </details>
            </div>
          </div>

          <div v-show="activeHelpTab === 'student'" class="mb-8">
            <div class="rounded-2xl border border-slate-200/80 bg-gradient-to-br from-primary/[0.06] to-transparent p-6 dark:border-slate-700/80 dark:from-primary/10 sm:p-8">
              <h3 class="mb-3 flex items-center gap-2 text-lg font-bold text-slate-900 dark:text-white">
                <LucideIcon name="school" />
                Dành cho học sinh
              </h3>
              <ul class="space-y-2 text-sm text-slate-600 dark:text-slate-400">
                <li class="flex gap-2"><span class="font-bold text-primary">•</span> Đăng nhập → <strong class="text-slate-800 dark:text-slate-200">Thi qua mã</strong> để nhập mã đề hoặc tìm theo tiêu đề.</li>
                <li class="flex gap-2"><span class="font-bold text-primary">•</span> Vào phòng chờ đúng giờ; khi được phép, màn hình làm bài sẽ mở.</li>
                <li class="flex gap-2"><span class="font-bold text-primary">•</span> <strong class="text-slate-800 dark:text-slate-200">Luyện tập</strong> tạo đề nhanh để ôn tập, không ảnh hưởng điểm chính thức.</li>
                <li class="flex gap-2"><span class="font-bold text-primary">•</span> Xem <strong class="text-slate-800 dark:text-slate-200">Lịch sử / Kết quả</strong> sau khi nộp bài.</li>
              </ul>
            </div>
          </div>

          <div v-show="activeHelpTab === 'teacher'" class="mb-8">
            <div class="rounded-2xl border border-slate-200/80 bg-gradient-to-br from-emerald-500/[0.06] to-transparent p-6 dark:border-slate-700/80 dark:from-emerald-500/10 sm:p-8">
              <h3 class="mb-3 flex items-center gap-2 text-lg font-bold text-slate-900 dark:text-white">
                <LucideIcon name="co_present" />
                Dành cho giáo viên
              </h3>
              <ul class="space-y-2 text-sm text-slate-600 dark:text-slate-400">
                <li class="flex gap-2"><span class="font-bold text-emerald-600">•</span> <strong class="text-slate-800 dark:text-slate-200">Tạo đề thi</strong>: nhập thông tin, thời gian, mã đề và câu hỏi (hoặc import).</li>
                <li class="flex gap-2"><span class="font-bold text-emerald-600">•</span> <strong class="text-slate-800 dark:text-slate-200">Giám sát</strong> theo dõi phiên thi, cảnh báo vi phạm.</li>
                <li class="flex gap-2"><span class="font-bold text-emerald-600">•</span> Sau kỳ thi: xem báo cáo, tổng kết theo lớp/đề.</li>
              </ul>
            </div>
          </div>

          <div v-show="activeHelpTab === 'overview'" class="rounded-2xl border border-dashed border-slate-300 bg-slate-50/80 px-6 py-6 text-center dark:border-slate-600 dark:bg-slate-900/30 sm:py-8">
            <a
              href="mailto:support@examportal.edu?subject=EduExam%20-%20Hỗ%20trợ"
              class="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-primary text-white font-bold text-sm hover:bg-primary/90 transition-colors shadow-lg shadow-primary/25"
            >
              <LucideIcon name="outgoing_mail" size="18" />
              Gửi yêu cầu hỗ trợ
            </a>
          </div>
        </section>
      </main>

      <footer class="shrink-0 border-t border-slate-200/80 px-4 py-3 text-center text-xs text-slate-500 sm:text-sm">
        <RouterLink to="/login" class="font-semibold text-primary hover:underline">Đăng nhập</RouterLink>
        <span class="mx-2 text-slate-300">·</span>
        <RouterLink to="/register" class="font-semibold text-primary hover:underline">Đăng ký</RouterLink>
        <span class="mx-2 text-slate-300">·</span>
        <RouterLink to="/forgot-password" class="text-slate-600 hover:text-primary hover:underline">Quên mật khẩu</RouterLink>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import BaseButton from '../shared/BaseButton.vue'

const helpTabs = [
  { id: 'overview', label: 'Tổng quan' },
  { id: 'student', label: 'Học sinh' },
  { id: 'teacher', label: 'Giáo viên' },
  { id: 'faq', label: 'FAQ' }
]
const activeHelpTab = ref('overview')

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
