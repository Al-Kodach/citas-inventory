plugins {
    alias(libs.plugins.citacomsysKotlinJvmApi)
}

dependencies {
    api(project(":test-suite:common"))
    api(project(":shared"))
    api(project(":feature:user:api"))
    api(project(":core-server:notification"))
    api(project(":core-server:domain"))
    api(project(":core-server:security"))
    api(project(":core-server:data"))
    api(project(":core-server:persistence"))
    api(project(":module:inventory:api"))
    api(project(":module:inventory:server"))

    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    api(libs.mockk)
    api(libs.ktor.server.auth)
    api(libs.ktor.server.auth.jwt)
    api(libs.ktor.server.test.host)
    api(libs.testcontainers.postgresql)
    api(libs.koin.ktor)
    api(libs.ktor.server.content.negotiation)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.serialization.kotlinx.json)
    api(libs.ktor.server.doublereceive)
    api(libs.junit.jupiter)
    api(libs.h2database.h2)
}
