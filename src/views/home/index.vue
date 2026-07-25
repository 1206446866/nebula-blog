<template>
  <NebulaHero @github="goGithub" />
  <ArticleSection />
  <TagSection />
  <CategorySection />
</template>

<script setup lang="ts">
import CategorySection from '@/components/client/home/CategorySection.vue'
import NebulaHero from '@/components/client/home/NebulaHero.vue'
import ArticleSection from '@/components/client/home/ArticleSection.vue'
import TagSection from '@/components/client/home/TagSection.vue'

defineOptions({
  name: 'ClintHome',
})

const goGithub = () => {
  window.open('https://github.com/1206446866/nebula-blog')
}
</script>
