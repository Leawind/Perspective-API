plugins {
    id("dev.kikugie.stonecutter")
    id("dev.isxander.modstitch.base")
    `maven-publish`
    id("me.modmuss50.mod-publish-plugin")
}

// region Versions & Project Info
val mcVersion: String by project
val modGroupValue = requiredProp("mod.group")
val modIdValue = requiredProp("mod.id")
val modVersionString = requiredProp("mod.version")
val modNameValue = requiredProp("mod.name")
val modDescriptionValue = requiredProp("mod.description")
val modAuthorValue = requiredProp("mod.author")
val modLicenseValue = requiredProp("mod.license")
val modLogoFile = requiredProp("mod.logo_file")
val modHomeUrl = requiredProp("mod.home_url")
val modSourceUrl = requiredProp("mod.source_url")
val modIssuesUrl = requiredProp("mod.issues_url")
val modEmail = requiredProp("mod.email")

val isFabric = modstitch.isLoom
val isNeoforge = modstitch.isModDevGradleRegular
val isForge = modstitch.isModDevGradleLegacy
val loader = when {
    isFabric -> "fabric"
    isNeoforge -> "neoforge"
    isForge -> "forge"
    else -> error("Unknown loader")
}
// Extra version labels are declared by each version variant for publishing platforms.
val publishedMinecraftVersions = buildList {
    add(mcVersion)
    findProperty("publish.additionalMcVersions")
        ?.toString()
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.forEach { add(it) }
}.distinct()

// Determine if unit testing is supported for this platform/version
// - Fabric: all versions
// - NeoForge: >= 1.20.5 (JUnit run type not available in older versions)
// - Forge: not supported
val supportsUnitTesting = isFabric || (isNeoforge && stonecutter.current.parsed >= "1.20.5")
// endregion

modstitch {
    minecraftVersion = mcVersion

    loom {
        if (isFabric) {
            fabricLoaderVersion = requiredProp("deps.fabricLoader")
            configureLoom {
                runs.named("client") {
                    runDir = project.relativePath(rootProject.file("run"))
                }
            }
        }
    }

    if (!isFabric) {
        runs {
            register("client") {
                client()
                gameDirectory.set(rootProject.layout.projectDirectory.dir("run"))
            }
        }
    }

    moddevgradle {
        if (isNeoforge) {
            neoForgeVersion = requiredProp("deps.neoforge")
        }
        if (isForge) {
            forgeVersion = requiredProp("deps.forge")
        }
    }

    metadata {
        modId = modIdValue
        modName = modNameValue
        modVersion = "$modVersionString+$loader-$mcVersion"
        modGroup = modGroupValue
        modDescription = modDescriptionValue
        modLicense = modLicenseValue
        modAuthor = modAuthorValue

        replacementProperties.put("logo_file", modLogoFile)
        replacementProperties.put("home_url", modHomeUrl)
        replacementProperties.put("source_url", modSourceUrl)
        replacementProperties.put("issues_url", modIssuesUrl)
        replacementProperties.put("email", modEmail)
        replacementProperties.put("mc", requiredProp("meta.mcDep"))
        if (isNeoforge) {
            replacementProperties.put("loaderVersion", requiredProp("meta.loaderDep"))
        }
    }

    mixin {
        addMixinsToModManifest = true
        configs.register("perspective_api")
        if (isFabric) configs.register("perspective_api.fabric")
        if (isForge) configs.register("perspective_api.forge")
        if (isNeoforge) configs.register("perspective_api.neoforge")
    }

    // Enable unit testing for supported platforms
    if (supportsUnitTesting) {
        unitTesting()
    }
}

stonecutter {
    constants {
        put("fabric", isFabric)
        put("neoforge", isNeoforge)
        put("forge", isForge)
    }

    replacements.string(current.parsed >= "1.21.11") {
        replace("net.minecraft.resources.ResourceLocation", "net.minecraft.resources.Identifier")
        replace("ResourceLocation", "Identifier")
    }

    replacements.string(current.parsed >= "26.1") {
        replace("net.minecraft.client.gui.GuiGraphics", "net.minecraft.client.gui.GuiGraphicsExtractor")
        replace("GuiGraphics", "GuiGraphicsExtractor")
    }
}

// region Dependencies

