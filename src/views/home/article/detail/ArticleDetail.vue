<template>
  <div class="page nebula-reading-room">
    <div class="nebula-atmosphere" aria-hidden="true">
      <span class="haze haze-violet" /><span class="haze haze-blue" /><span
        class="haze haze-rose"
      /><span class="distant-light" />
    </div>
    <div class="reading-frame">
      <div v-if="articleLoading" class="loading-state">
        <span class="state-kicker">NEBULA JOURNAL</span><el-skeleton :rows="10" animated />
      </div>
      <div v-else-if="!article" class="empty-state">
        <span class="state-kicker">NEBULA JOURNAL</span
        ><el-empty :description="loadError ? '这束星光暂时没有抵达' : '这篇文章已不在此处'"
          ><el-button v-if="loadError" @click="fetchArticle">重新加载</el-button
          ><RouterLink v-else to="/article">返回文章列表 ↗</RouterLink></el-empty
        >
      </div>
      <template v-else>
        <ArticleHero
          :author="currentAuthor"
          :article="article"
          :category="currentCategory"
          :tags="currentTags"
        />
        <div class="article-layout">
          <div class="reading-column">
            <ArticleContent :content="article.content" /><ArticleNavigation :article="article" />
          </div>
          <ArticleSidebar :article-id="article.id" :content="article.content" />
        </div>
        <ArticleComment :key="article.id" :article-id="article.id" :author-id="article.userId" />
        <footer class="journal-footer">
          <RouterLink to="/article">NEBULA<span>回到星海</span></RouterLink
          ><span>每一次阅读，都是一次微小的远行。</span
          ><span class="footer-star" aria-hidden="true">✦</span>
        </footer>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import ArticleHero from './components/ArticleHero.vue'
import ArticleContent from './components/ArticleContent.vue'
import ArticleSidebar from './components/ArticleSidebar.vue'
import ArticleNavigation from './components/ArticleNavigation.vue'
import ArticleComment from './components/ArticleComment.vue'

import { useArticleDetail } from './logic/useArticleDetail'

const {
  loadError,
  articleLoading,
  article,
  currentAuthor,
  currentCategory,
  currentTags,
  fetchArticle,
} = useArticleDetail()
</script>

<style scoped>
.page {
  --reading-ink: #e7e2f1;
  --reading-muted: #9a96b0;
  --reading-faint: #66657e;
  --reading-line: #c3b7e51a;
  --reading-accent: #c4b4ed;
  --reading-teal: #9bc9c9;
  --nebula-primary: #bca9e8;
  --nebula-text: #d2cedf;
  --nebula-text-secondary: #a29bb4;
  --nebula-border: #b2a0cd24;
  --el-color-primary: #bca9e8;
  --el-text-color-primary: #e7e2f1;
  --el-text-color-regular: #b8b0cc;
  --el-mask-color: #0b0b17cc;
  position: relative;
  isolation: isolate;
  min-height: 100vh;
  background: #090b16;
  color: var(--reading-ink);
  padding: 0 40px;
}
.nebula-atmosphere {
  position: absolute;
  inset: 0;
  z-index: -1;
  overflow: hidden;
  pointer-events: none;
  background:
    radial-gradient(ellipse at 8% 0%, #18343d45, transparent 32%),
    radial-gradient(ellipse at 94% 8%, #48336345, transparent 32%),
    radial-gradient(ellipse at 100% 70%, #3228422b, transparent 25%);
}
.haze {
  position: absolute;
  display: block;
  filter: blur(65px);
  border-radius: 50%;
  opacity: 0.45;
}
.haze-violet {
  width: 820px;
  height: 300px;
  right: -210px;
  top: 100px;
  background: radial-gradient(ellipse, #7e619c88, #3c2d7230 45%, transparent 70%);
  transform: rotate(-24deg);
}
.haze-blue {
  width: 640px;
  height: 250px;
  right: 260px;
  top: 40px;
  background: radial-gradient(ellipse, #46777b55, transparent 67%);
  transform: rotate(-30deg);
}
.haze-rose {
  width: 510px;
  height: 230px;
  right: -100px;
  top: 310px;
  background: radial-gradient(ellipse, #936c8340, transparent 68%);
  transform: rotate(-32deg);
}
.distant-light {
  position: absolute;
  inset: 0 0 auto;
  height: 820px;
  opacity: 0.7;
  background:
    radial-gradient(circle at 7% 12%, #c7bdd8aa 0 0.7px, transparent 1.5px),
    radial-gradient(circle at 67% 13%, #d8dae98c 0 1px, transparent 1.8px),
    radial-gradient(circle at 91% 29%, #b8b8df77 0 0.7px, transparent 1.5px),
    radial-gradient(circle at 78% 49%, #e3d1dc66 0 0.7px, transparent 1.8px),
    radial-gradient(circle at 38% 34%, #c6cddd4d 0 0.7px, transparent 1.3px);
}
.reading-frame {
  width: min(1280px, 100%);
  margin: 0 auto;
}
.article-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: clamp(32px, 5vw, 80px);
  align-items: start;
  padding-top: 48px;
}
.reading-column {
  min-width: 0;
}
.loading-state,
.empty-state {
  padding: 110px 0;
  max-width: 900px;
  margin: auto;
  min-height: 80vh;
  --el-skeleton-color: #b6a4dc0f;
  --el-skeleton-to-color: #b6a4dc1a;
}
.state-kicker {
  display: block;
  margin-bottom: 40px;
  font:
    10px/1.5 Consolas,
    monospace;
  letter-spacing: 0.24em;
  color: #9c8ab7;
}
.empty-state a {
  font-size: 13px;
  color: #c3b2e2;
}
.journal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 34px 0 40px;
  margin-top: 80px;
  border-top: 1px solid var(--reading-line);
  color: #777087;
  font-size: 11px;
}
.journal-footer > a {
  display: inline-flex;
  align-items: center;
  gap: 18px;
  color: #b1a1c9;
  font:
    12px/1.5 Georgia,
    serif;
  letter-spacing: 0.16em;
}
.journal-footer > a span {
  font-family: inherit;
  font-size: 11px;
  line-height: 1.5;
  letter-spacing: 0.06em;
  color: #847c96;
}
.footer-star {
  color: #b2a3cc;
  font-size: 17px;
}
@media (max-width: 1100px) {
  .article-layout {
    grid-template-columns: minmax(0, 1fr) 230px;
    gap: 32px;
  }
}
@media (max-width: 900px) {
  .page {
    padding: 0 26px;
  }
  .article-layout {
    grid-template-columns: minmax(0, 1fr);
    padding-top: 26px;
    gap: 28px;
  }
}
@media (max-width: 600px) {
  .page {
    padding: 0 20px;
  }
  .journal-footer {
    flex-wrap: wrap;
    margin-top: 52px;
    padding: 28px 0;
    gap: 18px;
  }
  .journal-footer > span:not(.footer-star) {
    order: 2;
    flex-basis: 100%;
    font-size: 10px;
  }
  .haze {
    opacity: 0.3;
  }
}
</style>
