import org.gradle.api.JavaVersion
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.reachu.library"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    buildFeatures { compose = true }

    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    sourceSets {
        getByName("main") {
            java.setSrcDirs(emptyList<String>())
            kotlin.setSrcDirs(listOf("src/main/kotlin"))
            resources.setSrcDirs(listOf("resources"))
        }
        getByName("test") {
            java.setSrcDirs(emptyList<String>())
            kotlin.setSrcDirs(listOf("test/kotlin"))
            resources.setSrcDirs(listOf("test/resources"))
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.core:core-ktx:1.13.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")
    implementation("io.socket:socket.io-client:2.1.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-svg:2.6.0")

    // JUnit 5 (unit tests)
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Compose UI testing (JUnit4, ejecutado vía Vintage Engine)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test:core-ktx:1.5.0")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.1")
}

kotlin { jvmToolchain(17) }

tasks.withType<Test> { useJUnitPlatform() }

// --- CONFIGURACIÓN DE PUBLICACIÓN (Vanniktech) ---

mavenPublishing {
    // Define las coordenadas de tu librería
    coordinates("io.github.reachudevteam", "reachu-kotlin-sdk", "1.0.1")

    // Configura el POM con toda la información necesaria para Maven Central
    pom {
        name.set("Vio Kotlin SDK")
        description.set("Vio Kotlin SDK for Android")
        url.set("https://github.com/VioLive/VioKotlinSDK")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("reachu")
                name.set("Reachu")
                email.set("dev@vio.live")
            }
        }
        scm {
            connection.set("scm:git:https://github.com/VioLive/VioKotlinSDK.git")
            developerConnection.set("scm:git:ssh://github.com/VioLive/VioKotlinSDK.git")
            url.set("https://github.com/VioLive/VioKotlinSDK")
        }
    }

    // Indica que publique en el nuevo Portal de Sonatype Central
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    
    // Firma automáticamente las publicaciones
    signAllPublications()
}

configure<SigningExtension> {
    useGpgCmd()
}