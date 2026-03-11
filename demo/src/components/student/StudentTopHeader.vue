<template>
  <header
    class="flex items-center justify-between whitespace-nowrap border-b border-solid border-primary/10 bg-white dark:bg-background-dark px-6 md:px-20 py-4"
  >
    <div class="flex items-center gap-3 text-primary dark:text-slate-100">
      <div class="size-8 bg-primary rounded-lg flex items-center justify-center text-white">
        <span class="material-symbols-outlined">menu_book</span>
      </div>
      <h2 class="text-xl font-bold leading-tight tracking-tight">ExamPortal</h2>
    </div>
    <div class="flex flex-1 justify-end gap-4 items-center">
      <slot name="rightActions" />
      <button v-if="showNotifications" class="flex items-center justify-center rounded-full size-10 bg-primary/10 text-primary hover:bg-primary/20 transition-colors" type="button">
        <span class="material-symbols-outlined">notifications</span>
      </button>
      <button
        v-if="showSignOut"
        type="button"
        @click="goToLogin"
        class="text-xs font-semibold px-3 py-1.5 rounded bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700"
      >
        Đăng xuất
      </button>
      <button v-if="showProfile" type="button" @click="goToProfile" class="flex items-center gap-3 pl-2 border-l border-primary/10 hover:-translate-y-0.5 transition-all duration-200">
        <div class="hidden md:block text-right">
          <p class="text-sm font-bold">{{ displayName }}</p>
          <p class="text-xs text-slate-500">{{ displayId }}</p>
        </div>
        <div
          class="rounded-full size-10 border-2 border-primary/20 bg-primary text-white flex items-center justify-center font-bold"
        >
          {{ avatarLabel }}
        </div>
      </button>
    </div>
  </header>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { clearAuthSession, fetchMyProfile } from '../../services/authService'

defineProps({
  showSignOut: {
    type: Boolean,
    default: true
  },
  showProfile: {
    type: Boolean,
    default: true
  },
  showNotifications: {
    type: Boolean,
    default: true
  }
})

const router = useRouter()
const profile = ref(null)

const displayName = computed(() => profile.value?.username || 'Sinh viên')
const displayId = computed(() => (profile.value?.id ? `ID: ${profile.value.id}` : ''))
const avatarLabel = computed(() => displayName.value.slice(0, 1).toUpperCase())

const loadProfile = async () => {
  try {
    profile.value = await fetchMyProfile()
  } catch {
    profile.value = null
  }
}

const goToLogin = () => {
  clearAuthSession()
  router.push('/login')
}

const goToProfile = () => {
  router.push('/student/profile')
}

onMounted(() => {
  loadProfile()
})
</script>
