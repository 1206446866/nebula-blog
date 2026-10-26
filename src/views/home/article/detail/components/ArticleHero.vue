<template>
  <section class="hero" aria-label="文章信息">
    <div class="hero-topline">
      <RouterLink to="/article" class="breadcrumb"
        ><span aria-hidden="true">↖</span> 文章<span class="breadcrumb-slash">/</span
        ><span>阅读空间</span></RouterLink
      ><span class="journal-name">NEBULA JOURNAL</span>
    </div>
    <div class="hero-grid">
      <div class="hero-content">
        <div class="article-label">
          <button v-if="category" type="button" @click="goCategory">{{ category.name }}</button
          ><span v-else>星云手记</span><i aria-hidden="true" /><span class="article-number"
            >NO. {{ String(article.id).padStart(3, '0') }}</span
          ><span v-if="article.status !== 1" class="draft-label">未发布</span>
        </div>
        <h1 class="title">{{ article.title }}</h1>
        <div v-if="tags.length" class="tag-list" aria-label="文章标签">
          <button v-for="tag in tags" :key="tag.id" type="button" @click="goTag(tag.id)">
            <span>#</span> {{ tag.name }}
          </button>
        </div>
        <div class="article-byline">
          <button class="author" type="button" :disabled="!author" @click="goAuthor">
            <el-avatar :size="40" :src="avatarUrl">{{
              author?.username?.charAt(0) || '注'
            }}</el-avatar
            ><span
              ><small>写于星海</small><strong>{{ author?.username || '已注销用户' }}</strong></span
            >
          </button>
          <span class="byline-divider" aria-hidden="true" />
          <div class="publication">
            <span>记录于</span
            ><time :datetime="article.createTime">{{
              formatDateTime(article.createTime) || '—'
            }}</time>
          </div>
        </div>
      </div>
      <aside class="hero-margin" aria-label="阅读概况">
        <div class="light-study" aria-hidden="true">
          <span class="light-core" /><span class="light-orbit orbit-one" /><span
            class="light-orbit orbit-two"
          /><span class="light-pin" />
        </div>
        <div class="margin-caption">
          <span>IN THE QUIET BETWEEN STARS</span>
          <p>在文字之间，<br />留一片星空。</p>
        </div>
        <div class="reading-facts">
          <span
            ><strong>{{ readingMinutes }}</strong
            ><small>分钟 · 预计阅读</small></span
          ><span
            ><strong>{{ compactCount(article.viewCount) }}</strong
            ><small>次 · 抵达此处</small></span
          >
        </div>
      </aside>
    </div>
    <div class="hero-bottomline">
      <span>一段文字，一次远行。</span
      ><span class="scroll-cue">向下阅读 <span aria-hidden="true">↓</span></span>
    </div>
  </section>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/date'
import type { ArticleVO } from '@/api/article'
import type { CategoryVO } from '@/api/category'
import type { TagVO } from '@/api/tag'
import type { UserVO } from '@/api/user'

import { useArticleHero } from '../logic/useArticleHero'

const props = defineProps<{
  article: ArticleVO
  category?: CategoryVO
  tags: TagVO[]
  author?: UserVO
}>()

const { avatarUrl, readingMinutes, compactCount, goAuthor, goCategory, goTag } =
  useArticleHero(props)
</script>

