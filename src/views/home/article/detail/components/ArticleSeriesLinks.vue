<script setup lang="ts">
import { toRef } from 'vue'
import { useArticleSeriesLinks } from '../logic/useArticleSeriesLinks'
const props = defineProps<{ articleId: number }>()
const { series, detail, selected, previous, next, index, choose } = useArticleSeriesLinks(
  toRef(props, 'articleId'),
)
</script>
<template>
  <section v-if="detail && index >= 0" class="series-reading" aria-label="系列阅读导航">
    <div class="series-reading-heading">
      <div>
        <small>系列篇章 · {{ index + 1 }} / {{ detail.articles.length }}</small
        ><RouterLink :to="`/series/${detail.series.id}`"
          ><h3>{{ detail.series.title }}</h3></RouterLink
        >
      </div>
      <select
        v-if="series.length > 1"
        :value="selected"
        aria-label="切换阅读系列"
        @change="choose(($event.target as HTMLSelectElement).value)"
      >
        <option v-for="item in series" :key="item.id" :value="item.id">{{ item.title }}</option>
      </select>
    </div>
    <div class="chapter-links">
      <RouterLink
        v-if="previous"
        :to="{ path: `/article/${previous.id}`, query: { series: String(selected) } }"
        >← 上一篇<span>{{ previous.title }}</span></RouterLink
      ><span v-else>你已来到这一段旅程的起点</span
      ><RouterLink
        v-if="next"
        :to="{ path: `/article/${next.id}`, query: { series: String(selected) } }"
        >下一篇 →<span>{{ next.title }}</span></RouterLink
      ><RouterLink v-else :to="`/series/${selected}`"
        >已读到系列末尾<span>查看完整篇章 ↗</span></RouterLink
      >
    </div>
  </section>
</template>
<style scoped>
.series-reading {
  border: 1px solid #b8a77633;
  border-radius: 14px;
  padding: 24px;
  margin: 32px 0;
  background: #9a7c4020;
  color: #c6c1d1;
}
.series-reading-heading {
  display: flex;
  align-items: center;
  gap: 20px;
  justify-content: space-between;
}
.series-reading-heading small {
  color: #c1aa77;
  font-size: 11px;
  letter-spacing: 0.06em;
}
h3 {
  font-size: 20px;
  font-weight: 400;
  margin: 10px 0 0;
  overflow-wrap: anywhere;
}
a {
  color: inherit;
  text-decoration: none;
}
a:hover {
  color: #e0c898;
}
.chapter-links {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  border-top: 1px solid #b8a77622;
  padding-top: 20px;
  margin-top: 20px;
  font-size: 12px;
  color: #9594ac;
}
.chapter-links a:last-child {
  text-align: right;
}
.chapter-links span {
  display: block;
  font-size: 14px;
  color: #c6c1d1;
  margin-top: 8px;
  overflow-wrap: anywhere;
}
select {
  max-width: 180px;
  background: #171727;
  color: #d3c9b0;
  border: 1px solid #b8a77655;
  padding: 7px;
  border-radius: 6px;
}
a:focus-visible,
select:focus-visible {
  outline: 2px solid #d1bb88;
  outline-offset: 4px;
}
@media (max-width: 600px) {
  .series-reading {
    padding: 18px;
  }
  .series-reading-heading {
    flex-wrap: wrap;
  }
  .chapter-links {
    gap: 12px;
  }
}
</style>
