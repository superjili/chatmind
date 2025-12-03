<template>
  <a-drawer
    v-model:open="visible"
    title="操作历史"
    placement="right"
    width="600"
    @close="handleClose"
  >
    <div class="operation-history">
      <!-- 筛选栏 -->
      <div class="filters">
        <a-space>
          <a-select 
            v-model:value="filterType" 
            style="width: 150px"
            placeholder="操作类型"
          >
            <a-select-option value="">全部类型</a-select-option>
            <a-select-option value="CREATE">创建</a-select-option>
            <a-select-option value="UPDATE">更新</a-select-option>
            <a-select-option value="DELETE">删除</a-select-option>
            <a-select-option value="MOVE">移动</a-select-option>
          </a-select>
          <a-button @click="loadOperations">刷新</a-button>
        </a-space>
      </div>

      <!-- 时间线展示 -->
      <a-spin :spinning="loading">
        <a-timeline class="operation-timeline">
          <a-timeline-item
            v-for="op in filteredOperations"
            :key="op.id"
            :color="getOperationColor(op.opType)"
          >
            <template #dot>
              <component :is="getOperationIcon(op.opType)" />
            </template>
            <div class="operation-item">
              <div class="operation-header">
                <span class="operation-type">{{ getOperationLabel(op.opType) }}</span>
                <span class="operation-time">{{ formatTime(op.createdAt) }}</span>
              </div>
              <div class="operation-content">
                <span class="operation-user">用户 #{{ op.userId }}</span>
                <span v-if="op.nodeId" class="operation-node">节点 #{{ op.nodeId }}</span>
              </div>
              <div v-if="op.opData" class="operation-data">
                {{ formatOpData(op.opData) }}
              </div>
            </div>
          </a-timeline-item>
        </a-timeline>
        
        <div v-if="operations.length === 0" class="empty-state">
          <a-empty description="暂无操作记录" />
        </div>
      </a-spin>
    </div>
  </a-drawer>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { 
  PlusOutlined, 
  EditOutlined, 
  DeleteOutlined, 
  DragOutlined 
} from '@ant-design/icons-vue'
import { operationApi } from '@/api/operation'
import dayjs from 'dayjs'

const props = defineProps({
  modelValue: Boolean,
  documentId: {
    type: [String, Number],
    required: true
  }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const operations = ref([])
const filterType = ref('')

const filteredOperations = computed(() => {
  if (!filterType.value) return operations.value
  return operations.value.filter(op => op.opType === filterType.value)
})

const loadOperations = async () => {
  loading.value = true
  try {
    const res = await operationApi.getRecentOperations(props.documentId, 100)
    operations.value = res.data || []
  } catch (error) {
    message.error('加载操作历史失败')
  } finally {
    loading.value = false
  }
}

const getOperationColor = (opType) => {
  const colors = {
    CREATE: 'green',
    UPDATE: 'blue',
    DELETE: 'red',
    MOVE: 'orange'
  }
  return colors[opType] || 'gray'
}

const getOperationIcon = (opType) => {
  const icons = {
    CREATE: PlusOutlined,
    UPDATE: EditOutlined,
    DELETE: DeleteOutlined,
    MOVE: DragOutlined
  }
  return icons[opType] || EditOutlined
}

const getOperationLabel = (opType) => {
  const labels = {
    CREATE: '创建节点',
    UPDATE: '更新节点',
    DELETE: '删除节点',
    MOVE: '移动节点'
  }
  return labels[opType] || opType
}

const formatTime = (timeStr) => {
  return dayjs(timeStr).format('YYYY-MM-DD HH:mm:ss')
}

const formatOpData = (dataStr) => {
  try {
    const data = JSON.parse(dataStr)
    if (data.content) return `内容: ${data.content}`
    if (data.parentId) return `父节点: #${data.parentId}`
    return JSON.stringify(data)
  } catch {
    return dataStr
  }
}

const handleClose = () => {
  visible.value = false
}

watch(visible, (newVal) => {
  if (newVal) {
    loadOperations()
  }
})
</script>

<style scoped>
.operation-history {
  padding: 16px 0;
}

.filters {
  margin-bottom: 24px;
}

.operation-timeline {
  margin-top: 16px;
}

.operation-item {
  padding: 8px 0;
}

.operation-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.operation-type {
  font-weight: 600;
  font-size: 14px;
}

.operation-time {
  color: #8c8c8c;
  font-size: 12px;
}

.operation-content {
  display: flex;
  gap: 12px;
  color: #595959;
  font-size: 13px;
  margin-bottom: 4px;
}

.operation-data {
  color: #8c8c8c;
  font-size: 12px;
  font-style: italic;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
}
</style>
