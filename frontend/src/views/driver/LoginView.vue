<template>
  <section class="screen">
    <div class="map">
      <div class="road road-one"></div>
      <div class="road road-two"></div>
      <div class="car">🚗</div>
      <div class="location-dot"></div>
    </div>
    <div class="panel">
      <h1>Driver Login</h1>
      <p class="sub">Sign in to start driving</p>
      <div class="input-card">
        <span style="font-size:20px">📱</span>
        <input v-model="phone" placeholder="Phone number" @keyup.enter="handleLogin" />
      </div>
      <div class="input-card">
        <span style="font-size:20px">🔒</span>
        <input v-model="password" type="password" placeholder="Password" @keyup.enter="handleLogin" />
      </div>
      <p class="error-msg" v-if="error">{{ error }}</p>
      <button @click="handleLogin" :disabled="loading">
        {{ loading ? 'Signing in...' : 'Sign In →' }}
      </button>
      <p class="sub" style="text-align:center;margin-top:16px;cursor:pointer;color:#1a73e8" @click="router.push('/driver/register')">
        Don't have an account? Register
      </p>
      <p class="sub" style="text-align:center;margin-top:8px;cursor:pointer;color:#6b7280" @click="router.push('/')">
        👤 Passenger? Login here
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

const phone = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  error.value = ''
  if (!phone.value || !password.value) {
    error.value = 'Please enter phone and password'
    return
  }
  loading.value = true
  const result = await authStore.login(phone.value, password.value)
  loading.value = false
  if (result.success) {
    if (authStore.userRole === 'DRIVER') {
      router.push('/driver/home')
    } else {
      error.value = 'This account is not a driver'
      authStore.logout()
    }
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
</style>