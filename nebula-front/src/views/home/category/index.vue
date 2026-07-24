<template>
  <div class="category-page">
    <!-- 顶部 -->
    <div class="page-header">
      <h1 class="page-title">技术分类</h1>

      <div class="search-box">
        <input v-model="keyword" placeholder="搜索分类..." />
      </div>
    </div>

    <!-- 分类列表 -->
    <div class="category-grid">
      <div
        v-for="item in filteredCategories"
        :key="item.id"
        class="category-card"
        @click="goToCategoryArticles(item.id)"
      >
        <div class="card-glow"></div>

        <div class="icon">
          {{ getIcon(item.name) }}
        </div>

        <h2>{{ item.name }}</h2>

        <p>{{ item.description }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useArticleFilterStore } from '@/stores/articleFilter.ts'

defineOptions({
  name: 'CategoryHome',
})
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { type CategoryVO, listCategoryApi } from '@/api/category'
import { ElMessage } from 'element-plus'

const router = useRouter()
const keyword = ref('')
const categories = ref<CategoryVO[]>([])

const fetchCategories = async () => {
  const res = await listCategoryApi()
  if (res.code === 200) {
    categories.value = res.data
  } else {
    ElMessage.error(res.message)
  }
}

onMounted(fetchCategories)

const filteredCategories = computed(() => {
  if (!keyword.value) return categories.value
  return categories.value.filter(
    (item) =>
      item.name.includes(keyword.value) ||
      (item.description && item.description.includes(keyword.value)),
  )
})

// 给不同分类分配默认图标
const getIcon = (name: string) => {
  const map: Record<string, string> = {
    前端: '💻',
    后端: '⚙️',
    数据库: '🗄️',
    云与运维: '☁️',
    人工智能: '🤖',
    安全: '🔐',
  }
  return map[name] || '📂'
}

// 点击分类跳转文章列表页
const filterStore = useArticleFilterStore()
const goToCategoryArticles = (categoryId: number) => {
  filterStore.addCategory(categoryId)
  router.push('/article')
}
</script>

<style scoped>
.category-page {
  min-height: 100vh;
  padding: 60px;
  background:
    radial-gradient(circle at top left, rgba(56, 189, 248, 0.15), transparent 40%),
    radial-gradient(circle at top right, rgba(129, 140, 248, 0.12), transparent 35%), #020617;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 60px;
}

.page-title {
  font-size: 56px;
  font-weight: 900;
  background: linear-gradient(90deg, #38bdf8, #818cf8, #22d3ee);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow:
    0 0 20px rgba(56, 189, 248, 0.35),
    0 0 40px rgba(129, 140, 248, 0.25);
}

.search-box input {
  width: 320px;
  height: 52px;
  border-radius: 14px;
  border: 1px solid rgba(56, 189, 248, 0.2);
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(16px);
  padding: 0 20px;
  color: white;
  font-size: 15px;
  transition: 0.3s;
}
.search-box input:focus {
  outline: none;
  border-color: rgba(56, 189, 248, 0.5);
  box-shadow:
    0 0 0 1px rgba(56, 189, 248, 0.5),
    0 0 25px rgba(56, 189, 248, 0.2);
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 30px;
}

.category-card {
  position: relative;
  overflow: hidden;
  height: 240px;
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.95), rgba(2, 6, 23, 0.85));
  border: 1px solid rgba(56, 189, 248, 0.18);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  transition: 0.4s;
  cursor: pointer;
}

.card-glow {
  position: absolute;
  width: 280px;
  height: 280px;
  background: radial-gradient(circle, rgba(56, 189, 248, 0.22), transparent 70%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  opacity: 0.4;
  transition: 0.4s;
}

.category-card::before {
  content: '';
  position: absolute;
  top: -150px;
  left: -50px;
  width: 220px;
  height: 220px;
  background: radial-gradient(circle, rgba(56, 189, 248, 0.25), transparent 70%);
}

.category-card::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 22px;
  padding: 1px;
  background: linear-gradient(120deg, transparent, rgba(56, 189, 248, 0.5), transparent);
  opacity: 0.3;
}

.category-card:hover {
  transform: translateY(-10px);
  border-color: rgba(56, 189, 248, 0.45);
  box-shadow:
    0 0 25px rgba(56, 189, 248, 0.18),
    0 0 60px rgba(56, 189, 248, 0.08);
}

.category-card:hover .card-glow {
  opacity: 0.9;
  transform: translate(-50%, -50%) scale(1.25);
}

.icon {
  font-size: 52px;
  margin-bottom: 12px;
  filter: drop-shadow(0 0 12px rgba(56, 189, 248, 0.5));
}

.category-card h2 {
  color: white;
  font-size: 30px;
  font-weight: 800;
  margin-bottom: 12px;
}

.category-card p {
  width: 85%;
  color: rgba(255, 255, 255, 0.65);
  line-height: 1.7;
  font-size: 14px;
  margin-bottom: 20px;
}
</style>
