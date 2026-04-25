<template>
  <div class="sd-root">

    <!-- ── Loading State ───────────────────────────────────────── -->
    <div v-if="loading" class="sd-loading">
      <div class="sd-loading__spinner" />
      <span class="sd-loading__text">Đang tải dữ liệu giám sát...</span>
    </div>

    <!-- ── Error State ────────────────────────────────────────── -->
    <div v-else-if="error" class="sd-error">
      <div class="sd-error__card">
        <LucideIcon name="alert-circle" :size="32" class="sd-error__icon" />
        <h3 class="sd-error__title">Không thể tải dữ liệu</h3>
        <p class="sd-error__msg">{{ error }}</p>
        <button class="sd-btn sd-btn--ghost" @click="loadData()">
          <LucideIcon name="refresh-cw" :size="14" />
          Thử lại
        </button>
      </div>
    </div>

    <template v-else>

      <!-- ── Top Command Bar ──────────────────────────────────── -->
      <header class="sd-topbar">
        <div class="sd-topbar__left">
          <button class="sd-back-btn" @click="goBack">
            <LucideIcon name="arrow-left" :size="16" />
            <span>Quay lại</span>
          </button>
          <div class="sd-topbar__divider" />
          <nav class="sd-breadcrumb" aria-label="breadcrumb">
            <RouterLink to="/teacher/live-monitoring" class="sd-breadcrumb__link">Giám sát</RouterLink>
            <LucideIcon name="chevron-right" :size="12" class="sd-breadcrumb__sep" />
            <RouterLink :to="sessionLink" class="sd-breadcrumb__link">Phiên thi</RouterLink>
            <LucideIcon name="chevron-right" :size="12" class="sd-breadcrumb__sep" />
            <span class="sd-breadcrumb__current">{{ studentName }}</span>
          </nav>
        </div>
        <div class="sd-topbar__right">
          <!-- Connection -->
          <div class="sd-conn-badge" :class="isRealtimeConnected ? 'sd-conn-badge--live' : 'sd-conn-badge--off'">
            <span class="sd-conn-badge__dot" />
            {{ isRealtimeConnected ? 'LIVE' : 'OFFLINE' }}
          </div>
          <!-- Last updated -->
          <span class="sd-topbar__updated">{{ lastUpdatedLabel }}</span>
          <!-- Refresh -->
          <button class="sd-topbar__refresh" @click="loadData(true)" :disabled="isRefreshing">
            <LucideIcon name="refresh-cw" :size="14" :class="{ 'sd-spin': isRefreshing }" />
            <span>Làm mới</span>
          </button>
          <!-- Attempt ID -->
          <div v-if="attemptId" class="sd-attempt-id">
            <LucideIcon name="hash" :size="12" />
            <span>{{ attemptId }}</span>
            <button class="sd-copy-btn" @click="copyAttemptId" title="Sao chép mã bài làm">
              <LucideIcon name="copy" :size="12" />
            </button>
          </div>
        </div>
      </header>

      <!-- ── Hero: Student Profile + Risk ──────────────────────── -->
      <div class="sd-hero" :class="`sd-hero--${riskBand}`">
        <div class="sd-hero__left">
          <!-- Avatar -->
          <div class="sd-avatar-wrap">
            <div class="sd-avatar" :style="{ background: avatarBg }">
              <span class="sd-avatar__initials" :style="{ color: riskColor }">{{ studentInitials }}</span>
            </div>
            <div class="sd-avatar__ring" :style="{ borderColor: attemptStatusColor }" />
            <div class="sd-avatar__risk-dot" :style="{ background: riskColor }" />
          </div>

          <!-- Info -->
          <div class="sd-hero__info">
            <div class="sd-hero__name-row">
              <h1 class="sd-hero__name">{{ studentName }}</h1>
              <span class="sd-status-badge" :style="{ color: attemptStatusColor, background: attemptStatusBg }">
                <LucideIcon :name="attemptStatusIcon" :size="11" />
                {{ attemptStatusLabel }}
              </span>
            </div>
            <div class="sd-hero__meta">
              <span v-if="studentCode" class="sd-hero__meta-item">
                <LucideIcon name="hash" :size="12" />
                {{ studentCode }}
              </span>
              <span v-if="studentEmail" class="sd-hero__meta-item">
                <LucideIcon name="mail" :size="12" />
                {{ studentEmail }}
              </span>
              <span class="sd-hero__meta-item">
                <LucideIcon name="clock" :size="12" />
                {{ sessionTime }}
              </span>
            </div>
            <div class="sd-hero__badges">
              <span class="sd-badge sd-badge--risk" :style="{ color: riskColor, background: riskBg, borderColor: riskColor + '30' }">
                <LucideIcon name="shield-alert" :size="11" />
                {{ riskLevelLabel }}
              </span>
              <span v-if="riskData.reviewRequired" class="sd-badge sd-badge--warn">
                <LucideIcon name="eye" :size="11" />
                Cần review
              </span>
              <span class="sd-badge sd-badge--neutral">
                <LucideIcon name="bar-chart-2" :size="11" />
                {{ violationCount }} vi phạm
              </span>
            </div>
          </div>
        </div>

        <!-- Risk Gauge -->
        <div class="sd-hero__right">
          <div class="sd-gauge-wrap">
            <svg class="sd-gauge-svg" viewBox="0 0 120 120">
              <circle class="sd-gauge-track" cx="60" cy="60" r="50" />
              <circle
                class="sd-gauge-fill"
                cx="60" cy="60" r="50"
                :stroke="riskColor"
                :stroke-dasharray="gaugeArc"
              />
              <circle class="sd-gauge-glow" cx="60" cy="60" r="50" :stroke="riskColor" opacity="0.15" stroke-dasharray="314" :stroke-dashoffset="gaugeDashOffset" />
            </svg>
            <div class="sd-gauge-center">
              <span class="sd-gauge-score" :style="{ color: riskColor }">{{ riskScore }}</span>
              <span class="sd-gauge-unit">điểm</span>
            </div>
          </div>
          <div class="sd-gauge-info">
            <p class="sd-gauge-desc">{{ riskDescription }}</p>
            <div class="sd-gauge-bar-track">
              <div class="sd-gauge-bar-fill" :style="{ width: riskScore + '%', background: riskColor }" />
            </div>
          </div>
        </div>
      </div>

      <!-- ── Stats Strip ──────────────────────────────────────── -->
      <div class="sd-stats-strip">
        <div class="sd-stat-tile" v-for="tile in statsTiles" :key="tile.label" :class="tile.cls">
          <div class="sd-stat-tile__icon">
            <LucideIcon :name="tile.icon" :size="16" />
          </div>
          <div class="sd-stat-tile__body">
            <span class="sd-stat-tile__value" :style="tile.valueStyle">{{ tile.value }}</span>
            <span class="sd-stat-tile__label">{{ tile.label }}</span>
          </div>
        </div>
      </div>

      <!-- ── Main Content Grid ────────────────────────────────── -->
      <div class="sd-grid">

        <!-- ── Left Column ── -->
        <div class="sd-col sd-col--main">

          <!-- Recommendation Card -->
          <div v-if="riskData.reviewRequired || riskData.recommendedAction || (riskData.reasons && riskData.reasons.length)" class="sd-card sd-card--action">
            <div class="sd-card__header">
              <LucideIcon name="lightbulb" :size="16" />
              <span class="sd-card__title">Đề xuất xử lý</span>
              <span class="sd-badge sd-badge--rec" :style="{ color: riskColor, background: riskBg, borderColor: riskColor + '30' }">
                {{ recommendedActionLabel }}
              </span>
            </div>
            <div class="sd-card__body">
              <div v-if="riskData.reasons && riskData.reasons.length" class="sd-reason-block">
                <p class="sd-section-label">Lý do chính</p>
                <div class="sd-reason-chips">
                  <span
                    v-for="reason in riskData.reasons"
                    :key="reason"
                    class="sd-reason-chip"
                    :style="{ color: '#fbbf24', background: 'rgba(251,191,36,0.1)', borderColor: 'rgba(251,191,36,0.25)' }"
                  >
                    <LucideIcon name="alert-triangle" :size="10" />
                    {{ reason }}
                  </span>
                </div>
              </div>
              <div v-if="riskData.evidenceSummary && riskData.evidenceSummary.length" class="sd-evidence-block">
                <p class="sd-section-label">Bằng chứng</p>
                <ul class="sd-evidence-list">
                  <li v-for="(item, i) in riskData.evidenceSummary" :key="i" class="sd-evidence-item">
                    <LucideIcon name="circle-dot" :size="10" class="sd-evidence-dot" />
                    {{ item }}
                  </li>
                </ul>
              </div>
            </div>
          </div>

          <!-- Violation Timeline -->
          <div class="sd-card">
            <div class="sd-card__header">
              <LucideIcon name="activity" :size="16" />
              <span class="sd-card__title">Dòng thời gian vi phạm</span>
              <span class="sd-card__badge">{{ timelineEvents.length }}</span>
            </div>
            <div class="sd-card__body--flush">
              <div v-if="timelineEvents.length === 0" class="sd-empty-state">
                <LucideIcon name="check-circle" :size="36" class="sd-empty-state__icon sd-empty-state__icon--success" />
                <p class="sd-empty-state__title">Không có vi phạm</p>
                <p class="sd-empty-state__sub">Học sinh không có hành vi đáng ngờ trong suốt phiên thi.</p>
              </div>
              <div v-else class="sd-timeline">
                <div v-for="(event, index) in timelineEvents" :key="event.key" class="sd-tl-item">
                  <div class="sd-tl-item__connector" v-if="index < timelineEvents.length - 1" />
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

        <!-- ── Right Column ── -->
        <div class="sd-col sd-col--side">

          <!-- Risk Breakdown -->
          <div v-if="hasBreakdown" class="sd-card">
            <div class="sd-card__header">
              <LucideIcon name="bar-chart-2" :size="16" />
              <span class="sd-card__title">Phân tích điểm rủi ro</span>
            </div>
            <div class="sd-card__body">
              <div v-for="(score, key) in riskData.breakdown" :key="key" class="sd-breakdown-row">
                <span class="sd-breakdown-label">{{ getVLabel(key) }}</span>
                <div class="sd-breakdown-bar-wrap">
                  <div class="sd-breakdown-bar">
                    <div class="sd-breakdown-fill" :style="{ width: Math.min(score, 100) + '%', background: sColor(score) }" />
                  </div>
                </div>
                <span class="sd-breakdown-score" :style="{ color: sColor(score) }">{{ score }}</span>
              </div>
            </div>
          </div>

          <!-- Suspicious Patterns -->
          <div v-if="suspiciousPatterns.length > 0" class="sd-card sd-card--warn">
            <div class="sd-card__header">
              <LucideIcon name="brain" :size="16" />
              <span class="sd-card__title">Mẫu hành vi đáng ngờ</span>
              <span class="sd-card__badge sd-card__badge--danger">{{ suspiciousPatterns.length }}</span>
            </div>
            <div class="sd-card__body--flush">
              <div
                v-for="p in suspiciousPatterns"
                :key="p.id"
                class="sd-pattern-card"
                :style="{ background: pBg(p.level), borderColor: pBorder(p.level) }"
              >
                <div class="sd-pattern-card__head">
                  <LucideIcon name="alert-triangle" :size="13" :style="{ color: pColor(p.level) }" />
                  <span class="sd-pattern-card__title">{{ p.title }}</span>
                  <span class="sd-pattern-card__level" :style="{ background: pColor(p.level) }">{{ p.level }}</span>
                </div>
                <p class="sd-pattern-card__desc">{{ p.description }}</p>
              </div>
            </div>
          </div>

          <!-- Realtime Feed -->
          <div class="sd-card">
            <div class="sd-card__header">
              <LucideIcon name="zap" :size="16" />
              <span class="sd-card__title">Sự kiện realtime</span>
              <span v-if="realtimeFeed.length" class="sd-card__badge">{{ realtimeFeed.length }}</span>
              <div class="sd-live-dot" :class="isRealtimeConnected ? 'sd-live-dot--on' : 'sd-live-dot--off'" :title="isRealtimeConnected ? 'Đang kết nối' : 'Đang chờ'" />
            </div>
            <div class="sd-card__body--flush">
              <div v-if="realtimeFeed.length === 0" class="sd-empty-compact">
                <LucideIcon name="inbox" :size="22" />
                <p>Chưa có sự kiện realtime</p>
              </div>
              <TransitionGroup v-else name="sd-feed" tag="div" class="sd-realtime-list">
                <div v-for="evt in realtimeFeed" :key="evt.id" class="sd-realtime-item" :class="`sd-realtime-item--${evt.tone}`">
                  <div class="sd-realtime-item__icon">
                    <LucideIcon :name="evt.icon" :size="13" />
                  </div>
                  <div class="sd-realtime-item__body">
                    <p class="sd-realtime-item__label">
                      {{ evt.label }}
                      <span v-if="evt.riskScore != null" class="sd-realtime-item__score">{{ evt.riskScore }}đ</span>
                    </p>
                    <p v-if="evt.message" class="sd-realtime-item__msg">{{ evt.message }}</p>
                  </div>
                  <span class="sd-realtime-item__time">{{ formatTime(evt.timestamp) }}</span>
                </div>
              </TransitionGroup>
            </div>
          </div>

          <!-- Latest Signals -->
          <div v-if="latestSignals.length > 0" class="sd-card">
            <div class="sd-card__header">
              <LucideIcon name="radio" :size="16" />
              <span class="sd-card__title">Tín hiệu gần đây</span>
            </div>
            <div class="sd-card__body--flush">
              <div v-for="sig in latestSignals" :key="sig.signalType + sig.createdAt" class="sd-signal-row">
                <div class="sd-signal-icon" :style="{ background: sigBg(sig.severity) }">
                  <LucideIcon name="alert-circle" :size="12" :style="{ color: sigFg(sig.severity) }" />
                </div>
                <div class="sd-signal-body">
                  <p class="sd-signal-type">{{ getVLabel(sig.signalType) }}</p>
                  <p class="sd-signal-evidence">{{ extractDetailsText(sig.evidence) || '—' }}</p>
                </div>
                <div class="sd-signal-meta">
                  <span class="sd-signal-conf">{{ Math.round((sig.confidence || 0) * 100) }}%</span>
                  <span class="sd-signal-sev" :class="`sd-signal-sev--${sig.severity?.toLowerCase()}`">{{ sig.severity }}</span>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>

      <!-- ── Action Bar ───────────────────────────────────────── -->
      <div class="sd-action-bar">
        <button class="sd-action-btn sd-action-btn--warn" :disabled="actionLoading === 'warning'" @click="issueWarning">
          <LucideIcon name="alert-triangle" :size="16" />
          <span>{{ actionLoading === 'warning' ? 'Đang gửi...' : 'Gửi cảnh báo' }}</span>
        </button>

        <button
          v-if="isStudentPaused"
          class="sd-action-btn sd-action-btn--resume"
          :disabled="actionLoading === 'resume'"
          @click="allowResume"
        >
          <LucideIcon name="play-circle" :size="16" />
          <span>{{ actionLoading === 'resume' ? 'Đang xử lý...' : 'Cho phép tiếp tục thi' }}</span>
        </button>

        <button
          v-else
          class="sd-action-btn sd-action-btn--danger"
          :disabled="actionLoading === 'invalidate' || isStudentTerminal"
          @click="suspendExam"
        >
          <LucideIcon name="x-circle" :size="16" />
          <span>{{ actionLoading === 'invalidate' ? 'Đang xử lý...' : 'Tạm dừng thi' }}</span>
        </button>

        <button class="sd-action-btn sd-action-btn--outline" @click="viewFullReport">
          <LucideIcon name="file-text" :size="16" />
          <span>Báo cáo đầy đủ</span>
        </button>

        <div class="sd-action-sep" />

        <div class="sd-action-meta">
          <span class="sd-action-meta__item">
            <LucideIcon name="user" :size="12" />
            {{ store.cards.length }} thí sinh
          </span>
          <span class="sd-action-meta__item sd-action-meta__item--success">
            <LucideIcon name="wifi" :size="12" />
            {{ onlineCount }} online
          </span>
          <span class="sd-action-meta__item sd-action-meta__item--warn">
            <LucideIcon name="alert-triangle" :size="12" />
            {{ alertCount }} nghi ngờ
          </span>
        </div>
      </div>

    </template>

    <!-- ── ConfirmDialog: Warning ── -->
    <ConfirmDialog
      v-model="showWarningDialog"
      variant="warning"
      title="Gửi cảnh báo"
      :message="`Gửi cảnh báo tới ${studentName}?`"
      confirm-label="Gửi cảnh báo"
      icon="alert-triangle"
      :show-reason="true"
      reason-label="Nội dung cảnh báo"
      :reason-placeholder="'Nhập nội dung cảnh báo (để trống = cảnh báo mặc định)'"
      :loading="actionLoading === 'warning'"
      @confirm="(reason) => handleWarningConfirm(reason)"
    />

    <!-- ── ConfirmDialog: Invalidate ── -->
    <ConfirmDialog
      v-model="showInvalidateDialog"
      variant="danger"
      title="Xác nhận dừng thi"
      message="Hành động này sẽ dừng bài thi. Không thể hoàn tác."
      confirm-label="Xác nhận dừng thi"
      icon="shield-alert"
      :show-reason="true"
      reason-label="Lý do dừng thi"
      :reason-placeholder="'Nhập lý do...'"
      :loading="actionLoading === 'invalidate'"
      @confirm="(reason) => handleInvalidateConfirm(reason)"
    />

    <!-- ── ConfirmDialog: Resume ── -->
    <ConfirmDialog
      v-model="showResumeDialog"
      variant="success"
      title="Cho phép tiếp tục thi"
      :message="`Học sinh ${studentName} sẽ được khôi phục bài thi và tiếp tục làm bài.`"
      confirm-label="Cho phép tiếp tục"
      icon="play-circle"
      :show-reason="true"
      reason-label="Lời nhắn"
      :reason-placeholder="'Lời nhắn tùy chọn...'"
      :loading="actionLoading === 'resume'"
      @confirm="(msg) => handleResumeConfirm(msg)"
    />

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
import ConfirmDialog from '../ui/ConfirmDialog.vue'

