import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
    },
    // user routes
    {
      path: '/',
      name: 'square',
      component: () => import('../views/PostSquare.vue'),
      meta: { requiresAuth: true, requiresUser: true },
    },
    {
      path: '/publish',
      name: 'publish',
      component: () => import('../views/PostPublish.vue'),
      meta: { requiresAuth: true, requiresUser: true },
    },
    {
      path: '/post/:id',
      name: 'postDetail',
      component: () => import('../views/PostDetail.vue'),
      meta: { requiresAuth: true, requiresUser: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/HomeView.vue'),
      meta: { requiresAuth: true, requiresUser: true },
    },
    {
      path: '/messages',
      name: 'messages',
      component: () => import('../views/MessagesView.vue'),
      meta: { requiresAuth: true, requiresUser: true },
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('../views/ChatView.vue'),
      meta: { requiresAuth: true, requiresUser: true },
    },
    {
      path: '/claims',
      name: 'claims',
      component: () => import('../views/ClaimProgress.vue'),
      meta: { requiresAuth: true, requiresUser: true },
    },
    {
      path: '/governance',
      name: 'governance',
      component: () => import('../views/GovernanceView.vue'),
      meta: { requiresAuth: true, requiresUser: true },
    },
    {
      path: '/my-posts',
      name: 'myPosts',
      component: () => import('../views/MyPostsView.vue'),
      meta: { requiresAuth: true, requiresUser: true },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'notFound',
      component: () => import('../views/NotFoundView.vue'),
    },
  ],
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  // not logged in → login
  if (to.meta.requiresAuth && !token) {
    next('/login')
    return
  }
  // already logged in, accessing /login → redirect by role
  if (to.path === '/login' && token) {
    next('/')
    return
  }
  next()
})

export default router
