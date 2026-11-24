plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
    alias(libs.plugins.citacomsysComposeMultiplatform)
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.connectivity.device)
            api(libs.work.runtime)
            api(libs.androidx.core.ktx)
        }

        commonMain.dependencies {
            implementation(project(":core-client:spi"))
            implementation(project(":core-client:notification"))
            implementation(project(":core-client:design"))
            implementation(project(":feature:telemetry:api"))
            implementation(project(":feature:organization:api"))
            implementation(project(":feature:workspace:api"))
            implementation(project(":feature:user:api"))
            implementation(project(":device-service"))
            implementation(project(":shared"))
            implementation(project(":kernel:api"))
            implementation(project(":core-client:domain"))
            implementation(project(":core-client:database"))
            implementation(project(":core-client:sync"))
            implementation(project(":core-client:remote"))

            implementation(libs.koin.compose.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.okio)
            implementation(libs.connectivity.core)
            implementation(libs.filekit.core)
            implementation(libs.ksafe)
        }

        jvmMain.dependencies {
            implementation(libs.connectivity.http)
        }

        webMain.dependencies {
            implementation(libs.connectivity.http)
        }
    }
}
