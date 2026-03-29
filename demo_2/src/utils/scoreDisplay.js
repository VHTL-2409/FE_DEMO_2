/**
 * Chuẩn hóa điểm hiển thị dạng x,x trên thang 10.
 * Backend trả `score` là tổng trọng số; nhiều đề chuẩn hóa tổng 100 → chia 10.
 * Nếu ≤ 10 coi như đã trên thang 10.
 */
export function scoreOnTenDisplay(raw) {
  if (raw == null || raw === '') return '—'
  const n = Number(raw)
  if (Number.isNaN(n)) return '—'
  if (n > 10) return (n / 10).toFixed(1)
  return n.toFixed(1)
}
