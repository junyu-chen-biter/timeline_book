<template>
  <div class="flex h-screen w-full bg-gray-50 overflow-hidden">
    
    <!-- Left Sidebar: Resource Tree -->
    <div class="w-72 bg-white border-r border-gray-200 flex flex-col">
      <div class="p-4 border-b border-gray-100 flex justify-between items-center">
        <h2 class="font-bold text-gray-700">资源库</h2>
        <div class="space-x-2">
          <el-button size="small" :icon="FolderAdd" circle @click="handleCreateFolder" />
          <el-button size="small" :icon="Upload" circle @click="triggerUpload" />
          <input type="file" ref="fileInput" class="hidden" @change="handleUpload" />
        </div>
      </div>
      
      <el-scrollbar class="flex-1">
        <el-tree
          :key="blockStore.refreshTreeTrigger"
          lazy
          :load="loadNode"
          :props="defaultProps"
          @node-click="handleNodeClick"
          highlight-current
        >
          <template #default="{ node, data }">
            <span class="custom-tree-node flex items-center gap-2">
              <el-icon v-if="data.type === 'FOLDER'"><Folder /></el-icon>
              <el-icon v-else-if="data.type === 'PDF'"><Document /></el-icon>
              <el-icon v-else-if="data.type === 'MARKDOWN'"><EditPen /></el-icon>
              <el-icon v-else><Files /></el-icon>
              <span class="truncate">{{ node.label }}</span>
            </span>
          </template>
        </el-tree>
      </el-scrollbar>
    </div>

    <!-- Center: Content Preview -->
    <div class="flex-1 flex flex-col overflow-hidden bg-gray-100 relative">
      <div v-if="blockStore.currentBlock" class="h-full flex flex-col">
        <!-- Toolbar -->
        <div class="h-12 bg-white border-b border-gray-200 flex items-center px-4 shadow-sm">
          <h1 class="text-lg font-medium text-gray-800">{{ blockStore.currentBlock.name }}</h1>
        </div>
        
        <!-- Preview Area -->
        <div class="flex-1 overflow-auto p-0 flex justify-center items-center bg-gray-50">
           <FilePreview :block="blockStore.currentBlock" />
        </div>
      </div>
      
      <div v-else class="h-full flex flex-col">
        <!-- Dashboard when no file selected -->
        <Dashboard />
      </div>
    </div>

    <!-- Right Sidebar: Study Tools -->
    <div class="w-96 bg-white border-l border-gray-200 flex flex-col shadow-xl z-10" v-if="blockStore.currentBlock">
      <StudySidebar />
    </div>
    <div v-else class="w-0 overflow-hidden transition-all duration-300"></div>

  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useBlockStore } from './stores/blockStore'
import { Folder, Document, EditPen, Files, FolderAdd, Upload } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessageBox, ElMessage } from 'element-plus'
import FilePreview from './components/FilePreview.vue'
import StudySidebar from './components/StudySidebar.vue'
import Dashboard from './components/Dashboard.vue'

const blockStore = useBlockStore()
const fileInput = ref(null)
let currentFolderId = null // 用于记录当前选中的文件夹ID，以便上传到该文件夹

const defaultProps = {
  label: 'name',
  children: 'children',
  isLeaf: (data, node) => data.type !== 'FOLDER'
}

// 懒加载逻辑
const loadNode = async (node, resolve) => {
  const parentId = node.level === 0 ? null : node.data.id
  try {
    const res = await axios.get('/api/blocks', { params: { parentId } })
    resolve(res.data)
  } catch (e) {
    resolve([])
  }
}

// 点击节点
const handleNodeClick = (data) => {
  if (data.type === 'FOLDER') {
    currentFolderId = data.id
  } else {
    // 如果是文件，设置当前预览
    blockStore.selectBlock(data)
    // 如果文件也有父级，也可以更新 currentFolderId (可选)
    currentFolderId = data.parentId
  }
}

// 创建文件夹
const handleCreateFolder = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入文件夹名称', '新建文件夹', {
      confirmButtonText: '创建',
      cancelButtonText: '取消',
    })
    if (value) {
      await blockStore.createFolder(value, currentFolderId)
      ElMessage.success('创建成功')
    }
  } catch (e) {
    // Cancelled
  }
}

// 上传文件
const triggerUpload = () => {
  fileInput.value.click()
}

const handleUpload = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  
  await blockStore.uploadFile(file, currentFolderId)
  ElMessage.success('上传成功')
  // 清空 input 以便重复上传同名文件
  fileInput.value.value = ''
}
</script>

<style scoped>
.custom-tree-node {
  font-size: 14px;
}
</style>