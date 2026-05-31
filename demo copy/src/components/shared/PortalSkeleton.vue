<template>
  <!--
    PortalSkeleton — unified loading overlay for Teacher (PortalLayout) and Student (StudentPortalLayout).
    Uses canonical skeleton-shimmer from animation.css so it is never tree-shaken.
    Props mirror the previous layout-level skeleton to maintain exact visual behavior.
  -->
  <Transition name="portal-skeleton">
    <div
      v-if="visible"
      class="portal-skeleton-overlay"
      aria-hidden="true"
    >
      <div class="portal-skeleton-bar portal-skeleton-bar--short" />
      <div class="portal-skeleton-bar" />
      <div class="portal-skeleton-bar portal-skeleton-bar--medium" />
      <div class="portal-skeleton-bar portal-skeleton-bar--short" />
      <div class="portal-skeleton-cards">
        <div class="portal-skeleton-card" v-for="i in 3" :key="i" />
      </div>
    </div>
  </Transition>
</template>

<script setup>
defineProps({
  visible: {
    type: Boolean,
    default: true
  }
})
</script>

<style scoped>
/* Skeleton overlay — inside portal stacking context, above all portal children */
.portal-skeleton-overlay {
  position: fixed;
  inset: 0;
  z-index: 9000;
  background: var(--color-bg);
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 32px 28px;
  pointer-events: none;
}

.portal-skeleton-bar {
  height: 18px;
  border-radius: 8px;
  background: linear-gradient(
    90deg,
    var(--color-border) 0%,
    var(--color-surface-muted) 40%,
    var(--color-border) 80%
  );
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.4s ease-in-out infinite;
  width: 100%;
}
.portal-skeleton-bar--short { width: 45%; }
.portal-skeleton-bar--medium { width: 70%; }

.portal-skeleton-cards {
  display: flex;
  gap: 16px;
  margin-top: 8px;
}
.portal-skeleton-card {
  flex: 1;
  height: 120px;
  border-radius: 12px;
  background: linear-gradient(
    90deg,
    var(--color-border) 0%,
    var(--color-surface-muted) 40%,
    var(--color-border) 80%
  );
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.4s ease-in-out infinite;
}

/* Fade skeleton — leave duration matches page-enter (280ms) so the overlay
   never lingers after the new page is fully visible */
.portal-skeleton-enter-active { transition: opacity 0.2s ease; }
.portal-skeleton-leave-active { transition: opacity 0.28s ease; }
.portal-skeleton-enter-from,
.portal-skeleton-leave-to { opacity: 0; }

@media (prefers-reduced-motion: reduce) {
  .portal-skeleton-bar,
  .portal-skeleton-card {
    animation: none;
  }
}
</style>
