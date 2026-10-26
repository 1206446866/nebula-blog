<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { useCategorySection } from './logic/useCategorySection'

const {
  section,
  activeIndex,
  activeRegion,
  regions,
  uid,
  paint,
  stars,
  motionEnabled,
  reducedMotion,
  isAnimated,
} = useCategorySection()
</script>

<template>
  <section
    id="category-atlas"
    ref="section"
    class="atlas-section"
    :style="{ '--atlas-color': activeRegion.color, '--atlas-rgb': activeRegion.rgb }"
    :class="{ 'is-still': !isAnimated }"
    aria-labelledby="atlas-title"
  >
    <div class="atlas-inner">
      <header class="atlas-header">
        <p class="atlas-coordinate">
          <span aria-hidden="true">✳</span> THE CELESTIAL ATLAS <i>/</i> 03
        </p>
        <button
          class="atlas-motion"
          type="button"
          :aria-pressed="!motionEnabled"
          :disabled="reducedMotion"
          :aria-label="motionEnabled ? '暂停星图动画' : '播放星图动画'"
          @click="motionEnabled = !motionEnabled"
        >
          <span :class="{ 'is-paused': !isAnimated }" />{{
            isAnimated ? '星图观测中' : '此刻，静静观测'
          }}<svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path v-if="motionEnabled" d="M6 4v8m4-8v8" stroke="currentColor" stroke-width="1.3" />
            <path v-else d="m6 4 6 4-6 4Z" fill="currentColor" />
          </svg>
        </button>
      </header>

      <div class="atlas-layout">
        <div class="atlas-copy">
          <p class="atlas-eyebrow"><span /> 为好奇心，找到方向</p>
          <h2 id="atlas-title">独自闪耀，<br /><em>也彼此成座。</em></h2>
          <p class="atlas-description">
            把散落的知识连成星座，<br />让每一条探索的路，都有迹可循。<br />总有一片星域，与你此刻的好奇相逢。
          </p>
          <RouterLink to="/category" class="atlas-explore"
            ><span>展开全部分类</span
            ><svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M5 12h14m-6-6 6 6-6 6" stroke="currentColor" stroke-width="1.3" /></svg
          ></RouterLink>
          <p class="atlas-footnote">从一个方向出发，看见更多世界。</p>
        </div>

        <div class="atlas-observatory" role="group" aria-label="点亮示例分类星座">
          <svg class="atlas-chart" viewBox="0 0 720 560" fill="none" aria-hidden="true">
            <defs>
              <radialGradient :id="`${uid}-haze`">
                <stop stop-color="var(--atlas-color)" stop-opacity=".09" />
                <stop offset="1" stop-color="var(--atlas-color)" stop-opacity="0" />
              </radialGradient>
              <radialGradient :id="`${uid}-star`">
                <stop stop-color="#eef9ff" />
                <stop offset=".12" stop-color="var(--atlas-color)" stop-opacity=".7" />
                <stop offset="1" stop-color="var(--atlas-color)" stop-opacity="0" />
              </radialGradient>
            </defs>
            <ellipse cx="360" cy="280" rx="330" ry="255" :fill="paint('haze')" />
            <g class="atlas-graticule" stroke="#90a7bd" stroke-opacity=".1" stroke-width=".6">
              <circle cx="360" cy="280" r="228" />
              <circle cx="360" cy="280" r="175" stroke-dasharray="1 8" />
              <circle cx="360" cy="280" r="75" />
              <ellipse cx="360" cy="280" rx="300" ry="100" transform="rotate(-31 360 280)" />
              <path d="M36 280h648M360 25v510M73 48l574 464M73 512 647 48" stroke-dasharray="2 8" />
              <path d="M40 80V55h25M655 505h25v-25M348 26h24M348 534h24" stroke-opacity=".5" />
            </g>
            <g fill="#b1c8dc">
              <circle
                v-for="(star, index) in stars"
                :key="index"
                :class="{ 'atlas-twinkle': index % 11 === 0 }"
                :cx="star.x"
                :cy="star.y"
                :r="star.r"
                :opacity="star.opacity"
                :style="{ animationDelay: `${(index % 7) * -0.8}s` }"
              />
            </g>
            <g
              v-for="(region, index) in regions"
              :key="region.id"
              class="atlas-constellation"
              :class="{ 'is-active': activeIndex === index }"
              :style="{ '--region-color': region.color }"
            >
              <path
                class="constellation-shadow"
                :d="region.path"
                :stroke="region.color"
                stroke-width="7"
                stroke-opacity=".07"
              />
              <path
                class="constellation-line"
                :d="region.path"
                :stroke="region.color"
                stroke-width=".9"
              />
              <circle
                v-for="(point, pointIndex) in region.points"
                :key="pointIndex"
                :cx="point[0]"
                :cy="point[1]"
                :r="pointIndex === 1 ? 3.2 : 1.9"
                :fill="region.color"
              />
              <g v-if="activeIndex === index">
                <circle
                  :cx="region.points[1][0]"
                  :cy="region.points[1][1]"
                  r="27"
                  :fill="paint('star')"
                />
                <path
                  :d="`M${region.points[1][0] - 12} ${region.points[1][1]}h24M${region.points[1][0]} ${region.points[1][1] - 12}v24`"
                  stroke="#eff8fa"
                  stroke-width=".7"
                />
              </g>
            </g>
            <g class="atlas-compass" stroke="#b4c7d6" stroke-opacity=".4" stroke-width=".7">
              <circle cx="360" cy="280" r="20" />
              <path
                d="m360 258 4 18 18 4-18 4-4 18-4-18-18-4 18-4ZM360 247v10m0 46v10m-33-33h10m46 0h10"
              />
            </g>
            <g fill="#697d91" font-family="monospace" font-size="7" letter-spacing="2">
              <text x="316" y="325">NEBULA ATLAS</text>
              <text x="350" y="16">N</text>
              <text x="350" y="554">S</text>
              <text x="12" y="283">W</text>
              <text x="698" y="283">E</text>
              <text x="48" y="43">A MAP OF CURIOSITY</text>
              <text x="535" y="523">FIND YOUR DIRECTION</text>
            </g>
          </svg>
          <button
            v-for="(region, index) in regions"
            :key="region.id"
            type="button"
            class="atlas-node"
            :class="{
              'is-active': activeIndex === index,
              'label-left': index === 1 || index === 3,
            }"
            :style="{ left: `${region.x}%`, top: `${region.y}%`, '--region-color': region.color }"
            :aria-label="`点亮${region.name}星座`"
            :aria-pressed="activeIndex === index"
            @click="activeIndex = index"
          >
            <span class="node-signal" /><span class="atlas-node-label"
              ><small>{{ region.code }} / {{ region.english }}</small
              ><span>{{ region.name }}</span></span
            >
          </button>
          <p class="atlas-map-hint">
            点击星点，连接一个新的方向 <span aria-hidden="true">＋</span>
          </p>
        </div>
      </div>

      <div class="atlas-observation" aria-live="polite">
        <span class="observation-mark" aria-hidden="true">✧</span>
        <p>
          <span>{{ activeRegion.name }}</span
          >{{ activeRegion.description }}
        </p>
        <span class="observation-note">{{ activeRegion.note }}</span>
      </div>
      <div class="atlas-routes" role="group" aria-label="选择分类示例领域">
        <button
          v-for="(region, index) in regions"
          :key="region.id"
          type="button"
          class="atlas-route"
          :class="{ 'is-active': activeIndex === index }"
          :style="{ '--region-color': region.color }"
          :aria-pressed="activeIndex === index"
          @click="activeIndex = index"
        >
          <span class="atlas-route-code"
            >{{ region.code }} / {{ region.english }}<span aria-hidden="true">↗</span></span
          ><span class="atlas-route-name">{{ region.name }}</span
          ><span class="atlas-route-tags">{{ region.tags.join(' · ') }}</span>
        </button>
      </div>
      <footer class="atlas-footer">
        <span>愿每次出发，都有星光相伴。</span
        ><span>EVERY JOURNEY BEGINS WITH CURIOSITY <i aria-hidden="true">↗</i></span>
      </footer>
    </div>
  </section>
