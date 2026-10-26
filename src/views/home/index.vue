<template>
  <NebulaHero id="home-start" tabindex="-1" @github="goGithub" />
  <ArticleSection />
  <TagSection />
  <CategorySection />
  <HomeClosing />
</template>

<script setup lang="ts">
import CategorySection from '@/components/client/home/CategorySection.vue'
import NebulaHero from '@/components/client/home/NebulaHero.vue'
import ArticleSection from '@/components/client/home/ArticleSection.vue'
import TagSection from '@/components/client/home/TagSection.vue'
import HomeClosing from '@/components/client/home/HomeClosing.vue'

import { useHomePage } from './logic/useHomePage'

defineOptions({
  name: 'ClintHome',
})

const { goGithub } = useHomePage()
</script>
