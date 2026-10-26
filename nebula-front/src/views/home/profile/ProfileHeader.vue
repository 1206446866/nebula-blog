<template>
  <div class="profile-wrapper">
    <div class="profile-header">
      <!-- 头像 -->
      <a
        :href="avatarUrl || undefined"
        target="_blank"
        rel="noopener noreferrer"
        class="avatar-link"
      >
        <el-avatar :size="90" :src="avatarUrl">
          {{ avatarText }}
        </el-avatar>
      </a>
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        style="display: none"
        @change="handleFileChange"
      />

      <!-- 信息 -->
      <div class="info">
        <!-- 第一行：名字 + NID + 状态 -->
        <div class="top-line">
          <div class="name">
            <span v-if="!editing">{{ username }}</span>
            <input
              v-else
              v-model="tempUsername"
              class="name-input"
              maxlength="20"
              aria-label="新昵称"
              @keydown.enter.prevent="changeName"
              @keydown.esc="cancelEdit"
            />
          </div>

          <div class="nid">{{ profile?.userProfileInfoVO.nid }}</div>

          <div class="status">
            <span class="dot" :style="{ background: statusColor }"></span>
            {{ statusText }}
          </div>
        </div>

        <!-- 第二行：角色 -->
        <div class="roles">
          <span class="tag" v-for="role in roles" :key="role"> {{ role }}</span>
        </div>

        <!-- 第三行：操作 -->
        <div class="actions" v-if="isSelf">
          <button class="btn" :disabled="uploading" @click="updateAvatar">
            {{ uploading ? '上传中...' : '更换头像' }}
          </button>

          <el-button
            class="btn"
            :loading="nameLoading"
            :disabled="nameLoading"
            @click="
              editing ? (tempUsername === username ? cancelEdit() : changeName()) : startEdit()
            "
          >
            <span v-if="!editing">更换昵称</span>
            <span v-else>
              {{ tempUsername === username ? '退出' : '保存昵称' }}
            </span>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { UserProfileVO } from '@/api/profile.ts'

import { useProfileHeader } from './logic/useProfileHeader'

defineOptions({
  name: 'ProfileHeader',
})
const props = defineProps<{
  profile: UserProfileVO | null
  isSelf: boolean
}>()
const emit = defineEmits<{
  (e: 'refresh'): void
}>()

const {
  username,
  avatarText,
  roles,
  statusText,
  statusColor,
  avatarUrl,
  uploading,
  fileInput,
  editing,
  tempUsername,
  nameLoading,
  updateAvatar,
  handleFileChange,
  startEdit,
  cancelEdit,
  changeName,
} = useProfileHeader(props, emit)
</script>

<style scoped>
.avatar-link {
  display: inline-block;
  cursor: pointer;
}
.profile-wrapper {
  width: 100%;
  display: flex;
  justify-content: center;
  padding-top: 60px;
}

.profile-header {
  width: 80%;
  display: flex;
  align-items: center;
  gap: 22px;

  padding: 26px 30px;

  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(18px);

  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;

  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25);
}

/* 头像 */
.avatar img {
  width: 82px;
  height: 82px;
  border-radius: 50%;
  object-fit: cover;

  border: 2px solid rgba(56, 189, 248, 0.4);
}

/* 个人信息 */
.info {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 顶部信息行 */
.top-line {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 昵称 */
.name {
  font-size: 24px;
  font-weight: 600;
  color: #e5e7eb;
}

/* 账号编号 */
.nid {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.8);
}

/* 账号状态 */
.status {
  display: flex;
  align-items: center;
  gap: 6px;

  font-size: 12px;
  color: #34d399;
}

.status .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #34d399;
}

/* 角色列表 */
.roles {
  display: flex;
  gap: 8px;
}

.tag {
  font-size: 11px;
  padding: 3px 8px;

  border-radius: 6px;

  background: rgba(56, 189, 248, 0.1);
  color: #38bdf8;

  border: 1px solid rgba(56, 189, 248, 0.25);
}

/* 操作区域 */
.actions {
  margin-top: 4px;
  display: flex;
  gap: 15px;
}

.btn {
  padding: 6px 14px;
  font-size: 13px;

  border-radius: 8px;
  border: 1px solid rgba(56, 189, 248, 0.35);

  background: rgba(56, 189, 248, 0.08);
  color: #38bdf8;

  cursor: pointer;
  transition: 0.2s;
}

.btn:hover {
  background: rgba(56, 189, 248, 0.18);
  transform: translateY(-1px);
}
.name-input {
  width: min(240px, 100%);
  padding: 5px 9px;
  border: 1px solid #38bdf850;
  border-radius: 7px;
  background: #02061780;
  color: #e2e8f0;
  font: inherit;
  font-size: 18px;
  outline-color: #7dd3fc;
}
.info {
  min-width: 0;
}
.top-line,
.roles {
  flex-wrap: wrap;
}
.name {
  overflow-wrap: anywhere;
}
@media (max-width: 1050px) {
  .profile-header {
    width: 90%;
  }
}
@media (max-width: 760px) {
  .profile-wrapper {
    padding-top: 28px;
  }
  .profile-header {
    width: calc(100% - 32px);
    padding: 20px;
    gap: 16px;
    align-items: flex-start;
  }
  .avatar-link {
    flex-shrink: 0;
  }
  .top-line {
    gap: 7px 10px;
  }
  .name {
    font-size: 20px;
  }
  .actions {
    flex-wrap: wrap;
    gap: 10px;
  }
  .btn {
    margin-left: 0;
    padding: 6px 10px;
    font-size: 12px;
  }
}
@media (max-width: 430px) {
  .profile-header {
    flex-direction: column;
  }
}
</style>
