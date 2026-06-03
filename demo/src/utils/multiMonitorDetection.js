const numberOrNull = (value) => (Number.isFinite(value) ? value : null)

const screenSnapshot = (screen = {}) => ({
  screenWidth: numberOrNull(screen.width),
  screenHeight: numberOrNull(screen.height),
  availWidth: numberOrNull(screen.availWidth),
  availHeight: numberOrNull(screen.availHeight),
  screenExtended: screen.isExtended === true
})

const windowGeometry = (win) => ({
  screenX: numberOrNull(Number.isFinite(win.screenX) ? win.screenX : win.screenLeft),
  screenY: numberOrNull(Number.isFinite(win.screenY) ? win.screenY : win.screenTop),
  outerWidth: numberOrNull(win.outerWidth),
  outerHeight: numberOrNull(win.outerHeight),
  innerWidth: numberOrNull(win.innerWidth),
  innerHeight: numberOrNull(win.innerHeight)
})

const normalizeScreenDetails = (details) => {
  const screens = Array.from(details?.screens || []).map((item) => ({
    left: numberOrNull(item.left),
    top: numberOrNull(item.top),
    width: numberOrNull(item.width),
    height: numberOrNull(item.height),
    availWidth: numberOrNull(item.availWidth),
    availHeight: numberOrNull(item.availHeight),
    isPrimary: item.isPrimary === true,
    isInternal: item.isInternal === true,
    label: typeof item.label === 'string' ? item.label : ''
  }))
  const current = details?.currentScreen ? {
    left: numberOrNull(details.currentScreen.left),
    top: numberOrNull(details.currentScreen.top),
    width: numberOrNull(details.currentScreen.width),
    height: numberOrNull(details.currentScreen.height),
    isPrimary: details.currentScreen.isPrimary === true,
    isInternal: details.currentScreen.isInternal === true,
    label: typeof details.currentScreen.label === 'string' ? details.currentScreen.label : ''
  } : null
  return { screens, currentScreen: current }
}

const queryWindowManagementPermission = async (nav) => {
  if (!nav?.permissions?.query) return 'unsupported'
  for (const name of ['window-management', 'window-placement']) {
    try {
      const status = await nav.permissions.query({ name })
      if (status?.state) return status.state
    } catch {
      // Try the legacy permission name before falling back.
    }
  }
  return 'unsupported'
}

export const detectMultiMonitor = async ({ win = window, nav = navigator } = {}) => {
  const screen = win?.screen || {}
  const baseEvidence = {
    ...screenSnapshot(screen),
    ...windowGeometry(win || {}),
    permissionState: 'unknown',
    detectionMethod: 'none',
    screenCount: null,
    currentScreen: null,
    screens: [],
    geometryEvidence: {}
  }

  baseEvidence.permissionState = await queryWindowManagementPermission(nav)

  if (typeof win?.getScreenDetails === 'function') {
    try {
      const details = await win.getScreenDetails()
      const normalized = normalizeScreenDetails(details)
      const screenCount = normalized.screens.length
      return {
        detected: screenCount > 1,
        evidence: {
          ...baseEvidence,
          detectionMethod: 'window_management_api',
          screenCount,
          currentScreen: normalized.currentScreen,
          screens: normalized.screens
        }
      }
    } catch (error) {
      baseEvidence.windowManagementError = error?.name || error?.message || 'getScreenDetails_failed'
    }
  }

  if (screen.isExtended === true) {
    return {
      detected: true,
      evidence: {
        ...baseEvidence,
        detectionMethod: 'screen_is_extended',
        screenCount: null
      }
    }
  }

  const sw = screen.width || 0
  const sh = screen.height || 0
  const aw = screen.availWidth || 0
  const ah = screen.availHeight || 0
  const screenX = Number.isFinite(win?.screenX) ? win.screenX : (Number.isFinite(win?.screenLeft) ? win.screenLeft : 0)
  const screenY = Number.isFinite(win?.screenY) ? win.screenY : (Number.isFinite(win?.screenTop) ? win.screenTop : 0)
  const geometryEvidence = {
    widthGap: sw > 0 && aw > 0 ? sw - aw : 0,
    heightGap: sh > 0 && ah > 0 ? sh - ah : 0,
    offsetFromPrimary: Math.abs(screenX) > Math.max(sw * 0.25, 160) || Math.abs(screenY) > Math.max(sh * 0.25, 160)
  }
  const detected = Boolean(
    (sw > 0 && aw > 0 && geometryEvidence.widthGap > 100)
    || (sh > 0 && ah > 0 && geometryEvidence.heightGap > 100)
    || geometryEvidence.offsetFromPrimary
  )

  return {
    detected,
    evidence: {
      ...baseEvidence,
      detectionMethod: detected ? 'geometry_fallback' : 'none',
      geometryEvidence
    }
  }
}
