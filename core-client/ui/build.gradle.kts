plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
    alias(libs.plugins.citacomsysComposeMultiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core-client:design"))
            api(project(":core-client:security"))

            implementation(project(":core-client:domain"))
            implementation(project(":core-client:notification"))
            implementation(project(":shared"))
            implementation(project(":core-client:remote"))
            implementation(project(":feature:user:api"))
            implementation(project(":feature:billing:api"))
            implementation(project(":feature:workspace:api"))
            implementation(project(":feature:organization:api",
))
            implementation(project(":feature:platform:api"))
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.filekit.dialogs.compose)

            implementation(libs.ksafe)
        }
    }
}





