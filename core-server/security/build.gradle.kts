plugins {
    alias(libs.plugins.citacomsysKtorApi)
}

dependencies {
    implementation(project(":admin:api"))
    implementation(project(":core-server:notification"))
    implementation(project(":kernel:api"))
    implementation(project(":feature:user:api"))
    implementation(project(":feature:auth:api"))
    implementation(project(":feature:platform:api"))
    implementation(project(":feature:organization:api"))
    implementation(project(":feature:workspace:api"))
    implementation(project(":feature:telemetry:api"))
    implementation(project(":module:inventory:api"))

    implementation(project(":core-server:domain"))
    implementation(project(":core-server:persistence"))

    implementation(project(":shared"))

    implementation(libs.koin.ktor)
    implementation(libs.mindrot.jbcrypt)
    api(libs.ktor.server.auth.jwt)
    api(libs.ktor.server.rate.limit)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.caffeine)
    implementation(libs.geoip2)
    implementation(libs.commons.compress)

    // Tailscale API client
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutinesTest)
    testImplementation(libs.mockk)
}