const REFRESH_INTERVAL_MS = 15000
const RISK_BAND_THRESHOLDS = { CRITICAL: 81, HIGH_RISK: 61, SUSPICIOUS: 31 }

const ATTEMPT_STATUS_META = {
  SUBMITTED: { label: 'Đã nộp', color: '#4ade80', bg: 'rgba(74,222,128,0.12)', icon: 'check-circle' },
  STOPPED: { label: 'Đã dừng', color: '#f87171', bg: 'rgba(248,113,113,0.12)', icon: 'x-circle' },
  PAUSED: { label: 'Tạm dừng', color: '#fbbf24', bg: 'rgba(251,191,36,0.12)', icon: 'pause-circle' },
  ACTIVE: { label: 'Đang thi', color: '#818cf8', bg: 'rgba(129,140,248,0.12)', icon: 'play-circle' },
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
  TAB_SWITCH: '#fbbf24', COPY_PASTE: '#f87171', DEVTOOLS_OPEN: '#f87171', EXIT_FULLSCREEN: '#fbbf24',
  MULTI_MONITOR: '#f87171', DUPLICATE_IP: '#f87171', PRINT_SCREEN: '#f87171', WINDOW_BLUR: '#fbbf24',
  IDLE_TIME: '#38bdf8', RIGHT_CLICK: '#38bdf8', HEARTBEAT_STALE: '#fbbf24', RAPID_QUESTION_SWITCH: '#fbbf24',
  DEVICE_FINGERPRINT_CHANGED: '#f87171', SYNC_BEHAVIOR: '#f87171', IP_FINGERPRINT_GRAPH: '#f87171',
  ANSWER_SIMILARITY: '#f87171', AI_MULTIPLE_FACES: '#f87171', AI_PHONE_DETECTED: '#f87171', AI_LOOKING_AWAY: '#fbbf24'
}

