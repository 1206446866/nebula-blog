<template>
  <section class="section">
    <div class="section-header">
      <span class="section-tag">ARTICLES</span>

      <h2>技术文章</h2>

      <p>从项目实战到架构设计， 记录开发过程中的经验、思考与成长。</p>
    </div>

    <div class="card-grid">
      <div class="article-card">
        <div class="icon">☕</div>

        <h3>Java & Spring Boot</h3>

        <p>后端开发、接口设计、 权限系统与工程实践。</p>
      </div>

      <div class="article-card">
        <div class="icon">⚡</div>

        <h3>Vue 3 & Frontend</h3>

        <p>Vue3、TypeScript、 Element Plus 与现代前端开发。</p>
      </div>

      <div class="article-card">
        <div class="icon">🤖</div>

        <h3>AI & Productivity</h3>

        <p>AI 应用实践、 Prompt Engineering 与效率工具。</p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
defineOptions({
  name: 'ArticleSection',
})
</script>

<style scoped>
.section {
  max-width: 1200px;

  margin: 0 auto;

  padding: 120px 24px;
}

.section-header {
  text-align: center;

  margin-bottom: 72px;
}

.section-tag {
  display: inline-block;

  padding: 8px 16px;

  border-radius: 999px;

  font-size: 13px;

  letter-spacing: 2px;

  color: #cbd5e1;

  border: 1px solid rgba(255, 255, 255, 0.08);

  background: rgba(255, 255, 255, 0.05);
}

.section-header h2 {
  margin-top: 24px;

  font-size: 48px;

  font-weight: 800;
}

.section-header p {
  margin-top: 16px;

  color: #94a3b8;

  font-size: 18px;
}

.card-grid {
  display: grid;

  grid-template-columns: repeat(3, 1fr);

  gap: 24px;
}

.article-card {
  padding: 32px;

  border-radius: 24px;

  background: rgba(255, 255, 255, 0.03);

  border: 1px solid rgba(255, 255, 255, 0.06);

  transition: all 0.3s ease;
}

.article-card:hover {
  transform: translateY(-6px);

  border-color: rgba(56, 189, 248, 0.3);
}

.icon {
  font-size: 40px;

  margin-bottom: 20px;
}

.article-card h3 {
  margin-bottom: 16px;

  font-size: 22px;
}

.article-card p {
  color: #94a3b8;

  line-height: 1.8;
}

@media (max-width: 900px) {
  .card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
