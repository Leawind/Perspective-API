import {
  compareVersions,
  formatVersion,
  incrementVersion,
  parseVersionTag,
  type SemVersion,
} from './semver.ts'

export type ReleaseChange = 'none' | 'fix' | 'feature' | 'breaking'
export type VersionBump = 'none' | 'patch' | 'minor' | 'major'

export interface ReleaseCommit {
  sha: string
  type: string
  scope?: string
  summary: string
  breaking: boolean
  breakingDescriptions: string[]
}

export type ReleasePlan =
  | { version: null }
  | {
    version: string
    tag: string
    isPrerelease: boolean
  }

export interface PlannedRelease {
  version: string
  tag: string
  isPrerelease: boolean
  head: string
  branch: string
}

export type StoredReleasePlan = { version: null } | PlannedRelease

export interface PreviousRelease {
  tag: string
  version: SemVersion
  isPromotion: boolean
}

const CHANGE_PRIORITY: Readonly<Record<ReleaseChange, number>> = {
  none: 0,
  fix: 1,
  feature: 2,
  breaking: 3,
}

export function extractChangeFromCommits(
  commits: Iterable<ReleaseCommit>,
  changeByCommitType: Readonly<Record<string, ReleaseChange>>,
): ReleaseChange {
  let result: ReleaseChange = 'none'
  console.log(`{change}: commit summary`)
  console.log(`------------------------`)
  for (const commit of commits) {
    const change = commit.breaking
      ? 'breaking'
      : changeByCommitType[commit.type] ?? 'none'

    console.log(`{${change}}: ${commit.summary}`)

    if (CHANGE_PRIORITY[change] > CHANGE_PRIORITY[result]) {
      result = change
    }
  }
  console.log(`------------------------`)
  console.log(`Final result: '${result}'`)
  return result
}

function latestVersion(
  tags: Iterable<string>,
  accepts: (version: SemVersion) => boolean,
): { tag: string; version: SemVersion } | undefined {
  let latest: { tag: string; version: SemVersion } | undefined
  for (const tag of tags) {
    const version = parseVersionTag(tag)
    if (
      version
      && accepts(version)
      && (!latest || compareVersions(version, latest.version) > 0)
    ) {
      latest = { tag, version }
    }
  }
  return latest
}

export function selectPreviousRelease(
  tags: Iterable<string>,
  prerelease?: string,
): PreviousRelease | undefined {
  const allTags = [...tags]
  const primary = latestVersion(
    allTags,
    (version) => version.prerelease === prerelease,
  )
  if (primary) { return { ...primary, isPromotion: false } }
  if (prerelease !== undefined) { return undefined }

  const promotion = latestVersion(
    allTags,
    (version) => version.prerelease !== undefined,
  )
  return promotion ? { ...promotion, isPromotion: true } : undefined
}

export function calculateVersion(
  previous: PreviousRelease,
  change: ReleaseChange,
  bumpByChange: Readonly<Record<ReleaseChange, VersionBump>>,
  prerelease?: string,
): string | null {
  const bump = bumpByChange[change]
  if (bump === 'none') {
    if (!previous.isPromotion || prerelease !== undefined) { return null }
    return formatVersion({
      major: previous.version.major,
      minor: previous.version.minor,
      patch: previous.version.patch,
    })
  }

  return formatVersion({
    ...incrementVersion(previous.version, bump),
    ...(prerelease === undefined ? {} : { prerelease }),
  })
}
