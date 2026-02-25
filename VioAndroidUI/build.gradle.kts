plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.reachu.VioUI"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-svg:2.6.0")
    implementation("com.klarna.mobile:sdk:2.10.0")
    implementation("com.stripe:stripe-android:20.49.0")
    implementation(project(":library"))
}

kotlin { jvmToolchain(17) }

// --- CONFIGURACIÓN DE PUBLICACIÓN (Vanniktech) ---

mavenPublishing {
    // Define las coordenadas de tu librería
    coordinates("io.github.viodevteam", "vio-android-ui", "1.0.0")

    // Configura el POM con toda la información necesaria para Maven Central
    pom {
        name.set("Vio Android UI")
        description.set("Vio Android UI components including Klarna, Stripe, and Vipps payment integrations")
        url.set("https://github.com/VioDevteam/VioKotlinSDK")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("vio")
                name.set("Vio")
                email.set("dev@vio.live")
            }
        }
        scm {
            connection.set("scm:git:https://github.com/VioDevteam/VioKotlinSDK.git")
            developerConnection.set("scm:git:ssh://github.com/VioDevteam/VioKotlinSDK.git")
            url.set("https://github.com/VioDevteam/VioKotlinSDK")
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