const VIOLATION_ICON_MAP = {
  TAB_SWITCH: 'layers', WINDOW_BLUR: 'layers', IDLE_TIME: 'clock', RIGHT_CLICK: 'mouse-pointer-2',
  EXIT_FULLSCREEN: 'minimize', COPY_PASTE: 'copy', DEVTOOLS_OPEN: 'code', PRINT_SCREEN: 'code',
  MULTI_MONITOR: 'monitor', DUPLICATE_IP: 'globe', RAPID_QUESTION_SWITCH: 'monitor', HEARTBEAT_STALE: 'wifi-off',
  DEVICE_FINGERPRINT_CHANGED: 'code', SYNC_BEHAVIOR: 'monitor', IP_FINGERPRINT_GRAPH: 'globe',
  ANSWER_SIMILARITY: 'copy', AI_MULTIPLE_FACES: 'monitor', AI_PHONE_DETECTED: 'monitor', AI_LOOKING_AWAY: 'wifi-off'
}

const VIOLATION_LABEL_MAP = {
  TAB_SWITCH: 'Chuyển tab', WINDOW_BLUR: 'Mất tiêu điểm cửa sổ', IDLE_TIME: 'Không hoạt động',
  RIGHT_CLICK: 'Click chuột phải', EXIT_FULLSCREEN: 'Thoát toàn màn hình', COPY_PASTE: 'Sao chép nội dung',
  DEVTOOLS_OPEN: 'Mở DevTools', PRINT_SCREEN: 'Chụp màn hình', MULTI_MONITOR: 'Nhiều màn hình',
  DUPLICATE_IP: 'IP trùng lặp', RAPID_QUESTION_SWITCH: 'Chuyển câu nhanh', HEARTBEAT_STALE: 'Mất kết nối',
  DEVICE_FINGERPRINT_CHANGED: 'Thay đổi thiết bị', SYNC_BEHAVIOR: 'Hành vi đồng bộ',
  IP_FINGERPRINT_GRAPH: 'Liên kết IP/fingerprint', ANSWER_SIMILARITY: 'Tương đồng đáp án',
  AI_MULTIPLE_FACES: 'Nhiều khuôn mặt', AI_PHONE_DETECTED: 'Phát hiện điện thoại', AI_LOOKING_AWAY: 'Nhìn lệch hướng'
}

