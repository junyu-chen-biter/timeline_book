<template>
  <div class="h-full overflow-auto bg-transparent p-8">
    <div class="w-full">
      <div class="flex justify-between items-center mb-6">
        <h1 class="text-3xl font-bold text-gray-800">👋 欢迎回来，开始今天的学习吧！</h1>
        <div class="flex gap-3">
          <el-button type="success" plain @click="showEventManager = true">添加普通事项</el-button>
          <el-button type="primary" plain @click="showSubjectManager = true">管理待学习科目</el-button>
        </div>
      </div>

      <!-- 统计卡片 -->
      <div class="mb-8 grid grid-cols-3 gap-6">
        <div class="rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
          <div class="mb-1 text-sm text-gray-500">待办总数</div>
          <div class="text-3xl font-bold text-blue-600">{{ recommendedItems.length }}</div>
        </div>
        <div class="rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
          <div class="mb-1 text-sm text-gray-500">高优先级任务</div>
          <div class="text-3xl font-bold text-orange-500">{{ topPriorityCount }}</div>
        </div>
        <div class="rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
          <div class="mb-1 text-sm text-gray-500">预计总耗时</div>
          <div class="text-3xl font-bold text-purple-600">{{ totalReviewTime }} min</div>
        </div>
      </div>

      <!-- 推荐复习区 -->
      <div class="space-y-6">
        <h2 class="font-bold text-gray-700 flex justify-between items-center">
          智能推荐任务列表
          <el-button size="small" :loading="loading" @click="fetchRecommended" circle><el-icon><Refresh /></el-icon></el-button>
        </h2>

        <div v-if="loading" class="p-8 text-center text-gray-400">加载中...</div>
        <div v-else-if="recommendedItems.length === 0" class="p-8 text-center text-gray-400">
          🎉 暂无推荐任务，或者请先创建科目/事项。
        </div>

        <div v-else class="grid grid-cols-1 gap-4">
          <div 
            v-for="item in recommendedItems" 
            :key="item.uniqueId"
            class="rounded-xl border shadow-sm overflow-hidden transition-all hover:shadow-md"
            :class="item.type === 'EVENT' ? 'bg-orange-50 border-orange-100' : 'bg-white border-gray-200'"
          >
            <!-- SUBJECT CARD -->
            <div v-if="item.type === 'SUBJECT'">
              <div 
                class="p-4 bg-gray-50 border-b border-gray-100 cursor-pointer flex items-center justify-between"
                @click="toggleExpand(item.subject.id)"
              >
                <div class="flex items-center gap-4">
                   <!-- Rank Index -->
                  <div class="text-xl font-bold text-gray-300 w-8 text-center">#{{ item.rank }}</div>
                  
                  <div>
                    <div class="flex items-center gap-2">
                      <el-tag size="small" effect="dark">科目</el-tag>
                      <h3 class="text-lg font-bold text-gray-800">{{ item.subject.name }}</h3>
                      <span :class="['text-xs px-2 py-0.5 rounded-full text-white', getDDLColor(item.subject.ddl)]">
                        DDL: {{ item.subject.ddl }}
                      </span>
                    </div>
                    <div class="mt-1 flex gap-3 text-xs text-gray-500">
                      <span>学分: {{ (item.subject.creditWeight * 100).toFixed(0) }}%</span>
                      <span>难度: {{ item.subject.difficulty }}/5</span>
                      <span>待复习: {{ item.files.length }} 个文件</span>
                      <span>预计: {{ item.totalReviewTimeMinutes }} 分钟</span>
                    </div>
                  </div>
                </div>

                <div class="text-right">
                  <div class="text-xs text-gray-400">推荐指数</div>
                  <div class="text-2xl font-bold text-blue-600">{{ item.priorityScore.toFixed(1) }}</div>
                </div>
              </div>

              <!-- Expanded File List -->
              <div v-if="expandedSubjects.has(item.subject.id)" class="bg-white">
                <ul class="divide-y divide-gray-50">
                  <li 
                    v-for="(fileItem, fIdx) in item.files" 
                    :key="fileItem.block.id"
                    class="flex items-center justify-between p-3 hover:bg-blue-50 cursor-pointer pl-16"
                    @click="handleSelect(fileItem.block)"
                  >
                    <div class="flex items-center gap-3">
                       <span class="text-gray-400 text-sm w-4">{{ fIdx + 1 }}.</span>
                       <div>
                         <div class="text-sm font-medium text-gray-700">{{ fileItem.block.name }}</div>
                         <div class="text-xs text-gray-400 flex gap-2">
                           <span>预计 {{ fileItem.block.estimatedReviewTimeMinutes || 30 }} min</span>
                           <span>{{ fileItem.scoreExplanation }}</span>
                         </div>
                       </div>
                    </div>
                    <div class="text-sm font-bold text-blue-500">
                      {{ fileItem.priorityScore.toFixed(1) }}
                    </div>
                  </li>
                </ul>
              </div>
            </div>

            <!-- EVENT CARD -->
            <div v-else-if="item.type === 'EVENT'" class="p-4 flex items-center justify-between">
               <div class="flex items-center gap-4">
                  <!-- Rank Index -->
                  <div class="text-xl font-bold text-gray-300 w-8 text-center">#{{ item.rank }}</div>
                  
                  <div>
                    <div class="flex items-center gap-2">
                      <el-tag size="small" type="warning" effect="dark">事项</el-tag>
                      <h3 class="text-lg font-bold text-gray-800">{{ item.event.title }}</h3>
                      <span :class="['text-xs px-2 py-0.5 rounded-full text-white', getDDLColor(item.event.deadline)]">
                        DDL: {{ item.event.deadline }}
                      </span>
                    </div>
                    <div class="mt-1 flex gap-3 text-xs text-gray-500">
                      <span>预计耗时: {{ item.event.duration }} min</span>
                      <span>重要性: {{ item.event.importance }}/5</span>
                      <span>{{ item.scoreExplanation }}</span>
                    </div>
                  </div>
               </div>

               <div class="flex items-center gap-6">
                 <div class="text-right">
                    <div class="text-xs text-gray-400">推荐指数</div>
                    <div class="text-2xl font-bold text-orange-600">{{ item.priorityScore.toFixed(1) }}</div>
                 </div>
                 <el-button type="success" size="small" circle @click="completeEvent(item.event.id)">
                   <el-icon><Check /></el-icon>
                 </el-button>
               </div>
            </div>

          </div>
        </div>
      </div>
    </div>

    <SubjectManager v-model="showSubjectManager" @change="fetchRecommended" />
    <EventManager v-model="showEventManager" @change="fetchRecommended" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue'
