import { assertEquals } from '@std/assert'

import {
  calculateVersion,
  type PreviousRelease,
  type ReleaseChange,
  type TagRef,
  type VersionBump,
} from '../internal/release.ts'
import { parseVersionTag } from '../internal/semver.ts'

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

function ref(tag: string): TagRef {
  const version = parseVersionTag(tag)
  if (!version) { throw new Error(`Invalid tag: ${tag}`) }
  return { tag, version }
}

function base(tag: string, isPromotion = false): PreviousRelease {
  return { ...ref(tag), isPromotion }
}

const ceiling = ref('v0.0.0')

Deno.test('maps the same change differently for Beta and stable releases', () => {
  assertEquals(
    calculateVersion({
      channel: 'beta',
      base: base('v1.2.3-beta'),
      change: 'feature',
      bumpByChange: betaBumps,
      maxTag: ceiling,
      hasTriggerCommits: true,
    }),
    '1.2.4-beta',
  )
  assertEquals(
    calculateVersion({
      channel: 'release',
      base: base('v1.2.3-beta'),
      change: 'feature',
      bumpByChange: mainBumps,
      maxTag: ceiling,
      hasTriggerCommits: true,
    }),
    '1.3.0',
  )
  assertEquals(
    calculateVersion({
      channel: 'beta',
      base: base('v1.2.3-beta'),
      change: 'breaking',
      bumpByChange: betaBumps,
      maxTag: ceiling,
      hasTriggerCommits: true,
    }),
    '1.3.0-beta',
  )
  assertEquals(
    calculateVersion({
      channel: 'release',
      base: base('v1.2.3-beta'),
      change: 'breaking',
      bumpByChange: mainBumps,
      maxTag: ceiling,
      hasTriggerCommits: true,
    }),
    '2.0.0',
  )
})

Deno.test('does not release a none change', () => {
  for (const channel of ['release', 'beta', 'alpha'] as const) {
    assertEquals(
      calculateVersion({
        channel,
        base: base('v1.2.3'),
        change: 'none',
        bumpByChange: channel === 'release' ? mainBumps : betaBumps,
        maxTag: ceiling,
        hasTriggerCommits: false,
      }),
      null,
    )
  }
})

Deno.test('promotes the first stable release without another change', () => {
  assertEquals(
    calculateVersion({
      channel: 'release',
      base: base('v1.2.3-beta', true),
      change: 'none',
      bumpByChange: mainBumps,
      maxTag: ceiling,
      hasTriggerCommits: false,
    }),
    '1.2.3',
  )
})

Deno.test('sequences Alpha versions on the same core', () => {
  assertEquals(
    calculateVersion({
      channel: 'alpha',
      base: base('v1.5.0-beta'),
      change: 'feature',
      bumpByChange: betaBumps,
      maxTag: ceiling,
      hasTriggerCommits: true,
    }),
    '1.5.1-alpha.1',
  )
  assertEquals(
    calculateVersion({
      channel: 'alpha',
      base: base('v1.5.0-beta'),
      change: 'fix',
      bumpByChange: betaBumps,
      lastAlpha: ref('v1.5.1-alpha.1'),
      maxTag: ceiling,
      hasTriggerCommits: true,
    }),
    '1.5.1-alpha.2',
  )
  assertEquals(
    calculateVersion({
      channel: 'alpha',
      base: base('v1.5.0-beta'),
      change: 'breaking',
      bumpByChange: betaBumps,
      lastAlpha: ref('v1.5.1-alpha.2'),
      maxTag: ceiling,
      hasTriggerCommits: true,
    }),
    '1.6.0-alpha.1',
  )
})

Deno.test('keeps Alpha above the last alpha line', () => {
  assertEquals(
    calculateVersion({
      channel: 'alpha',
      base: base('v1.5.0-beta'),
      change: 'fix',
      bumpByChange: betaBumps,
      lastAlpha: ref('v1.6.0-alpha.1'),
      maxTag: ceiling,
      hasTriggerCommits: true,
    }),
    '1.6.0-alpha.2',
  )
})

Deno.test('does not release Alpha without new trigger commits', () => {
  assertEquals(
    calculateVersion({
      channel: 'alpha',
      base: base('v1.5.0-beta'),
      change: 'feature',
      bumpByChange: betaBumps,
      lastAlpha: ref('v1.5.1-alpha.1'),
      maxTag: ceiling,
      hasTriggerCommits: false,
    }),
    null,
  )
})

Deno.test('floors versions below reachable tags', () => {
  assertEquals(
    calculateVersion({
      channel: 'release',
      base: base('v2.0.1'),
      change: 'fix',
      bumpByChange: mainBumps,
      maxTag: ref('v2.1.0-beta.3'),
      hasTriggerCommits: true,
    }),
    '2.1.0',
  )
  assertEquals(
    calculateVersion({
      channel: 'beta',
      base: base('v1.5.1-beta'),
      change: 'fix',
      bumpByChange: betaBumps,
      maxTag: ref('v1.6.0-alpha.1'),
      hasTriggerCommits: true,
    }),
    '1.6.0-beta',
  )
})
