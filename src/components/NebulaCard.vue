<!-- src/components/NebulaCard.vue -->
<template>
  <div class="nebula-card">
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'NebulaCard',
})
</script>

<style scoped>
.nebula-card {
  background: rgba(30, 41, 59, 0.7);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
</style>
