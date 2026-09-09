import { ref } from 'vue'

export function useAuroraGlow() {
  const glowX = ref(0)
  const glowY = ref(0)
  const glowActive = ref(false)
  const glowTargetX = ref(0)
  const glowTargetY = ref(0)
  const glowCurrentX = ref(0)
  const glowCurrentY = ref(0)
  let glowTicking = false

  function animateGlow() {
    const ease = 0.15
    glowCurrentX.value += (glowTargetX.value - glowCurrentX.value) * ease
    glowCurrentY.value += (glowTargetY.value - glowCurrentY.value) * ease
    glowX.value = glowCurrentX.value
    glowY.value = glowCurrentY.value
    if (
      Math.abs(glowTargetX.value - glowCurrentX.value) > 0.5 ||
      Math.abs(glowTargetY.value - glowCurrentY.value) > 0.5
    ) {
      requestAnimationFrame(animateGlow)
    } else {
      glowTicking = false
    }
  }

  function onMouseMove(event) {
    glowTargetX.value = event.clientX
    glowTargetY.value = event.clientY
    glowActive.value = true
    if (!glowTicking) {
      glowTicking = true
      requestAnimationFrame(animateGlow)
    }
  }

  function onMouseLeave() {
    glowActive.value = false
  }

  return {
    glowX,
    glowY,
    glowActive,
    onMouseMove,
    onMouseLeave,
  }
}
