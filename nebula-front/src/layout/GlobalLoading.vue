<template>
  <div v-if="loadingStore.isLoading" class="mask">
    <el-icon class="spinner">
      <Loading />
    </el-icon>
  </div>
</template>

<script setup lang="ts">
import { useLoadingStore } from '@/stores/loading'
import { Loading } from '@element-plus/icons-vue'

const loadingStore = useLoadingStore()
</script>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.spinner {
  font-size: 40px;
  color: #409eff;
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  100% {
    transform: rotate(360deg);
  }
}
</style>
