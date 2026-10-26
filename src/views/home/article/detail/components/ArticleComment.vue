<template>
  <section id="article-comments" class="article-comment" aria-label="文章评论">
    <header class="section-heading">
      <span class="eyebrow">02 / READER ECHOES</span>
      <h2>
        读者回声<span>{{ total }} 条讨论</span>
      </h2>
      <p>有些星光，来自相遇。<br />把你的想法，留在这段文字之后。</p>
      <span class="conversation-rule" aria-hidden="true" />
    </header>
    <div class="conversation-body">
      <CommentEditor :key="articleId" :article-id="articleId" @success="commentSent" />
      <div v-loading="loading" class="comments-content" :aria-busy="loading">
        <div v-if="error" class="comments-empty">
          <p>评论暂时没有加载出来</p>
          <el-button text @click="loadComments()">重新加载</el-button>
        </div>
        <CommentList
          v-else-if="list.length"
          :list="list"
          :article-id="articleId"
          :author-id="authorId"
          @refresh="loadComments()"
        />
        <div v-else-if="!loading" class="comments-empty">
          <span class="quiet-wave" aria-hidden="true"><i /><i /><i /><i /><i /></span>
          <p>这里还很安静，留下第一段回声吧。</p>
        </div>
      </div>
      <el-pagination
        v-if="total > pageSize"
        class="pagination"
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        :disabled="loading"
        @current-change="handlePageChange"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import CommentEditor from '@/views/home/article/components/CommentEditor.vue'
import CommentList from '@/views/home/article/components/CommentList.vue'

import { useArticleComment } from '../logic/useArticleComment'

const props = defineProps<{ articleId: number; authorId?: number }>()

const { list, page, pageSize, total, loading, error, loadComments, commentSent, handlePageChange } =
  useArticleComment(props)
</script>

