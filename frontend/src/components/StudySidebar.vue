<template>
  <div class="study-shell">
    <div class="study-header">
      <div>
        <p class="study-kicker">Learning Workspace</p>
        <h2 class="study-title">学习助手</h2>
      </div>

      <div class="stats-pill">
        <span>上次复习 {{ lastReviewTime }}</span>
        <span>累计 {{ reviewCount }} 次</span>
      </div>
    </div>

    <div class="study-topbar">
      <div class="subject-card">
        <label class="field-label">所属科目</label>
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

      <div class="progress-card">
        <div class="flex items-center justify-between text-xs text-slate-500">
          <span>复习进度</span>
          <span class="font-semibold text-slate-700">{{ reviewProgress }}%</span>
        </div>
        <el-slider
          v-model="reviewProgress"
          :min="10"
          :step="10"
          show-stops
          size="small"
        />
        <div class="flex gap-2">
          <el-button type="success" class="flex-1" @click="handleReview">
            完成复习
          </el-button>
          <el-button type="warning" class="flex-1" @click="handleMaster">
            彻底掌握
          </el-button>
        </div>
      </div>
    </div>

    <div class="editor-shell">
      <div class="editor-header">
        <div>
          <label class="field-label">笔记 / 疑惑点</label>
          <p class="text-xs text-slate-500">
            支持 Markdown，也支持简单颜色与高亮排版。
          </p>
        </div>
        <div class="view-switch">
          <el-segmented v-model="editorView" :options="viewOptions" size="small" />
        </div>
      </div>

      <div class="toolbar-wrap">
        <div class="editor-toolbar">
          <el-button size="small" @click="wrapSelection('# ', '', '标题')">H1</el-button>
          <el-button size="small" @click="wrapSelection('## ', '', '小节')">H2</el-button>
          <el-button size="small" @click="wrapSelection('**', '**', '重点')">加粗</el-button>
          <el-button size="small" @click="wrapSelection('`', '`', '代码')">代码</el-button>
          <el-button size="small" @click="wrapSelection('> ', '', '结论')">引用</el-button>
          <el-button size="small" @click="wrapSelection('- ', '', '条目')">列表</el-button>
          <el-button size="small" @click="wrapSelection('- [ ] ', '', '待确认问题')">任务</el-button>
          <el-button size="small" @click="wrapSelection('<mark>', '</mark>', '高亮内容')">高亮</el-button>
          <el-button size="small" @click="wrapSelection('<span style=&quot;color:#dc2626&quot;>', '</span>', '红色重点')">
            红字
          </el-button>
          <el-button size="small" @click="insertTemplate(exampleTemplate)">模板</el-button>
        </div>

        <el-button type="primary" @click="handleSaveNote">
          保存笔记
        </el-button>
      </div>

      <div class="editor-layout" :class="layoutClass">
        <div v-if="showEditor" class="editor-pane">
          <textarea
            ref="noteEditorRef"
            v-model="noteContent"
            class="editor-textarea"
            placeholder="把今天学到的内容、疑惑点、题目结论都记在这里..."
          ></textarea>
        </div>

        <div v-if="showPreview" class="preview-pane">
          <div class="preview-header-pill">实时预览</div>
          <div class="preview-content note-preview" v-html="renderedContent"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted, nextTick } from "vue";
import { useBlockStore } from "../stores/blockStore";
import dayjs from "dayjs";
import axios from "axios";
import { ElMessage } from "element-plus";

import MarkdownIt from "markdown-it";
import taskLists from "markdown-it-task-lists";
import hljs from "highlight.js";
import "highlight.js/styles/github.css";

const blockStore = useBlockStore();
const noteContent = ref("");
const reviewProgress = ref(100);
const subjectList = ref([]);
const currentSubjectId = ref(null);
const editorView = ref("split");
const noteEditorRef = ref(null);
const viewOptions = [
  { label: "编辑", value: "edit" },
  { label: "分屏", value: "split" },
  { label: "预览", value: "preview" },
];

const exampleTemplate = `# 今日学习总结

## 核心概念
- 结论 1
- 结论 2

## 易错点
- [ ] 还需要回顾的知识点

> 把真正容易忘的内容单独写一行，复习时会更快。
`;

const md = new MarkdownIt({
  linkify: true,
  breaks: true,
  html: true,
  typographer: true,
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(str, {
          language: lang,
          ignoreIllegals: true,
        }).value}</code></pre>`;
      } catch (__) {}
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`;
  },
});
md.use(taskLists, { label: true, labelAfter: true });

const renderedContent = computed(() => md.render(noteContent.value || ""));
const showEditor = computed(() => editorView.value !== "preview");
const showPreview = computed(() => editorView.value !== "edit");
const layoutClass = computed(() =>
  editorView.value === "split" ? "is-split" : "is-single",
);

const fetchSubjects = async () => {
  try {
    const res = await axios.get("/api/subjects");
    subjectList.value = res.data;
  } catch (e) {
    console.error(e);
  }
};

