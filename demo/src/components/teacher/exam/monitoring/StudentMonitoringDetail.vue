<template>
  <div class="smd-root">

    <!-- Loading -->
    <div v-if="loading" class="smd-loading">
      <div class="smd-loading__spinner" />
      <span>Đang tải dữ liệu học sinh...</span>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="smd-error">
      <LucideIcon name="alert-circle" :size="28" />
      <p>{{ error }}</p>
      <button class="smd-error__retry" @click="loadData">Thử lại</button>
    </div>

    <template v-else>

      <!-- ── Page Header ─────────────────────────────────────────── -->
      <header class="smd-header">
        <div class="smd-header__left">
          <RouterLink :to="backLink" class="portal-icon-btn">
            <LucideIcon name="arrow-left" :size="16" />
          </RouterLink>
          <div class="smd-header__avatar-wrap">
            <div class="smd-header__avatar" :style="{ background: riskBg }">
              <span :style="{ color: riskColor }">{{ studentInitials }}</span>
              <div class="smd-header__avatar-ring" :style="{ borderColor: statusColor }" />
            </div>
          </div>
          <div class="smd-header__info">
            <h1 class="smd-header__name">{{ studentName }}</h1>
            <div class="smd-header__meta">
              <span v-if="studentCode" class="smd-meta-tag">
                <LucideIcon name="hash" :size="11" />{{ studentCode }}
              </span>
              <span v-if="studentEmail" class="smd-meta-tag">
                <LucideIcon name="mail" :size="11" />{{ studentEmail }}
              </span>
              <span class="smd-meta-tag">
                <LucideIcon name="clock" :size="11" />{{ sessionTime }}
              </span>
            </div>
          </div>
        </div>

        <div class="smd-header__right">
          <div class="smd-header__badges">
            <span class="smd-badge smd-badge--risk" :style="{ color: riskColor, background: riskBg }">
              <LucideIcon name="shield-alert" :size="11" />
              {{ riskBandLabel }}
            </span>
            <span class="smd-badge smd-badge--status" :style="{ color: statusColor }">
              {{ statusLabel }}
            </span>
          </div>

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

          <!-- Device chips -->
          <div class="smd-device-chips">
            <span class="smd-device-chip" :class="{ 'smd-device-chip--on': attemptData.cameraOn }">
              <LucideIcon name="camera" :size="12" />Cam
            </span>
            <span class="smd-device-chip" :class="{ 'smd-device-chip--on': attemptData.micOn }">
              <LucideIcon name="mic" :size="12" />Mic
            </span>
          </div>
        </div>
      </header>

      <!-- ── Recommendation Banner ──────────────────────────────── -->
      <div v-if="riskData.reviewRequired || riskData.reasons?.length" class="smd-recommend">
        <div class="smd-recommend__icon">
          <LucideIcon name="lightbulb" :size="15" />
        </div>
        <div class="smd-recommend__body">
          <span class="smd-recommend__action" :style="{ color: riskColor }">
            {{ recommendedActionLabel }}
          </span>
          <div v-if="riskData.reasons?.length" class="smd-recommend__reasons">
            <span v-for="r in riskData.reasons" :key="r" class="smd-reason-tag">{{ r }}</span>
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
        </button>
      </div>

      <!-- ── Content ─────────────────────────────────────────── -->
      <div class="smd-content">

        <!-- Tab: Overview -->
        <div v-if="activeTab === 'overview'" class="smd-tab-pane">
          <div class="smd-overview-grid">

            <!-- Risk breakdown -->
            <div class="smd-card">
              <div class="smd-card__header">
                <LucideIcon name="bar-chart-2" :size="14" />
                <span>Phân tích điểm rủi ro</span>
              </div>
              <div class="smd-card__body">
                <div v-if="!hasBreakdown" class="smd-empty-sm">Không có dữ liệu phân tích</div>
                <div v-else class="smd-breakdown-list">
                  <div v-for="(score, key) in riskData.breakdown" :key="key" class="smd-breakdown-row">
                    <span class="smd-breakdown-row__label">{{ getVLabel(key) }}</span>
                    <div class="smd-breakdown-row__bar">
                      <div class="smd-breakdown-row__fill"
                        :style="{ width: Math.min(score, 100) + '%', background: sColor(score) }" />
                    </div>
                    <span class="smd-breakdown-row__score" :style="{ color: sColor(score) }">{{ score }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Progress -->
            <div class="smd-card">
              <div class="smd-card__header">
                <LucideIcon name="list-checks" :size="14" />
                <span>Tiến độ bài thi</span>
              </div>
              <div class="smd-card__body">
                <div class="smd-progress-block">
                  <span class="smd-progress-block__val">
                    {{ attemptData.answeredCount || 0 }}/{{ attemptData.totalQuestions || 0 }}
                  </span>
                  <span class="smd-progress-block__label">câu đã trả lời</span>
                </div>
                <div class="smd-progress-bar">
                  <div class="smd-progress-fill"
                    :style="{ width: progressPercent + '%', background: riskColor }" />
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
            <div class="smd-card">
              <div class="smd-card__header">
                <LucideIcon name="monitor" :size="14" />
                <span>Thông tin thiết bị</span>
              </div>
              <div class="smd-card__body smd-card__body--tight">
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
            <div v-if="suspiciousPatterns.length > 0" class="smd-card smd-card--warn">
              <div class="smd-card__header">
                <LucideIcon name="brain" :size="14" />
                <span>Mẫu hành vi đáng ngờ</span>
                <span class="smd-card__badge smd-card__badge--warn">{{ suspiciousPatterns.length }}</span>
              </div>
              <div class="smd-card__body">
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

          </div>
        </div>

        <!-- Tab: Timeline -->
        <div v-if="activeTab === 'timeline'" class="smd-tab-pane">

          <!-- Violation timeline -->
          <div class="smd-card">
            <div class="smd-card__header">
              <LucideIcon name="activity" :size="14" />
              <span>Dòng thời gian vi phạm</span>
              <span class="smd-card__badge">{{ filteredTimeline.length }}</span>
              <select v-model="timelineFilter" class="smd-card__filter-select">
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
            <div class="smd-card__body">
              <div v-if="filteredTimeline.length === 0" class="smd-empty-sm">
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
          <div class="smd-card">
            <div class="smd-card__header">
              <LucideIcon name="zap" :size="14" />
              <span>Sự kiện realtime</span>
              <div class="smd-live-dot" :class="isConnected ? 'smd-live-dot--on' : 'smd-live-dot--off'" />
              <span v-if="realtimeFeed.length" class="smd-card__badge">{{ realtimeFeed.length }}</span>
            </div>
            <div class="smd-card__body">
              <div v-if="realtimeFeed.length === 0" class="smd-empty-sm">
                <LucideIcon name="inbox" :size="22" />
                <p>Chưa có sự kiện realtime</p>
              </div>
              <div v-else class="smd-realtime-list">
                <div v-for="evt in realtimeFeed" :key="evt.id" class="smd-realtime-item"
                  :class="`smd-realtime-item--${evt.tone}`">
                  <div class="smd-realtime-item__icon">
                    <LucideIcon :name="evt.icon" :size="13" />
                  </div>
                  <div class="smd-realtime-item__body">
                    <span class="smd-realtime-item__label">{{ evt.label }}</span>
                    <span v-if="evt.riskScore != null" class="smd-realtime-item__score">{{ evt.riskScore }}</span>
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

          <!-- Action history -->
          <div class="smd-card">
            <div class="smd-card__header">
              <LucideIcon name="list" :size="14" />
              <span>Hành động đã thực hiện</span>
            </div>
            <div class="smd-card__body">
              <div v-if="actionHistory.length === 0" class="smd-empty-sm">
                Chưa có hành động nào
              </div>
              <div v-else class="smd-action-list">
                <div v-for="a in actionHistory" :key="a.key" class="smd-action-item">
                  <div class="smd-action-item__icon" :class="`smd-action-item__icon--${a.type}`">
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

          <!-- Control buttons -->
          <div class="smd-card">
            <div class="smd-card__header">
              <LucideIcon name="settings" :size="14" />
              <span>Điều khiển</span>
            </div>
            <div class="smd-card__body smd-card__body--controls">
              <button class="smd-ctrl-btn smd-ctrl-btn--warn"
                :disabled="actionLoading === 'warning'" @click="showWarnDialog = true">
                <LucideIcon name="alert-triangle" :size="15" />
                <div>
                  <div class="smd-ctrl-btn__label">Gửi cảnh báo</div>
                  <div class="smd-ctrl-btn__desc">Hiển thị thông báo cho học sinh</div>
                </div>
              </button>

              <button v-if="isStudentPaused" class="smd-ctrl-btn smd-ctrl-btn--success"
                :disabled="actionLoading === 'resume'" @click="confirmResumeAction">
                <LucideIcon name="play" :size="15" />
                <div>
                  <div class="smd-ctrl-btn__label">Cho phép tiếp tục</div>
                  <div class="smd-ctrl-btn__desc">Khôi phục bài thi cho học sinh</div>
                </div>
              </button>
              <button v-else class="smd-ctrl-btn smd-ctrl-btn--pause"
                :disabled="actionLoading === 'pause' || isStudentTerminal" @click="showPauseDialog = true">
                <LucideIcon name="pause" :size="15" />
                <div>
                  <div class="smd-ctrl-btn__label">Tạm dừng thi</div>
                  <div class="smd-ctrl-btn__desc">Tạm ngừng bài thi của học sinh</div>
                </div>
              </button>

              <button class="smd-ctrl-btn smd-ctrl-btn--danger"
                :disabled="actionLoading === 'invalidate' || isStudentTerminal" @click="showStopDialog = true">
                <LucideIcon name="x-circle" :size="15" />
                <div>
                  <div class="smd-ctrl-btn__label">Buộc nộp bài</div>
                  <div class="smd-ctrl-btn__desc">Kết thúc bài thi — không thể hoàn tác</div>
                </div>
              </button>
            </div>
          </div>
        </div>

        <!-- Tab: Notes -->
        <div v-if="activeTab === 'notes'" class="smd-tab-pane">

          <!-- Add note -->
          <div class="smd-card">
            <div class="smd-card__header">
              <LucideIcon name="sticky-note" :size="14" />
              <span>Thêm ghi chú</span>
            </div>
            <div class="smd-card__body">
              <textarea v-model="newNote" class="smd-note-input"
                rows="3" placeholder="Nhập ghi chú về hành vi của học sinh..." />
              <button class="smd-note-submit" :disabled="!newNote.trim() || noteLoading" @click="addNote">
                <LucideIcon name="plus" :size="14" />
                Thêm ghi chú
              </button>
            </div>
          </div>

          <!-- Note history -->
          <div class="smd-card">
            <div class="smd-card__header">
              <LucideIcon name="list" :size="14" />
              <span>Lịch sử ghi chú</span>
              <span class="smd-card__badge">{{ notes.length }}</span>
            </div>
            <div class="smd-card__body">
              <div v-if="notes.length === 0" class="smd-empty-sm">Chưa có ghi chú nào</div>
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

    <!-- ── Dialogs (PrimeVue) ────────────────────────────────── -->

    <!-- Warning dialog -->
    <Dialog v-model:visible="showWarnDialog" header="Gửi cảnh báo"
      :modal="true" :closable="true" :style="{ width: '420px' }"
      :pt="dialogBasePt('warn')">
      <p style="font-size: 0.875rem; color: var(--smd-text, #64748b); margin: 0 0 1rem;">
        Gửi cảnh báo tới <strong style="color: var(--smd-text-deep, #0f172a);">{{ studentName }}</strong>
      </p>
      <textarea v-model="warningMessage" rows="3"
        placeholder="Nội dung cảnh báo (để trống = mặc định)"
        style="width: 100%; padding: 0.625rem; border: 1.5px solid #e2e8f0; border-radius: 8px; background: #f8fafc; color: #0f172a; font-size: 0.85rem; resize: vertical; outline: none; box-sizing: border-box;" />
      <template #footer>
        <button class="smd-dialog__cancel" @click="showWarnDialog = false">Hủy</button>
        <button class="smd-dialog__confirm smd-dialog__confirm--warn"
          :disabled="actionLoading === 'warning'" @click="confirmSendWarning">
          {{ actionLoading === 'warning' ? 'Đang gửi...' : 'Gửi cảnh báo' }}
        </button>
      </template>
    </Dialog>

    <!-- Stop dialog -->
    <Dialog v-model:visible="showStopDialog" header="Xác nhận dừng thi"
      :modal="true" :closable="true" :style="{ width: '420px' }"
      :pt="dialogBasePt('danger')">
      <p style="font-size: 0.875rem; color: #64748b; margin: 0 0 1rem;">
        Hành động này sẽ <strong style="color: #dc2626;">buộc nộp bài</strong> của
        <strong style="color: #0f172a;">{{ studentName }}</strong>. Không thể hoàn tác.
      </p>
      <textarea v-model="stopReason" rows="2"
        placeholder="Lý do dừng thi (để trống = mặc định)"
        style="width: 100%; padding: 0.625rem; border: 1.5px solid #e2e8f0; border-radius: 8px; background: #f8fafc; color: #0f172a; font-size: 0.85rem; resize: vertical; outline: none; box-sizing: border-box;" />
      <template #footer>
        <button class="smd-dialog__cancel" @click="showStopDialog = false">Hủy</button>
        <button class="smd-dialog__confirm smd-dialog__confirm--danger"
          :disabled="actionLoading === 'invalidate'" @click="confirmStop">
          {{ actionLoading === 'invalidate' ? 'Đang xử lý...' : 'Xác nhận dừng thi' }}
        </button>
      </template>
    </Dialog>

    <!-- Pause dialog -->
    <Dialog v-model:visible="showPauseDialog" header="Tạm dừng thi"
      :modal="true" :closable="true" :style="{ width: '420px' }"
      :pt="dialogBasePt('pause')">
      <p style="font-size: 0.875rem; color: #64748b; margin: 0 0 1rem;">
        Tạm dừng bài thi của <strong style="color: #0f172a;">{{ studentName }}</strong>?
        Học sinh sẽ không thể tiếp tục cho đến khi được cho phép.
      </p>
      <textarea v-model="pauseReason" rows="2" placeholder="Lý do (tùy chọn)"
        style="width: 100%; padding: 0.625rem; border: 1.5px solid #e2e8f0; border-radius: 8px; background: #f8fafc; color: #0f172a; font-size: 0.85rem; resize: vertical; outline: none; box-sizing: border-box;" />
      <template #footer>
        <button class="smd-dialog__cancel" @click="showPauseDialog = false">Hủy</button>
        <button class="smd-dialog__confirm smd-dialog__confirm--pause"
          :disabled="actionLoading === 'pause'" @click="confirmPause">
          {{ actionLoading === 'pause' ? 'Đang xử lý...' : 'Xác nhận' }}
        </button>
      </template>
    </Dialog>

    <!-- Resume dialog -->
    <Dialog v-model:visible="showResumeDialog" header="Cho phép tiếp tục thi"
      :modal="true" :closable="true" :style="{ width: '420px' }"
      :pt="dialogBasePt('success')">
      <p style="font-size: 0.875rem; color: #64748b; margin: 0 0 1rem;">
        Học sinh <strong style="color: #0f172a;">{{ studentName }}</strong> sẽ được khôi phục bài thi.
      </p>
      <textarea v-model="resumeMessage" rows="2" placeholder="Lời nhắn (tùy chọn)"
        style="width: 100%; padding: 0.625rem; border: 1.5px solid #e2e8f0; border-radius: 8px; background: #f8fafc; color: #0f172a; font-size: 0.85rem; resize: vertical; outline: none; box-sizing: border-box;" />
      <template #footer>
        <button class="smd-dialog__cancel" @click="showResumeDialog = false">Hủy</button>
        <button class="smd-dialog__confirm smd-dialog__confirm--success"
          :disabled="actionLoading === 'resume'" @click="confirmResumeAction">
          {{ actionLoading === 'resume' ? 'Đang xử lý...' : 'Cho phép tiếp tục' }}
        </button>
      </template>
    </Dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import Dialog from 'primevue/dialog'
import LucideIcon from '../../../common/LucideIcon.vue'
import { useToast } from '../../../../composables/useToast'
import { useExamMonitoring } from '../../../../composables/useExamMonitoring'
import {
  fetchAttemptDetail,
  fetchAttemptRisk,
  sendTeacherWarning,
  pauseAttempt,
  resumeAttempt,
  invalidateAttempt,
  listMonitoringTimeline,
  addMonitoringNote
} from '../../../../services/examMonitoringService'
import { isAttemptPaused, isAttemptTerminal } from '../../../../utils/proctorStatusMeta'

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
  TAB_SWITCH: '#f59e0b', COPY_PASTE: '#dc2626', DEVTOOLS_OPEN: '#dc2626', EXIT_FULLSCREEN: '#f59e0b',
  MULTI_MONITOR: '#dc2626', DUPLICATE_IP: '#dc2626', PRINT_SCREEN: '#dc2626', WINDOW_BLUR: '#f59e0b',
  IDLE_TIME: '#0ea5e9', RIGHT_CLICK: '#0ea5e9', HEARTBEAT_STALE: '#f59e0b', RAPID_QUESTION_SWITCH: '#f59e0b',
  DEVICE_FINGERPRINT_CHANGED: '#dc2626', SYNC_BEHAVIOR: '#dc2626', IP_FINGERPRINT_GRAPH: '#dc2626',
  ANSWER_SIMILARITY: '#dc2626', AI_MULTIPLE_FACES: '#dc2626', AI_PHONE_DETECTED: '#dc2626',
  AI_LOOKING_AWAY: '#0ea5e9', WARNING_SENT: '#6366f1', NOTE: '#94a3b8'
}
const V_ICONS = {
  TAB_SWITCH: 'layers', WINDOW_BLUR: 'layers', IDLE_TIME: 'clock', RIGHT_CLICK: 'mouse-pointer-2',
  EXIT_FULLSCREEN: 'minimize', COPY_PASTE: 'copy', DEVTOOLS_OPEN: 'code', PRINT_SCREEN: 'code',
  MULTI_MONITOR: 'monitor', DUPLICATE_IP: 'globe', RAPID_QUESTION_SWITCH: 'monitor', HEARTBEAT_STALE: 'wifi-off',
  DEVICE_FINGERPRINT_CHANGED: 'code', SYNC_BEHAVIOR: 'monitor', IP_FINGERPRINT_GRAPH: 'globe',
  ANSWER_SIMILARITY: 'copy', AI_MULTIPLE_FACES: 'monitor', AI_PHONE_DETECTED: 'monitor', AI_LOOKING_AWAY: 'wifi-off',
  WARNING_SENT: 'alert-triangle', NOTE: 'sticky-note'
}
const V_LABELS = {
  TAB_SWITCH: 'Chuyển tab', WINDOW_BLUR: 'Mất tiêu điểm', IDLE_TIME: 'Không hoạt động',
  RIGHT_CLICK: 'Click chuột phải', EXIT_FULLSCREEN: 'Thoát toàn màn hình', COPY_PASTE: 'Sao chép nội dung',
  DEVTOOLS_OPEN: 'Mở DevTools', PRINT_SCREEN: 'Chụp màn hình', MULTI_MONITOR: 'Nhiều màn hình',
  DUPLICATE_IP: 'IP trùng lặp', RAPID_QUESTION_SWITCH: 'Chuyển câu nhanh', HEARTBEAT_STALE: 'Mất kết nối',
  DEVICE_FINGERPRINT_CHANGED: 'Thay đổi thiết bị', SYNC_BEHAVIOR: 'Hành vi đồng bộ',
  IP_FINGERPRINT_GRAPH: 'Liên kết IP', ANSWER_SIMILARITY: 'Tương đồng đáp án',
  AI_MULTIPLE_FACES: 'Nhiều khuôn mặt', AI_PHONE_DETECTED: 'Phát hiện điện thoại', AI_LOOKING_AWAY: 'Nhìn lệch hướng',
  WARNING_SENT: 'Cảnh báo đã gửi', NOTE: 'Ghi chú'
}
const REALTIME_META = {
  RISK_UPDATED: { icon: 'trending-up', tone: 'warn', label: 'Cập nhật rủi ro' },
  SUSPICIOUS: { icon: 'alert-triangle', tone: 'warn', label: 'Hành vi đáng ngờ' },
  WARNING_SENT: { icon: 'alert-circle', tone: 'warn', label: 'Đã gửi cảnh báo' },
  ATTEMPT_PAUSED: { icon: 'pause-circle', tone: 'warn', label: 'Tạm dừng' },
  ATTEMPT_RESUMED: { icon: 'play-circle', tone: 'success', label: 'Tiếp tục' },
  ATTEMPT_STOPPED: { icon: 'x-circle', tone: 'danger', label: 'Đình chỉ' }
}

