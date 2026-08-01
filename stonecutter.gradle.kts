plugins {
    id("dev.kikugie.stonecutter")

    id("dev.isxander.modstitch.base") version "0.8.5" apply false
    id("me.modmuss50.mod-publish-plugin") version "2.2.0" apply false
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT" apply false
}

stonecutter active "26.2-fabric"

val checkArchitecture by tasks.registering {
    group = "verification"
    description = "Checks source package dependencies and Stonecutter macro boundaries."

    val sourceRoot = layout.projectDirectory.dir("src/main/java")
    val javaSources = fileTree(sourceRoot) { include("**/*.java") }
    inputs.files(javaSources)

    doLast {
        val basePackage = "io.github.leawind.perspectiveapi"
        val apiPackage = "$basePackage.api"
        val bridgePackage = "$basePackage.internal.bridge"
        val implPackage = "$basePackage.internal.impl"
        val logicPackage = "$basePackage.internal.logic"
        val platformPackage = "$basePackage.platform"

        val allowedApiInternalImports = setOf("$bridgePackage.Bridge")
        val forbiddenBridgeImports = listOf(apiPackage, implPackage, logicPackage)
        val forbiddenUtilsImports =
            listOf(apiPackage, bridgePackage, implPackage, logicPackage, platformPackage)
        val violations = mutableListOf<String>()

        fun importedType(line: String): String? {
            val declaration = line.trim().removePrefix("/*").trim()
            if (!declaration.startsWith("import ")) return null
            return declaration
                .removePrefix("import ")
                .removePrefix("static ")
                .substringBefore(';')
                .trim()
        }

        javaSources.files.sortedBy { it.path }.forEach { sourceFile ->
            val relativePath = sourceFile.relativeTo(sourceRoot.asFile).invariantSeparatorsPath
            val isApi = relativePath.startsWith("io/github/leawind/perspectiveapi/api/")
            val isBridge =
                relativePath.startsWith("io/github/leawind/perspectiveapi/internal/bridge/")
            val isLogic = relativePath.startsWith("io/github/leawind/perspectiveapi/internal/logic/")
            val isUtils = relativePath.startsWith("io/github/leawind/perspectiveapi/internal/utils/")
            val isMixin = relativePath.contains("/mixin/") || relativePath.endsWith("Mixin.java")

            sourceFile.readLines().forEachIndexed { index, line ->
                val location = "$relativePath:${index + 1}"
                if ((isApi || isLogic) && (line.contains("/*?") || line.contains("/^?"))) {
                    violations += "$location: Stonecutter macro is forbidden in api and logic"
                }

                val importedName = importedType(line) ?: return@forEachIndexed
                if (
                    isApi &&
                    importedName.startsWith("$basePackage.internal.") &&
                    allowedApiInternalImports.none {
                        importedName == it || importedName.startsWith("$it.")
                    }
                ) {
                    violations += "$location: api cannot import $importedName"
                }
                if (
                    (isBridge || isMixin) &&
                    forbiddenBridgeImports.any { importedName.startsWith("$it.") }
                ) {
                    violations += "$location: bridge and Mixins cannot import $importedName"
                }
                if (isUtils && forbiddenUtilsImports.any { importedName.startsWith("$it.") }) {
                    violations += "$location: utils cannot import $importedName"
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Architecture violations:\n" + violations.joinToString("\n") { "- $it" },
            )
        }
    }
}

val buildAndCollect by tasks.registering(Sync::class) {
    group = "build"
    description = "Builds and collects all distributable jars."
    dependsOn(checkArchitecture)
    into(layout.buildDirectory.dir("libs"))
}

allprojects {
    repositories {
        mavenCentral()

        // Sometimes it responds with 502 Bad Gateway.
        // https://github.com/Leawind/Perspective-API/actions/runs/29914253769/job/88907885668
        // maven("https://maven.terraformersmc.com/") // ModMenu
        exclusiveContent {
            forRepository {
                maven("https://maven.gnomecraft.net/releases") {
                    name = "GnomeCraft (Terraformers Mirror)"
                }
            }
            filter { includeGroup("com.terraformersmc") }
        }

        exclusiveContent {
            forRepository { maven("https://maven.isxander.dev/releases") }
            filter { includeGroup("dev.isxander") }
        }
        exclusiveContent {
            forRepository { maven("https://maven.quiltmc.org/repository/release") }
            filter { includeGroup("org.quiltmc.parsers") }
        }
        maven("https://maven.neoforged.net/releases/") {
            content {
                includeGroupByRegex("net\\.neoforged(\\..*)?")
            }
        }
        maven("https://maven.minecraftforge.net/") {
            content {
                includeGroupByRegex("net\\.minecraftforge(\\..*)?")
            }
        }
        exclusiveContent {
            forRepository { maven("https://maven.nucleoid.xyz") }
            filter { includeGroupByRegex("eu\\.pb4(\\..*)?") }
        }
        exclusiveContent {
            forRepository { maven("https://thedarkcolour.github.io/KotlinForForge/") }
            filter { includeGroup("thedarkcolour") }
        }
    }
}
