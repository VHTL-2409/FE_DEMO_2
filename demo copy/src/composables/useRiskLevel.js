/**
 * Shared risk level utilities for proctoring.
 * Use this composable instead of duplicating risk-band logic across components.
 *
 * Usage:
 *   import { useRiskLevel } from '../../composables/useRiskLevel'
 *   const { resolveBand, resolveLevel, colorClass, icon } = useRiskLevel()
 */
export function useRiskLevel() {
  const BAND_ORDER = { CRITICAL: 0, HIGH_RISK: 1, SUSPICIOUS: 2, CLEAN: 3 }

  /**
   * @param {number|string} score - Risk score (0-100)
   * @returns {'CRITICAL'|'HIGH_RISK'|'SUSPICIOUS'|'CLEAN'}
   */
  const resolveBand = (score) => {
    const s = Number(score || 0)
    if (s >= 81) return 'CRITICAL'
    if (s >= 61) return 'HIGH_RISK'
    if (s >= 31) return 'SUSPICIOUS'
    return 'CLEAN'
  }

  /**
   * @param {number|string} score
   * @returns {'critical'|'high_risk'|'suspicious'|'clean'}
   */
  const resolveLevel = (score) => {
    const band = resolveBand(score)
    const map = { CRITICAL: 'critical', HIGH_RISK: 'high_risk', SUSPICIOUS: 'suspicious', CLEAN: 'clean' }
    return map[band]
  }

  /**
   * Risk band for sort priority (lower = more urgent).
   */
  const bandOrder = (band) => BAND_ORDER[band] ?? 4

  /**
   * @param {number|string} score
   * @returns {number} 0-100 clamped
   */
  const clampScore = (score) => Math.min(100, Math.max(0, Number(score || 0)))

  /**
   * CSS class suffix for variant colors.
   * @param {number|string} score
   * @returns {'danger'|'warning'|'caution'|'success'}
   */
  const variant = (score) => {
    const band = resolveBand(score)
    const map = { CRITICAL: 'danger', HIGH_RISK: 'warning', SUSPICIOUS: 'caution', CLEAN: 'success' }
    return map[band]
  }

  /**
   * CSS color class for use with --ds-* tokens.
   * @param {number|string} score
   * @returns {'ds-danger'|'ds-warning'|'ds-caution'|'ds-success'}
   */
  const colorClass = (score) => {
    const band = resolveBand(score)
    const map = { CRITICAL: 'ds-danger', HIGH_RISK: 'ds-warning', SUSPICIOUS: 'ds-caution', CLEAN: 'ds-success' }
    return map[band]
  }

  /**
   * Human-readable label.
   * @param {number|string} score
   * @returns {string}
   */
  const label = (score) => {
    const band = resolveBand(score)
    const map = {
      CRITICAL: 'Nghiêm trọng',
      HIGH_RISK: 'Nguy cơ cao',
      SUSPICIOUS: 'Đáng nghi',
      CLEAN: 'An toàn'
    }
    return map[band]
  }

  /**
   * Lucide icon name for the risk level.
   * @param {number|string} score
   * @returns {string}
   */
  const icon = (score) => {
    const band = resolveBand(score)
    const map = { CRITICAL: 'gpp_bad', HIGH_RISK: 'warning', SUSPICIOUS: 'report', CLEAN: 'check_circle' }
    return map[band]
  }

  /**
   * Whether this score should trigger a critical alert (>=81).
   * @param {number|string} score
   * @returns {boolean}
   */
  const isCritical = (score) => resolveBand(score) === 'CRITICAL'

  /**
   * Whether this score is at or above the suspicious threshold (>30).
   * @param {number|string} score
   * @returns {boolean}
   */
  const hasRisk = (score) => resolveBand(score) !== 'CLEAN'

  return { resolveBand, resolveLevel, bandOrder, clampScore, variant, colorClass, label, icon, isCritical, hasRisk }
}
