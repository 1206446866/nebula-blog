<script setup lang="ts">
import { useProfilePagination } from './logic/useProfilePagination'

const props = withDefaults(
  defineProps<{
    currentPage: number
    pageSize: number
    total: number
    loading?: boolean
  }>(),
  { loading: false },
)
const emit = defineEmits<{ 'update:currentPage': [page: number] }>()

const { totalPages, current, jumpPage, pages, changePage, jump } = useProfilePagination(props, emit)
</script>

<template>
  <nav class="profile-pagination" aria-label="个人主页内容分页" :aria-busy="loading">
    <div class="page-summary">
      <span class="summary-dot" />共 <strong>{{ total }}</strong> 条<span class="summary-separator"
        >/</span
      ><span>{{ current }} · {{ totalPages }} 页</span>
    </div>
    <div class="page-controls">
      <button
        type="button"
        class="page-button direction-button"
        aria-label="上一页"
        :disabled="loading || current === 1 || total === 0"
        @click="changePage(current - 1)"
      >
        <svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="m12 5-5 5 5 5" /></svg>
      </button>
      <template v-for="page in pages" :key="page">
        <span v-if="typeof page === 'string'" class="page-gap" aria-hidden="true">···</span>
        <button
          v-else
          type="button"
          class="page-button number-button"
          :class="{ 'is-current': page === current }"
          :aria-label="`第 ${page} 页`"
          :aria-current="page === current ? 'page' : undefined"
          :disabled="loading || total === 0"
          @click="changePage(page)"
        >
          {{ page }}
        </button>
      </template>
      <button
        type="button"
        class="page-button direction-button"
        aria-label="下一页"
        :disabled="loading || current === totalPages || total === 0"
        @click="changePage(current + 1)"
      >
        <svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="m8 5 5 5-5 5" /></svg>
      </button>
    </div>
    <form class="page-jump" @submit.prevent="jump">
      <label
        >前往<input
          v-model="jumpPage"
          aria-label="跳转页码"
          inputmode="numeric"
          autocomplete="off"
          :disabled="loading || total === 0"
          maxlength="9" /></label
      ><button type="submit" :disabled="loading || total === 0" aria-label="跳转到指定页">
        跳转<svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <path d="M3 8h10m-4-4 4 4-4 4" />
        </svg>
      </button>
    </form>
  </nav>
</template>

<style scoped>
.profile-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #94a3b81a;
  color: #7f90aa;
  font-size: 12px;
}
.page-summary,
.page-controls,
.page-jump,
.page-jump label,
.page-jump button {
  display: flex;
  align-items: center;
}
.page-summary {
  gap: 5px;
  font-size: 11px;
  white-space: nowrap;
}
.page-summary strong {
  color: #b7cae2;
  font-weight: 500;
}
.summary-dot {
  width: 4px;
  height: 4px;
  margin-right: 3px;
  border-radius: 50%;
  background: #63b4d9;
  box-shadow: 0 0 8px #38bdf830;
}
.summary-separator {
  margin: 0 6px;
  color: #3e506c;
}
.page-controls {
  gap: 6px;
}
button {
  font: inherit;
  cursor: pointer;
  transition:
    color 0.2s,
    background 0.2s,
    border-color 0.2s,
    box-shadow 0.2s;
}
.page-button {
  display: grid;
  min-width: 34px;
  height: 34px;
  padding: 0 8px;
  place-items: center;
  border: 1px solid #94a3b81c;
  border-radius: 9px;
  background: #12203680;
  color: #9dafc8;
  font-variant-numeric: tabular-nums;
}
.page-button svg {
  width: 17px;
  height: 17px;
  stroke: currentColor;
  stroke-width: 1.4;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.page-button:hover:not(:disabled) {
  border-color: #38bdf870;
  background: #38bdf816;
  color: #bcecff;
}
.page-button.is-current {
  border-color: #60b9ed80;
  background: linear-gradient(135deg, #38bdf829, #818cf82e);
  color: #bceaff;
  box-shadow:
    inset 0 1px #a5ddff14,
    0 0 16px #38bdf80c;
}
.page-gap {
  display: grid;
  width: 16px;
  height: 34px;
  place-items: center;
  color: #667c99;
  letter-spacing: 1px;
}
.page-jump {
  gap: 8px;
  font-size: 11px;
}
.page-jump label {
  gap: 8px;
}
.page-jump input {
  width: 42px;
  height: 32px;
  padding: 0 5px;
  border: 1px solid #94a3b826;
  border-radius: 8px;
  background: #070e1c80;
  color: #bed4ec;
  text-align: center;
  font: inherit;
  font-variant-numeric: tabular-nums;
}
.page-jump button {
  justify-content: center;
  gap: 4px;
  height: 32px;
  padding: 0 8px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #7fb6d3;
}
.page-jump button svg {
  width: 13px;
  height: 13px;
  stroke: currentColor;
  stroke-width: 1.2;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.page-jump button:hover:not(:disabled) {
  background: #38bdf812;
  color: #bceaff;
}
button:disabled {
  cursor: not-allowed;
  opacity: 0.32;
}
input:disabled {
  opacity: 0.4;
}
button:focus-visible,
input:focus-visible {
  outline: 2px solid #7dd3fc;
  outline-offset: 3px;
}
@media (max-width: 650px) {
  .profile-pagination {
    justify-content: center;
    gap: 14px 20px;
  }
  .page-summary {
    width: 100%;
    justify-content: center;
  }
  .page-button {
    min-width: 32px;
    height: 34px;
  }
  .page-controls {
    gap: 5px;
  }
}
@media (prefers-reduced-motion: reduce) {
  * {
    transition: none !important;
  }
}
</style>
