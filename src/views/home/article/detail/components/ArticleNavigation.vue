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
        <button
          type="button"
          class="action-item"
          :disabled="liking || article.status !== 1"
          :aria-pressed="!!articleState.liked"
          @click="like"
        >
          <ion-icon :name="articleState.liked ? 'heart' : 'heart-outline'"></ion-icon>
          <span>{{
            article.status !== 1
              ? '发布后可点赞'
              : liking
                ? '处理中…'
                : articleState.liked
                  ? '已喜欢'
                  : '喜欢'
          }}</span>
        </button>
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
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { type ArticleVO, likeArticleApi } from '@/api/article.ts'
import { ref, watch } from 'vue'
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
const liking = ref(false)
const auth = useAuthStore()
watch(
  () => props.article,
  (article) => {
    articleState.value = { ...article }
  },
)
const router = useRouter()

const goList = () => {
  router.push('/article')
}

// const goArticle = (id?: number) => {
//   if (!id) return
//   router.push(`/article/${id}`)
// }

const like = async () => {
  if (liking.value || props.article.status !== 1) return
  if (!auth.getToken()) {
    ElMessage.warning('请先登录')
    return
  }

  const articleId = props.article.id
  liking.value = true
  try {
    const res = await likeArticleApi({ articleId })
    if (props.article.id === articleId && res.code === 200) {
      articleState.value.liked = res.data
      ElMessage.success(res.data ? '点赞成功 ❤️' : '取消点赞')
    }
  } catch {
    // The request interceptor displays errors; failed requests do not change the heart state.
  } finally {
    liking.value = false
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
  background: transparent;
  border: 0;
  padding: 4px 8px;
  font: inherit;
  display: flex;
  flex-direction: column;
  align-items: center;

  font-size: 12px;
  color: #64748b;

  cursor: pointer;
  transition: 0.2s;
}

.action-item:disabled {
  opacity: 0.55;
  cursor: not-allowed;
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
