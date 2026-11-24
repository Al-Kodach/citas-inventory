plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
    alias(libs.plugins.citacomsysComposeMultiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core-client:notification"))
            implementation(project(":device-service"))
            implementation(libs.material.icons.extended)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.printer.core)
            implementation(libs.printer.compose)
        }
        jvmMain.dependencies {
            implementation(libs.printer.core)
            implementation(libs.printer.compose)
        }
    }
}