const SEVERITY_LABEL_MAP = { HIGH: 'Nghiêm trọng', MEDIUM: 'Trung bình', LOW: 'Thấp', CRITICAL: 'Nghiêm trọng' }
const RISK_LEVEL_LABEL_MAP = { CRITICAL: 'Nguy cơ cao', HIGH_RISK: 'Rủi ro cao', SUSPICIOUS: 'Đáng ngờ', CLEAN: 'Bình thường' }
const RECOMMENDED_ACTION_MAP = {
  PAUSE_AND_REVIEW: 'Tạm dừng và kiểm tra ngay', WARN_AND_ESCALATE: 'Cảnh báo và tăng cường giám sát',
  REVIEW_ATTEMPT: 'Mở hồ sơ để review thủ công', CONTINUE_MONITORING: 'Tiếp tục giám sát'
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
const cleanName = (raw) => (raw == null ? '' : String(raw).trim())
const studentName = computed(() => {
  const s = riskData.value.student
  const fromRisk = cleanName(s?.name || s?.username)
  if (fromRisk) return fromRisk
  const fromStore = cleanName(storeCard.value?.student || storeCard.value?.name)
  if (fromStore) return fromStore
  return cleanName(route.query.student || route.query.studentName) || '—'
})
const studentCode = computed(() => {
  const s = riskData.value.student
  if (s) return s.studentCode || String(s.id || '—')
  if (storeCard.value?.studentCode) return storeCard.value.studentCode
  return route.query.studentId ? String(route.query.studentId) : '—'
})
const studentEmail = computed(() =>
  riskData.value.student?.email || storeCard.value?.email || route.query.studentEmail || ''
)
const studentInitials = computed(() => {
  const name = studentName.value
  if (!name || name === '—') return '?'
  const parts = name.trim().split(/\s+/)
  const last = parts[parts.length - 1] || ''
  const first = parts[0] || ''
  return (last.charAt(0) + (parts.length > 1 ? first.charAt(0) : '')).toUpperCase()
})

// ── Attempt status ────────────────────────────────────────────────────────────
const storeCard = computed(() => {
  if (!attemptId.value) return null
  const id = Number(attemptId.value)
  return attempts.value.find(c => (c.id || c.attemptId) === id || String(c.id || c.attemptId) === String(attemptId.value)) || null
})
const resolvedStatus = computed(() =>
  riskData.value.status || riskData.value.attempt?.status || storeCard.value?.status || ''
)
const attemptStatusToken = computed(() => {
  const s = String(resolvedStatus.value).toUpperCase()
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
const isStudentPaused = computed(() => isAttemptPaused(resolvedStatus.value))
const isStudentTerminal = computed(() => isAttemptTerminal(resolvedStatus.value))
const sessionTime = computed(() => {
  const ts = riskData.value.attempt?.startedAt
  if (!ts) return '—'
  const diff = Math.floor((Date.now() - new Date(ts).getTime()) / 60000)
  if (diff < 1) return '<1 phút'
  if (diff < 60) return `${diff} phút`
  return `${Math.floor(diff / 60)}h ${diff % 60}p`
})

// ── Session stats ─────────────────────────────────────────────────────────────
const onlineCount = computed(() => attempts.value.filter(a => /^(ACTIVE|IN_PROGRESS)$/i.test(a.status || '')).length)
const alertCount = computed(() => attempts.value.filter(a => Number(a.riskScore || 0) > 30 || Boolean(a.reviewRequired)).length)
const submittedCount = computed(() => attempts.value.filter(a => /SUBMITTED/i.test(a.status || '')).length)

// ── Stats tiles ───────────────────────────────────────────────────────────────
const statsTiles = computed(() => [
  { icon: 'users', label: 'Tổng thí sinh', value: store.cards.length, valueStyle: { color: 'var(--ds-text)' }, cls: '' },
  { icon: 'wifi', label: 'Đang thi', value: onlineCount.value, valueStyle: { color: '#4ade80' }, cls: 'sd-stat-tile--success' },
  { icon: 'alert-triangle', label: 'Nghi ngờ', value: alertCount.value, valueStyle: { color: '#fbbf24' }, cls: 'sd-stat-tile--warn' },
  { icon: 'check-circle', label: 'Đã nộp', value: submittedCount.value, valueStyle: { color: '#818cf8' }, cls: 'sd-stat-tile--info' },
  { icon: 'shield-alert', label: 'Vi phạm', value: violationCount.value, valueStyle: { color: violationCount.value > 0 ? '#f87171' : 'var(--ds-text-muted)' }, cls: 'sd-stat-tile--danger' }
])

// ── Risk ──────────────────────────────────────────────────────────────────────
const riskScore = computed(() => Number(riskData.value.score ?? storeCard.value?.riskScore ?? 0))
const riskBand = computed(() => {
  const s = riskScore.value
  if (s >= RISK_BAND_THRESHOLDS.HIGH_RISK) return 'danger'
  if (s >= RISK_BAND_THRESHOLDS.SUSPICIOUS) return 'warn'
  return 'clean'
})
const riskLevelLabel = computed(() => RISK_LEVEL_LABEL_MAP[riskData.value.level] || riskData.value.level || '—')
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
  if (s >= RISK_BAND_THRESHOLDS.HIGH_RISK) return 'rgba(248,113,113,0.12)'
  if (s >= RISK_BAND_THRESHOLDS.SUSPICIOUS) return 'rgba(251,191,36,0.12)'
  return 'rgba(74,222,128,0.12)'
})
const avatarBg = computed(() => riskBg.value)
const recommendedActionLabel = computed(() => RECOMMENDED_ACTION_MAP[riskData.value.recommendedAction] || 'Tiếp tục giám sát')
const hasBreakdown = computed(() => {
  const b = riskData.value.breakdown
  return b && Object.keys(b).length > 0
})

// ── Gauge SVG ─────────────────────────────────────────────────────────────────
const gaugeArc = computed(() => {
  const circ = 2 * Math.PI * 50
  const ratio = Math.max(0, Math.min(100, riskScore.value)) / 100
  return `${ratio * circ} ${circ}`
})
const gaugeDashOffset = computed(() => {
  const circ = 2 * Math.PI * 50
  return circ - (Math.max(0, Math.min(100, riskScore.value)) / 100) * circ
})

// ── Details extraction ─────────────────────────────────────────────────────────
function extractDetailsText(raw) {
  if (raw == null) return ''
  if (typeof raw !== 'string') {
    if (typeof raw === 'object') return raw.details || raw.message || raw.evidence || ''
    return String(raw)
  }
  const trimmed = raw.trim()
  if (!trimmed) return ''
  if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
    try {
      const parsed = JSON.parse(trimmed)
      if (parsed && typeof parsed === 'object') return parsed.details || parsed.message || parsed.evidence || parsed.reason || ''
    } catch { /* ignore */ }
  }
  return raw
}

// ── Timeline events ───────────────────────────────────────────────────────────
// Count only actual fraud signals (not system events or exam events) as violations
const timelineEvents = computed(() => {
  const evts = []
  const seen = new Set()
  for (const item of timeline.value) {
    const at = item.at || item.createdAt || item.timestamp
    const eventType = item.eventType || item.signalType || ''
    const key = `${eventType}-${at}`
    if (seen.has(key)) continue
    seen.add(key)
    // Skip non-fraud-signal items for the timeline display
    const isFraudSignal = eventType === 'FRAUD_SIGNAL' || item.type === 'FRAUD_SIGNAL'
    evts.push({
      key,
      at,
      eventType,
      details: extractDetailsText(item.details) || extractDetailsText(item.evidence) || '',
      severity: item.severity || mapSeverity(eventType, item.confidence),
      isViolation: isFraudSignal
    })
  }
  return evts
})

