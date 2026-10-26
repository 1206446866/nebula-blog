/** 限制单个浏览器标签页内的图片任务并发，排队任务可在开始前取消。 */
export function createMediaQueue(concurrency = 2, maxPending = 128) {
  if (
    !Number.isInteger(concurrency) ||
    concurrency < 1 ||
    !Number.isInteger(maxPending) ||
    maxPending < 1
  )
    throw new Error('Invalid media queue limits')
  const pending: { start: () => void }[] = []
  let active = 0
  function drain() {
    while (active < concurrency && pending.length) pending.shift()?.start()
  }
  return function run<T>(task: () => Promise<T>, signal?: AbortSignal): Promise<T> {
    if (signal?.aborted) return Promise.reject(new DOMException('请求已取消', 'AbortError'))
    if (pending.length >= maxPending) return Promise.reject(new Error('图片请求较多，请稍后重试'))
    return new Promise<T>((resolve, reject) => {
      const entry = {
        start() {
          signal?.removeEventListener('abort', cancel)
          active++
          Promise.resolve()
            .then(() => {
              if (signal?.aborted) throw new DOMException('请求已取消', 'AbortError')
              return task()
            })
            .then(resolve, reject)
            .finally(() => {
              active--
              drain()
            })
        },
      }
      function cancel() {
        const index = pending.indexOf(entry)
        if (index < 0) return
        pending.splice(index, 1)
        signal?.removeEventListener('abort', cancel)
        reject(new DOMException('请求已取消', 'AbortError'))
      }
      signal?.addEventListener('abort', cancel, { once: true })
      pending.push(entry)
      drain()
    })
  }
}
