<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteMediaApi,
  listMediaApi,
  uploadArticleImageApi,
  type MediaAsset,
  type MediaEntry,
} from '@/api/media'
import { useAuthStore } from '@/stores/auth'
import { Role } from '@/constants/role'
import { formatDateTime } from '@/utils/date'
import MediaThumbnail from './MediaThumbnail.vue'

defineProps<{ picking?: boolean }>()
const emit = defineEmits<{ select: [asset: MediaAsset]; busy: [value: boolean] }>()
const auth = useAuthStore()
const entryToken = auth.getToken()
const entries = ref<MediaEntry[]>([])
const selected = ref<MediaEntry>()
const current = ref(1)
const total = ref(0)
const name = ref('')
const business = ref('')
const all = ref(false)
const loading = ref(false)
const error = ref(false)
const uploading = ref(false)
const deleting = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const canUpload = computed(
  () => auth.hasPermission('article:create') || auth.hasPermission('article:update'),
)
const canViewAll = computed(
  () =>
    auth.hasRole(Role.ADMIN) &&
    (auth.hasPermission('article:list') || auth.hasPermission('user:list')),
)
const canDelete = computed(
  () =>
    !!selected.value &&
    (selected.value.asset.userId === auth.user?.id ||
      (auth.hasRole(Role.ADMIN) &&
        auth.hasPermission(
          selected.value.asset.business === 'article' ? 'article:delete' : 'user:update',
        ))),
)
let disposed = false
let generation = 0
let controller: AbortController | undefined
const uploadController = new AbortController()
const active = () => !disposed && auth.getToken() === entryToken
function bytes(value: number) {
  return value < 1048576 ? `${Math.ceil(value / 1024)} KB` : `${(value / 1048576).toFixed(1)} MB`
}
async function load(reset = false) {
  if (reset) current.value = 1
  controller?.abort()
  controller = new AbortController()
  const run = ++generation
  loading.value = true
  error.value = false
  selected.value = undefined
  try {
    const result = await listMediaApi(
      {
        current: current.value,
        size: 12,
        name: name.value.trim() || undefined,
        business: business.value || undefined,
        all: all.value,
      },
      controller.signal,
    )
    if (!active() || run !== generation) return
    entries.value = result.data.records
    total.value = result.data.totalRow
    if (!entries.value.length && current.value > 1) {
      current.value--
      return void load()
    }
  } catch {
    if (active() && run === generation) {
      entries.value = []
      error.value = true
    }
  } finally {
    if (active() && run === generation) loading.value = false
  }
}
async function upload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || uploading.value || !canUpload.value || !active()) return
  if (!/^image\/(jpeg|png|gif|webp)$/.test(file.type) || !file.size || file.size > 10 * 1024 * 1024)
    return void ElMessage.warning('请选择不超过10MB的 JPG、PNG、GIF 或 WebP 图片')
  uploading.value = true
  emit('busy', true)
  try {
    const result = await uploadArticleImageApi(file, uploadController.signal)
    if (!active()) return
    name.value = ''
    business.value = 'article'
    await load(true)
    if (!active()) return
    selected.value = entries.value.find((entry) => entry.asset.id === result.data.id)
    ElMessage.success('图片已收入媒体库')
  } catch {
    /* 失败提示由请求层统一显示，编辑器内容保持不变。 */
  } finally {
    if (active()) {
      uploading.value = false
      emit('busy', false)
    }
  }
}
async function remove() {
  const entry = selected.value
  if (!entry || !canDelete.value || entry.references.length || deleting.value || !active()) return
  const confirmed = await ElMessageBox.confirm(
    '删除后无法恢复。已经插入但尚未保存的正文也可能引用这张图片，请确认不再需要它。',
    '删除这张图片？',
    {
      confirmButtonText: '删除图片',
      cancelButtonText: '保留',
      type: 'warning',
    },
  ).then(
    () => true,
    () => false,
  )
  if (!confirmed || !active()) return
  deleting.value = true
  try {
    await deleteMediaApi(entry.asset.id)
    if (active()) {
      ElMessage.success('图片已删除')
      await load()
    }
  } catch {
    /* 删除前由服务端重新检查图片引用。 */
  } finally {
    if (active()) deleting.value = false
  }
}
async function copyPath() {
  if (!selected.value) return
  try {
    await navigator.clipboard.writeText(selected.value.asset.path)
    ElMessage.success('资源路径已复制')
  } catch {
    ElMessage.info('复制失败，可以在下方选中路径复制')
  }
}
onMounted(() => void load())
onBeforeUnmount(() => {
  disposed = true
  generation++
  controller?.abort()
  uploadController.abort()
})
</script>

