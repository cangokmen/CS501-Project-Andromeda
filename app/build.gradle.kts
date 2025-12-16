import java.util.Properties
import java.io.FileInputStream

// This block reads the local.properties file
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.andromeda"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.andromeda"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // This line now correctly reads the property and makes it available in BuildConfig
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY") ?: ""}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        // This must be true to generate the BuildConfig file
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.gson)

    // Gemini API dependency
    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")

    // Watch
    implementation("com.google.android.gms:play-services-wearable:18.2.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3") // For testing coroutines
    testImplementation("io.mockk:mockk:1.13.8") // Powerful mocking library for Kotlin
    testImplementation("androidx.arch.core:core-testing:2.2.0") // For testing LiveData/StateFlow with main dispatcher

    // Optional: For testing Android-specific parts if needed (we'll focus on pure JVM tests first)
    testImplementation("org.robolectric:robolectric:4.11.1")
}
