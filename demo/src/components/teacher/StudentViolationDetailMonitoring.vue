<template>
  <div class="sd-root">

    <!-- Loading -->
    <div v-if="loading" class="sd-loading">
      <div class="sd-loading__spinner" />
      <span class="sd-loading__text">Đang tải dữ liệu...</span>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="sd-error">
      <div class="sd-error__card">
        <LucideIcon name="alert-circle" :size="28" class="sd-error__icon" />
        <p class="sd-error__msg">{{ error }}</p>
        <button class="sd-btn sd-btn--secondary" @click="loadData()">Thử lại</button>
      </div>
    </div>

    <template v-else>
      <!-- Top bar -->
      <div class="sd-topbar">
        <div class="sd-topbar__left">
          <button class="sd-topbar__back" @click="goBack">
            <LucideIcon name="arrow-left" :size="16" />
            <span>Quay lại</span>
          </button>
          <div class="sd-topbar__sep" />
          <nav class="sd-topbar__crumbs" aria-label="breadcrumb">
            <RouterLink to="/teacher/live-monitoring" class="sd-topbar__crumb">Giám sát thi</RouterLink>
            <LucideIcon name="chevron-right" :size="12" class="sd-topbar__crumb-sep" />
            <RouterLink :to="sessionLink" class="sd-topbar__crumb">Phiên thi</RouterLink>
            <LucideIcon name="chevron-right" :size="12" class="sd-topbar__crumb-sep" />
            <span class="sd-topbar__crumb sd-topbar__crumb--active">{{ studentName }}</span>
          </nav>
        </div>
        <div class="sd-topbar__right">
          <span class="sd-live" :class="isRealtimeConnected ? 'sd-live--on' : 'sd-live--off'">
            <span class="sd-live__dot" />
            {{ isRealtimeConnected ? 'LIVE' : 'OFFLINE' }}
          </span>
          <button
            class="sd-topbar__refresh"
            :disabled="loading"
            :title="lastUpdatedLabel"
            @click="loadData(true)"
          >
            <LucideIcon name="refresh-cw" :size="14" :class="{ 'sd-spin': isRefreshing }" />
            <span>{{ isRefreshing ? 'Đang làm mới...' : 'Làm mới' }}</span>
          </button>
          <div v-if="attemptId" class="sd-topbar__id">
            <LucideIcon name="hash" :size="12" />
            <span>{{ attemptId }}</span>
          </div>
        </div>
      </div>

      <!-- Hero -->
      <div class="sd-hero" :class="`sd-hero--${riskBand}`">
        <div class="sd-hero__left">
          <div class="sd-hero__avatar-wrap">
            <div class="sd-hero__avatar" :style="{ background: avatarBg }">
              <span class="sd-hero__initials" :style="{ color: riskColor }">{{ studentInitials }}</span>
              <div class="sd-hero__status-ring" :style="{ borderColor: attemptStatusColor }" />
            </div>
          </div>
          <div class="sd-hero__info">
            <div class="sd-hero__name-row">
              <h1 class="sd-hero__name">{{ studentName }}</h1>
              <span class="sd-hero__status-badge" :style="{ color: attemptStatusColor, background: attemptStatusBg }">
                <LucideIcon :name="attemptStatusIcon" :size="11" />
                {{ attemptStatusLabel }}
              </span>
            </div>
            <p class="sd-hero__meta">
              <span v-if="studentCode">{{ studentCode }}</span>
              <span v-if="studentCode && studentEmail" class="sd-hero__meta-sep">·</span>
              <span v-if="studentEmail">{{ studentEmail }}</span>
            </p>
            <div class="sd-hero__badges">
              <span class="sd-badge" :style="{ background: riskBg, color: riskColor }">
                <LucideIcon name="shield-alert" :size="11" />
                {{ riskLevelLabel }}
              </span>
              <span v-if="riskData.reviewRequired" class="sd-badge sd-badge--warn">
                <LucideIcon name="eye" :size="11" />
                Cần review
              </span>
              <span class="sd-badge sd-badge--neutral">
                <LucideIcon name="clock" :size="11" />
                {{ sessionTime }}
              </span>
            </div>
          </div>
        </div>

        <div class="sd-hero__right">
          <!-- Circular risk gauge -->
          <div class="sd-gauge">
            <svg class="sd-gauge__svg" viewBox="0 0 120 120">
              <circle class="sd-gauge__track" cx="60" cy="60" r="52" />
              <circle
                class="sd-gauge__fill"
                cx="60" cy="60" r="52"
                :stroke="riskColor"
                :stroke-dasharray="gaugeArc"
              />
            </svg>
            <div class="sd-gauge__center">
              <span class="sd-gauge__score" :style="{ color: riskColor }">{{ riskScore }}</span>
              <span class="sd-gauge__label">điểm rủi ro</span>
            </div>
          </div>
          <div class="sd-hero__gauge-desc">
            <p class="sd-gauge-desc__text">{{ riskDescription }}</p>
            <div class="sd-gauge-desc__track">
              <div class="sd-gauge-desc__fill" :style="{ width: riskScore + '%', background: riskColor }" />
            </div>
          </div>
        </div>
      </div>

      <!-- Stats row -->
      <div class="sd-stats">
        <div class="sd-stat">
          <div class="sd-stat__icon-wrap" style="color: #94a3b8">
            <LucideIcon name="users" :size="16" />
          </div>
          <div class="sd-stat__body">
            <span class="sd-stat__value">{{ store.cards.length }}</span>
            <span class="sd-stat__label">Tổng thí sinh</span>
          </div>
        </div>
        <div class="sd-stat">
          <div class="sd-stat__icon-wrap" style="color: #4ade80">
            <LucideIcon name="play-circle" :size="16" />
          </div>
          <div class="sd-stat__body">
            <span class="sd-stat__value" style="color: #4ade80">{{ onlineCount }}</span>
            <span class="sd-stat__label">Đang thi</span>
          </div>
        </div>
        <div class="sd-stat">
          <div class="sd-stat__icon-wrap" style="color: #fbbf24">
            <LucideIcon name="alert-triangle" :size="16" />
          </div>
          <div class="sd-stat__body">
            <span class="sd-stat__value" style="color: #fbbf24">{{ alertCount }}</span>
            <span class="sd-stat__label">Nghi ngờ</span>
          </div>
        </div>
        <div class="sd-stat">
          <div class="sd-stat__icon-wrap" style="color: #a5b4fc">
            <LucideIcon name="check-circle" :size="16" />
          </div>
          <div class="sd-stat__body">
            <span class="sd-stat__value" style="color: #a5b4fc">{{ submittedCount }}</span>
            <span class="sd-stat__label">Đã nộp</span>
          </div>
        </div>
        <div class="sd-stat">
          <div class="sd-stat__icon-wrap" :style="{ color: violationCount > 0 ? '#f87171' : '#94a3b8' }">
            <LucideIcon name="shield-alert" :size="16" />
          </div>
          <div class="sd-stat__body">
            <span class="sd-stat__value" :style="{ color: violationCount > 0 ? '#f87171' : 'white' }">{{ violationCount }}</span>
            <span class="sd-stat__label">Vi phạm</span>
          </div>
        </div>
      </div>

      <div class="sd-grid">
        <!-- Left column -->
        <div class="sd-grid__left">

          <!-- Recommendation -->
          <div v-if="riskData.reviewRequired || riskData.recommendedAction || (riskData.reasons && riskData.reasons.length)" class="sd-card">
            <div class="sd-card__header">
              <LucideIcon name="lightbulb" :size="16" />
              <span class="sd-card__title">Đề xuất xử lý</span>
            </div>
            <div class="sd-card__body">
              <div class="sd-rec__action">
                <span class="sd-rec__badge" :style="{ background: riskBg, color: riskColor }">{{ recommendedActionLabel }}</span>
              </div>
              <div v-if="riskData.reasons && riskData.reasons.length" class="sd-rec__reasons">
                <p class="sd-rec__label">Lý do chính</p>
                <div class="sd-rec__chips">
                  <span
                    v-for="reason in riskData.reasons"
                    :key="reason"
                    class="sd-chip"
                    :style="{ background: 'rgba(251,191,36,0.1)', color: '#fbbf24' }"
                  >{{ reason }}</span>
                </div>
              </div>
              <div v-if="riskData.evidenceSummary && riskData.evidenceSummary.length" class="sd-rec__evidence">
                <p class="sd-rec__label">Bằng chứng tóm tắt</p>
                <ul class="sd-rec__list">
                  <li v-for="(item, i) in riskData.evidenceSummary" :key="i" class="sd-rec__item">
                    <LucideIcon name="circle-dot" :size="10" class="sd-rec__item-dot" />
                    {{ item }}
                  </li>
                </ul>
              </div>
            </div>
          </div>

          <!-- Timeline -->
          <div class="sd-card">
            <div class="sd-card__header">
              <LucideIcon name="activity" :size="16" />
              <span class="sd-card__title">Dòng thời gian vi phạm</span>
              <span class="sd-card__badge">{{ timelineEvents.length }}</span>
            </div>
            <div class="sd-card__body sd-card__body--flush">
              <div v-if="timelineEvents.length === 0" class="sd-empty">
                <LucideIcon name="check-circle" :size="32" class="sd-empty__icon" />
                <p>Không có vi phạm nào được ghi nhận</p>
              </div>
              <div v-else class="sd-timeline">
                <div v-for="event in timelineEvents" :key="event.key" class="sd-tl-item">
                  <div class="sd-tl-item__dot" :style="{ background: getVColor(event.eventType) }">
                    <LucideIcon :name="getVIcon(event.eventType)" :size="10" style="color: white" />
                  </div>
                  <div class="sd-tl-item__content">
                    <div class="sd-tl-item__header">
                      <span class="sd-tl-item__type" :style="{ color: getVColor(event.eventType) }">{{ getVLabel(event.eventType) }}</span>
                      <span class="sd-tl-item__time">{{ formatTime(event.at) }}</span>
                    </div>
                    <p v-if="event.details" class="sd-tl-item__details">{{ event.details }}</p>
                    <span class="sd-tl-item__severity" :class="`sd-tl-item__severity--${getSeverityStatus(event.severity)}`">
                      {{ getSeverityLabel(event.severity) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

        </div>

        <!-- Right column -->
        <div class="sd-grid__right">

          <!-- Risk breakdown -->
          <div v-if="hasBreakdown" class="sd-card">
            <div class="sd-card__header">
              <LucideIcon name="bar-chart-2" :size="16" />
              <span class="sd-card__title">Phân tích điểm rủi ro</span>
            </div>
            <div class="sd-card__body">
              <div v-for="(score, key) in riskData.breakdown" :key="key" class="sd-breakdown">
                <span class="sd-breakdown__label">{{ getVLabel(key) }}</span>
                <div class="sd-breakdown__bar">
                  <div class="sd-breakdown__fill" :style="{ width: Math.min(score, 100) + '%', background: sColor(score) }" />
                </div>
                <span class="sd-breakdown__score" :style="{ color: sColor(score) }">{{ score }}</span>
              </div>
            </div>
          </div>

          <!-- Suspicious patterns -->
          <div v-if="suspiciousPatterns.length > 0" class="sd-card">
            <div class="sd-card__header">
              <LucideIcon name="brain" :size="16" />
              <span class="sd-card__title">Mẫu hành vi đáng ngờ</span>
              <span class="sd-card__badge sd-card__badge--danger">{{ suspiciousPatterns.length }}</span>
            </div>
            <div class="sd-card__body">
              <div
                v-for="p in suspiciousPatterns"
                :key="p.id"
                class="sd-pattern"
                :style="{ background: pBg(p.level), borderColor: pBorder(p.level) }"
              >
                <div class="sd-pattern__head">
                  <LucideIcon name="alert-triangle" :size="13" :style="{ color: pColor(p.level) }" />
                  <span class="sd-pattern__title">{{ p.title }}</span>
                  <span class="sd-pattern__level" :style="{ background: pColor(p.level) }">{{ p.level }}</span>
                </div>
                <p class="sd-pattern__desc">{{ p.description }}</p>
              </div>
            </div>
          </div>

          <!-- Realtime feed -->
          <div class="sd-card">
            <div class="sd-card__header">
              <LucideIcon name="bell-ring" :size="16" />
              <span class="sd-card__title">Cảnh báo &amp; sự kiện</span>
              <span v-if="realtimeFeed.length" class="sd-card__badge">{{ realtimeFeed.length }}</span>
              <span class="sd-card__live" :class="isRealtimeConnected ? 'sd-card__live--on' : 'sd-card__live--off'" :title="isRealtimeConnected ? 'Realtime đang hoạt động' : 'Đang chờ kết nối realtime'">
                <span class="sd-card__live-dot" />
              </span>
            </div>
            <div class="sd-card__body sd-card__body--flush">
              <div v-if="realtimeFeed.length === 0" class="sd-empty sd-empty--compact">
                <LucideIcon name="inbox" :size="22" class="sd-empty__icon" />
                <p>Chưa có sự kiện realtime</p>
              </div>
              <TransitionGroup v-else name="sd-feed" tag="div" class="sd-feed">
                <div v-for="evt in realtimeFeed" :key="evt.id" class="sd-feed__item" :class="`sd-feed__item--${evt.tone}`">
                  <div class="sd-feed__icon">
                    <LucideIcon :name="evt.icon" :size="13" />
                  </div>
                  <div class="sd-feed__body">
                    <p class="sd-feed__label">
                      {{ evt.label }}
                      <span v-if="evt.riskScore != null" class="sd-feed__score">{{ evt.riskScore }}đ</span>
                    </p>
                    <p v-if="evt.message" class="sd-feed__msg">{{ evt.message }}</p>
                  </div>
                  <span class="sd-feed__time">{{ formatTime(evt.timestamp) }}</span>
                </div>
              </TransitionGroup>
            </div>
          </div>

          <!-- Latest signals -->
          <div v-if="latestSignals.length > 0" class="sd-card">
            <div class="sd-card__header">
              <LucideIcon name="zap" :size="16" />
              <span class="sd-card__title">Tín hiệu gần đây</span>
            </div>
            <div class="sd-card__body">
              <div v-for="sig in latestSignals" :key="sig.signalType + sig.createdAt" class="sd-signal">
                <div class="sd-signal__icon" :style="{ background: sigBg(sig.severity) }">
                  <LucideIcon name="alert-circle" :size="12" :style="{ color: sigFg(sig.severity) }" />
                </div>
                <div class="sd-signal__body">
                  <p class="sd-signal__type">{{ getVLabel(sig.signalType) }}</p>
                  <p class="sd-signal__evidence">{{ sig.evidence || '—' }}</p>
                </div>
                <div class="sd-signal__meta">
                  <span class="sd-signal__conf">{{ Math.round((sig.confidence || 0) * 100) }}%</span>
                  <span class="sd-signal__sev" :class="`sd-signal__sev--${sig.severity?.toLowerCase()}`">{{ sig.severity }}</span>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>

      <!-- Actions -->
      <div class="sd-actions">
        <button class="sd-action sd-action--warn" :disabled="actionLoading === 'warning'" @click="issueWarning">
          <LucideIcon name="alert-triangle" :size="16" />
          <span>{{ actionLoading === 'warning' ? 'Đang gửi...' : 'Gửi cảnh báo' }}</span>
        </button>
        <button
          v-if="isStudentPaused"
          class="sd-action sd-action--success"
          :disabled="actionLoading === 'resume'"
          @click="allowResume"
        >
          <LucideIcon name="play-circle" :size="16" />
          <span>{{ actionLoading === 'resume' ? 'Đang xử lý...' : 'Cho phép tiếp tục thi' }}</span>
        </button>
        <button
          v-else
          class="sd-action sd-action--danger"
          :disabled="actionLoading === 'invalidate' || isStudentTerminal"
          @click="suspendExam"
        >
          <LucideIcon name="x-circle" :size="16" />
          <span>{{ actionLoading === 'invalidate' ? 'Đang xử lý...' : 'Tạm dừng thi' }}</span>
        </button>
        <button class="sd-action sd-action--outline" @click="viewFullReport">
          <LucideIcon name="file-text" :size="16" />
          <span>Báo cáo đầy đủ</span>
        </button>
      </div>
    </template>

    <!-- Warning dialog -->
    <Teleport to="body">
      <div v-if="showWarningDialog" class="sd-overlay" @click.self="showWarningDialog = false">
        <div class="sd-dialog">
          <div class="sd-dialog__head">
            <LucideIcon name="alert-triangle" :size="20" />
            <h3 class="sd-dialog__title">Gửi cảnh báo</h3>
          </div>
          <div class="sd-dialog__body">
            <p class="sd-dialog__desc">Gửi cảnh báo tới <strong>{{ studentName }}</strong>.</p>
            <textarea v-model="warningMessage" class="sd-dialog__textarea" rows="3" placeholder="Nhập nội dung cảnh báo (để trống = cảnh báo mặc định)" />
          </div>
          <div class="sd-dialog__foot">
            <button class="sd-btn sd-btn--secondary" @click="showWarningDialog = false">Hủy</button>
            <button class="sd-btn sd-btn--warn" @click="confirmSendWarning">Gửi cảnh báo</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Invalidate dialog -->
    <Teleport to="body">
      <div v-if="showInvalidateDialog" class="sd-overlay" @click.self="showInvalidateDialog = false">
        <div class="sd-dialog">
          <div class="sd-dialog__head sd-dialog__head--danger">
            <LucideIcon name="shield-alert" :size="20" />
            <h3 class="sd-dialog__title">Xác nhận dừng thi</h3>
          </div>
          <div class="sd-dialog__body">
            <p class="sd-dialog__desc sd-dialog__desc--danger">Hành động này sẽ <strong>dừng bài thi</strong>. Không thể hoàn tác.</p>
            <textarea v-model="invalidateReason" class="sd-dialog__textarea" rows="2" placeholder="Lý do dừng thi (để trống = lý do mặc định)" />
          </div>
          <div class="sd-dialog__foot">
            <button class="sd-btn sd-btn--secondary" @click="showInvalidateDialog = false">Hủy</button>
            <button class="sd-btn sd-btn--danger" @click="confirmInvalidate">Xác nhận dừng thi</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Resume dialog -->
    <Teleport to="body">
      <div v-if="showResumeDialog" class="sd-overlay" @click.self="showResumeDialog = false">
        <div class="sd-dialog">
          <div class="sd-dialog__head sd-dialog__head--success">
            <LucideIcon name="play-circle" :size="20" />
            <h3 class="sd-dialog__title">Cho phép tiếp tục thi</h3>
          </div>
          <div class="sd-dialog__body">
            <p class="sd-dialog__desc">Học sinh <strong>{{ studentName }}</strong> sẽ được khôi phục bài thi và tiếp tục làm bài.</p>
            <textarea v-model="resumeMessage" class="sd-dialog__textarea" rows="2" placeholder="Lời nhắn (tùy chọn)" />
          </div>
          <div class="sd-dialog__foot">
            <button class="sd-btn sd-btn--secondary" @click="showResumeDialog = false">Hủy</button>
            <button class="sd-btn sd-btn--success" @click="confirmResume">Cho phép tiếp tục</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useToast } from '../../composables/useToast'
import { useProctorDashboardStore } from '../../stores/proctorDashboardStore'
import { listExamAttempts } from '../../services/attemptService'
import {
  fetchAttemptRisk,
  listMonitoringTimeline,
  sendTeacherWarning,
  invalidateAttempt,
  resumeAttempt
} from '../../services/monitoringService'
import { isAttemptPaused, isAttemptTerminal } from '../../utils/proctorStatusMeta'
import { useRealtimeChannel } from '../../composables/useRealtimeChannel'
import LucideIcon from '../common/LucideIcon.vue'

const REFRESH_INTERVAL_MS = 15000
const RISK_BAND_THRESHOLDS = { CRITICAL: 81, HIGH_RISK: 61, SUSPICIOUS: 31 }

const ATTEMPT_STATUS_META = {
  SUBMITTED: { label: 'Đã nộp', color: '#4ade80', bg: 'rgba(74,222,128,0.12)', icon: 'check-circle' },
  STOPPED: { label: 'Đã dừng', color: '#f87171', bg: 'rgba(248,113,113,0.12)', icon: 'x-circle' },
  PAUSED: { label: 'Tạm dừng', color: '#fbbf24', bg: 'rgba(251,191,36,0.12)', icon: 'pause-circle' },
  ACTIVE: { label: 'Đang thi', color: '#a5b4fc', bg: 'rgba(165,180,252,0.12)', icon: 'play-circle' },
  UNKNOWN: { label: '—', color: '#94a3b8', bg: 'rgba(148,163,184,0.08)', icon: 'help-circle' }
}

const SEVERITY_MAP = {
  TAB_SWITCH: 'LOW', WINDOW_BLUR: 'LOW', IDLE_TIME: 'LOW', RIGHT_CLICK: 'LOW',
  EXIT_FULLSCREEN: 'MEDIUM', COPY_PASTE: 'MEDIUM', RAPID_QUESTION_SWITCH: 'MEDIUM',
  DUPLICATE_IP: 'MEDIUM', HEARTBEAT_STALE: 'MEDIUM',
  DEVTOOLS_OPEN: 'HIGH', PRINT_SCREEN: 'HIGH', MULTI_MONITOR: 'HIGH',
  DEVICE_FINGERPRINT_CHANGED: 'HIGH', SYNC_BEHAVIOR: 'HIGH',
  IP_FINGERPRINT_GRAPH: 'HIGH', ANSWER_SIMILARITY: 'HIGH',
  AI_MULTIPLE_FACES: 'HIGH', AI_PHONE_DETECTED: 'HIGH'
}

const VIOLATION_COLOR_MAP = {
  TAB_SWITCH: '#fbbf24', COPY_PASTE: '#f87171',
  DEVTOOLS_OPEN: '#f87171', EXIT_FULLSCREEN: '#fbbf24',
  MULTI_MONITOR: '#f87171', DUPLICATE_IP: '#f87171',
  PRINT_SCREEN: '#f87171', WINDOW_BLUR: '#fbbf24',
  IDLE_TIME: '#38bdf8', RIGHT_CLICK: '#38bdf8',
  HEARTBEAT_STALE: '#fbbf24', RAPID_QUESTION_SWITCH: '#fbbf24',
  DEVICE_FINGERPRINT_CHANGED: '#f87171', SYNC_BEHAVIOR: '#f87171',
  IP_FINGERPRINT_GRAPH: '#f87171', ANSWER_SIMILARITY: '#f87171',
  AI_MULTIPLE_FACES: '#f87171', AI_PHONE_DETECTED: '#f87171',
  AI_LOOKING_AWAY: '#fbbf24'
}

const VIOLATION_ICON_MAP = {
  TAB_SWITCH: 'layers', WINDOW_BLUR: 'layers', IDLE_TIME: 'clock',
  RIGHT_CLICK: 'mouse-pointer-2', EXIT_FULLSCREEN: 'minimize', COPY_PASTE: 'copy',
  DEVTOOLS_OPEN: 'code', PRINT_SCREEN: 'code', MULTI_MONITOR: 'monitor',
  DUPLICATE_IP: 'globe', RAPID_QUESTION_SWITCH: 'monitor', HEARTBEAT_STALE: 'wifi-off',
  DEVICE_FINGERPRINT_CHANGED: 'code', SYNC_BEHAVIOR: 'monitor',
  IP_FINGERPRINT_GRAPH: 'globe', ANSWER_SIMILARITY: 'copy',
  AI_MULTIPLE_FACES: 'monitor', AI_PHONE_DETECTED: 'monitor',
  AI_LOOKING_AWAY: 'wifi-off'
}

const VIOLATION_LABEL_MAP = {
  TAB_SWITCH: 'Chuyển tab', WINDOW_BLUR: 'Mất tiêu điểm cửa sổ',
  IDLE_TIME: 'Không hoạt động', RIGHT_CLICK: 'Click chuột phải',
  EXIT_FULLSCREEN: 'Thoát toàn màn hình', COPY_PASTE: 'Sao chép nội dung',
  DEVTOOLS_OPEN: 'Mở DevTools', PRINT_SCREEN: 'Chụp màn hình',
  MULTI_MONITOR: 'Nhiều màn hình', DUPLICATE_IP: 'IP trùng lặp',
  RAPID_QUESTION_SWITCH: 'Chuyển câu nhanh', HEARTBEAT_STALE: 'Mất kết nối',
  DEVICE_FINGERPRINT_CHANGED: 'Thay đổi thiết bị',
  SYNC_BEHAVIOR: 'Hành vi đồng bộ', IP_FINGERPRINT_GRAPH: 'Liên kết IP/fingerprint',
  ANSWER_SIMILARITY: 'Tương đồng đáp án', AI_MULTIPLE_FACES: 'Nhiều khuôn mặt',
  AI_PHONE_DETECTED: 'Phát hiện điện thoại', AI_LOOKING_AWAY: 'Nhìn lệch hướng'
}

const SEVERITY_LABEL_MAP = { HIGH: 'Nghiêm trọng', MEDIUM: 'Trung bình', LOW: 'Thấp', CRITICAL: 'Nghiêm trọng' }
const RISK_LEVEL_LABEL_MAP = { CRITICAL: 'Nguy cơ cao', HIGH_RISK: 'Rủi ro cao', SUSPICIOUS: 'Đáng ngờ', CLEAN: 'Bình thường' }
const RECOMMENDED_ACTION_MAP = {
  PAUSE_AND_REVIEW: 'Tạm dừng và kiểm tra ngay',
  WARN_AND_ESCALATE: 'Cảnh báo và tăng cường giám sát',
  REVIEW_ATTEMPT: 'Mở hồ sơ để review thủ công',
  CONTINUE_MONITORING: 'Tiếp tục giám sát'
}

const PATTERN_RULES = [
  { id: 'tab', key: 'TAB_SWITCH', threshold: 3, build: (n) => ({ title: 'Chuyển tab nhiều lần', description: `${n} lần chuyển tab, vượt ngưỡng bình thường (3 lần)`, level: n > 5 ? 'high' : 'medium' }) },
  { id: 'copy', key: 'COPY', threshold: 1, build: (n) => ({ title: 'Cố gắng copy nội dung', description: `${n} lần sao chép dữ liệu từ đề thi`, level: 'medium' }) },
  { id: 'fs', key: 'EXIT_FULLSCREEN', threshold: 1, build: (n) => ({ title: 'Thoát chế độ toàn màn hình', description: `${n} lần thoát toàn màn hình`, level: n > 2 ? 'high' : 'medium' }) },
  { id: 'dev', key: 'DEVTOOLS', threshold: 1, build: (n) => ({ title: 'Mở công cụ phát triển', description: `${n} lần mở DevTools`, level: 'high' }) },
  { id: 'dup', key: 'DUPLICATE_IP', threshold: 1, build: (n) => ({ title: 'Phát hiện IP trùng lặp', description: `${n} thiết bị khác cùng IP đang thi`, level: 'high' }) },
  { id: 'sync', key: 'SYNC', threshold: 1, build: (n) => ({ title: 'Hành vi đồng bộ', description: `${n} tín hiệu nhiều thí sinh thao tác đồng thời`, level: 'high' }) }
]

const mapSeverity = (type, confidence) =>
  SEVERITY_MAP[type] || ((confidence ?? 0) >= 0.8 ? 'HIGH' : (confidence ?? 0) >= 0.5 ? 'MEDIUM' : 'LOW')
const getVColor = (type) => VIOLATION_COLOR_MAP[type] || '#94a3b8'
const getVIcon = (type) => VIOLATION_ICON_MAP[type] || 'alert-circle'
const getVLabel = (type) => VIOLATION_LABEL_MAP[type] || type || '—'
const getSeverityLabel = (s) => SEVERITY_LABEL_MAP[s] || s || '—'
const getSeverityStatus = (s) =>
  s === 'HIGH' || s === 'CRITICAL' ? 'high' : s === 'MEDIUM' ? 'medium' : 'low'
const pColor = (l) => l === 'high' ? '#f87171' : l === 'medium' ? '#fbbf24' : '#38bdf8'
const pBg = (l) => l === 'high' ? 'rgba(248,113,113,0.08)' : l === 'medium' ? 'rgba(251,191,36,0.08)' : 'rgba(56,189,248,0.08)'
const pBorder = (l) => l === 'high' ? 'rgba(248,113,113,0.2)' : l === 'medium' ? 'rgba(251,191,36,0.2)' : 'rgba(56,189,248,0.2)'
const sColor = (score) => score >= 60 ? '#f87171' : score >= 30 ? '#fbbf24' : '#38bdf8'
const sigBg = (sev) => sev === 'HIGH' ? 'rgba(248,113,113,0.15)' : sev === 'MEDIUM' ? 'rgba(251,191,36,0.15)' : 'rgba(56,189,248,0.15)'
const sigFg = (sev) => sev === 'HIGH' ? '#f87171' : sev === 'MEDIUM' ? '#fbbf24' : '#38bdf8'

const formatTime = (ts) => {
  if (!ts) return '—'
  return new Date(ts).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

const router = useRouter()
const route = useRoute()
const toast = useToast()
const store = useProctorDashboardStore()
const { cards: attempts } = storeToRefs(store)
const realtime = useRealtimeChannel()
const isRealtimeConnected = realtime.isConnected

const attemptId = computed(() => route.query.attemptId || '')
const examId = computed(() => route.query.examId || '')
const sessionLink = computed(() => ({
  path: '/teacher/live-monitoring/session',
  query: examId.value ? { examId: examId.value } : {}
}))

const loading = ref(false)
const isRefreshing = ref(false)
const error = ref('')
const lastUpdatedAt = ref(null)
const actionLoading = ref('')
const showWarningDialog = ref(false)
const showInvalidateDialog = ref(false)
const showResumeDialog = ref(false)
const warningMessage = ref('')
const invalidateReason = ref('')
const resumeMessage = ref('')

const riskData = ref({})
const timeline = ref([])
const realtimeFeed = ref([])

let refreshTimer = null

// ── Student info ──────────────────────────────────────────────────────────────
const studentName = computed(() => {
  const s = riskData.value.student
  return s?.name || s?.username || '—'
})
const studentCode = computed(() => {
  const s = riskData.value.student
  if (!s) return '—'
  return s.studentCode || String(s.id || '—')
})
const studentEmail = computed(() => riskData.value.student?.email || '')
const studentInitials = computed(() => {
  const name = studentName.value
  if (!name || name === '—') return '?'
  const parts = name.trim().split(/\s+/)
  const last = parts[parts.length - 1] || ''
  const first = parts[0] || ''
  return (last.charAt(0) + (parts.length > 1 ? first.charAt(0) : '')).toUpperCase()
})

// ── Attempt status ────────────────────────────────────────────────────────────
const attemptStatusToken = computed(() => {
  const s = String(riskData.value.status || riskData.value.attempt?.status || '').toUpperCase()
  if (/SUBMITTED/.test(s)) return 'SUBMITTED'
  if (/STOPPED/.test(s)) return 'STOPPED'
  if (/PAUSED/.test(s)) return 'PAUSED'
  if (/ACTIVE|IN_PROGRESS/.test(s)) return 'ACTIVE'
  return 'UNKNOWN'
})
const attemptStatusMeta = computed(() => ATTEMPT_STATUS_META[attemptStatusToken.value])
const attemptStatusLabel = computed(() => attemptStatusMeta.value.label)
const attemptStatusColor = computed(() => attemptStatusMeta.value.color)
const attemptStatusBg = computed(() => attemptStatusMeta.value.bg)
const attemptStatusIcon = computed(() => attemptStatusMeta.value.icon)
const isStudentPaused = computed(() => isAttemptPaused(riskData.value.status || riskData.value.attempt?.status))
const isStudentTerminal = computed(() => isAttemptTerminal(riskData.value.status || riskData.value.attempt?.status))
const sessionTime = computed(() => {
  const ts = riskData.value.attempt?.startedAt
  if (!ts) return '—'
  const diff = Math.floor((Date.now() - new Date(ts).getTime()) / 60000)
  if (diff < 1) return '<1 phút'
  if (diff < 60) return `${diff} phút`
  return `${Math.floor(diff / 60)}h ${diff % 60}p`
})

// ── Stats for session (parent dashboard) ──────────────────────────────────────
const onlineCount = computed(() => attempts.value.filter(a => /^(ACTIVE|IN_PROGRESS)$/i.test(a.status || '')).length)
const alertCount = computed(() => attempts.value.filter(a => Number(a.riskScore || 0) > 30 || Boolean(a.reviewRequired)).length)
const submittedCount = computed(() => attempts.value.filter(a => /SUBMITTED/i.test(a.status || '')).length)

// ── Risk ──────────────────────────────────────────────────────────────────────
const riskScore = computed(() => Number(riskData.value.score ?? 0))
const riskBand = computed(() => {
  const s = riskScore.value
  if (s >= RISK_BAND_THRESHOLDS.HIGH_RISK) return 'danger'
  if (s >= RISK_BAND_THRESHOLDS.SUSPICIOUS) return 'warn'
  return 'clean'
})
const riskLevelLabel = computed(() =>
  RISK_LEVEL_LABEL_MAP[riskData.value.level] || riskData.value.level || '—'
)
const riskDescription = computed(() => {
  const s = riskScore.value
  if (s >= RISK_BAND_THRESHOLDS.CRITICAL) return 'Hành vi gian lận rõ ràng — cần xử lý ngay'
  if (s >= RISK_BAND_THRESHOLDS.HIGH_RISK) return 'Rủi ro cao — cần giám sát kỹ lưỡng'
  if (s >= RISK_BAND_THRESHOLDS.SUSPICIOUS) return 'Đáng ngờ — có hành vi bất thường'
  return 'Không có dấu hiệu bất thường'
})
const riskColor = computed(() => {
  const s = riskScore.value
  if (s >= RISK_BAND_THRESHOLDS.HIGH_RISK) return '#f87171'
  if (s >= RISK_BAND_THRESHOLDS.SUSPICIOUS) return '#fbbf24'
  return '#4ade80'
})
const riskBg = computed(() => {
  const s = riskScore.value
  if (s >= RISK_BAND_THRESHOLDS.HIGH_RISK) return 'rgba(248,113,113,0.15)'
  if (s >= RISK_BAND_THRESHOLDS.SUSPICIOUS) return 'rgba(251,191,36,0.15)'
  return 'rgba(74,222,128,0.15)'
})
const avatarBg = computed(() => riskBg.value)
const recommendedActionLabel = computed(() =>
  RECOMMENDED_ACTION_MAP[riskData.value.recommendedAction] || 'Tiếp tục giám sát'
)
const hasBreakdown = computed(() => {
  const b = riskData.value.breakdown
  return b && Object.keys(b).length > 0
})

// ── Gauge SVG ─────────────────────────────────────────────────────────────────
const gaugeArc = computed(() => {
  const circ = 2 * Math.PI * 52
  const ratio = Math.max(0, Math.min(100, riskScore.value)) / 100
  return `${ratio * circ} ${circ}`
})

// ── Timeline events ───────────────────────────────────────────────────────────
const timelineEvents = computed(() => {
  const evts = []
  const seen = new Set()
  for (const item of timeline.value) {
    const at = item.at || item.createdAt || item.timestamp
    const eventType = item.eventType || item.signalType || ''
    const key = `${eventType}-${at}`
    if (seen.has(key)) continue
    seen.add(key)
    evts.push({
      key,
      at,
      eventType,
      details: item.details || item.evidence || '',
      severity: item.severity || mapSeverity(eventType, item.confidence)
    })
  }
  return evts
})

// Single-pass aggregation: tránh duyệt list nhiều lần như trước
const eventStats = computed(() => {
  const counts = {
    severity: { HIGH: 0, MEDIUM: 0, LOW: 0 },
    pattern: { TAB_SWITCH: 0, COPY: 0, EXIT_FULLSCREEN: 0, DEVTOOLS: 0, DUPLICATE_IP: 0, SYNC: 0 }
  }
  for (const e of timelineEvents.value) {
    if (counts.severity[e.severity] != null) counts.severity[e.severity]++
    const t = e.eventType || ''
    if (/TAB_SWITCH/i.test(t)) counts.pattern.TAB_SWITCH++
    if (/COPY/i.test(t)) counts.pattern.COPY++
    if (/EXIT_FULLSCREEN/i.test(t)) counts.pattern.EXIT_FULLSCREEN++
    if (/DEVTOOLS/i.test(t)) counts.pattern.DEVTOOLS++
    if (/DUPLICATE_IP/i.test(t)) counts.pattern.DUPLICATE_IP++
    if (/SYNC/i.test(t)) counts.pattern.SYNC++
  }
  return counts
})

const violationCount = computed(() => timelineEvents.value.length)
const maxSeverity = computed(() => {
  const { HIGH, MEDIUM, LOW } = eventStats.value.severity
  if (HIGH > 0) return 'Nghiêm trọng'
  if (MEDIUM > 0) return 'Trung bình'
  if (LOW > 0) return 'Thấp'
  return '—'
})

// ── Latest signals ───────────────────────────────────────────────────────────
const latestSignals = computed(() => riskData.value.latestSignals || [])

// ── Suspicious patterns (single pass via eventStats) ──────────────────────────
const suspiciousPatterns = computed(() => {
  const counts = eventStats.value.pattern
  return PATTERN_RULES
    .filter(rule => counts[rule.key] >= rule.threshold)
    .map(rule => ({ id: rule.id, ...rule.build(counts[rule.key]) }))
})

const lastUpdatedLabel = computed(() => {
  if (!lastUpdatedAt.value) return 'Chưa cập nhật'
  return `Cập nhật ${new Date(lastUpdatedAt.value).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}`
})

// ── Data loading ──────────────────────────────────────────────────────────────
async function loadData(silent = false) {
  if (!attemptId.value) {
    error.value = 'Thiếu mã bài làm. Vui lòng quay lại danh sách giám sát.'
    return
  }
  if (silent) {
    isRefreshing.value = true
  } else {
    loading.value = true
    error.value = ''
  }
  try {
    // Promise.allSettled để 1 endpoint lỗi không kill toàn trang
    const [riskRes, timelineRes] = await Promise.allSettled([
      fetchAttemptRisk(attemptId.value),
      listMonitoringTimeline(attemptId.value)
    ])

    if (riskRes.status === 'fulfilled') {
      riskData.value = riskRes.value || {}
    } else if (!silent && Object.keys(riskData.value).length === 0) {
      // Chỉ báo lỗi khi mở lần đầu và không có dữ liệu nào
      throw riskRes.reason
    }

    if (timelineRes.status === 'fulfilled') {
      timeline.value = timelineRes.value || []
    }

    lastUpdatedAt.value = Date.now()
  } catch (err) {
    error.value = err?.message || 'Không thể tải dữ liệu giám sát'
    if (!silent) toast.error(error.value)
  } finally {
    loading.value = false
    isRefreshing.value = false
  }
}

// Khi mở trực tiếp bằng URL, store cards có thể rỗng → fallback fetch
async function ensureSessionContext() {
  if (!examId.value) return
  if (attempts.value.length > 0) return
  try {
    const fetched = await listExamAttempts(examId.value)
    if (Array.isArray(fetched) && fetched.length > 0) {
      store.setCards(fetched)
    }
  } catch {
    // Không cần báo lỗi: stats vẫn hiển thị 0 thay vì block trang detail
  }
}

function startAutoRefresh() {
  stopAutoRefresh()
  refreshTimer = window.setInterval(() => {
    if (document.hidden || loading.value || isRefreshing.value) return
    void loadData(true)
  }, REFRESH_INTERVAL_MS)
}
function stopAutoRefresh() {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
    refreshTimer = null
  }
}

const REALTIME_TYPE_META = {
  RISK_UPDATED: { icon: 'trending-up', tone: 'warn', label: 'Cập nhật rủi ro' },
  SUSPICIOUS: { icon: 'alert-triangle', tone: 'warn', label: 'Hành vi đáng ngờ' },
  WARNING_SENT: { icon: 'alert-circle', tone: 'warn', label: 'Đã gửi cảnh báo' },
  ATTEMPT_PAUSED: { icon: 'pause-circle', tone: 'warn', label: 'Tạm dừng' },
  ATTEMPT_RESUMED: { icon: 'play-circle', tone: 'success', label: 'Tiếp tục' },
  ATTEMPT_STOPPED: { icon: 'x-circle', tone: 'danger', label: 'Đình chỉ' }
}

function pushRealtimeEvent(payload = {}) {
  const type = String(payload.type || '').toUpperCase()
  const meta = REALTIME_TYPE_META[type] || { icon: 'activity', tone: 'neutral', label: type || 'Sự kiện' }
  const entry = {
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    type,
    icon: meta.icon,
    tone: meta.tone,
    label: meta.label,
    message: payload.message || '',
    riskScore: payload.riskScore ?? null,
    timestamp: payload.timestamp || new Date().toISOString()
  }
  realtimeFeed.value = [entry, ...realtimeFeed.value].slice(0, 30)
}

function handleRealtimePayload(payload) {
  if (!payload || typeof payload !== 'object') return
  const type = String(payload.type || '').toUpperCase()

  if (type === 'RISK_UPDATED' || type === 'SUSPICIOUS') {
    riskData.value = {
      ...riskData.value,
      score: payload.riskScore ?? riskData.value.score,
      level: payload.riskLevel || riskData.value.level,
      reasons: payload.reasons || riskData.value.reasons,
      recommendedAction: payload.recommendedAction || riskData.value.recommendedAction,
      reviewRequired: payload.reviewRequired ?? riskData.value.reviewRequired
    }
    lastUpdatedAt.value = Date.now()
  }

  if (type === 'ATTEMPT_PAUSED' || type === 'ATTEMPT_RESUMED' || type === 'ATTEMPT_STOPPED') {
    riskData.value = {
      ...riskData.value,
      status: payload.status || (type === 'ATTEMPT_PAUSED' ? 'PAUSED' : type === 'ATTEMPT_STOPPED' ? 'STOPPED' : 'IN_PROGRESS')
    }
    void loadData(true)
  }

  pushRealtimeEvent(payload)
}

async function connectRealtime() {
  if (!attemptId.value) return
  await realtime.connect({
    reconnectDelay: 5000,
    topics: [
      { destination: `/topic/attempts/${attemptId.value}/proctor-actions`, handler: handleRealtimePayload }
    ]
  })
}

// ── Actions ───────────────────────────────────────────────────────────────────
function goBack() {
  router.back()
}
function issueWarning() {
  warningMessage.value = ''
  showWarningDialog.value = true
}
function suspendExam() {
  invalidateReason.value = ''
  showInvalidateDialog.value = true
}
function allowResume() {
  resumeMessage.value = ''
  showResumeDialog.value = true
}

async function runAttemptAction({ key, call, payload, successMsg, errorMsg, closeDialog }) {
  if (!attemptId.value) return
  actionLoading.value = key
  closeDialog()
  try {
    await call(attemptId.value, payload)
    toast.success(successMsg)
    await loadData(true)
  } catch (err) {
    toast.error(err?.message || errorMsg)
  } finally {
    actionLoading.value = ''
  }
}

async function confirmSendWarning() {
  await runAttemptAction({
    key: 'warning',
    call: sendTeacherWarning,
    payload: warningMessage.value,
    successMsg: 'Đã gửi cảnh báo tới học sinh.',
    errorMsg: 'Gửi cảnh báo thất bại.',
    closeDialog: () => { showWarningDialog.value = false; warningMessage.value = '' }
  })
}

async function confirmInvalidate() {
  await runAttemptAction({
    key: 'invalidate',
    call: invalidateAttempt,
    payload: invalidateReason.value,
    successMsg: 'Đã dừng bài thi.',
    errorMsg: 'Dừng thi thất bại.',
    closeDialog: () => { showInvalidateDialog.value = false; invalidateReason.value = '' }
  })
}

async function confirmResume() {
  await runAttemptAction({
    key: 'resume',
    call: resumeAttempt,
    payload: resumeMessage.value,
    successMsg: 'Đã cho phép học sinh tiếp tục bài thi.',
    errorMsg: 'Khôi phục bài thi thất bại.',
    closeDialog: () => { showResumeDialog.value = false; resumeMessage.value = '' }
  })
}

function viewFullReport() {
  router.push({
    path: '/teacher/exams/review/incidents',
    query: examId.value ? { examId: examId.value } : {}
  })
}

// ── Keyboard handling ─────────────────────────────────────────────────────────
function handleEscape(e) {
  if (e.key !== 'Escape') return
  if (showWarningDialog.value) showWarningDialog.value = false
  else if (showInvalidateDialog.value) showInvalidateDialog.value = false
  else if (showResumeDialog.value) showResumeDialog.value = false
}

// ── Lifecycle ─────────────────────────────────────────────────────────────────
watch(attemptId, async (next, prev) => {
  if (!next || next === prev) return
  realtime.disconnect()
  realtimeFeed.value = []
  await loadData()
  await connectRealtime()
})

onMounted(async () => {
  window.addEventListener('keydown', handleEscape)
  await Promise.allSettled([loadData(), ensureSessionContext()])
  await connectRealtime()
  startAutoRefresh()
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleEscape)
  stopAutoRefresh()
  realtime.disconnect()
})
</script>

