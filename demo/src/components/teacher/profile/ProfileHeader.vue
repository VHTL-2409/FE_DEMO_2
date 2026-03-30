<template>
  <div class="ph">
    <!-- Avatar section -->
    <div class="ph__avatar-section">
      <div class="ph__avatar-wrap">
        <div class="ph__avatar" :class="{ 'ph__avatar--uploading': isUploading }">
          <img
            v-if="avatarUrl && avatarUrl !== 'Chưa điền'"
            :src="avatarUrl"
            :alt="displayName"
            class="ph__avatar-img"
          />
          <div v-else class="ph__avatar-fallback">
            {{ avatarInitial }}
          </div>
        </div>

        <!-- Upload overlay -->
        <label class="ph__avatar-upload" :class="{ 'ph__avatar-upload--disabled': isUploading }">
          <input
            type="file"
            class="sr-only"
            accept="image/png,image/jpeg,image/webp"
            :disabled="isUploading"
            @change="handleAvatarChange"
          />
          <LucideIcon :name="isUploading ? 'hourglass_empty' : 'photo_camera'" />
          <span>{{ isUploading ? 'Đang tải...' : 'Đổi ảnh' }}</span>
        </label>
      </div>

      <!-- Status badge -->
      <div v-if="accountStatus" class="ph__status" :class="statusClass">
        <LucideIcon :name="statusIcon" />
        {{ statusLabel }}
      </div>
    </div>

    <!-- Info section -->
    <div class="ph__info">
      <div class="ph__title-group">
        <h1 class="ph__name">{{ displayName }}</h1>
        <div class="ph__role-badge">
          <LucideIcon name="school" />
          {{ roleLabel }}
        </div>
      </div>

      <p class="ph__subtitle">{{ subtitle }}</p>

      <!-- Meta info -->
      <div class="ph__meta">
        <div class="ph__meta-item">
          <LucideIcon name="badge" />
          <span>{{ username }}</span>
        </div>
        <div class="ph__meta-item">
          <LucideIcon name="mail" />
          <span>{{ email }}</span>
        </div>
        <div v-if="phone && phone !== 'Chưa điền'" class="ph__meta-item">
          <LucideIcon name="phone" />
          <span>{{ phone }}</span>
        </div>
        <div v-if="teacherCode" class="ph__meta-item">
          <LucideIcon name="tag" />
          <span>ID: {{ teacherCode }}</span>
        </div>
      </div>
    </div>

    <!-- Actions -->
    <div class="ph__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  avatarUrl: { type: String, default: '' },
  displayName: { type: String, default: 'Giáo viên' },
  username: { type: String, default: '' },
  email: { type: String, default: '' },
  phone: { type: String, default: '' },
  teacherCode: { type: String, default: '' },
  subtitle: { type: String, default: 'Hồ sơ giáo viên và hoạt động thi cử' },
  role: { type: String, default: 'TEACHER' },
  accountStatus: { type: String, default: '' },
  isUploading: { type: Boolean, default: false }
})

const emit = defineEmits(['avatar-change'])

const avatarInitial = computed(() => {
  const name = String(props.displayName).trim()
  return name ? name.charAt(0).toUpperCase() : 'G'
})

const roleLabel = computed(() => {
  const r = String(props.role || '').toUpperCase()
  if (r === 'ADMIN' || r === 'ROLE_ADMIN') return 'Quản trị viên'
  if (r === 'TEACHER' || r === 'ROLE_TEACHER') return 'Giáo viên'
  return 'Người dùng'
})

const statusClass = computed(() => {
  const s = String(props.accountStatus || '').toUpperCase()
  if (s === 'ACTIVE') return 'ph__status--active'
  if (s === 'PENDING') return 'ph__status--pending'
  if (s === 'SUSPENDED' || s === 'BANNED') return 'ph__status--suspended'
  return ''
})

const statusIcon = computed(() => {
  const s = String(props.accountStatus || '').toUpperCase()
  if (s === 'ACTIVE') return 'verified_user'
  if (s === 'PENDING') return 'pending'
  if (s === 'SUSPENDED' || s === 'BANNED') return 'block'
  return ''
})

