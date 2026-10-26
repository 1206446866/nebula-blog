import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as THREE from 'three'

/** 首页仅作板块介绍，标签均为静态示例，不请求接口或展示真实数量。 */
const tags = [
  'Vue 3',
  '系统设计',
  'Java',
  '交互设计',
  'Docker',
  'Redis',
  'TypeScript',
  '摄影',
  'Spring Boot',
  '开源',
  'Linux',
  'AI 探索',
  'MySQL',
  '生活随笔',
  'WebGL',
  '设计灵感',
  'Git',
  '阅读',
  'MyBatis',
  '前端体验',
  '云原生',
  'Nebula',
]

/** 将标签绘成宽幅精灵，长词保持一行，避免原方形纹理里的文字挤压。 */
function createTagTexture(text: string) {
  const canvas = document.createElement('canvas')
  canvas.width = 640
  canvas.height = 144
  const ctx = canvas.getContext('2d')
  if (!ctx) return null
  ctx.font = '500 58px "Segoe UI", "Microsoft YaHei", sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.shadowColor = '#a5bac2'
  ctx.shadowBlur = 8
  ctx.fillStyle = '#ece8db'
  ctx.fillText(text, 320, 72, 590)
  const texture = new THREE.CanvasTexture(canvas)
  texture.colorSpace = THREE.SRGBColorSpace
  return texture
}