const eventStats = computed(() => {
  const counts = { severity: { HIGH: 0, MEDIUM: 0, LOW: 0 }, pattern: { TAB_SWITCH: 0, COPY: 0, EXIT_FULLSCREEN: 0, DEVTOOLS: 0, DUPLICATE_IP: 0, SYNC: 0 } }
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

// Only actual fraud signals count as violations (not system/monitoring events)
const violationCount = computed(() => timelineEvents.value.filter(e => e.isViolation).length)
const maxSeverity = computed(() => {
  const { HIGH, MEDIUM, LOW } = eventStats.value.severity
  if (HIGH > 0) return 'Nghiêm trọng'
  if (MEDIUM > 0) return 'Trung bình'
  if (LOW > 0) return 'Thấp'
  return '—'
})
const latestSignals = computed(() => riskData.value.latestSignals || [])
const suspiciousPatterns = computed(() =>
  PATTERN_RULES.filter(rule => eventStats.value.pattern[rule.key] >= rule.threshold)
    .map(rule => ({ id: rule.id, ...rule.build(eventStats.value.pattern[rule.key]) }))
)
const lastUpdatedLabel = computed(() => {
  if (!lastUpdatedAt.value) return 'Chưa cập nhật'
  return `Cập nhật ${new Date(lastUpdatedAt.value).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}`
})

// ── Data loading ──────────────────────────────────────────────────────────────
function resolveLoadErrorMessage(err) {
  const status = err?.status
  if (status === 404) return 'Không tìm thấy bài làm này.'
  if (status === 403) return 'Bạn không có quyền xem chi tiết bài làm này.'
  if (status === 401) return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
  if (status === 0) return 'Không thể kết nối tới máy chủ.'
  const msg = err?.message
  if (msg && /^(Attempt|User|Exam) not found/i.test(msg)) return 'Không tìm thấy bài làm này.'
  return msg || 'Không thể tải dữ liệu giám sát.'
}

async function loadData(silent = false) {
  if (!attemptId.value) { error.value = 'Thiếu mã bài làm.'; return }
  if (silent) isRefreshing.value = true
  else { loading.value = true; error.value = '' }
  try {
    const [riskRes, timelineRes] = await Promise.allSettled([
      fetchAttemptRisk(attemptId.value),
      listMonitoringTimeline(attemptId.value)
    ])
    if (riskRes.status === 'fulfilled') riskData.value = riskRes.value || {}
    else if (!silent && Object.keys(riskData.value).length === 0) throw riskRes.reason
    if (timelineRes.status === 'fulfilled') timeline.value = timelineRes.value || []
    lastUpdatedAt.value = Date.now()
  } catch (err) {
    error.value = resolveLoadErrorMessage(err)
    if (!silent) toast.error(error.value)
  } finally {
    loading.value = false
    isRefreshing.value = false
  }
}

async function ensureSessionContext() {
  if (!examId.value) return
  if (attempts.value.length > 0) return
  try {
    const fetched = await listExamAttempts(examId.value)
    if (Array.isArray(fetched) && fetched.length > 0) store.setCards(fetched)
  } catch { /* silent */ }
}

function startAutoRefresh() {
  stopAutoRefresh()
  refreshTimer = window.setInterval(() => {
    if (document.hidden || loading.value || isRefreshing.value) return
    void loadData(true)
  }, REFRESH_INTERVAL_MS)
}
function stopAutoRefresh() {
  if (refreshTimer) { window.clearInterval(refreshTimer); refreshTimer = null }
}

// ── Realtime ─────────────────────────────────────────────────────────────────
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
    riskData.value = { ...riskData.value, score: payload.riskScore ?? riskData.value.score, level: payload.riskLevel || riskData.value.level, reasons: payload.reasons || riskData.value.reasons, recommendedAction: payload.recommendedAction || riskData.value.recommendedAction, reviewRequired: payload.reviewRequired ?? riskData.value.reviewRequired }
    lastUpdatedAt.value = Date.now()
  }
  if (type === 'ATTEMPT_PAUSED' || type === 'ATTEMPT_RESUMED' || type === 'ATTEMPT_STOPPED') {
    riskData.value = { ...riskData.value, status: payload.status || (type === 'ATTEMPT_PAUSED' ? 'PAUSED' : type === 'ATTEMPT_STOPPED' ? 'STOPPED' : 'IN_PROGRESS') }
    void loadData(true)
  }
  pushRealtimeEvent(payload)
}

async function connectRealtime() {
  if (!attemptId.value) return
  await realtime.connect({
    reconnectDelay: 5000,
    topics: [{ destination: `/topic/attempts/${attemptId.value}/proctor-actions`, handler: handleRealtimePayload }]
  })
}

// ── Actions ───────────────────────────────────────────────────────────────────
function goBack() { router.back() }
function issueWarning() { warningMessage.value = ''; showWarningDialog.value = true }
function suspendExam() { invalidateReason.value = ''; showInvalidateDialog.value = true }
function allowResume() { resumeMessage.value = ''; showResumeDialog.value = true }

async function runAttemptAction({ key, call, payload, successMsg, errorMsg, closeDialog }) {
  if (!attemptId.value) return
  actionLoading.value = key
  closeDialog()
  try {
    await call(attemptId.value, payload)
    toast.success(successMsg)
    await loadData(true)
  } catch (err) {
    toast.error(resolveLoadErrorMessage(err) || errorMsg)
  } finally {
    actionLoading.value = ''
  }
}

async function confirmSendWarning() {
  await runAttemptAction({ key: 'warning', call: sendTeacherWarning, payload: warningMessage.value, successMsg: 'Đã gửi cảnh báo tới học sinh.', errorMsg: 'Gửi cảnh báo thất bại.', closeDialog: () => { showWarningDialog.value = false; warningMessage.value = '' } })
}
async function confirmInvalidate() {
  await runAttemptAction({ key: 'invalidate', call: invalidateAttempt, payload: invalidateReason.value, successMsg: 'Đã dừng bài thi.', errorMsg: 'Dừng thi thất bại.', closeDialog: () => { showInvalidateDialog.value = false; invalidateReason.value = '' } })
}
async function confirmResume() {
  await runAttemptAction({ key: 'resume', call: resumeAttempt, payload: resumeMessage.value, successMsg: 'Đã cho phép học sinh tiếp tục bài thi.', errorMsg: 'Khôi phục bài thi thất bại.', closeDialog: () => { showResumeDialog.value = false; resumeMessage.value = '' } })
}

// ConfirmDialog-compatible handlers (receive reason from the dialog)
async function handleWarningConfirm(reason) {
  await runAttemptAction({ key: 'warning', call: sendTeacherWarning, payload: reason, successMsg: 'Đã gửi cảnh báo tới học sinh.', errorMsg: 'Gửi cảnh báo thất bại.', closeDialog: () => { showWarningDialog.value = false; warningMessage.value = '' } })
}
async function handleInvalidateConfirm(reason) {
  await runAttemptAction({ key: 'invalidate', call: invalidateAttempt, payload: reason, successMsg: 'Đã dừng bài thi.', errorMsg: 'Dừng thi thất bại.', closeDialog: () => { showInvalidateDialog.value = false; invalidateReason.value = '' } })
}
async function handleResumeConfirm(msg) {
  await runAttemptAction({ key: 'resume', call: resumeAttempt, payload: msg, successMsg: 'Đã cho phép học sinh tiếp tục bài thi.', errorMsg: 'Khôi phục bài thi thất bại.', closeDialog: () => { showResumeDialog.value = false; resumeMessage.value = '' } })
}

function copyAttemptId() {
  if (!attemptId.value) return
  navigator.clipboard.writeText(String(attemptId.value)).then(() => {
    toast.success('Đã sao chép mã bài làm')
  }).catch(() => {
    toast.error('Không thể sao chép')
  })
}

function viewFullReport() {
  router.push({ path: '/teacher/exams/review/incidents', query: examId.value ? { examId: examId.value } : {} })
}

function handleEscape(e) {
  if (e.key !== 'Escape') return
  if (showWarningDialog.value) showWarningDialog.value = false
  else if (showInvalidateDialog.value) showInvalidateDialog.value = false
  else if (showResumeDialog.value) showResumeDialog.value = false
}

function resetAttemptState() {
  riskData.value = {}; timeline.value = []; realtimeFeed.value = []; error.value = ''; lastUpdatedAt.value = null
}

watch(attemptId, async (next, prev) => {
  if (!next || next === prev) return
  resetAttemptState()
  realtime.disconnect()
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
/* ── Root ──────────────────────────────────────────────────────────── */
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

/* ── Loading ─────────────────────────────────────────────────────── */
.sd-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  height: 60vh;
}
.sd-loading__spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--ds-border);
  border-top-color: var(--ds-primary);
  border-radius: 50%;
  animation: sd-spin 0.7s linear infinite;
}
.sd-loading__text { color: var(--ds-text-secondary); font-size: 0.875rem; }
@keyframes sd-spin { to { transform: rotate(360deg); } }

