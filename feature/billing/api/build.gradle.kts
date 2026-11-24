plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":feature:platform:api"))
    }
}