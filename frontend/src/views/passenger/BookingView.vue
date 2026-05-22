<template>
  <section class="screen">
    <div class="map-wrapper">
      <div class="map" id="booking-map"></div>
      <button class="logout-btn" @click="handleLogout">⏻</button>
      <div class="map-hint" v-if="selecting">Tap map to set {{ selecting === 'pickup' ? 'pickup' : 'destination' }}</div>
      <button class="locate-btn" @click="locateMe">◎</button>
    </div>
    <div class="panel">
      <h1>Where to?</h1>
      <div class="input-card" :class="{ focused: selecting === 'pickup' }" @click="focusInput('pickup')">
        <div class="line green"></div>
        <input ref="pickupInput" v-model="pickupName" placeholder="Search pickup location" @input="searchPlace('pickup')" @focus="selecting = 'pickup'" @blur="onInputBlur" />
      </div>
      <div class="suggestions" v-if="pickupSuggestions.length && selecting === 'pickup'">
        <div v-for="s in pickupSuggestions" :key="s.id" class="suggestion-item" @mousedown.prevent="selectSuggestion('pickup', s)">
          <span class="s-icon"></span>
          <div><strong>{{ s.name }}</strong><p>{{ s.address || s.district }}</p></div>
        </div>
      </div>
      <div class="input-card" :class="{ focused: selecting === 'dest' }" @click="focusInput('dest')">
        <div class="line red"></div>
        <input ref="destInput" v-model="destName" placeholder="Search destination" @input="searchPlace('dest')" @focus="selecting = 'dest'" @blur="onInputBlur" />
      </div>
      <div class="suggestions" v-if="destSuggestions.length && selecting === 'dest'">
        <div v-for="s in destSuggestions" :key="s.id" class="suggestion-item" @mousedown.prevent="selectSuggestion('dest', s)">
          <span class="s-icon"></span>
          <div><strong>{{ s.name }}</strong><p>{{ s.address || s.district }}</p></div>
        </div>
      </div>
      <h3>Recommended</h3>
      <div class="ride-option" :class="{ active: rideType === 'STANDARD' }" @click="rideType = 'STANDARD'">
        <span class="car-icon"></span>
        <div><strong>Economy</strong><p>Affordable everyday rides</p></div>
        <b v-if="pricing">¥{{ pricing.final_fare }}</b>
        <b v-else>--</b>
      </div>
      <div class="ride-option" :class="{ active: rideType === 'PREMIUM' }" @click="rideType = 'PREMIUM'">
        <span class="car-icon"></span>
        <div><strong>Comfort</strong><p>More space and comfort</p></div>
        <b v-if="pricing">¥{{ comfortFare }}</b>
        <b v-else>--</b>
      </div>
      <div class="pricing-detail" v-if="pricing">
        <div class="pricing-row"><span>Base fare</span><span>¥{{ pricing.base_fare }}</span></div>
        <div class="pricing-row"><span>Distance ({{ pricing.distance_km }}km)</span><span>¥{{ pricing.distance_fare }}</span></div>
        <div class="pricing-row"><span>Duration ({{ pricing.duration_minutes }}min)</span><span>¥{{ pricing.duration_fare }}</span></div>
        <div class="pricing-row sub"><span>Subtotal</span><span>¥{{ pricing.subtotal }}</span></div>
        <div class="pricing-row surcharge" v-for="s in pricing.surcharges" :key="s.type + s.reason">
          <span>{{ s.reason }}</span><span>×{{ s.multiplier }}</span>
        </div>
        <div class="pricing-row total" v-if="pricing.surcharges && pricing.surcharges.length > 0">
          <span>Total multiplier</span><span>×{{ pricing.total_multiplier }}</span>
        </div>
        <div class="pricing-row total">
          <span>Estimated fare</span><span>¥{{ pricing.final_fare }}</span>
        </div>
      </div>
      <p class="error-msg" v-if="error">{{ error }}</p>
      <button @click="requestRide" :disabled="loading">
        {{ loading ? 'Requesting...' : 'Request Ride →' }}
      </button>
      <div class="nav-links">
        <span @click="router.push('/orders')">📋 My Orders</span>
        <span @click="router.push('/profile')">👤 Profile</span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useOrderStore } from '@/stores/order'

