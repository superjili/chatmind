<template>
  <div class="editor">
    <div class="toolbar">
      <a-space>
        <a-button @click="$router.back()">
          <template #icon><ArrowLeftOutlined /></template>
          返回
        </a-button>
        <a-input 
          v-model:value="documentTitle" 
          @blur="handleSaveTitle"
          style="width: 300px"
          placeholder="文档标题"
        />
        <a-button type="primary" @click="aiDialogVisible = true">
          <template #icon><RobotOutlined /></template>
          AI生成
        </a-button>
        <a-button @click="saveDocument">
          <template #icon><SaveOutlined /></template>
          保存
        </a-button>
        <a-button @click="openVersionModal">
          <template #icon><HistoryOutlined /></template>
          历史版本
        </a-button>
        <a-button @click="operationHistoryVisible = true">
          <template #icon><ClockCircleOutlined /></template>
          操作历史
        </a-button>
        <a-button @click="diffModalVisible = true">
          <template #icon><DiffOutlined /></template>
          差异对比
        </a-button>
        <a-dropdown>
          <a-button>
            <template #icon><ExportOutlined /></template>
            导出
          </a-button>
          <template #overlay>
            <a-menu @click="handleExport">
              <a-menu-item key="markdown">Markdown</a-menu-item>
              <a-menu-item key="json">JSON</a-menu-item>
              <a-menu-item key="opml">OPML</a-menu-item>
              <a-menu-divider />
              <a-menu-item key="png">PNG图片</a-menu-item>
              <a-menu-item key="svg">SVG图片</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <a-button @click="importDialogVisible = true">
          <template #icon><ImportOutlined /></template>
          导入
        </a-button>
        <a-button @click="searchModalVisible = true">
          <template #icon><SearchOutlined /></template>
          搜索
        </a-button>
        
        <!-- 节点统计 -->
        <a-tag v-if="nodeCount > 100" color="blue">
          已加载: {{ loadedNodeIds.size }} / {{ nodeCount }}
        </a-tag>
        
        <!-- 在线用户 -->
        <div class="online-users">
          <a-avatar-group :max-count="5">
            <a-avatar 
              v-for="user in onlineUsers" 
              :key="user.userId"
              :style="{ backgroundColor: user.color }"
            >
              {{ user.username.charAt(0) }}
            </a-avatar>
          </a-avatar-group>
          <span v-if="isConnected" class="online-status">
            <span class="status-dot"></span>
            在线
          </span>
        </div>
      </a-space>
    </div>
    
    <div class="editor-main">
      <MindMapGraph
        ref="mindMapGraphRef"
        :data="treeData"
        :view-state="viewState"
        @node-click="handleNodeClick"
        @node-contextmenu="handleNodeContextMenu"
        @viewport-change="handleViewportChange"
        @node-expand="handleNodeExpand"
      />
      
      <!-- 协作光标和选中高亮 -->
      <CollaborationCursors
        :online-users="onlineUsers"
        :current-user-id="userId"
        :node-positions="nodePositions"
      />
    </div>

    <!-- AI生成对话框 -->
    <AIGenerateDialog
      v-model="aiDialogVisible"
      :generating="generating"
      :thinking-steps="thinkingSteps"
      @generate="handleGenerate"
    />

    <!-- 右键菜单 -->
    <NodeContextMenu
      :visible="contextMenuVisible"
      :x="contextMenuX"
      :y="contextMenuY"
      :node="selectedNode"
      @close="contextMenuVisible = false"
      @add-child="addChildNode"
      @ai-expand="expandNodeWithAI"
      @delete="deleteNode"
    />

    <!-- 操作历史 -->
    <OperationHistory
      v-model="operationHistoryVisible"
      :document-id="documentId"
    />

    <!-- 版本差异对比 -->
    <VersionDiff
      v-model="diffModalVisible"
      :document-id="documentId"
      :versions="versionList"
    />

    <!-- 搜索对话框 -->
    <a-modal
      v-model:open="searchModalVisible"
      title="搜索节点"
      :footer="null"
      width="600px"
    >
      <a-input-search
        v-model:value="searchKeyword"
        placeholder="输入关键词搜索"
        size="large"
        @search="handleSearch"
        :loading="searchLoading"
      />
      <a-list
        v-if="searchResults.length > 0"
        :data-source="searchResults"
        class="search-results"
      >
        <template #renderItem="{ item }">
          <a-list-item @click="handleSearchResultClick(item)">
            <a-list-item-meta>
              <template #title>
                <span v-html="highlightKeyword(item.content, searchKeyword)"></span>
              </template>
              <template #description>
                路径: {{ item.path || '根节点' }}
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
      <a-empty v-else-if="searchKeyword && !searchLoading" description="未找到匹配结果" />
    </a-modal>

    <!-- 导入对话框 -->
    <a-modal
      v-model:open="importDialogVisible"
      title="导入文档"
      @ok="handleImport"
      :confirm-loading="importLoading"
    >
      <a-form layout="vertical">
        <a-form-item label="导入格式">
          <a-select v-model:value="importFormat">
            <a-select-option value="markdown">Markdown</a-select-option>
            <a-select-option value="opml">OPML</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="文档内容">
          <a-textarea
            v-model:value="importContent"
            :rows="10"
            placeholder="粘贴文档内容..."
          />
        </a-form-item>
        <a-form-item label="文档标题">
          <a-input v-model:value="importTitle" placeholder="输入标题" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="versionModalVisible"
      title="历史版本"
      :footer="null"
      width="600px"
    >
      <a-table
        :data-source="versionList"
        :loading="versionLoading"
        :row-key="'id'"
        size="small"
        :pagination="false"
      >
        <a-table-column title="版本号" data-index="versionNumber" />
        <a-table-column title="名称" data-index="versionName" />
        <a-table-column title="类型" data-index="versionType" />
        <a-table-column title="节点数" data-index="nodeCount" />
        <a-table-column title="操作" key="action">
          <template #default="{ record }">
            <a-button type="link" @click="restoreVersion(record)">
              恢复
            </a-button>
          </template>
        </a-table-column>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, RobotOutlined, SaveOutlined, HistoryOutlined, ClockCircleOutlined, DiffOutlined, ExportOutlined, SearchOutlined, ImportOutlined } from '@ant-design/icons-vue'
