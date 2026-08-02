/**
 * Generates AI Agent skill from this project's public API Javadoc.
 *
 * The package Javadoc in `api/package-info.java` provides the dependency and integration
 * workflow. Public API types and members under the `api` package contribute their Javadoc as
 * reference material. Internal and package-private declarations are excluded.
 *
 * The generated Markdown file is written to `build/skills/use-perspective-api.md`, which is
 * intentionally ignored by Git. The GitHub Release publishing script invokes this generator and
 * uploads that uncompressed `.md` file as a release asset alongside the mod JARs.
 * @module gen-skills
 */
import { parse } from 'npm:java-parser@3.0.1'

export const DEFAULT_API_DIR =
  'src/main/java/io/github/leawind/perspectiveapi/api'
export const DEFAULT_OUTPUT_FILE = 'build/skills/use-perspective-api.md'

// deno-lint-ignore no-explicit-any
type N = any

type TypeKind = 'class' | 'interface' | 'record' | 'enum' | 'annotation'

interface TypeDoc {
  kind: TypeKind
  qualifiedName: string
  signature: string
  doc: string[]
  members: MemberDoc[]
  nested: TypeDoc[]
  enumConstants?: string
}

interface MemberDoc {
  signature: string
  doc: string[]
}

export interface GenerateSkillOptions {
  apiDir?: string
  outputFile?: string
}

async function walkJavaFiles(dir: string): Promise<string[]> {
  const out: string[] = []
  for await (const entry of Deno.readDir(dir)) {
    const path = `${dir}/${entry.name}`
    if (entry.isDirectory) {
      out.push(...await walkJavaFiles(path))
    } else if (entry.isFile && path.endsWith('.java')) {
      out.push(path)
    }
  }
  return out.sort()
}

function lineOf(src: string, offset: number): number {
  let line = 0
  for (let i = 0; i < offset && i < src.length; i++) {
    if (src[i] === '\n') { line++ }
  }
  return line
}

function docAbove(src: string, lineIndex: number): string[] {
  const lines = src.split('\n')
  const out: string[] = []
  let index = lineIndex - 1
  while (index >= 0) {
    const line = lines[index].trim()
    if (!line.startsWith('///')) { break }
    out.unshift(line.replace(/^\/\/\/ ?/, ''))
    index--
  }
  return out
}

function typeName(node: N): string | undefined {
  const children = node?.children
  if (!children) { return undefined }
  if (children.typeIdentifier) {
    return children.typeIdentifier[0].children?.Identifier?.[0]?.image
  }
  for (const value of Object.values(children)) {
    if (!Array.isArray(value)) { continue }
    for (const child of value as N[]) {
      if (child?.name === 'annotation') { continue }
      const result = typeName(child)
      if (result) { return result }
    }
  }
  return undefined
}

function hasToken(node: N, tokenName: string, depth = 0): boolean {
  if (!node?.children || depth > 3) { return false }
  if (node.children[tokenName]) { return true }
  for (const value of Object.values(node.children)) {
    if (!Array.isArray(value)) { continue }
    for (const child of value as N[]) {
      if (child?.name && hasToken(child, tokenName, depth + 1)) { return true }
    }
  }
  return false
}

function isExternallyAccessible(node: N, implicitPublic = false): boolean {
  return implicitPublic || hasToken(node, 'Public')
    || hasToken(node, 'Protected')
}

function cleanSignature(signature: string): string {
  return signature.trim().replace(/\s+/g, ' ')
}

const BODY_KEYS = [
  'methodBody',
  'constructorBody',
  'classBody',
  'interfaceBody',
  'recordBody',
  'enumBody',
  'annotationInterfaceBody',
]

function findBody(node: N, depth = 0): N | undefined {
  if (!node?.children || depth > 3) { return undefined }
  for (const key of BODY_KEYS) {
    const body = node.children[key]?.[0]
    if (body) { return body }
  }
  for (const value of Object.values(node.children)) {
    if (!Array.isArray(value)) { continue }
    for (const child of value as N[]) {
      if (!child?.name) { continue }
      const body = findBody(child, depth + 1)
      if (body) { return body }
    }
  }
  return undefined
}

function signatureOf(src: string, node: N): string {
  const start: number = node.location.startOffset
  const end: number = node.location.endOffset
  const body = findBody(node)
  if (body?.location?.startOffset != null) {
    return cleanSignature(src.slice(start, body.location.startOffset))
  }
  const text = src.slice(start, end + 1)
  const semicolon = text.indexOf(';')
  return cleanSignature(semicolon >= 0 ? text.slice(0, semicolon) : text)
}

