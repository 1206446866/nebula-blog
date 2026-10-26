<script setup lang="ts">
import {
  ArrowLeft,
  Check,
  Document,
  EditPen,
  View,
  Monitor,
  Cellphone,
} from '@element-plus/icons-vue'
import ArticlePreview from '@/components/article/ArticlePreview.vue'
import { formatDateTime } from '@/utils/date'
import MediaLibrary from '@/components/media/MediaLibrary.vue'

import { useArticleWriter } from './logic/useArticleWriter'

const {
  auth,
  route,
  categories,
  bodyHistory,
  form,
  id,
  status,
  scheduledPublishAt,
  scheduleError,
  scheduleTime,
  author,
  loading,
  loadError,
  saving,
  notice,
  catalogError,
  catalogLoading,
  mode,
  previewWidth,
  textarea,
  canEdit,
  canPublish,
  returnPath,
  selectedCategory,
  selectedTags,
  categoryOptions,
  tagOptions,
  wordCount,
  saveLabel,
  saveState,
  loadArticle,
  loadCatalogs,
  save,
  schedulePublication,
  cancelPublication,
  uploadingImages,
  libraryBusy,
  canUndoBody,
  canRedoBody,
  insertMarkdown,
  moveBodyHistory,
  beforeBodyInput,
  bodyInput,
  startBodyComposition,
  endBodyComposition,
  rememberBodySelection,
  bodyKeydown,
  insertHeading,
  insertCode,
  imageInput,
  mediaOpen,
  openMedia,
  chooseImage,
  selectMedia,
  imageChanged,
  pasteImages,
  dropImages,
  closeMedia,
} = useArticleWriter()
</script>

