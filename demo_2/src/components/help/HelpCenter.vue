<template>
  <div
    class="portal-viewport bg-[#faf9f5] font-display text-slate-900 dark:bg-slate-950 dark:text-slate-100"
    :class="{ 'teacher-portal-stitch': fromTeacher }"
  >
    <div class="relative flex h-full min-h-0 flex-1 w-full min-w-0 flex-col overflow-x-hidden">
      <header
        class="help-center-glass-header sticky top-0 z-50 flex shrink-0 items-center justify-between gap-4 border-b border-[#dbc2b0]/30 bg-[#faf9f5]/85 px-4 py-3 backdrop-blur-xl sm:px-8 dark:border-slate-700/50 dark:bg-slate-950/85"
      >
        <RouterLink to="/" class="flex items-center gap-3 text-primary transition-opacity hover:opacity-90">
          <div class="flex size-9 items-center justify-center rounded-lg bg-primary text-white shadow-lg shadow-primary/25">
            <span class="material-symbols-outlined text-xl">menu_book</span>
          </div>
          <div>
            <span class="text-base font-bold leading-tight">EduExam</span>
            <span class="hidden text-[10px] font-medium uppercase tracking-wider text-slate-500 sm:block">Support Workspace</span>
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

      <div
        class="flex shrink-0 flex-wrap gap-2 border-b border-[#dbc2b0]/25 bg-[#faf9f5]/70 px-4 py-2 backdrop-blur-md sm:px-8 dark:border-slate-700/50 dark:bg-slate-900/60"
      >
        <button
          v-for="tab in helpTabs"
          :key="tab.id"
          type="button"
          class="portal-focus rounded-full px-3.5 py-1.5 text-xs font-bold transition-colors sm:text-sm"
          :class="activeHelpTab === tab.id ? 'bg-primary text-white shadow-md shadow-primary/20' : 'bg-white/72 text-slate-600 hover:bg-white'"
          @click="activeHelpTab = tab.id"
        >
          {{ tab.label }}
        </button>
      </div>

      <main class="portal-main-scroll portal-scrollbar min-h-0 flex-1">
        <section class="portal-shell-bg relative overflow-hidden px-4 py-6 sm:px-8 sm:py-8">
          <div class="relative mx-auto flex max-w-5xl flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p class="portal-kicker mb-2">Knowledge Base & Support</p>
              <h1
                class="text-slate-900 sm:text-3xl lg:text-4xl"
                :class="fromTeacher ? 'stitch-font-headline font-bold tracking-tight text-amber-900 dark:text-amber-100' : 'portal-heading'"
              >
                Workspace hỗ trợ theo vai trò
              </h1>
              <p class="portal-subtitle mt-3 max-w-2xl text-sm sm:text-base">
                Chọn vai trò, mở checklist trước thi, xem playbook xử lý sự cố và gửi yêu cầu hỗ trợ với ngữ cảnh đã điền sẵn.
              </p>
            </div>
            <div class="flex flex-wrap gap-2">
              <span class="portal-chip rounded-full px-3 py-1.5 text-xs font-semibold text-primary">Role-based guides</span>
              <span class="portal-chip rounded-full px-3 py-1.5 text-xs font-semibold text-slate-600">Incident playbooks</span>
              <span class="portal-chip rounded-full px-3 py-1.5 text-xs font-semibold text-slate-600">Support composer</span>
            </div>
          </div>
        </section>

        <section class="w-full px-4 pb-6 sm:px-8 sm:pb-8">
          <div v-show="activeHelpTab === 'overview'" class="space-y-6">
            <div class="grid gap-4 sm:grid-cols-3">
              <RouterLink to="/login" class="portal-panel portal-card-lift group rounded-2xl p-5 text-left portal-focus">
                <span class="material-symbols-outlined mb-2 text-2xl text-primary">login</span>
                <h3 class="font-bold text-slate-900 dark:text-white">Đăng nhập</h3>
                <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Đi vào đúng khu vực tài khoản để bắt đầu sử dụng.</p>
              </RouterLink>
              <RouterLink to="/register" class="portal-panel portal-card-lift group rounded-2xl p-5 text-left portal-focus">
                <span class="material-symbols-outlined mb-2 text-2xl text-primary">person_add</span>
                <h3 class="font-bold text-slate-900 dark:text-white">Tạo tài khoản</h3>
                <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Đăng ký và kích hoạt email trước khi tham gia thi hoặc quản trị.</p>
              </RouterLink>
              <a :href="supportMailtoHref" class="portal-panel portal-card-lift group rounded-2xl p-5 text-left portal-focus">
                <span class="material-symbols-outlined mb-2 text-2xl text-primary">support_agent</span>
                <h3 class="font-bold text-slate-900 dark:text-white">Gửi hỗ trợ</h3>
                <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">{{ supportEmail }}</p>
              </a>
            </div>

            <div class="grid gap-4 lg:grid-cols-[1.08fr_0.92fr]">
              <div class="portal-panel rounded-2xl p-5">
                <div class="mb-4 flex items-center justify-between gap-3">
                  <div>
                    <p class="text-xs font-bold uppercase tracking-[0.18em] text-primary">Điểm vào nhanh</p>
                    <h2 class="mt-1 text-xl font-black tracking-tight text-slate-900 dark:text-white">Sự cố phổ biến</h2>
                  </div>
                </div>
                <div class="grid gap-3">
                  <button
                    v-for="scenario in issueScenarios"
                    :key="scenario.id"
                    type="button"
                    class="rounded-2xl border px-4 py-4 text-left transition-all"
                    :class="selectedScenario.id === scenario.id ? 'border-primary bg-primary/5' : 'border-slate-200/80 bg-slate-50/80 hover:border-primary/40 dark:border-slate-700/80 dark:bg-slate-800/40'"
                    @click="selectedScenarioId = scenario.id"
                  >
                    <div class="flex items-start justify-between gap-3">
                      <div>
                        <p class="text-sm font-semibold text-slate-900 dark:text-white">{{ scenario.title }}</p>
                        <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">{{ scenario.description }}</p>
                      </div>
                      <span class="rounded-full px-2.5 py-1 text-[11px] font-bold" :class="scenario.role === 'teacher' ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300' : 'bg-primary-100 text-primary-800 dark:bg-primary-900/30 dark:text-primary-300'">
                        {{ scenario.roleLabel }}
                      </span>
                    </div>
                  </button>
                </div>
              </div>

              <div class="portal-panel rounded-2xl p-5">
                <p class="text-xs font-bold uppercase tracking-[0.18em] text-primary">Support Composer</p>
                <h2 class="mt-1 text-xl font-black tracking-tight text-slate-900 dark:text-white">{{ selectedScenario.title }}</h2>
                <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">{{ selectedScenario.description }}</p>

                <div class="portal-panel-soft mt-4 rounded-2xl p-4">
                  <p class="text-sm font-semibold text-slate-900 dark:text-white">Checklist nên thử trước khi gửi ticket</p>
                  <ul class="mt-3 space-y-2 text-sm text-slate-600 dark:text-slate-300">
                    <li v-for="step in selectedScenario.checklist" :key="step" class="flex gap-2">
                      <span class="material-symbols-outlined text-base text-primary">check_circle</span>
                      {{ step }}
                    </li>
                  </ul>
                </div>

                <div class="mt-4 flex flex-wrap gap-2 text-xs font-semibold">
                  <span v-for="chip in supportDiagnostics" :key="chip" class="rounded-full bg-slate-100 px-3 py-1.5 text-slate-600 dark:bg-slate-800 dark:text-slate-300">{{ chip }}</span>
                </div>

                <a :href="supportMailtoHref" class="mt-5 inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-3 text-sm font-bold text-white transition-colors hover:bg-primary/90 shadow-lg shadow-primary/25">
                  <span class="material-symbols-outlined text-lg">outgoing_mail</span>
                  Gửi yêu cầu hỗ trợ
                </a>
              </div>
            </div>
          </div>

          <div v-show="activeHelpTab === 'student'" class="space-y-6">
            <div class="portal-panel rounded-2xl bg-gradient-to-br from-primary/[0.06] to-transparent p-6 sm:p-8">
              <h3 class="mb-3 flex items-center gap-2 text-lg font-bold text-slate-900 dark:text-white">
                <span class="material-symbols-outlined text-primary">school</span>
                Knowledge base cho học sinh
              </h3>
              <p class="text-sm text-slate-600 dark:text-slate-400">Các checklist này giúp học sinh vào đúng luồng: trước thi, trong lúc thi và sau khi nộp bài.</p>
            </div>

            <div class="grid gap-4 lg:grid-cols-3">
              <article v-for="guide in studentGuides" :key="guide.title" class="portal-panel rounded-2xl p-5">
                <h4 class="text-base font-bold text-slate-900 dark:text-white">{{ guide.title }}</h4>
                <ul class="mt-3 space-y-2 text-sm text-slate-600 dark:text-slate-300">
                  <li v-for="step in guide.steps" :key="step" class="flex gap-2">
                    <span class="material-symbols-outlined text-base text-primary">chevron_right</span>
                    {{ step }}
                  </li>
                </ul>
              </article>
            </div>
          </div>

          <div v-show="activeHelpTab === 'teacher'" class="space-y-6">
            <div class="portal-panel rounded-2xl bg-gradient-to-br from-emerald-500/[0.06] to-transparent p-6 sm:p-8">
              <h3 class="mb-3 flex items-center gap-2 text-lg font-bold text-slate-900 dark:text-white">
                <span class="material-symbols-outlined text-emerald-600 dark:text-emerald-400">co_present</span>
                Knowledge base cho giáo viên
              </h3>
              <p class="text-sm text-slate-600 dark:text-slate-400">Tập trung vào luồng tạo đề, Smart Import Studio, giám sát realtime và xử lý sau thi.</p>
            </div>

            <div class="grid gap-4 lg:grid-cols-3">
              <article v-for="guide in teacherGuides" :key="guide.title" class="portal-panel rounded-2xl p-5">
                <h4 class="text-base font-bold text-slate-900 dark:text-white">{{ guide.title }}</h4>
                <ul class="mt-3 space-y-2 text-sm text-slate-600 dark:text-slate-300">
                  <li v-for="step in guide.steps" :key="step" class="flex gap-2">
                    <span class="material-symbols-outlined text-base text-emerald-500">chevron_right</span>
                    {{ step }}
                  </li>
                </ul>
              </article>
            </div>
          </div>

          <div v-show="activeHelpTab === 'incident'" class="space-y-6">
            <div class="portal-panel rounded-2xl p-6">
              <p class="text-xs font-bold uppercase tracking-[0.18em] text-primary">Incident Playbooks</p>
              <h2 class="mt-1 text-xl font-black tracking-tight text-slate-900 dark:text-white">Playbook xử lý sự cố nhanh</h2>
              <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">Dùng khi cần hướng dẫn từng bước thay vì đọc FAQ chung.</p>
            </div>

            <div class="grid gap-4 lg:grid-cols-[0.85fr_1.15fr]">
              <div class="space-y-3">
                <button
                  v-for="scenario in issueScenarios"
                  :key="scenario.id"
                  type="button"
                  class="w-full rounded-2xl border px-4 py-4 text-left transition-all"
                  :class="selectedScenario.id === scenario.id ? 'border-primary bg-primary/5' : 'border-slate-200/80 bg-white hover:border-primary/40 dark:border-slate-700/80 dark:bg-slate-900/50'"
                  @click="selectedScenarioId = scenario.id"
                >
                  <p class="text-sm font-semibold text-slate-900 dark:text-white">{{ scenario.title }}</p>
                  <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">{{ scenario.roleLabel }}</p>
                </button>
              </div>

              <div class="portal-panel rounded-2xl p-5">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="rounded-full px-3 py-1 text-xs font-semibold" :class="selectedScenario.role === 'teacher' ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300' : 'bg-primary-100 text-primary-800 dark:bg-primary-900/30 dark:text-primary-300'">
                    {{ selectedScenario.roleLabel }}
                  </span>
                </div>
                <h3 class="mt-3 text-xl font-black tracking-tight text-slate-900 dark:text-white">{{ selectedScenario.title }}</h3>
                <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">{{ selectedScenario.description }}</p>
                <ul class="mt-4 space-y-3 text-sm text-slate-600 dark:text-slate-300">
                  <li v-for="step in selectedScenario.checklist" :key="step" class="flex gap-2">
                    <span class="material-symbols-outlined text-base text-primary">check_circle</span>
                    {{ step }}
                  </li>
                </ul>
              </div>
            </div>
          </div>

          <div v-show="activeHelpTab === 'faq'" class="space-y-3">
            <h2 class="mb-4 flex items-center gap-2 text-xl font-bold text-slate-900 dark:text-white">
              <span class="material-symbols-outlined text-primary">quiz</span>
              Câu hỏi thường gặp
            </h2>
            <details
              v-for="(item, idx) in faqs"
              :key="idx"
              class="portal-panel-soft group rounded-xl transition-shadow open:border-primary/20 open:shadow-md"
            >
              <summary class="flex cursor-pointer list-none items-center justify-between gap-3 px-5 py-4 font-semibold text-slate-900 marker:content-none dark:text-white [&::-webkit-details-marker]:hidden">
                <span>{{ item.q }}</span>
                <span class="material-symbols-outlined shrink-0 text-slate-400 transition-transform group-open:rotate-180">expand_more</span>
              </summary>
              <div class="border-t border-slate-100 px-5 pb-4 pt-3 text-sm leading-relaxed text-slate-600 dark:border-slate-800/80 dark:text-slate-400">
                {{ item.a }}
              </div>
            </details>
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
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import BaseButton from '../shared/BaseButton.vue'

