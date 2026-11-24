plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":kernel:api"))
            implementation(project(":feature:platform:api"))
            implementation(project(":feature:telemetry:api"))
            implementation(project(":module:device:api"))
            implementation(project(":feature:user:api"))
            implementation(project(":feature:billing:api"))
            implementation(project(":feature:workspace:api"))
            implementation(project(":feature:organization:api"))
            implementation(project(":core-client:domain"))

            api(libs.filekit.core)
        }
    }
}
