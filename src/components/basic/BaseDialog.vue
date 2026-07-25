<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :close-on-click-modal="false"
    :destroy-on-close="true"
  >
    <!-- 表单模式 -->
    <el-form
      v-if="showForm"
      ref="formRef"
      :model="innerModel"
      :label-width="labelWidth"
      :rules="rules"
    >
      <!-- ⚠️ 关键：给默认兜底 -->
      <slot :model="innerModel || {}" />
    </el-form>

    <!-- 普通模式 -->
    <div v-else>
      <slot />
    </div>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm"> 确认 </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

const props = defineProps({
  modelValue: Boolean,
  title: String,
  width: { type: String, default: '500px' },
  labelWidth: { type: String, default: '100px' },
  showForm: { type: Boolean, default: true },
  model: { type: Object, default: () => ({}) },
  rules: { type: Object as () => FormRules, default: () => ({}) },
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const visible = ref(false)
const formRef = ref<FormInstance>()

// ✅ 统一内部模型（关键修复点）
const innerModel = reactive<any>({})

// 同步外部 model -> 内部
watch(
  () => props.model,
  (val) => {
    Object.assign(innerModel, val || {})
  },
  { immediate: true, deep: true },
)

// v-model 同步
watch(
  () => props.modelValue,
  (val) => (visible.value = val),
  { immediate: true },
)

watch(visible, (val) => emit('update:modelValue', val))

const handleCancel = () => {
  visible.value = false
  emit('cancel')
}

const handleConfirm = async () => {
  if (props.showForm && formRef.value) {
    await formRef.value.validate()
  }
  emit('confirm', innerModel)
}
</script>
