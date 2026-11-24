plugins {
    alias(libs.plugins.citacomsysKtorFeature)
}

dependencies {
    implementation(project(":feature:support:api"))
    implementation(project(":core-server:persistence"))
    implementation(project(":core-server:domain"))
    implementation(project(":feature:telemetry:api"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
}
