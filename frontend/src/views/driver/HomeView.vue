<template>
  <section class="screen">
    <div class="panel full">
      <div class="header">
        <h1>Driver Home</h1>
        <div class="user-info">
          <div class="status-toggle" @click="toggleOnline">
            <span class="status-dot" :class="driverStore.isOnline ? 'online' : 'offline'"></span>
            <span class="status-text">{{ driverStore.isOnline ? 'Online' : 'Offline' }}</span>
          </div>
          <button class="logout-btn" @click="handleLogout">Logout</button>
        </div>
      </div>

      <div class="profile-card" v-if="driverStore.driverInfo">
        <div class="profile-avatar">️</div>
        <div class="profile-main">
          <strong class="profile-name">{{ driverStore.driverInfo.name }}</strong>
          <p class="profile-phone">☎ {{ maskPhone(driverStore.driverInfo.phone) }}</p>
        </div>
        <div class="profile-plate" @click="editPlate">
          <span class="plate-label">Plate</span>
          <span class="plate-value">{{ driverStore.driverInfo.vehiclePlate || 'Not set' }}</span>
          <span class="plate-edit">✎</span>
        </div>
      </div>

      <div v-if="plateEditing" class="plate-edit-bar">
        <input v-model="newPlate" placeholder="Enter new plate number" @keyup.enter="savePlate" ref="plateInput" />
        <button class="save-btn" @click="savePlate" :disabled="plateSaving">{{ plateSaving ? '...' : '✓' }}</button>
        <button class="cancel-btn" @click="plateEditing = false">✕</button>
      </div>

      <div v-if="currentOrder" class="current-order">
        <h2>Current Order</h2>
        <div class="order-card active" @click="goToOrder(currentOrder)">
          <div class="order-header">
            <span class="status-badge" :class="currentOrder.status.toLowerCase()">{{ statusText(currentOrder.status) }}</span>
            <span class="fare">¥{{ currentOrder.estimatedFare || '--' }}</span>
          </div>
          <div class="order-route">
            <span class="dot green"></span>
            <span>{{ currentOrder.pickupName || '--' }}</span>
          </div>
          <div class="order-route">
            <span class="dot red"></span>
            <span>{{ currentOrder.destName || '--' }}</span>
          </div>
          <div class="passenger-info">
            👤 {{ currentOrder.passengerName || 'Passenger' }} | ☎ {{ maskPhone(currentOrder.passengerPhone) }}
          </div>
        </div>
      </div>

      <div v-else class="pending-orders">
        <h2>Pending Orders <span class="count">({{ pendingOrders.length }})</span></h2>

        <div v-if="!driverStore.isOnline" class="offline-notice">
          <p>🔌 You are currently offline</p>
          <p class="hint">Go online to receive orders</p>
        </div>

        <div v-else-if="loading" class="loading">Loading...</div>

        <div v-else-if="pendingOrders.length === 0" class="empty">
          <p>🎯 No pending orders</p>
          <p class="hint">Waiting for passengers...</p>
        </div>

        <div v-else class="order-list">
          <div v-for="o in pendingOrders" :key="o.orderId" class="order-card" @click="acceptOrderHandler(o)">
            <div class="order-header">
              <span class="fare">¥{{ o.estimatedFare || '--' }}</span>
              <span class="distance">{{ o.distance ? o.distance + ' km' : '--' }}</span>
            </div>
            <div class="order-route">
              <span class="dot green"></span>
              <span>{{ o.pickupName || '--' }}</span>
            </div>
            <div class="order-route">
              <span class="dot red"></span>
              <span>{{ o.destName || '--' }}</span>
            </div>
            <div class="order-passenger">
              👤 {{ o.passengerName || 'Passenger' }} | ☎ {{ maskPhone(o.passengerPhone) }}
            </div>
            <button class="accept-btn">Accept Order</button>
          </div>
        </div>
      </div>

      <div class="nav-links">
        <span @click="router.push('/driver/history')">📋 History</span>
        <span @click="router.push('/driver/profile')">👤 Profile</span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useDriverStore } from '@/stores/driver'

