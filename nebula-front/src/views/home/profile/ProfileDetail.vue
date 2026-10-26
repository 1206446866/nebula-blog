<script setup lang="ts">
import { RouterLink } from 'vue-router'
import type { UserProfileVO } from '@/api/profile.ts'
import { formatDateTime, formatRelativeDay } from '@/utils/date.ts'
import ProfilePagination from './ProfilePagination.vue'

import { useProfileDetail, type TabType } from './logic/useProfileDetail'

defineOptions({ name: 'ProfileDetail' })
const props = defineProps<{ profile: UserProfileVO | null; isSelf: boolean; loading: boolean }>()
const emit = defineEmits<{ 'page-change': [type: TabType, page: number] }>()

const {
  activeTab,
  auth,
  tabId,
  tabs,
  activePage,
  articles,
  comments,
  drafts,
  loginRecords,
  emptyText,
  changePage,
  handleTabKey,
} = useProfileDetail(props, emit)
</script>

<template>
  <div class="profile-detail" :class="{ 'is-public': !isSelf }">
    <aside v-if="isSelf" class="glass-card login-card" aria-label="最近登录记录">
      <h3 class="card-title">登录记录<span class="private-label">仅自己可见</span></h3>
      <div v-if="loginRecords.length" class="timeline">
        <div v-for="record in loginRecords" :key="record.id" class="timeline-item">
          <div class="dot" :class="{ 'failed-login': record.status !== 1 }" />
          <div class="timeline-content">
            <div class="timeline-time">{{ formatRelativeDay(record.createTime) }}</div>
            <div class="timeline-desc">
              {{ record.device || '未知设备' }} · {{ record.browser || '未知浏览器'
              }}<span v-if="record.status !== 1" class="login-failure">登录未成功</span>
            </div>
          </div>
        </div>
      </div>
      <p v-else class="empty-login">暂无登录记录</p>
    </aside>

    <section class="glass-card content-card" :aria-busy="loading" aria-label="用户内容">
      <div v-if="isSelf && auth.hasPermission('article:create')" class="writing-entry">
        <span>把灵感写成下一篇文章</span>
        <RouterLink to="/write">写文章 <span aria-hidden="true">↗</span></RouterLink>
      </div>
      <div class="tab-header" role="tablist" aria-label="用户内容类型">
        <button
          v-for="(tab, index) in tabs"
          :id="`${tabId}-${tab.id}`"
          :key="tab.id"
          type="button"
          class="tab-item"
          :class="{ active: activeTab === tab.id }"
          role="tab"
          :aria-selected="activeTab === tab.id"
          :aria-controls="`${tabId}-panel`"
          :tabindex="activeTab === tab.id ? 0 : -1"
          @click="activeTab = tab.id"
          @keydown="handleTabKey($event, index)"
        >
          {{ tab.label }}<span class="tab-count">{{ tab.count }}</span>
        </button>
      </div>
      <div
        :id="`${tabId}-panel`"
        class="content-list"
        role="tabpanel"
        :aria-labelledby="`${tabId}-${activeTab}`"
      >
        <template v-if="activeTab === 'article'">
          <article v-for="article in articles" :key="article.id" class="content-item">
            <RouterLink class="title content-link" :to="`/article/${article.id}`">{{
              article.title
            }}</RouterLink>
            <p class="summary">{{ article.content || '暂无摘要' }}</p>
            <div class="meta">
              <span>{{ formatDateTime(article.createTime) }}</span
              ><span aria-label="浏览量">◉ {{ article.viewCount ?? 0 }}</span
              ><span aria-label="点赞数">♡ {{ article.likeCount ?? 0 }}</span
              ><span aria-label="评论数">💬 {{ article.comments ?? 0 }}</span>
              <RouterLink
                v-if="isSelf && auth.hasPermission('article:update')"
                class="edit-link"
                :to="`/write/${article.id}`"
                >编辑文章</RouterLink
              >
            </div>
          </article>
        </template>
        <template v-else-if="activeTab === 'comment'">
          <article v-for="comment in comments" :key="comment.id" class="content-item">
            <div class="comment-header">
              <RouterLink
                v-if="comment.articleTitle"
                class="title content-link"
                :to="`/article/${comment.articleId}`"
                >评论于：《{{ comment.articleTitle }}》</RouterLink
              ><span v-else class="title">文章已删除或不可访问</span
              ><time class="comment-time">{{ formatDateTime(comment.createTime) }}</time>
            </div>
            <p class="comment-content">{{ comment.content }}</p>
            <div class="meta">
              <span aria-label="点赞数">♡ {{ comment.likeCount ?? 0 }}</span>
            </div>
          </article>
        </template>
        <template v-else-if="isSelf">
          <article v-for="draft in drafts" :key="draft.id" class="content-item">
            <h3 class="title">{{ draft.title }}</h3>
            <p class="summary">{{ draft.content || '暂无摘要' }}</p>
            <div class="meta">
              <span>创建于：{{ formatDateTime(draft.createTime) }}</span
              ><span class="draft-badge">未发布</span>
              <span v-if="draft.scheduledPublishAt" class="draft-badge">{{
                draft.scheduleError
                  ? '定时发布已暂停'
                  : `定时：${formatDateTime(draft.scheduledPublishAt)}（上海）`
              }}</span>
              <template v-if="auth.hasPermission('article:update')">
                <RouterLink class="edit-link" :to="`/write/${draft.id}`">继续编辑</RouterLink>
                <RouterLink
                  class="edit-link"
                  :to="{ path: `/write/${draft.id}`, query: { mode: 'preview' } }"
                  >预览</RouterLink
                >
              </template>
            </div>
          </article>
        </template>
        <div v-if="!activePage?.records.length" class="content-empty">
          <svg viewBox="0 0 40 40" fill="none" aria-hidden="true">
            <path d="M10 6h14l7 7v21H10Z" />
            <path d="M24 6v8h7M15 21h11m-11 6h7" />
          </svg>
          <p>{{ emptyText }}</p>
          <span>有新的内容时，会在这里与你相遇。</span>
        </div>
      </div>
      <ProfilePagination
        :key="activeTab"
        :current-page="activePage?.pageNumber ?? 1"
        :page-size="activePage?.pageSize ?? 5"
        :total="activePage?.totalRow ?? 0"
        :loading="loading"
        @update:current-page="changePage"
      />
    </section>
  </div>
