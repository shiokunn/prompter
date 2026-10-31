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
        versionCode = 25
        versionName = "0.10.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // LiteRT-LM 0.18.1 ships native code for these two 64-bit ABIs.
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
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
        // Release keys are injected by CI from GitHub Secrets and never
        // committed. Without them assembleRelease produces an unsigned APK.
        val releaseKeystore = providers.environmentVariable("PROMPTER_KEYSTORE_PATH").orNull
        if (releaseKeystore != null) {
            create("publicRelease") {
                storeFile = file(releaseKeystore)
                storePassword = providers.environmentVariable("PROMPTER_STORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("PROMPTER_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("PROMPTER_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("stableDebug")
        }
        release {
            isMinifyEnabled = false
            // Coexists with the development build on the same device.
            applicationIdSuffix = ".release"
            signingConfig = signingConfigs.findByName("publicRelease")
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
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
