plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":feature:customer:api"))
            implementation(project(":module:sales:api"))
            implementation(project(":module:inventory:api"))
            implementation(project(":feature:billing:api"))
            implementation(project(":feature:workspace:api"))

            implementation(project(":feature:user:api"))
            implementation(project(":feature:telemetry:api"))
            implementation(project(":core-client:domain"))
            implementation(project(":core-client:mesh"))
            implementation(project(":core-client:barcode"))
            implementation(project(":core-client:printer"))
            implementation(project(":core-client:sync"))
            implementation(project(":core-client:database"))

            implementation(project(":device-service"))

            implementation(libs.connectivity.core)
            implementation(libs.connectivity.http)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ksafe)
        }
    }
}
