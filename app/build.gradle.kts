plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.curizm.tv"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.curizm.tv"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.3"
    }
    
    signingConfigs {
        create("release") {
            storeFile = file("../curizm-tv-release.keystore")
            storePassword = "curizm123"
            keyAlias = "curizm-tv"
            keyPassword = "curizm123"
        }
    }
    
    buildFeatures {
        viewBinding = true
        dataBinding = false
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    
    // ExoPlayer for HLS video playback
    implementation(libs.exoplayer.core)
    implementation(libs.exoplayer.hls)
    implementation(libs.exoplayer.ui)
    
    // WebSocket communication
    implementation(libs.socketio.client)
    implementation(libs.okhttp)
    
    // QR Code generation
    implementation(libs.zxing.core)
    implementation(libs.zxing.android)
    
    // JSON parsing
    implementation(libs.gson)
}