<script setup lang="ts">
import { useId } from 'vue'

defineProps<{ animated: boolean }>()

const uid = `nebula-${useId()}`
const paint = (name: string) => `url(#${uid}-${name})`
const stars = Array.from({ length: 86 }, (_, index) => ({
  x: (index * 137.508 + 23) % 760,
  y: (index * 91.73 + 19) % 600,
  radius: index % 9 === 0 ? 1.7 : index % 3 === 0 ? 1 : 0.6,
  opacity: 0.25 + (index % 5) * 0.13,
  delay: `${(index % 8) * -0.7}s`,
}))
</script>

<template>
  <svg
    class="nebula-art"
    :class="{ 'is-still': !animated }"
    viewBox="0 0 760 600"
    fill="none"
    aria-hidden="true"
  >
    <defs>
      <radialGradient :id="`${uid}-halo`">
        <stop stop-color="#4e388d" stop-opacity="0.45" />
        <stop offset="0.6" stop-color="#302966" stop-opacity="0.15" />
        <stop offset="1" stop-color="#151534" stop-opacity="0" />
      </radialGradient>
      <radialGradient :id="`${uid}-violet`" cx="0.4" cy="0.4" r="0.65">
        <stop stop-color="#9784dc" stop-opacity="0.65" />
        <stop offset="0.4" stop-color="#6d4fa8" stop-opacity="0.6" />
        <stop offset="0.78" stop-color="#47326d" stop-opacity="0.3" />
        <stop offset="1" stop-color="#241d46" stop-opacity="0" />
      </radialGradient>
      <radialGradient :id="`${uid}-blue`" cx="0.6" cy="0.35" r="0.7">
        <stop stop-color="#b7e5ed" stop-opacity="0.8" />
        <stop offset="0.28" stop-color="var(--nebula-color, #82bfd7)" stop-opacity="0.8" />
        <stop offset="0.6" stop-color="#42718f" stop-opacity="0.55" />
        <stop offset="1" stop-color="#173b58" stop-opacity="0" />
      </radialGradient>
      <linearGradient :id="`${uid}-filament`" x1="0" y1="1" x2="1" y2="0">
        <stop stop-color="#997ca8" stop-opacity="0" />
        <stop offset="0.3" stop-color="#b79ebf" stop-opacity="0.65" />
        <stop offset="0.57" stop-color="#d8cbdc" stop-opacity="0.8" />
        <stop offset="0.8" stop-color="#719cae" stop-opacity="0.6" />
        <stop offset="1" stop-color="#344e77" stop-opacity="0" />
      </linearGradient>
      <radialGradient :id="`${uid}-core`">
        <stop stop-color="#f7f5e9" />
        <stop offset="0.12" stop-color="#c3e9e9" stop-opacity="0.9" />
        <stop offset="0.35" stop-color="#a7d0e0" stop-opacity="0.35" />
        <stop offset="1" stop-color="#97b9ef" stop-opacity="0" />
      </radialGradient>
      <filter
        :id="`${uid}-cloud`"
        x="-25%"
        y="-35%"
        width="150%"
        height="170%"
        color-interpolation-filters="sRGB"
      >
        <feTurbulence
          type="fractalNoise"
          baseFrequency="0.012 0.019"
          numOctaves="3"
          seed="21"
          result="noise"
        />
        <feDisplacementMap
          in="SourceGraphic"
          in2="noise"
          scale="64"
          xChannelSelector="R"
          yChannelSelector="G"
        />
        <feGaussianBlur stdDeviation="5" />
      </filter>
      <filter
        :id="`${uid}-wisps`"
        x="-25%"
        y="-40%"
        width="150%"
        height="180%"
        color-interpolation-filters="sRGB"
      >
        <feTurbulence
          type="fractalNoise"
          baseFrequency="0.02"
          numOctaves="3"
          seed="9"
          result="noise"
        />
        <feDisplacementMap in="SourceGraphic" in2="noise" scale="32" />
        <feGaussianBlur stdDeviation="2.6" />
      </filter>
      <filter :id="`${uid}-blur`" x="-30%" y="-30%" width="160%" height="160%">
        <feGaussianBlur stdDeviation="13" />
      </filter>
      <linearGradient :id="`${uid}-orbit`">
        <stop stop-color="#b2c5db" stop-opacity="0" />
        <stop offset="0.35" stop-color="#9cb2d8" stop-opacity="0.22" />
        <stop offset="0.7" stop-color="#b9c9df" stop-opacity="0.25" />
        <stop offset="1" stop-color="#a1b3ca" stop-opacity="0" />
      </linearGradient>
    </defs>

    <ellipse cx="390" cy="296" rx="350" ry="265" :fill="paint('halo')" />
    <g class="starfield">
      <circle
        v-for="(star, index) in stars"
        :key="index"
        :class="{ 'twinkle-star': index % 7 === 0 }"
        :cx="star.x"
        :cy="star.y"
        :r="star.radius"
        fill="#d9e3f5"
        :opacity="star.opacity"
        :style="{ animationDelay: star.delay }"
      />
    </g>

    <g class="cloud-mass">
      <g :filter="paint('cloud')">
        <path
          d="M88 340c-11-67 62-111 126-133-8-57 63-84 119-65 45-65 154-58 172 13 77-3 138 50 108 109 71 32 57 98-6 115-20 64-89 82-149 50-53 44-146 28-157-15-72 25-152-2-152-41-39 1-65-12-61-33Z"
          :fill="paint('violet')"
        />
        <path
          d="M177 343c13-50 64-57 86-98 31-62 73-98 130-76 60-40 125 1 104 53 64 6 92 41 63 78 55 31 33 81-17 83-54 37-85 10-113 2-78 43-110 3-132-14-43 22-107 10-121-28Z"
          :fill="paint('blue')"
        />
        <path
          d="M137 354c48-37 70-28 113-67 39-34 50-91 107-85 27-35 73-30 101-4-59 13-61 44-106 67-57 26-83 91-138 109-31 11-62 3-77-20Z"
          fill="#8e6faa"
          opacity="0.4"
        />
      </g>
      <path
        d="M171 379c74-16 87-76 136-88 78-20 108-88 179-91 46-2 62 22 85 38"
        :stroke="paint('filament')"
        stroke-width="22"
        stroke-linecap="round"
        :filter="paint('wisps')"
      />
      <path
        d="M202 382c64-35 94-21 142-66 56-51 87-12 151-47 18-11 42-13 62-5"
        :stroke="paint('filament')"
        stroke-width="10"
        stroke-linecap="round"
        opacity="0.55"
        :filter="paint('wisps')"
      />
      <path
        d="M186 390c74-23 109-70 154-91 32-16 55-27 82-54-15 40-52 58-71 90-44 52-105 72-165 55Z"
        fill="#0b1025"
        opacity="0.7"
        :filter="paint('blur')"
      />
      <path
        d="M338 329c26-22 39-57 74-68 24-8 42-12 60-5"
        :stroke="paint('filament')"
        stroke-width="6"
        opacity="0.8"
        :filter="paint('wisps')"
      />
    </g>

    <g class="orbit-lines" stroke-width="0.7">
      <ellipse
        cx="390"
        cy="300"
        rx="286"
        ry="150"
        transform="rotate(-25 390 300)"
        :stroke="paint('orbit')"
      />
      <ellipse
        cx="390"
        cy="300"
        rx="226"
        ry="206"
        transform="rotate(30 390 300)"
        :stroke="paint('orbit')"
        stroke-dasharray="2 7"
      />
      <path
        d="M120 422 268 166l300 207-448 49Z"
        stroke="#bdd1e9"
        stroke-opacity="0.1"
        stroke-dasharray="3 7"
      />
    </g>

    <g class="core-star">
      <circle cx="404" cy="273" r="61" :fill="paint('core')" />
      <path
        d="m404 249 2.5 21.5L428 273l-21.5 2.5L404 297l-2.5-21.5L380 273l21.5-2.5Z"
        fill="#edf7f4"
        opacity="0.9"
      />
      <circle cx="404" cy="273" r="2.5" fill="#fff" />
    </g>
    <g stroke="#ccd9ed" stroke-opacity="0.5" stroke-width="0.8">
      <path d="M568 116v12m-6-6h12M169 343v8m-4-4h8M551 434v10m-5-5h10" />
    </g>
    <g fill="#77869e" font-family="monospace" font-size="8" letter-spacing="2">
      <text x="109" y="103">A FIELD OF POSSIBILITIES</text>
      <text x="485" y="511">IDEAS HAVE GRAVITY</text>
    </g>
    <g stroke="#7c8da8" stroke-width="0.7" opacity="0.45">
      <path d="M78 104V88h16m588 394v16h-16" />
    </g>
  </svg>
</template>

<style scoped>
.nebula-art {
  display: block;
  width: 100%;
  height: auto;
  overflow: visible;
}
.cloud-mass {
  transform-origin: 390px 300px;
  animation: nebula-drift 22s ease-in-out infinite;
}
.core-star {
  transform-origin: 404px 273px;
  animation: core-breathe 7s ease-in-out infinite;
}
.twinkle-star {
  animation: star-breathe 5s ease-in-out infinite alternate;
}
.is-still * {
  animation-play-state: paused !important;
}
@keyframes nebula-drift {
  0%,
  100% {
    transform: rotate(-3deg) scale(0.98);
  }
  50% {
    transform: rotate(3deg) scale(1.04);
  }
}
@keyframes core-breathe {
  0%,
  100% {
    opacity: 0.7;
    transform: scale(0.9);
  }
  50% {
    opacity: 1;
    transform: scale(1.1);
  }
}
@keyframes star-breathe {
  from {
    opacity: 0.2;
  }
  to {
    opacity: 0.9;
  }
}
@media (prefers-reduced-motion: reduce) {
  .nebula-art * {
    animation: none !important;
  }
}
</style>
