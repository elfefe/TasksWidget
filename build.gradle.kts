allprojects {
    group = "com.elfefe"
    version = "1.4.10"

    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://jitpack.io")
        maven("https://jogamp.org/deployment/maven")
    }
}

plugins {
    // Versions retirees d'ici : elles vivent desormais dans gradle.properties,
    // lues par le pluginManagement de settings.gradle.kts. Les redeclarer ici
    // recreerait la double verite que ce nettoyage supprime.
    kotlin("multiplatform") apply false
    kotlin("plugin.serialization") apply false
    id("org.jetbrains.compose") apply false
    id("org.openjfx.javafxplugin") version "0.0.13"
}

