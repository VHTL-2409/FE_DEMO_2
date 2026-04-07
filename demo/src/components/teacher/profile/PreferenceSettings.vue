<template>
  <div class="ps">
    <!-- Card header -->
    <div class="ps__header">
      <div class="ps__header-icon">
        <LucideIcon name="settings" />
      </div>
      <div>
        <h3 class="ps__title">Tùy chọn hệ thống</h3>
        <p class="ps__subtitle">Ngôn ngữ, giao diện, hiển thị</p>
      </div>
    </div>

    <!-- Content -->
    <div class="ps__content">

      <!-- Appearance -->
      <div class="ps__section">
        <div class="ps__section-header">
          <LucideIcon name="palette" />
          <h4 class="ps__section-title">Giao diện</h4>
        </div>
        <div class="ps__options">
          <div class="ps__option-row">
            <div class="ps__option-info">
              <span class="ps__option-label">Chế độ tối</span>
              <span class="ps__option-desc">Bật giao diện tối toàn ứng dụng</span>
            </div>
            <button
              type="button"
              class="ps__toggle-btn"
              :class="{ 'ps__toggle-btn--on': preferences.darkMode }"
              role="switch"
              :aria-checked="preferences.darkMode"
              @click="togglePref('darkMode')"
            >
              <span class="ps__toggle-thumb" />
            </button>
          </div>

          <div class="ps__option-row">
            <div class="ps__option-info">
              <span class="ps__option-label">Compact mode</span>
              <span class="ps__option-desc">Giảm khoảng cách, hiển thị nhiều nội dung hơn</span>
            </div>
            <button
              type="button"
              class="ps__toggle-btn"
              :class="{ 'ps__toggle-btn--on': preferences.compactMode }"
              role="switch"
              :aria-checked="preferences.compactMode"
              @click="togglePref('compactMode')"
            >
              <span class="ps__toggle-thumb" />
            </button>
          </div>
        </div>
      </div>

      <!-- Display -->
      <div class="ps__section">
        <div class="ps__section-header">
          <LucideIcon name="visibility" />
          <h4 class="ps__section-title">Hiển thị</h4>
        </div>
        <div class="ps__options">
          <div class="ps__option-row">
            <div class="ps__option-info">
              <span class="ps__option-label">Table per page</span>
              <span class="ps__option-desc">Số dòng mặc định trên mỗi trang bảng</span>
            </div>
            <select
              :value="preferences.tablePageSize"
              class="ps__select"
              @change="setPref('tablePageSize', Number($event.target.value))"
            >
              <option :value="10">10 dòng</option>
              <option :value="15">15 dòng</option>
              <option :value="20">20 dòng</option>
              <option :value="25">25 dòng</option>
              <option :value="50">50 dòng</option>
            </select>
          </div>

          <div class="ps__option-row">
            <div class="ps__option-info">
              <span class="ps__option-label">Chart animations</span>
              <span class="ps__option-desc">Hiệu ứng chuyển động trên biểu đồ</span>
            </div>
            <button
              type="button"
              class="ps__toggle-btn"
              :class="{ 'ps__toggle-btn--on': preferences.chartAnimations }"
              role="switch"
              :aria-checked="preferences.chartAnimations"
              @click="togglePref('chartAnimations')"
            >
              <span class="ps__toggle-thumb" />
            </button>
          </div>
        </div>
      </div>

      <!-- Language -->
      <div class="ps__section">
        <div class="ps__section-header">
          <LucideIcon name="language" />
          <h4 class="ps__section-title">Ngôn ngữ & Vùng</h4>
        </div>
        <div class="ps__options">
          <div class="ps__option-row">
            <div class="ps__option-info">
              <span class="ps__option-label">Ngôn ngữ giao diện</span>
              <span class="ps__option-desc">Ngôn ngữ hiển thị trong ứng dụng</span>
            </div>
            <select
              :value="preferences.language"
              class="ps__select"
              @change="setPref('language', $event.target.value)"
            >
              <option value="vi">Tiếng Việt</option>
              <option value="en">English</option>
            </select>
          </div>

          <div class="ps__option-row">
            <div class="ps__option-info">
              <span class="ps__option-label">Định dạng ngày</span>
              <span class="ps__option-desc">Cách hiển thị ngày tháng</span>
            </div>
            <select
              :value="preferences.dateFormat"
              class="ps__select"
              @change="setPref('dateFormat', $event.target.value)"
            >
              <option value="DD/MM/YYYY">DD/MM/YYYY</option>
              <option value="MM/DD/YYYY">MM/DD/YYYY</option>
              <option value="YYYY-MM-DD">YYYY-MM-DD</option>
            </select>
          </div>
        </div>
      </div>

      <!-- Exam defaults -->
      <div class="ps__section">
        <div class="ps__section-header">
          <LucideIcon name="quiz" />
          <h4 class="ps__section-title">Mặc định kỳ thi</h4>
        </div>
        <div class="ps__options">
          <div class="ps__option-row">
            <div class="ps__option-info">
              <span class="ps__option-label">Điểm đạt</span>
              <span class="ps__option-desc">Ngưỡng điểm đạt mặc định cho bài thi mới</span>
            </div>
            <select
              :value="preferences.defaultPassingScore"
              class="ps__select ps__select--sm"
              @change="setPref('defaultPassingScore', Number($event.target.value))"
            >
              <option :value="5">5.0 / 10</option>
              <option :value="6">6.0 / 10</option>
              <option :value="7">7.0 / 10</option>
              <option :value="8">8.0 / 10</option>
            </select>
          </div>

          <div class="ps__option-row">
            <div class="ps__option-info">
              <span class="ps__option-label">Thông báo vi phạm tự động</span>
              <span class="ps__option-desc">Tự động gửi cảnh báo khi phát hiện vi phạm</span>
            </div>
            <button
              type="button"
              class="ps__toggle-btn"
              :class="{ 'ps__toggle-btn--on': preferences.autoAlertViolation }"
              role="switch"
              :aria-checked="preferences.autoAlertViolation"
              @click="togglePref('autoAlertViolation')"
            >
              <span class="ps__toggle-thumb" />
            </button>
          </div>
        </div>
      </div>

      <!-- Danger zone -->
      <div class="ps__section ps__section--danger">
        <div class="ps__section-header">
          <LucideIcon name="delete_forever" />
          <h4 class="ps__section-title">Vùng nguy hiểm</h4>
        </div>
        <div class="ps__options">
          <div class="ps__option-row ps__option-row--danger">
            <div class="ps__option-info">
              <span class="ps__option-label">Xóa tài khoản</span>
              <span class="ps__option-desc">Xóa vĩnh viễn tài khoản và toàn bộ dữ liệu</span>
            </div>
            <button type="button" class="ps__danger-btn" @click="$emit('delete-account')">
              Xóa tài khoản
            </button>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'

