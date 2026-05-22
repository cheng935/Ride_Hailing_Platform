<template>
  <section class="screen">
    <div class="panel full">
      <div class="profile-header">
        <div class="avatar">🚗</div>
        <h1>{{ profile?.name || 'Loading...' }}</h1>
        <p class="role">{{ profile?.role || 'Driver' }}</p>
        <div class="status-badge" :class="profile?.isOnline ? 'online' : 'offline'">
          {{ profile?.isOnline ? '● Online' : '○ Offline' }}
        </div>
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
            <input v-model="editForm.name" placeholder="Enter name" @keyup.enter="saveProfile" ref="nameInput" />
            <button class="btn-save" @click="saveProfile" :disabled="saving">{{ saving ? '...' : '✓' }}</button>
            <button class="btn-cancel" @click="cancelEdit('name')">✕</button>
          </div>
          <span v-else class="field-value editable-text" @click="startEdit('name')">{{ profile?.name || '--' }} <span class="edit-icon">✎</span></span>
        </div>

        <div class="field-row">
          <label>Phone</label>
          <span class="field-value readonly">{{ maskPhone(profile?.phone) }}</span>
        </div>

        <div class="field-row">
          <label>Password</label>
          <div class="editable" v-if="editingPassword">
            <input v-model="editForm.password" type="password" placeholder="New password" />
            <button class="btn-save" @click="saveProfile" :disabled="saving">{{ saving ? '...' : '✓' }}</button>
            <button class="btn-cancel" @click="cancelEdit('password')">✕</button>
          </div>
          <span v-else class="field-value editable-text" @click="startEdit('password')">•••••••• <span class="edit-icon">✎</span></span>
        </div>

        <div class="field-row">
          <label>Role</label>
          <span class="field-value readonly role-badge driver">{{ profile?.role }}</span>
        </div>

        <div class="field-row">
          <label>Rating</label>
          <span class="field-value rating">{{ profile?.rating?.toFixed(1) || '5.0' }} ⭐</span>
        </div>
      </div>

      <div v-if="!loading" class="info-section vehicle-section">
        <h3>Vehicle Information</h3>

        <div class="field-row">
          <label>License No.</label>
          <span class="field-value readonly">{{ profile?.licenseNumber || '--' }}</span>
        </div>

        <div class="field-row">
          <label>Vehicle Type</label>
          <div class="editable" v-if="editingVehicleType">
            <input v-model="editForm.vehicleType" placeholder="e.g. Sedan, SUV, EV" @keyup.enter="saveProfile" />
            <button class="btn-save" @click="saveProfile" :disabled="saving">{{ saving ? '...' : '✓' }}</button>
            <button class="btn-cancel" @click="cancelEdit('vehicleType')">✕</button>
          </div>
          <span v-else class="field-value editable-text" @click="startEdit('vehicleType')">{{ profile?.vehicleType || '--' }} <span class="edit-icon">✎</span></span>
        </div>

        <div class="field-row">
          <label>Plate Number</label>
          <div class="editable" v-if="editingPlate">
            <input v-model="editForm.vehiclePlate" placeholder="e.g. 浙C·12345" @keyup.enter="saveProfile" />
            <button class="btn-save" @click="saveProfile" :disabled="saving">{{ saving ? '...' : '✓' }}</button>
            <button class="btn-cancel" @click="cancelEdit('plate')">✕</button>
          </div>
          <span v-else class="field-value editable-text plate" @click="startEdit('plate')">{{ profile?.vehiclePlate || 'Not set' }} <span class="edit-icon">✎</span></span>
        </div>
      </div>

      <div class="stats" v-if="!loading">
        <h3>Statistics</h3>
        <div class="stat-grid">
          <div class="stat-item">
            <span class="stat-value">{{ stats.totalTrips }}</span>
            <span class="stat-label">Total Trips</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">¥{{ stats.totalEarned.toFixed(2) }}</span>
            <span class="stat-label">Total Earned</span>
          </div>
        </div>
      </div>

      <div class="menu-list">
        <div class="menu-item" @click="router.push('/driver/home')">
          <span>🏠</span>
          <span>Driver Home</span>
          <span class="arrow">→</span>
        </div>
        <div class="menu-item" @click="router.push('/driver/history')">
          <span>📋</span>
          <span>Trip History</span>
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
const editingVehicleType = ref(false)
const editingPlate = ref(false)
const nameInput = ref(null)

const editForm = reactive({
  name: '',
  password: '',
  vehicleType: '',
  vehiclePlate: ''
})

