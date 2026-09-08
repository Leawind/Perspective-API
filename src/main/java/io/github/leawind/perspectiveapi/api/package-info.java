/// Integrate Perspective API into another client-side Minecraft mod.
///
/// ## API stability
///
/// Members marked `@ApiStatus.Experimental` may break without notice and without a changelog
/// entry. All other public API is relatively stable: breaking changes are still possible, but
/// they are declared as `BREAKING CHANGE` and reflected in the version number.
///
/// Consult the public API reference below before integrating. To find a compatible dependency,
/// query Modrinth with the target loader and Minecraft version:
///
/// ```sh
/// curl -sS 'https://api.modrinth.com/v2/project/perspective-api/version?loaders=%5B%22fabric%22%5D&game_versions=%5B%2226.2%22%5D&include_changelog=false'
/// ```
///
/// Replace `fabric` and `26.2` with the target values. Choose the newest `listed` result by
/// `date_published` and use its `version_number` verbatim. Do not use GitHub's `releases/latest`:
/// it omits prereleases.
///
/// Resolve the selected version from Modrinth Maven using the consuming build's normal mod
/// dependency configuration:
///
/// ```kotlin
/// repositories {
///   exclusiveContent {
///     forRepository { maven { url = uri("https://api.modrinth.com/maven") } }
///     filter { includeGroup("maven.modrinth") }
///   }
/// }
///
/// dependencies {
///   implementation("maven.modrinth:LIqveQm1:<version_number>")
/// }
/// ```
///
/// Declare `perspective_api` as a required client-side dependency in the mod metadata.
package io.github.leawind.perspectiveapi.api;
