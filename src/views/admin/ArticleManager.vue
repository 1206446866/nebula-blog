<template>
  <div class="page admin-management-page">
    <div class="header admin-management-header admin-management-header--large">
      <h1>文章管理</h1>
      <p class="subtitle admin-management-subtitle admin-management-subtitle--spaced">
        管理平台文章内容与发布信息
      </p>
    </div>

    <div class="content admin-management-content admin-management-content--column">
      <!-- 搜索 -->
      <el-form inline class="filter-form admin-management-filter">
        <el-form-item label="标题">
          <el-input v-model="titleFilter" placeholder="输入标题搜索" clearable />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="onSearch"> 查询 </el-button>
        </el-form-item>

        <el-form-item>
          <el-button v-if="auth.hasPermission('article:create')" type="success" @click="openCreate">
            新增文章
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <BaseTable
        :data="articles"
        :columns="columns"
        :loading="loading"
        :error="tableError"
        @retry="reloadTable"
        :current="current"
        :size="size"
        :total="total"
        @page-change="onPageChange"
        @size-change="onSizeChange"
      >
        <template #status="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'warning'">
            {{
              row.status === 1
                ? '已发布'
                : row.scheduleError
                  ? '定时已暂停'
                  : row.scheduledPublishAt
                    ? '定时待发布'
                    : '草稿'
            }}
          </el-tag>
          <small v-if="row.scheduledPublishAt" class="schedule-time"
            >{{ formatDateTime(row.scheduledPublishAt) }}（上海）</small
          >
        </template>

        <template #actions="{ row }">
          <el-button
            v-if="auth.hasPermission('article:update')"
            size="small"
            type="primary"
            @click="openEdit(row)"
          >
            {{ row.status === 1 ? '编辑' : '编辑草稿' }}
          </el-button>
          <el-button
            v-if="auth.hasPermission('article:update')"
            size="small"
            @click="openEdit(row, true)"
            >预览</el-button
          >
          <SafeButton
            v-if="auth.hasPermission('article:update')"
            size="small"
            :type="row.status === 1 ? 'warning' : 'success'"
            :onClick="() => switchStatus(row)"
          >
            {{ row.status === 1 ? '撤回' : '发布' }}
          </SafeButton>
          <SafeButton
            v-if="auth.hasPermission('article:delete')"
            size="small"
            type="danger"
            :onClick="() => deleteArticle(row)"
          >
            删除
          </SafeButton>
        </template>
      </BaseTable>
    </div>
  </div>
</template>

<script setup lang="ts">
import SafeButton from '@/components/basic/SafeButton.vue'
import BaseTable from '@/components/basic/BaseTable.vue'
import { formatDateTime } from '@/utils/date'

import { useArticleManager } from './logic/useArticleManager'

const {
  titleFilter,
  auth,
  articles,
  loading,
  tableError,
  current,
  size,
  total,
  reloadTable,
  onSearch,
  onPageChange,
  onSizeChange,
  columns,
  openCreate,
  openEdit,
  deleteArticle,
  switchStatus,
} = useArticleManager()
</script>

<style src="./styles/management.css"></style>

<style scoped>
.schedule-time {
  display: block;
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.6;
  margin-top: 6px;
}
</style>