const route = useRoute()
const fromTeacher = computed(() => route.query.from === 'teacher')

const supportEmail = 'support@examportal.edu.vn'

const helpTabs = [
  { id: 'overview', label: 'Tổng quan' },
  { id: 'student', label: 'Học sinh' },
  { id: 'teacher', label: 'Giáo viên' },
  { id: 'incident', label: 'Sự cố' },
  { id: 'faq', label: 'FAQ' }
]

const activeHelpTab = ref('overview')
const browserLabel = ref('Trình duyệt chưa xác định')
const onlineLabel = ref('Trạng thái mạng chưa xác định')

const issueScenarios = [
  {
    id: 'camera',
    title: 'Camera hoặc micro không hoạt động',
    description: 'Thường gặp khi học sinh vừa vào phòng chờ hoặc giám thị thấy tín hiệu thiết bị bị tắt.',
    role: 'student',
    roleLabel: 'Học sinh',
    checklist: [
      'Kiểm tra quyền camera/micro trong trình duyệt và trong hệ điều hành.',
      'Tải lại trang sau khi cấp quyền, sau đó vào lại phòng chờ hoặc phòng thi.',
      'Nếu vẫn lỗi, đổi sang trình duyệt Chromium mới nhất và đóng các app khác đang dùng camera.'
    ]
  },
  {
    id: 'network',
    title: 'Mất kết nối hoặc vào phòng thi bị treo',
    description: 'Áp dụng khi không vào được phòng thi, realtime chậm hoặc submit không ổn định.',
    role: 'student',
    roleLabel: 'Học sinh',
    checklist: [
      'Kiểm tra mạng đang ổn định, ưu tiên Wi-Fi mạnh hoặc cắm LAN.',
      'Làm mới trang một lần, sau đó đăng nhập lại và mở đúng mã đề/phiên thi.',
      'Nếu đang thi dở, ghi lại thời điểm sự cố rồi liên hệ giám thị để xác minh.'
    ]
  },
  {
    id: 'import',
    title: 'Import đề gặp lỗi parse hoặc preview bất thường',
    description: 'Dành cho giáo viên khi Smart Import Studio báo warning/error hoặc câu hỏi bị lệch.',
    role: 'teacher',
    roleLabel: 'Giáo viên',
    checklist: [
      'Mở lại preview và dùng issue panel để xác định câu nào đang thiếu nội dung/đáp án.',
      'Ưu tiên dùng mẫu CSV/XLSX chuẩn nếu tài liệu PDF/Word có định dạng quá phức tạp.',
      'Nếu phải gửi hỗ trợ, đính kèm mô tả tệp nguồn và số câu bị ảnh hưởng.'
    ]
  },
  {
    id: 'monitoring',
    title: 'Giám sát realtime báo nhiều cảnh báo',
    description: 'Dành cho giáo viên khi command center xuất hiện nhiều risk alerts hoặc thiết bị bị tắt.',
    role: 'teacher',
    roleLabel: 'Giáo viên',
    checklist: [
      'Ưu tiên mở command center và lọc theo risk queue để không bỏ sót ca nặng.',
      'Xem chi tiết sinh viên trước, sau đó mới gửi cảnh báo hoặc đình chỉ.',
      'Ghi rõ lý do nếu phải đình chỉ để tiện đối soát sau kỳ thi.'
    ]
  }
]

