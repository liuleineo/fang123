<template>
  <div class="web-layout min-h-screen flex flex-col">
    <!-- 顶栏 -->
    <header class="fixed top-0 left-0 right-0 h-[var(--header-height)] bg-white/80 backdrop-blur-md border-b border-gray-100 z-50 transition-shadow">
      <div class="section-container h-full flex items-center justify-between">
        <!-- Logo -->
        <router-link to="/" class="flex items-center gap-2.5 no-underline">
          <div class="w-9 h-9 rounded-lg bg-gradient-to-br from-[#0052D9] to-[#618DFF] flex items-center justify-center text-white font-bold text-base">
            杭
          </div>
          <span class="text-xl font-semibold text-[var(--color-text-primary)]">
            杭房一二三 
          </span>
        </router-link>

        <!-- PC 导航 -->
        <nav class="hidden md:flex items-center gap-1">
          <a
            v-for="item in navItems"
            :key="item.label"
            :href="item.href"
            class="px-4 py-2 text-sm text-[var(--color-text-secondary)] hover:text-[var(--color-primary)] rounded-[var(--radius-btn)] hover:bg-blue-50/50 transition-all"
          >
            {{ item.label }}
          </a>
          
        </nav>

        <!-- 右侧操作 -->
        <div class="flex items-center gap-3">
        
          <!-- 已登录：我的（下拉收纳 客户管理/任务中心/我的任务/会员中心/个人中心） -->
          <template v-if="isLoggedIn">
            <div ref="userMenuRef" class="relative hidden sm:block">
              <button
                class="inline-flex items-center gap-1.5 px-3 py-2 text-sm rounded-[var(--radius-btn)] transition-all"
                :class="userMenuOpen
                  ? 'text-[var(--color-primary)] bg-blue-50/60'
                  : 'text-[var(--color-text-secondary)] hover:text-[var(--color-primary)] hover:bg-gray-50'"
                @click="userMenuOpen = !userMenuOpen"
              >
                <User class="w-4 h-4" />
                我的
                <ChevronDown
                  class="w-3.5 h-3.5 transition-transform duration-200"
                  :class="userMenuOpen ? 'rotate-180' : ''"
                />
              </button>

              <transition name="user-dropdown">
                <div
                  v-if="userMenuOpen"
                  class="absolute right-0 top-[calc(100%+8px)] w-44 py-1.5 bg-white rounded-xl border border-gray-100 shadow-[0_8px_24px_rgba(0,0,0,0.08)] z-50"
                >
                  <router-link
                    v-for="item in userMenuItems"
                    :key="item.to"
                    :to="item.to"
                    class="flex items-center gap-2.5 px-4 py-2.5 text-sm transition-colors"
                    :class="isMenuActive(item.to)
                      ? 'text-[var(--color-primary)] bg-blue-50/60'
                      : 'text-[var(--color-text-secondary)] hover:text-[var(--color-primary)] hover:bg-blue-50/40'"
                    @click="userMenuOpen = false"
                  >
                    <component :is="item.icon" class="w-4 h-4 shrink-0" />
                    {{ item.label }}
                  </router-link>
                  <div class="my-1.5 border-t border-gray-100"></div>
                  <button
                    class="w-full flex items-center gap-2.5 px-4 py-2.5 text-sm text-[var(--color-danger)] hover:bg-red-50/60 transition-colors"
                    @click="userMenuOpen = false; handleLogout()"
                  >
                    <LogOut class="w-4 h-4 shrink-0" />
                    退出登录
                  </button>
                </div>
              </transition>
            </div>
          </template>
          <!-- 未登录 -->
          <template v-else>
            <router-link
              to="/login"
              class="hidden sm:inline-flex items-center gap-1.5 px-4 py-2 text-sm text-[var(--color-text-primary)] rounded-[var(--radius-btn)] hover:bg-gray-50 transition-all"
            >
              登录
            </router-link>
            <router-link
              to="/register"
              class="hidden sm:inline-flex items-center gap-1.5 px-4 py-2 text-sm text-white bg-[var(--color-primary)] rounded-[var(--radius-btn)] hover:bg-[var(--color-primary-hover)] transition-all font-medium"
            >
              注册
            </router-link>
          </template>
          <!-- 移动端菜单按钮 -->
          <button
            class="md:hidden p-2 text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] transition-colors"
            @click="mobileMenuOpen = !mobileMenuOpen"
          >
            <Menu v-if="!mobileMenuOpen" class="w-6 h-6" />
            <X v-else class="w-6 h-6" />
          </button>
        </div>
      </div>

      <!-- 移动端下拉菜单 -->
      <transition name="fade">
        <div v-if="mobileMenuOpen" class="md:hidden bg-white border-t border-gray-50 shadow-lg">
          <div class="section-container py-4 flex flex-col gap-2">
            <a
              v-for="item in navItems"
              :key="item.label"
              :href="item.href"
              class="px-4 py-3 text-sm text-[var(--color-text-secondary)] hover:bg-blue-50/50 rounded-lg transition-colors"
              @click="mobileMenuOpen = false"
            >
              {{ item.label }}
            </a>
            <div class="border-t border-gray-50 pt-2 mt-1">
              <template v-if="isLoggedIn">
                <!-- 移动端「我的」折叠分组 -->
                <button
                  class="w-full flex items-center justify-between px-4 py-3 text-sm text-[var(--color-text-secondary)] hover:bg-blue-50/50 rounded-lg transition-colors"
                  @click="mobileUserOpen = !mobileUserOpen"
                >
                  <span class="flex items-center gap-2">
                    <User class="w-4 h-4" />我的
                  </span>
                  <ChevronDown
                    class="w-4 h-4 transition-transform duration-200"
                    :class="mobileUserOpen ? 'rotate-180' : ''"
                  />
                </button>
                <div v-if="mobileUserOpen" class="pl-3 ml-4 border-l border-gray-100">
                  <router-link
                    v-for="item in userMenuItems"
                    :key="item.to"
                    :to="item.to"
                    class="flex items-center gap-2 px-3 py-2.5 text-sm text-[var(--color-text-secondary)] hover:bg-blue-50/50 rounded-lg transition-colors"
                    @click="mobileMenuOpen = false"
                  >
                    <component :is="item.icon" class="w-4 h-4 shrink-0" />
                    {{ item.label }}
                  </router-link>
                  <button class="w-full flex items-center gap-2 px-3 py-2.5 text-sm text-[var(--color-danger)] hover:bg-red-50/50 rounded-lg transition-colors" @click="handleLogout(); mobileMenuOpen = false">
                    <LogOut class="w-4 h-4 shrink-0" />退出登录
                  </button>
                </div>
              </template>
              <template v-else>
                <router-link to="/login" class="block px-4 py-3 text-sm text-[var(--color-text-secondary)] hover:bg-blue-50/50 rounded-lg transition-colors" @click="mobileMenuOpen = false">
                  登录
                </router-link>
                <router-link to="/register" class="block px-4 py-3 text-sm text-[var(--color-primary)] hover:bg-blue-50/50 rounded-lg transition-colors font-medium" @click="mobileMenuOpen = false">
                  注册
                </router-link>
              </template>
            </div>
          </div>
        </div>
      </transition>
    </header>

    <!-- 主内容 -->
    <main class="flex-1 pt-[var(--header-height)]">
      <router-view v-slot="{ Component }">
        <!-- key=路由全路径：强制重建组件实例，避免复用导致数据不刷新。
             注意：不用 <transition mode="out-in">——多根组件（如楼盘详情页 v-if/v-else）在过渡卸载时会挂起，导致切换后页面空白 -->
        <component :is="Component" :key="route.fullPath" />
      </router-view>
    </main>

    <!-- 底栏 -->
    <footer class="bg-[#1D2129] text-white/70 py-12">
      <div class="section-container">
        <div class="grid grid-cols-2 md:grid-cols-4 gap-8 mb-8">
          <div>
            <h4 class="text-white font-medium mb-4 text-sm">找房工具</h4>
            <ul class="space-y-2">
             
              <li><a href="/map" class="text-sm text-white/50 hover:text-white transition-colors">地图找房</a></li>
              <li><a href="/school-map" class="text-sm text-white/50 hover:text-white transition-colors">学区地图</a></li>
              <li><a href="/tupai-map" class="text-sm text-white/50 hover:text-white transition-colors">土拍地图</a></li>
              
            </ul>
          </div>

          <div>
            <h4 class="text-white font-medium mb-4 text-sm">行情信息</h4>
            <ul class="space-y-2">
             
              <li><a href="/" class="text-sm text-white/50 hover:text-white transition-colors">真实成交</a></li>
              <li><a href="/" class="text-sm text-white/50 hover:text-white transition-colors">成交行情</a></li>
              
            </ul>
          </div>
         
        </div>
        <div class="border-t border-white/10 pt-6 flex flex-col sm:flex-row items-center justify-between gap-4">
            <div class="flex items-center gap-2">
            <div class="w-6 h-6 rounded bg-gradient-to-br from-[#0052D9] to-[#618DFF] flex items-center justify-center text-white text-xs font-bold">N</div>
            <span class="text-xs text-white/40">hangfang123.com 杭房一二三 - 让买房变简单</span>
          </div>
          <span class="text-xs text-white/30">浙ICP备2021037366号-2</span>
           <span class="text-xs text-white/30">powered by 公子鱼</span>
           <span class="text-xs text-white/30">隐私政策/服务条款</span>

        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Menu, X, User, LogOut, ChevronDown, Users, ListChecks, BadgeCheck, Crown } from 'lucide-vue-next'
