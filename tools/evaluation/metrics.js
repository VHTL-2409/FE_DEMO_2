import fs from 'node:fs'

const parseArgs = () => {
  const args = process.argv.slice(2)
  const out = { input: '' }
  for (let i = 0; i < args.length; i += 1) {
    const key = args[i]
    const value = args[i + 1]
    if (key === '--input') out.input = value
  }
  if (!out.input) {
    console.error('Usage: node tools/evaluation/metrics.js --input path/to/labeled.csv')
    process.exit(2)
  }
  return out
}

const splitCsvLine = (line) => {
  // Minimal CSV splitter handling quoted commas
  const out = []
  let cur = ''
  let inQuotes = false
  for (let i = 0; i < line.length; i += 1) {
    const ch = line[i]
    if (ch === '"') {
      const next = line[i + 1]
      if (inQuotes && next === '"') {
        cur += '"'
        i += 1
      } else {
        inQuotes = !inQuotes
      }
      continue
    }
    if (ch === ',' && !inQuotes) {
      out.push(cur)
      cur = ''
      continue
    }
    cur += ch
  }
  out.push(cur)
  return out
}

const toBool = (value) => String(value || '').trim().toLowerCase() === 'true'
const toLabel = (value) => {
  const v = String(value || '').trim()
  if (v === '1') return 1
  if (v === '0') return 0
  return null
}

const main = () => {
  const { input } = parseArgs()
  const raw = fs.readFileSync(input, 'utf8')
  const lines = raw.split(/\r?\n/).filter(Boolean)
  if (lines.length < 2) {
    console.error('CSV must have header + at least 1 row.')
    process.exit(2)
  }

  const header = splitCsvLine(lines[0]).map((h) => h.trim())
  const idxSuspicious = header.findIndex((h) => h === 'suspicious')
  const idxLabel = header.findIndex((h) => h === 'label')
  if (idxSuspicious < 0) {
    console.error('Missing column: suspicious')
    process.exit(2)
  }
  if (idxLabel < 0) {
    console.error('Missing column: label (0/1)')
    process.exit(2)
  }

  let tp = 0
  let fp = 0
  let tn = 0
  let fn = 0
  let skipped = 0

  for (const line of lines.slice(1)) {
    const cols = splitCsvLine(line)
    const pred = toBool(cols[idxSuspicious]) ? 1 : 0
    const label = toLabel(cols[idxLabel])
    if (label === null) {
      skipped += 1
      continue
    }
    if (pred === 1 && label === 1) tp += 1
    else if (pred === 1 && label === 0) fp += 1
    else if (pred === 0 && label === 0) tn += 1
    else if (pred === 0 && label === 1) fn += 1
  }

  const precision = tp + fp === 0 ? 0 : tp / (tp + fp)
  const recall = tp + fn === 0 ? 0 : tp / (tp + fn)
  const f1 = precision + recall === 0 ? 0 : (2 * precision * recall) / (precision + recall)
  const accuracy = tp + fp + tn + fn === 0 ? 0 : (tp + tn) / (tp + fp + tn + fn)

  const fmt = (x) => `${(x * 100).toFixed(2)}%`

  console.log('## Anti-cheat metrics')
  console.log(`- Rows used: ${tp + fp + tn + fn}`)
  if (skipped) console.log(`- Skipped (missing label): ${skipped}`)
  console.log(`- TP/FP/TN/FN: ${tp}/${fp}/${tn}/${fn}`)
  console.log(`- Precision: ${fmt(precision)}`)
  console.log(`- Recall:    ${fmt(recall)}`)
  console.log(`- F1:        ${fmt(f1)}`)
  console.log(`- Accuracy:  ${fmt(accuracy)}`)
}

main()

