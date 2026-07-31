import * as ci from './internal/ci.ts'

const change: ci.ReleaseChange = ci.extractChange({
  feat: 'feature',
  fix: 'fix',
  perf: 'fix',
  i18n: 'fix',
  revert: 'fix',
})

let version: string | null = null
const branch = ci.currentBranch()

if (branch === 'release') {
  version = ci.bumpVersion(change, {
    none: 'none',
    breaking: 'major',
    feature: 'minor',
    fix: 'patch',
  })
} else if (branch === 'beta') {
  version = ci.bumpVersion(
    change,
    {
      none: 'none',
      breaking: 'minor',
      feature: 'patch',
      fix: 'patch',
    },
    { prerelease: 'beta' },
  )
}

if (version !== null) {
  console.log(`Bump version to ${version}`)

  ci.writePlan({
    version,
    tag: `v${version}`,
    isPrerelease: branch === 'beta',
  })
} else {
  ci.writePlan({ version: null })
}
