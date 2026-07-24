<template>
  <div class="tag-page">
    <!-- Header -->
    <div class="header">
      <h1 class="title">标签系统</h1>
      <p class="subtitle">点击标签，快速过滤文章</p>
    </div>

    <!-- Tag Grid -->
    <div class="tag-grid">
      <div v-for="tag in tags" :key="tag.id" class="tag-card" @click="goToTagArticles(tag.id)">
        <div class="glow"></div>

        <div class="tag-name">
          {{ tag.name }}
        </div>

        <div class="tag-desc">
          {{ tag.description || '暂无描述' }}
        </div>

        <div class="tag-footer">
          <span>{{ tag.articleCount ?? 0 }} 篇文章</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getTagHome, type TagVO } from '@/api/tag'
import { useArticleFilterStore } from '@/stores/articleFilter.ts'

defineOptions({
  name: 'TagPage',
})

const router = useRouter()

const tags = ref<TagVO[]>([])

const fetchTags = async () => {
  const res = await getTagHome()
  if (res.code === 200) {
    tags.value = res.data
  }
}

const filterStore = useArticleFilterStore()

const goToTagArticles = (tagId: number) => {
  filterStore.addTag(tagId)
  router.push('/article')
}

onMounted(() => {
  fetchTags()
})
</script>

<style scoped>
.tag-page {
  min-height: 100vh;
  padding: 60px;
  background:
    radial-gradient(circle at top left, rgba(56, 189, 248, 0.15), transparent 40%),
    radial-gradient(circle at top right, rgba(129, 140, 248, 0.12), transparent 35%), #020617;
}

/* header */
.header {
  margin-bottom: 50px;
}

.title {
  font-size: 52px;
  font-weight: 900;
  background: linear-gradient(90deg, #38bdf8, #818cf8, #22d3ee);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  color: rgba(255, 255, 255, 0.6);
  margin-top: 10px;
}

/* grid */
.tag-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 18px;
}

/* card */
.tag-card {
  position: relative;
  padding: 18px;
  border-radius: 14px;
  cursor: pointer;

  background: rgba(15, 23, 42, 0.75);
  border: 1px solid rgba(56, 189, 248, 0.15);

  transition: 0.3s;
  overflow: hidden;
}

.tag-card:hover {
  transform: translateY(-6px);
  border-color: rgba(56, 189, 248, 0.4);
  box-shadow: 0 0 25px rgba(56, 189, 248, 0.15);
}

.glow {
  position: absolute;
  width: 120px;
  height: 120px;
  background: radial-gradient(circle, rgba(56, 189, 248, 0.25), transparent 70%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  opacity: 0.5;
}

.tag-name {
  font-size: 18px;
  font-weight: 700;
  color: #7dd3fc;
}

.tag-desc {
  font-size: 12px;
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.6);
}

.tag-footer {
  margin-top: 12px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}
</style>
