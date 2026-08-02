import {
  assertEquals,
  assertFalse,
  assertRejects,
  assertStringIncludes,
} from '@std/assert'

import { generatePerspectiveApiSkill } from '../gen-skills.ts'

Deno.test('generates a single-file skill from public non-internal Javadoc', async () => {
  const root = await Deno.makeTempDir()
  const apiDir = `${root}/api`
  const outputFile = `${root}/build/use-perspective-api.md`
  await Deno.mkdir(apiDir)

  try {
    await Deno.writeTextFile(
      `${apiDir}/package-info.java`,
      `/// Package workflow.
package example.api;
`,
    )
    await Deno.writeTextFile(
      `${apiDir}/Example.java`,
      `package example.api;

import org.jetbrains.annotations.ApiStatus;

/// Public record documentation.
public record Example(String value) {
  /// Public method documentation.
  public String visible() { return value; }

  String packagePrivate() { return value; }

  @ApiStatus.Internal
  public String internal() { return value; }

  /// Public nested type documentation.
  public static final class Builder {
    public Example build() { return new Example(""); }
  }
}
`,
    )
    await Deno.writeTextFile(
      `${apiDir}/Hidden.java`,
      `package example.api;

final class Hidden {
  public void visibleOnlyInsidePackage() {}
}
`,
    )

    const result = await generatePerspectiveApiSkill({ apiDir, outputFile })
    const content = await Deno.readTextFile(result)

    assertEquals(result, outputFile)
    assertStringIncludes(content, '---\nname: use-perspective-api\n')
    assertStringIncludes(content, 'Package workflow.')
    assertStringIncludes(content, '## record `example.api.Example`')
    assertStringIncludes(content, 'public record Example(String value)')
    assertStringIncludes(content, 'public String visible()')
    assertStringIncludes(content, '### class `example.api.Example.Builder`')
    assertStringIncludes(content, 'public Example build()')
    assertFalse(content.includes('public Example\n'))
    assertFalse(content.includes('packagePrivate'))
    assertFalse(content.includes('internal()'))
    assertFalse(content.includes('example.api.Hidden'))
  } finally {
    await Deno.remove(root, { recursive: true })
  }
})

Deno.test('rejects an API source tree without package Javadoc', async () => {
  const root = await Deno.makeTempDir()
  try {
    await Deno.writeTextFile(
      `${root}/Example.java`,
      'public class Example {}\n',
    )
    await assertRejects(
      () =>
        generatePerspectiveApiSkill({
          apiDir: root,
          outputFile: `${root}/out.md`,
        }),
      Error,
      'Missing package Javadoc source',
    )
  } finally {
    await Deno.remove(root, { recursive: true })
  }
})
