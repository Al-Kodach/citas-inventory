plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":types"))
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
