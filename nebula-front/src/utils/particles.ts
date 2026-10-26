import { tsParticles } from '@tsparticles/engine'
import { loadFull } from 'tsparticles'

let initialization: Promise<typeof tsParticles> | undefined

/** 全站共享粒子功能注册；失败后允许重试，页面实例仍由各组件独立释放。 */
export function initializedParticles(): Promise<typeof tsParticles> {
  initialization ??= loadFull(tsParticles)
    .then(() => tsParticles)
    .catch((error) => {
      initialization = undefined
      throw error
    })
  return initialization
}
