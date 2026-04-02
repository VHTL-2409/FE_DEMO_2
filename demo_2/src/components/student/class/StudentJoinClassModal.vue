<template>
  <BaseModal
    :open="open"
    title="Tham gia lớp học"
    size="md"
    @close="$emit('close')"
  >
    <form @submit.prevent="handleJoin" class="space-y-4">
      <div class="cosmos-field">
        <input
          id="class-code"
          v-model="classCode"
          class="cosmos-field__input"
          type="text"
          placeholder=" "
          autocomplete="off"
        />
        <label for="class-code" class="cosmos-field__label">Mã lớp học</label>
      </div>
      <p v-if="error" class="cosmos-field__error">{{ error }}</p>
    </form>
    <template #footer>
      <button type="button" class="cosmos-btn cosmos-btn--ghost cosmos-btn--md" @click="$emit('close')">
        Huỷ
      </button>
      <button type="button" class="cosmos-btn cosmos-btn--primary cosmos-btn--md" @click="handleJoin" :disabled="!classCode.trim()">
        Tham gia
      </button>
    </template>
  </BaseModal>
</template>

<script setup>
import { ref } from 'vue'
import BaseModal from '../../shared/BaseModal.vue'

const props = defineProps({
  open: { type: Boolean, default: false }
})
const emit = defineEmits(['close', 'joined'])

const classCode = ref('')
const error = ref('')

const handleJoin = () => {
  if (!classCode.value.trim()) {
    error.value = 'Vui lòng nhập mã lớp học.'
    return
  }
  emit('joined', classCode.value.trim())
  classCode.value = ''
  error.value = ''
  emit('close')
}
</script>
