import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.android.library")
    kotlin("plugin.serialization") version embeddedKotlinVersion
}

group = project.group
version = project.version

val ktorVersion = "2.2.3"

kotlin {
    android()
    jvm("desktop") {
        jvmToolchain(11)
    }

    sourceSets {
        val commonMain by getting {
            resources.srcDirs("resources")

            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)

                implementation(compose("org.jetbrains.compose.ui:ui-util"))
                implementation("com.google.guava:guava:32.1.1-jre")
                implementation("com.google.cloud:google-cloud-firestore:3.13.4")
                implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")

                api("io.jsonwebtoken:jjwt-api:0.11.5")
                runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
                runtimeOnly("io.jsonwebtoken:jjwt-orgjson:0.11.5")

                implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

                implementation("com.google.code.gson:gson:2.10.1")

                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-cio:$ktorVersion")

                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-server-netty:$ktorVersion")

                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-gson:$ktorVersion")

                implementation("com.jcraft:jsch:0.1.55")

                implementation("org.apache.poi:poi:5.2.0")
                implementation("org.apache.poi:poi-ooxml:5.2.0")

                implementation("androidx.constraintlayout:constraintlayout:2.2.0-alpha07")
                implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0-alpha07")

                implementation("com.guardsquare:proguard-gradle:7.2.2")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.12.0")
            }
        }
        val androidTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
            }
        }
        val desktopMain by getting {
            dependencies {
                api(compose.preview)
            }
        }
        val desktopTest by getting
    }
}

android {
    compileSdk = 33
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 24
        targetSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    namespace = "com.elfefe.common"
}

dependencies {
    implementation("androidx.navigation:navigation-compose:2.7.6")
}
