import { watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import { useNavLabelMode } from './useNavLabelMode'

/**
 * Đồng bộ --demo2-sidebar-offset + data-teacher-nav / data-student-nav trên <html>
 * khi đổi menu rail/full. Cần chạy trên mọi layout có teacher/student portal (kể cả giám sát
 * chỉ dùng TeacherMonitoringLayout, không bọc PortalLayout).
 */
export function useTeacherShellDocumentSync() {
  const route = useRoute()
  const { mode } = useNavLabelMode()

  watchEffect(() => {
    if (typeof document === 'undefined') return
    const rail = mode.value === 'icons'
    const teacher = (route.path || '').startsWith('/teacher')
    const full = teacher ? '16rem' : '18rem'
    document.documentElement.style.setProperty('--demo2-sidebar-offset', rail ? '4.5rem' : full)
    if (teacher) {
      document.documentElement.setAttribute('data-teacher-nav', rail ? 'rail' : 'full')
      document.documentElement.removeAttribute('data-student-nav')
    } else if ((route.path || '').startsWith('/student/')) {
      document.documentElement.setAttribute('data-student-nav', rail ? 'rail' : 'full')
      document.documentElement.removeAttribute('data-teacher-nav')
    } else {
      document.documentElement.removeAttribute('data-teacher-nav')
      document.documentElement.removeAttribute('data-student-nav')
    }
  })
}
