import { generatePerspectiveApiSkill } from '../gen-skills.ts'
import * as ci from './internal/ci.ts'

ci.assertNotDryRun()
const plan = await ci.loadPublishableRelease()
await ci.assertRemoteTagTarget(plan)
const skillAsset = await generatePerspectiveApiSkill()
// The `#` label keeps the release asset downloadable as `use-perspective-api.md` while the
// generated file itself is a spec-compliant `SKILL.md`.
const assets = [
  ...await ci.findJarAssets(),
  `${skillAsset}#use-perspective-api.md`,
]
await ci.publishGitHubRelease(plan, assets, ci.NOTES_FILE)
