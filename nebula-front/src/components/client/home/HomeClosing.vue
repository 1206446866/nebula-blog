<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { useHomeClosing } from './logic/useHomeClosing'

const { canRead, links, goTop, year } = useHomeClosing()
</script>

<template>
  <footer class="home-closing" aria-labelledby="closing-title">
    <div class="closing-sky" aria-hidden="true">
      <svg viewBox="0 0 1200 400" fill="none" preserveAspectRatio="xMidYMid slice">
        <path
          d="M120 140h24m-12-12v24M1032 110h14m-7-7v14"
          stroke="#9eafc4"
          stroke-opacity=".4"
          stroke-width=".6"
        />
        <g fill="#c5c9c9">
          <circle cx="220" cy="80" r="1" opacity=".45" />
          <circle cx="420" cy="175" r=".8" opacity=".3" />
          <circle cx="830" cy="65" r="1" opacity=".4" />
          <circle cx="960" cy="230" r=".8" opacity=".5" />
          <circle cx="100" cy="285" r="1" opacity=".3" />
          <circle cx="1100" cy="325" r=".7" opacity=".3" />
        </g>
        <path
          d="M150 280c250-210 650-210 900-35"
          stroke="#bccbd3"
          stroke-opacity=".045"
          stroke-width=".6"
          stroke-dasharray="2 7"
        />
      </svg>
    </div>
    <div class="closing-horizon" aria-hidden="true" />
    <div class="closing-inner">
      <div class="closing-copy">
        <p class="closing-kicker"><span /> UNTIL THE NEXT LIGHT <span /></p>
        <h2 id="closing-title"><span>故事未完，</span><span>星光待续。</span></h2>
        <p class="closing-description">
          愿这一程的片刻灵感，<br class="closing-mobile-break" />成为下一次探索的起点。
        </p>
        <div class="closing-actions">
          <RouterLink v-if="canRead" to="/article" class="closing-read"
            ><span>继续阅读</span
            ><svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
              <path d="M3 10h13m-5-5 5 5-5 5" stroke="currentColor" stroke-width="1.2" /></svg
          ></RouterLink>
          <button type="button" class="closing-top" @click="goTop">
            <span>回到起点</span
            ><svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
              <path d="M10 16V4m-5 5 5-5 5 5" stroke="currentColor" stroke-width="1.2" />
            </svg>
          </button>
        </div>
      </div>
      <div class="closing-footer">
        <div class="closing-brand">
          <span class="brand-name">Nebula<span aria-hidden="true">✳</span></span>
          <p>记录思考，让灵感有处相逢。</p>
        </div>
        <nav v-if="links.length" class="closing-nav" aria-label="首页页脚导航">
          <RouterLink v-for="link in links" :key="link.to" :to="link.to">{{
            link.label
          }}</RouterLink>
        </nav>
        <p class="closing-copyright">© {{ year }} Nebula<span>向着下一片星光。</span></p>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.home-closing {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  color: #d4dedf;
  background: radial-gradient(ellipse at 50% 105%, #15263388, transparent 65%), #070b17;
  padding: 85px 64px 30px;
}
.home-closing::before {
  content: '';
  position: absolute;
  top: 0;
  left: 20%;
  right: 20%;
  height: 1px;
  background: linear-gradient(90deg, transparent, #b0c4cd20, transparent);
}
.closing-inner {
  position: relative;
  z-index: 1;
  max-width: 1312px;
  margin: auto;
}
.closing-sky {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.closing-sky svg {
  width: 100%;
  height: 100%;
}
.closing-horizon {
  position: absolute;
  bottom: -340px;
  left: -15%;
  width: 130%;
  height: 470px;
  border-top: 1px solid #b1c6d12e;
  border-radius: 50%;
  background: linear-gradient(#13212d66, #070b17 48%);
  box-shadow: 0 -12px 50px #86a9b90b;
  pointer-events: none;
}
.closing-copy {
  text-align: center;
  max-width: 850px;
  margin: 0 auto 75px;
}
.closing-kicker {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 17px;
  color: #8f9b9f;
  font-size: 8px;
  letter-spacing: 0.26em;
}
.closing-kicker span {
  width: 28px;
  height: 1px;
  background: #a4babd40;
}
.closing-copy h2 {
  margin: 30px 0 20px;
  font:
    400 clamp(32px, 3.5vw, 46px)/1.6 'Noto Serif SC',
    'Songti SC',
    'SimSun',
    Georgia,
    serif;
  letter-spacing: 0.12em;
  color: #dfe3df;
}
.closing-copy h2 span:last-child {
  color: #b5c7cb;
}
.closing-description {
  font-size: 12px;
  letter-spacing: 0.1em;
  line-height: 2.1;
  color: #7e929e;
}
.closing-mobile-break {
  display: none;
}
.closing-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 35px;
  margin-top: 32px;
}
.closing-read,
.closing-top {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 25px;
  color: #b9cdd5;
  border: 0;
  border-bottom: 1px solid #a8c2cf55;
  background: none;
  padding: 12px 0;
  font: inherit;
  font-size: 12px;
  letter-spacing: 0.12em;
  cursor: pointer;
  transition:
    color 0.2s,
    border-color 0.2s;
}
.closing-top {
  color: #8e9fa8;
  border-bottom-color: #a8c2cf22;
}
.closing-read svg,
.closing-top svg {
  width: 18px;
  height: 18px;
  transition: transform 0.2s;
}
.closing-read:hover,
.closing-top:hover {
  color: #e4f0f3;
  border-bottom-color: #c3dae6;
}
.closing-read:hover svg {
  transform: translateX(4px);
}
.closing-top:hover svg {
  transform: translateY(-3px);
}
.closing-footer {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 40px;
  align-items: center;
  border-top: 1px solid #a8c2cf16;
  padding-top: 25px;
}
.brand-name {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  color: #c1cdd2;
  font:
    23px Georgia,
    'Times New Roman',
    serif;
  letter-spacing: 0.03em;
}
.brand-name > span {
  color: #a59a81;
  font-size: 19px;
}
.closing-brand p {
  font-size: 9px;
  letter-spacing: 0.08em;
  color: #647b8a;
  margin-top: 10px;
}
.closing-nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 25px;
  font-size: 10px;
  letter-spacing: 0.15em;
  color: #8ea3af;
}
.closing-nav a {
  padding: 8px 0;
  transition: color 0.2s;
}
.closing-nav a:hover {
  color: #d3e2e6;
}
.closing-copyright {
  justify-self: end;
  color: #6a7f8d;
  font-size: 9px;
  letter-spacing: 0.08em;
  text-align: right;
}
.closing-copyright span {
  display: block;
  margin-top: 10px;
  font-size: 8px;
  color: #506877;
}
a:focus-visible,
button:focus-visible {
  outline: 1px solid #c0d2dd;
  outline-offset: 6px;
}
@media (max-width: 1050px) {
  .home-closing {
    padding: 75px 32px 28px;
  }
  .closing-footer {
    gap: 25px;
  }
  .closing-nav {
    gap: 20px;
  }
}
@media (max-width: 700px) {
  .home-closing {
    padding: 62px 24px 25px;
  }
  .closing-copy {
    margin-bottom: 55px;
  }
  .closing-kicker {
    font-size: 7px;
    gap: 12px;
    letter-spacing: 0.2em;
  }
  .closing-kicker span {
    width: 18px;
  }
  .closing-copy h2 {
    font-size: 32px;
    margin: 24px 0 18px;
  }
  .closing-copy h2 span {
    display: block;
  }
  .closing-description {
    font-size: 11px;
  }
  .closing-mobile-break {
    display: block;
  }
  .closing-actions {
    gap: 28px;
    margin-top: 25px;
  }
  .closing-read,
  .closing-top {
    font-size: 11px;
    gap: 20px;
  }
  .closing-horizon {
    bottom: -300px;
    height: 480px;
    left: -60%;
    width: 220%;
  }
  .closing-footer {
    grid-template-columns: 1fr auto;
    gap: 24px 15px;
    padding-top: 23px;
  }
  .closing-brand {
    grid-column: 1;
    grid-row: 1;
  }
  .closing-nav {
    grid-column: 2;
    grid-row: 1;
    gap: 15px;
    font-size: 9px;
  }
  .brand-name {
    font-size: 22px;
  }
  .closing-brand p {
    font-size: 8px;
  }
  .closing-copyright {
    grid-column: 1 / -1;
    justify-self: stretch;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 18px;
    border-top: 1px solid #a8c2cf0e;
    font-size: 8px;
  }
  .closing-copyright span {
    margin: 0;
    font-size: 8px;
  }
}
@media (max-width: 380px) {
  .home-closing {
    padding-left: 18px;
    padding-right: 18px;
  }
  .closing-copy h2 {
    font-size: 29px;
  }
  .closing-footer {
    gap: 23px 12px;
  }
  .closing-nav {
    gap: 11px;
  }
  .closing-brand p {
    letter-spacing: 0;
    font-size: 7px;
  }
}
@media (prefers-reduced-motion: reduce) {
  *,
  *::before {
    transition: none !important;
  }
}
</style>
