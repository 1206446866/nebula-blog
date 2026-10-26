import assert from 'node:assert/strict'
import { test } from 'node:test'
import { isValidNewPassword } from '../src/utils/password-policy.ts'
test('new password rules accept six characters and common passwords, preserve spaces and check UTF-8 boundaries', () => {
  for (const value of [
    '123456',
    'password',
    ' 1234 ',
    'a'.repeat(64),
    '中'.repeat(24),
    '😀'.repeat(6),
  ])
    assert.equal(isValidNewPassword(value), true)
  for (const value of ['12345', ' '.repeat(6), 'a'.repeat(65), '中'.repeat(25), '😀'.repeat(19)])
    assert.equal(isValidNewPassword(value), false)
})