import axios from 'axios'
import dayjs from 'dayjs'
import { ArrowRight, Refresh, Check } from '@element-plus/icons-vue'
import { useBlockStore } from '../stores/blockStore'
import SubjectManager from './SubjectManager.vue'
import EventManager from './EventManager.vue'
import { ElMessage } from 'element-plus'

const blockStore = useBlockStore()
const recommendedItems = ref([])
const loading = ref(false)
const showSubjectManager = ref(false)
const showEventManager = ref(false)
const expandedSubjects = reactive(new Set())

const fetchRecommended = async () => {
  loading.value = true
  try {
    const [subRes, evtRes] = await Promise.all([
      axios.get('/api/subjects/recommended'),
      axios.get('/api/events/recommended')
    ])

    const subjects = subRes.data.map(i => ({ ...i, type: 'SUBJECT', uniqueId: 's-' + i.subject.id }))
    const events = evtRes.data.map(i => ({ ...i, type: 'EVENT', uniqueId: 'e-' + i.event.id }))

    // Merge and sort
    const all = [...subjects, ...events].sort((a, b) => b.priorityScore - a.priorityScore)

    // Add rank
    recommendedItems.value = all.map((item, index) => ({
      ...item,
      rank: index + 1
    }))

    // Default expand top 1 if subject
    if (recommendedItems.value.length > 0 && recommendedItems.value[0].type === 'SUBJECT') {
      expandedSubjects.add(recommendedItems.value[0].subject.id)
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const toggleExpand = (id) => {
  if (expandedSubjects.has(id)) {
    expandedSubjects.delete(id)
  } else {
    expandedSubjects.add(id)
  }
}

const getDDLColor = (ddl) => {
  if (!ddl) return 'bg-gray-400'
  const days = dayjs(ddl).diff(dayjs(), 'day')
  if (days < 0) return 'bg-red-500'
  if (days <= 3) return 'bg-red-500'
  if (days <= 7) return 'bg-orange-500'
  return 'bg-green-500'
}

const topPriorityCount = computed(() => {
  return recommendedItems.value.filter(i => i.priorityScore > 0.7).length
})

const totalReviewTime = computed(() => {
  return recommendedItems.value.reduce((acc, item) => {
    if (item.type === 'SUBJECT') return acc + item.totalReviewTimeMinutes
    if (item.type === 'EVENT') return acc + item.event.duration
    return acc
  }, 0)
})

const handleSelect = (block) => {
  blockStore.selectBlock(block)
}

const completeEvent = async (id) => {
  try {
    await axios.patch(`/api/events/${id}`, { status: 'COMPLETED' })
    ElMessage.success('任务完成！')
    fetchRecommended()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  fetchRecommended()
})
</script>

