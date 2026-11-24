plugins {
    alias(libs.plugins.citacomsysKtorFeature)
}

dependencies {
    implementation(project(":feature:auth:api"))
    implementation(project(":feature:user:api"))

    implementation(project(":core-server:domain"))
    implementation(project(":core-server:persistence"))
    implementation(project(":core-server:security"))
    implementation(project(":feature:workspace:api"))

    implementation(libs.caffeine)
    implementation(project(":feature:platform:api"))
    implementation(project(":feature:telemetry:api"))
    implementation(project(":feature:organization:api"))
    implementation(project(":module:device:api"))
    implementation(project(":core-server:notification"))
}
