plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":module:device:api"))
            implementation(project(":feature:auth:api"))
            implementation(project(":device-service"))
            implementation(libs.ksafe.biometrics)
        }
    }
}
