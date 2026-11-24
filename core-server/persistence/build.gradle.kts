plugins {
    alias(libs.plugins.citacomsysKtorApi)
    alias(libs.plugins.fly.way)
}

dependencies {
    implementation(project(":core-server:domain"))
    api(project(":kernel:api"))
    implementation(project(":shared"))
    implementation(project(":types"))

    implementation(libs.ktor.server.core)
    implementation(libs.koin.ktor)

    // Exposed & Database
    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    api(libs.exposed.json)
    api(libs.exposed.kotlin.datetime)
    // Postgres
    implementation(libs.postgresql)
    implementation(libs.postgres.socket.factory)
    implementation(libs.hikari.cp)
    implementation(libs.jdbc.driver)
    implementation(libs.fly.way.core)

    implementation(libs.kotlinx.serialization.json)
}