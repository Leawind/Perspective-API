import { assertEquals } from '@std/assert'

import { createReleaseArgs } from '../internal/ci.ts'
import type { PlannedRelease } from '../internal/release.ts'

Deno.test('creates the GitHub release and tag for the planned commit', () => {
  const plan: PlannedRelease = {
    head: '1234567890abcdef',
    branch: 'beta',
    version: '1.2.3-beta',
    tag: 'v1.2.3-beta',
    isPrerelease: true,
  }

  assertEquals(
    createReleaseArgs(
      plan,
      ['versions/1.21-fabric/build/libs/mod.jar'],
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
    ],
  )
})
