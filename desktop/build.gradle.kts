import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.cli.jvm.main

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = project.group
version = project.version


kotlin {
    jvm {
        jvmToolchain(11)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":common"))
                implementation(compose.desktop.currentOs)
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TasksWidget"
            packageVersion = project.version.toString()
            modules("java.net.http")
        }
    }
}

tasks.withType(JavaExec::class.java) {
    File("""resources/version""").writeText(project.version.toString())
}
