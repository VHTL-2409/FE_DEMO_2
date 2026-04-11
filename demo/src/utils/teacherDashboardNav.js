/** Teacher dashboard routes: unified path + tab + exam results query. */
export const TEACHER_DASHBOARD_PATH = '/teacher/dashboard'

export function teacherDashboardLocation(tab = 'overview', extra = {}) {
  const q = { ...extra }
  if (tab === 'overview') {
    delete q.tab
  } else {
    q.tab = tab
  }
  return { path: TEACHER_DASHBOARD_PATH, query: q }
}

export function teacherResultsLocation(exam) {
  const id = exam?.id ?? exam
  const title = (exam && typeof exam === 'object' ? exam.title : '') || ''
  return {
    path: '/teacher/exams/review/summary',
    query: { examId: String(id), title }
  }
}
