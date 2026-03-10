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
      <button class="flex items-center justify-center rounded-full size-10 bg-primary/10 text-primary hover:bg-primary/20 transition-colors" type="button">
        <span class="material-symbols-outlined">notifications</span>
      </button>
      <button
        v-if="showSignOut"
        type="button"
        @click="goToLogin"
        class="text-xs font-semibold px-3 py-1.5 rounded bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700"
      >
        Sign Out
      </button>
      <div v-if="showProfile" class="flex items-center gap-3 pl-2 border-l border-primary/10">
        <div class="hidden md:block text-right">
          <p class="text-sm font-bold">Alex Johnson</p>
          <p class="text-xs text-slate-500">ID: 4421-STU</p>
        </div>
        <div
          class="bg-center bg-no-repeat aspect-square bg-cover rounded-full size-10 border-2 border-primary/20"
          style='background-image: url("https://lh3.googleusercontent.com/aida-public/AB6AXuB1e4bA4nxwNhHI-Fn4dFl5ffPsV2Qcq-aTU38W1KpDGwJVtSs8Uu50HUjL-6AQ1rsj8FzgZ85caSzJLBV2kIzkBDQx4LboDGfHJTBM4ekHERyEJBMrHARjYfGyK-OOed1VR2AVLI8Set2ttmV6DKD-1ADupmLpYFhEoCRyviMIao-qfOPN6LDLGiDaSyvu15GGz3wp3epYInY9djSMLy1DHqOjBrCWEn_nXjmRki5_ystPT2x5YTemNdGgEHmK39v616MkRN2Pcg")'
        ></div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'

defineProps({
  showSignOut: {
    type: Boolean,
    default: true
  },
  showProfile: {
    type: Boolean,
    default: true
  }
})

const router = useRouter()

const goToLogin = () => {
  router.push('/login')
}
</script>
