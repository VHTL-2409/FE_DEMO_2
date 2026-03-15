import { createRouter, createWebHistory } from 'vue-router'
import { getStoredToken, invalidateSession, validateSession } from './services/authService'
import { toastService } from './services/toastService'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: () => import('./components/login/LoginRoleSelection.vue'), meta: { guest: true } },
    { path: '/register', component: () => import('./components/login/RegistrationRoleSelection.vue'), meta: { guest: true } },
    { path: '/teacher/dashboard', component: () => import('./components/teacher/TeacherDashboardTopNav.vue'), meta: { requiresAuth: true } },
    { path: '/teacher/exams', component: () => import('./components/teacher/TeacherExamManagementUpdatedMenu.vue'), meta: { requiresAuth: true } },
    { path: '/teacher/exams/list', component: () => import('./components/teacher/TeacherExamListMenu.vue'), meta: { requiresAuth: true } },
    { path: '/teacher/exams/create', component: () => import('./components/teacher/TeacherCompleteExamCreationSyncedHeader.vue'), meta: { requiresAuth: true } },
    { path: '/teacher/exams/manual', component: () => import('./components/teacher/TeacherManualQuestionEntry.vue'), meta: { requiresAuth: true } },
    { path: '/teacher/exams/schedule', component: () => import('./components/teacher/TeacherExamScheduleCreate.vue'), meta: { requiresAuth: true } },
    { path: '/teacher/exams/created-success', component: () => import('./components/teacher/TeacherExamCreatedSuccess.vue'), meta: { requiresAuth: true } },
    { path: '/teacher/exams/review', redirect: to => ({ path: '/teacher/exams/review/summary', query: to.query }), meta: { requiresAuth: true } },
    { path: '/teacher/exams/review/summary', component: () => import('./components/teacher/TeacherExamReviewSummary.vue'), meta: { requiresAuth: true } },
    { path: '/teacher/exams/review/incidents', component: () => import('./components/teacher/TeacherIncidentReviewUpdatedMenu.vue'), meta: { requiresAuth: true } },
    { path: '/teacher/exams/review/student-report', component: () => import('./components/teacher/TeacherStudentReportDetail.vue'), meta: { requiresAuth: true } },
    { path: '/teacher/live-monitoring', component: () => import('./components/teacher/TeacherSelectExamUpdatedMenu.vue'), meta: { requiresAuth: true } },
    { path: '/teacher/live-monitoring/session', component: () => import('./components/teacher/TeacherLiveMonitoringUpdatedMenu.vue'), meta: { requiresAuth: true } },
    { path: '/teacher/live-monitoring/student-detail', component: () => import('./components/teacher/StudentViolationDetailMonitoring.vue'), meta: { requiresAuth: true } },
    { path: '/teacher/profile', component: () => import('./components/teacher/TeacherProfile.vue'), meta: { requiresAuth: true } },
    { path: '/student/dashboard', component: () => import('./components/student/StudentDashboardEnhancedNavigation.vue'), meta: { requiresAuth: true } },
    { path: '/student/exam-join', component: () => import('./components/student/StudentExamJoinByCode.vue'), meta: { requiresAuth: true } },
    { path: '/student/exam-waiting-room', component: () => import('./components/student/StudentExamWaitingRoom.vue'), meta: { requiresAuth: true } },
    { path: '/student/exam-interface', component: () => import('./components/student/StudentExamInterface.vue'), meta: { requiresAuth: true } },
    { path: '/student/submission-confirmation', component: () => import('./components/student/StudentSubmissionConfirmation.vue'), meta: { requiresAuth: true } },
    { path: '/student/study-history', component: () => import('./components/student/StudentStudyHistory.vue'), meta: { requiresAuth: true } },
    { path: '/student/exam-result', component: () => import('./components/student/StudentExamResultDetail.vue'), meta: { requiresAuth: true } },
    { path: '/student/generate-practice-test', component: () => import('./components/student/StudentGeneratePracticeTest.vue'), meta: { requiresAuth: true } },
    { path: '/student/profile', component: () => import('./components/student/StudentProfile.vue'), meta: { requiresAuth: true } }
  ]
})

const getDashboardByRole = (user) => {
  const roles = user?.roles || []
  if (roles.some(r => r === 'ROLE_TEACHER' || r === 'TEACHER')) {
    return '/teacher/dashboard'
  }
  return '/student/dashboard'
}

router.beforeEach(async (to, from, next) => {
  const token = getStoredToken()

  // Trang dành cho khách (login, register): nếu đã có token hợp lệ → redirect về dashboard
  if (to.meta.guest) {
    if (!token) return next()

    const cachedUser = await validateSession()
    if (cachedUser) {
      return next(getDashboardByRole(cachedUser))
    }

    invalidateSession()
    return next()  }

  // Trang yêu cầu đăng nhập: kiểm tra token
  if (to.meta.requiresAuth) {
    if (!token) {
      toastService.info('Vui lòng đăng nhập để tiếp tục.')
      return next('/login')
    }

    const cachedUser = await validateSession()

    if (!cachedUser) {
      invalidateSession()
      return next('/login')
    }

    return next()
  }

  next()
})

export default router