<style scoped>
.hero {
  padding: 36px 0 0;
}
.hero-topline {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}
.breadcrumb {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: #9c94ad;
  font-size: 11px;
}
.breadcrumb > span:first-child {
  color: #c3b5d8;
  font-size: 16px;
  margin-right: 3px;
}
.breadcrumb > span:last-child {
  color: #6c6680;
}
.breadcrumb-slash {
  color: #514960;
}
.journal-name {
  font:
    9px/1.5 Consolas,
    monospace;
  letter-spacing: 0.24em;
  color: #736885;
}
.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: clamp(32px, 5vw, 80px);
  padding: 76px 0 54px;
  align-items: center;
}
.hero-content {
  min-width: 0;
}
.article-label {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  color: #b0a7bd;
  font-size: 11px;
  letter-spacing: 0.08em;
}
.article-label button {
  font: inherit;
  letter-spacing: inherit;
  color: #c6b8de;
  border: 0;
  padding: 0;
  background: transparent;
  cursor: pointer;
}
.article-label i {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #665b77;
}
.article-number {
  font:
    10px/1.5 Consolas,
    monospace;
  letter-spacing: 0.12em;
  color: #7f728f;
}
.draft-label {
  color: #d0b9a4;
  font-size: 10px;
  border: 1px solid #d0b9a433;
  padding: 3px 7px;
  border-radius: 3px;
}
.title {
  font-family: 'Noto Serif SC', 'Songti SC', SimSun, Georgia, serif;
  font-size: clamp(36px, 4vw, 58px);
  font-weight: 500;
  line-height: 1.5;
  letter-spacing: 0.035em;
  margin: 22px 0 0;
  color: #f0eaf6;
  text-wrap: pretty;
  overflow-wrap: anywhere;
}
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 9px 20px;
  margin-top: 22px;
}
.tag-list button {
  border: 0;
  padding: 0;
  background: transparent;
  color: #9c91ae;
  font: inherit;
  font-size: 12px;
  cursor: pointer;
}
.tag-list button span {
  color: #61546f;
  margin-right: 2px;
}
.tag-list button:hover,
.article-label button:hover {
  color: #d2c4eb;
}
.article-byline {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-top: 42px;
}
.author {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  background: transparent;
  border: 0;
  padding: 0;
  color: #d5cadf;
  text-align: left;
  cursor: pointer;
  min-width: 0;
}
.author:disabled {
  cursor: default;
}
.author :deep(.el-avatar) {
  flex-shrink: 0;
  background: linear-gradient(140deg, #66506c, #304851);
  color: #e6dced;
  border: 1px solid #bcaccc26;
  font:
    16px Georgia,
    serif;
}
.author > span {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.author small {
  color: #7c708b;
  font-size: 9px;
  letter-spacing: 0.1em;
}
.author strong {
  font-weight: 500;
  font-size: 13px;
  overflow-wrap: anywhere;
}
.byline-divider {
  width: 1px;
  height: 28px;
  flex-shrink: 0;
  background: #bba5d124;
}
.publication {
  display: flex;
  flex-direction: column;
  gap: 5px;
  font-size: 10px;
  color: #aaa0b8;
}
.publication > span {
  font-size: 9px;
  color: #7c708b;
}
.publication time {
  font-family: Consolas, monospace;
  font-size: 11px;
}
.hero-margin {
  position: relative;
  align-self: stretch;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  min-height: 270px;
}
.light-study {
  position: absolute;
  width: 270px;
  height: 200px;
  right: -12px;
  top: -36px;
  pointer-events: none;
}
.light-core {
  position: absolute;
  inset: 30px 40px;
  border-radius: 50%;
  background: radial-gradient(
    ellipse at 55% 50%,
    #d8c2df40,
    #84639130 25%,
    #3e576d26 48%,
    transparent 72%
  );
  filter: blur(12px);
  transform: rotate(-32deg);
}
.light-orbit {
  position: absolute;
  inset: 35px 5px;
  border-radius: 50%;
  border: 1px solid #c6b3d91a;
  transform: rotate(-32deg);
}
.orbit-two {
  inset: 21px 30px;
  transform: rotate(30deg);
  border-color: #90b2c01a;
}
.light-pin {
  position: absolute;
  width: 4px;
  height: 4px;
  top: 57px;
  left: 198px;
  border-radius: 50%;
  background: #dfd7e6;
  box-shadow: 0 0 14px 3px #b6a3d255;
}
.margin-caption {
  position: relative;
  margin-top: 140px;
}
.margin-caption > span {
  color: #7c708a;
  font:
    7px/1.5 Consolas,
    monospace;
  letter-spacing: 0.15em;
}
.margin-caption p {
  font:
    19px/1.9 'Noto Serif SC',
    'Songti SC',
    SimSun,
    serif;
  color: #c2b2cf;
  letter-spacing: 0.14em;
  margin: 10px 0 0;
}
.reading-facts {
  display: flex;
  gap: 32px;
  padding-top: 25px;
  margin-top: 24px;
  border-top: 1px solid #bba5d11c;
}
.reading-facts > span {
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.reading-facts strong {
  font:
    25px/1 Georgia,
    serif;
  color: #cfc3df;
  font-weight: 400;
}
.reading-facts small {
  color: #7d738c;
  font-size: 9px;
}
.hero-bottomline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 21px;
  border-bottom: 1px solid #bfa6d92b;
  font-size: 10px;
  color: #756a84;
  letter-spacing: 0.1em;
}
.scroll-cue {
  display: flex;
  align-items: center;
  gap: 16px;
}
.scroll-cue > span {
  color: #b5a4c9;
  font-size: 17px;
}
button:focus-visible,
a:focus-visible {
  outline: 2px solid #c4b4ed;
  outline-offset: 5px;
  border-radius: 3px;
}
@media (max-width: 1100px) {
  .hero-grid {
    grid-template-columns: minmax(0, 1fr) 230px;
    gap: 32px;
    padding-top: 60px;
  }
  .light-study {
    width: 235px;
  }
}
@media (max-width: 900px) {
  .hero-grid {
    grid-template-columns: minmax(0, 1fr);
    padding: 56px 0 38px;
  }
  .hero-margin {
    display: none;
  }
  .title {
    font-size: clamp(32px, 5vw, 48px);
  }
}
@media (max-width: 600px) {
  .byline-divider {
    display: none;
  }
  .publication {
    flex-direction: row;
    align-items: center;
    gap: 12px;
    margin-left: 52px;
  }
  .hero {
    padding-top: 25px;
  }
  .journal-name {
    font-size: 7px;
    letter-spacing: 0.14em;
  }
  .breadcrumb {
    gap: 8px;
    font-size: 10px;
  }
  .hero-grid {
    padding-top: 46px;
  }
  .title {
    font-size: 34px;
    line-height: 1.55;
    margin-top: 20px;
  }
  .article-byline {
    display: grid;
    gap: 16px;
    margin-top: 34px;
  }
  .publication time {
    font-size: 10px;
  }
  .tag-list {
    gap: 10px 16px;
  }
  .hero-bottomline {
    font-size: 9px;
  }
}
</style>
