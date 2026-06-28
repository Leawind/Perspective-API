import gg.meza.stonecraft.mod
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.util.internal.VersionNumber

plugins {
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.10"
    id("gg.meza.stonecraft")
}

val props: Map<String, Any> = project.properties.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()

val archivesBaseName = mod.id
val archivesVersion = "${mod.version}-mc${mod.minecraftVersion}-${mod.loader}"

tasks.withType<Jar>().configureEach {
    archiveBaseName.set(archivesBaseName)
    archiveVersion.set(archivesVersion)
}

modSettings {
    // https://stonecraft.meza.gg/docs/configuration

    clientOptions {
        // https://minecraft.wiki/w/Options.txt
        fov = 88
        narrator = false
        musicVolume = 0.0
        guiScale = 3

        additionalLines = mapOf(
            "maxFps" to "60",
            "renderDistance" to "8",
            "simulationDistance" to "5",
            "mouseSensitivity" to "0.22",
            "key_key.togglePerspective" to "key.keyboard.v",
        )
    }

    val vars = props
        .filterKeys { it.startsWith("mod.") }
        .mapKeys { it.key.removePrefix("mod.") }
    variableReplacements.putAll(vars)
}

stonecutter {
    replacements.string(current.parsed >= "1.21.11") {
        replace(
            "net.minecraft.resources.ResourceLocation",
            "net.minecraft.resources.Identifier"
        )
        replace("ResourceLocation", "Identifier")
    }
}


repositories {
    mavenCentral()
    maven("https://jitpack.io")
    // Mod Menu (Fabric)
    maven {
        name = "Terraformers"
        url = uri("https://maven.terraformersmc.com/")
    }
    // KotlinForForge (required by YACL on NeoForge)
    maven {
        name = "Kotlin for Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
        content { includeGroup("thedarkcolour") }
    }
    // NeoForged
    maven {
        name = "NeoForged"
        url = uri("https://maven.neoforged.net/releases")
    }
    // Text Placeholder API
    maven {
        name = "Nucleoid"
        url = uri("https://maven.nucleoid.xyz")
    }
}

val shadowBundle: Configuration by configurations.creating
fun DependencyHandlerScope.shadowBundle(dependencyNotation: String) {
    if (mod.isForge) {
        add("forgeRuntimeLibrary", dependencyNotation)
    }
    implementation(dependencyNotation)
    add("shadowBundle", dependencyNotation)
}

fun DependencyHandlerScope.modImplAlias(dependencyNotation: String) {
    if (VersionNumber.parse(mod.minecraftVersion) >= VersionNumber.parse("26.1")) {
        implementation(dependencyNotation)
    } else {
        add("modImplementation", dependencyNotation)
    }
}

dependencies {
    if (mod.isForge) {
        if (VersionNumber.parse(mod.minecraftVersion) <= VersionNumber.parse("1.20.1")) {
            val mixinextrasDep = "io.github.llamalad7:mixinextras-common:0.4.1"
            compileOnly(mixinextrasDep)
            annotationProcessor(mixinextrasDep)
        }
    }

    if (mod.isFabric) {
        // ModMenu (Fabric only)
        // https://modrinth.com/mod/modmenu/versions
        modImplAlias("com.terraformersmc:modmenu:${project.property("mod.modmenu_version")}")
    }

    // region test
    testCompileOnly("org.jspecify:jspecify:1.0.0")

    if (mod.isFabric) {
        testImplementation("net.fabricmc:fabric-loader-junit:${props["loader_version"]}")
    } else {
        testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    }
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")


    testImplementation("com.google.jimfs:jimfs:1.3.0") {
        // conflict with 1.20.1-forge `guava:32.1.1-jre`
        exclude(group = "com.google.guava", module = "guava")
    }
    // endregion

    // region compile only
    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("org.jetbrains:annotations:24.0.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    // endregion
}

tasks.shadowJar {
    archiveBaseName.set(archivesBaseName)
    archiveVersion.set(archivesVersion)

    configurations = listOf(shadowBundle)

    dependsOn(tasks.processResources)
    tasks.findByName("generatePackMCMetaJson")?.let { dependsOn(it) }

    if (tasks.findByName("remapJar") == null) {
        archiveClassifier.set("")
    } else {
        archiveClassifier.set("shadow")
    }

    minimize()

    val dest = "${mod.group}.lib"
}

tasks.withType<RemapJarTask>().matching { it.name == "remapJar" }.configureEach {
    archiveBaseName.set(archivesBaseName)
    archiveVersion.set(archivesVersion)

    dependsOn(tasks.shadowJar)
    inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

if (mod.isFabric) {
    fabricApi {
        configureTests {
            createSourceSet = true
            modId = mod.id
            enableGameTests = false
            enableClientGameTests = false
            eula = true
            // must be false
            clearRunDirectory = false
            username = "Player0"
        }
    }
}

if (mod.isForge) {
    tasks.compileTestJava {
        dependsOn("generatePackMCMetaJson")
    }
}
tasks.test {
    useJUnitPlatform()
}

publishMods {
    modrinth {
    }
    curseforge {
        client = true
        server = false
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = archivesBaseName
            version = archivesVersion

            if (tasks.findByName("remapJar") == null) {
                artifact(tasks.shadowJar) {
                    classifier = ""
                }
            } else {
                artifact(tasks.named("remapJar")) {
                    classifier = ""
                }
            }
            pom {
                name.set(mod.name)
                description.set(mod.description)
            }
        }
    }

    repositories {
        mavenLocal()
    }
}
