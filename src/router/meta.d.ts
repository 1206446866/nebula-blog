import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    public?: boolean
    roles?: readonly string[]
    permission?: string
    idParam?: string
    articleQuery?: boolean
  }
}
