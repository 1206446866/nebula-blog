import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useArticleFilterStore = defineStore('articleFilter', () => {
  /**
   * 已选分类
   */
  const categoryIds = ref<number[]>([])

  /**
   * 已选标签
   */
  const tagIds = ref<number[]>([])

  /**
   * 添加分类
   */
  const addCategory = (id: number) => {
    if (!categoryIds.value.includes(id)) {
      categoryIds.value.push(id)
    }
  }

  /**
   * 移除分类
   */
  const removeCategory = (id: number) => {
    categoryIds.value = categoryIds.value.filter((item) => item !== id)
  }

  /**
   * 添加标签
   */
  const addTag = (id: number) => {
    if (!tagIds.value.includes(id)) {
      tagIds.value.push(id)
    }
  }

  /**
   * 移除标签
   */
  const removeTag = (id: number) => {
    tagIds.value = tagIds.value.filter((item) => item !== id)
  }

  /**
   * 清空所有筛选
   */
  const clear = () => {
    categoryIds.value = []
    tagIds.value = []
  }

  return {
    categoryIds,
    tagIds,

    addCategory,
    removeCategory,

    addTag,
    removeTag,

    clear,
  }
})
