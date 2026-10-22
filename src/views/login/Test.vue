<script setup lang="ts">
import { computed, ref } from 'vue'
import CloudVariants from './components/CloudVariants.vue'

defineOptions({ name: 'CloudStudyPage' })

const isRaining = ref(false)
const isPaused = ref(false)
const cloudStyles = [
  {
    id: 'cartoon',
    name: '软萌卡通',
    tag: 'CARTOON',
    title: '今天，也轻一点。',
    description: '什么都不用赶，和这朵云一起发会儿呆。',
  },
  {
    id: 'watercolor',
    name: '轻柔水彩',
    tag: 'WATERCOLOR',
    title: '让心情，慢慢晕开。',
    description: '水色、纸纹，和一朵没有边界的云。',
  },
  {
    id: 'paper',
    name: '层叠剪纸',
    tag: 'PAPER CUT',
    title: '剪一片，属于你的天。',
    description: '让光落在纸边，把天空叠出层次。',
  },
  {
    id: 'pixel',
    name: '复古像素',
    tag: 'PIXEL ART',
    title: '暂存今天的好心情。',
    description: '按下暂停键，在八比特的天空里歇一会儿。',
  },
  {
    id: 'ink',
    name: '东方水墨',
    tag: 'INK WASH',
    title: '云无心，以出岫。',
    description: '远山淡去，一笔留白，半日闲。',
  },
  {
    id: 'natural',
    name: '日落写意',
    tag: 'GOLDEN HOUR',
    title: '云边，藏着落日。',
    description: '一团柔软的光，停在暮色与晚风之间。',
  },
] as const
const activeStyle = ref<(typeof cloudStyles)[number]['id']>('watercolor')
const currentStyle = computed(() => cloudStyles.find((style) => style.id === activeStyle.value)!)
const currentNumber = computed(() =>
  String(cloudStyles.findIndex((style) => style.id === activeStyle.value) + 1).padStart(3, '0'),
)
const sunRays = Array.from({ length: 8 }, (_, index) => index * 45)
const raindrops = Array.from({ length: 9 }, (_, index) => ({
  x: 248 + index * 36,
  delay: `${index * -0.23}s`,
  duration: `${1.35 + (index % 3) * 0.2}s`,
}))
</script>

