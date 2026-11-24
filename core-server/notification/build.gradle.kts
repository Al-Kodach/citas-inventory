plugins {
    alias(libs.plugins.citacomsysKtorApi)
}

dependencies {
    implementation(project(":core-server:domain"))
    implementation(project(":core-server:persistence"))
    implementation(project(":feature:organization:api"))
    implementation(project(":feature:workspace:api"))
    implementation(project(":feature:telemetry:api"))
    implementation(project(":shared"))

    implementation(libs.ktor.server.config.yaml)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.koin.ktor)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.server.sse)
}
