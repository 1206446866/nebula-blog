// stores/loading.ts
import { defineStore } from 'pinia'

export const useLoadingStore = defineStore('loading', {
  state: () => ({
    count: 0,
  }),

  getters: {
    isLoading: (state) => state.count > 0,
  },

  actions: {
    start() {
      this.count++
    },
    end() {
      this.count = Math.max(0, this.count - 1)
    },
    reset() {
      this.count = 0
    },
  },
})
