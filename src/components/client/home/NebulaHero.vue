<script setup lang="ts">
defineOptions({
  name: 'NebulaHero',
})

const emit = defineEmits<{
  (e: 'github'): void
}>()
</script>

<template>
  <section class="hero">
    <div class="hero-bg"></div>

    <div class="hero-content">
      <div class="hero-tag">NEXT GENERATION KNOWLEDGE PLATFORM</div>

      <h1>Nebula</h1>

      <p>
        技术 · 分享 · 成长
        <br />
        Build modern technology experiences with Vue3, Spring Boot and AI.
      </p>

      <div class="hero-actions">
        <el-button type="primary" size="large" round @click="$router.push('/article')">
          开始探索
        </el-button>

        <el-button size="large" @click="emit('github')" round plain> Github </el-button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero {
  position: relative;

  min-height: 90vh;

  display: flex;
  align-items: center;
  justify-content: center;

  overflow: hidden;

  padding: 80px 24px;
}

.hero-bg {
  position: absolute;
  inset: 0;

  background: radial-gradient(circle at center, rgba(56, 189, 248, 0.15), transparent 65%);

  pointer-events: none;
}

.hero-content {
  position: relative;
  z-index: 2;

  max-width: 960px;

  text-align: center;
}

.hero-tag {
  display: inline-block;

  padding: 8px 16px;

  margin-bottom: 24px;

  border-radius: 999px;

  color: #cbd5e1;

  font-size: 13px;

  letter-spacing: 2px;

  background: rgba(255, 255, 255, 0.05);

  border: 1px solid rgba(255, 255, 255, 0.08);
}

.hero h1 {
  margin: 0;

  font-size: 96px;

  font-weight: 900;

  line-height: 1;

  background: linear-gradient(135deg, #38bdf8, #818cf8);

  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero p {
  margin-top: 32px;
  margin-bottom: 48px;

  color: #94a3b8;

  font-size: 22px;

  line-height: 1.8;
}

.hero-actions {
  display: flex;

  justify-content: center;

  gap: 16px;
}

@media (max-width: 768px) {
  .hero h1 {
    font-size: 64px;
  }

  .hero p {
    font-size: 18px;
  }

  .hero-actions {
    flex-direction: column;

    align-items: center;
  }
}
</style>
