<template>
  <div class="loupan-info-page">
    <!-- 面包屑 -->
    <div class="section-container py-4">
      <t-breadcrumb>
        <t-breadcrumb-item :to="`/loupan/${route.params.id}`">楼盘详情</t-breadcrumb-item>
        <t-breadcrumb-item>楼盘信息</t-breadcrumb-item>
      </t-breadcrumb>
    </div>

    <!-- 标题 -->
    <section class="bg-white border-b border-gray-50">
      <div class="section-container py-5">
        <h1 class="text-xl sm:text-2xl font-bold text-[var(--color-text-primary)] flex items-center gap-2">
          <Info class="w-6 h-6 text-[var(--color-primary)]" />
          楼盘信息
        </h1>
        <p v-if="loupan?.projectName" class="text-sm text-[var(--color-text-tertiary)] mt-1">{{ loupan.projectName }}</p>
      </div>
    </section>

    <section class="py-6 bg-[#F8FAFE] min-h-[60vh]">
      <div class="section-container">
        <div v-if="loading" class="text-center py-16"><t-loading /></div>
        <div v-else-if="!loupan" class="text-center py-16 text-[var(--color-text-tertiary)]">暂无楼盘信息</div>
        <div v-else class="bg-white rounded-xl border border-gray-100 p-5 sm:p-6">
          <LoupanInfoBlocks :loupan="loupan" :show-main-title="false" />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Info } from 'lucide-vue-next'
import request from '@/utils/request'
import LoupanInfoBlocks from '@/components/LoupanInfoBlocks.vue'

const route = useRoute()
const loupan = ref(null)
const loading = ref(true)

async function fetchInfo() {
  loading.value = true
  try {
    loupan.value = await request.get(`/public/loupans/${route.params.id}`)
  } catch {
    loupan.value = null
  } finally {
    loading.value = false
  }
}

onMounted(fetchInfo)

// 从别的楼盘详情跳到本页时组件会被复用，参数变化需重新拉取
watch(() => route.params.id, (id, old) => {
  if (id && id !== old) fetchInfo()
})
</script>
