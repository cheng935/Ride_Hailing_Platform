<template>
  <section class="screen">
    <div class="panel full">
      <div class="header">
        <button class="back-btn" @click="router.push('/driver/home')">← Back</button>
        <h1>Trip History</h1>
      </div>

      <div v-if="loading" class="loading">Loading...</div>

      <div v-else-if="orders.length === 0" class="empty">
        <p>No completed trips yet</p>
      </div>

      <div v-else class="order-list">
        <div v-for="o in orders" :key="o.orderId" class="order-card">
          <div class="order-header">
            <span class="date">{{ formatDate(o.createdAt) }}</span>
            <span class="fare">¥{{ o.actualFare || o.estimatedFare || '--' }}</span>
          </div>
          <div class="order-route">
            <span class="dot green"></span>
            <span>{{ o.pickupName || '--' }}</span>
          </div>
          <div class="order-route">
            <span class="dot red"></span>
            <span>{{ o.destName || '--' }}</span>
          </div>
          <div class="order-footer">
            <span>👤 {{ o.passengerName || 'Passenger' }}</span>
            <span class="status-badge" :class="o.paymentStatus?.toLowerCase()">{{ o.paymentStatus || 'UNPAID' }}</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useDriverStore } from '@/stores/driver'

const router = useRouter()
const driverStore = useDriverStore()

const orders = ref([])
const loading = ref(true)

function formatDate(dateStr) {
  if (!dateStr) return '--'
  const d = new Date(dateStr)
  return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

onMounted(async () => {
  const result = await driverStore.fetchOrderHistory()
  orders.value = (result || []).filter(o => o.status === 'COMPLETED')
  loading.value = false
})
</script>

<style scoped>
.screen { height: 100%; position: relative; background: #f8f9fb; }
.panel.full { position: absolute; left: 0; right: 0; top: 0; bottom: 0; background: white; padding: 20px 20px 220px 20px; overflow-y: auto; }
.header { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.back-btn { padding: 8px 16px; border: none; border-radius: 8px; background: #f3f4f6; color: #374151; font-weight: 600; cursor: pointer; }
h1 { margin: 0; font-size: 20px; color: #111827; font-weight: 800; }
.loading { text-align: center; color: #6b7280; padding: 40px; }
.empty { text-align: center; padding: 40px; color: #6b7280; }
.order-list { display: flex; flex-direction: column; gap: 12px; }
.order-card { background: #f8fafc; border-radius: 16px; padding: 16px; }
.order-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.date { font-size: 13px; color: #6b7280; }
.fare { font-weight: 700; color: #111827; font-size: 18px; }
.order-route { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; font-size: 14px; color: #374151; }
.dot { width: 10px; height: 10px; border-radius: 50%; }
.dot.green { background: #22c55e; }
.dot.red { background: #ef4444; }
.order-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; padding-top: 12px; border-top: 1px solid #e5e7eb; font-size: 13px; color: #6b7280; }
.status-badge { padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; }
.status-badge.paid { background: #d1fae5; color: #065f46; }
.status-badge.pending { background: #fef3c7; color: #92400e; }
.status-badge.unpaid { background: #fee2e2; color: #991b1b; }
</style>