/* ── Error ──────────────────────────────────────────────────────── */
.sd-error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60vh;
}
.sd-error__card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 2.5rem;
  border: 1px solid var(--ds-danger-soft);
  border-radius: var(--ds-radius-xl);
  background: var(--ds-danger-bg);
  max-width: 380px;
  text-align: center;
}
.sd-error__icon { color: var(--ds-danger); }
.sd-error__title { font-size: 1rem; font-weight: 800; color: var(--ds-danger); margin: 0; }
.sd-error__msg { font-size: 0.825rem; color: var(--ds-danger); margin: 0; }

/* ── Top Command Bar ────────────────────────────────────────────── */
.sd-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.625rem 1.5rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-surface);
  position: sticky;
  top: 0;
  z-index: 50;
  gap: 1rem;
  box-shadow: var(--ds-shadow-xs);
}
.sd-topbar__left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 0;
}
.sd-back-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.35rem 0.875rem;
  border-radius: var(--ds-radius-md);
  border: 1px solid var(--ds-border);
  background: var(--ds-surface);
  color: var(--ds-text-secondary);
  font-size: 0.775rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: all 0.15s;
}
.sd-back-btn:hover { background: var(--ds-primary-soft); border-color: var(--ds-primary-border); color: var(--ds-primary); }
.sd-topbar__divider { width: 1px; height: 20px; background: var(--ds-border); flex-shrink: 0; }
.sd-breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.775rem;
  min-width: 0;
  overflow: hidden;
}
.sd-breadcrumb__link {
  color: var(--ds-text-secondary);
  text-decoration: none;
  white-space: nowrap;
  transition: color 0.15s;
  flex-shrink: 0;
}
.sd-breadcrumb__link:hover { color: var(--ds-primary); }
.sd-breadcrumb__sep { color: var(--ds-text-muted); flex-shrink: 0; }
.sd-breadcrumb__current {
  color: var(--ds-text);
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sd-topbar__right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}
.sd-conn-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.2rem 0.6rem;
  border-radius: 9999px;
  font-size: 0.62rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  border: 1px solid transparent;
}
.sd-conn-badge__dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
.sd-conn-badge--live { background: rgba(22,163,74,0.1); color: var(--ds-success); border-color: rgba(22,163,74,0.2); }
.sd-conn-badge--live .sd-conn-badge__dot { animation: sd-pulse 1.4s ease-in-out infinite; }
.sd-conn-badge--off { background: var(--ds-surface-muted); color: var(--ds-text-muted); border-color: var(--ds-border); }
@keyframes sd-pulse { 0%,100%{opacity:1;transform:scale(1)} 50%{opacity:0.4;transform:scale(0.7)} }
.sd-topbar__updated { font-size: 0.72rem; color: var(--ds-text-muted); white-space: nowrap; }
.sd-topbar__refresh {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.35rem 0.875rem;
  border-radius: var(--ds-radius-md);
  border: 1px solid var(--ds-primary-border);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  font-size: 0.745rem;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s;
}
.sd-topbar__refresh:hover:not(:disabled) { background: var(--ds-primary); color: #fff; }
.sd-topbar__refresh:disabled { opacity: 0.5; cursor: not-allowed; }
.sd-spin { animation: sd-spin 0.8s linear infinite; }
.sd-attempt-id {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 0.2rem 0.625rem;
  border-radius: 9999px;
  background: var(--ds-surface-muted);
  color: var(--ds-text-muted);
  white-space: nowrap;
}
.sd-copy-btn {
  display: inline-flex;
  align-items: center;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0;
  color: var(--ds-text-muted);
  transition: color 0.15s;
}
.sd-copy-btn:hover { color: var(--ds-primary); }

/* ── Hero ───────────────────────────────────────────────────────── */
.sd-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  padding: 1.5rem 2rem;
  background: var(--ds-surface);
  border-radius: 0 0 var(--ds-radius-xl) var(--ds-radius-xl);
  margin: 0 1.5rem;
  border: 1px solid var(--ds-border);
  border-top: 4px solid;
  box-shadow: var(--ds-shadow-sm);
  flex-wrap: wrap;
}
.sd-hero--danger { border-top-color: var(--ds-danger); }
.sd-hero--warn { border-top-color: var(--ds-warning); }
.sd-hero--clean { border-top-color: var(--ds-success); }

.sd-hero__left {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  min-width: 0;
  flex: 1;
}

/* Avatar */
.sd-avatar-wrap {
  position: relative;
  flex-shrink: 0;
}
.sd-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.sd-avatar__initials { font-size: 1.375rem; font-weight: 900; }
.sd-avatar__ring {
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  border: 3px solid;
}
.sd-avatar__risk-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2.5px solid var(--ds-surface);
}

/* Hero info */
.sd-hero__info { min-width: 0; }
.sd-hero__name-row {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  margin-bottom: 0.375rem;
  flex-wrap: wrap;
}
.sd-hero__name {
  font-size: 1.25rem;
  font-weight: 900;
  color: var(--ds-text);
  margin: 0;
  letter-spacing: -0.01em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 320px;
}
.sd-status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.2rem 0.625rem;
  border-radius: 9999px;
  font-size: 0.68rem;
  font-weight: 700;
  white-space: nowrap;
  flex-shrink: 0;
}
.sd-hero__meta {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  margin-bottom: 0.625rem;
  flex-wrap: wrap;
}
.sd-hero__meta-item {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.75rem;
  color: var(--ds-text-secondary);
  font-weight: 600;
}
.sd-hero__badges {
  display: flex;
  gap: 0.375rem;
  flex-wrap: wrap;
}

/* Risk Gauge */
.sd-hero__right {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  flex-shrink: 0;
}
.sd-gauge-wrap {
  position: relative;
  width: 100px;
  height: 100px;
  flex-shrink: 0;
}
.sd-gauge-svg {
  transform: rotate(-90deg);
  width: 100%;
  height: 100%;
}
.sd-gauge-track { fill: none; stroke: var(--ds-gray-100); stroke-width: 8; }
.sd-gauge-fill { fill: none; stroke-width: 8; stroke-linecap: round; transition: stroke-dasharray 0.6s ease; }
.sd-gauge-glow { fill: none; stroke-width: 10; stroke-linecap: round; filter: blur(4px); }
.sd-gauge-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}
.sd-gauge-score { font-size: 1.75rem; font-weight: 900; line-height: 1; }
.sd-gauge-unit { font-size: 0.6rem; color: var(--ds-text-muted); font-weight: 600; }
.sd-gauge-info { min-width: 180px; }
.sd-gauge-desc { font-size: 0.8rem; color: var(--ds-text-secondary); margin: 0 0 0.5rem; }
.sd-gauge-bar-track { height: 5px; border-radius: 9999px; background: var(--ds-gray-100); overflow: hidden; }
.sd-gauge-bar-fill { height: 100%; border-radius: 9999px; transition: width 0.6s ease; }