const router = useRouter()
const authStore = useAuthStore()
const driverStore = useDriverStore()

const pendingOrders = ref([])
const currentOrder = ref(null)
const loading = ref(true)
const plateEditing = ref(false)
const plateSaving = ref(false)
const newPlate = ref('')
const plateInput = ref(null)

function maskPhone(phone) {
  if (!phone) return '--'
  const s = String(phone)
  if (s.length >= 7) return s.slice(0, 3) + '****' + s.slice(-4)
  return s
}

const statusMap = {
  PENDING: 'Pending',
  ACCEPTED: 'Accepted',
  PICKING_UP: 'Arrived',
  IN_PROGRESS: 'In Progress',
  COMPLETED: 'Completed'
}

function statusText(status) {
  return statusMap[status] || status
}

async function loadData() {
  const current = await driverStore.fetchCurrentOrder()
  currentOrder.value = current

  if (!current && driverStore.isOnline) {
    await driverStore.fetchPendingOrders()
    pendingOrders.value = driverStore.pendingOrders
  }

  loading.value = false
}

async function toggleOnline() {
  if (driverStore.isOnline) {
    await driverStore.goOffline()
    pendingOrders.value = []
  } else {
    const result = await driverStore.goOnline()
    if (result.success) {
      await loadData()
    } else {
      alert(result.message || 'Failed to go online')
    }
  }
}

async function acceptOrderHandler(order) {
  const result = await driverStore.acceptOrder(order.orderId)
  if (result.success) {
    currentOrder.value = result.order
    router.push('/driver/order/' + order.orderId)
  } else {
    alert(result.message || 'Failed to accept order')
    await loadData()
  }
}

function goToOrder(order) {
  router.push('/driver/order/' + order.orderId)
}

function editPlate() {
  newPlate.value = driverStore.driverInfo?.vehiclePlate || ''
  plateEditing.value = true
  nextTick(() => {
    plateInput.value?.focus()
  })
}

async function savePlate() {
  if (!newPlate.value.trim()) return
  plateSaving.value = true
  const result = await driverStore.updateVehiclePlate(newPlate.value.trim())
  plateSaving.value = false
  if (result.success) {
    plateEditing.value = false
  } else {
    alert(result.message || 'Failed to update plate')
  }
}

async function handleLogout() {
  if (driverStore.isOnline) {
    await driverStore.goOffline()
  }
  driverStore.stopRealtime()
  authStore.logout()
  router.push('/driver')
}

onMounted(async () => {
  await driverStore.fetchDriverStatus()
  await loadData()
  if (driverStore.isOnline) {
    driverStore.startRealtime(loadData)
  }
})

onUnmounted(() => {
  driverStore.stopRealtime()
})
</script>

