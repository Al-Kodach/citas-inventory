plugins {
    alias(libs.plugins.citacomsysKtorFeature)
}

dependencies {
    api(project(":feature:telemetry:api"))
    implementation(project(":feature:user:api"))

    implementation(project(":core-server:persistence"))
    implementation(project(":core-server:notification"))
}