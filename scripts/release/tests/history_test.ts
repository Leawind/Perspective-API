import { assertEquals } from '@std/assert'

import {
  selectLastAlpha,
  selectMaxTag,
  selectReleaseBase,
} from '../internal/release.ts'

Deno.test('derives the Beta base from the latest non-alpha tag', () => {
  const base = selectReleaseBase(
    ['v1.0.0-beta.13', 'v1.1.0-beta', 'v1.0.0'],
    'beta',
  )
  assertEquals(base?.tag, 'v1.1.0-beta')
  assertEquals(base?.isPromotion, false)
})

Deno.test('derives the Alpha core base from Beta and stable tags only', () => {
  assertEquals(
    selectReleaseBase(['v1.5.1-beta', 'v1.6.0-alpha.2'], 'alpha')?.tag,
    'v1.5.1-beta',
  )
  assertEquals(
    selectReleaseBase(['v1.5.0', 'v1.4.9-beta'], 'alpha')?.tag,
    'v1.5.0',
  )
})

Deno.test('uses a prerelease only when no stable release exists', () => {
  const promotion = selectReleaseBase(
    ['v1.0.0-beta.13', 'v1.1.0-beta'],
    'release',
  )
  assertEquals(promotion?.tag, 'v1.1.0-beta')
  assertEquals(promotion?.isPromotion, true)

  const stable = selectReleaseBase(
    ['v1.1.0-beta', 'v1.1.0', 'v1.2.0-beta'],
    'release',
  )
  assertEquals(stable?.tag, 'v1.1.0')
  assertEquals(stable?.isPromotion, false)
})

Deno.test('promotes from the highest reachable prerelease', () => {
  assertEquals(
    selectReleaseBase(['v1.5.1-beta', 'v1.6.0-alpha.2'], 'release')?.tag,
    'v1.6.0-alpha.2',
  )
})

Deno.test('selects the latest alpha tag by sequence', () => {
  assertEquals(
    selectLastAlpha([
      'v1.5.0-alpha.2',
      'v1.5.1-alpha.9',
      'v1.5.1-alpha.10',
    ])?.tag,
    'v1.5.1-alpha.10',
  )
})

Deno.test('selects the highest tag across channels', () => {
  assertEquals(
    selectMaxTag(['v1.5.1-beta', 'v1.6.0-alpha.2', 'v1.5.0'])?.tag,
    'v1.6.0-alpha.2',
  )
})
