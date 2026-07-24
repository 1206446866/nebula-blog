<template>
  <div class="article-navigation glass-card">
    <!-- 上一篇 -->
    <div class="nav-item" :class="{ disabled: !prevArticle }" @click="todo()">
      <div class="label">上一篇</div>
      <div class="title">
        {{ prevArticle?.title || '没有了' }}
      </div>
    </div>

    <!-- 中间：喜欢 -->
    <div class="center">
      <div class="center-header">
        <ion-icon name="compass-outline"></ion-icon>
      </div>

      <div class="actions">
        <div class="action-item" @click="like">
          <ion-icon :name="articleState.liked ? 'heart' : 'heart-outline'"></ion-icon>
          <span>{{ articleState.liked ? '已喜欢' : '喜欢' }}</span>
        </div>
      </div>
    </div>

    <!-- 下一篇 + 返回 -->
    <div class="right">
      <div class="nav-item" :class="{ disabled: !nextArticle }" @click="todo()">
<!--           @click="goArticle(nextArticle?.id)-->
        <div class="label">下一篇</div>
        <div class="title">
          {{ nextArticle?.title || '没有了' }}
        </div>
      </div>

      <div class="divider"></div>

      <div class="nav-item back" @click="goList">
        <ion-icon name="arrow-back-outline"></ion-icon>
        <span>返回列表</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { getCurrentUserId } from '@/utils/auth.ts'
import { ElMessage } from 'element-plus'
import { type ArticleVO, likeArticleApi } from '@/api/article.ts'
import { ref } from 'vue'
import { todo } from '@/utils/todo.ts'

interface ArticleNavItem {
  id: number
  title: string
}

const props = defineProps<{
  prevArticle?: ArticleNavItem | null
  nextArticle?: ArticleNavItem | null
  article: ArticleVO
}>()
const articleState = ref({
  ...props.article,
})
const router = useRouter()

const goList = () => {
  router.push('/article')
}

// const goArticle = (id?: number) => {
//   if (!id) return
//   router.push(`/article/${id}`)
// }

const like = async () => {
  const userId = Number(getCurrentUserId())

  if (!userId) {
    ElMessage.warning('请先登录')
    return
  }

  const res = await likeArticleApi({
    articleId: props.article.id,
    userId,
  })
  if (res.code === 200) {
    ElMessage.success(res.data?'点赞成功 ❤️':'取消点赞')
    articleState.value.liked = res.data
  } else {
    ElMessage.warning('点赞失败，似乎发生了错误')
  }
}
</script>

<style scoped>
.article-navigation {
  display: flex;
  align-items: center;
  justify-content: space-between;

  padding: 16px 20px;
  border-radius: 18px;
  gap: 20px;
}

/* 左右文章块 */
.nav-item {
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: 0.2s;
  min-width: 140px;
}

.nav-item:hover {
  color: var(--el-color-primary);
}

.nav-item.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* 返回 */
.back {
  flex-direction: row;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}

/* 文本 */
.label {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 4px;
}

.title {
  font-size: 13px;
  font-weight: 500;

  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 中间 */
.center {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.center-header {
  font-size: 18px;
  color: var(--el-color-primary);
}

.actions {
  display: flex;
  align-items: center;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;

  font-size: 12px;
  color: #64748b;

  cursor: pointer;
  transition: 0.2s;
}

.action-item i,
.action-item ion-icon {
  font-size: 18px;
  margin-bottom: 2px;
}

.action-item:hover {
  color: var(--el-color-primary);
  transform: translateY(-1px);
}

/* 右侧 */
.right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.divider {
  width: 1px;
  height: 30px;
  background: rgba(148, 163, 184, 0.3);
}
</style>
