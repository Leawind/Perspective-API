import { assertEquals } from '@std/assert'

import { createReleaseArgs } from '../internal/ci.ts'
import type { PlannedRelease } from '../internal/release.ts'

Deno.test('creates the GitHub release and tag for the planned commit', () => {
  const plan: PlannedRelease = {
    head: '1234567890abcdef',
    channel: 'beta',
    version: '1.2.3-beta',
    tag: 'v1.2.3-beta',
    isPrerelease: true,
  }

  assertEquals(
    createReleaseArgs(
      plan,
      [
        'versions/1.21-fabric/build/libs/mod.jar',
        'build/skills/use-perspective-api/SKILL.md#use-perspective-api.md',
      ],
      'build/release/notes.md',
    ),
    [
      'release',
      'create',
      'v1.2.3-beta',
      '--title',
      '1.2.3-beta',
      '--notes-file',
      'build/release/notes.md',
      '--target',
      '1234567890abcdef',
      '--prerelease',
      'versions/1.21-fabric/build/libs/mod.jar',
      'build/skills/use-perspective-api/SKILL.md#use-perspective-api.md',
    ],
  )

  const alpha: PlannedRelease = {
    head: '1234567890abcdef',
    channel: 'alpha',
    version: '1.5.1-alpha.3',
    tag: 'v1.5.1-alpha.3',
    isPrerelease: true,
  }
  assertEquals(
    createReleaseArgs(alpha, [], 'build/release/notes.md'),
    [
      'release',
      'create',
      'v1.5.1-alpha.3',
      '--title',
      '1.5.1-alpha.3',
      '--notes-file',
      'build/release/notes.md',
      '--target',
      '1234567890abcdef',
      '--prerelease',
    ],
  )
})
