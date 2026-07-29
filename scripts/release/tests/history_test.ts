import { assertEquals } from '@std/assert'

import { selectPreviousRelease } from '../internal/release.ts'

Deno.test('selects the latest tag for the requested prerelease', () => {
  const previous = selectPreviousRelease(
    ['v1.0.0-beta.13', 'v1.1.0-beta', 'v1.0.0'],
    'beta',
  )
  assertEquals(previous?.tag, 'v1.1.0-beta')
  assertEquals(previous?.isPromotion, false)
})

Deno.test('uses a prerelease only when no stable release exists', () => {
  const promotion = selectPreviousRelease(
    ['v1.0.0-beta.13', 'v1.1.0-beta'],
  )
  assertEquals(promotion?.tag, 'v1.1.0-beta')
  assertEquals(promotion?.isPromotion, true)

  const stable = selectPreviousRelease(
    ['v1.1.0-beta', 'v1.1.0', 'v1.2.0-beta'],
  )
  assertEquals(stable?.tag, 'v1.1.0')
  assertEquals(stable?.isPromotion, false)
})
