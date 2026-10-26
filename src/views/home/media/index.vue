<script setup lang="ts">
import MediaLibrary from '@/components/media/MediaLibrary.vue'
defineOptions({ name: 'MediaLibraryPage' })
</script>
<template>
  <main class="media-page"><MediaLibrary /></main>
</template>
<style scoped>
.media-page {
  min-height: 80vh;
  padding: 60px max(24px, calc((100vw - 1120px) / 2));
  background: radial-gradient(ellipse at top right, #5d427228, transparent 60%), #141321;
}
@media (max-width: 600px) {
  .media-page {
    padding: 30px 16px;
  }
}
</style>
