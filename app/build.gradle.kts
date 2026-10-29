plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.prompter.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.prompter.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 23
        versionName = "0.9.1-m9"

        // arrows Alpha is arm64-v8a. ML Kit ships a large JNI library for
        // every ABI; keeping only the target ABI cuts the APK roughly in half.
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        create("stableDebug") {
            // Debug-only key. Committed so every CI build uses the same
            // certificate and can overwrite the previous debug APK.
            storeFile = rootProject.file("keystore/prompter-debug.jks")
            storePassword = "prompterdebug"
            keyAlias = "prompterdebug"
            keyPassword = "prompterdebug"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("stableDebug")
        }
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    androidResources {
        // PromptDb uses AssetManager.openFd() to detect a changed asset.
        noCompress += "sqlite"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.18.1")
    implementation("org.jsoup:jsoup:1.20.1")
    implementation("org.bouncycastle:bcprov-jdk18on:1.80")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
