// ===========================
// build.gradle.kts (Module: app)
// ===========================
// 在 Android Studio 项目的 app 模块 build.gradle.kts 中添加以下依赖

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") // Firebase 插件
}

android {
    namespace = "com.lifesignal"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lifesignal"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    // ---- Firebase ----
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")    // 推送通知 (可选)
    implementation("com.google.firebase:firebase-analytics-ktx")    // 分析 (可选)

    // ---- Kotlin 协程 ----
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // ---- Jetpack Compose (UI 层) ----
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // ---- 图片加载 ----
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ---- QR 码生成 ----
    implementation("com.google.zxing:core:3.5.3")

    // ---- Google Maps & 位置服务 ----
    implementation("com.google.maps.android:maps-compose:6.2.1")          // Jetpack Compose 地图组件
    implementation("com.google.android.gms:play-services-maps:19.0.0")    // Google Maps SDK
    implementation("com.google.android.gms:play-services-location:21.3.0") // Fused Location Provider
    implementation("com.google.maps.android:android-maps-utils:3.8.2")    // 地图工具类 (聚类等)
}


// ===========================
// build.gradle.kts (Project 级别)
// ===========================
// 在项目级别 build.gradle.kts 中添加:

// plugins {
//     id("com.google.gms.google-services") version "4.4.2" apply false
// }


// ===========================
// settings.gradle.kts
// ===========================
// pluginManagement {
//     repositories {
//         google()
//         mavenCentral()
//         gradlePluginPortal()
//     }
// }
// dependencyResolution {
//     repositories {
//         google()
//         mavenCentral()
//     }
// }