const router = useRouter()
const authStore = useAuthStore()
const orderStore = useOrderStore()

const pickupName = ref('')
const destName = ref('')
const pickupLat = ref(null)
const pickupLng = ref(null)
const destLat = ref(null)
const destLng = ref(null)
const rideType = ref('STANDARD')
const error = ref('')
const loading = ref(false)
const pricing = ref(null)
let priceTimer = null

const comfortFare = computed(() => {
  if (!pricing.value) return '--'
  return Math.round(pricing.value.final_fare * 1.5 * 100) / 100
})
const selecting = ref('pickup')
const pickupSuggestions = ref([])
const destSuggestions = ref([])
const pickupInput = ref(null)
const destInput = ref(null)

let map = null
let startMarker = null
let endMarker = null
let driving = null
let placeSearch = null
let geocoder = null
let geolocation = null
let searchTimer = null
let myLocation = null

function focusInput(type) {
  selecting.value = type === 'pickup' ? 'pickup' : 'dest'
  const el = type === 'pickup' ? pickupInput.value : destInput.value
  if (el) el.focus()
}

let blurTimer = null
function onInputBlur() {
  clearTimeout(blurTimer)
  blurTimer = setTimeout(() => {
    pickupSuggestions.value = []
    destSuggestions.value = []
  }, 200)
}

function searchPlace(type) {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    const keyword = type === 'pickup' ? pickupName.value : destName.value
    if (!keyword || keyword.length < 2) {
      if (type === 'pickup') pickupSuggestions.value = []
      else destSuggestions.value = []
      return
    }
    if (!placeSearch) return
    placeSearch.search(keyword, function (status, result) {
      if (status === 'complete' && result.poiList) {
        const list = result.poiList.pois.map(p => ({
          id: p.id,
          name: p.name,
          address: p.address,
          district: p.pname + p.cityname + p.adname,
          lat: p.location ? p.location.lat : null,
          lng: p.location ? p.location.lng : null
        }))
        if (type === 'pickup') pickupSuggestions.value = list
        else destSuggestions.value = list
      } else {
        if (type === 'pickup') pickupSuggestions.value = []
        else destSuggestions.value = []
      }
    })
  }, 300)
}

function selectSuggestion(type, s) {
  if (type === 'pickup') {
    pickupName.value = s.name
    pickupLat.value = s.lat
    pickupLng.value = s.lng
    pickupSuggestions.value = []
  } else {
    destName.value = s.name
    destLat.value = s.lat
    destLng.value = s.lng
    destSuggestions.value = []
  }
  updateMap()
  fetchPricing()
}

function updateMap() {
  if (!map) return

  if (startMarker) map.remove(startMarker)
  if (endMarker) map.remove(endMarker)
  if (driving) { driving.clear(); driving = null }
  startMarker = null
  endMarker = null

  if (pickupLat.value && pickupLng.value) {
    startMarker = new AMap.Marker({
      position: [pickupLng.value, pickupLat.value],
      label: { content: ' Pickup', offset: new AMap.Pixel(28, 0) }
    })
    map.add(startMarker)
  }

  if (destLat.value && destLng.value) {
    endMarker = new AMap.Marker({
      position: [destLng.value, destLat.value],
      label: { content: ' Destination', offset: new AMap.Pixel(28, 0) }
    })
    map.add(endMarker)
  }

  if (pickupLat.value && destLat.value && pickupName.value !== destName.value) {
    driving = new AMap.Driving({
      map: map,
      autoFitView: true,
      hideMarkers: true
    })
    driving.search(
      new AMap.LngLat(pickupLng.value, pickupLat.value),
      new AMap.LngLat(destLng.value, destLat.value),
      function (status) {
        if (status === 'complete') map.setFitView()
      }
    )
  } else if (pickupLat.value) {
    map.setCenter([pickupLng.value, pickupLat.value])
    map.setZoom(15)
  }
}

