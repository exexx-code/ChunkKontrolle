// LabyMod 3 Addon für 1.8.9. libs/lm_api_mc1.8.9.jar lädt der GitHub-Build (.github/workflows/build.yml).
plugins {
    java
    id("gg.essential.loom") version "1.15.52"
    id("dev.architectury.architectury-pack200") version "0.1.3"
}

version = "1.0.1"
java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

loom {
    forge { pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter()) }
}

repositories { mavenCentral() }

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")
    compileOnly(files("libs/lm_api_mc1.8.9.jar"))
}

tasks.withType<JavaCompile> { options.encoding = "UTF-8" }

tasks.remapJar {
    archiveBaseName.set("ChunkBugAddon")
    targetNamespace.set("official") // LabyMod läuft auf Vanilla-Namen, nicht auf Forge-Namen
}