const statusLabel = computed(() => {
  const s = String(props.accountStatus || '').toUpperCase()
  if (s === 'ACTIVE') return 'Đã xác minh'
  if (s === 'PENDING') return 'Chờ xác minh'
  if (s === 'SUSPENDED') return 'Bị tạm ngưng'
  if (s === 'BANNED') return 'Bị cấm'
  return ''
})

const handleAvatarChange = (event) => {
  const file = event.target?.files?.[0]
  if (file) {
    emit('avatar-change', file)
  }
  event.target.value = ''
}
</script>


<style scoped>
.ph {
  display: flex;
  align-items: flex-start;
  gap: 1.5rem;
  padding: 1.5rem;
  background: var(--ds-surface);
  border: 1.5px solid var(--ds-border);
  border-radius: var(--ds-radius-2xl);
  transition: border-color 0.15s ease;
}

.dark .ph {
  border-color: var(--ds-border-strong);
}

/* Avatar section */
.ph__avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.ph__avatar-wrap {
  position: relative;
  display: inline-flex;
}

.ph__avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid var(--ds-primary-border);
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.15);
  transition: all 0.2s ease;
  background: var(--ds-primary);
}

.ph__avatar--uploading {
  opacity: 0.7;
}

.ph__avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ph__avatar-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  font-weight: 900;
  color: white;
  background: linear-gradient(135deg, var(--ds-primary) 0%, var(--ds-primary-hover, #4338ca) 100%);
}

.ph__avatar-upload {
  position: absolute;
  bottom: -4px;
  right: -4px;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.3rem 0.625rem;
  background: var(--ds-primary);
  color: white;
  border-radius: var(--ds-radius-full);
  font-size: 0.65rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.3);
  transition: all 0.15s ease;
  white-space: nowrap;
}

.ph__avatar-upload:hover:not(.ph__avatar-upload--disabled) {
  background: var(--ds-primary-hover, #4338ca);
  transform: scale(1.05);
}

.ph__avatar-upload--disabled {
  opacity: 0.6;
  cursor: not-allowed;
}


/* Status badge */
.ph__status {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.25rem 0.75rem;
  border-radius: var(--ds-radius-full);
  font-size: 0.7rem;
  font-weight: 700;
}


.ph__status--active {
  background: var(--ds-success-soft);
  color: var(--ds-success);
}

.ph__status--pending {
  background: rgba(234, 179, 8, 0.1);
  color: var(--ds-warning);
}

.ph__status--suspended {
  background: var(--ds-danger-soft);
  color: var(--ds-danger);
}

/* Info */
.ph__info {
  flex: 1;
  min-width: 0;
}

.ph__title-group {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.ph__name {
  font-family: var(--ds-font-display);
  font-size: 1.5rem;
  font-weight: 900;
  color: var(--ds-text);
  margin: 0;
  line-height: 1.2;
}

.dark .ph__name {
  color: #f1f5f9;
}

.ph__role-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.25rem 0.75rem;
  border-radius: var(--ds-radius-full);
  background: var(--ds-primary-soft);
  color: var(--ds-primary);
  font-size: 0.75rem;
  font-weight: 700;
}


.ph__subtitle {
  font-size: 0.875rem;
  color: var(--ds-text-muted);
  margin: 0.375rem 0 0;
  font-weight: 500;
}

/* Meta */
.ph__meta {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 0.75rem;
  flex-wrap: wrap;
}

.ph__meta-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8rem;
  color: var(--ds-text-secondary);
  font-weight: 500;
}


/* Actions */
.ph__actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex-shrink: 0;
}

/* Responsive */
@media (max-width: 640px) {
  .ph {
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 1rem;
    padding: 1.25rem;
  }

  .ph__meta {
    justify-content: center;
  }

  .ph__title-group {
    justify-content: center;
  }

  .ph__actions {
    width: 100%;
    flex-direction: row;
    justify-content: center;
    flex-wrap: wrap;
  }
}
</style>
