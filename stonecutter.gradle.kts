plugins {
    id("dev.kikugie.stonecutter")

    val modstitchVersion = "0.8.4"
    id("dev.isxander.modstitch.base") version modstitchVersion apply false
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT" apply false

    id("me.modmuss50.mod-publish-plugin") version "0.8.4" apply false
}

stonecutter active "26.2-fabric"

allprojects {
    repositories {
        mavenCentral()

        // Sometime it responses 502 Bad Gateway
        // https://github.com/Leawind/Perspective-API/actions/runs/29914253769/job/88907885668
        // maven("https://maven.terraformersmc.com/") // ModMenu
        maven("https://maven.gnomecraft.net/releases") {
            name = "GnomeCraft (Terraformers Mirror)"
        }

        maven("https://maven.isxander.dev/releases") // YACL
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.nucleoid.xyz") // Placeholder API (ModMenu dependency)
        exclusiveContent {
            forRepository { maven("https://thedarkcolour.github.io/KotlinForForge/") }
            filter { includeGroup("thedarkcolour") }
        }
    }
}
