plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.citacomsys.dev.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.citacomsys.dev.sandbox"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":app:shared"))
    implementation(project(":devApp:uiShared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
}
