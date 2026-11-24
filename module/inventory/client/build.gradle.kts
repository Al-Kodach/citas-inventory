plugins {
    alias(libs.plugins.citacomsysKmpFeature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":module:inventory:api"))
            implementation(project(":pharmacy-product-ext:api"))
            implementation(project(":retail-product-ext:api"))
            implementation(project(":feature:telemetry:api"))
            implementation(project(":feature:organization:api"))
            implementation(project(":feature:workspace:api"))
            implementation(project(":feature:user:api"))
            implementation(project(":core-client:sync"))
            implementation(project(":core-client:mesh"))
            api(project(":core-client:barcode"))
            implementation(project(":core-client:security"))
            implementation(project(":core-client:data"))
            implementation(project(":core-client:domain"))
            implementation(project(":core-client:database"))

            implementation(libs.ksafe)
            implementation(libs.ksafe.biometrics)
            implementation(libs.jetbrains.material3.adaptiveNavigation3)
        }
    }
}
