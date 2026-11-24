plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":types"))
            api(project(":kernel:api"))
            api(project(":feature:workspace:api"))
            implementation(project(":feature:platform:api"))
            implementation(project(":feature:billing:api"))
        }
    }
}
