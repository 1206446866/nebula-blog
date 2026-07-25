<template>
  <section class="sphere-section">
    <div class="left">
      <div ref="container" class="sphere-container"></div>
    </div>

    <div class="right">
      <div class="info-card">
        <!-- 扫描光（独立层） -->
        <div class="scan-line">
          <div class="scan-beam"></div>
        </div>

        <!-- 内容层 -->
        <div class="card-content">
          <!-- 标题区 -->
          <div class="header">
            <h1 class="title">标签</h1>
            <p class="subtitle">技术标签 · 交互式知识星云系统</p>
          </div>

          <div class="divider"></div>

          <div class="content">
            <p>• 基于 Three.js 构建的 3D 标签系统</p>
            <p>• 支持动态光影与空间分布算法</p>
            <p>• 用于展示技术栈 / 项目能力 / 知识结构</p>

            <div class="tags">
              <span>Vue3</span>
              <span>Spring Boot</span>
              <span>Three.js</span>
              <span>WebGL</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import * as THREE from 'three'

const container = ref<HTMLDivElement | null>(null)
// ================================
// 🌌 Nebula Tag Sphere（核心配置）
// ================================

// 球体基础参数（控制整体视觉尺度）
const radius = 90
// ↑ 标签球分布半径
// 调大：更松散 / 更“星云”
// 调小：更紧凑 / 更“核心”

// 标签球体大小
const tagSphereSize = 12
// ↑ 单个小球大小（视觉主体）

// 标签文字大小
const labelScale = 26
// ↑ Sprite 文字缩放
// 建议范围：20 ~ 32

// 外发光球体范围
const glowOffset = 30
// ↑ 外壳 = radius + glowOffset
// 控制“能量场”厚度

// 外发光强度
const glowIntensity = 5.95
// ↑ shader 发光倍率（越大越科幻，但容易过曝）

// ================================
// 🧠 标签文本处理
// ================================

function createTagTexture(text: string) {
  const canvas = document.createElement('canvas')

  canvas.width = 512
  canvas.height = 512

  const ctx = canvas.getContext('2d')!

  // 清空画布（防止旧帧残留）
  ctx.clearRect(0, 0, canvas.width, canvas.height)

  // ================================
  // ✨ 发光效果（文字辉光）
  // ================================
  ctx.shadowColor = '#7dd3fc'
  ctx.shadowBlur = 25

  ctx.fillStyle = '#ffffff'

  // 字体大小（核心视觉参数）
  ctx.font = 'bold 64px Arial'

  // 文本居中对齐
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'

  const centerX = canvas.width / 2
  const centerY = canvas.height / 2
  const lineHeight = 48

  const lines = text.split(' ')

  // ================================
  // 🧾 单行 / 双行逻辑
  // ================================
  if (lines.length === 1) {
    ctx.fillText(text, centerX, centerY)
  } else {
    ctx.fillText(lines[0] ?? '', centerX, centerY - lineHeight / 2)
    ctx.fillText(lines[1] ?? '', centerX, centerY + lineHeight / 2)
  }

  return new THREE.CanvasTexture(canvas)
}

// ================================
// 🎬 Three.js 场景初始化
// ================================

