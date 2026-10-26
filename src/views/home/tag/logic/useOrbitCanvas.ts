import { onBeforeUnmount, onMounted, watch, type Ref } from 'vue'

type SceneState = { paused: boolean; pointerX: number; pointerY: number }

/** 本地生成有前后遮挡的行星环，预绘制静态纹理，离页释放动画和监听。 */
export function useOrbitCanvas(canvas: Ref<HTMLCanvasElement | null>, state: SceneState) {
  let frame = 0
  let observer: ResizeObserver | undefined
  let motion: MediaQueryList | undefined
  let disposed = false
  let width = 1,
    height = 1,
    clock = 0,
    lastTime = 0,
    seed = 271828
  let background: HTMLCanvasElement | undefined
  let context: CanvasRenderingContext2D | null = null
  let geometry = { cx: 0, cy: 0, rx: 1, ry: 1, tilt: 1, planet: 1 }
  /** 固定种子保持环纹和构图稳定。 */
  function random() {
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0
    return seed / 4294967296
  }
  const stars = Array.from({ length: 320 }, () => ({
    x: random(),
    y: random(),
    radius: 0.2 + random() * 0.9,
    alpha: 0.1 + random() * 0.5,
  }))
  const motes = Array.from({ length: 96 }, () => ({
    angle: random() * Math.PI * 2,
    radius: 0.59 + random() * 0.68,
    size: 0.3 + random() * 1.1,
    phase: random() * Math.PI * 2,
  }))
  /** 将环平面的圆投影成倾斜椭圆。 */
  function project(angle: number, radius: number) {
    const x = Math.cos(angle) * radius,
      y = Math.sin(angle) * radius
    return {
      x: geometry.cx + x * geometry.rx,
      y: geometry.cy - x * geometry.tilt + y * geometry.ry,
    }
  }
  function glow(
    ctx: CanvasRenderingContext2D,
    x: number,
    y: number,
    radius: number,
    color: string,
  ) {
    const gradient = ctx.createRadialGradient(x, y, 0, x, y, radius)
    gradient.addColorStop(0, color)
    gradient.addColorStop(1, 'rgba(0,0,0,0)')
    ctx.fillStyle = gradient
    ctx.fillRect(x - radius, y - radius, radius * 2, radius * 2)
  }
  /** 环缝和明暗条纹形成物质层次，前半环更明亮。 */
  function drawRing(ctx: CanvasRenderingContext2D, front: boolean) {
    const start = front ? 0 : Math.PI,
      end = front ? Math.PI : Math.PI * 2
    ctx.save()
    ctx.translate(geometry.cx, geometry.cy)
    ctx.transform(geometry.rx, -geometry.tilt, 0, geometry.ry, 0, 0)
    for (let i = 0; i < 540; i++) {
      const radius = 0.54 + (i / 540) * 0.78
      if ((radius > 0.906 && radius < 0.941) || (radius > 1.124 && radius < 1.139)) continue
      const band =
        0.42 +
        Math.sin(radius * 210) * 0.15 +
        Math.sin(radius * 731) * 0.1 +
        (Math.sin(i * 11.7) * 0.5 + 0.5) * 0.3
      const color =
        radius < 0.68
          ? '101,131,146'
          : radius < 0.9
            ? '206,171,121'
            : radius < 1.12
              ? '204,192,163'
              : '105,137,154'
      const opacity = band * Math.sin(((radius - 0.54) / 0.78) * Math.PI)
      const light = ctx.createLinearGradient(0, -1.32, 0, 1.32)
      light.addColorStop(0, `rgba(${color},${opacity * 0.1})`)
      light.addColorStop(0.5, `rgba(${color},${opacity * 0.26})`)
      light.addColorStop(1, `rgba(${color},${opacity * 0.75})`)
      ctx.strokeStyle = light
      ctx.lineWidth = 0.001 + (Math.sin(i * 19.1) * 0.5 + 0.5) * 0.0015
      ctx.beginPath()
      ctx.arc(0, 0, radius, start, end)
      ctx.stroke()
    }
    for (const [radius, alpha] of [
      [0.54, 0.16],
      [0.9, 0.33],
      [0.95, 0.18],
      [1.12, 0.2],
      [1.32, 0.14],
    ] as const) {
      const edgeLight = ctx.createLinearGradient(0, -1.32, 0, 1.32)
      edgeLight.addColorStop(0, `rgba(224,211,176,${alpha * 0.2})`)
      edgeLight.addColorStop(0.5, `rgba(224,211,176,${alpha * 0.5})`)
      edgeLight.addColorStop(1, `rgba(224,211,176,${alpha})`)
      ctx.strokeStyle = edgeLight
      ctx.lineWidth = 0.0011
      ctx.beginPath()
      ctx.arc(0, 0, radius, start, end)
      ctx.stroke()
    }
    ctx.restore()
    for (let i = 0; i < 13000; i++) {
      const radius = 0.55 + random() * 0.76
      if (radius > 0.906 && radius < 0.941) continue
      const angle = start + random() * Math.PI,
        p = project(angle, radius)
      ctx.fillStyle = `rgba(${i % 3 ? '231,213,177' : '157,197,212'},${(0.04 + random() * 0.32 + Math.max(0, Math.sin(angle)) * 0.13) * (0.75 + Math.sin(angle) * 0.25)})`
      ctx.beginPath()
      ctx.arc(p.x, p.y, 0.14 + random() * 0.65, 0, Math.PI * 2)
      ctx.fill()
    }
  }
  /** 行星暗面保留气带纹理，光照集中在左上边缘。 */
  function drawPlanet(ctx: CanvasRenderingContext2D) {
    const { cx, cy, planet: radius } = geometry
    glow(ctx, cx, cy, radius * 1.16, 'rgba(124,137,137,.11)')
    ctx.save()
    ctx.beginPath()
    ctx.arc(cx, cy, radius, 0, Math.PI * 2)
    ctx.clip()
    const sphere = ctx.createRadialGradient(
      cx - radius * 0.48,
      cy - radius * 0.5,
      0,
      cx + radius * 0.12,
      cy + radius * 0.12,
      radius * 1.25,
    )
    sphere.addColorStop(0, '#6b6251')
    sphere.addColorStop(0.25, '#393b35')
    sphere.addColorStop(0.58, '#111b1d')
    sphere.addColorStop(0.82, '#070d12')
    sphere.addColorStop(1, '#03080c')
    ctx.fillStyle = sphere
    ctx.fillRect(cx - radius, cy - radius, radius * 2, radius * 2)
    for (let i = 0; i < 140; i++) {
      ctx.strokeStyle = `rgba(${i % 3 ? '191,170,126' : '84,119,125'},${0.008 + random() * 0.025})`
      ctx.lineWidth = 1 + random() * 5
      ctx.beginPath()
      ctx.ellipse(
        cx,
        cy - radius + (i / 140) * radius * 2,
        radius * 1.7,
        radius * 0.15,
        -0.27,
        0,
        Math.PI * 2,
      )
      ctx.stroke()
    }
    ctx.restore()
    ctx.save()
    ctx.strokeStyle = 'rgba(192,179,139,.38)'
    ctx.lineWidth = 0.7
    ctx.shadowColor = '#c6ad78'
    ctx.shadowBlur = 13
    ctx.beginPath()
    ctx.arc(cx, cy, radius, Math.PI * 1.03, Math.PI * 1.64)
    ctx.stroke()
    ctx.restore()
  }
  /** 按星野、后半环、行星、前半环的顺序预绘制，建立真实的遮挡关系。 */
  function buildBackground() {
    background = document.createElement('canvas')
    background.width = Math.ceil(width)
    background.height = Math.ceil(height)
    const ctx = background.getContext('2d')
    if (!ctx) return
    seed = 314159
    geometry =
      width <= 690
        ? {
            cx: width * 0.62,
            cy: height * 0.65,
            rx: width * 0.9,
            ry: height * 0.1,
            tilt: height * 0.14,
            planet: width * 0.19,
          }
        : {
            cx: width * 0.73,
            cy: height * 0.55,
            rx: width * 0.46,
            ry: height * 0.18,
            tilt: height * 0.253,
            planet: Math.min(width * 0.125, height * 0.24),
          }
    ctx.fillStyle = '#060a0e'
    ctx.fillRect(0, 0, width, height)
    glow(ctx, width * 0.76, height * 0.4, width * 0.5, 'rgba(46,75,88,.15)')
    glow(ctx, width * 0.43, height * 0.8, width * 0.34, 'rgba(112,82,47,.07)')
    for (const star of stars) {
      ctx.fillStyle = `rgba(196,213,218,${star.alpha * 0.5})`
      ctx.beginPath()
      ctx.arc(star.x * width, star.y * height, star.radius, 0, Math.PI * 2)
      ctx.fill()
    }
    drawRing(ctx, false)
    drawPlanet(ctx)
    drawRing(ctx, true)
    const glint = project(Math.PI * 0.7, 1.05)
    glow(ctx, glint.x, glint.y, Math.min(width, height) * 0.2, 'rgba(178,145,92,.055)')
  }
  function resize() {
    if (!canvas.value || !context || disposed) return
    const bounds = canvas.value.getBoundingClientRect()
    width = Math.max(1, bounds.width)
    height = Math.max(1, bounds.height)
    const ratio = Math.min(window.devicePixelRatio || 1, 1.5)
    canvas.value.width = Math.round(width * ratio)
    canvas.value.height = Math.round(height * ratio)
    context.setTransform(ratio, 0, 0, ratio, 0, 0)
    buildBackground()
    paint()
  }
  /** 仅更新少量沿环运行的微光，背面光点进入行星轮廓后隐藏。 */
  function paint() {
    if (!context || !background) return
    context.drawImage(background, 0, 0, width, height)
    for (const mote of motes) {
      const angle = mote.angle + (clock * 0.018) / mote.radius,
        point = project(angle, mote.radius)
      if (
        Math.sin(angle) < 0 &&
        Math.hypot(point.x - geometry.cx, point.y - geometry.cy) < geometry.planet
      )
        continue
      context.globalAlpha =
        (0.25 + Math.sin(clock * 0.22 + mote.phase) * 0.12) * (Math.sin(angle) < 0 ? 0.4 : 1)
      context.fillStyle = '#e7d5b5'
      context.beginPath()
      context.arc(
        point.x + (motion?.matches ? 0 : state.pointerX * 7),
        point.y + (motion?.matches ? 0 : state.pointerY * 5),
        mote.size,
        0,
        Math.PI * 2,
      )
      context.fill()
      if (mote.size > 1.15) {
        context.globalAlpha *= 0.12
        context.beginPath()
        context.arc(point.x, point.y, mote.size * 5, 0, Math.PI * 2)
        context.fill()
      }
    }
    context.globalAlpha = 1
  }
  function animate(time: number) {
    if (disposed) return
    if (!lastTime || time - lastTime >= 32) {
      if (lastTime) clock += Math.min(time - lastTime, 80) / 1000
      paint()
      lastTime = time
    }
    frame = requestAnimationFrame(animate)
  }
  /** 暂停、切后台或偏好减少动态效果时取消持续绘制。 */
  function syncAnimation() {
    cancelAnimationFrame(frame)
    lastTime = 0
    if (!context || disposed) return
    if (!document.hidden) paint()
    if (!state.paused && !motion?.matches && !document.hidden)
      frame = requestAnimationFrame(animate)
  }
  watch(() => state.paused, syncAnimation)
  watch(
    () => [state.pointerX, state.pointerY],
    () => {
      if (state.paused && !document.hidden) paint()
    },
  )
  onMounted(() => {
    context = canvas.value?.getContext('2d') ?? null
    if (!context || !canvas.value) return
    motion = window.matchMedia('(prefers-reduced-motion: reduce)')
    motion.addEventListener('change', syncAnimation)
    document.addEventListener('visibilitychange', syncAnimation)
    observer = new ResizeObserver(resize)
    observer.observe(canvas.value)
    resize()
    syncAnimation()
  })
  onBeforeUnmount(() => {
    disposed = true
    cancelAnimationFrame(frame)
    observer?.disconnect()
    motion?.removeEventListener('change', syncAnimation)
    document.removeEventListener('visibilitychange', syncAnimation)
    background = undefined
    context = null
  })
}
