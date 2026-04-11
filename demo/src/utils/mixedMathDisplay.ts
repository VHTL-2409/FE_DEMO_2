/**
 * Hỗ trợ hiển thị đề toán + tiếng Việt: không coi cả câu là một khối LaTeX.
 */

const VIETNAMESE_LATIN_RE =
  /[àáảãạăằẳẵặâầẩẫậèéẻẽẹêềểễệìíỉĩịòóỏõọôồổốộơờởỡợùúủũụưừửữựỳýỷỹỵđĐ]/u

export function containsVietnameseLatin(text: string): boolean {
  if (text == null || text === '') {
    return false
  }
  return VIETNAMESE_LATIN_RE.test(text)
}

function stripOuterInlineDollars(s: string): string {
  const t = s.trim()
  if (t.length >= 2 && t.startsWith('$') && t.endsWith('$') && t.split('$').length === 3) {
    return t.slice(1, -1).trim()
  }
  return t
}

function compactForCompare(s: string): string {
  return stripOuterInlineDollars(s).replace(/\s+/g, '')
}

function spaceCount(s: string): number {
  return (stripOuterInlineDollars(s).match(/ /g) || []).length
}

/** Hai dòng liền nhau: một $...$ một plain giống nội dung → giữ bản có nhiều khoảng trắng hơn (đọc được). */
export function collapseAdjacentDuplicateMathPlainLines(text: string): string {
  if (text == null || text === '' || !text.includes('\n')) {
    return text
  }
  const rawLines = text.split('\n')
  const out: string[] = []
  let i = 0
  while (i < rawLines.length) {
    const cur = rawLines[i]?.trim() ?? ''
    const nxt = rawLines[i + 1]?.trim() ?? ''
    if (cur.length > 0 && nxt.length > 0 && compactForCompare(cur) === compactForCompare(nxt)) {
      const pick = spaceCount(nxt) > spaceCount(cur) ? nxt : cur
      out.push(pick)
      i += 2
      continue
    }
    out.push(rawLines[i] ?? '')
    i += 1
  }
  return out.join('\n')
}