onMounted(() => {
  if (!container.value) return

  // 场景
  const scene = new THREE.Scene()

  // 相机（决定整体视野远近）
  const camera = new THREE.PerspectiveCamera(
    60,
    container.value.clientWidth / container.value.clientHeight,
    0.1,
    1000,
  )

  camera.position.z = 250
  // ↑ 相机距离（越大整体越小）

  // 渲染器
  const renderer = new THREE.WebGLRenderer({
    alpha: true,
    antialias: true,
  })

  renderer.setSize(container.value.clientWidth, container.value.clientHeight)

  container.value.appendChild(renderer.domElement)

  // ================================
  // 💡 光照系统
  // ================================

  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6)
  scene.add(ambientLight)

  const pointLight = new THREE.PointLight(0x38bdf8, 3)
  pointLight.position.set(200, 200, 200)
  scene.add(pointLight)

  // ================================
  // 🌐 主容器（所有球体）
  // ================================
  const group = new THREE.Group()
  scene.add(group)

  // ================================
  // 🌌 外层能量壳（Glow Sphere）
  // ================================
  const glowGeometry = new THREE.SphereGeometry(radius + glowOffset, 64, 64)

  const glowMaterial = new THREE.ShaderMaterial({
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,

    vertexShader: `
      varying vec3 vNormal;

      void main() {
        vNormal = normalize(normalMatrix * normal);

        gl_Position =
          projectionMatrix *
          modelViewMatrix *
          vec4(position,1.0);
      }
    `,

    fragmentShader: `
      varying vec3 vNormal;

      void main() {

        float intensity =
          pow(1.0 - abs(vNormal.z), 2.5);

        vec3 glowColor = vec3(0.22, 0.74, 0.97);

        gl_FragColor = vec4(glowColor, intensity * ${glowIntensity});
      }
    `,
  })

  const glowSphere = new THREE.Mesh(glowGeometry, glowMaterial)
  scene.add(glowSphere)

  // ================================
  // 🪐 标签球体生成
  // ================================

  const tags = [
    'Vue3',
    'Spring Boot',
    'Java',
    'Docker',
    'Redis',
    'MySQL',
    'AI',
    'Security',
    'Nebula',
    'JWT',
    'Linux',
    'Git',
    'Maven',
    'Nginx',
    'RabbitMQ',
    'Elastic Search',
    'Kafka',
    'Type Script',
    'Element Plus',
    'Vite',
    'Pinia',
    'Axios',
    'JPA',
    'MyBatis',
    'PostgreSQL',
    'MongoDB',
    'Kubernetes',
    'Cloud',
    'DevOps',
    'OpenAI',
  ]

  tags.forEach((text, i) => {
    const geometry = new THREE.SphereGeometry(tagSphereSize, 32, 32)

    const material = new THREE.ShaderMaterial({
      transparent: true,

      vertexShader: `
        varying vec3 vPosition;
        varying vec3 vNormal;

        void main() {
          vPosition = position;
          vNormal = normalize(normalMatrix * normal);

          gl_Position =
            projectionMatrix *
            modelViewMatrix *
            vec4(position,1.0);
        }
      `,

      fragmentShader: `
        varying vec3 vPosition;
        varying vec3 vNormal;

        void main() {

          float t = (vPosition.y + 10.0) / 20.0;

          vec3 top = vec3(0.55,0.36,0.96);
          vec3 middle = vec3(0.22,0.74,0.97);
          vec3 bottom = vec3(0.08,0.72,0.65);

          vec3 color;

          if(t > 0.5){
            color = mix(middle, top, (t - 0.5) * 2.0);
          } else {
            color = mix(bottom, middle, t * 2.0);
          }

          float Fresnel =
            pow(1.0 - abs(vNormal.z), 3.0);

          color += Fresnel * 1.2;

          float alpha = 0.65 + Fresnel * 0.85;

          gl_FragColor = vec4(color, alpha);
        }
      `,
    })

    const sphere = new THREE.Mesh(geometry, material)

    // ================================
    // 🏷️ 标签文字（Sprite）
    // ================================
    const texture = createTagTexture(text)

    const labelMaterial = new THREE.SpriteMaterial({
      map: texture,
      transparent: true,
      depthTest: false,
    })

    const label = new THREE.Sprite(labelMaterial)

    label.scale.set(labelScale, labelScale, 1)

    // 居中锚点
    label.center.set(0.5, 0.5)

    sphere.add(label)

    // ================================
    // 🌍 球体分布（Fibonacci Sphere）
    // ================================

    const phi = Math.acos(-1 + (2 * i) / tags.length)
    const theta = Math.sqrt(tags.length * Math.PI) * phi

    sphere.position.x = radius * Math.cos(theta) * Math.sin(phi)
    sphere.position.y = radius * Math.sin(theta) * Math.sin(phi)
    sphere.position.z = radius * Math.cos(phi)

    group.add(sphere)
  })

  // ================================
  // 🎞️ 动画循环
  // ================================

  const animate = () => {
    requestAnimationFrame(animate)

    // 整体旋转（星云运动）
    group.rotation.y += 0.0015
    group.rotation.x += 0.0006

    group.children.forEach((child) => {
      const sphere = child as THREE.Mesh

      const worldPos = new THREE.Vector3()
      sphere.getWorldPosition(worldPos)

      // 远近缩放（增强3D层次）
      const zFactor = (worldPos.z + radius) / (radius * 2)
      const scale = 0.9 + zFactor * 0.1
      sphere.scale.set(scale, scale, scale)

      // 文字始终朝向相机
      child.children.forEach((label) => {
        label.lookAt(camera.position)
      })
    })

    renderer.render(scene, camera)

    // 外壳呼吸效果
    const time = Date.now() * 0.001
    const pulse = 1 + Math.sin(time) * 0.03
    glowSphere.scale.set(pulse, pulse, pulse)
  }

  animate()
})
</script>
<style scoped>
.sphere-section {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  /* 🌌 更深一点的宇宙背景 */
  background:
    radial-gradient(circle at 30% 50%, rgba(56, 189, 248, 0.18), transparent 60%),
    radial-gradient(circle at 80% 40%, rgba(129, 140, 248, 0.12), transparent 55%), #050816;
  overflow: hidden;
  gap: 60px;
}
.left {
  flex: 6;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  position: relative;
}
.sphere-container {
  width: 100%;
  height: 100vh;
}
.right {
  flex: 4;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding-right: 80px;
}
.info-card {
  position: relative;
  overflow: hidden;
  width: 420px;
  padding: 28px;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.6), rgba(2, 6, 23, 0.3));
  border: 1px solid rgba(56, 189, 248, 0.2);
  border-radius: 16px;
  transition: all 0.4s ease;
  backdrop-filter: blur(12px);
  box-shadow: 0 0 40px rgba(56, 189, 248, 0.08);
}

