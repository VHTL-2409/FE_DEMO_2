<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  particles: {
    type: Number,
    default: 30
  },
  color: {
    type: String,
    default: 'cyan'
  },
  speed: {
    type: Number,
    default: 1
  }
})

const canvas = ref(null)
let animationId = null
let ctx = null

const initParticles = () => {
  if (!canvas.value) return
  
  ctx = canvas.value.getContext('2d')
  resizeCanvas()
  
  const particleArray = []
  
  for (let i = 0; i < props.particles; i++) {
    particleArray.push({
      x: Math.random() * canvas.value.width,
      y: Math.random() * canvas.value.height,
      size: Math.random() * 2 + 0.5,
      speedX: (Math.random() - 0.5) * 0.5 * props.speed,
      speedY: (Math.random() - 0.5) * 0.5 * props.speed,
      opacity: Math.random() * 0.5 + 0.1
    })
  }
  
  const animate = () => {
    ctx.clearRect(0, 0, canvas.value.width, canvas.value.height)
    
    particleArray.forEach((particle, index) => {
      particle.x += particle.speedX
      particle.y += particle.speedY
      
      if (particle.x < 0) particle.x = canvas.value.width
      if (particle.x > canvas.value.width) particle.x = 0
      if (particle.y < 0) particle.y = canvas.value.height
      if (particle.y > canvas.value.height) particle.y = 0
      
      ctx.beginPath()
      ctx.arc(particle.x, particle.y, particle.size, 0, Math.PI * 2)
      
      const colorMap = {
        cyan: '0, 245, 255',
        magenta: '255, 0, 255',
        violet: '139, 92, 246',
        amber: '255, 184, 0',
        green: '0, 255, 136',
        red: '255, 51, 102'
      }
      
      ctx.fillStyle = `rgba(${colorMap[props.color]}, ${particle.opacity})`
      ctx.fill()
      
      // Draw connections
      for (let j = index + 1; j < particleArray.length; j++) {
        const dx = particle.x - particleArray[j].x
        const dy = particle.y - particleArray[j].y
        const distance = Math.sqrt(dx * dx + dy * dy)
        
        if (distance < 150) {
          ctx.beginPath()
          ctx.moveTo(particle.x, particle.y)
          ctx.lineTo(particleArray[j].x, particleArray[j].y)
          ctx.strokeStyle = `rgba(${colorMap[props.color]}, ${0.1 * (1 - distance / 150)})`
          ctx.lineWidth = 0.5
          ctx.stroke()
        }
      }
    })
    
    animationId = requestAnimationFrame(animate)
  }
  
  animate()
}

const resizeCanvas = () => {
  if (!canvas.value) return
  canvas.value.width = window.innerWidth
  canvas.value.height = window.innerHeight
}

onMounted(() => {
  initParticles()
  window.addEventListener('resize', resizeCanvas)
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  window.removeEventListener('resize', resizeCanvas)
})
</script>

<template>
  <canvas ref="canvas" class="fg-particles" />
</template>

<style scoped>
.fg-particles {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
</style>
