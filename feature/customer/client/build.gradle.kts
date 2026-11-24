plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":feature:customer:api"))
            implementation(project(":core-client:database"))
            implementation(project(":core-client:mesh"))
            implementation(libs.sqldelight.coroutines.extensions)
            implementation(libs.ksafe)
            implementation(project(":core-client:domain"))
            implementation(project(":feature:telemetry:api"))
        }
    }
}
