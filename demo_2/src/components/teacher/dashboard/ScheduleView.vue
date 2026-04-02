<template>
  <div>
    <h3 class="text-lg font-bold ln-heading-display mb-4" style="color: var(--ln-text);">Lich tuan nay</h3>
    <div class="space-y-2">
      <div
        v-for="(item, i) in weekDays"
        :key="i"
        class="schedule-day flex items-center gap-3 p-3 rounded-xl transition-all duration-200"
        :class="item.isToday ? 'ln-glow-card' : 'ln-card'"
        :style="item.isToday ? 'border-color: var(--ln-cyan-border);' : 'border: 1px solid var(--ln-border);'"
      >
        <div class="text-center w-10 flex-shrink-0">
          <p class="text-[10px] font-bold uppercase" style="color: var(--ln-text-muted);">{{ item.dayName }}</p>
          <p class="text-lg font-black ln-heading-display" :style="item.isToday ? 'color: var(--ln-cyan); font-family: var(--ln-font-mono);' : 'color: var(--ln-text); font-family: var(--ln-font-mono);'">{{ item.day }}</p>
        </div>
        <div class="flex-1 min-w-0">
          <div v-if="item.exams.length > 0" class="space-y-1.5">
            <div
              v-for="exam in item.exams.slice(0, 2)"
              :key="exam.id"
              class="flex items-center gap-2 text-xs"
            >
              <span class="w-1.5 h-1.5 rounded-full flex-shrink-0" :style="`background: ${exam.color};`" />
              <span class="font-semibold truncate" style="color: var(--ln-text);">{{ exam.title }}</span>
              <span class="ml-auto text-[10px] font-mono flex-shrink-0" style="color: var(--ln-text-muted);">{{ exam.time }}</span>
            </div>
            <p v-if="item.exams.length > 2" class="text-[10px] font-semibold" style="color: var(--ln-text-muted);">+{{ item.exams.length - 2 }} ky thi khac</p>
          </div>
          <div v-else class="text-xs" style="color: var(--ln-text-faint);">Khong co ky thi</div>
        </div>
        <div v-if="item.isToday" class="flex-shrink-0">
          <span class="ln-badge ln-badge--cyan">Hom nay</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  scheduleSeries: { type: Object, default: () => ({ labels: [], starts: [], ends: [] }) }
})

const weekDays = [
  { dayName: 'T2', day: '30', isToday: false, exams: [] },
  { dayName: 'T3', day: '31', isToday: false, exams: [] },
  { dayName: 'T4', day: '1', isToday: false, exams: [{ id: 1, title: 'Toan 15 phut', time: '08:00', color: 'var(--ln-cyan)' }] },
  { dayName: 'T5', day: '2', isToday: false, exams: [{ id: 2, title: 'Vat ly', time: '09:00', color: 'var(--ln-violet)' }] },
  { dayName: 'T6', day: '3', isToday: true, exams: [{ id: 3, title: 'Hoa hoc CK', time: '10:00', color: 'var(--ln-success)' }] },
  { dayName: 'T7', day: '4', isToday: false, exams: [] },
  { dayName: 'CN', day: '5', isToday: false, exams: [] }
]
</script>
