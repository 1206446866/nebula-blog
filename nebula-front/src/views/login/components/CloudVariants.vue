<script setup lang="ts">
import { useId } from 'vue'

defineProps<{
  variant: 'watercolor' | 'paper' | 'pixel' | 'ink' | 'natural'
  isRaining: boolean
  isPaused: boolean
}>()

const uid = `cloud-art-${useId()}`
const paint = (name: string) => `url(#${uid}-${name})`
const rain = Array.from({ length: 10 }, (_, index) => ({
  x: 248 + index * 32,
  delay: `${index * -0.19}s`,
}))
const stars = [
  [154, 114],
  [221, 74],
  [607, 92],
  [658, 153],
  [128, 286],
  [677, 286],
]
</script>

<template>
  <svg
    class="variant-art"
    :class="[`art-${variant}`, { raining: isRaining, paused: isPaused }]"
    viewBox="0 0 800 440"
    role="img"
    :aria-labelledby="`${uid}-title ${uid}-description`"
  >
    <title :id="`${uid}-title`">
      {{
        {
          watercolor: '轻柔水彩云',
          paper: '层叠剪纸云',
          pixel: '复古像素云',
          ink: '东方水墨云',
          natural: '日落写意云',
        }[variant]
      }}{{ isRaining ? ' · 小雨' : ' · 晴' }}
    </title>
    <desc :id="`${uid}-description`">
      {{
        {
          watercolor: '蓝紫色水彩晕染的云，带着柔软的纸纹和淡淡的粉色。',
          paper: '奶白色云朵由多层纸张叠成，悬浮在青绿色的剪纸山谷上。',
          pixel: '由像素方块组成的白云、太阳与远山，像复古游戏中的天空。',
          ink: '墨色远山之间，留白的流云徐徐展开，旁边点缀朱红日轮与印章。',
          natural: '暮色中一朵被金色夕阳照亮的蓬松云团，下方是紫色山影。',
        }[variant]
      }}
    </desc>

    <defs>
      <clipPath :id="`${uid}-frame`">
        <rect
          x="58"
          y="36"
          width="684"
          height="340"
          :rx="variant === 'pixel' ? 0 : variant === 'ink' ? 16 : 140"
        />
      </clipPath>
      <path
        :id="`${uid}-cloud`"
        d="M228 289c-42 0-65-23-65-54 0-30 24-51 54-51 2-31 25-54 55-54 13-32 43-46 75-37 24 6 38 22 44 43 34-17 77-2 86 32 31-7 58 10 63 37 41-4 70 15 70 43 0 25-20 41-50 41Z"
      />
      <linearGradient :id="`${uid}-water-sky`" x2="0.8" y2="1">
        <stop stop-color="#e4eced" />
        <stop offset="0.5" stop-color="#d8dfef" />
        <stop offset="1" stop-color="#f0e4e6" />
      </linearGradient>
      <linearGradient :id="`${uid}-water-cloud`" x2="0.3" y2="1">
        <stop stop-color="#fffdf7" />
        <stop offset="0.58" stop-color="#faf8f6" />
        <stop offset="1" stop-color="#c0bdd8" />
      </linearGradient>
      <filter :id="`${uid}-water-edge`" x="-15%" y="-20%" width="130%" height="140%">
        <feTurbulence
          type="fractalNoise"
          baseFrequency="0.025"
          numOctaves="3"
          seed="12"
          result="noise"
        />
        <feDisplacementMap in="SourceGraphic" in2="noise" scale="13" />
        <feGaussianBlur stdDeviation="1.6" />
      </filter>
      <filter :id="`${uid}-paper-shadow`" x="-20%" y="-20%" width="150%" height="160%">
        <feDropShadow dx="0" dy="6" stdDeviation="4" flood-color="#173d47" flood-opacity="0.18" />
      </filter>
      <filter :id="`${uid}-ink-edge`" x="-10%" y="-20%" width="120%" height="140%">
        <feTurbulence
          type="fractalNoise"
          baseFrequency="0.04"
          numOctaves="3"
          seed="5"
          result="noise"
        />
        <feDisplacementMap in="SourceGraphic" in2="noise" scale="3" />
      </filter>
      <filter :id="`${uid}-soft`" x="-25%" y="-30%" width="150%" height="160%">
        <feGaussianBlur stdDeviation="5" />
      </filter>
      <linearGradient :id="`${uid}-sunset`" x2="0" y2="1">
        <stop stop-color="#727aaf" />
        <stop offset="0.48" stop-color="#c9a2bb" />
        <stop offset="1" stop-color="#f3c5a2" />
      </linearGradient>
      <linearGradient :id="`${uid}-gold-cloud`" x1="0.4" x2="0.6" y2="1">
        <stop stop-color="#fff9df" />
        <stop offset="0.35" stop-color="#ffe7c0" />
        <stop offset="0.7" stop-color="#e9b3b2" />
        <stop offset="1" stop-color="#a29ab9" />
      </linearGradient>
      <radialGradient :id="`${uid}-glow`">
        <stop stop-color="#fff2bf" stop-opacity="0.8" />
        <stop offset="1" stop-color="#ffe8bf" stop-opacity="0" />
      </radialGradient>
      <linearGradient :id="`${uid}-ink-mountain`" x2="0.2" y2="1">
        <stop stop-color="#64716d" stop-opacity="0.6" />
        <stop offset="1" stop-color="#a3aaa0" stop-opacity="0" />
      </linearGradient>
      <pattern :id="`${uid}-grain`" width="7" height="7" patternUnits="userSpaceOnUse">
        <circle cx="1" cy="2" r="0.65" fill="#555d69" opacity="0.11" />
        <circle cx="5" cy="6" r="0.4" fill="#fff" opacity="0.4" />
      </pattern>
      <pattern :id="`${uid}-pixel-grid`" width="16" height="16" patternUnits="userSpaceOnUse">
        <path d="M16 0H0v16" fill="none" stroke="#fff" stroke-opacity="0.05" />
      </pattern>
    </defs>

    <g :clip-path="paint('frame')">
      <template v-if="variant === 'watercolor'">
        <rect x="58" y="36" width="684" height="340" :fill="paint('water-sky')" />
        <g :filter="paint('water-edge')" opacity="0.5">
          <ellipse cx="216" cy="164" rx="147" ry="85" fill="#c3d9e5" />
          <ellipse cx="594" cy="285" rx="171" ry="85" fill="#dfbed2" />
          <ellipse cx="500" cy="66" rx="153" ry="76" fill="#d5d5eb" />
        </g>
        <circle
          class="scene-sun"
          cx="573"
          cy="114"
          r="35"
          fill="#f0d5b0"
          :filter="paint('water-edge')"
        />
        <path
          d="M58 338q145-39 297 4t387-8v42H58Z"
          fill="#c2ccd8"
          opacity="0.4"
          :filter="paint('water-edge')"
        />
        <g class="drifting" :filter="paint('water-edge')">
          <use :href="`#${uid}-cloud`" :fill="paint('water-cloud')" />
          <ellipse cx="290" cy="199" rx="76" ry="48" fill="#fffcf7" opacity="0.5" />
          <ellipse cx="399" cy="223" rx="110" ry="47" fill="#fdf7f0" opacity="0.35" />
          <path
            d="M208 275q92 18 164-1t182-5"
            fill="none"
            stroke="#b8b5d5"
            stroke-width="13"
            opacity="0.25"
          />
        </g>
        <g fill="#b9afcc" opacity="0.5">
          <circle cx="159" cy="275" r="3" />
          <circle cx="647" cy="207" r="2" />
          <circle cx="619" cy="220" r="4" />
          <circle cx="185" cy="116" r="2.5" />
        </g>
        <rect x="58" y="36" width="684" height="340" :fill="paint('grain')" />
      </template>

      <template v-else-if="variant === 'paper'">
        <rect x="58" y="36" width="684" height="340" fill="#235866" />
        <path d="M33 148Q344-38 750 124v270H33Z" fill="#397786" :filter="paint('paper-shadow')" />
        <path d="M39 217Q368 17 763 204v189H39Z" fill="#64a1aa" :filter="paint('paper-shadow')" />
        <circle
          class="scene-sun"
          cx="572"
          cy="115"
          r="44"
          fill="#edc47e"
          :filter="paint('paper-shadow')"
        />
        <circle cx="572" cy="115" r="29" fill="#f6dca1" />
        <path
          d="M38 322q116-65 227-11t231 1 266-24v104H38Z"
          fill="#94beb8"
          :filter="paint('paper-shadow')"
        />
        <path
          d="M38 364q156-66 308-17t419-9v68H38Z"
          fill="#c5d8c5"
          :filter="paint('paper-shadow')"
        />
        <g class="drifting" :filter="paint('paper-shadow')">
          <use :href="`#${uid}-cloud`" transform="translate(12 20)" fill="#3d7a82" />
          <use :href="`#${uid}-cloud`" transform="translate(7 12)" fill="#b8d2ca" />
          <use :href="`#${uid}-cloud`" transform="translate(3 5)" fill="#e2e5ce" />
          <use :href="`#${uid}-cloud`" fill="#fff4d8" />
          <path d="M183 242q8 31 55 31h314" fill="none" stroke="#ede0ba" stroke-width="2" />
        </g>
        <g fill="#dbe8d6" opacity="0.6">
          <circle cx="134" cy="153" r="3" />
          <circle cx="651" cy="203" r="3" />
          <circle cx="197" cy="92" r="2" />
        </g>
      </template>

      <template v-else-if="variant === 'pixel'">
        <rect x="58" y="36" width="684" height="340" fill="#263a69" />
        <rect x="58" y="36" width="684" height="340" :fill="paint('pixel-grid')" />
        <g shape-rendering="crispEdges">
          <g class="scene-sun" fill="#f6d18c">
            <path d="M576 84h32v8h8v32h-8v8h-32v-8h-8V92h8Z" />
            <path d="M588 64h8v8h-8Zm0 80h8v8h-8Zm-40-40h8v8h-8Zm80 0h8v8h-8Z" />
          </g>
          <g
            v-for="(star, index) in stars"
            :key="index"
            class="pixel-star"
            :style="{ animationDelay: `${index * -0.6}s` }"
            fill="#a9c6df"
          >
            <path :d="`M${star[0]} ${star[1]}h8v8h-8Z`" />
          </g>
          <path
            d="M58 336h32v-16h32v-16h32v-16h32v16h32v16h32v16h32v-16h32v-16h32v16h32v16h32v-32h32v-16h32v-16h32v16h32v16h32v16h32v-16h32v-16h32v16h32v16h32v64H58Z"
            fill="#465581"
          />
          <path
            d="M58 360h64v-16h64v16h96v-16h64v-16h64v16h64v16h96v-16h64v16h80v32H58Z"
            fill="#64749a"
          />
          <g class="drifting pixel-cloud">
            <path
              d="M196 224h24v-32h32v-32h32v-32h64v16h32v32h48v-16h48v32h32v16h48v32h32v48H196Z"
              fill="#121f43"
              transform="translate(8 8)"
            />
            <path
              d="M196 224h24v-32h32v-32h32v-32h64v16h32v32h48v-16h48v32h32v16h48v32h32v48H196Z"
              fill="#f7f5e9"
            />
            <path d="M196 264h80v-16h64v16h80v-16h80v16h88v24H196Z" fill="#b8c9db" />
            <path d="M268 184v-16h32v-24h32v8h-24v24h-32v16h-8Z" fill="#fff" />
          </g>
          <text
            x="96"
            y="75"
            fill="#a9c6df"
            font-family="monospace"
            font-size="12"
            letter-spacing="3"
          >
            SKY / LV. 01
          </text>
          <text
            x="594"
            y="352"
            fill="#d4dfec"
            font-family="monospace"
            font-size="11"
            letter-spacing="2"
          >
            TAKE IT EASY
          </text>
        </g>
      </template>

      <template v-else-if="variant === 'ink'">
        <rect x="58" y="36" width="684" height="340" fill="#f0f0e6" />
        <g :filter="paint('ink-edge')">
          <path
            d="M61 310q38-71 73-94t61-75q22 66 46 85t65 81Z"
            :fill="paint('ink-mountain')"
            opacity="0.5"
          />
          <path
            d="M418 335q27-113 72-152t55-77q11 46 46 83t83 146Z"
            :fill="paint('ink-mountain')"
            opacity="0.7"
          />
          <path d="M566 351q32-54 58-74t44-68q21 73 74 107v60Z" :fill="paint('ink-mountain')" />
          <path
            d="M62 360q49-73 90-49t79 47q53-35 101-7t101 25H62Z"
            fill="#a5ada2"
            opacity="0.27"
          />
          <circle class="scene-sun" cx="578" cy="101" r="25" fill="#bc7462" opacity="0.75" />
          <g class="drifting">
            <path
              d="M131 245q12-28 52-25-3-31 32-38t58 10q13-46 57-44t56 40q37-29 68 0 31-11 46 14 52-8 75 20 46-4 68 26-90 26-185 17t-170 1q-84 10-157-6Z"
              fill="#fafbf3"
              opacity="0.95"
            />
            <path
              d="M140 240q15-22 45-20-3-31 32-38t57 10q13-46 56-44t56 40q37-29 68 0 31-11 46 14 52-8 75 20 46-4 62 17"
              fill="none"
              stroke="#6a7c70"
              stroke-width="1.7"
              opacity="0.65"
            />
            <path
              d="M166 253q72 12 143 0t119-1 177-6m-355 22q115-12 218-1"
              fill="none"
              stroke="#9daa9e"
              stroke-width="1.1"
              opacity="0.55"
            />
          </g>
          <path
            d="M84 346q105-7 188 4m248-5q86-8 176-1"
            fill="none"
            stroke="#9da99b"
            stroke-width="1"
          />
        </g>
        <g fill="#69766c" font-family="'KaiTi', 'STKaiti', serif" font-size="15">
          <text x="112" y="95">云</text>
          <text x="112" y="120">起</text>
          <text x="112" y="145">时</text>
        </g>
        <g transform="translate(665 299)">
          <rect width="25" height="29" rx="2" fill="none" stroke="#b77764" stroke-width="2" />
          <text
            x="12.5"
            y="20"
            text-anchor="middle"
            fill="#b77764"
            font-family="serif"
            font-size="17"
          >
            闲
          </text>
        </g>
        <rect x="58" y="36" width="684" height="340" :fill="paint('grain')" />
      </template>

      <template v-else-if="variant === 'natural'">
        <rect x="58" y="36" width="684" height="340" :fill="paint('sunset')" />
        <ellipse cx="525" cy="188" rx="240" ry="180" :fill="paint('glow')" />
        <circle
          class="scene-sun"
          cx="559"
          cy="238"
          r="35"
          fill="#ffe3ae"
          opacity="0.85"
          :filter="paint('soft')"
        />
        <g
          fill="none"
          stroke="#f5d4cc"
          stroke-linecap="round"
          opacity="0.3"
          :filter="paint('soft')"
        >
          <path d="M111 109h175m238-26h140M88 302h126m310 9h129" stroke-width="7" />
        </g>
        <path
          d="m58 349 78-28 66 16 90-28 96 37 85-27 80 8 85-33 104 43v55H58Z"
          fill="#8d85a2"
          opacity="0.5"
        />
        <path
          d="m58 367 100-17 83 22 98-23 133 19 104-28 77 21 89-16v46H58Z"
          fill="#77768f"
          opacity="0.6"
        />
        <g class="drifting">
          <use :href="`#${uid}-cloud`" :fill="paint('gold-cloud')" :filter="paint('soft')" />
          <g :filter="paint('soft')">
            <ellipse cx="303" cy="165" rx="47" ry="49" fill="#fff5d9" opacity="0.8" />
            <ellipse cx="262" cy="197" rx="41" ry="35" fill="#fff2d3" opacity="0.75" />
            <ellipse cx="358" cy="191" rx="51" ry="42" fill="#ffeaca" opacity="0.7" />
            <ellipse cx="426" cy="205" rx="54" ry="33" fill="#ffe9cc" opacity="0.8" />
            <ellipse cx="494" cy="230" rx="40" ry="29" fill="#ffe0bd" opacity="0.75" />
            <path
              d="M202 267q97-20 168 1t191-8"
              fill="none"
              stroke="#a69bb8"
              stroke-width="17"
              opacity="0.4"
            />
          </g>
        </g>
        <g fill="none" stroke="#706d92" stroke-width="1.5" stroke-linecap="round">
          <path d="m620 155 7 3 7-3m-42-28 5 2 5-2" />
        </g>
        <rect x="58" y="36" width="684" height="340" :fill="paint('grain')" opacity="0.3" />
      </template>

      <g
        class="variant-rain"
        stroke="currentColor"
        :stroke-width="variant === 'pixel' ? 4 : 2.5"
        :stroke-linecap="variant === 'pixel' ? 'square' : 'round'"
      >
        <path
          v-for="(drop, index) in rain"
          :key="index"
          class="variant-drop"
          :d="`M${drop.x} 294l-5 12`"
          :style="{ animationDelay: drop.delay }"
        />
      </g>
    </g>
  </svg>