<template>
  <main
    class="cloud-page"
    :class="[{ 'is-raining': isRaining, 'is-paused': isPaused }, `theme-${activeStyle}`]"
  >
    <div class="sky-sheet">
      <header class="page-header">
        <div class="wordmark">
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path
              d="M12 2v20M2 12h20M5 5l14 14M5 19 19 5"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
            />
          </svg>
          <span>NEBULA<span class="wordmark-divider">/</span>天空放映室</span>
        </div>
        <span class="edition"
          >一朵云，六种模样 <span>{{ currentNumber }}</span></span
        >
      </header>

      <section class="cloud-study" aria-labelledby="cloud-heading">
        <div class="intro">
          <p class="eyebrow">CLOUD STUDIES / {{ currentStyle.tag }}</p>
          <h1 id="cloud-heading">{{ currentStyle.title }}</h1>
          <p class="description">{{ currentStyle.description }}</p>
        </div>

        <div class="style-picker" role="group" aria-label="选择云朵画风">
          <button
            v-for="(style, index) in cloudStyles"
            :key="style.id"
            type="button"
            class="style-option"
            :class="`swatch-${style.id}`"
            :aria-pressed="activeStyle === style.id"
            @click="activeStyle = style.id"
          >
            <span class="style-number">0{{ index + 1 }}</span>
            <svg class="style-preview" viewBox="0 0 100 56" fill="none" aria-hidden="true">
              <circle class="preview-sun" cx="77" cy="14" r="8" />
              <path
                v-if="style.id === 'pixel'"
                class="preview-cloud"
                d="M15 29h10V19h10V9h20v10h10v10h20v15H15Z"
              />
              <template v-else>
                <path
                  v-if="style.id === 'paper'"
                  class="preview-layer"
                  d="M11 42c0-9 7-14 16-13 0-14 19-20 27-9 12-5 24 5 22 13 13-1 18 14 8 16H20Z"
                />
                <path
                  class="preview-cloud"
                  d="M24 43c-17 0-18-22-2-24 1-16 24-20 31-5 12-7 25 2 25 12 18-2 20 17 4 17Z"
                />
                <path
                  v-if="style.id === 'cartoon'"
                  d="M40 28v3m20-3v3m-13 3q3 3 6 0"
                  stroke="#3e5e59"
                  stroke-width="2"
                  stroke-linecap="round"
                />
                <path
                  v-if="style.id === 'ink'"
                  d="m8 48 21-13 20 12 18-21 25 22"
                  stroke="#9b9c90"
                  stroke-width="1"
                />
              </template>
            </svg>
            <span class="style-name">{{ style.name }}</span>
          </button>
        </div>

        <figure class="illustration">
          <CloudVariants
            v-if="activeStyle !== 'cartoon'"
            :variant="activeStyle"
            :is-raining="isRaining"
            :is-paused="isPaused"
          />
          <svg
            v-else
            class="sky-art"
            viewBox="0 0 800 440"
            role="img"
            aria-labelledby="cloud-art-title cloud-art-description"
          >
            <title id="cloud-art-title">{{ isRaining ? '下着小雨的云朵' : '阳光下的云朵' }}</title>
            <desc id="cloud-art-description">
              一朵有着圆圆眼睛、粉色脸颊和微笑的奶白色卡通云，在浅蓝色天空里轻轻漂浮。
            </desc>
            <defs>
              <pattern id="cloud-study-grain" width="6" height="6" patternUnits="userSpaceOnUse">
                <circle cx="1" cy="1" r="0.65" fill="#48736b" opacity="0.1" />
              </pattern>
              <clipPath id="cloud-study-sky-clip">
                <rect x="58" y="36" width="684" height="340" rx="170" />
              </clipPath>
              <clipPath id="cloud-study-body-clip">
                <path
                  d="M246 300c-43 0-78-29-78-67 0-36 28-65 64-66 4-43 37-75 79-75 29 0 54 15 68 38 20-14 44-16 66-8 29 10 49 35 52 64 37-9 76 10 89 43 15 38-15 71-58 71Z"
                />
              </clipPath>
            </defs>

            <g clip-path="url(#cloud-study-sky-clip)">
              <rect class="sky-fill" x="58" y="36" width="684" height="340" rx="170" />
              <rect x="58" y="36" width="684" height="340" fill="url(#cloud-study-grain)" />
              <path
                d="M58 323c113-35 181 48 303 20s241-39 381 4v42H58Z"
                fill="#fffdf4"
                opacity="0.28"
              />
            </g>
            <g class="sun" stroke="#d5a54c" stroke-width="2.8" stroke-linecap="round">
              <path
                v-for="angle in sunRays"
                :key="angle"
                d="M560 63v-9"
                :transform="`rotate(${angle} 560 104)`"
              />
              <circle cx="560" cy="104" r="30" fill="#f7d987" stroke="none" />
              <path d="M547 108q4 5 8 0m10 0q4 5 8 0" stroke="#9e793d" stroke-width="2" />
            </g>
            <g class="breeze" fill="none" stroke="#93b4b6" stroke-width="2" stroke-linecap="round">
              <path d="M92 212h36c17 0 19-18 7-18-7 0-10 5-9 9M77 226h39" />
              <path d="M650 250h45c19 0 19-19 7-19-7 0-10 5-9 9M640 264h38" />
            </g>
            <g class="sparkles" fill="#e7b85b">
              <path class="sparkle" d="m176 113 3 9 9 3-9 3-3 9-3-9-9-3 9-3Z" />
              <path class="sparkle sparkle-two" d="m621 175 3 8 8 3-8 3-3 8-3-8-8-3 8-3Z" />
              <circle cx="230" cy="70" r="3" />
              <circle cx="656" cy="136" r="2.5" />
            </g>
            <ellipse class="cloud-shadow" cx="389" cy="357" rx="126" ry="9" fill="#426b70" />
            <g class="rain" stroke="#6f9eaf" stroke-width="4" stroke-linecap="round">
              <path
                v-for="(drop, index) in raindrops"
                :key="index"
                class="raindrop"
                :d="`M${drop.x} 301l-6 13`"
                :style="{ animationDelay: drop.delay, animationDuration: drop.duration }"
              />
            </g>

            <g class="floating-cloud">
              <path
                class="cloud-body"
                d="M246 300c-43 0-78-29-78-67 0-36 28-65 64-66 4-43 37-75 79-75 29 0 54 15 68 38 20-14 44-16 66-8 29 10 49 35 52 64 37-9 76 10 89 43 15 38-15 71-58 71Z"
                fill="#fffdf5"
              />
              <g clip-path="url(#cloud-study-body-clip)">
                <path
                  d="M158 260c47 45 85 17 134 20s72 15 121 4 125 17 185-32v68H158Z"
                  fill="#e5eee8"
                />
                <path
                  d="M190 223c1-21 17-35 34-37m28-18c5-29 27-49 51-50"
                  fill="none"
                  stroke="#fff"
                  stroke-width="9"
                  stroke-linecap="round"
                />
              </g>
              <path
                d="M246 300c-43 0-78-29-78-67 0-36 28-65 64-66 4-43 37-75 79-75 29 0 54 15 68 38 20-14 44-16 66-8 29 10 49 35 52 64 37-9 76 10 89 43 15 38-15 71-58 71Z"
                fill="none"
                stroke="#3e5e59"
                stroke-width="3.5"
                stroke-linejoin="round"
              />
              <ellipse cx="310" cy="245" rx="18" ry="10" fill="#f3bab0" opacity="0.75" />
              <ellipse cx="450" cy="245" rx="18" ry="10" fill="#f3bab0" opacity="0.75" />
              <g class="cloud-eyes" fill="#3e5e59">
                <ellipse cx="337" cy="224" rx="5.5" ry="8" />
                <ellipse cx="423" cy="224" rx="5.5" ry="8" />
              </g>
              <path
                d="M368 240q12 14 24 0"
                fill="none"
                stroke="#3e5e59"
                stroke-width="3"
                stroke-linecap="round"
              />
              <path
                d="m290 243-2 5m9-4-2 5m169-6-2 5m9-4-2 5"
                stroke="#d98d84"
                stroke-width="1.5"
                stroke-linecap="round"
              />
            </g>
            <g class="handwritten-note" fill="#69847c">
              <path
                d="M596 317c28-1 37-19 34-35m-6 7 6-8 6 7"
                fill="none"
                stroke="currentColor"
                stroke-width="1.4"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <text x="571" y="340" transform="rotate(-7 571 340)">正在认真发呆</text>
            </g>
          </svg>
          <figcaption class="weather-caption" aria-live="polite">
            <span class="weather-dot" />
            {{ isRaining ? '偶尔下点小雨，也没关系。' : '同一片天空，换一种心情。' }}
          </figcaption>
        </figure>

        <div class="controls">
          <div class="weather-switch" role="group" aria-label="切换云朵天气">
            <button type="button" :aria-pressed="!isRaining" @click="isRaining = false">
              <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
                <circle cx="10" cy="10" r="3.3" />
                <path
                  d="M10 1.5v2m0 13v2M1.5 10h2m13 0h2M4 4l1.5 1.5m9 9L16 16M4 16l1.5-1.5m9-9L16 4"
                />
              </svg>
              晒晒太阳
            </button>
            <button type="button" :aria-pressed="isRaining" @click="isRaining = true">
              <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
                <path
                  d="M5.5 12H5a3 3 0 0 1-.4-6 4.5 4.5 0 0 1 8.6-1A3.5 3.5 0 1 1 15 12h-.5M7 14l-1 3m4-3-1 3m4-3-1 3"
                />
              </svg>
              下点小雨
            </button>
          </div>
          <button
            class="motion-toggle"
            type="button"
            :aria-label="isPaused ? '继续动画' : '暂停动画'"
            :aria-pressed="isPaused"
            :title="isPaused ? '继续动画' : '暂停动画'"
            @click="isPaused = !isPaused"
          >
            <svg v-if="isPaused" viewBox="0 0 20 20" aria-hidden="true">
              <path d="m7 4 9 6-9 6Z" fill="currentColor" />
            </svg>
            <svg v-else viewBox="0 0 20 20" aria-hidden="true">
              <path d="M7 5v10m6-10v10" fill="none" stroke="currentColor" stroke-width="2" />
            </svg>
          </button>
        </div>
      </section>

      <footer class="page-footer">
        <span>留一点空白，给好心情。</span>
        <span class="footer-signature">MADE OF NOTHING BUT AIR <span>↗</span></span>
      </footer>
    </div>
  </main>