<style scoped>
/* ── Tone tokens (semantic colors used by inline styles + classes) ─── */
.sd-root {
  --sd-tone-danger: var(--ds-danger);
  --sd-tone-danger-bg: var(--ds-danger-bg);
  --sd-tone-warn: var(--ds-warning);
  --sd-tone-warn-bg: var(--ds-warning-bg);
  --sd-tone-success: var(--ds-success);
  --sd-tone-success-bg: var(--ds-success-bg);
  --sd-tone-info: var(--ds-info);
  --sd-tone-info-bg: var(--ds-info-bg);
  --sd-tone-neutral: var(--ds-text-muted);
  --sd-tone-neutral-bg: var(--ds-surface-muted);
  min-height: 100vh;
  background: var(--ds-bg);
  color: var(--ds-text);
  scroll-behavior: smooth;
}

/* ── Loading ──────────────────────────────────────────────────────── */
.sd-loading { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 0.875rem; height: 60vh; }
.sd-loading__spinner { width: 32px; height: 32px; border: 3px solid var(--ds-border); border-top-color: var(--ds-primary); border-radius: 50%; animation: sd-spin 0.7s linear infinite; }
.sd-loading__text { color: var(--ds-text-secondary); font-size: 0.875rem; }
@keyframes sd-spin { to { transform: rotate(360deg); } }

