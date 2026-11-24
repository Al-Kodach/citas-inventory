plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":feature:user:api"))
            implementation(project(":feature:telemetry:api"))
            implementation(project(":feature:organization:api",
))

            implementation(project(":core-client:data"))
            implementation(project(":core-client:domain"))
            implementation(project(":core-client:database"))
            implementation(project(":core-client:security"))
            implementation(project(":core-client:sync"))
        }
    }
}
