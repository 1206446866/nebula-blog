<script setup lang="ts">
defineOptions({ name: 'SiteSearchPage' })

import type { SearchResult } from '@/api/search'
import { searchTarget } from '@/utils/site-search'
import { formatDateTime } from '@/utils/date'
import ProfilePagination from '@/views/home/profile/ProfilePagination.vue'

import { useSiteSearch } from './logic/useSiteSearch'

const { keyword, query, result, loading, error, labels, tabs, total, load, navigate, submit } =
  useSiteSearch()
</script>

<template>
  <main class="search-page" :aria-busy="loading">
    <header>
      <span class="eyebrow">NEBULA / SEARCH</span>
      <h1>在星云中，寻找你的答案</h1>
      <p>搜索已发布文章、公开评论、用户、分类与标签。</p>
    </header>
    <form class="search-form" role="search" @submit.prevent="submit">
      <input
        v-model="keyword"
        type="search"
        maxlength="50"
        aria-label="搜索关键词"
        placeholder="输入想找的内容"
      /><button type="submit">搜索</button>
    </form>
    <nav class="search-tabs" aria-label="搜索结果类型">
      <button
        v-for="[type, label] in tabs"
        :key="type"
        type="button"
        :class="{ active: query.type === type }"
        :aria-pressed="query.type === type"
        @click="navigate(type)"
      >
        {{ label }}
        <span>{{
          type === 'all' ? total : (result?.counts[type as keyof SearchResult['counts']] ?? 0)
        }}</span>
      </button>
    </nav>
    <div v-if="error" class="search-status" role="alert">
      {{ error }} <button type="button" @click="load">重试</button>
    </div>
    <p v-else-if="loading" class="search-status" role="status">正在搜索…</p>
    <p v-else-if="!query.q" class="search-status">输入关键词，开始探索站内内容。</p>
    <template v-else-if="result">
      <p class="result-summary">“{{ query.q }}” · 当前分类共 {{ result.page.totalRow }} 条结果</p>
      <div v-if="result.page.records.length" class="result-list">
        <RouterLink
          v-for="item in result.page.records"
          :key="`${item.type}-${item.id}`"
          :to="searchTarget(item)"
          class="result-card"
        >
          <div class="result-meta">
            <span>{{ labels[item.type] }}</span
            ><time v-if="item.createTime">{{ formatDateTime(item.createTime) }}</time>
          </div>
          <h2>{{ item.title || '未命名内容' }}</h2>
          <p>{{ item.excerpt || '暂无简介' }}</p>
          <span class="result-open">查看{{ labels[item.type] }} ↗</span>
        </RouterLink>
      </div>
      <div v-else class="search-status">
        <strong>没有找到相关内容</strong>
        <p>试试更短的关键词，或切换其他分类。</p>
      </div>
      <ProfilePagination
        :current-page="result.page.pageNumber"
        :page-size="12"
        :total="result.page.totalRow"
        @update:current-page="(page) => navigate(query.type, page)"
      />
    </template>
  </main>
</template>

<style scoped>
.search-page {
  max-width: 980px;
  margin: 0 auto;
  padding: 44px 24px 60px;
  color: #dce5f4;
  min-height: calc(100dvh - 64px);
}
.eyebrow {
  color: #a5b4fc;
  letter-spacing: 0.16em;
  font-size: 11px;
}
h1 {
  font-size: clamp(23px, 4vw, 30px);
  font-weight: 600;
  margin: 12px 0;
}
header p,
.result-summary {
  color: #94a3b8;
  font-size: 13px;
  line-height: 1.7;
}
.search-form {
  display: flex;
  gap: 10px;
  margin: 28px 0 22px;
}
.search-form input {
  min-width: 0;
  flex: 1;
  padding: 13px 16px;
  color: #e2e8f0;
  background: #172139;
  border: 1px solid #a6b7df30;
  border-radius: 10px;
  font: inherit;
  font-size: 14px;
}
.search-form button,
.search-status button {
  border: 1px solid #7dd3fc44;
  border-radius: 9px;
  background: #38bdf81a;
  color: #bae6fd;
  padding: 10px 20px;
  cursor: pointer;
}
.search-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 18px;
  border-bottom: 1px solid #a6b7df24;
}
.search-tabs button {
  background: transparent;
  border: 1px solid transparent;
  color: #93a2bc;
  border-radius: 8px;
  padding: 8px 13px;
  cursor: pointer;
}
.search-tabs .active {
  color: #d2c5ff;
  background: #a78bfa18;
  border-color: #a78bfa38;
}
.search-tabs span {
  font-size: 11px;
  margin-left: 5px;
}
.result-list {
  display: grid;
  gap: 14px;
}
.result-card {
  padding: 22px;
  border: 1px solid #b6c6eb1c;
  border-radius: 14px;
  background: linear-gradient(120deg, #18233b88, #211c3480);
  color: inherit;
  text-decoration: none;
  min-width: 0;
}
.result-card:hover {
  border-color: #a5b4fc55;
}
.result-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #9ba8c2;
  font-size: 11px;
}
.result-meta span {
  color: #c4b5fd;
}
.result-card h2 {
  margin: 12px 0;
  font-size: 17px;
  font-weight: 600;
  overflow-wrap: anywhere;
}
.result-card p {
  color: #95a4bc;
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-line;
  overflow-wrap: anywhere;
}
.result-open {
  display: block;
  font-size: 12px;
  color: #a5deff;
  margin-top: 14px;
}
.search-status {
  padding: 48px 20px;
  text-align: center;
  line-height: 1.8;
  color: #94a3b8;
}
@media (max-width: 600px) {
  .search-page {
    padding: 28px 16px;
  }
  .search-tabs {
    gap: 3px;
  }
  .search-tabs button {
    padding: 8px 10px;
  }
}
</style>
