allprojects {
    group = "com.elfefe"
    version = "1.4.3"

    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://jitpack.io")
        maven("https://jogamp.org/deployment/maven")
    }
}

plugins {
    kotlin("multiplatform") version "1.9.0" apply false
    id("org.jetbrains.compose") version "1.5.0" apply false
    id("org.openjfx.javafxplugin") version "0.0.13"
    kotlin("plugin.serialization") version "1.9.0" apply false
//    kotlin("android") apply false
//    id("com.android.application") apply false
//    id("com.android.library") apply false
}

