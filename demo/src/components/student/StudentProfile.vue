<template>
  <div class="bg-[var(--ds-bg)] min-h-full">
    <div class="mx-auto max-w-6xl px-4 pb-10 pt-4 sm:px-6 lg:px-8">

      <div class="mb-5 ds-animate-fade-up">
        <PageHeader eyebrow="Học sinh" title="Hồ sơ học sinh" subtitle="Theo dõi thông tin cá nhân và cập nhật tài khoản.">
          <template #actions>
            <button type="button" class="inline-flex items-center gap-2 rounded-[var(--ds-radius-lg)] border border-[var(--ds-border)] bg-[var(--ds-surface)] px-4 py-2 text-sm font-semibold text-[var(--ds-text-secondary)] transition-colors hover:bg-[var(--ds-gray-100)]" @click="goToDashboard">
              <LucideIcon name="home" size="18" />
              Quay lại trang chủ
            </button>
          </template>
        </PageHeader>
      </div>

      <!-- Loading skeleton -->
      <div v-if="isLoading" class="ds-animate-fade-up" aria-busy="true">
        <div class="rounded-[var(--ds-radius-xl)] border border-[var(--ds-border)] bg-[var(--ds-surface)] p-8 shadow-[var(--ds-shadow-sm)]">
          <div class="flex flex-col items-center gap-6 md:flex-row md:items-start">
            <div class="size-24 rounded-full bg-[var(--ds-gray-100)] shrink-0"></div>
            <div class="flex-1 space-y-3 w-full">
              <div v-for="i in 6" :key="i" class="h-5 rounded bg-[var(--ds-gray-100)]" :style="{ width: `${60 + (i % 3) * 15}%` }"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Profile Card -->
      <section v-else class="ds-animate-fade-up" style="animation-delay: 0.05s">
        <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">

          <!-- Avatar + Identity -->
          <div class="lg:col-span-3 rounded-[var(--ds-radius-xl)] border border-[var(--ds-border)] bg-[var(--ds-surface)] p-6 shadow-[var(--ds-shadow-sm)] flex flex-col items-center text-center">
            <div class="relative shrink-0">
              <div class="size-24 overflow-hidden rounded-full border-4 shadow-[var(--ds-shadow-md)]" style="border-color: var(--ds-primary-soft);">
                <img v-if="profileAvatarSrc" :src="profileAvatarSrc" alt="Avatar" class="h-full w-full object-cover" />
                <div v-else class="flex h-full w-full items-center justify-center bg-[var(--ds-primary-soft)] text-3xl font-black" style="color: var(--ds-primary);">{{ profileInitial }}</div>
              </div>
              <label class="absolute -bottom-1 -right-1 flex size-8 cursor-pointer items-center justify-center rounded-full shadow-[var(--ds-shadow-sm)] transition-transform hover:scale-110" style="background-color: var(--ds-primary); color: white;">
                <input type="file" class="hidden" accept="image/png,image/jpeg" @change="handleAvatarChange" />
                <LucideIcon name="photo_camera" size="14" />
              </label>
            </div>

            <div class="mt-4">
              <h2 class="text-2xl font-bold text-[var(--ds-text)]">{{ profileName }}</h2>
              <div class="flex flex-wrap items-center gap-2 mt-1.5 justify-center">
                <span class="inline-flex items-center gap-1 rounded-full bg-[var(--ds-primary-soft)] px-2.5 py-0.5 text-xs font-semibold" style="color: var(--ds-primary);">
                  <LucideIcon name="badge" size="12" />
                  ID: {{ profileId }}
                </span>
                <span v-if="formatField(profileEmail) !== 'Chưa điền'" class="inline-flex items-center gap-1 rounded-full bg-[var(--ds-gray-100)] px-2.5 py-0.5 text-xs font-medium text-[var(--ds-text-secondary)]">
                  <LucideIcon name="mail" size="12" />
                  {{ formatField(profileEmail) }}
                </span>
              </div>
              <p v-if="isUploadingAvatar" class="mt-2 text-xs font-semibold" style="color: var(--ds-primary);">
                <LucideIcon name="progress_activity" size="14" class="animate-spin inline" />
                Đang tải ảnh...
              </p>
            </div>
          </div>

          <!-- Info + Edit + Password -->
          <div class="lg:col-span-9 rounded-[var(--ds-radius-xl)] border border-[var(--ds-border)] bg-[var(--ds-surface)] p-6 shadow-[var(--ds-shadow-sm)]">

            <!-- Info rows -->
            <div class="space-y-2 border-t border-[var(--ds-border)] pt-6">
              <div class="flex justify-between gap-4 p-3 rounded-xl hover:bg-[var(--ds-gray-50)] transition-colors">
                <span class="text-sm text-[var(--ds-text-muted)] flex items-center gap-2"><LucideIcon name="user" size="16" class="text-[var(--ds-primary)]" />Tên đăng nhập</span>
                <span class="text-sm font-semibold text-[var(--ds-text)]">{{ formatField(profileUsername) }}</span>
              </div>
              <div class="flex justify-between gap-4 p-3 rounded-xl hover:bg-[var(--ds-gray-50)] transition-colors">
                <span class="text-sm text-[var(--ds-text-muted)] flex items-center gap-2"><LucideIcon name="display_settings" size="16" class="text-[var(--ds-primary)]" />Tên hiển thị</span>
                <span class="text-sm font-semibold text-[var(--ds-text)]">{{ formatField(profileDisplayName) }}</span>
              </div>
              <div class="flex justify-between gap-4 p-3 rounded-xl hover:bg-[var(--ds-gray-50)] transition-colors">
                <span class="text-sm text-[var(--ds-text-muted)] flex items-center gap-2"><LucideIcon name="person" size="16" class="text-[var(--ds-primary)]" />Họ và tên</span>
                <span class="text-sm font-semibold text-[var(--ds-text)]">{{ formatField(profileFullName) }}</span>
              </div>
              <div class="flex justify-between gap-4 p-3 rounded-xl hover:bg-[var(--ds-gray-50)] transition-colors">
                <span class="text-sm text-[var(--ds-text-muted)] flex items-center gap-2"><LucideIcon name="cake" size="16" class="text-[var(--ds-primary)]" />Ngày sinh</span>
                <span class="text-sm font-semibold text-[var(--ds-text)]">{{ formatDate(profileDateOfBirth) }}</span>
              </div>
              <div class="flex justify-between gap-4 p-3 rounded-xl hover:bg-[var(--ds-gray-50)] transition-colors">
                <span class="text-sm text-[var(--ds-text-muted)] flex items-center gap-2"><LucideIcon name="phone" size="16" class="text-[var(--ds-primary)]" />Số điện thoại</span>
                <span class="text-sm font-semibold text-[var(--ds-text)]">{{ formatField(profilePhone) }}</span>
              </div>
              <div class="flex justify-between gap-4 p-3 rounded-xl hover:bg-[var(--ds-gray-50)] transition-colors">
                <span class="text-sm text-[var(--ds-text-muted)] flex items-center gap-2"><LucideIcon name="school" size="16" class="text-[var(--ds-primary)]" />Tổng số bài thi</span>
                <span class="text-sm font-semibold text-[var(--ds-primary)]">{{ attempts.length }}</span>
              </div>
            </div>

            <!-- Edit profile form -->
            <div class="mt-6">
              <div class="flex items-center justify-between mb-4">
                <h3 class="text-base font-bold text-[var(--ds-text)]">Chỉnh sửa hồ sơ</h3>
                <button type="button" class="text-sm font-semibold px-3 py-1.5 rounded-lg transition-colors" :class="isEditingProfile ? 'bg-[var(--ds-primary-soft)] text-[var(--ds-primary)]' : 'text-[var(--ds-text-secondary)] hover:bg-[var(--ds-gray-50)]'" @click="toggleEditProfile">{{ isEditingProfile ? 'Hủy' : 'Chỉnh sửa' }}</button>
              </div>

              <form v-if="isEditingProfile" class="grid grid-cols-1 md:grid-cols-2 gap-4" @submit.prevent="submitProfileUpdate">
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-medium text-[var(--ds-text-secondary)]">Tên hiển thị</label>
                  <input v-model="profileForm.displayName" class="ds-input w-full px-4 py-2.5 rounded-[var(--ds-radius-lg)] text-sm" placeholder="Nhập tên hiển thị" type="text" />
                </div>
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-medium text-[var(--ds-text-secondary)]">Họ và tên</label>
                  <input v-model="profileForm.fullName" class="ds-input w-full px-4 py-2.5 rounded-[var(--ds-radius-lg)] text-sm" placeholder="Nhập họ và tên" type="text" />
                </div>
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-medium text-[var(--ds-text-secondary)]">Ngày sinh</label>
                  <input v-model="profileForm.dateOfBirth" class="ds-input w-full px-4 py-2.5 rounded-[var(--ds-radius-lg)] text-sm" type="date" />
                </div>
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-medium text-[var(--ds-text-secondary)]">Email</label>
                  <input v-model="profileForm.email" class="ds-input w-full px-4 py-2.5 rounded-[var(--ds-radius-lg)] text-sm" placeholder="Nhập email" type="email" />
                </div>
                <div class="flex flex-col gap-2 md:col-span-2">
                  <label class="text-sm font-medium text-[var(--ds-text-secondary)]">Số điện thoại</label>
                  <input v-model="profileForm.phone" class="ds-input w-full px-4 py-2.5 rounded-[var(--ds-radius-lg)] text-sm max-w-md" placeholder="Nhập số điện thoại" type="tel" />
                </div>
                <div class="flex items-center gap-3 pt-2 md:col-span-2">
                  <button type="submit" :disabled="isSavingProfile" class="inline-flex items-center gap-2 rounded-[var(--ds-radius-lg)] px-5 py-2.5 text-sm font-bold text-white transition-colors disabled:opacity-60" style="background-color: var(--ds-primary);">
                    <LucideIcon name="progress_activity" size="18" />
                    {{ isSavingProfile ? 'Đang lưu...' : 'Lưu thay đổi' }}
                  </button>
                </div>
              </form>
            </div>

            <!-- Change password panel -->
            <div v-if="isChangingPassword" class="mt-6 rounded-[var(--ds-radius-xl)] border border-[var(--ds-border)] bg-[var(--ds-gray-50)] p-5">
              <h3 class="mb-4 text-base font-bold text-[var(--ds-text)]">Thay đổi mật khẩu</h3>
              <form class="space-y-4" @submit.prevent="submitChangePassword">
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-medium text-[var(--ds-text-secondary)]">Mật khẩu hiện tại</label>
                  <input v-model="passwordForm.currentPassword" class="ds-input w-full px-4 py-2.5 rounded-[var(--ds-radius-lg)] text-sm" placeholder="Nhập mật khẩu hiện tại" type="password" />
                </div>
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-medium text-[var(--ds-text-secondary)]">Mật khẩu mới</label>
                  <input v-model="passwordForm.newPassword" class="ds-input w-full px-4 py-2.5 rounded-[var(--ds-radius-lg)] text-sm" placeholder="Nhập mật khẩu mới" type="password" />
                </div>
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-medium text-[var(--ds-text-secondary)]">Xác nhận mật khẩu mới</label>
                  <input v-model="passwordForm.confirmPassword" class="ds-input w-full px-4 py-2.5 rounded-[var(--ds-radius-lg)] text-sm" placeholder="Nhập lại mật khẩu mới" type="password" />
                </div>
                <div class="flex items-center gap-3 pt-2">
                  <button type="submit" :disabled="isSavingPassword" class="inline-flex items-center gap-2 rounded-[var(--ds-radius-lg)] px-5 py-2.5 text-sm font-bold text-white transition-colors disabled:opacity-60" style="background-color: var(--ds-primary);">
                    <LucideIcon name="progress_activity" size="18" />
                    {{ isSavingPassword ? 'Đang cập nhật...' : 'Cập nhật mật khẩu' }}
                  </button>
                </div>
              </form>
            </div>

            <!-- Password toggle -->
            <div class="mt-6 pt-4 border-t border-[var(--ds-border)]">
              <button type="button" class="flex items-center gap-2 text-sm font-semibold text-[var(--ds-text-secondary)] hover:text-[var(--ds-primary)] transition-colors" @click="toggleChangePassword">
                <LucideIcon :name="isChangingPassword ? 'lock_open' : 'lock'" size="16" />
                {{ isChangingPassword ? 'Hủy đổi mật khẩu' : 'Đổi mật khẩu' }}
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword, fetchStudentProfile, updateSharedProfile, uploadAvatar } from '../../services/authService'
import { listMyAttempts } from '../../services/attemptService'
import { useToast } from '../../composables/useToast'
import PageHeader from '../ui/PageHeader.vue'
import LucideIcon from '../common/LucideIcon.vue'

