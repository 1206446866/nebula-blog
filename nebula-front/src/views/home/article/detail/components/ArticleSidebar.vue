<template>
  <aside class="article-sidebar">
    <div class="card glass-card progress-card">
      <span class="rail-kicker">READING POSITION</span>
      <div class="card-title">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 12h4l3-7 4 14 3-7h4" /></svg>
        <span>阅读进度</span>
      </div>
      <div
        class="progress-bar"
        role="progressbar"
        aria-label="文章阅读进度"
        :aria-valuenow="progress"
        :aria-valuemin="0"
        :aria-valuemax="100"
      >
        <div class="progress-fill" :style="{ width: progress + '%' }" />
      </div>
      <div class="progress-text">
        <strong>{{ Math.round(progress) }}</strong
        ><span>%</span><small>每一行，都是新的抵达</small>
      </div>
    </div>

    <nav class="card glass-card toc-card" aria-label="文章目录">
      <span class="rail-kicker">CONTENTS</span>
      <div class="card-title">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M9 6h12M9 12h12M9 18h12M3 6h1M3 12h1M3 18h1" />
        </svg>
        <span>文章目录</span>
        <span v-if="headings.length" class="heading-count">{{ headings.length }} 节</span>
      </div>
      <ol v-if="sections.length" class="toc-list">
        <li v-for="section in sections" :key="section.heading.id" class="toc-section">
          <a
            :href="`#${section.heading.id}`"
            class="toc-link"
            :class="{ active: activeId === section.heading.id }"
            :aria-current="activeId === section.heading.id ? 'location' : undefined"
            @click.prevent="goToHeading(section.heading)"
          >
            <span class="section-number">{{ section.heading.number }}.</span
            ><span class="heading-text">{{ section.heading.title }}</span>
          </a>
          <ol v-if="section.children.length" class="toc-children">
            <li v-for="heading in section.children" :key="heading.id">
              <a
                :href="`#${heading.id}`"
                class="toc-link child-link"
                :class="{ active: activeId === heading.id }"
                :aria-current="activeId === heading.id ? 'location' : undefined"
                @click.prevent="goToHeading(heading)"
              >
                <span class="section-number">{{ heading.number }}</span
                ><span class="heading-text">{{ heading.title }}</span>
              </a>
            </li>
          </ol>
        </li>
      </ol>
      <p v-else class="toc-empty">正文暂无一级或二级标题</p>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { useArticleSidebar } from '../logic/useArticleSidebar'

const props = defineProps<{ articleId: number; content: string }>()

const { progress, headings, activeId, sections, goToHeading } = useArticleSidebar(props)
</script>

