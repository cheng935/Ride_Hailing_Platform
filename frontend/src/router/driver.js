export default [
  {
    path: '/driver',
    name: 'driver-login',
    component: () => import('@/views/driver/LoginView.vue'),
    meta: { guest: true }
  },
  {
    path: '/driver/register',
    name: 'driver-register',
    component: () => import('@/views/driver/RegisterView.vue'),
    meta: { guest: true }
  },
  {
    path: '/driver/home',
    name: 'driver-home',
    component: () => import('@/views/driver/HomeView.vue'),
    meta: { requiresAuth: true, role: 'DRIVER' }
  },
  {
    path: '/driver/order/:id',
    name: 'driver-order',
    component: () => import('@/views/driver/OrderDetailView.vue'),
    meta: { requiresAuth: true, role: 'DRIVER' }
  },
  {
    path: '/driver/history',
    name: 'driver-history',
    component: () => import('@/views/driver/HistoryView.vue'),
    meta: { requiresAuth: true, role: 'DRIVER' }
  },
  {
    path: '/driver/profile',
    name: 'driver-profile',
    component: () => import('@/views/driver/ProfileView.vue'),
    meta: { requiresAuth: true, role: 'DRIVER' }
  }
]