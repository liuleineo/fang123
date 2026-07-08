import { createRouter, createWebHistory } from 'vue-router'
import WebLayout from '@/layouts/WebLayout.vue'

const routes = [
  {
    path: '/',
    component: WebLayout,
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'login',
        name: 'Login',
        component: () => import('@/views/Login.vue'),
        meta: { title: '登录' }
      },
      {
        path: 'register',
        name: 'Register',
        component: () => import('@/views/Register.vue'),
        meta: { title: '注册' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      },
      {
        path: 'tasks',
        name: 'Tasks',
        component: () => import('@/views/Tasks.vue'),
        meta: { title: '任务中心' }
      },
      {
        path: 'tasks/:id',
        name: 'TaskDetail',
        component: () => import('@/views/TaskDetail.vue'),
        meta: { title: '任务详情', requiresAuth: true }
      },
      {
        path: 'my-tasks',
        name: 'MyTasks',
        component: () => import('@/views/MyTasks.vue'),
        meta: { title: '我的任务', requiresAuth: true }
      },
      {
        path: 'member',
        name: 'Member',
        component: () => import('@/views/Member.vue'),
        meta: { title: '会员中心', requiresAuth: true }
      },
      {
        path: 'map',
        name: 'MapSearch',
        component: () => import('@/views/MapSearch.vue'),
        meta: { title: '地图找房' }
      },
      {
        path: 'tupai-map',
        name: 'TupaiMap',
        component: () => import('@/views/TupaiMap.vue'),
        meta: { title: '土拍地图' }
      },
      {
        path: 'loupan/:id',
        name: 'LoupanDetail',
        component: () => import('@/views/LoupanDetail.vue'),
        meta: { title: '楼盘详情' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '404' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - fang123` : 'fang123'
  if (to.meta.requiresAuth && !localStorage.getItem('token')) {
    next('/login')
  } else {
    next()
  }
})

export default router
