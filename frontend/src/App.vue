<template>
  <div class="app">
    <PhoneFrame>
      <router-view />
    </PhoneFrame>
  </div>
</template>

<script setup>
import PhoneFrame from '@/components/shared/PhoneFrame.vue'
</script>

<style>
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  background: linear-gradient(135deg, #dfe9f3 0%, #ffffff 100%);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

.app {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
}

::-webkit-scrollbar {
  width: 4px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 4px;
}
::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}
</style>