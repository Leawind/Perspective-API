/// Generates an AI Agent skill file from the public API source code.
///
/// Scans `src/main/java/io/github/leawind/perspectiveapi/api/` for public
/// types and members, extracts signatures and `///` Javadoc comments via
/// `npm:java-parser` CST analysis, and writes `build/perspective-api-usage.md`.
import { parse } from 'npm:java-parser'

const API_DIR = 'src/main/java/io/github/leawind/perspectiveapi/api'
const OUTPUT_FILE = 'build/perspective-api-usage.md'
const MANUAL_FILE = 'docs/usage-skill-manual.md'

// deno-lint-ignore no-explicit-any
type N = any

// region types

type TypeKind = 'class' | 'interface' | 'enum' | 'annotation'

interface TypeDoc {
  kind: TypeKind
  name: string
  qualifiedName: string
  signature: string
  doc: string[]
  members: MemberDoc[]
  nested: TypeDoc[]
  enumConstants?: string
}

interface MemberDoc {
  kind: 'method' | 'field'
  name: string
  signature: string
  doc: string[]
}

// endregion

// region file system

async function walkJavaFiles(dir: string): Promise<string[]> {
  const out: string[] = []
  for await (const e of Deno.readDir(dir)) {
    const p = `${dir}/${e.name}`
    if (e.isDirectory) {
      out.push(...await walkJavaFiles(p))
    } else if (e.isFile && p.endsWith('.java')) {
      out.push(p)
    }
  }
  return out.sort()
}

// endregion

// region source text helpers

function lineOf(src: string, off: number): number {
  let n = 0
  for (let i = 0; i < off && i < src.length; i++) {
    if (src[i] === '\n') { n++ }
  }
  return n
}

/// Collects consecutive `///` comment lines directly above `lineIdx`.
function docAbove(src: string, lineIdx: number): string[] {
  const lines = src.split('\n')
  const out: string[] = []
  let i = lineIdx - 1
  while (i >= 0) {
    const t = lines[i].trim()
    if (t.startsWith('///')) {
      out.unshift(t.replace(/^\/\/\/ ?/, ''))
      i--
    } else { break }
  }
  return out
}

/// Gets the type name from a declaration node via `typeIdentifier`.
function typeName(decl: N): string | undefined {
  const c = decl?.children
  if (!c) { return undefined }
  if (c.typeIdentifier) {
    return c.typeIdentifier[0].children?.Identifier?.[0]?.image
  }
  for (const v of Object.values(c)) {
    if (!Array.isArray(v)) { continue }
    for (const ch of v as N[]) {
      if (ch?.name && ch.name !== 'annotation') {
        const r = typeName(ch)
        if (r) { return r }
      }
    }
  }
  return undefined
}

/// Gets the method name from a method declaration node.
function methodName(node: N): string {
  const md = node.children?.methodHeader?.[0]?.children
    ?.methodDeclarator?.[0]
  return md?.children?.Identifier?.[0]?.image ?? '?'
}

/// Gets the field name from a field/constant declaration node.
function fieldName(node: N): string {
  const vdl = node.children?.variableDeclaratorList?.[0]
  const vd = vdl?.children?.variableDeclarator?.[0]
  const vdi = vd?.children?.variableDeclaratorId?.[0]
  return vdi?.children?.Identifier?.[0]?.image ?? '?'
}

function hasPrivate(node: N): boolean {
  const c = node?.children
  if (!c) { return false }
  // Check direct modifier children
  for (const [k, v] of Object.entries(c)) {
    if (!k.endsWith('Modifier') || !Array.isArray(v)) { continue }
    for (const m of v as N[]) {
      if (m?.children?.Private) { return true }
    }
  }
  // Check one level deep (e.g., fieldDeclaration inside classMemberDeclaration)
  for (const v of Object.values(c)) {
    if (!Array.isArray(v)) { continue }
    for (const ch of v as N[]) {
      if (!ch?.children) { continue }
      for (const [k2, v2] of Object.entries(ch.children)) {
        if (!k2.endsWith('Modifier') || !Array.isArray(v2)) { continue }
        for (const m of v2 as N[]) {
          if ((m as N)?.children?.Private) { return true }
        }
      }
    }
  }
  return false
}

