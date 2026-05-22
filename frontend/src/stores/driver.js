import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAuthStore } from './auth'
import { useWebSocket } from '@/composables/useWebSocket'

export const useDriverStore = defineStore('driver', () => {
  const pendingOrders = ref([])
  const currentOrder = ref(null)
  const orderHistory = ref([])
  const pollTimer = ref(null)
  const isOnline = ref(false)
  const driverInfo = ref(null)

  const { connect: wsConnect, disconnect: wsDisconnect, on: wsOn, off: wsOff, connected: wsConnected } = useWebSocket()

  async function fetchDriverInfo() {
    const auth = useAuthStore()
    try {
      const resp = await fetch('/api/ride/driver/me', {
        headers: auth.authHeaders
      })
      const data = await resp.json()
      if (data.code === 200) {
        driverInfo.value = data.data
        if (data.data.isOnline !== undefined) {
          isOnline.value = data.data.isOnline
        }
        return data.data
      }
      return null
    } catch (e) {
      return null
    }
  }

  async function updateVehiclePlate(plate) {
    const auth = useAuthStore()
    try {
      const params = new URLSearchParams({ vehiclePlate: plate })
      const resp = await fetch('/api/ride/driver/vehicle-plate?' + params, {
        method: 'PUT',
        headers: auth.authHeaders
      })
      const data = await resp.json()
      if (data.code === 200) {
        if (driverInfo.value) driverInfo.value.vehiclePlate = plate
        return { success: true }
      }
      return { success: false, message: data.message }
    } catch (e) {
      return { success: false, message: 'Network error' }
    }
  }

  async function updateProfile(profileData) {
    const auth = useAuthStore()
    try {
      const resp = await fetch('/api/ride/driver/profile', {
        method: 'PUT',
        headers: auth.authHeaders,
        body: JSON.stringify(profileData)
      })
      const data = await resp.json()
      if (data.code === 200) {
        driverInfo.value = data.data
        if (data.data.name) authStore.setAuth(authStore.token, authStore.userId, data.data.name, authStore.userRole)
        return { success: true }
      }
      return { success: false, message: data.message }
    } catch (e) {
      return { success: false, message: 'Network error' }
    }
  }

  async function fetchPendingOrders() {
    const auth = useAuthStore()
    try {
      const resp = await fetch('/api/ride/order/pending', {
        headers: auth.authHeaders
      })
      const data = await resp.json()
      if (data.code === 200) {
        pendingOrders.value = data.data || []
        return data.data
      }
      return []
    } catch (e) {
      return []
    }
  }

  async function acceptOrder(orderId) {
    const auth = useAuthStore()
    try {
      const resp = await fetch('/api/ride/order/' + orderId + '/accept', {
        method: 'PUT',
        headers: auth.authHeaders
      })
      const data = await resp.json()
      if (data.code === 200) {
        currentOrder.value = data.data
        return { success: true, order: data.data }
      }
      return { success: false, message: data.message }
    } catch (e) {
      return { success: false, message: 'Network error' }
    }
  }

  async function arrivePickup(orderId) {
    const auth = useAuthStore()
    try {
      const resp = await fetch('/api/ride/order/' + orderId + '/arrive', {
        method: 'PUT',
        headers: auth.authHeaders
      })
      const data = await resp.json()
      if (data.code === 200) {
        await fetchCurrentOrder()
        return { success: true }
      }
      return { success: false, message: data.message }
    } catch (e) {
      return { success: false, message: 'Network error' }
    }
  }

  async function startTrip(orderId) {
    const auth = useAuthStore()
    try {
      const resp = await fetch('/api/ride/order/' + orderId + '/start', {
        method: 'PUT',
        headers: auth.authHeaders
      })
      const data = await resp.json()
      if (data.code === 200) {
        await fetchCurrentOrder()
        return { success: true }
      }
      return { success: false, message: data.message }
    } catch (e) {
      return { success: false, message: 'Network error' }
    }
  }

  async function completeTrip(orderId) {
    const auth = useAuthStore()
    try {
      const resp = await fetch('/api/ride/order/' + orderId + '/complete', {
        method: 'PUT',
        headers: auth.authHeaders
      })
      const data = await resp.json()
      if (data.code === 200) {
        await fetchCurrentOrder()
        return { success: true }
      }
      return { success: false, message: data.message }
    } catch (e) {
      return { success: false, message: 'Network error' }
    }
  }

  async function cancelOrder(orderId, reason) {
    const auth = useAuthStore()
    try {
      const params = new URLSearchParams()
      if (reason) params.append('reason', reason)
      const resp = await fetch('/api/ride/order/' + orderId + '/cancel?' + params, {
        method: 'PUT',
        headers: auth.authHeaders
      })
      const data = await resp.json()
      if (data.code === 200) {
        currentOrder.value = null
        return { success: true }
      }
      return { success: false, message: data.message }
    } catch (e) {
      return { success: false, message: 'Network error' }
    }
  }

  async function fetchCurrentOrder() {
    const auth = useAuthStore()
    try {
      const resp = await fetch('/api/ride/order/my', {
        headers: auth.authHeaders
      })
      const data = await resp.json()
      if (data.code === 200) {
        const orders = data.data || []
        const activeOrders = orders.filter(o => ['ACCEPTED', 'PICKING_UP', 'IN_PROGRESS'].includes(o.status))
        currentOrder.value = activeOrders.length > 0 ? activeOrders[0] : null
        return currentOrder.value
      }
      currentOrder.value = null
      return null
    } catch (e) {
      return null
    }
  }

  async function goOnline() {
    const auth = useAuthStore()
    try {
      const resp = await fetch('/api/ride/driver/online', {
        method: 'PUT',
        headers: auth.authHeaders
      })
      const data = await resp.json()
      if (data.code === 200) {
        isOnline.value = true
        return { success: true }
      }
      return { success: false, message: data.message }
    } catch (e) {
      return { success: false, message: 'Network error' }
    }
  }

  async function goOffline() {
    const auth = useAuthStore()
    try {
      const resp = await fetch('/api/ride/driver/offline', {
        method: 'PUT',
        headers: auth.authHeaders
      })
      const data = await resp.json()
      if (data.code === 200) {
        isOnline.value = false
        pendingOrders.value = []
        return { success: true }
      }
      return { success: false, message: data.message }
    } catch (e) {
      return { success: false, message: 'Network error' }
    }
  }

  async function fetchDriverStatus() {
    await fetchDriverInfo()
  }

  async function fetchOrderHistory() {
    const auth = useAuthStore()
    try {
      const resp = await fetch('/api/ride/order/my', {
        headers: auth.authHeaders
      })
      const data = await resp.json()
      if (data.code === 200) {
        orderHistory.value = data.data || []
        return data.data
      }
      return []
    } catch (e) {
      return []
    }
  }

  function startPolling(onUpdate) {
    stopPolling()
    pollTimer.value = setInterval(async () => {
      await fetchPendingOrders()
      await fetchCurrentOrder()
      onUpdate?.()
    }, 3000)
  }

  function stopPolling() {
    if (pollTimer.value) {
      clearInterval(pollTimer.value)
      pollTimer.value = null
    }
  }

  function startRealtime(onUpdate) {
    wsConnect()

    wsOn('order:CREATED', (data) => {
      fetchPendingOrders()
      onUpdate?.()
    })

    wsOn('order:ACCEPTED', (data) => {
      fetchCurrentOrder()
      fetchPendingOrders()
      onUpdate?.()
    })

    wsOn('order:ARRIVED', (data) => {
      fetchCurrentOrder()
      onUpdate?.()
    })

    wsOn('order:STARTED', (data) => {
      fetchCurrentOrder()
      onUpdate?.()
    })

    wsOn('order:COMPLETED', (data) => {
      fetchCurrentOrder()
      onUpdate?.()
    })

    wsOn('order:CANCELLED', (data) => {
      fetchCurrentOrder()
      fetchPendingOrders()
      onUpdate?.()
    })

    startPolling(onUpdate)
  }

  function stopRealtime() {
    stopPolling()
    wsDisconnect()
  }

  function clearCurrentOrder() {
    currentOrder.value = null
  }

  return {
    pendingOrders,
    currentOrder,
    orderHistory,
    isOnline,
    driverInfo,
    fetchDriverInfo,
    fetchPendingOrders,
    acceptOrder,
    arrivePickup,
    startTrip,
    completeTrip,
    fetchCurrentOrder,
    fetchOrderHistory,
    goOnline,
    goOffline,
    fetchDriverStatus,
    updateVehiclePlate,
    updateProfile,
    cancelOrder,
    startPolling,
    stopPolling,
    startRealtime,
    stopRealtime,
    clearCurrentOrder
  }
})
