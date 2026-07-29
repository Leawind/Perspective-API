import * as ci from './internal/ci.ts'

const plan = await ci.loadPlannedRelease()

ci.replaceUnique({
  file: 'gradle.properties',
  findUnique: /^mod\.version=0\.0-SNAPSHOT$/,
  replaceWith: `mod.version=${plan.version}`,
})
