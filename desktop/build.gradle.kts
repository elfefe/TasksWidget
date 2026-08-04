import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.cli.jvm.main
import org.jetbrains.kotlin.load.kotlin.signatures

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = project.group
version = project.version


kotlin {
    jvm {
        // 17 comme le module common : deux niveaux de bytecode dans un meme
        // binaire n'avaient pas lieu d'etre.
        jvmToolchain(17)
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

            description = "TasksWidget is a multiplatform application designed to help you manage your tasks efficiently."
            copyright = "© 2024 Fedacier"

            windows {
                // Installation par-utilisateur : dans %LOCALAPPDATA%, sans
                // élévation. Les mises à jour (msiexec) s'appliquent alors sans
                // invite UAC — indispensable pour un auto-update sans surveillance.
                perUserInstall = true
                shortcut = true
                menu = true
            }
        }

        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")
    }
}

// L'écriture des ressources de version vit dans le build racine : elle
// alimente les deux modules, et seul le racine peut brancher la dépendance sur
// leurs tâches de ressources.

tasks.register<Exec>("signMsi") {
    dependsOn("packageMsi")
    val msiFile = file("build/compose/binaries/main/msi/TasksWidget-${version}.msi")
    commandLine(
        arrayOf("signtool",
        "sign",
        "/f", "\"${file("certificate.pfx").path}\"",
        "/p", "\"${file("key-password").readText().trim()}\"",
        "/fd", "SHA256",
        "/tr", "http://timestamp.digicert.com",
        "/td", "SHA256",
        "\"${msiFile.path}\"").joinToString(" ")
    )
}
