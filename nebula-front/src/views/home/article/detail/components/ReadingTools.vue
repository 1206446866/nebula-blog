<script setup lang="ts">
import { toRef } from 'vue'
import { useReadingTools } from '../logic/useReadingTools'
import { useArticleDownload } from '../logic/useArticleDownload'
const props = defineProps<{ articleId: number; enabled: boolean; title: string }>()
const { downloading, download } = useArticleDownload(
  toRef(props, 'articleId'),
  toRef(props, 'title'),
)
const { state, ready, busy, resumePending, resumePercent, error, load, toggle, resume } =
  useReadingTools(toRef(props, 'articleId'), toRef(props, 'enabled'))
</script>

<template>
  <section v-if="enabled" class="reading-tools" aria-label="私人阅读工具">
    <div class="reading-actions">
      <span class="reading-caption">留住这一束星光</span>
      <button :disabled="!ready || busy" :aria-pressed="state.favorite" @click="toggle('favorite')">
        {{ state.favorite ? '◆ 已收藏' : '◇ 收藏' }}
      </button>
      <button :disabled="!ready || busy" :aria-pressed="state.later" @click="toggle('later')">
        {{ state.later ? '◷ 已加入稍后读' : '◷ 稍后读' }}
      </button>
      <RouterLink to="/library">我的书架 ↗</RouterLink>
      <details>
        <summary>导出文章 ↓</summary>
        <div class="download-menu">
          <button :disabled="downloading" @click="download(false)">Markdown正文</button
          ><button :disabled="downloading" @click="download(true)">正文与本地图片ZIP</button>
        </div>
      </details>
    </div>
    <div v-if="resumePending" class="resume-prompt" role="status">
      <span>上次读到 {{ resumePercent }}%，接着读下去？</span>
      <button @click="resume(false)">继续阅读</button
      ><button @click="resume(true)">从头阅读</button>
    </div>
    <p v-if="error" role="status">{{ error }} <button @click="load">重试</button></p>
  </section>
</template>

<style scoped>
.reading-tools {
  margin: 0 auto 30px;
  padding: 18px 22px;
  max-width: 1320px;
  border: 1px solid rgba(139, 170, 224, 0.18);
  border-radius: 16px;
  background: rgba(16, 28, 55, 0.55);
  color: #c0cde5;
}
.reading-actions,
.resume-prompt {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.reading-caption {
  margin-right: auto;
  color: #97abc9;
  font-size: 13px;
}
button,
a {
  border: 1px solid #49628366;
  border-radius: 20px;
  background: transparent;
  color: inherit;
  padding: 8px 14px;
  font: inherit;
  font-size: 13px;
  text-decoration: none;
  cursor: pointer;
}
button[aria-pressed='true'] {
  background: #46618d55;
  color: #e5d4a8;
  border-color: #c5ae7377;
}
button:disabled {
  opacity: 0.5;
  cursor: wait;
}
button:focus-visible,
a:focus-visible {
  outline: 2px solid #c6b078;
  outline-offset: 3px;
}
.resume-prompt {
  border-top: 1px solid #49628344;
  margin-top: 14px;
  padding-top: 14px;
  font-size: 14px;
}
.reading-tools p {
  font-size: 13px;
}
details {
  position: relative;
  font-size: 13px;
}
summary {
  cursor: pointer;
  padding: 8px 12px;
}
.download-menu {
  position: absolute;
  right: 0;
  top: 36px;
  z-index: 5;
  background: #142039;
  border: 1px solid #496283;
  border-radius: 12px;
  padding: 10px;
  min-width: 185px;
  display: grid;
  gap: 8px;
}
summary:focus-visible {
  outline: 2px solid #c6b078;
  outline-offset: 3px;
}
@media (max-width: 600px) {
  .reading-tools {
    margin-inline: 16px;
    padding: 16px;
  }
  .reading-caption {
    width: 100%;
  }
  .resume-prompt span {
    width: 100%;
  }
}
</style>