/* ── Error ────────────────────────────────────────────────────────── */
.sd-error { display: flex; align-items: center; justify-content: center; height: 60vh; }
.sd-error__card { display: flex; flex-direction: column; align-items: center; gap: 0.75rem; padding: 2.5rem; border: 1px solid var(--ds-danger-soft); border-radius: var(--ds-radius-xl); background: var(--ds-danger-bg); max-width: 360px; text-align: center; box-shadow: var(--ds-shadow-sm); }
.sd-error__icon { color: var(--ds-danger); }
.sd-error__msg { color: var(--ds-danger); font-size: 0.875rem; font-weight: 600; }

/* ── Top bar ──────────────────────────────────────────────────────── */
.sd-topbar { display: flex; align-items: center; justify-content: space-between; padding: 0.625rem 1.5rem; border-bottom: 1px solid var(--ds-border); background: var(--ds-surface); position: sticky; top: 0; z-index: 50; gap: 1rem; box-shadow: var(--ds-shadow-xs); }
.sd-topbar__left { display: flex; align-items: center; gap: 0.75rem; min-width: 0; }
.sd-topbar__back { display: inline-flex; align-items: center; gap: 0.375rem; padding: 0.35rem 0.75rem; border-radius: var(--ds-radius-md); border: 1px solid var(--ds-border); background: var(--ds-surface); color: var(--ds-text-secondary); font-size: 0.775rem; font-weight: 600; cursor: pointer; transition: all 0.15s; white-space: nowrap; flex-shrink: 0; }
.sd-topbar__back:hover { background: var(--ds-primary-soft); border-color: var(--ds-primary-border); color: var(--ds-primary); }
.sd-topbar__sep { width: 1px; height: 20px; background: var(--ds-border); flex-shrink: 0; }
.sd-topbar__crumbs { display: flex; align-items: center; gap: 0.375rem; font-size: 0.775rem; min-width: 0; overflow: hidden; }
.sd-topbar__crumb { color: var(--ds-text-secondary); text-decoration: none; white-space: nowrap; transition: color 0.15s; }
.sd-topbar__crumb:hover { color: var(--ds-primary); }
.sd-topbar__crumb-sep { color: var(--ds-text-muted); flex-shrink: 0; }
.sd-topbar__crumb--active { color: var(--ds-text); font-weight: 700; overflow: hidden; text-overflow: ellipsis; }
.sd-topbar__right { display: flex; align-items: center; gap: 0.5rem; flex-shrink: 0; }
.sd-topbar__refresh { display: inline-flex; align-items: center; gap: 0.375rem; padding: 0.35rem 0.75rem; border-radius: var(--ds-radius-md); border: 1px solid var(--ds-primary-border); background: var(--ds-primary-soft); color: var(--ds-primary); font-size: 0.745rem; font-weight: 700; cursor: pointer; transition: all 0.15s; white-space: nowrap; }
.sd-topbar__refresh:hover:not(:disabled) { background: var(--ds-primary); color: #fff; }
.sd-topbar__refresh:disabled { opacity: 0.5; cursor: not-allowed; }
.sd-topbar__id { display: inline-flex; align-items: center; gap: 0.3rem; font-size: 0.7rem; font-weight: 700; padding: 0.2rem 0.625rem; border-radius: 9999px; background: var(--ds-primary-soft); color: var(--ds-primary); white-space: nowrap; flex-shrink: 0; }
.sd-spin { animation: sd-spin 0.8s linear infinite; }

/* Live badge */
.sd-live { display: inline-flex; align-items: center; gap: 0.35rem; padding: 0.2rem 0.6rem; border-radius: 9999px; font-size: 0.62rem; font-weight: 800; letter-spacing: 0.08em; border: 1px solid transparent; }
.sd-live__dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
.sd-live--on { background: var(--ds-success-bg); color: var(--ds-success); border-color: var(--ds-success-soft); }
.sd-live--on .sd-live__dot { animation: sd-pulse 1.4s ease-in-out infinite; }
.sd-live--off { background: var(--ds-surface-muted); color: var(--ds-text-muted); border-color: var(--ds-border); }
@keyframes sd-pulse { 0%,100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.4; transform: scale(0.7); } }

