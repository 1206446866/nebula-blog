<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()
const keyword = ref('')
const input = ref<HTMLInputElement>()
watch(
  () => route.query.q,
  (q) => {
    if (route.name === 'SiteSearch') keyword.value = typeof q === 'string' ? q : ''
  },
  { immediate: true },
)
function submit() {
  const q = keyword.value.trim()
  if (!q) {
    input.value?.focus()
    return
  }
  void router.push({ name: 'SiteSearch', query: { q, type: 'all' } })
}
</script>

<template>
  <form class="header-search" role="search" aria-label="全站搜索" @submit.prevent="submit">
    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="10.8" cy="10.8" r="6.5" />
      <path d="m16 16 4.5 4.5" />
    </svg>
    <input
      ref="input"
      v-model="keyword"
      type="search"
      maxlength="50"
      aria-label="全站搜索关键词"
      placeholder="搜索文章、评论、用户…"
    />
    <button type="submit" aria-label="搜索全站">搜索</button>
  </form>
</template>

<style scoped>
.header-search {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
  padding: 6px 8px 6px 12px;
  border: 1px solid #a6b7df30;
  border-radius: 11px;
  background: #8d9ac90a;
  color: #9caecb;
}
.header-search:focus-within {
  border-color: #a5b4fc88;
}
svg {
  width: 16px;
  height: 16px;
  stroke: currentColor;
  stroke-width: 1.6;
  flex-shrink: 0;
}
input {
  width: 100%;
  min-width: 0;
  font: inherit;
  font-size: 12px;
  background: transparent;
  border: 0;
  outline: none;
  color: #dce5f4;
}
input::placeholder {
  color: #7f8ca6;
}
button {
  flex-shrink: 0;
  padding: 5px 9px;
  border: 0;
  border-radius: 6px;
  background: #a5b4fc16;
  color: #c0d0ec;
  font: inherit;
  font-size: 11px;
  cursor: pointer;
}
button:focus-visible {
  outline: 2px solid #a5b4fc;
  outline-offset: 2px;
}
</style>