function onMapClick(e) {
  const lng = e.lnglat.getLng()
  const lat = e.lnglat.getLat()

  if (!geocoder) return

  geocoder.getAddress([lng, lat], function (status, result) {
    if (status === 'complete' && result.regeocode) {
      const addr = result.regeocode.formattedAddress
      if (selecting.value === 'pickup') {
        pickupName.value = addr
        pickupLat.value = lat
        pickupLng.value = lng
        pickupSuggestions.value = []
      } else {
        destName.value = addr
        destLat.value = lat
        destLng.value = lng
        destSuggestions.value = []
      }
      updateMap()
      fetchPricing()
    }
  })
}

function locateMe() {
  if (!map || !geolocation) return
  geolocation.getCurrentPosition(function (status, result) {
    if (status === 'complete' && result.position) {
      const pos = result.position
      myLocation = { lat: pos.lat, lng: pos.lng }
      map.setCenter([pos.lng, pos.lat])
      map.setZoom(15)
    }
  })
}

async function fetchPricing() {
  if (priceTimer) clearTimeout(priceTimer)
  if (!pickupLat.value || !pickupLng.value || !destLat.value || !destLng.value) {
    pricing.value = null
    return
  }
  priceTimer = setTimeout(async () => {
    const result = await orderStore.estimatePrice(
      pickupLat.value, pickupLng.value,
      destLat.value, destLng.value,
      pickupName.value, destName.value
    )
    if (result.success) {
      pricing.value = result.pricing
    }
  }, 500)
}

async function requestRide() {
  error.value = ''

  if (!pickupLat.value || !pickupLng.value) {
    error.value = 'Please select a pickup location'
    return
  }
  if (!destLat.value || !destLng.value) {
    error.value = 'Please select a destination'
    return
  }
  if (pickupName.value === destName.value) {
    error.value = 'Pickup and destination cannot be the same'
    return
  }

  loading.value = true
  const result = await orderStore.createOrder(
    pickupName.value, pickupLat.value, pickupLng.value,
    destName.value, destLat.value, destLng.value
  )
  loading.value = false

  if (result.success) {
    router.push('/tracking/' + result.order.orderId)
  } else {
    error.value = result.message
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/')
}

onMounted(() => {
  if (typeof AMap === 'undefined') return

  map = new AMap.Map('booking-map', {
    zoom: 13,
    center: [120.7, 27.95],
    resizeEnable: true,
    dragEnable: true,
    zoomEnable: true,
    doubleClickZoom: true,
    touchZoom: true
  })

  map.on('click', onMapClick)

  AMap.plugin(['AMap.PlaceSearch', 'AMap.Geocoder', 'AMap.Geolocation'], function () {
    placeSearch = new AMap.PlaceSearch({ city: '温州', pageSize: 6 })
    geocoder = new AMap.Geocoder()

    geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000,
      zoomToAccuracy: true,
      showButton: false,
      showMarker: true,
      markerOptions: {
        content: '<div style="width:14px;height:14px;background:#1a73e8;border:3px solid white;border-radius:50%;box-shadow:0 0 6px rgba(0,0,0,0.3);"></div>',
        offset: new AMap.Pixel(-7, -7)
      },
      showCircle: true,
      circleOptions: {
        strokeColor: '#1a73e8',
        strokeOpacity: 0.3,
        strokeWeight: 1,
        fillColor: '#1a73e8',
        fillOpacity: 0.1
      }
    })
    map.addControl(geolocation)

    geolocation.getCurrentPosition(function (status, result) {
      if (status === 'complete' && result.position) {
        myLocation = { lat: result.position.lat, lng: result.position.lng }
      }
    })
  })
})

onUnmounted(() => {
  if (searchTimer) clearTimeout(searchTimer)
  if (blurTimer) clearTimeout(blurTimer)
  if (priceTimer) clearTimeout(priceTimer)
  if (driving) { driving.clear(); driving = null }
  if (map) {
    map.off('click', onMapClick)
    map.destroy()
  }
})
</script>