</template>

<style scoped>
.cloud-page {
  --ink: #3e5e59;
  --accent: #698c7c;
  min-height: 100dvh;
  padding: 22px;
  color: var(--ink);
  background: #f5f4ee;
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.sky-sheet {
  display: flex;
  flex-direction: column;
  min-height: calc(100dvh - 44px);
  padding: 28px 42px 24px;
  border: 1px solid #d8e1d8;
  border-radius: 24px;
  background: #fbfcf6;
}
.page-header,
.wordmark,
.page-footer,
.controls,
.weather-switch,
.weather-switch button {
  display: flex;
  align-items: center;
}
.page-header,
.page-footer {
  justify-content: space-between;
  gap: 20px;
}
.wordmark {
  gap: 12px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1.5px;
}
.wordmark svg {
  width: 24px;
  height: 24px;
}
.wordmark-divider {
  margin: 0 12px;
  color: #a9b6aa;
  font-weight: 400;
}
.edition {
  color: #7b8c80;
  font-size: 11px;
  letter-spacing: 1.5px;
}
.edition span {
  margin-left: 10px;
  padding: 4px 8px;
  border: 1px solid #d8e1d8;
  border-radius: 5px;
  font-variant-numeric: tabular-nums;
}
.cloud-study {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 26px 0 28px;
  text-align: center;
}
.eyebrow {
  color: #8a9b8a;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 3px;
}
h1 {
  margin: 16px 0 14px;
  font-family: 'Songti SC', 'STSong', 'SimSun', serif;
  font-size: clamp(28px, 4vw, 42px);
  font-weight: 500;
  line-height: 1.4;
  letter-spacing: 5px;
}
.description {
  color: #819084;
  font-size: 13px;
  line-height: 1.8;
  letter-spacing: 1px;
}
.illustration {
  width: min(740px, 100%);
  margin: 0 0 22px;
}
.style-picker {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
  width: min(700px, 100%);
  margin: 24px 0 10px;
}
.style-option {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  min-width: 0;
  padding: 9px 6px 10px;
  border: 1px solid #e0e5dc;
  border-radius: 10px;
  background: transparent;
  transition:
    border-color 0.2s ease,
    background 0.2s ease,
    transform 0.2s ease;
}
.style-option:hover {
  transform: translateY(-2px);
  border-color: var(--accent);
}
.style-option[aria-pressed='true'] {
  border-color: var(--accent);
  background: #edf0e7;
  box-shadow: 0 0 0 1px var(--accent);
}
.style-number {
  position: absolute;
  top: 7px;
  left: 8px;
  color: #95a28f;
  font-size: 8px;
  font-variant-numeric: tabular-nums;
}
.style-preview {
  display: block;
  width: 65px;
  height: 36px;
}
.preview-cloud {
  fill: #fffdf5;
  stroke: #5f7b70;
  stroke-width: 1.7;
}
.preview-sun {
  fill: #e8cc85;
}
.style-name {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.5px;
  white-space: nowrap;
}
.swatch-watercolor .preview-cloud {
  fill: #d5d3e9;
  stroke: #c4bfd8;
  stroke-width: 3;
  filter: blur(1.4px);
}
.swatch-watercolor .preview-sun {
  fill: #e8c5ca;
}
.swatch-paper .preview-cloud {
  fill: #fff1d2;
  stroke: #d8c9a5;
}
.preview-layer {
  fill: #8ab4ae;
}
.swatch-pixel .preview-cloud {
  fill: #dae3ef;
  stroke: #536c93;
  stroke-width: 2;
}
.swatch-pixel .preview-sun {
  fill: #e6bc74;
}
.swatch-ink .preview-cloud {
  fill: #f1f1e8;
  stroke: #939d8e;
  stroke-width: 1;
}
.swatch-ink .preview-sun {
  fill: #bc8272;
}
.swatch-natural .preview-cloud {
  fill: #ecc7b6;
  stroke: #d5a9b3;
  stroke-width: 5;
  filter: blur(1px);
}
.swatch-natural .preview-sun {
  fill: #e8bd83;
}
.theme-watercolor {
  --accent: #9995b1;
}
.theme-paper {
  --accent: #548989;
}
.theme-pixel {
  --accent: #6c7fa2;
}
.theme-ink {
  --accent: #949b87;
}
.theme-natural {
  --accent: #b89094;
}
.sky-art {
  display: block;
  width: 100%;
  overflow: visible;
}
.sky-fill {
  fill: #dfedf0;
  transition: fill 0.8s ease;
}
.floating-cloud {
  transform-origin: 380px 210px;
  animation: float 6s ease-in-out infinite;
}
.cloud-body {
  transition: fill 0.8s ease;
}
.cloud-eyes {
  transform-origin: 380px 224px;
  animation: blink 7s infinite;
}
.cloud-shadow {
  transform-origin: 389px 357px;
  opacity: 0.08;
  animation: shadow-breathe 6s ease-in-out infinite;
}
.sun {
  transform-origin: 560px 104px;
  transition:
    opacity 0.7s ease,
    transform 0.9s ease;
}
.breeze {
  animation: breeze 7s ease-in-out infinite;
}
.sparkle {
  transform-box: fill-box;
  transform-origin: center;
  animation: twinkle 4s ease-in-out infinite;
}
.sparkle-two {
  animation-delay: -2s;
}
.rain {
  opacity: 0;
  transition: opacity 0.5s ease;
}
.raindrop {
  animation: rainfall linear infinite;
  animation-play-state: paused;
}
.handwritten-note {
  color: #69847c;
  font-family: 'KaiTi', 'STKaiti', serif;
  font-size: 16px;
  letter-spacing: 2px;
}
.weather-caption {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  min-height: 22px;
  margin-top: -8px;
  color: #788c7e;
  font-size: 12px;
  letter-spacing: 1px;
}
.weather-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #b9c99a;
  transition: background 0.5s ease;
}
.controls {
  justify-content: center;
  gap: 12px;
}
.weather-switch {
  gap: 4px;
  padding: 5px;
  border: 1px solid #dce4d9;
  border-radius: 40px;
  background: #f0f3eb;
}
button {
  color: var(--ink);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}
