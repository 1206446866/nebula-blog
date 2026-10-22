<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import NebulaCloud from './NebulaCloud.vue'

defineOptions({ name: 'ArticleSection' })

const section = ref<HTMLElement | null>(null)
const activeIndex = ref(0)
const motionEnabled = ref(true)
const isVisible = ref(true)
let observer: IntersectionObserver | undefined

const topics = [
  {
    id: 'backend',
    name: '后端与架构',
    star: '后端星域',
    english: 'BACKEND',
    description: '深入系统的运行轨迹，在接口、数据与架构之间，构建可靠的秩序。',
    tags: ['Java', 'Spring Boot', '系统设计'],
    color: '#8acbdd',
    rgb: '138, 203, 221',
    note: '在复杂之中，寻找清晰的结构。',
  },
  {
    id: 'frontend',
    name: '前端与体验',
    star: '前端星域',
    english: 'FRONTEND',
    description: '让想法拥有可见的形状，用交互、动效与细节，连接人与屏幕。',
    tags: ['Vue 3', 'TypeScript', '交互设计'],
    color: '#b5a2e0',
    rgb: '181, 162, 224',
    note: '让每一个想法，都有被看见的方式。',
  },
  {
    id: 'intelligence',
    name: 'AI 与新可能',
    star: '智能星域',
    english: 'INTELLIGENCE',
    description: '带着好奇心走向未知，探索智能工具如何改变创造与思考的方式。',
    tags: ['AI 应用', '智能体', '效率探索'],
    color: '#d9b68e',
    rgb: '217, 182, 142',
    note: '未知的边界，也是灵感的起点。',
  },
] as const

const activeTopic = computed(() => topics[activeIndex.value] ?? topics[0])
const isAnimated = computed(() => motionEnabled.value && isVisible.value)

onMounted(() => {
  if (!section.value || !('IntersectionObserver' in window)) return
  observer = new IntersectionObserver(
    ([entry]) => {
      isVisible.value = entry?.isIntersecting ?? false
    },
    { threshold: 0.08 },
  )
  observer.observe(section.value)
})
onBeforeUnmount(() => observer?.disconnect())
</script>

