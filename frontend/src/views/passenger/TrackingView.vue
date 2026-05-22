<template>
  <section class="screen">
    <div class="tracking-map" id="tracking-map"></div>
    <div class="panel">
      <h1>{{ trackingTitle }}</h1>
      <p class="sub">{{ trackingSub }}</p>
      <div class="driver-card" v-if="order?.driverName">
        <div class="avatar">👨‍✈️</div>
        <div>
          <strong>{{ order.driverName }}</strong>
          <p>{{ order.vehiclePlate || '--' }}</p>
          <p>{{ maskPhone(order.driverPhone) }}</p>
        </div>
        <button class="small-btn" v-if="order.driverPhone" @click="callDriver">☎</button>
      </div>
      <div class="trip-box">
        <div><span>Distance</span><strong>{{ order?.distance ? order.distance + ' km' : '--' }}</strong></div>
        <div><span>Status</span><strong>{{ statusLabel }}</strong></div>
        <div><span>Fare</span><strong>¥{{ order?.estimatedFare || '--' }}</strong></div>
      </div>
      <p class="error-msg" v-if="error">{{ error }}</p>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOrderStore } from '@/stores/order'

const route = useRoute()
const router = useRouter()
const orderStore = useOrderStore()

const order = ref(null)
const error = ref('')
let map = null
let driving = null
let startMarker = null
let endMarker = null
let lastDrawnKey = ''

function maskPhone(phone) {
  if (!phone) return '--'
  const s = String(phone)
  if (s.length >= 7) return s.slice(0, 3) + '****' + s.slice(-4)
  return s
}

const statusLabel = computed(() => {
  const m = { PENDING: 'Waiting', ACCEPTED: 'Accepted', PICKING_UP: 'Arrived', IN_PROGRESS: 'In Progress', COMPLETED: 'Done', CANCELLED: 'Cancelled' }
  return m[order.value?.status] || '--'
})

const trackingTitle = computed(() => {
  const m = {
    PENDING: 'Looking for a driver...',
    ACCEPTED: 'Driver is on the way',
    PICKING_UP: 'Driver has arrived',
    IN_PROGRESS: 'On the way to destination',
    COMPLETED: 'Trip completed',
    CANCELLED: 'Trip cancelled'
  }
  return m[order.value?.status] || 'Processing...'
})

const trackingSub = computed(() => {
  if (order.value?.status === 'PENDING') return 'Please wait while we find a nearby driver'
  if (order.value?.status === 'ACCEPTED') return 'Your driver is heading to the pickup point'
  if (order.value?.status === 'PICKING_UP') return 'Please board the vehicle'
  if (order.value?.status === 'IN_PROGRESS') return 'Enjoy your ride!'
  if (order.value?.status === 'CANCELLED') return 'The driver has cancelled this trip'
  return ''
})

function callDriver() {
  if (order.value?.driverPhone) {
    window.open('tel:' + order.value.driverPhone)
  }
}

function drawRoute(forceFitView) {
  if (!map || !order.value) return
  const o = order.value
  if (!o.pickupLat || !o.destLat) return

  const key = o.pickupLat + ',' + o.pickupLng + ',' + o.destLat + ',' + o.destLng
  if (key === lastDrawnKey && !forceFitView) return
  lastDrawnKey = key

  if (startMarker) map.remove(startMarker)
  if (endMarker) map.remove(endMarker)
  if (driving) { driving.clear(); driving = null }
  startMarker = null
  endMarker = null

  startMarker = new AMap.Marker({
    position: [o.pickupLng, o.pickupLat],
    label: { content: ' Pickup', offset: new AMap.Pixel(28, 0) }
  })
  endMarker = new AMap.Marker({
    position: [o.destLng, o.destLat],
    label: { content: ' Destination', offset: new AMap.Pixel(28, 0) }
  })
  map.add([startMarker, endMarker])

  driving = new AMap.Driving({ map: map, autoFitView: false, hideMarkers: true })
  driving.search(
    new AMap.LngLat(o.pickupLng, o.pickupLat),
    new AMap.LngLat(o.destLng, o.destLat),
    function (status) {
      if (status === 'complete' && forceFitView) {
        map.setFitView()
      }
    }
  )
}

function onOrderUpdate(updatedOrder) {
  order.value = updatedOrder
  drawRoute(false)
}

function onOrderComplete(completedOrder) {
  order.value = completedOrder
  router.push('/complete/' + completedOrder.orderId)
}

function onOrderCancel(cancelledOrder) {
  order.value = cancelledOrder
  setTimeout(() => {
    router.replace('/booking')
  }, 3000)
}

onMounted(async () => {
  const orderId = route.params.id
  if (orderId) {
    const fetchedOrder = await orderStore.fetchOrder(orderId)
    if (fetchedOrder) {
      order.value = fetchedOrder

      if (typeof AMap !== 'undefined' && fetchedOrder.pickupLat) {
        map = new AMap.Map('tracking-map', {
          zoom: 13,
          center: [fetchedOrder.pickupLng, fetchedOrder.pickupLat],
          resizeEnable: true,
          dragEnable: true,
          zoomEnable: true,
          doubleClickZoom: true,
          touchZoom: true
        })
        drawRoute(true)
      }

      if (fetchedOrder.status !== 'COMPLETED' && fetchedOrder.status !== 'CANCELLED') {
        orderStore.startRealtime(orderId, onOrderUpdate, onOrderComplete, onOrderCancel)
      } else if (fetchedOrder.status === 'COMPLETED') {
        router.push('/complete/' + orderId)
      }
    } else {
      error.value = 'Order not found'
    }
  }
})

onUnmounted(() => {
  orderStore.stopRealtime()
  if (driving) { driving.clear(); driving = null }
  if (map) map.destroy()
})
</script>

<style scoped>
.screen { height: 100%; position: relative; overflow-y: auto; padding-bottom: 200px; }
.tracking-map { height: 420px; flex-shrink: 0; }
.panel { background: rgba(255,255,255,0.88); backdrop-filter: blur(18px); padding: 26px; border-radius: 32px 32px 0 0; box-shadow: 0 -10px 35px rgba(0,0,0,0.08); }
h1 { margin: 0; font-size: 28px; color: #111827; font-weight: 800; }
.sub { color: #4b5563; font-weight: 500; margin-top: 6px; margin-bottom: 20px; }
.driver-card { display: flex; align-items: center; gap: 14px; background: #f8fafc; padding: 16px; border-radius: 22px; margin-bottom: 16px; }
.avatar { font-size: 36px; }
.driver-card p { margin: 4px 0 0; color: #6b7280; font-size: 13px; }
.small-btn { width: 44px; height: 44px; margin: 0 0 0 auto; border-radius: 50%; background: white; color: #111827; border: none; box-shadow: 0 6px 18px rgba(0,0,0,0.08); cursor: pointer; font-size: 18px; }
.trip-box { display: grid; grid-template-columns: repeat(3, 1fr); background: #f8fafc; border-radius: 20px; overflow: hidden; }
.trip-box div { padding: 16px 8px; text-align: center; border-right: 1px solid #e5e7eb; }
.trip-box div:last-child { border-right: none; }
.trip-box span { display: block; color: #6b7280; font-size: 12px; margin-bottom: 6px; }
.trip-box strong { color: #111827; font-size: 16px; }
.error-msg { color: #dc2626; font-size: 13px; margin: 4px 0; text-align: center; }
</style>