<template>
  <div class="writer-page">
    <div class="writer-shell">
      <header class="writer-heading">
        <div>
          <RouterLink :to="returnPath" class="back-link"
            ><el-icon><ArrowLeft /></el-icon
            >{{ route.query.from === 'admin' ? '文章管理' : '我的主页' }}</RouterLink
          >
          <p class="eyebrow">NEBULA / CREATIVE SPACE</p>
          <h1>让灵感，成为星光<span>。</span></h1>
          <p class="lead">从一个念头开始，写下值得被看见的故事。</p>
        </div>
        <div class="heading-orbit" aria-hidden="true"><span>✦</span></div>
      </header>

      <div v-if="loading" class="state-panel" role="status">正在打开你的写作空间…</div>
      <div v-else-if="loadError" class="state-panel" role="alert">
        <h2>暂时无法打开这篇文章</h2>
        <p>文章可能已被删除、权限已变化，或网络暂时不可用。</p>
        <el-button @click="loadArticle">重新加载</el-button>
      </div>
      <template v-else>
        <div class="workspace-bar">
          <div class="document-state">
            <span class="status-dot" :class="{ published: status === 1 }" />{{
              status === 1 ? '已发布文章' : scheduledPublishAt ? '待定时发布' : '私人草稿'
            }}<span class="save-state" role="status">{{ saveState }}</span>
          </div>
          <div class="save-actions">
            <RouterLink
              v-if="status === 1 && id && auth.hasPermission('article:list')"
              :to="`/article/${id}`"
              class="read-link"
              >阅读文章 ↗</RouterLink
            >
            <el-button
              :disabled="saving || uploadingImages || libraryBusy || !canEdit"
              :loading="saving"
              @click="save(false)"
              ><el-icon v-if="!saving"><Check /></el-icon>{{ saveLabel }}</el-button
            >
            <el-button
              v-if="canPublish"
              type="primary"
              :disabled="saving || uploadingImages || libraryBusy"
              @click="save(true)"
              >发布文章 ↗</el-button
            >
          </div>
        </div>
        <p v-if="notice" class="notice" role="status">{{ notice }}</p>
        <section v-if="canPublish" class="publication-panel" aria-label="定时发布设置">
          <div>
            <strong>定时发布</strong>
            <p>
              按上海时间（UTC+8）执行。修改并保存正文会取消原计划；后端恢复运行后会补发已到期的有效计划。
            </p>
            <p v-if="scheduledPublishAt" role="status">
              计划发布：{{ formatDateTime(scheduledPublishAt) }}（上海时间）
            </p>
            <p v-if="scheduleError" role="alert">{{ scheduleError }}</p>
          </div>
          <div class="publication-actions">
            <label for="publication-time" class="sr-only">计划发布时间（上海时间）</label>
            <input
              id="publication-time"
              v-model="scheduleTime"
              type="datetime-local"
              :disabled="saving || uploadingImages || libraryBusy"
            />
            <el-button
              :disabled="saving || uploadingImages || libraryBusy || !scheduleTime"
              @click="schedulePublication"
              >{{ scheduledPublishAt ? '保存并修改计划' : '保存并定时发布' }}</el-button
            >
            <el-button
              v-if="scheduledPublishAt"
              :disabled="saving || uploadingImages || libraryBusy"
              @click="cancelPublication"
              >取消计划</el-button
            >
          </div>
        </section>
        <p v-else-if="scheduledPublishAt" class="notice">
          计划于上海时间
          {{ formatDateTime(scheduledPublishAt) }} 发布。保存正文会取消原计划，需要管理员重新安排。
        </p>
        <p v-if="status === 1" class="notice">
          你正在编辑已发布文章。预览不会改变线上内容；点击“保存线上修改”后才会更新。
        </p>
        <p v-else-if="!canPublish" class="notice">
          这里可以保存和预览草稿，正式发布由有发布权限的管理员完成。
        </p>

        <div class="view-bar">
          <div class="view-modes" aria-label="写作视图">
            <button :aria-pressed="mode === 'edit'" @click="mode = 'edit'">
              <el-icon><EditPen /></el-icon>专注写作
            </button>
            <button :aria-pressed="mode === 'split'" @click="mode = 'split'">
              <el-icon><Document /></el-icon>实时预览
            </button>
            <button :aria-pressed="mode === 'preview'" @click="mode = 'preview'">
              <el-icon><View /></el-icon>阅读预览
            </button>
          </div>
          <div v-if="mode !== 'edit'" class="width-modes" aria-label="预览宽度">
            <button
              :aria-pressed="previewWidth === 'desktop'"
              @click="previewWidth = 'desktop'"
              aria-label="桌面预览"
            >
              <el-icon><Monitor /></el-icon><span>桌面</span>
            </button>
            <button
              :aria-pressed="previewWidth === 'mobile'"
              @click="previewWidth = 'mobile'"
              aria-label="窄屏预览"
            >
              <el-icon><Cellphone /></el-icon><span>窄屏</span>
            </button>
          </div>
        </div>

        <div class="workspace" :class="`mode-${mode}`">
          <section v-show="mode !== 'preview'" class="editor-panel" aria-label="文章编辑器">
            <div class="panel-caption"><span>01 / 写下灵感</span><span>Markdown</span></div>
            <fieldset :disabled="saving || uploadingImages || !canEdit">
              <label class="field-label" for="article-title">文章标题</label>
              <input
                id="article-title"
                v-model="form.title"
                class="title-input"
                placeholder="一个好故事，从标题开始"
                maxlength="255"
                autocomplete="off"
              />
              <div class="catalog-fields">
                <div>
                  <label class="field-label" for="article-category">分类 <span>必选</span></label>
                  <el-select
                    id="article-category"
                    v-model="form.categoryId"
                    placeholder="选择故事的归属"
                    filterable
                    :disabled="saving || !canEdit || !auth.hasPermission('category:list')"
                  >
                    <el-option
                      v-for="item in categoryOptions"
                      :key="item.id"
                      :label="item.name"
                      :value="item.id"
                    />
                  </el-select>
                </div>
                <div>
                  <label class="field-label" for="article-tags">标签 <span>可选</span></label>
                  <el-select
                    id="article-tags"
                    v-model="form.tagIds"
                    multiple
                    collapse-tags
                    collapse-tags-tooltip
                    :multiple-limit="100"
                    placeholder="添加关键词"
                    filterable
                    :disabled="saving || !canEdit || !auth.hasPermission('tag:list')"
                  >
                    <el-option
                      v-for="item in tagOptions"
                      :key="item.id"
                      :label="item.name"
                      :value="item.id"
                    />
                  </el-select>
                </div>
              </div>
              <p v-if="catalogError" class="catalog-hint">
                部分分类或标签加载失败，原选择会保留。<button
                  type="button"
                  :disabled="catalogLoading"
                  @click="loadCatalogs"
                >
                  重试
                </button>
              </p>
              <p
                v-else-if="!auth.hasPermission('category:list') && !form.categoryId"
                class="catalog-hint"
              >
                当前账号缺少分类读取权限，选择分类后才能保存，请联系管理员开通。
              </p>
              <p
                v-else-if="!catalogLoading && !categories.length && !form.categoryId"
                class="catalog-hint"
              >
                暂无可选分类，请先由管理员添加分类。
              </p>
              <div class="markdown-toolbar" aria-label="Markdown 快捷工具">
                <button
                  type="button"
                  title="撤销（Ctrl+Z）"
                  aria-label="撤销正文修改"
                  :disabled="!canUndoBody"
                  @click="moveBodyHistory()"
                >
                  ↶ 撤销
                </button>
                <button
                  type="button"
                  title="重做（Ctrl+Y / Ctrl+Shift+Z）"
                  aria-label="重做正文修改"
                  :disabled="!canRedoBody"
                  @click="moveBodyHistory(true)"
                >
                  ↷ 重做
                </button>
                <button
                  type="button"
                  title="上传图片"
                  aria-label="上传文章图片"
                  @click="chooseImage"
                >
                  ▧ 上传图片
                </button>
                <button type="button" title="媒体库" @click="openMedia">✧ 媒体库</button>
                <button
                  type="button"
                  title="一级标题"
                  aria-label="一级标题"
                  @click="insertHeading(1)"
                >
                  H₁
                </button>
                <button
                  type="button"
                  title="二级标题"
                  aria-label="二级标题"
                  @click="insertHeading(2)"
                >
                  H₂
                </button>
                <button type="button" title="加粗" @click="insertMarkdown('**', '**')">
                  <b>B</b>
                </button>
                <button type="button" title="斜体" @click="insertMarkdown('*', '*')">
                  <i>I</i>
                </button>
                <button
                  type="button"
                  title="引用"
                  @click="insertMarkdown('\n> ', '\n', '引用内容')"
                >
                  ❝
                </button>
                <button
                  type="button"
                  title="无序列表"
                  @click="insertMarkdown('\n- ', '\n', '列表项')"
                >
                  ☷
                </button>
                <button
                  type="button"
                  title="行内代码"
                  aria-label="行内代码"
                  @click="insertCode(false)"
                >
                  `code`
                </button>
                <button type="button" title="代码块" aria-label="代码块" @click="insertCode(true)">
                  &lt;/&gt;
                </button>
                <button
                  type="button"
                  title="链接"
                  @click="insertMarkdown('[', '](https://example.com)', '链接文字')"
                >
                  链接
                </button>
              </div>
              <label class="sr-only" for="article-content">文章正文</label>
              <textarea
                id="article-content"
                ref="textarea"
                :value="form.content"
                class="content-input"
                spellcheck="false"
                @beforeinput="beforeBodyInput"
                @input="bodyInput"
                @keydown="bodyKeydown"
                @compositionstart="startBodyComposition"
                @compositionend="endBodyComposition"
                @select="rememberBodySelection"
                @pointerdown="bodyHistory.boundary"
                @paste="pasteImages"
                @dragover.prevent
                @drop="dropImages"
                placeholder="在这里写下第一行…&#10;&#10;## 一个新的开始&#10;&#10;支持 Markdown，也可以粘贴或拖入图片。"
              />
            </fieldset>
            <footer class="editor-footer">
              <span>{{ wordCount.toLocaleString() }} 字符</span
              ><span role="status">{{
                uploadingImages ? '图片正在上传…' : '支持粘贴、拖入图片 · 插图后请保存文章'
              }}</span>
            </footer>
          </section>

          <section
            v-if="mode !== 'edit'"
            class="preview-panel"
            :class="{ 'narrow-preview': previewWidth === 'mobile' }"
            aria-label="文章效果预览"
          >
            <div class="panel-caption">
              <span>02 / 预见成文</span
              ><span>{{ previewWidth === 'mobile' ? '375 px · 窄屏' : '随容器宽度适配' }}</span>
            </div>
            <div class="preview-viewport">
              <ArticlePreview
                :title="form.title"
                :content="form.content"
                :author="author"
                :category="selectedCategory"
                :tags="selectedTags"
              />
            </div>
          </section>
        </div>
        <p class="workspace-note">在文字与星辰之间，留下一点属于你的光。</p>
      </template>
    </div>
    <input
      ref="imageInput"
      type="file"
      multiple
      accept="image/jpeg,image/png,image/gif,image/webp"
      class="sr-only"
      aria-label="选择文章图片"
      @change="imageChanged"
    />
    <el-dialog
      v-model="mediaOpen"
      title="媒体库"
      width="min(1040px, 94vw)"
      :before-close="closeMedia"
      :close-on-click-modal="false"
      :close-on-press-escape="!libraryBusy"
      destroy-on-close
      class="nebula-media-dialog"
    >
      <MediaLibrary v-if="mediaOpen" picking @select="selectMedia" @busy="libraryBusy = $event" />
    </el-dialog>
  </div>
