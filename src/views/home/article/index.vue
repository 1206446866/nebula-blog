<template>
  <ArticleHomeHero />
  <ArticleHomeFilter />
  <ArticleHomeField :articles="articles" />
  <ArticlePagination v-model:currentPage="currentPage" :page-size="pageSize" :total="total" />
</template>

<script setup lang="ts">
import { useArticleFilterStore } from '@/stores/articleFilter.ts'

defineOptions({
  name: 'ArticleHome',
})
import { onMounted, ref, watch } from 'vue'
import ArticleHomeHero from '@/views/home/article/components/ArticleHomeHero.vue'
import ArticleHomeFilter from '@/views/home/article/components/ArticleHomeFilter.vue'
import ArticleHomeField from '@/views/home/article/components/ArticleHomeField.vue'
import ArticlePagination from '@/views/home/article/components/ArticlePagination.vue'
import { type ArticleVO, getPublishedArticlePageApi } from '@/api/article.ts'
const articles = ref<ArticleVO[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStore = useArticleFilterStore()
async function loadArticles() {
  const res = await getPublishedArticlePageApi({
    categoryIds: filterStore.categoryIds,
    tagIds: filterStore.tagIds,
    current: currentPage.value,
    size: pageSize.value,
  })
  articles.value = res.data.records
  total.value = res.data.totalRow
}

watch(
  () => [filterStore.categoryIds, filterStore.tagIds],
  () => {
    currentPage.value = 1
    loadArticles()
  },
  {
    deep: true,
  },
)
onMounted(() => {
  loadArticles()
})
</script>
