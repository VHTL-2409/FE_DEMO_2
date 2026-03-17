import http from 'k6/http'
import { check, sleep } from 'k6'

// Usage:
//  k6 run tools/loadtest/k6_monitoring_events.js
// Or override:
//  k6 run -e BASE_URL=http://localhost:8082 -e EXAM_ID=1 -e USERNAME=student1 -e PASSWORD=123456 tools/loadtest/k6_monitoring_events.js

export const options = {
  scenarios: {
    monitoring_events: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 5 },
        { duration: '20s', target: 20 },
        { duration: '20s', target: 40 },
        { duration: '10s', target: 0 }
      ],
      gracefulRampDown: '10s'
    }
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<1200']
  }
}

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082'
const EXAM_ID = Number(__ENV.EXAM_ID || '1')
const USERNAME = __ENV.USERNAME || 'student1'
const PASSWORD = __ENV.PASSWORD || '123456'

const jsonHeaders = (token) => ({
  headers: {
    'Content-Type': 'application/json',
    Authorization: token ? `Bearer ${token}` : ''
  }
})

const login = () => {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }),
    jsonHeaders()
  )
  check(res, { 'login status 200': (r) => r.status === 200 })
  const body = res.json()
  // Compatible with common shapes: {data:{token}} or {token}
  return body?.data?.token || body?.token || ''
}

const startAttempt = (token) => {
  const res = http.post(`${BASE_URL}/api/exams/${EXAM_ID}/attempts/start`, null, {
    headers: { Authorization: `Bearer ${token}` }
  })
  check(res, { 'start attempt 200': (r) => r.status === 200 })
  const body = res.json()
  return body?.data?.attemptId || body?.attemptId
}

const randomEvent = () => {
  const events = [
    'TAB_SWITCH',
    'BLUR',
    'COPY_PASTE',
    'DEVTOOLS_OPEN',
    'IDLE_TIME',
    'EXIT_FULLSCREEN'
  ]
  return events[Math.floor(Math.random() * events.length)]
}

export default function () {
  const token = login()
  if (!token) return

  const attemptId = startAttempt(token)
  if (!attemptId) return

  // Burst a few events per VU iteration
  const burstCount = 3 + Math.floor(Math.random() * 6)
  for (let i = 0; i < burstCount; i += 1) {
    const eventType = randomEvent()
    const res = http.post(
      `${BASE_URL}/api/attempts/${attemptId}/monitoring/events`,
      JSON.stringify({
        eventType,
        details: `k6:${eventType}`
      }),
      jsonHeaders(token)
    )
    check(res, { 'event 200': (r) => r.status === 200 })
    sleep(0.2 + Math.random() * 0.4)
  }

  sleep(0.8 + Math.random() * 1.2)
}

