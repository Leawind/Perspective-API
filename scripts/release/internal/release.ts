import {
  compareVersions,
  formatVersion,
  parseVersionTag,
  type SemVersion,
} from './semver.ts'

export type ReleaseChange = 'none' | 'fix' | 'feature' | 'breaking'
export type ReleaseChannel = 'release' | 'beta' | 'alpha'

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
  channel: ReleaseChannel
}

export type StoredReleasePlan = { version: null } | PlannedRelease

export interface TagRef {
  tag: string
  version: SemVersion
}

export interface PreviousRelease extends TagRef {
  isPromotion: boolean
}

const CHANGE_PRIORITY: Readonly<Record<ReleaseChange, number>> = {
  none: 0,
  fix: 1,
  feature: 2,
  breaking: 3,
}

// Advisory only: the release version is entered by the person dispatching the
// workflow; this classification just logs what the commits suggest so the
// entered version can be double-checked against the COMMIT.md guidance.
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
  console.log(`Strongest detected change: '${result}'`)
  return result
}

function latestTag(
  tags: Iterable<string>,
  accepts: (version: SemVersion) => boolean,
): TagRef | undefined {
  let latest: TagRef | undefined
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

// Release notes cover commits since the latest tag of the released channel;
// the first stable release falls back to the latest prerelease (a promotion).
export function selectReleaseBase(
  tags: Iterable<string>,
  channel: ReleaseChannel,
): PreviousRelease | undefined {
  if (channel === 'release') {
    const stable = latestTag(
      tags,
      (version) => version.prerelease === undefined,
    )
    if (stable) { return { ...stable, isPromotion: false } }
    const prerelease = latestTag(
      tags,
      (version) => version.prerelease !== undefined,
    )
    return prerelease && { ...prerelease, isPromotion: true }
  }
  const nonAlpha = latestTag(tags, (version) => version.prerelease !== 'alpha')
  return nonAlpha && { ...nonAlpha, isPromotion: false }
}

export function selectLastAlpha(tags: Iterable<string>): TagRef | undefined {
  return latestTag(tags, (version) => version.prerelease === 'alpha')
}

export function selectMaxTag(tags: Iterable<string>): TagRef | undefined {
  return latestTag(tags, () => true)
}

export function channelOfVersion(
  version: SemVersion,
): ReleaseChannel | undefined {
  if (version.prerelease === undefined) {
    return version.sequence === undefined ? 'release' : undefined
  }
  if (version.prerelease === 'beta') {
    return version.sequence === undefined ? 'beta' : undefined
  }
  if (version.prerelease === 'alpha') {
    return version.sequence === undefined ? undefined : 'alpha'
  }
  return undefined
}

export function parseReleaseVersion(
  input: string,
): { version: SemVersion; channel: ReleaseChannel } {
  // Manual input carries no leading 'v'; the tag pattern does.
  const version = parseVersionTag(`v${input}`)
  if (!version || formatVersion(version) !== input) {
    throw new Error(
      `Invalid version '${input}': expected <core>, <core>-beta, or <core>-alpha.<N> without a leading 'v'.`,
    )
  }
  const channel = channelOfVersion(version)
  if (!channel) {
    throw new Error(
      `Invalid version '${input}': only alpha and beta labels are allowed; alpha requires a sequence (X-alpha.N) while beta and release must not carry one.`,
    )
  }
  return { version, channel }
}

export function assertAboveReachable(
  version: SemVersion,
  maxTag: TagRef,
): void {
  if (compareVersions(version, maxTag.version) <= 0) {
    throw new Error(
      `Version ${
        formatVersion(version)
      } must be greater than the reachable tag ${maxTag.tag}.`,
    )
  }
}
