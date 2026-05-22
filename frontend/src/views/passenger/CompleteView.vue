<template>
  <section class="screen complete">
    <div class="success">
      <div class="check">✓</div>
      <h1>Trip Completed</h1>
      <p>Thank you for riding with us!</p>
    </div>
    <div class="panel">
      <div class="fare-card">
        <h3>Fare Details</h3>
        <p><span>Base Fare</span><b>¥{{ order?.baseFare || '--' }}</b></p>
        <p><span>Distance Fare</span><b>¥{{ order?.distanceFare || '--' }}</b></p>
        <p><span>Duration Fare</span><b>¥{{ order?.durationFare || '--' }}</b></p>

        <template v-if="order?.surcharges && order.surcharges.length > 0">
          <hr />
          <p v-for="(s, idx) in order.surcharges" :key="idx" class="surcharge">
            <span>{{ s.reason }}</span><b>+¥{{ s.amount?.toFixed(2) || '0.00' }}</b>
          </p>
        </template>

        <hr />
        <p class="total"><span>Total</span><b>¥{{ order?.actualFare || order?.estimatedFare || '--' }}</b></p>
      </div>

      <div v-if="paymentStatus === 'UNPAID'">
        <button @click="handleInitiatePay" :disabled="payLoading">
          {{ payLoading ? 'Processing...' : '💳 Pay ¥' + (order?.actualFare || order?.estimatedFare || '--') }}
        </button>
      </div>

      <div v-if="paymentStatus === 'PENDING'">
        <p class="pending-msg">Payment initiated — confirm to complete</p>
        <button @click="handleConfirmPay" :disabled="payLoading">
          {{ payLoading ? 'Confirming...' : '✅ Confirm Payment' }}
        </button>
      </div>

      <div v-if="paymentStatus === 'PAID'">
        <p class="paid-msg">✅ Payment Successful!</p>
        <h3>Rate Your Driver</h3>
        <div class="stars">
          <span v-for="n in 5" :key="n" @click="rating = n" :class="{ selected: n <= rating }">★</span>
        </div>
        <textarea v-model="comment" placeholder="Share your experience"></textarea>
        <button @click="handleSubmitReview" :disabled="reviewLoading">
          {{ reviewLoading ? 'Submitting...' : 'Submit Rating →' }}
        </button>
      </div>

      <p class="error-msg" v-if="error">{{ error }}</p>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOrderStore } from '@/stores/order'

const route = useRoute()
const router = useRouter()
const orderStore = useOrderStore()

const order = ref(null)
const paymentStatus = ref('UNPAID')
const rating = ref(5)
const comment = ref('')
const payLoading = ref(false)
const reviewLoading = ref(false)
const error = ref('')

async function handleInitiatePay() {
  if (!order.value) return
  payLoading.value = true
  const result = await orderStore.initiatePayment(order.value.orderId)
  payLoading.value = false
  if (result.success) {
    paymentStatus.value = 'PENDING'
  } else {
    error.value = result.message
  }
}

async function handleConfirmPay() {
  if (!order.value) return
  payLoading.value = true
  const result = await orderStore.confirmPayment(order.value.orderId)
  payLoading.value = false
  if (result.success) {
    paymentStatus.value = 'PAID'
  } else {
    error.value = result.message
  }
}

async function handleSubmitReview() {
  if (!order.value) return
  reviewLoading.value = true
  const result = await orderStore.submitReview(
    order.value.orderId,
    order.value.driverId,
    rating.value,
    comment.value
  )
  reviewLoading.value = false
  if (result.success) {
    orderStore.clearCurrentOrder()
    router.push('/booking')
  } else {
    error.value = result.message
  }
}

onMounted(async () => {
  const orderId = route.params.id
  if (orderId) {
    const fetchedOrder = await orderStore.fetchOrder(orderId)
    if (fetchedOrder) {
      order.value = fetchedOrder
      paymentStatus.value = fetchedOrder.paymentStatus || 'UNPAID'
    }
  }
})
</script>

<style scoped>
.screen { height: 100%; position: relative; overflow-y: auto; padding-bottom: 200px; }
.complete { background: linear-gradient(180deg, #dbeafe 0%, #f8f9fb 45%); }
.success { text-align: center; padding-top: 70px; }
.check { width: 92px; height: 92px; background: #22c55e; color: white; font-size: 58px; line-height: 92px; border-radius: 50%; margin: 0 auto 24px; box-shadow: 0 15px 35px rgba(34,197,94,0.35); }
.success p { color: #6b7280; }
.panel { background: rgba(255,255,255,0.88); backdrop-filter: blur(18px); padding: 26px; border-radius: 32px 32px 0 0; box-shadow: 0 -10px 35px rgba(0,0,0,0.08); }
h1 { margin: 0; font-size: 28px; color: #111827; font-weight: 800; }
h3 { color: #111827; font-size: 20px; margin-bottom: 14px; }
.fare-card { background: #f8fafc; border-radius: 22px; padding: 18px; margin-bottom: 18px; }
.fare-card p { display: flex; justify-content: space-between; color: #4b5563; }
.fare-card .surcharge { color: #f59e0b; font-size: 14px; }
.fare-card .total { font-size: 20px; color: #111827; }
hr { border: none; border-top: 1px solid #e5e7eb; margin: 12px 0; }
button { width: 100%; height: 54px; border: none; border-radius: 18px; background: linear-gradient(135deg, #111827, #1e3a8a); color: white; font-size: 16px; font-weight: 700; cursor: pointer; margin-top: 14px; }
button:disabled { opacity: 0.6; cursor: not-allowed; }
.pending-msg { text-align: center; color: #1a73e8; font-weight: 600; margin: 12px 0; }
.paid-msg { text-align: center; color: #22c55e; font-weight: 700; font-size: 18px; margin: 12px 0; }
.stars { font-size: 34px; margin-bottom: 16px; }
.stars span { color: #d1d5db; cursor: pointer; }
.stars span.selected { color: #facc15; }
textarea { width: 100%; height: 90px; background: #f8fafc; border: 1px solid #edf0f5; border-radius: 18px; padding: 14px; resize: none; font-size: 15px; outline: none; }
.error-msg { color: #dc2626; font-size: 13px; margin: 4px 0; text-align: center; }
</style>
