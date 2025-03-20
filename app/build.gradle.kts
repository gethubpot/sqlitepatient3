plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Room schema export location
val schemaDir = "$projectDir/schemas"

// Create a task to copy schemas to test assets
tasks.register<Copy>("copySchemaToTestAssets") {
    from(schemaDir)
    into("$projectDir/src/androidTest/assets/databases")

    doFirst {
        file("$projectDir/src/androidTest/assets/databases").mkdirs()
    }
}

android {
    namespace = "com.example.sqlitepatient3"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.sqlitepatient3"
        minSdk = 33
        targetSdk = 35
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    ksp {
        arg("room.schemaLocation", schemaDir)
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
    }

    sourceSets {
        getByName("androidTest") {
            assets {
                srcDir(schemaDir)
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
    ksp(libs.room.compiler)
    implementation(libs.sqlite)

    // DataStore Preferences
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Fix for kotlinx-metadata-jvm
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

    // Migration testing
    androidTestImplementation(libs.androidx.core.ktx)
    androidTestImplementation(libs.androidx.junit.ktx)
    androidTestImplementation(libs.androidx.sqlite.framework)

    // Compose testing
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)

    // Debug implementations
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}

tasks.configureEach {
    when (name) {
        "mergeDebugAndroidTestAssets" -> {
            dependsOn("copySchemaToTestAssets")
        }
    }
}

tasks.whenTaskAdded {
    if (name.contains("compileDebugAndroidTestKotlin") ||
        name.contains("compileDebugAndroidTestJava") ||
        name.contains("compileDebugAndroidTestSources")) {
        dependsOn("copySchemaToTestAssets")
    }
}