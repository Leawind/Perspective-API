import { CommitParser } from 'npm:conventional-commits-parser@6.4.0'
import type { ReleaseCommit } from './release.ts'

const SKIP_RELEASE_PATTERN = /\[(?:skip release|release skip)\]/i
const parser = new CommitParser({
  headerPattern: /^([a-zA-Z][a-zA-Z0-9-]*)(?:\(([^)\r\n]+)\))?(!)?:\s+(.+)$/,
  headerCorrespondence: ['type', 'scope', 'breaking', 'subject'],
})

interface ParsedCommit {
  type?: string | null
  scope?: string | null
  breaking?: string | null
  subject?: string | null
  header: string
  notes: Array<{ text: string }>
  revert: { header: string } | null
}

export function parseCommit(
  sha: string,
  message: string,
): ReleaseCommit | undefined {
  if (SKIP_RELEASE_PATTERN.test(message)) { return undefined }

  const parsed = parser.parse(message) as ParsedCommit
  const type = parsed.type?.toLowerCase() ?? (parsed.revert ? 'revert' : '')
  const breakingDescriptions = parsed.notes
    .map((note) => note.text.trim())
    .filter(Boolean)
  const breaking = parsed.breaking === '!' || breakingDescriptions.length > 0

  if (!type) { return undefined }

  return {
    sha,
    type,
    ...(parsed.scope ? { scope: parsed.scope } : {}),
    summary: parsed.subject ?? parsed.revert?.header ?? parsed.header,
    breaking,
    breakingDescriptions,
  }
}

const SECTIONS = [
  ['Features', 'feat'],
  ['Bug Fixes', 'fix'],
  ['Performance Improvements', 'perf'],
  ['Internationalization', 'i18n'],
  ['Reverts', 'revert'],
] as const
const KNOWN_TYPES: ReadonlySet<string> = new Set(
  SECTIONS.map(([, type]) => type),
)

function commitLabel(commit: ReleaseCommit): string {
  const scope = commit.scope ? `(${commit.scope})` : ''
  return `${commit.type}${scope}: ${commit.summary} (${commit.sha.slice(0, 7)})`
}

function appendSection(
  lines: string[],
  title: string,
  commits: ReleaseCommit[],
): void {
  if (commits.length === 0) { return }
  lines.push('', `## ${title}`, '')
  for (const commit of commits) {
    lines.push(`- ${commitLabel(commit)}`)
    for (const description of commit.breakingDescriptions) {
      lines.push(`  - ${description}`)
    }
  }
}

export function generateReleaseNotes(
  version: string,
  commits: ReleaseCommit[],
): string {
  const lines = [`# ${version}`]
  appendSection(
    lines,
    'Breaking Changes',
    commits.filter((commit) => commit.breaking),
  )
  for (const [title, type] of SECTIONS) {
    appendSection(
      lines,
      title,
      commits.filter((commit) => !commit.breaking && commit.type === type),
    )
  }
  appendSection(
    lines,
    'Other Changes',
    commits.filter((commit) =>
      !commit.breaking && !KNOWN_TYPES.has(commit.type)
    ),
  )
  return `${lines.join('\n')}\n`
}
