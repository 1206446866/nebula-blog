<script setup lang="ts">
defineOptions({ name: 'ReadingLibraryPage' })
import { formatDateTime } from '@/utils/date'
import { useReadingLibrary } from './logic/useReadingLibrary'
const { kind, page, total, items, loading, failed, acting, clearing, load, remove, clear } =
  useReadingLibrary()
</script>
<template>
  <main class="library-page">
    <header>
      <p class="eyebrow">NEBULA / PERSONAL LIBRARY</p>
      <h1>我的书架<span>。</span></h1>
      <p>收好喜欢的文字，让每一次阅读都有归处。 <RouterLink to="/series">发现系列 ↗</RouterLink></p>
    </header>
    <div class="library-tabs" role="group" aria-label="书架分类">
      <button :aria-pressed="kind === 'favorite'" @click="kind = 'favorite'">收藏</button>
      <button :aria-pressed="kind === 'later'" @click="kind = 'later'">稍后读</button>
      <button :aria-pressed="kind === 'history'" @click="kind = 'history'">阅读历史</button>
      <button
        v-if="kind === 'history'"
        class="clear-history"
        :disabled="clearing || loading"
        @click="clear"
      >
        {{ clearing ? '正在清除…' : '清除历史' }}
      </button>
    </div>
    <p class="privacy-note">
      仅你可见。文章撤回或删除后不会显示；未收藏的阅读历史保留最近 1000 条。
    </p>
    <div v-if="loading" class="library-empty" role="status">正在整理星光…</div>
    <div v-else-if="failed" class="library-empty" role="alert">
      书架暂时无法打开 <button @click="load">重试</button>
    </div>
    <div v-else-if="!items.length" class="library-empty">
      <h2>这里还空着</h2>
      <p>在文章页收藏、加入稍后读，或开始阅读。</p>
      <RouterLink to="/article">去发现文章 ↗</RouterLink>
    </div>
    <div v-else class="library-list">
      <article v-for="(item, index) in items" :key="item.articleId" class="library-item">
        <span class="item-number">{{ String((page - 1) * 20 + index + 1).padStart(2, '0') }}</span>
        <div class="item-body">
          <RouterLink :to="`/article/${item.articleId}`"
            ><h2>{{ item.title }}</h2></RouterLink
          >
          <p>
            {{ item.author
            }}<span v-if="item.state.readAt"> · {{ formatDateTime(item.state.readAt) }}</span>
          </p>
          <div v-if="item.state.readAt" class="reading-track">
            <span :style="{ width: `${item.state.progress / 100}%` }" />
          </div>
          <small v-if="item.state.readAt">{{
            item.state.progress >= 9900
              ? '已读完'
              : `读到 ${Math.round(item.state.progress / 100)}%`
          }}</small>
        </div>
        <button v-if="kind !== 'history'" :disabled="!!acting" @click="remove(item)">
          {{ acting === item.articleId ? '处理中…' : '移出' }}
        </button>
        <RouterLink v-else :to="`/article/${item.articleId}`" class="continue-link"
          >继续 ↗</RouterLink
        >
      </article>
    </div>
    <el-pagination
      v-if="total > 20"
      v-model:current-page="page"
      :page-size="20"
      :total="total"
      layout="prev, pager, next"
      :disabled="loading"
    />
  </main>
</template>
<style scoped>
.library-page {
  min-height: 85vh;
  padding: 120px max(24px, calc((100vw - 1080px) / 2)) 80px;
  background: radial-gradient(ellipse at 80% 5%, #26335544, transparent 55%), #0a1122;
  color: #c7d2e7;
}
.eyebrow {
  font-size: 11px;
  letter-spacing: 0.24em;
  color: #c5ad75;
}
h1 {
  font:
    clamp(32px, 5vw, 50px) Georgia,
    serif;
  color: #f2e9d7;
  margin: 18px 0;
}
h1 span {
  color: #c5ad75;
}
header > p:last-child {
  font-size: 14px;
  color: #8d9eb9;
}
.library-tabs {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 42px;
}
button,
.continue-link {
  border: 1px solid #7185a34a;
  border-radius: 24px;
  padding: 9px 19px;
  font: inherit;
  font-size: 13px;
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-decoration: none;
}
button[aria-pressed='true'] {
  background: #c7ac7222;
  color: #e9d7a4;
  border-color: #c7ac7288;
}
.clear-history {
  margin-left: auto;
}
button:disabled {
  opacity: 0.45;
  cursor: wait;
}
a {
  color: inherit;
  text-decoration: none;
}
a:hover {
  color: #f0d8a0;
}
a:focus-visible,
button:focus-visible {
  outline: 2px solid #d7c08c;
  outline-offset: 4px;
}
.privacy-note {
  font-size: 12px;
  color: #7b8da9;
  margin: 20px 0;
}
.library-item {
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 26px 10px;
  border-bottom: 1px solid #7588ab22;
}
.item-number {
  font:
    20px Georgia,
    serif;
  color: #c5ad7566;
}
.item-body {
  flex: 1;
  min-width: 0;
}
h2 {
  font-size: 19px;
  font-weight: 500;
  overflow-wrap: anywhere;
  margin: 0 0 10px;
}
.item-body p,
small {
  font-size: 12px;
  color: #7f94b3;
}
.reading-track {
  max-width: 220px;
  height: 2px;
  background: #35435a;
  margin: 12px 0 5px;
}
.reading-track span {
  display: block;
  height: 100%;
  background: #bda573;
}
.library-empty {
  padding: 75px 20px;
  text-align: center;
  border: 1px solid #62799b22;
  border-radius: 18px;
  margin-top: 20px;
  color: #8d9eb9;
}
.library-empty a {
  display: inline-block;
  margin-top: 20px;
  color: #e0c898;
}
.el-pagination {
  justify-content: center;
  margin-top: 32px;
}
@media (max-width: 600px) {
  .library-page {
    padding: 100px 20px 60px;
  }
  .library-item {
    gap: 12px;
    padding: 24px 0;
  }
  .item-number {
    display: none;
  }
  h2 {
    font-size: 17px;
  }
  .library-tabs {
    gap: 8px;
  }
  button {
    padding: 8px 13px;
  }
  .clear-history {
    margin-left: 0;
  }
}
</style>
