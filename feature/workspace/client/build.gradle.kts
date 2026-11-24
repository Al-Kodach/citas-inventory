plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":feature:workspace:api"))
            api(project(":feature:home:client"))
            api(project(":kernel:api"))
            implementation(project(":kernel:client"))
            implementation(project(":feature:platform:api"))
            api(project(":feature:organization:api"))
            implementation(project(":feature:customer:client"))
            implementation(project(":feature:billing:api"))

            implementation(project(":device-service"))
            implementation(project(":core-client:security"))
            implementation(project(":core-client:domain"))
            implementation(project(":core-client:database"))
            implementation(project(":core-client:sync"))
            implementation(project(":core-client:notification"))
            implementation(project(":core-client:ui"))

            implementation(libs.ksafe)
            implementation(libs.ksafe.biometrics)
        }
    }
}