// The transitive `net.minecraftforge:unsafe` dependency uses the dynamic version `2.11.+`.
if (isForge || (isNeoforge && stonecutter.current.parsed < "1.21")) {
    configurations.configureEach {
        resolutionStrategy {
            force("org.apache.logging.log4j:log4j-api:2.24.3")
            force("org.apache.logging.log4j:log4j-core:2.24.3")
        }
    }
}

dependencies {
    if (isFabric) {
        modstitchModImplementation(
            "net.fabricmc.fabric-api:fabric-api:${requiredProp("deps.fabricApi")}",
        )
        modstitchModImplementation(
            "com.terraformersmc:modmenu:${requiredProp("mod.modmenu_version")}",
        )
    }

    modstitchModImplementation(
        "dev.isxander:yet-another-config-lib:${requiredProp("mod.yacl_version")}-$loader",
    )

    // Compile only
    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("org.jetbrains:annotations:24.0.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")

    // Test
    testCompileOnly("org.jspecify:jspecify:1.0.0")
    // Note: fabric-loader-junit is added by modstitch.unitTesting() for Fabric
    if (!isFabric) {
        testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    }
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.3")
    testImplementation("com.google.jimfs:jimfs:1.3.0") {
        exclude(group = "com.google.guava", module = "guava")
    }
}
// endregion

// region Tasks
tasks.test {
    useJUnitPlatform()
    // Disable tests for unsupported platforms
    if (!supportsUnitTesting) {
        enabled = false
    }
}

tasks.withType<JavaExec>().configureEach {
    if (name == "runClient") {
        workingDir(rootProject.layout.projectDirectory.dir("run"))
    }
}

// Skip test compilation for unsupported platforms
if (!supportsUnitTesting) {
    tasks.compileTestJava {
        enabled = false
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

java {
    withSourcesJar()
}

// Exclude default refmap for Forge (AP generates it, so resources version would duplicate)
if (isForge) {
    tasks.named<ProcessResources>("processResources") {
        exclude("perspective_api.refmap.json")
    }
}
// endregion

// region Publishing
val sourcesJar = tasks.named<Jar>("sourcesJar")
rootProject.tasks.named<Sync>("buildAndCollect") {
    dependsOn(modstitch.finalJarTask, sourcesJar)
    from(modstitch.finalJarTask.flatMap { it.archiveFile })
    from(sourcesJar.flatMap { it.archiveFile })
}

// Read the notes for this release when available, otherwise fall back to the changelog.
val releaseNotesPath = System.getenv("RELEASE_NOTES_FILE") ?: "CHANGELOG.md"
val changelogFile = rootProject.file(releaseNotesPath)
val changelogText = if (changelogFile.exists()) changelogFile.readText() else ""

afterEvaluate {
    publishMods {
        // Always dry run until environment variable `DRY_RUN` is set to `false`
        dryRun.set(System.getenv("DRY_RUN") != "false")
        displayName.set("$modVersionString for $mcVersion $loader")
        file = modstitch.finalJarTask.flatMap { it.archiveFile }
        additionalFiles.from(tasks.named("sourcesJar"))
        changelog.set(changelogText)

        type = if (modVersionString.contains("beta", true)) {
            BETA
        } else if (modVersionString.contains("alpha", true)) {
            ALPHA
        } else {
            STABLE
        }

        modLoaders.add(loader)
        modrinth {
            accessToken = System.getenv("MODRINTH_TOKEN")
            projectId = System.getenv("MODRINTH_ID")
            minecraftVersions.addAll(publishedMinecraftVersions)
            if (isFabric) {
                optional { slug.set("modmenu") }
            }
            optional { slug.set("yacl") }
        }
        curseforge {
            accessToken = System.getenv("CURSEFORGE_TOKEN")
            projectId = System.getenv("CURSEFORGE_ID")
            minecraftVersions.addAll(publishedMinecraftVersions)
            clientRequired = true
            serverRequired = false
            if (isFabric) {
                optional { slug.set("modmenu") }
            }
            optional { slug.set("yacl") }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = modIdValue
            version = "$modVersionString+$loader-$mcVersion"
            from(components["java"])
            pom {
                name.set(modNameValue)
                description.set(modDescriptionValue)
            }
        }
    }

    repositories {
        mavenLocal()
    }
}
// endregion

// region Helpers
fun requiredProp(property: String): String =
    findProperty(property)?.toString()?.takeIf { it.isNotBlank() }
        ?: error("Required Gradle property '$property' is missing or blank")
// endregion
