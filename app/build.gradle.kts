plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.kot0kot0.inti.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.kot0kot0.inti.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            // ライブラリ側に 'debug' 属性がない場合、デフォルト（release/無印）を許可する
            matchingFallbacks += listOf("release")
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
        compose = true
        // TimberでBuild設定に応じたログ分岐のためのBuildConfigを使えるようにする
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

repositories {
    google()
    mavenCentral()
    mavenLocal() // ローカルのリポジトリを参照可能にする
}

dependencies {
    implementation("io.github.kot0kot0:inti-sdk:1.0-SNAPSHOT")
    implementation("no.nordicsemi.android:ble:2.6.1")
    implementation("no.nordicsemi.android:ble-ktx:2.6.1")

    // Android Core
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.2")
    val composeVersion = "1.5.1"
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-graphics:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.material3:material3:1.1.1")
    implementation("androidx.compose.material:material:$composeVersion")

    // デバッグ用
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
}