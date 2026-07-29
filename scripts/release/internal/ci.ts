import { generateReleaseNotes, parseCommit } from './commits.ts'
import {
  calculateVersion,
  extractChangeFromCommits,
  selectPreviousRelease,
} from './release.ts'
import type {
  PlannedRelease,
  PreviousRelease,
  ReleaseChange,
  ReleaseCommit,
  ReleasePlan,
  StoredReleasePlan,
  VersionBump,
} from './release.ts'
import { formatVersion, parseVersionTag } from './semver.ts'

export type { ReleaseChange, ReleasePlan, VersionBump } from './release.ts'

const decoder = new TextDecoder()

export const PLAN_FILE = 'build/release/plan.json'
export const NOTES_FILE = 'build/release/notes.md'

interface CommandResult {
  code: number
  stdout: string
  stderr: string
}

interface ReleaseHistory {
  head: string
  previous: PreviousRelease
  commits: ReleaseCommit[]
}

let history: ReleaseHistory | undefined
let changeByCommitType: Readonly<Record<string, ReleaseChange>> = {}

async function capture(
  command: string,
  args: string[],
  options: { check?: boolean } = {},
): Promise<CommandResult> {
  const output = await new Deno.Command(command, {
    args,
    stdout: 'piped',
    stderr: 'piped',
  }).output()
  const result = {
    code: output.code,
    stdout: decoder.decode(output.stdout),
    stderr: decoder.decode(output.stderr),
  }
  if (options.check !== false && result.code !== 0) {
    throw new Error(
      `${command} ${args.join(' ')} failed (${result.code}):\n${result.stderr}`,
    )
  }
  return result
}

function captureSync(command: string, args: string[]): CommandResult {
  const output = new Deno.Command(command, {
    args,
    stdout: 'piped',
    stderr: 'piped',
  }).outputSync()
  const result = {
    code: output.code,
    stdout: decoder.decode(output.stdout),
    stderr: decoder.decode(output.stderr),
  }
  if (result.code !== 0) {
    throw new Error(
      `${command} ${args.join(' ')} failed (${result.code}):\n${result.stderr}`,
    )
  }
  return result
}

async function run(
  command: string,
  args: string[],
  env?: Record<string, string>,
): Promise<void> {
  const status = await new Deno.Command(command, {
    args,
    env,
    stdin: 'inherit',
    stdout: 'inherit',
    stderr: 'inherit',
  }).spawn().status
  if (!status.success) {
    throw new Error(`${command} ${args.join(' ')} failed (${status.code})`)
  }
}

function saveJsonSync(path: string, value: unknown): void {
  const separator = path.lastIndexOf('/')
  if (separator >= 0) {
    Deno.mkdirSync(path.slice(0, separator), { recursive: true })
  }
  Deno.writeTextFileSync(path, `${JSON.stringify(value, null, 2)}\n`)
}

async function loadJson<T>(
  path: string,
  missingMessage: string,
): Promise<T> {
  try {
    return JSON.parse(await Deno.readTextFile(path)) as T
  } catch (error) {
    if (error instanceof Deno.errors.NotFound) {
      throw new Error(missingMessage)
    }
    throw error
  }
}

export function replaceUniqueInText(
  text: string,
  from: RegExp,
  to: string,
): string {
  const flags = [...new Set(`${from.flags}gm`)].join('')
  const pattern = new RegExp(from.source, flags)
  const matches = [...text.matchAll(pattern)]
  if (matches.length !== 1) {
    throw new Error(
      `Expected ${String(from)} exactly once, found ${matches.length}.`,
    )
  }
  return text.replace(pattern, to)
}

export function replaceUnique(options: {
  file: string
  findUnique: RegExp
  replaceWith: string
}): void {
  const text = Deno.readTextFileSync(options.file)
  try {
    Deno.writeTextFileSync(
      options.file,
      replaceUniqueInText(text, options.findUnique, options.replaceWith),
    )
  } catch (error) {
    if (error instanceof Error) {
      throw new Error(`${error.message} File: ${options.file}`, {
        cause: error,
      })
    }
    throw error
  }
}

function git(...args: string[]): string {
  return captureSync('git', args).stdout.trim()
}

export function currentBranch(): string {
  return Deno.env.get('GITHUB_REF_NAME') ?? git('branch', '--show-current')
}

function readCommits(range: string): ReleaseCommit[] {
  const output = git('log', '--reverse', '--format=%H%x00%B%x00', range)
  if (!output) { return [] }

  const fields = output.split('\0')
  const commits: ReleaseCommit[] = []
  for (let index = 0; index + 1 < fields.length; index += 2) {
    const sha = fields[index].trim()
    if (!sha) { continue }
    const commit = parseCommit(sha, fields[index + 1])
    if (commit) { commits.push(commit) }
  }
  return commits
}

