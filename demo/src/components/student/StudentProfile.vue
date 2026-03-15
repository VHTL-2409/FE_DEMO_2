<template>
  <div :class="isDark ? 'dark' : 'light'" class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen">
    <div class="layout-container flex h-full grow flex-col">
      <StudentTopHeader />

      <main class="teacher-page-shell w-full">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

        <div class="relative space-y-8">
          <header class="flex flex-col md:flex-row md:items-end justify-between gap-4 animate-fade-up">
            <div>
              <h1 class="text-3xl font-black tracking-tight">Hồ sơ sinh viên</h1>
              <p class="text-slate-500 dark:text-slate-400 mt-1">Thông tin tài khoản và thống kê học tập của bạn.</p>
            </div>
            <button
              @click="goToDashboard"
              class="px-5 py-2.5 rounded-xl border border-primary/20 bg-primary/10 text-primary font-bold hover:bg-primary/20 hover:-translate-y-0.5 hover:shadow-md transition-all duration-200"
              type="button"
            >
              Quay lại trang chủ
            </button>
          </header>

          <p v-if="isLoading" class="text-sm text-slate-500">Đang tải hồ sơ...</p>

          <section class="grid grid-cols-1 gap-6 animate-fade-up-delay">
            <div class="teacher-card p-8 w-full">
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
                <div class="flex justify-between">
                  <span class="text-slate-500">Avatar URL</span>
                  <span class="font-semibold break-all text-right max-w-[60%]">{{ formatField(profileAvatarUrl) }}</span>
                </div>
              </div>

              <div class="mt-8 flex flex-wrap items-center gap-3">
                <button
                  type="button"
                  class="px-4 py-2 rounded-lg border border-primary/20 bg-primary/10 text-primary font-semibold hover:bg-primary/20 transition-all"
                  @click="toggleEditProfile"
                >
                  {{ isEditingProfile ? 'Đóng chỉnh sửa' : 'Chỉnh sửa thông tin' }}
                </button>
                <button
                  type="button"
                  class="px-4 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-700 dark:text-slate-200 font-semibold hover:border-primary/40 hover:text-primary transition-all"
                  @click="toggleChangePassword"
                >
                  {{ isChangingPassword ? 'Đóng đổi mật khẩu' : 'Đổi mật khẩu' }}
                </button>
              </div>

            </div>
          </section>
        </div>

        <div v-if="isEditingProfile" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 px-4 py-6" role="dialog" aria-modal="true" aria-labelledby="student-profile-edit-title">
          <div class="w-full max-w-2xl rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xl">
            <div class="flex items-center justify-between px-6 py-4 border-b border-slate-200 dark:border-slate-800">
              <h3 id="student-profile-edit-title" class="text-lg font-semibold">Cập nhật thông tin</h3>
              <button type="button" class="text-slate-500 hover:text-slate-700 dark:hover:text-slate-200" @click="toggleEditProfile">
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>
            <form class="px-6 py-5 space-y-4" @submit.prevent="submitProfileUpdate">
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
              <div class="flex flex-col sm:flex-row justify-end gap-3">
                <button
                  type="button"
                  class="px-5 py-2.5 rounded-lg border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-200 hover:border-primary/40 hover:text-primary transition-all"
                  @click="toggleEditProfile"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  :disabled="isSavingProfile"
                  class="px-5 py-2.5 rounded-lg bg-primary text-white font-semibold hover:bg-primary/90 transition-all disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  {{ isSavingProfile ? 'Đang lưu...' : 'Lưu thay đổi' }}
                </button>
              </div>
            </form>
          </div>
        </div>

        <div v-if="isChangingPassword" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 px-4 py-6" role="dialog" aria-modal="true" aria-labelledby="student-password-title">
          <div class="w-full max-w-lg rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xl">
            <div class="flex items-center justify-between px-6 py-4 border-b border-slate-200 dark:border-slate-800">
              <h3 id="student-password-title" class="text-lg font-semibold">Thay đổi mật khẩu</h3>
              <button type="button" class="text-slate-500 hover:text-slate-700 dark:hover:text-slate-200" @click="toggleChangePassword">
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>
            <form class="px-6 py-5 space-y-4" @submit.prevent="submitChangePassword">
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
              <div class="flex flex-col sm:flex-row justify-end gap-3">
                <button
                  type="button"
                  class="px-5 py-2.5 rounded-lg border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-200 hover:border-primary/40 hover:text-primary transition-all"
                  @click="toggleChangePassword"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  :disabled="isSavingPassword"
                  class="px-5 py-2.5 rounded-lg bg-primary text-white font-semibold hover:bg-primary/90 transition-all disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  {{ isSavingPassword ? 'Đang cập nhật...' : 'Cập nhật mật khẩu' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </main>

      <footer class="mt-auto px-6 md:px-20 lg:px-40 py-8 border-t border-slate-200 dark:border-slate-800 text-center">
        <p class="text-slate-500 dark:text-slate-400 text-sm">© 2026 Hệ thống thi trực tuyến ExamPortal. Bảo lưu mọi quyền.</p>
      </footer>
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
