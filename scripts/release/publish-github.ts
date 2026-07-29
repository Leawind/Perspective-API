import * as ci from './internal/ci.ts'

ci.assertNotDryRun()
const plan = await ci.loadPublishableRelease()
await ci.assertRemoteTagTarget(plan)
const assets = await ci.findJarAssets()
await ci.publishGitHubRelease(plan, assets, ci.NOTES_FILE)
