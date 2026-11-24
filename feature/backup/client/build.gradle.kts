plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":feature:backup:api"))

            implementation(project(":core-client:database"))
            implementation(project(":core-client:spi"))
            implementation(project(":core-client:domain"))
            implementation(project(":feature:workspace:api"))

            implementation(libs.okio)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)
        }
    }
}
