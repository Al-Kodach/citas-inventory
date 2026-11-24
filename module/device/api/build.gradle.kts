plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":feature:auth:api"))
            api(project(":kernel:api"))
        }
    }
}
