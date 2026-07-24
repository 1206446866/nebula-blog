<template>
  <section class="hero">
    <div class="hero-content">
      <!-- 标题 -->
      <h1 class="title">
        {{ article.title }}
      </h1>

      <!-- 元信息 -->
      <div class="meta">
        <div class="meta-item author" @click="goAuthor">
          <el-avatar :size="60" :src="avatarUrl">
            {{ author?.username?.charAt(0) }}
          </el-avatar>
          <span>{{ author?.username }}</span>
        </div>

        <div class="meta-item">
          <el-icon><Calendar /></el-icon>
          <span>{{ formatDate(article.createTime) }}</span>
        </div>
      </div>

      <!-- 分类 -->
      <div v-if="category" class="group">
        <span class="group-title">分类</span>

        <el-tag type="primary" size="large" round class="clickable" @click="goCategory">
          {{ category.name }}
        </el-tag>
      </div>

      <!-- 标签 -->
      <div v-if="tags.length" class="group">
        <span class="group-title">标签</span>

        <div class="tag-list">
          <el-tag
            v-for="tag in tags"
            :key="tag.id"
            size="large"
            effect="plain"
            round
            class="clickable"
            @click="goTag(tag.id)"
          >
            {{ tag.name }}
          </el-tag>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { Calendar } from '@element-plus/icons-vue'
import router from '@/router'

import type { ArticleVO } from '@/api/article'
import type { CategoryVO } from '@/api/category'
import type { TagVO } from '@/api/tag'
import type { UserVO } from '@/api/user.ts'
import { computed } from 'vue'

const props = defineProps<{
  article: ArticleVO
  category?: CategoryVO
  tags: TagVO[]
  author?: UserVO
}>()
const avatarUrl = computed(() => {
  const avatar = props.author?.avatar
  if (!avatar) return ''
  return `http://localhost:8080/upload/avatar/${avatar}`
})
const formatDate = (date: string) => dayjs(date).format('YYYY-MM-DD HH:mm:ss')

const goAuthor = () => {
  router.push(`/user/${props.article.userId}`)
}

const goCategory = () => {
  if (!props.category) {
    return
  }

  router.push({
    path: '/article',
    query: {
      categoryId: props.category.id,
    },
  })
}

const goTag = (tagId: number) => {
  router.push({
    path: '/article',
    query: {
      tagId,
    },
  })
}
</script>

<style scoped>
.hero {
  width: min(1200px, 100%);
  margin: 0 auto;
  padding: 64px 16px 40px;
}

.hero-content {
  max-width: 900px;
}

.title {
  margin: 0;
  font-size: 42px;
  font-weight: 800;
  line-height: 1.25;
  color: var(--el-text-color-primary);
}

.meta {
  margin: 24px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  //margin-bottom: 28px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.author {
  cursor: pointer;
  transition: color 0.25s;
}

.author:hover {
  color: var(--el-color-primary);
}

.group {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-top: 18px;
}

.group-title {
  width: 48px;
  flex-shrink: 0;
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.clickable {
  cursor: pointer;
}

@media (max-width: 768px) {
  .hero {
    padding: 40px 20px 28px;
  }

  .title {
    font-size: 30px;
  }

  .group {
    flex-direction: column;
    gap: 10px;
  }

  .group-title {
    width: auto;
    margin: 0;
  }
}
.meta-item.author {
  display: flex;
  align-items: center;
  gap: 15px;
  cursor: pointer;
}
</style>
