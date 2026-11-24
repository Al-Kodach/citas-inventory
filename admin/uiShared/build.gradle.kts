
plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":admin:api"))

            api(project(":feature:settings:client"))
            api(project(":feature:telemetry:client"))
            api(project(":feature:backup:client"))
            api(project(":feature:user:api"))
            api(project(":feature:billing:api"))
            api(project(":feature:organization:api"))
            api(project(":feature:organization:client"))
            api(project(":feature:workspace:api"))
            api(project(":feature:platform:api"))
            api(project(":feature:support:client"))

            api(project(":module:device:api"))
            api(project(":shared"))
            api(project(":device-service"))

            api(project(":core-client:remote"))
            api(project(":core-client:ui"))
            api(project(":core-client:data"))
            api(project(":core-client:database"))
            api(project(":core-client:domain"))
            api(project(":core-client:notification"))
            implementation(project(":core-client:security"))

            api(libs.ksafe)
            api(libs.ksafe.biometrics)
            api(libs.androidx.nav3.ui)
            api(libs.filekit.core)
            api(libs.filekit.dialogs)
            api(libs.filekit.dialogs.compose)

            api(libs.ktor.client.core)
            api(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.cio)
        }
    }
}