const route = useRoute()
const toast = useToast()
const { isConnected, subscribeToAttempt, unsubscribeFromAttempt } = useExamMonitoring()

const attemptId = computed(() => route.params.attemptId)
const examId = computed(() => route.params.examId)
const backLink = computed(() => `/teacher/exams/${examId.value}/monitoring`)

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

const showWarnDialog = ref(false)
const showStopDialog = ref(false)
const showPauseDialog = ref(false)
const showResumeDialog = ref(false)
const warningMessage = ref('')
const stopReason = ref('')
const pauseReason = ref('')
const resumeMessage = ref('')
const actionLoading = ref('')

const tabs = [
  { id: 'overview', label: 'Tổng quan', icon: 'layout-dashboard' },
  { id: 'timeline', label: 'Timeline', icon: 'activity' },
  { id: 'actions', label: 'Hành động', icon: 'settings' },
  { id: 'notes', label: 'Ghi chú', icon: 'sticky-note' }
]

// ── Student info ──────────────────────────────────────────────────────────────

const studentInitials = computed(() => {
  const name = attemptData.value.student || attemptData.value.studentName || '?'
  const parts = name.trim().split(/\s+/)
  return (parts[parts.length - 1].charAt(0) + (parts.length > 1 ? parts[0].charAt(0) : '')).toUpperCase()
})