</template>
<style scoped>
.profile-detail {
  width: 80%;
  margin: 24px auto 40px;
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  align-items: start;
  gap: 20px;
}

.glass-card {
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 24px;
}

.card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  font-size: 18px;
  font-weight: 600;
}

/* 时间轴 */

.timeline {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-height: 540px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #334155 transparent;
}

.timeline-item {
  display: flex;
  gap: 12px;
}

.dot {
  width: 10px;
  height: 10px;
  margin-top: 6px;
  border-radius: 50%;
  background: #38bdf8;
  flex-shrink: 0;
}

.timeline-time {
  font-weight: 600;
}

.timeline-desc {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 13px;
}

/* 内容页签 */

.tab-header {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid transparent;
  background: transparent;
  color: #94a3b8;
  font: inherit;
  font-size: 13px;
  padding: 10px 18px;
  border-radius: 12px;
  cursor: pointer;
  transition: 0.3s;
}

.tab-item:hover {
  background: rgba(56, 189, 248, 0.08);
}

.tab-item.active {
  background: rgba(56, 189, 248, 0.15);
  border-color: #38bdf830;
  color: #bae6fd;
}

/* 内容 */

.content-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.content-item {
  padding: 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.03);
  transition: 0.3s;
}

.content-item:hover {
  transform: translateY(-2px);
  background: rgba(56, 189, 248, 0.08);
}

.title {
  font-size: 16px;
  font-weight: 600;

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary {
  margin-top: 10px;
  color: #cbd5e1;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
  overflow-wrap: anywhere;
}

.meta {
  margin-top: 12px;
  display: flex;
  gap: 16px;
  color: #94a3b8;
  font-size: 13px;
  flex-wrap: wrap;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.comment-header .title {
  flex: 1;
  min-width: 0;
}

.comment-time {
  flex-shrink: 0;
  font-size: 13px;
  color: #94a3b8;
}

.comment-content {
  margin-top: 12px;
  line-height: 1.7;
  overflow-wrap: anywhere;
}
.profile-detail.is-public {
  grid-template-columns: minmax(0, 1fr);
}
.content-card {
  min-width: 0;
}
.writing-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 20px;
  margin-bottom: 20px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
  color: #94a3b8;
  font-size: 13px;
}
.writing-entry a {
  flex-shrink: 0;
  padding: 8px 13px;
  border: 1px solid rgba(125, 211, 252, 0.22);
  border-radius: 9px;
  color: #bae6fd;
  background: rgba(56, 189, 248, 0.08);
}
.edit-link {
  color: #7dd3fc;
}
.edit-link:hover,
.writing-entry a:hover {
  color: #e0f2fe;
}
.private-label {
  margin-left: 8px;
  font-size: 10px;
  font-weight: 400;
  color: #64748b;
  white-space: nowrap;
}
.failed-login {
  background: #d5a66c;
}
.login-failure {
  display: block;
  margin-top: 4px;
  color: #bd9c76;
  font-size: 11px;
}
.empty-login {
  color: #64748b;
  font-size: 13px;
}
.tab-count {
  color: #71859f;
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}
.tab-item.active .tab-count {
  color: #7dd3fc;
}
.content-link {
  display: block;
  color: #e2e8f0;
  transition: color 0.2s;
}
.content-link:hover {
  color: #7dd3fc;
}
.content-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 240px;
  color: #94a3b8;
}
.content-empty svg {
  width: 38px;
  height: 38px;
  stroke: #526a85;
  stroke-width: 1.2;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.content-empty p {
  font-size: 14px;
  color: #a4b6ce;
}
.content-empty > span {
  font-size: 12px;
  color: #61748e;
}
.draft-badge {
  color: #b5a8dc;
}
button:focus-visible,
a:focus-visible {
  outline: 2px solid #7dd3fc;
  outline-offset: 4px;
}
@media (max-width: 1050px) {
  .profile-detail {
    width: 90%;
    grid-template-columns: 220px minmax(0, 1fr);
  }
  .glass-card {
    padding: 20px;
  }
  .tab-item {
    padding: 10px 12px;
  }
  .tab-header {
    gap: 6px;
  }
  .private-label {
    font-size: 9px;
  }
}
@media (max-width: 760px) {
  .profile-detail {
    width: calc(100% - 32px);
    grid-template-columns: minmax(0, 1fr);
    gap: 16px;
  }
  .content-card {
    order: 0;
  }
  .login-card {
    order: 1;
  }
  .glass-card {
    padding: 18px;
    border-radius: 16px;
  }
  .timeline {
    max-height: 250px;
  }
  .tab-header {
    gap: 4px;
  }
  .tab-item {
    padding: 9px 10px;
    font-size: 12px;
  }
  .content-item {
    padding: 15px;
  }
  .meta {
    gap: 8px 14px;
    font-size: 11px;
  }
  .comment-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }
  .comment-header .title {
    max-width: 100%;
  }
  .comment-time {
    font-size: 11px;
  }
}
</style>
