import { createRouter, createWebHistory } from 'vue-router'
import { ref } from 'vue'
import {
  getCachedOrRefresh,
  invalidateSession,
  userHasAdminRole,
  userHasTeacherAccess,
  userCanAccessStudentArea
} from './services/authService'
import { pinia } from './stores'
import { useAuthStore } from './stores/authStore'
import { toastService } from './services/toastService'
import { readMonitoringSessionQuery } from './services/monitoringContextStorage'

export const routeNavPending = ref(false)

const devRoutes = import.meta.env.DEV
  ? [
      {
        path: '/dev/ui-kit',
        component: () => import('./components/dev/UiKitPage.vue')
      }
    ]
  : []

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash, behavior: 'smooth' }
    return { top: 0, behavior: 'smooth' }
  },
  routes: [
    { path: '/', component: () => import('./components/public/SiteIntroduction.vue'), alias: ['/gioi-thieu'] },
    { path: '/login', component: () => import('./components/login/LoginRoleSelection.vue'), meta: { guest: true } },
    { path: '/forgot-password', component: () => import('./components/login/ForgotPassword.vue'), meta: { guest: true } },
    { path: '/reset-password', component: () => import('./components/login/ResetPassword.vue'), meta: { guest: true } },
    { path: '/verify-email', component: () => import('./components/login/VerifyEmail.vue'), meta: { guest: true } },
    { path: '/verify-email-pending', component: () => import('./components/login/VerifyEmailPending.vue'), meta: { guest: true } },
    { path: '/register', component: () => import('./components/login/RegistrationRoleSelection.vue'), meta: { guest: true } },
    { path: '/help-center', component: () => import('./components/help/HelpCenter.vue') },
    { path: '/select-role', component: () => import('./components/login/SelectRole.vue'), meta: { requiresAuth: true } },
    {
      path: '/admin',
      component: () => import('./components/admin/AdminLayout.vue'),
      meta: { requiresAuth: true, adminOnly: true },
      children: [
        { path: '', redirect: '/admin/dashboard' },
        { path: 'dashboard', component: () => import('./components/admin/AdminSiteDashboard.vue') },
        {
          path: 'students',
          component: () => import('./components/admin/AdminUserManagement.vue'),
          props: { variant: 'students' }
        },
        {
          path: 'teachers',
          component: () => import('./components/admin/AdminUserManagement.vue'),
          props: { variant: 'teachers' }
        },
        {
          path: 'admins',
          component: () => import('./components/admin/AdminUserManagement.vue'),
          props: { variant: 'admins' }
        },
        { path: 'exams', component: () => import('./components/admin/AdminExamManagement.vue') }
      ]
    },
    { path: '/teacher/dashboard', component: () => import('./components/teacher/TeacherDashboard.vue'), meta: { requiresAuth: true, teacherOnly: true, layout: 'portal' } },
    { path: '/teacher/exams', redirect: '/teacher/exams/list' },
    { path: '/teacher/classes', component: () => import('./components/teacher/class/ClassManagementPage.vue'), meta: { requiresAuth: true, teacherOnly: true, layout: 'portal' } },
    { path: '/teacher/exams/list', component: () => import('./components/teacher/exam/list/ExamListPage.vue'), meta: { requiresAuth: true, teacherOnly: true, layout: 'portal' } },
    { path: '/teacher/exams/create', component: () => import('./components/teacher/exam/ExamTypeSelector.vue'), meta: { requiresAuth: true, teacherOnly: true, layout: 'portal' } },
    { path: '/teacher/exams/build', component: () => import('./components/teacher/exam/ExamCreationPage.vue'), meta: { requiresAuth: true, teacherOnly: true, layout: 'portal' } },
    { path: '/teacher/exams/manual', component: () => import('./components/teacher/TeacherManualQuestionEntry.vue'), meta: { requiresAuth: true, teacherOnly: true, layout: 'portal' } },
    { path: '/teacher/exams/schedule', component: () => import('./components/teacher/TeacherExamScheduleCreate.vue'), meta: { requiresAuth: true, teacherOnly: true, layout: 'portal' } },
    { path: '/teacher/exams/new-session', component: () => import('./components/teacher/TeacherExamNewSession.vue'), meta: { requiresAuth: true, teacherOnly: true, layout: 'portal' } },
    { path: '/teacher/exams/created-success', component: () => import('./components/teacher/TeacherExamCreatedSuccess.vue'), meta: { requiresAuth: true, teacherOnly: true, layout: 'portal' } },
    { path: '/teacher/exams/review', redirect: to => ({ path: '/teacher/exams/review/summary', query: to.query }), meta: { requiresAuth: true, teacherOnly: true, layout: 'portal' } },
    { path: '/teacher/exams/review/summary', component: () => import('./components/teacher/TeacherExamReviewSummary.vue'), meta: { requiresAuth: true, teacherOnly: true, layout: 'portal' } },
    { path: '/teacher/exams/review/incidents', component: () => import('./components/teacher/TeacherIncidentReviewUpdatedMenu.vue'), meta: { requiresAuth: true, teacherOnly: true, layout: 'portal' } },
    { path: '/teacher/exams/review/student-report', component: () => import('./components/teacher/TeacherStudentReportDetail.vue'), meta: { requiresAuth: true, teacherOnly: true, layout: 'portal' } },
    { path: '/teacher/exams/:id', component: () => import('./components/teacher/exam/detail/ExamDetailPage.vue'), meta: { requiresAuth: true, teacherOnly: true, layout: 'portal' } },
    { path: '/teacher/live-monitoring', component: () => import('./components/teacher/TeacherSelectExamUpdatedMenu.vue'), meta: { requiresAuth: true, teacherOnly: true, layout: 'monitoring' } },
    { path: '/teacher/live-monitoring/session', component: () => import('./components/teacher/TeacherLiveMonitoringUpdatedMenu.vue'), meta: { requiresAuth: true, teacherOnly: true, layout: 'monitoring' } },
    { path: '/teacher/live-monitoring/student-detail', component: () => import('./components/teacher/StudentViolationDetailMonitoring.vue'), meta: { requiresAuth: true, teacherOnly: true, layout: 'monitoring' } },
    { path: '/teacher/profile', component: () => import('./components/teacher/TeacherProfile.vue'), meta: { requiresAuth: true, teacherOnly: true, layout: 'portal' } },
    { path: '/teacher/analytics', component: () => import('./components/teacher/TeacherAnalyticsDashboard.vue'), meta: { requiresAuth: true, teacherOnly: true, layout: 'portal' } },
    { path: '/student/dashboard', component: () => import('./components/student/StudentDashboard.vue'), meta: { requiresAuth: true, studentOnly: true, layout: 'studentPortal' } },
    { path: '/student/exam-join', component: () => import('./components/student/StudentExamJoinByCode.vue'), meta: { requiresAuth: true, studentOnly: true, layout: 'studentPortal' } },
    { path: '/student/exam-waiting-room', component: () => import('./components/student/StudentExamWaitingRoom.vue'), meta: { requiresAuth: true, studentOnly: true, layout: 'studentPortal' } },
    { path: '/student/exam-interface', component: () => import('./components/student/StudentExamInterface.vue'), meta: { requiresAuth: true, studentOnly: true, layout: 'exam' } },
    { path: '/student/submission-confirmation', component: () => import('./components/student/StudentSubmissionConfirmation.vue'), meta: { requiresAuth: true, studentOnly: true, layout: 'studentPortal' } },
    { path: '/student/study-history', component: () => import('./components/student/StudentStudyHistory.vue'), meta: { requiresAuth: true, studentOnly: true, layout: 'studentPortal' } },
    { path: '/student/exam-result', component: () => import('./components/student/StudentExamResultDetail.vue'), meta: { requiresAuth: true, studentOnly: true, layout: 'studentPortal' } },
    { path: '/student/classes', component: () => import('./components/student/class/StudentClassList.vue'), meta: { requiresAuth: true, studentOnly: true, layout: 'studentPortal' } },
    { path: '/student/generate-practice-test', component: () => import('./components/student/StudentGeneratePracticeTest.vue'), meta: { requiresAuth: true, studentOnly: true, layout: 'studentPortal' } },
    { path: '/student/profile', component: () => import('./components/student/StudentProfile.vue'), meta: { requiresAuth: true, studentOnly: true, layout: 'studentPortal' } },
    { path: '/student/schedule', component: () => import('./components/student/StudentExamSchedule.vue'), meta: { requiresAuth: true, studentOnly: true, layout: 'studentPortal' } },
    ...devRoutes
  ]
})

