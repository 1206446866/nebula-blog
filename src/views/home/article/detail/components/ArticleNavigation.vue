<template>
  <section class="article-navigation" aria-label="文章互动">
    <div class="closing-heading">
      <span>THE END, FOR NOW</span>
      <h2>让这次阅读，留下一点回声。</h2>
      <p>如果文字曾与你共鸣，就为它送上一颗爱心。</p>
    </div>
    <div class="closing-actions">
      <button
        type="button"
        class="like-button"
        :class="{ 'is-liked': articleState.liked }"
        :disabled="liking || article.status !== 1"
        :aria-pressed="!!articleState.liked"
        @click="like"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path
            d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21l7.78-7.55 1.06-1.06a5.5 5.5 0 0 0 0-7.78Z"
          /></svg
        ><span>{{
          article.status !== 1
            ? '发布后可点赞'
            : liking
              ? '处理中…'
              : articleState.liked
                ? '已喜欢这篇文章'
                : '喜欢这篇文章'
        }}</span>
      </button>
      <a class="discussion-link" href="#article-comments"
        >参与讨论 <span aria-hidden="true">↓</span></a
      >
    </div>
    <nav class="continuation" aria-label="继续阅读">
      <button type="button" class="back-link" @click="goList">
        <span aria-hidden="true">↖</span> 回到文章星海</button
      ><button
        v-if="prevArticle"
        type="button"
        class="adjacent-link"
        @click="goArticle(prevArticle.id)"
      >
        上一篇 · {{ prevArticle.title }}</button
      ><button
        v-if="nextArticle"
        type="button"
        class="adjacent-link"
        @click="goArticle(nextArticle.id)"
      >
        下一篇 · {{ nextArticle.title }} ↗
      </button>
    </nav>
  </section>
</template>

<script setup lang="ts">
import { type ArticleVO } from '@/api/article.ts'

import { useArticleNavigation, type ArticleNavItem } from '../logic/useArticleNavigation'

const props = defineProps<{
  prevArticle?: ArticleNavItem | null
  nextArticle?: ArticleNavItem | null
  article: ArticleVO
}>()

const { articleState, liking, goList, goArticle, like } = useArticleNavigation(props)
</script>

<style scoped>
.article-navigation {
  margin: 18px 0 0;
  padding: 35px 0 0;
  border-top: 1px solid #bba3d020;
}
.closing-heading {
  text-align: center;
}
.closing-heading > span {
  color: #7c6e8d;
  font:
    8px/1.5 Consolas,
    monospace;
  letter-spacing: 0.2em;
}
.closing-heading h2 {
  font:
    22px/1.8 'Noto Serif SC',
    'Songti SC',
    SimSun,
    Georgia,
    serif;
  font-weight: 400;
  letter-spacing: 0.06em;
  color: #d2c4e0;
  margin: 13px 0 8px;
}
.closing-heading p {
  font-size: 11px;
  line-height: 1.8;
  color: #8d809d;
}
.closing-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 25px;
  margin: 26px 0 40px;
}
.like-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 13px 24px;
  border: 1px solid #bba0d342;
  border-radius: 7px;
  color: #d4c1e6;
  background: linear-gradient(120deg, #a587c01d, #9d83b40a);
  font: inherit;
  font-size: 12px;
  cursor: pointer;
  transition:
    background 0.2s,
    border-color 0.2s;
}
.like-button svg {
  width: 17px;
  height: 17px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.3;
  stroke-linejoin: round;
}
.like-button:hover {
  border-color: #c0a4db80;
  background: #ad8bce26;
}
.like-button.is-liked {
  color: #e3cce1;
  background: #ad8bce26;
}
.like-button.is-liked svg {
  fill: currentColor;
}
.like-button:disabled {
  opacity: 0.5;
  cursor: default;
}
.discussion-link {
  color: #a89bb9;
  font-size: 11px;
}
.discussion-link > span {
  margin-left: 10px;
  color: #c5b2db;
}
.continuation {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding-top: 22px;
  border-top: 1px solid #bba3d01a;
  flex-wrap: wrap;
}
.continuation button {
  border: 0;
  background: transparent;
  padding: 0;
  cursor: pointer;
  color: #8f809f;
  font: inherit;
  font-size: 10px;
  line-height: 1.8;
  text-align: left;
}
.back-link > span {
  color: #c7b6dd;
  margin-right: 9px;
  font-size: 14px;
}
.continuation button:hover,
.discussion-link:hover {
  color: #d7c5e9;
}
.adjacent-link {
  overflow-wrap: anywhere;
  max-width: 40%;
}
button:focus-visible,
a:focus-visible {
  outline: 2px solid #c4b4ed;
  outline-offset: 5px;
}
@media (max-width: 600px) {
  .closing-heading h2 {
    font-size: 20px;
  }
  .closing-actions {
    gap: 20px;
  }
  .like-button {
    padding: 12px 18px;
    font-size: 11px;
  }
  .continuation {
    gap: 15px;
  }
  .adjacent-link {
    max-width: 100%;
  }
}
@media (prefers-reduced-motion: reduce) {
  .like-button {
    transition: none;
  }
}
</style>
