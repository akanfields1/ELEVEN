plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.eleven"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.eleven"
        minSdk = 26
        targetSdk = 33
        versionCode = 2
        versionName = "0.1.1"
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
}