function cleanSig(s: string): string {
  return s.trim().replace(/\s+/g, ' ')
}

const BODY_KEYS = [
  'methodBody',
  'constructorBody',
  'classBody',
  'interfaceBody',
  'enumBody',
  'annotationInterfaceBody',
]

/// Finds the body node start offset, searching one level deep.
function findBodyStart(c: Record<string, N[]>): number {
  for (const k of BODY_KEYS) {
    const b = c[k]?.[0]
    if (b?.location?.startOffset != null) { return b.location.startOffset }
  }
  for (const v of Object.values(c)) {
    if (!Array.isArray(v)) { continue }
    for (const ch of v as N[]) {
      if (!ch?.children) { continue }
      for (const k of BODY_KEYS) {
        const b = ch.children[k]?.[0]
        if (b?.location?.startOffset != null) {
          return b.location.startOffset
        }
      }
    }
  }
  return -1
}

/// Extracts the declaration signature (without body) from source text.
function sigOf(src: string, node: N): string {
  const s: number = node.location.startOffset
  const e: number = node.location.endOffset
  const text = src.slice(s, e + 1)

  const bodyOff = findBodyStart(node.children ?? {})
  if (bodyOff >= 0) {
    const bodyText = src.slice(bodyOff, e + 1).trimStart()
    if (bodyText.startsWith('{')) {
      return cleanSig(src.slice(s, bodyOff))
    }
  }

  // No block body: end at first semicolon
  const si = text.indexOf(';')
  return cleanSig(si >= 0 ? text.slice(0, si) : text)
}

// endregion

// region CST extraction

function extractPackage(node: N): string {
  const pd = node?.children?.packageDeclaration?.[0]
  if (!pd) { return '' }
  return (pd.children?.Identifier ?? []).map((t: N) => t.image).join('.')
}

function extractTypes(node: N, src: string): TypeDoc[] {
  const c = node?.children
  if (!c) { return [] }
  const pkg = extractPackage(node)
  const out: TypeDoc[] = []

  for (const td of c.typeDeclaration ?? []) {
    const t = extractType(td, src, pkg)
    if (t) { out.push(t) }
  }
  return out
}

function extractType(node: N, src: string, pkg = ''): TypeDoc | null {
  const c = node.children
  if (!c) { return null }

  let kind: TypeKind | null = null
  let decl: N = null

  // Wrapper format: typeDeclaration or interfaceMemberDeclaration
  if (c.classDeclaration) {
    decl = c.classDeclaration[0]
    const dc = decl.children
    if (dc?.enumDeclaration) { kind = 'enum' }
    else { kind = 'class' }
  } else if (c.interfaceDeclaration) {
    decl = c.interfaceDeclaration[0]
    const dc = decl.children
    if (dc?.annotationInterfaceDeclaration) { kind = 'annotation' }
    else { kind = 'interface' }
  } else if (c.enumDeclaration) {
    // Direct classDeclaration node containing enumDeclaration
    kind = 'enum'
    decl = node
  } else if (c.annotationInterfaceDeclaration) {
    kind = 'annotation'
    decl = node
  } else if (c.normalClassDeclaration) {
    kind = 'class'
    decl = node
  } else if (c.normalInterfaceDeclaration) {
    kind = 'interface'
    decl = node
  }

  if (!kind || !decl) { return null }

  const name = typeName(decl)
  if (!name) { return null }
  if (hasPrivate(decl)) { return null }

  const qualifiedName = pkg ? `${pkg}.${name}` : name

  const signature = sigOf(src, decl)
  const doc = docAbove(src, lineOf(src, decl.location.startOffset))
  const members: MemberDoc[] = []
  const nested: TypeDoc[] = []
  let enumConstants: string | undefined

  // Body is inside normalXDeclaration wrapper, search one level
  let bodyNode: N | undefined
  let bodyKind = ''
  for (const v of Object.values(decl.children ?? {})) {
    if (!Array.isArray(v)) { continue }
    for (const ch of v as N[]) {
      if (!ch?.children) { continue }
      for (const bk of BODY_KEYS) {
        if (ch.children[bk]) {
          bodyNode = ch.children[bk][0]
          bodyKind = bk
          break
        }
      }
      if (bodyNode) { break }
    }
    if (bodyNode) { break }
  }

  if (bodyNode) {
    const nestedPkg = qualifiedName
    if (bodyKind === 'classBody') {
      extractClassBody(bodyNode, src, members, nested, nestedPkg)
    } else if (bodyKind === 'interfaceBody') {
      extractInterfaceBody(bodyNode, src, members, nested, nestedPkg)
    } else if (bodyKind === 'enumBody') {
      enumConstants = extractEnumConstants(bodyNode, src)
      extractEnumBody(bodyNode, src, members, nested, nestedPkg)
    } else if (bodyKind === 'annotationInterfaceBody') {
      extractAnnotationBody(bodyNode, src, members, nested, nestedPkg)
    }
  }

  return {
    kind,
    name,
    qualifiedName,
    signature,
    doc,
    members,
    nested,
    enumConstants,
  }
}

