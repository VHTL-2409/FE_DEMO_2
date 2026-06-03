import { describe, expect, it } from 'vitest'

import { isCameraRelatedSignal, normalizeSignalType, uniqueSignalTypes } from './proctorSignalTypes'

describe('proctorSignalTypes', () => {
  it('normalizes spoofing aliases to canonical signal types', () => {
    expect(normalizeSignalType('photo_attack')).toBe('PRINTED_PHOTO')
    expect(normalizeSignalType('video_replay')).toBe('SCREEN_REPLAY')
    expect(normalizeSignalType('display_attack')).toBe('SCREEN_DISPLAY')
    expect(normalizeSignalType('flat_face')).toBe('FLAT_IMAGE')
    expect(normalizeSignalType('model_spoof')).toBe('DEEPFAKE')
    expect(normalizeSignalType('low_live_score')).toBe('LOW_LIVENESS')
  })

  it('treats canonical spoofing signals as camera related and dedupes aliases', () => {
    expect(isCameraRelatedSignal('video_replay')).toBe(true)
    expect(uniqueSignalTypes(['screen_replay', 'video_replay', 'photo_attack'])).toEqual([
      'SCREEN_REPLAY',
      'PRINTED_PHOTO'
    ])
  })
})
