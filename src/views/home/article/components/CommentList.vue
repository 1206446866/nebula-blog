<template>
  <div class="list">
    <h3 class="title">评论（{{ list.length }}）</h3>

    <div v-if="list.length === 0" class="empty">暂无评论</div>

    <div v-for="item in list" :key="item.id" class="comment-card">
      <!-- 顶部信息 -->
      <div class="top">
        <div class="left">
          <el-avatar size="small" class="avatar" :src="avatarUrl(userMap.get(item.userId)?.avatar)">
            {{ userMap.get(item.userId)?.username.charAt(0) }}
          </el-avatar>

          <span class="name">
            {{ userMap.get(item.userId)?.username }}
          </span>
        </div>

        <span class="time">
          {{ formatDateTime(item.createTime) }}
        </span>
      </div>

      <!-- 内容 -->
      <div class="content">
        {{ item.content }}
      </div>

      <!-- 操作区 -->
      <div class="actions">
        <div class="left-actions">
          <span class="like" @click="likeComment(item)">
            <ion-icon :name="item.liked ? 'heart' : 'heart-outline'"></ion-icon>
            <span>{{ item.liked ? '已喜欢' : '喜欢' }}</span>
          </span>
          <!--          <span class="reply" @click="todo()">回复</span>-->
        </div>

        <el-button
          v-if="canDelete(item)"
          type="danger"
          text
          size="small"
          class="delete-btn"
          @click="emit('delete', item.id)"
        >
          删除
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { type CommentVO, likeCommentApi } from '@/api/comment'
import { getCurrentUserId } from '@/utils/auth'
import { useAuthStore } from '@/stores/auth.ts'
import { formatDateTime } from '@/utils/date.ts'
import type { UserVO } from '@/api/user.ts'
import { ElMessage } from 'element-plus'

defineProps<{
  list: CommentVO[]
  userMap: Map<number, UserVO>
}>()
const emit = defineEmits<{
  delete: [id: number]
}>()
const store = useAuthStore()
const avatarUrl = (avatar?: string) => {
  if (!avatar) return ''
  return `http://localhost:8080/upload/avatar/${avatar}`
}
const canDelete = (comment: CommentVO) => {
  return store.hasRole('ADMIN') || comment.userId === Number(getCurrentUserId())
}

const likeComment = async (comment: CommentVO) => {
  const userId = Number(getCurrentUserId())

  if (!userId) {
    ElMessage.warning('请先登录')
    return
  }

  const res = await likeCommentApi({
    commentId: comment.id,
    userId,
  })

  if (res.code === 200) {
    comment.liked = res.data

    ElMessage.success(res.data ? '点赞成功 ❤️' : '取消点赞')
  } else {
    ElMessage.warning('操作失败')
  }
}
</script>

<style scoped>
.list {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: #e5e7eb;
  margin-bottom: 8px;
}

/* 卡片 */
.comment-card {
  padding: 14px 16px;
  border-radius: 14px;

  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);

  transition: all 0.2s ease;
}

.comment-card:hover {
  background: rgba(56, 189, 248, 0.06);
  transform: translateY(-1px);
}

/* 顶部 */
.top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  background: rgba(56, 189, 248, 0.2);
  color: #38bdf8;
  font-weight: bold;
}

.name {
  color: #38bdf8;
  font-size: 14px;
  font-weight: 600;
}

.time {
  font-size: 12px;
  color: #94a3b8;
}

/* 内容 */
.content {
  color: #e5e7eb;
  font-size: 14px;
  line-height: 1.6;

  word-break: break-word;
}

/* 操作区 */
.actions {
  margin-top: 10px;

  display: flex;
  justify-content: space-between;
  align-items: center;

  font-size: 13px;
  color: #94a3b8;
}

.left-actions {
  display: flex;
  gap: 14px;
}

.like {
  display: inline-flex;
  align-items: center;
  gap: 4px;

  cursor: pointer;
  color: #64748b;
  transition: all 0.2s;
}

.like ion-icon {
  font-size: 16px;
}

.like:hover {
  color: var(--el-color-danger);
}

.delete-btn {
  padding: 0;
}

/* empty */
.empty {
  padding: 20px 0;
  color: #64748b;
  font-size: 14px;
}
</style>
