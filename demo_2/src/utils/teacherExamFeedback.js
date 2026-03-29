/**
 * Thông báo sau khi xuất bản / tạo đợt thi — dùng toast, không dùng trang riêng.
 */
export function toastExamPublished(toast, { title, code, isUpdate = false, duration = 10000 } = {}) {
  const name = (title && String(title).trim()) || 'Đề thi'
  const codeStr = code != null && String(code).trim() !== '' ? String(code).trim() : '—'
  const msg = isUpdate
    ? `Đã cập nhật lịch cho "${name}". Mã tham gia: ${codeStr}`
    : `Đã xuất bản "${name}". Mã tham gia: ${codeStr}`
  toast.success(msg, { duration })
}
