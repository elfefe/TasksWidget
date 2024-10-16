plugins {
    id("org.jetbrains.compose")
    id("com.android.application")
    kotlin("android")
}

group = project.group
version = project.version

repositories {
    jcenter()
}

dependencies {
    implementation(project(":common"))
    implementation("androidx.activity:activity-compose:1.5.0")
    implementation("dev.gitlive:firebase-auth:1.7.2")
    implementation("dev.gitlive:firebase-firestore:1.7.2")
    implementation("dev.gitlive:firebase-crashlytics:1.7.2")
    implementation("dev.gitlive:firebase-crashlytics:1.7.2")
}

android {
    compileSdkVersion(33)
    defaultConfig {
        applicationId = "com.elfefe.android"
        minSdkVersion(24)
        targetSdkVersion(33)
        versionCode = 1
        versionName = "1.0-SNAPSHOT"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    namespace = "com.elfefe.android"
}