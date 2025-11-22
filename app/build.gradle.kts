plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.riomarappnav"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.riomarappnav"
        minSdk = 24
        //noinspection OldTargetApi
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.2.21"
    }
}

dependencies {
    // Material Design
    implementation(libs.material.v190)

    // Glide
    implementation(libs.glide)
    implementation(libs.androidx.activity)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.play.services.location)
    implementation(libs.play.services.vision.common)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.storage)
    annotationProcessor(libs.compiler)

    // Import the BoM for the Firebase platform
    implementation(platform(libs.firebase.bom))

    // Firebase Libraries
    implementation(libs.google.firebase.auth)
    implementation(libs.firebase.analytics)

    // Google Play services library
    implementation(libs.play.services.auth)

    // AndroidX Libraries
    implementation(libs.androidx.core.ktx.v1120)
    implementation(libs.androidx.lifecycle.runtime.ktx.v262)
    implementation(libs.androidx.activity.compose.v172)
    implementation(libs.androidx.appcompat.v170)
    implementation(libs.androidx.constraintlayout)

    // CameraX
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle.v120)
    implementation(libs.androidx.camera.view.v120)
    implementation(libs.androidx.camera.core)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Material Design 3
    implementation(libs.material3)

    // Android Studio Preview support
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.ui.tooling)

    // UI Tests
    androidTestImplementation(libs.androidx.compose.ui.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.ui.test.manifest)

    // Datastore
    implementation(libs.androidx.datastore.preferences)

    // TensorFlow Lite & GPU Support
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)
    // --- ADICIONADO SUPORTE A GPU ---
    implementation(libs.tensorflow.lite.gpu)
    implementation(libs.tensorflow.lite.gpu.delegate.plugin)
    // -------------------------------

    // GPS
    implementation(libs.play.services.location)
}