const router = useRouter()
const profile = ref(null)
const attempts = ref([])
const isLoading = ref(false)
const isUploadingAvatar = ref(false)
const isEditingProfile = ref(false)
const toast = useToast()
const isSavingProfile = ref(false)
const profileForm = ref({ displayName: '', fullName: '', dateOfBirth: '', email: '', phone: '' })
const isChangingPassword = ref(false)
const passwordForm = ref({ currentPassword: '', newPassword: '', confirmPassword: '' })

const profileName = computed(() => profile.value?.displayName || profile.value?.username || 'Học sinh')
const profileInitial = computed(() => String(profileName.value).trim().charAt(0).toUpperCase() || 'S')
const profileId = computed(() => profile.value?.id || '-')
const profileUsername = computed(() => profile.value?.username || '-')
const profileDisplayName = computed(() => profile.value?.displayName || '-')
const profileFullName = computed(() => profile.value?.fullName || '-')
const profileDateOfBirth = computed(() => profile.value?.dateOfBirth || '-')
const profileEmail = computed(() => profile.value?.email || '-')
const profilePhone = computed(() => profile.value?.phone || '-')
const profileAvatarUrl = computed(() => profile.value?.avatarUrl || '-')

const resolveBackendUrl = (path) => {
  if (!path || path === '-' || path === '/-') return ''
  if (String(path).startsWith('http')) return path
  return `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082'}${path}`
}

