import { createRouter, createWebHistory } from 'vue-router'
import { getStoredToken, getStoredUser, validateSession } from './services/authService'
import LoginRoleSelection from './components/login/LoginRoleSelection.vue'
import RegistrationRoleSelection from './components/login/RegistrationRoleSelection.vue'
import TeacherDashboardTopNav from './components/teacher/TeacherDashboardTopNav.vue'
import StudentDashboardEnhancedNavigation from './components/student/StudentDashboardEnhancedNavigation.vue'
import TeacherExamManagementUpdatedMenu from './components/teacher/TeacherExamManagementUpdatedMenu.vue'
import TeacherExamListMenu from './components/teacher/TeacherExamListMenu.vue'
import TeacherCompleteExamCreationSyncedHeader from './components/teacher/TeacherCompleteExamCreationSyncedHeader.vue'
import TeacherIncidentReviewUpdatedMenu from './components/teacher/TeacherIncidentReviewUpdatedMenu.vue'
import TeacherExamReviewSummary from './components/teacher/TeacherExamReviewSummary.vue'
import TeacherStudentReportDetail from './components/teacher/TeacherStudentReportDetail.vue'
import TeacherManualQuestionEntry from './components/teacher/TeacherManualQuestionEntry.vue'
import TeacherExamScheduleCreate from './components/teacher/TeacherExamScheduleCreate.vue'
import TeacherExamCreatedSuccess from './components/teacher/TeacherExamCreatedSuccess.vue'
import TeacherSelectExamUpdatedMenu from './components/teacher/TeacherSelectExamUpdatedMenu.vue'
import TeacherLiveMonitoringUpdatedMenu from './components/teacher/TeacherLiveMonitoringUpdatedMenu.vue'
import StudentViolationDetailMonitoring from './components/teacher/StudentViolationDetailMonitoring.vue'
import StudentExamWaitingRoom from './components/student/StudentExamWaitingRoom.vue'
import StudentExamInterface from './components/student/StudentExamInterface.vue'
import StudentSubmissionConfirmation from './components/student/StudentSubmissionConfirmation.vue'
import StudentStudyHistory from './components/student/StudentStudyHistory.vue'
import StudentExamResultDetail from './components/student/StudentExamResultDetail.vue'
import StudentExamJoinByCode from './components/student/StudentExamJoinByCode.vue'
import StudentGeneratePracticeTest from './components/student/StudentGeneratePracticeTest.vue'
import StudentProfile from './components/student/StudentProfile.vue'
import TeacherProfile from './components/teacher/TeacherProfile.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: LoginRoleSelection, meta: { guest: true } },
    { path: '/register', component: RegistrationRoleSelection, meta: { guest: true } },
    { path: '/teacher/dashboard', component: TeacherDashboardTopNav, meta: { requiresAuth: true } },
    { path: '/teacher/exams', component: TeacherExamManagementUpdatedMenu, meta: { requiresAuth: true } },
    { path: '/teacher/exams/list', component: TeacherExamListMenu, meta: { requiresAuth: true } },
    { path: '/teacher/exams/create', component: TeacherCompleteExamCreationSyncedHeader, meta: { requiresAuth: true } },
    { path: '/teacher/exams/manual', component: TeacherManualQuestionEntry, meta: { requiresAuth: true } },
    { path: '/teacher/exams/schedule', component: TeacherExamScheduleCreate, meta: { requiresAuth: true } },
    { path: '/teacher/exams/created-success', component: TeacherExamCreatedSuccess, meta: { requiresAuth: true } },
    { path: '/teacher/exams/review', redirect: to => ({ path: '/teacher/exams/review/summary', query: to.query }), meta: { requiresAuth: true } },
    { path: '/teacher/exams/review/summary', component: TeacherExamReviewSummary, meta: { requiresAuth: true } },
    { path: '/teacher/exams/review/incidents', component: TeacherIncidentReviewUpdatedMenu, meta: { requiresAuth: true } },
    { path: '/teacher/exams/review/student-report', component: TeacherStudentReportDetail, meta: { requiresAuth: true } },
    { path: '/teacher/live-monitoring', component: TeacherSelectExamUpdatedMenu, meta: { requiresAuth: true } },
    { path: '/teacher/live-monitoring/session', component: TeacherLiveMonitoringUpdatedMenu, meta: { requiresAuth: true } },
    { path: '/teacher/live-monitoring/student-detail', component: StudentViolationDetailMonitoring, meta: { requiresAuth: true } },
    { path: '/teacher/profile', component: TeacherProfile, meta: { requiresAuth: true } },
    { path: '/student/dashboard', component: StudentDashboardEnhancedNavigation, meta: { requiresAuth: true } },
    { path: '/student/exam-join', component: StudentExamJoinByCode, meta: { requiresAuth: true } },
    { path: '/student/exam-waiting-room', component: StudentExamWaitingRoom, meta: { requiresAuth: true } },
    { path: '/student/exam-interface', component: StudentExamInterface, meta: { requiresAuth: true } },
    { path: '/student/submission-confirmation', component: StudentSubmissionConfirmation, meta: { requiresAuth: true } },
    { path: '/student/study-history', component: StudentStudyHistory, meta: { requiresAuth: true } },
    { path: '/student/exam-result', component: StudentExamResultDetail, meta: { requiresAuth: true } },
    { path: '/student/generate-practice-test', component: StudentGeneratePracticeTest, meta: { requiresAuth: true } },
    { path: '/student/profile', component: StudentProfile, meta: { requiresAuth: true } }
  ]
})

// Cache: chỉ validate token 1 lần mỗi lần load trang
let sessionChecked = false
let cachedUser = null

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

    // Validate token (chỉ gọi API 1 lần)
    if (!sessionChecked) {
      cachedUser = await validateSession()
      sessionChecked = true
    }

    if (cachedUser) {
      return next(getDashboardByRole(cachedUser))
    }

    return next()
  }

  // Trang yêu cầu đăng nhập: kiểm tra token
  if (to.meta.requiresAuth) {
    if (!token) {
      return next('/login')
    }

    // Validate token (chỉ gọi API 1 lần)
    if (!sessionChecked) {
      cachedUser = await validateSession()
      sessionChecked = true
    }

    if (!cachedUser) {
      return next('/login')
    }

    return next()
  }

  next()
})

export default router