import MindMapGraph from '@/components/MindMapGraph.vue'
import AIGenerateDialog from '@/components/AIGenerateDialog.vue'
import NodeContextMenu from '@/components/NodeContextMenu.vue'
import OperationHistory from '@/components/OperationHistory.vue'
import VersionDiff from '@/components/VersionDiff.vue'
import CollaborationCursors from '@/components/CollaborationCursors.vue'
import { useCollaboration } from '@/composables/useCollaboration'
import { useDocument } from '@/composables/useDocument'
import { useAIGenerate } from '@/composables/useAIGenerate'
import { nodeApi } from '@/api/node'
import { aiApi } from '@/api/ai'
import { versionApi } from '@/api/version'
import { exportApi, searchApi } from '@/api/export'

const route = useRoute()
const documentId = route.params.id
const userId = 1

const { onlineUsers, isConnected, setFocusNode, setSelectedNodes } = useCollaboration(documentId, userId, '用户' + userId)

const { 
  documentTitle, 
  treeData, 
  viewState,
  nodeCount,
  loadedNodeIds,
  loadDocument,
  loadSubtree, 
  saveTitle, 
  saveViewState 
} = useDocument(documentId)

const { generating, thinkingSteps, generate } = useAIGenerate(documentId, loadDocument)

const mindMapGraphRef = ref(null)
const aiDialogVisible = ref(false)
const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const selectedNode = ref(null)

const versionModalVisible = ref(false)
const versionList = ref([])
const versionLoading = ref(false)
const operationHistoryVisible = ref(false)
const diffModalVisible = ref(false)
const nodePositions = ref(new Map())
const searchModalVisible = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])
const searchLoading = ref(false)
const importDialogVisible = ref(false)
const importFormat = ref('markdown')
const importContent = ref('')
const importTitle = ref('')
const importLoading = ref(false)

const handleSaveTitle = () => {
  saveTitle(documentTitle.value)
}

const saveDocument = () => {
  message.success('文档已自动保存')
}

const openVersionModal = async () => {
  versionModalVisible.value = true
  versionLoading.value = true
  try {
    const res = await versionApi.getDocumentVersions(documentId)
    versionList.value = res.data || []
  } catch (error) {
    message.error('加载版本列表失败')
  } finally {
    versionLoading.value = false
  }
}

const handleGenerate = async (text) => {
  await generate(text)
  aiDialogVisible.value = false
}

const handleNodeClick = (node) => {
  selectedNode.value = node
  contextMenuVisible.value = false
  
  // 更新协作状态
  if (node) {
    setFocusNode(parseInt(node.id))
  }
}

const handleNodeContextMenu = ({ node, x, y }) => {
  selectedNode.value = node
  contextMenuX.value = x
  contextMenuY.value = y
  contextMenuVisible.value = true
}

const handleViewportChange = (state) => {
  saveViewState(state)
}

const handleNodeExpand = async (node) => {
  try {
    await loadSubtree(node.id)
  } catch (error) {
    console.error('加载子节点失败', error)
  }
}

const addChildNode = async () => {
  if (!selectedNode.value) return
  
  try {
    await nodeApi.createNode({
      documentId: parseInt(documentId),
      parentId: parseInt(selectedNode.value.id),
      content: '新节点',
      userId
    })
    await loadDocument()
    contextMenuVisible.value = false
  } catch (error) {
    message.error('添加节点失败')
  }
}

