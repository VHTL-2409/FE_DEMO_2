<template>
  <div :class="isDark ? 'dark' : 'light'" class="portal-viewport flex h-full min-h-0 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100">
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
        <StudentTopHeader class="shrink-0" />

        <main class="teacher-page-shell relative flex min-h-0 w-full flex-1 flex-col overflow-hidden">
          <div class="portal-scrollbar relative flex min-h-0 flex-1 flex-col space-y-5 overflow-y-auto md:space-y-6">
          <PageHeader
            eyebrow="Student Profile"
            title="Hồ sơ sinh viên"
            subtitle="Theo dõi thông tin cá nhân và cập nhật tài khoản trong một không gian rõ ràng hơn."
          >
            <template #actions>
              <BaseButton variant="ghost" @click="goToDashboard">
                Quay lại trang chủ
              </BaseButton>
            </template>
          </PageHeader>

          <div v-if="isLoading" class="animate-fade-up-delay" aria-busy="true">
            <BaseCard padding="lg" class="w-full rounded-[1.75rem]">
              <div class="flex flex-col md:flex-row gap-8 items-start">
                <div class="flex flex-col items-center mx-auto md:mx-0">
                  <SkeletonLoader variant="avatar" size="6rem" />
                  <div class="mt-4 w-40 space-y-2">
                    <div class="h-5 portal-skeleton rounded-md w-full" />
                    <div class="h-3 portal-skeleton rounded-md w-2/3 mx-auto" />
                  </div>
                </div>
                <div class="flex-1 space-y-3 w-full">
                  <SkeletonLoader variant="text" :lines="6" />
                </div>
              </div>
            </BaseCard>
          </div>

          <section v-else class="grid grid-cols-1 gap-6 animate-fade-up-delay">
            <BaseCard padding="lg" class="w-full">
              <div class="flex flex-col items-center text-center">
                <div class="size-24 rounded-full border-4 border-primary/20 bg-primary/10 flex items-center justify-center text-3xl font-black text-primary overflow-hidden">
                  <img v-if="profileAvatarUrl && formatField(profileAvatarUrl) !== 'Chưa điền'" :src="profileAvatarUrl" alt="Avatar" class="h-full w-full object-cover" />
                  <span v-else>{{ profileInitial }}</span>
                </div>
                <h2 class="mt-4 text-2xl font-bold">{{ profileName }}</h2>
                <p class="text-sm text-slate-500 dark:text-slate-400">ID: {{ profileId }}</p>
                <p class="text-sm text-slate-500 dark:text-slate-400">{{ formatField(profileEmail) }}</p>
                <label class="mt-4 inline-flex items-center gap-2 text-xs font-semibold text-primary px-3 py-1.5 rounded-lg border border-primary/20 bg-primary/10 cursor-pointer hover:bg-primary/20">
                  <input type="file" class="hidden" accept="image/png,image/jpeg" @change="handleAvatarChange" />
                  <span class="material-symbols-outlined text-sm">photo_camera</span>
                  {{ isUploadingAvatar ? 'Đang tải...' : 'Tải ảnh đại diện' }}
                </label>
              </div>

              <div class="mt-6 space-y-3 text-sm">
                <div class="flex justify-between">
                  <span class="text-slate-500">Tên đăng nhập</span>
                  <span class="font-semibold">{{ formatField(profileUsername) }}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-slate-500">Tên hiển thị</span>
                  <span class="font-semibold">{{ formatField(profileDisplayName) }}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-slate-500">Họ và tên</span>
                  <span class="font-semibold">{{ formatField(profileFullName) }}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-slate-500">Ngày sinh</span>
                  <span class="font-semibold">{{ formatDate(profileDateOfBirth) }}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-slate-500">Email</span>
                  <span class="font-semibold">{{ formatField(profileEmail) }}</span>
                </div>
                <div class="flex justify-between">
                  <span class="text-slate-500">Số điện thoại</span>
                  <span class="font-semibold">{{ formatField(profilePhone) }}</span>
                </div>
              </div>

              <div class="mt-8 flex flex-wrap items-center gap-3">
                <BaseButton size="sm" variant="ghost" @click="toggleEditProfile">
                  {{ isEditingProfile ? 'Đóng chỉnh sửa' : 'Chỉnh sửa thông tin' }}
                </BaseButton>
                <BaseButton size="sm" variant="secondary" @click="toggleChangePassword">
                  {{ isChangingPassword ? 'Đóng đổi mật khẩu' : 'Đổi mật khẩu' }}
                </BaseButton>
              </div>

            </BaseCard>
          </section>
          </div>

        <div v-if="isEditingProfile" class="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="student-profile-edit-title" @click.self="toggleEditProfile">
          <div class="modal-content w-full max-w-2xl">
            <div class="modal-header">
              <div class="flex items-center gap-3">
                <div class="size-10 rounded-xl bg-indigo-100 dark:bg-indigo-500/20 flex items-center justify-center">
                  <span class="material-symbols-outlined text-indigo-600 dark:text-indigo-400 text-xl">person</span>
                </div>
                <h3 id="student-profile-edit-title" class="text-lg font-bold text-slate-900 dark:text-slate-100">Cập nhật thông tin</h3>
              </div>
              <button type="button" class="modal-close-btn" aria-label="Đóng" @click="toggleEditProfile">
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>
            <form class="modal-body space-y-4" @submit.prevent="submitProfileUpdate">
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div class="flex flex-col gap-2">
                  <label for="student-profile-display-name" class="text-sm font-medium text-slate-600 dark:text-slate-300">Tên hiển thị</label>
                  <input
                    id="student-profile-display-name"
                    v-model="profileForm.displayName"
                    class="w-full px-4 py-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                    placeholder="Nhập tên hiển thị"
                    type="text"
                  />
                </div>
                <div class="flex flex-col gap-2">
                  <label for="student-profile-full-name" class="text-sm font-medium text-slate-600 dark:text-slate-300">Họ và tên</label>
                  <input
                    id="student-profile-full-name"
                    v-model="profileForm.fullName"
                    class="w-full px-4 py-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                    placeholder="Nhập họ và tên"
                    type="text"
                  />
                </div>
                <div class="flex flex-col gap-2">
                  <label for="student-profile-dob" class="text-sm font-medium text-slate-600 dark:text-slate-300">Ngày sinh</label>
                  <input
                    id="student-profile-dob"
                    v-model="profileForm.dateOfBirth"
                    class="w-full px-4 py-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                    type="date"
                  />
                </div>
                <div class="flex flex-col gap-2">
                  <label for="student-profile-email" class="text-sm font-medium text-slate-600 dark:text-slate-300">Email</label>
                  <input
                    id="student-profile-email"
                    v-model="profileForm.email"
                    class="w-full px-4 py-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                    placeholder="Nhập email"
                    type="email"
                  />
                </div>
                <div class="flex flex-col gap-2">
                  <label for="student-profile-phone" class="text-sm font-medium text-slate-600 dark:text-slate-300">Số điện thoại</label>
                  <input
                    id="student-profile-phone"
                    v-model="profileForm.phone"
                    class="w-full px-4 py-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                    placeholder="Nhập số điện thoại"
                    type="tel"
                  />
                </div>
              </div>
              <div class="modal-footer">
                <button
                  type="button"
                  class="px-5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-600 text-slate-600 dark:text-slate-300 font-semibold hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
                  @click="toggleEditProfile"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  :disabled="isSavingProfile"
                  class="px-5 py-2.5 rounded-xl bg-primary text-white font-semibold hover:bg-primary/90 transition-colors disabled:opacity-60 disabled:cursor-not-allowed flex items-center gap-2"
                >
                  <span class="material-symbols-outlined text-lg" v-if="isSavingProfile">hourglass_empty</span>
                  {{ isSavingProfile ? 'Đang lưu...' : 'Lưu thay đổi' }}
                </button>
              </div>
            </form>
          </div>
        </div>

        <div v-if="isChangingPassword" class="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="student-password-title" @click.self="toggleChangePassword">
          <div class="modal-content w-full max-w-lg">
            <div class="modal-header">
              <div class="flex items-center gap-3">
                <div class="size-10 rounded-xl bg-amber-100 dark:bg-amber-500/20 flex items-center justify-center">
                  <span class="material-symbols-outlined text-amber-600 dark:text-amber-400 text-xl">lock</span>
                </div>
                <h3 id="student-password-title" class="text-lg font-bold text-slate-900 dark:text-slate-100">Thay đổi mật khẩu</h3>
              </div>
              <button type="button" class="modal-close-btn" aria-label="Đóng" @click="toggleChangePassword">
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>
            <form class="modal-body space-y-4" @submit.prevent="submitChangePassword">
              <div class="flex flex-col gap-2">
                <label class="text-sm font-medium text-slate-600 dark:text-slate-300">Mật khẩu hiện tại</label>
                <input
                  v-model="passwordForm.currentPassword"
                  class="w-full px-4 py-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                  placeholder="Nhập mật khẩu hiện tại"
                  type="password"
                />
              </div>
              <div class="flex flex-col gap-2">
                <label class="text-sm font-medium text-slate-600 dark:text-slate-300">Mật khẩu mới</label>
                <input
                  v-model="passwordForm.newPassword"
                  class="w-full px-4 py-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                  placeholder="Nhập mật khẩu mới"
                  type="password"
                />
              </div>
              <div class="flex flex-col gap-2">
                <label class="text-sm font-medium text-slate-600 dark:text-slate-300">Xác nhận mật khẩu mới</label>
                <input
                  v-model="passwordForm.confirmPassword"
                  class="w-full px-4 py-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                  placeholder="Nhập lại mật khẩu mới"
                  type="password"
                />
              </div>
              <div class="modal-footer">
                <button
                  type="button"
                  class="px-5 py-2.5 rounded-xl border border-slate-200 dark:border-slate-600 text-slate-600 dark:text-slate-300 font-semibold hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
                  @click="toggleChangePassword"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  :disabled="isSavingPassword"
                  class="px-5 py-2.5 rounded-xl bg-primary text-white font-semibold hover:bg-primary/90 transition-colors disabled:opacity-60 disabled:cursor-not-allowed flex items-center gap-2"
                >
                  <span class="material-symbols-outlined text-lg" v-if="isSavingPassword">hourglass_empty</span>
                  {{ isSavingPassword ? 'Đang cập nhật...' : 'Cập nhật mật khẩu' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword, fetchStudentProfile, updateSharedProfile, uploadAvatar } from '../../services/authService'
import { listMyAttempts } from '../../services/attemptService'
import { useToast } from '../../composables/useToast'
import StudentTopHeader from './StudentTopHeader.vue'
import BaseCard from '../shared/BaseCard.vue'
import BaseButton from '../shared/BaseButton.vue'
import PageHeader from '../shared/PageHeader.vue'
import SkeletonLoader from '../shared/SkeletonLoader.vue'

const isDark = ref(false)
const router = useRouter()
const profile = ref(null)
const attempts = ref([])
const isLoading = ref(false)
const isUploadingAvatar = ref(false)
const isEditingProfile = ref(false)

const toast = useToast()
const isSavingProfile = ref(false)
const profileForm = ref({
  displayName: '',
  fullName: '',
  dateOfBirth: '',
  email: '',
  phone: ''
})
const isChangingPassword = ref(false)
const isSavingPassword = ref(false)
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileName = computed(() => profile.value?.displayName || profile.value?.username || 'Sinh viên')
const profileInitial = computed(() => String(profileName.value).trim().charAt(0).toUpperCase() || 'S')
const profileId = computed(() => profile.value?.id || '-')
const profileUsername = computed(() => profile.value?.username || '-')
const profileDisplayName = computed(() => profile.value?.displayName || '-')
const profileFullName = computed(() => profile.value?.fullName || '-')
const profileDateOfBirth = computed(() => profile.value?.dateOfBirth || '-')
const profileEmail = computed(() => profile.value?.email || '-')
const profilePhone = computed(() => profile.value?.phone || '-')
const profileAvatarUrl = computed(() => profile.value?.avatarUrl || '-')

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
    if (!profile.value) {
      profile.value = result || {}
    }
    if (result?.avatarUrl) {
      profile.value = { ...profile.value, avatarUrl: result.avatarUrl }
    }
  } catch (error) {
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
  if (isEditingProfile.value) {
    syncProfileForm()
  }
}

