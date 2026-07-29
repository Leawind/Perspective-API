import { assertEquals } from '@std/assert'

import {
  calculateVersion,
  type ReleaseChange,
  type VersionBump,
} from '../internal/release.ts'

const bumps: Readonly<Record<ReleaseChange, VersionBump>> = {
  none: 'none',
  breaking: 'major',
  feature: 'minor',
  fix: 'patch',
}

Deno.test('does not release a none change', () => {
  assertEquals(
    calculateVersion(
      {
        tag: 'v1.2.3',
        version: { major: 1, minor: 2, patch: 3 },
        isPromotion: false,
      },
      'none',
      bumps,
    ),
    null,
  )
})

Deno.test('promotes the first stable release without another change', () => {
  assertEquals(
    calculateVersion(
      {
        tag: 'v1.2.3-beta',
        version: { major: 1, minor: 2, patch: 3, prerelease: 'beta' },
        isPromotion: true,
      },
      'none',
      bumps,
    ),
    '1.2.3',
  )
})
