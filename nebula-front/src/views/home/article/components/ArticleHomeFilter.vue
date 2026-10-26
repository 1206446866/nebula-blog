<template>
  <section v-if="visible" class="filter">
    <div class="container">
      <!-- 分类筛选展示 -->
      <div v-if="filterStore.categoryIds.length" class="block">
        <div class="label">分类</div>

        <div class="row">
          <div v-for="id in filterStore.categoryIds" :key="id" class="chip">
            <span>{{ categoryMap.get(id) || `分类 #${id}` }}</span>

            <!-- ❗唯一动作：删除 -->
            <button class="close" @click="removeCategory(id)">×</button>
          </div>
        </div>
      </div>
      <!-- 🔥 分割线 -->
      <div class="divider" v-if="visible"></div>

      <!-- 标签筛选展示 -->
      <div v-if="filterStore.tagIds.length" class="block">
        <div class="label">标签</div>

        <div class="row">
          <div v-for="t in filterStore.tagIds" :key="t" class="chip">
            <span>{{ tagMap.get(t) || `标签 #${t}` }}</span>

            <!-- ❗唯一动作：删除 -->
            <button class="close" @click="removeTag(t)">×</button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useArticleHomeFilter } from '../logic/useArticleHomeFilter'

const { filterStore, visible, removeCategory, removeTag, categoryMap, tagMap } =
  useArticleHomeFilter()
</script>

<style scoped>
.filter {
  margin: 28px 0 36px;
}

.container {
  width: min(980px, calc(100% - 140px));
  margin: 0 auto;
}

.block {
  margin-bottom: 14px;
}

.label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  margin-bottom: 8px;
}

.row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;

  padding: 7px 12px;
  border-radius: 999px;

  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);

  color: rgba(255, 255, 255, 0.75);
}

.close {
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
}

.close:hover {
  color: #fff;
}
.divider {
  height: 1px;
  margin: 14px 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.72), transparent);
}
</style>
