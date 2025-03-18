plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.sqlitepatient3"
    compileSdk = 35  // Updated to Android 15 (API 35)

    defaultConfig {
        applicationId = "com.example.sqlitepatient3"
        minSdk = 33  // Android 13
        targetSdk = 35  // Updated to Android 15 (API 35)
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

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Adding this to support your test resources
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

ksp {
    arg("room.schemaLocation", layout.buildDirectory.dir("schemas").get().asFile.absolutePath)
}

dependencies {
    // Core Android
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.appcompat)
    implementation(libs.activity.compose)
    implementation(libs.androidx.work.runtime.ktx)

    // Java 21 desugaring support
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.core)
    implementation(libs.compose.icons.extended)

    // Navigation
    implementation(libs.navigation.compose)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)  // Already using KSP for Room, which is good
    implementation(libs.sqlite)

    // DataStore Preferences
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Hilt for Dependency Injection
    implementation(libs.hilt.android)
    // Change from kapt to ksp for Hilt
    ksp(libs.hilt.compiler)  // Changed from kapt to ksp
    implementation(libs.hilt.navigation.compose)

    // Fix for kotlinx-metadata-jvm version compatibility
    implementation(libs.kotlinx.metadata.jvm)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)

    // Android Testing
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)

    // Room Testing - Make sure this is included
    androidTestImplementation(libs.androidx.room.testing)

    // Additional testing dependencies
    androidTestImplementation(libs.androidx.core)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.androidx.truth)
    androidTestImplementation(libs.junit) // Make sure JUnit is available for androidTest
    androidTestImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}