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
    lintOptions {
        isAbortOnError = false
        isWarningsAsErrors = false
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
    kapt("androidx.room:room-compiler:2.6.1")

    // Retrofit2 for API calls
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    kapt("com.google.dagger:hilt-compiler:2.48")

    // Image Loading
    implementation(libs.glide)
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // Trabalho em segundo plano (notificações locais)
    implementation(libs.androidx.work.runtime)

    // Firebase Cloud Messaging only (no Analytics dependency).
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Kapt configuration
kapt {
    correctErrorTypes = true
}