<style scoped>
.screen { height: 100%; position: relative; background: #f8f9fb; }
.panel.full { position: absolute; left: 0; right: 0; top: 0; bottom: 0; background: white; padding: 20px 20px 280px 20px; overflow-y: auto; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
h1 { margin: 0; font-size: 24px; color: #111827; font-weight: 800; }
h2 { margin: 0 0 12px; font-size: 18px; color: #111827; font-weight: 700; }
.user-info { display: flex; align-items: center; gap: 12px; font-size: 14px; color: #6b7280; }
.logout-btn { padding: 6px 12px; border: none; border-radius: 8px; background: #fee2e2; color: #dc2626; font-weight: 600; cursor: pointer; font-size: 12px; }
.status-toggle { display: flex; align-items: center; gap: 6px; cursor: pointer; padding: 6px 14px; border-radius: 20px; background: #f1f5f9; transition: all 0.2s; }
.status-toggle:hover { background: #e2e8f0; }
.status-dot { width: 10px; height: 10px; border-radius: 50%; }
.status-dot.online { background: #22c55e; box-shadow: 0 0 6px rgba(34,197,94,0.5); }
.status-dot.offline { background: #9ca3af; }
.status-text { font-size: 13px; font-weight: 600; color: #374151; }

.profile-card { display: flex; align-items: center; gap: 14px; background: linear-gradient(135deg, #1e3a8a, #111827); padding: 16px 18px; border-radius: 18px; margin-bottom: 16px; color: white; }
.profile-avatar { font-size: 36px; }
.profile-main { flex: 1; }
.profile-name { font-size: 17px; font-weight: 700; display: block; }
.profile-phone { margin: 3px 0 0; font-size: 13px; color: rgba(255,255,255,0.7); }
.profile-plate { display: flex; flex-direction: column; align-items: center; background: rgba(255,255,255,0.15); padding: 8px 14px; border-radius: 12px; cursor: pointer; transition: background 0.2s; }
.profile-plate:hover { background: rgba(255,255,255,0.25); }
.plate-label { font-size: 10px; color: rgba(255,255,255,0.6); text-transform: uppercase; letter-spacing: 1px; }
.plate-value { font-size: 14px; font-weight: 700; margin-top: 2px; }
.plate-edit { font-size: 10px; color: rgba(255,255,255,0.5); margin-top: 2px; }

.plate-edit-bar { display: flex; gap: 8px; margin-bottom: 16px; }
.plate-edit-bar input { flex: 1; padding: 10px 14px; border: 2px solid #1e3a8a; border-radius: 12px; font-size: 15px; font-weight: 600; outline: none; }
.plate-edit-bar input:focus { border-color: #3b82f6; }
.save-btn { width: 44px; height: 44px; border: none; border-radius: 12px; background: #22c55e; color: white; font-size: 18px; font-weight: 700; cursor: pointer; }
.cancel-btn { width: 44px; height: 44px; border: none; border-radius: 12px; background: #fee2e2; color: #dc2626; font-size: 16px; cursor: pointer; }

.offline-notice { text-align: center; padding: 40px; }
.offline-notice p { margin: 0; color: #6b7280; font-size: 16px; }
.count { color: #6b7280; font-weight: 400; }
.loading { text-align: center; color: #6b7280; padding: 40px; }
.empty { text-align: center; padding: 40px; }
.empty p { margin: 0; color: #6b7280; }
.hint { font-size: 13px; margin-top: 8px !important; }
.order-list { display: flex; flex-direction: column; gap: 12px; }
.order-card { background: #f8fafc; border-radius: 16px; padding: 16px; cursor: pointer; transition: all 0.2s; border: 2px solid transparent; }
.order-card:hover { background: #fff7ed; border-color: #f59e0b; }
.order-card.active { background: #fff7ed; border-color: #f59e0b; }
.order-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.status-badge { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; }
.status-badge.accepted { background: #dbeafe; color: #1e40af; }
.status-badge.picking_up { background: #d1fae5; color: #065f46; }
.status-badge.in_progress { background: #e0e7ff; color: #3730a3; }
.fare { font-weight: 700; color: #111827; font-size: 18px; }
.distance { color: #6b7280; font-size: 14px; }
.order-route { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; font-size: 14px; color: #374151; }
.dot { width: 10px; height: 10px; border-radius: 50%; }
.dot.green { background: #22c55e; }
.dot.red { background: #ef4444; }
.passenger-info { margin-top: 12px; padding-top: 12px; border-top: 1px solid #e5e7eb; font-size: 13px; color: #6b7280; }
.order-passenger { margin-top: 8px; font-size: 13px; color: #6b7280; }
.accept-btn { width: 100%; height: 44px; border: none; border-radius: 12px; background: linear-gradient(135deg, #f59e0b, #d97706); color: white; font-size: 15px; font-weight: 700; cursor: pointer; margin-top: 12px; }
.nav-links { display: flex; justify-content: center; gap: 24px; margin-top: 20px; }
.nav-links span { color: #f59e0b; font-weight: 600; cursor: pointer; }
</style>
