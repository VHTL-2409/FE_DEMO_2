/**
 * Fix Vietnamese text issues across all Vue files
 */
import { readFileSync, writeFileSync, readdirSync, statSync } from 'fs'
import { join, relative, extname } from 'path'
import { fileURLToPath } from 'url'
import { dirname } from 'path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const srcDir = join(__dirname, 'src')

const FIXES = [
  // Analytics
  { pattern: '"Tat ca cau hoi deu co ty le dung tot"', replacement: '"Tất cả câu hỏi đều có tỷ lệ đúng tốt"' },
  { pattern: '"Phan bo diem"', replacement: '"Phân bố điểm"' },
  { pattern: '"Chua co du lieu diem"', replacement: '"Chưa có dữ liệu điểm"' },
  { pattern: '"Hoc ky nay"', replacement: '"Học kỳ này"' },
  { pattern: '"Tat ca ky thi"', replacement: '"Tất cả kỳ thi"' },
  { pattern: '"Phan tich ket qua"', replacement: '"Phân tích kết quả"' },

  // Modal / Detail
  { pattern: '"Mo ta"', replacement: '"Mô tả"' },
  { pattern: '"Dong"', replacement: '"Đóng"' },
  { pattern: '"Chinh sua"', replacement: '"Chỉnh sửa"' },
  { pattern: '"Nhan ban"', replacement: '"Nhân bản"' },
  { pattern: '"Xem chi tiet"', replacement: '"Xem chi tiết"' },

  // Score chart
  { pattern: '"Trung binh"', replacement: '"Trung bình"' },
  { pattern: '"Trung vi"', replacement: '"Trung vị"' },
  { pattern: '"Do lech chuan"', replacement: '"Độ lệch chuẩn"' },
  { pattern: '"Cao nhat"', replacement: '"Cao nhất"' },
  { pattern: '"Thap nhat"', replacement: '"Thấp nhất"' },
  { pattern: '"Khong dat"', replacement: '"Không đạt"' },
  { pattern: '"Dat "', replacement: '"Đạt "' },

  // Student table
  { pattern: '"Hoc sinh"', replacement: '"Học sinh"' },
  { pattern: '"Diem"', replacement: '"Điểm"' },
  { pattern: '"Trang thai"', replacement: '"Trạng thái"' },
  { pattern: '"Ma sinh vien"', replacement: '"Mã sinh viên"' },
  { pattern: '"Canh bao"', replacement: '"Cảnh báo"' },
  { pattern: '"Sinh vien"', replacement: '"Sinh viên"' },
  { pattern: '"Thoi gian"', replacement: '"Thời gian"' },
  { pattern: '"Warning"', replacement: '"Cảnh báo"' },
  { pattern: '"Hanh dong"', replacement: '"Hành động"' },

  // Incident review
  { pattern: "'Sarah Smith'", replacement: '"Nguyễn Văn A"' },
  { pattern: '"Sarah Smith"', replacement: '"Nguyễn Văn A"' },
  { pattern: '"Giao vien"', replacement: '"Giảng viên"' },

  // Proctoring
  { pattern: "'Duplicate IP'", replacement: '"IP trùng lặp"' },
  { pattern: '"Duplicate IP"', replacement: '"IP trùng lặp"' },

  // Misc labels
  { pattern: '"Dang phan tich..."', replacement: '"Đang phân tích..."' },
  { pattern: '"Tai bao cao"', replacement: '"Tải báo cáo"' },
  { pattern: '"Phan tich tuong dong dap an"', replacement: '"Phân tích tương đồng đáp án"' },
  { pattern: '"Hien "', replacement: '"Hiển "' },
  { pattern: '"cua "', replacement: '"của "' },
  { pattern: '"Tong "' , replacement: '"Tổng "' },
  { pattern: '"Tren "' , replacement: '"Trên "' },
  { pattern: '"Khong co "' , replacement: '"Không có "' },
  { pattern: '"Khong tim thay "', replacement: '"Không tìm thấy "' },
  { pattern: '"Thu doi "' , replacement: '"Thu gọn "' },
  { pattern: '"cau khac"', replacement: '"câu khác"' },
  { pattern: '"cau hoi"', replacement: '"câu hỏi"' },
  { pattern: '"Top cau hoi kho"', replacement: '"Top câu hỏi khó"' },
  { pattern: '"Ty le sai cao nhat"', replacement: '"Tỷ lệ sai cao nhất"' },
  { pattern: '"Ty le sai"', replacement: '"Tỷ lệ sai"' },
  { pattern: '"Xem tat ca cau hoi"', replacement: '"Xem tất cả câu hỏi"' },
  { pattern: '"Thoi gian bat dau"', replacement: '"Thời gian bắt đầu"' },
  { pattern: '"Ket qua"', replacement: '"Kết quả"' },
  { pattern: '"So luot canh bao"', replacement: '"Số lượt cảnh báo"' },
  { pattern: '"Canh bao dau tien"', replacement: '"Cảnh báo đầu tiên"' },
  { pattern: '"Canh bao gan nhat"', replacement: '"Cảnh báo gần nhất"' },
  { pattern: '"In bao cao"', replacement: '"In báo cáo"' },
  { pattern: '"Tai PDF"', replacement: '"Tải PDF"' },
  { pattern: '"Ty le sai: "', replacement: '"Tỷ lệ sai: "' },
  { pattern: '"Benh nhat"', replacement: '"Thấp nhất"' },
  { pattern: '"% sai"', replacement: '"% sai"' },
  { pattern: '"Hien thi"', replacement: '"Hiển thị"' },

  // More score/chart labels
  { pattern: '"Trung binh"', replacement: '"Trung bình"' },
  { pattern: '"Trung vi"', replacement: '"Trung vị"' },

  // Duration formatting
  { pattern: '" phut"', replacement: '" phút"' },
  { pattern: '" luot lam"', replacement: '" lượt làm"' },
  { pattern: '" luot"', replacement: '" lượt"' },
  { pattern: '" luot thi"', replacement: '" lượt thi"' },
  { pattern: '" hoc sinh"', replacement: '" học sinh"' },
  { pattern: '" nguoi"', replacement: '" người"' },
  { pattern: '" cau"', replacement: '" câu"' },

  // Status labels
  { pattern: '"Da nop"', replacement: '"Đã nộp"' },
  { pattern: '"Tam dung"', replacement: '"Tạm dừng"' },
  { pattern: '"Dang thi"', replacement: '"Đang thi"' },
  { pattern: '"Khong ro"', replacement: '"Không rõ"' },
  { pattern: '"Dang xem xet"', replacement: '"Đang xem xét"' },
  { pattern: '"Da gan co"', replacement: '"Đã gắn cờ"' },
  { pattern: '"Dang dieu tra"', replacement: '"Đang điều tra"' },

  // Empty states
  { pattern: '"Khong co ket qua nao"', replacement: '"Không có kết quả nào"' },
  { pattern: '"Thu doi tu khoa tim kiem"', replacement: '"Thử đổi từ khóa tìm kiếm"' },
  { pattern: '"Chua co du lieu hoc sinh"', replacement: '"Chưa có dữ liệu học sinh"' },

  // Misc
  { pattern: '"In "', replacement: '"In "' },
]

const fixedFiles = []
const errors = []

function processVueFile(filePath) {
  let content = readFileSync(filePath, 'utf-8')
  const original = content

  for (const fix of FIXES) {
    // Handle both string literals (with single or double quotes)
    const escapedPattern = fix.pattern.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    const reSingle = new RegExp(escapedPattern.replace(/^'|'$/g, ''), 'g')
    const reDouble = new RegExp(escapedPattern.replace(/^"|"$/g, ''), 'g')

    // Try single-quoted
    if (content.includes(fix.pattern)) {
      content = content.split(fix.pattern).join(fix.replacement)
    }
  }

  if (content !== original) {
    try {
      writeFileSync(filePath, content, 'utf-8')
      fixedFiles.push(relative(__dirname, filePath))
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

console.log(`\nFixed: ${fixedFiles.length} files`)
console.log(`Errors: ${errors.length} files`)
if (fixedFiles.length > 0) {
  fixedFiles.slice(0, 20).forEach(f => console.log(`  ${f}`))
  if (fixedFiles.length > 20) console.log(`  ... and ${fixedFiles.length - 20} more`)
}
if (errors.length > 0) {
  errors.forEach(e => console.log(`  Error: ${e}`))
}