function isInternal(src: string, node: N): boolean {
  return signatureOf(src, node).includes('@ApiStatus.Internal')
}

function extractPackage(compilationUnit: N): string {
  const declaration = compilationUnit.children?.packageDeclaration?.[0]
  if (!declaration) { return '' }
  return (declaration.children?.Identifier ?? [])
    .map((token: N) => token.image)
    .join('.')
}

function extractPackageDoc(compilationUnit: N, src: string): string[] {
  const declaration = compilationUnit.children?.packageDeclaration?.[0]
  if (!declaration) { return [] }
  return docAbove(src, lineOf(src, declaration.location.startOffset))
}

function declarationKind(node: N): { kind: TypeKind; declaration: N } | null {
  const children = node?.children
  if (!children) { return null }

  if (children.classDeclaration) {
    const declaration = children.classDeclaration[0]
    if (declaration.children?.recordDeclaration) {
      return { kind: 'record', declaration }
    }
    if (declaration.children?.enumDeclaration) {
      return { kind: 'enum', declaration }
    }
    return { kind: 'class', declaration }
  }
  if (children.interfaceDeclaration) {
    const declaration = children.interfaceDeclaration[0]
    if (declaration.children?.annotationInterfaceDeclaration) {
      return { kind: 'annotation', declaration }
    }
    return { kind: 'interface', declaration }
  }
  if (children.recordDeclaration) {
    return {
      kind: 'record',
      declaration: node,
    }
  }
  if (children.enumDeclaration) { return { kind: 'enum', declaration: node } }
  if (children.annotationInterfaceDeclaration) {
    return { kind: 'annotation', declaration: node }
  }
  if (children.normalClassDeclaration) {
    return { kind: 'class', declaration: node }
  }
  if (children.normalInterfaceDeclaration) {
    return { kind: 'interface', declaration: node }
  }
  return null
}

function extractType(
  node: N,
  src: string,
  packageName: string,
  implicitPublic = false,
): TypeDoc | null {
  const resolved = declarationKind(node)
  if (!resolved) { return null }
  const { kind, declaration } = resolved
  if (!isExternallyAccessible(declaration, implicitPublic)) { return null }
  if (isInternal(src, declaration)) { return null }

  const name = typeName(declaration)
  if (!name) { return null }
  const qualifiedName = packageName ? `${packageName}.${name}` : name
  const members: MemberDoc[] = []
  const nested: TypeDoc[] = []
  const body = findBody(declaration)
  let enumConstants: string | undefined

  if (body?.name === 'classBody') {
    extractClassBody(body, src, members, nested, qualifiedName, false)
  } else if (body?.name === 'recordBody') {
    extractRecordBody(body, src, members, nested, qualifiedName)
  } else if (body?.name === 'interfaceBody') {
    extractInterfaceBody(body, src, members, nested, qualifiedName)
  } else if (body?.name === 'enumBody') {
    enumConstants = extractEnumConstants(body)
    extractEnumBody(body, src, members, nested, qualifiedName)
  } else if (body?.name === 'annotationInterfaceBody') {
    extractAnnotationBody(body, src, members, nested, qualifiedName)
  }

  return {
    kind,
    qualifiedName,
    signature: signatureOf(src, declaration),
    doc: docAbove(src, lineOf(src, declaration.location.startOffset)),
    members,
    nested,
    enumConstants,
  }
}

function addClassBodyDeclaration(
  declaration: N,
  src: string,
  members: MemberDoc[],
  nested: TypeDoc[],
  qualifiedName: string,
  implicitPublic: boolean,
): void {
  const children = declaration.children
  if (!children) { return }

  const constructor = children.constructorDeclaration?.[0]
  if (
    constructor
    && isExternallyAccessible(constructor, implicitPublic)
    && !isInternal(src, constructor)
  ) {
    members.push(extractMember(constructor, src))
  }

  const member = children.classMemberDeclaration?.[0]
  if (!member) { return }
  const memberChildren = member.children ?? {}

  for (const key of ['methodDeclaration', 'fieldDeclaration']) {
    const candidate = memberChildren[key]?.[0]
    if (
      candidate
      && isExternallyAccessible(candidate, implicitPublic)
      && !isInternal(src, candidate)
    ) {
      members.push(extractMember(candidate, src))
    }
  }

  for (const key of ['classDeclaration', 'interfaceDeclaration']) {
    for (const candidate of memberChildren[key] ?? []) {
      const type = extractType(candidate, src, qualifiedName, implicitPublic)
      if (type) { nested.push(type) }
    }
  }
}

