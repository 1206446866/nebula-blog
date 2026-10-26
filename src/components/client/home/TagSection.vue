<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { useTagSphere } from './logic/useTagSphere'

const { container, paused, ready, activeTag, resetView, rotateWithKey, tags } = useTagSphere()
</script>

<template>
  <section id="tag-orbit" class="tag-orbit-section" aria-labelledby="tag-orbit-title">
    <div class="orbit-grain" aria-hidden="true" />
    <div class="orbit-inner">
      <header class="orbit-header">
        <p class="orbit-coordinate"><span aria-hidden="true">✳</span> THE TAG ORBITS <i>/</i> 02</p>
        <button class="orbit-motion" type="button" :aria-pressed="paused" @click="paused = !paused">
          <span class="motion-light" :class="{ 'is-paused': paused }" />
          {{ paused ? '静静观测' : '星环自转中' }}
          <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path v-if="!paused" d="M6 4v8m4-8v8" stroke="currentColor" stroke-width="1.3" />
            <path v-else d="m6 4 6 4-6 4Z" fill="currentColor" />
          </svg>
        </button>
      </header>
      <div class="orbit-layout">
        <div class="orbit-observatory">
          <div class="orbit-reticle" aria-hidden="true"><span /><span /><span /><span /></div>
          <div class="orbit-axis orbit-axis-top" aria-hidden="true">N / 23.4°</div>
          <div class="orbit-axis orbit-axis-bottom" aria-hidden="true">NEBULA · ORBITAL STUDY</div>
          <div
            ref="container"
            class="tag-sphere"
            role="group"
            tabindex="0"
            aria-label="可拖动的三维示例标签球，也可用方向键旋转"
            @keydown="rotateWithKey"
          />
          <div v-if="!ready" class="sphere-fallback" aria-label="示例标签星环">
            <div class="fallback-planet" />
            <div class="fallback-ring" />
            <span v-for="(tag, index) in tags.slice(0, 8)" :key="tag" :style="{ '--i': index }">{{
              tag
            }}</span>
          </div>
          <div class="sphere-caption">
            <span class="caption-line" />
            <div>
              <span class="caption-code">IN ORBIT / 灵感切片</span>
              <p>{{ activeTag }}</p>
            </div>
            <span class="caption-cross" aria-hidden="true">＋</span>
          </div>
          <div class="sphere-controls">
            <span>{{ ready ? '拖动球体，换个角度发现' : '每一种好奇，都有自己的轨道' }}</span
            ><button v-if="ready" type="button" aria-label="重置标签球视角" @click="resetView">
              重置视角 <span aria-hidden="true">↺</span>
            </button>
          </div>
        </div>
        <div class="orbit-copy">
          <p class="orbit-eyebrow"><span /> 让兴趣，彼此环绕</p>
          <h2 id="tag-orbit-title">灵感无界，<br /><em>各有引力。</em></h2>
          <p class="orbit-description">
            一个标签，是一个微小的坐标。<br />相近的好奇在此相遇，<br />散落的想法，慢慢连成自己的星环。
          </p>
          <div class="orbit-paths" aria-label="标签板块介绍">
            <div>
              <span class="path-number">01</span>
              <p>循着兴趣<small>从一个关键词，遇见更多可能</small></p>
              <span class="path-orbit" aria-hidden="true" />
            </div>
            <div>
              <span class="path-number">02</span>
              <p>连接灵感<small>让不同领域的思考，在轨道上交汇</small></p>
              <span class="path-orbit second-orbit" aria-hidden="true" />
            </div>
          </div>
          <RouterLink to="/tag" class="orbit-explore"
            ><span>进入标签星环</span
            ><svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M5 12h14m-6-6 6 6-6 6" stroke="currentColor" stroke-width="1.3" /></svg
          ></RouterLink>
          <p class="orbit-footnote">此刻绕行的，是一些灵感的名字。</p>
        </div>
      </div>
      <footer class="orbit-footer">
        <span>小小的坐标，通往辽阔的世界。</span
        ><span>EXPLORE BY CURIOSITY <i aria-hidden="true">↗</i></span>
      </footer>
    </div>
  </section>
</template>

