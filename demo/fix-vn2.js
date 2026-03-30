/**
 * Batch fix Vietnamese text using precise patterns
 */
import { readFileSync, writeFileSync, readdirSync, statSync } from 'fs'
import { join, relative, extname } from 'path'
import { fileURLToPath } from 'url'
import { dirname } from 'path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const srcDir = join(__dirname, 'src')

// All fixes: pattern -> replacement (exact match)
const FIXES = new Map([
  // DifficultyInsightCard
  ['"Top cau hoi kho"', '"Top câu hỏi khó"'],
  ['"Ty le sai cao nhat"', '"Tỷ lệ sai cao nhất"'],
  ['"Ty le sai: ${"', '"Tỷ lệ sai: ${"'],
  ['"Thu gon"', '"Thu gọn"'],
  ['`${sortedQuestions.length - displayLimit} cau khac`', '`${sortedQuestions.length - displayLimit} câu khác`'],
  ['"Xem tat ca cau hoi"', '"Xem tất cả câu hỏi"'],
  ['"Tất cả câu hỏi đều có tỷ lệ đúng tốt"', '"Tất cả câu hỏi đều có tỷ lệ đúng tốt"'], // already fixed
  ['"Tat ca cau hoi deu co ty le dung tot"', '"Tất cả câu hỏi đều có tỷ lệ đúng tốt"'],

  // ScoreDistributionChart
  ['"Phan bo diem"', '"Phân bố điểm"'],
  ['"Chua co du lieu diem"', '"Chưa có dữ liệu điểm"'],
  ['"Trung binh"', '"Trung bình"'],
  ['"Trung vi"', '"Trung vị"'],
  ['"Do lech chuan"', '"Độ lệch chuẩn"'],
  ['"Cao nhat"', '"Cao nhất"'],
  ['"Thap nhat"', '"Thấp nhất"'],
  ['"Dat ("', '"Đạt ("'],
  ['"Khong dat ("', '"Không đạt ("'],

  // StudentResultsTable
  ['"Hoc sinh"', '"Học sinh"'],
  ['"Diem"', '"Điểm"'],
  ['"Trang thai"', '"Trạng thái"'],
  ['"Ma sinh vien"', '"Mã sinh viên"'],
  ['"Canh bao"', '"Cảnh báo"'],
  ['"Sinh vien"', '"Sinh viên"'],
  ['"Thoi gian"', '"Thời gian"'],
  ['"Warning"', '"Cảnh báo"'],
  ['"Hanh dong"', '"Hành động"'],
  ['"Da nop"', '"Đã nộp"'],
  ['"Tam dung"', '"Tạm dừng"'],
  ['"Dang thi"', '"Đang thi"'],
  ['"Khong ro"', '"Không rõ"'],
  ['" phut"', '" phút"'],
  ['" luot lam"', '" lượt làm"'],
  ['" luot"', '" lượt"'],
  ['" cau"', '" câu"'],
  ['" nguoi"', '" người"'],
  ['" hoc sinh"', '" học sinh"'],

  // AnalyticsHeader
  ['"Phan tich ket qua"', '"Phân tích kết quả"'],
  ['"Hoc ky nay"', '"Học kỳ này"'],
  ['"Tat ca ky thi"', '"Tất cả kỳ thi"'],
  ['"Tat ca"', '"Tất cả"'],
  ['"Tuan nay"', '"Tuần này"'],
  ['"Thang nay"', '"Tháng này"'],
  ['"Hoc ky nay"', '"Học kỳ này"'],
  ['"Chon ky thi..."', '"Chọn kỳ thi..."'],

  // ExamDeleteModal
  ['"Xoa de thi"', '"Xóa đề thi"'],
  ['"Xoa nhieu de thi"', '"Xóa nhiều đề thi"'],
  ['"Xoa de thi vinh vien"', '"Xóa đề thi vĩnh viễn"'],
  ['"Ban co chan muon xoa"', '"Bạn có chắc muốn xóa"'],
  ['"da chon"', '"đã chọn"'],
  ['"Dang xoa..."', '"Đang xóa..."'],
  ['"Hanh dong nay khong the hoan tac"', '"Hành động này không thể hoàn tác"'],
  ['"Tat ca cau hoi, luot thi va du lieu lien quan se bi xoa vinh vien"', '"Tất cả câu hỏi, lượt thi và dữ liệu liên quan sẽ bị xóa vĩnh viễn"'],

  // ExamDetailModal
  ['"Ma de"', '"Mã đề"'],
  ['"Thoi gian"', '"Thời gian"'],
  ['"Thoi luong"', '"Thời lượng"'],
  ['"Cau hoi"', '"Câu hỏi"'],
  ['"Nguoi tao"', '"Người tạo"'],
  ['"Mo ta"', '"Mô tả"'],
  ['"Dong"', '"Đóng"'],
  ['"Chinh sua"', '"Chỉnh sửa"'],
  ['"Nhan ban"', '"Nhân bản"'],
  ['"Bat"', '"Bật"'],
  ['"Tat"', '"Tắt"'],

  // TeacherIncidentReviewUpdatedMenu
  ["'Sarah Smith'", '"Nguyễn Văn A"'],
  ['"Sarah Smith"', '"Nguyễn Văn A"'],

  // ProctoringEditor
  ["'Duplicate IP'", '"IP trùng lặp"'],
  ['"Duplicate IP"', '"IP trùng lặp"'],

  // TeacherExamReviewSummary
  ['"De thi da chon"', '"Đề thi đã chọn"'],
  ['"Sinh vien khong ro"', '"Sinh viên không rõ"'],
  ['"Phan tich ket qua"', '"Phân tích kết quả"'],
  ['"Hoc sinh"', '"Học sinh"'],
  ['"Diem TB"', '"Điểm TB"'],
  ['"Chua co du lieu"', '"Chưa có dữ liệu"'],
  ['"Ty le dat"', '"Tỷ lệ đạt"'],
  ['"Ty le hoan thanh"', '"Tỷ lệ hoàn thành"'],
  ['"Canh bao"', '"Cảnh báo"'],
  ['"Sinh vien"', '"Sinh viên"'],
  ['"Ma sinh vien"', '"Mã sinh viên"'],

  // ResultDetailPanel
  ['"Thoi gian"', '"Thời gian"'],
  ['"Ket qua"', '"Kết quả"'],
  ['"Trang thai"', '"Trạng thái"'],
  ['"Trinh do"', '"Trình độ"'],
  ['"Duoc"', '"Được"'],
  ['"Chua"', '"Chưa"'],

  // Various
  ['"Dang phan tich..."', '"Đang phân tích..."'],
  ['"Tai bao cao"', '"Tải báo cáo"'],
  ['"In bao cao"', '"In báo cáo"'],
  ['"Tai PDF"', '"Tải PDF"'],
  ['"Phan tich tuong dong dap an"', '"Phân tích tương đồng đáp án"'],
  ['"Hien thi"', '"Hiển thị"'],
  ['"Xem chi tiet"', '"Xem chi tiết"'],
  ['"Hien "', '"Hiển "'],
  ['"cua "', '"của "'],
  ['"Tong "', '"Tổng "'],
  ['"Tren "', '"Trên "'],
  ['"Khong co "', '"Không có "'],
  ['"Khong tim thay "', '"Không tìm thấy "'],
  ['"Thu doi "', '"Thu gọn "'],
])

