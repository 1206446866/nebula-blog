<script setup lang="ts">
import { computed, ref } from 'vue'
import { useMediaImages } from '@/composables/useMediaImages'
import { renderMarkdown } from '@/utils/markdown'
import '@/styles/markdown.css'

const props = defineProps<{
  title: string
  content: string
  author: string
  category?: string
  tags: string[]
}>()
const html = computed(() => renderMarkdown(props.content))
const body = ref<HTMLElement | null>(null)
useMediaImages(body, html)
const readingTime = computed(() =>
  Math.max(1, Math.ceil(props.content.replace(/\s/g, '').length / 400)),
)
// 此处仅供阅读预览，点击正文链接时保留尚未保存的内容。
function preventNavigation(event: MouseEvent) {
  if ((event.target as Element)?.closest('a')) event.preventDefault()
}
</script>

<template>
  <article class="article-preview">
    <div class="preview-inner">
      <header class="article-heading">
        <p class="preview-label">
          <span>✦</span> ARTICLE PREVIEW <span class="preview-note">仅预览</span>
        </p>
        <div v-if="category" class="category-label">{{ category }}</div>
        <h1 :class="{ placeholder: !title.trim() }">{{ title.trim() || '等待一个闪光的标题' }}</h1>
        <div class="article-meta">
          <span class="author-symbol">{{ author.charAt(0) }}</span
          ><span>{{ author }}</span
          ><span class="meta-divider">/</span><span>约 {{ readingTime }} 分钟阅读</span>
        </div>
        <div v-if="tags.length" class="article-tags">
          <span v-for="(tag, index) in tags" :key="index"># {{ tag }}</span>
        </div>
      </header>
      <div
        v-if="content.trim()"
        ref="body"
        class="preview-body markdown-body"
        @click="preventNavigation"
        v-html="html"
      />
      <div v-else class="empty-content">
        <span>✧</span>
        <p>文字落下，故事就开始了。</p>
        <small>左侧写下正文，这里会实时呈现。</small>
      </div>
      <footer class="preview-footer">
        <span>✦</span> 预览不会自动保存或发布 · 正文链接仅作展示
      </footer>
    </div>
  </article>
</template>

<style scoped>
.article-preview {
  container-type: inline-size;
  color: #dce5f3;
  background: radial-gradient(ellipse at 95% 0%, #6951b626, transparent 45%), #101727;
  min-height: inherit;
  overflow-wrap: anywhere;
}
.preview-inner {
  padding: 38px 36px;
}
.preview-label {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #9b9bc8;
  font-size: 9px;
  letter-spacing: 0.18em;
  margin: 0 0 32px;
}
.preview-label > span:first-child {
  color: #baacf4;
  font-size: 15px;
}
.preview-note {
  margin-left: auto;
  font-size: 10px;
  color: #73829e;
  letter-spacing: 0.05em;
}
.category-label {
  display: inline-block;
  border: 1px solid #a4a3ec30;
  background: #9591d310;
  border-radius: 5px;
  color: #afafe4;
  font-size: 10px;
  padding: 5px 9px;
  margin-bottom: 15px;
}
h1 {
  margin: 0;
  font-size: 34px;
  line-height: 1.45;
  font-weight: 650;
  letter-spacing: 0.015em;
  color: #eef1fc;
}
h1.placeholder {
  color: #63738f;
}
.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin: 22px 0 0;
  color: #8e9fb8;
  font-size: 11px;
}
.author-symbol {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(145deg, #46416b, #26394e);
  color: #c8cef0;
  display: grid;
  place-items: center;
  font-size: 11px;
}
.meta-divider {
  color: #47556f;
}
.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  margin-top: 18px;
  font-size: 10px;
  color: #708eaa;
}
.article-heading {
  padding-bottom: 28px;
  border-bottom: 1px solid #95a6cc21;
  margin-bottom: 28px;
}
.preview-body {
  font-size: 15px;
  line-height: 1.95;
  color: #bdcbe0;
}
.preview-body :deep(h2) {
  font-size: 22px;
  color: #e1e8f6;
}
.preview-body :deep(h3) {
  font-size: 18px;
  color: #d9e2f4;
}
.preview-body :deep(pre) {
  font-size: 12px;
  border: 1px solid #a0b1ce18;
}
.empty-content {
  min-height: 320px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #778ba8;
}
.empty-content > span {
  color: #687697;
  font-size: 38px;
}
.empty-content p {
  font-size: 14px;
  margin: 20px 0 10px;
}
.empty-content small {
  font-size: 11px;
  color: #526580;
}
.preview-footer {
  margin-top: 45px;
  padding-top: 22px;
  border-top: 1px solid #95a6cc18;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #5b6c88;
  font-size: 10px;
}
@container (max-width: 480px) {
  .preview-inner {
    padding: 25px 20px;
  }
  h1 {
    font-size: 26px;
  }
  .preview-body {
    font-size: 14px;
  }
  .preview-body :deep(h2) {
    font-size: 20px;
  }
  .preview-label {
    margin-bottom: 25px;
  }
}
</style>
