<script setup lang="ts">
defineOptions({ name: 'ArticleSeriesPage' })
import { useSeriesManager } from './logic/useSeriesManager'
const {
  auth,
  mine,
  page,
  total,
  items,
  loading,
  failed,
  visible,
  saving,
  editing,
  form,
  searching,
  query,
  choices,
  searchTotal,
  ordered,
  load,
  loadMore,
  open,
  add,
  move,
  save,
  remove,
} = useSeriesManager()
</script>
<template>
  <main class="series-page">
    <header>
      <p class="eyebrow">NEBULA / CONNECTED STORIES</p>
      <h1>让星光，连成篇章<span>。</span></h1>
      <p>把一段探索串成有序的旅程，让故事有始有终。</p>
    </header>
    <div class="series-toolbar">
      <div>
        <button :aria-pressed="!mine" @click="mine = false">发现系列</button
        ><button :aria-pressed="mine" @click="mine = true">我的系列</button>
      </div>
      <button v-if="auth.hasPermission('article:update')" @click="open()">＋ 创建系列</button>
    </div>
    <p v-if="loading" class="series-state" role="status">正在连接星光…</p>
    <p v-else-if="failed" class="series-state" role="alert">
      暂时无法读取系列 <button @click="load">重试</button>
    </p>
    <div v-else-if="!items.length" class="series-state">
      <h2>{{ mine ? '你的第一个系列，从这里开始' : '篇章正在酝酿' }}</h2>
      <p>系列可以先保持私有，公开后才会进入发现列表。未发布文章始终保密。</p>
    </div>
    <div v-else class="series-grid">
      <article v-for="item in items" :key="item.id">
        <span class="series-orbit" aria-hidden="true">◎</span
        ><small>{{ item.publicVisible ? `${item.articleCount} 篇已发布` : '私人编排' }}</small
        ><RouterLink :to="`/series/${item.id}`"
          ><h2>{{ item.title }}</h2></RouterLink
        >
        <p>{{ item.description || '一段等待展开的探索。' }}</p>
        <div class="series-card-footer">
          <RouterLink :to="`/series/${item.id}`">打开篇章 ↗</RouterLink
          ><template v-if="item.userId === auth.user?.id && auth.hasPermission('article:update')"
            ><button :disabled="saving" @click="open(item)">编排</button
            ><button :disabled="saving" @click="remove(item)">删除</button></template
          >
        </div>
      </article>
    </div>
    <el-pagination
      v-if="total > 20"
      v-model:current-page="page"
      :page-size="20"
      :total="total"
      layout="prev,pager,next"
      :disabled="loading"
    />
    <el-dialog
      v-model="visible"
      :title="editing ? '编排系列' : '创建系列'"
      width="min(850px,94vw)"
      :close-on-click-modal="!saving"
      :close-on-press-escape="!saving"
      :show-close="!saving"
      append-to-body
    >
      <el-form label-position="top" :disabled="saving"
        ><el-form-item label="系列名称"
          ><el-input v-model="form.title" maxlength="120" show-word-limit /></el-form-item
        ><el-form-item label="系列介绍"
          ><el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="2000"
            show-word-limit /></el-form-item
        ><el-form-item
          ><el-checkbox v-model="form.publicVisible"
            >公开这个系列（读者只能看到其中已发布的文章）</el-checkbox
          ></el-form-item
        ></el-form
      >
      <div class="series-editor">
        <section>
          <h3>阅读顺序 · {{ ordered.length }}/100</h3>
          <p v-if="!ordered.length">从文章列表选择自己的文章，也可以先保存空系列。</p>
          <div v-for="(item, index) in ordered" :key="item.id" class="ordered-row">
            <span
              >{{ index + 1 }}. {{ item.title
              }}<small v-if="item.status !== 1"> · 未发布</small></span
            ><button :disabled="saving || !index" aria-label="上移文章" @click="move(index, -1)">
              ↑</button
            ><button
              :disabled="saving || index === ordered.length - 1"
              aria-label="下移文章"
              @click="move(index, 1)"
            >
              ↓</button
            ><button
              :disabled="saving"
              aria-label="移出系列"
              @click="form.articleIds.splice(index, 1)"
            >
              ×
            </button>
          </div>
        </section>
        <section>
          <h3>添加自己的文章</h3>
          <el-input v-model="query" placeholder="搜索文章标题" :disabled="saving" maxlength="100" />
          <p v-if="searching" role="status">正在查找…</p>
          <button
            v-for="item in choices"
            :key="item.id"
            class="choice-row"
            :disabled="saving || form.articleIds.includes(item.id) || ordered.length >= 100"
            @click="add(item)"
          >
            {{ form.articleIds.includes(item.id) ? '✓' : '＋' }} {{ item.title
            }}<small>{{ item.status === 1 ? '已发布' : '未发布' }}</small></button
          ><button v-if="choices.length < searchTotal" :disabled="searching" @click="loadMore">
            加载更多文章
          </button>
        </section>
      </div>
      <template #footer
        ><el-button :disabled="saving" @click="visible = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save">保存系列</el-button></template
      >
    </el-dialog>
  </main>
