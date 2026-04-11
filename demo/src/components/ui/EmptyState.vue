<template>
  <div :class="['ds-empty-state flex flex-col items-center justify-center text-center', fill ? 'min-h-[300px]' : 'py-8 px-4', dense ? 'py-4' : '']">
    <!-- Icon -->
    <div v-if="icon" class="mb-4 flex size-14 items-center justify-center rounded-full bg-[var(--ds-gray-100)] text-[var(--ds-text-muted)]">
      <LucideIcon :name="icon" size="30" />
    </div>

    <!-- Custom icon slot -->
    <div v-if="$slots.icon" class="mb-4">
      <slot name="icon" />
    </div>

    <!-- Title -->
    <h3 v-if="title" class="text-base font-semibold text-[var(--ds-text)]">{{ title }}</h3>

    <!-- Description -->
    <p v-if="description" class="mt-1.5 max-w-sm text-sm text-[var(--ds-text-muted)] leading-relaxed">
      {{ description }}
    </p>

    <!-- Action -->
    <div v-if="$slots.action || actionLabel" class="mt-5">
      <slot name="action">
        <button
          v-if="actionLabel"
          type="button"
          class="inline-flex items-center gap-2 rounded-[var(--ds-radius-lg)] border border-[var(--ds-primary)] px-4 py-2 text-sm font-semibold text-[var(--ds-primary)] transition-colors hover:bg-[var(--ds-primary-soft)]"
          @click="$emit('action')"
        >
          {{ actionLabel }}
        </button>
      </slot>
    </div>
  </div>
</template>

<script setup>
defineProps({
  icon: { type: String, default: 'inbox' },
  title: { type: String, default: '' },
  description: { type: String, default: '' },
  actionLabel: { type: String, default: '' },
  fill: { type: Boolean, default: false },
  dense: { type: Boolean, default: false }
})

defineEmits(['action'])
</script>

