<template>
  <div>
    <!-- Chua dang nhap: Form login Azota -->
    <div v-if="!azotaToken" class="space-y-4">
      <div class="flex items-center gap-2 mb-3">
        <LucideIcon name="school" />
        <h4 class="text-base font-bold text-slate-900 dark:text-white">Nhap tu Azota.vn</h4>
      </div>
      <p class="text-xs text-slate-500 dark:text-slate-400">
        Dang nhap tai khoan Azota de tai danh sach de thi va cau hoi cua ban.
      </p>
      <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <label class="block">
          <span class="text-xs font-semibold text-slate-600 dark:text-slate-300">So dien thoai Azota</span>
          <input
            v-model="phone"
            type="tel"
            placeholder="0912345678"
            class="mt-1 w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
          />
        </label>
        <label class="block">
          <span class="text-xs font-semibold text-slate-600 dark:text-slate-300">Mat khau Azota</span>
          <input
            v-model="password"
            type="password"
            placeholder="Mật khẩu đăng nhập Azota"
            class="mt-1 w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100"
          />
        </label>
      </div>
      <button
        :disabled="loggingIn || !phone || !password"
        type="button"
        class="inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-white shadow-md transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
        @click="loginAzota"
      >
        <LucideIcon name="progress_activity" v-if="loggingIn"  text-lg animate-spin/>
        <LucideIcon name="login" v-else  text-lg/>
        {{ loggingIn ? 'Dang dang nhap...' : 'Dang nhap Azota' }}
      </button>
      <p v-if="loginError" class="text-xs text-red-500 font-semibold">{{ loginError }}</p>
    </div>

    <!-- Da dang nhap: Hien thi danh sach de thi -->
    <div v-else class="space-y-4">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <LucideIcon name="school" />
          <h4 class="text-base font-bold text-slate-900 dark:text-white">Chon de thi tu Azota</h4>
        </div>
        <button
          type="button"
          class="text-xs font-semibold text-slate-500 hover:text-red-500 transition-colors"
          @click="logoutAzota"
        >
          Dang xuat
        </button>
      </div>

      <!-- User info -->
      <div class="rounded-xl bg-primary/5 border border-primary/20 px-4 py-2.5 flex items-center gap-3">
        <LucideIcon name="account_circle" size="18" />
        <div class="text-xs">
          <p class="font-semibold text-slate-700 dark:text-slate-200">{{ azotaUserName || phone }}</p>
          <p v-if="azotaSchoolName" class="text-slate-500">{{ azotaSchoolName }}</p>
        </div>
      </div>

      <!-- Loading banks -->
      <div v-if="loadingBanks" class="flex items-center justify-center py-8">
        <LucideIcon name="progress_activity" size="30" />
        <span class="ml-2 text-sm text-slate-500">Đang tải danh sách đề thi...</span>
      </div>

      <!-- Danh sach de thi -->
      <div v-else-if="questionBanks.length > 0" class="space-y-2 max-h-72 overflow-y-auto">
        <label
          v-for="bank in questionBanks"
          :key="bank.id"
          class="flex items-start gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3 cursor-pointer transition-all hover:border-primary/40 hover:bg-primary/5 dark:border-slate-700 dark:bg-slate-800"
          :class="selectedBankIds.includes(bank.id) ? 'border-primary bg-primary/5' : ''"
        >
          <input
            v-model="selectedBankIds"
            type="checkbox"
            :value="bank.id"
            class="mt-1 h-4 w-4 accent-primary flex-shrink-0"
          />
          <div class="flex-1 min-w-0">
            <p class="text-sm font-semibold text-slate-900 dark:text-white truncate">{{ bank.title || 'De thi khong ten' }}</p>
            <p v-if="bank.subject" class="text-xs text-slate-500 mt-0.5">{{ bank.subject }}</p>
            <div class="flex flex-wrap gap-2 mt-1.5">
              <span v-if="bank.questionCount" class="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600 dark:bg-slate-700 dark:text-slate-300">{{ bank.questionCount }} cau</span>
              <span v-if="bank.examType" class="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-500 dark:bg-slate-700 dark:text-slate-400">{{ bank.examType }}</span>
              <span v-if="bank.grade" class="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-500 dark:bg-slate-700 dark:text-slate-400">Lop {{ bank.grade }}</span>
            </div>
          </div>
        </label>
      </div>

      <!-- Empty state -->
      <div v-else class="rounded-xl border border-dashed border-slate-200 py-8 text-center dark:border-slate-700">
        <LucideIcon name="folder_open" size="30" />
        <p class="mt-2 text-sm text-slate-500">Không có đề thi nào trên Azota</p>
      </div>

      <!-- Nut tai cau hoi -->
      <button
        v-if="questionBanks.length > 0"
        :disabled="loadingQuestions || selectedBankIds.length === 0"
        type="button"
        class="inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-white shadow-md transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
        @click="fetchSelectedQuestions"
      >
        <LucideIcon name="progress_activity" v-if="loadingQuestions"  text-lg animate-spin/>
        <LucideIcon name="download" v-else  text-lg/>
        {{ loadingQuestions ? 'Đang tải câu hỏi...' : `Tải câu hỏi từ ${selectedBankIds.length > 0 ? selectedBankIds.length + ' đề thi' : ''}` }}
      </button>

      <!-- Loading questions progress -->
      <div v-if="loadingQuestions" class="flex items-center gap-2 text-xs text-slate-500">
        <LucideIcon name="progress_activity" size="14" />
        Đang tải câu hỏi từ {{ selectedBankIds.length }} đề thi...
      </div>

      <p v-if="fetchError" class="text-xs text-red-500 font-semibold">{{ fetchError }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { azotaLogin, getAzotaQuestionBanks, getAzotaExamQuestions } from '../../services/azotaService'

const emit = defineEmits(['questions-fetched', 'loading-change'])

const AZOTA_TOKEN_KEY = 'azota_token'
const AZOTA_USER_KEY = 'azota_user'

const phone = ref('')
const password = ref('')
const loggingIn = ref(false)
const loginError = ref('')

const azotaToken = ref(localStorage.getItem(AZOTA_TOKEN_KEY) || '')
const azotaUserName = ref(localStorage.getItem(AZOTA_USER_KEY) || '')
const azotaSchoolName = ref('')

const questionBanks = ref([])
const loadingBanks = ref(false)
const selectedBankIds = ref([])

const loadingQuestions = ref(false)
const fetchError = ref('')

const loginAzota = async () => {
  if (!phone.value || !password.value) return
  loggingIn.value = true
  loginError.value = ''
  try {
    const resp = await azotaLogin(phone.value.trim(), password.value)
    if (!resp?.token) {
      loginError.value = 'Dang nhap that bai. Kiem tra so dien thoai va mat khau.'
      return
    }
    azotaToken.value = resp.token
    azotaUserName.value = resp.userName || resp.userId || phone.value
    azotaSchoolName.value = resp.schoolName || ''
    localStorage.setItem(AZOTA_TOKEN_KEY, resp.token)
    localStorage.setItem(AZOTA_USER_KEY, azotaUserName.value)
    await loadQuestionBanks()
  } catch (err) {
    loginError.value = err.message || 'Dang nhap that bai. Vui long thu lai.'
  } finally {
    loggingIn.value = false
  }
}

const logoutAzota = () => {
  azotaToken.value = ''
  azotaUserName.value = ''
  azotaSchoolName.value = ''
  questionBanks.value = []
  selectedBankIds.value = []
  localStorage.removeItem(AZOTA_TOKEN_KEY)
  localStorage.removeItem(AZOTA_USER_KEY)
}

const loadQuestionBanks = async () => {
  if (!azotaToken.value) return
  loadingBanks.value = true
  emit('loading-change', true)
  try {
    const banks = await getAzotaQuestionBanks(azotaToken.value)
    questionBanks.value = banks || []
  } catch (err) {
    questionBanks.value = []
  } finally {
    loadingBanks.value = false
    emit('loading-change', false)
  }
}

const fetchSelectedQuestions = async () => {
  if (selectedBankIds.value.length === 0) return
  loadingQuestions.value = true
  emit('loading-change', true)
  fetchError.value = ''
  try {
    const allQuestions = []
    for (const bankId of selectedBankIds.value) {
      const detail = await getAzotaExamQuestions(bankId, azotaToken.value)
      if (detail?.questions) {
        allQuestions.push(...detail.questions)
      }
    }
    emit('questions-fetched', allQuestions)
  } catch (err) {
    fetchError.value = err.message || 'Không thể tải câu hỏi. Vui lòng thử lại.'
  } finally {
    loadingQuestions.value = false
    emit('loading-change', false)
  }
}

onMounted(async () => {
  if (azotaToken.value) {
    await loadQuestionBanks()
  }
})
</script>

