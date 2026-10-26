import assert from 'node:assert/strict'
import { test } from 'node:test'
import { avatarUrl, imageMarkdown, mediaPath } from '../src/utils/resource.ts'

const path = 'upload/article/admin/20261023/0123456789abcdef0123456789abcdef.png'
test('resource paths support stored relative paths and both API URL forms', () => {
  for (const value of [path, '/' + path, '/api/' + path]) assert.equal(mediaPath(value), path)
  for (const value of [
    '../' + path,
    'https://evil.test/' + path,
    path + '?token=secret',
    'upload/article/../../secret.png',
  ])
    assert.equal(mediaPath(value), undefined)
})
test('avatars support migrated paths and legacy names without external hosts', () => {
  assert.equal(
    avatarUrl(path.replace('/article/', '/avatar/')),
    '/api/' + path.replace('/article/', '/avatar/'),
  )
  assert.equal(avatarUrl('old-avatar.png'), '/api/upload/avatar/old-avatar.png')
  for (const value of [null, '', '../secret.png', 'https://evil.test/avatar.png', path])
    assert.equal(avatarUrl(value), '')
})
test('inserted image Markdown has a stable relative URL and safe alternate text', () => {
  assert.equal(imageMarkdown(path, '星海.png'), `![星海](/${path})`)
  assert.doesNotMatch(imageMarkdown(path, 'a](`bad`)_<img>.png'), /!\[.*[<>`]/)
  assert.throws(() => imageMarkdown('https://evil.test/a.png', 'x'))
  assert.throws(() => imageMarkdown(path.replace('/article/', '/avatar/'), 'x'))
})
