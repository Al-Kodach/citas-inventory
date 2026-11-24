plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":feature:support:api"))
            implementation(project(":feature:auth:api"))
            implementation(project(":feature:telemetry:api"))
            implementation(project(":core-client:sync"))
            implementation(project(":core-client:domain"))
            implementation(project(":core-client:ui"))
            implementation(libs.koin.core)
        }
    }
}
