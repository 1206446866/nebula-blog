<script setup lang="ts">
</script>

<template>
  <section class="hero">
    <div class="container hero-inner">
      <div class="hero-left">
        <span class="hero-badge">星 云 博 客</span>

        <h1>探索技术的边界</h1>

        <p class="hero-subtitle">记录开发、设计与思考，分享值得沉淀的技术实践。</p>

        <div class="hero-stats">
          <div class="stat-item">
            <h2>32</h2>
            <span>文章</span>
          </div>

          <div class="stat-item">
            <h2>12</h2>
            <span>浏览</span>
          </div>

          <div class="stat-item">
            <h2>86</h2>
            <span>新文</span>
          </div>
        </div>
      </div>
    </div>

    <!-- subtle background glow -->
    <div class="bg-glow"></div>
  </section>
</template>

<style scoped>
.hero {
  position: relative;
  padding: 90px 0 70px;
  overflow: hidden;
}

/* layout */
.hero-inner {
  display: flex;
  justify-content: center;
}

.container {
  width: min(980px, calc(100% - 140px));
  margin: 0 auto;
}

/* badge */
.hero-badge {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;

  background: rgba(102, 179, 255, 0.12);
  border: 1px solid rgba(102, 179, 255, 0.25);

  font-size: 12px;
  letter-spacing: 1px;

  margin-bottom: 26px;

  color: rgba(255, 255, 255, 0.8);
}

/* title */
.hero h1 {
  font-size: 54px;
  line-height: 1.1;
  letter-spacing: -0.02em;

  margin-bottom: 18px;
}

/* subtitle */
.hero-subtitle {
  max-width: 560px;
  font-size: 16px;
  line-height: 1.85;

  color: rgba(255, 255, 255, 0.6);
}

/* stats */
.hero-stats {
  margin-top: 42px;
  display: flex;
  gap: 46px;
}

.stat-item {
  position: relative;
}

.stat-item h2 {
  font-size: 32px;
  font-weight: 600;
}

.stat-item span {
  display: block;
  margin-top: 4px;

  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}

/* divider */
.stat-item:not(:last-child)::after {
  content: '';
  position: absolute;
  right: -23px;
  top: 50%;
  transform: translateY(-50%);

  width: 1px;
  height: 22px;

  background: rgba(255, 255, 255, 0.08);
}

/* background glow */
.bg-glow {
  position: absolute;
  inset: -200px;

  background:
    radial-gradient(circle at 30% 20%, rgba(102, 179, 255, 0.18), transparent 40%),
    radial-gradient(circle at 70% 60%, rgba(120, 90, 255, 0.12), transparent 45%);

  filter: blur(60px);
  opacity: 0.8;

  z-index: -1;

  animation: floatGlow 10s ease-in-out infinite;
}

@keyframes floatGlow {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-18px);
  }
}
</style>
