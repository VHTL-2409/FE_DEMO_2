<template>
  <div :class="isDark ? 'dark' : 'light'" class="flex h-full min-h-0 flex-1 flex-col bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100">
    <div class="relative flex min-h-0 w-full flex-1 flex-col overflow-x-hidden overflow-y-hidden">
      <div class="layout-container flex min-h-0 flex-1 grow flex-col">
        <TeacherTopHeader active-section="profile" />

        <main class="teacher-stitch-main teacher-page-shell portal-scrollbar relative mx-auto w-full max-w-none min-h-0 flex-1 overflow-x-hidden overflow-y-auto">

          <div class="relative px-4 py-8 sm:px-8 lg:px-10">
            <header class="mb-10 flex flex-col gap-4 animate-fade-up md:mb-12 md:flex-row md:items-end md:justify-between">
              <div>
                <p class="portal-kicker mb-2">
                  <RouterLink to="/teacher/dashboard" class="text-slate-500 transition hover:text-[var(--role-primary)] dark:text-slate-400">Trang chủ</RouterLink>
                  <span class="mx-1.5 text-slate-300 dark:text-slate-600">/</span>
                  <span class="font-semibold text-[var(--role-primary)]">Hồ sơ</span>
                </p>
                <h1 class="stitch-font-headline text-4xl font-bold tracking-tight text-amber-900 dark:text-amber-100 md:text-5xl">
                  Hồ sơ giáo viên
                </h1>
              </div>
              <button
                @click="goToDashboard"
                class="inline-flex items-center gap-2 rounded-xl border border-primary/20 bg-primary/10 px-5 py-2.5 font-bold text-primary transition-all duration-200 hover:-translate-y-0.5 hover:bg-primary/20 hover:shadow-md"
                type="button"
              >
                <span class="material-symbols-outlined text-lg">arrow_back</span>
                Quay lại trang chủ
              </button>
            </header>

            <section class="grid grid-cols-1 gap-8 animate-fade-up-delay lg:grid-cols-12">
              <div class="space-y-8 lg:col-span-4">
                <div class="stitch-ambient-shadow rounded-2xl border border-slate-200/80 bg-white p-8 text-center shadow-sm dark:border-slate-800 dark:bg-slate-900">
                  <div class="mx-auto mb-6 flex size-40 items-center justify-center overflow-hidden rounded-full border-4 border-[var(--role-outline)] bg-[var(--role-primary)] text-4xl font-black text-white shadow-sm sm:size-44">
                    <img v-if="profileAvatarUrl && formatField(profileAvatarUrl) !== 'Chưa điền'" :src="profileAvatarUrl" alt="Avatar" class="h-full w-full object-cover" />
                    <span v-else>{{ avatarLabel }}</span>
                  </div>
                  <h2 class="text-2xl font-bold text-[#1b1b23] dark:text-slate-100">{{ profileName }}</h2>
                  <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Giảng viên quản lý thi</p>
                  <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">ID: {{ profileId }}</p>
                  <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">{{ formatField(profileEmail) }}</p>
                  <label class="mt-6 inline-flex w-full cursor-pointer items-center justify-center gap-2 rounded-xl border border-primary/20 bg-primary/10 px-4 py-3 text-sm font-semibold text-primary transition-colors hover:bg-primary/20">
                    <input type="file" class="hidden" accept="image/png,image/jpeg" @change="handleAvatarChange" />
                    <span class="material-symbols-outlined text-base">photo_camera</span>
                    {{ isUploadingAvatar ? 'Đang tải ảnh...' : 'Tải lên ảnh mới' }}
                  </label>
                </div>

                <div class="teacher-stitch-panel-muted rounded-2xl p-6">
                  <p class="mb-5 text-xs font-bold uppercase tracking-[0.18em] text-slate-500 dark:text-slate-400">
                    Bảo mật tài khoản
                  </p>
                  <div class="space-y-3">
                    <button
                      type="button"
                      class="flex w-full items-center justify-between rounded-xl bg-white px-4 py-4 text-left text-sm font-semibold text-[#1b1b23] transition hover:bg-[#f4f4f0] dark:bg-slate-800 dark:text-slate-100 dark:hover:bg-slate-700"
                      @click="toggleChangePassword"
                    >
                      <span class="flex items-center gap-3">
                        <span class="material-symbols-outlined text-[var(--role-primary)]">lock</span>
                        Đổi mật khẩu
                      </span>
                      <span class="material-symbols-outlined text-slate-400">chevron_right</span>
                    </button>
                    <button
                      type="button"
                      class="flex w-full items-center justify-between rounded-xl bg-white px-4 py-4 text-left text-sm font-semibold text-rose-600 transition hover:bg-rose-50 dark:bg-slate-800 dark:text-rose-400 dark:hover:bg-rose-950/30"
                      @click="goToLogin"
                    >
                      <span class="flex items-center gap-3">
                        <span class="material-symbols-outlined">logout</span>
                        Đăng xuất
                      </span>
                    </button>
                  </div>
                </div>
              </div>

              <div class="lg:col-span-8">
                <div class="stitch-ambient-shadow rounded-2xl border border-slate-200/80 bg-white p-8 shadow-sm dark:border-slate-800 dark:bg-slate-900 sm:p-10">
                  <div class="mb-8 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                      <h3 class="text-xl font-bold text-[#1b1b23] dark:text-slate-100">Thông tin cơ bản</h3>
                      <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        Dữ liệu hiển thị từ hồ sơ hiện tại; bạn có thể chỉnh sửa mà không ảnh hưởng luồng đăng nhập.
                      </p>
                    </div>
                    <button
                      type="button"
                      class="inline-flex items-center gap-2 rounded-xl border border-primary/20 bg-primary/10 px-4 py-2.5 text-sm font-semibold text-primary transition hover:bg-primary/20"
                      @click="toggleEditProfile"
                    >
                      <span class="material-symbols-outlined text-base">edit</span>
                      {{ isEditingProfile ? 'Đóng chỉnh sửa' : 'Chỉnh sửa thông tin' }}
                    </button>
                  </div>

                  <div class="grid grid-cols-1 gap-5 md:grid-cols-2 md:gap-6">
                    <div v-for="row in profileDisplayRows" :key="row.key" class="space-y-2">
                      <label class="block text-xs font-bold uppercase tracking-[0.18em] text-slate-500 dark:text-slate-400">
                        {{ row.label }}
                      </label>
                      <div class="teacher-stitch-panel-muted rounded-xl px-4 py-4 text-sm font-medium text-[#1b1b23] dark:bg-slate-800 dark:text-slate-100">
                        {{ row.value }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </section>
          </div>

          <div v-if="isEditingProfile" class="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="teacher-profile-edit-title" @click.self="toggleEditProfile">
            <div class="modal-content w-full max-w-2xl">
              <div class="modal-header">
                <div class="flex items-center gap-3">
                  <div class="size-10 rounded-xl bg-primary/10 dark:bg-primary/20 flex items-center justify-center">
                    <span class="material-symbols-outlined text-primary dark:text-amber-300 text-xl">person</span>
                  </div>
                  <h3 id="teacher-profile-edit-title" class="text-lg font-bold text-slate-900 dark:text-slate-100">Cập nhật thông tin</h3>
                </div>
                <button type="button" class="modal-close-btn" aria-label="Đóng" @click="toggleEditProfile">
                  <span class="material-symbols-outlined">close</span>
                </button>
              </div>
              <form class="modal-body space-y-4" @submit.prevent="submitProfileUpdate">
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div class="flex flex-col gap-2">
                    <label for="teacher-profile-display-name" class="text-sm font-medium text-slate-600 dark:text-slate-300">Tên hiển thị</label>
                    <input
                      id="teacher-profile-display-name"
                      v-model="profileForm.displayName"
                      class="w-full px-4 py-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                      placeholder="Nhập tên hiển thị"
                      type="text"
                    />
                  </div>
                  <div class="flex flex-col gap-2">
                    <label for="teacher-profile-full-name" class="text-sm font-medium text-slate-600 dark:text-slate-300">Họ và tên</label>
                    <input
                      id="teacher-profile-full-name"
                      v-model="profileForm.fullName"
                      class="w-full px-4 py-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                      placeholder="Nhập họ và tên"
                      type="text"
                    />
                  </div>
                  <div class="flex flex-col gap-2">
                    <label for="teacher-profile-dob" class="text-sm font-medium text-slate-600 dark:text-slate-300">Ngày sinh</label>
                    <input
                      id="teacher-profile-dob"
                      v-model="profileForm.dateOfBirth"
                      class="w-full px-4 py-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                      type="date"
                    />
                  </div>
                  <div class="flex flex-col gap-2">
                    <label for="teacher-profile-email" class="text-sm font-medium text-slate-600 dark:text-slate-300">Email</label>
                    <input
                      id="teacher-profile-email"
                      v-model="profileForm.email"
                      class="w-full px-4 py-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                      placeholder="Nhập email"
                      type="email"
                    />
                  </div>
                  <div class="flex flex-col gap-2">
                    <label for="teacher-profile-phone" class="text-sm font-medium text-slate-600 dark:text-slate-300">Số điện thoại</label>
                    <input
                      id="teacher-profile-phone"
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

          <div v-if="isChangingPassword" class="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="teacher-password-title" @click.self="toggleChangePassword">
            <div class="modal-content w-full max-w-lg">
              <div class="modal-header">
                <div class="flex items-center gap-3">
                  <div class="size-10 rounded-xl bg-amber-100 dark:bg-amber-500/20 flex items-center justify-center">
                    <span class="material-symbols-outlined text-amber-600 dark:text-amber-400 text-xl">lock</span>
                  </div>
                  <h3 id="teacher-password-title" class="text-lg font-bold text-slate-900 dark:text-slate-100">Thay đổi mật khẩu</h3>
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
import { changePassword, clearAuthSession, fetchTeacherProfile, updateSharedProfile, uploadAvatar } from '../../services/authService'
import { useToast } from '../../composables/useToast'
import TeacherTopHeader from './TeacherTopHeader.vue'

const isDark = ref(false)
const router = useRouter()
const profile = ref(null)
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

const profileName = computed(() => profile.value?.fullName || profile.value?.displayName || profile.value?.username || 'Giáo viên')
const profileId = computed(() => profile.value?.id || '-')
const profileUsername = computed(() => profile.value?.username || '-')
const profileDisplayName = computed(() => profile.value?.displayName || '-')
const profileFullName = computed(() => profile.value?.fullName || '-')
const profileDateOfBirth = computed(() => profile.value?.dateOfBirth || '-')
const profileEmail = computed(() => profile.value?.email || '-')
const profilePhone = computed(() => profile.value?.phone || '-')
const profileAvatarUrl = computed(() => profile.value?.avatarUrl || '-')
const avatarLabel = computed(() => profileName.value.slice(0, 1).toUpperCase())

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
  { key: 'dateOfBirth', label: 'Ngày sinh', value: formatDate(profile.value?.dateOfBirth) },
  { key: 'email', label: 'Email', value: formatField(profileEmail.value) },
  { key: 'phone', label: 'Số điện thoại', value: formatField(profilePhone.value) }
])

const loadProfile = async () => {
  try {
    profile.value = await fetchTeacherProfile()
    if (isEditingProfile.value) {
      syncProfileForm()
    }
  } catch {
    profile.value = null
  }
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

const goToDashboard = () => {
  router.push('/teacher/dashboard')
}

const goToLogin = () => {
  clearAuthSession()
  router.push('/login')
}

onMounted(() => {
  loadProfile()
})
</script>

