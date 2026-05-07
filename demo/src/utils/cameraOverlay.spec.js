import { describe, expect, it } from 'vitest'

import { normalizeVisualOverlay, projectCameraOverlay } from './cameraOverlay'

describe('cameraOverlay', () => {
  it('normalizes camel and snake case overlay fields', () => {
    const overlay = normalizeVisualOverlay({
      image_width: 160,
      image_height: 120,
      face_boxes: [{ x: 10, y: 12, w: 40, h: 50 }],
      eye_boxes: [{ left: 20, top: 24, width: 12, height: 8 }],
      pupil_points: [{ x: 26, y: 28 }],
      status: 'tracking',
      label: 'Đang theo dõi',
      tone: 'SUCCESS'
    })

    expect(overlay).toMatchObject({
      imageWidth: 160,
      imageHeight: 120,
      status: 'TRACKING',
      label: 'Đang theo dõi',
      tone: 'success'
    })
    expect(overlay.faceBoxes).toEqual([{ x: 10, y: 12, width: 40, height: 50, side: null }])
    expect(overlay.eyeBoxes).toEqual([{ x: 20, y: 24, width: 12, height: 8, side: null }])
    expect(overlay.pupilPoints).toEqual([{ x: 26, y: 28, side: null }])
  })

  it('projects overlay coordinates through object-fit cover', () => {
    const projected = projectCameraOverlay({
      imageWidth: 100,
      imageHeight: 50,
      faceBoxes: [{ x: 10, y: 5, width: 20, height: 10 }],
      eyeBoxes: [],
      pupilPoints: [{ x: 35, y: 18 }],
      status: 'TRACKING',
      label: 'Đang theo dõi',
      tone: 'success'
    }, 100, 100)

    expect(projected.scale).toBe(2)
    expect(projected.offsetX).toBe(-50)
    expect(projected.offsetY).toBe(0)
    expect(projected.faceBoxes[0].style).toEqual({
      left: '-30px',
      top: '10px',
      width: '40px',
      height: '20px'
    })
    expect(projected.pupilPoints[0].style).toEqual({
      left: '20px',
      top: '36px'
    })
  })

  it('mirrors x coordinates for selfie previews', () => {
    const projected = projectCameraOverlay({
      imageWidth: 100,
      imageHeight: 50,
      faceBoxes: [{ x: 10, y: 5, width: 20, height: 10 }],
      eyeBoxes: [],
      pupilPoints: [{ x: 35, y: 18 }],
      status: 'TRACKING',
      label: 'Đang theo dõi',
      tone: 'success'
    }, 200, 100, { mirrorX: true })

    expect(projected.faceBoxes[0].style.left).toBe('140px')
    expect(projected.pupilPoints[0].style.left).toBe('130px')
  })
})
