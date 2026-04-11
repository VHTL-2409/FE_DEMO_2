import { ref, computed } from 'vue'

const notifications = ref([
  { id: 1, title: 'Chào mừng đến ExamPortal', message: 'Hệ thống thi trực tuyến đã sẵn sàng.', time: new Date().toISOString(), read: false, type: 'info' },
  { id: 2, title: 'Hướng dẫn sử dụng', message: 'Truy cập mục Tạo đề thi để bắt đầu nhập câu hỏi.', time: new Date().toISOString(), read: true, type: 'info' }
])

const SUPPORT_EMAIL = 'support@examportal.edu.vn'
const TEACHER_CONTACT = 'teacher@examportal.edu.vn'

export function useNotifications() {
  const unreadCount = computed(() => notifications.value.filter((n) => !n.read).length)
  const hasUnread = computed(() => unreadCount.value > 0)

  const markAsRead = (id) => {
    const n = notifications.value.find((x) => x.id === id)
    if (n) n.read = true
  }

  const markAllAsRead = () => {
    notifications.value.forEach((n) => { n.read = true })
  }

  const openSupport = () => {
    window.location.href = `mailto:${SUPPORT_EMAIL}?subject=Hỗ trợ ExamPortal`
  }

  const openTeacherContact = () => {
    window.location.href = `mailto:${TEACHER_CONTACT}?subject=Trao đổi kết quả bài thi`
  }

  return {
    notifications,
    unreadCount,
    hasUnread,
    markAsRead,
    markAllAsRead,
    openSupport,
    openTeacherContact,
    SUPPORT_EMAIL,
    TEACHER_CONTACT
  }
}
