import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

// Keep local builds usable without committing google-services.json. CI/release builds
// should provide the Firebase client configuration for their application variant.
val hasGoogleServicesConfig = file("google-services.json").exists() ||
    fileTree("src").matching { include("**/google-services.json") }.files.isNotEmpty()
if (hasGoogleServicesConfig) {
    apply(plugin = "com.google.gms.google-services")
}

// Web client ID do OAuth. Configure via gradle.properties ou local.properties
// (GOOGLE_WEB_CLIENT_ID=...apps.googleusercontent.com). O valor é público,
// mas não deve ser hardcoded no código para permitir builds por ambiente.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
val googleWebClientId = (project.findProperty("GOOGLE_WEB_CLIENT_ID") as String?)
    ?: localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", "")

android {
    namespace = "br.com.tecpontes.appfinanceiro"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.tecpontes.appfinanceiro"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
    lint {
        abortOnError = false
        warningsAsErrors = false
    }


}

dependencies {
    // Core Android
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.swiperefreshlayout)

    // Material Design 3
    implementation(libs.material)

    // Jetpack Compose (as it's used for Theming)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)

    // Architecture Components
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Retrofit2 for API calls
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Image Loading
    implementation(libs.glide)
    kapt(libs.glide.compiler)

    // Trabalho em segundo plano (notificações locais)
    implementation(libs.androidx.work.runtime)

    // Firebase Cloud Messaging only (no Analytics dependency).
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Login com Google (Credential Manager)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Kapt configuration
kapt {
    correctErrorTypes = true
}
