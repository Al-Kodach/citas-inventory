plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":feature:organization:api"))
            api(project(":kernel:api"))
            api(project(":feature:workspace:api"))
            implementation(project(":feature:workspace:client"))
            
            implementation(project(":feature:platform:client"))
            implementation(project(":core-client:database"))
            implementation(project(":core-client:mesh"))
            implementation(project(":core-client:domain"))
            implementation(project(":core-client:ui"))
            implementation(project(":feature:settings:client"))
            implementation(project(":feature:platform:api"))
            implementation(project(":feature:billing:api"))
            implementation(project(":device-service"))
            implementation(libs.sqldelight.coroutines.extensions)
            implementation(libs.ksafe)
            implementation(libs.ksafe.compose)
        }
    }
}
