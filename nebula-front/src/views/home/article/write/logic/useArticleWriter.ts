import {
  cancelArticleScheduleApi,
  changeArticleStatusApi,
  createDraftApi,
  getArticleForEditApi,
  scheduleArticleApi,
  updateArticleApi,
} from '@/api/article'
import { listCategoryApi, type CategoryVO } from '@/api/category'
import { listTagApi, type TagVO } from '@/api/tag'
import { Role } from '@/constants/role'
import { useAuthStore } from '@/stores/auth'
import { positiveId } from '@/utils/article-query'
import { formatDateTime } from '@/utils/date'
import { publicationTime } from '@/utils/publication-time'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
import { useWriterBody } from './useWriterBody'
import { useWriterImages } from './useWriterImages'

/** 协调文章加载、草稿保存、发布计划和离页保护。 */
export function useArticleWriter() {
  const auth = useAuthStore()
  const route = useRoute()
  const router = useRouter()
  const entryToken = auth.getToken()
  const form = reactive({
    title: '',
    content: '',
    categoryId: undefined as number | undefined,
    tagIds: [] as number[],
  })
  const id = ref<number>()
  const status = ref(0)
  const scheduledPublishAt = ref<string | null>(null)
  const scheduleError = ref('')
  const scheduleTime = ref('')
  const author = ref(auth.user?.username || '作者')
  const loading = ref(true)
  const loadError = ref(false)
  const saving = ref(false)
  const savedAt = ref('')
  const savedSnapshot = ref('')
  const notice = ref('')
  const categories = ref<CategoryVO[]>([])
  const tags = ref<TagVO[]>([])
  const catalogError = ref(false)
  const catalogLoading = ref(false)
  const mode = ref<'edit' | 'split' | 'preview'>(
    route.query.mode === 'preview' ? 'preview' : 'split',
  )
  const previewWidth = ref<'desktop' | 'mobile'>('desktop')
  const textarea = ref<HTMLTextAreaElement>()
  let controller: AbortController | undefined
  const runtime = { generation: 0, disposed: false }

  let publicationTimer: ReturnType<typeof setInterval> | undefined
  let publicationController: AbortController | undefined
  const snapshot = () => JSON.stringify({ ...form, tagIds: [...form.tagIds].sort((a, b) => a - b) })
  const dirty = computed(
    () => !loading.value && !loadError.value && snapshot() !== savedSnapshot.value,
  )
  const canEdit = computed(() => auth.hasPermission(id.value ? 'article:update' : 'article:create'))
  const canPublish = computed(
    () =>
      auth.hasRole(Role.ADMIN) &&
      auth.hasPermission('article:update') &&
      status.value !== 1 &&
      canEdit.value,
  )
  const returnPath = computed(() =>
    route.query.from === 'admin' && auth.hasRole(Role.ADMIN) && auth.hasPermission('article:list')
      ? '/admin/article'
      : `/user/${auth.user?.id}`,
  )
  const selectedCategory = computed(
    () =>
      categories.value.find((item) => item.id === form.categoryId)?.name ||
      (form.categoryId ? `分类 #${form.categoryId}` : undefined),
  )
  const selectedTags = computed(() =>
    form.tagIds.map(
      (value) => tags.value.find((item) => item.id === value)?.name || `标签 #${value}`,
    ),
  )
  const categoryOptions = computed(() =>
    form.categoryId && !categories.value.some((item) => item.id === form.categoryId)
      ? [...categories.value, { id: form.categoryId, name: `当前分类 #${form.categoryId}` }]
      : categories.value,
  )
  const tagOptions = computed(() => [
    ...tags.value,
    ...form.tagIds
      .filter((value) => !tags.value.some((item) => item.id === value))
      .map((value) => ({ id: value, name: `当前标签 #${value}` })),
  ])
  const wordCount = computed(() => form.content.replace(/\s/g, '').length)
  const saveLabel = computed(() =>
    saving.value ? '正在保存…' : status.value === 1 ? '保存线上修改' : '保存草稿',
  )
  const saveState = computed(() =>
    dirty.value
      ? '有未保存的修改'
      : savedAt.value
        ? `已保存于 ${savedAt.value}`
        : id.value
          ? '内容已同步'
          : '尚未保存',
  )
  const uploadingImages = ref(false)
  const libraryBusy = ref(false)
  const editorContext = {
    form,
    textarea,
    saving,
    canEdit,
    uploadingImages,
    libraryBusy,
    runtime,
    isSessionCurrent: () => auth.getToken() === entryToken,
  }
  const {
    bodyHistory,
    canUndoBody,
    canRedoBody,
    insertMarkdown,
    applyEdit,
    resetBodyHistory,
    moveBodyHistory,
    beforeBodyInput,
    bodyInput,
    startBodyComposition,
    endBodyComposition,
    rememberBodySelection,
    bodyKeydown,
    insertHeading,
    insertCode,
  } = useWriterBody({ ...editorContext, loading, loadError })
  const {
    imageInput,
    mediaOpen,
    captureImagePosition,
    openMedia,
    chooseImage,
    selectMedia,
    uploadImages,
    imageChanged,
    pasteImages,
    dropImages,
    closeMedia,
    resetImageUpload,
    disposeImages,
  } = useWriterImages({ ...editorContext, applyEdit })

  async function loadArticle() {
    const requestedId = positiveId(route.params.id) ? Number(route.params.id) : undefined
    if (requestedId && requestedId === id.value && !loadError.value) return
    const run = ++runtime.generation
    publicationController?.abort()
    resetImageUpload()
    controller?.abort()
    controller = new AbortController()
    loading.value = true
    loadError.value = false
    id.value = undefined
    status.value = 0
    scheduledPublishAt.value = null
    scheduleError.value = ''
    scheduleTime.value = ''
    savedAt.value = ''
    notice.value = ''
    author.value = auth.user?.username || '作者'
    Object.assign(form, { title: '', content: '', categoryId: undefined, tagIds: [] })
    resetBodyHistory()
    try {
      if (route.params.id && !requestedId) throw new Error('Invalid article id')
      if (requestedId) {
        const response = await getArticleForEditApi(requestedId, controller.signal)
        if (runtime.disposed || run !== runtime.generation || auth.getToken() !== entryToken) return
        const article = response.data
        id.value = article.id
        status.value = article.status
        scheduledPublishAt.value = article.scheduledPublishAt || null
        scheduleError.value = article.scheduleError || ''
        scheduleTime.value = article.scheduledPublishAt?.replace(' ', 'T').slice(0, 16) || ''
        author.value = article.author || '作者'
        Object.assign(form, {
          title: article.title,
          content: article.content,
          categoryId: article.categoryId,
          tagIds: [...(article.tagIds || [])],
        })
      }
      resetBodyHistory()
      savedSnapshot.value = snapshot()
    } catch {
      if (!runtime.disposed && run === runtime.generation) loadError.value = true
    } finally {
      if (!runtime.disposed && run === runtime.generation) loading.value = false
    }
  }

  async function loadCatalogs() {
    if (catalogLoading.value) return
    catalogLoading.value = true
    const results = await Promise.allSettled([
      auth.hasPermission('category:list') ? listCategoryApi() : Promise.resolve(null),
      auth.hasPermission('tag:list') ? listTagApi() : Promise.resolve(null),
    ])
    if (runtime.disposed || auth.getToken() !== entryToken) return
    const [categoryResult, tagResult] = results
    if (categoryResult.status === 'fulfilled' && categoryResult.value)
      categories.value = categoryResult.value.data
    if (tagResult.status === 'fulfilled' && tagResult.value) tags.value = tagResult.value.data
    catalogError.value = results.some((result) => result.status === 'rejected')
    catalogLoading.value = false
  }

  async function save(publish = false, publishAt?: string) {
    if (
      uploadingImages.value ||
      libraryBusy.value ||
      saving.value ||
      loading.value ||
      loadError.value ||
      !canEdit.value ||
      auth.getToken() !== entryToken
    )
      return
    if ((publish || publishAt) && !canPublish.value) return
    if (!form.title.trim()) return void ElMessage.warning('先为文章写一个标题')
    if (!form.categoryId) return void ElMessage.warning('请选择文章分类')
    if (!form.content.trim()) return void ElMessage.warning('正文还没有内容')
    saving.value = true
    const run = runtime.generation
    const isCurrent = () =>
      !runtime.disposed && runtime.generation === run && auth.getToken() === entryToken
    let contentSaved = false
    try {
      if (publish || publishAt) {
        const confirmed = await ElMessageBox.confirm(
          publishAt
            ? `将先保存正文，并安排在上海时间 ${scheduleTime.value.replace('T', ' ')} 自动发布。`
            : '将先保存当前内容，再发布文章。发布后其他读者可以看到它。',
          publishAt ? '确认定时发布' : '准备好与大家见面了吗？',
          {
            confirmButtonText: publishAt ? '保存并定时发布' : '保存并发布',
            cancelButtonText: '再检查一下',
            type: 'info',
          },
        ).then(
          () => true,
          () => false,
        )
        if (!confirmed || !isCurrent()) return
      }
      const data = {
        title: form.title.trim(),
        content: form.content,
        categoryId: form.categoryId,
        tagIds: [...form.tagIds],
      }
      if (id.value) {
        const result = await updateArticleApi({ id: id.value, ...data })
        if (!result.data) throw new Error('Save failed')
      } else {
        const result = await createDraftApi(data)
        if (!isCurrent()) return
        if (!Number.isSafeInteger(result.data) || result.data < 1)
          throw new Error('Invalid article id')
        id.value = result.data
      }
      if (!isCurrent()) return
      form.title = data.title
      savedSnapshot.value = snapshot()
      savedAt.value = formatDateTime(new Date())
      const hadSchedule = !!scheduledPublishAt.value
      scheduledPublishAt.value = null
      scheduleError.value = ''
      contentSaved = true
      notice.value =
        status.value === 1
          ? '线上文章已更新。'
          : hadSchedule
            ? '草稿已保存，原定时计划已取消。需要定时发布时请重新安排。'
            : '草稿已保存，仅作者和有权限的管理员可见。'
      if (publishAt) {
        const result = await scheduleArticleApi(id.value!, publishAt)
        if (!isCurrent()) return
        scheduledPublishAt.value = result.data
        notice.value = `已安排在上海时间 ${formatDateTime(result.data)} 发布；发布前仍为私人草稿。`
      }
      if (publish) {
        const result = await changeArticleStatusApi({ id: id.value!, status: 1 })
        if (!isCurrent()) return
        if (!result.data) throw new Error('Publish failed')
        status.value = 1
        notice.value = '文章已发布，现在可以前往文章页阅读。'
      }
      ElMessage.success(
        publishAt
          ? '定时发布已安排'
          : publish
            ? '文章发布成功'
            : status.value === 1
              ? '修改已保存'
              : '草稿已保存',
      )
    } catch (error) {
      if (!isCurrent()) return
      if ((error as { code?: string }).code === 'ERR_CANCELED') return
      notice.value = contentSaved
        ? publishAt
          ? '草稿已保存，但定时安排未完成，请检查时间和文章当前状态后重试。'
          : '草稿已保存，但发布未完成。可以再次点击“发布文章”重试。'
        : '保存未完成，当前内容仍保留在编辑器中，请重试。'
    } finally {
      if (isCurrent()) {
        saving.value = false
        // 发布失败时仍保留新建文章的编号，避免重试时重复创建。
        if (id.value && !route.params.id && auth.hasPermission('article:update')) {
          await router.replace({
            name: 'ArticleEdit',
            params: { id: id.value },
            query: route.query,
          })
        }
      }
    }
  }

  async function schedulePublication() {
    const time = publicationTime(scheduleTime.value)
    if (!time) return void ElMessage.warning('请选择未来的上海时间')
    await save(false, time)
  }

  async function cancelPublication() {
    if (
      saving.value ||
      uploadingImages.value ||
      libraryBusy.value ||
      !id.value ||
      !canPublish.value
    )
      return
    const run = runtime.generation
    saving.value = true
    try {
      const result = await cancelArticleScheduleApi(id.value)
      if (runtime.disposed || run !== runtime.generation || auth.getToken() !== entryToken) return
      if (result.data) {
        scheduledPublishAt.value = null
        scheduleError.value = ''
        notice.value = '定时发布已取消，文章继续作为私人草稿保留。'
      }
    } catch {
      /* 请求拦截器统一显示错误，失败时保留原计划。 */
    } finally {
      if (!runtime.disposed && run === runtime.generation && auth.getToken() === entryToken)
        saving.value = false
    }
  }

  async function refreshPublication() {
    if (
      !scheduledPublishAt.value ||
      !id.value ||
      saving.value ||
      loading.value ||
      publicationController
    )
      return
    const run = runtime.generation,
      articleId = id.value
    const currentController = new AbortController()
    publicationController = currentController
    try {
      const response = await getArticleForEditApi(articleId, currentController.signal)
      if (
        runtime.disposed ||
        runtime.generation !== run ||
        auth.getToken() !== entryToken ||
        saving.value
      )
        return
      status.value = response.data.status
      scheduledPublishAt.value = response.data.scheduledPublishAt || null
      scheduleError.value = response.data.scheduleError || ''
      // 只同步发布状态，不覆盖尚未保存的正文、标题和撤销历史。
    } catch {
      /* 下次轮询会重试，正文保持不变。 */
    } finally {
      if (publicationController === currentController) publicationController = undefined
    }
  }

  async function confirmLeave() {
    if (auth.getToken() !== entryToken) return true
    if (saving.value || uploadingImages.value || libraryBusy.value) {
      ElMessage.info('正在保存或上传图片，请稍候')
      return false
    }
    if (!dirty.value) return true
    return ElMessageBox.confirm('还有未保存的修改，离开后这些修改将丢失。', '离开写作页？', {
      confirmButtonText: '放弃修改并离开',
      cancelButtonText: '继续写作',
      type: 'warning',
    }).then(
      () => true,
      () => false,
    )
  }

  onBeforeRouteLeave(confirmLeave)

  onBeforeRouteUpdate((to, from) => (to.params.id === from.params.id ? true : confirmLeave()))

  function beforeUnload(event: BeforeUnloadEvent) {
    if (dirty.value || saving.value || uploadingImages.value || libraryBusy.value) {
      event.preventDefault()
      event.returnValue = ''
    }
  }

  watch(() => route.params.id, loadArticle, { immediate: true })

  watch(
    () => route.query.mode,
    (value) => {
      if (value === 'preview') mode.value = 'preview'
    },
  )

  onMounted(() => {
    void loadCatalogs()
    window.addEventListener('beforeunload', beforeUnload)
    publicationTimer = setInterval(() => {
      void refreshPublication()
    }, 15000)
  })

  onBeforeUnmount(() => {
    if (publicationTimer) clearInterval(publicationTimer)
    publicationController?.abort()
    disposeImages()
    runtime.disposed = true
    runtime.generation++
    controller?.abort()
    window.removeEventListener('beforeunload', beforeUnload)
  })
  return {
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
    dirty,
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
    refreshPublication,
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
    captureImagePosition,
    openMedia,
    chooseImage,
    selectMedia,
    uploadImages,
    imageChanged,
    pasteImages,
    dropImages,
    closeMedia,
  }
}
