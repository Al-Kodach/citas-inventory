plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":feature:billing:api"))

            implementation(project(":feature:organization:client"))
            implementation(project(":feature:organization:client"))
            implementation(project(":feature:organization:api",
))
            implementation(project(":feature:platform:api"))

            implementation(project(":core-client:data"))
            implementation(project(":core-client:security"))
            implementation(project(":core-client:domain"))
            implementation(project(":core-client:design"))
            implementation(project(":device-service"))
            implementation(project(":core-client:remote"))

            implementation(libs.ksafe.biometrics)
            implementation(libs.ksafe)
            implementation(libs.kotlinx.datetime)
        }
    }
}