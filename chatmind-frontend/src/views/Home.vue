<template>
  <div class="home">
    <a-layout style="min-height: 100vh">
      <a-layout-header class="header">
        <div class="logo">ChatMind</div>
        <a-button type="primary" @click="createDocument">
          <template #icon><PlusOutlined /></template>
          新建脑图
        </a-button>
      </a-layout-header>
      
      <a-layout-content class="content">
        <a-card title="我的文档" :bordered="false">
          <a-empty v-if="documents.length === 0" description="暂无文档，点击新建按钮创建第一个脑图">
            <a-button type="primary" @click="createDocument">立即创建</a-button>
          </a-empty>
          <a-list
            v-else
            :data-source="documents"
            :grid="{ gutter: 16, column: 4 }"
          >
            <template #renderItem="{ item }">
              <a-list-item>
                <a-card hoverable>
                  <template #cover>
                    <div class="doc-preview" @click="openDocument(item.id)">
                      <FileTextOutlined style="font-size: 48px; color: #1890ff;" />
                    </div>
                  </template>
                  <a-card-meta :title="item.title" @click="openDocument(item.id)">
                    <template #description>
                      {{ formatTime(item.updatedAt) }}
                    </template>
                  </a-card-meta>
                  <template #actions>
                    <a-popconfirm
                      title="确定删除此文档？"
                      ok-text="删除"
                      cancel-text="取消"
                      @confirm="deleteDocument(item.id)"
                    >
                      <DeleteOutlined key="delete" style="color: #ff4d4f;" />
                    </a-popconfirm>
                  </template>
                </a-card>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-layout-content>
    </a-layout>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { PlusOutlined, FileTextOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { documentApi } from '@/api/document'

const router = useRouter()
const documents = ref([])

const loadDocuments = async () => {
  try {
    const res = await documentApi.getUserDocuments(1) // TODO: 实际用户ID
    documents.value = res.data || []
    if (documents.value.length === 0) {
      console.log('暂无文档')
    }
  } catch (error) {
    console.error('加载文档失败:', error)
  }
}

const createDocument = async () => {
  try {
    const res = await documentApi.createDocument({
      title: '未命名文档',
      ownerId: 1 // TODO: 实际用户ID
    })
    router.push(`/editor/${res.data.id}`)
  } catch (error) {
    message.error('创建文档失败')
  }
}

const openDocument = (id) => {
  router.push(`/editor/${id}`)
}

const deleteDocument = async (id) => {
  try {
    await documentApi.deleteDocument(id)
    message.success('文档已删除')
    await loadDocuments()
  } catch (error) {
    message.error('删除失败')
  }
}

const formatTime = (timestamp) => {
  return new Date(timestamp).toLocaleString('zh-CN')
}

onMounted(() => {
  loadDocuments()
})
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  background: #001529;
}

.logo {
  font-size: 24px;
  font-weight: bold;
  color: #fff;
}

.content {
  padding: 24px;
  background: #f0f2f5;
}

.doc-preview {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 150px;
  background: #fafafa;
}
</style>