const stats = ref({
  totalTrips: 0,
  totalEarned: 0
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

function startEdit(field) {
  switch (field) {
    case 'name':
      editForm.name = profile.value?.name || ''
      editingName.value = true
      nextTick(() => nameInput.value?.focus())
      break
    case 'password':
      editForm.password = ''
      editingPassword.value = true
      break
    case 'vehicleType':
      editForm.vehicleType = profile.value?.vehicleType || ''
      editingVehicleType.value = true
      break
    case 'plate':
      editForm.vehiclePlate = profile.value?.vehiclePlate || ''
      editingPlate.value = true
      break
  }
}

function cancelEdit(field) {
  switch (field) {
    case 'name': editingName.value = false; break
    case 'password': editingPassword.value = false; editForm.password = ''; break
    case 'vehicleType': editingVehicleType.value = false; break
    case 'plate': editingPlate.value = false; break
  }
}

async function saveProfile() {
  const data = {}
  let hasData = false

  if (editingName.value && editForm.name.trim()) {
    data.name = editForm.name.trim()
    hasData = true
  }
  if (editingPassword.value && editForm.password && editForm.password.length >= 6) {
    data.password = editForm.password
    hasData = true
  }
  if (editingVehicleType.value && editForm.vehicleType.trim()) {
    data.vehicleType = editForm.vehicleType.trim()
    hasData = true
  }
  if (editingPlate.value && editForm.vehiclePlate.trim()) {
    data.vehiclePlate = editForm.vehiclePlate.trim()
    hasData = true
  }

  if (!hasData) {
    error.value = 'Please enter a valid value'
    return
  }

  saving.value = true
  error.value = ''
  success.value = ''

  const result = await driverStore.updateProfile(data)
  saving.value = false

  if (result.success) {
    editingName.value = false
    editingPassword.value = false
    editingVehicleType.value = false
    editingPlate.value = false
    editForm.password = ''
    success.value = 'Profile updated successfully'
    setTimeout(() => success.value = '', 3000)
  } else {
    error.value = result.message
  }
}

function handleLogout() {
  if (driverStore.isOnline) {
    driverStore.goOffline()
  }
  driverStore.stopRealtime()
  authStore.logout()
  router.push('/driver')
}

onMounted(async () => {
  await loadProfile()
  const orders = await orderStore.fetchOrderHistory()
  if (orders) {
    stats.value.totalTrips = orders.filter(o => o.status === 'COMPLETED').length
    stats.value.totalEarned = orders
      .filter(o => o.status === 'COMPLETED' && o.paymentStatus === 'PAID')
      .reduce((sum, o) => sum + (o.actualFare || 0), 0)
  }
})
</script>

<style scoped>
.screen { height: 100%; position: relative; background: #f8f9fb; }
.panel.full { position: absolute; left: 0; right: 0; top: 0; bottom: 0; background: white; padding: 20px 20px 280px 20px; overflow-y: auto; }
.profile-header { text-align: center; margin-bottom: 20px; }
.avatar { font-size: 64px; margin-bottom: 12px; }
h1 { margin: 0; font-size: 24px; color: #111827; font-weight: 800; }
.role { color: #6b7280; font-size: 14px; margin-top: 4px; }
.status-badge { display: inline-block; margin-top: 8px; padding: 4px 16px; border-radius: 20px; font-size: 13px; font-weight: 600; }
.status-badge.online { background: #d1fae5; color: #065f46; }
.status-badge.offline { background: #f3f4f6; color: #6b7280; }

.loading { text-align: center; padding: 40px; color: #6b7280; }

.info-section { margin-bottom: 18px; }
.info-section h3 { color: #111827; font-size: 17px; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 2px solid #e5e7eb; }
.vehicle-section h3 { color: #1e3a8a; border-bottom-color: #bfdbfe; }

.field-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #f3f4f6; }
.field-row label { color: #6b7280; font-size: 13px; font-weight: 500; min-width: 95px; }
.field-value { color: #111827; font-size: 14px; font-weight: 600; text-align: right; }
.field-value.readonly { color: #9ca3af; font-weight: 400; }
.field-value.editable-text { cursor: pointer; color: #1e3a8a; transition: color 0.2s; }
.field-value.editable-text:hover { color: #3b82f6; }
.field-value.editable-text.plate { color: #f59e0b; font-weight: 700; }
.edit-icon { font-size: 11px; color: #9ca3af; margin-left: 4px; }
.role-badge { display: inline-block; background: #dbeafe; color: #1e40af; padding: 2px 10px; border-radius: 10px; font-size: 12px; }
.role-badge.driver { background: #fef3c7; color: #92400e; }
.rating { color: #f59e0b; }

.editable { display: flex; gap: 6px; align-items: center; }
.editable input { flex: 1; padding: 8px 10px; border: 2px solid #1e3a8a; border-radius: 10px; font-size: 14px; outline: none; min-width: 120px; }
.editable input:focus { border-color: #3b82f6; }
.btn-save { width: 34px; height: 34px; border: none; border-radius: 10px; background: #22c55e; color: white; font-size: 15px; font-weight: 700; cursor: pointer; }
.btn-cancel { width: 34px; height: 34px; border: none; border-radius: 10px; background: #fee2e2; color: #dc2626; font-size: 15px; cursor: pointer; }

.stats { margin-bottom: 18px; }
.stats h3 { color: #111827; font-size: 17px; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 2px solid #e5e7eb; }
.stat-grid { display: flex; gap: 16px; }
.stat-item { flex: 1; text-align: center; background: linear-gradient(135deg, #1e3a8a, #111827); border-radius: 14px; padding: 14px; color: white; }
.stat-value { display: block; font-size: 22px; font-weight: 800; }
.stat-label { font-size: 11px; opacity: 0.8; margin-top: 4px; }

.menu-list { display: flex; flex-direction: column; gap: 8px; }
.menu-item { display: flex; align-items: center; gap: 12px; padding: 14px; background: #f8fafc; border-radius: 12px; cursor: pointer; transition: all 0.2s; }
.menu-item:hover { background: #eef5ff; }
.menu-item.danger { color: #dc2626; }
.menu-item.danger:hover { background: #fee2e2; }
.menu-item span:first-child { font-size: 18px; }
.menu-item span:nth-child(2) { flex: 1; font-weight: 600; }
.arrow { color: #9ca3af; }

.error-msg { color: #dc2626; font-size: 13px; text-align: center; margin-top: 10px; }
.success-msg { color: #22c55e; font-size: 13px; text-align: center; margin-top: 10px; font-weight: 600; }
</style>
