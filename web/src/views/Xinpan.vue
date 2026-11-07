<template>
  <div class="xinpan-page">
    <!-- 标题区 -->
    <section class="bg-white border-b border-gray-50">
      <div class="section-container py-5">
        <h1 class="text-xl sm:text-2xl font-bold text-[var(--color-text-primary)] flex items-center gap-2">
          <Building2 class="w-6 h-6 text-[var(--color-primary)]" />
          新盘
        </h1>
        <p class="text-sm text-[var(--color-text-tertiary)] mt-1">杭州全域在售新盘，可按行政区、板块、楼盘类型筛选</p>
      </div>
    </section>

    <!-- 筛选 + 楼盘列表（与首页共用同一组件） -->
    <LoupanListSection :show-keyword="true" />
  </div>
</template>

<script setup>
import { Building2 } from 'lucide-vue-next'
import LoupanListSection from '@/components/LoupanListSection.vue'
</script>
