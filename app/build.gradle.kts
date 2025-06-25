import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt)
}

val secretsProperties = Properties().apply {
    val secretsPropertiesFile = rootProject.file("secrets.properties")
    if (secretsPropertiesFile.exists()) {
        load(FileInputStream(secretsPropertiesFile))
    }
}



android {
    namespace = "com.me.gemini"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.me.gemini"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val geminiKey = secretsProperties.getProperty("GEMINI_API_KEY") ?: "MISSING_KEY"
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8" // Match with your Kotlin version
    }


    buildFeatures {
        compose = true
        buildConfig = true // ✅ تأكد إنه جوه android
    }

}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xcontext-receivers"
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.07.00")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(composeBom)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //HILT
    implementation(libs.androidx.hilt.work)
    implementation(libs.hilt.android) // Use latest version
    ksp(libs.hilt.android.compiler) // For Kotlin projects
    // Hilt Navigation Compose
    implementation(libs.androidx.hilt.navigation.compose) // Use latest version

    // Gemini AI
    implementation(libs.generativeai)

    // WORKMANAGER
    implementation(libs.androidx.work.runtime.ktx)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.ui)
    implementation(libs.material3)
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Also ensure you have the basic Compose dependencies
    implementation(libs.androidx.activity.compose)
    implementation(composeBom)

    // Coil for images (if needed)
    implementation(libs.coil.compose)


    // Gemini SDK
    implementation(libs.generativeai)

    // Android Speech APIs
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Text-to-Speech (TTS)
    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.material.icons.extended)

    implementation(libs.accompanist.permissions) // Use latest version
    implementation(libs.androidx.activity.compose) // For rememberLauncherForActivityResult

    // TensorFlow Lite for on-device ML
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.task.text)


// Optional: For JSON parsing if your knowledge base is in JSON
    implementation(libs.gson)

}