const studentName = computed(() => attemptData.value.student || attemptData.value.studentName || '—')
const studentCode = computed(() => attemptData.value.studentCode || '')
const studentEmail = computed(() => attemptData.value.email || attemptData.value.studentEmail || '')

const sessionTime = computed(() => {
  const ts = attemptData.value.startedAt
  if (!ts) return '—'
  const diff = Math.floor((Date.now() - new Date(ts).getTime()) / 60000)
  if (diff < 1) return '<1 phút'
  if (diff < 60) return `${diff} phút`
  return `${Math.floor(diff / 60)}h ${diff % 60}p`
})

// ── Risk ─────────────────────────────────────────────────────────────────────

const riskScore = computed(() => Math.round(riskData.value.riskScore || 0))

const riskBand = computed(() => {
  const s = riskScore.value
  if (s >= RISK_BAND_THRESHOLDS.CRITICAL) return 'CRITICAL'
  if (s >= RISK_BAND_THRESHOLDS.HIGH_RISK) return 'HIGH_RISK'
  if (s >= RISK_BAND_THRESHOLDS.SUSPICIOUS) return 'SUSPICIOUS'
  return 'CLEAN'
})

const riskBandLabel = computed(() => RISK_LEVEL_LABELS[riskBand.value] || riskBand.value)
const riskColor = computed(() => {
  const map = { CRITICAL: '#dc2626', HIGH_RISK: '#f59e0b', SUSPICIOUS: '#eab308', CLEAN: '#16a34a' }
  return map[riskBand.value] || '#94a3b8'
})
const riskBg = computed(() => {
  const map = {
    CRITICAL: 'rgba(220,38,38,0.08)',
    HIGH_RISK: 'rgba(245,158,11,0.08)',
    SUSPICIOUS: 'rgba(234,179,8,0.08)',
    CLEAN: 'rgba(22,163,74,0.08)'
  }
  return map[riskBand.value] || 'rgba(0,0,0,0.04)'
})

