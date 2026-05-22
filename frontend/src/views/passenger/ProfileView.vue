<template>
  <section class="screen">
    <div class="panel full">
      <div class="profile-header">
        <div class="avatar">👤</div>
        <h1>{{ profile?.name || 'Loading...' }}</h1>
        <p class="role">{{ profile?.role || 'Passenger' }}</p>
      </div>

      <div v-if="loading" class="loading">Loading profile...</div>

      <div v-else class="info-section">
        <h3>Personal Information</h3>

        <div class="field-row">
          <label>User ID</label>
          <span class="field-value readonly">#{{ profile?.userId }}</span>
        </div>

        <div class="field-row">
          <label>Name</label>
          <div class="editable" v-if="editingName">
            <input v-model="editForm.name" placeholder="Enter name" @keyup.enter="saveName" ref="nameInput" />
            <button class="btn-save" @click="saveName" :disabled="saving">{{ saving ? '...' : '✓' }}</button>
            <button class="btn-cancel" @click="editingName = false">✕</button>
          </div>
          <span v-else class="field-value editable-text" @click="startEditName">{{ profile?.name || '--' }} <span class="edit-icon">✎</span></span>
        </div>

        <div class="field-row">
          <label>Phone</label>
          <span class="field-value readonly">{{ maskPhone(profile?.phone) }}</span>
        </div>

        <div class="field-row">
          <label>Password</label>
          <div class="editable" v-if="editingPassword">
            <input v-model="editForm.password" type="password" placeholder="New password" />
            <button class="btn-save" @click="savePassword" :disabled="saving">{{ saving ? '...' : '✓' }}</button>
            <button class="btn-cancel" @click="editingPassword = false">✕</button>
          </div>
          <span v-else class="field-value editable-text" @click="startEditPassword">•••••••• <span class="edit-icon">✎</span></span>
        </div>

        <div class="field-row">
          <label>Role</label>
          <span class="field-value readonly role-badge">{{ profile?.role }}</span>
        </div>

        <div class="field-row">
          <label>Rating</label>
          <span class="field-value rating">{{ profile?.rating?.toFixed(1) || '5.0' }} ⭐</span>
        </div>
      </div>

      <div class="stats" v-if="!loading">
        <h3>Statistics</h3>
        <div class="stat-grid">
          <div class="stat-item">
            <span class="stat-value">{{ stats.totalRides }}</span>
            <span class="stat-label">Total Rides</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">¥{{ stats.totalSpent.toFixed(2) }}</span>
            <span class="stat-label">Total Spent</span>
          </div>
        </div>
      </div>

      <div class="menu-list">
        <div class="menu-item" @click="router.push('/orders')">
          <span>📋</span>
          <span>My Orders</span>
          <span class="arrow">→</span>
        </div>
        <div class="menu-item" @click="router.push('/booking')">
          <span>🚗</span>
          <span>Book a Ride</span>
          <span class="arrow">→</span>
        </div>
        <div class="menu-item danger" @click="handleLogout">
          <span>🚪</span>
          <span>Logout</span>
          <span class="arrow">→</span>
        </div>
      </div>

      <p class="error-msg" v-if="error">{{ error }}</p>
      <p class="success-msg" v-if="success">{{ success }}</p>
    </div>
  </section>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useDriverStore } from '@/stores/driver'
import { useOrderStore } from '@/stores/order'

const router = useRouter()
const authStore = useAuthStore()
const driverStore = useDriverStore()
const orderStore = useOrderStore()

const profile = ref(null)
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const success = ref('')

const editingName = ref(false)
const editingPassword = ref(false)
const nameInput = ref(null)

const editForm = reactive({
  name: '',
  password: ''
})

const stats = ref({
  totalRides: 0,
  totalSpent: 0
})

function maskPhone(phone) {
  if (!phone) return '--'
  const s = String(phone)
  if (s.length >= 7) return s.slice(0, 3) + '****' + s.slice(-4)
  return s
}

async function loadProfile() {
  loading.value = true
  const info = await driverStore.fetchDriverInfo()
  if (info) {
    profile.value = info
  }
  loading.value = false
}

function startEditName() {
  editForm.name = profile.value?.name || ''
  editingName.value = true
  nextTick(() => nameInput.value?.focus())
}

async function saveName() {
  if (!editForm.name.trim()) return
  saving.value = true
  error.value = ''
  success.value = ''
  const result = await driverStore.updateProfile({ name: editForm.name.trim() })
  saving.value = false
  if (result.success) {
    editingName.value = false
    success.value = 'Name updated successfully'
    setTimeout(() => success.value = '', 3000)
  } else {
    error.value = result.message
  }
}

