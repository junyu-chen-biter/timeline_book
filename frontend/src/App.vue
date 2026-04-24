<template>
  <div class="app-shell">
    <aside class="sidebar-panel shell-card" :style="{ width: `${leftPanelWidth}px` }">
      <div class="panel-header">
        <div>
          <p class="panel-kicker">Study Library</p>
          <h2 class="panel-title">资源库</h2>
        </div>
        <div class="flex items-center gap-2">
          <el-button
            size="small"
            :icon="FolderAdd"
            circle
            @click="handleCreateFolder"
          />
          <el-button
            size="small"
            :icon="Upload"
            circle
            @click="openUploadDialog"
          />
          <input
            type="file"
            ref="fileInput"
            class="hidden"
            @change="handleUpload"
          />
        </div>
      </div>

      <div class="px-4 pb-3 text-xs text-slate-500">
        按科目整理文件，点击即可预览，拖动分割线可以快速调整三栏宽度。
      </div>

      <el-scrollbar class="flex-1 px-2 pb-3">
        <el-tree
          :key="blockStore.refreshTreeTrigger"
          lazy
          :load="loadNode"
          :props="defaultProps"
          @node-click="handleNodeClick"
          highlight-current
          class="resource-tree"
        >
          <template #default="{ node, data }">
            <div class="custom-tree-node group">
              <span class="flex items-center gap-2 overflow-hidden">
                <el-icon v-if="data.type === 'FOLDER'"><Folder /></el-icon>
                <el-icon v-else-if="data.type === 'PDF'"><Document /></el-icon>
                <el-icon v-else-if="data.type === 'MARKDOWN'"><EditPen /></el-icon>
                <el-icon v-else><Files /></el-icon>
                <span class="truncate">{{ node.label }}</span>
              </span>
              <el-button
                type="danger"
                link
                :icon="Delete"
                size="small"
                class="opacity-0 group-hover:opacity-100 transition-opacity"
                @click.stop="handleDelete(data)"
              />
            </div>
          </template>
        </el-tree>
      </el-scrollbar>
    </aside>

    <div
      class="splitter"
      @mousedown="startResize('left', $event)"
      title="拖动调整资源栏宽度"
    ></div>

    <main class="content-region">
      <section class="shell-card preview-panel">
        <template v-if="blockStore.currentBlock">
          <div class="panel-header preview-header">
            <div class="min-w-0">
              <p class="panel-kicker">Document Focus</p>
              <h1 class="panel-title truncate">{{ blockStore.currentBlock.name }}</h1>
            </div>
            <div class="file-badge">
              {{ blockStore.currentBlock.type }}
            </div>
          </div>

          <div class="preview-body">
            <FilePreview :block="blockStore.currentBlock" />
          </div>
        </template>

        <template v-else>
          <Dashboard />
        </template>
      </section>

      <template v-if="blockStore.currentBlock">
        <div
          class="splitter"
          @mousedown="startResize('right', $event)"
          title="拖动调整笔记栏宽度"
        ></div>

        <aside
          class="shell-card note-panel"
          :style="{ width: `${rightPanelWidth}px` }"
        >
          <StudySidebar />
        </aside>
      </template>
    </main>
  </div>
  <!-- 上传前选择科目 -->
  <el-dialog
    v-model="showUploadDialog"
    title="选择科目与预计时长"
    width="420px"
  >
    <el-form label-width="100px">
      <el-form-item label="所属科目">
        <el-select
          v-model="selectedSubjectId"
          placeholder="请选择科目"
          filterable
        >
          <el-option
            v-for="s in subjectList"
            :key="s.id"
            :label="s.name"
            :value="s.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="预计复习时长">
        <el-input
          v-model.number="estimatedMinutes"
          placeholder="例如 30（分钟）"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmUpload"
          >继续选择文件</el-button
        >
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue";
import { useBlockStore } from "./stores/blockStore";
import {
  Folder,
  Document,
  EditPen,
  Files,
  FolderAdd,
  Upload,
  Delete,
} from "@element-plus/icons-vue";
import axios from "axios";
import { ElMessageBox, ElMessage } from "element-plus";
import FilePreview from "./components/FilePreview.vue";
import StudySidebar from "./components/StudySidebar.vue";
import Dashboard from "./components/Dashboard.vue";

const blockStore = useBlockStore();
const fileInput = ref(null);
let currentFolderId = null; // 用于记录当前选中的文件夹ID，以便上传到该文件夹
const showUploadDialog = ref(false);
const subjectList = ref([]);
const selectedSubjectId = ref(null);
const estimatedMinutes = ref(30);
const leftPanelWidth = ref(320);
const rightPanelWidth = ref(430);
const activeResizeSide = ref(null);

const defaultProps = {
  label: "name",
  children: "children",
  isLeaf: (data, node) => data.type !== "FOLDER",
};

// 懒加载逻辑
const loadNode = async (node, resolve) => {
  const parentId = node.level === 0 ? null : node.data.id;
  try {
    const res = await axios.get("/api/blocks", { params: { parentId } });
    resolve(res.data);
  } catch (e) {
    resolve([]);
  }
};

// 点击节点
const handleNodeClick = (data) => {
  if (data.type === "FOLDER") {
    currentFolderId = data.id;
  } else {
    blockStore.selectBlock(data);
    // 如果文件也有父级，也可以更新 currentFolderId (可选)
    if (data.parentId) {
      currentFolderId = data.parentId;
    }
  }
};

