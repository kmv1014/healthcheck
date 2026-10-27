plugins {
    id("com.android.application")
}

android {
    namespace = "com.parivarhealth.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.parivarhealth.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed with the debug key so the APK installs directly. Use your own key before publishing to Play Store.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
