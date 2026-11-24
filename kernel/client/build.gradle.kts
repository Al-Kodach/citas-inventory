plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            api(libs.work.runtime)
        }

        commonMain.dependencies {
            api(project(":kernel:api"))
            implementation(project(":shared"))
            implementation(project(":core-client:domain"))
            implementation(project(":core-client:domain"))
            implementation(project(":core-client:remote"))
            implementation(project(":core-client:spi"))
            implementation(project(":core-client:security"))
            implementation(project(":device-service"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ksafe)
            implementation(libs.napier)
            implementation(libs.sqldelight.coroutines.extensions)
        }
    }
}
