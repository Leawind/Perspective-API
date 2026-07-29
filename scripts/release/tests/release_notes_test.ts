import { generateReleaseNotes, parseCommit } from '../internal/commits.ts'

function assert(condition: boolean, message: string): void {
  if (!condition) { throw new Error(message) }
}

Deno.test('groups Conventional Commit release notes', () => {
  const notes = generateReleaseNotes('1.1.0-beta', [
    parseCommit('1111111', 'feat: add option')!,
    parseCommit('2222222', 'fix!: remove behavior')!,
  ])
  assert(notes.includes('## Breaking Changes'), 'breaking section is missing')
  assert(notes.includes('## Features'), 'features section is missing')

  const otherNotes = generateReleaseNotes('1.1.1-beta', [
    parseCommit('3333333', 'docs: update guide')!,
  ])
  assert(otherNotes.includes('## Other Changes'), 'fallback section is missing')
})