<style scoped>
.tag-orbit-section {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  background: radial-gradient(ellipse at 30% 48%, #16212b 0, #0a111a 35%, #070c14 72%);
  color: #e6e1d6;
}
.tag-orbit-section::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(ellipse at 22% 56%, #bd9b6420, transparent 40%),
    linear-gradient(#070b17, transparent 12%, transparent 87%, #070b17);
}
.orbit-grain {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.22;
  background-image:
    radial-gradient(circle, #c2b69a 0 0.6px, transparent 1px),
    radial-gradient(circle, #8fadc0 0 0.6px, transparent 1px);
  background-size:
    173px 149px,
    257px 211px;
  background-position:
    31px 73px,
    105px 20px;
  mask-image: linear-gradient(transparent, #000 20%, #000 80%, transparent);
}
.orbit-inner {
  position: relative;
  max-width: 1440px;
  margin: 0 auto;
  padding: 52px 64px 30px;
}
.orbit-header,
.orbit-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}
.orbit-coordinate {
  display: flex;
  align-items: center;
  gap: 15px;
  color: #a5a59f;
  font-size: 10px;
  letter-spacing: 0.22em;
}
.orbit-coordinate > span {
  color: #ceaf79;
  font-size: 24px;
}
.orbit-coordinate i {
  font-style: normal;
  color: #595e61;
  padding: 0 6px;
}
.orbit-motion {
  display: flex;
  gap: 10px;
  align-items: center;
  border: 1px solid #d8c49c24;
  border-radius: 30px;
  background: #d8c49c04;
  padding: 10px 14px;
  color: #aaa99e;
  font: inherit;
  font-size: 10px;
  cursor: pointer;
}
.orbit-motion svg {
  width: 14px;
  height: 14px;
  margin-left: 8px;
}
.motion-light {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #d4b77d;
  box-shadow: 0 0 10px #d4b77d99;
}
.motion-light.is-paused {
  background: #777d80;
  box-shadow: none;
}
.orbit-layout {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  align-items: center;
  gap: 24px;
  min-height: 660px;
}
.orbit-observatory {
  position: relative;
  height: 620px;
  min-width: 0;
}
.tag-sphere {
  position: absolute;
  inset: 0 -65px;
  z-index: 2;
  touch-action: pan-y;
  cursor: grab;
  border-radius: 40%;
}
.tag-sphere:active {
  cursor: grabbing;
}
.tag-sphere :deep(canvas) {
  display: block;
  width: 100%;
  height: 100%;
}
.tag-sphere:focus-visible {
  outline: 1px dashed #c7ae7266;
  outline-offset: -50px;
}
.orbit-reticle {
  position: absolute;
  inset: 82px 12% 105px;
  border: 1px solid #aeb7bd0a;
  border-radius: 50%;
}
.orbit-reticle::after {
  content: '';
  position: absolute;
  inset: -14px;
  border-radius: 50%;
  border: 1px dashed #aeb7bd0c;
}
.orbit-reticle span {
  position: absolute;
  width: 5px;
  height: 5px;
  border: 1px solid #8c8b79;
  border-radius: 50%;
}
.orbit-reticle span:nth-child(1) {
  left: 50%;
  top: -3px;
}
.orbit-reticle span:nth-child(2) {
  right: -3px;
  top: 50%;
}
.orbit-reticle span:nth-child(3) {
  left: 50%;
  bottom: -3px;
}
.orbit-reticle span:nth-child(4) {
  left: -3px;
  top: 50%;
}
.orbit-axis {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  color: #7a817e;
  font-size: 8px;
  letter-spacing: 0.19em;
  white-space: nowrap;
}
.orbit-axis-top {
  top: 42px;
}
.orbit-axis-bottom {
  bottom: 60px;
  color: #6c716d;
}
.sphere-caption {
  position: absolute;
  z-index: 3;
  left: 8px;
  bottom: 100px;
  display: flex;
  align-items: center;
  gap: 12px;
  pointer-events: none;
}
.caption-line {
  width: 30px;
  height: 1px;
  background: #bca17088;
}
.caption-code {
  color: #89908e;
  font-size: 8px;
  letter-spacing: 0.16em;
}
.sphere-caption p {
  color: #e5d6b4;
  margin-top: 8px;
  font:
    21px Georgia,
    'Songti SC',
    'SimSun',
    serif;
}
.caption-cross {
  align-self: flex-end;
  color: #ad956b;
  margin: 0 0 2px 22px;
  font-size: 12px;
}
.sphere-controls {
  position: absolute;
  z-index: 3;
  bottom: 15px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  font-size: 10px;
  color: #818a8c;
}
.sphere-controls button {
  border: 0;
  border-left: 1px solid #d8c49c26;
  padding: 0 0 0 20px;
  background: none;
  color: #b5aa92;
  font: inherit;
  cursor: pointer;
}
.sphere-controls button span {
  padding-left: 5px;
}
.orbit-copy {
  position: relative;
  z-index: 3;
  padding: 30px 0 30px 28px;
}
.orbit-eyebrow {
  font-size: 11px;
  color: #b4a384;
  letter-spacing: 0.22em;
  display: flex;
  align-items: center;
  gap: 10px;
}
.orbit-eyebrow > span {
  width: 18px;
  height: 1px;
  background: #c8ab75;
}
.orbit-copy h2 {
  margin: 24px 0;
  color: #e5e2d9;
  font:
    400 clamp(38px, 4.1vw, 60px)/1.43 'Noto Serif SC',
    'Songti SC',
    'SimSun',
    Georgia,
    serif;
  letter-spacing: 0.06em;
}
.orbit-copy h2 em {
  font-style: normal;
  color: #d6bb89;
}
.orbit-description {
  font-size: 13px;
  line-height: 2.2;
  color: #959d9e;
  letter-spacing: 0.06em;
}
.orbit-paths {
  margin: 28px 0 30px;
  max-width: 355px;
}
.orbit-paths > div {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px 0;
  border-bottom: 1px solid #c9b89119;
}
.path-number {
  color: #928369;
  font:
    10px Georgia,
    serif;
}
.orbit-paths p {
  color: #c5c4bc;
  font-size: 12px;
  letter-spacing: 0.12em;
}
.orbit-paths small {
  display: block;
  font-size: 10px;
  color: #747e81;
  margin-top: 7px;
  letter-spacing: 0.03em;
}
.path-orbit {
  width: 26px;
  height: 12px;
  border: 1px solid #b9a17866;
  border-radius: 50%;
  transform: rotate(-30deg);
  margin-left: auto;
}
.path-orbit::after {
  content: '';
  display: block;
  width: 4px;
  height: 4px;
  background: #b9a178;
  border-radius: 50%;
  margin: -2px 0 0 6px;
}
.second-orbit {
  transform: rotate(30deg);
}
.orbit-explore {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 36px;
  min-width: 220px;
  padding: 15px 0;
  color: #e2c78f;
  font-size: 13px;
  letter-spacing: 0.16em;
  border-bottom: 1px solid #d7b87588;
  transition:
    color 0.2s,
    border-color 0.2s;
}
.orbit-explore svg {
  width: 21px;
  height: 21px;
  transition: transform 0.2s;
}
.orbit-explore:hover {
  color: #fff0cd;
  border-color: #ffe1a7;
}
.orbit-explore:hover svg {
  transform: translateX(5px);
}
.orbit-footnote {
  font-size: 9px;
  color: #656f73;
  margin-top: 15px;
  letter-spacing: 0.09em;
}
.orbit-footer {
  border-top: 1px solid #b6b8ad16;
  padding-top: 22px;
  color: #6f777b;
  font-size: 10px;
  letter-spacing: 0.1em;
}
.orbit-footer > span:last-child {
  font-size: 8px;
  letter-spacing: 0.2em;
}
.orbit-footer i {
  font-size: 15px;
  color: #a49273;
  font-style: normal;
  margin-left: 20px;
}
button:focus-visible,
a:focus-visible {
  outline: 1px solid #e2c78f;
  outline-offset: 6px;
}
/* 图形能力不可用时，用静态球体和标签保留板块的视觉入口。 */
.sphere-fallback {
  position: absolute;
  inset: 110px 50px;
  pointer-events: none;
}
.fallback-planet {
  position: absolute;
  inset: 40px 60px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, #39434b, #111923 55%, #080e17 72%);
  box-shadow:
    inset 8px 3px 22px #d3bb8b22,
    0 0 45px #b69a6522;
}
.fallback-ring {
  position: absolute;
  inset: 80px -10px;
  border-radius: 50%;
  border: 15px double #b39b6e99;
  transform: rotate(-24deg) scaleY(0.5);
}
.sphere-fallback > span {
  position: absolute;
  left: calc(6% + mod(var(--i), 4) * 24%);
  top: calc(20% + round(down, var(--i) / 4) * 58%);
  color: #c6c8c2;
  font-size: 12px;
}
@media (min-width: 1500px) {
  .orbit-layout {
    min-height: 730px;
  }
  .orbit-observatory {
    height: 690px;
  }
}
@media (max-width: 1000px) {
  .orbit-inner {
    padding: 40px 32px 25px;
  }
  .orbit-layout {
    gap: 0;
    grid-template-columns: 1.15fr 1fr;
  }
  .orbit-observatory {
    height: 590px;
  }
  .orbit-copy {
    padding-left: 18px;
  }
  .orbit-copy h2 {
    font-size: 42px;
  }
  .tag-sphere {
    inset: 0 -60px;
  }
  .orbit-reticle {
    inset: 100px 2% 120px;
  }
  .sphere-caption {
    left: -5px;
    bottom: 100px;
  }
  .caption-cross {
    display: none;
  }
  .orbit-axis-top {
    top: 70px;
  }
  .orbit-axis-bottom {
    bottom: 70px;
  }
  .sphere-controls {
    font-size: 9px;
    gap: 10px;
  }
  .sphere-controls button {
    padding-left: 10px;
  }
}
@media (max-width: 700px) {
  .orbit-inner {
    padding: 32px 24px 24px;
  }
  .orbit-coordinate {
    font-size: 8px;
    gap: 9px;
    letter-spacing: 0.15em;
  }
  .orbit-coordinate > span {
    font-size: 20px;
  }
  .orbit-coordinate i {
    padding: 0;
  }
  .orbit-motion {
    padding: 8px 10px;
    gap: 6px;
    font-size: 9px;
  }
  .orbit-motion svg {
    margin-left: 0;
    width: 12px;
  }
  .orbit-layout {
    display: flex;
    flex-direction: column;
    min-height: auto;
  }
  .orbit-copy {
    order: -1;
    padding: 42px 0 0;
    width: 100%;
  }
  .orbit-eyebrow {
    font-size: 10px;
  }
  .orbit-copy h2 {
    font-size: clamp(35px, 9vw, 48px);
    margin: 20px 0 16px;
  }
  .orbit-copy h2 br {
    display: none;
  }
  .orbit-copy h2 em {
    display: block;
  }
  .orbit-description {
    font-size: 12px;
    line-height: 2;
  }
  .orbit-paths {
    display: none;
  }
  .orbit-explore {
    min-width: 195px;
    font-size: 12px;
    padding: 13px 0;
    margin-top: 15px;
  }
  .orbit-footnote {
    display: none;
  }
  .orbit-observatory {
    width: 100%;
    height: 435px;
    margin: 4px 0 0;
  }
  .tag-sphere {
    inset: -12px -48px 0;
  }
  .orbit-reticle {
    inset: 75px 9% 74px;
  }
  .orbit-axis-top {
    top: 47px;
  }
  .orbit-axis-bottom {
    bottom: 44px;
    font-size: 7px;
  }
  .sphere-caption {
    bottom: 75px;
    left: 0;
    gap: 8px;
  }
  .caption-line {
    width: 17px;
  }
  .caption-code {
    font-size: 7px;
  }
  .sphere-caption p {
    font-size: 17px;
  }
  .sphere-controls {
    bottom: 6px;
  }
  .orbit-footer {
    padding-top: 18px;
    font-size: 9px;
  }
  .orbit-footer > span:last-child {
    font-size: 7px;
    letter-spacing: 0.1em;
  }
  .orbit-footer i {
    margin-left: 6px;
  }
  .sphere-fallback {
    inset: 80px 15px;
  }
}
@media (max-width: 380px) {
  .orbit-inner {
    padding-right: 18px;
    padding-left: 18px;
  }
  .orbit-header {
    gap: 8px;
  }
  .orbit-coordinate {
    gap: 6px;
    font-size: 7px;
    letter-spacing: 0.08em;
    white-space: nowrap;
  }
  .orbit-motion {
    padding: 7px 8px;
    font-size: 8px;
    white-space: nowrap;
  }
  .orbit-footer > span:last-child {
    display: none;
  }
}
@media (prefers-reduced-motion: reduce) {
  .orbit-explore,
  .orbit-explore svg {
    transition: none;
  }
}
</style>
