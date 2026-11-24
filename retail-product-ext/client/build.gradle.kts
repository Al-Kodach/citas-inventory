plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":retail-product-ext:api"))

            implementation(project(":feature:organization:client"))
            implementation(project(":feature:auth:client"))
            implementation(project(":feature:billing:client"))
            implementation(project(":feature:platform:client"))
            implementation(project(":feature:workspace:client"))

            implementation(project(":feature:user:client"))
            implementation(project(":feature:telemetry:client"))

            implementation(project(":module:sales:api"))
            implementation(project(":module:sales:client"))
            implementation(project(":module:inventory:api"))
            implementation(project(":module:inventory:client"))
            implementation(project(":feature:customer:client"))

            implementation(project(":core-client:database"))
            implementation(project(":core-client:mesh"))
            implementation(project(":core-client:domain"))
            implementation(project(":core-client:spi"))

            api(libs.ksafe)
            api(libs.ksafe.compose)
        }
    }
}
