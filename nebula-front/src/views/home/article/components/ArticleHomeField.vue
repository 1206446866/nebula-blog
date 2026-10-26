<template>
  <section class="page">
    <div class="hybrid">
      <div
        v-for="(article, i) in articles"
        :key="article.id"
        class="card"
        :style="{ animationDelay: i * 70 + 'ms' }"
        @click="goDetail(article.id)"
      >
        <!-- 柔和光晕层 -->
        <div class="glow"></div>

        <!-- 文章标题 -->
        <div class="title-row">
          <h3 class="title">
            {{ article.title }}
          </h3>

          <span class="time">
            {{ formatDate(article.createTime) }}
          </span>
        </div>

        <!-- 文章摘要 -->
        <p class="desc">
          {{
            article.content.length > 100
              ? article.content.substring(0, 100) + '...'
              : article.content
          }}
        </p>

        <!-- 文章卡片底部 -->
        <div class="footer">
          <div class="left-info">👁 {{ article.viewCount }}</div>

          <div class="right-info">
            {{ article.author }}
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ArticleVO } from '@/api/article.js'
import { formatDate } from '@/utils/date'

import { useArticleHomeField } from '../logic/useArticleHomeField'

defineProps<{
  articles: ArticleVO[]
}>()

const { goDetail } = useArticleHomeField()
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #0a0d14;
  padding: 70px 0;
  color: white;
}

/* 文章列表容器 */

.hybrid {
  width: min(1280px, calc(100% - 96px));
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  align-items: start;
}

/* 文章卡片 */

.card {
  height: 180px;
  display: flex;
  flex-direction: column;
  position: relative;
  padding: 20px 22px;
  cursor: pointer;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(14px);
  transition: all 0.28s ease;

  overflow: hidden;

  /* 关键：3D基础 */
  transform-style: preserve-3d;
  will-change: transform, opacity, filter;

  /* 使用卡片自身的入场动画。 */
  animation: floatIn 0.7s cubic-bezier(0.2, 0.8, 0.2, 1) both;
}

/* 悬停时轻微抬升并增强光晕 */
.card:hover {
  border-color: rgba(120, 180, 255, 0.28);
  box-shadow: 0 18px 55px rgba(0, 0, 0, 0.45);
  transform: translateY(-4px) scale(1.01);
}

/* 柔和渐变光晕 */
.glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at top left, rgba(120, 180, 255, 0.12), transparent 55%);
  opacity: 0;
  transition: 0.3s ease;
}

.card:hover .glow {
  opacity: 1;
}

/* 标题层级 */

.title {
  font-size: 19px;
  font-weight: 600;
  margin-bottom: 8px;
  letter-spacing: 2px;
  color: rgba(120, 180, 255, 0.72);
}

/* 摘要留白 */

.desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.62);

  line-height: 1.75;

  margin-bottom: 14px;

  max-width: 720px;
}

/* 文章卡片底部 */

.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 入场动画 */

@keyframes floatIn {
  0% {
    opacity: 0;

    transform: perspective(900px) translateY(40px) translateZ(-120px) scale(0.85);

    filter: blur(10px);
  }

  60% {
    opacity: 0.8;

    transform: perspective(900px) translateY(10px) translateZ(20px) scale(1.02);

    filter: blur(2px);
  }

  100% {
    opacity: 1;

    transform: perspective(900px) translateY(0) translateZ(0) scale(1);

    filter: blur(0);
  }
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;

  margin-bottom: 12px;
}

.title {
  flex: 1;

  font-size: 19px;
  font-weight: 600;
  line-height: 1.45;

  letter-spacing: 1px;
}

.time {
  flex-shrink: 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.42);
}

.left-info,
.right-info {
  font-size: 13px;
  color: rgba(120, 180, 255, 0.82);
  transition: color 0.25s ease;
}

.card:hover .left-info,
.card:hover .right-info,
.card:hover .time {
  color: rgba(150, 205, 255, 1);
}
</style>