<template>
  <section
    id="knowledge-nebula"
    ref="section"
    class="nebula-section"
    :style="{ '--nebula-color': activeTopic.color, '--nebula-rgb': activeTopic.rgb }"
    aria-labelledby="nebula-title"
  >
    <div class="section-haze" aria-hidden="true" />
    <div class="nebula-inner">
      <header class="nebula-header">
        <p class="section-coordinate">
          <span class="coordinate-mark" aria-hidden="true">✳</span> THE KNOWLEDGE NEBULA
          <span class="coordinate-divider">/</span> 01
        </p>
        <button
          type="button"
          class="motion-button"
          :aria-label="motionEnabled ? '暂停星云动画' : '播放星云动画'"
          :aria-pressed="!motionEnabled"
          @click="motionEnabled = !motionEnabled"
        >
          <span class="status-dot" :class="{ 'is-paused': !motionEnabled }" />
          <span>{{ motionEnabled ? '星云漫游中' : '此刻，静静观测' }}</span>
          <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path v-if="motionEnabled" d="M6 4v8m4-8v8" stroke="currentColor" stroke-width="1.4" />
            <path v-else d="m6 4 6 4-6 4Z" fill="currentColor" />
          </svg>
        </button>
      </header>

      <div class="nebula-layout">
        <div class="nebula-copy">
          <p class="nebula-eyebrow"><span /> 每一次探索，都在发光</p>
          <h2 id="nebula-title">思想有引力，<br /><span>知识成星云。</span></h2>
          <p class="nebula-description">
            从一行代码，到一个完整的世界。<br />把开发路上的灵感、实践与思考，<br
              class="desktop-break"
            />连成自己的知识宇宙。
          </p>
          <RouterLink to="/article" class="explore-link"
            ><span>探索全部文章</span
            ><svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
              <path
                d="M4 10h12m-5-5 5 5-5 5"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              /></svg
          ></RouterLink>
          <p class="copy-footnote">让好奇心，先行一步。<span aria-hidden="true">↗</span></p>
        </div>

        <div class="nebula-observatory" aria-label="知识星域互动图">
          <NebulaCloud :animated="isAnimated" />
          <div class="star-nodes" role="group" aria-label="在星图中选择知识星域">
            <button
              v-for="(topic, index) in topics"
              :key="topic.id"
              type="button"
              class="star-node"
              :class="[`node-${topic.id}`, { 'is-active': activeIndex === index }]"
              :style="{ '--node-color': topic.color }"
              :aria-label="`观测${topic.star}`"
              :aria-pressed="activeIndex === index"
              @click="activeIndex = index"
            >
              <span class="node-beacon" aria-hidden="true"><span /></span>
              <span class="node-label"
                ><span class="node-code">0{{ index + 1 }} / {{ topic.english }}</span
                ><span class="node-name">{{ topic.star }}</span></span
              >
            </button>
          </div>
          <div class="observation-note" aria-live="polite">
            <span class="observation-dot" /><span>{{ activeTopic.note }}</span>
          </div>
        </div>
      </div>

      <div class="routes-heading">
        <span>找到你的探索航线</span
        ><span class="routes-hint"
          >三片星域，同一种好奇心
          <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
            <path d="M10 3v14m-5-5 5 5 5-5" stroke="currentColor" stroke-width="1" /></svg
        ></span>
      </div>
      <div class="topic-routes" role="group" aria-label="选择知识星域">
        <button
          v-for="(topic, index) in topics"
          :key="topic.id"
          type="button"
          class="topic-route"
          :class="{ 'is-active': activeIndex === index }"
          :style="{ '--route-color': topic.color, '--route-rgb': topic.rgb }"
          :aria-pressed="activeIndex === index"
          @click="activeIndex = index"
        >
          <span class="route-topline"
            ><span class="route-code">0{{ index + 1 }} <span>/</span> {{ topic.english }}</span
            ><span v-if="activeIndex === index" class="route-selected"><span /> 正在观测</span
            ><svg v-else class="route-arrow" viewBox="0 0 20 20" fill="none" aria-hidden="true">
              <path d="M5 15 15 5H5m10 0v10" stroke="currentColor" stroke-width="1" /></svg
          ></span>
          <span class="route-title"
            ><svg viewBox="0 0 32 32" fill="none" aria-hidden="true">
              <template v-if="topic.id === 'backend'">
                <path d="m16 3 12 7-12 7L4 10Z" />
                <path d="m4 16 12 7 12-7M4 22l12 7 12-7" />
              </template>
              <template v-else-if="topic.id === 'frontend'">
                <rect x="3" y="5" width="26" height="22" rx="3" />
                <path d="M3 12h26M9 8.5h.1m4 0h.1M12 17l-4 3 4 3m8-6 4 3-4 3" />
              </template>
              <template v-else>
                <path d="m16 3 3.5 9.5L29 16l-9.5 3.5L16 29l-3.5-9.5L3 16l9.5-3.5Z" />
                <path d="m26 3 1 3 3 1-3 1-1 3-1-3-3-1 3-1Z" />
              </template></svg
            >{{ topic.name }}</span
          >
          <span class="route-description">{{ topic.description }}</span>
          <span class="route-tags"
            ><span v-for="tag in topic.tags" :key="tag">{{ tag }}</span></span
          >
        </button>
      </div>
      <footer class="nebula-footer">
        <span>写作，是与未知建立连接。</span><span>EXPLORE <i>·</i> CONNECT <i>·</i> CREATE</span>
      </footer>
    </div>
  </section>
</template>