function readReleaseHistory(prerelease?: string): ReleaseHistory {
  const tags = git('tag', '--merged', 'HEAD')
    .split('\n')
    .map((tag) => tag.trim())
    .filter(Boolean)
  const previous = selectPreviousRelease(tags, prerelease)
  if (!previous) {
    const kind = prerelease === undefined ? 'stable or prerelease' : prerelease
    throw new Error(`No reachable ${kind} release tag was found.`)
  }
  return {
    head: git('rev-parse', 'HEAD'),
    previous,
    commits: readCommits(`${previous.tag}..HEAD`),
  }
}

function expectedPrerelease(): string | undefined {
  return currentBranch() === 'beta' ? 'beta' : undefined
}

export function extractChange(
  map: Readonly<Record<string, ReleaseChange>>,
): ReleaseChange {
  changeByCommitType = map
  history = readReleaseHistory(expectedPrerelease())
  return extractChangeFromCommits(history.commits, map)
}

function canPublish(): boolean {
  const event = Deno.env.get('GITHUB_EVENT_NAME')
  return event === undefined || event === 'push'
}

export function bumpVersion(
  change: ReleaseChange,
  map: Readonly<Record<ReleaseChange, VersionBump>>,
  options: { prerelease: string } | undefined = undefined,
): string | null {
  if (!canPublish()) { return null }
  const releaseHistory = readReleaseHistory(options?.prerelease)
  history = releaseHistory
  return calculateVersion(
    releaseHistory.previous,
    change,
    map,
    options?.prerelease,
  )
}

function appendGithubOutputSync(values: object): void {
  const outputFile = Deno.env.get('GITHUB_OUTPUT')
  if (!outputFile) { return }
  const output = Object.entries(values)
    .map(([name, value]) => `${name}=${String(value)}`)
    .join('\n')
  Deno.writeTextFileSync(outputFile, `${output}\n`, { append: true })
}

function validateReleaseVersion(plan: {
  version: string
  tag: string
  isPrerelease: boolean
}): void {
  const version = parseVersionTag(plan.tag)
  if (!version || formatVersion(version) !== plan.version) {
    throw new Error(`Invalid release version: ${plan.version}`)
  }
  if ((version.prerelease !== undefined) !== plan.isPrerelease) {
    throw new Error(
      `Release prerelease flag does not match version: ${plan.version}`,
    )
  }
}

export function writePlan(plan: ReleasePlan): void {
  if (plan.version === null) {
    saveJsonSync(PLAN_FILE, plan)
    try {
      Deno.removeSync(NOTES_FILE)
    } catch (error) {
      if (!(error instanceof Deno.errors.NotFound)) { throw error }
    }
    appendGithubOutputSync({ release: false })
    return
  }

  if (plan.tag !== `v${plan.version}`) {
    throw new Error(`Release tag does not match version: ${plan.tag}`)
  }
  validateReleaseVersion(plan)
  if (!history) {
    throw new Error('Release history not found: call extractChange first.')
  }
  const releaseHistory = history
  const storedPlan: PlannedRelease = {
    ...plan,
    head: releaseHistory.head,
    branch: currentBranch(),
  }
  saveJsonSync(PLAN_FILE, storedPlan)

  const commits = releaseHistory.commits.filter((commit) =>
    commit.breaking || (changeByCommitType[commit.type] ?? 'none') !== 'none'
  )
  const notes = releaseHistory.previous.isPromotion && commits.length === 0
    ? `# ${plan.version}\n\nPromoted from ${
      releaseHistory.previous.tag.slice(1)
    }.\n`
    : generateReleaseNotes(plan.version, commits)
  Deno.writeTextFileSync(NOTES_FILE, notes)
  appendGithubOutputSync({ release: true })
}

function isPlannedRelease(plan: StoredReleasePlan): plan is PlannedRelease {
  return plan.version !== null
    && typeof plan.version === 'string'
    && typeof plan.tag === 'string'
    && typeof plan.isPrerelease === 'boolean'
    && typeof plan.head === 'string'
    && typeof plan.branch === 'string'
}

