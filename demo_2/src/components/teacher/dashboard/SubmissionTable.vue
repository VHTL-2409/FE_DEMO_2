<template>
  <div class="submission-table">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4">
      <div>
        <h3 class="text-lg font-bold ln-heading-display" style="color: var(--ln-text);">Ky thi gan day</h3>
        <p class="text-xs mt-0.5" style="color: var(--ln-text-muted);">{{ exams.length }} ky thi</p>
      </div>
      <div class="flex gap-2">
        <button class="ln-btn ln-btn--ghost ln-btn--sm" @click="$emit('view-all')">
          Xem tat ca
          <span class="material-symbols-outlined text-base">arrow_forward</span>
        </button>
        <button class="ln-btn ln-btn--primary ln-btn--sm ln-btn--shimmer" @click="$emit('create-exam')">
          <span class="material-symbols-outlined text-base" style="font-variation-settings:'FILL'1">add</span>
          Tao moi
        </button>
      </div>
    </div>

    <!-- Table -->
    <div class="ln-table-wrap">
      <table class="ln-table">
        <thead>
          <tr>
            <th>Ky thi</th>
            <th>Ngay</th>
            <th>Trang thai</th>
            <th>So cau</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="exams.length === 0">
            <td colspan="5" class="text-center py-8">
              <div class="ln-empty__icon mx-auto mb-3" style="width: 3rem; height: 3rem;">
                <span class="material-symbols-outlined text-xl" style="color: var(--ln-text-muted);">inbox</span>
              </div>
              <p class="text-sm" style="color: var(--ln-text-muted);">Chua co ky thi nao</p>
            </td>
          </tr>
          <tr
            v-for="exam in exams"
            :key="exam.id"
            class="cursor-pointer"
            @click="exam.disabled ? null : $emit('view-exam', exam)"
          >
            <td>
              <div>
                <p class="font-semibold" style="color: var(--ln-text);">{{ exam.title }}</p>
                <p class="text-xs mt-0.5" style="color: var(--ln-text-muted);">{{ exam.subtitle || '—' }}</p>
              </div>
            </td>
            <td>
              <span class="text-sm" style="color: var(--ln-text-secondary);">{{ exam.date }}</span>
            </td>
            <td>
              <span class="ln-badge ln-badge--sm" :class="statusBadgeClass(exam.statusKey)">
                {{ exam.status }}
              </span>
            </td>
            <td>
              <span class="ln-mono text-sm" style="color: var(--ln-text-secondary);">{{ exam.participants }}</span>
            </td>
            <td>
              <button
                class="ln-btn ln-btn--ghost ln-btn--sm"
                :disabled="exam.disabled"
                @click.stop="exam.disabled ? null : $emit('view-exam', exam)"
              >
                <span class="material-symbols-outlined text-base">chevron_right</span>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  exams: { type: Array, default: () => [] }
})

defineEmits(['view-exam', 'view-all', 'create-exam'])

const statusBadgeClass = (key) => {
  const map = {
    draft: 'ln-badge--neutral',
    upcoming: 'ln-badge--violet',
    started: 'ln-badge--success',
    ended: 'ln-badge--cyan'
  }
  return map[key] || 'ln-badge--neutral'
}
</script>