function extractClassBody(
  body: N,
  src: string,
  members: MemberDoc[],
  nested: TypeDoc[],
  qualifiedName: string,
  implicitPublic: boolean,
): void {
  for (const declaration of body.children?.classBodyDeclaration ?? []) {
    addClassBodyDeclaration(
      declaration,
      src,
      members,
      nested,
      qualifiedName,
      implicitPublic,
    )
  }
}

function extractRecordBody(
  body: N,
  src: string,
  members: MemberDoc[],
  nested: TypeDoc[],
  qualifiedName: string,
): void {
  for (const declaration of body.children?.recordBodyDeclaration ?? []) {
    // The record declaration already exposes its canonical constructor through the components.
    // A compact constructor has no standalone Java signature worth rendering.
    const classDeclaration = declaration.children?.classBodyDeclaration?.[0]
    if (classDeclaration) {
      addClassBodyDeclaration(
        classDeclaration,
        src,
        members,
        nested,
        qualifiedName,
        false,
      )
    }
  }
}

function extractInterfaceBody(
  body: N,
  src: string,
  members: MemberDoc[],
  nested: TypeDoc[],
  qualifiedName: string,
): void {
  for (const declaration of body.children?.interfaceMemberDeclaration ?? []) {
    const children = declaration.children ?? {}
    for (const key of ['interfaceMethodDeclaration', 'constantDeclaration']) {
      const candidate = children[key]?.[0]
      if (candidate && !isInternal(src, candidate)) {
        members.push(extractMember(candidate, src))
      }
    }
    for (const key of ['classDeclaration', 'interfaceDeclaration']) {
      for (const candidate of children[key] ?? []) {
        const type = extractType(candidate, src, qualifiedName, true)
        if (type) { nested.push(type) }
      }
    }
  }
}

function extractEnumBody(
  body: N,
  src: string,
  members: MemberDoc[],
  nested: TypeDoc[],
  qualifiedName: string,
): void {
  const declarations = body.children?.enumBodyDeclarations?.[0]
  if (!declarations) { return }
  extractClassBody(declarations, src, members, nested, qualifiedName, false)
}

function extractAnnotationBody(
  body: N,
  src: string,
  members: MemberDoc[],
  nested: TypeDoc[],
  qualifiedName: string,
): void {
  for (
    const declaration of body.children?.annotationInterfaceMemberDeclaration
      ?? []
  ) {
    const children = declaration.children ?? {}
    const element = children.annotationInterfaceElementDeclaration?.[0]
    if (element && !isInternal(src, element)) {
      members.push(extractMember(element, src))
    }
    for (const key of ['classDeclaration', 'interfaceDeclaration']) {
      for (const candidate of children[key] ?? []) {
        const type = extractType(candidate, src, qualifiedName, true)
        if (type) { nested.push(type) }
      }
    }
  }
}

function extractMember(node: N, src: string): MemberDoc {
  return {
    signature: signatureOf(src, node),
    doc: docAbove(src, lineOf(src, node.location.startOffset)),
  }
}

function extractEnumConstants(body: N): string {
  const list = body.children?.enumConstantList?.[0]
  if (!list) { return '' }
  return (list.children?.enumConstant ?? [])
    .map((constant: N) => constant.children?.Identifier?.[0]?.image)
    .filter(Boolean)
    .join(', ')
}

function compilationUnit(src: string): N {
  const parsed = parse(src)
  const unit = parsed.children?.ordinaryCompilationUnit?.[0]
    ?? parsed.children?.modularCompilationUnit?.[0]
  if (!unit) { throw new Error('Java source has no compilation unit') }
  return unit
}

function extractTypes(unit: N, src: string): TypeDoc[] {
  const packageName = extractPackage(unit)
  const out: TypeDoc[] = []
  for (const declaration of unit.children?.typeDeclaration ?? []) {
    const type = extractType(declaration, src, packageName)
    if (type) { out.push(type) }
  }
  return out
}

function joinInlineTags(lines: string[]): string[] {
  const out: string[] = []
  let pending = ''
  for (const line of lines) {
    if (pending) {
      pending += ` ${line.trim()}`
      if (pending.includes('}')) {
        out.push(pending)
        pending = ''
      }
    } else if (line.includes('{@') && !line.includes('}')) {
      pending = line
    } else {
      out.push(line)
    }
  }
  if (pending) { out.push(pending) }
  return out
}