const fixedFiles = []
const fixedCount = []
const errors = []

function processVueFile(filePath) {
  let content = readFileSync(filePath, 'utf-8')
  const original = content
  let count = 0

  for (const [pattern, replacement] of FIXES) {
    if (content.includes(pattern)) {
      const re = new RegExp(pattern.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g')
      const matches = content.match(re)
      if (matches) count += matches.length
      content = content.replace(re, replacement)
    }
  }

  if (content !== original) {
    try {
      writeFileSync(filePath, content, 'utf-8')
      fixedFiles.push(relative(__dirname, filePath))
      fixedCount.push(count)
    } catch (e) {
      errors.push(`${filePath}: ${e.message}`)
    }
  }
}

function walkDir(dir) {
  const entries = readdirSync(dir)
  for (const entry of entries) {
    const full = join(dir, entry)
    const stat = statSync(full)
    if (stat.isDirectory()) {
      walkDir(full)
    } else if (extname(entry) === '.vue') {
      try {
        processVueFile(full)
      } catch (e) {
        errors.push(`${full}: ${e.message}`)
      }
    }
  }
}

walkDir(srcDir)

let totalFixed = fixedCount.reduce((a, b) => a + b, 0)
console.log(`\nFixed: ${fixedFiles.length} files (${totalFixed} replacements total)`)
console.log(`Errors: ${errors.length} files`)
if (fixedFiles.length > 0) {
  fixedFiles.slice(0, 20).forEach((f, i) => console.log(`  [${fixedCount[i]}] ${f}`))
  if (fixedFiles.length > 20) console.log(`  ... and ${fixedFiles.length - 20} more`)
}
if (errors.length > 0) {
  errors.forEach(e => console.log(`  Error: ${e}`))
}
