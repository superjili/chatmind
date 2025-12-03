import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { documentApi } from '@/api/document'
import { nodeApi } from '@/api/node'
import { chunkApi } from '@/api/chunk'

export function useDocument(documentId) {
  const documentTitle = ref('未命名文档')
  const treeData = ref(null)
  const viewState = ref(null)
  const loadedNodeIds = ref(new Set()) // 跟踪已加载节点
  const nodeCount = ref(0) // 文档总节点数

  const loadDocument = async () => {
    try {
      const docRes = await documentApi.getDocument(documentId)
      documentTitle.value = docRes.data.title

      // 获取所有节点并判断是否需要分片加载
      const nodesRes = await nodeApi.getDocumentNodes(documentId)
      const nodes = nodesRes.data || []
      nodeCount.value = nodes.length
      
      // 小文档(<=100节点)直接加载,大文档使用分片
      if (nodes.length <= 100) {
        if (nodes.length > 0) {
          nodes.forEach(n => loadedNodeIds.value.add(n.id))
          treeData.value = buildTree(nodes)
        }
      } else {
        // 使用分片加载根节点
        await loadRootChunk()
      }
      
      // 恢复视图状态
      const metadata = docRes.data.metadata ? JSON.parse(docRes.data.metadata) : {}
      viewState.value = metadata.viewState || null
    } catch (error) {
      message.error('加载文档失败')
    }
  }

  const loadRootChunk = async () => {
    try {
      const res = await chunkApi.getRootChunk(documentId)
      const chunk = res.data
      
      if (chunk && chunk.rootNode) {
        const nodes = [chunk.rootNode, ...(chunk.childNodes || [])]
        nodes.forEach(n => loadedNodeIds.value.add(n.id))
        treeData.value = buildTree(nodes)
      }
    } catch (error) {
      console.error('加载根分片失败', error)
      message.error('加载根节点失败')
    }
  }

  const loadSubtree = async (nodeId) => {
    try {
      const res = await chunkApi.getSubtreeChunk(documentId, nodeId)
      const chunk = res.data
      
      if (chunk && chunk.childNodes) {
        const newNodes = chunk.childNodes.filter(n => !loadedNodeIds.value.has(n.id))
        if (newNodes.length > 0) {
          // 重新获取所有已加载节点并构建树
          const nodesRes = await nodeApi.getDocumentNodes(documentId)
          const allNodes = nodesRes.data || []
          const nodesToBuild = allNodes.filter(n => 
            loadedNodeIds.value.has(n.id) || newNodes.some(nn => nn.id === n.id)
          )
          newNodes.forEach(n => loadedNodeIds.value.add(n.id))
          treeData.value = buildTree(nodesToBuild)
        }
        return chunk.hasMore
      }
      return false
    } catch (error) {
      console.error('加载子树失败', error)
      return false
    }
  }

  const buildTree = (nodes) => {
    const nodeMap = new Map()
    nodes.forEach(node => {
      nodeMap.set(node.id, {
        id: String(node.id),
        label: node.content,
        children: []
      })
    })

    let root = null
    nodeMap.forEach((node, id) => {
      const originalNode = nodes.find(n => n.id === id)
      if (originalNode.parentId) {
        const parent = nodeMap.get(originalNode.parentId)
        if (parent) {
          parent.children.push(node)
        }
      } else {
        root = node
      }
    })

    return root || { id: '1', label: '根节点', children: [] }
  }

  const saveTitle = async (title) => {
    try {
      await documentApi.updateDocument(documentId, { title })
      message.success('标题已保存')
    } catch (error) {
      message.error('保存失败')
    }
  }

  let saveViewStateTimer = null
  const saveViewState = (state) => {
    if (saveViewStateTimer) clearTimeout(saveViewStateTimer)
    
    saveViewStateTimer = setTimeout(async () => {
      try {
        const metadata = {
          viewState: state
        }
        
        await documentApi.updateDocument(documentId, {
          metadata: JSON.stringify(metadata)
        })
        console.log('视图状态已保存')
      } catch (error) {
        console.error('保存视图状态失败', error)
      }
    }, 1000)
  }

  return {
    documentTitle,
    treeData,
    viewState,
    nodeCount,
    loadedNodeIds,
    loadDocument,
    loadSubtree,
    saveTitle,
    saveViewState
  }
}
