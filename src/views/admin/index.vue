<script setup lang="ts">
defineOptions({
  name: 'AdminHome',
})
</script>

<template>
  <section class="hero">
    <div class="hero-background"></div>

    <NebulaCard class="hero-card">
      <div class="hero-tag">NEXT GENERATION PLATFORM</div>

      <h1>Nebula</h1>

      <p>Build modern technology experiences with Vue 3 + Spring Boot + AI.</p>

      <div class="hero-actions">
        <el-button type="primary" size="large" round> Explore </el-button>

        <el-button size="large" round plain> Github </el-button>
      </div>
    </NebulaCard>
  </section>
</template>

<style scoped>
.hero {
  position: relative;

  min-height: calc(100vh - 72px);

  display: flex;
  justify-content: center;
  align-items: center;

  overflow: hidden;

  padding: 40px;
}

.hero-background {
  position: absolute;
  inset: 0;

  background: radial-gradient(circle at center, rgba(56, 189, 248, 0.15), transparent 60%);
}

.hero-card {
  width: 100%;
  max-width: 960px;

  padding: 96px 80px;

  text-align: center;

  position: relative;
  z-index: 2;
}

.hero-tag {
  display: inline-block;

  padding: 8px 16px;

  border-radius: 999px;

  margin-bottom: 24px;

  font-size: 14px;
  letter-spacing: 2px;

  background: rgba(255, 255, 255, 0.08);

  border: 1px solid rgba(255, 255, 255, 0.12);
}

.hero h1 {
  font-size: 96px;
  font-weight: 900;

  margin-bottom: 24px;

  line-height: 1;

  background: var(--nebula-gradient);

  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero p {
  font-size: 24px;
  line-height: 1.8;

  color: var(--nebula-text-secondary);

  margin-bottom: 48px;
}

.hero-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
}
</style>
