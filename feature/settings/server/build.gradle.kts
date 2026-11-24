plugins {
    alias(libs.plugins.citacomsysKtorFeature)
}

dependencies {
    implementation(project(":feature:user:api"))
    implementation(project(":feature:settings:api"))
    implementation(project(":core-server:persistence"))
    implementation(project(":feature:telemetry:api"))
    implementation(project(":feature:auth:api"))
}