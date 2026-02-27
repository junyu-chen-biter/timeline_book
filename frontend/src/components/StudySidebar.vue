<template>
  <div class="flex flex-col h-full bg-white">
    <div class="p-4 border-b border-gray-100">
      <h2 class="font-bold text-gray-700 mb-2">学习助手</h2>
      
      <!-- Subject Selection -->
      <div class="mb-3">
        <label class="text-xs text-gray-400 mb-1 block">所属科目</label>
        <el-select 
          v-model="currentSubjectId" 
          placeholder="选择科目" 
          size="small" 
          filterable
          @change="handleSubjectChange"
          class="w-full"
        >
          <el-option 
            v-for="s in subjectList" 
            :key="s.id" 
            :label="s.name" 
            :value="s.id" 
          />
        </el-select>
      </div>

      <div class="text-xs text-gray-500 flex justify-between">
        <span>上次复习: {{ lastReviewTime }}</span>
        <span>次数: {{ reviewCount }}</span>
      </div>
    </div>

    <!-- Action Area -->
    <div class="p-3 bg-gray-50 border-b border-gray-100 flex flex-col gap-3">
      <!-- Progress Slider -->
      <div class="px-1">
        <div class="flex justify-between text-xs text-gray-500 mb-1">
          <span>复习进度</span>
          <span>{{ reviewProgress }}%</span>
        </div>
        <el-slider v-model="reviewProgress" :min="10" :step="10" show-stops size="small" />
      </div>

      <div class="flex gap-2">
        <el-button type="success" class="flex-1" @click="handleReview">
          ✅ 完成复习
        </el-button>
        <el-button type="warning" class="flex-1" @click="handleMaster">
          🏆 彻底掌握
        </el-button>
      </div>
      
      <el-button type="primary" class="w-full" @click="handleSaveNote">
        💾 保存笔记
      </el-button>
    </div>

    <!-- Note Editor -->
    <div class="flex-1 p-3 flex flex-col overflow-hidden">
      <label class="text-xs font-bold text-gray-500 mb-2 uppercase tracking-wide">笔记 / 疑惑点</label>
      <textarea 
        v-model="noteContent"
        class="flex-1 w-full p-3 border border-gray-200 rounded-lg resize-none focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none text-sm transition-all shadow-sm"
        placeholder="在这里记录你的想法..."
      ></textarea>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import { useBlockStore } from '../stores/blockStore'
import dayjs from 'dayjs'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const blockStore = useBlockStore()
const noteContent = ref('')
const reviewProgress = ref(100)
const subjectList = ref([])
const currentSubjectId = ref(null)

// Fetch subjects
const fetchSubjects = async () => {
  try {
    const res = await axios.get('/api/subjects')
    subjectList.value = res.data
  } catch (e) {
    console.error(e)
  }
}

// Watch current block to update subject selection
watch(() => blockStore.currentBlock, (newBlock) => {
  if (newBlock) {
    currentSubjectId.value = newBlock.subjectId || null
  } else {
    currentSubjectId.value = null
  }
}, { immediate: true })

// Sync record to local state
watch(() => blockStore.currentRecord, (newRecord) => {
  if (newRecord) {
    noteContent.value = newRecord.note || ''
    reviewProgress.value = newRecord.lastReviewProgress || 100
  } else {
    noteContent.value = ''
    reviewProgress.value = 100
  }
}, { immediate: true })

const lastReviewTime = computed(() => {
  if (!blockStore.currentRecord?.lastReviewTime) return '从未'
  return dayjs(blockStore.currentRecord.lastReviewTime).format('MM-DD HH:mm')
})

const reviewCount = computed(() => blockStore.currentRecord?.reviewCount || 0)

const handleSubjectChange = async (val) => {
  if (!blockStore.currentBlock) return
  try {
    await axios.patch(`/api/blocks/${blockStore.currentBlock.id}`, {
      subjectId: val
    })
    // Update local store manually to reflect change without full refresh
    blockStore.currentBlock.subjectId = val
    ElMessage.success('科目已更新')
    blockStore.triggerRefresh() // Refresh tree/list if needed
  } catch (e) {
    ElMessage.error('更新科目失败')
  }
}

const handleSaveNote = async () => {
  await blockStore.saveNote(noteContent.value)
}

const handleReview = async () => {
  await blockStore.reviewCheckIn(reviewProgress.value, false)
}

const handleMaster = async () => {
  await blockStore.reviewCheckIn(100, true)
}

onMounted(() => {
  fetchSubjects()
})
</script>