const selectedScenarioId = ref(issueScenarios[0].id)

const selectedScenario = computed(() => issueScenarios.find((item) => item.id === selectedScenarioId.value) || issueScenarios[0])

const supportDiagnostics = computed(() => [
  browserLabel.value,
  onlineLabel.value,
  `Vai trò: ${selectedScenario.value.roleLabel}`,
  `Chủ đề: ${selectedScenario.value.title}`
])

const supportMailtoHref = computed(() => {
  const subject = `[EduExam Support] ${selectedScenario.value.title}`
  const body = [
    'Xin chào đội hỗ trợ,',
    '',
    `Tôi cần hỗ trợ về: ${selectedScenario.value.title}`,
    `Vai trò: ${selectedScenario.value.roleLabel}`,
    '',
    'Những gì tôi đã thử:',
    ...selectedScenario.value.checklist.map((step) => `- ${step}`),
    '',
    'Mô tả thêm:',
    '- ',
    '',
    'Thông tin môi trường:',
    ...supportDiagnostics.value.map((item) => `- ${item}`)
  ].join('\n')
  return `mailto:${supportEmail}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`
})

const studentGuides = [
  {
    title: 'Trước giờ thi',
    steps: [
      'Đăng nhập sớm để kiểm tra email xác minh, thiết bị và đường truyền.',
      'Chuẩn bị camera, micro, nguồn điện và không gian thi yên tĩnh.',
      'Nếu được yêu cầu, giữ tab thi ở chế độ toàn màn hình trước khi vào phòng.'
    ]
  },
  {
    title: 'Trong lúc thi',
    steps: [
      'Giữ kết nối ổn định và tránh đổi tab hoặc mở phần mềm khác.',
      'Nếu gặp lỗi thiết bị, xử lý nhanh theo playbook sự cố rồi báo giám thị.',
      'Chỉ nộp bài khi đã rà xong và xác nhận thời gian còn lại.'
    ]
  },
  {
    title: 'Sau khi nộp bài',
    steps: [
      'Xem Lịch sử / Kết quả để kiểm tra điểm, thời gian làm và câu sai.',
      'Dùng gợi ý luyện tập để tạo lượt ôn tập tiếp theo.',
      'Nếu cần khiếu nại kết quả, gửi hỗ trợ với attempt hoặc mã đề liên quan.'
    ]
  }
]

