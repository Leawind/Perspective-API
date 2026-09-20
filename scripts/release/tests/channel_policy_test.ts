import { assertEquals } from '@std/assert'

import {
  extractChangeFromCommits,
  type ReleaseCommit,
} from '../internal/release.ts'

function commit(type: string, breaking = false): ReleaseCommit {
  return {
    sha: '1234567890abcdef',
    type,
    summary: `${type} change`,
    breaking,
    breakingDescriptions: [],
  }
}

const changes = {
  feat: 'feature',
  fix: 'fix',
} as const

Deno.test('extracts the highest release change from commits', () => {
  assertEquals(extractChangeFromCommits([commit('docs')], changes), 'none')
  assertEquals(extractChangeFromCommits([commit('fix')], changes), 'fix')
  assertEquals(
    extractChangeFromCommits([commit('fix'), commit('feat')], changes),
    'feature',
  )
  assertEquals(
    extractChangeFromCommits([commit('feat'), commit('docs', true)], changes),
    'breaking',
  )
})
