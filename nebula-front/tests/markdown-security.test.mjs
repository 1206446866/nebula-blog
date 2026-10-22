import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import { JSDOM } from 'jsdom'
import hljs from 'highlight.js'

let dom
let renderMarkdown
let purifier

before(async () => {
  // No script execution or external resource loading is enabled in this DOM.
  dom = new JSDOM('', { url: 'https://nebula.test/' })
  globalThis.window = dom.window
  ;({ renderMarkdown } = await import('../src/utils/markdown.ts'))
  purifier = (await import('dompurify')).default
})

after(() => {
  dom.window.close()
  delete globalThis.window
})

function rendered(source) {
  const root = dom.window.document.createElement('div')
  root.innerHTML = renderMarkdown(source)
  return root
}

function assertSafe(root) {
  assert.equal(
    root.querySelector(
      'script,iframe,object,embed,svg,math,style,form,input,button,link,meta,base,template',
    ),
    null,
  )
  for (const element of root.querySelectorAll('*')) {
    for (const attribute of element.attributes) {
      assert.doesNotMatch(attribute.name, /^(on|style$|id$|name$|srcdoc$|data-|aria-)/i)
      if (attribute.name === 'href' || attribute.name === 'src') {
        const uri = attribute.value.replace(/[\u0000-\u0020]/g, '')
        assert.doesNotMatch(uri, /^(javascript|vbscript|file):/i)
        assert.doesNotMatch(uri, /^data:(?!image\/(png|jpeg|gif|webp);base64,)/i)
      }
    }
  }
}

const attacks = [
  ['image event handler', '<img src=x onerror="globalThis.__nebulaXssProbe=true">'],
  ['script tag', '<script>globalThis.__nebulaXssProbe=true</script>'],
  ['SVG event handler', '<svg onload="globalThis.__nebulaXssProbe=true"></svg>'],
  [
    'embedded frame',
    '<iframe srcdoc="<script>globalThis.__nebulaXssProbe=true</script>"></iframe>',
  ],
  [
    'MathML namespace',
    '<math><annotation-xml encoding="text/html"><img src=x onerror=alert(1)></annotation-xml></math>',
  ],
  [
    'CSS and clobbering attributes',
    '<div id="location" name="cookie" style="position:fixed;inset:0">content</div>',
  ],
  ['HTML form', '<form action="javascript:alert(1)"><button>click</button></form>'],
  ['HTML link', '<a href="javascript:alert(1)" onclick="alert(1)">link</a>'],
  ['javascript Markdown link', '[link](javascript:alert%281%29)'],
  ['mixed-case protocol', '[link](JaVaScRiPt:alert%281%29)'],
  ['HTML-encoded protocol', '[link](java&#x73;cript:alert%281%29)'],
  ['control character in protocol', '[link](java&#x09;script:alert%281%29)'],
  ['VBScript link', '[link](vbscript:msgbox%281%29)'],
  ['local file link', '[link](file:///C:/private.txt)'],
  ['HTML data URI', '[link](data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==)'],
  [
    'SVG data image',
    '![image](data:image/svg+xml;base64,PHN2ZyBvbmxvYWQ9ImFsZXJ0KDEpIj48L3N2Zz4=)',
  ],
  ['fence language attribute injection', '```js" onmouseover="alert(1)\nconst answer = 42\n```'],
  ['image alt attribute injection', '![alt" onerror="alert(1)](https://nebula.test/image.png)'],
]

for (const [name, payload] of attacks) {
  test(`blocks ${name}`, () => {
    const root = rendered(payload)
    assertSafe(root)
    assert.equal(dom.window.__nebulaXssProbe, undefined)
  })
}

test('raw HTML remains visible as text instead of becoming elements', () => {
  const input = '<img src=x onerror="alert(1)">'
  const root = rendered(input)
  assert.equal(root.querySelector('img'), null)
  assert.equal(root.textContent.trim(), input)
})

test('keeps normal headings, emphasis, lists, quotes and tables', () => {
  const root = rendered(
    '# Nebula\n\n**bold** *italic* ~~removed~~\n\n- one\n- two\n\n> quote\n\n| left | right |\n| :--- | ---: |\n| A | B |',
  )
  assert.equal(root.querySelector('h1').textContent, 'Nebula')
  assert.equal(root.querySelector('strong').textContent, 'bold')
  assert.equal(root.querySelector('em').textContent, 'italic')
  assert.equal(root.querySelector('s').textContent, 'removed')
  assert.equal(root.querySelectorAll('li').length, 2)
  assert.equal(root.querySelector('blockquote').textContent.trim(), 'quote')
  assert.equal(root.querySelector('th').getAttribute('align'), 'left')
  assert.equal(root.querySelector('td:last-child').getAttribute('align'), 'right')
  assertSafe(root)
})

test('keeps safe links, images, titles and ordered-list numbering', () => {
  const root = rendered(
    '[docs](https://nebula.test/docs "documentation")\n\n![cloud](/images/cloud.png "cloud image")\n\n3. third\n4. fourth',
  )
  assert.equal(root.querySelector('a').getAttribute('href'), 'https://nebula.test/docs')
  assert.equal(root.querySelector('a').getAttribute('title'), 'documentation')
  assert.equal(root.querySelector('img').getAttribute('src'), '/images/cloud.png')
  assert.equal(root.querySelector('img').getAttribute('alt'), 'cloud')
  assert.equal(root.querySelector('ol').getAttribute('start'), '3')
  assertSafe(root)
})

test('highlights code and keeps HTML examples inert', () => {
  const code = 'const example = "<img src=x onerror=alert(1)>";\n'
  const root = rendered('```javascript\n' + code + '```')
  assert.equal(root.querySelector('pre code').textContent, code)
  assert.ok(root.querySelector('code.hljs.language-javascript .hljs-keyword'))
  assert.equal(root.querySelector('img'), null)
  assertSafe(root)
})

test('unknown languages and highlighting errors fall back to escaped code', (t) => {
  const code = '<img src=x onerror=alert(1)>\n'
  assert.equal(
    rendered('```not-a-language\n' + code + '```').querySelector('code').textContent,
    code,
  )
  t.mock.method(hljs, 'highlight', () => {
    throw new Error('highlighting unavailable')
  })
  const root = rendered('```javascript\n' + code + '```')
  assert.equal(root.querySelector('code').textContent, code)
  assertSafe(root)
})

test('the final sanitizer also rejects unsafe HTML produced by a rendering extension', (t) => {
  t.mock.method(hljs, 'highlight', () => ({
    value:
      '<script>alert(1)</script><svg onload="alert(1)"></svg><img src="/test.png" onerror="alert(1)" style="position:fixed" id="location" name="cookie"><span class="hljs-keyword">const</span>',
  }))
  const root = rendered('```javascript\nconst answer = 42\n```')
  assert.ok(root.querySelector('.hljs-keyword'))
  assertSafe(root)
})

test('unsupported sanitization environments fail closed to plain escaped text', (t) => {
  const supported = purifier.isSupported
  t.after(() => {
    purifier.isSupported = supported
  })
  purifier.isSupported = false
  const input = '<img src=x onerror=alert(1)>\n# Heading'
  const root = rendered(input)
  assert.equal(root.querySelector('pre').textContent, input)
  assert.equal(root.querySelector('img, h1'), null)
  assertSafe(root)
})

test('empty content remains empty', () => {
  assert.equal(renderMarkdown(''), '')
  assert.equal(renderMarkdown(null), '')
  assert.equal(renderMarkdown(undefined), '')
})
