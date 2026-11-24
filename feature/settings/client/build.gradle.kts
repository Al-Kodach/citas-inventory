plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":feature:billing:api"))
            api(project(":feature:settings:api"))
            api(project(":feature:telemetry:api"))
            implementation(project(":module:device:api"))
            implementation(project(":feature:organization:api"))
            implementation(project(":feature:auth:api"))
            implementation(project(":feature:backup:client"))

            implementation(project(":core-client:sync"))
            implementation(project(":core-client:domain"))
            implementation(project(":core-client:database"))
            implementation(project(":core-client:ui"))
            implementation(project(":core-client:mesh"))
            implementation(libs.ksafe)
        }
    }
}
