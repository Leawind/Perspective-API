import { assertEquals } from '@std/assert'

import {
  compareVersions,
  formatVersion,
  parseVersionTag,
} from '../internal/semver.ts'

Deno.test('parses stable, prerelease, and sequenced tags', () => {
  assertEquals(parseVersionTag('v1.0.0-beta.13'), {
    major: 1,
    minor: 0,
    patch: 0,
    prerelease: 'beta',
    sequence: 13,
  })
  assertEquals(parseVersionTag('v1.5.1-alpha.3'), {
    major: 1,
    minor: 5,
    patch: 1,
    prerelease: 'alpha',
    sequence: 3,
  })
  assertEquals(parseVersionTag('v2.3.4'), {
    major: 2,
    minor: 3,
    patch: 4,
  })
  assertEquals(parseVersionTag('unrelated'), undefined)
})

Deno.test('compares cores, labels, and sequences', () => {
  const at = (tag: string) => parseVersionTag(tag)!
  assertEquals(
    compareVersions(at('v1.0.1-beta'), at('v1.0.0-beta.13')) > 0,
    true,
  )
  assertEquals(
    compareVersions(at('v1.5.1-alpha.9'), at('v1.5.1-alpha.10')) < 0,
    true,
  )
  assertEquals(
    compareVersions(at('v1.5.1-alpha.10'), at('v1.5.1-beta')) < 0,
    true,
  )
  assertEquals(compareVersions(at('v1.5.1-beta'), at('v1.5.1')) < 0, true)
})

Deno.test('formats stable, labeled, and sequenced versions', () => {
  assertEquals(formatVersion(parseVersionTag('v1.2.4')!), '1.2.4')
  assertEquals(formatVersion(parseVersionTag('v1.2.4-beta')!), '1.2.4-beta')
  assertEquals(
    formatVersion(parseVersionTag('v1.5.1-alpha.3')!),
    '1.5.1-alpha.3',
  )
})