/* ── Hero ─────────────────────────────────────────────────────────── */
.sd-hero { display: flex; align-items: center; justify-content: space-between; gap: 1.5rem; padding: 1.25rem 1.5rem; background: var(--ds-surface); border-radius: var(--ds-radius-xl); flex-wrap: wrap; margin: 1.25rem 1.5rem 0; border: 1px solid var(--ds-border); box-shadow: var(--ds-shadow-sm); }
.sd-hero--danger { border-top: 3px solid var(--ds-danger); }
.sd-hero--warn { border-top: 3px solid var(--ds-warning); }
.sd-hero--clean { border-top: 3px solid var(--ds-success); }

.sd-hero__left { display: flex; align-items: center; gap: 1rem; min-width: 0; flex: 1; }
.sd-hero__avatar-wrap { flex-shrink: 0; }
.sd-hero__avatar { position: relative; width: 52px; height: 52px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.sd-hero__initials { font-size: 1.125rem; font-weight: 900; }
.sd-hero__status-ring { position: absolute; inset: -2px; border-radius: 50%; border: 2px solid; }
.sd-hero__info { min-width: 0; }
.sd-hero__name-row { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.25rem; }
.sd-hero__name { font-size: 1.05rem; font-weight: 800; color: var(--ds-text); margin: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sd-hero__status-badge { display: inline-flex; align-items: center; gap: 0.25rem; padding: 0.2rem 0.55rem; border-radius: 9999px; font-size: 0.65rem; font-weight: 700; white-space: nowrap; }
.sd-hero__meta { font-size: 0.75rem; color: var(--ds-text-secondary); margin: 0 0 0.5rem; display: flex; align-items: center; gap: 0.375rem; flex-wrap: wrap; }
.sd-hero__meta-sep { opacity: 0.5; }
.sd-hero__badges { display: flex; gap: 0.375rem; flex-wrap: wrap; }
.sd-hero__right { display: flex; align-items: center; gap: 1rem; flex-shrink: 0; }

/* Gauge */
.sd-gauge { position: relative; width: 96px; height: 96px; flex-shrink: 0; }
.sd-gauge__svg { transform: rotate(-90deg); }
.sd-gauge__track { fill: none; stroke: var(--ds-gray-100); stroke-width: 8; }
.sd-gauge__fill { fill: none; stroke-width: 8; stroke-linecap: round; transition: stroke-dasharray 0.6s ease; will-change: stroke-dasharray; }
.sd-gauge__center { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; }
.sd-gauge__score { font-size: 1.5rem; font-weight: 900; line-height: 1; }
.sd-gauge__label { font-size: 0.55rem; color: var(--ds-text-muted); font-weight: 600; margin-top: 1px; }
.sd-hero__gauge-desc { min-width: 180px; }
.sd-gauge-desc__text { font-size: 0.8rem; color: var(--ds-text-secondary); margin: 0 0 0.5rem; }
.sd-gauge-desc__track { height: 5px; border-radius: 9999px; background: var(--ds-gray-100); overflow: hidden; }
.sd-gauge-desc__fill { height: 100%; border-radius: 9999px; transition: width 0.6s ease; will-change: width; }

/* ── Stats ──────────────────────────────────────────────────────── */
.sd-stats { display: grid; grid-template-columns: repeat(5, 1fr); gap: 0.75rem; margin: 1.25rem 1.5rem 0; }
.sd-stat { display: flex; align-items: center; gap: 0.75rem; padding: 0.875rem 1rem; background: var(--ds-surface); border: 1px solid var(--ds-border); border-radius: var(--ds-radius-lg); transition: all 0.15s; box-shadow: var(--ds-shadow-xs); }
.sd-stat:hover { border-color: var(--ds-primary-border); box-shadow: var(--ds-shadow-sm); transform: translateY(-1px); }
.sd-stat__icon-wrap { width: 34px; height: 34px; border-radius: var(--ds-radius-md); display: flex; align-items: center; justify-content: center; background: var(--ds-surface-muted); flex-shrink: 0; }
.sd-stat__body { display: flex; flex-direction: column; min-width: 0; }
.sd-stat__value { font-size: 1.25rem; font-weight: 800; color: var(--ds-text); font-variant-numeric: tabular-nums; line-height: 1.2; }
.sd-stat__label { font-size: 0.65rem; color: var(--ds-text-muted); margin-top: 2px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.04em; }

/* ── Grid ──────────────────────────────────────────────────────── */
.sd-grid { display: grid; grid-template-columns: 1fr 340px; gap: 1.25rem; margin: 1.25rem 1.5rem 0; align-items: start; }
.sd-grid__right { display: flex; flex-direction: column; gap: 1.25rem; }
@media (max-width: 1024px) {
  .sd-grid { grid-template-columns: 1fr; }
  .sd-stats { grid-template-columns: repeat(3, 1fr); }
  .sd-hero { flex-direction: column; align-items: flex-start; }
  .sd-hero__right { width: 100%; }
}
@media (max-width: 640px) {
  .sd-stats { grid-template-columns: repeat(2, 1fr); margin: 1rem 1rem 0; }
  .sd-hero { margin: 1rem 1rem 0; padding: 1rem; }
  .sd-grid { margin: 1rem 1rem 0; }
  .sd-actions { margin: 1rem 1rem 1.5rem; }
  .sd-topbar { padding: 0.5rem 1rem; flex-wrap: wrap; }
  .sd-topbar__refresh span { display: none; }
}

/* ── Cards ─────────────────────────────────────────────────────── */
.sd-card { background: var(--ds-surface); border: 1px solid var(--ds-border); border-radius: var(--ds-radius-xl); overflow: hidden; box-shadow: var(--ds-shadow-xs); }
.sd-card__header { display: flex; align-items: center; gap: 0.5rem; padding: 0.875rem 1.25rem; border-bottom: 1px solid var(--ds-border); color: var(--ds-text-secondary); background: var(--ds-surface-muted); }
.sd-card__title { font-size: 0.825rem; font-weight: 800; color: var(--ds-text); flex: 1; }
.sd-card__badge { font-size: 0.68rem; font-weight: 800; padding: 0.15rem 0.5rem; border-radius: 9999px; background: var(--ds-primary-soft); color: var(--ds-primary); }
.sd-card__badge--danger { background: var(--ds-danger-bg); color: var(--ds-danger); }
.sd-card__body { padding: 1.25rem; }
.sd-card__body--flush { padding: 0; }
.sd-card__live { display: inline-flex; align-items: center; justify-content: center; width: 14px; height: 14px; border-radius: 50%; }
.sd-card__live-dot { width: 8px; height: 8px; border-radius: 50%; background: currentColor; }
.sd-card__live--on { color: var(--ds-success); }
.sd-card__live--on .sd-card__live-dot { animation: sd-pulse 1.4s ease-in-out infinite; }
.sd-card__live--off { color: var(--ds-text-muted); }

/* ── Recommendation ──────────────────────────────────────────────── */
.sd-rec__action { margin-bottom: 0.875rem; }
.sd-rec__badge { display: inline-flex; padding: 0.35rem 0.75rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 700; }
.sd-rec__label { font-size: 0.68rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; color: var(--ds-text-muted); margin: 0 0 0.5rem; }
.sd-rec__chips { display: flex; gap: 0.375rem; flex-wrap: wrap; margin-bottom: 0.875rem; }
.sd-chip { display: inline-flex; padding: 0.25rem 0.625rem; border-radius: 9999px; font-size: 0.72rem; font-weight: 600; background: var(--ds-warning-bg); color: var(--ds-warning); }
.sd-rec__list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.375rem; }
.sd-rec__item { display: flex; align-items: flex-start; gap: 0.5rem; font-size: 0.8rem; color: var(--ds-text-secondary); }
.sd-rec__item-dot { color: var(--ds-text-muted); margin-top: 4px; flex-shrink: 0; }

/* ── Timeline ──────────────────────────────────────────────────────── */
.sd-timeline { display: flex; flex-direction: column; }
.sd-tl-item { display: flex; align-items: flex-start; gap: 0.75rem; padding: 0.875rem 1.25rem; border-bottom: 1px solid var(--ds-border); transition: background 0.15s; }
.sd-tl-item:last-child { border-bottom: none; }
.sd-tl-item:hover { background: var(--ds-surface-muted); }
.sd-tl-item__dot { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.sd-tl-item__content { flex: 1; min-width: 0; }
.sd-tl-item__header { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; margin-bottom: 0.25rem; }
.sd-tl-item__type { font-size: 0.8rem; font-weight: 700; }
.sd-tl-item__time { font-size: 0.72rem; color: var(--ds-text-muted); font-variant-numeric: tabular-nums; white-space: nowrap; flex-shrink: 0; }
.sd-tl-item__details { font-size: 0.78rem; color: var(--ds-text-secondary); margin: 0 0 0.3rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sd-tl-item__severity { display: inline-flex; padding: 0.15rem 0.5rem; border-radius: 9999px; font-size: 0.65rem; font-weight: 700; }
.sd-tl-item__severity--high { background: var(--ds-danger-bg); color: var(--ds-danger); }
.sd-tl-item__severity--medium { background: var(--ds-warning-bg); color: var(--ds-warning); }
.sd-tl-item__severity--low { background: var(--ds-info-bg); color: var(--ds-info); }

/* ── Breakdown ──────────────────────────────────────────────────── */
.sd-breakdown { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.75rem; }
.sd-breakdown:last-child { margin-bottom: 0; }
.sd-breakdown__label { font-size: 0.78rem; color: var(--ds-text-secondary); min-width: 130px; max-width: 130px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-weight: 600; }
.sd-breakdown__bar { flex: 1; height: 6px; border-radius: 9999px; background: var(--ds-gray-100); overflow: hidden; }
.sd-breakdown__fill { height: 100%; border-radius: 9999px; transition: width 0.5s ease; will-change: width; }
.sd-breakdown__score { font-size: 0.78rem; font-weight: 800; min-width: 24px; text-align: right; font-variant-numeric: tabular-nums; }

/* ── Pattern ────────────────────────────────────────────────────── */
.sd-pattern { padding: 0.75rem 1rem; border: 1px solid; border-radius: var(--ds-radius-lg); margin-bottom: 0.625rem; }
.sd-pattern:last-child { margin-bottom: 0; }
.sd-pattern__head { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.25rem; }
.sd-pattern__title { font-size: 0.825rem; font-weight: 700; color: var(--ds-text); flex: 1; }
.sd-pattern__level { font-size: 0.6rem; font-weight: 800; text-transform: uppercase; letter-spacing: 0.06em; padding: 0.15rem 0.4rem; border-radius: 9999px; color: white; }
.sd-pattern__desc { font-size: 0.75rem; color: var(--ds-text-secondary); padding-left: 1.5rem; margin: 0; }

/* ── Signal ─────────────────────────────────────────────────────── */
.sd-signal { display: flex; align-items: flex-start; gap: 0.625rem; margin-bottom: 0.75rem; padding-bottom: 0.75rem; border-bottom: 1px solid var(--ds-border); }
.sd-signal:last-child { margin-bottom: 0; padding-bottom: 0; border-bottom: none; }
.sd-signal__icon { width: 28px; height: 28px; border-radius: var(--ds-radius-md); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.sd-signal__body { flex: 1; min-width: 0; }
.sd-signal__type { font-size: 0.8rem; font-weight: 700; color: var(--ds-text); margin: 0 0 0.15rem; }
.sd-signal__evidence { font-size: 0.72rem; color: var(--ds-text-secondary); margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sd-signal__meta { display: flex; flex-direction: column; align-items: flex-end; gap: 0.25rem; flex-shrink: 0; }
.sd-signal__conf { font-size: 0.78rem; font-weight: 800; color: var(--ds-text); font-variant-numeric: tabular-nums; }
.sd-signal__sev { font-size: 0.6rem; font-weight: 800; padding: 0.15rem 0.4rem; border-radius: 9999px; }
.sd-signal__sev--high { background: var(--ds-danger-bg); color: var(--ds-danger); }
.sd-signal__sev--medium { background: var(--ds-warning-bg); color: var(--ds-warning); }
.sd-signal__sev--low { background: var(--ds-info-bg); color: var(--ds-info); }

/* ── Realtime feed ────────────────────────────────────────────────── */
.sd-feed { display: flex; flex-direction: column; }
.sd-feed__item { display: flex; align-items: flex-start; gap: 0.625rem; padding: 0.7rem 1.1rem; border-bottom: 1px solid var(--ds-border); transition: background 0.15s; border-left: 3px solid transparent; }
.sd-feed__item:last-child { border-bottom: none; }
.sd-feed__item:hover { background: var(--ds-surface-muted); }
.sd-feed__item--danger { border-left-color: var(--ds-danger); }
.sd-feed__item--warn { border-left-color: var(--ds-warning); }
.sd-feed__item--success { border-left-color: var(--ds-success); }
.sd-feed__item--neutral { border-left-color: var(--ds-text-muted); }
.sd-feed__icon { width: 26px; height: 26px; border-radius: var(--ds-radius-md); display: flex; align-items: center; justify-content: center; flex-shrink: 0; background: var(--ds-surface-muted); color: var(--ds-text-muted); }
.sd-feed__item--danger .sd-feed__icon { background: var(--ds-danger-bg); color: var(--ds-danger); }
.sd-feed__item--warn .sd-feed__icon { background: var(--ds-warning-bg); color: var(--ds-warning); }
.sd-feed__item--success .sd-feed__icon { background: var(--ds-success-bg); color: var(--ds-success); }
.sd-feed__body { flex: 1; min-width: 0; }
.sd-feed__label { font-size: 0.78rem; font-weight: 700; color: var(--ds-text); margin: 0 0 0.15rem; display: flex; align-items: center; gap: 0.5rem; }
.sd-feed__score { font-size: 0.65rem; font-weight: 800; padding: 0.1rem 0.4rem; border-radius: 9999px; background: var(--ds-warning-bg); color: var(--ds-warning); }
.sd-feed__msg { font-size: 0.72rem; color: var(--ds-text-secondary); margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sd-feed__time { font-size: 0.65rem; color: var(--ds-text-muted); white-space: nowrap; flex-shrink: 0; font-variant-numeric: tabular-nums; }
.sd-feed-enter-active, .sd-feed-leave-active { transition: all 0.25s ease; }
.sd-feed-enter-from { opacity: 0; transform: translateX(-8px); }
.sd-feed-leave-to { opacity: 0; }

/* ── Empty ──────────────────────────────────────────────────────── */
.sd-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 0.5rem; padding: 3rem; color: var(--ds-text-muted); font-size: 0.85rem; }
.sd-empty--compact { padding: 1.5rem 1rem; }
.sd-empty__icon { color: var(--ds-success); }

/* ── Actions ────────────────────────────────────────────────────── */
.sd-actions { display: flex; align-items: center; justify-content: flex-end; gap: 0.75rem; flex-wrap: wrap; margin: 1.5rem 1.5rem 2rem; }
.sd-action { display: inline-flex; align-items: center; gap: 0.5rem; padding: 0.625rem 1.25rem; border-radius: var(--ds-radius-md); font-size: 0.825rem; font-weight: 700; cursor: pointer; border: 1px solid transparent; transition: all 0.15s; box-shadow: var(--ds-shadow-xs); }
.sd-action:disabled { opacity: 0.5; cursor: not-allowed; box-shadow: none; }
.sd-action--warn { background: var(--ds-warning-bg); color: var(--ds-warning); border-color: var(--ds-warning-soft); }
.sd-action--warn:hover:not(:disabled) { background: var(--ds-warning); color: #fff; border-color: var(--ds-warning); box-shadow: var(--ds-shadow-sm); }
.sd-action--danger { background: var(--ds-danger-bg); color: var(--ds-danger); border-color: var(--ds-danger-soft); }
.sd-action--danger:hover:not(:disabled) { background: var(--ds-danger); color: #fff; border-color: var(--ds-danger); box-shadow: var(--ds-shadow-sm); }
.sd-action--success { background: var(--ds-success-bg); color: var(--ds-success); border-color: var(--ds-success-soft); }
.sd-action--success:hover:not(:disabled) { background: var(--ds-success); color: #fff; border-color: var(--ds-success); box-shadow: var(--ds-shadow-sm); }
.sd-action--outline { background: var(--ds-surface); color: var(--ds-text-secondary); border-color: var(--ds-border); }
.sd-action--outline:hover:not(:disabled) { background: var(--ds-surface-muted); color: var(--ds-text); border-color: var(--ds-primary-border); }

/* ── Dialog ─────────────────────────────────────────────────────── */
.sd-overlay { position: fixed; inset: 0; z-index: 200; background: rgba(15, 23, 42, 0.45); display: flex; align-items: center; justify-content: center; backdrop-filter: blur(4px); }
.sd-dialog { width: 100%; max-width: 460px; margin: 1rem; background: var(--ds-surface); border: 1px solid var(--ds-border); border-radius: var(--ds-radius-xl); box-shadow: var(--ds-shadow-xl); overflow: hidden; }
.sd-dialog__head { display: flex; align-items: center; gap: 0.625rem; padding: 1.25rem 1.5rem 1rem; border-bottom: 1px solid var(--ds-border); color: var(--ds-warning); }
.sd-dialog__head--danger { color: var(--ds-danger); }
.sd-dialog__head--success { color: var(--ds-success); }
.sd-dialog__title { font-size: 1rem; font-weight: 800; color: var(--ds-text); margin: 0; }
.sd-dialog__body { padding: 1.25rem 1.5rem; }
.sd-dialog__desc { font-size: 0.875rem; color: var(--ds-text-secondary); margin: 0 0 0.875rem; }
.sd-dialog__desc--danger { color: var(--ds-danger); }
.sd-dialog__textarea { width: 100%; padding: 0.625rem 0.875rem; border: 1px solid var(--ds-border); border-radius: var(--ds-radius-md); background: var(--ds-surface); color: var(--ds-text); font-size: 0.85rem; resize: vertical; outline: none; transition: all 0.15s; font-family: inherit; }
.sd-dialog__textarea:focus { border-color: var(--ds-primary); box-shadow: var(--ds-shadow-focus); }
.sd-dialog__textarea::placeholder { color: var(--ds-text-muted); }
.sd-dialog__foot { display: flex; align-items: center; justify-content: flex-end; gap: 0.625rem; padding: 1rem 1.5rem; border-top: 1px solid var(--ds-border); background: var(--ds-surface-muted); }

/* ── Buttons ────────────────────────────────────────────────────── */
.sd-btn { display: inline-flex; align-items: center; justify-content: center; gap: 0.375rem; padding: 0.55rem 1.05rem; border-radius: var(--ds-radius-md); font-size: 0.825rem; font-weight: 700; cursor: pointer; border: 1px solid transparent; transition: all 0.15s; }
.sd-btn--secondary { background: var(--ds-surface); color: var(--ds-text-secondary); border-color: var(--ds-border); }
.sd-btn--secondary:hover { background: var(--ds-surface-muted); color: var(--ds-text); border-color: var(--ds-primary-border); }
.sd-btn--warn { background: var(--ds-warning); color: #fff; }
.sd-btn--warn:hover { filter: brightness(1.05); box-shadow: var(--ds-shadow-sm); }
.sd-btn--danger { background: var(--ds-danger); color: #fff; }
.sd-btn--danger:hover { filter: brightness(1.05); box-shadow: var(--ds-shadow-sm); }
.sd-btn--success { background: var(--ds-success); color: #fff; }
.sd-btn--success:hover { filter: brightness(1.05); box-shadow: var(--ds-shadow-sm); }

/* ── Badges ─────────────────────────────────────────────────────── */
.sd-badge { display: inline-flex; align-items: center; gap: 0.3rem; padding: 0.2rem 0.6rem; border-radius: 9999px; border: 1px solid transparent; font-size: 0.7rem; font-weight: 700; }
.sd-badge--warn { background: var(--ds-warning-bg); color: var(--ds-warning); border-color: var(--ds-warning-soft); }
.sd-badge--neutral { background: var(--ds-surface-muted); color: var(--ds-text-secondary); border-color: var(--ds-border); }
</style>
