function firstDefined(...values) {
  return values.find(value => value !== undefined && value !== null)
}

function toNumber(value) {
  if (value === undefined || value === null || value === '') return null
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : null
}

function normalizeBox(box) {
  if (!box) return null

  let x
  let y
  let width
  let height
  let side = null
  if (Array.isArray(box) && box.length >= 4) {
    [x, y, width, height] = box
  } else if (typeof box === 'object') {
    x = firstDefined(box.x, box.left)
    y = firstDefined(box.y, box.top)
    width = firstDefined(box.width, box.w)
    height = firstDefined(box.height, box.h)
    side = box.side ?? null
  } else {
    return null
  }

  x = toNumber(x)
  y = toNumber(y)
  width = toNumber(width)
  height = toNumber(height)

  if (x === null || y === null || width === null || height === null) return null
  if (width <= 0 || height <= 0) return null

  return {
    x,
    y,
    width,
    height,
    side
  }
}

function normalizePoint(point) {
  if (!point) return null

  let x
  let y
  let side = null
  if (Array.isArray(point) && point.length >= 2) {
    [x, y] = point
  } else if (typeof point === 'object') {
    x = firstDefined(point.x, point.left)
    y = firstDefined(point.y, point.top)
    side = point.side ?? null
  } else {
    return null
  }

  x = toNumber(x)
  y = toNumber(y)
  if (x === null || y === null) return null

  return {
    x,
    y,
    side
  }
}

function normalizeBoxList(list) {
  return Array.isArray(list)
    ? list.map(normalizeBox).filter(Boolean)
    : []
}

function normalizePointList(list) {
  return Array.isArray(list)
    ? list.map(normalizePoint).filter(Boolean)
    : []
}

export function normalizeVisualOverlay(source) {
  if (!source || typeof source !== 'object') return null

  const faceBoxes = normalizeBoxList(firstDefined(source.faceBoxes, source.face_boxes, source.faceLocations, source.face_locations))
  const eyeBoxes = normalizeBoxList(firstDefined(source.eyeBoxes, source.eye_boxes))
  const pupilPoints = normalizePointList(firstDefined(source.pupilPoints, source.pupil_points))

  return {
    imageWidth: toNumber(firstDefined(source.imageWidth, source.image_width)) ?? 0,
    imageHeight: toNumber(firstDefined(source.imageHeight, source.image_height)) ?? 0,
    faceBoxes,
    eyeBoxes,
    pupilPoints,
    status: String(firstDefined(source.status, '') || '').toUpperCase(),
    label: String(firstDefined(source.label, '') || ''),
    tone: String(firstDefined(source.tone, 'success') || 'success').toLowerCase()
  }
}

function buildProjectedBox(box, scale, offsetX, offsetY, mirrorX, imageWidth) {
  const sourceX = mirrorX ? imageWidth - (box.x + box.width) : box.x
  return {
    ...box,
    style: {
      left: `${offsetX + (sourceX * scale)}px`,
      top: `${offsetY + (box.y * scale)}px`,
      width: `${box.width * scale}px`,
      height: `${box.height * scale}px`
    }
  }
}

function buildProjectedPoint(point, scale, offsetX, offsetY, mirrorX, imageWidth) {
  const sourceX = mirrorX ? imageWidth - point.x : point.x
  return {
    ...point,
    style: {
      left: `${offsetX + (sourceX * scale)}px`,
      top: `${offsetY + (point.y * scale)}px`
    }
  }
}

export function projectCameraOverlay(source, containerWidth, containerHeight, options = {}) {
  const normalized = normalizeVisualOverlay(source)
  if (!normalized) return null

  const width = Number(containerWidth) || 0
  const height = Number(containerHeight) || 0
  if (width <= 0 || height <= 0 || normalized.imageWidth <= 0 || normalized.imageHeight <= 0) {
    return null
  }

  const scale = Math.max(width / normalized.imageWidth, height / normalized.imageHeight)
  const renderedWidth = normalized.imageWidth * scale
  const renderedHeight = normalized.imageHeight * scale
  const offsetX = (width - renderedWidth) / 2
  const offsetY = (height - renderedHeight) / 2
  const mirrorX = options.mirrorX === true

  return {
    ...normalized,
    scale,
    renderedWidth,
    renderedHeight,
    offsetX,
    offsetY,
    faceBoxes: normalized.faceBoxes.map(box => buildProjectedBox(box, scale, offsetX, offsetY, mirrorX, normalized.imageWidth)),
    eyeBoxes: normalized.eyeBoxes.map(box => buildProjectedBox(box, scale, offsetX, offsetY, mirrorX, normalized.imageWidth)),
    pupilPoints: normalized.pupilPoints.map(point => buildProjectedPoint(point, scale, offsetX, offsetY, mirrorX, normalized.imageWidth))
  }
}
