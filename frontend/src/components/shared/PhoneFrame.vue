<template>
  <div class="phone">
    <div class="top">
      <span>{{ currentTime }}</span>
      <span>RideGo 🚗</span>
    </div>
    <div class="content">
      <slot></slot>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const currentTime = ref('')
let timer = null

function updateTime() {
  const now = new Date()
  const h = now.getHours().toString().padStart(2, '0')
  const m = now.getMinutes().toString().padStart(2, '0')
  currentTime.value = h + ':' + m
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 10000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.phone {
  width: 390px;
  height: 820px;
  background: #f8f9fb;
  border-radius: 42px;
  overflow: hidden;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.18);
  position: relative;
  border: 8px solid #111827;
  display: flex;
  flex-direction: column;
}
.top {
  height: 48px;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  font-weight: 700;
  color: #111827;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  position: relative;
  z-index: 10;
  flex-shrink: 0;
}
.content {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  position: relative;
}
</style>