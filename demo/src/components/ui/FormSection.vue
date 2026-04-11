<template>
  <section class="form-section">
    <div v-if="title || $slots.header" class="mb-4 flex items-center justify-between gap-3">
      <div>
        <h3 v-if="title" class="ds-text-section-title">{{ title }}</h3>
        <p v-if="description" class="mt-1 text-xs text-[var(--ds-text-muted)]">{{ description }}</p>
      </div>
      <div v-if="$slots.header">
        <slot name="header" />
      </div>
    </div>

    <div class="form-section__fields space-y-4">
      <slot />
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' },
  description: { type: String, default: '' }
})
</script>
