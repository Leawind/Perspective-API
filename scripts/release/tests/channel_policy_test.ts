import { assertEquals } from '@std/assert'

import {
  calculateVersion,
  type ReleaseChange,
  type VersionBump,
} from '../internal/release.ts'

const mainBumps: Readonly<Record<ReleaseChange, VersionBump>> = {
  none: 'none',
  breaking: 'major',
  feature: 'minor',
  fix: 'patch',
}
const betaBumps: Readonly<Record<ReleaseChange, VersionBump>> = {
  none: 'none',
  breaking: 'minor',
  feature: 'patch',
  fix: 'patch',
}
const previous = {
  tag: 'v1.2.3-beta',
  version: { major: 1, minor: 2, patch: 3, prerelease: 'beta' },
  isPromotion: false,
}

Deno.test('maps the same change differently for Beta and stable releases', () => {
  assertEquals(
    calculateVersion(previous, 'feature', betaBumps, 'beta'),
    '1.2.4-beta',
  )
  assertEquals(calculateVersion(previous, 'feature', mainBumps), '1.3.0')
  assertEquals(
    calculateVersion(previous, 'breaking', betaBumps, 'beta'),
    '1.3.0-beta',
  )
  assertEquals(calculateVersion(previous, 'breaking', mainBumps), '2.0.0')
})
