plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":types"))
            implementation(project(":module:inventory:api"))
        }
    }
}
