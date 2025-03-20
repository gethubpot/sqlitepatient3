plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.sqlitepatient3"
    compileSdk = 35  // Android 15 (API 35)

    defaultConfig {
        applicationId = "com.example.sqlitepatient3"
        minSdk = 33  // Android 13
        targetSdk = 35  // Android 15 (API 35)
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

    // Testing configuration
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    // Room schema export location - proper configuration for migration testing
    // This will be used by both KSP to export schemas and by tests to verify them
    val schemaDir = "$projectDir/schemas"

    // Configure KSP to export schemas
    ksp {
        arg("room.schemaLocation", schemaDir)
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
    }

    // Configure source sets to avoid duplicate content roots
    sourceSets {
        // Main source set includes the schemas as assets
        getByName("main") {
            assets {
                // Exclude the schemas directory from main assets to avoid duplication
                // We'll add it back during the packaging task
                srcDir(schemaDir)
            }
        }

        // AndroidTest source set will use a different resource directory
        getByName("androidTest") {
            // We'll create a directory for the test to access the schemas
            assets {
                // Do not directly reference the same schema directory
                // The schemas will be copied to the test directory by our custom task
            }
        }
    }

    // Create a task to copy schemas to test assets
    tasks.register<Copy>("copySchemaToTestAssets") {
        from(schemaDir)
        into("$projectDir/src/androidTest/assets/schemas")
        // Make sure this task runs before compiling androidTest
        tasks.whenTaskAdded {
            if (name.contains("compileDebugAndroidTestSources")) {
                dependsOn("copySchemaToTestAssets")
            }
        }
    }
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
    ksp(libs.room.compiler)  // Using KSP for Room
    implementation(libs.sqlite)

    // DataStore Preferences
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Hilt for Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)  // Using KSP for Hilt
    implementation(libs.hilt.navigation.compose)

    // Fix for kotlinx-metadata-jvm version compatibility
    implementation(libs.kotlinx.metadata.jvm)

    // Testing - Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)

    // Testing - Instrumented tests
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.androidx.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.junit)

    // Migration testing dependencies
    androidTestImplementation("androidx.test:core-ktx:1.6.0")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.2.0")
    androidTestImplementation("androidx.sqlite:sqlite-framework:2.4.0")

    // Compose testing
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)

    // Debug implementations
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}

// Make sure the copy schema task runs before any test compilation
tasks.whenTaskAdded {
    if (name.contains("compileDebugAndroidTestKotlin") || name.contains("compileDebugAndroidTestJava")) {
        dependsOn("copySchemaToTestAssets")
    }
}