</template>

<style scoped>
.variant-art {
  display: block;
  width: 100%;
  overflow: visible;
  color: #8198b1;
}
.drifting {
  transform-origin: 390px 210px;
  animation: drift 7s ease-in-out infinite;
}
.scene-sun {
  transition: opacity 0.7s ease;
}
.pixel-star {
  animation: pixel-twinkle 3s steps(2, end) infinite;
}
.art-pixel .drifting {
  animation-timing-function: steps(6, end);
}
.art-pixel {
  color: #a9c6df;
}
.art-paper {
  color: #e7edd4;
}
.art-ink {
  color: #91a295;
}
.art-natural {
  color: #dddaee;
}
.variant-rain {
  opacity: 0;
  transition: opacity 0.5s ease;
}
.variant-drop {
  animation: rain-fall 1.6s linear infinite;
  animation-play-state: paused;
}
.raining .variant-rain {
  opacity: 0.85;
}
.raining .variant-drop {
  animation-play-state: running;
}
.raining .scene-sun {
  opacity: 0.2;
}
.paused * {
  animation-play-state: paused !important;
}
@keyframes drift {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}
@keyframes pixel-twinkle {
  0%,
  100% {
    opacity: 0.3;
  }
  50% {
    opacity: 0.9;
  }
}
@keyframes rain-fall {
  0% {
    transform: translate(0, 0);
    opacity: 0;
  }
  20%,
  75% {
    opacity: 0.8;
  }
  100% {
    transform: translate(-14px, 56px);
    opacity: 0;
  }
}
@media (prefers-reduced-motion: reduce) {
  .variant-art * {
    animation: none !important;
    transition: none !important;
  }
  .variant-drop {
    opacity: 0.6;
    transform: translateY(20px);
  }
}
</style>
