import { assertEquals, assertThrows } from '@std/assert'

import {
  assertAboveReachable,
  channelOfVersion,
  parseReleaseVersion,
  type TagRef,
} from '../internal/release.ts'
import { parseVersionTag } from '../internal/semver.ts'

function ref(tag: string): TagRef {
  const version = parseVersionTag(tag)
  if (!version) { throw new Error(`Invalid tag: ${tag}`) }
  return { tag, version }
}

Deno.test('derives the channel from a manually entered version', () => {
  assertEquals(channelOfVersion(parseVersionTag('v1.5.1')!), 'release')
  assertEquals(channelOfVersion(parseVersionTag('v1.5.1-beta')!), 'beta')
  assertEquals(channelOfVersion(parseVersionTag('v1.5.1-alpha.3')!), 'alpha')
})

Deno.test('accepts well-formed release versions', () => {
  assertEquals(parseReleaseVersion('1.5.1').channel, 'release')
  assertEquals(parseReleaseVersion('1.6.0-beta').channel, 'beta')
  assertEquals(parseReleaseVersion('1.6.0-alpha.1').channel, 'alpha')
})

Deno.test('rejects malformed or channel-mismatched versions', () => {
  const rejects = (input: string) =>
    assertThrows(() => parseReleaseVersion(input))
  rejects('v1.5.1')
  rejects('1.5')
  rejects('1.05.1')
  rejects('1.5.1-rc.1')
  rejects('1.5.1-alpha')
  rejects('1.5.1-beta.2')
})

Deno.test('rejects versions at or below the reachable tags', () => {
  const maxTag = ref('v1.5.1-beta')
  assertThrows(() =>
    assertAboveReachable(parseVersionTag('v1.5.1-beta')!, maxTag)
  )
  assertThrows(() =>
    assertAboveReachable(parseVersionTag('v1.5.1-alpha.9')!, maxTag)
  )
  // A same-core alpha always sorts below its beta, so it needs a higher core.
  assertThrows(() =>
    assertAboveReachable(parseVersionTag('v1.5.1-alpha.10')!, maxTag)
  )
  assertAboveReachable(parseVersionTag('v1.5.2-beta')!, maxTag)
  assertAboveReachable(parseVersionTag('v1.5.2-alpha.1')!, maxTag)
})
