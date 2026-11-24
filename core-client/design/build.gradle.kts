plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
    alias(libs.plugins.citacomsysComposeMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core-client:spi"))
            implementation(project(":core-client:domain"))
            
            implementation(libs.navigation.compose)
            api(libs.material.icons.extended)
            api(libs.compose.components.resources)
            api(libs.material.kolor)

            api(libs.compose.adaptive.layout)
            api(libs.compose.adaptive)
            api(libs.compose.adaptive.navigation)
            api(libs.material3.adaptive.navigation.suite)

            api(libs.androidx.nav3.ui)
            implementation(libs.androidx.navigation3.runtime)

            api(libs.fastscroller.material3)
            api(libs.fastscroller.indicator)
            implementation(libs.koin.compose)
            implementation(libs.ksafe.compose)
        }

        jvmMain.dependencies {

        }
    }
}




