<template>
  <section class="screen">
    <div class="panel full">
      <div class="header">
        <button class="back-btn" @click="router.push('/driver/home')">← Back</button>
        <h1>Order Details</h1>
      </div>

      <div v-if="loading" class="loading">Loading...</div>

      <div v-else-if="!order" class="empty">
        <p>Order not found</p>
      </div>

      <div v-else class="order-detail">
        <div class="status-section">
          <span class="status-badge large" :class="order.status.toLowerCase()">{{ statusText(order.status) }}</span>
          <p class="fare">¥{{ order.actualFare || order.estimatedFare || '--' }}</p>
        </div>

        <div class="map-container">
          <div id="driver-order-map" class="order-map"></div>
        </div>

        <div class="route-section">
          <div class="route-item">
            <span class="dot green"></span>
            <div>
              <span class="label">Pickup</span>
              <span class="value">{{ order.pickupName || '--' }}</span>
            </div>
          </div>
          <div class="route-line"></div>
          <div class="route-item">
            <span class="dot red"></span>
            <div>
              <span class="label">Destination</span>
              <span class="value">{{ order.destName || '--' }}</span>
            </div>
          </div>
        </div>

        <div class="info-section">
          <div class="info-item">
            <span class="icon">👤</span>
            <span>{{ order.passengerName || 'Passenger' }}</span>
          </div>
          <div class="info-item">
            <span class="icon">☎</span>
            <span>{{ maskPhone(order.passengerPhone) }}</span>
            <button v-if="order.passengerPhone" class="call-btn" @click="callPassenger">Call</button>
          </div>
          <div class="info-item">
            <span class="icon">📏</span>
            <span>{{ order.distance ? order.distance + ' km' : '--' }}</span>
          </div>
        </div>

        <div class="action-section">
          <button v-if="order.status === 'ACCEPTED'" class="action-btn arrive" @click="handleArrive" :disabled="actionLoading">
            {{ actionLoading ? 'Processing...' : ' Arrive at Pickup' }}
          </button>
          <button v-if="order.status === 'PICKING_UP'" class="action-btn start" @click="showVerifyModal = true">
             Start Trip
          </button>
          <button v-if="order.status === 'IN_PROGRESS'" class="action-btn complete" @click="handleComplete" :disabled="actionLoading">
            {{ actionLoading ? 'Processing...' : ' Complete Trip' }}
          </button>
        </div>

        <p class="error-msg" v-if="error">{{ error }}</p>
      </div>
    </div>

    <Teleport to="body">
      <div class="verify-overlay" v-if="showVerifyModal" @click.self="showVerifyModal = false">
        <div class="verify-modal">
          <button class="cancel-trip-btn" @click="showCancelConfirm = true">✕ Close Trip</button>

          <div class="verify-icon">🔐</div>
          <h2>Verify Passenger</h2>
          <p class="verify-hint">Enter the last 4 digits of the passenger's phone number to start the trip</p>

          <div class="verify-phone-hint">
            ☎ Passenger phone: <strong>{{ maskPhone(order?.passengerPhone) }}</strong>
          </div>

          <div class="verify-input-row">
            <input
              v-for="i in 4"
              :key="i"
              :ref="el => digitRefs[i-1] = el"
              class="digit-input"
              type="text"
              maxlength="1"
              inputmode="numeric"
              v-model="digits[i-1]"
              @input="onDigitInput(i-1)"
              @keydown.backspace="onDigitBackspace(i-1, $event)"
            />
          </div>

          <p class="verify-error" v-if="verifyError">{{ verifyError }}</p>

          <button class="verify-btn" @click="handleVerify" :disabled="verifyLoading">
            {{ verifyLoading ? 'Verifying...' : '✓ Verify & Start Trip' }}
          </button>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div class="verify-overlay" v-if="showCancelConfirm" @click.self="showCancelConfirm = false">
        <div class="confirm-modal">
          <div class="confirm-icon">⚠️</div>
          <h2>Close Trip?</h2>
          <p class="confirm-hint">This will cancel the current order. The passenger will be notified. This action cannot be undone.</p>
          <div class="confirm-btns">
            <button class="confirm-no" @click="showCancelConfirm = false">Keep Order</button>
            <button class="confirm-yes" @click="handleCancelTrip" :disabled="cancelLoading">
              {{ cancelLoading ? 'Closing...' : 'Yes, Close Trip' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDriverStore } from '@/stores/driver'
import { useWebSocket } from '@/composables/useWebSocket'

const route = useRoute()
const router = useRouter()
const driverStore = useDriverStore()
const { connect: wsConnect, disconnect: wsDisconnect, on: wsOn, off: wsOff } = useWebSocket()

const order = ref(null)
const loading = ref(true)
const actionLoading = ref(false)
const error = ref('')
const pollTimer = ref(null)

let map = null
let driving = null
let pickupMarker = null
let destMarker = null
let driverMarker = null
let lastDrawnKey = ''

const showVerifyModal = ref(false)
const digits = ref(['', '', '', ''])
const digitRefs = ref([])
const verifyError = ref('')
const verifyLoading = ref(false)

const showCancelConfirm = ref(false)
const cancelLoading = ref(false)

function maskPhone(phone) {
  if (!phone) return '--'
  const s = String(phone)
  if (s.length >= 7) return s.slice(0, 3) + '****' + s.slice(-4)
  return s
}

function getPhoneLast4(phone) {
  if (!phone) return ''
  const s = String(phone)
  return s.length >= 4 ? s.slice(-4) : s
}

const statusMap = {
  PENDING: 'Pending',
  ACCEPTED: 'Accepted',
  PICKING_UP: 'Arrived at Pickup',
  IN_PROGRESS: 'In Progress',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled'
}

function statusText(status) {
  return statusMap[status] || status
}

function onDigitInput(index) {
  const val = digits.value[index]
  digits.value[index] = val.replace(/\D/g, '').slice(0, 1)
  if (digits.value[index] && index < 3) {
    digitRefs.value[index + 1]?.focus()
  }
}

function onDigitBackspace(index, event) {
  if (!digits.value[index] && index > 0) {
    digits.value[index - 1] = ''
    digitRefs.value[index - 1]?.focus()
  }
}

async function handleVerify() {
  const inputCode = digits.value.join('')
  if (inputCode.length !== 4) {
    verifyError.value = 'Please enter all 4 digits'
    return
  }

  const expected = getPhoneLast4(order.value?.passengerPhone)
  if (inputCode !== expected) {
    verifyError.value = 'Incorrect code. Please check with the passenger.'
    digits.value = ['', '', '', '']
    nextTick(() => digitRefs.value[0]?.focus())
    return
  }

  verifyError.value = ''
  verifyLoading.value = true
  const result = await driverStore.startTrip(order.value.orderId)
  verifyLoading.value = false

  if (result.success) {
    showVerifyModal.value = false
    digits.value = ['', '', '', '']
    order.value = driverStore.currentOrder
    lastDrawnKey = ''
    drawRoute()
  } else {
    verifyError.value = result.message || 'Failed to start trip'
  }
}

async function handleCancelTrip() {
  cancelLoading.value = true
  const result = await driverStore.cancelOrder(order.value.orderId, '司机验证失败，关闭行程')
  cancelLoading.value = false
  if (result.success) {
    showCancelConfirm.value = false
    showVerifyModal.value = false
    digits.value = ['', '', '', '']
    driverStore.clearCurrentOrder()
    router.replace('/driver/home')
  } else {
    verifyError.value = result.message || 'Failed to cancel order'
    showCancelConfirm.value = false
  }
}

function initMap() {
  if (!order.value || typeof AMap === 'undefined') return

  const pLat = order.value.pickupLat
  const pLng = order.value.pickupLng
  const dLat = order.value.destLat
  const dLng = order.value.destLng

  if (!pLat || !pLng || !dLat || !dLng) return

  nextTick(() => {
    const container = document.getElementById('driver-order-map')
    if (!container) return

    if (map) {
      drawRoute()
      return
    }

    map = new AMap.Map('driver-order-map', {
      zoom: 13,
      center: [pLng, pLat],
      resizeEnable: true,
      dragEnable: true,
      zoomEnable: true,
      doubleClickZoom: true,
      touchZoom: true
    })

    drawRoute()
  })
}

function drawRoute() {
  if (!map || !order.value) return

  const pLat = order.value.pickupLat
  const pLng = order.value.pickupLng
  const dLat = order.value.destLat
  const dLng = order.value.destLng

  if (!pLat || !pLng || !dLat || !dLng) return

  const key = `${pLat},${pLng}-${dLat},${dLng}-${order.value.status}`
  if (key === lastDrawnKey) return
  lastDrawnKey = key

  if (pickupMarker) map.remove(pickupMarker)
  if (destMarker) map.remove(destMarker)
  if (driverMarker) map.remove(driverMarker)
  if (driving) driving.clear()

  pickupMarker = new AMap.Marker({
    position: [pLng, pLat],
    label: { content: ' Pickup', offset: new AMap.Pixel(20, -4) }
  })
  destMarker = new AMap.Marker({
    position: [dLng, dLat],
    label: { content: ' Destination', offset: new AMap.Pixel(20, -4) }
  })
  map.add([pickupMarker, destMarker])

  driverMarker = new AMap.Marker({
    position: [pLng, pLat],
    icon: new AMap.Icon({
      size: new AMap.Size(30, 30),
      image: '//a.amap.com/jsapi_demos/static/demo-center/icons/poi-icon-default.png',
      imageSize: new AMap.Size(20, 20),
      imageOffset: new AMap.Pixel(-5, -5)
    }),
    label: { content: '🚗', offset: new AMap.Pixel(-2, -22) },
    zIndex: 120
  })
  map.add(driverMarker)

  if (order.value.status === 'IN_PROGRESS') {
    driverMarker.setPosition([dLng, dLat])
  }

  driving = new AMap.Driving({ map: map, autoFitView: true, hideMarkers: true })
  driving.search(
    new AMap.LngLat(pLng, pLat),
    new AMap.LngLat(dLng, dLat),
    function (status) {
      if (status === 'complete') {
        map.setFitView()
      }
    }
  )
}

async function loadOrder() {
  const orderId = route.params.id
  if (!orderId) return

  const result = await driverStore.fetchCurrentOrder()
  if (result && result.orderId == orderId) {
    order.value = result
    if (loading.value) {
      loading.value = false
      nextTick(() => initMap())
    } else {
      drawRoute()
    }
  } else {
    loading.value = false
    order.value = null
    router.replace('/driver/home')
  }
}

function callPassenger() {
  if (order.value?.passengerPhone) {
    window.open('tel:' + order.value.passengerPhone)
  }
}

async function handleArrive() {
  if (!order.value) return
  actionLoading.value = true
  error.value = ''
  const result = await driverStore.arrivePickup(order.value.orderId)
  actionLoading.value = false
  if (result.success) {
    order.value = driverStore.currentOrder
    lastDrawnKey = ''
    drawRoute()
  } else {
    error.value = result.message
  }
}

async function handleComplete() {
  if (!order.value) return
  actionLoading.value = true
  error.value = ''

  if (driverMarker && map && order.value.destLat && order.value.destLng) {
    driverMarker.setPosition([order.value.destLng, order.value.destLat])
    map.setCenter([order.value.destLng, order.value.destLat])
  }

  const result = await driverStore.completeTrip(order.value.orderId)
  actionLoading.value = false
  if (result.success) {
    driverStore.clearCurrentOrder()
    router.replace('/driver/home')
  } else {
    error.value = result.message
  }
}

onMounted(async () => {
  await loadOrder()
  pollTimer.value = setInterval(loadOrder, 5000)

  wsConnect()
  wsOn('order:ARRIVED', () => loadOrder())
  wsOn('order:STARTED', () => loadOrder())
  wsOn('order:COMPLETED', () => loadOrder())
  wsOn('order:CANCELLED', () => loadOrder())
})

onUnmounted(() => {
  if (pollTimer.value) clearInterval(pollTimer.value)
  wsDisconnect()
  if (driving) driving.clear()
  if (map) {
    map.destroy()
    map = null
  }
  lastDrawnKey = ''
})
</script>

<style scoped>
.screen { height: 100%; position: relative; background: #f8f9fb; }
.panel.full { position: absolute; left: 0; right: 0; top: 0; bottom: 0; background: white; padding: 20px 20px 280px 20px; overflow-y: auto; }
.header { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.back-btn { padding: 8px 16px; border: none; border-radius: 8px; background: #f3f4f6; color: #374151; font-weight: 600; cursor: pointer; }
h1 { margin: 0; font-size: 20px; color: #111827; font-weight: 800; }
.loading { text-align: center; color: #6b7280; padding: 40px; }
.empty { text-align: center; padding: 40px; color: #6b7280; }
.map-container { margin-bottom: 16px; border-radius: 16px; overflow: hidden; }
.order-map { height: 260px; width: 100%; }
.status-section { text-align: center; padding: 16px; background: #f8fafc; border-radius: 16px; margin-bottom: 16px; }
.status-badge { padding: 8px 20px; border-radius: 20px; font-size: 14px; font-weight: 600; }
.status-badge.large { font-size: 16px; padding: 10px 24px; }
.status-badge.accepted { background: #dbeafe; color: #1e40af; }
.status-badge.picking_up { background: #d1fae5; color: #065f46; }
.status-badge.in_progress { background: #e0e7ff; color: #3730a3; }
.status-badge.completed { background: #d1fae5; color: #065f46; }
.status-badge.cancelled { background: #fee2e2; color: #991b1b; }
.fare { margin: 8px 0 0; font-size: 28px; font-weight: 800; color: #111827; }
.route-section { background: #f8fafc; border-radius: 16px; padding: 16px; margin-bottom: 16px; }
.route-item { display: flex; align-items: flex-start; gap: 12px; }
.route-item .label { display: block; font-size: 12px; color: #6b7280; margin-bottom: 4px; }
.route-item .value { font-size: 15px; font-weight: 600; color: #111827; }
.route-line { width: 2px; height: 20px; background: #e5e7eb; margin-left: 4px; }
.dot { width: 12px; height: 12px; border-radius: 50%; margin-top: 4px; }
.dot.green { background: #22c55e; }
.dot.red { background: #ef4444; }
.info-section { background: #f8fafc; border-radius: 16px; padding: 16px; margin-bottom: 16px; }
.info-item { display: flex; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px solid #e5e7eb; }
.info-item:last-child { border-bottom: none; }
.icon { font-size: 18px; }
.call-btn { margin-left: auto; padding: 6px 12px; border: none; border-radius: 8px; background: #22c55e; color: white; font-weight: 600; cursor: pointer; font-size: 12px; }
.action-section { margin-top: 16px; }
.action-btn { width: 100%; height: 52px; border: none; border-radius: 14px; font-size: 17px; font-weight: 700; cursor: pointer; }
.action-btn.arrive { background: linear-gradient(135deg, #3b82f6, #1d4ed8); color: white; }
.action-btn.start { background: linear-gradient(135deg, #22c55e, #16a34a); color: white; }
.action-btn.complete { background: linear-gradient(135deg, #f59e0b, #d97706); color: white; }
.action-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.error-msg { color: #dc2626; font-size: 13px; margin-top: 12px; text-align: center; }

.verify-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  backdrop-filter: blur(4px);
}

.verify-modal {
  background: white;
  border-radius: 28px;
  padding: 32px 28px 28px;
  width: 340px;
  position: relative;
  text-align: center;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.2);
}

.cancel-trip-btn {
  position: absolute;
  top: 16px;
  left: 16px;
  padding: 6px 14px;
  border: none;
  border-radius: 8px;
  background: #fee2e2;
  color: #dc2626;
  font-weight: 700;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.cancel-trip-btn:hover { background: #fecaca; }

.verify-icon { font-size: 48px; margin-bottom: 8px; }
.verify-modal h2 { margin: 0 0 8px; font-size: 22px; color: #111827; font-weight: 800; }
.verify-hint { color: #6b7280; font-size: 14px; margin: 0 0 20px; line-height: 1.5; }

.verify-phone-hint {
  background: #f0f9ff;
  padding: 10px 16px;
  border-radius: 12px;
  font-size: 14px;
  color: #1e40af;
  margin-bottom: 20px;
}
.verify-phone-hint strong { font-size: 16px; }

.verify-input-row {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}
.digit-input {
  width: 56px;
  height: 64px;
  text-align: center;
  font-size: 28px;
  font-weight: 800;
  border: 2px solid #d1d5db;
  border-radius: 14px;
  outline: none;
  transition: border-color 0.2s;
  color: #111827;
}
.digit-input:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15); }

.verify-error { color: #dc2626; font-size: 13px; margin: 0 0 12px; font-weight: 600; }

.verify-btn {
  width: 100%;
  height: 52px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: white;
  font-size: 17px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.2s;
}
.verify-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.confirm-modal {
  background: white;
  border-radius: 28px;
  padding: 32px 28px 28px;
  width: 320px;
  text-align: center;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.2);
}
.confirm-icon { font-size: 48px; margin-bottom: 8px; }
.confirm-modal h2 { margin: 0 0 8px; font-size: 22px; color: #111827; font-weight: 800; }
.confirm-hint { color: #6b7280; font-size: 14px; margin: 0 0 24px; line-height: 1.5; }
.confirm-btns { display: flex; gap: 12px; }
.confirm-no {
  flex: 1;
  height: 48px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  background: white;
  color: #374151;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}
.confirm-yes {
  flex: 1;
  height: 48px;
  border: none;
  border-radius: 12px;
  background: #dc2626;
  color: white;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}
.confirm-yes:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
