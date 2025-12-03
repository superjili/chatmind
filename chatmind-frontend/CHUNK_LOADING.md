# 文档分片懒加载功能说明

## 功能概述

为了优化大型思维导图的加载性能,系统实现了基于Chunk的分片懒加载机制。

## 实现机制

### 1. 自动判断加载策略

- **小文档(≤100节点)**: 一次性加载全部节点
- **大文档(>100节点)**: 使用分片懒加载
  - 首次只加载根节点及其直接子节点
  - 双击节点时按需加载其子树

### 2. 核心API

#### ChunkController后端接口

```
GET /chunks/document/{documentId}/root
GET /chunks/document/{documentId}/subtree/{nodeId}
GET /chunks/document/{documentId}/levels?fromLevel=x&toLevel=y
DELETE /chunks/document/{documentId}/cache
```

#### 前端API封装

文件: `src/api/chunk.js`

```javascript
chunkApi.getRootChunk(documentId)          // 获取根分片
chunkApi.getSubtreeChunk(documentId, nodeId) // 获取子树分片
chunkApi.loadNodesByLevel(documentId, from, to) // 按层级加载
chunkApi.clearCache(documentId)            // 清除缓存
```

### 3. 状态管理

`useDocument` composable新增:

- `nodeCount`: 文档总节点数
- `loadedNodeIds`: 已加载节点ID集合
- `loadSubtree(nodeId)`: 懒加载指定节点的子树

### 4. 用户交互

- **双击节点**: 触发子树懒加载
- **工具栏标签**: 大文档显示"已加载: X / Y"进度信息

## 使用示例

```javascript
import { useDocument } from '@/composables/useDocument'

const { 
  treeData,           // 当前已加载的树数据
  nodeCount,          // 总节点数
  loadedNodeIds,      // 已加载节点集合
  loadDocument,       // 初始加载
  loadSubtree         // 懒加载子树
} = useDocument(documentId)

// 初始加载
await loadDocument()

// 懒加载特定节点的子树
await loadSubtree(nodeId)
```

## 性能优化

1. **避免重复加载**: `loadedNodeIds` 跟踪已加载节点
2. **防抖保存**: 视图状态保存使用1秒防抖
3. **渐进式加载**: 用户交互时才加载需要的部分
4. **缓存管理**: 后端支持缓存清理接口

## 注意事项

- 分片边界由后端 `ChunkService` 自动计算
- 前端透明处理,用户无感知切换
- 适合节点数>100的大型文档
