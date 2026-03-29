/**
 * Gom nhiều sự kiện resize thành một khung hình — tránh vòng lặp ResizeObserver ↔ echarts.resize().
 */
export function createRafThrottle(fn) {
  let id = null
  return () => {
    if (id != null) return
    id = requestAnimationFrame(() => {
      id = null
      fn()
    })
  }
}
