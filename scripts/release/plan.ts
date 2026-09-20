import * as ci from './internal/ci.ts'
import type {
  ReleaseChange,
  ReleaseChannel,
  VersionBump,
} from './internal/release.ts'

// Releasable commit types mapped to change tiers, shared by every channel.
const changeByType: Readonly<Record<string, ReleaseChange>> = {
  feat: 'feature',
  fix: 'fix',
  perf: 'fix',
  i18n: 'fix',
  revert: 'fix',
}

const bumpByChannel: Readonly<
  Record<ReleaseChannel, Readonly<Record<ReleaseChange, VersionBump>>>
> = {
  release: {
    none: 'none',
    breaking: 'major',
    feature: 'minor',
    fix: 'patch',
  },
  // The Beta stage keeps the major pinned and raises the minor on breaking
  // changes; Alpha shares the table because its core is the next Beta version.
  beta: {
    none: 'none',
    breaking: 'minor',
    feature: 'patch',
    fix: 'patch',
  },
  alpha: {
    none: 'none',
    breaking: 'minor',
    feature: 'patch',
    fix: 'patch',
  },
}

const channel = ci.releaseChannel()

if (channel === undefined || !ci.canPublish()) {
  ci.writePlan({ version: null })
} else {
  ci.planRelease({
    channel,
    changeByType,
    bumpByChange: bumpByChannel[channel],
  })
}
