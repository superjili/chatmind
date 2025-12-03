<template>
  <a-modal
    v-model:open="visible"
    title="AI生成脑图"
    @ok="handleGenerate"
    :confirmLoading="generating"
    :width="600"
  >
    <a-textarea
      v-model:value="prompt"
      :rows="4"
      placeholder="请输入要生成脑图的主题或描述..."
    />
    
    <!-- AI思考过程 -->
    <div v-if="generating && thinkingSteps.length > 0" class="thinking-process">
      <a-divider>🤖 AI思考中</a-divider>
      <a-timeline>
        <a-timeline-item
          v-for="(step, index) in thinkingSteps"
          :key="index"
          :color="index === thinkingSteps.length - 1 ? 'blue' : 'green'"
        >
          <template #dot>
            <LoadingOutlined v-if="index === thinkingSteps.length - 1" />
          </template>
          <div class="thinking-step">
            <div class="step-message">{{ step.message }}</div>
            <div class="step-time">{{ step.time }}</div>
          </div>
        </a-timeline-item>
      </a-timeline>
    </div>
  </a-modal>
</template>

<script setup>
import { ref, computed } from 'vue'
import { LoadingOutlined } from '@ant-design/icons-vue'

const props = defineProps({
  modelValue: Boolean,
  generating: Boolean,
  thinkingSteps: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'generate'])

const prompt = ref('')

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const handleGenerate = () => {
  emit('generate', prompt.value)
}
</script>

<style scoped>
.thinking-process {
  margin-top: 20px;
}

.thinking-step {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.step-message {
  font-size: 14px;
  color: #333;
}

.step-time {
  font-size: 12px;
  color: #999;
  margin-left: 12px;
}
</style>
