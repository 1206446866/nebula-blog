<template>
  <section class="category-section">
    <!-- Header -->
    <div class="section-header">
      <h2 class="section-title">Knowledge Domains</h2>
      <p class="section-subtitle">知识领域与技术方向</p>
    </div>

    <!-- Grid -->
    <div class="category-grid">
      <!-- Backend -->
      <div class="category-card">
        <div class="icon">🖥️</div>
        <h3 class="card-title">Backend</h3>
        <p class="card-desc">
          Spring Boot · Security · JWT · Redis · MySQL · RabbitMQ · Kafka · MyBatis
        </p>
      </div>

      <!-- Frontend -->
      <div class="category-card">
        <div class="icon">🖌️</div>
        <h3 class="card-title">Frontend</h3>
        <p class="card-desc">Vue3 · TypeScript · Pinia · Element Plus · Axios · Vite</p>
      </div>

      <!-- AI -->
      <div class="category-card">
        <div class="icon">🤖</div>
        <h3 class="card-title">AI</h3>
        <p class="card-desc">OpenAI · LLM · RAG · Agent · Prompt Engineering</p>
      </div>

      <!-- DevOps -->
      <div class="category-card">
        <div class="icon">☁️</div>
        <h3 class="card-title">DevOps</h3>
        <p class="card-desc">Docker · Linux · Nginx · Kubernetes · Cloud · DevOps</p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
// 目前这个 Section 不需要 JS 逻辑，后续可以加滚动联动或点击动画
</script>

<style scoped>
.category-section {
  min-height: 100vh;
  padding: 120px 80px;
  background: radial-gradient(circle at center, rgba(56, 189, 248, 0.08), transparent 60%), #050816;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 80px;
}

.section-header {
  text-align: center;
}

.section-title {
  font-size: 48px;
  font-weight: 900;
  background: linear-gradient(90deg, #38bdf8, #818cf8, #22d3ee, #38bdf8);
  background-size: 200% 100%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: glowMove 4s linear infinite;
  text-shadow:
    0 0 12px rgba(56, 189, 248, 0.25),
    0 0 30px rgba(129, 140, 248, 0.15);
}

.section-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 8px;
}

@keyframes glowMove {
  0% {
    background-position: 0 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 28px;
  width: 100%;
  max-width: 1200px;
}

.category-card {
  position: relative;
  padding: 28px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.75), rgba(2, 6, 23, 0.4));
  border: 1px solid rgba(56, 189, 248, 0.15);
  backdrop-filter: blur(10px);
  transition: all 0.35s ease;
  cursor: pointer;
  overflow: hidden;
}

.category-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 0 30px rgba(56, 189, 248, 0.15);
  border-color: rgba(56, 189, 248, 0.4);
}

.icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #38bdf8;
  text-shadow: 0 0 10px rgba(56, 189, 248, 0.3);
}

.card-title {
  font-size: 24px;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 12px;
}

.card-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.6;
}
</style>
