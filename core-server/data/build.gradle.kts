plugins {
    alias(libs.plugins.citacomsysKtorApi)
}

dependencies {
    implementation(project(":core-server:persistence"))
    implementation(project(":shared"))
    implementation(project(":core-server:domain"))

    implementation(libs.koin.ktor)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.cio)
}