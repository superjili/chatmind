import { ref, onMounted, onBeforeUnmount } from 'vue'
import { sessionApi } from '@/api/session'
import { message } from 'ant-design-vue'

export function useCollaboration(documentId, userId, username = '用户') {
  const onlineUsers = ref([])
  const isConnected = ref(false)
  const currentUser = ref(null)
  const selectedNodes = ref([])
  const focusNode = ref(null)
  let heartbeatTimer = null
  let fetchUsersTimer = null

  const connect = async () => {
    try {
      const res = await sessionApi.joinSession(documentId, userId, username)
      currentUser.value = res.data
      isConnected.value = true
      console.log('已加入协作会话')
      
      // 获取在线用户列表
      await fetchOnlineUsers()
      
      // 启动定时任务
      startHeartbeat()
      startFetchUsers()
    } catch (error) {
      console.error('加入会话失败', error)
      message.error('加入协作会话失败')
    }
  }

  const fetchOnlineUsers = async () => {
    try {
      const res = await sessionApi.getOnlineUsers(documentId)
      onlineUsers.value = res.data || []
    } catch (error) {
      console.error('获取在线用户失败', error)
    }
  }

  const disconnect = async () => {
    try {
      await sessionApi.leaveSession(documentId, userId)
      isConnected.value = false
      stopHeartbeat()
      stopFetchUsers()
      console.log('已离开协作会话')
    } catch (error) {
      console.error('离开会话失败', error)
    }
  }

  const startHeartbeat = () => {
    heartbeatTimer = setInterval(async () => {
      if (isConnected.value) {
        try {
          await sessionApi.heartbeat(documentId, userId)
        } catch (error) {
          console.error('心跳失败', error)
        }
      }
    }, 30000) // 30秒
  }

  const stopHeartbeat = () => {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  const startFetchUsers = () => {
    fetchUsersTimer = setInterval(() => {
      if (isConnected.value) {
        fetchOnlineUsers()
      }
    }, 5000) // 每5秒刷新在线用户
  }

  const stopFetchUsers = () => {
    if (fetchUsersTimer) {
      clearInterval(fetchUsersTimer)
      fetchUsersTimer = null
    }
  }

  const updatePresence = async (data) => {
    try {
      await sessionApi.updatePresence(documentId, {
        userId,
        focusNodeId: data.focusNodeId,
        selectedNodeIds: data.selectedNodeIds ? JSON.stringify(data.selectedNodeIds) : null,
        status: data.status || 'online'
      })
    } catch (error) {
      console.error('更新状态失败', error)
    }
  }

  const setFocusNode = (nodeId) => {
    focusNode.value = nodeId
    updatePresence({ focusNodeId: nodeId })
  }

  const setSelectedNodes = (nodeIds) => {
    selectedNodes.value = nodeIds
    updatePresence({ selectedNodeIds: nodeIds })
  }

  onMounted(() => {
    connect()
  })

  onBeforeUnmount(() => {
    disconnect()
  })

  return {
    onlineUsers,
    isConnected,
    currentUser,
    selectedNodes,
    focusNode,
    setFocusNode,
    setSelectedNodes,
    updatePresence
  }
}
