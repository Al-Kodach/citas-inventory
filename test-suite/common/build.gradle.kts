plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":types"))
            api(libs.kotlinx.datetime)
        }
    }
}
