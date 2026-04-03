<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <ProfileLayout ref="layoutRef">

      <!-- Header slot -->
      <template #header>
        <ProfileHeader
          :avatar-url="profileAvatarUrl"
          :display-name="profileName"
          :username="profileUsername"
          :email="profileEmail"
          :phone="profilePhone"
          :teacher-code="profileId"
          subtitle="Hồ sơ giáo viên và hoạt động giảng dạy"
          role="TEACHER"
          :is-uploading="isUploadingAvatar"
          @avatar-change="handleAvatarChange"
        >
          <template #actions>
            <button
              type="button"
              class="inline-flex items-center gap-2 rounded-[var(--ds-radius-xl)] border border-[var(--ds-border)] bg-[var(--ds-surface)] px-4 py-2 text-sm font-semibold text-[var(--ds-text-secondary)] transition-colors hover:bg-[var(--ds-gray-100)]"
              @click="goToDashboard"
            >
              <LucideIcon name="home" size="18" />
              Trang chủ
            </button>
          </template>
        </ProfileHeader>
      </template>

      <!-- Tab: Personal Info -->
      <template #tab-personal>
        <PersonalInfoForm
          ref="personalFormRef"
          :profile="profile"
          @save="handlePersonalSave"
        />
      </template>

      <!-- Tab: Professional Info -->
      <template #tab-professional>
        <ProfessionalInfoForm
          ref="professionalFormRef"
          :profile="profile"
          @save="handleProfessionalSave"
        />
      </template>

      <!-- Tab: Security -->
      <template #tab-security>
        <SecuritySettings
          ref="securityRef"
          @change-password="handleChangePassword"
        />
      </template>

      <!-- Tab: Notifications -->
      <template #tab-notifications>
        <NotificationSettings @update="handleNotificationUpdate" />
      </template>

      <!-- Tab: Preferences -->
      <template #tab-preferences>
        <PreferenceSettings
          @update="handlePreferenceUpdate"
          @delete-account="handleDeleteAccount"
        />
      </template>

    </ProfileLayout>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword, fetchTeacherProfile, updateSharedProfile, uploadAvatar } from '../../services/authService'
import { useToast } from '../../composables/useToast'

// New profile components
import ProfileLayout from './profile/ProfileLayout.vue'
import ProfileHeader from './profile/ProfileHeader.vue'
import PersonalInfoForm from './profile/PersonalInfoForm.vue'
import ProfessionalInfoForm from './profile/ProfessionalInfoForm.vue'
import SecuritySettings from './profile/SecuritySettings.vue'
import NotificationSettings from './profile/NotificationSettings.vue'
import PreferenceSettings from './profile/PreferenceSettings.vue'

const router = useRouter()
const toast = useToast()
const layoutRef = ref(null)
const personalFormRef = ref(null)
const professionalFormRef = ref(null)
const securityRef = ref(null)

const profile = ref(null)
const isUploadingAvatar = ref(false)

// Computed profile fields
const profileName = computed(() => profile.value?.displayName || profile.value?.username || 'Giáo viên')
const profileId = computed(() => String(profile.value?.id || ''))
const profileUsername = computed(() => profile.value?.username || '—')
const profileEmail = computed(() => profile.value?.email || '—')
const profilePhone = computed(() => profile.value?.phone || '—')
const profileAvatarUrl = computed(() => profile.value?.avatarUrl)

// Load profile
const loadProfile = async () => {
  try {
    profile.value = await fetchTeacherProfile()
  } catch {
    profile.value = null
    toast.error('Không thể tải hồ sơ.')
  }
}

// Avatar upload
const handleAvatarChange = async (file) => {
  isUploadingAvatar.value = true
  try {
    const result = await uploadAvatar(file)
    if (result?.avatarUrl) {
      profile.value = { ...profile.value, avatarUrl: result.avatarUrl }
      toast.success('Cập nhật ảnh đại diện thành công.')
    }
  } catch {
    toast.error('Không thể tải ảnh đại diện.')
  } finally {
    isUploadingAvatar.value = false
  }
}

// Personal info save
const handlePersonalSave = async (data) => {
  try {
    const payload = await updateSharedProfile(data)
    profile.value = { ...profile.value, ...payload }
    toast.success('Đã cập nhật thông tin cá nhân.')
  } catch {
    toast.error('Không thể cập nhật thông tin cá nhân.')
  }
}

// Professional info save (extended profile)
const handleProfessionalSave = async (data) => {
  try {
    const payload = await updateSharedProfile({
      ...profile.value,
      subject: data.subject,
      department: data.department,
      school: data.school,
      hireDate: data.hireDate,
      bio: data.bio
    })
    profile.value = { ...profile.value, ...payload }
    toast.success('Đã cập nhật thông tin chuyên môn.')
  } catch {
    toast.error('Không thể cập nhật thông tin chuyên môn.')
  }
}

// Password change
const handleChangePassword = async ({ currentPassword, newPassword }) => {
  try {
    await changePassword({ currentPassword, newPassword })
    toast.success('Đổi mật khẩu thành công.')
  } catch {
    toast.error('Không thể đổi mật khẩu. Vui lòng kiểm tra mật khẩu hiện tại.')
  }
}

// Notification settings update
const handleNotificationUpdate = ({ key, value }) => {
  // Could persist to server or local storage
  try {
    const stored = JSON.parse(localStorage.getItem('notification_prefs') || '{}')
    stored[key] = value
    localStorage.setItem('notification_prefs', JSON.stringify(stored))
    toast.success('Đã cập nhật cài đặt thông báo.')
  } catch {
    // Silently fail
  }
}

// Preference update
const handlePreferenceUpdate = ({ key, value }) => {
  // Preferences are already persisted in PreferenceSettings
  // Could sync with server here if needed
}

// Delete account
const handleDeleteAccount = () => {
  if (confirm('Bạn có chắc chắn muốn xóa tài khoản? Hành động này không thể hoàn tác.')) {
    toast.error('Liên hệ quản trị viên để xóa tài khoản.')
  }
}

const goToDashboard = () => router.push('/teacher/dashboard')

onMounted(loadProfile)
</script>
