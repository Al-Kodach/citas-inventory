plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":app:shared"))
            api(project(":pharmacy-product-ext:client"))
        }
    }
}
