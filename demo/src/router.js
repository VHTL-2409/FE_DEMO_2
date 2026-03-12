import { createRouter, createWebHistory } from 'vue-router'
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
import StudentGeneratePracticeTest from './components/student/StudentGeneratePracticeTest.vue'
import StudentProfile from './components/student/StudentProfile.vue'
import TeacherProfile from './components/teacher/TeacherProfile.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: LoginRoleSelection },
    { path: '/register', component: RegistrationRoleSelection },
    { path: '/teacher/dashboard', component: TeacherDashboardTopNav },
    { path: '/teacher/exams', component: TeacherExamManagementUpdatedMenu },
    { path: '/teacher/exams/list', component: TeacherExamListMenu },
    { path: '/teacher/exams/create', component: TeacherCompleteExamCreationSyncedHeader },
    { path: '/teacher/exams/manual', component: TeacherManualQuestionEntry },
    { path: '/teacher/exams/schedule', component: TeacherExamScheduleCreate },
    { path: '/teacher/exams/created-success', component: TeacherExamCreatedSuccess },
    { path: '/teacher/exams/review', redirect: to => ({ path: '/teacher/exams/review/summary', query: to.query }) },
    { path: '/teacher/exams/review/summary', component: TeacherExamReviewSummary },
    { path: '/teacher/exams/review/incidents', component: TeacherIncidentReviewUpdatedMenu },
    { path: '/teacher/exams/review/student-report', component: TeacherStudentReportDetail },
    { path: '/teacher/live-monitoring', component: TeacherSelectExamUpdatedMenu },
    { path: '/teacher/live-monitoring/session', component: TeacherLiveMonitoringUpdatedMenu },
    { path: '/teacher/live-monitoring/student-detail', component: StudentViolationDetailMonitoring },
    { path: '/teacher/profile', component: TeacherProfile },
    { path: '/student/dashboard', component: StudentDashboardEnhancedNavigation },
    { path: '/student/exam-waiting-room', component: StudentExamWaitingRoom },
    { path: '/student/exam-interface', component: StudentExamInterface },
    { path: '/student/submission-confirmation', component: StudentSubmissionConfirmation },
    { path: '/student/study-history', component: StudentStudyHistory },
    { path: '/student/exam-result', component: StudentExamResultDetail },
    { path: '/student/generate-practice-test', component: StudentGeneratePracticeTest },
    { path: '/student/profile', component: StudentProfile }
  ]
})

export default router