.info-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 0 60px rgba(56, 189, 248, 0.18);
  border-color: rgba(56, 189, 248, 0.35);
}
.info-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 16px;
  padding: 1px;
  background: linear-gradient(120deg, transparent, rgba(56, 189, 248, 0.4), transparent);
  opacity: 0.4;
}
.card-content {
  position: relative;
  z-index: 2;
}
.scan-line {
  width: 45%;
}
.scan-beam {
  position: absolute;
  top: 50%;
  left: -80%;
  width: 120%;
  height: 520px;
  transform: translateY(-50%);
  clip-path: polygon(0 10%, 80% 10%, 100% 50%, 80% 90%, 0 90%);
  animation: scanMove 3s linear infinite;
  background: linear-gradient(
    to right,
    rgba(56, 189, 248, 0) 0%,
    rgba(56, 189, 248, 0.02) 20%,
    rgba(56, 189, 248, 0.08) 45%,
    rgba(56, 189, 248, 0.18) 65%,
    rgba(56, 189, 248, 0.45) 85%,
    rgba(125, 211, 252, 0.95) 100%
  );
}
@keyframes scanDotMove {
  from {
    left: 40%;
  }

  to {
    left: 220%;
  }
}

@keyframes scanMove {
  from {
    left: -120%;
  }

  to {
    left: 100%;
  }
}
/* 标题区 */
.header {
  margin-bottom: 16px;
}

/* 主标题（核心科技渐变） */
.title {
  font-size: 64px;
  font-weight: 900;
  letter-spacing: 2px;
  /* 🌈 动态渐变 */
  background: linear-gradient(90deg, #38bdf8, #818cf8, #22d3ee, #38bdf8);
  background-size: 200% 100%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  /* ✨ 流动动画 */
  animation: glowMove 4s linear infinite;
  /* 🔥 更强发光 */
  text-shadow:
    0 0 10px rgba(56, 189, 248, 0.25),
    0 0 25px rgba(129, 140, 248, 0.18),
    0 0 50px rgba(34, 211, 238, 0.08);
}
/* 🌊 渐变流动动画 */
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
/* 副标题 */
.subtitle {
  font-size: 14px;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.6);
  /* ✨ 微弱发光 */
  text-shadow: 0 0 12px rgba(56, 189, 248, 0.15);
}
/* 分割线（科技感） */
.divider {
  height: 1px;
  margin: 18px 0;
  background: linear-gradient(90deg, transparent, rgba(56, 189, 248, 0.5), transparent);
}

/* 内容区 */
.content p {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  margin: 8px 0;
  line-height: 1.6;
}

/* 标签块 */
.tags {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tags span {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(56, 189, 248, 0.12);
  border: 1px solid rgba(56, 189, 248, 0.25);
  transition: all 0.3s ease;
  cursor: pointer;
  color: #7dd3fc;
}
.tags span:hover {
  transform: scale(1.08);
  background: rgba(56, 189, 248, 0.25);
  box-shadow: 0 0 12px rgba(56, 189, 248, 0.25);
}
</style>
