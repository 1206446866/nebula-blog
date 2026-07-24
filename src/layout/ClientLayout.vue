<template>
  <div class="layout">
    <ClientHeader />

    <main class="main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import ClientHeader from '../components/client/home/ClientHeader.vue'
</script>
