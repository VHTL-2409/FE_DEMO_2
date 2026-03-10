import { createRouter, createWebHistory } from 'vue-router'
import LoginRoleSelection from './components/login/LoginRoleSelection.vue'
import RegistrationRoleSelection from './components/login/RegistrationRoleSelection.vue'
import TeacherDashboardTopNav from './components/teacher/TeacherDashboardTopNav.vue'
import StudentDashboardEnhancedNavigation from './components/student/StudentDashboardEnhancedNavigation.vue'
import TeacherExamManagementUpdatedMenu from './components/teacher/TeacherExamManagementUpdatedMenu.vue'
import TeacherCompleteExamCreationSyncedHeader from './components/teacher/TeacherCompleteExamCreationSyncedHeader.vue'
import TeacherIncidentReviewUpdatedMenu from './components/teacher/TeacherIncidentReviewUpdatedMenu.vue'
import TeacherManualQuestionEntry from './components/teacher/TeacherManualQuestionEntry.vue'
import TeacherExamScheduleCreate from './components/teacher/TeacherExamScheduleCreate.vue'
import TeacherSelectExamUpdatedMenu from './components/teacher/TeacherSelectExamUpdatedMenu.vue'
import TeacherLiveMonitoringUpdatedMenu from './components/teacher/TeacherLiveMonitoringUpdatedMenu.vue'
import StudentViolationDetailMonitoring from './components/teacher/StudentViolationDetailMonitoring.vue'
import StudentExamWaitingRoom from './components/student/StudentExamWaitingRoom.vue'
import StudentExamInterface from './components/student/StudentExamInterface.vue'
import StudentSubmissionConfirmation from './components/student/StudentSubmissionConfirmation.vue'
import StudentStudyHistory from './components/student/StudentStudyHistory.vue'
import StudentExamResultDetail from './components/student/StudentExamResultDetail.vue'
import StudentGeneratePracticeTest from './components/student/StudentGeneratePracticeTest.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: LoginRoleSelection },
    { path: '/register', component: RegistrationRoleSelection },
    { path: '/teacher/dashboard', component: TeacherDashboardTopNav },
    { path: '/teacher/exams', component: TeacherExamManagementUpdatedMenu },
    { path: '/teacher/exams/create', component: TeacherCompleteExamCreationSyncedHeader },
    { path: '/teacher/exams/manual', component: TeacherManualQuestionEntry },
    { path: '/teacher/exams/schedule', component: TeacherExamScheduleCreate },
    { path: '/teacher/exams/review', component: TeacherIncidentReviewUpdatedMenu },
    { path: '/teacher/live-monitoring', component: TeacherSelectExamUpdatedMenu },
    { path: '/teacher/live-monitoring/session', component: TeacherLiveMonitoringUpdatedMenu },
    { path: '/teacher/live-monitoring/student-detail', component: StudentViolationDetailMonitoring },
    { path: '/student/dashboard', component: StudentDashboardEnhancedNavigation },
    { path: '/student/exam-waiting-room', component: StudentExamWaitingRoom },
    { path: '/student/exam-interface', component: StudentExamInterface },
    { path: '/student/submission-confirmation', component: StudentSubmissionConfirmation },
    { path: '/student/study-history', component: StudentStudyHistory },
    { path: '/student/exam-result', component: StudentExamResultDetail },
    { path: '/student/generate-practice-test', component: StudentGeneratePracticeTest }
  ]
})

export default router