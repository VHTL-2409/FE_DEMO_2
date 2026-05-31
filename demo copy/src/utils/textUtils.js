/**
 * Normalize a string: remove Vietnamese diacritics, lowercase, strip all spaces.
 * e.g. "Nguyễn Văn A" → "nguyenvana"
 */
export function removeAccents(str) {
  if (!str) return ''
  return str.normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd').replace(/Đ/g, 'D')
    .toLowerCase()
    .replace(/\s+/g, '')
}