<template>
  <section class="media-library" aria-label="媒体库">
    <header class="library-heading">
      <div>
        <p>NEBULA / COLLECTION</p>
        <h2>收集一束光<span>。</span></h2>
        <small>图片可以反复使用，故事也可以拥有自己的画面。</small>
      </div>
      <button
        v-if="canUpload"
        class="upload-button"
        :disabled="uploading"
        @click="fileInput?.click()"
      >
        {{ uploading ? '正在上传…' : '＋ 上传图片' }}
      </button>
      <input
        ref="fileInput"
        class="hidden-input"
        type="file"
        accept="image/jpeg,image/png,image/gif,image/webp"
        aria-label="上传媒体图片"
        @change="upload"
      />
    </header>
    <form class="library-filters" @submit.prevent="load(true)">
      <input v-model="name" aria-label="搜索图片名称" placeholder="寻找一张图片…" maxlength="100" />
      <select v-model="business" aria-label="图片用途" @change="load(true)">
        <option value="">全部用途</option>
        <option value="article">文章配图</option>
        <option value="avatar">头像</option>
      </select>
      <label v-if="canViewAll"
        ><input v-model="all" type="checkbox" @change="load(true)" />所有账号</label
      >
      <button type="submit" :disabled="loading">搜索</button>
    </form>
    <p class="library-note">JPG / PNG / GIF / WebP · 单张不超过10MB · 草稿配图仅对有权限的人开放</p>
    <div class="library-layout">
      <div class="collection">
        <div v-if="loading" class="empty-state" role="status">正在整理星光…</div>
        <div v-else-if="error" class="empty-state" role="alert">
          媒体库暂时无法加载 <button @click="load()">重试</button>
        </div>
        <div v-else-if="!entries.length" class="empty-state">
          <span>✧</span>
          <p>这里还没有图片</p>
          <small>上传第一张，为文字增添一点光。</small>
        </div>
        <div v-else class="media-grid">
          <button
            v-for="entry in entries"
            :key="entry.asset.id"
            class="media-card"
            :class="{ selected: selected?.asset.id === entry.asset.id }"
            :aria-label="`查看图片 ${entry.asset.originalName}`"
            :aria-pressed="selected?.asset.id === entry.asset.id"
            @click="selected = entry"
          >
            <span class="thumbnail"
              ><MediaThumbnail :id="entry.asset.id" :name="entry.asset.originalName"
            /></span>
            <span class="asset-name">{{ entry.asset.originalName }}</span
            ><span class="asset-meta"
              >{{ entry.asset.business === 'avatar' ? '头像' : '文章配图' }}
              <span>{{ bytes(entry.asset.size) }}</span></span
            >
          </button>
        </div>
        <div v-if="total > 12" class="library-pagination">
          <el-pagination
            v-model:current-page="current"
            :page-size="12"
            :total="total"
            layout="prev, pager, next"
            small
            @current-change="load()"
          />
        </div>
      </div>
      <aside class="asset-details">
        <template v-if="selected">
          <p class="details-label">IMAGE / {{ selected.asset.id }}</p>
          <h3>{{ selected.asset.originalName }}</h3>
          <dl>
            <dt>尺寸</dt>
            <dd>{{ selected.asset.width }} × {{ selected.asset.height }}</dd>
            <dt>大小</dt>
            <dd>{{ bytes(selected.asset.size) }}</dd>
            <dt>收录时间</dt>
            <dd>{{ formatDateTime(selected.asset.createTime) }}</dd>
          </dl>
          <p class="path-label">存储路径 <button @click="copyPath">复制</button></p>
          <code>{{ selected.asset.path }}</code>
          <p class="reference-heading">
            {{ selected.references.length ? '正在使用' : '尚未被已保存内容引用' }}
          </p>
          <ul v-if="selected.references.length">
            <li v-for="(reference, index) in selected.references" :key="index">
              {{ reference.business === 'avatar' ? '头像' : '文章' }} · {{ reference.title }}
            </li>
          </ul>
          <p v-else class="reference-note">上传的图片会保留在这里。插入正文后，请记得保存文章。</p>
          <button
            v-if="picking && selected.asset.business === 'article'"
            class="insert-button"
            :disabled="uploading || deleting"
            @click="emit('select', selected.asset)"
          >
            插入正文 ↗
          </button>
          <button
            class="delete-button"
            :disabled="!canDelete || !!selected.references.length || deleting || uploading"
            @click="remove"
          >
            {{
              deleting
                ? '正在删除…'
                : !canDelete
                  ? '当前账号不可删除'
                  : selected.references.length
                    ? '使用中的图片不可删除'
                    : '删除图片'
            }}
          </button>
        </template>
        <div v-else class="details-empty">
          <span>✦</span>
          <p>选中一张图片</p>
          <small>查看它的路径、尺寸与使用位置</small>
        </div>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.media-library {
  color: #c8c6da;
  font-size: 13px;
}
.library-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 28px;
}
.library-heading p,
.details-label {
  font:
    9px/1.5 Consolas,
    monospace;
  letter-spacing: 0.18em;
  color: #8981a4;
  margin: 0 0 12px;
}
.library-heading h2 {
  font:
    28px/1.5 Georgia,
    'Noto Serif SC',
    serif;
  color: #eee8f6;
  margin: 0 0 8px;
}
.library-heading h2 span {
  color: #a18dca;
}
.library-heading small {
  color: #898398;
  font-size: 11px;
  line-height: 1.8;
}
button,
input,
select {
  font: inherit;
}
button {
  cursor: pointer;
}
button:disabled {
  cursor: default;
  opacity: 0.45;
}
.upload-button,
.insert-button {
  background: #bba9dc;
  border: 1px solid #bba9dc;
  color: #211b32;
  padding: 11px 17px;
  border-radius: 8px;
  white-space: nowrap;
}
.library-filters {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.library-filters > input {
  flex: 1;
  min-width: 140px;
}
.library-filters > input,
.library-filters select,
.library-filters button {
  border: 1px solid #a99ac62b;
  background: #201d30;
  color: #c4bdd4;
  padding: 10px 12px;
  border-radius: 7px;
}
.library-filters label {
  font-size: 11px;
  display: flex;
  align-items: center;
  gap: 5px;
}
.library-note {
  font-size: 10px;
  color: #746e86;
  margin: 14px 0 22px;
  line-height: 1.8;
}
.library-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 230px;
  gap: 24px;
}
.collection {
  min-width: 0;
}
.media-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.media-card {
  min-width: 0;
  padding: 0;
  overflow: hidden;
  text-align: left;
  background: #1d1a2b;
  border: 1px solid #a998c51f;
  border-radius: 9px;
  color: inherit;
  transition: border-color 0.2s;
}
.media-card:hover,
.media-card.selected {
  border-color: #ad95d6;
}
.thumbnail {
  height: 120px;
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    repeating-conic-gradient(#ffffff02 0% 25%, transparent 0% 50%) 50% / 16px 16px,
    #12121f;
}
.asset-name {
  display: block;
  font-size: 11px;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
  margin: 12px 12px 7px;
}
.asset-meta {
  display: flex;
  justify-content: space-between;
  color: #7e758f;
  font-size: 9px;
  margin: 0 12px 12px;
}
.asset-details {
  border-left: 1px solid #a99ac61c;
  padding-left: 24px;
  min-width: 0;
}
.asset-details h3 {
  font-size: 14px;
  line-height: 1.6;
  overflow-wrap: anywhere;
  color: #ded6ec;
  margin: 0 0 24px;
}
.asset-details dl {
  font-size: 10px;
  display: grid;
  grid-template-columns: 60px minmax(0, 1fr);
  gap: 12px 8px;
}
.asset-details dt {
  color: #756c89;
}
.asset-details dd {
  margin: 0;
  overflow-wrap: anywhere;
}
.path-label {
  margin: 24px 0 10px;
  font-size: 10px;
  color: #81768f;
  display: flex;
  justify-content: space-between;
}
.path-label button {
  border: 0;
  padding: 0;
  background: none;
  color: #b1a0cc;
  font-size: 10px;
}
.asset-details code {
  user-select: all;
  display: block;
  color: #9a91aa;
  font:
    10px/1.8 Consolas,
    monospace;
  overflow-wrap: anywhere;
  padding: 10px;
  border: 1px solid #a99ac61c;
  border-radius: 6px;
}
.reference-heading {
  font-size: 11px;
  margin-top: 24px;
  color: #b9b0c8;
}
.asset-details ul {
  padding-left: 16px;
  font-size: 10px;
  color: #8b8399;
  line-height: 1.9;
  overflow-wrap: anywhere;
}
.reference-note {
  color: #7d748d;
  font-size: 10px;
  line-height: 1.8;
}
.insert-button,
.delete-button {
  display: block;
  width: 100%;
  margin-top: 18px;
  font-size: 11px;
}
.delete-button {
  border: 1px solid #bc8e9d29;
  background: transparent;
  color: #bb8e9e;
  padding: 10px;
  border-radius: 7px;
  margin-top: 10px;
}
.empty-state {
  min-height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 8px;
  color: #9f94b4;
}
.empty-state > span {
  font-size: 54px;
  color: #9480b4;
}
.empty-state p {
  margin: 10px 0 0;
}
.empty-state small,
.details-empty small {
  font-size: 10px;
  color: #71677f;
}
.details-empty {
  padding: 58px 0;
  text-align: center;
}
.details-empty > span {
  font-size: 26px;
  color: #7e6b9c;
}
.details-empty p {
  font-size: 12px;
  margin-top: 20px;
}
.library-pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
.library-pagination :deep(.el-pager li),
.library-pagination :deep(button) {
  background: transparent;
  color: #a69bb9;
}
.library-pagination :deep(.is-active) {
  color: #dcc5ff;
}
.hidden-input {
  position: absolute;
  width: 1px;
  height: 1px;
  clip-path: inset(50%);
  overflow: hidden;
}
button:focus-visible,
input:focus-visible,
select:focus-visible {
  outline: 2px solid #bda4e2;
  outline-offset: 3px;
}
@media (max-width: 750px) {
  .library-layout {
    grid-template-columns: minmax(0, 1fr);
  }
  .asset-details {
    border: 0;
    border-top: 1px solid #a99ac61c;
    padding: 22px 0 0;
  }
  .details-empty {
    padding: 12px 0;
  }
  .media-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .thumbnail {
    height: 110px;
  }
  .library-heading h2 {
    font-size: 23px;
  }
  .library-heading {
    align-items: flex-start;
  }
  .upload-button {
    padding: 9px 12px;
    font-size: 11px;
  }
  .library-heading small {
    display: block;
    max-width: 210px;
  }
}
</style>
