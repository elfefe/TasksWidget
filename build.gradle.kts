group "com.elfefe"
version "1.2.3"

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://jitpack.io")
    }
}

plugins {
    kotlin("multiplatform") apply false
    kotlin("android") apply false
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("org.jetbrains.compose") apply false
    id("org.openjfx.javafxplugin") version("0.0.13")
    id("com.google.gms.google-services") version("4.3.10") apply false
    id("com.google.firebase.crashlytics") version("2.9.5") apply false
}