function extractClassBody(
  body: N,
  src: string,
  members: MemberDoc[],
  nested: TypeDoc[],
  nestedPkg: string,
): void {
  for (const bd of body.children?.classBodyDeclaration ?? []) {
    const c = bd.children
    if (!c) { continue }
    if (c.constructorDeclaration) { continue }
    const cmd = c.classMemberDeclaration?.[0]
    if (!cmd) { continue }
    if (hasPrivate(cmd)) { continue }
    const mc = cmd.children
    if (!mc) { continue }

    if (mc.methodDeclaration) {
      members.push(extractMethod(mc.methodDeclaration[0], src))
    } else if (mc.fieldDeclaration) {
      members.push(extractField(mc.fieldDeclaration[0], src))
    }
    for (
      const k of ['classDeclaration', 'interfaceDeclaration']
    ) {
      for (const n of mc[k] ?? []) {
        const t = extractType(n, src, nestedPkg)
        if (t) { nested.push(t) }
      }
    }
  }
}

function extractInterfaceBody(
  body: N,
  src: string,
  members: MemberDoc[],
  nested: TypeDoc[],
  nestedPkg: string,
): void {
  for (const md of body.children?.interfaceMemberDeclaration ?? []) {
    const c = md.children
    if (!c) { continue }
    if (hasPrivate(md)) { continue }

    if (c.interfaceMethodDeclaration) {
      members.push(
        extractMethod(c.interfaceMethodDeclaration[0], src),
      )
    } else if (c.constantDeclaration) {
      members.push(extractField(c.constantDeclaration[0], src))
    }
    for (
      const k of ['classDeclaration', 'interfaceDeclaration']
    ) {
      for (const n of c[k] ?? []) {
        const t = extractType(n, src, nestedPkg)
        if (t) { nested.push(t) }
      }
    }
  }
}

function extractEnumBody(
  body: N,
  src: string,
  members: MemberDoc[],
  nested: TypeDoc[],
  nestedPkg: string,
): void {
  const ebd = body.children?.enumBodyDeclarations?.[0]
  if (!ebd) { return }
  for (const bd of ebd.children?.classBodyDeclaration ?? []) {
    const c = bd.children
    if (!c) { continue }
    if (c.constructorDeclaration) { continue }
    const cmd = c.classMemberDeclaration?.[0]
    if (!cmd) { continue }
    if (hasPrivate(cmd)) { continue }
    const mc = cmd.children
    if (!mc) { continue }

    if (mc.methodDeclaration) {
      members.push(extractMethod(mc.methodDeclaration[0], src))
    } else if (mc.fieldDeclaration) {
      members.push(extractField(mc.fieldDeclaration[0], src))
    }
    for (
      const k of ['classDeclaration', 'interfaceDeclaration']
    ) {
      for (const n of mc[k] ?? []) {
        const t = extractType(n, src, nestedPkg)
        if (t) { nested.push(t) }
      }
    }
  }
}