const submitProfileUpdate = async () => {
  if (!profileForm.value.displayName?.trim()) {
    toast.error('Vui lòng nhập tên hiển thị.')
    return
  }
  if (profileForm.value.email && !profileForm.value.email.includes('@')) {
    toast.error('Email không hợp lệ.')
    return
  }

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
  } catch (error) {
    toast.error('Không thể cập nhật thông tin.')
  } finally {
    isSavingProfile.value = false
  }
}

const toggleChangePassword = () => {
  isChangingPassword.value = !isChangingPassword.value
  if (!isChangingPassword.value) {
    passwordForm.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
  }
}

const submitChangePassword = async () => {
  if (!passwordForm.value.currentPassword || !passwordForm.value.newPassword || !passwordForm.value.confirmPassword) {
    toast.error('Vui lòng nhập đầy đủ thông tin.')
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    toast.error('Mật khẩu mới phải có ít nhất 6 ký tự.')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    toast.error('Mật khẩu xác nhận không khớp.')
    return
  }

  isSavingPassword.value = true
  try {
    await changePassword({
      currentPassword: passwordForm.value.currentPassword,
      newPassword: passwordForm.value.newPassword
    })
    toast.success('Đổi mật khẩu thành công.')
    passwordForm.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
    isChangingPassword.value = false
  } catch (error) {
    toast.error('Không thể đổi mật khẩu.')
  } finally {
    isSavingPassword.value = false
  }
}

const loadProfileData = async () => {
  isLoading.value = true

  try {
    const [profilePayload, attemptsPayload] = await Promise.all([
      fetchStudentProfile(),
      listMyAttempts()
    ])

    profile.value = profilePayload
    attempts.value = attemptsPayload || []
    if (isEditingProfile.value) {
      syncProfileForm()
    }
  } catch (error) {
    profile.value = null
    attempts.value = []
    toast.error('Không thể tải hồ sơ sinh viên.')
  } finally {
    isLoading.value = false
  }
}

const goToDashboard = () => {
  router.push('/student/dashboard')
}

onMounted(() => {
  loadProfileData()
})
</script>