</template>
<style scoped>
.series-page {
  min-height: 85vh;
  padding: 120px max(24px, calc((100vw - 1120px) / 2)) 80px;
  color: #c7d2e7;
  background: radial-gradient(ellipse at 10% 10%, #4b3c642d, transparent 55%), #0a1122;
}
.eyebrow {
  font-size: 11px;
  letter-spacing: 0.24em;
  color: #c5ad75;
}
h1 {
  font:
    clamp(28px, 4vw, 46px) Georgia,
    serif;
  color: #ece3d5;
  margin: 24px 0;
}
h1 span {
  color: #c5ad75;
}
header > p:last-child {
  font-size: 14px;
  color: #8d9eb9;
}
.series-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin: 40px 0 30px;
}
.series-toolbar > div {
  display: flex;
  gap: 10px;
}
button {
  font: inherit;
  cursor: pointer;
  border: 1px solid #8396b54a;
  border-radius: 20px;
  padding: 8px 15px;
  color: inherit;
  background: transparent;
  font-size: 13px;
}
button[aria-pressed='true'] {
  color: #e9d7a4;
  border-color: #c7ac7288;
  background: #c7ac7215;
}
button:disabled {
  opacity: 0.4;
  cursor: default;
}
a {
  color: inherit;
  text-decoration: none;
}
button:focus-visible,
a:focus-visible {
  outline: 2px solid #d1bb88;
  outline-offset: 3px;
}
.series-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px;
}
.series-grid article {
  padding: 28px;
  border: 1px solid #6c80a52a;
  border-radius: 16px;
  background: linear-gradient(135deg, #3e4e6a1f, #121b2b);
  position: relative;
  overflow: hidden;
}
.series-orbit {
  position: absolute;
  right: 20px;
  top: 12px;
  font-size: 70px;
  color: #b89e6322;
  pointer-events: none;
}
.series-grid small {
  color: #b6a47f;
  font-size: 11px;
}
h2 {
  font-size: 22px;
  line-height: 1.4;
  font-weight: 500;
  position: relative;
  overflow-wrap: anywhere;
}
.series-grid p {
  font-size: 13px;
  line-height: 1.9;
  white-space: pre-line;
  color: #8fa0bc;
  overflow-wrap: anywhere;
}
.series-card-footer {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-top: 26px;
  font-size: 13px;
}
.series-card-footer > a {
  margin-right: auto;
  color: #d4c297;
}
.series-card-footer button {
  padding: 5px 10px;
}
.series-state {
  padding: 70px 20px;
  border: 1px solid #6c80a52a;
  border-radius: 16px;
  text-align: center;
  color: #8fa0bc;
  line-height: 1.9;
}
.el-pagination {
  margin-top: 36px;
  justify-content: center;
}
.series-editor {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 22px;
  color: var(--el-text-color-primary);
}
.series-editor section {
  min-width: 0;
}
.series-editor h3 {
  font-size: 14px;
}
.series-editor p {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.ordered-row {
  display: flex;
  gap: 4px;
  padding: 9px 0;
  border-bottom: 1px solid var(--el-border-color);
  align-items: center;
  font-size: 13px;
}
.ordered-row > span {
  flex: 1;
  overflow-wrap: anywhere;
}
.ordered-row button {
  padding: 4px 8px;
  border-color: var(--el-border-color);
}
.choice-row {
  display: block;
  text-align: left;
  width: 100%;
  border-color: var(--el-border-color);
  margin-top: 8px;
  border-radius: 8px;
  overflow-wrap: anywhere;
}
.choice-row small {
  display: block;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-top: 5px;
}
@media (max-width: 650px) {
  .series-page {
    padding: 100px 20px 60px;
  }
  .series-grid,
  .series-editor {
    grid-template-columns: 1fr;
  }
  .series-grid article {
    padding: 22px;
  }
  .series-editor section {
    max-height: 300px;
    overflow: auto;
  }
}
</style>
