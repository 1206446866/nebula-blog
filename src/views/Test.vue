<template>
  <div class="nebula-blog">
    <!-- 云背景 -->
    <div class="cloud-layer">
      <div
        v-for="c in clouds"
        :key="c.id"
        class="cloud"
        :style="{
          width: c.size + 'px',
          height: c.size + 'px',
          left: c.x + '%',
          top: c.y + '%',
          animationDuration: c.duration + 's',
          animationDelay: c.delay + 's',
        }"
      ></div>
    </div>

    <!-- 粒子 -->
    <canvas ref="canvas" class="particle-canvas"></canvas>

    <!-- 极光 -->
    <div class="aurora a1"></div>
    <div class="aurora a2"></div>
    <div class="aurora a3"></div>

    <!-- Header -->
    <header class="header">
      <div class="logo">Nebula Blog</div>
      <nav>
        <a>Home</a>
        <a>Articles</a>
        <a>Tags</a>
        <a>About</a>
      </nav>
    </header>

    <!-- Hero -->
    <section class="hero">
      <h1>Nebula Articles</h1>
      <p>Explore ideas, architecture, and engineering insights.</p>

      <div class="search">
        <input placeholder="Search articles..." />
      </div>
    </section>

    <!-- 分类栏 -->
    <section class="category-bar">
      <div v-for="c in categories" :key="c" class="cat">
        {{ c }}
      </div>
    </section>

    <!-- 文章流 -->
    <section class="articles">
      <div class="article-card" v-for="a in articles" :key="a.id">
        <div class="cover"></div>

        <div class="content">
          <div class="tag">
            {{ a.tag }}
          </div>

          <h3>{{ a.title }}</h3>

          <p>{{ a.desc }}</p>

          <div class="meta">
            <span>{{ a.date }}</span>
            <span>{{ a.read }} min read</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const clouds = ref(
  Array.from({ length: 20 }, (_, i) => ({
    id: i,
    x: Math.random() * 100,
    y: Math.random() * 100,
    size: 140 + Math.random() * 300,
    duration: 25 + Math.random() * 35,
    delay: Math.random() * 10,
  })),
)

const categories = ref(['All', 'Java', 'SpringBoot', 'Vue3', 'AI', 'System', 'Database', 'Cloud'])

const articles = ref(
  Array.from({ length: 12 }, (_, i) => ({
    id: i,
    tag: ['AI', 'Java', 'Cloud', 'Vue'][i % 4],
    title: 'Nebula Article ' + (i + 1),
    desc: 'A deep dive into architecture, performance and engineering design.',
    date: '2026-06-21',
    read: 5 + (i % 6),
  })),
)

const canvas = ref(null)

onMounted(() => {
  const ctx = canvas.value.getContext('2d')
  const w = (canvas.value.width = window.innerWidth)
  const h = (canvas.value.height = window.innerHeight)

  const particles = Array.from({ length: 90 }, () => ({
    x: Math.random() * w,
    y: Math.random() * h,
    vx: (Math.random() - 0.5) * 0.4,
    vy: (Math.random() - 0.5) * 0.4,
    r: Math.random() * 1.6 + 0.4,
  }))

  const draw = () => {
    ctx.clearRect(0, 0, w, h)

    for (const p of particles) {
      p.x += p.vx
      p.y += p.vy

      if (p.x < 0 || p.x > w) p.vx *= -1
      if (p.y < 0 || p.y > h) p.vy *= -1

      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = 'rgba(120,180,255,0.45)'
      ctx.fill()
    }

    requestAnimationFrame(draw)
  }

  draw()
})
</script>

<style scoped>
.nebula-blog {
  min-height: 100vh;
  background: #050814;
  color: #e6f0ff;
  overflow: hidden;
  position: relative;
  font-family: Inter, 'PingFang SC', sans-serif;
}

/* 云层 */
.cloud-layer {
  position: fixed;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.cloud {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(120, 160, 255, 0.18), transparent 70%);
  filter: blur(40px);
  animation: float linear infinite;
}

@keyframes float {
  0% {
    transform: translateY(0) translateX(0) scale(1);
  }
  50% {
    transform: translateY(-40px) translateX(30px) scale(1.1);
  }
  100% {
    transform: translateY(0) translateX(0) scale(1);
  }
}

/* 粒子 */
.particle-canvas {
  position: fixed;
  inset: 0;
  z-index: 1;
}

/* 极光 */
.aurora {
  position: absolute;
  width: 600px;
  height: 600px;
  filter: blur(120px);
  opacity: 0.25;
  border-radius: 50%;
}

.a1 {
  background: #4f7cff;
  left: -200px;
  top: -200px;
}

.a2 {
  background: #8b5cf6;
  right: -200px;
  top: 200px;
}

.a3 {
  background: #22d3ee;
  bottom: -200px;
  left: 30%;
}

/* Header */
.header {
  position: relative;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  padding: 24px 60px;
}

.logo {
  font-weight: 700;
  font-size: 20px;
}

nav {
  display: flex;
  gap: 28px;
}

nav a {
  color: #bcd0ff;
  cursor: pointer;
  transition: 0.3s;
}

nav a:hover {
  color: #fff;
}

/* Hero */
.hero {
  position: relative;
  z-index: 10;
  text-align: center;
  padding: 80px 20px 40px;
}

.hero h1 {
  font-size: 56px;
  margin-bottom: 10px;
}

.hero p {
  color: #9bb3d6;
}

.search {
  margin-top: 24px;
}

.search input {
  width: 320px;
  padding: 12px 18px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
  color: #fff;
}

/* 分类 */
.category-bar {
  position: relative;
  z-index: 10;
  display: flex;
  justify-content: center;
  gap: 14px;
  flex-wrap: wrap;
  margin: 30px 0;
}

.cat {
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
  transition: 0.3s;
}

.cat:hover {
  background: rgba(120, 160, 255, 0.15);
}

/* 文章流 */
.articles {
  position: relative;
  z-index: 10;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 22px;
  padding: 40px 60px 100px;
}

.article-card {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 18px;
  overflow: hidden;
  transition: 0.35s;
  backdrop-filter: blur(10px);
}

.article-card:hover {
  transform: translateY(-8px);
  border-color: rgba(120, 160, 255, 0.3);
}

.cover {
  height: 140px;
  background: linear-gradient(135deg, #4f7cff, #8b5cf6, #22d3ee);
}

.content {
  padding: 16px;
}

.tag {
  font-size: 12px;
  color: #7fb2ff;
  margin-bottom: 8px;
}

.meta {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
  font-size: 12px;
  color: #8aa3c7;
}
</style>
