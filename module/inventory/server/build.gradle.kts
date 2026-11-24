plugins {
    alias(libs.plugins.citacomsysKtorFeature)
}

dependencies {
    api(project(":module:inventory:api"))
    implementation(project(":core-server:notification"))
    implementation(project(":core-server:persistence"))
    implementation(project(":feature:user:api"))
    implementation(project(":feature:telemetry:api"))
    implementation(project(":feature:organization:api"))
    implementation(project(":feature:workspace:api"))

    implementation(libs.identity.jvm)
    implementation(libs.kotlinx.datetime)

    testImplementation(project(":test-suite:server"))
}
