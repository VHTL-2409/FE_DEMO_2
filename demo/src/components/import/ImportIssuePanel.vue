<template>
  <aside class="staff-surface rounded-[1.5rem] p-4 dark:bg-slate-900">
    <div class="mb-3 flex items-center justify-between gap-3">
      <div>
        <h3 class="text-sm font-bold text-slate-900 dark:text-white">Vấn đề cần rà soát</h3>
        <p class="text-xs text-slate-500 dark:text-slate-400">{{ unresolvedCount }} chưa xử lý</p>
      </div>
      <span class="inline-flex items-center rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-600 dark:bg-slate-800 dark:text-slate-300">
        {{ issues.length }} mục
      </span>
    </div>

    <div v-if="!issues.length" class="rounded-xl border border-dashed border-emerald-200 bg-emerald-50 px-4 py-5 text-center text-sm text-emerald-700 dark:border-emerald-800 dark:bg-emerald-900/20 dark:text-emerald-300">
      Không có vấn đề nào cần review.
    </div>

    <div v-else class="space-y-3">
      <article
        v-for="issue in issues"
        :key="issue.key || issue.id || `${issue.issueType}-${issue.questionIndex}`"
        :class="issue.resolved ? 'border-emerald-200 bg-emerald-50/80 dark:border-emerald-800 dark:bg-emerald-900/10' : 'border-slate-200 bg-slate-50 dark:border-slate-800 dark:bg-slate-800/50'"
        class="rounded-xl border p-3"
      >
        <div class="flex items-start justify-between gap-3">
          <div>
            <p class="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">{{ issue.issueType }}</p>
            <p class="mt-1 text-sm font-semibold text-slate-900 dark:text-white">Câu {{ issue.questionIndex || '-' }}</p>
          </div>
          <span :class="badgeClass(issue.severity)" class="rounded-full px-2.5 py-1 text-[11px] font-semibold">{{ issue.severity }}</span>
        </div>
        <p class="mt-2 text-xs leading-relaxed text-slate-600 dark:text-slate-300">{{ issue.issueData }}</p>
        <button
          v-if="!issue.resolved"
          type="button"
          class="mt-3 inline-flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-700 transition hover:border-primary hover:text-primary dark:border-slate-700 dark:text-slate-200"
          @click="$emit('resolve', issue.id ?? issue.key)"
        >
          <LucideIcon name="task_alt" size="16" />
          Đánh dấu đã xử lý
        </button>
      </article>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  issues: {
    type: Array,
    default: () => []
  }
})

defineEmits(['resolve'])

const unresolvedCount = computed(() => props.issues.filter((issue) => !issue.resolved).length)

const badgeClass = (severity) => {
  const normalized = String(severity || '').toUpperCase()
  if (normalized === 'ERROR') return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300'
  if (normalized === 'WARNING') return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300'
  return 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300'
}
</script>