const teacherGuides = [
  {
    title: 'Chuẩn bị đề thi',
    steps: [
      'Dùng Smart Import Studio để preview trước khi tạo đề nháp.',
      'Rà warning/error trong issue panel thay vì import thẳng nhiều lần.',
      'Chỉ lên lịch khi title, monitoring rules và cấu trúc câu hỏi đã ổn.'
    ]
  },
  {
    title: 'Trong lúc giám sát',
    steps: [
      'Ưu tiên alert inbox và severity queue trước khi quét toàn bộ bảng thí sinh.',
      'Mở chi tiết từng ca rồi mới gửi cảnh báo hoặc đình chỉ.',
      'Theo dõi lại action history để tránh xử lý trùng một sinh viên.'
    ]
  },
  {
    title: 'Sau kỳ thi',
    steps: [
      'Xem review summary và report chi tiết để đánh giá chất lượng kỳ thi.',
      'Kiểm tra các signal rủi ro trước khi chốt kết quả.',
      'Tận dụng dữ liệu câu sai và gợi ý practice để hỗ trợ học sinh ôn lại.'
    ]
  }
]

const faqs = [
  {
    q: 'Làm sao để tham gia bài thi?',
    a: 'Đăng nhập bằng tài khoản học sinh, vào mục Thi qua mã rồi nhập mã đề hoặc tìm theo tiêu đề. Vào phòng chờ đúng giờ và làm theo hướng dẫn của hệ thống.'
  },
  {
    q: 'Quên mật khẩu hoặc email chưa xác minh?',
    a: 'Tại màn đăng nhập, chọn Quên mật khẩu để nhận email đặt lại. Nếu email chưa xác minh, kiểm tra hộp thư/spam hoặc yêu cầu gửi lại link xác minh.'
  },
  {
    q: 'Giáo viên import đề thi như thế nào cho an toàn?',
    a: 'Dùng Smart Import Studio để tải tệp, xem preview, xử lý warning/error rồi mới tạo đề nháp. Cách này giảm rủi ro import sai so với upload trực tiếp.'
  },
  {
    q: 'Khi nào nên liên hệ hỗ trợ?',
    a: 'Khi checklist xử lý sự cố không còn tác dụng, hoặc khi cần đội hỗ trợ kiểm tra dữ liệu kỳ thi, tài khoản, email hay luồng giám sát realtime.'
  }
]

onMounted(() => {
  if (typeof window === 'undefined') return
  browserLabel.value = `UA: ${window.navigator.userAgent.split(' ').slice(0, 3).join(' ')}`
  onlineLabel.value = window.navigator.onLine ? 'Kết nối mạng: online' : 'Kết nối mạng: offline'
})
</script>