<style scoped>
.screen { height: 100%; position: relative; overflow-y: auto; padding-bottom: 200px; }
.map-wrapper { position: relative; flex-shrink: 0; }
.map { height: 330px; }
.logout-btn { position: absolute; top: 10px; right: 10px; width: 30px; height: 30px; border: none; border-radius: 50%; background: rgba(0,0,0,0.55); color: white; font-size: 14px; cursor: pointer; z-index: 10; display: flex; align-items: center; justify-content: center; }
.logout-btn:hover { background: rgba(220,38,38,0.8); }
.map-hint { position: absolute; top: 10px; left: 50%; transform: translateX(-50%); background: rgba(0,0,0,0.55); color: white; padding: 4px 12px; border-radius: 14px; font-size: 11px; font-weight: 600; z-index: 10; pointer-events: none; }
.locate-btn { position: absolute; bottom: 10px; right: 10px; width: 34px; height: 34px; border: none; border-radius: 50%; background: white; box-shadow: 0 2px 8px rgba(0,0,0,0.15); font-size: 18px; color: #1a73e8; cursor: pointer; z-index: 10; display: flex; align-items: center; justify-content: center; }
.locate-btn:hover { background: #f0f7ff; }
.panel { background: rgba(255,255,255,0.88); backdrop-filter: blur(18px); padding: 26px; border-radius: 32px 32px 0 0; box-shadow: 0 -10px 35px rgba(0,0,0,0.08); }
h1 { margin: 0 0 4px; font-size: 28px; color: #111827; font-weight: 800; }
.input-card { display: flex; align-items: center; gap: 12px; background: white; padding: 14px; border-radius: 18px; margin-bottom: 4px; border: 2px solid #edf0f5; cursor: pointer; transition: border-color 0.2s; }
.input-card.focused { border-color: #1a73e8; }
.line { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; }
.green { background: #22c55e; }
.red { background: #ef4444; }
input { border: none; outline: none; background: transparent; width: 100%; font-size: 15px; color: #111827; font-weight: 600; }
input::placeholder { color: #9ca3af; font-weight: 400; }
.suggestions { background: white; border-radius: 16px; margin-bottom: 8px; max-height: 180px; overflow-y: auto; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.suggestion-item { display: flex; align-items: center; gap: 10px; padding: 12px 14px; cursor: pointer; border-bottom: 1px solid #f3f4f6; }
.suggestion-item:last-child { border-bottom: none; }
.suggestion-item:hover { background: #f0f7ff; }
.s-icon { font-size: 16px; }
.suggestion-item strong { font-size: 14px; color: #111827; }
.suggestion-item p { margin: 2px 0 0; font-size: 12px; color: #9ca3af; }
h3 { color: #111827; font-size: 20px; margin-bottom: 14px; }
.ride-option { display: flex; align-items: center; gap: 14px; padding: 14px; border-radius: 20px; background: #f8fafc; margin-bottom: 12px; border: 2px solid transparent; cursor: pointer; }
.ride-option.active { border-color: #1a73e8; background: #eef5ff; }
.ride-option p { margin: 4px 0 0; color: #4b5563; font-size: 13px; }
.ride-option strong { color: #111827; font-size: 18px; }
.ride-option b { color: #111827; font-size: 20px; margin-left: auto; }
.car-icon { font-size: 30px; }
button { width: 100%; height: 54px; border: none; border-radius: 18px; background: linear-gradient(135deg, #111827, #1e3a8a); color: white; font-size: 16px; font-weight: 700; cursor: pointer; margin-top: 14px; }
button:disabled { opacity: 0.6; cursor: not-allowed; }
.error-msg { color: #dc2626; font-size: 13px; margin: 4px 0; text-align: center; }
.nav-links { display: flex; justify-content: space-between; margin-top: 16px; }
.nav-links span { color: #1a73e8; font-weight: 600; cursor: pointer; font-size: 14px; }
.pricing-detail { background: #f8fafc; border-radius: 16px; padding: 14px 16px; margin-top: 12px; }
.pricing-row { display: flex; justify-content: space-between; align-items: center; font-size: 13px; color: #6b7280; padding: 3px 0; }
.pricing-row.sub { border-top: 1px solid #e5e7eb; margin-top: 6px; padding-top: 6px; color: #374151; font-weight: 600; }
.pricing-row.surcharge { color: #f59e0b; font-size: 12px; }
.pricing-row.total { border-top: 1px solid #e5e7eb; margin-top: 6px; padding-top: 6px; color: #111827; font-weight: 700; font-size: 15px; }
</style>
