import type { Options } from 'semantic-release'

const options: Options = {
  branches: [
    {
      name: 'main',
      prerelease: false,
    },
    {
      name: 'beta',
      prerelease: true,
    },
  ],
  tagFormat: 'v${version}',
  plugins: [
    [
      '@semantic-release/commit-analyzer',
      {
        preset: 'conventionalcommits',
      },
    ],
    [
      '@semantic-release/release-notes-generator',
      {
        preset: 'conventionalcommits',
      },
    ],
    'semantic-release-export-data',
    [
      '@semantic-release/changelog',
      {
        changelogTitle: '# Changelog',
        changelogFile: 'CHANGELOG.md',
      },
    ],
    [
      '@semantic-release/github',
      {
        assets: [
          {
            path: 'build/libs/*.jar',
          },
        ],
      },
    ],
    [
      '@semantic-release/exec',
      {
        prepareCmd: './scripts/release.sh "${nextRelease.version}"',
      },
    ],
  ],
}

const releasercPath = '.releaserc.json'
const releasercContent = JSON.stringify(options, null, 2) + '\n'

Deno.writeTextFileSync(releasercPath, releasercContent)
