allprojects {
    group = "com.elfefe"
    version = "1.6.2"

    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://jitpack.io")
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

/**
 * Ecrit la version dans les ressources embarquees, que le bandeau de mise a jour
 * relit a l'execution (`useResource("version")`).
 *
 * C'etait auparavant un `tasks.withType(JavaExec) { ... }` dans le module
 * desktop : execute a la *configuration*, et seulement quand on lancait
 * l'application. Un `packageMsi` n'y passait jamais, si bien que le binaire
 * publie embarquait la version du dernier `run` - soit celle d'avant. v1.5.0
 * embarquait "1.4.15", v1.5.1 embarquait "1.5.0" : l'application installee se
 * croyait en retard d'un cran et proposait indefiniment sa propre version.
 */
val writeVersionResources by tasks.registering {
    val appVersion = version.toString()
    val targets = listOf(
        file("desktop/resources/version"),
        file("common/src/commonMain/resources/version")
    )
    inputs.property("appVersion", appVersion)
    outputs.files(targets)
    doLast {
        targets.forEach { target ->
            target.parentFile.mkdirs()
            target.writeText(appVersion)
        }
    }
}

// Toute tache qui rassemble des ressources - `run` comme `packageMsi` - ecrit
// d'abord la version.
allprojects {
    tasks.matching { it.name.endsWith("ProcessResources") }.configureEach {
        dependsOn(writeVersionResources)
    }
}