<style scoped>
.nebula-section {
  --nebula-color: #8acbdd;
  --nebula-rgb: 138, 203, 221;
  position: relative;
  isolation: isolate;
  overflow: hidden;
  padding: 82px 28px 62px;
  color: #eef0f5;
  background: #060a17;
  scroll-margin-top: 24px;
}
.nebula-section::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -1;
  background:
    radial-gradient(ellipse at 74% 36%, #27204438, transparent 57%),
    linear-gradient(#020617, transparent 100px, transparent calc(100% - 70px), #050816);
  pointer-events: none;
}
.section-haze {
  position: absolute;
  inset: 0;
  z-index: -1;
  background-image: radial-gradient(#99adc6 0.65px, transparent 0.65px);
  background-size: 73px 79px;
  opacity: 0.13;
  mask-image: linear-gradient(transparent, #000 22%, #000 65%, transparent);
  pointer-events: none;
}
.nebula-inner {
  max-width: 1180px;
  margin: 0 auto;
}
.nebula-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 22px;
  border-bottom: 1px solid #bbc7e110;
}
.section-coordinate {
  display: flex;
  align-items: center;
  gap: 11px;
  color: #8a96af;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 10px;
  letter-spacing: 2.3px;
}
.coordinate-mark {
  color: #c4cddd;
  font-size: 23px;
  line-height: 1;
}
.coordinate-divider {
  margin: 0 4px;
  color: #4a536a;
}
.motion-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 0 7px 12px;
  border: 0;
  background: transparent;
  color: #8c99b0;
  font: inherit;
  font-size: 11px;
  letter-spacing: 1px;
  cursor: pointer;
}
.motion-button svg {
  width: 14px;
  height: 14px;
  margin-left: 5px;
}
.status-dot,
.observation-dot {
  width: 4px;
  height: 4px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #a6c3b2;
  box-shadow: 0 0 9px #a6c3b255;
}
.status-dot.is-paused {
  background: #7f8aa0;
  box-shadow: none;
}
.nebula-layout {
  display: grid;
  grid-template-columns: 0.88fr 1.2fr;
  align-items: center;
  min-height: 510px;
}
.nebula-copy {
  position: relative;
  z-index: 1;
  padding: 46px 0 45px 2px;
}
.nebula-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #a8aec0;
  font-size: 11px;
  letter-spacing: 2px;
}
.nebula-eyebrow > span {
  width: 18px;
  height: 1px;
  background: #b1acc4;
}
h2 {
  margin: 24px 0 23px;
  font-size: clamp(40px, 4.7vw, 64px);
  font-weight: 600;
  line-height: 1.36;
  letter-spacing: -1px;
}
h2 > span {
  color: #b7c2dc;
}
.nebula-description {
  color: #8f9bb2;
  font-size: 14px;
  line-height: 2;
  letter-spacing: 0.7px;
}
.explore-link {
  display: inline-flex;
  align-items: center;
  gap: 30px;
  margin-top: 31px;
  padding: 13px 17px 13px 22px;
  border: 1px solid #e4e7e8;
  border-radius: 5px;
  color: #18202e;
  background: #e4e7e8;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
  text-decoration: none;
  transition:
    background 0.25s,
    transform 0.25s,
    box-shadow 0.25s;
}
.explore-link svg {
  width: 20px;
  height: 20px;
  transition: transform 0.25s;
}
.explore-link:hover {
  background: #fff;
  transform: translateY(-2px);
  box-shadow: 0 5px 30px #bdc9eb15;
}
.explore-link:hover svg {
  transform: translateX(3px);
}
.copy-footnote {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 18px;
  color: #626f86;
  font-size: 10px;
  letter-spacing: 1px;
}
.copy-footnote > span {
  color: #8c9ab2;
  font-size: 13px;
}
.nebula-observatory {
  position: relative;
  width: calc(100% + 54px);
  margin: -15px -44px -15px -10px;
}
.star-nodes {
  position: absolute;
  inset: 0;
}
.star-node {
  --node-color: #8acbdd;
  position: absolute;
  display: block;
  width: 24px;
  height: 24px;
  padding: 0;
  transform: translate(-50%, -50%);
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #d8deea;
  text-align: left;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}
