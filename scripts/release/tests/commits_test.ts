import { parseCommit } from '../internal/commits.ts'

function assert(condition: boolean, message: string): void {
  if (!condition) { throw new Error(message) }
}

Deno.test('parses releasable conventional commits', () => {
  const feature = parseCommit('1234567890', 'feat(api): add callbacks')
  assert(feature?.type === 'feat', 'feature type was not parsed')
  assert(feature?.scope === 'api', 'feature scope was not parsed')
  assert(feature?.breaking === false, 'feature must be compatible')

  const breaking = parseCommit(
    'abcdef1234',
    'fix(impl): cache availability\n\nBREAKING CHANGE: evaluation timing changed',
  )
  assert(breaking?.breaking === true, 'breaking footer was not parsed')
  assert(
    breaking?.breakingDescriptions[0] === 'evaluation timing changed',
    'breaking description was not parsed',
  )

  const multiLineBreaking = parseCommit(
    'abcdef1234',
    'fix: change behavior\n\nBREAKING CHANGE: migration step one\nmigration step two',
  )
  assert(
    multiLineBreaking?.breakingDescriptions[0]
      === 'migration step one\nmigration step two',
    'multiline breaking description was not preserved',
  )
})

Deno.test('recognizes bang syntax and release skips', () => {
  assert(
    parseCommit('1234567', 'refactor(api)!: remove old API')?.breaking === true,
    'bang syntax was not recognized',
  )
  assert(
    parseCommit('1234567', 'feat: internal experiment [skip release]')
      === undefined,
    'skip release was not honored',
  )
  assert(
    parseCommit('1234567', 'refactor: simplify code')?.type === 'refactor',
    'valid non-releasing commit was discarded before policy analysis',
  )
  assert(
    parseCommit('1234567', 'revert: feat(api): add callbacks')?.type
      === 'revert',
    'revert did not trigger a compatible release',
  )
  assert(
    parseCommit(
      '1234567',
      'Revert "feat(api): add callbacks"\n\nThis reverts commit 1234567.',
    )?.type === 'revert',
    'Git revert was not recognized',
  )
})
