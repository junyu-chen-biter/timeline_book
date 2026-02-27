<template>
  <el-dialog
    v-model="visible"
    title="待学习科目管理"
    width="600px"
    :before-close="handleClose"
  >
    <!-- Add/Edit Form -->
    <el-form :model="form" label-width="100px" ref="formRef" :rules="rules" class="mb-6 border-b border-gray-100 pb-6">
      <el-form-item label="科目名称" prop="name">
        <el-input v-model="form.name" placeholder="例如：离散数学" />
      </el-form-item>
      <el-form-item label="DDL日期" prop="ddl">
        <el-date-picker
          v-model="form.ddl"
          type="date"
          placeholder="选择截止日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="学分占比" prop="creditWeight">
            <el-input-number v-model="form.creditWeight" :min="0" :max="100" :precision="1" />
            <span class="ml-2 text-gray-400">%</span>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="难度" prop="difficulty">
            <el-rate v-model="form.difficulty" :max="5" />
          </el-form-item>
        </el-col>
      </el-row>
      <div class="flex justify-end gap-2">
        <el-button @click="resetForm">重置</el-button>
        <el-button type="primary" @click="submitForm">
          {{ isEditing ? '保存修改' : '添加科目' }}
        </el-button>
      </div>
    </el-form>

    <!-- List -->
    <el-table :data="subjectList" style="width: 100%" height="300px">
      <el-table-column prop="name" label="科目" />
      <el-table-column label="DDL" width="120">
        <template #default="{ row }">
          <span :class="getDDLClass(row.ddl)">{{ row.ddl }}</span>
        </template>
      </el-table-column>
      <el-table-column label="学分" width="80">
        <template #default="{ row }">
          {{ (row.creditWeight * 100).toFixed(0) }}%
        </template>
      </el-table-column>
      <el-table-column label="难度" width="100">
        <template #default="{ row }">
          <el-rate :model-value="row.difficulty" disabled size="small" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const props = defineProps({
  modelValue: Boolean
})
const emit = defineEmits(['update:modelValue', 'change'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const subjectList = ref([])
const formRef = ref(null)
const isEditing = ref(false)
const editingId = ref(null)

const form = reactive({
  name: '',
  ddl: '',
  creditWeight: 10,
  difficulty: 3
})

const rules = {
  name: [{ required: true, message: '请输入科目名称', trigger: 'blur' }],
  ddl: [{ required: true, message: '请选择截止日期', trigger: 'change' }],
  creditWeight: [{ required: true, message: '请输入学分占比', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }]
}

const fetchSubjects = async () => {
  try {
    const res = await axios.get('/api/subjects')
    subjectList.value = res.data
  } catch (e) {
    console.error(e)
  }
}

const handleClose = () => {
  visible.value = false
}

const resetForm = () => {
  form.name = ''
  form.ddl = ''
  form.creditWeight = 10
  form.difficulty = 3
  isEditing.value = false
  editingId.value = null
}

const handleEdit = (row) => {
  form.name = row.name
  form.ddl = row.ddl
  form.creditWeight = row.creditWeight * 100
  form.difficulty = row.difficulty
  isEditing.value = true
  editingId.value = row.id
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该科目吗？删除后相关文件将变为未归类。', '提示', {
      type: 'warning'
    })
    await axios.delete(`/api/subjects/${row.id}`)
    ElMessage.success('删除成功')
    fetchSubjects()
    emit('change')
  } catch (e) {
    // cancel
  }
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const payload = {
          ...form,
          creditWeight: form.creditWeight / 100 // Convert back to 0-1
        }
        
        if (isEditing.value) {
          await axios.patch(`/api/subjects/${editingId.value}`, payload)
          ElMessage.success('更新成功')
        } else {
          await axios.post('/api/subjects', payload)
          ElMessage.success('创建成功')
        }
        resetForm()
        fetchSubjects()
        emit('change')
      } catch (e) {
        ElMessage.error(e.response?.data?.message || '操作失败')
      }
    }
  })
}

const getDDLClass = (ddl) => {
  const days = dayjs(ddl).diff(dayjs(), 'day')
  if (days < 0) return 'text-red-500 font-bold'
  if (days <= 7) return 'text-orange-500 font-bold'
  return 'text-green-600'
}

onMounted(() => {
  fetchSubjects()
})

// Expose fetch method to parent if needed
defineExpose({ fetchSubjects })
</script>
