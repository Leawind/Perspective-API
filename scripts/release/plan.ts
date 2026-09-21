import * as ci from './internal/ci.ts'
import type { ReleaseChange } from './internal/release.ts'

// Types are mapped to change tiers for release notes filtering and for the
// advisory log shown while planning; the version itself is entered manually
// when dispatching the workflow.
const changeByType: Readonly<Record<string, ReleaseChange>> = {
  feat: 'feature',
  fix: 'fix',
  perf: 'fix',
  i18n: 'fix',
  revert: 'fix',
}

const version = ci.requestedVersion()

if (version === undefined || !ci.canPublish()) {
  ci.writePlan({ version: null })
} else {
  ci.planRelease({ versionInput: version, changeByType })
}