export async function loadPlannedRelease(): Promise<PlannedRelease> {
  const plan = await loadJson<StoredReleasePlan>(
    PLAN_FILE,
    'Release plan not found: run plan.ts first.',
  )
  if (!isPlannedRelease(plan)) {
    throw new Error('The release plan does not contain a release.')
  }
  if (plan.tag !== `v${plan.version}`) {
    throw new Error(`Release tag does not match version: ${plan.tag}`)
  }
  validateReleaseVersion(plan)
  await verifyPlannedHead(plan.head)
  return plan
}

export async function loadPublishableRelease(): Promise<PlannedRelease> {
  const plan = await loadPlannedRelease()
  await assertReleaseBranch(plan.branch)
  return plan
}

export async function verifyPlannedHead(expectedHead: string): Promise<void> {
  const head = (await capture('git', ['rev-parse', 'HEAD'])).stdout.trim()
  if (head !== expectedHead) {
    throw new Error(
      `HEAD changed after planning: expected ${expectedHead}, found ${head}.`,
    )
  }
}

export async function assertReleaseBranch(
  expectedBranch: string,
): Promise<void> {
  const branch = Deno.env.get('GITHUB_REF_NAME')
    ?? (await capture('git', ['branch', '--show-current'])).stdout.trim()
  if (branch !== expectedBranch) {
    throw new Error(
      `Release plan targets ${expectedBranch}, but the current branch is ${branch}.`,
    )
  }
}

async function localTagCommit(tag: string): Promise<string | undefined> {
  const result = await capture(
    'git',
    ['rev-parse', '--verify', '--quiet', `${tag}^{commit}`],
    { check: false },
  )
  return result.code === 0 ? result.stdout.trim() : undefined
}

async function remoteTagCommit(tag: string): Promise<string | undefined> {
  const result = await capture(
    'git',
    [
      'ls-remote',
      '--tags',
      'origin',
      `refs/tags/${tag}`,
      `refs/tags/${tag}^{}`,
    ],
    { check: false },
  )
  if (result.code !== 0) {
    throw new Error(`Unable to check remote tag ${tag}: ${result.stderr}`)
  }
  if (!result.stdout.trim()) { return undefined }

  const references = result.stdout.trim().split('\n')
  const dereferenced = references.find((line) => line.endsWith('^{}'))
  return (dereferenced ?? references[0]).split(/\s+/)[0]
}

export async function assertRemoteTagTarget(
  plan: PlannedRelease,
): Promise<void> {
  const localCommit = await localTagCommit(plan.tag)
  if (localCommit && localCommit !== plan.head) {
    throw new Error(`${plan.tag} already points to a different commit.`)
  }
  const remoteCommit = await remoteTagCommit(plan.tag)
  if (remoteCommit && remoteCommit !== plan.head) {
    throw new Error(`${plan.tag} already exists remotely at another commit.`)
  }
}

export async function findJarAssets(): Promise<string[]> {
  const assets: string[] = []

  for await (const version of Deno.readDir('versions')) {
    if (!version.isDirectory) { continue }
    const directory = `versions/${version.name}/build/libs`
    try {
      for await (const entry of Deno.readDir(directory)) {
        if (entry.isFile && entry.name.endsWith('.jar')) {
          assets.push(`${directory}/${entry.name}`)
        }
      }
    } catch (error) {
      if (!(error instanceof Deno.errors.NotFound)) { throw error }
    }
  }

  assets.sort()
  if (assets.length === 0) {
    throw new Error('No JAR assets found in version build directories.')
  }
  return assets
}

export function assertNotDryRun(): void {
  if (Deno.env.get('DRY_RUN') !== 'false') {
    throw new Error('Refusing to publish unless DRY_RUN is exactly "false".')
  }
}

export function createReleaseArgs(
  plan: PlannedRelease,
  assets: string[],
  notesFile: string,
): string[] {
  const args = [
    'release',
    'create',
    plan.tag,
    '--title',
    plan.version,
    '--notes-file',
    notesFile,
    '--target',
    plan.head,
  ]
  if (plan.isPrerelease) { args.push('--prerelease') }
  args.push(...assets)
  return args
}

export async function publishGitHubRelease(
  plan: PlannedRelease,
  assets: string[],
  notesFile: string,
): Promise<void> {
  const release = await capture(
    'gh',
    ['release', 'view', plan.tag, '--json', 'tagName'],
    { check: false },
  )
  if (release.code !== 0) {
    await run('gh', createReleaseArgs(plan, assets, notesFile))
    return
  }

  await run('gh', ['release', 'upload', plan.tag, ...assets, '--clobber'])
  await run('gh', [
    'release',
    'edit',
    plan.tag,
    '--title',
    plan.version,
    '--notes-file',
    notesFile,
    plan.isPrerelease ? '--prerelease' : '--latest',
  ])
}
