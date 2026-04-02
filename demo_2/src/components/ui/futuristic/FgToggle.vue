<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  label: {
    type: String,
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  },
  color: {
    type: String,
    default: 'cyan',
    validator: (v) => ['cyan', 'magenta', 'amber', 'violet', 'green', 'red'].includes(v)
  }
})

const emit = defineEmits(['update:modelValue'])

const toggle = () => {
  if (!props.disabled) {
    emit('update:modelValue', !props.modelValue)
  }
}
</script>

<template>
  <label class="fg-toggle" :class="{ 'fg-toggle--disabled': disabled }">
    <input
      type="checkbox"
      :checked="modelValue"
      :disabled="disabled"
      @change="toggle"
    />
    <span class="fg-toggle__track">
      <span class="fg-toggle__thumb" />
    </span>
    <span v-if="label" class="fg-toggle__label">{{ label }}</span>
  </label>
</template>

<style scoped>
.fg-toggle {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  cursor: pointer;
  user-select: none;
}

.fg-toggle--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.fg-toggle input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.fg-toggle__track {
  position: relative;
  width: 52px;
  height: 28px;
  background: rgba(10, 10, 20, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  transition: all var(--fg-duration-normal) var(--fg-ease-out);
}

.fg-toggle__thumb {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 20px;
  height: 20px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 50%;
  transition: all var(--fg-duration-normal) var(--fg-ease-spring);
}

/* Cyan */
.fg-toggle input:checked + .fg-toggle__track.fg-toggle__track--cyan,
.fg-toggle:has(input:checked) .fg-toggle__track {
  background: rgba(0, 245, 255, 0.2);
  border-color: var(--fg-neon-cyan);
  box-shadow: 0 0 15px rgba(0, 245, 255, 0.3);
}

.fg-toggle:has(input:checked) .fg-toggle__thumb {
  left: 27px;
  background: var(--fg-neon-cyan);
  box-shadow: 0 0 10px var(--fg-neon-cyan);
}

/* Magenta */
.fg-toggle--magenta input:checked + .fg-toggle__track {
  background: rgba(255, 0, 255, 0.2);
  border-color: var(--fg-neon-magenta);
  box-shadow: 0 0 15px rgba(255, 0, 255, 0.3);
}

.fg-toggle--magenta:has(input:checked) .fg-toggle__thumb {
  background: var(--fg-neon-magenta);
  box-shadow: 0 0 10px var(--fg-neon-magenta);
}

/* Green */
.fg-toggle--green input:checked + .fg-toggle__track {
  background: rgba(0, 255, 136, 0.2);
  border-color: var(--fg-neon-green);
  box-shadow: 0 0 15px rgba(0, 255, 136, 0.3);
}

.fg-toggle--green:has(input:checked) .fg-toggle__thumb {
  background: var(--fg-neon-green);
  box-shadow: 0 0 10px var(--fg-neon-green);
}

/* Red */
.fg-toggle--red input:checked + .fg-toggle__track {
  background: rgba(255, 51, 102, 0.2);
  border-color: var(--fg-neon-red);
  box-shadow: 0 0 15px rgba(255, 51, 102, 0.3);
}

.fg-toggle--red:has(input:checked) .fg-toggle__thumb {
  background: var(--fg-neon-red);
  box-shadow: 0 0 10px var(--fg-neon-red);
}

.fg-toggle__label {
  font-size: 14px;
  color: #ffffff;
}
</style>
