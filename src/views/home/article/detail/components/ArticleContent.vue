<template>
  <div class="content-card" id="article-content">
    <div class="body-caption"><span>正文</span><span>THE STORY</span></div>
    <div ref="body" class="markdown-body" v-html="htmlContent" />
    <div class="body-end" aria-hidden="true"><span />✦<span /></div>
  </div>
</template>

<script setup lang="ts">
import '@/styles/markdown.css'

import { useArticleContent } from '../logic/useArticleContent'

const props = defineProps<{ content: string }>()

const { htmlContent, body } = useArticleContent(props)
</script>

<style scoped>
.content-card {
  min-width: 0;
  padding: 0 0 20px;
}
.body-caption {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 30px;
  color: #898095;
  font-size: 10px;
  letter-spacing: 0.12em;
}
.body-caption > span:last-child {
  font:
    8px/1.5 Consolas,
    monospace;
  color: #655c74;
  letter-spacing: 0.18em;
}
.markdown-body {
  color: #cfcbdb;
  font-size: 16px;
  line-height: 2.05;
  letter-spacing: 0.02em;
}
.markdown-body :deep(h1),
.markdown-body :deep(h2) {
  scroll-margin-top: 112px;
  font-family: 'Noto Serif SC', 'Songti SC', SimSun, Georgia, serif;
  color: #e7dfef;
  font-weight: 500;
  letter-spacing: 0.04em;
}
.markdown-body :deep(h1) {
  font-size: 31px;
  margin: 52px 0 24px;
  line-height: 1.6;
}
.markdown-body :deep(h2) {
  font-size: 24px;
  margin: 44px 0 22px;
  line-height: 1.65;
  padding-bottom: 12px;
  border-bottom: 1px solid #b9a5cf14;
}
.markdown-body :deep(h3) {
  font-size: 18px;
  font-weight: 500;
  color: #cbbfde;
  margin-top: 32px;
}
.markdown-body :deep(> :first-child) {
  margin-top: 0;
}
.markdown-body :deep(p) {
  margin: 20px 0;
}
.markdown-body :deep(a) {
  color: #bdb6dd;
  text-decoration: underline;
  text-decoration-color: #bdb6dd44;
  text-underline-offset: 5px;
}
.markdown-body :deep(blockquote) {
  border-left: 2px solid #af94c880;
  background: linear-gradient(90deg, #85699413, #85699403);
  padding: 20px 28px;
  margin: 30px 0;
  border-radius: 0 10px 10px 0;
  color: #b8abc9;
  font-family: 'Noto Serif SC', 'Songti SC', SimSun, Georgia, serif;
  font-size: 17px;
}
.markdown-body :deep(blockquote p) {
  margin: 0;
}
.markdown-body :deep(pre) {
  padding: 25px 28px;
  margin: 28px 0;
  border: 1px solid #b6a0ce1f;
  border-radius: 12px;
  background: #070b14;
  font-size: 12px;
  line-height: 1.9;
  box-shadow: inset 0 1px #b6a0ce05;
  letter-spacing: 0;
}
.markdown-body :deep(:not(pre) > code) {
  background: #a488cd14;
  color: #d2bfe6;
  padding: 0.2em 0.45em;
  border: 1px solid #a488cd12;
  border-radius: 4px;
  font-size: 0.87em;
  letter-spacing: 0;
}
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 1.4em;
  margin: 22px 0;
}
.markdown-body :deep(li) {
  padding-left: 5px;
  margin: 8px 0;
}
.markdown-body :deep(li::marker) {
  color: #a48bbd;
  font-size: 0.85em;
}
.markdown-body :deep(strong) {
  color: #e3d9ec;
  font-weight: 600;
}
.markdown-body :deep(th),
.markdown-body :deep(td) {
  border-color: #a08db52b;
  padding: 12px 16px;
  font-size: 13px;
}
.markdown-body :deep(th) {
  color: #cdbddd;
  background: #a08db509;
  font-weight: 500;
}
.markdown-body :deep(img) {
  border: 1px solid #b9a5cf24;
  border-radius: 12px;
  margin: 34px auto;
}
.markdown-body :deep(hr) {
  border-color: #b9a5cf24;
  margin: 40px 0;
}
.body-end {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  color: #9784ad;
  margin: 50px auto 16px;
  font-size: 14px;
}
.body-end > span {
  width: 50px;
  height: 1px;
  background: linear-gradient(90deg, transparent, #a48bbd40);
}
.body-end > span:last-child {
  transform: rotate(180deg);
}
@media (max-width: 600px) {
  .markdown-body {
    font-size: 15px;
    line-height: 2;
  }
  .markdown-body :deep(h1) {
    font-size: 27px;
    margin-top: 40px;
  }
  .markdown-body :deep(h2) {
    font-size: 22px;
    margin-top: 34px;
  }
  .markdown-body :deep(blockquote) {
    padding: 18px 20px;
    font-size: 16px;
  }
  .markdown-body :deep(pre) {
    padding: 18px;
    font-size: 11px;
  }
  .body-caption {
    margin-bottom: 25px;
  }
}
</style>
