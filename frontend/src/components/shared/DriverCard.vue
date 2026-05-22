<template>
  <div class="driver-card">
    <div class="avatar">👨‍✈️</div>
    <div class="info">
      <strong>{{ name }}</strong>
      <p>{{ vehiclePlate }} | ☎ {{ phone || '--' }}</p>
    </div>
    <button v-if="phone" class="call-btn" @click="$emit('call')">☎</button>
  </div>
</template>

<script setup>
defineProps({
  name: { type: String, default: '' },
  vehiclePlate: { type: String, default: '--' },
  phone: { type: String, default: '' }
})
defineEmits(['call'])
</script>

<style scoped>
.driver-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #f8fafc;
  padding: 16px;
  border-radius: 22px;
  margin-bottom: 16px;
}
.avatar { font-size: 36px; }
.info { flex: 1; }
.info p { margin: 4px 0 0; color: #6b7280; font-size: 13px; }
.call-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: white;
  color: #111827;
  border: none;
  box-shadow: 0 6px 18px rgba(0,0,0,0.08);
  cursor: pointer;
  font-size: 18px;
}
</style>