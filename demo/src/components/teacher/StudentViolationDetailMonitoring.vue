<template>
  <div class="mon-root">

    <!-- Loading -->
    <div v-if="loading" class="mon-loading">
      <div class="mon-loading__spinner" />
      <span>Đang tải dữ liệu...</span>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="mon-error">
      <LucideIcon name="alert-circle" :size="28" />
      <p>{{ error }}</p>
      <button class="mon-error__retry" @click="loadData()">Thử lại</button>
    </div>

    <template v-else>

      <!-- ── Page Header ─────────────────────────────────────────── -->
      <header class="mon-header">
        <div class="mon-header__left">
          <RouterLink :to="sessionLink" class="mon-icon-btn">
            <LucideIcon name="arrow-left" :size="16" />
          </RouterLink>
          <div class="smd-header__exam-info">
            <span class="smd-header__exam-name">{{ selectedExamTitle }}</span>
            <span class="smd-header__exam-code">{{ selectedExamCode }}</span>
          </div>
        </div>

        <div class="mon-header__center">
          <!-- Student avatar + name -->
          <div class="smd-header__student">
            <div class="smd-avatar" :style="{ background: riskBg }">
              <span :style="{ color: riskColor }">{{ studentInitials }}</span>
              <div class="smd-avatar__ring" :style="{ borderColor: statusColor }" />
            </div>
            <div class="smd-header__student-info">
              <h1 class="smd-header__student-name">{{ studentName }}</h1>
              <div class="smd-header__student-meta">
                <span v-if="studentCode" class="smd-meta-tag">
                  <LucideIcon name="hash" :size="11" />{{ studentCode }}
                </span>
                <span class="smd-meta-tag">
                  <LucideIcon name="clock" :size="11" />{{ sessionTime }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div class="mon-header__right">
          <!-- Risk gauge -->
          <div class="smd-gauge">
            <svg class="smd-gauge__svg" viewBox="0 0 100 100">
              <circle cx="50" cy="50" r="44" class="smd-gauge__track" />
              <circle cx="50" cy="50" r="44" class="smd-gauge__fill"
                :stroke="riskColor"
                :stroke-dasharray="gaugeArc" />
            </svg>
            <div class="smd-gauge__center">
              <span class="smd-gauge__score" :style="{ color: riskColor }">{{ riskScore }}</span>
              <span class="smd-gauge__label">điểm</span>
            </div>
          </div>

          <!-- Badges -->
          <div class="smd-badges">
            <span class="mon-badge" :style="{ color: riskColor, background: riskBg, borderColor: riskBg }">
              <LucideIcon name="shield-alert" :size="11" />
              {{ riskLevelLabel }}
            </span>
            <span class="mon-badge mon-badge--status" :style="{ color: statusColor }">
              {{ statusLabel }}
            </span>
          </div>
        </div>
      </header>

      <!-- ── Recommendation Banner ──────────────────────────────── -->
      <div v-if="riskData.reviewRequired || riskData.reasons?.length" class="mon-rec-card">
        <div class="mon-rec-card__icon">
          <LucideIcon name="lightbulb" :size="16" />
        </div>
        <div class="mon-rec-card__body">
          <span class="mon-rec-card__action" :style="{ color: riskColor }">
            {{ recommendedActionLabel }}
          </span>
          <div v-if="riskData.reasons?.length" class="mon-rec-card__reasons">
            <span v-for="reason in riskData.reasons.slice(0, 4)" :key="reason" class="mon-reason-tag">{{ reason }}</span>
          </div>
        </div>
      </div>

      <!-- ── Tabs ─────────────────────────────────────────────── -->
      <div class="smd-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          class="smd-tab"
          :class="{ 'smd-tab--active': activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          <LucideIcon :name="tab.icon" :size="14" />
          {{ tab.label }}
          <span v-if="tab.count !== undefined" class="smd-tab__count">{{ tab.count }}</span>
        </button>
      </div>

      <!-- ── Content ─────────────────────────────────────────── -->
      <div class="smd-content">

        <!-- Tab: Overview -->
        <div v-if="activeTab === 'overview'" class="smd-tab-pane">
          <div class="smd-overview-grid">

            <!-- Risk breakdown -->
            <div class="smd-panel">
              <div class="smd-panel__header">
                <LucideIcon name="bar-chart-2" :size="14" />
                <span>Phân tích điểm rủi ro</span>
              </div>
              <div class="smd-panel__body">
                <div v-if="!hasBreakdown" class="smd-empty">Không có dữ liệu phân tích</div>
                <div v-else class="smd-breakdown-list">
                  <div v-for="(score, key) in riskData.breakdown" :key="key" class="smd-breakdown-row">
                    <span class="smd-breakdown-row__label">{{ getVLabel(key) }}</span>
                    <div class="smd-breakdown-row__bar">
                      <div class="smd-breakdown-row__fill" :style="{ width: Math.min(score, 100) + '%', background: sColor(score) }" />
                    </div>
                    <span class="smd-breakdown-row__score" :style="{ color: sColor(score) }">{{ score }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Progress -->
            <div class="smd-panel">
              <div class="smd-panel__header">
                <LucideIcon name="list-checks" :size="14" />
                <span>Tiến độ bài thi</span>
              </div>
              <div class="smd-panel__body">
                <div class="smd-progress-block">
                  <span class="smd-progress-block__val">{{ attemptData.answeredCount || 0 }}/{{ attemptData.totalQuestions || 0 }}</span>
                  <span class="smd-progress-block__label">câu đã trả lời</span>
                </div>
                <div class="smd-progress-bar">
                  <div class="smd-progress-fill" :style="{ width: progressPercent + '%', background: riskColor }" />
                </div>
                <div class="smd-progress-row">
                  <span>{{ progressPercent }}% hoàn thành</span>
                  <span v-if="attemptData.remainingSeconds" class="smd-remaining">
                    <LucideIcon name="clock" :size="12" />
                    {{ formatRemaining(attemptData.remainingSeconds) }}
                  </span>
                </div>
              </div>
            </div>

            <!-- Device info -->
            <div class="smd-panel">
              <div class="smd-panel__header">
                <LucideIcon name="monitor" :size="14" />
                <span>Thông tin thiết bị</span>
              </div>
              <div class="smd-panel__body smd-panel__body--tight">
                <div class="smd-device-row">
                  <LucideIcon name="laptop" :size="13" />
                  <span class="smd-device-row__label">Thiết bị</span>
                  <span class="smd-device-row__val">{{ deviceInfo.device || '—' }}</span>
                </div>
                <div class="smd-device-row">
                  <LucideIcon name="globe" :size="13" />
                  <span class="smd-device-row__label">Trình duyệt</span>
                  <span class="smd-device-row__val">{{ deviceInfo.browser || '—' }}</span>
                </div>
                <div class="smd-device-row">
                  <LucideIcon name="cpu" :size="13" />
                  <span class="smd-device-row__label">Hệ điều hành</span>
                  <span class="smd-device-row__val">{{ deviceInfo.os || '—' }}</span>
                </div>
                <div class="smd-device-row">
                  <LucideIcon name="map-pin" :size="13" />
                  <span class="smd-device-row__label">Địa chỉ IP</span>
                  <span class="smd-device-row__val">{{ attemptData.clientIp || '—' }}</span>
                </div>
                <div class="smd-device-row">
                  <LucideIcon name="fingerprint" :size="13" />
                  <span class="smd-device-row__label">Fingerprint</span>
                  <span class="smd-device-row__val">{{ attemptData.deviceFingerprint || '—' }}</span>
                </div>
              </div>
            </div>

            <!-- Suspicious patterns -->
            <div v-if="suspiciousPatterns.length > 0" class="smd-panel smd-panel--warn">
              <div class="smd-panel__header">
                <LucideIcon name="brain" :size="14" />
                <span>Mẫu hành vi đáng ngờ</span>
                <span class="smd-panel__badge smd-panel__badge--warn">{{ suspiciousPatterns.length }}</span>
              </div>
              <div class="smd-panel__body">
                <div v-for="p in suspiciousPatterns" :key="p.id" class="smd-pattern">
                  <div class="smd-pattern__head">
                    <LucideIcon name="alert-triangle" :size="12" :style="{ color: pColor(p.level) }" />
                    <span class="smd-pattern__title">{{ p.title }}</span>
                    <span class="smd-pattern__level" :style="{ background: pColor(p.level) }">{{ p.level }}</span>
                  </div>
                  <p class="smd-pattern__desc">{{ p.description }}</p>
                </div>
              </div>
            </div>

            <!-- Quick actions -->
            <div class="smd-panel">
              <div class="smd-panel__header">
                <LucideIcon name="settings" :size="14" />
                <span>Điều khiển nhanh</span>
              </div>
              <div class="smd-panel__body smd-panel__body--controls">
                <button class="mon-action-btn mon-action-btn--warn" @click="showWarningDialog = true">
                  <LucideIcon name="alert-triangle" :size="14" />
                  Gửi cảnh báo
                </button>
                <button v-if="isStudentPaused" class="mon-action-btn mon-action-btn--success" @click="showResumeDialog = true">
                  <LucideIcon name="play" :size="14" />
                  Cho phép tiếp tục
                </button>
                <button v-else class="mon-action-btn mon-action-btn--pause" :disabled="isStudentTerminal" @click="showPauseDialog = true">
                  <LucideIcon name="pause" :size="14" />
                  Tạm dừng thi
                </button>
                <button class="mon-action-btn mon-action-btn--danger" :disabled="isStudentTerminal" @click="showStopDialog = true">
                  <LucideIcon name="x-circle" :size="14" />
                  Buộc nộp bài
                </button>
              </div>
            </div>

          </div>
        </div>

        <!-- Tab: Timeline -->
        <div v-if="activeTab === 'timeline'" class="smd-tab-pane">
          <div class="smd-panel">
            <div class="smd-panel__header">
              <LucideIcon name="activity" :size="14" />
              <span>Dòng thời gian vi phạm</span>
              <span class="smd-panel__badge">{{ filteredTimeline.length }}</span>
              <select v-model="timelineFilter" class="smd-panel__filter">
                <option value="">Tất cả loại</option>
                <option value="TAB_SWITCH">Chuyển tab</option>
                <option value="COPY_PASTE">Sao chép</option>
                <option value="EXIT_FULLSCREEN">Thoát toàn màn hình</option>
                <option value="DEVTOOLS">DevTools</option>
                <option value="DUPLICATE_IP">IP trùng</option>
                <option value="WARNING_SENT">Cảnh báo</option>
                <option value="NOTE">Ghi chú</option>
              </select>
            </div>
            <div class="smd-panel__body">
              <div v-if="filteredTimeline.length === 0" class="smd-empty">
                <LucideIcon name="check-circle" :size="24" />
                <p>Không có vi phạm nào</p>
              </div>
              <div v-else class="smd-timeline">
                <div v-for="(evt, idx) in filteredTimeline" :key="evt.key" class="smd-tl-item">
                  <div class="smd-tl-item__line" v-if="idx < filteredTimeline.length - 1" />
                  <div class="smd-tl-item__dot"
                    :style="{ background: getVColor(evt.eventType), boxShadow: `0 0 0 4px ${getVColor(evt.eventType)}22` }">
                    <LucideIcon :name="getVIcon(evt.eventType)" :size="10" style="color: white" />
                  </div>
                  <div class="smd-tl-item__content">
                    <div class="smd-tl-item__row">
                      <span class="smd-tl-item__type" :style="{ color: getVColor(evt.eventType) }">
                        {{ getVLabel(evt.eventType) }}
                      </span>
                      <span class="smd-tl-item__time">{{ formatTime(evt.at) }}</span>
                    </div>
                    <p v-if="evt.details" class="smd-tl-item__details">{{ evt.details }}</p>
                    <span class="smd-tl-item__sev" :class="`smd-tl-item__sev--${getSeverityStatus(evt.severity)}`">
                      {{ getSeverityLabel(evt.severity) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Realtime events -->
          <div class="smd-panel">
            <div class="smd-panel__header">
              <LucideIcon name="zap" :size="14" />
              <span>Sự kiện realtime</span>
              <div class="smd-live-dot" :class="isConnected ? 'smd-live-dot--on' : 'smd-live-dot--off'" />
              <span v-if="realtimeFeed.length" class="smd-panel__badge">{{ realtimeFeed.length }}</span>
            </div>
            <div class="smd-panel__body">
              <div v-if="realtimeFeed.length === 0" class="smd-empty">
                <LucideIcon name="inbox" :size="22" />
                <p>Chưa có sự kiện realtime</p>
              </div>
              <div v-else class="smd-realtime-list">
                <div v-for="evt in realtimeFeed" :key="evt.id" class="smd-realtime-item" :class="`smd-realtime-item--${evt.tone}`">
                  <div class="smd-realtime-item__icon">
                    <LucideIcon :name="evt.icon" :size="13" />
                  </div>
                  <div class="smd-realtime-item__body">
                    <span class="smd-realtime-item__label">{{ evt.label }}</span>
                    <p v-if="evt.message" class="smd-realtime-item__msg">{{ evt.message }}</p>
                  </div>
                  <span class="smd-realtime-item__time">{{ formatTime(evt.timestamp) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Tab: Actions -->
        <div v-if="activeTab === 'actions'" class="smd-tab-pane">
          <div class="smd-panel">
            <div class="smd-panel__header">
              <LucideIcon name="list" :size="14" />
              <span>Hành động đã thực hiện</span>
            </div>
            <div class="smd-panel__body">
              <div v-if="actionHistory.length === 0" class="smd-empty">Chưa có hành động nào</div>
              <div v-else class="smd-action-list">
                <div v-for="a in actionHistory" :key="a.key" class="smd-action-item">
                  <div class="smd-action-item__icon" :class="`smd-action-item__icon--${a.tone}`">
                    <LucideIcon :name="a.icon" :size="13" />
                  </div>
                  <div class="smd-action-item__body">
                    <span class="smd-action-item__label">{{ a.label }}</span>
                    <p v-if="a.message" class="smd-action-item__msg">{{ a.message }}</p>
                  </div>
                  <span class="smd-action-item__time">{{ formatTime(a.at || a.timestamp) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Tab: Notes -->
        <div v-if="activeTab === 'notes'" class="smd-tab-pane">
          <div class="smd-panel">
            <div class="smd-panel__header">
              <LucideIcon name="sticky-note" :size="14" />
              <span>Thêm ghi chú</span>
            </div>
            <div class="smd-panel__body">
              <textarea v-model="newNote" class="mon-note-input"
                rows="3" placeholder="Nhập ghi chú về hành vi của học sinh..." />
              <button class="mon-note-submit" :disabled="!newNote.trim() || noteLoading" @click="addNote">
                <LucideIcon name="plus" :size="14" />
                Thêm ghi chú
              </button>
            </div>
          </div>

          <div class="smd-panel">
            <div class="smd-panel__header">
              <LucideIcon name="list" :size="14" />
              <span>Lịch sử ghi chú</span>
              <span class="smd-panel__badge">{{ notes.length }}</span>
            </div>
            <div class="smd-panel__body">
              <div v-if="notes.length === 0" class="smd-empty">Chưa có ghi chú nào</div>
              <div v-else class="smd-notes-list">
                <div v-for="note in notes" :key="note.key" class="smd-note-item">
                  <div class="smd-note-item__body">{{ note.details || note.note || note.content }}</div>
                  <div class="smd-note-item__meta">
                    <span class="smd-note-item__author">Giám thị</span>
                    <span class="smd-note-item__time">{{ formatTime(note.at || note.timestamp) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </template>

    <!-- ── Dialogs ─────────────────────────────────────────────── -->
    <Teleport to="body">
      <!-- Warning -->
      <div v-if="showWarningDialog" class="mon-overlay" @click.self="showWarningDialog = false">
        <div class="mon-dialog">
          <div class="mon-dialog__head mon-dialog__head--warn">
            <LucideIcon name="alert-triangle" :size="18" />
            <h3>Gửi cảnh báo tới {{ studentName }}</h3>
          </div>
          <div class="mon-dialog__body">
            <p>Gửi cảnh báo tới <strong>{{ studentName }}</strong></p>
            <textarea v-model="warningMessage" class="mon-dialog__textarea" rows="3"
              placeholder="Nội dung cảnh báo (để trống = mặc định)" />
          </div>
          <div class="mon-dialog__foot">
            <button class="mon-dialog__cancel" @click="showWarningDialog = false">Hủy</button>
            <button class="mon-dialog__confirm mon-dialog__confirm--warn"
              :disabled="actionLoading === 'warning'" @click="confirmSendWarning">
              {{ actionLoading === 'warning' ? 'Đang gửi...' : 'Gửi cảnh báo' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Pause -->
      <div v-if="showPauseDialog" class="mon-overlay" @click.self="showPauseDialog = false">
        <div class="mon-dialog">
          <div class="mon-dialog__head mon-dialog__head--pause">
            <LucideIcon name="pause-circle" :size="18" />
            <h3>Tạm dừng thi</h3>
          </div>
          <div class="mon-dialog__body">
            <p>Tạm dừng bài thi của <strong>{{ studentName }}</strong>? Học sinh sẽ không thể tiếp tục cho đến khi được cho phép.</p>
            <textarea v-model="pauseReason" class="mon-dialog__textarea" rows="2" placeholder="Lý do (tùy chọn)" />
          </div>
          <div class="mon-dialog__foot">
            <button class="mon-dialog__cancel" @click="showPauseDialog = false">Hủy</button>
            <button class="mon-dialog__confirm mon-dialog__confirm--pause"
              :disabled="actionLoading === 'pause'" @click="confirmPauseAction">
              {{ actionLoading === 'pause' ? 'Đang xử lý...' : 'Xác nhận' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Resume -->
      <div v-if="showResumeDialog" class="mon-overlay" @click.self="showResumeDialog = false">
        <div class="mon-dialog">
          <div class="mon-dialog__head mon-dialog__head--success">
            <LucideIcon name="play-circle" :size="18" />
            <h3>Cho phép tiếp tục thi</h3>
          </div>
          <div class="mon-dialog__body">
            <p>Học sinh <strong>{{ studentName }}</strong> sẽ được khôi phục bài thi.</p>
            <textarea v-model="resumeMessage" class="mon-dialog__textarea" rows="2" placeholder="Lời nhắn (tùy chọn)" />
          </div>
          <div class="mon-dialog__foot">
            <button class="mon-dialog__cancel" @click="showResumeDialog = false">Hủy</button>
            <button class="mon-dialog__confirm mon-dialog__confirm--success"
              :disabled="actionLoading === 'resume'" @click="confirmResumeAction">
              {{ actionLoading === 'resume' ? 'Đang xử lý...' : 'Cho phép tiếp tục' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Stop -->
      <div v-if="showStopDialog" class="mon-overlay" @click.self="showStopDialog = false">
        <div class="mon-dialog">
          <div class="mon-dialog__head mon-dialog__head--danger">
            <LucideIcon name="shield-alert" :size="18" />
            <h3>Xác nhận dừng thi</h3>
          </div>
          <div class="mon-dialog__body">
            <p>Hành động này sẽ <strong>dừng bài thi</strong> của {{ studentName }}. Không thể hoàn tác.</p>
            <textarea v-model="stopReason" class="mon-dialog__textarea" rows="2"
              placeholder="Lý do dừng thi (để trống = mặc định)" />
          </div>
          <div class="mon-dialog__foot">
            <button class="mon-dialog__cancel" @click="showStopDialog = false">Hủy</button>
            <button class="mon-dialog__confirm mon-dialog__confirm--danger"
              :disabled="actionLoading === 'invalidate'" @click="confirmStop">
              {{ actionLoading === 'invalidate' ? 'Đang xử lý...' : 'Xác nhận dừng thi' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useToast } from '../../composables/useToast'
import { useExamMonitoring } from '../../composables/useExamMonitoring'
import {
  fetchAttemptDetail,
  fetchAttemptRisk,
  addMonitoringNote,
  sendTeacherWarning,
  pauseAttempt,
  resumeAttempt,
  invalidateAttempt
} from '../../services/examMonitoringService'
import { isAttemptPaused, isAttemptTerminal } from '../../utils/proctorStatusMeta'
import { normalizeSignalType as normalizeProctorSignalType } from '../../utils/proctorSignalTypes'

const RISK_BAND_THRESHOLDS = { CRITICAL: 81, HIGH_RISK: 61, SUSPICIOUS: 31 }
const RISK_LEVEL_LABELS = { CRITICAL: 'Nguy cơ cao', HIGH_RISK: 'Rủi ro cao', SUSPICIOUS: 'Đáng ngờ', CLEAN: 'Bình thường' }
const RECOMMENDED_ACTION_LABELS = {
  PAUSE_AND_REVIEW: 'Tạm dừng và kiểm tra ngay',
  WARN_AND_ESCALATE: 'Cảnh báo và tăng cường giám sát',
  REVIEW_ATTEMPT: 'Mở hồ sơ để review thủ công',
  CONTINUE_MONITORING: 'Tiếp tục giám sát'
}
const SEVERITY_LABELS = { HIGH: 'Nghiêm trọng', MEDIUM: 'Trung bình', LOW: 'Thấp', CRITICAL: 'Nghiêm trọng' }
const V_COLORS = {
  // CRITICAL types — red
  COPY_PASTE: 'var(--mon-risk-critical)', DEVTOOLS_OPEN: 'var(--mon-risk-critical)',
  MULTI_MONITOR: 'var(--mon-risk-critical)', DUPLICATE_IP: 'var(--mon-risk-critical)',
  IP_CHANGED: 'var(--mon-risk-critical)',
  PRINT_SCREEN: 'var(--mon-risk-critical)', DEVICE_FINGERPRINT_CHANGED: 'var(--mon-risk-critical)',
  SYNC_BEHAVIOR: 'var(--mon-risk-critical)', IP_FINGERPRINT_GRAPH: 'var(--mon-risk-critical)',
  ANSWER_SIMILARITY: 'var(--mon-risk-critical)', AI_MULTIPLE_FACES: 'var(--mon-risk-critical)',
  AI_PHONE_DETECTED: 'var(--mon-risk-critical)',
  AI_SPEAKING_DETECTED: 'var(--mon-risk-high)',
  NO_MIC: 'var(--mon-risk-high)',
  MULTIPLE_FACES: 'var(--mon-risk-critical)', FACE_SPOOFING_SUSPECTED: 'var(--mon-risk-critical)',
  // HIGH / WARNING types — orange/amber
  TAB_SWITCH: 'var(--mon-risk-high)', EXIT_FULLSCREEN: 'var(--mon-risk-high)',
  WINDOW_BLUR: 'var(--mon-risk-high)', HEARTBEAT_STALE: 'var(--mon-risk-high)',
  RAPID_QUESTION_SWITCH: 'var(--mon-risk-high)', FACE_NOT_DETECTED: 'var(--mon-risk-high)',
  NO_CAMERA: 'var(--mon-risk-high)',
  SCREEN_LEAVE: 'var(--mon-risk-high)', FULLSCREEN_VIOLATION: 'var(--mon-risk-high)',
  FULLSCREEN_EVASION: 'var(--mon-risk-high)', LONG_SCREEN_LEAVE: 'var(--mon-risk-high)',
  FACE_OBSTRUCTED_MASK: 'var(--mon-risk-high)', EYES_OBSTRUCTED: 'var(--mon-risk-high)',
  PARTIAL_FACE_VISIBLE: 'var(--mon-risk-high)', FACE_TOO_FAR: 'var(--mon-risk-high)',
  FACE_TURNED_AWAY: 'var(--mon-risk-high)', EYES_NOT_DETECTED: 'var(--mon-risk-high)',
  VERY_LOW_LIGHTING: 'var(--mon-risk-high)', VERY_BLURRY_FRAME: 'var(--mon-risk-high)',
  GAZE_OFF_SCREEN: 'var(--mon-risk-high)',
  // LOW / INFO types — sky blue
  IDLE_TIME: 'var(--mon-risk-low)', RIGHT_CLICK: 'var(--mon-risk-low)',
  AI_LOOKING_AWAY: 'var(--mon-risk-low)', FACE_TOO_CLOSE: 'var(--mon-risk-low)',
  FACE_NOT_CENTERED: 'var(--mon-risk-low)', LOW_LIGHTING: 'var(--mon-risk-low)',
  OVEREXPOSED_FRAME: 'var(--mon-risk-low)', BLURRY_FRAME: 'var(--mon-risk-low)',
  EYE_BLINK_ANOMALY: 'var(--mon-risk-low)', EYES_CLOSED_PROLONGED: 'var(--mon-risk-low)',
  RAPID_EYE_MOVEMENT: 'var(--mon-risk-low)',
  // NOTE / WARNING_SENT
  WARNING_SENT: 'var(--mon-primary)',
  NOTE: 'var(--mon-text-muted)'
}
const V_ICONS = {
  TAB_SWITCH: 'layers', WINDOW_BLUR: 'layers', IDLE_TIME: 'clock', RIGHT_CLICK: 'mouse-pointer-2',
  EXIT_FULLSCREEN: 'minimize', COPY_PASTE: 'copy', DEVTOOLS_OPEN: 'code', PRINT_SCREEN: 'code',
  MULTI_MONITOR: 'monitor', DUPLICATE_IP: 'globe', IP_CHANGED: 'globe', RAPID_QUESTION_SWITCH: 'monitor', HEARTBEAT_STALE: 'wifi-off',
  DEVICE_FINGERPRINT_CHANGED: 'code', SYNC_BEHAVIOR: 'monitor', IP_FINGERPRINT_GRAPH: 'globe',
  ANSWER_SIMILARITY: 'copy', AI_MULTIPLE_FACES: 'monitor', AI_PHONE_DETECTED: 'monitor', AI_SPEAKING_DETECTED: 'mic', AI_LOOKING_AWAY: 'wifi-off',
  NO_CAMERA: 'videocam_off', NO_MIC: 'mic_off',
  SCREEN_LEAVE: 'monitor-off', FULLSCREEN_VIOLATION: 'minimize', FULLSCREEN_EVASION: 'minimize',
  LONG_SCREEN_LEAVE: 'monitor-off',
  MULTIPLE_FACES: 'monitor', FACE_SPOOFING_SUSPECTED: 'shield-alert', FACE_NOT_DETECTED: 'user-x',
  FACE_OBSTRUCTED_MASK: 'shield-alert', EYES_OBSTRUCTED: 'eye-off', PARTIAL_FACE_VISIBLE: 'monitor',
  FACE_TOO_FAR: 'search', FACE_TOO_CLOSE: 'search', FACE_TURNED_AWAY: 'rotate-ccw',
  FACE_NOT_CENTERED: 'monitor', EYES_NOT_DETECTED: 'eye-off', VERY_LOW_LIGHTING: 'wb_sunny',
  LOW_LIGHTING: 'wb_sunny', OVEREXPOSED_FRAME: 'wb_sunny', VERY_BLURRY_FRAME: 'image-off',
  BLURRY_FRAME: 'image-off', EYE_BLINK_ANOMALY: 'eye', EYES_CLOSED_PROLONGED: 'eye-off',
  GAZE_OFF_SCREEN: 'eye-off', RAPID_EYE_MOVEMENT: 'activity',
  WARNING_SENT: 'alert-triangle', NOTE: 'sticky-note'
}
const V_LABELS = {
  TAB_SWITCH: 'Chuyển tab', WINDOW_BLUR: 'Mất tiêu điểm', IDLE_TIME: 'Không hoạt động',
  RIGHT_CLICK: 'Click chuột phải', EXIT_FULLSCREEN: 'Thoát toàn màn hình', COPY_PASTE: 'Sao chép nội dung',
  DEVTOOLS_OPEN: 'Mở DevTools', PRINT_SCREEN: 'Chụp màn hình', MULTI_MONITOR: 'Nhiều màn hình',
  DUPLICATE_IP: 'IP trùng lặp', IP_CHANGED: 'IP thay đổi', RAPID_QUESTION_SWITCH: 'Chuyển câu nhanh', HEARTBEAT_STALE: 'Mất kết nối',
  DEVICE_FINGERPRINT_CHANGED: 'Thay đổi thiết bị', SYNC_BEHAVIOR: 'Hành vi đồng bộ',
  IP_FINGERPRINT_GRAPH: 'Liên kết IP', ANSWER_SIMILARITY: 'Tương đồng đáp án',
  AI_MULTIPLE_FACES: 'Nhiều khuôn mặt', AI_PHONE_DETECTED: 'Phát hiện điện thoại', AI_SPEAKING_DETECTED: 'Tiếng ồn', AI_LOOKING_AWAY: 'Nhìn lệch hướng',
  NO_CAMERA: 'Camera tắt', NO_MIC: 'Micro tắt',
  SCREEN_LEAVE: 'Rời màn hình',
  FULLSCREEN_VIOLATION: 'Thoát toàn màn hình',
  FULLSCREEN_EVASION: 'Thoát toàn màn hình',
  LONG_SCREEN_LEAVE: 'Rời màn hình lâu',
  MULTIPLE_FACES: 'Nhiều khuôn mặt', FACE_SPOOFING_SUSPECTED: 'Nghi vấn giả mạo khuôn mặt',
  FACE_NOT_DETECTED: 'Không phát hiện khuôn mặt', FACE_OBSTRUCTED_MASK: 'Khuôn mặt bị che', EYES_OBSTRUCTED: 'Mắt bị che',
  PARTIAL_FACE_VISIBLE: 'Khuôn mặt không đầy đủ', FACE_TOO_FAR: 'Khuôn mặt quá xa', FACE_TOO_CLOSE: 'Khuôn mặt quá gần',
  FACE_TURNED_AWAY: 'Quay mặt đi', FACE_NOT_CENTERED: 'Khuôn mặt lệch tâm', EYES_NOT_DETECTED: 'Không phát hiện mắt',
  VERY_LOW_LIGHTING: 'Ánh sáng rất yếu', LOW_LIGHTING: 'Ánh sáng yếu', OVEREXPOSED_FRAME: 'Ảnh quá sáng',
  VERY_BLURRY_FRAME: 'Ảnh rất mờ', BLURRY_FRAME: 'Ảnh mờ', EYE_BLINK_ANOMALY: 'Nháy mắt bất thường',
  EYES_CLOSED_PROLONGED: 'Mắt nhắm lâu', GAZE_OFF_SCREEN: 'Nhìn lệch màn hình', RAPID_EYE_MOVEMENT: 'Mắt di chuyển nhanh',
  WARNING_SENT: 'Cảnh báo đã gửi', NOTE: 'Ghi chú'
}
const PATTERN_RULES = [
  { id: 'tab', key: 'TAB_SWITCH', threshold: 3, build: (n) => ({ title: 'Chuyển tab nhiều lần', description: `${n} lần chuyển tab (ngưỡng: 3)`, level: n > 5 ? 'high' : 'medium' }) },
  { id: 'copy', key: 'COPY_PASTE', threshold: 1, build: (n) => ({ title: 'Cố gắng copy nội dung', description: `${n} lần sao chép dữ liệu`, level: 'medium' }) },
  { id: 'dev', key: 'DEVTOOLS_OPEN', threshold: 1, build: (n) => ({ title: 'Mở công cụ phát triển', description: `${n} lần mở DevTools`, level: 'high' }) },
  { id: 'dup', key: 'DUPLICATE_IP', threshold: 1, build: (n) => ({ title: 'Phát hiện IP trùng lặp', description: `${n} thiết bị khác cùng IP`, level: 'high' }) }
]
const REALTIME_META = {
  RISK_UPDATED: { icon: 'trending-up', tone: 'warn', label: 'Cập nhật rủi ro' },
  SUSPICIOUS: { icon: 'alert-triangle', tone: 'warn', label: 'Hành vi đáng ngờ' },
  FRAUD_SIGNAL_RECORDED: { icon: 'activity', tone: 'warn', label: 'Tín hiệu giám sát' },
  FRAUD_WARNING_RECORDED: { icon: 'alert-triangle', tone: 'warn', label: 'Cảnh báo giám sát' },
  AI_SPEAKING_DETECTED: { icon: 'mic', tone: 'warn', label: 'Tiếng ồn' },
  NO_MIC: { icon: 'mic_off', tone: 'danger', label: 'Micro tắt' },
  WARNING_SENT: { icon: 'alert-circle', tone: 'warn', label: 'Đã gửi cảnh báo' },
  ATTEMPT_PAUSED: { icon: 'pause-circle', tone: 'warn', label: 'Tạm dừng' },
  ATTEMPT_RESUMED: { icon: 'play-circle', tone: 'success', label: 'Tiếp tục' },
  ATTEMPT_STOPPED: { icon: 'x-circle', tone: 'danger', label: 'Đình chỉ' }
}
const TIMELINE_HIDDEN_TYPES = new Set(['RISK_SCORE', 'RISK_UPDATED'])
const AI_CAMERA_TIMELINE_TYPES = new Set([
  'NO_CAMERA',
  'FACE_NOT_DETECTED',
  'MULTIPLE_FACES',
  'FACE_SPOOFING_SUSPECTED',
  'FACE_OBSTRUCTED_MASK',
  'EYES_OBSTRUCTED',
  'PARTIAL_FACE_VISIBLE',
  'FACE_TOO_FAR',
  'FACE_TOO_CLOSE',
  'FACE_TURNED_AWAY',
  'FACE_NOT_CENTERED',
  'EYES_NOT_DETECTED',
  'AI_SPEAKING_DETECTED',
  'NO_MIC',
  'VERY_LOW_LIGHTING',
  'LOW_LIGHTING',
  'OVEREXPOSED_FRAME',
  'VERY_BLURRY_FRAME',
  'BLURRY_FRAME',
  'EYE_BLINK_ANOMALY',
  'EYES_CLOSED_PROLONGED',
  'GAZE_OFF_SCREEN',
  'RAPID_EYE_MOVEMENT'
])
const TIMELINE_COLLAPSE_TYPES = new Set([
  'WARNING_SENT',
  'WARNING',
  'FRAUD_WARNING',
  'TAB_SWITCH',
  'WINDOW_BLUR',
  'SCREEN_LEAVE',
  'EXIT_FULLSCREEN',
  'LONG_SCREEN_LEAVE',
  ...AI_CAMERA_TIMELINE_TYPES
])

const route = useRoute()
const toast = useToast()
const { isConnected, subscribeToAttempt, unsubscribeFromAttempt } = useExamMonitoring()

const attemptId = computed(() => route.query.attemptId || '')
const examId = computed(() => route.query.examId || '')
const selectedExamTitle = computed(() => route.query.title || 'Kỳ thi')
const selectedExamCode = computed(() => route.query.code || '')
const sessionLink = computed(() => ({ path: `/teacher/exams/${examId.value}/monitoring` }))

const loading = ref(true)
const error = ref('')
const attemptData = ref({})
const riskData = ref({})
const timeline = ref([])
const realtimeFeed = ref([])
const activeTab = ref('overview')
const timelineFilter = ref('')
const newNote = ref('')
const noteLoading = ref(false)

const showWarningDialog = ref(false)
const showStopDialog = ref(false)
const showPauseDialog = ref(false)
const showResumeDialog = ref(false)
const warningMessage = ref('')
const stopReason = ref('')
const pauseReason = ref('')
const resumeMessage = ref('')
const actionLoading = ref('')
let refreshTimer = null

const tabs = computed(() => [
  { id: 'overview', label: 'Tổng quan', icon: 'layout-dashboard' },
  { id: 'timeline', label: 'Timeline', icon: 'activity', count: filteredTimeline.value.length },
  { id: 'actions', label: 'Hành động', icon: 'settings', count: actionHistory.value.length },
  { id: 'notes', label: 'Ghi chú', icon: 'sticky-note', count: notes.value.length }
])

// ── Student info ──────────────────────────────────────────────────────────────
const studentName = computed(() => {
  const s = riskData.value.student
  if (s) return s.name || s.username || '—'
  const a = attemptData.value
  if (a && a.student) return a.student
  return '—'
})

const studentCode = computed(() => {
  const s = riskData.value.student
  if (s) return s.studentCode || '—'
  return '—'
})

const studentInitials = computed(() => {
  const name = studentName.value
  if (!name || name === '—') return '?'
  const parts = name.trim().split(/\s+/)
  return (parts[parts.length - 1].charAt(0) + (parts.length > 1 ? parts[0].charAt(0) : '')).toUpperCase()
})

// ── Status ───────────────────────────────────────────────────────────────────
const attemptStatusToken = computed(() => {
  const s = String(riskData.value.status || attemptData.value.status || '').toUpperCase()
  if (/SUBMITTED/.test(s)) return 'SUBMITTED'
  if (/STOPPED/.test(s)) return 'STOPPED'
  if (/PAUSED/.test(s)) return 'PAUSED'
  if (/ACTIVE|IN_PROGRESS/.test(s)) return 'ACTIVE'
  return 'UNKNOWN'
})

const STATUS_META = {
  SUBMITTED: { label: 'Đã nộp', color: 'var(--mon-success)', bg: 'var(--mon-success-soft)' },
  STOPPED: { label: 'Đã dừng', color: 'var(--mon-danger)', bg: 'var(--mon-danger-soft)' },
  PAUSED: { label: 'Tạm dừng', color: 'var(--mon-warning)', bg: 'var(--mon-warning-soft)' },
  ACTIVE: { label: 'Đang thi', color: 'var(--mon-primary)', bg: 'var(--mon-primary-soft)' },
  UNKNOWN: { label: '—', color: 'var(--mon-text-muted)', bg: 'var(--mon-surface-2)' }
}
const statusMeta = computed(() => STATUS_META[attemptStatusToken.value] || STATUS_META.UNKNOWN)
const statusLabel = computed(() => statusMeta.value.label)
const statusColor = computed(() => statusMeta.value.color)
const isStudentPaused = computed(() => isAttemptPaused(riskData.value.status || attemptData.value.status))
const isStudentTerminal = computed(() => isAttemptTerminal(riskData.value.status || attemptData.value.status))

const sessionTime = computed(() => {
  const ts = attemptData.value.startedAt
  if (!ts) return '—'
  const diff = Math.floor((Date.now() - new Date(ts).getTime()) / 60000)
  if (diff < 1) return '<1 phút'
  if (diff < 60) return `${diff} phút`
  return `${Math.floor(diff / 60)}h ${diff % 60}p`
})

// ── Risk ─────────────────────────────────────────────────────────────────────
const riskScore = computed(() => Math.round(Number(riskData.value.score ?? attemptData.value.riskScore ?? 0)))
const riskBand = computed(() => {
  const s = riskScore.value
  if (s >= RISK_BAND_THRESHOLDS.CRITICAL) return 'CRITICAL'
  if (s >= RISK_BAND_THRESHOLDS.HIGH_RISK) return 'HIGH_RISK'
  if (s >= RISK_BAND_THRESHOLDS.SUSPICIOUS) return 'SUSPICIOUS'
  return 'CLEAN'
})
const riskLevelLabel = computed(() => RISK_LEVEL_LABELS[riskBand.value] || riskBand.value)
const riskColor = computed(() => {
  const s = riskScore.value
  if (s >= RISK_BAND_THRESHOLDS.HIGH_RISK) return 'var(--mon-danger)'
  if (s >= RISK_BAND_THRESHOLDS.SUSPICIOUS) return 'var(--mon-warning)'
  return 'var(--mon-success)'
})
const riskBg = computed(() => {
  const s = riskScore.value
  if (s >= RISK_BAND_THRESHOLDS.HIGH_RISK) return 'var(--mon-danger-soft)'
  if (s >= RISK_BAND_THRESHOLDS.SUSPICIOUS) return 'var(--mon-warning-soft)'
  return 'var(--mon-success-soft)'
})
const recommendedActionLabel = computed(() => RECOMMENDED_ACTION_LABELS[riskData.value.recommendedAction] || 'Tiếp tục giám sát')
const hasBreakdown = computed(() => { const b = riskData.value.breakdown; return b && Object.keys(b).length > 0 })

const gaugeArc = computed(() => {
  const circ = 2 * Math.PI * 44
  const ratio = Math.max(0, Math.min(100, riskScore.value)) / 100
  return `${ratio * circ} ${circ}`
})

const progressPercent = computed(() => {
  const total = attemptData.value.totalQuestions || 0
  if (!total) return 0
  return Math.round(((attemptData.value.answeredCount || 0) / total) * 100)
})

const deviceInfo = computed(() => ({
  device: attemptData.value.deviceFingerprint ? 'Desktop' : '—',
  browser: '—',
  os: '—'
}))

// ── Timeline ─────────────────────────────────────────────────────────────────
function extractDetails(raw) {
  if (!raw) return ''
  if (typeof raw === 'string') {
    const t = raw.trim()
    if (!t) return ''
    if (t.startsWith('{') || t.startsWith('[')) {
      try {
        const p = JSON.parse(t)
        return p.details || p.message || p.evidence || ''
      } catch { return raw }
    }
    return raw
  }
  if (typeof raw === 'object') return raw.details || raw.message || raw.evidence || ''
  return String(raw)
}

function normalizeTimelineType(value) {
  return normalizeProctorSignalType(value)
}

function normalizeTimelineText(value) {
  return String(value || '').trim()
}

function isCameraTimelineType(eventType = '') {
  return AI_CAMERA_TIMELINE_TYPES.has(eventType)
}

function isCameraTimelineCategory(category = '') {
  return ['AI_CAMERA', 'EYE_TRACKING', 'GAZE_TRACKING', 'AI_SPOOFING', 'CAMERA_PROCTORING']
    .includes(normalizeTimelineType(category))
}

function normalizeTimelineDetails(item = {}, eventType = '') {
  const raw = extractDetails(item.details || item.evidence || item.message || item.displayMessage || '')
  const text = normalizeTimelineText(raw)
  if (!text) return ''
  const label = normalizeTimelineText(getVLabel(eventType))
  if (text === label || text.toUpperCase() === eventType) return ''
  return text
}

function normalizeTimelineEvent(item = {}) {
  const eventType = normalizeTimelineType(item.eventType || item.type || item.warningType)
  if (!eventType || TIMELINE_HIDDEN_TYPES.has(eventType)) return null
  const at = item.at || item.createdAt || item.timestamp || item.issuedAt || null
  const severity = normalizeTimelineType(item.severity) || 'MEDIUM'
  const category = normalizeTimelineType(item.category) || ''
  const bucketSize = timelineBucketSize(eventType, category)
  const bucketAt = bucketSize > 0
    ? Math.floor(new Date(at || 0).getTime() / bucketSize) * bucketSize
    : new Date(at || 0).getTime()

  return {
    ...item,
    eventType,
    at,
    severity,
    details: normalizeTimelineDetails(item, eventType),
    category,
    _bucketAt: bucketAt
  }
}

function timelineBucketSize(eventType = '', category = '') {
  if (eventType === 'RISK_SCORE' || eventType === 'RISK_UPDATED') return 0
  if (TIMELINE_COLLAPSE_TYPES.has(eventType) || isCameraTimelineType(eventType) || isCameraTimelineCategory(category)) {
    return 15_000
  }
  return 1_000
}

function makeTimelineFingerprint(item = {}) {
  const eventType = normalizeTimelineType(item.eventType || item.type || item.warningType)
  const atBucket = item._bucketAt != null
    ? String(item._bucketAt)
    : String(item.at || item.createdAt || item.timestamp || item.issuedAt || '')
  const category = normalizeTimelineType(item.category) || ''
  const collapse = TIMELINE_COLLAPSE_TYPES.has(eventType) || isCameraTimelineType(eventType) || isCameraTimelineCategory(category)
  const severity = collapse ? '' : normalizeTimelineType(item.severity) || ''
  const details = collapse ? '' : normalizeTimelineText(item.details || item.message || item.displayMessage)
  const normalizedCategory = collapse ? 'CAMERA_PROCTORING' : category
  return [eventType, atBucket, normalizedCategory, severity, details].join('|')
}

function preferTimelineItem(candidate = {}, existing = {}) {
  if (!existing) return true
  const candidateType = (candidate.type || '').toUpperCase()
  const existingType = (existing.type || '').toUpperCase()
  if (candidateType === 'FRAUD_SIGNAL' && existingType !== 'FRAUD_SIGNAL') {
    return true
  }
  if (existingType === 'FRAUD_SIGNAL' && candidateType !== 'FRAUD_SIGNAL') return false
  if (candidate.details && !existing.details) return true
  if (candidate.riskImpact != null && existing.riskImpact == null) return true
  if (candidate.confidence != null && existing.confidence == null) return true
  const candidateText = normalizeTimelineText(candidate.details || candidate.message || candidate.displayMessage)
  const existingText = normalizeTimelineText(existing.details || existing.message || existing.displayMessage)
  return candidateText.length > existingText.length
}

const timelineEvents = computed(() => {
  const unique = new Map()
  for (const rawItem of timeline.value || []) {
    const item = normalizeTimelineEvent(rawItem)
    if (!item) continue
    const key = makeTimelineFingerprint(item)
    const existing = unique.get(key)
    if (!existing || preferTimelineItem(item, existing)) {
      unique.set(key, item)
    }
  }
  return [...unique.values()]
    .sort((a, b) => {
      const aTime = new Date(a.at || a.createdAt || a.timestamp || 0).getTime()
      const bTime = new Date(b.at || b.createdAt || b.timestamp || 0).getTime()
      return bTime - aTime
    })
    .map((item, index) => ({
      ...item,
      key: item.key || makeTimelineFingerprint(item) || `${item.eventType}-${index}`
    }))
})

const filteredTimeline = computed(() => {
  if (!timelineFilter.value) return timelineEvents.value
  const filter = normalizeTimelineType(timelineFilter.value)
  return timelineEvents.value.filter(e => normalizeTimelineType(e.eventType).includes(filter))
})

const eventStats = computed(() => {
  const c = { TAB_SWITCH: 0, COPY_PASTE: 0, DEVTOOLS_OPEN: 0, DUPLICATE_IP: 0 }
  for (const e of timelineEvents.value) {
    const t = normalizeTimelineType(e.eventType)
    if (t === 'TAB_SWITCH') c.TAB_SWITCH++
    if (t === 'COPY_PASTE') c.COPY_PASTE++
    if (t === 'DEVTOOLS_OPEN') c.DEVTOOLS_OPEN++
    if (t === 'DUPLICATE_IP') c.DUPLICATE_IP++
  }
  return c
})

const suspiciousPatterns = computed(() =>
  PATTERN_RULES.filter(r => eventStats.value[r.key] >= r.threshold)
    .map(r => ({ id: r.id, ...r.build(eventStats.value[r.key]) }))
)

// ── Notes & Actions ───────────────────────────────────────────────────────────
const notes = computed(() =>
  timelineEvents.value
    .filter(item => item.eventType === 'NOTE')
    .map(n => ({ ...n, key: n.key || `${n.eventType}-${n.at}` }))
)

const actionHistory = computed(() => {
  const actions = []
  for (const item of timelineEvents.value) {
    const type = String(item.eventType || item.type || '').toUpperCase()
    const isAction = ['WARNING_SENT', 'ATTEMPT_PAUSED', 'ATTEMPT_RESUMED', 'ATTEMPT_STOPPED', 'NOTE'].includes(type)
    if (!isAction) continue
    const TONE_MAP = {
      WARNING_SENT: 'warn', ATTEMPT_STOPPED: 'danger',
      ATTEMPT_RESUMED: 'success', NOTE: 'neutral'
    }
    const ICON_MAP = {
      WARNING_SENT: 'alert-triangle', ATTEMPT_PAUSED: 'pause',
      ATTEMPT_RESUMED: 'play', ATTEMPT_STOPPED: 'x-circle', NOTE: 'sticky-note'
    }
    const LABEL_MAP = {
      WARNING_SENT: 'Gửi cảnh báo', ATTEMPT_PAUSED: 'Tạm dừng',
      ATTEMPT_RESUMED: 'Cho phép tiếp tục', ATTEMPT_STOPPED: 'Buộc nộp', NOTE: 'Ghi chú'
    }
    actions.push({
      ...item,
      key: item.key || `${type}-${item.at}`,
      label: LABEL_MAP[type] || type,
      icon: ICON_MAP[type] || 'activity',
      tone: TONE_MAP[type] || 'neutral'
    })
  }
  return actions
})

// ── Helpers ───────────────────────────────────────────────────────────────────
const getVColor = (type) => V_COLORS[normalizeTimelineType(type)] || 'var(--mon-text-muted)'
const getVIcon = (type) => V_ICONS[normalizeTimelineType(type)] || 'alert-circle'
const getVLabel = (type) => {
  const signalType = normalizeTimelineType(type)
  return V_LABELS[signalType] || signalType || '—'
}
const getSeverityLabel = (s) => SEVERITY_LABELS[s] || s || '—'
const getSeverityStatus = (s) => s === 'HIGH' || s === 'CRITICAL' ? 'high' : s === 'MEDIUM' ? 'medium' : 'low'
const pColor = (l) => l === 'high' ? 'var(--mon-danger)' : l === 'medium' ? 'var(--mon-warning)' : 'var(--mon-info)'
const sColor = (score) => score >= 60 ? 'var(--mon-danger)' : score >= 30 ? 'var(--mon-warning)' : 'var(--mon-info)'

const formatTime = (ts) => {
  if (!ts) return '—'
  return new Date(ts).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

const formatRemaining = (seconds) => {
  if (!seconds) return '—'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  if (h > 0) return `${h}h ${m}p`
  return `${m} phút`
}

// ── Data loading ──────────────────────────────────────────────────────────────
async function loadData(silent = false) {
  if (!attemptId.value) { error.value = 'Thiếu mã bài làm.'; return }
  if (!silent) { loading.value = true; error.value = '' }
  try {
    const [detailRes, riskRes, timelineRes] = await Promise.allSettled([
      fetchAttemptDetail(attemptId.value),
      fetchAttemptRisk(attemptId.value),
      listMonitoringTimeline(attemptId.value)
    ])
    if (detailRes.status === 'fulfilled') attemptData.value = detailRes.value || {}
    if (riskRes.status === 'fulfilled') riskData.value = riskRes.value || {}
    if (timelineRes.status === 'fulfilled') timeline.value = timelineRes.value || []
  } catch (err) {
    error.value = err?.message || 'Không thể tải dữ liệu.'
    if (!silent) toast.error(error.value)
  } finally {
    loading.value = false
  }
}

function startAutoRefresh() {
  stopAutoRefresh()
  refreshTimer = setInterval(() => {
    if (!document.hidden && !loading.value) void loadData(true)
  }, 15000)
}

function stopAutoRefresh() {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
}

// ── Realtime ─────────────────────────────────────────────────────────────────
function pushRealtimeEvent(payload = {}) {
  const type = normalizeTimelineType(payload.type)
  if (!type || type === 'RISK_UPDATED') return
  const meta = REALTIME_META[type] || { icon: 'activity', tone: 'neutral', label: type || 'Sự kiện' }
  const timestamp = payload.timestamp || payload.issuedAt || new Date().toISOString()
  const logicalType = normalizeTimelineType(
    payload.warningType
      || payload.latestSignal?.signalType
      || payload.signalType
      || payload.eventType
      || type
  )
  const isFraudRealtime = type === 'FRAUD_SIGNAL_RECORDED' || type === 'FRAUD_WARNING_RECORDED'
  const message = payload.message || payload.latestSignal?.displayMessage || (isFraudRealtime ? getVLabel(logicalType) : '')
  const key = [
    isFraudRealtime ? logicalType : type,
    Math.floor(new Date(timestamp).getTime() / 5000),
    isFraudRealtime ? '' : normalizeTimelineText(message)
  ].join('|')
  if (realtimeFeed.value.some(evt => evt.id === key)) return
  realtimeFeed.value = [{
    id: key,
    type, icon: meta.icon, tone: meta.tone, label: meta.label,
    message,
    timestamp
  }, ...realtimeFeed.value].slice(0, 30)
}

function handleRealtimePayload(payload) {
  if (!payload || typeof payload !== 'object') return
  const type = String(payload.type || '').toUpperCase()
  if (type === 'RISK_UPDATED' || type === 'SUSPICIOUS') {
    riskData.value = { ...riskData.value, score: payload.riskScore ?? riskData.value.score, level: payload.riskLevel, reasons: payload.reasons || riskData.value.reasons }
    void loadData(true)
  }
  if (['ATTEMPT_PAUSED', 'ATTEMPT_RESUMED', 'ATTEMPT_STOPPED'].includes(type)) {
    riskData.value = { ...riskData.value, status: type === 'ATTEMPT_PAUSED' ? 'PAUSED' : type === 'ATTEMPT_STOPPED' ? 'STOPPED' : 'IN_PROGRESS' }
    void loadData(true)
  }
  pushRealtimeEvent(payload)
}

// ── Actions ───────────────────────────────────────────────────────────────────
async function runAction({ key, call, payload, successMsg, errorMsg, closeDialog }) {
  if (!attemptId.value) return
  actionLoading.value = key
  if (closeDialog) closeDialog()
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
  showWarningDialog.value = false
  await runAction({ key: 'warning', call: sendTeacherWarning, payload: warningMessage.value, successMsg: 'Đã gửi cảnh báo.', errorMsg: 'Gửi cảnh báo thất bại.' })
}

async function confirmStop() {
  showStopDialog.value = false
  await runAction({ key: 'invalidate', call: invalidateAttempt, payload: stopReason.value, successMsg: 'Đã dừng bài thi.', errorMsg: 'Dừng thi thất bại.' })
}

async function confirmPauseAction() {
  showPauseDialog.value = false
  await runAction({ key: 'pause', call: pauseAttempt, payload: pauseReason.value, successMsg: 'Đã tạm dừng bài thi.', errorMsg: 'Tạm dừng thất bại.' })
}

async function confirmResumeAction() {
  showResumeDialog.value = false
  await runAction({ key: 'resume', call: resumeAttempt, payload: resumeMessage.value, successMsg: 'Đã cho phép tiếp tục bài thi.', errorMsg: 'Khôi phục thất bại.' })
}

async function addNote() {
  if (!newNote.value.trim()) return
  noteLoading.value = true
  try {
    await addMonitoringNote(attemptId.value, newNote.value.trim())
    toast.success('Đã thêm ghi chú.')
    newNote.value = ''
    await loadData(true)
  } catch (err) {
    toast.error(err?.message || 'Thêm ghi chú thất bại.')
  } finally {
    noteLoading.value = false
  }
}

function handleEscape(e) {
  if (e.key !== 'Escape') return
  if (showWarningDialog.value) showWarningDialog.value = false
  else if (showStopDialog.value) showStopDialog.value = false
  else if (showPauseDialog.value) showPauseDialog.value = false
  else if (showResumeDialog.value) showResumeDialog.value = false
}

// ── Lifecycle ─────────────────────────────────────────────────────────────────
watch(attemptId, async (next, prev) => {
  if (!next || next === prev) return
  if (prev) unsubscribeFromAttempt(prev)
  await loadData()
  if (next) subscribeToAttempt(next)
})

onMounted(async () => {
  window.addEventListener('keydown', handleEscape)
  await loadData()
  if (attemptId.value) subscribeToAttempt(attemptId.value)
  startAutoRefresh()
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleEscape)
  stopAutoRefresh()
  if (attemptId.value) unsubscribeFromAttempt(attemptId.value)
})
</script>

<style scoped>
.mon-root {
  background: var(--mon-bg);
  min-height: 100vh;
}

/* ── Student header ──────────────────────────────────────────── */
.smd-header__student {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.smd-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.9rem;
  font-weight: 900;
  position: relative;
  flex-shrink: 0;
}
.smd-avatar__ring {
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  border: 3px solid;
}
.smd-header__student-info { min-width: 0; }
.smd-header__student-name {
  font-size: 1rem;
  font-weight: 800;
  color: var(--mon-text);
  margin: 0 0 0.2rem;
}
.smd-header__student-meta { display: flex; gap: 0.5rem; flex-wrap: wrap; }
.smd-meta-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.72rem;
  color: var(--mon-text-secondary);
  font-weight: 500;
}
.smd-badges { display: flex; gap: 0.375rem; }

/* Exam info */
.smd-header__exam-info { display: flex; flex-direction: column; }
.smd-header__exam-name {
  font-size: 0.825rem;
  font-weight: 700;
  color: var(--mon-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}
.smd-header__exam-code {
  font-size: 0.68rem;
  color: var(--mon-text-muted);
  font-family: monospace;
}

/* Gauge */
.smd-gauge {
  position: relative;
  width: 56px;
  height: 56px;
  flex-shrink: 0;
}
.smd-gauge__svg { transform: rotate(-90deg); width: 100%; height: 100%; }
.smd-gauge__track { fill: none; stroke: var(--mon-border); stroke-width: 8; }
.smd-gauge__fill {
  fill: none;
  stroke-width: 8;
  stroke-linecap: round;
  transition: stroke-dasharray 0.6s ease;
}
.smd-gauge__center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.smd-gauge__score { font-size: 1.1rem; font-weight: 900; line-height: 1; }
.smd-gauge__label { font-size: 0.5rem; color: var(--mon-text-muted); font-weight: 600; margin-top: 1px; }

/* ── Tabs ─────────────────────────────────────────────────────── */
.smd-tabs {
  display: flex;
  gap: 0.25rem;
  padding: 0.75rem 1.5rem;
  background: var(--mon-surface);
  border-bottom: 1px solid var(--mon-border);
}
.smd-tab {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: var(--mon-radius-sm);
  border: 1.5px solid transparent;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--mon-text-secondary);
  background: transparent;
  cursor: pointer;
  transition: all var(--mon-transition);
  font-family: var(--mon-font);
}
.smd-tab:hover { background: var(--mon-surface-2); color: var(--mon-text); }
.smd-tab--active {
  background: var(--mon-surface-2);
  border-color: var(--mon-border);
  color: var(--mon-primary);
}
.smd-tab__count {
  min-width: 18px;
  height: 18px;
  border-radius: 9999px;
  background: var(--mon-surface-2);
  color: var(--mon-text-secondary);
  font-size: 0.62rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}
.smd-tab--active .smd-tab__count { background: var(--mon-primary); color: var(--ds-surface); }

/* ── Content ─────────────────────────────────────────────────── */
.smd-content { padding: 1.25rem 1.5rem; }
.smd-tab-pane { display: flex; flex-direction: column; gap: 1rem; }

/* ── Overview Grid ────────────────────────────────────────────── */
.smd-overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}

/* ── Panel ───────────────────────────────────────────────────── */
.smd-panel {
  background: var(--mon-surface);
  border: 1.5px solid var(--mon-border);
  border-radius: var(--mon-radius-lg);
  overflow: hidden;
}
.smd-panel--warn { border-top: 2px solid var(--mon-warning); }
.smd-panel__header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--mon-border);
  color: var(--mon-text-secondary);
  font-size: 0.8rem;
  font-weight: 700;
  background: var(--mon-surface-2);
}
.smd-panel__badge {
  margin-left: auto;
  padding: 0.1rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.65rem;
  font-weight: 800;
  background: var(--mon-primary-soft);
  color: var(--mon-primary);
}
.smd-panel__badge--warn { background: var(--mon-warning-soft); color: var(--mon-warning); }
.smd-panel__filter {
  margin-left: auto;
  padding: 0.25rem 0.5rem;
  border: 1px solid var(--mon-border);
  border-radius: var(--mon-radius-xs);
  font-size: 0.72rem;
  color: var(--mon-text-secondary);
  background: var(--mon-surface);
  outline: none;
  cursor: pointer;
  font-family: var(--mon-font);
}
.smd-panel__body { padding: 1rem; }
.smd-panel__body--tight { padding: 0.75rem 1rem; }
.smd-panel__body--controls { display: flex; flex-direction: column; gap: 0.5rem; }

/* Live dot */
.smd-live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-left: 0.25rem;
}
.smd-live-dot--on { background: var(--mon-success); animation: mon-pulse 1.5s ease-in-out infinite; }
.smd-live-dot--off { background: var(--mon-text-secondary); }

/* Empty */
.smd-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1.5rem;
  color: var(--mon-text-secondary);
  font-size: 0.825rem;
  text-align: center;
}

/* Breakdown */
.smd-breakdown-list { display: flex; flex-direction: column; gap: 0.625rem; }
.smd-breakdown-row { display: flex; align-items: center; gap: 0.625rem; }
.smd-breakdown-row__label {
  font-size: 0.72rem;
  color: var(--mon-text-secondary);
  min-width: 120px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.smd-breakdown-row__bar { flex: 1; height: 5px; background: var(--mon-border); border-radius: 9999px; overflow: hidden; }
.smd-breakdown-row__fill { height: 100%; border-radius: 9999px; transition: width 0.5s ease; }
.smd-breakdown-row__score { font-size: 0.78rem; font-weight: 800; min-width: 24px; text-align: right; font-variant-numeric: tabular-nums; }

/* Progress */
.smd-progress-block { display: flex; align-items: baseline; gap: 0.5rem; margin-bottom: 0.625rem; }
.smd-progress-block__val { font-size: 1.5rem; font-weight: 900; color: var(--mon-text); }
.smd-progress-block__label { font-size: 0.8rem; color: var(--mon-text-secondary); }
.smd-progress-bar { height: 6px; background: var(--mon-border); border-radius: 9999px; overflow: hidden; margin-bottom: 0.5rem; }
.smd-progress-fill { height: 100%; border-radius: 9999px; transition: width 0.4s ease; }
.smd-progress-row { display: flex; justify-content: space-between; font-size: 0.775rem; color: var(--mon-text-secondary); }
.smd-remaining { display: flex; align-items: center; gap: 0.25rem; color: var(--mon-warning); font-weight: 600; }

/* Device rows */
.smd-device-row {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--mon-border);
  font-size: 0.775rem;
  color: var(--mon-text-secondary);
}
.smd-device-row:last-child { border-bottom: none; }
.smd-device-row__label { min-width: 90px; font-weight: 600; }
.smd-device-row__val { flex: 1; font-weight: 600; color: var(--mon-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* Pattern */
.smd-pattern {
  padding: 0.75rem;
  border-radius: var(--mon-radius-md);
  background: rgba(245,158,11,0.05);
  border: 1px solid rgba(245,158,11,0.12);
  margin-bottom: 0.5rem;
}
.smd-pattern:last-child { margin-bottom: 0; }
.smd-pattern__head { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.25rem; }
.smd-pattern__title { flex: 1; font-size: 0.8rem; font-weight: 700; color: var(--mon-text); }
.smd-pattern__level {
  font-size: 0.58rem;
  font-weight: 900;
  text-transform: uppercase;
  padding: 0.1rem 0.4rem;
  border-radius: 9999px;
  color: white;
}
.smd-pattern__desc { font-size: 0.72rem; color: var(--mon-text-secondary); margin: 0; }

/* Timeline */
.smd-timeline { display: flex; flex-direction: column; }
.smd-tl-item {
  display: flex;
  align-items: flex-start;
  gap: 0;
  position: relative;
  padding-bottom: 0.875rem;
}
.smd-tl-item__line {
  position: absolute;
  left: 11px;
  top: 24px;
  bottom: 0;
  width: 2px;
  background: var(--mon-border);
}
.smd-tl-item__dot {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 0.875rem;
  position: relative;
  z-index: 1;
}
.smd-tl-item__content { flex: 1; min-width: 0; padding-bottom: 0.75rem; border-bottom: 1px solid var(--mon-border); }
.smd-tl-item:last-child .smd-tl-item__content { border-bottom: none; padding-bottom: 0; }
.smd-tl-item__row { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; margin-bottom: 0.2rem; }
.smd-tl-item__type { font-size: 0.8rem; font-weight: 700; }
.smd-tl-item__time { font-size: 0.7rem; color: var(--mon-text-muted); font-variant-numeric: tabular-nums; white-space: nowrap; flex-shrink: 0; }
.smd-tl-item__details { font-size: 0.75rem; color: var(--mon-text-secondary); margin: 0 0 0.3rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.smd-tl-item__sev {
  display: inline-flex;
  padding: 0.1rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.62rem;
  font-weight: 700;
}
.smd-tl-item__sev--high { background: var(--mon-danger-soft); color: var(--mon-danger); }
.smd-tl-item__sev--medium { background: var(--mon-warning-soft); color: var(--mon-warning); }
.smd-tl-item__sev--low { background: var(--mon-info-soft); color: var(--mon-info); }

/* Realtime list */
.smd-realtime-list { display: flex; flex-direction: column; }
.smd-realtime-item {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
  padding: 0.625rem 0;
  border-bottom: 1px solid var(--mon-border);
  border-left: 3px solid transparent;
}
.smd-realtime-item:last-child { border-bottom: none; }
.smd-realtime-item--warn { border-left-color: var(--mon-warning); }
.smd-realtime-item--danger { border-left-color: var(--mon-danger); }
.smd-realtime-item--success { border-left-color: var(--mon-success); }
.smd-realtime-item__icon {
  width: 26px;
  height: 26px;
  border-radius: var(--mon-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--mon-border);
  color: var(--mon-text-secondary);
}
.smd-realtime-item--warn .smd-realtime-item__icon { background: var(--mon-warning-soft); color: var(--mon-warning); }
.smd-realtime-item--danger .smd-realtime-item__icon { background: var(--mon-danger-soft); color: var(--mon-danger); }
.smd-realtime-item--success .smd-realtime-item__icon { background: var(--mon-success-soft); color: var(--mon-success); }
.smd-realtime-item__body { flex: 1; min-width: 0; }
.smd-realtime-item__label { display: block; font-size: 0.78rem; font-weight: 700; color: var(--mon-text); }
.smd-realtime-item__score {
  font-size: 0.62rem;
  font-weight: 800;
  padding: 0.1rem 0.4rem;
  border-radius: 9999px;
  background: var(--mon-warning-soft);
  color: var(--mon-warning);
  margin-left: 0.375rem;
}
.smd-realtime-item__msg { font-size: 0.72rem; color: var(--mon-text-secondary); margin: 0.1rem 0 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.smd-realtime-item__time { font-size: 0.65rem; color: var(--mon-text-muted); font-variant-numeric: tabular-nums; flex-shrink: 0; white-space: nowrap; }

/* Action history */
.smd-action-list { display: flex; flex-direction: column; }
.smd-action-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.625rem 0;
  border-bottom: 1px solid var(--mon-border);
}
.smd-action-item:last-child { border-bottom: none; }
.smd-action-item__icon {
  width: 28px;
  height: 28px;
  border-radius: var(--mon-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--mon-border);
  color: var(--mon-text-secondary);
}
.smd-action-item__icon--warn { background: var(--mon-warning-soft); color: var(--mon-warning); }
.smd-action-item__icon--danger { background: var(--mon-danger-soft); color: var(--mon-danger); }
.smd-action-item__icon--success { background: var(--mon-success-soft); color: var(--mon-success); }
.smd-action-item__icon--neutral { background: var(--mon-primary-soft); color: var(--mon-primary); }
.smd-action-item__body { flex: 1; }
.smd-action-item__label { display: block; font-size: 0.8rem; font-weight: 700; color: var(--mon-text); }
.smd-action-item__msg { font-size: 0.72rem; color: var(--mon-text-secondary); margin: 0.2rem 0 0; }
.smd-action-item__time { font-size: 0.65rem; color: var(--mon-text-muted); font-variant-numeric: tabular-nums; flex-shrink: 0; white-space: nowrap; }

/* Notes */
.smd-notes-list { display: flex; flex-direction: column; }
.smd-note-item { padding: 0.75rem 0; border-bottom: 1px solid var(--mon-border); }
.smd-note-item:last-child { border-bottom: none; }
.smd-note-item__body { font-size: 0.825rem; color: var(--mon-text); margin-bottom: 0.375rem; }
.smd-note-item__meta { display: flex; gap: 0.75rem; }
.smd-note-item__author { font-size: 0.72rem; font-weight: 600; color: var(--mon-primary); }
.smd-note-item__time { font-size: 0.72rem; color: var(--mon-text-muted); font-variant-numeric: tabular-nums; }

/* ── Responsive ─────────────────────────────────────────────── */
@media (max-width: 768px) {
  .smd-header { flex-direction: column; align-items: flex-start; }
  .smd-overview-grid { grid-template-columns: 1fr; }
  .smd-content { padding: 1rem; }
  .smd-tabs { padding: 0.5rem 1rem; }
}

/* ── Reduced Motion ─────────────────────────────────────────── */
@media (prefers-reduced-motion: reduce) {
  .smd-panel { transition: none; }
  .smd-gauge__fill, .smd-progress-fill { transition: none; }
  .smd-breakdown-row__fill { transition: none; }
}
</style>