const expandNodeWithAI = async () => {
  if (!selectedNode.value) return
  
  try {
    await aiApi.expandNode({
      nodeId: parseInt(selectedNode.value.id),
      userId,
      count: 5
    })
    await loadDocument()
    contextMenuVisible.value = false
    message.success('AI扩展成功')
  } catch (error) {
    message.error('AI扩展失败')
  }
}

const deleteNode = async () => {
  if (!selectedNode.value) return
  
  try {
    await nodeApi.deleteNode(selectedNode.value.id)
    await loadDocument()
    contextMenuVisible.value = false
    message.success('删除成功')
  } catch (error) {
    message.error('删除失败')
  }
}

const restoreVersion = async (version) => {
  try {
    await versionApi.restoreVersion({
      documentId: parseInt(documentId),
      versionId: version.id
    })
    message.success('恢复成功')
    versionModalVisible.value = false
    await loadDocument()
  } catch (error) {
    message.error('恢复失败')
  }
}

// 导出时临时调整视图以适应所有节点
const fitViewForExport = () => {
  return new Promise((resolve) => {
    if (mindMapGraphRef.value && mindMapGraphRef.value.fitView) {
      mindMapGraphRef.value.fitView()
    }
    setTimeout(resolve, 100)
  })
}

// 恢复视图状态
const restoreViewState = (state) => {
  if (state) {
    viewState.value = { ...state }
  }
}

const handleExport = async ({ key }) => {
  try {
    if (key === 'png' || key === 'svg') {
      // 图片格式导出：使用前端渲染
      await exportHighRes(key)
    } else {
      // 文本格式导出
      const res = await exportApi.exportDocument(documentId, key)
      
      // 从响应头获取文件名
      const contentDisposition = res.headers['content-disposition']
      let filename = `${documentTitle.value || 'document'}.${key === 'markdown' ? 'md' : key}`
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="(.+)"/)
        if (filenameMatch) {
          filename = filenameMatch[1]
        }
      }
      
      // 创建Blob并下载
      const blob = new Blob([res.data], { 
        type: res.headers['content-type'] || 'text/plain' 
      })
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      a.click()
      window.URL.revokeObjectURL(url)
      message.success('导出成功')
    }
  } catch (error) {
    console.error('导出失败', error)
    message.error('导出失败')
  }
}

const exportAsImage = async (format) => {
  try {
    // 获取MindMapGraph组件实例
    const mindMapRef = document.querySelector('.graph-container')
    if (!mindMapRef) {
      message.error('未找到图形容器')
      return
    }
    
    // 等待图形完全渲染
    await new Promise(resolve => setTimeout(resolve, 300))
    
    let canvasElement = mindMapRef.querySelector('canvas')
    if (!canvasElement) {
      message.error('Canvas元素未找到，请确保图形已正确渲染')
      return
    }
    
    // 保存当前视图状态
    const currentViewState = viewState.value
    
    // 临时调整视图以适应所有节点
    await fitViewForExport()
    
    // 等待视图调整完成
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // 重新获取调整后的Canvas
    canvasElement = mindMapRef.querySelector('canvas')
    console.log('导出Canvas尺寸:', canvasElement.width, 'x', canvasElement.height)

    if (format === 'svg') {
      // Canvas转SVG：创建SVG元素并嵌入Canvas图像
      const canvas = canvasElement
      const dataURL = canvas.toDataURL('image/png')
      
      const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" 
     width="${canvas.width}" height="${canvas.height}" viewBox="0 0 ${canvas.width} ${canvas.height}">
  <image width="${canvas.width}" height="${canvas.height}" xlink:href="${dataURL}"/>
</svg>`
      
      const blob = new Blob([svg], { type: 'image/svg+xml;charset=utf-8' })
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${documentTitle.value || 'mindmap'}.svg`
      a.click()
      window.URL.revokeObjectURL(url)
      message.success('导出成功')
    } else if (format === 'png') {
      // Canvas直接转PNG
      canvasElement.toBlob((blob) => {
        if (blob) {
          const url = window.URL.createObjectURL(blob)
          const a = document.createElement('a')
          a.href = url
          a.download = `${documentTitle.value || 'mindmap'}.png`
          a.click()
          window.URL.revokeObjectURL(url)
          message.success('导出成功')
          
          // 恢复原始视图状态
          restoreViewState(currentViewState)
        } else {
          message.error('PNG生成失败')
          restoreViewState(currentViewState)
        }
      }, 'image/png')
      return // toBlob是异步的,提前返回
    }
    
    // SVG导出后恢复视图
    restoreViewState(currentViewState)
  } catch (error) {
    console.error('图片导出失败', error)
    message.error('图片导出失败: ' + error.message)
  }
}

