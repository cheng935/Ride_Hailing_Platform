import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import passengerRoutes from './passenger'
import driverRoutes from './driver'

const routes = [
  ...passengerRoutes,
  ...driverRoutes,
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth) {
    if (!authStore.isLoggedIn) {
      if (to.path.startsWith('/driver')) {
        next({ name: 'driver-login' })
      } else {
        next({ name: 'passenger-login' })
      }
      return
    }

    if (to.meta.role && authStore.userRole !== to.meta.role) {
      if (authStore.userRole === 'DRIVER') {
        next({ name: 'driver-home' })
      } else {
        next({ name: 'passenger-booking' })
      }
      return
    }
  }

  if (to.meta.guest && authStore.isLoggedIn) {
    if (authStore.userRole === 'DRIVER') {
      next({ name: 'driver-home' })
    } else {
      next({ name: 'passenger-booking' })
    }
    return
  }

  next()
})

export default router