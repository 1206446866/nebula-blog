<template>
  <div class="content-card" id="article-content">
    <div class="markdown-body" v-html="htmlContent" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import '@/styles/markdown.css'

const props = defineProps<{ content: string }>()
const escapeHtml = (s: string) =>
  s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

const md = new MarkdownIt({
  html: true,
  highlight(str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      return `<pre><code class="hljs language-${lang}">${hljs.highlight(str, { language: lang }).value}</code></pre>`
    }
    return `<pre><code class="hljs">${escapeHtml(str)}</code></pre>`
  },
})

const htmlContent = computed(() => md.render(props.content || ''))
</script>

<style scoped>
.content-card {
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  padding: 40px;
}
@media (max-width: 768px) {
  .content-card {
    padding: 24px 18px;
  }
}
</style>
