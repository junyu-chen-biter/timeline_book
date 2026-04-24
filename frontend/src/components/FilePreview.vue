<template>
  <div class="preview-shell">
    <template v-if="block.type === 'PDF'">
      <div class="preview-toolbar">
        <div class="toolbar-group">
          <span class="toolbar-label">阅读模式</span>
          <el-button size="small" @click="setPdfZoom(90)">紧凑</el-button>
          <el-button size="small" @click="setPdfZoom(125)">标准</el-button>
          <el-button size="small" @click="setPdfZoom(160)">沉浸</el-button>
        </div>

        <div class="toolbar-group">
          <el-button size="small" @click="adjustPdfZoom(-10)">-</el-button>
          <span class="zoom-badge">{{ pdfZoom }}%</span>
          <el-button size="small" @click="adjustPdfZoom(10)">+</el-button>
          <el-button size="small" type="primary" plain @click="fitPdfWidth">
            适应宽度
          </el-button>
        </div>
      </div>

      <div class="preview-surface">
        <iframe :src="pdfUrl" class="pdf-frame"></iframe>
      </div>
    </template>

    <template v-else-if="block.type === 'MARKDOWN'">
      <div class="preview-toolbar">
        <div class="toolbar-group">
          <span class="toolbar-label">Markdown 文档</span>
          <span class="text-xs text-slate-500">优化排版、代码块和引用样式</span>
        </div>
      </div>

      <div class="preview-surface markdown-surface">
        <article class="markdown-card note-preview" v-html="mdContent"></article>
      </div>
    </template>

    <template v-else>
      <div class="unsupported-state">
        <el-icon size="64" class="mb-4 text-slate-400"><Document /></el-icon>
        <p class="text-lg font-semibold text-slate-700">该文件类型暂不支持在线预览</p>
        <p class="mt-2 text-sm text-slate-500">建议直接下载或调用系统应用打开</p>
        <a :href="fileUrl" target="_blank" class="open-link">
          点击下载/打开文件
        </a>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import MarkdownIt from "markdown-it";
import taskLists from "markdown-it-task-lists";
import axios from "axios";
import { Document } from "@element-plus/icons-vue";

const props = defineProps({
  block: Object,
});

const md = new MarkdownIt({
  linkify: true,
  breaks: true,
  html: true,
  typographer: true,
});

md.use(taskLists, { label: true, labelAfter: true });

const mdContent = ref("");
const pdfZoom = ref(125);

const fileUrl = computed(() => {
  if (!props.block || !props.block.filePath) return "";
  const filename = props.block.filePath.split(/[\\/]/).pop();
  return `/api/blocks/files/${filename}`;
});

const pdfUrl = computed(() => {
  if (!fileUrl.value) return "";
  return `${fileUrl.value}#zoom=${pdfZoom.value}&toolbar=0&navpanes=0`;
});

const setPdfZoom = (value) => {
  pdfZoom.value = value;
};

const fitPdfWidth = () => {
  pdfZoom.value = 140;
};

const adjustPdfZoom = (delta) => {
  pdfZoom.value = Math.min(220, Math.max(70, pdfZoom.value + delta));
};

const loadMarkdown = async () => {
  if (props.block?.type === "MARKDOWN") {
    try {
      const res = await axios.get(fileUrl.value);
      mdContent.value = md.render(res.data);
    } catch (e) {
      mdContent.value = "<p>无法加载文件内容</p>";
    }
  }
};

watch(
  () => props.block,
  (newBlock) => {
    if (newBlock?.type === "PDF") {
      pdfZoom.value = 125;
    }
    loadMarkdown();
  },
  { immediate: true },
);
</script>

<style scoped>
.preview-shell {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 18px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.85);
  background: rgba(255, 255, 255, 0.88);
}

.toolbar-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar-label {
  font-size: 12px;
  font-weight: 700;
  color: #334155;
  letter-spacing: 0.04em;
}

.zoom-badge {
  min-width: 56px;
  text-align: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.preview-surface {
  flex: 1;
  min-height: 0;
  padding: 16px;
}

.pdf-frame {
  width: 100%;
  height: 100%;
  border: none;
  border-radius: 20px;
  background: white;
  box-shadow: inset 0 0 0 1px rgba(226, 232, 240, 0.9);
}

.markdown-surface {
  overflow: auto;
}

.markdown-card {
  min-height: calc(100% - 2px);
  padding: 24px 28px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
}

.unsupported-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 32px;
}

.open-link {
  margin-top: 16px;
  color: #2563eb;
  text-decoration: none;
  font-weight: 600;
}

.open-link:hover {
  text-decoration: underline;
}

.note-preview :deep(h1),
.note-preview :deep(h2),
.note-preview :deep(h3) {
  color: #0f172a;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.note-preview :deep(pre) {
  border-radius: 18px;
  padding: 16px;
  overflow: auto;
}

.note-preview :deep(code:not(pre code)) {
  padding: 0.15rem 0.45rem;
  border-radius: 8px;
  background: #e2e8f0;
  color: #0f172a;
}

.note-preview :deep(blockquote) {
  border-left: 4px solid #3b82f6;
  background: #eff6ff;
  border-radius: 0 16px 16px 0;
  padding: 12px 16px;
}
</style>
