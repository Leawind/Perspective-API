plugins {
    id("dev.kikugie.stonecutter")
    id("dev.isxander.modstitch.base")
    `maven-publish`
    id("me.modmuss50.mod-publish-plugin")
}

// ========== Versions & Project Info ==========
val mcVersion: String by project
val modVersionString = property("mod.version")!!.toString()

val isFabric = modstitch.isLoom
val isNeoforge = modstitch.isModDevGradleRegular
val isForge = modstitch.isModDevGradleLegacy
val loader = when {
    isFabric -> "fabric"
    isNeoforge -> "neoforge"
    isForge -> "forge"
    else -> error("Unknown loader")
}

// Determine if unit testing is supported for this platform/version
// - Fabric: all versions
// - NeoForge: >= 1.20.5 (JUnit run type not available in older versions)
// - Forge: not supported
val supportsUnitTesting = isFabric || (isNeoforge && stonecutter.current.parsed >= "1.20.5")

// ========== ModStitch Setup ==========
modstitch {
    minecraftVersion = mcVersion

    loom {
        prop("deps.fabricLoader") { fabricLoaderVersion = it }
    }

    moddevgradle {
        prop("deps.neoforge") { neoForgeVersion = it }
        prop("deps.forge") { forgeVersion = it }
    }

    metadata {
        modId = "perspective_api"
        modName = "Perspective API"
        modVersion = "$modVersionString+$loader-$mcVersion"
        modGroup = "io.github.leawind.perspectiveapi"
        modDescription = "A client-side API framework for managing and extending camera perspectives."
        modLicense = "MIT"
        modAuthor = "Leawind"

        replacementProperties.put("github", "Leawind/Perspective-API")
        replacementProperties.put("mc", findProperty("meta.mcDep")?.toString() ?: "*")
        replacementProperties.put("loaderVersion", findProperty("meta.loaderDep")?.toString() ?: "*")
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

// ========== Stonecutter ==========
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

// ========== Dependencies ==========
// Force specific log4j version to avoid dynamic version resolution issues in offline mode
// (transitive dependency from net.minecraftforge:unsafe uses 2.11.+)
// Use 2.24.3 which is compatible with both NeoForge and SLF4J bridge
configurations.all {
    resolutionStrategy {
        force("org.apache.logging.log4j:log4j-api:2.24.3")
        force("org.apache.logging.log4j:log4j-core:2.24.3")
    }
}

dependencies {
    if (isFabric) {
        prop("deps.fabricApi") { modstitchModImplementation("net.fabricmc.fabric-api:fabric-api:$it") }
        prop("mod.modmenu_version") { modstitchModImplementation("com.terraformersmc:modmenu:$it") }
    }

    prop("mod.yacl_version") { modstitchModImplementation("dev.isxander:yet-another-config-lib:$it-$loader") }

    // Compile only
    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("org.jetbrains:annotations:24.0.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")

    // Test
    testCompileOnly("org.jspecify:jspecify:1.0.0")
    // Note: fabric-loader-junit is added by modstitch.unitTesting() for Fabric
    if (!isFabric) {
        testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    }
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.google.jimfs:jimfs:1.3.0") {
        exclude(group = "com.google.guava", module = "guava")
    }
}

// ========== Tasks ==========
tasks.test {
    useJUnitPlatform()
    // Disable tests for unsupported platforms
    if (!supportsUnitTesting) {
        enabled = false
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

// Allow duplicate entries in jar (e.g. refmap from both AP and resources)
tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

// Exclude default refmap for Forge (AP generates it, so resources version would duplicate)
if (isForge) {
    tasks.named<ProcessResources>("processResources") {
        exclude("perspective_api.refmap.json")
    }
}

// ========== Publishing ==========
val buildAndCollect by tasks.registering(Copy::class) {
    group = "build"
    dependsOn(modstitch.finalJarTask, tasks.named("sourcesJar"))
    from(modstitch.finalJarTask.flatMap { it.archiveFile })
    from(tasks.named("sourcesJar").flatMap { (it as org.gradle.jvm.tasks.Jar).archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs"))
}

// read changelog
val changelogFile = rootProject.file("CHANGELOG.md")
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
            projectId = System.getenv("MODRINTH_ID")
            minecraftVersions.add(mcVersion)
            if (isFabric) {
                optional { slug.set("modmenu") }
            }
            optional { slug.set("yacl") }
        }
        curseforge {
            projectId = System.getenv("CURSEFORGE_ID")
            minecraftVersions.add(mcVersion)
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
            artifactId = "perspective_api"
            version = "$modVersionString+$loader-$mcVersion"
            from(components["java"])
            pom {
                name.set("Perspective API")
                description.set("A client-side API framework for managing and extending camera perspectives.")
            }
        }
    }

    repositories {
        mavenLocal()
    }
}

// ========== Helpers ==========
fun <T> prop(property: String, block: (String) -> T?): T? {
    return findProperty(property)?.toString()?.takeIf { it.isNotBlank() }?.let(block)
}
