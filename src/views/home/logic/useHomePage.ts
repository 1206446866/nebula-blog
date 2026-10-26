/** 管理客户端首页入口的状态与交互。 */
export function useHomePage() {
  const goGithub = () => {
    window.open('https://github.com/1206446866/nebula-blog')
  }

  return { goGithub }
}
