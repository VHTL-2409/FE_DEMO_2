<template>
  <div
    :class="[
      'ds-stat-card ds-surface group relative flex flex-col justify-between overflow-hidden',
      'rounded-[var(--ds-radius-xl)] border border-[var(--ds-border)] p-5 transition-[border-color,box-shadow,transform] duration-150',
      'hover:-translate-y-0.5 hover:shadow-[var(--ds-shadow-md)] hover:border-[var(--ds-primary-border)]',
      accent ? 'ds-stat-card--accent' : ''
    ]"
  >
    <!-- Accent glow for live cards -->
    <div v-if="accent" class="pointer-events-none absolute -right-6 -top-6 size-28 rounded-full opacity-10" :style="{ background: accentGlowColor }" />

    <div class="flex items-start justify-between gap-3">
      <!-- Icon -->
      <div
        :class="[
          'flex size-10 shrink-0 items-center justify-center rounded-[var(--ds-radius-lg)] transition-colors',
          accent
            ? 'text-white'
            : 'bg-[var(--ds-primary-soft)] text-[var(--ds-primary)]'
        ]"
        :style="accent ? { backgroundColor: 'rgba(255,255,255,0.2)' } : {}"
      >
        <LucideIcon :name="icon" size="22" />
      </div>

      <!-- Badge -->
      <span
        v-if="badge"
        class="rounded-full px-2 py-1 text-[10px] font-bold uppercase tracking-wider"
        :style="{ backgroundColor: badgeBg, color: badgeColor }"
      >
        {{ badge }}
      </span>

      <!-- CTA for accent cards -->
      <a
        v-if="accent && to"
        :href="to"
        class="hidden text-[11px] font-bold text-white/90 underline-offset-200 hover:underline sm:block"
        @click.prevent="$router && $router.push(to)"
      >
        Xem chi tiết
      </a>
    </div>

    <div class="mt-4">
      <p class="text-[11px] font-bold uppercase tracking-widest" :class="accent ? 'text-white/70' : 'text-[var(--ds-text-muted)]'">
        {{ label }}
      </p>
      <div class="flex items-baseline gap-2 mt-0.5">
        <p
          class="text-3xl font-extrabold tabular-nums leading-none"
          :class="accent ? 'text-white' : 'text-[var(--ds-text)]'"
        >
          {{ value }}
        </p>
        <span
          v-if="subValue"
          class="text-sm font-medium"
          :class="accent ? 'text-white/60' : 'text-[var(--ds-text-muted)]'"
        >
          {{ subValue }}
        </span>
      </div>
      <p
        v-if="sub"
        class="mt-1.5 text-xs"
        :class="accent ? 'text-white/60' : 'text-[var(--ds-text-muted)]'"
      >
        {{ sub }}
      </p>
    </div>

    <!-- Trend indicator -->
    <div
      v-if="trend !== null && trend !== undefined"
      class="mt-3 flex items-center gap-1 text-xs font-semibold"
      :style="{ color: trendColor }"
    >
      <LucideIcon :name="trend > 0 ? 'trending_up' : 'trending_down'" size="14" />
      <span>{{ Math.abs(trend) }}% so với tuần trước</span>
    </div>

    <!-- Sub-card highlight strip for accent -->
    <div
      v-if="accent"
      class="absolute bottom-0 left-0 right-0 h-1 rounded-b-[var(--ds-radius-xl)] opacity-40"
      :style="{ background: accentGlowColor }"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  subValue: { type: String, default: '' },
  sub: { type: String, default: '' },
  icon: { type: String, default: 'analytics' },
  badge: { type: String, default: '' },
  badgeVariant: { type: String, default: 'primary' },
  accent: { type: Boolean, default: false },
  accentColor: { type: String, default: '#4f46e5' },
  trend: { type: Number, default: null },
  to: { type: String, default: '' }
})

const badgeBg = computed(() => {
  const map = {
    primary: 'var(--ds-primary-soft)',
    success: 'var(--ds-success-bg)',
    warning: 'var(--ds-accent-bg)',
    danger: 'var(--ds-danger-bg)',
    info: 'var(--ds-info-bg)'
  }
  return map[props.badgeVariant] || map.primary
})

const badgeColor = computed(() => {
  const map = {
    primary: 'var(--ds-primary)',
    success: 'var(--ds-success)',
    warning: 'var(--ds-accent)',
    danger: 'var(--ds-danger)',
    info: 'var(--ds-info)'
  }
  return map[props.badgeVariant] || map.primary
})

const accentGlowColor = computed(() => {
  if (props.accentColor.startsWith('#')) return props.accentColor
  const map = {
    primary: '#4f46e5',
    amber: '#f59e0b',
    success: '#16a34a',
    warning: '#d97706'
  }
  return map[props.accentColor] || '#4f46e5'
})

const trendColor = computed(() => {
  if (props.trend > 0) return 'var(--ds-success)'
  if (props.trend < 0) return 'var(--ds-danger)'
  return 'var(--ds-text-muted)'
})
</script>

