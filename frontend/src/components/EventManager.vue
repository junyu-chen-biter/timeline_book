<template>
  <el-dialog
    v-model="visible"
    title="添加/管理普通事件"
    width="600px"
    @close="handleClose"
  >
    <!-- Form -->
    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" class="mb-6">
      <el-form-item label="事件名称" prop="title">
        <el-input v-model="form.title" placeholder="例如：取快递、锻炼身体" />
      </el-form-item>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="截止日期" prop="deadline">
            <el-date-picker
              v-model="form.deadline"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="预计耗时" prop="duration">
            <el-input-number v-model="form.duration" :min="5" :step="5" label="分钟" style="width: 100%" />
            <span class="ml-2 text-gray-400 text-xs">分钟</span>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="重要性" prop="importance">
        <el-rate v-model="form.importance" :max="5" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSubmit">{{ isEditing ? '保存修改' : '立即创建' }}</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- List -->
    <el-table :data="eventList" style="width: 100%" height="300px">
      <el-table-column prop="title" label="事件" />
      <el-table-column label="DDL" width="120">
        <template #default="{ row }">
          <span :class="getDDLClass(row.deadline)">{{ row.deadline }}</span>
        </template>
      </el-table-column>
      <el-table-column label="耗时" width="80">
        <template #default="{ row }">
          {{ row.duration }}m
        </template>
      </el-table-column>
      <el-table-column label="重要" width="100">
        <template #default="{ row }">
          <el-rate :model-value="row.importance" disabled size="small" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="success" size="small" @click="handleComplete(row)">完成</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
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

const eventList = ref([])
const formRef = ref(null)
const isEditing = ref(false)
const editingId = ref(null)

const form = reactive({
  title: '',
  deadline: '',
  importance: 3,
  duration: 30
})

const rules = {
  title: [{ required: true, message: '请输入事件名称', trigger: 'blur' }],
  deadline: [{ required: true, message: '请选择截止日期', trigger: 'change' }],
  importance: [{ required: true, message: '请选择重要性', trigger: 'change' }],
  duration: [{ required: true, message: '请输入预计耗时', trigger: 'change' }]
}

const fetchEvents = async () => {
  try {
    const res = await axios.get('/api/events')
    // Filter out completed if needed, but API returns all?
    // Backend controller returns all, but repo has findByStatus.
    // Let's filter client side or assume backend list endpoint returns all.
    // Actually, usually we want to manage pending ones here.
    eventList.value = res.data.filter(e => e.status === 'PENDING')
  } catch (e) {
    console.error(e)
  }
}

watch(visible, (val) => {
  if (val) {
    fetchEvents()
  }
})

const handleClose = () => {
  visible.value = false
}

const resetForm = () => {
  form.title = ''
  form.deadline = ''
  form.importance = 3
  form.duration = 30
  isEditing.value = false
  editingId.value = null
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEditing.value) {
          await axios.patch(`/api/events/${editingId.value}`, form)
          ElMessage.success('修改成功')
        } else {
          await axios.post('/api/events', form)
          ElMessage.success('创建成功')
        }
        resetForm()
        fetchEvents()
        emit('change')
      } catch (e) {
        ElMessage.error('操作失败')
      }
    }
  })
}

const handleComplete = async (row) => {
  try {
    await axios.patch(`/api/events/${row.id}`, { status: 'COMPLETED' })
    ElMessage.success('已标记为完成')
    fetchEvents()
    emit('change')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该事件吗？', '提示', { type: 'warning' })
    await axios.delete(`/api/events/${row.id}`)
    ElMessage.success('删除成功')
    fetchEvents()
    emit('change')
  } catch (e) {
    // cancel
  }
}

const getDDLClass = (ddl) => {
  const days = dayjs(ddl).diff(dayjs(), 'day')
  if (days < 0) return 'text-red-500 font-bold'
  if (days <= 3) return 'text-orange-500'
  return 'text-green-600'
}
</script>
