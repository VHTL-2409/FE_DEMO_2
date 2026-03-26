<template>
  <header
    class="mb-6 flex flex-col gap-3 md:mb-8 sm:flex-row sm:items-end sm:justify-between sm:gap-4"
  >
    <div class="min-w-0 flex-1">
      <p v-if="eyebrow || $slots.eyebrow" class="staff-kicker mb-2">
        <slot name="eyebrow">{{ eyebrow }}</slot>
      </p>
      <h1 class="teacher-page-title">
        <slot name="title">{{ title }}</slot>
      </h1>
      <p v-if="subtitle || $slots.subtitle" class="teacher-page-lead">
        <slot name="subtitle">{{ subtitle }}</slot>
      </p>
    </div>
    <div
      v-if="$slots.actions"
      class="flex shrink-0 flex-wrap items-center gap-2"
    >
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup>
defineProps({
  eyebrow: { type: String, default: '' },
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' }
})
</script>
