<template>
  <div :class="isDark ? 'dark' : 'light'" class="portal-viewport flex h-full min-h-0 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100">
    <div class="relative flex h-full min-h-0 flex-1 w-full flex-col overflow-x-hidden">
      <div class="layout-container flex h-full min-h-0 flex-1 grow flex-col">
        <StudentTopHeader active-section="profile" class="shrink-0" />

        <main class="teacher-page-shell student-stitch-main relative flex min-h-0 w-full max-w-screen-2xl flex-1 flex-col overflow-hidden">
          <div class="portal-scrollbar student-stitch-paper-bg relative flex min-h-0 flex-1 flex-col overflow-y-auto px-4 py-6 sm:px-8 lg:px-12">
          <header class="mb-10 flex flex-col gap-4 animate-fade-up sm:mb-12 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <nav class="mb-2 flex items-center gap-2 text-xs font-medium text-slate-500">
                <RouterLink to="/student/dashboard" class="transition hover:text-primary">Trang chủ</RouterLink>
                <span class="material-symbols-outlined text-[14px] text-slate-400">chevron_right</span>
                <span class="font-semibold text-primary">Hồ sơ</span>
              </nav>
              <h1 class="stitch-font-headline text-4xl font-extrabold tracking-tight text-[#191c1e] dark:text-slate-100 md:text-5xl">
                Hồ sơ học sinh
              </h1>
              <p class="mt-2 max-w-2xl text-lg font-medium text-[#464555] dark:text-slate-400">
                Quản lý thông tin cá nhân và thiết lập tài khoản của bạn.
              </p>
            </div>
            <BaseButton variant="ghost" class="shrink-0" @click="goToDashboard">Quay lại trang chủ</BaseButton>
          </header>

          <div v-if="isLoading" class="animate-fade-up-delay" aria-busy="true">
            <div class="grid grid-cols-1 gap-8 lg:grid-cols-12">
              <div class="lg:col-span-4">
                <div
                  class="rounded-xl border border-[#dbc2b0]/25 bg-white p-8 stitch-editorial-shadow dark:border-slate-600/40 dark:bg-slate-900"
                >
                  <div class="flex justify-center">
                    <SkeletonLoader variant="avatar" size="12rem" />
                  </div>
                  <div class="mx-auto mt-4 w-48 space-y-2">
                    <div class="h-6 portal-skeleton rounded-md" />
                    <div class="h-4 portal-skeleton rounded-md w-2/3 mx-auto" />
                  </div>
                </div>
              </div>
              <div class="lg:col-span-8">
                <div
                  class="rounded-xl border border-[#dbc2b0]/25 bg-white p-8 stitch-editorial-shadow dark:border-slate-600/40 dark:bg-slate-900"
                >
                  <SkeletonLoader variant="text" :lines="8" />
                </div>
              </div>
            </div>
          </div>

          <section v-else class="grid grid-cols-1 gap-8 animate-fade-up-delay lg:grid-cols-12">
            <!-- Cột trái: avatar + trạng thái (stitch bento) -->
            <div class="flex flex-col lg:col-span-4">
              <div
                class="relative flex w-full flex-col items-center overflow-hidden rounded-xl border border-[#dbc2b0]/25 bg-white p-8 stitch-editorial-shadow dark:border-slate-600/40 dark:bg-slate-900 dark:shadow-none dark:ring-1 dark:ring-slate-700"
              >
                <div class="absolute left-0 top-0 h-2 w-full bg-gradient-to-r from-primary-800 to-primary-500" />
                <div class="group relative mb-6 cursor-default">
                  <div class="rounded-full border-4 border-primary-100 p-1 dark:border-primary-500/40">
                    <div
                      class="flex size-40 items-center justify-center overflow-hidden rounded-full bg-[#f2f4f6] text-5xl font-black text-primary dark:bg-slate-800 sm:size-48"
                    >
                      <img
                        v-if="profileAvatarSrc"
                        :src="profileAvatarSrc"
                        alt="Avatar"
                        class="h-full w-full object-cover"
                      />
                      <span v-else>{{ profileInitial }}</span>
                    </div>
                  </div>
                </div>
                <h2 class="mb-1 text-2xl font-bold text-[#191c1e] dark:text-slate-100">{{ profileName }}</h2>
                <p class="mb-6 text-xs font-medium uppercase tracking-widest text-primary">Học sinh</p>
                <p class="mb-4 text-center text-sm text-[#464555] dark:text-slate-400">ID: {{ profileId }}</p>
                <label
                  class="flex w-full cursor-pointer items-center justify-center gap-2 rounded-lg bg-[#e6e8ea] py-3 text-sm font-semibold text-[#191c1e] transition hover:bg-[#d8dadc] dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
                >
                  <input type="file" class="hidden" accept="image/png,image/jpeg" @change="handleAvatarChange" />
                  <span class="material-symbols-outlined text-xl">upload</span>
                  {{ isUploadingAvatar ? 'Đang tải...' : 'Tải ảnh mới' }}
                </label>
              </div>

              <div
                class="mt-8 w-full rounded-xl border border-primary/10 bg-primary-100/40 p-6 dark:border-primary-500/30 dark:bg-primary-900/50"
              >
                <div class="mb-3 flex items-center gap-3">
                  <span class="material-symbols-outlined text-primary" style="font-variation-settings: 'FILL' 1">verified_user</span>
                  <span class="font-bold text-primary-800 dark:text-primary-200">Trạng thái tài khoản</span>
                </div>
                <p class="text-sm leading-relaxed text-primary-800/90 dark:text-primary-200/90">
                  Tài khoản đang hoạt động. Bạn có thể cập nhật thông tin hoặc đổi mật khẩu bất cứ lúc nào.
                </p>
              </div>
            </div>

            <!-- Cột phải: thông tin (read-only, kiểu form mock) -->
            <div class="lg:col-span-8">
              <div
                class="rounded-xl border border-[#dbc2b0]/25 bg-white p-8 stitch-editorial-shadow dark:border-slate-600/40 dark:bg-slate-900 dark:shadow-none dark:ring-1 dark:ring-slate-700 sm:p-10"
              >
                <h3 class="mb-8 flex items-center gap-3 text-xl font-bold text-[#191c1e] dark:text-slate-100">
                  <span class="material-symbols-outlined text-primary">badge</span>
                  Thông tin cá nhân
                </h3>
                <div class="grid grid-cols-1 gap-6 md:grid-cols-2 md:gap-8">
                  <div v-for="row in profileDisplayRows" :key="row.key" class="space-y-2">
                    <label class="block px-1 text-xs font-bold uppercase tracking-wider text-[#777587]">{{ row.label }}</label>
                    <div
                      class="rounded-lg border-0 bg-[#f2f4f6] px-5 py-4 text-sm font-medium text-[#191c1e] dark:bg-slate-800 dark:text-slate-100"
                    >
                      {{ row.value }}
                    </div>
                  </div>
                </div>

                <div class="mt-10 flex flex-wrap items-center gap-3 border-t border-[#eceef0] pt-8 dark:border-slate-700">
                  <BaseButton size="sm" variant="ghost" @click="toggleEditProfile">
                    {{ isEditingProfile ? 'Đóng chỉnh sửa' : 'Chỉnh sửa thông tin' }}
                  </BaseButton>
                  <BaseButton size="sm" variant="secondary" @click="toggleChangePassword">
                    {{ isChangingPassword ? 'Đóng đổi mật khẩu' : 'Đổi mật khẩu' }}
                  </BaseButton>
                </div>
              </div>
            </div>
          </section>
          </div>

        <div v-if="isEditingProfile" class="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="student-profile-edit-title" @click.self="toggleEditProfile">
          <div class="modal-content w-full max-w-2xl">
            <div class="modal-header">
              <div class="flex items-center gap-3">
                <div class="size-10 rounded-xl bg-primary-100 dark:bg-primary-500/20 flex items-center justify-center">
                  <span class="material-symbols-outlined text-primary-700 dark:text-primary-300 text-xl">person</span>
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
import { RouterLink, useRouter } from 'vue-router'
import { resolveBackendUrl } from '../../services/apiClient'
import { changePassword, fetchStudentProfile, updateSharedProfile, uploadAvatar } from '../../services/authService'
import { listMyAttempts } from '../../services/attemptService'
import { useToast } from '../../composables/useToast'
import StudentTopHeader from './StudentTopHeader.vue'
import BaseButton from '../shared/BaseButton.vue'
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