<style scoped>
.article-sidebar {
  display: flex;
  flex-direction: column;
  gap: 32px;
  position: sticky;
  top: 36px;
  min-width: 0;
}
.card {
  padding: 23px 21px;
  border-radius: 12px;
}
.glass-card {
  background: linear-gradient(145deg, #c7addd06, #848cc403);
  border: 1px solid #c4b2d91c;
}
.rail-kicker {
  display: block;
  color: #756781;
  font:
    8px/1.5 Consolas,
    monospace;
  letter-spacing: 0.2em;
  margin-bottom: 17px;
}
.card-title {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 12px;
  font-weight: 500;
  color: #c8bdd9;
  margin-bottom: 19px;
}
.card-title > svg {
  width: 15px;
  height: 15px;
  fill: none;
  stroke: #9f8eb5;
  stroke-width: 1.4;
  stroke-linecap: round;
  stroke-linejoin: round;
  flex-shrink: 0;
}
.progress-card {
  display: flex;
  flex-direction: column;
}
.progress-card .card-title {
  order: 1;
  margin-bottom: 12px;
}
.progress-card .rail-kicker {
  order: 0;
}
.progress-text {
  display: flex;
  align-items: baseline;
  gap: 5px;
  order: 2;
  color: #c9b6dd;
}
.progress-text strong {
  font:
    48px/1.2 Georgia,
    serif;
  letter-spacing: -0.05em;
  font-weight: 400;
}
.progress-text > span {
  font:
    12px Georgia,
    serif;
  color: #867694;
}
.progress-text small {
  font-size: 8px;
  color: #81718c;
  margin-left: auto;
  line-height: 1.8;
  max-width: 55px;
}
.progress-bar {
  width: 100%;
  height: 2px;
  order: 3;
  margin-top: 19px;
  background: #b6a2cb17;
  border-radius: 999px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #799da8, #b8a0d0, #d2b7cb);
  border-radius: 999px;
  transition: width 0.25s ease;
}
.toc-card {
  background: transparent;
  border: 0;
  padding: 0 0 0 2px;
}
.toc-card .card-title {
  padding-bottom: 18px;
  margin-bottom: 18px;
  border-bottom: 1px solid #baa2d022;
}
.heading-count {
  color: #81718d;
  font-size: 9px;
  font-weight: 400;
  margin-left: auto;
}
.toc-list,
.toc-children {
  list-style: none;
  padding: 0;
  margin: 0;
}
.toc-list {
  max-height: clamp(160px, calc(100dvh - 390px), 460px);
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #544261 transparent;
  padding: 2px 3px 2px 0;
}
.toc-section + .toc-section {
  margin-top: 12px;
}
.toc-link {
  display: flex;
  align-items: baseline;
  gap: 10px;
  color: #aba0ba;
  font-size: 12px;
  line-height: 1.8;
  text-decoration: none;
  border: 1px solid transparent;
  border-radius: 4px;
  padding: 7px 8px;
  transition:
    color 0.18s,
    background 0.18s;
}
.toc-link:hover {
  color: #e4d7f0;
  background: #b39aca08;
}
.toc-link.active {
  color: #ddc9ee;
  border-color: #b39aca12;
  background: linear-gradient(90deg, #ae88ce13, transparent);
}
.toc-link:focus-visible {
  outline: 2px solid #c4b4ed;
  outline-offset: 1px;
}
.section-number {
  flex-shrink: 0;
  color: #766582;
  font:
    10px/1.8 Consolas,
    monospace;
}
.active .section-number {
  color: #bca3d1;
}
.heading-text {
  min-width: 0;
  overflow-wrap: anywhere;
}
.toc-children {
  margin: 4px 0 8px 17px;
  border-left: 1px solid #a98abc24;
  padding-left: 10px;
}
.toc-children > li {
  position: relative;
}
.toc-children > li::before {
  content: '';
  position: absolute;
  left: -11px;
  top: 17px;
  width: 8px;
  border-top: 1px solid #a98abc24;
}
.child-link {
  color: #8f819f;
  font-size: 11px;
}
.toc-empty {
  margin: 0;
  color: #81718d;
  font-size: 11px;
  line-height: 1.9;
}
@media (max-width: 900px) {
  .article-sidebar {
    position: static;
    order: -1;
    display: grid;
    grid-template-columns: 200px minmax(0, 1fr);
    gap: 28px;
    align-items: start;
    padding-bottom: 26px;
    border-bottom: 1px solid #baa2d01c;
  }
  .toc-list {
    max-height: 200px;
  }
  .toc-card .rail-kicker {
    margin-bottom: 14px;
  }
  .toc-card .card-title {
    margin-bottom: 10px;
    padding-bottom: 12px;
  }
}
@media (max-width: 600px) {
  .article-sidebar {
    grid-template-columns: minmax(0, 1fr);
    gap: 26px;
  }
  .progress-card {
    display: grid;
    grid-template-columns: 1fr auto;
    padding: 17px 19px;
    column-gap: 18px;
  }
  .progress-card .rail-kicker {
    grid-column: 1;
    grid-row: 1;
    margin: 0 0 8px;
    font-size: 7px;
  }
  .progress-card .card-title {
    grid-column: 1;
    grid-row: 2;
    margin: 0;
    font-size: 11px;
  }
  .progress-text {
    grid-column: 2;
    grid-row: 1 / 3;
  }
  .progress-text strong {
    font-size: 36px;
  }
  .progress-text small {
    display: none;
  }
  .progress-bar {
    grid-column: 1 / -1;
    margin-top: 16px;
  }
  .toc-card .rail-kicker {
    display: none;
  }
  .toc-list {
    max-height: 180px;
  }
  .toc-link {
    font-size: 12px;
  }
  .child-link {
    font-size: 11px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .progress-fill,
  .toc-link {
    transition: none;
  }
}
</style>