function renderLink(content: string): string {
  const normalized = content.trim().replace(/,\s+/g, ',')
  const [target, ...label] = normalized.split(/\s+/)
  return `\`${label.length ? label.join(' ') : target}\``
}

function renderJavadoc(lines: string[], headingOffset = 0): string[] {
  return joinInlineTags(lines).map((line) => {
    let rendered = line
      .replace(/\{@code\s+([^}]+)\}/g, '`$1`')
      .replace(
        /\{@(?:link|linkplain)\s+([^}]+)\}/g,
        (_, content) => renderLink(content),
      )
    if (headingOffset > 0) {
      rendered = rendered.replace(
        /^(#{1,6})\s/,
        (_match, hashes: string) =>
          `${'#'.repeat(Math.min(6, hashes.length + headingOffset))} `,
      )
    }
    return rendered
  })
}

function renderType(type: TypeDoc, out: string[], depth: number): void {
  const heading = '#'.repeat(Math.min(depth + 2, 6))
  const label = type.kind === 'annotation' ? '@interface' : type.kind
  out.push(`${heading} ${label} \`${type.qualifiedName}\``, '')

  if (type.doc.length) { out.push(...renderJavadoc(type.doc, depth + 2), '') }

  const code: string[] = []
  if (type.kind === 'enum' && type.enumConstants) {
    code.push(`${type.signature} { ${type.enumConstants} }`)
  } else {
    code.push(type.signature)
  }
  for (const member of type.members) {
    if (member.doc.length) {
      code.push(
        ...renderJavadoc(member.doc).map((line) =>
          line ? `/// ${line}` : '///'
        ),
      )
    }
    code.push(member.signature)
  }

  out.push('```java', ...code, '```', '')
  for (const nested of type.nested) { renderType(nested, out, depth + 1) }
}

export async function generatePerspectiveApiSkill(
  options: GenerateSkillOptions = {},
): Promise<string> {
  const apiDir = options.apiDir ?? DEFAULT_API_DIR
  const outputFile = options.outputFile ?? DEFAULT_OUTPUT_FILE
  const files = await walkJavaFiles(apiDir)
  if (!files.length) { throw new Error(`No Java files found in ${apiDir}`) }

  const packageInfo = `${apiDir}/package-info.java`
  let packageDoc: string[]
  try {
    const source = await Deno.readTextFile(packageInfo)
    packageDoc = extractPackageDoc(compilationUnit(source), source)
  } catch (error) {
    if (error instanceof Deno.errors.NotFound) {
      throw new Error(`Missing package Javadoc source: ${packageInfo}`)
    }
    throw error
  }
  if (!packageDoc.length) {
    throw new Error(`Package Javadoc is empty: ${packageInfo}`)
  }

  const types: TypeDoc[] = []
  const errors: string[] = []
  for (const file of files) {
    if (file === packageInfo) { continue }
    try {
      const source = await Deno.readTextFile(file)
      types.push(...extractTypes(compilationUnit(source), source))
    } catch (error) {
      errors.push(`${file}: ${error instanceof Error ? error.message : error}`)
    }
  }
  if (errors.length) {
    throw new Error(`Failed to parse public API:\n${errors.join('\n')}`)
  }
  if (!types.length) {
    throw new Error(
      `No public API types found in ${apiDir}`,
    )
  }

  const out = [
    '---',
    'name: use-perspective-api',
    'description: Add Perspective API as a dependency to a Minecraft client mod and use its perspectives, modifiers, overrides, switchers, transitions, SPI providers, and runtime registrations. Use when implementing or maintaining a mod that integrates with Perspective API.',
    '---',
    '',
    '# Use Perspective API',
    '',
    ...renderJavadoc(packageDoc),
    '',
    '## Public API reference',
    '',
    '> Generated from public, non-internal API declarations and their Javadoc. Do not call APIs marked `@ApiStatus.Internal`.',
    '',
  ]
  for (const type of types) { renderType(type, out, 0) }

  const separator = outputFile.lastIndexOf('/')
  if (separator >= 0) {
    await Deno.mkdir(outputFile.slice(0, separator), { recursive: true })
  }
  await Deno.writeTextFile(outputFile, `${out.join('\n').trimEnd()}\n`)
  console.log(`Generated ${outputFile} (${types.length} public types)`)
  return outputFile
}

if (import.meta.main) { await generatePerspectiveApiSkill() }
