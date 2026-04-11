// User types
export interface User {
  username: string
  roles: Role[]
}

export type Role = 'ADMIN' | 'TEACHER' | 'STUDENT'

export interface AuthSession {
  token: string | null
  user: User | null
  isAuthenticated: boolean
  isBootstrapped: boolean
}

// API types
export interface ApiResponse<T> {
  success: boolean
  data?: T
  message?: string
}

export interface ApiError {
  message: string
  error?: string
  timestamp?: string
}

// Auth types
export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  username: string
  roles: Role[]
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
}

export interface RegisterResponse {
  token: string | null
  username: string
  roles: Role[]
  verificationPending: boolean
  verificationUrl: string | null
  emailSent: boolean
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

// Exam types
export interface Exam {
  id: number
  code: string
  title: string
  description: string
  startTime: string | null
  endTime: string | null
  timezone: string
  durationMinutes: number
  isActive: boolean
  createdBy: string | null
  questionCount: number
  participantCount: number
  practice: boolean
  monitorTabSwitch: boolean
  monitorBlur: boolean
  monitorExitFullscreen: boolean
  monitorCopyPaste: boolean
  monitorIdleTime: boolean
  monitorDevtools: boolean
  monitorDuplicateIp: boolean
  monitorFastSubmit: boolean
  monitorRightClick: boolean
  monitorPrintScreen: boolean
  monitorRapidQuestionSwitch: boolean
  monitorMultiMonitor: boolean
  requireCameraMic: boolean
}

export interface ExamAttempt {
  id: number
  examId: number
  studentId: number
  startedAt: string
  submittedAt: string | null
  status: AttemptStatus
  score: number
  riskScore: number
  riskLevel: string | null
  suspicious: boolean
  clientIp: string | null
}

export type AttemptStatus =
  | 'IN_PROGRESS'
  | 'PAUSED'
  | 'SUBMITTED'
  | 'AUTO_SUBMITTED'
  | 'STOPPED'

// Question types
export interface Question {
  id: number
  examId: number
  content: string
  type: QuestionType
  scoreWeight: number
  options: QuestionOption[]
  correctAnswer: string
  difficulty?: string
}

export type QuestionType =
  | 'SINGLE_CHOICE'
  | 'MULTIPLE_CHOICE'
  | 'ESSAY'

export interface QuestionOption {
  id: string
  text: string
}

// Toast types
export type ToastType = 'success' | 'error' | 'warning' | 'info'

export interface Toast {
  id: string
  type: ToastType
  title: string
  message?: string
  duration?: number
}

// Pagination
export interface PaginationParams {
  page?: number
  pageSize?: number
  sortBy?: string
  sortDir?: 'asc' | 'desc'
}

export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  pageSize: number
}

// Router meta types
export interface RouteMeta {
  requiresAuth?: boolean
  requiredRoles?: Role[]
  title?: string
}
