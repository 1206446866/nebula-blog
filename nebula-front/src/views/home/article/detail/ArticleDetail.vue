<template>
  <div class="page">
    <!-- Loading -->
    <el-skeleton v-if="articleLoading" :rows="8" animated />

    <!-- Empty -->
    <el-empty v-else-if="!article" description="文章不存在" />

    <!-- Content -->
    <template v-else>
      <!-- 顶部信息 -->
      <ArticleHero
        :author="currentAuthor"
        :article="article"
        :category="currentCategory"
        :tags="currentTags"
      />

      <!-- 主体 -->
      <div class="article-layout">
        <ArticleContent ref="contentRef" :content="article.content" />

        <ArticleSidebar />
      </div>

      <!-- 上一篇 / 下一篇 -->
      <ArticleNavigation :article="article" />

      <!-- 评论 -->
      <ArticleComment :article-id="article.id" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getArticleByIdApi, type ArticleVO } from '@/api/article'
import { listCategoryApi, type CategoryVO } from '@/api/category'
import { listTagApi, type TagVO } from '@/api/tag'
import ArticleHero from './components/ArticleHero.vue'
import ArticleContent from './components/ArticleContent.vue'
import ArticleSidebar from './components/ArticleSidebar.vue'
import ArticleNavigation from './components/ArticleNavigation.vue'
import ArticleComment from './components/ArticleComment.vue'
import { getUserById, type UserVO } from '@/api/user.ts'

const route = useRoute()

const articleId = Number(route.params.id)

const articleLoading = ref(false)

const article = ref<ArticleVO>()
const currentAuthor = ref<UserVO>()
const categories = ref<CategoryVO[]>([])
const tags = ref<TagVO[]>([])

const currentCategory = computed(() =>
  categories.value.find((item) => item.id === article.value?.categoryId),
)

const currentTags = computed(() => {
  const ids = article.value?.tagIds ?? []
  return tags.value.filter((item) => ids.includes(item.id))
})

const fetchArticle = async () => {
  articleLoading.value = true
  try {
    const res = await getArticleByIdApi(articleId)
    if (res.code !== 200){
      currentAuthor.value = undefined
      return
    }
    article.value = res.data
    const author = await getUserById(res.data.userId)
    if (author.code === 200) {
      currentAuthor.value = author.data
    }
  } finally {
    articleLoading.value = false
  }
}

const fetchMeta = async () => {
  const [categoryRes, tagRes] = await Promise.all([listCategoryApi(), listTagApi()])

  if (categoryRes.code === 200) {
    categories.value = categoryRes.data
  }

  if (tagRes.code === 200) {
    tags.value = tagRes.data
  }
}

onMounted(async () => {
  await Promise.all([fetchArticle(), fetchMeta()])
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: var(--nebula-bg);
  padding: 32px 24px 64px;
}

.article-layout {
  width: min(1400px, 100%);
  margin: 32px auto 48px;

  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 32px;

  align-items: start;
}

@media (max-width: 1100px) {
  .article-layout {
    grid-template-columns: 1fr;
  }
}
</style>
