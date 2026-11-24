plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":kernel:api"))
            implementation(project(":feature:billing:api"))
            implementation(project(":module:device:api"))
            implementation(project(":feature:platform:api"))
        }
    }
}