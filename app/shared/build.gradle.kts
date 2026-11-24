plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":feature:home:client"))
            api(project(":feature:backup:client"))
            api(project(":feature:workspace:client"))
            api(project(":feature:platform:client"))
            api(project(":feature:settings:client"))
            api(project(":feature:organization:client"))
            api(project(":feature:auth:client"))
            api(project(":feature:billing:client"))
            api(project(":feature:telemetry:client"))
            api(project(":feature:customer:client"))
            api(project(":feature:user:client"))
            api(project(":feature:support:client"))

            api(project(":device-service"))
            api(project(":shared"))
            api(project(":kernel:api"))
            api(project(":kernel:client"))

            api(project(":core-client:sync"))
            api(project(":core-client:remote"))
            api(project(":core-client:database"))
            api(project(":core-client:design"))
            api(project(":core-client:notification"))
            api(project(":core-client:domain"))
            api(project(":core-client:data"))
            api(project(":core-client:security"))
            api(project(":core-client:spi"))
            api(project(":core-client:barcode"))
            api(project(":core-client:printer"))
            api(project(":core-client:mesh"))

            api(project(":pharmacy-product-ext:client"))
            api(project(":retail-product-ext:client"))

            implementation(project(":module:inventory:client"))
            implementation(project(":module:sales:client"))
            implementation(project(":module:device:client"))

            api(libs.ksafe)
            api(libs.ksafe.compose)
            api(libs.filekit.core)
//            api(libs.logging.notification)
            implementation(libs.sqldelight.coroutines.extensions)
        }
    }
}