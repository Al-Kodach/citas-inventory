plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
    alias(libs.plugins.citacomsysComposeMultiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.kotlinx.serialization.json)
            api(libs.kmpnotifier.core)
            api(libs.kmpnotifier.local)
            implementation(libs.material.icons.extended)
        }
    }
}
