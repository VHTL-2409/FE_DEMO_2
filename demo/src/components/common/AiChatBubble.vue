<template>
  <div v-if="isTeacher" class="fixed bottom-4 right-4 z-[100] flex flex-col items-end gap-2 font-display">
    <transition name="fade-scale">
      <div
        v-show="open"
        class="w-[min(100vw-2rem,22rem)] h-[min(70vh,28rem)] flex flex-col rounded-2xl shadow-2xl border border-slate-200/80 dark:border-slate-700 bg-white/98 dark:bg-slate-900/98 overflow-hidden"
        role="dialog"
        aria-label="Trò chuyện AI"
      >
        <header
          class="shrink-0 flex items-center justify-between gap-2 px-4 py-3 bg-gradient-to-r from-violet-600 to-purple-600 text-white"
        >
          <div class="flex items-center gap-2 min-w-0">
            <span class="flex size-8 rounded-lg bg-white/20 items-center justify-center shrink-0">
              <svg class="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"
                />
              </svg>
            </span>
            <div class="min-w-0">
              <p class="font-semibold text-sm truncate">Trợ lý AI</p>
              <p class="text-[11px] text-white/80 truncate">Hỏi về học tập &amp; hệ thống</p>
            </div>
          </div>

          <div class="flex items-center gap-2 shrink-0">
            <select
              v-if="availableModels.length > 0"
              v-model="selectedModel"
              class="appearance-none bg-white/15 border border-white/20 rounded-lg px-2 py-1 text-xs text-white cursor-pointer hover:bg-white/25 focus:outline-none focus:ring-1 focus:ring-white/40 max-w-[9rem] truncate"
              title="Chọn model"
              @change="saveModel"
            >
              <option
                v-for="m in availableModels"
                :key="m"
                :value="m"
                class="text-slate-900 bg-white"
              >
                {{ m }}
              </option>
            </select>

            <button
              type="button"
              class="p-1.5 rounded-lg hover:bg-white/15 transition-colors"
              aria-label="Đóng"
              @click="open = false"
            >
              <svg class="size-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </header>

        <div ref="scrollRef" class="flex-1 overflow-y-auto px-3 py-3 space-y-3 text-sm">
          <p v-if="messages.length === 0" class="text-slate-500 dark:text-slate-400 text-center py-6 px-2">
            Xin chào! Bạn cần gợi ý ôn thi, giải thích bài, hay hướng dẫn dùng trang?
          </p>
          <div
            v-for="(m, i) in messages"
            :key="i"
            class="flex"
            :class="m.role === 'user' ? 'justify-end' : 'justify-start'"
          >
            <div
              class="max-w-[85%] rounded-2xl px-3 py-2 whitespace-pre-wrap break-words"
              :class="
                m.role === 'user'
                  ? 'bg-violet-600 text-white rounded-br-md'
                  : 'bg-slate-100 dark:bg-slate-800 text-slate-900 dark:text-slate-100 rounded-bl-md'
              "
            >
              {{ m.content }}
            </div>
          </div>
          <div v-if="loading" class="flex justify-start">
            <div class="bg-slate-100 dark:bg-slate-800 rounded-2xl rounded-bl-md px-3 py-2 text-slate-500 text-xs">
              Đang trả lời…
            </div>
          </div>
        </div>

        <footer class="shrink-0 p-3 border-t border-slate-200 dark:border-slate-700 bg-slate-50/80 dark:bg-slate-950/50">
          <div class="flex gap-2">
            <input
              v-model="input"
              type="text"
              maxlength="4000"
              placeholder="Nhập tin nhắn…"
              class="flex-1 min-w-0 rounded-xl border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-900 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-violet-500/50"
              :disabled="loading"
              @keydown.enter.prevent="send"
            />
            <button
              type="button"
              class="shrink-0 rounded-xl bg-violet-600 hover:bg-violet-700 text-white px-4 py-2 text-sm font-medium disabled:opacity-50"
              :disabled="loading || !input.trim()"
              @click="send"
            >
              Gửi
            </button>
          </div>
        </footer>
      </div>
    </transition>

    <button
      v-show="!open"
      type="button"
      class="flex size-14 rounded-full shadow-lg bg-gradient-to-br from-violet-600 to-purple-700 text-white items-center justify-center hover:scale-105 active:scale-95 transition-transform focus:outline-none focus:ring-4 focus:ring-violet-500/40"
      aria-label="Mở trò chuyện AI"
      @click="openPanel"
    >
      <svg class="size-7" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
        />
      </svg>
    </button>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '../../stores/authStore'
import { userHasTeacherAccess } from '../../services/authService'
import { sendChatMessage, getChatModels } from '../../services/aiService'
import { useToastStore } from '../../stores/toastStore'

const auth = useAuthStore()
const { user: authUser } = storeToRefs(auth)

const isTeacher = computed(() => {
  const u = authUser.value
  return u && userHasTeacherAccess(u)
})
const toast = useToastStore()

const open = ref(false)
const input = ref('')
const loading = ref(false)
const messages = ref([])
const scrollRef = ref(null)

const availableModels = ref([])
const selectedModel = ref('')
const modelsLoaded = ref(false)

const MODEL_STORAGE_KEY = 'fe_demo_chat_model'

const loadSavedModel = () => {
  try {
    const saved = localStorage.getItem(MODEL_STORAGE_KEY)
    if (saved) selectedModel.value = saved
  } catch { /* ignore */ }
}

const saveModel = () => {
  try {
    localStorage.setItem(MODEL_STORAGE_KEY, selectedModel.value)
  } catch { /* ignore */ }
}

const loadModels = async () => {
  if (modelsLoaded.value) return
  try {
    const data = await getChatModels()
    if (data?.models && Array.isArray(data.models) && data.models.length > 0) {
      availableModels.value = data.models
      if (!selectedModel.value || !data.models.includes(selectedModel.value)) {
        selectedModel.value = data.models[0]
        saveModel()
      }
    }
  } catch (e) {
    // non-critical - models will default to empty
  }
  modelsLoaded.value = true
}

const openPanel = async () => {
  open.value = true
  await loadModels()
}

const threadForApi = computed(() => messages.value.map((m) => ({ role: m.role, content: m.content })))

const scrollToBottom = async () => {
  await nextTick()
  const el = scrollRef.value
  if (el) el.scrollTop = el.scrollHeight
}

watch(
  () => messages.value.length,
  () => scrollToBottom()
)

const send = async () => {
  const text = input.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true

  try {
    const modelToSend = availableModels.value.length > 0 ? selectedModel.value : undefined
    const data = await sendChatMessage(threadForApi.value, modelToSend)
    const reply = data?.reply ?? ''
    if (data?.status === 'ERROR' || data?.error) {
      toast.warning(data?.error ? String(data.error) : reply || 'Không nhận được phản hồi.', { title: 'AI' })
    }
    messages.value.push({ role: 'assistant', content: reply || '—' })
  } catch (e) {
    toast.error(e?.message || 'Không gửi được tin nhắn.', { title: 'Lỗi' })
    messages.value.push({
      role: 'assistant',
      content: 'Đã xảy ra lỗi kết nối. Vui lòng thử lại sau.'
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// Init saved model on mount
loadSavedModel()
</script>

<style scoped>
.fade-scale-enter-active,
.fade-scale-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}
.fade-scale-enter-from,
.fade-scale-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.98);
}
</style>