const profileName = computed(() => profile.value?.displayName || profile.value?.username || 'Học sinh')
const profileInitial = computed(() => String(profileName.value).trim().charAt(0).toUpperCase() || 'S')
const profileId = computed(() => profile.value?.id || '-')
const profileUsername = computed(() => profile.value?.username || '-')
const profileDisplayName = computed(() => profile.value?.displayName || '-')
const profileFullName = computed(() => profile.value?.fullName || '-')
const profileDateOfBirth = computed(() => profile.value?.dateOfBirth || '-')
const profileEmail = computed(() => profile.value?.email || '-')
const profilePhone = computed(() => profile.value?.phone || '-')
/** URL đầy đủ tới BE — ảnh lưu dạng /avatars/... */
const profileAvatarSrc = computed(() => {
  const u = profile.value?.avatarUrl
  if (!u || u === '-') return ''
  return resolveBackendUrl(u)
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

const profileDisplayRows = computed(() => [
  { key: 'username', label: 'Tên đăng nhập', value: formatField(profileUsername.value) },
  { key: 'displayName', label: 'Tên hiển thị', value: formatField(profileDisplayName.value) },
  { key: 'fullName', label: 'Họ và tên', value: formatField(profileFullName.value) },
  { key: 'dob', label: 'Ngày sinh', value: formatDate(profile.value?.dateOfBirth) },
  { key: 'email', label: 'Email', value: formatField(profileEmail.value) },
  { key: 'phone', label: 'Số điện thoại', value: formatField(profilePhone.value) }
])

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
    await updateSharedProfile({
      displayName: profileForm.value.displayName.trim(),
      fullName: profileForm.value.fullName?.trim() || null,
      dateOfBirth: profileForm.value.dateOfBirth || null,
      email: profileForm.value.email?.trim() || null,
      phone: profileForm.value.phone?.trim() || null,
      avatarUrl: profile.value?.avatarUrl || null
    })
    profile.value = await fetchStudentProfile()
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
    toast.error('Không thể tải hồ sơ học sinh.')
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
