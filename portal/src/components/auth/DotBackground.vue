<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const DOT_SIZE = 8
const DOT_SPACING = 24
const ACTIVE_RADIUS = 100
const DEFAULT_COLOR = [226, 232, 240]
const ACTIVE_COLOR = [59, 130, 246]
const GLOW_COLOR = [96, 165, 250]

const canvasRef = ref(null)
const mouse = { x: -ACTIVE_RADIUS, y: -ACTIVE_RADIUS, active: false }
const canvasSize = { width: 0, height: 0 }

let animationFrame = 0
let context = null
let dots = []
let motionQuery = null
let resizeObserver = null

function interpolateColor(from, to, amount) {
  return from.map((channel, index) => Math.round(channel + (to[index] - channel) * amount))
}

function createDots(width, height) {
  const nextDots = []
  for (let x = DOT_SPACING / 2; x <= width; x += DOT_SPACING) {
    for (let y = DOT_SPACING / 2; y <= height; y += DOT_SPACING) {
      nextDots.push({ x, y, intensity: 0 })
    }
  }
  return nextDots
}

function resizeCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return

  const rect = canvas.getBoundingClientRect()
  const pixelRatio = Math.min(window.devicePixelRatio || 1, 2)
  canvasSize.width = rect.width
  canvasSize.height = rect.height
  canvas.width = Math.round(rect.width * pixelRatio)
  canvas.height = Math.round(rect.height * pixelRatio)
  context = canvas.getContext('2d')
  context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0)
  dots = createDots(rect.width, rect.height)
  drawDots(performance.now())
}

function drawDots(timestamp) {
  const canvas = canvasRef.value
  if (!canvas || !context) return

  context.clearRect(0, 0, canvasSize.width, canvasSize.height)

  let closestDot = null
  let closestDistance = Number.POSITIVE_INFINITY
  if (mouse.active) {
    dots.forEach((dot) => {
      const distance = Math.hypot(dot.x - mouse.x, dot.y - mouse.y)
      if (distance < closestDistance) {
        closestDistance = distance
        closestDot = dot
      }
    })
  }

  dots.forEach((dot) => {
    const distance = mouse.active ? Math.hypot(dot.x - mouse.x, dot.y - mouse.y) : ACTIVE_RADIUS
    const target = distance < ACTIVE_RADIUS ? 1 - distance / ACTIVE_RADIUS : 0
    dot.intensity += (target - dot.intensity) * 0.14

    const isClosest = dot === closestDot && closestDistance < DOT_SPACING
    const pulse = isClosest ? (Math.sin(timestamp / 180) + 1) * 0.25 : 0
    const glowAmount = isClosest ? Math.max(dot.intensity, 0.75) : dot.intensity ** 3
    const activeColor = interpolateColor(ACTIVE_COLOR, GLOW_COLOR, glowAmount)
    const color = interpolateColor(DEFAULT_COLOR, activeColor, dot.intensity)
    const size = DOT_SIZE + dot.intensity * 2 + pulse

    context.beginPath()
    context.arc(dot.x, dot.y, size / 2, 0, Math.PI * 2)
    context.fillStyle = `rgb(${color.join(',')})`
    context.shadowColor = `rgba(96, 165, 250, ${dot.intensity * 0.75})`
    context.shadowBlur = dot.intensity * 8
    context.fill()
  })

  context.shadowBlur = 0
}

function animate(timestamp) {
  drawDots(timestamp)
  animationFrame = window.requestAnimationFrame(animate)
}

function startAnimation() {
  window.cancelAnimationFrame(animationFrame)
  if (motionQuery?.matches) {
    dots.forEach((dot) => {
      dot.intensity = 0
    })
    drawDots(performance.now())
    return
  }
  animationFrame = window.requestAnimationFrame(animate)
}

function handlePointerMove(event) {
  const rect = canvasRef.value?.getBoundingClientRect()
  if (!rect) return

  mouse.x = event.clientX - rect.left
  mouse.y = event.clientY - rect.top
  mouse.active = mouse.x >= 0 && mouse.x <= rect.width && mouse.y >= 0 && mouse.y <= rect.height
}

function handlePointerLeave() {
  mouse.active = false
}

onMounted(() => {
  motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
  resizeObserver = new ResizeObserver(resizeCanvas)
  resizeObserver.observe(canvasRef.value)
  resizeCanvas()
  startAnimation()
  window.addEventListener('resize', resizeCanvas)
  window.addEventListener('pointermove', handlePointerMove, { passive: true })
  document.documentElement.addEventListener('mouseleave', handlePointerLeave)
  motionQuery.addEventListener('change', startAnimation)
})

onBeforeUnmount(() => {
  window.cancelAnimationFrame(animationFrame)
  window.removeEventListener('resize', resizeCanvas)
  window.removeEventListener('pointermove', handlePointerMove)
  document.documentElement.removeEventListener('mouseleave', handlePointerLeave)
  motionQuery?.removeEventListener('change', startAnimation)
  resizeObserver?.disconnect()
})
</script>

<template>
  <canvas ref="canvasRef" class="dot-canvas" aria-hidden="true" />
</template>

<style scoped>
.dot-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
</style>
