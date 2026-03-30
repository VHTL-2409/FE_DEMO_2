<template>
  <div
    class="portal-viewport min-h-0 flex-1 bg-background-light font-display text-slate-900 dark:bg-background-dark dark:text-slate-100"
  >
    <div class="portal-main-scroll portal-scrollbar mx-auto max-w-4xl px-4 py-8 md:px-8">
      <p class="mb-2 text-xs font-semibold uppercase tracking-wider text-primary">
        Dev only
      </p>
      <PageHeader title="UI kit" />

      <section class="mb-10 space-y-4">
        <h2 class="teacher-section-title text-slate-800 dark:text-slate-100">
          <LucideIcon name="palette" />
          Màu & nền
        </h2>
        <div class="flex flex-wrap gap-3">
          <div class="h-14 w-24 rounded-xl bg-primary shadow-soft" title="primary" />
          <div class="h-14 w-24 rounded-xl bg-primary-100 dark:bg-primary-900/40" title="primary-100" />
          <div class="h-14 w-24 rounded-xl border border-slate-200 bg-background-light dark:border-slate-600 dark:bg-background-dark" />
        </div>
      </section>

      <section class="mb-10 space-y-4">
        <h2 class="teacher-section-title text-slate-800 dark:text-slate-100">
          <LucideIcon name="smart_button" />
          Nút
        </h2>
        <div class="flex flex-wrap gap-3">
          <BaseButton>Primary</BaseButton>
          <BaseButton variant="secondary">Secondary</BaseButton>
          <BaseButton variant="ghost">Ghost</BaseButton>
          <BaseButton variant="danger">Danger</BaseButton>
          <BaseButton loading>Loading</BaseButton>
        </div>
      </section>

      <section class="mb-10 space-y-4">
        <h2 class="teacher-section-title text-slate-800 dark:text-slate-100">
          <LucideIcon name="input" />
          Form
        </h2>
          <BaseCard>
          <form class="max-w-md space-y-5" @submit.prevent>
            <BaseField label="Họ tên" hint="Gợi ý phụ dưới ô" v-slot="{ inputId, hintId, errorId }">
              <BaseInput
                :id="inputId"
                v-model="demoName"
                placeholder="Nguyễn Văn A"
                :hint-id="hintId"
                :error-id="errorId"
              />
            </BaseField>
            <BaseField label="Email" :error="demoError || undefined" v-slot="{ inputId, hintId, errorId }">
              <BaseInput
                :id="inputId"
                v-model="demoEmail"
                type="email"
                :invalid="!!demoError"
                :hint-id="hintId"
                :error-id="errorId"
              />
            </BaseField>
            <BaseButton type="submit">Gửi</BaseButton>
          </form>
        </BaseCard>
      </section>

      <section class="mb-10 space-y-4">
        <h2 class="teacher-section-title text-slate-800 dark:text-slate-100">
          <LucideIcon name="widgets" />
          Card & trạng thái
        </h2>
        <div class="grid gap-4 sm:grid-cols-2">
          <BaseCard class="p-5 shadow-soft">
            <p class="font-semibold text-slate-800 dark:text-slate-100">BaseCard</p>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Nội dung mẫu</p>
          </BaseCard>
          <EmptyState title="Không có dữ liệu" description="EmptyState — dùng trong danh sách trống." />
        </div>
      </section>

      <RouterLink
        to="/login"
        class="inline-flex items-center gap-2 text-sm font-semibold text-primary hover:underline portal-focus rounded-lg"
      >
        <LucideIcon name="arrow_back" size="18" />
        Về đăng nhập
      </RouterLink>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import PageHeader from '../shared/PageHeader.vue'
import BaseButton from '../shared/BaseButton.vue'
import BaseCard from '../shared/BaseCard.vue'
import BaseField from '../shared/BaseField.vue'
import BaseInput from '../shared/BaseInput.vue'
import EmptyState from '../shared/EmptyState.vue'

const demoName = ref('')
const demoEmail = ref('')
const demoError = ref('Ví dụ thông báo lỗi validation')
</script>