import { MessagePlugin } from 'tdesign-vue-next'

const router = useRouter()
const route = useRoute()
const mobileMenuOpen = ref(false)
const userMenuOpen = ref(false)
const mobileUserOpen = ref(false)
const userMenuRef = ref(null)
const loggedIn = ref(!!localStorage.getItem('token'))

const isLoggedIn = computed(() => loggedIn.value)

// 「我的」下拉菜单项：原平铺导航统一收纳至此
const userMenuItems = [
  { label: '客户管理', to: '/customers', icon: Users },
  { label: '任务中心', to: '/tasks', icon: ListChecks },
  { label: '我的任务', to: '/my-tasks', icon: BadgeCheck },
  { label: '会员中心', to: '/member', icon: Crown },
  { label: '个人中心', to: '/profile', icon: User }
]

function isMenuActive(to) {
  return route.path === to || route.path.startsWith(to + '/')
}

// 监听路由变化，更新登录状态并关闭菜单
watch(() => router.currentRoute.value.path, () => {
  loggedIn.value = !!localStorage.getItem('token')
  userMenuOpen.value = false
  mobileMenuOpen.value = false
})

// 点击外部 / 按 Esc 关闭下拉菜单
function handleDocumentClick(e) {
  if (userMenuOpen.value && userMenuRef.value && !userMenuRef.value.contains(e.target)) {
    userMenuOpen.value = false
  }
}

function handleKeydown(e) {
  if (e.key === 'Escape') {
    userMenuOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
  document.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  document.removeEventListener('keydown', handleKeydown)
})

function handleLogout() {
  localStorage.removeItem('token')
  loggedIn.value = false
  MessagePlugin.success('已退出登录')
  router.push('/')
}

const navItems = [
  { label: '首页', href: '/' },
  { label: '新盘', href: '/xinpan' },
  { label: '新盘信息', href: '/xinpanlist' },
  { label: '二手房', href: '/ershoufang' },
  { label: '地图找房', href: '/map' },
  { label: '土拍地图', href: '/tupai-map' },
  { label: '学校地图', href: '/school-map' },
]
</script>

<style scoped>
.user-dropdown-enter-active,
.user-dropdown-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.user-dropdown-enter-from,
.user-dropdown-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
