import { assertEquals } from '@std/assert'

import { replaceUniqueInText } from '../internal/ci.ts'

Deno.test('replaces exactly the Gradle project version', () => {
  assertEquals(
    replaceUniqueInText(
      'mod.id=perspective-api\nmod.version=0.0-SNAPSHOT\n',
      /^mod\.version=0\.0-SNAPSHOT$/,
      'mod.version=1.2.3-beta',
    ),
    'mod.id=perspective-api\nmod.version=1.2.3-beta\n',
  )
})

Deno.test('rejects a missing snapshot placeholder', () => {
  let rejected = false
  try {
    replaceUniqueInText(
      'mod.version=1.0.0\n',
      /^mod\.version=0\.0-SNAPSHOT$/,
      'mod.version=1.2.3',
    )
  } catch {
    rejected = true
  }
  assertEquals(rejected, true)
})

Deno.test('rejects duplicate snapshot placeholders', () => {
  let rejected = false
  try {
    replaceUniqueInText(
      'mod.version=0.0-SNAPSHOT\nother.version=0.0-SNAPSHOT\n',
      /^(?:mod|other)\.version=0\.0-SNAPSHOT$/,
      'mod.version=1.2.3',
    )
  } catch {
    rejected = true
  }
  assertEquals(rejected, true)
})
