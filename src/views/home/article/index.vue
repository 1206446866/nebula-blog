<template>
  <ArticleHomeHero />
  <ArticleHomeFilter />
  <div v-loading="loading"><ArticleHomeField :articles="articles" /></div>
  <div v-if="loadError" class="load-error">
    <p>文章暂时没有加载出来</p>
    <el-button @click="loadArticles">重新加载</el-button>
  </div>
  <ArticlePagination
    :current-page="currentPage"
    :page-size="pageSize"
    :total="total"
    @update:current-page="changePage"
  />
</template>

<script setup lang="ts">
import ArticleHomeHero from '@/views/home/article/components/ArticleHomeHero.vue'
import ArticleHomeFilter from '@/views/home/article/components/ArticleHomeFilter.vue'
import ArticleHomeField from '@/views/home/article/components/ArticleHomeField.vue'
import ArticlePagination from '@/views/home/article/components/ArticlePagination.vue'

import { useArticleHome } from './logic/useArticleHome'

defineOptions({
  name: 'ArticleHome',
})

const { articles, currentPage, pageSize, total, loading, loadError, loadArticles, changePage } =
  useArticleHome()
</script>
<style scoped>
.load-error {
  text-align: center;
  color: #94a3b8;
  padding: 24px;
}
</style>
