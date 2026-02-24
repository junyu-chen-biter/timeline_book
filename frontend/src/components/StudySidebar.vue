<template>
  <div class="flex flex-col h-full bg-white">
    <div class="p-4 border-b border-gray-100">
      <h2 class="font-bold text-gray-700 mb-2">学习助手</h2>
      <div class="text-xs text-gray-500 flex justify-between">
        <span>上次复习: {{ lastReviewTime }}</span>
        <span>次数: {{ reviewCount }}</span>
      </div>
    </div>

    <!-- Note Editor -->
    <div class="flex-1 p-4 flex flex-col">
      <label class="text-sm font-medium text-gray-600 mb-2">笔记 / 疑惑点</label>
      <textarea 
        v-model="noteContent"
        class="flex-1 w-full p-2 border border-gray-300 rounded resize-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-sm"
        placeholder="在这里记录你的想法..."
      ></textarea>
      <div class="mt-2 flex justify-end">
        <el-button type="primary" size="small" @click="handleSaveNote">保存笔记</el-button>
      </div>
    </div>

    <!-- Review Action -->
    <div class="p-4 border-t border-gray-100 bg-gray-50">
      <el-button type="success" class="w-full" size="large" @click="handleReview">
        ✅ 完成本次复习
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { useBlockStore } from '../stores/blockStore'
import dayjs from 'dayjs'

const blockStore = useBlockStore()
const noteContent = ref('')

// 同步 Store 中的记录到本地状态
watch(() => blockStore.currentRecord, (newRecord) => {
  if (newRecord) {
    noteContent.value = newRecord.note || ''
  } else {
    noteContent.value = ''
  }
}, { immediate: true })

const lastReviewTime = computed(() => {
  if (!blockStore.currentRecord?.lastReviewTime) return '从未'
  return dayjs(blockStore.currentRecord.lastReviewTime).format('MM-DD HH:mm')
})

const reviewCount = computed(() => blockStore.currentRecord?.reviewCount || 0)

const handleSaveNote = () => {
  blockStore.saveNote(noteContent.value)
}

const handleReview = () => {
  blockStore.reviewCheckIn()
}
</script>