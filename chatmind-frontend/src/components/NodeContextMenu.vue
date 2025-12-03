<template>
  <div 
    v-if="visible" 
    :style="style"
    class="context-menu"
    @mouseleave="handleClose"
  >
    <a-menu>
      <a-menu-item @click="handleAddChild">
        <PlusOutlined /> 添加子节点
      </a-menu-item>
      <a-menu-item @click="handleAIExpand">
        <RobotOutlined /> AI扩展
      </a-menu-item>
      <a-menu-divider />
      <a-menu-item @click="handleDelete" danger>
        <DeleteOutlined /> 删除节点
      </a-menu-item>
    </a-menu>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { PlusOutlined, RobotOutlined, DeleteOutlined } from '@ant-design/icons-vue'

const props = defineProps({
  visible: Boolean,
  x: Number,
  y: Number,
  node: Object
})

const emit = defineEmits(['close', 'add-child', 'ai-expand', 'delete'])

const style = computed(() => ({
  position: 'fixed',
  left: props.x + 'px',
  top: props.y + 'px'
}))

const handleClose = () => emit('close')
const handleAddChild = () => emit('add-child', props.node)
const handleAIExpand = () => emit('ai-expand', props.node)
const handleDelete = () => emit('delete', props.node)
</script>

<style scoped>
.context-menu {
  position: fixed;
  z-index: 1000;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}
</style>
