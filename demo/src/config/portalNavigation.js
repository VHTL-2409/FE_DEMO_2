export const studentSidebarItems = [
  { section: 'dashboard', to: '/student/dashboard', icon: 'home', label: 'Trang chủ' },
  { section: 'activity', to: '/student/study-history', icon: 'calendar_month', label: 'Lịch & kết quả' },
  { section: 'join', to: '/student/exam-join', icon: 'login', label: 'Vào thi' },
  { section: 'practice', to: '/student/generate-practice-test', icon: 'model_training', label: 'Luyện tập' },
  { section: 'classes', to: '/student/classes', icon: 'school', label: 'Lớp học' },
  { section: 'profile', to: '/student/profile', icon: 'account_circle', label: 'Hồ sơ' }
]

export function getStudentActiveSection(path = '') {
  if (path.startsWith('/student/dashboard')) return 'dashboard'
  if (path.startsWith('/student/exam-join')) return 'join'
  if (path.startsWith('/student/generate-practice-test')) return 'practice'
  if (
    path.startsWith('/student/study-history')
    || path.startsWith('/student/exam-result')
    || path.startsWith('/student/submission-confirmation')
    || path.startsWith('/student/exam-waiting-room')
    || path.startsWith('/student/exam-interface')
  ) return 'activity'
  if (path.startsWith('/student/classes')) return 'classes'
  if (path.startsWith('/student/profile')) return 'profile'
  return 'dashboard'
}

function normalizeMonitoringPath(path = '') {
  return path.replace(/\/teacher\/live_monitoring/g, '/teacher/live-monitoring')
}

export function getTeacherSidebarItems(alertCount = 0) {
  return [
    { section: 'dashboard', to: '/teacher/dashboard', icon: 'dashboard', label: 'Tổng quan' },
    { section: 'classes', to: '/teacher/classes', icon: 'groups', label: 'Lớp học' },
    { section: 'workspace', to: '/teacher/exams/create', icon: 'edit_square', label: 'Tạo và xuất bản' },
    { section: 'exams', to: '/teacher/exams/list', icon: 'list_alt', label: 'Đề thi' },
    {
      section: 'monitoring',
      to: '/teacher/live-monitoring',
      icon: 'live_tv',
      label: 'Giám sát',
      badge: alertCount > 0 ? String(alertCount) : null,
      badgeVariant: 'danger'
    },
    { section: 'profile', to: '/teacher/profile', icon: 'account_circle', label: 'Hồ sơ' }
  ]
}

export function getTeacherActiveSection(path = '', query = {}) {
  const p = normalizeMonitoringPath(path)
  if (p.startsWith('/teacher/dashboard')) {
    return 'dashboard'
  }
  if (p.startsWith('/teacher/classes') || p.startsWith('/teacher/class/')) return 'classes'
  if (p.startsWith('/teacher/exams/review/summary')) return 'exams'
  if (p.startsWith('/teacher/exams/review')) return 'exams'
  if (p.startsWith('/teacher/analytics')) return 'dashboard'
  if (p.startsWith('/teacher/live-monitoring')) return 'monitoring'
  if (p.startsWith('/teacher/profile')) return 'profile'
  if (
    p.startsWith('/teacher/exams/create')
    || p.startsWith('/teacher/exams/build')
    || p.startsWith('/teacher/exams/manual')
    || p.startsWith('/teacher/exams/schedule')
    || p.startsWith('/teacher/exams/new-session')
    || p.startsWith('/teacher/exams/created-success')
    || p.startsWith('/teacher/exams/waiting-room')
  ) return 'workspace'
  if (p.startsWith('/teacher/exams/list') || /^\/teacher\/exams\/\d+/.test(p)) return 'exams'
  return 'dashboard'
}
