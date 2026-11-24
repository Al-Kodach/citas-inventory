plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":feature:billing:api"))
            implementation(project(":feature:user:api"))
            implementation(project(":feature:platform:api"))
        }
    }
}
