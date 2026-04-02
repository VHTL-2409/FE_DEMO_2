<template>
  <div :class="rootClass" :title="alt">
    <img
      v-if="src"
      :src="src"
      :alt="alt"
      class="avatar-img"
      @error="onError"
    />
    <span v-else class="avatar-initials">
      {{ initials }}
    </span>
    <span
      v-if="status"
      :class="['avatar-status', `avatar-status--${status}`]"
      aria-label="Trạng thái"
    />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  src: { type: String, default: '' },
  name: { type: String, default: '' },
  alt: { type: String, default: '' },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['xs', 'sm', 'md', 'lg', 'xl'].includes(v)
  },
  status: {
    type: String,
    default: '',
    validator: (v) => ['', 'online', 'offline', 'busy', 'away'].includes(v)
  }
})

const failed = ref(false)

const initials = computed(() => {
  if (!props.name) return '?'
  const parts = props.name.trim().split(/\s+/)
  if (parts.length === 1) return parts[0][0].toUpperCase()
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
})

const rootClass = computed(() => {
  return ['avatar', `avatar--${props.size}`].filter(Boolean).join(' ')
})

const onError = () => {
  failed.value = true
}
</script>

<style scoped>
.avatar {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--glass-amber-soft), rgba(99,102,241,0.08));
  border: 1.5px solid var(--glass-amber-border, rgba(139,75,0,0.35));
  color: var(--glass-amber, #8d4b00);
  font-family: var(--font-display, 'Space Grotesk', system-ui, sans-serif);
  font-weight: 700;
  flex-shrink: 0;
  overflow: hidden;
  user-select: none;
}

.avatar--xs { width: 1.5rem;  height: 1.5rem;  font-size: 0.5rem;  }
.avatar--sm { width: 2rem;    height: 2rem;    font-size: 0.625rem; }
.avatar--md { width: 2.5rem; height: 2.5rem;  font-size: 0.75rem;  }
.avatar--lg { width: 3rem;   height: 3rem;    font-size: 0.875rem; }
.avatar--xl { width: 4rem;   height: 4rem;    font-size: 1rem;    }

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: inherit;
}

.avatar-initials {
  line-height: 1;
  text-transform: uppercase;
  letter-spacing: -0.02em;
}

.avatar-status {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 0.625rem;
  height: 0.625rem;
  border-radius: 50%;
  border: 2px solid var(--glass-surface-raised, #fff);
}

.avatar-status--online  { background: var(--glass-success, #16a34a); }
.avatar-status--offline { background: var(--glass-text-muted, #a8a29e); }
.avatar-status--busy   { background: var(--glass-danger, #dc2626);  }
.avatar-status--away    { background: var(--glass-warning, #d97706); }
</style>