// ── Status ───────────────────────────────────────────────────────────────────

const statusToken = computed(() => {
  const raw = String(attemptData.value.status || '').toUpperCase()
  if (/SUBMITTED|COMPLETED/.test(raw)) return 'SUBMITTED'
  if (/PAUSED/.test(raw)) return 'PAUSED'
  if (/STOPPED|OFFLINE/.test(raw)) return 'STOPPED'
  if (/ACTIVE|IN_PROGRESS/.test(raw)) return 'ONLINE'
  return 'OFFLINE'
})

const statusLabel = computed(() => {
  const map = { ONLINE: 'Đang thi', SUBMITTED: 'Đã nộp', PAUSED: 'Tạm dừng', STOPPED: 'Dừng', OFFLINE: 'Offline' }
  return map[statusToken.value] || statusToken.value
})

const statusColor = computed(() => {
  const map = { ONLINE: '#4f46e5', SUBMITTED: '#16a34a', PAUSED: '#f59e0b', STOPPED: '#dc2626', OFFLINE: '#94a3b8' }
  return map[statusToken.value] || '#94a3b8'
})

const isStudentPaused = computed(() => statusToken.value === 'PAUSED')
const isStudentTerminal = computed(() => statusToken.value === 'SUBMITTED' || statusToken.value === 'STOPPED')

// ── Gauge ─────────────────────────────────────────────────────────────────────

const gaugeArc = computed(() => {
  const circ = 2 * Math.PI * 44
  const score = Math.max(0, Math.min(100, riskScore.value))
  return `${(score / 100) * circ} ${circ}`
})

// ── Progress ─────────────────────────────────────────────────────────────────

const progressPercent = computed(() => {
  const total = attemptData.value.totalQuestions || 0
  const answered = attemptData.value.answeredCount || 0
  return total > 0 ? Math.round((answered / total) * 100) : 0
})

const hasBreakdown = computed(() => riskData.value.breakdown && Object.keys(riskData.value.breakdown).length > 0)

// ── Device info ────────────────────────────────────────────────────────────────

const deviceInfo = computed(() => {
  const raw = attemptData.value.deviceInfo || attemptData.value.metadata || {}
  return {
    device: raw.device || raw.platform || '—',
    browser: raw.browser || raw.userAgent ? (raw.browser || raw.userAgent).split(' ')[0] : '—',
    os: raw.os || raw.operatingSystem || '—'
  }
})

// ── Breakdown color ────────────────────────────────────────────────────────────

function sColor(score) {
  const s = Number(score || 0)
  if (s >= 81) return '#dc2626'
  if (s >= 61) return '#f59e0b'
  if (s >= 31) return '#eab308'
  return '#16a34a'
}

// ── Pattern detection ─────────────────────────────────────────────────────────

const suspiciousPatterns = computed(() => {
  const evts = timeline.value
  if (!evts?.length) return []
  const tabCount = evts.filter(e => e.eventType === 'TAB_SWITCH').length
  const copyCount = evts.filter(e => /COPY|PASTE/.test(e.eventType)).length
  const devCount = evts.filter(e => /DEVTOOLS|TOOLS/.test(e.eventType)).length
  const dupCount = evts.filter(e => e.eventType === 'DUPLICATE_IP').length
  const patterns = []
  if (tabCount >= 3) patterns.push({ id: 'tab', title: 'Chuyển tab nhiều lần', description: `${tabCount} lần chuyển tab (ngưỡng: 3)`, level: tabCount > 5 ? 'high' : 'medium' })
  if (copyCount >= 1) patterns.push({ id: 'copy', title: 'Cố gắng copy nội dung', description: `${copyCount} lần sao chép dữ liệu`, level: 'medium' })
  if (devCount >= 1) patterns.push({ id: 'dev', title: 'Mở công cụ phát triển', description: `${devCount} lần mở DevTools`, level: 'high' })
  if (dupCount >= 1) patterns.push({ id: 'dup', title: 'Phát hiện IP trùng lặp', description: `${dupCount} thiết bị khác cùng IP`, level: 'high' })
  return patterns
})

function pColor(level) {
  const map = { high: '#dc2626', medium: '#f59e0b', low: '#16a34a' }
  return map[level] || '#94a3b8'
}

// ── Timeline ──────────────────────────────────────────────────────────────────

const filteredTimeline = computed(() => {
  if (!timelineFilter.value) return timeline.value
  return timeline.value.filter(e => e.eventType === timelineFilter.value)
})

