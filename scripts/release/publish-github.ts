import { generatePerspectiveApiSkill } from '../gen-skills.ts'
import * as ci from './internal/ci.ts'

ci.assertNotDryRun()
const plan = await ci.loadPublishableRelease()
await ci.assertRemoteTagTarget(plan)
const skillAsset = await generatePerspectiveApiSkill()
const assets = [...await ci.findJarAssets(), skillAsset]
await ci.publishGitHubRelease(plan, assets, ci.NOTES_FILE)
