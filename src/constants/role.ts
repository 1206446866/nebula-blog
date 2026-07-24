export const Role = {
  SUPER_ADMIN: 'SUPER_ADMIN',
  ADMIN: 'ADMIN',
  USER: 'USER',
} as const

export type RoleType = (typeof Role)[keyof typeof Role]
