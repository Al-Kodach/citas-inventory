plugins {
    alias(libs.plugins.citacomsysKtorFeature)
}
dependencies {
    implementation(project(":feature:user:api"))
    implementation(project(":core-server:security"))

    implementation(libs.caffeine)
    implementation(project(":core-server:persistence"))

    implementation(project(":feature:telemetry:api"))

    testImplementation(project(":test-suite:server"))
}