const profileAvatarSrc = computed(() => {
  const raw = profile.value?.avatarUrl
  if (!raw || raw === '-' || raw === '/-') return ''
  return resolveBackendUrl(raw)
})

const formatField = (value) => {
  if (value === null || value === undefined) return 'Chưa điền'
  const normalized = String(value).trim()
  return normalized ? normalized : 'Chưa điền'
}

const formatDate = (value) => {
  if (!value) return 'Chưa điền'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Chưa điền'
  return new Intl.DateTimeFormat('vi-VN').format(date)
}

const handleAvatarChange = async (event) => {
  const file = event.target?.files?.[0]
  if (!file) return
  isUploadingAvatar.value = true
  try {
    const result = await uploadAvatar(file)
    if (!profile.value) profile.value = result || {}
    if (result?.avatarUrl) profile.value = { ...profile.value, avatarUrl: result.avatarUrl }
  } catch {
    toast.error('Không thể tải ảnh đại diện.')
  } finally {
    isUploadingAvatar.value = false
    event.target.value = ''
  }
}

const syncProfileForm = () => {
  profileForm.value = {
    displayName: profile.value?.displayName || '',
    fullName: profile.value?.fullName || '',
    dateOfBirth: profile.value?.dateOfBirth || '',
    email: profile.value?.email || '',
    phone: profile.value?.phone || ''
  }
}

