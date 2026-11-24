plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":feature:user:api"))
            implementation(project(":feature:workspace:api"))
            implementation(project(":feature:organization:api",
))

            implementation(project(":shared"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.okio)
        }
    }
}