function getVColor(type) { return V_COLORS[type] || '#94a3b8' }
function getVIcon(type) { return V_ICONS[type] || 'alert-circle' }
function getVLabel(type) { return V_LABELS[type] || type }

function getSeverityStatus(sev) {
  if (sev === 'HIGH' || sev === 'CRITICAL') return 'high'
  if (sev === 'MEDIUM') return 'medium'
  return 'low'
}

function getSeverityLabel(sev) { return SEVERITY_LABELS[sev] || sev || '' }

// ── Recommended action ─────────────────────────────────────────────────────────

const recommendedActionLabel = computed(() => {
  if (riskBand.value === 'CRITICAL') return RECOMMENDED_ACTION_LABELS.PAUSE_AND_REVIEW
  if (riskBand.value === 'HIGH_RISK') return RECOMMENDED_ACTION_LABELS.WARN_AND_ESCALATE
  if (riskBand.value === 'SUSPICIOUS') return RECOMMENDED_ACTION_LABELS.REVIEW_ATTEMPT
  return RECOMMENDED_ACTION_LABELS.CONTINUE_MONITORING
})

// ── Notes ───────────────────────────────────────────────────────────────────────

const notes = computed(() =>
  timeline.value.filter(e => e.eventType === 'NOTE')
)

async function addNote() {
  if (!newNote.value.trim() || noteLoading.value) return
  noteLoading.value = true
  try {
    await addMonitoringNote(attemptId.value, newNote.value)
    newNote.value = ''
    toast.success('Đã thêm ghi chú.')
    await loadTimeline()
  } catch (err) {
    toast.error(err?.message || 'Thêm ghi chú thất bại.')
  } finally {
    noteLoading.value = false
  }
}

// ── Action history ────────────────────────────────────────────────────────────

const ACTION_META = {
  WARNING_SENT: { icon: 'alert-triangle', label: 'Cảnh báo', type: 'warn' },
  ATTEMPT_PAUSED: { icon: 'pause-circle', label: 'Tạm dừng', type: 'warn' },
  ATTEMPT_RESUMED: { icon: 'play-circle', label: 'Tiếp tục', type: 'success' },
  ATTEMPT_STOPPED: { icon: 'x-circle', label: 'Đình chỉ', type: 'danger' },
  NOTE: { icon: 'sticky-note', label: 'Ghi chú', type: 'neutral' }
}

const actionHistory = computed(() =>
  timeline.value
    .filter(e => e.eventType in ACTION_META || e.eventType === 'NOTE')
    .map((e, i) => {
      const meta = ACTION_META[e.eventType] || {}
      return { ...e, key: e.key || `a-${i}`, icon: meta.icon || 'circle', label: meta.label || e.eventType, type: meta.type || 'neutral' }
    })
)

// ── Format helpers ────────────────────────────────────────────────────────────

function formatTime(ts) {
  if (!ts) return '—'
  return new Date(ts).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

function formatRemaining(seconds) {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) return `${h}h ${m}p`
  if (m > 0) return `${m}m ${s}s`
  return `${s}s`
}

// ── Data loading ───────────────────────────────────────────────────────────────

async function loadData() {
  if (!attemptId.value) return
  loading.value = true
  error.value = ''
  try {
    const [attempt, risk] = await Promise.allSettled([
      fetchAttemptDetail(attemptId.value),
      fetchAttemptRisk(attemptId.value)
    ])
    if (attempt.status === 'fulfilled' && attempt.value) {
      attemptData.value = attempt.value
    } else {
      error.value = 'Không tìm thấy thông tin bài thi.'
      return
    }
    if (risk.status === 'fulfilled' && risk.value) {
      riskData.value = risk.value
    }
    await loadTimeline()
  } catch (e) {
    error.value = e?.message || 'Tải dữ liệu thất bại.'
  } finally {
    loading.value = false
  }
}

async function loadTimeline() {
  try {
    timeline.value = await listMonitoringTimeline(attemptId.value)
  } catch { timeline.value = [] }
}

// ── Dialog helpers ────────────────────────────────────────────────────────────

function dialogBasePt(variant) {
  const colors = {
    warn: { header: '#f59e0b', bg: 'rgba(245,158,11,0.06)' },
    danger: { header: '#dc2626', bg: 'rgba(220,38,38,0.06)' },
    pause: { header: '#4f46e5', bg: 'rgba(79,70,229,0.06)' },
    success: { header: '#16a34a', bg: 'rgba(22,163,74,0.06)' }
  }
  const c = colors[variant] || colors.warn
  return {
    root: { class: 'portal-surface-card' },
    header: { style: { background: c.bg, border: 'none', padding: '1rem 1.25rem', color: c.header } },
    content: { style: { padding: '1rem 1.25rem' } },
    footer: { style: { padding: '0.75rem 1.25rem', border: 'none' } }
  }
}

// ── Actions ────────────────────────────────────────────────────────────────────

async function runAction({ key, call, successMsg, errorMsg }) {
  actionLoading.value = key
  try {
    await call(attemptId.value)
    toast.success(successMsg)
    await loadData()
    realtimeFeed.value.unshift({
      id: `local-${Date.now()}`,
      ...(REALTIME_META[key] || { icon: 'circle', tone: 'warn', label: key }),
      timestamp: Date.now()
    })
  } catch (err) {
    toast.error(err?.message || errorMsg)
  } finally {
    actionLoading.value = ''
  }
}

async function confirmSendWarning() {
  showWarnDialog.value = false
  await runAction({
    key: 'WARNING_SENT',
    call: (id) => sendTeacherWarning(id, warningMessage.value),
    successMsg: 'Đã gửi cảnh báo.',
    errorMsg: 'Gửi cảnh báo thất bại.'
  })
}

async function confirmStop() {
  showStopDialog.value = false
  await runAction({
    key: 'ATTEMPT_STOPPED',
    call: (id) => invalidateAttempt(id, stopReason.value),
    successMsg: 'Đã dừng bài thi.',
    errorMsg: 'Dừng thi thất bại.'
  })
}

async function confirmPause() {
  showPauseDialog.value = false
  await runAction({
    key: 'ATTEMPT_PAUSED',
    call: (id) => pauseAttempt(id, pauseReason.value),
    successMsg: 'Đã tạm dừng bài thi.',
    errorMsg: 'Tạm dừng thất bại.'
  })
}

async function confirmResume() {
  showResumeDialog.value = true
}

async function confirmResumeAction() {
  showResumeDialog.value = false
  await runAction({
    key: 'ATTEMPT_RESUMED',
    call: (id) => resumeAttempt(id, resumeMessage.value),
    successMsg: 'Đã cho phép tiếp tục.',
    errorMsg: 'Khôi phục thất bại.'
  })
}

// ── Realtime ──────────────────────────────────────────────────────────────────

function handleRealtimeMessage(payload) {
  const type = String(payload.type || '').toUpperCase()
  const meta = REALTIME_META[type]
  if (!meta) return
  realtimeFeed.value.unshift({
    id: `rt-${Date.now()}`,
    ...meta,
    riskScore: payload.riskScore,
    message: payload.message,
    timestamp: Date.now()
  })
  if (realtimeFeed.value.length > 50) realtimeFeed.value.length = 50

  // Update attempt data
  if (type === 'ATTEMPT_PAUSED' || type === 'ATTEMPT_RESUMED' || type === 'ATTEMPT_STOPPED') {
    attemptData.value = { ...attemptData.value, status: payload.status }
  }
  if (type === 'RISK_UPDATED' && payload.riskScore != null) {
    riskData.value = { ...riskData.value, riskScore: payload.riskScore }
  }
}

