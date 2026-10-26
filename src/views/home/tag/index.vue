<template>
  <section
    class="stardust-page"
    :class="{
      'is-paused': paused,
      'is-searching': isSearching,
      'has-incoming': incomingTags.length,
    }"
    :style="sceneStyle"
    @pointermove="movePointer"
    @pointerleave="resetPointer"
  >
    <OrbitField :paused="paused" :pointer-x="pointer.x" :pointer-y="pointer.y" />
    <div class="scene-vignette" aria-hidden="true"></div>
    <div class="orbital-map" aria-hidden="true">
      <span class="map-coordinate coordinate-north">ORBITAL PLANE / 23.4°</span>
      <span class="map-coordinate coordinate-east">NEBULA / RING 001</span>
      <span class="orbit-axis"></span>
    </div>
    <div class="page-intro">
      <p class="eyebrow"><span></span> THE ORBITAL ATLAS <i> / </i> 星环档案</p>
      <h1>万千灵感，<br /><span>自有轨道。</span></h1>
      <p class="intro-copy">相隔万里的热爱，在此交汇。<br />沿着光的轨迹，抵达思想的另一面。</p>
      <form class="star-seeker" role="search" aria-label="标签寻星" @submit.prevent="searchTags">
        <label for="tag-seeker-input" class="seeker-caption"
          ><span class="seeker-reticle" aria-hidden="true"></span>寻星 <i>/</i> FIND A TAG</label
        >
        <div class="seeker-input-line">
          <input
            id="tag-seeker-input"
            v-model="searchDraft"
            type="search"
            maxlength="50"
            autocomplete="off"
            placeholder="输入标签名，捕捉一束微光…"
            :disabled="loading || !!error"
            @keydown.esc.prevent="clearSearch"
          />
          <button
            v-if="searchDraft || isSearching"
            type="button"
            class="seeker-clear"
            aria-label="清除标签搜索"
            @click="clearSearch"
          >
            ×
          </button>
          <button
            type="submit"
            class="seeker-submit"
            aria-label="寻找标签"
            :disabled="loading || !!error"
            title="按 Enter 寻星"
          >
            ↗
          </button>
        </div>
        <div class="seeker-response">
          <p role="status" :class="{ 'has-response': isSearching }">{{ searchStatus }}</p>
          <div
            v-if="isSearching && searchBatchCount > 1"
            class="seeker-waves"
            aria-label="切换远处的搜索结果"
          >
            <button type="button" aria-label="上一束匹配星尘" @click="turnSearchBatch(-1)">
              ←
            </button>
            <span
              >{{ searchBatch + 1 }} <i>/ {{ searchBatchCount }}</i></span
            >
            <button type="button" aria-label="下一束匹配星尘" @click="turnSearchBatch(1)">→</button>
          </div>
        </div>
      </form>
    </div>
    <div class="scene-tools">
      <span class="live-state"><i></i>{{ paused ? '静谧观测' : '星环缓行' }}</span>
      <button
        class="tool-button"
        type="button"
        :aria-label="paused ? '继续漂浮' : '暂停漂浮'"
        :title="paused ? '继续漂浮' : '暂停漂浮'"
        :aria-pressed="paused"
        @click="paused = !paused"
      >
        <svg v-if="!paused" viewBox="0 0 24 24" aria-hidden="true">
          <path d="M9 6v12M15 6v12" />
        </svg>
        <svg v-else viewBox="0 0 24 24" aria-hidden="true"><path d="m9 5 10 7-10 7Z" /></svg>
      </button>
      <button
        class="tool-button"
        type="button"
        aria-label="重新编排星图"
        title="重新编排星图"
        @click="rearrange"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path
            d="M4 8h3c4 0 6 8 10 8h3M17 13l3 3-3 3M4 16h3c1.8 0 3.2-1.7 4.5-3.6M14 9c1-1 2-1 3-1h3M17 5l3 3-3 3"
          />
        </svg>
      </button>
    </div>
    <div v-if="loading || error || !tags.length" class="scene-message" role="status">
      <span class="message-star" aria-hidden="true">✧</span>
      <p>{{ loading ? '正在寻找散落的微光…' : error || '星图尚待点亮' }}</p>
      <span v-if="!loading && !error">新的标签，将成为这里的第一粒星尘。</span>
      <button v-if="error" type="button" class="retry-button" @click="refresh">重新连接 ↗</button>
    </div>
    <TransitionGroup
      v-else
      name="summoned-dust"
      tag="div"
      class="constellation"
      aria-label="标签星图"
    >
      <div
        v-for="(tag, index) in visibleTags"
        :key="tag.id"
        class="dust-anchor"
        :style="nodeStyle(index, tag.id)"
        :class="{
          'is-active': activeTag?.id === tag.id,
          'is-match': matchedIds.has(tag.id),
          'is-incoming': incomingIds.has(tag.id),
          'is-muted': isSearching && !matchedIds.has(tag.id),
        }"
      >
        <button
          class="dust-node"
          type="button"
          :title="tag.description || tag.name"
          :aria-label="tag.name + '，' + (tag.articleCount ?? 0) + ' 篇文章'"
          @pointerenter="activeId = tag.id"
          @focus="activeId = tag.id"
          @click="goToTagArticles(tag.id)"
        >
          <span class="dust-orbit" aria-hidden="true"></span>
          <span class="dust-core" aria-hidden="true"><i></i></span>
          <span class="dust-label"
            ><small>{{ tagNumber(tag.id) }} <i>/</i></small
            >{{ tag.name }}</span
          >
          <span class="dust-count">{{ tag.articleCount ?? 0 }} 篇文章 <span>↗</span></span>
        </button>
      </div>
    </TransitionGroup>
    <div class="observation-footer">
      <div class="archive-count">
        <span class="count-number">{{ countLabel }}</span
        ><span>环绕此间的灵感<br /><small>IDEAS IN ORBIT</small></span>
      </div>
      <div class="scene-hint">
        <span class="crosshair" aria-hidden="true"></span
        ><span>移动指针，感受引力<br /><small>点击星尘 · 开始探索</small></span>
      </div>
      <div v-if="pageCount > 1" class="atlas-pagination" aria-label="切换标签星图">
        <button type="button" aria-label="上一张星图" @click="turnPage(-1)">←</button>
        <span
          >{{ String(page + 1).padStart(2, '0')
          }}<i>/ {{ String(pageCount).padStart(2, '0') }}</i></span
        >
        <button type="button" aria-label="下一张星图" @click="turnPage(1)">→</button>
      </div>
    </div>
    <div v-if="activeTag && !loading && !error" class="observation-detail" aria-live="polite">
      <span class="detail-caption"
        >正在观测 <span> / {{ activeNumber }}</span></span
      >
      <button type="button" @click="goToTagArticles(activeTag.id)">
        <span>{{ activeTag.name }}</span
        ><span aria-hidden="true">↗</span>
      </button>
      <p>{{ activeTag.description || '每一次探索，都从一点微光开始。' }}</p>
    </div>
    <span class="edge-caption" aria-hidden="true">EVERY IDEA FINDS ITS OWN ORBIT</span>
  </section>