/* ── Stats Strip ─────────────────────────────────────────────────── */
.sd-stats-strip {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 0.75rem;
  margin: 1.25rem 1.5rem 0;
}
.sd-stat-tile {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1rem 1.125rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  box-shadow: var(--ds-shadow-xs);
  transition: all 0.15s;
}
.sd-stat-tile:hover { box-shadow: var(--ds-shadow-sm); transform: translateY(-1px); border-color: var(--ds-primary-border); }
.sd-stat-tile__icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ds-surface-muted);
  color: var(--ds-text-muted);
  flex-shrink: 0;
}
.sd-stat-tile--success .sd-stat-tile__icon { background: rgba(74,222,128,0.12); color: #4ade80; }
.sd-stat-tile--warn .sd-stat-tile__icon { background: rgba(251,191,36,0.12); color: #fbbf24; }
.sd-stat-tile--info .sd-stat-tile__icon { background: rgba(129,140,248,0.12); color: #818cf8; }
.sd-stat-tile--danger .sd-stat-tile__icon { background: rgba(248,113,113,0.1); color: #f87171; }
.sd-stat-tile__body { display: flex; flex-direction: column; min-width: 0; }
.sd-stat-tile__value { font-size: 1.375rem; font-weight: 900; line-height: 1.1; font-variant-numeric: tabular-nums; }
.sd-stat-tile__label { font-size: 0.65rem; color: var(--ds-text-muted); margin-top: 2px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; }

/* ── Grid ──────────────────────────────────────────────────────── */
.sd-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 1.25rem;
  margin: 1.25rem 1.5rem 0;
  align-items: start;
}
.sd-col { display: flex; flex-direction: column; gap: 1.25rem; }

/* ── Cards ─────────────────────────────────────────────────────── */
.sd-card {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  overflow: hidden;
  box-shadow: var(--ds-shadow-xs);
}
.sd-card--warn { border-top: 3px solid var(--ds-warning); }
.sd-card--action { border-top: 3px solid var(--ds-primary); }
.sd-card__header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.875rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
  color: var(--ds-text-secondary);
  background: var(--ds-surface-muted);
}
.sd-card__title {
  font-size: 0.825rem;
  font-weight: 800;
  color: var(--ds-text);
  flex: 1;
}
.sd-card__badge {
  font-size: 0.68rem;
  font-weight: 800;
  padding: 0.15rem 0.5rem;
  border-radius: 9999px;
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
}
.sd-card__badge--danger { background: var(--ds-danger-bg); color: var(--ds-danger); }
.sd-card__body { padding: 1.25rem; }
.sd-card__body--flush { padding: 0; }

/* ── Badges ─────────────────────────────────────────────────────── */
.sd-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.2rem 0.625rem;
  border-radius: 9999px;
  border: 1px solid transparent;
  font-size: 0.7rem;
  font-weight: 700;
}
.sd-badge--risk {}
.sd-badge--warn { color: var(--ds-warning); background: var(--ds-warning-bg); border-color: var(--ds-warning-soft); }
.sd-badge--neutral { color: var(--ds-text-muted); background: var(--ds-surface-muted); border-color: var(--ds-border); }
.sd-badge--rec {}

/* ── Section Label ──────────────────────────────────────────────── */
.sd-section-label {
  font-size: 0.68rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--ds-text-muted);
  margin: 0 0 0.625rem;
}

/* ── Reason Block ──────────────────────────────────────────────── */
.sd-reason-block { margin-bottom: 1rem; }
.sd-reason-chips { display: flex; gap: 0.375rem; flex-wrap: wrap; }
.sd-reason-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  font-size: 0.72rem;
  font-weight: 700;
  border: 1px solid;
}
.sd-evidence-block {}
.sd-evidence-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.375rem; }
.sd-evidence-item { display: flex; align-items: flex-start; gap: 0.5rem; font-size: 0.8rem; color: var(--ds-text-secondary); }
.sd-evidence-dot { color: var(--ds-text-muted); margin-top: 4px; flex-shrink: 0; }

/* ── Timeline ─────────────────────────────────────────────────── */
.sd-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 3rem;
  color: var(--ds-text-muted);
  text-align: center;
}
.sd-empty-state__icon { margin-bottom: 0.25rem; }
.sd-empty-state__icon--success { color: var(--ds-success); }
.sd-empty-state__title { font-size: 0.9rem; font-weight: 800; color: var(--ds-text); margin: 0; }
.sd-empty-state__sub { font-size: 0.8rem; margin: 0; }

.sd-timeline { display: flex; flex-direction: column; }
.sd-tl-item {
  display: flex;
  align-items: flex-start;
  gap: 0;
  position: relative;
  padding: 0 1.25rem;
}
.sd-tl-item__connector {
  position: absolute;
  left: calc(1.25rem + 13px);
  top: 28px;
  width: 2px;
  bottom: 0;
  background: var(--ds-border);
}
.sd-tl-item__dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 0.875rem;
  margin-right: 0.875rem;
  position: relative;
  z-index: 1;
  box-shadow: 0 0 0 3px var(--ds-surface);
}
.sd-tl-item__content {
  flex: 1;
  min-width: 0;
  padding: 0.875rem 0;
  border-bottom: 1px solid var(--ds-border);
}
.sd-tl-item:last-child .sd-tl-item__content { border-bottom: none; }
.sd-tl-item__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}
.sd-tl-item__type { font-size: 0.825rem; font-weight: 800; }
.sd-tl-item__time { font-size: 0.72rem; color: var(--ds-text-muted); white-space: nowrap; flex-shrink: 0; font-variant-numeric: tabular-nums; }
.sd-tl-item__details { font-size: 0.78rem; color: var(--ds-text-secondary); margin: 0 0 0.375rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sd-tl-item__severity {
  display: inline-flex;
  padding: 0.15rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.65rem;
  font-weight: 700;
}
.sd-tl-item__severity--high { background: var(--ds-danger-bg); color: var(--ds-danger); }
.sd-tl-item__severity--medium { background: var(--ds-warning-bg); color: var(--ds-warning); }
.sd-tl-item__severity--low { background: var(--ds-info-bg); color: var(--ds-info); }

/* ── Breakdown ──────────────────────────────────────────────────── */
.sd-breakdown-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.875rem;
}
.sd-breakdown-row:last-child { margin-bottom: 0; }
.sd-breakdown-label {
  font-size: 0.78rem;
  color: var(--ds-text-secondary);
  min-width: 140px;
  max-width: 140px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 600;
}
.sd-breakdown-bar-wrap { flex: 1; }
.sd-breakdown-bar { height: 6px; border-radius: 9999px; background: var(--ds-gray-100); overflow: hidden; }
.sd-breakdown-fill { height: 100%; border-radius: 9999px; transition: width 0.5s ease; }
.sd-breakdown-score { font-size: 0.8rem; font-weight: 800; min-width: 28px; text-align: right; font-variant-numeric: tabular-nums; }

/* ── Pattern Cards ────────────────────────────────────────────── */
.sd-pattern-card {
  padding: 0.875rem 1.25rem;
  border: 1px solid;
  border-radius: var(--ds-radius-lg);
  margin: 0.75rem 1rem;
}
.sd-pattern-card__head {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}
.sd-pattern-card__title { font-size: 0.825rem; font-weight: 800; color: var(--ds-text); flex: 1; }
.sd-pattern-card__level {
  font-size: 0.6rem;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  padding: 0.15rem 0.4rem;
  border-radius: 9999px;
  color: white;
}
.sd-pattern-card__desc { font-size: 0.75rem; color: var(--ds-text-secondary); padding-left: 1.5rem; margin: 0; }

/* ── Live Dot ──────────────────────────────────────────────────── */
.sd-live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.sd-live-dot--on { background: var(--ds-success); animation: sd-pulse 1.4s ease-in-out infinite; }
.sd-live-dot--off { background: var(--ds-text-muted); }

