import { generateReleaseNotes, parseCommit } from './commits.ts'
import {
  assertAboveReachable,
  extractChangeFromCommits,
  parseReleaseVersion,
  type PlannedRelease,
  type PreviousRelease,
  type ReleaseChange,
  type ReleaseChannel,
  type ReleaseCommit,
  type ReleasePlan,
  selectLastAlpha,
  selectMaxTag,
  selectReleaseBase,
  type StoredReleasePlan,
  type TagRef,
} from './release.ts'
import { formatVersion, parseVersionTag } from './semver.ts'

export type { ReleaseChange, ReleaseChannel, ReleasePlan } from './release.ts'

const decoder = new TextDecoder()

export const PLAN_FILE = 'build/release/plan.json'
export const NOTES_FILE = 'build/release/notes.md'

interface CommandResult {
  code: number
  stdout: string
  stderr: string
}

interface ReleaseHistory {
  channel: ReleaseChannel
  head: string
  base: PreviousRelease
  maxTag: TagRef
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

const RELEASE_CHANNELS: ReadonlySet<string> = new Set([
  'release',
  'beta',
  'alpha',
])

export function requestedVersion(): string | undefined {
  const value = Deno.env.get('RELEASE_VERSION')?.trim()
  return value === undefined || value === '' ? undefined : value
}

// Publishing only happens on manual dispatches (and locally); pushes and
// pull requests merely build and test.
export function canPublish(): boolean {
  const event = Deno.env.get('GITHUB_EVENT_NAME')
  return event === undefined || event === 'workflow_dispatch'
}

function isAncestor(ancestor: string, descendant: string): boolean {
  return captureSync('git', [
    'merge-base',
    '--is-ancestor',
    ancestor,
    descendant,
  ]).code === 0
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

function readReleaseHistory(channel: ReleaseChannel): ReleaseHistory {
  const tags = git('tag', '--merged', 'HEAD')
    .split('\n')
    .map((tag) => tag.trim())
    .filter(Boolean)
  const base = selectReleaseBase(tags, channel)
  if (!base) {
    throw new Error(
      channel === 'release'
        ? 'No reachable release tag was found.'
        : 'No reachable beta or stable release tag was found.',
    )
  }
  // Alpha notes cover commits since its own latest tag when that tag is
  // newer than the core base.
  const lastAlpha = selectLastAlpha(tags)
  const notesTag = channel === 'alpha' && lastAlpha
      && isAncestor(base.tag, lastAlpha.tag)
    ? lastAlpha.tag
    : base.tag
  return {
    channel,
    head: git('rev-parse', 'HEAD'),
    base,
    maxTag: selectMaxTag(tags) ?? base,
    commits: readCommits(`${notesTag}..HEAD`),
  }
}

export function planRelease(options: {
  versionInput: string
  changeByType: Readonly<Record<string, ReleaseChange>>
}): void {
  const requested = parseReleaseVersion(options.versionInput)
  const releaseHistory = readReleaseHistory(requested.channel)
  history = releaseHistory
  changeByCommitType = options.changeByType
  assertAboveReachable(requested.version, releaseHistory.maxTag)
  extractChangeFromCommits(releaseHistory.commits, options.changeByType)

  const version = formatVersion(requested.version)
  writePlan({
    version,
    tag: `v${version}`,
    isPrerelease: requested.channel !== 'release',
  })
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
  channel: ReleaseChannel
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
  const label = version.prerelease
  if (
    (plan.channel === 'release'
      && (label !== undefined || version.sequence !== undefined))
    || (plan.channel === 'beta'
      && (label !== 'beta' || version.sequence !== undefined))
    || (plan.channel === 'alpha'
      && (label !== 'alpha' || version.sequence === undefined))
  ) {
    throw new Error(
      `Version ${plan.version} does not match the ${plan.channel} channel.`,
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
    throw new Error(`Release tag does not match version: ${plan.version}`)
  }
  if (!history) {
    throw new Error('Release history not found: call planRelease first.')
  }
  const releaseHistory = history
  const storedPlan: PlannedRelease = {
    ...plan,
    head: releaseHistory.head,
    channel: releaseHistory.channel,
  }
  validateReleaseVersion(storedPlan)
  saveJsonSync(PLAN_FILE, storedPlan)

  const commits = releaseHistory.commits.filter((commit) =>
    commit.breaking || (changeByCommitType[commit.type] ?? 'none') !== 'none'
  )
  const notes = releaseHistory.base.isPromotion && commits.length === 0
    ? `# ${plan.version}\n\nPromoted from ${
      releaseHistory.base.tag.slice(1)
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
    && typeof plan.channel === 'string'
    && RELEASE_CHANNELS.has(plan.channel)
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
    throw new Error(`Release tag does not match version: ${plan.version}`)
  }
  validateReleaseVersion(plan)
  await verifyPlannedHead(plan.head)
  return plan
}

export async function loadPublishableRelease(): Promise<PlannedRelease> {
  const plan = await loadPlannedRelease()
  const requested = requestedVersion()
  if (requested !== plan.version) {
    throw new Error(
      `Release plan targets ${plan.version}, but RELEASE_VERSION is ${
        requested ?? 'unset'
      }.`,
    )
  }
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
