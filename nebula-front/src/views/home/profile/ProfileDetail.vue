<template>
  <div class="profile-detail">
    <!-- 登录记录 -->
    <div class="glass-card login-card">
      <h3 class="card-title">登录记录</h3>

      <div class="timeline">
        <div v-for="record in loginRecords" :key="record.id" class="timeline-item">
          <div class="dot" />

          <div class="timeline-content">
            <div class="timeline-time">
              {{ formatRelativeDay(record.createTime) }}
            </div>

            <div class="timeline-desc">{{ record.device }} · {{ record.browser }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 内容中心 -->
    <div class="glass-card content-card">
      <div class="tab-header">
        <div
          class="tab-item"
          :class="{ active: activeTab === 'article' }"
          @click="activeTab = 'article'"
        >
          我的文章 ({{ props.profile?.publicArticles.totalRow }})
        </div>

        <div
          class="tab-item"
          :class="{ active: activeTab === 'comment' }"
          @click="activeTab = 'comment'"
        >
          我的评论 ({{ props.profile?.comments.totalRow }})
        </div>

        <div
          class="tab-item"
          :class="{ active: activeTab === 'draft' }"
          v-if="isSelf"
          @click="activeTab = 'draft'"
        >
          我的草稿 ({{ props.profile?.draftArticles.totalRow }})
        </div>
      </div>

      <!-- 文章 -->
      <div v-if="activeTab === 'article'" class="content-list">
        <div v-for="article in articles" :key="article.id" class="content-item">
          <div class="title">
            {{ article.title }}
          </div>

          <div class="summary">
            {{ article.content }}
          </div>

          <div class="meta">
            <span>{{ formatDateTime(article.createTime) }}</span>
            <span>👁 {{ article.viewCount }}</span>
            <span>
              <ion-icon :name="'heart-outline'"></ion-icon>
              {{ article.likeCount }}</span
            >
            <span>💬 {{ article.comments }}</span>
          </div>
        </div>
        <ArticlePagination
          :current-page="props.profile?.publicArticles?.pageNumber ?? 1"
          :page-size="5"
          :total="props.profile?.publicArticles?.totalRow ?? 1"
          @update:current-page="changePage('article', $event)"
        />
      </div>

      <!-- 评论 -->
      <div v-else-if="activeTab === 'comment'" class="content-list">
        <div v-for="comment in comments" :key="comment.id" class="content-item">
          <div class="comment-header">
            <div class="title">评论于：《{{ comment.articleTitle }}》</div>

            <div class="comment-time">
              {{ formatDateTime(comment.createTime) }}
            </div>
          </div>

          <div class="comment-content">
            {{ comment.content }}
          </div>

          <div class="meta">
            <ion-icon :name="'heart-outline'"></ion-icon> {{ comment.likeCount }}
          </div>
        </div>
        <ArticlePagination
          :current-page="props.profile?.comments?.pageNumber ?? 1"
          :page-size="5"
          :total="props.profile?.comments?.totalRow ?? 1"
          @update:current-page="changePage('comment', $event)"
        />
      </div>

      <!-- 草稿 -->
      <div v-else class="content-list">
        <div v-for="draft in drafts" :key="draft.id" class="content-item">
          <div class="title">
            {{ draft.title }}
          </div>

          <div class="summary">
            {{ draft.content }}
          </div>

          <div class="meta">
            <span>创建于：{{ formatDateTime(draft.createTime) }}</span>
          </div>
        </div>
        <ArticlePagination
          :current-page="props.profile?.draftArticles?.pageNumber ?? 1"
          :page-size="5"
          :total="props.profile?.draftArticles?.totalRow ?? 1"
          @update:current-page="changePage('draft', $event)"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { UserProfileVO } from '@/api/profile.ts'
import { formatDateTime, formatRelativeDay } from '@/utils/date.ts'
import ArticlePagination from '@/views/home/article/components/ArticlePagination.vue'

defineOptions({
  name: 'ProfileDetail',
})
const props = defineProps<{
  profile: UserProfileVO | null
  isSelf: boolean
}>()
const emit = defineEmits<{
  (e: 'page-change', type: TabType, page: number): void
}>()
function changePage(type: 'article' | 'comment' | 'draft', page: number) {
  emit('page-change', type, page)
}
type TabType = 'article' | 'comment' | 'draft'
const activeTab = ref<TabType>('article')

const loginRecords = computed(() => props.profile?.loginLogs ?? [])

const articles = computed(() => props.profile?.publicArticles?.records ?? [])

const comments = computed(() => props.profile?.comments?.records ?? [])

const drafts = computed(() => props.profile?.draftArticles?.records ?? [])
</script>

<style scoped>
.profile-detail {
  width: 80%;
  margin: 24px auto 40px;
  display: grid;
  grid-template-columns: 280px 1fr;
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
  margin-bottom: 20px;
  font-size: 18px;
  font-weight: 600;
}

/* 时间轴 */

.timeline {
  display: flex;
  flex-direction: column;
  gap: 20px;
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

/* Tabs */

.tab-header {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.tab-item {
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
}

.meta {
  margin-top: 12px;
  display: flex;
  gap: 16px;
  color: #94a3b8;
  font-size: 13px;
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
}
</style>