const toggleEditProfile = () => {
  isEditingProfile.value = !isEditingProfile.value
  if (isEditingProfile.value) syncProfileForm()
}

const submitProfileUpdate = async () => {
  if (!profileForm.value.displayName?.trim()) { toast.error('Vui lòng nhập tên hiển thị.'); return }
  if (profileForm.value.email && !profileForm.value.email.includes('@')) { toast.error('Email không hợp lệ.'); return }
  isSavingProfile.value = true
  try {
    const payload = await updateSharedProfile({
      displayName: profileForm.value.displayName.trim(),
      fullName: profileForm.value.fullName?.trim() || null,
      dateOfBirth: profileForm.value.dateOfBirth || null,
      email: profileForm.value.email?.trim() || null,
      phone: profileForm.value.phone?.trim() || null,
      avatarUrl: profile.value?.avatarUrl || null
    })
    profile.value = { ...profile.value, ...payload }
    toast.success('Đã cập nhật thông tin.')
    isEditingProfile.value = false
  } catch {
    toast.error('Không thể cập nhật thông tin.')
  } finally {
    isSavingProfile.value = false
  }
}

const toggleChangePassword = () => {
  isChangingPassword.value = !isChangingPassword.value
  if (!isChangingPassword.value) passwordForm.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
}

const submitChangePassword = async () => {
  if (!passwordForm.value.currentPassword || !passwordForm.value.newPassword || !passwordForm.value.confirmPassword) { toast.error('Vui lòng nhập đầy đủ thông tin.'); return }
  if (passwordForm.value.newPassword.length < 6) { toast.error('Mật khẩu mới phải có ít nhất 6 ký tự.'); return }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) { toast.error('Mật khẩu xác nhận không khớp.'); return }
  isSavingPassword.value = true
  try {
    await changePassword({ currentPassword: passwordForm.value.currentPassword, newPassword: passwordForm.value.newPassword })
    toast.success('Đổi mật khẩu thành công.')
    passwordForm.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
    isChangingPassword.value = false
  } catch {
    toast.error('Không thể đổi mật khẩu.')
  } finally {
    isSavingPassword.value = false
  }
}

const loadProfileData = async () => {
  isLoading.value = true
  try {
    const [profilePayload, attemptsPayload] = await Promise.all([fetchStudentProfile(), listMyAttempts()])
    profile.value = profilePayload
    attempts.value = attemptsPayload || []
    if (isEditingProfile.value) syncProfileForm()
  } catch {
    profile.value = null
    attempts.value = []
    toast.error('Không thể tải hồ sơ học sinh.')
  } finally {
    isLoading.value = false
  }
}

const goToDashboard = () => router.push('/student/dashboard')

onMounted(() => { loadProfileData() })
</script>

<style scoped>
.ds-input {
  background: var(--ds-surface);
  border: 1px solid var(--ds-border);
  color: var(--ds-text);
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.ds-input::placeholder { color: var(--ds-text-muted); }
.ds-input:focus {
  border-color: var(--ds-primary);
  box-shadow: 0 0 0 3px var(--ds-primary-ring);
}
</style>
