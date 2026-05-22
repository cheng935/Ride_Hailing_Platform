export default [
  {
    path: '/',
    name: 'passenger-login',
    component: () => import('@/views/passenger/LoginView.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'passenger-register',
    component: () => import('@/views/passenger/RegisterView.vue'),
    meta: { guest: true }
  },
  {
    path: '/booking',
    name: 'passenger-booking',
    component: () => import('@/views/passenger/BookingView.vue'),
    meta: { requiresAuth: true, role: 'PASSENGER' }
  },
  {
    path: '/tracking/:id',
    name: 'passenger-tracking',
    component: () => import('@/views/passenger/TrackingView.vue'),
    meta: { requiresAuth: true, role: 'PASSENGER' }
  },
  {
    path: '/complete/:id',
    name: 'passenger-complete',
    component: () => import('@/views/passenger/CompleteView.vue'),
    meta: { requiresAuth: true, role: 'PASSENGER' }
  },
  {
    path: '/orders',
    name: 'passenger-orders',
    component: () => import('@/views/passenger/OrdersView.vue'),
    meta: { requiresAuth: true, role: 'PASSENGER' }
  },
  {
    path: '/profile',
    name: 'passenger-profile',
    component: () => import('@/views/passenger/ProfileView.vue'),
    meta: { requiresAuth: true, role: 'PASSENGER' }
  }
]