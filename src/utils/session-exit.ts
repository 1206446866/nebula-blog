const writers = new Map<symbol, { token: string; flush: () => Promise<void> }>()

/** 退出前尝试同步当前会话的编辑内容；离页后取消注册。 */
export function registerSessionWriter(token: string, flush: () => Promise<void>) {
  const key = Symbol()
  writers.set(key, { token, flush })
  return () => {
    writers.delete(key)
  }
}

export async function flushSessionWriters(token: string) {
  await Promise.allSettled(
    [...writers.values()]
      .filter((writer) => writer.token === token)
      .map((writer) => writer.flush()),
  )
}
