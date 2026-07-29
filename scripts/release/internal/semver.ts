export interface SemVersion {
  major: number
  minor: number
  patch: number
  prerelease?: string
  legacySequence?: number
}

const TAG_PATTERN =
  /^v(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-([0-9A-Za-z-]+)(?:\.(0|[1-9]\d*))?)?$/

export function parseVersionTag(tag: string): SemVersion | undefined {
  const match = TAG_PATTERN.exec(tag)
  if (!match) { return undefined }

  return {
    major: Number(match[1]),
    minor: Number(match[2]),
    patch: Number(match[3]),
    ...(match[4] === undefined ? {} : { prerelease: match[4] }),
    ...(match[5] === undefined ? {} : { legacySequence: Number(match[5]) }),
  }
}

export function compareVersions(a: SemVersion, b: SemVersion): number {
  if (a.major !== b.major) { return a.major - b.major }
  if (a.minor !== b.minor) { return a.minor - b.minor }
  if (a.patch !== b.patch) { return a.patch - b.patch }
  if (a.prerelease !== b.prerelease) {
    if (a.prerelease === undefined) { return 1 }
    if (b.prerelease === undefined) { return -1 }
    return a.prerelease.localeCompare(b.prerelease)
  }
  return (a.legacySequence ?? -1) - (b.legacySequence ?? -1)
}

export function incrementVersion(
  current: SemVersion,
  bump: 'patch' | 'minor' | 'major',
): SemVersion {
  switch (bump) {
    case 'major':
      return { major: current.major + 1, minor: 0, patch: 0 }
    case 'minor':
      return { major: current.major, minor: current.minor + 1, patch: 0 }
    case 'patch':
      return {
        major: current.major,
        minor: current.minor,
        patch: current.patch + 1,
      }
  }
}

export function formatVersion(version: SemVersion): string {
  const core = `${version.major}.${version.minor}.${version.patch}`
  return version.prerelease ? `${core}-${version.prerelease}` : core
}
