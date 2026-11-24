plugins {
    alias(libs.plugins.citacomsysKtorFeature)
}

dependencies {
    implementation(project(":feature:workspace:api"))
    implementation(project(":retail-product-ext:api"))
    implementation(project(":module:inventory:server"))
    implementation(project(":feature:telemetry:api"))
    implementation(project(":core-server:persistence"))
    implementation(project(":core-server:security"))
    implementation(project(":shared"))
}
