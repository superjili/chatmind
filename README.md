## ChatMind

一个开源的思维导图/脑图编辑器，支持大文档分片懒加载、版本管理、实时协作、导入导出、多格式图片与文本导出，以及基础的 AI 辅助功能。

### 特性亮点
- **分片懒加载**：大文档按 Chunk 加载，避免一次性渲染卡顿（>100 节点自动启用）。
- **操作历史与版本管理**：操作时间线、差异对比、版本快照与回滚。
- **实时协作 UI**：显示在线用户与光标定位、选中高亮（无需登录态）。
- **导入导出与搜索**：支持 Markdown/OPML 导入、Markdown/JSON/OPML/PNG/SVG 导出、全局与文档内搜索。
- **图片导出（高分辨率）**：前端内存渲染，完整导出整张脑图，清晰度不受页面缩放影响。
- **后端文件流下载**：导出采用文件流响应，浏览器原生下载，支持大文件。

### 技术栈
- **前端**：Vue 3 + Vite、Ant Design Vue、G6（Canvas 渲染）。
- **后端**：Spring Boot、Spring Data JPA、MyBatis-Plus（部分模块）、Spring AI（可选）。
- **基础设施**：Redis、MinIO（可选）、Knife4j API 文档（可选）。

### 快速开始

#### 1. 后端
1) 配置 `chatmind-backend/src/main/resources/application.yml` 的数据库与相关服务（Redis/MinIO 可选）。
2) 启动服务：
```bash
# 在后端目录
mvn spring-boot:run
```

#### 2. 前端
1) 安装依赖：
```bash
# 在前端目录
npm install
```
2) 启动开发服务器：
```bash
npm run dev
```
3) 打开浏览器访问开发地址（示例）：`http://localhost:3000`（实际端口以启动日志为准）。

### 使用说明
- **创建与编辑**：在编辑器中进行节点增删改、拖拽、折叠等常规操作。
- **分片加载**：节点数较大时自动按需加载；双击节点可懒加载未加载子树。
- **版本管理**：通过“历史版本/差异对比”查看变更并支持回滚。
- **导入**：在“导入”面板粘贴 Markdown/OPML 文本，设置标题后导入生成文档。
- **导出（文本）**：选择 Markdown/JSON/OPML，后端以文件流返回，浏览器下载。
- **导出（图片）**：选择 PNG/SVG，前端以高分辨率内存渲染导出整图；清晰度不受当前可视区缩放影响。
  - PNG：使用 Canvas 生成，适合直接分享。
  - SVG：以嵌入 PNG 的 SVG 形式导出，兼容性更好（当前渲染基于 Canvas）。

### 架构与约定
- **响应拦截器**：对 `arraybuffer/blob` 类型直接返回原始响应，避免误解析为 JSON。
- **导出实现**：
  - 后端使用 `ResponseEntity<byte[]>` 返回文件流，并设置 `Content-Disposition` 与 `Content-Type`。
  - 前端从响应头解析文件名并使用 `Blob` 触发下载。
- **多根节点导出**：导出服务遍历所有根节点，保证文档完整性。
- **子节点排序**：默认按节点 ID 排序，保持稳定顺序。

### 常见问题
- **为什么 SVG 不是纯矢量？**
  因为当前脑图渲染使用 G6 Canvas。为保证可导出，我们在 SVG 中嵌入了由 Canvas 生成的 PNG 图像。如需纯矢量导出，需要切换到 SVG 渲染或在后端生成矢量图。

- **图片导出不清晰/只导出局部？**
  现已采用高分辨率内存渲染，导出前会临时放大画布并 `fitView` 全图，导出后恢复原视图与尺寸。

### 目录结构（简要）
```
chatmind/
├── chatmind-backend/              # Spring Boot 后端
│   └── src/main/java/com/chatmind  # 后端业务代码
├── chatmind-frontend/             # Vue3 前端
│   └── src/                       # 前端源码
│       ├── views/Editor.vue       # 编辑器页面
│       ├── components/            # 组件（脑图、版本、协作等）
│       └── api/                   # 前端 API 封装
└── README.md
```
### 样式
![alt text](image.png)

### 开源与贡献
- 欢迎 Issue 和 PR，一起完善分片加载、协作、导出质量等功能。
- 在提交 PR 前请确保：代码通过构建，遵循现有风格，改动聚焦且有清晰说明。

### 致谢
- Ant Design Vue、G6、Spring Boot 等优秀开源项目。
