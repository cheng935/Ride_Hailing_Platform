import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAuthStore } from './auth'
import { useWebSocket } from '@/composables/useWebSocket'

export const useOrderStore = defineStore('order', () => {
  const currentOrder = ref(null)
  const orderHistory = ref([])
  const pollTimer = ref(null)

  const { connect: wsConnect, disconnect: wsDisconnect, on: wsOn, off: wsOff, connected: wsConnected } = useWebSocket()

  async function createOrder(pickupName, pickupLat, pickupLng, destName, destLat, destLng) {
    const auth = useAuthStore()
    try {
      const params = new URLSearchParams({
        pickupName, pickupLat, pickupLng, destName, destLat, destLng
      })
      const resp = await fetch('/api/ride/order?' + params, {
        method: 'POST',
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

  async function fetchOrder(orderId) {
    const auth = useAuthStore()
    try {
      const resp = await fetch('/api/ride/order/' + orderId, {
        headers: auth.authHeaders
      })
      const data = await resp.json()
      if (data.code === 200) {
        currentOrder.value = data.data
        return data.data
      }
      return null
    } catch (e) {
      return null
    }
  }

  async function fetchOrderHistory() {
    const auth = useAuthStore()
    try {
      const resp = await fetch('/api/ride/order/my', {
        headers: auth.authHeaders
      })
      const data = await resp.json()
      if (data.code === 200) {
        orderHistory.value = data.data
        return data.data
      }
      return []
    } catch (e) {
      return []
    }
  }

  function startPolling(orderId, onUpdate, onComplete, onCancel) {
    stopPolling()
    pollTimer.value = setInterval(async () => {
      const order = await fetchOrder(orderId)
      if (order) {
        onUpdate?.(order)
        if (order.status === 'COMPLETED') {
          stopPolling()
          onComplete?.(order)
        } else if (order.status === 'CANCELLED') {
          stopPolling()
          onCancel?.(order)
        }
      }
    }, 2000)
  }

  function stopPolling() {
    if (pollTimer.value) {
      clearInterval(pollTimer.value)
      pollTimer.value = null
    }
  }

  function startRealtime(orderId, onUpdate, onComplete, onCancel) {
    wsConnect()

    wsOn('order:ACCEPTED', (data) => {
      if (data.orderId == orderId) {
        fetchOrder(orderId).then(o => { if (o) onUpdate?.(o) })
      }
    })

    wsOn('order:ARRIVED', (data) => {
      if (data.orderId == orderId) {
        fetchOrder(orderId).then(o => { if (o) onUpdate?.(o) })
      }
    })

    wsOn('order:STARTED', (data) => {
      if (data.orderId == orderId) {
        fetchOrder(orderId).then(o => { if (o) onUpdate?.(o) })
      }
    })

    wsOn('order:COMPLETED', (data) => {
      if (data.orderId == orderId) {
        stopPolling()
        fetchOrder(orderId).then(o => { if (o) onComplete?.(o) })
      }
    })

    wsOn('order:CANCELLED', (data) => {
      if (data.orderId == orderId) {
        stopPolling()
        fetchOrder(orderId).then(o => { if (o) onCancel?.(o) })
      }
    })

    startPolling(orderId, onUpdate, onComplete, onCancel)
  }

  function stopRealtime() {
    stopPolling()
    wsDisconnect()
  }

  async function initiatePayment(orderId) {
    const auth = useAuthStore()
    try {
      const resp = await fetch('/api/ride/order/' + orderId + '/pay', {
        method: 'POST',
        headers: auth.authHeaders
      })
      const data = await resp.json()
      if (data.code === 200) {
        return { success: true, paymentId: data.data.paymentId }
      }
      return { success: false, message: data.message }
    } catch (e) {
      return { success: false, message: 'Network error' }
    }
  }

  async function confirmPayment(orderId) {
    const auth = useAuthStore()
    try {
      const resp = await fetch('/api/ride/order/' + orderId + '/confirm-pay', {
        method: 'POST',
        headers: auth.authHeaders
      })
      const data = await resp.json()
      if (data.code === 200) {
        return { success: true }
      }
      return { success: false, message: data.message }
    } catch (e) {
      return { success: false, message: 'Network error' }
    }
  }

  async function submitReview(orderId, driverId, rating, comment) {
    const auth = useAuthStore()
    try {
      const resp = await fetch('/api/reviews', {
        method: 'POST',
        headers: { ...auth.authHeaders, 'Content-Type': 'application/json' },
        body: JSON.stringify({
          reviewerId: Number(auth.userId),
          reviewedId: Number(driverId),
          orderId: Number(orderId),
          rating,
          comment
        })
      })
      const data = await resp.json()
      if (data.code === 200) {
        return { success: true }
      }
      return { success: false, message: data.message }
    } catch (e) {
      return { success: false, message: 'Network error' }
    }
  }

  function clearCurrentOrder() {
    stopPolling()
    currentOrder.value = null
  }

  async function estimatePrice(pickupLat, pickupLng, destLat, destLng, pickupName, destName) {
    try {
      const resp = await fetch('/api/pricing/estimate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pickupLat, pickupLng, destLat, destLng, pickupName, destName })
      })
      const data = await resp.json()
      if (data.code === 200) {
        return { success: true, pricing: data.data }
      }
      return { success: false, message: data.message }
    } catch (e) {
      return { success: false, message: 'Network error' }
    }
  }

  return {
    currentOrder, orderHistory,
    createOrder, fetchOrder, fetchOrderHistory,
    startPolling, stopPolling,
    startRealtime, stopRealtime,
    initiatePayment, confirmPayment, submitReview,
    clearCurrentOrder, estimatePrice
  }
})
