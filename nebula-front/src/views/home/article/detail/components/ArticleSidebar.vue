<template>
  <aside class="article-sidebar">
    <!-- 阅读进度 -->
    <div class="card glass-card progress-card">
      <div class="progress-title">
        <ion-icon name="pulse-outline"></ion-icon>
        <span>阅读进度</span>
      </div>

      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progress + '%' }"></div>
      </div>

      <div class="progress-text">{{ progress }}%</div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const progress = ref(0)

/**
 * 计算阅读进度
 */
const updateProgress = () => {
  const article = document.getElementById('article-content')
  if (!article) return
  // 正文在整个页面中的位置
  const articleTop = article.getBoundingClientRect().top + window.scrollY
  // 正文总高度
  const articleHeight = article.scrollHeight
  // 阅读锚点（屏幕下方 1/4，即距顶部 75%）
  // 使用阅读位置而非页面顶部计算进度，更符合博客阅读习惯。
  const anchor = window.scrollY + window.innerHeight * 0.75
  // 已阅读距离
  const read = anchor - articleTop
  // 百分比
  const percent = (read / articleHeight) * 100
  progress.value = Math.max(0, Math.min(100, Number(percent.toFixed(1))))
}

onMounted(() => {
  window.addEventListener('scroll', updateProgress, { passive: true })
  updateProgress()
})

onUnmounted(() => {
  window.removeEventListener('scroll', updateProgress)
})
</script>

<style scoped>
.article-sidebar {
  display: flex;
  flex-direction: column;
  gap: 24px;
  position: sticky;
  top: 96px;
}

/* 卡片基础（局部复用，不依赖全局） */
.card {
  padding: 20px;
  border-radius: 18px;
}

/* glass 风格（统一你评论/导航） */
.glass-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(10px);
}

/* 标题 */
.progress-title {
  display: flex;
  align-items: center;
  gap: 8px;

  font-size: 14px;
  font-weight: 600;
  color: #e5e7eb;

  margin-bottom: 12px;
}

/* 进度条容器 */
.progress-bar {
  width: 100%;
  height: 8px;

  background: rgba(255, 255, 255, 0.06);
  border-radius: 999px;

  overflow: hidden;
}

/* 进度填充 */
.progress-fill {
  height: 100%;
  width: 0;
  background: linear-gradient(90deg, #38bdf8, #0ea5e9);
  border-radius: 999px;

  transition: width 0.25s ease;
}

/* 百分比 */
.progress-text {
  margin-top: 10px;
  font-size: 12px;
  color: #94a3b8;
  text-align: right;
}

/* 移动端 */
@media (max-width: 1200px) {
  .article-sidebar {
    position: static;
  }
}
</style>
