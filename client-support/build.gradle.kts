plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":feature:user:client"))
            implementation(project(":feature:auth:client"))
            implementation(project(":feature:organization:client"))
            implementation(project(":feature:user:client"))
            implementation(project(":feature:settings:client"))
            implementation(project(":feature:home:client"))
            implementation(project(":feature:backup:client"))
            implementation(project(":feature:billing:client"))
            implementation(project(":feature:workspace:client"))
            implementation(project(":feature:platform:client"))
            implementation(project(":feature:organization:client"))
            implementation(project(":feature:workspace:api"))

            implementation(project(":module:sales:client"))
            implementation(project(":module:inventory:client"))
            implementation(project(":module:device:client"))

            implementation(project(":core-client:remote"))
            implementation(project(":shared"))

            implementation(project(":core-client:sync"))
            implementation(project(":core-client:design"))
            implementation(project(":core-client:domain"))
            implementation(project(":core-client:data"))
            implementation(project(":core-client:security"))
            implementation(project(":core-client:spi"))
            implementation(project(":core-client:database"))


            implementation(libs.ksafe)
        }
    }
}