// 高分辨率导出(不压缩视图，临时放大画布)
const exportHighRes = async (format, targetWidth = 4000) => {
  try {
    const el = mindMapGraphRef.value?.$el || document.querySelector('.graph-container')
    const graph = mindMapGraphRef.value?.getGraph?.()
    if (!el || !graph) {
      message.error('无法获取图形实例')
      return
    }

    const originalWidth = el.offsetWidth
    const originalHeight = el.offsetHeight
    const aspect = originalWidth / originalHeight || 1.6
    const targetHeight = Math.round(targetWidth / aspect)

    const currentViewState = viewState.value

    // 放大画布并适配全图
    graph.changeSize(targetWidth, targetHeight)
    graph.fitView()
    await new Promise(resolve => setTimeout(resolve, 500))

    const canvas = el.querySelector('canvas')
    if (!canvas) {
      message.error('Canvas未找到')
      graph.changeSize(originalWidth, originalHeight)
      restoreViewState(currentViewState)
      return
    }

    if (format === 'png') {
      canvas.toBlob((blob) => {
        if (blob) {
          const url = window.URL.createObjectURL(blob)
          const a = document.createElement('a')
          a.href = url
          a.download = `${documentTitle.value || 'mindmap'}_${targetWidth}.png`
          a.click()
          window.URL.revokeObjectURL(url)
          message.success('导出成功')
        } else {
          message.error('PNG生成失败')
        }
        // 恢复画布尺寸与视图
        graph.changeSize(originalWidth, originalHeight)
        restoreViewState(currentViewState)
      }, 'image/png')
      return
    }

    if (format === 'svg') {
      const dataURL = canvas.toDataURL('image/png')
      const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"
     width="${targetWidth}" height="${targetHeight}" viewBox="0 0 ${targetWidth} ${targetHeight}">
  <image width="${targetWidth}" height="${targetHeight}" xlink:href="${dataURL}"/>
</svg>`
      const blob = new Blob([svg], { type: 'image/svg+xml;charset=utf-8' })
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${documentTitle.value || 'mindmap'}_${targetWidth}.svg`
      a.click()
      window.URL.revokeObjectURL(url)
      message.success('导出成功')
      // 恢复画布尺寸与视图
      graph.changeSize(originalWidth, originalHeight)
      restoreViewState(currentViewState)
      return
    }
  } catch (error) {
    console.error('高分辨率导出失败', error)
    message.error('高分辨率导出失败: ' + error.message)
  }
}

const handleImport = async () => {
  if (!importContent.value.trim()) {
    message.warning('请输入导入内容')
    return
  }
  if (!importTitle.value.trim()) {
    message.warning('请输入文档标题')
    return
  }

  importLoading.value = true
  try {
    const res = await exportApi.importDocument({
      format: importFormat.value,
      content: importContent.value,
      title: importTitle.value,
      userId: userId
    })
    
    if (res.code === 0) {
      message.success('导入成功')
      importDialogVisible.value = false
      importContent.value = ''
      importTitle.value = ''
      // 跳转到新文档
      if (res.data?.id) {
        window.location.href = `/editor/${res.data.id}`
      }
    } else {
      message.error(res.message || '导入失败')
    }
  } catch (error) {
    console.error('导入失败', error)
    message.error('导入失败')
  } finally {
    importLoading.value = false
  }
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) return
  
  searchLoading.value = true
  try {
    const res = await searchApi.quickSearch(searchKeyword.value, documentId)
    searchResults.value = res.data.results || []
  } catch (error) {
    message.error('搜索失败')
  } finally {
    searchLoading.value = false
  }
}

const handleSearchResultClick = (item) => {
  searchModalVisible.value = false
  // 定位到搜索结果节点
  if (item.nodeId) {
    setFocusNode(item.nodeId)
  }
}

const highlightKeyword = (text, keyword) => {
  if (!keyword) return text
  const regex = new RegExp(`(${keyword})`, 'gi')
  return text.replace(regex, '<mark>$1</mark>')
}

onMounted(() => {
  loadDocument()
})
</script>

<style scoped>
.editor {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.toolbar {
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.editor-main {
  flex: 1;
  overflow: hidden;
}

.graph-container {
  width: 100%;
  height: 100%;
}

.online-users {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.online-status {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #52c41a;
  font-size: 14px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #52c41a;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.search-results {
  margin-top: 16px;
  max-height: 400px;
  overflow-y: auto;
}

.search-results :deep(.ant-list-item) {
  cursor: pointer;
  transition: background-color 0.2s;
}

.search-results :deep(.ant-list-item:hover) {
  background-color: #f5f5f5;
}

.search-results :deep(mark) {
  background-color: #fff566;
  padding: 2px 4px;
  border-radius: 2px;
}
</style>
