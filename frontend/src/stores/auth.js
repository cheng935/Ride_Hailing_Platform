import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(sessionStorage.getItem('token') || '')
  const userId = ref(sessionStorage.getItem('userId') || null)
  const userName = ref(sessionStorage.getItem('userName') || '')
  const userRole = ref(sessionStorage.getItem('userRole') || '')

  const isLoggedIn = computed(() => !!token.value)

  const authHeaders = computed(() => ({
    'Authorization': 'Bearer ' + token.value,
    'Content-Type': 'application/json'
  }))

  async function login(phone, password) {
    try {
      const resp = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone, password })
      })
      const data = await resp.json()
      if (data.code === 200) {
        setAuth(data.data.token, data.data.userId, data.data.name, data.data.role)
        return { success: true }
      }
      return { success: false, message: data.message || 'Login failed' }
    } catch (e) {
      return { success: false, message: 'Network error' }
    }
  }

  async function register(name, phone, password, role) {
    try {
      const params = new URLSearchParams({ name, phone, password, role })
      const resp = await fetch('/api/auth/register?' + params, { method: 'POST' })
      const data = await resp.json()
      if (data.code === 200) {
        return { success: true }
      }
      return { success: false, message: data.message || 'Registration failed' }
    } catch (e) {
      return { success: false, message: 'Network error' }
    }
  }

  function setAuth(newToken, newUserId, newUserName, newUserRole) {
    token.value = newToken
    userId.value = newUserId
    userName.value = newUserName
    userRole.value = newUserRole
    sessionStorage.setItem('token', newToken)
    sessionStorage.setItem('userId', newUserId)
    sessionStorage.setItem('userName', newUserName)
    sessionStorage.setItem('userRole', newUserRole || '')
  }

  function logout() {
    token.value = ''
    userId.value = null
    userName.value = ''
    userRole.value = ''
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userId')
    sessionStorage.removeItem('userName')
    sessionStorage.removeItem('userRole')
  }

  return {
    token, userId, userName, userRole,
    isLoggedIn, authHeaders,
    login, register, logout, setAuth
  }
})