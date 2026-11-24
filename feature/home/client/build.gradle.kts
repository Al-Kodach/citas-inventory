plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":feature:home:api"))

            implementation(project(":feature:auth:api"))
            implementation(project(":feature:organization:api"))
            implementation(project(":feature:organization:api",
))
            implementation(project(":feature:platform:api"))
            implementation(project(":core-client:data"))
            implementation(project(":core-client:security"))
            implementation(project(":core-client:domain"))
            implementation(project(":core-client:database"))
            implementation(project(":core-client:design"))
            implementation(project(":device-service"))
            implementation(project(":core-client:remote"))

            implementation(libs.ksafe)
            implementation(libs.kotlinx.datetime)
        }
    }
}