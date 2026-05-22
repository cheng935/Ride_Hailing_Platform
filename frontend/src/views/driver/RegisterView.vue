<template>
  <section class="screen">
    <div class="map">
      <div class="road road-one"></div>
      <div class="road road-two"></div>
      <div class="car">🚗</div>
      <div class="location-dot"></div>
    </div>
    <div class="panel">
      <h1>Driver Registration</h1>
      <p class="sub">Join our driver team</p>
      <div class="input-card">
        <span style="font-size:20px">👤</span>
        <input v-model="name" placeholder="Full name" />
      </div>
      <div class="input-card">
        <span style="font-size:20px">📱</span>
        <input v-model="phone" placeholder="Phone number" />
      </div>
      <div class="input-card">
        <span style="font-size:20px">🔒</span>
        <input v-model="password" type="password" placeholder="Password" />
      </div>
      <p class="error-msg" v-if="error">{{ error }}</p>
      <p class="success-msg" v-if="success">{{ success }}</p>
      <button @click="handleRegister" :disabled="loading">
        {{ loading ? 'Creating...' : 'Register as Driver →' }}
      </button>
      <p class="sub" style="text-align:center;margin-top:16px;cursor:pointer;color:#1a73e8" @click="router.push('/driver')">
        Already have an account? Sign in
      </p>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const name = ref('')
const phone = ref('')
const password = ref('')
const error = ref('')
const success = ref('')
const loading = ref(false)

async function handleRegister() {
  error.value = ''
  success.value = ''
  if (!name.value || !phone.value || !password.value) {
    error.value = 'Please fill in all fields'
    return
  }
  loading.value = true
  const result = await authStore.register(name.value, phone.value, password.value, 'DRIVER')
  loading.value = false
  if (result.success) {
    success.value = 'Registration successful! You can now sign in.'
    name.value = ''
    phone.value = ''
    password.value = ''
  } else {
    error.value = result.message
  }
}
</script>

<style scoped>
.screen { height: 100%; position: relative; }
.map { height: 330px; background: linear-gradient(135deg, #e9eef7, #ffffff); position: relative; overflow: hidden; }
.road { position: absolute; background: white; border-radius: 999px; box-shadow: 0 0 0 1px #e5e7eb; }
.road-one { width: 520px; height: 28px; top: 120px; left: -70px; transform: rotate(-28deg); }
.road-two { width: 460px; height: 24px; top: 190px; left: -30px; transform: rotate(22deg); }
.car { position: absolute; top: 138px; left: 210px; font-size: 28px; transform: rotate(-25deg); }
.location-dot { position: absolute; top: 160px; left: 165px; width: 22px; height: 22px; background: #f59e0b; border: 5px solid white; border-radius: 50%; box-shadow: 0 0 0 18px rgba(245,158,11,0.15); }
.panel { position: absolute; left: 0; right: 0; bottom: 0; background: rgba(255,255,255,0.88); backdrop-filter: blur(18px); padding: 26px; border-radius: 32px 32px 0 0; box-shadow: 0 -10px 35px rgba(0,0,0,0.08); }
h1 { margin: 0; font-size: 28px; color: #111827; font-weight: 800; }
.sub { color: #4b5563; font-weight: 500; margin-top: 6px; margin-bottom: 20px; }
.input-card { display: flex; align-items: center; gap: 12px; background: white; padding: 14px; border-radius: 18px; margin-bottom: 12px; border: 1px solid #edf0f5; }
input { border: none; outline: none; background: transparent; width: 100%; font-size: 15px; color: #111827; font-weight: 600; }
button { width: 100%; height: 54px; border: none; border-radius: 18px; background: linear-gradient(135deg, #f59e0b, #d97706); color: white; font-size: 16px; font-weight: 700; cursor: pointer; margin-top: 14px; }
button:disabled { opacity: 0.6; cursor: not-allowed; }
.error-msg { color: #dc2626; font-size: 13px; margin: 4px 0; text-align: center; }
.success-msg { color: #22c55e; font-size: 13px; margin: 4px 0; text-align: center; }
</style>