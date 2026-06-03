import { describe, expect, it } from 'vitest'
import { detectMultiMonitor } from './multiMonitorDetection'

const baseWindow = (overrides = {}) => ({
  screen: {
    width: 1920,
    height: 1080,
    availWidth: 1920,
    availHeight: 1040,
    isExtended: false
  },
  screenX: 0,
  screenY: 0,
  outerWidth: 1200,
  outerHeight: 800,
  innerWidth: 1200,
  innerHeight: 760,
  ...overrides
})

const grantedNavigator = {
  permissions: {
    query: async () => ({ state: 'granted' })
  }
}

describe('detectMultiMonitor', () => {
  it('uses Window Management API when screen details are available', async () => {
    const win = baseWindow({
      getScreenDetails: async () => ({
        currentScreen: { left: 0, top: 0, width: 1920, height: 1080, isPrimary: true },
        screens: [
          { left: 0, top: 0, width: 1920, height: 1080, isPrimary: true },
          { left: 1920, top: 0, width: 1920, height: 1080, isPrimary: false }
        ]
      })
    })

    const result = await detectMultiMonitor({ win, nav: grantedNavigator })

    expect(result.detected).toBe(true)
    expect(result.evidence.detectionMethod).toBe('window_management_api')
    expect(result.evidence.screenCount).toBe(2)
    expect(result.evidence.permissionState).toBe('granted')
  })

  it('falls back to screen.isExtended when screen details are unavailable', async () => {
    const win = baseWindow({
      screen: { width: 1920, height: 1080, availWidth: 1920, availHeight: 1040, isExtended: true }
    })

    const result = await detectMultiMonitor({ win, nav: {} })

    expect(result.detected).toBe(true)
    expect(result.evidence.detectionMethod).toBe('screen_is_extended')
  })

  it('falls back to geometry when the window sits outside the primary screen', async () => {
    const win = baseWindow({ screenX: 2200, screenY: 0 })

    const result = await detectMultiMonitor({ win, nav: {} })

    expect(result.detected).toBe(true)
    expect(result.evidence.detectionMethod).toBe('geometry_fallback')
    expect(result.evidence.geometryEvidence.offsetFromPrimary).toBe(true)
  })

  it('does not crash when getScreenDetails is denied', async () => {
    const win = baseWindow({
      getScreenDetails: async () => {
        throw new DOMException('denied', 'NotAllowedError')
      }
    })

    const result = await detectMultiMonitor({ win, nav: grantedNavigator })

    expect(result.detected).toBe(false)
    expect(result.evidence.windowManagementError).toBe('NotAllowedError')
    expect(result.evidence.detectionMethod).toBe('none')
  })
})