function startEditPassword() {
  editForm.password = ''
  editingPassword.value = true
}

async function savePassword() {
  if (!editForm.password || editForm.password.length < 6) {
    error.value = 'Password must be at least 6 characters'
    return
  }
  saving.value = true
  error.value = ''
  success.value = ''
  const result = await driverStore.updateProfile({ password: editForm.password })
  saving.value = false
  if (result.success) {
    editingPassword.value = false
    editForm.password = ''
    success.value = 'Password updated successfully'
    setTimeout(() => success.value = '', 3000)
  } else {
    error.value = result.message
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/')
}

onMounted(async () => {
  await loadProfile()
  const orders = await orderStore.fetchOrderHistory()
  if (orders) {
    stats.value.totalRides = orders.filter(o => o.status === 'COMPLETED').length
    stats.value.totalSpent = orders
      .filter(o => o.status === 'COMPLETED' && o.paymentStatus === 'PAID')
      .reduce((sum, o) => sum + (o.actualFare || 0), 0)
  }
})
</script>

<style scoped>
.screen { height: 100%; position: relative; background: #f8f9fb; }
.panel.full { position: absolute; left: 0; right: 0; top: 0; bottom: 0; background: white; padding: 26px 26px 226px 26px; overflow-y: auto; }
.profile-header { text-align: center; margin-bottom: 24px; }
.avatar { font-size: 64px; margin-bottom: 12px; }
h1 { margin: 0; font-size: 24px; color: #111827; font-weight: 800; }
.role { color: #6b7280; font-size: 14px; margin-top: 4px; }

.loading { text-align: center; padding: 40px; color: #6b7280; }

.info-section { margin-bottom: 20px; }
.info-section h3 { color: #111827; font-size: 18px; margin-bottom: 14px; padding-bottom: 8px; border-bottom: 2px solid #e5e7eb; }

.field-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #f3f4f6; }
.field-row label { color: #6b7280; font-size: 14px; font-weight: 500; min-width: 90px; }
.field-value { color: #111827; font-size: 15px; font-weight: 600; text-align: right; }
.field-value.readonly { color: #9ca3af; font-weight: 400; }
.field-value.editable-text { cursor: pointer; color: #1e3a8a; transition: color 0.2s; }
.field-value.editable-text:hover { color: #3b82f6; }
.edit-icon { font-size: 12px; color: #9ca3af; margin-left: 4px; }
.role-badge { display: inline-block; background: #dbeafe; color: #1e40af; padding: 2px 10px; border-radius: 10px; font-size: 13px; }
.rating { color: #f59e0b; }

.editable { display: flex; gap: 6px; align-items: center; }
.editable input { flex: 1; padding: 8px 12px; border: 2px solid #1e3a8a; border-radius: 10px; font-size: 14px; outline: none; }
.editable input:focus { border-color: #3b82f6; }
.btn-save { width: 36px; height: 36px; border: none; border-radius: 10px; background: #22c55e; color: white; font-size: 16px; font-weight: 700; cursor: pointer; }
.btn-cancel { width: 36px; height: 36px; border: none; border-radius: 10px; background: #fee2e2; color: #dc2626; font-size: 16px; cursor: pointer; }

.stats { margin-bottom: 20px; }
.stats h3 { color: #111827; font-size: 18px; margin-bottom: 14px; padding-bottom: 8px; border-bottom: 2px solid #e5e7eb; }
.stat-grid { display: flex; gap: 20px; }
.stat-item { flex: 1; text-align: center; background: #f8fafc; border-radius: 14px; padding: 16px; }
.stat-value { display: block; font-size: 24px; font-weight: 800; color: #111827; }
.stat-label { font-size: 12px; color: #6b7280; margin-top: 4px; }

.menu-list { display: flex; flex-direction: column; gap: 8px; }
.menu-item { display: flex; align-items: center; gap: 12px; padding: 16px; background: #f8fafc; border-radius: 12px; cursor: pointer; transition: all 0.2s; }
.menu-item:hover { background: #eef5ff; }
.menu-item.danger { color: #dc2626; }
.menu-item.danger:hover { background: #fee2e2; }
.menu-item span:first-child { font-size: 20px; }
.menu-item span:nth-child(2) { flex: 1; font-weight: 600; }
.arrow { color: #9ca3af; }

.error-msg { color: #dc2626; font-size: 13px; text-align: center; margin-top: 12px; }
.success-msg { color: #22c55e; font-size: 13px; text-align: center; margin-top: 12px; font-weight: 600; }
</style>
