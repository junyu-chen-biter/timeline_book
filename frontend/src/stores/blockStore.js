import { defineStore } from 'pinia'
import axios from 'axios'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

export const useBlockStore = defineStore('block', () => {
  const currentBlock = ref(null) // 当前选中的文件（用于预览）
  const refreshTreeTrigger = ref(0) // 用于触发树更新
  const currentRecord = ref(null)

  // 获取学习记录
  const loadRecord = async (blockId) => {
    currentRecord.value = null // Reset
    try {
      const res = await axios.get(`/api/study/${blockId}`)
      currentRecord.value = res.data
    } catch (e) {
      console.error("Failed to load record", e)
    }
  }

  // 选中文件
  const selectBlock = (block) => {
    currentBlock.value = block
    loadRecord(block.id)
  }

  // 刷新树
  const triggerRefresh = () => {
    refreshTreeTrigger.value++
  }

  // 上传文件
  const uploadFile = async (file, parentId, subjectId, estimatedReviewTimeMinutes) => {
    const formData = new FormData()
    formData.append('file', file)
    if (parentId) {
      formData.append('parentId', parentId)
    }
    if (subjectId) {
      formData.append('subjectId', subjectId)
    }
    if (estimatedReviewTimeMinutes) {
      formData.append('estimatedReviewTimeMinutes', estimatedReviewTimeMinutes)
    }

    try {
      await axios.post('/api/blocks/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      triggerRefresh()
    } catch (error) {
      console.error("Upload failed", error)
      ElMessage.error('上传失败')
    }
  }

  // 创建文件夹
  const createFolder = async (name, parentId) => {
    try {
      await axios.post('/api/blocks/folder', {
        name,
        parentId
      })
      triggerRefresh()
    } catch (error) {
      console.error("Create folder failed", error)
      ElMessage.error('创建文件夹失败')
    }
  }

  // 删除文件/文件夹
  const deleteBlock = async (id) => {
    try {
      await axios.delete(`/api/blocks/${id}`)
      ElMessage.success('删除成功')
      triggerRefresh() // Refresh the tree
      // If the deleted block was selected, clear selection
      if (currentBlock.value && currentBlock.value.id === id) {
        currentBlock.value = null
      }
    } catch (error) {
      console.error("Delete failed", error)
      ElMessage.error('删除失败')
    }
  }

  // 保存笔记
  const saveNote = async (note) => {
    if (!currentBlock.value) return
    try {
      await axios.post(`/api/study/${currentBlock.value.id}/note`, note, {
        headers: { 'Content-Type': 'text/plain' }
      })
      if (currentRecord.value) currentRecord.value.note = note
      ElMessage.success('笔记已保存')
    } catch (e) {
      ElMessage.error('保存失败')
    }
  }

  // 打卡
  const reviewCheckIn = async (progress = 100, isMastered = false) => {
    if (!currentBlock.value) return
    try {
      const res = await axios.post(`/api/study/${currentBlock.value.id}/review`, {
        progress,
        isMastered
      })
      currentRecord.value = res.data
      ElMessage.success('复习打卡成功！')
    } catch (e) {
      ElMessage.error('打卡失败')
    }
  }

  return {
    currentBlock,
    selectBlock,
    refreshTreeTrigger,
    triggerRefresh,
    uploadFile,
    createFolder,
    deleteBlock,
    currentRecord,
    loadRecord,
    saveNote,
    reviewCheckIn
  }
})