/* ── Realtime List ─────────────────────────────────────────────── */
.sd-empty-compact {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  padding: 2rem;
  color: var(--ds-text-muted);
  font-size: 0.8rem;
}
.sd-realtime-list { display: flex; flex-direction: column; }
.sd-realtime-item {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
  padding: 0.75rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
  border-left: 3px solid transparent;
}
.sd-realtime-item:last-child { border-bottom: none; }
.sd-realtime-item--danger { border-left-color: var(--ds-danger); }
.sd-realtime-item--warn { border-left-color: var(--ds-warning); }
.sd-realtime-item--success { border-left-color: var(--ds-success); }
.sd-realtime-item--neutral { border-left-color: var(--ds-text-muted); }
.sd-realtime-item__icon {
  width: 26px;
  height: 26px;
  border-radius: var(--ds-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--ds-surface-muted);
  color: var(--ds-text-muted);
}
.sd-realtime-item--danger .sd-realtime-item__icon { background: var(--ds-danger-bg); color: var(--ds-danger); }
.sd-realtime-item--warn .sd-realtime-item__icon { background: var(--ds-warning-bg); color: var(--ds-warning); }
.sd-realtime-item--success .sd-realtime-item__icon { background: var(--ds-success-bg); color: var(--ds-success); }
.sd-realtime-item__body { flex: 1; min-width: 0; }
.sd-realtime-item__label {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--ds-text);
  margin: 0 0 0.15rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.sd-realtime-item__score {
  font-size: 0.65rem;
  font-weight: 800;
  padding: 0.1rem 0.4rem;
  border-radius: 9999px;
  background: var(--ds-warning-bg);
  color: var(--ds-warning);
}
.sd-realtime-item__msg { font-size: 0.72rem; color: var(--ds-text-secondary); margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sd-realtime-item__time { font-size: 0.65rem; color: var(--ds-text-muted); white-space: nowrap; flex-shrink: 0; font-variant-numeric: tabular-nums; margin-top: 0.2rem; }
.sd-feed-enter-active { transition: all 0.25s ease; }
.sd-feed-enter-from { opacity: 0; transform: translateX(-8px); }

/* ── Signal Row ───────────────────────────────────────────────── */
.sd-signal-row {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
  padding: 0.75rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
}
.sd-signal-row:last-child { border-bottom: none; }
.sd-signal-icon {
  width: 28px;
  height: 28px;
  border-radius: var(--ds-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.sd-signal-body { flex: 1; min-width: 0; }
.sd-signal-type { font-size: 0.8rem; font-weight: 700; color: var(--ds-text); margin: 0 0 0.15rem; }
.sd-signal-evidence { font-size: 0.72rem; color: var(--ds-text-secondary); margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sd-signal-meta { display: flex; flex-direction: column; align-items: flex-end; gap: 0.25rem; flex-shrink: 0; }
.sd-signal-conf { font-size: 0.8rem; font-weight: 800; color: var(--ds-text); font-variant-numeric: tabular-nums; }
.sd-signal-sev { font-size: 0.6rem; font-weight: 800; padding: 0.15rem 0.4rem; border-radius: 9999px; }
.sd-signal-sev--high { background: var(--ds-danger-bg); color: var(--ds-danger); }
.sd-signal-sev--medium { background: var(--ds-warning-bg); color: var(--ds-warning); }
.sd-signal-sev--low { background: var(--ds-info-bg); color: var(--ds-info); }

/* ── Action Bar ───────────────────────────────────────────────── */
.sd-action-bar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1.25rem 1.5rem 2rem;
  flex-wrap: wrap;
}
.sd-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.375rem;
  border-radius: var(--ds-radius-md);
  font-size: 0.825rem;
  font-weight: 700;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.15s;
  box-shadow: var(--ds-shadow-xs);
}
.sd-action-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.sd-action-btn--warn { background: var(--ds-warning-bg); color: var(--ds-warning); border-color: var(--ds-warning-soft); }
.sd-action-btn--warn:hover:not(:disabled) { background: var(--ds-warning); color: #fff; border-color: var(--ds-warning); box-shadow: var(--ds-shadow-sm); }
.sd-action-btn--danger { background: var(--ds-danger-bg); color: var(--ds-danger); border-color: var(--ds-danger-soft); }
.sd-action-btn--danger:hover:not(:disabled) { background: var(--ds-danger); color: #fff; border-color: var(--ds-danger); box-shadow: var(--ds-shadow-sm); }
.sd-action-btn--resume { background: var(--ds-success-bg); color: var(--ds-success); border-color: var(--ds-success-soft); }
.sd-action-btn--resume:hover:not(:disabled) { background: var(--ds-success); color: #fff; border-color: var(--ds-success); box-shadow: var(--ds-shadow-sm); }
.sd-action-btn--outline { background: var(--ds-surface); color: var(--ds-text-secondary); border-color: var(--ds-border); }
.sd-action-btn--outline:hover:not(:disabled) { background: var(--ds-surface-muted); color: var(--ds-text); border-color: var(--ds-primary-border); }
.sd-action-sep { flex: 1; border-top: 1px dashed var(--ds-border); margin: 0 0.5rem; align-self: stretch; }
.sd-action-meta { display: flex; gap: 1rem; align-items: center; margin-left: auto; }
.sd-action-meta__item {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ds-text-muted);
}
.sd-action-meta__item--success { color: var(--ds-success); }
.sd-action-meta__item--warn { color: var(--ds-warning); }

/* ── Dialog ────────────────────────────────────────────────────── */
.sd-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(15,23,42,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}
.sd-dialog {
  width: 100%;
  max-width: 460px;
  margin: 1rem;
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  box-shadow: var(--ds-shadow-xl);
  overflow: hidden;
}
.sd-dialog__head {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 1.25rem 1.5rem 1rem;
  border-bottom: 1px solid var(--ds-border);
  color: var(--ds-warning);
}
.sd-dialog__head--danger { color: var(--ds-danger); }
.sd-dialog__head--success { color: var(--ds-success); }
.sd-dialog__title { font-size: 1rem; font-weight: 800; color: var(--ds-text); margin: 0; }
.sd-dialog__body { padding: 1.25rem 1.5rem; }
.sd-dialog__desc { font-size: 0.875rem; color: var(--ds-text-secondary); margin: 0 0 0.875rem; }
.sd-dialog__desc--danger { color: var(--ds-danger); }
.sd-dialog__textarea {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-md);
  background: var(--ds-surface);
  color: var(--ds-text);
  font-size: 0.85rem;
  resize: vertical;
  outline: none;
  transition: all 0.15s;
  font-family: inherit;
}
.sd-dialog__textarea:focus { border-color: var(--ds-primary); box-shadow: var(--ds-shadow-focus); }
.sd-dialog__textarea::placeholder { color: var(--ds-text-muted); }
.sd-dialog__foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.625rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--ds-border);
  background: var(--ds-surface-muted);
}

/* ── Buttons ────────────────────────────────────────────────────── */
.sd-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  padding: 0.55rem 1.125rem;
  border-radius: var(--ds-radius-md);
  font-size: 0.825rem;
  font-weight: 700;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.15s;
}
.sd-btn--secondary { background: var(--ds-surface); color: var(--ds-text-secondary); border-color: var(--ds-border); }
.sd-btn--secondary:hover { background: var(--ds-surface-muted); color: var(--ds-text); border-color: var(--ds-primary-border); }
.sd-btn--warn { background: var(--ds-warning); color: #fff; }
.sd-btn--warn:hover { filter: brightness(1.05); box-shadow: var(--ds-shadow-sm); }
.sd-btn--danger { background: var(--ds-danger); color: #fff; }
.sd-btn--danger:hover { filter: brightness(1.05); box-shadow: var(--ds-shadow-sm); }
.sd-btn--success { background: var(--ds-success); color: #fff; }
.sd-btn--success:hover { filter: brightness(1.05); box-shadow: var(--ds-shadow-sm); }
.sd-btn--ghost { background: transparent; color: var(--ds-text-secondary); border-color: var(--ds-border); }
.sd-btn--ghost:hover { background: var(--ds-surface-muted); color: var(--ds-text); }

/* ── Responsive ─────────────────────────────────────────────────── */
@media (max-width: 1024px) {
  .sd-grid { grid-template-columns: 1fr; }
  .sd-stats-strip { grid-template-columns: repeat(3, 1fr); }
  .sd-hero { flex-direction: column; align-items: flex-start; }
  .sd-hero__right { width: 100%; }
}
@media (max-width: 640px) {
  .sd-stats-strip { grid-template-columns: repeat(2, 1fr); margin: 1rem 1rem 0; }
  .sd-hero { margin: 1rem 1rem 0; padding: 1rem; }
  .sd-grid { margin: 1rem 1rem 0; }
  .sd-action-bar { padding: 1rem 1rem 1.5rem; }
  .sd-topbar { padding: 0.5rem 1rem; flex-wrap: wrap; }
  .sd-topbar__refresh span { display: none; }
}
</style>