</template>

<style scoped>
.atlas-section {
  --atlas-color: #a2ced9;
  --atlas-rgb: 162, 206, 217;
  position: relative;
  isolation: isolate;
  overflow: hidden;
  padding: 52px 64px 30px;
  background: radial-gradient(ellipse at 76% 38%, #13233366, transparent 60%), #080e19;
  color: #dce3e7;
}
.atlas-section::before {
  position: absolute;
  z-index: -1;
  content: '';
  inset: 0;
  background: linear-gradient(#070b17, transparent 16%, transparent 85%, #070b17);
  pointer-events: none;
}
.atlas-inner {
  max-width: 1312px;
  margin: auto;
}
.atlas-header,
.atlas-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}
.atlas-coordinate {
  display: flex;
  align-items: center;
  gap: 15px;
  font-size: 10px;
  color: #8b9fae;
  letter-spacing: 0.2em;
}
.atlas-coordinate > span {
  color: #b7cbd7;
  font-size: 24px;
}
.atlas-coordinate i {
  color: #586b7d;
  font-style: normal;
  margin: 0 6px;
}
.atlas-motion {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #9cabba;
  border: 1px solid #bbd1e324;
  border-radius: 30px;
  background: #bbd1e304;
  padding: 10px 14px;
  font: inherit;
  font-size: 10px;
  cursor: pointer;
  white-space: nowrap;
}
.atlas-motion > span {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #a2ced9;
  box-shadow: 0 0 9px #a2ced988;
}
.atlas-motion .is-paused {
  background: #687d91;
  box-shadow: none;
}
.atlas-motion svg {
  width: 14px;
  height: 14px;
  margin-left: 7px;
}
.atlas-motion:disabled {
  cursor: default;
  opacity: 0.65;
}
.atlas-layout {
  display: grid;
  grid-template-columns: 0.9fr 1.25fr;
  align-items: center;
  min-height: 580px;
  gap: 35px;
}
.atlas-copy {
  padding: 35px 0;
  position: relative;
  z-index: 1;
}
.atlas-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #a6b4c0;
  font-size: 11px;
  letter-spacing: 0.2em;
}
.atlas-eyebrow > span {
  width: 18px;
  height: 1px;
  background: #a1bfcd;
}
.atlas-copy h2 {
  margin: 24px 0;
  color: #e0e5e5;
  font:
    400 clamp(38px, 4.1vw, 60px)/1.43 'Noto Serif SC',
    'Songti SC',
    'SimSun',
    Georgia,
    serif;
  letter-spacing: 0.06em;
}
.atlas-copy em {
  color: #b0cbd6;
  font-style: normal;
}
.atlas-description {
  color: #8e9eac;
  font-size: 13px;
  line-height: 2.2;
  letter-spacing: 0.06em;
}
.atlas-explore {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 36px;
  min-width: 220px;
  margin-top: 30px;
  padding: 15px 0;
  border-bottom: 1px solid #acc7dc88;
  color: #c1dae5;
  font-size: 13px;
  letter-spacing: 0.16em;
  transition:
    color 0.2s,
    border-color 0.2s;
}
.atlas-explore svg {
  width: 21px;
  height: 21px;
  transition: transform 0.2s;
}
.atlas-explore:hover {
  color: #f1f8ff;
  border-color: #d5e9fb;
}
.atlas-explore:hover svg {
  transform: translateX(5px);
}
.atlas-footnote {
  font-size: 9px;
  letter-spacing: 0.09em;
  color: #637887;
  margin-top: 15px;
}
.atlas-observatory {
  position: relative;
  min-width: 0;
  aspect-ratio: 720/560;
  margin: 0 -10px 25px;
}
.atlas-chart {
  width: 100%;
  height: 100%;
  display: block;
  overflow: visible;
}
.atlas-constellation {
  opacity: 0.28;
  transition: opacity 0.5s;
}
.atlas-constellation.is-active {
  opacity: 1;
}
.is-active .constellation-line {
  animation: constellation-reveal 1.6s ease both;
  stroke-dasharray: 1000;
}
.atlas-twinkle {
  animation: atlas-shimmer 5s ease-in-out infinite alternate;
}
.atlas-node {
  position: absolute;
  transform: translate(-50%, -50%);
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: none;
  color: #a5b6c3;
  cursor: pointer;
}
.node-signal {
  position: absolute;
  inset: 5px;
  border: 1px solid #b1c9da33;
  border-radius: 50%;
  transition:
    border-color 0.3s,
    box-shadow 0.3s;
}
.atlas-node:hover .node-signal,
.atlas-node.is-active .node-signal {
  border-color: var(--region-color);
  box-shadow:
    0 0 0 5px #b1c9da08,
    0 0 20px #b1c9da22;
}
.atlas-node-label {
  position: absolute;
  left: 31px;
  top: -24px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 130px;
  text-align: left;
  text-shadow: 0 1px 8px #080e19;
}
.atlas-node-label small {
  color: #657f93;
  font-size: 7px;
  letter-spacing: 0.12em;
  white-space: nowrap;
}
.atlas-node-label > span {
  color: #8199ac;
  font-size: 12px;
  letter-spacing: 0.13em;
  white-space: nowrap;
}
.atlas-node.is-active .atlas-node-label > span,
.atlas-node:hover .atlas-node-label > span {
  color: #e1edf3;
}
.atlas-node.is-active small {
  color: var(--region-color);
}
.label-left .atlas-node-label {
  right: 31px;
  left: auto;
  text-align: right;
}
.atlas-map-hint {
  position: absolute;
  bottom: -10px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 9px;
  letter-spacing: 0.1em;
  color: #687e91;
  white-space: nowrap;
}
.atlas-map-hint span {
  color: #91a5b5;
  margin-left: 12px;
}
.atlas-observation {
  display: flex;
  align-items: center;
  gap: 17px;
  padding: 23px 0;
  border-top: 1px solid #b5c9db1c;
  border-bottom: 1px solid #b5c9db1c;
}
.observation-mark {
  color: var(--atlas-color);
  font-size: 27px;
}
.atlas-observation p {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #8296a9;
  font-size: 11px;
  line-height: 1.7;
}
.atlas-observation p > span {
  color: var(--atlas-color);
  font-size: 12px;
  letter-spacing: 0.14em;
}
.observation-note {
  color: #647c8f;
  margin-left: auto;
  padding-left: 20px;
  font:
    12px 'Songti SC',
    'SimSun',
    Georgia,
    serif;
  letter-spacing: 0.08em;
  white-space: nowrap;
}
.atlas-routes {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  padding-top: 25px;
  gap: 20px;
}
.atlas-route {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 14px;
  padding: 17px 18px;
  border: 1px solid transparent;
  border-top-color: #b5c9db22;
  border-radius: 2px;
  color: #a1b3c3;
  background: none;
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    background 0.3s,
    border-color 0.3s;
}
.atlas-route-code {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #687e92;
  font-size: 8px;
  letter-spacing: 0.16em;
}
.atlas-route-code > span {
  color: #7d96a9;
  font-size: 15px;
}
.atlas-route-name {
  color: #aebfcc;
  font:
    20px 'Songti SC',
    'SimSun',
    Georgia,
    serif;
  letter-spacing: 0.1em;
}
.atlas-route-tags {
  color: #6d8497;
  font-size: 10px;
  line-height: 1.8;
}
.atlas-route:hover,
.atlas-route.is-active {
  border-top-color: var(--region-color);
  background: linear-gradient(#b6d1df0a, transparent);
}
.atlas-route.is-active .atlas-route-code,
.atlas-route.is-active .atlas-route-name {
  color: var(--region-color);
}
.atlas-footer {
  padding-top: 30px;
  font-size: 10px;
  letter-spacing: 0.1em;
  color: #61778b;
}
.atlas-footer > span:last-child {
  font-size: 8px;
  letter-spacing: 0.15em;
}
.atlas-footer i {
  color: #97b3c4;
  font-size: 15px;
  margin-left: 20px;
  font-style: normal;
}
button:focus-visible,
a:focus-visible {
  outline: 1px solid var(--atlas-color);
  outline-offset: 5px;
}
.is-still .atlas-chart * {
  animation-play-state: paused !important;
}
.is-still .constellation-line {
  animation: none;
  stroke-dashoffset: 0;
}
@keyframes constellation-reveal {
  from {
    stroke-dashoffset: 1000;
  }
  to {
    stroke-dashoffset: 0;
  }
}
@keyframes atlas-shimmer {
  from {
    opacity: 0.15;
  }
  to {
    opacity: 0.65;
  }
}
@media (min-width: 1600px) {
  .atlas-layout {
    min-height: 650px;
  }
}
@media (max-width: 1050px) {
  .atlas-section {
    padding: 40px 32px 30px;
  }
  .atlas-layout {
    gap: 15px;
    min-height: 500px;
  }
  .atlas-copy h2 {
    font-size: 42px;
  }
  .atlas-routes {
    gap: 12px;
  }
  .atlas-route {
    padding: 15px 12px;
  }
  .atlas-route-name {
    font-size: 18px;
  }
  .atlas-route-code {
    letter-spacing: 0.05em;
    font-size: 7px;
  }
  .observation-note {
    display: none;
  }
  .atlas-node-label {
    left: 26px;
    min-width: 110px;
  }
  .label-left .atlas-node-label {
    right: 26px;
    left: auto;
  }
  .atlas-node-label > span {
    font-size: 11px;
  }
  .atlas-node-label small {
    font-size: 6px;
  }
}
@media (max-width: 700px) {
  .atlas-section {
    padding: 32px 24px 24px;
  }
  .atlas-header {
    gap: 12px;
  }
  .atlas-coordinate {
    gap: 8px;
    font-size: 7px;
    letter-spacing: 0.1em;
  }
  .atlas-coordinate > span {
    font-size: 20px;
  }
  .atlas-coordinate i {
    margin: 0;
  }
  .atlas-motion {
    font-size: 9px;
    padding: 8px 10px;
    gap: 6px;
  }
  .atlas-motion svg {
    width: 12px;
    margin: 0;
  }
  .atlas-layout {
    display: block;
    min-height: auto;
  }
  .atlas-copy {
    padding: 42px 0 10px;
  }
  .atlas-copy h2 {
    font-size: clamp(35px, 9vw, 48px);
    margin: 20px 0 16px;
  }
  .atlas-eyebrow {
    font-size: 10px;
  }
  .atlas-description {
    font-size: 12px;
    line-height: 2;
  }
  .atlas-explore {
    min-width: 195px;
    margin-top: 15px;
    font-size: 12px;
    padding: 13px 0;
  }
  .atlas-footnote {
    display: none;
  }
  .atlas-observatory {
    margin: 10px -18px 40px;
  }
  .atlas-node-label {
    top: -29px;
    left: 17px;
    min-width: 87px;
    gap: 4px;
  }
  .label-left .atlas-node-label {
    right: 17px;
    left: auto;
  }
  .atlas-node-label > span {
    font-size: 10px;
    letter-spacing: 0.05em;
  }
  .atlas-node-label small {
    font-size: 5px;
    letter-spacing: 0.02em;
  }
  .node-signal {
    inset: 7px;
  }
  .atlas-map-hint {
    font-size: 8px;
    bottom: -20px;
  }
  .atlas-observation {
    align-items: flex-start;
    gap: 12px;
    padding: 20px 0;
  }
  .atlas-observation p {
    font-size: 11px;
  }
  .atlas-observation p > span {
    font-size: 12px;
  }
  .observation-mark {
    font-size: 24px;
  }
  .atlas-routes {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    padding-top: 20px;
  }
  .atlas-route {
    gap: 11px;
    padding: 14px 10px;
  }
  .atlas-route-name {
    font-size: 18px;
  }
  .atlas-route-code {
    font-size: 7px;
    letter-spacing: 0.01em;
  }
  .atlas-route-tags {
    font-size: 9px;
  }
  .atlas-footer {
    font-size: 9px;
    padding-top: 25px;
  }
  .atlas-footer > span:last-child {
    font-size: 6px;
    max-width: 140px;
    line-height: 1.7;
  }
  .atlas-footer i {
    margin-left: 5px;
    font-size: 12px;
  }
}
@media (max-width: 380px) {
  .atlas-section {
    padding-left: 18px;
    padding-right: 18px;
  }
  .atlas-header {
    gap: 8px;
  }
  .atlas-coordinate {
    font-size: 6px;
    gap: 5px;
    white-space: nowrap;
  }
  .atlas-motion {
    font-size: 8px;
    padding: 7px 8px;
  }
  .atlas-coordinate > span {
    display: none;
  }
  .atlas-footer > span:last-child {
    display: none;
  }
  .atlas-route-tags {
    font-size: 8px;
  }
}
@media (prefers-reduced-motion: reduce) {
  *,
  *::before {
    animation: none !important;
    transition: none !important;
  }
}
</style>
