import http from 'k6/http'
import { check, sleep } from 'k6'

const baseUrl = __ENV.BASE_URL || 'http://localhost:8082'
const authToken = __ENV.AUTH_TOKEN || ''
const attemptIds = String(__ENV.ATTEMPT_IDS || '')
  .split(',')
  .map((value) => Number(value.trim()))
  .filter((value) => Number.isFinite(value) && value > 0)

if (!authToken) {
  throw new Error('AUTH_TOKEN is required')
}
if (!attemptIds.length) {
  throw new Error('ATTEMPT_IDS must contain at least one attempt id')
}

export const options = {
  scenarios: {
    proctoring: {
      executor: 'ramping-vus',
      startVUs: 20,
      stages: [
        { duration: '30s', target: 100 },
        { duration: '1m', target: 250 },
        { duration: '1m', target: 500 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '10s'
    }
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<1200']
  }
}

const headers = {
  Authorization: `Bearer ${authToken}`,
  'Content-Type': 'application/json'
}

function attemptIdForVu() {
  return attemptIds[(__VU - 1) % attemptIds.length]
}

export default function () {
  const attemptId = attemptIdForVu()
  const heartbeatPayload = JSON.stringify({
    fullscreen: true,
    visibility: 'VISIBLE',
    cameraOn: true,
    micOn: true,
    deviceFingerprint: `k6-${__VU}`,
    screenMetrics: {
      width: 1440,
      height: 900,
      pixelRatio: 1.0
    }
  })

  const batchPayload = JSON.stringify({
    sequence: (__ITER * 10) + 1,
    deviceFingerprint: `k6-${__VU}`,
    browserContext: {
      userAgent: 'k6-load-test',
      language: 'vi-VN',
      platform: 'load-test'
    },
    events: [
      {
        eventType: 'HEARTBEAT',
        occurredAt: new Date().toISOString(),
        details: 'k6 synthetic heartbeat'
      },
      {
        eventType: 'WINDOW_FOCUS',
        occurredAt: new Date().toISOString(),
        details: 'k6 focus confirmation'
      }
    ]
  })

  const heartbeatResponse = http.post(`${baseUrl}/api/v1/proctor/sessions/${attemptId}/heartbeat`, heartbeatPayload, { headers })
  check(heartbeatResponse, {
    'heartbeat status 200': (response) => response.status === 200
  })

  const batchResponse = http.post(`${baseUrl}/api/v1/proctor/sessions/${attemptId}/events/batch`, batchPayload, { headers })
  check(batchResponse, {
    'event batch status 200': (response) => response.status === 200
  })

  sleep(1)
}