</template>

<style scoped>
:global(.nebula-media-dialog) {
  --el-dialog-bg-color: #171522;
  --el-text-color-primary: #d9cde9;
  --el-text-color-regular: #b4a9c6;
  padding: 25px;
  border: 1px solid #b49acd32;
  border-radius: 16px;
}
.writer-page {
  min-height: calc(100vh - 64px);
  color: #dce6f4;
  background:
    radial-gradient(ellipse at 88% 8%, #4332802e, transparent 42%),
    radial-gradient(ellipse at 4% 36%, #164b6526, transparent 45%), #090e1b;
  padding: 36px 32px 28px;
}
.writer-shell {
  max-width: 1480px;
  margin: auto;
}
.writer-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 10px 30px;
  overflow: hidden;
}
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #8192ad;
  font-size: 12px;
  text-decoration: none;
  margin-bottom: 26px;
}
.eyebrow {
  color: #8e9abb;
  font-size: 10px;
  letter-spacing: 0.22em;
  margin: 0 0 12px;
}
h1 {
  margin: 0;
  font-size: clamp(25px, 3vw, 38px);
  font-weight: 600;
  letter-spacing: 0.04em;
}
h1 span {
  color: #99a5fa;
}
.lead {
  color: #8192ad;
  font-size: 13px;
  margin: 14px 0 0;
}
.heading-orbit {
  width: 120px;
  height: 120px;
  margin-right: 65px;
  border: 1px solid #8e9cde32;
  border-radius: 50%;
  display: grid;
  place-items: center;
  transform: rotate(-25deg);
  background: radial-gradient(circle, #8a75d926, transparent 65%);
  box-shadow: 0 0 70px #6b49bf13;
}
.heading-orbit::before {
  content: '';
  position: absolute;
  width: 175px;
  height: 58px;
  border: 1px solid #9aadeb28;
  border-radius: 50%;
}
.heading-orbit span {
  font-size: 55px;
  color: #b9c4f4;
  text-shadow: 0 0 35px #9984ea88;
}
.workspace-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 20px 0;
  border-top: 1px solid #a9b7db14;
}
.document-state,
.save-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  flex-wrap: wrap;
}
.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #b2a1ec;
  box-shadow: 0 0 12px #b2a1ec40;
}
.status-dot.published {
  background: #6ed4b2;
}
.save-state {
  font-size: 11px;
  color: #8290a7;
  margin-left: 10px;
}
.read-link {
  color: #9eb7ee;
  font-size: 12px;
  text-decoration: none;
  margin-right: 10px;
}
.save-actions :deep(.el-button) {
  margin-left: 0;
  border-radius: 9px;
  min-height: 36px;
  font-size: 12px;
  gap: 6px;
}
.save-actions :deep(.el-button--primary) {
  background: #b5b8fa;
  border-color: #b5b8fa;
  color: #171d38;
  font-weight: 600;
}
.notice {
  font-size: 12px;
  line-height: 1.7;
  padding: 12px 16px;
  margin: 0 0 18px;
  background: #9a9de908;
  border: 1px solid #9a9de921;
  border-radius: 8px;
  color: #a6b2ca;
}
.publication-panel {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  padding: 18px;
  margin-bottom: 18px;
  border: 1px solid #a5b4fc35;
  background: #a78bfa0c;
  border-radius: 12px;
  color: #c7d2fe;
}
.publication-panel strong {
  font-size: 14px;
}
.publication-panel p {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.8;
  margin: 6px 0 0;
}
.publication-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.publication-actions input {
  max-width: 100%;
  min-width: 0;
  border: 1px solid #a5b4fc44;
  border-radius: 8px;
  background: #0f172a;
  color: #dce5f4;
  padding: 9px;
  color-scheme: dark;
}
.view-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.view-modes,
.width-modes {
  display: flex;
  padding: 4px;
  border-radius: 10px;
  background: #141c2d;
  border: 1px solid #a4b8df12;
}
.view-bar button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: none;
  border-radius: 7px;
  padding: 9px 15px;
  color: #8192ad;
  background: transparent;
  font: inherit;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
}
.view-bar button[aria-pressed='true'] {
  background: #2a3450;
  color: #d5def7;
  box-shadow: 0 2px 6px #0002;
}
.width-modes button {
  padding: 9px 12px;
}
.workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}
.workspace.mode-edit,
.workspace.mode-preview {
  grid-template-columns: minmax(0, 1fr);
}
.mode-edit .editor-panel {
  width: 100%;
  max-width: 960px;
  margin: auto;
}
.editor-panel,
.preview-panel {
  min-width: 0;
  border: 1px solid #94a9d624;
  border-radius: 14px;
  overflow: hidden;
  background: #0e1626;
}
.panel-caption {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 16px 23px;
  border-bottom: 1px solid #94a9d619;
  color: #9aa9c1;
  font-size: 11px;
  letter-spacing: 0.06em;
}
.panel-caption span:last-child {
  color: #657793;
  font-size: 10px;
}
fieldset {
  border: 0;
  min-width: 0;
  margin: 0;
  padding: 24px;
}
.field-label {
  display: block;
  color: #97a7c0;
  font-size: 11px;
  margin-bottom: 10px;
}
.field-label span {
  color: #5d6e88;
  margin-left: 5px;
  font-size: 10px;
}
.title-input {
  display: block;
  box-sizing: border-box;
  background: transparent;
  border: 0;
  border-bottom: 1px solid #94a9d624;
  padding: 0 0 18px;
  width: 100%;
  font: inherit;
  font-size: 22px;
  color: #e5ecf9;
  outline: none;
}
.title-input:focus {
  border-color: #91a1de;
}
.title-input::placeholder {
  color: #455570;
  font-size: 19px;
}
.catalog-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
  margin: 22px 0;
}
.catalog-fields > div {
  min-width: 0;
}
.catalog-fields :deep(.el-select) {
  width: 100%;
}
.catalog-fields :deep(.el-select__wrapper) {
  background: #121e31;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #91a1c226;
  min-height: 38px;
}
.catalog-hint {
  color: #bcaa84;
  font-size: 11px;
  line-height: 1.6;
}
.catalog-hint button {
  border: 0;
  background: transparent;
  color: #a9bafa;
  cursor: pointer;
}
.markdown-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 3px;
  padding: 8px 5px;
  background: #142035;
  border: 1px solid #94a9d619;
  border-radius: 9px 9px 0 0;
}
.markdown-toolbar button {
  min-width: 34px;
  height: 30px;
  border: 0;
  border-radius: 5px;
  color: #a6b5cd;
  background: transparent;
  cursor: pointer;
}
.markdown-toolbar button:hover:not(:disabled) {
  background: #273654;
  color: #e4eaff;
}
.markdown-toolbar button:disabled {
  opacity: 0.4;
  cursor: default;
}
.content-input {
  display: block;
  width: 100%;
  box-sizing: border-box;
  min-height: 480px;
  resize: vertical;
  background: #0b1322;
  border: 1px solid #94a9d619;
  border-top: 0;
  border-radius: 0 0 9px 9px;
  color: #bac9df;
  padding: 21px;
  font:
    14px/1.95 'Cascadia Code',
    Consolas,
    monospace;
  outline: none;
  tab-size: 2;
}
.content-input:focus {
  box-shadow: inset 0 0 0 1px #829bd26b;
}
.content-input::placeholder {
  color: #435570;
}
.editor-footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border-top: 1px solid #94a9d619;
  color: #6a7e9e;
  padding: 16px 24px;
  font-size: 10px;
}
.preview-panel {
  background: #101727;
}
.preview-viewport {
  min-height: 760px;
}
.narrow-preview .preview-viewport {
  width: min(375px, 100%);
  margin: 24px auto;
  border: 1px solid #a0b2da25;
  border-radius: 20px;
  overflow: hidden;
  box-sizing: border-box;
}
.mode-preview .preview-viewport {
  max-width: 1040px;
  margin: auto;
}
.mode-preview .narrow-preview .preview-viewport {
  margin: 26px auto;
}
.workspace-note {
  margin: 26px 0 0;
  text-align: center;
  color: #50607c;
  font-size: 11px;
  letter-spacing: 0.08em;
}
.state-panel {
  padding: 90px 20px;
  text-align: center;
  color: #97aac9;
}
.state-panel h2 {
  font-size: 20px;
}
.state-panel p {
  margin: 18px 0;
  font-size: 13px;
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip-path: inset(50%);
  white-space: nowrap;
  border: 0;
}
button:focus-visible,
a:focus-visible {
  outline: 2px solid #a8bafa;
  outline-offset: 3px;
}
fieldset:disabled {
  opacity: 0.7;
}
@media (max-width: 960px) {
  .writer-page {
    padding: 28px 20px;
  }
  .workspace {
    grid-template-columns: minmax(0, 1fr);
  }
  .preview-viewport {
    min-height: 420px;
  }
  .heading-orbit {
    margin-right: 25px;
  }
}
@media (max-width: 600px) {
  .writer-page {
    padding: 24px 12px;
  }
  .writer-heading {
    padding: 0 4px 26px;
  }
  .heading-orbit {
    display: none;
  }
  .back-link {
    margin-bottom: 22px;
  }
  .workspace-bar {
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
  }
  .save-actions {
    width: 100%;
    justify-content: flex-end;
  }
  .save-state {
    margin-left: 3px;
  }
  .view-bar {
    flex-wrap: wrap;
  }
  .view-modes {
    flex: 1;
  }
  .view-modes button {
    flex: 1;
    padding: 9px;
    font-size: 11px;
  }
  .width-modes span {
    display: none;
  }
  .width-modes button {
    padding: 9px;
  }
  fieldset {
    padding: 20px 16px;
  }
  .catalog-fields {
    gap: 10px;
  }
  .title-input {
    font-size: 20px;
  }
  .title-input::placeholder {
    font-size: 17px;
  }
  .content-input {
    min-height: 400px;
    padding: 16px;
  }
  .panel-caption {
    padding: 15px 18px;
  }
  .editor-footer {
    padding: 14px 16px;
  }
}
</style>
