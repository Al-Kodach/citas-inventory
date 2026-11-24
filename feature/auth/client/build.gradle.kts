plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":feature:auth:api"))
                implementation(project(":feature:telemetry:api"))
                implementation(project(":feature:user:api"))

                implementation(project(":device-service"))
                implementation(project(":shared"))
                implementation(project(":core-client:remote"))
                implementation(project(":core-client:security"))
                implementation(project(":core-client:sync"))
                implementation(project(":core-client:database"))
                implementation(project(":core-client:domain"))
                implementation(project(":core-client:ui"))

                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ksafe)
                implementation(libs.ksafe.biometrics)
            }
        }
    }
}
