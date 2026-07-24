<template>
  <div class="article-comment">
    <!-- 编辑器 -->
    <CommentEditor :articleId="articleId" @success="loadComments" />

    <!-- 列表 -->
    <CommentList :list="list" :userMap="userMap" @delete="handleDelete" />

    <!-- 分页 -->
    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import CommentEditor from '@/views/home/article/components/CommentEditor.vue'
import CommentList from '@/views/home/article/components/CommentList.vue'
import { getCommentPageApi, deleteCommentByIdApi, type CommentVO } from '@/api/comment'
import { getUserByIdsApi, type UserVO } from '@/api/user.ts'

const props = defineProps<{
  articleId: number
}>()

const list = ref<CommentVO[]>([])
const userMap = ref<Map<number, UserVO>>(new Map<number, UserVO>())
/** 分页状态 */
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
/**
 *
 * 加载评论列表
 */
const loadComments = async () => {
  const res = await getCommentPageApi(props.articleId, undefined, page.value, pageSize.value)
  console.log(res)
  if (res.code === 200 && res.data) {
    list.value = res.data.records
    page.value = res.data.pageNumber
    pageSize.value = res.data.pageSize
    total.value = res.data.totalRow
    if(list.value.length===0)return
    // 去重后的用户ID
    const userIds = [...new Set(list.value.map((item) => item.userId))]
    const users = await getUserByIdsApi(userIds)
    if (users.code === 200) {
      userMap.value = new Map(users.data.map((u:UserVO) => [u.id, u]))
    }
  } else {
    list.value = []
  }
}

/**
 * 删除评论
 */
const handleDelete = async (id: number) => {
  const res = await deleteCommentByIdApi(id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    await loadComments()
  } else {
    ElMessage.error(res.msg || '删除失败')
  }
}
/**
 * 切换分页
 */
const handlePageChange = (p: number) => {
  page.value = p
  loadComments()
}

onMounted(() => {
  loadComments()
})
</script>

<style scoped>
.article-comment {
  margin-top: 32px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.pagination {
  display: flex;
  justify-content: center;
  margin-top: 12px;
  padding: 10px 0;
}

/* 覆盖 Element Plus 分页样式 */
.pagination :deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-button-color: #94a3b8;
  --el-pagination-hover-color: #38bdf8;
}

/* 页码按钮 */
.pagination :deep(.el-pager li) {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;

  color: #94a3b8;
  transition: all 0.2s ease;
  margin: 0 3px;
}

/* hover */
.pagination :deep(.el-pager li:hover) {
  color: #38bdf8;
  border-color: rgba(56, 189, 248, 0.4);
  transform: translateY(-1px);
}

/* 当前页 */
.pagination :deep(.el-pager li.is-active) {
  background: rgba(56, 189, 248, 0.15);
  border-color: rgba(56, 189, 248, 0.5);
  color: #38bdf8;
  font-weight: 600;
}

/* 上一页 / 下一页 */
.pagination :deep(.btn-prev),
.pagination :deep(.btn-next) {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;

  color: #94a3b8;
  transition: all 0.2s ease;
}

/* hover */
.pagination :deep(.btn-prev:hover),
.pagination :deep(.btn-next:hover) {
  color: #38bdf8;
  border-color: rgba(56, 189, 248, 0.4);
  transform: translateY(-1px);
}
</style>
