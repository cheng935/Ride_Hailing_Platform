<template>
  <section class="screen">
    <div class="panel full">
      <h1>My Orders</h1>
      <p class="sub">Your ride history</p>

      <div v-if="loading" class="loading">Loading...</div>

      <div v-else-if="orders.length === 0" class="empty">
        <p>No orders yet</p>
        <button @click="router.push('/booking')">Book a Ride →</button>
      </div>

      <div v-else class="order-list">
        <div v-for="o in orders" :key="o.orderId" class="order-card" @click="goToOrder(o)">
          <div class="order-header">
            <span class="status-badge" :class="o.status.toLowerCase()">{{ statusText(o.status) }}</span>
            <span class="fare">¥{{ o.actualFare || o.estimatedFare || '--' }}</span>
          </div>
          <div class="order-route">
            <span class="dot green"></span>
            <span class="route-text">{{ o.pickupName || '--' }}</span>
          </div>
          <div class="order-route">
            <span class="dot red"></span>
            <span class="route-text">{{ o.destName || '--' }}</span>
          </div>
          <div class="order-footer">
            <span>{{ formatDate(o.createdAt) }}</span>
            <span v-if="o.driverName">Driver: {{ o.driverName }}</span>
          </div>
        </div>
      </div>

      <div class="nav-links">
        <span @click="router.push('/booking')">← Back to Booking</span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useOrderStore } from '@/stores/order'

const router = useRouter()
const orderStore = useOrderStore()

const orders = ref([])
const loading = ref(true)

const statusMap = {
  PENDING: 'Waiting',
  ACCEPTED: 'Accepted',
  PICKING_UP: 'Arrived',
  IN_PROGRESS: 'In Progress',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled'
}

function statusText(status) {
  return statusMap[status] || status
}

function formatDate(dateStr) {
  if (!dateStr) return '--'
  const d = new Date(dateStr)
  return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

function goToOrder(order) {
  if (order.status === 'COMPLETED') {
    router.push('/complete/' + order.orderId)
  } else {
    router.push('/tracking/' + order.orderId)
  }
}

onMounted(async () => {
  const result = await orderStore.fetchOrderHistory()
  orders.value = result || []
  loading.value = false
})
</script>

<style scoped>
.screen { height: 100%; position: relative; background: #f8f9fb; }
.panel.full { position: absolute; left: 0; right: 0; top: 0; bottom: 0; background: white; padding: 26px 26px 226px 26px; overflow-y: auto; }
h1 { margin: 0; font-size: 28px; color: #111827; font-weight: 800; }
.sub { color: #4b5563; font-weight: 500; margin-top: 6px; margin-bottom: 20px; }
.loading { text-align: center; color: #6b7280; padding: 40px; }
.empty { text-align: center; padding: 40px; }
.empty p { color: #6b7280; margin-bottom: 16px; }
.order-list { display: flex; flex-direction: column; gap: 12px; }
.order-card { background: #f8fafc; border-radius: 16px; padding: 16px; cursor: pointer; transition: all 0.2s; }
.order-card:hover { background: #eef5ff; transform: translateY(-2px); }
.order-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.status-badge { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; }
.status-badge.pending { background: #fef3c7; color: #92400e; }
.status-badge.accepted { background: #dbeafe; color: #1e40af; }
.status-badge.picking_up { background: #d1fae5; color: #065f46; }
.status-badge.in_progress { background: #e0e7ff; color: #3730a3; }
.status-badge.completed { background: #d1fae5; color: #065f46; }
.status-badge.cancelled { background: #fee2e2; color: #991b1b; }
.fare { font-weight: 700; color: #111827; font-size: 18px; }
.order-route { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.dot { width: 10px; height: 10px; border-radius: 50%; }
.dot.green { background: #22c55e; }
.dot.red { background: #ef4444; }
.route-text { color: #374151; font-size: 14px; }
.order-footer { display: flex; justify-content: space-between; margin-top: 12px; padding-top: 12px; border-top: 1px solid #e5e7eb; font-size: 12px; color: #6b7280; }
button { width: 100%; height: 54px; border: none; border-radius: 18px; background: linear-gradient(135deg, #111827, #1e3a8a); color: white; font-size: 16px; font-weight: 700; cursor: pointer; }
.nav-links { margin-top: 20px; text-align: center; }
.nav-links span { color: #1a73e8; font-weight: 600; cursor: pointer; }
</style>