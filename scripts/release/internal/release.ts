import {
  compareVersions,
  formatVersion,
  incrementVersion,
  parseVersionTag,
  type SemVersion,
} from './semver.ts'

export type ReleaseChange = 'none' | 'fix' | 'feature' | 'breaking'
export type VersionBump = 'none' | 'patch' | 'minor' | 'major'
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

// The release channel counts commits from the latest stable tag, or from the
// latest prerelease tag while no stable release exists (a promotion). Beta and
// alpha both derive their core from the latest non-alpha tag.
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

function coreOf(version: SemVersion): SemVersion {
  return { major: version.major, minor: version.minor, patch: version.patch }
}

// Keeps a produced version above the highest reachable tag by adopting its
// core; when the channel label ranks below the ceiling at that core (for
// example a Beta below a stable release), advance one more patch.
function raiseAbove(version: SemVersion, ceiling: SemVersion): SemVersion {
  if (compareVersions(version, ceiling) > 0) { return version }
  const raised: SemVersion = { ...version, ...coreOf(ceiling) }
  return compareVersions(raised, ceiling) > 0
    ? raised
    : { ...raised, patch: raised.patch + 1 }
}

interface CalculateOptions {
  channel: ReleaseChannel
  base: PreviousRelease
  change: ReleaseChange
  bumpByChange: Readonly<Record<ReleaseChange, VersionBump>>
  lastAlpha?: TagRef
  maxTag: TagRef
  hasTriggerCommits: boolean
}

export function calculateVersion(options: CalculateOptions): string | null {
  const bump = options.bumpByChange[options.change]
  if (bump === 'none') {
    // Only the first stable release promotes a prerelease core as-is.
    if (options.channel !== 'release' || !options.base.isPromotion) {
      return null
    }
    const promoted = coreOf(options.base.version)
    return formatVersion(raiseAbove(promoted, options.maxTag.version))
  }
  if (options.channel === 'alpha') { return planAlpha(options, bump) }

  const core = incrementVersion(options.base.version, bump)
  const version: SemVersion = options.channel === 'beta'
    ? { ...core, prerelease: 'beta' }
    : core
  return formatVersion(raiseAbove(version, options.maxTag.version))
}

function planAlpha(
  options: CalculateOptions,
  bump: 'patch' | 'minor' | 'major',
): string | null {
  if (!options.hasTriggerCommits) { return null }
  const next = incrementVersion(coreOf(options.base.version), bump)
  let core = next
  let sequence = 1
  const last = options.lastAlpha
  if (last && compareVersions(core, coreOf(last.version)) <= 0) {
    // Stay on the last alpha line: the same or a floored core only
    // advances the sequence.
    core = coreOf(last.version)
    sequence = (last.version.sequence ?? 0) + 1
  }
  let version: SemVersion = { ...core, prerelease: 'alpha', sequence }
  return formatVersion(raiseAbove(version, options.maxTag.version))
}