// 删除节点
const handleDelete = (data) => {
  ElMessageBox.confirm(
    "确定要删除吗？如果是文件夹，将删除其下所有内容。",
    "警告",
    {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    },
  )
    .then(() => {
      blockStore.deleteBlock(data.id);
    })
    .catch(() => {
      // cancel
    });
};

// 创建文件夹
const handleCreateFolder = async () => {
  try {
    const { value } = await ElMessageBox.prompt(
      "请输入文件夹名称",
      "新建文件夹",
      {
        confirmButtonText: "创建",
        cancelButtonText: "取消",
      },
    );
    if (value) {
      await blockStore.createFolder(value, currentFolderId);
      ElMessage.success("创建成功");
    }
  } catch (e) {
    // Cancelled
  }
};

// 上传文件
const openUploadDialog = async () => {
  try {
    const res = await axios.get("/api/subjects");
    subjectList.value = res.data;
  } catch (e) {
    subjectList.value = [];
  }
  selectedSubjectId.value = null;
  estimatedMinutes.value = 30;
  showUploadDialog.value = true;
};

const confirmUpload = () => {
  if (!selectedSubjectId.value) {
    ElMessage.error("请先选择科目");
    return;
  }
  showUploadDialog.value = false;
  fileInput.value.click();
};

const handleUpload = async (e) => {
  const file = e.target.files[0];
  if (!file) return;

  await blockStore.uploadFile(
    file,
    currentFolderId,
    selectedSubjectId.value,
    estimatedMinutes.value,
  );
  ElMessage.success("上传成功");
  // 清空 input 以便重复上传同名文件
  fileInput.value.value = "";
};

const clamp = (value, min, max) => {
  const safeMax = Math.max(min, max);
  return Math.min(Math.max(value, min), safeMax);
};

const startResize = (side, event) => {
  activeResizeSide.value = side;
  document.body.classList.add("is-resizing");
  event.preventDefault();
};

const handleMouseMove = (event) => {
  if (!activeResizeSide.value) return;

  const viewportWidth = window.innerWidth;
  const centerMinWidth = 520;
  const leftMin = 260;
  const leftMax = 460;
  const rightMin = 360;
  const rightMax = 620;

  if (activeResizeSide.value === "left") {
    const dynamicMax = viewportWidth - rightPanelWidth.value - centerMinWidth;
    leftPanelWidth.value = clamp(event.clientX, leftMin, Math.min(leftMax, dynamicMax));
  }

  if (activeResizeSide.value === "right" && blockStore.currentBlock) {
    const desiredWidth = viewportWidth - event.clientX;
    const dynamicMax = viewportWidth - leftPanelWidth.value - centerMinWidth;
    rightPanelWidth.value = clamp(desiredWidth, rightMin, Math.min(rightMax, dynamicMax));
  }
};

const stopResize = () => {
  activeResizeSide.value = null;
  document.body.classList.remove("is-resizing");
};

onMounted(async () => {
  window.addEventListener("mousemove", handleMouseMove);
  window.addEventListener("mouseup", stopResize);

  try {
    const res = await axios.get("/api/subjects");
    subjectList.value = res.data;
  } catch (e) {
    // ignore
  }
});

onBeforeUnmount(() => {
  window.removeEventListener("mousemove", handleMouseMove);
  window.removeEventListener("mouseup", stopResize);
  document.body.classList.remove("is-resizing");
});
</script>

<style scoped>
.app-shell {
  display: flex;
  width: 100%;
  height: 100vh;
  padding: 12px;
  gap: 0;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.12), transparent 30%),
    radial-gradient(circle at bottom right, rgba(14, 165, 233, 0.12), transparent 24%),
    #f3f6fb;
  box-sizing: border-box;
}

.shell-card {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(14px);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.08);
  overflow: hidden;
}

.sidebar-panel,
.note-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.content-region {
  flex: 1;
  min-width: 0;
  display: flex;
}

.preview-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.preview-header {
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
}

.preview-body {
  flex: 1;
  min-height: 0;
  background: linear-gradient(180deg, #f8fafc 0%, #eef4ff 100%);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
}

.panel-kicker {
  margin: 0 0 4px;
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #64748b;
}

.panel-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.file-badge {
  flex-shrink: 0;
  padding: 8px 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, #dbeafe 0%, #eef2ff 100%);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.splitter {
  width: 12px;
  flex-shrink: 0;
  position: relative;
  cursor: col-resize;
}

.splitter::before {
  content: "";
  position: absolute;
  left: 50%;
  top: 50%;
  width: 4px;
  height: 72px;
  transform: translate(-50%, -50%);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(148, 163, 184, 0.2), rgba(59, 130, 246, 0.45), rgba(148, 163, 184, 0.2));
  transition: transform 0.2s ease, background 0.2s ease;
}

.splitter:hover::before {
  transform: translate(-50%, -50%) scaleY(1.08);
  background: linear-gradient(180deg, rgba(96, 165, 250, 0.2), rgba(37, 99, 235, 0.7), rgba(96, 165, 250, 0.2));
}

.resource-tree :deep(.el-tree) {
  background: transparent;
}

.resource-tree :deep(.el-tree-node__content) {
  height: 40px;
  border-radius: 14px;
  margin: 2px 6px;
}

.resource-tree :deep(.el-tree-node__content:hover) {
  background: rgba(59, 130, 246, 0.08);
}

.resource-tree :deep(.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content) {
  background: linear-gradient(90deg, rgba(37, 99, 235, 0.16), rgba(14, 165, 233, 0.08));
  color: #0f172a;
}

.custom-tree-node {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-right: 8px;
  font-size: 13px;
  color: #334155;
}

:global(body.is-resizing) {
  cursor: col-resize;
  user-select: none;
}
</style>