.node-backend {
  left: 35.3%;
  top: 27.7%;
}
.node-frontend {
  left: 74.7%;
  top: 62.2%;
}
.node-intelligence {
  left: 15.8%;
  top: 70.3%;
}
.node-beacon {
  position: absolute;
  inset: 4px;
  display: grid;
  place-items: center;
  border: 1px solid #9eafca40;
  border-radius: 50%;
  background: #080e20cc;
  transition:
    border-color 0.3s,
    box-shadow 0.3s;
}
.node-beacon > span {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--node-color);
  box-shadow: 0 0 9px var(--node-color);
}
.node-label {
  position: absolute;
  top: -5px;
  left: 31px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 102px;
  padding: 4px 0;
  text-shadow: 0 2px 8px #030716;
  transition: opacity 0.3s;
}
.node-code {
  color: #7687a1;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 8px;
  letter-spacing: 1.1px;
  white-space: nowrap;
}
.node-name {
  color: #b2bdd1;
  font-size: 12px;
  letter-spacing: 2px;
  white-space: nowrap;
}
.node-frontend .node-label {
  right: 31px;
  left: auto;
  text-align: right;
}
.star-node.is-active .node-beacon,
.star-node:hover .node-beacon {
  border-color: var(--node-color);
  box-shadow:
    0 0 0 5px #a8c7ed08,
    0 0 19px #a8c7ed21;
}
.star-node.is-active .node-name,
.star-node:hover .node-name {
  color: #fff;
}
.star-node.is-active .node-code {
  color: var(--node-color);
}
.observation-note {
  position: absolute;
  bottom: 5.3%;
  left: 16%;
  display: flex;
  align-items: center;
  gap: 9px;
  color: #718099;
  font-size: 10px;
  letter-spacing: 1px;
}
.observation-dot {
  width: 3px;
  height: 3px;
  background: var(--nebula-color);
  box-shadow: 0 0 9px var(--nebula-color);
}
.routes-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 0 20px;
  border-top: 1px solid #bbc7e110;
  color: #a7b1c6;
  font-size: 12px;
  letter-spacing: 1px;
}
.routes-hint {
  display: inline-flex;
  align-items: center;
  gap: 15px;
  color: #5d6b82;
  font-size: 10px;
}
.routes-hint svg {
  width: 17px;
  height: 17px;
}
.topic-routes {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}
.topic-route {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
  overflow: hidden;
  padding: 24px;
  border: 1px solid #9caec31a;
  border-radius: 10px;
  background: linear-gradient(130deg, #141c2c80, #0c122080);
  color: #ced7e6;
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.35s,
    background 0.35s,
    transform 0.35s;
}
.topic-route::before {
  content: '';
  position: absolute;
  top: 0;
  right: 17%;
  left: 17%;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--route-color), transparent);
  opacity: 0;
  transition: opacity 0.35s;
}
.topic-route:hover {
  border-color: #9caec340;
  transform: translateY(-4px);
}
.topic-route.is-active {
  border-color: rgba(var(--route-rgb), 0.35);
  background: linear-gradient(135deg, rgba(var(--route-rgb), 0.09), #0c122080 90%);
}
.topic-route.is-active::before {
  opacity: 0.8;
}
.route-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  min-height: 18px;
}
.route-code {
  color: #68778e;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 9px;
  letter-spacing: 1.6px;
}
.route-code > span {
  margin: 0 5px;
  color: #35435a;
}
.route-selected {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--route-color);
  font-size: 9px;
  letter-spacing: 0.5px;
}
.route-selected > span {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: currentColor;
}
.route-arrow {
  width: 17px;
  height: 17px;
  color: #78869e;
}
.route-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 23px;
  color: #d8dfec;
  font-size: 19px;
  font-weight: 500;
  letter-spacing: 0.5px;
}
.route-title > svg {
  width: 29px;
  height: 29px;
  flex-shrink: 0;
  stroke: var(--route-color);
  stroke-width: 1.15;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.route-description {
  display: block;
  margin-top: 16px;
  color: #8390a7;
  font-size: 12px;
  line-height: 1.95;
  letter-spacing: 0.3px;
}
.route-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: auto;
  padding-top: 22px;
}
.route-tags > span {
  padding: 4px 7px;
  border: 1px solid #a9b9d010;
  border-radius: 3px;
  color: #7c8ba2;
  background: #adbed005;
  font-size: 9px;
  line-height: 1.5;
}
.topic-route.is-active .route-title {
  color: #eff5fc;
}
.topic-route.is-active .route-code {
  color: var(--route-color);
}
.nebula-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 24px;
  color: #536078;
  font-size: 10px;
  letter-spacing: 1px;
}
.nebula-footer > span:last-child {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 8px;
  letter-spacing: 2px;
}
.nebula-footer i {
  margin: 0 6px;
  font-style: normal;
}
button:focus-visible,
a:focus-visible {
  outline: 2px solid var(--nebula-color);
  outline-offset: 6px;
}
@media (min-width: 1600px) {
  .nebula-section {
    padding-top: 100px;
    padding-bottom: 85px;
  }
  .nebula-layout {
    min-height: 560px;
  }
}
@media (max-width: 1050px) {
  .nebula-layout {
    grid-template-columns: 0.9fr 1.15fr;
    min-height: 450px;
  }
  h2 {
    font-size: 46px;
  }
  .nebula-copy {
    padding-top: 36px;
    padding-bottom: 36px;
  }
  .nebula-observatory {
    width: calc(100% + 35px);
    margin-left: -15px;
    margin-right: -20px;
  }
  .topic-route {
    padding: 20px;
  }
  .route-title {
    font-size: 17px;
    gap: 9px;
  }
  .route-code {
    font-size: 8px;
    letter-spacing: 0.7px;
  }
}
@media (max-width: 780px) {
  .nebula-section {
    padding: 54px 24px 44px;
  }
  .section-coordinate {
    font-size: 8px;
    letter-spacing: 1.6px;
    gap: 7px;
  }
  .coordinate-mark {
    font-size: 19px;
  }
  .motion-button {
    font-size: 10px;
    letter-spacing: 0;
  }
  .nebula-header {
    padding-bottom: 18px;
  }
  .nebula-layout {
    grid-template-columns: 1fr;
  }
  .nebula-copy {
    padding: 42px 0 10px;
    text-align: center;
  }
  .nebula-eyebrow,
  .copy-footnote {
    justify-content: center;
  }
  h2 {
    font-size: clamp(40px, 7vw, 58px);
  }
  .nebula-description {
    font-size: 13px;
  }
  .nebula-observatory {
    width: min(640px, 100%);
    margin: -4px auto 12px;
  }
  .topic-routes {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  .topic-route {
    padding: 23px;
  }
  .route-title {
    margin-top: 16px;
    font-size: 20px;
  }
  .route-description {
    margin-top: 13px;
    font-size: 13px;
  }
  .route-tags {
    padding-top: 18px;
  }
  .route-code {
    font-size: 9px;
    letter-spacing: 1.5px;
  }
  .routes-hint {
    font-size: 9px;
    gap: 6px;
  }
  .observation-note {
    left: 12%;
    font-size: 9px;
  }
}
@media (max-width: 430px) {
  .nebula-section {
    padding-right: 19px;
    padding-left: 19px;
  }
  .coordinate-divider,
  .section-coordinate .coordinate-mark {
    display: none;
  }
  .section-coordinate {
    font-size: 7px;
    letter-spacing: 1.2px;
  }
  .motion-button svg {
    margin-left: 0;
  }
  h2 {
    font-size: 39px;
    letter-spacing: -0.6px;
  }
  .nebula-observatory {
    width: calc(100% + 24px);
    max-width: none;
    margin-left: -12px;
    margin-right: -12px;
  }
  .node-code {
    font-size: 6px;
    letter-spacing: 0.4px;
  }
  .node-name {
    font-size: 10px;
    letter-spacing: 1px;
  }
  .node-label {
    gap: 3px;
    min-width: 80px;
    left: 25px;
  }
  .node-frontend .node-label {
    right: 25px;
  }
  .observation-note {
    bottom: 2%;
    font-size: 8px;
    letter-spacing: 0;
  }
  .routes-heading {
    font-size: 11px;
  }
  .routes-hint {
    font-size: 8px;
  }
  .nebula-footer > span:last-child {
    font-size: 7px;
    letter-spacing: 0.6px;
  }
  .nebula-footer {
    font-size: 8px;
  }
  .nebula-footer i {
    margin: 0 2px;
  }
}
@media (prefers-reduced-motion: reduce) {
  *,
  *::before {
    transition: none !important;
    animation: none !important;
  }
}
</style>
