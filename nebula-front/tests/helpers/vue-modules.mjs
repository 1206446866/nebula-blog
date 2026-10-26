import { readFileSync, existsSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import vm from 'node:vm'
import ts from 'typescript'

/** 使用同一 Vue 实例、接口替身和浏览器环境执行拆分后的页面模块。 */
function runInNewContext(code, sandbox, { entry } = {}) {
  const context = vm.createContext({ ...sandbox })
  const cache = new Map()
  const fallback = sandbox.require
  function localRequire(parent) {
    return (name) => {
      if (!name.startsWith('.') || !parent) return fallback(name)
      const target = path.resolve(path.dirname(parent), name)
      const filename = [
        target + '.ts',
        ...(target.endsWith('.js') ? [target.slice(0, -3) + '.ts'] : []),
      ].find(existsSync)
      if (!filename) return fallback(name)
      if (cache.has(filename)) return cache.get(filename).exports
      const module = { exports: {} }
      cache.set(filename, module)
      const { outputText } = ts.transpileModule(readFileSync(filename, 'utf8'), {
        compilerOptions: { module: ts.ModuleKind.CommonJS, esModuleInterop: true },
      })
      const execute = new vm.Script(`(function (module, exports, require) {\n${outputText}\n})`, {
        filename,
      }).runInContext(context)
      execute(module, module.exports, localRequire(filename))
      return module.exports
    }
  }
  context.require = localRequire(entry ? fileURLToPath(entry) : undefined)
  return vm.runInContext(code, context)
}

export default { runInNewContext }