.weather-switch button {
  justify-content: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 20px;
  border: 1px solid transparent;
  border-radius: 30px;
  background: transparent;
  color: #7c8e80;
  font-size: 12px;
  transition:
    background 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease;
}
.weather-switch button[aria-pressed='true'] {
  border-color: #e2e7dc;
  background: #fffef8;
  color: var(--ink);
  box-shadow: 0 2px 4px #3e5e5908;
}
.weather-switch button:hover {
  color: #28483f;
}
.weather-switch svg,
.motion-toggle svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  stroke: currentColor;
  stroke-width: 1.4;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.motion-toggle {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border: 1px solid #dce4d9;
  border-radius: 50%;
  background: transparent;
  color: #819084;
  transition: background 0.2s ease;
}
.motion-toggle:hover {
  background: #edf1e7;
}
button:focus-visible {
  outline: 2px solid #698c7c;
  outline-offset: 4px;
}
.page-footer {
  padding-top: 18px;
  border-top: 1px solid #e4e9df;
  color: #96a18f;
  font-size: 10px;
  line-height: 1.6;
  letter-spacing: 1.5px;
}
.footer-signature {
  font-size: 9px;
  letter-spacing: 1.8px;
}
.footer-signature span {
  margin-left: 10px;
  font-size: 14px;
}
.is-raining .sky-fill {
  fill: #dce5e9;
}
.is-raining .cloud-body {
  fill: #f5f7f2;
}
.is-raining .sun {
  opacity: 0.15;
  transform: translate(12px, 12px) scale(0.85);
}
.is-raining .rain {
  opacity: 1;
}
.is-raining .raindrop {
  animation-play-state: running;
}
.is-raining .weather-dot {
  background: #8bb0be;
}
.is-paused .sky-art * {
  animation-play-state: paused;
}
@keyframes float {
  0%,
  100% {
    transform: translateY(0) rotate(-1deg);
  }
  50% {
    transform: translateY(-12px) rotate(1deg);
  }
}
@keyframes blink {
  0%,
  43%,
  47%,
  100% {
    transform: scaleY(1);
  }
  45% {
    transform: scaleY(0.12);
  }
}
@keyframes shadow-breathe {
  0%,
  100% {
    transform: scaleX(1);
    opacity: 0.08;
  }
  50% {
    transform: scaleX(0.86);
    opacity: 0.05;
  }
}
@keyframes breeze {
  0%,
  100% {
    transform: translateX(-4px);
    opacity: 0.5;
  }
  50% {
    transform: translateX(5px);
    opacity: 0.85;
  }
}
@keyframes twinkle {
  0%,
  100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  50% {
    transform: scale(1.1);
    opacity: 1;
  }
}
@keyframes rainfall {
  0% {
    transform: translate(0, 0);
    opacity: 0;
  }
  15% {
    opacity: 0.65;
  }
  85% {
    opacity: 0.65;
  }
  100% {
    transform: translate(-18px, 64px);
    opacity: 0;
  }
}
@media (min-width: 1400px) {
  .illustration {
    width: 860px;
  }
}
@media (max-height: 900px) and (min-width: 641px) {
  .cloud-study {
    padding: 22px 0;
  }
  .illustration {
    width: min(600px, 100%);
    margin-bottom: 16px;
  }
  .style-picker {
    margin-top: 20px;
  }
}
@media (max-width: 640px) {
  .cloud-page {
    padding: 12px;
  }
  .sky-sheet {
    min-height: calc(100dvh - 24px);
    padding: 22px 14px 18px;
    border-radius: 18px;
  }
  .page-header {
    padding: 0 5px;
  }
  .wordmark {
    gap: 8px;
    font-size: 10px;
    letter-spacing: 0.5px;
  }
  .wordmark svg {
    width: 19px;
    height: 19px;
  }
  .wordmark-divider {
    margin: 0 7px;
  }
  .edition {
    font-size: 0;
  }
  .edition span {
    margin-left: 0;
    font-size: 10px;
  }
  .cloud-study {
    padding: 34px 0;
  }
  .eyebrow {
    font-size: 8px;
    letter-spacing: 2px;
  }
  h1 {
    margin: 18px 0 14px;
    font-size: clamp(26px, 6.5vw, 36px);
    letter-spacing: 3px;
  }
  .description {
    font-size: 11px;
    letter-spacing: 0;
  }
  .illustration {
    width: calc(100% + 16px);
    margin: 4px -8px 24px;
  }
  .style-picker {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
    margin: 24px 0 16px;
  }
  .style-option {
    padding: 7px 4px 9px;
  }
  .style-preview {
    height: 31px;
  }
  .weather-caption {
    margin-top: 12px;
    font-size: 11px;
  }
  .controls {
    gap: 8px;
  }
  .weather-switch button {
    padding: 0 13px;
    font-size: 11px;
  }
  .page-footer {
    justify-content: center;
    font-size: 9px;
  }
  .footer-signature {
    display: none;
  }
}
@media (prefers-reduced-motion: reduce) {
  .sky-art *,
  .cloud-page * {
    animation: none !important;
    transition: none !important;
  }
  .raindrop {
    opacity: 0.6;
    transform: translateY(22px);
  }
}
</style>
