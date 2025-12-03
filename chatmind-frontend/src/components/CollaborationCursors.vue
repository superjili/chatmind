<template>
  <div class="collaboration-cursors">
    <!-- 其他用户的光标和选中高亮 -->
    <div
      v-for="user in otherUsers"
      :key="user.userId"
      class="user-cursor"
    >
      <!-- 焦点节点高亮 -->
      <div
        v-if="user.focusNodeId"
        :class="['focus-indicator', `user-${user.userId}`]"
        :style="getFocusStyle(user)"
      >
        <div class="user-label" :style="{ backgroundColor: user.color }">
          {{ user.username }}
        </div>
      </div>

      <!-- 选中节点高亮 -->
      <div
        v-for="nodeId in getSelectedNodeIds(user)"
        :key="`${user.userId}-${nodeId}`"
        :class="['selection-indicator', `user-${user.userId}`]"
        :style="getSelectionStyle(user, nodeId)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  onlineUsers: {
    type: Array,
    default: () => []
  },
  currentUserId: {
    type: Number,
    required: true
  },
  nodePositions: {
    type: Map,
    default: () => new Map()
  }
})

const otherUsers = computed(() => {
  return props.onlineUsers.filter(u => u.userId !== props.currentUserId)
})

const getSelectedNodeIds = (user) => {
  try {
    return user.selectedNodeIds ? JSON.parse(user.selectedNodeIds) : []
  } catch {
    return []
  }
}

const getFocusStyle = (user) => {
  const pos = props.nodePositions.get(user.focusNodeId)
  if (!pos) return { display: 'none' }
  
  return {
    left: `${pos.x}px`,
    top: `${pos.y}px`,
    borderColor: user.color,
    boxShadow: `0 0 0 2px ${user.color}40`
  }
}

const getSelectionStyle = (user, nodeId) => {
  const pos = props.nodePositions.get(parseInt(nodeId))
  if (!pos) return { display: 'none' }
  
  return {
    left: `${pos.x}px`,
    top: `${pos.y}px`,
    backgroundColor: `${user.color}20`,
    borderColor: user.color
  }
}
</script>

<style scoped>
.collaboration-cursors {
  pointer-events: none;
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 100;
}

.focus-indicator,
.selection-indicator {
  position: absolute;
  width: 120px;
  height: 40px;
  border-radius: 4px;
  border: 2px solid;
  pointer-events: none;
  transition: all 0.2s ease;
}

.focus-indicator {
  animation: pulse 2s infinite;
}

.selection-indicator {
  opacity: 0.6;
}

.user-label {
  position: absolute;
  top: -24px;
  left: 0;
  padding: 2px 8px;
  border-radius: 4px;
  color: white;
  font-size: 12px;
  white-space: nowrap;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}
</style>
