<template>
  <div ref="graphContainer" class="graph-container"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import G6 from '@antv/g6'

const props = defineProps({
  data: {
    type: Object,
    default: null
  },
  viewState: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['node-click', 'node-contextmenu', 'viewport-change', 'node-expand'])

const graphContainer = ref(null)
let graph = null

const initGraph = () => {
  if (!graphContainer.value) return

  // 注册自定义节点
  G6.registerNode('mind-node', {
    draw(cfg, group) {
      const fontSize = 14
      const paddingH = 16
      const paddingV = 8

      let text = null
      let textBBox = { width: 60, height: fontSize }
      if (cfg.label) {
        text = group.addShape('text', {
          attrs: {
            text: cfg.label,
            x: 0,
            y: 0,
            fontSize,
            textAlign: 'center',
            textBaseline: 'middle',
            fill: '#fff',
            cursor: 'pointer'
          },
          name: 'text-shape'
        })
        text.set('zIndex', 1)
        try {
          textBBox = text.getBBox()
        } catch (e) {}
      }

      const width = Math.max(80, Math.ceil(textBBox.width + paddingH * 2))
      const height = Math.max(30, Math.ceil(textBBox.height + paddingV * 2))

      const rect = group.addShape('rect', {
        attrs: {
          x: -width / 2,
          y: -height / 2,
          width,
          height,
          radius: 4,
          fill: cfg.style?.fill || '#1890ff',
          stroke: cfg.style?.stroke || '#096dd9',
          lineWidth: 2,
          cursor: 'pointer'
        },
        name: 'rect-shape'
      })
      rect.set('zIndex', 0)
      if (text) {
        text.set('zIndex', 1)
      }
      group.sort()

      return rect
    },
    update(cfg, node) {
      const fontSize = 14
      const paddingH = 16
      const paddingV = 8

      const group = node.getContainer()
      const rect = group.find(e => e.get('name') === 'rect-shape')
      const text = group.find(e => e.get('name') === 'text-shape')
      
      if (text) {
        text.attr('text', cfg.label || '')
      } else if (cfg.label) {
        group.addShape('text', {
          attrs: {
            text: cfg.label,
            x: 0,
            y: 0,
            fontSize,
            textAlign: 'center',
            textBaseline: 'middle',
            fill: '#fff',
            cursor: 'pointer'
          },
          name: 'text-shape'
        })
        const newText = group.find(e => e.get('name') === 'text-shape')
        if (newText) newText.set('zIndex', 1)
      }

      // 重新计算文本尺寸
      let textBBox = { width: 60, height: fontSize }
      const textShape = group.find(e => e.get('name') === 'text-shape')
      if (textShape) {
        try {
          textBBox = textShape.getBBox()
        } catch (e) {}
      }

      const width = Math.max(80, Math.ceil(textBBox.width + paddingH * 2))
      const height = Math.max(30, Math.ceil(textBBox.height + paddingV * 2))

      if (rect) {
        rect.attr({
          x: -width / 2,
          y: -height / 2,
          width,
          height,
          fill: cfg.style?.fill || '#1890ff',
          stroke: cfg.style?.stroke || '#096dd9'
        })
        if (textShape) {
          textShape.set('zIndex', 1)
        }
        rect.set('zIndex', 0)
        group.sort()
      }
    }
  }, 'single-node')

  graph = new G6.TreeGraph({
    container: graphContainer.value,
    width: graphContainer.value.offsetWidth,
    height: graphContainer.value.offsetHeight,
    modes: {
      default: [
        'drag-canvas',
        'zoom-canvas',
        {
          type: 'drag-node',
          enableDelegate: true,
          shouldUpdate: () => true
        },
        'click-select'
      ]
    },
    defaultNode: {
      type: 'mind-node'
    },
    defaultEdge: {
      type: 'cubic-horizontal',
      style: {
        stroke: '#d9d9d9'
      }
    },
    layout: {
      type: 'compactBox',
      direction: 'LR',
      getHGap: () => 80,
      getVGap: () => 20
    }
  })

  bindEvents()
}

const bindEvents = () => {
  if (!graph) return

  graph.on('node:click', (e) => {
    const { item } = e
    const model = item.getModel()
    emit('node-click', model)
  })

  graph.on('node:dblclick', (e) => {
    const { item } = e
    const model = item.getModel()
    // 双击节点触发展开/收起
    if (model.collapsed) {
      graph.updateItem(item, { collapsed: false })
    } else {
      // 如果节点还有未加载的子节点,触发懒加载
      emit('node-expand', model)
    }
  })

  graph.on('node:contextmenu', (e) => {
    e.preventDefault()
    const { item, canvasX, canvasY } = e
    const model = item.getModel()
    emit('node-contextmenu', { node: model, x: canvasX, y: canvasY })
  })

  graph.on('canvas:click', () => {
    emit('node-click', null)
  })

  graph.on('viewportchange', () => {
    if (!graph || isSettingViewState) return
    const zoom = graph.getZoom()
    const matrix = graph.get('group').getMatrix()
    emit('viewport-change', { zoom, matrix })
  })
}

let isSettingViewState = false

const renderGraph = (data, viewState) => {
  if (!graph || !data) return

  graph.data(data)
  graph.render()

  // 设置视图时阻止触发事件
  isSettingViewState = true
  nextTick(() => {
    if (viewState?.zoom || viewState?.matrix) {
      if (viewState.zoom) graph.zoomTo(viewState.zoom)
      if (viewState.matrix) graph.get('group').setMatrix(viewState.matrix)
    } else {
      graph.fitView()
    }
    setTimeout(() => {
      isSettingViewState = false
    }, 100)
  })
}

watch(() => props.data, (newData) => {
  if (newData) {
    renderGraph(newData, props.viewState)
  }
})

onMounted(() => {
  initGraph()
  if (props.data) {
    renderGraph(props.data, props.viewState)
  }

  window.addEventListener('resize', () => {
    if (graph) {
      graph.changeSize(
        graphContainer.value.offsetWidth,
        graphContainer.value.offsetHeight
      )
      graph.fitView()
    }
  })
})

onBeforeUnmount(() => {
  if (graph) {
    graph.destroy()
  }
})

// 暴露fitView方法供父组件调用
defineExpose({
  fitView: () => {
    if (graph) {
      graph.fitView()
    }
  },
  getGraph: () => graph
})
</script>

<style scoped>
.graph-container {
  width: 100%;
  height: 100%;
}
</style>
