plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":feature:telemetry:api"))
                implementation(project(":core-client:database"))
                implementation(project(":core-client:domain"))
                implementation(project(":device-service"))
                implementation(libs.sqldelight.coroutines.extensions)
                implementation(libs.ksafe.biometrics)
            }
        }
    }
}
