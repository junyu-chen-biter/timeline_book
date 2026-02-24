<template>
  <div class="h-full overflow-auto bg-gray-50 p-8">
    <div class="mx-auto max-w-4xl">
      <h1 class="mb-6 text-3xl font-bold text-gray-800">👋 欢迎回来，开始今天的学习吧！</h1>

      <!-- 统计卡片 (Mock Data) -->
      <div class="mb-8 grid grid-cols-3 gap-6">
        <div class="rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
          <div class="mb-1 text-sm text-gray-500">今日待复习</div>
          <div class="text-3xl font-bold text-blue-600">{{ recommendedList.length }}</div>
        </div>
        <div class="rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
          <div class="mb-1 text-sm text-gray-500">已完成</div>
          <div class="text-3xl font-bold text-green-600">0</div>
        </div>
        <div class="rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
          <div class="mb-1 text-sm text-gray-500">总文件数</div>
          <div class="text-3xl font-bold text-purple-600">-</div>
        </div>
      </div>

      <!-- 推荐列表 -->
      <div class="overflow-hidden rounded-xl border border-gray-100 bg-white shadow-sm">
        <div class="flex items-center justify-between border-b border-gray-100 p-4">
          <h2 class="font-bold text-gray-700">智能推荐复习任务</h2>
          <el-button size="small" :loading="loading" @click="fetchRecommended">刷新</el-button>
        </div>
        
        <div v-if="loading" class="p-8 text-center text-gray-400">
          加载中...
        </div>

        <div v-else-if="recommendedList.length === 0" class="p-8 text-center text-gray-400">
          🎉 太棒了，所有复习任务都已完成！
        </div>

        <ul v-else class="divide-y divide-gray-100">
          <li 
            v-for="(item, index) in recommendedList" 
            :key="item.block.id"
            class="group flex cursor-pointer items-center justify-between p-4 transition-colors hover:bg-blue-50"
            @click="handleSelect(item.block)"
          >
            <div class="flex items-center gap-4">
              <span class="w-8 text-center text-2xl font-bold text-gray-200 group-hover:text-blue-200">
                {{ index + 1 }}
              </span>
              <div>
                <div class="font-medium text-gray-800">{{ item.block.name }}</div>
                <div class="mt-1 flex gap-2 text-xs text-gray-500">
                  <span v-if="item.lastReviewTime">上次复习: {{ formatDate(item.lastReviewTime) }}</span>
                  <span v-else class="text-orange-500">从未复习</span>
                  <span class="rounded bg-gray-100 px-1 text-gray-400">{{ item.scoreExplanation }}</span>
                </div>
              </div>
            </div>
            
            <div class="flex items-center gap-4">
              <!-- 优先级分数展示 -->
              <div class="text-right">
                <div class="text-xs text-gray-400">推荐指数</div>
                <div class="font-bold text-blue-600">{{ item.priorityScore.toFixed(1) }}</div>
              </div>
              <el-icon class="text-gray-300"><ArrowRight /></el-icon>
            </div>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import dayjs from 'dayjs'
import { ArrowRight } from '@element-plus/icons-vue'
import { useBlockStore } from '../stores/blockStore'

const blockStore = useBlockStore()
const recommendedList = ref([])
const loading = ref(false)

const fetchRecommended = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/blocks/recommended')
    recommendedList.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const formatDate = (date) => {
  return dayjs(date).format('MM-DD HH:mm')
}

const handleSelect = (block) => {
  blockStore.selectBlock(block)
}

onMounted(() => {
  fetchRecommended()
})
</script>