const emit = defineEmits(['update', 'delete-account'])

const PREF_STORAGE_KEY = 'teacher_preferences'

const loadPrefs = () => {
  try {
    const stored = localStorage.getItem(PREF_STORAGE_KEY)
    if (stored) return JSON.parse(stored)
  } catch {}
  return null
}

const defaults = {
  darkMode: false,
  compactMode: false,
  tablePageSize: 15,
  chartAnimations: true,
  language: 'vi',
  dateFormat: 'DD/MM/YYYY',
  defaultPassingScore: 5,
  autoAlertViolation: true
}

const preferences = reactive({ ...defaults, ...(loadPrefs() || {}) })

const persist = () => {
  try {
    localStorage.setItem(PREF_STORAGE_KEY, JSON.stringify(preferences))
  } catch {}
  emit('update', { ...preferences })
}

const togglePref = (key) => {
  preferences[key] = !preferences[key]
  persist()
}

const setPref = (key, value) => {
  preferences[key] = value
  persist()
}

defineExpose({ preferences })
</script>


<style scoped>
.ps {
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  overflow: hidden;
}

.dark .ps {
  border-color: var(--ds-border-strong);
}

/* Header */
.ps__header {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 1.125rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
  background: var(--ds-gray-50);
}

.dark .ps__header {
  border-bottom-color: var(--ds-border-strong);
  background: var(--ds-gray-800);
}