router.beforeEach(async (to, from, next) => {
  if (to.path === '/teacher/live-monitoring/session' && !to.query?.examId) {
    const restored = readMonitoringSessionQuery()
    if (restored) {
      return next({ path: to.path, query: { ...to.query, ...restored }, replace: true })
    }
  }

  const authStore = useAuthStore(pinia)
  authStore.syncFromStorage()
  const token = authStore.token

  if (to.meta.guest) {
    if (!token) return next()

    const cachedUser = await authStore.refreshSession()
    if (cachedUser) {
      return next(authStore.dashboardPath)
    }

    invalidateSession()
    return next()
  }

  if (to.meta.requiresAuth) {
    if (!token) {
      toastService.info('Vui lòng đăng nhập để tiếp tục.')
      return next('/login')
    }

    const cachedUser = await authStore.refreshSession()

    if (!cachedUser) {
      invalidateSession()
      return next('/login')
    }

    if (!cachedUser.roles?.length && to.path !== '/select-role') {
      return next('/select-role')
    }

    if (cachedUser.roles?.length && to.path === '/select-role') {
      return next(authStore.dashboardPath)
    }

    if (to.meta.adminOnly && !userHasAdminRole(cachedUser)) {
      return next(authStore.dashboardPath)
    }

    if (to.meta.teacherOnly && !userHasTeacherAccess(cachedUser)) {
      toastService.warning('Bạn không có quyền truy cập khu vực dành cho giáo viên.')
      return next(authStore.dashboardPath)
    }

    if (to.meta.studentOnly && !userCanAccessStudentArea(cachedUser)) {
      toastService.warning('Bạn không có quyền truy cập khu vực dành cho học sinh.')
      return next(authStore.dashboardPath)
    }

    return next()
  }

  next()
})

router.afterEach(() => {
  window.scrollTo({ top: 0, behavior: 'instant' })
})

export default router
