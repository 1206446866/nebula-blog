<template>
  <article class="comment-thread">
    <CommentEntry
      :comment="comment"
      :author-id="authorId"
      :disabled="deleting || submitting"
      @reply="startReply"
      @delete="remove"
    >
      <CommentEditor
        v-if="target?.id === comment.id"
        :key="target.id"
        :article-id="articleId"
        :parent-id="target.id"
        :reply-to="target.username"
        :disabled="deleting"
        @busy="submitting = $event"
        @cancel="target = null"
        @success="replySent"
      />
    </CommentEntry>
    <div v-if="replyCount > 0 || expanded" class="thread-replies">
      <button
        type="button"
        class="expand"
        :aria-expanded="expanded"
        :disabled="deleting || submitting"
        @click="toggleExpanded"
      >
        <span class="thread-line"></span>{{ expanded ? '收起回复' : `展开 ${replyCount} 条回复`
        }}<span>{{ expanded ? '−' : '+' }}</span>
      </button>
      <div v-if="expanded" v-loading="loading" class="replies-body">
        <div v-if="error" class="reply-error">
          回复加载失败 <button type="button" @click="loadReplies(replyPage)">重新加载</button>
        </div>
        <template v-else>
          <div v-for="reply in replies" :key="reply.id" class="reply-row">
            <CommentEntry
              :comment="reply"
              :author-id="authorId"
              reply
              :disabled="deleting || submitting || loading"
              @reply="startReply"
              @delete="remove"
            >
              <CommentEditor
                v-if="target?.id === reply.id"
                :key="target.id"
                :article-id="articleId"
                :parent-id="target.id"
                :reply-to="target.username"
                :disabled="deleting"
                @busy="submitting = $event"
                @cancel="target = null"
                @success="replySent"
              />
            </CommentEntry>
          </div>
          <p v-if="!replies.length && !loading" class="reply-empty">还没有回复，接着聊聊吧。</p>
          <el-pagination
            v-if="replyCount > replySize"
            small
            :disabled="deleting || submitting || loading"
            :current-page="replyPage"
            :page-size="replySize"
            :total="replyCount"
            layout="prev, pager, next"
            @current-change="changeReplyPage"
          />
        </template>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { type CommentVO } from '@/api/comment'
import CommentEntry from './CommentEntry.vue'
import CommentEditor from './CommentEditor.vue'

import { useCommentThread } from '../logic/useCommentThread'

const props = defineProps<{ comment: CommentVO; articleId: number; authorId?: number }>()
const emit = defineEmits<{ removed: [] }>()

const {
  expanded,
  loading,
  error,
  deleting,
  submitting,
  target,
  replies,
  replyCount,
  replyPage,
  replySize,
  loadReplies,
  toggleExpanded,
  changeReplyPage,
  startReply,
  replySent,
  remove,
} = useCommentThread(props, emit)
</script>

<style scoped>
.comment-thread {
  padding: 24px;
  border: 1px solid rgba(148, 163, 184, 0.12);
  background: linear-gradient(140deg, rgba(20, 29, 50, 0.6), rgba(12, 19, 35, 0.4));
  border-radius: 18px;
  transition: border-color 0.2s;
}
.comment-thread:hover {
  border-color: rgba(129, 140, 248, 0.26);
}
.thread-replies {
  margin: 14px 0 0 54px;
}
.expand {
  display: flex;
  gap: 10px;
  align-items: center;
  border: 0;
  background: none;
  color: #a5b4fc;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 0;
}
.thread-line {
  width: 22px;
  height: 1px;
  background: rgba(129, 140, 248, 0.4);
}
.expand > span:last-child {
  color: #7886a2;
}
.replies-body {
  min-height: 36px;
  margin-top: 12px;
  border-left: 1px solid rgba(129, 140, 248, 0.16);
  padding-left: 18px;
}
.reply-row {
  padding: 14px 0;
}
.reply-row + .reply-row {
  border-top: 1px solid rgba(148, 163, 184, 0.08);
}
.reply-error,
.reply-empty {
  font-size: 12px;
  color: #8d9bb6;
  padding: 14px 0;
}
.reply-error button {
  border: 0;
  background: none;
  color: #a5b4fc;
  cursor: pointer;
}
.replies-body :deep(.el-pagination) {
  justify-content: flex-end;
  margin: 12px 0 0;
  --el-pagination-bg-color: transparent;
  --el-pagination-button-bg-color: transparent;
  --el-pagination-text-color: #94a3b8;
  --el-pagination-button-color: #94a3b8;
  --el-pagination-hover-color: #a5b4fc;
}
@media (max-width: 600px) {
  .comment-thread {
    padding: 16px;
  }
  .thread-replies {
    margin-left: 0;
  }
  .replies-body {
    padding-left: 12px;
  }
}
</style>
