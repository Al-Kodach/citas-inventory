plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":feature:user:api"))
        }
    }
}
