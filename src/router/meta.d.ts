import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    public?: boolean
    guest?: boolean
    roles?: readonly string[]
    permission?: string
    idParam?: string
    articleQuery?: boolean
  }
}