</template>

<script setup lang="ts">
import OrbitField from './components/OrbitField.vue'
import { useTagPage } from './logic/useTagPage'
import { useTagConstellation } from './logic/useTagConstellation'
defineOptions({ name: 'TagPage' })
const { tags, loading, error, refresh, goToTagArticles } = useTagPage()
const {
  page,
  pageCount,
  paused,
  activeId,
  activeTag,
  activeNumber,
  visibleTags,
  countLabel,
  pointer,
  sceneStyle,
  nodeStyle,
  tagNumber,
  searchDraft,
  isSearching,
  matchedIds,
  incomingTags,
  incomingIds,
  searchBatch,
  searchBatchCount,
  searchStatus,
  searchTags,
  clearSearch,
  turnSearchBatch,
  movePointer,
  resetPointer,
  turnPage,
  rearrange,
} = useTagConstellation(tags)
</script>

<style scoped>
.stardust-page {
  --ink: #e8e1d2;
  position: relative;
  isolation: isolate;
  min-height: 700px;
  height: 100svh;
  overflow: hidden;
  background: #060a0e;
  color: var(--ink);
  font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;
}
button {
  font: inherit;
}
button:focus-visible {
  outline: 1px solid #d9c7ff;
  outline-offset: 7px;
}
.scene-vignette {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(90deg, #060a0e85, transparent 36%),
    linear-gradient(0deg, #060a0ef2, transparent 19%, transparent 80%, #060a0e50),
    radial-gradient(ellipse at 70% 56%, transparent 35%, #03070b70 100%);
}
.orbital-map {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.42;
}
.orbit-axis {
  position: absolute;
  width: 180px;
  height: 1px;
  top: 55%;
  left: 73%;
  background: linear-gradient(90deg, #c3b38f40, transparent);
  transform: rotate(-90deg);
  transform-origin: left;
}
.map-coordinate {
  position: absolute;
  color: #b4ad9c;
  font: 9px/1 monospace;
  letter-spacing: 0.22em;
  opacity: 0.65;
}
.coordinate-north {
  top: 16%;
  left: 47%;
}
.coordinate-east {
  top: 55%;
  left: 74%;
  font-size: 7px;
  opacity: 0.8;
}
.page-intro {
  position: absolute;
  z-index: 8;
  top: 18%;
  left: 6%;
  pointer-events: none;
}
.eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #aca38e;
  font-size: 9px;
  letter-spacing: 0.21em;
}
.eyebrow > span {
  width: 16px;
  height: 1px;
  background: #d4b77c;
  box-shadow: 0 0 10px #c3a57380;
}
.eyebrow i {
  font-style: normal;
  opacity: 0.4;
  margin: 0 4px;
}
h1 {
  margin: 29px 0 23px;
  font:
    400 clamp(43px, 4.4vw, 72px)/1.4 'Noto Serif SC',
    'Songti SC',
    'SimSun',
    serif;
  letter-spacing: 0.12em;
  color: #eee8d9;
}
h1 > span {
  color: #bdad89;
}
.intro-copy {
  color: #858b89;
  font-size: 12px;
  line-height: 2.2;
  letter-spacing: 0.12em;
}
.star-seeker {
  width: 278px;
  margin-top: 30px;
  pointer-events: auto;
}
.seeker-caption {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #a0967e;
  font:
    8px/1.5 'Segoe UI',
    sans-serif;
  letter-spacing: 0.2em;
}
.seeker-caption i {
  font-style: normal;
  color: #51445f;
}
.seeker-reticle {
  position: relative;
  width: 11px;
  height: 11px;
  border: 1px solid #b6a37550;
  border-radius: 50%;
}
.seeker-reticle::after {
  content: '';
  position: absolute;
  inset: 3px;
  border-radius: 50%;
  background: #d8c391;
  box-shadow: 0 0 8px #cfb67b;
}
.seeker-input-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 9px;
  padding-bottom: 8px;
  border-bottom: 1px solid #baaa844a;
  background: linear-gradient(0deg, #baa2760a, transparent);
  transition:
    border-color 0.5s,
    box-shadow 0.5s;
}
.seeker-input-line:focus-within {
  border-bottom-color: #e6cea0ab;
  box-shadow: 0 9px 22px -13px #c6ab769c;
}
.seeker-input-line input {
  min-width: 0;
  width: 100%;
  padding: 4px 0;
  outline: none;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: #e5d8bc;
  font:
    12px/1.8 'Segoe UI',
    'Microsoft YaHei',
    sans-serif;
  letter-spacing: 0.05em;
}
.seeker-input-line input::placeholder {
  color: #7f827a;
  font-size: 10px;
}
.seeker-input-line input::-webkit-search-cancel-button {
  -webkit-appearance: none;
}
.seeker-input-line button,
.seeker-waves button {
  flex-shrink: 0;
  padding: 3px 5px;
  border: 0;
  background: none;
  color: #c8b488;
  cursor: pointer;
  transition:
    color 0.3s,
    text-shadow 0.3s;
}
.seeker-input-line button:hover,
.seeker-waves button:hover {
  color: #ffedda;
  text-shadow: 0 0 14px #e6c992;
}
.seeker-input-line button:disabled {
  opacity: 0.3;
  cursor: default;
}
.seeker-submit {
  font-size: 21px;
}
.seeker-clear {
  font-size: 17px;
  opacity: 0.65;
}
.seeker-response {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 12px;
  min-height: 26px;
  margin-top: 4px;
}
.seeker-response p {
  color: #62566f;
  font-size: 8px;
  letter-spacing: 0.06em;
  line-height: 1.8;
}
.seeker-response p.has-response {
  color: #c7b994;
}
.seeker-waves {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #c6b4d3;
  font: 9px/1 monospace;
}
.seeker-waves i {
  color: #766184;
  font-style: normal;
}
.star-seeker::after {
  content: '';
  display: block;
  width: 24px;
  height: 1px;
  margin-top: 10px;
  background: #a78ab22b;
}
.scene-tools {
  position: absolute;
  top: 15%;
  right: 5.5%;
  z-index: 3;
  display: flex;
  align-items: center;
  gap: 10px;
}
.live-state {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 12px;
  color: #a09aaf;
  letter-spacing: 0.13em;
  font-size: 10px;
}
.live-state i {
  width: 4px;
  height: 4px;
  background: #dac18f;
  border-radius: 50%;
  box-shadow: 0 0 10px #dac18f;
}
.tool-button {
  display: grid;
  place-items: center;
  width: 33px;
  height: 33px;
  border: 1px solid #a99ac524;
  border-radius: 50%;
  background: #14111f44;
  color: #aaa0bd;
  cursor: pointer;
  transition: 0.3s;
}
.tool-button:hover {
  color: #ece2ff;
  border-color: #b4a1d76e;
  background: #b6a0dd14;
}
.tool-button svg {
  width: 13px;
  height: 13px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.4;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.constellation {
  position: absolute;
  inset: 0;
}
.dust-anchor {
  position: absolute;
  width: max-content;
  z-index: 2;
  left: var(--x);
  top: var(--y);
  transform: translate(
    calc(-50% + var(--pointer-x, 0px) * var(--depth)),
    calc(-50% + var(--pointer-y, 0px) * var(--depth))
  );
  transition:
    left 1.8s cubic-bezier(0.2, 0.7, 0.2, 1),
    top 1.8s cubic-bezier(0.2, 0.7, 0.2, 1),
    transform 0.8s ease-out,
    opacity 0.8s ease;
}
.dust-anchor.is-muted {
  opacity: 0.25;
}
.dust-anchor.is-muted:hover {
  opacity: 0.65;
}
.dust-anchor.is-match {
  z-index: 6;
}
.constellation .is-match .dust-label {
  color: #fff1d3;
  text-shadow:
    0 0 18px #e2cb9b9c,
    0 0 40px #f3c6ab50;
}
.constellation .is-match .dust-core {
  background: #fff9eb;
  box-shadow:
    0 0 8px 3px #fff0dc,
    0 0 30px 12px #d5bf8e8c,
    0 0 85px 25px #e4c58f45;
}
.constellation .is-match .dust-core::before {
  width: 90px;
  left: -42px;
  opacity: 1;
}
.constellation .is-match .dust-core i {
  inset: -60px;
  background: radial-gradient(circle, #ead4a148, #d8c0980a 48%, transparent 70%);
}
.constellation .is-match .dust-orbit {
  opacity: 1;
  transform: scale(1.2);
  border-color: #e7d3a545;
}
.constellation .is-match .dust-count {
  color: #deceaab8;
}
.summoned-dust-enter-active {
  animation: stardust-arrive 1.65s cubic-bezier(0.12, 0.7, 0.2, 1) both;
}
.summoned-dust-leave-active {
  transition:
    opacity 0.4s,
    filter 0.4s;
  pointer-events: none;
}
.summoned-dust-leave-to {
  opacity: 0;
  filter: blur(5px);
}
@keyframes stardust-arrive {
  from {
    opacity: 0;
    translate: var(--flight-x) var(--flight-y);
    scale: 0.15;
    filter: blur(7px);
  }
  45% {
    opacity: 0.9;
    filter: blur(1px);
  }
  to {
    opacity: 1;
    translate: 0 0;
    scale: 1;
    filter: blur(0);
  }
}
.dust-anchor.is-active {
  z-index: 4;
}
.dust-node {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  border: 0;
  background: none;
  color: rgb(var(--node-color));
  cursor: pointer;
  padding: 32px 20px 16px;
  min-width: 100px;
  animation: stardust-float var(--duration) ease-in-out var(--delay) infinite alternate;
}
.dust-core {
  position: relative;
  display: block;
  width: 5px;
  height: 5px;
  margin-bottom: 18px;
  border-radius: 50%;
  background: #fff6eb;
  box-shadow:
    0 0 5px 1px #fff0dc,
    0 0 16px 4px rgba(var(--node-color), 0.5),
    0 0 48px 10px rgba(var(--node-color), 0.16);
  transition: 0.7s;
}
.dust-core::before,
.dust-core::after {
  content: '';
  position: absolute;
  background: linear-gradient(90deg, transparent, rgba(var(--node-color), 0.9), transparent);
  width: 52px;
  height: 1px;
  top: 2px;
  left: -24px;
  opacity: 0.8;
}
.dust-core::after {
  transform: rotate(90deg);
  width: 32px;
  left: -14px;
  opacity: 0.5;
}
.dust-core i {
  position: absolute;
  inset: -40px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(var(--node-color), 0.14), transparent 65%);
}
.dust-label {
  display: block;
  max-width: 240px;
  font:
    400 var(--node-size)/1.4 'Noto Serif SC',
    'Songti SC',
    'SimSun',
    serif;
  text-shadow: 0 0 22px rgba(var(--node-color), 0.35);
  letter-spacing: 0.04em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: center;
}
.dust-label small {
  margin-right: 7px;
  font: 8px/1.4 monospace;
  color: rgba(var(--node-color), 0.42);
  letter-spacing: 0.03em;
}
.dust-label small i {
  margin-left: 3px;
  font-style: normal;
  opacity: 0.4;
}
.dust-count {
  margin-top: 8px;
  color: rgba(var(--node-color), 0.38);
  font-size: 9px;
  letter-spacing: 0.12em;
  transition: 0.4s;
}
.dust-count > span {
  display: inline-block;
  margin-left: 8px;
  opacity: 0;
  transition: 0.4s;
}
.dust-orbit {
  position: absolute;
  width: 112px;
  height: 112px;
  top: -21px;
  border: 1px solid rgba(var(--node-color), 0.06);
  border-radius: 50%;
  transform: scale(0.7);
  opacity: 0;
  transition: 0.8s;
}
.dust-orbit::after {
  content: '';
  position: absolute;
  inset: 10px;
  border: 1px dashed rgba(var(--node-color), 0.13);
  border-radius: 50%;
}
.is-active .dust-core,
.dust-node:hover .dust-core {
  box-shadow:
    0 0 8px 2px #fff0dc,
    0 0 32px 10px rgba(var(--node-color), 0.35),
    0 0 90px 25px rgba(var(--node-color), 0.1);
}
.is-active .dust-orbit,
.dust-node:hover .dust-orbit {
  opacity: 1;
  transform: scale(1);
}
.is-active .dust-label,
.dust-node:hover .dust-label {
  color: #f5e7cd;
}
.is-active .dust-count,
.dust-node:hover .dust-count {
  color: rgba(var(--node-color), 0.8);
}
.is-active .dust-count > span,
.dust-node:hover .dust-count > span {
  opacity: 1;
  transform: translate(2px, -2px);
}
.observation-footer {
  position: absolute;
  z-index: 3;
  bottom: 6.5%;
  left: 6%;
  display: flex;
  align-items: center;
  gap: 60px;
}
.archive-count {
  display: flex;
  align-items: center;
  gap: 16px;
}
.count-number {
  font:
    300 39px/1 'Segoe UI',
    sans-serif;
  color: #c7b99b;
  letter-spacing: -0.06em;
}
.archive-count > span:last-child {
  color: #8a8299;
  font-size: 10px;
  line-height: 1.8;
  letter-spacing: 0.12em;
}
.archive-count small {
  color: #4f4a5f;
  font: 7px/1.8 monospace;
  letter-spacing: 0.15em;
}
.scene-hint {
  display: flex;
  align-items: center;
  gap: 13px;
  color: #696278;
  font-size: 9px;
  line-height: 1.8;
  letter-spacing: 0.08em;
}
.scene-hint small {
  color: #494354;
  font-size: 9px;
}
.crosshair {
  position: relative;
  width: 24px;
  height: 24px;
  border: 1px solid #8b7eac20;
  border-radius: 50%;
}
.crosshair::before,
.crosshair::after {
  content: '';
  position: absolute;
  width: 11px;
  height: 1px;
  top: 10px;
  left: 5px;
  background: #8b7eac80;
}
.crosshair::after {
  transform: rotate(90deg);
}
.atlas-pagination {
  display: flex;
  align-items: center;
  gap: 15px;
  font: 10px/1 monospace;
  color: #c1b7d2;
}
.atlas-pagination i {
  font-style: normal;
  color: #615970;
  margin-left: 7px;
}
.atlas-pagination button {
  border: 0;
  color: #b3a4cc;
  background: none;
  cursor: pointer;
  padding: 8px;
}
.observation-detail {
  position: absolute;
  z-index: 5;
  bottom: 6.5%;
  right: 6%;
  width: 240px;
  border-top: 1px solid #a49ab62b;
  padding-top: 15px;
}
.detail-caption {
  color: #949387;
  font-size: 8px;
  letter-spacing: 0.2em;
}
.detail-caption > span {
  color: #5c546c;
}
.observation-detail button {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  width: 100%;
  margin: 12px 0 8px;
  padding: 0;
  border: 0;
  background: none;
  color: #cebf9f;
  cursor: pointer;
  text-align: left;
}
.observation-detail button > span:first-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font:
    18px/1.5 'Noto Serif SC',
    'Songti SC',
    'SimSun',
    serif;
}
.observation-detail button > span:last-child {
  font: 22px/1 sans-serif;
  color: #8c7b9d;
}
.observation-detail p {
  color: #727873;
  font-size: 10px;
  line-height: 1.8;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.edge-caption {
  position: absolute;
  top: 51%;
  left: 19px;
  transform: rotate(-90deg) translateX(-50%);
  transform-origin: left top;
  color: #484053;
  font: 7px/1 monospace;
  letter-spacing: 0.2em;
}
.scene-message {
  position: absolute;
  z-index: 4;
  left: 61%;
  top: 49%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #b1a3c4;
  font-size: 13px;
  line-height: 2;
  letter-spacing: 0.1em;
}
.message-star {
  display: block;
  margin-bottom: 16px;
  color: #d4bddb;
  font-size: 48px;
  text-shadow: 0 0 40px #d4bddb80;
}
.scene-message > span:last-of-type:not(.message-star) {
  color: #776a87;
  font-size: 10px;
}
.retry-button {
  border: 1px solid #a395b948;
  border-radius: 0;
  padding: 6px 16px;
  margin-top: 15px;
  background: #b09bcd0a;
  color: #d0bfdf;
  cursor: pointer;
}
.is-paused .dust-node {
  animation-play-state: paused;
}
@keyframes stardust-float {
  from {
    transform: translateY(0);
  }
  to {
    transform: translate(calc(var(--drift) * 0.65), calc(var(--drift) * -0.4));
  }
}
@media (min-width: 1800px) {
  .page-intro {
    left: 8%;
  }
  .scene-tools {
    right: 8%;
  }
}
@media (max-width: 1050px) {
  .page-intro {
    top: 18%;
    left: 5%;
  }
  h1 {
    font-size: 47px;
  }
  .intro-copy {
    font-size: 10px;
  }
  .star-seeker {
    width: 180px;
    margin-top: 20px;
  }
  .dust-label {
    font-size: calc(var(--node-size) * 0.85);
  }
  .scene-hint {
    display: none;
  }
  .observation-detail {
    width: 210px;
  }
}
@media (max-width: 650px) {
  .stardust-page {
    min-height: 980px;
    height: 100svh;
  }
  .stardust-page.has-incoming {
    min-height: 1180px;
  }
  .page-intro {
    top: 17%;
    left: 8%;
  }
  .eyebrow {
    font-size: 7px;
    gap: 7px;
    letter-spacing: 0.14em;
  }
  h1 {
    font-size: 40px;
    margin: 20px 0 14px;
    line-height: 1.35;
  }
  .intro-copy {
    font-size: 10px;
  }
  .scene-tools {
    top: 45%;
    right: 7%;
  }
  .live-state {
    font-size: 8px;
    margin-right: 2px;
  }
  .constellation {
    top: 48%;
    bottom: 23%;
  }
  .dust-anchor {
    left: var(--compact-x);
    top: var(--compact-y);
  }
  .dust-label {
    font-size: clamp(13px, calc(var(--node-size) * 0.62), 21px);
    max-width: 120px;
  }
  .star-seeker {
    width: min(278px, 75vw);
    margin-top: 21px;
  }
  .dust-label small {
    font-size: 6px;
    margin-right: 4px;
  }
  .dust-node {
    min-width: 65px;
    padding: 25px 9px 12px;
  }
  .dust-core {
    width: 3px;
    height: 3px;
    margin-bottom: 12px;
  }
  .dust-core::before {
    width: 32px;
    left: -14px;
    top: 1px;
  }
  .dust-core::after {
    width: 20px;
    left: -8px;
    top: 1px;
  }
  .dust-count {
    font-size: 7px;
    margin-top: 5px;
    letter-spacing: 0;
  }
  .dust-orbit {
    width: 80px;
    height: 80px;
    top: -10px;
  }
  .observation-footer {
    left: 8%;
    bottom: 5%;
    gap: 20px;
  }
  .count-number {
    font-size: 29px;
  }
  .archive-count {
    gap: 10px;
  }
  .archive-count > span:last-child {
    font-size: 8px;
  }
  .archive-count small {
    font-size: 6px;
  }
  .observation-detail {
    bottom: 12%;
    left: 8%;
    right: 8%;
    width: auto;
    max-width: 270px;
    background: linear-gradient(180deg, #060a0ed9, #060a0e00);
  }
  .observation-detail button {
    font-size: 17px;
    margin-top: 8px;
  }
  .observation-detail p {
    font-size: 9px;
  }
  .edge-caption {
    display: none;
  }
  .scene-message {
    left: 50%;
    top: 57%;
    width: 80%;
  }
  .coordinate-north,
  .coordinate-east,
  .orbit-axis {
    display: none;
  }
}
@media (min-width: 651px) and (max-height: 760px) {
  h1 {
    font-size: 45px;
    margin: 20px 0 15px;
  }
  .star-seeker {
    margin-top: 20px;
    width: min(250px, 21.5vw);
  }
  .observation-footer,
  .observation-detail {
    bottom: 4%;
  }
  .count-number {
    font-size: 30px;
  }
  .observation-detail {
    width: 210px;
  }
  .observation-detail p {
    font-size: 9px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .dust-node {
    animation: none;
  }
  .dust-anchor {
    transition: none;
    transform: translate(-50%, -50%);
  }
  .summoned-dust-enter-active,
  .summoned-dust-leave-active {
    animation: none;
    transition: none;
  }
}
</style>
