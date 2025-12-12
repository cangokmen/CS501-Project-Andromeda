plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.watch"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.andromeda"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Use the Compose BOM to manage versions of Compose libraries consistently.
    // This is the single source of truth for versions.
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))

    // Base Compose UI libraries (versions are handled by the BOM)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // --- WEAR COMPOSE LIBRARIES ---
    // This contains the modern Slider, Button, etc.
    implementation("androidx.wear.compose:compose-material3:1.0.0-alpha21")
    // Foundation library for Wear
    implementation("androidx.wear.compose:compose-foundation")

    // Required for Icons.Default.Check
    implementation("androidx.compose.material:material-icons-extended")

    // Required for ComponentActivity and splash screen
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Google Play Services for data synchronization
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")

    implementation("com.google.code.gson:gson:2.10.1")
}