.ps__header-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ds-radius-lg);
  background: var(--ds-gray-100);
  color: var(--ds-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dark .ps__header-icon {
  background: var(--ds-gray-700);
  color: #94a3b8;
}


.ps__title {
  font-family: var(--ds-font-display);
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--ds-text);
  margin: 0;
}

.dark .ps__title {
  color: #f1f5f9;
}

.ps__subtitle {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  margin: 0.2rem 0 0;
  font-weight: 500;
}

/* Content */
.ps__content {
  display: flex;
  flex-direction: column;
}

/* Section */
.ps__section {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--ds-border);
}

.dark .ps__section {
  border-bottom-color: var(--ds-border-strong);
}

.ps__section:last-child {
  border-bottom: none;
}

.ps__section--danger {
  background: rgba(220, 38, 38, 0.02);
}

.dark .ps__section--danger {
  background: rgba(220, 38, 38, 0.05);
}

.ps__section-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.875rem;
}


.ps__section-title {
  font-size: 0.8rem;
  font-weight: 800;
  color: var(--ds-text-muted);
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.dark .ps__section-title {
  color: #94a3b8;
}

/* Options */
.ps__options {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.ps__option-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.625rem 0.75rem;
  border-radius: var(--ds-radius-xl);
  background: var(--ds-gray-50);
  transition: background 0.1s ease;
}

.dark .ps__option-row {
  background: var(--ds-gray-800);
}

.ps__option-row:hover {
  background: var(--ds-gray-100);
}

.dark .ps__option-row:hover {
  background: var(--ds-gray-700);
}

.ps__option-row--danger {
  background: rgba(220, 38, 38, 0.04);
}

.dark .ps__option-row--danger {
  background: rgba(220, 38, 38, 0.08);
}

.ps__option-row--danger:hover {
  background: rgba(220, 38, 38, 0.06);
}

.dark .ps__option-row--danger:hover {
  background: rgba(220, 38, 38, 0.1);
}

.ps__option-info {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  min-width: 0;
}

.ps__option-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ds-text);
}

.dark .ps__option-label {
  color: #f1f5f9;
}

.ps__option-desc {
  font-size: 0.7rem;
  color: var(--ds-text-muted);
  font-weight: 500;
}

/* Toggle switch */
.ps__toggle-btn {
  width: 44px;
  height: 24px;
  border-radius: 12px;
  background: var(--ds-gray-300);
  border: none;
  cursor: pointer;
  position: relative;
  transition: background 0.2s ease;
  flex-shrink: 0;
}

.dark .ps__toggle-btn {
  background: var(--ds-gray-600);
}

.ps__toggle-btn--on {
  background: var(--ds-primary);
}

.ps__toggle-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s ease;
}

.ps__toggle-btn--on .ps__toggle-thumb {
  transform: translateX(20px);
}

/* Select */
.ps__select {
  padding: 0.4rem 2rem 0.4rem 0.75rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-xl);
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ds-text-secondary);
  outline: none;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2364748b' d='M2 4l4 4 4-4'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.625rem center;
  background-color: var(--ds-surface);
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  min-width: 140px;
  font-family: inherit;
}

.dark .ps__select {
  background-color: var(--ds-gray-700);
  border-color: var(--ds-border-strong);
  color: #94a3b8;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2394a3b8' d='M2 4l4 4 4-4'/%3E%3C/svg%3E");
}

.ps__select:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}

.ps__select--sm {
  min-width: 100px;
}

/* Danger button */
.ps__danger-btn {
  padding: 0.4rem 1rem;
  border-radius: var(--ds-radius-xl);
  border: 1.5px solid rgba(220, 38, 38, 0.3);
  background: transparent;
  color: var(--ds-danger);
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.15s ease, background-color 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  font-family: inherit;
  flex-shrink: 0;
}

.ps__danger-btn:hover {
  background: var(--ds-danger);
  color: white;
  border-color: var(--ds-danger);
}
</style>