/** 保留原有标签球、球面分布及自转，增加环带、拖动与可见时渲染。 */
export function useTagSphere() {
  const container = ref<HTMLDivElement | null>(null)
  const paused = ref(false)
  const ready = ref(false)
  const activeTag = ref('Vue 3')
  let rotate = (_x: number, _y: number) => {}
  let reset = () => {}
  let dispose = () => {}

  const rotateWithKey = (event: KeyboardEvent) => {
    const direction = {
      ArrowLeft: [-0.15, 0],
      ArrowRight: [0.15, 0],
      ArrowUp: [0, -0.15],
      ArrowDown: [0, 0.15],
    }[event.key]
    if (!direction || !ready.value) return
    event.preventDefault()
    rotate(direction[0] ?? 0, direction[1] ?? 0)
  }

  onMounted(() => {
    const host = container.value
    if (!host) return
    let renderer: THREE.WebGLRenderer
    try {
      renderer = new THREE.WebGLRenderer({
        alpha: true,
        antialias: true,
        powerPreference: 'low-power',
      })
    } catch {
      // 无 WebGL 时保留静态星环和入口，不阻断整个首页。
      return
    }
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.5))
    renderer.setClearColor(0x000000, 0)
    renderer.domElement.setAttribute('aria-hidden', 'true')
    host.appendChild(renderer.domElement)
    const scene = new THREE.Scene()
    const camera = new THREE.PerspectiveCamera(43, 1, 0.1, 1000)
    camera.position.set(0, 8, 370)
    camera.lookAt(0, 0, 0)

    const system = new THREE.Group()
    system.rotation.z = -0.23
    scene.add(system)
    const globe = new THREE.Group()
    globe.rotation.set(0.13, -0.32, 0)
    system.add(globe)
    const radius = 91

    // 中央暗色天体为原标签球增加空间参照，远处的小球会被自然遮挡。
    const core = new THREE.Mesh(
      new THREE.SphereGeometry(65, 64, 40),
      new THREE.ShaderMaterial({
        vertexShader: `varying vec3 vNormal; varying vec3 vPosition;
          void main() { vNormal = normalize(normalMatrix * normal); vPosition = position;
            gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }`,
        fragmentShader: `varying vec3 vNormal; varying vec3 vPosition;
          void main() {
            float light = max(dot(normalize(vNormal), normalize(vec3(-0.65, 0.6, 0.8))), 0.0);
            float band = sin(vPosition.y * 0.3 + sin(vPosition.x * 0.05) * 2.0) * 0.012;
            float edge = pow(1.0 - abs(vNormal.z), 3.8);
            vec3 color = mix(vec3(0.018, 0.028, 0.043), vec3(0.16, 0.20, 0.23), light * 0.7);
            color += band + edge * vec3(0.13, 0.11, 0.07) * light;
            gl_FragColor = vec4(color, 1.0);
          }`,
      }),
    )
    system.add(core)

    // 延续原球体的边缘辉光，降低亮度，让标签和环带承担视觉主体。
    const glow = new THREE.Mesh(
      new THREE.SphereGeometry(96, 48, 32),
      new THREE.ShaderMaterial({
        transparent: true,
        depthWrite: false,
        blending: THREE.AdditiveBlending,
        vertexShader: `varying vec3 vNormal;
          void main() { vNormal = normalize(normalMatrix * normal);
            gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }`,
        fragmentShader: `varying vec3 vNormal;
          void main() { float edge = pow(1.0 - abs(vNormal.z), 3.0);
            gl_FragColor = vec4(0.47, 0.57, 0.61, edge * 0.075); }`,
      }),
    )
    system.add(glow)

    const nodeGeometry = new THREE.SphereGeometry(4.2, 20, 16)
    const nodeMaterials = [0xd8bc84, 0x95b7c2, 0xa1a9c1].map(
      (color) =>
        new THREE.ShaderMaterial({
          uniforms: { baseColor: { value: new THREE.Color(color) } },
          vertexShader: `varying vec3 vNormal;
          void main() { vNormal = normalize(normalMatrix * normal);
            gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }`,
          fragmentShader: `uniform vec3 baseColor; varying vec3 vNormal;
          void main() {
            float light = max(dot(normalize(vNormal), normalize(vec3(-0.5, 0.65, 1.0))), 0.0);
            float edge = pow(1.0 - abs(vNormal.z), 3.0);
            vec3 color = baseColor * (0.28 + light * 0.75) + edge * baseColor * 0.32;
            gl_FragColor = vec4(color, 1.0);
          }`,
        }),
    )
    const nodeMeshes: THREE.Mesh[] = []
    const labels: THREE.Sprite[] = []
    tags.forEach((text, i) => {
      const node = new THREE.Mesh(nodeGeometry, nodeMaterials[i % nodeMaterials.length])
      // 保留原来的球面分布算法，让既有标签球继续成为整个构图的骨架。
      const phi = Math.acos(-1 + (2 * i) / tags.length)
      const theta = Math.sqrt(tags.length * Math.PI) * phi
      node.position.set(
        radius * Math.cos(theta) * Math.sin(phi),
        radius * Math.sin(theta) * Math.sin(phi),
        radius * Math.cos(phi),
      )
      node.userData.tag = text
      globe.add(node)
      nodeMeshes.push(node)
      const texture = createTagTexture(text)
      if (!texture) return
      const label = new THREE.Sprite(
        new THREE.SpriteMaterial({ map: texture, transparent: true, depthWrite: false }),
      )
      label.scale.set(66, 14.85, 1)
      label.position.set(0, 9, 0)
      node.add(label)
      labels.push(label)
    })

    // 稀疏经纬线强调球体结构，避免重绘成单纯的平面标签云。
    const gridMaterial = new THREE.LineBasicMaterial({
      color: 0x9bb5c2,
      transparent: true,
      opacity: 0.09,
    })
    for (const latitude of [-0.6, 0, 0.6]) {
      const r = Math.sqrt(1 - latitude * latitude) * 94
      const points = Array.from({ length: 129 }, (_, i) => {
        const angle = (i / 128) * Math.PI * 2
        return new THREE.Vector3(Math.cos(angle) * r, latitude * 94, Math.sin(angle) * r)
      })
      globe.add(new THREE.Line(new THREE.BufferGeometry().setFromPoints(points), gridMaterial))
    }
    for (let i = 0; i < 3; i++) {
      const points = Array.from({ length: 129 }, (_, j) => {
        const angle = (j / 128) * Math.PI * 2
        return new THREE.Vector3(Math.cos(angle) * 94, Math.sin(angle) * 94, 0)
      })
      const line = new THREE.Line(new THREE.BufferGeometry().setFromPoints(points), gridMaterial)
      line.rotation.y = (i / 3) * Math.PI
      globe.add(line)
    }

    // 真正的三维环带经过球体前后，环带和标签球使用同一深度关系。
    const rings = new THREE.Group()
    rings.rotation.set(1.12, -0.12, 0)
    system.add(rings)
    for (let i = 0; i < 48; i++) {
      const inner = 112 + i * 0.66
      if ((i > 19 && i < 23) || i === 37) continue
      const ring = new THREE.Mesh(
        new THREE.RingGeometry(inner, inner + (i % 4 === 0 ? 0.5 : 0.22), 200),
        new THREE.MeshBasicMaterial({
          color: i % 3 === 0 ? 0xe2cf9c : 0xa38c61,
          side: THREE.DoubleSide,
          transparent: true,
          opacity: i % 4 === 0 ? 0.49 : 0.2,
          depthWrite: false,
        }),
      )
      rings.add(ring)
    }
    const dustPoints: number[] = []
    // 固定种子使示例星尘稳定，不会因每次访问而闪烁换位。
    let seed = 73
    const random = () => {
      seed = (seed * 16807) % 2147483647
      return seed / 2147483647
    }
    for (let i = 0; i < 1700; i++) {
      const angle = random() * Math.PI * 2
      const r = 109 + random() * 40
      dustPoints.push(Math.cos(angle) * r, Math.sin(angle) * r, (random() - 0.5) * 1.5)
    }
    const dustGeometry = new THREE.BufferGeometry()
    dustGeometry.setAttribute('position', new THREE.Float32BufferAttribute(dustPoints, 3))
    rings.add(
      new THREE.Points(
        dustGeometry,
        new THREE.PointsMaterial({
          color: 0xc6b17f,
          size: 0.65,
          transparent: true,
          opacity: 0.5,
          depthWrite: false,
        }),
      ),
    )

    let frame = 0,
      lastTime = 0,
      visible = true,
      hasArea = true,
      contextLost = false
    let dragging = false,
      pointerId: number | null = null,
      previousX = 0,
      previousY = 0
    const motion = window.matchMedia('(prefers-reduced-motion: reduce)')
    paused.value = motion.matches
    const worldPosition = new THREE.Vector3()
    const raycaster = new THREE.Raycaster()
    const pointer = new THREE.Vector2()
    let highlighted: THREE.Mesh | undefined

    /** 按远近弱化标签，复用向量，不在每帧为每个节点重复分配对象。 */
    const draw = () => {
      globe.updateWorldMatrix(true, true)
      for (const node of nodeMeshes) {
        node.getWorldPosition(worldPosition)
        const depth = THREE.MathUtils.clamp((worldPosition.z + radius) / (radius * 2), 0, 1)
        node.scale.setScalar(node === highlighted ? 1.35 : 0.76 + depth * 0.24)
        const label = node.children[0] as THREE.Sprite | undefined
        if (label) label.material.opacity = 0.28 + depth * 0.72
      }
      renderer.render(scene, camera)
    }
    const canAnimate = () =>
      !paused.value && !motion.matches && visible && hasArea && !document.hidden && !contextLost
    const animate = (time: number) => {
      frame = 0
      if (!canAnimate()) return
      if (!lastTime || time - lastTime >= 32) {
        const elapsed = lastTime ? Math.min(time - lastTime, 64) : 32
        if (!dragging && !highlighted) {
          globe.rotation.y += elapsed * 0.00007
          globe.rotation.x += elapsed * 0.000012
          rings.rotation.z += elapsed * 0.000015
        }
        lastTime = time
        draw()
      }
      frame = requestAnimationFrame(animate)
    }
    const syncAnimation = () => {
      cancelAnimationFrame(frame)
      frame = 0
      lastTime = 0
      if (!contextLost && hasArea && visible && !document.hidden) draw()
      if (canAnimate()) frame = requestAnimationFrame(animate)
    }
    const drawWhenStill = () => {
      if (!frame && hasArea && !contextLost && visible && !document.hidden) draw()
    }
    rotate = (x, y) => {
      globe.rotation.y += x
      globe.rotation.x = THREE.MathUtils.clamp(globe.rotation.x + y, -1.2, 1.2)
      drawWhenStill()
    }
    reset = () => {
      globe.rotation.set(0.13, -0.32, 0)
      rings.rotation.z = 0
      highlighted = undefined
      activeTag.value = 'Vue 3'
      drawWhenStill()
    }
    const resize = () => {
      const { width, height } = host.getBoundingClientRect()
      hasArea = width > 0 && height > 0
      if (hasArea) {
        camera.aspect = width / height
        // 窄屏拉远相机，保持球体完整，环带只在边缘轻微延伸。
        camera.position.z = camera.aspect < 1 ? 410 * Math.max(1, 435 / width) : 370
        camera.updateProjectionMatrix()
        renderer.setSize(width, height)
        const textScale = Math.min(1.35, Math.max(1, 600 / height))
        for (const label of labels) label.scale.set(66 * textScale, 14.85 * textScale, 1)
      }
      syncAnimation()
    }
    const down = (event: PointerEvent) => {
      if (!event.isPrimary || event.button !== 0) return
      dragging = true
      pointerId = event.pointerId
      previousX = event.clientX
      previousY = event.clientY
      host.setPointerCapture(event.pointerId)
    }
    const move = (event: PointerEvent) => {
      if (dragging && event.pointerId === pointerId) {
        rotate((event.clientX - previousX) * 0.006, (event.clientY - previousY) * 0.006)
        previousX = event.clientX
        previousY = event.clientY
        highlighted = undefined
        return
      }
      if (event.pointerType === 'touch') return
      const rect = host.getBoundingClientRect()
      if (!rect.width || !rect.height) return
      pointer.set(
        ((event.clientX - rect.left) / rect.width) * 2 - 1,
        -((event.clientY - rect.top) / rect.height) * 2 + 1,
      )
      raycaster.setFromCamera(pointer, camera)
      const hit = raycaster.intersectObjects([...nodeMeshes, core], true)[0]
      const node = hit?.object instanceof THREE.Sprite ? hit.object.parent : hit?.object
      highlighted = node?.userData.tag ? (node as THREE.Mesh) : undefined
      if (highlighted) activeTag.value = highlighted.userData.tag as string
      drawWhenStill()
    }
    const up = (event: PointerEvent) => {
      if (event.pointerId !== pointerId) return
      dragging = false
      pointerId = null
      if (host.hasPointerCapture(event.pointerId)) host.releasePointerCapture(event.pointerId)
    }
    const leave = () => {
      highlighted = undefined
      drawWhenStill()
    }
    const lostContext = (event: Event) => {
      event.preventDefault()
      contextLost = true
      ready.value = false
      syncAnimation()
    }
    const resizeObserver = new ResizeObserver(resize)
    const intersectionObserver = new IntersectionObserver(
      ([entry]) => {
        visible = entry?.isIntersecting ?? false
        syncAnimation()
      },
      { threshold: 0.02 },
    )
    resizeObserver.observe(host)
    intersectionObserver.observe(host)
    document.addEventListener('visibilitychange', syncAnimation)
    motion.addEventListener('change', syncAnimation)
    host.addEventListener('pointerdown', down)
    host.addEventListener('pointermove', move)
    host.addEventListener('pointerup', up)
    host.addEventListener('pointercancel', up)
    host.addEventListener('lostpointercapture', up)
    host.addEventListener('pointerleave', leave)
    renderer.domElement.addEventListener('webglcontextlost', lostContext)
    const stopWatch = watch(paused, syncAnimation)
    ready.value = true
    resize()

    dispose = () => {
      cancelAnimationFrame(frame)
      stopWatch()
      resizeObserver.disconnect()
      intersectionObserver.disconnect()
      document.removeEventListener('visibilitychange', syncAnimation)
      motion.removeEventListener('change', syncAnimation)
      host.removeEventListener('pointerdown', down)
      host.removeEventListener('pointermove', move)
      host.removeEventListener('pointerup', up)
      host.removeEventListener('pointercancel', up)
      host.removeEventListener('lostpointercapture', up)
      host.removeEventListener('pointerleave', leave)
      renderer.domElement.removeEventListener('webglcontextlost', lostContext)
      if (pointerId !== null && host.hasPointerCapture(pointerId))
        host.releasePointerCapture(pointerId)
      // 几何体和材质存在共享，只释放一次；文字纹理也需显式释放。
      const geometries = new Set<THREE.BufferGeometry>()
      const materials = new Set<THREE.Material>()
      scene.traverse((object) => {
        const mesh = object as THREE.Mesh
        if (mesh.geometry) geometries.add(mesh.geometry)
        if (mesh.material)
          for (const material of Array.isArray(mesh.material) ? mesh.material : [mesh.material])
            materials.add(material)
      })
      for (const label of labels) label.material.map?.dispose()
      for (const geometry of geometries) geometry.dispose()
      for (const material of materials) material.dispose()
      renderer.dispose()
      renderer.forceContextLoss()
      renderer.domElement.remove()
      rotate = () => {}
      reset = () => {}
      ready.value = false
    }
  })
  onBeforeUnmount(() => dispose())

  return { container, paused, ready, activeTag, tags, rotateWithKey, resetView: () => reset() }
}