// ── Lifecycle ────────────────────────────────────────────────────────────────

watch(attemptId, (id) => {
  if (id) {
    void loadData()
    subscribeToAttempt(id)
  }
}, { immediate: true })

onMounted(() => {
  if (attemptId.value) {
    subscribeToAttempt(attemptId.value)
  }
})

onUnmounted(() => {
  if (attemptId.value) {
    unsubscribeFromAttempt(attemptId.value)
  }
})
</script>

<style scoped>
/* ── Root ──────────────────────────────────────────────────────────── */
.smd-root {
  font-family: var(--mon-font, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif);
  background: var(--mon-bg, #f8fafc);
  color: var(--mon-text, #0f172a);
  min-height: 100vh;
}

/* ── Loading ─────────────────────────────────────────────────────── */
.smd-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  min-height: 60vh;
  color: var(--mon-text-secondary, #64748b);
  font-size: 0.875rem;
}
.smd-loading__spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--mon-border, #e2e8f0);
  border-top-color: var(--mon-primary, #4f46e5);
  border-radius: 50%;
  animation: smd-spin 0.7s linear infinite;
}
@keyframes smd-spin { to { transform: rotate(360deg); } }

/* ── Error ───────────────────────────────────────────────────────── */
.smd-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  min-height: 60vh;
  color: var(--mon-danger, #ef4444);
  font-size: 0.875rem;
}
.smd-error__retry {
  padding: 0.5rem 1.25rem;
  border: 1.5px solid var(--mon-danger, #ef4444);
  border-radius: 8px;
  background: transparent;
  color: var(--mon-danger, #ef4444);
  font-size: 0.825rem;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s ease;
}
.smd-error__retry:hover { background: rgba(239,68,68,0.1); }

/* ── Header ──────────────────────────────────────────────────────── */
.smd-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.875rem 1.5rem;
  background: var(--mon-surface, #ffffff);
  border-bottom: 1px solid var(--mon-border, #e2e8f0);
  flex-wrap: wrap;
}
.smd-header__left { display: flex; align-items: center; gap: 0.75rem; }
.smd-header__right { display: flex; align-items: center; gap: 0.75rem; }
.smd-header__info { flex: 1; min-width: 0; }
.smd-header__name {
  font-size: 1rem;
  font-weight: 800;
  color: var(--mon-text, #0f172a);
  margin: 0 0 0.375rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.smd-header__meta { display: flex; gap: 0.5rem; flex-wrap: wrap; }
.smd-header__badges { display: flex; gap: 0.375rem; flex-wrap: wrap; }

.smd-header__avatar-wrap { flex-shrink: 0; }
.smd-header__avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  font-weight: 900;
  position: relative;
}
.smd-header__avatar-ring {
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  border: 3px solid;
}

/* ── Badge ──────────────────────────────────────────────────────── */
.smd-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.2rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.65rem;
  font-weight: 700;
}

/* ── Gauge ──────────────────────────────────────────────────────── */
.smd-gauge {
  position: relative;
  width: 80px;
  height: 80px;
}
.smd-gauge__svg { transform: rotate(-90deg); width: 100%; height: 100%; }
.smd-gauge__track { fill: none; stroke: var(--mon-border, #e2e8f0); stroke-width: 8; }
.smd-gauge__fill { fill: none; stroke-width: 8; stroke-linecap: round; transition: stroke-dasharray 0.6s ease; }
.smd-gauge__center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.smd-gauge__score { font-size: 1.125rem; font-weight: 900; line-height: 1; }
.smd-gauge__label { font-size: 0.6rem; color: var(--mon-text-secondary, #64748b); font-weight: 600; margin-top: 2px; }

/* ── Meta tag ───────────────────────────────────────────────────── */
.smd-meta-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.2rem 0.5rem;
  border-radius: 6px;
  background: var(--mon-surface-2, #f1f5f9);
  border: 1px solid var(--mon-border, #e2e8f0);
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--mon-text-secondary, #64748b);
}

/* ── Device chips ───────────────────────────────────────────────── */
.smd-device-chips { display: flex; gap: 0.375rem; }
.smd-device-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.68rem;
  font-weight: 700;
  background: var(--mon-surface-2, #f1f5f9);
  color: var(--mon-text-muted, #94a3b8);
  border: 1px solid var(--mon-border, #e2e8f0);
}
.smd-device-chip--on { background: rgba(16,185,129,0.1); color: #10b981; border-color: rgba(16,185,129,0.2); }

/* ── Recommend banner ───────────────────────────────────────────── */
.smd-recommend {
  display: flex;
  gap: 0.75rem;
  padding: 0.875rem 1.5rem;
  background: rgba(245,158,11,0.06);
  border-bottom: 1px solid rgba(245,158,11,0.15);
}
.smd-recommend__icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(245,158,11,0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--mon-warning, #f59e0b);
  flex-shrink: 0;
}
.smd-recommend__body { flex: 1; }
.smd-recommend__action { display: block; font-size: 0.825rem; font-weight: 800; margin-bottom: 0.375rem; }
.smd-recommend__reasons { display: flex; gap: 0.375rem; flex-wrap: wrap; }
.smd-reason-tag {
  display: inline-flex;
  padding: 0.2rem 0.625rem;
  border-radius: 9999px;
  font-size: 0.68rem;
  font-weight: 600;
  background: rgba(245,158,11,0.1);
  color: var(--mon-warning, #f59e0b);
  border: 1px solid rgba(245,158,11,0.2);
}

/* ── Tabs ───────────────────────────────────────────────────────── */
.smd-tabs {
  display: flex;
  gap: 0.25rem;
  padding: 0.625rem 1.5rem;
  background: var(--mon-surface, #ffffff);
  border-bottom: 1px solid var(--mon-border, #e2e8f0);
  overflow-x: auto;
}
.smd-tabs::-webkit-scrollbar { height: 0; }
.smd-tab {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.4rem 0.875rem;
  border-radius: 8px;
  border: 1.5px solid transparent;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--mon-text-secondary, #64748b);
  background: transparent;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
  font-family: inherit;
}
.smd-tab:hover { background: var(--mon-surface-2, #f1f5f9); color: var(--mon-text, #0f172a); }
.smd-tab--active { background: var(--mon-primary-soft, rgba(79,70,229,0.08)); border-color: rgba(79,70,229,0.2); color: var(--mon-primary, #4f46e5); }

/* ── Content ────────────────────────────────────────────────────── */
.smd-content { padding: 1.25rem 1.5rem; }
.smd-tab-pane { animation: smd-fade 0.2s ease; }
@keyframes smd-fade { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: none; } }

/* ── Overview grid ──────────────────────────────────────────────── */
.smd-overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}

/* ── Card ───────────────────────────────────────────────────────── */
.smd-card {
  background: var(--mon-surface, #ffffff);
  border: 1.5px solid var(--mon-border, #e2e8f0);
  border-radius: 16px;
  overflow: hidden;
}
.smd-card--warn { border-color: rgba(245,158,11,0.2); }
.smd-card__header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.875rem 1rem;
  border-bottom: 1px solid var(--mon-border, #e2e8f0);
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--mon-text, #0f172a);
  background: var(--mon-surface-2, #f1f5f9);
}
.smd-card__badge {
  margin-left: auto;
  padding: 0.05rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.65rem;
  background: rgba(0,0,0,0.06);
  color: var(--mon-text-secondary, #64748b);
  font-weight: 800;
}
.smd-card__badge--warn { background: rgba(245,158,11,0.1); color: var(--mon-warning, #f59e0b); }
.smd-card__filter-select {
  margin-left: auto;
  padding: 0.2rem 0.5rem;
  border: 1.5px solid var(--mon-border, #e2e8f0);
  border-radius: 6px;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--mon-text-secondary, #64748b);
  background: var(--mon-surface, #ffffff);
  outline: none;
  cursor: pointer;
}
.smd-card__body { padding: 1rem; }
.smd-card__body--tight { padding: 0.75rem 1rem; }
.smd-card__body--controls { display: flex; flex-direction: column; gap: 0.75rem; }

/* ── Breakdown ──────────────────────────────────────────────────── */
.smd-empty-sm {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1.5rem;
  color: var(--mon-text-muted, #94a3b8);
  font-size: 0.825rem;
  text-align: center;
}
.smd-breakdown-list { display: flex; flex-direction: column; gap: 0.625rem; }
.smd-breakdown-row { display: flex; align-items: center; gap: 0.625rem; }
.smd-breakdown-row__label { font-size: 0.78rem; color: var(--mon-text-secondary, #64748b); font-weight: 600; width: 120px; flex-shrink: 0; }
.smd-breakdown-row__bar { flex: 1; height: 6px; background: var(--mon-border, #e2e8f0); border-radius: 9999px; overflow: hidden; }
.smd-breakdown-row__fill { height: 100%; border-radius: 9999px; transition: width 0.4s ease; }
.smd-breakdown-row__score { font-size: 0.78rem; font-weight: 800; width: 30px; text-align: right; font-variant-numeric: tabular-nums; }

/* ── Progress ───────────────────────────────────────────────────── */
.smd-progress-block { display: flex; align-items: baseline; gap: 0.5rem; margin-bottom: 0.75rem; }
.smd-progress-block__val { font-size: 1.5rem; font-weight: 900; color: var(--mon-text, #0f172a); font-variant-numeric: tabular-nums; }
.smd-progress-block__label { font-size: 0.78rem; color: var(--mon-text-secondary, #64748b); font-weight: 600; }
.smd-progress-bar { height: 6px; background: var(--mon-border, #e2e8f0); border-radius: 9999px; overflow: hidden; margin-bottom: 0.5rem; }
.smd-progress-fill { height: 100%; border-radius: 9999px; transition: width 0.4s ease; }
.smd-progress-row { display: flex; justify-content: space-between; font-size: 0.72rem; color: var(--mon-text-secondary, #64748b); font-weight: 600; }
.smd-remaining { display: inline-flex; align-items: center; gap: 0.25rem; color: var(--mon-warning, #f59e0b); }

/* ── Device rows ────────────────────────────────────────────────── */
.smd-device-row { display: flex; align-items: center; gap: 0.5rem; padding: 0.5rem 0; font-size: 0.8rem; border-bottom: 1px solid var(--mon-border, #e2e8f0); }
.smd-device-row:last-child { border-bottom: none; }
.smd-device-row__label { flex: 1; color: var(--mon-text-secondary, #64748b); font-weight: 600; }
.smd-device-row__val { font-weight: 700; color: var(--mon-text, #0f172a); }

/* ── Pattern ────────────────────────────────────────────────────── */
.smd-pattern { padding: 0.625rem 0; border-bottom: 1px solid var(--mon-border, #e2e8f0); }
.smd-pattern:last-child { border-bottom: none; }
.smd-pattern__head { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.25rem; }
.smd-pattern__title { font-size: 0.8rem; font-weight: 700; color: var(--mon-text, #0f172a); flex: 1; }
.smd-pattern__level { font-size: 0.6rem; font-weight: 800; padding: 0.1rem 0.4rem; border-radius: 9999px; color: white; }
.smd-pattern__desc { font-size: 0.72rem; color: var(--mon-text-secondary, #64748b); margin: 0; }

/* ── Timeline ───────────────────────────────────────────────────── */
.smd-timeline { display: flex; flex-direction: column; }
.smd-tl-item { display: flex; gap: 0.75rem; padding: 0.625rem 0; position: relative; }
.smd-tl-item__line {
  position: absolute;
  left: 12px;
  top: 32px;
  bottom: 0;
  width: 1.5px;
  background: var(--mon-border, #e2e8f0);
}
.smd-tl-item__dot {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}
.smd-tl-item__content { flex: 1; min-width: 0; }
.smd-tl-item__row { display: flex; justify-content: space-between; align-items: center; gap: 0.5rem; margin-bottom: 0.125rem; }
.smd-tl-item__type { font-size: 0.8rem; font-weight: 700; }
.smd-tl-item__time { font-size: 0.72rem; color: var(--mon-text-muted, #94a3b8); font-variant-numeric: tabular-nums; white-space: nowrap; }
.smd-tl-item__details { font-size: 0.75rem; color: var(--mon-text-secondary, #64748b); margin: 0.25rem 0 0; }
.smd-tl-item__sev {
  display: inline-flex;
  font-size: 0.6rem;
  font-weight: 800;
  padding: 0.1rem 0.4rem;
  border-radius: 9999px;
  margin-top: 0.25rem;
}
.smd-tl-item__sev--high { background: rgba(220,38,38,0.1); color: #dc2626; }
.smd-tl-item__sev--medium { background: rgba(245,158,11,0.1); color: #f59e0b; }
.smd-tl-item__sev--low { background: rgba(22,163,74,0.1); color: #16a34a; }

/* ── Realtime feed ──────────────────────────────────────────────── */
.smd-live-dot { width: 8px; height: 8px; border-radius: 50%; }
.smd-live-dot--on { background: #10b981; animation: smd-pulse 1.5s ease-in-out infinite; }
.smd-live-dot--off { background: var(--mon-text-muted, #94a3b8); }
@keyframes smd-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }

.smd-realtime-list { display: flex; flex-direction: column; gap: 0.5rem; }
.smd-realtime-item {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
  padding: 0.625rem 0.75rem;
  border-radius: 10px;
  background: var(--mon-surface-2, #f1f5f9);
  border-left: 3px solid;
}
.smd-realtime-item--warn { border-left-color: var(--mon-warning, #f59e0b); background: rgba(245,158,11,0.06); }
.smd-realtime-item--success { border-left-color: var(--mon-success, #10b981); background: rgba(16,185,129,0.06); }
.smd-realtime-item--danger { border-left-color: var(--mon-danger, #ef4444); background: rgba(239,68,68,0.06); }
.smd-realtime-item__icon { width: 28px; height: 28px; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; background: rgba(0,0,0,0.05); color: var(--mon-text-secondary, #64748b); }
.smd-realtime-item__body { flex: 1; min-width: 0; }
.smd-realtime-item__label { display: block; font-size: 0.8rem; font-weight: 700; color: var(--mon-text, #0f172a); margin-bottom: 0.125rem; }
.smd-realtime-item__score { display: inline-block; font-size: 0.72rem; font-weight: 900; padding: 0.05rem 0.4rem; border-radius: 9999px; background: rgba(0,0,0,0.06); margin-left: 0.375rem; color: var(--mon-danger, #ef4444); }
.smd-realtime-item__msg { font-size: 0.72rem; color: var(--mon-text-secondary, #64748b); margin: 0; }
.smd-realtime-item__time { font-size: 0.68rem; color: var(--mon-text-muted, #94a3b8); font-variant-numeric: tabular-nums; white-space: nowrap; }

/* ── Action history ─────────────────────────────────────────────── */
.smd-action-list { display: flex; flex-direction: column; }
.smd-action-item { display: flex; align-items: flex-start; gap: 0.75rem; padding: 0.75rem 0; border-bottom: 1px solid var(--mon-border, #e2e8f0); }
.smd-action-item:last-child { border-bottom: none; }
.smd-action-item__icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.smd-action-item__icon--warn { background: rgba(245,158,11,0.1); color: #f59e0b; }
.smd-action-item__icon--success { background: rgba(16,185,129,0.1); color: #10b981; }
.smd-action-item__icon--danger { background: rgba(239,68,68,0.1); color: #ef4444; }
.smd-action-item__icon--neutral { background: rgba(148,163,184,0.1); color: #94a3b8; }
.smd-action-item__body { flex: 1; }
.smd-action-item__label { display: block; font-size: 0.825rem; font-weight: 700; color: var(--mon-text, #0f172a); margin-bottom: 0.125rem; }
.smd-action-item__msg { font-size: 0.72rem; color: var(--mon-text-secondary, #64748b); margin: 0; }
.smd-action-item__time { font-size: 0.68rem; color: var(--mon-text-muted, #94a3b8); font-variant-numeric: tabular-nums; white-space: nowrap; }

/* ── Control buttons ────────────────────────────────────────────── */
.smd-ctrl-btn {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border-radius: 12px;
  border: 1.5px solid;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: left;
  width: 100%;
  background: transparent;
  font-family: inherit;
}
.smd-ctrl-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.smd-ctrl-btn__label { display: block; font-weight: 700; margin-bottom: 0.125rem; }
.smd-ctrl-btn__desc { font-size: 0.72rem; font-weight: 400; opacity: 0.75; }
.smd-ctrl-btn--warn { border-color: rgba(245,158,11,0.25); color: #f59e0b; }
.smd-ctrl-btn--warn:hover:not(:disabled) { background: rgba(245,158,11,0.06); }
.smd-ctrl-btn--pause { border-color: rgba(79,70,229,0.2); color: var(--mon-primary, #4f46e5); }
.smd-ctrl-btn--pause:hover:not(:disabled) { background: rgba(79,70,229,0.06); }
.smd-ctrl-btn--success { border-color: rgba(16,185,129,0.25); color: #10b981; }
.smd-ctrl-btn--success:hover:not(:disabled) { background: rgba(16,185,129,0.06); }
.smd-ctrl-btn--danger { border-color: rgba(239,68,68,0.25); color: #ef4444; }
.smd-ctrl-btn--danger:hover:not(:disabled) { background: rgba(239,68,68,0.06); }

/* ── Notes ──────────────────────────────────────────────────────── */
.smd-note-input {
  width: 100%;
  padding: 0.75rem;
  border: 1.5px solid var(--mon-border, #e2e8f0);
  border-radius: 12px;
  font-size: 0.85rem;
  color: var(--mon-text, #0f172a);
  background: var(--mon-surface-2, #f1f5f9);
  resize: vertical;
  outline: none;
  font-family: inherit;
  margin-bottom: 0.75rem;
  transition: border-color 0.15s ease;
  box-sizing: border-box;
}
.smd-note-input:focus { border-color: var(--mon-primary, #4f46e5); }
.smd-note-input::placeholder { color: var(--mon-text-muted, #94a3b8); }
.smd-note-submit {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 12px;
  border: none;
  background: var(--mon-primary, #4f46e5);
  color: white;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s ease;
  font-family: inherit;
}
.smd-note-submit:hover:not(:disabled) { background: var(--mon-primary-hover, #4338ca); }
.smd-note-submit:disabled { opacity: 0.5; cursor: not-allowed; }

.smd-notes-list { display: flex; flex-direction: column; }
.smd-note-item { padding: 0.75rem 0; border-bottom: 1px solid var(--mon-border, #e2e8f0); }
.smd-note-item:last-child { border-bottom: none; }
.smd-note-item__body { font-size: 0.825rem; color: var(--mon-text, #0f172a); margin-bottom: 0.375rem; }
.smd-note-item__meta { display: flex; gap: 0.75rem; }
.smd-note-item__author { font-size: 0.72rem; font-weight: 600; color: var(--mon-primary, #4f46e5); }
.smd-note-item__time { font-size: 0.72rem; color: var(--mon-text-muted, #94a3b8); font-variant-numeric: tabular-nums; }

/* ── Dialog buttons ────────────────────────────────────────────── */
:deep(.p-dialog-header),
:deep(.p-dialog-content),
:deep(.p-dialog-footer) {
  font-family: inherit;
}
.smd-dialog__cancel {
  padding: 0.5rem 1rem;
  border-radius: 8px;
  border: 1.5px solid var(--mon-border, #e2e8f0);
  background: var(--mon-surface, #ffffff);
  color: var(--mon-text-secondary, #64748b);
  font-size: 0.825rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
}
.smd-dialog__cancel:hover { border-color: #94a3b8; color: var(--mon-text, #0f172a); }

.smd-dialog__confirm {
  padding: 0.5rem 1rem;
  border-radius: 8px;
  border: none;
  font-size: 0.825rem;
  font-weight: 700;
  cursor: pointer;
  transition: filter 0.15s ease;
  font-family: inherit;
}
.smd-dialog__confirm:disabled { opacity: 0.5; cursor: not-allowed; }
.smd-dialog__confirm--warn { background: #f59e0b; color: #0f172a; }
.smd-dialog__confirm--warn:hover:not(:disabled) { filter: brightness(1.05); }
.smd-dialog__confirm--danger { background: #dc2626; color: white; }
.smd-dialog__confirm--danger:hover:not(:disabled) { filter: brightness(1.05); }
.smd-dialog__confirm--pause { background: #4f46e5; color: white; }
.smd-dialog__confirm--pause:hover:not(:disabled) { filter: brightness(1.05); }
.smd-dialog__confirm--success { background: #16a34a; color: white; }
.smd-dialog__confirm--success:hover:not(:disabled) { filter: brightness(1.05); }

/* ── Responsive ─────────────────────────────────────────────────── */
@media (max-width: 768px) {
  .smd-header { flex-direction: column; align-items: flex-start; }
  .smd-header__right { width: 100%; }
  .smd-overview-grid { grid-template-columns: 1fr; }
  .smd-tabs { padding: 0.5rem 1rem; }
  .smd-content { padding: 0.75rem 1rem; }
}
</style>
