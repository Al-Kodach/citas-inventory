plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
    alias(libs.plugins.citacomsysComposeMultiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(project(":core-client:domain"))
//            implementation(project(":core-client:design"))
            implementation(libs.material.icons.extended)
            implementation(libs.compose.adaptive.layout)
            implementation(libs.compose.adaptive)
            implementation(libs.compose.adaptive.navigation)
            implementation(libs.material3.adaptive.navigation.suite)
            api(libs.jetbrains.material3.adaptiveNavigation3)
            implementation(libs.androidx.navigation3.runtime)
        }
    }
}
