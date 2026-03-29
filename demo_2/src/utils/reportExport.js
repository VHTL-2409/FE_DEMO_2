/**
 * Xuất báo cáo CSV từ dữ liệu bảng
 */
export function exportToCsv(rows, columns, filename = 'report.csv') {
  const header = columns.map((c) => c.label || c.key).join(',')
  const escape = (val) => {
    const s = String(val ?? '')
    if (s.includes(',') || s.includes('"') || s.includes('\n')) {
      return `"${s.replace(/"/g, '""')}"`
    }
    return s
  }
  const lines = [header]
  for (const row of rows) {
    const values = columns.map((c) => escape(row[c.key]))
    lines.push(values.join(','))
  }
  const csv = '\uFEFF' + lines.join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
