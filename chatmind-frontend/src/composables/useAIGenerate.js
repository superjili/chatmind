import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { aiApi } from '@/api/ai'

export function useAIGenerate(documentId, onSuccess) {
  const generating = ref(false)
  const thinkingSteps = ref([])
  
  const thinkingMessages = [
    '🤔 正在分析你的需求...',
    '💡 理解了！开始构思脑图结构...',
    '🎯 确定主题和分支...',
    '🌱 生成第一层节点...',
    '🌳 扩展子节点...',
    '✨ 优化内容结构...',
    '🎉 完成！正在生成脑图...'
  ]

  let thinkingInterval = null

  const startThinking = () => {
    thinkingSteps.value = []
    let stepIndex = 0
    
    thinkingInterval = setInterval(() => {
      if (stepIndex < thinkingMessages.length) {
        thinkingSteps.value.push({
          message: thinkingMessages[stepIndex],
          time: new Date().toLocaleTimeString()
        })
        stepIndex++
      }
    }, 2000)
  }

  const stopThinking = () => {
    if (thinkingInterval) {
      clearInterval(thinkingInterval)
      thinkingInterval = null
    }
    thinkingSteps.value = []
  }

  const generate = async (text) => {
    if (!text?.trim()) {
      message.warning('请输入生成内容')
      return
    }

    generating.value = true
    startThinking()
    
    try {
      await aiApi.generateMindmap({
        text,
        documentId: parseInt(documentId),
        userId: 1
      })
      
      stopThinking()
      message.success('生成成功')
      
      if (onSuccess) {
        await onSuccess()
      }
    } catch (error) {
      stopThinking()
      message.error('AI生成失败')
    } finally {
      generating.value = false
    }
  }

  return {
    generating,
    thinkingSteps,
    generate
  }
}
