<template>
  <div class="w-full h-full flex flex-col">
    <!-- PDF Preview -->
    <iframe 
      v-if="block.type === 'PDF'" 
      :src="fileUrl" 
      class="w-full h-full border-none"
    ></iframe>

    <!-- Markdown Preview -->
    <div 
      v-else-if="block.type === 'MARKDOWN'" 
      class="w-full h-full overflow-auto p-8 bg-white prose max-w-none"
      v-html="mdContent"
    ></div>

    <!-- PPT / Others -->
    <div v-else class="flex flex-col items-center justify-center h-full text-gray-500">
      <el-icon size="64" class="mb-4"><Document /></el-icon>
      <p class="text-lg">该文件类型不支持在线预览</p>
      <a :href="fileUrl" target="_blank" class="text-blue-500 hover:underline mt-2">
        点击下载/打开文件
      </a>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import MarkdownIt from 'markdown-it'
import axios from 'axios'
import { Document } from '@element-plus/icons-vue'

const props = defineProps({
  block: Object
})

const md = new MarkdownIt()
const mdContent = ref('')

const fileUrl = computed(() => {
  if (!props.block || !props.block.filePath) return ''
  const filename = props.block.filePath.split(/[\\/]/).pop()
  return `/api/blocks/files/${filename}`
})

const loadMarkdown = async () => {
  if (props.block && props.block.type === 'MARKDOWN') {
    try {
      const res = await axios.get(fileUrl.value)
      mdContent.value = md.render(res.data)
    } catch (e) {
      mdContent.value = '无法加载文件内容'
    }
  }
}

watch(() => props.block, loadMarkdown, { immediate: true })
</script>