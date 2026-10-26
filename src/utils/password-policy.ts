export const newPasswordMessage = '新密码需为6至64个字符且不超过72字节'

/** 只校验新设置的密码，已有账号的登录密码和当前密码确认不受影响。 */
export function isValidNewPassword(password: string): boolean {
  const count = Array.from(password).length
  return (
    Boolean(password.trim()) &&
    count >= 6 &&
    count <= 64 &&
    new TextEncoder().encode(password).length <= 72
  )
}