<style scoped>
.article-comment {
  display: grid;
  grid-template-columns: 230px minmax(0, 1fr);
  gap: 70px;
  margin-top: 95px;
  padding-top: 58px;
  border-top: 1px solid #bfa6d92b;
  scroll-margin-top: 36px;
}
.section-heading {
  padding-top: 4px;
}
.eyebrow {
  color: #897398;
  font:
    8px/1.5 Consolas,
    monospace;
  letter-spacing: 0.15em;
}
.section-heading h2 {
  font:
    30px/1.5 'Noto Serif SC',
    'Songti SC',
    SimSun,
    Georgia,
    serif;
  letter-spacing: 0.07em;
  font-weight: 400;
  color: #dfd2e9;
  margin: 18px 0;
}
.section-heading h2 span {
  display: block;
  font:
    10px/1.8 Consolas,
    sans-serif;
  letter-spacing: 0.02em;
  color: #8b7a9b;
  margin-top: 12px;
}
.section-heading p {
  color: #8b7f9b;
  font-size: 11px;
  line-height: 2.2;
}
.conversation-rule {
  display: block;
  width: 36px;
  height: 1px;
  background: #ab8fc14a;
  margin-top: 29px;
}
.conversation-body {
  min-width: 0;
}
.comments-content {
  min-height: 100px;
  margin-top: 28px;
}
.comments-empty {
  padding: 35px 12px;
  color: #8a7d99;
  font-size: 11px;
  text-align: center;
}
.quiet-wave {
  display: inline-flex;
  gap: 4px;
  height: 24px;
  align-items: center;
  margin-bottom: 16px;
}
.quiet-wave i {
  width: 1px;
  height: 7px;
  background: #a88ebd66;
}
.quiet-wave i:nth-child(2),
.quiet-wave i:nth-child(4) {
  height: 14px;
}
.quiet-wave i:nth-child(3) {
  height: 23px;
}
.article-comment :deep(.comment-editor) {
  padding: 22px 24px;
  border: 1px solid #b7a2cd29;
  border-radius: 10px;
  background: linear-gradient(130deg, #9c78b30c, #647b9505);
}
.article-comment :deep(.editor-heading) {
  color: #cebfde;
  font-size: 13px;
  margin-bottom: 17px;
  font-weight: 500;
}
.article-comment :deep(.editor-note) {
  color: #7d6d8e;
  font-size: 10px;
}
.article-comment :deep(.el-textarea__inner) {
  color: #dfd5e9;
  background: #080a145e;
  border-radius: 5px;
  box-shadow: 0 0 0 1px #b7a2cd1f inset;
  font-size: 14px;
  line-height: 1.9;
  padding: 15px 17px 25px;
}
.article-comment :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #bba0d366 inset;
}
.article-comment :deep(.el-textarea__inner::placeholder) {
  color: #6d617d;
}
.article-comment :deep(.el-input__count) {
  color: #7e6f8e;
  background: transparent;
  font-size: 10px;
}
.article-comment :deep(.editor-actions > span) {
  color: #7f6f8e;
  font-size: 10px;
}
.article-comment :deep(.editor-actions .el-button) {
  background: #b9a2ce;
  color: #191223;
  border: 1px solid #c4afd5;
  padding: 9px 17px;
  border-radius: 5px;
  font-size: 11px;
  font-weight: 500;
}
.article-comment :deep(.comment-thread) {
  padding: 25px 0;
  border: 0;
  border-bottom: 1px solid #b5a1c91c;
  border-radius: 0;
  background: transparent;
}
.article-comment :deep(.comment-list) {
  gap: 0;
}
.article-comment :deep(.comment-entry .avatar) {
  background: linear-gradient(145deg, #6a4f6d66, #34505b66);
  color: #dbcae8;
  border: 1px solid #b9a2d123;
}
.article-comment :deep(.name) {
  color: #cdbddd;
  font-size: 13px;
  font-weight: 500;
}
.article-comment :deep(.content) {
  color: #bab0ca;
  font-size: 14px;
}
.article-comment :deep(time) {
  color: #776883;
  font-size: 11px;
}
.article-comment :deep(.author-badge) {
  color: #c9addb;
  background: #a88ac71a;
  border-radius: 3px;
}
.article-comment :deep(.entry-actions button) {
  color: #8f7ca0;
  font-size: 12px;
}
.article-comment :deep(.entry-actions button:hover),
.article-comment :deep(.entry-actions .liked) {
  color: #d3b9e4;
}
.article-comment :deep(.expand),
.article-comment :deep(.reply-target span) {
  color: #bba0d0;
}
.article-comment :deep(.thread-line) {
  background: #a58bb754;
}
.article-comment :deep(.replies-body) {
  border-left-color: #ab8fc12b;
}
.article-comment :deep(.reply-row + .reply-row) {
  border-top-color: #b5a1c914;
}
.article-comment :deep(.el-loading-mask) {
  background: #0b0b17cc;
}
.pagination {
  justify-content: flex-end;
  margin-top: 28px;
  --el-pagination-bg-color: transparent;
  --el-pagination-button-bg-color: transparent;
  --el-pagination-text-color: #9a89ab;
  --el-pagination-button-color: #9a89ab;
  --el-pagination-hover-color: #cfb7e1;
}
.pagination :deep(.el-pager li) {
  border-radius: 4px;
  background: #b496cc09;
  margin: 0 4px;
  font-size: 11px;
}
.pagination :deep(.el-pager li.is-active) {
  color: #dfc6ef;
  background: #b496cc1c;
}
@media (max-width: 1100px) {
  .article-comment {
    grid-template-columns: 190px minmax(0, 1fr);
    gap: 40px;
  }
}
@media (max-width: 900px) {
  .article-comment {
    grid-template-columns: minmax(0, 1fr);
    gap: 30px;
    margin-top: 70px;
    padding-top: 40px;
  }
  .section-heading h2 {
    display: flex;
    align-items: baseline;
    gap: 20px;
    margin: 12px 0;
  }
  .section-heading h2 span {
    display: inline;
    margin: 0;
  }
  .section-heading p br {
    display: none;
  }
  .conversation-rule {
    display: none;
  }
}
@media (max-width: 600px) {
  .article-comment {
    gap: 24px;
    margin-top: 56px;
    padding-top: 34px;
  }
  .section-heading h2 {
    font-size: 27px;
  }
  .article-comment :deep(.comment-editor) {
    padding: 17px;
  }
  .article-comment :deep(.editor-note) {
    display: none;
  }
  .article-comment :deep(.editor-actions > span) {
    max-width: 130px;
    line-height: 1.8;
  }
}
</style>
