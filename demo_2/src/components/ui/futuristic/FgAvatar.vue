<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: {
    type: String,
    default: ''
  },
  src: {
    type: String,
    default: ''
  },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg', 'xl'].includes(v)
  },
  status: {
    type: String,
    default: null,
    validator: (v) => [null, 'online', 'offline', 'busy', 'away'].includes(v)
  },
  statusPosition: {
    type: String,
    default: 'bottom-right',
    validator: (v) => ['top-left', 'top-right', 'bottom-left', 'bottom-right'].includes(v)
  }
})

const avatarClasses = computed(() => [
  'fg-avatar',
  `fg-avatar--${props.size}`
])

const initials = computed(() => {
  if (!props.name) return '?'
  const parts = props.name.trim().split(' ')
  if (parts.length >= 2) {
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
  }
  return parts[0].substring(0, 2).toUpperCase()
})

const statusClass = computed(() => {
  if (!props.status) return ''
  return `fg-avatar__status--${props.status}`
})

const statusPositionClass = computed(() => {
  return `fg-avatar__status--${props.statusPosition.replace('-', '-')}`
})
</script>

<template>
  <div class="fg-avatar-wrapper">
    <div :class="avatarClasses">
      <img 
        v-if="src" 
        :src="src" 
        :alt="name" 
        class="fg-avatar__img"
      />
      <span v-else class="fg-avatar__initials">
        {{ initials }}
      </span>
      
      <!-- Status indicator -->
      <span 
        v-if="status"
        class="fg-avatar__status"
        :class="[statusClass, statusPositionClass]"
      />
    </div>
    
    <!-- Name tooltip -->
    <span v-if="name" class="fg-avatar__tooltip">{{ name }}</span>
  </div>
</template>

<style scoped>
.fg-avatar-wrapper {
  position: relative;
  display: inline-flex;
}

.fg-avatar {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-family: 'Manrope', sans-serif;
  font-weight: 700;
  text-transform: uppercase;
  background: linear-gradient(135deg, rgba(0, 245, 255, 0.2), rgba(139, 92, 246, 0.2));
  border: 2px solid rgba(255, 255, 255, 0.1);
  transition: all var(--fg-duration-normal) var(--fg-ease-out);
  overflow: hidden;
}

.fg-avatar::before {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--fg-neon-cyan), var(--fg-neon-violet));
  z-index: -1;
  opacity: 0;
  transition: opacity var(--fg-duration-normal) var(--fg-ease-out);
}

.fg-avatar:hover::before {
  opacity: 0.5;
}

.fg-avatar:hover {
  transform: scale(1.05);
  border-color: rgba(0, 245, 255, 0.3);
}

/* Size variants */
.fg-avatar--sm { 
  width: 32px; 
  height: 32px; 
  font-size: 11px; 
}

.fg-avatar--md { 
  width: 40px; 
  height: 40px; 
  font-size: 13px; 
}

.fg-avatar--lg { 
  width: 56px; 
  height: 56px; 
  font-size: 18px; 
}

.fg-avatar--xl { 
  width: 80px; 
  height: 80px; 
  font-size: 24px; 
}

.fg-avatar__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: inherit;
}

.fg-avatar__initials {
  color: #ffffff;
  text-shadow: 0 0 10px rgba(0, 245, 255, 0.3);
}

/* Status indicator */
.fg-avatar__status {
  position: absolute;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid var(--fg-bg-dark);
}

.fg-avatar__status--online { 
  background: var(--fg-neon-green); 
  box-shadow: 0 0 6px var(--fg-neon-green);
}

.fg-avatar__status--offline { 
  background: rgba(255, 255, 255, 0.3); 
}

.fg-avatar__status--busy { 
  background: var(--fg-neon-red); 
  box-shadow: 0 0 6px var(--fg-neon-red);
}

.fg-avatar__status--away { 
  background: var(--fg-neon-amber); 
  box-shadow: 0 0 6px var(--fg-neon-amber);
}

/* Status positions */
.fg-avatar__status--top-left { top: -2px; left: -2px; }
.fg-avatar__status--top-right { top: -2px; right: -2px; }
.fg-avatar__status--bottom-left { bottom: -2px; left: -2px; }
.fg-avatar__status--bottom-right { bottom: -2px; right: -2px; }

/* Tooltip */
.fg-avatar__tooltip {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 500;
  color: #ffffff;
  background: rgba(10, 10, 20, 0.95);
  border: 1px solid rgba(0, 245, 255, 0.2);
  border-radius: var(--fg-radius-sm);
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  transition: all var(--fg-duration-fast) var(--fg-ease-out);
  z-index: 100;
}

.fg-avatar-wrapper:hover .fg-avatar__tooltip {
  opacity: 1;
  visibility: visible;
}
</style>
