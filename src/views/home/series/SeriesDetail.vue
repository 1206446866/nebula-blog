<script setup lang="ts">
import { useSeriesDetail } from './logic/useSeriesDetail'
const { detail, loading, failed, load } = useSeriesDetail()
</script>
<template>
  <main class="series-detail">
    <RouterLink to="/series" class="back">← 系列专题</RouterLink>
    <p class="eyebrow">NEBULA / A JOURNEY IN CHAPTERS</p>
    <p v-if="loading" role="status">正在展开篇章…</p>
    <div v-else-if="failed" role="alert">
      <h1>暂时无法打开这个系列</h1>
      <p>系列可能尚未公开、已经删除，或网络暂时不可用。</p>
      <el-button @click="load">重新加载</el-button>
    </div>
    <template v-else-if="detail"
      ><h1>{{ detail.series.title }}</h1>
      <p class="description">{{ detail.series.description || '跟随文字，继续这一段探索。' }}</p>
      <p class="chapter-count">
        {{ detail.articles.length }} 个已发布篇章
        {{ detail.series.publicVisible ? '' : '· 私人系列预览' }}
      </p>
      <ol v-if="detail.articles.length">
        <li v-for="(item, index) in detail.articles" :key="item.id">
          <RouterLink
            :to="{ path: `/article/${item.id}`, query: { series: String(detail.series.id) } }"
            ><span>{{ String(index + 1).padStart(2, '0') }}</span>
            <h2>{{ item.title }}</h2>
            <b>↗</b></RouterLink
          >
        </li>
      </ol>
      <p v-else>还没有已发布的篇章，草稿会继续保持私有。</p></template
    >
  </main>
</template>
<style scoped>
.series-detail {
  min-height: 85vh;
  padding: 120px max(24px, calc((100vw - 960px) / 2)) 80px;
  background: radial-gradient(ellipse at 70% 0%, #4e42542b, transparent 60%), #0a1122;
  color: #c7d2e7;
}
.back {
  font-size: 13px;
  color: #8d9eb9;
}
.eyebrow {
  letter-spacing: 0.2em;
  color: #b7a77c;
  font-size: 11px;
  margin-top: 40px;
}
h1 {
  font:
    clamp(28px, 5vw, 46px)/1.4 Georgia,
    serif;
  color: #eae2d2;
  overflow-wrap: anywhere;
}
.description {
  white-space: pre-line;
  line-height: 2;
  font-size: 15px;
  color: #91a2bc;
  overflow-wrap: anywhere;
}
.chapter-count {
  font-size: 12px;
  color: #ab9975;
  margin-top: 35px;
}
ol {
  padding: 0;
  list-style: none;
  margin-top: 25px;
}
li {
  border-bottom: 1px solid #7e90b028;
}
li a {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 26px 0;
  color: inherit;
  text-decoration: none;
}
li span {
  font:
    22px Georgia,
    serif;
  color: #b59c6577;
}
h2 {
  font-size: 19px;
  font-weight: 400;
  flex: 1;
  overflow-wrap: anywhere;
}
li b {
  font-weight: 400;
  color: #b6a47f;
}
li a:hover h2 {
  color: #e0c898;
}
a:focus-visible {
  outline: 2px solid #d1bb88;
  outline-offset: 4px;
}
@media (max-width: 600px) {
  .series-detail {
    padding: 100px 22px 60px;
  }
  li a {
    gap: 14px;
  }
  h2 {
    font-size: 17px;
  }
}
</style>
