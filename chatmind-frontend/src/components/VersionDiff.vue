<template>
  <a-modal
    v-model:open="visible"
    title="版本差异对比"
    width="800px"
    :footer="null"
  >
    <div class="version-diff">
      <!-- 版本选择 -->
      <div class="version-selector">
        <a-space>
          <div>
            <label>源版本:</label>
            <a-select 
              v-model:value="fromVersionId" 
              style="width: 200px"
              placeholder="选择源版本"
              @change="handleCompare"
            >
              <a-select-option 
                v-for="v in versions" 
                :key="v.id" 
                :value="v.id"
              >
                v{{ v.versionNumber }} - {{ v.versionName }}
              </a-select-option>
            </a-select>
          </div>
          <a-button type="text" disabled>→</a-button>
          <div>
            <label>目标版本:</label>
            <a-select 
              v-model:value="toVersionId" 
              style="width: 200px"
              placeholder="选择目标版本"
              @change="handleCompare"
            >
              <a-select-option value="current">当前版本</a-select-option>
              <a-select-option 
                v-for="v in versions" 
                :key="v.id" 
                :value="v.id"
              >
                v{{ v.versionNumber }} - {{ v.versionName }}
              </a-select-option>
            </a-select>
          </div>
        </a-space>
      </div>

      <!-- 差异统计 -->
      <div v-if="diffData" class="diff-stats">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-statistic 
              title="新增" 
              :value="diffData.stats?.addedCount || 0" 
              :value-style="{ color: '#52c41a' }"
            />
          </a-col>
          <a-col :span="6">
            <a-statistic 
              title="删除" 
              :value="diffData.stats?.removedCount || 0" 
              :value-style="{ color: '#f5222d' }"
            />
          </a-col>
          <a-col :span="6">
            <a-statistic 
              title="更新" 
              :value="diffData.stats?.updatedCount || 0" 
              :value-style="{ color: '#1890ff' }"
            />
          </a-col>
          <a-col :span="6">
            <a-statistic 
              title="移动" 
              :value="diffData.stats?.movedCount || 0" 
              :value-style="{ color: '#faad14' }"
            />
          </a-col>
        </a-row>
      </div>

      <!-- 差异详情 -->
      <a-spin :spinning="loading">
        <div v-if="diffData" class="diff-details">
          <a-tabs>
            <a-tab-pane key="added" tab="新增节点">
              <a-list 
                :data-source="diffData.addedNodes || []" 
                size="small"
              >
                <template #renderItem="{ item }">
                  <a-list-item>
                    <a-tag color="green">+</a-tag>
                    <span>{{ item.content }}</span>
                  </a-list-item>
                </template>
              </a-list>
              <a-empty v-if="!diffData.addedNodes?.length" description="无新增节点" />
            </a-tab-pane>

            <a-tab-pane key="removed" tab="删除节点">
              <a-list 
                :data-source="diffData.removedNodes || []" 
                size="small"
              >
                <template #renderItem="{ item }">
                  <a-list-item>
                    <a-tag color="red">-</a-tag>
                    <span>{{ item.content }}</span>
                  </a-list-item>
                </template>
              </a-list>
              <a-empty v-if="!diffData.removedNodes?.length" description="无删除节点" />
            </a-tab-pane>

            <a-tab-pane key="updated" tab="更新节点">
              <a-list 
                :data-source="diffData.updatedNodes || []" 
                size="small"
              >
                <template #renderItem="{ item }">
                  <a-list-item>
                    <a-tag color="blue">~</a-tag>
                    <div class="updated-node">
                      <div class="old-content">旧: {{ item.oldContent }}</div>
                      <div class="new-content">新: {{ item.newContent }}</div>
                    </div>
                  </a-list-item>
                </template>
              </a-list>
              <a-empty v-if="!diffData.updatedNodes?.length" description="无更新节点" />
            </a-tab-pane>

            <a-tab-pane key="moved" tab="移动节点">
              <a-list 
                :data-source="diffData.movedNodes || []" 
                size="small"
              >
                <template #renderItem="{ item }">
                  <a-list-item>
                    <a-tag color="orange">↔</a-tag>
                    <div class="moved-node">
                      <span>{{ item.content }}</span>
                      <span class="move-info">
                        从 #{{ item.oldParentId }} → #{{ item.newParentId }}
                      </span>
                    </div>
                  </a-list-item>
                </template>
              </a-list>
              <a-empty v-if="!diffData.movedNodes?.length" description="无移动节点" />
            </a-tab-pane>
          </a-tabs>
        </div>
      </a-spin>
    </div>
  </a-modal>
</template>

<script setup>
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { diffApi } from '@/api/operation'

const props = defineProps({
  modelValue: Boolean,
  documentId: {
    type: [String, Number],
    required: true
  },
  versions: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const fromVersionId = ref(null)
const toVersionId = ref('current')
const diffData = ref(null)

const handleCompare = async () => {
  if (!fromVersionId.value) {
    message.warning('请选择源版本')
    return
  }

  loading.value = true
  diffData.value = null

  try {
    let res
    if (toVersionId.value === 'current') {
      res = await diffApi.compareWithCurrent(props.documentId, fromVersionId.value)
    } else {
      res = await diffApi.compareVersions(fromVersionId.value, toVersionId.value)
    }
    diffData.value = res.data
  } catch (error) {
    message.error('对比失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.version-diff {
  padding: 16px 0;
}

.version-selector {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.version-selector label {
  margin-right: 8px;
  color: #595959;
}

.diff-stats {
  margin-bottom: 24px;
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
}

.diff-details {
  margin-top: 16px;
}

.updated-node {
  flex: 1;
}

.old-content {
  color: #f5222d;
  text-decoration: line-through;
  margin-bottom: 4px;
}

.new-content {
  color: #52c41a;
}

.moved-node {
  display: flex;
  justify-content: space-between;
  width: 100%;
}

.move-info {
  color: #8c8c8c;
  font-size: 12px;
}
</style>
