import { assertEquals } from '@std/assert'

import {
  compareVersions,
  formatVersion,
  incrementVersion,
  parseVersionTag,
} from '../internal/semver.ts'

Deno.test('parses stable, prerelease, and legacy tags', () => {
  assertEquals(parseVersionTag('v1.0.0-beta.13'), {
    major: 1,
    minor: 0,
    patch: 0,
    prerelease: 'beta',
    legacySequence: 13,
  })
  assertEquals(parseVersionTag('v2.3.4'), {
    major: 2,
    minor: 3,
    patch: 4,
  })
  assertEquals(parseVersionTag('unrelated'), undefined)
})

Deno.test('compares semantic core and legacy prerelease sequences', () => {
  const legacy = parseVersionTag('v1.0.0-beta.13')!
  const nextPatch = parseVersionTag('v1.0.1-beta')!
  assertEquals(compareVersions(nextPatch, legacy) > 0, true)
})

Deno.test('increments and formats semantic versions', () => {
  const current = parseVersionTag('v1.2.4-beta')!
  assertEquals(formatVersion(incrementVersion(current, 'patch')!), '1.2.5')
  assertEquals(formatVersion(incrementVersion(current, 'minor')!), '1.3.0')
  assertEquals(formatVersion(incrementVersion(current, 'major')!), '2.0.0')
})