function extractAnnotationBody(
  body: N,
  src: string,
  members: MemberDoc[],
  nested: TypeDoc[],
  nestedPkg: string,
): void {
  for (
    const md of body.children?.annotationInterfaceMemberDeclaration
      ?? []
  ) {
    const c = md.children
    if (!c) { continue }
    if (hasPrivate(md)) { continue }

    if (c.annotationInterfaceElementDeclaration) {
      members.push(
        extractMethod(
          c.annotationInterfaceElementDeclaration[0],
          src,
        ),
      )
    }
    for (
      const k of ['classDeclaration', 'interfaceDeclaration']
    ) {
      for (const n of c[k] ?? []) {
        const t = extractType(n, src, nestedPkg)
        if (t) { nested.push(t) }
      }
    }
  }
}

function extractMethod(node: N, src: string): MemberDoc {
  return {
    kind: 'method',
    name: methodName(node),
    signature: sigOf(src, node),
    doc: docAbove(src, lineOf(src, node.location.startOffset)),
  }
}

function extractField(node: N, src: string): MemberDoc {
  return {
    kind: 'field',
    name: fieldName(node),
    signature: sigOf(src, node),
    doc: docAbove(src, lineOf(src, node.location.startOffset)),
  }
}

function extractEnumConstants(body: N, src: string): string {
  const ecl = body.children?.enumConstantList?.[0]
  if (!ecl) { return '' }
  const names: string[] = []
  for (const ec of ecl.children?.enumConstant ?? []) {
    const n = ec.children?.Identifier?.[0]?.image
    if (n) { names.push(n) }
  }
  return names.join(', ')
}

// endregion

// region markdown generation

function renderType(t: TypeDoc, out: string[], depth: number): void {
  const h = '#'.repeat(Math.min(depth + 2, 6))
  const label = t.kind === 'annotation' ? '@interface' : t.kind
  out.push(`${h} ${label} \`${t.qualifiedName}\``)
  out.push('')

  if (t.doc.length) {
    out.push(...t.doc)
    out.push('')
  }

  // Collect type declaration + all members into one code block
  const codeLines: string[] = []
  if (t.kind === 'enum' && t.enumConstants) {
    codeLines.push(`${t.signature} { ${t.enumConstants} }`)
  } else {
    codeLines.push(t.signature)
  }
  for (const m of t.members) {
    // Include inline doc as /// comments
    if (m.doc.length) {
      codeLines.push(...m.doc.map((l) => (l ? `/// ${l}` : '///')))
    }
    codeLines.push(m.signature)
  }

  const codeContent = codeLines.join('\n')
  const fence = codeContent.includes('```') ? '````' : '```'
  out.push(`${fence}java`)
  out.push(codeContent)
  out.push(fence)
  out.push('')

  for (const n of t.nested) { renderType(n, out, depth + 1) }
}

// endregion

// region main

function processFile(path: string, src: string): TypeDoc[] {
  const cst = parse(src)
  const ocu = cst.children?.ordinaryCompilationUnit?.[0]
    ?? cst.children?.modularCompilationUnit?.[0]
  if (!ocu) { return [] }
  return extractTypes(ocu, src)
}

const files = await walkJavaFiles(API_DIR)
if (!files.length) {
  console.error(`Error: no .java files found in ${API_DIR}`)
  Deno.exit(1)
}

const allTypes: TypeDoc[] = []
const errors: string[] = []

for (const f of files) {
  try {
    allTypes.push(...processFile(f, await Deno.readTextFile(f)))
  } catch (e) {
    errors.push(`${f}: ${e instanceof Error ? e.message : e}`)
  }
}

if (errors.length) {
  console.error('Failed to parse:')
  for (const e of errors) { console.error(`  ${e}`) }
  Deno.exit(1)
}

const out: string[] = [
  '# Perspective API Usage',
  '',
  '> Auto-generated from public API source. Do not edit manually.',
  '',
]

// Merge manual doc at the beginning
try {
  const manual = await Deno.readTextFile(MANUAL_FILE)
  out.push(manual.trim(), '')
  console.log(`Merged manual doc: ${MANUAL_FILE}`)
} catch {
  // docs/llms-manual.md not found, skip merge
}

for (const t of allTypes) { renderType(t, out, 0) }

const content = out.join('\n')

await Deno.mkdir('build', { recursive: true })
await Deno.writeTextFile(OUTPUT_FILE, content)
console.log(
  `Generated ${OUTPUT_FILE} (${allTypes.length} types from ${files.length} files)`,
)

// endregion