watch(
  () => blockStore.currentBlock,
  (newBlock) => {
    if (newBlock) {
      currentSubjectId.value = newBlock.subjectId || null;
    } else {
      currentSubjectId.value = null;
    }
  },
  { immediate: true },
);

watch(
  () => blockStore.currentRecord,
  (newRecord) => {
    if (newRecord) {
      noteContent.value = newRecord.note || "";
      reviewProgress.value = newRecord.lastReviewProgress || 100;
      editorView.value = newRecord.note ? "split" : "edit";
    } else {
      noteContent.value = "";
      reviewProgress.value = 100;
      editorView.value = "edit";
    }
  },
  { immediate: true },
);

const lastReviewTime = computed(() => {
  if (!blockStore.currentRecord?.lastReviewTime) return "从未";
  return dayjs(blockStore.currentRecord.lastReviewTime).format("MM-DD HH:mm");
});

const reviewCount = computed(() => blockStore.currentRecord?.reviewCount || 0);

const handleSubjectChange = async (val) => {
  if (!blockStore.currentBlock) return;
  try {
    await axios.patch(`/api/blocks/${blockStore.currentBlock.id}`, {
      subjectId: val,
    });
    blockStore.currentBlock.subjectId = val;
    ElMessage.success("科目已更新");
    blockStore.triggerRefresh();
  } catch (e) {
    ElMessage.error("更新科目失败");
  }
};

const replaceSelection = (nextText, selectionStart, selectionEnd) => {
  noteContent.value =
    noteContent.value.slice(0, selectionStart) +
    nextText +
    noteContent.value.slice(selectionEnd);
};

const wrapSelection = async (prefix, suffix = "", fallback = "内容") => {
  const textarea = noteEditorRef.value;
  if (!textarea) {
    editorView.value = "edit";
    await nextTick();
  }

  const editor = noteEditorRef.value;
  if (!editor) return;

  const selectionStart = editor.selectionStart;
  const selectionEnd = editor.selectionEnd;
  const selectedText = noteContent.value.slice(selectionStart, selectionEnd) || fallback;
  const nextText = `${prefix}${selectedText}${suffix}`;

  replaceSelection(nextText, selectionStart, selectionEnd);

  await nextTick();
  editor.focus();
  editor.setSelectionRange(selectionStart + prefix.length, selectionStart + prefix.length + selectedText.length);
};

const insertTemplate = async (template) => {
  const textarea = noteEditorRef.value;
  if (!textarea) {
    editorView.value = "edit";
    await nextTick();
  }

  noteContent.value = noteContent.value
    ? `${noteContent.value.trimEnd()}\n\n${template}`
    : template;

  await nextTick();
  noteEditorRef.value?.focus();
};

const handleSaveNote = async () => {
  await blockStore.saveNote(noteContent.value);
  editorView.value = "split";
};

const handleReview = async () => {
  await blockStore.reviewCheckIn(reviewProgress.value, false);
};

const handleMaster = async () => {
  await blockStore.reviewCheckIn(100, true);
};

onMounted(() => {
  fetchSubjects();
});
</script>

<style scoped>
.study-shell {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(248, 250, 252, 0.98));
}

.study-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 20px 12px;
}

.study-kicker {
  margin: 0 0 6px;
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #64748b;
}

.study-title {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
}

.stats-pill {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(241, 245, 249, 0.95);
  font-size: 12px;
  color: #475569;
}

.study-topbar {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 12px;
  padding: 0 20px 14px;
}

.subject-card,
.progress-card {
  padding: 14px;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  background: white;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.progress-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.field-label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.editor-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 0 20px 20px;
}

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 8px 0 12px;
}

.view-switch {
  flex-shrink: 0;
}

.toolbar-wrap {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 20px;
  background: rgba(248, 250, 252, 0.95);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.editor-toolbar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.editor-layout {
  flex: 1;
  min-height: 0;
  display: grid;
  gap: 12px;
  padding-top: 12px;
}

.editor-layout.is-split {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
}

.editor-layout.is-single {
  grid-template-columns: minmax(0, 1fr);
}

.editor-pane,
.preview-pane {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-radius: 22px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  background: white;
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.05);
  overflow: hidden;
}

.editor-textarea {
  flex: 1;
  width: 100%;
  resize: none;
  border: none;
  outline: none;
  padding: 18px 18px 24px;
  font-size: 14px;
  line-height: 1.75;
  color: #0f172a;
  background:
    linear-gradient(transparent 31px, rgba(226, 232, 240, 0.5) 32px),
    linear-gradient(90deg, rgba(219, 234, 254, 0.3), rgba(219, 234, 254, 0.3));
  background-size: 100% 32px, 100% 100%;
  font-family: "Consolas", "Courier New", monospace;
}

.preview-header-pill {
  padding: 12px 16px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.95);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.preview-content {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 18px 20px 24px;
  color: #0f172a;
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

@media (max-width: 1200px) {
  .study-topbar,
  .editor-layout.is-split {
    grid-template-columns: 1fr;
  }

  .toolbar-wrap {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
